package com.beatpath.app.ui.games

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
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
import com.beatpath.app.ui.aero.ClayChip
import com.beatpath.app.ui.aero.Gel
import com.beatpath.app.ui.aero.LouBubble
import com.beatpath.app.ui.aero.clayCard
import com.beatpath.app.ui.aero.isLandscape
import com.beatpath.app.ui.lesson.RainbowKeys
import com.beatpath.app.ui.lou.Lou
import com.beatpath.app.ui.lou.LouMood
import com.beatpath.app.ui.lou.LouLook
import com.beatpath.app.ui.theme.AeroType
import com.beatpath.app.ui.theme.OpenSans
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

/** A cake order: the chord's root (MIDI) and whether it is minor. */
private data class Order(val root: Int, val minor: Boolean) {
    val name get() = "${Basics.name(root)} ${if (minor) "minor" else "major"}"
    val notes get() = listOf(root, root + if (minor) 3 else 4, root + 7)
}

/**
 * Chord Cake: Lou is a chef. Each order names a chord; tap three keys to stack it, bottom note first.
 * Each note is a cake layer in its rainbow color. Right chords get a candle and Lou plays them; wrong ones wobble and slide apart.
 * 90 seconds on the clock; a wrong cake costs 5 seconds.
 */
@Composable
fun BoxScope.ChordCake(control: GameControl, engine: AudioEngine, sfx: Sfx) {
    val scope = rememberCoroutineScope()
    val rnd = remember { Random(System.nanoTime()) }
    var score by remember { mutableIntStateOf(0) }
    var left by remember { mutableLongStateOf(90_000L) }
    var order by remember { mutableStateOf(Order(60, false)) }
    var layers by remember { mutableStateOf(listOf<Int>()) }
    var say by remember { mutableStateOf("Order up! C major. Start on C, then take, skip, take.") }
    var mood by remember { mutableStateOf(LouMood.Happy) }
    var busy by remember { mutableStateOf(false) }
    var candle by remember { mutableStateOf(false) }
    var sounding by remember { mutableStateOf(setOf<Int>()) }
    var now by remember { mutableLongStateOf(0L) }
    var over by remember { mutableStateOf(false) }
    val wobble = remember { Animatable(0f) }
    val drop = remember { Animatable(0f) }

    fun nextOrder() {
        val white = listOf(60, 65, 67)                       // C, F, G major: all white keys
        val whiteMinor = listOf(62, 64, 69)                  // D, E, A minor: all white keys
        order = when {
            score < 3 -> Order(white.random(rnd), false)
            score < 6 -> if (rnd.nextBoolean()) Order(white.random(rnd), false) else Order(whiteMinor.random(rnd), true)
            else -> Order(listOf(60, 62, 64, 65, 67, 69).random(rnd), rnd.nextBoolean())
        }
        layers = emptyList(); candle = false
        say = when (score) {
            0 -> say
            3 -> "New: minor cakes! Minor is take, then 3 keys up, then 4 more."
            6 -> "Now any chord. Count: major is 4 + 3, minor is 3 + 4."
            else -> "Next order: ${order.name}!"
        }
    }

    LaunchedEffect(Unit) {
        var last = 0L
        gameLoop(control) { t, _ ->
            now = t; left = (left - (t - last)).coerceAtLeast(0); last = t
            if (left <= 0 && !over) { over = true; control.end(score) }
        }
    }

    fun check() {
        busy = true
        val want = order.notes
        val ok = layers.map { it % 12 }.toSet() == want.map { it % 12 }.toSet() && layers.first() % 12 == order.root % 12 && layers.first() == layers.min()
        scope.launch {
            if (ok) {
                score++; candle = true; mood = LouMood.Cheer; sfx.correct()
                say = listOf("Yum! ${order.name}!", "Perfect layers!", "A tasty ${order.name}!").random(rnd)
                layers.sorted().forEach { engine.ping(it, 1.1f, 0.6f) }
                wobble.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = 300f)); wobble.snapTo(0f)
                delay(900)
            } else {
                left = (left - 5000).coerceAtLeast(0); mood = LouMood.Encourage; sfx.miss()
                val n = want.map { Basics.name(it) }
                say = if (layers.first() % 12 != order.root % 12 || layers.first() != layers.min()) "The bottom layer must be ${n[0]}. Then take ${n[1]} and ${n[2]}."
                      else "Close! ${order.name} is ${n.joinToString(", ")}."
                drop.animateTo(1f, tween(420)); drop.snapTo(0f)
                layers = emptyList()
                // play the right chord so the ear learns it
                want.forEach { engine.ping(it, 0.9f, 0.55f) }
                delay(700); busy = false; mood = LouMood.Idle; return@launch
            }
            nextOrder(); busy = false; mood = LouMood.Happy
        }
    }

    val land = isLandscape()
    Column(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val w = constraints.maxWidth.toFloat(); val h = constraints.maxHeight.toFloat()
            val density = androidx.compose.ui.platform.LocalDensity.current.density
            val measurer = rememberTextMeasurer()
            // the order ticket
            Column(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 64.dp, end = 16.dp).clayCard(16.dp).padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ORDER", style = AeroType.Caps)
                Text(order.name, style = AeroType.Title.copy(fontSize = 22.sp), modifier = Modifier.semantics { contentDescription = "Order: ${order.name}" })
                Text("${layers.size} of 3 layers", style = AeroType.Small)
            }
            Canvas(Modifier.fillMaxSize()) {
                val cx = w * 0.58f; val plateY = h * 0.82f
                val lw = minOf(w * 0.46f, 230f * density); val lh = 34f * density
                drawOval(Color(0x22083A5A), Offset(cx - lw * 0.72f, plateY - 4f), Size(lw * 1.44f, 22f * density))
                drawOval(Brush.verticalGradient(listOf(Color.White, Color(0xFFDCE6EE)), plateY - 12f, plateY + 14f), Offset(cx - lw * 0.7f, plateY - 12f * density), Size(lw * 1.4f, 24f * density))
                layers.forEachIndexed { i, note ->
                    val tilt = sin(wobble.value * 6.28f) * 4f * (i + 1)
                    val slide = drop.value * (if (i % 2 == 0) -1 else 1) * w * 0.5f * (i + 1) / 3f
                    val y = plateY - 10f * density - (i + 1) * lh
                    val width = lw * (1f - i * 0.12f)
                    rotate(tilt, Offset(cx, y + lh / 2)) {
                        val c = NoteColors.of(note)
                        drawRoundRect(c.copy(red = c.red * 0.75f, green = c.green * 0.75f, blue = c.blue * 0.75f), Offset(cx - width / 2 + slide, y + 4f), Size(width, lh), CornerRadius(14f * density))
                        drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), c), y, y + lh * 0.7f), Offset(cx - width / 2 + slide, y), Size(width, lh - 4f), CornerRadius(14f * density))
                        // frosting drips
                        for (d in 0 until 5) drawCircle(Color.White.copy(alpha = 0.85f), 5f * density, Offset(cx - width / 2 + slide + width * (0.12f + d * 0.19f), y + 4f + (d % 2) * 5f))
                        val l = measurer.measure(Basics.name(note), TextStyle(fontFamily = OpenSans, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = NoteColors.ink(note)))
                        drawText(l, topLeft = Offset(cx + slide - l.size.width / 2f, y + lh / 2 - l.size.height / 2f))
                    }
                }
                if (candle && layers.size == 3) {
                    val top = plateY - 10f * density - 3 * lh
                    drawRoundRect(Color(0xFFFFF1E8), Offset(cx - 5f * density, top - 34f * density), Size(10f * density, 32f * density), CornerRadius(4f))
                    val f = 1f + sin(now / 80f) * 0.15f
                    drawOval(Brush.radialGradient(listOf(Color(0xFFFFF59D), Color(0xFFFF9800), Color(0x00FF9800)), Offset(cx, top - 42f * density), 14f * density * f),
                        Offset(cx - 8f * density * f, top - 58f * density * f), Size(16f * density * f, 26f * density * f))
                }
                // time bar
                val k = left / 90_000f
                drawRoundRect(Color(0x55FFFFFF), Offset(w * 0.1f, h - 14f * density), Size(w * 0.8f, 8f * density), CornerRadius(4f * density))
                drawRoundRect(if (k > 0.25f) Color(0xFF62B826) else Color(0xFFEF5B6E), Offset(w * 0.1f, h - 14f * density), Size(w * 0.8f * k, 8f * density), CornerRadius(4f * density))
            }
            // chef Lou (always in a chef's hat in this game)
            Lou(Modifier.offset { IntOffset((w * 0.04f).toInt(), (h * 0.82f - 110 * density).toInt()) }.size(110.dp, 110.dp), mood = mood,
                outfit = LouLook.outfit.copy(hat = "hat.chef"))
            LouBubble(say, Modifier.align(Alignment.TopStart).statusBarsPadding().padding(top = 64.dp, start = 16.dp, end = 170.dp))
            Row(Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (layers.isNotEmpty() && !busy) ClayChip("↶ Undo", { layers = layers.dropLast(1); sfx.toggleOff() }, gel = Gel.Cream, fontSize = 14)
            }
        }
        RainbowKeys(Basics.KeyView(range = 60..83, letters = score < 6, rainbow = true, sharps = score >= 6), layers.toSet(), sounding, emptySet(), null,
            Modifier.fillMaxWidth().height(if (land) 120.dp else 170.dp).navigationBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp)) { k ->
            if (busy || control.paused || over || layers.size >= 3) return@RainbowKeys
            engine.ping(k, 0.5f); sounding = sounding + k
            layers = layers + k
            if (layers.size == 3) check()
        }
        LaunchedEffect(sounding) { if (sounding.isNotEmpty()) { delay(250); sounding = emptySet() } }
    }
    GameHud(score, 0, 0, "${(left / 1000)}s")
}
