# Phone widget editor validation

Status: implemented and installed on both TVs as **0.1.40**. Actual Android HTTP,
TV approval and mixed Apply/Undo passed on Cast1. External phone-to-TV LAN
acceptance remains unverified after container connection timeouts.

Frozen APK SHA-256:
`8752cf1811b2197386d8a772276c9de359077ea680799074d7ba496b18634f58`.

## Implementation

Home settings → More → Configure from phone enables a foreground-only local
HTTP server, readable address and dynamic QR invitation. TV approval grants a
temporary single-editor session; replacements, inactivity, Stop and Activity exit
revoke access. The bundled phone UI edits Rain, Calendar, Air quality, Favorites
and widget geometry without a companion app or cloud account.

Widget configuration and Air settings now share the widget-home preferences file.
Existing per-widget Air settings are copied once into its aggregate map; existing
history/feed caches stay in their separate stores. Phone Apply commits layout,
Air settings, revision, composite Undo and operation receipt in one synchronous
SharedPreferences transaction. Native TV edits advance the same revision. Undo
restores mixed changes together. API access does not expose arbitrary preferences,
files, app launch/uninstall or remote key injection.

Phone preview is schematic. Timed native TV preview uses draft geometry and
configuration-only Rain/Air tiles; no draft location requests or cache writes.
Back, Cancel, timeout and session teardown restore the previous layout and view.

## Container evidence

- 444 JVM tests passed, including approval/replacement/revocation/expiry,
  origin/authentication/body-limit HTTP checks and configuration validation.
- Six calendar pipeline and two deployment-workflow checks passed.
- English/Thai parity: 552 resources. Android lint: zero errors (338 warnings).
- Debug APK assembly passed inside the shared build container.
- Chromium with Pixel phone viewport and WebKit with iPhone viewport passed actual
  bundled-UI checks against a controlled API fixture: mixed Rain/Air edits, Favorites,
  preview isolation, Apply, Undo, conflict draft retention, text escaping and mobile
  width. This is browser emulation, not physical Android/iPhone acceptance.
- Browser captures: private `artifacts/phone-browser/`. Reproduction:
  `docker run --rm -v /home/will/homeboard-tv:/workspace -w /workspace mcr.microsoft.com/playwright:v1.59.1-noble node dev/phone-browser-check.cjs`.
  Install its test-only playwright/jsqr/pngjs packages in `artifacts/phone-browser`
  inside that container; no host Java/Android/Node tooling is required.

## Deployment and coordinator evidence

0.1.37–0.1.39 were numbered intermediate iterations. 0.1.40 contains the final
compact phone action bar, WebKit style policy and debug-only Android HTTP probe.
Its complete launcher source manifest matches the working source. Both TVs received
the same frozen APK; no older deployment number was reused.

- Cast1 install/launch: J‑747724309353 completed successfully.
- Cast2 background install: J‑991e2ea45192 has verified installed hash. Its deferred
  launch continuation was canceled after installed-hash proof, before the temporary
  Cast1 probe registration. Personal use was retained.
- Cast2 proof: `artifacts/deployments/0.1.40/cast2-install-proof/`.

Initial native scripts completed without proving the whole application flow:
J‑7e0e5db2ac60 did not capture the QR screen at its early checkpoint;
J‑c2906652c9cb captured a readable QR/address, but the test browser did not finish
connecting during that session. Neither counts as a passing Apply/Undo round trip.
No household configuration change was confirmed from those scripts.

The extended external-browser attempt J‑18fa7f940a49 also timed out connecting
from the browser container while the TV session was active. The original short
window alone did not explain the failure. It was canceled safely; no successful
external-browser Apply is claimed. Private evidence is under
`artifacts/phone-actual-final/`. The exact external network cause is not established.

**Passing actual Android HTTP round trip: J‑c86ee768b408.** The debug-only probe
runs the same server, actual socket HTTP requests, ordinary TV Allow button,
repository and API against retained household configuration. Screenshots were
inspected and show the staged Apply, native preview and `PHONE_PASS` report.
The assertions cover:

1. Pending request becomes approved through the actual TV UI.
2. One HTTP Apply changes Rain label, Air station, Calendar lookahead and Favorites
   ordering together; retrying the identical operation leaves the revision unchanged.
3. Native timed preview is shown, then canceled without persisting extra changes.
4. One HTTP Undo restores the canonical complete draft snapshot, including all
   nine favorites, their order, widget settings, artwork and geometry.
5. Stop closes/revokes the session; the dashboard shows the original Lumpini Park
   station and Calendar range again.

Evidence: `artifacts/deployments/0.1.40/native-phone-check/`. The job completed
successfully with ready device and safe cleanup. A separate alias registration
was refused because the package already had a catalog identity. The existing
Homeboard catalog was briefly changed to the probe activity, then restored to
`com.gathin.tvhome.MainActivity` immediately after the probe operation started.
Normal MainActivity registration and the same frozen hash were confirmed.
Only this work's superseded launch continuations were canceled; other owners'
queued jobs and Personal use markers were retained.

This proves the Android-side HTTP and configuration integration, **not an external
physical phone connection**. Chromium/WebKit fixture tests prove the browser UI
independently. A real phone on the household LAN is the remaining acceptance gate.

## Remaining boundaries

Local HTTP is intended for a trusted home network and is not encrypted. No cloud
relay, persistent pairing, always-on background service, phone geolocation or
cross-TV settings transfer is included. Physical-phone Safari/Chrome acceptance,
network isolation variations and crash/failure injection remain separate from
container browser checks. Provider lookup still depends on available internet
and Android geocoder support. Earlier language/system Undo is handled by TV
controls; phone Undo supports widget/layout and composite phone history entries.
