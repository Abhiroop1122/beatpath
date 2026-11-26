package com.beatpath.app.lessons

import com.beatpath.app.model.ChannelKind
import com.beatpath.app.model.Note
import com.beatpath.app.model.Pattern
import com.beatpath.app.model.Pitch
import com.beatpath.app.model.Project
import com.beatpath.app.model.TICKS_PER_BAR
import com.beatpath.app.model.TICKS_PER_BEAT
import com.beatpath.app.model.TICKS_PER_STEP
import kotlin.math.abs

/** Which Studio window a step happens in. Plugins open as FL-style plugin windows. */
enum class Win { RACK, ROLL, PLAYLIST, MIXER, EQ, COMP, REVERB, DELAY, LIMITER, SYNTH }

/** Live engine readings that mixing lessons check, measured since the step started or the mix last changed. */
data class Meters(
    val playedLoops: Int = 0,
    val masterMaxPeak: Float = 0f,                          // linear
    val clipCount: Int = 0,
    val channelMaxDb: Map<ChannelKind, Float> = emptyMap(), // post-fader peaks, dBFS
    val compMaxGr: Map<ChannelKind, Float> = emptyMap(),    // dB
    val limiterMaxGr: Float = 0f,                           // dB
    val loudnessLufs: Float = -70f,                         // integrated, BS.1770
)

data class Verdict(val pass: Boolean, val headline: String, val body: String)

/** A ghost outline the guide can draw (drum step or piano-roll note). */
data class Ghost(val channel: ChannelKind, val step: Int = -1, val pitch: Int = -1, val tick: Int = -1, val lengthTicks: Int = TICKS_PER_BEAT)

data class LessonStep(
    /** What Lou says (short, friendly, one idea). */
    val say: String,
    /** The single action, shown as the task line. */
    val task: String,
    val window: Win,
    val channel: ChannelKind = ChannelKind.KICK,
    /** Outlines shown while guided; empty = unguided. */
    val guides: (Project) -> List<Ghost> = { emptyList() },
    /** Revealed by the second hint. */
    val hintGhosts: List<Ghost> = emptyList(),
    val hint: String,
    val check: (Project, Meters) -> Verdict,
    /** Applied when the step starts (pre-fill, change tempo, load a broken mix to fix…). */
    val setup: ((Project) -> Project)? = null,
    /** Mixing steps: playback must run this many loops before Check (listen first). */
    val needsLoops: Int = 0,
    /** Optional A/B ear question shown before the task. */
    val listen: ListenQuestion? = null,
)

data class ListenQuestion(val question: String, val a: (Project) -> Project, val b: (Project) -> Project, val answerIsA: Boolean, val explain: String)

enum class Snap(val label: String, val ticks: Int) { BAR("Bar", 1920), BEAT("Beat", 480), HALF("½ beat", 240), STEP("Step", 120), HALF_STEP("½ step", 60), TRIPLET("⅓ beat", 160) }

data class Lesson(
    val id: String,
    val level: Int,
    val title: String,
    val idea: String,
    val minutes: Int,
    val bpm: Int,
    val bars: Int,
    val steps: List<LessonStep>,
    val flTip: String,
    val setup: (Project) -> Project = { it },
    /** Channels visible in the Rack (others hidden to keep the screen simple). */
    val show: List<ChannelKind> = listOf(ChannelKind.KICK),
    /** Channels that play but can't be edited in this lesson. */
    val locked: Set<ChannelKind> = emptySet(),
    /** Piano-roll rows (MIDI), high→low is drawn top→bottom. */
    val rows: IntRange = 55..76,
    /** Highlighted scale (pitch classes) in the Piano roll, or null. */
    val scale: Set<Int>? = null,
    /** Channels drawn as ghost outlines in the Piano roll. */
    val ghost: Set<ChannelKind> = emptySet(),
    val snap: Snap = Snap.BEAT,
    val jam: Boolean = false,
)

data class Level(val number: Int, val name: String, val blurb: String, val track: Track)
enum class Track { BEATS, NOTES, HARMONY, MELODY, BASS, ARRANGE, SOUND, MIX, MASTER }

// =====================================================================
// Check primitives (generic; lessons compose these)
// =====================================================================
object Checks {
    fun ok(headline: String, body: String) = Verdict(true, headline, body)
    fun no(headline: String, body: String) = Verdict(false, headline, body)

    fun stepName(step0: Int) = "step ${step0 + 1} (beat ${step0 % 16 / 4 + 1}${if (step0 >= 16) ", bar ${step0 / 16 + 1}" else ""})"

    /** Drum steps (0-based) must equal [want] exactly within the pattern. */
    fun drumSteps(p: Project, kind: ChannelKind, want: Set<Int>, success: String): Verdict {
        val on = (0 until p.pattern.steps).filter { p.pattern.channel(kind).stepOn(it) }.toSet()
        val missing = (want - on).sorted(); val extra = (on - want).sorted()
        return when {
            missing.isEmpty() && extra.isEmpty() -> ok("Nailed it!", success)
            missing.isNotEmpty() -> no("Almost", "${kind.label} still needs ${stepName(missing.first())}.")
            else -> no("One extra hit", "${kind.label} on ${stepName(extra.first())} isn't part of this pattern — tap it to remove it.")
        }
    }

    fun notes(p: Project, k: ChannelKind): List<Note> = p.pattern.channel(k).notes.sortedWith(compareBy({ it.startTick }, { it.pitch }))

    /** The notes that start together at [tick] form exactly these pitch classes (any octave/inversion unless [bassPc] given). */
    fun chordAt(p: Project, k: ChannelKind, tick: Int, pcs: Set<Int>, name: String, bassPc: Int? = null): Verdict {
        val all = notes(p, k)
        val at = all.filter { it.startTick == tick }
        val got = at.map { it.pitch % 12 }.toSet()
        val where = "bar ${tick / TICKS_PER_BAR + 1}, beat ${tick % TICKS_PER_BAR / TICKS_PER_BEAT + 1}"
        if (got == pcs && at.size >= pcs.size) {
            if (bassPc != null && at.minByOrNull { it.pitch }!!.pitch % 12 != bassPc) return no("Right notes, different bottom", "Put ${Pitch.pcName(bassPc)} as the lowest note of $name.")
            return ok("That's $name!", "All its notes start together on $where.")
        }
        val late = all.filter { it.startTick != tick && it.pitch % 12 in (pcs - got) && abs(it.startTick - tick) < TICKS_PER_BAR }
        if (late.isNotEmpty()) return no("One note is off the beat", "${Pitch.flName(late.first().pitch)} should start on $where with the others — block chords start together.")
        val extra = got - pcs
        if (extra.isNotEmpty()) return no("One note doesn't belong", "${Pitch.pcName(extra.first())} isn't in $name. It uses ${pcs.joinToString(", ") { Pitch.pcName(it) }}.")
        return no("Keep stacking", "$name still needs ${(pcs - got).joinToString(" and ") { Pitch.pcName(it) }} on $where.")
    }

    /** One chord per bar, in order. */
    fun progression(p: Project, k: ChannelKind, chords: List<Pair<String, Set<Int>>>): Verdict {
        chords.forEachIndexed { bar, (name, pcs) ->
            val v = chordAt(p, k, bar * TICKS_PER_BAR, pcs, name)
            if (!v.pass) return v.copy(body = "Bar ${bar + 1}: " + v.body)
        }
        return ok("Progression complete!", chords.joinToString(" → ") { it.first } + ". Press play and hear it move.")
    }

    data class MelodyRules(
        val minNotes: Int = 4,
        val scale: Set<Int>? = null,
        val minStepwise: Float = 0f,     // share of moves that are ≤ 2 semitones
        val maxLeap: Int = 12,
        val endsOnPc: Int? = null,
        val needsRest: Boolean = false,
        val lowest: Int = 0, val highest: Int = 127,
        val noOverlaps: Boolean = true,
        /** Pitch classes allowed on each beat 1 of a bar (chord tones), keyed by bar. */
        val chordTonesOnDownbeats: Map<Int, Set<Int>> = emptyMap(),
    )

    fun melody(p: Project, k: ChannelKind, r: MelodyRules, success: String): Verdict {
        val ns = notes(p, k)
        if (ns.size < r.minNotes) return no("Keep writing", "Use at least ${r.minNotes} notes — you have ${ns.size}.")
        if (r.noOverlaps) {
            val stacked = ns.groupBy { it.startTick }.values.firstOrNull { it.size > 1 }
            if (stacked != null) return no("One note at a time", "A melody plays one note at a time. Two notes start together at ${posName(stacked[0].startTick)}.")
        }
        r.scale?.let { sc -> ns.firstOrNull { it.pitch % 12 !in sc }?.let { return no("Outside the key", "${Pitch.flName(it.pitch)} isn't in this scale. Try a neighbouring shaded row.") } }
        ns.firstOrNull { it.pitch < r.lowest || it.pitch > r.highest }?.let { return no("Out of range", "Keep notes between ${Pitch.flName(r.lowest)} and ${Pitch.flName(r.highest)}.") }
        val moves = ns.zipWithNext { a, b -> abs(b.pitch - a.pitch) }
        moves.indexOfFirst { it > r.maxLeap }.takeIf { it >= 0 }?.let { return no("That jump is big", "From ${Pitch.flName(ns[it].pitch)} to ${Pitch.flName(ns[it + 1].pitch)} leaps ${moves[it]} semitones. Keep leaps within ${r.maxLeap}.") }
        if (moves.isNotEmpty() && r.minStepwise > 0f) {
            val share = moves.count { it <= 2 }.toFloat() / moves.size
            if (share < r.minStepwise) return no("Lots of jumping", "Only ${(share * 100).toInt()}% of moves are steps. Melodies people sing move mostly by step — try neighbouring notes.")
        }
        if (r.needsRest) {
            val gaps = ns.zipWithNext().any { (a, b) -> b.startTick > a.startTick + a.durationTicks } || ns.first().startTick > 0
            if (!gaps) return no("Let it breathe", "Leave at least one gap (a rest) between two notes.")
        }
        r.endsOnPc?.let { pc -> if (ns.last().pitch % 12 != pc) return no("End on home", "Finish on ${Pitch.pcName(pc)} — the home note — so it sounds complete.") }
        r.chordTonesOnDownbeats.forEach { (bar, pcs) ->
            val n = ns.firstOrNull { it.startTick == bar * TICKS_PER_BAR }
                ?: return no("Start bar ${bar + 1} with a note", "Put a note right on beat 1 of bar ${bar + 1}.")
            if (n.pitch % 12 !in pcs) return no("Lean on the chord", "Beat 1 of bar ${bar + 1} sounds best on a chord tone: ${pcs.joinToString(", ") { Pitch.pcName(it) }}.")
        }
        return ok("Sounds like a melody!", success)
    }

    /** Onsets (ticks within each bar) of [k] must equal those of [ref] in bar [refBar] when compared to [bar]. */
    fun sameRhythm(p: Project, k: ChannelKind, refBar: Int, bar: Int): Boolean {
        fun on(b: Int) = notes(p, k).filter { it.startTick / TICKS_PER_BAR == b }.map { it.startTick % TICKS_PER_BAR }.toSet()
        return on(refBar) == on(bar) && on(bar).isNotEmpty()
    }

    fun posName(tick: Int) = "bar ${tick / TICKS_PER_BAR + 1} beat ${tick % TICKS_PER_BAR / TICKS_PER_BEAT + 1}" +
        (tick % TICKS_PER_BEAT).let { if (it == 0) "" else " step ${it / TICKS_PER_STEP + 1}" }

    fun inRange(v: Float, lo: Float, hi: Float) = v in lo..hi
}

object Scales {
    val C_MAJOR = setOf(0, 2, 4, 5, 7, 9, 11)
    val A_MINOR = C_MAJOR
    val C_MINOR = setOf(0, 2, 3, 5, 7, 8, 10)
    val C_PENTA = setOf(0, 2, 4, 7, 9)
    val A_MINOR_PENTA = setOf(9, 0, 2, 4, 7)
    fun triad(rootPc: Int, minor: Boolean = false) = setOf(rootPc % 12, (rootPc + if (minor) 3 else 4) % 12, (rootPc + 7) % 12)
    fun seventh(rootPc: Int, kind: String) = when (kind) {
        "maj7" -> setOf(rootPc, rootPc + 4, rootPc + 7, rootPc + 11)
        "m7" -> setOf(rootPc, rootPc + 3, rootPc + 7, rootPc + 10)
        else -> setOf(rootPc, rootPc + 4, rootPc + 7, rootPc + 10)
    }.map { it % 12 }.toSet()
}

/** Pattern-building helpers for lesson setups. */
object Seed {
    fun note(pitch: Int, beat: Float, beats: Float, vel: Int = 100) =
        Note(Pattern.nextId(), pitch, (beat * TICKS_PER_BEAT).toInt(), (beats * TICKS_PER_BEAT).toInt(), vel)
    fun chord(pitches: List<Int>, beat: Float, beats: Float) = pitches.map { note(it, beat, beats) }
    fun drums(p: Pattern, kind: ChannelKind, vararg steps1: Int, vel: Int = 100): Pattern =
        p.setNotes(kind, Pattern.drums(kind, *steps1, velocity = vel))
}
