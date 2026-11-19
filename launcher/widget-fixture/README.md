# Widget feasibility probe

Stage 0 of the Android app widget plan. This separate APK reads installed providers
and the normal Android binding-approval resolver through public Android APIs.
It never allocates/binds a widget ID, starts setup, grants permissions, changes Home
or reads provider content. The initial census build contains no provider itself.

Build/lint in the normal container with `task widget-fixture`. Package:
`net.biohack.homeboard.widgetfixture`, activity `.CensusActivity`. Version 1 is the
immutable `0.0.1-census` build. New deployments of this package must increment its
own version code; Homeboard's deployment numbers remain independent.

Run only through coordinator registration and complete install/check jobs; the
check recipe is `dev/device/android-widgets-census.json`. Coordinator logcat
contains one `HB_WIDGET_CENSUS` provider record per component and a summary.
The complete report is also saved to the app-private `files/widget-census.json`;
there is no exported file provider or network server. Exported coordinator
screens/logs provide the stage-0 evidence without direct-device extraction.

`new-binding-unavailable` means no normal consent activity resolves for this fresh
host on the current device. It is not a failure to render and does not establish
that privileged launchers cannot host widgets. Follow the plan's per-device gate.

Remove this dedicated probe through a coordinator-supported Android uninstall
operation after diagnostics. Never uninstall Homeboard or user apps to clean it up.

## Version 2: bounded developer-grant experiment

Version 2 (`0.0.2-bind-test`) retains `.CensusActivity` and adds `.BindActivity` and
`.CleanupReceiver`. The registered `homeboard-widget-fixture` descriptor now points
to `.BindActivity`; use explicit replacement with the frozen v2 APK hash.

BindActivity allocates one ID in its own fixed host (8751), tries binding the
separate `net.biohack.homeboard.widgetprovider/.ProbeProvider`, and waits for the
provider's actual text and a laid-out host view. It draws that view into a visible
proof image, releases its widget ID, then logs `HB_WIDGET_BIND: result=...` with
bound/rendered/idsReleased. Placeholder content cannot establish rendering.
Interruption releases unfinished IDs; a new launch clears only this fixed host's
old IDs. The exported cleanup receiver is specific to this disposable APK and can
only delete this app's host; it returns `result=1, data="ids=0"` on verified cleanup.

The app cannot grant its own binding permission. The coordinator's explicit,
fixture-only `widget_bind_test` check option journals/temporarily grants/restores
that permission and invokes the cleanup receiver even after a failed test. The
baseline recipe omits the option. `android-widgets-bind-granted.json` requires the
reviewed coordinator capability installed first. Neither recipe changes the
Homeboard application or its default-Home role.
