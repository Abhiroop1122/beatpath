package com.beatpath.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import com.beatpath.app.R

/*
 * Open Sans (SIL OFL 1.1) — designed by Steve Matteson, who also drew Segoe, the face of
 * Windows Aero. Humanist, open and friendly without looking like Duolingo's rounded display type.
 * One variable file (wght 300–800, wdth 75–100); research/aero-lou-sound.md §2.
 */
@OptIn(ExperimentalTextApi::class)
private fun open(weight: Int, width: Float = 100f) = Font(
    R.font.opensans, FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.width(width)),
)

val OpenSans = FontFamily(open(400), open(500), open(600), open(700), open(800))
/** Condensed cut (87.5% width) for dense Studio labels. */
val OpenSansNarrow = FontFamily(open(500, 87.5f), open(600, 87.5f), open(700, 87.5f))

/** Aero palette (research/aero-lou-sound.md §1, contrast-checked). */
object AeroColors {
    val Ink = Color(0xFF0E2A3F)
    val Ink2 = Color(0xFF35546B)
    val Primary = Color(0xFF1F84D2)
    val PrimaryDeep = Color(0xFF0A548F)
    val Leaf = Color(0xFF25985A)
    val LeafDeep = Color(0xFF115E34)
    val Aqua = Color(0xFF3FD0E8)
    val Sun = Color(0xFFFFC93C)
    val Amber = Color(0xFFFFB020)
    val Rose = Color(0xFFD93A4A)
    val Lou = Color(0xFF1FA3A0)
}

object AeroType {
    private val base = TextStyle(fontFamily = OpenSans, color = AeroColors.Ink, textDirection = TextDirection.Content)
    val Hero = base.copy(fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = (-0.5).sp)
    val Title = base.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 29.sp)
    val H2 = base.copy(fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 24.sp)
    val Body = base.copy(fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp, color = AeroColors.Ink2)
    val BodyStrong = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp)
    val Label = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    val Small = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = AeroColors.Ink2)
    val Caps = base.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp, color = AeroColors.PrimaryDeep)
    val Button = base.copy(fontWeight = FontWeight.Bold, fontSize = 15.5.sp)
    val Studio = TextStyle(fontFamily = OpenSansNarrow, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFFE9EEF1))
}

@Composable
fun BeatpathTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary = AeroColors.Primary, onPrimary = Color.White, background = Color(0xFFE9F6FF), surface = Color.White, onSurface = AeroColors.Ink),
        typography = androidx.compose.material3.Typography().let { t ->
            t.copy(bodyLarge = t.bodyLarge.copy(fontFamily = OpenSans), bodyMedium = t.bodyMedium.copy(fontFamily = OpenSans), titleLarge = t.titleLarge.copy(fontFamily = OpenSans),
                labelLarge = t.labelLarge.copy(fontFamily = OpenSans), headlineSmall = t.headlineSmall.copy(fontFamily = OpenSans))
        },
        content = content,
    )
}
