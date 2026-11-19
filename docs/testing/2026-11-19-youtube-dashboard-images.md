# YouTube dashboard images

The current guide and promotional image use an unmodified Cast1 screenshot from
**0.1.74**, coordinator **J‑e238afe3972c**. The debug-only probe uses fresh isolated
preferences and the production first-run dashboard and app discovery. YouTube's
real installed launcher is seeded by the production code. Its native report
passed and asserts household preferences unchanged. No app was launched from
Favorites and no saved favorite list was changed.

[Reviewed dashboard](../how-to/quick-start/dashboard-youtube.png).

PNG SHA-256: `52f3afdbf82c46f549204de1f1e980a9b34207bb4f814832c42c1016e78995a8`.
Frozen APK SHA-256:
`80fb505dc9952649363024b88f26b0a346a49c439f28468f89af1f7f9918a4a3`.
Full private receipts and native report are under
`artifacts/youtube-dashboard/0.1.74/J-e238afe3972c/evidence/`.

Visual review confirms YouTube's icon and label in the full-width bottom row,
hourly Rain, Calendar and PM2.5 trends. Readings and coverage status are captured
as displayed, not retouched. Historical test screenshots remain unchanged.

The illustrated quick start and promotional sheet reference this image. The print
manual's layout diagram also names YouTube. The promotional hero now includes
the entire dashboard rather than clipping off Favorites. Public-site export
sources reference the same PNG with a fresh-test-layout caption.

The capture build passed 503 JVM tests, debug lint, 661 English/Thai resource
checks, 7 calendar pipeline tests, 70 cleanup/recovery tests and 4 deployment tests.
Cast1 deployment **J‑27a8ae0abc08** succeeded. Cast2's independent debug install
**J‑371cfe2918df** failed `personal-install-in-use`; its public release installation
is separate. The shared app catalog was restored to MainActivity after capture.
