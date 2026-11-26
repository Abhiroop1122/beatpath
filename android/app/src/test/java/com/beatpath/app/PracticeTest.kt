package com.beatpath.app

import com.beatpath.app.lessons.Practice
import com.beatpath.app.lessons.Seed
import com.beatpath.app.model.ChannelKind
import com.beatpath.app.model.Pattern
import com.beatpath.app.model.Project
import com.beatpath.app.ui.lesson.EarKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PracticeTest {
    @Test fun generatedEarItemsAreAlwaysAnswerable() {
        val rnd = Random(7)
        repeat(300) {
            val step = Practice.item(EarKind.StepLeap, rnd); val gap = kotlin.math.abs(step.b - step.a)
            assertTrue("step/leap gap is clearly one or the other: $gap", gap in 1..2 || gap in 4..9)
            val hl = Practice.item(EarKind.HigherPad, rnd); assertTrue("high/low notes differ", hl.a != hl.b)
            val cs = Practice.item(EarKind.ChordSpot, rnd); assertTrue(cs.b in 0..2)
            val cq = Practice.item(EarKind.ChordQuality, rnd); assertTrue(cq.b in 0..1)
        }
    }

    @Test fun dailyMixLeansOnWeakSkills() {
        val weak = Practice.Skill.CHORD_SPOT
        val kinds = (0 until 50).flatMap { Practice.mixKinds(Random(it), acc = { s -> if (s == weak) 0.1f else 0.95f }) }
        val share = kinds.count { it == EarKind.ChordSpot } / kinds.size.toFloat()
        assertTrue("weak skill gets the biggest share ($share)", share > 0.3f)
        assertEquals(8, Practice.earSpec(Practice.MIX, Random(1)).items.size)
        assertFalse("practice skips the teach step", Practice.earSpec(Practice.MIX, Random(1)).teach)
        // never three of the same in a row
        Practice.mixKinds(Random(3), n = 40, acc = { 0.5f }).windowed(3).forEach { assertFalse(it.all { k -> k == it[0] }) }
    }

    @Test fun dictationChecksTheCopiedTune() {
        val lesson = Practice.lesson(Practice.DICTATION, 42)!!
        val start = lesson.setup(Project(1, "t", pattern = Pattern(2)))
        val melody = start.pattern.channel(ChannelKind.LEAD2).notes.sortedBy { it.startTick }.map { it.pitch }
        assertEquals(4, melody.size)
        val check = lesson.steps[0].check
        assertFalse(check(start, com.beatpath.app.lessons.Meters()).pass)
        val right = start.copy(pattern = start.pattern.setNotes(ChannelKind.LEAD, melody.mapIndexed { i, p -> Seed.note(p, 4f + i, 1f) }))
        assertTrue(check(right, com.beatpath.app.lessons.Meters()).pass)
        val wrong = start.copy(pattern = start.pattern.setNotes(ChannelKind.LEAD, melody.mapIndexed { i, p -> Seed.note(if (i == 2) p + 2 else p, 4f + i, 1f) }))
        val v = check(wrong, com.beatpath.app.lessons.Meters())
        assertFalse(v.pass); assertTrue(v.body.contains("Note 3"))
    }

    @Test fun spacedReviewMovesOutThenRetires() {
        val today = 100L
        assertNull("solo first time never needs review", Practice.nextReview(null, true, today))
        assertEquals(101L to 0, Practice.nextReview(null, false, today))
        assertEquals(103L to 1, Practice.nextReview(101L to 0, true, today))
        assertEquals(107L to 2, Practice.nextReview(103L to 1, true, today))
        assertNull("mastered after the 7-day pass", Practice.nextReview(107L to 2, true, today))
        assertEquals("help again resets to tomorrow", 101L to 0, Practice.nextReview(107L to 2, false, today))
    }
}
