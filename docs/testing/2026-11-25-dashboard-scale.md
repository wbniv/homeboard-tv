# Dashboard scale · 25 November 2026

**Current baseline:** ordinary Homeboard 0.1.170 uses native UHD **100%** for
the same 16×8 workspace previously labelled 50%, with 150%/200% for larger
widgets. Saved sizes and positions are unchanged. See the
[production rollout and actual preview](2026-11-26-automatic-4k.md). Percentages
and version/install states below describe the dated scale implementation.

## Follow-up acceptance · 26 November

Native scale preview/Cancel/Save/Undo and four widget Back/Restore checks now pass.
Phone browser UI, actual Android editor protocol and an ordinary provider control
pass at 50% / 16×8. Native provider fixture hosting also passed that evening:
move/resize, expansion, Back/Restore and same-binding Remove/Undo. Original layout,
focus and permission were restored, with zero native host widget IDs remaining.
External phone transport remains unverified. See the [current acceptance record](2026-11-26-dashboard-scale-acceptance.md)
for evidence, deployment 0.1.163 and remaining scope. Earlier incomplete sessions
below remain historical records.

Implemented in local build **0.1.161** for ordinary Homeboard and the separate
native 4K dashboard app. **0.1.162 is installed on Cast1, Cast2 and Pixel** in
both packages. Install verification is complete; scale interaction acceptance
is recorded in the follow-up above. The earlier network interruption and later deployment are recorded
separately below.

## Behavior at initial implementation · 0.1.161

**Home settings → Appearance → Dashboard scale** opens the actual Home preview
with 100%, 75% and 50% choices and explicit Save scale / Cancel. The preview shows
the resulting columns and rows. At 50%, the default 8×4 grid becomes 16×8.
Tile contents shrink, while settings, the header, All Apps and expanded widgets
retain normal dimensions. This workspace choice does not change HDMI mode,
Android display density or the dashboard's rendering buffer.

The preview uses a separate layout copy; persisted state and phone snapshots
continue to report the saved layout. Auto grid fit grows with the workspace;
explicit counts scale proportionally, bounded to 24 per direction and the existing
four-column/two-row minima. Saved widget positions and explicit spans are retained.
Widgets with automatic spans have their current resolved spans captured when
changing scale, so they also become smaller instead of growing with grid width.

Save commits the scale, counts and captured spans through the existing atomic
layout/Undo path. Save without a change adds no scale mutation. Cancel, Back,
leaving the app or opening another flow discards the draft and restores navigation.
Recreation reads only committed settings. Expanded widgets keep their existing
Back/Restore flow and saved dashboard sizes.

Each scaled tile has a transformed parent, preserving its own focus animation;
app highlight geometry uses the displayed bounds. Provider size reporting uses
logical tile dimensions. Phone validation, numeric controls and full-width widget
spans now support the larger grid; the phone cannot change scale itself.

Highlight bounds explicitly compose ancestor view matrices and scroll offsets.
This accounts for the scaled tile parent rather than relying on offset-only
coordinates; compare the [AOSP ViewGroup implementation](https://android.googlesource.com/platform/frameworks/base/+/c6e2e8f/core/java/android/view/ViewGroup.java).

## Container verification

- Six DashboardScale regression tests pass: fourfold capacity, settings/position
  retention, round-trip persistence, Auto/intermediate scale, bounded/minimum
  counts, layout Undo, unsupported scale, automatic spans, isolated preview copy
  and a 16×8/full-width-widget phone round trip.
- **697 JVM tests**, zero failures/errors. Calendar pipeline: seven passed;
  cleanup: 71 passed; deployment helper: five passed.
- Android lint passed for both packages. English/Thai resource parity passed.
- Phone model tests pass, including the new larger-grid case and range rejection.
  Existing Chromium/WebKit phone suites passed for editing, movement, preview,
  cancellation, Update/Undo, recovery, large text and world-clock settings.

The first new assertions compared JSON serialization order; they were corrected
to compare object contents. A minimum-count case also prompted clamping smaller
explicit grids before saving. These are covered by the final passing tests.

Both APKs assembled successfully as 0.1.161. Ordinary output:
`launcher/app/build/outputs/apk/debug/app-debug.apk`, SHA-256
`14a5a41982bc83e2bc08173c9c0e2b12177ca1b39114885b1fd8b29e17dacc7d`.
The native APK and exact source archive are frozen under
`artifacts/native-dashboard/0.1.161/`, APK SHA-256
`b0bd943c1d642d60dfcad39a8d7d108d93c472d11411c350fcd7b29bf2b13139`.
Neither package was submitted or installed in this iteration. Build inputs include
the existing working-tree changes; the frozen source/hash record identifies the
native artifact precisely. The ordinary output has not been frozen for deployment.

## Original acceptance checklist · superseded by the follow-up above

- Check 100%/75%/50% preview, Save, Cancel, Back, interruption and relaunch.
- Inspect text/focus readability from the sofa on Cast1 and native Cast2.
- Add/move/resize widgets into the extra 16×8 space; phone Update/Undo must retain
  scale, spans and placements, including full-width widgets.
- Check touch/focus transforms, Favorites/app highlights and Android providers.
- Open each widget full-screen, then Back and visible Restore dashboard; verify
  the original page, focus, scale and saved widget size.
- Resume through a fresh coordinator-owned foreground session. No direct transport,
  network changes or FPS measurements belong to this change.

## Fleet deployment after reconnect · 0.1.162

Will confirmed network connectivity returned and requested deploy, commit and
push. `task deploy` advanced the version and froze ordinary Homeboard under
`artifacts/deployments/0.1.162/`, SHA-256
`2313233737d26ca2b98eb230813af37857ce78957dea532a031bb64d7028042e`.
Cast1 J‑1ec0d462024d, Cast2 J‑bc9ba453b79c and Pixel J‑395ec972f881 succeeded,
with the identical installed hash verified in each receipt. Tablet
J‑1db7b7ca2aff remains queued while unreachable.

Native 4K Dashboard Test passed container lint/assembly at the same version and
was frozen with exact sources under `artifacts/native-dashboard/0.1.162/`, SHA-256
`2874b8f7481e0e1595987822f0e0b4cc04476671b54a8b8c30d08013505abc51`.
Cast1 J‑0cfa4fae3c30, Cast2 J‑7d00177e41ab and Pixel J‑f1c2dbc077f0 all verify
that installed hash. TV launch continuations reported plugin-error; automatic
recoveries J‑c61222a3961f and J‑261a67b92161 completed with health ready. Pixel's
native launch continuation reported device-locked. These are separate from the
verified installs. Tablet J‑f161e5600393 remains queued. This session's older
tablet ordinary/native jobs were cancelled after replacement submission.

Cast2's native foreground session J‑38dbdd347b8a launched and captured the Home
dashboard and menus. Navigation did not reach the scale panel and the job ended
with target-left-foreground, health ready. The subsequent finish request was
rejected because the session had already ended. No scale choice was saved;
this is **not** a passing scale/Save/Cancel/Undo acceptance. Complete evidence is
in `docs/diagnostics/2026-11-25-scale-162-session/`. Preserve the device's current
foreground and saved scale; do not replay stale navigation as cleanup.

The complete deployment working tree passed 697 JVM tests and lint. The selected
scale-only commit was independently exported from the index and compiled for
both app modules in the container: **683 tests passed**, zero failures/errors.
This verifies that the commit does not depend on unfinished Trello files. The
deployed working-tree APKs retain their own exact source/hash records; the commit
excludes unrelated Trello/timepiece work.

[Plan and Cooper review](../plans/2026-11-23-native-4k-dashboard.md#dashboard-scale-implementation)
· [Design mockup and workflow](../plans/2026-11-25-dashboard-scale.svg)
· [User guide](../how-to/quick-start.md#fit-more-widgets-on-home)
