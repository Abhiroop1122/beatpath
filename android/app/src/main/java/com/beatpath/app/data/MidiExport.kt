package com.beatpath.app.data

import com.beatpath.app.model.Project
import java.io.ByteArrayOutputStream

/**
 * Standard MIDI File writer (plan §10).
 * Whole pattern → format 1, one track per non-empty channel, 480 PPQ.
 * Drum channels go to MIDI channel 10 with General MIDI note numbers so they land on kits in Live/Logic.
 * End-of-track is placed on the Pattern boundary so leading/trailing silence survives import.
 */
object MidiExport {
    private const val PPQ = 480

    fun pattern(project: Project): ByteArray {
        val chans = project.pattern.channels.filter { it.notes.isNotEmpty() && !it.muted }
        val out = ByteArrayOutputStream()
        out.write("MThd".toByteArray()); out.u32(6); out.u16(1); out.u16(chans.size + 1); out.u16(PPQ)
        out.track(tempoTrack(project))
        val end = project.pattern.lengthTicks
        chans.forEach { ch ->
            val midiCh = if (ch.isDrum) 9 else 0
            data class Ev(val tick: Int, val on: Boolean, val pitch: Int, val vel: Int)
            val evs = ch.notes.flatMap {
                listOf(Ev(it.startTick, true, it.pitch, it.velocity), Ev((it.startTick + it.durationTicks).coerceAtMost(end), false, it.pitch, 0))
            }.sortedWith(compareBy<Ev> { it.tick }.thenBy { it.on }) // note-offs before note-ons at a shared tick
            val body = ByteArrayOutputStream()
            body.metaText(0x03, ch.name)
            var last = 0
            evs.forEach { e ->
                body.vlq(e.tick - last); last = e.tick
                body.write((if (e.on) 0x90 else 0x80) or midiCh); body.write(e.pitch); body.write(if (e.on) e.vel.coerceIn(1, 127) else 0)
            }
            body.vlq(end - last); body.write(0xFF); body.write(0x2F); body.write(0)
            out.track(body.toByteArray())
        }
        return out.toByteArray()
    }

    private fun tempoTrack(p: Project): ByteArray {
        val b = ByteArrayOutputStream()
        b.metaText(0x03, p.name)
        val mpq = 60_000_000 / p.bpm
        b.vlq(0); b.write(0xFF); b.write(0x51); b.write(3); b.write(mpq shr 16 and 0xFF); b.write(mpq shr 8 and 0xFF); b.write(mpq and 0xFF)
        b.vlq(0); b.write(0xFF); b.write(0x58); b.write(4); b.write(4); b.write(2); b.write(24); b.write(8)
        b.vlq(p.pattern.lengthTicks); b.write(0xFF); b.write(0x2F); b.write(0)
        return b.toByteArray()
    }

    private fun ByteArrayOutputStream.metaText(type: Int, s: String) {
        val bytes = s.toByteArray(); vlq(0); write(0xFF); write(type); vlq(bytes.size); write(bytes)
    }
    private fun ByteArrayOutputStream.track(data: ByteArray) { write("MTrk".toByteArray()); u32(data.size); write(data) }
    private fun ByteArrayOutputStream.u32(v: Int) { write(v ushr 24 and 0xFF); write(v ushr 16 and 0xFF); write(v ushr 8 and 0xFF); write(v and 0xFF) }
    private fun ByteArrayOutputStream.u16(v: Int) { write(v ushr 8 and 0xFF); write(v and 0xFF) }
    private fun ByteArrayOutputStream.vlq(value: Int) {
        var v = value.coerceAtLeast(0)
        var buf = v and 0x7F
        while (true) { v = v ushr 7; if (v == 0) break; buf = buf shl 8; buf = buf or ((v and 0x7F) or 0x80) }
        while (true) { write(buf and 0xFF); if (buf and 0x80 != 0) buf = buf ushr 8 else break }
    }
}
