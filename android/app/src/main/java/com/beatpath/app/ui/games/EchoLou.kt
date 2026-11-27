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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
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
import com.beatpath.app.ui.aero.LouBubble
import com.beatpath.app.ui.aero.claySurface
import com.beatpath.app.ui.lou.LouMood
import com.beatpath.app.ui.theme.AeroColors
import com.beatpath.app.ui.theme.AeroType
import com.beatpath.app.ui.theme.OpenSans
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/** C D E G A: five notes that always sound good together (the pentatonic scale), each with its scale number. */
private val PADS = listOf(60 to "1", 62 to "2", 64 to "3", 67 to "5", 69 to "6")

/** A ring spreading from a pad, and a note rising from it. */
private class Ripple(val pad: Int, val born: Long)

/**
 * Echo Lou: Lou hops from pad to pad singing a tune (each pad sinks under him and sends up its note);
 * then you tap the same pads in the same order while he watches and nods. The tune grows by one note each round
 * and gets a little faster. Two hearts; a slip replays the tune slowly.
 */
@Composable
fun BoxScope.EchoLou(control: GameControl, engine: AudioEngine, sfx: Sfx) {
    val scope = rememberCoroutineScope()
    val rnd = remember { Random(System.nanoTime()) }
    var tune by remember { mutableStateOf(listOf(rnd.nextInt(5))) }
    var pos by remember { mutableIntStateOf(0) }
    var hearts by remember { mutableIntStateOf(3) }
    var best by remember { mutableIntStateOf(0) }
    var pressed by remember { mutableIntStateOf(-1) }
    var listening by remember { mutableStateOf(true) }
    var say by remember { mutableStateOf("Watch me and listen!") }
    var mood by remember { mutableStateOf(LouMood.Happy) }
    var round by remember { mutableIntStateOf(1) }
    var over by remember { mutableStateOf(false) }
    var ripples by remember { mutableStateOf(listOf<Ripple>()) }
    var now by remember { mutableLongStateOf(0L) }
    var watching by remember { mutableIntStateOf(2) }
    val padAt = remember { Animatable(2f) }       // which pad Lou is over (fractional mid-hop)
    val hop = remember { Animatable(0f) }         // 0..1 along the current hop's arc
    val squash = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(Unit) { gameLoop(control) { t, _ -> now = t; if (ripples.isNotEmpty()) ripples = ripples.filter { t - it.born < 900 } } }

    fun sound(i: Int, sec: Float) { engine.ping(PADS[i].first, sec); ripples = ripples + Ripple(i, now) }
    /** Lou hops onto pad [i]: a quick crouch, an arc, and a squash as the pad sinks; the note sounds on landing. */
    suspend fun hopTo(i: Int, ms: Int, sec: Float) {
        val from = padAt.value
        squash.animateTo(0.14f, tween((ms * 0.15f).toInt()))
        hop.snapTo(0f)
        scope.launch { squash.animateTo(-0.06f, tween((ms * 0.2f).toInt())) }
        scope.launch { padAt.animateTo(i.toFloat(), tween((ms * 0.55f).toInt(), easing = FastOutSlowInEasing)) }
        hop.animateTo(1f, tween((ms * 0.55f).toInt(), easing = LinearEasing))
        if (abs(from - i) < 0.01f) hop.snapTo(0f)
        pressed = i; sound(i, sec)
        squash.snapTo(0.16f); scope.launch { squash.animateTo(0f, spring(dampingRatio = 0.4f, stiffness = 500f)) }
        delay((ms * 0.3f).toLong()); pressed = -1
    }
    suspend fun playTune(slow: Boolean = false) {
        listening = true; mood = LouMood.Listen; pos = 0
        val gap = ((if (slow) 860 else 680) - tune.size * 18).coerceAtLeast(400)
        delay(450)
        for (i in tune) {
            while (control.paused) delay(100)
            mood = LouMood.Happy
            hopTo(i, gap, gap / 1000f * 0.9f)
        }
        listening = false; mood = LouMood.Idle; say = "Your turn! ${tune.size} note${if (tune.size > 1) "s" else ""}."
    }
    LaunchedEffect(round) {
        say = if (round == 1) "Watch me and listen!" else listOf("Here comes a longer one!", "Listen closely…", "One more note!").random(rnd)
        playTune()
    }

    fun tap(i: Int) {
        if (listening || over || control.paused) return
        sound(i, 0.45f); pressed = i; watching = i
        scope.launch { delay(170); if (pressed == i) pressed = -1 }
        if (tune[pos] == i) {
            pos++
            scope.launch { squash.snapTo(0.1f); squash.animateTo(0f, spring(dampingRatio = 0.4f, stiffness = 600f)) }   // a little nod
            if (pos == tune.size) {
                best = tune.size; sfx.correct(); mood = LouMood.Cheer; listening = true
                say = listOf("You remembered it all!", "Perfect echo!", "What a memory!").random(rnd)
                scope.launch {
                    // a happy jump on the spot
                    repeat(2) { squash.animateTo(0.15f, tween(90)); hop.snapTo(0f); squash.animateTo(-0.08f, tween(90)); delay(160); squash.animateTo(0f, spring(0.4f, 500f)) }
                    delay(400); tune = tune + rnd.nextInt(5); round++
                }
            }
        } else {
            hearts--; sfx.miss(); mood = LouMood.Sad; listening = true
            val want = PADS[tune[pos]]
            say = "Oops! The next one was ${Basics.name(want.first)} (number ${want.second}). Listen slowly."
            scope.launch {
                repeat(3) { shake.animateTo(1f, tween(70)); shake.animateTo(-1f, tween(70)) }; shake.animateTo(0f, tween(70))
                if (hearts <= 0) { delay(700); while (control.paused) delay(100); over = true; control.end(best); return@launch }
                delay(700); playTune(slow = true)
            }
        }
    }

    LaunchedEffect(listening, round) { if (GameDebug.autoplay && !listening) { tune.toList().forEach { delay(420); tap(it) } } }
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth; val h = maxHeight
        val density = androidx.compose.ui.platform.LocalDensity.current.density
        val padSize = minOf(w / 5.4f, 104.dp)
        val cy = h * 0.66f; val arc = minOf(h * 0.12f, 90.dp)
        fun padCenter(i: Float): Pair<androidx.compose.ui.unit.Dp, androidx.compose.ui.unit.Dp> =
            (w * (0.13f + i * 0.185f)) to (cy - arc * sin(Math.PI * i / 4.0).toFloat())
        // ripples and rising notes, behind the pads
        Canvas(Modifier.fillMaxSize()) {
            ripples.forEach { r ->
                val k = ((now - r.born) / 900f).coerceIn(0f, 1f)
                val (px, py) = padCenter(r.pad.toFloat())
                val c = NoteColors.of(PADS[r.pad].first)
                val center = Offset(px.toPx(), py.toPx())
                drawCircle(c.copy(alpha = 0.5f * (1f - k)), padSize.toPx() * (0.55f + k * 0.6f), center, style = Stroke((6f * (1f - k) + 1f) * density))
                val l = measurer.measure("♪", TextStyle(fontFamily = OpenSans, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = c.copy(alpha = 1f - k)))
                drawText(l, topLeft = Offset(center.x - l.size.width / 2f + sin(k * 8f) * 10f * density, center.y - padSize.toPx() * 0.9f - k * 70f * density))
            }
        }
        PADS.forEachIndexed { i, (note, num) ->
            val (px, py) = padCenter(i.toFloat())
            val sink = remember { Animatable(0f) }
            LaunchedEffect(pressed == i) { if (pressed == i) { sink.snapTo(1f); sink.animateTo(0f, spring(dampingRatio = 0.4f, stiffness = 420f)) } }
            val c = NoteColors.of(note)
            Box(Modifier.offset(px - padSize / 2, py - padSize / 2).size(padSize, padSize + 5.dp)
                .graphicsLayer { scaleX = 1f + sink.value * 0.06f; scaleY = 1f - sink.value * 0.05f; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f) }
                .claySurface(Gel.Cream, padSize / 2, depth = 5.dp, press = sink.value, outline = c)
                .clip(CircleShape).pointerInput(i) { detectTapGestures(onPress = { tap(i) }) }
                .semantics { contentDescription = "Pad ${Basics.name(note)}, number $num"; role = Role.Button }, contentAlignment = Alignment.TopCenter) {
                Box(Modifier.padding(top = padSize * 0.16f + 5.dp * sink.value).size(padSize * 0.66f).clip(CircleShape).drawBehind {
                    drawCircle(Brush.radialGradient(listOf(lerp(c, Color.White, 0.55f), c, lerp(c, Color.Black, 0.18f)), center + Offset(-size.width * 0.2f, -size.height * 0.25f), size.minDimension * 0.75f))
                }, contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(Basics.name(note), style = AeroType.Title.copy(color = NoteColors.ink(note), fontSize = 22.sp, lineHeight = 24.sp))
                        Text(num, style = AeroType.Small.copy(color = NoteColors.ink(note), fontSize = 11.sp, lineHeight = 12.sp))
                    }
                }
            }
        }
        // Lou stands on the pad he's over; during a hop he follows an arc between pads
        val (lx, ly) = padCenter(padAt.value)
        val arcUp = sin(hop.value * Math.PI).toFloat() * 56f
        val footPx = (ly - padSize / 2 + 6.dp).value * density - arcUp * density
        val gazePad = if (listening) padAt.value else watching.toFloat()
        val gaze = Offset(((gazePad - padAt.value) / 2f).coerceIn(-1f, 1f), if (listening) 0.2f else 0.8f)
        LouActor(lx.value * density, footPx, 84.dp, mood, squash = squash.value, lean = shake.value * 12f + (if (hop.value in 0.01f..0.99f) 8f else 0f), look = gaze)
        LouBubble(say, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 60.dp, start = 16.dp, end = 16.dp))
        Text(if (listening) "Listen…" else "Your turn: ${pos + 1} of ${tune.size}", style = AeroType.H2.copy(color = AeroColors.PrimaryDeep),
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 18.dp))
    }
    GameHud(best, hearts, 3, "Tune: ${tune.size}")
}

@Suppress("unused") private val keepIntOffset = IntOffset(0, 0)
