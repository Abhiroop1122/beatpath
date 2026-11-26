# Beatpath — Build Plan

Where the app is now, what to build next, and how to add lessons once the current course runs out.

---

## 1. Where we are (1.0.0)

> **0.4 shipped as 1.0.0.** It includes everything in section 2: the course-complete card, the Practice tab (Daily mix, endless drills, melody dictation) and spaced review.

| Area | State |
|---|---|
| Course | 81 lessons: 5 ear lessons plus 76 Studio lessons in 18 levels, from your first beat to writing an 8-bar hook with harmony. |
| Studio | FL-style Channel Rack (4 drum kits) and Piano roll (an instrument picker for each channel). MIDI and WAV export. |
| Guide | Lou: a "Today's idea" card, word chips that explain terms, a 3-step hint ladder, "Back to the task", and theory and FL tips. |
| Look | Frutiger Aero: sky-blue menu art with floating bubbles, glass panels, and compact gel buttons. Full portrait and landscape layouts. |
| Quality | 23 unit tests. A UI check script in `android/tools/ui-check/`. |
| After the course | ✅ **Done in 1.0:** course-complete card, Practice tab (Daily mix, 7 endless drills, melody dictation) and spaced review (1/3/7 days). |
| Not done | A signed release build, audio latency on a real phone, and audio focus (pausing for calls and other apps). |

---

## 2. When the lessons are over (✅ built in 1.0)

Right now, once you finish lesson 81, the Learn path is all checkmarks, the Studio hub's "Continue" button disappears, and nothing invites you back. Fix that before adding more content. Two cheap builds do it:

### 2a. "Course complete" moment (½ day)
- When `store.nextLesson()` returns `null`, the Learn path ends with a celebration card: Lou cheers, you see your total XP and the number of lessons you did without help.
- The card has three buttons:
  - **Practice**: the endless mode in 2b.
  - **Make a song**: free play with an 8-bar template.
  - **Redo for gold**: replay the lessons you finished with help, to earn a gold star.
- In the Studio hub, replace the "Continue" button with "Daily practice".

### 2b. Endless practice (2–3 days), so the app never runs out
These are generated, not hand-written, so they last forever:
- **Ear drills:** random pairs and chords drawn from what you've already learned (high/low, step/leap, major/minor, then intervals and the chords in a progression). `EarLessons.spec(id)` already builds items from a list. Add a generator that makes a fresh `EarSpec` with random `EarItem`s.
- **Melody dictation:** Lou plays 3 to 5 notes from a scale you know, and you copy them in the Piano roll. Check with the existing `Checks` helpers: compare pitches and start times.
- **Chord spotting:** play I–V–vi–IV-style loops, and you name the chord that changed.
- **Daily mix:** 5 items picked from your weakest skills. Track a hit rate for each skill in `Store`, which already saves to JSON.
- **Spaced review:** lessons you finished with help come back after 1, 3 and 7 days.

---

## 3. How to add more lessons (step by step)

Lessons are Kotlin data, so adding one doesn't need any new screens.

### Files
- `android/app/src/main/java/com/beatpath/app/lessons/Curriculum.kt`: levels 1–10, the helpers `n()`, `at()`, `steps()` and `drumStep()`, and the master `lessons` list.
- `.../lessons/TheoryCurriculum.kt`: levels 11–18. Copy this pattern for a new pack.
- `.../lessons/LessonModel.kt`: `Lesson`, `LessonStep`, `Ghost`, `Verdict` and the `Checks` helpers.
- `.../lessons/Teaching.kt`: glossary terms (they become "? word" chips automatically) and tip pools.

### Recipe: a new level with lessons
1. **Make a pack file.** Copy `TheoryCurriculum.kt` to, for example, `ChordsPackCurriculum.kt`, with `object ChordsPack { val levels = …; val lessons = … }`.
2. **Add the level** with the next free number:
   ```kotlin
   Level(19, "Chord voicings", "Inversions, open voicings and smooth movement.", Track.HARMONY)
   ```
3. **Write lessons.** Each one is one idea with 1 to 3 steps, and each step is a single action:
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
   - **`id`:** `area.short-name`, unique. It becomes the debug route and the progress key, so never rename it after release.
   - **`setup`:** pre-fills the project, for example a locked drum loop or a reference melody (`locked` plus `ghost` channels).
   - **`check`:** use or extend `Checks`. Return a `Verdict` with a headline and a body that says *exactly* what's wrong ("C is still at the bottom").
   - **`jam = true`:** marks an open-ended review lesson at the end of a level.
4. **Register the pack** in `Curriculum.kt`:
   ```kotlin
   val levels = listOf(...) + TheoryCurriculum.levels + ChordsPack.levels
   val lessons = l1 + … + TheoryCurriculum.lessons + ChordsPack.lessons
   ```
   The Learn path, Studio hub, Me levels and unlock order (`Store.order`) all update automatically.
5. **Add glossary words** in `Teaching.kt` for new terms (for example `t("voicing", "How a chord's notes are stacked…")`). The chips appear by themselves.
6. **Test** (see section 6):
   - Add a case to `CurriculumTest.kt` that builds the correct answer and asserts `check` passes, and that an empty or wrong answer fails.
   - Update the lesson-count assertion.
   - Open it on the emulator: `adb shell am start -S -n com.beatpath.app/.MainActivity --es route voice.first-inversion`.

### Recipe: a new ear-training drill
Add an id in `data/Store.kt → EarIds`, then a `when` branch in `EarLessons.spec(id)` with an `EarKind`, answers and items. Add a new `EarKind` only if the answer buttons differ.

### Writing guidelines (keep the teaching clear)
- One idea per lesson, one action per step, and the guided step comes before the unguided one.
- Lou's lines: under about 25 words, plain language, no jargon without a glossary entry.
- A hint names the exact place (step numbers, bar.beat, note names). A failure message says what is wrong and where.
- End each level with a jam lesson and finish every lesson with an FL tip.

### Ideas for the next packs (about 6 lessons each)
1. **Chord voicings:** inversions, open voicings, voice leading.
2. **Rhythm II:** 6/8 feel, swing amounts, polyrhythm basics.
3. **Genre grooves:** lo-fi, house, trap, pop. Use the new drum kits.
4. **Bass lines II:** walking bass, octave bass, 808 glides.
5. **Melody III:** call and response, countermelody across sections.
6. **Arranging:** intro, build, drop, breakdown. This needs Song mode, see 0.6.
7. **Ear training II:** intervals, chord progressions, rhythm dictation.

---

## 4. Suggested build roadmap

| Version | Theme | What's in it |
|---|---|---|
| ~~0.4~~ **1.0 ✅** | Never run out | Course-complete card, endless practice (ear drills, melody dictation, daily mix), spaced review. |
| **0.5** | Real-phone polish | Measure audio latency on 2–3 phones and move to Oboe/AAudio if it's over ~40 ms. Pause for calls and other apps (audio focus), headphone unplug handling, reduce-motion setting (bubbles off), a TalkBack pass. |
| **0.6** | Content pack 1 | Chord voicings and Ear training II (about 12 lessons), plus glossary updates. |
| **0.7** | Song mode lite | Chain patterns A/B/C into 8–16 bars (the data model already has `playlist` and `songMode`), and export the whole song as MIDI or WAV. This unlocks the Arranging pack. |
| **0.8** | Content packs 2–3 | Genre grooves, Bass II and Melody III. More kits and instrument presets for each genre. |
| **0.9** | Release prep | Signed release AAB, app icon variants, a privacy policy (no data leaves the phone today), opt-in crash reporting, Play Store listing and screenshots. |
| **1.0** | Launch | Internal test track, then closed beta, then production. |

Later ideas:
- downloadable lesson packs as JSON, so new lessons ship without an app update
- a weekly challenge
- sharing a loop link
- tablet layouts that use the extra width for knobs and 16 steps

### Lessons without app updates (for after 1.0)
Lessons are Kotlin lambdas today. To ship them remotely:
- Define a JSON schema for `say`, `task`, `window`, `channel`, `guides` and `hint`, plus a **declarative check** chosen from a fixed list: `drumSteps`, `notesAt`, `chordAt`, `inScale`, `contour`, `rhythmMatches`. Those map onto the existing `Checks` functions.
- Bundle packs in `assets/`, and later download them.
- Keep complex lessons in Kotlin.

---

## 5. Building and releasing

```bash
cd "android"
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug testDebugUnitTest          # debug APK + unit tests
cp app/build/outputs/apk/debug/app-debug.apk ../Beatpath-<version>-debug.apk
```

For every build:
- Bump `versionCode` (+1) and `versionName` in `android/app/build.gradle.kts`.
- Update `android/README.md`.

Release (from 0.9):
1. Create a keystore once and keep it safe. **If you lose it, you can't update the app.**
2. Add a `signingConfigs { release { … } }` block that reads from `~/.gradle/gradle.properties`, never from the repo.
3. Build with `./gradlew bundleRelease`, which makes an `.aab` for the Play Store.

---

## 6. Checking a build (every time)

1. **Unit tests:** `./gradlew testDebugUnitTest`. Every lesson's setup and checks are tested.
2. **Crash check of every lesson** in both orientations, about 4 minutes on the emulator:
   ```bash
   android/tools/ui-check/fast.sh 0
   ```
   ```bash
   android/tools/ui-check/fast.sh 1
   ```
   The lesson list is `tools/ui-check/ids.txt`. Regenerate it when you add lessons:
   ```bash
   grep -ohE 'Lesson\(\s*"[a-z0-9.-]+"' android/app/src/main/java/com/beatpath/app/lessons/*.kt | grep -oE '"[^"]+"' | tr -d '"' > android/tools/ui-check/ids.txt
   ```
3. **Layout audit with screenshots**, slower: `python3 android/tools/ui-check/sweep.py 0` (or `1`). It flags small, unlabeled or overlapping tap targets. Screenshots go to `tools/ui-check/out/`.
4. **Button walk-through:** `python3 android/tools/ui-check/ui.py "rot 0; route home; tap Studio tab; back; shot s1; crash"`. Commands are `route`, `rot`, `tap <label>`, `back`, `shot`, `scan` and `crash`.
5. **By hand on a real phone:** sound on, then on a full lesson check:
   - Hint, then "Back to the task"
   - Back from every screen
   - rotation mid-lesson
   - export MIDI and open it in FL.

### Navigation rules (keep these when adding screens)
- Every screen has a visible way back (← or ✕) and handles the system Back button.
- Back closes the topmost layer first (tray, sheet, dialog, open card), then leaves the screen.
- Leaving a lesson with progress asks first. Leaving free play with unsaved changes offers Save.
- Leaving returns you to the tab you came from (`Route.*.from`).
