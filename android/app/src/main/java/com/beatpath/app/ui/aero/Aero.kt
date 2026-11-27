package com.beatpath.app.ui.aero

import androidx.compose.runtime.setValue
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.composed
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.platform.LocalConfiguration
import com.beatpath.app.ui.theme.AeroType

/** Frutiger Aero palette: sky, water, fresh grass, glass and white. Text always sits on solid-enough glass. */
object Aero {
    val SkyTop = Color(0xFF2E9BE6)
    val SkyMid = Color(0xFF8FD3FF)
    val SkyLow = Color(0xFFE9F8FF)
    val Grass = Color(0xFF7ACC3A)
    val GrassDeep = Color(0xFF3E9A1F)
    val Aqua = Color(0xFF1FB4D8)
    val AquaDeep = Color(0xFF0B79A8)
    val Ink = Color(0xFF0C2B40)        // body text on glass (contrast ≥ 7:1 on white glass)
    val Ink2 = Color(0xFF3D5A6E)
    val Glass = Color(0xD9FFFFFF)      // 85% white
    val GlassEdge = Color(0xFFFFFFFF)
    val Shadow = Color(0x330B4A70)
    val Good = Color(0xFF2FA84F)
    val GoodInk = Color(0xFF145C28)
    val Warn = Color(0xFFF2A51A)
    val WarnInk = Color(0xFF6B4300)
    val Orange = Color(0xFFFF8A1F)
}

/** A blurred copy of the screen's background, aligned the same way, so glass can show a real frosted backdrop. */
class Frost(val image: androidx.compose.ui.graphics.ImageBitmap, val biasX: Float)
val LocalFrost = androidx.compose.runtime.staticCompositionLocalOf<Frost?> { null }

/** Draws the blurred background exactly as it sits behind this element (needs [pos] and [root] from layout). */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFrost(f: Frost, pos: Offset, root: androidx.compose.ui.unit.IntSize) {
    val iw = f.image.width; val ih = f.image.height
    val scale = maxOf(root.width / iw.toFloat(), root.height / ih.toFloat())
    val dw = iw * scale; val dh = ih * scale
    val ox = (root.width - dw) / 2f * (1f + f.biasX); val oy = (root.height - dh) / 2f
    drawImage(f.image, androidx.compose.ui.unit.IntOffset.Zero, androidx.compose.ui.unit.IntSize(iw, ih),
        androidx.compose.ui.unit.IntOffset(kotlin.math.round(ox - pos.x).toInt(), kotlin.math.round(oy - pos.y).toInt()),
        androidx.compose.ui.unit.IntSize(kotlin.math.round(dw).toInt(), kotlin.math.round(dh).toInt()), filterQuality = androidx.compose.ui.graphics.FilterQuality.Low)
}

/**
 * Frosted backdrop: on the menu it paints the blurred artwork behind the element (true frosted glass);
 * elsewhere it does nothing and the caller's translucent fill carries the look.
 */
fun Modifier.frost(): Modifier = composed {
    val f = LocalFrost.current ?: return@composed this
    var pos by remember { androidx.compose.runtime.mutableStateOf(Offset.Zero) }
    var root by remember { androidx.compose.runtime.mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    this.onGloballyPositioned { c -> pos = c.positionInRoot(); root = c.findRootCoordinates().size }
        .drawBehind { if (root.width > 0) drawFrost(f, pos, root) }
}

/** True when the screen is wider than tall (phone turned sideways). Layouts switch to side-by-side. */
@Composable
fun isLandscape(): Boolean = LocalConfiguration.current.let { it.screenWidthDp > it.screenHeightDp }

/** Keeps content clear of the camera cutout and side navigation bar when the phone is sideways. */
@Composable
fun Modifier.sideSafe(): Modifier = this.windowInsetsPadding(WindowInsets.displayCutout.union(WindowInsets.navigationBars).only(WindowInsetsSides.Horizontal))

private class Bubble(val x: Float, val y: Float, val r: Float, val rises: Int, val sways: Int, val phase: Float)

// fixed set so every screen feels the same; rises/sways are whole numbers so the 60 s loop is seamless
private val bubbleSet = listOf(
    Bubble(0.10f, 0.05f, 0.050f, 1, 2, 0.00f), Bubble(0.84f, 0.18f, 0.070f, 1, 1, 0.30f), Bubble(0.66f, 0.42f, 0.040f, 2, 3, 0.55f),
    Bubble(0.20f, 0.55f, 0.075f, 1, 2, 0.15f), Bubble(0.92f, 0.70f, 0.045f, 2, 2, 0.80f), Bubble(0.38f, 0.86f, 0.055f, 1, 1, 0.45f),
    Bubble(0.52f, 0.28f, 0.030f, 2, 3, 0.65f), Bubble(0.04f, 0.78f, 0.035f, 2, 1, 0.90f), Bubble(0.74f, 0.95f, 0.060f, 1, 2, 0.25f),
)

/** App-wide motion setting (Me → Calm motion). */
object Motion { var reduce by androidx.compose.runtime.mutableStateOf(false) }

/** Glassy bubbles that drift slowly upward with a gentle sway, looping forever (one rise takes 30–60 s). */
@Composable
fun FloatingBubbles(modifier: Modifier = Modifier) {
    if (Motion.reduce) return
    val t by androidx.compose.animation.core.rememberInfiniteTransition(label = "bubbles").animateFloat(0f, 1f,
        androidx.compose.animation.core.infiniteRepeatable<Float>(androidx.compose.animation.core.tween(60_000, easing = androidx.compose.animation.core.LinearEasing)), label = "t")
    Canvas(modifier.fillMaxSize()) {
        val unit = minOf(size.width, size.height)
        bubbleSet.forEach { b ->
            val r = unit * b.r
            // rise: wraps from just below the bottom edge to just above the top edge
            val span = 1f + 2.4f * b.r * unit / size.height
            val yy = (b.y + t * b.rises) % 1f
            val cy = size.height * (yy * span) - r * 1.2f
            val cx = size.width * b.x + unit * 0.035f * kotlin.math.sin(2f * Math.PI.toFloat() * (t * b.sways + b.phase)).toFloat()
            val c = Offset(cx, size.height + r * 1.2f - (cy + r * 1.2f))
            drawCircle(Brush.radialGradient(listOf(Color(0x00FFFFFF), Color(0x38FFFFFF)), c, r), r, c)
            drawCircle(Color(0x66FFFFFF), r, c, style = Stroke(1.3f))
            drawCircle(Color(0x99FFFFFF), r * 0.22f, c + Offset(-r * 0.4f, -r * 0.4f))
        }
    }
}

/** Sky themes bought in Lou's Shop: a color wash over the menu art, and stars for dusk. */
@Composable
fun SkyTint(sky: String?, modifier: Modifier = Modifier.fillMaxSize()) {
    when (sky) {
        "sky.sunset" -> Box(modifier.drawBehind {
            drawRect(Brush.verticalGradient(0f to Color(0x8CFF8A65), 0.5f to Color(0x59FFB74D), 1f to Color(0x40FFD180)))
        })
        "sky.dusk" -> {
            val tw by androidx.compose.animation.core.rememberInfiniteTransition(label = "stars").animateFloat(0f, 1f,
                androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(2600), androidx.compose.animation.core.RepeatMode.Reverse), label = "tw")
            Canvas(modifier) {
                drawRect(Brush.verticalGradient(0f to Color(0x997B5FC7), 0.6f to Color(0x668E7AD6), 1f to Color(0x40B39DDB)))
                val rnd = java.util.Random(11)
                repeat(36) { i ->
                    val x = rnd.nextFloat() * size.width; val y = rnd.nextFloat() * size.height * 0.6f
                    val a = (0.35f + 0.65f * (if (i % 2 == 0) tw else 1f - tw)) * (0.5f + rnd.nextFloat() * 0.5f)
                    drawCircle(Color.White.copy(alpha = a), 1.2f + rnd.nextFloat() * 2.2f, Offset(x, y))
                }
            }
        }
    }
}

/** Sky gradient with soft light bokeh bubbles — the classic Aero backdrop, kept calm so text stays readable. */
@Composable
fun AeroBackground(modifier: Modifier = Modifier, photo: Boolean = false, content: @Composable BoxScope.() -> Unit) {
    if (photo) {
        val blurred = androidx.compose.ui.graphics.ImageBitmap.imageResource(com.beatpath.app.R.drawable.menu_sky_blur)
        // main menu: the sky-blue ripple artwork, cropped to fit any screen shape
        Box(modifier.fillMaxSize()) {
            androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(com.beatpath.app.R.drawable.menu_sky), null, Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop, alignment = androidx.compose.ui.BiasAlignment(0.45f, 0f))
            // soft white veil at the top keeps headings crisp
            Box(Modifier.fillMaxSize().drawBehind { drawRect(Brush.verticalGradient(0f to Color(0x40FFFFFF), 0.35f to Color(0x00FFFFFF))) })
            FloatingBubbles()
            androidx.compose.runtime.CompositionLocalProvider(LocalFrost provides Frost(blurred, 0.45f)) { content() }
        }
        return
    }
    Box(modifier.fillMaxSize().drawBehind {
        drawRect(Brush.verticalGradient(listOf(Aero.SkyTop, Aero.SkyMid, Aero.SkyLow, Color(0xFFF4FFF0))))
        // grass hill glow at the bottom
        drawOval(Brush.radialGradient(listOf(Color(0x557ACC3A), Color(0x007ACC3A)), Offset(size.width * 0.5f, size.height * 1.05f), size.width * 0.9f),
            Offset(-size.width * 0.4f, size.height * 0.72f), Size(size.width * 1.8f, size.height * 0.6f))
    }) {
        FloatingBubbles()
        content()
    }
}

/** Panels are soft clay cards now (the name is kept so every screen picked up the new look at once). */
@Suppress("UNUSED_PARAMETER")
fun Modifier.glass(corner: Dp = 22.dp, tint: Color = Aero.Glass): Modifier = this.clayCard(corner)

/** Clay colors: [top] the lit face, [bottom] the body, [edge] the darker base the button sits on, [text] the label. */
enum class Gel(val top: Color, val bottom: Color, val edge: Color, val text: Color) {
    Aqua(Color(0xFF6CC4F2), Color(0xFF2E9BE0), Color(0xFF1A6FAE), Color.White),
    Grass(Color(0xFF8FD64F), Color(0xFF62B826), Color(0xFF428C14), Color.White),
    Orange(Color(0xFFFFAA55), Color(0xFFF2852B), Color(0xFFC25F12), Color.White),
    Silver(Color(0xFFFFFFFF), Color(0xFFF1F5F8), Color(0xFFC6D3DC), Color(0xFF0C2B40)),
    Off(Color(0xFFE8ECEF), Color(0xFFDCE2E6), Color(0xFFB9C3CA), Color(0xFF7C8A94)),
    /** Cards and bubbles: warm white clay. */
    Cream(Color(0xFFFFFFFF), Color(0xFFF3F8FB), Color(0xFFD5E1E9), Color(0xFF0E2A3F)),
    /** Studio controls: dark clay, like FL's panels. */
    Slate(Color(0xFF5B6B77), Color(0xFF424F59), Color(0xFF232C33), Color(0xFFE9EEF1)),
    /** Studio "on" state: FL's lime, in clay. */
    Lime(Color(0xFFD2F38B), Color(0xFFA9E34B), Color(0xFF6E9A22), Color(0xFF1F2E08)),
    Pink(Color(0xFFFF9EC4), Color(0xFFEF5B9C), Color(0xFFB83A72), Color.White),
    Purple(Color(0xFFB79CF2), Color(0xFF8A63D8), Color(0xFF5F3FA8), Color.White),
    Gold(Color(0xFFFFE08A), Color(0xFFF4B731), Color(0xFFB8820F), Color(0xFF4A3200)),
}

/** A fine, fixed grain that makes flat color read as soft clay. Built once. */
private val clayGrain: androidx.compose.ui.graphics.ImageBitmap by lazy {
    val n = 96; val rnd = java.util.Random(7)
    val px = IntArray(n * n) { val v = rnd.nextInt(256); val a = 10 + rnd.nextInt(16); (a shl 24) or (v shl 16) or (v shl 8) or v }
    android.graphics.Bitmap.createBitmap(px, n, n, android.graphics.Bitmap.Config.ARGB_8888).asImageBitmap()
}
internal val clayGrainBrush by lazy { androidx.compose.ui.graphics.ShaderBrush(androidx.compose.ui.graphics.ImageShader(clayGrain, androidx.compose.ui.graphics.TileMode.Repeated, androidx.compose.ui.graphics.TileMode.Repeated)) }

/** Clay button: sits on its own darker base and squishes down onto it when pressed. */
@Composable
fun GelButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, gel: Gel = Gel.Aqua, enabled: Boolean = true, height: Dp = 46.dp) {
    val g = if (enabled) gel else Gel.Off
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, spring(dampingRatio = 0.55f, stiffness = 900f), label = "clay")
    Box(
        modifier.height(height + 4.dp)
            .graphicsLayer { scaleX = 1f - press * 0.025f; scaleY = 1f - press * 0.04f; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f) }
            .claySurface(g, height / 2, depth = 4.dp, press = if (enabled) press else 0.5f)
            .clip(RoundedCornerShape(50))
            .clickable(src, null, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // one line always: long labels shrink a little instead of wrapping and getting clipped
        val st = AeroType.Button.copy(color = g.text, shadow = if (g.text == Color.White) androidx.compose.ui.graphics.Shadow(Color(0x40000000), Offset(0f, 1.5f), 2f) else null)
        androidx.compose.foundation.text.BasicText(text, style = st, maxLines = 1, softWrap = false,
            autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = st.fontSize, stepSize = 0.5.sp),
            modifier = Modifier.padding(start = if (height < 44.dp) 10.dp else 16.dp, end = if (height < 44.dp) 10.dp else 16.dp, bottom = 4.dp)
                .graphicsLayer { translationY = 4.dp.toPx() - (1f - press) * 4.dp.toPx() * (if (enabled) 1f else 0.5f) })
    }
}

/** Round clay orb (Lou button, section badges). */
@Composable
fun GelOrb(modifier: Modifier, gel: Gel, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.drawBehind {
        val c = center; val r = size.minDimension / 2
        drawCircle(Color(0x2A083A5A), r, c + Offset(0f, 4.dp.toPx()))
        drawCircle(gel.edge, r, c + Offset(0f, 2.dp.toPx()))
        drawCircle(Brush.radialGradient(listOf(gel.top, gel.bottom), c - Offset(r * 0.3f, r * 0.4f), r * 1.5f), r * 0.97f, c)
        drawCircle(clayGrainBrush, r * 0.97f, c)
        drawOval(Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x00FFFFFF)), c.y - r, c.y), Offset(c.x - r * 0.6f, c.y - r * 0.86f), Size(r * 1.2f, r * 0.75f))
    }, contentAlignment = Alignment.Center, content = content)
}

@Composable
fun Sparkle(modifier: Modifier) { Canvas(modifier) { drawCircle(Color(0x99FFFFFF), size.minDimension / 2) } }
