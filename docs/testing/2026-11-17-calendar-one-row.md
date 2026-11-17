# Calendar one-row resize · 17 November 2026

The Calendar minimum is now **2 columns × 1 row** in both remote resizing and
saved layout validation. Short calendars use one row of square date cards with
a compact heading, page count and status footer. Taller calendars retain their
existing layout. The default size remains 6×3.

## Container checks and artifact

`task check` and the checks repeated by `task deploy` passed: **423 JVM tests**,
zero failures/errors/skips, six calendar pipeline checks, two deployment helper
checks, parity for 462 English/Thai resources, and Android lint. New tests cover
one-row persistence/packing, minimum dimensions and compact card bounds.

- Version: **0.1.27**.
- Frozen APK: `artifacts/deployments/0.1.27/homeboard.apk`.
- SHA-256: `db4909c049068b87466cc2774b5c8cc7bbe6777ca103d07db3c2c32cbfbda89c`.
- Base revision: `50919e6`, plus the calendar changes recorded in that directory's
  `deployment.json` and `source-sha256.json`.

## Install evidence

Cast1 background job `J‑849c47071b6f` refused installation over the foreground
Homeboard app (`install-over-foreground`). The complete install/brief-launch retry
`J‑9528cf432ffa` completed successfully. Its `apk-installations.json` verifies the
installed hash above. Evidence is in
`artifacts/deployments/0.1.27/chromecast-test-01/foreground-evidence/`.

Cast2 job `J‑7c86d5bab316` verified installation of the identical APK. Its foreground
`verify-launch` continuation remains queued during Personal use. Evidence is in
`artifacts/deployments/0.1.27/chromecast-test-02/evidence/`. Installation is confirmed;
foreground and visual acceptance on Cast2 are deferred. Local waiting clients were
stopped without cancelling either queued device job or changing Personal use.

## Cast1 remote acceptance

Navigation job `J‑413e5c4a6466` completed and captured the user's existing Calendar
on Home page 2, alongside two air-quality widgets. This established the starting
layout for `dev/device/calendar-one-row-resize-home.json`. Every coordinator check
relaunches Home on page 1 with the device settings control focused; the recipe
first navigates down to Rain, right to page 2, then right to Calendar. It is
specific to that existing layout.

Initial resize job `J‑6adf604258c1` assumed the preceding job's page/focus would
persist. It instead opened Android Settings from the fresh starting screen and
the coordinator's foreground guard stopped it. Automatic readd `J‑dc7607fb7758`
completed and restored Ready. The evidence establishes a test navigation error,
not a Calendar crash.

Job `J‑02a19b0bb4f8` completed, but its captures exposed another script error:
Calendar uses a radial menu where one Down selects Resize immediately. Extra
Down/OK inputs enlarged and saved the calendar before the intended Up inputs.
It does not establish one-row acceptance.

Corrected resize job `J‑380b32e814ba` uses the radial menu's Down arm, saves the
one-row size, then previews two rows and cancels back to the saved size. It
**completed successfully**, with the device Ready afterward. Reviewed captures in
`artifacts/deployments/0.1.27/chromecast-test-01/resize-final-evidence/` establish:

- `input-frame-003.png`: the actual resize footer reads **8×1**, and repeated Up
  stays at one row. Date numbers, flower/Buddha icons, flag bands, month/year and
  event labels fit inside the compact strip, with Offline dates below the cards.
- `input-frame-004.png`: OK saved the one-row calendar.
- `input-frame-005.png`: a subsequent resize previews **8×2**, with the normal
  heading, week ranges and two event labels.
- `input-frame-006.png`: Back restores the saved one-row size.

The user's Calendar remains **8×1** on Cast1. Other widgets and Calendar source
settings were retained. Device acceptance covers this actual saved layout; narrow
2×1/wide 12×1, multi-page compact results and enlarged Thai text have not received
separate visual acceptance. Persistence/packing and short geometry are covered by
the passing unit tests.

The separate debug-only `CalendarResizeProbeActivity` and
`dev/device/calendar-one-row-check.json` exercise isolated layouts at 2×1, 6×1,
12×1 and 6×2. They were compiled but have not run: changing the catalog activity
while normal foreground continuations are pending could affect those jobs. Actual
Home acceptance uses the normal MainActivity registration instead.
