# Home favorite sizes · 16 November 2026

## Dense style-specific grids · 0.1.8

Commit `6a0366b` adds separate icon and banner grid sizing to both Home and All
Apps. At 960 × 540 dp with a 20 dp label height, Auto fits 11 × 4 small icons
or six wider banners across three taller rows. Home banners use two placement
columns per card; changing style resolves against the new grid. The regression
suite now has 362 passing tests, including readable pitch, maximum fit, larger
text and restoring Home placement across a style-dependent grid change.
Container lint passed with zero errors, 194 warnings and 96 informational findings.

`task deploy` built and froze **0.1.8** at
`artifacts/deployments/0.1.8/homeboard.apk`, SHA-256
`3971e4def28158712819cd909095623e56a02cdf6d39eca4efbe5365d71d35fa`.
Cast1 job **J‑9e1052196a58** installed and launch-verified it successfully, with
ready health. The screenshot shows six All Apps banners across three rows,
including icon fallback and rectangular focus. It is not a Home screenshot.

![0.1.8 All Apps: six banners across three rows](../../artifacts/deployments/0.1.8/evidence/screenshot.png)

Cast2 background-install job **J‑12a621b7a7fc** was admitted during personal use
but returned `personal-install-unverified`; installation is not claimed. Its
evidence is at `artifacts/deployments/0.1.8/cast2-install/`. Personal use itself
does not prevent background-install admission.

Before the Home visual recipe could run, separate Projectivy work deployed
**0.1.9** on Cast1 in **J‑fb03bd528d6a**. Its receipt verifies replacement of
the 0.1.8 hash above with
`2396809ac2fff4935bfb67d913597cb14aafb52f8e26ee62aecd4201118eaf39`.
That worktree is based on `290e675` and does not include the Home favorite size
or dense Home implementation on `main`. Will confirmed About showed 0.1.9;
this explains the unchanged dashboard. Evidence is at
`artifacts/evidence/cast1-installed-0.1.9/`.

The Home recipe `dev/device/dense-home-style-check.json` remains unexecuted.
It returns from All Apps with one Back press, captures Home, selects Banners,
captures the result, then uses Undo and captures the restored layout. Verify
that Home is using Small icons at the start. No Home banner visual acceptance
is claimed from the All Apps screenshot or the unit tests.

Will assigned the other agent to merge its Projectivy work into `main` before
the next combined APK. Home favorites are in `be49bd7`; dense style-specific
grids are in `6a0366b`. Preserve both changes and use a deployment code greater
than 9. No further older-APK install or scripted navigation was submitted here.

## Earlier 0.1.7 validation

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
