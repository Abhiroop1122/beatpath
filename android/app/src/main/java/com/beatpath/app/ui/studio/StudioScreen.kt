package com.beatpath.app.ui.studio

import com.beatpath.app.model.SoundPresets
import android.widget.Toast
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.MoreVert
import com.beatpath.app.ui.aero.isLandscape
import com.beatpath.app.ui.aero.sideSafe
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Piano
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatpath.app.audio.AudioEngine
import com.beatpath.app.audio.Sfx
import com.beatpath.app.data.MidiExport
import com.beatpath.app.data.WavExport
import com.beatpath.app.lessons.Ghost
import com.beatpath.app.lessons.Lesson
import com.beatpath.app.lessons.Meters
import com.beatpath.app.lessons.Snap
import com.beatpath.app.lessons.Win
import com.beatpath.app.model.ChannelKind
import com.beatpath.app.model.Pattern
import com.beatpath.app.model.Project
import com.beatpath.app.model.TICKS_PER_BEAT
import com.beatpath.app.model.TICKS_PER_STEP
import com.beatpath.app.ui.aero.Gel
import com.beatpath.app.ui.aero.GelButton
import com.beatpath.app.ui.aero.LouBubble
import com.beatpath.app.ui.aero.LouOrb
import com.beatpath.app.ui.aero.ResultTray
import com.beatpath.app.ui.aero.TrayMsg
import com.beatpath.app.ui.aero.glass
import com.beatpath.app.ui.fl.Fl
import com.beatpath.app.ui.fl.FlTransport
import com.beatpath.app.ui.lou.Lou
import com.beatpath.app.ui.lou.LouMood
import com.beatpath.app.ui.lou.rememberLou
import com.beatpath.app.ui.theme.AeroType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The Studio, kept simple: FL's Channel Rack and Piano roll, a transport, and Lou as the coach.
 * Lessons drive everything; free play gives the same two windows with every channel.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun StudioScreen(
    engine: AudioEngine,
    sfx: Sfx,
    initial: Project,
    lesson: Lesson?,
    onSave: (Project) -> Unit,
    onExit: () -> Unit,
    onLessonDone: (lessonId: String, attemptId: String, unassisted: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val lou = rememberLou(sfx)
    val start = remember { if (lesson == null) initial else lesson.setup(initial.copy(bpm = lesson.bpm, pattern = Pattern(lesson.bars))) }
    var project by remember { mutableStateOf(start) }
    val undo = remember { mutableStateListOf<Project>() }
    val redo = remember { mutableStateListOf<Project>() }
    var playing by remember { mutableStateOf(false) }
    var metronome by remember { mutableStateOf(lesson != null && lesson.level <= 3) }
    var playhead by remember { mutableIntStateOf(-1) }
    var beatPhase by remember { mutableFloatStateOf(0f) }
    var flSheet by remember { mutableStateOf<FlInfo?>(null) }
    var coachOpen by remember { mutableStateOf(true) }
    var tempoDialog by remember { mutableStateOf(false) }
    var velTarget by remember { mutableStateOf<Pair<ChannelKind, Int>?>(null) }
    var exportMenu by remember { mutableStateOf(false) }
    var leaveDialog by remember { mutableStateOf(false) }

    // ---- lesson state ----
    val attemptId = remember { "${lesson?.id}-${System.currentTimeMillis()}" }
    var stepIdx by remember { mutableIntStateOf(0) }
    val step = lesson?.steps?.getOrNull(stepIdx)
    var win by remember { mutableStateOf(if (step?.window == Win.ROLL) Win.ROLL else Win.RACK) }
    var focus by remember { mutableStateOf(step?.channel ?: ChannelKind.KICK) }
    var tray by remember { mutableStateOf<TrayMsg?>(null) }
    var lastPass by remember { mutableStateOf(false) }
    var hintLevel by remember { mutableIntStateOf(0) }
    var usedHelp by remember { mutableStateOf(false) }
    var loops by remember { mutableIntStateOf(0) }
    var introOpen by remember { mutableStateOf(lesson != null) }
    val track = remember(lesson) { lesson?.let { l -> com.beatpath.app.lessons.Curriculum.levels.firstOrNull { it.number == l.level }?.track } ?: com.beatpath.app.lessons.Track.MELODY }

    val visible = lesson?.show ?: listOf(ChannelKind.KICK, ChannelKind.SNARE, ChannelKind.CLAP, ChannelKind.HAT, ChannelKind.OPEN_HAT, ChannelKind.BASS, ChannelKind.CHORDS, ChannelKind.LEAD)
    val locked = lesson?.locked ?: emptySet()

    fun edit(p: Project) { if (p == project) return; undo.add(project); if (undo.size > 120) undo.removeAt(0); redo.clear(); project = p }
    fun editPattern(pat: Pattern) = edit(project.copy(pattern = pat))

    LaunchedEffect(stepIdx) {
        val s = step ?: return@LaunchedEffect
        s.setup?.let { project = it(project) }
        win = if (s.window == Win.ROLL) Win.ROLL else Win.RACK; focus = s.channel; hintLevel = 0; tray = null; loops = 0
        lou.say(s.say, if (introOpen && stepIdx == 0) LouMood.Happy else LouMood.Idle)
    }
    LaunchedEffect(Unit) { if (lesson == null) lou.say("Free play! Tap steps for a beat, or open the Piano roll.", LouMood.Happy) }
    LaunchedEffect(project.pattern, project.bpm, playing) { if (playing) engine.update(project.pattern, project.bpm) }
    LaunchedEffect(project.mix) { engine.mix = project.mix }
    LaunchedEffect(metronome) { engine.setMetronome(metronome) }
    LaunchedEffect(playing) {
        if (!playing) { playhead = -1; beatPhase = 0f; return@LaunchedEffect }
        var last = -1
        while (true) {
            withFrameNanos { }
            val t = engine.playheadTick()
            if (t >= 0 && last >= 0 && t < last) loops++
            if (t >= 0) last = t
            playhead = t
            beatPhase = if (t >= 0) (t % TICKS_PER_BEAT) / TICKS_PER_BEAT.toFloat() else 0f
        }
    }

    var saved by remember { mutableStateOf(start) }
    /** Leaving: lessons with progress ask first; free play with unsaved changes offers to save. */
    fun requestExit() {
        val lessonProgress = lesson != null && !introOpen && (stepIdx > 0 || undo.isNotEmpty())
        val unsaved = lesson == null && project != saved && project.pattern.channels.any { it.notes.isNotEmpty() }
        if (lessonProgress || unsaved) { leaveDialog = true; sfx.tap() } else { engine.stop(); playing = false; onExit() }
    }

    fun togglePlay() { if (playing) { engine.stop(); playing = false } else { engine.play(project.pattern, project.bpm); playing = true }; sfx.tap() }
    fun stopAll() { engine.stop(); playing = false }
    // Back closes the topmost thing first: result tray → (then) leave the screen
    BackHandler { if (tray != null) tray = null else requestExit() }

    fun check() {
        val s = step ?: return
        if (s.needsLoops > 0 && loops < s.needsLoops) {
            if (!playing) togglePlay()
            lou.say("Let's listen first — I'll check after one loop.", LouMood.Listen); return
        }
        val v = s.check(project, Meters(playedLoops = loops))
        lastPass = v.pass
        val last = stepIdx == lesson!!.steps.lastIndex
        if (v.pass) {
            // every success teaches one more thing: FL tip on the final step, otherwise alternating theory / FL tips
            val tip = if (last) "FL tip: ${lesson.flTip}" else com.beatpath.app.lessons.Tips.next(track)
            sfx.correct(); lou.react(if (last) LouMood.Cheer else LouMood.Happy); lou.say(v.headline, lou.mood)
            tray = TrayMsg(true, v.headline, v.body, if (last) "Finish lesson" else "Next step", tip = tip)
        } else {
            sfx.miss(); lou.react(LouMood.Encourage); lou.say(v.body, LouMood.Encourage); usedHelp = true
            tray = TrayMsg(false, v.headline, v.body, "Try again", tip = "Stuck? Tap Hint — I'll explain it another way, then show gold outlines.")
        }
    }

    val midiLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/midi")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ok = runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(MidiExport.pattern(project)) } ?: error("no stream") }.isSuccess
        lou.say(if (ok) "Saved! In FL: open a channel's Piano roll → File → Import MIDI file." else "Export didn't work — your loop is still safe here.", if (ok) LouMood.Happy else LouMood.Encourage)
    }
    val wavLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/wav")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        lou.say("Turning your loop into a WAV…", LouMood.Think)
        val snapshot = project
        scope.launch {
            val ok = runCatching {
                val bytes = withContext(Dispatchers.Default) { WavExport.render(snapshot) }
                withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("no stream") }
            }.isSuccess
            lou.say(if (ok) "WAV saved — share it with anyone!" else "The WAV export failed — your loop is safe.", if (ok) LouMood.Cheer else LouMood.Encourage)
        }
    }

    val ghosts: List<Ghost> = buildList {
        step?.guides?.invoke(project)?.let { addAll(it) }
        if (hintLevel >= 2) step?.hintGhosts?.let { addAll(it) }
    }
    val rollChannel = when {
        focus.isDrum && lesson?.rows?.let { it.first == it.last } == true -> focus
        focus.isDrum -> visible.firstOrNull { !it.isDrum } ?: ChannelKind.LEAD
        else -> focus
    }

    val land = isLandscape()
    val terms = com.beatpath.app.lessons.Glossary.find(listOfNotNull(lou.bubble, step?.task).joinToString(" "))
    val title = lesson?.title ?: project.name
    val subtitle = lesson?.let { "${if (it.level == 0) "Practice" else "Level ${it.level}"} · step ${stepIdx + 1} of ${it.steps.size}" } ?: "${project.pattern.bars} bar${if (project.pattern.bars > 1) "s" else ""} · free play"
    fun saveNow() { onSave(project); saved = project; sfx.select(); Toast.makeText(context, "Saved to My loops", Toast.LENGTH_SHORT).show() }
    fun exportOrNudge() { if (project.pattern.channels.all { it.notes.isEmpty() }) lou.say("Add some notes first, then export.", LouMood.Think) else exportMenu = true }
    fun hint(s: com.beatpath.app.lessons.LessonStep) {
        hintLevel++; usedHelp = true; sfx.select(); lou.react(LouMood.Think)
        lou.say(when {
            hintLevel == 1 -> s.hint
            s.hintGhosts.isNotEmpty() -> "Look for the gold outlines — they show exactly where. Tap inside each one. ${s.hint}"
            else -> "Here's the idea again in plain words: ${lesson!!.idea} ${s.hint}"
        }, LouMood.Think)
    }

    val windowArea: @Composable (Modifier) -> Unit = { m ->
        Box(m) {
            val mod = Modifier.fillMaxSize()
            if (win == Win.ROLL) FlRoll(
                pattern = project.pattern, channel = rollChannel,
                rows = lesson?.rows ?: if (rollChannel == ChannelKind.BASS) 28..60 else 52..84, scale = lesson?.scale,
                ghostChannels = lesson?.ghost ?: emptySet(), ghosts = ghosts, playheadTick = playhead,
                locked = rollChannel in locked, defaultSnap = lesson?.snap ?: Snap.BEAT, engine = engine,
                onEdit = { editPattern(it) }, onInfo = { key -> flSheet = FlGuide.items[key]; sfx.tap() }, modifier = mod,
                channelChoices = visible.filter { !it.isDrum }, onChannel = { focus = it },
                showVelocity = lesson?.id == "roll.melody-dynamics", compact = land,
                soundName = if (rollChannel.isDrum) null else SoundPresets.nameOf(rollChannel, project.mix.synth(rollChannel)),
                soundChoices = SoundPresets.forKind(rollChannel).map { it.name },
                onSound = { name ->
                    SoundPresets.forKind(rollChannel).firstOrNull { it.name == name }?.let { pr ->
                        val m = project.mix.withSynth(rollChannel) { pr.patch }
                        edit(project.copy(mix = m)); engine.mix = m
                        if (!playing) {
                            val root = if (rollChannel == ChannelKind.BASS) 36 else 60
                            (if (rollChannel == ChannelKind.CHORDS) listOf(0, 4, 7) else listOf(0)).forEach { iv -> engine.playNote(root + iv, 0.6f, 0.8f, channel = rollChannel) }
                        }
                        lou.say("${pr.name} on ${rollChannel.label}. Same notes, new sound — in FL you'd load a different plugin or preset on the channel.", LouMood.Happy)
                    }
                },
            ) else FlRack(
                pattern = project.pattern, visible = visible, selected = focus,
                playheadStep = if (playhead >= 0) playhead / TICKS_PER_STEP else -1, ghosts = ghosts,
                onToggle = { k, s ->
                    if (k in locked) { lou.say("${k.label} is part of the backing in this lesson.", LouMood.Think); return@FlRack }
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    val on = !project.pattern.channel(k).stepOn(s)
                    editPattern(project.pattern.toggleStep(k, s))
                    if (on) { if (!playing) engine.playDrum(k) } else sfx.toggleOff()
                },
                onVelocity = { k, s -> if (k !in locked && project.pattern.channel(k).stepOn(s)) velTarget = k to s },
                onSelect = { focus = it },
                onOpenRoll = { focus = it; win = Win.ROLL; sfx.swoosh() },
                onMute = { k -> editPattern(project.pattern.update(k) { it.copy(muted = !it.muted) }); sfx.toggleOn() },
                onChannelVolume = { k, v -> editPattern(project.pattern.update(k) { it.copy(volume = v) }) },
                onChannelPan = { k, v -> editPattern(project.pattern.update(k) { it.copy(pan = v) }) },
                onInfo = { flSheet = FlGuide.items["rack"]; sfx.tap() },
                swing = project.pattern.swing, onSwing = { editPattern(project.pattern.copy(swing = it)) },
                lockedKinds = locked, showSwing = lesson == null || lesson.id == "rack.swing", modifier = mod,
                kit = project.mix.drumKit,
                onKit = { k ->
                    val m = project.mix.copy(drumKit = k)
                    edit(project.copy(mix = m)); engine.mix = m
                    if (!playing) { engine.playDrum(ChannelKind.KICK); engine.playDrum(ChannelKind.HAT, 0.7f) }
                    lou.say("${k.label} kit: ${k.blurb} The rhythm stays the same — only the drum sounds change.", LouMood.Happy)
                },
            )
        }
    }
    val metroButton: @Composable () -> Unit = {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(if (metronome) Color(0x55A9E34B) else Fl.Dark).border(1.dp, Fl.Edge, RoundedCornerShape(10.dp))
            .clickable { metronome = !metronome; sfx.toggleOn() }.semantics { contentDescription = "Metronome ${if (metronome) "on" else "off"}" }, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Timer, null, tint = if (metronome) Fl.Lime else Fl.Text2, modifier = Modifier.size(22.dp))
        }
    }
    val windowTabs: @Composable () -> Unit = {
        Row(Modifier.clip(RoundedCornerShape(10.dp)).background(Fl.Dark).border(1.dp, Fl.Edge, RoundedCornerShape(10.dp)).padding(3.dp)) {
            SegTab(Icons.Rounded.GridOn, "Rack", "Channel rack (F6)", win == Win.RACK, showLabel = !land) { win = Win.RACK; sfx.tap() }
            SegTab(Icons.Rounded.Piano, "Roll", "Piano roll (F7)", win == Win.ROLL, showLabel = !land) { win = Win.ROLL; sfx.tap() }
        }
    }
    val exportItems: @Composable () -> Unit = {
        DropdownMenuItem(text = { Text("MIDI for FL Studio (.mid)") }, onClick = { exportMenu = false; midiLauncher.launch("${project.name} - ${project.bpm} BPM.mid") })
        DropdownMenuItem(text = { Text("Audio to share (.wav)") }, onClick = { exportMenu = false; wavLauncher.launch("${project.name} - ${project.bpm} BPM.wav") })
    }
    val offTask = step != null && lou.bubble != step.say
    val chips: @Composable () -> Unit = {
        // tap a music word to have Lou explain it; after a hint or explanation, one tap brings the task back
        if (terms.isNotEmpty() || offTask) FlowRow(Modifier.padding(top = 6.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (offTask) Text("↩ Back to the task", style = AeroType.Small.copy(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFF1C7FC4)).clickable { sfx.tap(); lou.say(step!!.say, LouMood.Idle) }
                    .padding(horizontal = 10.dp, vertical = 8.dp).semantics { contentDescription = "Back to the task: show the instructions again" })
            terms.forEach { term ->
                Text("? ${term.word}", style = AeroType.Small.copy(color = Color(0xFF0A548F), fontSize = 11.sp),
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0x332E9BE6)).clickable { sfx.select(); lou.say(term.meaning, LouMood.Think) }
                        .padding(horizontal = 10.dp, vertical = 8.dp).semantics { contentDescription = "What does ${term.word} mean?" })
            }
        }
    }
    val taskLine: @Composable () -> Unit = { step?.let { Text("▸ ${it.task}", style = AeroType.Label.copy(color = Color(0xFF0A548F)), modifier = Modifier.padding(top = 4.dp)) } }

    Box(Modifier.fillMaxSize().background(Fl.Deep)) {
        if (land) Row(Modifier.fillMaxSize().sideSafe().statusBarsPadding()) {
            // ---------- sideways: FL on the left, Lou's coach panel on the right ----------
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TopIcon(Icons.Rounded.ChevronLeft, "Back") { requestExit() }
                    FlTransport(playing, project.bpm, onPlay = { togglePlay() }, onStop = { stopAll(); sfx.tap() }, onTempo = { tempoDialog = true })
                    metroButton()
                    windowTabs()
                    if (!coachOpen) Text(title, style = AeroType.Studio.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 6.dp))
                    else Spacer(Modifier.weight(1f))
                    TopIcon(Icons.AutoMirrored.Rounded.Undo, "Undo", undo.isNotEmpty()) { redo.add(project); project = undo.removeAt(undo.lastIndex) }
                    TopIcon(Icons.AutoMirrored.Rounded.Redo, "Redo", redo.isNotEmpty()) { undo.add(project); project = redo.removeAt(redo.lastIndex) }
                    Box {
                        TopIcon(Icons.Rounded.MoreVert, "Save or export") { exportMenu = true }
                        DropdownMenu(exportMenu, { exportMenu = false }) {
                            DropdownMenuItem(text = { Text("Save to My loops") }, onClick = { exportMenu = false; saveNow() })
                            if (project.pattern.channels.any { it.notes.isNotEmpty() }) exportItems()
                        }
                    }
                    if (!coachOpen) LouOrb(lou, { coachOpen = true; sfx.tap() }, Modifier.size(44.dp), beatPhase = if (playing) beatPhase else null)
                }
                windowArea(Modifier.weight(1f).fillMaxWidth().navigationBarsPadding())
            }
            if (coachOpen) Column(Modifier.width(268.dp).fillMaxHeight().navigationBarsPadding().padding(6.dp).glass(20.dp).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Lou(Modifier.size(58.dp, 50.dp), mood = lou.mood, mouthOpen = lou.mouth, beatPhase = if (playing) beatPhase else null)
                    Column(Modifier.weight(1f).padding(start = 4.dp)) {
                        Text(title, style = AeroType.H2.copy(fontSize = 15.sp, lineHeight = 18.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(subtitle, style = AeroType.Small.copy(fontSize = 11.sp), maxLines = 1)
                    }
                    Icon(Icons.Rounded.ChevronRight, "Hide Lou", tint = Color(0xFF35546B), modifier = Modifier.size(36.dp).clip(CircleShape).clickable { coachOpen = false; sfx.tap() }.padding(6.dp))
                }
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                    lou.bubble?.let { LouBubble(it) }
                    taskLine(); chips()
                }
                if (step != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GelButton(if (playing) "Stop" else "Listen", { togglePlay() }, Modifier.weight(1f), gel = Gel.Silver, height = 38.dp)
                        GelButton(if (hintLevel == 0) "Hint" else "More help", { hint(step) }, Modifier.weight(1f), gel = Gel.Silver, height = 38.dp)
                    }
                    GelButton("Check", { check() }, Modifier.fillMaxWidth(), gel = Gel.Aqua, height = 42.dp)
                }
            }
        } else Column(Modifier.fillMaxSize().statusBarsPadding()) {
            // ---------- top bar ----------
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                TopIcon(Icons.Rounded.ChevronLeft, "Back") { requestExit() }
                Column(Modifier.weight(1f)) {
                    Text(title, style = AeroType.Studio.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(subtitle, style = AeroType.Studio.copy(fontSize = 11.sp, color = Fl.Text3), maxLines = 1)
                }
                if (lesson == null) TopText("${project.pattern.bars} bar${if (project.pattern.bars > 1) "s" else ""}", "Pattern length ${project.pattern.bars} bars, tap to change") {
                    editPattern(project.pattern.withBars(when (project.pattern.bars) { 1 -> 2; 2 -> 4; else -> 1 }))
                }
                if (!coachOpen) LouOrb(lou, { coachOpen = true; sfx.tap() }, Modifier.size(42.dp), beatPhase = if (playing) beatPhase else null)
                TopIcon(Icons.AutoMirrored.Rounded.Undo, "Undo", undo.isNotEmpty()) { redo.add(project); project = undo.removeAt(undo.lastIndex) }
                TopIcon(Icons.AutoMirrored.Rounded.Redo, "Redo", redo.isNotEmpty()) { undo.add(project); project = redo.removeAt(redo.lastIndex) }
                TopIcon(Icons.Rounded.Save, "Save to My loops") { saveNow() }
                Box {
                    TopIcon(Icons.Rounded.UploadFile, "Export MIDI or WAV") { exportOrNudge() }
                    DropdownMenu(exportMenu, { exportMenu = false }) { exportItems() }
                }
            }
            // ---------- transport ----------
            Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FlTransport(playing, project.bpm, onPlay = { togglePlay() }, onStop = { stopAll(); sfx.tap() }, onTempo = { tempoDialog = true },
                    compact = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 420)
                metroButton()
                Spacer(Modifier.weight(1f).width(0.dp))
                windowTabs()
            }

            // ---------- active window ----------
            windowArea(Modifier.weight(1f).fillMaxWidth())

            // ---------- Lou coach (stacked below the window, never covers it) ----------
            if (coachOpen) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(6.dp).glass(20.dp).padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Lou(Modifier.size(50.dp, 44.dp), mood = lou.mood, mouthOpen = lou.mouth, beatPhase = if (playing) beatPhase else null)
                        Column(Modifier.weight(1f).padding(start = 4.dp)) {
                            lou.bubble?.let { LouBubble(it) }
                            taskLine(); chips()
                        }
                        Icon(Icons.Rounded.ExpandMore, "Hide Lou", tint = Color(0xFF35546B), modifier = Modifier.size(36.dp).clip(CircleShape).clickable { coachOpen = false; sfx.tap() }.padding(6.dp))
                    }
                    if (step != null) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GelButton(if (playing) "Stop" else "Listen", { togglePlay() }, Modifier.weight(1f), gel = Gel.Silver, height = 40.dp)
                        GelButton(if (hintLevel == 0) "Hint" else "More help", { hint(step) }, Modifier.weight(1f), gel = Gel.Silver, height = 40.dp)
                        GelButton("Check", { check() }, Modifier.weight(1.2f), gel = Gel.Aqua, height = 40.dp)
                    }
                }
            }

            if (!coachOpen) Spacer(Modifier.navigationBarsPadding())
        }

        if (introOpen && lesson != null) {
            Box(Modifier.fillMaxSize().background(Color(0x99081420)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
                Column(Modifier.sideSafe().statusBarsPadding().navigationBarsPadding().padding(if (land) 10.dp else 20.dp).widthIn(max = if (land) 640.dp else 520.dp)
                    .glass(26.dp).padding(if (land) 14.dp else 18.dp), verticalArrangement = Arrangement.spacedBy(if (land) 8.dp else 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Lou(if (land) Modifier.size(60.dp, 52.dp) else Modifier.size(80.dp, 70.dp), mood = lou.mood, mouthOpen = lou.mouth)
                        Column(Modifier.padding(start = 8.dp)) {
                            Text(if (lesson.level == 0) "PRACTICE · ENDLESS" else "LEVEL ${lesson.level} · TODAY'S IDEA", style = AeroType.Caps)
                            Text(lesson.title, style = AeroType.Title)
                        }
                    }
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(lesson.idea, style = AeroType.BodyStrong.copy(fontSize = 17.sp, lineHeight = 24.sp))
                        // the extras stay folded so the card reads in one glance
                        com.beatpath.app.ui.aero.Fold("Words & tips") {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                com.beatpath.app.lessons.Glossary.find(lesson.idea + " " + lesson.title, 3).forEach { term -> Text("• ${term.word}: ${term.meaning}", style = AeroType.Body.copy(fontSize = 14.sp)) }
                                Text("💡 ${com.beatpath.app.lessons.Tips.theoryFor(track).first()}", style = AeroType.Body.copy(fontSize = 14.sp, color = Color(0xFF0A548F)))
                                Text("In FL Studio: ${lesson.flTip}", style = AeroType.Small)
                            }
                        }
                    }
                    GelButton("Let's go", { introOpen = false; sfx.select(); step?.let { lou.say(it.say, LouMood.Idle) } }, Modifier.fillMaxWidth(), gel = Gel.Grass, height = if (land) 42.dp else 46.dp)
                }
            }
        }

        ResultTray(tray, modifier = Modifier.align(Alignment.BottomCenter), onCta = {
            val passed = lastPass
            tray = null
            if (passed && lesson != null) {
                if (stepIdx < lesson.steps.lastIndex) { stopAll(); stepIdx++ } else { stopAll(); sfx.complete(); onLessonDone(lesson.id, attemptId, !usedHelp) }
            } else if (!passed && !playing) togglePlay()
        })

        flSheet?.let { inf ->
            ModalBottomSheet(onDismissRequest = { flSheet = null }, containerColor = Color(0xFFF3FAFF)) {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("WHERE IS THIS IN FL?", style = AeroType.Caps)
                    Text(inf.title, style = AeroType.Title)
                    Fact("Shortcut / place", inf.shortcut); Fact("On desktop", inf.desktop); Fact("Here on your phone", inf.phone)
                    lesson?.let { Fact("Tip for this lesson", it.flTip) }
                    GelButton("Got it", { flSheet = null }, Modifier.fillMaxWidth())
                }
            }
        }

        if (tempoDialog) AlertDialog(containerColor = Color(0xFFF3FAFF),
            onDismissRequest = { tempoDialog = false }, confirmButton = { TextButton({ tempoDialog = false }) { Text("Done") } },
            title = { Text("Tempo", style = AeroType.H2) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GelButton("−5", { edit(project.copy(bpm = (project.bpm - 5).coerceAtLeast(60))) }, Modifier.width(72.dp), gel = Gel.Silver)
                    Text("${project.bpm} BPM", style = AeroType.Title)
                    GelButton("+5", { edit(project.copy(bpm = (project.bpm + 5).coerceAtMost(180))) }, Modifier.width(72.dp), gel = Gel.Silver)
                }
            },
        )

        if (leaveDialog) AlertDialog(containerColor = Color(0xFFF3FAFF), onDismissRequest = { leaveDialog = false },
            title = { Text(if (lesson != null) "Leave this lesson?" else "Save your loop?", style = AeroType.H2) },
            text = { Text(if (lesson != null) "Your progress in this lesson won't be kept. You can start it again from the path any time." else "You have changes that aren't in My loops yet.", style = AeroType.Body) },
            confirmButton = {
                if (lesson != null) TextButton({ leaveDialog = false; stopAll(); onExit() }) { Text("Leave") }
                else TextButton({ leaveDialog = false; saveNow(); stopAll(); onExit() }) { Text("Save & leave") }
            },
            dismissButton = {
                Row {
                    if (lesson == null) TextButton({ leaveDialog = false; stopAll(); onExit() }) { Text("Don't save") }
                    TextButton({ leaveDialog = false }) { Text(if (lesson != null) "Keep going" else "Keep editing") }
                }
            })

        velTarget?.let { (k, s) ->
            AlertDialog(containerColor = Color(0xFFF3FAFF), onDismissRequest = { velTarget = null }, confirmButton = {}, dismissButton = { TextButton({ velTarget = null }) { Text("Cancel") } }, title = { Text("How hard is step ${s % 16 + 1} hit?", style = AeroType.H2) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Soft (ghost)" to 45, "Medium" to 80, "Loud (accent)" to 115).forEach { (label, v) ->
                            GelButton(label, {
                                editPattern(project.pattern.update(k) { ch -> ch.copy(notes = ch.notes.map { if (it.startTick == s * TICKS_PER_STEP) it.copy(velocity = v) else it }) })
                                engine.playDrum(k, v / 127f); velTarget = null
                            }, Modifier.fillMaxWidth(), gel = if (v == 45) Gel.Silver else if (v == 80) Gel.Aqua else Gel.Orange, height = 42.dp)
                        }
                    }
                })
        }
    }
}

@Composable
private fun Fact(head: String, text: String) { Column { Text(head, style = AeroType.Small); Text(text, style = AeroType.BodyStrong) } }

@Composable
private fun TopIcon(icon: ImageVector, desc: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).clickable(enabled = enabled, onClick = onClick).semantics { contentDescription = desc }, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = if (enabled) Fl.Text else Fl.Edge, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun TopText(label: String, desc: String, onClick: () -> Unit) {
    Text(label, style = AeroType.Studio, modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Fl.Dark).border(1.dp, Fl.Edge, RoundedCornerShape(6.dp))
        .clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 8.dp).semantics { contentDescription = desc })
}

@Composable
private fun SegTab(icon: ImageVector, label: String, desc: String, active: Boolean, showLabel: Boolean = true, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).background(if (active) Fl.Frame else Color.Transparent).clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 9.dp).semantics { contentDescription = "$desc${if (active) ", open" else ""}" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (active) Fl.Lime else Fl.Text2, modifier = Modifier.size(18.dp))
        if (showLabel) {
            Spacer(Modifier.width(5.dp))
            Text(label, color = if (active) Fl.Text else Fl.Text2, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        }
    }
}
