# Default dashboard and empty-page navigation · 17 November 2026

## Delivered behavior

New installations create Rain, Calendar, Air quality and Favorites together. The
factory uses the fitted grid, placing all four on one page with four or more rows
and Rain/Favorites first on shorter grids. The preset preserves selected widget
configuration, favorite order, unrelated preferences and additional widgets.

Existing layouts, including explicitly saved empty Home, load unchanged. The
startup-only Rain/Favorites rearrangement was removed from saved-layout loading.
Home settings → Widgets / Restore → Default dashboard offers an arrangement
preview, page inspection, Cancel and a single Undoable apply. Leaving the activity
cancels an uncommitted preview.

An empty page receives canvas focus rather than falling back to Device settings.
Right follows the normal Home-page sequence and then opens All Apps. Up reaches
the settings row; Right from its final button also advances from an empty page.
Down returns from the header to an empty canvas. All Apps can return to empty
Home, and a Favorites container with no apps remains a navigable widget.

## Container checks

0.1.30 passed 429 JVM tests, six calendar pipeline checks, two deployment checks,
English/Thai parity for 467 resources, Android lint (zero errors; 316 warnings) and
debug APK assembly. New checks cover default composition, all supported grid
shapes, deterministic placement, overflow, saved-empty serialization, preservation
of existing settings and the finite page-advance policy.

Older tests now construct explicit Air-only fixtures where that initial state is
what they exercise; they no longer depend on the product's first-run composition.
Two earlier attempts consumed 0.1.28 and 0.1.29 before packaging: first an Undo test
compared JSON key order, then its replacement called a comparator missing from
Android's test compile API. Direct restored-state assertions corrected that test.
Neither attempt produced an installed APK.

## Deployment

The identical frozen 0.1.30 APK is installed on both TVs. Its SHA-256 is
`6df4db825788c328050565adaea0532e7febf68123635aef34506a85c654fa8c`.

- Cast1: **J‑3169d1d6da5d** completed installation and launch verification with
  matching installed hash and device ready.
- Cast2: **J‑66af97aacafa** reports a verified installed hash matching the frozen
  APK. Its foreground verification continuation is deferred during Personal use.
  The hold and default Home app were not changed.

Per-target evidence and source hashes are under `artifacts/deployments/0.1.30/`.
This session cancelled its superseded 0.1.26 verification **J‑5ead5cfce69b**; another
session's deferred work was left intact.

## Physical acceptance

Debug-only DashboardProbeActivity uses isolated layout, rain and air preferences.
It dispatches real key events through MainActivity and checks empty Home → All
Apps, settings-header escape, intermediate blank pages, final page → All Apps,
preset preview/Cancel, saved apply and Undo back to navigable empty Home. Final
screenshots also inspect the four-widget composition and readable controls.

Owned Cast1 acceptance **J‑d3ebe4296df6** completed with cleanup. The fixture reached
its final PASS report, confirming the preceding native assertions for navigation,
header escape, blank intermediate pages, preset Cancel/apply, persistence and Undo.
The screenshots show all four widgets together. They also exposed clipping of the
second line in empty Favorites: Add apps was not readable. Child header geometry
was being updated after measurement in onSizeChanged. The correction moves that
geometry into onMeasure, and the isolated probe now asserts a measured two-line
empty action. This requires a newly numbered 0.1.31 build and another capture. Additional permission,
font-scale and Cast2 foreground acceptance remain separate from installation.


## Final build 0.1.31

The Favorites correction passed the same 429 JVM tests, calendar/deployment
checks, resource parity and lint before packaging. The identical frozen APK
`5f5e451f6f659640979fc7dc484c95c3d0b3234d0bfd8fcdc4c83afe6ecb3348` is installed
on both TVs, confirmed by each job's installed SHA-256 proof. Cast1 installation
and launch **J‑ad576c6f9d18** completed; Cast2 **J‑0e305bf1ab78** installed in the
background and defers foreground verification during Personal use. This session's
superseded 0.1.30 verification **J‑66af97aacafa** was cancelled.

Final isolated acceptance **J‑9cf4b071ce97** checks the same navigation/Undo flow
and additionally asserts that empty Favorites has a measured two-line action.
It completed with cleanup and the final PASS report, including the measured
two-line Favorites assertion. The final screenshot was inspected: all four
widgets share page 1 and both No favorites yet / Add apps lines are fully visible.
Evidence: `artifacts/deployments/0.1.31/dashboard-acceptance/`.
The normal launch descriptor was restored to MainActivity after the owned test
activity started. All 366 frozen source-file hashes matched the implementation
before the final commit.

The native Air quality card was found to already default to automatic local model
data. The preset preserves that current default and existing source settings; the
plan and illustration were corrected instead of imposing an AirBKK station choice.
The captured model card labels its source and measurement basis explicitly.
