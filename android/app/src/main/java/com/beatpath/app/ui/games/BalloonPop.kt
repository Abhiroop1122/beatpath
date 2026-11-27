package com.beatpath.app.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatpath.app.audio.AudioEngine
import com.beatpath.app.audio.Sfx
import com.beatpath.app.lessons.Basics
import com.beatpath.app.model.NoteColors
import com.beatpath.app.ui.aero.LouBubble
import com.beatpath.app.ui.aero.isLandscape
import com.beatpath.app.ui.lesson.RainbowKeys
import com.beatpath.app.ui.lou.LouMood
import com.beatpath.app.ui.theme.OpenSans
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** A balloon: its note, a lane (0..1 across), when it set off, how long its rise takes. [aimed] once a dart is on its way. */
private class Balloon(val note: Int, val x: Float, val born: Long, val riseMs: Long, val sway: Float) {
    var aimed = false; var popped = -1L; var lost = -1L
}
/** A dart: thrown from Lou's hand at [from] at time [born], homing on [target]; it lands after [flightMs]. */
private class Dart(val from: Offset, val target: Balloon, val born: Long, val flightMs: Long)
/** A scrap of rubber flying off a popped balloon. */
private class Scrap(val at: Offset, val vx: Float, val vy: Float, val spin: Float, val color: Color, val born: Long)

/**
 * Balloon Pop: note balloons float up. Touch the matching key and Lou runs under the balloon, winds up and throws a dart;
 * the balloon pops (and sings its note) only when the dart reaches it. The help fades as you improve: first the key
 * letters, then the rainbow colors; then sharps and flats join in. Five hearts: a balloon that floats away or a key
 * with no balloon costs one.
 */
@Composable
fun BoxScope.BalloonPop(control: GameControl, engine: AudioEngine, sfx: Sfx) {
    val rnd = remember { Random(System.nanoTime()) }
    var score by remember { mutableIntStateOf(0) }
    var hearts by remember { mutableIntStateOf(5) }
    var now by remember { mutableLongStateOf(0L) }
    var say by remember { mutableStateOf("Touch a balloon's key and I'll pop it!") }
    var mood by remember { mutableStateOf(LouMood.Happy) }
    var moodUntil by remember { mutableLongStateOf(0L) }
    var wrong by remember { mutableStateOf<Int?>(null) }
    var sounding by remember { mutableStateOf(setOf<Int>()) }
    var bursts by remember { mutableStateOf(listOf<Burst>()) }
    var floaters by remember { mutableStateOf(listOf<Floater>()) }
    var darts by remember { mutableStateOf(listOf<Dart>()) }
    var scraps by remember { mutableStateOf(listOf<Scrap>()) }
    val balloons = remember { mutableListOf<Balloon>() }
    var nextSpawn by remember { mutableLongStateOf(700L) }
    var over by remember { mutableStateOf(false) }
    // Lou's position along the ledge (px), where he's running to, and when he threw
    var louX by remember { mutableFloatStateOf(-1f) }
    var louTargetX by remember { mutableFloatStateOf(-1f) }
    var throwAt by remember { mutableLongStateOf(-10_000L) }
    var area by remember { mutableStateOf(Size(1f, 1f)) }
    val keyRef = remember { mutableStateOf<(Int) -> Unit>({}) }
    val d = androidx.compose.ui.platform.LocalDensity.current.density
    val landNow = isLandscape()
    // balloons escape here: below the HUD and Lou's bubble, never hidden behind them (in landscape the bubble sits to the right)
    val escapeY = (if (landNow) 86f else 186f) * d
    fun ledgeY() = area.height - 14f * d

    fun stage() = when { score < 6 -> 1; score < 13 -> 2; score < 20 -> 3; else -> 4 }
    fun react(m: LouMood, ms: Long) { mood = m; moodUntil = now + ms }
    fun posOf(b: Balloon, t: Long = now): Offset {
        val k = ((t - b.born) / b.riseMs.toFloat()).coerceAtLeast(0f)
        val bottom = area.height + 40f * d; val top = escapeY
        return Offset(area.width * b.x + sin(t / 520f + b.sway) * 12f * d, bottom - (bottom - top) * k)
    }
    fun lose(msg: String) {
        hearts--; react(LouMood.Sad, 800); say = msg; sfx.miss()
        if (hearts <= 0 && !over) { over = true; control.end(score) }
    }
    fun pop(b: Balloon) {
        b.popped = now
        val p = posOf(b); val c = if (stage() >= 3) Color(0xFFF2F4F8) else NoteColors.of(b.note)
        engine.ping(b.note, 0.5f, 0.8f); sfx.pop()
        bursts = bursts + Burst(p, NoteColors.of(b.note), now)
        scraps = scraps + (0 until 6).map { i -> val a = i / 6f * 6.28f + rnd.nextFloat(); Scrap(p, cos(a) * (2f + rnd.nextFloat() * 2f), sin(a) * 2.5f - 2f, rnd.nextFloat() * 20f - 10f, c, now) }
        floaters = floaters + Floater("${Basics.bothNames(b.note)}!", lerp(NoteColors.of(b.note), Color.Black, 0.15f), p, now, big = true)
        score++; react(LouMood.Cheer, 700)
        say = when (score) { 6 -> "Letters are off the keys now. You know them!"; 13 -> "Colors off too! Use the black-key groups."; 20 -> "Sharps and flats are joining in!"
            else -> listOf("Pop!", "Bullseye!", "${Basics.name(b.note)}, got it!", "Nice aim!").random(rnd) }
    }

    LaunchedEffect(Unit) {
        gameLoop(control) { t, _ ->
            now = t
            if (area.width <= 1f) return@gameLoop
            if (louX < 0) { louX = area.width * 0.18f; louTargetX = louX }
            // spawn: never right on top of another balloon
            if (t >= nextSpawn && balloons.count { it.popped < 0 && it.lost < 0 } < 4) {
                val pool = if (stage() >= 4) (60..71).toList() else listOf(60, 62, 64, 65, 67, 69, 71)
                val live = balloons.filter { it.popped < 0 && it.lost < 0 }
                val span = 0.7f
                var x = 0.12f + rnd.nextFloat() * span
                repeat(8) { if (live.any { abs(it.x - x) < 0.14f && (t - it.born) < it.riseMs * 0.4f }) x = 0.12f + rnd.nextFloat() * span }
                val rise = (8200L - score * 140L).coerceAtLeast(4200L)
                balloons += Balloon(pool.random(rnd), x, t, rise, rnd.nextFloat() * 6.28f)
                nextSpawn = t + (2400L - score * 50L).coerceAtLeast(1150L)
            }
            // darts land: that's the moment the balloon pops
            darts.filter { t - it.born >= it.flightMs }.forEach { dt -> if (dt.target.popped < 0 && dt.target.lost < 0) pop(dt.target) }
            darts = darts.filter { t - it.born < it.flightMs }
            // balloons that reach the top float away (one with a dart already flying at it still gets hit)
            balloons.filter { it.popped < 0 && it.lost < 0 && t - it.born > it.riseMs && !it.aimed }.forEach {
                it.lost = t; lose("${Basics.bothNames(it.note)} floated away. Its key was the ${NoteColors.name(it.note)} one.")
            }
            balloons.removeAll { (it.popped >= 0 && t - it.popped > 800) || (it.lost >= 0 && t - it.lost > 900) }
            // Lou runs toward where he wants to throw from (eases in, like a real run that slows to a stop)
            louX += (louTargetX - louX) * 0.22f
            if (t > moodUntil) mood = if (abs(louTargetX - louX) > 6f) LouMood.Happy else LouMood.Idle
            if (bursts.isNotEmpty()) bursts = bursts.filter { t - it.born < 650 }
            if (floaters.isNotEmpty()) floaters = floaters.filter { t - it.born < 950 }
            if (scraps.isNotEmpty()) scraps = scraps.filter { t - it.born < 900 }
            if (GameDebug.autoplay && t % 1300 < 17) balloons.filter { it.popped < 0 && it.lost < 0 && !it.aimed && posOf(it).y < area.height * 0.75f }.maxByOrNull { posOf(it).y }?.let { keyRef.value(it.note) }
        }
    }

    fun onKey(k: Int) {
        if (control.paused || over) return
        engine.ping(k, 0.3f, 0.6f); sounding = sounding + k
        // the lowest balloon with this note that no dart is chasing yet
        val target = balloons.filter { it.popped < 0 && it.lost < 0 && !it.aimed && it.note == k }.maxByOrNull { posOf(it).y }
        if (target == null) { wrong = k; lose("That's ${Basics.bothNames(k)}. No balloon has that name."); return }
        target.aimed = true
        val p = posOf(target)
        louTargetX = p.x.coerceIn(area.width * 0.1f, area.width * 0.9f)
        throwAt = now
        // the dart leaves Lou's raised hand after a short wind-up and flies at a steady speed; far balloons take a bit longer
        val hand = Offset(louTargetX + 22f * d, ledgeY() - 70f * d)
        val flight = (180L + ((hand - p).getDistance() / (2.2f * d)).toLong()).coerceIn(220L, 520L)
        darts = darts + Dart(hand, target, now + 90, flight)
        sfx.whoosh()
    }
    keyRef.value = { onKey(it) }
    LaunchedEffect(wrong) { if (wrong != null) { kotlinx.coroutines.delay(400); wrong = null } }
    LaunchedEffect(sounding) { if (sounding.isNotEmpty()) { kotlinx.coroutines.delay(220); sounding = emptySet() } }

    val land = isLandscape()
    val measurer = rememberTextMeasurer()
    val view = when (stage()) {
        1 -> Basics.KeyView(range = 60..71, letters = true, rainbow = true)
        2 -> Basics.KeyView(range = 60..71, letters = false, rainbow = true)
        else -> Basics.KeyView(range = 60..71, letters = false, rainbow = false)
    }
    val field: @Composable (Modifier) -> Unit = { fm ->
        BoxWithConstraints(fm.clipToBounds()) {
            area = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
            val ledge = ledgeY()
            Canvas(Modifier.fillMaxSize().semantics { contentDescription = "Balloons: ${balloons.filter { it.popped < 0 && it.lost < 0 }.joinToString { Basics.bothNames(it.note) }.ifEmpty { "none yet" }}" }) {
                // the escape line: a dotted line balloons drift past when they're lost
                var x = 0f
                while (x < size.width) { drawCircle(Color.White.copy(alpha = 0.35f), 3f * d, Offset(x, escapeY)); x += 18f * d }
                balloons.forEach { b ->
                    if (b.popped >= 0) return@forEach
                    val p = if (b.lost >= 0) posOf(b, b.lost).let { it.copy(y = it.y - (now - b.lost) * 0.35f) } else posOf(b)
                    val fade = if (b.lost >= 0) (1f - (now - b.lost) / 900f).coerceIn(0f, 1f) else 1f
                    val col = if (stage() >= 3) Color(0xFFF2F4F8) else NoteColors.of(b.note)
                    val r = 32f * d
                    // string, swaying a beat behind the balloon
                    val sp = Path().apply { moveTo(p.x, p.y + r * 1.15f); quadraticTo(p.x + sin(now / 300f + b.sway) * 10f * d, p.y + r * 1.8f, p.x - sin(now / 400f + b.sway) * 4f * d, p.y + r * 2.5f) }
                    drawPath(sp, Color.White.copy(alpha = 0.75f * fade), style = Stroke(1.6f * d))
                    rotate(sin(now / 600f + b.sway) * 6f, p) {
                        drawOval(Brush.radialGradient(listOf(lerp(col, Color.White, 0.55f), col, lerp(col, Color.Black, 0.22f)), Offset(p.x - r * 0.35f, p.y - r * 0.45f), r * 1.5f),
                            Offset(p.x - r, p.y - r * 1.15f), Size(r * 2f, r * 2.3f), alpha = fade)
                        drawOval(Color.White.copy(alpha = 0.55f * fade), Offset(p.x - r * 0.58f, p.y - r * 0.88f), Size(r * 0.42f, r * 0.66f))
                        val knot = Path().apply { moveTo(p.x - 5f * d, p.y + r * 1.2f); lineTo(p.x + 5f * d, p.y + r * 1.2f); lineTo(p.x, p.y + r * 1.08f); close() }
                        drawPath(knot, lerp(col, Color.Black, 0.25f).copy(alpha = fade))
                        val l = measurer.measure(Basics.name(b.note), TextStyle(fontFamily = OpenSans, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp,
                            color = (if (stage() >= 3) Color(0xFF0E2A3F) else NoteColors.ink(b.note)).copy(alpha = fade)))
                        drawText(l, topLeft = Offset(p.x - l.size.width / 2f, p.y - r * 0.12f - l.size.height / 2f))
                    }
                    // a target ring while a dart is on its way
                    if (b.aimed) drawCircle(Color.White.copy(alpha = 0.8f), r * 1.35f, p.copy(y = p.y - r * 0.1f), style = Stroke(2.5f * d))
                }
                // darts arc from Lou's hand to where the balloon is now (they home in)
                darts.forEach { dt ->
                    if (now < dt.born) return@forEach
                    val k = ((now - dt.born) / dt.flightMs.toFloat()).coerceIn(0f, 1f)
                    val to = posOf(dt.target)
                    val at = Offset(dt.from.x + (to.x - dt.from.x) * k, dt.from.y + (to.y - dt.from.y) * k - sin(k * Math.PI).toFloat() * 30f * d)
                    val dir = (to - dt.from).let { it / it.getDistance().coerceAtLeast(1f) }
                    val tail = at - dir * 20f * d
                    drawLine(Color(0xFF6B4F2A), tail, at, 3.5f * d)
                    drawCircle(Color(0xFFB0BEC5), 3.2f * d, at)
                    drawPath(Path().apply { moveTo(tail.x, tail.y); lineTo(tail.x - dir.x * 7f * d - dir.y * 5f * d, tail.y - dir.y * 7f * d + dir.x * 5f * d); lineTo(tail.x - dir.x * 7f * d + dir.y * 5f * d, tail.y - dir.y * 7f * d - dir.x * 5f * d); close() }, Color(0xFFEF5B6E))
                }
                // rubber scraps tumbling down after a pop
                scraps.forEach { s ->
                    val k = (now - s.born) / 1000f
                    val p = Offset(s.at.x + s.vx * k * 60f * d, s.at.y + (s.vy * k + 4.5f * k * k) * 60f * d)
                    rotate(s.spin * k * 40f, p) { drawOval(s.color.copy(alpha = (1f - k / 0.9f).coerceIn(0f, 1f)), Offset(p.x - 6f * d, p.y - 3f * d), Size(12f * d, 6f * d)) }
                }
                drawBursts(bursts, now)
                drawFloaters(floaters, now, measurer)
                // the grassy ledge Lou runs along
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF9BE15A), Color(0xFF62B826)), ledge, size.height), Offset(0f, ledge), Size(size.width, size.height - ledge + 20f),
                    androidx.compose.ui.geometry.CornerRadius(10f * d))
            }
            // Lou leans back to wind up, snaps forward to throw, and keeps his eyes on the lowest balloon
            val sinceThrow = now - throwAt
            val lean = when { sinceThrow < 90 -> -14f * (sinceThrow / 90f); sinceThrow < 260 -> -14f + 26f * ((sinceThrow - 90) / 170f); sinceThrow < 520 -> 12f * (1f - (sinceThrow - 260) / 260f); else -> 0f }
            val running = abs(louTargetX - louX) > 6f
            val hop = if (running) abs(sin(now / 70f)) * 7f * d else 0f
            val focus = balloons.filter { it.popped < 0 && it.lost < 0 }.maxByOrNull { posOf(it).y }
            val gaze = focus?.let { val p = posOf(it); Offset(((p.x - louX) / (area.width * 0.4f)).coerceIn(-1f, 1f), -0.9f) } ?: Offset(0f, -0.4f)
            if (louX >= 0) LouActor(louX, ledge - hop, if (land) 70.dp else 84.dp, mood, squash = if (sinceThrow in 0..120) 0.08f else if (running) -0.03f else 0f,
                lean = lean + if (running) (if (louTargetX > louX) 6f else -6f) else 0f, look = gaze, groundY = ledge)
            if (!land) LouBubble(say, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 60.dp, start = 16.dp, end = 16.dp))
        }
    }
    val keys: @Composable (Modifier) -> Unit = { km -> RainbowKeys(view, emptySet(), sounding, emptySet(), wrong, km) { onKey(it) } }
    if (land) androidx.compose.foundation.layout.Row(Modifier.fillMaxSize()) {
        // landscape: the balloon field on the left at full height; Lou's words and the keyboard on the right
        field(Modifier.weight(0.56f).fillMaxSize())
        Column(Modifier.weight(0.44f).fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(start = 8.dp, end = 12.dp, top = 56.dp, bottom = 8.dp)) {
            LouBubble(say, Modifier.fillMaxWidth())
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            keys(Modifier.fillMaxWidth().height(150.dp))
        }
    } else Column(Modifier.fillMaxSize()) {
        field(Modifier.weight(1f).fillMaxWidth())
        keys(Modifier.fillMaxWidth().height(176.dp).navigationBarsPadding().padding(horizontal = 10.dp, vertical = 6.dp))
    }
    GameHud(score, hearts, 5, when (stage()) { 1 -> "Letters on"; 2 -> "No letters"; 3 -> "No colors"; else -> "Sharps too" })
}
