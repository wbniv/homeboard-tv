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

## Personal use and reservations: operation-specific policy

Use the shared [command policy matrix](../../../chromecast-coordinator/docs/plans/2026-11-16-nondisruptive-work-during-personal-use-command-matrix.md).
Personal use is not a blanket block on device work. Metadata/status/evidence
reads, current-screen screenshots, preview/live capture and safe owned cleanup
are permitted within their documented conditions. Passive inspection/recovery,
profiling and background APK installation have operation-specific preconditions.
Launch, scripted navigation and settings changes are separate foreground effects.
Consult the matrix for the actual command and parameters rather than inferring
permission from a device's reserved/paused badge.

Submit authorized install-only jobs independently of foreground verification,
including during another authorized owner's reservation or personal use. Do not
replace an unsuccessful install-only attempt with a held launch/check and call
installation "waiting for personal use to end." Keep the independent install
result and foreground queue status separate.

An admitted install can still fail execution preconditions. Report its exact
reason and evidence: `personal-install-in-use` is not a reservation denial, and
`personal-install-unverified` is not a pending installation. Never clear a hold or
use direct device commands as a workaround. Old plans and transcript advice to
wait before installing are superseded by this policy; historical receipts remain
records of what happened, not rules for future admission.

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

For the signed public package use `task deploy VARIANT=release`. It preserves the
same version/freeze/device workflow below while using the independent
`homeboard-tv-public` catalog entry and `net.biohack.homeboard` package. The normal
debug catalog entry is not replaced. Release jobs and later acceptance must use
`APP=homeboard-tv-public`; public and `.debug` preferences remain separate.
Fresh public installs can show Android's location-permission window. Complete that
choice through a coordinator-owned live Remote session before running an
app-only scripted check. Keep the foreground guard in place; a system prompt is
not evidence of an app crash. The shared Task include at
`/opt/wf-device-coordinator/Taskfile.chromecast.yml` provides the supported
`remote:start`, `remote:tap`, `remote:frame` and `remote:stop` operations. Export
Remote frames only under `docs/diagnostics/`, and finish the owned session with
coordinator cleanup.

Cast1 remains the default for routine foreground testing. Will's November 17
Cast2 availability is temporary and permits checks that specifically need Cast2;
it does not change that testing default. Installation targets and foreground
testing targets are separate choices.

Use `task deploy` for a new Homeboard deployment. It defaults to Cast1 and the Pixel
(Will, 2026‑11‑21: one of each device type; the tablet joins once the coordinator reports
`install ready` for it, and the Nest Hub Max cannot take an APK). Set `DEVICE=<id>` or a
comma-separated list for an explicit target override, such as Cast2.
Every target receives one identical frozen APK and version. All jobs are submitted
before watching either, and each target has its own evidence directory.
It runs `task bump`, container checks and packaging, verifies matching version
metadata, freezes `artifacts/deployments/<major.minor.version_code>/homeboard.apk`,
then runs coordinator devices/queue, registration, submit, watch and evidence.
The About dialog displays that installed APK version. `RECIPE=/absolute/file.json`
selects a complete reviewed job instead of the default brief iteration check.
Never reuse a deployment number. Low-level coordinator tasks remain available
for reference capture, job control and evidence; manually assembled new installs
must follow the same bump/build/freeze rule.

Deploy each working build to **Cast1 and the Pixel** (add other targets with `DEVICE=`; see the 2026‑11‑21 fleet note in AGENTS.md). Use Cast1 for routine
foreground acceptance, and let the coordinator defer foreground jobs on either TV
while Personal use is active.
After each working APK iteration, check devices and
queue, then register the new APK hash and install it through a complete
coordinator-owned job, preserving data/settings. Submit requested installs even
when Cast2 has a personal-use marker; the coordinator admits background installs
and defers foreground verification as needed. Devices/queue are status checks,
not an extra agent-side gate. Installation does not select Homeboard as the
default Home app. Additional targets in one deployment receive the same frozen
APK/version; rebuilds and new revisions need a fresh number.

### Parallel worktrees and one deployed application

Worktrees using `net.biohack.homeboard.debug` replace the same installed app;
their features do not accumulate on the TV. Merge the intended feature branches
before building the combined APK. Will assigned the Projectivy agent to merge
its work into `main` and own the next combined deployment after 0.1.9. This
workstream must not reinstall its older 0.1.8 artifact during that handoff.

Before the next bump, compare version codes and frozen deployments across the
participating worktrees. Use a code greater than every attempted deployment,
including failures. Preserve the Home favorites and density commits `be49bd7`
and `6a0366b` in the combined source. Record source revision, local changes,
frozen APK hash, coordinator job and installed-hash evidence. About's number
identifies an APK version; the source revision establishes its feature contents.

### Background install and foreground verification

For an additional target receiving an already frozen deployment, submit a
complete background install after devices/queue checks:

```sh
task chromecast:submit DEVICE=chromecast-test-02 APP=homeboard-tv APK=/absolute/frozen/homeboard.apk WORKFLOW=install
task chromecast:watch JOB=<job>
task chromecast:evidence JOB=<job> OUT=/absolute/evidence-directory
```

An install-and-launch `check` job is foreground work and can be deferred in its
entirety during personal use. An `install` job can be admitted in the background;
let the coordinator enforce its package/process checks. Submit launch verification
separately when required and let the coordinator defer that foreground job.
Keep install, launch and visual acceptance outcomes separate.

`personal-install-unverified` is a failed background verification, not merely a
queued install waiting for personal use to end. Export its evidence and report
that installation is unconfirmed. Do not substitute an older APK, remove the
personal-use marker, alter the default Home app, or fall back to direct device
commands. If a deferred job is superseded, cancel only the job owned by this
session before submitting its replacement.

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


### November 17 deployment preference

Will requested every working build on both Cast1 and Cast2 on November 17. On
2026‑11‑21 he superseded that with one of each device type, so `task deploy` now defaults
to Cast1 and the Pixel. `task deploy` performs bump/check/build/freeze
once and submits `dev/device/install-only.json` to each target. It does not release
Personal use. Installation and later foreground/visual verification are separate
coordinator jobs. The default of `task build` remains compilation only; use
`task deploy` for working app iterations.


## Coordinator-owned scripted Remote

For normal Android settings UI during a test, use `task chromecast:remote-start
DEVICE=<id>`, then `remote-tap JOB=<id> KEY=<key>` or `remote-hold` with
`MILLISECONDS`. `remote-frame` writes only under `docs/diagnostics`; finish with
`remote-stop` and export evidence. These forward to the coordinator's enrolled,
owner-checked Remote protocol. They do not use direct device transport. Preserve
unrelated settings and let the coordinator restore the prior foreground.

## Adaptive app input checks

For a coordinator-owned `record` job with `interactive: true` and `record: false`, use `task chromecast:input-hold JOB=<job> KEYS=DPAD_CENTER MILLISECONDS=100`, `input-frame` with an `OUT` PNG, `input-sample`, and `input-finish`. Wait for the `interacting` phase before input. Observe each result before sending dependent input; finish, watch and export evidence. The coordinator owns input and foreground checks throughout. These commands do not provide cross-app or raw transport exceptions.
