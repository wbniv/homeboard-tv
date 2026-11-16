# English/Thai worktree validation

Implementation branch: `feature/thai-language`, isolated at
`/home/will/homeboard-tv/.worktrees/thai-language`, based on `e288f89`.
The concurrent calendar edits in the main checkout are excluded.

## Scope

- [x] English, Thai and Follow device compass with current-mode marker.
- [x] Native locale owner, persistent independent setting, language Undo and
  recreation back to Home settings.
- [x] Paired core UI, air-quality condition/station, chart and wallpaper-status resources.
- [x] Container resource gate integrated into test/lint/APK/bundle tasks.
- [ ] Full staged-index, semantic review and deployment acceptance enforcement.
- [ ] Linguistic review by a competent Thai reviewer.
- [ ] Device/emulator checks: switching/relaunch, API 26/32 and API 33/34 behavior,
  focus recovery, Thai shaping/wrapping, reduced motion and larger text.

The resource gate covers the new `homeboard*.xml` catalogs. It does not establish
complete reachable coverage of inherited resources, cached Undo text, custom
aliases or provider metadata. Raw provider readiness metadata and some inherited
surfaces remain to be inventoried. Do not call this a fully accepted Thai release.

No APK has been installed by this workstream. Before installing a combined
candidate, coordinate one deployment owner, merge intended features and consume
a fresh deployment number using `task deploy` through the shared coordinator.

## Container results

- [x] `task check`: 376 JVM tests, zero failures/errors/skips; Android lint passed.
- [x] Structural translation gate: 315 English/Thai Homeboard resources validated.
- [x] `task build`: debug APK packaged successfully.
- [x] `task bundle`: unsigned release bundle packaged successfully, including R8
  optimization and resource shrinking.

Artifacts are development builds using the unchanged `0.1.14` source version,
not numbered deployments. Any installation must use a fresh `task deploy` bump.

| Artifact | SHA-256 |
|---|---|
| `launcher/app/build/outputs/apk/debug/app-debug.apk` | `61f7022fff9ffc474c6df9187c071dd22e3867f69390fe8d0beb6097d9292ca4` |
| `launcher/app/build/outputs/bundle/release/app-release.aab` | `430518001a7eb69651e0aec417a6cb6cd7cdd88cdeb3bd7ceda067e4e61eebad` |

The unit tests exercise supported-mode validation, ordered device-locale
resolution/fallback and preservation of station-label/feed identity across
English/Thai resources. They do not prove Android recreation or rendering.

## Combined deployment attempts

The Thai worktree merged the committed Background-menu and About QR fixes from
`fix/background-menus` in `1622657`; unfinished calendar work in main is excluded.
The combined candidate passed 380 tests, lint and the 315-resource structural gate.

- [x] Attempt 0.1.17 through coordinator job J‑75ba5585e758; evidence exported.
- [x] Diagnose `INSTALL_FAILED_UPDATE_INCOMPATIBLE`: the nested worktree's new
  Gradle volume generated a different debug signing key. Coordinator evidence
  confirms the previous installed APK stayed unchanged and device health is ready.
- [x] Verify the existing project cache certificate matches the installed APK
  (`87884e9c…`); name the shared cache explicitly in Compose for worktrees.
- [ ] Deploy the corrected build with a fresh number and verify launch/evidence.

The failed 0.1.17 APK SHA-256 is
`14b30d083a909c49fc23f75864ef6e3d6bca3170f42f78b19b054f32ce57ac38`.
The complete failed evidence and source patch are retained under
`artifacts/deployments/0.1.17/`. No uninstall or settings reset was performed.
