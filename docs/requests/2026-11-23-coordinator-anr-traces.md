# Coordinator request: retained ANR trace for a registered app

**Status: exact-exit passive collection passed on deployed coordinator 0.1.229; the original retained trace was available. The background-loading app fix passed Cast1 acceptance in 0.1.130; see the investigation record.**

## Evidence and requested outcome

Homeboard debug 0.1.128 on Cast1 had an ANR exit at 19:22:35.006 Asia/Bangkok on 23 November (PID 10163, ApplicationExitInfo reason 6, foreground importance 100). The initial owned Remote was `J‑f08d7bae2773`. Package diagnostics `J‑98cca1ded641` confirmed this exit and relaunched the app. Its logcat belongs to replacement PID 10662, starting around 19:24:23, so it cannot identify the blocked thread in PID 10163. Its skipped-frame messages and 825/869 ms frames are later startup observations, not the ANR's cause.

Passive follow-up `J‑52988aa6e960` completed at approximately 19:44 with no new exit records, foreground unchanged, health ready and verified cleanup. History still includes the original ANR and two older ANRs from that morning. This establishes that no subsequent ANR exit is recorded in that retained history; it does not establish that the original fault is fixed or reproducible. Evidence: `docs/diagnostics/chromecast-app-diagnostics-J-52988aa6e960/` and `docs/diagnostics/chromecast-app-diagnostics-J-98cca1ded641/` in Homeboard's private workspace.

The coordinator's `scripts/device-capture/WfPlatform.java::exits` reads process, PID, timestamp, reason, status, importance, PSS and RSS. It does not collect the exit description or retained trace. Please extend registered-package diagnostics to export the description and, when permitted and retained, the ANR trace for a specified exit identity. This should be a passive package-scoped read through the existing owned job, with no force stop, launch, data/history clearing, unrelated package scan or arbitrary path/shell operation.

Android's [ApplicationExitInfo reference](https://developer.android.com/reference/android/app/ApplicationExitInfo#getTraceInputStream()) documents the retained trace stream and notes that traces can be absent or overwritten in the circular buffer. Availability and permission on this Google TV build still require verification. This request does not assert that the shell helper can retrieve the trace merely because it can list exit reasons.

## Acceptance in the original request

- [ ] Target a registered app and exact process/PID/exit timestamp; retain attribution and the system description.
- [ ] Export a bounded trace artifact through ordinary owner-only evidence, with hash, byte count and truncation status. Mark absent, overwritten, denied, malformed and timed-out results explicitly.
- [ ] Preserve foreground, process, app preferences, enabled accessibility services and diagnostic history. In particular, launching diagnostics currently uses `am force-stop`, which removes Homeboard's helper from Android's enabled list on this TV; use the passive path for this investigation.
- [ ] Handle a main process and its package-owned child processes, missing trace, permission refusal, excessive size and transport failure in container tests. Preserve existing interface parity through app-diagnostics.
- [ ] On Cast1, retrieve the retained PID 10163 trace if available and identify the blocked main-thread stack and relevant dependency. If unavailable, report that result; a later reproduction needs deliberate scoped trace collection, with helper state restored and reverified afterward.

No Homeboard source fix is justified from an exit reason or a later skipped-frame message alone. Keep the ANR investigation open until a stack or equivalent causal evidence is available.


## Historical candidate and release review (superseded below)

The coordinator supplied branch `passive-anr-trace-20261123`, commit `59271389d47439dd29cb325c25211324210f3223`. It inherits the separate Remote close/recovery follow-up `7ffe94e50fcb0f461b094b83060db85976650b58`; both are outside deployed 0.1.226. Review and release preparation now uses an isolated clone at `/tmp/coordinator-passive-anr-release-20261123`. Targeted combined container checks passed 183 tests, and the independently rebuilt Android helper matches the candidate hash. Candidate version is 0.1.227; deployment and the exact-exit device request remain pending. This supersedes the initial implementation-gap status, not the ANR's unknown cause.


### Historical reviewed release attempt

Release source `release-passive-anr-20261123` at `586e020a653f5836d5f7ffdaf9821b9d2e1dc2c7` is preserved in the coordinator repository as a local branch. Candidate bundle: `/tmp/coordinator-passive-anr-0.1.227`; hash review: `/tmp/coordinator-passive-anr-0.1.227.review.json`. The pinned Python 3.14 container build and private dependency-import check passed, as did the 183-test combined regression. The reviewed upgrade was started through the administrator terminal at 20:37, preserving recovery; it is waiting for another owner's Cast1 Remote `J‑7c25d827bfe7` to finish. Service remains 0.1.226 until verified otherwise.

The exact passive query is armed to run once service/client report 0.1.227 at the exact reviewed revision. Its durable request/status and eventual submit/watch/export logs are under the private directory `docs/diagnostics/homeboard-retained-anr-20261123/`; `follow-up-status.json` identifies any submitted job. No query has run while the old release is live. The watcher selects no other exit, changes no app settings and sends no device input. A successful export still requires inspecting the actual trace state and any available stack.


### Superseding result: retained trace collected on 0.1.229

The 0.1.227 attempt failed its recovery-consistency check and was rolled back. Coordinator 0.1.229 (`14899926`) was subsequently verified live. The old revision-pinned watcher was stopped. Owned passive request `J‑f69f2e716471` selected the exact original exit and exported an untruncated 58,787-byte trace with verified hash and cleanup, foreground unchanged and no new exits. The original main thread was waiting for preferences to load in `AirQualityCard.legacyConfiguration` during `MainActivity.onCreate`; the preference loader was parsing XML. See the [retained-trace investigation](../testing/2026-11-23-homeboard-anr.md) for the dependency, private evidence, subsequent 0.1.130 app fix and acceptance limits. This is device acceptance of exact-exit collection, not a Homeboard fix or independent acceptance of unrelated Remote recovery changes.
