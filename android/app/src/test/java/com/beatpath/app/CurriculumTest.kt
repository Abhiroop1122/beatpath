package com.beatpath.app

import com.beatpath.app.lessons.Curriculum
import com.beatpath.app.lessons.Meters
import com.beatpath.app.model.ChannelKind
import com.beatpath.app.model.CompSettings
import com.beatpath.app.model.EqSettings
import com.beatpath.app.model.Note
import com.beatpath.app.model.Pattern
import com.beatpath.app.model.Project
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CurriculumTest {
    private fun start(id: String): Project {
        val l = Curriculum.byId(id)!!
        return l.setup(Project(1, "t", l.bpm, Pattern(l.bars)))
    }
    private fun stepSetup(id: String, i: Int, p: Project) = Curriculum.byId(id)!!.steps[i].setup?.invoke(p) ?: p
    private fun check(id: String, i: Int, p: Project, m: Meters = Meters()) = Curriculum.byId(id)!!.steps[i].check(p, m)
    private fun notes(vararg n: Triple<Int, Int, Int>) = n.map { (p, t, d) -> Note(Pattern.nextId(), p, t, d) }

    @Test fun idsUniqueAndLevelsCovered() {
        val ids = Curriculum.lessons.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(18, Curriculum.levels.size)
        Curriculum.levels.forEach { lv -> assertTrue("level ${lv.number} empty", Curriculum.inLevel(lv.number).isNotEmpty()) }
        assertEquals(76, Curriculum.lessons.size)
    }

    @Test fun everyCheckRunsOnSetupWithoutCrashing() {
        Curriculum.lessons.forEach { l ->
            var p = l.setup(Project(1, "t", l.bpm, Pattern(l.bars)))
            l.steps.forEachIndexed { i, s ->
                p = s.setup?.invoke(p) ?: p
                s.guides(p)
                val v = s.check(p, Meters())
                assertTrue("${l.id} step $i has empty feedback", v.headline.isNotBlank() && v.body.isNotBlank())
            }
        }
    }

    @Test fun fourOnTheFloor() {
        var p = start("rack.four-on-floor")
        assertFalse(check("rack.four-on-floor", 0, p).pass)
        listOf(0, 4, 8, 12).forEach { p = p.copy(pattern = p.pattern.toggleStep(ChannelKind.KICK, it)) }
        assertTrue(check("rack.four-on-floor", 0, p).pass)
        p = p.copy(pattern = p.pattern.toggleStep(ChannelKind.KICK, 2))
        val v = check("rack.four-on-floor", 0, p)
        assertFalse(v.pass); assertTrue(v.body.contains("step 3"))
    }

    @Test fun cMajorLateNoteIsCalledLate() {
        var p = start("roll.build-c-major")
        p = p.copy(pattern = p.pattern.setNotes(ChannelKind.CHORDS, notes(Triple(60, 0, 480), Triple(64, 0, 480), Triple(67, 480, 480))))
        val v = check("roll.build-c-major", 0, p)
        assertFalse(v.pass); assertTrue(v.headline.contains("off the beat"))
        p = p.copy(pattern = p.pattern.setNotes(ChannelKind.CHORDS, notes(Triple(60, 0, 480), Triple(64, 0, 480), Triple(55, 0, 480))))
        assertTrue("inversions allowed", check("roll.build-c-major", 0, p).pass)
    }

    @Test fun fourChordLoop() {
        var p = start("roll.four-chord-loop")
        val add = notes(Triple(55, 1920, 1920), Triple(59, 1920, 1920), Triple(62, 1920, 1920), Triple(57, 3840, 1920), Triple(60, 3840, 1920), Triple(64, 3840, 1920),
            Triple(53, 5760, 1920), Triple(57, 5760, 1920), Triple(60, 5760, 1920))
        p = p.copy(pattern = p.pattern.update(ChannelKind.CHORDS) { it.copy(notes = it.notes + add) })
        assertTrue(check("roll.four-chord-loop", 0, p).pass)
    }

    @Test fun bassRoots() {
        var p = start("roll.bass-roots")
        p = p.copy(pattern = p.pattern.setNotes(ChannelKind.BASS, notes(Triple(36, 0, 1920), Triple(43, 1920, 1920), Triple(45, 3840, 1920), Triple(41, 5760, 1920))))
        assertTrue(check("roll.bass-roots", 0, p).pass)
        p = p.copy(pattern = p.pattern.setNotes(ChannelKind.BASS, notes(Triple(36, 0, 1920), Triple(42, 1920, 1920), Triple(45, 3840, 1920), Triple(41, 5760, 1920))))
        assertFalse(check("roll.bass-roots", 0, p).pass)
    }

    @Test fun motifRepeat() {
        var p = start("roll.motif-repeat")
        val b1 = notes(Triple(60, 0, 240), Triple(62, 240, 240), Triple(64, 960, 240))
        p = p.copy(pattern = p.pattern.setNotes(ChannelKind.LEAD, b1 + b1.map { it.copy(id = Pattern.nextId(), startTick = it.startTick + 1920) }))
        assertTrue(check("roll.motif-repeat", 0, p).pass)
    }

    @Test fun voiceLeadingRejectsRootPositionAcceptsSmooth() {
        var p = start("roll.voice-leading")
        assertFalse(check("roll.voice-leading", 0, p).pass)
        p = p.copy(pattern = p.pattern.setNotes(ChannelKind.CHORDS, notes(Triple(60, 0, 1920), Triple(64, 0, 1920), Triple(67, 0, 1920),
            Triple(59, 1920, 1920), Triple(62, 1920, 1920), Triple(67, 1920, 1920), Triple(60, 3840, 1920), Triple(64, 3840, 1920), Triple(69, 3840, 1920),
            Triple(60, 5760, 1920), Triple(65, 5760, 1920), Triple(69, 5760, 1920))))
        assertTrue(check("roll.voice-leading", 0, p).pass)
    }

    @Test fun wavHeaderIsValid() {
        val bytes = com.beatpath.app.data.WavExport.encode(FloatArray(96) { (it % 2) * 0.5f }, 48000)
        assertEquals("RIFF", String(bytes, 0, 4)); assertEquals("WAVE", String(bytes, 8, 4)); assertEquals("data", String(bytes, 36, 4))
        assertEquals(44 + 96 * 2, bytes.size)
    }

    @Test fun clonePatternKeepsOriginal() {
        val p0 = Project(1, "t", 96, Pattern(1).toggleStep(ChannelKind.KICK, 0))
        val p1 = p0.clonePattern()
        assertEquals(2, p1.patternCount); assertEquals(1, p1.current)
        assertTrue(p1.patternAt(0)!!.channel(ChannelKind.KICK).stepOn(0)); assertTrue(p1.pattern.channel(ChannelKind.KICK).stepOn(0))
        val p2 = p1.switchTo(0)
        assertEquals(0, p2.current); assertTrue(p2.others.containsKey(1))
    }

    @Test fun gMajorScale() {
        var p = start("key.g-major")
        p = p.copy(pattern = p.pattern.setNotes(ChannelKind.LEAD, listOf(55, 57, 59, 60, 62, 64, 66, 67).mapIndexed { i, x -> Note(Pattern.nextId(), x, i * 240, 240) }))
        assertTrue(check("key.g-major", 0, p).pass)
        p = p.copy(pattern = p.pattern.setNotes(ChannelKind.LEAD, listOf(55, 57, 59, 60, 62, 64, 65, 67).mapIndexed { i, x -> Note(Pattern.nextId(), x, i * 240, 240) }))
        val v = check("key.g-major", 0, p); assertFalse(v.pass); assertTrue(v.body.contains("F#5"))
    }

    @Test fun harmonyInThirds() {
        var p = start("harm.thirds")
        val lead = p.pattern.channel(ChannelKind.LEAD).notes
        val third = lead.map { n -> val off = (3..4).first { (n.pitch - it) % 12 in com.beatpath.app.lessons.Scales.C_MAJOR }; n.copy(id = Pattern.nextId(), pitch = n.pitch - off) }
        p = p.copy(pattern = p.pattern.setNotes(ChannelKind.LEAD2, third))
        check("harm.thirds", 0, p).let { assertTrue(it.headline + " / " + it.body, it.pass) }
        p = p.copy(pattern = p.pattern.setNotes(ChannelKind.LEAD2, lead.map { it.copy(id = Pattern.nextId(), pitch = it.pitch - 7) }))
        assertFalse(check("harm.thirds", 0, p).pass)
    }

    @Test fun transposeAndInvert() {
        var p = start("key.transpose")
        p = p.copy(pattern = p.pattern.update(ChannelKind.LEAD) { c -> c.copy(notes = c.notes.map { it.copy(pitch = it.pitch + 7) }) })
        assertTrue(check("key.transpose", 0, p).pass)
        var q = start("dev.invert")
        q = q.copy(pattern = q.pattern.update(ChannelKind.LEAD) { c -> c.copy(notes = c.notes + listOf(67 to 0, 64 to 240, 65 to 480, 60 to 960).map { (x, t) -> Note(Pattern.nextId(), x, 1920 + t, 240) }) })
        assertTrue(check("dev.invert", 0, q).pass)
    }

    @Test fun harmonizeMelody() {
        var p = start("song.harmonize")
        val chords = listOf(listOf(60, 64, 67), listOf(55, 59, 62), listOf(57, 60, 64), listOf(55, 59, 62)) // C, G, Am, G
        p = p.copy(pattern = p.pattern.setNotes(ChannelKind.CHORDS, chords.flatMapIndexed { b, c -> c.map { Note(Pattern.nextId(), it, b * 1920, 1920) } }))
        assertTrue(check("song.harmonize", 0, p).pass)
    }

    @Test fun secondaryDominant() {
        var p = start("prog.secondary-dominant")
        val ch = listOf(listOf(60, 64, 67), listOf(57, 61, 64, 67), listOf(62, 65, 69), listOf(55, 59, 62, 65))
        p = p.copy(pattern = p.pattern.setNotes(ChannelKind.CHORDS, ch.flatMapIndexed { b, c -> c.map { Note(Pattern.nextId(), it, b * 1920, 1920) } }))
        assertTrue(check("prog.secondary-dominant", 0, p).pass)
    }
}
