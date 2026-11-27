package com.beatpath.app.ui.studio

import com.beatpath.app.ui.aero.flBox
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import kotlinx.coroutines.flow.first
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.HighlightAlt
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatpath.app.audio.AudioEngine
import com.beatpath.app.lessons.Ghost
import com.beatpath.app.lessons.Snap
import com.beatpath.app.model.ChannelKind
import com.beatpath.app.model.Note
import com.beatpath.app.model.Pattern
import com.beatpath.app.model.Pitch
import com.beatpath.app.model.NoteColors
import com.beatpath.app.model.TICKS_PER_BAR
import com.beatpath.app.model.TICKS_PER_BEAT
import com.beatpath.app.ui.fl.Fl
import com.beatpath.app.ui.fl.FlWindow

enum class Tool(val label: String, val key: String, val icon: ImageVector, val info: String, val tint: Color) {
    Draw("Draw", "P", Icons.Rounded.Edit, "draw", Color(0xFFF2C14E)),
    Paint("Paint", "B", Icons.Rounded.Brush, "paint", Color(0xFFE9EEF1)),
    Delete("Delete", "D", Icons.Rounded.Delete, "delete", Color(0xFFE9EEF1)),
    Select("Select", "E", Icons.Rounded.HighlightAlt, "select", Color(0xFFE9EEF1)),
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FlRoll(
    pattern: Pattern,
    channel: ChannelKind,
    rows: IntRange,
    scale: Set<Int>?,
    ghostChannels: Set<ChannelKind>,
    ghosts: List<Ghost>,
    playheadTick: Int,
    locked: Boolean,
    defaultSnap: Snap,
    engine: AudioEngine,
    onEdit: (Pattern) -> Unit,
    onInfo: (String) -> Unit,
    modifier: Modifier = Modifier,
    channelChoices: List<ChannelKind> = emptyList(),
    onChannel: (ChannelKind) -> Unit = {},
    showVelocity: Boolean = false,
    compact: Boolean = false,
    soundName: String? = null,
    soundChoices: List<String> = emptyList(),
    onSound: (String) -> Unit = {},
) {
    var soundMenu by remember { mutableStateOf(false) }
    var tool by remember { mutableStateOf(Tool.Draw) }
    var snap by remember(defaultSnap) { mutableStateOf(defaultSnap) }
    var selection by remember { mutableStateOf(setOf<Long>()) }
    // new notes copy the last length you set, but never start longer than the grid: on a step grid a new note is one step,
    // so it can't cover the very next spot you want to tap
    var lastLength by remember(defaultSnap) { mutableIntStateOf(if (channel.isDrum) 60 else defaultSnap.ticks.coerceAtMost(TICKS_PER_BEAT * 4)) }
    LaunchedEffect(snap) { if (!channel.isDrum && lastLength > snap.ticks && snap.ticks < TICKS_PER_BEAT) lastLength = snap.ticks }
    var barsVisible by remember(pattern.bars, compact) { mutableIntStateOf(pattern.bars.coerceAtMost(if (compact) 4 else 2)) }
    var listMode by remember { mutableStateOf(false) }
    var showVel by remember(showVelocity) { mutableStateOf(showVelocity) }
    val current by rememberUpdatedState(pattern)
    val high = rows.last; val low = rows.first
    val rowList = (high downTo low).toList()
    val density = LocalDensity.current
    val rowH = if (rowList.size <= 3) 44.dp else 30.dp
    val keyW = 54.dp
    val ch = pattern.channel(channel)

    fun withNotes(f: (List<Note>) -> List<Note>) = current.update(channel) { it.copy(notes = f(it.notes)) }
    fun preview(p: Int, v: Float = 0.8f) { if (channel.isDrum) engine.playDrum(channel, v) else engine.playNote(p, 0.35f, v, channel = channel) }

    val padV = if (compact) 7.dp else 10.dp
    Column(modifier.background(Fl.Frame)) {
        // ---------- toolbars ----------
        val soundChip: @Composable () -> Unit = {
            if (soundName != null && soundChoices.isNotEmpty()) Box {
                // instrument picker for this channel: same notes, different sound
                Row(Modifier.flBox(6.dp).clickable { soundMenu = true }
                    .padding(horizontal = 8.dp, vertical = padV).semantics { contentDescription = "Sound $soundName, tap to change" }, verticalAlignment = Alignment.CenterVertically) {
                    Text("♪ ", color = Fl.Lime, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(soundName, color = Fl.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.widthIn(max = 110.dp))
                    Text(" ▾", color = Fl.Text2, fontSize = 11.sp)
                }
                DropdownMenu(soundMenu, { soundMenu = false }) {
                    soundChoices.forEach { name ->
                        DropdownMenuItem(text = { Text((if (name == soundName) "✓ " else "") + name, fontWeight = if (name == soundName) FontWeight.Bold else FontWeight.Normal) },
                            onClick = { soundMenu = false; onSound(name) })
                    }
                }
            }
        }
        val channelChips: @Composable (Modifier) -> Unit = { m ->
            Row(m.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (if (channelChoices.isEmpty() || channel.isDrum) listOf(channel) else channelChoices).forEach { k ->
                    val on = k == channel
                    Text(k.label, color = if (on) Fl.LimeDark else Fl.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                        modifier = Modifier.flBox(6.dp, on = on, lime = true).clickable { if (!on) { onChannel(k); selection = emptySet() } }
                            .padding(horizontal = 10.dp, vertical = padV).semantics { contentDescription = "Edit ${k.label}${if (on) ", selected" else ""}" })
                }
            }
        }
        val viewOptions: @Composable () -> Unit = {
            Row(
                Modifier.flBox(6.dp)
                    .combinedClickable(onLongClick = { onInfo("snap") }) {
                        val opts = if (channel.isDrum) listOf(Snap.STEP, Snap.HALF_STEP, Snap.TRIPLET) else listOf(Snap.BEAT, Snap.HALF, Snap.STEP, Snap.TRIPLET)
                        snap = opts[(opts.indexOf(snap) + 1).mod(opts.size)]
                    }.padding(horizontal = 8.dp, vertical = padV).semantics { contentDescription = "Snap ${snap.label}, tap to change" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Snap ", color = Fl.Text3, fontSize = 11.sp)
                Text(snap.label, color = Fl.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            if (!channel.isDrum && !listMode) Text("🌈", fontSize = 13.sp,
                modifier = Modifier.flBox(6.dp, on = NoteColors.rainbowRoll).clickable { NoteColors.setRainbow(!NoteColors.rainbowRoll) }
                    .padding(horizontal = 8.dp, vertical = padV).semantics { contentDescription = if (NoteColors.rainbowRoll) "Rainbow note colors on, tap to turn off" else "Rainbow note colors off, tap to turn on" })
            if (!listMode) Text("Vel", color = if (showVel) Fl.Lime else Fl.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.flBox(6.dp).clickable { showVel = !showVel }.padding(horizontal = 10.dp, vertical = padV)
                    .semantics { contentDescription = if (showVel) "Hide the velocity lane" else "Show the velocity lane" })
            Text(if (listMode) "Grid" else "List", color = if (listMode) Fl.Lime else Fl.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.flBox(6.dp).clickable { listMode = !listMode }.padding(horizontal = 10.dp, vertical = padV)
                    .semantics { contentDescription = if (listMode) "Show the grid" else "Show notes as a list" })
            Text("?", color = Fl.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.size(34.dp).flBox(17.dp).clickable { onInfo("roll") }.padding(top = 7.dp).semantics { contentDescription = "Where is this in FL?" })
        }
        val zoomButtons: @Composable () -> Unit = {
            if (pattern.bars > 1 && !listMode) {
                Icon(Icons.Rounded.ZoomOut, "Show more bars", tint = if (barsVisible < pattern.bars) Fl.Text else Fl.Edge, modifier = Modifier.size(if (compact) 34.dp else 38.dp).flBox(8.dp).clickable { barsVisible = minOf(pattern.bars, barsVisible * 2) }.padding(7.dp))
                Icon(Icons.Rounded.ZoomIn, "Show fewer bars", tint = if (barsVisible > 1) Fl.Text else Fl.Edge, modifier = Modifier.size(if (compact) 34.dp else 38.dp).flBox(8.dp).clickable { barsVisible = maxOf(1, barsVisible / 2) }.padding(7.dp))
            }
        }
        val toolButtons: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
            Tool.entries.forEach { t ->
                val on = tool == t
                Row(
                    Modifier.weight(1f).height(if (compact) 37.dp else 41.dp).flBox(8.dp, on = on)
                        .combinedClickable(onLongClick = { onInfo(t.info) }) { tool = t; if (t != Tool.Select) selection = emptySet() }
                        .semantics { contentDescription = "${t.label} tool, FL key ${t.key}${if (on) ", selected" else ""}" },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(t.icon, null, tint = if (on) t.tint else Fl.Text2, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(t.label, color = if (on) Fl.Text else Fl.Text2, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(" ${t.key}", color = Fl.Text3, fontSize = 10.sp, maxLines = 1)
                }
            }
        }
        val bar = Modifier.fillMaxWidth().background(Fl.Title)
        if (compact) {
            // sideways: everything about "what & how" on one row, tools + zoom on the next
            Row(bar.padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                soundChip(); channelChips(Modifier.weight(1f)); viewOptions()
            }
            if (!listMode && (!locked || pattern.bars > 1)) Row(bar.padding(start = 6.dp, end = 6.dp, bottom = 5.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!locked) toolButtons() else Spacer(Modifier.weight(1f))
                zoomButtons()
            }
        } else {
            // upright: 1) which channel + its sound, 2) view options + zoom, 3) FL tools
            Row(bar.padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                channelChips(Modifier.weight(1f)); soundChip()
            }
            Row(bar.padding(start = 6.dp, end = 6.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                viewOptions(); Spacer(Modifier.weight(1f)); zoomButtons()
            }
            if (!listMode && !locked) Row(bar.padding(start = 6.dp, end = 6.dp, bottom = 5.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                toolButtons()
            }
        }

        if (listMode) NoteListEditor(pattern, channel, rows, snap, locked, engine, onEdit, Modifier.weight(1f).fillMaxWidth())
        else BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().background(Fl.Lane)) {
            val gridViewW = maxWidth - keyW
            val gridW = gridViewW * (pattern.bars.toFloat() / barsVisible)
            val gridWpx = with(density) { gridW.toPx() }
            val rowPx = with(density) { rowH.toPx() }
            val ticks = pattern.lengthTicks
            val pxPerTick = gridWpx / ticks
            val measurer = rememberTextMeasurer()
            val vScroll = rememberScrollState()
            val hScroll = rememberScrollState()
            val viewH by androidx.compose.runtime.rememberUpdatedState(maxHeight)
            // re-centre only when the music changes (new rows or channel), never when panels around the roll resize
            LaunchedEffect(rows, channel) {
                // wait until the grid has been measured, otherwise the scroll is clamped to 0 (top of the range)
                kotlinx.coroutines.withTimeoutOrNull(1500) { androidx.compose.runtime.snapshotFlow { vScroll.maxValue }.first { it > 0 } }
                // open centred on the music: existing notes, else the first outline, else the middle of the range
                val focusPitch = current.channel(channel).notes.map { it.pitch }.filter { it in low..high }.takeIf { it.isNotEmpty() }?.average()
                    ?: ghosts.firstOrNull { it.pitch in low..high }?.pitch?.toDouble()
                    ?: ghostChannels.flatMap { current.channel(it).notes }.map { it.pitch }.filter { it in low..high }.takeIf { it.isNotEmpty() }?.average()
                    ?: ((low + high) / 2.0)
                val y = ((high - focusPitch) * rowPx - with(density) { viewH.toPx() } / 2 + rowPx / 2).toInt()
                vScroll.scrollTo(y.coerceAtLeast(0))
            }
            // follow the first lesson ghost horizontally
            val firstGhostTick = ghosts.firstOrNull { it.pitch >= 0 }?.tick ?: -1
            LaunchedEffect(firstGhostTick, barsVisible) {
                if (firstGhostTick >= 0) hScroll.animateScrollTo(((firstGhostTick / TICKS_PER_BAR) * TICKS_PER_BAR * pxPerTick).toInt())
            }

            fun pitchAt(y: Float) = (high - (y / rowPx).toInt()).coerceIn(low, high)
            fun tickAt(x: Float) = ((x / pxPerTick).toInt() / snap.ticks * snap.ticks).coerceIn(0, ticks - snap.ticks)
            fun noteAt(x: Float, y: Float): Note? {
                val p = pitchAt(y); val t = x / pxPerTick
                return current.channel(channel).notes.firstOrNull { it.pitch == p && t >= it.startTick && t < it.startTick + it.durationTicks }
            }

            Column(Modifier.fillMaxSize()) {
                // ruler
                Row(Modifier.fillMaxWidth().height(22.dp).background(Fl.Ruler)) {
                    Box(Modifier.width(keyW).height(22.dp).background(Brush.linearGradient(listOf(Fl.Note, Color(0xFF7FCF7B)))))
                    Box(Modifier.horizontalScroll(hScroll, enabled = false)) {
                        Canvas(Modifier.width(gridW).height(22.dp)) {
                            for (b in 0 until pattern.bars) for (beat in 0 until 4) {
                                val x = (b * TICKS_PER_BAR + beat * TICKS_PER_BEAT) * pxPerTick
                                drawText(measurer, if (beat == 0) "${b + 1}" else "${b + 1}.${beat + 1}", Offset(x + 4, 3f),
                                    TextStyle(color = if (beat == 0) Fl.Text else Fl.Text3, fontSize = if (beat == 0) 11.sp else 9.sp))
                            }
                            if (playheadTick >= 0) drawRect(Fl.Lime, Offset(playheadTick * pxPerTick - 4, 14f), Size(8f, 8f))
                        }
                    }
                }
                Box(Modifier.weight(1f).verticalScroll(vScroll)) {
                    Row {
                        // keyboard: tap to preview
                        Column(Modifier.width(keyW)) {
                            rowList.forEach { p ->
                                val black = Pitch.isBlack(p)
                                Box(
                                    Modifier.width(keyW).height(rowH)
                                        .background(if (black) Brush.horizontalGradient(listOf(Color(0xFF1A1D20), Color(0xFF4A5157))) else Brush.horizontalGradient(listOf(Color(0xFFD9DDE0), Color.White)))
                                        .border(0.5.dp, Color(0xFF9AA3AA))
                                        .clickable { preview(p) }
                                        .semantics { contentDescription = "Preview ${Pitch.flName(p)}" },
                                    contentAlignment = Alignment.CenterEnd,
                                ) {
                                    val inScale = scale?.contains(p % 12) == true
                                    if (NoteColors.rainbowRoll && !channel.isDrum) Box(Modifier.align(Alignment.CenterStart).padding(start = 2.dp).size(5.dp, rowH - 4.dp).background(NoteColors.of(p), RoundedCornerShape(2.dp)))
                                    Text(if (channel.isDrum) channel.label else Pitch.flName(p), fontSize = 9.sp, fontWeight = FontWeight.SemiBold,
                                        color = when { black -> Color(0xFFB4BDC4); inScale || p % 12 == 0 -> Color(0xFF2F5E33); else -> Color(0xFF6B747B) },
                                        modifier = Modifier.padding(end = 4.dp))
                                }
                            }
                        }
                        Box(Modifier.horizontalScroll(hScroll)) {
                            Canvas(
                                Modifier.width(gridW).height(rowH * rowList.size)
                                    .semantics { contentDescription = "Piano roll grid, ${ch.notes.size} notes on ${ch.name}${if (locked) ", locked" else ""}" }
                                    .pointerInput(tool, snap, channel, locked) {
                                        if (locked) return@pointerInput
                                        detectTapGestures(
                                            onTap = { o ->
                                                val hit = noteAt(o.x, o.y)
                                                when (tool) {
                                                    Tool.Draw, Tool.Paint -> if (hit != null) { preview(hit.pitch, hit.velocity / 127f); lastLength = hit.durationTicks }
                                                    else {
                                                        val p = pitchAt(o.y); val t = tickAt(o.x)
                                                        onEdit(withNotes { it + Note(Pattern.nextId(), p, t, lastLength.coerceAtMost(ticks - t)) }); preview(p)
                                                    }
                                                    Tool.Delete -> hit?.let { n -> onEdit(withNotes { it.filterNot { x -> x.id == n.id } }) }
                                                    Tool.Select -> hit?.let { n -> selection = if (n.id in selection) selection - n.id else selection + n.id; preview(n.pitch, 0.6f) }
                                                }
                                            },
                                            onLongPress = { o -> noteAt(o.x, o.y)?.let { n -> tool = Tool.Select; selection = setOf(n.id) } },
                                        )
                                    }
                                    .pointerInput(tool, snap, channel, locked) {
                                        if (locked) return@pointerInput
                                        var mode = 0; var target: Note? = null; var start = Offset.Zero; var acc = Offset.Zero
                                        var painted = mutableSetOf<Int>()
                                        detectDragGestures(
                                            onDragStart = { o ->
                                                start = o; acc = Offset.Zero; painted = mutableSetOf()
                                                val hit = noteAt(o.x, o.y); target = hit
                                                mode = when {
                                                    tool == Tool.Paint && hit == null -> 3
                                                    hit != null && tool != Tool.Delete -> if ((hit.startTick + hit.durationTicks) * pxPerTick - o.x < maxOf(22.dp.toPx(), hit.durationTicks * pxPerTick * 0.3f)) 2 else 1
                                                    tool == Tool.Delete -> 4
                                                    else -> 0
                                                }
                                                if (mode == 3) { val t = tickAt(o.x); painted += t; onEdit(withNotes { it + Note(Pattern.nextId(), pitchAt(o.y), t, snap.ticks) }); preview(pitchAt(o.y), 0.6f) }
                                            },
                                            onDrag = { c, d ->
                                                c.consume(); acc += d
                                                val n = target
                                                when (mode) {
                                                    1 -> if (n != null) {
                                                        val nt = (((n.startTick * pxPerTick + acc.x) / pxPerTick).toInt() / snap.ticks * snap.ticks).coerceIn(0, ticks - n.durationTicks)
                                                        val np = (n.pitch - (acc.y / rowPx).toInt()).coerceIn(low, high)
                                                        val cur = current.channel(channel).notes.firstOrNull { it.id == n.id }
                                                        if (cur != null && (cur.startTick != nt || cur.pitch != np)) {
                                                            if (cur.pitch != np) preview(np, 0.5f)
                                                            onEdit(withNotes { l -> l.map { if (it.id == n.id) it.copy(startTick = nt, pitch = np) else it } })
                                                        }
                                                    }
                                                    2 -> if (n != null) {
                                                        val endT = ((n.startTick + n.durationTicks) * pxPerTick + acc.x) / pxPerTick
                                                        val len = ((endT - n.startTick).toInt() / snap.ticks * snap.ticks).coerceIn(snap.ticks, ticks - n.startTick)
                                                        val cur = current.channel(channel).notes.firstOrNull { it.id == n.id }
                                                        if (cur != null && cur.durationTicks != len) { lastLength = len; onEdit(withNotes { l -> l.map { if (it.id == n.id) it.copy(durationTicks = len) else it } }) }
                                                    }
                                                    3 -> { val p = start + acc; val t = tickAt(p.x)
                                                        if (t !in painted && current.channel(channel).notes.none { it.pitch == pitchAt(start.y) && it.startTick == t }) {
                                                            painted += t; onEdit(withNotes { it + Note(Pattern.nextId(), pitchAt(start.y), t, snap.ticks) }) } }
                                                    4 -> { val p = start + acc; noteAt(p.x, p.y)?.let { h -> onEdit(withNotes { it.filterNot { x -> x.id == h.id } }) } }
                                                }
                                            },
                                        )
                                    }
                            ) {
                                // lanes (+ scale shading)
                                rowList.forEachIndexed { i, p ->
                                    val y = i * rowPx
                                    val inScale = scale?.contains(p % 12) == true
                                    drawRect(when { inScale -> Color(0xFF41566A); Pitch.isBlack(p) -> Fl.LaneBlack; else -> Fl.Lane }, Offset(0f, y), Size(size.width, rowPx))
                                    drawRect(if (p % 12 == 0) Color(0xFF26313A) else Color(0xFF34424C), Offset(0f, y + rowPx - 1f), Size(size.width, 1f))
                                }
                                // time grid: snap lines faint, beats medium, bars strong (FL)
                                var t = 0
                                val stepT = minOf(snap.ticks, 120)
                                while (t <= ticks) {
                                    val x = t * pxPerTick
                                    val c = when { t % TICKS_PER_BAR == 0 -> Color(0xFF1E272E); t % TICKS_PER_BEAT == 0 -> Color(0xFF2A353E); else -> Color(0xFF34424C) }
                                    drawRect(c, Offset(x, 0f), Size(if (t % TICKS_PER_BAR == 0) 2.5f else if (t % TICKS_PER_BEAT == 0) 1.5f else 1f, size.height))
                                    t += stepT
                                }
                                // ghost channels (FL grey blocks)
                                ghostChannels.filter { it != channel }.forEach { gk ->
                                    current.channel(gk).notes.forEach { gn ->
                                        if (gn.pitch in low..high) drawRoundRect(Fl.Ghost, Offset(gn.startTick * pxPerTick + 1f, (high - gn.pitch) * rowPx + 3f), Size(gn.durationTicks * pxPerTick - 2f, rowPx - 6f), CornerRadius(3f))
                                    }
                                }
                                // lesson ghosts (gold dashed)
                                ghosts.filter { it.pitch >= 0 && it.channel == channel && it.pitch in low..high }.forEach { g ->
                                    val o = Offset(g.tick * pxPerTick + 1f, (high - g.pitch) * rowPx + 2f); val s = Size(g.lengthTicks * pxPerTick - 2f, rowPx - 4f)
                                    drawRoundRect(Color(0x33FFE26A), o, s, CornerRadius(6f))
                                    drawRoundRect(Color(0xFFFFE26A), o, s, CornerRadius(6f), style = Stroke(3.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f))))
                                }
                                // notes (FL mint)
                                current.channel(channel).notes.forEach { n ->
                                    if (n.pitch !in low..high) return@forEach
                                    val y = (high - n.pitch) * rowPx; val x = n.startTick * pxPerTick; val w = n.durationTicks * pxPerTick
                                    val sel = n.id in selection
                                    val rb = NoteColors.rainbowRoll && !channel.isDrum
                                    val top = when { sel -> Fl.NoteSel; rb -> androidx.compose.ui.graphics.lerp(NoteColors.of(n.pitch), Color.White, 0.3f); else -> Color(0xFFC9F3C6) }
                                    val bot = when { sel -> Color(0xFF9DB7EA); rb -> NoteColors.of(n.pitch); else -> Fl.NoteBottom }
                                    drawRoundRect(Brush.verticalGradient(listOf(top, bot), y + 2f, y + rowPx - 2f), Offset(x + 1f, y + 2f), Size(w - 2f, rowPx - 4f), CornerRadius(4f))
                                    drawRect(if (sel) Color(0xFF5A77B0) else if (rb) androidx.compose.ui.graphics.lerp(NoteColors.of(n.pitch), Color.Black, 0.3f) else Fl.NoteEdge, Offset(x + 1f, y + rowPx - 5f), Size(w - 2f, 3f))
                                    // velocity as a thin bar inside the note (FL shows it in the event editor; this keeps it visible)
                                    drawRect(Color(0x55000000), Offset(x + 3f, y + 4f), Size((w - 6f) * n.velocity / 127f, 2f))
                                    if (w > 44f) drawText(measurer, Pitch.flName(n.pitch), Offset(x + 6f, y + rowPx / 2 - 9f), TextStyle(color = if (sel) Color(0xFF24406F) else if (rb) NoteColors.ink(n.pitch) else Fl.NoteInk, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                                    if (w > 26f) drawRoundRect(Color(0x66FFFFFF), Offset(x + w - 8f, y + rowPx / 2 - 7f), Size(3f, 12f), CornerRadius(2f))
                                }
                                if (playheadTick >= 0) drawRect(Color(0xDDFFFFFF), Offset(playheadTick * pxPerTick, 0f), Size(2f, size.height))
                            }
                        }
                    }
                }
                // velocity lane (FL event editor) — shown when a lesson needs it, or on "Vel"
                if (showVel) Row(Modifier.fillMaxWidth().height(56.dp).background(Color(0xFF34414A))) {
                    Box(Modifier.width(keyW).height(56.dp), contentAlignment = Alignment.Center) { Text("Velocity", color = Fl.Text3, fontSize = 9.sp) }
                    Box(Modifier.horizontalScroll(hScroll, enabled = false)) {
                        Canvas(Modifier.width(gridW).height(56.dp).pointerInput(channel, locked) {
                            if (locked) return@pointerInput
                            fun setVel(o: Offset) {
                                val t = o.x / pxPerTick
                                val n = current.channel(channel).notes.minByOrNull { kotlin.math.abs(it.startTick - t) } ?: return
                                if (kotlin.math.abs(n.startTick * pxPerTick - o.x) > 30f) return
                                val v = ((1f - o.y / size.height) * 127).toInt().coerceIn(1, 127)
                                val targets = if (n.id in selection) selection else setOf(n.id)
                                onEdit(withNotes { l -> l.map { if (it.id in targets) it.copy(velocity = v) else it } })
                            }
                            detectDragGestures(onDragStart = { setVel(it) }) { c, _ -> c.consume(); setVel(c.position) }
                        }.semantics { contentDescription = "Velocity lane: drag a stem up or down" }) {
                            current.channel(channel).notes.forEach { n ->
                                val x = n.startTick * pxPerTick + 3f; val h = (size.height - 8f) * n.velocity / 127f
                                drawRect(Fl.NoteBottom, Offset(x, size.height - h), Size(2.5f, h))
                                drawCircle(Fl.Note, 4.5f, Offset(x + 1f, size.height - h))
                                drawRect(Color(0x88B7EDB4), Offset(x, size.height - h - 0.5f), Size(minOf(n.durationTicks * pxPerTick, 30f), 1f))
                            }
                        }
                    }
                }
            }
        }

        // ---------- selection actions (FL shortcuts printed on each) ----------
        // only notes that still exist count (a step change or undo can remove selected notes)
        val liveIds = current.channel(channel).notes.map { it.id }.toSet()
        androidx.compose.animation.AnimatedVisibility((selection intersect liveIds).isNotEmpty() && !locked,
            enter = androidx.compose.animation.expandVertically(androidx.compose.animation.core.spring(dampingRatio = 1f, stiffness = 500f)) + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.shrinkVertically(androidx.compose.animation.core.spring(dampingRatio = 1f, stiffness = 500f)) + androidx.compose.animation.fadeOut()) {
            Row(Modifier.fillMaxWidth().background(Fl.Title).padding(5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                fun shift(d: Int) = onEdit(withNotes { l -> l.map { if (it.id in selection) it.copy(pitch = (it.pitch + d).coerceIn(low, high)) else it } })
                fun vel(v: Int) = onEdit(withNotes { l -> l.map { if (it.id in selection) it.copy(velocity = v) else it } })
                SelChip("↑ semi", "Shift ↑", Modifier.weight(1f)) { shift(1) }
                SelChip("↓ semi", "Shift ↓", Modifier.weight(1f)) { shift(-1) }
                SelChip("↑ oct", "Ctrl ↑", Modifier.weight(1f)) { shift(12) }
                SelChip("↓ oct", "Ctrl ↓", Modifier.weight(1f)) { shift(-12) }
                SelChip("Copy →", "Ctrl B", Modifier.weight(1f)) {
                    val sel = current.channel(channel).notes.filter { it.id in selection }
                    if (sel.isNotEmpty()) {
                        // FL-style duplicate, bar-aligned: copy to the right by the number of bars the selection covers
                        val firstBar = sel.minOf { it.startTick } / TICKS_PER_BAR
                        val lastBar = (sel.maxOf { it.startTick + it.durationTicks } - 1) / TICKS_PER_BAR
                        val shiftBy = (lastBar - firstBar + 1) * TICKS_PER_BAR
                        val copies = sel.mapNotNull { n -> val s = n.startTick + shiftBy; if (s + n.durationTicks <= pattern.lengthTicks) n.copy(id = Pattern.nextId(), startTick = s) else null }
                        onEdit(withNotes { it + copies }); selection = copies.map { it.id }.toSet()
                    }
                }
                SelChip("Soft", "vel 45", Modifier.weight(1f)) { vel(45) }
                SelChip("Loud", "vel 115", Modifier.weight(1f)) { vel(115) }
            }
        }
    }
}

@Composable
private fun SelChip(label: String, key: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.height(42.dp).flBox(6.dp).border(1.dp, Color(0x55A9E34B), RoundedCornerShape(6.dp)).clickable(onClick = onClick)
        .semantics { contentDescription = "$label ($key)" }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(label, color = Fl.Text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(key, color = Fl.Lime, fontSize = 8.sp, maxLines = 1)
    }
}

@Suppress("unused") private val keepIcon = Icons.Rounded.ContentCopy
