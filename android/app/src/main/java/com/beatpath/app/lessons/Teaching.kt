package com.beatpath.app.lessons

/**
 * Plain-language teaching helpers: a glossary Lou can explain on tap, and a pool of short tips
 * (music theory + FL Studio) he shares after correct answers so learning never stops at "correct!".
 */
object Glossary {
    data class Term(val word: String, val pattern: Regex, val meaning: String)

    private fun t(word: String, meaning: String, vararg alt: String) =
        Term(word, Regex("\\b(" + (listOf(word) + alt).joinToString("|") { Regex.escape(it) } + ")\\b", RegexOption.IGNORE_CASE), meaning)

    val terms = listOf(
        t("beat", "A beat is the steady pulse you tap your foot to. Most music has four beats in a bar.", "beats"),
        t("bar", "A bar is a group of beats — usually four. Bar 1, bar 2… are like the boxes music is counted in.", "bars"),
        t("step", "In FL Studio a step is a sixteenth note: four steps make one beat. Each pad in the Channel Rack is one step.", "steps"),
        t("tempo", "Tempo is how fast the beat goes, measured in BPM — beats per minute."),
        t("BPM", "BPM means beats per minute. 90 BPM is relaxed; 128 is dance speed."),
        t("backbeat", "The backbeat is beats 2 and 4 — where the clap or snare usually hits."),
        t("velocity", "Velocity is how hard a note is played. Higher velocity is louder and punchier."),
        t("pitch", "Pitch is how high or low a note sounds."),
        t("octave", "An octave is the same note, higher or lower — 12 semitones apart. C5 and C6 are an octave apart.", "octaves"),
        t("semitone", "A semitone is the smallest step on a keyboard: from one key to the very next key, black or white.", "semitones", "half step"),
        t("whole step", "A whole step is two semitones — skipping one key."),
        t("scale", "A scale is a set of notes that sound good together, like the white keys from C to C.", "scales"),
        t("key", "The key is the home base of a song: its home note and the scale it uses."),
        t("tonic", "The tonic is the home note of a key. Melodies feel finished when they land on it.", "home note"),
        t("interval", "An interval is the distance between two notes, counted in steps.", "intervals"),
        t("chord", "A chord is three or more notes played together.", "chords"),
        t("triad", "A triad is a three-note chord: a root, a third and a fifth.", "triads"),
        t("root", "The root is the note a chord is named after — C is the root of C major."),
        t("major", "Major sounds bright and happy. A major chord's middle note sits 4 semitones above the root."),
        t("minor", "Minor sounds darker or sadder. A minor chord's middle note sits only 3 semitones above the root."),
        t("progression", "A chord progression is a series of chords played one after another, like C – G – Am – F.", "progressions"),
        t("inversion", "An inversion is the same chord with a different note at the bottom.", "inversions"),
        t("chord tone", "A chord tone is a note that's inside the chord playing right now. They always sound good.", "chord tones"),
        t("melody", "A melody is a line of single notes you could sing — the tune.", "melodies"),
        t("motif", "A motif is a short musical idea, a few notes long, that you repeat and change.", "motifs"),
        t("rest", "A rest is silence on purpose. Gaps make melodies breathe.", "rests"),
        t("syncopation", "Syncopation puts notes between the beats, which makes rhythm bounce.", "syncopated"),
        t("leap", "A leap is a jump of more than two semitones in a melody.", "leaps"),
        t("stepwise", "Stepwise means moving to the next note over — smooth and easy to sing."),
        t("pentatonic", "The pentatonic scale has five notes (C D E G A) and no clashing notes at all."),
        t("harmony", "Harmony is two or more notes sounding together — like a second voice under a melody."),
        t("bassline", "The bassline is the low part. It usually plays the root of each chord.", "bass line"),
        t("ghost note", "A ghost note is a very quiet hit you feel more than hear.", "ghost notes"),
        t("swing", "Swing pushes every second sixteenth a little late, so the groove bounces."),
        t("Piano roll", "FL's Piano roll is where you draw notes: time runs left to right, pitch goes up and down."),
        t("Channel Rack", "FL's Channel Rack holds every instrument; its pads are steps you switch on to make beats."),
        t("snap", "Snap decides where notes land on the grid: on beats, half beats or steps."),
    )

    /** Up to [max] glossary terms that appear in [text], in the order they appear. */
    fun find(text: String, max: Int = 3): List<Term> =
        terms.mapNotNull { term -> term.pattern.find(text)?.let { it.range.first to term } }
            .sortedBy { it.first }.map { it.second }.distinctBy { it.word.lowercase() }.take(max)
}

object Tips {
    private val theory = mapOf(
        Track.BEATS to listOf(
            "Count out loud: \"one-and-two-and\". Your mouth keeps time better than your eyes.",
            "Kicks often land on beats 1 and 3; snares or claps on 2 and 4. Start there, then break the rule.",
            "Hi-hats are the clock of a beat — busier hats make a song feel faster without changing tempo.",
            "Leave space. A beat with fewer hits often hits harder.",
        ),
        Track.NOTES to listOf(
            "Every major scale uses the same recipe: whole, whole, half, whole, whole, whole, half.",
            "Two white keys with no black key between them — E–F and B–C — are only a semitone apart.",
            "Middle C is called C5 in FL Studio, but C4 in many other apps. Same note, different label.",
            "Learn one scale really well before learning many. C major is the white keys.",
        ),
        Track.HARMONY to listOf(
            "Build a chord by stacking every other note of the scale: skip one, take one.",
            "The middle note decides major or minor. Lower it by one semitone and major becomes minor.",
            "Chords that share notes flow smoothly into each other — C and Am share C and E.",
            "If a progression feels finished, it probably just landed on the home chord.",
        ),
        Track.MELODY to listOf(
            "Sing your melody. If you can't sing it, it's probably too jumpy.",
            "Put chord tones on beats 1 and 3, and use in-between notes on the off-beats.",
            "Repeat an idea, then change just one thing. That's how hooks are made.",
            "After a big jump up, step back down. It sounds natural to the ear.",
            "Rests are part of the melody. Silence makes the next note matter.",
        ),
        Track.BASS to listOf(
            "When in doubt, the bass plays the root of the chord on beat 1.",
            "Lock the bass with the kick drum and they sound like one big instrument.",
            "Low notes take up a lot of space — keep basslines simple.",
        ),
        Track.ARRANGE to listOf(
            "A chorus usually sits higher than the verse — lift the melody to lift the energy.",
            "Take things away before a big moment so it hits harder when they come back.",
            "Finish ideas: an 8-bar hook you love beats ten 1-bar loops.",
        ),
    )
    private val fl = listOf(
        "In FL Studio, press F6 for the Channel Rack and F7 for the Piano roll.",
        "Right-click deletes notes in FL's Piano roll when you're using Draw or Paint.",
        "Space plays and stops in FL. Get used to it — you'll press it thousands of times.",
        "Ctrl+B in FL's Piano roll duplicates your selection to the right.",
        "In FL, Shift+↑/↓ moves selected notes a semitone; Ctrl+↑/↓ moves them an octave.",
        "Turn on FL's scale highlighting (Piano roll → Helpers) to see which notes are in key.",
        "Clone a Pattern in FL before changing it, so you can always go back.",
        "Ghost channels in FL's Piano roll show other instruments' notes behind the one you edit.",
        "Save often in FL: Ctrl+S. Use \"Save new version\" to keep a history of your song.",
        "Export a MIDI file from Beatpath and import it in FL: Piano roll → File → Import MIDI file.",
    )
    private var i = 0

    /** Alternates a theory tip for [track] with a general FL tip. */
    fun next(track: Track): String {
        i++
        return if (i % 2 == 0) "FL tip: " + fl[(i / 2) % fl.size]
        else "Theory tip: " + (theory[track] ?: theory[Track.MELODY]!!).let { it[(i / 2) % it.size] }
    }

    fun theoryFor(track: Track): List<String> = theory[track] ?: theory[Track.MELODY]!!
}
