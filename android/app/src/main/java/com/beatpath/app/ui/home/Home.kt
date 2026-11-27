package com.beatpath.app.ui.home
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Piano
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatpath.app.audio.AudioEngine
import com.beatpath.app.audio.Sfx
import com.beatpath.app.data.EarIds
import com.beatpath.app.data.Store
import com.beatpath.app.lessons.Curriculum
import com.beatpath.app.lessons.Track
import com.beatpath.app.model.Project
import com.beatpath.app.ui.aero.AeroBackground
import com.beatpath.app.ui.aero.Gel
import com.beatpath.app.ui.aero.GelButton
import com.beatpath.app.ui.aero.GelOrb
import com.beatpath.app.ui.aero.LouBubble
import com.beatpath.app.ui.aero.glass
import com.beatpath.app.ui.aero.isLandscape
import com.beatpath.app.ui.aero.sideSafe
import com.beatpath.app.ui.lesson.EarLessons
import com.beatpath.app.ui.lou.Lou
import com.beatpath.app.ui.lou.LouMood
import com.beatpath.app.ui.lou.rememberLou
import com.beatpath.app.ui.theme.AeroColors
import com.beatpath.app.ui.theme.AeroType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Text colours for the light-blue menu artwork: deep sky ink with a soft white glow so it reads on any part of the image. */
internal val MenuInk = Color(0xFF083F70)
internal val MenuInk2 = Color(0xFF1E4F78)
internal val MenuGlow = androidx.compose.ui.graphics.Shadow(Color(0xCCFFFFFF), androidx.compose.ui.geometry.Offset(0f, 1f), 8f)

enum class Tab(val label: String, val icon: ImageVector) { Learn("Learn", Icons.Rounded.Route), Practice("Practice", Icons.Rounded.Bolt), Studio("Studio", Icons.Rounded.Tune), Loops("My loops", Icons.Rounded.Album), Me("Me", Icons.Rounded.Person) }

// ============================ Welcome ============================
@Composable
fun WelcomeScreen(sfx: Sfx, onStart: () -> Unit, onJump: () -> Unit) {
    val lou = rememberLou(sfx)
    LaunchedEffect(Unit) {
        delay(500); lou.react(LouMood.Happy); delay(400)
        lou.say("Hi, I'm Lou! Let's learn music theory and melody — step by step, all the way to FL Studio.", LouMood.Happy)
    }
    if (isLandscape()) AeroBackground(photo = true) {
        // sideways: Lou and his greeting on the left, the name and buttons on the right
        Row(Modifier.fillMaxSize().sideSafe().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                lou.bubble?.let { LouBubble(it) }
                Lou(Modifier.size(190.dp, 166.dp), mood = lou.mood, mouthOpen = lou.mouth)
            }
            Spacer(Modifier.width(24.dp))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("beatpath", style = AeroType.Hero.copy(color = MenuInk, shadow = MenuGlow, fontSize = 44.sp))
                Text("music theory & melody, from zero to FL Studio", style = AeroType.Label.copy(color = MenuInk2, shadow = MenuGlow), textAlign = TextAlign.Center)
                Spacer(Modifier.height(22.dp))
                GelButton("Start from zero", { sfx.select(); onStart() }, Modifier.fillMaxWidth(), gel = Gel.Grass)
                Spacer(Modifier.height(10.dp))
                GelButton("I already make beats", { sfx.tap(); onJump() }, Modifier.fillMaxWidth(), gel = Gel.Silver)
                Spacer(Modifier.height(8.dp))
                Text("No account · works offline · free", style = AeroType.Small.copy(color = MenuInk2, shadow = MenuGlow))
            }
        }
    } else AeroBackground(photo = true) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(0.6f))
            Text("beatpath", style = AeroType.Hero.copy(color = MenuInk, shadow = MenuGlow, fontSize = 46.sp))
            Text("music theory & melody, from zero to FL Studio", style = AeroType.Label.copy(color = MenuInk2, shadow = MenuGlow))
            Spacer(Modifier.height(18.dp))
            lou.bubble?.let { LouBubble(it, Modifier.padding(horizontal = 6.dp)) }
            Lou(Modifier.size(260.dp, 226.dp), mood = lou.mood, mouthOpen = lou.mouth)
            Spacer(Modifier.weight(1f))
            GelButton("Start from zero", { sfx.select(); onStart() }, Modifier.fillMaxWidth(), gel = Gel.Grass)
            Spacer(Modifier.height(12.dp))
            GelButton("I already make beats", { sfx.tap(); onJump() }, Modifier.fillMaxWidth(), gel = Gel.Silver)
            Spacer(Modifier.height(10.dp))
            Text("No account · works offline · free", style = AeroType.Small.copy(color = MenuInk2, shadow = MenuGlow))
        }
    }
}

// ============================ Scaffold ============================
@Composable
fun HomeScaffold(tab: Tab, sfx: Sfx, onTab: (Tab) -> Unit, content: @Composable () -> Unit) {
    if (isLandscape()) AeroBackground(photo = true) {
        // sideways: a glass navigation rail on the left, content centred at a readable width
        Row(Modifier.fillMaxSize().sideSafe().statusBarsPadding()) {
            Column(Modifier.fillMaxHeight().navigationBarsPadding().padding(start = 8.dp, top = 8.dp, bottom = 8.dp).glass(26.dp).padding(vertical = 8.dp, horizontal = 4.dp),
                verticalArrangement = Arrangement.SpaceEvenly, horizontalAlignment = Alignment.CenterHorizontally) {
                Tab.entries.forEach { t -> TabItem(t, t == tab) { sfx.tap(); onTab(t) } }
            }
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.widthIn(max = 680.dp).fillMaxSize()) { content() }
            }
        }
    } else AeroBackground(photo = true) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Box(Modifier.weight(1f)) { content() }
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp).glass(26.dp).padding(6.dp), horizontalArrangement = Arrangement.SpaceAround) {
                Tab.entries.forEach { t -> TabItem(t, t == tab) { sfx.tap(); onTab(t) } }
            }
        }
    }
}

@Composable
private fun TabItem(t: Tab, on: Boolean, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(18.dp)).then(if (on) Modifier.background(Color(0x332E9BE6)) else Modifier)
        .clickable(onClick = onClick).padding(horizontal = 9.dp, vertical = 6.dp)
        .semantics { contentDescription = "${t.label} tab${if (on) ", selected" else ""}" }, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(t.icon, null, tint = if (on) AeroColors.PrimaryDeep else AeroColors.Ink2, modifier = Modifier.size(24.dp))
        Text(t.label, style = AeroType.Small.copy(color = if (on) AeroColors.PrimaryDeep else AeroColors.Ink2, fontSize = 11.sp))
    }
}

// ============================ Learn path ============================
private data class Node(val id: String, val title: String, val idea: String, val minutes: Int, val icon: ImageVector, val jam: Boolean)
private data class Section(val number: Int, val name: String, val blurb: String, val nodes: List<Node>, val gel: Gel)

private fun sections(): List<Section> {
    val ear = Section(0, "Ear training", "Hear pitch before you make it.", EarIds.all.map { id ->
        val s = EarLessons.spec(id); Node(id, s.title, s.intro, 2, Icons.Rounded.Hearing, false) }, Gel.Aqua)
    val basics = listOf(1 to ("Music basics" to "Notes, the keyboard and the rainbow colors."), 2 to ("How notes fit together" to "Counting, scales, chords and what note comes next.")).map { (part, t) ->
        Section(-3 + part, t.first, t.second, com.beatpath.app.lessons.Basics.lessons.filter { it.part == part }.map { Node(it.id, it.title, it.idea, it.minutes, Icons.Rounded.Piano, false) }, Gel.Orange)
    }
    return basics + listOf(ear) + Curriculum.levels.map { lv ->
        val icon = when (lv.track) { Track.BEATS -> Icons.Rounded.GridOn; Track.NOTES, Track.HARMONY, Track.MELODY, Track.BASS -> Icons.Rounded.Piano
            Track.ARRANGE -> Icons.Rounded.MusicNote; Track.SOUND -> Icons.Rounded.Waves; Track.MIX, Track.MASTER -> Icons.Rounded.Tune }
        val gel = when (lv.track) { Track.SOUND, Track.MIX, Track.MASTER -> Gel.Orange; Track.BEATS -> Gel.Aqua; else -> Gel.Grass }
        Section(lv.number, lv.name, lv.blurb, Curriculum.inLevel(lv.number).map { Node(it.id, it.title, it.idea, it.minutes, icon, it.jam) }, gel)
    }
}

@Composable
fun LearnPath(store: Store, sfx: Sfx, onPractice: () -> Unit = {}, onSong: () -> Unit = {}, onShop: () -> Unit = {}, onStart: (String) -> Unit) {
    val secs = remember { sections() }
    val lou = rememberLou(sfx)
    val next = store.nextLesson()
    val celebrateId = remember { store.takeJustCompleted() }
    var open by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    androidx.activity.compose.BackHandler(enabled = open != null) { open = null }
    LaunchedEffect(Unit) {
        val idx = secs.indexOfFirst { s -> s.nodes.any { it.id == next } }
        if (idx > 0) listState.scrollToItem(idx + 1)
        lou.say(when {
            store.courseComplete() -> "You finished the course! Keep sharp in Practice."
            store.completed.isEmpty() -> "Tap the glowing bubble to start!"
            else -> "Welcome back! Tap the glowing bubble."
        }, LouMood.Happy)
    }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.fillMaxWidth().glass(20.dp).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("beatpath", style = AeroType.H2.copy(color = AeroColors.PrimaryDeep), modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.MusicNote, "Practice days", tint = Color(0xFFB04E06), modifier = Modifier.size(20.dp)); Text("${store.streak()}", style = AeroType.H2.copy(color = Color(0xFFB04E06)))
                Spacer(Modifier.width(10.dp))
                XpPill(store.balance, Modifier.clickable { sfx.tap(); onShop() }.semantics { contentDescription = "${store.balance} XP to spend. Open Lou's Shop" })
            }
        }
        item {
            lou.bubble?.let { b ->
                Row(verticalAlignment = Alignment.Bottom) {
                    Lou(Modifier.size(64.dp, 56.dp), mood = lou.mood, mouthOpen = lou.mouth)
                    LouBubble(b, Modifier.weight(1f).padding(start = 4.dp, bottom = 6.dp))
                }
            }
        }
        if (store.courseComplete()) item { CourseComplete(store, sfx, onPractice, onSong, onStart) }
        items(secs, key = { it.number }) { sec ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val done = sec.nodes.count { it.id in store.completed }
                Row(Modifier.fillMaxWidth().glass(22.dp).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    GelOrb(Modifier.size(44.dp), sec.gel) { Text(when { sec.number == 0 -> "♪"; sec.number < 0 -> "${sec.number + 3}"; else -> "${sec.number}" }, style = AeroType.H2.copy(color = Color.White)) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(when { sec.number == -2 -> "START HERE · BASICS 1"; sec.number == -1 -> "BASICS 2"; sec.number == 0 -> "EAR WARM-UP"; else -> "LEVEL ${sec.number}" }, style = AeroType.Caps)
                        Text(sec.name, style = AeroType.H2)
                        Text(sec.blurb, style = AeroType.Body.copy(fontSize = 13.sp))
                    }
                    Text("$done/${sec.nodes.size}", style = AeroType.Label.copy(color = AeroColors.Ink2))
                }
                sec.nodes.forEachIndexed { i, node ->
                    val unlocked = store.isUnlocked(node.id); val isDone = node.id in store.completed
                    val gold = node.id in store.unassisted; val current = node.id == next
                    val x = listOf(0, 56, 84, 56, 0, -56, -84, -56)[i % 8]
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.offset(x = x.dp)) {
                            PathOrb(node, unlocked, isDone, gold, current, node.id == celebrateId) { sfx.tap(); open = if (open == node.id) null else node.id }
                        }
                        AnimatedVisibility(open == node.id, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                            Column(Modifier.padding(top = 10.dp).fillMaxWidth().glass(22.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("${if (node.jam) "JAM · " else ""}ABOUT ${node.minutes} MIN${if (isDone) " · PRACTICE AGAIN" else ""}", style = AeroType.Caps)
                                Text(node.title, style = AeroType.Title)
                                Text(if (unlocked) node.idea else "Finish the lesson before this one to unlock it — or turn on “Unlock all lessons” in Me.", style = AeroType.Body)
                                if (unlocked) GelButton(if (isDone) "Practice again" else "Start", { open = null; sfx.select(); onStart(node.id) }, Modifier.fillMaxWidth(), gel = Gel.Grass)
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

/** Shown on the Learn path once every lesson is done: celebrate, then point to what's next. */
@Composable
private fun CourseComplete(store: Store, sfx: Sfx, onPractice: () -> Unit, onSong: () -> Unit, onStart: (String) -> Unit) {
    val notGold = store.order.filter { it !in store.unassisted }
    Column(Modifier.fillMaxWidth().glass(24.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Lou(Modifier.size(120.dp, 104.dp), mood = LouMood.Cheer)
        Text("COURSE COMPLETE", style = AeroType.Caps)
        Text("All ${store.order.size} lessons done!", style = AeroType.Title, textAlign = TextAlign.Center)
        Text("⭐ ${store.xp} XP · ${store.unassisted.size} solo", style = AeroType.BodyStrong, textAlign = TextAlign.Center)
        GelButton("Practice", { sfx.select(); onPractice() }, Modifier.fillMaxWidth(), gel = Gel.Grass)
        GelButton("Make a song", { sfx.select(); onSong() }, Modifier.fillMaxWidth(), gel = Gel.Aqua)
        if (notGold.isNotEmpty()) GelButton("Redo for gold · ${notGold.size}", { sfx.select(); onStart(notGold.first()) }, Modifier.fillMaxWidth(), gel = Gel.Orange)
    }
}

/** Level bubble in clay: green tick when done (every finished lesson), orange star when playable, grey when locked. */
@Composable
private fun PathOrb(node: Node, unlocked: Boolean, done: Boolean, gold: Boolean, current: Boolean, celebrate: Boolean, onClick: () -> Unit) {
    val breathe by rememberInfiniteTransition(label = "orb").animateFloat(0f, 1f,
        infiniteRepeatable(tween(1400, easing = androidx.compose.animation.core.FastOutSlowInEasing), RepeatMode.Reverse), label = "b")
    val src = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.9f else 1f, spring(dampingRatio = 0.45f, stiffness = 700f), label = "press")
    // a lesson you just finished pops in: starts small, springs past full size, settles
    val pop = remember { androidx.compose.animation.core.Animatable(if (celebrate) 0.55f else 1f) }
    LaunchedEffect(celebrate) { if (celebrate) { kotlinx.coroutines.delay(250); pop.animateTo(1f, spring(dampingRatio = 0.38f, stiffness = 260f)) } }
    val img = when { done -> com.beatpath.app.R.drawable.clay_tick; unlocked -> com.beatpath.app.R.drawable.clay_star; else -> com.beatpath.app.R.drawable.clay_star_locked }
    Box(Modifier.size(86.dp), contentAlignment = Alignment.Center) {
        if (current) Box(Modifier.size(84.dp).graphicsLayer { val k = 0.94f + breathe * 0.08f; scaleX = k; scaleY = k; alpha = 0.55f + breathe * 0.35f }
            .border(3.dp, Color(0xCCFFFFFF), CircleShape))
        Box(Modifier.size(70.dp)
            .graphicsLayer { val k = press * pop.value * (if (current) 1f + breathe * 0.03f else 1f); scaleX = k; scaleY = k; translationY = if (current) -breathe * 2.dp.toPx() else 0f }
            .drawBehind { drawOval(Color(0x30083A5A), Offset(size.width * 0.1f, size.height * 0.86f), Size(size.width * 0.8f, size.height * 0.18f)) }
            .clip(CircleShape).clickable(src, null, onClick = onClick)
            .semantics { contentDescription = "${node.title}${if (done) ", completed" else if (!unlocked) ", locked" else ""}" }) {
            androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(img), null, Modifier.fillMaxSize())
        }
        if (gold) Box(Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp).size(26.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }
            .background(Brush.verticalGradient(listOf(Color(0xFFFFD86B), Color(0xFFE9A21C))), CircleShape).border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Star, "Solo", tint = Color.White, modifier = Modifier.size(16.dp)) }
        if (!unlocked) Box(Modifier.align(Alignment.BottomEnd).padding(bottom = 6.dp, end = 6.dp).size(24.dp).background(Color(0xFF8E9AA3), CircleShape).border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Lock, null, tint = Color.White, modifier = Modifier.size(14.dp)) }
    }
}

// ============================ Studio hub ============================
@Composable
fun StudioHub(store: Store, sfx: Sfx, onFree: () -> Unit, onStart: (String) -> Unit) {
    val lou = rememberLou(sfx)
    LaunchedEffect(Unit) { lou.say("Your studio. Play freely, or pick a lesson.", LouMood.Happy) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Studio", style = AeroType.Hero.copy(color = MenuInk, shadow = MenuGlow))
        Row(verticalAlignment = Alignment.Bottom) {
            Lou(Modifier.size(110.dp, 96.dp), mood = lou.mood, mouthOpen = lou.mouth)
            lou.bubble?.let { LouBubble(it, Modifier.weight(1f).padding(bottom = 12.dp)) }
        }
        GelButton("Free play — empty pattern", { sfx.select(); onFree() }, Modifier.fillMaxWidth(), gel = Gel.Grass)
        val next = store.nextLesson()
        if (next != null) {
            val title = store.titleOf(next)
            GelButton("Continue: $title", { sfx.select(); onStart(next) }, Modifier.fillMaxWidth(), gel = Gel.Aqua)
        } else GelButton("Daily practice", { sfx.select(); onStart(com.beatpath.app.lessons.Practice.MIX) }, Modifier.fillMaxWidth(), gel = Gel.Aqua)
        Text("ALL STUDIO LESSONS", style = AeroType.Caps.copy(color = MenuInk, shadow = MenuGlow))
        Curriculum.levels.forEach { lv ->
            val inLv = Curriculum.inLevel(lv.number)
            // folded by default; the level with your next lesson starts open
            com.beatpath.app.ui.aero.FoldCard("Level ${lv.number} · ${lv.name}", trailing = "${inLv.count { it.id in store.completed }}/${inLv.size}", startOpen = inLv.any { it.id == next }) {
              Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                inLv.forEach { l ->
                    val unlocked = store.isUnlocked(l.id)
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(enabled = unlocked) { sfx.select(); onStart(l.id) }.padding(vertical = 8.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(when { l.id in store.completed -> Icons.Rounded.Check; !unlocked -> Icons.Rounded.Lock; else -> Icons.Rounded.PlayArrow }, null,
                            tint = if (l.id in store.completed) AeroColors.Leaf else AeroColors.Ink2, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(l.title, style = AeroType.BodyStrong.copy(color = if (unlocked) AeroColors.Ink else AeroColors.Ink2), modifier = Modifier.weight(1f))
                        Text("${l.minutes} min", style = AeroType.Small)
                    }
                }
              }
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

// ============================ My loops ============================
@Composable
fun LoopsScreen(store: Store, engine: AudioEngine, sfx: Sfx, onOpen: (Project) -> Unit, onNew: () -> Unit) {
    var auditioning by remember { mutableStateOf<Long?>(null) }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("My loops", style = AeroType.Hero.copy(color = MenuInk, shadow = MenuGlow), modifier = Modifier.weight(1f))
                GelButton("+ New", { sfx.select(); onNew() }, Modifier.width(96.dp), gel = Gel.Grass, height = 40.dp)
            }
            if (store.projects.isEmpty()) {
                Column(Modifier.fillMaxWidth().glass(22.dp).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Lou(Modifier.size(140.dp, 120.dp), mood = LouMood.Sleepy)
                    Text("Nothing saved yet", style = AeroType.H2)
                    Text("Make something in Studio and tap the save icon — it lands here.", style = AeroType.Body, textAlign = TextAlign.Center)
                }
            }
            store.projects.forEach { p ->
                var menu by remember { mutableStateOf(false) }
                val on = auditioning == p.id
                Row(Modifier.fillMaxWidth().glass(20.dp).clickable { engine.stop(); auditioning = null; onOpen(p) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p.name, style = AeroType.H2, maxLines = 1)
                        Text("${p.pattern.bars} bar${if (p.pattern.bars > 1) "s" else ""} · ${p.bpm} BPM · ${p.pattern.channels.sumOf { it.notes.size }} notes", style = AeroType.Small)
                    }
                    GelOrb(Modifier.size(48.dp).clip(CircleShape).clickable {
                        if (on) { engine.stop(); auditioning = null } else { engine.mix = p.mix; engine.play(p.pattern, p.bpm); auditioning = p.id }
                    }.semantics { contentDescription = if (on) "Stop" else "Play ${p.name}" }, if (on) Gel.Orange else Gel.Aqua) {
                        Icon(if (on) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.White)
                    }
                    Box {
                        Icon(Icons.Rounded.MoreVert, "More actions", tint = AeroColors.Ink2, modifier = Modifier.size(44.dp).clip(CircleShape).clickable { menu = true }.padding(10.dp))
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem(text = { Text("Duplicate") }, onClick = { menu = false; store.saveProject(p.copy(id = System.currentTimeMillis(), name = p.name + " copy")) })
                            DropdownMenuItem(text = { Text("Delete") }, onClick = {
                                menu = false; if (on) { engine.stop(); auditioning = null }
                                val removed = store.deleteProject(p.id)
                                scope.launch { if (removed != null && snack.showSnackbar("Deleted ${p.name}", "Undo") == SnackbarResult.ActionPerformed) store.restoreProject(removed) }
                            })
                        }
                    }
                }
            }
            Text("Tip: open a loop → export → MIDI for FL Studio.", style = AeroType.Small.copy(color = MenuInk, shadow = MenuGlow))
        }
        SnackbarHost(snack, Modifier.align(Alignment.BottomCenter))
    }
}

// ============================ Me ============================
@Composable
fun MeScreen(store: Store, engine: AudioEngine, onShop: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Me", style = AeroType.Hero.copy(color = MenuInk, shadow = MenuGlow))
        Column(Modifier.fillMaxWidth().glass(22.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Lou(Modifier.size(84.dp, 74.dp), mood = LouMood.Happy)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Lou's Shop", style = AeroType.Title)
                    Text("Spend XP on hats, glasses, colors and skies.", style = AeroType.Body)
                }
            }
            LevelBar(store.xp, Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                XpPill(store.balance)
                Spacer(Modifier.width(10.dp))
                GelButton("Open the shop", onShop, Modifier.weight(1f), gel = Gel.Orange)
            }
        }
        Column(Modifier.fillMaxWidth().glass(22.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${store.streak()} day beat", style = AeroType.Title)
                    Text(if (store.streak() == 0) "Finish one lesson to light today." else "Your week is grooving.", style = AeroType.Body)
                }
                Lou(Modifier.size(80.dp, 70.dp), mood = if (store.streak() > 0) LouMood.Happy else LouMood.Sleepy)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val today = LocalDate.now()
                for (i in 6 downTo 0) {
                    val d = today.minusDays(i.toLong()); val lit = d.toString() in store.practiceDays
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        GelOrb(Modifier.size(34.dp), if (lit) (if (i == 0) Gel.Orange else Gel.Grass) else Gel.Off) {}
                        Text(d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), style = AeroType.Small)
                    }
                }
            }
            Text("⭐ ${store.xp} XP · ${store.completed.size} of ${store.order.size} lessons", style = AeroType.BodyStrong)
        }
        Text("LEVELS", style = AeroType.Caps.copy(color = MenuInk, shadow = MenuGlow))
        Column(Modifier.fillMaxWidth().glass(22.dp).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Curriculum.levels.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { lv ->
                        val ids = Curriculum.inLevel(lv.number).map { it.id }
                        val done = ids.all { it in store.completed }; val solo = ids.all { it in store.unassisted }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            GelOrb(Modifier.size(48.dp), if (solo) Gel.Orange else if (done) Gel.Grass else Gel.Off) { Text("${lv.number}", style = AeroType.H2.copy(color = Color.White)) }
                            Text(lv.name, style = AeroType.Small.copy(fontSize = 10.sp), textAlign = TextAlign.Center, maxLines = 2)
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        Text("SETTINGS", style = AeroType.Caps.copy(color = MenuInk, shadow = MenuGlow))
        Column(Modifier.fillMaxWidth().glass(22.dp).padding(14.dp)) {
            SettingRow("Sounds", store.sfxOn) { store.changeSfx(it); engine.sfxEnabled = it }
            SettingRow("Unlock all lessons", store.unlockAll) { store.changeUnlockAll(it) }
        }
        com.beatpath.app.ui.aero.FoldCard("Try it in FL Studio") {
            Text("Open FL Studio → Channel Rack (F6). Left-click steps 1, 5, 9 and 13 on the Kick, right-click one off, then press Space.", style = AeroType.Body)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SettingRow(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = AeroType.BodyStrong, modifier = Modifier.weight(1f)); Switch(on, onChange)
    }
}

@Suppress("unused") private val keepIcon = Icons.Rounded.GraphicEq
