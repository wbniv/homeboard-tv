# World clock

Add **Home settings → Widgets / Restore → Add widget → World clock**. Each new
instance starts with **Local · Follow TV**, independently of other clock widgets.
Small panels show the primary clock; wider/taller panels show more places. A
count indicates configured places beyond the compact panel's capacity.

**Short OK/click opens full screen. Hold OK opens Configure, Move, Resize and
Remove.** Adding a clock preserves the existing dashboard.

## Settings

Configure world clock → Places offers offline city/code/zone search, add/remove,
labels, primary/reference choices and reorder controls. The Studio cities preset
adds Bangkok, Seattle, Denver, Munich and Barcelona. Tivissa uses Europe/Madrid;
its own label and coordinates stay separate from Barcelona's.

Each instance supports up to twelve places. A zone-only selection has no guessed
map position. Primary controls the smallest panel and initial expanded selection.
Reference controls relative dates and differences; default reference follows the
TV system zone. Display offers a title, Manual or West → east ordering, 24-hour
(default), 12-hour or device format, and optional full-screen seconds.

Changes stay in a draft until **Save changes**. Back cancels; normal Undo restores
a saved change. Removing the widget does not remove any other clock instance.

In **Configure from phone**, select the World clock card. Add/search places,
edit labels, choose primary/reference and drag the place grips to reorder.
Alt+Up/Down and explicit movement buttons are also available. A move selects
Manual ordering; draft Undo restores the previous order and ordering mode.
Use **Update TV** to apply the complete draft. Phone clock previews identify the
TV clock snapshot and use its time zone, independently of the phone's zone.

## Full screen

Highlight a place for its full local date/time, zone, UTC offset, reference
difference, daylight-saving status and next offset change. OK opens full details.

**Compare time** starts at the next half hour. Left/Right on the timeline moves
one shared instant by 30 elapsed minutes. Up/Down selects a place; the interval
applies to every row. Duration cycles 15/30/60/90 minutes. Previous/Next pages the
24-hour window; Previous stops at the live window. Past starts are unavailable.
An expired selection visibly advances to the next half hour. **Now** returns live.

**Map** shows the bundled world outline with matching place pins/time badges.
Up/Down selects a place, updating details; pointer selection uses its pin.
Missing-coordinate entries remain available in clocks and comparison. **Clocks**
returns to the clock grid. There is no map network service or geolocation request.

Back leaves comparison first, then nested detail/full screen. **Restore dashboard**
returns directly to the original page, widget focus and saved size. Browsing adds
no Undo entries. Shared city colors link cards, map and timeline. Sun/moon and
strip shading mean **06:00–18:00 local clock hours**, not sunrise/sunset or weather.

All clocks use the same sampled instant and the TV's named time-zone rules. Follow
TV means its system zone, not a phone location. No account, time API or location
permission is needed. Accuracy depends on the system clock and installed rules;
unknown saved zones stay labeled unavailable until explicitly changed.

See the [plan and mockups](../plans/2026-11-17-world-clock-widget.md).
