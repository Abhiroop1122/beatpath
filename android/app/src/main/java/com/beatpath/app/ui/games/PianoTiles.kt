package com.beatpath.app.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatpath.app.audio.AudioEngine
import com.beatpath.app.audio.Sfx
import com.beatpath.app.lessons.Basics
import com.beatpath.app.model.ChannelKind
import com.beatpath.app.model.Note
import com.beatpath.app.model.NoteColors
import com.beatpath.app.model.Pattern
import com.beatpath.app.model.TICKS_PER_BAR
import com.beatpath.app.model.TICKS_PER_BEAT
import com.beatpath.app.ui.aero.LouBubble
import com.beatpath.app.ui.aero.isLandscape
import com.beatpath.app.ui.lesson.RainbowKeys
import com.beatpath.app.ui.lou.LouMood
import com.beatpath.app.ui.theme.OpenSans
import kotlin.math.abs

/** A song for Piano Tiles: melody notes (pitch, beat, length in beats) and a chord every half bar. */
private class Song(val title: String, val bpm: Int, val melody: List<Triple<Int, Float, Float>>, val halfBarChords: List<String>)

private const val C = 60; private const val D = 62; private const val E = 64; private const val F = 65; private const val G = 67; private const val A = 69

/** Quarter notes in a row; a Pair with 2f is a half note, 1.5f a dotted quarter, 0.5f an eighth. */
private fun tune(vararg n: Any): List<Triple<Int, Float, Float>> {
    var beat = 0f
    return n.map { x -> val (p, len) = if (x is Pair<*, *>) (x.first as Int) to (x.second as Float) else (x as Int) to 1f; Triple(p, beat, len).also { beat += len } }
}

private val SONGS = listOf(
    Song("Twinkle Twinkle", 80, tune(C, C, G, G, A, A, G to 2f, F, F, E, E, D, D, C to 2f, G, G, F, F, E, E, D to 2f, G, G, F, F, E, E, D to 2f,
        C, C, G, G, A, A, G to 2f, F, F, E, E, D, D, C to 2f),
        listOf("C", "C", "F", "C", "F", "C", "G", "C", "C", "F", "C", "G", "C", "F", "C", "G", "C", "C", "F", "C", "F", "C", "G", "C")),
    Song("Ode to Joy", 94, tune(E, E, F, G, G, F, E, D, C, C, D, E, E to 1.5f, D to 0.5f, D to 2f, E, E, F, G, G, F, E, D, C, C, D, E, D to 1.5f, C to 0.5f, C to 2f),
        listOf("C", "C", "G", "G", "C", "C", "C", "G", "C", "C", "G", "G", "C", "C", "G", "C")),
    Song("Mary Had a Little Lamb", 100, tune(E, D, C, D, E, E, E to 2f, D, D, D to 2f, E, G, G to 2f, E, D, C, D, E, E, E, E, D, D, E, D, C to 4f),
        listOf("C", "C", "C", "C", "G", "G", "C", "C", "C", "C", "C", "C", "G", "G", "C", "C")),
)

private const val COUNT_IN = 4f   // one bar of clicks before the melody

private class Tile(val pitch: Int, val beat: Float, val len: Float) { var state = 0; var at = 0L; var grade = 0 }   // 0 waiting, 1 hit, 2 missed

/** The backing band for a song (count-in clicks, soft drums, bass and chords), starting at bar [fromBar]. */
private fun backing(song: Song, fromBar: Int): Pattern {
    val songBars = (song.halfBarChords.size + 1) / 2
    val bars = 1 + songBars + 1
    val chord = mapOf("C" to listOf(48, 52, 55), "F" to listOf(48, 53, 57), "G" to listOf(47, 50, 55))
    val root = mapOf("C" to 36, "F" to 41, "G" to 43)
    val all = mutableMapOf<ChannelKind, MutableList<Note>>()
    fun add(k: ChannelKind, pitch: Int, tick: Int, len: Int, vel: Int) { all.getOrPut(k) { mutableListOf() } += Note(Pattern.nextId(), pitch, tick, len, vel) }
    for (b in 0 until bars) for (beat in 0 until 4) {
        val t = b * TICKS_PER_BAR + beat * TICKS_PER_BEAT
        if (b == 0) add(ChannelKind.HAT, ChannelKind.HAT.drumPitch, t, 120, if (beat == 0) 120 else 90)   // count-in: 1, 2, 3, 4
        else if (b <= songBars) {
            if (beat == 0 || beat == 2) add(ChannelKind.KICK, ChannelKind.KICK.drumPitch, t, 120, 70)
            add(ChannelKind.HAT, ChannelKind.HAT.drumPitch, t, 120, 45)
        }
    }
    song.halfBarChords.forEachIndexed { i, name ->
        val t = TICKS_PER_BAR + i * TICKS_PER_BAR / 2
        chord[name]!!.forEach { add(ChannelKind.CHORDS, it, t, TICKS_PER_BAR / 2 - 20, 38) }
        add(ChannelKind.BASS, root[name]!!, t, TICKS_PER_BAR / 2 - 20, 60)
    }
    val from = fromBar.coerceIn(0, bars - 1)
    var p = Pattern(bars - from)
    all.forEach { (k, ns) -> p = p.setNotes(k, ns.filter { it.startTick >= from * TICKS_PER_BAR }.map { it.copy(startTick = it.startTick - from * TICKS_PER_BAR) }) }
    return p
}

/**
 * Piano Tiles: rainbow tiles fall down the keyboard's lanes, timed to a real song played by a soft backing band.
 * Touch a key as its tile reaches the line and you play the melody: long tiles are long notes, like the Piano roll
 * standing up. Lou sits on top of the piano, drops the tiles, watches the next one, and dances to the beat.
 * Timing runs on the audio engine's own clock, so the tiles always match what you hear.
 */
@Composable
fun BoxScope.PianoTiles(control: GameControl, engine: AudioEngine, sfx: Sfx) {
    val clock = remember { AudioClock(engine) }
    var songIdx by remember { mutableIntStateOf(0) }
    val song = SONGS[songIdx]
    fun tilesFor(i: Int) = SONGS[i].melody.map { (p, b, l) -> Tile(p, b + COUNT_IN, l) }
    var tiles by remember { mutableStateOf(tilesFor(0)) }
    var beat by remember { mutableFloatStateOf(-1f) }
    var now by remember { mutableLongStateOf(0L) }
    var score by remember { mutableIntStateOf(0) }
    var hearts by remember { mutableIntStateOf(5) }
    var combo by remember { mutableIntStateOf(0) }
    var mood by remember { mutableStateOf(LouMood.Happy) }
    var moodUntil by remember { mutableLongStateOf(0L) }
    var say by remember { mutableStateOf("It's the Piano roll standing up! Play each note as it lands.") }
    var floaters by remember { mutableStateOf(listOf<Floater>()) }
    var bursts by remember { mutableStateOf(listOf<Burst>()) }
    var sounding by remember { mutableStateOf(mapOf<Int, Long>()) }
    var wrongKey by remember { mutableStateOf<Int?>(null) }
    var betweenSongs by remember { mutableLongStateOf(-1L) }
    var pausedAt by remember { mutableFloatStateOf(-1f) }
    var over by remember { mutableStateOf(false) }
    var geo by remember { mutableStateOf(Triple(1f, 1f, 1f)) }
    var lastBar by remember { mutableIntStateOf(-99) }
    val pressRef = remember { mutableStateOf<(Int) -> Unit>({}) }   // lane left, lane width, hit-line y (px, in the tile area)
    val first = remember { mutableStateOf(true) }
    DisposableEffect(Unit) { onDispose { clock.stop() } }

    fun react(m: LouMood, ms: Long) { mood = m; moodUntil = now + ms }
    fun laneOf(p: Int) = listOf(C, D, E, F, G, A, 71).indexOf(p)

    LaunchedEffect(songIdx) {
        if (!first.value) { kotlinx.coroutines.delay(600) }
        first.value = false
        tiles = tilesFor(songIdx); beat = -1f; lastBar = -99
        clock.start(backing(SONGS[songIdx], 0), SONGS[songIdx].bpm, 0f)
        say = when (songIdx) { 0 -> "Practice song! Play each note as its tile lands. Misses don't count yet."; 1 -> "Ode to Joy! Letters are gone from the keys now."; else -> "Last song. Colors only: you know these keys!" }
    }
    // a song ended: Lou cheers for a moment, then the next song (or the results)
    LaunchedEffect(betweenSongs) {
        if (betweenSongs < 0) return@LaunchedEffect
        kotlinx.coroutines.delay(1500)
        while (control.paused) kotlinx.coroutines.delay(100)
        if (songIdx + 1 >= SONGS.size) { over = true; control.end(score) } else { songIdx++; betweenSongs = -1L }
    }
    // pause: stop the band; resume one bar before where we were, with that bar as a count-in
    LaunchedEffect(control.paused) {
        if (control.paused) { pausedAt = beat; clock.stop() }
        else if (pausedAt >= 0) {
            val bar = ((pausedAt / 4).toInt() - 1).coerceAtLeast(0)
            if (betweenSongs < 0) clock.start(backing(SONGS[songIdx], bar), SONGS[songIdx].bpm, bar * 4f); pausedAt = -1f
        }
    }
    // every frame: read the audio clock and judge tiles that slipped past
    fun frame() {
        val b = clock.beat() ?: return
        beat = b
        if (now > moodUntil) mood = LouMood.Dance
        // each new bar: Lou reads out its notes, so you learn to read the tune as you play it
        val barNow = kotlin.math.floor((b - COUNT_IN) / 4f).toInt()
        if (barNow != lastBar && b >= COUNT_IN - 1f) {
            lastBar = barNow
            val inBar = tiles.filter { kotlin.math.floor((it.beat - COUNT_IN) / 4f).toInt() == barNow.coerceAtLeast(0) }
            if (inBar.isNotEmpty()) say = "Bar ${barNow.coerceAtLeast(0) + 1}: " + inBar.joinToString("  ") { Basics.name(it.pitch) + if (it.len >= 2f) "—" else "" }
        }
        val ms = clock.msPerBeat
        tiles.filter { it.state == 0 && (b - it.beat) * ms > 200 }.forEach { tl ->
            // the first song is practice: a miss shows the right key but costs no heart
            tl.state = 2; tl.at = now; combo = 0; if (songIdx > 0) hearts--; sfx.miss(); react(LouMood.Sad, 700)
            floaters = floaters + Floater("Miss", Color(0xFFEF5B6E), Offset(geo.first + (laneOf(tl.pitch) + 0.5f) * geo.second, geo.third - 40f), now)
            say = "That was ${Basics.name(tl.pitch)}. Watch for its color!"
            if (hearts <= 0 && !over) { over = true; clock.stop(); control.end(score) }
        }
        val last = tiles.maxOf { it.beat + it.len }
        if (b > last + 1.5f && betweenSongs < 0 && !over) {
            betweenSongs = now; clock.stop(); sfx.complete(); react(LouMood.Cheer, 1600)
            floaters = floaters + Floater("Song complete!", Color(0xFFF2852B), Offset(geo.first + geo.second * 3.5f, geo.third * 0.45f), now, big = true)
        }
        if (floaters.isNotEmpty()) floaters = floaters.filter { now - it.born < 950 }
        if (bursts.isNotEmpty()) bursts = bursts.filter { now - it.born < 650 }
        if (sounding.isNotEmpty()) sounding = sounding.filterValues { it > now }
    }
    LaunchedEffect(Unit) { gameLoop(control) { t, _ ->
        now = t; frame()
        if (GameDebug.autoplay) tiles.firstOrNull { it.state == 0 && beat >= it.beat - 0.02f }?.let { pressRef.value(it.pitch) }
    } }

    fun press(k: Int) {
        if (control.paused || over) return
        val b = clock.beat() ?: beat
        val ms = clock.msPerBeat
        val tl = tiles.filter { it.state == 0 && it.pitch == k && abs(b - it.beat) * ms <= 210 }.minByOrNull { abs(b - it.beat) }
        // a hit plays the note for as long as its tile, so long tiles sound long
        val holdSec = tl?.let { it.len * ms / 1000f * 0.92f } ?: 0.35f
        engine.playNote(k, holdSec.coerceAtMost(2.5f), 0.8f, AudioEngine.Timbre.KEYS)
        sounding = sounding + (k to now + (holdSec * 1000).toLong().coerceAtLeast(160))
        val x = geo.first + (laneOf(k) + 0.5f) * geo.second
        if (tl != null) {
            val off = abs(b - tl.beat) * ms
            tl.state = 1; tl.at = now; tl.grade = if (off <= 90) 2 else 1
            score += tl.grade; combo++
            bursts = bursts + Burst(Offset(x, geo.third), NoteColors.of(k), now)
            floaters = floaters + Floater(if (tl.grade == 2) "Perfect" else "Good", if (tl.grade == 2) Color(0xFFF2852B) else Color(0xFF2E9BE0), Offset(x, geo.third - 70f), now)
            if (combo > 0 && combo % 10 == 0) { react(LouMood.Cheer, 900); floaters = floaters + Floater("$combo in a row!", Color(0xFF62B826), Offset(geo.first + geo.second * 3.5f, geo.third * 0.35f), now, big = true) }
        } else if (tiles.any { it.state == 0 && abs(b - it.beat) * ms <= 210 }) {
            // the right moment, the wrong key: show which key was wanted
            wrongKey = k; combo = 0; react(LouMood.Surprised, 450)
            val want = tiles.filter { it.state == 0 }.minByOrNull { abs(b - it.beat) }!!
            say = "That's ${Basics.name(k)}. The tile is ${Basics.name(want.pitch)}: the ${NoteColors.name(want.pitch)} key."
        }
    }
    pressRef.value = { press(it) }
    LaunchedEffect(wrongKey) { if (wrongKey != null) { kotlinx.coroutines.delay(320); wrongKey = null } }

    val land = isLandscape()
    val measurer = rememberTextMeasurer()
    // Lou on top of the piano (portrait) or beside it (landscape), looking at the next tile and dancing to the beat
    val louSpot: @Composable (Modifier, Boolean) -> Unit = { m, side ->
        BoxWithConstraints(m) {
            val wPx = constraints.maxWidth.toFloat(); val hPx = constraints.maxHeight.toFloat()
            val next = tiles.filter { it.state == 0 }.minByOrNull { it.beat }
            val louX = if (side) wPx * 0.5f else wPx * 0.84f
            val gazeX = next?.let { ((geo.first + (laneOf(it.pitch) + 0.5f) * geo.second - (if (side) wPx * 1.5f else louX)) / (wPx * 0.5f)).coerceIn(-1f, 1f) } ?: 0f
            val bob = if (beat >= 0) (1f - (beat % 1f)).let { it * it } else 0f
            val louFoot = if (side) hPx * 0.62f else hPx
            LouActor(louX, louFoot - bob * 6f, if (side) 84.dp else 92.dp, mood, squash = bob * 0.06f, look = Offset(if (side) 1f else gazeX, 0.9f), beatPhase = if (beat >= 0) beat % 1f else null)
            LouBubble(say, if (side) Modifier.align(Alignment.BottomCenter).padding(horizontal = 8.dp, vertical = 8.dp)
                           else Modifier.align(Alignment.CenterStart).padding(start = 14.dp, end = 110.dp))
        }
    }
    val lanesAndKeys: @Composable (Modifier) -> Unit = { m ->
        Column(m) {
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 10.dp)) {
                val w = constraints.maxWidth.toFloat(); val h = constraints.maxHeight.toFloat()
                val lanes = 7; val laneW = w / lanes
                geo = Triple(0f, laneW, h)
                val pxPerBeat = h / 3.4f
                val showLetters = songIdx == 0 || songIdx == 1
                Canvas(Modifier.fillMaxSize().clipToBounds().semantics { contentDescription = "Falling tiles. Next: ${tiles.firstOrNull { it.state == 0 }?.let { Basics.name(it.pitch) } ?: "none"}" }) {
                    // lanes: a faint wash of each key's color
                    for (i in 0 until lanes) drawRect(NoteColors.of(listOf(C, D, E, F, G, A, 71)[i]).copy(alpha = 0.07f + (i % 2) * 0.03f), Offset(i * laneW, 0f), Size(laneW, h))
                    // beat lines scroll down with the music: the Piano roll's grid, standing up
                    if (beat >= 0) {
                        var gb = kotlin.math.floor(beat).toInt()
                        while (true) {
                            val y = h - (gb - beat) * pxPerBeat; if (y < -10) break
                            if (y <= h) drawLine(Color.White.copy(alpha = if (gb % 4 == 0) 0.55f else 0.22f), Offset(0f, y), Offset(w, y), if (gb % 4 == 0) 2.5f else 1.2f)
                            gb++
                        }
                    }
                    // tiles
                    tiles.forEach { tl ->
                        if (beat < 0) return@forEach
                        val lane = laneOf(tl.pitch); val c = NoteColors.of(tl.pitch)
                        val bottom = h - (tl.beat - beat) * pxPerBeat
                        val top = bottom - tl.len * pxPerBeat + 6f
                        if (top > h + 40 || bottom < -10) return@forEach
                        val x = lane * laneW + 5f; val tw = laneW - 10f
                        when (tl.state) {
                            1 -> {   // hit: the tile glows and drains into the key for as long as its note sounds
                                val flash = (1f - (now - tl.at) / 220f).coerceIn(0f, 1f)
                                val bright = lerp(c, Color.White, 0.35f + flash * 0.45f)
                                drawRoundRect(bright.copy(alpha = 0.35f), Offset(x - 4f, top - 4f), Size(tw + 8f, bottom - top + 8f), CornerRadius(14f))
                                drawRoundRect(Brush.verticalGradient(listOf(Color.White, bright), top, bottom), Offset(x, top), Size(tw, bottom - top), CornerRadius(12f))
                            }
                            2 -> drawRoundRect(Color(0x66808A92), Offset(x, top), Size(tw, bottom - top), CornerRadius(12f))
                            else -> {
                                val fadeIn = (bottom / (pxPerBeat * 0.8f)).coerceIn(0f, 1f)
                                drawRoundRect(lerp(c, Color.Black, 0.25f).copy(alpha = fadeIn), Offset(x, top + 5f), Size(tw, bottom - top), CornerRadius(12f))
                                drawRoundRect(Brush.verticalGradient(listOf(lerp(c, Color.White, 0.35f), c), top, bottom).let { it }, Offset(x, top), Size(tw, bottom - top - 2f), CornerRadius(12f), alpha = fadeIn)
                                drawRoundRect(Color.White.copy(alpha = 0.35f * fadeIn), Offset(x + 6f, top + 5f), Size(tw - 12f, ((bottom - top) * 0.22f).coerceAtMost(26f)), CornerRadius(8f))
                                if (showLetters) {
                                    val l = measurer.measure(Basics.name(tl.pitch), TextStyle(fontFamily = OpenSans, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = NoteColors.ink(tl.pitch).copy(alpha = fadeIn)))
                                    drawText(l, topLeft = Offset(x + tw / 2 - l.size.width / 2f, bottom - l.size.height - 10f))
                                }
                            }
                        }
                    }
                    // the hit line, glowing on the beat
                    val glow = if (beat >= 0) (1f - (beat % 1f)) else 0f
                    drawRect(Brush.verticalGradient(listOf(Color(0x00FFFFFF), Color.White.copy(alpha = 0.35f + glow * 0.35f)), h - 40f, h), Offset(0f, h - 40f), Size(w, 40f))
                    drawLine(Color.White, Offset(0f, h - 1.5f), Offset(w, h - 1.5f), 3f + glow * 2f)
                    drawBursts(bursts, now)
                    drawFloaters(floaters, now, measurer)
                    if (beat in 0f..COUNT_IN) {
                        val n = beat.toInt() + 1
                        val l = measurer.measure("$n", TextStyle(fontFamily = OpenSans, fontWeight = FontWeight.ExtraBold, fontSize = 64.sp, color = Color(0xFF0A548F).copy(alpha = 1f - (beat % 1f) * 0.7f)))
                        drawText(l, topLeft = Offset(w / 2 - l.size.width / 2f, h * 0.35f - l.size.height / 2f))
                    }
                }
            }
            RainbowKeys(Basics.KeyView(range = 60..71, letters = songIdx == 0, rainbow = true), emptySet(), sounding.keys, emptySet(), wrongKey,
                Modifier.fillMaxWidth().height(if (land) 104.dp else 168.dp).navigationBarsPadding().padding(start = 10.dp, end = 10.dp, bottom = 6.dp)) { k -> press(k) }
        }
    }
    if (land) Row(Modifier.fillMaxSize().padding(top = 60.dp)) {
        louSpot(Modifier.weight(0.28f).fillMaxHeight(), true)
        lanesAndKeys(Modifier.weight(0.72f).fillMaxHeight())
    } else Column(Modifier.fillMaxSize()) {
        louSpot(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 56.dp).height(96.dp), false)
        lanesAndKeys(Modifier.weight(1f).fillMaxWidth())
    }
    GameHud(score, hearts, 5, if (songIdx == 0) "Practice song" else "${songIdx + 1}/3 · ${song.title}")
}
