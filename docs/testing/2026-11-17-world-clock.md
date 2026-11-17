# World clock validation · 17 November 2026

The [clock plan](../plans/2026-11-17-world-clock-widget.md) is implemented in the
combined source. Implementation began in `.worktrees/world-clock`, based on
`6941aa2`. Integration used a context-checked patch for tracked files and copied
only owned new files. Existing air-quality backgrounds, phone identity/generation
tracking, app artwork and expansion animation remain in the combined source.

## Native container checks

The isolated native check passed **456 JVM tests**, with zero failures/errors/skips,
including twelve clock tests. Calendar pipeline (six) and deployment helper (two)
checks passed, alongside **603** paired English/Thai Homeboard resources and Android
lint with zero errors. The native debug APK assembled successfully.

Clock tests cover single-instant conversion; DST skipped/repeated hours and their
distinct offsets; fractional-hour zones and reference boundaries; date-line
differences; duration-overlay clipping; persistent IDs, primary/reference and
independent widget instances; strict draft validation; unknown stored zones;
offline catalog search; map projection and bundled polygon bounds.

The map uses Natural Earth 1:110m land polygons, rounded to three decimal places
and bundled in `world-clock-land.json`. Source checksum and public-domain terms
are retained in `assets/licenses/world-clock-natural-earth.txt`; there is no map
network dependency. Sun/moon and strip shading explicitly represent local
06:00–18:00 clock hours, not measured solar conditions.

## Phone checks

`dev/world-clock-model-check.cjs` passed in the Playwright container against the
actual editor model. It verifies defaults, validation, independent display merges,
atomic places/primary/reference conflict resolution, replaced-instance recovery
under a new widget identity, and English/Thai clock-string parity.

`dev/world-clock-phone-check.cjs` passed in **Chromium and WebKit**, with Pixel and
iPhone viewport profiles. It renders the actual clock form using the editor UI
helper contract, actual strings/drag module and active styles. Checks include:

- TV-zone snapshot in Bangkok while the browser zone is New York.
- Offline place search, Tivissa metadata without an invented airport code,
  custom labels and text escaping.
- Explicit primary/reference selection, Studio cities preset and stable identities
  across pointer dragging, Alt+Arrow reordering and Move up/down.
- Pointer cancellation removes ghosts/markers without changing order; draft Undo
  restores order. Shared editor disposers cancel dragging on rerender/navigation.
- Time format, seconds, Thai fields and phone-width layout.

Artifacts are local under `artifacts/world-clock-phone/`. The combined
`task phone:check` also passed the dashboard controller's existing scenarios and
both clock checks before deployment. Native API Apply/Undo remains server-validated;
component tests alone do not establish a physical phone's LAN connectivity.

## Combined deployment and TV acceptance

Deployment **0.1.43** is frozen under the shared deployment owner:

- APK: `artifacts/deployments/0.1.43/homeboard.apk`.
- SHA-256: `514b60dee90435ac7373a4f0ca83b507acf367b37bf94179488a3c7078153254`.
- Combined checks: **458 JVM tests**, zero failures/errors/skips, **603** paired
  resources and Android lint with zero errors (349 warnings).
- Initial install submissions: Cast1 **J‑82155f6d43c6**, Cast2 **J‑4e7cc10663a3**.

No app source edits are allowed after the freeze; a source defect requires a new
version before another build/install. A code audit found an optional-seconds
cadence issue when returning to Now after restoring comparison mode. The simple
correction will be tracked under the next deployment if needed; 0.1.43 acceptance
does not establish that restoration case.

The debug-only `WorldClockProbeActivity` seeds small, wide and tall clock panels
in a separate preference namespace. It does not edit the household dashboard or
favorite apps. The coordinator keeps one catalog identity for the package:
temporarily register `homeboard-tv` with this activity against the identical frozen
APK, submit its complete check, then restore the catalog to MainActivity after
the job completes. Hold the registration for the whole session: the preceding
phone review raised a possible launch/registration timing issue. Coordinate catalog ownership with the
native phone review; registrations must not race. Run
`dev/device/world-clock-check.json` on Cast1 through the coordinator and
export/review the evidence. Intended checks are actual short OK, highlighted-place
details, nested Back, future comparison, map selection, Home restoration and hold
for settings. A completed recipe is execution evidence; each capture must be
reviewed before claiming those behaviors passed.

Final frozen hash, coordinator jobs and reviewed captures will be recorded here
after acceptance. Enlarged Thai text, arbitrary pointer input and system-clock
changes remain separate device acceptance cases unless explicitly exercised.

### Reviewed 0.1.43 clock captures

Cast1 **J‑a84bba67da26 completed Ready**. Its command log confirms an explicit
force-stop and launch of `com.gathin.tvhome.WorldClockProbeActivity`. The catalog
was held on that activity until the job completed, then restored to MainActivity.
Evidence: `artifacts/deployments/0.1.43/world-clock-review/`.

- `input-frame-001.png`: isolated one-page Home contains the intended 1×1, wide
  one-row and tall clock fixtures; no household dashboard is used.
- `input-frame-002.png`: actual short OK opens native full-screen clocks, with
  Tivissa selected and its exact date, zone, offset and reference difference.
- `input-frame-003.png`: Right selects Beijing and immediately updates the pane.
- `input-frame-004.png`: OK opens the complete selected-place detail.
- `input-frame-005.png`: nested Back works; comparison shows 20:00 in Bangkok,
  the aligned city times and Denver's 06:00–06:30 interval.
- `input-frame-006.png`: Right advances the shared comparison by 30 minutes,
  updating all clocks, detail start/end and the overlay together.

The later script segment sent an extra Up after leaving comparison and reached
Configure instead of Map. Frames 007–010 are not accepted as map, dashboard-restore
or hold evidence. The corrected recipe removes that Up; a shorter dedicated
`world-clock-map-check.json` is also available.

The captures exposed two clock-only polish issues: the smallest panel's hidden
place count overlaps its date, and a selected city can have its timeline row
outside the viewport. The 0.1.44 follow-up reserves a small count footer and scrolls
the selected timeline row into view on activation/selection/Compare. It also fixes
the optional-seconds cadence found by code inspection. These changes preserve the
phone controller, shared expansion behavior and household configuration.

### 0.1.44 implementation checkpoint

- Frozen APK: `artifacts/deployments/0.1.44/homeboard.apk`.
- SHA-256: `9600eb9015a4a17a636379bfc6333e4c833302b01ec9ac8a712426b03a975049`.
- The combined container check again passed **458 JVM tests**, **603** paired
  English/Thai resources and Android lint with zero errors; APK assembly passed.
- Initial Cast1 background install **J‑56a3214ee400** refused installation over
  foreground Homeboard. The shared deployment owner is handling a complete
  foreground retry. Cast2 install submission is **J‑16a32f357557**.
- The three small clock corrections are included in this frozen version.
  Actual 0.1.44 clock visual/map/restoration acceptance remains pending; the
  earlier core screenshots above are explicitly from 0.1.43.

At the requested commit/push checkpoint, retain these limits rather than treating
the unreviewed map-script segment or a coordinator completion alone as a pass.
Will's subsequent request to make his own saved clock settings/size the new-widget
defaults is a separate follow-up: read the actual household configuration and
preserve existing cards; never infer that preset from the isolated review fixture.
