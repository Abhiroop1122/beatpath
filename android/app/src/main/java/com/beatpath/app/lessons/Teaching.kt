package com.beatpath.app.lessons

/**
 * Plain-language teaching helpers: a glossary Lou can explain on tap, and a pool of short tips
 * (music theory) he shares after correct answers so learning never stops at "correct!".
 */
object Glossary {
    data class Term(val word: String, val pattern: Regex, val meaning: String)

    private fun t(word: String, meaning: String, vararg alt: String) =
        Term(word, Regex("\\b(" + (listOf(word) + alt).joinToString("|") { Regex.escape(it) } + ")\\b", RegexOption.IGNORE_CASE), meaning)

    val terms = listOf(
        t("beat", "A beat is the steady tick of music, like a clock. It's what you tap your foot to.", "beats"),
        t("bar", "A bar is a group of beats, usually 4. Music is counted in bars: 1 2 3 4, 1 2 3 4.", "bars"),
        t("step", "In FL Studio, a step is one little box in the Channel Rack. 4 steps make one beat.", "steps"),
        t("tempo", "Tempo is how fast the beat goes. We count it in BPM: beats per minute."),
        t("BPM", "BPM means beats per minute. 90 BPM feels like walking. 128 BPM feels like dancing."),
        t("backbeat", "The backbeat is beats 2 and 4. That's where the clap or snare usually hits."),
        t("velocity", "Velocity is how hard a note is played. More velocity means a louder note."),
        t("pitch", "Pitch is how high or low a note sounds."),
        t("octave", "An octave is the same note, higher or lower. It's 12 keys away, like C5 and C6.", "octaves"),
        t("half step", "A half step is the smallest move: from one key to the very next key, black or white. Grown-ups also call it a semitone.", "half steps", "semitone", "semitones"),
        t("whole step", "A whole step is two half steps. You skip over one key."),
        t("scale", "A scale is a set of notes that sound good together, like a staircase. The white keys from C to C make one.", "scales"),
        t("key", "A song's key is its home note and the family of notes it uses. C major uses the white keys."),
        t("tonic", "The tonic is the home note. Tunes feel finished when they land on it.", "home note"),
        t("interval", "An interval is the gap between two notes. Count the letters to name it.", "intervals"),
        t("chord", "A chord is three or more notes played at the same time.", "chords"),
        t("triad", "A triad is a chord with 3 notes. Take a note, skip one, take one, skip one, take one.", "triads"),
        t("root", "The root is the bottom note of a chord. It gives the chord its name, like C in C major."),
        t("major", "Major sounds bright and happy, like sunshine. Its middle note is 4 half steps above the root."),
        t("minor", "Minor sounds darker, like a rainy day. Its middle note is only 3 half steps above the root."),
        t("progression", "A chord progression is a few chords played one after another, like C, G, A minor, F.", "progressions"),
        t("inversion", "An inversion is the same chord with a different note at the bottom.", "inversions"),
        t("chord tone", "A chord tone is a note inside the chord that's playing right now. It always fits.", "chord tones"),
        t("melody", "A melody is the tune: a line of single notes you could sing.", "melodies"),
        t("motif", "A motif is a tiny tune, just a few notes long. You repeat it and change it a little.", "motifs"),
        t("rest", "A rest is a quiet gap on purpose. It lets the music breathe.", "rests"),
        t("syncopation", "Syncopation puts notes in between the beats. It makes the rhythm bounce.", "syncopated"),
        t("leap", "A leap is a jump in a tune, over other notes. Like a frog hopping.", "leaps"),
        t("stepwise", "Stepwise means moving to the very next note. It sounds smooth and is easy to sing."),
        t("pentatonic", "The pentatonic scale has just 5 notes, like C D E G A. They all sound good together."),
        t("harmony", "Harmony is notes sounding together, like a second voice singing along with the tune."),
        t("bassline", "The bassline is the low part of a song. It often plays the bottom note of each chord.", "bass line"),
        t("ghost note", "A ghost note is a very quiet hit. You feel it more than you hear it.", "ghost notes"),
        t("swing", "Swing makes every other step a tiny bit late. It makes the groove bounce."),
        t("Piano roll", "The Piano roll is where you draw notes in FL Studio. Time goes left to right. High notes are at the top."),
        t("Channel Rack", "The Channel Rack holds your sounds in FL Studio. Switch its little boxes on to make a beat."),
        t("snap", "Snap makes notes jump neatly onto the grid lines, like little magnets."),
    )

    /** Up to [max] glossary terms that appear in [text], in the order they appear. */
    fun find(text: String, max: Int = 3): List<Term> =
        terms.mapNotNull { term -> term.pattern.find(text)?.let { it.range.first to term } }
            .sortedBy { it.first }.map { it.second }.distinctBy { it.word.lowercase() }.take(max)
}

object Tips {
    private val theory = mapOf(
        Track.BEATS to listOf(
            "Count out loud: \"1 and 2 and\". Your voice keeps time better than your eyes.",
            "Kicks often go on beats 1 and 3. Claps go on 2 and 4. Start there, then play around.",
            "Hi-hats are like a ticking clock. More hi-hats make a beat feel faster.",
            "Leave some gaps. A beat with fewer hits can hit harder.",
        ),
        Track.NOTES to listOf(
            "Every major scale uses the same recipe: whole, whole, half, whole, whole, whole, half.",
            "E to F and B to C have no black key between them. They are only a half step apart.",
            "FL Studio calls middle C \"C5\". Some apps call it C4. It's the same note.",
            "Learn one scale really well first. C major is easy: it's just the white keys.",
        ),
        Track.HARMONY to listOf(
            "Build a chord like this: take a note, skip one, take one, skip one, take one.",
            "The middle note sets the mood. Move it down one key and a happy chord turns sad.",
            "Chords that share notes flow nicely into each other. C and A minor share C and E.",
            "If a row of chords feels finished, it probably just landed on the home chord.",
        ),
        Track.MELODY to listOf(
            "Sing your tune. If you can't sing it, it might be too jumpy.",
            "On beats 1 and 3, use notes from the chord. They always fit.",
            "Play an idea, then play it again with one small change. That's how catchy tunes are made.",
            "After a big jump up, take a small step back down. It sounds natural.",
            "Quiet gaps are part of the tune. Silence makes the next note matter.",
        ),
        Track.BASS to listOf(
            "Not sure what the bass should play? Try the chord's bottom note on beat 1.",
            "Let the bass play with the kick drum. Together they sound like one big instrument.",
            "Low notes take up lots of room. Keep basslines simple.",
        ),
        Track.ARRANGE to listOf(
            "Choruses often sit higher than verses. Lift the tune to lift the energy.",
            "Take some sounds away before a big moment. It hits harder when they come back.",
            "Finish your ideas. One 8-bar tune you love beats ten tiny loops.",
        ),
    )
    /** FL tips are no longer shown (the app owner removed them); kept here for reference. */
    @Suppress("unused")
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

    /** The next theory tip for [track], cycling through that track's pool. */
    fun next(track: Track): String {
        val pool = theoryFor(track)
        val tip = pool[i % pool.size]
        i++
        return "Theory tip: $tip"
    }

    fun theoryFor(track: Track): List<String> = theory[track] ?: theory[Track.MELODY]!!
}
