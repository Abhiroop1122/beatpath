package com.beatpath.app.lessons

import com.beatpath.app.lessons.Basics.Arc
import com.beatpath.app.lessons.Basics.BeatView
import com.beatpath.app.lessons.Basics.Hit
import com.beatpath.app.lessons.Basics.Kind
import com.beatpath.app.lessons.Basics.KeyView
import com.beatpath.app.lessons.Basics.Page
import com.beatpath.app.lessons.Basics.RollView
import com.beatpath.app.lessons.Basics.TapMode
import com.beatpath.app.lessons.Basics.Tune
import com.beatpath.app.lessons.Basics.Visual
import com.beatpath.app.lessons.Basics.chords
import com.beatpath.app.lessons.Basics.seq

/**
 * Ear training II: hearing jumps, chord moods inside a song, the four-chord loop, the way home,
 * long and short notes, and a mixed quiz. Same page format and screen as [Basics].
 * Pitches use FL Studio's numbering: MIDI 60 = C5.
 */
object EarTwo {

    // ---------------- page builders (same signatures as Basics') ----------------
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
    private const val C = 60; private const val D = 62; private const val E = 64; private const val F = 65
    private const val G = 67; private const val A = 69; private const val B = 71; private const val C2 = 72
    private const val D2 = 74; private const val E2 = 76; private const val G2 = 79

    private val CMAJ = listOf(C, E, G); private val DMIN = listOf(D, F, A); private val EMIN = listOf(E, G, B)
    private val FMAJ = listOf(F, A, C2); private val GMAJ = listOf(G, B, D2); private val AMIN = listOf(A, C2, E2)
    private val LOOP = chords(CMAJ, GMAJ, AMIN, FMAJ)

    /** One pitch played with these lengths, in beats (one beat = [beatMs]). */
    private fun lengths(pitch: Int, beats: List<Float>, beatMs: Int = 500): Tune {
        var t = 0f
        return Tune(beats.map { b -> Hit(listOf(pitch), (t * beatMs).toInt(), b * beatMs / 1000f * 0.9f).also { t += b } })
    }
    private fun two(a: Int, b: Int) = seq(a, b, gap = 650, dur = 0.6f)

    private val JUMPS = listOf("2nd · step", "5th · jump", "Octave")
    private const val JUMP_HINT = "Did it move to the next note, jump far, or land on the same letter?"
    private val MOOD = listOf("Major · bright", "Minor · dark")
    private val HOME = listOf("Yes, finished", "No, unfinished")

    // =====================================================================
    val lessons: List<Basics.BasicsLesson> = listOf(
        Basics.BasicsLesson("eartwo.intervals", 3, "Hear the jump", "A 2nd is a tiny step, a 5th a big jump, an octave the same letter higher.", 3, listOf(
            show("Remember intervals from the basics? A 2nd is a tiny move, like stepping to the next stair.",
                KeyView(lit = setOf(C, D), arcs = listOf(Arc(C, D, "2nd"))), two(C, D)),
            show("A 5th is a big, strong jump, like a trumpet call. Listen: C up to G.",
                KeyView(lit = setOf(C, G), arcs = listOf(Arc(C, G, "5th"))), two(C, G)),
            show("An octave is the biggest jump here. Same letter, 12 keys up. It almost sounds like one note.",
                KeyView(lit = setOf(C, C2), arcs = listOf(Arc(C, C2, "octave"))), two(C, C2)),
            choose("Ears only now.", "Which jump was it?", JUMPS, 1, "D up to A is a 5th. Big and bold.", JUMP_HINT, two(D, A)),
            choose("Again.", "Which jump was it?", JUMPS, 0, "F to G is the very next white key. A 2nd.", JUMP_HINT, two(F, G)),
            choose("And this one?", "Which jump was it?", JUMPS, 2, "G up to the next G. An octave blends almost into one sound.", JUMP_HINT, two(G, G2)),
            choose("Jumps can go down too.", "Which jump was it?", JUMPS, 1, "G down to C is a 5th, going down.", JUMP_HINT, two(G, C)),
            tap("Now find one yourself.", "Tap the note a 5th above the glowing C.", listOf(G), "Count five letters: C, D, E, F, G.", "C to G: a 5th, 7 half steps.",
                KeyView(lit = setOf(C))),
        )),
        Basics.BasicsLesson("eartwo.moods", 3, "Moods inside a song", "In a song, chords take turns. Major ones sound bright, minor ones sound dark.", 3, listOf(
            show("Remember major and minor? Major sounds bright, minor sounds darker. In songs they take turns.",
                KeyView(lit = setOf(C, E, G)), chords(CMAJ, AMIN, gap = 1100, dur = 1.0f)),
            choose("Listen to the last chord.", "Is the last chord bright or dark?", MOOD, 1, "The last one was A minor. Dark and soft.",
                "Play it again and listen to the very end.", chords(CMAJ, FMAJ, AMIN)),
            choose("Another song.", "Is the last chord bright or dark?", MOOD, 0, "It ended on G major. Bright!",
                "Play it again and listen to the very end.", chords(AMIN, DMIN, GMAJ)),
            choose("One more.", "Is the last chord bright or dark?", MOOD, 1, "That was E minor: E, G, B. Dark.",
                "Play it again and listen to the very end.", chords(CMAJ, EMIN)),
            choose("Now find it in the middle.", "Which chord was dark?", listOf("First", "Second", "Third"), 1, "The second one was D minor.",
                "Listen for the one that sounds a little sad.", chords(CMAJ, DMIN, GMAJ)),
            tap("Build the dark chord from the first song.", "Build A minor: A, C, E.", AMIN, "A, skip B, C, skip D, E. All white keys.",
                "A minor. Now you can see the dark sound too.", mode = TapMode.CHORD),
        )),
        Basics.BasicsLesson("eartwo.four-chords", 3, "The four-chord loop", "C, G, A minor, F: chords I, V, vi and IV. Thousands of songs use it.", 3, listOf(
            show("Remember chord numbers? This loop is in thousands of songs: C, G, A minor, F. In numbers: I, V, vi, IV.",
                KeyView(tags = mapOf(C to "I", G to "V", A to "vi", F to "IV")), LOOP),
            show("The dark one always comes third: A minor, chord vi. Listen for it.",
                KeyView(lit = setOf(A, C2, E2), tags = mapOf(A to "vi")), LOOP),
            choose("Ears only.", "Is this the four-chord loop?", listOf("Yes", "No, something else"), 0, "C, G, A minor, F. That's the one.",
                "Listen for a dark chord in third place.", LOOP),
            choose("And this one?", "Is this the four-chord loop?", listOf("Yes", "No, something else"), 1, "That was C, F, G, C. No dark chord at all.",
                "Listen for a dark chord in third place.", chords(CMAJ, FMAJ, GMAJ, CMAJ)),
            choose("Count along.", "Which chord came third?", listOf("F", "G", "A minor"), 2, "A minor, the dark one. Chord vi.",
                "Count the chords: one, two, three.", LOOP),
            tap("Build one from the loop.", "Build chord V, the second chord of the loop.", GMAJ, "V starts on note 5, G. Then G, B, D.",
                "G major, chord V. It wants to move on.", mode = TapMode.CHORD),
        )),
        Basics.BasicsLesson("eartwo.home", 3, "Hear the way home", "A tune feels finished when it lands on home, note 1.", 3, listOf(
            show("Remember home, note 1? A tune that lands there sounds finished, like the last word of a story.",
                KeyView(tags = mapOf(C to "1"), lit = setOf(C)), seq(E, D, F, E, D, C)),
            choose("Ears only.", "Did this tune end at home?", HOME, 0, "It landed on C. Home.",
                "Does it feel like it could stop there?", seq(G, E, F, D, C, dur = 0.5f)),
            choose("And this one?", "Did this tune end at home?", HOME, 1, "It stopped on A. It wants to keep going.",
                "Does it feel like it could stop there?", seq(C, E, G, A, dur = 0.5f)),
            choose("Two endings.", "Which ending sounds finished?", listOf("The first", "The second"), 1, "The second one walked down to C.",
                "Which one feels like a full stop?", seq(G, F, E, D, dur = 0.5f) + seq(G, F, D, C, dur = 0.5f)),
            tap("Finish it.", "This tune stops on D. Tap the home note it wants.", listOf(C), "Home is note 1, C. It sits one step below D.",
                "D steps down to C. Home!", KeyView(lit = setOf(D)), anyOctave = true, tune = seq(A, G, F, E, D)),
            tap("This one climbs.", "The tune climbs to B. Tap where it wants to go.", listOf(C2), "B is note 7. It pulls up a half step to home.",
                "B up to C. Tension, then rest.", KeyView(lit = setOf(B)), tune = seq(G, A, B)),
        )),
        Basics.BasicsLesson("eartwo.rhythm", 3, "Long and short notes", "Some notes last a long time, some are quick. Together they make rhythm.", 3, listOf(
            show("Remember note lengths? Notes can be long, like a yawn. Or short, like a hop. Listen.",
                visual = Visual.BEATS, beats = BeatView(listOf(listOf(2f, 1f, 1f)), listOf("long, short, short")), tune = lengths(G, listOf(2f, 1f, 1f))),
            choose("Which row did you hear?", "Which rhythm was it?", listOf("A", "B"), 1, "Short, short, long. Row B.", "Listen for where the long note is.",
                tune = lengths(E, listOf(1f, 1f, 2f)), visual = Visual.BEATS, beats = BeatView(listOf(listOf(2f, 1f, 1f), listOf(1f, 1f, 2f)), listOf("A", "B"))),
            choose("Ears only.", "Which note was the longest?", listOf("First", "Second", "Third"), 1, "The second one rang for 2 beats.",
                "Which one kept ringing?", lengths(C, listOf(1f, 2f, 1f))),
            choose("Quick or slow?", "Were these notes long or short?", listOf("Mostly long", "Mostly short"), 1, "Lots of quick half-beat notes. Short!",
                "Do they hop, or float?", lengths(E, List(8) { 0.5f })),
            choose("And these?", "Were these notes long or short?", listOf("Mostly long", "Mostly short"), 0, "Two notes, 2 beats each. Long and floaty.",
                "Do they hop, or float?", lengths(C, listOf(2f, 2f))),
            choose("One more row.", "Which rhythm was it?", listOf("A", "B"), 0, "Short, short, then longer and longer. Row A.", "Is the long note at the start or the end?",
                tune = lengths(G, listOf(0.5f, 0.5f, 1f, 2f)), visual = Visual.BEATS,
                beats = BeatView(listOf(listOf(0.5f, 0.5f, 1f, 2f), listOf(2f, 1f, 0.5f, 0.5f)), listOf("A", "B"))),
        )),
        Basics.BasicsLesson("eartwo.quiz", 3, "Ear quiz", "Jumps, moods, the four-chord loop, home and rhythm, all mixed up.", 4, listOf(
            show("Quiz time! Everything from this unit, all mixed up. You can play each sound again.", visual = Visual.NONE),
            choose("A jump.", "Which jump was it?", JUMPS, 1, "E up to B is a 5th.", JUMP_HINT, two(E, B)),
            choose("A mood.", "Is the last chord bright or dark?", MOOD, 1, "D minor at the end. Dark.",
                "Play it again and listen to the very end.", chords(FMAJ, DMIN)),
            choose("The famous loop.", "Which chord came third?", listOf("F", "G", "A minor"), 2, "A minor, chord vi.",
                "Count the chords: one, two, three.", LOOP),
            choose("Home or not?", "Did this tune end at home?", HOME, 1, "It stopped on G. Not home yet.",
                "Does it feel like it could stop there?", seq(C, D, E, G, dur = 0.5f)),
            choose("Long and short.", "Which note was the longest?", listOf("First", "Second", "Third"), 0, "The first one rang for 2 beats.",
                "Which one kept ringing?", lengths(E, listOf(2f, 1f, 1f))),
            roll("Hear it, then draw it.", "Draw the 4 notes you hear, one per beat.", setOf(C to 0, D to 1, E to 2, C to 3),
                "It starts on C, steps up twice, then jumps back down to C.", "You wrote down a tune by ear!").copy(tune = seq(C, D, E, C)),
            tap("Last one.", "Tap the note an octave above the glowing E.", listOf(E2), "Same yellow color, 12 keys to the right.", "E to E: an octave.",
                KeyView(lit = setOf(E)), tune = two(E, E2)),
        )),
    )

    val ids: List<String> = lessons.map { it.id }
    fun byId(id: String?) = lessons.firstOrNull { it.id == id }
}
