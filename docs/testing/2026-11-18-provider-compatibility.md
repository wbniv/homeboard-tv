# Independent wallpaper provider compatibility campaign

Execution of the [provider inventory/test plan](../plans/2026-11-18-wallpaper-provider-compatibility.md).
Results are collected on Cast1. The current installation/directory implementation is
0.1.59, installed and launch-verified on Cast1 (`J‑70a593f6f0c4`). Cast2 is
unreachable through the coordinator; its identical APK is queued as `J‑39c23fd4710c`.
Superseded queued 0.1.56/57/58 installs were canceled.

The approved [current plan](../plans/2026-11-18-provider-testing-spike-and-installation.md)
requires actual screenshot proof and explicit incomplete checks. See the
[18-entry dispositions](2026-11-18-provider-dispositions.json) and
[directory screenshot record](2026-11-18-provider-directory.md).

## Frozen inputs and probe

Homeboard 0.1.56: SHA-256
`b5e7c1e5a920cb21c6d173134cb91bc9d0a875823b336d2802ddd18d8426994f`.
Container checks, lint and APK packaging passed; translation parity covers 610
resources. The new code is debug-only and leaves production provider behavior
unchanged. Cast1 deployment `J‑860d193aa3cc` installed and verified that APK.

`dev/provider-acquire.py` freezes public release assets and hashes.
`dev/provider-inspect.py` uses Android tools inside the build container for
package/version, manifest and signature inspection. `dev/provider-campaign.py`
owns sequential complete coordinator jobs and evidence export. No raw device
control is used. Its optional attempt labels preserve earlier failed evidence.

`ProviderCompatibilityProbeActivity` tests one real installed provider via the
production registry, IPC host and renderer. It reads but never writes household
preferences, does not import/export provider preferences, and hashes media URIs
instead of recording URLs that might contain credentials. Each successful baseline
requires initial rendering, production timer deliveries for timer providers,
release of motion resources while hidden, rendering after resume, and rendering
after explicit refresh. Video samples at separate times record pixel differences.
Results are in the coordinator-exported `wf.log`, with a final screenshot and log.
Keep-screen-on applies only to the temporary probe window.

A baseline pass is **partial overall compatibility**. Provider-specific settings,
real cache expiration, external actions, fault injection, actual Home menu
Preview/Apply/Undo and extended playback soaks need their own evidence. The probe
lists these as not run. Providers without usable default/configured media are
blocked, not silently passed. A negative video difference alone does not prove a
decoder failure: a scene may be static. Initial first-frame readiness is separate.

## Device/session evidence

Root: `artifacts/provider-compatibility/2026-11-18/` (private artifacts, not committed).

- Pre-test inventory: `J‑4d24d884eab8`, `inventory-before/`. Only Overflight was
  already installed among the acquired external plugins. Its existing version and
  preferences are preserved. Installed DreamServices are recorded separately.
- Daily Photos install: `J‑b61f2d50fa01`, `daily-photos/install/evidence/`.
  The installation hash was verified; a later launcher-artwork continuation hit
  `plugin-error`. Coordinator recovery `J‑54e68860cfc3` completed.
- Daily Photos first probe: `J‑e730c8956061`, `daily-photos/test/evidence/`.
  `target-left-foreground` before the native report; no compatibility conclusion.
- Aerial Views install: `J‑c5d41c1a7c7b`, `aerial-views/install/evidence/`.
  Installed hash verified. Its unrelated launcher-artwork verification stalled and
  was canceled through the coordinator; final state says APK installed, launcher
  unverified, health ready.
- Aerial Views first probe: `J‑05aa16bd3e41`, `aerial-views/test/evidence/`.
  Scripted-input helper failed during a capture-only recipe. Diagnostics show the
  Homeboard probe still resumed. This does not establish provider failure.
- Diagnostic inventory `J‑2d5d5a5e9d77`, `launch-diagnostics/`, did not show a new
  Homeboard crash. Supported Cast1 recovery `J‑08adf4b61c46` completed.

The recipe now uses native measurements and a final capture without the scripted
input helper. New provider installation sessions use `check` with a brief settings
launch, avoiding an irrelevant launcher-artwork continuation. These are ordinary
coordinator workflows, not direct-device workarounds.

## Acquisition and source findings

Thirteen official release APKs were frozen: Daily Photos 1.10, Aerial Views 1.8.5,
Core Motion 1.0.0, Weather 2.11, Overflight 1.20, Sofadrop 1.2.2, Immich 0.0.5,
Trakt 1.3.15, Booru 1.2, z9m Jellyfin 1.02, TV Background Suite 1.3.13,
Wallpaparr 1.0.0 and Random Wallpapers 1.0.0. See each `acquisition.json`,
`badging.txt`, `manifest.txt` and `signature.txt` for exact identities/hashes.
Core Motion's `motion` release is outside the initial ten-release page; its
explicit tag was subsequently located and frozen.

Additional source snapshots:

- Mikescher Jellyfin: `6efcbdc`, unmodified container build succeeded for 1.2.
  APK hash `5e47d9023c8f5b7dba8ffda48062030266ccd88bcf589da0dbccf865d4a291cf`.
  Its binder deliberately excludes the server URL/token from import/export;
  configuring a disposable server requires the supported settings UI workflow.
- jfchenier Jellyfin candidate: `0be71ff`. Service implementation still returns
  the sample drawable/image/animation/Lottie/video list, not Jellyfin data. It
  should be classified as a template fork, not another implemented Jellyfin source.
- Unsplash source located at
  [adrianjost/Projectivy-Plugin-Splashing-Wallpaper-Provider](https://github.com/adrianjost/Projectivy-Plugin-Splashing-Wallpaper-Provider),
  revision `6e79206`; no published release APK. Its source uses `YOUR_ACCESS_KEY`,
  so an unmodified source build cannot establish live Unsplash operation.
- m3u8: `8b52e75`, unmodified source-only container build succeeded for 1.0.
  APK hash `6ead6ad32ae6102963231d3b4a1e82b3907aede18b47d821562d05dc7d0ffd50`.
  This proves compilation, not stream playback.
- Movie Art: [maintainer source](https://github.com/Dak0r/Movie-Art-For-Projectivy-Plugin),
  revision `980a766`; no published release APK. Source build expects a separate
  TMDB key properties file. No household account/key was requested or used.

Random Wallpapers is service-only, with no declared activity. The coordinator's
current application registration expects an activity; registration/install
support needs to be established before submitting a synthetic activity name.
The campaign records this constraint instead of inventing a launcher component.

Native per-provider outcomes and remaining work follow as sessions complete.

## Completed baseline and controls evidence

| Provider / GitHub edition | Baseline | Home / All Apps controls | Overall |
|---|---|---|---|
| Daily Photos 1.10 | Pass `J‑7d6a96e9a8c5` | Pass `J‑574ba5b5263f`, 10 assertions | Partial |
| Aerial Views 1.8.5 | Pass `J‑51149b48d317` | Pass `J‑e29c57cd7fe3`, 10 assertions | Partial |
| Overflight 1.20 | Pass `J‑325067405c22` | Earlier HD/SDR check; current full controls not run | Partial |
| Core Motion 1.0.0 | Pass `J‑eaf00a2dbd46` on Homeboard 0.1.59 | Not run | Partial |

Native baseline reports verify initial rendering, timer deliveries, motion sample
changes, hidden resource release, resume and refresh. The controls reports verify
Preview visible at 0/75/100% widget opacity, Back cancels, Apply makes one Undo entry,
independent wallpaper dimming, and Hold Back Undo on Home and All Apps. All report
household preferences unchanged. GitHub edition evidence does not verify Play editions.

Core Motion install `J‑695cc70c19a6` succeeded; rendering check `J‑8edb39e37bac`
failed `target-left-foreground` before a native report. Its retry on 0.1.59
passed initial rendering, two timer deliveries, hidden resource release, resume
and refresh: five renders and measured motion difference 391484. Household
preferences were unchanged. This verifies the returned animated loop, not every
feed/format or full menu controls. Immich install
`J‑a4221af1b5a2` succeeded; real backend/rendering tests remain blocked.

Sofadrop install `J‑e14bc47f8e2e` stalled at installer commit and ended
`uncertain` / `plugin-error`, with `cleanup-installer-commit-unresolved`.
The coordinator recovered; no install verification or warning screenshot was
obtained. This does **not** establish a Play Protect block or malware finding.
No bypass/retry was attempted, and the subsequent Booru install was not submitted.
Weather remains separately excluded on the evidence in its security review.

The machine-readable dispositions give every scoped entry a status and the
specific missing work. Backend credentials, source-only builds, service-only
coordinator admission, current foreground failures and unfinished fault/soak checks
remain explicit. No claim is made that all eighteen providers work.
