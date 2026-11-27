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
        want.firstOrNull { it !in got }?.let { (pitch, tick) -> return no("Almost", "Put ${Pitch.flName(pitch)} at ${Checks.posName(tick)}.") }
        (got - want.toSet()).firstOrNull()?.let { (pitch, tick) -> return no("One extra note", "${Pitch.flName(pitch)} at ${Checks.posName(tick)} isn't part of this. Use the Delete tool to remove it.") }
        return ok("Spot on!", success)
    }

    private fun rhythmOf(p: Project, kind: ChannelKind) = notes(p, kind).map { it.startTick to it.durationTicks }

    private fun rhythmCheck(p: Project, kind: ChannelKind, ref: List<Pair<Int, Int>>, durTol: Int, success: String): Verdict {
        val got = rhythmOf(p, kind)
        ref.forEach { (on, d) ->
            val m = got.firstOrNull { it.first == on } ?: return no("A note is missing", "Start a note at ${Checks.posName(on)}.")
            if (abs(m.second - d) > durTol) return no(if (m.second > d) "Too long" else "Too short",
                "The note at ${Checks.posName(on)} should last ${beatsName(d)}. Drag its right edge.")
        }
        got.firstOrNull { g -> ref.none { it.first == g.first } }?.let { return no("One extra note", "Nothing should start at ${Checks.posName(it.first)}. That spot is a rest.") }
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
        Level(1, "First beat", "Make a drum beat with kick, clap and hi-hats.", Track.BEATS),
        Level(2, "First notes", "Draw notes in the Piano roll: up, down, long and short.", Track.NOTES),
        Level(3, "Groove builder", "Fast hats, surprise kicks, open hats and a drum fill.", Track.BEATS),
        Level(4, "Chords", "Stack 3 notes into chords and play the famous four-chord loop.", Track.HARMONY),
        Level(5, "Feel", "Soft and loud hits, swing, and fast trap hat rolls.", Track.BEATS),
        Level(6, "Bass and 808", "Low notes that hold up the song and hit with the kick.", Track.BASS),
        Level(7, "Melody I: lines", "Tunes that fit the chords, have a shape and come home.", Track.MELODY),
        Level(8, "Melody II: motifs", "Make a tiny tune idea, repeat it, change it, answer it.", Track.MELODY),
        Level(9, "Harmony II", "Flip chords, move them smoothly, add a 4th note, try minor.", Track.HARMONY),
        Level(10, "Make it a track", "Fill gaps, add a second tune, build a drop, make your hook.", Track.ARRANGE),
    ) + TheoryCurriculum.levels + PacksCurriculum.levels

    private val l1 = listOf(
        Lesson("rack.four-on-floor", 1, "Four on the floor", "A beat is a steady pulse, like a heartbeat. 4 beats make 1 bar.", 2, 90, 1,
            show = listOf(KICK), steps = listOf(
                drumStep("Hi, I'm Lou! This is the Channel Rack. Each row is one drum. The kick is the big boom, like a heartbeat.", "Tap the glowing square on the Kick row.", KICK, steps(*FOUR),
                    "16 steps make 1 bar. Each beat starts a group of 4 steps. Tap steps 1, 5, 9 and 13.", "A kick on every beat. Boom, boom, boom, boom. Feel the heartbeat?"),
                drumStep("Same beat, but faster: 120 BPM. BPM means beats per minute. No glowing help this time.", "Put a kick on every beat by yourself.", KICK, steps(*FOUR),
                    "Steps 1, 5, 9 and 13. That's the first step of each group of 4.", "Same pattern, faster tempo. The heartbeat just sped up!", guided = false,
                    setup = { it.clear(KICK).bpm(120) }),
            ), flTip = "F6 opens the Channel Rack. Left-click a step to turn it on, right-click to turn it off, Space to play."),
        Lesson("rack.backbeat", 1, "Clap on 2 and 4", "The clap lands on beats 2 and 4. This is called the backbeat.", 2, 95, 1,
            show = listOf(KICK, CLAP), locked = setOf(KICK), setup = { drumsFill(it, KICK, FOUR) },
            steps = listOf(
                drumStep("Count with me: ONE, two, THREE, four. The clap answers on 2 and 4. Clap along!", "Put a clap on beat 2 and beat 4.", CLAP, steps(5, 13),
                    "Beat 2 starts on step 5. Beat 4 starts on step 13.", "That's the backbeat. Almost every pop and dance song has it."),
                drumStep("A bit faster now, and no glowing help.", "Clap on beats 2 and 4 again, at 124 BPM.", CLAP, steps(5, 13), "Steps 5 and 13.", "Locked in! Boom, clap, boom, clap.", guided = false,
                    setup = { it.clear(CLAP).bpm(124) }),
            ), flTip = "This kick + clap is FL's classic house starting point. Clone a channel with Alt+C to try another clap sound."),
        Lesson("rack.eighth-hats", 1, "Hats in between", "Hi-hats tick twice per beat: on the beat and on the \"and\" in between.", 2, 95, 1,
            show = listOf(KICK, CLAP, HAT), locked = setOf(KICK, CLAP), setup = { drumsFill(drumsFill(it, KICK, FOUR), CLAP, intArrayOf(5, 13)) },
            steps = listOf(
                drumStep("Hi-hats go tss, tss and keep time. Count \"1 and 2 and\". Every other square gets a hat.", "Fill the Hat row: tap one, skip one.", HAT, steps(*EIGHTHS),
                    "Tap one, skip one: steps 1, 3, 5, 7, 9, 11, 13 and 15.", "Two hats per beat. Now the groove has a motor!"),
                drumStep("Now at 110 BPM, from memory.", "Tap one, skip one on the Hat row.", HAT, steps(*EIGHTHS), "All the odd steps: 1, 3, 5, 7, 9, 11, 13 and 15.", "Smooth. You remembered!", guided = false,
                    setup = { it.clear(HAT).bpm(110) }),
            ), flTip = "In FL, right-click a channel name → Fill each 2 steps does this in one click."),
        Lesson("rack.offbeat-hat", 1, "House hats on the \"and\"", "Offbeat hats play only in between the kicks.", 2, 124, 1,
            show = listOf(KICK, CLAP, HAT), locked = setOf(KICK, CLAP),
            setup = { drumsFill(drumsFill(drumsFill(it, KICK, FOUR), CLAP, intArrayOf(5, 13)), HAT, EIGHTHS) },
            steps = listOf(
                drumStep("House music puts the hat between the kicks. Boom tss, boom tss. Let's remove the hats that play with a kick.", "Turn off the hats that play with the kick.", HAT, steps(3, 7, 11, 15),
                    "Tap steps 1, 5, 9 and 13 on the Hat row to turn them off. Keep 3, 7, 11 and 15.", "Offbeat hats. Instant house music!"),
            ), flTip = "House usually sits at 115–130 BPM. Drag FL's tempo display to try 124."),
        Lesson("rack.jam-rock", 1, "Jam: a beat from words", "Build a beat from a description, with no glowing help.", 3, 100, 1,
            show = listOf(KICK, CLAP, HAT), jam = true,
            steps = listOf(LessonStep("Your turn, no help! Kick on beats 1 and 3. Clap on 2 and 4. Hats on every other step.", "Build the whole beat.", Win.RACK, KICK,
                hint = "Kick: steps 1 and 9. Clap: steps 5 and 13. Hat: steps 1, 3, 5, 7, 9, 11, 13 and 15.",
                hintGhosts = (steps(1, 9).map { Ghost(KICK, step = it) } + steps(5, 13).map { Ghost(CLAP, step = it) }),
                check = chk@{ p, _ ->
                    listOf(Checks.drumSteps(p, KICK, steps(1, 9), ""), Checks.drumSteps(p, CLAP, steps(5, 13), ""), Checks.drumSteps(p, HAT, steps(*EIGHTHS), ""))
                        .firstOrNull { !it.pass } ?: ok("Level 1 complete!", "You built a beat just from words. That's how producers talk to each other.")
                })),
            flTip = "Desktop mission: build this in FL's Channel Rack and right-click a step off."),
    )

    private val l2 = listOf(
        Lesson("roll.first-notes", 2, "Draw four notes", "The Piano roll is a keyboard on its side. Up is higher, right is later.", 2, 90, 1,
            show = listOf(KICK, CLAP, HAT, LEAD), locked = setOf(KICK, CLAP, HAT), rows = 60..72, setup = { beatDrums(it) },
            steps = listOf(
                LessonStep("Remember the keyboard on its side? That's the Piano roll. Each row is one key. Tap a row to place a note.", "Place C5, D5, E5 and G5, one on each beat.", Win.ROLL, LEAD,
                    guides = rollGuide(LEAD, listOf(Triple(60, 0, 480), Triple(62, 480, 480), Triple(64, 960, 480), Triple(67, 1440, 480))),
                    hint = "Tap the gold outlines, one note per beat, going up like stairs. The 5 in C5 just tells you which C.", check = chk@{ p, _ -> exactNotes(p, LEAD, listOf(60 to 0, 62 to 480, 64 to 960, 67 to 1440), "Your first melody! Four notes climbing up the stairs.") }),
                LessonStep("Now walk back down the stairs: G5, E5, D5, C5.", "Place the four notes going down.", Win.ROLL, LEAD, setup = { it.clear(LEAD) },
                    hint = "Beat 1 is G5. Then one note per beat, going down: E5, D5, C5.", check = chk@{ p, _ -> exactNotes(p, LEAD, listOf(67 to 0, 64 to 480, 62 to 960, 60 to 1440), "Up and down. Now you can draw any tune!") }),
            ), flTip = "F7 opens a channel's Piano roll. Draw tool is P; right-click deletes on desktop."),
        Lesson("roll.long-short-rest", 2, "Long, short and silence", "Notes can be long or short. A rest is a quiet gap, and it's part of the music.", 3, 90, 2,
            show = listOf(LEAD), rows = 59..62, snap = Snap.HALF,
            steps = listOf(LessonStep("Drag a note's right edge to make it longer or shorter. An empty gap is a rest, a little silence.", "Copy the gold outlines on the C5 row.", Win.ROLL, LEAD,
                guides = { p -> listOf(0 to 960, 960 to 480, 1920 to 240, 2160 to 240, 2400 to 480, 2880 to 960).filter { (t, d) -> p.pattern.channel(LEAD).notes.none { it.startTick == t && abs(it.durationTicks - d) <= 60 } }.map { (t, d) -> Ghost(LEAD, pitch = 60, tick = t, lengthTicks = d) } },
                hint = "Bar 1: a 2 beat note, a 1 beat note, then a 1 beat rest. Bar 2: two short notes, a 1 beat note, a 2 beat note.",
                check = chk@{ p, _ -> rhythmCheck(p, LEAD, listOf(0 to 960, 960 to 480, 1920 to 240, 2160 to 240, 2400 to 480, 2880 to 960), 60, "Long, short and a rest. That's rhythm!") })),
            flTip = "Drag a note's right edge to resize. Right-click the magnet to choose the default note length."),
        Lesson("roll.c-major-scale", 2, "Build the C major scale", "The major scale recipe: whole, whole, half, whole, whole, whole, half.", 3, 90, 1,
            show = listOf(LEAD), rows = 59..73, snap = Snap.HALF,
            steps = listOf(
                LessonStep("Remember the major scale? Climb C to C on white keys only. Make the first note half a beat long. New notes copy it.", "Climb from C5 to C6 on white keys only.", Win.ROLL, LEAD,
                    guides = rollGuide(LEAD, listOf(60, 62, 64, 65, 67, 69, 71, 72).mapIndexed { i, pp -> Triple(pp, i * 240, 240) }),
                    hint = "Eight notes, half a beat each: C D E F G A B C. Drag a note's right edge to make it shorter.",
                    check = chk@{ p, _ -> val seq = notes(p, LEAD).map { it.pitch }
                        if (seq == listOf(60, 62, 64, 65, 67, 69, 71, 72) && notes(p, LEAD).zipWithNext().all { (a, b) -> b.startTick > a.startTick }) ok("That's C major!", "Whole, whole, half, whole, whole, whole, half. Every major scale uses this recipe.")
                        else no("Not quite the scale", "Use only white keys, from C5 up to C6. Put them one after another, left to right.") }),
                LessonStep("Now walk back down, from C6 to C5.", "Play the scale going down.", Win.ROLL, LEAD, setup = { it.clear(LEAD) }, hint = "Start on C6 at beat 1. Then go down one white key every half beat.",
                    check = chk@{ p, _ -> if (notes(p, LEAD).map { it.pitch } == listOf(72, 71, 69, 67, 65, 64, 62, 60)) ok("Down we go!", "You know C major both ways.") else no("Check the order", "Left to right: C6 B5 A5 G5 F5 E5 D5 C5.") }),
            ), flTip = "Piano roll → Snap to scale → Major, root C turns on FL's scale highlighting."),
        Lesson("roll.pentatonic", 2, "Jam: five safe notes", "Penta means five. The pentatonic scale has 5 notes that always sound good together.", 3, 90, 2,
            show = listOf(KICK, CLAP, HAT, LEAD), locked = setOf(KICK, CLAP, HAT), rows = 59..74, scale = Scales.C_PENTA, snap = Snap.HALF, jam = true, setup = { beatDrums(it) },
            steps = listOf(LessonStep("Only use the shaded rows: C D E G A. Nothing can sound wrong! Play anything, then end on C, your home note.", "Write 5 to 10 notes on shaded rows. End on a C.", Win.ROLL, LEAD,
                hint = "Any shaded row works. Make your last note, the one furthest right, a C.",
                check = chk@{ p, _ -> melody(p, LEAD, Checks.MelodyRules(minNotes = 5, scale = Scales.C_PENTA, endsOnPc = 0), "You just made up a tune in C pentatonic!").let {
                    if (it.pass && notes(p, LEAD).size > 10) no("A bit busy", "Use 10 notes or fewer. Space makes tunes catchy.") else it } })),
            flTip = "In FL, play only the black keys from E♭ — that's the minor pentatonic."),
    )

    private val l3 = listOf(
        Lesson("rack.sixteenth-hats", 3, "Sixteenths: 1 e and a", "Each step is a sixteenth note. 4 steps fit in 1 beat.", 2, 90, 1,
            show = listOf(KICK, CLAP, HAT), locked = setOf(KICK, CLAP), setup = { drumsFill(drumsFill(it, KICK, intArrayOf(1, 9)), CLAP, intArrayOf(5, 13)) },
            steps = listOf(drumStep("Say \"one e and a, two e and a\". That's 4 sounds in each beat, one for every step.", "Fill every step on the Hat row.", HAT, (0 until 16).toSet(),
                "Tap all 16 steps, from 1 to 16.", "Sixteenth hats. Busy and fast, like a speeding train!"))
            , flTip = "FL's step is a 1/16 note in 4/4. You can click-drag across steps to paint them."),
        Lesson("rack.hiphop-kick", 3, "Surprise kicks", "Syncopation means a surprise hit in between the beats. It makes heads nod.", 3, 90, 1,
            show = listOf(KICK, CLAP, HAT), locked = setOf(CLAP, HAT), setup = { drumsFill(drumsFill(it, CLAP, intArrayOf(5, 13)), HAT, EIGHTHS) },
            steps = listOf(
                drumStep("Hip-hop kicks like to dodge the beat. Let's kick on beat 1, and on the \"and\" after beat 3.", "Kick on step 1 and step 11.", KICK, steps(1, 11), "Beat 3 starts on step 9. Its \"and\" is 2 steps later, on step 11.", "Feel that push? That's syncopation."),
                LessonStep("Now make your own! Keep step 1. Add a surprise kick between the beats. Leave steps 5 and 13 for the clap.", "Make your own surprise kick pattern.", Win.RACK, KICK,
                    setup = { it.clear(KICK).bpm(87) }, hint = "Use 2 to 5 kicks. Try steps like 8, 11 or 15. Not 5 or 13.",
                    check = chk@{ p, _ ->
                        val k = (0 until 16).filter { p.pattern.channel(KICK).stepOn(it) }.toSet()
                        when {
                            0 !in k -> no("Start on the one", "Keep a kick on step 1.")
                            k.any { it == 4 || it == 12 } -> no("Leave 2 and 4", "Steps 5 and 13 belong to the clap. Turn the kick off there.")
                            k.none { it % 4 != 0 } -> no("Add a surprise", "Put at least one kick on a step that isn't a beat. The beats are steps 1, 5, 9 and 13.")
                            k.size !in 2..5 -> no("Keep it simple", "Use 2 to 5 kicks in total.")
                            else -> ok("That's your groove!", "Surprise kicks are what make heads nod.")
                        }
                    }),
            ), flTip = "This is the classic hip-hop / 2-step kick (the 1st and 6th eighth notes)."),
        Lesson("rack.open-hat", 3, "Open hat on the \"and\"", "An open hat rings long, tssss. A closed hat is short, ts.", 3, 124, 1,
            show = listOf(KICK, CLAP, HAT, OPEN_HAT), locked = setOf(KICK, CLAP),
            setup = { drumsFill(drumsFill(drumsFill(it, KICK, FOUR), CLAP, intArrayOf(5, 13)), HAT, EIGHTHS) },
            steps = listOf(LessonStep("A drummer can't hit both hats at once. Put open hats on the \"and\". Then remove the closed hats there.", "Open Hat on steps 3, 7, 11, 15. Turn off the closed Hat on those steps.", Win.RACK, OPEN_HAT,
                guides = { p -> steps(3, 7, 11, 15).sorted().firstOrNull { !p.pattern.channel(OPEN_HAT).stepOn(it) }?.let { listOf(Ghost(OPEN_HAT, step = it)) } ?: emptyList() },
                hint = "Open Hat row: steps 3, 7, 11 and 15. Hat row: keep only 1, 5, 9 and 13.",
                check = chk@{ p, _ ->
                    val o = Checks.drumSteps(p, OPEN_HAT, steps(3, 7, 11, 15), "")
                    val hats = (0 until 16).filter { p.pattern.channel(HAT).stepOn(it) }.toSet()
                    when {
                        !o.pass -> o
                        hats.intersect(steps(3, 7, 11, 15)).isNotEmpty() -> no("Both hats at once", "Turn off the closed Hat on steps 3, 7, 11 and 15, where the Open Hat plays.")
                        !hats.containsAll(steps(*FOUR)) -> no("Keep the closed hats", "Leave closed hats on steps 1, 5, 9 and 13.")
                        else -> ok("Ts, tssss!", "Open hats on the \"and\". That's the classic disco lift.")
                    }
                })), flTip = "Channel settings → Misc → Cut / Cut by makes the closed hat choke the open hat in FL."),
        Lesson("rack.jam-two-bars", 3, "Jam: two bars and a fill", "Repeat bar 1, then change the end of bar 2. That change is called a fill.", 4, 95, 2,
            show = listOf(KICK, SNARE, HAT), jam = true,
            setup = { p -> p.set(KICK, steps(1, 11).map { note(36, it * 120, 120) }).set(SNARE, steps(5, 13).map { note(38, it * 120, 120) }).set(HAT, steps(*EIGHTHS).map { note(42, it * 120, 120) }) },
            steps = listOf(LessonStep("The snare is a sharp crack, like the clap. Bar 2 copies bar 1, then plays a drum fill at the very end.", "Make bar 2: same hats, change 1 to 3 kicks, and a snare fill on beat 4.", Win.RACK, KICK,
                hint = "Bar 2 is steps 17 to 32. Copy bar 1: kick 17 and 27, snare 21 and 29, hats on odd steps. Add 1 kick, and snares on 30 and 31.",
                check = chk@{ p, _ ->
                    fun on(k: ChannelKind, bar: Int) = (0 until 16).filter { p.pattern.channel(k).stepOn(bar * 16 + it) }.toSet()
                    val kickDiff = (on(KICK, 0) union on(KICK, 1)) - (on(KICK, 0) intersect on(KICK, 1))
                    when {
                        on(KICK, 0) != steps(1, 11) || on(SNARE, 0) != steps(5, 13) -> no("Keep bar 1", "Bar 1 should stay the same: kick on 1 and 11, snare on 5 and 13.")
                        on(HAT, 1) != on(HAT, 0) -> no("Hats stay the same", "Bar 2's hats should copy bar 1's hats exactly.")
                        kickDiff.size !in 1..3 -> no("Change the kick a little", "Bar 2's kick should be different from bar 1 in 1 to 3 steps.")
                        4 !in on(SNARE, 1) -> no("Keep the backbeat", "Bar 2 still needs a snare on step 21, its beat 2.")
                        on(SNARE, 1).count { it in 12..15 } < 3 -> no("Add the fill", "Put at least 3 snares on steps 29 to 32. That's beat 4 of bar 2.")
                        else -> ok("Level 3 complete!", "A 2 bar beat with a fill. It turns around like a real drummer.")
                    }
                })), flTip = "Patterns can be any length: set the Channel Rack's pattern length to 32 steps for two bars."),
    )

    private val l4 = listOf(
        Lesson("roll.build-c-major", 4, "Build C major", "A chord is 3 notes that start together. C major is C, E and G.", 2, 96, 1,
            show = listOf(CHORDS), rows = 55..76,
            steps = listOf(
                LessonStep("Remember take, skip, take? Stack C5, E5 and G5 on beat 1. They all start together.", "Stack C, E and G on beat 1.", Win.ROLL, CHORDS,
                    guides = rollGuide(CHORDS, C.map { Triple(it, 0, 480) }), hint = "Take C5. Skip D, take E5. Skip F, take G5.",
                    check = chk@{ p, _ -> Checks.chordAt(p, CHORDS, 0, pcs(C), "C major") }),
                LessonStep("Again on beat 3, with no outlines this time.", "Build C major on beat 3.", Win.ROLL, CHORDS, hint = "C5, E5 and G5, all starting on beat 3.",
                    check = chk@{ p, _ -> Checks.chordAt(p, CHORDS, 960, pcs(C), "C major") }),
            ), flTip = "FL's Piano roll has a Chord stamp, but building by hand teaches what's inside."),
        Lesson("roll.minor-droop", 4, "Major to minor", "Move the middle note down one half step. Happy major turns into sad minor.", 2, 96, 1,
            show = listOf(CHORDS), rows = 55..76, setup = { it.set(CHORDS, chordNotes(C, 0, 1920)) },
            steps = listOf(
                LessonStep("Hear that bright C major? Move only the middle note, E, down one row to E♭. Now it sounds sad.", "Make C minor: move E5 down to E♭5.", Win.ROLL, CHORDS,
                    hint = "Tap the Select tool, tap E5, then tap the ↓ semi button once. Semi means one half step.", check = chk@{ p, _ -> Checks.chordAt(p, CHORDS, 0, setOf(0, 3, 7), "C minor") }),
                LessonStep("Now build A minor on beat 3 by yourself: A, C and E.", "Build A minor on beat 3.", Win.ROLL, CHORDS, setup = { it.clear(CHORDS) },
                    hint = "A4, C5 and E5. All white keys: take, skip, take, skip, take.", check = chk@{ p, _ -> Checks.chordAt(p, CHORDS, 960, setOf(9, 0, 4), "A minor") }),
            ), flTip = "In FL, Shift+↓ moves a selection one semitone."),
        Lesson("roll.two-chords", 4, "Two chords, two bars", "Copy a chord and slide it up to make a new chord.", 3, 96, 2,
            show = listOf(CHORDS), rows = 55..76,
            steps = listOf(LessonStep("Build C for all of bar 1. Copy it into bar 2, then slide the copy up 5 rows. It becomes F!", "C in bar 1, F in bar 2. Each one a whole bar long.", Win.ROLL, CHORDS,
                hint = "Stretch each note to a whole bar. Select all three notes, tap Copy →, then tap ↑ semi 5 times.",
                check = chk@{ p, _ ->
                    val a = Checks.chordAt(p, CHORDS, 0, pcs(C), "C"); val b = Checks.chordAt(p, CHORDS, 1920, pcs(F), "F")
                    when { !a.pass -> a; !b.pass -> b
                        notes(p, CHORDS).any { it.durationTicks < 1440 } -> no("Let them ring", "Make each chord last most of its bar. Drag the right edges.")
                        else -> ok("C to F!", "That move is in thousands of songs.") }
                })), flTip = "In FL's Piano roll, Ctrl+B duplicates the selection to the right."),
        Lesson("roll.four-chord-loop", 4, "The four-chord loop", "C, G, Am, F: four chords from the key of C that loop forever.", 4, 96, 4,
            show = listOf(KICK, CLAP, HAT, CHORDS), locked = setOf(KICK, CLAP, HAT), rows = 55..76,
            setup = { beatDrums(it).set(CHORDS, chordNotes(C, 0, 1920)) },
            steps = listOf(
                LessonStep("Bar 1 is C. Add G, A minor and F, one chord per bar. Listen: which one feels like home?", "Build G, Am and F in bars 2, 3 and 4.", Win.ROLL, CHORDS,
                    hint = "G is G B D. Am is A C E. F is F A C. Each one starts on beat 1 of its bar.", check = chk@{ p, _ -> Checks.progression(p, CHORDS, LOOP.map { it.first to pcs(it.second) }) }),
                LessonStep("Let's swap bar 4 from F to D minor. Listen to what changes.", "Make bar 4 D minor: D, F and A.", Win.ROLL, CHORDS,
                    hint = "Keep F and A. Move the C up two rows, to D.", check = chk@{ p, _ -> Checks.chordAt(p, CHORDS, 3 * 1920, pcs(Dm), "D minor") }),
            ), flTip = "Clone the pattern in FL to keep version A while you try B."),
        Lesson("roll.chord-stabs", 4, "Offbeat stabs", "Stabs are short, punchy chords. Here they play on the \"and\", between kicks.", 3, 122, 2,
            show = listOf(KICK, CHORDS), locked = setOf(KICK), rows = 55..76, snap = Snap.STEP, setup = { drumsFill(it, KICK, FOUR) },
            steps = listOf(LessonStep("House piano stabs are short chords between the kicks. Am in bar 1, F in bar 2, on steps 3, 7, 11 and 15.", "Short Am stabs in bar 1, F stabs in bar 2. Use steps 3, 7, 11 and 15.", Win.ROLL, CHORDS,
                hint = "Build Am (A C E) on step 3. Drag its right edges so it lasts 2 steps or less. New notes copy that length.",
                check = chk@{ p, _ ->
                    for (bar in 0..1) {
                        val want = if (bar == 0) pcs(Am) else pcs(F)
                        val ons = onsetsInBar(p, CHORDS, bar)
                        if (ons != setOf(240, 720, 1200, 1680)) return@chk no("Check the timing", "Bar ${bar + 1}: stabs start on steps 3, 7, 11 and 15 only.")
                        for (o in ons) { val got = barNotes(p, CHORDS, bar).filter { it.startTick % 1920 == o }.map { it.pitch % 12 }.toSet()
                            if (got != want) return@chk no("Wrong chord", "Bar ${bar + 1} uses ${if (bar == 0) "Am (A C E)" else "F (F A C)"}.") }
                    }
                    if (notes(p, CHORDS).any { it.durationTicks > 240 }) no("Shorter!", "Stabs are short: 2 steps or less. Drag each right edge to the left.") else ok("Level 4 complete!", "Offbeat stabs. That's a real house piano!")
                })), flTip = "Paint tool (B) drags repeated notes; set Snap to Step for stabs."),
    )

    private val l5 = listOf(
        Lesson("rack.hat-accents", 5, "Loud and soft hats", "Velocity means how hard a sound is hit. Mixing loud and soft makes a groove.", 3, 92, 1,
            show = listOf(KICK, SNARE, HAT), locked = setOf(KICK, SNARE),
            setup = { drumsFill(drumsFill(drumsFill(it, KICK, intArrayOf(1, 9)), SNARE, intArrayOf(5, 13)), HAT, (1..16).toList().toIntArray()) },
            steps = listOf(LessonStep("Velocity is how hard a drum is hit. Press and hold a step to pick Soft, Medium or Loud.", "Make every even-numbered hat Soft: steps 2, 4, 6, up to 16.", Win.RACK, HAT,
                hint = "Press and hold step 2, then tap Soft (ghost). Do the same for 4, 6, 8, 10, 12, 14 and 16.",
                check = chk@{ p, _ ->
                    val odd = (0 until 16 step 2).mapNotNull { vel(p, HAT, it) }; val even = (1 until 16 step 2).mapNotNull { vel(p, HAT, it) }
                    when { odd.size + even.size < 16 -> no("Keep all 16 hats", "Don't remove any hats. Just make some of them softer.")
                        odd.min() - even.max() < 25 -> no("Bigger difference", "Make the even steps Soft, so they are much quieter than the odd steps.")
                        else -> ok("Hear the bounce?", "Loud and soft hits make a machine sound like a real drummer.") }
                })), flTip = "Open the Graph editor in FL's Channel Rack, or the Piano roll velocity lane."),
        Lesson("rack.ghost-snare", 5, "Ghost notes", "A ghost note is a very quiet snare. You feel it more than you hear it.", 3, 90, 1,
            show = listOf(KICK, SNARE, HAT), locked = setOf(KICK, HAT),
            setup = { p -> drumsFill(drumsFill(p, KICK, intArrayOf(1, 8, 11)), HAT, EIGHTHS).set(SNARE, steps(5, 13).map { note(38, it * 120, 120, 110) }) },
            steps = listOf(LessonStep("Here's a boom bap trick: tiny, quiet snares right next to the loud ones. Add two, then make them Soft.", "Add 2 or more Soft snares next to the loud ones. Not on a kick step.", Win.RACK, SNARE,
                hint = "Try step 4 and step 14. Then press and hold each one and pick Soft (ghost).",
                check = chk@{ p, _ ->
                    val sn = (0 until 16).mapNotNull { s -> vel(p, SNARE, s)?.let { s to it } }
                    val loud = sn.filter { it.second >= 90 }.map { it.first }.toSet(); val ghosts = sn.filter { it.second <= 55 }.map { it.first }
                    val kicks = (0 until 16).filter { p.pattern.channel(KICK).stepOn(it) }.toSet()
                    when { loud != steps(5, 13) -> no("Keep the backbeat loud", "Only steps 5 and 13 should be loud. Press and hold your new snares and pick Soft.")
                        ghosts.size < 2 -> no("Add ghosts", "Add at least two Soft snares.")
                        ghosts.any { it in kicks } -> no("Not on a kick", "Ghost notes never sit on a kick step. The kicks are on steps 1, 8 and 11.")
                        ghosts.none { it in steps(4, 6, 12, 14) } -> no("Stay close to the backbeat", "Put a ghost right next to step 5 or 13. Try steps 4, 6, 12 or 14.")
                        else -> ok("Ghosted!", "That bouncy feel in boom bap? Ghost notes.") }
                })), flTip = "In FL, ghost a step by lowering its velocity in the Graph editor."),
        Lesson("rack.swing", 5, "Swing it", "Swing makes every other step a tiny bit late. Straight turns bouncy.", 2, 90, 1,
            show = listOf(KICK, SNARE, HAT), locked = setOf(KICK, SNARE, HAT),
            setup = { p -> drumsFill(drumsFill(p, KICK, intArrayOf(1, 9)), SNARE, intArrayOf(5, 13)).set(HAT, (0 until 16).map { note(42, it * 120, 120, if (it % 2 == 0) 110 else 60) }) },
            steps = listOf(LessonStep("Press play. Then turn the Swing knob at the top of the Channel Rack. Stop when the beat bounces, like skipping.", "Set swing between 20% and 70%.", Win.RACK, HAT,
                hint = "Drag the Swing knob up a little at a time. Listen as you go.", needsLoops = 1,
                check = chk@{ p, _ -> if (p.pattern.swing in 0.2f..0.7f) ok("Bouncy!", "Swing is why lo-fi and hip-hop feel human.") else no(if (p.pattern.swing < 0.2f) "Still straight" else "Too wobbly", "Aim for between 20% and 70% swing.") })),
            flTip = "FL's Swing knob is in the Channel Rack and only moves even steps."),
        Lesson("roll.trap-roll", 5, "Jam: trap beat and hat roll", "In trap the clap waits until beat 3. A hat roll is a burst of super fast hats.", 4, 140, 2,
            show = listOf(KICK, CLAP, HAT), locked = setOf(KICK), jam = true, rows = 42..42, snap = Snap.HALF_STEP,
            setup = { p -> drumsFill(p, HAT, EIGHTHS).set(KICK, listOf(note(36, 0, 120), note(36, 12 * 120, 120), note(36, (16 + 4) * 120, 120))) },
            steps = listOf(
                LessonStep("Trap feels slow and heavy because the clap waits until beat 3. Put a clap on step 9 in both bars.", "Clap on step 9 of each bar: steps 9 and 25.", Win.RACK, CLAP,
                    hint = "Beat 3 is step 9. In bar 2, that's step 25.", check = chk@{ p, _ -> Checks.drumSteps(p, CLAP, steps(9, bars = 2), "Half-time. Now it feels huge!") }),
                LessonStep("Now the Hat is in the Piano roll. Near the end of bar 2, tap 3 to 6 hats in a row, very close together.", "Add one fast hat roll at the end of bar 2.", Win.ROLL, HAT,
                    hint = "Snap is ½ step, so taps land very close. Tap 3 to 6 times in a row, side by side, on beat 4 of bar 2.",
                    check = chk@{ p, _ ->
                        val hs = notes(p, HAT).map { it.startTick }.sorted()
                        var best = 1; var run = 1
                        for (i in 1 until hs.size) { val d = hs[i] - hs[i - 1]; run = if (d == 60 || d == 80) run + 1 else 1; best = maxOf(best, run) }
                        if (best >= 3) ok("Level 5 complete!", "Just one roll here and there sounds great. That's tasteful trap.") else no("Roll it", "Put at least 3 hats in a row, each half a step apart.")
                    }),
            ), flTip = "In FL's Piano roll, set Snap to 1/2 step for 32nds; draw a rising velocity line."),
    )

    private val chordBack = { p: Project -> beatDrums(loopChords(p)) }
    private val l6 = listOf(
        Lesson("roll.bass-roots", 6, "Roots under the chords", "The bass plays each chord's root. That's the note the chord is named after.", 3, 96, 4,
            show = listOf(KICK, CLAP, HAT, CHORDS, BASS), locked = setOf(KICK, CLAP, HAT, CHORDS), ghost = setOf(CHORDS), rows = 36..52, setup = chordBack,
            steps = listOf(LessonStep("That's me, the bass! I play each chord's name note, way down low. C, G, A, F, one long note per bar.", "One long root note per bar: C, G, A, then F.", Win.ROLL, BASS,
                guides = rollGuide(BASS, listOf(Triple(36, 0, 1920), Triple(43, 1920, 1920), Triple(45, 3840, 1920), Triple(41, 5760, 1920))),
                hint = "C3, G3, A3 and F3. Make each one last the whole bar.",
                check = chk@{ p, _ ->
                    val want = listOf(0, 7, 9, 5)
                    for (b in 0..3) { val ns = barNotes(p, BASS, b)
                        if (ns.size != 1) return@chk no("One note per bar", "Bar ${b + 1} needs exactly one bass note.")
                        if (ns[0].pitch % 12 != want[b]) return@chk no("Wrong root", "Bar ${b + 1}'s chord is ${listOf("C", "G", "Am", "F")[b]}. Its root is ${Pitch.pcName(want[b])}.")
                        if (ns[0].durationTicks < 1440) return@chk no("Hold it", "Let bar ${b + 1}'s bass note ring for most of the bar. Drag its right edge.") }
                    ok("Now it has a floor!", "Root notes make chords sound strong and solid.")
                })), flTip = "In FL, load a bass instrument (e.g. BooBass) and keep it simple: one note per chord."),
        Lesson("roll.bass-lock-kick", 6, "Lock to the kick", "When the bass and kick hit together, they sound like one big drum.", 3, 96, 4,
            show = listOf(KICK, CHORDS, BASS), locked = setOf(KICK, CHORDS), ghost = setOf(CHORDS, KICK), rows = 36..52, snap = Snap.STEP,
            setup = { p -> roots(drumsFill(loopChords(p), KICK, intArrayOf(1, 8, 11)), listOf(0, 7, 9, 5), 2 * TICKS_PER_STEP) },
            steps = listOf(LessonStep("See the grey kick shapes? Add bass notes that start with every kick: steps 1, 8 and 11 of each bar.", "Bass on every kick. Use the same note all bar.", Win.ROLL, BASS,
                hint = "Each bar has a short note on step 1. Tap it once so new notes come out short. Add the same note on steps 8 and 11.",
                check = chk@{ p, _ ->
                    val want = listOf(0, 7, 9, 5)
                    for (b in 0..3) {
                        if (onsetsInBar(p, BASS, b) != setOf(0, 7 * 120, 10 * 120)) return@chk no("Match the kick", "Bar ${b + 1}: start bass notes exactly on steps 1, 8 and 11.")
                        if (barNotes(p, BASS, b).any { it.pitch % 12 != want[b] }) return@chk no("Same root", "Keep bar ${b + 1} on ${Pitch.pcName(want[b])}.")
                    }
                    ok("Locked in!", "Kick plus bass sounds like one punchy drum.")
                })), flTip = "In FL, Ghost channels show the kick's notes behind the bass."),
        Lesson("roll.octave-bass", 6, "Octave bounce", "Jump between a low note and the same note one octave up.", 3, 120, 2,
            show = listOf(CHORDS, BASS), locked = setOf(CHORDS), ghost = setOf(CHORDS), rows = 36..60, snap = Snap.HALF,
            setup = { it.set(CHORDS, chordNotes(Am, 0, 1920) + chordNotes(F, 1920, 1920)) },
            steps = listOf(LessonStep("Disco bass goes low, high, low, high, twice per beat. Remember octaves? Same letter, 12 rows higher.", "Low A on each beat, high A on each \"and\". Then the same with F in bar 2.", Win.ROLL, BASS,
                hint = "Make your first note half a beat long, so new notes copy it. Bar 1: A3, A4, A3, A4 and so on. Bar 2: F3, F4, F3, F4.",
                check = chk@{ p, _ ->
                    for ((b, pc) in listOf(0 to 9, 1 to 5)) {
                        val ns = barNotes(p, BASS, b)
                        if (ns.map { it.startTick % 1920 }.toSet() != (0 until 8).map { it * 240 }.toSet()) return@chk no("Two notes per beat", "Bar ${b + 1}: one note every half beat, 8 notes in all.")
                        if (ns.any { it.pitch % 12 != pc }) return@chk no("Stay on the root", "Bar ${b + 1} uses only ${Pitch.pcName(pc)}, low and high.")
                        val lows = ns.filter { (it.startTick % 480) == 0 }.map { it.pitch }.toSet(); val highs = ns.filter { (it.startTick % 480) == 240 }.map { it.pitch }.toSet()
                        if (lows.size != 1 || highs.size != 1 || highs.first() - lows.first() != 12) return@chk no("Bounce an octave", "Low note on each beat. The same note 12 rows higher on each \"and\".")
                    }
                    ok("Bounce!", "That's the disco octave bass.")
                })), flTip = "Ctrl+↑ moves selected notes up an octave in FL."),
        Lesson("roll.808-long", 6, "808: long notes in C minor", "An 808 is a deep bass drum that plays notes. Start it with a kick and let it ring.", 4, 140, 2,
            show = listOf(KICK, CLAP, BASS), locked = setOf(KICK, CLAP), ghost = setOf(KICK), rows = 24..48, scale = Scales.C_MINOR, snap = Snap.STEP,
            setup = { p -> drumsFill(p, CLAP, intArrayOf(9)).set(KICK, listOf(note(36, 0, 120), note(36, 1200, 120), note(36, 1920, 120), note(36, 1920 + 840, 120))) },
            steps = listOf(LessonStep("Headphones help here! Start a long C on the first kick. Then add 1 to 3 notes on other kicks, on shaded rows.", "Write an 808 line in C minor. Start on C.", Win.ROLL, BASS,
                hint = "Kicks are on steps 1 and 11 of bar 1, and steps 1 and 8 of bar 2. Make one note 2 beats long. Shaded rows are C minor, a darker key.",
                check = chk@{ p, _ ->
                    val kicks = notes(p, KICK).map { it.startTick }.toSet(); val b = notes(p, BASS)
                    when { b.isEmpty() -> no("Start the 808", "Put a C on the first kick, bar 1 beat 1.")
                        b.first().pitch % 12 != 0 -> no("Start on C", "Your first 808 note should be a C.")
                        b.any { it.startTick !in kicks } -> no("Ride the kick", "Every 808 note must start where a kick is.")
                        b.any { it.pitch % 12 !in Scales.C_MINOR } -> no("Stay in key", "Use the shaded rows. They are the notes of C minor.")
                        b.none { it.durationTicks >= 960 } -> no("Let it ring", "Make at least one note 2 beats long. Drag its right edge.")
                        b.groupBy { it.startTick }.any { it.value.size > 1 } -> no("One at a time", "An 808 plays one note at a time. Remove the stacked note.")
                        else -> ok("Boom.", "Deep, long and with the kick. That's an 808!") }
                })), flTip = "Set the 808 channel to Mono in FL. Overlapping notes make a glide."),
        Lesson("roll.bass-approach", 6, "Jam: walk into the next chord", "An approach note is a bass note that leans into the next chord's root.", 3, 96, 4,
            show = listOf(CHORDS, BASS), locked = setOf(CHORDS), ghost = setOf(CHORDS), rows = 36..52, jam = true,
            setup = { p -> roots(loopChords(p), listOf(0, 7, 9, 5), 1440) },
            steps = listOf(LessonStep("Beat 4 is empty in bars 1, 2 and 3. Add one note there that leans into the next root, 1 or 2 rows away.", "Add a note on beat 4 of bars 1, 2 and 3.", Win.ROLL, BASS,
                hint = "Before G, try F or A. Before A, try G or B. Before F, try E or G.",
                check = chk@{ p, _ ->
                    val next = listOf(43, 45, 41)
                    for (b in 0..2) {
                        val a = notes(p, BASS).firstOrNull { it.startTick == b * 1920 + 1440 } ?: return@chk no("Fill beat 4", "Bar ${b + 1} needs a note on beat 4.")
                        val d = (0..3).map { o -> abs(a.pitch - (next[b] - 12 * o + 24)) }.min()
                        if (d !in 1..2 || a.pitch % 12 !in Scales.C_MAJOR) return@chk no("Closer!", "Bar ${b + 1}, beat 4: use a white key 1 or 2 rows away from ${Pitch.pcName(next[b] % 12)}.")
                    }
                    ok("Level 6 complete!", "Walking bass. Your line pulls the chords along!")
                })), flTip = "In FL, Duplicate bars, then edit only the last note."),
    )

    private val melodyBack = { p: Project -> roots(beatDrums(loopChords(p)), listOf(0, 7, 9, 5)) }
    private val back4 = listOf(KICK, CLAP, HAT, CHORDS, BASS)
    private val l7 = listOf(
        Lesson("roll.chord-tone-on-one", 7, "A chord note on every 1", "Chord notes are the notes inside the chord playing right now. They always fit.", 3, 96, 4,
            show = back4 + LEAD, locked = back4.toSet(), ghost = setOf(CHORDS), rows = 60..84, setup = melodyBack,
            steps = listOf(LessonStep("The grey shapes behind are the chords. Start each bar's tune on one of that chord's notes.", "Put a note on beat 1 of every bar, using a note from that bar's chord.", Win.ROLL, LEAD,
                hint = "Bar 1 (C): C, E or G. Bar 2 (G): G, B or D. Bar 3 (Am): A, C or E. Bar 4 (F): F, A or C.",
                check = chk@{ p, _ -> melody(p, LEAD, Checks.MelodyRules(minNotes = 4, lowest = 60, highest = 84, noOverlaps = false,
                    chordTonesOnDownbeats = (0..3).associateWith { chordPcsAtBar(p, it) }), "Every bar starts on its chord. Instant harmony!") })),
            flTip = "In FL, Piano roll → View → Ghost channels (Alt+V) shows the chords behind the lead."),
        Lesson("roll.steps-and-leaps", 7, "Step, step, leap, turn back", "Steps sound smooth. Leaps sound exciting. After a big leap, turn back.", 4, 96, 2,
            show = listOf(KICK, CLAP, HAT, LEAD), locked = setOf(KICK, CLAP, HAT), rows = 59..79, scale = Scales.C_MAJOR, snap = Snap.HALF, setup = { beatDrums(it) },
            steps = listOf(LessonStep("Mostly take steps to the next shaded row, with one bigger leap. After a big leap, go back the other way.", "Write 6 to 10 notes, with one leap that turns back.", Win.ROLL, LEAD,
                hint = "A leap skips at least one shaded row. If you leap up, step down next.",
                check = chk@{ p, _ ->
                    val ns = notes(p, LEAD); val base = melody(p, LEAD, Checks.MelodyRules(minNotes = 6, scale = Scales.C_MAJOR, minStepwise = 0.6f), "")
                    if (!base.pass) return@chk base
                    if (ns.size > 10) return@chk no("A bit long", "Use 10 notes or fewer.")
                    val moves = ns.zipWithNext { a, b -> b.pitch - a.pitch }
                    if (moves.none { abs(it) >= 3 }) return@chk no("Add a leap", "Make one jump that skips at least one shaded row.")
                    val bad = moves.zipWithNext().firstOrNull { (a, b) -> abs(a) >= 6 && (b == 0 || (a > 0) == (b > 0)) }
                    if (bad != null) no("Turn back after the leap", "After a big jump, move the other way. That's what singers do.") else ok("That sings!", "Steps plus one leap that turns back. Great tunes have this shape.")
                })), flTip = "Scale highlighting shows which rows are a step away."),
        Lesson("roll.contour-arch", 7, "Shape: a hill", "Tunes have a shape. This one climbs to one top note and comes back down, like a hill.", 3, 96, 2,
            show = listOf(LEAD), rows = 59..79, scale = Scales.C_MAJOR, snap = Snap.HALF,
            steps = listOf(LessonStep("Start low. Climb up to ONE highest note near the middle. Then walk back down, like going over a hill.", "Write a hill: 6 to 10 notes, with one top note in the middle.", Win.ROLL, LEAD,
                hint = "Only one note can be the very top. Start and end at least 3 rows below it.",
                check = chk@{ p, _ ->
                    val ns = notes(p, LEAD).map { it.pitch }
                    if (ns.size !in 6..10) return@chk no("6 to 10 notes", "Use between 6 and 10 notes.")
                    val peak = ns.max(); val idx = ns.indexOf(peak)
                    when { ns.count { it == peak } > 1 -> no("One top note", "Only one note should be the highest.")
                        idx < ns.size * 0.25 || idx > ns.size * 0.75 -> no("Top in the middle", "Put the highest note near the middle, not near the start or end.")
                        ns.first() > peak - 3 || ns.last() > peak - 3 -> no("Start and end lower", "Begin and end at least 3 rows below the top note.")
                        ns.max() - ns.min() > 12 -> no("Smaller hill", "Keep it within one octave: 12 rows from lowest to highest.")
                        ns.any { it % 12 !in Scales.C_MAJOR } -> no("Stay in C major", "Use the shaded rows.")
                        else -> ok("Beautiful hill!", "Going up builds excitement. Coming down lets it relax.") }
                })), flTip = "Zoom out in FL (Ctrl+scroll) to see the whole shape."),
        Lesson("roll.rhythm-first", 7, "Rhythm first, then pitch", "A tune is rhythm plus pitch. Keep a good rhythm and just change the notes.", 3, 96, 2,
            show = back4 + LEAD, locked = back4.toSet(), ghost = setOf(CHORDS), rows = 59..79, scale = Scales.C_MAJOR,
            setup = { p -> melodyBack(p).set(LEAD, listOf(0 to 480, 480 to 240, 720 to 240, 960 to 480, 1920 to 240, 2160 to 240, 2400 to 960).map { (t, d) -> note(60, t, d) }) },
            steps = listOf(LessonStep("This rhythm is already good, but every note is C. Move notes up or down only, never left or right.", "Give the rhythm a tune. Start each bar on a chord note.", Win.ROLL, LEAD,
                hint = "Drag notes straight up or down. Bar 1's chord is C (C E G). Bar 2's chord is G (G B D).",
                check = chk@{ p, _ ->
                    val ref = listOf(0, 480, 720, 960, 1920, 2160, 2400)
                    val ns = notes(p, LEAD)
                    when { ns.map { it.startTick } != ref -> no("Keep the rhythm", "Only move notes up or down. Don't change when they start.")
                        ns.map { it.pitch }.toSet().size < 3 -> no("More notes", "Use at least 3 different notes.")
                        ns.any { it.pitch % 12 !in Scales.C_MAJOR } -> no("Stay in key", "Use the shaded rows.")
                        ns[0].pitch % 12 !in pcs(C) || ns[4].pitch % 12 !in pcs(G) -> no("Lean on the chord", "Start bar 1 on C, E or G. Start bar 2 on G, B or D.")
                        else -> ok("A real tune!", "Good rhythm plus chord notes makes a melody.") }
                })), flTip = "In FL, Shift+↑/↓ moves selected notes in pitch without changing timing."),
        Lesson("roll.land-home", 7, "Jam: land at home", "A tune sounds finished when it lands on the home note.", 3, 96, 2,
            show = listOf(CHORDS, LEAD), locked = setOf(CHORDS), ghost = setOf(CHORDS), rows = 59..79, jam = true,
            setup = { it.set(CHORDS, chordNotes(G, 0, 1920) + chordNotes(C, 1920, 1920)) },
            steps = listOf(LessonStep("The G chord wants to go home to C. Write 4 to 10 notes, mostly steps. Leave a small gap. Land on a long C.", "End on a C in bar 2, on beat 1 or 3, at least 1 beat long.", Win.ROLL, LEAD,
                hint = "B loves to step up to C. Try B at the end of bar 1, then a long C on beat 1 of bar 2.",
                check = chk@{ p, _ ->
                    val base = melody(p, LEAD, Checks.MelodyRules(minNotes = 4, scale = Scales.C_MAJOR, minStepwise = 0.5f, endsOnPc = 0, needsRest = true), "")
                    val last = notes(p, LEAD).lastOrNull()
                    when { !base.pass -> base
                        last == null || last.startTick !in setOf(1920, 2880) || last.durationTicks < 480 -> no("Land on a strong beat", "End with a C on beat 1 or 3 of bar 2, at least 1 beat long.")
                        notes(p, LEAD).size > 10 -> no("Fewer notes", "Use 10 notes or fewer.")
                        else -> ok("Level 7 complete!", "Your tune goes somewhere and comes home.") }
                })), flTip = "End phrases with a longer note and some space so the loop breathes."),
    )

    private val l8 = listOf(
        Lesson("roll.motif-repeat", 8, "Make a motif, repeat it", "A motif is a tiny tune idea. Repeating it makes it catchy.", 3, 96, 2,
            show = listOf(CHORDS, LEAD), locked = setOf(CHORDS), ghost = setOf(CHORDS), rows = 59..79, scale = Scales.C_MAJOR, snap = Snap.HALF,
            setup = { it.set(CHORDS, chordNotes(C, 0, 1920) + chordNotes(G, 1920, 1920)) },
            steps = listOf(LessonStep("A motif is a tiny tune, like a catchphrase. Write 3 to 5 notes in bar 1, with a gap. Then copy it to bar 2.", "Motif in bar 1, exact copy in bar 2.", Win.ROLL, LEAD,
                hint = "Use the Select tool to tap every note in bar 1, then tap Copy →.",
                check = chk@{ p, _ ->
                    val b1 = barNotes(p, LEAD, 0); val b2 = barNotes(p, LEAD, 1)
                    when { b1.size !in 3..5 -> no("3 to 5 notes", "Bar 1 needs 3 to 5 notes.")
                        !hasRest(p, LEAD, 240, 0, 0) -> no("Leave a gap", "Leave at least half a beat of silence in bar 1.")
                        b2.map { it.pitch to it.startTick % 1920 } != b1.map { it.pitch to it.startTick } -> no("Exact copy", "Bar 2 should repeat bar 1 exactly: same notes, same timing.")
                        b1.any { it.pitch % 12 !in Scales.C_MAJOR } -> no("Stay in key", "Use the shaded rows.")
                        else -> ok("Catchy!", "Say it twice and people remember it.") }
                })), flTip = "In FL, select bar 1 (E tool) then Ctrl+B."),
        Lesson("roll.same-rhythm-new-ending", 8, "Same rhythm, different ending", "A variation keeps most of an idea and changes a little. Here, just the last note.", 3, 96, 2,
            show = listOf(LEAD), rows = 59..79,
            setup = { p -> p.set(LEAD, listOf(60 to 0, 64 to 480, 62 to 960).flatMap { (pp, t) -> listOf(note(pp, t, 480), note(pp, t + 1920, 480)) }) },
            steps = listOf(LessonStep("Both bars are the same. Keep the timing. Just give bar 2 a different last note.", "Change only the last note of bar 2.", Win.ROLL, LEAD,
                hint = "Drag bar 2's third note, on beat 3, up or down. Don't move it sideways.",
                check = chk@{ p, _ ->
                    val b1 = barNotes(p, LEAD, 0); val b2 = barNotes(p, LEAD, 1)
                    when { b2.map { it.startTick % 1920 } != b1.map { it.startTick } -> no("Keep the rhythm", "Bar 2 needs the same three notes on beats 1, 2 and 3.")
                        b2.dropLast(1).map { it.pitch } != b1.dropLast(1).map { it.pitch } -> no("Only the ending", "Keep the first two notes of bar 2 the same as bar 1.")
                        b2.last().pitch == b1.last().pitch -> no("Change the ending", "Give bar 2's last note a different pitch.")
                        else -> ok("Variation!", "Same rhythm, new ending. That's how catchy tunes grow.") }
                })), flTip = "Clone the pattern in FL before you vary it, so you keep both."),
        Lesson("roll.question-answer", 8, "Question and answer", "The question tune stops without going home. The answer starts the same way and lands home.", 4, 96, 4,
            show = listOf(CHORDS, LEAD), locked = setOf(CHORDS), ghost = setOf(CHORDS), rows = 59..79, scale = Scales.C_MAJOR, snap = Snap.HALF,
            setup = { it.set(CHORDS, listOf(C, G, F, C).flatMapIndexed { i, ch -> chordNotes(ch, i * 1920, 1920) }) },
            steps = listOf(LessonStep("Bars 1 and 2 ask a question: end on D, G or B. Bars 3 and 4 answer: start like bar 1, end on C.", "Write a question in bars 1 and 2, and an answer in bars 3 and 4.", Win.ROLL, LEAD,
                hint = "Leave a gap in bars 1 or 2. Copy bar 1's rhythm into bar 3. End bar 4 on C.",
                check = chk@{ p, _ ->
                    val q = notes(p, LEAD).filter { it.startTick < 3840 }; val a = notes(p, LEAD).filter { it.startTick >= 3840 }
                    when { q.isEmpty() || a.isEmpty() -> no("Both halves", "Write notes in bars 1 and 2, and in bars 3 and 4.")
                        q.last().pitch % 12 == 0 -> no("Leave the question open", "End bars 1 and 2 on D, G or B, not on C.")
                        a.last().pitch % 12 != 0 || a.last().startTick < 5760 -> no("Answer at home", "End bar 4 on a C.")
                        rhythmSimilarity(onsetsInBar(p, LEAD, 0), onsetsInBar(p, LEAD, 2)) < 0.75f -> no("Echo the start", "Bar 3 should start with the same rhythm as bar 1.")
                        !hasRest(p, LEAD, 240, 0, 1) -> no("Breathe", "Leave a gap, a short rest, somewhere in bars 1 and 2.")
                        notes(p, LEAD).any { it.pitch % 12 !in Scales.C_MAJOR } -> no("Stay in key", "Use the shaded rows.")
                        else -> ok("Question and answer!", "Your melody is having a conversation.") }
                })), flTip = "Clone the pattern in FL to try a second answer."),
        Lesson("roll.sequence", 8, "Move the motif up a step", "A sequence is the same motif again, one step higher, like climbing a stair.", 3, 96, 2,
            show = listOf(LEAD), locked = emptySet(), rows = 59..79, scale = Scales.C_MAJOR,
            setup = { p -> p.set(LEAD, listOf(note(60, 0, 480), note(62, 480, 240), note(64, 720, 240))) },
            steps = listOf(LessonStep("Copy bar 1 into bar 2. Then move every copied note up to the next shaded row.", "Make bar 2 the same tune, one step higher: D, E, F.", Win.ROLL, LEAD,
                hint = "Select all three notes and tap Copy →. Then drag each copy up one shaded row: C to D, D to E, E to F.",
                check = chk@{ p, _ ->
                    val b2 = barNotes(p, LEAD, 1)
                    when { b2.map { it.startTick % 1920 } != listOf(0, 480, 720) -> no("Same rhythm", "Bar 2 needs the same rhythm as bar 1.")
                        b2.map { it.pitch } != listOf(62, 64, 65) -> no("One step up", "Bar 2 should be D5, E5, F5.")
                        else -> ok("Sequence!", "Same idea, one step higher. Music does this everywhere.") }
                })), flTip = "With Snap to scale on in FL, moved notes snap to scale notes."),
        Lesson("roll.jam-four-bar-melody", 8, "Jam: four-bar melody", "Chord notes on strong beats, other notes in between, little rests and a home ending.", 5, 96, 4,
            show = back4 + LEAD, locked = back4.toSet(), ghost = setOf(CHORDS), rows = 59..84, jam = true, snap = Snap.HALF, setup = melodyBack,
            steps = listOf(LessonStep("Everything together! Chord notes on beats 1 and 3, anything in between, two little rests, and end on C.", "Write a 4 bar melody over the loop.", Win.ROLL, LEAD,
                hint = "Look at the grey chord shapes on beats 1 and 3. Leave a gap in bars 1 or 2, and another in bars 3 or 4.",
                check = chk@{ p, _ ->
                    val ns = notes(p, LEAD)
                    when { ns.size < 6 -> no("More notes", "Write at least 6 notes.")
                        strongBeatChordToneShare(p, LEAD) < 0.85f -> no("Lean on the chords", "Notes on beats 1 and 3 should be chord notes. Check the grey shapes.")
                        (0..3).any { barNotes(p, LEAD, it).size > 8 } -> no("Less crowded", "Use 8 notes per bar at most.")
                        !hasRest(p, LEAD, 240, 0, 1) || !hasRest(p, LEAD, 240, 2, 3) -> no("Breathe", "Leave a gap in bars 1 or 2, and another in bars 3 or 4.")
                        ns.last().pitch % 12 != 0 -> no("Come home", "End on C.")
                        else -> ok("Level 8 complete!", "That's a real 4 bar melody. Export it and play it in FL!") }
                })), flTip = "Export this as MIDI and import it into FL."),
    )

    private fun voiceCost(a: List<Int>, b: List<Int>) = a.sorted().zip(b.sorted()).sumOf { (x, y) -> abs(x - y) }
    private val l9 = listOf(
        Lesson("roll.inversions", 9, "Flip the chord", "An inversion is the same chord with a different note at the bottom.", 3, 90, 1,
            show = listOf(CHORDS), rows = 55..79,
            steps = listOf(LessonStep("Beat 1: C, E, G. Beat 2: move C up an octave, so E, G, C. Beat 3: move E up too, so G, C, E.", "Build C three ways, on beats 1, 2 and 3.", Win.ROLL, CHORDS,
                hint = "Same three letters each time. Only the lowest note changes: C, then E, then G. The ↑ oct button lifts a note one octave.",
                check = chk@{ p, _ ->
                    listOf(Triple(0, 0, "root position"), Triple(480, 4, "1st inversion"), Triple(960, 7, "2nd inversion")).forEach { (t, bass, name) ->
                        val v = Checks.chordAt(p, CHORDS, t, pcs(C), "C ($name)", bassPc = bass); if (!v.pass) return@chk v }
                    ok("Flipped!", "Same chord, three different flavors.")
                })), flTip = "Select the lowest note in FL and press Ctrl+↑."),
        Lesson("roll.voice-leading", 9, "Smooth moves", "Move each chord note to the nearest note of the next chord. Small moves sound smooth.", 4, 96, 4,
            show = listOf(CHORDS), rows = 52..79, setup = { loopChords(it) },
            steps = listOf(LessonStep("Hear how jumpy that sounds? Change G, Am and F so each note moves as little as possible.", "Keep C, G, Am, F, but make every move small.", Win.ROLL, CHORDS,
                hint = "Move just one note per bar by an octave. Bar 2: lift G4 up. Bar 3: lift A4 up. Bar 4: drop C6 down.",
                check = chk@{ p, _ ->
                    val prog = Checks.progression(p, CHORDS, LOOP.map { it.first to pcs(it.second) }); if (!prog.pass) return@chk prog
                    val v = (0..3).map { b -> notes(p, CHORDS).filter { it.startTick == b * 1920 }.map { it.pitch } }
                    if (v.any { it.size != 3 }) return@chk no("Three notes each", "Each chord keeps exactly 3 notes.")
                    val costs = v.zipWithNext { a, b -> voiceCost(a, b) }
                    if (costs.any { it > 5 } || costs.sum() > 10) no("Still jumpy", "Your notes move ${costs.sum()} rows in total. Keep shared notes still. Move the others by 1 or 2 rows.") else ok("Silky!", "Total movement: ${costs.sum()} rows. That's pro voicing.")
                })), flTip = "In FL, stamp a chord then move single notes by octave."),
        Lesson("roll.sevenths-ii-v-i", 9, "Seventh chords: ii, V, I", "Add a 4th note on top of a chord for a jazzy seventh chord.", 4, 82, 4,
            show = listOf(CHORDS), rows = 52..79,
            steps = listOf(LessonStep("Take, skip, take, skip, take, skip, take! Dm7 is D F A C. G7 is G B D F. Cmaj7 is C E G B.", "Dm7, G7 and Cmaj7 in bars 1 to 3. Then C or Cmaj7 in bar 4.", Win.ROLL, CHORDS,
                hint = "Build the 3 note chord first. Then skip one white key above its top note, and add the next one.",
                check = chk@{ p, _ ->
                    listOf(Triple(0, Scales.seventh(2, "m7"), "Dm7"), Triple(1920, Scales.seventh(7, "7"), "G7"), Triple(3840, Scales.seventh(0, "maj7"), "Cmaj7")).forEach { (t, s, nm) ->
                        val v = Checks.chordAt(p, CHORDS, t, s, nm); if (!v.pass) return@chk v }
                    val last = chordPcsAtBar(p, 3)
                    if (last != pcs(C) && last != Scales.seventh(0, "maj7")) no("Go home", "Bar 4: play C or Cmaj7.") else ok("Smooth jazz!", "Chords 2, 5, 1: a favorite in jazz and lo-fi.")
                })), flTip = "FL's Chord stamp has min7, 7 and maj7 — compare with yours."),
        Lesson("roll.relative-minor", 9, "Same chords, new home: A minor", "Start the loop on Am, and A starts to feel like home.", 4, 90, 4,
            show = listOf(CHORDS, BASS), rows = 36..79,
            steps = listOf(LessonStep("Play Am, F, C, G, one per bar. Then switch to Bass and add the roots A, F, C, G. Which chord feels like home?", "Build Am, F, C, G, plus their bass roots.", Win.ROLL, CHORDS,
                hint = "Am is A C E. F is F A C. C is C E G. G is G B D. Bass: A, F, C, G on beat 1 of each bar.",
                check = chk@{ p, _ ->
                    val prog = Checks.progression(p, CHORDS, listOf("Am" to pcs(Am), "F" to pcs(F), "C" to pcs(C), "G" to pcs(G))); if (!prog.pass) return@chk prog
                    val want = listOf(9, 5, 0, 7)
                    for (b in 0..3) if (notes(p, BASS).none { it.startTick == b * 1920 && it.pitch % 12 == want[b] }) return@chk no("Add the bass", "Bar ${b + 1} needs a bass ${Pitch.pcName(want[b])} on beat 1. Switch to the Bass channel.")
                    ok("Minor mood!", "The same notes as C major, but now A is home.")
                })), flTip = "In FL's scale menu, A minor highlights the same white keys as C major."),
        Lesson("roll.dark-trap-melody", 9, "Jam: dark melody in C minor", "C minor is a darker key with three flats. Trap tunes use few notes and lots of space.", 4, 140, 2,
            show = listOf(KICK, CLAP, HAT, CHORDS, LEAD), locked = setOf(KICK, CLAP, HAT, CHORDS), ghost = setOf(CHORDS), rows = 59..79, jam = true, snap = Snap.HALF,
            setup = { p -> drumsFill(drumsFill(drumsFill(p, KICK, intArrayOf(1, 11)), CLAP, intArrayOf(9)), HAT, EIGHTHS).set(CHORDS, chordNotes(listOf(60, 63, 67), 0, 1920) + chordNotes(listOf(56, 60, 63), 1920, 1920)) },
            steps = listOf(LessonStep("C minor uses the black keys E♭, A♭ and B♭. Use few notes, lots of space, and end on C or G.", "Write a dark melody in C minor.", Win.ROLL, LEAD,
                hint = "C minor is C D E♭ F G A♭ B♭. If it sounds sunny, a plain E, A or B snuck in.",
                check = chk@{ p, _ ->
                    val ns = notes(p, LEAD)
                    when { ns.size < 3 -> no("More notes", "Write at least 3 notes.")
                        ns.any { it.pitch % 12 !in Scales.C_MINOR } -> no("Stay dark", "Use only C minor notes: C D E♭ F G A♭ B♭.")
                        (0..1).any { barNotes(p, LEAD, it).size > 6 } -> no("Breathe more", "Use 6 notes per bar at most.")
                        !hasRest(p, LEAD, 480) -> no("Leave space", "Leave a full beat of silence somewhere.")
                        ns.last().pitch % 12 !in setOf(0, 7) -> no("Land it", "End on C or G.")
                        else -> ok("Level 9 complete!", "Dark, spacey and in key. That's a trap melody.") }
                })), flTip = "In FL: Snap to scale → Minor, root C."),
    )

    private val l10 = listOf(
        Lesson("roll.passing-tones", 10, "Passing tones", "A passing tone is an in-between note that fills a gap, off the beat.", 3, 96, 2,
            show = listOf(CHORDS, LEAD), locked = setOf(CHORDS), ghost = setOf(CHORDS), rows = 59..79, snap = Snap.HALF,
            setup = { p -> p.set(CHORDS, chordNotes(C, 0, 1920) + chordNotes(G, 1920, 1920)).set(LEAD, listOf(60, 64, 67, 64, 62, 59, 62, 67).mapIndexed { i, pp -> note(pp, i * 480, 240) }) },
            steps = listOf(LessonStep("C to E skips over D. Add D on the \"and\" between them. Then find one more gap and fill it.", "Add 2 or more in-between notes, each on an \"and\".", Win.ROLL, LEAD,
                hint = "Look for two notes with one white key missing between them. Try D between C and E, or F between E and G.",
                check = chk@{ p, _ ->
                    val ns = notes(p, LEAD)
                    if ((0 until 8).any { i -> ns.none { it.startTick == i * 480 } }) return@chk no("Keep the beat notes", "Don't remove the notes on the beats.")
                    var count = 0
                    for (i in 1 until ns.size - 1) { val a = ns[i - 1]; val x = ns[i]; val b = ns[i + 1]
                        val chord = chordPcsAtBar(p, x.startTick / 1920)
                        if (x.startTick % 480 != 0 && x.pitch % 12 !in chord && (x.pitch - a.pitch) * (b.pitch - x.pitch) > 0 && abs(x.pitch - a.pitch) <= 2 && abs(b.pitch - x.pitch) <= 2) count++ }
                    if (count >= 2) ok("Smooth!", "Passing tones glue the chord notes together.") else no("Find another gap", "You have $count so far. Add a note between two notes that skip one white key.")
                })), flTip = "Set FL's Snap to ½ beat before adding in-between notes."),
        Lesson("roll.counter-melody", 10, "Counter-melody", "A counter-melody is a second tune. It answers in the gaps, lower than the lead.", 4, 96, 4,
            show = listOf(CHORDS, LEAD, LEAD2), locked = setOf(CHORDS, LEAD), ghost = setOf(LEAD, CHORDS), rows = 52..76, scale = Scales.C_MAJOR,
            setup = { p -> loopChords(p).set(LEAD, listOf(67 to 0, 69 to 480, 67 to 960, 72 to 1920, 71 to 2400, 67 to 3840, 69 to 4320, 67 to 4800, 64 to 5760, 60 to 6240).map { (pp, t) -> note(pp, t, 480) }) },
            steps = listOf(LessonStep("A good counter-melody talks when the lead is quiet, not over it. Write Lead 2 in the gaps, lower down.", "Write 4 or more Lead 2 notes, mostly in the gaps, below the lead.", Win.ROLL, LEAD2,
                hint = "The lead is quiet on beat 4 of bars 1 and 3. It is also quiet on beats 3 and 4 of bars 2 and 4. Use E5 or lower.",
                check = chk@{ p, _ ->
                    val l2 = notes(p, LEAD2); val l1 = notes(p, LEAD)
                    fun leadSounding(t: Int) = l1.any { t >= it.startTick && t < it.startTick + it.durationTicks && t == it.startTick }
                    when { l2.size < 4 -> no("More notes", "Write at least 4 notes for Lead 2.")
                        l2.count { !leadSounding(it.startTick) }.toFloat() / l2.size < 0.7f -> no("Answer in the gaps", "Most Lead 2 notes should start where the lead is not starting a note.")
                        l2.map { it.pitch }.sorted()[l2.size / 2] > l1.map { it.pitch }.sorted()[l1.size / 2] - 3 -> no("Go lower", "Keep Lead 2 lower than the lead. Try E5 or below.")
                        l2.any { it.pitch % 12 !in Scales.C_MAJOR } -> no("Stay in key", "Use the shaded rows.")
                        else -> ok("Two voices!", "Counter-melodies make a track feel full.") }
                })), flTip = "In FL, add a second instrument channel and turn on Ghost channels."),
        Lesson("roll.melody-dynamics", 10, "Accent the hook", "Make notes on strong beats louder and the in-between notes softer.", 3, 96, 2,
            show = listOf(LEAD), rows = 59..79,
            setup = { p -> p.set(LEAD, listOf(60 to 0, 62 to 240, 64 to 480, 67 to 960, 64 to 1200, 62 to 1440, 60 to 1920, 64 to 2400, 62 to 2880).map { (pp, t) -> note(pp, t, 240, 100) }) },
            steps = listOf(LessonStep("Remember velocity? Make the notes on beats 1 and 3 loud, and the others softer. Never silent, though!", "Make the beat 1 and beat 3 notes louder than the rest.", Win.ROLL, LEAD,
                hint = "Tap Select. Tap every note that is not on beat 1 or 3, then tap Soft.",
                check = chk@{ p, _ ->
                    val ns = notes(p, LEAD); val strong = ns.filter { it.startTick % 960 == 0 }; val weak = ns - strong.toSet()
                    when { ns.any { it.velocity < 30 } -> no("Too quiet", "Keep every note loud enough to hear.")
                        strong.map { it.velocity }.average() - weak.map { it.velocity }.average() < 20 -> no("More contrast", "Notes on beats 1 and 3 should be clearly louder. Make the others Soft.")
                        else -> ok("It breathes!", "Loud and soft makes a tune sound played by a person.") }
                })), flTip = "In FL's Piano roll event editor, drag a line across velocities."),
        Lesson("pattern.intro-build-drop", 10, "Intro, build, drop", "Take instruments away, then bring them back. The return feels huge.", 4, 120, 2,
            show = listOf(KICK, SNARE, HAT, CHORDS, BASS), rows = 36..76,
            setup = { p -> roots(drumsFill(drumsFill(drumsFill(loopChords(p, LOOP.take(2)), KICK, FOUR), SNARE, intArrayOf(5, 13)), HAT, EIGHTHS), listOf(0, 7)) },
            steps = listOf(
                LessonStep("Songs often start small. Tap the green lights to mute the Kick and the Bass. Hear it waiting?", "Mute the Kick and the Bass.", Win.RACK, KICK, hint = "Tap the little green light on the left of the Kick row and the Bass row.",
                    check = chk@{ p, _ -> if (p.pattern.channel(KICK).muted && p.pattern.channel(BASS).muted) ok("Intro!", "Starting small makes people lean in.") else no("Mute both", "The Kick and Bass lights should both be off.") }),
                LessonStep("Now build excitement. In bar 2, fill the hats with sixteenths and add a snare roll at the end.", "Bar 2: hats on almost every step, and 4 or more snares on steps 25 to 32.", Win.RACK, HAT, hint = "Bar 2 is steps 17 to 32. Fill most hat steps there. Then add snares on steps 25 to 32.",
                    check = chk@{ p, _ ->
                        val hats = (16 until 32).count { p.pattern.channel(HAT).stepOn(it) }; val sn = (24 until 32).count { p.pattern.channel(SNARE).stepOn(it) }
                        if (hats >= 12 && sn >= 4) ok("Tension!", "Busy hats plus a snare roll make a build up.") else no("Build more", "Bar 2 needs 12 or more hats, and 4 or more snares on steps 25 to 32.") }),
                LessonStep("Here comes the drop, when everything crashes back in! Turn the Kick and Bass back on.", "Bring the Kick and Bass back.", Win.RACK, KICK, hint = "Tap their green lights again.",
                    check = chk@{ p, _ -> if (!p.pattern.channel(KICK).muted && !p.pattern.channel(BASS).muted) ok("DROP!", "It hits harder because it was missing.") else no("Unmute", "Turn the Kick and Bass back on.") }),
            ), flTip = "In FL, place Patterns in the Playlist (F5) and switch PAT → SONG to arrange."),
        Lesson("capstone.hook", 10, "Capstone: your four-bar hook", "Use everything you know: drums, chords, bass and a catchy tune.", 10, 96, 4,
            show = listOf(KICK, SNARE, CLAP, HAT, CHORDS, BASS, LEAD), rows = 36..84, jam = true,
            steps = listOf(LessonStep("Your track, your choices! I'll only check the basics. Show me what you've got!", "Make a 4 bar hook with drums, chords, bass and a lead.", Win.RACK, KICK,
                hint = "Kick on step 1 of every bar. Snare or clap on beats 2 and 4. A chord on each beat 1. Bass plays each root. A lead that repeats with a change and ends on C.",
                check = chk@{ p, _ ->
                    for (b in 0..3) {
                        if (!p.pattern.channel(KICK).stepOn(b * 16)) return@chk no("Drums", "Bar ${b + 1}: put a kick on its first step, step ${b * 16 + 1}.")
                        val back = (p.pattern.channel(SNARE).stepOn(b * 16 + 4) && p.pattern.channel(SNARE).stepOn(b * 16 + 12)) || (p.pattern.channel(CLAP).stepOn(b * 16 + 4) && p.pattern.channel(CLAP).stepOn(b * 16 + 12)) || p.pattern.channel(CLAP).stepOn(b * 16 + 8)
                        if (!back) return@chk no("Backbeat", "Bar ${b + 1}: snare or clap on beats 2 and 4. Or one clap on beat 3 for a trap feel.")
                        val ch = chordPcsAtBar(p, b)
                        if (ch.size < 3 || !ch.all { it in Scales.C_MAJOR }) return@chk no("Chords", "Bar ${b + 1}: start a chord of 3 or more white keys on beat 1.")
                        val root = rootPcAtBar(p, b)
                        if (notes(p, BASS).none { it.startTick == b * 1920 && it.pitch % 12 == root }) return@chk no("Bass", "Bar ${b + 1}: the bass plays the chord's root on beat 1.")
                    }
                    val lead = notes(p, LEAD)
                    when { lead.size < 6 -> no("Lead", "Write a lead tune with at least 6 notes.")
                        rhythmSimilarity(onsetsInBar(p, LEAD, 0) + onsetsInBar(p, LEAD, 1).map { it + 1920 }, onsetsInBar(p, LEAD, 2) + onsetsInBar(p, LEAD, 3).map { it + 1920 }) < 0.75f -> no("Repeat the hook", "Bars 3 and 4 should copy the rhythm of bars 1 and 2.")
                        barNotes(p, LEAD, 0).map { it.pitch } + barNotes(p, LEAD, 1).map { it.pitch } == barNotes(p, LEAD, 2).map { it.pitch } + barNotes(p, LEAD, 3).map { it.pitch } -> no("Add a twist", "Change at least one note in bars 3 and 4.")
                        strongBeatChordToneShare(p, LEAD) < 0.75f -> no("Lean on the chords", "Lead notes on beats 1 and 3 should be chord notes.")
                        lead.last().pitch % 12 != 0 -> no("Come home", "End the lead on C.")
                        else -> ok("You made a track!", "Drums, chords, bass and a hook. Export the MIDI and rebuild it in FL Studio.") }
                })), flTip = "Export MIDI, import each channel into FL, then rebuild it from memory in a new Pattern."),
    )

    val lessons: List<Lesson> = l1 + l2 + l3 + l4 + l5 + l6 + l7 + l8 + l9 + l10 + TheoryCurriculum.lessons + PacksCurriculum.lessons
    fun byId(id: String?) = lessons.firstOrNull { it.id == id }
    fun inLevel(level: Int) = lessons.filter { it.level == level }
}
