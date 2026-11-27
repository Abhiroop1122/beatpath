package com.beatpath.app.lessons

/**
 * Music basics: the unit before everything else. Notes, the keyboard, the rainbow note colors, counting half steps,
 * scales, chords, which note to play next, rhythm, and finally the Piano roll. Each lesson is a short run of pages;
 * the screen is ui/lesson/BasicsLesson.kt. Research and sources: research/basics-research.md.
 *
 * Pitches use FL Studio's numbering: MIDI 60 = C5.
 */
object Basics {

    // ---------------- sound ----------------
    /** [notes] start together [atMs] after the tune starts. Drum hits: [drum] with notes 0 = kick, 1 = hat. */
    data class Hit(val notes: List<Int>, val atMs: Int, val dur: Float = 0.45f, val drum: Boolean = false)
    data class Tune(val hits: List<Hit>) {
        val lengthMs: Int get() = hits.maxOfOrNull { it.atMs + (it.dur * 1000).toInt() } ?: 0
        operator fun plus(o: Tune): Tune { val off = lengthMs + 350; return Tune(hits + o.hits.map { it.copy(atMs = it.atMs + off) }) }
    }
    fun seq(vararg n: Int, gap: Int = 480, dur: Float = 0.42f) = Tune(n.mapIndexed { i, p -> Hit(listOf(p), i * gap, dur) })
    fun chord(vararg n: Int, dur: Float = 1.2f) = Tune(listOf(Hit(n.toList(), 0, dur)))
    fun chords(vararg c: List<Int>, gap: Int = 900, dur: Float = 0.85f) = Tune(c.mapIndexed { i, ns -> Hit(ns, i * gap, dur) })
    fun clicks(count: Int, bpm: Int = 100) = Tune((0 until count).map { Hit(listOf(if (it % 4 == 0) 0 else 1), it * 60000 / bpm, 0.1f, drum = true) })

    // ---------------- what the screen shows ----------------
    enum class Visual { KEYS, BEATS, ROLL, NONE }
    /** A labelled bracket drawn over the keys, from one key to another. */
    data class Arc(val from: Int, val to: Int, val label: String)
    data class KeyView(
        val range: IntRange = 60..84,
        val letters: Boolean = true,
        /** Names on the black keys too (C♯…). */
        val sharps: Boolean = false,
        /** FL-style octave numbers on the C keys (C5, C6). */
        val octaves: Boolean = false,
        val rainbow: Boolean = true,
        /** Keys glowing gold: "look here". */
        val lit: Set<Int> = emptySet(),
        val arcs: List<Arc> = emptyList(),
        /** Small labels above keys (scale numbers, half-step counts, chord numerals). */
        val tags: Map<Int, String> = emptyMap(),
    )
    /** Rows of note-length blocks (in beats) drawn over one 4-beat bar. */
    data class BeatView(val rows: List<List<Float>> = listOf(listOf(1f, 1f, 1f, 1f)), val labels: List<String> = emptyList())
    data class RollView(val rows: IntRange = 60..72, val shown: Set<Pair<Int, Int>> = emptySet())

    enum class Kind { SHOW, TAP, CHOOSE, ROLL }
    /** ONE: pick a key. CHORD: pick several, any order. SEQUENCE: play keys in order (checked as you go). */
    enum class TapMode { ONE, CHORD, SEQUENCE }

    data class Page(
        val kind: Kind,
        /** Lou's bubble. */
        val say: String,
        /** The bold line under the bubble: the one thing to do or answer. */
        val task: String = "",
        val visual: Visual = Visual.KEYS,
        val keys: KeyView = KeyView(),
        val beats: BeatView = BeatView(),
        val roll: RollView = RollView(),
        val tune: Tune? = null,
        /** SHOW pages: keys to tap before Continue. */
        val needTaps: Int = 0,
        val target: List<Int> = emptyList(),
        val mode: TapMode = TapMode.ONE,
        /** Any octave of the target letter counts. */
        val anyOctave: Boolean = false,
        val options: List<String> = emptyList(),
        val answer: Int = 0,
        val cells: Set<Pair<Int, Int>> = emptySet(),
        val hint: String = "",
        /** Said after a right answer. */
        val success: String = "",
    )

    data class BasicsLesson(val id: String, val part: Int, val title: String, val idea: String, val minutes: Int, val pages: List<Page>)

    private fun show(say: String, keys: KeyView = KeyView(), tune: Tune? = null, needTaps: Int = 0, visual: Visual = Visual.KEYS,
                     beats: BeatView = BeatView(), roll: RollView = RollView()) =
        Page(Kind.SHOW, say, keys = keys, tune = tune, needTaps = needTaps, visual = visual, beats = beats, roll = roll)
    private fun tap(say: String, task: String, target: List<Int>, hint: String, success: String, keys: KeyView = KeyView(),
                    mode: TapMode = TapMode.ONE, anyOctave: Boolean = false, tune: Tune? = null) =
        Page(Kind.TAP, say, task, keys = keys, target = target, mode = mode, anyOctave = anyOctave, hint = hint, success = success, tune = tune)
    private fun choose(say: String, question: String, options: List<String>, answer: Int, success: String, hint: String = "",
                       tune: Tune? = null, keys: KeyView? = null, visual: Visual = if (keys != null) Visual.KEYS else Visual.NONE, beats: BeatView = BeatView()) =
        Page(Kind.CHOOSE, say, question, visual = visual, keys = keys ?: KeyView(), beats = beats, options = options, answer = answer,
            success = success, hint = hint, tune = tune)
    private fun roll(say: String, task: String, cells: Set<Pair<Int, Int>>, hint: String, success: String, rows: IntRange = 60..72) =
        Page(Kind.ROLL, say, task, visual = Visual.ROLL, roll = RollView(rows), cells = cells, hint = hint, success = success)

    // FL numbering: C5 = 60
    private const val C = 60; private const val Cs = 61; private const val D = 62; private const val Ds = 63; private const val E = 64
    private const val F = 65; private const val Fs = 66; private const val G = 67; private const val Gs = 68; private const val A = 69
    private const val As = 70; private const val B = 71; private const val C2 = 72; private const val D2 = 74; private const val E2 = 76
    private const val F2 = 77; private const val G2 = 79
    private val PLAIN = KeyView(letters = false, rainbow = false)
    private val BLACK = setOf(61, 63, 66, 68, 70, 73, 75, 78, 80, 82)
    private val C_MAJOR = listOf(C, D, E, F, G, A, B, C2)
    private val G_MAJOR = listOf(G, A, B, C2, D2, E2, 78, G2)
    private fun wwh(scale: List<Int>) = scale.zipWithNext().map { (a, b) -> Arc(a, b, if (b - a == 2) "W" else "H") }

    // =====================================================================
    // Part 1: notes and the keyboard
    // =====================================================================
    private val part1 = listOf(
        BasicsLesson("basics.note", 1, "What is a note?", "A note is one sound with a pitch: how high or low it is.", 2, listOf(
            show("Music is built from notes. A note is one sound with a pitch: how high or low it is. Tap a few keys.", PLAIN, needTaps = 3),
            show("Left is low, right is high. Listen: low… then high.", PLAIN.copy(lit = setOf(C, 84)), seq(C, 84, gap = 700, dur = 0.6f)),
            tap("Your turn.", "Tap the highest key.", listOf(84), "High notes live on the right. Go all the way right.", "The furthest right key is the highest.", PLAIN),
            choose("Ears only now.", "Is this note high or low?", listOf("Low", "High"), 1, "High, like a bird.", "Listen again: is it up in the air?", seq(88, dur = 0.7f)),
            choose("One more.", "High or low?", listOf("Low", "High"), 0, "Low, like a big drum.", "Listen again: is it deep?", seq(41, dur = 0.8f)),
            show("A note also has a length. Listen: a long note… then a short one.", PLAIN.copy(lit = setOf(G)),
                Tune(listOf(Hit(listOf(G), 0, 1.6f), Hit(listOf(G), 2000, 0.18f)))),
            choose("Pitch and length: that's a note.", "Which note was longer?", listOf("The first", "The second"), 1, "The second one held on longer.",
                "Listen for which one keeps ringing.", Tune(listOf(Hit(listOf(E), 0, 0.18f), Hit(listOf(E), 700, 1.5f)))),
        )),
        BasicsLesson("basics.alphabet", 1, "The musical alphabet", "Notes are named with 7 letters, A to G, then the letters repeat.", 3, listOf(
            show("Notes use just 7 letters: A B C D E F G. After G the letters start again.", KeyView(rainbow = false), seq(*C_MAJOR.toIntArray())),
            show("Each letter has a rainbow color: C red, D orange, E yellow, F green, G blue, A indigo, B violet. Tap some.", KeyView(), needTaps = 3),
            choose("Quick quiz.", "What comes after G?", listOf("H", "A", "C"), 1, "There's no H. After G the letters start again at A."),
            choose("And backwards.", "What comes before C?", listOf("D", "A", "B"), 2, "B comes right before C."),
            tap("Find it by color.", "Tap E, the yellow key.", listOf(E), "E is yellow: the third white key from C.", "Yellow E!", anyOctave = true),
            tap("No letters now.", "Tap G, the blue key.", listOf(G), "Blue is G: count C, D, E, F, G.", "Blue G!", KeyView(letters = false), anyOctave = true),
        )),
        BasicsLesson("basics.keyboard", 1, "Find any note", "Black keys come in groups of 2 and 3. They're your map.", 3, listOf(
            show("See the black keys? A group of 2, then a group of 3, over and over.", PLAIN.copy(lit = BLACK)),
            show("C sits just left of every group of 2 black keys.", PLAIN.copy(lit = setOf(C, C2, 84))),
            tap("No colors, no letters.", "Find a C.", listOf(C), "Find two black keys side by side. C is the white key just left of them.", "That's C. Every C sits there.", PLAIN, anyOctave = true),
            show("F sits just left of every group of 3 black keys.", PLAIN.copy(lit = setOf(F, F2))),
            tap("Your turn.", "Tap an F.", listOf(F), "Find three black keys together. F is the white key just left.", "F, found!", PLAIN, anyOctave = true),
            tap("Inside the group of 2…", "Tap D.", listOf(D), "D is the white key between the 2 black keys.", "D sits in the middle of the 2.", PLAIN, anyOctave = true),
            tap("Inside the group of 3…", "Tap A.", listOf(A), "A sits between the 2nd and 3rd black keys of the group of 3.", "A, found!", PLAIN, anyOctave = true),
        )),
        BasicsLesson("basics.octave", 1, "Octaves", "The same letter higher up is an octave: the same note, only higher.", 2, listOf(
            show("Listen: C, then the next C up. Same name, same color, just higher. That jump is an octave.", KeyView(lit = setOf(C, C2)), seq(C, C2, gap = 650, dur = 0.6f)),
            show("An octave is 12 keys away, black and white. FL Studio numbers them: C5, then C6.", KeyView(octaves = true, lit = setOf(C, C2), arcs = listOf(Arc(C, C2, "12 keys")))),
            tap("Jump up.", "Tap the C one octave above the glowing C.", listOf(C2), "Go 12 keys right: the next C, just left of the next 2 black keys.", "C5 to C6: one octave.", KeyView(octaves = true, lit = setOf(C))),
            tap("Again.", "Tap the E one octave above the glowing E.", listOf(E2), "Find the next yellow key to the right.", "E5 to E6!", KeyView(octaves = true, lit = setOf(E))),
            choose("Ears only.", "An octave, or two different notes?", listOf("Octave", "Different notes"), 0, "D and D, 12 keys apart. Octaves blend almost into one sound.",
                "An octave sounds like the same note, higher.", seq(D, D2, gap = 650, dur = 0.6f)),
            choose("And this one?", "An octave, or two different notes?", listOf("Octave", "Different notes"), 1, "That was D, then A: two different notes.",
                "Do they sound like the same note, or two notes?", seq(D, A, gap = 650, dur = 0.6f)),
        )),
        BasicsLesson("basics.steps", 1, "Half steps and whole steps", "The next key over is a half step. Skip one key and it's a whole step.", 3, listOf(
            show("The smallest move in music is a half step: to the very next key, black or white.", KeyView(lit = setOf(C, Cs), arcs = listOf(Arc(C, Cs, "½"))), seq(C, Cs)),
            show("Two half steps make a whole step: you skip one key.", KeyView(lit = setOf(C, D), arcs = listOf(Arc(C, D, "whole"))), seq(C, D)),
            show("The rule: E to F and B to C have no black key between them. They're only half steps.",
                KeyView(lit = setOf(E, F, B, C2), arcs = listOf(Arc(E, F, "½"), Arc(B, C2, "½"))), seq(E, F, B, C2)),
            choose("Think about the rule.", "E to F is a…", listOf("Half step", "Whole step"), 0, "No black key between E and F, so it's a half step.",
                "Is there a black key between them?", keys = KeyView(lit = setOf(E, F))),
            choose("And this pair?", "F to G is a…", listOf("Half step", "Whole step"), 1, "F♯ sits between them, so F to G skips a key: a whole step.",
                "Count the keys from F to G.", keys = KeyView(lit = setOf(F, G))),
            tap("Your turn.", "Tap the key a whole step above the glowing D.", listOf(E), "Skip the black key: D, D♯, then E.", "D to E: a whole step.", KeyView(lit = setOf(D))),
            tap("Last one.", "Tap the key a half step above the glowing B.", listOf(C2), "B and C have no black key between them.", "B to C: a half step.", KeyView(lit = setOf(B))),
        )),
        BasicsLesson("basics.sharps", 1, "Sharps and flats", "Black keys borrow names: ♯ sharp is a half step up, ♭ flat a half step down.", 3, listOf(
            show("Black keys borrow their neighbors' names. Sharp ♯ means a half step up. Flat ♭ means a half step down.", KeyView(sharps = true)),
            show("So this key has two names: C♯ and D♭. FL Studio writes sharps, like C#5.", KeyView(sharps = true, lit = setOf(Cs), arcs = listOf(Arc(C, Cs, "C♯")))),
            tap("Find it.", "Tap F♯.", listOf(Fs), "Start on F (green) and go one key right.", "F♯: a half step above F.", anyOctave = true),
            choose("Two names, one key.", "What's another name for A♯?", listOf("G♭", "B♭", "A♭"), 1, "A♯ is a half step above A, and a half step below B: B♭.",
                "A♯ is just below which white key?", keys = KeyView(sharps = true, lit = setOf(As))),
            tap("Flats go down.", "Tap E♭.", listOf(Ds), "Start on E (yellow) and go one key left.", "E♭: a half step below E.", anyOctave = true),
            choose("One more.", "Which is the same key as G♯?", listOf("A♭", "F♯", "G♭"), 0, "G♯ is a half step above G and a half step below A: A♭.",
                "Which white key is just above G♯?", keys = KeyView(lit = setOf(Gs))),
        )),
        BasicsLesson("basics.counting", 1, "Count to any note", "Every key is one half step. Count keys to work out any note.", 3, listOf(
            show("The main calculation in music: count half steps. Every key you pass, black or white, is 1.",
                KeyView(tags = mapOf(Cs to "1", D to "2", Ds to "3", E to "4"), lit = setOf(C, E)), seq(C, Cs, D, Ds, E, gap = 380)),
            show("Start key is 0. So C up 4 half steps lands on E. Musicians write it like this: C + 4 = E.",
                KeyView(tags = mapOf(C to "0", Cs to "1", D to "2", Ds to "3", E to "4"), arcs = listOf(Arc(C, E, "+4")), lit = setOf(C, E))),
            tap("Calculate.", "C + 3 half steps = ?", listOf(Ds), "Count every key after C: C♯ (1), D (2), D♯ (3).", "C + 3 = E♭. That's the sad sound in minor chords.", KeyView(lit = setOf(C))),
            tap("Again.", "E + 3 half steps = ?", listOf(G), "F (1), F♯ (2), G (3). Remember: E to F is just one key.", "E + 3 = G.", KeyView(lit = setOf(E))),
            tap("A bigger one.", "A + 7 half steps = ?", listOf(E2), "A♯ 1, B 2, C 3, C♯ 4, D 5, D♯ 6, E 7.", "A + 7 = E. Seven half steps is a fifth: the strongest pairing in music.", KeyView(lit = setOf(A))),
            choose("Backwards now.", "How many half steps from C up to G?", listOf("5", "7", "8"), 1, "C♯ D D♯ E F F♯ G: 7 half steps.", "Count every key after C until you reach G.",
                keys = KeyView(lit = setOf(C, G))),
            choose("Last one.", "From one C to the next C?", listOf("8", "10", "12"), 2, "12 half steps: one octave.", "Every octave has 7 white and 5 black keys.",
                keys = KeyView(lit = setOf(C, C2))),
        )),
        BasicsLesson("basics.major-scale", 1, "The major scale", "A major scale follows one rule: whole, whole, half, whole, whole, whole, half.", 3, listOf(
            show("A scale is a ladder of notes. The major scale's rule: W W H W W W H. W is a whole step, H a half step.",
                KeyView(arcs = wwh(C_MAJOR), lit = C_MAJOR.toSet()), seq(*C_MAJOR.toIntArray(), gap = 400)),
            show("Start on C and the rule lands on white keys only: C D E F G A B C. A whole rainbow!", KeyView(lit = C_MAJOR.toSet())),
            tap("Climb the ladder.", "Play C major, from C up to the next C.", C_MAJOR, "White keys only, left to right, from C to C.", "That's C major: do re mi fa so la ti do.",
                KeyView(), TapMode.SEQUENCE),
            show("The rule works from any note. Start on G and you need one black key: F♯.", KeyView(arcs = wwh(G_MAJOR), lit = G_MAJOR.toSet(), sharps = true), seq(*G_MAJOR.toIntArray(), gap = 400)),
            tap("Use the rule.", "Play G major, from G up to the next G.", G_MAJOR, "G A B C D E, then the black key F♯, then G.", "G major: one sharp, F♯.", KeyView(range = 60..84), TapMode.SEQUENCE),
            choose("Check the rule.", "Which note is NOT in C major?", listOf("F♯", "F", "B"), 0, "C major uses only white keys. F♯ belongs to G major."),
        )),
    )

    // =====================================================================
    // Part 2: how notes fit together
    // =====================================================================
    private val part2 = listOf(
        BasicsLesson("basics.degrees", 2, "Scale numbers and home", "Number a scale 1 to 7. Note 1 is home: melodies feel finished there.", 3, listOf(
            show("Musicians number a scale's notes 1 to 7. In C major, C is 1, D is 2… B is 7.",
                KeyView(tags = C_MAJOR.dropLast(1).mapIndexed { i, p -> p to "${i + 1}" }.toMap() + (C2 to "1"))),
            show("Note 1 is home, the tonic. Listen: this tune stops on 2. It feels unfinished…", KeyView(tags = mapOf(C to "1", D to "2"), lit = setOf(D)), seq(C, D, E, F, G, F, E, D)),
            show("…and here it lands on 1. Finished!", KeyView(tags = mapOf(C to "1"), lit = setOf(C)), seq(C, D, E, F, G, F, E, D, C)),
            choose("Trust your ears.", "Did this tune end at home?", listOf("Yes, finished", "No, unfinished"), 0, "It walked down to C, note 1. Home.",
                "Does it feel like it could stop there?", seq(G, F, E, D, C, dur = 0.5f)),
            choose("And this one?", "Did it end at home?", listOf("Yes, finished", "No, unfinished"), 1, "It stopped on D, note 2. It wants one more note.",
                "Does it leave you hanging?", seq(C, E, G, F, D, dur = 0.5f)),
            tap("Count up from home.", "In C major, tap note 5.", listOf(G), "C is 1, D 2, E 3, F 4, G 5.", "G is 5: the strongest note after home.", anyOctave = true),
            tap("One more.", "Tap note 7.", listOf(B), "Count to 7: it's the last key before the next C.", "B is 7. It sits a half step under home and pulls up to it.", anyOctave = true),
        )),
        BasicsLesson("basics.intervals", 2, "Intervals", "An interval is the distance between two notes: count the letters, then the half steps.", 3, listOf(
            show("An interval is the gap between two notes. Count the letters, both ends included: C D E is a 3rd.",
                KeyView(lit = setOf(C, E), arcs = listOf(Arc(C, E, "3rd"))), seq(C, E)),
            show("Half steps give the exact size: 3 is a small (minor) 3rd, 4 a big (major) 3rd, 7 a 5th, 12 an octave.",
                KeyView(lit = setOf(C), tags = mapOf(Ds to "3", E to "4", G to "7", C2 to "12"))),
            choose("Count the letters.", "C to F is a…", listOf("3rd", "4th", "5th"), 1, "C D E F: four letters, a 4th (5 half steps).", "Say the letters from C to F.",
                keys = KeyView(lit = setOf(C, F))),
            choose("Again.", "D to F is a…", listOf("2nd", "3rd", "4th"), 1, "D E F: a 3rd. It's 3 half steps, so a minor 3rd.", "Say the letters from D to F.",
                keys = KeyView(lit = setOf(D, F))),
            tap("Work it out.", "Tap the note a 5th above D.", listOf(A), "Count letters: D (1) E (2) F (3) G (4) A (5).", "D to A: a 5th, 7 half steps.", KeyView(lit = setOf(D))),
            choose("Ears only.", "A small step, or a big leap of a 5th?", listOf("Step (2nd)", "Leap (5th)"), 1, "C up to G: a 5th. Leaps sound bold.",
                "Did it move to the next note, or jump?", seq(C, G, gap = 650, dur = 0.6f)),
        )),
        BasicsLesson("basics.chords", 2, "What is a chord?", "A chord is 3 or more notes together. Stack every other note to build one.", 3, listOf(
            show("A chord is three or more notes played together.", KeyView(lit = setOf(C, E, G)), chord(C, E, G)),
            show("The triad rule: take a note, skip one, take one, skip one, take one. C (skip D) E (skip F) G. Red, yellow, blue!",
                KeyView(lit = setOf(C, E, G), tags = mapOf(C to "take", D to "skip", E to "take", F to "skip", G to "take"))),
            tap("Build it.", "Build C major: C, E and G.", listOf(C, E, G), "Every other white key, starting from C.", "C major: the first chord most people learn.", mode = TapMode.CHORD),
            show("The bottom note names the chord. It's called the root. This one is G: G, B, D.", KeyView(lit = setOf(G, B, D2), tags = mapOf(G to "root")), chord(G, B, D2)),
            tap("Use the rule.", "Build a chord on G.", listOf(G, B, D2), "G, skip A, B, skip C, D.", "G major: G, B, D.", mode = TapMode.CHORD),
            tap("One more.", "Build a chord on F.", listOf(F, A, C2), "F, skip G, A, skip B, C.", "F major: F, A, C.", mode = TapMode.CHORD),
        )),
        BasicsLesson("basics.major-minor", 2, "Major and minor", "Major is 4 + 3 half steps, minor is 3 + 4. The middle note sets the mood.", 3, listOf(
            show("Chords have moods. Major sounds bright… minor sounds darker.", KeyView(lit = setOf(C, E, G)), chord(C, E, G) + chord(C, Ds, G)),
            show("The math: major is 4 half steps, then 3. Minor is 3, then 4. Only the middle note moves.",
                KeyView(lit = setOf(C, E, G), arcs = listOf(Arc(C, E, "+4"), Arc(E, G, "+3")))),
            tap("Darken it.", "Make C minor: C, E♭, G.", listOf(C, Ds, G), "Start from C major and move E one key left.", "C minor: 3 + 4.", mode = TapMode.CHORD),
            tap("Calculate.", "Build D major: D, then + 4, then + 3.", listOf(D, Fs, A), "D + 4 = F♯, then F♯ + 3 = A.", "D major: D, F♯, A.", KeyView(sharps = true), TapMode.CHORD),
            tap("Now minor.", "Build A minor: A, then + 3, then + 4.", listOf(A, C2, E2), "A + 3 = C, then C + 4 = E.", "A minor: A, C, E. All white keys.", mode = TapMode.CHORD),
            choose("Ears only.", "Major or minor?", listOf("Major · bright", "Minor · dark"), 1, "D minor: D, F, A. 3 + 4, so it's dark.",
                "Bright and happy, or a little sad?", chord(D, F, A, dur = 1.6f)),
        )),
        BasicsLesson("basics.key-chords", 2, "The chords in a key", "Use the triad rule on every note of a scale and you get the key's chords.", 3, listOf(
            show("Use the triad rule on every note of C major, white keys only, and you get 7 chords.",
                KeyView(tags = mapOf(C to "I", D to "ii", E to "iii", F to "IV", G to "V", A to "vi", B to "vii°")),
                chords(listOf(C, E, G), listOf(D, F, A), listOf(E, G, B), listOf(F, A, C2), listOf(G, B, D2), listOf(A, C2, E2), gap = 700, dur = 0.6f)),
            show("Big numbers are major: I, IV and V. Small ones are minor: ii, iii and vi. These six chords cover most pop songs.",
                KeyView(tags = mapOf(C to "I", D to "ii", E to "iii", F to "IV", G to "V", A to "vi")), chords(listOf(C, E, G), listOf(G, B, D2), listOf(A, C2, E2), listOf(F, A, C2))),
            choose("Count up.", "In C major, which chord is V?", listOf("F", "G", "A minor"), 1, "Count to 5 from C: C D E F G. G major is V.", "V starts on note 5."),
            tap("Build it.", "Build chord IV in C major.", listOf(F, A, C2), "IV starts on note 4, F. Then take, skip, take: F, A, C.", "IV is F major.", mode = TapMode.CHORD),
            tap("And vi.", "Build chord vi.", listOf(A, C2, E2), "Note 6 is A: A, C, E.", "vi is A minor, C major's sad twin.", mode = TapMode.CHORD),
            choose("Use the math.", "Chord ii is D, F, A. Major or minor?", listOf("Major", "Minor"), 1, "D to F is 3 half steps, so ii is minor.", "Count D to F in half steps.",
                keys = KeyView(lit = setOf(D, F, A))),
        )),
        BasicsLesson("basics.next-note", 2, "What note comes next?", "Melodies move mostly by step, lean toward home, and land on chord notes.", 4, listOf(
            show("Good melodies mostly move by step: to the next note of the scale.", KeyView(lit = setOf(E, F, G, D, C)), seq(E, F, G, F, E, D, C)),
            show("Some notes pull. 7 wants to rise to 1. Over a G chord, 4 likes to fall to 3. Hear B pull up to C?",
                KeyView(tags = mapOf(B to "7", C2 to "1", F to "4", E to "3"), arcs = listOf(Arc(B, C2, "↑"), Arc(F, E, "↓"))), seq(G, A, B, C2, dur = 0.5f)),
            tap("Finish the pull.", "This tune stops on B (7). Tap the note it wants next.", listOf(C2), "7 rises a half step to home.", "B to C: tension, then release.",
                KeyView(lit = setOf(B)), tune = seq(E, G, A, B)),
            tap("This time 4.", "This tune ends on F (4). Tap where it wants to fall.", listOf(E), "4 falls a half step to 3: F to E.", "F falls to E. Smooth.",
                KeyView(lit = setOf(F)), tune = seq(G, A, G, F)),
            show("After a big leap, step back the other way. It fills the gap and sounds natural.", KeyView(lit = setOf(C, A), arcs = listOf(Arc(C, A, "leap"))), seq(C, A, G, F, E)),
            tap("Your move.", "The tune leapt from C up to A. Tap the best next note.", listOf(G), "Take a small step back down from A.", "A leap up, then a step down: the classic shape.",
                KeyView(lit = setOf(C, A)), tune = seq(C, A, gap = 600)),
            choose("On strong beats, land on a note of the chord.", "The chord is C major (C E G). Which note is safest on beat 1?", listOf("E", "F", "B"), 0,
                "E is in the C chord. F and B rub against it.", "Which note is part of C, E, G?", keys = KeyView(lit = setOf(C, E, G))),
            tap("Bring it home.", "Finish this tune.", listOf(C), "End on note 1: C.", "Ending on home sounds finished.", anyOctave = true, tune = seq(G, F, E, D)),
        )),
        BasicsLesson("basics.rhythm", 2, "Beats, bars and lengths", "A steady beat, grouped in bars of 4. Notes last whole, half, quarter or eighth beats.", 3, listOf(
            show("Music runs on a steady beat: the pulse you tap your foot to.", visual = Visual.BEATS, tune = clicks(8)),
            show("Beats come in groups called bars, usually 4 beats each. Count 1 2 3 4, 1 2 3 4.", visual = Visual.BEATS, tune = clicks(8)),
            show("Note lengths: a whole note fills the bar, a half note is 2 beats, a quarter is 1, an eighth half a beat.", visual = Visual.BEATS,
                beats = BeatView(listOf(listOf(4f), listOf(2f, 2f), List(4) { 1f }, List(8) { 0.5f }), listOf("whole", "half", "quarter", "eighth"))),
            choose("Count them.", "How many quarter notes fit in a 4-beat bar?", listOf("2", "4", "8"), 1, "4: one on every beat.", "A quarter note lasts 1 beat.",
                visual = Visual.BEATS, beats = BeatView(listOf(List(4) { 1f }), listOf("quarter"))),
            choose("Lengths.", "How long is a half note?", listOf("1 beat", "2 beats", "4 beats"), 1, "2 beats: half of a 4-beat bar.", "It's half of the bar."),
            choose("Smaller.", "How many eighth notes fit in one beat?", listOf("1", "2", "4"), 1, "2: count \"1 and\".", "An eighth is half a beat."),
            choose("Speed.", "Tempo, how fast the beat goes, is measured in…", listOf("BPM", "Bars", "Octaves"), 0, "BPM, beats per minute. 120 BPM is 2 beats a second."),
            show("FL Studio splits each beat into 4 steps: 16 per bar. That's the Channel Rack grid you'll use soon.", visual = Visual.BEATS,
                beats = BeatView(listOf(List(4) { 1f }, List(16) { 0.25f }), listOf("beats", "FL steps")), tune = clicks(4)),
        )),
        BasicsLesson("basics.piano-roll", 2, "Keyboard to Piano roll", "Turn the keyboard on its side: that's the Piano roll. Up is higher, right is later.", 3, listOf(
            show("Turn the keyboard on its side and you get the Piano roll. Keys on the left, high notes at the top.", visual = Visual.ROLL),
            show("Time runs left to right. Each column here is one beat, and a note is a bar you draw.", visual = Visual.ROLL,
                roll = RollView(shown = setOf(C to 0, E to 1, G to 2, C2 to 3)), tune = seq(C, E, G, C2)),
            roll("Your turn.", "Put C on beat 1.", setOf(C to 0), "Find the red C row at the bottom, then tap the first column.", "One note: pitch C, beat 1."),
            roll("Climb.", "Place C, D, E, F on beats 1 to 4.", setOf(C to 0, D to 1, E to 2, F to 3), "One note per column, each a row higher (white rows only).", "A climbing melody!"),
            roll("Stack it.", "Put C, E and G all on beat 1.", setOf(C to 0, E to 0, G to 0), "Three notes in the first column: red, yellow, blue.", "A chord in the Piano roll: notes stacked in one column."),
            show("That's exactly how FL Studio's Piano roll works. Next: warm up your ears, then into FL's Channel Rack.", visual = Visual.ROLL,
                roll = RollView(shown = setOf(C to 0, E to 0, G to 0, D to 1, F to 2, E to 3)), tune = chord(C, E, G, dur = 0.5f) + seq(D, F, E)),
        )),
    )

    val lessons: List<BasicsLesson> = part1 + part2
    val ids: List<String> = lessons.map { it.id }
    fun byId(id: String?) = lessons.firstOrNull { it.id == id }
    fun isBasics(id: String?) = id != null && id.startsWith("basics.")

    // ---------------- names ----------------
    private val sharpNames = arrayOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")
    private val flatNames = arrayOf("C", "D♭", "D", "E♭", "E", "F", "G♭", "G", "A♭", "A", "B♭", "B")
    fun name(midi: Int): String = sharpNames[midi % 12]
    /** "C♯/D♭" for black keys, the letter for white keys. */
    fun bothNames(midi: Int): String = if (sharpNames[midi % 12] == flatNames[midi % 12]) name(midi) else "${sharpNames[midi % 12]}/${flatNames[midi % 12]}"
    fun isBlack(midi: Int) = sharpNames[midi % 12].length > 1

    // ---------------- checking ----------------
    private fun same(page: Page, a: Int, b: Int) = if (page.anyOctave) a % 12 == b % 12 else a == b

    /** The next key a SEQUENCE expects after [played] (all correct so far), or null when done. */
    fun nextInSequence(page: Page, played: List<Int>): Int? = page.target.getOrNull(played.size)

    /** Is tapping [key] after [played] right in a SEQUENCE page? */
    fun sequenceOk(page: Page, played: List<Int>, key: Int): Boolean = nextInSequence(page, played)?.let { same(page, it, key) } == true

    /** Judge a TAP page (ONE or CHORD). */
    fun judgeTap(page: Page, picked: List<Int>): Verdict {
        if (picked.isEmpty()) return Checks.no("Tap a key first", page.hint)
        return when (page.mode) {
            TapMode.ONE -> {
                val k = picked.last()
                if (same(page, k, page.target.first())) Checks.ok(praise(k), page.success)
                else Checks.no("That's ${bothNames(k)}", page.hint)
            }
            TapMode.CHORD -> {
                val want = page.target; val got = picked.distinct()
                val missing = want.filter { w -> got.none { same(page, it, w) } }
                val extra = got.filter { g -> want.none { same(page, g, it) } }
                when {
                    missing.isEmpty() && extra.isEmpty() -> Checks.ok("That's the chord!", page.success)
                    extra.isNotEmpty() -> Checks.no("${bothNames(extra.first())} doesn't belong", "Tap it again to take it out. ${page.hint}")
                    else -> Checks.no("Keep stacking", "It still needs ${missing.joinToString(" and ") { bothNames(it) }}. ${page.hint}")
                }
            }
            TapMode.SEQUENCE -> if (picked.size == page.target.size && picked.indices.all { same(page, picked[it], page.target[it]) })
                Checks.ok("All the way up!", page.success) else Checks.no("Not finished", page.hint)
        }
    }

    fun judgeChoice(page: Page, choice: Int?): Verdict = when (choice) {
        null -> Checks.no("Pick an answer", page.hint)
        page.answer -> Checks.ok(listOf("Right!", "Exactly!", "You got it!").random(), page.success)
        else -> Checks.no("Not quite", if (page.hint.isNotEmpty()) "${page.hint} The answer is ${page.options[page.answer]}." else "It's ${page.options[page.answer]}. ${page.success}")
    }

    /** ROLL pages: (pitch, beat 0–3) cells must match exactly. */
    fun judgeRoll(page: Page, cells: Set<Pair<Int, Int>>): Verdict {
        val missing = page.cells - cells; val extra = cells - page.cells
        return when {
            missing.isEmpty() && extra.isEmpty() -> Checks.ok("Spot on!", page.success)
            extra.isNotEmpty() -> extra.first().let { (p, b) -> Checks.no("One extra note", "${name(p)} on beat ${b + 1} isn't part of it. Tap it to remove it.") }
            else -> missing.first().let { (p, b) -> Checks.no("Almost", "Put ${name(p)} on beat ${b + 1}. ${page.hint}") }
        }
    }

    private fun praise(k: Int) = "${bothNames(k)}!"
}
