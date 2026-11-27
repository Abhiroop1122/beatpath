package com.beatpath.app.ui.studio

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatpath.app.model.Clip
import com.beatpath.app.model.Project
import com.beatpath.app.ui.aero.Gel
import com.beatpath.app.ui.aero.claySurface
import com.beatpath.app.ui.aero.flBox
import com.beatpath.app.ui.fl.Fl

/** Loop names and colors: A, B and C. */
val LoopGels = listOf(Gel.Aqua, Gel.Orange, Gel.Pink)
fun loopName(i: Int) = "ABC".getOrElse(i) { '?' }.toString()

/** A, B, C loop picker: which loop the Rack and Piano roll are editing right now. */
@Composable
fun LoopPicker(project: Project, onPick: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until 3) {
            val on = project.current == i
            Box(Modifier.width(40.dp).height(44.dp).then(if (on) Modifier.clip(RoundedCornerShape(8.dp)).background(LoopGels[i].bottom) else Modifier.flBox(8.dp))
                .combinedClickable(onClick = { onPick(i) }).semantics { contentDescription = "Loop ${loopName(i)}${if (on) ", editing" else ""}" },
                contentAlignment = Alignment.Center) {
                Text(loopName(i), color = if (on) Color.White else Fl.Text, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 3.dp))
            }
        }
    }
}

/**
 * The song row: one tile per bar. Tap a bar to place loop A, tap again for B, then C, then empty.
 * Press and hold to clear. Play switches between one loop and the whole song.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongView(project: Project, playBar: Int, onProject: (Project) -> Unit, onTap: () -> Unit, modifier: Modifier = Modifier) {
    fun loopAt(bar: Int) = project.playlist.firstOrNull { it.startBar == bar }?.pattern
    fun setBar(bar: Int, loop: Int?) {
        val rest = project.playlist.filter { it.startBar != bar }
        onProject(project.copy(playlist = if (loop == null) rest else rest + Clip(loop, bar, 0), songMode = true))
    }
    Column(modifier.background(Fl.Frame).verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Play:", color = Fl.Text2, fontSize = 13.sp)
            Spacer(Modifier.width(8.dp))
            listOf(false to "One loop", true to "Whole song").forEach { (song, label) ->
                val on = project.songMode == song
                Text(label, color = if (on) Color(0xFF1F2E08) else Fl.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 6.dp).flBox(8.dp, on = on, lime = true).combinedClickable(onClick = { onTap(); onProject(project.copy(songMode = song)) })
                        .padding(start = 12.dp, end = 12.dp, top = 11.dp, bottom = 14.dp).semantics { contentDescription = "Play $label${if (on) ", selected" else ""}" })
            }
            Spacer(Modifier.weight(1f))
            Text("${project.songBars} bars", color = Fl.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.flBox(8.dp).combinedClickable(onClick = {
                    onTap(); val n = if (project.songBars == 8) 16 else 8
                    onProject(project.copy(songBars = n, playlist = project.playlist.filter { it.startBar < n }))
                }).padding(start = 12.dp, end = 12.dp, top = 11.dp, bottom = 14.dp).semantics { contentDescription = "Song length ${project.songBars} bars, tap to change" })
        }
        BoxWithConstraints {
            val perRow = if (maxWidth > 600.dp) 8 else 4
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (0 until project.songBars).chunked(perRow).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { bar ->
                            val loop = loopAt(bar)
                            val playingHere = playBar == bar && project.songMode
                            val pop by animateFloatAsState(if (playingHere) 1.06f else 1f, spring(dampingRatio = 0.5f, stiffness = 600f), label = "bar")
                            Box(Modifier.weight(1f).aspectRatio(1f).graphicsLayer { scaleX = pop; scaleY = pop }
                                .then(if (loop != null) Modifier.clip(RoundedCornerShape(12.dp)).background(LoopGels[loop].bottom).border(if (playingHere) 2.dp else 0.dp, Color.White, RoundedCornerShape(12.dp)) else Modifier.flBox(12.dp, on = playingHere))
                                .combinedClickable(onLongClick = { onTap(); setBar(bar, null) }) { onTap(); setBar(bar, when (loop) { null -> 0; 0 -> 1; 1 -> 2; else -> null }) }
                                .semantics { contentDescription = "Bar ${bar + 1}: ${loop?.let { "loop ${loopName(it)}" } ?: "empty"}. Tap to change." },
                                contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(loop?.let { loopName(it) } ?: "+", color = if (loop != null) Color.White else Fl.Text2, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                                    Text("bar ${bar + 1}", color = if (loop != null) Color.White.copy(alpha = 0.85f) else Fl.Text3, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
        Text("1. Make loop A in the Rack or Piano roll.  2. Pick B (top bar) and make a different loop.  3. Tap bars here to place A, B or C.  4. Play the whole song!",
            color = Fl.Text2, fontSize = 12.sp, lineHeight = 17.sp)
    }
}
