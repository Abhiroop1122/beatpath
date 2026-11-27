package com.beatpath.app

import com.beatpath.app.lessons.Basics
import com.beatpath.app.lessons.Basics.Kind
import com.beatpath.app.lessons.Basics.TapMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BasicsTest {
    private val pages = Basics.lessons.flatMap { l -> l.pages.map { l.id to it } }

    @Test fun unitShape() {
        assertEquals(16, Basics.lessons.size)
        assertEquals("ids are unique", Basics.ids.size, Basics.ids.toSet().size)
        assertTrue(Basics.ids.all { it.startsWith("basics.") })
        Basics.lessons.forEach { l ->
            assertTrue("${l.id} has 4–9 pages", l.pages.size in 4..9)
            assertTrue("${l.id} asks something", l.pages.any { it.kind != Kind.SHOW })
            l.pages.forEach { p -> assertTrue("${l.id}: Lou's line stays short (${p.say.split(" ").size} words)", p.say.split(" ").size <= 32) }
        }
    }

    @Test fun everyQuestionPassesWhenRightAndFailsWhenWrong() {
        pages.forEach { (id, p) ->
            when (p.kind) {
                Kind.TAP -> {
                    assertTrue("$id: targets on the keyboard", p.target.all { it in p.keys.range })
                    if (p.mode == TapMode.SEQUENCE) {
                        val played = mutableListOf<Int>()
                        p.target.forEach { k -> assertTrue("$id: sequence accepts $k", Basics.sequenceOk(p, played, k)); played += k }
                        assertTrue(Basics.judgeTap(p, played).pass)
                        assertFalse("$id: a wrong first key is refused", Basics.sequenceOk(p, emptyList(), p.target.first() + 1))
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
                }
                Kind.ROLL -> {
                    assertTrue("$id: cells fit the roll", p.cells.all { it.first in p.roll.rows && it.second in 0..3 })
                    assertTrue(Basics.judgeRoll(p, p.cells).pass)
                    assertFalse(Basics.judgeRoll(p, p.cells.drop(1).toSet()).pass)
                    assertFalse(Basics.judgeRoll(p, p.cells + (p.roll.rows.first to 3)).pass || (p.roll.rows.first to 3) in p.cells)
                }
                Kind.SHOW -> {}
            }
        }
    }

    /** The theory itself: every chord is a real major/minor triad, every scale follows W W H W W W H. */
    @Test fun theoryIsRight() {
        pages.filter { it.second.kind == Kind.TAP && it.second.mode == TapMode.CHORD }.forEach { (id, p) ->
            val s = p.target.sorted(); val shape = s[1] - s[0] to s[2] - s[1]
            assertTrue("$id: ${p.target} is a root-position triad, got $shape", shape == (4 to 3) || shape == (3 to 4))
        }
        pages.filter { it.second.mode == TapMode.SEQUENCE }.forEach { (id, p) ->
            assertEquals("$id: major-scale steps", listOf(2, 2, 1, 2, 2, 2, 1), p.target.zipWithNext { a, b -> b - a })
        }
        // "X + n half steps" tasks: the answer really is n keys above the glowing key
        pages.filter { it.second.task.contains(Regex("\\+ \\d+ half steps")) }.forEach { (id, p) ->
            val n = Regex("\\+ (\\d+)").find(p.task)!!.groupValues[1].toInt()
            assertEquals("$id: ${p.task}", n, p.target.single() - p.keys.lit.single())
        }
        assertEquals("C♯/D♭", Basics.bothNames(61))
        assertEquals("E", Basics.bothNames(64))
    }
}
