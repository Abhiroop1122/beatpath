package com.beatpath.app.ui.fl

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** FL Studio default-theme colours, measured from Image-Line manual screenshots (research/fl-ui-reference.md). */
object Fl {
    val Frame = Color(0xFF4E565E)
    val Title = Color(0xFF434A51)
    val Panel = Color(0xFF5B636B)
    val Deep = Color(0xFF2B3035)
    val Dark = Color(0xFF3A4147)
    val Edge = Color(0xFF262B2F)
    val Text = Color(0xFFE9EEF1)
    val Text2 = Color(0xFFB4BDC4)
    val Text3 = Color(0xFF8E979E)
    val Lime = Color(0xFFA9E34B)
    val LimeDark = Color(0xFF1E2A10)
    val Lane = Color(0xFF3A4852)
    val LaneBlack = Color(0xFF33404A)
    val Ruler = Color(0xFF2B353D)
    val Note = Color(0xFFB7EDB4)
    val NoteBottom = Color(0xFF93D990)
    val NoteEdge = Color(0xFF5E9E5B)
    val NoteInk = Color(0xFF2F5E33)
    val NoteSel = Color(0xFFC9D9F7)
    val Ghost = Color(0x995A6A75)
    val Red = Color(0xFFE5484D)
    val Lcd = Color(0xFFD9E2E8)
    val Orange = Color(0xFFF2A33A)
}

/** FL step button: grey groups light ice-blue, maroon groups light pink, glossy top, small LED line when lit. */
@Composable
fun FlStep(on: Boolean, maroon: Boolean, modifier: Modifier, glow: Boolean = false, playhead: Boolean = false, velocity: Float = 1f, label: String, onClick: () -> Unit, onLong: (() -> Unit)? = null) {
    val (top, bottom) = when {
        on && maroon -> Color(0xFFF6CDD0) to Color(0xFFD38E95)
        on -> Color(0xFFE6EEF3) to Color(0xFFA9BCC8)
        maroon -> Color(0xFF6A4A4F) to Color(0xFF4E3438)
        else -> Color(0xFF4A5157) to Color(0xFF363C41)
    }
    @OptIn(ExperimentalFoundationApi::class)
    Box(
        modifier
            .clip(RoundedCornerShape(5.dp))
            .combinedClickable(onLongClick = onLong, onClick = onClick)
            .semantics { contentDescription = "$label, ${if (on) "on" else "off"}" }
            .drawBehind {
                val r = CornerRadius(5.dp.toPx())
                drawRoundRect(Brush.verticalGradient(listOf(top, bottom)), cornerRadius = r)
                if (playhead) drawRoundRect(Color(0x55FFFFFF), cornerRadius = r)
                drawRoundRect(Color(0x26FFFFFF), Offset(3.dp.toPx(), 2.dp.toPx()), Size(size.width - 6.dp.toPx(), size.height * 0.28f), CornerRadius(3.dp.toPx()))
                if (on) {
                    // velocity shown as the LED line's length
                    val w = (size.width - 12.dp.toPx()) * velocity.coerceIn(0.15f, 1f)
                    drawRoundRect(Fl.Red, Offset((size.width - w) / 2, 6.dp.toPx()), Size(w, 2.dp.toPx()), CornerRadius(1f))
                }
                drawRoundRect(Color(0xFF1F2428), cornerRadius = r, style = Stroke(1.dp.toPx()))
                if (glow) drawRoundRect(Color(0xFFFFE26A), cornerRadius = r, style = Stroke(2.5.dp.toPx()))
            }
    )
}

@Composable
fun FlLed(on: Boolean, modifier: Modifier = Modifier.size(12.dp)) {
    Canvas(modifier) {
        if (on) {
            drawCircle(Color(0x55A9E34B), size.minDimension * 0.75f)
            drawCircle(Brush.radialGradient(listOf(Color(0xFFD8FF8F), Color(0xFF8CD43A)), center, size.minDimension / 2), size.minDimension / 2)
        } else drawCircle(Brush.radialGradient(listOf(Color(0xFF6B7278), Color(0xFF3A4045)), center, size.minDimension / 2), size.minDimension / 2)
        drawCircle(Color(0xFF1F2428), size.minDimension / 2, style = Stroke(1f))
    }
}

/**
 * FL-style rotary knob (270° travel). Drag up/down to turn — big drag area so it works on a phone.
 * [value] is 0..1; [bipolar] draws the arc from the centre (pan).
 */
@Composable
fun FlKnob(value: Float, onChange: (Float) -> Unit, modifier: Modifier = Modifier.size(28.dp), bipolar: Boolean = false, accent: Color = Fl.Lime, label: String = "knob") {
    Canvas(
        modifier
            .semantics { contentDescription = "$label ${(value * 100).toInt()} percent" }
            .pointerInput(Unit) {
                var v = value
                detectVerticalDragGestures(onDragStart = { v = value }) { change, dy ->
                    change.consume(); v = (v - dy / 220f).coerceIn(0f, 1f); onChange(v)
                }
            }
    ) {
        val r = size.minDimension / 2
        val start = 135f
        val sweep = 270f
        drawArc(Color(0xFF23272B), start, sweep, false, Offset(center.x - r, center.y - r), Size(r * 2, r * 2), style = Stroke(r * 0.18f, cap = StrokeCap.Round))
        val from = if (bipolar) 270f else start
        val to = start + sweep * value
        drawArc(accent, from, to - from, false, Offset(center.x - r, center.y - r), Size(r * 2, r * 2), style = Stroke(r * 0.18f, cap = StrokeCap.Round))
        drawCircle(Brush.verticalGradient(listOf(Color(0xFF5C646B), Color(0xFF23272B))), r * 0.72f)
        drawCircle(Color(0xFF2B3035), r * 0.6f)
        val a = (start + sweep * value) * PI.toFloat() / 180f
        drawLine(Fl.Text, center + Offset(cos(a) * r * 0.15f, sin(a) * r * 0.15f), center + Offset(cos(a) * r * 0.55f, sin(a) * r * 0.55f), r * 0.14f, StrokeCap.Round)
    }
}

/** Grey channel button with the channel name (FL's rack "Channel button"). */
@Composable
fun FlChannelButton(name: String, tint: Color, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(6.dp))
            .background(Brush.verticalGradient(listOf(tint, Color(0xFF4E565E))))
            .border(1.dp, if (selected) Fl.Lime else Color(0xFF2A2F33), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "$name channel" },
        contentAlignment = Alignment.Center,
    ) { Text(name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1) }
}

/** The main-toolbar transport: PAT/SONG stack, play, stop, record and the tempo LCD. */
@Composable
fun FlTransport(playing: Boolean, bpm: Int, onPlay: () -> Unit, onStop: () -> Unit, onTempo: () -> Unit, modifier: Modifier = Modifier, compact: Boolean = false) {
    val bw = if (compact) 40.dp else 46.dp
    Row(modifier.clip(RoundedCornerShape(10.dp)).background(Fl.Title).padding(5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.clip(RoundedCornerShape(8.dp)).background(Fl.Dark).border(1.dp, Fl.Edge, RoundedCornerShape(8.dp))) {
            Box(Modifier.size(bw, 40.dp).background(if (playing) Color(0xFF4A5A3A) else Color.Transparent).clickable(onClick = onPlay).semantics { contentDescription = if (playing) "Pause" else "Play" }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.PlayArrow, null, tint = if (playing) Fl.Lime else Fl.Text, modifier = Modifier.size(24.dp))
            }
            Box(Modifier.width(1.dp).height(40.dp).background(Fl.Edge))
            Box(Modifier.size(bw, 40.dp).clickable(onClick = onStop).semantics { contentDescription = "Stop" }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Stop, null, tint = Fl.Text, modifier = Modifier.size(22.dp))
            }
        }
        Row(
            Modifier.height(40.dp).clip(RoundedCornerShape(8.dp)).background(Brush.verticalGradient(listOf(Color(0xFFE6EDF1), Color(0xFFC9D4DB))))
                .border(1.dp, Color(0xFF8E9AA2), RoundedCornerShape(8.dp)).clickable(onClick = onTempo).padding(horizontal = if (compact) 7.dp else 10.dp)
                .semantics { contentDescription = "Tempo $bpm BPM, tap to change" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("$bpm", color = Color(0xFF1E2428), fontSize = 21.sp, fontWeight = FontWeight.Medium)
            Text(".000", color = Color(0xFF4A545B), fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 5.dp))
        }
    }
}

/** FL window chrome: dark title bar with a speaker glyph, title and right-side extras. */
@Composable
fun FlWindow(title: String, modifier: Modifier = Modifier, extras: @Composable () -> Unit = {}, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(modifier.background(Fl.Frame).border(1.dp, Fl.Edge)) {
        Row(Modifier.fillMaxWidth().height(34.dp).background(Fl.Title).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("▸", color = Fl.Text2, fontSize = 11.sp)
            Text("  🔈 ", color = Fl.Text, fontSize = 11.sp)
            Text(title, color = Fl.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1)
            extras()
        }
        content()
    }
}

/** Vertical peak meter + fader for the Mixer (green → yellow → red, clip lamp on top). */
@Composable
fun FlMeter(levelDb: Float, clipped: Boolean, modifier: Modifier = Modifier.width(8.dp).fillMaxHeight()) {
    Canvas(modifier) {
        drawRect(Color(0xFF1C2024))
        val norm = ((levelDb + 48f) / 48f).coerceIn(0f, 1f)
        val h = size.height - 6.dp.toPx()
        drawRect(Brush.verticalGradient(listOf(Color(0xFFE5484D), Color(0xFFF2C14E), Color(0xFF7FD44A), Color(0xFF3E9E3A)), 0f, h),
            Offset(0f, 6.dp.toPx() + h * (1 - norm)), Size(size.width, h * norm))
        drawRect(if (clipped) Fl.Red else Color(0xFF3A1F21), Offset.Zero, Size(size.width, 4.dp.toPx()))
    }
}

@Composable
fun FlFader(valueDb: Float, onChange: (Float) -> Unit, modifier: Modifier, label: String) {
    // range −30 … +5.6 dB like FL insert faders (top region more resolution)
    val minDb = -30f; val maxDb = 5.6f
    val norm = ((valueDb - minDb) / (maxDb - minDb)).coerceIn(0f, 1f)
    Canvas(
        modifier.semantics { contentDescription = "$label fader ${"%.1f".format(valueDb)} dB" }
            .pointerInput(Unit) {
                var v = norm
                detectVerticalDragGestures(onDragStart = { v = ((valueDb - minDb) / (maxDb - minDb)) }) { c, dy ->
                    c.consume(); v = (v - dy / size.height).coerceIn(0f, 1f); onChange(minDb + v * (maxDb - minDb))
                }
            }
    ) {
        val cx = size.width / 2
        drawRoundRect(Color(0xFF1C2024), Offset(cx - 2.dp.toPx(), 0f), Size(4.dp.toPx(), size.height), CornerRadius(2f))
        val zero = size.height * (1 - (0f - minDb) / (maxDb - minDb))
        drawLine(Fl.Text3, Offset(cx - 8.dp.toPx(), zero), Offset(cx + 8.dp.toPx(), zero), 1f)
        val y = size.height * (1 - norm)
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFC9D2D9), Color(0xFF7D878F)), y - 9.dp.toPx(), y + 9.dp.toPx()),
            Offset(cx - 11.dp.toPx(), y - 9.dp.toPx()), Size(22.dp.toPx(), 18.dp.toPx()), CornerRadius(3.dp.toPx()))
        drawLine(Color(0xFF2B3035), Offset(cx - 9.dp.toPx(), y), Offset(cx + 9.dp.toPx(), y), 2f)
    }
}

fun Modifier.flPanel(): Modifier = this.background(Fl.Panel)

