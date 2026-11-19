# Web dashboard module systems: design prior art for Homeboard TV

Scope: the *internal* design of self-hosted and open-source web dashboards (module API, layout engine, config schema and editor, plugin distribution), as patterns to copy or code to reuse in Homeboard TV (MIT, Java, native Android Views, D‑pad). Researched 2026‑11‑19. Each finding carries **CONFIRMED** (read on the official page, in the project's source, or from the GitHub/registry API) or **UNVERIFIED** (from memory, a snippet, or a page I could not read in full). Repository statistics come from the GitHub REST API (`gh api repos/OWNER/REPO`) on 2026‑11‑19. Systems covered: MagicMirror², Home Assistant dashboards (Lovelace) + HACS, Homarr, Homepage, Dashy, Glance, Grafana, openHAB MainUI, Node‑RED Dashboard 2.0, Smashing, Heimdall, Organizr, plus TRMNL (e‑ink dashboard with a plugin and layout‑slot model) and the MagicMirror phone‑editor modules MMM‑Remote‑Control and MMM‑Config.

## 1. Widget contract: language, lifecycle hooks, data/render split, refresh scheduling

### Takeaway
Three contract shapes recur. (a) **Front‑end object with lifecycle hooks plus an optional back‑end helper** (MagicMirror `Module.register` + `node_helper`, Smashing widget + job, Node‑RED node + Vue component). (b) **A custom element that receives config and live state** (Home Assistant cards: `setConfig`, `set hass`, sizing hints). (c) **A declarative definition record**: the host owns fetching and refresh, and the widget declares its API mapping, options and query keys (Homepage `widget.js`, Homarr `createWidgetDefinition`, Grafana `PanelPlugin`). Splitting data fetching from rendering is explicit in MagicMirror, Smashing, Homepage, Homarr and Node‑RED.

### Cited Findings

**MagicMirror² (JavaScript, Electron/browser front end + Node.js back end)**
- A module is a core file named after the module (`modulename.js`) that registers the module definition; it is required for the module to work. CONFIRMED — [MM core module file](https://docs.magicmirror.builders/module-development/core-module-file.html)
- Instance properties: `this.name`, `this.identifier` (unique per instance), `this.hidden`, `this.config` (user config merged over the module's `defaults`), `this.data` (classes, file, path, header, position). CONFIRMED — [MM core module file](https://docs.magicmirror.builders/module-development/core-module-file.html)
- Rendering: the module calls `this.updateDom(speed|options)` to ask the core to re‑render it; rendering is asynchronous, `DOM_OBJECTS_UPDATED` fires when it is done, and the update is animated only if content really changed. The stock clock calls `updateDom()` from a `setInterval` set up in `start()`, so **refresh scheduling is the module's own job**. CONFIRMED — [MM core module file](https://docs.magicmirror.builders/module-development/core-module-file.html)
- Templating alternative: `getTemplate` / `getTemplateData` feed a Nunjucks template. CONFIRMED — [MM core module file](https://docs.magicmirror.builders/module-development/core-module-file.html)
- `suspend()` is called when the module is hidden via `module.hide()`, "to perform tasks like halting the update timers". CONFIRMED — [MM core module file](https://docs.magicmirror.builders/module-development/core-module-file.html). The matching `resume()` hook and the `getStyles`/`getScripts` asset hooks are UNVERIFIED in this pass (not in the extracts I read).
- Inter‑module bus: `sendNotification(notification, payload)` and `notificationReceived(notification, payload, sender)`; the core sends `ALL_MODULES_STARTED`, `DOM_OBJECTS_CREATED` and `MODULE_DOM_CREATED` at startup. CONFIRMED — [MM core module file](https://docs.magicmirror.builders/module-development/core-module-file.html)
- **Data/render split:** `node_helper.js` is a Node.js script for back‑end tasks; **only one node helper instance exists per module type**, even with two calendar modules. The front end talks to it via `sendSocketNotification`/`socketNotificationReceived`, and when the helper sends a notification **every module instance of that type receives it**. The socket opens on the module's first `sendSocketNotification`. CONFIRMED — [MM node helper](https://docs.magicmirror.builders/module-development/node-helper.html), [MM core module file](https://docs.magicmirror.builders/module-development/core-module-file.html)
- `requiresVersion: "2.1.0"`: the core refuses to run the module on an older MagicMirror; it should also be set in the node helper. CONFIRMED — [MM core module file](https://docs.magicmirror.builders/module-development/core-module-file.html)

**Home Assistant dashboard cards (JavaScript custom elements, any framework)**
- A custom card is a custom element that "gets its configuration and the state of Home Assistant, and renders whatever you want". CONFIRMED — [HA custom card](https://contributors.home-assistant.io/frontend/custom-ui/card/) (the old developers.home-assistant.io URL redirects here)
- Contract: `setConfig(config)` (throw and HA shows the error instead of the card); `set hass(hass)` called "with a new hass object on every change in the home"; `getCardSize()` (height in the masonry view "in units of 50 pixels"); `getGridOptions()` (size in the sections grid); the example also uses `connectedCallback`/`disconnectedCallback`. CONFIRMED — [HA custom card](https://contributors.home-assistant.io/frontend/custom-ui/card/)
- Push model: the host pushes state, and the card does not poll. Because `hass` changes on every state change, a card can fire a `context-request` event to subscribe to only part of the data (contexts such as `hassApi`, `hassInternationalization`). CONFIRMED — [HA custom card](https://contributors.home-assistant.io/frontend/custom-ui/card/)
- Formatting helpers on `hass` (`formatEntityState()`, `formatEntityName()`) localise states and units; `window.loadCardHelpers()` lets a card create built‑in cards inside itself; `ha-card` gives the standard background/border, but `ha-*` elements are "not a promised API". CONFIRMED — [HA custom card](https://contributors.home-assistant.io/frontend/custom-ui/card/)

**Homarr v1/v2 (TypeScript, Next.js, React, tRPC)**
- Each widget kind calls `createWidgetDefinition(kind, {...})` and exports `{ definition, componentLoader }` (lazy‑loaded React component). CONFIRMED — [Homarr clock/index.ts](https://github.com/homarr-labs/homarr/blob/HEAD/packages/widgets/src/clock/index.ts)
- `WidgetDefinition` fields: `icon`, `supportsAdvancedFocus`, `supportsInlineEditing`, `deferUntilVisible` ("start the browser runtime on first visibility, then retain its state offscreen"), `queryKey`/`queryKeys`/`queryMatcher`, `refetchInterval`, `supportedIntegrations`, `integrationsRequired`, `maxIntegrations`, `createOptions(settings)`, per‑error‑code `errors` (icon + message), `contextActions`, `activitySignal`. Runtime actions include `refresh`, `getDataStatus` (`updatedAt`, `hasError`, `isRefreshing`), `togglePolling`. CONFIRMED — [Homarr definition.ts](https://github.com/homarr-labs/homarr/blob/HEAD/packages/widgets/src/definition.ts)
- **Data/render split:** data comes from server‑side tRPC queries keyed by `queryKey`; the server starts supported widgets' initial queries together after resolving the board layout, results stream in independently, queries for collapsed sections wait until opened, and "live refresh intervals still apply". CONFIRMED — [Homarr boards docs](https://homarr.dev/docs/management/boards/), [Homarr definition.ts](https://github.com/homarr-labs/homarr/blob/HEAD/packages/widgets/src/definition.ts)
- Integrations (external services such as Pi‑hole or media servers) are a separate concept that a widget binds to via `supportedIntegrations`. CONFIRMED — [Homarr definition.ts](https://github.com/homarr-labs/homarr/blob/HEAD/packages/widgets/src/definition.ts)

**Homepage (gethomepage, JavaScript, Next.js)**
- A service widget is a definition file `src/widgets/<widget>/widget.js` plus a React `component.jsx`, optionally a custom `proxy.js`; tests are expected for each (`component.test.jsx`, `widget.test.js`, `proxy.test.js`). CONFIRMED — [Homepage widget authoring](https://gethomepage.dev/widgets/authoring/getting-started/)
- The definition is declarative, for example AdGuard: `{ api: "{url}/control/{endpoint}", proxyHandler: genericProxyHandler, mappings: { stats: { endpoint: "stats" } } }`. The server proxies the API call, so credentials stay server‑side. CONFIRMED (shape) — [Homepage adguard/widget.js](https://github.com/gethomepage/homepage/blob/HEAD/src/widgets/adguard/widget.js); "credentials stay server‑side" is an inference from the proxy design.
- Guidelines: minimise the number of API calls, avoid a custom proxy unless necessary, and keep widgets read‑only. CONFIRMED — [Homepage widget authoring](https://gethomepage.dev/widgets/authoring/getting-started/)

**Dashy (Vue 2)**
- Widgets are Vue components (about 96 files in `src/components/Widgets`, by my count from the directory listing) configured as `- type: clock` with an `options:` map; each widget has a Refresh Data button and a full‑page view with its own URL. CONFIRMED — [Dashy widgets](https://dashy.to/docs/widgets), [Dashy Widgets dir](https://github.com/Lissy93/dashy/tree/HEAD/src/components/Widgets)
- Generic fallback widgets (iframe, HTML embed, data feed, API response) cover simple data without writing a component. CONFIRMED — [Dashy widgets](https://dashy.to/docs/widgets)
- Per‑widget options such as `updateInterval`, `useProxy` and `timeout` are UNVERIFIED (my grep of the page found only `ignoreErrors`).

**Glance (Go, server‑rendered HTML)**
- Built‑in widgets are Go code with a per‑widget `cache` duration (`30s`, `5m`, `2h`, `1d`). CONFIRMED — [Glance configuration.md](https://github.com/glanceapp/glance/blob/main/docs/configuration.md)
- **Extension widget protocol:** "nothing more than an HTTP request to a server that returns a few special headers": `Widget-Title`, `Widget-Title-URL`, `Widget-Content-Type` (only `html` today), `Widget-Content-Frameless`. HTML renders only if the user sets `allow-potentially-dangerous-html: true`, otherwise as plain text. Default cache is 30 minutes. Extensions can reuse Glance CSS classes (`color-subdue`, `size-h1`, `data-dynamic-relative-time`, and others). The doc warns that the API "may change". The stated long‑term goal is **generic JSON content types** (videos, forum posts, markets) rendered by Glance's own styles. CONFIRMED — [Glance extensions.md](https://github.com/glanceapp/glance/blob/main/docs/extensions.md)
- A `custom-api` widget exists, listed in the configuration TOC beside Extension. CONFIRMED that it exists; its template mechanics are UNVERIFIED in this pass — [Glance configuration.md](https://github.com/glanceapp/glance/blob/main/docs/configuration.md)

**Grafana (TypeScript/React front end; Go back end for data sources)**
- `src/module.ts` exports a class extending `GrafanaPlugin`: `PanelPlugin`, `DataSourcePlugin` or `AppPlugin`. `PanelProps` exposes runtime info such as panel dimensions and the current time range. CONFIRMED — [Grafana build a panel plugin](https://grafana.com/developers/plugin-tools/tutorials/build-a-panel-plugin)
- **Data/render split:** panels render data frames supplied by data‑source plugins and do not fetch for themselves. The scaffold: `new PanelPlugin<SimpleOptions>(SimplePanel).setPanelOptions(builder => builder.addTextInput({path, name, description, defaultValue}).addBooleanSwitch(...).addRadio({..., showIf: config => config.showSeriesCount}))`. CONFIRMED (scaffold) — [plugin-tools panel template module.ts](https://github.com/grafana/plugin-tools/blob/HEAD/packages/create-plugin/templates/panel/src/module.ts). The panel/data‑source separation is the documented plugin‑type split — [Grafana build a panel plugin](https://grafana.com/developers/plugin-tools/tutorials/build-a-panel-plugin)

**openHAB MainUI personal widgets (YAML → Vue/Framework7)**
- Personal widgets are YAML: "YAML → Vue (Framework7) → HTML". Components carry a `config` section and nested `slots`; widget expressions bind config to item states. Created in Developer Tools > Widgets, with a YAML editor and live preview. CONFIRMED — [openHAB personal widgets](https://www.openhab.org/docs/ui/personal-widgets.html)
- Used on a page as `component: widget:<uid>` with `config:` holding the widget's props. CONFIRMED — [openHAB personal widgets](https://www.openhab.org/docs/ui/personal-widgets.html)

**Node‑RED Dashboard 2.0 (FlowFuse; Node.js + Vue 3)**
- A widget is (1) a Node‑RED node in the editor palette (`/nodes/ui-x.html` for properties, edit UI and help; `/nodes/ui-x.js` for server behaviour) and (2) client `.vue` code. Node‑RED sends a `ui-config` object listing pages, themes, groups and widgets; core types map to built‑in components, and third‑party widgets load a `.umd.js` from the widget's `/resources` folder. Each component receives `id`, `props` and `state`. Third‑party widgets exist since v0.8.0. CONFIRMED — [Node‑RED D2 third‑party widgets](https://dashboard.flowfuse.com/contributing/widgets/third-party.html)

**Smashing (Ruby/Sinatra + CoffeeScript), successor of Shopify Dashing**
- A widget is a trio `number.coffee`, `number.html`, `number.scss`. Data comes from **jobs** (`SCHEDULER.every '2s' do … send_event('karma', {current:, last:}) end`) pushed to widgets bound by `data-id`. CONFIRMED — [Smashing widget template](https://github.com/Smashing/smashing/tree/HEAD/templates/project/widgets/number), [Smashing sample job](https://github.com/Smashing/smashing/blob/HEAD/templates/project/jobs/sample.rb)
- The README says Smashing "looks especially great on TVs". CONFIRMED — [Smashing](https://github.com/Smashing/smashing)

**Heimdall / Organizr (brief)**
- Heimdall tiles are "Foundation apps" (icon + default colour) or "Enhanced apps", which call a service's API to show live stats (for example, NZBGet queue size and speed). CONFIRMED — [Heimdall README](https://github.com/linuxserver/Heimdall)
- Organizr is a tab/iframe aggregator ("Tabs that will be loaded all in one webpage", iFrame toggle per tab), not a widget framework. CONFIRMED — [Organizr README](https://github.com/causefx/Organizr)

**TRMNL (e‑ink dashboard; added as notable)**
- Private plugins are Liquid templates, with data from webhooks or async polling (an optional serverless transform persists `trmnl.state`). The Markup Editor has **one tab per layout size (Full, Half Horizontal, Half Vertical, Quadrant)** plus a Shared tab prepended to all. CONFIRMED — [TRMNL templates](https://docs.trmnl.com/go/private-plugins/templates)

### Inferences
- MagicMirror's "one helper per type, broadcast to all instances" model is a simple way to share a single fetch across several instances of one widget kind (two rain tiles for the same city). Homarr reaches the same goal with query‑key de‑duplication (`queryKey` + `queryMatcher` per instance options), which is finer‑grained: two instances share data only when their inputs match.
- HA's push model (`set hass`) and Smashing's `send_event` both make widgets passive renderers of a host‑owned data stream; Homepage, Homarr and Grafana go further and make the *host* own fetch, proxy and refresh, leaving the widget as config + view. For a native app that wants third‑party widgets without arbitrary code, the declarative "definition record + host‑side fetch" shape (Homepage `widget.js`, Glance's future JSON content types) is the closest fit.
- TRMNL's "one template per size slot" maps closely onto Homeboard's compact tile versus expanded full‑screen view: a widget declares a rendering per size class rather than one responsive view.

### Gaps
- MagicMirror `getStyles`, `getScripts`, `getTranslations` and `resume()` were not captured in this pass; they are widely documented but are left UNVERIFIED here.
- HA's `getConfigElement` (custom editor element) appears on the card‑editor page under "A visual editor of your own", but I did not capture the method text; see §3.
- Dashy's per‑widget refresh options were not confirmed.

## 2. Layout model: grids, regions, columns, sections, breakpoints, collisions

### Takeaway
Layouts fall into five families: **named regions** (MagicMirror), **column stacks** (Glance, Homepage groups, Dashy sections, HA masonry), **section grids with per‑card span hints** (HA sections: 12 columns, 56 px rows, `getGridOptions`), **free x/y/w/h grids** (Grafana 24 columns, Homarr, Smashing/gridster, openHAB fixed grid, Node‑RED Fixed) and **fixed size slots** (TRMNL full/half/quadrant). The most transferable item for a D‑pad editor is Homarr's documented **deterministic keyboard/numeric placement** (`placeGridItem`: pin the active item, push collisions out of the way, compact the rest vertically). Homarr has also moved off gridstack to its own fixed‑geometry core.

### Cited Findings

**MagicMirror² regions**
- `position` is one of `top_bar`, `top_left`, `top_center`, `top_right`, `upper_third`, `middle_center`, `lower_third`, `bottom_left`, `bottom_center`, `bottom_right`, `bottom_bar`, `fullscreen_above`, `fullscreen_below`. Multiple modules in one region stack in config‑file order. A module with no position still runs, invisibly. CONFIRMED — [MM module configuration](https://docs.magicmirror.builders/modules/configuration.html)
- Custom regions: add `class="region top3 left"` to `index.html` and the position becomes `top3_left`; the config checker validates it. "All these regions will resize as needed." CONFIRMED — [MM module configuration](https://docs.magicmirror.builders/modules/configuration.html)

**Home Assistant: masonry and sections views**
- Masonry places cards in columns using `getCardSize()` to balance them. CONFIRMED — [HA custom card](https://contributors.home-assistant.io/frontend/custom-ui/card/)
- Sections view: "A section is a grid of 12 columns. Rows are 56 pixels high, with 8 pixels between them." `getGridOptions()` returns `columns` (default 12, or `"full"`), `rows` (default `"auto"` = content height, not resizable), `min_columns`/`max_columns`, `min_rows`/`max_rows`. Without it the card is full width with content height. Docs advise multiples of 3 (3, 6, 9, 12); `rows: 2` = 120 px. CONFIRMED — [HA custom card](https://contributors.home-assistant.io/frontend/custom-ui/card/)
- View options: "Max number of sections wide", and **"Dense section placement"**, which auto‑fills gaps at the cost of order control ("only applies to horizontal gaps if you used sections more than one column wide"). CONFIRMED — [HA sections](https://www.home-assistant.io/dashboards/sections/)
- The per‑card YAML key that stores a user's chosen size (`grid_options`) is UNVERIFIED in this pass.

**Homarr**
- Current layout core (README in source): fixed logical units, where one track is a 212 × 212 logical‑pixel footprint, cards are inset 10 px "matching the legacy GridStack gutter", multi‑cell items occupy whole tracks, and the canvas is scaled uniformly to the available width. Persisted `xOffset/yOffset/width/height` convert to `x/y/w/h`; children render in `sortGridPlacementsForReadingOrder` order. "Edit mode can use any interaction library, but its final coordinates should be normalized through this core. **`placeGridItem` is the deterministic keyboard/numeric fallback: the active item is pinned while collisions move out of the way and all other items compact vertically.**" CONFIRMED — [Homarr layout README](https://github.com/homarr-labs/homarr/blob/HEAD/apps/nextjs/src/components/board/layout/README.md)
- **Responsive layouts:** one board holds several layouts with their own item positions. "Moving or resizing an item in one layout does not change its coordinates in the others." Mobile and Base are protected layouts; new boards use 3 Mobile columns; extra layouts need unique breakpoints; Homarr picks the highest breakpoint that fits. "Reset from Base" regenerates a layout from Base. Narrowing and then re‑widening restores the saved source arrangement until the user edits. Containers (nested grids, collapsible) and rails (1–3 fixed side columns) exist. Headers support Enter/Space. CONFIRMED — [Homarr boards docs](https://homarr.dev/docs/management/boards/)
- History: legacy Homarr 0.x (`ajnart/homarr`, archived) depended on `fily-publish-gridstack`. A GitHub code search of `homarr-labs/homarr` for "gridstack" returns only a README, a 2023 blog post and `.vscode/settings.json`, which suggests the dependency is gone. CONFIRMED (dependency in 0.16.0) — [ajnart/homarr package.json](https://github.com/ajnart/homarr/blob/HEAD/package.json); "gone in v1/v2" is an inference from the code search and the README's "legacy GridStack" wording — [Homarr layout README](https://github.com/homarr-labs/homarr/blob/HEAD/apps/nextjs/src/components/board/layout/README.md)

**Grafana**
- Classic model: `gridPos {x, y, w, h}`; `w` 1–24 ("the width of the dashboard is divided into 24 columns"), `h` in units of 30 px, `x` in the same units as `w`. Dashboards carry an integer `schemaVersion`, "incremented each time a Grafana update brings changes to said schema". CONFIRMED — [Grafana JSON model](https://grafana.com/docs/grafana/latest/dashboards/build-dashboards/view-dashboard-json-model/)
- Dynamic dashboards: panels group into **rows and tabs**, each with a **Custom** layout (manual position and size; default) or an **Auto grid** layout (panels auto‑sized into a uniform grid with a "Min column width" of Standard, Narrow, Wide or Custom; no manual changes; show/hide rules only in Auto grid). Trying to resize in Auto grid offers "Edit auto layout" or "Switch to custom layout". CONFIRMED — [Grafana dynamic dashboards](https://grafana.com/docs/grafana/latest/visualizations/dashboards/build-dashboards/create-dynamic-dashboard/)

**Glance / Homepage / Dashy (column stacks)**
- Glance: per page, `columns` of `size: small` (fixed 300 px) or `size: full` (remaining width); up to 3 columns, with 1 or 2 full columns. Some widgets offer `style` values tuned for full vs small columns (`vertical-list`, `horizontal-cards`, `vertical-cards`). CONFIRMED — [Glance configuration.md](https://github.com/glanceapp/glance/blob/main/docs/configuration.md)
- Homepage: service/bookmark groups laid out as `style: row` or column with `columns: N` in a `layout:` block; group order follows the layout block. CONFIRMED — [Homepage settings](https://gethomepage.dev/configs/settings/)
- Dashy: widget sections take section display options, for example `displayData.cols: 2` for double width. CONFIRMED — [Dashy widgets](https://dashy.to/docs/widgets)

**openHAB MainUI**
- Responsive layouts (masonry, `oh-grid-row`/`oh-grid-col`) are the recommended default; **Fixed Grid layouts** give predefined positions and sizes for "wall mounted tablets" and floor plans. CONFIRMED — [openHAB layout pages](https://www.openhab.org/docs/ui/layout-pages.html), [openHAB building pages](https://www.openhab.org/docs/ui/building-pages.html)
- The web UI depends on `grid-layout-plus` ^1.1.0 and `vue-magic-grid`. CONFIRMED — [openhab-webui package.json](https://github.com/openhab/openhab-webui/blob/HEAD/bundles/org.openhab.ui/web/package.json). That `grid-layout-plus` backs Fixed Grid layouts is UNVERIFIED (inferred from the name).

**Node‑RED Dashboard 2.0**
- Page layouts: Grid, Fixed, Notebook, Tabs. Group and widget width is a share of **12 columns** in Grid/Notebook, or a multiple of **90 px** in Fixed. Breakpoints per page set the column count at each screen size. CONFIRMED — [Node‑RED D2 layouts](https://dashboard.flowfuse.com/layouts/)

**Smashing**
- Dashboards are ERB pages using **gridster**: `<li data-row="1" data-col="1" data-sizex="2" data-sizey="1">`, with drag to rearrange ("Protip: You can drag the widgets around!"). CONFIRMED — [Smashing sample.erb](https://github.com/Smashing/smashing/blob/HEAD/templates/project/dashboards/sample.erb)

**TRMNL**
- Fixed slots: full, half vertical, half horizontal, quadrant ("mashups" combine plugins into one screen). CONFIRMED — [TRMNL templates](https://docs.trmnl.com/go/private-plugins/templates), [larapaper README](https://github.com/usetrmnl/larapaper)

**Grid libraries (for reference)**
- react‑grid‑layout (MIT, 22 496★, pushed 2026‑11‑19), gridstack.js (MIT, 9 258★, pushed 2026‑11‑17), Muuri (MIT, 10 937★, last push 2024‑05‑25, stale). CONFIRMED — GitHub API: [react-grid-layout](https://github.com/react-grid-layout/react-grid-layout), [gridstack.js](https://github.com/gridstack/gridstack.js), [Muuri](https://github.com/haltu/muuri)

### Inferences
- HA sections and Node‑RED both use a 12‑column grid where a widget declares width in columns; Grafana uses 24. Homeboard's Auto/4–12 columns sits in the same family, and HA's "multiples of 3" advice shows how to keep mixed sizes aligned (3/6/9/12 ≈ Small/Medium/Large/full).
- Homarr's `placeGridItem` (pinned active item, pushed collisions, vertical compaction) is the only documented collision algorithm designed for *keyboard* input, the D‑pad case. Its layout core also sorts children into reading order for rendering, which a D‑pad focus order needs too.
- Homarr's per‑breakpoint layouts with "Reset from Base" correspond to Homeboard's Auto columns/rows on TVs of different resolutions: one Base layout, with derived layouts that stay generated until the user edits them.
- Grafana's Custom versus Auto grid choice per group is a clean model for "user places tiles" versus "host packs tiles" (Homeboard's app favourites packing around panels).

### Gaps
- I did not read the HA frontend source for its sections grid (whether it is CSS grid or a library) or its reflow rules beyond "dense placement".
- I did not read Homarr's `placeGridItem` implementation itself, only the README description of it.
- The library behind Homarr v1/v2 edit‑mode drag is not identified.

## 3. Configuration: files vs UI editors, schema‑driven forms, per‑instance settings, validation, sharing

### Takeaway
The strongest pattern is **one declarative schema per widget kind that the host renders as a form**: HA `getConfigForm()` with selectors, Grafana `setPanelOptions` builders, Homarr `createOptions` via `optionsBuilder`, and MagicMirror's community `config.schema.json`. File‑first systems (MagicMirror `config.js`, Glance/Homepage YAML) have no in‑app editor or rely on add‑ons. HA keeps both YAML and UI storage modes per dashboard; Dashy offers YAML, a validated JSON editor and a visual editor.

### Cited Findings
- **MagicMirror:** modules are configured as entries in the main config file (`config.js`) with `module`, `position`, `header`, `classes`, `config`, and similar keys. CONFIRMED — [MM module configuration](https://docs.magicmirror.builders/modules/configuration.html). MagicMirror itself has no UI editor; MMM‑Config and MMM‑Remote‑Control add one (§4). CONFIRMED — [MMM‑Config](https://github.com/sdetweil/MMM-Config)
- **HA card editor and picker:** a card registers in the picker via `window.customCards.push({type, name, description, preview, documentationURL})`, and `preview: true` shows a live preview. `static getStubConfig(hass, entities, entitiesFallback)` returns the starting config for a newly added card. `getEntitySuggestion` lets the card be suggested for suitable entities. CONFIRMED — [HA card editor](https://contributors.home-assistant.io/frontend/custom-ui/card-editor/)
- **HA schema form:** `static getConfigForm()` returns `{ schema: [...], computeLabel, computeHelper, assertConfig }`. Fields carry `name`, a `selector` (the same selectors used by blueprints and the automation editor: `entity`, `number`, `select`, `icon`, `theme`, …), `required`, `default`, `context` (link to another field), `visible` (conditional), and can be grouped (`type: "grid"`). `assertConfig` throws to disable the visual editor when the YAML cannot be represented, and the visual editor returns once the error clears. CONFIRMED — [HA card editor](https://contributors.home-assistant.io/frontend/custom-ui/card-editor/)
- **HA custom editor element:** for cards that need more than a form there is "A visual editor of your own" (the long‑standing `static getConfigElement()` returning an editor element that fires `config-changed`). Section heading CONFIRMED; method details UNVERIFIED in this pass — [HA card editor](https://contributors.home-assistant.io/frontend/custom-ui/card-editor/)
- **HA storage:** dashboards are in `mode: storage` (UI‑managed) or `mode: yaml` (file, needs a hyphenated key and a `filename`). `resource_mode: yaml|storage` controls whether card JS resources are listed in YAML or managed in the UI, and resources can be reloaded without a restart. CONFIRMED — [HA dashboards](https://www.home-assistant.io/dashboards/dashboards/)
- **Homarr:** each widget's options come from `createOptions()` built with `optionsBuilder` factories (`factory.switch({defaultValue, withDescription})`, `factory.text(...)`, selects from preset lists); options persist per item in the board database (SQLite or PostgreSQL schemas). CONFIRMED — [Homarr clock/index.ts](https://github.com/homarr-labs/homarr/blob/HEAD/packages/widgets/src/clock/index.ts), [Homarr db schema dir](https://github.com/homarr-labs/homarr/tree/HEAD/packages/db/schema). Board import/export format: UNVERIFIED (not found on the boards page).
- **Grafana:** panel options come from the builder (`addTextInput`, `addBooleanSwitch`, `addRadio`, `addSelect`, `addNumberInput`, `addFieldNamePicker`, with `showIf` for conditional fields). Standard field options are enabled through `useFieldConfig`. CONFIRMED — [panel template module.ts](https://github.com/grafana/plugin-tools/blob/HEAD/packages/create-plugin/templates/panel/src/module.ts), [panel options doc template](https://github.com/grafana/plugin-tools/blob/HEAD/packages/create-plugin/templates/docs/panel/docs/options.md). The whole dashboard is one JSON document with `schemaVersion`, so dashboards export and import as JSON. CONFIRMED — [Grafana JSON model](https://grafana.com/docs/grafana/latest/dashboards/build-dashboards/view-dashboard-json-model/)
- **Dashy:** one `/user-data/conf.yml`, editable three ways: the YAML file directly, a UI JSON editor "with built-in validation", or a UI visual (interactive) editor, which arrived in v1.8.9. The UI can "export, backup, reset, validate and download" the config, and `yarn validate-config` checks it against the schema. The widgets page says "Widgets cannot currently be edited through the UI". CONFIRMED — [Dashy configuring](https://dashy.to/docs/configuring), [Dashy widgets](https://dashy.to/docs/widgets)
- **Glance, Homepage:** YAML files only (Glance `glance.yml` pages/columns/widgets; Homepage `services.yaml`, `settings.yaml` layout). CONFIRMED — [Glance configuration.md](https://github.com/glanceapp/glance/blob/main/docs/configuration.md), [Homepage settings](https://gethomepage.dev/configs/settings/). The Homepage `widgets.yaml` (info widgets) file name is UNVERIFIED in this pass.
- **openHAB:** personal widgets are authored in a YAML editor with live preview and "Set props" (Ctrl‑P), then configured per placement through `config:` props. CONFIRMED — [openHAB personal widgets](https://www.openhab.org/docs/ui/personal-widgets.html)
- **Node‑RED D2:** each widget node's properties are edited in the Node‑RED editor (`ui-x.html` defines the edit UI); the dashboard receives the result as `ui-config`. CONFIRMED — [Node‑RED D2 third‑party widgets](https://dashboard.flowfuse.com/contributing/widgets/third-party.html)
- **TRMNL:** a plugin's form fields (`custom_fields_values`) are exposed to its Liquid template. CONFIRMED — [TRMNL templates](https://docs.trmnl.com/go/private-plugins/templates)

### Inferences
- HA's split of **catalog metadata** (`customCards` entry), **default instance** (`getStubConfig`), **form schema** (`getConfigForm` + `assertConfig`) and **sizing hints** (`getGridOptions`) is a complete, small contract that maps one‑to‑one onto what Homeboard's "add widget" flow, settings UI and Resize presets need, whether widgets are compiled in or third‑party.
- A schema that the host renders lets the same definition drive a D‑pad form on the TV *and* the phone web editor; this is the main reason to prefer HA/Grafana/Homarr‑style declarative options over per‑widget hand‑built settings screens.
- HA's `assertConfig` "fall back to raw editing when the form cannot represent the config" is a useful escape hatch for Homeboard's phone editor (raw JSON per instance when a form cannot express it).

### Gaps
- HA's exact on‑disk location and format of storage‑mode dashboards was not verified.
- Homarr's export/import and Homepage `widgets.yaml` details are unconfirmed.

## 4. Phone‑as‑remote‑editor flows (QR pairing analogues)

### Takeaway
The closest analogues to Homeboard's "TV shows a QR code, the phone edits" flow are two **MagicMirror community modules**. MMM‑Remote‑Control shows a QR code and serves a mobile web UI and REST API. MMM‑Config shows a QR code and serves a schema‑generated form that rewrites `config.js` after backing it up. Neither uses a draft‑then‑Apply transaction like Homeboard's; MMM‑Config's "rename old config, write new" is the nearest thing to Undo.

### Cited Findings
- **MMM‑Remote‑Control** (MIT, 604★, pushed 2026‑11‑12, no GitHub releases): a browser UI that "should work fine on any device", PWA support, and a **"QR Code Display: Show a QR code on your mirror for instant mobile connection"** (options `showQRCode`, `qrCodeSize`, `qrCodePosition`, `hostname`). It can hide and show modules, has an edit‑view menu, and offers a RESTful API with optional `apiKey`/`secureEndpoints`. The mirror must be opened to the LAN via `address` and `ipWhitelist`. CONFIRMED — [MMM‑Remote‑Control README](https://github.com/Jopyth/MMM-Remote-Control)
- MMM‑Remote‑Control's config editor reads an optional **`config.schema.json`** beside a module (`properties`, `items`, `type`, `enum`, `default`, `description`) to suggest missing settings and render typed controls. Module `defaults` take precedence over schema defaults, and schemas are "editor hints, not full JSON Schema validation". CONFIRMED — [MMM‑Remote‑Control README](https://github.com/Jopyth/MMM-Remote-Control)
- **MMM‑Config** (MIT, 57★, pushed 2026‑09‑25): "a dynamically built form, based on modules installed … and the contents of the config.js", served at the path `/configure` on the MagicMirror host and port, with a module installer at `/installer`. `showQR` shows a QR code on screen; if MM listens on localhost the QR is replaced by an explanation. **"On form submission, a new config.js is constructed and saved, AFTER renaming the current config.js out of the way."** The form regenerates only when `config.js` or the modules folder changed (`force_update` overrides). CONFIRMED — [MMM‑Config README](https://github.com/sdetweil/MMM-Config)
- Home Assistant, Grafana, Homarr, Dashy and openHAB are edited from any browser on the network with user accounts; none shows a QR pairing code on the display as a core feature, as far as these docs show. CONFIRMED that their editors are browser UIs (pages cited in §3); "no QR pairing" is an absence finding, not a confirmed statement.

### Inferences
- MagicMirror's ecosystem has converged independently on "display shows a QR code, phone opens a LAN web editor generated from per‑module schemas", which validates Homeboard's design direction. Homeboard's transactional draft/Apply/Undo is stronger than both modules (MMM‑Config keeps one backup file; MMM‑Remote‑Control applies changes live).
- MMM‑Remote‑Control's need for an `ipWhitelist` shows the security cost of a permanent LAN editor; Homeboard's *temporary* server avoids it.

### Gaps
- I found no other open‑source web dashboard with a built‑in QR pairing editor in this pass; DAKboard and similar commercial products were out of scope (covered by the earlier investigation).

## 5. Plugin distribution: registries, install mechanism, review, signing, sandboxing, versioning

### Takeaway
Distribution ranges from **none, PR into core** (Homepage, Homarr, Dashy, Glance built‑ins), through **wiki list + git clone** (MagicMirror, 1 527 catalogued modules) and **curated GitHub‑repo index with automated checks** (HACS, now built into HA 2026.11 as "Marketplace": 1 182 dashboard plugins), to **reviewed and cryptographically signed** (Grafana: MANIFEST.txt, Community/Commercial/Private levels). None of the web dashboards sandboxes third‑party front‑end code beyond what the browser gives; Glance's extension is the only out‑of‑process model, and its HTML needs an explicit "dangerous HTML" opt‑in.

### Cited Findings

**MagicMirror²**
- Third‑party modules are listed on a wiki page; a daily job turns it into the filterable site modules.magicmirror.builders and runs "rudimentary" automated checks (missing licence, `node_modules` committed, deprecated deps, naming not starting `MMM-`, outdated CI actions), with results in `result.html`. CONFIRMED — [MagicMirror‑3rd‑Party‑Modules README](https://github.com/MagicMirrorOrg/MagicMirror-3rd-Party-Modules)
- The catalogue JSON lists **1 527 modules**. Licences: MIT 1 245, none 131, ISC 56, GPL‑3.0 39, Apache‑2.0 18, NOASSERTION 14, AGPL‑3.0 10. Last commit by year: 2026: 499, 2025: 202, 2024: 156, 2023 and older: 670. So about 46 % show no commit since 2023. CONFIRMED (my aggregation of the feed) — [modules.json](https://modules.magicmirror.builders/data/modules.json)
- Install is manual: `cd ~/MagicMirror/modules && git clone … && npm install`, then add an entry to `config.js`. CONFIRMED — [MMM‑Remote‑Control README](https://github.com/Jopyth/MMM-Remote-Control), [MMM‑Config README](https://github.com/sdetweil/MMM-Config). MMM‑Config adds a web installer page. CONFIRMED — [MMM‑Config README](https://github.com/sdetweil/MMM-Config)
- Compatibility gate: `requiresVersion`. No signing or sandboxing; `node_helper` runs as Node.js with the user's privileges. CONFIRMED for `requiresVersion` — [MM core module file](https://docs.magicmirror.builders/module-development/core-module-file.html); "no sandbox" is an inference from the node_helper description — [MM node helper](https://docs.magicmirror.builders/module-development/node-helper.html)

**Home Assistant + HACS / Marketplace**
- **"Starting with Home Assistant 2026.11, HACS ships with Home Assistant itself, as the Marketplace integration"** under Settings > Marketplace: same catalog, same repositories, same `hacs.json`. The HACS custom integration gets no new features; existing downloads carry over. "Getting into the catalog still works the same, through hacs/default." CONFIRMED — [HACS Marketplace page](https://hacs.xyz/docs/marketplace/)
- Inclusion: only the owner or a major contributor may submit a PR to `hacs/default`. The repo must be public on GitHub, pass the HACS Action (and Hassfest for integrations) without ignores, and have a full GitHub release. Automated checks: brands, core‑domain conflict, manifest, hacs‑validation, HACS manifest (`name`), archived, releases, owner, **OSI‑approved licence GitHub can identify**, images, repository. Minor issues get the PR drafted and major ones get it closed. "New additions still take months to be reviewed." CONFIRMED — [HACS include](https://hacs.xyz/docs/publish/include/)
- Dashboard ("plugin") repos need `.js` files in `dist/` or the repo root, one named after the repository; `boilerplate-card` is the reference template. CONFIRMED — [HACS plugin](https://hacs.xyz/docs/publish/plugin/)
- Catalogue size from `hacs/default`: **1 182 dashboard (plugin) repos, 5 410 integrations**. CONFIRMED (counted from the repo's `plugin` and `integration` files via the GitHub API) — [hacs/default](https://github.com/hacs/default)
- Cards load as dashboard resources (a JS module URL); there is no sandbox, and a card runs in the HA frontend page. CONFIRMED that cards are registered resources — [HA custom card](https://contributors.home-assistant.io/frontend/custom-ui/card/); "no sandbox" is an inference.

**Grafana**
- Public plugins are reviewed, then granted a signature level (**Community** or **Commercial**) and listed in the catalog; **Private** signatures are for in‑organisation use. Signing uses an Access Policy token and `sign-plugin`, which writes `MANIFEST.txt` into `dist/`. CONFIRMED — [Grafana sign a plugin](https://grafana.com/developers/plugin-tools/publish-a-plugin/sign-a-plugin)
- Public catalog API: 391 plugins (212 data source, 138 panel, 41 app); signature types community 182, grafana 121, commercial 88. CONFIRMED as what [grafana.com/api/plugins](https://grafana.com/api/plugins) returned on 2026‑11‑19; this endpoint may omit some listings.

**Homepage**
- No plugin system: widgets are added by PR. Service widgets must "target a feature request discussion with at least 20 'up-votes'", and maintainers "reserve the right to decline widgets for projects that are very young (eg < ~1y) or those with a small reach". CONFIRMED — [Homepage widget authoring](https://gethomepage.dev/widgets/authoring/getting-started/). 168 widget directories exist in `src/widgets`, registered in `widgets.js`/`components.js`. CONFIRMED (GitHub API listing) — [Homepage src/widgets](https://github.com/gethomepage/homepage/tree/HEAD/src/widgets)

**Homarr, Dashy, Glance**
- Homarr widgets are compiled in through a central `registry.ts`/`manifest.ts` keyed by the declared `widgetKinds`; I count about 58 widget‑kind directories. CONFIRMED (structure) — [Homarr widgets src](https://github.com/homarr-labs/homarr/tree/HEAD/packages/widgets/src), [Homarr manifest.ts](https://github.com/homarr-labs/homarr/blob/HEAD/packages/widgets/src/manifest.ts). "No third‑party widget loading" is an inference from that compile‑time registry.
- Dashy: build‑your‑own via a tutorial and PR; widget requests are welcome with no promise they get built. CONFIRMED — [Dashy widgets](https://dashy.to/docs/widgets)
- Glance: extensions are self‑hosted HTTP services with no registry. A separate `glanceapp/community-widgets` repo (CC0‑1.0, 918★) collects shared configs. CONFIRMED — [Glance extensions.md](https://github.com/glanceapp/glance/blob/main/docs/extensions.md), GitHub API for [community-widgets](https://github.com/glanceapp/community-widgets)

**openHAB, Node‑RED, Heimdall, TRMNL**
- openHAB personal widgets are shared by copy‑pasting YAML "from examples by the community". CONFIRMED — [openHAB personal widgets](https://www.openhab.org/docs/ui/personal-widgets.html). A one‑click community marketplace for UI widgets via the add‑on store is UNVERIFIED in this pass.
- Node‑RED D2 third‑party widgets are standalone packages ("as was very popular in Node-RED Dashboard"), built from an example node repository. CONFIRMED — [Node‑RED D2 third‑party widgets](https://dashboard.flowfuse.com/contributing/widgets/third-party.html). Install through the Node‑RED palette manager (npm) is UNVERIFIED in this pass.
- Heimdall app definitions live in `linuxserver/Heimdall-Apps` (MIT, 695 app directories, pushed 2026‑11‑01). CONFIRMED — GitHub API, [Heimdall-Apps](https://github.com/linuxserver/Heimdall-Apps)
- TRMNL recipes: "170+ from the OSS community catalog, 1000+ from the TRMNL catalog", importable into the MIT self‑hosted server LaraPaper. CONFIRMED — [larapaper README](https://github.com/usetrmnl/larapaper)

### Inferences
- HACS is the most relevant governance model for a small project: an index of independently owned repos, admission by PR, automated checks (release exists, OSI licence, not archived, owner submits) and slow human review. Grafana adds signing, which matters if Homeboard ever loads code; for declarative widgets (schema + remote JSON) a HACS‑style index without signing may suffice.
- MagicMirror's catalogue shows the long‑tail cost of an open ecosystem: almost half of the modules have no commit since 2023. A registry with an "archived/stale" signal (MagicMirror's site shows last commit; HACS rejects archived repos) is worth copying.
- Homepage's "≥ 20 up‑votes, decline young/small projects" rule is a ready‑made curation policy for a compiled‑in widget set.

### Gaps
- No source shows any of these web dashboards sandboxing third‑party front‑end code; I found no documentation of a sandbox (iframe or CSP) for HA cards or MagicMirror modules.
- The Grafana catalog total may be undercounted by the public API.

## 6. Activity and licence

### Takeaway
All the major systems are active as of 2026‑11 except Smashing (dormant since 2023) and the archived Dashing and Homarr 0.x. **MIT‑compatible code to reuse:** MagicMirror, Dashy, Heimdall, Smashing, HACS, MMM‑Remote‑Control, MMM‑Config, and the grid libraries (MIT); HA frontend, Homarr v1/v2, Grafana plugin‑tools/SDK, Node‑RED D2, Homer (Apache‑2.0, compatible with attribution and NOTICE). **Do not copy code from:** Homepage and Organizr (GPL‑3.0), Glance and Grafana core (AGPL‑3.0). openHAB web UI is EPL‑2.0 (file‑level copyleft), so do not relicense its files as MIT.

### Cited Findings
All rows CONFIRMED via the GitHub REST API on 2026‑11‑19 (each linked repo), with releases via `gh api repos/X/releases/latest`.

| Project | Licence | Stars | Last push | Latest release | Notes |
|---|---|---|---|---|---|
| [MagicMirrorOrg/MagicMirror](https://github.com/MagicMirrorOrg/MagicMirror) | MIT | 24 230 | 2026‑11‑18 | v2.38.0, 2026‑10‑01 | 1 527 catalogued 3rd‑party modules |
| [home-assistant/frontend](https://github.com/home-assistant/frontend) | Apache‑2.0 | 5 811 | 2026‑11‑19 | 20261028.6, 2026‑11‑13 | Lovelace / cards |
| [hacs/integration](https://github.com/hacs/integration) | MIT | 7 893 | 2026‑11‑19 | 2.0.5, 2025‑01‑28 | folded into HA 2026.11 as Marketplace |
| [hacs/default](https://github.com/hacs/default) | MIT | 664 | 2026‑11‑17 | — | 1 182 plugins, 5 410 integrations |
| [piitaya/lovelace-mushroom](https://github.com/piitaya/lovelace-mushroom) | Apache‑2.0 | 5 275 | 2026‑11‑12 | — | example popular custom card |
| [homarr-labs/homarr](https://github.com/homarr-labs/homarr) | Apache‑2.0 | 5 404 | 2026‑11‑19 | v2.13.1, 2026‑11‑14 | current Homarr (created 2023‑12) |
| [ajnart/homarr](https://github.com/ajnart/homarr) | MIT | 7 190 | 2026‑06‑01 | — | **archived**; 0.x, gridstack |
| [gethomepage/homepage](https://github.com/gethomepage/homepage) | **GPL‑3.0** | 33 734 | 2026‑11‑19 | v2.8.0, 2026‑11‑11 | 168 widgets |
| [Lissy93/dashy](https://github.com/Lissy93/dashy) | MIT | 27 026 | 2026‑11‑16 | 4.9.0, 2026‑10‑23 | |
| [glanceapp/glance](https://github.com/glanceapp/glance) | **AGPL‑3.0** | 38 306 | 2026‑11‑07 | v0.8.8, 2026‑11‑07 | |
| [glanceapp/community-widgets](https://github.com/glanceapp/community-widgets) | CC0‑1.0 | 918 | 2026‑11‑05 | — | |
| [grafana/grafana](https://github.com/grafana/grafana) | **AGPL‑3.0** | 77 988 | 2026‑11‑19 | v13.3.1, 2026‑11‑10 | |
| [grafana/plugin-tools](https://github.com/grafana/plugin-tools) | Apache‑2.0 | 88 | 2026‑11‑19 | — | scaffolds, signing |
| [grafana/grafana-plugin-sdk-go](https://github.com/grafana/grafana-plugin-sdk-go) | Apache‑2.0 | 254 | 2026‑11‑19 | — | |
| [openhab/openhab-webui](https://github.com/openhab/openhab-webui) | EPL‑2.0 | 258 | 2026‑11‑18 | none on GitHub | MainUI + HABPanel |
| [FlowFuse/node-red-dashboard](https://github.com/FlowFuse/node-red-dashboard) | Apache‑2.0 | 363 | 2026‑11‑18 | v1.37.0, 2026‑11‑16 | Dashboard 2.0 |
| [node-red/node-red-dashboard](https://github.com/node-red/node-red-dashboard) | NOASSERTION (per API) | 1 407 | 2025‑08‑07 | — | Dashboard 1.x, quiet for over a year |
| [Smashing/smashing](https://github.com/Smashing/smashing) | MIT | 3 299 | 2023‑03‑10 | v1.3.6, 2022‑12‑08 | **dormant** |
| [Shopify/dashing](https://github.com/Shopify/dashing) | MIT | 10 894 | 2019‑04‑29 | — | **archived** |
| [linuxserver/Heimdall](https://github.com/linuxserver/Heimdall) | MIT | 9 418 | 2026‑10‑29 | v2.8.4, 2026‑10‑29 | 695 app defs in Heimdall‑Apps |
| [causefx/Organizr](https://github.com/causefx/Organizr) | **GPL‑3.0** | 5 830 | 2026‑05‑19 | 1.90, 2019‑02‑26 (last tagged release) | releases stale, commits continue |
| [homer-dashboard/homer](https://github.com/homer-dashboard/homer) | Apache‑2.0 | 11 731 | 2026‑11‑19 | — | static YAML dashboard (not profiled) |
| [pawelmalak/flame](https://github.com/pawelmalak/flame) | MIT | 6 677 | 2026‑06‑25 | — | not profiled |
| [Jopyth/MMM-Remote-Control](https://github.com/Jopyth/MMM-Remote-Control) | MIT | 604 | 2026‑11‑12 | none on GitHub | QR + phone UI |
| [sdetweil/MMM-Config](https://github.com/sdetweil/MMM-Config) | MIT | 57 | 2026‑09‑25 | — | QR + schema form |
| [usetrmnl/larapaper](https://github.com/usetrmnl/larapaper) | MIT | 459 | 2026‑11‑19 | — | TRMNL self‑hosted server (formerly byos_laravel) |
| [react-grid-layout](https://github.com/react-grid-layout/react-grid-layout) | MIT | 22 496 | 2026‑11‑19 | — | library |
| [gridstack.js](https://github.com/gridstack/gridstack.js) | MIT | 9 258 | 2026‑11‑17 | — | library |
| [haltu/muuri](https://github.com/haltu/muuri) | MIT | 10 937 | 2024‑05‑25 | — | library, stale |

- Licence obligations (general, not legal advice): Apache‑2.0 code can go into an MIT‑licensed app if its licence text and NOTICE are kept and modified files are marked; GPL‑3.0/AGPL‑3.0 code would force the combined work under (A)GPL; EPL‑2.0 is weak copyleft at file level. UNVERIFIED as legal analysis (summary of well‑known licence terms; no source fetched in this pass).

### Inferences
- Of the two best design references for layout, HA frontend (Apache‑2.0) and Homarr (Apache‑2.0), code or algorithm ports (for example Homarr's `placeGridItem`) are licence‑compatible with attribution. Grafana's *schema ideas* (gridPos, schemaVersion) are free to copy as ideas, but its core code is AGPL.
- Homepage's and Glance's designs can be studied and re‑implemented, but their code cannot be copied into Homeboard.

### Gaps
- Homarr v1.0's original release date was not looked up.
- `node-red/node-red-dashboard` shows NOASSERTION in the API; its actual licence file was not inspected.

## 7. What transfers to a native Android TV dashboard driven by a D‑pad

### Takeaway
The transferable pieces are data shapes and contracts, not code. HA's per‑kind sizing hints and schema form, Homarr's keyboard placement algorithm and per‑breakpoint layouts, Grafana's `gridPos` + `schemaVersion` and Custom/Auto split, MagicMirror's per‑type shared data helper and suspend‑on‑hide, TRMNL's per‑size templates, and Glance's "remote endpoint returns content plus headers" protocol (ideally in its future JSON form) all map onto existing Homeboard concepts.

### Cited Findings
(Each item names its source mechanism, already cited above; the mapping to Homeboard is inference and appears under Inferences.)
- HA `getGridOptions()` → `columns`, `rows`, `min_*`, `max_*`, "auto" height. CONFIRMED — [HA custom card](https://contributors.home-assistant.io/frontend/custom-ui/card/)
- HA `getStubConfig()`, `window.customCards` metadata, `getConfigForm()` schema + `assertConfig`. CONFIRMED — [HA card editor](https://contributors.home-assistant.io/frontend/custom-ui/card-editor/)
- Homarr `placeGridItem` keyboard/numeric placement, reading‑order sort, per‑breakpoint layouts with Reset from Base, `deferUntilVisible`, `refetchInterval`, query de‑duplication. CONFIRMED — [Homarr layout README](https://github.com/homarr-labs/homarr/blob/HEAD/apps/nextjs/src/components/board/layout/README.md), [Homarr boards docs](https://homarr.dev/docs/management/boards/), [Homarr definition.ts](https://github.com/homarr-labs/homarr/blob/HEAD/packages/widgets/src/definition.ts)
- Grafana `gridPos {x,y,w,h}`, integer `schemaVersion`, Custom vs Auto grid per row/tab. CONFIRMED — [Grafana JSON model](https://grafana.com/docs/grafana/latest/dashboards/build-dashboards/view-dashboard-json-model/), [Grafana dynamic dashboards](https://grafana.com/docs/grafana/latest/visualizations/dashboards/build-dashboards/create-dynamic-dashboard/)
- MagicMirror named regions, one `node_helper` per module type, `suspend()` on hide, `requiresVersion`. CONFIRMED — [MM module configuration](https://docs.magicmirror.builders/modules/configuration.html), [MM node helper](https://docs.magicmirror.builders/module-development/node-helper.html), [MM core module file](https://docs.magicmirror.builders/module-development/core-module-file.html)
- Glance extension headers and planned JSON content types. CONFIRMED — [Glance extensions.md](https://github.com/glanceapp/glance/blob/main/docs/extensions.md)
- TRMNL per‑size Markup tabs (Full, Half H, Half V, Quadrant). CONFIRMED — [TRMNL templates](https://docs.trmnl.com/go/private-plugins/templates)
- MMM‑Remote‑Control `config.schema.json` hints + QR; MMM‑Config QR + regenerate form + backup old config. CONFIRMED — [MMM‑Remote‑Control](https://github.com/Jopyth/MMM-Remote-Control), [MMM‑Config](https://github.com/sdetweil/MMM-Config)

### Inferences
- **Sizing hints per widget kind** (HA `getGridOptions`): give each Homeboard widget kind `defaultSpan`, `minSpan`, `maxSpan` and an "auto height" flag. Small/Medium/Large presets then become named points inside those bounds, and Resize can refuse illegal sizes up front instead of failing after layout. HA's "multiples of 3 on 12 columns" suggests presets that stay aligned when columns change.
- **D‑pad Move** (Homarr `placeGridItem`): a deterministic "pin the moved tile, push colliders, compact vertically" rule is exactly what a step‑by‑step remote‑control move needs: every arrow press produces a predictable layout, with no drag physics. Reading‑order sorting of placements gives a stable D‑pad focus order and accessibility order.
- **Layout document versioning** (Grafana `schemaVersion`): an integer schema version on Homeboard's single JSON layout+settings document lets future migrations run at load, which matters once phone‑edited drafts and older APKs coexist.
- **Auto vs Custom** (Grafana, HA dense placement): an explicit per‑page or per‑region flag for "host packs tiles" (Homeboard's Auto/app‑favourites packing) versus "user positions tiles" makes the two behaviours legible, and Grafana's "resize in Auto prompts switch to Custom" is a good UX for a remote.
- **Per‑resolution layouts** (Homarr): Homeboard's Auto columns/rows could adopt Homarr's Base + derived layouts with "stays generated until edited" and "Reset from Base", for households moving the same config between a 720p and a 4K TV.
- **Shared data providers** (MagicMirror node_helper, Homarr query keys): one provider per widget kind, keyed by its inputs (city, station), so `widget:rain:1` and `widget:rain:2` for the same location fetch once. `suspend()`/`deferUntilVisible` → pause refresh for off‑page or screensaver‑hidden tiles.
- **Schema‑driven settings** (HA `getConfigForm`, Grafana builder, MMM‑Remote‑Control `config.schema.json`): one declarative options schema per widget kind can render both the TV‑side settings screen (D‑pad form controls) and the phone web editor. `getStubConfig` gives sane defaults on Add, and `assertConfig` gives a raw‑JSON fallback.
- **Catalog metadata** (HA `customCards`): name, description, preview flag and documentation URL per kind feed an "Add widget" picker; HA's `getEntitySuggestion` hints at context‑aware suggestions (for example, suggest the rain widget when a location is set).
- **Third‑party widgets without arbitrary code** (Glance extension, TRMNL recipes): a remote HTTPS endpoint returning *structured JSON in a small set of host‑rendered content types* (Glance's stated long‑term goal) with title/link headers and a cache TTL is a native‑friendly plugin protocol. Rendering stays in Homeboard's Views, and no code loading is needed. Glance's HTML variant would need a WebView and its "dangerous HTML" opt‑in.
- **Per‑size templates** (TRMNL): a plugin supplies a rendering for each size class (compact tile, expanded) rather than one responsive layout, which fits fixed TV cells.
- **Named regions** (MagicMirror): less useful for Homeboard's main grid, but a fit for fixed overlay slots (a screensaver/ambient mode with `top_bar` clock and `bottom_bar` ticker) where a grid is overkill.
- **Distribution governance** (HACS, Homepage): if Homeboard opens to outside widgets, a HACS‑style index (PR to a default list, automated checks for a release, an OSI licence and a non‑archived repo, owner‑only submission) plus Homepage's demand gate is a proven low‑maintenance model; Grafana‑style signing becomes relevant only if executable code is loaded.

### Gaps
- No source examined describes D‑pad or remote‑control focus navigation for any of these web dashboards; the closest is Homarr's keyboard placement and Enter/Space header support. Focus‑navigation prior art would have to come from Android TV launchers (outside this note's scope).
- I did not benchmark how any of these layout engines behave at TV distances or resolutions.
