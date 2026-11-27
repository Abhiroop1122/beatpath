package com.beatpath.app.audio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTimestamp
import android.media.AudioTrack
import android.os.Process
import com.beatpath.app.model.ChannelKind
import com.beatpath.app.model.DrumKit
import com.beatpath.app.model.Mix
import com.beatpath.app.model.Pattern
import com.beatpath.app.model.Pitch
import com.beatpath.app.model.TICKS_PER_BEAT
import com.beatpath.app.model.TICKS_PER_STEP
import com.beatpath.app.model.Wave
import java.util.concurrent.ArrayBlockingQueue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Beatpath audio engine.
 *
 * Signal flow (mirrors FL's Mixer):
 *   channel voices → channel buffer → insert (low cut, low shelf, peak, high shelf → compressor → fader → pan)
 *   → master (+ reverb and delay returns) → master fader → limiter (optional) → peak meter → output.
 * Ear-training notes, UI sounds, Lou's chirps and the metronome bypass the mix so lesson audio is never coloured.
 *
 * One render thread writes interleaved stereo float PCM to a low-latency AudioTrack. Voices, filters and
 * buffers are preallocated; the UI talks to it only through a bounded command queue and volatile snapshots.
 * The playhead comes from AudioTrack.getTimestamp (frames actually presented). Plan §9 swaps in Oboe later.
 */
class AudioEngine {

    enum class Timbre { KEYS, PLUCK, PURE, BASS, PAD }
    enum class SfxType { BUBBLE, CHIME, SOFT, SWOOSH, SYLLABLE, GLIDE, SPARKLE, PAD, GLITTER, BLIP }

    data class SfxSpec(
        val type: SfxType, val freq: Double, val durSec: Float, val gain: Float,
        val freq2: Double = 0.0, val f1: Double = 0.0, val f2: Double = 0.0, val delaySec: Float = 0f,
    )

    private sealed interface Cmd {
        data class NoteOn(val midi: Int, val velocity: Float, val durationSec: Float, val timbre: Timbre, val channel: ChannelKind?) : Cmd
        data class Drum(val kind: ChannelKind, val velocity: Float) : Cmd
        data class Play(val pattern: Pattern, val bpm: Int) : Cmd
        data class Update(val pattern: Pattern, val bpm: Int) : Cmd
        data object Stop : Cmd
        data class Metronome(val on: Boolean) : Cmd
        data class Effect(val spec: SfxSpec) : Cmd
    }

    val sampleRate: Int = AudioTrack.getNativeOutputSampleRate(AudioTrack.MODE_STREAM).coerceIn(22050, 96000)
    private val frames = 192
    private val queue = ArrayBlockingQueue<Cmd>(512)
    @Volatile private var running = false
    private var thread: Thread? = null
    private var track: AudioTrack? = null

    @Volatile var sfxEnabled: Boolean = true
    /** Set while an ear-training prompt plays: no SFX may mask it (plan §4.3). */
    @Volatile var quietSfx: Boolean = false
    /** Goes up each time playback is stopped from outside (a call, another app, headphones unplugged), so screens can show Stop. */
    var interruptions by androidx.compose.runtime.mutableIntStateOf(0)
    fun interrupt() { stop(); interruptions++ }
    /** Mixer + plugin settings; swap the whole object to change anything (render thread picks it up). */
    @Volatile var mix: Mix = Mix()

    // ---------------- meters (render thread writes, UI reads) ----------------
    private val nCh = ChannelKind.entries.size
    /** Post-fader peak per channel insert, linear. */
    val channelPeak = FloatArray(nCh)
    /** Compressor gain reduction per insert, dB (positive). */
    val channelGr = FloatArray(nCh)
    @Volatile var masterPeak: Float = 0f; private set
    @Volatile var limiterGr: Float = 0f; private set
    /** Number of samples that hit/exceeded 0 dBFS on the master since [resetClip]. */
    @Volatile var clipCount: Int = 0; private set
    /** Highest master peak since [resetClip], linear (can exceed 1.0 when the mix clips). */
    @Volatile var maxPeakSinceReset: Float = 0f; private set
    fun resetClip() { clipCount = 0; maxPeakSinceReset = 0f; resetLoudness = true }
    @Volatile private var resetLoudness = false
    /** Integrated loudness (LUFS) of the master since the last reset; momentary over the last 400 ms. */
    val loudnessLufs: Float get() = loudness.integrated
    val momentaryLufs: Float get() = loudness.momentary

    // ---------------- transport ----------------
    @Volatile var isPlaying: Boolean = false; private set
    @Volatile private var playStartFrame: Long = 0
    @Volatile private var framesPerTick: Double = 1.0
    @Volatile private var patternTicks: Int = 1
    private var framesWritten: Long = 0
    private var pattern: Pattern? = null
    private var metronomeOn = false
    private var loopIndex = 0L

    // ---------------- voices ----------------
    private val maxVoices = 64
    private val vActive = BooleanArray(maxVoices)
    private val vKind = IntArray(maxVoices)   // 0 tone, 1 kick, 2 clap, 3 hat, 4 click, 5 snare, 6 open hat, 7 synth, 10+ sfx
    private val vCh = IntArray(maxVoices)     // channel ordinal, -1 = direct (ear/preview), -2 = sfx, -3 = click
    private val vFreq = DoubleArray(maxVoices)
    private val vFreq2 = DoubleArray(maxVoices)
    private val vPhase = DoubleArray(maxVoices)
    private val vPhase2 = DoubleArray(maxVoices)
    private val vAge = IntArray(maxVoices)
    private val vLen = IntArray(maxVoices)
    private val vVel = FloatArray(maxVoices)
    private val vTimbre = IntArray(maxVoices)
    private val vStartAt = LongArray(maxVoices)
    private val vFilt = FloatArray(maxVoices)
    private val vSvf = Array(maxVoices) { Svf() }
    private val bq = Array(maxVoices) { DoubleArray(16) }
    private var rng = 0x2545F491

    // ---------------- buses ----------------
    private val chBuf = Array(nCh) { DoubleArray(frames) }
    private val directBuf = DoubleArray(frames)
    /** UI + Lou sounds: their own lush bus (chorus + big dreamy reverb) so they shimmer without touching the music. */
    private val sfxBuf = DoubleArray(frames)
    private val sfxChorus = Chorus(sampleRate)
    private val sfxVerb = Reverb(sampleRate).apply { set(0.92f, 0.25f) }
    private val sfxTmp = DoubleArray(2)
    private val sfxTmp2 = DoubleArray(2)
    private val out = FloatArray(frames * 2)
    private val inserts = Array(nCh) { InsertDsp(sampleRate) }
    private val reverb = Reverb(sampleRate)
    private val reverbLowCut = Biquad()
    private val delay = Delay(sampleRate)
    private val limiter = Limiter(sampleRate)
    private val loudness = LoudnessMeter(sampleRate)
    private var appliedMix: Mix? = null
    private val tmp2 = DoubleArray(2)
    private val tmp2b = DoubleArray(2)

    private class InsertDsp(sr: Int) {
        val hp = Biquad(); val low = Biquad(); val lowMid = Biquad(); val mid = Biquad(); val pres = Biquad(); val high = Biquad(); val lp = Biquad()
        val comp = Compressor(sr)
        var sc = -1
        var compOn = false
        var gain = 1.0; var panL = 0.7071; var panR = 0.7071
        var rvb = 0.0; var dly = 0.0
    }

    fun start() {
        if (running) return
        val minBuf = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_FLOAT)
        val t = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(sampleRate).setEncoding(AudioFormat.ENCODING_PCM_FLOAT).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
            .setBufferSizeInBytes(maxOf(minBuf, frames * 2 * 4 * 4))
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track = t
        running = true
        t.play()
        thread = Thread({ renderLoop(t) }, "beatpath-audio").also { it.start() }
    }

    fun release() {
        running = false
        isPlaying = false
        thread?.join(500)
        queue.clear()
        track?.run { stop(); release() }
        track = null
    }

    // ---------------- public API ----------------
    /** Preview a note. With [channel] it goes through that channel's synth + insert (you hear the mix); without, it is a clean reference tone. */
    fun playNote(midi: Int, durationSec: Float = 0.6f, velocity: Float = 0.8f, timbre: Timbre = Timbre.KEYS, channel: ChannelKind? = null) {
        queue.offer(Cmd.NoteOn(midi, velocity, durationSec, timbre, channel))
    }
    fun playDrum(kind: ChannelKind, velocity: Float = 0.9f) { queue.offer(Cmd.Drum(kind, velocity)) }
    fun play(pattern: Pattern, bpm: Int) { queue.offer(Cmd.Play(pattern, bpm)) }
    fun update(pattern: Pattern, bpm: Int) { queue.offer(Cmd.Update(pattern, bpm)) }
    fun stop() { queue.offer(Cmd.Stop) }
    fun setMetronome(on: Boolean) { queue.offer(Cmd.Metronome(on)) }
    fun sfx(spec: SfxSpec) { if (sfxEnabled && !quietSfx) queue.offer(Cmd.Effect(spec)) }

    private val ts = AudioTimestamp()

    fun playheadTick(): Int {
        if (!isPlaying) return -1
        val t = track ?: return -1
        val presented: Long = if (t.getTimestamp(ts)) ts.framePosition + ((System.nanoTime() - ts.nanoTime) * sampleRate / 1_000_000_000L)
        else t.playbackHeadPosition.toLong() and 0xFFFFFFFFL
        val since = presented - playStartFrame
        if (since < 0) return -1
        return ((since / framesPerTick).toLong() % patternTicks).toInt()
    }

    // ---------------- render thread ----------------
    private fun renderLoop(t: AudioTrack) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        while (running) {
            drainCommands()
            applyMixIfChanged()
            renderBlock()
            t.write(out, 0, frames * 2, AudioTrack.WRITE_BLOCKING)
            framesWritten += frames
        }
    }

    private fun drainCommands() {
        while (true) {
            when (val c = queue.poll() ?: return) {
                is Cmd.NoteOn -> {
                    val len = (c.durationSec * sampleRate).toInt()
                    if (c.channel != null && !c.channel.isDrum) allocSynth(c.channel, c.midi, c.velocity, len, framesWritten)
                    else allocTone(c.midi, c.velocity, len, c.timbre.ordinal, framesWritten, -1)
                }
                is Cmd.Drum -> allocDrum(c.kind, c.velocity, framesWritten)
                is Cmd.Play -> { pattern = c.pattern; setTempo(c.bpm, c.pattern); buildEvents(c.pattern); playStartFrame = framesWritten; evIdx = 0; loopIndex = 0; isPlaying = true }
                is Cmd.Update -> {
                    pattern = c.pattern
                    if (isPlaying) {
                        val pos = (framesWritten - playStartFrame) / framesPerTick
                        setTempo(c.bpm, c.pattern)
                        playStartFrame = framesWritten - (pos * framesPerTick).toLong()
                        val abs = ((framesWritten - playStartFrame) / framesPerTick).toLong()
                        loopIndex = abs / patternTicks
                        buildEvents(c.pattern)
                        seekEvents(abs % patternTicks + 1)
                    } else { setTempo(c.bpm, c.pattern); buildEvents(c.pattern) }
                }
                Cmd.Stop -> {
                    isPlaying = false
                    for (i in 0 until maxVoices) if (vActive[i] && vCh[i] >= 0) {
                        if (vStartAt[i] > framesWritten) vActive[i] = false
                        else if ((vKind[i] == 0 || vKind[i] == 7) && vAge[i] < vLen[i]) vLen[i] = vAge[i]
                    }
                }
                is Cmd.Metronome -> metronomeOn = c.on
                is Cmd.Effect -> allocSfx(c.spec, framesWritten + (c.spec.delaySec * sampleRate).toLong())
            }
        }
    }

    private fun setTempo(bpm: Int, p: Pattern) {
        framesPerTick = sampleRate * 60.0 / bpm / TICKS_PER_BEAT
        patternTicks = p.lengthTicks
        delay.delaySamples = (mix.delay.steps * TICKS_PER_STEP * framesPerTick).toInt()
    }

    private fun applyMixIfChanged() {
        val m = mix
        if (m === appliedMix) return
        appliedMix = m
        val sr = sampleRate
        for (k in ChannelKind.entries) {
            val d = inserts[k.ordinal]; val ins = m.insert(k); val eq = ins.eq
            if (eq.enabled) {
                if (eq.lowCutHz > 21f) d.hp.highPass(sr, eq.lowCutHz.toDouble()) else d.hp.bypass = true
                if (eq.lowGainDb != 0f) d.low.lowShelf(sr, eq.lowFreq.toDouble(), eq.lowGainDb.toDouble()) else d.low.bypass = true
                if (eq.midGainDb != 0f) d.mid.peak(sr, eq.midFreq.toDouble(), eq.midQ.toDouble(), eq.midGainDb.toDouble()) else d.mid.bypass = true
                if (eq.highGainDb != 0f) d.high.highShelf(sr, eq.highFreq.toDouble(), eq.highGainDb.toDouble()) else d.high.bypass = true
                if (eq.lowMidGainDb != 0f) d.lowMid.peak(sr, eq.lowMidFreq.toDouble(), eq.lowMidQ.toDouble(), eq.lowMidGainDb.toDouble()) else d.lowMid.bypass = true
                if (eq.presGainDb != 0f) d.pres.peak(sr, eq.presFreq.toDouble(), eq.presQ.toDouble(), eq.presGainDb.toDouble()) else d.pres.bypass = true
                if (eq.highCutHz < 19000f) d.lp.lowPass(sr, eq.highCutHz.toDouble()) else d.lp.bypass = true
            } else { d.hp.bypass = true; d.low.bypass = true; d.mid.bypass = true; d.high.bypass = true; d.lowMid.bypass = true; d.pres.bypass = true; d.lp.bypass = true }
            d.sc = ins.comp.sidechain?.takeIf { it != k }?.ordinal ?: -1
            d.compOn = ins.comp.enabled
            d.comp.thresholdDb = ins.comp.thresholdDb; d.comp.ratio = ins.comp.ratio; d.comp.makeupDb = ins.comp.makeupDb
            d.comp.setTimes(ins.comp.attackMs, ins.comp.releaseMs)
            d.gain = dbToGain(ins.volumeDb).toDouble()
            val a = (ins.pan.coerceIn(-1f, 1f) + 1) * PI / 4      // constant-power pan law
            d.panL = cos(a); d.panR = sin(a)
            d.rvb = ins.reverbSend.toDouble(); d.dly = ins.delaySend.toDouble()
        }
        reverb.set(m.reverb.size, m.reverb.damping)
        reverbLowCut.highPass(sr, m.reverb.lowCutHz.toDouble())
        delay.feedback = m.delay.feedback.toDouble().coerceIn(0.0, 0.92); delay.damping = m.delay.damping.toDouble()
        delay.delaySamples = (m.delay.steps * TICKS_PER_STEP * framesPerTick).toInt()
        limiter.ceiling = dbToGain(m.master.ceilingDb).toDouble().coerceAtMost(1.0)
        limiter.drive = dbToGain(m.master.limiterGainDb).toDouble()
    }

    /** Flattened, tick-sorted events for the current pattern (rebuilt on Play/Update, never in the hot loop). */
    private class Ev(val tick: Int, val ch: Int, val pitch: Int, val vel: Float, val durTicks: Int, val drum: Boolean)
    private var events: Array<Ev> = emptyArray()
    private var evIdx = 0
    private var swingFrames = 0.0

    private fun buildEvents(p: Pattern) {
        val list = ArrayList<Ev>()
        for (ch in p.channels) {
            if (ch.muted) continue
            for (n in ch.notes) list.add(Ev(n.startTick, ch.kind.ordinal, n.pitch, n.velocity / 127f * ch.volume / 0.8f, n.durationTicks, ch.isDrum))
        }
        // metronome as events on every beat (ch = -3)
        var t = 0
        while (t < p.lengthTicks) { list.add(Ev(t, -3, if (t % (TICKS_PER_BEAT * 4) == 0) 1 else 0, 0.3f, 0, true)); t += TICKS_PER_BEAT }
        list.sortBy { it.tick }
        events = list.toTypedArray()
        swingFrames = p.swing * TICKS_PER_STEP * 0.66 * framesPerTick
    }

    private fun seekEvents(tickInPattern: Long) {
        evIdx = events.indexOfFirst { it.tick >= tickInPattern }.let { if (it < 0) events.size else it }
    }

    /** Schedule every event whose start frame falls inside the next block, sample-accurately (swing delays off-beat 16ths). */
    private fun scheduleBlock(blockStart: Long, blockEnd: Long) {
        if (!isPlaying || events.isEmpty()) return
        var guard = 0
        while (guard++ < 4096) {
            if (evIdx >= events.size) { evIdx = 0; loopIndex++ }
            val e = events[evIdx]
            val swung = if (e.ch >= 0 && e.tick % (2 * TICKS_PER_STEP) == TICKS_PER_STEP) swingFrames else 0.0
            val frame = playStartFrame + ((loopIndex * patternTicks + e.tick) * framesPerTick + swung).toLong()
            if (frame >= blockEnd) return
            val at = maxOf(frame, blockStart)
            when {
                e.ch == -3 -> if (metronomeOn) allocClick(e.pitch == 1, at)
                e.drum -> allocDrum(ChannelKind.entries[e.ch], e.vel, at)
                else -> allocSynth(ChannelKind.entries[e.ch], e.pitch, e.vel, (e.durTicks * framesPerTick).toInt(), at)
            }
            evIdx++
        }
    }

    private fun freeVoice(): Int {
        for (i in 0 until maxVoices) if (!vActive[i]) return i
        var oldest = 0
        for (i in 1 until maxVoices) if (vAge[i] > vAge[oldest]) oldest = i
        return oldest
    }

    private fun base(v: Int, kind: Int, at: Long, ch: Int) {
        vActive[v] = true; vKind[v] = kind; vPhase[v] = 0.0; vPhase2[v] = 0.0; vAge[v] = 0; vStartAt[v] = at; vFilt[v] = 0f; vCh[v] = ch
        java.util.Arrays.fill(bq[v], 0.0); vSvf[v].reset()
    }

    private fun allocTone(midi: Int, vel: Float, lenFrames: Int, timbre: Int, at: Long, ch: Int) {
        val v = freeVoice(); base(v, 0, at, ch)
        vFreq[v] = Pitch.hz(midi); vLen[v] = maxOf(lenFrames, sampleRate / 20); vVel[v] = vel; vTimbre[v] = timbre
    }

    private fun allocSynth(kind: ChannelKind, midi: Int, vel: Float, lenFrames: Int, at: Long) {
        val v = freeVoice(); base(v, 7, at, kind.ordinal)
        val patch = mix.synth(kind)
        vFreq[v] = Pitch.hz(midi + 12 * patch.octave)
        vLen[v] = maxOf(lenFrames, sampleRate / 30)
        vVel[v] = vel * if (kind == ChannelKind.BASS) 0.85f else 0.55f
        vTimbre[v] = kind.ordinal
    }

    private fun allocDrum(kind: ChannelKind, vel: Float, at: Long) {
        val v = freeVoice()
        val kit = (appliedMix ?: mix).drumKit
        val (k, len) = when (kind) {
            ChannelKind.KICK -> 1 to kit.kickLen; ChannelKind.CLAP -> 2 to 0.25; ChannelKind.SNARE -> 5 to 0.22
            ChannelKind.OPEN_HAT -> 6 to 0.35; else -> 3 to 0.08
        }
        base(v, k, at, kind.ordinal); vLen[v] = (len * sampleRate).toInt(); vVel[v] = vel; vTimbre[v] = kit.ordinal
    }

    private fun allocClick(accent: Boolean, at: Long) {
        val v = freeVoice(); base(v, 4, at, -3)
        vFreq[v] = if (accent) 1760.0 else 1320.0; vLen[v] = (0.03 * sampleRate).toInt(); vVel[v] = 0.3f
    }

    private fun allocSfx(s: SfxSpec, at: Long) {
        val v = freeVoice(); base(v, 10 + s.type.ordinal, at, -2)
        vFreq[v] = s.freq; vFreq2[v] = if (s.freq2 > 0) s.freq2 else s.freq
        vLen[v] = (s.durSec * sampleRate).toInt().coerceAtLeast(64); vVel[v] = s.gain
        if (s.type == SfxType.SYLLABLE) { setBandpass(v, 0, s.f1, 5.0); setBandpass(v, 8, s.f2, 7.0) }
        if (s.type == SfxType.SWOOSH) setBandpass(v, 0, s.freq, 1.2)
    }

    /** RBJ band-pass (0 dB peak) in bq[v][o..o+7]: b0, b2, a1, a2 | x1, x2, y1, y2. Filters live at o = 0 and o = 8. */
    private fun setBandpass(v: Int, o: Int, f: Double, q: Double) {
        val w0 = 2 * PI * f.coerceIn(40.0, sampleRate * 0.45) / sampleRate
        val alpha = sin(w0) / (2 * q); val a0 = 1 + alpha
        bq[v][o] = alpha / a0; bq[v][o + 1] = -alpha / a0; bq[v][o + 2] = -2 * cos(w0) / a0; bq[v][o + 3] = (1 - alpha) / a0
    }

    private fun bandpass(v: Int, o: Int, x: Double): Double {
        val c = bq[v]
        val y = c[o] * x + c[o + 1] * c[o + 5] - c[o + 2] * c[o + 6] - c[o + 3] * c[o + 7]
        c[o + 5] = c[o + 4]; c[o + 4] = x
        c[o + 7] = c[o + 6]; c[o + 6] = y
        return y
    }

    /** Lo-fi kit: a gentle bit-crush for that dusty sampler feel. */
    private fun crush(x: Double, kit: DrumKit): Double = if (kit.crush) kotlin.math.round(x * 20.0) / 20.0 else x

    private fun noise(): Float {
        rng = rng xor (rng shl 13); rng = rng xor (rng ushr 17); rng = rng xor (rng shl 5)
        return (rng and 0xFFFF) / 32768f - 1f
    }

    private fun renderBlock() {
        val blockStart = framesWritten
        scheduleBlock(blockStart, blockStart + frames)
        for (b in chBuf) java.util.Arrays.fill(b, 0.0)
        java.util.Arrays.fill(directBuf, 0.0)
        java.util.Arrays.fill(sfxBuf, 0.0)
        val sr = sampleRate.toDouble()
        val twoPi = 2 * PI
        val sfxOn = sfxEnabled && !quietSfx
        val m = appliedMix ?: mix

        // ---- voices into their buses ----
        for (v in 0 until maxVoices) {
            if (!vActive[v]) continue
            val startOffset = (vStartAt[v] - blockStart).toInt()
            if (startOffset >= frames) continue
            val target = if (vCh[v] >= 0) chBuf[vCh[v]] else if (vCh[v] == -2) sfxBuf else directBuf
            val gainBus = if (vCh[v] == -2 && !sfxOn) 0.0 else 1.0
            val patch = if (vKind[v] == 7) m.synth(ChannelKind.entries[vTimbre[v]]) else null
            var i = maxOf(0, startOffset)
            while (i < frames) {
                val age = vAge[v]
                if (age >= vLen[v] + releaseFrames(v, patch)) { vActive[v] = false; break }
                val t = age / sr
                val s: Double = when (vKind[v]) {
                    0 -> toneSample(v, age, sr, twoPi)
                    7 -> synthSample(v, age, sr, patch!!, blockStart + i)
                    1 -> {
                        val kit = DrumKit.entries[vTimbre[v]]
                        val f = kit.kickF0 + kit.kickSweep * exp(-t * kit.kickRate)
                        vPhase[v] += twoPi * f / sr
                        val click = if (age < 90) noise() * 0.25 * (1 - age / 90.0) else 0.0
                        var x = sin(vPhase[v]) * exp(-t * kit.kickDecay) + click
                        if (kit.kickDrive > 1.0) x = kotlin.math.tanh(x * kit.kickDrive) / kotlin.math.tanh(kit.kickDrive)
                        crush(x, kit) * vVel[v] * 0.9
                    }
                    2 -> {
                        val kit = DrumKit.entries[vTimbre[v]]
                        val burst = when { t < 0.010 -> 1.0; t < 0.013 -> 0.2; t < 0.022 -> 0.9; t < 0.025 -> 0.2; else -> exp(-(t - 0.025) * kit.clapDecay) }
                        val n = noise(); vFilt[v] += kit.clapColor * (n - vFilt[v])
                        crush((n - vFilt[v]) * burst * 0.6, kit) * vVel[v]
                    }
                    3, 6 -> {
                        val kit = DrumKit.entries[vTimbre[v]]
                        val n = noise(); vFilt[v] += kit.hatColor * (n - vFilt[v])
                        crush((n - vFilt[v]) * exp(-t * if (vKind[v] == 3) kit.hatDecay else kit.openDecay) * 0.35, kit) * vVel[v]
                    }
                    5 -> {
                        val kit = DrumKit.entries[vTimbre[v]]
                        vPhase[v] += twoPi * (kit.snareTone + 60 * exp(-t * 40)) / sr
                        val n = noise(); vFilt[v] += 0.5f * (n - vFilt[v])
                        crush(sin(vPhase[v]) * exp(-t * kit.snareToneDecay) * 0.5 + (n - vFilt[v] * 0.3) * exp(-t * kit.snareNoiseDecay) * 0.45, kit) * vVel[v]
                    }
                    4 -> { vPhase[v] += twoPi * vFreq[v] / sr; sin(vPhase[v]) * exp(-t * 120) * vVel[v] }
                    else -> sfxSample(v, age, t, sr, twoPi)
                }
                target[i] += s * gainBus
                vAge[v] = age + 1
                i++
            }
        }

        // ---- mixer ----
        if (resetLoudness) { loudness.reset(); resetLoudness = false }
        var masterMax = 0f
        val mastGain = dbToGain(m.master.volumeDb).toDouble()
        for (k in 0 until nCh) { channelGr[k] = 0f }
        for (i in 0 until frames) {
            var l = 0.0; var r = 0.0; var rvbIn = 0.0; var dlyIn = 0.0
            for (k in 0 until nCh) {
                val d = inserts[k]
                var x = chBuf[k][i]
                if (x == 0.0 && d.hp.bypass && !d.compOn) continue
                x = d.lp.process(d.high.process(d.pres.process(d.mid.process(d.lowMid.process(d.low.process(d.hp.process(x)))))))
                if (d.compOn) x = d.comp.process(x, if (d.sc >= 0) chBuf[d.sc][i] else x)
                x *= d.gain
                val ax = abs(x).toFloat()
                if (ax > channelPeak[k]) channelPeak[k] = ax
                l += x * d.panL * 1.4142; r += x * d.panR * 1.4142
                rvbIn += x * d.rvb; dlyIn += x * d.dly
            }
            reverb.process(reverbLowCut.process(rvbIn), tmp2)
            delay.process(dlyIn, tmp2b)
            l += tmp2[0] * m.reverb.wet + tmp2b[0] * m.delay.wet
            r += tmp2[1] * m.reverb.wet + tmp2b[1] * m.delay.wet
            l *= mastGain; r *= mastGain
            if (m.master.limiterOn) { limiter.process(l, r, tmp2); l = tmp2[0]; r = tmp2[1] }
            val pk = maxOf(abs(l), abs(r)).toFloat()
            if (pk > masterMax) masterMax = pk
            if (pk >= 1f) clipCount++
            loudness.process(l, r)
            // digital clipping is real here: the mix hard-clips at 0 dBFS, exactly what the meter warns about
            l = l.coerceIn(-1.0, 1.0); r = r.coerceIn(-1.0, 1.0)
            // clean buses (ear prompts, click) sit on top, softly limited
            val direct = tanh(directBuf[i] * 0.9)
            // lush SFX bus: dry + chorus + long shimmering reverb
            val sx = sfxBuf[i]
            sfxChorus.process(sx, sfxTmp)
            sfxVerb.process(sx + (sfxTmp[0] + sfxTmp[1]) * 0.3, sfxTmp2)
            val sfxL = sx * 0.75 + sfxTmp[0] * 0.45 + sfxTmp2[0] * 0.9
            val sfxR = sx * 0.75 + sfxTmp[1] * 0.45 + sfxTmp2[1] * 0.9
            out[2 * i] = ((l * 0.92) + direct + tanh(sfxL)).coerceIn(-1.0, 1.0).toFloat()
            out[2 * i + 1] = ((r * 0.92) + direct + tanh(sfxR)).coerceIn(-1.0, 1.0).toFloat()
        }
        for (k in 0 until nCh) if (inserts[k].compOn) { channelGr[k] = inserts[k].comp.meterGr; inserts[k].comp.meterGr = 0f }
        limiterGr = limiter.meterGr; limiter.meterGr = 0f
        masterPeak = masterMax
        if (masterMax > maxPeakSinceReset) maxPeakSinceReset = masterMax
    }

    /**
     * Offline bounce for WAV export: renders [loops] passes of the pattern plus a tail, through the same
     * synths, inserts, sends and master as playback. Call on a fresh, unstarted engine, off the main thread.
     */
    fun renderOffline(p: Pattern, bpm: Int, m: Mix, loops: Int, tailSec: Float = 2f): FloatArray {
        mix = m; applyMixIfChanged()
        pattern = p; setTempo(bpm, p); buildEvents(p)
        metronomeOn = false; sfxEnabled = false
        framesWritten = 0; playStartFrame = 0; evIdx = 0; loopIndex = 0; isPlaying = true
        val musicFrames = (p.lengthTicks * loops * framesPerTick).toLong()
        val total = musicFrames + (tailSec * sampleRate).toLong()
        val blocks = ((total + frames - 1) / frames).toInt()
        val result = FloatArray(blocks * frames * 2)
        for (b in 0 until blocks) {
            if (framesWritten >= musicFrames) isPlaying = false
            renderBlock()
            System.arraycopy(out, 0, result, b * frames * 2, frames * 2)
            framesWritten += frames
        }
        return result
    }

    /** UI calls this each frame to read & decay channel meters. */
    fun takeChannelPeak(k: ChannelKind): Float { val v = channelPeak[k.ordinal]; channelPeak[k.ordinal] = v * 0.85f; return v }

    private fun releaseFrames(v: Int, patch: com.beatpath.app.model.SynthPatch?): Int = when (vKind[v]) {
        0 -> ((if (vTimbre[v] == Timbre.PAD.ordinal) 0.45 else 0.18) * sampleRate).toInt()
        7 -> ((patch?.releaseMs ?: 150f) * 0.001 * sampleRate).toInt()
        else -> 0
    }

    private fun synthSample(v: Int, age: Int, sr: Double, p: com.beatpath.app.model.SynthPatch, absFrame: Long): Double {
        val a = maxOf(1.0, p.attackMs * 0.001 * sr)
        val d = maxOf(1.0, p.decayMs * 0.001 * sr)
        val rel = maxOf(1.0, p.releaseMs * 0.001 * sr)
        val len = vLen[v]
        fun adsr(x: Int): Double = when {
            x < a -> x / a
            x < a + d -> 1.0 - (1.0 - p.sustain) * ((x - a) / d)
            else -> p.sustain.toDouble()
        }
        val env = if (age < len) adsr(age) else adsr(len) * (1.0 - (age - len) / rel).coerceAtLeast(0.0)
        // tempo-synced LFO (phase from the song position so every voice wobbles together, in time)
        val lfo = if (p.lfoTarget == com.beatpath.app.model.LfoTarget.OFF) 0.0 else {
            val period = maxOf(1.0, p.lfoSteps * TICKS_PER_STEP * framesPerTick)
            sin(2 * PI * (((absFrame - playStartFrame).coerceAtLeast(0L)) / period)) * p.lfoAmount
        }
        val f = vFreq[v] * if (p.lfoTarget == com.beatpath.app.model.LfoTarget.PITCH) Math.pow(2.0, lfo / 12.0) else 1.0
        val dt = f / sr
        vPhase[v] = (vPhase[v] + dt) % 1.0
        val dt2 = f * Math.pow(2.0, p.detuneCents / 1200.0) / sr
        vPhase2[v] = (vPhase2[v] + dt2) % 1.0
        fun osc(ph: Double, inc: Double): Double = when (p.wave) {
            Wave.SINE -> sin(2 * PI * ph)
            Wave.TRIANGLE -> 4 * abs(ph - 0.5) - 1
            Wave.SAW -> (2 * ph - 1) - polyBlep(ph, inc)
            Wave.SQUARE -> (if (ph < 0.5) 1.0 else -1.0) + polyBlep(ph, inc) - polyBlep((ph + 0.5) % 1.0, inc)
        }
        var s = osc(vPhase[v], dt)
        if (p.detuneCents != 0f) s = (s + osc(vPhase2[v], dt2)) * 0.6
        // filter opens a little at note start for a livelier attack; coefficients refreshed every 32 samples
        if (age and 31 == 0) {
            val envOct = p.filterEnvOct * exp(-age / (maxOf(5f, p.filterEnvDecayMs) * 0.001 * sr))
            val lfoOct = if (p.lfoTarget == com.beatpath.app.model.LfoTarget.CUTOFF) lfo * 2.0 else 0.0
            vSvf[v].set(sampleRate, p.cutoffHz * (1.0 + 0.6 * exp(-age / (0.08 * sr))) * Math.pow(2.0, envOct + lfoOct), p.resonance.toDouble())
        }
        s = vSvf[v].process(s)
        return s * env * vVel[v] * 0.5
    }

    private fun polyBlep(t: Double, dt: Double): Double = when {
        t < dt -> { val x = t / dt; x + x - x * x - 1 }
        t > 1 - dt -> { val x = (t - 1) / dt; x * x + x + x + 1 }
        else -> 0.0
    }

    private fun toneSample(v: Int, age: Int, sr: Double, twoPi: Double): Double {
        val len = vLen[v]
        val timbre = vTimbre[v]
        val attack = (if (timbre == Timbre.PAD.ordinal) 0.12 else 0.006) * sr
        fun sustainAt(x: Int): Double = when (timbre) {
            Timbre.PLUCK.ordinal -> exp(-(x - attack) / sr * 5.0)
            Timbre.PURE.ordinal, Timbre.PAD.ordinal -> 1.0
            Timbre.BASS.ordinal -> 0.6 + 0.4 * exp(-(x - attack) / sr * 2.0)
            else -> 0.55 + 0.45 * exp(-(x - attack) / sr * 3.0)
        }
        val rel = releaseFrames(v, null).toDouble()
        val env = when {
            age < attack -> age / attack
            age < len -> sustainAt(age)
            else -> sustainAt(len) * (1.0 - (age - len) / rel).coerceAtLeast(0.0)
        }
        val f = if (timbre == Timbre.BASS.ordinal) vFreq[v] * (1 + 0.5 * exp(-age / sr * 40)) else vFreq[v]
        vPhase[v] += twoPi * f / sr
        if (vPhase[v] > twoPi * 1000) vPhase[v] -= twoPi * 1000
        val p = vPhase[v]
        val wave = when (timbre) {
            Timbre.PURE.ordinal -> sin(p) * 0.85 + sin(2 * p) * 0.12
            Timbre.PLUCK.ordinal -> sin(p) * 0.7 + sin(2 * p) * 0.25 + sin(3 * p) * 0.1
            Timbre.BASS.ordinal -> tanh(sin(p) * 1.6) * 0.9
            Timbre.PAD.ordinal -> sin(p) * 0.5 + sin(p * 1.004) * 0.3 + sin(2 * p) * 0.15
            else -> sin(p) * 0.75 + sin(2 * p) * 0.2 + sin(4 * p) * 0.05
        }
        return wave * env * vVel[v] * 0.5
    }

    private fun sfxSample(v: Int, age: Int, t: Double, sr: Double, twoPi: Double): Double {
        val len = vLen[v]
        val p = age.toDouble() / len
        val g = vVel[v].toDouble()
        return when (vKind[v] - 10) {
            SfxType.BUBBLE.ordinal -> {
                vPhase[v] += twoPi * vFreq[v] * (1.0 - 0.55 * p) / sr
                (sin(vPhase[v]) + 0.25 * sin(2 * vPhase[v])) * minOf(1.0, age / (0.002 * sr)) * exp(-p * 4.5) * g
            }
            SfxType.CHIME.ordinal, SfxType.SPARKLE.ordinal -> {
                val chime = vKind[v] - 10 == SfxType.CHIME.ordinal
                vPhase2[v] += twoPi * vFreq[v] * (if (chime) 3.5 else 7.0) / sr
                vPhase[v] += twoPi * vFreq[v] / sr
                sin(vPhase[v] + 2.2 * exp(-t * 14) * sin(vPhase2[v])) * minOf(1.0, age / (0.0015 * sr)) * exp(-t * if (chime) 5.5 else 11.0) * g
            }
            SfxType.PAD.ordinal -> { // dreamy pad voice: two detuned soft triangles, slow swell and fade
                vPhase[v] += vFreq[v] * 1.004 / sr; vPhase2[v] += vFreq[v] * 0.996 / sr
                val tri1 = 4 * abs((vPhase[v] % 1.0) - 0.5) - 1; val tri2 = 4 * abs((vPhase2[v] % 1.0) - 0.5) - 1
                val env = sin(PI * p).let { it * it } * (1 - 0.3 * p)
                (tri1 + tri2) * 0.5 * env * g
            }
            SfxType.GLITTER.ordinal -> { // tiny glassy FM ping, very fast decay
                vPhase2[v] += twoPi * vFreq[v] * 2.76 / sr; vPhase[v] += twoPi * vFreq[v] / sr
                sin(vPhase[v] + 1.4 * exp(-t * 30) * sin(vPhase2[v])) * minOf(1.0, age / (0.001 * sr)) * exp(-t * 18) * g
            }
            SfxType.BLIP.ordinal -> { // Y2K bubbly digital blip: soft square (odd harmonics) with a pitch bend
                val f = vFreq[v] + (vFreq2[v] - vFreq[v]) * (1 - (1 - p) * (1 - p))
                vPhase[v] += twoPi * f / sr
                val sq = sin(vPhase[v]) + sin(3 * vPhase[v]) / 3 * 0.6 + sin(5 * vPhase[v]) / 5 * 0.3
                sq * minOf(1.0, age / (0.002 * sr)) * exp(-p * 3.2) * g
            }
            SfxType.SOFT.ordinal -> {
                vPhase[v] += vFreq[v] / sr
                (4 * abs((vPhase[v] % 1.0) - 0.5) - 1) * minOf(1.0, age / (0.01 * sr)) * exp(-p * 3.0) * g
            }
            SfxType.SWOOSH.ordinal -> {
                if (age % 64 == 0) setBandpass(v, 0, vFreq[v] + (vFreq2[v] - vFreq[v]) * p, 1.2)
                val env = sin(PI * p)
                bandpass(v, 0, noise().toDouble()) * env * env * g * 2.5
            }
            SfxType.SYLLABLE.ordinal -> {
                vPhase[v] += vFreq[v] * (1.0 + 0.06 * (1 - p)) * (1 + 0.01 * sin(t * 2 * PI * 6)) / sr
                val saw = 2 * ((vPhase[v] % 1.0) - 0.5)
                val src = saw * 0.7 + sin(vPhase[v] * twoPi) * 0.6
                val env = minOf(1.0, age / (0.012 * sr)) * minOf(1.0, (len - age) / (0.035 * sr)).coerceAtLeast(0.0)
                (bandpass(v, 0, src) * 1.6 + bandpass(v, 8, src) * 0.9 + sin(vPhase[v] * twoPi) * 0.35) * env * g
            }
            else -> {
                val f = vFreq[v] + (vFreq2[v] - vFreq[v]) * (p * p * (3 - 2 * p))
                vPhase[v] += twoPi * f * (1 + 0.015 * sin(t * 2 * PI * 5.5)) / sr
                val env = minOf(1.0, age / (0.02 * sr)) * minOf(1.0, (len - age) / (0.06 * sr)).coerceAtLeast(0.0)
                (sin(vPhase[v]) * 0.8 + sin(2 * vPhase[v]) * 0.25 + sin(3 * vPhase[v]) * 0.08) * env * g
            }
        }
    }
}
