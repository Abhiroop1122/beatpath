package com.beatpath.app.model

import androidx.compose.ui.graphics.Color

/** 480 ticks per quarter note; one FL "step" (a sixteenth) is 120 ticks. */
const val TICKS_PER_BEAT = 480
const val TICKS_PER_STEP = 120
const val TICKS_PER_BAR = TICKS_PER_BEAT * 4

enum class ChannelKind(val label: String, val isDrum: Boolean, val drumPitch: Int, val color: Color) {
    KICK("Kick", true, 36, Color(0xFF6E7780)),
    SNARE("Snare", true, 38, Color(0xFF6E7780)),
    CLAP("Clap", true, 39, Color(0xFF6E7780)),
    HAT("Hat", true, 42, Color(0xFF6E7780)),
    OPEN_HAT("Open hat", true, 46, Color(0xFF6E7780)),
    BASS("Bass", false, 0, Color(0xFF7B5A6E)),
    CHORDS("Chords", false, 0, Color(0xFF5A6E8A)),
    LEAD("Lead", false, 0, Color(0xFF5A7E6E)),
    LEAD2("Lead 2", false, 0, Color(0xFF6E6A8A)),
}

data class Note(
    val id: Long,
    val pitch: Int,        // MIDI 0–127
    val startTick: Int,
    val durationTicks: Int,
    val velocity: Int = 100, // 1–127
)

data class Channel(
    val kind: ChannelKind,
    val muted: Boolean = false,
    val volume: Float = 0.8f,   // FL default channel volume is 78%
    val pan: Float = 0f,        // -1..1 (shown on the knob; the phone engine is mono)
    val notes: List<Note> = emptyList(),
) {
    val name: String get() = kind.label
    val isDrum: Boolean get() = kind.isDrum
    val drumPitch: Int get() = kind.drumPitch
    val color: Color get() = kind.color

    fun stepOn(step: Int): Boolean = notes.any { it.startTick == step * TICKS_PER_STEP }
    fun stepVelocity(step: Int): Int? = notes.firstOrNull { it.startTick == step * TICKS_PER_STEP }?.velocity
}

data class Pattern(
    val bars: Int = 1,
    val channels: List<Channel> = defaultChannels(),
    /** FL Channel Rack swing, 0..1: delays every second 16th (steps 2, 4, 6…). */
    val swing: Float = 0f,
) {
    val lengthTicks: Int get() = bars * TICKS_PER_BAR
    val steps: Int get() = bars * 16

    fun channel(kind: ChannelKind): Channel = channels.firstOrNull { it.kind == kind } ?: Channel(kind)

    fun update(kind: ChannelKind, f: (Channel) -> Channel): Pattern =
        if (channels.any { it.kind == kind }) copy(channels = channels.map { if (it.kind == kind) f(it) else it })
        else copy(channels = channels + f(Channel(kind)))

    fun toggleStep(kind: ChannelKind, step: Int, velocity: Int = 100): Pattern = update(kind) { ch ->
        val tick = step * TICKS_PER_STEP
        if (ch.notes.any { it.startTick == tick }) ch.copy(notes = ch.notes.filterNot { it.startTick == tick })
        else ch.copy(notes = ch.notes + Note(nextId(), ch.drumPitch, tick, TICKS_PER_STEP, velocity))
    }

    fun setNotes(kind: ChannelKind, notes: List<Note>): Pattern = update(kind) { it.copy(notes = notes) }

    /** Change length; notes beyond the new end are trimmed (the editor offers a copy before shrinking). */
    fun withBars(n: Int): Pattern = copy(bars = n, channels = channels.map { ch ->
        ch.copy(notes = ch.notes.filter { it.startTick < n * TICKS_PER_BAR }.map { it.copy(durationTicks = it.durationTicks.coerceAtMost(n * TICKS_PER_BAR - it.startTick)) })
    })

    companion object {
        private var idSeq = System.nanoTime()
        fun nextId(): Long = ++idSeq
        fun defaultChannels() = ChannelKind.entries.map { Channel(it) }

        /** Build drum steps quickly: steps are 1-based like FL's step numbers. */
        fun drums(kind: ChannelKind, vararg steps1: Int, velocity: Int = 100) =
            steps1.map { Note(nextId(), kind.drumPitch, (it - 1) * TICKS_PER_STEP, TICKS_PER_STEP, velocity) }
    }
}

/** A Pattern clip on the Playlist (FL: Pattern Clip), placed on a track at a bar. */
data class Clip(val pattern: Int, val startBar: Int, val track: Int = 0)

/**
 * A project = FL's Pattern bank + Playlist. [pattern] is the Pattern being edited (index [current]);
 * the others live in [others]. SONG mode plays the Playlist arrangement; PAT mode loops [pattern].
 */
data class Project(
    val id: Long,
    val name: String,
    val bpm: Int = 90,
    val pattern: Pattern = Pattern(),
    val mix: Mix = Mix(),
    val editedAt: Long = System.currentTimeMillis(),
    val current: Int = 0,
    val others: Map<Int, Pattern> = emptyMap(),
    val playlist: List<Clip> = emptyList(),
    val songMode: Boolean = false,
    val songBars: Int = 8,
) {
    val patternCount: Int get() = maxOf(current, others.keys.maxOrNull() ?: 0) + 1
    fun patternAt(i: Int): Pattern? = if (i == current) pattern else others[i]

    /** FL: pick another Pattern in the Pattern selector. */
    fun switchTo(i: Int): Project = if (i == current) this else copy(
        others = (others + (current to pattern)) - i,
        pattern = others[i] ?: Pattern(bars = pattern.bars, channels = pattern.channels.map { it.copy(notes = emptyList()) }),
        current = i,
    )

    /** FL: Clone pattern — a copy you can change while keeping the original. */
    fun clonePattern(): Project {
        val n = patternCount
        val copyP = pattern.copy(channels = pattern.channels.map { ch -> ch.copy(notes = ch.notes.map { it.copy(id = Pattern.nextId()) }) })
        return copy(others = others + (current to pattern), pattern = copyP, current = n)
    }

    fun newPattern(): Project = switchTo(patternCount)

    /** Length of the arrangement in bars (at least [songBars]). */
    val songLength: Int get() = maxOf(songBars, playlist.maxOfOrNull { it.startBar + (patternAt(it.pattern)?.bars ?: 1) } ?: 0)

    /** The arrangement flattened into one long Pattern, so the engine plays SONG mode exactly like a loop. */
    fun songPattern(): Pattern {
        if (!songMode || playlist.isEmpty()) return pattern
        val bars = songLength
        val channels = ChannelKind.entries.map { k ->
            val base = pattern.channel(k)
            val notes = playlist.flatMap { clip ->
                val p = patternAt(clip.pattern) ?: return@flatMap emptyList()
                val ch = p.channel(k)
                if (ch.muted) emptyList() else ch.notes.map { it.copy(startTick = it.startTick + clip.startBar * TICKS_PER_BAR) }
            }.filter { it.startTick < bars * TICKS_PER_BAR }
            base.copy(muted = false, notes = notes)
        }
        return Pattern(bars, channels, pattern.swing)
    }
}

object Pitch {
    private val names = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    /** FL convention: MIDI 60 is labelled C5. */
    fun flName(midi: Int): String = names[midi % 12] + (midi / 12)
    fun pcName(pc: Int): String = names[((pc % 12) + 12) % 12]
    fun isBlack(midi: Int): Boolean = names[midi % 12].contains('#')
    fun hz(midi: Int): Double = 440.0 * Math.pow(2.0, (midi - 69) / 12.0)
}
