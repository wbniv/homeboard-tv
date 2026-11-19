# Original inline critique · November 19, 2026

Archived before revision; see the plan’s review disposition for decisions.

> [!NOTE]
> **Review, 2026‑11‑19.** The critiques appear inline as callouts beside the text they address. **CAUTION** means settle it before implementation starts, **WARNING** means fix it in the plan, and **NOTE** is minor. Three matter most:
>
> - the provider census under **Scope and evidence**: nobody has checked that any app on Cast1 offers a widget;
> - the grid-density policy under **Java implementation boundaries**: a global column change can make a widget impossible to fit;
> - older builds meeting the new key, in the same section: skewed worktree deploys make that likely.
>
> Housekeeping: `TODO.md` has no entry for this plan, and the plan and its bundle are not yet committed.

> [!NOTE]
> **Critique: there are six built-in widgets, not five.** `WidgetKind` now lists Grocery first, before Air, Calendar, Rain, Favorites and World clock ([WidgetKind.java](../../launcher/app/src/main/java/com/gathin/tvhome/WidgetKind.java), line 5). Update the opening paragraph above and the overview page.

> [!CAUTION]
> **Critique: count the providers before building anything.** The evidence shows that Android *can* host widgets on Cast1, not that any app on Cast1 *offers* one. The 2026‑11‑18 inventory ([packages.txt](../../artifacts/provider-compatibility/2026-11-18/inventory-before/packages.txt)) lists few third-party apps: Ticket to Ride, YouTube, Google TV, Play Games, the Projectivy Overflight plugin and Homeboard's own packages. Whether any of them declares a widget provider is unknown. TV builds of apps rarely ship one, and Google Play on a TV hides phone-only apps, so step 4's "two real third-party providers" may have no candidates unless phone APKs are sideloaded.
>
> Add a step 0 that needs no new code: a coordinator diagnostics recipe that records the installed providers (`dumpsys appwidget`) and whether anything handles `android.appwidget.action.APPWIDGET_BIND` (`pm resolve-activity`), on Cast1 and Cast2. State the kill criteria now. If no bind activity resolves, stop. If one resolves but no real provider exists, decide whether a feature that only the fixture can use is worth steps 2–4.

> [!WARNING]
> **Critique: there is no memory budget.** At inventory time Cast1 reported 1.47 GB of memory in total and 457 MB available ([hardware-software.json](../../artifacts/provider-compatibility/2026-11-18/inventory-before/hardware-software.json), `memory_bytes`). Each hosted widget inflates its `RemoteViews` inside Homeboard's process, and each update wakes the provider's process. A list widget also keeps the provider's `RemoteViewsService` bound. Nothing in the acceptance table measures this. Add a row for Homeboard and provider memory with zero, one and four Android widgets (coordinator `meminfo`), and for behavior after 24 h on screen. If the numbers call for it, cap the number of live Android widgets per page.

> [!WARNING]
> **Critique: Configure is not always possible.** The hold menu, the expanded pane and mockup panel 4 ("Configure in provider app") all offer Configure unconditionally. Re-running a provider's configuration activity on an existing ID is supported only when the provider declares `widgetFeatures="reconfigurable"` (`WIDGET_FEATURE_RECONFIGURABLE`, API 28). Otherwise the activity may finish at once, or it may set up fresh state that the existing tile never shows. Show Configure only for reconfigurable providers, and offer **Remove and add again** for the rest. At add time, `configuration_optional` (API 31) lets the host skip configuration; say whether Homeboard honors it.
>
> The flowchart also has no branch for node M when no legal placement exists, for example when the provider's minimum exceeds the grid at the current column count. Add that branch: release the pending ID and explain why.

> [!WARNING]
> **Critique: on touch screens, every tap opens the expanded view.** With the full-screen contract unchanged, a Pixel or tablet user can never press a widget's own button, such as play/pause or a checkbox, on the dashboard. Every phone launcher passes taps through to the widget, so on touch screens Homeboard would be the clumsiest widget host on the device. The contract suits the TV remote. For touch, consider passing taps through and moving Homeboard's actions to long press, with expansion behind a small affordance. This is Will's call; record the decision here.

> [!NOTE]
> **Critique: name the mechanism for the input boundary.** In compact mode, set the wrapper to `FOCUS_BLOCK_DESCENDANTS` and intercept touches in `onInterceptTouchEvent`. **Interact with widget** switches it to `FOCUS_AFTER_DESCENDANTS`. List, grid and stack widgets scroll, so in Interact mode D-pad Left/Right at a list's edge must not change the dashboard page.

> [!CAUTION]
> **Critique: older builds will meet the new key, and that path is untested.** `HomeLayout.parse` keeps any `widget:` key ([HomeLayout.java](../../launcher/app/src/main/java/com/gathin/tvhome/HomeLayout.java), `read()`). `WidgetKind.of` throws `IllegalArgumentException` for a key it does not know ([WidgetKind.java](../../launcher/app/src/main/java/com/gathin/tvhome/WidgetKind.java), line 12). `MainActivity.expandWidget` calls it without a guard ([MainActivity.java](../../launcher/app/src/main/java/com/gathin/tvhome/MainActivity.java), line 2046), although `PhoneConfiguration.kind` catches it.
>
> An older build therefore reads `widget:android:…`, keeps it, and fails wherever the key first reaches an unguarded switch; nobody has traced where. This is likely, not hypothetical: the deployment section below has several worktrees deploying to the same TVs, and the 20 persisted Undo snapshots (`MAX_UNDO_ENTRIES`, `undo_history`, MainActivity.java lines 68–69) would carry the key into an older build as well.
>
> Ship a tolerant build first. It should render an unknown key as an inert **Widget not supported by this version** tile and never crash. Add the layout `schemaVersion` that the [investigation's Stage 0](../investigations/2026-11-19-widget-plugin-systems.md) proposes. Add a test that parses a layout containing an Android instance with today's `WidgetKind`.

> [!CAUTION]
> **Critique: grid density is global, so "reject an impossible fit" has no answer for it yet.** Rejecting works when one tile is resized. Cell size, however, also changes when Will picks a column count (Auto or 4–12), a row count or a default panel size, and when the display changes. Any of these can push an Android widget below its provider minimum. Homeboard cannot reject a global setting because of one widget without silently blocking that setting. Launcher3 resolves this case by dropping the item (`GridSizeMigrationLogic`, see the [investigation](../investigations/2026-11-19-widget-plugin-systems.md)), and this plan rightly forbids deletion, so it must choose one of three policies:
>
> - block the change with a message that names the widget;
> - let the tile span more cells than its saved size;
> - keep the tile in place as **Too small at this grid size**.
>
> The plan also never says how Homeboard's S/M/L panel sizes and `panelSpans` map onto a provider's `minWidth`, `minHeight` and resize steps in dp. Write that mapping down; the picker's "Minimum 2 × 2" in the mockups depends on it.

> [!WARNING]
> **Critique: this lifecycle needs diagrams.** The add model is a state machine written as one line of prose, and it stops at `committed`. It omits the failure ends (released) and everything after commit: removed but retained for Undo, unavailable, set up again, and garbage-collected. Per the house rule ("plans need mockups and diagrams"), add two diagrams:
>
> - a Mermaid `stateDiagram-v2` covering the instance's whole life;
> - a `sequenceDiagram` for allocate → bind dialog → configure activity → result → commit, including process death while the provider's activity is in front.
>
> Name the store too. The durable instance records and the layout should be written by one `SharedPreferences` editor in a single `commit()`, the way `PhoneConfigurationStore.trackInstances` already chains its edit. Two stores written separately reopen the interrupted-commit problem this section tries to close.

> [!WARNING]
> **Critique: the existing retention policy keeps bindings for a long time.** Undo keeps 20 snapshots and persists them across restarts (`MAX_UNDO_ENTRIES = 20`, `PREF_UNDO_HISTORY`, MainActivity.java lines 68–69). A removed widget therefore stays bound until 20 later changes push its snapshot out, which may take days. While it is bound but hidden, the provider still receives `onUpdate` on its `updatePeriodMillis` schedule. On a TV with 1.4 GB of memory those are wasted wakeups. That cost is acceptable, but the plan should name the place where garbage collection runs. Run it at the eviction point where a snapshot falls off the history, as well as at startup reconciliation; otherwise it runs nowhere in particular.

> [!WARNING]
> **Critique: the plan never says what the browser shows.** A browser cannot render `RemoteViews`, so the phone editor needs a defined representation for an Android tile. One option is the app icon with the widget label and size. Another is a snapshot bitmap that the TV draws from the live host view and serves with the draft. The browser also cannot know the provider's minimum, maximum and resize axes. Send them in the draft model for each instance; otherwise a resize looks legal in the browser and fails only at **Update TV**. Add the phone editor's tile and its **Add on the TV** notice to the mockups.

> [!WARNING]
> **Critique: the sequence needs a step 0 and a smaller step 2.**
>
> - **Step 0:** run the provider census from the critique under **Scope and evidence** first. It costs one coordinator recipe and can end the project before a fixture exists.
> - **Fixture identity:** Cast1 already has a `net.biohack.homeboard.fixture` package installed ([packages.txt](../../artifacts/provider-compatibility/2026-11-18/inventory-before/packages.txt)). Say whether the widget fixture is that package or a new one, and how it is removed from Will's TVs afterwards.
> - **Split step 2:** it bundles ownership, picker, pending state, sizing, serialization, lifecycle, recovery, expansion and Undo, with nothing deployable before the end. Split it so each part ships and gets lived with:
>     1. one fixed-size tile, where Remove deletes the ID;
>     2. sizing and resize;
>     3. Undo retention;
>     4. expanded interaction.
> - **Defer touch devices:** step 3 brings in two devices that the coordinator cannot reach today, and on them Homeboard competes with the phone's own launcher, which hosts widgets better. Consider making v1 Cast1 and Cast2 only, with Pixel and tablet as a follow-up. This is Will's call.

> [!WARNING]
> **Critique: `/verify` has nothing to run here.** The table says what evidence is needed but not how to produce it. The `[verify]` workflow runs numbered steps exactly as written and pastes their output below each one. Add numbered verification steps, each with its command or coordinator job and its expected result. Add rows for the gaps raised above: memory and wakeups, a 24 h soak, an older build reading the new layout, and a global grid change with an Android widget at its minimum.

> [!WARNING]
> **Critique: the mockups do not follow the house mockup rule.** The rule asks for self-contained `.html` screens on a 1440×900 canvas, each with a same-basename `.png`. The bundle has instead:
>
> - one `index.html` that loads `mockups.svg` by a relative reference, which `md-to-html.sh` does not inline, so the embedded preview comes out blank;
> - a 1200×1360 poster of six panels;
> - no PNG at all.
>
> The panels show the happy path and one failure. They omit the states the text promises:
>
> - the empty picker (**No widget apps installed on this device**);
> - an unsupported device (**Android widgets cannot be added on this device**);
> - an impossible fit;
> - **Interact with widget** with focus inside the provider;
> - a display-only provider;
> - the phone editor's tile and its notice.
>
> Panel 4 also offers Configure unconditionally (see the Configure critique under **User workflow**).
