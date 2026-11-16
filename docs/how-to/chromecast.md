# Shared Chromecast testing

The adapter forwards to `/opt/wf-device-coordinator/bin/chromecast` using `WF_CC_*`
variables. Override `CC_CLIENT` for another installed coordinator CLI. The coordinator owns
installation, remote input, cleanup and evidence; this repository contains no device-control copy.

Before a session run `task chromecast:version`, `task chromecast:devices`,
`task chromecast:support DEVICE=<id>` and `task chromecast:queue`.
Build/freeze the intended APK and submit one complete coordinator-supported job with
`task chromecast:submit DEVICE=<id> APK=<absolute-apk> WORKFLOW=<reviewed-workflow>`.
Select the supported workflow for this package; do not assume an existing app validator applies.
Then use `task chromecast:watch JOB=<job>` and
`task chromecast:evidence JOB=<job> OUT=<absolute-output-directory>`.
Cancel only jobs owned by the current session using `task chromecast:cancel JOB=<job>`.

`task chromecast:check`, `profile`, `record`, `capture`, `readd`, and `message` are also
forwarded to the installed client. Their semantics and supported variables belong to that client.
The Task adapter exposes the common parameters; add mappings when a reviewed workflow needs them.
Never fall back to raw ADB, upstream install scripts or Gradle install tasks. If the client/service
is unavailable, stop device work. A sandbox socket denial warrants retrying the same Task command
with escalation, not bypassing the coordinator.

Creating this project does not select Homeboard as the default Home or screensaver on any device.

## Air-quality smoke test

After container compilation, register the app using `task chromecast:app-register`
with `APP=homeboard-tv`, the absolute `APK` path, and
`ACTIVITY=com.gathin.tvhome.MainActivity`. Registration stages APK metadata; it does
not install it. Then check devices and queue, and submit one job with the same APK,
`DEVICE=chromecast-test-02` and
`RECIPE=/absolute/homeboard-tv/dev/device/air-quality-check.json`.
The recipe exercises the card, actions menu and station picker, then returns to the
dock. Follow with watch and evidence. The coordinator restores the prior foreground;
this workflow does not set Homeboard as the default Home app.

Registration pins the APK hash. After rebuilding Homeboard, re-register its APK
with `REPLACE=true` before submitting; otherwise the coordinator rejects the new
artifact as a mismatch. This updates Homeboard's catalog entry, not another app.

## Widget Home acceptance recipes

The historical air-quality recipes target the superseded dock prototype. Current
recipes are `widget-home-check.json` (first-run air widget and empty Home),
`widget-app-management-from-air.json` (air-only Home and visible, unfavorited fixture
at the start), and `widget-uninstall-cancel.json` (fixture first in All Apps). These
are stateful remote scripts: inspect evidence and verify the documented starting
layout before each run. A completed coordinator job proves execution, not that the
UI matched every intended assertion.

`task fixture` builds the disposable `net.biohack.homeboard.fixture` app. Register
and install it through a complete coordinator job, then test launch and uninstall
using Homeboard's UI. Remove that fixture through Android confirmation after the
test; never substitute an existing user app. Final Homeboard APK branding uses
`ic_homeboard` and `homeboard_banner` resources.

### Cross-app input limitation discovered on 15 November

The installed coordinator's `check` input helper requires Homeboard to remain the
foreground package. Jobs `J‑f4c363380e28` and `J‑e54e97a2c155` stopped with
`WfKeyScore: Target app left foreground` when Homeboard correctly launched the
fixture. This is a helper guard, not a Homeboard crash. The coordinator restored
the prior foreground and recovered. The original all-in-one app-management recipes
are therefore unsuitable for this coordinator version. Use
`widget-move-delete-restore.json` with an existing fixture favorite to test the
in-app operations without crossing that boundary.

Do not run `widget-uninstall-cancel.json` until a supported coordinator workflow
can explicitly cover the Android confirmation activity and return to Homeboard.
Uninstall cancellation/removal still requires device validation. The disposable
fixture remains installed on chromecast-test-01 pending supported cleanup; do not
remove it using raw ADB or alter the shared coordinator to bypass its guard.

## Iterative install target

Use `task deploy DEVICE=chromecast-test-01` for a new Homeboard deployment.
It runs `task bump`, container checks and packaging, verifies matching version
metadata, freezes `artifacts/deployments/<major.minor.version_code>/homeboard.apk`,
then runs coordinator devices/queue, registration, submit, watch and evidence.
The About dialog displays that installed APK version. `RECIPE=/absolute/file.json`
selects a complete reviewed job instead of the default brief iteration check.
Never reuse a deployment number. Low-level coordinator tasks remain available
for reference capture, job control and evidence; manually assembled new installs
must follow the same bump/build/freeze rule.

Keep iterative installs on **Cast1** (`chromecast-test-01`) while Will is testing
Homeboard. Once Will says it is usable, move iterative testing to Cast2
(`chromecast-test-02`). After each working APK iteration, check devices and
queue, then register the new APK hash and install it through a complete
coordinator-owned job, preserving data/settings. Submit requested installs even
when Cast2 has a personal-use marker; the coordinator admits background installs
and defers foreground verification as needed. Devices/queue are status checks,
not an extra agent-side gate. Installation does not select Homeboard as the
default Home app. Additional targets in one deployment receive the same frozen
APK/version; rebuilds and new revisions need a fresh number.

Use `dev/device/iteration-check.json` for the current install/brief-launch cycle
while Will tests interactively. It sends no scripted navigation. Historical menu
recipes need revision before replaying after the App artwork row was added.

## Bounded Homeboard end-to-end smoke test

The current smoke test asks the coordinator to launch Homeboard, keep it running
for three seconds, and capture the TV display. The offline OCR oracle checks the
captured dashboard card for PM2.5, Bangkok and AirBKK, exercising public air-quality
data through the app's rendered UI. It does not change the default launcher. The
earlier Home → All Apps → Home recording attempt failed before producing captures:
the coordinator's foreground guard returned `target-left-foreground`. Do not count
that failed script as navigation evidence. After build, copy the APK to a new
private `artifacts/validation/` path, record its SHA-256/build provenance, and
register that frozen file. Submit `dev/device/homeboard-smoke.json`, watch to a
terminal state, export evidence, and run:

```sh
task chromecast:verify-evidence EVIDENCE=/absolute/path/to/evidence
```

The OCR oracle must find `PM2.5`, `Bangkok` and `AirBKK` in the screenshot's
dashboard-card crop. Exercise its failure path by requiring impossible text, such
as `unobtainium`, with `--expect-text`; that command must exit nonzero. Keep both
outcomes in the machine-readable run record. A coordinator job can complete with
an app-level assertion failure, so report job cleanup and OCR outcome separately.
