# Calendar widget

Implemented and checked on Cast1 in **0.1.21**. See the
[validation record](../testing/2026-11-16-calendar-and-grid.md).

Add it with **Home settings → Widgets & restore → Add widget → Calendar**.
Adding it again focuses the existing Calendar. It contains only dates with
matching upcoming events, sorted by date. Events on the same date share one
card; flower/Buddha icons and flag-colored edges identify their meaning/origin.

Hold OK on the widget, choose **Configure calendar → Number of days**, then
choose **7 days**, **30 days** or **Custom…**. The number includes today and applies
to every enabled holiday source. Custom accepts a positive whole number up to
2147483647; malformed or overflowing input is rejected. Save commits one change;
Cancel/Back leaves the previous value. Home's Undo restores the previous settings.

**Flower and Buddhist weeks** selects This week, Next week, or This week + next
week independently for flower work and Wan Phra. Weeks start Monday. Only today
and future dates are displayed, including preparation/selling days whose Wan
Phra anchor falls beyond the selected week. Monday is shown as fixed in settings;
a changeable week start remains TODO. Changing the holiday day count leaves
these week settings unchanged.

**Sources and scope** enables/disables Thai government, flower work, Wan Phra,
Spanish, US federal and mainland China dates. The Spanish settings include
national/Catalunya/Tarragona-province/Tivissa scope. Tarragona city holidays are
not included as Tivissa holidays. No unverified province-wide dates are invented.
China make-up working days are a separate optional toggle, initially Off.

Press OK for **All matching dates**, **Next page**, **Previous page**, or Configure.
The date picker includes every matching date; it is not capped at six. Resize
changes the visible card count and pages instead of dropping events. Calendar
has a minimum 2×2 grid size. Move/Delete/Undo work like other dashboard widgets.

## Current data limits

This implementation reads **reviewed offline provider JSON**. It does not yet
perform live emergency-closure checks or run the validated Thai lunar calculator.
Those remain explicit work in the [plan](../plans/2026-11-16-thai-calendar-widgets.md).
Do not rely on this offline snapshot alone for an office visit.

- Thai government baseline: 2026, including separately scoped Bangkok closures.
- Flower/Wan Phra anchors: reviewed 2026 dates through 24 December, with derived
  work blocks. Dates beyond that coverage remain unknown.
- Spanish national/regional and Tivissa-centre local dates: 2026. No 2027 local
  dates or other Tivissa population centres are inferred.
- US federal actual/observed dates: 2026–2027, including the 31 December 2027
  observed New Year date from the adjoining OPM schedule.
- Mainland China holiday periods and separate make-up work days: 2026–2027.

The widget displays Offline dates and incomplete coverage, including on paged
views. A larger requested range is retained, with the data gap shown; it is not
silently shortened. Incomplete or empty results never mean that offices are open.

## JSON handoffs

The independent container commands are:

```sh
task calendar:normalize
task calendar:assemble
task calendar:check
```

`dev/calendar/reviewed/*.json` contains reviewed facts and source links.
Normalize expands periods and flower anchors into
`dev/calendar/providers/*-calendar.json`. Assemble validates those files and
atomically writes `launcher/app/src/main/assets/calendar-snapshot.json`, which
the native Java widget consumes. Input hashes make the handoffs replayable.
Python is a build-container tool only; the APK has no Python dependency.
The live fetch/extract/reconciliation stages remain pending; these commands
never mark the offline snapshot as freshly checked official data.

English/Thai resources are included. Machine-assisted Thai copy has not received
an independent human linguistic signoff; structural resource parity is checked.
