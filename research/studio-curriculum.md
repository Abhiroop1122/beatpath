# Studio curriculum research: how producers are taught, and a 47-lesson Studio path

**Research brief for Beatpath (plan.md §6–§8) · 26 November 2026**
Scope: what to teach in the Studio (Channel Rack + Piano roll), in what order, and how to auto-check it. Every lesson below is phone-sized (1–4 bars, 4/4), edit-based (no live-tap grading, per plan §3) and checked by rules the engine can evaluate.

Method note: the built-in WebSearch/WebFetch tools failed (model error). Pages were read with `curl` and stripped to text. Every source in the list at the end was actually fetched and read. Anything not confirmed from a fetched page is marked **[U]**.

---

## A. Findings

### A1. What order do the established courses teach in?

| Source (read) | Order of topics | What Beatpath should take from it |
|---|---|---|
| **Ableton Learning Music** [1–6] | Make beats (four on the floor → tempo & genre → backbeat → bars → rock vs house → song examples) → Notes & scales (explore pitch with **five notes A C D E G** → make patterns with pitch → keys & scales (major) → minor → chromatic) → Chords (major triads → minor triads → song examples → 1-5-6-4) → **Basslines** → **Melodies** → Song structure → Advanced (building scales, diatonic triads, inversions, voicings, 7ths, pentatonic, modes) | **Drums first, then pitch, then chords, then bass, then melody.** First pitch play uses a pentatonic set, so nothing sounds wrong. Every exercise has a **"Fill in solution"** button. The first screen is pre-made loop boxes to switch on and off, so sound comes before theory |
| **Image-Line "Learn FL Studio"** (13 videos by Synthet) [7–9] | Finding your way around → **Making a beat in Channel Rack** → **Creating melodies in Piano Roll** → layering loops → Loop Starter → arranging in the Playlist → generators → effects in the Mixer → automation → recording audio/MIDI → export → customizing | FL's own beginner path is **beat in the Channel Rack first, then the Piano roll**, then arrangement, then mixing. Same order as plan.md |
| **Hooktheory I** (book contents) [10] | Major scale → relative notation (scale degrees) → chords → **combining chords and melody** → I chord as home → chord function → vi (minor sound) → Melody: meter, **stable vs unstable scale degrees**, rhythmic patterns, melodic themes → iii, ii → inversions (I6, V6, cadential 6/4) | **Chords before melody**, and melody is taught *against* chords (stable = in the current chord). Inversions come last |
| **Hooktheory "Popular progressions"** [11] | Difficulty tiers: Beginner (I ii iii IV V vi) → Intermediate I (inverted chords) → Intermediate II (secondary dominants, inverted 7ths) → Advanced (borrowed chords) | A ready-made difficulty ladder for the harmony track |
| **musictheory.net lessons** [12] | Basics (notes, rests, measures, steps) → scales & keys → intervals → triads → inversions → 7th chords → diatonic chords → voicing → non-harmonic tones → phrases & cadences → common progressions | The classical order. Useful as a checklist, but it delays making music. Beatpath should follow Ableton's order and use this list for coverage |
| **Melodics** [13–15] | Lessons are "broken into steps … introduce concepts & techniques as you need them". Courses group lessons around one concept. A guided path per instrument. **Practice mode** loops a hard part, slows it and can wait for you. 15 **Stages** per instrument act as checkpoints that "test what you've learned – not to teach you something new". They unlock in order. Daily goal is 5 minutes | Matches plan's Solo nodes and Unit Jams. Keep teaching and testing separate: a level ends with a cue-free check, not new content |
| **Simply Piano** (marketing page only) [16] | "Step-by-step tutorials perfectly paced for beginners", real-time feedback, songs you love | The sequencing detail could not be verified **[U]** |

**Conclusion: drums first, then a short pitch block, then chords, then bass, then melody.** The order Kick → backbeat → hats → notes → triads → progression → bass → melody → arrangement is the consensus of Ableton, Image-Line and Hooktheory. Plan.md already follows it. The one change is **bass before melody** (Ableton and the NI/Audeobox guides). Plan.md defers bass to v1.1.

### A2. Drum patterns as they are actually taught (step numbers are 1–16 per bar)

| Pattern | Steps (from source) | Tempo | Source |
|---|---|---|---|
| Four on the floor | Kick 1, 5, 9, 13 | — | Ableton [2], Audeobox [17] |
| Backbeat | Clap/snare 5, 13 ("second and fourth beat") | — | Ableton [3] |
| Eighth-note hats | 1, 3, 5, 7, 9, 11, 13, 15 | — | Audeobox [17], NI [18] |
| House | 4-on-floor kick, clap 5 & 13, closed hats on eighths, **open hats on the eighths between beats** (3, 7, 11, 15) | 123 BPM (NI); house 115–130 (Ableton) | NI [18], Ableton [4] |
| Rock | Kick on beats 1 & 3 (1, 9), snare 2 & 4, eighth hats | 80 BPM (NI) | NI [18] |
| Hip-hop / boom bap | Eighth hats, snare 5 & 13, **kick on the 1st and 6th eighth (steps 1, 11)**, a quiet swung kick on the last 16th of beat 1 (step 4, vel ≈71) | 87 BPM (NI); 85–95, sweet spot 88–92 (Plugg) | NI [18], Plugg [21] |
| Beginner hip-hop variation | Start from four on the floor, remove kicks 5 & 13, add kick on step 11 "for syncopation" | 140 | Audeobox [17] |
| Trap | Eighth hats; **clap on beat 3 only (step 9)**, i.e. half-time; kicks 1 & 13 (bar 1), beat 2 (bar 2); hat **rolls of three 32nds** replacing one hat; roll middle notes a bit softer | 130 (NI); 130–160, 140 standard, half-time feels like 70 (Audeobox) | NI [18], Audeobox [19] |
| Trap rolls | Rolls at the end of each 2-bar phrase; velocity ramps 50→100 %; **"Rolling every bar dilutes impact — two to four rolls per eight bars"** | 140 | Audeobox [19], Plugg [22] |
| Ghost notes | Snare ghosts at **vel 25–55 against a 100–127 backbeat**, on the 16th **just before and just after** beats 2 & 4, **never on a kick step**. Hats: 8ths ≈90, in-between 16ths ≈50–60 | — | Plugg [20], NI funk (ghost snares ≈75) [18] |
| Velocity | "Accent strong beats, keep in-between hits 20 to 40 steps lower." The machine-gun sound comes from the same sample at the same velocity | — | Plugg [23] |
| Swing (FL) | FL Global Swing "holds odd 16th notes … at the expense of even 16th notes". It needs both Global and Channel swing > 0. Programming in double time breaks it | — | FL manual [24] |

Source quality note: Audeobox [17] labels step 4 as "the 'and' of beat 1". Step 4 is actually the last 16th of beat 1 ("a"); the "and" is step 3. Beatpath's copy must count "1 e & a" correctly (Hattie does this in plan §4.2). The Plugg articles [20–23] are commercial blog posts. Their numbers are consistent with NI and the FL manual, but they are not authoritative.

### A3. Bass and 808 as taught

- **Start with the chord roots**, then add rhythm, then octave jumps, then "get off the root" (7ths/5ths), then variation every 4 or 8 bars. NI teaches it in exactly this order: G1–D2–F1–G1 under Gm7–Dm7–Fm7–Gm7, then syncopation against a four-on-the-floor kick, then the last note of each chord up an octave [25].
- Lock the bass to the kick, or contrast it. Ableton's definition: basslines "reinforce a song's chords, while also using interesting rhythmic patterns that relate to or contrast with the drums" [5]. **Arpeggio basslines** outline a triad (Grimes "Oblivion" in Ableton [5]). "Ideas that repeat three times" and passing tones (Daft Punk) [5].
- Beginner bass rules (Audeobox): "Keep bass notes below C2" (their octave labels; FL's own labels differ, see below). "Simple, sustained notes are more effective than busy patterns." **One note per bar or per chord change is often enough** [17].
- 808: the kick is short and sits on 1 & 3; the 808 carries the low end. Set it mono. Glides come from overlapping notes (a 16th overlap = snappy, a beat = slow). **"An out-of-tune 808 kills a beat faster than anything else"** [19].
- Pitch-label caution: FL labels MIDI 60 as **C5** (plan §6, `Pitch.flName`). Many web tutorials use C4 = 60. Beatpath should always show FL labels and keep MIDI numbers internal. Bass range used below: **MIDI 36–52 = FL C3–E4**. 808 range: **MIDI 24–43 = FL C2–G3**.
- Phone speakers reproduce little sub-bass **[U: exact cutoff not verified]**. Bass lessons need a bass sound with upper harmonics, a "headphones recommended" chip, and a visual note display, so learners can still succeed.

### A4. Chords and progressions

- Triad = root + 3rd + 5th, stacked from the scale. Major sounds "happy"/"uplifting", minor "sad"/"dark" (Ableton, also the framing for Thea's droop) [6].
- Ableton calls 1-5-6-4 "the most common chord progression". OMT calls I–V–vi–IV the "Best-Seller" progression. **Its rotation vi–IV–I–V is the same as minor i–VI–III–VII**: "whether you hear this progression in major or minor depends on whether you hear the first chord as the tonic" [26]. This is a perfect advanced lesson: same chords, new home.
- **Harmonic rhythm** (how long each chord lasts) is a separate, teachable variable: one chord per bar vs two per bar [27]. Offbeat chords (reggae/house stabs) are a standard accompaniment texture [28].
- Inversions: first inversion has the 3rd lowest ("C/E"), second has the 5th lowest ("C/G"). **Voicing** = how the chord's notes are spread or doubled [29]. 7ths: Cmaj7 = C E G B, Cm7 = C E♭ G B♭, built by stacking another third [29].
- FL tools that match: **Chord stamp** (places a chord at a clicked pitch/time, follows snap and scale), **Snap to scale** (forces edited notes into the scale; FL won't move existing notes until you edit them), **scale highlighting**, **Ghost channels** (Alt+V) [30]. Beatpath should teach building chords by hand first (plan Unit 9), and present the stamp later as the "desktop shortcut".

### A5. Melody writing: teachable rules, with sources

| # | Rule | Source | Auto-check |
|---|---|---|---|
| M1 | Start with the pentatonic: the safe palette. Its "strongest notes are the tonic, third, and fifth" | NI [31]; Ableton's first pitch screen uses A C D E G [6] | `melodyInScale(lead, pentatonic)` |
| M2 | **Chord tones are stable, others are unstable.** Unstable notes on weak beats are mild; on strong beats they create tension that "often (but not always)" resolves to a stable tone | Hooktheory [32] | `strongBeatChordTones(lead, chords, beats {1,3}) ≥ r` |
| M3 | In 4/4, beats 1 & 3 are strong, 1 strongest | Hooktheory [33] | defines "strong beat" |
| M4 | Contrast **stepwise** motion with **leaps**: "This contrast between stepwise and leapwise motion helps to make the melody strong and memorable" | Ableton (Joy Division example) [5] | `stepwiseRatio ≥ 0.6` and `hasLeap(≥3)` |
| M5 | **Leaps larger than a 4th should change direction afterwards.** Consecutive leaps should outline a triad | Open Music Theory "Rules of Melody" [34] | `leapRecovery(minLeap = 6 semitones, opposite)` |
| M6 | Tendency tones: 7 goes to 8 (B→C), 4 goes to 3 (F→E); 6 and 2 resolve down by step | OMT [34] | soft rule: hint when B is not followed by C |
| M7 | **Motif**: the smallest identifiable idea, repeated through the section | Berklee (Stolpe) [35]; OMT [36] | `motifRepeat(bar1 → bar2)` |
| M8 | **Use more repetition**: "we vary the melody so much … the listener doesn't get a clear sense of what the melody is" | Berklee [35]; NI ("Popular melodies are simple, memorable, and repetitive") [31] | `rhythmSimilarity(bar1, bar3) ≥ 0.75` |
| M9 | **Repeat with a changed ending = question and answer.** Kraftwerk: phrases 1 & 2 identical except the last note; the first "sounds somehow incomplete", the second resolves. Ikonika: "only the last note differs" | Ableton [5] | `variation(sameRhythm, lastPitchDiffers)`; `endsOn(answer, tonic)`, `!endsOn(call, tonic)` |
| M10 | Variation techniques: inversion, interval change, augmentation/diminution, rhythmic change, **ornamentation with passing/neighbour tones**, extension, transposition/sequence | OMT [37]; Ableton (phrase 3 = phrase 1 transposed up) [5] | `diatonicTranspose(bar1 → bar2, +1)` |
| M11 | **Passing tones** pass between chord tones by step; **neighbour tones** step away and return | OMT [38, 39] | `passingTones(lead, chords) ≥ n` |
| M12 | Control **rhythm and space**, not only pitch: long vs short notes, "lots of rest space or very little", start on, before or after the downbeat | Berklee [35] | `hasRest(≥½ beat)`, density per bar |
| M13 | Trap melodies "breathe": **no more than 6–8 notes per bar**, leave gaps | Audeobox [19] | `densityPerBar ≤ 8` (≤ 6 in trap) |
| M14 | Phrases are usually 4 bars (8 common) and end with a sense of completion | OMT [40] | phrase lengths of 2 and 4 bars; `endsOn(tonic)` for closed phrases |
| M15 | Bass under melody: bass follows roots; the melody defines the song's identity and covers a wider range | Ableton [5] | `rootsFollow(bass, chords)` |

### A6. Beginner mistakes the sources warn about (turn each into a hint)

1. **Everything at one velocity** ("machine gun") → accents and ghosts [20, 23].
2. **Too many hat rolls** → 2–4 per 8 bars [22]. **Ghost notes on kick steps** → flam mush [20].
3. **Over-busy bass, bass too high** → long simple notes, one per chord [17]. **Untuned 808** [19].
4. **Too little repetition / over-varied melody** [35]. **Too many melody notes, no gaps** [19].
5. **Stacking several swing/groove sources** (FL swing + groove + humanize) "makes timing mushy" [21].
6. **Clutter**: "Simple and clean beats a complicated mess every time"; get the kick/snare relationship right before adding complexity [17].
7. **Melodies with all 12 notes** easily sound "wrong". Most music uses 7-note (or 5-note) subsets [6].

### A7. What makes interactive lessons easy for total beginners (synthesis)

- **Sound in the first second, from a pre-made context.** Ableton opens with loop boxes to toggle. Each grid exercise sits on top of other parts that already sound good [1–3]. In Beatpath, every melodic lesson ships with drums and ghost chords pre-filled, so the learner's 3 notes already sound like music.
- **One new idea per exercise, explained in one sentence, with the solution one tap away** (Ableton "Fill in solution"; Melodics "introduce concepts … as you need them") [2, 13]. Map to plan's hint ladder; the reveal is tagged "with help".
- **Hide what isn't needed.** Show only the channels and rows the lesson uses, and lock the rest (Ableton's grids show only 4 drum rows or a handful of pitches) [2, 6].
- **Practice mode: loop, slow down, wait** (Melodics) [13]. In Beatpath: loop the bar, halve the tempo on "Hear it slower", and solo the target channel.
- **Separate teaching from testing** (Melodics Stages) [14]. Each level ends with a cue-free Jam/Solo check that introduces nothing new.
- **Short daily sessions** (Melodics 5-minute daily goal) [13], consistent with plan's 3–7 minutes.
- **Real music, not nursery rhymes** (Melodics "no nursery rhymes here"; Ableton uses Daft Punk, Beyoncé, Kraftwerk) [5, 13]. Name genres (house, trap, boom bap) in lesson titles. Never imitate or quote a copyrighted melody note-for-note.
- **Accept more than one right answer** where the prompt allows it. Check only the named constraint (plan §8). Constraint-based composition checks (M1–M14) let a learner write their own melody and still get an objective pass.

### A8. Not verified this pass [U]

In The Mix, Busy Works Beats (blog returned only an embedded video), Andrew Huang, Point Blank and EDMProd (HTTP 403 / no text). Their specific lesson orders are **not** used. Simply Piano's internal sequencing. The exact sub-bass cutoff of phone speakers. Whether FL's Channel Rack paints steps by click-drag in every version (believed true). The exact FL menu path for "Clone pattern" in the current FL version.

---

## B. The Studio curriculum: 10 levels, 47 lessons

### B0. Conventions used in every lesson

- **Grid:** 480 ticks per beat, 120 per step (`Score.kt`). Steps are numbered **1–16 per bar**, as FL shows them. `b2:5` = bar 2, step 5. `b1.3` = bar 1, beat 3. `tick(b, s) = (b−1)·1920 + (s−1)·120`.
- **Pitch:** FL labels, **C5 = MIDI 60**. A pitch class (pc) is a note name in any octave.
- **Every lesson has two phases**, like the existing two lessons: **Guided** (glows/outlines, hint ladder) then **Fresh** (a new tempo, key or position with cues off). A lesson marked **Jam** has no guided phase. It is the level's cue-free checkpoint and can feed a Solo node.
- **"Every bar"** means the rule must hold in each bar of the loop.
- **Locked** = pre-filled notes the learner can hear but can't edit in this lesson. The check also asserts they are unchanged (`unchanged(ch)`).
- **Check rules** use the primitives in section C. A rule only checks what the prompt names (plan §8). Velocity and duration are ignored unless named.
- **Channels needed** beyond the current MVP set (Kick, Clap, Hat, Chords, Lead) are marked ★: **Snare★, Open Hat★, Bass★ (Lou), Lead 2★**. See the contradictions list in the summary.

**Path order** interleaves rhythm and pitch, so melody-curious learners hear pitch by lesson 6, and trap rolls (Level 5) come after the Piano roll is known (Level 2):

| Level | Name | Window | Lessons | Stage |
|---|---|---|---|---|
| 1 | First beat | Rack | 5 | Absolute beginner |
| 2 | First notes | Roll | 4 | Absolute beginner |
| 3 | Groove builder | Rack | 4 | Beginner |
| 4 | Chords | Roll | 5 | Beginner |
| 5 | Feel and dynamics | Rack (+ Roll for rolls) | 4 | Beginner+ |
| 6 | Bass and 808 | Roll | 5 | Intermediate |
| 7 | Melody I: lines | Roll | 5 | Intermediate |
| 8 | Melody II: motifs and phrases | Roll | 5 | Intermediate+ |
| 9 | Harmony II: inversions, 7ths, minor | Roll | 5 | Advanced |
| 10 | Make it a track | Roll + Rack + Patterns | 5 | Advanced |

---

### Level 1 · First beat (Absolute beginner · Channel Rack · 1 bar)

**1.1 `rack.four-on-floor` · Four on the floor** *(exists)*
- Rack · 1 bar · 90 BPM (Fresh 120) · Key — · Shown: Kick. Setup: empty, metronome on.
- **New idea:** a beat is a steady pulse; four beats make a bar.
- Steps: tap glowing step 1 → playback starts and loops → glows move to 5, 9, 13.
- **Check:** `stepSetEquals(Kick, every bar, {1,5,9,13})`.
- Hint: "Every beat gets a kick. Beats start on the first pad of each group of four."
- FL tip: F6 opens the Channel Rack; left-click a step on, right-click it off; Space plays.

**1.2 `rack.backbeat` · Clap on 2 and 4**
- Rack · 1 bar · 95 BPM (Fresh 124) · Shown: Kick (locked {1,5,9,13}), Clap.
- **New idea:** the backbeat. Beats 2 and 4 get the clap.
- Steps: hear the kick loop → tap Clap on beat 2 (step 5) → tap beat 4 (step 13).
- **Check:** `stepSetEquals(Clap, every bar, {5,13})` ∧ `unchanged(Kick)`.
- Hint: "Count 1-2-3-4 with Kiko. The clap answers on 2 and 4."
- FL tip: the same pattern in FL is the default house/techno starting point; clone the channel with Alt+C to try another clap.

**1.3 `rack.eighth-hats` · Hats in between**
- Rack · 1 bar · 95 BPM (Fresh 110) · Shown: Kick + Clap (locked from 1.2), Hat.
- **New idea:** eighth notes. Two hats per beat: the beat and the "and".
- Steps: tap step 1 and step 3 (Hattie counts "1 &") → finish the bar → optional **Fill each 2** button.
- **Check:** `stepSetEquals(Hat, every bar, {1,3,5,7,9,11,13,15})`.
- Hint: "Every other pad. Skip one, tap one."
- FL tip: right-click a channel name → **Fill each 2 steps** does this in one click.

**1.4 `rack.offbeat-hat` · House hats on the "and"**
- Rack · 1 bar · 124 BPM (Fresh 120) · Shown: Kick {1,5,9,13} and Clap {5,13} locked; Hat pre-filled with eighths.
- **New idea:** the offbeat. Remove the hats that land with the kick, so they hit only *between* beats.
- Steps: hear eighths vs offbeat-only (A/B) → delete hats on 1, 5, 9, 13.
- **Check:** `stepSetEquals(Hat, every bar, {3,7,11,15})`.
- Hint: "Keep only the hats that fall *between* kicks: the 'and' of each beat."
- FL tip: house usually sits at 115–130 BPM. Drag FL's tempo display to try 124.

**1.5 `rack.jam-rock` · Jam: beat from words**
- Rack · 1 bar · 100 BPM (Fresh: 90) · Shown: Kick, Clap, Hat, all empty. **Jam (no glows).**
- **New idea:** none. Build a described beat: "kick on 1 and 3, clap on 2 and 4, eighth-note hats".
- **Check:** `stepSetEquals(Kick,{1,9})` ∧ `stepSetEquals(Clap,{5,13})` ∧ `stepSetEquals(Hat,{1,3,5,7,9,11,13,15})`.
- Hint (ladder): replay the target → solo the wrong channel and show its beat numbers → reveal.
- FL tip: in FL the same beat is 3 channels; Desktop mission "Build it in FL and right-click a step off".

### Level 2 · First notes (Absolute beginner · Piano roll · Lead · C major)

**2.1 `roll.first-notes` · Draw four notes**
- Roll · 1 bar · 90 BPM · Key C · Snap Beat · Shown: Lead rows C5–C6; drums from 1.5 playing, locked.
- **New idea:** Piano roll: left = earlier, up = higher; one tap places and plays a note.
- Steps: tap C5 at beat 1 → D5 at beat 2 → E5 at beat 3 → G5 at beat 4 (outlines).
- **Check:** `notesExactly(Lead, [(C5,b1.1),(D5,b1.2),(E5,b1.3),(G5,b1.4)], dims={pitch,onset})`. Fresh: the same notes walking down (G5 E5 D5 C5).
- Hint: "Rows are pitches. Tink sits higher on higher rows."
- FL tip: F7 opens a channel's Piano roll; Draw tool is P; right-click deletes on desktop.

**2.2 `roll.long-short-rest` · Long, short and silence**
- Roll · 2 bars · 90 BPM · Snap ½ beat · Shown: one row (C5) only. Setup: reference rhythm plays on "Hear example".
- **New idea:** note length and rests. A rest is a choice.
- Target: b1.1 2 beats · b1.3 1 beat · (b1.4 silent) · b2.1 ½ · b2.1& ½ · b2.2 1 beat · b2.3 2 beats.
- Steps: place each start → drag the right edge to set length → leave beat 4 empty.
- **Check:** `rhythmMatches(Lead, ref, onsetTol=0, durTol=60)` ∧ `silentSpan(Lead, b1.4, 480)`. Pitch ignored.
- Hint: "Your second note is right. Shorten it so beat 4 is silent." (plan §5 rests example).
- FL tip: drag a note's right edge to resize; right-click the Snap (magnet) to set the default note length.

**2.3 `roll.c-major-scale` · Build the C major scale**
- Roll · 1 bar · 90 BPM · Snap ½ beat · Shown: C5–C6, **scale highlighting off**.
- **New idea:** a scale is a recipe of whole and half steps: W W H W W W H.
- Steps: C5 on 1 → step up W, W, H … (Kiko shows "W" or "H" above each gap) → C6 on beat 4&.
- **Check:** `pitchSequence(Lead) == [60,62,64,65,67,69,71,72]` ∧ `onsetsStrictlyIncreasing`. Fresh: the scale descending.
- Hint: "E→F and B→C have no black key between them: that's a half step."
- FL tip: Piano roll → Snap to scale → Major (ionian), root C turns on FL's scale highlighting.

**2.4 `roll.pentatonic` · Five safe notes (Jam)**
- Roll · 2 bars · 90 BPM · Snap ½ beat · Drums locked · Rows highlighted: **C D E G A** (C major pentatonic). **Jam.**
- **New idea:** a pentatonic scale has no notes that clash. Make anything, then end at home.
- **Check:** `noteCount(Lead) ∈ [5,10]` ∧ `melodyInScale(Lead, C, pentatonicMajor, 1.0)` ∧ `endsOn(Lead, pc=C)`.
- Hint: "Any highlighted note works. Finish on a C and it sounds finished."
- FL tip: turn off FL's scale snap, and play black keys only from E♭: that's the minor pentatonic (Ableton's trick).

### Level 3 · Groove builder (Beginner · Channel Rack)

**3.1 `rack.sixteenth-hats` · Sixteenths: "1 e & a"**
- Rack · 1 bar · 90 BPM · Kick {1,9}, Clap {5,13} locked; Hat empty.
- **New idea:** a step is a sixteenth: four per beat.
- Steps: Hattie counts "1 e & a"; fill beat 1 (steps 1–4) → fill the bar (drag across pads).
- **Check:** `stepSetEquals(Hat, every bar, {1..16})`.
- Hint: "One pad per syllable: 1-e-&-a, 2-e-&-a …"
- FL tip: FL's step = 1/16 in 4/4. **[U]** click-drag across steps paints them.

**3.2 `rack.hiphop-kick` · Syncopated kick (hip-hop)**
- Rack · 1 bar · 90 BPM (Fresh 87) · Clap {5,13} and Hat eighths locked; Kick empty.
- **New idea:** syncopation: a kick *between* beats pushes the groove.
- Guided: Kick on 1 and 11 (glows; "1 … and-of-3"). Fresh: "make your own kick".
- **Check (Guided):** `stepSetEquals(Kick, {1,11})`. **Check (Fresh):** `1 ∈ Kick` ∧ `count(Kick \ {1,5,9,13}) ≥ 1` ∧ `Kick ∩ {5,13} = ∅` ∧ `stepCount(Kick) ∈ [2,5]`.
- Hint: "Leave beats 2 and 4 to the clap. Put one kick on a pad that isn't a beat."
- FL tip: this is the classic hip-hop/2-step kick (1st and 6th eighth notes).

**3.3 `rack.open-hat` · Open hat on the "and"** (Open Hat★)
- Rack · 1 bar · 124 BPM · Kick 4-on-floor, Clap {5,13} locked; Hat pre-filled eighths; Open Hat empty.
- **New idea:** an open hat is a long "tss" that the closed hat cuts off.
- Steps: add Open Hat on 3, 7, 11, 15 → remove closed hats from those steps.
- **Check:** `stepSetEquals(OpenHat, {3,7,11,15})` ∧ `disjoint(Hat, OpenHat)` ∧ `stepSetContains(Hat, {1,5,9,13})`.
- Hint: "A real drummer can't play open and closed at once. Remove the closed hat where the open one plays."
- FL tip: Channel settings → Misc → **Cut / Cut by** makes the closed hat choke the open hat.

**3.4 `rack.jam-two-bars` · Jam: two bars and a fill** (Snare★)
- Rack · 2 bars · 95 BPM · bar 1 pre-filled (Kick {1,11}, Snare {5,13}, Hat eighths), bar 2 empty. **Jam.**
- **New idea:** a 2-bar loop: repeat, then change the end so the loop "turns around".
- Steps: Duplicate bar 1 → change 1–3 kick steps in bar 2 → add a snare fill in bar 2's last beat.
- **Check:** `unchanged(bar 1)` ∧ `barDiffSize(Kick, 1, 2) ∈ [1,3]` ∧ `5 ∈ Snare@b2` ∧ `|Snare@b2 ∩ {13,14,15,16}| ≥ 3` ∧ `barEquals(Hat, 1, 2)`.
- Hint: "Bar 2 copies bar 1, then breaks the pattern only at the very end."
- FL tip: Patterns can be any length; set the Channel Rack's pattern length to 32 steps (2 bars).

### Level 4 · Chords (Beginner · Piano roll · Chords · C major)

**4.1 `roll.build-c-major` · Build C major** *(exists)*
- **Check:** `chordAt(Chords, b1.1, {C,E,G}, simultaneous)`; Fresh at b1.3.

**4.2 `roll.minor-droop` · Thea droops: C minor**
- Roll · 1 bar · 96 BPM · C major (C5 E5 G5, whole note) pre-filled.
- **New idea:** lower the 3rd by one semitone: major → minor.
- Steps: hear major → select E5 (Select E) → move it down a semitone → hear minor.
- **Check:** `chordAt(Chords, b1.1, {C,E♭,G})`. Fresh: build **A minor** on b1.3 from scratch → `chordAt(b1.3, {A,C,E}, allowInversion)`.
- Hint: "Only Thea (the middle bird) moves: one row down."
- FL tip: Shift+↓ moves a selection one semitone.

**4.3 `roll.two-chords` · Two chords, two bars**
- Roll · 2 bars · 96 BPM · bar 1 empty. Drums locked.
- **New idea:** same shape, new place: move a whole chord.
- Steps: build C (whole note) → **Duplicate** to bar 2 → move it up 5 semitones (C→F, E→A, G→C).
- **Check:** `chordAt(b1.1,{C,E,G})` ∧ `chordAt(b2.1,{F,A,C}, allowInversion)` ∧ `holds(Chords, b1.1, ≥1440)` ∧ `holds(b2.1, ≥1440)`.
- Hint: "Select all three notes and move them together."
- FL tip: in FL's Piano roll, Ctrl+B duplicates the selection to the right.

**4.4 `roll.four-chord-loop` · The four-chord loop (I–V–vi–IV)**
- Roll · 4 bars · 96 BPM · Key C · Drums from 1.5 locked · C in bar 1 pre-filled.
- **New idea:** a progression: C – G – Am – F. Hear which chord feels like home.
- Steps: G in bar 2 (same shape as C, up 7) → Am in bar 3 (minor) → F in bar 4.
- **Check:** `chordAtBars(Chords, [{C,E,G},{G,B,D},{A,C,E},{F,A,C}], allowInversion, octaveFree)`; staggered starts fail. Fresh: swap bar 4 to **Dm** `{D,F,A}`.
- Hint: "G is C's shape moved up. Am is minor: the middle note sits lower."
- FL tip: Clone the pattern to keep version A while you try B (plan Unit 10).

**4.5 `roll.chord-stabs` · Offbeat stabs**
- Roll · 2 bars · 122 BPM · Am (bar 1), F (bar 2) given as one long ghost each; Chords empty.
- **New idea:** chord rhythm: re-strike short chords on the "and", like house stabs.
- Steps: build a short Am on step 3 → Duplicate to 7, 11, 15 → same for F in bar 2.
- **Check:** `every bar: onsets(Chords) == {3,7,11,15}` ∧ `every onset group: pcs == chord(bar)` ∧ `maxDuration(Chords) ≤ 240`.
- Hint: "Short and off the beat, exactly where 1.4's open hats were."
- FL tip: Paint tool (B) drags repeated notes; set Snap to "Step" for stabs.

### Level 5 · Feel and dynamics (Beginner+ · Channel Rack, Piano roll for rolls)

**5.1 `rack.hat-accents` · Loud and soft hats**
- Rack · 1 bar · 92 BPM · 16th hats pre-filled at vel 100; Kick/Snare locked.
- **New idea:** velocity: how hard a hit is. Accents make a groove.
- Steps: long-press a hat → set Soft → set every in-between 16th (even steps) Soft.
- **Check:** `stepSetEquals(Hat,{1..16})` ∧ `min(vel(Hat, odd steps)) − max(vel(Hat, even steps)) ≥ 25`.
- Hint: "Loud on 1-&-2-&, soft on the e's and a's."
- FL tip: open the Graph editor in the Channel Rack (manual: Ctrl+K) or the Piano roll velocity lane; Alt+mouse-wheel changes a note's velocity.

**5.2 `rack.ghost-snare` · Ghost notes (boom bap)**
- Rack · 1 bar · 90 BPM · Kick {1,8,11}, Snare {5,13} vel 110, Hat eighths, locked except Snare.
- **New idea:** ghost notes: very quiet snares that you feel more than hear.
- Steps: add a snare on step 4 → set it Soft → add one more next to beat 4.
- **Check:** `loud(Snare, vel≥90) == {5,13}` ∧ `ghosts = Snare where vel ≤ 55; |ghosts| ≥ 2` ∧ `ghosts ∩ Kick = ∅` ∧ `ghosts ∩ {4,6,12,14} ≠ ∅`.
- Hint: "Ghosts live right before or after the backbeat, and never on a kick."
- FL tip: in FL, ghost a step by lowering its velocity in the Graph editor; keep the backbeat at 100+.

**5.3 `rack.swing` · Swing it**
- Rack · 1 bar · 90 BPM · 16th hats (accented from 5.1) locked; Swing knob 0 %.
- **New idea:** swing delays every second 16th: straight → bouncy.
- Steps: A/B straight vs 50 % → set swing yourself until it bounces.
- **Check:** `swingAmount ∈ [20%, 70%]` ∧ `count(Hat ∩ evenSteps) ≥ 4`.
- Hint: "Swing only moves the 'e' and 'a' pads. If none are on, you won't hear it."
- FL tip: FL's Swing knob is in the Channel Rack; it only affects even steps, and a channel's own Swing setting must be above 0 %.

**5.4 `roll.trap-roll` · Jam: trap half-time and a hat roll**
- Rack + Hat Piano roll · 2 bars · 140 BPM · Key — · Hat eighths and Kick {1,13}|{5} pre-filled; Clap empty. **Jam.** Snap: **½ step (32nd)** and **triplet** available on the Hat roll.
- **New idea:** half-time: clap only on beat 3. Rolls: bursts of 32nds before a hit.
- Steps: Clap on step 9 in both bars → open Hat's Piano roll → in bar 2 beat 4, replace one hat with 3–6 notes ½-step apart → ramp velocity up (optional).
- **Check:** `stepSetEquals(Clap, every bar, {9})` ∧ `rollExists(Hat, minNotes=3, spacing ∈ {60,80}, within=480)` ∧ `rollsPerBar(Hat) ≤ 1`. Bonus (not required): `velocityRamp(roll, nondecreasing)`.
- Hint: "One roll per phrase sounds like a drummer showing off. Every beat sounds like a broken record."
- FL tip: in FL's Piano roll set Snap to 1/2 step for 32nds; draw a rising velocity line in the event editor.

### Level 6 · Bass and 808 (Intermediate · Piano roll · Bass★ "Lou")

All lessons ghost the Chords channel and keep the drums locked. A "headphones recommended" chip shows.

**6.1 `roll.bass-roots` · Roots under the chords**
- Roll · 4 bars · 96 BPM · Key C · Chords C–G–Am–F (from 4.4) ghosted; Bass empty; rows C3–E4.
- **New idea:** the bass usually plays the chord's root, the note the chord is named after.
- Steps: hold a C under bar 1 → G, A, F under bars 2–4 (whole notes).
- **Check:** `rootsFollow(Bass, Chords, at=bar start, octaveFree)` ∧ `notesInRange(Bass, 36, 52)` ∧ `every bar: noteCount == 1 ∧ duration ≥ 1440`.
- Hint: "Lou sings Ro's note (the lowest bird), just much lower."
- FL tip: in FL, load a bass instrument (e.g. BooBass), then F7. Keep it simple: one note per chord.

**6.2 `roll.bass-lock-kick` · Lock to the kick**
- Roll · 4 bars · 96 BPM · Kick {1,8,11} every bar (ghosted from the Rack) · roots from 6.1 pre-filled.
- **New idea:** bass and kick hit together and sound like one big drum.
- Steps: Kiko flashes the kick on the roll → cut each bar's long root into notes that start with every kick.
- **Check:** `every bar: onsets(Bass) == onsets(Kick)` ∧ `every Bass note: pc == root(bar)` ∧ `monophonic(Bass)`.
- Hint: "Each kick gets a bass note. Same pitch all bar."
- FL tip: open Ghost channels (Alt+V) to see the kick's notes behind the bass.

**6.3 `roll.octave-bass` · Octave bounce (disco/house)**
- Roll · 2 bars · 120 BPM · Chords Am | F ghosted; Bass empty; Snap ½ beat.
- **New idea:** jump between the root and the same note an octave up.
- Steps: low A on beat 1 → high A on the "and" → repeat through the bar (Duplicate) → same for F.
- **Check:** `every bar: onsets(Bass) == {1,3,5,7,9,11,13,15}` ∧ `pcs == root(bar)` ∧ `octaveAlternation(Bass, low,+12)`.
- Hint: "Low, high, low, high: Lou bounces."
- FL tip: Ctrl+↑ moves the selected notes up an octave in FL.

**6.4 `roll.808-long` · 808: long notes in C minor**
- Roll · 2 bars · 140 BPM · Key C minor · Kick {1,11}|{1,8} and half-time clap locked; 808 rows C2–G3.
- **New idea:** an 808 is a long, tuned bass-drum. Start each note on a kick and let it ring.
- Steps: C on bar 1's kick → hold it 2+ beats → add 1–3 more notes on later kicks from the highlighted C-minor rows.
- **Check:** `onsets(Bass) ⊆ onsets(Kick)` ∧ `melodyInScale(Bass, C, naturalMinor)` ∧ `firstNote(Bass).pc == C` ∧ `count(Bass, dur ≥ 960) ≥ 1` ∧ `notesInRange(Bass, 24, 43)` ∧ `monophonic(Bass)`.
- Hint: "Tuned to the key, or the whole beat sounds off. Stay on the highlighted rows."
- FL tip: set the 808 channel to Mono. Overlapping two notes makes a glide in FL (a 16th overlap is snappy, a beat is slow).

**6.5 `roll.bass-approach` · Jam: walk into the next chord**
- Roll · 4 bars · 96 BPM · Key C · roots from 6.1 shortened to 3 beats, locked on beat 1. **Jam.**
- **New idea:** an approach note: a step above or below the next root on beat 4.
- **Check:** `for b in 1..3: exists Bass note at b.4 with |pitch − nextRoot(b+1)| ∈ {1,2} ∧ pc ∈ C major` ∧ `unchanged(roots)`.
- Hint: "Before G, try F or A. Before A, try G or B."
- FL tip: in FL, this is just one short extra note per bar; Duplicate bars, then edit only the last note.

### Level 7 · Melody I: lines (Intermediate · Piano roll · Lead "Tink")

Backing in every lesson: the drums and C–G–Am–F from 4.4 (ghosted and audible), Bass roots from 6.1.

**7.1 `roll.chord-tone-on-one` · A chord tone on every 1**
- Roll · 4 bars · 96 BPM · Key C · Ghost: Chords on · Lead empty.
- **New idea:** chord tones (notes inside the current chord) always fit.
- Steps: pick one ghost-outlined note per bar → place it on beat 1, any length.
- **Check:** `every bar: ∃ note at b.1 ∧ pc ∈ chordPcs(bar)` ∧ `notesInRange(Lead, 60, 84)`.
- Hint: "The outlined rows under your finger are Ro, Thea and Fi of that bar."
- FL tip: Piano roll → View → Ghost channels (Alt+V) shows the chords behind the lead.

**7.2 `roll.steps-and-leaps` · Step, step, leap (and back)**
- Roll · 2 bars · 96 BPM · Key C · Snap ½ beat.
- **New idea:** stepwise motion is smooth; a leap is exciting; after a big leap, turn back.
- Steps: hear a stepwise line vs one with a leap → write 6–10 notes with at least one leap → after the leap, move the other way.
- **Check:** `noteCount ∈ [6,10]` ∧ `melodyInScale(C major)` ∧ `distinctPitches ≥ 3` ∧ `stepwiseRatio ≥ 0.6` ∧ `hasLeap(≥3)` ∧ `leapRecovery(minLeap=6, opposite)`.
- Hint: "You jumped up a 5th. Step *down* next and it sounds like a singer, not a robot."
- FL tip: in FL, scale highlighting shows which rows are a step away (the next highlighted row).

**7.3 `roll.contour-arch` · Shape: the arch**
- Roll · 2 bars · 96 BPM · Key C.
- **New idea:** contour: rise to a peak and come back down.
- Steps: start low → climb to one highest note near the middle → come back down. Fresh: a valley (lowest note in the middle).
- **Check:** `noteCount ∈ [6,10]` ∧ `contourPeak(Lead, position ∈ [25%,75%] of notes, unique)` ∧ `first, last ≤ peak − 3` ∧ `rangeSpan ≤ 12` ∧ `melodyInScale(C major)`.
- Hint: "Make one note the top of the hill, and only one."
- FL tip: nothing new in FL. Zoom out (Ctrl+scroll) to see the shape of the whole line.

**7.4 `roll.rhythm-first` · Rhythm first, then pitch**
- Roll · 2 bars · 96 BPM · Key C · Setup: 7 grey note blocks with the rhythm locked (b1.1 1 beat · b1.2 ½ · b1.2& ½ · b1.3 1 · rest · b2.1 ½ · b2.1& ½ · b2.2 2 beats) on C5. Blocks move **only vertically**.
- **New idea:** melody = rhythm + pitch. Keep a good rhythm and change only the notes.
- Steps: drag each block to a pitch → hear it → make the first note of each bar a chord tone.
- **Check:** `rhythmMatches(Lead, ref)` ∧ `melodyInScale(C major)` ∧ `distinctPitches ≥ 3` ∧ `every bar: noteAt(b.1).pc ∈ chordPcs(bar)`.
- Hint: "Rhythm is locked, so pitch is your only choice. Try a step, then a leap."
- FL tip: in FL, hold Shift while dragging to move vertically without changing timing **[U: modifier varies by version]**. Otherwise use Shift+↑/↓.

**7.5 `roll.land-home` · Jam: land at home**
- Roll · 2 bars · 96 BPM · Key C · Chords: G (bar 1) → C (bar 2), ghosted. **Jam.**
- **New idea:** a phrase ends by landing on the tonic over the home chord.
- **Check:** `noteCount ∈ [4,10]` ∧ `melodyInScale(C major)` ∧ `stepwiseRatio ≥ 0.5` ∧ `endsOn(Lead, pc=C, onset ∈ {b2.1, b2.3}, dur ≥ 480)` ∧ `hasRest(Lead, ≥240)`.
- Hint: "B wants to go up to C. Try B → C across the bar line." (tendency tone M6)
- FL tip: in FL, end the phrase with a longer note and an empty half-bar so the loop breathes.

### Level 8 · Melody II: motifs and phrases (Intermediate+ · Piano roll · Lead)

**8.1 `roll.motif-repeat` · Make a motif, repeat it**
- Roll · 2 bars · 96 BPM · Key C · backing C–G (bars 1–2).
- **New idea:** a motif is a short idea you can recognize; repeating it makes it catchy.
- Steps: write 3–5 notes in bar 1 with a gap → Duplicate into bar 2.
- **Check:** `noteCount(bar 1) ∈ [3,5]` ∧ `hasRest(bar 1, ≥240)` ∧ `motifRepeat(Lead, bar1 → bar2, exact)` ∧ `melodyInScale(C major)`.
- Hint: "Don't change it yet. Repetition is the point."
- FL tip: in FL, select bar 1 (E tool) then Ctrl+B.

**8.2 `roll.same-rhythm-new-ending` · Same rhythm, different ending** *(plan sample D)*
- As specified in plan §7 D (C5, E5, D5 on beats 1–3; bar 2 a duplicate).
- **Check:** `rhythmMatches(bar2, bar1)` ∧ `pitches(bar2)[0..n−2] == pitches(bar1)[0..n−2]` ∧ `last(bar2).pitch ≠ last(bar1).pitch`.

**8.3 `roll.question-answer` · Question and answer**
- Roll · 4 bars · 96 BPM · Key C · Chords **C | G | F | C** (I–V–IV–I) ghosted.
- **New idea:** call and response: the question stops somewhere unfinished; the answer starts the same way and lands home.
- Steps: write bars 1–2, ending on D, G or B over the G chord → copy bar 1's rhythm into bar 3 → end bar 4 on C.
- **Check:** `rhythmSimilarity(bar1, bar3) ≥ 0.75` ∧ `last(bars1–2).pc ≠ C` ∧ `endsOn(Lead, pc=C, bar=4)` ∧ `restBetween(bars1–2, bars3–4, ≥240)` ∧ `melodyInScale(C major)`.
- Hint: "Your question already ends on C, so it sounds answered. End it on D and let bar 4 bring it home."
- FL tip: in FL, Clone the pattern to keep the question/answer version and try a second answer.

**8.4 `roll.sequence` · Move the motif up a step**
- Roll · 2 bars · 96 BPM · Key C · bar 1 motif locked (C5 D5 E5, rhythm ♩ ♪♪ ♩).
- **New idea:** a sequence: the same motif, one scale step higher.
- Steps: Duplicate bar 1 → move it up to the next highlighted row (D–E–F).
- **Check:** `rhythmMatches(bar2, bar1)` ∧ `diatonicTranspose(Lead, bar1 → bar2, +1 degree, key C)`. Fresh: +2 degrees (E–F–G).
- Hint: "Use scale steps, not semitones: E goes to F, not F♯."
- FL tip: with Snap to scale on in FL, moved notes snap to scale notes.

**8.5 `roll.jam-four-bar-melody` · Jam: four-bar melody over the loop**
- Roll · 4 bars · 96 BPM · Key C · Chords C–G–Am–F, drums, bass. **Jam.** Ghost on, scale highlight off.
- **New idea:** combine everything: chord tones on strong beats, passing notes in between, space, home ending.
- **Check:** `strongBeatChordTones(Lead, Chords, beats {1,3}) ≥ 0.85` ∧ `nonChordTonesOnWeak(Lead) ≥ 1` ∧ `densityPerBar ≤ 8` ∧ `hasRest(≥240) in bars 1–2 and in bars 3–4` ∧ `rhythmSimilarity(bar1, bar3) ≥ 0.6` ∧ `endsOn(pc=C)`.
- Hint: shows a per-rule checklist (✔/✗) and auditions the failing bar.
- FL tip: export this as MIDI and import it into FL (plan §10).

### Level 9 · Harmony II: inversions, 7ths, minor (Advanced)

**9.1 `roll.inversions` · Flip the chord**
- Roll · 1 bar · 90 BPM · Key C · Chords empty.
- **New idea:** an inversion puts a different chord note at the bottom: same chord, new colour.
- Steps: C–E–G on beat 1 → move C up an octave for beat 2 (E–G–C) → move E up for beat 3 (G–C–E).
- **Check:** `chordAt(b1.1,{C,E,G}) ∧ bassPc(b1.1)==C` ∧ `chordAt(b1.2) ∧ bassPc==E` ∧ `chordAt(b1.3) ∧ bassPc==G`.
- Hint: "Lift the bottom note one octave. Ro flies over the others."
- FL tip: in FL, select the lowest note and press Ctrl+↑ (one octave).

**9.2 `roll.voice-leading` · Smooth moves (voice leading)**
- Roll · 4 bars · 96 BPM · C–G–Am–F pre-filled in **root position** (jumpy).
- **New idea:** voice leading: move each chord note to the nearest note of the next chord.
- Steps: hear root-position vs smooth → re-voice G as B–D–G → Am as C–E–A → F as C–F–A.
- **Check:** `chordAtBars(pcs as 4.4, allowInversion)` ∧ `noteCountAt(each bar.1) == 3` ∧ `voiceLeadingCost(bar b → b+1) ≤ 5 for each change` ∧ `Σ cost ≤ 10` (root position scores 21+).
- Hint: "Keep shared notes still (C stays C from C to Am). Move the others by one or two rows."
- FL tip: in FL, the same voicing trick works with the Chord stamp: stamp, then move single notes by octave.

**9.3 `roll.sevenths-ii-v-i` · Seventh chords: ii–V–I (lo-fi)**
- Roll · 4 bars · 82 BPM · Key C · drums with swing from 5.3.
- **New idea:** add a fourth note (another 3rd up) for a jazzy 7th chord: Dm7 → G7 → Cmaj7.
- **Check:** `chordAt(b1.1,{D,F,A,C})` ∧ `chordAt(b2.1,{G,B,D,F})` ∧ `chordAt(b3.1,{C,E,G,B})` ∧ `chordAt(b4.1, {C,E,G,B} or {C,E,G})`, all `allowInversion`.
- Hint: "Stack one more skip-a-note on top: D-F-A, then C."
- FL tip: in FL, Chord stamp → min7, 7, maj7. Compare with what you built.

**9.4 `roll.relative-minor` · Same chords, new home: A minor**
- Roll · 4 bars · 90 BPM · Key A minor · Chords empty; Bass empty.
- **New idea:** start the loop on Am (Am–F–C–G = i–VI–III–VII) and **A becomes home**: the same chords as vi–IV–I–V.
- Steps: build the 4 chords → add bass roots (A, F, C, G).
- **Check:** `chordAtBars(Chords, [{A,C,E},{F,A,C},{C,E,G},{G,B,D}], allowInversion)` ∧ `rootsFollow(Bass, Chords)`.
- Hint: "These are the 4.4 chords in a new order. Which one feels like home now?"
- FL tip: in FL's scale menu, A minor (aeolian) highlights the same white keys as C major.

**9.5 `roll.dark-trap-melody` · Jam: dark melody in C minor**
- Roll · 2 bars · 140 BPM · Key C minor (black keys) · Chords Cm | A♭ ghosted · trap drums from 5.4 · 808 from 6.4. **Jam**, scale highlight off.
- **New idea:** a minor key with black keys; trap melodies breathe.
- **Check:** `melodyInScale(Lead, C, naturalMinor)` ∧ `densityPerBar ≤ 6` ∧ `hasRest(≥480)` ∧ `strongBeatChordTones ≥ 0.75` ∧ `endsOn(pc ∈ {C, G})`.
- Hint: "E♭ not E, A♭ not A, B♭ not B. If it sounds sunny, a 'natural' crept in."
- FL tip: in FL, Snap to scale → Minor, root C; high-pass the melody so it stays out of the 808's way.

### Level 10 · Make it a track (Advanced)

**10.1 `roll.passing-tones` · Passing tones**
- Roll · 2 bars · 96 BPM · Key C · Chords C | G · Lead pre-filled with chord tones on every beat (C5 E5 G5 E5 | D5 B4 D5 G5), locked onsets.
- **New idea:** a passing tone fills a gap of a third with the note in between, off the beat.
- Steps: hear C→E → add D on the "and" between them → find one more gap.
- **Check:** `unchanged(original beat notes)` ∧ `passingTones(Lead, Chords) ≥ 2` (a note on a weak position, not a chord tone, approached and left by step in the same direction, between two chord tones).
- Hint: "Only gaps of 3–4 semitones have a note in between."
- FL tip: in FL, set Snap to ½ beat before adding in-between notes.

**10.2 `roll.counter-melody` · Counter-melody** (Lead 2★)
- Roll · 4 bars · 96 BPM · Key C · Lead locked (a call-and-response melody that rests in the second half of bars 2 and 4) · Lead 2 empty, its rows below the Lead.
- **New idea:** a second line answers in the gaps, lower than the lead.
- Steps: watch the lead's gaps glow → write 4+ notes for Lead 2, mostly in those gaps.
- **Check:** `fillsGaps(Lead2, Lead) ≥ 0.7` (share of Lead 2 onsets where the Lead is resting or holding) ∧ `registerBelow(Lead2, Lead, median ≥ 3 semitones)` ∧ `melodyInScale(C major)` ∧ `noteCount(Lead2) ≥ 4`.
- Hint: "Talk when the lead is quiet. Don't talk over it."
- FL tip: in FL, add a second instrument channel and turn on Ghost channels to see the lead.

**10.3 `roll.melody-dynamics` · Accent the hook**
- Roll · 2 bars · 96 BPM · Lead motif (from 8.1 or supplied) at vel 100.
- **New idea:** velocity in a melody: strong-beat notes louder, in-between notes softer.
- **Check:** `mean(vel, notes on beats 1/3) − mean(vel, other notes) ≥ 20` ∧ `min(vel) ≥ 30`.
- Hint: "Softer isn't silent. Keep every note above 30 so it's heard."
- FL tip: in FL, the Piano roll event editor shows velocity; drag a line across several notes.

**10.4 `pattern.intro-build-drop` · Intro, build, drop with Patterns**
- Patterns + Rack · 2-bar Patterns · 120 BPM · Pattern 1 = full loop (supplied or the learner's).
- **New idea:** arrangement by subtraction: the same loop with layers removed.
- Steps: Clone P1 → P2 "Intro": mute Kick and Bass → Clone P1 → P3 "Build": mute Kick, fill hats with 16ths, add a snare roll in the last half-bar → play P2 → P3 → P1.
- **Check:** `P2: emptyOrMuted(Kick) ∧ emptyOrMuted(Bass) ∧ equals(Chords, P1.Chords)` ∧ `P3: emptyOrMuted(Kick) ∧ stepCount(Hat@last bar) ≥ 12 ∧ |Snare@last bar ∩ {9..16}| ≥ 4` ∧ `P1 unchanged`.
- Hint: "The drop hits harder when the kick has been missing."
- FL tip: in FL, place the Patterns in the Playlist (F5) and switch PAT → SONG.

**10.5 `capstone.hook` · Capstone: your four-bar hook**
- All windows · 4 bars · learner's choice of BPM (80–150) and key (C major, A minor or C minor). Empty Pattern. **Jam** (hints allowed, logged).
- **Check (all):**
  - Drums: `every bar: 1 ∈ Kick` ∧ `backbeat(Snare∪Clap) == {5,13} or {9}` ∧ `stepCount(Hat) ≥ 4`.
  - Chords: `every bar: diatonicChordAt(b.1, key)` (triad or 7th).
  - Bass: `rootsFollow(Bass, Chords)`.
  - Lead: `rhythmSimilarity(bars 1–2, bars 3–4) ≥ 0.75` ∧ `!identical(bars 1–2, bars 3–4)` ∧ `strongBeatChordTones ≥ 0.75` ∧ `endsOn(tonic)` ∧ `hasRest(≥240)`.
- Then plan's capstone flow: explain one choice, export MIDI/WAV, desktop rebuild mission.

### B1. Mapping to plan.md's Learn units (where each Studio lesson is offered)

| Plan unit | Studio lessons offered |
|---|---|
| 3 Feel the beat | 1.1, 1.2 |
| 4 Split the beat | 1.3, 1.4, 1.5, 3.1 |
| 5–6 Lengths / open the Piano roll | 2.1, 2.2 |
| 7–8 Steps, keys and scales | 2.3, 2.4 |
| 9 Build chords | 4.1, 4.2, 4.3 |
| 10 Progressions | 4.4, 4.5 |
| 11 Melody over chords | 7.1, 7.4, 8.1, 8.2, 8.3 |
| 12 Shape your hook | 5.1, 10.3, 10.5 |
| v1.1 Groove & arrangement | 3.2–3.4, 5.2–5.4, 6.x, 10.4 |
| Later (plan "Later") | 7.2, 7.3, 7.5, 8.4, 8.5, 9.x, 10.1, 10.2 |

---

## C. Check-rule primitives the engine needs

All primitives read the `Pattern` (channels → notes with pitch, startTick, durationTicks, velocity) plus lesson context (key, chord map, reference events). Each returns `{pass: Boolean, evidence: List<NoteRef|TickRange>, code: String}`. The `code` selects the feedback text and the hint ladder; `evidence` is what the tray highlights and auditions. Compose them with `all(...)`, `any(...)`, `atLeast(n, ...)`.

**Selectors and helpers**
- `bars(pattern)`, `barSpan(b)`, `tick(b, step)`, `beatTick(b, beat)`; `steps(ch, bar) → Set<1..16>` (drum note start ticks ÷ 120, per bar).
- `onsetGroups(ch, tol=0)`: notes grouped by identical start tick (block chords).
- `skyline(ch)`: a monophonic reduction (highest note per onset) for melodic rules when notes overlap.
- `chordMap(ctx)`: the chord in force at any tick, from a ghost channel (pcs of the onset group in force) or from authored chord names.
- `isStrong(tick)`: beats 1 and 3 (configurable). `isWeak(tick)`: everything else.
- `scale(key, type)`: major, naturalMinor, pentatonicMajor, pentatonicMinor.

**Drums and steps**
- `stepSetEquals(ch, set, bars = EVERY | [b…])`
- `stepSetContains(ch, set)`, `stepSetExcludes(ch, set)`, `disjoint(chA, chB)`
- `stepCount(ch, bar, min, max)`, `offBeatCount(ch, bar)` (steps ∉ {1,5,9,13})
- `barEquals(ch, a, b)`, `barDiffSize(ch, a, b) → |symmetric difference|`
- `backbeat(chs) ∈ {{5,13}, {9}}`
- `rollExists(ch, minNotes, allowedSpacings{60,80}, withinTicks)`, `rollsPerBar(ch) ≤ n`
- `swingAmount(min, max)` (a Pattern/Project parameter, matching FL Global Swing)

**Velocity**
- `velocityWhere(ch, selector, op, value)` (e.g. `loud(Snare, ≥90) == {5,13}`)
- `velocityContrast(ch, groupA, groupB, minDiff)` (min of A − max of B, or mean-difference mode)
- `ghostNotes(ch, maxVel, minCount, allowedSteps, avoidChannel = Kick)`
- `velocityRamp(noteRun, nondecreasing | nonincreasing)`
- `minVelocity(ch) ≥ v`

**Pitch, rhythm and timing**
- `notesExactly(ch, [(pitch, tick, dur?)], dims)`
- `rhythmMatches(ch | span, reference, onsetTol, durTol)` (onsets and durations only, pitch ignored)
- `rhythmSimilarity(spanA, spanB) → Jaccard(onsets relative to span start)`
- `silentSpan(ch, tick, length)`, `hasRest(ch, minTicks, span?)`, `restBetween(spanA, spanB, minTicks)`
- `holds(ch, tick, minDur)`, `durationAtLeast(ch, selector, ticks)`, `maxDuration(ch) ≤ t`
- `notesInRange(ch, lo, hi)`, `rangeSpan(ch) ≤ n`, `monophonic(ch)`
- `noteCount(ch, span, min, max)`, `densityPerBar(ch) ≤ n`, `distinctPitches(ch) ≥ n`
- `pitchSequence(ch) == [...]`, `onsetsStrictlyIncreasing(ch)`
- `onsetsSubsetOf(chA, chB)`, `onsetsEqual(chA, chB, perBar)` (bass locks to kick)
- `unchanged(ch | span, reference)` (locked or seed content preserved)

**Harmony**
- `chordAt(ch, tick, pcs, allowInversion = true, octaveFree = true, requireSimultaneous = true, exactCount?)`. Staggered starts fail (plan §6 sample C); a "late" note is reported separately from a "missing" one.
- `chordAtBars(ch, [pcs per bar], options)`
- `bassPc(ch, tick)` (lowest sounding pitch class, for inversions)
- `chordQuality(ch, tick) ∈ {maj, min, dim, 7, maj7, m7}`, `diatonicChordAt(ch, tick, key)`
- `voiceLeadingCost(ch, tickA, tickB)` (minimum total semitone movement over the best one-to-one matching of voices) and `Σ cost ≤ k`
- `rootsFollow(bassCh, chordMap, at = barStart | everyChordChange, octaveFree)`
- `octaveAlternation(ch, pattern = [low, +12])`
- `approachNote(bassCh, beforeTick, targetPitch, maxSemis = 2, inScale)`

**Melody (operate on `skyline(Lead)`)**
- `melodyInScale(ch, key, scale, minRatio = 1.0, allowChromatic = 0)`
- `stepwiseRatio(ch) ≥ r`, where step = 1–2 semitones; repeated notes are excluded from both numerator and denominator
- `hasLeap(ch, minSemis)`, `leapRecovery(ch, minLeap, mode = opposite | oppositeStep)`
- `contourPeak(ch, positionRange, unique, minDrop)` / `contourValley(...)`
- `endsOn(ch, pc | pcs, onsetIn?, minDur?)`, `startsOn(...)`
- `strongBeatChordTones(lead, chordMap, beats, minRatio)`
- `nonChordTonesOnWeak(lead, chordMap) ≥ n`, `passingTones(lead, chordMap) ≥ n`, `neighborTones(...)`
- `motifRepeat(ch, srcSpan, dstSpan, mode = exact | rhythmOnly)`
- `variation(srcSpan, dstSpan, sameRhythm, pitchDiffs ∈ [min, max], where = last | any)`
- `diatonicTranspose(ch, srcSpan, dstSpan, degrees, key)`
- `tendencyResolution(ch, key)` (soft rule: 7→8, 4→3; hint only, never fails a check)
- `fillsGaps(counterCh, leadCh) → share of onsets during lead rests or holds`, `registerBelow(chA, chB, semis)`
- `velocityAccents(ch, strongSelector, minDiff)`

**Patterns and arrangement**
- `patternCount ≥ n`, `emptyOrMuted(pattern, ch)`, `equals(pattern.ch, otherPattern.ch)`, `patternsDiffer(a, b)`

**Engine notes**
- Evaluate on the grid exactly (all edits are snapped). Allow tolerances only where a primitive names them.
- Report **the earliest failing rule first**, with one musical sentence and one highlighted span (plan §8: "the earliest useful correction").
- For Jams, show a **checklist** of every rule (✔/✗), so open composition never feels like guessing.
- For every graded prompt, author a passing example, a near miss with its hint, and an alternate valid answer (plan §8 authoring rule). Unit-test each primitive on them.

---

## Sources (all fetched and read on 26 Nov 2026)

1. Ableton Learning Music, Get started: https://learningmusic.ableton.com/
2. Ableton, Beat and tempo: https://learningmusic.ableton.com/make-beats/beat-and-tempo.html ; Make beats: https://learningmusic.ableton.com/make-beats/make-beats.html
3. Ableton, Backbeats: https://learningmusic.ableton.com/make-beats/backbeats.html ; Bars: https://learningmusic.ableton.com/make-beats/bars.html
4. Ableton, Tempo and genre: https://learningmusic.ableton.com/make-beats/tempo-and-genre.html ; Rock and house: https://learningmusic.ableton.com/make-beats/rock-and-house.html ; Single Ladies: https://learningmusic.ableton.com/make-beats/single-ladies.html
5. Ableton, Make basslines / Around the World / Oblivion: https://learningmusic.ableton.com/make-basslines/make-basslines.html , …/around-the-world.html , …/oblivion.html ; Make melodies / Love Will Tear Us Apart / Tour de France / Praxis / Ride: https://learningmusic.ableton.com/make-melodies/make-melodies.html , …/love-will-tear-us-apart.html , …/tour-de-france.html , …/praxis.html , …/ride.html ; Song structure: https://learningmusic.ableton.com/song-structure/song-structure.html
6. Ableton, Explore pitch / Keys and scales / Minor scales / Adding more notes: https://learningmusic.ableton.com/notes-and-scales/notes-and-scales.html , …/keys-and-scales.html , …/minor-scales.html , …/adding-more-notes.html ; Chords / Major triads / Minor triads / 1-5-6-4: https://learningmusic.ableton.com/chords/chords.html , …/major-triads.html , …/minor-triads.html , …/1-5-6-4.html
7. Image-Line, Learn FL Studio (lesson list): https://www.image-line.com/learn/
8. Image-Line, Making a beat in Channel Rack (lesson 2/13): https://www.image-line.com/learn/lesson/making-a-beat-in-channel-rack
9. Image-Line, Creating melodies in Piano Roll (lesson 3/13): https://www.image-line.com/learn/lesson/creating-melodies-in-piano-roll
10. Hooktheory I, contents: https://www.hooktheory.com/books/one
11. Hooktheory, Popular chord progressions: https://www.hooktheory.com/theorytab/common-chord-progressions
12. musictheory.net, Lessons: https://www.musictheory.net/lessons
13. Melodics, How it works: https://melodics.com/how-it-works
14. Melodics, Stages: https://support.melodics.com/en/articles/15091931-stages
15. Melodics, Understanding your progress: https://support.melodics.com/en/articles/15091765-understanding-your-progress-in-melodics
16. Simply Piano: https://www.joytunes.com/simply-piano
17. Audeobox, How to make beats in FL Studio: https://www.audeobox.com/learn/fl-studio/how-to-make-beats-in-fl-studio/
18. Native Instruments, 7 drum patterns every producer should know: https://blog.native-instruments.com/drum-patterns/
19. Audeobox, How to make trap beats in FL Studio: https://www.audeobox.com/learn/fl-studio/how-to-make-trap-beats-in-fl-studio/
20. Plugg Supply, Ghost notes in drum programming: https://plugg-supply.net/articles/ghost-notes-in-drum-programming
21. Plugg Supply, Boom bap drums: https://plugg-supply.net/articles/how-to-make-boom-bap-beats-2027-drums-only
22. Plugg Supply, Trap hi-hat rolls for beginners: https://plugg-supply.net/articles/trap-hi-hat-roll-patterns-for-beginners
23. Plugg Supply, Velocity in drum programming: https://plugg-supply.net/articles/velocity-in-drum-programming
24. FL Studio manual, Channel Rack & Step Sequencer: https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/channelrack.htm
25. Native Instruments, How to write a bass line: https://blog.native-instruments.com/how-to-write-a-bassline/
26. Open Music Theory, The Best-Seller Progression: https://musictheory.pugetsound.edu/mt21c/BestsellerProgression.html
27. Open Music Theory, Harmonic Rhythm: https://musictheory.pugetsound.edu/mt21c/HarmonicRhythm.html
28. Open Music Theory, Afterbeats and Offbeats: https://musictheory.pugetsound.edu/mt21c/AfterbeatsOffbeats.html
29. Ableton, Inversions / Voicings / Seventh chords / Diatonic triads / Pentatonic: https://learningmusic.ableton.com/advanced-topics/inversions.html , …/voicings.html , …/seventh-chords.html , …/diatonic-triads.html , …/pentatonic.html
30. FL Studio manual, Piano roll: https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/pianoroll.htm
31. Native Instruments, How to write a melody in 5 easy steps: https://blog.native-instruments.com/how-to-write-a-melody/
32. Hooktheory, Stable/Unstable Scale Degree: https://www.hooktheory.com/support/musicreference?concept=music-concepts-stable-unstable-scale-degree
33. Hooktheory, Meter: https://www.hooktheory.com/support/musicreference?concept=music-concepts-meter
34. Open Music Theory, Rules of Melody: https://musictheory.pugetsound.edu/mt21c/RulesOfMelody.html
35. Berklee Online (Andrea Stolpe), Writing Melodies: 3 Simple Tools: https://online.berklee.edu/takenote/simple-tools-for-better-melodies/
36. Open Music Theory, Motive: https://musictheory.pugetsound.edu/mt21c/MotiveSection.html
37. Open Music Theory, Melodic Alteration: https://musictheory.pugetsound.edu/mt21c/MelodicAlteration.html
38. Open Music Theory, Passing Tones: https://musictheory.pugetsound.edu/mt21c/PassingTones.html
39. Open Music Theory, Neighbor Tones: https://musictheory.pugetsound.edu/mt21c/NeighborTones.html
40. Open Music Theory, Phrase: https://musictheory.pugetsound.edu/mt21c/PhraseSection.html
