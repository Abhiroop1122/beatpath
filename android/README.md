# Beatpath 1.2.1 for Android

Beatpath teaches music theory and melody writing step by step, with an FL Studio-style Studio to practice in. It's written in Kotlin with Jetpack Compose.
The design file is `../fl duolingo.pen` and the research notes are in `../research/`.

## Lou
- Lou is drawn from the exact paths of `../docs/lou.svg` (`ui/lou/LouArt.kt`), the same artwork as the GitHub banner, and `ui/lou/Lou.kt` animates the parts. His poses blend from one mood to the next.
- He breathes, blinks, looks around, and sways his hands and feet. The hands and feet are cut out of the body outline on a separate layer and redrawn a few degrees rotated, so nothing opens up in the body.
- He doesn't talk out loud. What he says appears in his speech bubble, and he reacts with short chirps (happy, "hmm", surprised, yawn).
- When you try to leave a lesson he turns sad (droopy lids, a frown, a rolling tear) with a small "aww" sound (`LeaveDialog` in `ui/aero/Clay.kt`).
- To see every mood, open the dev gallery: `adb shell am start -n com.beatpath.app/.MainActivity --ez lou_gallery true`.

## Teaching
- Each Studio lesson opens with a "Today's idea" card. It states the idea in one sentence and folds the term definitions, a theory tip and the FL location behind *Words & tips*.
- Music words in Lou's bubble turn into chips such as "? chord". Tap one and Lou explains the word.
- After a correct answer, the result tray has a short theory tip behind *💡 Tip*.
- Hints come in three levels: a clue, then gold outlines showing where to click, then the idea again in other words. "↩ Back to the task" brings the instructions back.

## Course
There are 125 lessons:

- 16 Music basics lessons (new in 1.1, `lessons/Basics.kt`, screen `ui/lesson/BasicsLesson.kt`). They teach on a rainbow keyboard, a beat bar and a small Piano roll: notes, the letters, finding keys, octaves, half and whole steps, sharps and flats, counting half steps, the major scale, scale numbers, intervals, chords, major and minor, the chords in a key, which note comes next, rhythm, and the Piano roll. Research: `../research/basics-research.md`.
- 5 ear lessons: high or low, soft or loud, contour, step or leap, and major or minor.
- 98 Studio lessons in 22 levels, from a first beat to an 8-bar hook with harmony:
  - Levels 1 to 10: beats, notes and keys, chords, feel, bass, melody I and II, harmony II and making a track.
  - Levels 11 to 18: new keys, harmony lines, tune rhythms, minor keys and modes, chord journeys, growing a tune, shaping a song, and songwriting.
  - Levels 19 to 22 (`lessons/PacksCurriculum.kt`): chord voicings, genre grooves, bass lines II and melody III.
- 6 Ear training II lessons (`lessons/EarTwo.kt`, the same page format as Music basics), placed after level 10.
- Every lesson was rewritten so an 8-year-old can follow it. It uses only ideas taught earlier and explains each new word the first time.

## Practice (new in 1.0)
The Practice tab has three parts:

- The Daily mix is 8 ear questions, weighted toward the skills you get wrong.
- Review brings back lessons you needed help with after 1, 3 and 7 days.
- Drills are endless: high or low, pitch or volume, up or down, step or leap, major or minor, "Which chord?" (C, then F, G or A minor), and melody dictation, where you hear 4 notes and write them in the Piano roll.

The Studio Piano roll's 🌈 button colors notes and keys with the same rainbow (saved in settings).

When every lesson is done, the Learn path shows a course-complete card with Practice, Make a song and Redo for gold.

## Mini games (Studio tab)
The Studio tab holds the games, free play and the song maker. Lessons are only on the Learn path.
- `ui/games/GameKit.kt` has the shared shell: an intro where Lou explains, a 3-2-1 countdown, the hearts and score bar, sad-Lou "Stop playing?" on Back, and results with 1 to 3 stars, best score and XP (`Store.recordGame`).
- `AudioClock` (in GameKit.kt) follows the engine's playhead, which includes output latency, so the rhythm games stay locked to the sound. `LouActor` places Lou with squash, lean, gaze and a ground shadow.
- The games: `PianoTiles.kt` (play real songs as tiles land on the rainbow keyboard), `SkyHop.kt` (hear up, down or the same, then steps and leaps), `BeatRun.kt` (jump on the beat, then the "and"), `BalloonPop.kt` (note names on the rainbow keyboard, with the help fading), `ChordCake.kt` (build major and minor chords as cake layers) and `EchoLou.kt` (repeat a growing pentatonic tune).
- Debug routes: `--es route game.tiles` or `game.skyhop` (or `game.beatrun`, `game.balloons`, `game.chordcake`, `game.echo`). Add `--ez autoplay true` in a debug build to watch a game play itself.

## Song maker
Free play has loops A, B and C (`Project.switchTo`) and a Song window (`ui/studio/SongView.kt`) with 8 or 16 bar tiles. Play switches between one loop and the whole song (`Project.songPattern()`), and MIDI and WAV export the whole song when it's on.

## Quests, streaks and Lou's Shop
- `data/Shop.kt` holds the shop items (hats, glasses, extras, colors, each with a rarity from its price), the mystery box, the level curve and `Quests` (3 a day picked by date, plus streak chests on days 3, 7, 14 and 30).
- `Store` keeps the daily counters (`qd.<date>.<metric>`), claimed quests, opened chests, game bests, spent XP, owned and equipped items.
- `ui/lou/LouLook.kt` draws the hats, glasses and extras on Lou's body and swaps his colors.

## Studio
- The Channel Rack has LEDs, grey and maroon step pads, and swing. Pan and volume knobs appear only on wide screens.
- The Piano roll has keys, ghost channels from other instruments, the Draw (P), Paint (B), Delete (D) and Select (E) tools, snap down to triplets, an optional velocity lane, zoom, and a list view for TalkBack users.
- The transport is Play, Stop and the tempo display.
- There are no mixing or mastering screens; the app is about theory and melody.

## Sound
- Everything is synthesized on the phone, with no samples.
- The Channel Rack's Kit picker switches between Classic, 808, House and Lo-fi. The pattern stays the same and only the drums change.
- In the Piano roll, the ♪ Sound picker switches each melodic channel's preset:
  - Lead: square lead, bell, flute, saw lead, pluck, chiptune, soft keys
  - Chords: warm saw, soft pad, e-piano, strings, pluck chords, organ
  - Bass: saw, sub, square, pluck, wobble
- Your choices are saved with the loop and used in WAV export.
- The interface sounds (taps, toggles, correct and miss cues, lesson complete, XP ticks) and Lou's chirps use soft bells with chorus and a long reverb.

## XP and Lou's Shop
- `data/Shop.kt` lists the items (hats, glasses, Lou colors, sky themes), the mystery box price and the level curve (100 XP for level 2, then 50 more each level).
- `Store` keeps `spent`, `owned` and `equipped`. The balance is XP minus spent; the level uses lifetime XP, so shopping never lowers it.
- `ui/lou/LouLook.kt` draws hats and glasses on Lou and swaps his colors. They move with his body, so they bounce, squash and tilt with him.
- `ui/home/ShopScreen.kt` is the shop. Debug builds accept `--ei debug_xp 1500` and `--es route shop` for testing.

## Look
- Everything is clay (`ui/aero/Clay.kt`). `claySurface` draws a contact shadow, a darker base, a matte body lit from the top left, grain and a soft highlight, and `press` sinks it onto the base. `glass()` cards, `GelButton`, `GelOrb`, `ClayIconButton`, `ClayChip`, `ClayToggle` and `ClayDialog` all use it. The FL windows are not clay: `flBox` keeps FL's flat dark buttons with a hairline edge and FL's lime when on.
- Buttons and orbs are clay: a matte body on a darker base with fine grain, and they squish down onto the base when pressed (`GelButton`, `GelOrb` in `ui/aero/Aero.kt`). Learn path bubbles use the clay images in `res/drawable-nodpi` (green tick when done, orange star when playable, grey when locked), and a lesson you just finished pops in.
- The main menu (Welcome and the Home tabs) uses a light sky-blue ripple image with bubbles floating up over it. Panels and buttons there are frosted glass: each one shows a blurred copy of the image behind it.
- Ear lessons use a sky gradient with the same bubbles, and the Studio uses dark FL-style colors.
- The font is Open Sans (OFL). Screen changes use spring fades.

## Rotation
- Every screen has its own landscape layout. In the Studio, the FL windows sit on the left and Lou's coach is a side panel. Home uses a navigation rail, and the ear lessons, Welcome and Lesson complete screens go side by side.
- Turning the phone mid-lesson keeps your progress. The app respects the rotation lock.

## Export
MIDI goes into FL Studio through Piano roll → File → Import MIDI file. WAV is for sharing.

## Build and test
```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug testDebugUnitTest
```
The unit tests cover:
- MIDI export
- every lesson's setup and checks, with passing and failing answers
- the sound presets
- the shop's level math and the mystery box
- every Music basics question (right and wrong answers), and that each chord, scale and "X + n" sum is correct
- the practice generators, melody dictation and the spaced-review schedule

The UI check scripts are in `tools/ui-check/`.

To open a screen directly, pass its route: `adb shell am start -S -n com.beatpath.app/.MainActivity --es route <lesson id | basics.note | ear.high-low | practice | practice.mix | practice.dictation | home | studio | welcome | complete>`.

## Smoothness rules
- Lou's bubble and the bottom buttons keep a fixed space, so nothing moves under your finger after Check. The bubble eases to its new size and its words cross-fade.
- The result tray slides in and out with no bounce and blocks taps underneath. While it shows, the answer can't be changed.
- Moving to a new step or page resets everything in the same frame, and undo never reaches into the previous step.
- A missed answer: the tray says what's wrong and where, the right choice gets a green outline, and Back works like "Try again". Single picks clear; chords and roll notes stay so you can fix them.
- Hints have up to three different rungs and the button greys out when they're used up.
- Only one sound plays at a time, and leaving a lesson never leaves app sounds muted.

## Phones
- Audio focus: calls and other apps stop playback (`AudioEngine.interrupt`), and so does unplugging headphones.
- Calm motion (Me tab) stops the bubbles and keeps Lou still. It starts on if the phone's animations are off.
- Crash reports: `data/CrashLog.kt` writes a report file on a crash. The Me tab then offers to share it through the share sheet or delete it.

## Known gaps
- On phones narrower than about 400dp, Channel Rack step pads are about 30dp wide, like FL's compact grid. Every pad is 48dp tall.
- The app has been tested on an emulator at phone size (1080×2340 at 440dpi) in portrait and landscape. Audio latency hasn't been measured on a real phone.
