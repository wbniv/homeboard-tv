# Full-screen widget validation · 17 November 2026

The [full-screen plan](../plans/2026-11-17-widget-full-screen.md) is implemented
in the current candidate. Short OK/click opens the shared native full-screen
window; hold OK retains widget settings/actions. Calendar, Rain, air quality and
Favorites each supply richer content through an exhaustive widget-type factory.

## Candidate 0.1.33

- Base revision: `d77eeab`, plus the full-screen implementation and resources.
- Frozen APK: `artifacts/deployments/0.1.33/homeboard.apk`.
- SHA-256: `6bbaa155e949a082582096ea1819765acca7e1961a188f498549ff9633b32fcf`.
- Container checks: **436 JVM tests**, zero failures/errors/skips; six calendar
  pipeline tests; two deployment helper checks; **535** paired English/Thai
  Homeboard resources; Android lint with zero errors. Debug APK assembly passed.
- `source-sha256.json` and `deployment.json` in the artifact directory preserve
  the exact build inputs and original install attempts.

New model tests cover four complete week boundaries and navigation, the expanded
calendar query independent of compact seven-day settings, station-coordinate
validation/duplicates/missing readings, nearby distance filtering, map projection,
expanded rain day intervals, independent missing amount/probability and unweighted
rain amounts. The widget picker and exhaustive factory share `WidgetKind`.

## Device installation

Cast1 background job **J‑22b254cde936** refused installation over foreground
Homeboard (`install-over-foreground`). The complete install/brief-launch retry
**J‑96ada9a19787** completed; its installed-hash evidence and initial dashboard
capture are in `artifacts/deployments/0.1.33/chromecast-test-01/foreground-evidence/`.

Cast2 install job **J‑13029358ffb9** verified the identical installed hash. Its
foreground `verify-launch` continuation is queued during Personal use. Evidence:
`artifacts/deployments/0.1.33/chromecast-test-02/evidence/`. Its local waiting client
was stopped after exporting the verified receipt; the queued device job remains.

## Actual remote acceptance

Recipe: `dev/device/widgets-full-screen-check.json`. This is specific to the
observed one-screen dashboard with Calendar initially focused, Rain on its left,
air quality below Rain and Favorites on the lower right. It uses actual short
80 ms OK and 800 ms hold inputs. It does not change any widget settings or launch
favorite apps.

Job **J‑9e4a7f029fd6 completed successfully**. Reviewed evidence is in
`artifacts/deployments/0.1.33/chromecast-test-01/fullscreen-evidence/`:

- `input-frame-002.png`: short 80 ms OK opens Calendar with **28 visible cells**,
  four complete Monday–Sunday weeks and a highlighted-day detail pane.
- `input-frame-003.png`: Right selects the next date and updates its detail.
- `input-frame-004.png`: OK opens nested detail; two Back presses return to Home.
- `input-frame-005.png`: the dashboard's original layout and Calendar focus return.
- `input-frame-006.png`: an 800 ms hold opens the widget settings/actions menu;
  releasing it does not expand Calendar.
- `input-frame-007.png`: short OK opens Rain directly with the seven-day overview,
  amount chart, independently aligned probability strip and exact interval detail.
  The captured day has zero forecast rain, while its nonzero probabilities remain
  visible independently.
- `input-frame-008.png`: real OpenStreetMap tiles, nearby AirBKK station markers,
  station measurements, an actual device fix with accuracy/age, and the station pane.
- `input-frame-009.png`: Favorites uses three banner columns and three visible
  rows, with full highlighted-app name, version/update and actions on the right.
- `input-frame-010.png`: Back restores the original dashboard and favorite selection.

This pass found station clusters could overlap the device dot; the final polish
draws the location above station markers with a contrasting caption. It also keeps
Rain's All available window active when choosing later days and reduces repeated
Calendar prose in the preview while retaining full source links in drilldown.
That revision was built with the finished hourly-dashboard work in **0.1.35**.

## Combined deployment 0.1.35

- Frozen APK: `artifacts/deployments/0.1.35/homeboard.apk`.
- SHA-256: `b77e9129e5d89b8bf77e24afe467b4fbc841af4684f91e4aae7d072884e92d87`.
- **437 JVM tests** passed with zero failures/errors/skips, plus the pipeline,
  deployment helper, 535-resource translation parity and Android lint checks.
- Every launcher file matches the frozen 0.1.35 source manifest. Both TVs have
  verified installations of this exact APK. Cast1 install/navigation acceptance
  **J‑0f70faefb3e1** is recorded under `artifacts/deployments/0.1.35/hourly-followup/`;
  Cast2 proof is under `cast2-hourly-install-proof/` in the same version directory.
- The other agent's completed hourly Rain/compact air-quality/Favorites-ribbon
  work is included; see [its record](2026-11-17-hourly-dashboard.md).
  Its Cast2 preset application **J‑720a69f46fd1** stays queued during Personal use.

The first final-polish script **J‑f7c8ab638f28** assumed Favorites focus survived
the coordinator's fresh launch. The actual start capture showed the device-settings
button focused, and its first OK opened Android settings. The foreground guard
stopped the script; automatic readd **J‑57f13f63c145** restored Ready. This is a
test navigation error, not a widget crash.

The corrected recipe `widgets-full-screen-hourly-dashboard.json` starts from that
observed settings focus, moves down through Rain to Calendar, then checks Calendar,
the location marker above station clusters and expanded Favorites from the ribbon.
Job **J‑83814fc0fd45 completed successfully**. Evidence in
`artifacts/deployments/0.1.35/fullscreen-final-evidence/` confirms the four-week
Calendar with shorter highlighted-day preview, the blue actual-location marker
and contrasting caption above station clusters, expanded Favorites from the new
ribbon, and restoration of the hourly dashboard.

Will subsequently requested more than nine Favorites slots. The final density
change targets **4×4 capacity** using the measured viewport and reflows at larger
font scales. Its new geometry test checks 16-slot capacity, bounded row height
and larger-text reflow.

## Final density deployment 0.1.36

- Frozen APK: `artifacts/deployments/0.1.36/homeboard.apk`.
- SHA-256: `3bdecc4482253b0df19732d448ea561c5668a9a50daf8d5a7d2bd4101040cc2e`.
- **438 JVM tests** passed with zero failures/errors/skips, plus the pipeline,
  deployment helper, translation parity and Android lint checks.
- Cast2 job **J‑2fa88a5990eb** verified the installed hash. Its foreground
  continuation remains queued during Personal use; the local waiting client was
  stopped without canceling the durable job.
- Cast1 background install **J‑49b8cbacde1f** refused installation over foreground
  Homeboard. The follow-up **J‑b90c5f69ab86** verified the exact installed hash and
  completed the density check. Captures in
  `artifacts/deployments/0.1.36/favorites-density-evidence/` show **ten existing
  favorites together**, four columns, the app detail pane and Back restoring Home.
  No favorites were added by the test.

The first density script kept focus on Restore dashboard, so Right moved to
Configure rather than changing apps. The saved recipe now presses Down into the
app grid before checking Right and the highlighted-app pane.
The corrected check **J‑e031c26155b8 completed successfully**. Its reviewed captures
in `artifacts/deployments/0.1.36/favorites-navigation-evidence/` show selection
moving from TV Media Phase 0 to World Foundry Aquarium Screensaver, with the right
pane immediately updating the full app name, version and update date. Back restores
the dashboard and its Favorites selection.

## Limits still requiring acceptance

- Actual pointer input, enlarged Thai text, recreation, settings return and
  Favorites app launch/return require separate coordinator-supported acceptance.
- Regional station coverage remains AirBKK. Actual location, map tiles, station
  feed and station history have separate availability; no global physical-station
  provider is inferred from model coverage.
- Calendar uses the reviewed offline provider snapshot; expansion does not add
  live emergency closures or an unimplemented lunar calculator.
