# Air-quality condition background

Version **0.1.41** blends the existing reading condition color into the entire
air-quality tile, with a matching resting border. Focus keeps the tint and uses
the palette's brighter, thicker focus border. The existing UI background opacity
still applies. Missing readings use the neutral palette surface.

Container `task deploy` checks passed: calendar pipeline, deployment checks,
JVM unit tests, translation parity, Android lint and debug APK assembly.
`git diff --check` passed.

Frozen APK: `artifacts/deployments/0.1.41/homeboard.apk`, SHA-256
`4280e9a55bffe44874ac6656827c4f400cba6c4c8047392c19340f5d39a2a490`.
Source base: `460eea9`, with the local AirQualityCard background change recorded
in the deployment source manifest.

- Cast1 background job `J‑7d30894706a4` refused installation because Homeboard
  was foreground. Retried the identical frozen APK using the complete check
  recipe in `J‑ffb2d81eb70f`, which succeeded. Its screenshot confirms a blue
  full-tile background for Lumpini Park's VERY GOOD reading (9.1 µg/m³), with
  readable station, value, badge, trends and status text.
- Cast2 job `J‑11858913ded5` has a successful install-stage receipt and verified
  installed SHA-256 matching the frozen APK. Its automatic verify-launch
  continuation is queued during Personal use; foreground acceptance is deferred.

Evidence: `artifacts/deployments/0.1.41/chromecast-test-01/check-evidence/` and
`artifacts/deployments/0.1.41/chromecast-test-02/evidence/`.
Only the live blue condition has visual device evidence. The remaining condition
colors, model reading, missing reading, focus transitions, opacity changes and
refresh failure were not separately exercised on a TV in this pass.
