# UI/UX research brief: Duolingo grammar, FL Studio fidelity, touch music UX, Android/M3

Research date: 26 Nov 2026. Companion to `plan.md` §4 (Design language) and §6 (Studio).

The built-in web search and fetch tools were down for this pass, so pages were pulled directly from their sources. Each finding carries one of three confidence tags:

- [V]: verified today from a primary source, such as an official manual, docs, a company blog, or Duolingo's own production web CSS.
- [V-css]: read from duolingo.com's production stylesheets (`d35aaqx5ub95lt.cloudfront.net/css/*`). These describe the web app. The Android app is native, so treat the values as good guides to proportion rather than exact Android dp.
- [U]: unverified. It comes from prior knowledge or observation and needs a device check before we rely on it.

Nothing here is meant to be copied. Section 3 lists the imitation risks.

## 1. Findings

### 1.1 Duolingo: design language and screen anatomy

#### What "premium" is made of, in Duolingo's own words

- The 2026 core-tab refresh treated craft as a system. Headers got tiered sizes with consistent title positions, the team used "a minimal number of [type] styles", and whitespace defined structure "instead of forcing containers around" components. The refresh also added self-QA against design overlays and dogfooding across languages and devices. [V] https://blog.duolingo.com/core-tabs-redesign/
- Characters are simple, geometric, big-eyed and quirky. Each new character was built from Duo's shape language: "very geometric", big eyes, and a distinctive body shape. [V] https://blog.duolingo.com/building-character/
- The path is a single guided path of circular levels grouped into units. A unit header describes what the unit teaches ("get directions", not "City 3"), and tapping a level opens a popover with the topic. Practice and Stories sit inline. After you scroll, a floating arrow jumps back to your position. The legendary (gold) state applies per unit. [V] https://blog.duolingo.com/new-duolingo-home-screen-design/
- Separating the streak (one lesson a day) from the daily goal raised D14 retention by 3.3% and the share of new learners with a streak by 19%. Streak milestones are "power-up" moments, with a phoenix transformation animation and a share card. The team went through several rough timing passes before refining it. [V] https://blog.duolingo.com/improving-the-streak/ , https://blog.duolingo.com/streak-milestone-design-animation/
- design.duolingo.com now 301-redirects to `blog.duolingo.com/hub/design/`, so the brand-guidelines site cited in plan.md [20] no longer exists as a standalone guide. [V]

#### The 3D "lip" button in production CSS [V-css]

- The primary button is 50px tall with a 16px radius (the root token overrides a 12px default) and a 4px lip. The lip is drawn as `box-shadow: 0 4px 0 <darker shade of the same hue>` on a pseudo-layer behind the face.
- When pressed, the face moves down by `translateY(lip)` and the shadow goes to `none`, so the button sinks into its lip. The transition covers only `transform` and `box-shadow`. Hover is `brightness(1.1)` (web only).
- The secondary (outline) button has a white face, a 2px light grey border and a 2px lip in the border color, and it sinks 2px when pressed.
- A disabled button renders flat, using the pressed transform with no lip and a grey fill, so a disabled Check looks visibly "not raised yet".
- Labels are weight 700, 16px, uppercase, with 0.04em letter-spacing, in Duolingo's proprietary `duolingo-sans` font.
- Color tokens come in named pairs of a fill and a darker lip: macaw/whale (blue), owl/tree-frog (green), cardinal/fire-ant (red), and bee and fox (yellow and orange). A macaw fill, for example, gets a `0 4px 0 #048fd1` lip. The idea worth keeping is that every fill token has a paired, darker lip token of the same hue.
- On the web, a path node is a 70×57px ellipse (wider than tall) with an 8px lip, which is the level color darkened 20% with black. It sinks 8px on press. Path micro-animations run 0.3s ease-out, with some at 0.6s ease-in-out and a 1.2s linear shine.

#### Lesson and challenge screen anatomy

- From top to bottom: exit (X), a segmented or continuous progress bar, an optional character with a speech bubble, the prompt, one answer surface, and a fixed footer holding Skip and Check. [V for the footer CSS; U for exact mobile spacing]
- Feedback footer on the web [V-css]:
  - Minimum height 140px, padding 24px 16px, and a 2px top border.
  - Neutral: white with a light grey border.
  - Correct: pale green tint with a mid-green top border, dark-green text, and a green "Continue" button.
  - Incorrect: pale pink tint with a pink border, red text, and a red button.
  - The tray replaces the Check footer in the same region and offers one next action.
- Mobile tray, observed [U]: it springs up from the bottom with an icon, a headline ("Nice!"/"Correct solution:") and a full-width Continue in the result color. Sound and haptic fire together.

#### Character animation with Rive

- Duolingo animates its world characters in a Rive State Machine. One state machine runs pose/emotion states and mouth (viseme) states in parallel, with 20+ mouth shapes per character. Speech timing drives the mouth inputs, idle states cover blinks, nods and eyebrows, and the outcome of a challenge moves the character to a final reaction state. A single runtime file is small enough for Android, iOS and Web. [V] https://blog.duolingo.com/world-character-visemes/
- The "modular actor" approach layers reusable motion components, so eyes, brows and mouth combine into an expression, and 8 head × 8 body idle animations give 64+ idle variations. Inputs are "named logically for developers", and the Rive file is handed off weekly ("Riv Deliv"). [V] https://rive.app/blog/duolingo-s-ai-powered-video-call-brings-lily-to-life
- Duolingo hires Creative Technologists to own the state-machine logic between animators and engineers. [V] https://rive.app/blog/creative-technologists-duolingo-s-solution-to-the-designer-to-developer-handoff
- When Duolingo Math shipped on Android, the team moved hard-to-draw interactive visuals from Swift drawing code to Rive with programmatic inputs to get the same result on every platform. [V] https://blog.duolingo.com/developing-math/
- Rive has changed its platform, and this matters for us. Its docs now call state-machine Inputs legacy: "For new projects: Use Data Binding instead". Data Binding means View Models with bound properties. On Android the Compose API is in beta, and the View-based API is the mature, "legacy" one. [V] https://rive.app/docs/editor/state-machine/inputs , https://rive.app/docs/runtimes/android/android , https://rive.app/docs/runtimes/data-binding

#### Duolingo Music and Math

- Music (launched October 2023) uses an on-screen piano keyboard in every lesson. Skills progress from finding notes on the keyboard, to finding them on a staff, to playing full songs. The course covers rhythm, note names and ear training (high vs low). [V] https://blog.duolingo.com/music-course/
- Music UI details, observed but not documented [U]:
  - Keys are labelled and color-coded by note name, and the same colors appear on staff noteheads.
  - Rhythm items use one big tap pad, with notes scrolling toward a hit line.
  - Songs play with backing audio and a "follow the highlighted note" mode.
  - Check these against the live app before borrowing any of them, and do not reuse the color-per-note mapping as is.

### 1.2 FL Studio (desktop and Mobile)

#### Window and tool shortcuts

Source: the Image-Line manual. [V] https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/basics_shortcuts.htm

- Windows: F5 toggles the Playlist, F6 the Step Sequencer (Channel Rack) and F7 the Piano roll. F8 opens the Plugin Picker, F9 shows or hides the Mixer, and Alt+F8 shows or hides the Browser. Space starts and stops playback, and L switches between Pattern and Song.
- Piano roll tools: P Draw (pencil), B Paint, D Delete, E Select, T Mute, C Slice, Y Playback, Z Zoom.
- Piano roll editing:
  - Right-click deletes; Del deletes the selection.
  - Alt bypasses snap; Backspace toggles global snap.
  - Alt+V toggles Ghost channels; Alt+Q quantizes.
  - PgUp/PgDn zooms. Double-clicking a note opens its properties.
- Ctrl+B depends on context. In the Piano roll and Playlist it is "Duplicate selection to the right", but in the Record/Transport group the same keys mean "Toggle blend notes", so the chip should be scoped to the Piano roll.
- Transpose: the Piano roll menu lists Shift+↑/↓ for one semitone and Ctrl+↑/↓ for one octave. [V] https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/pianoroll_menu.htm (plan.md says plain ↑/↓ for a semitone.)

#### Snap

The Global/Local snap list. [V] https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/toolbar_panels.htm

- The options are Line (nearest gridline, with resolution following zoom), Cell, (none), Steps 1/6 to 1 (step), Beats 1/6 to 1 (beat), and Bar.
- In note values, 1 beat is a quarter, 1/2 beat an eighth, 1/4 beat a sixteenth (equal to 1 step at the default 4 steps per beat), 1/3 beat a triplet, 1/2 step a 32nd and 1/4 step a 64th.
- The plan's Beat / ½ beat / Step names therefore match FL. FL's default "Line"/"Main" snap changes with zoom; our fixed snaps simplify this on purpose, and a desktop mission should point that out.

#### Middle C

The piano roll scripting reference says note 60 "corresponds to C5 (which is like the MIDI standard, but an octave higher)". [V] https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/pianoroll_scripting_api.htm

#### Channel Rack

[V] https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/channelrack.htm

- Each row reads left to right: Mute, Pan, Volume, Mixer-track target, Channel button, Selector/activity light, then step buttons or a Piano roll preview.
- Pattern length runs from 1 to 512 steps.
- Swing delays the even 16ths.
- The Graph Editor (Ctrl+K) holds per-step velocity, pan and similar values.
- "Mute removed steps" keeps a removed step's data as a muted note, which would work well as a data model for undo.
- Alternating shading [U]: step buttons are shaded in groups of 4 (one beat), and the default skin alternates a grey or darker group with a reddish group. Screenshots show this, but the manual text does not mention it. TR-style drum machines use the same color grouping, so the imitation risk is low as long as we use our own colors.

#### FL Studio Mobile

FL Studio Mobile has no desktop tool palette; you edit with gestures and pop-ups. [V] https://www.image-line.com/fl-studio-learning/fl-studio-mobile-online-manual/html/plugins/FL%20Studio%20Mobile_Editors.htm

- Piano roll:
  - Tap an empty cell to add a note.
  - Tap a note to open Copy/Delete/Snap controls.
  - Tap and hold a note to delete it (through a pop-up).
  - To select, hold on the background until a selection rectangle appears, then drag.
  - A short tap on the Snap icon toggles snap; a long press opens its options.
  - Velocity and Pan live in a panel you drag up, where you drag a bar for each note.
  - The selection's More… menu has Transpose, ±1 semitone controls, "Double Up/Down" (octave copy), Set length and Snap-to-scale.
- Drum (step) sequencer:
  - Tap steps to toggle them.
  - Fill helpers set every step, every 2nd or every 4th.
  - Per-step Velocity/Pan/Pitch sit in a bar editor you drag up.
  - Swing is a slider.
  - A separate "Note track" handles off-grid timings.

### 1.3 Touch music-making references

| App | What it does well (touch step-seq / piano roll) | Source |
|---|---|---|
| GarageBand iOS: Beat Sequencer | Explicit edit modes under the grid: Step On/Off, Velocity, and others. Drag across the grid in any direction to paint steps on or off. In Velocity mode you drag up or down on a step, and touch-and-hold opens an expanded view for precise drags. Patterns longer than 16 steps show a section overview above the grid; tap a section to edit it. To reorder rows, hold (the row dims) and then drag. Mute/solo appear when you drag the row header right. | [V] https://support.apple.com/guide/garageband-iphone/create-drum-patterns-with-the-beat-sequencer-chs2fd3ab74e/ios |
| GarageBand iOS: note editor | Swipe with one finger to pan left, right, up or down, and pinch to zoom. "Add Notes" is a spring-loaded mode: hold the button and tap the grid, or drag the button right to lock it. A double-arrow handle on the last selected note sets length. Notes can be fully edited with VoiceOver through rotor actions (Move, Length, Velocity, Copy, Paste). | [V] https://support.apple.com/guide/garageband-iphone/edit-notes-in-regions-chsf2f99dfe/ios , https://support.apple.com/guide/garageband-iphone/use-voiceover-to-edit-notes-in-regions-chs0bc818790/ios |
| Ableton Note: MIDI editor | Tap to add a note; its length matches the previous note. Hold and drag to draw a note of any length, and drag a note's right edge to resize it. Tapping a note deletes it. A long press selects the note and opens a menu of editing buttons: Nudge ←/→, Shorten/Extend by the current grid division, Transpose, Quantize, Velocity. Each selected note has a velocity slider inside it, and saturation shows velocity. Pinch with two fingers to zoom; swipe to pan. The "In Key" keys layout shows only scale notes, as identical rectangles, and the pads offer a Fourths or Sequential layout. Settings include a High Contrast theme and "Show Touches". | [V] https://www.ableton.com/en/note/manual/ |
| FL Studio Mobile | Tap to add a note; hold to delete it. Snap toggles on tap and opens options on hold. Velocity is in a lane you drag up. | [V] (above) |
| BandLab (mobile) | A Pencil (draw) tool in the piano roll: while it is on, a tap places or removes a note. Double-tap a region to open the MIDI editor. The time grid's labels change with zoom. On Android, "Audio Safe Mode" works around driver problems. A latency test keeps recordings in sync. Bluetooth is "impractical" for recording but fine for listening. | [V] https://help.bandlab.com/hc/en-us/articles/360022659314-Editing-MIDI-Regions , https://help.bandlab.com/hc/en-us/articles/115002959414-Fixing-Latency , https://help.bandlab.com/hc/en-us/articles/59503001825945-Audio-Safe-Mode-Android-Only |
| Koala Sampler | Its "no brake pedal" approach keeps people making music instead of micro-editing, "like drawing with a permanent marker". Big pads and very few menus. | [V] https://www.koalasampler.com/how-it-was-made/ |
| Yousician | A learning path with "jump ahead". The audio settings expose buffer size (latency vs stability). The app detects increased latency and disables the mixer. Bluetooth adds latency. | [V] https://support.yousician.com/hc/en-us/articles/203751452-Game-tab , https://support.yousician.com/hc/en-us/articles/202893381-Can-I-use-Bluetooth-headphones |
| Melodics | A guided path that "always has your next lesson queued", plus stars, streaks and stages. | [V] https://melodics.com/ ; [U] per-note early/late timing feedback and tempo-ramp practice |
| Simply Piano | [U] A mode that waits for the correct note, used before a tempo mode, and notes that turn green or red as they pass a play line. | not verified |
| Groovepad | [U] A color-coded pad grid (one color per sound category) with quantized launch, so the result sounds good right away and needs no timing skill. | not verified |

#### Documented pitfalls

- Bluetooth audio latency cannot be avoided and affects every app (Koala, BandLab, Yousician). [V] https://www.koalasampler.com/help/audio-interfaces/the-sound-is-delayed-with-my-bluetooth-headphones-speakers.html
- Android output latency varies widely by device. [V] https://www.koalasampler.com/help/android/why-is-there-so-much-latency-on-my-android-device.html
- OEM three-finger gestures (the Xiaomi/Poco screenshot gesture) take over multi-touch. [V] https://www.koalasampler.com/help/android/i-cant-use-more-than-2-fingers-on-my-poco-xiaomi-phone.html
- Uninstalling an Android app deletes its data, so local-only projects are lost on reinstall unless they are backed up. [V] https://www.koalasampler.com/help/android/important-note-about-android.html

### 1.4 Android and Material 3

- Touch targets should be at least 48×48dp (about 9mm) and separated by 8dp or more. The visual element can be smaller if padding extends the target. [V] https://support.google.com/accessibility/android/answer/7101858 , https://developer.android.com/guide/topics/ui/accessibility/apps
  - The WCAG 2.2 AA floor is 24×24 CSS px, with exceptions for spacing. [V] https://www.w3.org/WAI/WCAG22/Understanding/target-size-minimum.html
- Edge-to-edge is enforced for apps targeting API 35. On Android 16 (API 36) the opt-out (`windowOptOutEdgeToEdgeEnforcement`) is deprecated and disabled, so every screen must handle insets. [V] https://developer.android.com/about/versions/16/behavior-changes-16
- Predictive back: for apps targeting API 36 on Android 16+, the system animations (back-to-home, cross-task, cross-activity) are on by default, `onBackPressed` is not called, and `KEYCODE_BACK` is not dispatched. You must use `OnBackPressedCallback` / Compose `PredictiveBackHandler` or temporarily opt out. [V] same page; https://developer.android.com/develop/ui/compose/system/predictive-back
- Inward swipes from the left and right edges trigger Back, so drags that start at an edge conflict with the gesture. Apps must opt specific regions out with `setSystemGestureExclusionRects` (Compose: `Modifier.systemGestureExclusion`). [V] https://developer.android.com/develop/ui/views/touch-and-input/gestures/gesturenav
  - [U] The system caps exclusion at about 200dp of height per edge.
- Haptics:
  - Prefer `HapticFeedbackConstants` for standard interactions, and `VibrationEffect.Composition` primitives such as `PRIMITIVE_TICK`, `LOW_TICK`, `CLICK`, `THUD`, `SPIN` and `QUICK_RISE` for custom effects.
  - Check `areAllPrimitivesSupported`.
  - Avoid legacy one-shot vibrations.
  - Google's guidance: "Given the choice of buzzy haptics or no haptics for touch feedback, choose no haptics."
  - Sequences of escalating ticks suit drag, snap or drop interactions.
  - [V] https://developer.android.com/develop/ui/views/haptics/haptics-principles , https://developer.android.com/develop/ui/views/haptics/custom-haptic-effects
- Motion: Compose Material 3 (1.5+) ships `MotionScheme.expressive()`, "recommended for prominent UI elements and hero interactions", and `MotionScheme.standard()` for "utilitarian UI elements and recurring interactions". Each provides spatial and effects specs at fast, default and slow speeds. [V] https://developer.android.com/reference/kotlin/androidx/compose/material3/MotionScheme
- Dark theme:
  - Baseline surfaces are dark grey rather than black and use tonal surface colors. Elevation overlays are deprecated in favor of tonal surfaces. [V] https://github.com/material-components/material-components-android/blob/master/docs/theming/Dark.md
  - Desaturate accents on dark surfaces. [U, M2 guidance]
  - UI components and states need a non-text contrast of at least 3:1 against adjacent colors. [V] https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html

#### Contrast check of the plan.md palette (computed WCAG ratios)

| Pair | Ratio | Verdict |
|---|---|---|
| Violet `#6C4BFF` on white / paper | 5.1 / 4.9 | ✔ text |
| Violet on Studio ink / panel | 3.6 / 3.3 | UI components only, not body text |
| Mint `#19C98A` on white / paper | 2.2 / 2.0 | ✘ fails even 3:1, so it can't be text or a thin icon on Learn |
| Amber `#FFB020` on white / paper | 1.8 / 1.7 | ✘ fails |
| White text on Mint / Amber | 2.2 / 1.8 | ✘ fails |
| Mint / Amber / Sky on ink `#15121F` | 8.6 / 10.1 / 8.4 | ✔ |
| Rose `#F0476A` with white text | 3.6 | Large text only |
| Grid `#231D33` vs panel `#1F1A2C` vs ink | 1.04 to 1.14 | Effectively invisible, so lines and pads need their own contrast |

## 2. Design implications for our app

### Buttons, depth and shape (Learn)

1. Lip button: the face is a solid fill. A 4dp lip sits underneath, drawn as a separate shape offset downward in the paired darker token of the same hue (about 20 to 25% darker; define the pair, e.g. `violet` + `violetLip`). On press the face translates 4dp down and the lip collapses to 0, using a `quick` spring for transform and lip only, with no scale. On release it springs back with low bounce. Fire the haptic on press-down, not on release.
2. Check button:
   - Full width minus a 16dp gutter on each side, a 52dp face plus a 4dp lip (56dp total), a 16dp radius, and a bold label at 16 to 17sp.
   - When disabled it is flat (no lip) and neutral grey. It rises with a spring once the answer can be submitted.
   - Use sentence case instead of caps. Uppercase with 0.04em tracking is a Duolingo signature (see §3), and sentence case localizes better.
   - Place it above the gesture inset with `windowInsetsPadding(navigationBars)` plus 16dp.
3. Secondary buttons (Hear mine, Skip, Hear the difference) have a white face, a 2dp border and a 2dp lip in the border color, and sink 2dp on press.
4. Cards and choice tiles (audio pads, sound picker) have a 2dp border plus a 4dp lip. When one is selected, its border and lip switch to Violet, the fill tints by about 8%, and a check glyph appears, so the state never depends on color alone.
5. Tokens come in pairs. Every saturated fill (Violet, Mint, Amber, Rose, Sky, Teal, Coral, plus each unit color) ships with a `*Lip` variant and a text-safe `*Ink` variant. On Learn, Mint and Amber are used only as fills. Their text and icons use darker inks on tinted trays (e.g. Mint-ink about `#0B7A54`, Amber-ink about `#8A5A00`; contrast-check them).

### Path

6. Nodes: make them about 72×64dp (≥48dp target), wider than tall, with a 6 to 8dp lip. A press sinks the full lip. To stay away from Duolingo's ellipse, shape nodes as rounded-square "pads" (corner about 22dp) that light up like step buttons. That shape is our own and echoes the Channel Rack.
7. Path rhythm: nodes sit on a gentle zig-zag with 12 to 16dp vertical gaps. The current node gets a "Start" speech-bubble tooltip with Kiko beside it. A floating "jump to current" FAB appears once you scroll away. The unit header shows a descriptive goal ("Meet pitch", not "Unit 1").

### Lesson screen and feedback tray

8. Anatomy, top to bottom: Exit (48dp target); a progress bar (12 to 16dp tall, full radius, Violet fill with a 4dp lighter highlight stripe, springing on each step; [U] on Duolingo's exact height); the prompt and character; one answer surface; and the fixed footer.
9. Feedback tray:
   - It springs up from the bottom with a low-bounce spring (`MotionScheme.expressive` spatial, or about 300 to 350ms) and replaces the Check footer, so a second sheet never appears.
   - Size: minimum height about 140dp plus the nav inset, 24dp top padding, 16dp sides, and a 2dp top border in the state's lip color.
   - Contents: an icon, a headline and a one-line explanation in the ink color, plus one full-width lip button (Continue / Try again) in the state color with a dark label.
   - A correct answer gets a Mint-tinted tray and a miss gets an Amber-tinted one. The shape and icon also differ, not only the color.
10. Feedback timing: on Check, the sound and haptic land in the same frame as the tray starts moving. The cast reaction starts within 100ms, and the tray headline is readable by about 250ms. "Continue" never auto-advances.

### Characters and motion

11. Build the cast in Rive with Data Binding (View Models) instead of the legacy Inputs.
    - Model it as a `CharacterVM` with an enum `mood` (idle, listen, talk, correct, miss, celebrate, sleep), a number `beatPhase`, a trigger `downbeat` and a string `bubbleText`.
    - Use the mature View-based runtime through `AndroidView` until the Compose API is out of beta.
    - Run layered state machines as Duolingo does: a body/emotion layer, a face layer (eyes, brows) and an instrument-voice layer, plus randomized idle variants (for example 4 head × 4 body).
12. Motion schemes: use M3 expressive for Learn hero moments (tray, node unlock, celebrations) and M3 standard for Studio (sheets, tool switches). Keep plan.md's 120/240/400ms tokens only for fades that don't use springs.
13. Celebrations are full-screen and short. They run anticipation, burst and settle over 1.2 to 2.0s, with the count-up synced to the burst. A Share/Continue pair always appears, and Continue is primary.

### Studio: Channel Rack

14. Pad geometry:
    - Portrait shows 8 steps (two beats) per page, with a full-bar mini-map above the grid (like GarageBand's section overview). Pads are 36 to 40dp wide with no dead gaps, so the hit area is contiguous, and rows are 48dp tall.
    - A full 16-step bar is shown only in landscape or at widths of about 720dp and up, since 16 × 40dp + a 64dp name column + gutters ≈ 736dp.
    - Lessons in Unit 3 may show only 4 steps (one beat) at 64dp.
15. Beat grouping:
    - Alternate the shading of 4-step groups, with groups 1 and 3 on surface A and groups 2 and 4 on surface B, and keep a contrast of at least 1.5:1 between A and B. Put a bar line of ≥3:1 at each bar.
    - An active pad uses the channel color as fill (≥3:1 against the inactive pad) plus a 2dp inner highlight.
    - An inactive pad is an outlined rounded square (radius 8dp).
    - The playhead column is a translucent white band with an outline, so it shows on both group shades.
16. Step gestures:
    - Tap toggles a step. Dragging across pads paints them with the state of the first pad touched, as in GarageBand.
    - A long press opens an expanded velocity slider above the finger (the GarageBand/Note pattern).
    - Make Velocity an explicit lane or mode rather than a hidden gesture.
    - Each toggle gives one `PRIMITIVE_TICK` (or `HapticFeedbackConstants.CLOCK_TICK`); painting gives one tick for each pad that changes.
    - Exclude the grid from the system back gesture only while a drag is in progress, and keep side gutters of at least 16dp so the first and last pads are not on the gesture edge.

### Studio: Piano roll

17. Default zoom is one bar at Beat snap. With a 40dp keyboard gutter, a 360dp screen gives about 75dp per beat. Rows are 32dp tall, and the row plus snapping tolerance forms a 48dp touch target. Pinch zooms within limits that keep the step-snap column at 24dp or wider. A two-finger drag pans. One finger always uses the active tool.
18. Scale-fold mode for beginners (like Note's "In Key") shows only in-scale rows (7 per octave), with the root row tinted. Turn it off for chromatic and unaided pitch checks. [U] FL has its own "fold" view; confirm the exact menu name before using it on the FL chip.
19. Tools: keep the tool bar with FL's names (Draw P, Paint B, Delete D, Select E). Any selection also gets precision buttons in the style of Ableton Note: ◀▶ nudge by snap, Shorten/Extend by snap, ▲▼ semitone, ⏶⏷ octave, and Duplicate. The buttons give an accessible, exact way to edit, and dragging gives a fast one.
    - New notes take the previous note's length (as in Note). A right-edge handle resizes the note; its hit zone is at least 16dp and extends past short notes.
    - FL Mobile uses tap-hold to delete, so our Draw tool should not. For consistency, a long press means "Note details" everywhere.
20. Shortcut chips must match the manual: "Shift+↑/↓" for a semitone (not "↑/↓"), "Ctrl+↑/↓" for an octave, and Ctrl+B labelled as a Piano roll shortcut. F5/F6/F7/F9 and Alt+F8 are verified. Show "Snap: Beat / ½ beat / Step", with a footnote in the FL sheet that desktop FL defaults to "Line" (which depends on zoom).

### Android platform

21. Edge-to-edge on every screen (targetSdk 36):
    - Path and Studio grids scroll under transparent system bars.
    - Fixed footers (Check, tray, window bar) pad by the `navigationBars` insets.
    - The Studio toolbar pads by `statusBars` / `displayCutout`.
    - Test both 3-button and gesture navigation.
22. Predictive back:
    - In a lesson, register a `PredictiveBackHandler` that scales the lesson card toward the path node and, on commit, shows "Exit lesson? Your progress here is saved". The preview animation is fine, but a gesture alone must never discard work.
    - In Studio, back closes the top sheet, then the tool, then leaves Studio. Drafts are always saved.
    - Never use `onBackPressed`.
23. Haptics:
    - At startup, probe `areAllPrimitivesSupported(TICK, CLICK, THUD, LOW_TICK)`.
    - If the probe fails, fall back to `HapticFeedbackConstants` (CONFIRM, REJECT, CLOCK_TICK).
    - If the device only has buzzy legacy vibration, turn UI haptics off by default and hide "Feel the beat".
    - Haptics are never a timing reference (plan.md already says this).
24. Targets: transport, tools, Check, the window bar and Exit get 48dp or more. Step pads and piano cells may be as small as 32 to 36dp visually, provided the hit areas are contiguous, zoom is always available, and the list editors (note and step lists) offer an accessible alternative. Document this as an intentional exception for density.

### Dark theme and contrast

25. Studio surfaces:
    - Increase the separation in the dark palette, going from ink `#15121F` to a panel of about `#221C30`, then grid groups A at about `#2A2340` and B at about `#342B4D`.
    - Bar lines should reach ≥3:1 against both groups (a light violet-grey at about 60% opacity).
    - Beat lines are subtler but thicker; step lines are decorative.
    - Check all of these again with a contrast tool after tuning.
26. Text on accents: Violet works as a fill under white text (5.1:1), but on dark surfaces use a lighter violet (e.g. about `#A08CFF`) for text and icons. Provide a High-contrast Studio theme (Note ships one) with brighter lines and an outline on every pad.

### Audio

27. The first time Studio or a lesson plays audio, detect Bluetooth output and show a one-time chip that doesn't block anything: "Bluetooth adds delay. Fine for listening; use wired or speaker for rhythm checks". Items graded on rhythm then widen their tolerance or switch to variants that need no tapping.
28. Back up local projects with Android Auto Backup and Key/Value backup, and offer a manual "Export loop". Tell users that uninstalling deletes local loops unless they are backed up.

## 3. Imitation and look-alike risks (review before beta)

- The highest risk is Duolingo's overall look: a bright flat palette, a rounded bold uppercase label font, lip buttons, a zig-zag path of elliptical lipped nodes, pale green/pink result trays with "Continue", an owl or green mascot, and its sound cues. Each of these alone is generic (the lip button is a common skeuomorphic pattern), but together they read as a clone. What sets us apart:
  - A Violet brand with an Amber miss, and never green/red as the pair.
  - Sentence-case labels.
  - Pad-shaped path nodes.
  - An instrument cast that sings (no speech, and no mouth visemes copied).
  - Musical feedback sounds that match the key.
  - The dark, DAW-style Studio.
- Don't use `duolingo-sans`/Feather, Duolingo's animal-named palette values, its tray copy ("Nice!", "Correct solution:"), the phoenix/fire streak motif, or the color-per-note key labels from Music.
- FL Studio:
  - Use FL's names, shortcuts and data model nominatively.
  - Do not reproduce the default skin: the grey/orange UI, the red/grey step-button look, the fruit logo, or the knob art. Beat-group shading is a generic convention, so render it in our palette.
  - "Where is this in FL?" sheets use only original schematics (plan.md already says this).
- Ableton/GarageBand: we borrow only gestures and patterns, which are not protectable, and copy no icons or layouts verbatim.

## 4. UX risks to review against

1. Portrait step-pad width: is 16 steps at 40dp in portrait physically impossible below about 720dp? Paging must feel seamless. The view can optionally follow the playhead, and pattern edits on page 2 should stay visible in the mini-map.
2. Gesture-edge conflicts: a piano-roll drag that starts near the screen edge triggers Back. Check the gutters and exclusion rects on Pixel, Samsung and Xiaomi.
3. OEM gestures that take over multi-touch (the three-finger screenshot) break a two-finger pan or pinch while a note is held. Test this and add a help article.
4. Pinch vs. edit ambiguity: the second finger lands about 50 to 100ms after the first. Delay committing a one-finger "place note" until about 80ms have passed or movement exceeds the touch slop, and cancel it if a second pointer appears (plan.md §6.3 already asks for this; verify on a device).
5. Accidental deletes: Note's "tap a note to delete" and FL Mobile's "tap-hold to delete" are opposite conventions. Pick one approach: delete only through the Delete tool or the details sheet, with undo in a snackbar.
6. Contrast failures for Mint/Amber on light surfaces and for grid lines in Studio (see the table in §1.4).
7. Latency and fair grading (Bluetooth, device variance): no timing-graded item should fail a learner only because of output latency.
8. Character noise vs. focus: the Rive rig, the tray, sound and haptics on every answer may tire users. Test a "Minimal" cast mode with users who play long sessions, and allow at most one large rig per screen.
9. Clarity of the disabled Check: a flat grey Check must still read as a button, with a label and an outline of ≥3:1, and it must say why it is disabled ("Place a note first").
10. Data loss through predictive back: confirm that the back preview animation never commits an exit without saving, including partway through a gesture in Studio.
11. Rive dependency: the Compose API is in beta and Inputs are legacy. Lock the runtime version, and put characters behind a `CharacterView` interface so low-end devices can fall back to a static or Lottie version.
12. Haptic quality on low-end Android: buzzy motors make the app feel cheap, so default those devices to off.
13. Look-alike review: run a side-by-side screenshot test (path, lesson, tray, celebration) with five people who don't know the app, asking "Which app is this?"

### Verification gaps (not confirmed this pass)

- Duolingo's Android dp values for progress-bar height, tray animation timing and mobile node size. Only the web CSS was measured.
- Duolingo Music's key coloring and rhythm-tap UI.
- Simply Piano, Groovepad and Melodics specifics.
- FL Studio's beat-group step shading and the name of the "fold" menu. Both are visible in the product but were not found in the manual text.
- The cap of about 200dp on gesture-exclusion height.
- Material's dark-theme `#121212` and its desaturation guidance. The M3 pages are rendered with JS and could not be fetched.
