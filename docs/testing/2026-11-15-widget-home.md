# Widget Home validation — 15 November 2026

Compilation, tests, lint and APK/bundle packaging ran in the project container.
The branded debug APK is `05b536197e3e1576e2f8e8fbace4fe7cdbe31a096c3a4d384037e977d377182c`.
It contains the Homeboard icon and TV banner. All 327 unit tests passed. Lint has
zero errors, 179 warnings and 96 informational findings, including inherited unused
resources and a square launcher-icon shape warning. The release APK is unsigned.

| Artifact | Bytes | Rounded size |
|---|---:|---:|
| Debug APK | 2,532,094 | 2.53 MB / 2.41 MiB |
| Optimized unsigned release APK | 355,649 | 356 KB / 347 KiB |

## Device evidence

All sessions used the shared coordinator on chromecast-test-01. The default Home
app was not changed. Downloaded evidence lives under the ignored `evidence/`
directory; selected screenshots are preserved in `docs/how-to/quick-start/`.

| Job | Evidence directory | Observed result |
|---|---|---|
| J‑305df3cd6bcd | widget-home-first | AirBKK station menu, readable text after selection, Home/All Apps, last-widget deletion and empty background verified |
| J‑0506f74d5364 | widget-fixture-install | Disposable test app installed for launch/uninstall checks |
| J‑f4c363380e28 | Coordinator record | Stopped when opening fixture; foreground guard interrupted script; automatic recovery completed |
| J‑531c8eb79bce | widget-app-management | Script completed, but starting state already had a favorite; do not count move/launch assertions as passes. Screenshots verify favorite persistence, unfavorite and app hide/restore |
| J‑e54e97a2c155 | widget-app-management-branded | Branded APK installed, app favorited and shown on Home; opening fixture triggered coordinator foreground guard again |
| J‑840335b543b9 | widget-move-delete-restore | Home move preview/cancel/save, delete, All Apps hide/restore work. Raw screenshots retain headers/backgrounds; confirmation heading crowds buttons |

The interrupted scripts logged `WfKeyScore: Target app left foreground`, from the
coordinator input helper. Android activity logs show the fixture launch. This is
not a Homeboard Java crash, and it is not evidence that return navigation passed.
No retry of that unsupported cross-app sequence is planned.

## Remaining checks

- [x] Complete in-app move/delete/restore screenshot review.
- [x] Recheck raw screenshots: content was present; the apparent disappearance was a preview artifact.
- [ ] Verify confirmation sizing and explicit focus restoration.
- [ ] Verify app ordering, compact sizing, palettes and saved empty relaunch.
- [ ] Validate launch/return and uninstall confirmation/cancellation/removal using a supported coordinator workflow.
- [ ] Remove `net.biohack.homeboard.fixture` from the test device using supported coordinator cleanup; it remains installed because current scripted input cannot traverse Android's uninstall activity.
- [ ] Verify default-launcher Home-key behavior in an explicitly configured device session.
- [ ] Broaden RTL, accessibility text sizing, theme and outage/recovery coverage.

## Screenshot interpretation correction

The image preview made unchanged regions of successive captures appear missing.
Fresh resized views of `air-quality-final/input-frame-004.png`,
`widget-move-delete-restore/input-frame-009.png`, and
`widget-render-appearance/input-frame-006.png` show the text and backgrounds intact.
Earlier disappearing-content reports were incorrect. No GPU or upstream rendering
defect has been established. The experimental software-layer workaround was removed.

The current grid implementation measures and lays out its own cells, avoiding
layout requests inside an on-layout callback; focus restoration preserves the
requested tile across rebuilding the views. Confirmation height includes the
measured wrapped heading. These changes are under final device verification.

`J‑0efbef4e4a53` verified All Apps move/save/cancel, compact air quality and Forest
palette. It then unexpectedly opened Android Settings during the scripted reduced-motion
sequence, triggering the coordinator's foreground guard. That later sequence is
not a pass; the fresh captures up to the palette change are valid.

## Manual iteration handoff

This log records Cast1 testing through 16 November. Will then moved the target to
Cast2; the coordinator currently pauses it for another owner's personal use.
Subsequent available-device updates use `iteration-check.json` for
installation and a brief launch check, without scripted remote input. Cast2 is
now the requested target but is paused for another owner's personal use. The
launcher-icon/TV-banner preference is implemented and persisted;
its visual toggle, final confirmation sizing/focus behavior, reduced motion and
empty relaunch are awaiting manual acceptance. Earlier scripted recipes reflect
the menu structure of their recorded build and must be reviewed before replaying.

Latest iteration: `J‑51b30d10f6dc` completed on cast1. Installation and the brief
launch check passed; evidence is in `evidence/iteration-app-artwork`. No scripted
navigation was sent. Container recheck passed all 327 tests, lint with zero errors,
debug/release APK assembly and unsigned release bundle packaging. The installed
APK includes the App artwork choice and the explicit grid/focus/confirmation sizing
changes. Will is performing interactive acceptance on cast1.

Slide-transition iteration: the Home/All Apps surfaces enter from opposite sides
with a 420 ms decelerating slide. APK built in the container and installed on cast1
under coordinator job `J‑a029a6947a1f`; no scripted remote input was sent.

## Home-role setting iteration

The Homeboard settings action now requests Android's `ROLE_HOME` on Android 10+
devices, falling back to the system Home settings page where the role API is not
available. It reports whether Homeboard holds the role when Android returns. This
does not itself remap a physical TV remote key: Android TV's official navigation
guide says its Home button returns to Google TV Home. The role can govern standard
Home intents, while the remote's firmware may route its key directly to Google TV.
Any Home-key interception approach is a separate design and policy question.

Container `task check` (unit tests and lint) and `task build` passed. The installed
APK is `artifacts/validation/homeboard-cast1-home-role-2026-11-15.apk`, SHA-256
`bcfb9fc909285cd21a9aff64cb2f9af429c9d3973aac0b5d81ee958d1d3f9db3`. Coordinator
job `J‑021ed70b2b35` installed the exact artifact on cast1 and completed its
three-second launch check with device health ready; evidence is in
`evidence/cast1-home-role-2026-11-15`. The system prompt was not operated by the
test. Will should open Homeboard settings and try the chooser interactively, then
check whether the physical remote Home button changes behavior.

## Coordinator guide integration and E2E run

Followed `/home/will/chromecast-coordinator/docs/automated-device-testing.md`.
The Task adapter exposes coordinator `version`, `support` and the shared device
workflows. Cast1 reports coordinator 0.1.159 (`be53557a`); its `check` and
`record` workflows are ready. Frozen artifact and recipe/oracle checksums are in
`artifacts/validation/homeboard-e2e-checksums.sha256`; provenance and both job
outcomes are in `artifacts/validation/homeboard-e2e-receipt.json`.

| Job | Workflow | Result | Evidence |
|---|---|---|---|
| J‑6303c520ac1e | record, Home → All Apps → Home | Failed before its first capture: `target-left-foreground` | `evidence/homeboard-e2e-first` |
| J‑1a282e99b15e | check, launch and capture | Succeeded; exact APK hash reused; coordinator cleanup restored Google TV Home | `evidence/homeboard-e2e-launch-check` |

The successful screenshot visibly shows the Homeboard screen and its Bangkok ·
Benchasiri Park card with a PM2.5 reading (26.5 µg/m³ at capture time). The
offline OCR oracle passed for PM2.5, Bangkok and AirBKK. The intentional negative
case requiring `unobtainium` returned exit code 1. Together, the successful
coordinator launch/capture, rendered live-data card, and positive/negative oracle
form the verified smoke test. Neither job changed the default Home app.

The checked-in smoke recipe is now the successful three-second `check` workflow;
it no longer tries scripted navigation. The earlier `record` attempt failed
before its first capture, so it provides no evidence about sliding between Home
and All Apps. It remains separately recorded to explain why the bounded smoke test
uses the coordinator's supported launch-check capture instead.

## Cast1 artwork, density, settings gear and wallpaper iteration

This iteration makes Launcher icons use a 6 × 5 grid (up to 30 tiles) and TV
banners use 4 × 2. It adds the same-size circular icon-mask preference, a persistent
top-right device-settings gear, excludes Android Settings and developer log viewers
from the app list, and adds photo selection with app-private persistence. The
Homeboard-only appearance controls remain separate from Android device Settings.

`task check` passed in the container (unit tests and Android lint); `task build`
assembled the debug APK. The final artifact is
`artifacts/validation/homeboard-cast1-iteration-2026-11-15-r2.apk`, SHA-256
`862d260c7c1470dcd9fab5a0f266027cf93c35c964b99d0560e86db6ed32ccde`.

| Job | Result | Evidence |
|---|---|---|
| J‑55a8ce7b7ab0 | Final predecessor hash installed; launch check stopped with `target-left-foreground` while Android Settings was foreground. No screenshot was produced. | `evidence/cast1-ui-iteration` |
| J‑d7edf5d9961c | Final hash installed; check completed and health remained ready. | `evidence/cast1-ui-iteration-final` |

The successful screenshot verifies the dashboard and visible top-right gear; the
air-quality screenshot oracle also passed. The screenshot is included in the
[quick start](../how-to/quick-start/device-settings-gear.png). The coordinator
workflow sent no scripted navigation. Visually checking the 30-app layout, icon
mask choices and wallpaper selection/persistence remains Will's interactive
acceptance on cast1. Cast2 is unchanged.

## Homeboard settings and All Apps page-up iteration

The next APK adds a persistent sliders button for Homeboard settings beside the
Android device Settings gear. From All Apps, Up on the first row now returns to
the previous page at the same column; Up on page 1 focuses Homeboard settings.
The container `task check` and `task build` both passed. The built APK is
`artifacts/validation/homeboard-cast1-iteration-2026-11-15-r3.apk`, SHA-256
`09a35cc0ee0c052d881261399ae5ce1582637983c2e450d43da7f1bba79b87d6`.

Coordinator job `J‑69aec171e5d4` installed and verified that exact APK hash on
Cast1. Its launch check stopped with `target-left-foreground` because Android
System Settings was foreground; no screenshot was produced. Device health
remained ready and cleanup completed. The install is verified, but the two
settings controls and page-up behavior still need Will's interactive check.
Cast2 was not touched.

## Color palettes and compact-app focus recovery

The background settings now offer ten named color choices with matching circular
swatches, a visible `Current` label, and fixed pages of six choices. The selected
palette is saved and restored across launcher sessions. In compact All Apps, the
glass highlight snaps to the saved app after resume/window-focus regain, then uses
the normal spring animation on the next D-pad move. This also handles Android
retaining focus without emitting another focus-change callback.

`task check` (unit tests and lint) and `task build` passed in the container. Final
APK: `artifacts/validation/homeboard-cast1-palette-2026-11-15-r2.apk`, SHA-256
`7e33dbf3ee5bd08150150d5c3a245eff596ba37b724c85f22e3b77cd3e9cf80f`.
Coordinator job `J‑72a2cfeba591` installed that exact hash and completed the brief
launch check on Cast1 with health ready. Its screenshot updates the [quick-start
All Apps image](../how-to/quick-start/all-apps.png).

Earlier, scripted menu job `J‑5d5bf193f038` installed an intermediate APK but
stopped before the first scripted input with an Android coordinator-plugin error.
The coordinator's automatic recovery finished and restored Cast1 to ready. A later
simple install/launch job succeeded. The palette screen and resume-after-app focus
snap still require interactive verification on Cast1; Cast2 was not touched.

## Compact Home favorites and measured focus highlight

Home favorite apps now use 48 dp launcher icons, 16 sp labels, the same optional
circular mask, and a small glass focus highlight centered on the icon. Their full
Home grid cell has no app-card background, so a favorite does not turn into a
banner-sized tile. The All Apps artwork selector still controls Launcher icons vs.
TV banners in All Apps; Home favorites remain compact either way.

The first Cast1 screenshot exposed that the highlight could be laid out before the
icon had a measured size. The final build snaps it to the measured icon after every
new surface render, in addition to resume and window-focus regain. Container
`task check` and `task build` passed. The final APK is
`artifacts/validation/homeboard-cast1-home-favorites-2026-11-15-r2.apk`, SHA-256
`20cf4078d80eae625ec8905cbf5fa7f36cd5878adde1f905a7ab5aa4eeebeeda`.

Coordinator job `J‑720f2cddffb0` installed that exact hash and completed the
Homeboard check on Cast1 with health ready. Its captured Home page shows two
favorites at compact size; the focused glass highlight now surrounds one icon
instead of appearing offscreen. The screenshot replaces
`quick-start/favorite-home.png`. Manual verification of returning from an actually
launched app remains useful; the coordinator's foreground guard prevents a scripted
cross-app return sequence.
