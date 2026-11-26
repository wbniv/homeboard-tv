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
| [Visible wallpaper preview](2026-11-17-visible-wallpaper-preview.md) | Reveals media beside directional controls without changing saved opacity; Up opens Icons and Down opens Background. | Working tree | Fix |
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

| [World clock widget](2026-11-17-world-clock-widget.md) | Implements configurable city clocks, shared city colors, an offline map, full-screen duration comparison and TV/phone settings inspired by wald3n.com. | 0.1.44; container checks and Cast1 core/map/restoration/hold checks passed; timeline auto-scroll limitation recorded | Feature |

| [Phone controller: Alan Cooper goal-directed design review](2026-11-17-phone-editor-goal-directed-design.md) | Dashboard-first phone editing, directly reorderable lists, contextual controls, one explicit TV update and conflict/draft recovery. | 0.1.47 installed on both TVs; native HTTP/browser checks passed; physical-phone LAN pending | UX / Feature |

| [World clock saved preset](2026-11-17-world-clock-saved-preset.md) | Uses Will’s captured six-place, 24-hour, 8×1 clock as the default for new TV and phone-created widgets. | 0.1.47 installed on both TVs; native preset creation and browser checks passed | Feature |

| [World clock in the fixed top bar](2026-11-17-world-clock-header.md) | Places filled city cards left of settings, outside the dashboard grid, with fixed-slot navigation, phone controls and safe data transition. | 0.1.52 installed on both TVs; Cast1 native checks passed | UX / Feature |

| [Phone icons for each TV](2026-11-17-phone-tv-shortcuts.md) | Adds named home-screen shortcut guidance, per-TV identity URLs and icon metadata without saving pairing credentials. | 0.1.52 installed on both TVs | Feature |
| [Hold Back to undo on TV](2026-11-17-hold-back-undo.md) | Makes one-second Back undo one saved change on Home/All Apps, with progress, confirmation and preserved short Back/editor behavior. | Implemented; Cast1 verified, Cast2 install queued | UX / Fix |
| [Opacity belongs to widget backgrounds](2026-11-18-widget-background-opacity.md) | Removes full-screen palette dimming from widget opacity and keeps wallpaper/video brightness independent. | Implemented; Cast1 verified, Cast2 install queued | UX / Fix |

| [Wallpaper provider inventory and compatibility testing](2026-11-18-wallpaper-provider-compatibility.md) | Inventories all ten catalog entries and eight additional projects/leads, with per-provider cases and isolated coordinator testing. | 18 dispositions; partial compatibility evidence collected | Investigation / Tooling |

| [Provider testing spike and user installation](2026-11-18-provider-testing-spike-and-installation.md) | Defines a bounded 18-provider spike, security exclusions, evidence requirements and a Google Play first installation flow without bundled APKs. | Implemented 0.1.59; seven Cast1 screenshots passed; Cast2 queued | Investigation / UX |
| [Thai years and time presentation](2026-11-18-thai-years-and-time-presentation.md) | Thai BE years, localized duration units and selective metadata clock/stopwatch icons, following Parking Space and Python TUI conventions. | Implementation authorized; verification in progress | Localization / UX |

| [Optional TV cleanup and restore](2026-11-18-tv-cleanup-and-restore.md) | Copies reviewed existing cleanup/recovery code and tests into an optional companion integration, with strict protection, scan-only defaults, restore-first gates, mockups and Cast1/Cast2 screenshot proof. | Read-only inspection milestone implemented; real cleanup gates open | Feature / Safety |
| [Client-side Wan Phra calculation](2026-11-19-client-side-wan-phra.md) | Implements the pinned Java Thai civil calendar and shared flower anchors, with independently checked 2024–2027 observances and screenshot acceptance. | Working tree | Feature |
| [Two-year offline holiday updates](2026-11-19-offline-holiday-updates.md) | Implements persistent annual data, source updates, safe publication, native refresh controls and rollover checks, with explicit publisher/coverage limits. | Working tree | Feature |

| [Publish Homeboard TV APK downloads on biohack.net](2026-11-19-biohack-apk-publication.md) | Publishes a signed public APK, a biohack.net download page, versioned hosting and updated installation guides, with phone/desktop review previews. | Public 0.1.73 published | Infra / Docs |

| [Grocery list: phone entry, products and household staples](2026-11-19-grocery-list-widget.md) | Designs Phase 1 phone-first list entry and persistent staples, Phase 2 products/units and reported stock, and the Phase 3 purchase/sync boundary, with TV/phone mockups and workflow diagrams. | Merged into main; Cooper revision in 0.1.81; Cast2 unconfirmed | Feature / UX |

| [Opt-in ads that pay the user (Homeboard Rewards)](2026-11-19-ads-that-pay-the-user.md) | Designs off-by-default rewards: a 50% commission share paid to PromptPay without stored balances, gated sponsored offers, a PDPA consent ladder, a no-commerce build, the data boundary, a legal checklist and kill criteria, with TV/phone mockups. | Plan proposed | Feature / Policy |

| [Groceries: Cooper goal-directed design review](2026-11-19-grocery-goal-directed-design.md) | Applies shared Cooper filter v1.0 to quick capture, staple review, contextual quantities and recoverable drafts. | Merged into main; Cast1 0.1.81 verified | UX |

| [Grocery Phase 3: away-from-home and offline shopping](2026-11-19-grocery-shopping-sync.md) | Records required offline resilience, the selected API Gateway/Lambda/DynamoDB backend, durable phone operations, purchase/Undo semantics and distinct TV delivery, with mockups and state/backend diagrams. | AWS backend selected; implementation planned | Feature / UX |

| [Cooking timers and event alarms](2026-11-19-cooking-timers.md) | Defines cooking-first duration timers, one-off event alarms, background sound, native sizes and direct phone controls with lifecycle and screenshot acceptance. | [`97bdbc9`](https://github.com/wbniv/homeboard-tv/commit/97bdbc9) | Feature / UX |
| [Publish cooking timers and household alarms](2026-11-19-timers-publication.md) | Updates the signed public download, user guides and website with reviewed timer screenshots and scoped publication checks. | [`97bdbc9`](https://github.com/wbniv/homeboard-tv/commit/97bdbc9) | Release / Docs |

---

## How this index was derived

- **Order** = each plan's *creation* commit (oldest commit that touched the file), by committer date.
- **Commit(s)** = the full `git log --follow` set per plan, oldest → newest.
- **Summaries / categories** are concise editorial descriptions of each plan.
- **Updated** 2026-11-19 with cooking timers and their public-release workflow.
- **Seeded** 2026-11-15 via `seed-plan-index.sh` — eight plan rows, with summaries
  and categories filled by hand.
- **Updated** 2026-11-16 to ten plans, including the Google TV app focus implementation.
- **Draft added** 2026-11-16 for Home favorite app sizes, bringing the index to eleven plans.

- [Standalone cleanup connections](2026-11-19-cleanup-connections.md): private pairing,
  connection, identity confirmation and inspection; container/browser fixture proof.

- [Cleanup execution and recovery](2026-11-19-cleanup-execution.md): full inventory,
  reviewed selections, recovery-before-confirmation, execution and saved sessions.
- [Default YouTube favorite](2026-11-19-default-youtube-favorite.md): seed installed
  YouTube on new dashboards while preserving saved choices.
| [Android app widgets](2026-11-19-android-app-widgets.md) | Hosts installed Android widgets with saved instances, expansion, Undo and explicit Chromecast permission setup; records device and provider acceptance gates. | This change | Feature |
| [Timers on every page](2026-11-20-timers-on-every-page.md) | Plans the shared timer tick and visible indicators across Home, All Apps and expanded widgets, with staged acceptance. | `aa27069`, `2e38b41`, `2fd314a`, `2b05cbd` | Feature |
| [Native 4K dashboard](2026-11-23-native-4k-dashboard.md) | Records the actual 4K dashboard proof, native overlays and device acceptance while keeping automatic launcher integration and remaining gates open. | `c3234b6` | Investigation |
| [Rendering resolution in About](2026-11-25-rendering-status.md) | Shows measured dashboard and rendering-surface sizes separately from TV output, with device checks and deployment evidence. | This change | Feature |
| [Screensaver shortcut](2026-11-23-screensaver-shortcut.md) | Adds the top-row shortcut and records Cast1 chooser/Back acceptance with other device and presentation checks pending. | `c3234b6` | Feature |
| [Trello board widget](2026-11-23-trello-board-widget.md) | Proposes a read-only board widget and richer expanded view; source, authentication and implementation remain undecided. | `ff92b1a` | Feature |
| [Air-quality startup without a preference wait](2026-11-23-air-quality-startup.md) | Loads saved air settings and cache on a worker; preserves selections/history and records Cast1 0.1.130 acceptance with other installs queued. | `2c370f4`, `de27e5b` | Fix |
| [What a screensaver app must offer Homeboard (Stage 5)](2026-11-26-screensaver-companion-contract.md) | Defines the metadata and provider admission a companion screensaver app needs; proposed, with Stage 5 blocked on WorldFoundry. | [`12e5c64`](https://github.com/wbniv/homeboard-tv/commit/12e5c64) | Docs |
- [Screensavers: choose, configure and get more](2026-11-20-screensavers.md): one Screensaver
  screen for built-in, provider and Android screensavers, with mockups; plan for review.
- [Screensavers Stage 0: the probe recipe](2026-11-20-screensavers-stage0-probe.md): how the
  plan's eleven device questions are answered through the coordinator, and the two
  coordinator additions the write tests need; draft for review.
- [Control interface: one command model, links and a local listener](2026-11-20-control-interface.md):
  one vocabulary and state for navigation-only links, a shell-only receiver, an opt-in LAN
  listener with devices approved on the TV, and in-process callers; plan for review.
- [Home button: make the remote's Home open Homeboard on Google TV](2026-11-20-home-button.md):
  Home settings → Home button, through the accessibility helper with a runtime target, honest
  running state, repair and Undo; Cast1 physical repeat/routing and recovery checks
  passed, Cast2 0.1.119 install verified. Update/helper acceptance and precise timing
  remain open; see the [current evidence](../testing/2026-11-23-home-button.md#remaining-checks).
