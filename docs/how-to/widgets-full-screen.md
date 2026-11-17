# Full-screen widgets

**Short OK/click opens a widget full screen. Hold OK opens its settings/actions.**
Back closes a nested detail first, then restores the original dashboard page,
widget focus and saved size. The visible **Restore dashboard** button closes the
expanded view directly. Expanding and browsing never resize the Home widget or
add Undo entries. Configure is available in the expanded header.

## Calendar

The expanded Calendar shows four complete Monday–Sunday weeks and a detail pane
for the highlighted day. Arrows move the highlighted date; details update immediately.
OK opens longer details. Previous/Next browse four-week blocks; Today returns to
the current week. All dates shows the events in the displayed window.

The expanded window uses every enabled source across the displayed dates, even
when the compact widget is configured for seven days or This week. Source/scope
toggles still apply. Empty and unavailable coverage remain distinct, and offline
dates remain labeled. The saved compact ranges stay unchanged.

## Rain

Full screen opens a seven-day overview and the selected day's hourly chart/details.
Choose **24 hours**, **48 hours**, **7 days** or **All available** to browse the
returned forecast without changing the Home widget's ranges. The first day can be
partial; missing intervals are unavailable, not dry.

Bar height represents forecast rain amount. The aligned strip beneath it represents
precipitation probability for the same hour. Arrows select an hour and immediately
show exact start/end, amount, probability and source. OK drills into detail.
Probability does not scale or fade amount bars. The total, peak hourly probability
and wettest complete three-hour block are labeled separately.

## Air quality

**Nearby map** shows monitoring stations with PM2.5 values and a separate device
location marker. Accuracy and location age are shown when available; an old fix is
labeled last-known. A station/model location is never substituted for the device.

Choose a radius, Recenter, station marker or nearby station row. Highlighting a
station updates its measurement, distance, timestamp, source and measurement basis.
Pan map enables remote panning; Back exits that mode first. Zoom buttons change
scale. Overlapping markers show a station count; the list exposes individual stations.
AirBKK coverage is regional: a map outside that coverage can show no supported
stations. Tile, location and feed availability are separate states.

**History** opens the selected station's data. A separately labeled model button
opens configured model history when applicable. Choose 24 hours, 7 days or 30 days;
arrows select exact samples, and Day shows available daily min/max and sample counts.
OK opens the full values list. Actual sample coverage and gaps stay visible.
Map/history browsing does not change the compact widget's source or location.

## Favorites

Focus the Favorites header/container and press OK to open its full-screen browser.
On Home, Back from the apps reaches the header. Selecting an individual app with
OK still launches that app.

The expanded browser uses banners with a right-hand panel showing the highlighted
app's full name, version, update date and actions. This presentation is independent
of the compact icon/banner setting. At normal TV text size, its four-column grid
has room for four visible rows (16 apps); larger text reduces the density. Additional
apps scroll within the grid. From Restore dashboard, press Down to enter the apps.
App info opens Android's app page; Remove from
favorites keeps the app installed. Reorder uses explicit Move left/Move right and
Done/Back controls, with normal Undo after saving.

Returning from an app retains selection/scroll during the session. Missing apps
or metadata are labeled unavailable. Empty Favorites still has the restore action.

## Adding a widget type

Register a type in `WidgetKind` and implement its compact renderer and expanded
content factory in MainActivity. The picker uses that same registration; the
exhaustive expanded factory must handle every type. Expanded content supplies its
view, transient Bundle state and lifecycle cleanup through `WidgetExpansion.Content`.
Use the shared restore/detail stack. Keep fetching/caching owned by the existing
controller; avoid duplicate requests while two presentations reference the data.

Specify the richer information/timeframes, empty/errors and restoration tests in
the widget plan and mockups before implementation. Run all checks in the build
container and validate real TV input through the coordinator. See the
[full-screen plan](../plans/2026-11-17-widget-full-screen.md).
