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
data class LouOutfit(val hat: String? = null, val face: String? = null, val skin: String? = null)

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
