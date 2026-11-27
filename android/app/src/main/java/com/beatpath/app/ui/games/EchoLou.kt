package com.beatpath.app.ui.games

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.beatpath.app.ui.lou.Lou
import com.beatpath.app.ui.lou.LouMood
import com.beatpath.app.ui.theme.AeroType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** C D E G A: five notes that always sound good together (the pentatonic scale), each with its scale number. */
private val PADS = listOf(60 to "1", 62 to "2", 64 to "3", 67 to "5", 69 to "6")

/**
 * Echo Lou: Lou hops from pad to pad singing a tune; you tap the same pads in the same order.
 * The tune grows by one note each round and gets a little faster. Two hearts; a slip replays the tune slowly.
 */
@Composable
fun BoxScope.EchoLou(control: GameControl, engine: AudioEngine, sfx: Sfx) {
    val scope = rememberCoroutineScope()
    val rnd = remember { Random(System.nanoTime()) }
    var tune by remember { mutableStateOf(listOf(rnd.nextInt(5))) }
    var pos by remember { mutableIntStateOf(0) }
    var hearts by remember { mutableIntStateOf(2) }
    var best by remember { mutableIntStateOf(0) }
    var lit by remember { mutableIntStateOf(-1) }
    var listening by remember { mutableStateOf(true) }
    var say by remember { mutableStateOf("Watch me and listen!") }
    var mood by remember { mutableStateOf(LouMood.Happy) }
    var round by remember { mutableIntStateOf(1) }
    var over by remember { mutableStateOf(false) }
    val hopX = remember { Animatable(2f) }      // which pad Lou is above (fractional while hopping)
    val hopY = remember { Animatable(0f) }

    suspend fun hopTo(i: Int, gap: Long) {
        scope.launch { hopX.animateTo(i.toFloat(), tween((gap * 0.6f).toInt(), easing = FastOutSlowInEasing)) }
        hopY.animateTo(1f, tween((gap * 0.3f).toInt())); hopY.animateTo(0f, tween((gap * 0.3f).toInt()))
    }
    suspend fun playTune(slow: Boolean = false) {
        listening = true; mood = LouMood.Listen; pos = 0
        val gap = ((if (slow) 820L else 640L) - tune.size * 18L).coerceAtLeast(360L)
        delay(500)
        for (i in tune) {
            while (control.paused) delay(100)   // the dialog is open: Lou waits, mid-tune
            hopTo(i, gap)
            lit = i; engine.ping(PADS[i].first, gap / 1000f * 0.9f)
            delay(gap / 3); lit = -1
        }
        listening = false; mood = LouMood.Happy; say = "Your turn! ${tune.size} note${if (tune.size > 1) "s" else ""}."
    }
    LaunchedEffect(round) {
        say = if (round == 1) "Watch me and listen!" else listOf("Here comes a longer one!", "Listen closely…", "One more note!").random(rnd)
        playTune()
    }

    fun tap(i: Int) {
        if (listening || over || control.paused) return
        engine.ping(PADS[i].first, 0.45f); lit = i
        scope.launch { delay(180); if (lit == i) lit = -1 }
        if (tune[pos] == i) {
            pos++
            if (pos == tune.size) {
                best = tune.size; sfx.correct(); mood = LouMood.Cheer
                say = listOf("You remembered it all!", "Perfect echo!", "What a memory!").random(rnd)
                listening = true
                scope.launch { delay(900); tune = tune + rnd.nextInt(5); round++ }
            }
        } else {
            hearts--; sfx.miss(); mood = LouMood.Sad; listening = true
            val want = PADS[tune[pos]]
            say = "Oops! The next one was ${Basics.name(want.first)} (number ${want.second}). Listen slowly."
            scope.launch {
                if (hearts <= 0) { delay(900); while (control.paused) delay(100); over = true; control.end(best); return@launch }
                delay(1100); playTune(slow = true)
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth; val h = maxHeight
        val padSize = minOf(w / 5.4f, 104.dp)
        val cy = h * 0.66f; val arc = minOf(h * 0.12f, 90.dp)
        // five pads in a wide, gentle arc: the middle pad sits highest, like a smile turned upside down
        fun padCenter(i: Float): Pair<androidx.compose.ui.unit.Dp, androidx.compose.ui.unit.Dp> =
            (w * (0.11f + i * 0.195f)) to (cy - arc * sin(Math.PI * i / 4.0).toFloat())
        PADS.forEachIndexed { i, (note, num) ->
            val (px, py) = padCenter(i.toFloat())
            val bounce = remember { Animatable(1f) }
            LaunchedEffect(lit == i) { if (lit == i) { bounce.snapTo(1.18f); bounce.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 500f)) } }
            val src = remember { MutableInteractionSource() }
            val gel = Gel.Cream
            Box(Modifier.offset(px - padSize / 2, py - padSize / 2).size(padSize, padSize + 4.dp)
                .graphicsLayer { scaleX = bounce.value; scaleY = bounce.value }
                .claySurface(gel, padSize / 2, depth = 4.dp, press = if (lit == i) 1f else 0f, outline = NoteColors.of(note))
                .clip(CircleShape).clickable(src, null, role = Role.Button) { tap(i) }
                .semantics { contentDescription = "Pad ${Basics.name(note)}, number $num" }, contentAlignment = Alignment.Center) {
                Box(Modifier.size(padSize * 0.72f).claySurface(if (lit == i) Gel.Gold else Gel.Cream, padSize * 0.36f, depth = 2.dp).clip(CircleShape), contentAlignment = Alignment.Center) {}
                Box(Modifier.size(padSize * 0.62f).graphicsLayer { alpha = if (lit == i) 1f else 0.85f }.claySurfaceColor(NoteColors.of(note), padSize * 0.31f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(Basics.name(note), style = AeroType.Title.copy(color = NoteColors.ink(note), fontSize = 22.sp))
                        Text(num, style = AeroType.Small.copy(color = NoteColors.ink(note)))
                    }
                }
            }
        }
        // Lou hops above the pads
        val (lx, ly) = padCenter(hopX.value)
        Lou(Modifier.offset(lx - 46.dp, ly - padSize / 2 - 86.dp - (hopY.value * 46).dp).size(92.dp, 80.dp), mood = mood)
        LouBubble(say, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 64.dp, start = 16.dp, end = 16.dp))
        Text(if (listening) "Listen…" else "Your turn: ${pos + 1} of ${tune.size}", style = AeroType.H2.copy(color = Color(0xFF0A548F)),
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 18.dp))
    }
    GameHud(best, hearts, 2, "Tune: ${tune.size}")
}

/** A clay disc in any color (for the note faces on the pads). */
private fun Modifier.claySurfaceColor(c: Color, @Suppress("UNUSED_PARAMETER") corner: androidx.compose.ui.unit.Dp): Modifier = this.clip(CircleShape).drawBehind {
    drawCircle(androidx.compose.ui.graphics.Brush.radialGradient(listOf(Color.White.copy(alpha = 0.6f), c, c.copy(red = c.red * 0.82f, green = c.green * 0.82f, blue = c.blue * 0.82f)),
        center + androidx.compose.ui.geometry.Offset(-size.width * 0.2f, -size.height * 0.25f), size.minDimension * 0.75f))
}
