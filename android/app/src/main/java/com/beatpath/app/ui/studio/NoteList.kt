package com.beatpath.app.ui.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatpath.app.audio.AudioEngine
import com.beatpath.app.lessons.Snap
import com.beatpath.app.model.ChannelKind
import com.beatpath.app.model.Note
import com.beatpath.app.model.Pattern
import com.beatpath.app.model.Pitch
import com.beatpath.app.model.TICKS_PER_BAR
import com.beatpath.app.model.TICKS_PER_BEAT
import com.beatpath.app.model.TICKS_PER_STEP
import com.beatpath.app.ui.fl.Fl

private fun where(t: Int) = "bar ${t / TICKS_PER_BAR + 1}, beat ${t % TICKS_PER_BAR / TICKS_PER_BEAT + 1}" +
    (t % TICKS_PER_BEAT).let { if (it == 0) "" else ", step ${it / TICKS_PER_STEP + 1}" }
private fun len(t: Int) = when { t % TICKS_PER_BEAT == 0 -> "${t / TICKS_PER_BEAT} beat${if (t != TICKS_PER_BEAT) "s" else ""}"; else -> "${t / TICKS_PER_STEP} step${if (t != TICKS_PER_STEP) "s" else ""}" }

/**
 * Accessible note editor (plan §6.4): every note as a row with plain buttons, using the same edits as the grid.
 * Designed for TalkBack: each row reads "E5, bar 1, beat 2, 1 beat, velocity 100", every button says what it does.
 */
@Composable
fun NoteListEditor(pattern: Pattern, channel: ChannelKind, rows: IntRange, snap: Snap, locked: Boolean, engine: AudioEngine, onEdit: (Pattern) -> Unit, modifier: Modifier = Modifier) {
    val ch = pattern.channel(channel)
    val notes = ch.notes.sortedWith(compareBy({ it.startTick }, { it.pitch }))
    val step = snap.ticks
    fun upd(id: Long, f: (Note) -> Note) = onEdit(pattern.update(channel) { c -> c.copy(notes = c.notes.map { if (it.id == id) f(it) else it }) })
    fun hear(n: Note) { if (channel.isDrum) engine.playDrum(channel, n.velocity / 127f) else engine.playNote(n.pitch, 0.4f, n.velocity / 127f, channel = channel) }
    Column(modifier.background(Fl.Panel).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("${ch.name}: ${notes.size} notes · snap ${snap.label}", color = Fl.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
        if (!locked) Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Fl.Deep).border(1.dp, Fl.Lime, RoundedCornerShape(8.dp)).clickable {
            val start = ((notes.maxOfOrNull { it.startTick + it.durationTicks } ?: 0) / step * step).coerceAtMost(pattern.lengthTicks - step)
            val pitch = if (channel.isDrum) channel.drumPitch else notes.lastOrNull()?.pitch ?: (rows.first + (rows.last - rows.first) / 2)
            val n = Note(Pattern.nextId(), pitch, start, if (channel.isDrum) TICKS_PER_STEP else TICKS_PER_BEAT)
            onEdit(pattern.update(channel) { it.copy(notes = it.notes + n) }); hear(n)
        }.semantics { contentDescription = "Add a note after the last one" }.padding(12.dp), contentAlignment = Alignment.Center) {
            Text("+ Add note", color = Fl.Lime, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxSize()) {
            items(notes, key = { it.id }) { n ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Fl.Dark).border(1.dp, Fl.Edge, RoundedCornerShape(8.dp)).padding(8.dp)) {
                    Text("${if (channel.isDrum) channel.label else Pitch.flName(n.pitch)} · ${where(n.startTick)} · ${len(n.durationTicks)} · velocity ${n.velocity}",
                        color = Fl.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { hear(n) }.semantics { contentDescription = "${if (channel.isDrum) channel.label else Pitch.flName(n.pitch)}, ${where(n.startTick)}, ${len(n.durationTicks)}, velocity ${n.velocity}. Double tap to hear." })
                    if (!locked) Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 6.dp)) {
                        Btn("◀", "Move earlier") { upd(n.id) { it.copy(startTick = (it.startTick - step).coerceAtLeast(0)) } }
                        Btn("▶", "Move later") { upd(n.id) { it.copy(startTick = (it.startTick + step).coerceAtMost(pattern.lengthTicks - it.durationTicks)) } }
                        if (!channel.isDrum) {
                            Btn("▲", "One semitone higher") { upd(n.id) { it.copy(pitch = (it.pitch + 1).coerceAtMost(rows.last)) }; hear(n.copy(pitch = n.pitch + 1)) }
                            Btn("▼", "One semitone lower") { upd(n.id) { it.copy(pitch = (it.pitch - 1).coerceAtLeast(rows.first)) }; hear(n.copy(pitch = n.pitch - 1)) }
                            Btn("−", "Shorter") { upd(n.id) { it.copy(durationTicks = (it.durationTicks - step).coerceAtLeast(step)) } }
                            Btn("+", "Longer") { upd(n.id) { it.copy(durationTicks = (it.durationTicks + step).coerceAtMost(pattern.lengthTicks - it.startTick)) } }
                        }
                        Btn("Vel", "Cycle velocity soft, medium, loud") { upd(n.id) { it.copy(velocity = when { it.velocity < 60 -> 80; it.velocity < 100 -> 115; else -> 45 }) } }
                        Btn("✕", "Delete note") { onEdit(pattern.update(channel) { c -> c.copy(notes = c.notes.filterNot { it.id == n.id }) }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Btn(label: String, desc: String, onClick: () -> Unit) {
    Box(Modifier.size(42.dp).clip(RoundedCornerShape(6.dp)).background(Fl.Deep).border(1.dp, Fl.Edge, RoundedCornerShape(6.dp)).clickable(onClick = onClick)
        .semantics { contentDescription = desc }.padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
        Text(label, color = Fl.Text, fontSize = if (label.length > 2) 10.sp else 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}
