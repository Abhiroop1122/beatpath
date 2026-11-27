# Beatpath build plan

This covers where the app stands, what to build next, and how to add lessons.

## 1. Where we are (1.2.0)

| Area | State |
|---|---|
| Course | 125 lessons: 16 Music basics, 5 ear lessons, 98 Studio lessons in 22 levels, and 6 Ear training II lessons after level 10. Every lesson was reviewed for an 8-year-old: one idea at a time, only ideas taught earlier, every new word explained. |
| Studio tab | 5 mini games with Lou (Sky Hop, Beat Run, Balloon Pop, Chord Cake, Echo Lou), free play and the song maker (loops A, B and C arranged into 8 or 16 bars). Lessons live only on the Learn path. |
| Rewards | XP from lessons, practice and games. 3 daily quests, streak chests on days 3, 7, 14 and 30, levels and titles, and Lou's Shop (hats, glasses, extras, colors, mystery box). |
| Look | Clay everywhere outside the FL windows: cards, buttons, chips, switches and dialogs. The Channel Rack, Piano roll and transport keep FL's flat look. Clay tick and star bubbles on the path. Sad Lou with a tear when you try to leave a lesson. |
| Phones | Audio focus (pauses for calls and other apps), stop on headphone unplug, Calm motion setting, and opt-in crash reports shared only through the share sheet. |
| Quality | Unit tests for every lesson, the packs, the shop and the practice generators; `walk.py` plays every lesson like a child (hint, wrong answer, retry, leave). |
| Not done | Audio latency measured on a real phone, a release signing key, the Play Store account. |

## 2. After the last lesson (built in 1.0)

Before 1.0, finishing lesson 81 left the Learn path full of checkmarks, the Studio hub's "Continue" button disappeared, and the app had nothing new to offer. Version 1.0 added two things.

### 2a. Course complete
When `store.nextLesson()` returns `null`, the Learn path shows a card where Lou cheers and you see your XP and how many lessons you passed without help. The card has three buttons:

- Practice opens the Practice tab.
- Make a song opens free play with a blank 4-bar pattern.
- Redo for gold reopens the first lesson you passed with help, so you can earn its gold star.

The Studio hub's "Continue" button becomes "Daily practice".

### 2b. Endless practice
The app generates these exercises, so they don't run out.

- Ear drills draw random items for each skill: high or low, pitch or volume, up or down, step or leap, and major or minor. `Practice.earSpec(id)` builds a fresh `EarSpec` each time.
- In "Which chord?", Lou plays C and then F, G or A minor, and you name the second chord.
- Melody dictation plays four notes in C major in bar 1, and you write them in bar 2 of the Piano roll. `Practice.checkDictation` compares pitches and beats.
- The Daily mix is 8 questions. Skills with a lower hit rate (kept per skill in `Store`) get picked more often.
- Spaced review brings back a lesson you needed help with after 1 day, then 3, then 7, and retires it once you pass it on your own (`Practice.nextReview`).

## 3. How to add lessons

Lessons are Kotlin data, so a new lesson doesn't need a new screen.

### Files
- `android/app/src/main/java/com/beatpath/app/lessons/Curriculum.kt` holds levels 1 to 10, the helpers `n()`, `at()`, `steps()` and `drumStep()`, and the master `lessons` list.
- `.../lessons/TheoryCurriculum.kt` holds levels 11 to 18. Copy its pattern for a new pack.
- `.../lessons/LessonModel.kt` defines `Lesson`, `LessonStep`, `Ghost`, `Verdict` and the `Checks` helpers.
- `.../lessons/Teaching.kt` has the glossary terms, which become "? word" chips on their own, and the tip pools.

### Adding a level with lessons
1. Make a pack file. Copy `TheoryCurriculum.kt` to a new file such as `ChordsPackCurriculum.kt`, with `object ChordsPack { val levels = …; val lessons = … }`.
2. Add the level with the next free number:
   ```kotlin
   Level(19, "Chord voicings", "Inversions, open voicings and smooth movement.", Track.HARMONY)
   ```
3. Write the lessons. Each one teaches one idea in 1 to 3 steps, and each step asks for a single action:
   ```kotlin
   Lesson("voice.first-inversion", 19, "First inversion", "Same chord, different bottom note: move the root up an octave.", 3, 90, 1,
       show = listOf(CHORDS), rows = 55..79, snap = Snap.BEAT,
       steps = listOf(
           LessonStep(
               say = "Here's C major: C E G. Lift the C to the top.",   // Lou's bubble: short, friendly, one idea
               task = "Turn C–E–G into E–G–C.",                         // the task line under the bubble
               window = Win.ROLL, channel = CHORDS,
               guides = { listOf(Ghost(CHORDS, pitch = n("E5"), tick = 0), Ghost(CHORDS, pitch = n("G5"), tick = 0), Ghost(CHORDS, pitch = n("C6"), tick = 0)) },
               hint = "Move only the C: drag it up 12 rows.",          // hint 1. Hint 2 shows the outlines; hint 3 restates the idea
               // chordAt checks the chord's notes (pitch classes) at that time; bassPc = 4 also requires E at the bottom
               check = { p, _ -> Checks.chordAt(p, CHORDS, at(1, 1f), Scales.triad(0), "C major (first inversion)", bassPc = 4) },
           ),
       ),
       flTip = "In FL's Piano roll, select a chord and press Alt+↑ to invert it.")
   ```
   - `id` takes the form `area.short-name` and must be unique. It's also the debug route and the progress key, so don't rename it after a release.
   - `setup` fills in the project before the lesson starts, for example with a locked drum loop or a reference melody (`locked` and `ghost` channels).
   - `check` uses or extends `Checks`. It returns a `Verdict` whose body says what's wrong, for example "C is still at the bottom".
   - `jam = true` marks the open-ended review lesson at the end of a level.
4. Register the pack in `Curriculum.kt`:
   ```kotlin
   val levels = listOf(...) + TheoryCurriculum.levels + ChordsPack.levels
   val lessons = l1 + … + TheoryCurriculum.lessons + ChordsPack.lessons
   ```
   The Learn path, the Studio hub, the levels on the Me tab and the unlock order (`Store.order`) pick up the new lessons automatically.
5. Add glossary words for new terms in `Teaching.kt`, for example `t("voicing", "How a chord's notes are stacked…")`.
6. Test it (section 6):
   - In `CurriculumTest.kt`, build the correct answer and assert that `check` passes, then assert that an empty or wrong answer fails.
   - Update the lesson-count assertion.
   - Open the lesson on the emulator: `adb shell am start -S -n com.beatpath.app/.MainActivity --es route voice.first-inversion`.

### Adding a Music basics lesson
Add a `BasicsLesson` to `part1` or `part2` in `lessons/Basics.kt`. A lesson is a list of pages built with `show(…)` (explain, with a tune and keys to tap), `tap(…)` (pick a key, build a chord, or play a scale in order), `choose(…)` (multiple choice, optionally by ear) and `roll(…)` (place notes in the small Piano roll). `BasicsTest` checks every answer automatically; add the new id to `tools/ui-check/ids.txt`.

### Adding an ear-training lesson
Add an id in `data/Store.kt → EarIds` and a `when` branch in `EarLessons.spec(id)` with an `EarKind`, answers and items. You only need a new `EarKind` when the answer buttons are different. For an endless drill, add a `Practice.Skill` entry and an item generator in `Practice.item`.

### Writing guidelines
- Teach one idea per lesson and ask for one action per step. Put the guided step before the unguided one.
- Keep Lou's lines under about 25 words, in plain language. Any jargon needs a glossary entry.
- A hint names the exact place: step numbers, bar and beat, or note names. A failure message says what is wrong and where.
- End each level with a jam lesson and each lesson with an FL tip.

### Ideas for the next packs (about 6 lessons each)
1. Chord voicings: inversions, open voicings, voice leading.
2. Rhythm II: 6/8 feel, swing amounts, simple polyrhythms.
3. Genre grooves: lo-fi, house, trap and pop, using the drum kits.
4. Bass lines II: walking bass, octave bass, 808 glides.
5. Melody III: call and response, a countermelody across sections.
6. Arranging: intro, build, drop and breakdown, using the song maker.
7. Ear training II: intervals, chord progressions, rhythm dictation.

## 4. Roadmap

| Version | Theme | What's in it |
|---|---|---|
| 1.0 (done) | Practice | Course-complete card, endless practice (ear drills, melody dictation, Daily mix), spaced review. |
| 1.1 (done) | Music basics | 16 theory lessons before the ear unit, on a rainbow keyboard, with the rainbow carried into the Piano roll. |
| 1.2 (done) | Games and polish | Mini games, song maker, quests and streak chests, a bigger shop, clay UI, content packs 1 to 3, Ear training II, audio focus, a new icon, PRIVACY.md and a store listing draft. |
| 1.3 | Real phones | Measure audio latency on 2 or 3 phones and move to Oboe/AAudio if it's over about 40 ms. A full TalkBack pass on a real phone. (Audio focus, headphone unplug and Calm motion shipped in 1.2.) |
| 1.4 | Release | The owner creates the release signing key, then `./gradlew bundleRelease` for the AAB, store screenshots and the Play Console listing (draft in docs/play-store-listing.md). (Icon, privacy policy and crash reports shipped in 1.2.) |
| 2.0 | Play Store | Internal test track, then a closed beta, then production. |

Later ideas:
- downloadable lesson packs in JSON, so new lessons ship without an app update
- a weekly challenge
- sharing a loop by link
- tablet layouts that use the extra width for knobs and 16 steps

### Shipping lessons without an app update
Lessons are Kotlin lambdas today. To ship them remotely:
- Define a JSON schema for `say`, `task`, `window`, `channel`, `guides` and `hint`, with a declarative check chosen from a fixed list: `drumSteps`, `notesAt`, `chordAt`, `inScale`, `contour` and `rhythmMatches`. Each maps onto an existing `Checks` function.
- Bundle packs in `assets/` at first, and download them later.
- Keep complex lessons in Kotlin.

## 5. Building and releasing

```bash
cd "android"
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug testDebugUnitTest          # debug APK + unit tests
cp app/build/outputs/apk/debug/app-debug.apk ../Beatpath-<version>-debug.apk
```

For every build, add 1 to `versionCode`, set `versionName` in `android/app/build.gradle.kts`, and update `android/README.md`.

GitHub releases use `./gradlew assembleRelease`. That APK is signed with the local debug key so anyone can sideload it. For the Play Store (1.6):

1. Create a keystore once and keep it safe. If you lose it, you can't publish updates to the app.
2. Add a `signingConfigs { release { … } }` block that reads its passwords from `~/.gradle/gradle.properties`, never from the repo, and use it for the release build type.
3. Build with `./gradlew bundleRelease`, which makes the `.aab` the Play Store needs.

## 6. Checking a build

1. Run the unit tests with `./gradlew testDebugUnitTest`. They cover every lesson's setup and checks.
2. Open every lesson in both orientations to catch crashes. This takes about 4 minutes on the emulator:
   ```bash
   android/tools/ui-check/fast.sh 0
   ```
   ```bash
   android/tools/ui-check/fast.sh 1
   ```
   The lesson list is in `tools/ui-check/ids.txt`. Regenerate it after adding lessons:
   ```bash
   grep -ohE 'Lesson\(\s*"[a-z0-9.-]+"' android/app/src/main/java/com/beatpath/app/lessons/*.kt | grep -oE '"[^"]+"' | tr -d '"' > android/tools/ui-check/ids.txt
   ```
3. For a layout audit with screenshots, run `python3 android/tools/ui-check/sweep.py 0` (or `1`). It flags tap targets that are small, unlabeled or overlapping, and saves screenshots to `tools/ui-check/out/`. It's slow, because the screen dump waits for animations to settle.
4. To walk through buttons, run `python3 android/tools/ui-check/ui.py "rot 0; route home; tap Studio tab; back; shot s1; crash"`. The commands are `route`, `rot`, `tap <label>`, `back`, `swipe`, `shot`, `scan` and `crash`.
5. On a real phone with sound on, play a full lesson and check:
   - Hint, then "Back to the task"
   - Back from every screen
   - rotating mid-lesson
   - exporting MIDI and opening it in FL

### Navigation rules for new screens
- Every screen has a visible way back (← or ✕) and handles the system Back button.
- Back closes the topmost layer first (tray, sheet, dialog or open card) and only then leaves the screen.
- Leaving a lesson with progress asks first, and leaving free play with unsaved changes offers to save.
- Leaving returns you to the tab you came from (`Route.*.from`).
