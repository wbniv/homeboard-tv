# Enable the native dashboard's provider-host acceptance

**Resolved:** coordinator support was deployed in 0.1.238. Permission setup
J‑6794cfc8f663 passed; actual native provider hosting was tested on Cast2 on
26 November with coordinator 0.1.240, Remote J‑fbd4a4d2143b. Compact rendering,
move/resize into the extra 16×8 workspace, expansion, Back, visible Restore and
Remove/Undo retaining binding ID 3 passed. All test layout changes were undone,
preserving the original 50% dashboard and older history. Permission restoration
J‑6dd68068564f completed healthy, verified disabled; native host has zero widget
IDs remaining. See the
[acceptance record](../testing/2026-11-26-dashboard-scale-acceptance.md#native-provider-follow-up--evening).
The original request below is historical.

Will requested actual provider-widget testing in the native 4K dashboard on
26 November. Coordinator 0.1.236 still restricts `widget-binding` to normal
Homeboard packages. This prevents the required permission setup for
`homeboard-native-4k-proof` / `net.biohack.homeboard.dashboardproof`.

Requested narrow support: add that exact app/package pair to the typed workflow's
allowlist, preserving frozen APK hash checks, user 0, recorded prior setting,
verified readback and inverse recipe. Keep arbitrary packages rejected. Add
validation and cleanup-record tests covering the new pair and wrong alias/package
combinations; CLI/MCP/GUI should retain the same typed workflow semantics.
Deploy through the coordinator's reviewed maintenance workflow, preserving other
owners, enrollment and pending jobs. No direct grant or transport bypass.

## Prepared device request

`dev/device/native-4k-widget-binding-enable.json` targets the existing frozen APK:
`artifacts/native-dashboard/0.1.162/dashboard-proof.apk`, SHA-256
`2874b8f7481e0e1595987822f0e0b4cc04476671b54a8b8c30d08013505abc51`.

Submit on Cast2 using the normal devices/queue/submit/watch/evidence workflow.
Record the grant's prior value and restore that exact value using the resulting
inverse recipe after testing. Do not assume it was disabled or revoke an existing
user grant. The initial blocked submission changes no permission.

## Acceptance after support is deployed

Use an owned Cast2 Remote session on the real native activity. Preserve the saved
50% dashboard and existing Undo entries. Add the installed dedicated provider
fixture to a temporary page in the 16×8 grid. Capture compact RemoteViews in the
extra workspace, move/resize and verify saved bounds; short OK must show live
provider content and richer details. Verify Back and visible Restore return to
the same page, focus and saved span. Remove/Undo must retain its binding. Preserve
household widgets and release only test-owned IDs after they are no longer
referenced by saved layout/history. Restore the prior permission, original page,
50% scale and focus, then stop the owned session and export cleanup evidence.

This must run inside the native package. The already passing ordinary-app
AndroidWidgetProbeActivity is a control, not acceptance of SurfaceControl hosting.
