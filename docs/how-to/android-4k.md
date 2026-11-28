# Render an Android View dashboard at 4K

Use this HOWTO when a TV receives a 3840 × 2160 signal but Android gives your app
an ordinary 1920 × 1080 UI. The aim is to render the actual interactive View
hierarchy at higher resolution while retaining its layout and remote controls.

Homeboard has demonstrated this in a separate engineering app on Android 14.
**Automatic native rendering is implemented in ordinary Homeboard 0.1.165.**
On Android 11+ TVs with active UHD output and an ordinary UI below UHD, the same
dashboard uses a bounded 3840×2160 embedded hardware hierarchy. Already-native
UHD UI, HD outputs and phones use ordinary rendering. Setup failure recreates
the ordinary dashboard without resetting saved layout; native allocation is not
retried in that process. Output-size changes re-evaluate the renderer, and text
scale/density changes rebuild its resources. No system density or HDMI mode is
changed to achieve this. See the
[production rollout checks](../testing/2026-11-26-automatic-4k.md). Follow the
[verification record](../testing/2026-11-25-native-4k-dashboard.md) for accepted
behavior and the [implementation plan](../plans/2026-11-23-native-4k-dashboard.md)
for remaining work. Examples here match the repository's compile SDK 34; the
direct embedding module requires Android 11 / API 30 or newer.

In **Home settings → About**, read **Dashboard rendering** separately from
**TV output**. The normal launcher and proof also show the observed
**Rendering surface** size.
For native 4K, its measured dashboard and surface should both read
**3840 × 2160 (4K)**. Ordinary Homeboard can show a 1920 × 1080 dashboard while
the TV output reads 3840 × 2160; the output alone does not establish 4K rendering.

For more widget capacity, open **Home settings → Appearance → Dashboard scale**.
On UHD-capable TVs, **100%** means the full native workspace (typically 16×8),
with **150%** and **200%** for larger widgets. Build 0.1.170 corrected the old
50% label to native 100%; existing widget sizes and positions are preserved.
Fresh UHD layouts start at 100%. HD-only devices retain their own 100% baseline
and 75%/50% choices. Settings and expanded widgets keep their normal size.
Workspace scale is independent of the rendering buffer and changes no Android
display density. See the [user guide](quick-start.md#fit-more-widgets-on-home),
[scale acceptance](../testing/2026-11-26-dashboard-scale-acceptance.md) and
[actual 0.1.170 native 100% preview](../testing/2026-11-26-automatic-4k.md).

## 1 Measure the four resolutions

| Measurement | What it answers | Where to obtain it |
| --- | --- | --- |
| Active output mode | What resolution and refresh rate is the source sending? | `Display.getMode()` and the coordinator's display report. |
| Logical UI and window | What size does Android normally give the app? | Activity resource metrics, measured decor/window bounds. |
| Rendering buffer and hierarchy | How many pixels does this implementation actually draw? | Surface callbacks and measured root dimensions after layout. |
| Physical detail | Does the complete output path resolve individual pixels? | Independent compositor/output evidence or direct panel observation. |

Record all four separately. A supported-mode entry means the connection offers
that input mode; its ID is an identifier, not a sequential row number. Neither a
4K HDMI mode nor an enlarged screenshot proves a native 4K dashboard.

Android documents that a SurfaceView can reach full display resolution on TVs
whose ordinary UI layer has lower resolution. This is the useful starting point,
not a guarantee that an entire app will work inside it.
[Android surface guidance](https://developer.android.com/media/media3/ui/surface).

For an Activity, basic diagnostic measurements look like:

```java
Display display = getWindowManager().getDefaultDisplay();
Display.Mode active = display.getMode();
Display.Mode[] offered = display.getSupportedModes();
DisplayMetrics logical = getResources().getDisplayMetrics();

// Log before substituting rendering resources:
// logical.widthPixels, logical.heightPixels, logical.densityDpi
// active.getPhysicalWidth(), active.getPhysicalHeight(), active.getRefreshRate()
// Every offered mode's ID, width, height and refresh rate.
```

The accessor above matches the existing proof; adapt display/window discovery to
your target SDK and windowing environment. Orient mode dimensions to the actual
window. The TV proof is landscape; phone rotation and multi-window require their
own handling. Read the active mode again after an output change.

Do not use global `wm size` or `wm density` changes to make the application appear
4K. Keep a diagnostic's requested mode, accepted mode and allocated buffer distinct.
The standalone resolution tester
is documented in the [tester sections of the plan](../plans/2026-11-23-native-4k-dashboard.md#standalone-tester).

## 2 Choose the rendering path

| Path | Appropriate use and result here |
| --- | --- |
| Ordinary Activity hierarchy | Use directly when Android already supplies the required native UI dimensions. |
| SurfaceView with a hardware Canvas | Useful for pixel patterns or a renderer that already draws its own scene. Allocating a 4K buffer does not transplant an interactive app. |
| VirtualDisplay plus Presentation | Hosted the real Homeboard hierarchy at 4K. Required explicit window/context and forwarded-focus handling. |
| Activity launched on a private display | Check platform support first. Cast2 reported secondary-display activities unsupported, so this candidate was rejected. |
| SurfaceControlViewHost embedded in SurfaceView | Rendered the actual hierarchy at 4K without using a virtual display as the composition output. This is the production embedding path as well as the direct-embedding proof. |

Do not substitute a screenshot, scaled bitmap or a hand-redrawn facsimile for the
real dashboard. Preserve its actual text, charts, app icons, widgets, menus and
callbacks so the proof exercises the code you intend to ship.

An Activity display request can be ignored or rejected depending on platform
support and admission. Treat that as a failed candidate rather than falling back
to a misleading resolution claim.
[ActivityOptions display contract](https://developer.android.com/reference/android/app/ActivityOptions#setLaunchDisplayId(int)).

## 3 Preserve layout size while increasing pixel density

Double the pixel dimensions and rendering density together when moving a fixed
layout from 1080p to 4K:

| Rendering target | Density | Width available in dp |
| --- | ---: | ---: |
| 1920 × 1080 | 320 dpi | 960 dp |
| 3840 × 2160 | 640 dpi | 960 dp |

The relationship for this uniform-scale example is:

```text
scale = targetWidthPixels / originalWidthPixels
renderDensityDpi = originalDensityDpi × scale
widthDp = widthPixels × 160 / densityDpi
```

Increasing dimensions alone changes the layout's available dp. Increasing density
while leaving content in the physical 1080p window produces oversized controls.
Apply the rendering resources consistently to the actual hierarchy.

The proof keeps separate rendering Resources with copied metrics/configuration;
it does not update system density or mutate shared global Resources. That is a
prototype technique, not a complete resource-management solution for arbitrary
locales, window sizes and configuration changes. See the implementation before
adapting it:
[Native4kEmbeddedDashboardActivity.java](../../launcher/dashboard-proof/src/main/java/com/gathin/tvhome/Native4kEmbeddedDashboardActivity.java).

Keep text sizes in sp and preserve the user's font settings. The proof's manual
metric scaling still needs large-text acceptance. Android deprecated
`DisplayMetrics.scaledDensity` in API 34 because font scaling can be nonlinear;
use SDK-supported sp conversion rather than treating font scale as a universal
linear multiplier.
[DisplayMetrics font-scaling guidance](https://developer.android.com/reference/android/util/DisplayMetrics#scaledDensity).

## 4 Embed the actual hierarchy

The direct path is:

```text
Physical Activity window
  └─ SurfaceView with a native-size buffer
      └─ SurfaceControlViewHost surface package
          └─ Input bridge and overlay plane
              └─ Actual dashboard hierarchy
```

Create the physical SurfaceView, request its native buffer size and wait for a
valid attached surface/host token. The following is the core attachment fragment
from the proof, not a standalone Activity:

```java
surface = new SurfaceView(getBaseContext());
surface.setZOrderOnTop(true);
// Install surface in the physical Activity window, not in the embedded root.
surface.getHolder().addCallback(this);
surface.getHolder().setFixedSize(targetWidth, targetHeight);

// In surfaceChanged, after validating dimensions and the host token:
viewHost = new SurfaceControlViewHost(
        this, physicalDisplay, surface.getHostToken());
viewHost.setView(actualDashboardRoot, targetWidth, targetHeight);
surface.setChildSurfacePackage(viewHost.getSurfacePackage());
```

Construct the dashboard Views with the rendering resources established first.
Keep the physical SurfaceView's context separate. If you override `setContentView`
to capture the real dashboard root, provide a non-recursive route to install the
physical SurfaceView. Log the root size after layout, not at construction time.

Size grids from their actual containing hierarchy once measured. In the Cast2
embedded proof, Android later reported resource dimensions of 1920 × 1080 while
retaining density 4 and a 3840 × 2160 root. Dividing those resource dimensions by
density incorrectly collapsed All Apps to four columns and one row. Resource
dimensions are only a pre-layout fallback for the grid, not its attached bounds.

Use `setChildSurfacePackage` rather than manually reparenting the root surface:
the API links the embedded accessibility hierarchy. SurfaceView manages the
package's reparenting as its surface comes and goes; your code still owns and must
release the SurfaceControlViewHost. These API facilities do not establish TalkBack
or touch acceptance for your particular app.
[SurfaceControlViewHost](https://developer.android.com/reference/android/view/SurfaceControlViewHost),
[SurfaceView embedding](https://developer.android.com/reference/android/view/SurfaceView#setChildSurfacePackage(android.view.SurfaceControlViewHost.SurfacePackage)).

The host-token constructor and method names above match compile SDK 34. Check
current API signatures and deprecations when porting to a newer SDK.

## 5 Route input through the application's real behavior

Embedding Views does not automatically reproduce an Activity's custom key logic.
Homeboard handles page navigation, long OK, Undo and widget actions in MainActivity.
The embedded root therefore forwards keys to that existing handler. When the
handler delegates to its window, dispatch locally into the embedded tree without
calling the forwarding override again. Otherwise you can create a dispatch loop.

For forwarded Presentation/Dialog windows, an unhandled D-pad event may need
explicit focus search: forwarding a key to a window is not the same as running
its ViewRootImpl input pipeline. First let the focused content consume the event;
only then search for the next focus target on key-down. Custom chart Left/Right
handling must retain priority. Constrain modal focus search to the modal itself.

Test short presses, holds, repeats, cancelled releases, scrolling, nested detail,
Back and the visible Restore action. Confirm the original page, focus and saved
widget size after restoration. Test touch and keyboard/IME separately; a passing
D-pad recipe does not establish them.

## 6 Keep dialogs on the rendering plane

A normal Dialog opened with the physical Activity context can remain on the
1080p display while using 4K-sized resources. That produced oversized Rain controls
in the first full-dashboard experiment.

Two approaches were exercised:

- The Presentation candidate gives its dialogs the presentation context and the
  appropriate private-presentation window type, then forwards input to the active
  dialog. This is specific to the app-owned display.
- The direct embedded candidate mounts modal content in an overlay plane inside
  the same native hierarchy. Shared expansion/editor code keeps its normal
  Dialog behavior when no embedded overlay plane exists.

The implementation is in
[DashboardDialog.java](../../launcher/app/src/main/java/com/gathin/tvhome/DashboardDialog.java)
and [DashboardDialogs.java](../../launcher/app/src/main/java/com/gathin/tvhome/DashboardDialogs.java).
It preserves existing content and callbacks rather than maintaining a second
full-screen widget design. Verify sizing, dismissal, draft Save/Cancel, focus and
accessibility isolation for every modal type.

## 7 Handle lifecycle, recovery and resource cost

Retain ownership of the view host and release it when the Activity is destroyed.
Do not release the surface package a second time after transferring its ownership
to SurfaceView. Handle attachment failure explicitly; do not label an ordinary
fallback window as native 4K.

Exercise surface destruction/recreation, background return, actual process death,
output changes and memory pressure. Save navigation and expanded-widget state
independently of rendering objects. A production implementation must re-evaluate
output size and safely choose the ordinary path when the native path is unavailable.
Production selection/recovery is implemented in
[NativeDashboardActivity.java](../../launcher/app/src/main/java/com/gathin/tvhome/NativeDashboardActivity.java)
and [NativeDashboardHost.java](../../launcher/app/src/main/java/com/gathin/tvhome/NativeDashboardHost.java).
The separate proof activities retain their explicit comparison paths.

One 3840 × 2160 four-byte pixel buffer is about 31.6 MiB. Multiple buffers, decoded
images, transitions and compositor allocations increase the total. Budget for
those explicitly. Package PSS alone does not account for every graphics buffer.
Keep image decode targets bounded; test animation and video separately before
raising asset limits. Under the native renderer Homeboard decodes still provider images for
up to 3840 × 2160 (one hardware buffer, about +10 MiB PSS over the 1080p path on Cast2) when
`ActivityManager` memory admits it, and falls back to 1080p on refusal or failure; animated images
stay at 1080p and 4K video plays at its stream size. Measurements and the readback method are in
[4K wallpapers and video](../plans/2026-11-28-4k-wallpapers-and-video.md).

Measure frame timing, input response, memory and thermal behavior against the
ordinary renderer on the same hardware and comparable state. A missing windowless
frame series is **unmeasured**, not zero jank. Our direct embedded hierarchy was
not included in the collector's per-window rows; process aggregates still showed
slow frames. No performance improvement has been accepted. Performance characterization
continues separately and does not gate native 4K deployment, per Will's direction.

## 8 Reproduce the Homeboard proof

Open **Homeboard 4K Dashboard Test**, the purple dashboard-grid icon. This is a
separate app from Homeboard TV and the striped Homeboard 4K Tester. Menu exits.
The proof has independent settings and does not become the default Home app.

For a source build, use the existing container environment:

```sh
task build-image  # First setup, or when the build image changes.
task check

task run -- ./gradlew --no-daemon \
  :dashboard-proof:lintDebug :dashboard-proof:assembleDebug
```

This produces
`launcher/dashboard-proof/build/outputs/apk/debug/dashboard-proof-debug.apk`.
A new deployment needs a newly advanced version before building, then a frozen APK,
exact source inputs and hashes. Use the [build guide](build.md) and
[coordinator workflow](chromecast.md); never install through Gradle or raw ADB.

To reproduce the already frozen 0.1.149 rendering check on this workspace:

```sh
task chromecast:version
task chromecast:devices
task chromecast:support DEVICE=chromecast-test-02
task chromecast:queue

task chromecast:submit \
  DEVICE=chromecast-test-02 APP=homeboard-native-4k-proof \
  APK=/home/will/homeboard-tv/artifacts/native-dashboard/0.1.149/dashboard-proof.apk \
  RECIPE=/home/will/homeboard-tv/dev/device/native-4k-dashboard-proof.json

# Replace JOB_ID with the ID returned above.
task chromecast:watch JOB=JOB_ID
task chromecast:evidence JOB=JOB_ID OUT=/absolute/evidence-directory
```

The existing catalog entry pins this package, activity and APK hash. If it has
moved to a newer build, use that deployment's frozen artifact and record instead.
Do not downgrade it or replace another owner's queued artifact to replay an old
example. For a fresh catalog, register the intended frozen APK with
`task chromecast:app-register`, using activity
`com.gathin.tvhome.Native4kEmbeddedDashboardActivity` and the separate proof app ID.

Navigation recipes require the explicit `Native4kEmbeddedAcceptance` alias to
establish Home/Rain focus. Regular icon launches retain focus and may begin on a
different control. Review starting-state requirements before submitting a script;
completed jobs alone do not prove the intended UI was reached. Keep install hash
verification separate from launcher-artwork and foreground acceptance.

## Acceptance checklist

- Record active output, logical window, buffer and measured hierarchy dimensions.
- Verify physical pixel detail independently of a scaled capture.
- Exercise the real Home, All Apps, menus and every widget's richer full screen.
- Verify Back and Restore preserve page, focus and saved size.
- Cover live app widgets, video/wallpaper, touch, IME, Thai and larger text.
- Check screen-reader traversal, modal isolation and focus restoration.
- Measure comparable frame timing, input response and total resource cost.
- Test output/lifecycle/allocation recovery and a 1080p control.
- Enable automatic selection only for a path that has passed those checks.

For exact passed checks, failed candidates and retained evidence, use the
[25 November verification record](../testing/2026-11-25-native-4k-dashboard.md).
