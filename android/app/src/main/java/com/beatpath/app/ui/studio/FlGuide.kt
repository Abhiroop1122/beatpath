package com.beatpath.app.ui.studio

/** "Where is this in FL?" content: FL name, shortcut, the desktop way, and the phone way. */
data class FlInfo(val title: String, val shortcut: String, val desktop: String, val phone: String)

object FlGuide {
    val items = mapOf(
        "rack" to FlInfo("Channel Rack", "F6", "Every instrument is a channel. Left-click a step to turn it on, right-click to turn it off. The green light mutes; the number box routes it to a Mixer track.", "Tap a step to toggle it, long-press to set how hard it's hit. Tap the green light to mute."),
        "roll" to FlInfo("Piano roll", "F7", "Open it from a channel (right-click → Piano roll). Time runs left to right; higher notes sit higher. Velocity lives in the event editor at the bottom.", "Tap a channel button or the Piano roll tab. Drag the green stems at the bottom to change velocity."),
        "draw" to FlInfo("Draw tool", "P", "Left-click to place a note; drag its right edge to change the length. Right-click deletes.", "Tap an empty cell to place and hear a note. Drag a note to move it, drag its right end to resize."),
        "paint" to FlInfo("Paint tool", "B", "Drag to paint repeated notes along the grid.", "Drag across a row to paint notes at every snap position."),
        "delete" to FlInfo("Delete tool", "D", "Click a note to delete it. With Draw or Paint, right-click also deletes.", "Pick Delete, then tap (or swipe over) notes."),
        "select" to FlInfo("Select tool", "E", "Drag a box to select. Shift+↑/↓ moves a semitone, Ctrl+↑/↓ an octave, Ctrl+B duplicates in the Piano roll.", "Tap notes to select them, then use the action buttons that appear."),
        "snap" to FlInfo("Snap (magnet)", "Piano roll toolbar", "Snap sets where notes land: Beat, ½ beat, Step (a 16th), ½ step (a 32nd) and finer.", "Tap the magnet to change snap."),
        "play" to FlInfo("Play / Stop", "Space", "Space starts and stops. PAT plays the current Pattern; SONG plays the Playlist.", "The ▶ and ■ buttons. Beatpath loops the Pattern, like FL's PAT mode."),
        "tempo" to FlInfo("Tempo", "drag the LCD", "Click-drag the tempo display up or down to change BPM.", "Tap the tempo LCD."),
        "mixer" to FlInfo("Mixer", "F9", "Each channel goes to an insert track. Faders set level in dB, the knob pans, the slots hold effects, and the Master is the final output.", "Swipe the strips sideways. Drag a fader, turn a pan knob, tap EQ/Comp to open a plugin."),
        "eq" to FlInfo("Fruity Parametric EQ 2", "Mixer slot", "Drag the band tokens on the graph. Bands can be high pass, shelves or peaks. Cut before you boost.", "Drag the coloured dots: left/right is frequency, up/down is boost or cut."),
        "comp" to FlInfo("Fruity Compressor", "Mixer slot", "THRESHOLD, RATIO, GAIN, ATTACK, RELEASE. Watch gain reduction, not the knobs.", "Same knobs; the orange bar shows how much it's turning down."),
        "reverb" to FlInfo("Fruity Reeverb 2", "Send track", "Put it on its own Mixer track at DRY 0% and send parts to it. Rarely use reverb on the Master.", "The Rev knob on each strip sends to the shared room."),
        "delay" to FlInfo("Fruity Delay 3", "Send track", "Tempo-synced TIME, FEEDBACK for repeats, CUTOFF to darken the echoes.", "The Dly knob on each strip sends to the delay."),
        "limiter" to FlInfo("Fruity Limiter", "Master slot", "Last on the Master. GAIN pushes in, CEIL stops the peaks. A few dB of reduction is plenty.", "Master strip → Limiter."),
        "playlist" to FlInfo("Playlist", "F5", "Arrange Pattern clips on tracks. Switch the transport to SONG to hear the arrangement; PAT loops one Pattern.", "Pick a Pattern on the left, tap the grid to place it, tap a clip to remove it. Then switch to SONG."),
        "synth" to FlInfo("3x Osc + Envelope", "Channel settings", "3x Osc has the oscillator shapes; the ADSR and filter (MOD X/Y) live on the channel's Envelope / Instrument tab.", "Tap Synth: waves, filter and envelope in one panel."),
    )
}
