# Recorded Remote customer test, 23 November 2026

Coordinator handoff: `/home/will/chromecast-coordinator/docs/handoffs/2026-11-23-homeboard-remote-capture-customer-test.md`.

## Setup and preliminary results

- [x] Installed coordinator and client report 0.1.226 (`6030fd88`). Cast1 is Chromecast HD, Android 14, output 1920×1080 at 59.94 Hz. Homeboard About reports 0.1.128; test package is `net.biohack.homeboard.debug`.
- [x] Initial Remote preflight `J‑f08d7bae2773` found Homeboard failed to stay open when launched through its Google TV tile. Registered-package diagnostics `J‑98cca1ded641` subsequently reported an ANR exit at 19:22:35 local, matching the preflight period. The diagnostics job explicitly relaunched the debug app and completed; this does not diagnose the ANR's cause. Public-package passive diagnostics `J‑7ff9ce80ba9e` showed no new exit during its collection.
- [x] Recorded preflight `J‑d69c7238e891` opened through the **installed shared Taskfile** with `CAPTURE_DURATION=120`. Homeboard showed On / Setup needed. No Home transition is claimed from this run.
- [x] Finished recorded preflight normally; downloaded all 15 artifacts. Readiness reports a decoded startup frame; 84 indexed frames all map to the MP4, with no incomplete tail, nonmonotonic timestamps or dropped leading frames. MP4 decoded in the container using passthrough and a microsecond output time base. The manifest records server termination and forward removal; terminal receipt reports no unresolved effects. Timing remains unavailable. Static intervals and the host-derived final frame hold do not establish unseen-frame coverage or physical-panel presentation.
- [x] Through owned Remote `J‑9c58df3a83eb`, restored the previously authorized helper using Android's accessibility UI and its normal consent. Enabled only **Home button · Homeboard TV (test)**. The other three service entries were Off and were not changed; TalkBack, Switch Access and other controls were not toggled. Returned to Homeboard and captured **On · Working**.
- [x] Physical remote was not used. All observations here are coordinator screenshots/captured output, not direct physical-panel observations.
- [x] YouTube opens at its existing account chooser. No account was selected, added or changed. This is the planned YouTube starting screen for these runs, a narrower scenario than playing video or browsing after login.
- [x] Three separate recorded Home returns and evidence inspection completed (results below). Cast1 was acquired by another owner's Clocks benchmark when setup Remote ended; recording admission reported Device is busy. Other jobs and holds have not been cancelled or released by this session.

Private evidence root: `docs/diagnostics/homeboard-remote-customer-20261123/`. It contains preflight screenshots, recorded-preflight evidence, setup evidence and the On / Working and About screenshots. Registered diagnostics are also exported to `docs/diagnostics/chromecast-app-diagnostics-J-98cca1ded641/` and `docs/diagnostics/chromecast-app-diagnostics-J-7ff9ce80ba9e/`. Do not publish these raw screen/account records.

Exact Home-release delay and proof of an absent flash remain outside this exploratory acceptance: input/video clock correlation is explicitly unvalidated in 0.1.226.


## Three recorded returns: passed for the account-chooser scenario

Each run used the installed shared Taskfile with `CAPTURE_DURATION=120`, confirmed the existing YouTube account chooser in a before screenshot, sent exactly one `HOME`, waited six seconds and captured Homeboard. No account selection was made. Homeboard's YouTube favorite restored the starting app between runs; it was launched after the stability check in runs 1 and 2. Final state after run 3 was Homeboard with the helper enabled.

| Run | Owned job | Decoded/indexed frames | Observed captured transition | Finish and cleanup |
| --- | --- | ---: | --- | --- |
| 1 | J‑0b0588462fbf | 53 | YouTube → Homeboard, including a blended transition frame; no identifiable Google TV frame | Completed; server exited; owned forward removed; no unresolved effects |
| 2 | J‑83d79c14c00b | 70 | YouTube → Google TV → Homeboard; Google TV visibly captured in frames 36–46, with Homeboard entering in frame 47 | Completed; server exited; owned forward removed; no unresolved effects |
| 3 | J‑a8a33e0fcc36 | 42 | YouTube → Homeboard; no identifiable Google TV frame | Completed; server exited; owned forward removed; no unresolved effects |

- [x] All three exports include `remote-capture.json`, `interactive-timeline.json`, `live/frames.jsonl`, `live/stream.scrcpy` and a playable `live/segment-001.mp4`. Each export verified 13 artifacts. Frame counts above were independently checked by ffprobe and full RGB decoding in the container; every frame was visually reviewed in private contact sheets. No decoder errors occurred with the original variable-rate time base preserved.
- [x] All frame indices report complete packet framing, zero incomplete tail, no nonmonotonic timestamps, and every indexed frame mapped to an MP4 sample. Readiness reports a decoded startup frame. Each input timeline records one applied HOME with `released: true` and device-side down/up injection intervals; this is injection acknowledgement, not proof of app delivery time.
- [x] Homeboard is visually stable for more than 500 ms of encoded PTS in each run and is present in the screenshot taken after the six-second wait. Each receipt reports succeeded, health ready and no surviving owned processes; the terminal status has no unresolved effects. Export manifests contain concrete cleanup records, rather than the earlier cancellation case's pending marker.
- [x] These commands exercise the **installed shared Taskfile end to end**, resolving the handoff's uncertainty about that entry point. No coordinator code or release was changed here.
- [ ] Exact Home-release latency, exact Google TV flash duration and physical-panel timing are not accepted. Each manifest explicitly says `input/video clock mapping unvalidated`. No Google TV frame in runs 1 and 3 means none was observed in this capture, not proof of zero flash. Long static gaps, capture exclusions, sampling limits and host-derived final frame holds remain limitations. These runs do not clear the timing gate or establish playback/protected-content coverage.

Approximate execution: 19:35–19:39 Asia/Bangkok on 23 November. Private folders are `run-1/`, `run-2/`, `run-3/` under the evidence root above. `verified-recordings.json` records per-run decoded counts, input receipts, mapping and cleanup; `run-1-frames.jpg`, `run-2-frames.jpg` and `run-3-frames.jpg` contain every decoded frame with rebased PTS labels. Raw screen/account evidence stays local.

The preflight ANR remains a separate Homeboard reliability finding; a successful relaunch and these return checks do not explain or fix its cause. Cancellation/interruption acceptance for recorded Remote remains the coordinator operator's separate test; this customer exercise used normal Finish.


## Passive ANR follow-up

Owned diagnostics `J‑52988aa6e960` ran with `LAUNCH_APP=false` at approximately 19:44. It completed with foreground unchanged, no settings applied, no new exit records and verified cleanup. The history still records the 19:22:35 ANR and two morning ANRs; no later ANR exit is present. The earlier diagnostic log is from replacement PID 10662, not ANR PID 10163. Its slow startup frames cannot explain that earlier hang. No speculative app change was made. The coordinator currently collects exit metadata but not the retained ANR stack; see the [specific collection request](../requests/2026-11-23-coordinator-anr-traces.md).


### Superseding ANR evidence

The retained-trace collector was later deployed in coordinator 0.1.229. Exact-exit job `J‑f69f2e716471` recovered the original PID 10163 stack: the main thread was waiting in air-card preference loading during initial rendering. See the [investigation and accepted startup fix](2026-11-23-homeboard-anr.md). This supersedes the missing-stack limitation, not the three customer capture results or the parked timing gate.
