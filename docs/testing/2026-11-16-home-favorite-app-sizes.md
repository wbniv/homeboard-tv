# Home favorite sizes · 16 November 2026

Small icons and Banners are implemented independently of All Apps artwork.
Implementation commit: `be49bd7`. `task deploy` advanced the APK to **0.1.7**
(version code 7), passed container tests/lint and built the debug APK. All 357
unit tests passed; lint reported zero errors, 194 warnings and 96 informational
findings. Tests cover persistence, independent artwork choices, both placement
paths, fixed widgets, collisions, overflow, snapshot restoration, wide move
previews and banner fitting with large labels.

Frozen APK: `artifacts/deployments/0.1.7/homeboard.apk`.
SHA-256: `99efd1edae62c2871b82fe5c257b242f3071fa9f3ae30f8cfd885faabfac065e`.
Both target devices use this identical file. No app data was cleared and the
default Home app was not changed.

## Cast1

Job **J‑1f15bd8f4c72** installed the APK and verified its installed SHA-256.
Its subsequent scripted input helper stopped after the first Menu key; the
coordinator reported `plugin-error` at `plugins/android/scripted_input.py:159`,
with an input-helper command returning 137. Automatic recovery
**J‑597326babccf** restored ready health. The installed-hash evidence is at
`artifacts/deployments/0.1.7/evidence/apk-installations.json`.

The follow-up brief launch check **J‑01b0121ba30c** reused the installed APK and
completed successfully with ready health. Its screenshot shows the saved Home
widgets, favorite membership and ordering intact, with a focused small icon.
Evidence: `artifacts/deployments/0.1.7/cast1-verification/`.

![Cast1 Home after installing 0.1.7](../../artifacts/deployments/0.1.7/cast1-verification/screenshot.png)

This confirms installation and launch. It does **not** establish on-device
acceptance of Banners, fallback artwork, the rectangular focus halo or size-change
Undo: the scripted helper failed before those steps. The complete recipe remains
`dev/device/home-favorite-style-check.json`. The expanded About/palette/All Apps
recipe also remains pending; container tests do not replace its visual assertions.

## Cast2

Will requested the same deployment on Cast2. The initial foreground check
**J‑701d081555e3** was deferred during personal use and cancelled before grant to
allow a background install. Background-install job **J‑408789084372** returned
`personal-install-unverified` with ready health; its installation record says
`unverified`, so installation is not claimed. Evidence is at
`artifacts/deployments/0.1.7/cast2-install/`.

Complete install-and-check job **J‑619e5c575044** is queued for the same frozen
APK. The coordinator is deferring it during personal use. Its eventual install,
launch and cleanup outcomes must be recorded separately. No personal-use marker
was removed or bypassed.
