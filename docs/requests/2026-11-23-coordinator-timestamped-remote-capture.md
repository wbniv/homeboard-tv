# Coordinator request: timestamped capture during an owned Remote session

**Status: Remote integration draft implemented and under verification; not
deployed. Video/input clock correlation and real-device acceptance remain
unverified.** Written 23 November 2026 after reviewing installed coordinator
0.1.224 (`cf5700f9`). This Homeboard session has not filed an external issue;
the draft implementation is saved on coordinator branch `remote-timestamped-capture-20261123` (initial implementation `dc2e46d6`).

## Reported implementation progress

Will supplied this coordinator implementation update on 23 November:

- A frame-index prototype preserves original variable-rate scrcpy timestamps
  and flags incomplete packet tails.
- Its two container tests passed.
- It is not connected to an owned Remote session. Remote's Home tap uses a
  different input path from the timestamped directional-key helper, so video
  cannot yet be correlated with **Home release**.

These are reported prototype results, not tests independently rerun by this
Homeboard session. They do not satisfy the requested measurement or establish
that the deployed coordinator has the capability.

**Current implementation work (23 November):** an isolated coordinator branch,
`remote-timestamped-capture-20261123`, adds bounded opt-in Remote recording,
original frame PTS and MP4 mappings, device-side tap injection brackets including
Home, CLI/MCP/GUI entry points, owned status, Finish and partial/recovery exports.
The input helper compiles in a container. Focused media, Java, interface and
cleanup tests pass; a full run exposed contract-record drift and an overbroad
Finish rule that were subsequently corrected, plus one service-startup timeout.
This is a source draft, not deployed capability or validated timing.

A visual-clock fixture draws independent device timestamps into captured pixels.
Its original APK is installed on Cast1. After Will returned to the home network
and confirmed debugging enabled, the fixture was launched through an owned Remote
and recorded successfully (`J‑44a900f4838e`). All 104 video frames decoded valid
clock values. Original video timestamps followed the drawn clock by 24–190 ms
(median 28 ms). This supports a shared clock origin on that run; it does not prove
physical-panel timing or a maximum capture delay, and the recording contained no
Home input. Owned capture cleanup was verified and its reservation released.

The full draft rerun passed 6,046 tests, with five skips and one simulated recovery
wait timeout. After adjusting two test-only bounded waits, all 91 coordinator/MCP
tests passed. The recording/analyzer tests passed 18 checks, including an irregular
frame-time regression found while decoding the real capture.

**Connectivity:** Cast1 and Pixel are reachable again. Cast2 still has no verified
current debugging advertisement. The earlier school-hotspot blocker has ended.
The proposed private Tailscale gateway on the charging Lenovo tablet (or a
Chromecast) remains unconfigured; no always-on home server currently exists.

**Deployment ownership:** Will reports a separate coordinator session is also
implementing this capability. This Homeboard session stopped its candidate build
before release/deployment. Draft `10582ef3` and the Cast1 evidence are retained for
that session to reconcile; candidate version 0.1.226 is not reserved. No service
upgrade or administrator prompt was started here.

**Remaining:** reconcile and deploy the coordinator-owned implementation and finish live acceptance;
validate the actual Home tap and video/input clock relationship, interface and
cleanup behavior on hardware; deploy through the reviewed coordinator workflow;
then analyze three Homeboard returns. Exports deliberately keep timing marked
unavailable. Exact flash/redirect timing remains blocked.

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
