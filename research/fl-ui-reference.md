# FL Studio UI reference (for the Studio screens)

Observed on 26 Nov 2026 from screenshots in Image-Line's official manual (FL Studio 2024/2025 default theme):
- Channel Rack: https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/channelrack.htm (`img_shot/channelrack_main.png`, `channelrack_loopstarter_pianoroll.png`)
- Piano roll: https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/pianoroll.htm (`img_shot/pianoroll_general.png`)
- Transport: https://www.image-line.com/fl-studio-learning/fl-studio-online-manual/html/toolbar_panels.htm (`img_shot/panel_transport.png`)

The colors below are approximated from those screenshots. We redraw every control ourselves in Compose, and the app ships no Image-Line bitmaps, icons or logos.

## Window chrome
- The window frame is slate grey `#4E565E`, and the title bar is a little darker at `#434A51`. The title is white-grey text preceded by a speaker/"play" glyph ("Channel rack", "Piano roll - FLEX").
- The Channel Rack body is an inner panel in `#5B636B`; the step area sits on the frame grey.
- On the right of the header are a swing knob, a step-count box ("16"), a curved undo arrow, a graph icon and a yellow "loop" icon, followed by ×.

## Channel Rack row, left to right
1. Mute LED: a small, glowing, round lime-green light (`#9BE34B`) that turns grey when muted. A padlock replaces it when the channel is locked.
2. Pan knob and volume knob: small dark knobs (`#2E3338` with a lighter rim) with a short white indicator line.
3. Mixer track box: a rounded rectangle on a dark background showing a large number (1, 2, 3…), with tiny ▲▼ arrows on its right edge.
4. Channel button: a wide rounded rectangle with the channel name centred in white and a small instrument icon on the right. It takes the channel's color, which is grey `#626B73` by default, mauve `#7B5A6E` in some templates, and red `#B5454A` when the plugin is missing.
5. Selector: a thin vertical bar. Selected channels get a lime-green outline and fill (`#B5F05A`).
6. Step buttons: 16 tall rounded rectangles (about 0.65:1 width to height) in groups of 4, alternating dark grey (`#3F454B`) and dark maroon (`#5C4044`). Each has a glossy vertical gradient that is lighter at the top, and a 1 px dark outline.
   - Lit steps are silver/ice blue (`#D7E3EA` to `#A9BCC8`) in the grey groups and pink (`#F2C1C4` to `#D9959B`) in the maroon groups.
   - Lit steps also show a small red/coral line near the top, like an LED.
7. Instrument channels that have notes show a mini Piano roll preview in place of steps: a dark field with light green note bars.

Below the rows is a "+" for adding channels. Dragging the right edge changes the pattern length. The pattern selector is a rounded box that shows the pattern name, with ▲▼ and a "+".

## Piano roll
- Toolbar, left to right: ▸ menu, wrench, magnet (snap), music note, stamp and undo, then the tool icons: pencil (Draw, yellow), brush (Paint), Paint-and-drag, ⊘ (Delete), speaker-x (Mute), slice, select (dashed box), zoom (magnifier) and playback (speaker). The target channel name comes last.
- The keyboard on the left has realistic white keys with a soft grey gradient and black keys with a glossy dark gradient. Every C is labelled (C5, C6). The top-left corner shows the current note color as a light green slant.
- The grid has blue-slate lanes in `#3A4852`, with the rows for black keys a little darker at `#33404A`. Faint lines mark 16ths, stronger lines mark beats, and the strongest mark bars. Bar numbers sit in the ruler at the top (`#2B353D`).
- Notes are pale mint green (`#B7EDB4` at the top to `#93D990` at the bottom), rounded by about 2 px, with a darker green bottom border and the note name in dark green (`#2F5E33`) inside at the left.
  - A selected note, or a note in another color, appears light blue (`#AFC6F2`).
  - Ghost notes from other channels are flat grey-blue blocks (`#5A6A75`) at about 60% opacity.
- The event editor sits below a splitter, set to "Control ▸ Velocity". Each note's velocity is a vertical green stem with a circle head and a short horizontal tail.

## Transport panel (main toolbar)
- PAT/SONG is a stacked two-line toggle. The active half is lime green (`#A9E34B`) with dark text; the inactive half has grey text on dark.
- Play ▶ and Stop ■ are square buttons with light glyphs in a dark rounded segment (`#3A4147`).
- Record is a round, dark button with a red dot (`#E5484D`).
- Tempo is an LCD-style box with a light grey-blue background (`#D9E2E8`), dark digits ("130.000") and ▲▼ on the right.

## Mobile translation rules
- Keep FL's shapes, colors and order, but enlarge the touch targets: rows at least 48 dp tall, and steps at least 40 dp wide in landscape, with 8 per page in portrait.
- Knobs stay as visuals. Tapping a knob opens a sheet with a large slider, because tiny drag knobs are unusable on a phone.
- Keep the right-click conventions in the "Where is this in FL?" sheets, since on a phone the app uses tap and long-press instead.
