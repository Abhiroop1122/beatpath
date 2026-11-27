package com.beatpath.app.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
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
import com.beatpath.app.model.ChannelKind
import com.beatpath.app.model.NoteColors
import com.beatpath.app.ui.aero.LouBubble
import com.beatpath.app.ui.lou.Lou
import com.beatpath.app.ui.lou.LouMood
import com.beatpath.app.ui.theme.OpenSans
import kotlin.math.abs
import kotlin.math.sin

/** A block to jump over, due at [at] ms; [half] marks an "and" between beats. */
private class Block(val at: Long, val half: Boolean, val color: Color) { var judged = false; var hit = 0 }

/**
 * Beat Run: Lou runs to a steady drum. Blocks arrive exactly on the beat; tap to jump as one reaches him.
 * Part 1 has blocks on beats 1 and 3, part 2 on every beat, part 3 adds the "and" between beats. Three hearts.
 */
@Composable
fun BoxScope.BeatRun(control: GameControl, engine: AudioEngine, sfx: Sfx) {
    var score by remember { mutableIntStateOf(0) }
    var hearts by remember { mutableIntStateOf(3) }
    var now by remember { mutableLongStateOf(0L) }
    var beatShown by remember { mutableIntStateOf(-1) }
    var say by remember { mutableStateOf("Count with me: 1, 2, 3, 4!") }
    var mood by remember { mutableStateOf(LouMood.Happy) }
    var jumpAt by remember { mutableLongStateOf(-10_000L) }
    var tripAt by remember { mutableLongStateOf(-10_000L) }
    var flash by remember { mutableStateOf<Pair<String, Long>?>(null) }
    var bursts by remember { mutableStateOf(listOf<Burst>()) }
    val blocks = remember { mutableListOf<Block>() }
    // the beat grid: every beat's time in ms, built as we go so the tempo can rise
    val beats = remember { mutableListOf<Long>() }
    var bpm by remember { mutableIntStateOf(84) }
    var nextSound by remember { mutableIntStateOf(0) }
    var over by remember { mutableStateOf(false) }
    var done by remember { mutableIntStateOf(0) }   // blocks judged; the run ends after 56
    val lead = 2000L // the run starts after one bar of count-in

    fun part() = when { score < 10 -> 1; score < 22 -> 2; else -> 3 }
    fun ensureBeats(until: Long) {
        while ((beats.lastOrNull() ?: (lead - 60000L / bpm)) < until) {
            val t = (beats.lastOrNull() ?: (lead - 60000L / bpm)) + 60000L / bpm
            val i = beats.size
            beats += t
            // count-in bar (beats 0..3) has no blocks
            if (i >= 4) {
                val inBar = i % 4
                val p = part()
                val color = NoteColors.of(listOf(0, 2, 4, 5, 7, 9, 11)[i % 7])
                if (p == 1 && (inBar == 0 || inBar == 2)) blocks += Block(t, false, color)
                if (p >= 2) blocks += Block(t, false, color)
                if (p == 3 && inBar == 1) blocks += Block(t + 30000L / bpm, true, NoteColors.of(9))
            }
            if (i > 0 && i % 16 == 0 && bpm < 120) bpm += 6
        }
    }

    LaunchedEffect(Unit) {
        gameLoop(control) { t, _ ->
            now = t
            ensureBeats(t + 4000)
            // play the beat: kick on 1, hat on the others
            while (nextSound < beats.size && beats[nextSound] <= t) {
                engine.playDrum(if (nextSound % 4 == 0) ChannelKind.KICK else ChannelKind.HAT, if (nextSound % 4 == 0) 0.95f else 0.7f)
                beatShown = nextSound % 4; nextSound++
            }
            // blocks that passed Lou without a jump
            blocks.filter { !it.judged && t - it.at > 160 }.forEach { b ->
                b.judged = true; done++; hearts--; tripAt = t; mood = LouMood.Surprised; sfx.miss()
                flash = "Miss" to t; say = if (b.half) "That one was on the \"and\", between beats." else "Jump right on the beat!"
                if (hearts <= 0 && !over) { over = true; control.end(score) }
                if (done >= 56 && !over) { over = true; control.end(score) }
            }
            if (blocks.size > 60) blocks.removeAll { it.judged && t - it.at > 2000 }
            if (bursts.isNotEmpty()) bursts = bursts.filter { t - it.born < 650 }
            if (t - jumpAt > 600 && t - tripAt > 700) mood = LouMood.Dance
        }
    }

    val measurer = rememberTextMeasurer()
    BoxWithConstraints(Modifier.fillMaxSize().pointerInput(Unit) {
        detectTapGestures {
            if (control.paused || over) return@detectTapGestures
            val t = now
            jumpAt = t
            val b = blocks.filter { !it.judged }.minByOrNull { abs(it.at - t) }
            val d = b?.let { abs(it.at - t) } ?: Long.MAX_VALUE
            // a tap with no block near the beat is a jump at the wrong time: the next block is missed.
            // so tapping as fast as you can doesn't win; only tapping on the beat does
            if (b != null && d > 150 && b.at > t && b.at - t < 450) {
                b.judged = true; done++; hearts--; tripAt = t; mood = LouMood.Surprised; sfx.miss()
                flash = "Too early" to t; say = "Wait for the beat, then jump!"
                if (hearts <= 0 && !over) { over = true; control.end(score) }
            } else if (b != null && d <= 150) {
                b.judged = true; done++; b.hit = if (d <= 70) 2 else 1
                if (done >= 56 && !over) { over = true; control.end(score + b.hit) }
                score += b.hit
                flash = (if (b.hit == 2) "Perfect!" else "Good") to t
                if (b.hit == 2) sfx.correct() else sfx.tap()
                say = when (part()) { 1 -> "Beats 1 and 3!"; 2 -> "Every beat now!"; else -> "And the \"and\"!" }
            }
        }
    }.semantics { contentDescription = "Beat Run. Tap anywhere to jump on the beat." }) {
        val w = constraints.maxWidth.toFloat(); val h = constraints.maxHeight.toFloat(); val density = androidx.compose.ui.platform.LocalDensity.current.density
        val ground = h * 0.70f; val louX = w * 0.24f
        val speed = w * 0.55f / (60000f / bpm) // a beat of travel = 55% of the screen
        Canvas(Modifier.fillMaxSize()) {
            // hills scroll slowly, the road fast: depth
            for (i in -1..4) {
                val x = ((i * w * 0.4f - now * speed * 0.15f) % (w * 1.6f) + w * 1.6f) % (w * 1.6f) - w * 0.3f
                drawOval(Color(0x3357B83A), Offset(x, ground - h * 0.12f), Size(w * 0.6f, h * 0.24f))
            }
            drawRect(Brush.verticalGradient(listOf(Color(0xFF8BD650), Color(0xFF5DA82E)), ground, h), Offset(0f, ground), Size(w, h - ground))
            val stripe = 70f * density
            var sx = -((now * speed) % (stripe * 2))
            while (sx < w) { drawRoundRect(Color(0x33FFFFFF), Offset(sx, ground + 18f * density), Size(stripe, 8f * density), CornerRadius(8f)); sx += stripe * 2 }
            // the line where you jump
            drawLine(Color(0x88FFFFFF), Offset(louX, ground - 120f * density), Offset(louX, ground), 3f * density)
            // blocks (clay cubes)
            blocks.forEach { b ->
                if (b.judged && b.hit > 0) return@forEach
                val x = louX + (b.at - now) * speed
                if (x < -80 || x > w + 80) return@forEach
                val s = (if (b.half) 30f else 40f) * density
                drawRoundRect(Color(0x22083A5A), Offset(x - s / 2, ground - 4f), Size(s, 10f), CornerRadius(5f))
                drawRoundRect(b.color.copy(alpha = 0.75f), Offset(x - s / 2, ground - s), Size(s, s), CornerRadius(9f))
                drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), b.color), ground - s, ground - s * 0.3f), Offset(x - s / 2, ground - s - 4f), Size(s, s - 4f), CornerRadius(9f))
            }
            drawBursts(bursts, now)
            // beat counter 1 2 3 4
            for (i in 0 until 4) {
                val cx = w / 2 + (i - 1.5f) * 50f * density; val cy = h * 0.2f
                val on = beatShown == i
                drawCircle(if (on) Color(0xFFF2852B) else Color(0x99FFFFFF), (if (on) 20f else 15f) * density, Offset(cx, cy))
                val l = measurer.measure("${i + 1}", TextStyle(fontFamily = OpenSans, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = if (on) Color.White else Color(0xFF0A548F)))
                drawText(l, topLeft = Offset(cx - l.size.width / 2f, cy - l.size.height / 2f))
            }
            flash?.let { (text, at) ->
                val k = ((now - at) / 700f).coerceIn(0f, 1f)
                if (k < 1f) {
                    val l = measurer.measure(text, TextStyle(fontFamily = OpenSans, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp,
                        color = (if (text == "Miss") Color(0xFFEF5B6E) else if (text == "Good") Color(0xFF2E9BE0) else Color(0xFFF2852B)).copy(alpha = 1f - k)))
                    drawText(l, topLeft = Offset(louX - l.size.width / 2f, ground - 190f * density - k * 30f * density))
                }
            }
        }
        LaunchedEffect(flash) { flash?.let { (text, at) -> if (text != "Miss") bursts = bursts + Burst(Offset(louX, ground - 60f * density), Color(0xFFFFC93C), at) } }
        // Lou: a running bob on every beat, a jump arc when you tap, a stumble on a miss
        val jp = ((now - jumpAt) / 380f).coerceIn(0f, 1f)
        val jumpY = if (jp < 1f) sin(jp * Math.PI).toFloat() * 110f * density else 0f
        val bob = abs(sin(now / (60000f / bpm) * Math.PI).toFloat()) * 6f * density
        val trip = ((now - tripAt) / 500f).coerceIn(0f, 1f)
        val louW = 92f * density; val louH = 80f * density
        Lou(Modifier.offset { IntOffset((louX - louW / 2).toInt(), (ground - louH - jumpY - bob).toInt()) }.size((louW / density).dp, (louH / density).dp)
            .graphicsLayer { rotationZ = if (trip < 1f) sin(trip * Math.PI).toFloat() * -18f else if (jp < 1f) -8f * sin(jp * Math.PI).toFloat() else 0f },
            mood = mood, beatPhase = if (beatShown >= 0) ((now - (beats.getOrNull(nextSound - 1) ?: 0L)) / (60000f / bpm)).coerceIn(0f, 1f) else null)
        LouBubble(say, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 64.dp, start = 16.dp, end = 16.dp))
    }
    GameHud(score, hearts, 3, "$bpm BPM")
}
