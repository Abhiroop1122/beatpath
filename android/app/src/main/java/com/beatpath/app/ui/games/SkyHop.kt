package com.beatpath.app.ui.games

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatpath.app.audio.AudioEngine
import com.beatpath.app.audio.Sfx
import com.beatpath.app.lessons.Basics
import com.beatpath.app.model.NoteColors
import com.beatpath.app.ui.aero.Gel
import com.beatpath.app.ui.aero.GelButton
import com.beatpath.app.ui.aero.LouBubble
import com.beatpath.app.ui.lou.LouMood
import com.beatpath.app.ui.theme.OpenSans
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/** A cloud to land on: how far the note moves (+2 leap up … −2 leap down, 0 same), its label, its row from the top. */
private data class Cloud(val move: Int, val label: String, val slot: Int) {
    /** How Lou says what happened, in a full sentence. */
    val went: String get() = when (move) { 2 -> "It was a big leap up"; 1 -> if (label == "Higher") "It went up" else "It was a small step up"; 0 -> "It stayed the same"
        -1 -> if (label == "Lower") "It went down" else "It was a small step down"; else -> "It was a big leap down" }
}
/** A sung note floating up from Lou's speaker. */
private class Sung(val text: String, val color: Color, val born: Long, val toClouds: Boolean)

/**
 * Sky Hop: Lou stands on a cloud and sings two notes. Tap the cloud where the second note went: higher, lower or the same;
 * after 6 points, a small step or a big leap. Lou winds up, jumps, and lands; the camera follows him to his new cloud.
 * A wrong cloud puffs apart under him and he drops back home. Three hearts.
 */
@Composable
fun BoxScope.SkyHop(control: GameControl, engine: AudioEngine, sfx: Sfx) {
    val scope = rememberCoroutineScope()
    val rnd = remember { Random(System.nanoTime()) }
    var score by remember { mutableIntStateOf(0) }
    var hearts by remember { mutableIntStateOf(3) }
    var from by remember { mutableIntStateOf(67) }
    var to by remember { mutableIntStateOf(72) }
    var clouds by remember { mutableStateOf(listOf<Cloud>()) }
    var picked by remember { mutableStateOf<Cloud?>(null) }
    var good by remember { mutableStateOf<Boolean?>(null) }
    var busy by remember { mutableStateOf(true) }
    var say by remember { mutableStateOf("Listen to my two notes!") }
    var mood by remember { mutableStateOf(LouMood.Happy) }
    var round by remember { mutableIntStateOf(0) }
    var sung by remember { mutableStateOf(listOf<Sung>()) }
    var bursts by remember { mutableStateOf(listOf<Burst>()) }
    var now by remember { mutableLongStateOf(0L) }
    val appear = remember { Animatable(0f) }      // new clouds gliding in
    val crouch = remember { Animatable(0f) }      // wind-up squash before the jump
    val jump = remember { Animatable(0f) }        // 0 = home, 1 = on the picked cloud
    val land = remember { Animatable(0f) }        // landing squash and cloud dip
    val pan = remember { Animatable(0f) }         // camera follows Lou to his new cloud
    val fall = remember { Animatable(0f) }        // wrong cloud: drop, then come back home
    val poof = remember { Animatable(0f) }        // wrong cloud puffing apart
    LaunchedEffect(Unit) { gameLoop(control) { t, _ -> now = t; if (bursts.isNotEmpty()) bursts = bursts.filter { t - it.born < 650 }; if (sung.isNotEmpty()) sung = sung.filter { t - it.born < 1400 } } }
    DisposableEffect(Unit) { onDispose { engine.quietSfx = false } }

    fun hard() = score >= 6
    fun newRound() {
        val opts = if (!hard()) listOf(Cloud(1, "Higher", 0), Cloud(0, "Same", 1), Cloud(-1, "Lower", 2))
                   else listOf(Cloud(2, "Leap up", 0), Cloud(1, "Step up", 1), Cloud(-1, "Step down", 2), Cloud(-2, "Leap down", 3))
        val pick = opts.random(rnd)
        val size = when (abs(pick.move)) { 0 -> 0; 1 -> if (hard()) 1 + rnd.nextInt(2) else 3 + rnd.nextInt(6); else -> 5 + rnd.nextInt(5) }
        val dir = Integer.signum(pick.move)
        var f = from
        if (f + dir * size !in 55..86) f = if (dir > 0) 60 else 79
        from = f; to = f + dir * size
        clouds = opts; picked = null; good = null; round++
    }
    suspend fun waitIfPaused() { while (control.paused) delay(100) }
    suspend fun playPair(slow: Boolean = false) {
        waitIfPaused()
        try {
            engine.quietSfx = true; mood = LouMood.Listen
            engine.ping(from, if (slow) 0.8f else 0.55f); sung = sung + Sung("♪", NoteColors.of(from), now, false)
            delay(if (slow) 950 else 650); waitIfPaused()
            engine.ping(to, if (slow) 0.8f else 0.55f); sung = sung + Sung("♪?", NoteColors.of(to), now, true)
            delay(600); mood = LouMood.Idle
        } finally { engine.quietSfx = false }
    }
    fun correctCloud() = clouds.first { c -> when { to == from -> c.move == 0; to > from -> c.move > 0 && (!hard() || (abs(to - from) <= 2) == (c.move == 1)); else -> c.move < 0 && (!hard() || (abs(to - from) <= 2) == (c.move == -1)) } }

    LaunchedEffect(round) {
        if (round == 0) { newRound(); return@LaunchedEffect }
        busy = true
        say = if (round == 1) "Listen to my two notes!" else if (hard() && score == 6) "Now: a small step, or a big leap?" else listOf("Listen…", "Where does it go?", "Ears ready!").random(rnd)
        appear.snapTo(0f); delay(150)
        appear.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
        playPair(); busy = false
    }

    fun choose(c: Cloud) {
        if (busy || picked != null || control.paused) return
        busy = true; picked = c
        val right = correctCloud(); val ok = c == right
        scope.launch {
            // wind up, launch, land
            crouch.animateTo(1f, tween(130)); sfx.whoosh()
            launch { crouch.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 900f)) }
            jump.animateTo(1f, tween(540, easing = LinearEasing))
            engine.ping(to, 0.5f)
            if (ok) {
                good = true; score++; sfx.correct(); mood = LouMood.Cheer
                land.snapTo(1f); launch { land.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = 380f)) }
                say = listOf("Yes! It went ${if (to > from) "up" else if (to < from) "down" else "nowhere"}!", "Great ears!", "Hop hop!", "${Basics.name(to)}! Right there.").random(rnd)
                delay(500); waitIfPaused()
                // the camera glides over: this cloud becomes Lou's new home
                pan.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
                from = to; pan.snapTo(0f); jump.snapTo(0f); mood = LouMood.Happy
            } else {
                good = false; hearts--; sfx.miss(); mood = LouMood.Surprised
                say = "Oops! ${right.went}. Listen again."
                launch { poof.snapTo(0f); poof.animateTo(1f, tween(420)) }
                fall.animateTo(1f, tween(480, easing = androidx.compose.animation.core.FastOutLinearInEasing))
                jump.snapTo(0f)
                fall.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 220f))    // drops back onto the home cloud
                mood = LouMood.Encourage
                if (hearts <= 0) { delay(300); waitIfPaused(); control.end(score); return@launch }
                playPair(slow = true); delay(200)
                poof.snapTo(0f)
            }
            newRound()
        }
    }

    LaunchedEffect(busy, round) { if (GameDebug.autoplay && !busy && picked == null && clouds.isNotEmpty()) { delay(700); choose(correctCloud()) } }
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = constraints.maxWidth.toFloat(); val h = constraints.maxHeight.toFloat()
        val density = androidx.compose.ui.platform.LocalDensity.current.density
        val home = Offset(w * 0.22f, h * 0.56f)
        val cw = minOf(w * 0.38f, 190f * density); val ch = 54f * density
        fun slotPos(c: Cloud): Offset {
            val n = clouds.size; val top = h * 0.30f; val bottom = h * 0.80f
            val y = if (n <= 1) h / 2 else top + (bottom - top) * c.slot / (n - 1).toFloat()
            return Offset(w * 0.73f, y)
        }
        val landed = picked?.let { slotPos(it) } ?: home
        val camera = (landed - home) * pan.value                       // the world slides so the landed cloud ends where home was
        fun cloudPos(c: Cloud): Offset { val p = slotPos(c); val glide = (1f - appear.value) * w * 0.45f; return Offset(p.x + glide, p.y) - camera }
        Canvas(Modifier.fillMaxSize().pointerInput(clouds, busy) {
            detectTapGestures(onPress = { o -> clouds.minByOrNull { (cloudPos(it) - o).getDistance() }?.takeIf { (cloudPos(it) - o).getDistance() < cw * 0.7f }?.let { choose(it) } })
        }) {
            // drifting background clouds, slower the farther away (parallax), and they move with the camera a little
            for (i in 0 until 5) {
                val x = ((i * 0.27f + now / 60000f * (0.5f + i * 0.1f)) % 1.2f) * w - w * 0.1f - camera.x * 0.2f
                drawCloud(Offset(x, h * (0.18f + i * 0.15f) - camera.y * 0.2f), 90f + i * 14f, 30f + i * 4f, Color(0x55FFFFFF))
            }
            // home cloud (slides away as the camera moves)
            drawCloud(home + Offset(0f, 18f * density) - camera, cw * 0.95f, ch, Color.White.copy(alpha = (1f - pan.value * 1.4f).coerceIn(0f, 1f)))
            clouds.forEach { c ->
                val p = cloudPos(c)
                val isPicked = picked == c
                val dip = if (isPicked && good == true) land.value * 10f * density else 0f
                val col = when { isPicked && good == true -> NoteColors.of(to); good == false && c == correctCloud() -> Color(0xFFB9F0A0); else -> Color.White }
                val alpha = when { pan.value > 0f && !isPicked -> (1f - pan.value * 2f).coerceIn(0f, 1f); isPicked && good == false -> (1f - poof.value).coerceIn(0f, 1f); else -> appear.value.coerceIn(0f, 1f) }
                val bob = sin(now / 900f + c.slot) * 3f * density
                val scale = if (isPicked && good == false) 1f + poof.value * 0.5f else 1f
                drawCloud(Offset(p.x, p.y + dip + bob), cw * scale, ch * scale, col.copy(alpha = alpha))
                if (isPicked && good == false) for (i in 0 until 6) {      // puffs flying off the cloud
                    val a = i / 6f * 6.28f; val r = poof.value * 70f * density
                    drawCircle(Color.White.copy(alpha = 1f - poof.value), (14f - poof.value * 8f) * density, Offset(p.x + kotlin.math.cos(a) * r, p.y + sin(a) * r * 0.5f))
                }
                val label = if (isPicked && good == true) "${Basics.name(to)}!" else if (good == false && c == correctCloud()) c.went else c.label
                val l = measurer.measure(label, TextStyle(fontFamily = OpenSans, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp,
                    color = (if (isPicked && good == true) NoteColors.ink(to) else Color(0xFF0A548F)).copy(alpha = alpha)))
                drawText(l, topLeft = Offset(p.x - l.size.width / 2f, p.y + dip + bob - l.size.height / 2f - 4f * density))
            }
            // sung notes: the first rises from Lou, the second floats over toward the clouds with a question mark
            sung.forEach { s ->
                val k = ((now - s.born) / 1400f).coerceIn(0f, 1f)
                val start = home + Offset(0f, -60f * density)
                val end = if (s.toClouds) Offset(w * 0.55f, h * 0.42f) else start + Offset(-20f * density, -90f * density)
                val pos = start + (end - start) * easeOutCubic(k) + Offset(sin(k * 9f) * 8f * density, 0f)
                val l = measurer.measure(s.text, TextStyle(fontFamily = OpenSans, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, color = s.color.copy(alpha = 1f - k * k)))
                drawText(l, topLeft = Offset(pos.x - l.size.width / 2f, pos.y - l.size.height / 2f))
            }
            drawBursts(bursts, now)
        }
        // one invisible button per cloud, so TalkBack can pick a cloud by name
        clouds.forEach { c ->
            val p = cloudPos(c)
            Box(Modifier.offset { IntOffset((p.x - cw / 2).toInt(), (p.y - ch / 2).toInt()) }.size((cw / density).dp, (ch / density).dp)
                .semantics { contentDescription = "Cloud: ${c.label}"; role = Role.Button }
                .pointerInput(c, busy) { detectTapGestures(onPress = { choose(c) }) })
        }
        // Lou: crouch, arc to the cloud leaning forward, squash on landing; on a miss he drops and bounces back home
        val p = jump.value
        val target = landed
        val x = home.x + (target.x - home.x) * p - camera.x
        val arcH = h * 0.2f + abs(target.y - home.y) * 0.3f
        val dropY = fall.value * h * 0.55f
        val y = home.y + (target.y - home.y) * p - sin(p * Math.PI).toFloat() * arcH - camera.y + dropY + (if (picked != null && good == true) land.value * 10f * density else 0f)
        val squash = crouch.value * 0.2f + land.value * 0.14f - (if (p in 0.05f..0.5f) 0.08f else 0f)
        val lean = if (p in 0.01f..0.99f) 10f * (if (target.x > home.x) 1f else -1f) else 0f
        val gazeAt = if (picked != null) Offset(0.6f, (target.y - home.y) / h * 3f) else Offset(0.8f, 0f)
        LouActor(x, y + 12f * density, 96.dp, mood, squash = squash, lean = lean + fall.value * 25f, look = Offset(gazeAt.x, gazeAt.y.coerceIn(-1f, 1f)))
        LaunchedEffect(good) { if (good == true) bursts = bursts + Burst(slotPos(picked!!), NoteColors.of(to), now) }
        LouBubble(say, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 60.dp, start = 16.dp, end = 16.dp))
        GelButton("↻  Hear again", { if (!busy) scope.launch { busy = true; playPair(); busy = false } }, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp).width(200.dp), gel = Gel.Silver, height = 42.dp)
    }
    GameHud(score, hearts, 3, if (hard()) "Steps & leaps" else "Up or down")
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCloud(c: Offset, w: Float, h: Float, color: Color) {
    if (color.alpha <= 0.01f) return
    val shade = Color(0xFFDCEBF5).copy(alpha = color.alpha)
    drawOval(Color(0xFF083A5A).copy(alpha = 0.10f * color.alpha), Offset(c.x - w / 2, c.y + h * 0.25f), Size(w, h * 0.45f))
    drawOval(shade, Offset(c.x - w / 2, c.y - h * 0.25f), Size(w, h * 0.7f))
    drawCircle(color, h * 0.42f, Offset(c.x - w * 0.22f, c.y - h * 0.1f))
    drawCircle(color, h * 0.55f, Offset(c.x + w * 0.05f, c.y - h * 0.28f))
    drawCircle(color, h * 0.38f, Offset(c.x + w * 0.28f, c.y - h * 0.05f))
    drawOval(Brush.verticalGradient(listOf(color, shade), c.y - h * 0.3f, c.y + h * 0.4f), Offset(c.x - w / 2, c.y - h * 0.3f), Size(w, h * 0.7f))
}
