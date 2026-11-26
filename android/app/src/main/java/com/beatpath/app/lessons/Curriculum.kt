package com.beatpath.app.lessons

import com.beatpath.app.lessons.Checks.melody
import com.beatpath.app.lessons.Checks.no
import com.beatpath.app.lessons.Checks.notes
import com.beatpath.app.lessons.Checks.ok
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
import com.beatpath.app.model.Note
import com.beatpath.app.model.Pattern
import com.beatpath.app.model.Pitch
import com.beatpath.app.model.Project
import com.beatpath.app.model.TICKS_PER_BAR
import com.beatpath.app.model.TICKS_PER_BEAT
import com.beatpath.app.model.TICKS_PER_STEP
import kotlin.math.abs

/**
 * The Studio curriculum: music theory and melody making, beginner → advanced,
 * built from research/studio-curriculum.md (beats, notes, chords, feel, bass, melody, harmony, a finished hook).
 * Every lesson = guided phase(s) then a fresh, cue-free phase; Jams have no cues at all.
 */
object Curriculum {

    // ---------------- notation helpers ----------------
    /** "C5" → 60 (FL labels), supports # and b. */
    fun n(name: String): Int {
        val pcs = mapOf('C' to 0, 'D' to 2, 'E' to 4, 'F' to 5, 'G' to 7, 'A' to 9, 'B' to 11)
        var pc = pcs[name[0]]!!; var i = 1
        if (name[i] == '#') { pc++; i++ } else if (name[i] == 'b') { pc--; i++ }
        return name.substring(i).toInt() * 12 + pc
    }
    /** Tick at bar (1-based) and beat (1-based, 1.5 = the "and" of 1). */
    fun at(bar: Int, beat: Float) = (bar - 1) * TICKS_PER_BAR + ((beat - 1f) * TICKS_PER_BEAT).toInt()
    /** 0-based step indices for 1-based FL step numbers, repeated over [bars]. */
    fun steps(vararg s1: Int, bars: Int = 1): Set<Int> = (0 until bars).flatMap { b -> s1.map { b * 16 + it - 1 } }.toSet()
    private val EIGHTHS = intArrayOf(1, 3, 5, 7, 9, 11, 13, 15)
    private val FOUR = intArrayOf(1, 5, 9, 13)
    private fun note(p: Int, tick: Int, len: Int, vel: Int = 100) = Note(Pattern.nextId(), p, tick, len, vel)
    private fun chordNotes(pitches: List<Int>, tick: Int, len: Int) = pitches.map { note(it, tick, len) }
    private fun drumsFill(p: Project, kind: ChannelKind, s1: IntArray, bars: Int = p.pattern.bars, vel: Int = 100) =
        p.copy(pattern = p.pattern.setNotes(kind, steps(*s1, bars = bars).map { note(kind.drumPitch, it * TICKS_PER_STEP, TICKS_PER_STEP, vel) }))
    private fun Project.set(kind: ChannelKind, notes: List<Note>) = copy(pattern = pattern.setNotes(kind, notes))
    private fun Project.clear(vararg kinds: ChannelKind) = copy(pattern = kinds.fold(pattern) { acc, k -> acc.setNotes(k, emptyList()) })
    private fun Project.bpm(b: Int) = copy(bpm = b)

    private val C = listOf(n("C5"), n("E5"), n("G5"))
    private val F = listOf(n("F5"), n("A5"), n("C6"))
    private val G = listOf(n("G4"), n("B4"), n("D5"))
    private val Am = listOf(n("A4"), n("C5"), n("E5"))
    private val Dm = listOf(n("D5"), n("F5"), n("A5"))
    private fun pcs(l: List<Int>) = l.map { it % 12 }.toSet()
    private val LOOP = listOf("C" to C, "G" to G, "Am" to Am, "F" to F)
    private fun loopChords(p: Project, bars: List<Pair<String, List<Int>>> = LOOP) =
        p.set(CHORDS, bars.flatMapIndexed { i, (_, ch) -> chordNotes(ch, i * TICKS_PER_BAR, TICKS_PER_BAR) })
    private fun beatDrums(p: Project) = drumsFill(drumsFill(drumsFill(p, KICK, intArrayOf(1, 9)), CLAP, intArrayOf(5, 13)), HAT, EIGHTHS)
    private fun roots(p: Project, rootsPc: List<Int>, len: Int = TICKS_PER_BAR) =
        p.set(BASS, rootsPc.mapIndexed { i, pc -> note(36 + pc, i * TICKS_PER_BAR, len) })

    // ---------------- step builders ----------------
    /** Guided drum step: glow the next missing pad; check exact set. */
    private fun drumStep(say: String, task: String, kind: ChannelKind, target: Set<Int>, hint: String, success: String, guided: Boolean = true, setup: ((Project) -> Project)? = null) =
        LessonStep(say, task, Win.RACK, kind,
            guides = { p -> if (!guided) emptyList() else target.sorted().firstOrNull { !p.pattern.channel(kind).stepOn(it) }?.let { listOf(Ghost(kind, step = it)) } ?: emptyList() },
            hintGhosts = target.map { Ghost(kind, step = it) },
            hint = hint, check = chk@{ p, _ -> Checks.drumSteps(p, kind, target, success) }, setup = setup)

    private fun rollGuide(kind: ChannelKind, targets: List<Triple<Int, Int, Int>>) = { p: Project ->
        targets.filter { (pitch, tick, _) -> p.pattern.channel(kind).notes.none { it.pitch == pitch && it.startTick == tick } }
            .take(1).map { (pitch, tick, len) -> Ghost(kind, pitch = pitch, tick = tick, lengthTicks = len) }
    }

    /** Exact notes by pitch + onset (durations ignored). */
    private fun exactNotes(p: Project, kind: ChannelKind, want: List<Pair<Int, Int>>, success: String): Verdict {
        val got = notes(p, kind).map { it.pitch to it.startTick }.toSet()
        want.firstOrNull { it !in got }?.let { (pitch, tick) -> return no("Almost", "Place ${Pitch.flName(pitch)} at ${Checks.posName(tick)}.") }
        (got - want.toSet()).firstOrNull()?.let { (pitch, tick) -> return no("One extra note", "${Pitch.flName(pitch)} at ${Checks.posName(tick)} isn't part of this. Use Delete (D) to remove it.") }
        return ok("Spot on!", success)
    }

    private fun rhythmOf(p: Project, kind: ChannelKind) = notes(p, kind).map { it.startTick to it.durationTicks }

    private fun rhythmCheck(p: Project, kind: ChannelKind, ref: List<Pair<Int, Int>>, durTol: Int, success: String): Verdict {
        val got = rhythmOf(p, kind)
        ref.forEach { (on, d) ->
            val m = got.firstOrNull { it.first == on } ?: return no("A note is missing", "Start a note at ${Checks.posName(on)}.")
            if (abs(m.second - d) > durTol) return no(if (m.second > d) "Too long" else "Too short",
                "The note at ${Checks.posName(on)} should last ${beatsName(d)} — drag its right edge.")
        }
        got.firstOrNull { g -> ref.none { it.first == g.first } }?.let { return no("One extra note", "Nothing should start at ${Checks.posName(it.first)} — that's a rest.") }
        return ok("Rhythm nailed!", success)
    }

    private fun beatsName(t: Int) = when (t) { 1920 -> "a whole bar"; 960 -> "2 beats"; 480 -> "1 beat"; 240 -> "half a beat"; 120 -> "one step"; else -> "${t / 480f} beats" }

    private fun barNotes(p: Project, k: ChannelKind, bar0: Int) = notes(p, k).filter { it.startTick / TICKS_PER_BAR == bar0 }
    private fun onsetsInBar(p: Project, k: ChannelKind, bar0: Int) = barNotes(p, k, bar0).map { it.startTick % TICKS_PER_BAR }.toSet()
    private fun rhythmSimilarity(a: Set<Int>, b: Set<Int>): Float = if (a.isEmpty() && b.isEmpty()) 1f else (a intersect b).size.toFloat() / (a union b).size

    private fun chordPcsAtBar(p: Project, bar0: Int): Set<Int> = notes(p, CHORDS).filter { it.startTick / TICKS_PER_BAR == bar0 }.let { ns ->
        val first = ns.minOfOrNull { it.startTick } ?: return emptySet(); ns.filter { it.startTick == first }.map { it.pitch % 12 }.toSet()
    }
    private fun rootPcAtBar(p: Project, bar0: Int): Int? = notes(p, CHORDS).filter { it.startTick == bar0 * TICKS_PER_BAR }.minByOrNull { it.pitch }?.pitch?.let { lowest ->
        // root = chord member whose triad fits; for root-position seeds the lowest note
        val set = chordPcsAtBar(p, bar0)
        set.firstOrNull { r -> setOf(r, (r + 4) % 12, (r + 7) % 12).all { it in set } || setOf(r, (r + 3) % 12, (r + 7) % 12).all { it in set } } ?: (lowest % 12)
    }

    private fun strongBeatChordToneShare(p: Project, k: ChannelKind): Float {
        val strong = notes(p, k).filter { (it.startTick % TICKS_PER_BAR) in setOf(0, 2 * TICKS_PER_BEAT) }
        if (strong.isEmpty()) return 0f
        return strong.count { it.pitch % 12 in chordPcsAtBar(p, it.startTick / TICKS_PER_BAR) }.toFloat() / strong.size
    }

    private fun hasRest(p: Project, k: ChannelKind, min: Int, fromBar0: Int = 0, toBar0: Int = 99): Boolean {
        val ns = notes(p, k).filter { it.startTick / TICKS_PER_BAR in fromBar0..toBar0 }
        if (ns.isEmpty()) return false
        val endOfRange = minOf((toBar0 + 1) * TICKS_PER_BAR, p.pattern.lengthTicks)
        var cursor = fromBar0 * TICKS_PER_BAR
        for (nn in ns) { if (nn.startTick - cursor >= min) return true; cursor = maxOf(cursor, nn.startTick + nn.durationTicks) }
        return endOfRange - cursor >= min
    }

    private fun vel(p: Project, k: ChannelKind, step: Int) = p.pattern.channel(k).stepVelocity(step)

    // =====================================================================
    // LEVELS
    // =====================================================================
    val levels = listOf(
        Level(1, "First beat", "Kick, clap and hats in the Channel Rack.", Track.BEATS),
        Level(2, "First notes", "The Piano roll: pitch, length, scales.", Track.NOTES),
        Level(3, "Groove builder", "Sixteenths, syncopation, open hats, fills.", Track.BEATS),
        Level(4, "Chords", "Major, minor and the four-chord loop.", Track.HARMONY),
        Level(5, "Feel and dynamics", "Velocity, ghost notes, swing, trap rolls.", Track.BEATS),
        Level(6, "Bass and 808", "Roots, kick lock, octaves, long 808s.", Track.BASS),
        Level(7, "Melody I: lines", "Chord tones, steps & leaps, shape, landing home.", Track.MELODY),
        Level(8, "Melody II: motifs", "Repetition, variation, question & answer.", Track.MELODY),
        Level(9, "Harmony II", "Inversions, voice leading, 7ths, minor keys.", Track.HARMONY),
        Level(10, "Make it a track", "Passing tones, counter-melody, arrangement, capstone.", Track.ARRANGE),
    ) + TheoryCurriculum.levels

    private val l1 = listOf(
        Lesson("rack.four-on-floor", 1, "Four on the floor", "A beat is a steady pulse; four beats make a bar.", 2, 90, 1,
            show = listOf(KICK), steps = listOf(
                drumStep("Hey, I'm Lou. Every beat gets a kick — that's the heartbeat of dance music.", "Tap the glowing step on the Kick row.", KICK, steps(*FOUR),
                    "Beats start on the first pad of each group of four: steps 1, 5, 9 and 13.", "A kick on every beat. Feel my belly bounce?"),
                drumStep("Same idea, faster: 120 BPM. No glows this time.", "Build four on the floor on your own.", KICK, steps(*FOUR),
                    "Steps 1, 5, 9, 13 — the first pad of each group.", "Same pattern, new tempo — the beat just moves faster.", guided = false,
                    setup = { it.clear(KICK).bpm(120) }),
            ), flTip = "F6 opens the Channel Rack. Left-click a step to turn it on, right-click to turn it off, Space to play."),
        Lesson("rack.backbeat", 1, "Clap on 2 and 4", "The backbeat: beats 2 and 4 get the clap.", 2, 95, 1,
            show = listOf(KICK, CLAP), locked = setOf(KICK), setup = { drumsFill(it, KICK, FOUR) },
            steps = listOf(
                drumStep("Count with me: ONE two THREE four. The clap answers on 2 and 4.", "Put the Clap on beat 2 and beat 4.", CLAP, steps(5, 13),
                    "Beat 2 starts at step 5, beat 4 at step 13.", "That's the backbeat — almost every pop and house song has it."),
                drumStep("Faster, and no glows.", "Clap on 2 and 4 again at 124 BPM.", CLAP, steps(5, 13), "Steps 5 and 13.", "Locked in!", guided = false,
                    setup = { it.clear(CLAP).bpm(124) }),
            ), flTip = "This kick + clap is FL's classic house starting point. Clone a channel with Alt+C to try another clap sound."),
        Lesson("rack.eighth-hats", 1, "Hats in between", "Eighth notes: two hats per beat — the beat and the \"and\".", 2, 95, 1,
            show = listOf(KICK, CLAP, HAT), locked = setOf(KICK, CLAP), setup = { drumsFill(drumsFill(it, KICK, FOUR), CLAP, intArrayOf(5, 13)) },
            steps = listOf(
                drumStep("Hi-hats keep time between the big hits. Count \"1 and 2 and\" — every other pad.", "Fill the Hat row: every other step.", HAT, steps(*EIGHTHS),
                    "Skip one, tap one: steps 1, 3, 5, 7, 9, 11, 13, 15.", "Eighth-note hats — the groove just got a motor."),
                drumStep("Now at 110 BPM, from memory.", "Every other pad on the Hat.", HAT, steps(*EIGHTHS), "Odd-numbered steps.", "Smooth.", guided = false,
                    setup = { it.clear(HAT).bpm(110) }),
            ), flTip = "In FL, right-click a channel name → Fill each 2 steps does this in one click."),
        Lesson("rack.offbeat-hat", 1, "House hats on the \"and\"", "The offbeat: hats only between the beats.", 2, 124, 1,
            show = listOf(KICK, CLAP, HAT), locked = setOf(KICK, CLAP),
            setup = { drumsFill(drumsFill(drumsFill(it, KICK, FOUR), CLAP, intArrayOf(5, 13)), HAT, EIGHTHS) },
            steps = listOf(
                drumStep("House music puts the hat BETWEEN kicks — that \"tss\" on the and.", "Remove the hats that land with the kick.", HAT, steps(3, 7, 11, 15),
                    "Keep steps 3, 7, 11, 15. Tap 1, 5, 9, 13 to turn them off.", "Offbeat hats — instant house."),
            ), flTip = "House usually sits at 115–130 BPM. Drag FL's tempo display to try 124."),
        Lesson("rack.jam-rock", 1, "Jam: a beat from words", "Build a described beat with no glows.", 3, 100, 1,
            show = listOf(KICK, CLAP, HAT), jam = true,
            steps = listOf(LessonStep("Your turn, no help: kick on 1 and 3, clap on 2 and 4, eighth-note hats.", "Build it.", Win.RACK, KICK,
                hint = "Beat 3 starts at step 9. Hats on every odd step.",
                hintGhosts = (steps(1, 9).map { Ghost(KICK, step = it) } + steps(5, 13).map { Ghost(CLAP, step = it) }),
                check = chk@{ p, _ ->
                    listOf(Checks.drumSteps(p, KICK, steps(1, 9), ""), Checks.drumSteps(p, CLAP, steps(5, 13), ""), Checks.drumSteps(p, HAT, steps(*EIGHTHS), ""))
                        .firstOrNull { !it.pass } ?: ok("Level 1 complete!", "A full beat from a description — that's how producers talk to each other.")
                })),
            flTip = "Desktop mission: build this in FL's Channel Rack and right-click a step off."),
    )

    private val l2 = listOf(
        Lesson("roll.first-notes", 2, "Draw four notes", "Piano roll: left is earlier, up is higher.", 2, 90, 1,
            show = listOf(KICK, CLAP, HAT, LEAD), locked = setOf(KICK, CLAP, HAT), rows = 60..72, setup = { beatDrums(it) },
            steps = listOf(
                LessonStep("This is the Piano roll. Each row is a note — higher rows, higher notes. Tap to place one.", "Place C5, D5, E5, G5 on beats 1–4.", Win.ROLL, LEAD,
                    guides = rollGuide(LEAD, listOf(Triple(60, 0, 480), Triple(62, 480, 480), Triple(64, 960, 480), Triple(67, 1440, 480))),
                    hint = "Follow the outline: one note per beat, climbing.", check = chk@{ p, _ -> exactNotes(p, LEAD, listOf(60 to 0, 62 to 480, 64 to 960, 67 to 1440), "Your first melody — four notes climbing.") }),
                LessonStep("Now walk back down: G5, E5, D5, C5.", "Place the notes going down.", Win.ROLL, LEAD, setup = { it.clear(LEAD) },
                    hint = "Beat 1 is G5, then each beat one note lower.", check = chk@{ p, _ -> exactNotes(p, LEAD, listOf(67 to 0, 64 to 480, 62 to 960, 60 to 1440), "Up and down — you can draw any line now.") }),
            ), flTip = "F7 opens a channel's Piano roll. Draw tool is P; right-click deletes on desktop."),
        Lesson("roll.long-short-rest", 2, "Long, short and silence", "Note length and rests. A rest is a choice.", 3, 90, 2,
            show = listOf(LEAD), rows = 59..62, snap = Snap.HALF,
            steps = listOf(LessonStep("Drag a note's right edge to make it longer. Empty space is a rest — silence is part of rhythm.", "Copy the outlined rhythm on C5.", Win.ROLL, LEAD,
                guides = { p -> listOf(0 to 960, 960 to 480, 1920 to 240, 2160 to 240, 2400 to 480, 2880 to 960).filter { (t, d) -> p.pattern.channel(LEAD).notes.none { it.startTick == t && abs(it.durationTicks - d) <= 60 } }.map { (t, d) -> Ghost(LEAD, pitch = 60, tick = t, lengthTicks = d) } },
                hint = "Beat 4 of bar 1 stays empty — that's the rest.",
                check = chk@{ p, _ -> rhythmCheck(p, LEAD, listOf(0 to 960, 960 to 480, 1920 to 240, 2160 to 240, 2400 to 480, 2880 to 960), 60, "Long, short and a rest — that's rhythm.") })),
            flTip = "Drag a note's right edge to resize. Right-click the magnet to choose the default note length."),
        Lesson("roll.c-major-scale", 2, "Build the C major scale", "A scale is a recipe: Whole Whole Half Whole Whole Whole Half.", 3, 90, 1,
            show = listOf(LEAD), rows = 59..73, snap = Snap.HALF,
            steps = listOf(
                LessonStep("A whole step skips a key; a half step is the very next key. E→F and B→C are halves — no black key between.", "Climb from C5 to C6 on white keys only.", Win.ROLL, LEAD,
                    guides = rollGuide(LEAD, listOf(60, 62, 64, 65, 67, 69, 71, 72).mapIndexed { i, pp -> Triple(pp, i * 240, 240) }),
                    hint = "Eight notes, half a beat each: C D E F G A B C.",
                    check = chk@{ p, _ -> val seq = notes(p, LEAD).map { it.pitch }
                        if (seq == listOf(60, 62, 64, 65, 67, 69, 71, 72) && notes(p, LEAD).zipWithNext().all { (a, b) -> b.startTick > a.startTick }) ok("That's C major!", "W-W-H-W-W-W-H. Every major scale uses this recipe.")
                        else no("Not quite the scale", "Use only white keys from C5 up to C6, one after another, left to right.") }),
                LessonStep("Now come back down, C6 to C5.", "Descend the scale.", Win.ROLL, LEAD, setup = { it.clear(LEAD) }, hint = "Start on C6 and step down each half beat.",
                    check = chk@{ p, _ -> if (notes(p, LEAD).map { it.pitch } == listOf(72, 71, 69, 67, 65, 64, 62, 60)) ok("Down we go!", "You know C major both ways.") else no("Check the order", "C6 B5 A5 G5 F5 E5 D5 C5, left to right.") }),
            ), flTip = "Piano roll → Snap to scale → Major, root C turns on FL's scale highlighting."),
        Lesson("roll.pentatonic", 2, "Jam: five safe notes", "The pentatonic scale has no clashing notes.", 3, 90, 2,
            show = listOf(KICK, CLAP, HAT, LEAD), locked = setOf(KICK, CLAP, HAT), rows = 59..74, scale = Scales.C_PENTA, snap = Snap.HALF, jam = true, setup = { beatDrums(it) },
            steps = listOf(LessonStep("Only the shaded rows: C D E G A. Nothing can sound wrong. Make anything — then end on a C.", "Write 5–10 notes on the shaded rows and finish on C.", Win.ROLL, LEAD,
                hint = "Any shaded note works. The last note should be a C.",
                check = chk@{ p, _ -> melody(p, LEAD, Checks.MelodyRules(minNotes = 5, scale = Scales.C_PENTA, endsOnPc = 0), "You just improvised in C pentatonic.").let {
                    if (it.pass && notes(p, LEAD).size > 10) no("A bit busy", "Keep it to 10 notes or fewer — space makes melodies catchy.") else it } })),
            flTip = "In FL, play only the black keys from E♭ — that's the minor pentatonic."),
    )

    private val l3 = listOf(
        Lesson("rack.sixteenth-hats", 3, "Sixteenths: 1 e & a", "A step is a sixteenth: four per beat.", 2, 90, 1,
            show = listOf(KICK, CLAP, HAT), locked = setOf(KICK, CLAP), setup = { drumsFill(drumsFill(it, KICK, intArrayOf(1, 9)), CLAP, intArrayOf(5, 13)) },
            steps = listOf(drumStep("Say \"one-e-and-a, two-e-and-a\". One pad per syllable.", "Fill every step on the Hat.", HAT, (0 until 16).toSet(),
                "All 16 pads.", "Sixteenth hats — busy and driving."))
            , flTip = "FL's step is a 1/16 note in 4/4. You can click-drag across steps to paint them."),
        Lesson("rack.hiphop-kick", 3, "Syncopated kick", "Syncopation: a kick between beats pushes the groove.", 3, 90, 1,
            show = listOf(KICK, CLAP, HAT), locked = setOf(CLAP, HAT), setup = { drumsFill(drumsFill(it, CLAP, intArrayOf(5, 13)), HAT, EIGHTHS) },
            steps = listOf(
                drumStep("Hip-hop kicks dodge the beat. Try 1 and the \"and\" of 3.", "Kick on step 1 and step 11.", KICK, steps(1, 11), "Step 11 is half a beat after beat 3.", "Feel that push?"),
                LessonStep("Make your own: keep 1, add at least one kick between beats, leave 2 and 4 for the clap.", "Write your own syncopated kick.", Win.RACK, KICK,
                    setup = { it.clear(KICK).bpm(87) }, hint = "Use steps like 8, 11 or 15 — not 5 or 13.",
                    check = chk@{ p, _ ->
                        val k = (0 until 16).filter { p.pattern.channel(KICK).stepOn(it) }.toSet()
                        when {
                            0 !in k -> no("Start on the one", "Keep a kick on step 1.")
                            k.any { it == 4 || it == 12 } -> no("Leave 2 and 4", "Steps 5 and 13 belong to the clap.")
                            k.none { it % 4 != 0 } -> no("Add a surprise", "Put at least one kick on a pad that isn't a beat.")
                            k.size !in 2..5 -> no("Keep it lean", "Use 2–5 kicks.")
                            else -> ok("That's your groove!", "Syncopation is what makes heads nod.")
                        }
                    }),
            ), flTip = "This is the classic hip-hop / 2-step kick (the 1st and 6th eighth notes)."),
        Lesson("rack.open-hat", 3, "Open hat on the \"and\"", "An open hat is a long \"tss\" that the closed hat cuts off.", 3, 124, 1,
            show = listOf(KICK, CLAP, HAT, OPEN_HAT), locked = setOf(KICK, CLAP),
            setup = { drumsFill(drumsFill(drumsFill(it, KICK, FOUR), CLAP, intArrayOf(5, 13)), HAT, EIGHTHS) },
            steps = listOf(LessonStep("A drummer can't play open and closed hat at once. Put the open hat on the ands and remove the closed hat there.", "Open hat on 3, 7, 11, 15; remove closed hats on those steps.", Win.RACK, OPEN_HAT,
                guides = { p -> steps(3, 7, 11, 15).sorted().firstOrNull { !p.pattern.channel(OPEN_HAT).stepOn(it) }?.let { listOf(Ghost(OPEN_HAT, step = it)) } ?: emptyList() },
                hint = "Open Hat row: 3, 7, 11, 15. Hat row: keep only 1, 5, 9, 13.",
                check = chk@{ p, _ ->
                    val o = Checks.drumSteps(p, OPEN_HAT, steps(3, 7, 11, 15), "")
                    val hats = (0 until 16).filter { p.pattern.channel(HAT).stepOn(it) }.toSet()
                    when {
                        !o.pass -> o
                        hats.intersect(steps(3, 7, 11, 15)).isNotEmpty() -> no("Both hats at once", "Remove the closed hat where the open hat plays.")
                        !hats.containsAll(steps(*FOUR)) -> no("Keep the closed hats", "Leave closed hats on 1, 5, 9, 13.")
                        else -> ok("Tss-tss!", "Open hats on the and — the classic disco/house lift.")
                    }
                })), flTip = "Channel settings → Misc → Cut / Cut by makes the closed hat choke the open hat in FL."),
        Lesson("rack.jam-two-bars", 3, "Jam: two bars and a fill", "Repeat a bar, then change the end so the loop turns around.", 4, 95, 2,
            show = listOf(KICK, SNARE, HAT), jam = true,
            setup = { p -> p.set(KICK, steps(1, 11).map { note(36, it * 120, 120) }).set(SNARE, steps(5, 13).map { note(38, it * 120, 120) }).set(HAT, steps(*EIGHTHS).map { note(42, it * 120, 120) }) },
            steps = listOf(LessonStep("Bar 2 copies bar 1, then breaks the pattern at the very end — a fill.", "Make bar 2: same hats, 1–3 kick changes, a snare fill in beat 4.", Win.RACK, KICK,
                hint = "Copy bar 1 into bar 2, then add 3+ snares on steps 13–16 of bar 2.",
                check = chk@{ p, _ ->
                    fun on(k: ChannelKind, bar: Int) = (0 until 16).filter { p.pattern.channel(k).stepOn(bar * 16 + it) }.toSet()
                    val kickDiff = (on(KICK, 0) union on(KICK, 1)) - (on(KICK, 0) intersect on(KICK, 1))
                    when {
                        on(KICK, 0) != steps(1, 11) || on(SNARE, 0) != steps(5, 13) -> no("Keep bar 1", "Bar 1 should stay as it was.")
                        on(HAT, 1) != on(HAT, 0) -> no("Hats stay the same", "Copy bar 1's hats into bar 2.")
                        kickDiff.size !in 1..3 -> no("Change the kick a little", "Bar 2's kick should differ from bar 1 in 1–3 steps.")
                        4 !in on(SNARE, 1) -> no("Keep the backbeat", "Bar 2 still needs a snare on step 5.")
                        on(SNARE, 1).count { it in 12..15 } < 3 -> no("Add the fill", "Put at least 3 snares on steps 13–16 of bar 2.")
                        else -> ok("Level 3 complete!", "A 2-bar loop with a fill — it turns around like a real drummer.")
                    }
                })), flTip = "Patterns can be any length: set the Channel Rack's pattern length to 32 steps for two bars."),
    )

    private val l4 = listOf(
        Lesson("roll.build-c-major", 4, "Build C major", "A chord = notes that start together. C major = C, E, G.", 2, 96, 1,
            show = listOf(CHORDS), rows = 55..76,
            steps = listOf(
                LessonStep("Stack three notes on one beat: C5, E5, G5. Skip a white key between each.", "Stack C, E and G on beat 1.", Win.ROLL, CHORDS,
                    guides = rollGuide(CHORDS, C.map { Triple(it, 0, 480) }), hint = "C5, then two white keys up (E5), then two more (G5).",
                    check = chk@{ p, _ -> Checks.chordAt(p, CHORDS, 0, pcs(C), "C major") }),
                LessonStep("Again, on beat 3, without the outline.", "Build C major on beat 3.", Win.ROLL, CHORDS, hint = "C–E–G, all starting on beat 3.",
                    check = chk@{ p, _ -> Checks.chordAt(p, CHORDS, 960, pcs(C), "C major") }),
            ), flTip = "FL's Piano roll has a Chord stamp, but building by hand teaches what's inside."),
        Lesson("roll.minor-droop", 4, "Major to minor", "Lower the middle note one semitone: major → minor.", 2, 96, 1,
            show = listOf(CHORDS), rows = 55..76, setup = { it.set(CHORDS, chordNotes(C, 0, 1920)) },
            steps = listOf(
                LessonStep("Hear that bright C major? Move just the middle note, E, down one row to E♭. Same chord, sad colour.", "Make C minor: move E5 down to E♭5.", Win.ROLL, CHORDS,
                    hint = "Select E5 (Select tool) and press Shift ↓, or drag it down one row.", check = chk@{ p, _ -> Checks.chordAt(p, CHORDS, 0, setOf(0, 3, 7), "C minor") }),
                LessonStep("Build A minor on beat 3 from scratch: A, C, E.", "Build A minor on beat 3.", Win.ROLL, CHORDS, setup = { it.clear(CHORDS) },
                    hint = "A4, C5, E5 — all white keys, skip one between each.", check = chk@{ p, _ -> Checks.chordAt(p, CHORDS, 960, setOf(9, 0, 4), "A minor") }),
            ), flTip = "In FL, Shift+↓ moves a selection one semitone."),
        Lesson("roll.two-chords", 4, "Two chords, two bars", "Same shape, new place: move a whole chord.", 3, 96, 2,
            show = listOf(CHORDS), rows = 55..76,
            steps = listOf(LessonStep("Build C for the whole of bar 1, duplicate it, then move the copy up so it becomes F.", "C in bar 1, F in bar 2 — each a whole bar long.", Win.ROLL, CHORDS,
                hint = "Select all three notes → Duplicate (Ctrl B) → Shift ↑ five times.",
                check = chk@{ p, _ ->
                    val a = Checks.chordAt(p, CHORDS, 0, pcs(C), "C"); val b = Checks.chordAt(p, CHORDS, 1920, pcs(F), "F")
                    when { !a.pass -> a; !b.pass -> b
                        notes(p, CHORDS).any { it.durationTicks < 1440 } -> no("Let them ring", "Make each chord last most of its bar.")
                        else -> ok("C to F!", "That move is in thousands of songs.") }
                })), flTip = "In FL's Piano roll, Ctrl+B duplicates the selection to the right."),
        Lesson("roll.four-chord-loop", 4, "The four-chord loop", "A progression: C – G – Am – F.", 4, 96, 4,
            show = listOf(KICK, CLAP, HAT, CHORDS), locked = setOf(KICK, CLAP, HAT), rows = 55..76,
            setup = { beatDrums(it).set(CHORDS, chordNotes(C, 0, 1920)) },
            steps = listOf(
                LessonStep("Bar 1 is C. Add G, A minor and F — one chord per bar. Which one feels like home?", "Build G, Am and F in bars 2–4.", Win.ROLL, CHORDS,
                    hint = "G = G B D. Am = A C E. F = F A C.", check = chk@{ p, _ -> Checks.progression(p, CHORDS, LOOP.map { it.first to pcs(it.second) }) }),
                LessonStep("Swap bar 4 from F to D minor and hear what changes.", "Make bar 4 Dm (D, F, A).", Win.ROLL, CHORDS,
                    hint = "Move each F-chord note to D, F, A.", check = chk@{ p, _ -> Checks.chordAt(p, CHORDS, 3 * 1920, pcs(Dm), "D minor") }),
            ), flTip = "Clone the pattern in FL to keep version A while you try B."),
        Lesson("roll.chord-stabs", 4, "Offbeat stabs", "Chord rhythm: short chords on the \"and\".", 3, 122, 2,
            show = listOf(KICK, CHORDS), locked = setOf(KICK), rows = 55..76, snap = Snap.STEP, setup = { drumsFill(it, KICK, FOUR) },
            steps = listOf(LessonStep("House stabs: short chords between kicks. Am in bar 1, F in bar 2, on steps 3, 7, 11, 15.", "Short Am stabs in bar 1, F stabs in bar 2, on the ands.", Win.ROLL, CHORDS,
                hint = "Build one short Am on step 3, then Duplicate. Keep each stab two steps or less.",
                check = chk@{ p, _ ->
                    for (bar in 0..1) {
                        val want = if (bar == 0) pcs(Am) else pcs(F)
                        val ons = onsetsInBar(p, CHORDS, bar)
                        if (ons != setOf(240, 720, 1200, 1680)) return@chk no("Check the timing", "Bar ${bar + 1}: stabs start on steps 3, 7, 11 and 15 only.")
                        for (o in ons) { val got = barNotes(p, CHORDS, bar).filter { it.startTick % 1920 == o }.map { it.pitch % 12 }.toSet()
                            if (got != want) return@chk no("Wrong chord", "Bar ${bar + 1} uses ${if (bar == 0) "Am (A C E)" else "F (F A C)"}.") }
                    }
                    if (notes(p, CHORDS).any { it.durationTicks > 240 }) no("Shorter!", "Stabs are short: two steps or less.") else ok("Level 4 complete!", "Offbeat stabs — that's a house piano.")
                })), flTip = "Paint tool (B) drags repeated notes; set Snap to Step for stabs."),
    )

    private val l5 = listOf(
        Lesson("rack.hat-accents", 5, "Loud and soft hats", "Velocity: how hard a hit is. Accents make a groove.", 3, 92, 1,
            show = listOf(KICK, SNARE, HAT), locked = setOf(KICK, SNARE),
            setup = { drumsFill(drumsFill(drumsFill(it, KICK, intArrayOf(1, 9)), SNARE, intArrayOf(5, 13)), HAT, (1..16).toList().toIntArray()) },
            steps = listOf(LessonStep("Long-press a step to set how hard it's hit. Loud on 1-and-2-and, soft on the e's and a's.", "Make every even-numbered hat Soft.", Win.RACK, HAT,
                hint = "Long-press step 2 → Soft. Then 4, 6, 8 … 16.",
                check = chk@{ p, _ ->
                    val odd = (0 until 16 step 2).mapNotNull { vel(p, HAT, it) }; val even = (1 until 16 step 2).mapNotNull { vel(p, HAT, it) }
                    when { odd.size + even.size < 16 -> no("Keep all 16 hats", "Don't delete hats — just make some softer.")
                        odd.min() - even.max() < 25 -> no("Bigger difference", "Make the in-between hats clearly softer than the on-beat ones.")
                        else -> ok("Hear the bounce?", "Accents turn a machine into a drummer.") }
                })), flTip = "Open the Graph editor in FL's Channel Rack, or the Piano roll velocity lane."),
        Lesson("rack.ghost-snare", 5, "Ghost notes", "Very quiet snares you feel more than hear.", 3, 90, 1,
            show = listOf(KICK, SNARE, HAT), locked = setOf(KICK, HAT),
            setup = { p -> drumsFill(drumsFill(p, KICK, intArrayOf(1, 8, 11)), HAT, EIGHTHS).set(SNARE, steps(5, 13).map { note(38, it * 120, 120, 110) }) },
            steps = listOf(LessonStep("Boom-bap trick: tiny snares right before or after the big ones. Add two, then make them Soft.", "Add 2+ soft snares near the backbeat (not on a kick).", Win.RACK, SNARE,
                hint = "Try step 4 and step 14, then long-press → Soft.",
                check = chk@{ p, _ ->
                    val sn = (0 until 16).mapNotNull { s -> vel(p, SNARE, s)?.let { s to it } }
                    val loud = sn.filter { it.second >= 90 }.map { it.first }.toSet(); val ghosts = sn.filter { it.second <= 55 }.map { it.first }
                    val kicks = (0 until 16).filter { p.pattern.channel(KICK).stepOn(it) }.toSet()
                    when { loud != steps(5, 13) -> no("Keep the backbeat loud", "Steps 5 and 13 stay loud.")
                        ghosts.size < 2 -> no("Add ghosts", "Add at least two Soft snares.")
                        ghosts.any { it in kicks } -> no("Not on a kick", "Ghosts never sit on a kick step.")
                        ghosts.none { it in steps(4, 6, 12, 14) } -> no("Hug the backbeat", "Put a ghost right next to 5 or 13 (steps 4, 6, 12 or 14).")
                        else -> ok("Ghosted!", "That swing in boom bap? Ghost notes.") }
                })), flTip = "In FL, ghost a step by lowering its velocity in the Graph editor."),
        Lesson("rack.swing", 5, "Swing it", "Swing delays every second 16th: straight → bouncy.", 2, 90, 1,
            show = listOf(KICK, SNARE, HAT), locked = setOf(KICK, SNARE, HAT),
            setup = { p -> drumsFill(drumsFill(p, KICK, intArrayOf(1, 9)), SNARE, intArrayOf(5, 13)).set(HAT, (0 until 16).map { note(42, it * 120, 120, if (it % 2 == 0) 110 else 60) }) },
            steps = listOf(LessonStep("Turn the Swing knob in the Channel Rack header while it plays. Stop when it bounces.", "Set swing between 20% and 70%.", Win.RACK, HAT,
                hint = "Drag the Swing knob up a little at a time.", needsLoops = 1,
                check = chk@{ p, _ -> if (p.pattern.swing in 0.2f..0.7f) ok("Bouncy!", "Swing is why lo-fi and hip-hop feel human.") else no(if (p.pattern.swing < 0.2f) "Still straight" else "Too lopsided", "Aim between 20% and 70% swing.") })),
            flTip = "FL's Swing knob is in the Channel Rack and only moves even steps."),
        Lesson("roll.trap-roll", 5, "Jam: trap half-time + hat roll", "Half-time: clap on beat 3. Rolls: bursts of 32nds.", 4, 140, 2,
            show = listOf(KICK, CLAP, HAT), locked = setOf(KICK), jam = true, rows = 42..42, snap = Snap.HALF_STEP,
            setup = { p -> drumsFill(p, HAT, EIGHTHS).set(KICK, listOf(note(36, 0, 120), note(36, 12 * 120, 120), note(36, (16 + 4) * 120, 120))) },
            steps = listOf(
                LessonStep("Trap feels slow because the clap waits until beat 3. Clap on step 9 in both bars.", "Clap on step 9 of each bar.", Win.RACK, CLAP,
                    hint = "Step 9 = beat 3.", check = chk@{ p, _ -> Checks.drumSteps(p, CLAP, steps(9, bars = 2), "Half-time — now it feels huge.") }),
                LessonStep("Open the Hat in the Piano roll. In bar 2, beat 4, add 3–6 quick hats half a step apart.", "Add one 32nd-note hat roll in bar 2.", Win.ROLL, HAT,
                    hint = "Snap is ½ step. Tap 3–6 times in a row near the end of bar 2.",
                    check = chk@{ p, _ ->
                        val hs = notes(p, HAT).map { it.startTick }.sorted()
                        var best = 1; var run = 1
                        for (i in 1 until hs.size) { val d = hs[i] - hs[i - 1]; run = if (d == 60 || d == 80) run + 1 else 1; best = maxOf(best, run) }
                        if (best >= 3) ok("Level 5 complete!", "One roll per phrase — tasteful trap.") else no("Roll it", "Place at least 3 hats half a step apart.")
                    }),
            ), flTip = "In FL's Piano roll, set Snap to 1/2 step for 32nds; draw a rising velocity line."),
    )

    private val chordBack = { p: Project -> beatDrums(loopChords(p)) }
    private val l6 = listOf(
        Lesson("roll.bass-roots", 6, "Roots under the chords", "The bass plays each chord's root — the note it's named after.", 3, 96, 4,
            show = listOf(KICK, CLAP, HAT, CHORDS, BASS), locked = setOf(KICK, CLAP, HAT, CHORDS), ghost = setOf(CHORDS), rows = 36..52, setup = chordBack,
            steps = listOf(LessonStep("That's me — the bass! I sing the chord's name note, way down low. C, G, A, F — one long note per bar.", "One long root note per bar: C, G, A, F.", Win.ROLL, BASS,
                guides = rollGuide(BASS, listOf(Triple(36, 0, 1920), Triple(43, 1920, 1920), Triple(45, 3840, 1920), Triple(41, 5760, 1920))),
                hint = "C3, G3, A3, F3 — each held for the whole bar.",
                check = chk@{ p, _ ->
                    val want = listOf(0, 7, 9, 5)
                    for (b in 0..3) { val ns = barNotes(p, BASS, b)
                        if (ns.size != 1) return@chk no("One note per bar", "Bar ${b + 1} needs exactly one bass note.")
                        if (ns[0].pitch % 12 != want[b]) return@chk no("Wrong root", "Bar ${b + 1}'s chord is ${listOf("C", "G", "Am", "F")[b]} — its root is ${Pitch.pcName(want[b])}.")
                        if (ns[0].durationTicks < 1440) return@chk no("Hold it", "Let bar ${b + 1}'s bass note ring most of the bar.") }
                    ok("Now it has a floor!", "Bass roots make chords sound solid.")
                })), flTip = "In FL, load a bass instrument (e.g. BooBass) and keep it simple: one note per chord."),
        Lesson("roll.bass-lock-kick", 6, "Lock to the kick", "Bass and kick hit together — one big drum.", 3, 96, 4,
            show = listOf(KICK, CHORDS, BASS), locked = setOf(KICK, CHORDS), ghost = setOf(CHORDS, KICK), rows = 36..52,
            setup = { p -> roots(drumsFill(loopChords(p), KICK, intArrayOf(1, 8, 11)), listOf(0, 7, 9, 5)) },
            steps = listOf(LessonStep("Cut each long bass note into short notes that start with every kick: steps 1, 8 and 11.", "Bass notes on every kick, same pitch all bar.", Win.ROLL, BASS,
                hint = "In each bar: notes at step 1, 8 and 11, all on that bar's root.",
                check = chk@{ p, _ ->
                    val want = listOf(0, 7, 9, 5)
                    for (b in 0..3) {
                        if (onsetsInBar(p, BASS, b) != setOf(0, 7 * 120, 10 * 120)) return@chk no("Match the kick", "Bar ${b + 1}: start bass notes exactly on steps 1, 8 and 11.")
                        if (barNotes(p, BASS, b).any { it.pitch % 12 != want[b] }) return@chk no("Same root", "Keep bar ${b + 1} on ${Pitch.pcName(want[b])}.")
                    }
                    ok("Locked in!", "Kick + bass = one punchy sound.")
                })), flTip = "In FL, Ghost channels show the kick's notes behind the bass."),
        Lesson("roll.octave-bass", 6, "Octave bounce", "Jump between the root and the same note an octave up.", 3, 120, 2,
            show = listOf(CHORDS, BASS), locked = setOf(CHORDS), ghost = setOf(CHORDS), rows = 36..60, snap = Snap.HALF,
            setup = { it.set(CHORDS, chordNotes(Am, 0, 1920) + chordNotes(F, 1920, 1920)) },
            steps = listOf(LessonStep("Disco bass: low, high, low, high — eighth notes. A in bar 1, F in bar 2.", "Low root on the beat, high root on the and.", Win.ROLL, BASS,
                hint = "A2 on beat, A3 on the and, all bar. Then F.",
                check = chk@{ p, _ ->
                    for ((b, pc) in listOf(0 to 9, 1 to 5)) {
                        val ns = barNotes(p, BASS, b)
                        if (ns.map { it.startTick % 1920 }.toSet() != (0 until 8).map { it * 240 }.toSet()) return@chk no("Eighth notes", "Bar ${b + 1}: one note every half beat.")
                        if (ns.any { it.pitch % 12 != pc }) return@chk no("Stay on the root", "Bar ${b + 1} uses ${Pitch.pcName(pc)} only.")
                        val lows = ns.filter { (it.startTick % 480) == 0 }.map { it.pitch }.toSet(); val highs = ns.filter { (it.startTick % 480) == 240 }.map { it.pitch }.toSet()
                        if (lows.size != 1 || highs.size != 1 || highs.first() - lows.first() != 12) return@chk no("Bounce an octave", "Low on the beat, exactly 12 rows higher on the and.")
                    }
                    ok("Bounce!", "That's the disco/house octave bass.")
                })), flTip = "Ctrl+↑ moves selected notes up an octave in FL."),
        Lesson("roll.808-long", 6, "808: long notes in C minor", "An 808 is a tuned bass drum: start on a kick, let it ring.", 4, 140, 2,
            show = listOf(KICK, CLAP, BASS), locked = setOf(KICK, CLAP), ghost = setOf(KICK), rows = 24..48, scale = Scales.C_MINOR,
            setup = { p -> drumsFill(p, CLAP, intArrayOf(9)).set(KICK, listOf(note(36, 0, 120), note(36, 1200, 120), note(36, 1920, 120), note(36, 1920 + 840, 120))) },
            steps = listOf(LessonStep("Headphones help here! Start a long C on the first kick, then 1–3 more notes on later kicks, all on shaded rows.", "Write an 808 line in C minor, starting on C.", Win.ROLL, BASS,
                hint = "Each bass note must start where a kick is. Make at least one note 2 beats long.",
                check = chk@{ p, _ ->
                    val kicks = notes(p, KICK).map { it.startTick }.toSet(); val b = notes(p, BASS)
                    when { b.isEmpty() -> no("Start the 808", "Put a C on the first kick.")
                        b.first().pitch % 12 != 0 -> no("Start on C", "The first 808 note should be a C.")
                        b.any { it.startTick !in kicks } -> no("Ride the kick", "Every 808 note starts on a kick.")
                        b.any { it.pitch % 12 !in Scales.C_MINOR } -> no("Stay in key", "Use the shaded C-minor rows.")
                        b.none { it.durationTicks >= 960 } -> no("Let it ring", "Make at least one note 2 beats long.")
                        b.groupBy { it.startTick }.any { it.value.size > 1 } -> no("One at a time", "808s play one note at a time.")
                        else -> ok("Boom.", "Tuned, long, locked to the kick — that's an 808.") }
                })), flTip = "Set the 808 channel to Mono in FL. Overlapping notes make a glide."),
        Lesson("roll.bass-approach", 6, "Jam: walk into the next chord", "An approach note: a step above or below the next root.", 3, 96, 4,
            show = listOf(CHORDS, BASS), locked = setOf(CHORDS), ghost = setOf(CHORDS), rows = 36..52, jam = true,
            setup = { p -> roots(loopChords(p), listOf(0, 7, 9, 5), 1440) },
            steps = listOf(LessonStep("Beat 4 of bars 1–3 is empty. Add one note that leans into the next root — one or two steps away.", "Add an approach note on beat 4 of bars 1, 2 and 3.", Win.ROLL, BASS,
                hint = "Before G, try F or A. Before A, try G or B. Before F, try E or G.",
                check = chk@{ p, _ ->
                    val next = listOf(43, 45, 41)
                    for (b in 0..2) {
                        val a = notes(p, BASS).firstOrNull { it.startTick == b * 1920 + 1440 } ?: return@chk no("Fill beat 4", "Bar ${b + 1} needs a note on beat 4.")
                        val d = (0..3).map { o -> abs(a.pitch - (next[b] - 12 * o + 24)) }.min()
                        if (d !in 1..2 || a.pitch % 12 !in Scales.C_MAJOR) return@chk no("Closer!", "Bar ${b + 1}'s approach should be 1–2 semitones from ${Pitch.pcName(next[b] % 12)} and in C major.")
                    }
                    ok("Level 6 complete!", "Walking bass — the line pulls the chords along.")
                })), flTip = "In FL, Duplicate bars, then edit only the last note."),
    )

    private val melodyBack = { p: Project -> roots(beatDrums(loopChords(p)), listOf(0, 7, 9, 5)) }
    private val back4 = listOf(KICK, CLAP, HAT, CHORDS, BASS)
    private val l7 = listOf(
        Lesson("roll.chord-tone-on-one", 7, "A chord tone on every 1", "Chord tones — notes inside the current chord — always fit.", 3, 96, 4,
            show = back4 + LEAD, locked = back4.toSet(), ghost = setOf(CHORDS), rows = 60..84, setup = melodyBack,
            steps = listOf(LessonStep("The outlines behind you are the chords. Start each bar's melody on one of them.", "One note on beat 1 of every bar, from that bar's chord.", Win.ROLL, LEAD,
                hint = "Bar 1: C E or G. Bar 2: G B or D. Bar 3: A C or E. Bar 4: F A or C.",
                check = chk@{ p, _ -> melody(p, LEAD, Checks.MelodyRules(minNotes = 4, lowest = 60, highest = 84, noOverlaps = false,
                    chordTonesOnDownbeats = (0..3).associateWith { chordPcsAtBar(p, it) }), "Every bar lands on its chord — instant harmony.") })),
            flTip = "In FL, Piano roll → View → Ghost channels (Alt+V) shows the chords behind the lead."),
        Lesson("roll.steps-and-leaps", 7, "Step, step, leap — and back", "Steps are smooth; leaps are exciting; after a big leap, turn back.", 4, 96, 2,
            show = listOf(KICK, CLAP, HAT, LEAD), locked = setOf(KICK, CLAP, HAT), rows = 59..79, scale = Scales.C_MAJOR, snap = Snap.HALF, setup = { beatDrums(it) },
            steps = listOf(LessonStep("Mostly steps (next shaded row), one bigger leap — and after the leap, go the other way.", "Write 6–10 notes with one leap that turns back.", Win.ROLL, LEAD,
                hint = "Jumped up a 5th? Step DOWN next.",
                check = chk@{ p, _ ->
                    val ns = notes(p, LEAD); val base = melody(p, LEAD, Checks.MelodyRules(minNotes = 6, scale = Scales.C_MAJOR, minStepwise = 0.6f), "")
                    if (!base.pass) return@chk base
                    if (ns.size > 10) return@chk no("A bit long", "Keep it to 10 notes.")
                    val moves = ns.zipWithNext { a, b -> b.pitch - a.pitch }
                    if (moves.none { abs(it) >= 3 }) return@chk no("Add a leap", "Make one jump of 3+ semitones.")
                    val bad = moves.zipWithNext().firstOrNull { (a, b) -> abs(a) >= 6 && (b == 0 || (a > 0) == (b > 0)) }
                    if (bad != null) no("Turn back after the leap", "After a big jump, move the other way — it sounds like a singer.") else ok("That sings!", "Steps with one leap that turns back — the shape of great melodies.")
                })), flTip = "Scale highlighting shows which rows are a step away."),
        Lesson("roll.contour-arch", 7, "Shape: the arch", "Contour: rise to one peak and come back down.", 3, 96, 2,
            show = listOf(LEAD), rows = 59..79, scale = Scales.C_MAJOR, snap = Snap.HALF,
            steps = listOf(LessonStep("Start low, climb to ONE highest note near the middle, then come back down.", "Write an arch: 6–10 notes, one peak in the middle.", Win.ROLL, LEAD,
                hint = "Make one note the top of the hill — and only one.",
                check = chk@{ p, _ ->
                    val ns = notes(p, LEAD).map { it.pitch }
                    if (ns.size !in 6..10) return@chk no("6–10 notes", "Use between 6 and 10 notes.")
                    val peak = ns.max(); val idx = ns.indexOf(peak)
                    when { ns.count { it == peak } > 1 -> no("One peak", "Only one note should be the highest.")
                        idx < ns.size * 0.25 || idx > ns.size * 0.75 -> no("Peak in the middle", "Put the highest note near the middle.")
                        ns.first() > peak - 3 || ns.last() > peak - 3 -> no("Start and end lower", "Begin and end at least 3 semitones below the peak.")
                        ns.max() - ns.min() > 12 -> no("Smaller range", "Keep it within one octave.")
                        ns.any { it % 12 !in Scales.C_MAJOR } -> no("Stay in C major", "Use shaded rows.")
                        else -> ok("Beautiful arch!", "Tension rises, then releases.") }
                })), flTip = "Zoom out in FL (Ctrl+scroll) to see the whole shape."),
        Lesson("roll.rhythm-first", 7, "Rhythm first, then pitch", "Melody = rhythm + pitch. Keep a good rhythm, change only the notes.", 3, 96, 2,
            show = back4 + LEAD, locked = back4.toSet(), ghost = setOf(CHORDS), rows = 59..79, scale = Scales.C_MAJOR,
            setup = { p -> melodyBack(p).set(LEAD, listOf(0 to 480, 480 to 240, 720 to 240, 960 to 480, 1920 to 240, 2160 to 240, 2400 to 960).map { (t, d) -> note(60, t, d) }) },
            steps = listOf(LessonStep("The rhythm is already good. Drag each note up or down only — don't move it left or right.", "Give the rhythm a tune; start each bar on a chord tone.", Win.ROLL, LEAD,
                hint = "Bar 1 is C (C E G), bar 2 is G (G B D).",
                check = chk@{ p, _ ->
                    val ref = listOf(0, 480, 720, 960, 1920, 2160, 2400)
                    val ns = notes(p, LEAD)
                    when { ns.map { it.startTick } != ref -> no("Keep the rhythm", "Only drag notes up or down — their timing stays.")
                        ns.map { it.pitch }.toSet().size < 3 -> no("More pitches", "Use at least 3 different notes.")
                        ns.any { it.pitch % 12 !in Scales.C_MAJOR } -> no("Stay in key", "Use the shaded rows.")
                        ns[0].pitch % 12 !in pcs(C) || ns[4].pitch % 12 !in pcs(G) -> no("Lean on the chord", "Start bar 1 on C/E/G and bar 2 on G/B/D.")
                        else -> ok("A real tune!", "Good rhythm + chord tones = melody.") }
                })), flTip = "In FL, Shift+↑/↓ moves selected notes in pitch without changing timing."),
        Lesson("roll.land-home", 7, "Jam: land at home", "A phrase ends by landing on the home note.", 3, 96, 2,
            show = listOf(CHORDS, LEAD), locked = setOf(CHORDS), ghost = setOf(CHORDS), rows = 59..79, jam = true,
            setup = { it.set(CHORDS, chordNotes(G, 0, 1920) + chordNotes(C, 1920, 1920)) },
            steps = listOf(LessonStep("G wants to go home to C. Write 4–10 notes, leave a little silence, and land on a long C in bar 2.", "End on a C in bar 2 (beat 1 or 3), at least a beat long.", Win.ROLL, LEAD,
                hint = "B wants to go up to C — try B → C across the bar line.",
                check = chk@{ p, _ ->
                    val base = melody(p, LEAD, Checks.MelodyRules(minNotes = 4, scale = Scales.C_MAJOR, minStepwise = 0.5f, endsOnPc = 0, needsRest = true), "")
                    val last = notes(p, LEAD).lastOrNull()
                    when { !base.pass -> base
                        last == null || last.startTick !in setOf(1920, 2880) || last.durationTicks < 480 -> no("Land on a strong beat", "End with a C on beat 1 or 3 of bar 2, at least one beat long.")
                        notes(p, LEAD).size > 10 -> no("Fewer notes", "10 notes max.")
                        else -> ok("Level 7 complete!", "You can write a line that goes somewhere and comes home.") }
                })), flTip = "End phrases with a longer note and some space so the loop breathes."),
    )

    private val l8 = listOf(
        Lesson("roll.motif-repeat", 8, "Make a motif, repeat it", "A motif is a short idea; repeating it makes it catchy.", 3, 96, 2,
            show = listOf(CHORDS, LEAD), locked = setOf(CHORDS), ghost = setOf(CHORDS), rows = 59..79, scale = Scales.C_MAJOR, snap = Snap.HALF,
            setup = { it.set(CHORDS, chordNotes(C, 0, 1920) + chordNotes(G, 1920, 1920)) },
            steps = listOf(LessonStep("Write 3–5 notes in bar 1 with a gap, then copy them exactly into bar 2. Don't change it — repetition is the point.", "Motif in bar 1, exact copy in bar 2.", Win.ROLL, LEAD,
                hint = "Select bar 1's notes → Duplicate (Ctrl B).",
                check = chk@{ p, _ ->
                    val b1 = barNotes(p, LEAD, 0); val b2 = barNotes(p, LEAD, 1)
                    when { b1.size !in 3..5 -> no("3–5 notes", "Bar 1 needs 3 to 5 notes.")
                        !hasRest(p, LEAD, 240, 0, 0) -> no("Leave a gap", "Leave half a beat of silence in bar 1.")
                        b2.map { it.pitch to it.startTick % 1920 } != b1.map { it.pitch to it.startTick } -> no("Exact copy", "Bar 2 should repeat bar 1 exactly.")
                        b1.any { it.pitch % 12 !in Scales.C_MAJOR } -> no("Stay in key", "Use shaded rows.")
                        else -> ok("Catchy!", "Say it twice and people remember it.") }
                })), flTip = "In FL, select bar 1 (E tool) then Ctrl+B."),
        Lesson("roll.same-rhythm-new-ending", 8, "Same rhythm, different ending", "Variation: keep the rhythm, change one note.", 3, 96, 2,
            show = listOf(LEAD), rows = 59..79,
            setup = { p -> p.set(LEAD, listOf(60 to 0, 64 to 480, 62 to 960).flatMap { (pp, t) -> listOf(note(pp, t, 480), note(pp, t + 1920, 480)) }) },
            steps = listOf(LessonStep("Both bars are the same. Keep the three attacks and the gap — give bar 2 a different LAST note.", "Change only the last note of bar 2.", Win.ROLL, LEAD,
                hint = "Drag bar 2's third note (beat 3) up or down. Don't move it sideways.",
                check = chk@{ p, _ ->
                    val b1 = barNotes(p, LEAD, 0); val b2 = barNotes(p, LEAD, 1)
                    when { b2.map { it.startTick % 1920 } != b1.map { it.startTick } -> no("Keep the rhythm", "Bar 2 needs the same three attacks on beats 1, 2, 3.")
                        b2.dropLast(1).map { it.pitch } != b1.dropLast(1).map { it.pitch } -> no("Only the ending", "Keep the first two notes of bar 2 the same.")
                        b2.last().pitch == b1.last().pitch -> no("Change the ending", "Give bar 2's last note a different pitch.")
                        else -> ok("Variation!", "Same rhythm, new ending — that's how hooks evolve.") }
                })), flTip = "Clone the pattern in FL before you vary it, so you keep both."),
        Lesson("roll.question-answer", 8, "Question and answer", "The question stops unfinished; the answer starts the same way and lands home.", 4, 96, 4,
            show = listOf(CHORDS, LEAD), locked = setOf(CHORDS), ghost = setOf(CHORDS), rows = 59..79, scale = Scales.C_MAJOR, snap = Snap.HALF,
            setup = { it.set(CHORDS, listOf(C, G, F, C).flatMapIndexed { i, ch -> chordNotes(ch, i * 1920, 1920) }) },
            steps = listOf(LessonStep("Bars 1–2 ask (end on D, G or B). Bars 3–4 answer: start like bar 1, end on C.", "Write a question (bars 1–2) and an answer (bars 3–4).", Win.ROLL, LEAD,
                hint = "Copy bar 1's rhythm into bar 3. End bar 4 on C.",
                check = chk@{ p, _ ->
                    val q = notes(p, LEAD).filter { it.startTick < 3840 }; val a = notes(p, LEAD).filter { it.startTick >= 3840 }
                    when { q.isEmpty() || a.isEmpty() -> no("Both halves", "Write notes in bars 1–2 and in bars 3–4.")
                        q.last().pitch % 12 == 0 -> no("Leave the question open", "End bars 1–2 on D, G or B — not C.")
                        a.last().pitch % 12 != 0 || a.last().startTick < 5760 -> no("Answer at home", "End bar 4 on a C.")
                        rhythmSimilarity(onsetsInBar(p, LEAD, 0), onsetsInBar(p, LEAD, 2)) < 0.75f -> no("Echo the start", "Bar 3 should start with the same rhythm as bar 1.")
                        !hasRest(p, LEAD, 240, 0, 1) -> no("Breathe", "Leave a gap before the answer.")
                        notes(p, LEAD).any { it.pitch % 12 !in Scales.C_MAJOR } -> no("Stay in key", "Use shaded rows.")
                        else -> ok("Call and response!", "Your melody has a conversation.") }
                })), flTip = "Clone the pattern in FL to try a second answer."),
        Lesson("roll.sequence", 8, "Move the motif up a step", "A sequence: the same motif, one scale step higher.", 3, 96, 2,
            show = listOf(LEAD), locked = emptySet(), rows = 59..79, scale = Scales.C_MAJOR,
            setup = { p -> p.set(LEAD, listOf(note(60, 0, 480), note(62, 480, 240), note(64, 720, 240))) },
            steps = listOf(LessonStep("Copy bar 1 into bar 2 and move it up ONE shaded row — scale steps, not semitones.", "Bar 2 = bar 1 moved up one scale step (D E F).", Win.ROLL, LEAD,
                hint = "C→D, D→E, E→F (not F#).",
                check = chk@{ p, _ ->
                    val b2 = barNotes(p, LEAD, 1)
                    when { b2.map { it.startTick % 1920 } != listOf(0, 480, 720) -> no("Same rhythm", "Bar 2 needs the same rhythm as bar 1.")
                        b2.map { it.pitch } != listOf(62, 64, 65) -> no("One scale step", "Bar 2 should be D5, E5, F5.")
                        else -> ok("Sequence!", "Same idea, higher — music uses this everywhere.") }
                })), flTip = "With Snap to scale on in FL, moved notes snap to scale notes."),
        Lesson("roll.jam-four-bar-melody", 8, "Jam: four-bar melody", "Chord tones on strong beats, passing notes between, space, home ending.", 5, 96, 4,
            show = back4 + LEAD, locked = back4.toSet(), ghost = setOf(CHORDS), rows = 59..84, jam = true, snap = Snap.HALF, setup = melodyBack,
            steps = listOf(LessonStep("Everything together. Chord tones on beats 1 and 3, something in between, breathe twice, end on C.", "Write a 4-bar melody over the loop.", Win.ROLL, LEAD,
                hint = "Check the outlines on beats 1 and 3. Leave a gap in bars 1–2 and in bars 3–4.",
                check = chk@{ p, _ ->
                    val ns = notes(p, LEAD)
                    when { ns.size < 6 -> no("More notes", "Write at least 6 notes.")
                        strongBeatChordToneShare(p, LEAD) < 0.85f -> no("Lean on the chords", "Notes on beats 1 and 3 should be chord tones.")
                        (0..3).any { barNotes(p, LEAD, it).size > 8 } -> no("Less crowded", "8 notes per bar max.")
                        !hasRest(p, LEAD, 240, 0, 1) || !hasRest(p, LEAD, 240, 2, 3) -> no("Breathe", "Leave a gap in bars 1–2 and in bars 3–4.")
                        ns.last().pitch % 12 != 0 -> no("Come home", "End on C.")
                        else -> ok("Level 8 complete!", "That's a real 4-bar melody. Export it and play it in FL!") }
                })), flTip = "Export this as MIDI and import it into FL."),
    )

    private fun voiceCost(a: List<Int>, b: List<Int>) = a.sorted().zip(b.sorted()).sumOf { (x, y) -> abs(x - y) }
    private val l9 = listOf(
        Lesson("roll.inversions", 9, "Flip the chord", "An inversion puts a different chord note at the bottom.", 3, 90, 1,
            show = listOf(CHORDS), rows = 55..79,
            steps = listOf(LessonStep("C–E–G on beat 1. Beat 2: lift C up an octave (E–G–C). Beat 3: lift E too (G–C–E).", "Three versions of C: root, 1st and 2nd inversion.", Win.ROLL, CHORDS,
                hint = "Same three notes; only the lowest note changes.",
                check = chk@{ p, _ ->
                    listOf(Triple(0, 0, "root position"), Triple(480, 4, "1st inversion"), Triple(960, 7, "2nd inversion")).forEach { (t, bass, name) ->
                        val v = Checks.chordAt(p, CHORDS, t, pcs(C), "C ($name)", bassPc = bass); if (!v.pass) return@chk v }
                    ok("Flipped!", "Same chord, three colours.")
                })), flTip = "Select the lowest note in FL and press Ctrl+↑."),
        Lesson("roll.voice-leading", 9, "Smooth moves", "Voice leading: move each note to the nearest note of the next chord.", 4, 96, 4,
            show = listOf(CHORDS), rows = 52..79, setup = { loopChords(it) },
            steps = listOf(LessonStep("Hear how jumpy that is? Re-voice G, Am and F so each note moves as little as possible.", "Keep C–G–Am–F but make the moves small.", Win.ROLL, CHORDS,
                hint = "G as B–D–G, Am as C–E–A, F as C–F–A.",
                check = chk@{ p, _ ->
                    val prog = Checks.progression(p, CHORDS, LOOP.map { it.first to pcs(it.second) }); if (!prog.pass) return@chk prog
                    val v = (0..3).map { b -> notes(p, CHORDS).filter { it.startTick == b * 1920 }.map { it.pitch } }
                    if (v.any { it.size != 3 }) return@chk no("Three notes each", "Each chord keeps exactly 3 notes.")
                    val costs = v.zipWithNext { a, b -> voiceCost(a, b) }
                    if (costs.any { it > 5 } || costs.sum() > 10) no("Still jumpy", "Total movement is ${costs.sum()} semitones. Keep shared notes still; move others by 1–2.") else ok("Silky!", "Total movement: ${costs.sum()} semitones — pro voicing.")
                })), flTip = "In FL, stamp a chord then move single notes by octave."),
        Lesson("roll.sevenths-ii-v-i", 9, "Seventh chords: ii–V–I", "Add one more note on top for jazzy 7th chords.", 4, 82, 4,
            show = listOf(CHORDS), rows = 52..79,
            steps = listOf(LessonStep("Stack one more skip-a-note on top: Dm7 (D F A C), G7 (G B D F), Cmaj7 (C E G B).", "Dm7, G7, Cmaj7 in bars 1–3; C or Cmaj7 in bar 4.", Win.ROLL, CHORDS,
                hint = "Build the triad, then add the note two white keys above its top.",
                check = chk@{ p, _ ->
                    listOf(Triple(0, Scales.seventh(2, "m7"), "Dm7"), Triple(1920, Scales.seventh(7, "7"), "G7"), Triple(3840, Scales.seventh(0, "maj7"), "Cmaj7")).forEach { (t, s, nm) ->
                        val v = Checks.chordAt(p, CHORDS, t, s, nm); if (!v.pass) return@chk v }
                    val last = chordPcsAtBar(p, 3)
                    if (last != pcs(C) && last != Scales.seventh(0, "maj7")) no("Resolve", "Bar 4: C or Cmaj7.") else ok("Smooth jazz!", "ii–V–I: the most important progression in jazz and lo-fi.")
                })), flTip = "FL's Chord stamp has min7, 7 and maj7 — compare with yours."),
        Lesson("roll.relative-minor", 9, "Same chords, new home: A minor", "Start the loop on Am and A becomes home.", 4, 90, 4,
            show = listOf(CHORDS, BASS), rows = 36..79,
            steps = listOf(LessonStep("Am – F – C – G, then bass roots A, F, C, G. Which chord feels like home now?", "Build Am–F–C–G and their bass roots.", Win.ROLL, CHORDS,
                hint = "The same chords as before, in a new order.",
                check = chk@{ p, _ ->
                    val prog = Checks.progression(p, CHORDS, listOf("Am" to pcs(Am), "F" to pcs(F), "C" to pcs(C), "G" to pcs(G))); if (!prog.pass) return@chk prog
                    val want = listOf(9, 5, 0, 7)
                    for (b in 0..3) if (notes(p, BASS).none { it.startTick == b * 1920 && it.pitch % 12 == want[b] }) return@chk no("Add the bass", "Bar ${b + 1} needs a bass ${Pitch.pcName(want[b])} on beat 1 (switch to Bass).")
                    ok("Minor mood!", "Same notes as C major — but A is home now.")
                })), flTip = "In FL's scale menu, A minor highlights the same white keys as C major."),
        Lesson("roll.dark-trap-melody", 9, "Jam: dark melody in C minor", "A minor key with black keys; trap melodies breathe.", 4, 140, 2,
            show = listOf(KICK, CLAP, HAT, CHORDS, LEAD), locked = setOf(KICK, CLAP, HAT, CHORDS), ghost = setOf(CHORDS), rows = 59..79, jam = true, snap = Snap.HALF,
            setup = { p -> drumsFill(drumsFill(drumsFill(p, KICK, intArrayOf(1, 11)), CLAP, intArrayOf(9)), HAT, EIGHTHS).set(CHORDS, chordNotes(listOf(60, 63, 67), 0, 1920) + chordNotes(listOf(56, 60, 63), 1920, 1920)) },
            steps = listOf(LessonStep("C minor uses E♭, A♭ and B♭. Few notes, lots of space, end on C or G.", "Write a dark melody in C minor.", Win.ROLL, LEAD,
                hint = "If it sounds sunny, a natural E, A or B crept in.",
                check = chk@{ p, _ ->
                    val ns = notes(p, LEAD)
                    when { ns.size < 3 -> no("More notes", "Write at least 3 notes.")
                        ns.any { it.pitch % 12 !in Scales.C_MINOR } -> no("Stay dark", "Use C minor: C D E♭ F G A♭ B♭.")
                        (0..1).any { barNotes(p, LEAD, it).size > 6 } -> no("Breathe more", "6 notes per bar max.")
                        !hasRest(p, LEAD, 480) -> no("Leave space", "Leave a full beat of silence somewhere.")
                        ns.last().pitch % 12 !in setOf(0, 7) -> no("Resolve", "End on C or G.")
                        else -> ok("Level 9 complete!", "Dark, spacious, in key — that's a trap melody.") }
                })), flTip = "In FL: Snap to scale → Minor, root C."),
    )

    private val l10 = listOf(
        Lesson("roll.passing-tones", 10, "Passing tones", "A passing tone fills a gap with the note in between, off the beat.", 3, 96, 2,
            show = listOf(CHORDS, LEAD), locked = setOf(CHORDS), ghost = setOf(CHORDS), rows = 59..79, snap = Snap.HALF,
            setup = { p -> p.set(CHORDS, chordNotes(C, 0, 1920) + chordNotes(G, 1920, 1920)).set(LEAD, listOf(60, 64, 67, 64, 62, 59, 62, 67).mapIndexed { i, pp -> note(pp, i * 480, 240) }) },
            steps = listOf(LessonStep("C to E skips D. Add D on the \"and\" between them. Find one more gap and fill it.", "Add 2+ passing tones on the ands.", Win.ROLL, LEAD,
                hint = "Only gaps of 3–4 semitones have a note in between.",
                check = chk@{ p, _ ->
                    val ns = notes(p, LEAD)
                    if ((0 until 8).any { i -> ns.none { it.startTick == i * 480 } }) return@chk no("Keep the beat notes", "Don't remove the notes on the beats.")
                    var count = 0
                    for (i in 1 until ns.size - 1) { val a = ns[i - 1]; val x = ns[i]; val b = ns[i + 1]
                        val chord = chordPcsAtBar(p, x.startTick / 1920)
                        if (x.startTick % 480 != 0 && x.pitch % 12 !in chord && (x.pitch - a.pitch) * (b.pitch - x.pitch) > 0 && abs(x.pitch - a.pitch) <= 2 && abs(b.pitch - x.pitch) <= 2) count++ }
                    if (count >= 2) ok("Smooth!", "Passing tones glue chord tones together.") else no("Find another gap", "You have $count passing tone(s). Add one between two notes 3–4 semitones apart.")
                })), flTip = "Set FL's Snap to ½ beat before adding in-between notes."),
        Lesson("roll.counter-melody", 10, "Counter-melody", "A second line answers in the gaps, lower than the lead.", 4, 96, 4,
            show = listOf(CHORDS, LEAD, LEAD2), locked = setOf(CHORDS, LEAD), ghost = setOf(LEAD, CHORDS), rows = 52..76, scale = Scales.C_MAJOR,
            setup = { p -> loopChords(p).set(LEAD, listOf(67 to 0, 69 to 480, 67 to 960, 72 to 1920, 71 to 2400, 67 to 3840, 69 to 4320, 67 to 4800, 64 to 5760, 60 to 6240).map { (pp, t) -> note(pp, t, 480) }) },
            steps = listOf(LessonStep("Talk when the lead is quiet — not over it. Write Lead 2 in its gaps, lower down.", "Write 4+ Lead 2 notes, mostly in the gaps, below the lead.", Win.ROLL, LEAD2,
                hint = "The lead rests in the second half of bars 2 and 4.",
                check = chk@{ p, _ ->
                    val l2 = notes(p, LEAD2); val l1 = notes(p, LEAD)
                    fun leadSounding(t: Int) = l1.any { t >= it.startTick && t < it.startTick + it.durationTicks && t == it.startTick }
                    when { l2.size < 4 -> no("More notes", "Write at least 4 notes for Lead 2.")
                        l2.count { !leadSounding(it.startTick) }.toFloat() / l2.size < 0.7f -> no("Answer in the gaps", "Most Lead 2 notes should start where the lead isn't starting a note.")
                        l2.map { it.pitch }.sorted()[l2.size / 2] > l1.map { it.pitch }.sorted()[l1.size / 2] - 3 -> no("Go lower", "Keep Lead 2 a few notes below the lead.")
                        l2.any { it.pitch % 12 !in Scales.C_MAJOR } -> no("Stay in key", "Use shaded rows.")
                        else -> ok("Two voices!", "Counter-melodies make a track feel full.") }
                })), flTip = "In FL, add a second instrument channel and turn on Ghost channels."),
        Lesson("roll.melody-dynamics", 10, "Accent the hook", "Velocity in a melody: strong beats louder, in-between notes softer.", 3, 96, 2,
            show = listOf(LEAD), rows = 59..79,
            setup = { p -> p.set(LEAD, listOf(60 to 0, 62 to 240, 64 to 480, 67 to 960, 64 to 1200, 62 to 1440, 60 to 1920, 64 to 2400, 62 to 2880).map { (pp, t) -> note(pp, t, 240, 100) }) },
            steps = listOf(LessonStep("Select a note and use the velocity buttons. Beats 1 and 3 loud, the others softer — but never silent.", "Make beat-1/beat-3 notes 20+ louder than the rest.", Win.ROLL, LEAD,
                hint = "Select the in-between notes → Soft.",
                check = chk@{ p, _ ->
                    val ns = notes(p, LEAD); val strong = ns.filter { it.startTick % 960 == 0 }; val weak = ns - strong.toSet()
                    when { ns.any { it.velocity < 30 } -> no("Too quiet", "Keep every note above 30 so it's heard.")
                        strong.map { it.velocity }.average() - weak.map { it.velocity }.average() < 20 -> no("More contrast", "Strong-beat notes should be clearly louder.")
                        else -> ok("It breathes!", "Dynamics make a melody sound played, not typed.") }
                })), flTip = "In FL's Piano roll event editor, drag a line across velocities."),
        Lesson("pattern.intro-build-drop", 10, "Intro, build, drop", "Arrangement by subtraction: remove layers, then bring them back.", 4, 120, 2,
            show = listOf(KICK, SNARE, HAT, CHORDS, BASS), rows = 36..76,
            setup = { p -> roots(drumsFill(drumsFill(drumsFill(loopChords(p, LOOP.take(2)), KICK, FOUR), SNARE, intArrayOf(5, 13)), HAT, EIGHTHS), listOf(0, 7)) },
            steps = listOf(
                LessonStep("Intro first: tap the green lights to mute Kick and Bass. Hear how it waits?", "Mute Kick and Bass.", Win.RACK, KICK, hint = "Tap the LED left of each channel name.",
                    check = chk@{ p, _ -> if (p.pattern.channel(KICK).muted && p.pattern.channel(BASS).muted) ok("Intro!", "Less is more at the start.") else no("Mute both", "Kick and Bass LEDs should be off.") }),
                LessonStep("Build: fill bar 2's hats with sixteenths and add a snare roll in its last half.", "16th hats in bar 2; 4+ snares on steps 9–16 of bar 2.", Win.RACK, HAT, hint = "Bar 2 = steps 17–32.",
                    check = chk@{ p, _ ->
                        val hats = (16 until 32).count { p.pattern.channel(HAT).stepOn(it) }; val sn = (24 until 32).count { p.pattern.channel(SNARE).stepOn(it) }
                        if (hats >= 12 && sn >= 4) ok("Tension!", "Busier hats + a snare roll = a build.") else no("Build more", "Bar 2 needs 12+ hats and 4+ snares on its last 8 steps.") }),
                LessonStep("Drop! Unmute Kick and Bass.", "Bring Kick and Bass back.", Win.RACK, KICK, hint = "Tap their LEDs again.",
                    check = chk@{ p, _ -> if (!p.pattern.channel(KICK).muted && !p.pattern.channel(BASS).muted) ok("DROP!", "It hits harder because it was missing.") else no("Unmute", "Turn Kick and Bass back on.") }),
            ), flTip = "In FL, place Patterns in the Playlist (F5) and switch PAT → SONG to arrange."),
        Lesson("capstone.hook", 10, "Capstone: your four-bar hook", "Everything you know: drums, chords, bass and a hook.", 10, 96, 4,
            show = listOf(KICK, SNARE, CLAP, HAT, CHORDS, BASS, LEAD), rows = 36..84, jam = true,
            steps = listOf(LessonStep("Your track, your choices. I'll only check the essentials. Show me what you've got!", "Make a 4-bar hook: drums, chords, bass, lead.", Win.RACK, KICK,
                hint = "Kick on 1 every bar · backbeat · chords on beat 1 · bass roots · a lead that repeats with a twist and ends home.",
                check = chk@{ p, _ ->
                    for (b in 0..3) {
                        if (!p.pattern.channel(KICK).stepOn(b * 16)) return@chk no("Drums", "Bar ${b + 1}: kick on step 1.")
                        val back = (p.pattern.channel(SNARE).stepOn(b * 16 + 4) && p.pattern.channel(SNARE).stepOn(b * 16 + 12)) || (p.pattern.channel(CLAP).stepOn(b * 16 + 4) && p.pattern.channel(CLAP).stepOn(b * 16 + 12)) || p.pattern.channel(CLAP).stepOn(b * 16 + 8)
                        if (!back) return@chk no("Backbeat", "Bar ${b + 1}: snare or clap on 2 and 4 (or clap on 3 for half-time).")
                        val ch = chordPcsAtBar(p, b)
                        if (ch.size < 3 || !ch.all { it in Scales.C_MAJOR }) return@chk no("Chords", "Bar ${b + 1}: a C-major-key chord starting on beat 1.")
                        val root = rootPcAtBar(p, b)
                        if (notes(p, BASS).none { it.startTick == b * 1920 && it.pitch % 12 == root }) return@chk no("Bass", "Bar ${b + 1}: bass plays the chord root on beat 1.")
                    }
                    val lead = notes(p, LEAD)
                    when { lead.size < 6 -> no("Lead", "Write a hook with at least 6 notes.")
                        rhythmSimilarity(onsetsInBar(p, LEAD, 0) + onsetsInBar(p, LEAD, 1).map { it + 1920 }, onsetsInBar(p, LEAD, 2) + onsetsInBar(p, LEAD, 3).map { it + 1920 }) < 0.75f -> no("Repeat the hook", "Bars 3–4 should echo the rhythm of bars 1–2.")
                        barNotes(p, LEAD, 0).map { it.pitch } + barNotes(p, LEAD, 1).map { it.pitch } == barNotes(p, LEAD, 2).map { it.pitch } + barNotes(p, LEAD, 3).map { it.pitch } -> no("Add a twist", "Change something in bars 3–4.")
                        strongBeatChordToneShare(p, LEAD) < 0.75f -> no("Lean on chords", "Lead notes on beats 1 and 3 should be chord tones.")
                        lead.last().pitch % 12 != 0 -> no("Come home", "End the lead on C.")
                        else -> ok("You made a track!", "Drums, harmony, bass and a hook. Export the MIDI and rebuild it in FL Studio.") }
                })), flTip = "Export MIDI, import each channel into FL, then rebuild it from memory in a new Pattern."),
    )

    val lessons: List<Lesson> = l1 + l2 + l3 + l4 + l5 + l6 + l7 + l8 + l9 + l10 + TheoryCurriculum.lessons
    fun byId(id: String?) = lessons.firstOrNull { it.id == id }
    fun inLevel(level: Int) = lessons.filter { it.level == level }
}
