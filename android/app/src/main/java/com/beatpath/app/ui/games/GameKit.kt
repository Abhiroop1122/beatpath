package com.beatpath.app.ui.games

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatpath.app.audio.AudioEngine
import com.beatpath.app.audio.Sfx
import com.beatpath.app.ui.aero.AeroBackground
import com.beatpath.app.ui.aero.ClayIconButton
import com.beatpath.app.ui.aero.Gel
import com.beatpath.app.ui.aero.GelButton
import com.beatpath.app.ui.aero.GelOrb
import com.beatpath.app.ui.aero.LeaveDialog
import com.beatpath.app.ui.aero.LouBubble
import com.beatpath.app.ui.aero.clayCard
import com.beatpath.app.ui.aero.isLandscape
import com.beatpath.app.ui.aero.sideSafe
import com.beatpath.app.ui.home.Confetti
import com.beatpath.app.ui.lou.Lou
import com.beatpath.app.ui.lou.LouMood
import com.beatpath.app.ui.theme.AeroColors
import com.beatpath.app.ui.theme.AeroType
import kotlinx.coroutines.delay
import kotlin.math.sin

/** A mini game: what it teaches, how to play, and the scores that earn 1, 2 and 3 stars. */
data class GameInfo(val id: String, val title: String, val teaches: String, val how: String, val gel: Gel, val icon: ImageVector, val stars: Triple<Int, Int, Int>) {
    fun starsFor(score: Int) = when { score >= stars.third -> 3; score >= stars.second -> 2; score >= stars.first -> 1; else -> 0 }
}

object Games {
    val skyHop = GameInfo("game.skyhop", "Sky Hop", "Hear if a note goes up, down or stays", "Lou plays two notes. Tap the cloud where the second note went: higher, lower or the same.", Gel.Aqua, Icons.Rounded.Star, Triple(5, 10, 16))
    val beatRun = GameInfo("game.beatrun", "Beat Run", "Keep a steady beat", "Lou runs to the drum. Tap anywhere to jump when a block reaches him, right on the beat.", Gel.Orange, Icons.Rounded.Star, Triple(8, 16, 26))
    val balloonPop = GameInfo("game.balloons", "Balloon Pop", "Find note names on the keyboard", "Balloons float up with a note name. Tap that key before the balloon flies away.", Gel.Pink, Icons.Rounded.Star, Triple(8, 15, 24))
    val chordCake = GameInfo("game.chordcake", "Chord Cake", "Build chords: take, skip, take", "Read the order, then tap three keys to stack the chord. Bottom note first.", Gel.Purple, Icons.Rounded.Star, Triple(3, 6, 9))
    val echo = GameInfo("game.echo", "Echo Lou", "Remember and repeat a melody", "Watch Lou hop and sing. Then tap the same pads in the same order.", Gel.Grass, Icons.Rounded.Star, Triple(4, 6, 9))
    val all = listOf(skyHop, beatRun, balloonPop, chordCake, echo)
    fun byId(id: String?) = all.firstOrNull { it.id == id }
}

/** What a game can do to the shell around it. */
class GameControl internal constructor(private val finish: (Int) -> Unit) {
    var paused by mutableStateOf(false)
        internal set
    fun end(score: Int) = finish(score)
}

private enum class Phase { Intro, Countdown, Play, Results }

/**
 * Every game runs inside this: an intro card where Lou explains, a 3-2-1 countdown, the game,
 * and a results card with stars, best score and XP. Back (or ✕) pauses and Lou asks, sadly, if you want to leave.
 */
@Composable
fun GameShell(info: GameInfo, best: Int, sfx: Sfx, onExit: () -> Unit, onResult: (score: Int, stars: Int) -> Int,
              game: @Composable BoxScope.(GameControl) -> Unit) {
    var phase by remember { mutableStateOf(Phase.Intro) }
    var run by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var stars by remember { mutableIntStateOf(0) }
    var xp by remember { mutableIntStateOf(0) }
    var newBest by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }
    var pendingEnd by remember { mutableStateOf<Int?>(null) }
    fun finishWith(s: Int) {
        score = s; stars = info.starsFor(s); newBest = s > best && s > 0
        xp = onResult(s, stars)
        if (stars > 0) sfx.complete() else sfx.louSad()
        phase = Phase.Results
    }
    // a game that ends while the leave dialog is open waits for the answer: stay shows results, leave records nothing
    val control = remember(run) { GameControl { s -> if (leaving) pendingEnd = s else finishWith(s) } }
    // leaving the app (home button, a call) pauses the game and asks on return, instead of playing on in the dark
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    androidx.compose.runtime.DisposableEffect(lifecycle, phase) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_PAUSE && (phase == Phase.Play || phase == Phase.Countdown)) { control.paused = true; leaving = true }
        }
        lifecycle.addObserver(obs); onDispose { lifecycle.removeObserver(obs) }
    }
    BackHandler { when (phase) { Phase.Play, Phase.Countdown -> { control.paused = true; leaving = true }; else -> onExit() } }
    val land = isLandscape()
    AeroBackground {
        when (phase) {
            Phase.Intro -> Column(Modifier.fillMaxSize().sideSafe().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(if (land) 10.dp else 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(if (land) 4.dp else 14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    ClayIconButton(Icons.Rounded.Close, "Back to games", onExit)
                    Spacer(Modifier.weight(1f))
                    if (best > 0) Text("Best: $best", style = AeroType.H2.copy(color = AeroColors.PrimaryDeep))
                }
                Column(Modifier.widthIn(max = 520.dp).fillMaxWidth().clayCard(28.dp).padding(if (land) 12.dp else 20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(if (land) 4.dp else 10.dp)) {
                    val bob = remember { Animatable(0f) }
                    LaunchedEffect(Unit) { while (true) { bob.animateTo(1f, tween(900, easing = FastOutSlowInEasing)); bob.animateTo(0f, tween(900, easing = FastOutSlowInEasing)) } }
                    Lou(Modifier.size(if (land) 84.dp else 150.dp, if (land) 74.dp else 132.dp).graphicsLayer { translationY = -bob.value * 8.dp.toPx() }, mood = LouMood.Happy)
                    Text(info.title, style = if (land) AeroType.Title else AeroType.Hero, textAlign = TextAlign.Center)
                    Text(info.teaches, style = AeroType.H2.copy(color = AeroColors.PrimaryDeep, fontSize = 17.sp), textAlign = TextAlign.Center)
                    Text(info.how, style = AeroType.Body.copy(fontSize = if (land) 14.sp else 16.sp), textAlign = TextAlign.Center)
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        listOf(info.stars.first, info.stars.second, info.stars.third).forEachIndexed { i, s ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row { repeat(i + 1) { Icon(Icons.Rounded.Star, null, tint = Color(0xFFF4B731), modifier = Modifier.size(18.dp)) } }
                                Text("$s", style = AeroType.Label)
                            }
                        }
                    }
                    GelButton("Play", { sfx.select(); score = 0; run++; phase = Phase.Countdown }, Modifier.fillMaxWidth(), gel = Gel.Grass)
                }
            }
            Phase.Countdown -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                var n by remember { mutableIntStateOf(3) }
                LaunchedEffect(run) {
                    for (i in 3 downTo 1) { while (leaving) delay(100); n = i; sfx.tap(); delay(650) }
                    while (leaving) delay(100)   // the count waits while the leave dialog is open
                    sfx.select(); phase = Phase.Play
                }
                AnimatedContent(n, transitionSpec = { (scaleIn(spring(dampingRatio = 0.45f, stiffness = 500f), initialScale = 0.4f) + fadeIn()) togetherWith fadeOut(tween(120)) }, label = "cd") { v ->
                    GelOrb(Modifier.size(140.dp), info.gel) { Text("$v", style = AeroType.Hero.copy(color = Color.White, fontSize = 64.sp)) }
                }
            }
            Phase.Play -> Box(Modifier.fillMaxSize().sideSafe()) {
                androidx.compose.runtime.key(run) { game(control) }
                ClayIconButton(Icons.Rounded.Close, "Pause and leave", { control.paused = true; leaving = true },
                    Modifier.align(Alignment.TopStart).statusBarsPadding().padding(10.dp), size = 40.dp)
            }
            Phase.Results -> Column(Modifier.fillMaxSize().sideSafe().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Column(Modifier.widthIn(max = 520.dp).fillMaxWidth().clayCard(28.dp).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Lou(Modifier.size(if (land) 110.dp else 150.dp, if (land) 96.dp else 132.dp), mood = if (stars > 0) LouMood.Cheer else LouMood.Encourage)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(3) { i ->
                            val s = remember(run) { Animatable(0f) }
                            LaunchedEffect(run) { delay(300L + i * 260L); s.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 400f)); if (i < stars) sfx.xpTick(i * 3) }
                            Icon(Icons.Rounded.Star, if (i < stars) "Star earned" else "Star not earned", tint = if (i < stars) Color(0xFFF4B731) else Color(0x55FFFFFF),
                                modifier = Modifier.size(if (i == 1) 56.dp else 44.dp).graphicsLayer { scaleX = s.value; scaleY = s.value; rotationZ = (1f - s.value) * -40f })
                        }
                    }
                    Text(if (stars == 3) "Amazing!" else if (stars > 0) "Nice playing!" else "Good try!", style = AeroType.Title)
                    Text("Score $score${if (newBest) " · New best!" else ""}", style = AeroType.H2.copy(color = AeroColors.PrimaryDeep))
                    if (xp > 0) Text("+$xp XP", style = AeroType.H2.copy(color = Color(0xFF9A5200)))
                    else Text(if (stars == 0) "Get ${info.stars.first} to earn your first star." else "", style = AeroType.Body)
                    GelButton("Play again", { sfx.select(); score = 0; run++; phase = Phase.Countdown }, Modifier.fillMaxWidth(), gel = Gel.Grass)
                    GelButton("Back to games", onExit, Modifier.fillMaxWidth(), gel = Gel.Silver)
                }
                Confetti(if (stars == 3) run else 0)
            }
        }
        if (leaving) LeaveDialog(sfx, title = "Oh no! Stop playing?", body = "Your score in this round won't count.",
            onLeave = { leaving = false; onExit() }, onStay = { leaving = false; control.paused = false; pendingEnd?.let { pendingEnd = null; finishWith(it) } })
    }
}

/** Score, hearts and a small label, across the top of a game. */
@Composable
fun GameHud(score: Int, hearts: Int, maxHearts: Int, label: String = "", modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().statusBarsPadding().padding(start = 64.dp, end = 14.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (maxHearts > 0) Row(Modifier.semantics { contentDescription = "$hearts of $maxHearts hearts left" }) {
            repeat(maxHearts) { i ->
                val alive = i < hearts
                val s = remember { Animatable(1f) }
                LaunchedEffect(alive) { if (!alive) { s.animateTo(1.5f, tween(120)); s.animateTo(1f, spring(dampingRatio = 0.4f)) } }
                Icon(Icons.Rounded.Favorite, null, tint = if (alive) Color(0xFFEF5B6E) else Color(0x66FFFFFF), modifier = Modifier.size(26.dp).graphicsLayer { scaleX = s.value; scaleY = s.value })
            }
        }
        Spacer(Modifier.weight(1f))
        if (label.isNotEmpty()) Text(label, style = AeroType.Label.copy(color = AeroColors.PrimaryDeep), modifier = Modifier.padding(end = 10.dp))
        val bump = remember { Animatable(1f) }
        LaunchedEffect(score) { if (score > 0) { bump.snapTo(1.35f); bump.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 500f)) } }
        Box(Modifier.clayCard(18.dp).padding(horizontal = 14.dp, vertical = 6.dp)) {
            Text("$score", style = AeroType.Title.copy(fontSize = 22.sp), modifier = Modifier.graphicsLayer { scaleX = bump.value; scaleY = bump.value }
                .semantics { contentDescription = "Score $score" })
        }
    }
}

/** A quick burst of sparkles at a point (for pops, catches, perfect hits). */
class Burst(val at: Offset, val color: Color, val born: Long)

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBursts(bursts: List<Burst>, now: Long) {
    bursts.forEach { b ->
        val t = ((now - b.born) / 600f).coerceIn(0f, 1f); if (t >= 1f) return@forEach
        repeat(10) { i ->
            val a = i / 10f * 6.283f + b.born % 7
            val d = 18f + t * 70f
            drawCircle(b.color.copy(alpha = 1f - t), (1f - t) * 7f + 1f, Offset(b.at.x + kotlin.math.cos(a) * d, b.at.y + sin(a) * d - t * 20f))
        }
        drawCircle(Color.White.copy(alpha = (1f - t) * 0.7f), 10f + t * 30f, b.at, style = androidx.compose.ui.graphics.drawscope.Stroke(3f * (1f - t)))
    }
}

/** Frame clock for game loops: calls [tick] every frame with (ms since start, ms since last frame) until cancelled; skips while paused. */
suspend fun gameLoop(control: GameControl, tick: (Long, Long) -> Unit) {
    var start = -1L; var last = 0L; var pausedFor = 0L; var pauseAt = -1L; var lastRaw = -1L
    while (true) {
        androidx.compose.runtime.withFrameNanos { nanos ->
            val ms = nanos / 1_000_000
            if (start < 0) { start = ms; last = 0 }
            // a long gap between frames means the app was hidden: don't count that time as play
            if (lastRaw >= 0 && ms - lastRaw > 250) pausedFor += ms - lastRaw - 16
            lastRaw = ms
            if (control.paused) { if (pauseAt < 0) pauseAt = ms; return@withFrameNanos }
            if (pauseAt >= 0) { pausedFor += ms - pauseAt; pauseAt = -1 }
            val t = ms - start - pausedFor
            tick(t, (t - last).coerceIn(0, 50)); last = t
        }
    }
}

/** Plays one note on the engine's clean keys sound. */
fun AudioEngine.ping(midi: Int, dur: Float = 0.45f, vel: Float = 0.75f) = playNote(midi, dur, vel, AudioEngine.Timbre.KEYS)

@Suppress("unused") private val keepLinear = LinearEasing
