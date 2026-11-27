package com.beatpath.app.lessons

import com.beatpath.app.lessons.Checks.no
import com.beatpath.app.lessons.Checks.notes
import com.beatpath.app.lessons.Checks.ok
import com.beatpath.app.lessons.Checks.posName
import com.beatpath.app.model.ChannelKind
import com.beatpath.app.model.ChannelKind.BASS
import com.beatpath.app.model.ChannelKind.CHORDS
import com.beatpath.app.model.ChannelKind.CLAP
import com.beatpath.app.model.ChannelKind.HAT
import com.beatpath.app.model.ChannelKind.KICK
import com.beatpath.app.model.ChannelKind.LEAD
import com.beatpath.app.model.ChannelKind.LEAD2
import com.beatpath.app.model.ChannelKind.OPEN_HAT
import com.beatpath.app.model.ChannelKind.SNARE
import com.beatpath.app.model.DrumKit
import com.beatpath.app.model.Note
import com.beatpath.app.model.Pattern
import com.beatpath.app.model.Pitch
import com.beatpath.app.model.Project
import com.beatpath.app.model.TICKS_PER_BAR
import com.beatpath.app.model.TICKS_PER_BEAT
import com.beatpath.app.model.TICKS_PER_STEP
import kotlin.math.abs

/**
 * Studio levels 19–22: chord voicings, genre grooves, bass lines II and melody III.
 * Same shape as the main curriculum: one idea per lesson, a guided step first, then a step without outlines,
 * and a jam to finish each level. Every check names the exact bar, beat or step that needs fixing.
 */
object PacksCurriculum {

    // ---------------- helpers (private copies of the Curriculum ones) ----------------
    private const val BAR = TICKS_PER_BAR
    private const val BEAT = TICKS_PER_BEAT

    /** "C5" → 60 (FL labels), supports # and b. */
    private fun n(name: String): Int {
        val table = mapOf('C' to 0, 'D' to 2, 'E' to 4, 'F' to 5, 'G' to 7, 'A' to 9, 'B' to 11)
        var pc = table.getValue(name[0]); var i = 1
        if (name[i] == '#') { pc++; i++ } else if (name[i] == 'b') { pc--; i++ }
        return name.substring(i).toInt() * 12 + pc
    }
    /** 0-based step indices for 1-based FL step numbers, repeated over [bars]. */
    private fun steps(vararg s1: Int, bars: Int = 1): Set<Int> = (0 until bars).flatMap { b -> s1.map { b * 16 + it - 1 } }.toSet()
    private val EIGHTHS = intArrayOf(1, 3, 5, 7, 9, 11, 13, 15)
    private val FOUR = intArrayOf(1, 5, 9, 13)
    private val ANDS = intArrayOf(3, 7, 11, 15)

    private fun note(p: Int, tick: Int, len: Int, vel: Int = 100) = Note(Pattern.nextId(), p, tick, len, vel)
    private fun chordNotes(pitches: List<Int>, tick: Int, len: Int = BAR) = pitches.map { note(it, tick, len) }
    private fun drumsFill(p: Project, kind: ChannelKind, s1: IntArray, bars: Int = p.pattern.bars, vel: Int = 100) =
        p.copy(pattern = p.pattern.setNotes(kind, steps(*s1, bars = bars).map { note(kind.drumPitch, it * TICKS_PER_STEP, TICKS_PER_STEP, vel) }))
    private fun drumsAt(p: Project, kind: ChannelKind, steps0: List<Int>) =
        p.copy(pattern = p.pattern.setNotes(kind, steps0.map { note(kind.drumPitch, it * TICKS_PER_STEP, TICKS_PER_STEP) }))
    private fun Project.set(kind: ChannelKind, ns: List<Note>) = copy(pattern = pattern.setNotes(kind, ns))
    private fun Project.clear(vararg kinds: ChannelKind) = copy(pattern = kinds.fold(pattern) { acc, k -> acc.setNotes(k, emptyList()) })
    private fun Project.kit(k: DrumKit) = copy(mix = mix.copy(drumKit = k))
    private fun Project.swing(s: Float) = copy(pattern = pattern.copy(swing = s))
    private fun chordsOf(p: Project, bars: List<List<Int>>) = p.set(CHORDS, bars.flatMapIndexed { i, c -> chordNotes(c, i * BAR) })
    private fun pcs(l: List<Int>) = l.map { it % 12 }.toSet()
    private fun bar(p: Project, k: ChannelKind, b0: Int) = notes(p, k).filter { it.startTick / BAR == b0 }
    private fun onsets(p: Project, k: ChannelKind, b0: Int) = bar(p, k, b0).map { it.startTick % BAR }
    private fun pcList(s: Set<Int>) = s.joinToString(", ") { Pitch.pcName(it) }

    private fun chordPcsAtBar(p: Project, bar0: Int): Set<Int> = notes(p, CHORDS).filter { it.startTick / BAR == bar0 }.let { ns ->
        val first = ns.minOfOrNull { it.startTick } ?: return emptySet(); ns.filter { it.startTick == first }.map { it.pitch % 12 }.toSet()
    }

    /** Guided drum step: glow the next missing pad; check the exact set. */
    private fun drumStep(say: String, task: String, kind: ChannelKind, target: Set<Int>, hint: String, success: String, guided: Boolean = true, setup: ((Project) -> Project)? = null) =
        LessonStep(say, task, Win.RACK, kind,
            guides = { p -> if (!guided) emptyList() else target.sorted().firstOrNull { !p.pattern.channel(kind).stepOn(it) }?.let { listOf(Ghost(kind, step = it)) } ?: emptyList() },
            hintGhosts = target.map { Ghost(kind, step = it) },
            hint = hint, check = { p, _ -> Checks.drumSteps(p, kind, target, success) }, setup = setup)

    /** Outline the first target note that isn't placed yet. */
    private fun rollGuide(kind: ChannelKind, targets: List<Triple<Int, Int, Int>>) = { p: Project ->
        targets.filter { (pitch, tick, _) -> p.pattern.channel(kind).notes.none { it.pitch == pitch && it.startTick == tick } }
            .take(1).map { (pitch, tick, len) -> Ghost(kind, pitch = pitch, tick = tick, lengthTicks = len) }
    }
    private fun ghosts(kind: ChannelKind, targets: List<Triple<Int, Int, Int>>) = targets.map { (pitch, tick, len) -> Ghost(kind, pitch = pitch, tick = tick, lengthTicks = len) }

    // =====================================================================
    val levels = listOf(
        Level(19, "Chord voicings", "Inversions, open chords and smooth moves.", Track.HARMONY),
        Level(20, "Genre grooves", "Lo-fi, house, trap, pop and drum and bass.", Track.BEATS),
        Level(21, "Bass lines II", "Octave jumps, walking, following the kick, long 808s.", Track.BASS),
        Level(22, "Melody III", "Call and response, countermelodies, a high point, coming home.", Track.MELODY),
    )

    // =====================================================================
    // Level 19 · Chord voicings
    // =====================================================================
    private val C5 = listOf(n("C5"), n("E5"), n("G5"))
    private val F5 = listOf(n("F5"), n("A5"), n("C6"))
    private val F4 = listOf(n("F4"), n("A4"), n("C5"))
    private val G4 = listOf(n("G4"), n("B4"), n("D5"))
    private val G5 = listOf(n("G5"), n("B5"), n("D6"))
    private val Am4 = listOf(n("A4"), n("C5"), n("E5"))
    private val C_PCS = pcs(C5); private val F_PCS = pcs(F5); private val G_PCS = pcs(G4); private val AM_PCS = pcs(Am4)

    private fun voicing(p: Project, bar0: Int) = notes(p, CHORDS).filter { it.startTick == bar0 * BAR }.map { it.pitch }
    private fun voiceCost(a: List<Int>, b: List<Int>) = a.sorted().zip(b.sorted()).sumOf { (x, y) -> abs(x - y) }

    /** The chord on beat 1 of [bar0] has the right notes, exactly 3 of them, with [bassPc] at the bottom. */
    private fun shapeCheck(p: Project, bar0: Int, chordPcs: Set<Int>, bassPc: Int, name: String): Verdict? {
        val v = Checks.chordAt(p, CHORDS, bar0 * BAR, chordPcs, name, bassPc)
        if (!v.pass) return v.copy(body = "Bar ${bar0 + 1}: " + v.body)
        val count = voicing(p, bar0).size
        if (count != 3) return no("Three notes only", "Bar ${bar0 + 1} has $count notes. Keep just 3. Move notes instead of adding them.")
        return null
    }

    private fun inversion(p: Project, bar0: Int, chordPcs: Set<Int>, bassPc: Int, name: String, success: String): Verdict =
        shapeCheck(p, bar0, chordPcs, bassPc, name) ?: ok("Flipped!", success)

    private fun openShape(p: Project, bar0: Int, chordPcs: Set<Int>, rootPc: Int, name: String, success: String): Verdict {
        shapeCheck(p, bar0, chordPcs, rootPc, name)?.let { return it }
        val ps = voicing(p, bar0)
        val span = ps.max() - ps.min()
        return when {
            span <= 12 -> no("Spread it wider", "Bar ${bar0 + 1}'s notes still fit inside one octave. Lift the middle note up 12 rows.")
            span > 24 -> no("Too far apart", "Bar ${bar0 + 1}'s notes are more than two octaves apart. Bring the top note down.")
            else -> ok("Wide open!", success)
        }
    }

    private fun smoothPair(p: Project, from0: Int, to0: Int, toPcs: Set<Int>, toName: String, maxCost: Int, success: String): Verdict {
        val v = Checks.chordAt(p, CHORDS, to0 * BAR, toPcs, toName)
        if (!v.pass) return v.copy(body = "Bar ${to0 + 1}: " + v.body)
        val b = voicing(p, to0)
        if (b.size != 3) return no("Three notes only", "Bar ${to0 + 1} has ${b.size} notes. Keep exactly 3.")
        val cost = voiceCost(voicing(p, from0), b)
        if (cost > maxCost) return no("Still jumpy", "From bar ${from0 + 1} to bar ${to0 + 1} the notes move $cost rows in all. Keep the shared note still. Move the others by 1 or 2 rows.")
        return ok("Smooth!", success)
    }

    private fun smoothLoop(p: Project, chords: List<Pair<String, Set<Int>>>, maxEach: Int, maxTotal: Int, head: String, success: String): Verdict {
        val prog = Checks.progression(p, CHORDS, chords); if (!prog.pass) return prog
        val v = chords.indices.map { voicing(p, it) }
        v.indexOfFirst { it.size != 3 }.takeIf { it >= 0 }?.let { return no("Three notes each", "Bar ${it + 1} has ${v[it].size} notes. Each chord keeps exactly 3.") }
        val costs = v.zipWithNext { a, b -> voiceCost(a, b) }
        costs.indexOfFirst { it > maxEach }.takeIf { it >= 0 }?.let { i ->
            return no("Big jump", "From bar ${i + 1} to bar ${i + 2} the notes move ${costs[i]} rows in all. Keep shared notes still and step the others.")
        }
        if (costs.sum() > maxTotal) return no("Still a bit jumpy", "All together the notes move ${costs.sum()} rows. Try to get it to $maxTotal or less.")
        return ok(head, "$success Total movement: ${costs.sum()} rows.")
    }

    private val rootLoop = { p: Project -> chordsOf(p, listOf(C5, F5, G5, C5)) }
    private val smoothTargets = listOf(listOf(n("C5"), n("F5"), n("A5")), listOf(n("B4"), n("D5"), n("G5")), C5)
        .flatMapIndexed { i, ch -> ch.map { Triple(it, (i + 1) * BAR, BAR) } }

    private val l19 = listOf(
        Lesson("voice.first-inversion", 19, "First inversion", "Lift the bottom note up an octave. Now a new note sits at the bottom.", 3, 90, 2,
            show = listOf(CHORDS), rows = 55..81, setup = { chordsOf(it, listOf(C5, F5)) },
            steps = listOf(
                LessonStep("Remember inversions from level 9? Same chord, but a different note at the bottom. Let's lift C.", "Move the bottom note C5 up to C6.", Win.ROLL, CHORDS,
                    guides = rollGuide(CHORDS, listOf(Triple(n("C6"), 0, BAR))), hintGhosts = ghosts(CHORDS, listOf(Triple(n("C6"), 0, BAR))),
                    hint = "Drag C5 in bar 1 up 12 rows to C6. E5 and G5 stay where they are.",
                    check = { p, _ -> inversion(p, 0, C_PCS, 4, "C major", "E is at the bottom now. That's first inversion. It sounds lighter.") }),
                LessonStep("Your turn with F. Lift its bottom note up an octave. Then A sits at the bottom.", "Make F in bar 2 first inversion, with A at the bottom.", Win.ROLL, CHORDS,
                    hint = "In bar 2, drag F5 up 12 rows to F6. You get A5, C6 and F6.",
                    check = { p, _ -> inversion(p, 1, F_PCS, 9, "F major", "A at the bottom. Same F chord, new color.") }),
            ), flTip = "In FL, select the lowest note of a chord and move it up an octave."),
        Lesson("voice.second-inversion", 19, "Second inversion", "Lift the bottom two notes. Now the top note of the chord sits at the bottom.", 3, 90, 2,
            show = listOf(CHORDS), rows = 50..81, setup = { chordsOf(it, listOf(C5, F4)) },
            steps = listOf(
                LessonStep("Second inversion lifts two notes. C goes up 12 rows, then E goes up too. Now G is at the bottom.", "Lift C5 and E5 up to C6 and E6.", Win.ROLL, CHORDS,
                    guides = rollGuide(CHORDS, listOf(Triple(n("C6"), 0, BAR), Triple(n("E6"), 0, BAR))),
                    hintGhosts = ghosts(CHORDS, listOf(Triple(n("C6"), 0, BAR), Triple(n("E6"), 0, BAR))),
                    hint = "In bar 1, drag C5 up to C6, then E5 up to E6. G5 stays put.",
                    check = { p, _ -> inversion(p, 0, C_PCS, 7, "C major", "G at the bottom. That's second inversion. It sounds open and floaty.") }),
                LessonStep("Now F. Lift its two bottom notes up an octave, so C ends up at the bottom.", "Make F in bar 2 second inversion, with C at the bottom.", Win.ROLL, CHORDS,
                    hint = "In bar 2, lift F4 to F5 and A4 to A5. You get C5, F5 and A5.",
                    check = { p, _ -> inversion(p, 1, F_PCS, 0, "F major", "C at the bottom of F. You can flip any chord now.") }),
            ), flTip = "In FL, select two notes and move them up an octave together."),
        Lesson("voice.open", 19, "Open chords", "Closed chords squeeze notes close. Open chords spread them wide.", 3, 90, 2,
            show = listOf(CHORDS), rows = 43..72,
            setup = { chordsOf(it, listOf(listOf(n("C4"), n("E4"), n("G4")), listOf(n("A3"), n("C4"), n("E4")))) },
            steps = listOf(
                LessonStep("Closed chords squeeze notes close, like a hug. Open chords spread out, like arms wide. Lift the middle note up 12 rows.", "Lift E4 up to E5 to open up C.", Win.ROLL, CHORDS,
                    guides = rollGuide(CHORDS, listOf(Triple(n("E5"), 0, BAR))), hintGhosts = ghosts(CHORDS, listOf(Triple(n("E5"), 0, BAR))),
                    hint = "In bar 1, drag E4 up 12 rows to E5. You get C4, G4 and E5.",
                    check = { p, _ -> openShape(p, 0, C_PCS, 0, "C major", "Same three notes, but now the chord sounds big and airy.") }),
                LessonStep("Now open up A minor in bar 2. Same trick: the middle note jumps up an octave.", "Open up A minor in bar 2.", Win.ROLL, CHORDS,
                    hint = "In bar 2, drag C4 up 12 rows to C5. You get A3, E4 and C5.",
                    check = { p, _ -> openShape(p, 1, AM_PCS, 9, "A minor", "Wide and soft. Film music loves open chords.") }),
            ), flTip = "In FL, spread chord notes across octaves to make pads sound bigger."),
        Lesson("voice.shared-note", 19, "Keep the shared note", "When two chords share a note, keep it still. Move the others a tiny step.", 3, 90, 2,
            show = listOf(CHORDS), rows = 52..79, setup = { chordsOf(it, listOf(C5, F5)) },
            steps = listOf(
                LessonStep("Remember smooth moves from level 9? C and F share the note C, so keep it still. Move the other two a tiny step.", "Make F in bar 2 as C5, F5 and A5.", Win.ROLL, CHORDS,
                    guides = rollGuide(CHORDS, listOf(n("C5"), n("F5"), n("A5")).map { Triple(it, BAR, BAR) }),
                    hintGhosts = ghosts(CHORDS, listOf(n("C5"), n("F5"), n("A5")).map { Triple(it, BAR, BAR) }),
                    hint = "In bar 2, drag C6 down 12 rows to C5. Now C stays, E steps to F and G steps to A.",
                    check = { p, _ -> smoothPair(p, 0, 1, F_PCS, "F major", 3, "C stayed still and the others took tiny steps. Hear how smooth it is?") }),
                LessonStep("Now C to G. They share the note G. Keep G5 still and move the other two to the nearest G chord notes.", "Make G in bar 2 with the smallest moves.", Win.ROLL, CHORDS,
                    setup = { chordsOf(it, listOf(C5, G4)) },
                    hint = "The G chord uses G, B and D. In bar 2, try B4, D5 and G5.",
                    check = { p, _ -> smoothPair(p, 0, 1, G_PCS, "G major", 3, "G stayed still. C stepped down to B and E stepped down to D.") }),
            ), flTip = "In FL, move single chord notes by octaves to find the closest shape."),
        Lesson("voice.smooth-loop", 19, "Smooth C, F, G, C", "Voice leading: every note takes the shortest path to the next chord.", 4, 90, 4,
            show = listOf(CHORDS), rows = 52..79, setup = rootLoop,
            steps = listOf(
                LessonStep("Let's smooth a whole song: C, F, G, C. Every note should take the shortest path to the next chord.", "Reshape F, G and C in bars 2 to 4 so each note moves a little.", Win.ROLL, CHORDS,
                    guides = rollGuide(CHORDS, smoothTargets), hintGhosts = ghosts(CHORDS, smoothTargets),
                    hint = "Bar 2: C5, F5, A5. Bar 3: B4, D5, G5. Bar 4: C5, E5, G5.",
                    check = { p, _ -> smoothLoop(p, listOf("C" to C_PCS, "F" to F_PCS, "G" to G_PCS, "C" to C_PCS), 6, 12, "Silky!", "Each chord glides into the next.") }),
                LessonStep("Once more from memory. Same four chords, smallest moves you can find.", "Smooth out C, F, G, C again.", Win.ROLL, CHORDS,
                    setup = rootLoop,
                    hint = "Keep shared notes still. F and G share nothing, so slide each note just 1 to 3 rows.",
                    check = { p, _ -> smoothLoop(p, listOf("C" to C_PCS, "F" to F_PCS, "G" to G_PCS, "C" to C_PCS), 6, 12, "From memory!", "That's real voice leading.") }),
            ), flTip = "Pianists play chords this way so their hands barely move."),
        Lesson("voice.jam", 19, "Jam: your own smooth chords", "Pick any shape for each chord, but make every move small.", 4, 90, 4,
            show = listOf(CHORDS, BASS), locked = setOf(BASS), rows = 36..79, jam = true,
            setup = { it.set(BASS, listOf(0, 9, 5, 7).mapIndexed { i, pc -> note(36 + pc, i * BAR, BAR) }) },
            steps = listOf(LessonStep("Jam time! Build C, A minor, F and G. Pick any shapes you like, but make every move small.", "Play C, Am, F, G, one per bar, with small moves.", Win.ROLL, CHORDS,
                hint = "Start with C5, E5, G5. A minor shares C and E with it, so only one note moves.",
                check = { p, _ -> smoothLoop(p, listOf("C" to C_PCS, "Am" to AM_PCS, "F" to F_PCS, "G" to G_PCS), 6, 12, "Level 19 complete!", "You voice chords like a piano player.") })),
            flTip = "Try the same shapes with a Soft pad sound for a dreamy feel."),
    )

    // =====================================================================
    // Level 20 · Genre grooves
    // =====================================================================
    private fun trapRoll(p: Project): Verdict {
        val start = BAR + 3 * BEAT
        val hs = notes(p, HAT).map { it.startTick }.filter { it in start until 2 * BAR }.distinct().sorted()
        var best = if (hs.isEmpty()) 0 else 1; var run = 1
        for (i in 1 until hs.size) { val d = hs[i] - hs[i - 1]; run = if (d == 60 || d == 80) run + 1 else 1; best = maxOf(best, run) }
        return if (best >= 3) ok("Rrrrrattle!", "A hat roll at the end of the loop. That's trap.")
        else no("Roll it", "Bar 2, beat 4 needs at least 3 hats in a row, each half a step apart.")
    }

    private val l20 = listOf(
        Lesson("groove.lofi", 20, "Lo-fi beat", "Lo-fi is slow, soft and lazy. Swing makes it lean back.", 3, 80, 1,
            show = listOf(KICK, SNARE, HAT), locked = setOf(SNARE),
            setup = { drumsFill(it.kit(DrumKit.LOFI), SNARE, intArrayOf(5, 13)).swing(0.35f) },
            steps = listOf(
                drumStep("Lo-fi is slow, soft and dusty, like a sleepy cat. I picked the Lo-fi kit and 80 BPM. Start with a lazy kick.", "Kick on step 1 and step 11.", KICK, steps(1, 11),
                    "Step 11 is half a beat after beat 3. It's the hip-hop kick from level 3.", "That kick leans back. Very chill."),
                drumStep("Remember swing from level 5? I turned it up, so every second hat comes a bit late. Now add the hats.", "Put a hat on all 16 steps.", HAT, (0 until 16).toSet(),
                    "Tap every pad on the Hat row, from step 1 to step 16.", "Hear the hats limp a little? That's swing. Lo-fi loves it.", guided = false),
            ), flTip = "Lo-fi beats usually sit between 70 and 90 BPM."),
        Lesson("groove.house", 20, "House beat", "House is dance music: a kick on every beat and open hats in between.", 3, 124, 1,
            show = listOf(KICK, CLAP, OPEN_HAT), locked = setOf(CLAP),
            setup = { drumsFill(it.kit(DrumKit.HOUSE), CLAP, intArrayOf(5, 13)) },
            steps = listOf(
                drumStep("House is dance music. Its kick never stops, one on every beat. Remember four on the floor from level 1?", "Kick on steps 1, 5, 9 and 13.", KICK, steps(*FOUR),
                    "The first pad of each group of four: steps 1, 5, 9 and 13.", "Thump, thump, thump. Now people can dance."),
                drumStep("House puts an open hat between the kicks. It goes tss on every and.", "Open hat on steps 3, 7, 11 and 15.", OPEN_HAT, steps(*ANDS),
                    "The and sits halfway between two beats: steps 3, 7, 11 and 15.", "Boom tss, boom tss. That's house!", guided = false),
            ), flTip = "House usually sits between 118 and 128 BPM."),
        Lesson("groove.trap", 20, "Trap beat", "Trap is fast but feels slow: the clap waits for beat 3, and the hats roll.", 4, 140, 2,
            show = listOf(KICK, CLAP, HAT), locked = setOf(KICK), rows = 42..42, snap = Snap.HALF_STEP,
            setup = { drumsFill(drumsAt(it.kit(DrumKit.EIGHT08), KICK, listOf(0, 10, 16, 23)), HAT, EIGHTHS) },
            steps = listOf(
                drumStep("Trap is heavy, even at 140 BPM. Remember half time from level 5? The clap waits for beat 3.", "Clap on step 9 in both bars.", CLAP, steps(9, bars = 2),
                    "Step 9 is beat 3. In bar 2 that's step 25.", "Half time. It feels huge and slow."),
                LessonStep("Trap hats love to roll, like a drum spinning. Add quick hats half a step apart at the end of bar 2.", "Add a roll of 3 or more quick hats in bar 2, beat 4.", Win.ROLL, HAT,
                    hint = "Snap is half a step. Tap 3 to 6 times in a row, starting at bar 2, beat 4.",
                    check = { p, _ -> trapRoll(p) }),
            ), flTip = "Trap usually sits between 130 and 150 BPM, with the clap on beat 3."),
        Lesson("groove.pop", 20, "Pop beat", "Pop beats are simple and steady, so the singer can shine.", 3, 100, 1,
            show = listOf(KICK, SNARE, HAT), locked = setOf(HAT),
            setup = { drumsFill(it.kit(DrumKit.CLASSIC), HAT, EIGHTHS) },
            steps = listOf(
                drumStep("Pop beats are simple, so the singer can shine. Kick on beats 1 and 3, plus one little push.", "Kick on steps 1, 9 and 11.", KICK, steps(1, 9, 11),
                    "Step 9 is beat 3. Step 11 is the and right after it.", "Simple and bouncy. Perfect for singing over."),
                drumStep("Now the snare takes the backbeat, just like the clap did in level 1.", "Snare on beat 2 and beat 4.", SNARE, steps(5, 13),
                    "Beat 2 is step 5. Beat 4 is step 13.", "That's a pop beat. You hear it on the radio every day.", guided = false),
            ), flTip = "Pop songs often sit between 90 and 120 BPM."),
        Lesson("groove.dnb", 20, "Drum and bass", "Tempo changes the style: the same beat, much faster, becomes drum and bass.", 3, 174, 1,
            show = listOf(KICK, SNARE, HAT), locked = setOf(HAT),
            setup = { drumsFill(it.kit(DrumKit.CLASSIC), HAT, EIGHTHS) },
            steps = listOf(
                drumStep("Drum and bass is super fast: 174 BPM! Its kick plays on step 1 and step 11.", "Kick on steps 1 and 11.", KICK, steps(1, 11),
                    "Step 11 is half a beat after beat 3.", "Already racing!"),
                drumStep("Add the snare on 2 and 4. It's the lo-fi beat again, just over twice as fast!", "Snare on steps 5 and 13.", SNARE, steps(5, 13),
                    "Beat 2 is step 5. Beat 4 is step 13.", "Same pattern, faster tempo, new style. Tempo matters!", guided = false),
            ), flTip = "Drum and bass usually sits between 170 and 178 BPM."),
        Lesson("groove.jam", 20, "Jam: make it house", "Change a beat's style by changing its kick and hats.", 4, 124, 1,
            show = listOf(KICK, CLAP, HAT, OPEN_HAT), jam = true,
            setup = { drumsFill(drumsFill(drumsFill(it.kit(DrumKit.HOUSE), KICK, intArrayOf(1, 11)), CLAP, intArrayOf(5, 13)), HAT, EIGHTHS) },
            steps = listOf(LessonStep("Jam! This beat has a hip-hop kick. Make it house: a kick on every beat and open hats on the ands.", "Turn this beat into house.", Win.RACK, KICK,
                hint = "Kick on steps 1, 5, 9, 13. Open hat on 3, 7, 11, 15. Remove the closed hats on those steps.",
                hintGhosts = steps(*FOUR).map { Ghost(KICK, step = it) } + steps(*ANDS).map { Ghost(OPEN_HAT, step = it) },
                check = { p, _ ->
                    val closed = (0 until 16).filter { p.pattern.channel(HAT).stepOn(it) }.toSet()
                    val both = closed.intersect(steps(*ANDS)).sorted()
                    listOf(Checks.drumSteps(p, KICK, steps(*FOUR), ""), Checks.drumSteps(p, CLAP, steps(5, 13), ""), Checks.drumSteps(p, OPEN_HAT, steps(*ANDS), ""))
                        .firstOrNull { !it.pass }
                        ?: if (both.isNotEmpty()) no("Both hats at once", "Remove the closed hat on ${Checks.stepName(both.first())}. The open hat plays there.")
                        else ok("Level 20 complete!", "Same clap, new kick and hats, and it's house. You can switch styles now.")
                })),
            flTip = "Try the same beat with each kit and hear how the style changes."),
    )

    // =====================================================================
    // Level 21 · Bass lines II
    // =====================================================================
    private val lowC = listOf(n("C4"), n("E4"), n("G4"))
    private val lowG = listOf(n("G3"), n("B3"), n("D4"))
    private val lowAm = listOf(n("A3"), n("C4"), n("E4"))
    private val lowF = listOf(n("F3"), n("A3"), n("C4"))

    private fun octaveBar(p: Project, bar0: Int, pc: Int, success: String): Verdict {
        val ns = bar(p, BASS, bar0)
        if (ns.map { it.startTick % BAR } != listOf(0, BEAT, 2 * BEAT, 3 * BEAT)) return no("One note per beat", "Bar ${bar0 + 1} needs 4 bass notes, one on each beat.")
        ns.firstOrNull { it.pitch % 12 != pc }?.let { return no("Stay on ${Pitch.pcName(pc)}", "${Pitch.flName(it.pitch)} at ${posName(it.startTick)} isn't a ${Pitch.pcName(pc)}. Every note is ${Pitch.pcName(pc)}, low or high.") }
        val low = ns.filter { (it.startTick % BAR) % (2 * BEAT) == 0 }.map { it.pitch }.toSet()
        val high = ns.filter { (it.startTick % BAR) % (2 * BEAT) != 0 }.map { it.pitch }.toSet()
        if (low.size != 1 || high.size != 1 || high.first() - low.first() != 12) return no("Jump one octave", "Bar ${bar0 + 1}: beats 1 and 3 low, beats 2 and 4 exactly 12 rows higher.")
        return ok("Boing, boing!", success)
    }

    private fun walkBar(p: Project, bar0: Int, rootPc: Int, nextBass: Int, success: String): Verdict {
        val ns = bar(p, BASS, bar0)
        if (ns.map { it.startTick % BAR } != listOf(0, BEAT, 2 * BEAT, 3 * BEAT)) return no("One note per beat", "Bar ${bar0 + 1} needs 4 bass notes, one on each beat.")
        if (ns[0].pitch % 12 != rootPc) return no("Start on the root", "Beat 1 of bar ${bar0 + 1} should be ${Pitch.pcName(rootPc)}, the chord's name note.")
        ns.firstOrNull { it.pitch % 12 !in Scales.C_MAJOR }?.let { return no("White keys only", "${Pitch.flName(it.pitch)} at ${posName(it.startTick)} isn't in C major.") }
        ns.zipWithNext().firstOrNull { (a, b) -> b.pitch - a.pitch !in 1..2 }?.let { (a, b) ->
            return no("One step up each beat", "${Pitch.flName(a.pitch)} to ${Pitch.flName(b.pitch)} isn't one step up. Go to the very next white key.")
        }
        if (nextBass - ns.last().pitch !in 1..2) return no("Land on the next chord", "Your last note, ${Pitch.flName(ns.last().pitch)}, should sit one step below ${Pitch.flName(nextBass)} in bar ${bar0 + 2}.")
        return ok("Walking!", success)
    }

    private fun followKick(p: Project, bar0: Int, pc: Int, success: String): Verdict {
        val kicks = (0 until 16).filter { p.pattern.channel(KICK).stepOn(bar0 * 16 + it) }.map { it * TICKS_PER_STEP }.toSet()
        if (kicks.isEmpty()) return no("No kick yet", "Bar ${bar0 + 1} has no kick to follow.")
        val ons = onsets(p, BASS, bar0)
        (kicks - ons.toSet()).minOrNull()?.let { return no("Hit with the kick", "Add a bass note at ${posName(bar0 * BAR + it)}, where the kick hits.") }
        (ons.toSet() - kicks).minOrNull()?.let { return no("Only with the kick", "The bass note at ${posName(bar0 * BAR + it)} has no kick. Remove it.") }
        if (ons.size != kicks.size) return no("One note per kick", "Bar ${bar0 + 1} has two bass notes starting together. Keep one.")
        bar(p, BASS, bar0).firstOrNull { it.pitch % 12 != pc }?.let { return no("Stay on the root", "${Pitch.flName(it.pitch)} at ${posName(it.startTick)} should be ${Pitch.pcName(pc)}, the chord's name note.") }
        return ok("Locked in!", success)
    }

    /** 808 notes start on kicks and hold until the next kick (the last one to the end). Checks the kicks at [which]. */
    private fun holdCheck(p: Project, which: List<Int>?, success: String): Verdict {
        val kicks = notes(p, KICK).map { it.startTick }.distinct().sorted()
        if (kicks.isEmpty()) return no("No kick yet", "An 808 rides the kick, and there's no kick here.")
        val ends = kicks.drop(1) + p.pattern.lengthTicks
        val b = notes(p, BASS)
        b.firstOrNull { it.startTick !in kicks }?.let { return no("Ride the kick", "The 808 note at ${posName(it.startTick)} has no kick. Every 808 note starts on a kick.") }
        if (b.groupBy { it.startTick }.any { it.value.size > 1 }) return no("One at a time", "An 808 plays one note at a time. Two notes start together.")
        b.firstOrNull { it.pitch % 12 !in Scales.C_MINOR }?.let { return no("Stay in key", "${Pitch.flName(it.pitch)} isn't in C minor. Use the shaded rows.") }
        for (i in which ?: kicks.indices.toList()) {
            val k = kicks[i]
            val m = b.firstOrNull { it.startTick == k } ?: return no("Add a note", "Put an 808 note on the kick at ${posName(k)}.")
            if (i == 0 && m.pitch % 12 != 0) return no("Start on C", "The first 808 note should be a C.")
            val want = ends[i] - k
            val endName = if (ends[i] >= p.pattern.lengthTicks) "the end of bar ${p.pattern.bars}" else "the next kick at ${posName(ends[i])}"
            if (m.durationTicks < want - TICKS_PER_STEP) return no("Hold it longer", "The note at ${posName(k)} stops early. Drag its end to $endName.")
            if (m.durationTicks > want + TICKS_PER_STEP) return no("Too long", "The note at ${posName(k)} runs past the next kick. End it at $endName.")
        }
        return ok("Boom.", success)
    }

    private val trapKicks = listOf(0, 10, 16, 24)

    private val l21 = listOf(
        Lesson("bass2.octave", 21, "Octave jumps", "The bass can jump between a note and the same note one octave up.", 3, 100, 2,
            show = listOf(CHORDS, BASS), locked = setOf(CHORDS), ghost = setOf(CHORDS), rows = 29..52,
            setup = { chordsOf(it, listOf(lowC, lowG)) },
            steps = listOf(
                LessonStep("Remember the octave bounce from level 6? This time it's one note per beat: low, high, low, high.", "Bar 1: C3, C4, C3, C4, one on each beat.", Win.ROLL, BASS,
                    guides = rollGuide(BASS, listOf(Triple(n("C3"), 0, BEAT), Triple(n("C4"), BEAT, BEAT), Triple(n("C3"), 2 * BEAT, BEAT), Triple(n("C4"), 3 * BEAT, BEAT))),
                    hint = "Low C3 on beats 1 and 3. High C4 on beats 2 and 4.",
                    check = { p, _ -> octaveBar(p, 0, 0, "Octave jumps make the bass bounce.") }),
                LessonStep("Bar 2 has a G chord. Same bounce, but on G.", "Bar 2: octave jumps on G.", Win.ROLL, BASS,
                    hint = "Low G2 on beats 1 and 3. High G3 on beats 2 and 4.",
                    check = { p, _ -> octaveBar(p, 1, 7, "Low G, high G. You can bounce any chord now.") }),
            ), flTip = "In FL, select a note and move it up an octave with Ctrl and the up arrow."),
        Lesson("bass2.walk-up", 21, "Walk up to the next chord", "A walking bass climbs one step per beat and lands on the next chord.", 3, 96, 4,
            show = listOf(CHORDS, BASS), locked = setOf(CHORDS), ghost = setOf(CHORDS), rows = 33..55, scale = Scales.C_MAJOR,
            setup = { chordsOf(it, listOf(lowC, lowG, lowF, lowC)).set(BASS, listOf(note(n("G3"), BAR, BAR), note(n("C4"), 3 * BAR, BAR))) },
            steps = listOf(
                LessonStep("A walking bass climbs one step per beat, like walking upstairs. It arrives right on the next chord.", "Bar 1: walk up C3, D3, E3, F3 into G.", Win.ROLL, BASS,
                    guides = rollGuide(BASS, listOf("C3", "D3", "E3", "F3").mapIndexed { i, s -> Triple(n(s), i * BEAT, BEAT) }),
                    hint = "One note per beat: C3, D3, E3, F3. The G3 in bar 2 is already there.",
                    check = { p, _ -> walkBar(p, 0, 0, n("G3"), "Step, step, step, step, and you're on G!") }),
                LessonStep("Bar 3 is F and bar 4 is C. Walk up from F so you land on C.", "Bar 3: walk up from F3 into C4.", Win.ROLL, BASS,
                    hint = "F3, G3, A3, B3. Each one is the next white key up.",
                    check = { p, _ -> walkBar(p, 2, 5, n("C4"), "B steps right up to C. The bass pulls the song along.") }),
            ), flTip = "Jazz and funk bass players walk like this all the time."),
        Lesson("bass2.follow-kick", 21, "Follow the kick", "Bass notes that start with the kick punch like one big drum.", 3, 92, 2,
            show = listOf(KICK, CHORDS, BASS), locked = setOf(KICK, CHORDS), ghost = setOf(KICK, CHORDS), rows = 36..52,
            setup = { drumsFill(chordsOf(it, listOf(lowAm, lowF)), KICK, intArrayOf(1, 4, 11)) },
            steps = listOf(
                LessonStep("Remember locking bass to the kick in level 6? This kick is jumpier. Follow it like a shadow.", "Bar 1: an A bass note on every kick.", Win.ROLL, BASS,
                    guides = rollGuide(BASS, listOf(0, 3, 10).map { Triple(n("A3"), it * TICKS_PER_STEP, 2 * TICKS_PER_STEP) }),
                    hint = "Kicks are on steps 1, 4 and 11. Put A3 on each one.",
                    check = { p, _ -> followKick(p, 0, 9, "Kick and bass hit together like one big drum.") }),
                LessonStep("Bar 2 is an F chord. Follow the same kicks, this time with F.", "Bar 2: an F bass note on every kick.", Win.ROLL, BASS,
                    hint = "Steps 1, 4 and 11 of bar 2. Use F3.",
                    check = { p, _ -> followKick(p, 1, 5, "Your bass is the kick's shadow now.") }),
            ), flTip = "In FL, turn on ghost notes to see the kick behind the bass."),
        Lesson("bass2.808-hold", 21, "An 808 that holds", "An 808 note should ring right up to the next kick.", 4, 140, 2,
            show = listOf(KICK, CLAP, BASS), locked = setOf(KICK, CLAP), ghost = setOf(KICK), rows = 24..48, scale = Scales.C_MINOR, snap = Snap.HALF,
            setup = { drumsFill(drumsAt(it.kit(DrumKit.EIGHT08), KICK, trapKicks), CLAP, intArrayOf(9)) },
            steps = listOf(
                LessonStep("Remember the long 808 from level 6? A good 808 holds until the next kick. No gaps.", "Place C3 on the first kick and hold it until the next kick.", Win.ROLL, BASS,
                    guides = rollGuide(BASS, listOf(Triple(n("C3"), 0, 10 * TICKS_PER_STEP))),
                    hint = "Start at bar 1, beat 1. Drag its end to step 11, where the next kick is.",
                    check = { p, _ -> holdCheck(p, listOf(0), "It rings right up to the next kick. Smooth and heavy.") }),
                LessonStep("Now give every other kick an 808 note. Each one holds until the next kick.", "Add an 808 note on each kick, held until the next one.", Win.ROLL, BASS,
                    hint = "Kicks: bar 1 step 11, bar 2 beat 1, bar 2 beat 3. The last note holds to the end.",
                    check = { p, _ -> holdCheck(p, null, "Every kick gets a long, tuned boom. That's an 808 line!") }),
            ), flTip = "In FL, set an 808 channel to mono so notes never pile up."),
        Lesson("bass2.jam", 21, "Jam: your bass line", "Roots on beat 1, plus an octave jump or a walk.", 5, 96, 4,
            show = listOf(KICK, CLAP, HAT, CHORDS, BASS), locked = setOf(KICK, CLAP, HAT, CHORDS), ghost = setOf(CHORDS), rows = 29..60, scale = Scales.C_MAJOR, snap = Snap.HALF, jam = true,
            setup = { drumsFill(drumsFill(drumsFill(chordsOf(it, listOf(lowC, lowG, lowAm, lowF)), KICK, intArrayOf(1, 9)), CLAP, intArrayOf(5, 13)), HAT, EIGHTHS) },
            steps = listOf(LessonStep("Jam! Write your own bass line under C, G, Am and F. Start each bar on the root. Add one trick.", "Write a bass line with an octave jump or a walk.", Win.ROLL, BASS,
                hint = "Roots: C, G, A, F on beat 1. Trick: jump 12 rows, or step into the next root on beat 4.",
                check = { p, _ ->
                    val roots = listOf(0, 7, 9, 5)
                    val ns = notes(p, BASS)
                    val miss = (0..3).firstOrNull { b -> ns.none { it.startTick == b * BAR && it.pitch % 12 == roots[b] } }
                    val walk = (0..2).any { b ->
                        val last = bar(p, BASS, b).lastOrNull(); val next = ns.firstOrNull { it.startTick == (b + 1) * BAR }
                        last != null && next != null && last.startTick % BAR >= 3 * BEAT && abs(next.pitch - last.pitch) in 1..2
                    }
                    when {
                        miss != null -> no("Start on the root", "Bar ${miss + 1} needs ${Pitch.pcName(roots[miss])} on beat 1.")
                        ns.groupBy { it.startTick }.any { it.value.size > 1 } -> no("One note at a time", "Bass plays one note at a time. Two notes start together.")
                        ns.any { it.pitch % 12 !in Scales.C_MAJOR } -> no("Stay in key", "Use the shaded rows only.")
                        ns.size < 8 -> no("More notes", "Use at least 8 bass notes in all. You have ${ns.size}.")
                        (0..3).any { bar(p, BASS, it).size > 8 } -> no("Less busy", "Keep each bar to 8 notes or fewer.")
                        ns.zipWithNext().none { (a, b) -> abs(b.pitch - a.pitch) == 12 } && !walk ->
                            no("Add a trick", "Add an octave jump, or walk one step into the next chord's root on beat 4.")
                        else -> ok("Level 21 complete!", "Roots, bounce and walking. Your bass lines move now.")
                    }
                })),
            flTip = "Try your line with the Pluck bass sound."),
    )

    // =====================================================================
    // Level 22 · Melody III
    // =====================================================================
    private val call1 = listOf(Triple(n("E5"), 0, BEAT), Triple(n("G5"), BEAT, BEAT), Triple(n("A5"), 2 * BEAT, BEAT))
    private val call2 = listOf(Triple(n("B5"), 2 * BAR, 240), Triple(n("D6"), 2 * BAR + 240, 240), Triple(n("B5"), 2 * BAR + BEAT, BEAT), Triple(n("G5"), 2 * BAR + 2 * BEAT, BEAT))

    private fun answerCheck(p: Project, callBar0: Int, ansBar0: Int, success: String): Verdict {
        val call = bar(p, LEAD, callBar0); val ans = bar(p, LEAD2, ansBar0)
        notes(p, LEAD2).firstOrNull { it.startTick / BAR == callBar0 }?.let { return no("Wait your turn", "Lead 2 has a note at ${posName(it.startTick)}. That's the call. Answer in bar ${ansBar0 + 1}.") }
        if (ans.isEmpty()) return no("Answer back", "Write Lead 2 notes in bar ${ansBar0 + 1}.")
        if (ans.map { it.startTick % BAR } != call.map { it.startTick % BAR }) {
            return no("Copy the rhythm", "Start Lead 2 notes where the call does: ${call.joinToString(", ") { posName(it.startTick % BAR + ansBar0 * BAR) }}.")
        }
        ans.firstOrNull { it.pitch % 12 !in Scales.C_MAJOR }?.let { return no("Stay in key", "${Pitch.flName(it.pitch)} isn't in C major. Use the shaded rows.") }
        if (ans.last().pitch % 12 != 0) return no("End at home", "Finish bar ${ansBar0 + 1} on a C, the home note.")
        return ok("They're talking!", success)
    }

    private val busyLead = listOf(
        listOf(72, 67, 64, 67, 72, 67, 64, 67), listOf(71, 67, 74, 67, 71, 67, 74, 67),
        listOf(72, 69, 64, 69, 72, 69, 64, 69), listOf(72, 69, 65, 69, 72, 69, 65, 69),
    ).flatMapIndexed { b, ps -> ps.mapIndexed { i, x -> Triple(x, b * BAR + i * 240, 240) } }

    private fun counterCheck(p: Project, want: List<Int>, minLen: Int, success: String): Verdict {
        for (b in 0..3) {
            val ns = bar(p, LEAD2, b)
            if (ns.map { it.startTick % BAR } != want) return no("Slow and steady",
                if (want.size == 1) "Bar ${b + 1} needs one Lead 2 note, starting on beat 1." else "Bar ${b + 1} needs two Lead 2 notes, on beat 1 and beat 3.")
            ns.firstOrNull { it.durationTicks < minLen }?.let { return no("Hold it longer", "The Lead 2 note at ${posName(it.startTick)} is short. Let it ring for ${if (minLen >= BAR - BEAT) "the whole bar" else "2 beats"}.") }
            val chord = chordPcsAtBar(p, b)
            ns.firstOrNull { it.pitch % 12 !in chord }?.let { return no("Pick a chord note", "${Pitch.flName(it.pitch)} at ${posName(it.startTick)} isn't in bar ${b + 1}'s chord. Use ${pcList(chord)}.") }
            val lowLead = bar(p, LEAD, b).minOfOrNull { it.pitch } ?: continue
            ns.firstOrNull { it.pitch >= lowLead }?.let { return no("Stay underneath", "${Pitch.flName(it.pitch)} at ${posName(it.startTick)} is as high as the lead. Move it below ${Pitch.flName(lowLead)}.") }
        }
        return ok("Two voices!", success)
    }

    private val climb = listOf(
        Triple(n("C5"), 0, BEAT), Triple(n("D5"), BEAT, BEAT), Triple(n("E5"), 2 * BEAT, BEAT), Triple(n("F5"), 3 * BEAT, BEAT),
        Triple(n("G5"), BAR, BEAT), Triple(n("A5"), BAR + BEAT, BEAT), Triple(n("B5"), BAR + 2 * BEAT, 2 * BEAT),
        Triple(n("A5"), 2 * BAR + 2 * BEAT, BEAT), Triple(n("G5"), 2 * BAR + 3 * BEAT, BEAT),
        Triple(n("F5"), 3 * BAR, BEAT), Triple(n("E5"), 3 * BAR + BEAT, BEAT), Triple(n("D5"), 3 * BAR + 2 * BEAT, BEAT), Triple(n("C5"), 3 * BAR + 3 * BEAT, BEAT),
    )
    private val PEAK = 2 * BAR

    private fun peakPlaced(p: Project): Verdict {
        val ns = notes(p, LEAD)
        val top = ns.filter { it.startTick == PEAK }
        if (top.isEmpty()) return no("Place the peak", "Put a note on bar 3, beat 1.")
        if (top.size > 1) return no("One note at a time", "Bar 3, beat 1 has ${top.size} notes. Keep just one.")
        val others = ns.filter { it.startTick != PEAK }.maxOfOrNull { it.pitch } ?: 0
        if (top[0].pitch <= others) return no("Go higher", "The note on bar 3, beat 1 must be the highest of all. ${Pitch.flName(others)} is as high or higher.")
        if (top[0].pitch % 12 !in AM_PCS) return no("Pick a chord note", "Bar 3 is A minor: A, C, E. Try C6.")
        return ok("Mountain top!", "Everything climbs up to it and then walks back down.")
    }

    private fun peakShape(p: Project, peakBars: IntRange): Verdict? {
        val ns = notes(p, LEAD)
        val top = ns.maxOf { it.pitch }
        val count = ns.count { it.pitch == top }
        if (count > 1) return no("One top note", "${Pitch.flName(top)} shows up $count times. Only one note should be the highest.")
        val t = ns.first { it.pitch == top }
        if (t.startTick / BAR !in peakBars) return no("Move the peak", "Your highest note, ${Pitch.flName(top)}, is at ${posName(t.startTick)}. Put the peak in bar ${peakBars.joinToString(" or ") { "${it + 1}" }}.")
        if (ns.first().pitch > top - 5 || ns.last().pitch > top - 5) return no("Start and end lower", "Begin and end at least 5 rows below your peak, ${Pitch.flName(top)}.")
        return null
    }

    private fun endHome(p: Project, minLen: Int, success: String): Verdict {
        val ns = notes(p, LEAD)
        val last = ns.lastOrNull() ?: return no("Write the tune", "Add some notes first.")
        if (last.startTick < BAR) return no("Add the ending", "Bar 2 is empty. Put a C on bar 2, beat 1.")
        if (ns.count { it.startTick == last.startTick } > 1) return no("One note at a time", "Two notes start at ${posName(last.startTick)}. Keep one.")
        if (last.pitch % 12 != 0) return no("Not home yet", "The last note is ${Pitch.flName(last.pitch)}. End on C, note 1.")
        if (last.startTick !in setOf(BAR, BAR + 2 * BEAT)) return no("Land on a strong beat", "Start the last C on bar 2, beat 1 or beat 3.")
        if (last.durationTicks < minLen) return no("Hold it longer", "Make the last C at least ${minLen / BEAT} beats long. A long note says the end.")
        return ok("Home!", success)
    }

    private val lowLoop = { p: Project -> chordsOf(p, listOf(lowC, lowG, lowAm, lowF)) }

    private val l22 = listOf(
        Lesson("mel3.call-response", 22, "Call and response", "One instrument calls, another answers with the same rhythm.", 4, 96, 4,
            show = listOf(CHORDS, LEAD, LEAD2), locked = setOf(CHORDS, LEAD), ghost = setOf(CHORDS, LEAD), rows = 52..79, scale = Scales.C_MAJOR, snap = Snap.HALF,
            setup = { chordsOf(it, listOf(lowC, lowF, lowG, lowC)).set(LEAD, (call1 + call2).map { (x, t, d) -> note(x, t, d) }) },
            steps = listOf(
                LessonStep("Remember question and answer from level 8? Now two players talk. Lead calls in bar 1. Lead 2 answers in bar 2.", "Answer in bar 2 on Lead 2: same rhythm, end on C.", Win.ROLL, LEAD2,
                    guides = rollGuide(LEAD2, listOf(Triple(n("G5"), BAR, BEAT), Triple(n("F5"), BAR + BEAT, BEAT), Triple(n("C5"), BAR + 2 * BEAT, BEAT))),
                    hint = "Copy the call's rhythm: beats 1, 2 and 3 of bar 2. Try G5, F5, C5.",
                    check = { p, _ -> answerCheck(p, 0, 1, "Lead calls, Lead 2 answers. That's call and response.") }),
                LessonStep("Lead calls again in bar 3. Write your own answer in bar 4. Same rhythm, end on home.", "Answer the second call in bar 4.", Win.ROLL, LEAD2,
                    hint = "Copy the call's rhythm: beat 1, the and of beat 1, beat 2, beat 3. End on a C.",
                    check = { p, _ -> answerCheck(p, 2, 3, "Your two players have a real conversation.") }),
            ), flTip = "Give the answer a different sound so the two voices stand apart."),
        Lesson("mel3.countermelody", 22, "A countermelody", "Under a busy lead, a second line plays slow, long notes.", 4, 96, 4,
            show = listOf(CHORDS, LEAD, LEAD2), locked = setOf(CHORDS, LEAD), ghost = setOf(CHORDS, LEAD), rows = 48..79, scale = Scales.C_MAJOR, snap = Snap.HALF,
            setup = { lowLoop(it).set(LEAD, busyLead.map { (x, t, d) -> note(x, t, d) }) },
            steps = listOf(
                LessonStep("Remember the counter-melody from level 10? This lead is busy. So Lead 2 plays slow, long notes underneath, like a calm friend.", "Lead 2: one long note per bar, under the lead.", Win.ROLL, LEAD2,
                    guides = rollGuide(LEAD2, listOf(n("G4"), n("G4"), n("A4"), n("A4")).mapIndexed { b, x -> Triple(x, b * BAR, BAR) }),
                    hint = "Hold each note for the whole bar. Try G4, G4, A4, A4.",
                    check = { p, _ -> counterCheck(p, listOf(0), BAR - BEAT, "Busy on top, calm below. They never bump into each other.") }),
                LessonStep("Now let Lead 2 move a bit more. Two long notes per bar, still under the lead.", "Lead 2: two notes per bar, on beats 1 and 3.", Win.ROLL, LEAD2,
                    setup = { it.clear(LEAD2) },
                    hint = "Each note lasts 2 beats. Pick notes from each bar's chord, below the lead.",
                    check = { p, _ -> counterCheck(p, listOf(0, 2 * BEAT), 3 * BEAT / 2, "A slow, singing line under a busy one. That's a countermelody.") }),
            ), flTip = "Give Lead 2 a soft sound so it stays in the background."),
        Lesson("mel3.peak", 22, "Climb to one high point", "A great melody climbs to one highest note, then comes back down.", 4, 96, 4,
            show = listOf(CHORDS, LEAD), locked = setOf(CHORDS), ghost = setOf(CHORDS), rows = 55..81, scale = Scales.C_MAJOR, snap = Snap.HALF,
            setup = { lowLoop(it).set(LEAD, climb.map { (x, t, d) -> note(x, t, d) }) },
            steps = listOf(
                LessonStep("Remember the arch from level 7? A great tune climbs to one high point, like a mountain top. Then it comes back down.", "Add the high point: C6 on bar 3, beat 1.", Win.ROLL, LEAD,
                    guides = rollGuide(LEAD, listOf(Triple(n("C6"), PEAK, 2 * BEAT))),
                    hint = "The tune climbs through bars 1 and 2. Put C6 on bar 3, beat 1, 2 beats long.",
                    check = { p, _ -> peakPlaced(p) }),
                LessonStep("Now write your own climb. Start low, rise to one top note in bar 3, then come back down.", "Write 8 to 14 notes with one highest note in bar 3.", Win.ROLL, LEAD,
                    setup = { it.clear(LEAD) },
                    hint = "Only one note may be the highest. Start and end at least 5 rows below it.",
                    check = { p, _ ->
                        val ns = notes(p, LEAD)
                        if (ns.size !in 8..14) no("8 to 14 notes", "Use between 8 and 14 notes. You have ${ns.size}.")
                        else Checks.melody(p, LEAD, Checks.MelodyRules(minNotes = 8, scale = Scales.C_MAJOR, minStepwise = 0.5f), "").let { base ->
                            if (!base.pass) base else peakShape(p, 2..2) ?: ok("What a climb!", "One mountain top, right where the song feels biggest.")
                        }
                    }),
            ), flTip = "Many songs put their highest note about two thirds of the way through."),
        Lesson("mel3.end-home", 22, "End on home", "A phrase sounds finished when it lands on home, note 1, on a strong beat.", 3, 96, 2,
            show = listOf(CHORDS, LEAD), locked = setOf(CHORDS), ghost = setOf(CHORDS), rows = 55..79, scale = Scales.C_MAJOR, snap = Snap.HALF,
            setup = { chordsOf(it, listOf(lowG, lowC)).set(LEAD, listOf(n("B5") to 0, n("D6") to BEAT, n("C6") to 2 * BEAT, n("B5") to 3 * BEAT).map { (x, t) -> note(x, t, BEAT) } + note(n("D5"), BAR, BAR)) },
            steps = listOf(
                LessonStep("Remember home, note 1, from the basics? This tune stops on D. It feels like a question with no answer.", "Move the last note, D5, down to C5.", Win.ROLL, LEAD,
                    guides = rollGuide(LEAD, listOf(Triple(n("C5"), BAR, BAR))),
                    hint = "Drag the long note in bar 2 down 2 rows, from D5 to C5.",
                    check = { p, _ -> endHome(p, BEAT, "Ahh, finished. Landing on C sounds like coming home.") }),
                LessonStep("This time the ending is missing. Add the last note yourself: home, on a strong beat, nice and long.", "End the tune on C in bar 2, at least 2 beats long.", Win.ROLL, LEAD,
                    setup = { it.set(LEAD, listOf(n("E5") to 0, n("G5") to BEAT, n("F5") to 2 * BEAT, n("D5") to 3 * BEAT).map { (x, t) -> note(x, t, BEAT) }) },
                    hint = "Put C5 on bar 2, beat 1. Then drag it to 2 beats or longer.",
                    check = { p, _ -> endHome(p, 2 * BEAT, "D stepped down to C on beat 1. A perfect ending.") }),
            ), flTip = "Ending on a long home note gives the loop a place to breathe."),
        Lesson("mel3.jam", 22, "Jam: a melody with a peak", "One high point in the middle, some space, and a long ending at home.", 5, 96, 4,
            show = listOf(KICK, CLAP, HAT, CHORDS, BASS, LEAD), locked = setOf(KICK, CLAP, HAT, CHORDS, BASS), ghost = setOf(CHORDS), rows = 55..84, scale = Scales.C_MAJOR, snap = Snap.HALF, jam = true,
            setup = { p ->
                drumsFill(drumsFill(drumsFill(chordsOf(p, listOf(lowC, lowF, lowG, lowC)), KICK, intArrayOf(1, 9)), CLAP, intArrayOf(5, 13)), HAT, EIGHTHS)
                    .set(BASS, listOf(0, 5, 7, 0).mapIndexed { i, pc -> note(36 + pc, i * BAR, BAR) })
            },
            steps = listOf(LessonStep("Jam! Write a 4-bar melody. Climb to one high point in bar 2 or 3, leave a gap, and end home.", "Write a melody with one peak that ends on a long C.", Win.ROLL, LEAD,
                hint = "One highest note in bar 2 or 3. A little silence somewhere. Last note: C on bar 4, beat 1 or 3.",
                check = { p, _ ->
                    val ns = notes(p, LEAD)
                    val base = Checks.melody(p, LEAD, Checks.MelodyRules(minNotes = 8, scale = Scales.C_MAJOR, minStepwise = 0.5f, needsRest = true, endsOnPc = 0), "")
                    val last = ns.lastOrNull()
                    when {
                        !base.pass -> base
                        ns.size > 16 -> no("Fewer notes", "Keep it to 16 notes or fewer. You have ${ns.size}.")
                        else -> peakShape(p, 1..2) ?: when {
                            last == null || last.startTick !in setOf(3 * BAR, 3 * BAR + 2 * BEAT) || last.durationTicks < BEAT ->
                                no("Land on a strong beat", "End with a C on bar 4, beat 1 or beat 3, at least one beat long.")
                            else -> ok("Level 22 complete!", "A climb, a peak, a breath and a home ending. That's a real melody.")
                        }
                    }
                })),
            flTip = "Export it as MIDI and try it with a different lead sound."),
    )

    val lessons: List<Lesson> = l19 + l20 + l21 + l22
}
