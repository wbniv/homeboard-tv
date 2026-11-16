# Plans — homeboard-tv

*Index of the plans in this directory — one row per `*.md`, **oldest → newest** by the commit that first
introduced it, with a one-sentence summary, the commit(s) that touched it, and a category. Summaries can be
auto-generated from each plan's TL;DR (refine as needed); the shared `check-plan-index.sh` drift hook flags
missing rows on commit.*

**Categories** (adapt to the project): `Feature` · `Fix` · `Investigation` · `Tooling` · `Docs` · `Infra` · `Platform`.

| Plan | Summary | Commit(s) | Category |
|---|---|---|---|
| [First integration: Bangkok air quality](2026-11-15-air-quality.md) | Adds a public AirBKK PM2.5 widget with station selection and visible source data. | [`b2c5553`](https://github.com/wbniv/homeboard-tv/commit/b2c5553) | Feature |
| [Flexible Home grid](2026-11-15-flexible-home-grid.md) | Defines responsive Home columns, spanning panels, per-panel sizing and focus behavior. | [`b2c5553`](https://github.com/wbniv/homeboard-tv/commit/b2c5553) | Feature |
| [Homeboard TV menus: icons, no scrolling, goal directed](2026-11-15-menu-redesign.md) | Groups remote-friendly menus into compact screens with labeled actions and clear recovery. | [`b2c5553`](https://github.com/wbniv/homeboard-tv/commit/b2c5553) | Feature |
| [Homeboard TV project foundation](2026-11-15-project-foundation.md) | Records product identity, CleanYourTV provenance, container builds and release boundaries. | [`b2c5553`](https://github.com/wbniv/homeboard-tv/commit/b2c5553) | Platform |
| [Homeboard TV: widgets first, All Apps to the right](2026-11-15-widget-home.md) | Sets the widgets-first Home, rightward All Apps flow and separate tile management actions. | [`b2c5553`](https://github.com/wbniv/homeboard-tv/commit/b2c5553) | Feature |
| [About dialog, free grid placement and per-widget air quality](2026-11-16-about-move-air-widgets.md) | Tracks the About view, free grid placement and independent location-aware air widgets. | [`b2c5553`](https://github.com/wbniv/homeboard-tv/commit/b2c5553) | Feature |
| [Palette-driven surfaces, condition trends and directional panel resizing](2026-11-16-palette-trend-resize.md) | Tracks palette-derived UI, condition-colored PM2.5 trends, panel resize and current Cast2 verification blocker. | [`b2c5553`](https://github.com/wbniv/homeboard-tv/commit/b2c5553) | Feature |
| [PM2.5 30-day history and sparklines](2026-11-16-pm25-30-day-sparkline.md) | Adds a bounded 30-day history and condition-colored 24-hour and 30-day traces. | [`b2c5553`](https://github.com/wbniv/homeboard-tv/commit/b2c5553) | Feature |
| [Compass pie menu redesign](2026-11-16-pie-menu-redesign.md) | Replaces card menus with compass choices, then revises reversible actions around Undo, bounded grid controls, clearer Restore wording and an About page. | [`9cb567a`](https://github.com/wbniv/homeboard-tv/commit/9cb567a) | Feature |
| [Google TV app icons and glowing focus](2026-11-16-google-tv-app-focus.md) | Matches circular app artwork, focus geometry and halo to Cast2's reference and records numbered deployments and device validation. | [`531779b`](https://github.com/wbniv/homeboard-tv/commit/531779b) | Feature |
| [Home favorite app sizes](2026-11-16-home-favorite-app-sizes.md) | Adds independent Home artwork, separate dense icon/banner grids on both surfaces, reflow and Undo; records the combined-build handoff. | [`be49bd7`](https://github.com/wbniv/homeboard-tv/commit/be49bd7), [`9b2da17`](https://github.com/wbniv/homeboard-tv/commit/9b2da17), [`6a0366b`](https://github.com/wbniv/homeboard-tv/commit/6a0366b), [`4f3dd10`](https://github.com/wbniv/homeboard-tv/commit/4f3dd10) | Feature |

---

## How this index was derived

- **Order** = each plan's *creation* commit (oldest commit that touched the file), by committer date.
- **Commit(s)** = the full `git log --follow` set per plan, oldest → newest.
- **Summaries / categories** are concise editorial descriptions of each plan.
- **Seeded** 2026-11-15 via `seed-plan-index.sh` — eight plan rows, with summaries
  and categories filled by hand.
- **Updated** 2026-11-16 to ten plans, including the Google TV app focus implementation.
- **Draft added** 2026-11-16 for Home favorite app sizes, bringing the index to eleven plans.
