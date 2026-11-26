# Reduced-scale acceptance · 26 November 2026

**Evening update:** native 4K provider-host acceptance now passes for the dedicated
fixture. The earlier permission blocker below is resolved; see the follow-up.

Will authorized Undo, phone editing, provider widgets and full-screen return
checks, preserving Cast2's saved 50% dashboard. Baseline: native dashboard
0.1.162, 16×8 grid, Rain/Calendar/Air/Favorites on Home page 1. Current device
control used owned coordinator Remotes J‑bac2402572e0 and J‑3b8972038efe. No default Home,
network settings or household credentials are changed.

**Later baseline correction:** 0.1.170 calls this same UHD workspace **100%**,
with 150%/200% for larger widgets. The 50%/75% labels in the dated checks below
are the original labels, not current UHD instructions. See the
[automatic 4K rollout](2026-11-26-automatic-4k.md).

## Native Cast2 results

- 75% preview renders 11×5; Back cancels and reopening still shows saved 50%/16×8.
- Save temporary 100%, then Undo history → newest Dashboard scale entry restores
  the original 50% grid, widget sizes and placements. Older Undo entries retained.
- Native Rain, Calendar and Air expand at normal size. Both Back and the visible
  Restore dashboard action return to page 1 with the originating widget focused
  and the saved 50% sizes. Screenshots are under `docs/diagnostics/scale-acceptance-*.png`.
- Favorites expansion shows larger app artwork and version details. Both Back
  and visible Restore return to the original Favorites/WF Clocks focus and size.
- Both owned Remotes were explicitly stopped with no unresolved effects. The final
  capture confirms the original 50% Home layout and WF Clocks focus. Useful images
  are retained in [the evidence folder](2026-11-26-dashboard-scale/), including
  [the final dashboard](2026-11-26-dashboard-scale/native-final-50.png).

## Phone transport and isolated probes

Cast2's product editor opens at its LAN address, but both a container browser and
host HTTP connection were refused. A normal editor Stop/reopen did not change the
result. The shared coordinator operations guide documents that direct host
traffic to enrolled devices is restricted; this does not establish a product
server failure. No firewall bypass, alternate device transport or network change
is permitted or attempted. An external phone/browser round trip is not yet passed.

The existing debug-only PhoneEditorProbeActivity exercises the actual Android LAN
listener, Origin/session checks, visible TV approval, mixed Apply/retry/preview/
Undo/Stop from an on-device client. Its isolated preferences now start at 50%/16×8.
Rain moves to zero-based column 9, row 4, span 6×2, beyond the old 8×4 bounds.
Job **J‑cf3a2513a19c** completed healthy and explicitly logged `PHONE_PASS` for
these operations, unchanged scale and composite Undo restoration. This verifies
the real protocol and Android transaction, separately from external reachability.
[Final Android report](2026-11-26-dashboard-scale/android-phone-pass.png).

The actual production phone HTML/JS/CSS also passed in container Chromium/Pixel 7
and WebKit/iPhone 13 against a controlled API fixture: 16×8 placement, move/resize
beyond the old bounds, unsaved preview/cancel, Update and Undo, with no script
errors or horizontal overflow. [Chromium](2026-11-26-dashboard-scale/chromium-phone-moved.png)
and [WebKit](2026-11-26-dashboard-scale/webkit-phone-moved.png) captures are saved.
Reproduce with `dev/scale-phone-browser-check.cjs` in the Playwright container,
`mcr.microsoft.com/playwright:v1.59.1-noble`, mounting the repo at `/workspace`.

The existing AndroidWidgetProbeActivity uses separate preferences and host ID
8724. At 50%/16×8 it passed moving/resizing a real fixture RemoteViews widget to
page 2, zero-based column 10, row 4, span 4×2. Short OK, expanded content,
visible Restore and Back, phone Remove/Undo with the same binding, disposable ID
release and unchanged household preferences all passed. Job **J‑92b1eb458362**
completed healthy with an explicit [machine pass result](2026-11-26-dashboard-scale/android-provider-result.json).
Its capture schedule began after expansion had finished: the screenshots show
the restored compact widget, while assertions inspected the actual expanded dialog
and RemoteViews. Do not label those screenshots as expanded captures.

At this earlier checkpoint, these probes were controls in ordinary Homeboard
and **native 4K provider hosting was still unverified** (subsequently passed
for the fixture in the evening follow-up below): the native package displays Widget permission setup, Android's
consent activity is unavailable, and the coordinator widget-binding workflow only
accepts normal Homeboard packages. The native request was rejected before any job
or permission change. No allowlist modification or raw grant was attempted. Cast1
already allowed normal-package binding; no binding permission was changed here.

The two debug-only probes and `dev/device/scale-phone-editor-check.json` /
`dev/device/scale-provider-check.json` preserve this reproduction. Each probe was
temporarily registered as the normal app's activity; **MainActivity is restored
in the catalog**. Complete evidence is in
`docs/diagnostics/2026-11-26-scale-phone-final/` and
`docs/diagnostics/2026-11-26-scale-provider-final/`.

Initial phone job J‑bc9cba4c0060 failed in coordinator key-score scheduling because
its capture-only recipe had no key events; automatic recovery completed. Recipes
now include a harmless directional event after probe completion. A completed job
alone is not treated as a probe pass.

## Build, deployment and remaining scope

Container check/build passed with 697 JVM tests. Ordinary **0.1.163** includes
the revised debug probes and pre-existing working-tree changes recorded in its
manifest; it is not a clean scale-only release. Frozen APK:
`artifacts/deployments/0.1.163/homeboard.apk`, SHA-256
`c6046f346d8ba171295e2922d9de68ecb0564a88f3e00267b24667f826c0a03f`.

`task deploy` verified this hash on Cast1 J‑e87ab57e0a57, Cast2 J‑ccc77d43da7b
and Pixel J‑65cd19283138. Pixel's overall job failed with `device-locked` after the verified install;
this is not foreground acceptance. Ordinary tablet J‑f6674175f7d8 remains queued
offline; superseded ordinary 0.1.162 J‑1db7b7ca2aff was cancelled. Native 0.1.162
is unchanged, with tablet J‑f161e5600393 still queued. Both probe jobs and final
Remote cleanup report healthy devices.

No production scaling defect was reproduced. Remaining: external phone/browser
round trip, broader provider compatibility,
text/accessibility and lifecycle/output checks. FPS belongs to separate measurement
work. The four tested dashboard widgets do not establish every widget type's
acceptance or native one-pixel delivery.

## Native provider follow-up · evening

Coordinator 0.1.238 added the exact native package to the typed permission workflow.
The coordinator's J‑6794cfc8f663 verified `before: false`, `target: true` for
`net.biohack.homeboard.dashboardproof`; that grant was left for this acceptance.
This test used coordinator 0.1.240 and the unchanged frozen native 0.1.162 APK.

Owned Cast2 Remote **J‑fbd4a4d2143b**, approximately 20:05–20:13 Bangkok:

- The installed separate-APK fixture bound as **ID 3** and rendered actual
  RemoteViews in the native embedded dashboard at saved 50% scale.
  [Initial 2×2 widget](2026-11-26-dashboard-scale/native-provider-added.png).
- Resize to **4×2**, then move to zero-based **column 10, row 4, page 0** passed:
  the provider occupied space outside the original 8×4 grid. Text was clipped at
  the initial minimum size but readable after resizing. Existing widgets stayed
  in place. [Resized](2026-11-26-dashboard-scale/native-provider-resized.png),
  [moved](2026-11-26-dashboard-scale/native-provider-moved.png).
- Short OK opened the actual provider view plus app/version and saved 4×2 details
  at normal full-screen size. [Expanded](2026-11-26-dashboard-scale/native-provider-expanded.png).
- Both [Back](2026-11-26-dashboard-scale/native-provider-back.png) and the
  [visible Restore action](2026-11-26-dashboard-scale/native-provider-restore-focus.png)
  returned to Home page 1, provider focus and the same 4×2 position.
- Delete removed the fixture; Undo restored **the same ID 3**, text, size and
  placement. [Restored binding](2026-11-26-dashboard-scale/native-provider-undone.png).
  Undo returns focus to the settings control used to invoke it, so that image
  is not a claim that Undo focused the provider.
- Undo through the test Add entry removed all three remaining test transactions
  (Move, Resize, Add), preserving older history. Original 50% dashboard and
  Favorites/WF Clocks focus restored. [Final layout](2026-11-26-dashboard-scale/native-provider-final-layout.png).
  No app update, system display setting or household widget configuration changed.

The Remote was explicitly stopped; cleanup reports no unresolved effects.
Full evidence: `docs/diagnostics/2026-11-26-native-provider-acceptance/`.
Permission restoration to the recorded original **false** completed healthy as
**J‑6dd68068564f**. Its `widget-binding.json` reports `target: false`,
`verified: true`; the post-change appwidget dump shows native host 8723 with
**widgets.size=0**, proving the test binding was released. Evidence:
`docs/diagnostics/2026-11-26-native-provider-permission-restored/`.
This passes the dedicated provider fixture in the native package, not every
third-party provider's configuration, interaction or resize behavior.
