# Visible wallpaper preview · 17 November 2026

Build **0.1.49**, frozen APK
`artifacts/deployments/0.1.49/homeboard.apk`, SHA-256
`4ca492d8e0388b732ac47b766e3e73b9173c296a3beb515678ce966073e5732f`.
The frozen source manifest includes the combined World clock and phone-editor
changes present at packaging. Later unrelated header corrections need a new build.

- Container `task check`: 462 JVM tests passed, no failures/errors; Android lint
  passed. English/Thai validation passed for 604 resources. `task build` passed
  through `task deploy`.
- Cast1 install-only J‑0abffcf4c1f9 declined installation over the foreground app.
  Complete foreground check **J‑ac6bcf65ec3e** then installed and verified the same
  frozen hash, completed with health ready, and displayed **ALL CHECKS PASSED ·
  57 menu regressions**. Evidence: `artifacts/deployments/0.1.49/background-menu/`.
- The real launcher menu regression checks Up → Icons and Down → Background on
  Home and All Apps, saved 100% UI opacity, composed preview pixels with no tint
  or scrim obscuring the provider color, controls inside the right half, unchanged
  saved opacity, Apply/Back restoration, provider options, Retry, Undo and source
  credits. The fixture isolates launcher preferences and verifies Will's settings
  remain unchanged.
- Cast2 **J‑c05510a23bec** verified installation of the identical frozen hash.
  Its install-stage receipt succeeded; foreground verification remains deferred
  during Personal use. Evidence:
  `artifacts/deployments/0.1.49/chromecast-test-02/evidence/`.
- The coordinator briefly lost its socket after both installation commits.
  Receipts and probe screenshots were exported after recovery; no direct-device
  fallback was used.

The real Overflight HD/SDR preview is checked separately with
`dev/device/overflight-visible-preview.json`. This browses the installed Overflight
provider, previews without applying it, captures two frames and cancels back to
the dashboard; it does not change provider preferences or saved UI opacity.

Real-plugin scripted capture remains **unverified**: J‑cfced95b3633 aborted after
the first Menu key because Google TV became foreground. J‑9df83704bf17 replaced
Menu with navigation through the on-screen settings button, but also aborted
after the first Up key, before reaching the provider. Both jobs retained a live
Homeboard process without a reported app crash and the coordinator recovered to
health ready. Evidence is under `overflight-preview/` and
`overflight-preview-dpad/` in the frozen deployment directory. These jobs are not
video acceptance evidence. The successful 57-case in-process menu probe tests the
actual launcher composition and restoration, independently of external key input.
