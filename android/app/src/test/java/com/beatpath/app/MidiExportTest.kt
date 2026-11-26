package com.beatpath.app

import com.beatpath.app.data.MidiExport
import com.beatpath.app.model.ChannelKind
import com.beatpath.app.model.Note
import com.beatpath.app.model.Pattern
import com.beatpath.app.model.Project
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MidiExportTest {

    private data class Ev(val tick: Int, val status: Int, val d1: Int, val d2: Int)
    private data class Track(val events: List<Ev>, val endTick: Int)

    /** Minimal independent SMF parser: header + tracks, absolute ticks, meta end-of-track. */
    private fun parse(bytes: ByteArray): Triple<Int, Int, List<Track>> {
        var i = 0
        fun u8() = bytes[i++].toInt() and 0xFF
        fun u16() = (u8() shl 8) or u8()
        fun u32() = (u16() shl 16) or u16()
        fun vlq(): Int { var v = 0; while (true) { val b = u8(); v = (v shl 7) or (b and 0x7F); if (b and 0x80 == 0) return v } }
        assertEquals("MThd", String(bytes, 0, 4)); i = 4
        assertEquals(6, u32())
        val format = u16(); val ntrk = u16(); val ppq = u16()
        val tracks = (0 until ntrk).map {
            assertEquals("MTrk", String(bytes, i, 4)); i += 4
            val len = u32(); val end = i + len
            var tick = 0; val evs = mutableListOf<Ev>(); var endTick = -1
            while (i < end) {
                tick += vlq()
                val st = u8()
                if (st == 0xFF) { val type = u8(); val l = vlq(); i += l; if (type == 0x2F) endTick = tick }
                else evs += Ev(tick, st, u8(), u8())
            }
            Track(evs, endTick)
        }
        assertEquals(bytes.size, i)
        return Triple(format, ppq, tracks)
    }

    private fun project(): Project {
        var p = Pattern(bars = 1)
        for (s in listOf(0, 4, 8, 12)) p = p.toggleStep(ChannelKind.KICK, s)
        // C major block chord on beat 1 with a leading-rest-free start, and a lead with a leading rest
        p = p.update(ChannelKind.CHORDS) { c -> c.copy(notes = listOf(60, 64, 67).map { Note(it.toLong(), it, 0, 480, 100) }) }
        p = p.update(ChannelKind.LEAD) { c -> c.copy(notes = listOf(Note(1, 72, 480, 120, 90), Note(2, 72, 600, 120, 60))) }
        return Project(1, "Test", 96, p)
    }

    @Test fun headerFormatAndResolution() {
        val (format, ppq, tracks) = parse(MidiExport.pattern(project()))
        assertEquals(1, format)
        assertEquals(480, ppq)
        assertEquals(4, tracks.size) // tempo + kick + chords + lead (empty channels omitted)
    }

    @Test fun everyTrackEndsOnThePatternBoundary() {
        val (_, _, tracks) = parse(MidiExport.pattern(project()))
        tracks.forEach { assertEquals(1920, it.endTick) }
    }

    @Test fun drumsUseChannel10AndGmKick() {
        val (_, _, tracks) = parse(MidiExport.pattern(project()))
        val kickOns = tracks[1].events.filter { it.status == 0x99 }
        assertEquals(listOf(0, 480, 960, 1440), kickOns.map { it.tick })
        assertTrue(kickOns.all { it.d1 == 36 })
    }

    @Test fun chordTonesShareOneStartTick() {
        val (_, _, tracks) = parse(MidiExport.pattern(project()))
        val ons = tracks[2].events.filter { it.status == 0x90 }
        assertEquals(setOf(60, 64, 67), ons.map { it.d1 }.toSet())
        assertTrue(ons.all { it.tick == 0 })
    }

    @Test fun leadingRestVelocityAndSamePitchRetriggerSurvive() {
        val (_, _, tracks) = parse(MidiExport.pattern(project()))
        val lead = tracks[3].events
        assertEquals(480, lead.first { it.status == 0x90 }.tick) // leading rest kept
        assertEquals(listOf(90, 60), lead.filter { it.status == 0x90 }.map { it.d2 })
        // at tick 600 the note-off of the first C6 must come before the next note-on
        val at600 = lead.filter { it.tick == 600 }
        assertEquals(0x80, at600.first().status)
        assertEquals(0x90, at600.last().status)
    }

    @Test fun toggleStepIsReversible() {
        val p = Pattern().toggleStep(ChannelKind.HAT, 2)
        assertTrue(p.channel(ChannelKind.HAT).stepOn(2))
        assertFalse(p.toggleStep(ChannelKind.HAT, 2).channel(ChannelKind.HAT).stepOn(2))
    }
}
