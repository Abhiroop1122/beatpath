# FL Studio UI reference (for the Studio screens)

Observed from Image-Line's official manual screenshots on 26 Nov 2026 (FL Studio 2024/2025 default theme):
- Channel Rack: https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/channelrack.htm (`img_shot/channelrack_main.png`, `channelrack_loopstarter_pianoroll.png`)
- Piano roll: https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/pianoroll.htm (`img_shot/pianoroll_general.png`)
- Transport: https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/toolbar_panels.htm (`img_shot/panel_transport.png`)

Colors below are approximations from those screenshots. We redraw every control ourselves in Compose; no Image-Line bitmaps, icons or logos ship in the app.

## Window chrome
- Window frame: slate grey `#4E565E`, title bar a little darker `#434A51`. The title is white-grey text with a speaker/"play" glyph before it ("Channel rack", "Piano roll - FLEX").
- Inner panel: `#5B636B` for the Channel Rack body; the step area sits on the frame grey.
- Header controls on the right: swing knob, a step-count box ("16"), a curved undo arrow, a graph icon and a yellow "loop" icon, then ×.

## Channel Rack row (left → right)
1. **Mute LED**: a small round lime-green light (`#9BE34B`, glowing) that is grey when muted. (A padlock replaces it when the channel is locked.)
2. **Pan knob** and **volume knob**: small dark knobs (`#2E3338` with a lighter rim) with a short white indicator line.
3. **Mixer track box**: a rounded rectangle with a large number (1, 2, 3…), dark background, with tiny ▲▼ arrows on its right edge.
4. **Channel button**: a wide rounded rectangle carrying the channel name in white, centred, with a small instrument icon on the right. It takes the channel's color: grey `#626B73` by default, mauve `#7B5A6E` in some templates, and red `#B5454A` when the plugin is missing.
5. **Selector**: a thin vertical bar. Selected channels get a lime-green outline and fill (`#B5F05A`).
6. **Step buttons**: 16 tall rounded rectangles (about 0.65:1 width to height) in groups of 4 that alternate **dark grey** (`#3F454B`) and **dark maroon** (`#5C4044`). Each has a glossy vertical gradient (lighter top) and a 1 px dark outline.
   - **Lit steps**: silver/ice blue (`#D7E3EA` → `#A9BCC8`) in the grey groups, pink (`#F2C1C4` → `#D9959B`) in the maroon groups.
   - Lit steps also show a small red/coral line near the top, like an LED.
7. **Instrument channels with notes** show a mini Piano roll preview instead of steps: a dark field with light green note bars.

Below the rows there's a "+" for adding channels. You drag the right edge to change pattern length. The pattern selector is a rounded box showing the pattern name with ▲▼ and a "+".

## Piano roll
- **Toolbar**, left to right: ▸ menu, wrench, magnet (snap), music note, stamp, undo; then tool icons: pencil (Draw, yellow), brush (Paint), Paint-and-drag, ⊘ (Delete), speaker-x (Mute), slice, select (dashed box), zoom (magnifier), playback (speaker). Then the target channel name.
- **Keyboard** on the left: realistic white keys with a soft grey gradient and black keys with a glossy dark gradient. Every C is labelled (C5, C6). The top-left corner shows the current note color (light green slant).
- **Grid**: blue-slate lanes `#3A4852`, with rows for black keys a little darker `#33404A`. Faint lines mark 16ths, stronger lines beats, the strongest bars. Bar numbers are in the ruler at the top (`#2B353D`).
- **Notes**: pale mint green (`#B7EDB4` top → `#93D990` bottom), rounded about 2 px, with a darker green bottom border and the note name inside in dark green (`#2F5E33`) at the left.
  - A selected note, or a note in another color, shows as light blue (`#AFC6F2`).
  - Ghost notes (other channels) are flat grey-blue blocks (`#5A6A75`), about 60% opacity.
- **Event editor** below a splitter: "Control ▸ Velocity". Each note's velocity is a vertical green stem with a circle head and a short horizontal tail.

## Transport panel (main toolbar)
- **PAT/SONG**: a stacked two-line toggle. The active half is lime green (`#A9E34B`) with dark text; the inactive half is grey text on dark.
- **Play ▶ and Stop ■**: square buttons in a dark rounded segment (`#3A4147`) with light glyphs.
- **Record**: round, dark, with a red dot (`#E5484D`).
- **Tempo**: an LCD-style box, light grey-blue background (`#D9E2E8`) with dark digits ("130.000") and ▲▼ on the right.

## Mobile translation rules
- Keep FL's shapes, colors and order. Enlarge touch targets: rows at least 48 dp, steps at least 40 dp wide in landscape and 8 per page in portrait.
- Knobs stay visual. Tap a knob to open a large slider sheet, since tiny drag knobs are unusable on a phone.
- Keep the right-click conventions in the "Where is this in FL?" sheets, because a phone uses tap and long-press instead.
