package com.beatpath.app.ui.lesson

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import com.beatpath.app.lessons.Basics
import com.beatpath.app.model.NoteColors
import com.beatpath.app.ui.theme.AeroColors
import com.beatpath.app.ui.theme.OpenSans

private val Gold = Color(0xFFFFC93C)
private val GoldDeep = Color(0xFFB98500)

private fun DrawScope.centeredText(m: TextMeasurer, text: String, cx: Float, y: Float, style: TextStyle) {
    val l = m.measure(text, style)
    drawText(l, topLeft = Offset(cx - l.size.width / 2f, y))
}

/**
 * A tappable keyboard for Music basics. White keys can wear their rainbow color, glowing keys say "look here",
 * arcs and tags explain distances, and picked keys light up in their note color.
 */
@Composable
fun RainbowKeys(
    view: Basics.KeyView,
    picked: Set<Int>,
    sounding: Set<Int>,
    hintKeys: Set<Int>,
    wrong: Int?,
    modifier: Modifier,
    onKey: (Int) -> Unit,
) {
    val measurer = rememberTextMeasurer()
    val whites = view.range.filter { !Basics.isBlack(it) }
    val head = view.tags.isNotEmpty() || view.arcs.isNotEmpty()
    Canvas(modifier
        .semantics { contentDescription = "Keyboard from ${Basics.name(view.range.first)} to ${Basics.name(view.range.last)}. Picked: ${picked.sorted().joinToString { Basics.bothNames(it) }.ifEmpty { "none" }}" }
        .pointerInput(view.range, head) {
            // keys sound the instant a finger touches them, like a real piano (not when it lifts)
            detectTapGestures(onPress = { o ->
                val top = if (head) 40.dp2px(density) else 4.dp2px(density)
                if (o.y < top) return@detectTapGestures
                val ww = size.width / whites.size.toFloat(); val kh = size.height - top
                // black keys sit on top, so test them first
                val black = view.range.filter { Basics.isBlack(it) }.firstOrNull { k ->
                    val x = blackX(k, whites, ww); o.x in x..(x + ww * 0.62f) && o.y < top + kh * 0.6f
                }
                val key = black ?: whites.getOrNull((o.x / ww).toInt().coerceIn(0, whites.size - 1))
                key?.let(onKey)
            })
        }) {
        val top = if (head) 40.dp2px(density) else 4.dp2px(density)
        val ww = size.width / whites.size; val kh = size.height - top
        val r = CornerRadius(6.dp2px(density))
        fun state(k: Int) = when { k == wrong -> 3; k in picked || k in sounding -> 2; k in hintKeys -> 4; k in view.lit -> 1; else -> 0 }
        // white keys
        whites.forEachIndexed { i, k ->
            val x = i * ww; val c = NoteColors.of(k); val st = state(k)
            val fill = when {
                st == 3 -> Color(0xFFFFD9A0)
                st == 2 -> if (view.rainbow) c else AeroColors.Primary
                view.rainbow -> lerp(Color.White, c, 0.16f)
                else -> Color.White
            }
            drawRoundRect(Brush.verticalGradient(listOf(fill, lerp(fill, Color(0xFFDCE3E8), 0.35f)), top, top + kh), Offset(x + 1f, top), Size(ww - 2f, kh), r)
            if (view.rainbow && st != 2) drawRoundRect(c, Offset(x + 3f, top + kh * 0.72f), Size(ww - 6f, kh * 0.28f - 3f), r)
            if (st == 1) { drawRoundRect(Gold.copy(alpha = if (view.rainbow) 0.16f else 0.35f), Offset(x + 1f, top), Size(ww - 2f, kh), r); drawRoundRect(GoldDeep, Offset(x + 2f, top + 1f), Size(ww - 4f, kh - 2f), r, style = Stroke(3.dp2px(density))) }
            if (st == 4) drawRoundRect(GoldDeep, Offset(x + 3f, top + 2f), Size(ww - 6f, kh - 4f), r, style = Stroke(3.dp2px(density), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))))
            drawRoundRect(Color(0xFF9AA8B3), Offset(x + 1f, top), Size(ww - 2f, kh), r, style = Stroke(1f))
            if (view.letters || (view.octaves && k % 12 == 0)) {
                val label = if (view.octaves && k % 12 == 0) "C${k / 12}" else Basics.name(k)
                val ink = when { st == 2 && view.rainbow -> NoteColors.ink(k); st == 2 -> Color.White; view.rainbow -> NoteColors.ink(k); else -> AeroColors.Ink }
                centeredText(measurer, label, x + ww / 2, top + kh - 24.dp2px(density),
                    TextStyle(fontFamily = OpenSans, fontWeight = FontWeight.Bold, fontSize = if (label.length > 1) 12.sp else 15.sp, color = ink))
            }
        }
        // black keys
        view.range.filter { Basics.isBlack(it) }.forEach { k ->
            val x = blackX(k, whites, ww); val bw = ww * 0.62f; val bh = kh * 0.6f; val st = state(k); val c = NoteColors.of(k)
            val fill = when (st) { 3 -> Color(0xFFE39A2C); 2 -> if (view.rainbow) c else AeroColors.Primary; else -> Color(0xFF1C2228) }
            drawRoundRect(Brush.verticalGradient(listOf(lerp(fill, Color.White, 0.12f), fill), top, top + bh), Offset(x, top), Size(bw, bh), CornerRadius(4.dp2px(density)))
            if (view.rainbow && st < 2) drawRoundRect(c, Offset(x + 2f, top + bh * 0.8f), Size(bw - 4f, bh * 0.2f - 2f), CornerRadius(3f))
            if (st == 1) drawRoundRect(Gold, Offset(x - 1f, top - 1f), Size(bw + 2f, bh + 2f), CornerRadius(5f), style = Stroke(3.dp2px(density)))
            if (st == 4) drawRoundRect(Gold, Offset(x - 1f, top - 1f), Size(bw + 2f, bh + 2f), CornerRadius(5f), style = Stroke(3.dp2px(density), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))))
            if (view.sharps) {
                val n = Basics.name(k)
                centeredText(measurer, n, x + bw / 2, top + bh * 0.42f, TextStyle(fontFamily = OpenSans, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White))
            }
        }
        // tags and arcs above the keys
        fun cx(k: Int) = if (Basics.isBlack(k)) blackX(k, whites, ww) + ww * 0.31f else whites.indexOf(k) * ww + ww / 2
        val tagStyle = TextStyle(fontFamily = OpenSans, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = AeroColors.PrimaryDeep)
        view.tags.forEach { (k, t) ->
            if (k !in view.range) return@forEach
            // a label never spills onto the next key: long ones shrink to the key's width
            val wide = measurer.measure(t, tagStyle).size.width
            val style = if (wide > ww * 0.95f) tagStyle.copy(fontSize = tagStyle.fontSize * (ww * 0.95f / wide).coerceAtLeast(0.55f)) else tagStyle
            centeredText(measurer, t, cx(k), if (view.arcs.isEmpty()) 18.dp2px(density) else 0f, style)
        }
        view.arcs.forEach { a ->
            if (a.from !in view.range || a.to !in view.range) return@forEach
            val x1 = cx(a.from); val x2 = cx(a.to); val y = top - 7.dp2px(density)
            val lo = minOf(x1, x2); val hi = maxOf(x1, x2)
            drawLine(AeroColors.PrimaryDeep, Offset(lo, y), Offset(hi, y), 2.dp2px(density))
            drawLine(AeroColors.PrimaryDeep, Offset(lo, y), Offset(lo, y + 6.dp2px(density)), 2.dp2px(density))
            drawLine(AeroColors.PrimaryDeep, Offset(hi, y), Offset(hi, y + 6.dp2px(density)), 2.dp2px(density))
            centeredText(measurer, a.label, (lo + hi) / 2, y - 17.dp2px(density), tagStyle)
        }
    }
}

private fun Int.dp2px(density: Float) = this * density
private fun blackX(k: Int, whites: List<Int>, ww: Float): Float = (whites.indexOf(k - 1) + 1) * ww - ww * 0.31f

/**
 * One 4-beat bar. The top row lights the beat that's sounding; extra rows show note lengths as blocks.
 */
@Composable
fun BeatBar(view: Basics.BeatView, beat: Int, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier.semantics { contentDescription = "One bar of 4 beats${if (view.labels.isNotEmpty()) ": " + view.labels.joinToString() else ""}" }) {
        val labelW = if (view.labels.isNotEmpty()) 64 * density else 0f
        val w = size.width - labelW
        val headH = 44 * density
        val gap = 6 * density
        // beat counter
        for (b in 0 until 4) {
            val x = labelW + b * w / 4
            val on = b == beat
            drawRoundRect(if (on) Brush.verticalGradient(listOf(Color(0xFF9BE15A), Color(0xFF2F8F1C))) else Brush.verticalGradient(listOf(Color(0xCCFFFFFF), Color(0x99FFFFFF))),
                Offset(x + gap / 2, 0f), Size(w / 4 - gap, headH), CornerRadius(12 * density))
            centeredText(measurer, "${b + 1}", x + w / 8, headH / 2 - 12 * density,
                TextStyle(fontFamily = OpenSans, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = if (on) Color.White else AeroColors.PrimaryDeep))
        }
        if (view.rows.size <= 1 && view.labels.isEmpty()) return@Canvas
        val rowsTop = headH + 10 * density
        val rowH = ((size.height - rowsTop) / view.rows.size).coerceAtMost(46 * density)
        val palette = listOf(0, 2, 4, 7, 9, 11).map { NoteColors.of(it) }
        view.rows.forEachIndexed { ri, row ->
            val y = rowsTop + ri * rowH
            view.labels.getOrNull(ri)?.let { centeredText(measurer, it, labelW / 2, y + rowH / 2 - 9 * density, TextStyle(fontFamily = OpenSans, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AeroColors.Ink)) }
            var t = 0f
            row.forEach { len ->
                val x = labelW + t / 4f * w
                drawRoundRect(palette[ri % palette.size], Offset(x + 2f, y + 3f), Size(len / 4f * w - 4f, rowH - 6f), CornerRadius(8 * density))
                drawRoundRect(Color(0x55FFFFFF), Offset(x + 6f, y + 6f), Size((len / 4f * w - 12f).coerceAtLeast(0f), (rowH - 12f) * 0.35f), CornerRadius(6 * density))
                t += len
            }
        }
    }
}

/** A small Piano roll: rainbow keys down the left, 4 beat columns, tap a cell to add or remove a 1-beat note. */
@Composable
fun MiniRoll(view: Basics.RollView, cells: Set<Pair<Int, Int>>, hintCells: Set<Pair<Int, Int>>, sounding: Set<Int>, modifier: Modifier, onCell: ((Int, Int) -> Unit)?) {
    val measurer = rememberTextMeasurer()
    val rows = view.rows.reversed().toList() // high at the top
    Canvas(modifier
        .semantics { contentDescription = "Piano roll, notes: ${(cells + view.shown).sortedBy { it.second }.joinToString { "${Basics.name(it.first)} on beat ${it.second + 1}" }.ifEmpty { "none" }}" }
        .pointerInput(onCell != null, view.rows) {
            if (onCell == null) return@pointerInput
            detectTapGestures { o ->
                val keyW = 54 * density; val headH = 22 * density
                if (o.x < keyW || o.y < headH) return@detectTapGestures
                val rowH = (size.height - headH) / rows.size
                val r = ((o.y - headH) / rowH).toInt().coerceIn(0, rows.size - 1)
                val b = ((o.x - keyW) / ((size.width - keyW) / 4f)).toInt().coerceIn(0, 3)
                onCell(rows[r], b)
            }
        }) {
        val keyW = 54 * density; val headH = 22 * density
        val rowH = (size.height - headH) / rows.size
        val colW = (size.width - keyW) / 4f
        drawRoundRect(Color(0xFF2B353C), Offset(keyW, headH), Size(size.width - keyW, size.height - headH), CornerRadius(6 * density))
        rows.forEachIndexed { i, p ->
            val y = headH + i * rowH
            val black = Basics.isBlack(p)
            drawRect(if (black) Color(0xFF26313A) else Color(0xFF34424C), Offset(keyW, y), Size(size.width - keyW, rowH))
            drawRect(Color(0xFF1E272E), Offset(keyW, y + rowH - 1f), Size(size.width - keyW, 1f))
            // the key, sideways
            val c = NoteColors.of(p)
            drawRect(if (black) Color(0xFF1C2228) else lerp(Color.White, c, 0.16f), Offset(0f, y), Size(keyW - 2f, rowH - 1f))
            drawRect(c, Offset(keyW - 14 * density, y + 1f), Size(10 * density, rowH - 3f))
            if (!black) centeredText(measurer, if (p % 12 == 0) "C${p / 12}" else Basics.name(p), keyW / 2 - 6 * density, y + rowH / 2 - 8 * density,
                TextStyle(fontFamily = OpenSans, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = AeroColors.Ink))
        }
        for (b in 0 until 4) {
            val x = keyW + b * colW
            drawLine(Color(0xFF1E272E), Offset(x, headH), Offset(x, size.height), 2f)
            centeredText(measurer, "${b + 1}", x + colW / 2, 2 * density, TextStyle(fontFamily = OpenSans, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AeroColors.PrimaryDeep))
        }
        fun note(p: Int, b: Int, ghost: Boolean) {
            val i = rows.indexOf(p); if (i < 0) return
            val o = Offset(keyW + b * colW + 3f, headH + i * rowH + 2f); val s = Size(colW - 6f, rowH - 4f)
            val c = NoteColors.of(p)
            if (ghost) drawRoundRect(Color(0xFFFFE26A), o, s, CornerRadius(5f), style = Stroke(2.5f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f))))
            else {
                drawRoundRect(if (p in sounding) lerp(c, Color.White, 0.35f) else c, o, s, CornerRadius(5f))
                drawRoundRect(Color(0x55FFFFFF), Offset(o.x + 3f, o.y + 2f), Size(s.width - 6f, s.height * 0.35f), CornerRadius(4f))
                if (rowH > 16 * density) centeredText(measurer, Basics.name(p), o.x + 18 * density, o.y + s.height / 2 - 8 * density,
                    TextStyle(fontFamily = OpenSans, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = NoteColors.ink(p)))
            }
        }
        hintCells.filter { it !in cells }.forEach { (p, b) -> note(p, b, true) }
        (view.shown + cells).forEach { (p, b) -> note(p, b, false) }
    }
}
