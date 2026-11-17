# Rain device-location default · 17 November 2026

## Implementation

An empty Rain location now defaults to automatic device location. An existing
foreground location grant starts resolution without another prompt or coordinate
confirmation. Fixed city/address/coordinate choices remain fixed. Automatic fixes
are cached separately from layout settings and do not create Undo history.

The shared Android lookup checks permission before cache access, coalesces Rain
and air-quality callers, validates coordinate range and original fix age, and
releases its listener on timeout, pause and destruction. No background-location
permission or Google account/API-key dependency was added. English and Thai copy
cover Automatic, permission, finding-location, unavailable, last-known and retry
states.

## Container validation

- 419 JVM tests passed, including eight new location/source policy tests.
- Six calendar pipeline tests and two deployment orchestration tests passed.
- All 462 English/Thai resource keys and format arguments match.
- Android lint passed with zero errors (302 warnings); debug APK assembly passed.
- The initial 0.1.24 attempt stopped at test compilation before producing a
  deployment. Its number was consumed, then 0.1.25 passed the corrected tests.

The two-target deploy regression checks verify that both jobs are submitted before
watching either, use the identical frozen APK and retain each target's evidence
even when the first watcher exits nonzero.

## Physical device evidence

Homeboard 0.1.25 installed and launched on Cast1 as **J‑b6a37b0e6cca**; its installed
SHA-256 matches the frozen APK. Cast2 background install **J‑83eea56781d1** connected
but was refused as `personal-install-unverified` before installer dispatch.

The first isolated probe **J‑6c237cfa5636** failed because a capture-only input script
produced an incomplete coordinator key score, not an app crash. Automatic recovery
completed. A single bounded Up press corrected that recipe.

Corrected isolated probe **J‑7572f7d7e71c** completed with cleanup. Captures in
`artifacts/deployments/0.1.25/cast1-rain-location-recheck/` were visually inspected:
Rain obtained a real device fix and live forecast with the existing grant, without
manual location selection. The 1×1 and Fill Home states and return from the fixed
Madrid example to Device location rendered. Its preferences were isolated from
Will's dashboard. Screenshot timing follows actual capture times; the report bar
identifies the actual stage rather than relying on the requested capture name.

The captures exposed chart time labels sharing the baseline with daily rows or
attribution. The chart now reserves a separate line before that content. This
source change produced newly numbered **0.1.26**, rather than replacing 0.1.25.

0.1.26 frozen APK SHA-256:
`6753e2ea743c8ddb8d686090fe701c56381c05e3eaf2cd437d33dc805e466a7c`.
Cast1 installation/launch **J‑7f7855d88551** succeeded with matching installed hash.
Cast2 install **J‑99f6503d5adb** was admitted during Personal use but refused with
the same ownership-verification error; no Cast2 installation of this build is
confirmed yet. Per-target evidence and source hashes are frozen with deployment
metadata at `artifacts/deployments/0.1.26/`.

## Remaining verification

A narrowly scoped coordinator ownership-proof fix passed 174 container tests and
is undergoing the reviewed maintenance deployment. It retains private package
metadata and, when a package dump identifies the exact package without the parsed
UID field, uses strict exact-package Package Manager UID proof. Shared/running
package and foreground protections remain. The original package output was not
saved, so its exact cause is still unconfirmed pending this diagnostic update.

Cast2 installation, final chart-spacing captures and foreground acceptance on
Cast2 remain pending. Denied/revoked permission, stale/future timestamps, retries,
cancellation and source serialization have automated policy coverage; shared TV
permission/location settings were not changed to manufacture those conditions.


## Coordinator retry evidence

Reviewed coordinator 0.1.174 (`07100597`) was deployed with keys, enrollment,
reservations and existing recovery state preserved. Cast2 retry **J‑07f515f668c2**
connected but still refused ownership verification, without installing. Its new
private diagnostic establishes the actual format: the package dump reports appId,
and Package Manager reports a comma-separated UID list for multiple Android users.
The strict single-number UID parser rejects that list. A further narrow parser
correction must verify that all listed UIDs map to the same Android app ID before
continuing the existing process/foreground safeguards. Cast2 remains on the prior
installed APK; 0.1.26 is confirmed only on Cast1 at this point.


## Deployment completed

Coordinator 0.1.175 (`2157cc5`) passed 177 tests and was deployed through the
reviewed maintenance helper with existing keys, enrollment, reservations and
recovery state retained. Cast2 background retry **J‑5ead5cfce69b** installed the
unchanged frozen 0.1.26 APK successfully during Personal use. Its
`apk-installations.json` reports `status: verified` and installed SHA-256
`6753e2ea743c8ddb8d686090fe701c56381c05e3eaf2cd437d33dc805e466a7c`, identical
to Cast1 and the frozen APK. Evidence: `artifacts/deployments/0.1.26/cast2-installed/`.

The job's foreground verify-launch continuation remains queued under the same
owner while Personal use is active. Installation is verified; Cast2 launch/visual
acceptance is deferred. The hold and default Home app were not changed. This
supersedes the earlier unconfirmed Cast2 installation status in this record.

Implementation commit `f30db99` was pushed to origin/main. All 358 entries in the
frozen source SHA-256 manifest matched the committed implementation before
separate calendar work began in the shared checkout. That later work is not in
this frozen APK.


Final normal-Home check **J‑19b8b84238a6** completed on Cast1 with cleanup. The
0.1.26 screenshot at `artifacts/deployments/0.1.26/cast1-main-final/screenshot.png`
was inspected: the existing dashboard displays Device location, a live next-24-hour
rain summary and chart without a setup prompt; chart tick labels are separated
from subsequent content at this saved size. This used the actual retained Home
settings rather than the isolated probe. Cast2 foreground acceptance remains
deferred, as recorded above.
