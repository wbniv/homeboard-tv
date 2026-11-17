# Fast full-screen widget opening

Version **0.1.42** shows the expansion shell before constructing richer widget
views. Its opening zoom is configured for 140 ms, from 92% to 100%, anchored
toward the originating widget. Reduced motion skips it. Back/Restore can dismiss
the shell, pending content callbacks are canceled on release, and saving before
content creation retains the initial content state. Configure becomes available
after content mounts. Configure return and recreation do not replay the zoom.

Container `task check` passed, including calendar/deployment checks, JVM tests,
552-resource translation parity and Android lint. `task deploy` repeated the
checks for the incremented version and built/froze the debug APK successfully.
`git diff --check` passed.

Frozen APK: `artifacts/deployments/0.1.42/homeboard.apk`, SHA-256
`1099e7ab5baa963ed72f33ff15108b4d0fe86700a9f1786359ed2a9291c83d82`.
Source base is `6941aa2`, with local expansion changes in the frozen source
manifest. The phone redesign was held out of active entrypoints during this build.

## Installation and acceptance

Cast1 background install **J‑17b7bf35ccf6** refused to install over foreground
Homeboard. Install-and-record **J‑ce8dbaa8cd41** installed the exact new hash.
Its navigation assumed Home but actually started in All Apps and launched Quilt
Night; the coordinator guard stopped it and automatic readd **J‑6fa82170bc69**
restored Ready. The first normalization retry **J‑61d67d6bd0d1** also failed:
its initial Back release was 3.5 seconds late, compressing subsequent navigation
and launching YouTube. Automatic readd **J‑be605072dc4d** restored Ready. These
are script failures, not successful animation evidence or Homeboard crashes.

The revised `dev/device/widget-zoom-record.json` gives navigation time to settle
and normalizes Home/header focus with distinct key presses. Cast1 recording
**J‑e4afa127a301 completed successfully**, using the identical installed APK.
Reviewed evidence: `artifacts/deployments/0.1.42/zoom-final-evidence/`.

- `capture-001.mp4`: Calendar's shell appears at a smaller scale, zooms to fill
  the window, shows the localized loading body, then mounts Calendar content.
  Subsequent air-quality, Rain and Favorites openings also reach richer content.
- `input-frame-002.png`: Calendar's four weeks and highlighted-day detail.
- `input-frame-003.png`: repeated open/quick Back restores Calendar focus and
  the same one-page dashboard layout.
- `input-frame-004.png`: nearby station map and actual/last-known device location.
- `input-frame-005.png`: Rain's richer chart and interval detail.
- `input-frame-006.png`: Favorites banner grid and selected-app detail.
- `input-frame-007.png`: original dashboard restored after Favorites.

Cast2 install **J‑fd7073eb7437** has a successful install-stage receipt with the
matching installed APK hash. Its automatic verify-launch continuation remains
queued during Personal use. Receipt and install artifacts are under
`artifacts/deployments/0.1.42/chromecast-test-02/evidence/`.

## Limits

The recording has variable frame spacing; it establishes visible zoom and
shell-before-content behavior, not exact frame pacing or measured 140 ms timing.
The quick Back release was delivered about 297 ms after the preceding OK release,
so that result does not establish cancellation inside the 140 ms animation.
Reduced motion, disabled system animations, recreation while loading and Configure
return were reviewed in code but not separately exercised on the TV in this pass.
This change provides opening feedback; synchronous detail construction and network
fetches still take their existing time after the shell appears.
