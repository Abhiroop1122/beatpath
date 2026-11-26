package com.beatpath.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatpath.app.audio.Sfx
import com.beatpath.app.data.Store
import com.beatpath.app.lessons.Curriculum
import com.beatpath.app.lessons.Practice
import com.beatpath.app.ui.aero.Gel
import com.beatpath.app.ui.aero.GelButton
import com.beatpath.app.ui.aero.GelOrb
import com.beatpath.app.ui.aero.LouBubble
import com.beatpath.app.ui.aero.glass
import com.beatpath.app.ui.aero.isLandscape
import com.beatpath.app.ui.lesson.EarLessons
import com.beatpath.app.ui.lou.Lou
import com.beatpath.app.ui.lou.LouMood
import com.beatpath.app.ui.lou.rememberLou
import com.beatpath.app.ui.theme.AeroColors
import com.beatpath.app.ui.theme.AeroType

/** Practice tab: a Daily mix, spaced review of tricky lessons, and endless drills for every skill. */
@Composable
fun PracticeScreen(store: Store, sfx: Sfx, onStart: (String) -> Unit) {
    val lou = rememberLou(sfx)
    val due = store.dueReviews()
    LaunchedEffect(Unit) {
        lou.say(if (due.isNotEmpty()) "${due.size} to review today — quick redos make them stick." else "Practice never runs out!", LouMood.Happy)
    }
    val cols = if (isLandscape()) 3 else 2
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Practice", style = AeroType.Hero.copy(color = MenuInk, shadow = MenuGlow))
        Row(verticalAlignment = Alignment.Bottom) {
            Lou(Modifier.size(84.dp, 74.dp), mood = lou.mood, mouthOpen = lou.mouth)
            lou.bubble?.let { LouBubble(it, Modifier.weight(1f).padding(start = 4.dp, bottom = 8.dp)) }
        }

        // ---- daily mix ----
        Column(Modifier.fillMaxWidth().glass(22.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GelOrb(Modifier.size(44.dp), Gel.Aqua) { Icon(Icons.Rounded.Bolt, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("DAILY MIX", style = AeroType.Caps)
                    Text("8 questions, made for you", style = AeroType.H2)
                }
            }
            GelButton("Start daily mix", { sfx.select(); onStart(Practice.MIX) }, Modifier.fillMaxWidth(), gel = Gel.Grass)
        }

        // ---- spaced review ----
        Column(Modifier.fillMaxWidth().glass(22.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("REVIEW${if (due.isNotEmpty()) " · ${due.size} DUE" else ""}", style = AeroType.Caps)
            if (due.isEmpty()) Text("Nothing due. Tricky lessons come back here.", style = AeroType.Body)
            due.forEach { id ->
                val title = Curriculum.byId(id)?.title ?: EarLessons.spec(id).title
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { sfx.select(); onStart(id) }.padding(vertical = 10.dp, horizontal = 6.dp)
                    .semantics { contentDescription = "Review $title" }, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Replay, null, tint = AeroColors.PrimaryDeep, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(title, style = AeroType.BodyStrong, modifier = Modifier.weight(1f))
                    Text("Review ›", style = AeroType.Label.copy(color = AeroColors.PrimaryDeep))
                }
            }
        }

        // ---- drills ----
        Text("DRILLS · ENDLESS", style = AeroType.Caps.copy(color = MenuInk, shadow = MenuGlow))
        Practice.Skill.entries.chunked(cols).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { sk -> SkillCard(sk, store, Modifier.weight(1f)) { sfx.select(); onStart(sk.route) } }
                repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SkillCard(sk: Practice.Skill, store: Store, modifier: Modifier, onClick: () -> Unit) {
    val n = store.attempts(sk.id)
    // shown as your real score; the smoothed value is only used to weight the Daily mix
    val acc = store.skills[sk.id]?.let { (a, c) -> if (a == 0) 0f else c / a.toFloat() } ?: 0f
    Column(modifier.glass(20.dp).clickable(onClick = onClick).padding(12.dp)
        .semantics { contentDescription = "${sk.label} drill, ${if (n == 0) "new" else "${(acc * 100).toInt()} percent"}" },
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(sk.label, style = AeroType.H2.copy(fontSize = 16.sp), maxLines = 1)
        // accuracy bar: amber → leaf as you improve
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0x332E9BE6))) {
            if (n > 0) Box(Modifier.fillMaxWidth(acc.coerceIn(0.05f, 1f)).fillMaxHeight().clip(RoundedCornerShape(4.dp))
                .background(if (acc >= 0.8f) AeroColors.Leaf else if (acc >= 0.55f) AeroColors.Primary else AeroColors.Amber))
        }
        Text(if (n == 0) "New · tap to start" else "${(acc * 100).toInt()}% right · $n answer${if (n == 1) "" else "s"}", style = AeroType.Small.copy(fontSize = 11.sp, color = AeroColors.Ink2))
    }
}
