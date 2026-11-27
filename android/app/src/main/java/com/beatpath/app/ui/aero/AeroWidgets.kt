package com.beatpath.app.ui.aero

import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.rotate
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.beatpath.app.ui.lou.Lou
import com.beatpath.app.ui.lou.LouState
import com.beatpath.app.ui.theme.AeroColors
import com.beatpath.app.ui.theme.AeroType

/** Speech bubble with a little tail pointing at Lou (left). Announced to TalkBack as a live region. */
@Composable
fun LouBubble(text: String, modifier: Modifier = Modifier) {
    // one glass card that eases to its new size (no bounce), while the words cross-fade inside it
    Box(modifier.glass(18.dp).animateContentSize(spring(dampingRatio = 1f, stiffness = 380f)).semantics { liveRegion = LiveRegionMode.Polite }) {
        androidx.compose.animation.Crossfade(text, animationSpec = tween(200), label = "bubble") { t ->
            Text(t, style = AeroType.BodyStrong, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
        }
    }
}

/** Lou + what he's saying: the coach used everywhere. */
@Composable
fun LouCoach(lou: LouState, modifier: Modifier = Modifier, size: Int = 84, beatPhase: Float? = null, extra: @Composable () -> Unit = {}) {
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Lou(Modifier.size((size * 1.15f).dp, size.dp), mood = lou.mood, mouthOpen = lou.mouth, beatPhase = beatPhase)
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            lou.bubble?.let { LouBubble(it) }
            extra()
        }
    }
}

data class TrayMsg(val good: Boolean, val headline: String, val body: String, val cta: String, val tip: String? = null)

/** Glass result tray that slides up after Check. Green-leaf for correct, sun-amber for "not yet". */
@Composable
fun ResultTray(msg: TrayMsg?, onCta: () -> Unit, modifier: Modifier = Modifier) {
    // keep the last message so the tray can slide out with its content instead of vanishing
    val last = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(msg) }
    if (msg != null) last.value = msg
    AnimatedVisibility(msg != null, modifier = modifier,
        enter = slideInVertically(spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)) { it } + fadeIn(tween(160)),
        exit = slideOutVertically(tween(220, easing = androidx.compose.animation.core.FastOutLinearInEasing)) { it } + fadeOut(tween(200))) {
        val m = last.value ?: return@AnimatedVisibility
        val edge = if (m.good) AeroColors.Leaf else AeroColors.Amber
        val ink = if (m.good) AeroColors.LeafDeep else Color(0xFF6B4300)
        val land = isLandscape()
        Column(
            Modifier.widthIn(max = 640.dp).fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFFFFFFF), if (m.good) Color(0xFFE3F8E9) else Color(0xFFFFF3D6))))
                .pointerInput(Unit) { detectTapGestures { } }   // taps on the tray never reach the buttons underneath
                .navigationBarsPadding().padding(horizontal = 18.dp, vertical = if (land) 12.dp else 18.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(if (land) 6.dp else 10.dp),
        ) {
            Box(Modifier.fillMaxWidth().padding(bottom = 2.dp).background(edge, RoundedCornerShape(2.dp)).padding(1.5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (m.good) Icons.Rounded.CheckCircle else Icons.Rounded.Hearing, null, tint = ink, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(8.dp))
                Text(m.headline, style = AeroType.H2.copy(color = ink))
            }
            Text(m.body, style = AeroType.Body.copy(color = ink), modifier = Modifier.heightIn(max = if (land) 96.dp else 280.dp).verticalScroll(rememberScrollState()))
            m.tip?.let { tip -> Fold("💡 Tip", ink) { Text(tip, style = AeroType.Body.copy(color = ink, fontSize = 14.sp)) } }
            GelButton(m.cta, onCta, Modifier.fillMaxWidth(), gel = if (m.good) Gel.Grass else Gel.Orange, height = if (land) 42.dp else 46.dp)
        }
    }
}

/** Tap-to-open line: a short label that unfolds extra text (keeps screens light). */
@Composable
fun Fold(label: String, color: Color = AeroColors.PrimaryDeep, startOpen: Boolean = false, content: @Composable () -> Unit) {
    var open by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(startOpen) }
    val turn by androidx.compose.animation.core.animateFloatAsState(if (open) 180f else 0f, spring(dampingRatio = 0.8f, stiffness = 500f), label = "fold")
    Column {
        Row(Modifier.clip(RoundedCornerShape(10.dp)).clickable { open = !open }.padding(vertical = 6.dp, horizontal = 2.dp)
            .semantics { contentDescription = "$label, ${if (open) "tap to fold" else "tap to open"}" }, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = AeroType.Label.copy(color = color))
            Icon(androidx.compose.material.icons.Icons.Rounded.KeyboardArrowDown, null, tint = color, modifier = Modifier.size(20.dp).rotate(turn))
        }
        AnimatedVisibility(open, enter = androidx.compose.animation.expandVertically(spring(dampingRatio = 0.9f, stiffness = 400f)) + fadeIn(),
            exit = androidx.compose.animation.shrinkVertically(tween(180)) + fadeOut(tween(120))) {
            Box(Modifier.padding(top = 2.dp, bottom = 4.dp)) { content() }
        }
    }
}

/** Glass card with a title row that folds its content away; used for long lists (Studio levels, Me). */
@Composable
fun FoldCard(title: String, modifier: Modifier = Modifier, trailing: String? = null, startOpen: Boolean = false, content: @Composable () -> Unit) {
    var open by androidx.compose.runtime.saveable.rememberSaveable(title) { androidx.compose.runtime.mutableStateOf(startOpen) }
    val turn by androidx.compose.animation.core.animateFloatAsState(if (open) 180f else 0f, spring(dampingRatio = 0.8f, stiffness = 500f), label = "foldcard")
    Column(modifier.fillMaxWidth().glass(20.dp)) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable { open = !open }.padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics { contentDescription = "$title${trailing?.let { ", $it" } ?: ""}, ${if (open) "open, tap to fold" else "folded, tap to open"}" },
            verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = AeroType.H2.copy(fontSize = 16.sp), modifier = Modifier.weight(1f))
            trailing?.let { Text(it, style = AeroType.Label.copy(color = AeroColors.Ink2)); Spacer(Modifier.width(6.dp)) }
            Icon(androidx.compose.material.icons.Icons.Rounded.KeyboardArrowDown, null, tint = AeroColors.Ink2, modifier = Modifier.size(22.dp).rotate(turn))
        }
        AnimatedVisibility(open, enter = androidx.compose.animation.expandVertically(spring(dampingRatio = 0.9f, stiffness = 400f)) + fadeIn(),
            exit = androidx.compose.animation.shrinkVertically(tween(180)) + fadeOut(tween(120))) {
            Box(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp)) { content() }
        }
    }
}

/** Small round Lou button (opens the coach when collapsed). */
@Composable
fun LouOrb(lou: LouState, onClick: () -> Unit, modifier: Modifier = Modifier, beatPhase: Float? = null) {
    GelOrb(modifier.clip(RoundedCornerShape(50)).clickable(onClick = onClick), Gel.Aqua) {
        Lou(Modifier.size(46.dp, 40.dp), mood = lou.mood, mouthOpen = lou.mouth, beatPhase = beatPhase)
    }
}
