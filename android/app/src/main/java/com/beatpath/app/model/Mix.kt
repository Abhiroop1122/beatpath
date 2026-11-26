package com.beatpath.app.model

/**
 * Mixer + plugin state, modelled on FL's Mixer (one insert track per channel, a master track,
 * effect slots, sends). Values use FL-like units so lessons can quote real settings.
 */
data class EqSettings(
    val enabled: Boolean = false,
    val lowCutHz: Float = 20f,       // high-pass; 20 = off
    val lowGainDb: Float = 0f,       // low shelf @ lowFreq
    val lowFreq: Float = 120f,
    val midGainDb: Float = 0f,       // peak band
    val midFreq: Float = 1000f,
    val midQ: Float = 1f,
    val highGainDb: Float = 0f,      // high shelf @ highFreq
    val highFreq: Float = 6000f,
    // extra bands so the graph matches Fruity Parametric EQ 2's seven bands
    val lowMidGainDb: Float = 0f,    // peak band 2 (low-mids, "mud")
    val lowMidFreq: Float = 350f,
    val lowMidQ: Float = 1f,
    val presGainDb: Float = 0f,      // peak band 3 (presence)
    val presFreq: Float = 3500f,
    val presQ: Float = 1f,
    val highCutHz: Float = 20000f,   // low-pass; 20000 = off
)

data class CompSettings(
    val enabled: Boolean = false,
    val thresholdDb: Float = 0f,
    val ratio: Float = 1f,
    val attackMs: Float = 10f,
    val releaseMs: Float = 120f,
    val makeupDb: Float = 0f,
    /** Sidechain key: when set, the compressor listens to that channel instead of itself (FL: Fruity Limiter COMP → SIDECHAIN). */
    val sidechain: ChannelKind? = null,
)

data class InsertTrack(
    val volumeDb: Float = 0f,        // fader, FL default 0 dB at unity on inserts
    val pan: Float = 0f,             // -1..1
    val eq: EqSettings = EqSettings(),
    val comp: CompSettings = CompSettings(),
    val reverbSend: Float = 0f,      // 0..1
    val delaySend: Float = 0f,       // 0..1
)

data class ReverbSettings(val size: Float = 0.55f, val damping: Float = 0.5f, val wet: Float = 0.8f, val lowCutHz: Float = 250f)
data class DelaySettings(val steps: Int = 3, val feedback: Float = 0.35f, val damping: Float = 0.4f, val wet: Float = 0.8f)

data class MasterSettings(
    val volumeDb: Float = 0f,
    val limiterOn: Boolean = false,
    val limiterGainDb: Float = 0f,   // drive into the limiter (FL Limiter "GAIN")
    val ceilingDb: Float = 0f,       // FL Limiter "CEIL"
)

enum class Wave { SINE, TRIANGLE, SAW, SQUARE }
enum class LfoTarget { OFF, CUTOFF, PITCH }

/** A tiny subtractive synth — the sound-design playground (3x Osc / FLEX-style controls). */
data class SynthPatch(
    val wave: Wave = Wave.SAW,
    val attackMs: Float = 5f,
    val decayMs: Float = 300f,
    val sustain: Float = 0.6f,
    val releaseMs: Float = 180f,
    val cutoffHz: Float = 6000f,
    val resonance: Float = 0.2f,     // 0..0.95
    val detuneCents: Float = 0f,     // second oscillator detune for width
    val octave: Int = 0,
    /** Filter envelope (FL: Envelope / Instrument → Mod X): how far the cutoff opens at note start, in octaves 0..5. */
    val filterEnvOct: Float = 0f,
    val filterEnvDecayMs: Float = 200f,
    /** LFO (FL: the LFO box on each envelope tab). Tempo-synced rate in steps (16ths). */
    val lfoTarget: LfoTarget = LfoTarget.OFF,
    val lfoSteps: Int = 4,
    val lfoAmount: Float = 0.4f,     // 0..1 (cutoff: up to ±2 octaves; pitch: up to ±1 semitone)
)

data class Mix(
    val inserts: Map<ChannelKind, InsertTrack> = ChannelKind.entries.associateWith { InsertTrack() },
    val master: MasterSettings = MasterSettings(),
    val reverb: ReverbSettings = ReverbSettings(),
    val delay: DelaySettings = DelaySettings(),
    val drumKit: DrumKit = DrumKit.CLASSIC,
    val synths: Map<ChannelKind, SynthPatch> = mapOf(
        ChannelKind.BASS to SynthPatch(wave = Wave.SAW, attackMs = 3f, decayMs = 600f, sustain = 0.8f, releaseMs = 120f, cutoffHz = 900f, resonance = 0.1f, octave = 0),
        ChannelKind.CHORDS to SynthPatch(wave = Wave.SAW, attackMs = 25f, decayMs = 500f, sustain = 0.55f, releaseMs = 300f, cutoffHz = 2200f, resonance = 0.15f, detuneCents = 9f),
        ChannelKind.LEAD to SynthPatch(wave = Wave.SQUARE, attackMs = 3f, decayMs = 260f, sustain = 0.35f, releaseMs = 160f, cutoffHz = 3800f, resonance = 0.25f),
        ChannelKind.LEAD2 to SynthPatch(wave = Wave.TRIANGLE, attackMs = 8f, decayMs = 400f, sustain = 0.5f, releaseMs = 220f, cutoffHz = 2500f),
    ),
) {
    fun insert(k: ChannelKind) = inserts[k] ?: InsertTrack()
    fun synth(k: ChannelKind) = synths[k] ?: SynthPatch()
    fun withInsert(k: ChannelKind, f: (InsertTrack) -> InsertTrack) = copy(inserts = inserts + (k to f(insert(k))))
    fun withSynth(k: ChannelKind, f: (SynthPatch) -> SynthPatch) = copy(synths = synths + (k to f(synth(k))))
}
