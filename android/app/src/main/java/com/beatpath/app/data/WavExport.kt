package com.beatpath.app.data

import com.beatpath.app.audio.AudioEngine
import com.beatpath.app.model.Project
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** 16-bit PCM stereo WAV bounce of a project (plan §10: "share a WAV"). */
object WavExport {
    fun render(project: Project, loops: Int = 2): ByteArray {
        val engine = AudioEngine()          // fresh, never started: renders offline
        val pcm = engine.renderOffline(project.songPattern(), project.bpm, project.mix, loops)
        return encode(pcm, engine.sampleRate)
    }

    fun encode(interleavedStereo: FloatArray, sampleRate: Int): ByteArray {
        val dataBytes = interleavedStereo.size * 2
        val b = ByteBuffer.allocate(44 + dataBytes).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray()); b.putInt(36 + dataBytes); b.put("WAVE".toByteArray())
        b.put("fmt ".toByteArray()); b.putInt(16); b.putShort(1); b.putShort(2); b.putInt(sampleRate)
        b.putInt(sampleRate * 4); b.putShort(4); b.putShort(16)
        b.put("data".toByteArray()); b.putInt(dataBytes)
        for (x in interleavedStereo) b.putShort((x.coerceIn(-1f, 1f) * 32767f).toInt().toShort())
        return ByteArrayOutputStream().also { it.write(b.array()) }.toByteArray()
    }
}
