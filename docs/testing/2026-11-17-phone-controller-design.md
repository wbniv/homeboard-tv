# Goal-directed phone controller validation

Scope: approved [controller design](../plans/2026-11-17-phone-editor-goal-directed-design.md),
including Will's direct list manipulation requirement. The combined APK also
preserves the fullscreen animation and integrates the independently approved world
clock. See the separate clock validation record for its native visual acceptance.

## Implemented behavior

The controller opens on the current dashboard and named card rows. Task pages show
relevant controls; geometry uses one-based numbers under Advanced placement.
Favorites has drag grips, insertion feedback, neighbor movement, edge scrolling,
cancel, draft Undo and accessible movement. Clock places use the same interaction.
Add apps is separate. A single draft feeds review and **Update [TV name]**.

TV identity and per-widget generations prevent retargeting stale drafts. Independent
TV changes rebase; conflicting fields require a choice. An unknown update result
retains its exact operation ID until receipt reconciliation or same-operation retry.
Drafts/pending operations use tab storage without session credentials. A stable
local listening port permits recovery across sessions at the same browser origin.
Changed address/port and denied session storage remain limits.

A failed SharedPreferences commit restores the entire transaction in memory,
including the operation receipt and revision, rather than acknowledging a failed
save on the next retry. A fault-injection unit test verifies this behavior.

## Container validation

- `task check`: Java unit tests, English/Thai native resource parity and Android lint.
- `task phone:check`: pure draft/merge/geometry tests, JS English/Thai string and
  placeholder parity, Chromium Pixel and WebKit iPhone UI acceptance, plus clock
  component/model acceptance.
- Real bundled UI against a controlled API: dashboard-first navigation, typing,
  keyboard and pointer reorder, insertion marker, pointer cancellation, long-list
  autoscroll, local reorder Undo, separate app picker, HTML escaping, combined
  Update/Undo, native-preview request isolation, one-based placement, snapped card
  move/resize and outside-drop cancellation, independent and conflicting TV edits,
  lost-response receipt reconciliation, same-tab restore, TV identity isolation,
  Thai controls and 200% text sizing without horizontal overflow.
- Private screenshots: `artifacts/phone-browser/`.

These are browser engine tests with phone viewports, not a physical handset test.
A separate physical phone reaching Chromecast over the household LAN has not been
verified. The earlier external-container connection timed out; its cause remains
unestablished. This redesign does not convert that failed connection into evidence
of successful LAN access.

## Deployment and native acceptance

0.1.43 was an intermediate combined build, hash
`514b60dee90435ac7373a4f0ca83b507acf367b37bf94179488a3c7078153254`.
Cast1 installation/launch job `J‑82155f6d43c6` passed. Cast2 `J‑4e7cc10663a3`
verified the identical installed hash; foreground verification remains deferred.

Phone probe `J‑b4780477ad8f` completed but its app-level assertion failed with
403 before authenticated state. Captures show native menus rather than successful
pairing. Do not count it as phone API acceptance. The household Undo-history
inspection `J‑e23407e5f919` shows clock edits and a resize. Will confirmed those
are his edits; they have been preserved. No rollback was attempted. The failed
probe does not establish an app pairing defect: its blind scripted key could act
on arbitrary UI while Will was editing.

The corrected debug probe uses an isolated preference namespace, seeds a default
four-card dashboard with copied app keys, and clicks only a visible Allow button.
It cancels callbacks on pause. The recipe sends no blind navigation key. This
avoids interpreting arbitrary focus as TV approval or changing household settings.
A new numbered deployment is required for that fixture and the evidenced clock
corrections. Final deployment/native acceptance is pending.

Installs preserve app data;
foreground acceptance is a separate coordinator job. The native HTTP probe checks
TV approval, metadata, authenticated artwork/activity, mixed Update, idempotent
retry, preview, composite Undo and session Stop, restoring household settings.
