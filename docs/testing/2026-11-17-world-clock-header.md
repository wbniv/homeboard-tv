# Fixed header clock and TV phone shortcuts · 17 November 2026

The [approved header plan](../plans/2026-11-17-world-clock-header.md) and
[phone shortcut addition](../plans/2026-11-17-phone-tv-shortcuts.md) are implemented.

The header has its own saved configuration outside `HomeLayout.widgets`. Older
saved layouts move one standard clock there, retain city IDs and extra grid
clocks, and remove only the captured standard list's redundant Local entry.
Old Undo snapshots normalize grid placement while retaining the current header.
New snapshots restore the complete atomic header/grid/Favorites draft.

Container checks cover migration, custom lists, strict header validation and
native defaults. Chromium/Pixel and WebKit/iPhone exercise the fixed row, absence
of geometry/removal controls, shared Apply/Undo and recovery, independent city
field merges and atomic places/primary/reference conflicts. Shortcut checks cover
separate names and TV IDs, reconnect, mismatch rejection and credential-free URLs.
The native HTTP test covers HTML escaping, manifest identity and rejecting a
wrong-TV pairing request before showing the approval prompt.

## Native evidence

Interim 0.1.48: SHA256 `e2a7544c782dc795335182b43b643e1d0e65e5a8947f2078f2e19ce68f1f45ca`.
Cast1 install-and-launch J‑12e73451caa0 succeeded after background installation
correctly declined install-over-foreground. Cast2 J‑6bafcbb0cb2d verified that
same installed hash; its foreground check remained deferred during Personal use.

Isolated Cast1 header check J‑9353fc79d282 completed. Screenshots verify the
five filled cards, fixed settings clearance, Homeboard Settings focus handoff,
full-screen cards and five-row 24-hour planner, nested place details, Back to
header, and hold opening clock configuration. It also exposed the old All Apps
heading overlaying the clock; the final source moves that label into its footer.
The sun glyph needed a wider text allocation. Both are corrected for the final
build. Evidence: `artifacts/deployments/0.1.48/header-review/`.

Version 0.1.49 combines this work with the separate visible wallpaper-preview
change. That work owns its deployment and preview checks; the final header
corrections follow in the next numbered combined APK.

## Combined 0.1.50 acceptance

Frozen SHA256: `fa21b922ae825107d290d61e7c9581cca9bca3b1a75e0d9dff457d197f7d9cf5`.
All 462 JVM tests passed, lint passed, and 604 English/Thai resources matched.
Both phone browser engines and both clock model/component suites passed.
Every frozen launcher source checksum matched the working tree at packaging.

- Cast1 J‑ce9ba31c19eb: installed hash verified; 58-second header check completed.
  Screenshots confirm the All Apps footer correction, sun/moon glyphs, minute
  refresh on All Apps, opening full screen there and Back restoring header focus.
- Cast2 J‑52ea7fb4a38b: background installation hash verified; foreground deferred.
- J‑770217ad0012: 200% text, 12-hour time, 12 places, off-screen primary and +9.
  Header and Settings remain readable and separated. This verifies the header;
  the existing Air card clips at that global font size and needs separate work.
- J‑da420ddc33f8: `MEETING_PASS` from real native Compare, 60/90/15/30-minute
  buttons, Map, Now and nested Back. Live map and five-city timeline captured.
- J‑d22737c6ca41: screenshot reports **ALL CHECKS PASSED · 57 menu regressions**,
  including the combined wallpaper-preview change and unchanged real settings.
- J‑08b3723ed0ae: native phone probe caught cleartext-policy rejection in its new
  test-only manifest GET helper, before pairing. The existing controller and
  browser tests are unaffected; the helper is corrected in the next diagnostic
  build using the same bounded socket transport as its existing POST checks.

Read-only native clock capture matches Will's original five named records,
including every saved ID and setting, after explicitly removing Local and
moving primary to his existing Bangkok ID. Record:
`artifacts/deployments/0.1.50/household-header.json`.

Empty fixture launches J‑273229a73f42 and J‑85221940f0e9 stopped before their first
capture with `target-left-foreground`; the coordinator found Android TV Settings.
They are not empty-page acceptance passes. Exported startup evidence lacks the
app crash log. The diagnostic fixture now retains a visible startup exception
and stack trace if that is the cause. No raw-device fallback was used.

Version 0.1.51 changes only debug acceptance helpers and version metadata;
production header/controller sources remain the verified 0.1.50 implementation.
Final native phone and empty-page outcomes follow below.

## Limits

Physical phone home-screen installation uses the browser's own UI and has not
been observed on a physical handset. Browser fixtures verify the controller
workflow; they do not constitute that OS acceptance. Shortcuts retain the TV's
current local address and stable installation ID. The foreground-only listener
must be opened on that TV before reconnecting; DHCP changes require a new link.
Cast2's installation proof and deferred foreground verification are separate.


## Native phone 0.1.51 and final navigation correction

0.1.51 SHA256: `d1b92a79a4e002e2dc8b1cdd311d6cdd5d2605c9f3a262bd969bc381971a2695`.
Only two debug acceptance helpers differ from 0.1.50; all production source is
identical. Cast1 J‑19505205b383 and Cast2 J‑bea9f9b4edd3 verified that installed
hash. Native log reports **PHONE_PASS** for real Android HTTP, TV approval,
shortcut metadata, mixed header/grid/Favorites Apply, duplicate retry, preview,
composite Undo and Stop. Main launch J‑a39cf9e7498e completed.

Cold empty-fixture J‑ea0669a2e347 again left the target before its first capture.
A separate source review found a navigation regression: the new header fallback
could replace empty surface focus, requiring extra Right presses to reach All
Apps. 0.1.52 restores the existing blank-surface navigation when the previous grid
focus is gone; explicit header/settings focus remains available and unchanged.
Its focused native test enters empty Home after normal startup and checks a
single Right, Back without refilling, reaching the header, opening the planner
and returning to header focus. This avoids mistaking a failed fixture launch for
navigation evidence. Final deployment and focused regression outcome follow.


## Final 0.1.52 deployment

Frozen SHA256: `0e4673f56d3324c7be1b2e8ce382bbc593394444460f4bd4169db90166d02030`.
Both installed hashes are verified: Cast1 J‑7663b0b98bf5 (normal MainActivity
launch also passed), Cast2 J‑bc56456bbb8c (foreground check deferred during
Personal use). All frozen launcher source hashes match the working tree.
462 JVM tests, Android lint and 604-resource English/Thai parity passed.

The dedicated empty-header activity again failed the coordinator startup guard
before screenshots (J‑b6508de6025a). Its launch problem remains distinct from
navigation. The existing isolated **DashboardProbeActivity** successfully ran
J‑7f12a20dfbf6 against this final APK. Its final capture reports:
**PASS: two-row single-screen default; all Right exits verified**. That includes
removing every grid widget, a single Right opening All Apps, Back retaining an
empty Home, populated controls, every widget/Favorites exit, Undo focus, immediate
default placement, saved preview exit, and two-/four-row defaults. This provides
positive native coverage of the final empty-surface correction.

Normal MainActivity registration is restored for the same frozen APK. Coordinator
cleanup explicitly verified MainActivity in the foreground and restored input
state. Superseded launch checks owned by this session were canceled; the current
Cast2 foreground check remains deferred. Installation never changed the default
Home app, wiped app data or released Personal use.

The final artifact record is
`artifacts/deployments/0.1.52/final-verification.json`. Prior native phone,
planner, enlarged-header and wallpaper/menu results above apply to unchanged
components; the final production change is the empty-surface focus correction.
The observed native display was 1920×1080 with normal and 200% text; separate
physical 720p/4K and home-screen shortcut creation have not been observed.
