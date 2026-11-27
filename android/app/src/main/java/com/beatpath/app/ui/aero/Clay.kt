package com.beatpath.app.ui.aero

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.beatpath.app.audio.Sfx
import com.beatpath.app.ui.lou.Lou
import com.beatpath.app.ui.lou.LouMood
import com.beatpath.app.ui.theme.AeroType

/**
 * The clay look, shared by every control: a soft contact shadow, a darker base the piece sits on,
 * a matte body lit from the top left (a radial "shader" of light), fine grain and a soft highlight.
 * [press] 0..1 sinks the body onto its base. The bottom [depth] of the box is the base.
 */
fun Modifier.claySurface(gel: Gel, corner: Dp, depth: Dp = 4.dp, press: Float = 0f, outline: Color? = null): Modifier = this.drawBehind {
    val d = depth.toPx(); val r = CornerRadius(corner.toPx().coerceAtMost((size.height - d) / 2f))
    val body = Size(size.width, size.height - d)
    drawRoundRect(Color(0x24083A5A), Offset(0f, d + 1.5.dp.toPx()), body, r)                         // soft contact shadow
    drawRoundRect(Color(0x10083A5A), Offset(-1.dp.toPx(), d + 3.dp.toPx()), Size(body.width + 2.dp.toPx(), body.height), r)
    drawRoundRect(outline ?: gel.edge, Offset(0f, d), body, r)                                           // base
    val top = d * press
    drawRoundRect(Brush.verticalGradient(listOf(gel.top, gel.bottom), top, top + body.height), Offset(0f, top), body, r)
    // light from the top left, falling off to the bottom right: reads as a rounded, pressed-in surface
    drawRoundRect(Brush.radialGradient(listOf(Color(0x30FFFFFF), Color(0x00FFFFFF)), Offset(body.width * 0.25f, top + body.height * 0.15f), maxOf(body.width, body.height) * 0.9f), Offset(0f, top), body, r)
    drawRoundRect(Brush.verticalGradient(listOf(Color(0x00000000), Color(0x14000000)), top + body.height * 0.6f, top + body.height), Offset(0f, top), body, r)
    drawRoundRect(clayGrainBrush, Offset(0f, top), body, r)
    val hl = body.height.coerceAtMost(40.dp.toPx())
    drawRoundRect(Brush.verticalGradient(listOf(Color(if (press > 0.5f) 0x2EFFFFFF else 0x4DFFFFFF), Color(0x00FFFFFF)), top, top + hl * 0.55f),
        Offset(r.x * 0.5f, top + 2.dp.toPx()), Size(body.width - r.x, hl * 0.45f), CornerRadius(r.x))
    if (outline != null) drawRoundRect(outline, Offset(0f, top), body, r, style = androidx.compose.ui.graphics.drawscope.Stroke(2.5.dp.toPx()))
}

/** A soft clay card (the old glass panels). Opaque, warm white, with a shallow base and a little shadow. */
fun Modifier.clayCard(corner: Dp = 22.dp, gel: Gel = Gel.Cream): Modifier = this.claySurface(gel, corner, depth = 3.dp).clip(RoundedCornerShape(corner))

/** Press state for clay controls: 0 at rest, 1 fully pressed, with a little spring. */
@Composable
fun clayPress(src: MutableInteractionSource): Float {
    val pressed by src.collectIsPressedAsState()
    val p by animateFloatAsState(if (pressed) 1f else 0f, spring(dampingRatio = 0.55f, stiffness = 900f), label = "press")
    return p
}

/** Round clay icon button (close, back, play…). */
@Composable
fun ClayIconButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, gel: Gel = Gel.Cream,
                   size: Dp = 44.dp, iconSize: Dp = 22.dp, enabled: Boolean = true, tint: Color? = null) {
    val src = remember { MutableInteractionSource() }
    val p = clayPress(src)
    val g = if (enabled || gel == Gel.Slate) gel else Gel.Off
    Box(modifier.size(size, size + 3.dp).claySurface(g, size / 2, depth = 3.dp, press = p)
        .clip(RoundedCornerShape(size / 2)).clickable(src, null, enabled = enabled, role = Role.Button, onClick = onClick)
        .semantics { contentDescription = description }, contentAlignment = Alignment.TopCenter) {
        Icon(icon, null, tint = tint ?: g.text, modifier = Modifier.padding(top = (size - iconSize) / 2 + 3.dp * p).size(iconSize))
    }
}

/** Small clay pill for chips and toggles. */
@Composable
fun ClayChip(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, gel: Gel = Gel.Cream, description: String? = null, fontSize: Int = 12, enabled: Boolean = true) {
    val src = remember { MutableInteractionSource() }
    val p = clayPress(src)
    Box(modifier.claySurface(gel, 14.dp, depth = 3.dp, press = p).clip(RoundedCornerShape(14.dp))
        .clickable(src, null, enabled = enabled, role = Role.Button, onClick = onClick)
        .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier)
        .defaultMinSize(minHeight = 42.dp).padding(start = 12.dp, end = 12.dp, top = 6.dp + 3.dp * p, bottom = 9.dp - 3.dp * p), contentAlignment = Alignment.Center) {
        Text(text, style = AeroType.Label.copy(color = gel.text, fontSize = fontSize.sp), maxLines = 1)
    }
}

/** A clay dialog that springs in. */
@Composable
fun ClayDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val s = remember { Animatable(0.85f) }
        LaunchedEffect(Unit) { s.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 500f)) }
        Box(Modifier.padding(24.dp).widthIn(max = 440.dp).fillMaxWidth().graphicsLayer { scaleX = s.value; scaleY = s.value; alpha = ((s.value - 0.85f) / 0.15f).coerceIn(0f, 1f) }
            .clayCard(28.dp).padding(20.dp)) { content() }
    }
}

/** "Oh no!" Lou is sad when you try to leave a lesson; he sniffles, and cheers if you stay. */
@Composable
fun LeaveDialog(sfx: Sfx, onLeave: () -> Unit, onStay: () -> Unit,
                title: String = "Oh no! Do you want to leave?", body: String = "Your progress in this lesson won't be saved. Stay a little longer?") {
    LaunchedEffect(Unit) { sfx.louSad() }
    ClayDialog(onDismiss = { sfx.louHappy(); onStay() }) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Lou(Modifier.size(132.dp, 116.dp), mood = LouMood.Sad)
            Text(title, style = AeroType.Title.copy(fontSize = 22.sp), textAlign = TextAlign.Center)
            Text(body, style = AeroType.Body, textAlign = TextAlign.Center)
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GelButton("Leave", { onLeave() }, Modifier.weight(1f), gel = Gel.Silver)
                GelButton("Keep going", { sfx.louHappy(); onStay() }, Modifier.weight(1.3f), gel = Gel.Grass)
            }
        }
    }
}

/** FL Studio controls keep FL's own flat look (not clay): dark fill, a hairline edge, and FL's lime when on. */
fun Modifier.flBox(corner: Dp, on: Boolean = false, lime: Boolean = false): Modifier =
    this.clip(RoundedCornerShape(corner))
        .background(if (on && lime) Color(0xFFA9E34B) else if (on) Color(0xFF2A3B1E) else Color(0xFF2B3237))
        .border(if (on && !lime) 1.5.dp else 1.dp, if (on) Color(0xFFA9E34B) else Color(0xFF1E2428), RoundedCornerShape(corner))

/** A clay on/off switch: a sunken track and a round clay knob that slides across. */
@Composable
fun ClayToggle(on: Boolean, onChange: (Boolean) -> Unit, description: String, modifier: Modifier = Modifier) {
    val k by animateFloatAsState(if (on) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 500f), label = "toggle")
    Box(modifier.size(62.dp, 40.dp).claySurface(if (on) Gel.Grass else Gel.Off, 18.dp, depth = 3.dp).clip(RoundedCornerShape(15.dp))
        .clickable(role = Role.Switch) { onChange(!on) }.semantics { contentDescription = "$description, ${if (on) "on" else "off"}" }) {
        Box(Modifier.padding(start = (3 + 24 * k).dp, top = 3.dp).size(31.dp, 33.dp).claySurface(Gel.Cream, 15.dp, depth = 2.dp))
    }
}
