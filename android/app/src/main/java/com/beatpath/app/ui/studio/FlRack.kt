package com.beatpath.app.ui.studio

import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import com.beatpath.app.model.DrumKit
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatpath.app.lessons.Ghost
import com.beatpath.app.model.Channel
import com.beatpath.app.model.ChannelKind
import com.beatpath.app.model.Pattern
import com.beatpath.app.model.TICKS_PER_STEP
import com.beatpath.app.ui.fl.Fl
import com.beatpath.app.ui.fl.FlChannelButton
import com.beatpath.app.ui.fl.FlKnob
import com.beatpath.app.ui.fl.FlLed
import com.beatpath.app.ui.fl.FlStep
import com.beatpath.app.ui.fl.FlWindow

@Composable
fun FlRack(
    pattern: Pattern,
    visible: List<ChannelKind>,
    selected: ChannelKind,
    playheadStep: Int,
    ghosts: List<Ghost>,
    onToggle: (ChannelKind, Int) -> Unit,
    onVelocity: (ChannelKind, Int) -> Unit,
    onSelect: (ChannelKind) -> Unit,
    onOpenRoll: (ChannelKind) -> Unit,
    onMute: (ChannelKind) -> Unit,
    onChannelVolume: (ChannelKind, Float) -> Unit,
    onChannelPan: (ChannelKind, Float) -> Unit,
    onInfo: () -> Unit,
    swing: Float = 0f,
    onSwing: (Float) -> Unit = {},
    lockedKinds: Set<ChannelKind> = emptySet(),
    showSwing: Boolean = false,
    modifier: Modifier = Modifier,
    kit: DrumKit = DrumKit.CLASSIC,
    onKit: ((DrumKit) -> Unit)? = null,
) {
    var kitMenu by remember { mutableStateOf(false) }
    FlWindow("Channel rack", modifier, extras = {
        if (showSwing) {
            Text("Swing", color = Fl.Text3, fontSize = 10.sp)
            Spacer(Modifier.width(4.dp))
            FlKnob(swing, onSwing, Modifier.size(32.dp), label = "Swing")
            Spacer(Modifier.width(8.dp))
        }
        if (onKit != null) Box {
            // drum kit picker: same pattern, different drum sounds
            Row(Modifier.height(30.dp).clip(RoundedCornerShape(6.dp)).background(Fl.Dark).border(1.dp, Fl.Edge, RoundedCornerShape(6.dp)).clickable { kitMenu = true }
                .padding(horizontal = 8.dp).semantics { contentDescription = "Drum kit ${kit.label}, tap to change" }, verticalAlignment = Alignment.CenterVertically) {
                Text("Kit ", color = Fl.Text3, fontSize = 11.sp)
                Text(kit.label, color = Fl.Lime, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(" ▾", color = Fl.Text2, fontSize = 11.sp)
            }
            DropdownMenu(kitMenu, { kitMenu = false }) {
                DrumKit.entries.forEach { k ->
                    DropdownMenuItem(text = {
                        Column {
                            Text((if (k == kit) "✓ " else "") + k.label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(k.blurb, fontSize = 12.sp, color = Color(0xFF4A5A66))
                        }
                    }, onClick = { kitMenu = false; onKit(k) })
                }
            }
        }
        Spacer(Modifier.width(6.dp))
        Text("${pattern.steps}", color = Fl.Text2, fontSize = 13.sp,
            modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(Fl.Dark).border(1.dp, Fl.Edge, RoundedCornerShape(5.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
        Spacer(Modifier.width(8.dp))
        Text("?", color = Fl.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.size(26.dp).clip(RoundedCornerShape(13.dp)).background(Fl.Dark).clickable(onClick = onInfo).padding(top = 3.dp).semantics { contentDescription = "Where is this in FL?" },
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Fl.Panel)) {
            // knobs only when there's real room; 16 steps per page whenever each pad can stay at least 30dp wide
            val wide = maxWidth >= 760.dp
            val lockW = if (lockedKinds.isEmpty()) 0 else 18
            val leftW = if (wide) (34 + 26 + 26 + 34 + lockW + 104 + 5 + 6 * 6).dp else (34 + lockW + 62 + 5 + 3 * 6).dp
            val gap = 3.dp; val groupGap = 6.dp
            fun stepWidth(n: Int) = (maxWidth - leftW - 22.dp - groupGap * (n / 4 - 1) - gap * (n - n / 4)) / n
            val perPage = if (stepWidth(16) >= 30.dp) 16 else 8
            val pages = pattern.steps / perPage
            var page by remember { mutableIntStateOf(0) }
            val firstGhost = ghosts.firstOrNull { it.step >= 0 }?.step ?: -1
            // follow the outlines when a new step or hint shows them — but never right after you tap a pad,
            // so the step you just placed stays in view (the gold marks in the mini-map show where to go next)
            var lastTap by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
            LaunchedEffect(firstGhost, perPage) {
                if (firstGhost >= 0 && android.os.SystemClock.uptimeMillis() - lastTap > 1200) page = firstGhost / perPage
            }
            LaunchedEffect(pages) { if (page >= pages) page = 0 }
            // exact left cluster: LED 28 + [pan 26 + vol 26 + mixer 34] + lock 12 + button (104|62) + selector 5, plus 6dp gaps
            val stepW = stepWidth(perPage)

            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // page bar + mini-map (tap a block to jump)
                Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Steps ${page * perPage + 1}–${page * perPage + perPage}" + if (pattern.bars > 1) "  ·  bar ${page * perPage / 16 + 1}" else "",
                        color = Fl.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (pg in 0 until pages) {
                            Row(Modifier.height(40.dp).clickable { page = pg }.semantics { contentDescription = "Show steps ${pg * perPage + 1} to ${pg * perPage + perPage}${if (pg == page) ", showing" else ""}" }
                                .padding(horizontal = 2.dp, vertical = 12.dp).clip(RoundedCornerShape(4.dp)).border(1.5.dp, if (pg == page) Fl.Lime else Color.Transparent, RoundedCornerShape(4.dp)).padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(1.dp), verticalAlignment = Alignment.CenterVertically) {
                                for (s in 0 until perPage) {
                                    val step = pg * perPage + s
                                    val maroon = (step / 4) % 2 == 1
                                    val on = visible.any { pattern.channel(it).isDrum && pattern.channel(it).stepOn(step) }
                                    Box(Modifier.size(5.dp, 9.dp).background(when {
                                        step == playheadStep -> Fl.Lime
                                        ghosts.any { it.step == step } -> Color(0xFFF2C14E)   // outlines still waiting on this page
                                        on -> if (maroon) Color(0xFFD38E95) else Color(0xFFA9BCC8)
                                        else -> if (maroon) Color(0xFF5C4044) else Color(0xFF3F454B)
                                    }))
                                }
                            }
                        }
                    }
                }
                visible.forEach { kind ->
                    val ch = pattern.channel(kind)
                    Row(Modifier.fillMaxWidth().height(50.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(34.dp, 44.dp).clickable { onMute(kind) }.semantics { contentDescription = "${ch.name} ${if (ch.muted) "muted, tap to unmute" else "on, tap to mute"}" }, contentAlignment = Alignment.Center) { FlLed(!ch.muted) }
                        if (wide) {
                            FlKnob((ch.pan + 1f) / 2f, { onChannelPan(kind, it * 2f - 1f) }, Modifier.size(26.dp), bipolar = true, label = "${ch.name} pan")
                            FlKnob(ch.volume, { onChannelVolume(kind, it) }, Modifier.size(26.dp), label = "${ch.name} volume")
                            Box(Modifier.size(34.dp, 30.dp).clip(RoundedCornerShape(6.dp)).background(Fl.Dark).border(1.dp, Fl.Edge, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                                Text("${kind.ordinal + 1}", color = Fl.Text, fontSize = 15.sp)
                            }
                        }
                        if (lockedKinds.isNotEmpty()) Box(Modifier.width(12.dp), contentAlignment = Alignment.Center) { if (kind in lockedKinds) Text("🔒", fontSize = 10.sp) }
                        FlChannelButton(ch.name, kind.color, kind == selected, Modifier.width(if (wide) 104.dp else 62.dp).height(34.dp)) { onSelect(kind); if (!kind.isDrum) onOpenRoll(kind) }
                        Box(Modifier.size(5.dp, 32.dp).clip(RoundedCornerShape(2.dp)).background(if (kind == selected) Color(0xFFB5F05A) else Fl.Dark))
                        if (ch.isDrum) {
                            Row(horizontalArrangement = Arrangement.spacedBy(groupGap)) {
                                for (g in 0 until perPage / 4) Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                                    for (i in 0 until 4) {
                                        val step = page * perPage + g * 4 + i
                                        val vel = ch.stepVelocity(step)
                                        FlStep(
                                            on = vel != null, maroon = (step / 4) % 2 == 1,
                                            modifier = Modifier.width(stepW).height(46.dp),
                                            glow = ghosts.any { it.channel == kind && it.step == step },
                                            playhead = step == playheadStep, velocity = (vel ?: 100) / 127f,
                                            label = "${ch.name} step ${step + 1}",
                                            onClick = { lastTap = android.os.SystemClock.uptimeMillis(); onToggle(kind, step) },
                                            onLong = { onVelocity(kind, step) },
                                        )
                                    }
                                }
                            }
                        } else MiniPreview(ch, pattern, page, perPage, Modifier.weight(1f).height(42.dp)) { onSelect(kind); onOpenRoll(kind) }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun MiniPreview(ch: Channel, pattern: Pattern, page: Int, perPage: Int, modifier: Modifier, onClick: () -> Unit) {
    BoxWithConstraints(modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF2E3A42)).border(1.dp, Fl.Edge, RoundedCornerShape(4.dp)).clickable(onClick = onClick)
        .semantics { contentDescription = "Open ${ch.name} in the Piano roll" }) {
        val start = page * perPage * TICKS_PER_STEP
        val span = perPage * TICKS_PER_STEP
        val ns = ch.notes.filter { it.startTick < start + span && it.startTick + it.durationTicks > start }
        if (ns.isEmpty()) Text("Tap to draw notes  ›", color = Fl.Text3, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp))
        val lo = (ns.minOfOrNull { it.pitch } ?: 48) - 1; val hi = (ns.maxOfOrNull { it.pitch } ?: 72) + 1
        ns.forEach { n ->
            val x = maxWidth * ((n.startTick - start).coerceAtLeast(0) / span.toFloat())
            val w = maxWidth * (n.durationTicks / span.toFloat())
            val y = (maxHeight - 8.dp) * (1f - (n.pitch - lo) / (hi - lo).toFloat().coerceAtLeast(1f))
            Box(Modifier.padding(start = x, top = y + 2.dp).width(w.coerceAtLeast(4.dp)).height(4.dp).background(Fl.Note))
        }
    }
}
