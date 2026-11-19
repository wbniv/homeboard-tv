# Default YouTube favorite

Build **0.1.72** seeds the installed YouTube TV launcher into a fresh dashboard's
Favorites after successful app discovery. It uses the actual discovered component,
not a hard-coded activity. Android YouTube is a fallback when the TV package is
absent. Neither installed means no unavailable shortcut. Existing lists, saved
empty Home and removals remain unchanged. Explicit default-layout restores retain
favorite contents.

Container validation passed **503 JVM tests**, Android debug/release lint,
661 English/Thai resource checks, 7 calendar pipeline tests, 70 cleanup/recovery
tests and 4 deployment-helper tests. New favorite tests cover resolved component
seeding, persistence before discovery, one-time consumption, removal persistence,
missing apps and pre-existing lists. No actual fresh-install TV screenshot is
claimed: shared TV data was preserved.

The signed release APK is frozen under `artifacts/deployments/0.1.72/`:
`0d2fde1f96d6a589e21fb95e662371dc85c59f6fc20ac4af0b2ffc0cb49ef7b4`.

- Cast1 **J‑1574414bbfae**: installed hash verified and foreground/launcher
  verification succeeded.
- Cast2 **J‑5e89c14c266f**: installation stage succeeded and installed hash matches
  the same frozen APK. Evidence is in `chromecast-test-02/progress-evidence/`,
  including `install-stage-receipt.json` and `apk-installations.json`.
  Its foreground `verify-launch` continuation is queued during Personal use.
  Installation and foreground acceptance are distinct outcomes; the job remains
  queued for its admitted continuation, with its watcher running.

See the [default favorite plan](../plans/2026-11-19-default-youtube-favorite.md).
