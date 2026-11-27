# Music basics: research notes

These notes back the Music basics unit (16 lessons in `android/app/src/main/java/com/beatpath/app/lessons/Basics.kt`). The facts were checked on 26 November 2026 against the sources listed under each heading.

## Order of teaching

Sound comes before symbols. Gordon's Music Learning Theory calls it "sound before sight", Kodály teaches a concept through experience and names it afterwards, and Suzuki students learn by ear before reading notes. So every lesson plays the idea first (Lou's tune, a key you tap) and gives it a name after. Lessons are 2 to 4 minutes and come back through spaced review, because spaced practice beats massed practice (Cepeda et al. 2006 found it better in 259 of 271 comparisons).

Sources: https://en.wikipedia.org/wiki/Music_learning_theory · https://en.wikipedia.org/wiki/Kod%C3%A1ly_method · https://en.wikipedia.org/wiki/Suzuki_method · https://en.wikipedia.org/wiki/Spacing_effect

## The rainbow colors

The app colors the seven letters like a rainbow: C red, D orange, E yellow, F green, G blue, A indigo, B violet. Black keys take the color between their neighbors. This is close to the Boomwhackers tubes used in school music rooms (C red, D orange, E yellow, F lime, G teal, A indigo, B magenta), so a learner who has seen those will feel at home. The same colors show in the Studio Piano roll while its 🌈 toggle is on.

Source: https://boomwhackers.com/products/boomwhackers%C2%AE-c-major-diatonic-scale-set

## How to work out notes

| Idea | Rule used in the lessons |
|---|---|
| Half step | The very next key, black or white. Every key you pass counts 1. |
| Whole step | 2 half steps (skip one key). E to F and B to C are half steps. |
| Intervals | Count letters with both ends included (C D E is a 3rd). Sizes in half steps: m2 1, M2 2, m3 3, M3 4, P4 5, tritone 6, P5 7, m6 8, M6 9, m7 10, M7 11, octave 12. |
| Major scale | W W H W W W H. From C it uses only white keys; from G it needs F♯ (one sharp). |
| Natural minor | W H W W H W W (taught later, in level 14). |
| Triads | Take, skip, take, skip, take. Major is root + 4 + 3 half steps, minor is + 3 + 4, diminished + 3 + 3, augmented + 4 + 4. |
| Chords in a major key | I major, ii minor, iii minor, IV major, V major, vi minor, vii° diminished. |

Sources: https://openmusictheory.github.io/scales.html · https://en.wikipedia.org/wiki/Interval_(music) · https://openmusictheory.github.io/triads.html · https://en.wikipedia.org/wiki/Triad_(music)

## Which note comes next

The "What note comes next?" lesson teaches five habits:

1. Move mostly by step.
2. 7 (the leading tone) pulls up to 1. Open Music Theory: "when ti appears it tends to be followed by do."
3. 4 tends to fall to 3, mainly over the V chord, where it is a dissonance that resolves down by step. The lesson says "over a G chord" for that reason. 2 to 1 is a softer pull, so the lesson doesn't teach it as a rule.
4. After a big leap, step back the other way. This is Meyer's "gap fill". von Hippel and Huron (2000) showed it mostly follows from melodies drifting back toward the middle of their range, so the lesson presents it as a writing habit, not a law.
5. Put chord tones on strong beats and end on 1.

Sources: https://openmusictheory.github.io/tendencyTonesFunctionalDissonances.html · https://openmusictheory.github.io/cantusFirmus.html · https://openmusictheory.github.io/secondSpecies.html · von Hippel, P. and Huron, D. (2000), "Why Do Skips Precede Reversals? The Effect of Tessitura on Melodic Structure", Music Perception 18(1), 59 to 85, doi:10.2307/40285901

## Rhythm

In 4/4 a whole note fills the bar (4 beats), a half note lasts 2, a quarter 1, an eighth half a beat. Tempo is in beats per minute. FL Studio splits each beat into 4 steps, so the Channel Rack shows 16 steps per bar.

Sources: https://openmusictheory.github.io/rhythmicValues.html · https://en.wikipedia.org/wiki/Tempo

## FL Studio naming

FL numbers notes an octave higher than many MIDI charts: note 60 is C5 ("like the MIDI standard, but an octave higher"), and black keys are written with sharps, such as G#4. The app uses the same names so what you learn matches FL. FL's own Piano roll page also describes the major scale as tone, tone, semitone, tone, tone, tone, semitone.

Sources: https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/pianoroll_scripting_api.htm · https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/chansettings_misc.htm · https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/pianoroll.htm

## The 16 lessons

Part 1, notes and the keyboard: What is a note?, The musical alphabet, Find any note, Octaves, Half steps and whole steps, Sharps and flats, Count to any note, The major scale.

Part 2, how notes fit together: Scale numbers and home, Intervals, What is a chord?, Major and minor, The chords in a key, What note comes next?, Beats bars and lengths, Keyboard to Piano roll.

After that the course continues with the ear warm-up and then FL's Channel Rack (level 1).
