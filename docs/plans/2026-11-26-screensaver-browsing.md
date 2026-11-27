# Browse screensavers without page buttons

**Status: implementation authorized by Will ("do"), 26 November; steps 2–4 built on branch `screensaver-browsing`; device acceptance (step 5) run on Cast1 with 0.1.173 on 27 November, partial: see the evidence notes under Acceptance and the [Cast1 record](../testing/2026-11-26-screensaver-list-cast1.md).**
The documentation sweep precedes the UI change. Will, 26 November: “I hate the
'previous' and 'next' buttons on the screensaver page … there must be a better way.”

**Recommendation:** replace the four-item pages with one continuously scrolling
list. Up and Down move through screensavers themselves. The focused row stays
visible and updates the adjacent detail pane. Right or OK enters that entry's
actions. Browsing never changes the saved screensaver.

**[Open the visual review and keyboard prototype](2026-11-26-screensaver-browsing/review.html)**
— includes four screen mockups, the navigation diagram and the implementation plan.

## Current problem

`MainActivity.drawScreensaver()` uses `SCREENSAVER_PAGE_SIZE = 4`, then adds
Previous and Next buttons. Reaching another screensaver requires leaving the
list to operate its paging controls. Descriptions appear in both the cards and
detail pane, making rows tall and limiting the working set.

The [current Cast2 capture](2026-11-26-screensaver-browsing/current-native-picker.png)
also exposes a separate native-rendering sizing defect: resource/window bounds
can be smaller than the actual embedded dashboard, squeezing the panel and
clipping its controls. That width correction was deployed separately in 0.1.168;
the proposed navigation is still a design only. Removing paging alone would not
fix geometry. The proposed layout fits measured dashboard bounds.

## Proposed screens

![Continuous list with visible focus and a separate saved-choice badge](2026-11-26-screensaver-browsing/list.svg)

![Action focus while preserving the browsed entry and list position](2026-11-26-screensaver-browsing/actions.svg)

![Large text uses fewer visible rows and scrolling details](2026-11-26-screensaver-browsing/large-text.svg)

![An Android app without preview exposes only its supported actions](2026-11-26-screensaver-browsing/no-preview.svg)

The images are proposed UI illustrations, not device acceptance captures. Preview
artwork is illustrative. Do not promise a live embedded preview from another app:
the current companion contract supplies a separate full-screen activity.

## Navigation and state

![Focus and return-state diagram](2026-11-26-screensaver-browsing/workflow.svg)

![Directional focus map with first and last row boundaries](2026-11-26-screensaver-browsing/focus-map.svg)

| Input or event | Result |
|---|---|
| Open Screensaver | Focus the saved entry; scroll it into view. Preserve the actual Home and Android choices. |
| Up / Down in list | Move one entry; scroll only enough to reveal it. Update details, without playing, launching or saving. |
| Hold Up / Down | Ordinary key-repeat browsing; bounded at the ends; no wrap to the other end. |
| Up on the first entry | Move to the header Back button. Down from Back returns to that entry. |
| Down on the last entry | Stay on the last entry. The count and scroll indicator show the end. |
| Right or short OK on a row | Focus that entry's first available action. No implicit selection or app launch. |
| Left from actions | Return to the same row and scroll position. |
| Back from action area | Return to the same list row. Back from the list closes the picker to its originating settings entry. |
| Header Back | Close the whole picker to its origin. |
| Preview, then Back | Restore the same entry, scroll position and Preview action; save nothing. Preserve the existing explicit acceptance behavior of each preview. |
| Use this screensaver | Use the existing save/Undo transaction; update its badge in place. Keep focus on the action. |
| Options / app settings, then Back | Restore the same entry, scroll position and initiating action. |
| Touch / pointer | Scroll naturally; tap a row to inspect it, tap an action to act. No page controls. |

**As built (branch `screensaver-browsing`):** the workflow above is what the code does. The
list is a RecyclerView with stable ids (one per entry id); a row's focus redraws only the
detail pane and the count. `ScreensaverPicker.State` keeps the browsed id, the first visible
row's id and offset, the last action's tag and whether the chooser came from Home settings
or from holding OK on Start screensaver; a return (Preview of either kind, Options, an app's
settings screen, Use everywhere, a package change, an Activity recreation) restores all four.
Back from the action area returns to the row; Back or header Back from the list closes to
the origin. Below 720 dp of measured width the list and detail stack vertically with the
same state. Use redraws the screen in place on the same row, offset and action.

The focus ring means “you are browsing this”; the saved-choice badge means “this
is used.” If Home and Android use different entries, label the badges **On Home**
and **In other apps** with the actual scopes, rather than displaying two ambiguous
“In use” badges. The details retain the existing **When it starts** and Android
handover explanation. Changing scope still uses the existing permission and
confirmation flow.

## Layout

- Use the available dashboard area, with a header and a stable left list/right
  detail arrangement. Calculate bounds from the measured root, not Android's
  lower-resolution window metrics. Keep this UI independent of widget scale.
- List rows show an icon, app/name and at most a short capability/status line.
  Long explanations belong on the right. At normal TV text size, show roughly
  six rows and a partial next row where space permits; the count is not a fixed
  page size or an acceptance requirement.
- A subtle, non-focusable scrollbar plus “3 of 12” communicate position. A partly
  visible next row indicates more content. Neither is an extra navigation stop.
- Keep built-in entries first, then installed apps in stable localized name order.
  Use the existing stable entry/component ID for focus and saved identity. Do not
  expand each app's internal scenes into fake separate screensavers: its own
  settings still choose the tank, clock or scene.
- Right pane: a bounded static illustration/icon, title, short description,
  available actions, and timing/scope. Its content can scroll independently; keep
  actions reachable and do not let long help displace them below the screen.
- Large text shows fewer rows; allow longer names to wrap to two lines and let
  details scroll. Do not shrink the type to retain six rows. On narrow touch
  displays, stack list and detail with the same saved identity and explicit Back
  to list; preserve position. This is a layout adaptation, not a second settings
  model.

## Supported actions and edge states

Built-ins retain Preview, Use this screensaver and Options. Apps declaring a
preview screen retain that app's real Preview, Home selection and app settings.
Android-only apps show “Preview is not available from this app”, their settings
action if provided, and the existing Use everywhere flow. Hide unsupported
actions rather than add a dead-end Preview button.

**Folded in by Will (26 November, "fold it into the list rework"):** when Home's choice is an app,
**Change** under When it starts opens only When it starts (Home timer, In other apps, the
Android-follows note), titled for the app, never the built-in Clock's look options.

Two built-ins alone need no scrollbar. A long catalogue uses the same list with
recycled rows. Re-querying installed apps must preserve the browsed ID and first
visible ID/offset; do not snap to item zero. If the focused app disappears, focus
the nearest remaining entry and explain its removal. If it was the saved choice,
show its unavailable state and the existing runtime fallback; do not silently
save a replacement. Permission denial and a failed external launch keep the
entry and initiating action available for retry, using the existing failure UI.

**Decided 26 November (coordinator's recommendation; Will: "whatever you recommend"; deferred,
not reviewed):** when Homeboard does not manage the TV and the saved app is gone, the saved
choice is kept (nothing is saved over it), Home plays the default slot meanwhile, the notice appears once per
disappearance, and the list shows the app as an unavailable row with no Preview or Use; a
reinstall works again. When Homeboard manages the TV, the previous fall-back (restore Android's
setting, choose the default slot) is unchanged, because Android's behaviour with a missing
component is untested.

## Cooper design review

Apply the [shared filter](/home/will/docs/howtos/alan-cooper-design-filter.md).
The primary person is Will browsing from the sofa with a D-pad. His stated
objection is the navigation work imposed by page buttons. His goal is to find,
preview and choose a screensaver; page boundaries are an implementation detail.

The design removes those artificial stops, keeps the objects themselves navigable,
distinguishes browsing from a saved choice, and remembers position through Preview,
Options and interruptions. It reuses existing save, scope, permission and Undo
semantics. Descriptions move out of the navigation rail to reduce visual and
remote-control effort. Visible overflow/count, larger-text layouts and explicit
returns make the same interaction discoverable without adding onboarding.

A thumbnail grid was considered: it favors visual comparison, but most installed
Android apps expose neither a thumbnail nor a preview, and it weakens the stable
Right-to-actions relationship. A horizontal carousel keeps much of the same
serial browsing problem. A continuous vertical list best fits the current mixed
catalogue and remote controls. Search and filters are deferred until a real
catalogue demonstrates a need.

## Implementation sequence

1. Verify the separately deployed measured-bounds correction (0.1.168) at 1080p
   and 4K. Keep it separate from this proposed navigation change.
2. Replace `screensaverPage`/`SCREENSAVER_PAGE_SIZE` and the Previous/Next builder
   with a vertical RecyclerView using stable IDs. Bind a row once; focusing it
   updates only the detail pane and positional hint. Do not rebuild the list on
   every focus move.
3. Add explicit cross-pane focus handling and a small picker state containing
   browsed ID, list anchor/offset and last action ID. Restore it after subviews,
   external activities and Activity recreation. Fresh openings start at the
   active choice. Remove the old pagination state; no compatibility layer.
4. Preserve existing ScreensaverRegistry capability checks, Stage 5 companion
   preview contract, timing/scope management, error handling and Undo. Add English
   and Thai strings for new labels and counts.
5. Check in containers, deploy one numbered frozen APK through the coordinator,
   then run the acceptance walkthrough below. Update the user guide and workflow
   diagram to the implemented behavior.

## Acceptance

- Cast1 at 1080p and Cast2 at native 4K: reach every entry using Up/Down alone;
  no Previous/Next buttons and no page reset. Hold a direction across former
  boundaries and verify release stops movement.

  **Evidence (Cast1, 0.1.173, 2026‑11‑27): PASS for Up/Down, partial for hold.** Every entry reached with Down alone, 10 of 10 (`J‑a88c845b3791`) and, after the coordinator removed the old Homeboard, 9 of 9 (`J‑6f99f658d854`); no page buttons, no reset at the old boundaries. The coordinator's held keys give no Android key repeat (700 ms and 1500 ms holds each moved one row, then stopped: `J‑478184f312c4`), so repeat browsing is not testable here; 12 Downs 150 ms apart stopped on the last row. Cast2 at 4K not run. [Cast1 record](../testing/2026-11-26-screensaver-list-cast1.md).
- Compare zero external apps, the actual installed set and a controlled 40-entry
  fixture; first/last boundaries and scrollbar/count are correct.

  **Evidence: PASS for the installed set only.** First row: Up goes to header Back; last row: Down stays; "N of M" and the thumb track the row. Zero external apps and the 40-entry fixture not run.

  **Round 2 (0.1.175):** Cast2 at native 4K not run; the deploy to `chromecast-test-02` was refused by the agent's permission check and needs Will's approval.
- Right/OK, Left, Back and header Back follow the table. Return from both types
  of Preview, built-in Options and an external settings activity to the exact
  entry, action and list position.

  **Evidence: PASS.** Right/OK, Left, Back and header Back behave as the table (`J‑9064f6100da8`, `J‑39e7e17a2f03`, `J‑b810c89abb7c`); returns from the built-in Clock's Preview, Home background's Options, WF Clocks' app screen and Use everywhere land on the same entry, action and scroll. No listed app declares an external settings activity, so that return is untested.
- Merely browsing changes no saved choice or Android setting. Use/Undo restores
  the previous choice and scope. Permission refusal changes neither.

  **Evidence: PASS (permission refusal not run).** Android's screensaver settings identical before and after (`J‑90804e8a2588`, `J‑45873a4ed35f`); Use moved the badge in place and Undo restored Clock (`J‑218b87e8e591`, `J‑6f99f658d854`). Finding: after Use, focus goes to Preview for a built-in but to the row for an app (return tag `act:use`, `MainActivity.java:3197`).

  **Round 2 (0.1.175):** after Use, focus now stays on Preview for both WF Clocks and a built-in (`J‑2857aaaa029e`); Undo restored Clock.
- Test preview-capable, Android-only, settings-less, removed and failed-to-launch
  apps. Unsupported actions are absent and explanations remain readable.

  **Evidence: PASS for preview-capable (WF Clocks), Android-only (Aerial Views, Ambient Mode and others) and settings-less entries; removed and failed-to-launch not run on 0.1.173.**

  **Round 2 (0.1.175):** a failed external launch tested on the Pixel: Open Clock settings for Android's Clock showed "Android could not open that action" and kept the entry and action for a retry (`J‑721ea550e5ae`).
- Larger device text (1.3× and 2×), long Thai names, narrow touch layouts and
  accessibility traversal: rows and actions remain reachable, focus never leaks
  to Home, no clipped action labels, no forced text shrink.

  **Evidence: not run** (no coordinator workflow for large text, narrow touch layouts or accessibility traversal on Cast1).

  **Round 2 (0.1.175): partial, with defects.** Cast1 at 1.3×: actions reachable and the pane scrolls, but names beside a badge break mid-word and are ellipsized. At 2.0×: Options is clipped at the pane edge, the last row's name disappears behind its badge, and the When it starts values break mid-word (`J‑eb788c478c96`, `J‑4878341483f5`). Pixel: Homeboard runs in landscape, so the stacked layout is not reachable; taps inspect rows and act on actions, but a row name breaks mid-word, the action row is cut at the bottom, and swipes change the inspected entry instead of scrolling naturally (`J‑721ea550e5ae`). Long Thai names and accessibility traversal not run.
- Pause/recreate and package refresh retain picker state. Scroll does not start
  app activities, decode large artwork or create per-row live players.

  **Evidence: partial.** Pause and return through Preview, Options and Use everywhere keep the state; Activity recreation and a package change while the picker is open cannot be produced by the coordinator. Browsing ran in a check that fails if another app comes to the front, and completed, so scrolling started no app activity.

This plan changes the chooser only. It preserves the implemented
[top-row Start screensaver button](2026-11-26-top-row-start-screensaver.md), the
[companion contract](2026-11-26-screensaver-companion-contract.md), or who owns
Android's idle behavior.
