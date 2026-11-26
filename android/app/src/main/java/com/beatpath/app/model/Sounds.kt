package com.beatpath.app.model

/**
 * Drum kits for the Channel Rack pads. Every kit is synthesized (no samples), so each one is a set of
 * recipe numbers: kick pitch sweep and length, snare tone, how bright the hats are, and so on.
 */
enum class DrumKit(
    val label: String, val blurb: String,
    val kickF0: Double, val kickSweep: Double, val kickRate: Double, val kickDecay: Double, val kickLen: Double, val kickDrive: Double,
    val snareTone: Double, val snareToneDecay: Double, val snareNoiseDecay: Double,
    val hatColor: Float, val hatDecay: Double, val openDecay: Double,
    val clapColor: Float, val clapDecay: Double, val crush: Boolean,
) {
    CLASSIC("Classic", "Clean and punchy — a good all-rounder.", 48.0, 110.0, 28.0, 7.5, 0.45, 1.0, 190.0, 22.0, 16.0, 0.70f, 60.0, 9.0, 0.45f, 18.0, false),
    EIGHT08("808", "Long, booming kick and crisp hats — hip-hop and trap.", 42.0, 70.0, 16.0, 2.4, 1.1, 1.4, 230.0, 30.0, 22.0, 0.82f, 78.0, 7.0, 0.50f, 14.0, false),
    HOUSE("House", "Tight, driving kick and bright hats — dance music.", 50.0, 140.0, 36.0, 9.5, 0.35, 1.5, 210.0, 26.0, 20.0, 0.86f, 70.0, 8.0, 0.55f, 20.0, false),
    LOFI("Lo-fi", "Soft, dusty and warm — chill beats.", 55.0, 80.0, 30.0, 9.0, 0.35, 0.8, 170.0, 18.0, 12.0, 0.35f, 45.0, 10.0, 0.30f, 22.0, true),
}

/** Instrument sounds for the Piano roll channels: named recipes for the built-in synth. */
object SoundPresets {
    data class Preset(val name: String, val patch: SynthPatch)
    private val d = Mix().synths

    private val leadSet = listOf(
        Preset("Bell", SynthPatch(wave = Wave.SINE, attackMs = 1f, decayMs = 1200f, sustain = 0f, releaseMs = 700f, cutoffHz = 9000f, resonance = 0f, octave = 1)),
        Preset("Flute", SynthPatch(wave = Wave.SINE, attackMs = 70f, decayMs = 300f, sustain = 0.85f, releaseMs = 220f, cutoffHz = 6000f, resonance = 0f, lfoTarget = LfoTarget.PITCH, lfoSteps = 2, lfoAmount = 0.12f)),
        Preset("Saw lead", SynthPatch(wave = Wave.SAW, attackMs = 3f, decayMs = 300f, sustain = 0.7f, releaseMs = 160f, cutoffHz = 5000f, resonance = 0.25f, detuneCents = 7f)),
        Preset("Pluck", SynthPatch(wave = Wave.SAW, attackMs = 1f, decayMs = 240f, sustain = 0f, releaseMs = 220f, cutoffHz = 1600f, resonance = 0.3f, filterEnvOct = 2.5f, filterEnvDecayMs = 140f)),
        Preset("Chiptune", SynthPatch(wave = Wave.SQUARE, attackMs = 1f, decayMs = 120f, sustain = 0.6f, releaseMs = 60f, cutoffHz = 12000f, resonance = 0f)),
        Preset("Soft keys", SynthPatch(wave = Wave.TRIANGLE, attackMs = 2f, decayMs = 900f, sustain = 0.25f, releaseMs = 350f, cutoffHz = 3200f, resonance = 0.1f)),
    )
    val byKind: Map<ChannelKind, List<Preset>> = mapOf(
        ChannelKind.LEAD to listOf(Preset("Square lead", d.getValue(ChannelKind.LEAD))) + leadSet,
        ChannelKind.LEAD2 to listOf(Preset("Soft triangle", d.getValue(ChannelKind.LEAD2))) + leadSet,
        ChannelKind.CHORDS to listOf(
            Preset("Warm saw", d.getValue(ChannelKind.CHORDS)),
            Preset("Soft pad", SynthPatch(wave = Wave.SAW, attackMs = 380f, decayMs = 800f, sustain = 0.8f, releaseMs = 900f, cutoffHz = 1500f, resonance = 0.1f, detuneCents = 12f)),
            Preset("E-piano", SynthPatch(wave = Wave.TRIANGLE, attackMs = 2f, decayMs = 900f, sustain = 0.25f, releaseMs = 350f, cutoffHz = 3000f, resonance = 0.1f)),
            Preset("Strings", SynthPatch(wave = Wave.SAW, attackMs = 250f, decayMs = 600f, sustain = 0.85f, releaseMs = 600f, cutoffHz = 3200f, resonance = 0.05f, detuneCents = 15f)),
            Preset("Pluck chords", SynthPatch(wave = Wave.SAW, attackMs = 1f, decayMs = 260f, sustain = 0f, releaseMs = 260f, cutoffHz = 1800f, resonance = 0.3f, filterEnvOct = 2.5f, filterEnvDecayMs = 150f)),
            Preset("Organ", SynthPatch(wave = Wave.SQUARE, attackMs = 10f, decayMs = 100f, sustain = 1f, releaseMs = 80f, cutoffHz = 2600f, resonance = 0f)),
        ),
        ChannelKind.BASS to listOf(
            Preset("Saw bass", d.getValue(ChannelKind.BASS)),
            Preset("Sub bass", SynthPatch(wave = Wave.SINE, attackMs = 2f, decayMs = 800f, sustain = 0.9f, releaseMs = 120f, cutoffHz = 3000f, resonance = 0f)),
            Preset("Square bass", SynthPatch(wave = Wave.SQUARE, attackMs = 2f, decayMs = 300f, sustain = 0.5f, releaseMs = 100f, cutoffHz = 700f, resonance = 0.3f)),
            Preset("Pluck bass", SynthPatch(wave = Wave.SAW, attackMs = 1f, decayMs = 180f, sustain = 0.15f, releaseMs = 90f, cutoffHz = 1400f, resonance = 0.35f, filterEnvOct = 2f, filterEnvDecayMs = 120f)),
            Preset("Wobble bass", SynthPatch(wave = Wave.SAW, attackMs = 3f, decayMs = 500f, sustain = 0.8f, releaseMs = 120f, cutoffHz = 600f, resonance = 0.5f, lfoTarget = LfoTarget.CUTOFF, lfoSteps = 2, lfoAmount = 0.6f)),
        ),
    )

    fun forKind(k: ChannelKind): List<Preset> = byKind[k] ?: emptyList()
    fun nameOf(k: ChannelKind, p: SynthPatch): String = forKind(k).firstOrNull { it.patch == p }?.name ?: "Custom"
}
