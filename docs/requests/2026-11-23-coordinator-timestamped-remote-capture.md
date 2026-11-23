# Coordinator request: timestamped capture during an owned Remote session

**Status: draft request, not filed in the coordinator repository or an external
issue tracker, and not sent to maintainers.** Written 23 November 2026 after
reviewing installed coordinator 0.1.224 (`cf5700f9`).

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

## Current capability gap

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

The Homeboard side can then perform and analyze this test autonomously. This
draft does not claim coordinator implementation or timing acceptance.
