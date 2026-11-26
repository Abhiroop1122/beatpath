# Beatpath 1.0.0 — Android

Learn music theory and melody writing, step by step, then make it in FL Studio. Kotlin + Jetpack Compose.
Design: `../fl duolingo.pen`. Research: `../research/`.

## What's in it
- **Lou, the only guide.**
  - He's drawn in code. His poses blend smoothly from mood to mood instead of snapping.
  - He doesn't talk out loud: what he says appears in his speech bubble. He reacts with small, non-verbal chirps (happy, "hmm", surprised, yawn).
  - Dev gallery: `adb shell am start -n com.beatpath.app/.MainActivity --ez lou_gallery true`.
- **Teaching that explains itself:**
  - Every Studio lesson opens with a "Today's idea" card: the idea in plain words, the key terms explained, a theory tip, and where to find it in FL.
  - Music words in Lou's bubble become **? chips**. Tap one and Lou explains it.
  - After each correct answer, Lou adds a theory tip or an FL tip.
  - Hints climb a ladder: a clue, then gold outlines showing exactly where, then the idea again in other words.
- **Sounds:** a soft lushcore/Y2K kit (glassy bells, chorus and a big airy reverb). Includes taps, toggles, correct and miss cues, completion, XP ticks, and Lou's chirps.
- **Frutiger Aero shell:**
  - The main menu (Welcome and the Home tabs) uses a sky-blue ripple artwork, recoloured from the supplied image to the app's sky blue.
  - Lessons use the sky gradient with bubbles.
  - Glass panels, compact gel buttons, Open Sans (OFL). Screen changes use soft spring fades.
- **More sounds:**
  - Channel Rack **Kit** picker: Classic, 808, House, Lo-fi. Same pattern, different drums. All synthesized, so there are no samples.
  - Piano roll **♪ Sound** picker for each melodic channel:
    - Lead: square lead, bell, flute, saw lead, pluck, chiptune, soft keys
    - Chords: warm saw, soft pad, e-piano, strings, pluck chords, organ
    - Bass: saw, sub, square, pluck, wobble
  - Your choices are saved with the loop and included in WAV export.
- **Auto-rotate:** every screen has its own landscape layout.
  - Studio: the FL windows sit on the left, with Lou's coach as a side panel.
  - Home: a side navigation rail.
  - Ear lessons, Welcome and Complete: side by side.
  - Turning the phone mid-lesson keeps your progress. Rotation lock is respected.
- **FL-faithful Studio, kept simple:**
  - Channel Rack: LEDs, glossy grey and maroon steps, swing; pan and volume knobs only on wide screens.
  - Piano roll: real keys, ghost channels, Draw P / Paint B / Delete D / Select E, snap including triplets, an optional velocity lane, zoom, and an accessible list view.
  - Play/Stop and the tempo LCD.
  - No mixing or mastering screens: the app is about theory and melody.
- **Export:** MIDI for FL Studio (Piano roll → File → Import MIDI file) and WAV to share.
- **Practice (new in 1.0):**
  - A Practice tab with:
    - **Daily mix:** 8 ear questions weighted toward your weak skills.
    - **Spaced review:** lessons you needed help with return after 1, 3 and 7 days.
    - **Endless drills:** high/low, pitch vs volume, up/down, step/leap, major/minor, *which chord?* (I–IV–V–vi) and **melody dictation** (hear 4 notes, write them in the Piano roll).
  - **Course complete card:** finish all 81 lessons and the Learn path offers Practice, Make a song, or Redo for gold.
- **Less text:**
  - Lesson extras are folded behind *Words & tips*.
  - Tray tips are behind *💡 Tip*.
  - Studio levels are fold cards.
- **Frosted glass on the menu:** panels and buttons show a blurred copy of the sky artwork behind them.
- **Course: 81 lessons.**
  - 5 ear lessons: high/low, soft/loud, contour, step/leap, major/minor.
  - 76 Studio lessons in 18 levels, from your first beat to writing an 8-bar hook with harmony:
    - beats, notes and keys, chords, feel, bass, melody I and II, harmony II, making a track
    - keys & scales II, harmony lines, melodic rhythm, minor & modes, progressions II, melody development, phrases & form, songwriting

## Build & test
```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug testDebugUnitTest
```
27 unit tests cover:
- MIDI export
- the curriculum: every lesson's setup and checks, with pass and fail cases
- the sound presets
- the practice generators, melody dictation and the spaced-review schedule

The UI check scripts are in `tools/ui-check/`.

Open any screen directly: `adb shell am start -S -n com.beatpath.app/.MainActivity --es route <lesson id | ear.high-low | practice | practice.mix | practice.dictation | home | studio | welcome | complete>`.

## Known gaps
- On narrow phones (under about 400dp wide), Channel Rack step pads are 30dp wide, like FL's own compact grid. Every pad is 48dp tall.
- Tested on an emulator at phone size (1080×2340 @ 440dpi), in portrait and landscape. Audio latency hasn't been measured on a real phone.
