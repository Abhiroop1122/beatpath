package com.beatpath.app.lessons

import com.beatpath.app.model.ChannelKind
import com.beatpath.app.model.Pitch
import com.beatpath.app.model.Project
import com.beatpath.app.model.TICKS_PER_BAR
import com.beatpath.app.model.TICKS_PER_BEAT
import com.beatpath.app.ui.lesson.EarItem
import com.beatpath.app.ui.lesson.EarKind
import com.beatpath.app.ui.lesson.EarSpec
import kotlin.random.Random

/**
 * Endless practice: every drill is generated, so the app never runs out after the course.
 * Ear drills reuse the ear-lesson screen; melody dictation is a generated Studio lesson.
 */
object Practice {
    const val MIX = "practice.mix"
    const val CHORDS = "practice.chords"
    const val DICTATION = "practice.dictation"
    private const val EAR = "practice.ear."

    enum class Skill(val id: String, val label: String, val blurb: String, val kind: EarKind?) {
        HIGH_LOW("high-low", "High or low", "Which note is higher?", EarKind.HigherPad),
        SOFT_LOUD("soft-loud", "Pitch or volume", "Higher — or just louder?", EarKind.SoftLoud),
        CONTOUR("contour", "Up or down", "Where does the melody go?", EarKind.Contour),
        STEP_LEAP("step-leap", "Step or leap", "Small step or a jump?", EarKind.StepLeap),
        MAJOR_MINOR("major-minor", "Major or minor", "Bright or dark chord?", EarKind.ChordQuality),
        CHORD_SPOT("chord-spot", "Which chord?", "C, then F, G or A minor?", EarKind.ChordSpot),
        DICTATION("dictation", "Melody dictation", "Hear 4 notes, write them.", null);

        /** Route id that starts this drill. */
        val route: String get() = when (this) { DICTATION -> Practice.DICTATION; CHORD_SPOT -> CHORDS; else -> EAR + id }
    }

    fun skillOf(kind: EarKind): Skill = Skill.entries.first { it.kind == kind }
    fun isPractice(id: String?) = id?.startsWith("practice.") == true
    fun isEar(id: String) = isPractice(id) && id != DICTATION

    /** Set by the app to the learner's accuracy per skill (0..1); drives the Daily mix. */
    var accuracy: (Skill) -> Float = { 0.5f }

    // ------------------------------------------------------------------ ear drills
    fun item(kind: EarKind, rnd: Random): EarItem = when (kind) {
        EarKind.HigherPad -> {
            val a = rnd.nextInt(50, 76); val gap = rnd.nextInt(3, 13) * if (rnd.nextBoolean()) 1 else -1
            EarItem(a, (a + gap).coerceIn(45, 84).let { if (it == a) a + 3 else it }, kind = kind)
        }
        EarKind.SoftLoud -> {
            val a = rnd.nextInt(57, 72)
            when (rnd.nextInt(3)) {
                0 -> if (rnd.nextBoolean()) EarItem(a, a, 0.35f, 0.95f, kind) else EarItem(a, a, 0.95f, 0.35f, kind) // same note, one louder
                1 -> EarItem(a, a + rnd.nextInt(3, 8), 0.95f, 0.4f, kind)   // B higher but softer — a trap for "louder"
                else -> EarItem(a + rnd.nextInt(3, 8), a, 0.4f, 0.95f, kind) // A higher but softer
            }
        }
        EarKind.Contour -> {
            val a = rnd.nextInt(57, 73)
            val move = when (rnd.nextInt(5)) { 0 -> 0; 1, 2 -> rnd.nextInt(2, 8); else -> -rnd.nextInt(2, 8) }
            EarItem(a, a + move, kind = kind)
        }
        EarKind.StepLeap -> {
            val a = rnd.nextInt(60, 73)
            val size = if (rnd.nextBoolean()) rnd.nextInt(1, 3) else rnd.nextInt(4, 10)
            EarItem(a, a + size * if (rnd.nextBoolean()) 1 else -1, kind = kind)
        }
        EarKind.ChordQuality -> EarItem(rnd.nextInt(55, 68), rnd.nextInt(2), kind = kind)
        EarKind.ChordSpot -> EarItem(60, rnd.nextInt(3), kind = kind)
    }

    private fun items(kind: EarKind, n: Int, rnd: Random): List<EarItem> {
        val out = mutableListOf<EarItem>()
        while (out.size < n) { val it = item(kind, rnd); if (out.lastOrNull() != it) out += it }
        return out
    }

    /** Daily mix: 8 questions, weighted toward the skills you get wrong most. */
    fun mixKinds(rnd: Random, n: Int = 8, acc: (Skill) -> Float = accuracy): List<EarKind> {
        val skills = Skill.entries.filter { it.kind != null }
        val weights = skills.map { (1f - acc(it)).coerceIn(0f, 1f) + 0.15f }
        val picked = mutableListOf<EarKind>()
        while (picked.size < n) {
            var r = rnd.nextFloat() * weights.sum()
            val i = weights.indexOfFirst { w -> r -= w; r <= 0f }.let { if (it < 0) weights.lastIndex else it }
            val k = skills[i].kind!!
            if (picked.size >= 2 && picked.takeLast(2).all { it == k }) continue // no three in a row
            picked += k
        }
        return picked
    }

    fun earSpec(id: String, rnd: Random = Random(System.nanoTime())): EarSpec = when {
        id == MIX -> EarSpec(id, EarKind.HigherPad, "Daily mix", "Eight questions from all your ear skills — more of the ones you find hard.",
            mixKinds(rnd).map { item(it, rnd) }, teach = false)
        id == CHORDS -> EarSpec(id, EarKind.ChordSpot, "Which chord?", "I play C, then F, G or A minor. Name the second chord.",
            items(EarKind.ChordSpot, 8, rnd), teach = false)
        else -> {
            val skill = Skill.entries.firstOrNull { EAR + it.id == id && it.kind != null } ?: Skill.HIGH_LOW
            EarSpec(id, skill.kind!!, skill.label + "?", skill.blurb, items(skill.kind, 8, rnd), teach = false)
        }
    }

    // ------------------------------------------------------------------ melody dictation
    private val cMajor = listOf(60, 62, 64, 65, 67, 69, 71, 72)

    /** Four quarter notes in C major, mostly stepwise, starting and ending on a chord tone (C, E or G). */
    fun dictationMelody(rnd: Random): List<Int> {
        var d = listOf(0, 2, 4).random(rnd)
        val out = mutableListOf(cMajor[d])
        repeat(3) { i ->
            val moves = if (i == 2) listOf(0, 2, 4, 7).map { it - d }.filter { kotlin.math.abs(it) in 1..3 } // land on a chord tone
                        else listOf(-2, -1, 1, 2, 1, -1, 3, -3)
            val m = moves.filter { d + it in cMajor.indices }.ifEmpty { listOf(if (d > 3) -1 else 1) }.random(rnd)
            d += m; out += cMajor[d]
        }
        return out
    }

    fun lesson(id: String?, seed: Long): Lesson? {
        if (id != DICTATION) return null
        val melody = dictationMelody(Random(seed))
        val ref = melody.mapIndexed { i, p -> Seed.note(p, i.toFloat(), 1f) }
        val targets = melody.mapIndexed { i, p -> Ghost(ChannelKind.LEAD, pitch = p, tick = TICKS_PER_BAR + i * TICKS_PER_BEAT) }
        return Lesson(DICTATION, 0, "Melody dictation", "Train your ear: hear a short tune, then write it yourself.", 3, 90, 2,
            setup = { p -> p.copy(pattern = p.pattern.setNotes(ChannelKind.LEAD2, ref)) },
            show = listOf(ChannelKind.LEAD), locked = setOf(ChannelKind.LEAD2), rows = 57..74, scale = Scales.C_MAJOR, snap = Snap.BEAT,
            steps = listOf(LessonStep(
                say = "Tap Listen: bar 1 plays my tune — four notes in C major. Write the same notes in bar 2, one per beat.",
                task = "Copy the 4 notes into bar 2 (beats 1–4).",
                window = Win.ROLL, channel = ChannelKind.LEAD,
                hintGhosts = targets,
                hint = "It starts on ${Pitch.flName(melody[0])}. Sing it back, then find each note: up or down from the last one?",
                check = { p, _ -> checkDictation(p, melody) },
            )),
            flTip = "Transcribing melodies by ear is how most producers learn. In FL, loop a section (Ctrl+click the timeline) and play along on the Piano roll keys.")
    }

    fun checkDictation(p: Project, melody: List<Int>): Verdict {
        val mine = p.pattern.channel(ChannelKind.LEAD).notes.filter { it.startTick >= TICKS_PER_BAR }
        for ((i, want) in melody.withIndex()) {
            val at = TICKS_PER_BAR + i * TICKS_PER_BEAT
            val n = mine.filter { kotlin.math.abs(it.startTick - at) < TICKS_PER_BEAT / 4 }.minByOrNull { kotlin.math.abs(it.pitch - want) }
                ?: return Checks.no("Not yet", "Put a note on bar 2, beat ${i + 1}. ${if (mine.isEmpty()) "Start with the first note you heard." else ""}".trim())
            if (n.pitch != want) {
                val dir = if (want > n.pitch) "higher" else "lower"
                return Checks.no("Close", "Note ${i + 1} (beat ${i + 1}) should be $dir than ${Pitch.flName(n.pitch)}. Listen to bar 1 again.")
            }
        }
        val extra = mine.count { m -> melody.indices.none { i -> kotlin.math.abs(m.startTick - (TICKS_PER_BAR + i * TICKS_PER_BEAT)) < TICKS_PER_BEAT / 4 } }
        if (extra > 0) return Checks.no("Almost", "All four notes are right — remove the $extra extra note${if (extra > 1) "s" else ""}.")
        return Checks.ok("You wrote it by ear!", "${melody.joinToString(" – ") { Pitch.flName(it) }}. That's real transcription.")
    }

    // ------------------------------------------------------------------ spaced review
    /** Days until a lesson comes back: after you needed help, then 1, 3 and 7 days of getting it right. */
    val reviewIntervals = listOf(1, 3, 7)

    /**
     * Next review for a lesson, as (due epoch day, stage), or null when it's mastered / never needed review.
     * Needing help resets the schedule to tomorrow; each solo pass moves it further out.
     */
    fun nextReview(prev: Pair<Long, Int>?, unassisted: Boolean, today: Long): Pair<Long, Int>? = when {
        !unassisted -> today + reviewIntervals[0] to 0
        prev == null -> null
        prev.second + 1 >= reviewIntervals.size -> null
        else -> (today + reviewIntervals[prev.second + 1]) to prev.second + 1
    }
}

