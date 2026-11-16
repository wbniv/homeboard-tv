# Home favorite app sizes

**Status: implemented, container checks passed, installed and launch-verified on Cast1; Cast2 queued and visual acceptance pending · 16 November 2026**

[Deployment and validation record](../testing/2026-11-16-home-favorite-app-sizes.md)
records the exact 0.1.7 APK, successful Cast1 launch, scripted-input failure and
the coordinator-owned Cast2 job deferred during personal use.

Let Will display favorited apps on Home as **Small icons** or **Banners**.
This is one setting for all Home app favorites, independent of the existing
All Apps artwork choice. Default to Small icons so existing Home layouts retain
their current presentation until Will changes it.

## Screen mockups

![Home favorites size choice, Small icons, Banners and Undo](2026-11-16-home-favorite-app-sizes/mockups.svg)

The mockup uses placeholder app artwork. Actual icons and banners come from
installed apps. It shows a six-column Home grid to make the span change clear;
the implementation must also fit the configured Auto and fixed grids.

## Settings and remote interaction

Use **Home settings → Appearance → Icons**. Turn the existing Icons leaf into
two branches, each showing its current value:

| Icons branch | Choices | Applies to |
|---|---|---|
| Home favorites · Small icons | Small icons / Banners | All favorited apps on Home |
| All Apps · TV banners | Launcher icons / TV banners | The existing All Apps view |

The Home favorites submenu shows **Small icons · Current** and **Banners**,
or the reverse current marker. Its subtitle explains: “Banners use two Home
columns. Apps may move to another row or page.” Use the existing compass menu:
Up chooses Small icons, Right chooses Banners, OK performs the displayed center
action, and Back returns without selecting a new value. Merely opening the
submenu changes nothing.

Choosing a size applies it immediately and returns to the invoking surface,
using the existing terminal-action behavior. Show **Home favorites: Banners**
or **Home favorites: Small icons**, with the existing **Undo** action. There is
no separate confirmation step. Re-selecting the current choice does not create
an Undo entry or reflow the layout. Opening this setting from All Apps must keep
the All Apps artwork selection and return location intact.

This first version is a global Home setting. It does not introduce per-app size
overrides, arbitrary app resizing, or another grid-density control.

## Tile appearance and layout

| Property | Small icons | Banners |
|---|---|---|
| Home grid span | 1 column × 1 row | 2 columns × 1 row |
| Artwork | Launcher icon | App-provided TV banner |
| Aspect and mask | Existing icon shape or circular-mask choice | 16:9 image with rounded corners |
| Focus | Existing icon focus, including circular ring and halo | Rounded rectangular outline and soft halo around the image |
| Label | Existing fitted label slot | Centered below the image, ellipsized as needed |
| Missing banner | Not applicable | Center the launcher icon on a neutral 16:9 plate, with the app label below |

Fit a banner's 16:9 image inside the space left after reserving its label and
focus clearance. Never stretch an icon into banner proportions or crop a banner
merely to fill a dense cell. Circular masking applies to small icons only.
Keep focus enlargement inside the available image area and leave the label
stationary. Preserve the full app name in the tile's accessibility description.

Changing size must preserve favorites membership, logical ordering, widget
configuration and grid dimensions. Resolve enlarged tiles against the Home
grid before rendering:

1. Capture the current resolved positions and full layout snapshot for Undo.
2. Keep widgets at their resolved positions and spans.
3. Place favorites in their existing logical order. Keep each app's prior origin
   if the new span fits without overlap; otherwise use the existing deterministic
   placement search for the next available position, including another page.
4. Persist the new size and resolved positions together as one undoable change.
5. Restore focus to the initiating app when it is still present, following it
   to its resolved page. For settings opened from a top control, retain that
   control's focus and clamp the Home page to the new page count.

Shrinking banners to small icons keeps their current origins; it does not silently
repack every gap. Undo restores the entire preceding layout, including exact
positions, page and size. Changing the setting to Small icons later is a new
layout change, rather than a hidden restoration of an earlier arrangement.
An empty Home stays empty. Newly favorited apps use the current Home size.

Move mode must use the selected app span for occupancy, collision resolution and
directional navigation. Removing and restoring a favorite keeps the global size
choice. Deleting or uninstalling an app continues to use the existing actions.

## Choice and action flow

```mermaid
flowchart TD
    A[Home or All Apps] --> B[Home settings]
    B --> C[Appearance]
    C --> D[Icons]
    D --> E[Home favorites with current size]
    D --> L[All Apps artwork with its own current choice]
    E --> F{Choose size}
    F -->|Up| G[Small icons: one Home cell]
    F -->|Right| H[Banners: two Home columns]
    E -->|Back| D
    G --> I[Resolve positions and save one layout change]
    H --> I
    I --> J[Return to caller with updated Home and Undo]
    J -->|Undo| K[Restore prior size, positions and page]
    J -->|Hold OK on app| M[Existing Move and app actions]
    L --> N[Change All Apps artwork only]
```

## Implementation changes

`appArtworkSettings()` now branches into Home favorites and All Apps. Presentation
is selected through `HomeLayout.appUsesBanner()`, and both packing paths share
the same span calculation. `BannerAppTile` reserves a stationary label and focus
clearance before fitting 16:9 artwork. `BannerFocusView` supplies rectangular
focus without a circular mask or glass overlay.

- Add a persisted `homeAppSize` value with exactly `small` and `banner`. An absent
  field uses `small`, preserving current saved layouts. Validate unsupported
  values at the normal layout parsing boundary; do not introduce aliases or
  a separate compatibility layer.
- Share the app-span calculation between automatic packing and explicit-position
  packing. Keep widget span calculation unchanged. Use the same result in move
  previews and nearest-neighbor navigation.
- Centralize the app presentation decision by surface and size. Home uses
  `homeAppSize`; All Apps keeps `appBanners`. Use this decision consistently in
  artwork resolution, tile construction, focus classification and focus restore.
- Reuse installed activity banner lookup and icon fallback. Keep `CircularAppTile`
  for small circular icons. Give banner images their own aspect-preserving layout
  and rectangular focus path; do not alter menu glass or round banner artwork.
- Update `configureTile()`, `createAppFocusHighlight()`, the global focus listener
  and `snapDenseAppHighlight()`: their current Home-is-compact assumptions must
  not leave a circular halo on a banner or retain a halo after changing size.
- Apply the size and resolved positions through the existing layout save/Undo
  mechanism as one transaction. Capture the prior Home page and focus alongside
  the layout for this action: the generic layout snapshot alone does not contain
  navigation state. Undo from All Apps restores that underlying Home state
  without forcing a surface switch. Cancel stale focus animation before rebuilding
  the surface and honor reduced motion during the change and subsequent moves.
- Replace the “Home favorites stay compact” copy and update the quick start and
  related grid/focus plans to describe both supported Home presentations.

## Dated agenda

### 16 November 2026

- [x] Review current artwork settings, Home rendering and placement code.
- [x] Specify the independent Home size setting, mockups and action flow.
- [x] Implement persistence, shared span resolution and size-change Undo.
- [x] Implement the menu branches, banner layout and focus integration.
- [x] Add regression tests and run `task check` and `task build` in containers (357 tests, zero failures; lint has zero errors).

### Device acceptance after implementation

- [x] Deploy a newly numbered APK through the coordinator, preserving app data
  (0.1.7 installed and launch-verified on Cast1; identical APK queued for Cast2).
- [ ] Capture Small icons, Banners, fallback artwork, focus movement and Undo.
- [ ] Verify favorite ordering, widget positions, collisions, page overflow,
  moving a wide tile and returning from an app.
- [ ] Verify both Home sizes with both All Apps artwork settings; each choice
  must remain independent across relaunch and configuration changes.
- [ ] Check an empty Home, one favorite, many favorites, long app names,
  dense grids, RTL, larger text and reduced motion.

Use devices/queue for status and submit complete coordinator-owned jobs; let the
coordinator schedule them. Do not add a personal-use gate. Record actual install,
launch and visual acceptance outcomes separately.

## Acceptance tests

Unit tests should cover saved-size round trips and the default for an absent
field; 1 × 1 and 2 × 1 spans in both placement paths; keeping widgets fixed;
nonoverlapping placement at the right edge and across page boundaries; and Undo
restoring exact prior positions and size. Include a crowded-grid case where
changing to Banners creates an extra Home page, then Undo removes that page.

Device evidence must show usable 16:9 artwork, a readable label, the correct
focus shape and no leftover circular halo on banners. Exercise focus leaving
for top controls and menus, rapid app-to-app navigation, saved focus on resume,
and returning from a launched app. A completed coordinator job establishes job
execution; inspect its captures before marking visual acceptance complete.
