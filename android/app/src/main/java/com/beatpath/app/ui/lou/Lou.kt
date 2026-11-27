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
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale
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
enum class LouMood { Idle, Listen, Happy, Cheer, Think, Surprised, Encourage, Sleepy, Dance, Sad }

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
        // droopy lids, brows tilted up in the middle, head down, the smile turned into a frown
        LouMood.Sad -> floatArrayOf(0.6f, 0.02f, 0.02f, 14f, 0f, -3f, 0.085f, 0f, 1f, 0f, 0f, -0.75f)
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
 * Lou, the guide, drawn from his exact SVG artwork (docs/lou.svg, the GitHub banner's Lou). His parts move
 * on their own: breathing, eased blinks every 2 to 6 s, look-arounds, mood lids and mouth, a talking
 * mouth, a cheer jump with anticipation and landing squash, and a beat-locked bounce and speaker pump.
 */
@Composable
fun Lou(
    modifier: Modifier = Modifier,
    mood: LouMood = LouMood.Idle,
    mouthOpen: Float = 0f,
    beatPhase: Float? = null,
    reduceMotion: Boolean = com.beatpath.app.ui.aero.Motion.reduce,
    outfit: LouOutfit = LouLook.outfit,
) {
    val moodState by rememberUpdatedState(mood)
    val beat by rememberUpdatedState(beatPhase)
    val mouthIn by rememberUpdatedState(mouthOpen)
    val look by rememberUpdatedState(outfit)
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
                    LouMood.Sad -> 0f to 0.8f
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
        drawLou(pose, moodState, t, t - moodSince, blink, beat, reduceMotion, look)
    }
}

/** A hand or foot of the SVG body: the round region cut out of the body, the slightly bigger patch drawn back, and its joint. */
private class Limb(val hand: Boolean, val side: Float, cx: Float, cy: Float, r: Float, px: Float, py: Float, margin: Float = 3.5f) {
    val cut = Path().apply { addOval(Rect(Offset(cx, cy), r)) }
    val patch = Path().apply { addOval(Rect(Offset(cx, cy), r + margin)) }
    val pivot = Offset(px, py)
}

// positions measured on docs/lou.svg (SVG units): hands are the round lumps, feet the flat stubs at the sides
private val Limbs = listOf(
    Limb(hand = false, side = -1f, cx = 12f, cy = 128.5f, r = 8f, px = 23f, py = 127.5f, margin = 2.5f),
    Limb(hand = false, side = 1f, cx = 158f, cy = 127.5f, r = 8f, px = 148f, py = 126.5f, margin = 2.5f),
    Limb(hand = true, side = -1f, cx = 37f, cy = 126f, r = 16f, px = 37f, py = 111f),
    Limb(hand = true, side = 1f, cx = 131f, cy = 126f, r = 16f, px = 131f, py = 111f),
)

/** Smooth 0→1→0 bump with eased edges (for jumps): ease-in-out sine. */
private fun bump(p: Float) = sin(p.coerceIn(0f, 1f) * PI.toFloat())
private fun smooth(e0: Float, e1: Float, x: Float): Float { val k = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f); return k * k * (3 - 2 * k) }

/**
 * Draws Lou from his exact SVG artwork (LouArt) and animates the parts: the whole body breathes, jumps,
 * bounces and tilts; the heavy lids blink and move with mood; the pupils look around inside the eye
 * whites; the smile widens, flattens or opens into a talking mouth; the speaker pumps on the beat and
 * the arm lines sway. At rest with mood Idle he matches the SVG exactly.
 */
private fun DrawScope.drawLou(pose: Pose, mood: LouMood, t: Float, since: Float, blink: Float, beatPhase: Float?, still: Boolean, outfit: LouOutfit = LouOutfit()) {
    val A = LouArt
    val skin = skinOf(outfit.skin, t)
    // a hat pokes above his head, so leave room for it inside the canvas
    val headroom = if (outfit.hat != null) 30f else 0f
    // fit the 171 × 144 artwork into the canvas, centred, standing on the bottom edge
    val s = min(size.width / A.WIDTH, size.height / (A.HEIGHT + headroom))
    val ox = (size.width - A.WIDTH * s) / 2f
    val oy = size.height - A.HEIGHT * s

    // ---------------- body motion (same curves as before, in SVG units) ----------------
    var squash = 0f; var lift = 0f; var pump = 0f
    var tilt = pose.tilt
    if (!still) {
        squash = sin(t * 2f * PI.toFloat() / 4.2f) * 0.018f   // breathing, 4.2 s cycle
        when (mood) {
            LouMood.Cheer -> {
                val c = since % 1.1f
                when {
                    c < 0.2f -> squash += 0.11f * bump(c / 0.4f)                                    // anticipation
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
            pump = k * k * (3 - 2 * k)
            squash += pump * 0.06f
            if (mood == LouMood.Dance) tilt += 3f * sin(t * PI.toFloat())
        }
    }
    val base = Offset(A.WIDTH / 2f, 136f)              // where his feet meet the ground
    val liftU = lift * 120f

    withTransform({ translate(ox, oy); scale(s, s, Offset.Zero) }) {
        // ground shadow shrinks while he's in the air
        val shK = 1f - lift * 2.2f
        drawOval(Color(0x2E0B3A40), Offset(base.x - 62f * shK, 136.5f), Size(124f * shK, 7f))

        withTransform({
            translate(0f, -liftU)
            scale(1f + squash * 0.7f, 1f - squash, base)
            rotate(tilt, base)
        }) {
            // Hands and feet are part of the body outline in the SVG. To sway them, the body goes on its own layer,
            // each hand or foot is cut out of it and drawn back rotated a few degrees around its wrist or ankle.
            // The redrawn piece is slightly bigger than the cut, so no gap ever opens inside the body.
            val excited = mood == LouMood.Happy || mood == LouMood.Dance || mood == LouMood.Cheer
            fun handAngle(side: Float) = if (still) 0f else
                2.2f * sin(t * 1.6f + side * 1.3f) + (if (excited) 5f * sin(t * 6f + side) * pose.arm else 0f)
            fun footAngle(side: Float) = if (still) 0f else
                -side * (2.5f * (0.5f + 0.5f * sin(t * 1.25f + side * 2f)) + pump * 7f)
            drawContext.canvas.saveLayer(Rect(-30f, -40f, A.WIDTH + 30f, A.HEIGHT + 20f), androidx.compose.ui.graphics.Paint())
            drawPath(A.body.path, skin.tint(A.body.color))
            for (limb in Limbs) {
                val ang = if (limb.hand) handAngle(limb.side) else footAngle(limb.side)
                if (kotlin.math.abs(ang) < 0.01f) continue
                clipPath(limb.cut) { drawPath(A.body.path, Color.Black, blendMode = androidx.compose.ui.graphics.BlendMode.Clear) }
                rotate(ang, limb.pivot) { clipPath(limb.patch) { drawPath(A.body.path, skin.tint(A.body.color)) } }
            }
            drawContext.canvas.restore()
            drawPath(A.belly.path, skin.tint(A.belly.color))

            // arm lines and the little shadow under each hand move with that hand
            for ((side, parts) in listOf(-1f to listOf(A.armL, A.shadeL), 1f to listOf(A.armR, A.shadeR))) {
                val limb = Limbs.first { it.hand && it.side == side }
                rotate(handAngle(side), limb.pivot) { parts.forEach { drawPath(it.path, skin.tint(it.color)) } }
            }

            // speaker: the cone pumps on the beat (and a little while he talks)
            val talkPump = if (still) 0f else pose.mouth * 0.5f * (0.5f + 0.5f * sin(t * 18f))
            val sc = A.ring1.bounds.center
            scale(1f + pump * 0.06f + talkPump * 0.03f, pivot = sc) { A.rings.forEach { drawPath(it.path, it.color) } }

            // eyes, layered like the SVG: the dark dome (socket) first, then the white half-eye and pupil on top.
            // Opening (happy, surprised) grows the white up into the dome; closing (sleepy, blink) covers it from the top.
            for (side in listOf(-1f, 1f)) {
                val white = if (side < 0) A.whiteL else A.whiteR
                val pupil = if (side < 0) A.pupilL else A.pupilR
                val lid = if (side < 0) A.lidL else A.lidR
                val wb = white.bounds
                val brow = if (side < 0) pose.browL else pose.browR
                val l = (pose.lid - (brow - 0.08f) * 0.6f).coerceIn(0f, 1f)
                val openK = if (l < 0.42f) (0.42f - l) / 0.42f else 0f
                val closeK = max(if (l > 0.42f) (l - 0.42f) / 0.58f else 0f, blink)
                scale(pose.eyeScale, pivot = wb.center) {
                    rotate(side * pose.browTilt * 0.5f, lid.bounds.center) { drawPath(lid.path, skin.tint(lid.color)) }
                    // the white grows upward from its bottom edge, staying inside the dome
                    val grow = 1f + openK * (lid.bounds.bottom - lid.bounds.top) / wb.height * 0.55f
                    val eyeArea = Path().apply { op(white.path, lid.path, androidx.compose.ui.graphics.PathOperation.Union) }
                    clipPath(eyeArea) {
                        scale(1f, grow, Offset(wb.center.x, wb.bottom)) {
                            drawPath(white.path, white.color)
                            clipPath(white.path) {
                                translate(pose.lookX * wb.width * 0.16f, pose.lookY * wb.height * 0.22f) { drawPath(pupil.path, pupil.color) }
                                if (closeK > 0.001f) drawRect(skin.tint(lid.color), Offset(wb.left - 1f, wb.top - 1f), Size(wb.width + 2f, (wb.height + 2f) * closeK))
                            }
                        }
                    }
                }
            }

            if (mood == LouMood.Sad && !still) {
                // one slow tear rolls down from the right eye
                val tp = (since % 2.2f) / 2.2f
                val tear = Offset(108f, 37f + tp * 16f)
                val a = (if (tp < 0.15f) tp / 0.15f else 1f - (tp - 0.15f) / 0.85f).coerceIn(0f, 1f)
                val drop = Path().apply { moveTo(tear.x, tear.y - 4.5f); cubicTo(tear.x + 3.2f, tear.y - 0.5f, tear.x + 3.2f, tear.y + 3f, tear.x, tear.y + 3f); cubicTo(tear.x - 3.2f, tear.y + 3f, tear.x - 3.2f, tear.y - 0.5f, tear.x, tear.y - 4.5f); close() }
                drawPath(drop, Color(0xFF7FD3FF).copy(alpha = a)); drawCircle(Color.White.copy(alpha = a * 0.8f), 0.9f, Offset(tear.x - 1f, tear.y))
            }
            drawNeck(outfit.neck, t)
            drawFace(outfit.face)
            drawHat(outfit.hat, t)

            // mouth: his own smile, widened or flattened by mood; opens into a round mouth when he talks
            val m = A.mouth; val mb = m.bounds; val mc = mb.center
            val open = (pose.mouth + pose.mouthExtra).coerceIn(0f, 1f)
            val openK = smooth(0.04f, 0.22f, open)
            val sx = (0.75f + pose.mouthW * 2.5f) * (1f - 0.3f * pose.thinkMouth)
            val sy = (pose.smile * (0.8f + pose.mouthW * 2f)) * (1f - 0.7f * pose.thinkMouth)
            if (openK < 0.99f) translate(pose.thinkMouth * 3f, 0f) {
                scale(sx, if (sy < 0f) sy.coerceAtMost(-0.15f) else sy.coerceAtLeast(0.15f), mc) { drawPath(m.path, m.color.copy(alpha = 1f - openK)) }
            }
            if (openK > 0.01f) {
                val mw = mb.width * sx * 0.78f
                val top = mb.top + mb.height * 0.2f
                val depth = 2.5f + open * 9f
                val shape = Path().apply {
                    moveTo(mc.x - mw / 2f, top)
                    quadraticTo(mc.x, top - 1.2f, mc.x + mw / 2f, top)
                    quadraticTo(mc.x + mw / 2f, top + depth * 1.15f, mc.x, top + depth)
                    quadraticTo(mc.x - mw / 2f, top + depth * 1.15f, mc.x - mw / 2f, top)
                    close()
                }
                drawPath(shape, m.color.copy(alpha = openK))
                clipPath(shape) { drawOval(Color(0xFFE86A7E).copy(alpha = openK), Offset(mc.x - mw * 0.28f, top + depth * 0.55f), Size(mw * 0.56f, depth)) }
            }

            if (mood == LouMood.Sleepy && !still) {
                val zp = (t % 2.4f) / 2.4f
                drawZ(Offset(140f + zp * 12f, 22f - zp * 24f), 7f * (0.6f + zp), bump(zp))
            }
        }
    }
}

private fun DrawScope.drawZ(at: Offset, s: Float, alpha: Float) {
    val p = Path().apply { moveTo(at.x, at.y); lineTo(at.x + s, at.y); lineTo(at.x, at.y + s); lineTo(at.x + s, at.y + s) }
    drawPath(p, LouArt.body.color.copy(alpha = alpha.coerceIn(0f, 1f)), style = Stroke(s * 0.22f, cap = StrokeCap.Round))
}
