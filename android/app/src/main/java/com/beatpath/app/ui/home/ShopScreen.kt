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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatpath.app.audio.Sfx
import com.beatpath.app.data.Shop
import com.beatpath.app.data.Store
import com.beatpath.app.ui.aero.AeroBackground
import com.beatpath.app.ui.aero.Gel
import com.beatpath.app.ui.aero.GelButton
import com.beatpath.app.ui.aero.GelOrb
import com.beatpath.app.ui.aero.LouBubble
import com.beatpath.app.ui.aero.glass
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
        Box(Modifier.fillMaxWidth().height(12.dp).drawBehind {
            val r = CornerRadius(size.height / 2)
            drawRoundRect(Color(0x55FFFFFF), cornerRadius = r)
            drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFFFFC86B), Color(0xFFF2852B))), size = Size(size.width * k.coerceAtLeast(0.03f), size.height), cornerRadius = r)
            drawRoundRect(Color(0x55FFFFFF), Offset(3f, 2f), Size((size.width * k - 6f).coerceAtLeast(0f), size.height * 0.35f), r)
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
    Row(modifier.clip(RoundedCornerShape(50)).background(Color(0xE6FFFFFF)).border(1.5.dp, Color(0xFFF2B35B), RoundedCornerShape(50)).padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        GelOrb(Modifier.size(26.dp), Gel.Orange) { Icon(Icons.Rounded.Star, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
        Spacer(Modifier.width(6.dp))
        Text("$balance", style = AeroType.H2.copy(color = Color(0xFF9A5200), fontSize = 17.sp))
    }
}

@Composable
fun ShopScreen(store: Store, sfx: Sfx, onBack: () -> Unit) {
    val lou = rememberLou(sfx)
    val scope = rememberCoroutineScope()
    var preview by remember { mutableStateOf<Shop.Item?>(null) }
    var confetti by remember { mutableIntStateOf(0) }
    var mysteryShake by remember { mutableStateOf(false) }
    BackHandler { onBack() }
    LaunchedEffect(Unit) {
        lou.say(if (store.balance >= 80) "You've got ${store.balance} XP to spend. Dress me up!" else "Finish lessons to earn XP, then spend it here.", LouMood.Happy)
    }
    val eq = store.equipped
    val outfit = LouOutfit(eq[Shop.Slot.HAT], eq[Shop.Slot.FACE], eq[Shop.Slot.SKIN]).let { o ->
        when (preview?.slot) {
            Shop.Slot.HAT -> o.copy(hat = preview!!.id); Shop.Slot.FACE -> o.copy(face = preview!!.id); Shop.Slot.SKIN -> o.copy(skin = preview!!.id); else -> o
        }
    }
    fun celebrate(item: Shop.Item, mystery: Boolean) {
        confetti++; sfx.complete(); lou.react(LouMood.Cheer)
        lou.say(if (mystery) "Mystery box: ${item.name}! ${item.blurb}" else "${item.name}! ${item.blurb}", LouMood.Cheer)
        preview = null
        scope.launch { delay(2200); lou.mood = LouMood.Happy }
    }
    val land = isLandscape()
    AeroBackground(photo = true) {
        Column(Modifier.fillMaxSize().sideSafe().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = AeroColors.Ink, modifier = Modifier.size(44.dp).clip(CircleShape).clickable { onBack() }.padding(10.dp))
                Text("Lou's Shop", style = AeroType.Title.copy(color = MenuInk, shadow = MenuGlow), modifier = Modifier.weight(1f).padding(start = 4.dp))
                XpPill(store.balance)
            }
            BoxWithConstraints(Modifier.weight(1f)) {
                val cols = if (maxWidth > 600.dp) 5 else 3
                val hero: @Composable (Modifier) -> Unit = { m ->
                    Column(m.glass(24.dp).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(contentAlignment = Alignment.Center) {
                            Lou(Modifier.size(170.dp, 150.dp), mood = lou.mood, mouthOpen = lou.mouth, outfit = outfit)
                        }
                        lou.bubble?.let { LouBubble(it, Modifier.fillMaxWidth().height(64.dp)) }
                        LevelBar(store.xp, Modifier.fillMaxWidth())
                        // mystery box: the gamble
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            val shake = remember { Animatable(0f) }
                            LaunchedEffect(mysteryShake) { if (mysteryShake) { repeat(6) { shake.animateTo(if (it % 2 == 0) 14f else -14f, tween(60)) }; shake.animateTo(0f, spring(0.4f, 600f)) } }
                            GelOrb(Modifier.size(52.dp).graphicsLayer { rotationZ = shake.value }, Gel.Aqua) { Text("?", style = AeroType.Title.copy(color = Color.White)) }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Mystery box", style = AeroType.H2.copy(fontSize = 16.sp))
                                Text("A random item you don't own", style = AeroType.Small)
                            }
                            val left = Shop.items.count { it.id !in store.owned }
                            GelButton(if (left == 0) "All owned" else "★ ${Shop.MYSTERY_PRICE}", {
                                scope.launch {
                                    mysteryShake = true; sfx.select(); delay(520); mysteryShake = false
                                    store.openMystery()?.let { celebrate(it, true) }
                                }
                            }, Modifier.width(104.dp), gel = Gel.Orange, enabled = left > 0 && store.balance >= Shop.MYSTERY_PRICE, height = 40.dp)
                        }
                    }
                }
                val shelves: @Composable () -> Unit = {
                    Shop.Slot.entries.forEach { slot ->
                        Text(slot.label.uppercase(), style = AeroType.Caps.copy(color = MenuInk, shadow = MenuGlow), modifier = Modifier.padding(top = 6.dp, start = 4.dp))
                        Shop.items.filter { it.slot == slot }.chunked(cols).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEach { item ->
                                    ItemCard(item, store, preview?.id == item.id, Modifier.weight(1f),
                                        onPreview = { sfx.tap(); preview = if (preview?.id == item.id) null else item; if (preview != null) lou.say("${item.name}: ${item.blurb}", LouMood.Happy) },
                                        onBuy = { if (store.buy(item)) celebrate(item, false) else { sfx.miss(); lou.say("You need ${item.price - store.balance} more XP. A lesson or two will do it!", LouMood.Encourage) } },
                                        onWear = { sfx.toggleOn(); if (store.equipped[slot] == item.id) store.unequip(slot) else store.equip(item) })
                                }
                                repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
                if (land) Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    hero(Modifier.weight(0.85f).verticalScroll(rememberScrollState()).navigationBarsPadding())
                    Column(Modifier.weight(1.15f).verticalScroll(rememberScrollState()).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) { shelves() }
                } else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    hero(Modifier.fillMaxWidth())
                    shelves()
                }
            }
        }
        Confetti(confetti)
    }
}

@Composable
private fun ItemCard(item: Shop.Item, store: Store, previewing: Boolean, modifier: Modifier, onPreview: () -> Unit, onBuy: () -> Unit, onWear: () -> Unit) {
    val owned = item.id in store.owned
    val wearing = store.equipped[item.slot] == item.id
    val lift by animateFloatAsState(if (previewing) 1f else 0f, spring(0.6f, 500f), label = "lift")
    Column(modifier.graphicsLayer { translationY = -lift * 4.dp.toPx(); scaleX = 1f + lift * 0.03f; scaleY = 1f + lift * 0.03f }
        .glass(18.dp).border(if (previewing || wearing) 2.5.dp else 0.dp, if (wearing) AeroColors.Leaf else AeroColors.Primary, RoundedCornerShape(18.dp))
        .clip(RoundedCornerShape(18.dp)).clickable(onClick = onPreview).padding(8.dp)
        .semantics { contentDescription = "${item.name}, ${if (owned) if (wearing) "wearing" else "owned" else "${item.price} XP"}" },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.fillMaxWidth().height(62.dp), contentAlignment = Alignment.Center) {
            if (item.slot == Shop.Slot.SKY) {
                Box(Modifier.size(70.dp, 54.dp).clip(RoundedCornerShape(12.dp)).background(Brush.verticalGradient(listOf(Color(0xFF8ED0F5), Color(0xFFCDEBFA))))) {
                    com.beatpath.app.ui.aero.SkyTint(item.id, Modifier.fillMaxSize())
                }
            } else {
                val o = when (item.slot) { Shop.Slot.HAT -> LouOutfit(hat = item.id); Shop.Slot.FACE -> LouOutfit(face = item.id); else -> LouOutfit(skin = item.id) }
                Lou(Modifier.size(72.dp, 62.dp), outfit = o, reduceMotion = true)
            }
        }
        Text(item.name, style = AeroType.Label, textAlign = TextAlign.Center, maxLines = 2, modifier = Modifier.height(34.dp))
        when {
            wearing -> GelButton("Wearing", onWear, Modifier.fillMaxWidth(), gel = Gel.Grass, height = 32.dp)
            owned -> GelButton(if (item.slot == Shop.Slot.SKY) "Use" else "Wear", onWear, Modifier.fillMaxWidth(), gel = Gel.Silver, height = 32.dp)
            else -> GelButton("★ ${item.price}", onBuy, Modifier.fillMaxWidth(), gel = if (store.balance >= item.price) Gel.Orange else Gel.Off, height = 32.dp)
        }
    }
}
