package com.beatpath.app.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/* Real-time DSP blocks. All are allocation-free in process(); buffers are sized up front. */

fun dbToGain(db: Float): Float = 10f.pow(db / 20f)
fun gainToDb(g: Float): Float = if (g <= 1e-6f) -120f else 20f * kotlin.math.log10(g)

/** RBJ "Audio EQ Cookbook" biquad, transposed direct form II. */
class Biquad {
    private var b0 = 1.0; private var b1 = 0.0; private var b2 = 0.0; private var a1 = 0.0; private var a2 = 0.0
    private var z1 = 0.0; private var z2 = 0.0
    var bypass = true

    fun highPass(sr: Int, f: Double, q: Double = 0.707) = set(sr, f, q) { w, alpha, cw ->
        doubleArrayOf((1 + cw) / 2, -(1 + cw), (1 + cw) / 2, 1 + alpha, -2 * cw, 1 - alpha)
    }
    fun lowPass(sr: Int, f: Double, q: Double = 0.707) = set(sr, f, q) { _, alpha, cw ->
        doubleArrayOf((1 - cw) / 2, 1 - cw, (1 - cw) / 2, 1 + alpha, -2 * cw, 1 - alpha)
    }
    fun peak(sr: Int, f: Double, q: Double, gainDb: Double) = set(sr, f, q) { _, alpha, cw ->
        val a = 10.0.pow(gainDb / 40)
        doubleArrayOf(1 + alpha * a, -2 * cw, 1 - alpha * a, 1 + alpha / a, -2 * cw, 1 - alpha / a)
    }
    fun lowShelf(sr: Int, f: Double, gainDb: Double) = set(sr, f, 0.707) { _, alpha, cw ->
        val a = 10.0.pow(gainDb / 40); val sa = 2 * sqrt(a) * alpha
        doubleArrayOf(a * ((a + 1) - (a - 1) * cw + sa), 2 * a * ((a - 1) - (a + 1) * cw), a * ((a + 1) - (a - 1) * cw - sa),
            (a + 1) + (a - 1) * cw + sa, -2 * ((a - 1) + (a + 1) * cw), (a + 1) + (a - 1) * cw - sa)
    }
    fun highShelf(sr: Int, f: Double, gainDb: Double) = set(sr, f, 0.707) { _, alpha, cw ->
        val a = 10.0.pow(gainDb / 40); val sa = 2 * sqrt(a) * alpha
        doubleArrayOf(a * ((a + 1) + (a - 1) * cw + sa), -2 * a * ((a - 1) + (a + 1) * cw), a * ((a + 1) + (a - 1) * cw - sa),
            (a + 1) - (a - 1) * cw + sa, 2 * ((a - 1) - (a + 1) * cw), (a + 1) - (a - 1) * cw - sa)
    }

    private inline fun set(sr: Int, f: Double, q: Double, coeffs: (Double, Double, Double) -> DoubleArray) {
        val w0 = 2 * PI * f.coerceIn(10.0, sr * 0.45) / sr
        val alpha = sin(w0) / (2 * q)
        val c = coeffs(w0, alpha, cos(w0))
        val a0 = c[3]
        b0 = c[0] / a0; b1 = c[1] / a0; b2 = c[2] / a0; a1 = c[4] / a0; a2 = c[5] / a0
        bypass = false
    }

    fun process(x: Double): Double {
        if (bypass) return x
        val y = b0 * x + z1
        z1 = b1 * x - a1 * y + z2
        z2 = b2 * x - a2 * y
        return y
    }

    /** Magnitude response in dB at [f] — drives the EQ graph so the curve matches what you hear. */
    fun responseDb(sr: Int, f: Double): Double {
        if (bypass) return 0.0
        val w = 2 * PI * f / sr
        val cr = cos(w); val ci = -sin(w); val c2r = cos(2 * w); val c2i = -sin(2 * w)
        val nr = b0 + b1 * cr + b2 * c2r; val ni = b1 * ci + b2 * c2i
        val dr = 1 + a1 * cr + a2 * c2r; val di = a1 * ci + a2 * c2i
        return 10 * kotlin.math.log10((nr * nr + ni * ni) / (dr * dr + di * di))
    }
}

/** Feed-forward compressor with soft knee, peak envelope and makeup gain. */
class Compressor(private val sr: Int) {
    var thresholdDb = 0f; var ratio = 1f; var makeupDb = 0f; var kneeDb = 6f
    private var attackCoef = 0.0; private var releaseCoef = 0.0
    private var grDb = 0.0
    /** Largest gain reduction in the last block, in positive dB (for the meter). */
    var meterGr = 0f

    fun setTimes(attackMs: Float, releaseMs: Float) {
        attackCoef = exp(-1.0 / (max(0.1f, attackMs) * 0.001 * sr))
        releaseCoef = exp(-1.0 / (max(1f, releaseMs) * 0.001 * sr))
    }

    /** [key] is the detector input: the signal itself, or another track for sidechain ducking. */
    fun process(x: Double, key: Double = x): Double {
        val levelDb = 20 * kotlin.math.log10(abs(key) + 1e-9)
        val over = levelDb - thresholdDb
        val target = when {
            ratio <= 1f -> 0.0
            2 * over < -kneeDb -> 0.0
            2 * abs(over) <= kneeDb -> (1 - 1.0 / ratio) * (over + kneeDb / 2).pow(2) / (2 * kneeDb)
            else -> (1 - 1.0 / ratio) * over
        }
        grDb = if (target > grDb) attackCoef * grDb + (1 - attackCoef) * target else releaseCoef * grDb + (1 - releaseCoef) * target
        if (grDb > meterGr) meterGr = grDb.toFloat()
        return x * 10.0.pow((makeupDb - grDb) / 20)
    }
}

/** Master limiter: instant attack, smooth release, then a hard ceiling so nothing passes 0 dBFS. */
class Limiter(private val sr: Int) {
    var ceiling = 1.0; var drive = 1.0
    private var env = 1.0
    private val release = exp(-1.0 / (0.08 * sr))
    var meterGr = 0f

    fun process(l: Double, r: Double, out: DoubleArray) {
        val dl = l * drive; val dr = r * drive
        val peak = max(abs(dl), abs(dr))
        val need = if (peak > ceiling) ceiling / peak else 1.0
        env = if (need < env) need else release * env + (1 - release) * need
        val gr = (-20 * kotlin.math.log10(env)).toFloat()
        if (gr > meterGr) meterGr = gr
        out[0] = (dl * env).coerceIn(-ceiling, ceiling)
        out[1] = (dr * env).coerceIn(-ceiling, ceiling)
    }
}

/** Freeverb (Jezar) — 8 combs + 4 allpasses per side, sizes scaled from 44.1 kHz. */
class Reverb(sr: Int) {
    private val scale = sr / 44100.0
    private val combL = intArrayOf(1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617).map { Comb((it * scale).toInt()) }
    private val combR = intArrayOf(1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617).map { Comb(((it + 23) * scale).toInt()) }
    private val apL = intArrayOf(556, 441, 341, 225).map { AllPass((it * scale).toInt()) }
    private val apR = intArrayOf(556, 441, 341, 225).map { AllPass(((it + 23) * scale).toInt()) }

    fun set(size: Float, damping: Float) {
        val fb = 0.7 + 0.28 * size
        val d = damping * 0.4
        combL.forEach { it.feedback = fb; it.damp = d }; combR.forEach { it.feedback = fb; it.damp = d }
    }

    fun process(input: Double, out: DoubleArray) {
        val x = input * 0.015
        var l = 0.0; var r = 0.0
        for (c in combL) l += c.process(x)
        for (c in combR) r += c.process(x)
        for (a in apL) l = a.process(l)
        for (a in apR) r = a.process(r)
        out[0] = l; out[1] = r
    }

    private class Comb(n: Int) {
        private val buf = DoubleArray(maxOf(1, n)); private var i = 0; private var store = 0.0
        var feedback = 0.84; var damp = 0.2
        fun process(x: Double): Double {
            val y = buf[i]
            store = y * (1 - damp) + store * damp
            buf[i] = x + store * feedback
            if (++i >= buf.size) i = 0
            return y
        }
    }
    private class AllPass(n: Int) {
        private val buf = DoubleArray(maxOf(1, n)); private var i = 0
        fun process(x: Double): Double {
            val b = buf[i]; val y = -x + b
            buf[i] = x + b * 0.5
            if (++i >= buf.size) i = 0
            return y
        }
    }
}

/** Tempo-synced feedback delay: damping low-pass in the loop, smoothed time changes (no clicks), slight stereo offset. */
class Delay(private val sr: Int) {
    private val buf = DoubleArray(sr * 2)
    private var w = 0
    private var lp = 0.0
    private var cur = (sr / 4).toDouble()
    var delaySamples = sr / 4
    var feedback = 0.35; var damping = 0.4

    private fun read(d: Double): Double {
        val n = buf.size
        val pos = w - d
        val fl = kotlin.math.floor(pos)
        val i0 = Math.floorMod(fl.toInt(), n)
        val frac = pos - fl
        return buf[i0] * (1 - frac) + buf[(i0 + 1) % n] * frac
    }

    fun process(x: Double, out: DoubleArray) {
        cur += (delaySamples.coerceIn(1, buf.size - 2) - cur) * 0.0008
        val y = read(cur)
        lp += (1 - damping) * (y - lp)
        buf[w] = x + lp * feedback
        w = (w + 1) % buf.size
        out[0] = y; out[1] = read(cur + sr / 90.0)
    }
}

/**
 * Cytomic (Andrew Simper) trapezoidal SVF low-pass: stable and correct up to near Nyquist,
 * unlike the Chamberlin SVF which tops out around sr/6. Coefficients are refreshed by the caller.
 */
class Svf {
    private var ic1 = 0.0; private var ic2 = 0.0
    private var a1 = 1.0; private var a2 = 0.0; private var a3 = 0.0
    fun set(sr: Int, cutoff: Double, resonance: Double) {
        val g = tan(PI * cutoff.coerceIn(20.0, sr * 0.45) / sr)
        val k = 2.0 - 2.0 * resonance.coerceIn(0.0, 0.95)
        a1 = 1.0 / (1.0 + g * (g + k)); a2 = g * a1; a3 = g * a2
    }
    fun process(x: Double): Double {
        val v3 = x - ic2
        val v1 = a1 * ic1 + a2 * v3
        val v2 = ic2 + a2 * ic1 + a3 * v3
        ic1 = 2 * v1 - ic1; ic2 = 2 * v2 - ic2
        return v2
    }
    fun reset() { ic1 = 0.0; ic2 = 0.0 }
}

/** Lush stereo chorus: two modulated delay taps (L/R in quadrature) for that dreamy Y2K shimmer. */
class Chorus(private val sr: Int) {
    private val buf = DoubleArray(sr / 20)
    private var w = 0
    private var ph = 0.0
    private fun tap(d: Double): Double {
        val n = buf.size; val pos = w - d; val f = kotlin.math.floor(pos)
        val i0 = Math.floorMod(f.toInt(), n); val fr = pos - f
        return buf[i0] * (1 - fr) + buf[(i0 + 1) % n] * fr
    }
    fun process(x: Double, out: DoubleArray) {
        buf[w] = x; w = (w + 1) % buf.size
        ph += 2 * PI * 0.55 / sr
        val base = 0.014 * sr; val depth = 0.0035 * sr
        out[0] = tap(base + depth * sin(ph)); out[1] = tap(base + depth * cos(ph))
    }
}

/**
 * ITU-R BS.1770 loudness (what streaming services normalise to, in LUFS).
 * K-weighting = high shelf (+4 dB @ 1.68 kHz) then high-pass (38 Hz); 400 ms blocks with 75 % overlap;
 * integrated loudness uses the absolute (−70 LUFS) and relative (−10 LU) gates.
 */
class LoudnessMeter(private val sr: Int) {
    private val shelfL = Biquad().apply { highShelf(sr, 1681.97, 3.99984) }
    private val shelfR = Biquad().apply { highShelf(sr, 1681.97, 3.99984) }
    private val hpL = Biquad().apply { highPass(sr, 38.1355, 0.5003) }
    private val hpR = Biquad().apply { highPass(sr, 38.1355, 0.5003) }
    private val subLen = sr / 10
    private var subSum = 0.0; private var subCount = 0
    private val subs = DoubleArray(4); private var subIdx = 0; private var subsFilled = 0
    private val blocks = DoubleArray(12000); private var nBlocks = 0
    var momentary = -70f; private set
    var integrated = -70f; private set

    fun reset() { subSum = 0.0; subCount = 0; subsFilled = 0; nBlocks = 0; momentary = -70f; integrated = -70f }

    fun process(l: Double, r: Double) {
        val kl = hpL.process(shelfL.process(l)); val kr = hpR.process(shelfR.process(r))
        subSum += kl * kl + kr * kr
        if (++subCount < subLen) return
        subs[subIdx] = subSum / subCount; subIdx = (subIdx + 1) % 4; subSum = 0.0; subCount = 0
        if (subsFilled < 4) { subsFilled++; if (subsFilled < 4) return }
        val ms = subs.average()
        momentary = lufs(ms)
        if (nBlocks < blocks.size) blocks[nBlocks++] = ms
        // gated integration
        var sum = 0.0; var n = 0
        for (i in 0 until nBlocks) if (lufs(blocks[i]) > -70f) { sum += blocks[i]; n++ }
        if (n == 0) return
        val rel = lufs(sum / n) - 10f
        sum = 0.0; n = 0
        for (i in 0 until nBlocks) { val lb = lufs(blocks[i]); if (lb > -70f && lb > rel) { sum += blocks[i]; n++ } }
        if (n > 0) integrated = lufs(sum / n)
    }

    private fun lufs(meanSquare: Double): Float = if (meanSquare <= 1e-12) -120f else (-0.691 + 10 * kotlin.math.log10(meanSquare)).toFloat()
}

@Suppress("unused") private fun unusedLn() = ln(1.0) + tan(0.0)
