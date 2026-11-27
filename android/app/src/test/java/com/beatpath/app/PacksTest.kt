package com.beatpath.app

import com.beatpath.app.lessons.Basics
import com.beatpath.app.lessons.Basics.Kind
import com.beatpath.app.lessons.Basics.TapMode
import com.beatpath.app.lessons.Curriculum
import com.beatpath.app.lessons.EarTwo
import com.beatpath.app.lessons.Lesson
import com.beatpath.app.lessons.Meters
import com.beatpath.app.lessons.PacksCurriculum
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
import com.beatpath.app.model.Project
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PacksTest {
    private val BAR = 1920
    private val BEAT = 480
    private val STEP = 120

    // ---------------- project editing helpers ----------------
    private fun nn(pitch: Int, tick: Int, len: Int) = Note(Pattern.nextId(), pitch, tick, len)
    private fun Project.notesOf(k: ChannelKind) = pattern.channel(k).notes
    private fun Project.put(k: ChannelKind, ns: List<Note>) = copy(pattern = pattern.setNotes(k, ns))
    private fun Project.add(k: ChannelKind, vararg ns: Triple<Int, Int, Int>) = put(k, notesOf(k) + ns.map { (p, t, d) -> nn(p, t, d) })
    /** Replace the chord starting on beat 1 of [bar0] with [pitches] (a whole bar long). */
    private fun Project.chord(bar0: Int, vararg pitches: Int) =
        put(CHORDS, notesOf(CHORDS).filter { it.startTick != bar0 * BAR } + pitches.map { nn(it, bar0 * BAR, BAR) })
    private fun Project.chords(vararg bars: List<Int>) = put(CHORDS, bars.flatMapIndexed { b, ps -> ps.map { nn(it, b * BAR, BAR) } })
    /** Drum row = exactly these 0-based steps. */
    private fun Project.drums(k: ChannelKind, vararg steps0: Int) = put(k, steps0.map { nn(k.drumPitch, it * STEP, STEP) })
    private fun Project.line(k: ChannelKind, vararg ns: Triple<Int, Int, Int>) = put(k, ns.map { (p, t, d) -> nn(p, t, d) })

    private data class Case(val right: (Project) -> Project, val wrong: (Project) -> Project)

    private val climb = listOf(
        Triple(60, 0, BEAT), Triple(62, BEAT, BEAT), Triple(64, 2 * BEAT, BEAT), Triple(65, 3 * BEAT, BEAT),
        Triple(67, BAR, BEAT), Triple(69, BAR + BEAT, BEAT), Triple(71, BAR + 2 * BEAT, 2 * BEAT),
        Triple(69, 2 * BAR + 2 * BEAT, BEAT), Triple(67, 2 * BAR + 3 * BEAT, BEAT),
        Triple(65, 3 * BAR, BEAT), Triple(64, 3 * BAR + BEAT, BEAT), Triple(62, 3 * BAR + 2 * BEAT, BEAT), Triple(60, 3 * BAR + 3 * BEAT, BEAT),
    )
    private val smooth = listOf(listOf(60, 64, 67), listOf(60, 65, 69), listOf(59, 62, 67), listOf(60, 64, 67))
    private val roots = listOf(listOf(60, 64, 67), listOf(65, 69, 72), listOf(67, 71, 74), listOf(60, 64, 67))

    /** For every step of every new Studio lesson: how to solve it, and one plausible wrong answer. */
    private val cases: Map<String, List<Case>> = mapOf(
        "voice.first-inversion" to listOf(
            Case({ it.chord(0, 64, 67, 72) }, { it.chord(0, 60, 64, 67, 72) }),
            Case({ it.chord(1, 69, 72, 77) }, { it.chord(1, 72, 77, 81) }),
        ),
        "voice.second-inversion" to listOf(
            Case({ it.chord(0, 67, 72, 76) }, { it.chord(0, 64, 67, 72) }),
            Case({ it.chord(1, 60, 65, 69) }, { it.chord(1, 57, 60, 65) }),
        ),
        "voice.open" to listOf(
            Case({ it.chord(0, 48, 55, 64) }, { it.chord(0, 52, 55, 60) }),
            Case({ it.chord(1, 45, 52, 60) }, { it.chord(1, 45, 48, 52) }),
        ),
        "voice.shared-note" to listOf(
            Case({ it.chord(1, 60, 65, 69) }, { it.chord(1, 65, 69, 72) }),
            Case({ it.chord(1, 59, 62, 67) }, { it.chord(1, 55, 59, 62) }),
        ),
        "voice.smooth-loop" to listOf(
            Case({ it.chords(*smooth.toTypedArray()) }, { it.chords(smooth[0], smooth[1], roots[2], smooth[3]) }),
            Case({ it.chords(*smooth.toTypedArray()) }, { it.chords(*roots.toTypedArray()) }),
        ),
        "voice.jam" to listOf(
            Case({ it.chords(listOf(60, 64, 67), listOf(60, 64, 69), listOf(60, 65, 69), listOf(59, 62, 67)) },
                { it.chords(listOf(60, 64, 67), listOf(57, 60, 64), listOf(65, 69, 72), listOf(67, 71, 74)) }),
        ),
        "groove.lofi" to listOf(
            Case({ it.drums(KICK, 0, 10) }, { it.drums(KICK, 0, 8) }),
            Case({ it.drums(HAT, *(0 until 16).toList().toIntArray()) }, { it.drums(HAT, 0, 2, 4, 6, 8, 10, 12, 14) }),
        ),
        "groove.house" to listOf(
            Case({ it.drums(KICK, 0, 4, 8, 12) }, { it.drums(KICK, 0, 4, 8) }),
            Case({ it.drums(OPEN_HAT, 2, 6, 10, 14) }, { it.drums(OPEN_HAT, 0, 4, 8, 12) }),
        ),
        "groove.trap" to listOf(
            Case({ it.drums(CLAP, 8, 24) }, { it.drums(CLAP, 8) }),
            Case({ it.add(HAT, Triple(42, 3420, 60), Triple(42, 3480, 60)) }, { it.add(HAT, Triple(42, 3480, 60)) }),
        ),
        "groove.pop" to listOf(
            Case({ it.drums(KICK, 0, 8, 10) }, { it.drums(KICK, 0, 8) }),
            Case({ it.drums(SNARE, 4, 12) }, { it.drums(SNARE, 4) }),
        ),
        "groove.dnb" to listOf(
            Case({ it.drums(KICK, 0, 10) }, { it.drums(KICK, 0, 4, 8, 12) }),
            Case({ it.drums(SNARE, 4, 12) }, { it.drums(SNARE, 4, 8, 12) }),
        ),
        "groove.jam" to listOf(
            Case({ it.drums(KICK, 0, 4, 8, 12).drums(OPEN_HAT, 2, 6, 10, 14).drums(HAT, 0, 4, 8, 12) },
                { it.drums(KICK, 0, 4, 8, 12).drums(OPEN_HAT, 2, 6, 10, 14) }),
        ),
        "bass2.octave" to listOf(
            Case({ it.line(BASS, Triple(36, 0, BEAT), Triple(48, BEAT, BEAT), Triple(36, 2 * BEAT, BEAT), Triple(48, 3 * BEAT, BEAT)) },
                { it.line(BASS, Triple(36, 0, BEAT), Triple(48, BEAT, BEAT), Triple(36, 2 * BEAT, BEAT), Triple(47, 3 * BEAT, BEAT)) }),
            Case({ it.add(BASS, Triple(31, BAR, BEAT), Triple(43, BAR + BEAT, BEAT), Triple(31, BAR + 2 * BEAT, BEAT), Triple(43, BAR + 3 * BEAT, BEAT)) },
                { it.add(BASS, Triple(31, BAR, BEAT), Triple(43, BAR + BEAT, BEAT), Triple(31, BAR + 2 * BEAT, BEAT), Triple(55, BAR + 3 * BEAT, BEAT)) }),
        ),
        "bass2.walk-up" to listOf(
            Case({ it.add(BASS, Triple(36, 0, BEAT), Triple(38, BEAT, BEAT), Triple(40, 2 * BEAT, BEAT), Triple(41, 3 * BEAT, BEAT)) },
                { it.add(BASS, Triple(36, 0, BEAT), Triple(38, BEAT, BEAT), Triple(40, 2 * BEAT, BEAT), Triple(43, 3 * BEAT, BEAT)) }),
            Case({ it.add(BASS, Triple(41, 2 * BAR, BEAT), Triple(43, 2 * BAR + BEAT, BEAT), Triple(45, 2 * BAR + 2 * BEAT, BEAT), Triple(47, 2 * BAR + 3 * BEAT, BEAT)) },
                { it.add(BASS, Triple(41, 2 * BAR, BEAT), Triple(43, 2 * BAR + BEAT, BEAT), Triple(45, 2 * BAR + 2 * BEAT, BEAT), Triple(48, 2 * BAR + 3 * BEAT, BEAT)) }),
        ),
        "bass2.follow-kick" to listOf(
            Case({ it.line(BASS, Triple(45, 0, 240), Triple(45, 360, 240), Triple(45, 1200, 240)) }, { it.line(BASS, Triple(45, 0, 240), Triple(45, 360, 240)) }),
            Case({ it.add(BASS, Triple(41, BAR, 240), Triple(41, BAR + 360, 240), Triple(41, BAR + 1200, 240)) },
                { it.add(BASS, Triple(41, BAR, 240), Triple(41, BAR + 360, 240), Triple(41, BAR + 1200, 240), Triple(41, BAR + 480, 240)) }),
        ),
        "bass2.808-hold" to listOf(
            Case({ it.line(BASS, Triple(36, 0, 1200)) }, { it.line(BASS, Triple(36, 0, 480)) }),
            Case({ it.add(BASS, Triple(39, 1200, 720), Triple(36, BAR, 960), Triple(31, BAR + 960, 960)) },
                { it.add(BASS, Triple(39, 1300, 620), Triple(36, BAR, 960), Triple(31, BAR + 960, 960)) }),
        ),
        "bass2.jam" to listOf(
            Case({ it.line(BASS, *listOf(0, 7, 9, 5).flatMapIndexed { b, pc -> listOf(Triple(36 + pc, b * BAR, BEAT), Triple(48 + pc, b * BAR + 2 * BEAT, BEAT)) }.toTypedArray()) },
                { it.line(BASS, *listOf(0, 7, 9, 5).flatMapIndexed { b, pc -> listOf(Triple(36 + pc, b * BAR, BEAT), Triple(36 + pc, b * BAR + 2 * BEAT, BEAT)) }.toTypedArray()) }),
        ),
        "mel3.call-response" to listOf(
            Case({ it.line(LEAD2, Triple(67, BAR, BEAT), Triple(65, BAR + BEAT, BEAT), Triple(60, BAR + 2 * BEAT, BEAT)) },
                { it.line(LEAD2, Triple(67, BAR, BEAT), Triple(65, BAR + BEAT, BEAT), Triple(67, BAR + 2 * BEAT, BEAT)) }),
            Case({ it.add(LEAD2, Triple(67, 3 * BAR, 240), Triple(65, 3 * BAR + 240, 240), Triple(64, 3 * BAR + BEAT, BEAT), Triple(60, 3 * BAR + 2 * BEAT, BEAT)) },
                { it.add(LEAD2, Triple(67, 3 * BAR, BEAT), Triple(64, 3 * BAR + BEAT, BEAT), Triple(60, 3 * BAR + 2 * BEAT, BEAT)) }),
        ),
        "mel3.countermelody" to listOf(
            Case({ it.line(LEAD2, Triple(55, 0, BAR), Triple(55, BAR, BAR), Triple(57, 2 * BAR, BAR), Triple(57, 3 * BAR, BAR)) },
                { it.line(LEAD2, Triple(67, 0, BAR), Triple(55, BAR, BAR), Triple(57, 2 * BAR, BAR), Triple(57, 3 * BAR, BAR)) }),
            Case({ it.line(LEAD2, *listOf(55 to 52, 55 to 59, 57 to 60, 57 to 60).flatMapIndexed { b, (x, y) -> listOf(Triple(x, b * BAR, 2 * BEAT), Triple(y, b * BAR + 2 * BEAT, 2 * BEAT)) }.toTypedArray()) },
                { it.line(LEAD2, Triple(55, 0, BAR), Triple(55, BAR, BAR), Triple(57, 2 * BAR, BAR), Triple(57, 3 * BAR, BAR)) }),
        ),
        "mel3.peak" to listOf(
            Case({ it.add(LEAD, Triple(72, 2 * BAR, 2 * BEAT)) }, { it.add(LEAD, Triple(71, 2 * BAR, 2 * BEAT)) }),
            Case({ it.line(LEAD, *(climb + Triple(72, 2 * BAR, 2 * BEAT)).toTypedArray()) }, { it.line(LEAD, *climb.toTypedArray()) }),
        ),
        "mel3.end-home" to listOf(
            Case({ it.put(LEAD, it.notesOf(LEAD).map { n -> if (n.startTick == BAR) n.copy(pitch = 60) else n }) },
                { it.put(LEAD, it.notesOf(LEAD).map { n -> if (n.startTick == BAR) n.copy(pitch = 64) else n }) }),
            Case({ it.add(LEAD, Triple(60, BAR, 2 * BEAT)) }, { it.add(LEAD, Triple(60, BAR, BEAT)) }),
        ),
        "mel3.jam" to listOf(
            Case({
                val tune = climb.take(6) + listOf(Triple(71, BAR + 2 * BEAT, BEAT), Triple(72, 2 * BAR, 2 * BEAT), Triple(69, 2 * BAR + 2 * BEAT, BEAT), Triple(67, 2 * BAR + 3 * BEAT, BEAT),
                    Triple(65, 3 * BAR, BEAT), Triple(62, 3 * BAR + BEAT, BEAT), Triple(60, 3 * BAR + 2 * BEAT, 2 * BEAT))
                it.line(LEAD, *tune.toTypedArray())
            }, {
                val tune = climb.take(6) + listOf(Triple(71, BAR + 2 * BEAT, BEAT), Triple(72, 2 * BAR, 2 * BEAT), Triple(69, 2 * BAR + 2 * BEAT, BEAT), Triple(67, 2 * BAR + 3 * BEAT, BEAT),
                    Triple(65, 3 * BAR, BEAT), Triple(64, 3 * BAR + BEAT, BEAT), Triple(62, 3 * BAR + 2 * BEAT, 2 * BEAT))
                it.line(LEAD, *tune.toTypedArray())
            }),
        ),
    )

    private fun start(l: Lesson) = l.setup(Project(1, "t", l.bpm, Pattern(l.bars)))

    // =====================================================================
    // Studio packs
    // =====================================================================
    @Test fun levelsAndIds() {
        val ids = PacksCurriculum.lessons.map { it.id }
        assertEquals("ids are unique", ids.size, ids.toSet().size)
        // registered in the course exactly once, and no id clashes with an older lesson
        assertTrue("each pack lesson appears once in the course", ids.all { id -> Curriculum.lessons.count { it.id == id } == 1 })
        assertTrue(ids.all { it.startsWith("voice.") || it.startsWith("groove.") || it.startsWith("bass2.") || it.startsWith("mel3.") })
        assertEquals(listOf(19, 20, 21, 22), PacksCurriculum.levels.map { it.number })
        assertTrue(PacksCurriculum.levels.all { lv -> Curriculum.levels.count { it.number == lv.number } == 1 })
        PacksCurriculum.levels.forEach { lv ->
            val ls = PacksCurriculum.lessons.filter { it.level == lv.number }
            assertTrue("level ${lv.number} has 5–6 lessons", ls.size in 5..6)
            assertTrue("level ${lv.number} ends with a jam", ls.last().jam)
            assertEquals("level ${lv.number} has one jam", 1, ls.count { it.jam })
        }
        PacksCurriculum.lessons.forEach { l ->
            assertTrue("${l.id}: 1–3 steps", l.steps.size in 1..3)
            assertTrue("${l.id}: flTip set", l.flTip.isNotBlank())
        }
    }

    @Test fun firstStepGuidedLaterStepUnguided() {
        PacksCurriculum.lessons.filter { !it.jam }.forEach { l ->
            var p = start(l)
            l.steps.forEachIndexed { i, s ->
                p = s.setup?.invoke(p) ?: p
                val g = s.guides(p)
                if (i == 0) assertTrue("${l.id}: first step is guided", g.isNotEmpty() || s.hintGhosts.isNotEmpty())
                p = cases.getValue(l.id)[i].right(p)
            }
            assertTrue("${l.id}: a later step has no outlines", l.steps.size > 1 && l.steps.drop(1).all { s -> s.guides(start(l)).isEmpty() })
        }
    }

    @Test fun everyStepPassesWhenRightAndFailsWhenWrong() {
        assertEquals("every lesson has cases", PacksCurriculum.lessons.map { it.id }.toSet(), cases.keys)
        PacksCurriculum.lessons.forEach { l ->
            val cs = cases.getValue(l.id)
            assertEquals("${l.id}: one case per step", l.steps.size, cs.size)
            var p = start(l)
            l.steps.forEachIndexed { i, s ->
                p = s.setup?.invoke(p) ?: p
                val before = s.check(p, Meters())
                assertFalse("${l.id} step $i: passes before any work (${before.headline})", before.pass)
                assertTrue("${l.id} step $i: empty feedback", before.headline.isNotBlank() && before.body.isNotBlank())
                val bad = s.check(cs[i].wrong(p), Meters())
                assertFalse("${l.id} step $i: wrong answer passes", bad.pass)
                assertTrue("${l.id} step $i: wrong answer gets a reason", bad.body.isNotBlank())
                p = cs[i].right(p)
                val v = s.check(p, Meters())
                assertTrue("${l.id} step $i: right answer fails: ${v.headline} / ${v.body}", v.pass)
            }
        }
    }

    @Test fun emptyAnswersFail() {
        PacksCurriculum.lessons.forEach { l ->
            val empty = Project(1, "t", l.bpm, Pattern(l.bars))
            l.steps.forEachIndexed { i, s -> assertFalse("${l.id} step $i passes on an empty project", s.check(empty, Meters()).pass) }
        }
    }

    @Test fun grooveKitsAndTempos() {
        fun lesson(id: String) = PacksCurriculum.lessons.first { it.id == id }
        assertEquals(DrumKit.LOFI, start(lesson("groove.lofi")).mix.drumKit)
        assertTrue(lesson("groove.lofi").bpm in 70..90)
        assertTrue("lo-fi swings", start(lesson("groove.lofi")).pattern.swing > 0.2f)
        assertEquals(DrumKit.HOUSE, start(lesson("groove.house")).mix.drumKit); assertEquals(124, lesson("groove.house").bpm)
        assertEquals(DrumKit.EIGHT08, start(lesson("groove.trap")).mix.drumKit); assertEquals(140, lesson("groove.trap").bpm)
        assertEquals(DrumKit.CLASSIC, start(lesson("groove.pop")).mix.drumKit)
        assertEquals(DrumKit.EIGHT08, start(lesson("bass2.808-hold")).mix.drumKit)
    }

    @Test fun studioWordingIsKidFriendly() {
        PacksCurriculum.lessons.forEach { l ->
            listOf(l.title, l.idea).forEach { assertNoDash(l.id, it) }
            l.steps.forEach { s ->
                assertTrue("${l.id}: Lou says ${words(s.say)} words", words(s.say) <= 25)
                listOf(s.say, s.task, s.hint).forEach { t -> assertNoDash(l.id, t); assertShortSentences(l.id, t) }
            }
        }
    }

    // =====================================================================
    // Ear training II
    // =====================================================================
    private val earPages = EarTwo.lessons.flatMap { l -> l.pages.map { l.id to it } }

    @Test fun earUnitShape() {
        assertTrue(EarTwo.lessons.size in 5..7)
        assertEquals("ids are unique", EarTwo.ids.size, EarTwo.ids.toSet().size)
        assertTrue(EarTwo.ids.all { it.startsWith("eartwo.") })
        // Basics.byId finds Ear training II too (one route for page-style lessons), but the ids never clash with Music basics
        assertTrue(EarTwo.ids.none { id -> Basics.lessons.any { it.id == id } })
        assertTrue(EarTwo.ids.all { Basics.byId(it) != null })
        EarTwo.lessons.forEach { l ->
            assertEquals(3, l.part)
            assertTrue("${l.id} has 4–9 pages", l.pages.size in 4..9)
            assertTrue("${l.id} asks something", l.pages.any { it.kind != Kind.SHOW })
            assertTrue("${l.id} plays sounds", l.pages.any { it.tune != null })
            l.pages.forEach { p ->
                assertTrue("${l.id}: Lou's line has ${words(p.say)} words", words(p.say) <= 25)
                listOf(p.say, p.task, p.hint, p.success).forEach { t -> assertNoDash(l.id, t); assertShortSentences(l.id, t) }
                if (p.kind == Kind.CHOOSE && p.visual != Basics.Visual.KEYS) assertTrue("${l.id}: ear question has a sound: ${p.task}", p.tune != null)
            }
        }
    }

    @Test fun earQuestionsPassWhenRightAndFailWhenWrong() {
        earPages.forEach { (id, p) ->
            when (p.kind) {
                Kind.TAP -> {
                    assertTrue("$id: targets on the keyboard", p.target.all { it in p.keys.range })
                    if (p.mode == TapMode.SEQUENCE) {
                        val played = mutableListOf<Int>()
                        p.target.forEach { k -> assertTrue(Basics.sequenceOk(p, played, k)); played += k }
                        assertTrue(Basics.judgeTap(p, played).pass)
                    } else {
                        assertTrue("$id: right answer passes", Basics.judgeTap(p, p.target).pass)
                        val wrong = p.target.dropLast(1) + (p.target.last() + 1)
                        assertFalse("$id: wrong answer fails", Basics.judgeTap(p, wrong).pass)
                        assertFalse(Basics.judgeTap(p, emptyList()).pass)
                    }
                }
                Kind.CHOOSE -> {
                    assertTrue("$id: answer index valid", p.answer in p.options.indices)
                    assertTrue(Basics.judgeChoice(p, p.answer).pass)
                    p.options.indices.filter { it != p.answer }.forEach { assertFalse(Basics.judgeChoice(p, it).pass) }
                    assertFalse(Basics.judgeChoice(p, null).pass)
                }
                Kind.ROLL -> {
                    assertTrue("$id: cells fit the roll", p.cells.all { it.first in p.roll.rows && it.second in 0..3 })
                    assertTrue(Basics.judgeRoll(p, p.cells).pass)
                    assertFalse(Basics.judgeRoll(p, p.cells.drop(1).toSet()).pass)
                    assertFalse(Basics.judgeRoll(p, emptySet()).pass)
                }
                Kind.SHOW -> {}
            }
        }
    }

    /** The theory is right: chords are real triads, interval answers match the sounds played. */
    @Test fun earTheoryIsRight() {
        earPages.filter { it.second.kind == Kind.TAP && it.second.mode == TapMode.CHORD }.forEach { (id, p) ->
            val s = p.target.sorted(); val shape = s[1] - s[0] to s[2] - s[1]
            assertTrue("$id: ${p.target} is a root-position triad, got $shape", shape == (4 to 3) || shape == (3 to 4))
        }
        // "Which jump" questions: two notes, and the answer matches their distance
        earPages.filter { it.second.kind == Kind.CHOOSE && it.second.options.firstOrNull()?.startsWith("2nd") == true }.forEach { (id, p) ->
            val hits = p.tune!!.hits; assertEquals(2, hits.size)
            val d = kotlin.math.abs(hits[1].notes.single() - hits[0].notes.single())
            val want = when (d) { 1, 2 -> 0; 7 -> 1; 12 -> 2; else -> -1 }
            assertEquals("$id: ${p.task} distance $d", want, p.answer)
        }
        // "Bright or dark" questions: the last chord's middle note decides
        earPages.filter { it.second.options.firstOrNull()?.startsWith("Major") == true }.forEach { (id, p) ->
            val last = p.tune!!.hits.last().notes.sorted()
            assertEquals("$id: last chord $last", if (last[1] - last[0] == 4) 0 else 1, p.answer)
        }
        // "Did it end at home?" questions: home is C
        earPages.filter { it.second.options.firstOrNull() == "Yes, finished" }.forEach { (id, p) ->
            val lastNote = p.tune!!.hits.last().notes.single()
            assertEquals("$id: ends on ${Basics.name(lastNote)}", if (lastNote % 12 == 0) 0 else 1, p.answer)
        }
    }

    // ---------------- wording helpers ----------------
    private fun words(s: String) = s.split(Regex("\\s+")).count { it.isNotBlank() }
    private fun assertNoDash(id: String, s: String) = assertFalse("$id: no dashes in \"$s\"", s.contains("—") || s.contains(" – ") || s.contains(" - "))
    private fun assertShortSentences(id: String, s: String) =
        s.split(Regex("(?<=[.!?])\\s+")).forEach { sentence -> assertTrue("$id: sentence too long (${words(sentence)} words): \"$sentence\"", words(sentence) <= 16) }
}
