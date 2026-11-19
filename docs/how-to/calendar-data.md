# Offline calendars and holiday updates

Calendar keeps published holidays in the TV's private app storage. Its target is
the entire current year and following year for each enabled source and scope;
changing the widget's visible range does not remove stored annual data.
The bundled snapshot gives an immediate offline starting point. Successful source
updates persist through app restarts and upgrades without replacing the APK.

## Check coverage or refresh

Open Calendar with short OK, then choose **Sources & offline data**. The same
screen is available under **Configure calendar → Sources & offline data**.

It shows the two required years, missing scopes, the latest successful source
check and any failure. **Refresh holiday data** checks the enabled sources now.
The screen updates while the check runs; repeated requests share the running
update. Back returns to Calendar. Back again or **Restore dashboard** returns to
the original dashboard page, Calendar focus and size.

Long source details scroll with the remote/touch; a wedge indicates additional
content, with no scrollbar. Widget headers keep today's date first. Thai uses
Buddhist Era years in dates and update timestamps.

## Automatic maintenance

Android schedules an hourly network-constrained wake-up. The repository checks
annual sources only when their daily check is due, with catch-up on app resume.
Thai special-closure announcement checks target 06:00 and 18:00 Bangkok time.
Standby/background scheduling is inexact; this is not an immediate closure-alert
service. Next-next-year publications are sought during October–December.

Failures retry with backoff from one hour to a day. Each provider updates
independently. A validated new revision replaces the active file atomically and
retains the previous provider snapshot for recovery. Empty, malformed, wrong-year
or unexpectedly reduced publications cannot erase known dates. Older years are
pruned only when replacement coverage for both required years is complete.

No account, companion server or phone application is needed. The phone dashboard
editor continues to configure the same TV; it does not maintain another calendar.

## Local Wan Phra and flower work

Wan Phra uses a Java Thai civil lunisolar calculator on the TV. It works offline
and supplies the flower preparation/selling anchors as well. Source details show
the supported interval and calculator revision; day details include lunar month,
waxing/waning day and second month 8 when applicable.

The independently checked observance interval is **2024–2027**. Flower blocks
requiring an anchor beyond that interval are marked incomplete rather than
silently guessed. The calculator's wider arithmetic was compared against the
pinned reference for every day of 1900–2100; that broader test is not a claim of
independent published-calendar validation for every year.

## Coverage and source limits

- The seed contains Thai government dates for 2026; future annual coverage is
  shown as missing until a recognized official publication is retrieved.
- Spanish national/Catalunya dates for 2026–2027 are bundled. Tivissa-centre local
  dates are verified for 2026; the checked official dataset had no 2027 entries.
  Tarragona province amendments remain separately incomplete.
- US federal actual/observed dates and China's holiday/working-day arrangements
  have 2026–2027 offline seeds. Inauguration Day's DC-area-only exception is not
  treated as a nationwide federal holiday.

The updater uses OPM, BOE, Catalunya's official local dataset, official Chinese
annual notices and Thai government sources. Publisher availability and document
formats vary. An unrecognized Thai closure/cancellation or unclear jurisdiction
requires review and reports attention; it never becomes a guessed nationwide
closure. Source/parser changes may need an app update, while ordinary recognized
data updates do not. Missing data never means an office is open.

See the [implementation plan](../plans/2026-11-19-offline-holiday-updates.md) and
[verification record](../testing/2026-11-19-calendar-data.md).
