package com.beatpath.app.lessons

import com.beatpath.app.lessons.Checks.no
import com.beatpath.app.lessons.Checks.notes
import com.beatpath.app.lessons.Checks.ok
import com.beatpath.app.model.ChannelKind
import com.beatpath.app.model.ChannelKind.CHORDS
import com.beatpath.app.model.ChannelKind.LEAD
import com.beatpath.app.model.ChannelKind.LEAD2
import com.beatpath.app.model.Note
import com.beatpath.app.model.Pattern
import com.beatpath.app.model.Pitch
import com.beatpath.app.model.Project
import com.beatpath.app.model.TICKS_PER_BAR
import com.beatpath.app.model.TICKS_PER_BEAT
import kotlin.math.abs

/**
 * Levels 11–18: music theory and melody writing, intermediate → advanced.
 * Keys in every direction, harmony lines, melodic rhythm, minor and modes, richer progressions,
 * motif development, phrase forms and full song melodies. Everything is written in the Piano roll
 * and auto-checked on exactly the rule the lesson teaches.
 * Text is written for young beginners: short sentences, every new word explained where it first appears.
 */
object TheoryCurriculum {

    // ---------------- helpers ----------------
    private fun note(p: Int, tick: Int, len: Int, vel: Int = 100) = Note(Pattern.nextId(), p, tick, len, vel)
    private fun Project.set(k: ChannelKind, ns: List<Note>) = copy(pattern = pattern.setNotes(k, ns))
    private fun chord(ps: List<Int>, tick: Int, len: Int = TICKS_PER_BAR) = ps.map { note(it, tick, len) }
    private fun chords(p: Project, bars: List<List<Int>>) = p.set(CHORDS, bars.flatMapIndexed { i, c -> chord(c, i * TICKS_PER_BAR) })
    private fun pcs(l: List<Int>) = l.map { it % 12 }.toSet()
    private fun lead(p: Project) = notes(p, LEAD)
    private fun bar(p: Project, k: ChannelKind, b: Int) = notes(p, k).filter { it.startTick / TICKS_PER_BAR == b }
    private fun rel(ns: List<Note>, b: Int) = ns.map { it.startTick - b * TICKS_PER_BAR }

    /** Kid-friendly position: "bar 1, beat 2" or "bar 1, the "and" after beat 2". */
    private fun at(t: Int): String {
        val b = t / TICKS_PER_BAR + 1; val r = t % TICKS_PER_BAR; val beat = r / TICKS_PER_BEAT + 1
        return when (r % TICKS_PER_BEAT) {
            0 -> "bar $b, beat $beat"
            TICKS_PER_BEAT / 2 -> "bar $b, the \"and\" after beat $beat"
            else -> Checks.posName(t)
        }
    }

    /** Outline the next missing target note (pitch, tick, length), one at a time. */
    private fun outline(k: ChannelKind, ts: List<Triple<Int, Int, Int>>) = { p: Project ->
        ts.filter { (pp, t, _) -> notes(p, k).none { it.pitch == pp && it.startTick == t } }
            .take(1).map { (pp, t, len) -> Ghost(k, pitch = pp, tick = t, lengthTicks = len) }
    }

    private val G_MAJOR = setOf(7, 9, 11, 0, 2, 4, 6)
    private val F_MAJOR = setOf(5, 7, 9, 10, 0, 2, 4)
    private val D_MAJOR = setOf(2, 4, 6, 7, 9, 11, 1)
    private val A_HARMONIC = setOf(9, 11, 0, 2, 4, 5, 8)
    private val C_SCALE = listOf(0, 2, 4, 5, 7, 9, 11)

    /** Move [pitch] by [degrees] steps inside C major (diatonic transposition). */
    private fun diatonic(pitch: Int, degrees: Int): Int {
        val pc = pitch % 12; val oct = pitch / 12
        val idx = C_SCALE.indexOf(pc).takeIf { it >= 0 } ?: return pitch + degrees * 2
        val t = idx + degrees
        return (oct + Math.floorDiv(t, 7)) * 12 + C_SCALE[Math.floorMod(t, 7)]
    }

    private fun sequenceCheck(p: Project, want: List<Int>, okHead: String, okBody: String, hint: String): Verdict {
        val got = lead(p)
        return when {
            got.map { it.pitch } == want && got.zipWithNext().all { (a, b) -> b.startTick > a.startTick } -> ok(okHead, okBody)
            got.size < want.size -> no("Keep going", "You have ${got.size} of ${want.size} notes. $hint")
            else -> {
                val i = got.indices.firstOrNull { it >= want.size || got[it].pitch != want[it] } ?: want.size
                if (i < want.size && i < got.size) no("Check note ${i + 1}", "Note ${i + 1} should be ${Pitch.flName(want[i])}, not ${Pitch.flName(got[i].pitch)}. $hint")
                else no("Too many notes", "Use exactly ${want.size} notes, one after another, left to right.")
            }
        }
    }

    private fun scaleGuide(want: List<Int>, len: Int = 240) = { p: Project ->
        want.withIndex().firstOrNull { (i, pp) -> lead(p).none { it.pitch == pp && it.startTick == i * len } }
            ?.let { (i, pp) -> listOf(Ghost(LEAD, pitch = pp, tick = i * len, lengthTicks = len)) } ?: emptyList()
    }

    private fun scaleLesson(
        id: String, title: String, idea: String, say: String, want: List<Int>, rows: IntRange, hint: String, tip: String, level: Int = 11,
        okBody: String = "Same recipe as C major: W W H W W W H. Only the starting note changed.",
    ) =
        Lesson(id, level, title, idea, 3, 90, 1, show = listOf(LEAD), rows = rows, snap = Snap.HALF,
            steps = listOf(
                LessonStep(say, "Follow the outlines, from low to high.", Win.ROLL, LEAD, guides = scaleGuide(want), hint = hint,
                    check = { p, _ -> sequenceCheck(p, want, "That's $title!", okBody, hint) }),
                LessonStep("Now build it again with no outlines. Can you remember it?", "Build the scale again from memory.", Win.ROLL, LEAD, setup = { it.set(LEAD, emptyList()) }, hint = hint,
                    check = { p, _ -> sequenceCheck(p, want, "From memory!", "You can build this scale all by yourself now.", hint) }),
            ), flTip = tip)

    /** Lead2 must answer every lead note at the same tick, [lo]..[hi] semitones below, inside [scale]. */
    private fun harmonyCheck(p: Project, lo: Int, hi: Int, name: String, scale: Set<Int> = Scales.C_MAJOR): Verdict {
        val l = lead(p); val h = notes(p, LEAD2)
        val size = if (lo == hi) "$lo" else "$lo or $hi"
        val a = if (name.startsWith("o")) "an" else "a"
        for (n in l) {
            val m = h.firstOrNull { it.startTick == n.startTick } ?: return no("Harmony missing", "Add a note $a $name below ${Pitch.flName(n.pitch)}, at ${at(n.startTick)}.")
            val d = n.pitch - m.pitch
            if (d <= 0) return no("Go below", "At ${at(n.startTick)}, Lead 2 plays ${Pitch.flName(m.pitch)}. It should sit under the lead note ${Pitch.flName(n.pitch)}.")
            if (d !in lo..hi) return no("Not $a $name", "Under ${Pitch.flName(n.pitch)} you have ${Pitch.flName(m.pitch)}, $d half steps lower. You need $size half steps lower.")
            if (m.pitch % 12 !in scale) return no("Stay in key", "${Pitch.flName(m.pitch)} isn't in the key. Use the shaded rows.")
        }
        if (h.size > l.size) return no("One harmony per note", "Lead 2 should only play when the lead plays. Remove the extra notes.")
        return ok("Beautiful harmony!", "Two voices moving together. That's how singers and twin guitars make harmony.")
    }

    // =====================================================================
    val levels = listOf(
        Level(11, "New keys", "G, F and D major, and moving a tune to a new key.", Track.NOTES),
        Level(12, "Harmony lines", "A second voice under the tune: octaves, 3rds and 6ths.", Track.MELODY),
        Level(13, "Tune rhythms", "Long-short notes, off-beat notes, early notes and triplets.", Track.MELODY),
        Level(14, "Minor keys and modes", "Sad scales: natural minor, harmonic minor and Dorian.", Track.HARMONY),
        Level(15, "Chord journeys", "Falling 5ths, a walking bass, surprise chords and going-to chords.", Track.HARMONY),
        Level(16, "Growing a tune", "Stretch an idea, chop it, flip it, and find the peak.", Track.MELODY),
        Level(17, "Shaping a song", "Musical sentences, 8-bar questions and answers, verse and chorus.", Track.MELODY),
        Level(18, "Songwriting", "Chords for a tune, an 8-bar hook, and your final song.", Track.ARRANGE),
    )

    // ---------------- Level 11 · New keys ----------------
    private val l11 = listOf(
        scaleLesson("key.g-major", "G major", "Use the major recipe from G. You need one black key: F♯.",
            "A key is a home note plus its scale. Use the C major recipe, W W H W W W H, but start on G.",
            listOf(55, 57, 59, 60, 62, 64, 66, 67), 53..69, "Start on G4. Go G A B C D E, then the black key F♯, then G5.",
            "In FL: Piano roll → Snap to scale → Major, root G highlights G major."),
        scaleLesson("key.f-major", "F major", "Start the recipe on F. Note 4 becomes a black key: B♭.",
            "Now start on F. Notes 3 and 4 must be a half step apart. So after A comes B♭, not B.",
            listOf(53, 55, 57, 58, 60, 62, 64, 65), 52..67, "Start on F4: F G A, then B♭, the black key just below B. Then C D E F.",
            "Every major key has its own key signature: G has one sharp, F has one flat."),
        Lesson("key.transpose", 11, "Move a tune to a new key", "Move every note up by the same amount. It's the same tune, in a new key.", 3, 96, 1,
            show = listOf(LEAD), rows = 59..84, setup = { it.set(LEAD, listOf(60 to 0, 62 to 240, 64 to 480, 60 to 960, 64 to 1200, 65 to 1440, 67 to 1680).map { (pp, t) -> note(pp, t, 240) }) },
            steps = listOf(LessonStep("This tune is in C. Move every note up 7 half steps. Then it's the same tune, in G!", "Move the whole tune up 7 half steps.", Win.ROLL, LEAD,
                hint = "With the Select tool, tap every note. Then tap ↑ semi 7 times. Or tap ↑ oct once, then ↓ semi 5 times.",
                check = chk@{ p, _ ->
                    val want = listOf(67 to 0, 69 to 240, 71 to 480, 67 to 960, 71 to 1200, 72 to 1440, 74 to 1680)
                    val got = lead(p).map { it.pitch to it.startTick }
                    if (got == want) ok("Same tune, new key!", "Singers do this all the time, to fit a song to their voice.")
                    else if (got.map { it.second } != want.map { it.second }) no("Keep the rhythm", "Only move notes up. Don't slide them left or right, and keep all 7.")
                    else {
                        val d = got.first().first - 60
                        val i = got.indices.first { got[it] != want[it] }
                        if (d != 7) no("Not quite", "The first note moved ${if (d > 0) "up $d" else if (d < 0) "down ${-d}" else "0"} half steps. Move every note up 7, so C becomes G.")
                        else no("Not quite", "At ${at(got[i].second)} you have ${Pitch.flName(got[i].first)}. It should be ${Pitch.flName(want[i].first)}. Move every note up the same amount.")
                    }
                })), flTip = "In FL, Ctrl+A selects all notes in the Piano roll; Shift+↑ moves them a semitone."),
        Lesson("key.jam-d-major", 11, "Jam: a tune in D major", "D major has two black keys: F♯ and C♯.", 3, 96, 2,
            show = listOf(LEAD), rows = 59..79, scale = D_MAJOR, snap = Snap.HALF, jam = true,
            steps = listOf(LessonStep("The shaded rows are D major. Write 6 to 12 notes. Move mostly by steps, and end on D.", "Write a D major tune of 6 to 12 notes. End on D.", Win.ROLL, LEAD,
                hint = "A step is the next shaded row. Use F♯ and C♯, never plain F or C. Finish on D5 or D6.",
                check = { p, _ -> Checks.melody(p, LEAD, Checks.MelodyRules(minNotes = 6, scale = D_MAJOR, minStepwise = 0.5f, endsOnPc = 2), "A tune in a brand new key. Your ears and hands know D major now.")
                    .let { if (it.pass && lead(p).size > 12) no("Too many notes", "Use 12 notes or fewer.") else it } })),
            flTip = "Set FL's scale highlighting to D major to see the same shaded rows."),
    )

    // ---------------- Level 12 · Harmony lines ----------------
    private val hook = listOf(64 to 0, 65 to 240, 67 to 480, 69 to 960, 67 to 1440, 72 to 1920, 71 to 2400, 69 to 2880, 67 to 3360)
    private fun hookSetup(p: Project) = p.set(LEAD, hook.map { (pp, t) -> note(pp, t, 240) })
    private val l12 = listOf(
        Lesson("harm.octave", 12, "Double it an octave down", "Play the same tune 12 half steps lower. It sounds bigger, with no new notes.", 2, 90, 2,
            show = listOf(LEAD, LEAD2), locked = setOf(LEAD), ghost = setOf(LEAD), rows = 48..76, snap = Snap.HALF, setup = ::hookSetup,
            steps = listOf(LessonStep("Lead 2 is a second voice. Copy the locked tune onto Lead 2, one octave lower.", "Lead 2: the same notes, one octave lower, at the same times.", Win.ROLL, LEAD2,
                hint = "Same letter, 12 half steps down: E5 becomes E4. The 2nd note starts on the \"and\" after beat 1.",
                check = { p, _ -> harmonyCheck(p, 12, 12, "octave") })),
            flTip = "In FL, select notes and press Ctrl+↓ to drop them an octave."),
        Lesson("harm.thirds", 12, "Harmony in 3rds", "A 3rd below each note is the sweetest harmony.", 4, 90, 2,
            show = listOf(LEAD, LEAD2), locked = setOf(LEAD), ghost = setOf(LEAD), rows = 52..76, scale = Scales.C_MAJOR, snap = Snap.HALF, setup = ::hookSetup,
            steps = listOf(LessonStep("Now put a 3rd below each lead note. On the shaded rows, that's two rows down.", "Lead 2: a 3rd below every lead note, at the same time.", Win.ROLL, LEAD2,
                hint = "Count 3 letters down, starting on the lead note. Under E put C. Under F put D. Under G put E. Under C6 put A5.",
                check = { p, _ -> harmonyCheck(p, 3, 4, "3rd") })),
            flTip = "In FL, select the lead notes, Ctrl+C, paste into a second channel and move them down two scale rows."),
        Lesson("harm.sixths", 12, "Harmony in 6ths", "A 6th below sounds wider and warmer than a 3rd.", 3, 90, 2,
            show = listOf(LEAD, LEAD2), locked = setOf(LEAD), ghost = setOf(LEAD), rows = 48..76, scale = Scales.C_MAJOR, snap = Snap.HALF, setup = ::hookSetup,
            steps = listOf(LessonStep("Now go further down: a 6th. Count 6 letters down from each lead note. That's five shaded rows.", "Lead 2: a 6th below every lead note, at the same time.", Win.ROLL, LEAD2,
                hint = "Under E5, count E D C B A G: put G4. Under F5 put A4. Under G5 put B4.",
                check = { p, _ -> harmonyCheck(p, 8, 9, "6th") })),
            flTip = "Thirds and sixths are the classic harmony intervals — try both on a desktop lead in FL."),
    )

    // ---------------- Level 13 · Tune rhythms ----------------
    private fun syncoCheck(p: Project): Verdict {
        val ns = lead(p)
        if (ns.size < 3) return no("More notes", "Write at least 3 notes.")
        val offs = ns.filter { it.startTick % TICKS_PER_BEAT == 240 }
        return when {
            offs.size < 2 -> no("More off-beat notes", "Start at least 2 notes on an \"and\", halfway between two beats.")
            offs.none { it.durationTicks >= TICKS_PER_BEAT } -> no("Hold one over", "Make one \"and\" note last 1 beat or more, so it rings past the next beat.")
            ns.any { it.pitch % 12 !in Scales.C_MAJOR } -> no("Stay in key", "Use only the shaded rows.")
            else -> ok("Bouncy!", "Notes between the beats make a tune bounce. Pop and R&B songs love this.")
        }
    }
    private val dotted = listOf(0 to 720, 720 to 240, 960 to 720, 1680 to 240)
    private val l13 = listOf(
        Lesson("rhy.dotted", 13, "Long-short rhythms", "A dotted note is one and a half times as long. Long, short, long, short.", 3, 96, 1,
            show = listOf(LEAD), rows = 59..79, snap = Snap.HALF,
            steps = listOf(LessonStep("A dot makes a note longer by half. Copy the outlines: long, short, long, short. Any pitch is fine.", "Copy the rhythm: 1½ beats, ½ beat, 1½ beats, ½ beat.", Win.ROLL, LEAD,
                guides = { p -> dotted.filter { (t, d) -> lead(p).none { it.startTick == t && it.durationTicks == d } }.take(1).map { (t, d) -> Ghost(LEAD, pitch = 64, tick = t, lengthTicks = d) } },
                hint = "Notes start on beat 1, the \"and\" after beat 2, beat 3, and the \"and\" after beat 4. Long notes fill 3 small cells.",
                check = { p, _ ->
                    val got = lead(p).map { it.startTick to it.durationTicks }
                    if (got == dotted) ok("Dotted!", "Long-short gives a skipping, marching feel.")
                    else dotted.firstOrNull { it !in got }?.let { (t, d) -> no("Check the rhythm", "At ${at(t)} you need a note ${if (d == 720) "1½ beats" else "½ beat"} long.") }
                        ?: no("Extra notes", "Use only 4 notes: long, short, long, short.")
                })), flTip = "Drag the right edge of notes in FL with Snap ½ beat to make dotted lengths."),
        Lesson("rhy.syncopation", 13, "Off-beat tune", "Syncopation: start notes on the \"and\" and let them ring over the beat.", 3, 96, 1,
            show = listOf(LEAD), rows = 59..79, scale = Scales.C_MAJOR, snap = Snap.HALF,
            steps = listOf(
                LessonStep("Syncopation means notes start between the beats. It makes a tune bounce. Follow the outlines.", "Copy the outlined notes.", Win.ROLL, LEAD,
                    guides = outline(LEAD, listOf(Triple(64, 0, 240), Triple(67, 240, 480), Triple(65, 720, 240), Triple(64, 960, 960))),
                    hint = "The 2nd note starts on the \"and\" after beat 1 and rings past beat 2.",
                    check = { p, _ -> syncoCheck(p) }),
                LessonStep("Now make your own. Put 2 notes on an \"and\", and let one ring past the next beat.", "Write 3+ notes: 2 on an \"and\", 1 held over a beat.", Win.ROLL, LEAD,
                    setup = { it.set(LEAD, emptyList()) },
                    hint = "The thin lines between the beat lines are the \"and\"s. Make one of those notes 1 beat long or more.",
                    check = { p, _ -> syncoCheck(p) }),
            ), flTip = "In FL set Snap to ½ beat to place notes on the ands."),
        Lesson("rhy.anticipation", 13, "Arrive early", "An early note plays the next chord's note half a beat before it starts.", 3, 96, 2,
            show = listOf(CHORDS, LEAD), locked = setOf(CHORDS), ghost = setOf(CHORDS), rows = 59..79, scale = Scales.C_MAJOR, snap = Snap.HALF,
            setup = { p -> chords(p, listOf(listOf(60, 64, 67), listOf(55, 59, 62))).set(LEAD, listOf(note(64, 0, 480), note(65, 480, 480), note(64, 960, 480))) },
            steps = listOf(LessonStep("Bar 2 is a G chord. Sneak in a G chord note half a beat early. Let it ring into bar 2.", "Start G, B or D on the last \"and\" of bar 1. Hold it into bar 2.", Win.ROLL, LEAD,
                hint = "Bar 1, the \"and\" after beat 4: the last thin line before bar 2. Make the note 1 beat long.",
                hintGhosts = listOf(Ghost(LEAD, pitch = 67, tick = TICKS_PER_BAR - 240, lengthTicks = TICKS_PER_BEAT)),
                check = { p, _ ->
                    val a = lead(p).firstOrNull { it.startTick == TICKS_PER_BAR - 240 }
                    when { a == null -> no("Arrive early", "Start a note in bar 1, on the \"and\" after beat 4.")
                        a.pitch % 12 !in setOf(7, 11, 2) -> no("Use the next chord", "That note should be G, B or D, the notes of the G chord.")
                        a.durationTicks < TICKS_PER_BEAT -> no("Hold it over", "Make it 1 beat long or more, so it rings into bar 2.")
                        else -> ok("Early bird!", "Arriving early makes a tune sound excited. It gets there before the band.") }
                })), flTip = "Anticipations are everywhere in pop toplines — try nudging a downbeat note ½ beat early in FL."),
        Lesson("rhy.triplets", 13, "Triplets", "A triplet squeezes three equal notes into one beat.", 3, 90, 1,
            show = listOf(LEAD), rows = 59..79, scale = Scales.C_MAJOR, snap = Snap.TRIPLET,
            steps = listOf(
                LessonStep("Snap is now ⅓ beat, so each beat has 3 small cells. Say \"trip-a-let\". Follow the outlines in beat 1.", "Put three notes in beat 1.", Win.ROLL, LEAD,
                    guides = outline(LEAD, listOf(Triple(64, 0, 160), Triple(65, 160, 160), Triple(67, 320, 160))),
                    hint = "Tap the 3 small cells inside beat 1, one after another.",
                    check = { p, _ ->
                        if (lead(p).map { it.startTick }.filter { it < 480 }.toSet() == setOf(0, 160, 320)) ok("Trip-a-let!", "Three even notes in one beat.")
                        else no("Three even notes", "Beat 1 needs 3 notes, one in each small cell.")
                    }),
                LessonStep("Now fill beat 2 with three more, on your own. Hear it roll?", "Put three notes in beat 2 as well.", Win.ROLL, LEAD,
                    hint = "Beat 2 also has 3 small cells. Put one note in each.",
                    check = { p, _ ->
                        val on = lead(p).map { it.startTick }.filter { it < 960 }
                        if (on.toSet() == setOf(0, 160, 320, 480, 640, 800)) ok("Triplets!", "Three notes in a beat give a rolling, galloping feel.") else no("Three in each beat", "Beats 1 and 2 each need 3 notes, one in each small cell.")
                    }),
            ), flTip = "In FL's Snap menu pick 1/3 beat for triplets."),
    )

    // ---------------- Level 14 · Minor keys and modes ----------------
    private val l14 = listOf(
        scaleLesson("minor.natural", "A natural minor", "Start on A and use only white keys. It sounds sadder than C major.",
            "A minor uses the same white keys as C major. It just starts on A. Hear how it sounds sadder?",
            listOf(57, 59, 60, 62, 64, 65, 67, 69), 55..71, "White keys only, from A4 up to A5: A B C D E F G A.", "In FL's scale menu, choose Minor (aeolian), root A.", level = 14,
            okBody = "Natural minor's recipe is W H W W H W W. Same keys as C major, new home."),
        scaleLesson("minor.harmonic", "A harmonic minor", "Raise note 7 a half step: G becomes G♯ and pulls hard up to A.",
            "Harmonic minor changes one note. Note 7, G, goes up to G♯. Now it leans hard into A.",
            listOf(57, 59, 60, 62, 64, 65, 68, 69), 55..71, "Like A natural minor, but note 7 is the black key G♯, just below A5.", "FL's scale menu has Harmonic minor too.", level = 14,
            okBody = "One raised note gives a big, dramatic, movie-like sound."),
        scaleLesson("mode.dorian", "D Dorian", "A mode is a scale that starts on a different white key. Dorian starts on D.",
            "A mode uses the white keys but starts somewhere new. Start on D. It sounds minor, with one bright note: B.",
            listOf(62, 64, 65, 67, 69, 71, 72, 74), 60..76, "White keys only, from D5 up to D6: D E F G A B C D.", "Dorian is all over funk and lo-fi. FL calls it Dorian in the scale menu.", level = 14,
            okBody = "D to F is 3 half steps, so it sounds minor. Note 6, B, adds a cool, bright color."),
        Lesson("minor.melody", 14, "Jam: a minor tune", "Use G♯ over the E chord for a dramatic minor pull.", 4, 90, 4,
            show = listOf(CHORDS, LEAD), locked = setOf(CHORDS), ghost = setOf(CHORDS), rows = 55..79, snap = Snap.HALF, jam = true,
            setup = { p -> chords(p, listOf(listOf(57, 60, 64), listOf(62, 65, 69), listOf(56, 59, 64), listOf(57, 60, 64))) },
            steps = listOf(LessonStep("The chords are Am, Dm, E, Am. Write a tune in A minor. Over the E chord in bar 3, use G♯. End on A.", "Write 6+ notes in A minor. Put a G♯ in bar 3 and end on A.", Win.ROLL, LEAD,
                hint = "The E chord is E, G♯, B. G♯ is the black key just below A. It wants to step up to A.",
                check = chk@{ p, _ ->
                    val ns = lead(p)
                    when { ns.size < 6 -> no("More notes", "Write at least 6 notes.")
                        ns.any { it.pitch % 12 !in (A_HARMONIC + 7) } -> no("Stay in A minor", "Use A B C D E F G, plus G♯ over the E chord.")
                        bar(p, LEAD, 2).none { it.pitch % 12 == 8 } -> no("Use G♯", "Bar 3 is the E chord. Put a G♯ somewhere in bar 3.")
                        ns.last().pitch % 12 != 9 -> no("Come home", "Make your last note an A.")
                        else -> ok("Level 14 complete!", "That G♯ to A pull is the sound of minor-key drama.") }
                })), flTip = "Try the same progression in FL with a string or piano sound — instant film score."),
    )

    // ---------------- Level 15 · Chord journeys ----------------
    /** Each bar: chord name + the voicing shown as outlines. Step 1 is outlined, step 2 is from memory. */
    private fun progLesson(id: String, title: String, idea: String, say: String, bars: List<Pair<String, List<Int>>>, hint: String, tip: String,
                           bass: List<Int?>? = null, done: String = "Great progression!"): Lesson {
        val names = bars.joinToString(", ") { it.first }
        fun verdict(p: Project, head: String): Verdict {
            bars.forEachIndexed { i, (name, voicing) ->
                val v = Checks.chordAt(p, CHORDS, i * TICKS_PER_BAR, pcs(voicing), name, bassPc = bass?.getOrNull(i))
                if (!v.pass) return v.copy(body = "Bar ${i + 1}: " + v.body)
            }
            return ok(head, bars.joinToString(" → ") { it.first } + ". Play it and feel where it pulls.")
        }
        val guide = { p: Project ->
            bars.withIndex().firstOrNull { (i, b) -> !Checks.chordAt(p, CHORDS, i * TICKS_PER_BAR, pcs(b.second), b.first, bassPc = bass?.getOrNull(i)).pass }
                ?.let { (i, b) -> b.second.filter { pp -> notes(p, CHORDS).none { it.pitch == pp && it.startTick == i * TICKS_PER_BAR } }
                    .map { Ghost(CHORDS, pitch = it, tick = i * TICKS_PER_BAR, lengthTicks = TICKS_PER_BAR) } } ?: emptyList()
        }
        return Lesson(id, 15, title, idea, 5, 90, bars.size, show = listOf(CHORDS), rows = 43..79,
            steps = listOf(
                LessonStep(say, "Follow the outlines: $names.", Win.ROLL, CHORDS, guides = guide, hint = hint, check = { p, _ -> verdict(p, "Nice chords!") }),
                LessonStep("Now build it again with no outlines. You can do it!", "Build $names from memory.", Win.ROLL, CHORDS,
                    setup = { it.set(CHORDS, emptyList()) }, hint = hint, check = { p, _ -> verdict(p, done) }),
            ), flTip = tip)
    }
    private val l15 = listOf(
        progLesson("prog.circle", "Chord chain: falling 5ths", "Each chord's root falls a 5th to the next: Am → Dm → G → C.",
            "Count 5 letters down from A: A G F E D. So Am falls to Dm. Keep falling by 5ths and you reach C, home!",
            listOf("Am" to listOf(57, 60, 64), "Dm" to listOf(62, 65, 69), "G" to listOf(55, 59, 62), "C" to listOf(60, 64, 67)),
            "Am = A C E. Dm = D F A. G = G B D. C = C E G. One chord per bar, on beat 1.", "This chain powers jazz standards and countless pop songs."),
        progLesson("prog.bassline", "Walking bass down", "Keep simple chords, but choose the bottom note: C, B, A, G walks down.",
            "G/B means a G chord with B at the bottom, like a flipped chord. The bottom notes walk down: C, B, A, G.",
            listOf("C" to listOf(60, 64, 67), "G/B" to listOf(59, 62, 67), "Am" to listOf(57, 60, 64), "Am/G" to listOf(55, 57, 60, 64)),
            "C = C E G. G/B = B D G, with B lowest. Am = A C E. Am/G = A minor with an extra G at the very bottom.",
            "Slash chords like G/B are written the same way in FL's chord stamp.",
            bass = listOf(0, 11, 9, 7)),
        progLesson("prog.borrowed", "A surprise color chord", "Borrow Fm from C minor for a bittersweet surprise in a C major song.",
            "C, F, Fm, C. Fm is borrowed from C minor, like a surprise color. Just move F's middle note, A, down to A♭.",
            listOf("C" to listOf(60, 64, 67), "F" to listOf(53, 57, 60), "Fm" to listOf(53, 56, 60), "C" to listOf(60, 64, 67)),
            "F = F A C. Fm = F A♭ C. A♭ is the black key just below A. Only the middle note moves.", "Borrowed iv is the emotional chord in many ballads."),
        progLesson("prog.secondary-dominant", "A going-to chord", "A7 is a chord that wants to go to Dm. It pulls hard, like G7 pulls to C.",
            "G7 wants to go home to C. A7 does the same job for Dm. Its C♯ is a black key that leans up into D.",
            listOf("C" to listOf(60, 64, 67), "A7" to listOf(57, 61, 64, 67), "Dm" to listOf(62, 65, 69), "G7" to listOf(55, 59, 62, 65)),
            "C = C E G. A7 is G7 moved up 2 half steps: A C♯ E G. Dm = D F A. G7 = G B D F.", "Secondary dominants are the secret behind \"Beatles-y\" chord moves.",
            done = "Level 15 complete!"),
    )

    // ---------------- Level 16 · Growing a tune ----------------
    private val motif = listOf(60 to 0, 64 to 240, 62 to 480, 67 to 960)
    private val motif2 = listOf(64 to 0, 65 to 240, 69 to 480, 67 to 960)
    private fun motifNotes(m: List<Pair<Int, Int>>) = m.map { (pp, t) -> note(pp, t, if (t == 960) 480 else 240) }
    /** Mirror [m]'s scale-step moves, starting from [start]. */
    private fun mirror(m: List<Int>, start: Int) = m.zipWithNext { a, b -> C_SCALE.indexOf(b % 12) - C_SCALE.indexOf(a % 12) + 7 * (b / 12 - a / 12) }
        .runningFold(start) { acc, d -> diatonic(acc, -d) }
    private fun invertCheck(p: Project, m: List<Pair<Int, Int>>, start: Int, okHead: String, okBody: String): Verdict {
        val want = mirror(m.map { it.first }, start)
        val b2 = bar(p, LEAD, 1)
        return when { rel(b2, 1) != m.map { it.second } -> no("Same rhythm", "Bar 2 needs bar 1's rhythm: beat 1, the \"and\" after beat 1, beat 2 and beat 3.")
            b2.map { it.pitch } != want -> no("Mirror the moves", "Bar 2 should be ${want.joinToString(" ") { Pitch.flName(it) }}.")
            else -> ok(okHead, okBody) }
    }
    private val l16 = listOf(
        Lesson("dev.augment", 16, "Stretch the idea", "Augment means make bigger. Same notes, but each one lasts twice as long.", 3, 96, 2,
            show = listOf(LEAD), rows = 59..79, scale = Scales.C_MAJOR,
            setup = { p -> p.set(LEAD, listOf(note(60, 0, 240), note(64, 240, 240), note(67, 480, 480))) },
            steps = listOf(LessonStep("Bar 1 has a quick little idea. Play it again in bar 2, with every note twice as long.", "Bar 2: C for 1 beat, E for 1 beat, G for 2 beats.", Win.ROLL, LEAD,
                hint = "Bar 2: C5 on beat 1, E5 on beat 2, G5 on beat 3. Hold G5 to the end of the bar.",
                hintGhosts = listOf(Ghost(LEAD, pitch = 60, tick = 1920, lengthTicks = 480), Ghost(LEAD, pitch = 64, tick = 2400, lengthTicks = 480), Ghost(LEAD, pitch = 67, tick = 2880, lengthTicks = 960)),
                check = { p, _ ->
                    val b2 = bar(p, LEAD, 1).map { Triple(it.pitch, it.startTick - TICKS_PER_BAR, it.durationTicks) }
                    if (b2 == listOf(Triple(60, 0, 480), Triple(64, 480, 480), Triple(67, 960, 960))) ok("Stretched!", "A slow version of an idea sounds big and grand. Great for endings.")
                    else no("Double every length", "Bar 2 needs C5 (1 beat), E5 (1 beat), then G5 (2 beats), from beat 1.")
                })), flTip = "In FL, select notes and use Tools → Scale levels/length (Alt+X) or just drag each end."),
        Lesson("dev.fragment", 16, "Chop and climb", "Take a small piece of the idea and repeat it, one step higher each time.", 4, 96, 2,
            show = listOf(LEAD), rows = 59..79, scale = Scales.C_MAJOR, snap = Snap.HALF,
            setup = { p -> p.set(LEAD, listOf(note(60, 0, 240), note(62, 240, 240), note(64, 480, 480), note(67, 960, 480))) },
            steps = listOf(LessonStep("Take just the first two notes, C D. In bar 2, play that pair three times, one shaded row higher each time.", "Bar 2: D E, then E F, then F G, on beats 1, 2 and 3.", Win.ROLL, LEAD,
                hint = "Each pair is two ½-beat notes that start on a beat. Beat 1: D E. Beat 2: E F. Beat 3: F G.",
                hintGhosts = listOf(62 to 0, 64 to 240, 64 to 480, 65 to 720, 65 to 960, 67 to 1200).map { (pp, t) -> Ghost(LEAD, pitch = pp, tick = TICKS_PER_BAR + t, lengthTicks = 240) },
                check = { p, _ ->
                    val want = listOf(62 to 0, 64 to 240, 64 to 480, 65 to 720, 65 to 960, 67 to 1200)
                    val b2 = bar(p, LEAD, 1).map { it.pitch to it.startTick - TICKS_PER_BAR }
                    if (b2 == want) ok("Climbing!", "A small piece climbing step by step builds excitement before a big moment.")
                    else no("Climb in pairs", "Bar 2 should be D E, E F, F G. Each pair starts on a beat: 1, 2 and 3.")
                })), flTip = "Duplicate (Ctrl+B) then nudge with Shift+↑ in FL to build sequences fast."),
        Lesson("dev.invert", 16, "Flip it upside down", "Mirror the idea: where it went up, go down by the same number of shaded rows.", 4, 96, 2,
            show = listOf(LEAD), rows = 55..79, scale = Scales.C_MAJOR, snap = Snap.HALF,
            setup = { p -> p.set(LEAD, motifNotes(motif)) },
            steps = listOf(
                LessonStep("This flip is different from flipping a chord. Picture the tune in a mirror: up becomes down, down becomes up.", "Bar 2: the idea upside down, starting on G. Follow the outlines.", Win.ROLL, LEAD,
                    guides = outline(LEAD, listOf(Triple(67, 1920, 240), Triple(64, 2160, 240), Triple(65, 2400, 240), Triple(60, 2880, 480))),
                    hint = "The idea goes up 2 rows, down 1, up 3. Flipped, starting on G: down 2 (E), up 1 (F), down 3 (C).",
                    check = { p, _ -> invertCheck(p, motif, 67, "Flipped!", "Same rhythm, moves turned upside down.") }),
                LessonStep("A new idea! Flip this one on your own. Start bar 2 on the same note, E.", "Bar 2: flip the new idea, starting on E5, same rhythm.", Win.ROLL, LEAD,
                    setup = { p -> p.set(LEAD, motifNotes(motif2)) },
                    hint = "This idea goes up 1 row, up 2, down 1. Flipped from E: down 1, down 2, up 1.",
                    check = { p, _ -> invertCheck(p, motif2, 64, "Flipped on your own!", "Bach loved this trick. The flipped idea still sounds like it belongs.") }),
            ), flTip = "FL's Piano roll Tools menu has a Flip function; doing it by hand teaches what it means."),
        Lesson("dev.climax", 16, "Jam: find the peak", "A tune's one highest note sounds best about two thirds of the way through.", 4, 96, 4,
            show = listOf(CHORDS, LEAD), locked = setOf(CHORDS), ghost = setOf(CHORDS), rows = 59..84, scale = Scales.C_MAJOR, snap = Snap.HALF, jam = true,
            setup = { p -> chords(p, listOf(listOf(60, 64, 67), listOf(57, 60, 64), listOf(53, 57, 60), listOf(55, 59, 62))) },
            steps = listOf(LessonStep("Write 10 or more notes over 4 bars. Climb slowly, reach just one highest note in bar 3, then come down.", "10+ notes. Just one highest note, and it's in bar 3.", Win.ROLL, LEAD,
                hint = "Climb a little in bars 1 and 2. Reach your top note in bar 3. Then step back down to the end.",
                check = chk@{ p, _ ->
                    val ns = lead(p)
                    if (ns.size < 10) return@chk no("More notes", "Write at least 10 notes. You have ${ns.size}.")
                    val top = ns.maxOf { it.pitch }
                    val peakBar = ns.first { it.pitch == top }.startTick / TICKS_PER_BAR
                    when { ns.count { it.pitch == top } > 1 -> no("Just one peak", "${ns.count { it.pitch == top }} notes share the top row, ${Pitch.flName(top)}. Only one note should be the highest.")
                        peakBar != 2 -> no("Peak in bar 3", "Your highest note, ${Pitch.flName(top)}, is in bar ${peakBar + 1}. Move the peak to bar 3.")
                        ns.any { it.pitch % 12 !in Scales.C_MAJOR } -> no("Stay in key", "Use only the shaded rows.")
                        else -> ok("Level 16 complete!", "A peak in the right place makes a tune feel like a journey.") }
                })), flTip = "Zoom out in FL to see the contour — the peak should stand out."),
    )

    // ---------------- Level 17 · Shaping a song ----------------
    private val l17 = listOf(
        Lesson("form.sentence", 17, "The musical sentence", "Say an idea, say it again a bit different, then keep going to an ending.", 5, 96, 4,
            show = listOf(LEAD), rows = 59..84, scale = Scales.C_MAJOR, snap = Snap.HALF,
            steps = listOf(LessonStep("Bar 1: a short idea. Bar 2: the same rhythm with new notes. Bars 3 and 4: more notes that end on C.", "Write a 4-bar sentence that ends on C.", Win.ROLL, LEAD,
                hint = "Copy bar 1 into bar 2, then move a note up or down. Bars 3 and 4 need at least as many notes as bars 1 and 2.",
                check = chk@{ p, _ ->
                    val b1 = bar(p, LEAD, 0); val b2 = bar(p, LEAD, 1)
                    when { b1.size < 2 -> no("An idea first", "Put 2 or more notes in bar 1.")
                        rel(b1, 0) != rel(b2, 1) -> no("Repeat the rhythm", "Bar 2 needs exactly the same rhythm as bar 1.")
                        b1.map { it.pitch } == b2.map { it.pitch } -> no("Change a note", "Bar 2 is an exact copy. Move at least one of its notes up or down.")
                        bar(p, LEAD, 2).size + bar(p, LEAD, 3).size < b1.size + b2.size -> no("Keep going", "Bars 3 and 4 need at least ${b1.size + b2.size} notes, as many as bars 1 and 2.")
                        lead(p).last().pitch % 12 != 0 -> no("End home", "Make your last note a C.")
                        lead(p).any { it.pitch % 12 !in Scales.C_MAJOR } -> no("Stay in key", "Use only the shaded rows.")
                        else -> ok("A sentence!", "Idea, idea again, then onward to the end. Beethoven used this shape all the time.") }
                })), flTip = "Many FL toplines follow this: two short calls, then a longer line."),
        Lesson("form.period", 17, "8-bar question and answer", "A 4-bar question that ends open, then a 4-bar answer that ends at home.", 6, 96, 8,
            show = listOf(CHORDS, LEAD), locked = setOf(CHORDS), ghost = setOf(CHORDS), rows = 59..84, scale = Scales.C_MAJOR, snap = Snap.HALF,
            setup = { p -> chords(p, listOf(listOf(60, 64, 67), listOf(53, 57, 60), listOf(55, 59, 62), listOf(55, 59, 62), listOf(60, 64, 67), listOf(53, 57, 60), listOf(55, 59, 62), listOf(60, 64, 67))) },
            steps = listOf(
                LessonStep("Remember question and answer? Now each part is 4 bars long. First, the question in bars 1 to 4.", "Bars 1 to 4: write 4+ notes. End bar 4 on G, B or D.", Win.ROLL, LEAD,
                    hint = "Bar 4 is a G chord. End there on G, B or D, so it sounds like a question.",
                    check = chk@{ p, _ ->
                        val q = lead(p).filter { it.startTick < 4 * TICKS_PER_BAR }
                        when { q.size < 4 -> no("More notes", "Write at least 4 notes in bars 1 to 4.")
                            q.last().pitch % 12 !in setOf(7, 11, 2) -> no("Leave it open", "End bars 1 to 4 on G, B or D, not on ${Pitch.pcName(q.last().pitch % 12)}.")
                            q.any { it.pitch % 12 !in Scales.C_MAJOR } -> no("Stay in key", "Use only the shaded rows.")
                            else -> ok("Good question!", "It sounds open, like it's waiting. Now let's answer it.") }
                    }),
                LessonStep("Now the answer, in bars 5 to 8. Start bar 5 with bar 1's rhythm, and land on C at the end.", "Bars 5 to 8: 4+ notes. Bar 5 copies bar 1's rhythm. End on C in bar 8.", Win.ROLL, LEAD,
                    hint = "Copy bar 1 into bar 5, then change the notes after it. Your last note goes in bar 8, and it's a C.",
                    check = chk@{ p, _ ->
                        val q = lead(p).filter { it.startTick < 4 * TICKS_PER_BAR }; val a = lead(p).filter { it.startTick >= 4 * TICKS_PER_BAR }
                        when { q.size < 4 || a.size < 4 -> no("Both halves", "Write at least 4 notes in bars 1 to 4, and 4 in bars 5 to 8.")
                            q.last().pitch % 12 !in setOf(7, 11, 2) -> no("Leave the question open", "End bar 4 on G, B or D.")
                            a.last().pitch % 12 != 0 || a.last().startTick < 7 * TICKS_PER_BAR -> no("Answer at home", "Your last note should be a C, in bar 8.")
                            rel(bar(p, LEAD, 0), 0).toSet() != rel(bar(p, LEAD, 4), 4).toSet() -> no("Echo the start", "Bar 5 should use the same rhythm as bar 1.")
                            lead(p).any { it.pitch % 12 !in Scales.C_MAJOR } -> no("Stay in key", "Use only the shaded rows.")
                            else -> ok("A real 8-bar tune!", "This question-and-answer shape is in folk, pop and classical tunes.") }
                    }),
            ), flTip = "In FL, make bars 1–4 and copy them to 5–8 (Ctrl+B), then change only the ending."),
        Lesson("form.verse-chorus", 17, "Verse and chorus", "The chorus sits higher than the verse and repeats its rhythm.", 5, 96, 4,
            show = listOf(LEAD), rows = 55..84, scale = Scales.C_MAJOR, snap = Snap.HALF,
            steps = listOf(LessonStep("Bars 1 and 2 are the verse: keep it low. Bars 3 and 4 are the chorus: go higher, and repeat a rhythm.", "Low verse in bars 1 and 2. Higher chorus in bars 3 and 4, with bar 4 copying bar 3's rhythm.", Win.ROLL, LEAD,
                hint = "Verse: G5 and below. Chorus: try C6 and above. Copy bar 3 into bar 4, then change notes if you like.",
                check = chk@{ p, _ ->
                    val v = lead(p).filter { it.startTick < 2 * TICKS_PER_BAR }; val c = lead(p).filter { it.startTick >= 2 * TICKS_PER_BAR }
                    if (v.size < 3 || c.size < 3) return@chk no("Both parts", "Write 3 or more notes in the verse, and 3 or more in the chorus.")
                    when { v.any { it.pitch > 67 } -> no("Verse lower", "Keep every verse note at G5 or lower.")
                        c.map { it.pitch }.average() < v.map { it.pitch }.average() + 4 -> no("Lift the chorus", "The chorus should sit clearly higher. Try notes from C6 up.")
                        rel(bar(p, LEAD, 2), 2) != rel(bar(p, LEAD, 3), 3) -> no("Repeat the chorus rhythm", "Bar 4 should use the same rhythm as bar 3.")
                        else -> ok("Level 17 complete!", "A low verse and a high, catchy chorus. That's the shape of most hit songs.") }
                })), flTip = "In FL, put the verse and chorus in different Patterns and arrange them in the Playlist."),
    )

    // ---------------- Level 18 · Songwriting ----------------
    private val tune = listOf(64 to 0, 62 to 480, 60 to 960, 62 to 1440, 67 to 1920, 65 to 2400, 64 to 2880, 62 to 3360, 60 to 3840, 64 to 4320, 69 to 4800, 67 to 5280, 67 to 5760)
    private val diatonicTriads = listOf(setOf(0, 4, 7), setOf(2, 5, 9), setOf(4, 7, 11), setOf(5, 9, 0), setOf(7, 11, 2), setOf(9, 0, 4))
    private val l18 = listOf(
        Lesson("song.harmonize", 18, "Find chords for a tune", "Pick a chord that has the tune's beat 1 note inside it.", 6, 90, 4,
            show = listOf(LEAD, CHORDS), locked = setOf(LEAD), ghost = setOf(LEAD), rows = 48..79,
            setup = { p -> p.set(LEAD, tune.map { (pp, t) -> note(pp, t, 480) }) },
            steps = listOf(LessonStep("The tune is ready. Give each bar a chord on beat 1. The chord must contain the tune's note on beat 1.", "One chord per bar, on beat 1, that fits the tune.", Win.ROLL, CHORDS,
                hint = "Bar 1 starts on E: C, Em or Am. Bar 2 starts on G: C, Em or G. Bar 3 starts on C: C, F or Am. Bar 4 starts on G: C, Em or G.",
                check = chk@{ p, _ ->
                    for (b in 0..3) {
                        val m = lead(p).first { it.startTick == b * TICKS_PER_BAR }.pitch % 12
                        val c = notes(p, CHORDS).filter { it.startTick == b * TICKS_PER_BAR }.map { it.pitch % 12 }.toSet()
                        if (c.size < 3) return@chk no("Bar ${b + 1}: add a chord", "Build a 3-note chord on beat 1 of bar ${b + 1}.")
                        if (c !in diatonicTriads) return@chk no("Bar ${b + 1}: stay in key", "Use a chord from C major: C, Dm, Em, F, G or Am.")
                        if (m !in c) return@chk no("Bar ${b + 1}: include the tune", "The tune plays ${Pitch.pcName(m)} on beat 1 here. Pick a chord that has ${Pitch.pcName(m)} in it.")
                    }
                    ok("Harmonised!", "That's how songwriters find chords for a tune they hummed.")
                })), flTip = "In FL, put the melody on one channel, turn on ghost channels, and try chords under it."),
        Lesson("song.hook", 18, "Jam: an 8-bar hook", "Repeat, change, climb and come home, over C G Am F twice.", 8, 100, 8,
            show = listOf(CHORDS, LEAD), locked = setOf(CHORDS), ghost = setOf(CHORDS), rows = 59..84, snap = Snap.HALF, jam = true,
            setup = { p -> chords(p, List(8) { listOf(listOf(60, 64, 67), listOf(55, 59, 62), listOf(57, 60, 64), listOf(53, 57, 60))[it % 4] }) },
            steps = listOf(LessonStep("Bars 5 to 8 echo bars 1 to 4 but change something. Use chord notes on beats 1 and 3. Peak late, end on C.", "Write an 8-bar hook of 10+ notes that ends on C.", Win.ROLL, LEAD,
                hint = "Write 4 bars. Select them and tap Copy →. Then make the copy's ending higher or different.",
                check = chk@{ p, _ ->
                    val ns = lead(p)
                    if (ns.size < 10) return@chk no("More notes", "Write at least 10 notes. You have ${ns.size}.")
                    val first = ns.filter { it.startTick < 4 * TICKS_PER_BAR }; val second = ns.filter { it.startTick >= 4 * TICKS_PER_BAR }
                    val strong = ns.filter { it.startTick % TICKS_PER_BAR in setOf(0, 960) }
                    val ct = strong.count { n -> n.pitch % 12 in notes(p, CHORDS).filter { it.startTick == n.startTick / TICKS_PER_BAR * TICKS_PER_BAR }.map { it.pitch % 12 } }
                    when { first.isEmpty() || second.isEmpty() -> no("Both halves", "Write notes in bars 1 to 4 and in bars 5 to 8.")
                        rel(first, 0).toSet().intersect(second.map { it.startTick - 4 * TICKS_PER_BAR }.toSet()).size.toFloat() / maxOf(1, rel(first, 0).size) < 0.6f -> no("Echo the first half", "Bars 5 to 8 should reuse most of the rhythm of bars 1 to 4.")
                        first.map { it.pitch } == second.map { it.pitch } -> no("Change something", "Bars 5 to 8 are an exact copy. Change a few notes.")
                        strong.isNotEmpty() && ct.toFloat() / strong.size < 0.75f -> no("Lean on the chords", "Notes on beats 1 and 3 should be chord notes. Check the outlines.")
                        second.maxOf { it.pitch } < first.maxOf { it.pitch } -> no("Lift the second half", "Put your highest note in bars 5 to 8.")
                        ns.last().pitch % 12 != 0 -> no("Resolve", "Make your last note a C.")
                        else -> ok("That's a hook!", "Repeat, change, and a late peak. That's the recipe behind catchy songs.") }
                })), flTip = "Export it as MIDI and drop it on a lead synth in FL."),
        Lesson("song.final", 18, "Final song: tune and harmony", "Everything together: chords, your tune, and a harmony line under it.", 10, 96, 4,
            show = listOf(CHORDS, LEAD, LEAD2), locked = setOf(CHORDS), ghost = setOf(CHORDS, LEAD), rows = 48..84, scale = Scales.C_MAJOR, snap = Snap.HALF, jam = true,
            setup = { p -> chords(p, listOf(listOf(60, 64, 67), listOf(57, 60, 64), listOf(53, 57, 60), listOf(55, 59, 62))) },
            steps = listOf(
                LessonStep("Write your 4-bar tune on Lead. Use chord notes on beats 1 and 3, and leave a little gap somewhere.", "Write a tune of 8+ notes on Lead.", Win.ROLL, LEAD,
                    hint = "The chords are C, Am, F, G. Try ending on G, B or D, so the loop rolls around again.",
                    check = chk@{ p, _ ->
                        val ns = lead(p)
                        val strong = ns.filter { it.startTick % TICKS_PER_BAR in setOf(0, 960) }
                        val ct = strong.count { n -> n.pitch % 12 in notes(p, CHORDS).filter { it.startTick == n.startTick / TICKS_PER_BAR * TICKS_PER_BAR }.map { it.pitch % 12 } }
                        when { ns.size < 8 -> no("More notes", "Write at least 8 notes. You have ${ns.size}.")
                            strong.isEmpty() || ct.toFloat() / strong.size < 0.75f -> no("Lean on the chords", "Notes on beats 1 and 3 should mostly be chord notes.")
                            ns.zipWithNext().none { (a, b) -> b.startTick - (a.startTick + a.durationTicks) >= 240 } -> no("Breathe", "Leave a gap of half a beat or more between two notes.")
                            ns.any { it.pitch % 12 !in Scales.C_MAJOR } -> no("Stay in key", "Use only the shaded rows.")
                            else -> ok("Tune done!", "Now give it a partner.") }
                    }),
                LessonStep("Now Lead 2. Put a 3rd or 6th under 6 or more of your notes, at the same moments.", "Lead 2: a 3rd or 6th below 6 or more of your notes.", Win.ROLL, LEAD2,
                    hint = "A 3rd is two shaded rows down. A 6th is five shaded rows down. Start each one with its Lead note.",
                    check = { p, _ ->
                        val pairs = notes(p, LEAD2).count { h -> lead(p).any { it.startTick == h.startTick && (it.pitch - h.pitch) in (setOf(3, 4, 8, 9)) } }
                        when { pairs < 6 -> no("More harmony", "$pairs of your notes have a 3rd or 6th under them. Aim for 6 or more.")
                            notes(p, LEAD2).any { it.pitch % 12 !in Scales.C_MAJOR } -> no("Stay in key", "Use only the shaded rows.")
                            else -> ok("Course complete!", "Chords, a tune and a harmony line. You can write music! Export it and build it in FL Studio.") }
                    }),
            ), flTip = "In FL, put the harmony on a second channel with a softer sound, and pan the two leads apart."),
    )

    val lessons: List<Lesson> = l11 + l12 + l13 + l14 + l15 + l16 + l17 + l18

    @Suppress("unused") private fun unusedAbs() = abs(0)
}
