# Coordinator request: timestamped capture during an owned Remote session

**Status: three Cast1 customer captures passed on coordinator 0.1.226 (`6030fd88`) for YouTube's account chooser. Coordinator was subsequently upgraded to 0.1.229; exact timing remains unavailable.**

## Current customer result, 23 November 2026

The coordinator-owned implementation supersedes this session's isolated draft and candidate deployment work. Its [customer handoff](/home/will/chromecast-coordinator/docs/handoffs/2026-11-23-homeboard-remote-capture-customer-test.md) explicitly retains the clock-correlation limitation.

Using the installed shared Taskfile, this Homeboard session ran three independent recorded Remote sessions on Cast1 / Android 14 / Homeboard 0.1.128, after restoring the authorized debug Home-button helper through Android's accessibility UI and confirming On / Working. Each started at YouTube's existing account chooser, sent one Home tap, returned to stable Homeboard, exported playable video and original frame indices/input receipts, and completed with verified cleanup. Google TV is visibly captured in run 2; no identifiable Google TV frame appears in runs 1 and 3. No account selection or change was needed. See the [customer test record](../testing/2026-11-23-home-button-remote-customer.md) for all three jobs, verification, private evidence paths and preflight findings.

**Remaining:** independently validate the input/video clock mapping and uncertainty before claiming exact Home-release latency or flash duration. The manifests correctly report timing unavailable. Capture absence cannot prove zero flash or physical-panel timing. Playback/protected-content coverage and cancellation/interruption are separate acceptance scenarios.

Earlier isolated draft `remote-timestamped-capture-20261123` (`712a706a`, implementation `dc2e46d6`) and its 104-frame visual-clock calibration remain comparison evidence, not the deployed implementation. The measured 24–190 ms draw-clock/video difference from that earlier run is not a calibrated bound for these new jobs. The off-site Tailscale gateway proposal remains unconfigured; it was unnecessary for this same-network test. No external issue was filed.

## Problem and required result

Homeboard needs to measure the transition from YouTube through Google TV Home
to Homeboard after a short Home press. Report two separate quantities:

- Home release to the first stable Homeboard presentation.
- The duration for which Google TV Home is visibly presented.

Current [device evidence](../testing/2026-11-23-home-button.md#timed-screenshot-check)
only supplies conservative bounds including command transport and screenshot
transfer. Three samples were below 4.2 seconds; they do not measure actual
redirect duration or a brief flash. This test should require no physical input
from Will.

## Deployed capability gap (reviewed on 0.1.224)

- An owned Remote session can send Home and cross app boundaries. Its attached
  observer in `plugins/android/operations.py::_service_video_observer` writes
  raw H.264 without per-frame presentation timestamps.
- `plugins/android/preview_video.py::stream_scrcpy` receives framed packets but
  exports their data without retaining presentation timestamps.
- The MP4-producing `record-live` workflow preserves timing, but its input
  policy accepts only Finish. App-scoped interactive recording requires the
  target to stay in front, which this transition deliberately does not do.

Do not estimate timestamps by counting raw H.264 frames at an assumed frame rate;
static and variable-rate content make that unsuitable for this measurement.

## Requested capability

Attach a timestamp-preserving capture to a coordinator-owned Remote session.
Retain frame presentation times in a container or exported sidecar and correlate
the Remote key-down/key-up times to that same time base. Export the clock mapping,
its uncertainty, dropped-frame/stream-gap information and capture dimensions.
Host request arrival or video download time must not stand in for device input
or presentation time.

Keep existing ownership, admission and cleanup behavior. Cross-app input remains
part of the owned Remote session; do not relax an app-recording foreground guard
or introduce arbitrary shell commands. Capture must stop when its owning session
ends and export a complete cleanup receipt.

## Acceptance checks

1. Through one owned session, launch YouTube, confirm Homeboard's Home setting
   On / Working, return to YouTube, and record a short Home press and the complete
   transition. Export video, frame times, input times and clock mapping.
2. Verify timing against variable-rate content containing static intervals.
   Preserve presentation intervals and flag missing frames; do not synthesize a
   successful measurement when timestamps or coverage are missing.
3. Analyze at least three returns. Identify the first Google TV frame, its last
   visible presentation interval, and the first Homeboard frame that remains
   stable for at least 500 ms. A static frame may remain presented for that
   interval; consecutive encoded frames are not required.
4. Report both quantities above with resolution/uncertainty. If no Google TV
   frame is captured, report no observed flash within capture coverage, not proof
   that a zero-duration flash occurred. If Google TV never disappears or
   Homeboard fails to stabilize, report that failure.
5. Check normal finish, cancellation and transport interruption: stop only owned
   capture/input resources, preserve the user's current foreground, retain
   available evidence and accurately report incomplete timing or cleanup.

The Homeboard side can perform and analyze this test autonomously once the
integration and acceptance above are complete and deployed. The frame-index
prototype alone does not establish timing acceptance.


## Clock-validation follow-up after customer acceptance

The installed 0.1.226 exporter always emits `timing.status=unavailable` in `remote_capture_session.py`. HOME receipts provide `clock=android-uptime-millis` and down/up injection start/end intervals. The manifest and frames retain video PTS and capture boot identity, but these jobs do not export an independently validated uptime-to-video conversion or its uncertainty. The three successful returns establish the recording/input/export path, not that missing relationship.

The coordinator owner can use the three private customer exports and the earlier isolated visual-clock fixture as starting evidence. Required next checks remain:

- [ ] Trace the pinned video PTS clock and the deployed helper's actual Home injection clock from source. Verify the relationship independently on the same enrolled device and boot, with before/after anchors, quantization and discontinuity checks. Do not substitute the earlier draft's different helper or calibration run.
- [ ] Use the visual fixture to validate the proposed mapping on the deployed capture path. Retain the per-frame observations and distinguish observed rendering/capture lag from a guaranteed latency bound. Define whether the accepted metric describes captured output or physical-panel presentation.
- [ ] Export an explicit mapping/provenance/uncertainty result or an unavailable reason. Clock changes, reboot, missing anchors and insufficient coverage must fail closed. Original PTS and unknown unseen-frame coverage remain intact.
- [ ] Deploy the reviewed result through the coordinator's owner, then rerun the Homeboard measurements with its accepted clock contract. This Homeboard session is not preparing a competing coordinator release.

The separate preflight ANR needs the [retained-trace request](2026-11-23-coordinator-anr-traces.md). Neither follow-up requires physical button input from Will at this stage.
