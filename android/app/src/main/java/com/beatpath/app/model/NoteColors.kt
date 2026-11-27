package com.beatpath.app.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * The rainbow note colors taught in Music basics: C red, D orange, E yellow, F green, G blue, A indigo, B violet.
 * Black keys take the color halfway between their neighbors. The same colors appear in the Studio's Piano roll
 * when the rainbow toggle is on, so the keyboard you learned on maps straight onto the roll.
 */
object NoteColors {
    private val pcs = longArrayOf(
        0xFFE53935, // C  red
        0xFFF2652A, // C♯
        0xFFFB8C00, // D  orange
        0xFFF9B21E, // D♯
        0xFFFDD835, // E  yellow
        0xFF43A047, // F  green
        0xFF1E9E8C, // F♯
        0xFF1E88E5, // G  blue
        0xFF3367C9, // G♯
        0xFF3949AB, // A  indigo
        0xFF6236AE, // A♯
        0xFF8E24AA, // B  violet
    ).map { Color(it) }

    fun of(midi: Int): Color = pcs[((midi % 12) + 12) % 12]
    /** Readable text on top of [of]: dark on the warm, light colors, white on the rest. */
    fun ink(midi: Int): Color = if (((midi % 12) + 12) % 12 in 2..4) Color(0xFF3A2A00) else Color.White
    fun name(midi: Int) = arrayOf("red", "red-orange", "orange", "orange-yellow", "yellow", "green", "teal", "blue", "blue-indigo", "indigo", "purple", "violet")[((midi % 12) + 12) % 12]

    /** Studio Piano roll: color notes and keys by pitch. Saved by the Store. */
    var rainbowRoll by mutableStateOf(true)
    var onRainbowChange: (Boolean) -> Unit = {}
    fun setRainbow(v: Boolean) { rainbowRoll = v; onRainbowChange(v) }
}
