package com.beatpath.app.ui.lesson

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import com.beatpath.app.audio.AudioEngine
import com.beatpath.app.audio.Sfx
import com.beatpath.app.lessons.Basics
import com.beatpath.app.lessons.Basics.Kind
import com.beatpath.app.lessons.Basics.TapMode
import com.beatpath.app.lessons.Basics.Visual
import com.beatpath.app.lessons.Verdict
import com.beatpath.app.model.ChannelKind
import com.beatpath.app.ui.aero.AeroBackground
import com.beatpath.app.ui.aero.Gel
import com.beatpath.app.ui.aero.GelButton
import com.beatpath.app.ui.aero.LouBubble
import com.beatpath.app.ui.aero.ResultTray
import com.beatpath.app.ui.aero.TrayMsg
import com.beatpath.app.ui.aero.glass
import com.beatpath.app.ui.aero.claySurface
import com.beatpath.app.ui.aero.isLandscape
import com.beatpath.app.ui.aero.sideSafe
import com.beatpath.app.ui.lou.Lou
import com.beatpath.app.ui.lou.LouMood
import com.beatpath.app.ui.lou.rememberLou
import com.beatpath.app.ui.theme.AeroColors
import com.beatpath.app.ui.theme.AeroType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Music basics: Lou explains one idea per page on a rainbow keyboard, a beat bar or a small Piano roll. */
@Composable
fun BasicsLessonScreen(lesson: Basics.BasicsLesson, engine: AudioEngine, sfx: Sfx, onExit: () -> Unit, onFinished: (Boolean, String, Int) -> Unit) {
    val scope = rememberCoroutineScope()
    val lou = rememberLou(sfx)
    val attempt = remember { "${lesson.id}-${System.currentTimeMillis()}" }
    val started = remember { System.currentTimeMillis() }
    var idx by remember { mutableIntStateOf(0) }
    val page = lesson.pages[idx]
    var picked by remember { mutableStateOf(listOf<Int>()) }
    var choice by remember { mutableStateOf<Int?>(null) }
    var cells by remember { mutableStateOf(setOf<Pair<Int, Int>>()) }
    var taps by remember { mutableIntStateOf(0) }
    var hintLevel by remember { mutableIntStateOf(0) }
    var wrongKey by remember { mutableStateOf<Int?>(null) }
    var sounding by remember { mutableStateOf(setOf<Int>()) }
    var beat by remember { mutableIntStateOf(-1) }
    var tray by remember { mutableStateOf<TrayMsg?>(null) }
    var lastGood by remember { mutableStateOf(false) }
    var helped by remember { mutableStateOf(false) }
    var confirmExit by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    var tuneJob by remember { mutableStateOf<Job?>(null) }

    fun stopTune() { tuneJob?.cancel(); tuneJob = null; beat = -1; engine.quietSfx = false }
    // never leave the app's sounds muted, whatever way the lesson ends
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { tuneJob?.cancel(); engine.quietSfx = false } }

    fun sound(notes: List<Int>, dur: Float) = scope.launch {
        sounding = sounding + notes; delay((dur * 1000).toLong().coerceAtLeast(180)); sounding = sounding - notes.toSet()
    }
    fun playTune(t: Basics.Tune) {
        stopTune()
        tuneJob = scope.launch {
            try {
                engine.quietSfx = true; lou.mood = LouMood.Listen
                var clock = 0; var drumIdx = 0
                for (h in t.hits.sortedBy { it.atMs }) {
                    delay((h.atMs - clock).toLong()); clock = h.atMs
                    if (h.drum) { engine.playDrum(if (h.notes.first() == 0) ChannelKind.KICK else ChannelKind.HAT, 0.85f); beat = drumIdx % 4; drumIdx++ }
                    else { h.notes.forEach { engine.playNote(it, h.dur, 0.72f, AudioEngine.Timbre.KEYS) }; sound(h.notes, h.dur) }
                }
                delay(600)
                if (lou.mood == LouMood.Listen) lou.mood = LouMood.Idle
            } finally { beat = -1; engine.quietSfx = false }
        }
    }
    fun finishPage(v: Verdict) {
        stopTune(); checking = false
        lastGood = v.pass
        if (v.pass) { sfx.correct(); lou.react(LouMood.Happy); lou.say(listOf("Nice!", "Yes!", "You got it!", "That's it!").random(), LouMood.Happy) }
        else { sfx.miss(); helped = true; lou.react(LouMood.Encourage); lou.say("Close! Have another go.", LouMood.Encourage) }
        tray = TrayMsg(v.pass, v.headline, v.body, if (v.pass) "Continue" else "Try again")
    }
    /** Clear everything for the page about to show, in the same frame, so nothing from the last page flashes. */
    fun enterPage(i: Int) {
        stopTune()
        picked = emptyList(); choice = null; cells = emptySet(); taps = 0; hintLevel = 0; wrongKey = null; tray = null; checking = false
        idx = i
    }
    fun next() {
        if (idx + 1 >= lesson.pages.size) { stopTune(); onFinished(!helped, attempt, ((System.currentTimeMillis() - started) / 1000).toInt()) }
        else { sfx.swoosh(); enterPage(idx + 1) }
    }
    /** After a miss: single picks start fresh, while built answers (chords, notes in the roll) stay so you can fix them. */
    fun retry() {
        tray = null
        when { page.kind == Kind.TAP && page.mode != TapMode.CHORD -> picked = emptyList(); page.kind == Kind.CHOOSE -> choice = null }
        lou.say(page.hint.ifEmpty { page.say }, LouMood.Encourage)
        page.tune?.takeIf { page.kind == Kind.CHOOSE }?.let { playTune(it) }
    }
    BackHandler { when { tray != null && !lastGood -> retry(); tray != null -> next(); else -> confirmExit = true } }

    fun onKey(k: Int) {
        if (tray != null || checking) return
        engine.playNote(k, 0.55f, 0.75f, AudioEngine.Timbre.KEYS); sound(listOf(k), 0.4f)
        when (page.kind) {
            Kind.SHOW -> taps++
            Kind.TAP -> when (page.mode) {
                TapMode.ONE -> picked = listOf(k)
                TapMode.CHORD -> picked = if (k in picked) picked - k else picked + k
                TapMode.SEQUENCE -> if (Basics.sequenceOk(page, picked, k)) {
                    picked = picked + k
                    if (picked.size == page.target.size) { checking = true; scope.launch { delay(350); finishPage(Basics.judgeTap(page, picked)) } }
                } else {
                    wrongKey = k; helped = true; sfx.miss(); lou.react(LouMood.Encourage)
                    val want = Basics.nextInSequence(page, picked)
                    lou.say(if (picked.isEmpty()) "Start on ${want?.let { Basics.name(it) }}." else "Not ${Basics.name(k)}. ${page.hint}", LouMood.Encourage)
                    scope.launch { delay(500); if (wrongKey == k) wrongKey = null }
                }
            }
            else -> {}
        }
    }

    LaunchedEffect(idx) {
        lou.say(page.say, if (idx == 0) LouMood.Happy else LouMood.Idle)
        page.tune?.let { delay(650); playTune(it) }
    }

    val land = isLandscape()
    val total = lesson.pages.size
    val progress by androidx.compose.animation.core.animateFloatAsState((idx + if (tray?.good == true) 1 else 0) / total.toFloat(),
        spring(dampingRatio = 1f, stiffness = 120f), label = "progress")
    AeroBackground {
        Column(Modifier.fillMaxSize().sideSafe().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = if (land) 4.dp else 8.dp), verticalAlignment = Alignment.CenterVertically) {
                com.beatpath.app.ui.aero.ClayIconButton(Icons.Rounded.Close, "Leave lesson", { confirmExit = true }, size = 40.dp, iconSize = 20.dp)
                Spacer(Modifier.width(8.dp))
                if (land) Text(lesson.title, style = AeroType.H2.copy(color = AeroColors.Ink), modifier = Modifier.padding(end = 14.dp), maxLines = 1)
                Box(Modifier.weight(1f).height(16.dp).semantics { contentDescription = "Page ${idx + 1} of $total" }.drawBehind {
                    val r = CornerRadius(size.height / 2)
                    drawRoundRect(Color(0x66FFFFFF), cornerRadius = r)
                    val w = size.width * progress.coerceAtLeast(0.04f)
                    drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF9BE15A), Color(0xFF2F8F1C))), size = Size(w, size.height), cornerRadius = r)
                    drawRoundRect(Color(0x88FFFFFF), Offset(4f, 2f), Size((w - 8f).coerceAtLeast(0f), size.height * 0.35f), r)
                    drawRoundRect(Color(0xAAFFFFFF), cornerRadius = r, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
                })
            }
            if (!land) Text(lesson.title, style = AeroType.Title.copy(color = AeroColors.Ink), modifier = Modifier.padding(horizontal = 20.dp))

            // Lou and his bubble live outside the page animation, in a strip of fixed height: what he says never moves the keys
            val talkStrip: @Composable (Modifier) -> Unit = { m ->
                // tall enough for Lou's longest line (4 lines); anything longer scrolls inside the bubble instead of being cut off
                Row(m.height(if (land) 104.dp else 124.dp), verticalAlignment = Alignment.Bottom) {
                    Lou(Modifier.size(if (land) 70.dp else 76.dp, if (land) 61.dp else 66.dp), mood = lou.mood, mouthOpen = lou.mouth)
                    Box(Modifier.weight(1f).fillMaxHeight().padding(start = 4.dp, bottom = 4.dp), contentAlignment = Alignment.BottomStart) {
                        lou.bubble?.let { LouBubble(it, Modifier.heightIn(max = if (land) 100.dp else 120.dp).verticalScroll(rememberScrollState())) }
                    }
                }
            }
            // part 0 = whole page (portrait), 1 = words and answers (landscape left), 2 = keyboard or roll (landscape right)
            val pageBody: @Composable (Int, Int) -> Unit = { i, part ->
                val landscape = part != 0
                val p = lesson.pages[i]
                val live = i == idx
                val hintKeys = if (live && hintLevel >= 2 && p.kind == Kind.TAP) (if (p.mode == TapMode.SEQUENCE) listOfNotNull(Basics.nextInSequence(p, picked)) else p.target).toSet() else emptySet()
                val visual: @Composable (Modifier) -> Unit = { m ->
                    when (p.visual) {
                        Visual.KEYS -> RainbowKeys(p.keys, if (live) picked.toSet() else emptySet(), if (live) sounding else emptySet(), hintKeys, if (live) wrongKey else null,
                            m.heightIn(max = if (landscape) 210.dp else 230.dp)) { if (live) onKey(it) }
                        Visual.BEATS -> BeatBar(p.beats, if (live) beat else -1, m.heightIn(max = 260.dp))
                        Visual.ROLL -> MiniRoll(p.roll, if (live) cells else emptySet(), if (live && hintLevel >= 2) p.cells else emptySet(), if (live) sounding else emptySet(), m,
                            if (p.kind == Kind.ROLL) { pitch, b ->
                                if (live && tray == null) {
                                    val c = pitch to b
                                    if (c in cells) { cells = cells - c; sfx.toggleOff() }
                                    else { cells = cells + c; engine.playNote(pitch, 0.45f, 0.75f, AudioEngine.Timbre.KEYS); sound(listOf(pitch), 0.35f) }
                                }
                            } else null)
                        Visual.NONE -> Box(m)
                    }
                }
                val controls: @Composable () -> Unit = {
                    if (p.task.isNotEmpty()) Text(p.task, style = AeroType.H2.copy(fontSize = 18.sp), modifier = Modifier.padding(horizontal = 4.dp))
                    if (p.kind == Kind.TAP && p.mode == TapMode.SEQUENCE)
                        Text("${if (live) picked.size else 0} of ${p.target.size} notes", style = AeroType.Label.copy(color = AeroColors.PrimaryDeep), modifier = Modifier.padding(horizontal = 4.dp))
                    if (p.kind == Kind.CHOOSE) Options(p.options, if (live) choice else null, if (live) tray?.good else null, p.answer) { if (live && tray == null) { choice = it; sfx.select() } }
                    p.tune?.let { t -> GelButton(if (p.visual == Visual.BEATS) "↻  Play again" else "↻  Hear it again", { if (live) playTune(t) }, Modifier.fillMaxWidth(), gel = Gel.Silver, height = 40.dp) }
                }
                when (part) {
                    1 -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top)) { controls() }
                    2 -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (p.visual != Visual.NONE) visual(Modifier.fillMaxWidth().then(if (p.visual == Visual.ROLL) Modifier.fillMaxHeight() else Modifier.height(200.dp)))
                    }
                    else -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        controls()
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            if (p.visual != Visual.NONE) visual(Modifier.fillMaxWidth().then(if (p.visual == Visual.ROLL) Modifier.fillMaxHeight() else Modifier.height(220.dp)))
                        }
                    }
                }
            }
            val pages: @Composable (Modifier, Int) -> Unit = { m, part ->
                AnimatedContent(idx, transitionSpec = { (slideInHorizontally(spring(dampingRatio = 1f, stiffness = 320f)) { it / 5 } + fadeIn(tween(240, delayMillis = 60))) togetherWith fadeOut(tween(120)) },
                    label = "page$part", modifier = m) { i -> pageBody(i, part) }
            }
            if (land) Row(Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(0.95f).fillMaxHeight()) {
                    talkStrip(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    pages(Modifier.weight(1f).fillMaxWidth(), 1)
                }
                pages(Modifier.weight(1.05f).fillMaxHeight(), 2)
            } else Column(Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 6.dp)) {
                talkStrip(Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                pages(Modifier.weight(1f).fillMaxWidth(), 0)
            }

            // the button bar always keeps its space; while the tray is up it fades out instead of leaving a hole
            val barShown by androidx.compose.animation.core.animateFloatAsState(if (tray == null) 1f else 0f, tween(160), label = "bar")
            val canHint = page.kind != Kind.SHOW && page.hint.isNotEmpty()
            val hintMax = if (page.kind == Kind.CHOOSE) 1 else 2
            val (label, enabled) = when (page.kind) {
                Kind.SHOW -> if (taps < page.needTaps) "Tap ${page.needTaps - taps} more key${if (page.needTaps - taps > 1) "s" else ""}" to false else "Continue" to true
                Kind.TAP -> if (page.mode == TapMode.SEQUENCE) "Start over" to picked.isNotEmpty() else "Check" to picked.isNotEmpty()
                Kind.CHOOSE -> "Check" to (choice != null)
                Kind.ROLL -> "Check" to cells.isNotEmpty()
            }
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = if (land) 6.dp else 14.dp)
                .graphicsLayer { alpha = barShown; translationY = (1f - barShown) * 12.dp.toPx() },
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                val live = tray == null && !checking
                if (canHint) GelButton(if (hintLevel >= 1 && hintMax > 1) "Show me" else "Hint", {
                    if (live) { helped = true; hintLevel++; lou.react(LouMood.Think)
                        lou.say(if (hintLevel >= 2) "Follow the gold outline." else page.hint, LouMood.Think) }
                }, Modifier.width(if (land) 130.dp else 116.dp), gel = Gel.Silver, enabled = live && hintLevel < hintMax, height = if (land) 42.dp else 46.dp)
                val seq = page.kind == Kind.TAP && page.mode == TapMode.SEQUENCE
                GelButton(label, {
                    if (live) when (page.kind) {
                        Kind.SHOW -> next()
                        Kind.TAP -> if (seq) { picked = emptyList(); sfx.toggleOff() } else finishPage(Basics.judgeTap(page, picked))
                        Kind.CHOOSE -> finishPage(Basics.judgeChoice(page, choice))
                        Kind.ROLL -> finishPage(Basics.judgeRoll(page, cells))
                    }
                }, if (land) Modifier.width(260.dp) else Modifier.weight(1f), gel = when { !enabled -> Gel.Off; seq -> Gel.Silver; else -> Gel.Grass },
                    enabled = enabled && live, height = if (land) 42.dp else 46.dp)
            }
        }
        ResultTray(tray, modifier = Modifier.align(Alignment.BottomCenter), onCta = { if (lastGood) next() else retry() })
        if (confirmExit) com.beatpath.app.ui.aero.LeaveDialog(sfx, onLeave = { confirmExit = false; stopTune(); onExit() }, onStay = { confirmExit = false })
    }
}

/** Answer buttons: side by side when short, stacked when long. */
@Composable
private fun Options(options: List<String>, choice: Int?, result: Boolean?, answer: Int, onPick: (Int) -> Unit) {
    val short = options.all { it.length <= 9 }
    val item: @Composable (Int, Modifier) -> Unit = { i, m ->
        val sel = choice == i
        val edge = when { sel && result == true -> AeroColors.Leaf; sel && result == false -> AeroColors.Amber; result == false && i == answer -> AeroColors.Leaf; sel -> AeroColors.Primary; else -> Color(0xCCFFFFFF) }
        Box(m.heightIn(min = 52.dp).claySurface(Gel.Cream, 18.dp, depth = 4.dp, outline = edge.takeIf { it != Color(0xCCFFFFFF) }).clip(RoundedCornerShape(18.dp)).clickable { onPick(i) }
            .semantics { contentDescription = "${options[i]}${if (sel) ", selected" else ""}" }.padding(horizontal = 10.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
            Text(options[i], style = AeroType.H2.copy(fontSize = 17.sp, color = if (sel) AeroColors.PrimaryDeep else AeroColors.Ink), textAlign = TextAlign.Center)
        }
    }
    if (short) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { options.indices.forEach { item(it, Modifier.weight(1f)) } }
    else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { options.indices.forEach { item(it, Modifier.fillMaxWidth()) } }
}
