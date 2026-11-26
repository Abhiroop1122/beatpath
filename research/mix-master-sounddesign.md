# Sound design, mixing and mastering: FL-style plugin windows, a 30-lesson path, and phone DSP recipes

**Research pass, 26 Nov 2026.** Scope: the advanced half of Beatpath (plan §2 "v1.2 Sound & mix" and §7 "Production pathway"). This covers sound design, mixing and mastering, taught in on-screen plugin windows that look and behave like FL Studio's stock plugins. The note, drum, chord and melody path lives in `research/studio-curriculum.md` (Levels 1–10) and isn't repeated here. This file continues at **Level 11** and uses the same lesson-ID and check conventions.

**Sources.** Image-Line's online manual pages and screenshots for each plugin, Image-Line's *Compare editions* page, Sound On Sound, iZotope, Spotify for Artists, the W3C copy of the RBJ EQ Cookbook, Julius O. Smith's CCRMA pages on Freeverb, Cytomic's SVF paper and Martin Finke's polyBLEP article. Every one was fetched and read today (list at the end). Anything not confirmed in a source is marked **[U]**. Colors are approximations read from the manual screenshots. As in `fl-ui-reference.md`, we redraw every control; no Image-Line bitmaps, logos or names-as-branding ship.

---

## 0. Decisions at a glance

| Question | Decision |
|---|---|
| Which plugins do we mimic? | **7 windows**: (1) a **Synth** window = **3x Osc** + FL's **Channel settings › Envelope/Instrument** tab, which is where FL keeps ADSR, filter and LFO; (2) **Fruity Parametric EQ 2**; (3) **Fruity Compressor**, plus Fruity Limiter's COMP tab for sidechain only; (4) **Fruity Reeverb 2**; (5) **Fruity Delay 3**; (6) **Fruity Limiter** (LIMIT tab) on the master; (7) the **Mixer** track strip (fader, pan, meter, FX slots, sends), with **Fruity Balance** as a desktop tip only. |
| Edition risk | All seven exist in **every edition, Fruity included** (§A0). Maximus (Producer+), Sytrus (Producer+), LuxeVerb and Emphasis (All Plugins) appear only as "on desktop you may also have…" tips. |
| How many controls on the phone? | **3–6 per window**, always shown with the **full FL name** (e.g. "THRES · Threshold"). A lesson unlocks **one new knob at a time**. The others are visible but locked (dimmed, with a lock glyph). **All controls** opens in free play. |
| Why Fruity Compressor and not Limiter-COMP for compression lessons? | Fruity Compressor has the cleanest layout FL ships: six labelled cells with live value readouts and documented ranges. That suits the "very easy to understand" requirement. The manual calls it "legacy" but also "useful when you are learning how compressors work, due to its simplicity". The desktop tip names Fruity Limiter's COMP tab as the modern route. |
| How are checks graded? | Every check runs on an **offline render of two loops through the same DSP graph** after a one-loop warm-up. That makes grading deterministic and independent of the device, latency or speaker. Rules combine **parameter values** with **measured signal facts**: peak dBFS, K-weighted loudness, gain reduction, band energy. (§B0) |
| The A/B rule | Every A/B comparison is **loudness-matched by the engine** to within 0.5 LU, and the screen says so ("Level-matched ✓"). The one exception is lesson 14.1, which shows *why* we match. |
| DSP | It fits on a mid-range phone with real headroom (≈ 2–4 % of one big core for 9 channels, by the estimate in §C0). Inserts are mono, pan sits after the inserts (FL's order), and there is one shared reverb and one delay on sends. The existing `Dsp.kt` is a sound start; §C10 lists six specific fixes. |

---

## A. The plugins and the Mixer

### A0. Which FL edition has what (Image-Line *Compare editions*, read today)

| Plugin | Edition | Used here for |
|---|---|---|
| 3x OSC | All editions | Synth oscillators |
| Channel settings (Envelope/Instrument tab) | All editions (part of every Sampler-based channel) | ADSR, filter (MOD X / MOD Y), LFO |
| FLEX | All editions [U: the page lists it among instruments, and a quoted user calls it "a free plug-in"] | Desktop tip: the same ideas behind 8 Macros (Play view) and an Edit view with Cutoff / Res / Env Amt |
| Fruity Parametric EQ 2 | All editions | EQ |
| Fruity Compressor | All editions | Compression |
| Fruity Limiter | All editions | Master limiting; COMP tab sidechain |
| Fruity Reeverb 2 | All editions | Reverb |
| Fruity Delay 3 | All editions | Delay |
| Fruity Balance | All editions | Pan/volume as a plugin (desktop tip) |
| Fruity Soft Clipper | All editions | Optional desktop tip (THRES, POST) |
| Soundgoodizer, Fruity Multiband Compressor | All editions | Desktop tips only |
| Maximus | **Producer Edition and up** | Desktop tip only |
| Sytrus | **Producer Edition and up** | Not used |
| LuxeVerb, Emphasis | **All Plugins Edition only** | Not used |

Every lesson's "FL tip" uses only Fruity-Edition plugins, so no learner is told to open something they don't own.

### A1. Synth: "3x Osc" plus the Channel settings Envelope tab

**What FL really does.** 3x Osc is "a 3 x Oscillator, subtractive synthesizer feeding the FL Studio Sampler". 3x Osc has **no** ADSR or filter of its own. Envelopes, the filter and LFOs live on the channel's **Envelope / Instrument** settings tab, which has sub-tabs **Panning · Volume · Mod X · Mod Y · Pitch**. By default **Mod X = filter cutoff** and **Mod Y = resonance**. Our Synth window therefore has the same two tabs FL has, so a learner who opens a 3x Osc channel on desktop finds the knobs where they expect them.

**3x Osc parameters (manual):**

| FL name | Range / values | Notes |
|---|---|---|
| Shape selectors (per osc) | sine, triangle, square, saw, rounded saw, noise, custom | |
| COARSE | −24 … +24 semitones | |
| FINE | −1 … +1 semitone | |
| Mix level (osc 2 and 3 only) | 0–100 % | Ratio mix: total stays at 100 %. "To mix all 3 equally: osc 2 50 %, osc 3 33 %" |
| PHASE OFS, DETUNE (stereo), Pan (per osc) | — | DETUNE is centre-off. It sets slightly different L/R frequencies |
| Invert, AM OSC 3, HQ, PHASE RAND | switches/knob | HQ = anti-aliased oscillators |
| Defaults | [U]. The manual screenshot shows COARSE 24 / 12 / 0 and all three set to sine, but that may not be the init state | |

**Envelope / Instrument tab parameters (manual):**

- **Volume envelope:** DELAY, ATT, HOLD, DEC, SUS, REL, AMT, TENSION (per stage) and **Tempo**. The envelope only applies when its orange LED is on.
- **LFO:** shapes sine / triangle / square; DELAY, ATT, AMT (centre = 0, bipolar), SPEED, **Tempo**, **Global**.
- **Filter:** MOD X (cutoff), MOD Y (resonance). Types: Fast LP (default), LP, BP, HP, BS, LPx2, SVF LP, SVF LPx2. *"When Mod X is 100 % and Mod Y is 0 % the filtering is completely bypassed. This is the default state."*
- Header row: ON LED, PAN, VOL, PITCH, RANGE, **TRACK** (the Mixer track box).

**Visual essentials (screenshots):**
- *3x Osc:* a light brushed-silver panel (`#AEB6BD`→`#8E979F`). There are three rows, each with a big italic numbered tab (1/2/3) on the left. Shape icons sit in a 4×2 grid, and the active shape is a **yellow-orange glyph on a dark circle** (`#E8A93A` on `#3A3F44`). Next come two short vertical sliders with a red centre line (PHASE OFS, DETUNE), a large chrome **COARSE** knob with the number printed on it, a smaller FINE knob, a pan knob, and on rows 2–3 a large mix-level knob. Knob ticks are lime/orange. A dark footer bar holds AM OSC 3, HQ (orange LED) and PHASE RAND, with the light-grey script logo on the right.
- *Envelope tab:* dark grey (`#3C4248`). Sub-tabs sit along the top, the active one in mint green (`#5BE3A0`). The envelope graph is a dark box with a **mint-green polyline and round nodes**. Small dark knobs with mint arcs read DELAY ATT HOLD DEC SUS REL, with TENSION knobs under them. The LFO box has shape icons and DELAY ATT AMT SPEED. The Filter box has two large chrome knobs (MOD X, MOD Y) and a "Fast LP ▸" dropdown.

**Beginner subset (phone).** Two tabs, mirroring FL:

| Tab | Controls (full FL name kept) | Units shown |
|---|---|---|
| **3x Osc** | OSC 1 Shape · OSC 2 Shape · OSC 2 COARSE · OSC 2 FINE · OSC 2 Mix level | icon; semitones; cents (FL shows FINE as a fraction of a semitone: show "+12 ct (0.12 st)"); % |
| **Envelope ▸ Volume** | ATT · DEC · SUS · REL | ms or s; SUS in % |
| **Envelope ▸ Mod X** (filter) | MOD X · Cutoff, MOD Y · Resonance, then the Mod X envelope AMT · ATT · DEC · SUS | % with Hz under it ("45 % · 630 Hz"); % |
| **Envelope ▸ LFO** (Level 12.3) | target (Mod X / Pitch / Volume), shape, AMT, SPEED, Tempo | %, steps when Tempo is on |

The filter type is fixed to **SVF LP** in lessons; "LPx2" unlocks in free play. Hidden: HOLD, DELAY, TENSION, PHASE OFS, DETUNE, AM, PHASE RAND, osc 3. **Our addition:** a live **mini-scope** (waveform) and a small **ADSR graph** that animates per note. FL has the graph but not the scope; label the scope "Beatpath view".

### A2. Fruity Parametric EQ 2

**Parameters (manual):**
- **7 bands**, each with Band Type: **Low Pass, Band Pass, High Pass, Notch, Low Shelf, Peaking (the default type), High Shelf**, or OFF.
- **Filter slope** (dots under the type icon): centre = 2 (−12 dB/oct). "Steep" 4/6/8 = −24/−36/−48 dB/oct; "Gentle" variants have the same slopes but a softer knee.
- **EQ fader** (gain) per band, on a **±18 dB** scale (labels on the display). Disabled for LP/BP/HP/Notch.
- **FREQ** (centre/cutoff) and **BW** (bandwidth) knobs per band.
- **Band tokens** on the graph: drag to set freq and gain, mouse-wheel for BW, Alt+click to reset, Shift+click to solo the band, right-click for Type / Order / Key.
- A **Main level** (click outside the bands and drag).
- Monitor modes (off / input / output) with a spectrum histogram (20 Hz–20 kHz) and an optional heatmap.
- LIN (linear phase), HQ+ (FL 24 "decramped" filters), **Compare** (spare-state A/B).
- Credits thank **Robert Bristow-Johnson's EQ Cookbook**, the same formulas we use in §C2.
- Default band types and frequencies: **[U]**. A common observation is band 1 = low shelf, 2–6 = peaking, 7 = high shelf, all at 0 dB; verify on desktop.

**Visual essentials (screenshot `plugin_fx_FruityParametricEQ2.png`):**
- A large dark slate-blue graph (`#25323B`) with a faint grid and the dB scale **+18 / +12 / +6 / 0 / −6 / −12 / −18** on the right edge.
- Frequency labels **20, 50, 100, 200, 500, 1k, 2k, 5k, 10k** along the bottom.
- **Two header strips** along the top: piano octaves **C1…C9**, and region names **SUB · BASS · LOW MID · MID · HIGH MID · PRS · TREBLE**. This is a gift for teaching; keep it.
- The response curve is a **white** line. The spectrum is drawn as **pink/red vertical bars** (`#E0566E`) with a translucent fill.
- **Band tokens** are numbered rings, one color each: 1 purple `#A070F0` · 2 magenta `#E060C0` · 3 orange `#F08050` · 4 yellow `#E8D040` · 5 green `#60D060` · 6 teal `#50D0C0` · 7 blue `#5090F0`.
- To the right: 7 slim vertical faders, each topped by a type icon in the band's color.
- Under them: color chips plus **FREQ** and **BW** knob rows with colored arcs.
- Bottom-left: the faint "PARAMETRIC EQ 2" word mark (don't copy it; use our own label). Bottom-right: LIN, HQ+, ✓ tokens, MONITOR and COMPARE.

**Beginner subset (phone, portrait):**
1. **Graph** at full width, with the region header (SUB…TREBLE) and spectrum **on by default**. Tokens are 44 dp touch targets. Drag a token to set freq (x) and gain (y); pinch on the token or use the BW slider for width.
2. **Band chips 1–7** in the EQ2 colors. Lessons show only the bands they need.
3. Panel for the selected band:
   - **Type**, five icons: *High pass (low cut)*, *Low shelf*, *Peaking*, *High shelf*, *Low pass (high cut)*. Band Pass and Notch appear in free play only.
   - **FREQ** (Hz/kHz, log), **Gain** (dB, ±18), **BW** (FL's BW; show Q underneath in "More").
4. **Compare** = our auto level-matched bypass toggle (§B0). **Solo band** = FL's Shift-solo, shown as a headphone icon.
5. The slope stays at FL's default 12 dB/oct; "Steep 4 (24 dB/oct)" unlocks in free play.

### A3. Fruity Compressor (and Fruity Limiter's COMP tab)

**Fruity Compressor parameters (manual):**

| FL name | Range | Meaning |
|---|---|---|
| **Threshold** | 0.0 to −60.0 dB | Compression starts above this level |
| **Ratio** | 0.4:1 to 30:1 | Below 1:1 it expands |
| **Gain** | −30 to +30 dB | Make-up gain |
| **Attack** | 0.0 to 400.0 ms | |
| **Release** | 1 to 4000 ms | |
| **Type** (knee) | Hard 0 dB · Medium 6 dB · Vintage 7 dB · Soft 15 dB, each also as "/R" | "/R" = TCR (Transient Controlled Release, auto-release). Vintage eases the ratio back toward 1:1 well above threshold (LA-2A-like) |
| Sidechain | Mixer track number box | |
| Defaults | **[U]** | The manual screenshot shows −8.4 dB, 3.2:1, 3.4 dB, 10.8 ms, 117 ms, Soft/R, which looks like a set example |

**Visual (screenshot):** a compact dark-grey window (`#3E444A`) with **two rows of three cells**: THRESHOLD · RATIO · GAIN / ATTACK · RELEASE · TYPE. Each cell has a dark knob on the left with a **pink/magenta value arc** (`#F2A6D8`), a grey header tab carrying the name, and a **large light value readout** below ("‑8.4 dB", "3.2 : 1", "117 ms", "Soft/R"). Sidechain is a small number spinner beside GAIN. It is the easiest FL dynamics UI to read, and it ports well to a phone (a 2×3 grid of 48 dp knobs, or 3×2 in portrait).

**Fruity Limiter COMP tab (manual):** GAIN (purple, make-up), THRES (blue), RATIO (blue), KNEE (blue), ATT, REL, SUSTAIN (RMS window 0–1000 ms), CURVE 1–8, SIDECHAIN. The compressor works on **RMS**, the limiter on **peaks**. Set THRES to 0 dB to disable it.

**Beginner subset (phone):** **THRESHOLD, RATIO, GAIN, ATTACK, RELEASE**. TYPE is fixed to **Medium (6 dB knee)** and hidden until free play. **Our addition, flagged as "Beatpath view":** a vertical **gain-reduction meter** (0 to −20 dB, white/orange) and a 2-second scrolling strip like Fruity Limiter's display (teal waveform, white gain-envelope line). FL's Fruity Compressor window has no GR meter in the screenshot; Sound On Sound recommends "a compressor with a gain‑reduction meter" for beginners.

### A4. Fruity Reeverb 2

**Parameters (manual):**
- **MID/SIDE**: MID is the default.
- **H.CUT**, **L.CUT**: high/low cut on the reverb.
- **DELAY** (pre-delay), with a **TEMPO** switch.
- **SIZE** (room size; "the display will give you feedback").
- **DIFF** (diffusion), **MOD**, **SPEED**.
- **BASS** (bass decay multiplier), **CROSS** (bass crossover).
- **DEC**: decay = time to −60 dB, "up to 20 seconds".
- **DAMP**: high damping; at maximum it reads OFF and is bypassed.
- **DRY**, **ER** (early reflections), **WET**, **SEP** (stereo separation).

Manual guidance: *"When using Reeverb 2 in a Send mixer track [DRY] should be set to 0 %"* and WET to 100 %. Reverb "is easy to overdo… creating a washed out and 'muddy' sound", and you should "rarely" put it on the Master. Numeric ranges and defaults for SIZE, DELAY, H.CUT and L.CUT: **[U]**.

**Visual (screenshot):**
- Far left: a **3-D white wireframe cylinder** (the "room") glowing on a warm mauve-grey panel (`#7A6A6E`). It grows with SIZE.
- Then a dark strip with a **MID/SIDE** toggle, H.CUT and L.CUT knobs.
- A central block: DELAY (+ TEMPO LED), SIZE, MOD, DIFF, SPEED.
- Next: BASS, DEC, CROSS, DAMP.
- Right: three **vertical sliders DRY · ER · WET** and a SEP knob.
- Knobs are dark grey with thin **dusty-pink arcs** (`#C98A93`). Large italic "REEVERB 2" lettering sits in the background (don't copy the lettering).

**Beginner subset (phone):** **DEC, WET** (Level 17.1) → **+ L.CUT, DELAY** (17.3) → **+ SIZE, H.CUT** (free play). DRY appears as a slider when the reverb is on an insert. On a send track it is pinned to 0 % with a note "send track: DRY 0 %, WET 100 % (FL manual)". Keep the wireframe room. It is the clearest "what does SIZE mean" visual in FL, and it costs nothing to draw.

### A5. Fruity Delay 3

**Parameters (manual):**
- INPUT **Wet** (fader).
- DELAY TIME **Time**: 1–1000 ms, or **1/48 step to 16 steps (1 bar)** with **Tempo Sync**. "Each dot around the knob represents one step"; right-click › Set gives musical values.
- **Keep Pitch**, **Smoothing**, **Offset/Pan**.
- DELAY MODEL **Mono / Stereo / Ping pong / Off**, plus a **Stereo** spread knob.
- FEEDBACK **Level**: can exceed 100 %; the ring turns green → orange → red.
- **Cutoff**, **Res**, filter type **LP / HP / BP / Off** (inside the feedback loop, so "each echo is further filtered").
- **Samp Rate**, **Bits**.
- MODULATION Rate (0–20 Hz) / Time / Cutoff; DIFFUSION Level / Spread; FEEDBACK DISTORTION Limit/Sat, Level, Knee, Symmetry, with a transfer-curve display.
- OUTPUT **Wet**, **Tone** (LP ← off → HP), **Dry**.
- Defaults: **[U]**.

**Visual (screenshot):** cool grey panels (`#6E777F`) with dark section headers (INPUT, DELAY TIME, DELAY MODEL, FEEDBACK, MODULATION, DIFFUSION, FEEDBACK DISTORTION, OUTPUT). Knobs are dark with **lime-green arcs** (`#B8F060`). The feedback **LEVEL** knob is big, with an **orange arc** (`#F08A3A`). Mode lists use a vertical pill switch. The output column runs WET / TONE / DRY.

**Beginner subset (phone):** **TIME** (Tempo Sync is always on in lessons; chips show **1/8 = 2 steps, dotted 1/8 = 3 steps, 1/4 = 4 steps**), **FEEDBACK Level** (lessons clamp it to ≤ 90 %), **FEEDBACK Cutoff** (LP), **DELAY MODEL** (Mono / Ping pong), **OUTPUT Wet**, **Dry** (pinned to 0 % on a send track).

### A6. Fruity Limiter (LIMIT tab) on the master

**Parameters (manual):**
- **Signal flow:** COMP → GAIN → Limiter (with gate) → SAT → out.
- **GAIN** (purple) is the limiter input gain.
- **SAT** (saturation threshold).
- **CEIL**: the ceiling. *"setting the ceiling level to the maximum (+12 dB) should effectively prevent [the] limiter engaging."*
- **ATT** sets a **look-ahead** delay (latency). **CURVE** 1–8. **REL**. **SUSTAIN** is the peak-averaging window, 0–1000 ms.
- **NOISE GATE**: REL, GAIN, THRES.
- **Display:** input peaks purple, output green (grey where they overlap), analysis envelope blue, gain envelope white; level markers GAIN purple, CEIL green, THRESH blue.
- Comparison bank (▼ store, ⇅ flip).
- The FL "Levels, Mixing & Clipping" page: *"put Fruity Limiter in the last FX slot on the Master Mixer track… and with the default settings, raise the GAIN knob."*

**Visual (screenshots):**
- Top: a wide scrolling **display** on dark slate (`#3B4349`). The signal is a pale teal fill (`#9FC2C4`), the gain envelope a white line, the ceiling a green line.
- A left toolbar has display toggles and a speed slider.
- Bottom: a dark-grey panel with section titles **LOUDNESS · ENVELOPE · NOISE GATE**.
  - LOUDNESS: GAIN has a **purple ring**, SAT a **dull-red ring**, CEIL a **green ring**. In COMP mode THRES, KNEE and RATIO have **blue rings**.
  - ENVELOPE: ATT, REL, SUSTAIN, with CURVE spinners.
  - NOISE GATE: REL, GAIN, THRES (brown rings).
- **LIMIT | COMP** tabs bottom-left; ▼ ⇅ compare tokens bottom-right.

**Beginner subset (phone):** **GAIN, CEIL, REL**, a **gain-reduction readout** ("−2.4 dB"), and the scrolling display (teal waveform, white gain line, green ceiling line). ATT is hidden and fixed to 0 (our limiter needs no look-ahead, §C4). SAT and the gate are hidden. The COMP tab is shown greyed out with "Compression lives in Level 16".

### A7. Mixer track strip and Fruity Balance

**Fruity Balance (manual):** two controls, **Balance** and **Volume**. It is "a true panning effect… mixing left and right channels". The screenshot shows a Balance knob (purple pointer, reads "Centered"), a lime stereo meter, and a Volume knob with a **cyan arc** (reads "0.0dB 1.00"). Panning in lessons happens on the Mixer pan knob, as FL learners do it. Fruity Balance is the desktop tip for automating pan or volume inside an FX chain.

**Mixer controls we keep (manual + `mixer_main.png`):**

| Control | FL behavior | Phone version |
|---|---|---|
| Track header | Number (M, 1, 2…) and name label | Same. Insert names follow the channel ("Kick"…) |
| **Peak meter** | Stereo bars. "Output is in dB". Over-0 dB peaks turn **orange** | Two bars, −60…+6 dB, **green → yellow (−6 dB) → orange (> 0 dB)**, peak-hold tick, latched clip dot on the master |
| **Mute** | Green LED. Right-click or Ctrl-click solos | Tap = mute, long-press = solo (FL chip: "Right-click = Solo") |
| **Pan** | Small knob. Default "Circular panning law", centre −3 dB relative to hard-panned | Knob reading "40 % L … C … 40 % R"; tap opens a slider sheet |
| **Level fader** | Post-FX volume. Grey; **green when selected** | Fader in **dB** (−∞ … +5.6 dB [U for FL's maximum]; we use +6), double-tap for 0.0 dB |
| Stereo separation | Middle = off; right = mono | Master only, as a **Mono** check button (lesson 14.4) |
| **Effect slots** | **10 per track**, each with a mix knob and an on/off LED | 3 visible slots + "+". LED = bypass |
| **Sends** | Any insert can be a send. A send switch at the bottom of the destination track becomes a **send-level knob**. Post-fader by default | Two fixed send tracks, **"Reverb"** and **"Delay"**, each with a send knob on every insert |
| Routing | Channel Rack **Mixer track box** (drag the number) or Channel settings **TRACK**. If unset ("---") audio goes to the **Master** | The same number box on each Channel Rack row (already in `fl-ui-reference.md`) |
| Track inspector | FX slots, a 3-band integrated EQ, in/out | Slots only. The integrated EQ is hidden (we teach EQ 2) |

**Signal order (manual):** *"audio passes through any Effects loaded on the Effects Slots and then through the integrated effects (… stereo separation, volume and panning). Finally the audio is sent to the Master."* Our graph uses that order (§C0).

**Visual:** a dark blue-grey body (`#3B4650`) with column strips. The Master is leftmost with a wider meter. Selected tracks show a **green fader cap** (`#7CD43C`); record-armed ones are orange. Send links are drawn as **green cables** at the bottom. The FX slot list on the right is a dark column of rows reading "Slot 1…10", each with a round mix knob and a green LED.

### A8. FL Mixer facts the lessons teach (manual, verbatim facts paraphrased)

1. **All audio passes through the Mixer.** There are 500 insert tracks, one Current track and the Master. New channels go to the **Master** unless routed.
2. **Routing:** a channel's **Mixer track box** in the Channel Rack sets its insert. Click **TRACK** to auto-assign the first free insert. Ctrl+L routes the selected channels to the selected track; Ctrl+Shift+L routes them in sequence.
3. **Insert tracks practically can't clip** (32-bit float). *"Only the Master track or any track routed to an ASIO Output can clip."* Insert meters "are merely guides to the relative volumes".
4. **0 dB on the Master** is the limit. Orange peaks above 0 dB mean clipping in the render. Severe clipping "will distort and crackle" and usually can't be fixed afterwards.
5. **Sampler channels load at about −5.2 dB**, and the default note velocity is 100, so channels start below full level on purpose.
6. **dB rules of thumb:** +6 dB ≈ twice the level, −6 dB ≈ half, **±1 dB ≈ the just-noticeable difference.**
7. **Mix first, master second**, "first build a comfortable and well constructed chair". Mixing = level, pan, EQ, maybe compression per track. Mastering = EQ, multiband compression and/or limiting on the master.
8. Panning advice: *"+/- 40 % max is a good range"*, with kick and bass usually centred.
9. EQ advice: cut lows on everything that isn't bass. *"Cut… until you start to notice it, then back off a little."*
10. Reverb: *"Use subtle delay rather than reverb"* in busy mixes; automate the reverb amount.
11. Loudness: put Fruity Limiter last on the Master and raise GAIN, with "a little here and there, not all at once".

### A9. Making plugin windows clean on a phone (rules)

1. **One window at a time, full screen,** with FL's header (`◂ Fruity Parametric EQ 2 (Bass)`) and a **"Where is this in FL?"** chip. Window chrome follows `fl-ui-reference.md`.
2. **Never more than 6 live controls.** Locked controls are drawn at 40 % opacity with 🔒. The coach names the *one* control the step is about, and that knob pulses.
3. **Tap a knob → bottom-sheet slider**, as in `fl-ui-reference.md`. The sheet shows the FL name, the value with units, a **"Reset" (FL: Alt+click)** button and fine ±steps. Dragging the knob directly is allowed for experts.
4. **Every value has a plain-words subtitle,** e.g. "RATIO 4 : 1, *strong*" or "DEC 1.2 s, *small hall*". The subtitle bands are authored per control.
5. **The graph is the hero** where FL has one (EQ curve, limiter display, envelope, reverb room). Pictures teach faster than numbers.
6. **The Compare toggle sits in the same spot in every window** (bottom right, as FL's ▼⇅), and it is always loudness-matched.

---

## B. Curriculum: Levels 11–18 (30 lessons)

### B0. Conventions

**Hosts:** Padma (plan §2, sound and mix) leads. Lou (the sub-bass character) appears in every kick/bass lesson. Kiko hands over the mixer.

**Practice loop "Night Bus"** (preloaded unless a lesson says otherwise): 2 bars, 96 BPM, A minor.
- **Kick** on steps 1, 5, 9, 13 (four on the floor).
- **Clap** on 5, 13. **Hat** on 8ths, velocities 100/70.
- **Bass** (Lou: sine + saw, roots A–F | C–G).
- **Chords** (saw pad, Am–F | C–G). **Lead** (square pluck, a 6-note motif).

Each channel is routed to the insert of the same name (Kick → 1 … Lead → 6). **Insert 7 "Reverb"** and **insert 8 "Delay"** are send tracks, and the **Master** has Fruity Limiter in its last slot (bypassed until Level 18). Lessons list only what differs from this. A **"Use my hook"** button after each fresh example reapplies the lesson to the learner's own Pattern, ungraded.

**Lesson shape** (same as `studio-curriculum.md`):
1. **Hear** a level-matched A/B, with a question answered by ear.
2. **Guided**: one knob unlocks, glow plus hint ladder.
3. **Check.**
4. **Fresh**: a different loop or sound, cues off.
5. Optional **Use my hook**.
6. **FL tip** card.

**Measurements** (all on an offline render: warm-up 1 loop, measure 2 loops = 5 s at 96 BPM):

| Primitive | Definition |
|---|---|
| `peak(T)` | Max \|sample\| on track T's output (post-fader, post-pan) over the window, in dBFS. `peak(Master)` is post-limiter |
| `tp(T)` | True-peak estimate: 4× oversampled peak (§C8). Used only in 18.x |
| `loud(T)` | K-weighted mean-square loudness of T over the window, "LU" ≈ ungated LUFS (§C8). Used for **relative** comparisons |
| `loudDiff(A,B)` | `loud(A) − loud(B)` in dB |
| `gr(T)` | Compressor/limiter gain-reduction trace in dB (≥ 0). `grPk` = 95th percentile; `grFloor` = 10th percentile across each inter-hit gap |
| `band(T, lo, hi)` | Energy of T in [lo, hi] Hz (2× biquad band-pass, §C8), dB |
| `masking(A, B, lo, hi)` | Mean over frames where both A and B are above −40 dB in [lo, hi] of `min(bandA, bandB)`, minus the same for the reference mix. It falls when one gets out of the other's way |
| `ret(T → S)` | Loudness of send track S's output with only T's send active |
| `onOff(T, plugin)` | `loud(T with plugin) − loud(T bypassed)`. Used to require **level-matched** processing |
| `p(path)` | A parameter value, e.g. `p(Bass.eq[1].freq)` |
| `ear(q)` | Answer to an A/B question. These grade the *listening* objective separately from the *doing* objective, plan §6.4 style |

**Check robustness:**
- Ranges are wide enough that several settings pass.
- Every check reports the **first** failing rule with one sentence plus "Hear yours vs target".
- Every rule is unit-tested on an authored pass, a near miss and an alternate valid answer (the authoring rule in `studio-curriculum.md`).
- Any lesson whose idea is **below ~150 Hz** (11.2, 14.2, 15.2, 15.4, 16.4) asks for headphones first if the route is the phone speaker (Android `AudioDeviceInfo.TYPE_BUILTIN_SPEAKER`). Phone speakers reproduce little real bass [U: typical small-speaker roll-off; measure on the target phones].

---

### Level 11 · Sound design I: shape a sound (Synth window · Padma)

**11.1 `snd.shapes` · Four shapes**
- **New idea:** the oscillator **shape** sets the tone color. Sine is pure and round; saw is bright and buzzy; square is hollow; triangle is soft.
- **Setup:** Lead channel alone, one held A4 looping, OSC 1 = sine, OSC 2 mix 0 %. Envelope off (organ-like). Only OSC 1 Shape is unlocked.
- **Steps:** tap each shape and hear it (the engine loudness-matches shapes, §C9). Then answer three ear rounds. Then "make it the brightest".
- **A/B:** "Which one is brighter?" (sine vs saw, then square vs triangle, then triangle vs saw).
- **Check:** `ear ≥ 2/3` ∧ `p(Lead.osc1.shape) == SAW`.
- **Fresh:** a pad note at C4: "Make it soft and pure" → `shape ∈ {SINE, TRIANGLE}`.
- **Hint:** "More buzz means more high harmonics. Look at the scope: sharp corners mean bright."
- **FL tip:** add a **3x Osc** channel. The shape buttons are the icons on each oscillator row. Osc 1 is the top row.

**11.2 `snd.two-osc` · Two oscillators: octave and detune**
- **New idea:** a second oscillator an **octave down** adds weight; a few **cents of detune** adds thickness.
- **Setup:** Bass (Lou), a saw on OSC 1. OSC 2 is a saw at COARSE 0, mix 0 %. Unlocked: OSC 2 Mix level, COARSE, FINE.
- **Steps:** raise OSC 2 Mix to 50 % and hear it double. Set COARSE to −12 and hear it get heavier. Nudge FINE to +8…+20 ct and hear the slow "chorus" beating.
- **A/B:** "Which bass has two oscillators?" (both loudness-matched).
- **Check:** `p(osc2.coarse) == −12` ∧ `30 % ≤ p(osc2.mix) ≤ 60 %` ∧ `5 ct ≤ |p(osc2.fine)| ≤ 25 ct`.
- **Fresh:** a lead: "Add an octave **up** with a little detune" → `coarse == +12` ∧ fine within the same band.
- **Hint:** "−12 semitones is exactly one octave down."
- **FL tip:** in 3x Osc, COARSE is the big numbered knob (±24 st). FINE is ±1 semitone. On desktop the mix knobs are on rows 2 and 3 only, because osc 1 takes "the rest".

**11.3 `snd.attack-release` · Snap or swell (ATT and REL)**
- **New idea:** **attack** is how fast a note starts; **release** is how long it rings after you let go.
- **Setup:** Chords, a saw pad, Volume envelope ATT 2 ms, DEC 0, SUS 100 %, REL 20 ms. Unlocked: ATT, REL.
- **Steps:** hear a hard, clicky start. Raise ATT until the chord **swells** in. Raise REL until the chords overlap smoothly.
- **A/B:** "Which pad fades in?" / "Which one stops dead?"
- **Check:** `ATT ≥ 250 ms` ∧ `REL ≥ 400 ms` ∧ `ATT ≤ one bar (2.5 s)`.
- **Fresh:** Hat-like noise blip: "Make it snap" → `ATT ≤ 5 ms ∧ REL ≤ 80 ms`.
- **Hint:** "ATT longer than the gap between chords means the chord never reaches full volume."
- **FL tip:** on desktop, ADSR isn't on the 3x Osc panel. Open the channel's **Envelope / Instrument** tab, choose **Volume**, and switch the envelope **on** (orange LED) first.

**11.4 `snd.pluck-pad` · Pluck and pad from one patch** *(Jam: delivers plan §7's "a pluck and a pad from the same patch")*
- **New idea:** **decay** and **sustain** turn one sound into a short pluck or a held pad.
- **Setup:** Lead, the learner's 11.2 patch (or supplied). All four of ATT/DEC/SUS/REL are unlocked. The Pattern clones automatically into "Pluck" and "Pad" versions.
- **Steps:** Pluck: short attack, short decay, no sustain. Pad: slow attack, high sustain, long release.
- **Check (Pluck):** `ATT ≤ 10 ms` ∧ `100 ms ≤ DEC ≤ 500 ms` ∧ `SUS ≤ 15 %` ∧ `REL ≤ 300 ms`.
- **Check (Pad):** `ATT ≥ 150 ms` ∧ `SUS ≥ 60 %` ∧ `REL ≥ 400 ms`.
- **A/B:** none (a Jam). Checklist ✔/✗ per rule.
- **Hint:** "A pluck is all decay: SUS at zero means the note fades even while held."
- **FL tip:** save each as a **Channel preset** (FL saves 3x Osc sounds as *channel* presets, because the envelope lives on the channel).

### Level 12 · Sound design II: filter and movement

**12.1 `snd.lowpass` · Dark to bright: the low-pass filter**
- **New idea:** a **low-pass** filter removes highs above the **cutoff**. Lower cutoff = darker.
- **Setup:** Chords saw pad. Filter MOD X 100 % (bypassed, as FL's default), MOD Y 0 %. Unlocked: MOD X, then MOD Y.
- **Steps:**
  1. Sweep MOD X down while the loop plays, and watch the "630 Hz" readout and the EQ-style curve.
  2. Park it where the pad sits *behind* the lead.
  3. Add a little MOD Y and hear the peak at the cutoff.
- **A/B:** "Which pad is darker?" and "Which one has a resonant 'whistle'?" (MOD Y 0 vs 60 %).
- **Check:** `600 Hz ≤ cutoffHz(p(MOD X)) ≤ 2.5 kHz` ∧ `15 % ≤ p(MOD Y) ≤ 60 %` ∧ `peak(Chords) ≤ peak(Chords@start) + 3 dB` (resonance must not make it jump in level).
- **Fresh:** a bass with "make it rounder, no buzz" → `cutoff ≤ 400 Hz`.
- **Hint:** "MOD X is FL's name for the filter cutoff. 100 % means the filter is wide open."
- **FL tip:** on the Envelope / Instrument tab, the **Filter** box has MOD X and MOD Y and a type list (Fast LP is the default; try **SVF LP**).

**12.2 `snd.filter-env` · The pluck "yow": a filter envelope**
- **New idea:** an **envelope on the cutoff** opens the filter at the start of each note, then closes it.
- **Setup:** Lead square, MOD X 25 % (dark). The **Mod X** sub-tab is unlocked: envelope on, AMT, ATT, DEC, SUS.
- **Steps:** turn the Mod X envelope on. Raise AMT and hear each note "open". Shorten DEC for a snappy pluck.
- **A/B:** "Which lead *opens up* at the start of every note?"
- **Check:** `modX.env.on` ∧ `AMT ≥ +30 %` ∧ `ATT ≤ 10 ms` ∧ `80 ms ≤ DEC ≤ 500 ms` ∧ `SUS ≤ 30 %` ∧ `MOD X base ≤ 50 %`.
- **Fresh:** a bass: "a quick 'blip' of brightness per note" (same rule, DEC ≤ 250 ms).
- **Hint:** "AMT at the centre means the envelope does nothing. Turn it right."
- **FL tip:** Envelope / Instrument › **Mod X** tab: switch the envelope on and set AMT. It's the same ADSR you used for Volume.

**12.3 `snd.lfo` · Movement: the LFO wobble**
- **New idea:** an **LFO** is a slow repeating wave that moves a knob for you. Synced to tempo, it wobbles in time.
- **Setup:** Bass, saw, MOD X 40 %. The LFO box is unlocked (target Mod X): shape, AMT, SPEED, Tempo.
- **Steps:** turn on Tempo. Set SPEED to 1 beat (4 steps). Raise AMT. Then try ½ beat.
- **A/B:** "Which bass wobbles in time with the kick?" (synced vs free 2.3 Hz).
- **Check:** `lfo.target == MOD_X` ∧ `lfo.tempo == on` ∧ `lfo.speedSteps ∈ {2, 4, 8}` ∧ `20 % ≤ |AMT| ≤ 80 %`.
- **Fresh:** **vibrato** on the Lead: `target == PITCH ∧ speed free 4–7 Hz ∧ AMT small (≤ 15 %)` ("a singer's wobble is small and fast").
- **Hint:** "Tempo on makes SPEED count in steps. 4 steps = one beat."
- **FL tip:** each envelope tab (Volume, Mod X, Pitch) has its own **LFO** box. Tick **Tempo** to sync, then right-click SPEED › Set for musical values.

### Level 13 · Mixer basics (Mixer F9 · Kiko hands over to Padma)

**13.1 `mix.route` · Give every channel a track**
- **New idea:** each channel should go to its **own insert track** so it can be processed alone. Unrouted channels go straight to the Master.
- **Setup:** all six channels routed to **Master ("---")**, as on a fresh FL channel. The Mixer is open beside a mini Channel Rack.
- **Steps:**
  1. Drag Kick's **Mixer track box** to 1 and watch insert 1's meter come alive.
  2. Use **TRACK** (auto-assign) for the rest.
  3. Rename isn't needed; names follow the channel.
- **Check:** `∀ ch ∈ {Kick, Clap, Hat, Bass, Chords, Lead}: route(ch) ∉ {Master}` ∧ `all route(ch) distinct` ∧ `route ∉ {Reverb, Delay}` ∧ `each routed insert shows signal (peak > −60 dBFS)`.
- **A/B:** none. Instead, an ear check: "Solo insert 4. Which sound is it?" (four choices).
- **Hint:** "The little number box in front of each channel name is its Mixer track."
- **FL tip:** select channels in the Channel Rack, select a Mixer track and press **Ctrl+L** (or **Ctrl+Shift+L** to route several in a row).

**13.2 `mix.slots` · Mute, solo and effect slots**
- **New idea:** **solo** to find a sound, **mute** to hear the mix without it, and **effect slots** to process one track.
- **Setup:** the routed loop. One hidden "mystery" layer (a quiet shaker on insert 5).
- **Steps:**
  1. Find the shaker by soloing tracks (long-press the LED).
  2. Unsolo.
  3. Add **Fruity Parametric EQ 2** to the Hat track's **slot 1**.
  4. Toggle its LED off and on and hear the difference (the EQ has a preset cut so the change is audible).
- **Check:** `ear(which insert has the shaker) correct` ∧ `no track soloed at the end` ∧ `Hat.slot[1] == ParametricEQ2 ∧ enabled` ∧ `the LED was toggled ≥ 1 time`.
- **Hint:** "Solo is a long-press on the green light. On desktop it's right-click."
- **FL tip:** left-click an empty slot's arrow to load an effect. Ctrl-click (or right-click) a Mixer track's LED to solo it, Ctrl+right-click to unmute all.

**13.3 `mix.meters` · Read the meter: 0 dBFS and clipping**
- **New idea:** **0 dBFS** is the ceiling. Only the **Master** can clip; insert meters are a guide.
- **Setup:** all inserts +6 dB hot, master fader 0 dB. The master meter flashes **orange** with audible crackle (limiter bypassed).
- **Steps:**
  1. Hear the crackle.
  2. Pull the **insert faders** down together (multi-select). The master fader stays at 0 dB.
  3. Watch the clip dot clear (tap to reset).
- **A/B (level-matched):** "Which version crackles on the kick?" (clipped vs clean).
- **Check:** `peak(Master) ≤ −3.0 dBFS` ∧ `|p(Master.fader)| ≤ 0.5 dB` ∧ `no insert muted` ∧ `loudDiff(Kick, Lead)` within ±2 dB of its starting value (the balance is kept; everything came down together).
- **Fresh:** the clipping comes from one track (Bass +10 dB): fix only that track.
- **Hint:** "Turn down the tracks, not the Master. Leave the Master at 0 dB, and it stays an honest meter."
- **FL tip:** hold **Ctrl+Shift** and click several Mixer tracks to move their faders together. Orange peaks on the Master mean the export will clip.

### Level 14 · Levels and panning

**14.1 `mix.louder-trap` · Louder sounds better (and fools you)** *(principle lesson)*
- **New idea:** a mix that's **1–2 dB louder** sounds "better" even when nothing else changed. So always compare at the **same loudness**.
- **Setup:** two copies of the same mix; B is +1.5 dB. *Not* level-matched (the one deliberate exception).
- **Steps:**
  1. "Which mix is better?" Most pick B.
  2. Reveal: they're identical, and B is 1.5 dB louder.
  3. Turn on **Level-match** and hear them become identical.
  4. Then a real difference: B has a +2 dB high shelf. "Can you still hear a difference now?"
- **Check:** `ear(level-matched pair: "same or different?") correct for 2 of 3 rounds` (pairs: identical, identical, +3 dB shelf).
- **Hint:** "+1 dB is about the smallest change you can hear (FL manual: the 'just noticeable difference')."
- **FL tip:** on desktop, match levels by ear with the plugin's main level/GAIN before you bypass it. FL's ▼⇅ compare buttons don't match levels for you.

**14.2 `mix.balance` · Build the balance from the kick**
- **New idea:** a **static balance** comes first. Start with the most important element and add the rest around it.
- **Setup:** all faders at −∞. The kick is the anchor.
- **Steps:**
  1. Bring up Kick until its insert peaks around −10 dBFS.
  2. Add Bass under it ("you should feel Lou, not lose the kick").
  3. Add Clap, Hat, Chords, Lead in that order, one at a time.
- **A/B:** "In which mix does the bass cover the kick?" (bass +6 dB vs balanced).
- **Check:** `−14 ≤ peak(Kick) ≤ −6 dBFS` ∧ `−6 ≤ loudDiff(Bass, Kick) ≤ +1` ∧ `−12 ≤ loudDiff(Hat, Kick) ≤ −4` ∧ `all six faders > −∞` ∧ `peak(Master) ≤ −3 dBFS`.
- **Fresh:** a different loop (trap tempo, 808 bass).
- **Hint:** "If you can't hear the kick's thump in the first second, the bass is too loud."
- **FL tip:** in FL, mix with the Mixer faders. Channel Rack volume knobs set the level *going into* the insert effects.

**14.3 `mix.lead-on-top` · Lead on top, not in your face**
- **New idea:** the **lead** sits a little above the chords. Too high sounds pasted on; too low gets lost.
- **Setup:** Lead −8 dB under the chords. Unlocked: Lead fader only.
- **A/B:** "In which mix can you sing the melody back?" / "Which lead sounds pasted on top?" (+10 dB).
- **Check:** `+2 ≤ loudDiff(Lead, Chords) ≤ +7` ∧ `peak(Master) ≤ −3 dBFS`.
- **Hint:** "Raise it until the melody is clear, then a little less."
- **FL tip:** double-click (or Alt+click) a fader in FL to reset it to its default.

**14.4 `mix.pan` · Give everyone a seat (pan and a mono check)**
- **New idea:** **pan** places sounds left–right. Kick, bass and lead stay centred; supporting parts move out a little.
- **Setup:** everything at centre. Unlocked: pan knobs, the Master **Mono** check.
- **Steps:**
  1. Pan Hat right 20–40 %.
  2. Pan Chords left 20–40 %.
  3. Press **Mono** on the Master and check that nothing disappears.
- **A/B:** "Which mix is wider?" and "In which one are the hats on the right?"
- **Check:**
  - `|pan(Kick)| ≤ 5 %` ∧ `|pan(Bass)| ≤ 5 %` ∧ `|pan(Lead)| ≤ 10 %`
  - ∧ `+15 % ≤ pan(Hat) ≤ +40 %` ∧ `−40 % ≤ pan(Chords) ≤ −15 %`
  - ∧ `Mono was pressed ≥ 1 time and turned off again`.
- **Fresh:** a counter-melody (Lead 2): `−60 % ≤ pan(Lead2) ≤ −20 %`, opposite the hats.
- **Hint:** "FL's manual suggests staying within ±40 % for most parts."
- **FL tip:** FL's track **Stereo separation** knob turned fully right makes a track mono. Use it on the Master for a quick mono check, then set it back to the middle.

### Level 15 · EQ (Fruity Parametric EQ 2)

**15.1 `eq.sweep` · Where sounds live**
- **New idea:** the EQ graph runs from low (left) to high (right). Each instrument lives in a region: **SUB · BASS · LOW MID · MID · HIGH MID · PRS · TREBLE**.
- **Setup:** EQ 2 on the Master, spectrum on, one Peaking band (band 4) at +9 dB, BW narrow. Unlocked: FREQ for band 4.
- **Steps:**
  1. Sweep the boost slowly while the loop plays ("boost and sweep", which Sound On Sound recommends with a narrow Q of about 3–4).
  2. Stop where the **hats** jump out, then where the **bass** swells.
  3. Then three blind rounds.
- **A/B:** "Which boost is at 100 Hz / 1 kHz / 8 kHz?" (three loudness-matched clips).
- **Check:** `ear ≥ 2/3` ∧ (`hat stop: band4.freq ∈ [5 k, 14 k]`) ∧ (`bass stop: band4.freq ∈ [50, 160]`).
- **Hint:** "Read the strip above the graph: TREBLE is where hats sparkle, BASS is where Lou lives."
- **FL tip:** in EQ 2, hold **Shift** and click a band token to solo just that band. It's FL's built-in "where is it?" tool.

**15.2 `eq.lowcut` · Clear the low end (High pass)**
- **New idea:** a **high-pass (low cut)** removes rumble from parts that don't need bass. It is the most common fix in a mix.
- **Setup:** Chords and Hat have rumbly low end (their synths include sub content). Each insert has EQ 2 with band 1 unlocked.
- **Steps:**
  1. On **Chords**: band 1 type **High pass**, then raise FREQ until the chords start to thin, then back off.
  2. Repeat on **Hat**.
  3. On **Bass**, only remove sub-rumble.
- **A/B:** "Which mix has the muddier low end?"
- **Check:**
  - `Chords.eq[1].type == HP ∧ 100 ≤ freq ≤ 300 Hz`
  - ∧ `Hat.eq[1].type == HP ∧ 250 ≤ freq ≤ 800 Hz`
  - ∧ (`Bass.eq[1] off` ∨ `Bass.eq[1].type == HP ∧ 25 ≤ freq ≤ 45 Hz`)
  - ∧ `band(Master, 40, 150)` falls by ≥ 1.5 dB vs the start
  - ∧ `band(Bass, 50, 120)` is unchanged within 1 dB.
- **Fresh:** Lead: `HP ∈ [120, 350] Hz`.
- **Hint:** "Cut until you hear it change, then back off a little (FL manual)."
- **FL tip:** in EQ 2 drag a band's type icon to **High pass**. Use FL's default slope (−12 dB/oct) unless you need steeper.

**15.3 `eq.cut-first` · Cut before you boost**
- **New idea:** to fix a sound, **cut** the bad part with a narrow band rather than boosting everything else. Boost only a little and wide.
- **Setup:** Chords with a boxy +8 dB resonance at 450 Hz baked into the patch. Unlocked: one Peaking band (FREQ, Gain, BW).
- **Steps:**
  1. Find the box: boost-and-sweep as in 15.1.
  2. Flip the same band to a **cut**.
  3. Widen BW until the box is gone, but no further.
  4. Compare (level-matched).
- **A/B:** "Which chords sound boxy / 'cardboard'?"
- **Check:** `∃ Peaking band: 300 ≤ freq ≤ 650 Hz ∧ −10 ≤ gain ≤ −3 dB` ∧ `no band on Chords with gain > +3 dB` ∧ `|onOff(Chords, EQ)| ≤ 1.5 dB` (the cut didn't just make it quieter).
- **Fresh:** a harsh lead at 3 kHz: `∃ cut ∈ [2 k, 4.5 k], −8…−2 dB`.
- **Hint:** "Narrow cuts can go deep. Boosts should stay small and wide (Sound On Sound)."
- **FL tip:** right-click a token › **Key** sets the band to a note's frequency, which is handy when a single note rings.

**15.4 `eq.kick-bass` · Kick and bass: make room**
- **New idea:** kick and bass fight in the same low region. **Cut a little of one where the other is strongest.**
- **Setup:** the kick's fundamental is around 55 Hz; Lou's bass is also strong at 50–70 Hz. Unlocked: one Peaking band on Bass.
- **Steps:**
  1. Solo Kick: watch the spectrum peak and note its frequency (the coach labels it).
  2. On Bass, cut a band there by 2–6 dB, wide-ish.
  3. Un-solo and listen to the kick come through.
- **A/B:** "Which mix has the kick and bass fighting?"
- **Check:** `∃ Bass Peaking band: freq within ±25 % of f_kick (55 Hz → 41–69 Hz) ∧ −6 ≤ gain ≤ −2 dB` ∧ `masking(Kick, Bass, 40, 100)` improves by ≥ 1.5 dB ∧ `loudDiff(Bass, Kick)` stays within ±2 dB of its start.
- **Fresh:** a kick at 70 Hz, bass at 90 Hz → band at 56–88 Hz.
- **Hint:** "You aren't removing the bass, just making a pocket the kick can punch through."
- **FL tip:** on desktop you can also **sidechain** (Level 16.4) or move notes so kick and bass don't land together. FL's manual lists both.

### Level 16 · Compression (Fruity Compressor)

**16.1 `comp.threshold-ratio` · Even it out: threshold, ratio, gain**
- **New idea:** a compressor **turns down the loud parts** above the **threshold**, by the **ratio**. Then **gain** brings the whole thing back up.
- **Setup:** Bass played with uneven velocities (some notes +8 dB). Fruity Compressor on the Bass insert at Threshold 0 dB, Ratio 1:1. Unlocked one at a time: THRESHOLD → RATIO → GAIN. The GR meter is on.
- **Steps:**
  1. Lower THRESHOLD until the GR meter moves on the loud notes.
  2. Set RATIO to around 4:1.
  3. Raise GAIN until **Compare** (level-matched) shows the same loudness.
  4. Listen: the notes are now even.
- **A/B (level-matched):** "Which bass has more even notes?"
- **Check:** `THRES ≤ −12 dB` ∧ `2:1 ≤ RATIO ≤ 6:1` ∧ `2 ≤ grPk(Bass) ≤ 8 dB` ∧ `|onOff(Bass, Comp)| ≤ 1 dB` ∧ the note-to-note loudness spread (max − min per-note loudness) falls by ≥ 3 dB.
- **Fresh:** a vocal-like lead with a loud word.
- **Hint:** "Watch the meter, not just the knob: aim for about 3–6 dB of gain reduction on the loud notes."
- **FL tip:** FL's manual calls Fruity Compressor a legacy plugin and suggests **Fruity Limiter's COMP tab** for new projects. It has the same THRES, RATIO and GAIN.

**16.2 `comp.attack` · Attack keeps the punch**
- **New idea:** a **slow attack** lets the hit's first click through before the compressor clamps down. That's punch. A very fast attack flattens it.
- **Setup:** a drum bus (Kick + Clap routed to insert 9 "Drums"), compressor at THRES −20, RATIO 4:1, ATTACK 0.1 ms. Unlocked: ATTACK (then RELEASE fixed 120 ms).
- **A/B (level-matched):** "Which kick has more click and punch?" (0.1 ms vs 20 ms).
- **Check:** `10 ≤ ATTACK ≤ 40 ms` ∧ `3 ≤ grPk ≤ 7 dB` ∧ crest factor (peak − loud) of the Drums track is ≥ 2 dB higher than at 0.1 ms.
- **Hint:** "Attack is how long the compressor waits. Let the 'click' out before it closes."
- **FL tip:** route two channels to one insert to make a bus. Both Channel Rack track boxes get the same number.

**16.3 `comp.release-overdo` · Release, and how much is too much**
- **New idea:** **release** lets go between hits. Too long and it never recovers; too much compression sucks the life out.
- **Setup:** Chords crushed: THRES −40 dB, RATIO 20:1, RELEASE 2000 ms, GR about 15 dB constantly. It sounds flat and lifeless. Unlocked: THRESHOLD, RATIO, RELEASE.
- **Steps:** bring the GR down by raising THRESHOLD. Then lower RATIO. Then shorten RELEASE until the meter "breathes" back between chord changes.
- **A/B (level-matched):** "Which chords sound squashed and lifeless?"
- **Check:** `grPk ≤ 6 dB` ∧ `RATIO ≤ 4:1` ∧ `80 ≤ RELEASE ≤ 400 ms` ∧ `grFloor ≤ 1 dB` (it recovers between hits) ∧ `|onOff| ≤ 1 dB`.
- **Hint:** "Sound On Sound's test: push it, then see how *little* compression you can get away with."
- **FL tip:** on desktop, the "/R" Type options turn on **TCR**, an automatic release that helps avoid pumping.

**16.4 `comp.sidechain` · Duck the bass under the kick** *(bonus, Fruity Limiter COMP tab)*
- **New idea:** **sidechain** compression turns the bass down *when the kick plays*, so they stop fighting. It is the "pumping" sound of dance music when overdone.
- **Setup:** the 15.4 mix without the EQ pocket. A compressor on Bass with **SIDECHAIN = Kick**. Unlocked: THRES, RATIO, REL.
- **Steps:** lower THRES until the bass dips on each kick. Set REL so the bass is back before the next beat.
- **A/B:** "Which mix lets the kick punch through?" and "Which one is pumping on purpose?"
- **Check:** `sidechain == Kick` ∧ `3 ≤ grPk(Bass) ≤ 10 dB` ∧ `50 ≤ REL ≤ 250 ms` ∧ `grFloor(Bass) ≤ 1 dB before the next kick` ∧ `masking(Kick, Bass, 40, 100)` improves by ≥ 2 dB.
- **Hint:** "The bass should be back up before the next kick. Shorten REL if it isn't."
- **FL tip:** the FL manual's five steps: on the Kick track, right-click the Bass track's send switch › *Sidechain to this track*, then pick that track in Fruity Limiter's **SIDECHAIN** box on the COMP tab.

### Level 17 · Space: reverb and delay

**17.1 `fx.reverb-insert` · Near and far: Reeverb 2**
- **New idea:** reverb puts a sound **in a room**. More **WET** = further away; longer **DEC** = bigger space.
- **Setup:** Clap with Fruity Reeverb 2 in insert slot 1: DEC 1.0 s, WET 0 %, DRY 100 %. Unlocked: WET, then DEC.
- **Steps:**
  1. Raise WET and hear the clap move back.
  2. Change DEC from 0.5 s to 3 s; the wireframe room grows.
  3. Settle on a tight room.
- **A/B (level-matched):** "Which clap sounds further away?"
- **Check:** `0.4 ≤ DEC ≤ 1.5 s` ∧ `5 % ≤ WET ≤ 25 %` ∧ `DRY ≥ 80 %`.
- **Hint:** "Turn WET up until you clearly hear the room, then back off by a third."
- **FL tip:** in Reeverb 2, DEC is the time to fade by 60 dB. FL's manual: "Rarely should you use reverb on the Master track."

**17.2 `fx.reverb-send` · One room for everyone (a send track)**
- **New idea:** put **one** reverb on a **send track** and *send* parts to it. Everything sits in the same room, and the low end stays dry.
- **Setup:** insert 7 "Reverb" holds Reeverb 2 at DRY 100 %, WET 25 % (wrong for a send). All sends are at 0.
- **Steps:**
  1. Set the reverb track to **DRY 0 %, WET 100 %**.
  2. Turn up the Reverb send on Lead, Chords and Clap.
  3. Leave Kick and Bass dry.
- **A/B (level-matched):** "Which mix has the kick and bass in the reverb?" (muddy) vs "only the top parts".
- **Check:**
  - `Reverb.DRY == 0 %` ∧ `Reverb.WET ≥ 90 %`
  - ∧ `send(Lead→Reverb) > 0` ∧ `send(Chords→Reverb) > 0`
  - ∧ `send(Kick→Reverb) == 0` ∧ `send(Bass→Reverb) == 0`
  - ∧ `ret(Lead→Reverb) ≤ loud(Lead) − 8 dB`.
- **Hint:** "On a send track the reverb is 100 % wet. The *send knobs* decide how much each part gets."
- **FL tip:** select the Lead track, then click the small **send arrow** at the bottom of the Reverb track. It becomes the send-level knob. Sends are post-fader by default.

**17.3 `fx.reverb-clean` · Keep the reverb out of the way (L.CUT, pre-delay, not too much)**
- **New idea:** cut the reverb's lows (**L.CUT**), add a little **pre-delay** (DELAY) so the dry sound stays in front, and use **less** than you think.
- **Setup:** washed out: Reverb DEC 6 s, L.CUT off, DELAY 0, Lead send high. Unlocked: L.CUT, DELAY, DEC, and the Lead send.
- **Steps:** set L.CUT to about 300 Hz and hear the mud clear. Set DELAY to 20–60 ms. Bring DEC down to 1.5–2.5 s. Lower the Lead send until the reverb is felt more than heard.
- **A/B (level-matched):** "Which reverb muddies the bass?" / "Which lead is still upfront?"
- **Check:** `150 ≤ L.CUT ≤ 600 Hz` ∧ `15 ≤ DELAY ≤ 80 ms` ∧ `DEC ≤ 2.5 s` ∧ `ret(Lead→Reverb) ≤ loud(Lead) − 10 dB` ∧ `band(Reverb, 40, 150)` falls by ≥ 6 dB.
- **Hint:** "If you notice the reverb *as reverb*, it's probably too much. Try muting it: you should miss it."
- **FL tip:** Reeverb 2's DELAY has a **TEMPO** switch for beat-synced pre-delay. FL's manual suggests **delay instead of reverb** in busy parts.

**17.4 `fx.delay-sync` · Echoes in time (Fruity Delay 3)**
- **New idea:** a **tempo-synced** delay repeats in time with the beat. **Feedback** sets how many repeats; **cutoff** makes each repeat darker so it doesn't clutter.
- **Setup:** insert 8 "Delay" holds Fruity Delay 3 at Tempo Sync on, TIME 3 steps, FEEDBACK 0 %, Cutoff open, DRY 100 % (wrong for a send). Lead send to Delay is 0. Unlocked in order: DRY/WET, TIME, FEEDBACK Level, Cutoff, DELAY MODEL.
- **Steps:**
  1. Set DRY 0 %, WET 100 %.
  2. Send the Lead.
  3. Try 2, 3, 4 steps and keep the one that grooves.
  4. Feedback to 2–4 repeats.
  5. Cutoff down to about 3 kHz.
  6. Try **Ping pong**.
- **A/B (level-matched):** "Which echo is in time with the beat?" (3 steps vs 340 ms unsynced).
- **Check:** `TIME ∈ {2, 3, 4} steps (tempo sync on)` ∧ `15 % ≤ FB ≤ 50 %` ∧ `filter == LP ∧ Cutoff ≤ 6 kHz` ∧ `DRY == 0` ∧ `ret(Lead→Delay) ≤ loud(Lead) − 8 dB` ∧ `peak(Master) ≤ −1 dBFS`.
- **Fresh:** at 140 BPM; ping pong required: `model == PING_PONG`.
- **Hint:** "3 steps is a dotted eighth, the classic pop echo. One step = a sixteenth."
- **FL tip:** right-click Delay 3's **TIME › Set** for musical values. Each dot around the knob is one step.

### Level 18 · Mastering (the Master track)

**18.1 `mst.headroom` · Leave room before mastering**
- **New idea:** a finished **mix** should peak a few dB **below** 0 dBFS. Mastering adds the last loudness, not the mix.
- **Setup:** the learner's mix from Levels 14–17 (or a supplied mix peaking at +2 dBFS). Master limiter bypassed.
- **Steps:** select all inserts, lower them together until the Master peaks around −6 dBFS, and keep the master fader at 0 dB.
- **Check:** `−8 ≤ peak(Master, limiter bypassed) ≤ −3 dBFS` ∧ `|Master.fader| ≤ 0.5 dB` ∧ the balance is unchanged (all pairwise `loudDiff` within ±1 dB of the start).
- **A/B:** none (a technical step).
- **Hint:** "Move every fader by the same amount, and your balance survives."
- **FL tip:** FL's manual: "Concentrate FIRST on making a good mix. THEN work on mastering it."

**18.2 `mst.limiter` · Fruity Limiter: ceiling and gain**
- **New idea:** a **limiter** stops peaks at the **ceiling**. Raising its **GAIN** makes the track louder, until it starts to squash.
- **Setup:** Fruity Limiter (LIMIT tab) in the Master's last slot, enabled at GAIN 0, CEIL 0. Unlocked: CEIL, then GAIN, then REL.
- **Steps:**
  1. Set CEIL to −1.0 dB.
  2. Raise GAIN while watching the white gain line dip on the drums.
  3. Stop at a few dB of gain reduction.
  4. Then try way too much and hear the drums flatten.
- **A/B (level-matched):** "Which master has squashed drums?" (GR 3 dB vs 12 dB).
- **Check:** `−1.0 ≤ CEIL ≤ −0.3 dB` ∧ `peak(Master) ≤ CEIL + 0.05 dB over 2 loops` ∧ `1 ≤ grPk(Limiter) ≤ 5 dB` ∧ `grPk ≤ 6 dB`.
- **Hint:** "If the white line is dipping on every hit by more than about 5 dB, back off GAIN."
- **FL tip:** FL's manual suggests this exact move: Fruity Limiter last on the Master, raise **GAIN** with default settings. (Maximus, in Producer and up, is the multiband version.)

**18.3 `mst.loudness` · Loud enough, not crushed**
- **New idea:** streaming services **turn loud songs down** (Spotify to −14 LUFS), so squashing for loudness buys nothing. Aim for "about there" with clean peaks.
- **Setup:** the 18.2 master. A loudness readout appears ("−11.8 LU") plus a "Spotify playback preview" toggle that applies −14 LUFS normalization.
- **Steps:**
  1. Compare an over-limited master (GR 10 dB) with a moderate one in the normalization preview. The loud one now sounds *smaller*.
  2. Set GAIN for a final loudness in range.
- **A/B:** "With normalization on, which master has bigger drums?"
- **Check:** `−14 ≤ loud(Master) ≤ −9 LU` ∧ `tp(Master) ≤ −1.0 dBTP` ∧ `grPk(Limiter) ≤ 6 dB`.
- **Hint:** "Spotify's guidance: master around −14 LUFS integrated, true peak below −1 dB."
- **FL tip:** FL's **Wave Candy** has loudness meters. Peak meters alone are a poor guide to loudness (FL manual).

**18.4 `mst.capstone` · Mix and master your hook** *(Jam; the Level 11–18 Solo node)*
- **Setup:** the learner's own hook (from `studio-curriculum.md` 10.5) with default routing. Hints are allowed and logged.
- **Check (checklist ✔/✗):**
  - Routing: every channel on its own insert.
  - Low end: `HP on Chords, Lead, Hat`; `|pan(Kick)|, |pan(Bass)| ≤ 5 %`.
  - Balance: `−6 ≤ loudDiff(Bass, Kick) ≤ +1`, `+1 ≤ loudDiff(Lead, Chords) ≤ +7`.
  - Dynamics: no insert with `grPk > 8 dB`; every compressor has `|onOff| ≤ 1.5 dB`.
  - Space: `send(Kick→Reverb) == 0 ∧ send(Bass→Reverb) == 0`; `ret(any → Reverb) ≤ source − 8 dB`.
  - Master: mix headroom `peak(pre-limiter) ≤ −3 dBFS`; `CEIL ∈ [−1, −0.3]`; `peak ≤ CEIL`; `−14 ≤ loud ≤ −9 LU`; `grPk(Limiter) ≤ 6 dB`.
- **Then:** export WAV, plus a **desktop mission**: "Rebuild this mix in FL with the same plugins (all are in Fruity Edition). Check-in: what CEIL did you use on Fruity Limiter? Which send track holds Reeverb 2?"

### B9. Common-mistake detectors (coach nudges, available in free play too)

These run on every render and are shown once per session, with the lesson that fixes each.

| Mistake | Detector | Nudge (links to) |
|---|---|---|
| Clipping the master | `peak(Master) > 0 dBFS` (limiter off) | "Your master is clipping: turn tracks down, not the master" (13.3) |
| Mixing too loud overall | `peak(Master pre-limiter) > −1 dBFS` for > 50 % of loops | "Leave room for mastering" (18.1) |
| Fixing with the master fader | `Master.fader < −3 dB` while inserts are > +3 dB | (13.3) |
| Over-compressing | any `grPk > 10 dB` or `ratio ≥ 10 ∧ THRES ≤ −30` | "Try half as much" (16.3) |
| Unmatched compare | `|onOff| > 2 dB` on a compressor/EQ | "Louder isn't better: match it with GAIN" (14.1) |
| Boosting instead of cutting | any EQ band `gain > +6 dB`, or ≥ 3 bands boosted > +3 dB | "Try cutting what's in the way" (15.3) |
| Too much reverb | `ret(T→Reverb) > loud(T) − 6 dB`, or `DEC > 4 s` in a busy loop, or `WET > 40 %` on an insert reverb | "Mute the reverb: do you miss it, or does it get clearer?" (17.3) |
| Low end in the reverb | `send(Kick or Bass → Reverb) > 0` | (17.2) |
| Runaway delay | `FB > 80 %` | (17.4) |
| Solo left on | a solo active at export | — |
| Everything centred | all pans 0 with ≥ 5 tracks | (14.4) |
| Speaker for bass lessons | route == built-in speaker during 11.2, 14.2, 15.x, 16.4 | "Plug in headphones: phone speakers can't play Lou's lowest notes" |

### B10. Teaching principles (what the research agrees on, and how we enforce it)

1. **Ear first, numbers second.** Each lesson opens with a level-matched A/B and a question before any knob. Ear answers are graded separately from settings (plan §6.4's "visual practice is not ear-training mastery").
2. **Level-matched A/B, always,** done by the engine (§C8). Without it, louder wins every comparison: FL puts the JND at about 1 dB, and Sound On Sound warns that auto-make-up "one-knob" compressors make compressed sounds feel better and "encourage inexperienced users to dial in more compression than is necessary". Lesson 14.1 makes the bias felt once, then it is designed out.
3. **One knob at a time.** Lessons unlock a single control per step; the rest are visible but locked. This matches the "one new idea" rule in plan §7.
4. **Too much, then back off.** FL's manual (EQ cuts) and Sound On Sound (compression) both teach "push past it, then pull back". Lessons 15.2, 16.3 and 17.1 script it.
5. **Judge in context, not solo.** Solo is for *finding* (15.1, 15.4). Checks measure the full mix. Sound On Sound: evaluate EQ "in the context of the mix, rather than while the sound in question is soloed".
6. **Cut before boost, small boosts wide, narrow cuts deep** (Sound On Sound *Using EQ*).
7. **Gain reduction is the truth for compressors.** Target ranges are in dB of GR, not knob positions (Sound On Sound recommends a GR meter; starting point around 6 dB, then reduce).
8. **Mix before master;** the master fader stays at 0 dB and the master limiter starts bypassed (FL manual).
9. **Loudness isn't a goal on its own:** normalization preview (Spotify −14 LUFS, −1 dBTP).
10. **Protect ears and translation.** Suggest headphones for low-end lessons, use moderate playback level, and offer a break prompt after about 20 minutes of mix lessons [U: ear-fatigue guidance; general practice, not sourced here].

---

## C. DSP recipes for a real-time Kotlin engine

### C0. Graph, order and CPU budget

```
per channel (mono):  voices ─► channel volume/vel ─► INSERT FX (mono): EQ2 bands ─► Compressor ─► [Reverb/Delay if inserted]
                     ─► fader (dB) ─► pan (const-power) ─► stereo bus
                     └─ post-fader sends (mono) ─► Reverb send track (mono in → stereo out) ─► fader
                                               └─► Delay send track (mono in → stereo out)  ─► fader
master (stereo):     Σ tracks ─► Master EQ2 (stereo, ≤ 3 bands) ─► Fruity Limiter (stereo-linked) ─► master fader ─► meters ─► DAC
```

- **Mono inserts** halve the cost. Every source here is mono (drums, mono synth voices). FL's own order is FX slots → volume → pan, so processing before the pan is faithful. Stereo begins at the pan or at the send returns.
- **Insert reverb (17.1)** runs mono-in, stereo-out. A track whose last FX slot is stereo-out switches to a balance-style pan, as FL's Balance plugin does: it pans by mixing L into R.
- **Budget** (per output sample at 48 kHz; mul-adds, rough):

| Block | Count | Cost each | Total |
|---|---|---|---|
| Synth voice (2 polyBLEP osc + SVF + 2 env) | 16 voices max | ~40 | 640 |
| Drum sample voice | 6 | ~8 | 48 |
| Insert EQ (≤ 4 active biquads, mono) | 9 tracks | 4 × 5 = 20 | 180 |
| Compressor (log + exp per sample) | 9 | ~30 (with fast log2/exp2) | 270 |
| Reverb (16 combs + 8 AP + filters) | 1 | ~110 | 110 |
| Delay (2 taps + LP + interpolation) | 1 | ~25 | 25 |
| Master EQ (3 bands stereo) + limiter + meters | 1 | ~80 | 80 |
| **Total** | | | **≈ 1 350 ops/sample ≈ 65 M ops/s** |

That is roughly **2–4 % of one 2+ GHz ARMv8 core**, even with the JVM's overhead [U: an estimate; profile on the target low-end phone]. Rules:
- Allocation-free `process()`.
- Coefficients recomputed at **control rate** (every 32 samples), with smoothed parameters.
- Skip blocks whose input is silent (FL's "smart disable").
- Mark idle voices.
- Biquads, envelopes and the compressor can run in `Float`. Keep reverb and comb state in `Float` too, but flush denormals (below).

**Denormals:** ART on ARM64 doesn't promise flush-to-zero [U]. Add `1e-20f` alternating sign to comb/filter inputs, or snap `|state| < 1e-15 → 0` once per block.

**Offline check render:** the same graph, run on a worker thread into a `FloatArray`, with no `AudioTrack`. Render 3 loops (1 warm-up + 2 measured) in under 0.5 s on a mid-range phone [U: estimate].

### C1. Parameter smoothing (no zipper noise, no clicks)

- One-pole smoother per continuously-variable parameter: `s += k·(target − s)`, with `k = 1 − exp(−1/(τ·fs))` and **τ = 20 ms** for gains, cutoffs and EQ freq/gain.
- Smooth gain in **linear** terms and frequency in **log2** terms.
- Recompute biquad/SVF coefficients from the smoothed values every 32 samples (biquad) or every sample (SVF, which is cheap: one `tan` via a lookup table or `tan` per 32 samples plus interpolated `g`).
- Discrete switches (filter type, delay model, shape) **crossfade over 10 ms** between old and new outputs.

### C2. EQ: RBJ biquads (W3C *Audio EQ Cookbook*)

Given `fs`, `f0`, `dBgain`, `Q`:

```
A     = 10^(dBgain/40)                 // peaking & shelves only
w0    = 2π·f0/fs ;  c = cos w0 ;  s = sin w0
alpha = s / (2Q)                       // or from bandwidth BW (octaves): alpha = s·sinh( ln2/2 · BW · w0/s )
shelf: 2√A·alpha = s·√((A²+1)(1/S − 1) + 2A)   (S = shelf slope; S = 1 is steepest monotonic)
```

| Type (FL name) | b0 | b1 | b2 | a0 | a1 | a2 |
|---|---|---|---|---|---|---|
| **High pass** (low cut) | (1+c)/2 | −(1+c) | (1+c)/2 | 1+α | −2c | 1−α |
| **Low pass** (high cut) | (1−c)/2 | 1−c | (1−c)/2 | 1+α | −2c | 1−α |
| **Peaking** | 1+α·A | −2c | 1−α·A | 1+α/A | −2c | 1−α/A |
| **Low shelf** | A[(A+1)−(A−1)c+2√Aα] | 2A[(A−1)−(A+1)c] | A[(A+1)−(A−1)c−2√Aα] | (A+1)+(A−1)c+2√Aα | −2[(A−1)+(A+1)c] | (A+1)+(A−1)c−2√Aα |
| **High shelf** | A[(A+1)+(A−1)c+2√Aα] | −2A[(A−1)+(A+1)c] | A[(A+1)+(A−1)c−2√Aα] | (A+1)−(A−1)c+2√Aα | 2[(A−1)−(A+1)c] | (A+1)−(A−1)c−2√Aα |
| Band pass (0 dB peak) | α | 0 | −α | 1+α | −2c | 1−α |
| Notch | 1 | −2c | 1 | 1+α | −2c | 1−α |

- Normalize by a0.
- Run in **transposed direct form II**: `y = b0·x + z1; z1 = b1·x − a1·y + z2; z2 = b2·x − a2·y`.
- **FL's "BW"** = octaves. Map our BW knob to 0.1–4 octaves (log) and convert to `alpha` with the BW formula. Show Q in "More": `Q = 1/(2·sinh(ln2/2·BW))`.
- **Slopes:** FL "2" = one biquad at Q 0.707 (12 dB/oct). "Steep 4" = two cascaded biquads at **Butterworth Qs 0.5412 and 1.3066** (24 dB/oct).
- **The graph must match what you hear:** plot `|H(e^{jω})|` per band summed in dB (the existing `Biquad.responseDb` does exactly this) at 256 log-spaced points, recomputed only when a parameter changes.
- **Frequency clamp:** `f0 ≤ 0.45·fs`. Near Nyquist, RBJ shelves/peaks "cramp" (FL's HQ+ "decramped" exists for that reason). That is acceptable for teaching; just stop the Peaking band at 16 kHz in lessons.

### C3. Compressor (Fruity Compressor model: feed-forward, log-domain, soft knee)

Topology: a gain computer in dB followed by attack/release smoothing of the **gain reduction** (a "log-domain, decoupled/branching" detector). This is the design analysed by Giannoulis, Massberg & Reiss, *Digital Dynamic Range Compressor Design – A Tutorial and Analysis*, JAES 2012 **[U: paper not fetched this pass; formulas are standard]**. The existing `Compressor` in `Dsp.kt` already follows it.

```
per sample (mono insert; for stereo, detect on max(|L|,|R|)):
  xd  = 20·log10(|x| + 1e-9)                               // detector input in dB  (RMS option: see below)
  ov  = xd − T                                              // T = Threshold (dB), R = Ratio, W = knee width (dB)
  gc  = if (2·ov < −W)          0
        else if (2·|ov| ≤ W)    (1 − 1/R) · (ov + W/2)² / (2W)
        else                    (1 − 1/R) · ov              // desired gain reduction (dB, ≥ 0)
  αA  = exp(−1/(attack_s·fs)),  αR = exp(−1/(release_s·fs))   // attack 0 → αA = 0 (instant)
  gr  = if (gc > gr) αA·gr + (1−αA)·gc else αR·gr + (1−αR)·gc
  y   = x · 10^((makeup − gr)/20)
  meter: grMeter = max(grMeter, gr) per UI frame; also log gr into the check trace
```

- **Knee:** Hard 0 / **Medium 6 (lessons)** / Vintage 7 / Soft 15 dB, matching FL's Type list. For **"Vintage"**, ease the ratio toward 1:1 far above threshold: `Reff = 1 + (R−1)·exp(−max(0, ov−12)/12)` [U: our approximation of the manual's description].
- **Ratio < 1** (FL allows 0.4:1) means expansion. The same formula works, with `gc` negative and clamped to ≥ −12 dB. Hidden in lessons.
- **RMS detection** (Fruity Limiter COMP's SUSTAIN): `ms += k·(x² − ms)` with a window of SUSTAIN ms, then `xd = 10·log10(ms)`. Use it for the Drums bus option; peak detection is the default.
- **Sidechain:** compute `xd` from the key track's post-fader mono signal (the Kick); apply the gain to the Bass. Key buffers are available because the engine renders tracks in dependency order: key tracks first.
- **Cost:** replace `log10`/`pow` with fast `log2`/`exp2` approximations (5th-order polynomial on the mantissa; error < 0.01 dB). That matters only if profiling says so.
- **Level-matched compare** (`onOff`): measured on the offline render, not real-time.

### C4. Limiter / ceiling (no look-ahead)

The Fruity Limiter LIMIT model is: input **GAIN** → peak limiter to **CEIL** with **REL**, stereo-linked.

```
in   = x·g_in                              // GAIN (dB → linear)
pk   = max(|inL|, |inR|)
need = if (pk > ceil) ceil/pk else 1       // gain needed this sample
env  = if (need < env) need                 // instant attack (no look-ahead)
       else env + (1 − αR)·(need − env)     // REL: αR = exp(−1/(rel_s·fs)), default 80 ms
       // optional "hold": keep env for 2–5 ms after a new minimum before releasing (reduces low-freq distortion)
out  = in·env
out  = softSafety(out)                      // see below; guarantees ≤ ceil
grDb = −20·log10(env)
```

- **Why it's safe:** with instant attack, `|out| ≤ ceil` holds on every sample, so the master check `peak ≤ CEIL` is exact (sample peak).
- **Cost:** instant gain changes distort bass a little at high GR. Mitigations, in order:
  1. Release ≥ 50 ms plus a 3 ms hold.
  2. **Two-stage:** a gentle stage at `ceil + 2 dB` with 1 ms attack takes the bulk; the instant stage catches the rest.
  3. If quality matters more than zero latency, add a **1.5 ms look-ahead** (72 samples at 48 kHz). Run a sliding-window **minimum of `need`** over the window (monotonic deque, O(1)) and smooth it with a short ramp. Delay the audio by the same 72 samples. 1.5 ms of latency is irrelevant to lesson playback (nothing is graded live) but would matter if a live-play mode is added.
- **softSafety:** `if |y| > ceil·0.98 → y = sign·ceil·(0.98 + 0.02·tanh((|y|/ceil − 0.98)/0.02))`. It only touches the top 2 % and never exceeds `ceil`.
- **True peak (18.3):** the meter oversamples 4× with a 16-tap-per-phase polyphase FIR (windowed sinc), on the meter path only. The limiter itself stays sample-peak. Lessons set CEIL ≤ −1.0 dB to leave room for inter-sample overs.

### C5. Tempo-synced feedback delay (Fruity Delay 3 subset)

```
stepSec   = 60 / BPM / 4                     // one FL step = 1/16 note
D         = TIME_steps · stepSec · fs        // samples; smoothed (C1) and read with linear interpolation
buffer    = 4 s per channel (16 steps = one bar = 4 s at 60 BPM); clamp D ≤ buffer − 2
read:     yL = interp(bufL, w − D),  yR = interp(bufR, w − D·(1+offset))
feedback: fbL = LP(yL) · FB,  fbR = LP(yR) · FB          // LP = one-pole or SVF-LP at Cutoff (Res ≤ 0.5)
          fb  = tanh-ish saturator (Delay 3's "Feedback distortion: Sat"): fb = fb / (1 + |fb|·0.25)  keeps FB ≥ 100% bounded
write:    Mono:      bufL[w] = in + fbL;            bufR mirrors bufL
          Stereo:    bufL[w] = inL + fbL;  bufR[w] = inR + fbR
          Ping pong: bufL[w] = in + fbR;  bufR[w] = fbL                 // mono in → left only; cross-feed swaps sides each repeat
out:      L = Dry·inL + Wet·yL ;  R = Dry·inR + Wet·yR                  // send track: Dry = 0
```

- **TIME changes:** when Keep Pitch is off, as in FL, slew `D` with the 20 ms smoother; the pitch bend is the authentic Delay 3 behavior. In lessons, **crossfade** between two read heads over 30 ms instead (no pitch artefact). That is "Keep Pitch on" behavior.
- **FB clamp:** 0–90 % in lessons; the saturator allows up to 110 % in free play.
- Optional **Tone** (output LP/HP) = one more one-pole.
- **Mono-in ping pong** (our sends are mono): feed the input only to the left line, so repeats alternate L, R, L…

### C6. Light algorithmic reverb (Freeverb core, mapped to Reeverb 2's controls)

**Freeverb structure** (JOS, *Physical Audio Signal Processing*, figure read today):
- Mono input (`(L+R)·gain`, gain = 0.015) feeds **8 parallel lowpass-feedback combs**, then **4 series allpasses**, per channel.
- Combs at 44.1 kHz: **1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617** samples.
- Allpasses: **556, 441, 341, 225**, with feedback **g = 0.5**.
- The right channel adds **stereospread = 23** samples to all twelve lengths.
- Defaults: `damp = 0.5·0.4 = 0.2`, `feedback f = roomsize·0.28 + 0.7 = 0.84`.
- Output: `outL·wet1 + outR·wet2 + inL·dry`.

**Lengths at 48 kHz** (×48000/44100 = 1.08844, rounded):

| | L (48 kHz) | R = L + 25 |
|---|---|---|
| Combs | 1215, 1293, 1390, 1476, 1548, 1623, 1695, 1760 | 1240, 1318, 1415, 1501, 1573, 1648, 1720, 1785 |
| Allpasses | 605, 480, 371, 245 | 630, 505, 396, 270 |

Prime-adjusting the scaled lengths (e.g. 1213, 1291, 1381, 1471, 1549, 1621, 1693, 1759) reduces coincident echoes [U: common practice, optional].

**Comb and allpass** (per sample):

```
comb:    y = buf[i];  store = y·(1−d) + store·d;  buf[i] = x + store·f;  i++   → returns y
allpass: b = buf[i];  y = −x + b;  buf[i] = x + b·g;  i++                       → returns y  (g = 0.5)
```

**Mapping FL Reeverb 2 controls to the core:**

| Reeverb 2 | Implementation |
|---|---|
| **DEC** (s, time to −60 dB) | Per-comb feedback from the decay time instead of "roomsize": `f_k = 10^(−3·N_k / (DEC·fs))`, with N_k the comb length (e.g. DEC 1.5 s, N 1476 @48 k → f = 10^(−0.0615) ≈ 0.868). Clamp f ≤ 0.985. The allpasses set a lower bound of about 0.3 s |
| **SIZE** | Scale all delay lengths by `0.6 + 0.4·size` (allocate buffers at ×1.0). Change it only when the room is quiet, or crossfade between two instances on SIZE edits (it isn't automated in lessons). The wireframe display scales with it |
| **DAMP** | `d = 0.05 + 0.6·damp` (0 = bright). FL's max = OFF → `d = 0` |
| **DELAY** (pre-delay) | A 0–200 ms delay line on the mono input, tempo option: steps × stepSec |
| **L.CUT** | One-pole (or 12 dB biquad) high-pass on the **reverb input**, 20 Hz–1 kHz. Lesson 17.3 expects 150–600 Hz |
| **H.CUT** | One-pole low-pass on the **reverb output**, 1–20 kHz |
| **WET / DRY** | `wet1 = WET·(1 + SEP)/2`, `wet2 = WET·(1 − SEP)/2` (SEP = 1 default, full width), `dry = DRY`. Send track: DRY = 0 |
| ER, DIFF, MOD, BASS, CROSS | Not in lessons. Optional: ER = 4 taps from the pre-delay line (e.g. 7, 11, 17, 23 ms). MOD = ±8-sample LFO on two comb lengths (fractional read) to break up metallic ringing |

**Cost:** ~24 delay-line reads and writes plus about 60 mul-adds per sample. Fine on one shared send.

### C7. Pan law, faders and sends

- **Constant-power pan** (FL "Circular panning law": centre is 3 dB down relative to hard pan):
  `θ = (p + 1)·π/4` for `p ∈ [−1, +1]` (−100 % … +100 %); `gL = cos θ`, `gR = sin θ` → centre `0.7071` each (−3 dB).
  The existing engine multiplies by √2 so that centre = unity. That is the same law with a +3 dB offset, which is fine. Keep it consistent everywhere, *including* the offline check render, so meter readings in lessons match.
- **Stereo-in balance** (insert reverb or a stereo source, as Fruity Balance / FL mixer pan): for `p > 0`, `L' = L·cos(p·π/2)`, `R' = R + L·sin(p·π/2)`, and mirror for `p < 0` [U: FL says its pan "works by mixing one side… into the other"; exact curve unverified].
- **Fader:** stored in dB, −∞ … +6 dB, default 0 dB. On-screen taper (our design; FL's exact taper is [U]):
  `pos ∈ [0,1]`, `dB = 6 + 40·log10(pos)` for pos > 0.03 (pos 1 → +6, pos 0.708 → 0 dB, pos 0.25 → −18 dB, pos 0.1 → −34 dB), and −∞ below 0.03.
  Apply as smoothed linear gain.
- **Sends:** post-fader, after pan for stereo or before pan for our mono sends. Send knob 0–100 % maps to −∞ … 0 dB with the same taper as the fader. FL sends are post-fader by default (manual).

### C8. Meters and loudness (also used by the checks)

- **Peak meter** (per track, per channel): `pk = max(|x|)` per block, displayed as `20·log10(pk)`.
  - Ballistics: instant rise, fall **20 dB/s**.
  - Peak-hold tick for 1.5 s.
  - **Master clip latch** when `pk > 1.0` (0 dBFS); tap to clear.
  - Colors: green below −6 dB, yellow −6…0 dB, **orange above 0 dB**, as in FL.
  - Range −60…+6 dB.
- **RMS** (for "loud" bars): `ms += k·(x² − ms)` with τ = 300 ms; dB = 10·log10(ms). Show RMS as a darker inner bar.
- **K-weighted loudness** (ITU-R BS.1770; coefficients for **48 kHz**) [U: from memory of the BS.1770 / pyloudnorm values; verify before shipping]:
  - Stage 1 (high shelf, +4 dB): `b = [1.53512485958697, −2.69169618940638, 1.19839281085285]`, `a = [1, −1.69065929318241, 0.73248077421585]`.
  - Stage 2 (RLB high-pass): `b = [1, −2, 1]`, `a = [1, −1.99004745483398, 0.99007225036621]`.
  - Other sample rates: design stage 1 as an RBJ high shelf (f0 ≈ 1681.97 Hz, +3.99984 dB, Q ≈ 0.70718) and stage 2 as an RBJ high-pass (f0 ≈ 38.135 Hz, Q ≈ 0.50033) [U].
  - Loudness = `−0.691 + 10·log10(Σ_ch mean(z_ch²))` over the window.
  - **Momentary** = 400 ms window, **short-term** = 3 s. Our **`loud(T)`** = ungated mean over the 2 measured loops. True integrated LUFS adds gating (−70 LUFS absolute, −10 LU relative on 400 ms blocks with 75 % overlap); that is only needed if we ever claim "LUFS". The UI says "LU".
- **Loudness matching for A/B:** render both versions offline, compute `loud(A)` and `loud(B)`, and apply `−(loud(B) − loud(A))` dB to B on playback. Re-measure after every change, debounced 300 ms. Badge: "Level-matched ✓ (B −1.4 dB)".
- **Band energy** (`band(T, lo, hi)`): an RBJ high-pass at `lo` then a low-pass at `hi`, both Q 0.707, then RMS. **Masking** frames are 50 ms.
- **Gain-reduction trace:** the compressor/limiter write `gr` (dB) into a per-track ring at 1 kHz decimation for the check render, giving `grPk` (95th percentile) and `grFloor` (10th percentile of each inter-onset gap, from the Pattern's note-on times).

### C9. Subtractive synth voice (3x Osc + Envelope tab)

**Oscillators** (phase `t ∈ [0,1)`, increment `dt = f/fs`):

```
polyBLEP(t, dt):  if t < dt:      u = t/dt;        return u + u − u·u − 1
                  if t > 1 − dt:  u = (t − 1)/dt;  return u·u + u + u + 1
                  else 0
saw     = (2t − 1) − polyBLEP(t, dt)
square  = (t < 0.5 ? 1 : −1) + polyBLEP(t, dt) − polyBLEP((t + 0.5) mod 1, dt)
triangle= 1 − 4·|t − 0.5|      // naive is acceptable: its harmonics fall 12 dB/oct, so aliasing is low
                                // (for a cleaner one: integrate the BLEP square, tri += 4·dt·square, with a 0.999 leak)
sine    = sin(2πt)  (table lookup, 2048 points + linear interpolation)
noise   = xorshift32 → [−1, 1]
```

(polyBLEP as given in Martin Finke's article, credited there to "Tale".)

- **Shape loudness match:** RMS sine 0.707, saw 0.577, square 1.0, triangle 0.577. Multiply by `[1.0, 1.225, 0.707, 1.225]` (sine, saw, square, tri) so shapes switch at equal RMS. Lesson 11.1 additionally loudness-matches via `loud()`, since brightness changes perceived loudness.
- **Osc 2:** `f2 = f·2^((COARSE + FINE_cents/100)/12)`. Ratio mix as in FL: `out = (1 − m2)·osc1 + m2·osc2`.
- **ADSR (Volume, Mod X):** linear attack; exponential decay and release toward targets with a 1 % overshoot so they arrive on time:
  - `coef = exp(−ln((1 + o)/o) / (time_s·fs))`, `o = 0.01`
  - decay: `env = SUS − o + (env − (SUS − o))·coef`, clamped at SUS
  - release: target `−o`, end the voice at `env < 1e-4`
  - Velocity scales the peak.
  - Retrigger from the current level (no click).
- **Filter: Cytomic TPT state-variable filter** (Andrew Simper, *SvfLinearTrapOptimised2*). It is stable under fast modulation all the way to Nyquist:

```
g  = tan(π·fc/fs)          // fc clamped to [20, 0.49·fs]; tan from a 1024-pt table over [0, 0.49] + lerp, or per-sample tan
k  = 2 − 2·res              // res ∈ [0, 0.98]  → k ∈ [0.04, 2]   (k = 1/Q)
a1 = 1/(1 + g·(g + k));  a2 = g·a1;  a3 = g·a2
per sample:
  v3 = x − ic2eq
  v1 = a1·ic1eq + a2·v3
  v2 = ic2eq + a2·ic1eq + a3·v3
  ic1eq = 2·v1 − ic1eq;  ic2eq = 2·v2 − ic2eq
  low = v2;  band = v1;  high = x − k·v1 − v2
```

- **MOD X → cutoff:** `fc = 20·1000^x` Hz (0 % = 20 Hz, 50 % ≈ 632 Hz, 70 % ≈ 2.5 kHz, 100 % = 20 kHz). At MOD X = 100 % and MOD Y = 0, **bypass** (FL's default).
- **Modulation:** `x_eff = MODX + AMT·envModX(t) + lfoAmt·lfo(t)`, clamped to [0, 1]. Key-tracking is optional (+0.5 octave per octave).
- **"LPx2"** (free play) = two SVFs in series, the second with k = 1.41.
- **Resonance loudness guard:** multiply the output by `1/(1 + 0.6·res)`, so the 12.1 check "resonance didn't jump the level" has a fair chance.
- **LFO:** sine/triangle/square from a phase accumulator. Tempo mode: `rate = 1/(speedSteps·stepSec)`. Global vs per-note restart as in FL's **Global** switch. **Pitch** target: `f·2^(AMT·lfo·range/12)` with a range of ±1 semitone in lessons (vibrato).
- **Voices:** 8 per synth channel. Steal the oldest released voice first, with a 5 ms fade on steal.
- **Per-voice cost:** about 40 mul-adds, plus a table `tan` once per 16 samples when cutoff modulation is smooth.

### C10. Review of the existing `android/.../audio/Dsp.kt` (what to change)

The current engine (one insert per channel: HP, low shelf, peak, high shelf → compressor → fader → constant-power pan; shared Freeverb and delay sends; master limiter) matches §C0's graph. Six changes:

1. **`Svf` is a Chamberlin SVF, clamped to `sr/6`** (8 kHz at 48 kHz). MOD X can't open fully, and it misbehaves under fast envelope sweeps. **Replace it with the Cytomic TPT SVF** (§C9).
2. **`Reverb.set(size, damping)` has no decay time, pre-delay, L.CUT or H.CUT**, but Levels 17.1–17.3 grade **DEC**, **DELAY** and **L.CUT**. Add the mapping in §C6 (per-comb feedback from DEC, a pre-delay line, an input HP and an output LP).
3. **`Delay` uses integer, unsmoothed `delaySamples`**, so tempo/TIME changes click. It has no ping-pong mode and no cutoff in Hz. Add the interpolated read plus crossfade, Mono/Ping pong, and an LP at Cutoff (§C5).
4. **The compressor's `meterGr` only keeps the max per block.** Also write the GR trace for checks (§C8) and expose the knee (Type).
5. **`Limiter`** is correct as a lookahead-free ceiling. Add REL as a parameter (it is fixed at 80 ms), an optional 3 ms hold, and the soft safety stage (§C4). Report GR to the display as a trace, not only as a max.
6. **EQ model:** `EqSettings` is a fixed 4-band (HP/low shelf/peak/high shelf) layout. To look like **Parametric EQ 2**, store **7 bands**, each `{type, freq, gainDb, bwOct, enabled}`, run only enabled bands, and keep the existing `Biquad` (its RBJ formulas and `responseDb` are right). Model deltas:
   - `SynthPatch` += `osc2Shape, osc2Coarse, osc2FineCents, osc2Mix, modX, modY, modXEnv{on, amt, a, d, s, r}, lfo{target, shape, amt, speedSteps|hz, tempo}`
   - `CompSettings` += `knee/type, sidechain: ChannelKind?`
   - `ReverbSettings` → `decaySec, size, damp, preDelayMs, lowCutHz, highCutHz, dry, wet`
   - `DelaySettings` += `model, cutoffHz, dry`
   - `MasterSettings` += `limiterReleaseMs, eq (3 bands)`
   - `InsertTrack` += `fxSlots: List<PluginRef>` (order + bypass), `solo`, `mute`

---

## Not verified this pass [U]

- Exact **defaults** of Fruity Compressor, Fruity Limiter, Reeverb 2, Delay 3, Parametric EQ 2 (band types and frequencies) and 3x Osc. The manual pages give ranges but not init values. Check in desktop FL before writing "starting values match FL" anywhere.
- Mixer fader maximum (+5.6 dB is widely quoted) and FL's fader taper; the exact curve of FL's stereo pan.
- Numeric ranges of Reeverb 2's SIZE, DELAY, H.CUT and L.CUT, and of the Channel-settings ADSR knobs.
- FLEX edition availability (listed as an instrument; a user quote calls it free).
- BS.1770 K-weighting coefficients (from memory), and the Giannoulis et al. compressor paper (not fetched).
- CPU estimates (profile on the lowest-spec target), ART denormal behaviour, and phone-speaker bass roll-off.
- Ear-fatigue break guidance.

## Sources (fetched and read 26 Nov 2026)

**Image-Line (FL Studio online manual and site)**
- Fruity Parametric EQ 2: https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/plugins/Fruity%20Parametric%20EQ%202.htm (+ screenshot `img_plug/plugin_fx_FruityParametricEQ2.png`)
- Fruity Compressor: https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/plugins/Fruity%20Compressor.htm (+ `img_plug/Fruity_compressor.png`)
- Fruity Limiter: https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/plugins/Fruity%20Limiter.htm (+ `fruity_limiter_comp.png`, `fruity_limiter_limiter.png`)
- Fruity Reeverb 2: https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/plugins/Fruity%20Reeverb%202.htm (+ `reeverb2.png`)
- Fruity Delay 3: https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/plugins/Fruity%20Delay%203.htm (+ `Fruity_delay_3.png`)
- Fruity Balance: https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/plugins/Fruity%20Balance.htm (+ `Fruity_balance.png`)
- Fruity Soft Clipper: https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/plugins/Fruity%20Soft%20Clipper.htm
- Maximus: https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/plugins/Maximus.htm
- 3x Osc: https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/plugins/3x%20Osc.htm (+ `3xOsc.png`)
- FLEX: https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/plugins/FLEX.htm
- Mixer: https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/mixer.htm (+ `img_shot/mixer_main.png`)
- Levels, Mixing & Clipping: https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/mixer_levelsandmixing.htm
- Channel settings, Envelope/Instrument: https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/chansettings_ins.htm (+ `img_shot/chansettings_envinst.png`)
- Channel settings, Main: https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/chansettings_main.htm
- Compare editions: https://www.image-line.com/fl-studio/compare-editions

**Mixing education**
- Sound On Sound, "Compression Made Easy": https://www.soundonsound.com/techniques/compression-made-easy
- Sound On Sound, "Using EQ": https://www.soundonsound.com/techniques/using-eq
- iZotope, "Gain staging: what it is and how to do it": https://www.izotope.com/en/learn/gain-staging-what-it-is-and-how-to-do-it.html (redirects to izotope.com/community/blog/…)
- iZotope, "Principles of equalization": https://www.izotope.com/en/learn/principles-of-equalization.html (redirects as above)
- Spotify for Artists, "Loudness normalization": https://support.spotify.com/us/artists/article/loudness-normalization/

**DSP**
- W3C, *Audio EQ Cookbook* (R. Bristow-Johnson, ed. R. Toy): https://www.w3.org/TR/audio-eq-cookbook/
- J. O. Smith III, *Physical Audio Signal Processing*: Freeverb https://ccrma.stanford.edu/~jos/pasp/Freeverb.html (and the figure `img56.png`), Main loop https://ccrma.stanford.edu/~jos/pasp/Freeverb_Main_Loop.html, LBCF https://ccrma.stanford.edu/~jos/pasp/Lowpass_Feedback_Comb_Filter.html, Allpass https://ccrma.stanford.edu/~jos/pasp/Freeverb_Allpass_Approximation.html, Schroeder reverberators https://ccrma.stanford.edu/~jos/pasp/Schroeder_Reverberators.html
- A. Simper (Cytomic), *Linear Trapezoidal Integrated SVF*: https://cytomic.com/files/dsp/SvfLinearTrapOptimised2.pdf
- M. Finke, "Making Audio Plugins 18: PolyBLEP Oscillator": https://www.martin-finke.de/articles/audio-plugins-018-polyblep-oscillator/

Tried but not read: Sound On Sound "Q. What is pan law?" (HTTP 410); iZotope reverb and compression learn pages (404); the Giannoulis/Massberg/Reiss JAES 2012 PDF (unreachable).
