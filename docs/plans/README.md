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
| [Projectivy wallpaper plugin host](2026-11-16-projectivy-plugin-host.md) | Hosts the public wallpaper-provider v1 API, adds independent UI opacity and groups dense All Apps artwork and circular masking under Icons. | [`ad798a2`](https://github.com/wbniv/homeboard-tv/commit/ad798a2), [`81a9270`](https://github.com/wbniv/homeboard-tv/commit/81a9270) | Feature |
| [Multi-source holiday and Buddhist flower-work calendar](2026-11-16-thai-calendar-widgets.md) | Implements the combined Calendar display with persisted range settings and reviewed offline JSON; plans live providers for Thai closures, Spanish/Catalunya/Tarragona/Tivissa, US federal and mainland China holidays and Buddhist dates, feeding one event-only Calendar widget with flag-color card edges and a configurable shared holiday lookahead and separate Monday-week ranges. | Plan in progress | Feature |

| [Language setting and English/Thai translation](2026-11-16-language-setting-and-translations.md) | Implements a native language compass and core English/Thai resources; tracks remaining review and acceptance gates. | Worktree implementation | Feature |

| [Rain widget: hourly outlook and the following days](2026-11-16-rain-widget.md) | Implements configurable hourly and following-day rain forecasts, location lookup, animated clouds and adaptive 1×1-to-full-screen layouts; records container checks and pending Cast1 validation. | Implemented; Cast1 validation pending | Feature |

| [First-page rain and a bounded Favorites widget](2026-11-16-first-page-rain-and-favorites-container.md) | Implements Rain and a resizable Favorites container on page 1, with internal scrolling, directional wedges, remote focus handoff and preserved app order. | 0.1.23 built; Cast1 deployment queued | Feature |

| [Rain defaults to device location](2026-11-17-rain-default-device-location.md) | Implements automatic use of granted device location, fixed-place overrides, bounded lookups and permission-aware fallback. | 0.1.26 installed on Cast1 and Cast2 | Fix |

| [Calendar one-row resize](2026-11-17-calendar-one-row.md) | Allows a persisted 2×1 minimum and a compact calendar strip, with resize and device acceptance checks. | 0.1.27 installed on both TVs; Cast1 resize verified | Fix |

| [Default dashboard and empty-page navigation](2026-11-17-default-dashboard-and-empty-page-navigation.md) | Keeps all four default widgets on one screen, applies the preset immediately with Undo and fixes Right to All Apps. | 0.1.32 installed on both TVs; actual Cast1 Right/Back verified | Fix / Feature |

| [Every widget: full-screen detail and dashboard restoration](2026-11-17-widget-full-screen.md) | Implements richer full-screen content for all widget types, four-week Calendar detail, rain amount/probability charts, nearby station maps and a dense Favorites banner grid/app panel; Back restores Home. | 0.1.36 installed on both TVs; core Cast1 checks passed | Feature |

| [Default dashboard: hourly rain and bottom Favorites](2026-11-17-default-dashboard-hourly-rain.md) | Proposes shallow full-width hourly rain, compact air quality with 24-hour/30-day sparklines and a full-width bottom Favorites row. | 0.1.35 installed on both TVs; Cast1 layout and navigation verified | Feature / Fix |

| [Configure Homeboard widgets from a phone](2026-11-17-phone-widget-editor.md) | Adds a TV-hosted local web editor with temporary TV-approved pairing, per-instance widget/layout controls, draft preview, atomic Apply and Undo. | 0.1.40 installed on both TVs; native HTTP and browser UI passed; physical-phone LAN acceptance pending | Feature |

---

## How this index was derived

- **Order** = each plan's *creation* commit (oldest commit that touched the file), by committer date.
- **Commit(s)** = the full `git log --follow` set per plan, oldest → newest.
- **Summaries / categories** are concise editorial descriptions of each plan.
- **Seeded** 2026-11-15 via `seed-plan-index.sh` — eight plan rows, with summaries
  and categories filled by hand.
- **Updated** 2026-11-16 to ten plans, including the Google TV app focus implementation.
- **Draft added** 2026-11-16 for Home favorite app sizes, bringing the index to eleven plans.
