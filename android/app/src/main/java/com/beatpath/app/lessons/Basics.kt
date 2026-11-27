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
        BasicsLesson("basics.note", 1, "What is a note?", "A note is one sound. It can be high or low, long or short.", 2, listOf(
            show("Music is made of notes. A note is one sound. Some notes are high and some are low. Tap a few keys!", PLAIN, needTaps = 3),
            show("Low notes live on the left. High notes live on the right. Listen: low… then high.", PLAIN.copy(lit = setOf(C, 84)), seq(C, 84, gap = 700, dur = 0.6f)),
            tap("Your turn!", "Tap the highest key.", listOf(84), "High notes live on the right. Go all the way to the right end.",
                "The far right key is the highest. How high or low a note sounds is called its pitch.", PLAIN),
            choose("Now use only your ears.", "Is this note high or low?", listOf("Low", "High"), 1, "High, like a little bird singing.",
                "Listen again. Does it sound tiny, up in the sky?", seq(88, dur = 0.7f)),
            choose("One more.", "High or low?", listOf("Low", "High"), 0, "Low, like a big bear growling.", "Listen again. Does it sound big and deep?", seq(41, dur = 0.8f)),
            show("Notes also have a length. Some are long and some are short. Listen: a long note… then a short one.", PLAIN.copy(lit = setOf(G)),
                Tune(listOf(Hit(listOf(G), 0, 1.6f), Hit(listOf(G), 2000, 0.18f)))),
            choose("High or low, long or short. That's a note!", "Which note was longer?", listOf("The first", "The second"), 1,
                "The second one rang on longer, like saying hellooooo.", "Listen again. Which one keeps ringing?",
                Tune(listOf(Hit(listOf(E), 0, 0.18f), Hit(listOf(E), 700, 1.5f)))),
        )),
        BasicsLesson("basics.alphabet", 1, "The musical alphabet", "Notes are named with just 7 letters, A to G. Then the letters start over.", 3, listOf(
            show("The white keys use just 7 letters: A B C D E F G. After G, the letters start again. Listen, starting from C.",
                KeyView(rainbow = false), seq(*C_MAJOR.toIntArray())),
            show("Each letter has a rainbow color. C red, D orange, E yellow, F green, G blue, A indigo (dark blue), B violet (purple). Tap some!", KeyView(), needTaps = 3),
            choose("Quick quiz!", "What comes after G?", listOf("H", "A", "C"), 1, "There's no H in music. After G, we start again at A.",
                "Music only uses the letters A to G."),
            choose("Now go backwards.", "What comes before C?", listOf("D", "A", "B"), 2, "B comes right before C, just like in the alphabet.",
                "Say the alphabet: A, B, C. Which letter is just before C?"),
            tap("Find it by color.", "Tap E, the yellow key.", listOf(E), "Look for yellow. It's the third white key: C, D, E.", "Yellow E! Any E on the keyboard counts.", anyOctave = true),
            tap("Now the letters are hiding.", "Tap G, the blue key.", listOf(G), "Look for blue. Or count white keys from red C: C, D, E, F, G.",
                "Blue G! Colors help you find notes fast.", KeyView(letters = false), anyOctave = true),
        )),
        BasicsLesson("basics.keyboard", 1, "Find any note", "Black keys come in groups of 2 and 3. They are your map.", 3, listOf(
            show("Look at the black keys. They come in little groups: 2, then 3, then 2, then 3. Over and over.", PLAIN.copy(lit = BLACK)),
            show("C lives just left of every group of 2 black keys. Like a house next to two trees.", PLAIN.copy(lit = setOf(C, C2, 84))),
            tap("No colors, no letters. You can do it!", "Find a C.", listOf(C), "Find 2 black keys side by side. C is the white key just to their left.",
                "That's C! Every C sits in the same spot.", PLAIN, anyOctave = true),
            show("F lives just left of every group of 3 black keys.", PLAIN.copy(lit = setOf(F, F2))),
            tap("Your turn.", "Tap an F.", listOf(F), "Find 3 black keys together. F is the white key just to their left.",
                "F, found! Groups of 2 point to C. Groups of 3 point to F.", PLAIN, anyOctave = true),
            tap("Now look inside the group of 2.", "Tap D.", listOf(D), "D is the white key in the middle of the 2 black keys.", "D sits right in the middle, like the filling in a sandwich.", PLAIN, anyOctave = true),
            tap("Now look inside the group of 3.", "Tap A.", listOf(A), "Find the group of 3 black keys. A is the white key between the 2nd and 3rd one.",
                "A, found! Now you can find any note with no help.", PLAIN, anyOctave = true),
        )),
        BasicsLesson("basics.octave", 1, "Octaves", "An octave is the same note, higher up. Same letter, same color.", 2, listOf(
            show("Listen: C, then the next C up. Same name, same color, just higher. Like two sisters singing one tune. That's an octave.",
                KeyView(lit = setOf(C, C2)), seq(C, C2, gap = 650, dur = 0.6f)),
            show("From one C to the next C is 12 keys, black and white. FL Studio gives them numbers: C5, then C6.",
                KeyView(octaves = true, lit = setOf(C, C2), arcs = listOf(Arc(C, C2, "12 keys")))),
            tap("Jump up!", "Tap the C one octave above the glowing C.", listOf(C2), "Look right for the next red key. It's just left of the next 2 black keys.",
                "C5 to C6: one octave up!", KeyView(octaves = true, lit = setOf(C))),
            tap("Again!", "Tap the E one octave above the glowing E.", listOf(E2), "Look right for the next yellow key.", "E5 to E6. Same color, higher up.",
                KeyView(octaves = true, lit = setOf(E))),
            choose("Now use only your ears.", "An octave, or two different notes?", listOf("Octave", "Different notes"), 0,
                "D and D, an octave apart. They blend together almost like one sound.", "An octave sounds like the same note, just higher. Does it?",
                seq(D, D2, gap = 650, dur = 0.6f)),
            choose("And this one?", "An octave, or two different notes?", listOf("Octave", "Different notes"), 1, "That was D, then A. Two different notes.",
                "Do they sound like the same note, or two different notes?", seq(D, A, gap = 650, dur = 0.6f)),
        )),
        BasicsLesson("basics.steps", 1, "Half steps and whole steps", "The very next key is a half step. Skip one key and it's a whole step.", 3, listOf(
            show("The smallest move in music is a half step. It goes to the very next key, black or white.", KeyView(lit = setOf(C, Cs), arcs = listOf(Arc(C, Cs, "½"))), seq(C, Cs)),
            show("Two half steps make a whole step. You skip over one key in the middle.", KeyView(lit = setOf(C, D), arcs = listOf(Arc(C, D, "whole"))), seq(C, D)),
            show("Watch out! E to F and B to C have no black key between them. So they're only half steps.",
                KeyView(lit = setOf(E, F, B, C2), arcs = listOf(Arc(E, F, "½"), Arc(B, C2, "½"))), seq(E, F, B, C2)),
            choose("Look closely at the keys.", "E to F is a…", listOf("Half step", "Whole step"), 0, "No black key between E and F, so it's a half step.",
                "Look between E and F. Is there a black key?", keys = KeyView(lit = setOf(E, F))),
            choose("And this pair?", "F to G is a…", listOf("Half step", "Whole step"), 1, "There's a black key between F and G. So you skip a key: a whole step.",
                "Look between F and G. Is there a black key in the middle?", keys = KeyView(lit = setOf(F, G))),
            tap("Your turn.", "Tap the key a whole step above the glowing D.", listOf(E), "Skip over the black key next to D. Land on the white key after it.",
                "D to E: a whole step. You skipped one key.", KeyView(lit = setOf(D))),
            tap("Last one!", "Tap the key a half step above the glowing B.", listOf(C2), "B and C have no black key between them. Go to the very next key.",
                "B to C: a half step, the smallest move.", KeyView(lit = setOf(B))),
        )),
        BasicsLesson("basics.sharps", 1, "Sharps and flats", "Black keys borrow names. Sharp ♯ means one key up. Flat ♭ means one key down.", 3, listOf(
            show("Black keys borrow their neighbors' names. Sharp ♯ means a half step up. Flat ♭ means a half step down, like a flat tire.", KeyView(sharps = true)),
            show("So this black key has two names: C♯ (C sharp) and D♭ (D flat). FL Studio writes it as C#.",
                KeyView(sharps = true, lit = setOf(Cs), arcs = listOf(Arc(C, Cs, "C♯")))),
            tap("Find it.", "Tap F♯.", listOf(Fs), "Start on F, the green key. Go one key to the right.", "F♯ (F sharp): a half step above F.", anyOctave = true),
            choose("Two names, one key.", "What's another name for A♯?", listOf("G♭", "B♭", "A♭"), 1, "A♯ is just above A and just below B. So it's also B♭.",
                "Which white key is just above A♯? Use that letter with ♭.", keys = KeyView(sharps = true, lit = setOf(As))),
            tap("Flats go down.", "Tap E♭.", listOf(Ds), "Start on E, the yellow key. Go one key to the left.", "E♭ (E flat): a half step below E.", anyOctave = true),
            choose("One more.", "Which is the same key as G♯?", listOf("A♭", "F♯", "G♭"), 0, "G♯ is just above G and just below A. So it's also A♭.",
                "Which white key is just above G♯? Use that letter with ♭.", keys = KeyView(lit = setOf(Gs))),
        )),
        BasicsLesson("basics.counting", 1, "Count to any note", "Every key is one half step. Count keys to find any note.", 3, listOf(
            show("Time for music math! We count half steps. Every key you pass, black or white, counts as 1.",
                KeyView(tags = mapOf(Cs to "1", D to "2", Ds to "3", E to "4"), lit = setOf(C, E)), seq(C, Cs, D, Ds, E, gap = 380)),
            show("The start key is 0. Count 4 keys up from C and you land on E. We write it: C + 4 = E.",
                KeyView(tags = mapOf(C to "0", Cs to "1", D to "2", Ds to "3", E to "4"), arcs = listOf(Arc(C, E, "+4")), lit = setOf(C, E))),
            tap("Your turn to count.", "C + 3 half steps = ?", listOf(Ds), "Start at C. Count every key after it: C♯ is 1, D is 2, D♯ is 3.",
                "C + 3 = D♯, also called E♭. You'll meet it again in sad-sounding chords.", KeyView(lit = setOf(C))),
            tap("Again!", "E + 3 half steps = ?", listOf(G), "F is 1, F♯ is 2, G is 3. Remember: F is right next door to E.", "E + 3 = G. Great counting!", KeyView(lit = setOf(E))),
            tap("A bigger one.", "A + 7 half steps = ?", listOf(E2), "Count on your fingers: A♯ 1, B 2, C 3, C♯ 4, D 5, D♯ 6, E 7.",
                "A + 7 = E. You counted past G, and the letters started over!", KeyView(lit = setOf(A))),
            choose("Now count the gap.", "How many half steps from C up to G?", listOf("5", "7", "8"), 1, "C♯, D, D♯, E, F, F♯, G. That's 7 half steps.",
                "Count every key after C, black and white, until you reach G.", keys = KeyView(lit = setOf(C, G))),
            choose("Last one!", "How many half steps from one C to the next C?", listOf("8", "10", "12"), 2,
                "12 half steps make one octave: 7 white keys and 5 black keys.", "Count every key after C, black and white, until the next C.",
                keys = KeyView(lit = setOf(C, C2))),
        )),
        BasicsLesson("basics.major-scale", 1, "The major scale", "A scale is a staircase of notes. The major scale follows one recipe: W W H W W W H.", 3, listOf(
            show("A scale is like a staircase of notes. You climb it one step at a time. Listen to the C major scale.",
                KeyView(lit = C_MAJOR.toSet()), seq(*C_MAJOR.toIntArray(), gap = 400)),
            show("The major scale has a recipe: W W H W W W H. W means a whole step. H means a half step.",
                KeyView(arcs = wwh(C_MAJOR), lit = C_MAJOR.toSet())),
            show("Start on C and the recipe uses only white keys: C D E F G A B C. A whole rainbow!", KeyView(lit = C_MAJOR.toSet())),
            tap("Climb the stairs!", "Play C major, from C up to the next C.", C_MAJOR, "Use white keys only. Go left to right, from red C to the next red C.",
                "That's C major! Eight notes, climbing like stairs from C to C.", KeyView(), TapMode.SEQUENCE),
            show("The recipe works from any note. Start on G and you need one black key: F♯.", KeyView(arcs = wwh(G_MAJOR), lit = G_MAJOR.toSet(), sharps = true),
                seq(*G_MAJOR.toIntArray(), gap = 400)),
            tap("Use the recipe.", "Play G major, from G up to the next G.", G_MAJOR, "G A B C D E, then the black key F♯, then G.", "G major! It has one sharp: F♯.",
                KeyView(range = 60..84), TapMode.SEQUENCE),
            choose("Check the recipe.", "Which note is NOT in C major?", listOf("F♯", "F", "B"), 0, "C major uses only white keys. F♯ is a black key, so it's not in.",
                "C major uses only white keys. Which answer is a black key?"),
        )),
    )

    // =====================================================================
    // Part 2: how notes fit together
    // =====================================================================
    private val part2 = listOf(
        BasicsLesson("basics.degrees", 2, "Scale numbers and home", "Give each note of a scale a number, 1 to 7. Note 1 is home: tunes feel finished there.", 3, listOf(
            show("Musicians number the notes of a scale. In C major, C is 1, D is 2, E is 3… up to B, which is 7.",
                KeyView(tags = C_MAJOR.dropLast(1).mapIndexed { i, p -> p to "${i + 1}" }.toMap() + (C2 to "1"))),
            show("Note 1 is home. Its grown-up name is the tonic. Listen: this tune stops on 2. It feels unfinished…",
                KeyView(tags = mapOf(C to "1", D to "2"), lit = setOf(D)), seq(C, D, E, F, G, F, E, D)),
            show("…and this one lands on 1. Home! It feels finished, like the end of a story.", KeyView(tags = mapOf(C to "1"), lit = setOf(C)), seq(C, D, E, F, G, F, E, D, C)),
            choose("Trust your ears.", "Did this tune end at home?", listOf("Yes, finished", "No, unfinished"), 0, "It walked down to C, note 1. Home sweet home.",
                "Does it feel like the end of a story?", seq(G, F, E, D, C, dur = 0.5f)),
            choose("And this one?", "Did it end at home?", listOf("Yes, finished", "No, unfinished"), 1, "It stopped on D, note 2. It wants one more note.",
                "Does it leave you hanging, like a question?", seq(C, E, G, F, D, dur = 0.5f)),
            tap("Count up from home.", "In C major, tap note 5.", listOf(G), "Count white keys from C: C is 1, D 2, E 3, F 4, G 5.",
                "G is note 5. It's the strongest note after home.", anyOctave = true),
            tap("One more.", "Tap note 7.", listOf(B), "Keep counting past G: A is 6, B is 7. It's the last white key before the next C.",
                "B is note 7. It sits right below home and wants to go up to it.", anyOctave = true),
        )),
        BasicsLesson("basics.intervals", 2, "Intervals", "An interval is the gap between two notes. Count the letters to name it.", 3, listOf(
            show("An interval is the gap between two notes. Count the letters, both ends too. C, D, E is 3 letters, so it's a 3rd.",
                KeyView(lit = setOf(C, E), arcs = listOf(Arc(C, E, "3rd"))), seq(C, E)),
            show("Half steps tell the exact size. From C, 3 is a small 3rd, 4 a big 3rd, 7 a 5th, 12 an octave.",
                KeyView(lit = setOf(C), tags = mapOf(Ds to "3", E to "4", G to "7", C2 to "12"))),
            choose("Count the letters.", "C to F is a…", listOf("3rd", "4th", "5th"), 1, "C, D, E, F: four letters, so it's a 4th.",
                "Say the letters from C to F. Count them on your fingers.", keys = KeyView(lit = setOf(C, F))),
            choose("Again!", "D to F is a…", listOf("2nd", "3rd", "4th"), 1, "D, E, F: three letters, a 3rd. It's 3 half steps, so a small 3rd.",
                "Say the letters from D to F. Count them on your fingers.", keys = KeyView(lit = setOf(D, F))),
            tap("Work it out.", "Tap the note a 5th above D.", listOf(A), "Count 5 letters, starting with D: D 1, E 2, F 3, G 4, A 5.",
                "D to A is a 5th. That's 7 half steps.", KeyView(lit = setOf(D))),
            choose("A step goes to the very next note. A leap jumps over notes, like a frog. Listen!", "A small step, or a big leap?",
                listOf("Step (2nd)", "Leap (5th)"), 1, "C up to G: a 5th. Leaps sound bold and exciting.",
                "Did it go to the very next note, or jump over some?", seq(C, G, gap = 650, dur = 0.6f)),
        )),
        BasicsLesson("basics.chords", 2, "What is a chord?", "A chord is 3 or more notes played together. Take one, skip one to build it.", 3, listOf(
            show("A chord is three or more notes played at the same time. Like friends singing together. Listen!", KeyView(lit = setOf(C, E, G)), chord(C, E, G)),
            show("Here's the recipe: take a note, skip one, take one, skip one, take one. C, skip D, E, skip F, G. Red, yellow, blue!",
                KeyView(lit = setOf(C, E, G), tags = mapOf(C to "✓", D to "✗", E to "✓", F to "✗", G to "✓"))),
            tap("Build it!", "Build the C chord: C, E and G.", listOf(C, E, G), "Take every other white key, starting from C: C, E, G.",
                "This is C major, the first chord most people learn. A 3-note chord is called a triad.", mode = TapMode.CHORD),
            show("The bottom note names the chord. It's called the root, like a tree's root. This chord starts on G: G, B, D.",
                KeyView(lit = setOf(G, B, D2), tags = mapOf(G to "root")), chord(G, B, D2)),
            tap("Use the recipe.", "Build a chord on G.", listOf(G, B, D2), "G, skip A, take B, skip C, take D.", "G major: G, B, D. Its root is G.", mode = TapMode.CHORD),
            tap("One more!", "Build a chord on F.", listOf(F, A, C2), "F, skip G, take A, skip B, take C.", "F major: F, A, C. You can build chords now!", mode = TapMode.CHORD),
        )),
        BasicsLesson("basics.major-minor", 2, "Major and minor", "Major chords sound bright. Minor chords sound darker. The middle note makes the change.", 3, listOf(
            show("Chords have moods! Major sounds bright, like sunshine. Minor sounds darker, like a rainy day. Listen to both.",
                KeyView(lit = setOf(C, E, G)), chord(C, E, G) + chord(C, Ds, G)),
            show("Here's the math. Major: 4 half steps, then 3. Minor: 3 half steps, then 4. Only the middle note moves.",
                KeyView(lit = setOf(C, E, G), arcs = listOf(Arc(C, E, "+4"), Arc(E, G, "+3")))),
            tap("Make it darker.", "Make C minor: C, E♭, G.", listOf(C, Ds, G), "Start from C major: C, E, G. Move E one key left, onto the black key.",
                "C minor: 3, then 4. Hear how it sounds a bit sad?", mode = TapMode.CHORD),
            tap("Do the math.", "Build D major: D, then + 4, then + 3.", listOf(D, Fs, A), "From D, count 4 keys up to F♯. From F♯, count 3 more to A.",
                "D major: D, F♯, A. Bright!", KeyView(sharps = true), TapMode.CHORD),
            tap("Now minor.", "Build A minor: A, then + 3, then + 4.", listOf(A, C2, E2), "From A, count 3 keys up to C. From C, count 4 more to E.",
                "A minor: A, C, E. All white keys!", mode = TapMode.CHORD),
            choose("Now use only your ears.", "Major or minor?", listOf("Major · bright", "Minor · dark"), 1, "D minor: D, F, A. It's 3, then 4, so it sounds dark.",
                "Does it sound like sunshine, or like a rainy day?", chord(D, F, A, dur = 1.6f)),
        )),
        BasicsLesson("basics.key-chords", 2, "The chords in a key", "Build a chord on each note of a scale. That family of chords is called a key.", 3, listOf(
            show("Use the chord recipe on each white key, starting on C. You get a family of chords. This family is called a key.",
                KeyView(lit = setOf(C, D, E, F, G, A)),
                chords(listOf(C, E, G), listOf(D, F, A), listOf(E, G, B), listOf(F, A, C2), listOf(G, B, D2), listOf(A, C2, E2), gap = 700, dur = 0.6f)),
            show("Each chord gets a Roman number. I is 1, IV is 4, V is 5, vi is 6. Look at the tags!",
                KeyView(tags = mapOf(C to "I", D to "ii", E to "iii", F to "IV", G to "V", A to "vi"))),
            show("Big letters mean major: I, IV, V. Small letters mean minor: ii, iii, vi. These chords make lots of pop songs!",
                KeyView(tags = mapOf(C to "I", D to "ii", E to "iii", F to "IV", G to "V", A to "vi")), chords(listOf(C, E, G), listOf(G, B, D2), listOf(A, C2, E2), listOf(F, A, C2))),
            choose("Count up.", "In C major, which chord is V?", listOf("F", "G", "A minor"), 1, "Count to 5 from C: C, D, E, F, G. So chord V is G major.",
                "V means 5. Count 5 white keys up from C."),
            tap("Build it!", "Build chord IV in C major.", listOf(F, A, C2), "IV means 4. Note 4 is F. Then take one, skip one: F, A, C.", "Chord IV is F major.", mode = TapMode.CHORD),
            tap("And vi.", "Build chord vi.", listOf(A, C2, E2), "vi means 6. Note 6 is A. Take one, skip one: A, C, E.", "Chord vi is A minor, C major's sad twin.", mode = TapMode.CHORD),
            choose("Use the math.", "Chord ii is D, F, A. Major or minor?", listOf("Major", "Minor"), 1, "D to F is 3 half steps, so chord ii is minor. Small letters, minor chord!",
                "Count the half steps from D up to F. 3 means minor. 4 means major.", keys = KeyView(lit = setOf(D, F, A))),
        )),
        BasicsLesson("basics.next-note", 2, "What note comes next?", "Tunes like small steps, pull toward home, and fit the chord.", 4, listOf(
            show("Good tunes mostly walk in small steps, to the next note of the scale. Like walking down stairs one at a time.",
                KeyView(lit = setOf(E, F, G, D, C)), seq(E, F, G, F, E, D, C)),
            show("Some notes pull like magnets. Note 7 wants to go up to 1. Note 4 slides down to 3. Hear B pull up to C?",
                KeyView(tags = mapOf(B to "7", C2 to "1", F to "4", E to "3"), arcs = listOf(Arc(B, C2, "↑"), Arc(F, E, "↓"))), seq(G, A, B, C2, dur = 0.5f)),
            tap("Finish the pull.", "This tune stops on B (note 7). Tap the note it wants next.", listOf(C2), "7 wants to go up to 1. Go to the very next key up from B.",
                "B to C! A little stretch, then a happy landing.", KeyView(lit = setOf(B)), tune = seq(E, G, A, B)),
            tap("This time it's note 4.", "This tune ends on F (note 4). Tap where it wants to go.", listOf(E), "4 likes to slide down to 3. Go one white key left of F.",
                "F slides down to E. Smooth!", KeyView(lit = setOf(F)), tune = seq(G, A, G, F)),
            show("After a big jump, take a small step back the other way. It fills the gap, like landing after a hop.",
                KeyView(lit = setOf(C, A), arcs = listOf(Arc(C, A, "leap"))), seq(C, A, G, F, E)),
            tap("Your move.", "The tune jumped from C up to A. Tap the best next note.", listOf(G), "Take one small step back down from A. Go one white key left.",
                "A jump up, then a step down. That shape sounds great!", KeyView(lit = setOf(C, A)), tune = seq(C, A, gap = 600)),
            choose("When a chord is playing, notes from inside the chord fit best.", "The chord is C major (C, E, G). Which tune note fits best?", listOf("E", "F", "B"), 0,
                "E is in the C chord, so it fits. F and B rub against it.", "Look at the glowing keys. Which answer is C, E or G?", keys = KeyView(lit = setOf(C, E, G))),
            tap("Bring it home!", "Finish this tune.", listOf(C), "End on note 1, home. That's C.", "Ending on home sounds finished. The end!", anyOctave = true, tune = seq(G, F, E, D)),
        )),
        BasicsLesson("basics.rhythm", 2, "Beats, bars and lengths", "Music has a steady beat, in groups of 4 called bars. Notes can be long or short.", 3, listOf(
            show("Music has a steady beat, like a clock going tick, tick, tick. It's what you tap your foot to.", visual = Visual.BEATS, tune = clicks(8)),
            show("Beats come in groups called bars. Most bars have 4 beats. Count along: 1 2 3 4, 1 2 3 4.", visual = Visual.BEATS, tune = clicks(8)),
            show("Notes are like pizza! A whole note fills the bar. A half note is 2 beats, a quarter 1 beat, an eighth half a beat.", visual = Visual.BEATS,
                beats = BeatView(listOf(listOf(4f), listOf(2f, 2f), List(4) { 1f }, List(8) { 0.5f }), listOf("whole", "half", "quarter", "eighth"))),
            choose("Count them.", "How many quarter notes fit in a 4-beat bar?", listOf("2", "4", "8"), 1, "4! One on every beat, like 4 slices of pizza.",
                "A quarter note lasts 1 beat. How many beats are in the bar?", visual = Visual.BEATS, beats = BeatView(listOf(List(4) { 1f }), listOf("quarter"))),
            choose("Now a longer one.", "How long is a half note?", listOf("1 beat", "2 beats", "4 beats"), 1, "2 beats. Two halves make the whole bar.",
                "It's half of a 4-beat bar."),
            choose("Now a shorter one.", "How many eighth notes fit in one beat?", listOf("1", "2", "4"), 1, "2! Count them like this: \"1 and\".",
                "An eighth note lasts half a beat."),
            show("How fast the beat goes is called the tempo. We count it in BPM: beats per minute. Listen: slow, then fast.", visual = Visual.BEATS,
                tune = clicks(4, 70) + clicks(4, 140)),
            choose("Speed check!", "Tempo, how fast the beat goes, is counted in…", listOf("BPM", "Bars", "Octaves"), 0,
                "BPM, beats per minute. 120 BPM is 2 beats every second.", "Tempo is counted in beats per minute. Which answer is short for that?"),
            show("FL Studio cuts each beat into 4 small steps. That makes 16 steps in a bar. You'll use them soon in the Channel Rack!", visual = Visual.BEATS,
                beats = BeatView(listOf(List(4) { 1f }, List(16) { 0.25f }), listOf("beats", "FL steps")), tune = clicks(4)),
        )),
        BasicsLesson("basics.piano-roll", 2, "Keyboard to Piano roll", "Turn the keyboard on its side: that's the Piano roll. Up is higher, right is later.", 3, listOf(
            show("Turn the keyboard on its side and you get the Piano roll. Low notes are at the bottom. High notes are at the top.", visual = Visual.ROLL),
            show("Time goes left to right, like reading a book. Each column is one beat. A note is a little bar you draw.", visual = Visual.ROLL,
                roll = RollView(shown = setOf(C to 0, E to 1, G to 2, C2 to 3)), tune = seq(C, E, G, C2)),
            roll("Your turn!", "Put C on beat 1.", setOf(C to 0), "Find the red C row at the bottom. Tap the first column.", "One note: C, on beat 1."),
            roll("Let's climb.", "Place C, D, E, F on beats 1 to 4.", setOf(C to 0, D to 1, E to 2, F to 3), "Like stairs: one note in each column, each one a white row higher.",
                "A climbing tune, like walking up stairs!"),
            roll("Stack it!", "Put C, E and G all on beat 1.", setOf(C to 0, E to 0, G to 0), "Put three notes in the first column: red C, yellow E, blue G.",
                "A chord in the Piano roll: notes stacked in one column."),
            show("That's just how FL Studio's Piano roll works! Next come some ear games. Then you'll make beats in FL's Channel Rack.", visual = Visual.ROLL,
                roll = RollView(shown = setOf(C to 0, E to 0, G to 0, D to 1, F to 2, E to 3)), tune = chord(C, E, G, dur = 0.5f) + seq(D, F, E)),
        )),
    )

    val lessons: List<BasicsLesson> = part1 + part2
    val ids: List<String> = lessons.map { it.id }
    /** Any page-style lesson: Music basics or Ear training II. */
    fun byId(id: String?) = lessons.firstOrNull { it.id == id } ?: EarTwo.byId(id)
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
