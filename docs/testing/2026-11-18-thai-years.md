# Thai years, time metadata and 65% widget background defaults

Implementation follows the [approved plan](../plans/2026-11-18-thai-years-and-time-presentation.md).
The combined target is **0.1.61**, including the subsequent 65% default request.
The earlier 0.1.60 was frozen before that request; its Cast1 background install
was declined with `install-over-foreground`, rather than interrupting the screen.
Neither that refusal nor an accepted queue entry is installation proof.

## Behavior and data boundary

Effective Thai interface language uses Buddhist Era years; English uses CE.
Calendar month/date/coverage/flower-range labels, World clock details and daylight
saving transition dates, Air detail timestamps, Favorites update dates and Rain
display formatting share the native chronology policy. The phone World clock
preview explicitly chooses its calendar independently of the phone's locale.

Stored ISO dates, API request/parser formats, instants, named zones and calculations
are unchanged. Metadata timestamps use ⏲; age and meeting durations use ⏱ with
localized units. Clock faces, city clocks, hour axes and date headings remain
undecorated. Signed UTC offsets remain offsets. Existing Calendar entry backgrounds
are retained following Will's clarification.

One shared constant defines **65%** for first-run/missing values, initial widget
fills and the opacity Reset action/label. Explicit stored values stay unchanged.
The separate wallpaper dimming and pie-control defaults remain 35% and 95%.

## Container and browser verification

The combined container run passed **478 JVM tests**, zero failures, and Android
lint with zero errors. Translation parity covers **636 English/Thai resources**.
JVM tests cover BE/CE switching, language overriding a conflicting calendar locale
extension, New Year in Bangkok versus Seattle, leap day, unchanged ISO values,
duration thresholds/negative age, clean clock values and signed offsets. Opacity
regressions check the new default/Reset and preservation of explicit 75%.

`task phone:check` passed model tests and Chromium/WebKit UI suites. The final
World clock component checks additionally passed BE/CE switching, retained stored
settings, TV-zone snapshots and supporting timestamp icons. Screenshots were
regenerated after scrolling to the top and waiting for painting.

| Browser fixture | Thai BE | English CE |
|---|---|---|
| Chromium | [Screenshot](2026-11-18-thai-years/phone-chromium-be.png) | [Screenshot](2026-11-18-thai-years/phone-chromium-ce.png) |
| WebKit | [Screenshot](2026-11-18-thai-years/phone-webkit-be.png) | [Screenshot](2026-11-18-thai-years/phone-webkit-ce.png) |

These are real browser renders of the shipped component with controlled fixture
state, not a physical paired-phone session. The literal `<b>Family</b>` label is
an intentional escaping test; it must appear as text rather than HTML.

## Native screenshot verification

`dev/date-presentation-proof.py` drives sequential complete coordinator jobs.
Debug activities use isolated widget/cache preferences and a forced effective
locale context, without changing the saved app/system language. Each report checks
BE/CE output, localized duration/ago units, clean clock values, 65% fresh defaults
and unchanged household preferences/language. The runner also checks receipt
success and the exact screen and APK version, preventing an old `wf.log` from
being mistaken for a new result.

The Air detail screenshot uses a labeled synthetic station with a measurement
65 minutes before the test. This exercises production date/age rendering; it is
not evidence of a real station reading or network success. Favorites inspects the
installed Homeboard package's update date. Restore acceptance clicks the visible
Restore dashboard control and waits for its dismiss callback before checking focus.

The first native job, **J‑ca2ff6542036**, verified installation of 0.1.61 on
Cast1 by APK hash, then failed with `target-left-foreground` before writing a
native report or screenshot. Cleanup restored MainActivity safely. This is
**installation proof only**: native BE/CE, Restore and visual acceptance remain
unverified. The probe failure is being diagnosed; no native screenshot pass is
claimed. The retained [deployment evidence summary](2026-11-18-thai-years/deployment.json)
separates installation from acceptance.

The earlier Cast1 background job **J‑c87a6cf91c00** declined installation with
`install-over-foreground`. Cast2 job **J‑8b2978c6f567** remains queued because
coordinator control is unavailable. It is not a confirmed Cast2 installation.
Cast1 is the routine foreground target. Cast2 installation and foreground outcomes
are recorded separately; unreachable/personal-use deferrals are never counted as
successful installs or screenshot acceptance.

## Follow-up on 0.1.62

After returning to the Chromecast network, Cast1 installed 0.1.62 and the Thai
dashboard and expanded Calendar probes passed with version-matched reports and
reviewed screenshots. These verify BE **2569**, Thai weekdays, localized duration
assertions and unchanged household preferences/language. See the
[Calendar verification record](2026-11-18-calendar-weekdays-and-contrast.md).
The earlier 0.1.61 failure remains historical; the other native clock, air,
Favorites and Restore screenshot cases remain open.
