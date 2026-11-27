package com.beatpath.app.audio

import com.beatpath.app.audio.AudioEngine.SfxSpec
import com.beatpath.app.audio.AudioEngine.SfxType

/**
 * Lushcore / Y2K sound kit. Everything is synthesised and runs through the SFX bus's chorus +
 * long shimmering reverb: glassy bells, glitter, soft maj7/maj9 pad swells and bubbly digital blips.
 * All in C major (Cmaj9 colours) so nothing clashes with lessons. Levels stay well under the music.
 */
class Sfx(private val e: AudioEngine) {
    private fun hz(midi: Int) = 440.0 * Math.pow(2.0, (midi - 69) / 12.0)
    private fun s(t: SfxType, midi: Int, dur: Float, g: Float, to: Int = midi, delay: Float = 0f) =
        e.sfx(SfxSpec(t, hz(midi), dur, g, freq2 = hz(to), delaySec = delay))

    /** Glassy blip + a whisper of glitter. */
    fun tap() { s(SfxType.BLIP, 91, 0.06f, 0.06f, to = 96); s(SfxType.GLITTER, 103, 0.18f, 0.025f, delay = 0.01f) }
    fun toggleOn() { s(SfxType.BLIP, 84, 0.08f, 0.07f, to = 91); s(SfxType.GLITTER, 100, 0.2f, 0.02f, delay = 0.02f) }
    fun toggleOff() = s(SfxType.BLIP, 91, 0.08f, 0.06f, to = 84)
    fun select() { s(SfxType.GLITTER, 100, 0.3f, 0.05f); s(SfxType.GLITTER, 103, 0.3f, 0.04f, delay = 0.05f) }
    fun swoosh() = e.sfx(SfxSpec(SfxType.SWOOSH, 600.0, 0.32f, 0.08f, freq2 = 3800.0))

    /** Cmaj9 bell arpeggio over a soft pad swell, glitter on top. */
    fun correct() {
        listOf(84, 88, 91, 95, 98).forEachIndexed { i, m -> s(SfxType.CHIME, m, 0.9f, 0.07f, delay = i * 0.055f) }
        listOf(60, 64, 67, 71).forEach { m -> s(SfxType.PAD, m, 1.1f, 0.05f) }
        listOf(103, 107, 108).forEachIndexed { i, m -> s(SfxType.GLITTER, m, 0.25f, 0.025f, delay = 0.25f + i * 0.04f) }
    }

    /** Gentle and dreamy — "not quite", never harsh. */
    fun miss() {
        listOf(57, 60, 64, 67).forEach { m -> s(SfxType.PAD, m, 0.8f, 0.035f) }   // Am7 haze
        s(SfxType.SOFT, 76, 0.22f, 0.07f); s(SfxType.SOFT, 74, 0.32f, 0.06f, delay = 0.16f)
    }

    fun complete() {
        listOf(48, 52, 55, 59, 62).forEach { m -> s(SfxType.PAD, m, 2.4f, 0.05f) }  // Cmaj9 swell
        listOf(72, 76, 79, 83, 84, 88, 91, 95, 96).forEachIndexed { i, m -> s(SfxType.CHIME, m, 1.0f, 0.06f, delay = 0.15f + i * 0.07f) }
        listOf(100, 103, 105, 107, 108).forEachIndexed { i, m -> s(SfxType.GLITTER, m, 0.3f, 0.03f, delay = 0.8f + i * 0.05f) }
    }

    fun xpTick(i: Int) = s(SfxType.GLITTER, 96 + (i % 5) * 2, 0.12f, 0.03f)

    // ---- Lou: cute chibi chirps (non-verbal reactions; he doesn't talk aloud) ----
    fun louHappy() { s(SfxType.BLIP, 79, 0.09f, 0.08f, to = 88); s(SfxType.BLIP, 84, 0.12f, 0.08f, to = 91, delay = 0.1f); s(SfxType.GLITTER, 103, 0.3f, 0.03f, delay = 0.18f) }
    fun louHmm() = s(SfxType.GLIDE, 76, 0.45f, 0.08f, to = 72)
    fun louSurprised() { s(SfxType.BLIP, 76, 0.12f, 0.09f, to = 93); s(SfxType.GLITTER, 105, 0.25f, 0.03f, delay = 0.08f) }
    fun louYawn() = s(SfxType.GLIDE, 81, 0.7f, 0.06f, to = 69)
    /** A small sad "aww": two falling glides. */
    fun louSad() { s(SfxType.GLIDE, 79, 0.34f, 0.08f, to = 74); s(SfxType.GLIDE, 75, 0.62f, 0.075f, to = 66, delay = 0.3f) }
    /** A sparkle before Lou speaks — his little "ding" of personality. */
    fun louChime() { s(SfxType.GLITTER, 100, 0.25f, 0.035f); s(SfxType.GLITTER, 104, 0.25f, 0.03f, delay = 0.04f) }

    /** Fallback babble when no speech engine exists: high, bright "chibi" syllables. */
    fun louSyllable(pitchHz: Double, durSec: Float, vowel: Int) {
        val (f1, f2) = VOWELS[vowel % VOWELS.size]
        e.sfx(SfxSpec(SfxType.SYLLABLE, pitchHz, durSec, 0.3f, f1 = f1, f2 = f2))
    }

    companion object {
        // child-like vowel formants (scaled up) for a small, cute voice
        val VOWELS = listOf(400.0 to 1100.0, 600.0 to 1300.0, 950.0 to 1600.0, 750.0 to 1450.0, 550.0 to 2300.0)
    }
}
