# Rain widget validation · 16 November 2026

Native rain implementation follows the [approved plan](../plans/2026-11-16-rain-widget.md).

## Container checks

Initial `task check` passed 402 JVM tests with zero failures/errors, six calendar
pipeline tests, bilingual resource parity and Android lint. The new rain tests
cover precipitation amount/probability separation, preceding-hour timestamp
conversion, partial daily rows with no overlap, missing/invalid values, expired
coverage, DST day lengths, half-hour zones, invalid units/arrays/timestamps,
coordinate validation, independent widget settings, layout round trips and
saved size spans. No host Java/Android tooling is used.

The numbered deployment repeats tests/lint and assembles the debug APK through
`task deploy`. Release bundle packaging is tracked separately below.

## Device review

`RainProbeActivity` is debug-only. It uses separate Home/cache preferences and a
public Bangkok city coordinate, leaving the user's actual Home/settings intact.
The review uses real Home layout and rain rendering, requests the public forecast,
cycles 1×1, 2×2, 4×2, 2×3, 4×3, 6×3 and Fill Home, and opens hourly/full-screen
details. It also checks platform street-address geocoding using a public business
address. The footer identifies whether the forecast parsed and whether the address
backend resolved that query. Values are live, so wet animation is not guaranteed
in that location's current forecast.

The coordinator recipe `dev/device/rain-review.json` captures these states in one
complete session. Device results are pending until evidence is exported and
visually inspected; a successful coordinator job alone is not UI acceptance.

## Remaining acceptance

- Expanded English/Thai, enlarged-font, long-location and intermediate-span matrix.
- Wet-animation frame/recording evidence and on-device navigation performance.
- Failure/recovery and delayed-response races on the physical TV without changing
  shared-device networking.
- Confirm address-backend result and record any service-dependent limitation.

## Deployment and packaging

The final 0.1.22 candidate passed `task check` (402 JVM tests, six pipeline
checks, 442 bilingual resources, lint: zero errors). `task build` succeeded and
`task deploy` froze:

- APK: `artifacts/deployments/0.1.22/homeboard.apk`
- SHA-256: `04f649e343de8e6f488a99aa9ef1ff8535ea7a23beeec6be93de619ba21b5b92`
- Source base: `bce542649ca11cfc1421a084e419885898c95796` plus the rain changes in
  this workspace; `artifacts/rain-source-manifest.json` records source hashes.
- Install/brief-launch job: `J‑0e6901c495ce`, **queued**, not confirmed installed.

Cast1 is recovery-required after a prior session. The coordinator reconnect job
`J‑fe0ef60a1030` failed with `transport-unavailable`; the device advertised a
wireless-debugging endpoint but ended the secure handshake. This is not an app
failure and does not prove that pairing renewal is the only possible repair.
Reconnect evidence is in `artifacts/rain-reconnect-evidence/`. No raw-device
fallback, data reset or default Home change was attempted.

The isolated size review has **not run**. The coordinator permits one application
registration per package, so a separate `homeboard-rain-review` alias was rejected.
After the pending normal deployment completes, register the same frozen APK as
`APP=homeboard-tv ACTIVITY=com.gathin.tvhome.RainProbeActivity REPLACE=true`, submit
`dev/device/rain-review.json`, watch/export evidence, then restore the same catalog
entry's activity to `com.gathin.tvhome.MainActivity`. Registration changes catalog
launch metadata, not the TV's default Home. Do not change this catalog activity
while the normal deployment is still pending.

A live public-API smoke check ran inside the build container with generic Bangkok
coordinates. The forecast returned **264 hourly samples**, `Asia/Bangkok`, and the
expected epoch/mm/cm/percent units. City lookup returned five candidates. These
checks establish endpoint/schema availability from the build environment; they do
not establish TV connectivity, address-backend availability or UI rendering.

`task bundle` passed in the build container. The unsigned release bundle is
`launcher/app/build/outputs/bundle/release/app-release.aab` (1,339,469 bytes).
The debug-only review activity and wallpaper fixture are excluded from release.
No Play signing or publication was performed.

Machine-readable results are in `artifacts/rain-validation.json`; the queued job's
current receipt is exported in `artifacts/rain-pending-deployment-evidence/`.
