package com.beatpath.app.ui.lesson

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import com.beatpath.app.audio.AudioEngine
import com.beatpath.app.audio.Sfx
import com.beatpath.app.data.EarIds
import com.beatpath.app.ui.aero.AeroBackground
import com.beatpath.app.ui.aero.Gel
import com.beatpath.app.ui.aero.GelButton
import com.beatpath.app.ui.aero.LouBubble
import com.beatpath.app.ui.aero.ResultTray
import com.beatpath.app.ui.aero.TrayMsg
import com.beatpath.app.ui.aero.glass
import com.beatpath.app.ui.aero.isLandscape
import com.beatpath.app.ui.aero.sideSafe
import com.beatpath.app.ui.lou.Lou
import com.beatpath.app.ui.lou.LouMood
import com.beatpath.app.ui.lou.rememberLou
import com.beatpath.app.ui.theme.AeroColors
import com.beatpath.app.ui.theme.AeroType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** One question. [kind] overrides the lesson's kind, so a Daily mix can hold different question types. */
data class EarItem(val a: Int, val b: Int, val velA: Float = 0.8f, val velB: Float = 0.8f, val kind: EarKind? = null)
/** ChordSpot: Lou plays C, then F, G or A minor — which one? (b = 0, 1, 2) */
enum class EarKind { HigherPad, SoftLoud, Contour, StepLeap, ChordQuality, ChordSpot }
data class EarSpec(val id: String, val kind: EarKind, val title: String, val intro: String, val items: List<EarItem>, val teach: Boolean = kind == EarKind.HigherPad)

object EarLessons {
    fun spec(id: String) = if (id.startsWith("practice.")) com.beatpath.app.lessons.Practice.earSpec(id) else when (id) {
        EarIds.SOFT_LOUD -> EarSpec(id, EarKind.SoftLoud, "Soft or loud?", "Sometimes a note is just louder. Listen for higher — not louder.",
            listOf(EarItem(64, 64, 0.35f, 0.95f), EarItem(60, 67, 0.95f, 0.35f), EarItem(62, 62, 0.95f, 0.4f), EarItem(65, 60, 0.4f, 0.95f)).shuffled())
        EarIds.STEP_LEAP -> EarSpec(id, EarKind.StepLeap, "Step or leap?", "Melodies move by steps (the next note over) or leaps (a jump). Listen to the gap.",
            listOf(EarItem(60, 62), EarItem(60, 67), EarItem(64, 65), EarItem(62, 69), EarItem(67, 65), EarItem(72, 64)).shuffled())
        EarIds.MAJOR_MINOR -> EarSpec(id, EarKind.ChordQuality, "Major or minor?", "Major chords sound bright and happy. Minor chords sound darker. Trust your feeling.",
            listOf(EarItem(60, 0), EarItem(57, 1), EarItem(65, 0), EarItem(62, 1), EarItem(67, 0), EarItem(64, 1)).shuffled())
        EarIds.CONTOUR -> EarSpec(id, EarKind.Contour, "Up, down or the same?", "Listen to where the second note goes.",
            listOf(EarItem(60, 67), EarItem(69, 62), EarItem(64, 64), EarItem(62, 65), EarItem(67, 65)).shuffled())
        else -> EarSpec(EarIds.HIGH_LOW, EarKind.HigherPad, "High or low?", "I can sing really high… and really low. Let's train your ears.",
            listOf(EarItem(48, 72), EarItem(67, 55), EarItem(60, 67, 0.95f, 0.45f), EarItem(64, 60)))
    }
}

private enum class Ans { A, B, Same, Up, Down, Step, Leap, Major, Minor, IV, V, Vi }

@Composable
fun EarLessonScreen(spec: EarSpec, engine: AudioEngine, sfx: Sfx, onExit: () -> Unit, onFinished: (Boolean, String, Int) -> Unit,
                    onAnswer: (EarKind, Boolean) -> Unit = { _, _ -> }) {
    val scope = rememberCoroutineScope()
    val lou = rememberLou(sfx)
    val attempt = remember { "${spec.id}-${System.currentTimeMillis()}" }
    val started = remember { System.currentTimeMillis() }
    val teach = spec.teach
    val first = if (teach) 1 else 0
    val total = first + spec.items.size
    var step by remember { mutableIntStateOf(0) }
    var sel by remember { mutableStateOf<Ans?>(null) }
    var tray by remember { mutableStateOf<TrayMsg?>(null) }
    var lastGood by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf<String?>(null) }
    var helped by remember { mutableStateOf(false) }
    var heard by remember { mutableStateOf(setOf<String>()) }
    var louHigh by remember { mutableStateOf<Boolean?>(null) }
    var confirmExit by remember { mutableStateOf(false) }
    var audioJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    /** One sound at a time: a new one (or a new question) cancels the last, and app sounds always come back on. */
    fun launchAudio(block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit): kotlinx.coroutines.Job {
        audioJob?.cancel()
        return scope.launch { try { block() } finally { playing = null; engine.quietSfx = false } }.also { audioJob = it }
    }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { audioJob?.cancel(); engine.quietSfx = false } }
    fun kindOf(it: EarItem) = it.kind ?: spec.kind

    fun tone(id: String, midi: Int, vel: Float, dur: Float = 0.7f) = launchAudio {
        engine.quietSfx = true; lou.quiet(); lou.mood = LouMood.Listen
        playing = id; engine.playNote(midi, dur, vel, AudioEngine.Timbre.PURE)
        delay((dur * 1000).toLong() + 250); if (playing == id) playing = null
        engine.quietSfx = false
    }
    fun pair(it: EarItem, slow: Boolean = false) = launchAudio {
        engine.quietSfx = true; lou.quiet(); lou.mood = LouMood.Listen
        if (kindOf(it) == EarKind.ChordSpot) {
            // home chord C, then F (IV), G (V) or A minor (vi) — close voicings so only the colour changes
            val second = when (it.b) { 0 -> listOf(60, 65, 69); 1 -> listOf(59, 62, 67); else -> listOf(60, 64, 69) }
            val len = if (slow) 1.3f else 0.9f
            playing = "A"; listOf(48, 60, 64, 67).forEach { n -> engine.playNote(n, len, 0.55f, AudioEngine.Timbre.KEYS) }
            delay((len * 1000).toLong() + if (slow) 350 else 150)
            playing = "B"; (listOf(second.first() - 12) + second).forEach { n -> engine.playNote(n, len, 0.55f, AudioEngine.Timbre.KEYS) }
            delay((len * 1000).toLong() + 300); playing = null; engine.quietSfx = false; return@launchAudio
        }
        if (kindOf(it) == EarKind.ChordQuality) {
            // one chord: root, third (major 4 / minor 3 semitones up), fifth — played together
            val third = if (it.b == 1) 3 else 4
            playing = "A"; listOf(0, third, 7).forEach { iv -> engine.playNote(it.a + iv, if (slow) 1.6f else 1.1f, 0.6f, AudioEngine.Timbre.KEYS) }
            delay(if (slow) 1900 else 1400); playing = null; engine.quietSfx = false; return@launchAudio
        }
        val d = if (slow) 1f else 0.65f
        playing = "A"; engine.playNote(it.a, d, it.velA, AudioEngine.Timbre.PURE); delay((d * 1000).toLong() + if (slow) 450 else 220)
        playing = "B"; engine.playNote(it.b, d, it.velB, AudioEngine.Timbre.PURE); delay((d * 1000).toLong() + 300)
        playing = null; engine.quietSfx = false
    }
    fun right(it: EarItem) = when (kindOf(it)) {
        EarKind.HigherPad -> if (it.a > it.b) Ans.A else Ans.B
        EarKind.SoftLoud -> when { it.a == it.b -> Ans.Same; it.a > it.b -> Ans.A; else -> Ans.B }
        EarKind.Contour -> when { it.b > it.a -> Ans.Up; it.b < it.a -> Ans.Down; else -> Ans.Same }
        EarKind.StepLeap -> if (kotlin.math.abs(it.b - it.a) <= 2) Ans.Step else Ans.Leap
        EarKind.ChordQuality -> if (it.b == 1) Ans.Minor else Ans.Major
        EarKind.ChordSpot -> when (it.b) { 0 -> Ans.IV; 1 -> Ans.V; else -> Ans.Vi }
    }

    fun trayCta() {
        val item = spec.items[step - first]
        tray = null
        if (lastGood) {
            if (step + 1 >= total) onFinished(!helped, attempt, ((System.currentTimeMillis() - started) / 1000).toInt()) else step++
        } else { sel = null; pair(item, slow = true) }
    }
    BackHandler { if (tray != null) trayCta() else confirmExit = true }

    LaunchedEffect(step) {
        audioJob?.cancel(); sel = null
        when {
            teach && step == 0 -> lou.say(spec.intro + " Tap HIGH, then LOW.", LouMood.Happy)
            else -> { lou.say(when (kindOf(spec.items[step - first])) { EarKind.HigherPad -> if (teach) "Ears only now. Which pad is higher?" else "Which pad is higher? Tap a pad to hear it again."; EarKind.SoftLoud -> "Higher, or the same note played louder?"; EarKind.Contour -> "Does the second note go up, down, or stay?"
                EarKind.StepLeap -> "A small step, or a leap?"; EarKind.ChordQuality -> "Bright and happy, or dark and sad?"
                EarKind.ChordSpot -> "C, then which chord: F, G or A minor?" }, LouMood.Idle)
                delay(1400); pair(spec.items[step - first]) }
        }
    }

    val land = isLandscape()
    val progress by animateFloatAsState((step + if (tray?.good == true) 1 else 0) / total.toFloat(), spring(dampingRatio = 1f, stiffness = 120f), label = "progress")
    AeroBackground {
        Column(Modifier.fillMaxSize().sideSafe().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = if (land) 4.dp else 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Close, "Leave lesson", tint = AeroColors.Ink, modifier = Modifier.size(40.dp).clip(CircleShape).clickable { confirmExit = true }.padding(8.dp))
                Spacer(Modifier.width(8.dp))
                if (land) Text(spec.title, style = AeroType.H2.copy(color = Color.White), modifier = Modifier.padding(end = 14.dp))
                // glossy progress tube
                Box(Modifier.weight(1f).height(16.dp).drawBehind {
                    val r = CornerRadius(size.height / 2)
                    drawRoundRect(Color(0x66FFFFFF), cornerRadius = r)
                    val w = size.width * progress.coerceAtLeast(0.04f)
                    drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF9BE15A), Color(0xFF2F8F1C))), size = Size(w, size.height), cornerRadius = r)
                    drawRoundRect(Color(0x88FFFFFF), Offset(4f, 2f), Size((w - 8f).coerceAtLeast(0f), size.height * 0.35f), r)
                    drawRoundRect(Color(0xAAFFFFFF), cornerRadius = r, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
                })
            }
            if (!land) Text(spec.title, style = AeroType.Title.copy(color = Color.White), modifier = Modifier.padding(horizontal = 20.dp))
            AnimatedContent(step, transitionSpec = { (slideInHorizontally(androidx.compose.animation.core.spring(dampingRatio = 0.9f, stiffness = 300f)) { it / 4 } + fadeIn(androidx.compose.animation.core.tween(280))) togetherWith fadeOut(androidx.compose.animation.core.tween(160)) }, label = "s", modifier = Modifier.weight(1f)) { s ->
                val teachStep = teach && s == 0
                // Teaching: Lou literally goes up for high and down for low (cue removed in checks)
                val louTeach: @Composable (Modifier) -> Unit = { m ->
                    BoxWithConstraints(m.glass(26.dp)) {
                        val lh = if (land) 96.dp else 150.dp
                        val y by animateDpAsState(when (louHigh) { true -> 0.dp; false -> maxHeight - lh; null -> (maxHeight - lh) / 2 }, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "y")
                        Text("HIGH", style = AeroType.Caps, modifier = Modifier.align(Alignment.TopEnd).padding(14.dp))
                        Text("LOW", style = AeroType.Caps, modifier = Modifier.align(Alignment.BottomEnd).padding(14.dp))
                        Lou(Modifier.align(Alignment.TopCenter).offset(y = y).size(lh * 1.13f, lh), mood = if (playing != null) LouMood.Happy else lou.mood, mouthOpen = if (playing != null) 0.8f else lou.mouth)
                    }
                }
                val highLow: @Composable () -> Unit = {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GelButton("High ▲", { heard = heard + "high"; louHigh = true; tone("high", 79, 0.8f) }, Modifier.weight(1f), gel = Gel.Aqua)
                        GelButton("Low ▼", { heard = heard + "low"; louHigh = false; tone("low", 43, 0.8f) }, Modifier.weight(1f), gel = Gel.Aqua)
                    }
                }
                val answers: @Composable () -> Unit = {
                    val it = spec.items[s - first]
                    val k = kindOf(it)
                    val big = if (land) 112.dp else 148.dp
                    GelButton(if (k == EarKind.ChordSpot || k == EarKind.ChordQuality) "↻  Hear it again" else "↻  Hear both again", { pair(it) }, Modifier.fillMaxWidth(), gel = Gel.Silver, height = 40.dp)
                    val result = tray?.good
                    if (k == EarKind.ChordSpot) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(Ans.IV to "F · IV", Ans.V to "G · V", Ans.Vi to "Am · vi").forEach { (a, l) ->
                                Pad(l, sel == a, result, false, Modifier.weight(1f).height(if (land) 108.dp else 118.dp), correct = right(it) == a) { if (tray == null) { sel = a; sfx.select() } }
                            }
                        }
                    } else if (k == EarKind.StepLeap || k == EarKind.ChordQuality) {
                        val opts = if (k == EarKind.StepLeap) listOf(Ans.Step to "Step", Ans.Leap to "Leap") else listOf(Ans.Major to "Major · bright", Ans.Minor to "Minor · dark")
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            opts.forEach { (a, l) -> Pad(l, sel == a, result, false, Modifier.weight(1f).height(if (land) 112.dp else 132.dp), correct = right(it) == a) { if (tray == null) { sel = a; sfx.select() } } }
                        }
                    } else if (k == EarKind.Contour) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(Ans.Up to "Up ▲", Ans.Same to "Same", Ans.Down to "Down ▼").forEach { (a, l) ->
                                Pad(l, sel == a, result, false, Modifier.weight(1f).height(if (land) 108.dp else 118.dp), correct = right(it) == a) { if (tray == null) { sel = a; sfx.select() } }
                            }
                        }
                    } else {
                        val padH = if (k == EarKind.SoftLoud && land) 110.dp else big
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Pad("Pad A", sel == Ans.A, result, playing == "A", Modifier.weight(1f).height(padH), correct = right(it) == Ans.A) { if (tray == null) { sel = Ans.A; tone("A", it.a, it.velA) } }
                            Pad("Pad B", sel == Ans.B, result, playing == "B", Modifier.weight(1f).height(padH), correct = right(it) == Ans.B) { if (tray == null) { sel = Ans.B; tone("B", it.b, it.velB) } }
                        }
                        if (k == EarKind.SoftLoud) Pad("Same note", sel == Ans.Same, result, false, Modifier.fillMaxWidth().height(if (land) 52.dp else 64.dp), small = true, correct = right(it) == Ans.Same) { if (tray == null) { sel = Ans.Same; sfx.select() } }
                    }
                }
                if (land) Row(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Column(Modifier.weight(0.9f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (teachStep) louTeach(Modifier.fillMaxWidth().weight(1f))
                        else {
                            lou.bubble?.let { LouBubble(it, Modifier.fillMaxWidth()) }
                            Lou(Modifier.size(112.dp, 98.dp), mood = lou.mood, mouthOpen = lou.mouth)
                        }
                    }
                    Column(Modifier.weight(1.1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)) {
                        if (teachStep) { lou.bubble?.let { LouBubble(it, Modifier.fillMaxWidth()) }; highLow() } else answers()
                    }
                } else Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (teachStep) {
                        louTeach(Modifier.fillMaxWidth().weight(1f))
                        highLow()
                    } else {
                        answers()
                        Spacer(Modifier.weight(1f))
                    }
                    // Lou + bubble (bottom)
                    Row(verticalAlignment = Alignment.Bottom) {
                        if (!teachStep) Lou(Modifier.size(92.dp, 80.dp), mood = lou.mood, mouthOpen = lou.mouth)
                        lou.bubble?.let { LouBubble(it, Modifier.weight(1f).padding(start = 6.dp, bottom = 10.dp)) }
                    }
                }
            }
            val barShown by animateFloatAsState(if (tray == null) 1f else 0f, androidx.compose.animation.core.tween(160), label = "bar")
            run {
                val isItem = step >= first
                val enabled = if (isItem) sel != null else heard.size == 2
                Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = if (land) 8.dp else 16.dp)
                    .graphicsLayer { alpha = barShown; translationY = (1f - barShown) * 12.dp.toPx() }) {
                    GelButton(if (isItem) "Check" else if (enabled) "Continue" else "Hear both to continue", {
                        if (tray != null) Unit else if (!isItem) { sfx.swoosh(); step++ } else {
                            val item = spec.items[step - first]; val ok = sel == right(item)
                            lastGood = ok; onAnswer(kindOf(item), ok)
                            // neutral (unpitched) feedback in ear training so a chime can't bias the next answer
                            if (ok) { sfx.swoosh(); lou.react(LouMood.Happy) } else { lou.react(LouMood.Encourage); helped = true }
                            val r = right(item)
                            val ik = kindOf(item)
                            val body = when {
                                ik == EarKind.ChordSpot -> when (r) {
                                    Ans.IV -> if (ok) "F — chord IV. Open and hopeful; it lifts away from home." else "That was F (IV): open, lifting away from home. Hear it again."
                                    Ans.V -> if (ok) "G — chord V. Hear how it wants to go back to C?" else "That was G (V): tense, it wants to resolve to C. Hear it again."
                                    else -> if (ok) "A minor — chord vi. Same notes as C, but sadder." else "That was A minor (vi): C's sad twin. Hear it again."
                                }
                                ik == EarKind.Contour -> if (ok) "Yes — the second note went ${r.name.lowercase()}." else "It went ${r.name.lowercase()}. Hear them slowly."
                                ik == EarKind.StepLeap -> if (r == Ans.Step) (if (ok) "A step — the next note over. Smooth, singable." else "That was a step: the very next note. Listen again.") else (if (ok) "A leap! Jumps add excitement." else "That was a leap — a jump over other notes. Hear it slowly.")
                                ik == EarKind.ChordQuality -> if (r == Ans.Major) (if (ok) "Major — bright. The middle note sits high." else "That was major: bright and open. Listen again.") else (if (ok) "Minor — darker. The middle note drops a semitone." else "That was minor: a little sad. Hear it slowly.")
                                r == Ans.Same -> if (ok) "Same pitch — just louder. Loudness isn't pitch." else "They were the same note, one louder. Listen again."
                                else -> if (ok) "Pad ${r.name} was higher." else "Pad ${r.name} is higher. Hear them side by side, slowly."
                            }
                            lou.say(body, if (ok) LouMood.Happy else LouMood.Encourage)
                            tray = TrayMsg(ok, if (ok) listOf("Nice ears!", "Spot on!", "You heard it!").random() else "Close — listen once more", body, if (ok) "Continue" else "Hear the difference")
                        }
                    }, Modifier.align(Alignment.CenterEnd).fillMaxWidth(if (land) 0.53f else 1f), gel = if (enabled) Gel.Grass else Gel.Off, enabled = enabled && tray == null, height = if (land) 42.dp else 46.dp)
                }
            }
        }
        ResultTray(tray, modifier = Modifier.align(Alignment.BottomCenter), onCta = { trayCta() })
        if (confirmExit) AlertDialog(containerColor = Color(0xFFF3FAFF), onDismissRequest = { confirmExit = false },
            confirmButton = { TextButton({ confirmExit = false; onExit() }) { Text("Leave") } },
            dismissButton = { TextButton({ confirmExit = false }) { Text("Keep going") } },
            title = { Text("Leave this lesson?", style = AeroType.H2) }, text = { Text("You can start it again from the path any time.", style = AeroType.Body) })
    }
}

@Composable
private fun Pad(label: String, selected: Boolean, result: Boolean?, playing: Boolean, modifier: Modifier, small: Boolean = false, correct: Boolean = false, onClick: () -> Unit) {
    val s by animateFloatAsState(if (playing) 1.05f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "p")
    val edge = when { selected && result == true -> AeroColors.Leaf; selected && result == false -> AeroColors.Amber; result == false && correct -> AeroColors.Leaf; selected -> AeroColors.Primary; else -> Color(0xCCFFFFFF) }
    if (small) {
        // short pad: small orb beside the label, so nothing gets cut off
        Row(modifier.scale(s).glass(24.dp).border(3.dp, edge, RoundedCornerShape(24.dp)).clickable(onClick = onClick)
            .semantics { contentDescription = "$label${if (selected) ", selected" else ""}" }, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).drawBehind {
                drawCircle(Brush.verticalGradient(listOf(Color(0xFF5FD4F5), Color(0xFF0B7FB8))))
                drawOval(Color(0x99FFFFFF), Offset(size.width * 0.18f, size.height * 0.06f), Size(size.width * 0.64f, size.height * 0.4f))
            }, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.GraphicEq, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.width(10.dp))
            Text(label, style = AeroType.H2)
        }
        return
    }
    Column(modifier.scale(s).glass(24.dp).border(3.dp, edge, RoundedCornerShape(24.dp)).clickable(onClick = onClick)
        .semantics { contentDescription = "$label${if (selected) ", selected" else ""}" }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(56.dp).drawBehind {
            drawCircle(Brush.verticalGradient(listOf(Color(0xFF5FD4F5), Color(0xFF0B7FB8))))
            drawOval(Color(0x99FFFFFF), Offset(size.width * 0.18f, size.height * 0.06f), Size(size.width * 0.64f, size.height * 0.4f))
        }, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.GraphicEq, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
        Spacer(Modifier.height(8.dp))
        Text(label, style = AeroType.H2, textAlign = TextAlign.Center)
    }
}

/** Lesson complete: Lou celebrates, XP counts up with little ticks. */
@Composable
fun LessonCompleteScreen(sfx: Sfx, xp: Int, unassisted: Boolean, seconds: Int, onContinue: () -> Unit, onAgain: () -> Unit, balance: Int = 0, onShop: (() -> Unit)? = null) {
    val lou = rememberLou(sfx)
    var shown by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        lou.react(LouMood.Cheer); delay(300)
        lou.say(if (unassisted) "Every answer on your own. That's real skill!" else "Done! A little help is how everyone learns.", LouMood.Cheer)
        for (i in 1..xp) { shown = i; if (i % 2 == 0) sfx.xpTick(i); delay(45) }
    }
    if (isLandscape()) AeroBackground {
        Row(Modifier.fillMaxSize().sideSafe().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Lou(Modifier.size(190.dp, 166.dp), mood = lou.mood, mouthOpen = lou.mouth)
                Spacer(Modifier.height(8.dp))
                lou.bubble?.let { LouBubble(it) }
            }
            Spacer(Modifier.width(24.dp))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Lesson complete!", style = AeroType.Hero.copy(color = Color.White))
                Row(Modifier.fillMaxWidth().glass(20.dp).padding(vertical = 12.dp)) {
                    Stat("XP", "+$shown", Modifier.weight(1f)); Stat("ON YOUR OWN", if (unassisted) "Yes" else "With help", Modifier.weight(1f))
                    Stat("TIME", if (seconds > 0) "%d:%02d".format(seconds / 60, seconds % 60) else "—", Modifier.weight(1f))
                }
                GelButton("Continue", onContinue, Modifier.fillMaxWidth(), gel = Gel.Grass, height = 44.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GelButton("Practice again", onAgain, Modifier.weight(1f), gel = Gel.Silver, height = 44.dp)
                    onShop?.let { GelButton("Shop · ★ $balance", it, Modifier.weight(1f), gel = Gel.Orange, height = 44.dp) }
                }
            }
        }
    } else AeroBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(20.dp))
            Lou(Modifier.size(240.dp, 210.dp), mood = lou.mood, mouthOpen = lou.mouth)
            Text("Lesson complete!", style = AeroType.Hero.copy(color = Color.White))
            Spacer(Modifier.height(10.dp))
            lou.bubble?.let { LouBubble(it) }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth().glass(20.dp).padding(vertical = 14.dp)) {
                Stat("XP", "+$shown", Modifier.weight(1f)); Stat("ON YOUR OWN", if (unassisted) "Yes" else "With help", Modifier.weight(1f))
                Stat("TIME", if (seconds > 0) "%d:%02d".format(seconds / 60, seconds % 60) else "—", Modifier.weight(1f))
            }
            Spacer(Modifier.weight(1f))
            GelButton("Continue", onContinue, Modifier.fillMaxWidth(), gel = Gel.Grass)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GelButton("Practice again", onAgain, Modifier.weight(1f), gel = Gel.Silver)
                onShop?.let { GelButton("Shop · ★ $balance", it, Modifier.weight(1f), gel = Gel.Orange) }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = AeroType.Caps)
        Text(value, style = AeroType.H2)
    }
}

@Suppress("unused") private fun bg(m: Modifier) = m.background(Color.Transparent)
