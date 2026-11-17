# First-page Rain and Favorites validation · 17 November 2026

The [approved layout plan](../plans/2026-11-16-first-page-rain-and-favorites-container.md)
is implemented in the current workspace. Rain and one bounded Favorites widget
are arranged on page 1 once, retaining favorite order and independent rain settings.

## Container verification

`task check` passed 411 JVM tests with no failures, six calendar pipeline checks,
parity for 455 English/Thai resources and Android lint with no errors. The new
`FavoritesContainerTest` covers:

- Existing shortcut conversion with ordered, unique app identities.
- Rain and Favorites on page 1 at 4/6/8/10/12 columns and 2/3/4 rows, without
  overlapping widgets or dropping other widgets.
- Adding 100 favorites without changing outer page count, panel spans or Rain's page.
- Removing/re-adding the panel, empty lists, serialization, Undo snapshots and
  confirmed package/activity updates.
- Independent container artwork without outer grid reflow.
- Vertical-grid and horizontal-ribbon focus boundaries, including incomplete rows.

The deployment task repeats checks against its newly numbered candidate. Debug APK
assembly and release bundle results will be recorded below once complete.

## Device status and evidence

Cast1 (`chromecast-test-01`) remains recovery-required. Coordinator reconnect job
`J‑9e5544ca5bc7` failed with `transport-unavailable`; wireless debugging advertised
an endpoint but ended the secure handshake. Evidence is exported at
`artifacts/favorites-reconnect-evidence/`. This does not establish an app failure or
prove a specific network/pairing cause. No direct-device fallback was attempted.

This session's older queued 0.1.22 job `J‑0e6901c495ce` was cancelled before the
replacement deployment, avoiding installation of a superseded build. Its final
receipt is exported at `artifacts/favorites-superseded-0.1.22/`.

The debug-only `FavoritesProbeActivity` uses isolated Home, air and rain preferences
and actual installed app identities. It exercises initial conversion/placement,
app focus, internal scrolling, header access, the 1×1 opener and larger browser,
and checks that neither scrollbar nor overscroll is enabled. The recipe is
`dev/device/favorites-container-review.json`.

Once the normal deployment completes, register the identical frozen APK as
`APP=homeboard-tv ACTIVITY=com.gathin.tvhome.FavoritesProbeActivity REPLACE=true`,
submit that recipe as a complete coordinator job, watch/export evidence, and
restore the catalog activity to `com.gathin.tvhome.MainActivity` afterward. Do not
change catalog activity while the normal deployment is still pending. This review
has not run; compilation of the probe does not establish device acceptance.

## Remaining device acceptance

- Inspect the actual first page with the user's saved favorites and rain settings.
- Confirm child focus, horizontal/vertical wedge visibility at start/middle/end,
  fully visible focused apps, and clean exits to other widgets/All Apps.
- Verify scroll/focus restoration after app launch/return, internal reorder versus
  outer Move/Resize, header/Back and full-size browser behavior.
- Check icon/banner variants, enlarged English/Thai text and Reduced motion at
  compact, tall, wide and Fill Home sizes.
- Exercise supported cross-app workflows; do not interpret the coordinator's
  foreground guard as a launcher crash or bypass it.

## Deployment and packaging

`task deploy DEVICE=chromecast-test-01` advanced the candidate to **0.1.23**,
repeated the passing container checks, built the APK and submitted the complete
install/brief-launch job **J‑aa72105a4d35**. It is **queued for Cast1 recovery**, not
confirmed installed. `task deploy` is watching the job; its normal evidence export
will run after the job reaches a terminal state. The current queued evidence is
exported at `artifacts/favorites-pending-0.1.23/`.

- Frozen APK: `artifacts/deployments/0.1.23/homeboard.apk`.
- SHA-256: `da03d53aa301cc21750a2aabf34bd42c34ce28e18a6b061438c67df07bf63232`.
- Base revision: `f16c73db376b48a82d51efcd2425fe957060c1a7`, plus this workspace's
  Favorites implementation. `artifacts/favorites-source-manifest.json` records
  all launcher source/build-input hashes including the new files.
- Machine-readable validation: `artifacts/favorites-validation.json`.
- Final lint: zero errors, 297 warnings; 411 JVM tests with zero failures/errors.
- `task bundle` passed. Unsigned release bundle: `launcher/app/build/outputs/bundle/release/app-release.aab`
  (1,460,989 bytes), SHA-256 `0b688e42a058b6d58618ba2a20acee067a46529050640a1610a1be5337dd8094`.
  No release signing or publication was performed.



## Cast2 deployment request · 17 November 2026

Will explicitly requested deployment to Cast2. Reused the **identical frozen
0.1.23 APK**, with no rebuild or version reuse for different bytes. Devices and
queue checks preceded submission.

Background install job **J‑42bf2124b88a** failed with
`personal-install-unverified`. It connected, but pre-install package verification
could not establish the existing application state; no successful install was
recorded. Evidence: `artifacts/deployments/0.1.23/cast2-install-evidence/`.

Normal install/brief-launch job **J‑800413b165cc** is **queued**, held by Cast2's
personal-use marker. The coordinator owns that admission decision. The marker,
default Home and app data were not changed. The queued evidence is exported at
`artifacts/deployments/0.1.23/cast2-launch-evidence/`; machine-readable outcomes
are in `artifacts/deployments/0.1.23/cast2-deployment.json`.

Cast2 installation and visual acceptance remain unconfirmed. End Personal use in
the coordinator when ready for the queued foreground install/check to run.


## Cast1 recovered: installation confirmed · 17 November 2026, 01:17

After coordinator recovery, job **J‑aa72105a4d35 completed successfully**. Exported
receipt: `artifacts/deployments/0.1.23/cast1-latest-evidence/`. The coordinator
verified the installed APK SHA-256 as
`da03d53aa301cc21750a2aabf34bd42c34ce28e18a6b061438c67df07bf63232`, matching frozen
0.1.23, and launched MainActivity. Cleanup succeeded with device health ready.

The screenshot was visually inspected: Home page 1 contains the Rain card, a
bounded Favorites panel with **nine retained apps**, and a **down wedge** without
a scrollbar. Rain is unconfigured and displays Set location. The screenshot alone
does not establish remote navigation, scrolled labels, launch/return behavior or
animation. The earlier connection blocker is resolved for this deployment.

Cast2 job **J‑800413b165cc remains queued for Personal use** as of the same check;
its installation is not confirmed. This supersedes the earlier queued Cast1 status.


## Cast2 retry · 17 November 2026, 09:34

A fresh devices/queue check still showed the Personal use hold. Submitted background
install **J‑8517777f8a29** with the same frozen 0.1.23 APK. Cancelled this session's
old queued foreground job **J‑800413b165cc** so it would not block that install.
The background attempt connected but again failed pre-install verification with
`personal-install-unverified`; no installation was confirmed. Evidence is in
`artifacts/deployments/0.1.23/cast2-retry-install-evidence/`. A replacement normal
install/check was then submitted, and Will was asked to turn off Personal use in
the coordinator GUI. No personal-use setting was changed by the agent.


## Reserved/background install verification follow-up · 17 November 2026

Reservations and Personal use already admit `install` plus authorized passive
workflows. The earlier advice to turn off Personal use treated the wrong layer as
the blocker. Failed install traces ended at process-list verification, before
installer dispatch. No reservation or personal-use hold was released.

An isolated coordinator fix preserves spaced process names when parsing UID/NAME
rows and retains bounded private process-query evidence. It still rejects
unverifiable ownership, shared packages, running target UIDs and visible targets.
144 coordinator tests passed. Reviewed coordinator **0.1.169**, revision
`66750c38bea970c9a474440ca5ed48ccab2d8154`, was deployed through the maintenance
helper with state, identities and reservations retained. The fix was prepared at
`/tmp/homeboard-coordinator-install-fix`; its tested source patch is also preserved
at `artifacts/coordinator-background-install-fix.patch`.

Post-update Cast2 install **J‑85c749cfd4d5** was admitted during Personal use, but
failed **before process parsing or installation** with `device-not-found-on-network`:
no current Wireless debugging endpoint was advertised. Evidence is exported at
`artifacts/cast2-background-install-0169/`. Thus the code change is tested in
containers and deployed, but its real-device parser outcome and Cast2 installation
remain unverified. Requested a check of Cast2's wake/debugging state. The hold was
not changed. Earlier foreground job J‑5462827c09f7 was cancelled by its owner before
non-disruptive inspection J‑cbc16c84f022 completed successfully during Personal use.

The current pending request is nondisruptive install **J‑96cea4399727**, using the
same frozen APK with retry enabled. Its hold is **waiting for connection**, not
Personal use. Current receipt: `artifacts/cast2-install-awaiting-connection/`.


## Confirmed Cast2 address · 17 November, 10:13

Will confirmed `192.168.4.27:36355`, matching the protected enrolled endpoint.
Discovery subsequently returned. Coordinator job **J‑96dde4049275** made three
connection attempts against the advertised address; each failed in under a second
with `transport-unavailable`. The coordinator infers possible expired debugging
authorization, but this evidence does not establish the cause. Exported evidence:
`artifacts/cast2-latest-connection/`. Personal use remains enabled.

The current background install is **J‑3111b6b27111**, queued waiting for connection,
using the unchanged frozen 0.1.23 APK. Installation on Cast2 is still unverified.

A separate coordinator candidate, revision `75fe9dd` (0.1.172), permits explicit
serial-verified saved-endpoint probes during Personal use; 148 container tests
passed. It is committed in `/tmp/homeboard-coordinator-connect-current` and is
**not deployed**. Cast1 has another owner's active test, and the present Cast2
refusal already occurs with discovery available. The installed coordinator remains
0.1.170, including the earlier process-name parser fix.
