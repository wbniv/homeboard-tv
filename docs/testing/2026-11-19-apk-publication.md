# Public APK and biohack.net publication — 2026-11-19

Published page: https://biohack.net/homeboard/

Final Cloudflare deployment: https://27e766ca.biohack-net.pages.dev

## Artifact

- Public APK: **0.1.73**, version code **73**, `net.biohack.homeboard`.
- Frozen file: `artifacts/deployments/0.1.73/homeboard.apk`.
- Size: **1,156,666 bytes**.
- SHA-256: `98c091c3d81b01ac2b0efffcee390818ab881c1d489e0928d6a1ca5a84da0c3c`.
- Signing-certificate SHA-256: `4ae3fa8a0c926c8b82edff09e20653bcaaa1ea55e5ae3a274291314853bd3d59`.
- Base source revision: `6b130547c1cbb2ea42283d606d64223611a16d84`.
  The immutable source archive includes the actual app build inputs, release
  version and signing configuration code, without private signing material.
- Public download:
  https://biohack.net/homeboard/releases/0.1.73/homeboard-tv-0.1.73.apk

The downloaded production APK matched the frozen SHA-256. HTTPS returned
`application/vnd.android.package-archive`, attachment disposition, `nosniff` and
immutable caching. Private keys/passwords remain outside the repositories; the
build mounts signing material read-only.

Version 70 was consumed by a verification failure: the first verifier incorrectly
classified the inherited private boot `ProbeActivity` as a debug probe. The check
was corrected to distinguish runtime behavior from debug-only UI probes. Version
71 was installed as the initial public-package candidate. Version 72 belongs to
the separate debug deployment; version 73 is the frozen public update. Version
74 supplies the separately verified dashboard illustration, not the public APK.

## Container validation

- 503 JVM tests, zero failures/errors/skips; debug and release Android lint passed.
- Seven calendar pipeline tests, 70 cleanup/recovery checks and four deployment
  regressions passed. Release verification checked the signature, zip alignment,
  package/version/minimum SDK, non-debuggable status and absence of test fixtures.
- Website tests: 64 total, 62 passed, two existing conditional skips, no failures.
  Both release tests passed, including rejection of invalid/tampered metadata.
- Astro production build: 176 pages. The existing homepage link check passed;
  browser checks separately verified the nested Homeboard pages and their local
  guide/image/download targets.
- Chromium and WebKit passed at 390px and 1280px widths, with no horizontal
  overflow or JavaScript errors. The download link matched the release asset;
  guide images loaded and the printed QR decoded to the canonical page.
- The guide generator produced two A4 pages. Both pages were visually inspected.
  Release-note list spacing was corrected after Will's screenshot review.

Local logs and browser captures are under `artifacts/publication-*` and
`artifacts/cloudflare-publication-final.log`. The site build used the existing
dependencies and a separate writable Vite cache inside its container.

## Device evidence and limits

All device operations used the shared coordinator, preserving app data and the
default Home selection. Public and debug packages remain separate installations.

| Device / check | Job | Result |
| --- | --- | --- |
| Cast1 public 0.1.71 install | `J‑745f2169f250` | Succeeded |
| Cast2 public 0.1.71 install | `J‑1c408064a5c8` | Installation succeeded; deferred launcher continuation was later cancelled when superseded by 73 |
| Cast1 public 0.1.73 update | `J‑a8faa8f830b5` | Succeeded |
| Cast2 public 0.1.73 update | `J‑283d7e7b8324` | Install-stage receipt succeeded; foreground continuation deferred during Personal use |
| Cast1 public UI acceptance | `J‑47d9b8accb05` | Completed; dashboard, palette selection, About version 0.1.73 and dashboard restoration captured |
| Cast1 illustration | `J‑e238afe3972c` | 0.1.74 debug test-dashboard capture passed its unchanged-household-preferences assertion |

The first scripted public-app check, `J‑d20376bd3130`, stopped because Android's
location-permission controller was foreground. This was a system prompt, not an
app crash. The coordinator's owned live remote session completed consent; a
working full-screen Rain view was captured. The final scripted acceptance then
completed successfully. Owned remote sessions were ended with coordinator cleanup.

Public acceptance screenshots are in
`artifacts/public-release-acceptance/0.1.73/`, with the About/version capture in
`input-frame-004.png`. Cast2's successful installation stage is recorded in
`artifacts/deployments/0.1.73/chromecast-test-02/evidence/install-stage-receipt.json`.
Do not report its queued foreground continuation as completed acceptance.

The coordinator installed the APKs in place without clearing data. Customized
settings retention across a later higher-version update was not separately
observed. Physical USB/file-transfer installation, a physical-phone QR scan, and
foreground behavior on other Android TV models remain unverified. Browser QR
decoding and the public HTTPS downloads were checked independently.

## Publication

The production site was built from biohack.net revision
`9f813cf239cd274b73ee57126751fb7d3ff1c7a8` plus the scoped Homeboard changes.
Unrelated Thai-review working edits were excluded. Cloudflare OAuth was renewed
with account/user read and Pages write access. An initial deployment's mistyped
revision metadata was corrected by deploying the same bytes with the verified
base revision; `27e766ca` is the final deployment.

The live page, illustrated guide, print HTML, PDF, dashboard image, license,
third-party notices and checksum file were fetched over HTTPS. The public APK
download hash matched the signed frozen artifact. See
`artifacts/publication-live-checks.json` for the link results.
