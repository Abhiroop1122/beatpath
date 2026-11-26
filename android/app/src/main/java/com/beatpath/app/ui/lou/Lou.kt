package com.beatpath.app.ui.lou

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** Everything Lou can feel. Each mood is a target pose that every body part glides toward. */
enum class LouMood { Idle, Listen, Happy, Cheer, Think, Surprised, Encourage, Sleepy, Dance }

/**
 * The blended pose. Every value eases toward the current mood's target with a critically damped
 * response (≈110 ms for the face, ≈220 ms for the body), so mood changes melt into each other the
 * way iOS springs do — nothing ever snaps.
 */
private class Pose {
    var lid = 0.42f; var browL = 0.08f; var browR = 0.08f; var browTilt = 0f
    var arm = 0f; var tilt = 0f; var mouthW = 0.10f; var mouthExtra = 0f
    var eyeScale = 1f; var happyEyes = 0f; var thinkMouth = 0f; var smile = 1f
    var mouth = 0f; var lookX = 0f; var lookY = 0f

    fun target(m: LouMood): FloatArray = when (m) {   // lid, browL, browR, browTilt, arm, tilt, mouthW, mouthExtra, eyeScale, happyEyes, thinkMouth, smile
        LouMood.Idle -> floatArrayOf(0.42f, 0.08f, 0.08f, 0f, 0f, 0f, 0.10f, 0f, 1f, 0f, 0f, 1f)
        LouMood.Listen -> floatArrayOf(0.5f, 0.12f, 0.12f, 0f, 0.1f, 4f, 0.09f, 0f, 1f, 0f, 0f, 0.8f)
        LouMood.Happy -> floatArrayOf(0.2f, 0.25f, 0.25f, 0f, 0.45f, 0f, 0.14f, 0.25f, 1f, 0f, 0f, 1f)
        LouMood.Cheer -> floatArrayOf(0.2f, 0.3f, 0.3f, 0f, 1f, 0f, 0.16f, 0.7f, 1f, 1f, 0f, 1f)
        LouMood.Think -> floatArrayOf(0.35f, 0.05f, 0.4f, 8f, 0.25f, 6f, 0.10f, 0f, 1f, 0f, 1f, 1f)
        LouMood.Surprised -> floatArrayOf(0f, 0.55f, 0.55f, 0f, 0.7f, 0f, 0.07f, 0.6f, 1.15f, 0f, 0f, 1f)
        LouMood.Encourage -> floatArrayOf(0.3f, 0.2f, 0.2f, -10f, 0.35f, -5f, 0.11f, 0f, 1f, 0f, 0f, 0.6f)
        LouMood.Sleepy -> floatArrayOf(0.72f, 0.05f, 0.05f, 0f, 0f, 0f, 0.09f, 0f, 1f, 0f, 0f, 0.9f)
        LouMood.Dance -> floatArrayOf(0.35f, 0.15f, 0.15f, 0f, 0.4f, 0f, 0.12f, 0.15f, 1f, 0f, 0f, 1f)
    }

    fun ease(m: LouMood, mouthIn: Float, lx: Float, ly: Float, dt: Float) {
        val tg = target(m)
        val face = 1f - exp(-dt / 0.11f); val body = 1f - exp(-dt / 0.22f); val mo = 1f - exp(-dt / 0.045f); val eye = 1f - exp(-dt / 0.09f)
        lid += (tg[0] - lid) * face; browL += (tg[1] - browL) * face; browR += (tg[2] - browR) * face; browTilt += (tg[3] - browTilt) * face
        arm += (tg[4] - arm) * body; tilt += (tg[5] - tilt) * body; mouthW += (tg[6] - mouthW) * face; mouthExtra += (tg[7] - mouthExtra) * face
        eyeScale += (tg[8] - eyeScale) * face; happyEyes += (tg[9] - happyEyes) * face; thinkMouth += (tg[10] - thinkMouth) * face; smile += (tg[11] - smile) * face
        mouth += (mouthIn - mouth) * mo
        lookX += (lx - lookX) * eye; lookY += (ly - lookY) * eye
    }
}

/**
 * Lou, the guide: a big teal sub-bass creature with a woofer belly, drawn from layered parts so each
 * part moves on its own: breathing, eased blinks every 2–6 s, soft look-around, word-synced mouth,
 * a cheer jump with anticipation and landing squash, and a beat-locked bounce + woofer pump.
 */
@Composable
fun Lou(
    modifier: Modifier = Modifier,
    mood: LouMood = LouMood.Idle,
    mouthOpen: Float = 0f,
    beatPhase: Float? = null,
    reduceMotion: Boolean = false,
) {
    val moodState by rememberUpdatedState(mood)
    val beat by rememberUpdatedState(beatPhase)
    val mouthIn by rememberUpdatedState(mouthOpen)
    val pose = remember { Pose() }
    var t by remember { mutableFloatStateOf(0f) }
    var blink by remember { mutableFloatStateOf(0f) }
    var moodSince by remember { mutableFloatStateOf(0f) }
    var frame by remember { mutableFloatStateOf(0f) }   // forces redraw each frame

    LaunchedEffect(reduceMotion) {
        val rnd = Random(System.nanoTime())
        var start = -1L
        var last = 0f
        var nextBlink = 1.5f
        var nextLook = 2.5f
        var tx = 0f; var ty = 0f
        var lastMood: LouMood? = null
        while (true) {
            withFrameNanos { now ->
                if (start < 0) start = now
                val time = (now - start) / 1e9f
                val dt = (time - last).coerceIn(0f, 0.05f); last = time
                t = time
                if (lastMood != moodState) { lastMood = moodState; moodSince = time }
                if (!reduceMotion) {
                    // eased blink: smooth close/open over 170 ms (cosine), sometimes a double blink
                    if (time >= nextBlink) {
                        val p = (time - nextBlink) / 0.17f
                        blink = if (p < 1f) ((1 - cos(p * 2 * PI.toFloat())) / 2f) else 0f
                        if (p >= 1f) nextBlink = time + if (rnd.nextFloat() < 0.15f) 0.22f else 2f + rnd.nextFloat() * 4f
                    }
                    if (time >= nextLook) {
                        tx = (rnd.nextFloat() - 0.5f) * 1.5f; ty = (rnd.nextFloat() - 0.5f) * 0.7f
                        if (rnd.nextFloat() < 0.45f) { tx = 0f; ty = 0f }
                        nextLook = time + 1.4f + rnd.nextFloat() * 3.5f
                    }
                }
                val (lx, ly) = when (moodState) {
                    LouMood.Think -> 0.8f to -0.9f
                    LouMood.Listen -> 0f to -0.2f
                    LouMood.Cheer, LouMood.Happy, LouMood.Surprised, LouMood.Dance -> 0f to 0f
                    else -> tx to ty
                }
                pose.ease(moodState, mouthIn, lx, ly, if (reduceMotion) 1f else dt)
                frame = time
            }
        }
    }

    Canvas(modifier.semantics { contentDescription = "Lou, your guide, looking ${mood.name.lowercase()}" }) {
        @Suppress("UNUSED_VARIABLE") val f = frame
        drawLou(pose, moodState, t, t - moodSince, blink, beat, reduceMotion)
    }
}

private val BodyTop = Color(0xFF2BB3B0)
private val BodyBottom = Color(0xFF0F7E86)
private val BodyRim = Color(0xFF0A5F66)
private val Belly = Color(0xFF7FE0D2)
private val BellyDeep = Color(0xFF45C2B6)
private val Ink = Color(0xFF0E2A2E)

/** Smooth 0→1→0 bump with eased edges (for jumps): ease-in-out sine. */
private fun bump(p: Float) = sin(p.coerceIn(0f, 1f) * PI.toFloat())

private fun DrawScope.drawLou(pose: Pose, mood: LouMood, t: Float, since: Float, blink: Float, beatPhase: Float?, still: Boolean) {
    val w = size.width; val h = size.height
    val unit = min(w / 1.15f, h)
    val cx = w / 2f
    val floor = h * 0.97f

    // ---------------- body motion (all continuous curves) ----------------
    var squash = 0f; var lift = 0f; var pump = 0f
    var tilt = pose.tilt
    if (!still) {
        squash = sin(t * 2f * PI.toFloat() / 4.2f) * 0.018f   // breathing, 4.2 s cycle
        when (mood) {
            LouMood.Cheer -> {
                val c = since % 1.1f
                when {
                    c < 0.2f -> squash += 0.11f * bump(c / 0.4f)                                    // anticipation (eased)
                    c < 0.72f -> { val p = (c - 0.2f) / 0.52f; lift = bump(p) * 0.22f; squash -= 0.07f * bump(p) }
                    c < 0.95f -> squash += 0.09f * bump((c - 0.72f) / 0.23f)                      // soft landing
                }
            }
            LouMood.Happy -> squash += sin(t * 7f) * 0.015f
            LouMood.Surprised -> { val p = min(1f, since / 0.35f); lift = bump(p) * 0.05f }
            LouMood.Sleepy -> squash += sin(t * 2f * PI.toFloat() / 5f) * 0.02f
            LouMood.Listen -> tilt += 3f * sin(t * 1.1f)
            else -> {}
        }
        if (beatPhase != null) {
            val k = (1f - beatPhase).coerceIn(0f, 1f)
            pump = k * k * (3 - 2 * k)       // smoothstep instead of a hard decay
            squash += pump * 0.06f
            if (mood == LouMood.Dance) tilt += 3f * sin(t * PI.toFloat())
        }
    }
    val bodyW = unit * 1.05f * (1f + squash * 0.7f)
    val bodyH = unit * 0.86f * (1f - squash)
    val baseY = floor - lift * unit
    val left = cx - bodyW / 2f
    val top = baseY - bodyH

    val shadowK = 1f - lift * 2.2f
    drawOval(Color(0x33000000), Offset(cx - bodyW * 0.42f * shadowK, floor - unit * 0.035f), Size(bodyW * 0.84f * shadowK, unit * 0.07f))

    withTransform({ rotate(tilt, Offset(cx, baseY)) }) {
        drawOval(BodyRim, Offset(cx - bodyW * 0.34f, baseY - unit * 0.07f), Size(bodyW * 0.22f, unit * 0.09f))
        drawOval(BodyRim, Offset(cx + bodyW * 0.12f, baseY - unit * 0.07f), Size(bodyW * 0.22f, unit * 0.09f))

        // arms: eased raise + gentle sway (never snaps)
        val armY = top + bodyH * 0.55f
        for (side in listOf(-1f, 1f)) {
            val ax = cx + side * bodyW * 0.38f
            val wave = if (!still && (mood == LouMood.Happy || mood == LouMood.Dance || mood == LouMood.Cheer)) 9f * sin(t * 6f + side) * pose.arm else 0f
            val ang = -side * (28f + pose.arm * 125f + wave)
            rotate(ang, Offset(ax, armY)) {
                val len = unit * 0.3f; val wid = unit * 0.13f
                drawRoundRect(Brush.horizontalGradient(listOf(BodyTop, BodyBottom), ax - wid / 2f, ax + wid / 2f), Offset(ax - wid / 2f, armY), Size(wid, len), CornerRadius(wid / 2f))
                drawRoundRect(BodyRim, Offset(ax - wid / 2f, armY), Size(wid, len), CornerRadius(wid / 2f), style = Stroke(unit * 0.01f))
            }
        }

        val body = Path().apply {
            moveTo(left + bodyW * 0.06f, baseY - bodyH * 0.06f)
            cubicTo(left - bodyW * 0.02f, top + bodyH * 0.45f, left + bodyW * 0.18f, top, cx, top)
            cubicTo(left + bodyW * 0.82f, top, left + bodyW * 1.02f, top + bodyH * 0.45f, left + bodyW * 0.94f, baseY - bodyH * 0.06f)
            cubicTo(left + bodyW * 0.9f, baseY + bodyH * 0.01f, left + bodyW * 0.1f, baseY + bodyH * 0.01f, left + bodyW * 0.06f, baseY - bodyH * 0.06f)
            close()
        }
        drawPath(body, Brush.verticalGradient(listOf(BodyTop, BodyBottom), top, baseY))
        drawPath(body, BodyRim, style = Stroke(unit * 0.012f))
        clipPath(body) {
            val bellyC = Offset(cx, top + bodyH * 0.68f)
            drawOval(Brush.verticalGradient(listOf(Belly, BellyDeep), bellyC.y - bodyH * 0.3f, bellyC.y + bodyH * 0.3f), Offset(bellyC.x - bodyW * 0.3f, bellyC.y - bodyH * 0.27f), Size(bodyW * 0.6f, bodyH * 0.6f))
            drawOval(Brush.verticalGradient(listOf(Color(0x8CFFFFFF), Color(0x00FFFFFF)), top, top + bodyH * 0.35f), Offset(left + bodyW * 0.14f, top + bodyH * 0.03f), Size(bodyW * 0.72f, bodyH * 0.32f))
            drawOval(Color(0x66FFFFFF), Offset(left + bodyW * 0.22f, top + bodyH * 0.07f), Size(bodyW * 0.18f, bodyH * 0.07f))
        }

        // woofer
        val wc = Offset(cx, top + bodyH * 0.7f); val r = unit * 0.15f
        drawCircle(Color(0xFF173D41), r * 1.12f, wc)
        drawCircle(Brush.radialGradient(listOf(Color(0xFF5E7477), Color(0xFF223236)), wc, r), r * (1f + pump * 0.08f), wc)
        for (i in 1..2) drawCircle(Color(0x33FFFFFF), r * (0.55f + i * 0.17f) * (1f + pump * 0.08f), wc, style = Stroke(unit * 0.006f))
        drawCircle(Brush.radialGradient(listOf(Color(0xFF8FA3A6), Color(0xFF2C3E42)), wc - Offset(r * 0.1f, r * 0.1f), r * 0.4f), r * 0.36f * (1f + pump * 0.15f), wc)
        drawCircle(Color(0x99FFFFFF), r * 0.1f, wc - Offset(r * 0.14f, r * 0.14f))

        // face
        val eyeY = top + bodyH * 0.3f; val eyeDX = bodyW * 0.14f; val eyeR = unit * 0.075f
        val lid = max(pose.lid, blink)
        for (side in listOf(-1f, 1f)) {
            val ec = Offset(cx + side * eyeDX, eyeY)
            val er = eyeR * pose.eyeScale
            // open eye, fading out as happy (closed-arc) eyes fade in
            val openA = (1f - pose.happyEyes).coerceIn(0f, 1f)
            if (openA > 0.01f) {
                drawCircle(Color.White.copy(alpha = openA), er, ec)
                val pupil = Offset(ec.x + pose.lookX * er * 0.35f, ec.y + pose.lookY * er * 0.3f + er * 0.12f)
                drawCircle(Ink.copy(alpha = openA), er * 0.55f, pupil)
                drawCircle(Color.White.copy(alpha = openA), er * 0.17f, pupil + Offset(-er * 0.18f, -er * 0.2f))
                if (lid > 0.01f) {
                    val lidH = er * 2f * lid
                    clipPath(Path().apply { addOval(Rect(ec, er)) }) {
                        drawRect(Brush.verticalGradient(listOf(BodyTop, Color(0xFF22A3A2)), ec.y - er, ec.y - er + lidH), Offset(ec.x - er, ec.y - er), Size(er * 2f, lidH))
                    }
                    drawLine(BodyRim.copy(alpha = openA), Offset(ec.x - er * 0.95f, ec.y - er + lidH), Offset(ec.x + er * 0.95f, ec.y - er + lidH), unit * 0.012f, StrokeCap.Round)
                }
                drawCircle(BodyRim.copy(alpha = openA), er, ec, style = Stroke(unit * 0.008f))
            }
            if (pose.happyEyes > 0.01f) {
                drawArc(Ink.copy(alpha = pose.happyEyes.coerceIn(0f, 1f)), 200f, 140f, false, Offset(ec.x - er, ec.y - er * 0.6f), Size(er * 2f, er * 1.4f), style = Stroke(unit * 0.018f, cap = StrokeCap.Round))
            }
            val by = ec.y - eyeR * (1.35f + if (side < 0) pose.browL else pose.browR)
            rotate(side * pose.browTilt, Offset(ec.x, by)) { drawLine(BodyRim, Offset(ec.x - eyeR * 0.7f, by), Offset(ec.x + eyeR * 0.7f, by), unit * 0.02f, StrokeCap.Round) }
        }
        drawOval(Color(0x40FF8FA3), Offset(cx - eyeDX - eyeR * 1.6f, eyeY + eyeR * 0.9f), Size(eyeR * 1.3f, eyeR * 0.6f))
        drawOval(Color(0x40FF8FA3), Offset(cx + eyeDX + eyeR * 0.3f, eyeY + eyeR * 0.9f), Size(eyeR * 1.3f, eyeR * 0.6f))

        // mouth: one continuous shape — smile line blends into an open mouth as it opens
        val my = eyeY + eyeR * 1.7f
        val mw = unit * pose.mouthW
        val open = (pose.mouth + pose.mouthExtra).coerceIn(0f, 1f)
        val smileArc = 0.7f * pose.smile
        if (pose.thinkMouth > 0.5f && open < 0.08f) {
            drawLine(Ink, Offset(cx - mw * 0.1f, my + unit * 0.01f), Offset(cx + mw * 0.6f, my - unit * 0.005f), unit * 0.014f, StrokeCap.Round)
        } else {
            val mh = unit * 0.09f * open
            val p = Path().apply {
                moveTo(cx - mw / 2f, my)
                quadraticTo(cx, my + mw * 0.35f * smileArc - mh * 0.25f, cx + mw / 2f, my)        // upper lip (a smile when closed)
                quadraticTo(cx, my + mw * 0.35f * smileArc + mh * 1.6f, cx - mw / 2f, my)          // lower lip opens smoothly
                close()
            }
            if (open > 0.03f) {
                drawPath(p, Color(0xFF3A1420))
                clipPath(p) { drawOval(Color(0xFFE86A7E), Offset(cx - mw * 0.3f, my + mh * 0.6f), Size(mw * 0.6f, mh + 1f)) }
            }
            drawPath(Path().apply { moveTo(cx - mw / 2f, my); quadraticTo(cx, my + mw * 0.35f * smileArc - mh * 0.25f, cx + mw / 2f, my) }, Ink, style = Stroke(unit * 0.014f, cap = StrokeCap.Round))
        }
        if (mood == LouMood.Sleepy && !still) {
            val zp = (t % 2.4f) / 2.4f
            drawZ(Offset(cx + bodyW * 0.38f + zp * unit * 0.1f, top - zp * unit * 0.2f), unit * 0.05f * (0.6f + zp), bump(zp))
        }
    }
}

private fun DrawScope.drawZ(at: Offset, s: Float, alpha: Float) {
    val p = Path().apply { moveTo(at.x, at.y); lineTo(at.x + s, at.y); lineTo(at.x, at.y + s); lineTo(at.x + s, at.y + s) }
    drawPath(p, Color(0xFF0F7E86).copy(alpha = alpha.coerceIn(0f, 1f)), style = Stroke(s * 0.22f, cap = StrokeCap.Round))
}
