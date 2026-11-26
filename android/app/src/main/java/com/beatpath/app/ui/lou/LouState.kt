package com.beatpath.app.ui.lou

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.beatpath.app.audio.Sfx

/**
 * Lou's brain: mood and speech bubble. He doesn't talk out loud — what he says appears in his bubble,
 * and he reacts with small non-verbal chirps.
 */
class LouState(private val sfx: Sfx) {
    var mood by mutableStateOf(LouMood.Idle)
    var bubble by mutableStateOf<String?>(null)
    /** Kept for drawing; stays closed because Lou doesn't speak aloud. */
    var mouth by mutableFloatStateOf(0f)

    @Suppress("UNUSED_PARAMETER")
    fun say(text: String, mood: LouMood = LouMood.Idle, voice: Boolean = false) {
        bubble = text
        this.mood = mood
    }

    fun react(m: LouMood) {
        mood = m
        when (m) {
            LouMood.Happy, LouMood.Cheer -> sfx.louHappy()
            LouMood.Think, LouMood.Encourage -> sfx.louHmm()
            LouMood.Surprised -> sfx.louSurprised()
            LouMood.Sleepy -> sfx.louYawn()
            else -> {}
        }
    }

    fun quiet() { mouth = 0f }
}

@Composable
fun rememberLou(sfx: Sfx): LouState {
    val lou = remember(sfx) { LouState(sfx) }
    DisposableEffect(lou) { onDispose { lou.quiet() } }
    return lou
}
