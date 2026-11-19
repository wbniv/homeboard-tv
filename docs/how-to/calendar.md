# Calendar widget

Implemented and checked on Cast1 in **0.1.21**. See the
[validation record](../testing/2026-11-16-calendar-and-grid.md).
One-row resizing and the compact date strip are verified on Cast1 in **0.1.27**;
see the [resize validation](../testing/2026-11-17-calendar-one-row.md).

Add it with **Home settings → Widgets & restore → Add widget → Calendar**.
Adding it again focuses the existing Calendar. It contains only dates with
matching upcoming events, sorted by date. Events on the same date share one
card; flower/Buddha icons and flag-colored edges identify their meaning/origin.
Date cards show the localized weekday above the large day number, on both the
dashboard and the full-screen calendar. Full dates remain available in day details.
Card backgrounds contrast with the surrounding panel in light and dark palettes;
their fills still follow the widget-background opacity setting.

The widget header begins with **Today · weekday day month year**, using the TV's
local date. Thai uses Buddhist Era years. Today stays first when a narrow widget
truncates the following holiday/lookahead text, and refreshes across midnight.

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

Press OK to open **four complete weeks with highlighted-day details**. Arrows
select a date; OK opens its full details. Back restores the dashboard. The expanded
window includes ordinary days and looks up events independently of compact ranges,
while retaining enabled sources/scopes. Hold OK on Home for configuration and size.
See [full-screen controls](widgets-full-screen.md). Resize changes the visible
compact card count and pages instead of dropping events. Calendar
has a minimum **2 columns × 1 row** grid size. Short widgets show a compact strip
with square dates, icons, flag bands, paging and status. Full event details remain
available through OK. Move/Delete/Undo work like other dashboard widgets.

## Current data limits

Calendar calculates Wan Phra/flower blocks locally and reads published holidays
from a persistent, automatically refreshed offline cache. See
[offline data, refresh controls and source limits](calendar-data.md).

- Thai government baseline: 2026, including separately scoped Bangkok closures.
- Flower/Wan Phra anchors: independently checked civil-calendar calculations for
  2024–2027. Dates outside the validated interval report incomplete coverage.
- Spanish national/regional dates: 2026–2027. Tivissa-centre local dates: 2026.
  No unpublished local dates or other Tivissa population centres are inferred.
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
Normalize expands published holiday periods into
`dev/calendar/providers/*-calendar.json`. Assemble validates those files and
atomically writes `launcher/app/src/main/assets/calendar-snapshot.json`, the
initial offline seed. Input hashes make the handoffs replayable. Buddhist/flower
event arrays are no longer bundled; the TV derives them for each viewing window.
Python is a build-container tool only; the APK has no Python dependency.
The runtime Java fetch/extract/reconciliation modules update private provider
revisions. `task calendar:replay -- ...` can replay captured publications or run
the same network updater in the build container. Generating the bundled asset
never marks it as a freshly checked official source.

English/Thai resources are included. Machine-assisted Thai copy has not received
an independent human linguistic signoff; structural resource parity is checked.
