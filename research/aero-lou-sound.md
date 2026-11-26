# Research brief: Frutiger Aero look, type, Lou's animation and the sound kit

Research date: 26 Nov 2026. Companion to `plan.md` §4 (Design language), and it replaces §4.2 (cast), §4.5 (sound) and §4.6 (visual tokens) where they conflict. The new direction is **one guide (Lou)**, a **Frutiger Aero** look instead of Duolingo's grammar, and new fonts in place of Fredoka and Nunito. The Studio screens' FL-style chrome is out of scope; this brief only covers the app surroundings around them.

**Method and confidence.** The built-in WebSearch and WebFetch tools failed (model error), so every source was pulled with `curl` from the original page, Wikipedia's raw wikitext, the GitHub API, or a Wayback Machine snapshot. Tags:

- **[V]**: verified from a source fetched today (URL given).
- **[M]**: measured today. These are contrast ratios computed with the WCAG 2.x relative-luminance formula, and font URLs checked with `curl -sI`.
- **[U]**: unverified. Prior knowledge or a design judgement. Check it on a device before relying on it.

---

## 0. Decisions at a glance

| Topic | Decision |
|---|---|
| Look | "Aero Sky": a sky-to-mist gradient, translucent white glass panels, glossy gel buttons with a top highlight band, a few slow bokeh bubbles, aqua blue + leaf green + white, light from the top. The modern restraint: flat-ish content cards, glass only for chrome, and gloss only on things you can press. |
| Dark surroundings (Studio shell) | "Aero Night": deep blue-black `#0B1620`, smoky glass at 6–10 % white, an aqua glow accent. |
| Font | **Open Sans** (SIL OFL 1.1), designed by **Steve Matteson, who also designed Segoe**, the typeface behind Windows Aero. Bundle the single variable file `OpenSans[wdth,wght].ttf` (532,636 bytes; wght 300–800, wdth 75–100). |
| Mascot | **Lou** only. One `Canvas` rig with 10 layers, driven by a code state machine with 11 states, springs plus sine/back easings, and blinks every 2–6 s. |
| Sound | Every SFX is synthesized in the existing `AudioEngine`. They are glassy, rounded and tonal, in the lesson key. UI SFX peak at −24 dBFS, hero SFX at −18 dBFS, and Lou's voice at −20 dBFS. Nothing plays during prompt audio, and ear-training items get a neutral, non-pitched variant. |

---

## 1. Frutiger Aero

### 1.1 What it is [V]

- **Definition.** A design style "prevalent from the mid-2000s to the early 2010s". It "originated in user interface designs" and its themes are "technology in harmony with nature", with natural imagery, bright colours and skeuomorphic elements. https://en.wikipedia.org/wiki/Frutiger_Aero
- **Name and origin.** Coined **in 2018 by Sofi Xian (formerly Sofia Lee) of the Consumer Aesthetics Research Institute (CARI)**. The name joins the **Frutiger** typeface (Adrian Frutiger) and **Windows Aero**. It is also called "Web 2.0 Gloss". Critics date it to roughly 2004–2013. It revived in 2023–24 as a Gen-Z nostalgia aesthetic. *TechRadar* suggested Apple's 2025 **Liquid Glass** was likely influenced by it. (Same Wikipedia article. CARI's own page, https://cari.institute/aesthetics/frutiger-aero, returned 200, but it is a JS app with no readable server text.)
- **Motifs named by critics:** blue skies, grass, aurorae, lens flares, bokeh, water and tropical fish (a seapunk overlap), glossy and reflective 3D surfaces, rounded edges, translucent elements, and yellows "and particularly blues and greens". [V, Wikipedia]
- **Exemplars:** Windows Vista (2006) is "one of the first mature examples", Windows 7 expanded on it, plus the Wii and *Wii Sports*, the first iPhone, the Samsung Galaxy S, *Purble Place*, *The Sims 3* and *Fruit Ninja*. [V, Wikipedia] Also the Nintendo DSi menu and Zune [U].
- **Windows Aero** stands for "Authentic, Energetic, Reflective, and Open". It emphasised "animation, glass, and translucency", glass that users could tint, and **Segoe UI** as the system font. Its notifications "fade in and out" and are **queued** instead of interrupting full-screen apps, which is the same queueing idea as our "no SFX over prompt audio" rule. [V] https://en.wikipedia.org/wiki/Windows_Aero
- **Mac OS X Aqua** was "themed to replicate water", with "droplet-like" components, reflective effects and translucency, and **blue, white and gray** as its principal colours. Jobs said "when you saw it you wanted to lick it", and the system font was **Lucida Grande**. [V] https://en.wikipedia.org/wiki/Aqua_(user_interface)
- **Vista's sounds were designed to match the look.** Jim Allchin wrote that the cues should be "just noticeable enough that if they weren't there you would miss them", and that like the visuals they are **"rounded" and "translucent"**, "much gentler and softer" than XP's. The startup sound uses "dual ascending 'glassy' melodies" over a Robert Fripp soundscape. [V] https://web.archive.org/web/20061110152317/http://windowsvistablog.com/blogs/windowsvista/archive/2006/11/09/the-sounds-of-windows-vista.aspx

### 1.2 Visual vocabulary → our building blocks

| Aero motif | Where it goes in Beatpath | Keep tasteful by… |
|---|---|---|
| Sky gradient + light from the top | Every Learn background | One gradient per screen; no photo backgrounds |
| Glass (translucent white panel, bright top edge) | Top bar, bottom nav, feedback tray, Lou's speech bubble, sheets | Glass on **chrome only**, never behind long text blocks; ≥ 55 % white fill |
| Gel buttons (glossy highlight band, saturated base, glow) | Primary CTA (Check/Continue), path nodes as "orbs", the selected nav orb | Only **pressable** things are glossy. Secondary buttons are glass, tertiary are text |
| Bubbles and bokeh | Background ambience, celebration bursts, the XP counter | ≤ 14 bokeh dots, ≤ 6 real bubbles, slow drift; static under reduced motion |
| Water droplets / reflections | Lou's eye catch-lights, the gel highlight, a reflection under Lou on the path | No mirror reflections of UI text |
| Grass and leaf green | The "correct" colour, the Path's horizon hills | Bluish leaf green, **not** Duolingo's `#58CC02` (hue ~94°; ours is ~145°) |
| Aurora ribbons / swooshes | Unit headers, the Lesson Complete screen | 1–2 ribbons at 12–20 % alpha |
| Lens flare / sun glow | Path top-right glow, the Lesson Complete burst | A radial glow only, no hexagon flare sprites |
| Orbs (Vista Start orb) | Bottom-nav selected state, path nodes, Lou's woofer | — |

**Modernising for 2026 [U, design judgement]:**
1. Run "Aero 2026" like Liquid Glass: the material carries the style, the content stays clean. Lesson content sits on near-opaque white cards (92–96 %) with simple type, while the gloss lives in buttons, nav and Lou.
2. Keep gradients soft (two or three stops, no banding), with **no pinstripes, brushed metal or bevel-and-emboss on text**.
3. Use more whitespace and fewer containers than 2007 did. One accent colour per screen, plus feedback colours.
4. Animate the material, not just objects. A slow specular sweep across a gel button on first appearance (600 ms, once) and bubbles that drift. **Never loop shimmer on more than one element at a time.**
5. Accessibility rules (§1.8) beat the aesthetic every time.

### 1.3 Palette

All pairs below were **measured [M]** with the WCAG formula. AA needs 4.5:1 for normal text and 3:1 for large text (≥ 18.66 px bold or 24 px regular) and for UI graphics. Reference: https://www.w3.org/WAI/WCAG21/Understanding/contrast-minimum.html

**Aero Sky (Learn, light)**

| Token | Hex | Role | Measured contrast |
|---|---|---|---|
| `skyTop` | `#5DB8F0` | Top of the background gradient | — |
| `skyHigh` | `#7CC8F5` | Background at ~25 % | Ink on it **8.05** |
| `skyMid` | `#BFE7FB` | Background at ~60 % | Ink **11.28** |
| `mist` | `#F2FBFF` | Background bottom; page base | Ink **14.08** |
| `ink` | `#0E2A3F` | Primary text, icons | — |
| `ink2` | `#35546B` | Secondary text | **7.61** on mist; **6.50** on glass over skyHigh |
| `aeroBlue` | `#1F84D2` → `#0A548F` | Primary gel button (top → bottom) | White label **≥ 4.5** below 42 % height (see §1.4) |
| `aeroBlueText` | `#0A64A8` | Links and accent text on light surfaces | **5.89** on mist |
| `aqua` | `#3FD0E8` | Highlights, glows, progress fill (graphic only) | — |
| `leaf` | `#25985A` → `#115E34` | Correct / success gel | White label ≥ 4.29 at 42 %, ≥ 5.29 at 50 % |
| `leafText` | `#17703F` | "Correct!" text on light surfaces | **5.84** on mist |
| `leafLight` | `#6FD39A` | Success tray tint | Ink on it **8.06** |
| `louTeal` | `#1FA3A0` | Lou's body mid-tone | Body vs skyMid 2.36; a **`louDeep` outline is needed** |
| `louDeep` | `#0B6366` | Lou's outline, shadow side, teal text | **5.36** vs skyMid; **6.69** on mist |
| `louBelly` | `#8FE3D6` | Lou's belly and woofer surround | — |
| `sun` | `#FFC93C` | XP, streak, stars | Ink on it **9.62** |
| `amber` | `#FFB020` | Try-again tray (a miss is information) | Ink on it **8.08**. **Never** white text on amber |
| `rose` | `#D93A4A` | Destructive actions only | White on it **4.51** |
| `glassFill` | `#FFFFFF` @ 55–70 % | Glass panels | Ink on 55 % glass over skyHigh **11.30** |
| `glassEdgeTop` | `#FFFFFF` @ 90 % | 1 dp top rim | — |
| `glassEdgeBottom` | `#FFFFFF` @ 25 % | 1 dp bottom rim | — |
| `shadowTint` | `#0B3D5C` | Every shadow is tinted navy, never pure black | — |

**Aero Night (dark surroundings around Studio; the Studio canvas itself follows FL)**

| Token | Hex | Role | Measured contrast |
|---|---|---|---|
| `nightBg` | `#0B1620` (gradient to `#0E2233` at top) | App shell background | Text **16.31** |
| `nightSurface` | `#13222E` | Panels | text2 **7.74** |
| `nightSurface2` | `#1A2E3D` | Raised panels, sheets | — |
| `nightGlass` | `#FFFFFF` @ 6–10 % + 1 dp top rim @ 20 % + bottom rim `#000000` @ 40 % | Smoky glass toolbars | Text on 8 % glass **11.40** |
| `nightText` | `#E8F4FA` | Primary text | — |
| `nightText2` | `#9DB7C7` | Secondary text | **7.74** |
| `nightAqua` | `#4FC3F7` | Accent, focus glow, playhead-adjacent chrome | **8.09** |
| `nightMint` | `#3DDC97` | Correct | **9.17** |
| `nightAmber` | `#FFB020` | Try again | **8.86** |

**Avoid:** Duolingo green `#58CC02` and anything within ±20° of its hue at high saturation, FL's orange-dominant chrome, and Microsoft's or Apple's actual logos and orb artwork.

### 1.4 Gel button recipe (Compose)

The layers below are drawn bottom to top. Units are dp, and fractions are of the button height `h`.

1. **Outer glow / drop shadow.** `dropShadow(shape, Shadow(radius = 10.dp, spread = 0.dp, color = shadowTint.copy(alpha = .30f), offset = DpOffset(0.dp, 4.dp)))`, plus a second coloured glow `Shadow(radius = 14.dp, color = baseTop.copy(alpha = .35f))`. `Modifier.dropShadow`/`innerShadow` exist in current Compose [V] https://developer.android.com/develop/ui/compose/graphics/draw/shadows
2. **Base.** `Brush.verticalGradient(0f to baseTop, 1f to baseBottom)`. Blue is `#1F84D2 → #0A548F`, leaf `#25985A → #115E34`, Lou teal `#16908D → #0A5A5D`, and amber `#FFC54D → #F09A0A` (amber takes an **ink** label).
3. **Bottom bounce light** (Aero's "light passing through the gel"): a radial gradient centred at (0.5w, 1.1h), radius 0.7w, from `aqua` @ 35 % to transparent. Clip it to the shape.
4. **Inner shadow** (gives the gel thickness): `innerShadow(shape, Shadow(radius = 3.dp, color = Color.Black.copy(alpha = .25f), offset = DpOffset(0.dp, (-1).dp)))`.
5. **Top highlight band.** A rounded rect inset 2 dp on each side, from y = 2 dp to y = **0.42 h**, with corners matching the button minus 2 dp. Fill it with `Brush.verticalGradient(0f to White.copy(.50f), 1f to White.copy(.08f))`. Its bottom edge is hard; that crisp horizon line is the Aero signature.
6. **Rim.** A 1 dp stroke with `Brush.verticalGradient(White.copy(.70f), White.copy(.10f), Black.copy(.20f))`.
7. **Label.** White, Open Sans **Bold 18 sp** (counts as large text), with `Shadow(color = Color(0x99062A4A), offset = Offset(0f, 1.5f), blurRadius = 2f)`.

**Measured label contrast [M]**, white on blue gel at each fraction of the height: 0.35 h → 3.81, 0.42 h → 4.51, 0.50 h → 5.52, 0.65 h → 6.11. A centred 18 sp Bold label spans roughly 0.36–0.64 h, so it clears 3:1 (large text) everywhere and 4.5:1 across most of the glyph. **Don't** raise the highlight alpha above 0.50 or move the band below 0.42 h. At 0.60 alpha with the band to 0.45 h, white measured only 2.7–3.3 in the label zone.

**Press state:** translate y +2 dp, shadow offset 4 → 1 dp, highlight alpha × 0.6, base darkened 8 %. Use `spring(dampingRatio = 0.55f, stiffness = 700f)`. **Disabled:** desaturate to `#9FB4C3 → #7C93A3`, no glow, label `#F2FBFF` at 80 % (exempt from contrast rules, but keep the text label).

**Sizes:** primary CTA height 52 dp, corner radius 16 dp. Gel orbs (path nodes, nav selected) are circles 56–72 dp with the highlight band as an ellipse covering the top 45 %. Minimum touch target is 48 dp.

```kotlin
fun Modifier.gel(top: Color, bottom: Color, radius: Dp = 16.dp) = this
    .dropShadow(RoundedCornerShape(radius), Shadow(10.dp, Color(0x4D0B3D5C), offset = DpOffset(0.dp, 4.dp)))
    .clip(RoundedCornerShape(radius))
    .background(Brush.verticalGradient(listOf(top, bottom)))
    .drawWithCache {
        val r = radius.toPx(); val inset = 2.dp.toPx()
        val band = Brush.verticalGradient(0f to Color.White.copy(.50f), 1f to Color.White.copy(.08f),
            startY = inset, endY = size.height * .42f)
        val bounce = Brush.radialGradient(listOf(Color(0x593FD0E8), Color.Transparent),
            center = Offset(size.width / 2, size.height * 1.1f), radius = size.width * .7f)
        onDrawWithContent {
            drawRect(bounce)
            drawRoundRect(band, Offset(inset, inset), Size(size.width - 2 * inset, size.height * .42f - inset),
                CornerRadius(r - inset))
            drawContent()
            drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(.7f), Color.White.copy(.1f), Color.Black.copy(.2f))),
                style = Stroke(1.dp.toPx()), cornerRadius = CornerRadius(r))
        }
    }
```

### 1.5 Glass panel recipe

1. **Backdrop.** True backdrop blur isn't built into Compose. `Modifier.blur()` blurs the composable's **own** content and "is only supported on Android 12 and later; attempts to use this modifier on older Android versions are ignored" [V] https://developer.android.com/develop/ui/compose/graphics/images/customize. Options:
   - **A (recommended, zero-dependency):** the sky background is procedural, so draw it a second time inside the panel's bounds with `Modifier.blur(24.dp)` on API 31+. It is blurred "for free", because the panel only sits over our own background. Below API 31, skip the blur and raise the fill alpha to 70 %.
   - **B:** **Haze** (`dev.chrisbanes.haze`, Apache-2.0, v2.1.0 released 2 Nov 2026, with a separate `haze-glass` refraction module) [V] https://github.com/chrisbanes/haze. Use it if glass must sit over moving content such as Lou or scrolling lists.
2. **Fill:** white @ 55 % (bars, tray) to 70 % (speech bubble), plus a vertical sheen `Brush.verticalGradient(0f to White.copy(.35f), .5f to White.copy(.0f))` over the top half.
3. **Rim:** a 1 dp stroke, `verticalGradient(White.copy(.90f), White.copy(.25f))`. In dark mode it fades from white @ 20 % to black @ 40 %.
4. **Shadow:** `dropShadow(Shadow(radius = 18.dp, color = shadowTint.copy(.18f), offset = DpOffset(0.dp, 6.dp)))`.
5. **Radius:** panels 20 dp, bottom sheets 28 dp on the top corners, speech bubble 20 dp with a 12 dp tail.
6. **Text on glass:** `ink`/`ink2` only, never white. A 55 % glass over the darkest sky stop measured **11.30** for ink [M]. Long text belongs on 92–96 % cards, not glass.
7. **Optional "engraved" text on glass headers:** a 1 dp white @ 60 % text shadow offset +1 dp in y (the Vista and Aqua look).

### 1.6 Background recipe ("Aero Sky")

1. **Sky:** `Brush.verticalGradient(0f to #5DB8F0, .25f to #7CC8F5, .60f to #BFE7FB, 1f to #F2FBFF)`. Add 1–2 % noise dither as a tiled 64×64 noise bitmap at 3 % alpha if banding shows on 8-bit panels [U].
2. **Sun glow:** a radial gradient at (0.85 w, 0.06 h), radius 0.65 w, from white @ 55 % → white @ 0. Draw it once and cache it.
3. **Aurora ribbons** (unit header and Lesson Complete only): two cubic Béziers spanning the width, stroke 48–80 dp, `Brush.horizontalGradient(transparent, aqua@18 %, white@22 %, transparent)`. Phase drifts with a 40 s loop.
4. **Bokeh dots:** 10–14 circles, radius 8–48 dp. Fill is a radial white @ 22 % → 0, with a 1 dp ring of white @ 30 %. They drift **up** at 6–14 dp/s with a horizontal sine wobble (amplitude 6 dp, 0.15–0.25 Hz) and wrap from bottom to top. Draw them in one `Canvas` from a precomputed array, with one `withFrameNanos` loop.
5. **Bubbles** (3–6, larger, 24–64 dp):
   - a transparent body plus a rim stroke of 1.5 dp, `sweepGradient(white@80 %, aqua@40 %, white@20 %, white@80 %)`;
   - a specular ellipse at the top-left (−0.35 r, −0.40 r), size 0.45 r × 0.25 r, rotated −30°, white @ 85 %;
   - a small secondary highlight at the bottom-right, white @ 30 %.
   - On tap or on celebration they **pop**: 90 ms scale 1 → 1.25 with alpha → 0, 6 droplet particles, plus the bubble-pop SFX.
6. **Horizon hills** (Path screen only): two overlapping hills filled with `verticalGradient(#8BD66B, #3FA35A)`, with a gel highlight band across the top 30 % at white @ 35 %.
7. **Dark (Aero Night):** a vertical gradient `#0E2233 → #0B1620 → #07121C`, one teal aurora ribbon at `#19C6C0` @ 10 %, bokeh at 8 % alpha, and no sun.
8. **Reduced motion** (system animator scale 0, or the in-app toggle): freeze the bokeh, bubbles and aurora at their initial positions.
9. **Performance:** cache every `Brush`/`Path` in `drawWithCache`. Read time only in the draw phase, so nothing recomposes per frame. Stop the loop when the screen isn't resumed.

### 1.7 Radii, shadows, motion tokens

| Token | Value |
|---|---|
| Radius XS / S / M / L / XL | 8 / 12 / 16 / 20 / 28 dp; capsule = h/2 for chips |
| Primary gel button | 16 dp (capsule is acceptable for a single hero CTA) |
| Cards (content) | 20 dp, fill white 94 %, rim white 100 % top → `#D6EAF5` bottom |
| Elevation 1 (cards) | navy `#0B3D5C` @ 12 %, y = 2, blur 6 |
| Elevation 2 (glass bars, tray) | navy @ 18 %, y = 6, blur 18 |
| Elevation 3 (dialogs, Lou's shadow) | navy @ 25 %, y = 12, blur 28 |
| Glow (selected, focus) | `aqua` @ 45 %, blur 12, spread 2 |
| Focus ring (accessibility) | 2 dp `#0A64A8` + 2 dp white outer, always visible on keyboard/D-pad focus |
| Motion | Keep the plan's quick 120 / standard 240 / emphasized 400 ms. Springs: buttons `damping .55, stiffness 700`; sheets `damping .85, stiffness 400`; Lou (heavy) `damping .45–.6, stiffness 120–250` |

### 1.8 Accessibility rules for gloss and glass

1. Text never sits on the top 40 % highlight band of a gel button. The label is vertically centred, and the band stops at 0.42 h with ≤ 0.50 alpha [M].
2. White text only on the darker 60 % of gels (blue, leaf, teal, rose). **Ink text on amber, sun, aqua and light glass.**
3. Glass under text is ≥ 55 % white, and paragraphs go on ≥ 92 % cards.
4. Feedback always carries an icon and words ("Correct!", "Not quite"), not just colour.
5. Check contrast against the **lightest** pixel under each glyph, not the average. Re-run the script (`contrast.py`, recipe in §5) when you change tokens.
6. Respect "Remove animations" and font scale up to 200 %. Gel buttons grow in height, and the highlight band stays at 0.42 h.

---

## 2. Font

### 2.1 Era fonts and why we can't ship them

The Aero-era UI fonts were **Segoe UI** (Windows Vista/7, humanist, Steve Matteson, with a protested resemblance to Frutiger), **Lucida Grande** (Mac OS X 10.0–10.9), **Myriad** (Apple marketing) and **Frutiger** itself. Wikipedia notes Frutiger "was never used in a major user interface associated with the style". All of these are proprietary. [V] https://en.wikipedia.org/wiki/Segoe , https://en.wikipedia.org/wiki/Aqua_(user_interface) , https://en.wikipedia.org/wiki/Frutiger_Aero

### 2.2 Open candidates (all checked today)

| Font | Licence [V] | Designer [V] | Files and weights [V] | Aero fit | Verdict |
|---|---|---|---|---|---|
| **Open Sans** | OFL 1.1 | **Steve Matteson** (also the designer of Segoe) | Variable `OpenSans[wdth,wght].ttf`, wght 300–800, **wdth 75–100**; static TTFs upstream | Same hand as Segoe: humanist, open apertures, "neutral, yet friendly". Reads Vista/7-era and is nothing like Duolingo's rounded faces | **Primary** |
| Selawik | OFL 1.1, Reserved Font Name "Selawik" | Microsoft | **No TTFs in the repo**, only `Selawik_Release.zip` (530,652 B, release 1.01 from 2015); UFO sources Light/Regular/Bold | Literally the "open source replacement for Segoe UI", but its README says it is "missing kerning to match Segoe UI" and "needs improved hinting" | Not recommended |
| Hind | OFL 1.1 | Indian Type Foundry | Static 300/400/500/600/700, ~280–300 KB each (Devanagari included) | The most Frutiger-like of the set: flat terminals, humanist, "developed explicitly for UI" | **Runner-up** |
| Signika | OFL 1.1 | Anna Giedryś | Variable wght 300–700 + **GRAD** | A signage face like Frutiger, slightly condensed and softer | Alternative |
| Istok Web | OFL 1.1 | Andrey Panov | 400/700 + italics | Frutiger-ish, but only 2 weights | No |
| PT Sans | OFL | ParaType | 400/700 + italics | Too "Russian grotesk" | No |
| Fira Sans | OFL | Carrois | 9 weights | Reads Firefox OS/Meta, more 2013 than 2007 | No |
| Source Sans 3 | OFL | Paul D. Hunt | Variable 200–900 | Close to Myriad/Franklin. A fine body face, but less Aero | Possible |
| Mukta | OFL | Ek Type | 200–800 static | Humanist, similar to Hind | Possible |

Sources: GitHub API listings and `METADATA.pb`/`DESCRIPTION.en_us.html` under https://github.com/google/fonts/tree/main/ofl/ (opensans, hind, istokweb, signika, mukta, sourcesans3, ptsans, firasans), and the Selawik README/LICENSE/releases at https://github.com/microsoft/Selawik. Google Fonts describes Open Sans as "a humanist sans serif… designed by Steve Matteson… with an upright stress, open forms and a neutral, yet friendly appearance… optimized for print, web, and mobile interfaces". Wikipedia credits Segoe to Matteson. [V]

### 2.3 Recommendation: Open Sans

**Bundle one file:** the variable roman. Variable `FontVariation.Settings` requires API 26, and the project's `minSdk = 27` qualifies [V, `android/app/build.gradle.kts`]. There's no italic file; italics are rare in the UI, and the static italic can be added later if needed.

**Verified URLs** (`curl -sI` → HTTP 200 [M]):

| File | URL | Size |
|---|---|---|
| Variable roman (recommended) | https://raw.githubusercontent.com/google/fonts/main/ofl/opensans/OpenSans%5Bwdth,wght%5D.ttf | 532,636 B |
| Same, pinned to commit | https://raw.githubusercontent.com/google/fonts/6ebe737e44fd64ee736bbc6feed6136a6dbfd222/ofl/opensans/OpenSans%5Bwdth,wght%5D.ttf | 532,636 B |
| Licence (ship it in `assets/licenses/`) | https://raw.githubusercontent.com/google/fonts/main/ofl/opensans/OFL.txt | 4,389 B |
| Variable italic (optional) | https://raw.githubusercontent.com/google/fonts/main/ofl/opensans/OpenSans-Italic%5Bwdth,wght%5D.ttf | — (200) |
| Static fallback: Regular 400 | https://raw.githubusercontent.com/googlefonts/opensans/main/fonts/ttf/OpenSans-Regular.ttf | 147,528 B |
| Static fallback: SemiBold 600 | https://raw.githubusercontent.com/googlefonts/opensans/main/fonts/ttf/OpenSans-SemiBold.ttf | 150,592 B |
| Static fallback: Bold 700 | https://raw.githubusercontent.com/googlefonts/opensans/main/fonts/ttf/OpenSans-Bold.ttf | 147,264 B |
| Static fallback: ExtraBold 800 | https://raw.githubusercontent.com/googlefonts/opensans/main/fonts/ttf/OpenSans-ExtraBold.ttf | 151,700 B |
| Upstream licence | https://raw.githubusercontent.com/googlefonts/opensans/main/OFL.txt | 4,216 B ("SIL OPEN FONT LICENSE Version 1.1") |
| Runner-up Hind 400/500/600/700 | https://raw.githubusercontent.com/google/fonts/main/ofl/hind/Hind-Regular.ttf (and `-Medium`, `-SemiBold`, `-Bold`) | 299,532 / 277,300 / 281,940 / 286,604 B |

Save the file as `res/font/open_sans_var.ttf`, because resource names must be lowercase with no brackets. **Delete `fredoka.ttf` and `nunito.ttf`.**

**Type scale** (Learn). Keep the weights to four: 400, 600, 700, 800.

| Style | Size / line | Weight | wdth | Notes |
|---|---|---|---|---|
| Display (Lesson Complete, Unit title) | 32/38 | 800 | 100 | letterSpacing −0.25 sp; optional white text with a navy glow on sky |
| Title | 24/30 | 700 | 100 | |
| Headline (prompt) | 20/26 | 700 | 100 | |
| Body | 16/24 | 400 | 100 | |
| Body strong | 16/24 | 600 | 100 | |
| Button label | 18/22 | 700 | 100 | Counts as WCAG large text |
| Label / chip | 14/20 | 600 | 100 | |
| Caption | 12/16 | 400 | 100 | Minimum size in Learn |
| Studio shell label (dense) | 12/16 | 600 | **87.5** | The condensed width replaces a second font; use `fontFeatureSettings = "tnum"` for BPM, bars and ticks (whether Open Sans figures are tabular by default is [U], so force it) |

```kotlin
@OptIn(ExperimentalTextApi::class) // drop the opt-in if your Compose version has it stable [U]
private fun os(w: Int, wdth: Float = 100f) = Font(
    R.font.open_sans_var, FontWeight(w),
    variationSettings = FontVariation.Settings(FontVariation.weight(w), FontVariation.width(wdth)))
val Aero = FontFamily(os(400), os(600), os(700), os(800))
val AeroCondensed = FontFamily(os(400, 87.5f), os(600, 87.5f), os(700, 87.5f))
```

---

## 3. Lou: code-driven character animation

### 3.1 What the pros do, and what we copy

- **Duolingo** uses **Rive state machines**: "a visual representation of the logic that connects the animations ('states')", which lets engineers "programmatically control which animation states are called, how they are called, and how they transition and blend". They run **body/pose states and separate mouth (viseme) states at the same time**, with 20+ mouth shapes per character. They drive visemes from phoneme timings synced to audio, layer **idle behaviour like "head nods, blinking, and eyebrows moving"**, and end on a reaction state for right or wrong. [V] https://blog.duolingo.com/world-character-visemes/
- Their animators do "multiple passes of rough animation… before refining the overall rhythm and energy" (timing first, polish later). [V] https://blog.duolingo.com/streak-milestone-design-animation/
- **We copy the architecture, not the tool:** independent layers (body, face, mouth, idle) with their own state, all driven by one clock and one state machine in Kotlin.
- **Classic principles** [V] https://en.wikipedia.org/wiki/Twelve_basic_principles_of_animation:
  - squash and stretch gives "weight and flexibility" (keep the volume constant);
  - anticipation;
  - follow-through and overlapping action;
  - slow in / slow out ("more pictures… near the beginning and end of an action");
  - secondary action;
  - timing;
  - exaggeration;
  - appeal.
- **Blink physiology** [V] https://en.wikipedia.org/wiki/Blinking: a blink lasts "on average 100–150 milliseconds" (UCL), with "between each blink… an interval of 2–10 seconds". The rate falls to 4–5 per minute when focused, which we use for Listening.

### 3.2 Rig (Canvas layers, drawn back to front)

Lou sits in a 1:1 box. Units are fractions of the box size `S`; the rig origin is bottom-centre (the feet).

| # | Layer | Shape | Animated params |
|---|---|---|---|
| 0 | Ground shadow | Ellipse 0.70 S × 0.08 S, navy @ 22 % | Width scales 1/(stretchY); alpha drops while airborne |
| 1 | Back arm (L) | Capsule 0.10 × 0.26 S, `louDeep` → `louTeal` | Rotation (pivot at the shoulder), lag |
| 2 | Body | Rounded "gumdrop" path (bottom wider), radial gradient `#3FC1BC` (top-left) → `#1FA3A0` → `#0E7C7C` (bottom-right) + 2.5 dp `louDeep` outline | `scaleX`, `scaleY` (pivot at the feet), `tilt` (±6°), `y` |
| 3 | Body gel highlight | Ellipse at the top-left, white @ 45 → 0 % | Moves with the body; specular slides 2 % opposite the tilt |
| 4 | Belly | Ellipse 0.55 × 0.45 S, `louBelly` with a soft inner shadow | Jiggle spring (secondary motion) |
| 5 | Woofer | Surround ring (`#2E3B45`), cone (radial `#4A5A66 → #1C252C`), dust cap (gel orb `#5E6E7A` + highlight) | `coneScale` 0.92–1.15, `capOffset` |
| 6 | Eyes | Two white ellipses (0.17 × 0.15 S), navy iris, 2 catch-lights (water-droplet Aero touch) | `lookX/lookY` (iris offset ≤ 0.035 S), `pupil` size |
| 7 | Eyelids | A teal lid path covering the top of each eye; **resting lid = 0.45 (sleepy half-lidded)** | `lid` 0 (open) … 1 (closed), per eye for winks |
| 8 | Brows | Short capsules, `louDeep` | `browY`, `browTilt` (sad = inner up, think = one up) |
| 9 | Mouth | Path morph between 6 shapes: rest (small flat smile), `M` (closed line), `A` (open oval), `O` (round), `E` (wide), `grin` | `mouthOpen` 0–1, `shape` crossfade (`lerp` of control points) |
| 10 | Front arm (R) | As layer 1 | Rotation, lag |
| 11 | FX | Zzz glyphs, sparkles, bubbles, "?" glass bubble | Per-state particles |

**Implementation:**
- One `LouState` holds a `MutableState<LouPose>` (pure floats). One `LaunchedEffect` runs a `withFrameNanos` loop that advances springs and timers. The `Canvas` reads the pose **in the draw phase only**, so there is no recomposition per frame.
- Pause when the lifecycle isn't RESUMED or Lou is off-screen. Keep one Lou per screen.
- Use **critically-damped / under-damped springs** (semi-implicit Euler: `v += (k(target−x) − c·v)·dt; x += v·dt`) for body scale, tilt, arms, belly, brows and gaze, so state changes blend automatically (Rive-style blending without blend trees).
- **Volume rule:** `scaleX = 1 / sqrt(scaleY)`. Squash keeps the area constant.

### 3.3 Always-on idle layers (run under every state unless overridden)

1. **Breathing.** Lou is big and calm, so he breathes slowly: a **4.2 s period**. `scaleY = 1 + 0.022·sin(2πt/4.2)`, `scaleX = 1/sqrt(scaleY)`. The belly lags 180 ms, the woofer cone breathes ±2 %, and the arms swing ±2° in phase. Ease: pure sine (already slow in, slow out).
2. **Blinks.**
   - The next blink comes after `uniform(2.0, 6.0)` s. 15 % chance of a double blink (gap 120 ms).
   - Lid curve: **close 55 ms (ease-in quad) → hold 35 ms → open 80 ms (ease-out quad) = 170 ms**. The human 100–150 ms reads as a flicker at 60 fps, so the cartoon cut runs slightly longer [V/U].
   - Lou's resting lid is 0.45 (sleepy), so a blink animates 0.45 → 1 → 0.45.
   - Blink on every big gaze shift (> 0.5 of max) and on every state change into Happy, Surprised or Thinking (it reads as a "cut").
3. **Gaze / look-around.** Every `uniform(5, 11)` s, pick a target (mostly the content area or the learner, sometimes the sky). The eyes move first (spring, stiffness 300, damping 0.8, ~120 ms). The body tilts toward the target 150 ms later (stiffness 120, damping 0.6) by ≤ 3°. Hold 0.8–2.0 s, then return to centre.
4. **Micro-motion.** Brows drift ±1 % at 0.13 Hz, and there is a 1 px body sway at 0.07 Hz. That keeps him from ever looking frozen.

### 3.4 States, timings and transitions

The trigger names match the plan's shared properties (`listen`, `talk`, `correct`, `miss`, `celebrate`, `idle`, `sleep`, `beatPhase`, `downbeat`). Priority from high to low: **Listening > Surprised > Celebrate/Happy > Encourage > Talking > Thinking > Dancing > Idle > Sleepy**. A higher state interrupts a lower one. Each state returns to `Idle` (or the previous looping state) when done.

| State | Trigger | Motion recipe | Timing | Sound (§4.4) |
|---|---|---|---|---|
| **Idle** | Default | Breathing, blinks, look-around, lid 0.45, rest smile | Loops | — |
| **Listening** | Prompt or example audio starts | Anticipation: lids drop to 0.8 (eyes nearly closed, "feeling the bass") over 250 ms (ease-in-out). Body leans 3° toward the speaker icon. Breathing amplitude halves. **Blinks paused**, look-around off, bubbles hidden, mouth `M`. The woofer pumps gently to the prompt's beat if it has one (beatPhase), amplitude ×0.5 | Enter 250 ms, exit 200 ms | **Silent. No Lou sounds, no UI SFX** |
| **Talking** | A speech bubble appears | Mouth driven by the babble synth's syllable envelope (same scheduler, §4.4). The vowel picks the mouth shape (a→`A`, o/u→`O`, e→`E`, m→`M`). Brows rise +2 % on stressed syllables, plus a small head bob (scaleY −1.5 %) on each stressed syllable. Arms gesture ±8° every 2–3 syllables. The bubble text reveals in step with the syllables | Syllables 60–140 ms; the whole line is capped at 1.4 s even if the text is long | Babble |
| **Happy** (correct) | `correct` | **Anticipation** squash: scaleY 0.90, scaleX 1.05 over 110 ms (ease-in cubic). **Jump**: y −0.10 S, stretch scaleY 1.10 over 180 ms (ease-out). Hang 60 ms. **Fall** 160 ms (ease-in). **Land** squash 0.92, then spring back (damping 0.45, stiffness 220: 2–3 wobbles). Arms swing up with an 80 ms lag (follow-through), and the belly jiggle spring is kicked. Eyes become happy arcs (lid 0.55 + bottom-lid curve), with a grin. The woofer pulses ×1.15 on landing | Total ≈ 650 ms + settle 300 ms | "bwoop" at take-off; haptic THUD on landing |
| **Celebrate** (lesson complete) | `celebrate` | Happy jump ×2 (second jump 70 % height), then a 1.5 s **dance loop** (see Dancing) with a confetti bubble burst; woofer at max | ≈ 2.8 s | Fanfare plus "bwoop" |
| **Encourage** (sad-ish but kind, on a miss) | `miss` | **No frown, no slump** (plan §4.3). Brows: inner ends up 4 % (sympathetic). Head tilt 6° over 300 ms (ease-in-out sine), lid 0.35 (more open = attentive). A slow nod (scaleY 0.97 → 1, 2 × 350 ms). One arm raises in an "it's okay" gesture (rotate −25°, spring). Mouth small `O`, then a soft smile | Enter 300 ms, hold until the tray closes | One soft "hm?" (the Thinking sound at 70 % length, rising contour); the miss tone already plays from the UI |
| **Thinking** | Hints, "let's look at this", loading > 400 ms | Eyes look up-left (iris −0.03 S, −0.03 S). One brow up 5 %, mouth pushed to the side (`M` offset 0.03 S). An arm taps the chin: 2 taps, 180 ms each, then a 600 ms pause, looped. Small body sway ±1.5° at 0.5 Hz. A "…" glass bubble pulses | Loops | "hmm" once on entry |
| **Surprised** | A big streak or score, first time Studio opens, a new feature | **Anticipation** squash 0.95 (60 ms), then **pop** stretch scaleY 1.12 (90 ms, ease-out back: `CubicBezierEasing(0.34, 1.56, 0.64, 1f)`). Lids snap to 0 (fully open; his only wide-eyed moment), pupils shrink 20 %, brows +8 %, mouth `O`. Hold 400 ms, then settle back to the 0.45 lid over 300 ms | ≈ 850 ms | "oh!" |
| **Sleepy** | 45 s without interaction on Path/Home, or a reminder screen | Lids droop 0.45 → 0.85 over 2 s (ease-in-out). Breathing slows to a 6 s period with amplitude ×1.5. The head nods down slowly 4° and jerks back up (overshoot spring) every 8–12 s. A **yawn** every ~20 s: mouth `A` opens over 600 ms, holds 500 ms, closes over 400 ms, with the body stretching scaleY 1.06 and the arms out. "Z" glass bubbles rise at 0.5 per second | Loops; any touch → blink, then Idle (300 ms) | Yawn on each yawn, else silent |
| **Dancing** | Music the learner made is playing (Unit Jam, Hear mine, loop preview) | Driven by the audio engine's **reported playback position** (plan §4.4), not a UI timer. Per beat (phase φ 0..1): `scaleY = 1 − 0.06·e^(−8φ)` (squash on the beat), `coneScale = 1 + 0.15·e^(−10φ)` (the **woofer pumps** on kicks or bass notes when known, else on every beat). Head bob on eighths at ±1.5 %. Body sway ±4° over 2 beats (sine). Arms alternate on the backbeat. Eyes closed to 0.75 (feeling it) | Locked to tempo | None (the music is the sound) |
| **Pointing** (tutorial) | `point(target)` | The body turns 6° toward the target, the near arm extends (rotate to the target angle) with a 1.2× overshoot spring, and the eyes follow the target | Enter 350 ms | Select blip |

**Transition rules**

1. Every state entry blends through springs. Never jump-cut more than one layer, except blinks, which hide cuts.
2. Each state has a minimum dwell time (Happy 650 ms, Encourage 600 ms) before a lower-priority state can replace it.
3. **Listening hard-gates** all sound and particles from Lou (plan §4.3 rule 2).
4. **Reduced motion:** crossfade poses (200 ms alpha) instead of jumps and squash. Keep blinks (small), drop look-around, sway and dance, and show the woofer statically lit on beats instead of scaling.
5. **Visibility "Off"** removes Lou and his voice; the UI SFX remain.

**Easing cheat-sheet (Compose)**

| Use | Easing |
|---|---|
| Anticipation | `CubicBezierEasing(0.55f, 0f, 0.75f, 0f)` (ease-in cubic) |
| Release / jump | `CubicBezierEasing(0.22f, 1f, 0.36f, 1f)` (ease-out quint) |
| Pop | `CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)` (ease-out back) |
| Landing and wobble | `spring(dampingRatio = 0.45f, stiffness = 220f)` |
| Gaze | `spring(0.8f, 300f)` |
| Body follow | `spring(0.6f, 120f)` |
| Breathing / sway | Sine |

---

## 4. Sound: UI SFX and Lou's voice

### 4.1 Principles (sourced)

- **Material sound guidance** [V] https://material.io/design/sound/applying-sound-to-ui.html (archived at https://web.archive.org/web/20190923132432/https://material.io/design/sound/applying-sound-to-ui.html) and https://web.archive.org/web/20191118144921/https://material.io/design/sound/about-sound.html:
  - Earcons link "an action, or a state change, to an audio cue".
  - "Use decorative sound sparingly" and "limit the frequency of decorative sound to reduce user fatigue".
  - "Building silence into a UI design is just as important as knowing when to apply sound".
  - Hero sounds "occur infrequently" and are applied consistently.
  - Primary UX sounds "should be suitable for being played often (without feeling annoying or redundant)".
  - UI sounds should "work like a well-coordinated orchestra".
- **The Aero sound ethos** [V] (Vista blog, §1.1): "rounded" and "translucent", "gentler and softer", "glassy" ascending melodies, and just noticeable enough that you'd miss them if they were gone.
- **Game feel / "juice"** [V] https://en.wikipedia.org/wiki/Game_feel: Swink's six areas are input, response, context, **polish**, metaphor and rules. "Polish" means the layered sound, motion and particle response that sells an interaction. (The "Juice it or lose it" talk by Jonasson & Purho, 2012, is the usual reference [U].)
- **Haptics** [V] https://developer.android.com/develop/ui/views/haptics/haptics-principles:
  - "Less is more".
  - "Favor rich and clear haptics over buzzy haptics".
  - "Correlate event importance and frequency with strength".
  - Co-design haptics with audio and visuals, because out-of-sync haptics feel broken.
  - Use `HapticFeedbackConstants` first, then `VibrationEffect.Composition` primitives, and **avoid legacy one-shot vibrations**.
  - `CONFIRM`/`REJECT`, `GESTURE_THRESHOLD_ACTIVATE` and `SEGMENT_FREQUENT_TICK` exist in the reference [V] https://developer.android.com/reference/android/view/HapticFeedbackConstants; check the per-constant API levels.
- **Duolingo-style UI sound** (observation, [U]): very short, bright, tonal, mostly major-key intervals, a recognisable "correct ding" with a rising figure, and a soft low "nope" that's never a buzzer.

**Our rules**

1. **One sonic material: glass.** Use sine and triangle partials, bell-like inharmonic partials, and soft noise "air". Never square buzzers or sawtooth leads in UI.
2. **Key-locked.** Tonal SFX use scale degrees of the current lesson key (C major outside lessons). Same family, same key.
3. **Short.** Primary UX sounds ≤ 150 ms, secondary ≤ 400 ms, hero ≤ 1.6 s.
4. **Variation against fatigue.** Every play gets random pitch ±12 cents and level ±1.5 dB. Tap sounds round-robin over 3 seeds. The same SFX is dropped if retriggered within 40 ms.
5. **Haptic plus sound plus motion fire on the same frame**, with the sound scheduled using the engine's latency estimate.

### 4.2 Levels and ducking (protect the ear-training prompts)

| Bus | Peak level | Linear gain | Notes |
|---|---|---|---|
| Prompt / music / learner loop | −6 dBFS | 0.50 | Reference |
| Hero SFX (correct, complete, celebrate) | −18 dBFS | 0.126 | 12 dB under music |
| Lou voice | −20 dBFS | 0.10 | |
| UI SFX (tap, select, toggle, swoosh, XP tick) | −24 to −30 dBFS | 0.063–0.032 | ≈ 18 dB under music (plan §4.5) |

**Gating rules** (implement them in the engine's command queue, not in UI code)

1. **Prompt lock.** While prompt, example or "Hear mine" audio is scheduled **or** playing, plus **300 ms pre-roll** and **250 ms post-roll**, every SFX and Lou-voice command is **dropped**. Lou goes to Listening. Only the press feedback of the play/stop button itself may play, and it uses the *neutral* click.
2. **Tonal hygiene for ear training.** On pitch, interval or chord items, the correct chime and the miss tone switch to **neutral variants** (non-pitched: glass click plus noise sparkle) on the screen *before* the next listening item. Otherwise the chime's pitches can prime the learner's sense of key or interval [U, reasoning]. The tonal versions are fine on rhythm, sequencing and Studio steps.
3. **Settle time.** After any hero SFX, the next prompt can't start until the SFX tail is below −60 dB, or for **at least 600 ms**, whichever is shorter. Continue does wait for it.
4. **Music ducking.** If a UI sound must play while the *learner's own loop* plays in Studio (not a graded prompt), duck the loop by 4 dB for 120 ms around the SFX. Graded prompts are never ducked; their SFX are dropped instead (rule 1).
5. **Voice limits.** At most 6 SFX voices; steal the oldest. Lou's babble never overlaps itself; a new line cancels the old one with a 25 ms fade.
6. **System.** Play through the same `AudioTrack` (`USAGE_GAME`, `CONTENT_TYPE_SONIFICATION` for the SFX bus if split out). Don't request audio focus for UI blips [U]. Honour the in-app "UI sounds" and "Lou's voice" toggles. Default UI SFX to off when the ringer is silent or vibrate [U, product choice]. For focus behaviour see [V] https://developer.android.com/media/optimize/audio-focus
7. **Phone speakers.** Small speakers barely reproduce below ~200–300 Hz [U]. Lou's "low" voice must get its identity from **harmonics and formants (250–1600 Hz)** so it reads as low via the missing-fundamental effect. Sub-bass sines are a headphone-only bonus at ≤ −12 dB relative.

### 4.3 Synth primitives (sample-by-sample Kotlin, fits `AudioEngine.renderBlock`)

`t` is seconds since the voice started, `sr` is the sample rate, and `dt = 1/sr`. Keep a phase accumulator per oscillator: `ph += f*dt; ph -= floor(ph)`.

| Primitive | Formula |
|---|---|
| Sine | `sin(2π·ph)` |
| Triangle | `1 − 4·abs(ph − 0.5)` |
| PolyBLEP saw (band-limited) | `2ph − 1 − blep(ph, f·dt)`. Needed for Lou's voice; a naive saw aliases audibly at the formant gain |
| Pulse (width w) | `saw(ph) − saw(ph + w)` (each with BLEP) |
| Noise | xorshift32 → [−1, 1] (the engine already has `noise()`) |
| Exp pitch glide | `f(t) = f1 + (f0 − f1)·e^(−t/τp)` |
| FM bell | `sin(2π·phc + I(t)·sin(2π·phm))`, `fm = r·fc`, `I(t) = I0·e^(−t/τI)` |
| Additive "glass bar" | Partials at ratios **1, 2.756, 5.404, 8.933** (free-free bar modes) [U, standard acoustics], each with its own exp decay |
| AD/ADSR | Linear attack, then exp decay `e^(−t/τ)`; release `e^(−tr/τr)`; hard-stop the voice when env < 1e-4 |
| One-pole LP | `a = 1 − e^(−2π·fc/sr)`; `y += a·(x − y)` |
| One-pole HP | `y = x − lp(x)` |
| SVF band-pass (Chamberlin) for formants | `F = 2·sin(π·fc/sr)`, `q = 1/Q`; per sample: `low += F·band; high = x − low − q·band; band += F·high`, output `band`. Stable while fc < sr/6 (8 kHz at 48 kHz), so it's fine for F1/F2 |
| Soft clip | `tanh(drive·x)/tanh(drive)` |
| Level | `gain = 10^(dB/20)` |

**Patch format suggestion:** a `data class Sfx(val layers: List<Layer>, val peakDb: Float)`, where `Layer(osc, f0, f1, tauP, fmRatio, fmIndex, tauI, noiseMix, filter, fc0, fc1, tauFc, q, attack, tau, hold, release, delay, gainDb)`. All recipes below are data, and the engine renders them with one generic voice. "Key-locked" frequencies are given for **C major**; transpose by the lesson key (`f × 2^(semitones/12)`).

Reference pitches: C4 261.63 · E4 329.63 · G4 392.00 · C5 523.25 · D5 587.33 · E5 659.26 · G5 783.99 · A5 880.00 · C6 1046.50 · D6 1174.66 · E6 1318.51 · G6 1567.98 · A6 1760.00 · C7 2093.00 · E7 2637.02 · G7 3135.96. Lou's range: D2 73.42 · E2 82.41 · G2 98.00 · A2 110.00 · C3 130.81 · D3 146.83 · E3 164.81 · G3 196.00.

### 4.4 Recipes

**UI**

1. **Aero tap / bubble pop** (every gel button and bubble tap). Real bubbles "plink" with an upward chirp [U, Minnaert-resonance folklore; tune by ear].
   - L1: sine rising from 700 Hz to **1318.5 Hz (E6)**: `f(t) = 1318.5 − 618.5·e^(−t/0.015)`. Attack 1.5 ms, decay τ 18 ms, length 70 ms.
   - L2: noise click, HP 4 kHz, attack 0, τ 2 ms, at −12 dB relative.
   - Peak **−26 dBFS**. Haptic `CLOCK_TICK`/`VIRTUAL_KEY`.
2. **Select** (choosing an answer card, list item).
   - Glass-bar additive on **G6 1568 Hz**: partials ×1, ×2.756, ×5.404 at 0, −9, −18 dB, with τ 90 / 40 / 18 ms. Attack 1 ms, length 220 ms.
   - Plus L2: a 3 ms noise tick, HP 6 kHz, −14 dB relative.
   - Peak **−24 dBFS**. Haptic `CONTEXT_CLICK` or Composition `PRIMITIVE_TICK` @ 0.5.
3. **Toggle on.**
   - Triangle, f **C6 → G6** (1046.5 → 1568; glide τp 12 ms, reaching the target in ~40 ms). Attack 2 ms, τ 45 ms, length 120 ms. LP 3.5 kHz.
   - Plus a sine octave above at −12 dB (sparkle).
   - Peak −26 dBFS. Haptic `TOGGLE_ON` where available [U API 34], else `CLOCK_TICK`.
4. **Toggle off.** Mirror of toggle on: **G5 → C5** (784 → 523), LP 2.2 kHz, τ 40 ms, peak −28 dBFS (off is quieter and darker). Haptic `TOGGLE_OFF`.
5. **Correct chime** (glassy bell arpeggio, rising 1-3-5-8).
   - Notes **C6, E6, G6, C7** at onsets 0, 55, 110, 165 ms. Velocities 0.8, 0.85, 0.9, 1.0.
   - Each note is an FM bell: `fm = 3.5·fc` (inharmonic, glassy), `I0 = 1.8`, τI 70 ms. Amp attack 1.5 ms, τ 320 ms, hard stop at 900 ms.
   - Add a duplicate at +7 cents and −6 dB for shimmer.
   - Sparkle: noise burst, HP 7 kHz, attack 0, τ 25 ms, at onset 0, −20 dB relative.
   - Total ≈ 1.0 s. Peak **−18 dBFS**. Haptic `CONFIRM` (API 30+), else Composition `PRIMITIVE_CLICK`, then `PRIMITIVE_TICK` after 60 ms.
   - **Neutral variant** (ear-training items): drop the pitched notes. Use L2 sparkle plus the tap's noise click, plus a 120 ms "air" swell (BP 2.5 kHz, Q 0.8). −22 dBFS.
6. **Wrong / try again** (soft, warm, never a buzzer).
   - Triangle, f **E5 → D5** (659 → 587; a gentle step down, glide τp 60 ms starting at 40 ms). Plus a sine an octave below at −10 dB for warmth.
   - LP 1.4 kHz. Attack 12 ms, τ 170 ms, length 380 ms. 4.5 Hz vibrato ±8 cents after 120 ms.
   - Peak **−24 dBFS**. Haptic: Composition `PRIMITIVE_LOW_TICK` ×2 at a 90 ms gap [U API 31]. **Not** `REJECT` (too "error").
   - **Neutral variant:** a 140 ms soft "thup" (noise BP 600 Hz, Q 1.5, attack 8 ms, τ 50 ms) at −26 dBFS.
7. **Lesson complete fanfare** (≤ 1.6 s, hero).
   - (a) Bell arpeggio **C5 E5 G5 C6 E6** at 0/80/160/240/320 ms (bell patch from #5, τ 400 ms).
   - (b) At 400 ms, a sustained chord **C5 E5 G5 C6**: two detuned PolyBLEP saws per note (±6 cents). LP sweeps 800 → 3,200 Hz (τ 150 ms), then decays to 1,500 Hz. Attack 30 ms, sustain 0.7, release τ 300 ms after 900 ms.
   - (c) Glitter: 10 sine pings at random **pentatonic** notes C7–G7 (C D E G A), 25 ms each, spaced 40 ms from 400 ms on, −16 dB relative.
   - (d) Woofer "thump" on the chord: sine 150 → 55 Hz, τp 35 ms, τ 120 ms, −8 dB relative (mostly felt on headphones).
   - Peak **−16 dBFS**. Haptic: Composition `PRIMITIVE_QUICK_RISE` → `PRIMITIVE_THUD` at 400 ms [U API 31].
8. **XP tick** (count-up).
   - One tick per increment, **max 20 per second**. Collapse increments if the counter is faster.
   - Sine plus a 2nd harmonic at −12 dB. Pitch walks **up the major pentatonic** from C6 (C6 D6 E6 G6 A6 C7, then holds C7). Attack 1 ms, τ 9 ms, length 30 ms.
   - Peak −30 dBFS. Finish with the **select** sound on C7.
   - Haptic: `SEGMENT_FREQUENT_TICK` [V exists], or `PRIMITIVE_TICK` @ 0.2 every 3rd tick.
9. **Page swoosh** (screen transition, sheet open).
   - Noise through an SVF band-pass, Q 1.2. Centre sweeps **600 → 3,500 Hz** exponentially over 180 ms (forward); backward/close is **3,500 → 600 Hz**.
   - Amp: attack 70 ms (sine-in), decay τ 60 ms, length 240 ms.
   - Optional sine "whoosh body" 220 → 330 Hz at −14 dB relative.
   - Peak **−30 dBFS**. No haptic. Play it only for user-initiated navigation, not automatic transitions.

**Lou's voice** (formant "babble": a warm, low, mumbly gibberish that sings in key; Animal Crossing-style gibberish is the genre reference [U])

10. **Babble syllables** (Talking).
    - **Source:** a PolyBLEP saw (or pulse, w 0.35, for variety) at f0 chosen per syllable from the **key's scale notes in Lou's range** (C major: E2 82.4, G2 98, A2 110, C3 130.8, D3 146.8). Add a contour per syllable: start ±1 semitone off the target and glide in with τp 25 ms.
    - **Phrase contour:** the first syllable sits on degree 5, and the line drifts down to the tonic by its last syllable (statement). Questions end with a rise of +3–5 semitones on the last syllable.
    - **Syllable length** `uniform(60, 140) ms`, gap `uniform(20, 50) ms`. Every 4–7 syllables, a "word break" of 150–250 ms. Stress on every 2nd–3rd syllable: +2 dB, +1 semitone, length ×1.3.
    - **Envelope:** attack 8 ms, sustain 1.0, release 25 ms. 30 % of syllables get a **plosive onset** (noise, LP 2 kHz, 6 ms, −15 dB relative); 15 % get a **nasal onset** "m" (first 15 ms with F2 muted and F1 at 250 Hz).
    - **Formants:** two parallel SVF band-passes, F1 Q 5 at gain 1.0 and F2 Q 8 at gain 0.5, plus a direct LP 450 Hz "body" path at 0.35. Lou's vowels use male averages (Catford, via https://en.wikipedia.org/wiki/Formant [V]) **scaled × 0.85** so he sounds bigger:
      - /a/ 850, 1610 → **720, 1370**
      - /ɔ/ 500, 700 → **425, 595**
      - /o/ 360, 640 → **306, 544**
      - /u/ 250, 595 → **213, 506**
      - /e/ 390, 2300 → **330, 1955**
    - Weight the vowel choice toward back vowels: a 30 %, ɔ 25 %, o 25 %, u 10 %, e 10 %. Lou is mellow.
    - **Glide** formant centres between syllables with τ 20 ms (that's the "talking" feel).
    - **Saturation:** `tanh(1.6x)`. Optional sub layer: sine at f0, −12 dB relative.
    - **Line length:** syllables = `clamp(chars / 2.5, 3, 12)`, so a line lasts ≤ ~1.4 s. The speech bubble's typewriter reveal advances per syllable.
    - **The same scheduler emits `(startTime, vowel, stress)` events** to the animation layer: mouth shape = vowel, mouth open = amp envelope (smoothed with a 15 ms one-pole). One source of truth, as with Duolingo's viseme timings.
    - Peak **−20 dBFS**.
11. **Happy "bwoop"** (correct / celebrate take-off).
    - Saw source, f0 **G2 → G3** (98 → 196 Hz; exp rise τp 55 ms, so it's mostly there by 150 ms) with a 2-cent overshoot.
    - Vowel morph **/u/ → /a/** (F1 213 → 720, F2 506 → 1370) over the first 120 ms.
    - Attack 10 ms, sustain, release τ 60 ms. Length 260 ms.
    - Plus a woofer thump: sine 160 → 60 Hz, τp 40 ms, τ 70 ms, −6 dB relative.
    - Peak **−18 dBFS**. Transpose the G to degree 5 of the key.
12. **Thinking "hmm"** (closed-mouth hum).
    - Saw f0 **A2 110 Hz** (degree 6 of C, which keeps it "unresolved"; in other keys use degree 2 or 6).
    - Filter: a single BP at **250 Hz, Q 4** plus LP 600 Hz (the nasal "mmm"). No plosive.
    - Contour: +1 semitone at 35 % of the length, then −2 semitones by the end (a pondering arc). 5 Hz vibrato ±15 cents fading in after 150 ms.
    - Attack 40 ms, release τ 120 ms. Length 600 ms.
    - Variant "hm-mm": 220 ms + 80 ms gap + 340 ms, with the second part a whole step lower.
    - Peak **−22 dBFS**.
13. **Surprised "oh!"**
    - Saw f0 **A2 → A3** (110 → 220 Hz) with spring overshoot to 240 Hz at 60 ms, settling on 220 by 120 ms.
    - Vowel /o/ (306, 544), widening slightly to /ɔ/. Attack 4 ms, τ 90 ms. Length 200 ms.
    - Woofer pop: sine 180 → 80 Hz, τp 20 ms, τ 40 ms, −6 dB relative.
    - Peak **−18 dBFS**.
14. **Yawn** (Sleepy).
    - Saw f0 **D3 146.8 → G3 196** (rise over the first 30 %, τp 200 ms), then slide down to **D2 73.4** by the end.
    - Vowel morph **/a/ (720, 1370) → /ɔ/ → /o/ → /u/** across the length.
    - Breath noise: BP 1.2 kHz, Q 0.7, at −14 dB relative, growing to −8 dB in the last 400 ms ("haaah").
    - Amp: attack 300 ms (sine-in), sustain, release τ 350 ms. Length **1.4 s**.
    - Peak **−24 dBFS** (quiet).
15. **"hm?" encourage** (on a miss, optional). The Thinking patch at 60 % length with a **rising** end contour (+3 semitones). Peak −24 dBFS. Skip it if the miss tone already played within 300 ms; in that case just animate.

**Haptic map** (from the plan §4.5, adjusted):

| Event | Haptic |
|---|---|
| Tap | `CLOCK_TICK` |
| Toggle | `TOGGLE_ON`/`TOGGLE_OFF` (fallback `CLOCK_TICK`) |
| Check pressed | `CONFIRM` only if correct, otherwise nothing |
| Correct | Click + tick |
| Miss | Two low ticks |
| Lou landing | `PRIMITIVE_THUD` @ 0.6 |
| Lesson complete | Rise → thud |

The API levels for `TOGGLE_*` and the 31+ primitives are [U]; guard them with `Vibrator.arePrimitivesSupported()`.

---

## 5. Build checklist (numbered, actionable)

1. Add `res/font/open_sans_var.ttf` from the pinned google/fonts URL (§2.3). Add `OFL.txt` to licences. Delete Fredoka and Nunito. Wire up the `Aero` and `AeroCondensed` families and the type scale in `ui/theme/Theme.kt`.
2. Replace the §4.6 colour tokens with the Aero Sky and Aero Night tables (§1.3). Remove Groove Violet as the brand accent; `aeroBlue` takes over. Leaf replaces Mint for "correct" in Learn, and `nightMint` replaces it in dark.
3. Build `Modifier.gel()`, `GlassPanel`, `AeroSkyBackground` (gradient + glow + bokeh + bubbles) and `AeroNightBackground` per §1.4–1.6, with a reduced-motion switch.
4. Keep a contrast script in `tools/` (the WCAG formula from §1.3) and run it on every token change. Every pair listed in §1.3 must pass.
5. Build `LouRig` (the Canvas layers in §3.2), `LouPose`, and `LouController` (the state machine in §3.4, priorities, dwell times). Rig and states first; polish timing in a second pass (Duolingo's "rough passes first").
6. Extend `AudioEngine` with an SFX voice type driven by a data `Sfx` patch (§4.3), a Lou-voice voice (saw + 2 SVF formants + scheduler emitting viseme events), and a gate in `drainCommands()` implementing §4.2 rules 1–5.
7. Encode recipes 1–15 as data. Transpose by the lesson key. Add the "neutral" variants and choose them automatically on ear-training items.
8. Add settings: UI sounds, Lou's voice, Character visibility (Full / Off), Reduced motion (follows the system by default).
9. Device QA: listen on a phone speaker (Lou must be audible), on earbuds (no harsh highs > −24 dBFS) and in a silent room; check that the 60 fps draw has no recomposition per frame (Layout Inspector), and that contrast holds at 200 % font scale.

---

## 6. Sources

Each was fetched today with `curl` (WebSearch and WebFetch were unavailable).

1. Frutiger Aero (definition, CARI/Sofi Xian 2018, motifs, exemplars, Liquid Glass): https://en.wikipedia.org/wiki/Frutiger_Aero
2. CARI aesthetic page (JS-only; existence checked): https://cari.institute/aesthetics/frutiger-aero
3. Windows Aero (acronym, glass/translucency, Segoe UI, queued notifications): https://en.wikipedia.org/wiki/Windows_Aero
4. Aqua (water theme, "lickable", blue/white/gray, Lucida Grande): https://en.wikipedia.org/wiki/Aqua_(user_interface)
5. Segoe (Steve Matteson, humanist, Frutiger dispute): https://en.wikipedia.org/wiki/Segoe
6. "The Sounds of Windows Vista" (rounded/translucent sounds, glassy melodies): https://web.archive.org/web/20061110152317/http://windowsvistablog.com/blogs/windowsvista/archive/2006/11/09/the-sounds-of-windows-vista.aspx
7. Open Sans metadata, description and files: https://github.com/google/fonts/tree/main/ofl/opensans ; upstream statics: https://github.com/googlefonts/opensans/tree/main/fonts/ttf
8. Selawik (OFL, RFN, release zip, known issues): https://github.com/microsoft/Selawik
9. Hind / Signika / Istok Web / Mukta / Source Sans 3 / PT Sans / Fira Sans metadata: https://github.com/google/fonts/tree/main/ofl/
10. Duolingo, "How Duolingo Animates Its World Characters" (Rive state machines, visemes, idle layers): https://blog.duolingo.com/world-character-visemes/
11. Duolingo, streak milestone animation (timing passes): https://blog.duolingo.com/streak-milestone-design-animation/
12. Duolingo, intern 3D animation case study (rigging context): https://blog.duolingo.com/duolingo-art-intern-animation-case-study/
13. Twelve basic principles of animation: https://en.wikipedia.org/wiki/Twelve_basic_principles_of_animation
14. Blinking (100–150 ms; 2–10 s intervals; reduced rate when focused): https://en.wikipedia.org/wiki/Blinking
15. Formant (average male vowel formants, Catford 2001): https://en.wikipedia.org/wiki/Formant
16. Material Design, "Applying sound to UI" (archived): https://web.archive.org/web/20190923132432/https://material.io/design/sound/applying-sound-to-ui.html
17. Material Design, "About sound" (archived): https://web.archive.org/web/20191118144921/https://material.io/design/sound/about-sound.html
18. Android haptics design principles: https://developer.android.com/develop/ui/views/haptics/haptics-principles
19. `HapticFeedbackConstants` reference: https://developer.android.com/reference/android/view/HapticFeedbackConstants
20. Android audio focus: https://developer.android.com/media/optimize/audio-focus
21. Compose Brush/gradients: https://developer.android.com/develop/ui/compose/graphics/draw/brush
22. Compose shadows (`dropShadow`, `innerShadow`): https://developer.android.com/develop/ui/compose/graphics/draw/shadows
23. Compose blur (Android 12+ only): https://developer.android.com/develop/ui/compose/graphics/images/customize
24. Haze (backdrop blur/glass for Compose, Apache-2.0): https://github.com/chrisbanes/haze
25. Game feel (Swink's six areas, polish): https://en.wikipedia.org/wiki/Game_feel
26. WCAG 2.1 contrast minimum: https://www.w3.org/WAI/WCAG21/Understanding/contrast-minimum.html
