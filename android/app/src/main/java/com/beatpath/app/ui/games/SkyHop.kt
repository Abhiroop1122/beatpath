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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
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
import com.beatpath.app.ui.lou.Lou
import com.beatpath.app.ui.lou.LouMood
import com.beatpath.app.ui.theme.OpenSans
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

/** One cloud to land on: how far the note moves (half steps) and its label. */
private data class Cloud(val move: Int, val label: String, val slot: Int)

/**
 * Sky Hop: Lou stands on a cloud and plays two notes. Tap the cloud where the second note went: up, down or the same.
 * After 6 points the clouds split into small steps and big leaps. Three hearts.
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
    val jump = remember { Animatable(0f) }
    val appear = remember { Animatable(0f) }
    var bursts by remember { mutableStateOf(listOf<Burst>()) }
    var now by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) { gameLoop(control) { t, _ -> now = t; if (bursts.isNotEmpty()) bursts = bursts.filter { t - it.born < 650 } } }

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
            engine.ping(from, if (slow) 0.8f else 0.55f); delay(if (slow) 950 else 650); waitIfPaused(); engine.ping(to, if (slow) 0.8f else 0.55f); delay(600)
            mood = LouMood.Idle
        } finally { engine.quietSfx = false }
    }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { engine.quietSfx = false } }
    fun correctCloud() = clouds.first { c -> when { to == from -> c.move == 0; to > from -> c.move > 0 && (!hard() || (abs(to - from) <= 2) == (c.move == 1)); else -> c.move < 0 && (!hard() || (abs(to - from) <= 2) == (c.move == -1)) } }

    LaunchedEffect(round) {
        if (round == 0) { newRound(); return@LaunchedEffect }
        busy = true; appear.snapTo(0f); jump.snapTo(0f)
        say = if (round == 1) "Listen to my two notes!" else if (hard() && score == 6) "Now: a small step or a big leap?" else listOf("Listen…", "Where does it go?", "Ears ready!").random(rnd)
        delay(350); appear.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 260f))
        playPair(); busy = false
    }

    fun choose(c: Cloud) {
        if (busy || picked != null || control.paused) return
        busy = true; picked = c
        val right = correctCloud()
        val ok = c == right
        scope.launch {
            mood = LouMood.Happy
            jump.animateTo(1f, tween(560, easing = LinearEasing))
            engine.ping(to, 0.5f)
            good = ok
            if (ok) {
                score++; sfx.correct(); mood = LouMood.Cheer
                say = listOf("Yes! It went ${if (to > from) "up" else if (to < from) "down" else "nowhere"}!", "Great ears!", "Hop hop!").random(rnd)
            } else {
                hearts--; sfx.miss(); mood = LouMood.Surprised
                say = "Oops! It was ${right.label.lowercase()}. Listen again."
            }
            delay(if (ok) 750 else 400)
            if (!ok) { playPair(slow = true); delay(200) }
            if (hearts <= 0) { delay(300); waitIfPaused(); control.end(score); return@launch }
            from = to; jump.snapTo(0f); newRound()
        }
    }

    val measurer = rememberTextMeasurer()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = constraints.maxWidth.toFloat(); val h = constraints.maxHeight.toFloat()
        val density = androidx.compose.ui.platform.LocalDensity.current.density
        val louW = 96f * density; val louH = 84f * density
        val home = Offset(w * 0.2f, h * 0.55f)
        fun cloudPos(c: Cloud): Offset {
            val n = clouds.size
            val top = h * 0.24f; val bottom = h * 0.80f
            val y = if (n <= 1) h / 2 else top + (bottom - top) * c.slot / (n - 1).toFloat()
            return Offset(w * 0.72f, y)
        }
        val cw = minOf(w * 0.4f, 190f * density); val ch = 52f * density
        Canvas(Modifier.fillMaxSize().pointerInput(clouds, busy) {
            detectTapGestures { o -> clouds.minByOrNull { (cloudPos(it) - o).getDistance() }?.takeIf { (cloudPos(it) - o).getDistance() < cw }?.let { choose(it) } }
        }.semantics { contentDescription = "Clouds: ${clouds.joinToString { it.label }}. Tap where the second note went." }) {
            // drifting background clouds (parallax)
            for (i in 0 until 5) {
                val x = ((i * 0.27f + now / 60000f * (0.5f + i * 0.1f)) % 1.2f) * w - w * 0.1f
                drawCloud(Offset(x, h * (0.12f + i * 0.17f)), 90f + i * 14f, 30f + i * 4f, Color(0x55FFFFFF))
            }
            drawCloud(home + Offset(0f, louH * 0.45f), cw * 0.9f, ch, Color.White)
            clouds.forEach { c ->
                val p = cloudPos(c); val k = appear.value
                val isPicked = picked == c
                val col = when { isPicked && good == true -> NoteColors.of(to); isPicked && good == false -> Color(0xFFFFD9A0); good == false && c == correctCloud() -> Color(0xFFB9F0A0); else -> Color.White }
                drawCloud(Offset(p.x + (1f - k) * w * 0.3f, p.y), cw * (0.85f + 0.15f * k), ch, col.copy(alpha = k))
                val label = if (isPicked && good == true) "${Basics.name(to)}!" else c.label
                val l = measurer.measure(label, TextStyle(fontFamily = OpenSans, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Color(0xFF0A548F)))
                drawText(l, topLeft = Offset(p.x + (1f - k) * w * 0.3f - l.size.width / 2f, p.y - l.size.height / 2f), alpha = k)
            }
            drawBursts(bursts, now)
        }
        // Lou jumps along an arc from home to the picked cloud
        val target = picked?.let { cloudPos(it) } ?: home
        val p = jump.value
        val x = home.x + (target.x - home.x) * p
        val y = home.y + (target.y - home.y) * p - kotlin.math.sin(p * Math.PI).toFloat() * h * 0.18f
        val squash = if (p > 0.92f || p < 0.06f) 0.9f else 1f
        Lou(Modifier.offset { IntOffset((x - louW / 2).toInt(), (y - louH * 0.9f).toInt()) }.size((louW / density).dp, (louH / density).dp)
            .graphicsLayer { scaleY = squash; scaleX = 2f - squash; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f) }, mood = mood)
        // one invisible button per cloud, so TalkBack (and Switch Access) can pick a cloud by name
        clouds.forEach { c ->
            val p = cloudPos(c)
            Box(Modifier.offset { IntOffset((p.x - cw / 2).toInt(), (p.y - ch / 2).toInt()) }.size((cw / density).dp, (ch / density).dp)
                .semantics { contentDescription = "Cloud: ${c.label}"; role = androidx.compose.ui.semantics.Role.Button }
                .pointerInput(c, busy) { detectTapGestures { choose(c) } })
        }
        LouBubble(say, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 64.dp, start = 16.dp, end = 16.dp))
        LaunchedEffect(good) { if (good == true) bursts = bursts + Burst(cloudPos(picked!!), NoteColors.of(to), now) }
        GelButton("↻  Hear again", { if (!busy) scope.launch { busy = true; playPair(); busy = false } }, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp).width(200.dp), gel = Gel.Silver, height = 42.dp)
    }
    GameHud(score, hearts, 3, if (hard()) "Steps & leaps" else "Up or down")
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCloud(c: Offset, w: Float, h: Float, color: Color) {
    if (color.alpha <= 0.01f) return
    val shade = Color(0xFFDCEBF5).copy(alpha = color.alpha)
    drawOval(Color(0x22083A5A).copy(alpha = 0.13f * color.alpha), Offset(c.x - w / 2, c.y + h * 0.25f), Size(w, h * 0.45f))
    drawOval(shade, Offset(c.x - w / 2, c.y - h * 0.25f), Size(w, h * 0.7f))
    drawCircle(color, h * 0.42f, Offset(c.x - w * 0.22f, c.y - h * 0.1f))
    drawCircle(color, h * 0.55f, Offset(c.x + w * 0.05f, c.y - h * 0.28f))
    drawCircle(color, h * 0.38f, Offset(c.x + w * 0.28f, c.y - h * 0.05f))
    drawOval(Brush.verticalGradient(listOf(color, shade), c.y - h * 0.3f, c.y + h * 0.4f), Offset(c.x - w / 2, c.y - h * 0.3f), Size(w, h * 0.7f))
}
