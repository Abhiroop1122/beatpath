# UI/UX research brief: Duolingo grammar, FL Studio fidelity, touch music UX, Android/M3

Research date: 26 Nov 2026. Companion to `plan.md` §4 (Design language) and §6 (Studio).

**Method and confidence.** The built-in web search and fetch tools were down for this pass, so pages were pulled directly from their sources. Three confidence tags are used:

- **[V]** Verified from a primary source fetched today: an official manual, docs, a company blog, or Duolingo's own production web CSS.
- **[V-css]** Read from duolingo.com's production stylesheets (`d35aaqx5ub95lt.cloudfront.net/css/*`). These describe the **web** app. The Android app is native, so treat these values as strong proportions, not exact Android dp.
- **[U]** Unverified. It comes from prior knowledge or observation and needs a device check before we rely on it.

Nothing here is meant to be copied. Section 6 lists the imitation risks.

---

## 1. Key findings

### 1.1 Duolingo: design language and screen anatomy

**What "premium" is made of (their own words)**
- **Craft is a deliberate system.** The 2026 core-tab refresh gave headers tiered sizes with consistent title positions, used "a minimal number of [type] styles", and let whitespace define structure "instead of forcing containers around" components. It also added design-overlay self-QA and dogfooding across languages and devices. [V] https://blog.duolingo.com/core-tabs-redesign/
- **Characters are simple, geometric, big-eyed and quirky.** Each new character was built from Duo's shape language: "very geometric", big eyes, and a distinctive body shape. [V] https://blog.duolingo.com/building-character/
- **The path.** It is a single guided path of circular levels grouped into units. A unit header describes what the unit teaches ("get directions", not "City 3"), and tapping a level opens a popover with the topic. Practice and Stories sit inline. A floating arrow jumps back to your position after scrolling. The legendary (gold) state applies per unit. [V] https://blog.duolingo.com/new-duolingo-home-screen-design/
- **Streak.** Separating "streak" (one lesson a day) from "daily goal" raised D14 retention by 3.3% and new-learner streak share by 19%. Milestones are "power-up" moments: a phoenix transformation animation plus a share card. Timing went through several rough passes before refinement. [V] https://blog.duolingo.com/improving-the-streak/ , https://blog.duolingo.com/streak-milestone-design-animation/
- **design.duolingo.com now 301-redirects** to `blog.duolingo.com/hub/design/`. The brand-guidelines site cited in plan.md [20] is no longer a standalone guide. [V]

**The 3D "lip" button (production CSS) [V-css]**
- **Primary button.** Height **50px**, radius **16px** (the root token overrides a 12px default), lip **4px**. The lip is drawn as `box-shadow: 0 4px 0 <darker shade of the same hue>` on a pseudo-layer behind the face.
- **Pressed state.** The face moves down `translateY(lip)` and the shadow goes to `none`, so the button physically sinks into its lip. The transition covers only `transform` and `box-shadow`. Hover is `brightness(1.1)` (web only).
- **Secondary/outline button.** White face, 2px border in a light grey, 2px lip in the border color, and it sinks 2px when pressed.
- **Disabled state.** It renders flat: the pressed transform with no lip and a grey fill. A disabled Check is visibly "not raised yet".
- **Label.** Weight 700, 16px, **uppercase**, letter-spacing **0.04em**, in their proprietary `duolingo-sans` font.
- **Color tokens are named pairs, a fill plus a darker lip:** macaw/whale (blue), owl/tree-frog (green), cardinal/fire-ant (red), bee and fox (yellow and orange). One example is a macaw fill with a `0 4px 0 #048fd1` lip. The principle to keep: **every fill token has a paired, darker lip token of the same hue.**
- **Path node (web).** A **70×57px ellipse** (wider than tall) with an **8px lip**: the level color darkened 20% with black. It sinks 8px on press. Path micro-animations run **0.3s ease-out**, with some at 0.6s ease-in-out and a 1.2s linear shine.

**Lesson/challenge screen anatomy**
- **Top to bottom:** exit (X), a segmented/continuous progress bar, an optional character with a speech bubble, the prompt, one answer surface, and a fixed footer holding Skip/Check. [V for the footer CSS; U for exact mobile spacing]
- **Feedback footer (web) [V-css].**
  - Minimum height **140px**, padding **24px 16px**, with a **2px top border**.
  - Neutral: white with a light grey border.
  - Correct: pale green tint with a mid-green top border, dark-green text, and a green "Continue" button.
  - Incorrect: pale pink tint with a pink border, red text, and a red button.
  - The tray **replaces the Check footer in place**: same region, one next action.
- **Mobile tray (observed) [U].** It springs up from the bottom with an icon plus a headline ("Nice!"/"Correct solution:") and a full-width result-colored Continue. Sound and haptic fire together.

**Character animation with Rive**
- Duolingo animates its world characters in a **Rive State Machine**. One state machine runs **pose/emotion states and mouth (viseme) states in parallel**, with 20+ mouth shapes per character. Speech timing drives the mouth inputs, idle states cover blinks, nods and eyebrows, and the outcome of a challenge moves the character to a final **reaction state**. A single runtime file stays small enough for Android, iOS and Web. [V] https://blog.duolingo.com/world-character-visemes/
- **"Modular actor".** Reusable motion components are layered: eyes, brows and mouth combine into an expression. 8 head × 8 body idle animations combine into 64+ idle variations. Inputs are "named logically for developers", and the Rive file is handed off weekly ("Riv Deliv"). [V] https://rive.app/blog/duolingo-s-ai-powered-video-call-brings-lily-to-life
- Duolingo hires **Creative Technologists** to own state-machine logic between animators and engineers. [V] https://rive.app/blog/creative-technologists-duolingo-s-solution-to-the-designer-to-developer-handoff
- Duolingo Math moved hard-to-draw interactive visuals from Swift drawing code to **Rive with programmatic inputs** for cross-platform parity when it shipped on Android. [V] https://blog.duolingo.com/developing-math/
- **Rive platform change (important).** Rive's docs now say state-machine **Inputs are legacy**: "For new projects: Use Data Binding instead". Data Binding means View Models with bound properties. On Android, the **Compose API is in beta**; the View-based API is the mature, "legacy" one. [V] https://rive.app/docs/editor/state-machine/inputs , https://rive.app/docs/runtimes/android/android , https://rive.app/docs/runtimes/data-binding

**Duolingo Music and Math**
- **Music** (launched October 2023) uses an on-screen piano keyboard for every lesson. Skills progress from finding notes on the keyboard, to finding them on a staff, to playing full songs. It covers rhythm, note names and ear training (high vs low). [V] https://blog.duolingo.com/music-course/
- **[U] Music UI details** (observed, not documented):
  - Keys are labelled and color-coded per note name, and the same colors appear on staff noteheads.
  - Rhythm items use one big tap pad, with notes scrolling toward a hit line.
  - Songs play with backing audio and a "follow the highlighted note" mode.
  - Verify these against the live app before borrowing any of them, and do not reuse the color-per-note mapping verbatim.

### 1.2 FL Studio (desktop and Mobile)

**Window and tool shortcuts** (Image-Line manual) [V] https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/basics_shortcuts.htm
- **Windows:** F5 toggles Playlist, F6 toggles Step Sequencer (Channel Rack), F7 toggles Piano roll, F8 opens the Plugin Picker, F9 shows/hides the Mixer, Alt+F8 shows/hides the Browser. Space starts/stops playback and **L switches Pattern/Song**.
- **Piano roll tools:** P Draw (pencil), B Paint, D Delete, E Select, T Mute, C Slice, Y Playback, Z Zoom.
- **Piano roll editing:**
  - Right-click deletes; Del deletes the selection.
  - Alt bypasses snap; Backspace toggles global snap.
  - Alt+V toggles Ghost channels; Alt+Q quantizes.
  - PgUp/PgDn zooms. Double-click on a note opens its properties.
- **Ctrl+B is context-dependent.** In the Piano roll and Playlist it is "Duplicate selection to the right" ✔. In the Record/Transport group the same keys mean "Toggle blend notes", so scope the chip to the Piano roll.
- **Transpose.** The Piano roll menu lists **Shift+↑/↓ = one semitone** and **Ctrl+↑/↓ = one octave**. [V] https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/pianoroll_menu.htm (plan.md says plain ↑/↓ for a semitone; see §5.)

**Snap** (the Global/Local snap list) [V] https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/toolbar_panels.htm
- Options: **Line** (nearest gridline; resolution follows zoom), **Cell**, **(none)**, **Steps 1/6–1 (step)**, **Beats 1/6–1 (beat)**, and **Bar**.
- Mapping to note values: 1 beat = quarter, 1/2 beat = eighth, 1/4 beat = sixteenth (= 1 step at the default 4 steps per beat), 1/3 beat = triplets, 1/2 step = 32nd, 1/4 step = 64th.
- So the plan's **Beat / ½ beat / Step** names are faithful. FL's default "Line"/"Main" snap changes with zoom, and our fixed snaps are a deliberate simplification to point out in a desktop mission.

**Middle C.** The piano roll scripting reference says note 60 "corresponds to C5 (which is like the MIDI standard, but an octave higher)". [V] https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/pianoroll_scripting_api.htm

**Channel Rack** [V] https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/channelrack.htm
- Each row reads left to right: Mute, Pan, Volume, Mixer-track target, Channel button, Selector/activity light, then **step buttons or a Piano roll preview**.
- Pattern length runs from 1 to 512 steps.
- Swing delays the even 16ths.
- The Graph Editor (Ctrl+K) holds per-step velocity, pan and similar.
- "Mute removed steps" keeps a removed step's data as a muted note, which is a nice data model for undo.
- **[U] Alternating shading.** Step buttons are shaded in groups of 4 (one beat); the default skin alternates a grey/darker group with a reddish group. This is visible in screenshots but not stated in the manual text. It is also a generic drum-machine convention (TR-style color groups), so the imitation risk is low as long as we use our own colors.

**FL Studio Mobile** has no desktop tool palette. Editing is gesture-plus-popup. [V] https://www.image-line.com/fl-studio-learning/fl-studio-mobile-online-manual/html/plugins/FL%20Studio%20Mobile_Editors.htm
- **Piano roll:**
  - Tap an empty cell to add a note.
  - Tap a note to open Copy/Delete/Snap controls.
  - **Tap-and-hold a note to delete it** (through a pop-up).
  - To select, hold on the background until a selection rectangle shows, then drag.
  - The **Snap icon toggles on short tap; long-press opens its options.**
  - Velocity and Pan live in a **drag-up panel** where you drag a bar per note.
  - The selection's More… menu has Transpose, ±1 semitone controls, "Double Up/Down" (octave copy), Set length and Snap-to-scale.
- **Drum (step) sequencer:**
  - Tap steps to toggle them.
  - There are fill helpers (every step, every 2, every 4).
  - Per-step Velocity/Pan/Pitch sit in a drag-up bar editor.
  - Swing is a slider.
  - A separate "Note track" handles off-grid timings.

### 1.3 Touch music-making references

| App | What it does well (touch step-seq / piano roll) | Source |
|---|---|---|
| **GarageBand iOS: Beat Sequencer** | **Explicit edit modes** under the grid: Step On/Off, Velocity, and others. **Drag across the grid in any direction to paint steps on or off.** In Velocity mode you drag up/down on a step; **touch-and-hold opens an expanded view** for precise drags. Patterns longer than 16 steps show a **section overview above the grid**; tap a section to edit it. Rows are reordered by hold-dims-then-drag. Mute/solo appear when you drag the row header right. | [V] https://support.apple.com/guide/garageband-iphone/create-drum-patterns-with-the-beat-sequencer-chs2fd3ab74e/ios |
| **GarageBand iOS: note editor** | Pan with a one-finger swipe left/right/up/down and **pinch to zoom**. **"Add Notes" is a spring-loaded mode:** hold the button and tap the grid, or **drag the button right to lock it.** Length uses a double-arrow handle on the last selected note. Full VoiceOver editing works through rotor actions (Move, Length, Velocity, Copy, Paste). | [V] https://support.apple.com/guide/garageband-iphone/edit-notes-in-regions-chsf2f99dfe/ios , https://support.apple.com/guide/garageband-iphone/use-voiceover-to-edit-notes-in-regions-chs0bc818790/ios |
| **Ableton Note: MIDI editor** | Tap to add a note, whose **length matches the previous note**. Hold and drag to draw a note of any length. Drag a note's **right edge** to resize it. **Tap a note to delete it.** Long-press selects and opens an **editing menu of buttons**: Nudge ←/→, Shorten/Extend by the current grid division, Transpose, Quantize, Velocity. A **velocity slider sits inside each selected note**, and saturation shows velocity. Two-finger pinch zooms; swiping pans. An **"In Key" keys layout shows only scale notes as identical rectangles**, and the pads offer a Fourths or Sequential layout. Settings include a **High Contrast theme** and "Show Touches". | [V] https://www.ableton.com/en/note/manual/ |
| **FL Studio Mobile** | Tap to add a note; hold to delete it. Snap toggles on tap and opens options on hold. A drag-up velocity lane. | [V] (above) |
| **BandLab (mobile)** | A piano-roll **Pencil (draw) tool**: with it on, a tap places or removes a note. Double-tap a region to open the MIDI editor. The time grid's labels change with zoom. On Android, **"Audio Safe Mode"** works around driver problems. A **latency test** keeps recordings in sync. Bluetooth is "impractical" for recording but fine for listening. | [V] https://help.bandlab.com/hc/en-us/articles/360022659314-Editing-MIDI-Regions , https://help.bandlab.com/hc/en-us/articles/115002959414-Fixing-Latency , https://help.bandlab.com/hc/en-us/articles/59503001825945-Audio-Safe-Mode-Android-Only |
| **Koala Sampler** | Its "no brake pedal" philosophy is to keep people making music instead of micro-editing: "like drawing with a permanent marker". Big pads, a minimum of menus. | [V] https://www.koalasampler.com/how-it-was-made/ |
| **Yousician** | Learning path with "jump ahead". The audio settings expose buffer size (latency vs stability). The app **detects increased latency and disables the mixer.** Bluetooth adds latency. | [V] https://support.yousician.com/hc/en-us/articles/203751452-Game-tab , https://support.yousician.com/hc/en-us/articles/202893381-Can-I-use-Bluetooth-headphones |
| **Melodics** | A guided path that "always has your next lesson queued", plus stars, streaks and stages. | [V] https://melodics.com/ ; **[U]** per-note early/late timing feedback and tempo-ramp practice |
| **Simply Piano** | **[U]** A wait-for-the-correct-note mode before a tempo mode, and notes that turn green/red as they pass a play line. | not verified |
| **Groovepad** | **[U]** A color-coded pad grid (one color per sound category) with quantized launch, so an instant "sounds good" moment comes with no timing skill. | not verified |

**Known pitfalls (documented)**
- **Bluetooth audio latency** is unavoidable and affects every app (Koala, BandLab, Yousician). [V] https://www.koalasampler.com/help/audio-interfaces/the-sound-is-delayed-with-my-bluetooth-headphones-speakers.html
- **Android output latency varies widely by device.** [V] https://www.koalasampler.com/help/android/why-is-there-so-much-latency-on-my-android-device.html
- **OEM three-finger gestures** (Xiaomi/Poco screenshot) steal multi-touch. [V] https://www.koalasampler.com/help/android/i-cant-use-more-than-2-fingers-on-my-poco-xiaomi-phone.html
- **Uninstalling an Android app deletes its data**, so local-only projects are lost on reinstall unless they are backed up. [V] https://www.koalasampler.com/help/android/important-note-about-android.html

### 1.4 Android and Material 3

- **Touch targets.** At least **48×48dp (about 9mm), separated by 8dp or more**. The visual element can be smaller if padding extends the target. [V] https://support.google.com/accessibility/android/answer/7101858 , https://developer.android.com/guide/topics/ui/accessibility/apps
  - WCAG 2.2 AA's floor is **24×24 CSS px** with spacing exceptions. [V] https://www.w3.org/WAI/WCAG22/Understanding/target-size-minimum.html
- **Edge-to-edge.** It is enforced for apps targeting API 35. On **Android 16 (API 36)** the opt-out (`windowOptOutEdgeToEdgeEnforcement`) is deprecated and disabled, so every screen must handle insets. [V] https://developer.android.com/about/versions/16/behavior-changes-16
- **Predictive back.** For apps targeting API 36 on Android 16+, the system animations (back-to-home, cross-task, cross-activity) are **on by default**, `onBackPressed` is **not called**, and `KEYCODE_BACK` is not dispatched. You must use `OnBackPressedCallback` / Compose `PredictiveBackHandler` or temporarily opt out. [V] same page; https://developer.android.com/develop/ui/compose/system/predictive-back
- **Back gesture vs. edge drags.** Inward swipes from the left and right edges are Back. Apps must opt specific regions out with `setSystemGestureExclusionRects` (Compose: `Modifier.systemGestureExclusion`). [V] https://developer.android.com/develop/ui/views/touch-and-input/gestures/gesturenav
  - [U] The system caps exclusion to about 200dp of height per edge.
- **Haptics.**
  - Prefer `HapticFeedbackConstants` for standard interactions, and `VibrationEffect.Composition` primitives such as `PRIMITIVE_TICK`, `LOW_TICK`, `CLICK`, `THUD`, `SPIN` and `QUICK_RISE` for rich effects.
  - **Check `areAllPrimitivesSupported`.**
  - Avoid legacy one-shot vibrations.
  - **"Given the choice of buzzy haptics or no haptics for touch feedback, choose no haptics."**
  - Escalating tick sequences suit drag, snap or drop interactions.
  - [V] https://developer.android.com/develop/ui/views/haptics/haptics-principles , https://developer.android.com/develop/ui/views/haptics/custom-haptic-effects
- **Motion.** Compose Material 3 (1.5+) ships `MotionScheme.expressive()`, which is "recommended for prominent UI elements and hero interactions", and `MotionScheme.standard()` for "utilitarian UI elements and recurring interactions". Each provides spatial and effects specs at fast, default and slow speeds. [V] https://developer.android.com/reference/kotlin/androidx/compose/material3/MotionScheme
- **Dark theme.**
  - Baseline surfaces are dark grey, not black, and use tonal surface colors; elevation overlays are deprecated in favor of tonal surfaces. [V] https://github.com/material-components/material-components-android/blob/master/docs/theming/Dark.md
  - Desaturate accents on dark surfaces. [U, M2 guidance]
  - **Non-text contrast** for UI components and states must be **≥3:1** against adjacent colors. [V] https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html

**Contrast check of the plan.md palette (computed WCAG ratios)**

| Pair | Ratio | Verdict |
|---|---|---|
| Violet `#6C4BFF` on white / paper | 5.1 / 4.9 | ✔ text |
| Violet on Studio ink / panel | 3.6 / 3.3 | UI components only, **not body text** |
| **Mint `#19C98A` on white / paper** | **2.2 / 2.0** | ✘ fails even 3:1, so it can't be text or a thin icon on Learn |
| **Amber `#FFB020` on white / paper** | **1.8 / 1.7** | ✘ fails |
| White text on Mint / Amber | 2.2 / 1.8 | ✘ fails |
| Mint / Amber / Sky on ink `#15121F` | 8.6 / 10.1 / 8.4 | ✔ |
| Rose `#F0476A` with white text | 3.6 | Large text only |
| Grid `#231D33` vs panel `#1F1A2C` vs ink | 1.04–1.14 | Effectively invisible, so lines and pads need their own contrast |

---

## 2. Design implications for our app

**Buttons, depth and shape (Learn)**
1. **Lip button.** The face is a solid fill. A **4dp lip** sits underneath, drawn as a separate shape offset downward in the **paired darker token** of the same hue (about 20–25% darker; define the pair, e.g. `violet` + `violetLip`). On press the face **translates 4dp down and the lip collapses to 0**: a `quick` spring for transform and lip only, no scale. Release springs back with low bounce. Fire the haptic on press-down, not release.
2. **Check button.**
   - Full width minus a 16dp gutter on each side, **52dp face + 4dp lip (56dp total)**, **16dp radius**, bold label at 16–17sp.
   - Disabled state: rendered **flat (no lip), neutral grey**. It "rises" with a spring when the answer becomes submittable.
   - Use **sentence case, not caps**. Uppercase with 0.04em tracking is a Duolingo signature (see §3), and sentence case localizes better.
   - Sit above the gesture inset: `windowInsetsPadding(navigationBars)` plus 16dp.
3. **Secondary buttons** (Hear mine, Skip, Hear the difference): white face, 2dp border, **2dp lip** in the border color, sink 2dp on press.
4. **Cards and choice tiles** (audio pads, sound picker): 2dp border plus a 4dp lip. When selected, the border and lip switch to Violet, the fill tints by about 8%, and a check glyph appears. Never rely on color alone.
5. **Tokens come in pairs:** every saturated fill (Violet, Mint, Amber, Rose, Sky, Teal, Coral, plus each unit color) ships a `*Lip` and an `*Ink` (text-safe) variant. **Mint and Amber are fills only on Learn.** Their text and icons use darker inks (e.g. Mint-ink about `#0B7A54`, Amber-ink about `#8A5A00`; contrast-check them) on tinted trays.

**Path**

6. **Nodes.** Make them about **72×64dp** (≥48dp target), **wider than tall**, with a **6–8dp lip**. The press sinks the full lip. To avoid Duolingo's ellipse, shape nodes as **rounded-square "pads" (corner about 22dp) that light like step buttons**: our identity, and a direct echo of the Channel Rack.
7. **Path rhythm.** Nodes sit on a gentle zig-zag with 12–16dp vertical gaps. The current node gets a "Start" speech-bubble tooltip and Kiko beside it. A **floating "jump to current" FAB** appears once you scroll away. The unit header shows a descriptive goal ("Meet pitch", not "Unit 1").

**Lesson screen and feedback tray**

8. **Anatomy, top to bottom:** Exit (48dp target), a progress bar (**12–16dp tall**, full radius, Violet fill with a 4dp lighter highlight stripe, springs on each step; [U] on Duolingo's exact height), the prompt and character, **one** answer surface, and the fixed footer.
9. **Feedback tray.**
   - Springs up from the bottom using a low-bounce spring (`MotionScheme.expressive` spatial, or about 300–350ms) and **replaces** the Check footer, so there's never a second sheet.
   - Size: minimum height **about 140dp** plus the nav inset, 24dp top padding, 16dp sides, and a 2dp top border in the state's lip color.
   - Contents: icon, headline and one-line explanation in **ink color**, plus one full-width lip button (Continue / Try again) in the state color with a dark label.
   - Correct uses a **Mint tint** tray; a miss uses an **Amber tint** tray, with the shape and icon differing as well as the color.
10. **Feedback timing.** On Check, the sound and haptic land in the same frame as the tray starts moving. The cast reaction starts within 100ms. The tray headline is readable by about 250ms. "Continue" is never auto-advanced.

**Characters and motion**

11. **Build the cast in Rive using Data Binding (View Models), not legacy Inputs.**
    - Model it as a `CharacterVM` with an enum `mood` (idle, listen, talk, correct, miss, celebrate, sleep), number `beatPhase`, trigger `downbeat`, and string `bubbleText`.
    - Use the mature View-based runtime via `AndroidView` until the Compose API leaves beta.
    - Run **layered state machines** in the Duolingo pattern: body/emotion layer, face layer (eyes, brows), instrument-voice layer, plus randomized idle variants (for example 4 head × 4 body).
12. **Motion schemes.** Use **M3 expressive** for Learn hero moments (tray, node unlock, celebrations) and **M3 standard** for Studio (sheets, tool switches). Keep plan.md's 120/240/400ms tokens only for non-spring fades.
13. **Celebrations** are full-screen and short. The rhythm is anticipation, then burst, then settle in 1.2–2.0s, with the count-up synced to the burst. There is always a Share/Continue pair, and Continue is primary.

**Studio: Channel Rack**

14. **Pad geometry.**
    - **Portrait shows 8 steps (two beats) per page** with a full-bar mini-map above the grid (GarageBand's section overview). Pads are **36–40dp** wide with no dead gaps, which keeps the hit area contiguous, and rows are 48dp tall.
    - A **full 16-step bar is landscape-only, or for widths of about 720dp and up.** The arithmetic: 16 × 40dp + a 64dp name column + gutters ≈ 736dp.
    - Lessons in Unit 3 may show only 4 steps (one beat) at 64dp.
15. **Beat grouping.**
    - Alternate the shading of 4-step groups: groups 1 and 3 on surface A, 2 and 4 on surface B, with a **≥1.5:1 difference between A and B**. Put a **bar line ≥3:1** at each bar.
    - An active pad uses the channel color fill (≥3:1 against the inactive pad) plus a 2dp inner highlight.
    - An inactive pad is an outlined rounded square (radius 8dp).
    - The playhead column is a translucent white band that is also outlined, so it's visible on both group shades.
16. **Step gestures.**
    - Tap toggles a step. **Drag across pads paints the state of the first pad touched**, like GarageBand.
    - Long-press opens an **expanded velocity slider** above the finger (GarageBand/Note pattern).
    - Make **Velocity an explicit lane/mode** rather than a hidden gesture.
    - Each toggle gives one `PRIMITIVE_TICK` (or `HapticFeedbackConstants.CLOCK_TICK`); painting gives one tick per newly changed pad.
    - Exclude the grid from the system back gesture only while a drag is in progress, and keep **≥16dp side gutters** so the first and last pads aren't on the gesture edge.

**Studio: Piano roll**

17. **Default zoom: one bar at Beat snap.** With a 40dp keyboard gutter, a 360dp screen gives about 75dp per beat. **Row height is 32dp**, with 48dp touch targets formed from the row plus snapping tolerance. **Pinch zooms** within limits that keep the step-snap column ≥24dp. Two-finger drag pans. One finger always means the active tool.
18. **Scale-fold mode for beginners** (Note's "In Key"): show only in-scale rows (7 per octave) with the root row tinted. Turn it off for chromatic and unaided pitch checks. [U] FL's own "fold" view exists; verify the exact menu name before using the FL chip.
19. **Tools.** Keep the FL-named tool bar (Draw P, Paint B, Delete D, Select E). Add **Ableton-Note-style precision buttons** on any selection: ◀▶ nudge by snap, Shorten/Extend by snap, ▲▼ semitone, ⏶⏷ octave, Duplicate. These are the accessible and exact path, while drag is the fast one.
    - New notes inherit the **previous note's length** (Note). Resize from a right-edge handle with a hit zone of at least 16dp, extending past short notes.
    - Since FL Mobile uses tap-hold to delete, **avoid tap-hold as delete in our Draw tool.** Long-press means "Note details" everywhere for consistency.
20. **Shortcut chips must match the manual:** "Shift+↑/↓" for a semitone (not "↑/↓"), "Ctrl+↑/↓" for an octave, and Ctrl+B labelled for the Piano roll specifically. F5/F6/F7/F9 and Alt+F8 are verified. Show "Snap: Beat / ½ beat / Step" with a footnote in the FL sheet that desktop FL defaults to "Line" (zoom-dependent).

**Android platform**

21. **Edge-to-edge everywhere** (targetSdk 36):
    - Path and Studio grids scroll under transparent system bars.
    - Fixed footers (Check, tray, window bar) pad by `navigationBars` insets.
    - The Studio toolbar pads by `statusBars` / `displayCutout`.
    - Test 3-button and gesture navigation.
22. **Predictive back.**
    - From a lesson, register a `PredictiveBackHandler` that **scales the lesson card toward the path node** and, on commit, shows "Exit lesson? Your progress here is saved". A preview is fine; never discard work on a gesture alone.
    - Studio: back closes the top sheet, then the tool, then leaves Studio, and drafts are always saved.
    - Never use `onBackPressed`.
23. **Haptics.**
    - At startup, probe `areAllPrimitivesSupported(TICK, CLICK, THUD, LOW_TICK)`.
    - If the probe fails, fall back to `HapticFeedbackConstants` (CONFIRM, REJECT, CLOCK_TICK).
    - If the device only has buzzy legacy vibration, **turn UI haptics off by default and hide "Feel the beat"**.
    - Haptics are never a timing reference (already in plan.md).
24. **Targets.** Transport, tools, Check, window bar and Exit get 48dp or more. Step pads and piano cells may go down to 32–36dp visually, but with **contiguous hit areas, an always-available zoom, and the list editors** (note and step lists) as the accessible path. Document this as an intentional density exception.

**Dark theme and contrast**

25. **Studio surfaces.**
    - Increase the separation in the dark palette: ink `#15121F` → panel about `#221C30` → grid groups A about `#2A2340` and B about `#342B4D`.
    - Bar lines ≥3:1 against both (a light violet-grey at about 60% opacity).
    - Beat lines are subtler but **thicker**; step lines are decorative.
    - Re-verify all of these with a contrast tool after tuning.
26. **Text on accents.** Violet is fine as a fill under white text (5.1:1), but on dark surfaces use a **lighter violet** (e.g. about `#A08CFF`) for text and icons. Provide a **High-contrast Studio theme** (Note ships one) with brighter lines and outlines on every pad.

**Audio-related UX**

27. On the first Studio/lesson audio, **detect Bluetooth output** and show a one-time, non-blocking chip: "Bluetooth adds delay. Fine for listening; use wired or speaker for rhythm checks". Rhythm-graded items then widen their tolerance or switch to tap-free variants.
28. **Back up local projects** with Android Auto Backup and Key/Value backup, plus manual "Export loop". Tell users that uninstalling deletes local loops unless they're backed up.

---

## 3. Imitation and look-alike risks (review before beta)

- **Highest risk: Duolingo's gestalt.** That is a bright flat palette plus a rounded bold **uppercase** label font, lip buttons, a zig-zag path of **elliptical lipped nodes**, pale green/pink result trays with "Continue", an owl or green mascot and its sound cues. Any one of these is generic (the lip button is a common skeuomorphic pattern). Using all of them together reads as a clone. Our differentiators:
  - Violet brand and amber miss, never green/red as the pair.
  - Sentence-case labels.
  - Pad-shaped path nodes.
  - Instrument cast that sings (no speech or mouth visemes copied).
  - Musical, key-matched feedback sounds.
  - The dark DAW-style Studio.
- **Don't** use `duolingo-sans`/Feather, Duolingo's animal-named palette values, its tray copy ("Nice!", "Correct solution:"), the phoenix/fire streak motif, or its color-per-note key labels from Music.
- **FL Studio.**
  - Use FL's **names, shortcuts and data model** nominatively.
  - Do not reproduce the default skin: grey/orange UI, the red/grey step-button look, the fruit logo, or knob art. Beat-group shading is a generic convention; render it in our palette.
  - "Where is this in FL?" sheets use original schematics only (already in plan.md).
- **Ableton/GarageBand.** We borrow gestures and patterns only, which isn't protectable, and copy no icons or layouts verbatim.

---

## 4. UX risks to review against

1. **Portrait step-pad width.** Is 16 steps at 40dp in portrait physically impossible below about 720dp? Paging must feel seamless: auto-follow the playhead (optional), and keep pattern edits on page 2 visible in the mini-map.
2. **Gesture-edge conflicts.** A piano-roll drag starting near the screen edge triggers Back. Check gutters and exclusion rects on Pixel, Samsung and Xiaomi.
3. **Multi-touch stolen by OEM gestures** (three-finger screenshot) breaks two-finger pan/pinch plus a held note. Test it, and add a help article.
4. **Pinch vs. edit ambiguity.** The second finger lands about 50–100ms after the first. Delay the commit of a one-finger "place note" until about 80ms or until movement exceeds the touch slop, and cancel it if a second pointer appears (plan.md §6.3 already wants this; verify on-device).
5. **Deleting by accident.** Note's "tap a note to delete" and FL Mobile's "tap-hold to delete" are opposite conventions. Pick one: delete only through the Delete tool or the details sheet, plus undo with a snackbar.
6. **Contrast failures** of Mint/Amber on light surfaces, and of grid lines in Studio (see the table in §1.4).
7. **Latency and grading fairness** (Bluetooth, device variance): no timing-graded item should fail a learner purely because of output latency.
8. **Character noise vs. focus.** The Rive rig plus the tray plus sound plus haptic on every answer may fatigue users. Test a "Minimal" cast mode with long-session users; limit one large rig per screen.
9. **Disabled-Check clarity.** A flat grey Check must still read as a button, with a label and a ≥3:1 outline, and explain why it's disabled ("Place a note first").
10. **Predictive-back data loss.** Confirm that the back preview animation never commits an exit without saving, including in the middle of a gesture in Studio.
11. **Rive dependency risk.** The Compose API is in beta and Inputs are legacy. Lock the runtime version, and wrap characters behind a `CharacterView` interface so a static/Lottie fallback is possible on low-end devices.
12. **Haptic quality on low-end Android.** Buzzy motors cheapen the feel, so default those devices to off.
13. **Look-alike review.** Run a side-by-side screenshot test (path, lesson, tray, celebration) with five people who don't know the app: "Which app is this?"

---

### Verification gaps (not confirmed this pass)

- The Duolingo **Android** dp values. Only web CSS was measured: progress-bar height, tray animation timing, and mobile node size.
- Duolingo Music's key coloring and rhythm-tap UI.
- Simply Piano, Groovepad and Melodics specifics.
- FL Studio's **beat-group step shading** and the "fold" menu name. Both are visible in the product but not found in the manual text.
- The about-200dp cap on gesture-exclusion height.
- Material dark-theme `#121212` and desaturation guidance. M3 pages are JS-rendered and weren't fetchable.
