# Native dashboard rendering proof

This separate engineering app runs the real Homeboard dashboard sources and
resources at the current output resolution. It has its own data and settings,
application ID `net.biohack.homeboard.dashboardproof`, and no Home intent or boot
receiver filters. Its launcher label is **Homeboard 4K Dashboard Test**.

For the rendering approach, input/window handling and reproduction steps, see the
[Android 4K HOWTO](../../docs/how-to/android-4k.md).

The launcher opens the direct SurfaceControlViewHost candidate. The older
Presentation comparison remains available by explicit activity for engineering.
Menu exits either proof. Regular icon launches retain navigation; the exported
`Native4kEmbeddedAcceptance` and `Native4kPresentationAcceptance` aliases establish
Home/Rain focus for deterministic coordinator recipes. They reset navigation only
in this proof package on a fresh launch, not household Homeboard data.

Android 11 or newer is required for the direct embedding API. The normal Homeboard
app retains its own platform support and does not enable native rendering from
this proof's presence. See the [plan](../../docs/plans/2026-11-23-native-4k-dashboard.md)
and [verification record](../../docs/testing/2026-11-25-native-4k-dashboard.md).

All build commands run in the build container:

```sh
task run -- ./gradlew --no-daemon :dashboard-proof:lintDebug :dashboard-proof:assembleDebug
```

For a new install, advance the shared version with `task bump` before building,
freeze the APK and exact source inputs, then use coordinator registration, install,
watch and evidence. Never use Gradle install or raw ADB. Deployment artifacts for
this investigation are under `artifacts/native-dashboard/<version>/`. Registering
a duplicate app entry for the same package is not supported; keep the normal
Homeboard catalog entry unchanged. Cancel only this session's superseded queued
proof jobs before replacing their artifact.
