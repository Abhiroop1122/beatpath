package com.beatpath.app.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.beatpath.app.lessons.Basics
import com.beatpath.app.model.NoteColors
import com.beatpath.app.ui.aero.LouBubble
import com.beatpath.app.ui.aero.isLandscape
import com.beatpath.app.ui.lesson.RainbowKeys
import com.beatpath.app.ui.lou.Lou
import com.beatpath.app.ui.lou.LouMood
import com.beatpath.app.ui.theme.OpenSans
import kotlin.math.sin
import kotlin.random.Random

private class Balloon(val note: Int, val x: Float, val born: Long, val riseMs: Long, val sway: Float) { var popped = -1L; var lost = false }
private class Dart(val from: Offset, val to: Offset, val born: Long)

/**
 * Balloon Pop: balloons rise with a note name. Tap that key on the rainbow keyboard before the balloon floats away;
 * Lou throws a dart and it pops with its note. The helpers fade as you get better: first the key letters go,
 * then the rainbow colors, then sharps and flats join in. Five hearts.
 */
@Composable
fun BoxScope.BalloonPop(control: GameControl, engine: AudioEngine, sfx: Sfx) {
    val rnd = remember { Random(System.nanoTime()) }
    var score by remember { mutableIntStateOf(0) }
    var hearts by remember { mutableIntStateOf(5) }
    var now by remember { mutableLongStateOf(0L) }
    var say by remember { mutableStateOf("Pop the balloon: tap its key!") }
    var mood by remember { mutableStateOf(LouMood.Happy) }
    var wrong by remember { mutableStateOf<Int?>(null) }
    var sounding by remember { mutableStateOf(setOf<Int>()) }
    var bursts by remember { mutableStateOf(listOf<Burst>()) }
    var darts by remember { mutableStateOf(listOf<Dart>()) }
    val balloons = remember { mutableListOf<Balloon>() }
    var nextSpawn by remember { mutableLongStateOf(600L) }
    var over by remember { mutableStateOf(false) }
    val onKeyRef = remember { mutableStateOf<(Int) -> Unit>({}) }
    var areaW by remember { mutableStateOf(1f) }; var areaH by remember { mutableStateOf(1f) }

    fun stage() = when { score < 6 -> 1; score < 13 -> 2; score < 20 -> 3; else -> 4 }
    val view = when (stage()) {
        1 -> Basics.KeyView(range = 60..71, letters = true, rainbow = true)
        2 -> Basics.KeyView(range = 60..71, letters = false, rainbow = true)
        else -> Basics.KeyView(range = 60..71, letters = false, rainbow = false)
    }
    fun lose(why: String) {
        hearts--; mood = LouMood.Surprised; say = why; sfx.miss()
        if (hearts <= 0 && !over) { over = true; control.end(score) }
    }

    LaunchedEffect(Unit) {
        gameLoop(control) { t, _ ->
            now = t
            if (t >= nextSpawn && balloons.count { it.popped < 0 && !it.lost } < 4) {
                val pool = if (stage() >= 4) (60..71).toList() else listOf(60, 62, 64, 65, 67, 69, 71)
                val note = pool.random(rnd)
                val rise = (7000L - score * 120L).coerceAtLeast(3600L)
                balloons += Balloon(note, 0.12f + rnd.nextFloat() * 0.76f, t, rise, rnd.nextFloat() * 6.28f)
                nextSpawn = t + (2300L - score * 45L).coerceAtLeast(1100L)
            }
            balloons.filter { it.popped < 0 && !it.lost && t - it.born > it.riseMs }.forEach {
                it.lost = true; lose("${Basics.bothNames(it.note)} flew away! Find it next time.")
            }
            balloons.removeAll { (it.popped >= 0 && t - it.popped > 700) || (it.lost && t - it.born > it.riseMs + 400) }
            if (bursts.isNotEmpty()) bursts = bursts.filter { t - it.born < 650 }
            if (darts.isNotEmpty()) darts = darts.filter { t - it.born < 260 }
            if (t % 900 < 20 && mood != LouMood.Dance) mood = LouMood.Idle
        }
    }

    fun posOf(b: Balloon): Offset {
        val k = ((now - b.born) / b.riseMs.toFloat()).coerceIn(0f, 1.1f)
        return Offset(areaW * b.x + sin(now / 500f + b.sway) * 14f, areaH * (1.02f - k * 1.1f))
    }

    val land = isLandscape()
    Column(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            areaW = constraints.maxWidth.toFloat(); areaH = constraints.maxHeight.toFloat()
            val density = androidx.compose.ui.platform.LocalDensity.current.density
            val measurer = rememberTextMeasurer()
            val louAt = Offset(areaW * 0.08f, areaH * 0.92f)
            Canvas(Modifier.fillMaxSize().semantics { contentDescription = "Balloons: ${balloons.filter { it.popped < 0 && !it.lost }.joinToString { Basics.bothNames(it.note) }.ifEmpty { "none yet" }}" }) {
                balloons.forEach { b ->
                    val p = posOf(b)
                    val col = if (stage() >= 3) Color(0xFFF7F7FB) else NoteColors.of(b.note)
                    if (b.popped >= 0) return@forEach
                    val r = 34f * density
                    // string
                    val sp = Path().apply { moveTo(p.x, p.y + r); quadraticTo(p.x + sin(now / 300f + b.sway) * 10f, p.y + r * 1.6f, p.x, p.y + r * 2.3f) }
                    drawPath(sp, Color(0x88FFFFFF), style = Stroke(2f * density))
                    drawOval(Color(0x22083A5A), Offset(p.x - r * 0.8f, p.y + r * 0.2f), Size(r * 1.8f, r * 1.8f))
                    drawOval(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.9f), col, col.copy(red = col.red * 0.8f, green = col.green * 0.8f, blue = col.blue * 0.8f)),
                        Offset(p.x - r * 0.35f, p.y - r * 0.45f), r * 1.5f), Offset(p.x - r, p.y - r * 1.15f), Size(r * 2f, r * 2.3f))
                    drawOval(Color.White.copy(alpha = 0.55f), Offset(p.x - r * 0.6f, p.y - r * 0.85f), Size(r * 0.5f, r * 0.7f))
                    val l = measurer.measure(Basics.name(b.note), TextStyle(fontFamily = OpenSans, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp,
                        color = if (stage() >= 3) Color(0xFF0E2A3F) else NoteColors.ink(b.note)))
                    drawText(l, topLeft = Offset(p.x - l.size.width / 2f, p.y - l.size.height / 2f))
                }
                darts.forEach { d ->
                    val k = ((now - d.born) / 220f).coerceIn(0f, 1f)
                    val at = Offset(d.from.x + (d.to.x - d.from.x) * k, d.from.y + (d.to.y - d.from.y) * k - sin(k * Math.PI).toFloat() * 40f)
                    drawLine(Color(0xFF0E2A3F), at, at + (d.from - d.to) * (0.08f / ((d.from - d.to).getDistance() / 400f).coerceAtLeast(0.2f)), 4f * density)
                    drawCircle(Color(0xFFEF5B6E), 5f * density, at)
                }
                drawBursts(bursts, now)
            }
            Lou(Modifier.offset { IntOffset((louAt.x - 40 * density).toInt(), (louAt.y - 72 * density).toInt()) }.size(84.dp, 74.dp), mood = mood)
            LouBubble(say, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 64.dp, start = 16.dp, end = 16.dp))
            LaunchedEffect(wrong) { if (wrong != null) { kotlinx.coroutines.delay(450); wrong = null } }
            // key taps come from below: aim the dart from Lou to the balloon
            onKeyRef.value = { k ->
                if (!control.paused && !over) {
                    engine.ping(k, 0.4f)
                    val target = balloons.filter { it.popped < 0 && !it.lost && it.note == k }.minByOrNull { posOf(it).y }
                    if (target != null) {
                        target.popped = now
                        darts = darts + Dart(louAt + Offset(20f * density, -50f * density), posOf(target), now)
                        bursts = bursts + Burst(posOf(target), NoteColors.of(k), now + 200)
                        score++; mood = LouMood.Happy; sfx.correct()
                        say = when (score) { 6 -> "No more letters on the keys. You know them!"; 13 -> "Colors off too. Just the black-key map!"; 20 -> "Now sharps and flats join in!"; else -> "${Basics.bothNames(k)}! Pop!" }
                    } else {
                        wrong = k
                        lose("That's ${Basics.bothNames(k)}. No balloon has that name.")
                    }
                }
            }
        }
        RainbowKeys(view, emptySet(), sounding, emptySet(), wrong, Modifier.fillMaxWidth().height(if (land) 130.dp else 180.dp).navigationBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp)) { k ->
            sounding = sounding + k
            onKeyRef.value(k)
        }
        LaunchedEffect(sounding) { if (sounding.isNotEmpty()) { kotlinx.coroutines.delay(250); sounding = emptySet() } }
    }
    GameHud(score, hearts, 5, when (stage()) { 1 -> "Letters on"; 2 -> "No letters"; 3 -> "No colors"; else -> "Sharps too" })
}
