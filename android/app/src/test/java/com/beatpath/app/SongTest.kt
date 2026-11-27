package com.beatpath.app

import com.beatpath.app.model.ChannelKind
import com.beatpath.app.model.Clip
import com.beatpath.app.model.Note
import com.beatpath.app.model.Pattern
import com.beatpath.app.model.Project
import com.beatpath.app.model.TICKS_PER_BAR
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SongTest {
    private fun loop(bars: Int, pitch: Int) = Pattern(bars).setNotes(ChannelKind.LEAD, (0 until bars).map { Note(Pattern.nextId(), pitch, it * TICKS_PER_BAR, TICKS_PER_BAR) })

    @Test fun clipsNeverStack() {
        // loop A is 2 bars, loop B is 1 bar; A on bar 1 then B on bar 2: bar 2 must play only B
        val p = Project(1, "song", pattern = loop(2, 60), others = mapOf(1 to loop(1, 67)), current = 0,
            playlist = listOf(Clip(0, 0, 0), Clip(1, 1, 0)), songMode = true, songBars = 8)
        val notes = p.songPattern().channel(ChannelKind.LEAD).notes
        val bar2 = notes.filter { it.startTick / TICKS_PER_BAR == 1 }
        assertEquals(listOf(67), bar2.map { it.pitch })
        assertTrue("no note runs past its clip", notes.filter { it.pitch == 60 }.all { it.startTick + it.durationTicks <= TICKS_PER_BAR })
    }

    @Test fun aLongLoopFillsItsBarsWhenNothingFollows() {
        val p = Project(1, "song", pattern = loop(2, 60), playlist = listOf(Clip(0, 4, 0)), songMode = true, songBars = 8)
        val bars = p.songPattern().channel(ChannelKind.LEAD).notes.map { it.startTick / TICKS_PER_BAR }
        assertEquals(listOf(4, 5), bars.sorted())
    }

    @Test fun loopModePlaysTheLoopOnly() {
        val p = Project(1, "song", pattern = loop(1, 60), playlist = listOf(Clip(0, 3, 0)), songMode = false)
        assertEquals(1, p.songPattern().bars)
    }
}
