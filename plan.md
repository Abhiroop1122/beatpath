# Beatpath (working name): a Duolingo-style Android course that teaches music production in FL Studio

**Product and build plan · Revision 2 · 26 November 2026 · Proposed scope, not an implemented app**

**Design direction: Duolingo's teaching grammar and polish, our own characters and look, and a phone-sized studio built on FL Studio's window model.** Home is a friendly lesson path hosted by a cast of instrument characters. A lesson asks one question at a time, often answered by making sound. The **Studio** (formerly "Playground") is a phone-sized FL-style workspace. It has a Channel Rack, a Piano roll, Patterns and a sound Browser, and every control shows its FL name and desktop shortcut. You learn music and FL together, so nothing has to be re-learned when you sit down at the desktop.

Your first lesson has two characters. Tink, a tiny bell sprite, sings one note. Lou, a big sleepy sub-bass creature, sings another. Which one is higher? Then the check comes: two identical pads with no character size to give it away, and you answer by ear alone. That is day one: **pitch**. By day four you have a drum loop of your own in the Channel Rack. By the end of the course you can open an empty Pattern in **FL Studio desktop** and write a four-bar hook (drums, chords and a lead with a recognizable motif and a deliberate variation) without the phone.

**Product promise:** in a few minutes on your phone, learn one idea, hear what it changes, and use it in music you own, using the same moves you will use in FL. Exporting from the phone is useful. Being able to start again in FL without the phone is the learning goal.

---

## What changed in revision 2

| Area | Revision 1 | Revision 2 |
|---|---|---|
| Visual identity | "Rounded cards, one accent per unit" | A full design language (§4): an original character cast led by **Kiko**, state-machine character animation synced to the audio clock, pressable controls, musical UI sounds, haptics, signature celebration moments, and rules that stop the characters giving away ear-training answers |
| Playground | One Pattern, two fixed piano-roll parts | **Studio** (§6): FL's window model on a phone, with a Channel Rack step sequencer, Piano roll, Pattern selector, Browser and PAT mode. Playlist arrives in v1.1 and Mixer in v1.2. F-key and tool-shortcut chips on every control, plus a **"Where is this in FL?"** sheet |
| Teaching FL | Optional transfer cards | FL skill is a course goal. Every unit has a **Studio move** and an optional **Desktop mission** with check-in questions |
| Curriculum | 10 units | **12 units** (§7). Drums arrive in units 3–4 through the step sequencer, so the first finished loop comes early. A new **chord progression** unit comes before melody, because revision 1 jumped from two chords straight to melody writing |
| Motivation | Streak strip only | XP, a gentle streak with freezes, daily quests, musical badges and gold "Solo" nodes tied to unaided mastery (§8). Hearts, leagues, gems and timers are rejected, with the reasons given |
| MVP scope | Lead + Chords | Adds Kick/Clap/Hat channels, up to four Patterns, WAV share, onboarding, skip-ahead tests and local reminders |
| Estimate | 14–23 developer weeks | **23–32 developer weeks** plus part-time illustration/animation and sound design (§11), with an explicit cut list |

---

## 1. Who it serves

The primary learner may never have made a beat or named a note. FL Studio is their destination, not a prerequisite. Someone already making beats can test out of early units. Assume one Android phone, short sessions, sound through the speaker or headphones, and occasional access to desktop FL. A desktop, MIDI keyboard, microphone, account or network connection is never required for the phone course. The starter course uses English and 4/4 time.

**Tone:** the audience is teens and adults who want to make music they would play for friends. The characters are playful but never babyish. They have studio attitude and musician humor ("That kick is *loud*. Respect."). They do not use nursery talk.

## 2. Releases and scope

| Release | Learner gets | Decision to move forward |
|---|---|---|
| **Design prototype** (clickable + one live Rive rig) | First launch, Unit 1 lesson 1, the Channel Rack lesson, a lesson-complete screen and Kiko's main states | Five beginners can say what to do on each screen without help. Nobody calls it "for kids". Kiko runs at 60 fps beside playing audio on the target phone |
| **First playable** | Unit 1 with Kiko, Tink and Lou; four-on-the-floor in the Channel Rack; one chord exercise in the Piano roll from both entry points; free play, save/reopen, single-channel MIDI export | A first-timer practices pitch without help. A tester builds and hears a triad, exits the coach without losing work, and imports the notes in FL |
| **Personal-use MVP** | 12 units × 3 lessons plus a Unit Jam for each unit. Studio with Kick, Clap, Hat, Chords and Lead channels; Piano roll with Draw, Paint, Delete and Select; up to 4 Patterns; 1/2/4-bar loops; about 12 sounds in a Browser; velocity; MIDI and WAV export; XP, streak, quests, badges and Solo nodes; local reminders; desktop missions | Complete the course offline at the learner's pace and pass the gates in §11. Distribute as a signed Android build |
| **v1.1 Groove & arrangement** | Bass channel (Lou), swing, a **Playlist** with Pattern clips and **SONG** mode up to 16 bars, the Mute tool, a home-screen streak widget | Build when learners finish hooks and ask "how do I turn this into a song?" |
| **v1.2 Sound & mix** | Padma joins. A synth panel (waveform, ADSR, low-pass filter), a **Mixer** with one insert per channel, a reverb/delay send and a master limiter meter; the Slice tool | Pilot each new unit before expanding |

**Boundary (all releases unless revisited):** no audio recording, plugin hosting, `.flp` generation, AI melody generation, leagues or social rankings, accounts or cloud sync, ads, subscriptions or in-app currency.

## 3. What the research changes

Rows 1–5 come from the revision 1 research pass. Rows 6–8 come from prior knowledge of Duolingo's product design and FL Studio's workflow. They had no fresh research pass on this date, so verify them against the owner's FL version.

| Finding | Product decision |
|---|---|
| Image-Line distinguishes an instrument Channel, the notes for that Channel in a Pattern, and placements of the Pattern in the Playlist. The Piano roll separates time snap, scale highlighting and velocity. [1–4] | Use FL's names exactly. Teach Channel → Pattern → (later) Playlist in that order. |
| Duolingo Music uses short interactive keyboard, rhythm and listening exercises. Ableton Learning Music and Learning Synths let learners manipulate beats, pitches and synth controls directly. [5–7] | Borrow the short session and the immediate sound. Here the answer is often an editable phrase that carries into free play and then into FL. |
| Composing from a basic idea is a standard teaching sequence. Spaced practice improved delayed melodic recall in one tested singing task, with no established interval for composing on a phone. [8–9] | Practice both recall and invention. Treat review intervals as pilot settings. |
| Android separates touch, output and round-trip latency, and the device, route and buffering all matter. [10–12] | Schedule playback on the audio clock. Drive the playhead **and the character animation** from the engine's reported position. Never grade live tapping in the MVP. |
| Standard MIDI Files interchange timed events, but import paths differ by DAW. [13–16] | Export ordinary `.mid` files with DAW-specific import cards, plus a separate "rebuild from memory" mission. |
| Duolingo's polish comes from a recurring cast of characters with distinct personalities, reactions to every answer, springy pressable controls, celebratory feedback and constant visible progress. Its public brand guidelines show these as a system rather than one-off decoration. [20] | Adopt the *grammar* (cast, reactions, depth, motion, visible progress) with an original cast, palette, type and shapes. Characters are instruments, so their reactions are sound as well as motion. |
| FL's main windows have fixed function-key shortcuts: Playlist F5, Channel Rack F6, Piano roll F7, Mixer F9 and Browser Alt+F8. Piano roll tools have single-letter shortcuts (Draw P, Paint B, Delete D, Select E, Mute T, Slice C). On desktop, left-click draws and right-click deletes, in both the Channel Rack and the Piano roll. [1–2, 21] | The Studio's window bar uses the same names and shows the F-key as a chip. Each tool shows its letter. Desktop missions teach the mouse conventions a touchscreen can't (right-click delete). |
| In FL, Channel Rack steps are notes. Opening a drum channel's Piano roll shows each step as a short note. A "step" is a sixteenth note in 4/4 by default. [1–2] | One data model: a step is a note one step long. Snap labels use FL's **Beat / ½ beat / Step** with note values as subtitles. |

---

## 4. Design language: Duolingo's grammar, our own identity

### 4.1 What makes the experience feel premium, translated

| Duolingo pattern | Why it works | Our version |
|---|---|---|
| One prompt, one answer surface and a fixed bottom button on each screen | Focus; no hunting | Same. The answer surface is sometimes a tiny DAW window (four step pads, one bar of Piano roll, a velocity lane) |
| A recurring cast who react to you | Emotional feedback; a miss feels safe; the app has a personality | A cast of **instrument characters**. Each one's "voice" is its sound, so a reaction is also a musical cue |
| Chunky, pressable controls with a visible depth lip and springy motion | Tactile, toy-like confidence | **Learn** screens use 4 dp depth-lip buttons that compress on press. **Studio** uses flatter, denser controls with the same press physics and haptics |
| Immediate, celebratory feedback through sound, color and motion together | Reinforcement you can feel | Feedback sounds are generated in the lesson's key and never play over prompt audio |
| Progress is always visible: progress bar, path, streak, XP | Momentum | Plus **musical** progress: your loops grow, and each unit ends with the cast performing *your* music |
| Instant loads, shared-element transitions, consistent illustration | "Premium" means nothing ever feels unfinished | Local-only data means no spinners. Node → lesson and thumbnail → Studio are shared-element transitions |

### 4.2 The cast (original characters)

| Character | What it is | Personality | Teaches / appears in | Signature reactions |
|---|---|---|---|---|
| **Kiko** (host) | A round kick drum with big eyes and a speaker-cone belly that thumps | Warm, upbeat, a bit of a sound nerd | Path host, lesson intros, the feedback tray, reminders. In Studio it shrinks to a **beat light** that bounces on every downbeat | Correct: double-thump hop. Miss: curious head tilt, "Hm, listen to beat 2." Idle: taps a foot in tempo. Sleepy when the app has been idle a while |
| **Tink** | A tiny bell sprite with a ringing halo | Quick, curious, easily excited | High pitch; the **Lead** channel | Sparkles on a high note; spins when a melody resolves |
| **Lou** | A huge, slow, low-voiced sub-bass creature | Unbothered, dry humor | Low pitch, octaves; the **Bass** channel (v1.1) | Slow approving nod; the floor wobbles on low notes |
| **Hattie** | A hi-hat whose top cymbal is a hat | Chatty, never misses a step | Rhythm, subdivision, the step sequencer | Drum-roll on "3 in a row"; counts "1-e-and-a" in speech bubbles |
| **The Triads: Ro, Thea and Fi** | Three note-birds perched on a wire | Ro is steady (root), Thea is moody (third: lifts for major, droops for minor), Fi is loyal (fifth) | Chords and progressions | Perch together on a beat for a block chord; Thea's mood visibly shows major vs minor |
| **Padma** (v1.2) | A soft pad cloud that fades in and out | Dreamy, patient | Envelopes, sound design, reverb | Fades in exactly as slowly as the attack you set |

Characters **sing their instrument sound instead of speaking**. Text appears in speech bubbles. With no recorded dialogue there is no localization cost, and ear training stays clean. The MVP ships Kiko, Tink, Lou, Hattie and the Triads (the three birds share one rig).

### 4.3 Rules for characters in lessons

1. **Teach with the cue, check without it.** In a teaching step, Tink (small, high on screen) sings high and Lou (large, low) sings low. In a **checked** step both pads are identical, with matching size, position and animation, so the answer must come from the ear. Loudness never follows pitch in either step. Every listening item gets a cue audit before publishing (§11).
2. **Characters listen when the music plays.** Ears perk, eyes close, bubbles hide. No character babble or UI sound plays during prompt audio, **Hear example** or **Hear mine**.
3. **No guilt, no mockery, no sulking.** A miss gets curiosity and a next step, never sadness. Reminders invite; they never shame.
4. **Never cover the work.** Characters live in the header and the feedback tray. In Studio they never overlap the grid, the steps or the playhead.
5. **Reduced motion** holds poses and crossfades between states. **Character visibility** can be set to Full, Minimal (Kiko only) or Off for learners who want a pure tool.

### 4.4 Motion system

- **Characters:** Rive state machines [22] driven through **data binding** (a View Model per character; Rive now treats state-machine Inputs as legacy) with shared properties: `listen`, `talk`, `correct`, `miss`, `celebrate`, `idle`, `sleep`, `beatPhase` (0–1) and a `downbeat` trigger. Layer body/emotion, face and randomized idle variants. Rive's Compose API is still beta, so wrap it behind our own `CharacterView`; the first playable uses a Compose stand-in with the same states. Beat-synced motion reads the **audio engine's reported playback position** each frame, not a UI timer. The bounce therefore lands on the beat the learner hears, including known output latency.
- **UI motion:** Compose spring specs: bouncy for buttons and nodes, low-bounce for trays and sheets. Three duration tokens: *quick* 120 ms, *standard* 240 ms, *emphasized* 400 ms. Shared-element transitions for node → lesson preview → lesson and for loop thumbnail → Studio. Predictive back on Android 14+.
- **Signature micro-interactions:**
  - **Place a note:** a squash-and-stretch pop, and the note sounds. The audition *is* the feedback.
  - **Toggle a step:** the pad lights and then bounces each time the playhead hits it.
  - **Press Check:** the button compresses and the tray springs up. Correct: a mint tray, a cast cheer, and a rising three-note figure in the lesson key. Miss: a warm amber tray, Kiko's tilt and one soft tone. The learner's edit stays intact.
  - **Three in a row:** Hattie's drum-roll chip.
  - **Lesson complete:** XP counts up next to "unassisted" and "time". A mini piano-roll thumbnail of what you made plays with a moving playhead.
  - **Unit Jam (signature moment):** the cast performs *your* loop, each character playing its own channel, on a little stage. Tap a character to solo its part.
  - **Path:** the current node pulses at 90 BPM with Kiko sitting beside it. Completed nodes stay still; gold Solo nodes shimmer once when earned.
- **Performance rules:** at most one large animated rig per screen. Off-screen rigs pause. All animation stops in the background. 60 fps everywhere, and 90/120 Hz where the display offers it without dropped frames.

### 4.5 UI sound and haptics

- UI sounds come from the same synth engine, tuned to the current lesson's key (C major outside lessons). They are short and sit about 18 dB below the music, with a separate **UI sounds** toggle.
- A UI sound never plays over prompt audio. It is queued until the audio ends or dropped.
- Haptics use Android's composition primitives (`VibrationEffect.Composition`, API 30+), with predefined effects on older devices [23]: a tick on step toggle, a click on Check, a soft thud on correct. An optional **Feel the beat** pulse thuds on downbeats. Haptics are **never** the timing reference for a graded rhythm, because haptic latency varies by device.

### 4.6 Visual tokens

Two surfaces, one system:

| Token | Learn (light, playful) | Studio (dark, DAW-dense) |
|---|---|---|
| Background | Warm paper `#FFF8EF` | Ink `#15121F` |
| Surface | White cards, 20 dp radius, 4 dp depth lip | `#1F1A2C` panels, 12 dp radius; grid `#231D33` with bright bar lines, medium beat lines and faint step lines; FL-style alternating shading every four steps |
| Brand accent | **Groove Violet** `#6C4BFF` | Same |
| Correct | **Mint** `#19C98A` | Same |
| Try again | **Amber** `#FFB020` (a miss is information, not an error) | Same |
| Destructive | **Rose** `#F0476A`, for delete only | Same |
| Channel colors | Kick/Clap/Hat warm coral family; Lead (Tink) Sky `#3DB8FF`; Chords (Triads) Violet; Bass (Lou) Deep Teal. Note color = channel color, as in FL's channel colors | Same |
| Type | Rounded geometric display face for headings (e.g. Nunito ExtraBold or Fredoka, both SIL OFL); Nunito for body | Inter with tabular figures for BPM, bars, ticks and velocity |
| Icons | Original, rounded, 2 dp stroke | Same, **always with a text label** in toolbars |

Each unit gets one saturated header color. Feedback is always readable by shape, icon and text as well as color, and contrast meets WCAG AA in both themes.

**Avoid:** Duolingo's owl, its green (`#58CC02`) and its custom typeface; FL Studio's fruit logo, orange-dominant theme, skins and plugin artwork. Use FL's name nominatively only: "Teaches skills you can use in FL Studio" plus "Not affiliated with or endorsed by Image-Line." Run a look-alike review before beta (§11).

### 4.7 Key moments storyboard

| Moment | What happens |
|---|---|
| **First launch (0–60 s)** | Kiko bounces in on a soft kick: "Hi, I'm Kiko. Let's hear something." No sign-up and no questions. One big button opens Tink and Lou singing. **First sound within 10 seconds of launch.** |
| **After lesson 1** | "Nice ears." Choose a daily goal of 3, 5 or 10 minutes. Then: "Ever made a beat? Which app do you use?" FL labels are on by default. Learners who already make beats are offered **Jump ahead** tests. |
| **Reminder opt-in** | Asked after the **second** completed lesson, with Kiko explaining what reminders look like. Never on first launch. |
| **Correct / miss** | The tray, cast reaction, sound and haptic described in §4.4. A miss always offers **Hear the difference** first. |
| **Lesson complete** | XP, unassisted count and time, the mini-roll of your work, then **Continue**. Reopening never awards XP twice. |
| **Unit Jam** | The cast performs your loop, followed by **Keep in My loops**, **Before / Mine** and **Try in FL**. |
| **Streak day** | Today's beat lights up on a seven-step strip that plays as a tiny drum pattern, so your week literally sounds like a beat. |
| **Comeback after a gap** | "Welcome back. 'My Hook' is right where you left it." Resume or take a two-minute warm-up review. No broken-streak shame screen; past practice days stay lit. |

---

## 5. Journey, screens and navigation

**Bottom navigation (always visible outside a lesson):** **Learn** · **Studio** · **My loops** · **Me**.

- **Learn:** the path, today's quests, a **Practice** hub (due reviews and "Fix my misses", built from your own recent misses) and unit guides.
- **Studio:** **Free play** or **Practice with a lesson** without leaving the instrument. Free play makes every built tool available from day one. Guided practice reveals controls as needed and remembers where you stopped.
- **My loops:** audition, name, duplicate, reopen, delete with undo and export. Each row shows a mini piano-roll thumbnail with bars, BPM, key and last edit.
- **Me:** streak calendar, badges, character outfits, Desktop missions log and settings (labels, characters, motion, sounds, haptics, reminders).

**A daily session (3–7 minutes, design target):** listen to two versions → rebuild one element → change it intentionally → hear the result against the backing → save. Next time, retrieve the same idea with a different starting note, tempo or sound.

| Screen or state | Duolingo-familiar interaction | Our version |
|---|---|---|
| Learn home | Vertical path of round nodes, a colored unit header, one highlighted next node, a mascot beside it | Header reads **Unit 1 · Meet pitch**, with a speaker button that plays a note pair. Node icons: **ear** (listen), **pencil** (make), **window** (Studio move), **loop arrows** (review), **monitor** (Desktop mission, optional), and a **stage** (Unit Jam) at the end. Kiko sits beside the current node, bouncing at the unit's tempo |
| Jump ahead | "Jump here?" placement test on a later unit | 6–8 cue-free items drawn from the unit's mastery checks. Pass to unlock everything before it. Fail to keep what you passed and start there |
| Lesson preview | Tap a node for its goal, expected time and Start | "Keep the rhythm. Change the ending. · 3 min." Audition the example. An optional unit guide pairs a short explanation with a playable example and its FL term |
| Challenge | Exit, a segmented progress bar, one prompt, one answer surface, a fixed **Check** | The answer surface can be two audio pads, four steps, a bar of Piano roll, a velocity lane or a sound picker. **Hear mine** auditions without submitting. Exit saves the exact exercise and its notes |
| Feedback | A bottom tray with the result and one next action | Fixed task: "Beat 3 is right. Shorten the note to leave a gap." Then **Hear the gap** and **Try again** with the edit intact. Open prompt: **Compare & keep**, where the learner chooses and the tray explains what their choice *does* |
| Unit finish | Celebration and the next node | The **Unit Jam** (§4.4), then **Before / Mine**, **Keep in My loops** and **Try in FL** |
| Solo node (gold) | Duolingo's harder "legendary" replay | Replay a finished unit's checks **with hints and cues off**. Passing turns the node gold and counts as mastery evidence (§8) |

**How a path node teaches (rests example):** first play two versions of the *same notes* and ask "Which leaves space before beat 3?" Then show a highlighted note to shorten. Next, hide the outline and ask for the gap on a different beat. Finally, let the learner put a rest anywhere in their own bar. If they move the note instead of shortening it, play both onsets against the click and offer a duration handle. If they succeed unaided, advance to variation rather than repeat the explanation.

---

## 6. Studio: an FL-style workspace on a phone

### 6.1 Layout

Portrait (the landscape layout gives the grid the extra width and collapses the coach into a replayable prompt chip):

```text
┌────────────────────────────────────────────────────────┐
│ ‹  My Hook        Pattern 1 ▾      PAT     96 BPM ▾     │  toolbar
│ ▶ Play  ■ Stop   Metronome   (Kiko beat light)   ↶ ↷    │
├────────────────────────────────────────────────────────┤
│                                                        │
│        Active window: Channel Rack or Piano roll       │
│                                                        │
├────────────────────────────────────────────────────────┤
│ Coach · "Put a kick on every beat."  Hear · Hint · Check│  collapsible
├────────────────────────────────────────────────────────┤
│ Channel Rack F6 │ Piano roll F7 │ Browser │ Lessons     │  window bar
└────────────────────────────────────────────────────────┘
```

Channel Rack (one bar; steps grouped by beat with FL-style alternating shading):

```text
Channel Rack · Pattern 1
 ●  Kick     [■][ ][ ][ ] [■][ ][ ][ ] [■][ ][ ][ ] [■][ ][ ][ ]
 ●  Clap     [ ][ ][ ][ ] [■][ ][ ][ ] [ ][ ][ ][ ] [■][ ][ ][ ]
 ●  Hat      [ ][ ][■][ ] [ ][ ][■][ ] [ ][ ][■][ ] [ ][ ][■][ ]
 ●  Chords   ▁▁▁▁▁▁▁▁▁▁▁▁ mini piano-roll preview ▁▁▁▁▁▁  ›
 ●  Lead     ▁▁▁ ▁▁ ▁▁▁▁▁ mini piano-roll preview ▁▁▁▁▁▁  ›
 (● mute light · tap a name for volume/pan/color/sound · › opens Piano roll)
```

Piano roll (one bar visible, with a bar navigator for the rest):

```text
Piano roll · Lead              Pattern 1 · 96 BPM       ▶  ■
Goal: Keep the rhythm. Change the last note.        Hear example
            | 1     2     3     4 | 1     2     3     4 |
G5          |                     |             ▬▬      |
E5          |       ▬▬            |       ▬▬            |
D5          |             ▬▬      |                     |
C5          | ▬▬                  | ▬▬                  |
     Draw P · Paint B · Delete D · Select E     Snap: Beat ▾    ↶
     Notes / Velocity        Ghost: Chords ☑        Hear mine · Check
```

### 6.2 Windows, tools and their FL equivalents

| Studio element | FL desktop equivalent | Phone behavior | Release |
|---|---|---|---|
| Toolbar transport | Play/Stop (Space), tempo display, metronome, PAT/SONG (L) | Large Play/Stop, tap-or-drag tempo, click on/off, loop length 1/2/4 bars. Only PAT is shown until the Playlist exists; no greyed-out SONG | MVP; SONG v1.1 |
| **Channel Rack** | Channel Rack (F6): left-click a step on, right-click off; per-channel mute, volume, pan, color; instrument channels show a piano-roll preview | Tap a step to toggle it. Long-press a step for velocity. Tap a channel name for volume, pan, color and sound. A channel with notes shows its mini-roll; tap › to open its Piano roll. The MVP channel set is fixed (Kick, Clap, Hat, Chords, Lead) with show/hide. Add/remove channels in v1.1 | MVP |
| **Piano roll** | Piano roll (F7): Draw P, Paint B, Delete D, Select E, Mute T, Slice C; snap; ghost channels; scale highlighting; velocity in the event area | Tap an empty cell to place and hear a note. Drag the body to move it and the right edge to resize. Each tool is a labelled button with its letter chip. Velocity lane below the grid. **Ghost** toggle shows other channels' notes as outlines that can't be grabbed | MVP (Mute v1.1, Slice v1.2) |
| Snap | Snap menu: Beat, ½ beat, Step (and finer) | **Beat** (quarter note), **½ beat** (eighth), **Step** (sixteenth). Changing snap affects future edits and never silently quantizes existing work | MVP |
| Scale highlighting | Piano roll scale highlighting helper | Shade the scale and mark its root, with a separate optional **snap to scale** that shows a visible on/off state. Both hidden for unaided pitch checks | MVP |
| Duplicate, transpose | In the Piano roll, Ctrl+B duplicates the selection (elsewhere the same keys do other things); Shift+↑/↓ moves a semitone, Ctrl+↑/↓ an octave | **Duplicate** button (chip: Ctrl+B); semitone (chip: Shift ↑/↓) and octave (chip: Ctrl ↑/↓) buttons on a selection | MVP |
| **Patterns** | Pattern selector; clone pattern | Up to 4 Patterns per project with New, **Clone**, Rename and Delete. Clone is how a learner keeps version A and tries version B, the FL way | MVP |
| **Browser** | Browser (Alt+F8) | Sound list by category (Drums, Keys, Plucks, Pads, Bass) with tap-to-audition, then **Use on this channel** | MVP (about 12 sounds) |
| **Playlist** | Playlist (F5): Pattern clips, Paint clips, SONG mode | Up to 16 bars, one lane per Pattern, tap to place clips, drag to move, loop region | v1.1 |
| **Mixer** | Mixer (F9): channels routed to insert tracks; sends; master limiter | One insert per channel, fader, pan, mute, one reverb and one delay send, master meter showing clipping | v1.2 |

**"Where is this in FL?"** Every Studio control has a small **FL** chip, and long-pressing the control opens the same sheet. The sheet gives the FL name, where it lives on desktop (an original schematic drawing, not a screenshot), the shortcut, and the one difference that matters. Example: "On desktop, right-click a note to delete it. Here, use Delete (D) or long-press → Delete." Chips can be hidden in settings and default to on when the learner picks FL.

### 6.3 Touch rules

- One-finger edits act only through the selected tool. Two fingers pan and pinch without adding notes, and a drag that turns into a pinch cancels the pending edit.
- Preview during a drag and commit on release. Undo the whole gesture in one step. Undo/redo, list editing and drag editing all update the same score.
- Keep zoom buttons, plus a **Note details** sheet for pitch, bar/beat/step, length and velocity.
- Limit edits to the loop. If shrinking the loop would remove notes, offer a trimmed copy or cancel.
- **Step pads:** in portrait, 16 pads across a narrow phone are too small. Sixteen pads plus the channel-name column need about 720 dp, which no phone has in portrait. So portrait always pages by half-bar ("Beats 1–2 / 3–4") with a full-bar mini-map. Landscape always shows 16.

### 6.4 Accessibility

48 dp or larger targets for transport and tools. A chronological **note list** offers Add, Audition, Move and Resize through the same commands as the grid, and a **step list** does the same for the Channel Rack. TalkBack hears position, pitch, duration, selection and tool state, but not the playhead. Support large text, high contrast, reduced motion, a visual beat pulse (Kiko's beat light or a plain pulse) and the optional haptic beat. A visual contour exercise is available when audio isn't practical. It is recorded as visual practice, not ear-training mastery. [17]

### 6.5 Lessons built into Studio

**Learn** recommends what to practice; **Studio** is where practice happens. Both entry points open the same lesson and attempt. A **Lessons** button in the window bar opens a sheet with **Resume**, **Practice again** (a fresh example) and a topic list. Picking a lesson opens the coach above the active window: one instruction, **Hear example**, **Hint** and **Check**. Collapse it for space and reopen it at the same step.

**Protect the learner's work:** starting guided practice from an existing loop first saves the loop, then opens a separate practice draft with the lesson's setup. If saving fails, the original stays open. Leaving offers **Resume later**, **Keep a free-play copy** or **Return to my loop**. Advancement comes from the same submitted check whether launched from Learn or Studio. Revisiting a completed lesson adds practice evidence without unlocking anything twice. Early listening tasks save progress without creating an empty project.

**Interaction acceptance check:** from cold launch, a first-timer can hear and retry the first pitch pair without finding a transport or naming a key. From an existing loop, start the chord lesson, request a hint, resume after closing the app, keep the chord variation and return to the original notes unchanged. Test both entry points before authoring the rest of the course.

---

## 7. Curriculum: what you actually make

**Pacing:** each numbered activity is a separate lesson of 1–4 minutes, not a checklist for one sitting. Each lesson introduces one idea, lets the learner hear or see it, practices it with a cue, then tries a fresh example without the cue. **One more example** comes before **Next lesson**; there is no timer and no pressure to advance today. A missed check recommends the specific earlier exercise. A unit's **Studio move** is learned *inside* its lessons, at the moment the task needs it. It is not a separate tool tutorial.

| Unit (learning order) | Three lessons | Studio move learned | Ready for the next idea when you can… |
|---|---|---|---|
| **1 · Meet pitch** (Tink & Lou) | ① High or low? Two pads. ② The same note soft and loud: loudness isn't pitch. ③ Does the pair go up, down or stay the same? | Tap to play, replay | Identify the higher sound in a fresh pair from **identical** pads |
| **2 · Find notes on the keys** | ① Find C beside the two black keys. ② Walk the white keys C–D–E–F–G–A–B. ③ Find the next C; Lou sings low C, Tink high C: an octave | The Piano roll's keyboard previews notes without placing them | Find a named white key in two octaves |
| **3 · Feel the beat** (Kiko) | ① Follow a pulse as Kiko bounces; tap along, ungraded. ② The same pulse faster or slower: that speed is **tempo** (BPM). ③ Four beats make a bar: put a Kick on every beat ("four on the floor") | **Channel Rack**: toggle steps, Play/Stop, tempo | Build four-on-the-floor at a new tempo and point to beat 1 |
| **4 · Split the beat** (Hattie) | ① Two hats per beat: eighth notes. ② Four per beat: FL calls each one a **step** (a sixteenth). ③ Clap on 2 and 4; leave a step empty and hear the space | Mute/solo, save, metronome. **Your first loop goes into My loops** | Build a described beat ("kick on 1 and 3, clap on 2 and 4, eighth-note hats") |
| **5 · Long and short notes** | ① Hold one pitch for 1, 2 or 4 beats: quarter, half, whole. ② Two eighths inside one beat (just like the hats). ③ A rest: leave a beat silent | A single-row length bar, a preview of the Piano roll | Rebuild a one-pitch rhythm with the requested gap |
| **6 · Open the Piano roll** | ① From the Channel Rack, open Lead's Piano roll and **Draw** one note: left is earlier. ② More rows: move a note up while keeping its start; **Delete** a note. ③ Drag the right edge to lengthen it; snap **Beat / ½ beat / Step**; undo | Draw (P), Delete (D), snap, undo. Drum steps appear here as short notes | Draw two notes at requested beats, then change one's pitch and the other's length independently |
| **7 · Steps between notes** | ① The smallest step is a semitone; black keys carry sharps and flats. ② Whole step vs half step. ③ Count the steps between two pitches, then find the same distance elsewhere on the roll | **Select** (E); transpose a semitone/octave | Move E to F by one semitone and C to D by two, on an unshaded grid |
| **8 · Keys and scales** | ① Hear a phrase return to its home note (tonic). ② Build C major from the whole/half pattern W-W-H-W-W-W-H. ③ Compare C minor with C major using the same rhythm; turn on **scale highlighting** and see it match what you built | Scale highlighting, off for checks | Find home and choose notes from the requested scale with shading off |
| **9 · Build chords by hand** (the Triads) | ① Hear C–E–G one after another, then stacked; Ro, Thea and Fi perch on one beat. ② Thea droops: lower E to E♭ to make C minor. ③ C major in bar 1 and F major in bar 2 on **Chords** | Chords channel, selection box, **Duplicate** (Ctrl+B) | Build a named major or minor triad without a stamp and switch chords on time |
| **10 · Chord progressions** *(new)* | ① Hear a four-chord loop (C–G–Am–F). Which chord feels like home? ② Build G and Am with the same recipe (Am is minor, so Thea droops). ③ Put four chords over four bars on top of your unit 4 drums, then swap F for Dm and hear what changes | **Clone pattern** to keep version A and try B | Build a four-chord progression in C from chord names and say which chord feels like home |
| **11 · Melody over your chords** | ① On Lead, play one chord tone per bar with **Ghost: Chords** visible. ② Give a three-note idea a rhythm, including a rest. ③ Repeat it with a changed ending: call and response (sample lesson D) | Ghost channels, **Paint** (B) for repeated rhythms, Duplicate bar | Write and vary a short motif while the chord part stays intact. A supplied progression is available if starting here |
| **12 · Shape your hook** | ① Change one note's velocity and hear the accent. ② In the **Browser**, compare a pluck with a pad on the same notes and listen for the release. ③ Capstone: four bars of drums, chords and a lead with call and answer; choose sounds; export MIDI and WAV | Browser, velocity lane, export | Play the hook, explain one musical choice, then rebuild a different example in desktop FL |

Each unit ends in a **Unit Jam**: one longer task that uses the whole unit, can span two sessions with save points, and finishes with the cast performing the result.

**Desktop missions** are optional, repeatable and never block phone progress. Each ends with one or two **check-in questions** that are easy to answer only after doing it in FL:

| After unit | Mission in FL desktop | Check-in example |
|---|---|---|
| 1–2 | Open any instrument, click keys on the Piano roll keyboard and turn on **Typing keyboard to piano** | "What octave number does FL give middle C?" (C5) |
| 3–4 | Open the Channel Rack (F6). Left-click steps for four-on-the-floor, right-click to remove one. Set the tempo and press Space | "How many steps does one beat cover?" (4) |
| 5–6 | Open a channel's Piano roll (F7). Draw (P), right-click to delete, set snap to Beat. Open the Kick's Piano roll and find your steps as notes | "What happens when you right-click a note?" |
| 7–8 | Select notes (E) and move them with Shift+↑/↓ (semitone) and Ctrl+↑/↓ (octave). Turn scale highlighting on, then off | "Which key moves a selection one octave?" |
| 9–10 | Build your progression on a Chords channel in a new Pattern. Duplicate a bar with Ctrl+B. Clone the Pattern and change one chord | "What's the new Pattern's number?" |
| 11–12 | Add a Lead channel in the same Pattern and turn on ghost channels. Place the Pattern in the Playlist (F5) and switch to SONG. Then the **rebuild mission**: leave the exported file closed and write a new call and answer from scratch | "How many bars long is your Playlist clip?" |

**Production pathway after the MVP** (each step produces a new version of the same piece):

1. **v1.1 Groove & arrangement:** Lou's bass channel (root notes, then an octave answer); straight vs swung hats; a two-Pattern groove; the **Playlist** with an 8–16 bar section, subtracting a layer before the return, and **SONG** mode.
2. **v1.2 Sound & mix:** Padma's units: waveforms at matched loudness, attack/decay/sustain/release, low-pass filter sweeps. Deliver a pluck and a pad from the same patch. In the **Mixer**: balance the lead against the bass, compare dry and reverb at matched level, and spot clipping on the master meter.
3. **Later:** inversions and voice leading, a progression in a new key, transposing a motif by scale degree, tension and release.

### Sample lessons

**A. Unit 1, lesson 1: "High or low?" (first launch, about 90 seconds)**

1. Kiko: "Two friends want to sing for you." Tink pops in high on the screen and Lou rumbles in low. Each sings when tapped. *(Teaching step: size and position match pitch.)*
2. "Tink sings **high**. Lou sings **low**. Tap them both again." Hearing both advances the lesson.
3. **Check:** the two characters slide behind two identical violet pads. "Which pad is higher?" The pads play in random order. *(No visual cue.)*
4. Correct: mint tray, Tink sparkles, a rising figure plays. Miss: amber tray, "Listen again: pad B is Lou's note," and both pads replay slowly.
5. Two more fresh pairs, cue-free, with the gap between pitches narrowing.
6. Lesson complete: "You just heard pitch." **One more pair** or **Continue**.

**B. Unit 3, lesson 3: "Four on the floor" (Channel Rack)**

Setup: one bar, 90 BPM, only the Kick channel visible, metronome on. Kiko: "Every beat gets a kick." Step 1 glows. The learner taps it, playback starts automatically and loops, and Kiko bounces on each kick. The glow moves to step 5, then steps 9 and 13. Hint level 2 shows the beat numbers above steps 1, 5, 9 and 13. Check: the Kick has notes on exactly steps 1, 5, 9 and 13, with any velocity. Fresh example without glows: "Now at 120 BPM: four on the floor." The FL chip on the Channel Rack header reads **F6**, and the tray offers the Desktop mission card.

**C. Unit 9, lesson 1: "Build C major" (Piano roll, coach in Studio)**

From a blank Studio, choose **Lessons → Chords → Build C major**. The coach plays C–E–G one note at a time, then together, while Ro, Thea and Fi land on one wire. "Put Ro (C) on beat 1." The learner taps the C5 row at beat 1. "Add Thea (E) above it, same beat." Then "Fi (G), same beat." If G starts late, its start line is highlighted: **Hear yours** (staggered) vs **Hear stacked**. Then the guide hides: "Build it again at beat 3." Check: the notes sounding together at beat 3 form the set {C, E, G}; staggered starts fail a block-chord check even when the pitch names match. Then **Free play**: change any note and hear it, unchecked.

**D. Unit 11, lesson 3: "Same rhythm, different ending"**

**Prerequisites:** move a note; count four beats. **Setup:** two bars, 4/4, 96 BPM, snap **Beat**, the learner's own C-major chords (or a supplied pair) ghosted on Chords, pluck sound. Bar one has C5, E5 and D5 on beats 1, 2 and 3, one beat each, with beat 4 silent. Bar two starts as a duplicate. In FL labels those pitches are MIDI 60, 64 and 62. [4]

1. **Hear it.** Version A plays both bars unchanged. Version B changes only the last D to C. "What changed: the rhythm or the ending pitch?" Each version replays against the same chords. On a miss, solo the final two beats.
2. **Try it.** "Keep the three attacks and the gap. Give bar two a different last pitch." Bar one is locked as the reference. Any pitch can be auditioned; C is an example, not the answer key.
3. **Check it.** Compare bar two's onsets and durations with bar one's, check that the first two pitches match, and check that the third differs. Velocity and the choice of new pitch are ignored. `C–E–G` on the same beats passes. `C–E–C` with the C on beat 4 gets: "Your ending changed pitch *and* arrived a beat later. Move it to beat 3 to hear the pitch change on its own." That beat is highlighted and the attempt is kept.
4. **Make it yours.** Open a copy with the constraints removed: "Which ending would you loop? Play both before you choose." The tray explains what the chosen ending does ("G is in the chord, so it sounds finished. A leans forward and wants to keep going."). Neither ending is marked wrong. **Clone pattern** keeps both versions. In the next review, a new three-note call with different pitches; the demonstration stays hidden until requested.

---

## 8. Feedback, progression and motivation

| Activity | Evidence and feedback |
|---|---|
| Reproduce a named pitch or rhythm | Compare the edited events on the musical grid one-to-one, including missing and extra notes. Check only the dimensions named in the prompt. Give the earliest useful correction ("Your second note starts on beat 3; listen to beat 2"). Slow replay changes playback tempo, not expected positions. |
| Step-sequencer rhythm | Compare the set of active steps per channel and ignore velocity unless asked. "Your clap is on 3; the backbeat lives on 2 and 4." Ungraded tap-along stays ungraded. |
| Relative ear training | Play a reference pitch before asking about up/down, interval or contour. If any starting pitch is allowed, compare pitch differences, not note numbers. The visual reveal is a hint, tagged as one. |
| Chord building | Compare the notes sounding together on the requested beat as an unordered set. Staggered attacks fail a block-chord check. Accept inversions and octave changes when the prompt asks only for chord identity. |
| Compose to a constraint | State the constraint before play. Show which conditions were met and audition the affected bar. A chromatic note is not a mistake unless the task requires scale notes. When several answers work, **explain what the choice does**. |
| Free work, Unit Jams and capstone | No beauty score. Ask the learner to point to the repeated idea, play the variation and explain one choice. Keep both versions. Desktop missions are self-reported plus check-in answers. |

**Hint ladder:** replay the contrast → isolate the beat or bar → show the target region or demonstrate the edit. Reveals allow completion and are tagged "with help". **Mastery** means two unassisted successes on different examples, one of them on a later day, or a passed **Solo** node. Reviews start at roughly 1, 3 and 7 days after success. After a miss, give one coached retry and queue a fresh example for the next session. Cap daily review prompts at three and keep overdue work optional. Local dates and stable attempt IDs mean reopening a result never awards progress twice.

**Motivation mechanics:**

| Mechanic | Ship? | Rule and reason |
|---|---|---|
| XP | Yes | Earned for lessons, reviews and Unit Jams, with a small unassisted bonus. Repeating an already-mastered lesson earns a reduced amount, so grinding easy lessons doesn't pay. |
| Daily goal + streak | Yes | Finish one lesson or review to light today's beat. **Streak freezes:** earn one per 7-day streak and hold up to two. Past days stay lit after a gap. |
| Daily quests | Yes | Three local quests that rotate, e.g. "Make one loop", "Two reviews unassisted", "Five minutes of free play". Rewards are XP and cosmetics. |
| Badges | Yes | Musical milestones: First pitch, First beat, First chord, First progression, First hook, First export, each Desktop mission, all units Solo. |
| Cosmetics | Yes | Character outfits and Studio color themes unlocked by milestones. No currency. **Tools and core sounds are never locked.** |
| Hearts / lives | **No** | They punish experimentation; the course promises unlimited retries. |
| Leagues / leaderboards | **No (MVP)** | They need a backend and reward speed over musicianship. Revisit later as opt-in friends only. |
| Gems, shop, ads, paywall | **No** | They distract from the goal and conflict with the privacy plan. |
| Timers / speed bonuses | **No** | Speed of tapping is not musical skill. |

**Reminders:** local notifications only, requested after the second completed lesson using Android 13+'s runtime notification permission [24]. At most one per day, at a time the learner picks, in Kiko's voice and never guilt-tripping ("'My Hook' is ready when you are"). After three ignored days they drop to weekly, and they can be turned off from the notification itself. A home-screen widget showing the streak strip and **Resume** comes in v1.1.

**Content authoring:** lessons ship as versioned data with an objective, prerequisite skills, seed Pattern, reference events, allowed tools, target constraints, hint sequence, **character script** (which character, which states, which bubble text), **cue audit flag**, accessibility alternative, desktop mission and credited assets. For every graded prompt, author a passing example, a near miss with its intended hint, and an alternate valid answer wherever the prompt admits one. Review by ear on the phone and in FL before publishing. All examples and sounds are original.

---

## 9. Android build and data plan

**Recommended stack:**

- Kotlin and Jetpack Compose for the app shell [25]
- Compose Canvas for the Piano roll and step grid, plus semantic list editors for accessibility
- Room for projects, attempts and progress
- **Rive Android runtime** for character state machines [22]; Compose animation for UI motion; vector drawables for icons
- **Oboe** behind a narrow native (C++) interface for the audio engine [10]
- WorkManager for local reminders

Proposed minimum is Android 8.1 (API 27); verify the learner's phone before fixing it. Composition haptics need API 30+ and fall back gracefully below that.

| Component | Responsibility and contract |
|---|---|
| **Score model** | A project holds tempo, meter, key label, Channels (kind: drum or instrument; sound preset; color; mute; volume; pan) and up to 4 Patterns. Each Pattern stores length (1/2/4 bars) and per-Channel notes. A note has a stable ID, MIDI pitch 0–127, start tick, positive duration and velocity 1–127, at 480 ticks per quarter (a step is 120 ticks). **Channel Rack steps are notes** one step long on the channel's fixed pitch, as in FL. Rests are gaps. v1.1 adds Playlist clips (Pattern ID, start bar, length). Display labels never transpose data. |
| **Playback** | Convert an immutable score revision into timestamped events off the real-time thread. Schedule against absolute audio frames using the negotiated sample rate. Preallocate voices and events: no file access, locks or allocations in the callback. Stop releases all voices. Apply loop edits at a defined musical boundary, with immediate audition on touch. |
| **Transport clock for UI** | The engine publishes its playback position (frames presented, via the stream timestamp) through a lock-free atomic. The playhead, step lights, Kiko's beat light and character dances all read it, so **what you see matches what you hear**. |
| **Sounds** | Original drum one-shots (kick, clap, closed hat; snare and open hat for free play); a compact multisampled keys instrument; a small subtractive synth for pluck, pad, lead and (v1.1) bass presets; a velocity-sensitive amp envelope; a master **limiter** (as in FL's default template) and a light shared reverb so the phone sounds good on the first tap. Separate click. Author-level-matched A/B comparisons. Audio assets ≤ 25 MB. |
| **Characters & motion** | Rive files bundled in the app, one rig per character with the shared input contract in §4.4. A `CharacterDirector` maps lesson events (prompt playing, correct, miss, celebrate) to inputs and enforces the §4.3 rules in code: it mutes babble during prompt audio and swaps to identical pads in checked listening items. |
| **Android audio lifecycle** | Request low-latency output and exclusive mode where available, falling back to shared mode and tuning buffers if underruns occur. Request audio focus before playback. Stop on focus loss, backgrounding or headset removal. Reopen on route change and require Play to resume. Show a Bluetooth delay hint once; no live performance grading in the MVP. [10–12] |
| **Persistence** | Save every committed edit off the audio thread, serializing writes by revision so a stale autosave can't overwrite a newer one. Store lesson step, seed version and attempt together, and commit completion, XP, quest progress and review update in one transaction. Migrate by schema version and keep the old data if conversion fails. A failed save stays visibly unsaved and can be retried. |
| **Privacy and delivery** | Local profile only; no login, ads, microphone or broad storage permission. The system document picker and share sheet handle export. Pilot measurements stay on the device and are collected only with consent. The app explains that MIDI saves notes, not learning history, and that uninstalling may remove local projects. |

**Performance budgets (target phone, release build):**

| Measure | Budget |
|---|---|
| Cold start to first sound in lesson 1 | ≤ 2 s |
| Learn screens with one character animating | p95 frame time ≤ 16.7 ms (JankStats/Macrobenchmark [26]) |
| Studio: 5 channels playing + Kiko beat light + editing for 10 minutes | Zero audio underruns, no accumulating drift, p95 frame ≤ 16.7 ms |
| Warmed touch-to-sound on built-in or wired output | Median ≤ 50 ms over 30 taps (external capture), p95 reported |
| APK / install size | ≤ 60 MB |

**Rules that keep the editor, audio and export in agreement:** snap to musical ticks; clamp starts and ends to the Pattern; sort note-offs before note-ons for same-pitch retriggers at a shared tick. Allow simultaneous pitches on Chords. Reject ambiguous same-pitch overlaps within a Channel with a visible conflict, and never delete a neighbor silently. Release and retrigger deterministically at the loop end; never accumulate rounded per-beat delays. Grade only the exercise's target channel, never the click, previews, ghost notes or UI sounds.

---

## 10. Take it to the desktop

**Export contract:**

- **One channel** → Standard MIDI File format 0, one track, 480 PPQ, tempo and 4/4 metadata, note on/off pairs, velocities, and end-of-track at the Pattern boundary, with leading and trailing silence kept. Name: `My Hook - Chords - 96 BPM.mid`.
- **Whole Pattern** → format 1, one track per non-empty channel. Drums use General MIDI note numbers (Kick 36, Snare 38, Clap 39, Closed hat 42, Open hat 46) so they land on drum kits in Live and Logic.
- **Audio** → a WAV of the loop played twice with its tail, rendered offline by the same engine and shared through the Android share sheet. The sound is the same as on the phone, so learners can send their hook to a friend.
- Every export leaves out preview, metronome, guide notes, authored backing and UI sounds. Export is disabled for empty channels. Write a complete temporary file before copying it to the chosen destination, and report success only after close. A cancelled or failed copy leaves the project intact. [13]

| Destination | Transfer card | Check after import |
|---|---|---|
| **FL Studio desktop (primary)** | *One channel:* create or pick a channel, open its Piano roll → menu → **File → Import MIDI file**, with **Realign events** off to keep an initial rest. *Whole Pattern:* main menu **File → Import → MIDI file** creates one channel per track (verify on the owner's FL version). Set the project BPM from the card. Play in PAT, place the Pattern in the Playlist and switch to SONG. [1, 14] | First onset, ending, note lengths, velocity and bar count. If the host trims trailing space, set the loop to the displayed bar count. |
| Ableton Live | Drag the `.mid` onto a MIDI track that has an instrument (a Drum Rack for drums), open the clip's Note Editor, and set tempo and loop length from the card. [15] | Same pitches and rhythm. |
| Logic Pro for Mac | Put the playhead at a bar start, then **File → Import → MIDI File**. Assign a software instrument or Drum Machine Designer, open the region in Piano Roll and check tempo. [16] | Region start and length. Octave labels differ (Live and Logic call MIDI 60 "C3"; FL calls it "C5"). |

The **rebuild mission** is deliberately different: *"Leave the file closed. Draw a call in an empty Pattern, give it a different answer, and listen in context."* Import proves compatibility; this proves the skill transferred.

---

## 11. Delivery, evaluation and release gates

Estimates are planning ranges for one Android developer with access to physical phones and desktop FL. They assume a **part-time illustrator/Rive animator**, a **part-time sound designer** and a producer/teacher for content review. They are not benchmark results or commitments.

| Milestone | Effort | Exit test |
|---|---|---|
| 0. Design sprint & character bible *(design-led; overlaps M1)* | 2–3 weeks dev support; 3–4 weeks design | Character sheet and personality notes, Kiko rig with all §4.4 states, tokens, a clickable prototype of the six key screens, motion spec. Five beginners pass the prototype gate in §2. |
| 1. Audio & workflow spike | 2–3 weeks | On the target phone: toggle steps, draw and move a note, hear it, loop without a seam, and watch Kiko's beat light stay on the audible beat. Import an exported fixture in FL. Record the device, route and latency baseline. |
| 2. First playable | 3–4 weeks | Unit 1 with characters and onboarding, tested with three absolute beginners. Run the chord exercise through both entry points with hint, resume, free variation, return to the original and MIDI handoff. |
| 3. Studio MVP | 6–8 weeks | Channel Rack, Piano roll tools, Patterns, Browser, velocity, list editors, persistence, MIDI and WAV export. Stack a triad under an independent Lead over drums and export each channel intact. Finish a project in airplane mode. |
| 4. Course & progression | 7–10 weeks | All 36 lessons and 12 Unit Jams with authored examples, cue audits, review variants and desktop missions. XP, streak, quests, badges, Solo nodes and reminders work. Due reviews survive process death and app upgrades. The producer signs off the audio. |
| 5. Polish & personal beta | 3–4 weeks | Motion and sound polish; performance budgets met on three phones; accessibility pass; look-alike review; pilot run; signed build. |

**Serial total: 23–32 developer weeks** (up from 14–23), plus about 8–12 weeks of part-time illustration and animation and 3–4 weeks of sound design, largely in parallel.

**If time is tight, cut in this order:** WAV share → cosmetics → three characters reduced to static illustrations (keep Kiko animated) → Patterns from 4 to 2 → merge units 10 and 11 into one longer unit → ship units 1–6 as a first pilot and add 7–12 in order. **Never cut:** audio timing, editor accuracy, protect-your-work, the list editors, cue-free checks or the FL export check.

| What could fail | Test and proposed gate |
|---|---|
| Learner follows highlights but cannot compose | Pilot with five beginners. About a week after unit 12, ask for a fresh four-bar phrase with no overlay or imported file. Aim for at least four of five to independently program a beat, stack a triad progression, play a repeated motif over it, alter its answer, and rebuild it in FL. Report assistance and the actual counts. |
| **Characters leak answers or pull attention from listening** | Automated cue audit: every checked listening item renders identical visuals for each option. In the pilot, compare accuracy on cue-free items with characters set to Full vs Off; a meaningful drop sends the item back for redesign. |
| **Animation causes jank or audio glitches** | The 10-minute Studio soak in §9 on a lower-cost phone: zero underruns and frame budgets met. If it fails, degrade in order: pause characters during playback, then lower the rig frame rate, then disable reverb. Never touch audio priority. |
| **Gamification drives grinding instead of skill** | Track XP against unassisted mastery. If learners replay mastered lessons for XP, reduce repeat XP further. Quests must never require repeating mastered content. |
| **Tone reads as childish** | Ask in the prototype test and pilot. If more than one in five says so, reduce character presence in Studio and make humor drier before cutting the cast. |
| Phone editing overwhelms listening | First session: four of five beginners can replay and compare pitches without coaching. After unit 6: place, move, resize, undo and replay independently. Test small portrait, landscape and large text. A TalkBack user completes a chord exercise and a step-sequencer exercise via the list editors. |
| Audio feels late or drifts | Test the owner's phone, a lower-cost phone and a recent one, logging OS, sample rate, buffer, route and underruns. Meet the latency budget in §9. Run a five-minute loop while editing with no dropout or drift. Document Bluetooth separately. |
| Correct music gets rejected | Run each lesson's fixtures on boundary beats, octave labels, alternate endings and extra or missing notes. Every declared valid variant passes and every near miss gets a relevant hint. Free play never says "wrong". |
| Export loses musical intent | Reference files with a leading rest, trailing rest, repeated adjacent pitch, a sixteenth, a velocity change, GM drums and a four-bar ending. Parse exports independently. Import manually into recorded versions of FL, Live and Logic. An FL pass is required for beta; the other cards stay labelled "unverified" until tested. |
| **FL changes menus or shortcuts** | Missions and FL chips carry a "verified on FL x.y" label for the owner's version and are versioned as lesson data, so they can be updated without an app release. |
| **Look-alike or trademark problems** | Before beta, review the cast, palette, type, icons and copy against Duolingo's and Image-Line's marks. FL's name is used nominatively only, with the non-affiliation line. Run a trademark search on the app name. |
| Work disappears | Force-stop after a confirmed save, rotate mid-edit, unplug headphones, cancel an export, simulate low storage and migrate an older score. The last confirmed score reopens, a failed write is visible, and retrying a completion never duplicates XP, a review or a project. |

---

## 12. Design review: third pass

- **Delight vs. listening.** Characters make the app warmer, but they also make ear training riskier: a big low creature *is* a pitch cue. Decision: teach with the cue and check without it, mute character sounds during prompts, enforce both in `CharacterDirector`, and audit every item. Learners who want a pure tool can turn characters down or off.
- **FL fidelity vs. touch.** Right-click delete, hover hints and tiny knobs don't exist on a phone. Decision: copy FL's **names, window model, shortcuts and data model** (steps are notes) and make the gestures touch-native. Teach the desktop mouse conventions explicitly in missions. Copying FL's *look* would be a trademark risk and would make a cramped phone UI.
- **Scope growth is justified, but only so far.** The drums and Patterns earn their place: beats are the fastest route to a finished loop (motivation by unit 4), the Channel Rack is FL's front door, and Clone pattern is the FL-native way to keep A/B versions. The Playlist and Mixer still wait. They are taught first on desktop in missions and arrive on the phone in v1.1 and v1.2.
- **Pacing check, second look.**
  - Unit 3 introduces the Channel Rack, tempo and bars together. To keep that manageable, step toggling is the only interaction; there are no knobs.
  - Unit 6 spreads Draw, Delete and resize across three lessons.
  - The new unit 10 fixes revision 1's jump from two chords straight to melody.
  - Unit 12's capstone was heavy for one sitting, so it moves into the Unit Jam with save points.
  - Unit 1's "soft vs loud" lesson stays: it heads off the most common beginner confusion (louder = higher).
- **Motivation without manipulation.** Keep the habit mechanics (streak, goal, quests, XP) and reject the scarcity mechanics (hearts, gems, leagues, timers). Tie the most visible reward, the gold Solo node, to unaided mastery, so the shiny thing and the real goal are the same thing.
- **Answers that aren't wrong.** Open prompts always end with **Compare & keep** and an explanation of what the choice *does*, as the brief asked: "several answers work, so explain, don't mark wrong."
- **Open inputs:** the exact Android model and OS, FL edition and version, preferred genres, whether the learner uses Bluetooth, the budget for illustrator, animator and sound designer, and a trademark search on the app name. Touch latency, animation performance on a low-cost phone, transfer outcomes and every time estimate remain unvalidated until the first device session and pilot.

---

## Sources and further reading

Sources 1–19 were reviewed for revision 1 on 26 November 2026. Sources 20–26 were added in revision 2 from prior knowledge without a fresh research pass, so check each link before relying on it. Lesson counts, mastery rules, gamification rules, performance budgets, effort estimates and release targets are proposals for this app.

1. Image-Line: [FL Studio workflow](https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/basics_workflow.htm) and [Channel Rack](https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/channelrack.htm): instruments, step sequencer, Patterns and Playlist roles.
2. Image-Line: [Piano roll](https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/pianoroll.htm): axes, tools, snap and note properties.
3. Image-Line: [Piano roll menu](https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/pianoroll_menu.htm): snap values and score/MIDI commands.
4. Image-Line: [Piano roll scripting reference, tutorial 1](https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/pianoroll_scripting_api.htm): explicit note 60/C5 example.
5. Duolingo: [Music course design](https://blog.duolingo.com/music-course/): short interactive exercises and accessible instrumentation.
6. Ableton: [Learning Music: Make melodies](https://learningmusic.ableton.com/make-melodies/make-melodies.html): interactive sequence from beats and pitch to phrases.
7. Ableton: [Learning Synths playground](https://learningsynths.ableton.com/en/playground): oscillator, envelope and filter controls.
8. Open Music Theory: [Digital workbook](https://viva.pressbooks.pub/openmusictheory/chapter/digital-workbook/): composition from a basic idea to a phrase.
9. Katz: [Spaced learning and melodic memory](https://doi.org/10.1177/03057356251401906): delayed recall, with limits on generalization.
10. Android Developers: [Low-latency audio with Oboe](https://developer.android.com/games/sdk/oboe/low-latency-audio): stream setup, callback constraints and buffers.
11. Android Developers: [Audio latency](https://developer.android.com/ndk/guides/audio/audio-latency): latency types and measurement limits.
12. Android Developers: [Manage audio focus](https://developer.android.com/media/optimize/audio-focus): focus requests and losses.
13. MIDI Association: [Standard MIDI Files](https://midi.org/standard-midi-files) and [specification](https://midi.org/standard-midi-files-specification).
14. Image-Line: [Piano roll MIDI import dialog](https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/pianoroll_midi.htm): blending and realigning imported events.
15. Ableton: [Understanding MIDI files](https://help.ableton.com/hc/en-us/articles/209068169-Understanding-MIDI-files).
16. Apple: [Standard MIDI files in Logic Pro for Mac](https://support.apple.com/guide/logicpro/standard-midi-files-lgcpdf6a3851/mac).
17. Android Developers: [Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics) and [accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults).
18. Duolingo: [Introducing the learning path](https://blog.duolingo.com/new-duolingo-home-screen-design/): unit grouping, a clear next lesson and integrated practice.
19. Duolingo: [The Duolingo teaching method](https://blog.duolingo.com/duolingo-teaching-method/): learn by doing, recognition to application, targeted hints.
20. Duolingo: [Design blog](https://blog.duolingo.com/tag/design/): the character cast, color, shape and motion as one system. design.duolingo.com now redirects here. *For studying the grammar only; nothing is copied.*
21. Image-Line: [Keyboard shortcuts](https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/basics_shortcuts.htm): window function keys and Piano roll tool letters.
22. Rive: [Android runtime](https://github.com/rive-app/rive-android): state machines, inputs and Android integration.
23. Android Developers: [Haptics](https://developer.android.com/develop/ui/views/haptics): composition primitives and design principles.
24. Android Developers: [Notification runtime permission](https://developer.android.com/develop/ui/views/notifications/notification-permission).
25. Android Developers: [Compose animation](https://developer.android.com/develop/ui/compose/animation/introduction): springs and shared-element transitions.
26. Android Developers: [JankStats](https://developer.android.com/topic/performance/jankstats): measuring frame performance in production builds.
