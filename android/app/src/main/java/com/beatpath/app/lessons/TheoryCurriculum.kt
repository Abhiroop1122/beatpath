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
                else no("Too many notes", "Use exactly ${want.size} notes, left to right.")
            }
        }
    }

    private fun scaleGuide(want: List<Int>, len: Int = 240) = { p: Project ->
        want.withIndex().firstOrNull { (i, pp) -> lead(p).none { it.pitch == pp && it.startTick == i * len } }
            ?.let { (i, pp) -> listOf(Ghost(LEAD, pitch = pp, tick = i * len, lengthTicks = len)) } ?: emptyList()
    }

    private fun scaleLesson(id: String, title: String, idea: String, say: String, want: List<Int>, rows: IntRange, hint: String, tip: String, level: Int = 11) =
        Lesson(id, level, title, idea, 3, 90, 1, show = listOf(LEAD), rows = rows, snap = Snap.HALF,
            steps = listOf(
                LessonStep(say, "Build it from the outline, low to high.", Win.ROLL, LEAD, guides = scaleGuide(want), hint = hint,
                    check = { p, _ -> sequenceCheck(p, want, "That's $title!", "Same W-W-H-W-W-W-H recipe — just a new starting note.", hint) }),
                LessonStep("Again from memory — no outline.", "Build it again without help.", Win.ROLL, LEAD, setup = { it.set(LEAD, emptyList()) }, hint = hint,
                    check = { p, _ -> sequenceCheck(p, want, "From memory!", "You can build this scale anywhere now.", hint) }),
            ), flTip = tip)

    /** Lead2 must answer every lead note at the same tick, [lo]..[hi] semitones below, inside [scale]. */
    private fun harmonyCheck(p: Project, lo: Int, hi: Int, name: String, scale: Set<Int> = Scales.C_MAJOR): Verdict {
        val l = lead(p); val h = notes(p, LEAD2)
        for (n in l) {
            val m = h.firstOrNull { it.startTick == n.startTick } ?: return no("Harmony missing", "Add a $name under ${Pitch.flName(n.pitch)} at ${Checks.posName(n.startTick)}.")
            val d = n.pitch - m.pitch
            if (d !in lo..hi) return no("Not a $name", "Under ${Pitch.flName(n.pitch)} you have ${Pitch.flName(m.pitch)} ($d semitones below). A $name is $lo–$hi semitones below.")
            if (m.pitch % 12 !in scale) return no("Stay in key", "${Pitch.flName(m.pitch)} isn't in the key — use the shaded rows.")
        }
        if (h.size > l.size) return no("One harmony per note", "Lead 2 should only play where the lead plays.")
        return ok("Beautiful harmony!", "Two voices moving together — that's how vocal harmonies and twin leads work.")
    }

    // =====================================================================
    val levels = listOf(
        Level(11, "Keys and scales II", "G, F and D major; moving a melody to a new key.", Track.NOTES),
        Level(12, "Harmony lines", "Thirds, sixths and octaves under a melody.", Track.MELODY),
        Level(13, "Melodic rhythm", "Syncopation, anticipation, dotted notes, triplets.", Track.MELODY),
        Level(14, "Minor keys and modes", "Natural & harmonic minor, Dorian, minor melodies.", Track.HARMONY),
        Level(15, "Progressions II", "Circle of fifths, secondary dominants, borrowed chords.", Track.HARMONY),
        Level(16, "Melody development", "Augment, invert, fragment, and place the climax.", Track.MELODY),
        Level(17, "Phrases and form", "Periods, sentences, verse vs chorus.", Track.MELODY),
        Level(18, "Songwriting", "Harmonise a tune, an 8-bar hook, your final song.", Track.ARRANGE),
    )

    // ---------------- Level 11 · Keys and scales II ----------------
    private val l11 = listOf(
        scaleLesson("key.g-major", "G major", "Start the major recipe on G — you'll need one black key, F♯.",
            "Same recipe as C major — W W H W W W H — but start on G. One note becomes a black key.",
            listOf(55, 57, 59, 60, 62, 64, 66, 67), 53..69, "From G: whole, whole, half, whole, whole, whole, half. The 7th note is F♯.",
            "In FL: Piano roll → Snap to scale → Major, root G highlights G major."),
        scaleLesson("key.f-major", "F major", "Start on F and the 4th note becomes B♭.",
            "Now from F. Watch the half step between notes 3 and 4 — that's where B♭ comes in.",
            listOf(53, 55, 57, 58, 60, 62, 64, 65), 52..67, "From F: whole, whole, half — so A to B♭, not B.",
            "Every major key has its own key signature: G has one sharp, F has one flat."),
        Lesson("key.transpose", 11, "Move a melody to a new key", "Transposing: every note moves by the same number of semitones.", 3, 96, 1,
            show = listOf(LEAD), rows = 59..84, setup = { it.set(LEAD, listOf(60 to 0, 62 to 240, 64 to 480, 60 to 960, 64 to 1200, 65 to 1440, 67 to 1680).map { (pp, t) -> note(pp, t, 240) }) },
            steps = listOf(LessonStep("This tune is in C. Move ALL of it up 7 semitones and it's the same tune in G.", "Transpose the whole melody up 7 semitones.", Win.ROLL, LEAD,
                hint = "Select every note (Select tool), then Shift ↑ seven times — or Ctrl ↑ once and Shift ↓ five times.",
                check = chk@{ p, _ ->
                    val want = listOf(67 to 0, 69 to 240, 71 to 480, 67 to 960, 71 to 1200, 72 to 1440, 74 to 1680)
                    val got = lead(p).map { it.pitch to it.startTick }
                    if (got == want) ok("Same tune, new key!", "Singers do this all the time to fit their voice.")
                    else if (got.map { it.second } != want.map { it.second }) no("Keep the rhythm", "Only move notes up — don't change their timing.")
                    else { val d = got.first().first - 60; no("Not quite", "You moved it $d semitones. G is 7 semitones above C.") }
                })), flTip = "In FL, Ctrl+A selects all notes in the Piano roll; Shift+↑ moves them a semitone."),
        Lesson("key.jam-d-major", 11, "Jam: a melody in D major", "D major has two sharps: F♯ and C♯.", 3, 96, 2,
            show = listOf(LEAD), rows = 59..79, scale = D_MAJOR, snap = Snap.HALF, jam = true,
            steps = listOf(LessonStep("Shaded rows are D major. Write 6–12 notes, mostly steps, and end on D.", "Write a D-major melody that ends on D.", Win.ROLL, LEAD,
                hint = "Remember: F♯ and C♯, never F or C.",
                check = { p, _ -> Checks.melody(p, LEAD, Checks.MelodyRules(minNotes = 6, scale = D_MAJOR, minStepwise = 0.5f, endsOnPc = 2), "A melody in a new key — your ears and hands both know D major now.")
                    .let { if (it.pass && lead(p).size > 12) no("A bit long", "12 notes max.") else it } })),
            flTip = "Set FL's scale highlighting to D major to see the same shaded rows."),
    )

    // ---------------- Level 12 · Harmony lines ----------------
    private val hook = listOf(64 to 0, 65 to 240, 67 to 480, 69 to 960, 67 to 1440, 72 to 1920, 71 to 2400, 69 to 2880, 67 to 3360)
    private fun hookSetup(p: Project) = p.set(LEAD, hook.map { (pp, t) -> note(pp, t, 240) })
    private val l12 = listOf(
        Lesson("harm.thirds", 12, "Harmony in thirds", "A third below each note — the sweetest harmony.", 4, 90, 2,
            show = listOf(LEAD, LEAD2), locked = setOf(LEAD), ghost = setOf(LEAD), rows = 52..76, scale = Scales.C_MAJOR, setup = ::hookSetup,
            steps = listOf(LessonStep("The lead is locked. On Lead 2, put a note a third below each lead note — skip one shaded row down.", "Lead 2: a third below every lead note, same timing.", Win.ROLL, LEAD2,
                hint = "Under E put C, under F put D, under G put E… (two shaded rows down = 3 or 4 semitones).",
                check = { p, _ -> harmonyCheck(p, 3, 4, "third") })),
            flTip = "In FL, select the lead notes, Ctrl+C, paste into a second channel and move them down two scale rows."),
        Lesson("harm.sixths", 12, "Harmony in sixths", "A sixth below sounds wider and richer than a third.", 3, 90, 2,
            show = listOf(LEAD, LEAD2), locked = setOf(LEAD), ghost = setOf(LEAD), rows = 48..76, scale = Scales.C_MAJOR, setup = ::hookSetup,
            steps = listOf(LessonStep("Now go further down: a sixth below each note — five shaded rows down.", "Lead 2: a sixth (8–9 semitones) below every lead note.", Win.ROLL, LEAD2,
                hint = "A sixth below is the same as a third above, moved down an octave. Under E5 put G4.",
                check = { p, _ -> harmonyCheck(p, 8, 9, "sixth") })),
            flTip = "Thirds and sixths are the classic harmony intervals — try both on a desktop lead in FL."),
        Lesson("harm.octave", 12, "Double it an octave down", "Octave doubling makes a hook fat without new notes.", 2, 90, 2,
            show = listOf(LEAD, LEAD2), locked = setOf(LEAD), ghost = setOf(LEAD), rows = 48..76, setup = ::hookSetup,
            steps = listOf(LessonStep("Copy the hook exactly 12 semitones lower on Lead 2.", "Lead 2: the same hook, one octave below.", Win.ROLL, LEAD2,
                hint = "Every Lead 2 note = lead note − 12. E5 → E4.",
                check = { p, _ -> harmonyCheck(p, 12, 12, "octave") })),
            flTip = "In FL, select notes and press Ctrl+↓ to drop them an octave."),
    )

    // ---------------- Level 13 · Melodic rhythm ----------------
    private val l13 = listOf(
        Lesson("rhy.syncopation", 13, "Syncopated melody", "Start notes on the \"and\" and hold them across the beat.", 3, 96, 1,
            show = listOf(LEAD), rows = 59..79, scale = Scales.C_MAJOR, snap = Snap.HALF,
            steps = listOf(LessonStep("Put at least two notes on the \"and\" (between beats), and let one of them ring over the next beat.", "2+ notes on an \"and\", one held across a beat.", Win.ROLL, LEAD,
                hint = "Snap is ½ beat: the lines between the strong beat lines are the ands.",
                check = chk@{ p, _ ->
                    val ns = lead(p)
                    if (ns.size < 3) return@chk no("More notes", "Write at least 3 notes.")
                    val offs = ns.filter { it.startTick % TICKS_PER_BEAT == 240 }
                    when { offs.size < 2 -> no("More off-beats", "Start at least two notes on an \"and\".")
                        offs.none { it.durationTicks >= TICKS_PER_BEAT } -> no("Hold one over", "Make one off-beat note last a full beat or more, so it crosses the next beat.")
                        ns.any { it.pitch % 12 !in Scales.C_MAJOR } -> no("Stay in key", "Use the shaded rows.")
                        else -> ok("Funky!", "Syncopation makes melodies bounce — pop and R&B live on it.") }
                })), flTip = "In FL set Snap to ½ beat to place notes on the ands."),
        Lesson("rhy.anticipation", 13, "Anticipation", "Arrive at the next chord's note half a beat early.", 3, 96, 2,
            show = listOf(CHORDS, LEAD), locked = setOf(CHORDS), ghost = setOf(CHORDS), rows = 59..79, scale = Scales.C_MAJOR, snap = Snap.HALF,
            setup = { p -> chords(p, listOf(listOf(60, 64, 67), listOf(55, 59, 62))).set(LEAD, listOf(note(64, 0, 480), note(65, 480, 480), note(64, 960, 480))) },
            steps = listOf(LessonStep("Bar 2 is a G chord. Put a G-chord note (G, B or D) half a beat BEFORE bar 2 and hold it across the bar line.", "A G, B or D starting at bar 1 beat 4-and, held over into bar 2.", Win.ROLL, LEAD,
                hint = "Start it on the last ½-beat line of bar 1 and make it at least a beat long.",
                check = { p, _ ->
                    val a = lead(p).firstOrNull { it.startTick == TICKS_PER_BAR - 240 }
                    when { a == null -> no("Arrive early", "Start a note on the \"and\" of beat 4 in bar 1.")
                        a.pitch % 12 !in setOf(7, 11, 2) -> no("Use the next chord", "That note should belong to G: G, B or D.")
                        a.durationTicks < TICKS_PER_BEAT -> no("Hold it over", "Make it last at least a beat so it crosses into bar 2.")
                        else -> ok("Pushed!", "Anticipations make a melody feel eager — it gets there before the band.") }
                })), flTip = "Anticipations are everywhere in pop toplines — try nudging a downbeat note ½ beat early in FL."),
        Lesson("rhy.dotted", 13, "Dotted rhythms", "A dotted note is 1½ times as long: long-short, long-short.", 3, 96, 1,
            show = listOf(LEAD), rows = 59..79, snap = Snap.HALF,
            steps = listOf(LessonStep("Copy the outlined long-short rhythm: 1½ beats, ½ beat, 1½ beats, ½ beat. Any notes you like.", "Rhythm: 1½, ½, 1½, ½ beats.", Win.ROLL, LEAD,
                guides = { p -> listOf(0 to 720, 720 to 240, 960 to 720, 1680 to 240).filter { (t, d) -> lead(p).none { it.startTick == t && it.durationTicks == d } }.take(1).map { (t, d) -> Ghost(LEAD, pitch = 64, tick = t, lengthTicks = d) } },
                hint = "Notes start on beat 1, the \"and\" of 2, beat 3 and the \"and\" of 4.",
                check = { p, _ ->
                    val got = lead(p).map { it.startTick to it.durationTicks }
                    if (got == listOf(0 to 720, 720 to 240, 960 to 720, 1680 to 240)) ok("Dotted!", "Long-short gives a lilting, marching feel.") else no("Check the lengths", "Long notes are 1½ beats (3 half-beat cells), short ones ½ beat.")
                })), flTip = "Drag the right edge of notes in FL with Snap ½ beat to make dotted lengths."),
        Lesson("rhy.triplets", 13, "Triplets", "Three equal notes squeezed into one beat.", 3, 90, 1,
            show = listOf(LEAD), rows = 59..79, scale = Scales.C_MAJOR, snap = Snap.TRIPLET,
            steps = listOf(LessonStep("Snap is now ⅓ beat. Fill beats 1 and 2 with three notes each — a rolling triplet feel.", "Six notes: three in beat 1, three in beat 2.", Win.ROLL, LEAD,
                hint = "Tap three cells inside beat 1, then three inside beat 2.",
                check = { p, _ ->
                    val on = lead(p).map { it.startTick }.filter { it < 960 }
                    if (on.toSet() == setOf(0, 160, 320, 480, 640, 800)) ok("Triplets!", "Three-in-one gives a swing or waltz-like roll.") else no("Even thirds", "Each beat needs notes at its start, ⅓ and ⅔ of the way through.")
                })), flTip = "In FL's Snap menu pick 1/3 beat for triplets."),
    )

    // ---------------- Level 14 · Minor keys and modes ----------------
    private val l14 = listOf(
        scaleLesson("minor.natural", "A natural minor", "Start on A and use only white keys: the relative minor of C.",
            "A minor uses the same white keys as C major — it just starts on A. Hear how it sounds sadder?",
            listOf(57, 59, 60, 62, 64, 65, 67, 69), 55..71, "White keys only, from A4 up to A5.", "In FL's scale menu, choose Minor (aeolian), root A.", level = 14),
        scaleLesson("minor.harmonic", "A harmonic minor", "Raise the 7th note: G becomes G♯ and pulls hard back to A.",
            "Harmonic minor raises the 7th — G♯ instead of G. That gap gives it an exotic, dramatic sound.",
            listOf(57, 59, 60, 62, 64, 65, 68, 69), 55..71, "Like natural minor, but the 7th note is G♯ (a black key).", "FL's scale menu has Harmonic minor too.", level = 14),
        scaleLesson("mode.dorian", "D Dorian", "White keys from D: minor, but with a bright 6th (B).",
            "Start on D and use only white keys. It's minor — but the 6th note, B, adds a cool, jazzy brightness.",
            listOf(62, 64, 65, 67, 69, 71, 72, 74), 60..76, "White keys only, from D5 up to D6.", "Dorian is all over funk and lo-fi. FL calls it Dorian in the scale menu.", level = 14),
        Lesson("minor.melody", 14, "Jam: a minor melody", "Use G♯ over the E chord for that dramatic minor pull.", 4, 90, 4,
            show = listOf(CHORDS, LEAD), locked = setOf(CHORDS), ghost = setOf(CHORDS), rows = 55..79, snap = Snap.HALF, jam = true,
            setup = { p -> chords(p, listOf(listOf(57, 60, 64), listOf(62, 65, 69), listOf(56, 59, 64), listOf(57, 60, 64))) },
            steps = listOf(LessonStep("Chords: Am – Dm – E – Am. Write a melody in A minor; over the E chord (bar 3) use G♯, and end on A.", "A-minor melody with a G♯ in bar 3, ending on A.", Win.ROLL, LEAD,
                hint = "G♯ is the black key just below A. It wants to resolve up to A.",
                check = chk@{ p, _ ->
                    val ns = lead(p)
                    when { ns.size < 6 -> no("More notes", "Write at least 6 notes.")
                        ns.any { it.pitch % 12 !in (A_HARMONIC + 7) } -> no("Stay in A minor", "Use A B C D E F G (and G♯ over E).")
                        bar(p, LEAD, 2).none { it.pitch % 12 == 8 } -> no("Use G♯", "Bar 3 is the E chord — put a G♯ in it.")
                        ns.last().pitch % 12 != 9 -> no("Come home", "End on A.")
                        else -> ok("Level 14 complete!", "That G♯ → A pull is the sound of minor-key drama.") }
                })), flTip = "Try the same progression in FL with a string or piano sound — instant film score."),
    )

    // ---------------- Level 15 · Progressions II ----------------
    private fun progLesson(id: String, title: String, idea: String, say: String, bars: List<Pair<String, Set<Int>>>, hint: String, tip: String, bass: List<Int?>? = null) =
        Lesson(id, 15, title, idea, 4, 90, bars.size, show = listOf(CHORDS), rows = 43..79,
            steps = listOf(LessonStep(say, "Build: ${bars.joinToString(" – ") { it.first }}.", Win.ROLL, CHORDS, hint = hint,
                check = chk@{ p, _ ->
                    bars.forEachIndexed { i, (name, set) ->
                        val v = Checks.chordAt(p, CHORDS, i * TICKS_PER_BAR, set, name, bassPc = bass?.getOrNull(i))
                        if (!v.pass) return@chk v.copy(body = "Bar ${i + 1}: " + v.body)
                    }
                    ok("Great progression!", bars.joinToString(" → ") { it.first } + ". Play it and feel where it pulls.")
                })), flTip = tip)
    private val l15 = listOf(
        progLesson("prog.circle", "Circle of fifths", "Each chord's root falls a fifth: Am → Dm → G → C.",
            "Roots falling by fifths sound like they're always heading home. Build Am, Dm, G, C.",
            listOf("Am" to setOf(9, 0, 4), "Dm" to setOf(2, 5, 9), "G" to setOf(7, 11, 2), "C" to setOf(0, 4, 7)),
            "Dm = D F A. G = G B D.", "This chain powers jazz standards and countless pop songs."),
        progLesson("prog.secondary-dominant", "Secondary dominant", "A7 is the \"five\" of Dm: it pulls hard into it.",
            "Make bar 2 an A7 — A, C♯, E, G. The C♯ is borrowed; it yanks the ear toward Dm.",
            listOf("C" to setOf(0, 4, 7), "A7" to setOf(9, 1, 4, 7), "Dm" to setOf(2, 5, 9), "G7" to setOf(7, 11, 2, 5)),
            "A7 = A C♯ E G. G7 = G B D F.", "Secondary dominants are the secret behind \"Beatles-y\" chord moves."),
        progLesson("prog.borrowed", "Borrowed chord", "F minor in a C-major song: a bittersweet twist.",
            "C – F – Fm – C. In bar 3, lower F's middle note A to A♭: a chord borrowed from C minor.",
            listOf("C" to setOf(0, 4, 7), "F" to setOf(5, 9, 0), "Fm" to setOf(5, 8, 0), "C" to setOf(0, 4, 7)),
            "Fm = F A♭ C. Only the middle note moves down.", "Borrowed iv is the emotional chord in many ballads."),
        progLesson("prog.bassline", "Descending bass line", "Keep the chords, walk the lowest note down: C – G/B – Am – Am/G.",
            "Same chords, but choose the bottom note: C, then B, then A, then G — a walking-down bass.",
            listOf("C" to setOf(0, 4, 7), "G/B" to setOf(7, 11, 2), "Am" to setOf(9, 0, 4), "Am/G" to setOf(9, 0, 4, 7)),
            "G/B means a G chord with B as its lowest note. Am/G adds G under A minor.", "Slash chords like G/B are written the same way in FL's chord stamp.",
            bass = listOf(0, 11, 9, 7)),
    )

    // ---------------- Level 16 · Melody development ----------------
    private val motif = listOf(60 to 0, 64 to 240, 62 to 480, 67 to 960)
    private val l16 = listOf(
        Lesson("dev.augment", 16, "Augmentation", "Same notes, every length doubled — the idea slows down.", 3, 96, 2,
            show = listOf(LEAD), rows = 59..79, scale = Scales.C_MAJOR,
            setup = { p -> p.set(LEAD, listOf(note(60, 0, 240), note(64, 240, 240), note(67, 480, 480))) },
            steps = listOf(LessonStep("Bar 1 is a quick motif. Write it again in bar 2 with every note twice as long.", "Bar 2: C, E, G with lengths 1, 1 and 2 beats.", Win.ROLL, LEAD,
                hint = "Bar 2 starts at 1920. C for 1 beat, E for 1 beat, G for 2 beats.",
                check = { p, _ ->
                    val b2 = bar(p, LEAD, 1).map { Triple(it.pitch, it.startTick - TICKS_PER_BAR, it.durationTicks) }
                    if (b2 == listOf(Triple(60, 0, 480), Triple(64, 480, 480), Triple(67, 960, 960))) ok("Augmented!", "Slowing a motif makes it sound grand — great for endings.")
                    else no("Double everything", "Bar 2 should be C (1 beat), E (1 beat), G (2 beats), back to back from beat 1.")
                })), flTip = "In FL, select notes and use Tools → Scale levels/length (Alt+X) or just drag each end."),
        Lesson("dev.invert", 16, "Inversion", "Mirror the motif: where it went up, go down by the same number of scale steps.", 4, 96, 2,
            show = listOf(LEAD), rows = 55..79, scale = Scales.C_MAJOR,
            setup = { p -> p.set(LEAD, motif.map { (pp, t) -> note(pp, t, if (t == 960) 480 else 240) }) },
            steps = listOf(LessonStep("The motif goes C up to E, down to D, up to G. Start bar 2 on G and mirror each move: down, up, down — same number of shaded rows.", "Bar 2: the motif upside-down, starting on G, same rhythm.", Win.ROLL, LEAD,
                hint = "Moves were +2, −1, +3 scale steps. Mirrored from G: −2 (E), +1 (F), −3 (C).",
                check = { p, _ ->
                    val want = motif.map { it.first }.zipWithNext { a, b -> C_SCALE.indexOf(b % 12) - C_SCALE.indexOf(a % 12) + 7 * (b / 12 - a / 12) }
                        .runningFold(67) { acc, d -> diatonic(acc, -d) }
                    val b2 = bar(p, LEAD, 1)
                    when { rel(b2, 1) != motif.map { it.second } -> no("Same rhythm", "Bar 2 needs the motif's rhythm.")
                        b2.map { it.pitch } != want -> no("Mirror the moves", "Bar 2 should be ${want.joinToString(" ") { Pitch.flName(it) }}.")
                        else -> ok("Inverted!", "Bach loved this trick — the same idea, flipped, still belongs.") }
                })), flTip = "FL's Piano roll Tools menu has a Flip function; doing it by hand teaches what it means."),
        Lesson("dev.fragment", 16, "Fragmentation", "Take just a piece of the motif and sequence it upward.", 4, 96, 2,
            show = listOf(LEAD), rows = 59..79, scale = Scales.C_MAJOR, snap = Snap.HALF,
            setup = { p -> p.set(LEAD, listOf(note(60, 0, 240), note(62, 240, 240), note(64, 480, 480), note(67, 960, 480))) },
            steps = listOf(LessonStep("Take only the first two notes (C D). In bar 2 play them three times, each pair one shaded row higher: D E, E F, F G.", "Bar 2: D E · E F · F G on beats 1, 2, 3 (half-beat notes).", Win.ROLL, LEAD,
                hint = "Each pair is two half-beat notes starting on a beat.",
                check = { p, _ ->
                    val want = listOf(62 to 0, 64 to 240, 64 to 480, 65 to 720, 65 to 960, 67 to 1200)
                    val b2 = bar(p, LEAD, 1).map { it.pitch to it.startTick - TICKS_PER_BAR }
                    if (b2 == want) ok("Developed!", "A fragment climbing in steps builds energy — a classic pre-chorus move.") else no("Climb in pairs", "Bar 2: D E, then E F, then F G, each pair starting on a beat.")
                })), flTip = "Duplicate (Ctrl+B) then nudge with Shift+↑ in FL to build sequences fast."),
        Lesson("dev.climax", 16, "Jam: place the climax", "A melody's single highest note works best about two-thirds through.", 4, 96, 4,
            show = listOf(CHORDS, LEAD), locked = setOf(CHORDS), ghost = setOf(CHORDS), rows = 59..84, scale = Scales.C_MAJOR, snap = Snap.HALF, jam = true,
            setup = { p -> chords(p, listOf(listOf(60, 64, 67), listOf(57, 60, 64), listOf(53, 57, 60), listOf(55, 59, 62))) },
            steps = listOf(LessonStep("Write 10+ notes over 4 bars. Make ONE highest note, and put it in bar 3. End on a note of the G chord or C.", "10+ notes, single peak in bar 3.", Win.ROLL, LEAD,
                hint = "Climb gradually, peak once in bar 3, then come down.",
                check = chk@{ p, _ ->
                    val ns = lead(p)
                    if (ns.size < 10) return@chk no("More notes", "Write at least 10 notes.")
                    val top = ns.maxOf { it.pitch }
                    when { ns.count { it.pitch == top } > 1 -> no("One peak", "Only one note should be the highest.")
                        ns.first { it.pitch == top }.startTick / TICKS_PER_BAR != 2 -> no("Peak in bar 3", "Put the highest note in bar 3.")
                        ns.any { it.pitch % 12 !in Scales.C_MAJOR } -> no("Stay in key", "Use the shaded rows.")
                        else -> ok("Level 16 complete!", "A well-placed climax is what makes a melody feel like a journey.") }
                })), flTip = "Zoom out in FL to see the contour — the peak should stand out."),
    )

    // ---------------- Level 17 · Phrases and form ----------------
    private val l17 = listOf(
        Lesson("form.period", 17, "The 8-bar period", "Question (4 bars, ends open) + answer (4 bars, ends home).", 6, 96, 8,
            show = listOf(CHORDS, LEAD), locked = setOf(CHORDS), ghost = setOf(CHORDS), rows = 59..84, scale = Scales.C_MAJOR, snap = Snap.HALF,
            setup = { p -> chords(p, listOf(listOf(60, 64, 67), listOf(53, 57, 60), listOf(55, 59, 62), listOf(55, 59, 62), listOf(60, 64, 67), listOf(53, 57, 60), listOf(55, 59, 62), listOf(60, 64, 67))) },
            steps = listOf(LessonStep("Bars 1–4 ask: end bar 4 on G, B or D. Bars 5–8 answer: start like bar 1 and end on C in bar 8.", "Write an 8-bar question-and-answer melody.", Win.ROLL, LEAD,
                hint = "Copy bar 1's rhythm into bar 5. Finish bar 4 on a G-chord note and bar 8 on C.",
                check = chk@{ p, _ ->
                    val q = lead(p).filter { it.startTick < 4 * TICKS_PER_BAR }; val a = lead(p).filter { it.startTick >= 4 * TICKS_PER_BAR }
                    when { q.size < 4 || a.size < 4 -> no("Both halves", "Write at least 4 notes in bars 1–4 and in bars 5–8.")
                        q.last().pitch % 12 !in setOf(7, 11, 2) -> no("Leave the question open", "End bar 4 on G, B or D.")
                        a.last().pitch % 12 != 0 || a.last().startTick < 7 * TICKS_PER_BAR -> no("Answer at home", "End bar 8 on C.")
                        rel(bar(p, LEAD, 0), 0).toSet() != rel(bar(p, LEAD, 4), 4).toSet() -> no("Echo the start", "Bar 5 should have the same rhythm as bar 1.")
                        lead(p).any { it.pitch % 12 !in Scales.C_MAJOR } -> no("Stay in key", "Use the shaded rows.")
                        else -> ok("A real period!", "This 8-bar shape is the backbone of folk, pop and classical tunes.") }
                })), flTip = "In FL, make bars 1–4 and copy them to 5–8 (Ctrl+B), then change only the ending."),
        Lesson("form.sentence", 17, "The sentence", "Idea, idea again, then a busier continuation that ends.", 5, 96, 4,
            show = listOf(LEAD), rows = 59..84, scale = Scales.C_MAJOR, snap = Snap.HALF,
            steps = listOf(LessonStep("Bar 1: a short idea. Bar 2: same rhythm, different notes. Bars 3–4: more notes, moving to an ending on C.", "Write a 4-bar sentence.", Win.ROLL, LEAD,
                hint = "Bar 2 copies bar 1's rhythm. Bars 3–4 should have at least as many notes as bars 1–2.",
                check = chk@{ p, _ ->
                    val b1 = bar(p, LEAD, 0); val b2 = bar(p, LEAD, 1)
                    when { b1.size < 2 -> no("An idea first", "Put 2+ notes in bar 1.")
                        rel(b1, 0) != rel(b2, 1) -> no("Repeat the rhythm", "Bar 2 needs bar 1's rhythm.")
                        b1.map { it.pitch } == b2.map { it.pitch } -> no("Vary it", "Change at least one pitch in bar 2.")
                        bar(p, LEAD, 2).size + bar(p, LEAD, 3).size < b1.size + b2.size -> no("Keep going", "Bars 3–4 should be busier than bars 1–2.")
                        lead(p).last().pitch % 12 != 0 -> no("End home", "Finish on C.")
                        lead(p).any { it.pitch % 12 !in Scales.C_MAJOR } -> no("Stay in key", "Use the shaded rows.")
                        else -> ok("A sentence!", "Idea, repeat, continue, end — Beethoven used this shape constantly.") }
                })), flTip = "Many FL toplines follow this: two short calls, then a longer line."),
        Lesson("form.verse-chorus", 17, "Verse vs chorus", "The chorus sits higher and repeats its rhythm.", 5, 96, 4,
            show = listOf(LEAD), rows = 55..84, scale = Scales.C_MAJOR, snap = Snap.HALF,
            steps = listOf(LessonStep("Bars 1–2 are the verse: keep it low (G5 and below). Bars 3–4 are the chorus: higher, and bar 4 repeats bar 3's rhythm.", "Low verse, higher repeated chorus.", Win.ROLL, LEAD,
                hint = "Verse notes ≤ G5. Chorus average at least 4 semitones higher.",
                check = chk@{ p, _ ->
                    val v = lead(p).filter { it.startTick < 2 * TICKS_PER_BAR }; val c = lead(p).filter { it.startTick >= 2 * TICKS_PER_BAR }
                    if (v.size < 3 || c.size < 3) return@chk no("Both parts", "Write 3+ notes in the verse and in the chorus.")
                    when { v.any { it.pitch > 67 } -> no("Verse lower", "Keep the verse at G5 or lower.")
                        c.map { it.pitch }.average() < v.map { it.pitch }.average() + 4 -> no("Lift the chorus", "The chorus should sit clearly higher.")
                        rel(bar(p, LEAD, 2), 2) != rel(bar(p, LEAD, 3), 3) -> no("Repeat the chorus rhythm", "Bar 4 should use bar 3's rhythm.")
                        else -> ok("Level 17 complete!", "Lower verse, higher catchy chorus — the shape of almost every hit.") }
                })), flTip = "In FL, put the verse and chorus in different Patterns and arrange them in the Playlist."),
    )

    // ---------------- Level 18 · Songwriting ----------------
    private val tune = listOf(64 to 0, 62 to 480, 60 to 960, 62 to 1440, 67 to 1920, 65 to 2400, 64 to 2880, 62 to 3360, 60 to 3840, 64 to 4320, 69 to 4800, 67 to 5280, 67 to 5760)
    private val diatonicTriads = listOf(setOf(0, 4, 7), setOf(2, 5, 9), setOf(4, 7, 11), setOf(5, 9, 0), setOf(7, 11, 2), setOf(9, 0, 4))
    private val l18 = listOf(
        Lesson("song.harmonize", 18, "Harmonise a melody", "Choose chords that contain the melody's downbeat note.", 6, 90, 4,
            show = listOf(LEAD, CHORDS), locked = setOf(LEAD), ghost = setOf(LEAD), rows = 48..79,
            setup = { p -> p.set(LEAD, tune.map { (pp, t) -> note(pp, t, 480) }) },
            steps = listOf(LessonStep("The melody is given. For each bar, build a C-major-key chord on beat 1 that contains the melody note on beat 1.", "One chord per bar that fits the melody.", Win.ROLL, CHORDS,
                hint = "Bar 1 starts on E: C (C E G) or Am (A C E) both fit. Bar 2 starts on G: C, Em or G.",
                check = chk@{ p, _ ->
                    for (b in 0..3) {
                        val m = lead(p).first { it.startTick == b * TICKS_PER_BAR }.pitch % 12
                        val c = notes(p, CHORDS).filter { it.startTick == b * TICKS_PER_BAR }.map { it.pitch % 12 }.toSet()
                        if (c.size < 3) return@chk no("Bar ${b + 1}", "Build a 3-note chord on beat 1 of bar ${b + 1}.")
                        if (c !in diatonicTriads) return@chk no("Bar ${b + 1}: stay in key", "Use a chord from C major: C, Dm, Em, F, G or Am.")
                        if (m !in c) return@chk no("Bar ${b + 1}: include the tune", "The melody plays ${Pitch.pcName(m)} here — pick a chord that contains it.")
                    }
                    ok("Harmonised!", "That's how songwriters find chords for a melody they hummed.")
                })), flTip = "In FL, put the melody on one channel, turn on ghost channels, and try chords under it."),
        Lesson("song.hook", 18, "Jam: an 8-bar hook", "Repeat, vary, climb, resolve — over C–G–Am–F twice.", 8, 100, 8,
            show = listOf(CHORDS, LEAD), locked = setOf(CHORDS), ghost = setOf(CHORDS), rows = 59..84, snap = Snap.HALF, jam = true,
            setup = { p -> chords(p, List(8) { listOf(listOf(60, 64, 67), listOf(55, 59, 62), listOf(57, 60, 64), listOf(53, 57, 60))[it % 4] }) },
            steps = listOf(LessonStep("Bars 5–8 should echo bars 1–4 but change something; land chord tones on beats 1 and 3; peak in the second half; end on C.", "Write an 8-bar hook.", Win.ROLL, LEAD,
                hint = "Write 4 bars, Duplicate them, then raise or change the second half's ending.",
                check = chk@{ p, _ ->
                    val ns = lead(p)
                    if (ns.size < 10) return@chk no("More notes", "Write at least 10 notes.")
                    val first = ns.filter { it.startTick < 4 * TICKS_PER_BAR }; val second = ns.filter { it.startTick >= 4 * TICKS_PER_BAR }
                    val strong = ns.filter { it.startTick % TICKS_PER_BAR in setOf(0, 960) }
                    val ct = strong.count { n -> n.pitch % 12 in notes(p, CHORDS).filter { it.startTick == n.startTick / TICKS_PER_BAR * TICKS_PER_BAR }.map { it.pitch % 12 } }
                    when { first.isEmpty() || second.isEmpty() -> no("Both halves", "Write in bars 1–4 and 5–8.")
                        rel(first, 0).toSet().intersect(second.map { it.startTick - 4 * TICKS_PER_BAR }.toSet()).size.toFloat() / maxOf(1, rel(first, 0).size) < 0.6f -> no("Echo the first half", "Bars 5–8 should reuse most of bars 1–4's rhythm.")
                        first.map { it.pitch } == second.map { it.pitch } -> no("Change something", "Vary the second half.")
                        strong.isNotEmpty() && ct.toFloat() / strong.size < 0.75f -> no("Lean on the chords", "Notes on beats 1 and 3 should be chord tones.")
                        second.maxOf { it.pitch } < first.maxOf { it.pitch } -> no("Lift the second half", "Put the highest note in bars 5–8.")
                        ns.last().pitch % 12 != 0 -> no("Resolve", "End on C.")
                        else -> ok("That's a hook!", "Repetition, variation and a late peak — the recipe behind most catchy toplines.") }
                })), flTip = "Export it as MIDI and drop it on a lead synth in FL."),
        Lesson("song.final", 18, "Final song: melody + harmony", "Everything: chords, a hook, and a harmony line under it.", 10, 96, 4,
            show = listOf(CHORDS, LEAD, LEAD2), locked = setOf(CHORDS), ghost = setOf(CHORDS, LEAD), rows = 48..84, scale = Scales.C_MAJOR, snap = Snap.HALF, jam = true,
            setup = { p -> chords(p, listOf(listOf(60, 64, 67), listOf(57, 60, 64), listOf(53, 57, 60), listOf(55, 59, 62))) },
            steps = listOf(
                LessonStep("Write your 4-bar melody on Lead: chord tones on beats 1 and 3, a rest somewhere, end on a G, B or D (we'll loop it).", "Write the lead melody.", Win.ROLL, LEAD,
                    hint = "Bars: C – Am – F – G.",
                    check = chk@{ p, _ ->
                        val ns = lead(p)
                        val strong = ns.filter { it.startTick % TICKS_PER_BAR in setOf(0, 960) }
                        val ct = strong.count { n -> n.pitch % 12 in notes(p, CHORDS).filter { it.startTick == n.startTick / TICKS_PER_BAR * TICKS_PER_BAR }.map { it.pitch % 12 } }
                        when { ns.size < 8 -> no("More notes", "Write at least 8 notes.")
                            strong.isEmpty() || ct.toFloat() / strong.size < 0.75f -> no("Lean on the chords", "Beats 1 and 3 should mostly be chord tones.")
                            ns.zipWithNext().none { (a, b) -> b.startTick - (a.startTick + a.durationTicks) >= 240 } -> no("Breathe", "Leave a half-beat rest somewhere.")
                            ns.any { it.pitch % 12 !in Scales.C_MAJOR } -> no("Stay in key", "Use the shaded rows.")
                            else -> ok("Melody done!", "Now give it a partner.") }
                    }),
                LessonStep("Now Lead 2: harmonise at least 6 of your melody notes a third or sixth below, at the same moments.", "Harmony on 6+ notes: 3–4 or 8–9 semitones below.", Win.ROLL, LEAD2,
                    hint = "Two shaded rows down = a third. Five rows down = a sixth.",
                    check = { p, _ ->
                        val pairs = notes(p, LEAD2).count { h -> lead(p).any { it.startTick == h.startTick && (it.pitch - h.pitch) in (setOf(3, 4, 8, 9)) } }
                        when { pairs < 6 -> no("More harmony", "$pairs notes are harmonised. Aim for 6 or more, a third or sixth below.")
                            notes(p, LEAD2).any { it.pitch % 12 !in Scales.C_MAJOR } -> no("Stay in key", "Use the shaded rows.")
                            else -> ok("Course complete!", "Chords, a melody and a harmony line — you can write music. Export it and rebuild it in FL Studio.") }
                    }),
            ), flTip = "In FL, put the harmony on a second channel with a softer sound, and pan the two leads apart."),
    )

    val lessons: List<Lesson> = l11 + l12 + l13 + l14 + l15 + l16 + l17 + l18

    @Suppress("unused") private fun unusedAbs() = abs(0)
}
