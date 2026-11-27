package com.beatpath.app.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatpath.app.audio.Sfx
import com.beatpath.app.data.Shop
import com.beatpath.app.data.Store
import com.beatpath.app.ui.aero.AeroBackground
import com.beatpath.app.ui.aero.ClayChip
import com.beatpath.app.ui.aero.ClayIconButton
import com.beatpath.app.ui.aero.Gel
import com.beatpath.app.ui.aero.GelButton
import com.beatpath.app.ui.aero.GelOrb
import com.beatpath.app.ui.aero.LouBubble
import com.beatpath.app.ui.aero.clayCard
import com.beatpath.app.ui.aero.clayPress
import com.beatpath.app.ui.aero.claySurface
import com.beatpath.app.ui.aero.isLandscape
import com.beatpath.app.ui.aero.sideSafe
import com.beatpath.app.ui.lou.Lou
import com.beatpath.app.ui.lou.LouMood
import com.beatpath.app.ui.lou.LouOutfit
import com.beatpath.app.ui.lou.rememberLou
import com.beatpath.app.ui.theme.AeroColors
import com.beatpath.app.ui.theme.AeroType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Level chip + bar: level and title from lifetime XP. */
@Composable
fun LevelBar(totalXp: Int, modifier: Modifier = Modifier) {
    val lv = Shop.level(totalXp); val (into, need) = Shop.levelProgress(totalXp)
    val k by animateFloatAsState(into / need.toFloat(), tween(700, easing = FastOutSlowInEasing), label = "lv")
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("LEVEL $lv · ${Shop.title(lv).uppercase()}", style = AeroType.Caps, modifier = Modifier.weight(1f))
            Text("$into / $need XP", style = AeroType.Small)
        }
        Box(Modifier.fillMaxWidth().height(14.dp).claySurface(Gel.Off, 7.dp, depth = 2.dp).drawBehind {
            val r = CornerRadius((size.height - 2.dp.toPx()) / 2)
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFFFC86B), Color(0xFFF2852B))), size = Size(size.width * k.coerceAtLeast(0.04f), size.height - 2.dp.toPx()), cornerRadius = r)
            drawRoundRect(Color(0x55FFFFFF), Offset(4f, 2f), Size((size.width * k - 8f).coerceAtLeast(0f), (size.height - 2.dp.toPx()) * 0.35f), r)
        }.semantics { contentDescription = "Level $lv, $into of $need XP to the next level" })
    }
}

/** Confetti burst that plays once each time [trigger] changes (not on first show). */
@Composable
fun Confetti(trigger: Int, modifier: Modifier = Modifier) {
    val p = remember { Animatable(1f) }
    LaunchedEffect(trigger) { if (trigger > 0) { p.snapTo(0f); p.animateTo(1f, tween(1700, easing = androidx.compose.animation.core.LinearEasing)) } }
    if (p.value >= 1f) return
    Canvas(modifier.fillMaxSize()) {
        val rnd = java.util.Random(trigger.toLong())
        val colors = listOf(0, 2, 4, 5, 7, 9, 11).map { com.beatpath.app.model.NoteColors.of(it) }
        repeat(70) {
            val ang = rnd.nextFloat() * 2f * PI.toFloat(); val speed = 0.35f + rnd.nextFloat() * 0.65f
            val t = p.value
            val x = size.width / 2 + cos(ang) * speed * size.width * 0.6f * t
            val y = size.height * 0.32f + sin(ang) * speed * size.height * 0.3f * t + size.height * 0.55f * t * t
            val spin = t * 720f * (if (it % 2 == 0) 1 else -1)
            val c = colors[it % colors.size].copy(alpha = (1f - t).coerceIn(0f, 1f))
            val w = 6f + rnd.nextFloat() * 6f
            rotate(spin + it * 17f, Offset(x, y)) { drawRoundRect(c, Offset(x - w / 2, y - w / 4), Size(w, w / 2), CornerRadius(2f)) }
        }
    }
}

/** The XP balance pill: a little clay coin and the number. */
@Composable
fun XpPill(balance: Int, modifier: Modifier = Modifier) {
    Row(modifier.claySurface(Gel.Cream, 18.dp, depth = 3.dp).padding(start = 4.dp, end = 12.dp, top = 3.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        GelOrb(Modifier.size(26.dp), Gel.Orange) { Icon(Icons.Rounded.Star, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
        Spacer(Modifier.width(6.dp))
        Text("$balance", style = AeroType.H2.copy(color = Color(0xFF9A5200), fontSize = 17.sp))
    }
}

fun Shop.Rarity.gel() = when (this) { Shop.Rarity.COMMON -> Gel.Cream; Shop.Rarity.RARE -> Gel.Aqua; Shop.Rarity.EPIC -> Gel.Purple; Shop.Rarity.LEGENDARY -> Gel.Gold }

private const val MYSTERY_TAB = 4

@Composable
fun ShopScreen(store: Store, sfx: Sfx, onBack: () -> Unit) {
    val lou = rememberLou(sfx)
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<Shop.Item?>(null) }
    var confetti by remember { mutableIntStateOf(0) }
    var boxState by remember { mutableIntStateOf(0) }       // 0 closed, 1 shaking, 2 open
    var revealed by remember { mutableStateOf<Shop.Item?>(null) }
    BackHandler { onBack() }
    LaunchedEffect(Unit) {
        lou.say(if (store.balance >= 80) "You have ${store.balance} XP. Pick something for me to try on!" else "Finish lessons and games to earn XP. Then dress me up!", LouMood.Happy)
    }
    val eq = store.equipped
    val worn = LouOutfit(eq[Shop.Slot.HAT], eq[Shop.Slot.FACE], eq[Shop.Slot.SKIN], eq[Shop.Slot.NECK])
    val outfit = selected?.let { s ->
        when (s.slot) { Shop.Slot.HAT -> worn.copy(hat = s.id); Shop.Slot.FACE -> worn.copy(face = s.id); Shop.Slot.SKIN -> worn.copy(skin = s.id); Shop.Slot.NECK -> worn.copy(neck = s.id) }
    } ?: worn
    fun celebrate(item: Shop.Item, mystery: Boolean) {
        confetti++; sfx.complete(); lou.react(LouMood.Cheer)
        lou.say(if (mystery) "Surprise! ${item.name}! ${item.blurb}" else "${item.name}! ${item.blurb}", LouMood.Cheer)
        scope.launch { delay(2200); lou.mood = LouMood.Happy }
    }
    val land = isLandscape()

    val stage: @Composable (Modifier) -> Unit = { m ->
        Column(m.clayCard(28.dp).drawBehind {
            // a soft spotlight from above onto the pedestal
            drawRect(Brush.verticalGradient(listOf(Color(0xFFE8F4FC), Color(0xFFFFFFFF))))
            drawOval(Brush.radialGradient(listOf(Color(0x66FFF2C2), Color(0x00FFF2C2)), Offset(size.width / 2, size.height * 0.35f), size.width * 0.5f),
                Offset(size.width * 0.1f, 0f), Size(size.width * 0.8f, size.height * 0.75f))
        }.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.fillMaxWidth().height(if (land) 150.dp else 196.dp), contentAlignment = Alignment.BottomCenter) {
                // clay pedestal
                Box(Modifier.width(190.dp).height(34.dp).claySurface(Gel.Off, 17.dp, depth = 6.dp))
                Lou(Modifier.padding(bottom = 18.dp).size(if (land) 150.dp else 190.dp, if (land) 130.dp else 166.dp), mood = lou.mood, mouthOpen = lou.mouth, outfit = outfit)
            }
            lou.bubble?.let { LouBubble(it, Modifier.fillMaxWidth().height(84.dp).verticalScroll(rememberScrollState())) }
            AnimatedContent(selected, transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) }, label = "sel") { s ->
                if (s == null) LevelBar(store.xp, Modifier.fillMaxWidth())
                else Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.name, style = AeroType.H2, modifier = Modifier.weight(1f))
                        Box(Modifier.claySurface(s.rarity.gel(), 10.dp, depth = 2.dp).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text(s.rarity.label.uppercase(), style = AeroType.Caps.copy(color = s.rarity.gel().text, fontSize = 10.sp))
                        }
                    }
                    val owned = s.id in store.owned; val wearing = store.equipped[s.slot] == s.id
                    when {
                        wearing -> GelButton("Take it off", { sfx.toggleOff(); store.unequip(s.slot); selected = null }, Modifier.fillMaxWidth(), gel = Gel.Silver)
                        owned -> GelButton("Wear it", { sfx.toggleOn(); store.equip(s); lou.react(LouMood.Happy) }, Modifier.fillMaxWidth(), gel = Gel.Grass)
                        store.balance >= s.price -> GelButton("Buy for ★ ${s.price}", { if (store.buy(s)) celebrate(s, false) }, Modifier.fillMaxWidth(), gel = Gel.Orange)
                        else -> GelButton("Need ${s.price - store.balance} more XP", { sfx.miss(); lou.say("Play a mini game or finish a lesson to earn ${s.price - store.balance} more XP!", LouMood.Encourage) },
                            Modifier.fillMaxWidth(), gel = Gel.Off)
                    }
                }
            }
        }
    }
    val tabs: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (Shop.Slot.entries.map { it.label } + "Mystery box").forEachIndexed { i, label ->
                ClayChip(label, { sfx.tap(); tab = i; selected = null }, gel = if (tab == i) Gel.Aqua else Gel.Cream, fontSize = 14,
                    description = "$label${if (tab == i) ", selected" else ""}")
            }
        }
    }
    val shelf: @Composable (Int) -> Unit = { cols ->
        if (tab == MYSTERY_TAB) MysteryBox(store, sfx, boxState, revealed) {
            if (boxState != 0) return@MysteryBox
            boxState = 1   // set before launching, so a double tap can't open (and pay for) two boxes
            scope.launch {
                sfx.select(); delay(900)
                val got = store.openMystery()
                if (got != null) { revealed = got; boxState = 2; celebrate(got, true) } else boxState = 0
            }
        }.also { if (boxState == 2) LaunchedEffect(revealed) { delay(3500); boxState = 0; revealed = null } }
        else Shop.items.filter { it.slot == Shop.Slot.entries[tab] }.chunked(cols).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                row.forEach { item ->
                    ItemTile(item, store, selected?.id == item.id, Modifier.weight(1f)) {
                        sfx.tap(); selected = if (selected?.id == item.id) null else item
                        if (selected != null) lou.say(item.blurb, LouMood.Happy)
                    }
                }
                repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }

    AeroBackground(photo = true) {
        Column(Modifier.fillMaxSize().sideSafe().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                ClayIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
                Text("Lou's Shop", style = AeroType.Title.copy(color = MenuInk, shadow = MenuGlow), modifier = Modifier.weight(1f).padding(start = 10.dp))
                XpPill(store.balance)
            }
            BoxWithConstraints(Modifier.weight(1f)) {
                val cols = if (maxWidth > 600.dp) 4 else 3
                if (land) Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    stage(Modifier.weight(0.9f).verticalScroll(rememberScrollState()).navigationBarsPadding())
                    Column(Modifier.weight(1.1f).verticalScroll(rememberScrollState()).navigationBarsPadding()) { tabs(); shelf(cols) }
                } else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    stage(Modifier.fillMaxWidth())
                    tabs()
                    Column { shelf(cols) }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
        Confetti(confetti)
    }
}

@Composable
private fun ItemTile(item: Shop.Item, store: Store, chosen: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val owned = item.id in store.owned
    val wearing = store.equipped[item.slot] == item.id
    val src = remember { MutableInteractionSource() }
    val p = clayPress(src)
    val lift by animateFloatAsState(if (chosen) 1f else 0f, spring(0.55f, 500f), label = "lift")
    Column(modifier.graphicsLayer { translationY = -lift * 5.dp.toPx(); scaleX = 1f + lift * 0.04f; scaleY = 1f + lift * 0.04f }
        .claySurface(Gel.Cream, 18.dp, depth = 5.dp, press = p, outline = if (chosen) AeroColors.Primary else item.rarity.gel().edge)
        .clip(RoundedCornerShape(18.dp)).clickable(src, null, role = Role.Button, onClick = onClick)
        .semantics { contentDescription = "${item.name}, ${item.rarity.label}, ${if (owned) if (wearing) "wearing" else "owned" else "${item.price} XP"}" }
        .padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val o = when (item.slot) { Shop.Slot.HAT -> LouOutfit(hat = item.id); Shop.Slot.FACE -> LouOutfit(face = item.id); Shop.Slot.NECK -> LouOutfit(neck = item.id); else -> LouOutfit(skin = item.id) }
        Lou(Modifier.size(84.dp, 74.dp), outfit = o, reduceMotion = true)
        Text(item.name, style = AeroType.Label, textAlign = TextAlign.Center, maxLines = 2, modifier = Modifier.height(34.dp))
        when {
            wearing -> Text("✓ Wearing", style = AeroType.Label.copy(color = AeroColors.Leaf))
            owned -> Text("Owned", style = AeroType.Label.copy(color = AeroColors.Ink2))
            else -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Star, null, tint = if (store.balance >= item.price) Color(0xFFF2852B) else Color(0xFFAEB9C1), modifier = Modifier.size(16.dp))
                Text(" ${item.price}", style = AeroType.Label.copy(color = if (store.balance >= item.price) Color(0xFF9A5200) else AeroColors.Ink2))
            }
        }
    }
}

/** A clay gift box: it shakes, the lid pops off, and the prize floats up. */
@Composable
private fun MysteryBox(store: Store, sfx: Sfx, state: Int, revealed: Shop.Item?, onOpen: () -> Unit) {
    val shake = remember { Animatable(0f) }
    val lid = remember { Animatable(0f) }
    LaunchedEffect(state) {
        when (state) {
            1 -> repeat(8) { shake.animateTo(if (it % 2 == 0) 1f else -1f, tween(55)) }.also { shake.animateTo(0f, spring(0.4f, 600f)) }
            2 -> lid.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 180f))
            else -> lid.snapTo(0f)
        }
    }
    val left = Shop.items.count { it.id !in store.owned }
    Column(Modifier.fillMaxWidth().clayCard(24.dp).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(200.dp, 170.dp), contentAlignment = Alignment.BottomCenter) {
            if (revealed != null) {
                val o = when (revealed.slot) { Shop.Slot.HAT -> LouOutfit(hat = revealed.id); Shop.Slot.FACE -> LouOutfit(face = revealed.id); Shop.Slot.NECK -> LouOutfit(neck = revealed.id); else -> LouOutfit(skin = revealed.id) }
                Lou(Modifier.padding(bottom = (40 + 60 * lid.value).dp).size(96.dp, 84.dp).graphicsLayer { alpha = lid.value; scaleX = 0.5f + lid.value * 0.5f; scaleY = 0.5f + lid.value * 0.5f },
                    outfit = o, mood = LouMood.Cheer)
            }
            Canvas(Modifier.size(150.dp, 120.dp).graphicsLayer { rotationZ = shake.value * 6f }) {
                val w = size.width; val h = size.height
                // box body
                drawRoundRect(Color(0xFF1A6FAE), Offset(w * 0.08f, h * 0.38f), Size(w * 0.84f, h * 0.62f), CornerRadius(16f))
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF6CC4F2), Color(0xFF2E9BE0)), h * 0.34f, h), Offset(w * 0.08f, h * 0.34f), Size(w * 0.84f, h * 0.6f), CornerRadius(16f))
                drawRect(Brush.verticalGradient(listOf(Color(0xFFFF9EC4), Color(0xFFEF5B9C))), Offset(w * 0.44f, h * 0.34f), Size(w * 0.12f, h * 0.6f))
                // lid lifts and tilts away
                val ly = -lid.value * h * 0.9f
                rotate(lid.value * 28f, Offset(w * 0.9f, h * 0.3f + ly)) {
                    drawRoundRect(Color(0xFF1A6FAE), Offset(w * 0.02f, h * 0.24f + ly), Size(w * 0.96f, h * 0.18f), CornerRadius(12f))
                    drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF8ED4F7), Color(0xFF3FA9E8)), h * 0.2f + ly, h * 0.38f + ly), Offset(w * 0.02f, h * 0.2f + ly), Size(w * 0.96f, h * 0.18f), CornerRadius(12f))
                    drawRect(Color(0xFFEF5B9C), Offset(w * 0.44f, h * 0.2f + ly), Size(w * 0.12f, h * 0.18f))
                    drawOval(Color(0xFFFF9EC4), Offset(w * 0.28f, h * 0.06f + ly), Size(w * 0.22f, h * 0.16f))
                    drawOval(Color(0xFFFF9EC4), Offset(w * 0.5f, h * 0.06f + ly), Size(w * 0.22f, h * 0.16f))
                }
            }
        }
        Text(if (revealed != null) "You got: ${revealed.name}!" else "A surprise item for Lou", style = AeroType.H2, textAlign = TextAlign.Center)
        Text(if (left == 0) "You own everything. Wow!" else "Something you don't have yet. Rare things are rarer!", style = AeroType.Body, textAlign = TextAlign.Center)
        GelButton(if (left == 0) "All owned" else "Open for ★ ${Shop.MYSTERY_PRICE}", onOpen, Modifier.fillMaxWidth(), gel = Gel.Orange,
            enabled = left > 0 && store.balance >= Shop.MYSTERY_PRICE && state == 0)
    }
}
