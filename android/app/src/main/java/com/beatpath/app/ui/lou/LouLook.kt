package com.beatpath.app.ui.lou

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.sin

/** What Lou is wearing: a hat, glasses and a body color, bought with XP in Lou's Shop. */
data class LouOutfit(val hat: String? = null, val face: String? = null, val skin: String? = null, val neck: String? = null)

/** The outfit every Lou on screen wears. The Store sets it at start and whenever you equip something. */
object LouLook {
    var outfit by mutableStateOf(LouOutfit())
    /** Menu sky theme (null = the normal blue sky). */
    var sky by mutableStateOf<String?>(null)
}

internal class Skin(val body: Color, val belly: Color, val dark: Color)

private val BODY = Color(0xFF178888); private val BELLY = Color(0xFF39B4A7); private val DARK = Color(0xFF157575)

internal fun skinOf(id: String?, t: Float): Skin? = when (id) {
    "skin.coral" -> Skin(Color(0xFFE0605A), Color(0xFFFF9A8B), Color(0xFFB8443F))
    "skin.grape" -> Skin(Color(0xFF7B4FC9), Color(0xFFA98BE8), Color(0xFF5E37A3))
    "skin.sunny" -> Skin(Color(0xFFE39A17), Color(0xFFFFD15C), Color(0xFFB97A0B))
    "skin.midnight" -> Skin(Color(0xFF2E4270), Color(0xFF5B76B0), Color(0xFF1D2A47))
    "skin.mint" -> Skin(Color(0xFF2FAE86), Color(0xFF7FE0BE), Color(0xFF1F8466))
    "skin.bubblegum" -> Skin(Color(0xFFE5679F), Color(0xFFFFA8CB), Color(0xFFB84779))
    "skin.rainbow" -> { val h = (t * 40f) % 360f; Skin(Color.hsv(h, 0.62f, 0.6f), Color.hsv((h + 25f) % 360f, 0.45f, 0.82f), Color.hsv(h, 0.65f, 0.48f)) }
    else -> null
}

/** Swap the artwork's teal for the outfit's color; everything else keeps its own color. */
internal fun Skin?.tint(c: Color): Color = if (this == null) c else when (c) { BODY -> body; BELLY -> belly; DARK -> dark; else -> c }

// Head geometry in Lou's 171 × 144 SVG units: the dome's top is near (86, 4); the eyes sit at (71, 29) and (104, 29).
private const val HX = 86.5f

/** Hats sit on the dome, drawn after the body so they move, squash and tilt with him. */
internal fun DrawScope.drawHat(id: String?, t: Float) {
    when (id) {
        "hat.party" -> rotate(-8f, Offset(HX, 8f)) {
            val cone = Path().apply { moveTo(HX - 17f, 9f); lineTo(HX + 17f, 9f); lineTo(HX + 1f, -26f); close() }
            drawPath(cone, Brush.verticalGradient(listOf(Color(0xFFFF8FB1), Color(0xFFE0457B)), -26f, 9f))
            listOf(0.25f, 0.55f, 0.82f).forEachIndexed { i, k ->
                val y = 9f - 35f * k; val half = 17f * (1f - k)
                drawLine(listOf(Color(0xFFFFE26A), Color(0xFF7ED3FF), Color(0xFFB4F07A))[i], Offset(HX - half + 1f, y), Offset(HX + half + 1f, y), 2.4f, StrokeCap.Round)
            }
            drawCircle(Color(0xFFFFE26A), 4.5f + sin(t * 5f) * 0.4f, Offset(HX + 1f, -27f))
            drawOval(Color(0x33000000), Offset(HX - 18f, 7.5f), Size(36f, 4f))
        }
        "hat.beanie" -> {
            val dome = Path().apply { moveTo(HX - 41f, 16f); cubicTo(HX - 38f, -12f, HX + 38f, -12f, HX + 41f, 16f); close() }
            drawPath(dome, Brush.verticalGradient(listOf(Color(0xFFFF7A59), Color(0xFFD94A2B)), -8f, 16f))
            for (k in -3..3) drawLine(Color(0x33000000), Offset(HX + k * 10f, 14f), Offset(HX + k * 8f, -2f + kotlin.math.abs(k) * 2f), 1.2f)
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFFFB199), Color(0xFFE8663F)), 11f, 22f), Offset(HX - 43f, 11f), Size(86f, 10f), CornerRadius(5f))
            drawCircle(Color(0xFFFFF1E8), 7f, Offset(HX, -6f)); drawCircle(Color(0x22000000), 7f, Offset(HX, -6f), style = Stroke(1f))
        }
        "hat.headphones" -> {
            drawArc(Color(0xFF2B3340), 195f, 150f, false, Offset(HX - 50f, -14f), Size(100f, 70f), style = Stroke(7f, cap = StrokeCap.Round))
            drawArc(Color(0x55FFFFFF), 205f, 60f, false, Offset(HX - 50f, -14f), Size(100f, 70f), style = Stroke(2f, cap = StrokeCap.Round))
            for (side in listOf(-1f, 1f)) {
                val x = HX + side * 49f
                drawRoundRect(Color(0xFF2B3340), Offset(x - 8f, 12f), Size(16f, 26f), CornerRadius(7f))
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF6CC4F2), Color(0xFF2E9BE0)), 14f, 36f), Offset(x - 5.5f, 14.5f), Size(11f, 21f), CornerRadius(5f))
            }
        }
        "hat.chef" -> {
            drawRoundRect(Color(0xFFF3F6F8), Offset(HX - 22f, -2f), Size(44f, 14f), CornerRadius(3f))
            for ((dx, rr) in listOf(-16f to 12f, 0f to 15f, 16f to 12f)) drawCircle(Color.White, rr, Offset(HX + dx, -10f))
            drawCircle(Color(0x14000000), 15f, Offset(HX, -10f), style = Stroke(1f))
            drawLine(Color(0x22000000), Offset(HX - 21f, 8f), Offset(HX + 21f, 8f), 1.4f)
        }
        "hat.wizard" -> rotate(6f, Offset(HX, 8f)) {
            drawOval(Color(0xFF4B2E9E), Offset(HX - 32f, 3f), Size(64f, 12f))
            val cone = Path().apply { moveTo(HX - 20f, 8f); lineTo(HX + 20f, 8f); quadraticTo(HX + 8f, -14f, HX + 22f, -34f); quadraticTo(HX - 6f, -18f, HX - 20f, 8f); close() }
            drawPath(cone, Brush.verticalGradient(listOf(Color(0xFF8A63D8), Color(0xFF5F3FA8)), -34f, 8f))
            val tw = 0.6f + 0.4f * sin(t * 4f)
            drawStar(Offset(HX - 4f, -6f), 4.5f, Color(0xFFFFE26A).copy(alpha = tw)); drawStar(Offset(HX + 8f, -20f), 3f, Color(0xFFFFE26A).copy(alpha = 1f - tw * 0.5f))
            drawCircle(Color(0xFFFFE26A), 3.5f, Offset(HX + 22f, -34f))
        }
        "hat.cap" -> {
            val dome = Path().apply { moveTo(HX - 36f, 14f); cubicTo(HX - 34f, -8f, HX + 34f, -8f, HX + 36f, 14f); close() }
            drawPath(dome, Brush.verticalGradient(listOf(Color(0xFF5AB0F0), Color(0xFF1E78C8)), -4f, 14f))
            drawRoundRect(Color(0xFF155E9C), Offset(HX + 10f, 9f), Size(44f, 8f), CornerRadius(4f))
            drawCircle(Color(0xFF155E9C), 3.5f, Offset(HX, -2.5f))
            drawCircle(Color.White, 5.5f, Offset(HX - 12f, 6f)); drawCircle(Color(0xFFE53935), 3f, Offset(HX - 12f, 6f))
        }
        "hat.flowers" -> {
            drawArc(Color(0xFF43A047), 190f, 160f, false, Offset(HX - 40f, 2f), Size(80f, 30f), style = Stroke(3f))
            val cols = listOf(Color(0xFFFF8FB1), Color(0xFFFFE26A), Color(0xFF7ED3FF), Color(0xFFFFB199), Color(0xFFB79CF2))
            for (i in 0 until 7) {
                val a = Math.toRadians(195.0 + i * 25.0); val c = Offset(HX + 40f * kotlin.math.cos(a).toFloat(), 17f + 15f * kotlin.math.sin(a).toFloat())
                for (k in 0 until 5) { val b = Math.toRadians(k * 72.0 + t * 20); drawCircle(cols[i % cols.size], 3.2f, c + Offset(3.4f * kotlin.math.cos(b).toFloat(), 3.4f * kotlin.math.sin(b).toFloat())) }
                drawCircle(Color(0xFFFFF59D), 2.2f, c)
            }
        }
        "hat.crown" -> {
            val c = Path().apply {
                moveTo(HX - 20f, 10f); lineTo(HX - 22f, -10f); lineTo(HX - 10f, 0f); lineTo(HX, -16f); lineTo(HX + 10f, 0f); lineTo(HX + 22f, -10f); lineTo(HX + 20f, 10f); close()
            }
            drawPath(c, Brush.verticalGradient(listOf(Color(0xFFFFE27A), Color(0xFFE9A21C)), -16f, 10f))
            drawPath(c, Color(0xFFB97A0B), style = Stroke(1.2f))
            drawRect(Color(0x55FFFFFF), Offset(HX - 19f, 3f), Size(38f, 2.5f))
            val glint = 0.5f + 0.5f * sin(t * 3f)
            drawCircle(Color(0xFFE53935), 3f, Offset(HX, 4f)); drawCircle(Color(0xFF1E88E5), 2.4f, Offset(HX - 12f, 5f)); drawCircle(Color(0xFF43A047), 2.4f, Offset(HX + 12f, 5f))
            drawCircle(Color.White.copy(alpha = glint), 1.6f, Offset(HX - 18f, -8f)); drawCircle(Color.White.copy(alpha = 1f - glint), 1.6f, Offset(HX + 18f, -8f))
        }
    }
}

/** Glasses sit over the eyes, after the eyes are drawn. */
internal fun DrawScope.drawFace(id: String?) {
    when (id) {
        "face.shades" -> {
            for (cx in listOf(71f, 104f)) {
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF3A4452), Color(0xFF10151B)), 22f, 37f), Offset(cx - 13f, 22f), Size(26f, 15f), CornerRadius(6f))
                drawLine(Color(0x88FFFFFF), Offset(cx - 8f, 25f), Offset(cx - 2f, 25f), 1.6f, StrokeCap.Round)
            }
            drawLine(Color(0xFF10151B), Offset(84f, 26f), Offset(91f, 26f), 2.2f)
            drawLine(Color(0xFF10151B), Offset(58f, 25f), Offset(52f, 23f), 2f); drawLine(Color(0xFF10151B), Offset(117f, 25f), Offset(123f, 23f), 2f)
        }
        "face.stars" -> {
            for (cx in listOf(71f, 104f)) { drawStar(Offset(cx, 29f), 13f, Color(0xEEFFC93C)); drawStar(Offset(cx, 29f), 8f, Color(0x66FFFFFF)) }
            drawLine(Color(0xFFB98500), Offset(84f, 27f), Offset(91f, 27f), 2f)
        }
        "face.monocle" -> {
            drawCircle(Color(0x33BFE6FF), 11f, Offset(104f, 29f)); drawCircle(Color(0xFFB98500), 11f, Offset(104f, 29f), style = Stroke(2.2f))
            drawLine(Color(0x88FFFFFF), Offset(98f, 24f), Offset(101f, 22f), 1.6f, StrokeCap.Round)
            val chain = Path().apply { moveTo(114f, 33f); quadraticTo(126f, 50f, 118f, 66f) }
            drawPath(chain, Color(0xFFB98500), style = Stroke(1.2f))
        }
        "face.mustache" -> {
            val m = Path().apply {
                moveTo(86f, 38f); cubicTo(80f, 34f, 70f, 34f, 66f, 41f); cubicTo(71f, 39f, 76f, 41f, 79f, 42f); cubicTo(83f, 43f, 85f, 41f, 86f, 40f)
                cubicTo(87f, 41f, 89f, 43f, 93f, 42f); cubicTo(96f, 41f, 101f, 39f, 106f, 41f); cubicTo(102f, 34f, 92f, 34f, 86f, 38f); close()
            }
            drawPath(m, Color(0xFF4A3426))
        }
        "face.hearts" -> {
            for (cx in listOf(71f, 104f)) {
                val h = Path().apply {
                    moveTo(cx, 38f)
                    cubicTo(cx - 16f, 29f, cx - 13f, 18f, cx - 6.5f, 19f); cubicTo(cx - 3f, 19.5f, cx - 1f, 21f, cx, 24f)
                    cubicTo(cx + 1f, 21f, cx + 3f, 19.5f, cx + 6.5f, 19f); cubicTo(cx + 13f, 18f, cx + 16f, 29f, cx, 38f); close()
                }
                drawPath(h, Brush.verticalGradient(listOf(Color(0xEEFF6F9C), Color(0xEED81B60)), 19f, 38f))
                drawCircle(Color(0x99FFFFFF), 2f, Offset(cx - 6f, 24f))
            }
            drawLine(Color(0xFFD81B60), Offset(84f, 25f), Offset(91f, 25f), 2f)
        }
    }
}

/** Extras around Lou's middle, drawn after the belly and speaker so they sit on top. */
internal fun DrawScope.drawNeck(id: String?, t: Float) {
    when (id) {
        "neck.bowtie" -> {
            val c = Offset(HX, 55f)
            val l = Path().apply { moveTo(c.x, c.y); lineTo(c.x - 13f, c.y - 7f); lineTo(c.x - 13f, c.y + 7f); close() }
            val r = Path().apply { moveTo(c.x, c.y); lineTo(c.x + 13f, c.y - 7f); lineTo(c.x + 13f, c.y + 7f); close() }
            drawPath(l, Color(0xFFE53935)); drawPath(r, Color(0xFFE53935)); drawCircle(Color(0xFFB71C1C), 3.5f, c)
            drawCircle(Color(0x55FFFFFF), 1.6f, c + Offset(-8f, -2f))
        }
        "neck.scarf" -> {
            drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFFFF7A59), Color(0xFFFFB199), Color(0xFFFF7A59))), Offset(HX - 40f, 50f), Size(80f, 11f), CornerRadius(5.5f))
            for (k in 0 until 6) drawLine(Color(0x55FFFFFF), Offset(HX - 34f + k * 13f, 50f), Offset(HX - 30f + k * 13f, 61f), 2f)
            val sway = sin(t * 2f) * 2f
            drawRoundRect(Color(0xFFE8663F), Offset(HX + 16f + sway, 58f), Size(11f, 24f), CornerRadius(4f))
        }
        "neck.chain" -> {
            val chain = Path().apply { moveTo(HX - 26f, 50f); quadraticTo(HX, 74f, HX + 26f, 50f) }
            drawPath(chain, Color(0xFFE9A21C), style = Stroke(2.6f))
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF1A8), Color(0xFFE9A21C)), Offset(HX - 2f, 60f), 7f), 6.5f, Offset(HX, 62f))
            val glint = 0.5f + 0.5f * sin(t * 3f)
            drawCircle(Color.White.copy(alpha = glint), 1.4f, Offset(HX - 2.5f, 59.5f))
        }
    }
}

private fun DrawScope.drawStar(c: Offset, r: Float, color: Color) {
    val p = Path()
    for (i in 0 until 10) {
        val a = Math.toRadians(-90.0 + i * 36.0); val rr = if (i % 2 == 0) r else r * 0.45f
        val pt = Offset(c.x + rr * kotlin.math.cos(a).toFloat(), c.y + rr * kotlin.math.sin(a).toFloat())
        if (i == 0) p.moveTo(pt.x, pt.y) else p.lineTo(pt.x, pt.y)
    }
    p.close(); drawPath(p, color)
}
