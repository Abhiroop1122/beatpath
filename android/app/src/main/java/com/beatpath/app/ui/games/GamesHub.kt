package com.beatpath.app.ui.games

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatpath.app.audio.Sfx
import com.beatpath.app.data.Store
import com.beatpath.app.model.NoteColors
import com.beatpath.app.ui.aero.Gel
import com.beatpath.app.ui.aero.GelButton
import com.beatpath.app.ui.aero.LouBubble
import com.beatpath.app.ui.aero.clayPress
import com.beatpath.app.ui.aero.claySurface
import com.beatpath.app.ui.home.MenuGlow
import com.beatpath.app.ui.home.MenuInk
import com.beatpath.app.ui.lou.Lou
import com.beatpath.app.ui.lou.LouMood
import com.beatpath.app.ui.lou.rememberLou
import com.beatpath.app.ui.theme.AeroType
import kotlin.math.sin

/** The Studio tab: mini games with Lou, and the two ways to make your own music. Lessons live on the Learn path only. */
@Composable
fun StudioHub(store: Store, sfx: Sfx, onFree: () -> Unit, onSong: () -> Unit, onGame: (String) -> Unit) {
    val lou = rememberLou(sfx)
    LaunchedEffect(Unit) { lou.say("Play a game with me, or make your own music!", LouMood.Happy) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Studio", style = AeroType.Hero.copy(color = MenuInk, shadow = MenuGlow))
        Row(verticalAlignment = Alignment.Bottom) {
            Lou(Modifier.size(104.dp, 90.dp), mood = lou.mood, mouthOpen = lou.mouth)
            lou.bubble?.let { LouBubble(it, Modifier.weight(1f).padding(bottom = 12.dp)) }
        }
        Text("MINI GAMES", style = AeroType.Caps.copy(color = MenuInk, shadow = MenuGlow))
        BoxWithConstraints {
            val cols = when { maxWidth > 820.dp -> 5; maxWidth > 560.dp -> 3; else -> 2 }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Games.all.chunked(cols).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { g -> GameCard(g, store.best(g.id), Modifier.weight(1f)) { sfx.select(); onGame(g.id) } }
                        repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        Text("MAKE MUSIC", style = AeroType.Caps.copy(color = MenuInk, shadow = MenuGlow))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GelButton("Free play", { sfx.select(); onFree() }, Modifier.weight(1f), gel = Gel.Grass)
            GelButton("Song maker", { sfx.select(); onSong() }, Modifier.weight(1f), gel = Gel.Aqua)
        }
        Text("Free play is one loop. Song maker chains loops A, B and C into a whole song.", style = AeroType.Small.copy(color = MenuInk))
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun GameCard(g: GameInfo, best: Int, modifier: Modifier, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val p = clayPress(src)
    val t by rememberInfiniteTransition(label = "card").animateFloat(0f, 1f, infiniteRepeatable(tween(2400), RepeatMode.Reverse), label = "t")
    val stars = g.starsFor(best)
    Column(modifier.graphicsLayer { scaleX = 1f - p * 0.03f; scaleY = 1f - p * 0.03f }.claySurface(g.gel, 22.dp, depth = 5.dp, press = p)
        .clip(RoundedCornerShape(22.dp)).clickable(src, null, role = Role.Button, onClick = onClick)
        .semantics { contentDescription = "${g.title}: ${g.teaches}. ${if (best > 0) "Best $best, $stars stars" else "Not played yet"}" }
        .padding(start = 12.dp, end = 12.dp, top = 12.dp + 5.dp * p, bottom = 16.dp)) {
        Canvas(Modifier.fillMaxWidth().height(78.dp)) { drawGameArt(g.id, t) }
        Text(g.title, style = AeroType.H2.copy(color = g.gel.text, fontSize = 18.sp))
        Text(g.teaches, style = AeroType.Small.copy(color = g.gel.text.copy(alpha = 0.9f)), maxLines = 2, modifier = Modifier.height(32.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(3) { i -> Icon(Icons.Rounded.Star, null, tint = if (i < stars) Color(0xFFFFE08A) else Color(0x55FFFFFF), modifier = Modifier.size(16.dp)) }
            Spacer(Modifier.weight(1f))
            if (best > 0) Text("Best $best", style = AeroType.Label.copy(color = g.gel.text))
        }
    }
}

/** A small moving picture for each game card. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGameArt(id: String, t: Float) {
    val w = size.width; val h = size.height
    when (id) {
        Games.pianoTiles.id -> {
            // tiles falling into a little rainbow keyboard
            val keys = listOf(0, 2, 4, 5, 7, 9, 11); val kw = w / 7f
            keys.forEachIndexed { i, pc -> drawRoundRect(NoteColors.of(pc).copy(alpha = 0.9f), Offset(i * kw + 1f, h * 0.78f), Size(kw - 2f, h * 0.22f), CornerRadius(4f)) }
            listOf(2 to 0.0f, 4 to 0.35f, 0 to 0.7f).forEach { (lane, ph) ->
                val y = ((t + ph) % 1f) * h * 0.7f
                drawRoundRect(NoteColors.of(keys[lane]), Offset(lane * kw + 3f, y), Size(kw - 6f, h * 0.18f), CornerRadius(6f))
            }
        }
        Games.skyHop.id -> {
            for ((i, y) in listOf(0.75f, 0.45f, 0.2f).withIndex()) {
                val x = w * (0.2f + i * 0.3f)
                drawOval(Color.White, Offset(x - 26f, h * y), Size(52f, 20f)); drawCircle(Color.White, 14f, Offset(x - 4f, h * y))
            }
            drawCircle(Color(0xFF178888), 13f, Offset(w * (0.2f + t * 0.6f), h * (0.68f - t * 0.55f) - sin(t * 3.14f) * 12f))
        }
        Games.beatRun.id -> {
            drawRect(Color(0x66FFFFFF), Offset(0f, h * 0.8f), Size(w, 4f))
            for (i in 0 until 3) { val x = ((i * 0.38f + 1f - t) % 1f) * w; drawRoundRect(NoteColors.of(i * 4), Offset(x, h * 0.55f), Size(22f, 22f), CornerRadius(6f)) }
            drawCircle(Color(0xFF178888), 14f, Offset(w * 0.2f, h * 0.66f - sin(t * 6.28f).coerceAtLeast(0f) * 24f))
        }
        Games.balloonPop.id -> {
            for (i in 0 until 4) {
                val x = w * (0.15f + i * 0.23f); val y = h * (0.85f - ((t + i * 0.27f) % 1f) * 0.7f)
                drawLine(Color(0x88FFFFFF), Offset(x, y + 14f), Offset(x, y + 30f), 2f)
                drawOval(NoteColors.of(listOf(0, 4, 7, 9)[i]), Offset(x - 12f, y - 14f), Size(24f, 28f))
            }
        }
        Games.chordCake.id -> {
            listOf(0, 4, 7).forEachIndexed { i, pc ->
                val lw = w * (0.6f - i * 0.1f)
                drawRoundRect(NoteColors.of(pc), Offset(w / 2 - lw / 2, h * 0.75f - i * 18f - (if (i == 2) sin(t * 3.14f) * 4f else 0f)), Size(lw, 16f), CornerRadius(8f))
            }
            drawRoundRect(Color(0xFFFFF1E8), Offset(w / 2 - 3f, h * 0.18f), Size(6f, 16f), CornerRadius(3f))
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF59D), Color(0x00FF9800)), Offset(w / 2, h * 0.14f), 10f + t * 3f), 10f + t * 3f, Offset(w / 2, h * 0.14f))
        }
        else -> {
            listOf(0, 2, 4, 7, 9).forEachIndexed { i, pc ->
                val a = Math.PI * (1.0 - i / 4.0)
                val c = Offset(w / 2 + (w * 0.36f * kotlin.math.cos(a)).toFloat(), h * 0.7f - (h * 0.4f * kotlin.math.sin(a)).toFloat())
                val on = (t * 5).toInt() == i
                drawCircle(NoteColors.of(pc), if (on) 15f else 12f, c)
            }
        }
    }
}
