package com.beatpath.app.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
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
import com.beatpath.app.model.ChannelKind
import com.beatpath.app.model.NoteColors
import com.beatpath.app.model.Pattern
import com.beatpath.app.ui.aero.LouBubble
import com.beatpath.app.ui.lou.LouMood
import com.beatpath.app.ui.theme.OpenSans
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin

/** A block due at [beat] (a whole beat, or x.5 for the "and" between beats). */
private class Block(val beat: Float, val color: Color) { var state = 0; var at = 0L }   // 0 coming, 1 cleared, 2 missed

/** One bar of groove: kick on 1 and 3, clap on 2 and 4, soft eighth-note hats. */
private fun groove(): Pattern {
    var p = Pattern(1)
    p = p.setNotes(ChannelKind.KICK, Pattern.drums(ChannelKind.KICK, 1, 9, velocity = 110))
    p = p.setNotes(ChannelKind.CLAP, Pattern.drums(ChannelKind.CLAP, 5, 13, velocity = 80))
    p = p.setNotes(ChannelKind.HAT, Pattern.drums(ChannelKind.HAT, 1, 3, 5, 7, 9, 11, 13, 15, velocity = 50))
    return p
}

/**
 * Beat Run: Lou runs to a drum groove; blocks arrive exactly on the beat and you tap to jump as each reaches him.
 * Part 1: blocks on beats 1 and 3. Part 2: every beat. Part 3: the "and" between beats joins in. The tempo rises slowly.
 * Everything runs on the audio engine's clock (the beat the speaker is playing), so the blocks always match the drum.
 */
@Composable
fun BoxScope.BeatRun(control: GameControl, engine: AudioEngine, sfx: Sfx) {
    val clock = remember { AudioClock(engine) }
    val pattern = remember { groove() }
    var bpm by remember { mutableIntStateOf(84) }
    var beat by remember { mutableFloatStateOf(-1f) }
    var now by remember { mutableLongStateOf(0L) }
    var score by remember { mutableIntStateOf(0) }
    var hearts by remember { mutableIntStateOf(3) }
    var done by remember { mutableIntStateOf(0) }
    var mood by remember { mutableStateOf(LouMood.Happy) }
    var moodUntil by remember { mutableLongStateOf(0L) }
    var say by remember { mutableStateOf("Listen: 1, 2, 3, 4. Jump on the beat!") }
    var floaters by remember { mutableStateOf(listOf<Floater>()) }
    var bursts by remember { mutableStateOf(listOf<Burst>()) }
    var jumpBeat by remember { mutableFloatStateOf(-99f) }
    var tripAt by remember { mutableLongStateOf(-10_000L) }
    var over by remember { mutableStateOf(false) }
    val blocks = remember { mutableListOf<Block>() }
    var genUntil by remember { mutableFloatStateOf(4f) }      // blocks are made up to this beat; bar 1 is the count-in
    var pausedAt by remember { mutableFloatStateOf(-1f) }
    var lastTempoBar by remember { mutableIntStateOf(0) }
    val tapRef = remember { mutableStateOf({}) }
    DisposableEffect(Unit) { onDispose { clock.stop() } }
    LaunchedEffect(Unit) { clock.start(pattern, bpm, 0f) }

    fun part() = when { score < 10 -> 1; score < 24 -> 2; else -> 3 }
    fun react(m: LouMood, ms: Long) { mood = m; moodUntil = now + ms }
    fun finish() { if (!over) { over = true; clock.stop(); control.end(score) } }

    LaunchedEffect(control.paused) {
        if (control.paused) { pausedAt = beat; clock.stop() }
        else if (pausedAt >= 0) {
            // resume on the next bar with a fresh count-in bar: blocks that were coming are cleared away
            val start = (floor(pausedAt / 4f) + 1) * 4f
            blocks.removeAll { it.state == 0 }; genUntil = start + 4f; pausedAt = -1f
            clock.start(pattern, bpm, start)
        }
    }

    LaunchedEffect(Unit) {
        gameLoop(control) { t, _ ->
            now = t
            val b = clock.beat() ?: return@gameLoop
            beat = b
            // make blocks two bars ahead
            while (genUntil < b + 8f) {
                val gb = genUntil; val inBar = (gb.toInt() % 4)
                val col = NoteColors.of(listOf(0, 2, 4, 5, 7, 9, 11)[gb.toInt() % 7])
                when (part()) {
                    1 -> if (inBar == 0 || inBar == 2) blocks += Block(gb, col)
                    2 -> blocks += Block(gb, col)
                    else -> { blocks += Block(gb, col); if (inBar == 1) blocks += Block(gb + 0.5f, NoteColors.of(9)) }
                }
                genUntil += 1f
            }
            // speed up a little every 4 bars
            val bar = floor(b / 4f).toInt()
            if (bar >= lastTempoBar + 4 && bpm < 120) { lastTempoBar = bar; bpm += 6; clock.tempo(pattern, bpm) }
            // a block that passed Lou without a jump: he trips
            val ms = clock.msPerBeat
            blocks.filter { it.state == 0 && (b - it.beat) * ms > 160 }.forEach { bl ->
                bl.state = 2; bl.at = t; done++; hearts--; tripAt = t; react(LouMood.Surprised, 700); sfx.miss()
                floaters = floaters + Floater("Miss", Color(0xFFEF5B6E), Offset(0f, 0f), t)
                say = if (bl.beat % 1f != 0f) "That one was on the \"and\", halfway between beats." else "Jump right on the beat. Count along!"
                if (hearts <= 0) finish()
            }
            if (done >= 56) finish()
            if (t > moodUntil) mood = LouMood.Dance
            if (blocks.size > 40) blocks.removeAll { it.state != 0 && b - it.beat > 3f }
            if (floaters.isNotEmpty()) floaters = floaters.filter { t - it.born < 950 }
            if (bursts.isNotEmpty()) bursts = bursts.filter { t - it.born < 650 }
            if (GameDebug.autoplay && blocks.any { it.state == 0 && b >= it.beat - 0.02f }) tapRef.value()
        }
    }

    fun tap() {
        if (control.paused || over) return
        val b = clock.beat() ?: return
        val ms = clock.msPerBeat
        if (b - jumpBeat < 0.25f) return            // still in the air from the last jump
        jumpBeat = b; sfx.whoosh()
        val bl = blocks.filter { it.state == 0 }.minByOrNull { abs(it.beat - b) } ?: return
        val off = abs(bl.beat - b) * ms
        when {
            off <= 150 -> {
                bl.state = 1; bl.at = now; done++
                val grade = if (off <= 70) 2 else 1; score += grade
                floaters = floaters + Floater(if (grade == 2) "Perfect!" else "Good", if (grade == 2) Color(0xFFF2852B) else Color(0xFF2E9BE0), Offset(0f, 0f), now)
                bursts = bursts + Burst(Offset(-1f, -1f), bl.color, now)
                if (grade == 2) react(LouMood.Cheer, 450)
                say = when (part()) { 1 -> "Beats 1 and 3!"; 2 -> "Every beat now!"; else -> "And the \"and\" too!" }
                if (done >= 56) finish()
            }
            bl.beat > b && off < 450 -> {
                // jumped too early: comes down right onto the block
                bl.state = 2; bl.at = now; done++; hearts--; tripAt = now; react(LouMood.Surprised, 700); sfx.miss()
                floaters = floaters + Floater("Too early", Color(0xFFEF5B6E), Offset(0f, 0f), now)
                say = "Wait for the beat, then jump!"
                if (hearts <= 0) finish()
            }
        }
    }

    tapRef.value = { tap() }
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onPress = { tap() }) }
        .semantics { contentDescription = "Beat Run. Tap anywhere to jump on the beat." }) {
        val w = constraints.maxWidth.toFloat(); val h = constraints.maxHeight.toFloat()
        val d = androidx.compose.ui.platform.LocalDensity.current.density
        val ground = h * 0.72f; val louX = w * 0.24f
        val pxPerBeat = w * 0.30f
        // the take-off spot sits a quarter beat ahead of Lou: a block on the beat touches it as he jumps,
        // and slides under him exactly at the top of his half-beat jump
        val takeoffX = louX + pxPerBeat * 0.25f
        val b = beat
        val phase = if (b >= 0) b % 1f else 0f
        Canvas(Modifier.fillMaxSize()) {
            val scroll = if (b >= 0) b * pxPerBeat else 0f
            // far hills (slow), near hills (faster): depth
            for (i in -1..4) {
                val x = ((i * w * 0.45f - scroll * 0.12f) % (w * 1.8f) + w * 1.8f) % (w * 1.8f) - w * 0.35f
                drawOval(Color(0x3357B83A), Offset(x, ground - h * 0.14f), Size(w * 0.7f, h * 0.28f))
            }
            for (i in -1..5) {
                val x = ((i * w * 0.33f - scroll * 0.35f) % (w * 1.65f) + w * 1.65f) % (w * 1.65f) - w * 0.25f
                drawOval(Color(0x4462B826), Offset(x, ground - h * 0.07f), Size(w * 0.42f, h * 0.16f))
            }
            drawRect(Brush.verticalGradient(listOf(Color(0xFF8BD650), Color(0xFF5DA82E)), ground, h), Offset(0f, ground), Size(w, h - ground))
            // road stripes move one beat of distance per beat: the ground keeps time too
            val stripe = pxPerBeat / 2f
            var sx = -(scroll % (stripe * 2))
            while (sx < w) { drawRoundRect(Color(0x40FFFFFF), Offset(sx, ground + 20f * d), Size(stripe, 7f * d), CornerRadius(7f)); sx += stripe * 2 }
            // the take-off spot: a ring that pulses on every beat
            val pulse = 1f - phase
            drawOval(Color.White.copy(alpha = 0.25f + pulse * 0.4f), Offset(takeoffX - 30f * d, ground - 6f * d), Size(60f * d, 14f * d), style = Stroke((2f + pulse * 2.5f) * d))
            // blocks
            var cue = false
            blocks.forEach { bl ->
                val x = takeoffX + (bl.beat - b) * pxPerBeat
                val s = (if (bl.beat % 1f != 0f) 30f else 40f) * d
                if (bl.state == 1) {
                    // kicked away: flies up and spins off
                    val k = ((now - bl.at) / 600f).coerceIn(0f, 1f); if (k >= 1f) return@forEach
                    // Lou taps it with his feet at the top of the jump and it spins off ahead
                    val fx = louX + pxPerBeat * 0.25f * (1f - k) + k * w * 0.45f; val fy = ground - s - sin(k * Math.PI).toFloat() * h * 0.2f - k * h * 0.1f
                    rotate(k * 400f, Offset(fx, fy + s / 2)) { drawRoundRect(bl.color.copy(alpha = 1f - k), Offset(fx - s / 2, fy), Size(s, s), CornerRadius(9f)) }
                    return@forEach
                }
                if (x < -80 || x > w + 80) return@forEach
                if (bl.state == 0 && bl.beat - b in 0f..0.35f && done < 4) cue = true
                drawRoundRect(Color(0x22083A5A), Offset(x - s / 2, ground - 4f), Size(s, 10f), CornerRadius(5f))
                val c = if (bl.state == 2) lerp(bl.color, Color.Gray, 0.6f) else bl.color
                drawRoundRect(lerp(c, Color.Black, 0.25f), Offset(x - s / 2, ground - s), Size(s, s), CornerRadius(9f))
                drawRoundRect(Brush.verticalGradient(listOf(lerp(c, Color.White, 0.45f), c), ground - s, ground - s * 0.3f), Offset(x - s / 2, ground - s - 4f), Size(s, s - 4f), CornerRadius(9f))
            }
            // the band's count-in: 1, 2, 3, 4 before the first block
            if (b in 0f..4f) {
                val l = measurer.measure("${b.toInt() + 1}", TextStyle(fontFamily = OpenSans, fontWeight = FontWeight.ExtraBold, fontSize = 96.sp, color = Color(0xFF0A548F).copy(alpha = 1f - phase * 0.7f)))
                drawText(l, topLeft = Offset(w / 2 - l.size.width / 2f, maxOf(h * 0.36f, 180f * d)))
            }
            if (cue) {
                val l = measurer.measure("TAP!", TextStyle(fontFamily = OpenSans, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = Color(0xFFF2852B)))
                drawText(l, topLeft = Offset(takeoffX - l.size.width / 2f, ground - 150f * d))
            }
            // beat lights 1 2 3 4, from the audio clock
            val inBar = if (b >= 0) floor(b).toInt() % 4 else -1
            for (i in 0 until 4) {
                val wide = w > h
                val cx = (if (wide) w * 0.80f else w / 2) + (i - 1.5f) * 46f * d; val cy = if (wide) h * 0.36f else maxOf(h * 0.26f, 150f * d)
                val on = inBar == i
                val r = (if (on) 19f + (1f - phase) * 4f else 15f) * d
                drawCircle(if (on) Color(0xFFF2852B) else Color(0x99FFFFFF), r, Offset(cx, cy))
                val l = measurer.measure("${i + 1}", TextStyle(fontFamily = OpenSans, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = if (on) Color.White else Color(0xFF0A548F)))
                drawText(l, topLeft = Offset(cx - l.size.width / 2f, cy - l.size.height / 2f))
            }
            // feedback words above Lou, sparkles where the block was cleared
            val wordY = if (w > h) ground - 150f * d else maxOf(ground - 175f * d, 190f * d)
            drawFloaters(floaters.map { Floater(it.text, it.color, Offset(louX + 60f * d, wordY), it.born, big = true) }, now, measurer)
            drawBursts(bursts.map { Burst(Offset(louX + 30f * d, ground - 40f * d), it.color, it.born) }, now)
            // dust at each footstep (two steps per beat)
            if (b >= 0 && b - jumpBeat > 0.5f) {
                val step = (b * 2f) % 1f
                if (step < 0.4f) { val k = step / 0.4f; drawCircle(Color.White.copy(alpha = 0.45f * (1f - k)), (5f + k * 10f) * d, Offset(louX - 30f * d - k * 26f * d, ground - 4f * d)) }
            }
            // stars around Lou's head after a trip
            val tk = ((now - tripAt) / 800f)
            if (tk in 0f..1f) for (i in 0 until 3) {
                val a = tk * 9f + i * 2.09f
                drawCircle(Color(0xFFFFD54F).copy(alpha = 1f - tk), 5f * d, Offset(louX + kotlin.math.cos(a) * 30f * d, ground - 100f * d + sin(a) * 9f * d))
            }
        }
        // Lou: two bouncy steps per beat, leaning into the run; a jump arc of half a beat; a stumble on a miss
        val jp = if (b >= 0) ((b - jumpBeat) / 0.5f) else 2f
        val inAir = jp in 0f..1f
        val jumpY = if (inAir) sin(jp * Math.PI).toFloat() * 112f * d else 0f
        val step = if (b >= 0) (b * 2f) % 1f else 0f
        val bob = if (!inAir) abs(sin(step * Math.PI)).toFloat() * 7f * d else 0f
        val landing = if (b >= 0) (b - jumpBeat - 0.5f) else 1f
        val squash = when { inAir && jp < 0.15f -> -0.1f; landing in 0f..0.15f -> 0.12f * (1f - landing / 0.15f); !inAir && step < 0.12f -> 0.05f; else -> 0f }
        val trip = ((now - tripAt) / 520f)
        val lean = if (trip in 0f..1f) -24f * sin(trip * Math.PI).toFloat() else if (inAir) -6f else 7f
        LouActor(louX, ground - jumpY - bob, 92.dp, mood, squash = squash, lean = lean, look = Offset(0.9f, 0.2f), groundY = ground,
            beatPhase = if (b >= 0) phase else null)
        LouBubble(say, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 60.dp, start = 16.dp, end = 16.dp))
    }
    GameHud(score, hearts, 3, "$bpm BPM")
}
