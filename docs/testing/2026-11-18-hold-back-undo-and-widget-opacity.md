# TV Hold Back Undo and widget-background opacity

The [Hold Back plan](../plans/2026-11-17-hold-back-undo.md) and
[widget opacity correction](../plans/2026-11-18-widget-background-opacity.md)
are implemented together. Version 0.1.53 is the first combined deployment.

The pure gesture tests exercise the one-second boundary, short Back, orphan
release, one undo per physical press, repeat events, fresh presses, cancellation,
no history and a changed history snapshot. UI code binds the gesture to an
unobstructed Home/All Apps window, cancels on lifecycle/focus changes, consumes
release across recreation, and blocks competing phone updates during the hold.

The Undo banner names the action, advertises Hold Back, shows progress after
250 ms, and confirms completion. Confirmation does not expose an inactive Undo
button. The clickable alternative has focus styling, reachable Down navigation,
Up/Back exit, a 20-second lifetime and paused dismissal while focused. Editor
hints tell the user to close the editor before using the shortcut.

Widget opacity no longer has a full-screen palette layer. It affects widget card
fills, including the fixed clock, while Settings/app artwork and uncovered
wallpaper are unchanged. Wallpaper dimming remains independent. Saved percentages
and the existing Undo storage are retained.

Native acceptance uses isolated preferences. The expanded BackgroundMenu probe
checks actual saved-state restoration, one-shot/repeat/release semantics, short
Back, no-history feedback, editor exclusion, cancellation by another key, the
clickable alternative, opacity preview cancellation and saved-opacity Undo.
The opacity probe compares uncovered wallpaper pixels across 0/75/100%, inspects
actual Favorites/header fill alpha, and checks opaque controls/content/borders.
Neither probe changes the household layout or language.

Container validation passes 468 JVM tests, Android lint and parity for 610
English/Thai resources. Six of the JVM tests exercise the new gesture state.

Cast1 opacity acceptance passed in coordinator job `J‑c16ee4a12b53` on 0.1.53.
The report reads **ALL CHECKS PASSED · widget backgrounds only**. Home/All Apps
captures and direct pixel/alpha assertions verify unchanged uncovered wallpaper
at 0/75/100%, changing card fills and opaque controls, text, artwork and borders.
Evidence: `artifacts/deployments/0.1.53/widget-opacity/`.

The first menu run, `J‑0339e8222eb5`, found a stale test expectation for the old
opacity label before reaching the new gesture cases. After correcting it,
`J‑46a4b4f1a4f4` installed 0.1.54 with verified SHA-256
`f8b6862e0dc223a5601de89bfbfdcfb09ef0c6e45f31c1bc54f1bf2ea49dbf82`.
Its blank final capture does not establish an acceptance pass. The next debug
probe bounds menu-page navigation and logs each case to diagnose any stall.

The first coordinator-injected physical Back job, `J‑1e478161859f`, failed in
the coordinator input helper before the scheduled Back event was delivered.
Recovery completed safely. This is not physical long-press acceptance evidence.

Final 0.1.55 native acceptance passed on Cast1 in `J‑94c6945235b6`:
**ALL CHECKS PASSED · 68 menu regressions**, present in both the screenshot and
per-case log. The production implementation is unchanged from 0.1.53; later
revisions only correct/improve the debug test. Original household preferences
and language remained unchanged. The installed APK hash is
`c39139daa58d243fc159e878f8eff755c33b429dfcaf742725a9d6d97047eae6`.
The frozen launcher source map matches the working tree.
Evidence: `artifacts/deployments/0.1.55/hold-back-menu/`.

Cast2 is unreachable through the coordinator; Will does not know its current
endpoint. Installation requests remain queued pending connection. Do not count
either a queued install or deferred foreground verification as installation.
Its current 0.1.55 installation request is `J‑84044c088db6`; the superseded
0.1.54 request was canceled. Cast1's initial background install was declined
because the package was foreground; the complete acceptance job then installed
and verified the same frozen 0.1.55 APK.

The final coordinator-injected Back hold passed in `J‑5a79144f8444` on Cast1.
The isolated dashboard recipe completed its navigation/default-layout cases.
The input timeline records accepted Back down/up at 64.031/65.417 seconds
(1,386 ms held), with no foreground abort or emergency release. Frame 11 and
the final screenshot show **Undid Applied default dashboard**, without an inert
Undo button; the fixture's final regression report remains PASS. The fixture
uses isolated settings. Evidence: `artifacts/deployments/0.1.55/physical-back/`.
The catalog was then restored to `com.gathin.tvhome.MainActivity` with the same
frozen APK.

Normal-launcher verification `J‑747356188a05` completed on Cast1 after catalog
restoration. The captured household dashboard shows Rain, Calendar, air quality
with 24-hour/30-day sparklines, Favorites and the configured fixed city cards.
Evidence: `artifacts/deployments/0.1.55/final-launch/`.
