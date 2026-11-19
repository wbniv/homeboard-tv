# Layout engines, settings schemas and D‑pad navigation: prior art for Homeboard TV

Research date 2026‑11‑19. **CONFIRMED** means read this session in source code or official docs at the cited URL. **UNVERIFIED** means it comes from a search snippet, from memory, or from a page that could not be read in full. GitHub numbers come from `gh api repos/OWNER/REPO` on 2026‑11‑19 (stars, last push, SPDX licence, archived flag). The library inventory table is at the end of the last section.

## Q1: Grid placement and reflow: how do the major engines place items, resolve collisions, handle density changes and overflow?

### Takeaway
There are three families. **Gravity compactors** (react-grid-layout, gridstack.js, Grafana) store absolute `{x,y,w,h}` and pull items up or left to close gaps. **Packers** (Muuri, Packery, Masonry, CSS Grid `dense`) derive positions from item order and size. **Launcher3** stores absolute cells per page; it resolves drops with a push, then block-move, then single-move, then shrink search, and it migrates the database one way when the grid size changes. Only gridstack keeps a per-column-count cache, so 12→1→12 restores the original positions. Only react-grid-layout persists one layout per breakpoint. Launcher3 is the only maintained Java implementation, and its algorithms are compact enough to re-implement.

### Cited Findings

**react-grid-layout (RGL)**
- CONFIRMED: v2 is a full TypeScript rewrite. It ships `react-grid-layout/core` as "Pure layout algorithms (framework-agnostic)", alongside the React package, a `legacy` v1 API and `extras`. — [RGL README](https://raw.githubusercontent.com/react-grid-layout/react-grid-layout/HEAD/README.md)
- CONFIRMED: compaction is a pluggable `Compactor` interface: `{ type: "vertical"|"horizontal"|null|string; allowOverlap: boolean; preventCollision?: boolean; compact(layout, cols): Layout }`. Built-ins are `verticalCompactor` (the default), `horizontalCompactor`, `noCompactor`, and a factory `getCompactor('vertical', allowOverlap, preventCollision)`. The README includes a custom "gravity" compactor example. — [RGL README](https://raw.githubusercontent.com/react-grid-layout/react-grid-layout/HEAD/README.md)
- CONFIRMED: when `allowOverlap` is true, items may stack, dragging does not push, and compaction is skipped after drag or resize. When `preventCollision` is true (and `allowOverlap` false), "Dragging into another item is blocked (item snaps back)" and "Other items are NOT pushed away". The README recommends that mode with `noCompactor` "for: fixed grids, slot-based layouts". The legacy `verticalCompact` prop maps to `compactType={null}` or `compactor={noCompactor}`. — [RGL README](https://raw.githubusercontent.com/react-grid-layout/react-grid-layout/HEAD/README.md)
- CONFIRMED: the responsive mode stores **one layout array per breakpoint**: `layouts: { lg: [{i,x,y,w,h}], md: [...] }`, with `breakpoints: {lg:1200, md:996, sm:768, xs:480, xxs:0}`, `cols: {lg:12, md:10, sm:6, xs:4, xxs:2}`, and an `onLayoutChange(layout, allLayouts)` callback that hands back every breakpoint's layout for saving. — [RGL README](https://raw.githubusercontent.com/react-grid-layout/react-grid-layout/HEAD/README.md)
- CONFIRMED: `findOrGenerateResponsiveLayout()` returns a clone if a layout exists for the breakpoint. Otherwise it "generates a new layout from the nearest larger breakpoint", then runs `correctBounds` and the compactor. — [RGL src/core/responsive.ts](https://raw.githubusercontent.com/react-grid-layout/react-grid-layout/HEAD/src/core/responsive.ts)
- CONFIRMED: MIT licence, 22,496 stars, last push 2026‑11‑19, not archived. — `gh api repos/react-grid-layout/react-grid-layout`

**gridstack.js**
- CONFIRMED: "Mobile-friendly modern pure Typescript library (no external dependencies)". It ships Angular, React and Vue wrappers, and plain use needs only `gridstack-all.js` and `gridstack.min.css` (positions are CSS-percentage based). — [gridstack README](https://raw.githubusercontent.com/gridstack/gridstack.js/HEAD/README.md)
- CONFIRMED: `column(n, layout)` re-lays items when the column count changes. `ColumnOptions` = `'moveScale'` (default: "scale widget positions and sizes"), `'move'` (keep sizes), `'scale'` (keep positions), `'none'`, `'list'` ("Treat items as a sorted list, keeping them sequentially without resizing (unless too big)") or `'compact'` ("Similar to list, but uses compact() method to fill empty slots by reordering"). "items will never be outside of the current column boundaries". — [gridstack API.md v14.1.1](https://raw.githubusercontent.com/gridstack/gridstack.js/HEAD/doc/API.md)
- CONFIRMED: the engine caches layouts per column count: `_layouts` holds "cached layouts of difference column count so we can restore back (eg 12 -> 1 -> 12)". `columnChanged()` calls `cacheLayout(this.nodes, prevColumn)` whenever the column count shrinks, and `column()` "cache[s] the original layout so you can revert back to previous positions without loss". — [gridstack-engine.ts](https://raw.githubusercontent.com/gridstack/gridstack.js/HEAD/src/gridstack-engine.ts); [API.md](https://raw.githubusercontent.com/gridstack/gridstack.js/HEAD/doc/API.md)
- CONFIRMED: responsive behaviour comes from `columnOpts: Responsive`, which takes `breakpoints?: Breakpoint[]` ("explicit width:column breakpoints"), `columnWidth?` ("wanted width to maintain (+-50%) to dynamically pick a column count"), `columnMax?` (default 12) and `breakpointForWindow?` (window width versus grid width). — [API.md](https://raw.githubusercontent.com/gridstack/gridstack.js/HEAD/doc/API.md)
- CONFIRMED: `compact(layout='compact'|'list', doSort=true)` reclaims empty space. `'compact'` "find[s] truly empty spaces, may reorder items", and `'list'` keeps sort order and moves items up in sequence. `maxRow` defaults to 0, meaning no maximum. — [API.md](https://raw.githubusercontent.com/gridstack/gridstack.js/HEAD/doc/API.md)
- CONFIRMED: collision handling is in `_fixCollisions()`. When a dragged node hits another and the mode is not `'float'`, the engine first tries `swap(node, collide)`. `batchUpdate()` temporarily forces `'float'` mode ("let things go anywhere for now... will restore and possibly reposition later"). — [gridstack-engine.ts](https://raw.githubusercontent.com/gridstack/gridstack.js/HEAD/src/gridstack-engine.ts)
- UNVERIFIED: the exact wording of the user-facing `float: boolean` option ("widgets don't float up to fill gaps"). The engine's `'float'` mode is confirmed, but the option's docstring was not located in `types.ts`.
- CONFIRMED: serialisation uses plain `{x, y, w, h, content}` objects through `save()` and `load(layout, addRemove)`. — [gridstack README](https://raw.githubusercontent.com/gridstack/gridstack.js/HEAD/README.md); [API.md](https://raw.githubusercontent.com/gridstack/gridstack.js/HEAD/doc/API.md)
- CONFIRMED: MIT licence, 9,258 stars, last push 2026‑11‑17. — `gh api repos/gridstack/gridstack.js`

**Muuri, Packery, Masonry and CSS Grid**
- CONFIRMED: Muuri's default layout is configured by `{fillGaps:false, horizontal:false, alignRight:false, alignBottom:false, rounding:false}`. A custom layout function can replace it, and the default algorithm runs in "two shared web workers" when available. Muuri credits Jylänki's "A Thousand Ways to Pack the Bin" survey. — [Muuri README](https://raw.githubusercontent.com/haltu/muuri/HEAD/README.md)
- CONFIRMED: Muuri is MIT, has 10,937 stars, and was **last pushed 2024‑05‑25 (about 18 months stale)**. — `gh api repos/haltu/muuri`
- CONFIRMED: Packery "uses a bin-packing algorithm to fill in empty gaps". Elements can be "stamped in place, fit in a specific spot, or dragged around". — [packery.metafizzy.co](https://packery.metafizzy.co/)
- CONFIRMED: "Packery v3 is licensed under the MIT License. Whereas earlier versions … were previously dual licensed for commercial license". GitHub reports the licence as `none`, so read the docs page rather than the API. Last push 2025‑03‑08, 4,319 stars. — [Packery licence](https://packery.metafizzy.co/license.html); `gh api repos/metafizzy/packery`
- CONFIRMED: Masonry is a "Cascading grid layout library" under the MIT licence (GitHub again reports `none`). Last push 2024‑06‑07. — [Masonry README](https://raw.githubusercontent.com/desandro/masonry/HEAD/README.md); [masonry.desandro.com](https://masonry.desandro.com/)
- CONFIRMED: in CSS `grid-auto-flow: dense` the "packing algorithm attempts to fill in holes earlier in the grid, if smaller items come up later. This may cause items to appear out-of-order". The default "sparse" mode "only ever moves 'forward'… never backtracking". — [MDN grid-auto-flow](https://developer.mozilla.org/en-US/docs/Web/CSS/grid-auto-flow)

**Android Launcher3 (Apache‑2.0, AOSP `main`)**
- CONFIRMED: the drop solver lives in `celllayout/ReorderAlgorithm.java` (© 2023 AOSP, Apache‑2.0). Sibling files include `ItemConfiguration.kt`, `ReorderParameters.kt`, `ViewCluster.kt`, `CellPosMapper.java` and `MulticellReorderAlgorithm.java`. — [Launcher3 celllayout/](https://android.googlesource.com/platform/packages/apps/Launcher3/+/refs/heads/main/src/com/android/launcher3/celllayout/)
- CONFIRMED: there are three solution kinds. `dropInPlaceSolution` succeeds when the nearest target region is already empty. `closestEmptySpaceReorder` drops "in the closest empty space, without moving any other item". `findReorderSolution` moves other items to make room. — [ReorderAlgorithm.java](https://android.googlesource.com/platform/packages/apps/Launcher3/+/refs/heads/main/src/com/android/launcher3/celllayout/ReorderAlgorithm.java)
- CONFIRMED: `findReorderSolutionRecursive` first tries the exact nearest cell. If that fails, it shrinks the dragged widget "in an alternating pattern, shrink 1 in x, then 1 in y" down to `minSpanX`/`minSpanY`. — [ReorderAlgorithm.java](https://android.googlesource.com/platform/packages/apps/Launcher3/+/refs/heads/main/src/com/android/launcher3/celllayout/ReorderAlgorithm.java)
- CONFIRMED: `rearrangementExists` collects the views that intersect the target rectangle, sorted by cellX then cellY. Any view with `canReorder == false` aborts the attempt. It then tries, in order: (1) `attemptPushInDirection`, the push mechanic, where "no displaced item travels through another item without also displacing that item"; (2) `addViewsToTempLocation`, moving the intersecting views "as a block"; (3) `addViewToTempLocation` for each view individually. — [ReorderAlgorithm.java](https://android.googlesource.com/platform/packages/apps/Launcher3/+/refs/heads/main/src/com/android/launcher3/celllayout/ReorderAlgorithm.java)
- CONFIRMED: the push step builds a `ViewCluster`, picks the leading edge from the direction vector, and computes `pushDistance`. Each step it absorbs any view touching that edge into the cluster and shifts the cluster by one cell. It calls `currentState.save()` first so a failure can revert. A diagonal direction is tried one component at a time; a single-axis direction is tried forward, then reversed, then with the axes swapped. — [ReorderAlgorithm.java](https://android.googlesource.com/platform/packages/apps/Launcher3/+/refs/heads/main/src/com/android/launcher3/celllayout/ReorderAlgorithm.java)
- CONFIRMED: `findNearestArea(cellX, cellY, spanX, spanY, direction, occupied, blockOccupied, result)` scans every vacant origin that fits the span. It scores by `Math.hypot(x - cellX, y - cellY)` in grid units and breaks ties by the dot product of the candidate's direction with the drag direction. It returns `-1,-1` if nothing fits. — [ReorderAlgorithm.java](https://android.googlesource.com/platform/packages/apps/Launcher3/+/refs/heads/main/src/com/android/launcher3/celllayout/ReorderAlgorithm.java)
- CONFIRMED: `CellLayout` drag modes are `MODE_SHOW_REORDER_HINT = 0`, `MODE_DRAG_OVER = 1` and `MODE_ON_DROP = 2` (among others). — [CellLayout.java](https://android.googlesource.com/platform/packages/apps/Launcher3/+/refs/heads/main/src/com/android/launcher3/CellLayout.java)
- CONFIRMED: `MulticellReorderAlgorithm` subclasses the solver for foldables. It inserts a non-reorderable one-column "seam" view in the middle, runs the parent algorithm, then removes the seam and shifts cellX back. — [MulticellReorderAlgorithm.java](https://android.googlesource.com/platform/packages/apps/Launcher3/+/refs/heads/main/src/com/android/launcher3/celllayout/MulticellReorderAlgorithm.java)
- CONFIRMED: grid migration is now `model/GridSizeMigrationLogic.kt`, with `GridSizeMigrationDBController.java`, `DeviceGridState.java`, `GridBackupTable.java` and `DbEntry.kt` alongside. The older name `GridSizeMigrationUtil` no longer appears in the `model/` listing. — [Launcher3 model/](https://android.googlesource.com/platform/packages/apps/Launcher3/+/refs/heads/main/src/com/android/launcher3/model/)
- CONFIRMED: the migration has a special case. When the column count is equal and the destination has more rows ("strictly taller"), it copies the table verbatim, optionally shifting cells down if the first page's items sat in the bottom half. Otherwise it copies to a temp table and, inside one `SQLiteTransaction`, migrates the hotseat and then the workspace, then commits. Either way it writes `destDeviceState.writeToPrefs()` "so that the migration does not run again". — [GridSizeMigrationLogic.kt](https://android.googlesource.com/platform/packages/apps/Launcher3/+/refs/heads/main/src/com/android/launcher3/model/GridSizeMigrationLogic.kt)
- CONFIRMED: workspace migration diffs the source against the destination (`getItemsToBeAdded`, `getItemsToBeRemoved`), sorts the items to add, and fills existing screens in order with `solveGridPlacement`. "In case the new grid is smaller, there might be some leftover items … we add them to new screens until all of them are placed". On screen 0 placement starts at row 1 when the smartspace/QSB occupies row 0. — [GridSizeMigrationLogic.kt](https://android.googlesource.com/platform/packages/apps/Launcher3/+/refs/heads/main/src/com/android/launcher3/model/GridSizeMigrationLogic.kt)
- CONFIRMED: in `solveGridPlacement`, an entry whose `minSpanX > trgX || minSpanY > trgY` is removed from the to-place iterator and never placed. Placement uses `findPlacementForEntry` with a `(mNextStartX, mNextStartY)` cursor that "serves as a memoization of last placement", and may assign new `spanX`/`spanY` values. — [GridSizeMigrationLogic.kt](https://android.googlesource.com/platform/packages/apps/Launcher3/+/refs/heads/main/src/com/android/launcher3/model/GridSizeMigrationLogic.kt)
- UNVERIFIED: the sort key `DbEntry` uses in `workspaceToBeAdded.sort()` (believed to be screen, then row, then column). `DbEntry.kt` was not read.
- CONFIRMED: Apache‑2.0 §4 allows redistributing copies or derivative works in source or object form if you (a) include a copy of the licence, (b) mark modified files "stating that You changed the files", (c) keep the original copyright, patent and attribution notices, and (d) carry forward any NOTICE file. §3 grants a royalty-free patent licence. — [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)

**Java and Kotlin ports**
- CONFIRMED (stats only): none of the Android-native grid libraries found is maintained. `felipecsl/AsymmetricGridView` (MIT, Java, last push 2018‑10‑13), `askerov/DynamicGrid` (Apache‑2.0, **archived**, last push 2018‑05‑22) and `Arasthel/SpannedGridLayoutManager` (MIT, Kotlin, **archived**, last push 2018‑11‑30). — `gh api` for each repo
- No Java or Kotlin port of RGL or gridstack turned up in this session (searched GitHub repo names only, not exhaustively).

### Inferences
- RGL's `core` and gridstack's `GridStackEngine` (described as "without any DOM operations") are pure functions over `{x,y,w,h}` arrays. Either could run unchanged in the phone editor. Reaching the TV's Java code would mean porting or re-implementing them; each is a few hundred lines of logic. The gridstack figure comes from its engine description; actual line counts were not measured.
- Launcher3's `ReorderAlgorithm` and `findNearestArea` are self-contained grid algorithms over a `boolean[][]` occupancy matrix. Re-implementing them from their description carries no licence obligation. Copying the files into an MIT repo is allowed if the copied files keep their Apache header, are marked as changed, and the repo ships the Apache licence text and any NOTICE. Those files stay Apache‑2.0 while the rest of the project stays MIT.
- Homeboard already "reflows items without deleting them" on density change, which is stricter than Launcher3. Launcher3 silently skips items whose minimum span exceeds the new grid, and its migration is one-way (guarded by `DeviceGridState` prefs and backed by `GridBackupTable`). gridstack's per-column cache is the closest match to non-destructive reflow with a return path.
- Multi-page overflow: Launcher3 appends new screens for leftovers. RGL, gridstack, Muuri and Packery are single infinite-height canvases with no paging concept, and gridstack's `maxRow` only caps the height.

### Gaps
- RGL's and gridstack's exact collision push order (which item moves first, and recursion limits) was not traced line by line.
- `DbEntry` ordering and how `findPlacementForEntry` chooses shrunk spans were not read.
- I found no maintained Android or Java library implementing dashboard-style compaction outside Launcher3.

## Q2: Layout schemas: what survives grid-density and device-size changes?

### Takeaway
Schemas fall on a spectrum. At one end they store **absolute cells** (Launcher3, Grafana, RGL per breakpoint, gridstack). At the other they store **size intent plus order** and let the host place items (Home Assistant sections, WidgetKit families, Android `targetCellWidth`, Wear OS slots). Absolute schemas survive density changes only when paired with an explicit rule: one-way migration (Launcher3), per-breakpoint copies with a derive-from-larger fallback (RGL), or proportional scaling plus a per-column cache (gridstack). Intent schemas survive by construction but give up exact placement.

### Cited Findings
- CONFIRMED: **Grafana** `gridPos` has `w` from 1 to 24 ("the width of the dashboard is divided into 24 columns"), `h` "In grid height units, each represents 30 pixels", and `x` and `y` in the same units. "The grid has a negative gravity that moves panels up if there is empty space above a panel." — [Grafana JSON model](https://grafana.com/docs/grafana/latest/dashboards/build-dashboards/view-dashboard-json-model/)
- CONFIRMED: Grafana is licensed **AGPL‑3.0**, with 77,988 stars. — `gh api repos/grafana/grafana`
- CONFIRMED: **Home Assistant sections view.** "Each section is divided in 12 columns". A card's `getGridOptions()` returns `columns` (default 12, or `"full"`), `rows` (left undefined to "ignore the rows of the grid"), `min_rows` (default 1), `max_rows`, `min_columns` (default 1) and `max_columns`. One cell is the section width ÷ 12 (about 30 px) by 56 px, with 8 px gaps. — [HA custom card developer docs](https://raw.githubusercontent.com/home-assistant/developers.home-assistant/master/docs/frontend/custom-ui/custom-card.md)
- CONFIRMED: an HA sections view has "Max number of sections wide" and a "Dense section placement" toggle that allows "cards to be arranged automatically in order to fill gaps between cards… you have less control over the order". — [HA sections docs source](https://raw.githubusercontent.com/home-assistant/home-assistant.io/current/source/_dashboards/sections.markdown)
- CONFIRMED: HA's older masonry view uses only `getCardSize()`, where "A height of 1 is equivalent to 50 pixels", and the host distributes cards across columns. — [HA custom card developer docs](https://raw.githubusercontent.com/home-assistant/developers.home-assistant/master/docs/frontend/custom-ui/custom-card.md)
- UNVERIFIED: the per-card YAML override key `grid_options: {columns, rows}`. The user docs page was fetched, but the key did not surface in the extracted text.
- CONFIRMED: the **Launcher3 `favorites` table** columns include `container` (`CONTAINER_DESKTOP = -100`, `CONTAINER_HOTSEAT = -101`, …), `screen` ("if container is CONTAINER_DESKTOP"), `cellX`, `cellY`, `spanX`, `spanY` and `profileId`, with a `favorites_tmp` table used during migration. The grid dimensions are stored separately in `DeviceGridState`. — [LauncherSettings.java](https://android.googlesource.com/platform/packages/apps/Launcher3/+/refs/heads/main/src/com/android/launcher3/LauncherSettings.java); [model/ listing](https://android.googlesource.com/platform/packages/apps/Launcher3/+/refs/heads/main/src/com/android/launcher3/model/)
- CONFIRMED: **Android AppWidget** `targetCellWidth` and `targetCellHeight` "define the target size of the widget in terms of launcher grid cells. If defined, these attributes are used instead of minWidth or minHeight". `maxResizeWidth` and `maxResizeHeight` "define the maximum size that the launcher allows the user to resize the widget to". — [Android app widget layouts](https://developer.android.com/develop/ui/views/appwidgets/layouts)
- CONFIRMED: **Apple WidgetKit** `WidgetFamily` values are `systemSmall`, `systemMedium`, `systemLarge`, `systemExtraLarge`, `systemExtraLargePortrait`, `accessoryCircular`, `accessoryRectangular`, `accessoryInline` and `accessoryCorner` (watchOS). These are size classes, not coordinates. — [WidgetFamily (Apple docs JSON)](https://developer.apple.com/tutorials/data/documentation/widgetkit/widgetfamily.json)
- CONFIRMED (API names only): **Wear OS** watch faces declare `ComplicationSlot`s with `ComplicationSlotBounds`, including `ComplicationSlotBounds.perComplicationTypeBounds` and a `ComplicationSlotBoundsType`. — [ComplicationSlot reference](https://developer.android.com/reference/kotlin/androidx/wear/watchface/ComplicationSlot)
- UNVERIFIED: Wear OS bounds are `RectF`s in a unit square (0..1) relative to the watch face, so they scale with the screen. The reference page text did not expose this.
- CONFIRMED: RGL keeps a layout per breakpoint and generates missing ones from the nearest larger breakpoint. gridstack scales by `newColumn/oldColumn` (`moveScale`) and caches the pre-shrink layout. Launcher3 migrates once and stores the device state. — sources as in Q1

### Inferences
- Patterns that make an absolute schema survive density changes:
    1. **Store the grid size the layout was authored against** (Launcher3 `DeviceGridState`; RGL's breakpoint key). That makes "has the grid changed?" detectable.
    2. **Keep a canonical layout and derive the others.** RGL derives from the nearest larger breakpoint; gridstack caches the widest layout and restores it when the column count grows back.
    3. **Store intent alongside position:** minimum span and preferred span (HA `min_columns`/`columns`, Launcher3 `minSpanX`, AppWidget `targetCell*`/`maxResize*`), so a reflow can shrink an item to its minimum before evicting it.
    4. **Keep a stable order key** so packers (gridstack `'list'`, CSS `dense`, HA dense placement) can reflow deterministically.
- Homeboard's (page, row, column) + span + Small/Medium/Large preset already sits between these two ends. The preset works like a WidgetKit family; the span works like `targetCell*`.
- Grafana's 24-column absolute schema survives *device* size changes only because column width is fluid and height is fixed in pixels. It has no density concept at all.

### Gaps
- Whether HA stores section card order as a list (implied by "less control over the order") or with explicit positions was not confirmed in source.
- WidgetKit's placement rules (how the home screen arranges families) are system-owned and undocumented as an algorithm.

## Q3: Settings schemas to forms: can one schema drive both a phone web form and a D‑pad TV form?

### Takeaway
Every surveyed system is a **flat or shallow list of typed fields**: key, type, label, default, required, plus type-specific options. The differences lie in the type vocabulary and in whether layout hints sit in a separate "UI schema". JSON Schema plus a separate UI schema (JSON Forms, rjsf) is the most portable data contract, and Java validators exist. Of the vanilla-JS renderers, only `json-editor` is maintained, MIT-licensed and dependency-free, and it supports only JSON Schema drafts 3 and 4. Home Assistant's selector-based `ha-form` is the closest design analogue to "declare once, render anywhere", but its renderer is Lit/TypeScript under Apache‑2.0. No surveyed system ships a D‑pad renderer; the closest TV-native one is AndroidX Preference.

### Cited Findings

**JSON Schema renderers (web)**
- CONFIRMED: JSON Forms' "UI schema … describes the general layout of a form", built from elements that are "either Controls or Layouts". — [JSON Forms UI schema docs](https://jsonforms.io/docs/uischema/)
- CONFIRMED: the JSON Forms "Vanilla Renderers" package "must be combined with JSON Forms React", so it is not framework-free. — [vanilla-renderers README](https://raw.githubusercontent.com/eclipsesource/jsonforms/HEAD/packages/vanilla-renderers/README.md)
- CONFIRMED: JSON Forms is MIT with 2,758 stars and was last pushed 2026‑11‑18. — `gh api repos/eclipsesource/jsonforms`
- UNVERIFIED: JSON Forms layout type names (`VerticalLayout`, `HorizontalLayout`, `Group`, `Categorization`) and its Angular and Vue renderer sets were not extracted this session.
- CONFIRMED: react-jsonschema-form (rjsf) is Apache‑2.0, TypeScript/React, with 15,943 stars, last pushed 2026‑11‑19. — `gh api repos/rjsf-team/react-jsonschema-form`
- UNVERIFIED: rjsf `uiSchema` details were not read this session.
- CONFIRMED: **json-editor** "has no dependencies. It only needs a modern browser". It "has full support for JSON Schema version 3 and 4", offers themes for Bootstrap 4, Spectre and Tailwind, and a `show_errors` option of `interaction`, `change`, `always` or `never`. Its API includes `getValue()`, `setValue()` and `validate()`, and it supports `"format"` hints such as `color` and `markdown`. — [json-editor README](https://raw.githubusercontent.com/json-editor/json-editor/HEAD/README.md)
- CONFIRMED: json-editor is MIT with 4,904 stars, last pushed 2026‑10‑30. — `gh api repos/json-editor/json-editor`
- CONFIRMED: **jsonform** requires jQuery and Underscore and is styled for Bootstrap 3. It is MIT, last pushed 2024‑06‑19 (stale). — [jsonform README](https://raw.githubusercontent.com/jsonform/jsonform/HEAD/README.md); `gh api repos/jsonform/jsonform`
- CONFIRMED: **brutusin/json-forms** has an optional Bootstrap extension. It is Apache‑2.0, last pushed 2023‑07‑15 (stale). — [brutusin README](https://raw.githubusercontent.com/brutusin/json-forms/HEAD/README.md); `gh api repos/brutusin/json-forms`
- CONFIRMED (stats only): formio.js is MIT with 2,088 stars, last pushed 2026‑11‑18. — `gh api repos/formio/formio.js`
- UNVERIFIED: formio uses its own form JSON rather than JSON Schema.

**Validators**
- CONFIRMED: **ajv** (the JavaScript validator) is MIT, last pushed 2026‑09‑06. — `gh api repos/ajv-validator/ajv`
- CONFIRMED: **networknt/json-schema-validator** (Java, Apache‑2.0) supports drafts v4, v6, v7, 2019‑09 and 2020‑12, and "The JSON parser used is the Jackson parser". — [networknt README](https://raw.githubusercontent.com/networknt/json-schema-validator/HEAD/README.md)
- CONFIRMED: **everit-org/json-schema** (Java, Apache‑2.0) "is currently in maintenance mode and superseded by erosb/json-sKema". It supports drafts 04, 06 and 07 and "uses the org.json API". — [everit README](https://raw.githubusercontent.com/everit-org/json-schema/HEAD/README.md)
- CONFIRMED: **erosb/json-sKema** (Kotlin, MIT) is a "Json Schema validator library for the Java Virtual Machine" implementing draft 2020‑12. It has 84 stars and was last pushed 2026‑11‑13. — [json-sKema README](https://raw.githubusercontent.com/erosb/json-sKema/HEAD/README.md)
- UNVERIFIED: Android ships the `org.json` package, which would make everit dependency-light on Android. Android compatibility of all three Java validators (desugaring, Jackson size, reflection) was not confirmed.

**Home Assistant selectors and `ha-form`**
- CONFIRMED: HA documents about 44 selector types, including Action, Area, Boolean, Color temperature, Date, Device, Duration, Entity, Icon, Location, Media, Number, Object, Select, Text, Time, Template and Theme. "Selectors can be used to specify what values are accepted for a blueprint". — [HA selectors docs](https://www.home-assistant.io/docs/blueprint/selectors/)
- CONFIRMED: the `ha-form` schema (`HaFormSchema`) is a union. Every field shares a base of `name`, `default?`, `required?`, `disabled?`, `description?` and `context?`. Field kinds are `HaFormSelector {selector}` and primitive types `string`, `integer`, `float`, `boolean`, `select`, `multi_select`, `positive_time_period_dict` and `constant`. Layout kinds are `grid` (`column_min_width`, `flatten`), `expandable` (`title`, `expanded`), `optional_actions` and `divider`. Visibility conditions are `HaFormFieldCondition` / `And` / `Or` / `Not`. The renderer is `HaFormElement extends LitElement`. — [ha-form/types.ts](https://raw.githubusercontent.com/home-assistant/frontend/dev/src/components/ha-form/types.ts)
- CONFIRMED: HA cards provide `getConfigElement()` (a custom editor element) and `getStubConfig()` (the default config for the card picker). — [HA custom card developer docs](https://raw.githubusercontent.com/home-assistant/developers.home-assistant/master/docs/frontend/custom-ui/custom-card.md)
- CONFIRMED: home-assistant/frontend is Apache‑2.0 with 5,811 stars, last pushed 2026‑11‑19. — `gh api repos/home-assistant/frontend`

**Pixlet, TRMNL and Adaptive Cards**
- CONFIRMED: a **Pixlet** app defines `get_schema()` returning `schema.Schema(version = "1", fields = [...])`. Field types are Color, Datetime, Dropdown, Generated, Location, LocationBased, OAuth2, PhotoSelect, Text, Toggle and Typeahead, and each takes an `icon` (Font Awesome 6.1.1 names). "Dynamic fields" (`LocationBased`, `Typeahead`, `Generated`) take a `handler` that is "called with user inputs". — [Pixlet schema docs](https://raw.githubusercontent.com/tidbyt/pixlet/main/docs/schema/schema.md)
- CONFIRMED: tidbyt/pixlet is Apache‑2.0, last pushed 2026‑11‑18. A community fork, tronbyt/pixlet, was last pushed 2026‑11‑16. — `gh api`
- CONFIRMED: **TRMNL** custom fields are YAML. Only `keyname`, `name` and `field_type` are required. Optional keys include `description` (HTML-friendly), `description-LOCALE`, `help_text`, `optional`, `default` and `placeholder`. Types are `author_bio`, `url`, `string`, `multi_string`, `text`, `code`, `number`, `password`, `date`, `time`, `select`, `xhrSelect` (single or multi), `xhrSelectSearch`, `plugin_instance_select`, `time_zone`, `copyable`, `copyable_webhook_url`, `boolean` and `lat_lon`. They are chosen "to leverage native browser validations and UX features". — [TRMNL form builder](https://help.trmnl.com/en/articles/10513740-custom-plugin-form-builder)
- CONFIRMED: TRMNL exposes saved values to templates as `trmnl.plugin_settings.custom_fields_values`. — [TRMNL docs (llms-full)](https://docs.trmnl.com/go/llms-full.txt)
- CONFIRMED: **Adaptive Cards** schema 1.3+ adds client-side validation. `Input.Text` gets `isRequired`, `regex` and `maxLength`; `Input.Number`, `Input.Date` and `Input.Time` get `isRequired`, `min` and `max`; `Input.ChoiceSet` and `Input.Toggle` get `isRequired`. Every input gets `errorMessage` and a `label`, which is "the recommended way of tagging inputs" over `placeholder`. — [Adaptive Cards input validation](https://learn.microsoft.com/en-us/adaptive-cards/authoring-cards/input-validation)
- CONFIRMED: microsoft/AdaptiveCards is MIT with 1,975 stars, last pushed 2026‑11‑18. — `gh api`
- UNVERIFIED: AdaptiveCards ships both JavaScript and Android (Java) renderers from the same repo. Not read this session.

**Android-native settings**
- CONFIRMED: an **AndroidX Preference** "settings screen contains a hierarchy". "You can define this hierarchy as an XML resource, or you can … [create] a hierarchy in code". Values persist to `SharedPreferences` (`getDefaultSharedPreferences`). — [AndroidX settings guide](https://developer.android.com/develop/ui/views/components/settings)
- UNVERIFIED: `androidx.leanback.preference` provides D‑pad TV settings fragments (`LeanbackSettingsFragmentCompat`), and Google now steers TV UI toward Compose for TV. The Leanback release page was fetched, but its deprecation status could not be extracted.

**Schema versioning and migration of saved settings**
- CONFIRMED: Home Assistant config entries carry `VERSION` and `MINOR_VERSION` (both default 1). A "minor version bump is backwards compatible. Only add things that older code can ignore, like a new key with a default". `async_migrate_entry` runs before setup when the stored major version is older or the minor version differs, and "An entry with a newer major version than your config flow fails to set up without calling your code". — [HA config entries docs](https://developers.home-assistant.io/docs/config_entries_index)
- CONFIRMED: Pixlet stamps `version = "1"` on the schema object. — [Pixlet schema docs](https://raw.githubusercontent.com/tidbyt/pixlet/main/docs/schema/schema.md)

### Inferences
- A common denominator any of these could map to is `{key, type, label, description, default, required, options|min|max|regex, visibleIf}`. Mapping each:
    - **JSON Schema:** `properties` + `required` + `enum`/`minimum`/`pattern`, with layout and widget hints in a separate UI schema.
    - **HA selectors:** `{name, selector:{number:{min,max,mode}}}`.
    - **Pixlet and TRMNL:** typed field lists.
    - **Adaptive Cards:** `Input.*` with `isRequired`/`min`/`max`/`regex`.
- Under that model a D‑pad TV renderer and a phone HTML renderer differ only in widget choice per type: toggle, picker list, number stepper, or text input that hands off to the phone.
- JSON Schema is the only candidate with mature validators on *both* sides (ajv in JavaScript; networknt or json-sKema on the JVM). The other formats would need hand-written validation per side. Their advantage is a TV-sized vocabulary: Pixlet, TRMNL and HA all avoid arbitrary nested objects, which are awkward with a remote.
- json-editor is the only maintained, MIT, framework-free renderer found. It is pinned to drafts 3 and 4, so `if`/`then` and `dependentRequired` (later drafts) would not be available for conditional fields. HA's `HaFormCondition` and TRMNL's per-field `optional` show that a simple custom condition grammar is common practice instead.
- For migration, HA's major/minor rule is directly reusable as a convention: the minor version only adds keys that have defaults, while the major version requires a migrate function, and an entry newer than the code refuses to load.

### Gaps
- I found no published D‑pad or TV renderer for JSON Schema, HA selectors or Adaptive Cards (the Adaptive Cards Android renderer was not checked for TV focus support).
- The size and minimum SDK of the Java JSON Schema validators on Android were not measured.
- Pixlet's field-level validation (required, regex) was not documented in the fetched page.

## Q4: D‑pad spatial navigation over irregular tiles

### Takeaway
Nearly every engine runs the same core. It filters candidates to those lying in the pressed direction, prefers candidates that overlap the source's "beam" or slice on the cross axis, and then minimises a weighted distance. They differ in edge versus centre measurement, in the weights, and in whether **containers with focus memory** (`enterTo: last-focused`, `saveLastFocusedChild`, `data-focus`) exist. Android `FocusFinder` has the beam logic and the 13:1 weighting but no per-region memory or container boundaries, beyond `nextFocus*` ids and keyboard-navigation clusters. The framework-agnostic options to study or port are Norigin's core (MIT) and BBC lrud-spatial (Apache‑2.0).

### Cited Findings

**Android `FocusFinder`**
- CONFIRMED: `isBetterCandidate` first requires both rectangles to be candidates (`isCandidate`). It then applies `beamBeats` in both orders, and falls back to `getWeightedDistanceFor(major, minor) = 13 * major² + minor²`, commented "Warning: this fudge factor is finely tuned, be sure to run all focus tests if you dare tweak it". — [FocusFinder.java (AOSP main)](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/view/FocusFinder.java)
- CONFIRMED: in `beamBeats`, "for horizontal directions, being exclusively in beam always wins". For vertical directions "beams only beat up to a point": the in-beam rectangle wins unless the other is "completely closer". User-specified `nextFocus*` ids and keyboard-navigation clusters (`getNextClusterForwardId`, `touchscreenBlocksFocus`) are handled separately. — [FocusFinder.java](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/view/FocusFinder.java)
- CONFIRMED: Android TV docs say "The Android framework automatically applies a directional navigation scheme" that moves focus "to the nearest" object, and that it can be overridden with `android:nextFocusDown` and the other `nextFocus*` attributes. — [Android TV navigation](https://developer.android.com/training/tv/get-started/navigation)
- UNVERIFIED: Compose's `Modifier.focusRestorer()` and `focusProperties { enter/exit }` give per-group focus memory. The Compose page fetched mentions `focusProperties`, but `focusRestorer` was not confirmed.

**Norigin Spatial Navigation**
- CONFIRMED: `@noriginmedia/norigin-spatial-navigation-core` (v4.2.1, MIT) is "Core spatial navigation logic for web and TV", with separate React and React Native bindings packages. — [packages/core/package.json](https://raw.githubusercontent.com/NoriginMedia/Norigin-Spatial-Navigation/HEAD/packages/core/package.json); [README](https://raw.githubusercontent.com/NoriginMedia/Norigin-Spatial-Navigation/HEAD/README.md)
- CONFIRMED: the repo has 498 stars and was last pushed 2026‑11‑17. — `gh api`
- CONFIRMED: its algorithm (`sortSiblingsByPriority`) is commented "Inspired by" MDN's Firefox OS TV remote-control navigation design. A sibling counts as an "adjacent slice" if its cross-axis overlap is at least `ADJACENT_SLICE_THRESHOLD = 0.2` of the source's extent. Adjacent slices are weighted `ADJACENT_SLICE_WEIGHT = 5` against diagonal `1`, and primary-axis distance carries `MAIN_COORDINATE_WEIGHT = 5`. `distanceCalculationMethod` is `'center' | 'edges' | 'corners'` (default `'corners'`), with an optional `customDistanceCalculationFunction`. — [SpatialNavigation.ts](https://raw.githubusercontent.com/NoriginMedia/Norigin-Spatial-Navigation/HEAD/packages/core/src/SpatialNavigation.ts)
- CONFIRMED: per-container options are `saveLastFocusedChild`, `preferredChildFocusKey`, `isFocusBoundary` with `focusBoundaryDirections`, and `trackChildren`. The layout cache goes stale after `LAYOUT_STALE_TIME = 16` ms ("60fps"). — [SpatialNavigation.ts](https://raw.githubusercontent.com/NoriginMedia/Norigin-Spatial-Navigation/HEAD/packages/core/src/SpatialNavigation.ts)
- UNVERIFIED (search snippet): Norigin's "Focus Maps" guide says the pattern was inspired by a Netflix article. — [Norigin devportal](https://devportal.noriginmedia.com/docs/Norigin-Spatial-Navigation/)

**BBC LRUD and lrud-spatial**
- CONFIRMED: **bbc/lrud** is a tree-based model (`registerNode('root', {orientation:'vertical'})`, `handleKeyEvent`). It replaced `grid` with `isIndexAligned`. "LRUD is now in maintenance mode … A new library called LRUD Spatial is now available". GitHub shows it **archived**, last pushed 2023‑07‑19, Apache‑2.0. — [LRUD README](https://raw.githubusercontent.com/bbc/lrud/HEAD/README.md); `gh api repos/bbc/lrud`
- CONFIRMED: **bbc/lrud-spatial** is DOM-based. It takes the exit edge of the focused element and the entry edge of each candidate, then picks "the line between the exit and entry coordinates that has the shortest length". It supports an `lrud-ignore` class, a per-element `data-lrud-overlap-threshold` (0.0 to 1.0), containers (`nav`, `section`, `.lrud-container`) and container focus memory through `data-focus`. It is Apache‑2.0 with 68 stars, last pushed 2026‑07‑09. — [lrud-spatial README](https://raw.githubusercontent.com/bbc/lrud-spatial/HEAD/README.md); `gh api`
- CONFIRMED: **bamlab/react-tv-space-navigation** is a React wrapper around LRUD's core. It notes a virtualisation bug with "hundreds of rows" and says "if you don't require web support, using the native react-native-tvos solution might be a better fit". It is MIT, last pushed 2025‑10‑24. — [bamlab README](https://raw.githubusercontent.com/bamlab/react-tv-space-navigation/HEAD/README.md); `gh api`

**js-spatial-navigation and Enact Spotlight**
- CONFIRMED: **luke-chang/js-spatial-navigation** groups elements into sections with these options:
    - `enterTo`: `''`, `'last-focused'` or `'default-element'`.
    - `restrict`: `'self-first'` (default), `'self-only'` or `'none'`.
    - `straightOnly` (default false).
    - `straightOverlapThreshold`, default 0.5: "an element is counted in the straight directions only if it overlaps the straight area at least 0.3x of its total area" when set to 0.3.
    - `rememberSource`: "the previously focused element will have higher priority".
    - `leaveFor`.

    It is **MPL‑2.0** with 432 stars, last pushed 2024‑05‑23. — [js-spatial-navigation README](https://raw.githubusercontent.com/luke-chang/js-spatial-navigation/HEAD/README.md); `gh api`
- CONFIRMED: **Enact Spotlight**, LG webOS's framework, "is based on a fork of JavaScript SpatialNavigation". It is Apache‑2.0 (keeping MPL‑2.0 attribution for Luke Chang's code), with 348 stars, last pushed 2026‑11‑19. — [Enact spotlight README](https://raw.githubusercontent.com/enactjs/enact/HEAD/packages/spotlight/README.md); `gh api repos/enactjs/enact`
- CONFIRMED: MPL‑2.0 is "file-level" copyleft "designed to encourage contributors to share modifications … while still allowing them to combine your code with code under other licenses". The MPL FAQ answers "Yes" to combining MPL with BSD and Apache code in one program. — [MPL 2.0 FAQ](https://www.mozilla.org/en-US/MPL/2.0/FAQ/)

**W3C CSS Spatial Navigation**
- CONFIRMED: **WICG/spatial-navigation** holds the polyfill (`spatial-navigation-polyfill` on npm) and demos. The spec "has been migrated from WICG to CSS WG as an official draft" (css-nav-1). It defines `--spatial-navigation-contain`, `--spatial-navigation-action` and `--spatial-navigation-function` ("Selects the navigation algorithm"), plus `spatialNavigationSearch()`, `focusableAreas()` and `getSpatialNavigationContainer()`. The repo is **archived**, last pushed 2026‑03‑23, under the W3C Software and Document License (GitHub shows NOASSERTION). — [WICG README](https://raw.githubusercontent.com/WICG/spatial-navigation/HEAD/README.md); [LICENSE.md](https://raw.githubusercontent.com/WICG/spatial-navigation/HEAD/LICENSE.md); `gh api`

**Engineering write-ups**
- CONFIRMED: **Spotify (2023)** kept an in-memory navigation tree of `NavContainer`s (one with a `cycle` option). They split the engine into `NavEngine` (keeps the tree current), `FindFocusIn` (tracks focus) and `Navigate`. `Navigate` "uses the lowest common ancestor algorithm to find the parent element", which helps cyclic navigation. They built a drawing tool that outputs navigation trees as JSON so focus logic can be unit-tested without the app, and called "coming back to the navigation menu from the main view" the non-trivial case. — [Spotify Engineering: TV Spatial Navigation](https://engineering.atspotify.com/2023/5/tv-spatial-navigation)
- CONFIRMED: **Amazon Vega** "Cartesian focus management" moves focus to the "closest" item. For non-uniform items "any element in the same general direction… is a candidate", and "the shortest weighted distance (distance along the input direction is weighted less heavily)" wins. "Focus candidates are selected without regard to their Z-ordering". Overrides are `TVFocusGuideView` and `nextFocusUp`/`Down`/`Left`/`Right`, plus a documented "Restore focus" pattern. — [Amazon Vega focus management](https://developer.amazon.com/docs/vega/0.24/focus-management.html)
- These weightings conflict. Amazon says distance along the input direction is weighted *less*; Android `FocusFinder` weights major-axis (input-direction) distance 13× *more*; Norigin weights the main coordinate 5× more. — [Amazon Vega](https://developer.amazon.com/docs/vega/0.24/focus-management.html) vs [FocusFinder.java](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/view/FocusFinder.java) vs [SpatialNavigation.ts](https://raw.githubusercontent.com/NoriginMedia/Norigin-Spatial-Navigation/HEAD/packages/core/src/SpatialNavigation.ts)
- CONFIRMED (stats only): react-native-tvos is MIT with 1,432 stars, last pushed 2026‑11‑19. — `gh api`
- No primary Netflix or Disney engineering write-up on their TV focus engine was found in this session.

### Inferences
- **Weaknesses of `FocusFinder` on irregular grids.** It scores each press from geometry alone, so on mixed spans the result depends on which tile is "in beam". Concretely:
    - Moving from a 1×1 under a 3×2 widget and back can land on a different 1×1 than the one you started from.
    - There is no "return to where I came from" (`rememberSource`) and no per-region `last-focused`.
    - Vertical beam rules differ from horizontal ones by design.

  The usual fixes in the other libraries are (a) remembering the source element per direction, (b) container-level memory with `enterTo`, and (c) adjacent-slice thresholds (0.2 in Norigin, 0.5 in js-spatial-navigation, per-element in lrud-spatial) so a small overlap does not count as "straight".
- Homeboard's tiles live on a known cell grid, so a cell-space variant is possible. Run the same beam and weighted-distance scoring on (row, col, spanX, spanY) instead of pixels, which makes it deterministic and unit-testable without layout. Spotify's "draw a layout → JSON nav tree → test" tool is the same idea.
- For an MIT Java codebase: Norigin core (MIT) and lrud-spatial (Apache‑2.0) are small enough to port with attribution. js-spatial-navigation's MPL‑2.0 allows combination, but copied files would stay MPL. Re-implementing from the documented algorithms avoids all of this.

### Gaps
- No Netflix or Disney primary source was found. The Netflix "focus maps" attribution comes only from a Norigin snippet.
- Compose for TV focus APIs (`focusRestorer`, `focusGroup`) and Leanback `BrowseFrameLayout` focus-search hooks were not confirmed this session.
- No library's handling of focus across *pages* (Homeboard's paged grid) was found. All the surveyed engines assume scrolling containers.

## Q5: Undo and transactional editing patterns in these layout systems

### Takeaway
None of the web grid libraries (RGL, gridstack) ships undo. Both expose immutable or serialisable layout arrays plus change callbacks, which suits host-side snapshot undo. Launcher3 uses **solve on a scratch copy, then commit or revert** for drags, **deferred deletes with commit or abort** for its Undo snackbar, and **copy to a temp table inside one SQLite transaction** for migration. Grafana keeps whole-dashboard version snapshots with diff and restore. Home Assistant versions saved settings with major/minor numbers.

### Cited Findings
- CONFIRMED: during a reorder search, Launcher3 copies live state into an `ItemConfiguration` (`copyCurrentStateToSolution`) and a scratch occupancy grid (`mTmpOccupied`). "We save the current configuration -- if we fail to find a solution we will revert to the initial state". The live grid changes only when a solution is applied on drop. — [ReorderAlgorithm.java](https://android.googlesource.com/platform/packages/apps/Launcher3/+/refs/heads/main/src/com/android/launcher3/celllayout/ReorderAlgorithm.java)
- CONFIRMED: Launcher3 `ModelWriter` has `prepareToUndoDelete`. "Delete operations tracked using enqueueDeleteRunnable will only be called if commitDelete is called… one of commitDelete() or abortDelete MUST be called after this method". — [ModelWriter.java](https://android.googlesource.com/platform/packages/apps/Launcher3/+/refs/heads/main/src/com/android/launcher3/model/ModelWriter.java)
- CONFIRMED: grid migration copies to `favorites_tmp` (`TMP_TABLE`), runs in `SQLiteTransaction`, commits, and writes `DeviceGridState` prefs in a `finally` block. A `GridBackupTable` class exists. — [GridSizeMigrationLogic.kt](https://android.googlesource.com/platform/packages/apps/Launcher3/+/refs/heads/main/src/com/android/launcher3/model/GridSizeMigrationLogic.kt); [model/ listing](https://android.googlesource.com/platform/packages/apps/Launcher3/+/refs/heads/main/src/com/android/launcher3/model/)
- CONFIRMED: gridstack's `batchUpdate()` is called "EARLY as it will call saveInitial() so we can detect where we started for _dirty and collision". The per-column `_layouts` cache lets a column change be reversed. A grep of the API docs for "undo" or "history" found nothing. — [gridstack-engine.ts](https://raw.githubusercontent.com/gridstack/gridstack.js/HEAD/src/gridstack-engine.ts); [API.md](https://raw.githubusercontent.com/gridstack/gridstack.js/HEAD/doc/API.md)
- CONFIRMED: RGL's compactor contract says "Array of layout items (clone before mutating!)" and returns a new layout. The README mentions `onDragStop` callbacks but no undo or history feature. — [RGL README](https://raw.githubusercontent.com/react-grid-layout/react-grid-layout/HEAD/README.md)
- CONFIRMED: Grafana's "dashboard version history feature lets you compare dashboards and restore previously saved dashboard versions. Grafana saves 20 versions by default". — [Grafana version history](https://grafana.com/docs/grafana/latest/dashboards/build-dashboards/manage-version-history/)
- CONFIRMED: Home Assistant's config entry major/minor versioning and migrate hook are described under Q3. — [HA config entries docs](https://developers.home-assistant.io/docs/config_entries_index)

### Inferences
- Snapshot undo (store the whole small layout array per step) is what these libraries implicitly support. RGL's immutability rule and gridstack's `save()` make each step a cheap copy. Command-pattern undo would need inverse operations for push, reflow and compaction cascades, which no library provides.
- Homeboard's "draft on phone → apply transactionally" maps onto two of Launcher3's patterns: the scratch `ItemConfiguration` then commit (draft, then apply), and temp table plus transaction (atomic apply). Grafana's version list is the precedent for keeping N prior applied layouts for rollback.

### Gaps
- I found no open-source dashboard editor with an inspectable command-pattern undo stack for grid layouts. Excalidraw and similar canvas editors were out of scope and not checked.

## Q6: Library inventory: licence, activity, and fit for vanilla JS or Java

### Takeaway
Actively maintained and MIT- or Apache-compatible: RGL core, gridstack, Norigin core, lrud-spatial, json-editor, ajv, networknt and json-sKema, plus Launcher3 and FocusFinder as Apache‑2.0 Java reference code. **Archived or stale:** bbc/lrud (archived), WICG spatial-navigation (archived), SpannedGridLayoutManager and DynamicGrid (archived), Muuri, Masonry, jsonform and brutusin (no push since 2023–2024), everit (maintenance mode). **Licence flags:** Grafana is AGPL‑3.0, so borrow concepts only. js-spatial-navigation is MPL‑2.0 (file-level copyleft). The WICG polyfill uses the W3C Software and Document License.

### Cited Findings
All rows come from `gh api repos/OWNER/REPO` on 2026‑11‑19 unless a different source is cited. "Plain JS/Java?" is CONFIRMED where a source is cited in the Q1–Q5 findings above; otherwise it says UNVERIFIED.

| Repo | Stars | Last push | Licence | Archived | Plain JS / Java use |
|---|---|---|---|---|---|
| [react-grid-layout](https://github.com/react-grid-layout/react-grid-layout) | 22,496 | 2026‑11‑19 | MIT | no | `core` subpackage is framework-agnostic TS (CONFIRMED) |
| [gridstack.js](https://github.com/gridstack/gridstack.js) | 9,258 | 2026‑11‑17 | MIT | no | Plain JS, no dependencies (CONFIRMED) |
| [muuri](https://github.com/haltu/muuri) | 10,937 | 2024‑05‑25 | MIT | no (stale) | Plain JS (CONFIRMED) |
| [packery](https://github.com/metafizzy/packery) | 4,319 | 2025‑03‑08 | MIT for v3 (GitHub shows none) | no | Plain JS or jQuery (CONFIRMED) |
| [masonry](https://github.com/desandro/masonry) | 16,681 | 2024‑06‑07 | MIT (GitHub shows none) | no (stale) | Plain JS or jQuery (CONFIRMED) |
| [Launcher3 (AOSP)](https://android.googlesource.com/platform/packages/apps/Launcher3/) | n/a | `main` | Apache‑2.0 | n/a | Java/Kotlin reference (CONFIRMED) |
| [AsymmetricGridView](https://github.com/felipecsl/AsymmetricGridView) | 1,829 | 2018‑10‑13 | MIT | no (stale) | Android Java (UNVERIFIED fit) |
| [DynamicGrid](https://github.com/askerov/DynamicGrid) | 915 | 2018‑05‑22 | Apache‑2.0 | **yes** | Android Java |
| [SpannedGridLayoutManager](https://github.com/Arasthel/SpannedGridLayoutManager) | 620 | 2018‑11‑30 | MIT | **yes** | Android Kotlin |
| [Norigin-Spatial-Navigation](https://github.com/NoriginMedia/Norigin-Spatial-Navigation) | 498 | 2026‑11‑17 | MIT | no | `-core` package is framework-free (CONFIRMED) |
| [bbc/lrud](https://github.com/bbc/lrud) | 104 | 2023‑07‑19 | Apache‑2.0 | **yes** | Plain JS, tree model |
| [bbc/lrud-spatial](https://github.com/bbc/lrud-spatial) | 68 | 2026‑07‑09 | Apache‑2.0 | no | Plain JS over the DOM (CONFIRMED) |
| [js-spatial-navigation](https://github.com/luke-chang/js-spatial-navigation) | 432 | 2024‑05‑23 | MPL‑2.0 | no | Plain JS (CONFIRMED) |
| [enactjs/enact](https://github.com/enactjs/enact) (Spotlight) | 348 | 2026‑11‑19 | Apache‑2.0 | no | React-based framework (UNVERIFIED whether standalone) |
| [WICG/spatial-navigation](https://github.com/WICG/spatial-navigation) | 224 | 2026‑03‑23 | W3C SW & Doc | **yes** | Plain JS polyfill |
| [react-tv-space-navigation](https://github.com/bamlab/react-tv-space-navigation) | 322 | 2025‑10‑24 | MIT | no | React only |
| [react-native-tvos](https://github.com/react-native-tvos/react-native-tvos) | 1,432 | 2026‑11‑19 | MIT | no | React Native |
| [lightning-tv/solid](https://github.com/lightning-tv/solid) | 102 | 2026‑10‑12 | Apache‑2.0 | **yes** | Solid/Lightning |
| [rdkcentral/Lightning](https://github.com/rdkcentral/Lightning) | 204 | 2026‑10‑12 | Apache‑2.0 | no | WebGL TV framework (UNVERIFIED focus engine details) |
| [react-jsonschema-form](https://github.com/rjsf-team/react-jsonschema-form) | 15,943 | 2026‑11‑19 | Apache‑2.0 | no | React only |
| [jsonforms](https://github.com/eclipsesource/jsonforms) | 2,758 | 2026‑11‑18 | MIT | no | Its "vanilla" renderers still need React (CONFIRMED) |
| [json-editor](https://github.com/json-editor/json-editor) | 4,904 | 2026‑10‑30 | MIT | no | Plain JS, no dependencies, drafts 3/4 (CONFIRMED) |
| [jsonform](https://github.com/jsonform/jsonform) | 2,813 | 2024‑06‑19 | MIT | no (stale) | Needs jQuery + Underscore (CONFIRMED) |
| [brutusin/json-forms](https://github.com/brutusin/json-forms) | 618 | 2023‑07‑15 | Apache‑2.0 | no (stale) | Plain JS (UNVERIFIED) |
| [formio.js](https://github.com/formio/formio.js) | 2,088 | 2026‑11‑18 | MIT | no | Plain JS (UNVERIFIED) |
| [ajv](https://github.com/ajv-validator/ajv) | 14,882 | 2026‑09‑06 | MIT | no | Plain JS validator |
| [networknt/json-schema-validator](https://github.com/networknt/json-schema-validator) | 1,089 | 2026‑11‑09 | Apache‑2.0 | no | Java, uses Jackson (CONFIRMED) |
| [everit-org/json-schema](https://github.com/everit-org/json-schema) | 900 | 2025‑08‑01 | Apache‑2.0 | no (maintenance mode) | Java, uses org.json (CONFIRMED) |
| [erosb/json-sKema](https://github.com/erosb/json-sKema) | 84 | 2026‑11‑13 | MIT | no | Kotlin/JVM, draft 2020‑12 (CONFIRMED) |
| [home-assistant/frontend](https://github.com/home-assistant/frontend) | 5,811 | 2026‑11‑19 | Apache‑2.0 | no | Lit/TS (CONFIRMED) |
| [tidbyt/pixlet](https://github.com/tidbyt/pixlet) | 848 | 2026‑11‑18 | Apache‑2.0 | no | Go/Starlark (schema concept only) |
| [microsoft/AdaptiveCards](https://github.com/microsoft/AdaptiveCards) | 1,975 | 2026‑11‑18 | MIT | no | JS + Android renderers (UNVERIFIED) |
| [usetrmnl/byos_hanami](https://github.com/usetrmnl/byos_hanami) | 794 | 2026‑11‑19 | MIT | no | Ruby (schema concept only) |
| [grafana/grafana](https://github.com/grafana/grafana) | 77,988 | 2026‑11‑19 | **AGPL‑3.0** | no | Schema concept only |

- CONFIRMED: AOSP `FocusFinder.java` is Apache‑2.0 Java and already sits inside Android, so Homeboard calls it implicitly through standard View focus search. — [FocusFinder.java](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/view/FocusFinder.java)

### Inferences
- Licence compatibility with an MIT project:
    - **MIT** (RGL, gridstack, Packery v3, Masonry, Muuri, Norigin, json-editor, ajv, json-sKema, JSON Forms): use freely, keeping the copyright notice.
    - **Apache‑2.0** (Launcher3, FocusFinder, lrud-spatial, networknt, everit, HA frontend, rjsf, Enact): usable in an MIT app if the licence and NOTICE ship with it and modified files are marked. The patent grant is a plus. Those files stay Apache‑2.0.
    - **MPL‑2.0** (js-spatial-navigation): copied files stay MPL, and modifications to them must be shared. The rest of the project can stay MIT.
    - **AGPL‑3.0** (Grafana): do not copy code. The `gridPos` schema idea is not protected expression.
- For the TV (Java), no maintained drop-in library exists for grid compaction or spatial navigation. Re-implementing from Launcher3, FocusFinder and Norigin is the realistic path. For the phone (vanilla JS), gridstack, RGL `core`, Norigin `-core` and json-editor are drop-in candidates. Whether RGL `core` and Norigin `-core` can be used from vanilla JS without a bundler (ESM-only builds) was not verified.

### Gaps
- Bundle sizes and whether ESM-only packages (RGL v2 removed its UMD bundle, per its migration table) can be loaded without a build step on the TV-served phone page were not measured.
- Enact Spotlight's usability outside Enact/React was not checked.
