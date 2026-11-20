# Timers Stage 1 deployment attempt

`task deploy` built debug **0.1.111** from `f74da21`, including Stage 1 commit
`d6785fe`. The queue was checked first: both TVs were idle with nothing queued.
Another session's job appeared before submission; the coordinator serialized work.
Both targets used frozen APK SHA-256
`c20c01af039239e86df913b7d71f546659d8b7f352ba3af7df07c7a72b075104`.

Container checks passed: 644 JVM tests across 88 suites, Android lint, 888
English/Thai resources, 7 calendar tests, 71 cleanup tests and 5 deployment tests.
The JVM tasks reused valid cached results. `TimerTickerScheduleTest` has 5 tests,
`TimerIndicatorRulesTest` has 10 and `KitchenTimersTest` has 8, all passing.

| Device / operation | Job | Result |
|---|---|---|
| Cast1 background install | `J‑bce0318aedb3` | Failed `install-over-foreground`; no installation |
| Cast2 background install | `J‑3b372957ca6a` | Failed `install-refused`: `INSTALL_FAILED_UPDATE_INCOMPATIBLE` |
| Cast1 foreground install/check | `J‑6f6782536322` | Failed `install-refused`: `INSTALL_FAILED_UPDATE_INCOMPATIBLE`; launch/UI check not reached |

Receipts and installer evidence are in
`artifacts/deployments/0.1.111/<device>/evidence`; Cast1's follow-up is under
`chromecast-test-01/foreground-check/evidence`. `deployment.json` records the
separate attempts. Both devices retained APK hash
`6d761d7343f6c39f40e81c115848f2a73d2879a32ecb2866bb1ec50b53b49766`,
matching frozen **0.1.108**.

Container `apksigner` verified different signing certificates:

- Installed 0.1.108: `ee78e78e6bb55051d62e90927c0a938f162f8b8a6737828e1f29ea9df6c737af`.
- Attempted 0.1.111: `605ba2152237e18f2fdf235f3d75b6e888990d59108baa805b2f918442e93bc6`.

The active rootless Docker daemon's `homeboard-tv_gradle-cache` contains the latter
key, created November 20. The host debug key differs from both. The system Docker
daemon could not be inspected: socket access was denied and sudo requires interactive
authentication. The original key has not been located; the system Docker cache is
only a candidate. Recover the certificate-matching original key before building a
new numbered deployment. Keep 111 consumed. No uninstall or data reset was performed.

Follow-up recovery: Will authenticated the recovery terminal. System Docker and
the device-agent Docker daemon each reported zero volumes; system Docker also
lacks the Homeboard build image. The expected original cache was therefore not
available there. A search of accessible files under `/home/will` found the current
rootless key, the host debug key and unrelated development/release keys, with no
matching original key found. Recovery did not replace the active key. The terminal
log is `artifacts/debug-signing-recovery/recovery.log`. An original-key backup or
the location of the earlier build cache is now required for a data-preserving update.

The 0.1.111 attempt did not deploy Stage 1. The successful replacement below
supersedes its installation status; Cast1 timer acceptance is still open.

## New signing identity and successful 0.1.112 deployment

Will explicitly authorized resetting and reinstalling both TVs after the original
key could not be recovered. `task deploy` with a reviewed `replace-apk` recipe
built **0.1.112**, checked the exact installed 0.1.108 APK as its recovery input,
and submitted both replacements before watching them. Both succeeded:

| Target | Replacement job | Installed APK SHA-256 |
|---|---|---|
| Cast1 | `J‑29a51e42c02d` | `d43d90f391faf1188c409f6a3a03b1f52bbf9be87413a4c28927c962e4f3dc30` |
| Cast2 | `J‑55e41268720c` | `d43d90f391faf1188c409f6a3a03b1f52bbf9be87413a4c28927c962e4f3dc30` |

Each job's `replace-apk.json` verifies the old and new hashes. Evidence is under
`artifacts/deployments/0.1.112/<device>/evidence`. These were authorized data-reset
replacements, not ordinary data-preserving updates. Container checks and lint
passed again; 644 JVM tests passed using valid cached results.

The new certificate, verified with container `apksigner`, is
`dbb3446edcf5f2a66e810c266ee9195dce842e95d618d75529fc26c0d6caf3ff`.
The key and its second local copy live outside Docker caches; their bytes match.
See [durable signing setup](../how-to/build.md#shared-worktree-debug-signing).
Compose mounts the key directory read-only and the app explicitly selects the
saved keystore. CI generates a separate temporary identity. Missing-key behavior
was tested through `:app:validateSigningDebug --rerun-tasks` with an empty signing
directory: it failed with `Keystore file ... not found`, as required. The key must
also receive a protected off-machine backup; that has not been performed here.

## Cast1 post-install checks

The timer UI regression `J‑467ef8a96612` and ordinary launch check
`J‑122e185d713b` both stopped at `target-left-foreground`; their activity evidence
shows Google TV Home in front. The timer run produced no proof result. These are
failed checks, not successful timer rendering or countdown acceptance.

Passive diagnostics `J‑5566131af0a8` completed and preserved the foreground. Its
exit history shows `user-requested` process exits (reason 10), with no Java/native
crash entry; this does not establish the cause of the foreground changes. Evidence
is under `artifacts/deployments/0.1.112/chromecast-test-01/diagnostics/evidence`.
The ordinary launch receipt is under `launch-check/evidence`; timer evidence is
under `artifacts/timer-proof/0.1.112/ui/J-467ef8a96612/evidence`.

**Stage 1 is installed on both TVs. Cast1 timer acceptance remains pending, so
Stage 2 remains undispatched.** This record does not establish Stage 2 chip behavior.
