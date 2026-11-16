# Compass pie menu redesign · 16 November 2026

## Current local build and pending Cast1 regression

The local artwork-setting change now preserves All Apps when the user switches
between dense launcher icons and TV banners. After the grid changes size, it
restores focus to an app on the visible page. Home favorites remain compact.
The current version code is 6 (0.1.6); the debug APK is
`artifacts/validation/homeboard-pie-about-0.1.6.apk`, SHA-256
`3cd64aab9981191917cc595c23b945b55624c57f1ff87e06833db24a75ac1945`.

Container unit tests, lint and APK build passed after this change. Cast1
verification remains pending until the TV and development machine are back on
the same network. The coordinator recipe is ready at
`dev/device/pie-artwork-about-check.json`; it covers the menu-label captures,
About source links, palette preview and reset, then both artwork choices from
All Apps.

The 0.1.5 coordinator run `J‑14da7356edda` exposed the regression: after
switching from launcher icons to TV banners, the app returned to Home. The
screenshots below record the before and after states. The 0.1.6 code change
addresses that state transition, but has not yet been exercised on Cast1.

![About page and source cards](../../artifacts/evidence/cast1-pie-artwork-about-0.1.5-final-2026-11-16/input-frame-003.png)
![About source card focused](../../artifacts/evidence/cast1-pie-artwork-about-0.1.5-final-2026-11-16/input-frame-004.png)
![Palette current color](../../artifacts/evidence/cast1-pie-artwork-about-0.1.5-final-2026-11-16/input-frame-006.png)
![Palette preview](../../artifacts/evidence/cast1-pie-artwork-about-0.1.5-final-2026-11-16/input-frame-007.png)
![Palette restored](../../artifacts/evidence/cast1-pie-artwork-about-0.1.5-final-2026-11-16/input-frame-008.png)
![All Apps before switching artwork](../../artifacts/evidence/cast1-pie-artwork-about-0.1.5-final-2026-11-16/input-frame-010.png)
![Dense launcher icons in All Apps](../../artifacts/evidence/cast1-pie-artwork-about-0.1.5-final-2026-11-16/input-frame-011.png)
![Regression: TV banners selection returned to Home](../../artifacts/evidence/cast1-pie-artwork-about-0.1.5-final-2026-11-16/input-frame-012.png)

An earlier 0.1.6 device attempt exited Homeboard because its recipe sent three
initial Back presses. Those inputs were removed from the recipe; they were a
test-startup issue, not an app regression. The corrected recipe is pending the
next shared-network session.

## Earlier coordinator evidence

Built the final APK in the Android container and installed it on Cast1 through
the shared Chromecast coordinator. APK SHA-256:
`54b0c442b93c01a8b8889d2bc6a1f4e9ef6cadb083f5b8a4fa5bb6bfb49304e2`.

Coordinator job `J‑58b07f997ec1` completed with health `ready`. Its remote
recipe covered Home settings → Home behavior → Grid columns, moved Auto to 4,
applied the setting, opened Undo history, and undid the change. It then opened
About, used Back to return to Home settings, navigated to Restore hidden app,
and returned from the empty state. Cast1's captures show the bounded choices,
the Undo result, the content-first About screen, and the explanatory empty
state. The original grid setting was restored before the job ended.

![Bounded grid selector on Cast1](../../artifacts/evidence/cast1-pie-full-flow-empty-state-2026-11-16/input-frame-003.png)
![Grid column choice moved from Auto to 4](../../artifacts/evidence/cast1-pie-full-flow-empty-state-2026-11-16/input-frame-004.png)
![Undo history and available action](../../artifacts/evidence/cast1-pie-full-flow-empty-state-2026-11-16/input-frame-006.png)
![Undo restored the grid setting](../../artifacts/evidence/cast1-pie-full-flow-empty-state-2026-11-16/input-frame-007.png)
![Content-first About screen](../../artifacts/evidence/cast1-pie-full-flow-empty-state-2026-11-16/input-frame-008.png)
![Restore hidden app explains its empty state](../../artifacts/evidence/cast1-pie-full-flow-empty-state-2026-11-16/input-frame-009.png)

Evidence bundle: `artifacts/evidence/cast1-pie-full-flow-empty-state-2026-11-16/`.
Recipe: [pie-undo-grid-check.json](../../dev/device/pie-undo-grid-check.json).
The coordinator also completed a shorter earlier run while the empty-state
copy was still being corrected. This final run uses the corrected build.

The final screenshot verifies visible menu content and scripted remote
navigation, but Will's hands-on remote review is still open for palette focus
preview, focus movement among ordinary pie choices, and the overall 10-foot
readability.
