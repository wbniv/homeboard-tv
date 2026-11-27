# The continuous Screensaver list on Cast1

**Date:** 2026‑11‑26 22:43 to 2026‑11‑27 07:14 Bangkok. The deploy ran on the evening of
26 November; the device acceptance ran 06:19–07:14 on 27 November (23:19–00:14 UTC), after
an API usage limit paused the run overnight. **Device:** Cast1 (`chromecast-test-01`,
Chromecast HD, Android 14, 1080p59.94). **Build:** Homeboard **0.1.173**
(`net.biohack.homeboard.debug`, frozen APK sha256 `3e691302…`), the first deployed build with
the list rework (`bd29ece`, `3e1956f`). It was built from the shared `main` working tree, which
also held another session's uncommitted work (a combined build, approved for Cast1). Deploy job
`J‑faa4eebdd197`. Every check reused the installed APK ("exact-match"), so no other session had
replaced 0.1.173 on Cast1 overnight. The build does **not** carry the removed-app fix
(`272568c`, `f225564`), which was merged later. **Covers:** the plan's
[Acceptance](../plans/2026-11-26-screensaver-browsing.md#acceptance), as far as Cast1 and the
coordinator allow. Run by the coordinator only; no direct device access.

**A device change during the run.** The coordinator removed `net.biohack.homeboard` (the old
release, whose Dream was the duplicate "Clock" row) in job `J‑3a13c41a95bb` at 07:07:25–07:07:37.
The list went from **10** entries to **9**. Jobs before it: state, browse, actions, focus,
returns, use‑change (ended about 07:07:16). Jobs after it: hold and remap; remap repeats the
browse on the nine-entry list.

## Result

| Request | Result |
|---|---|
| 1 Deploy, `task check` | **PASS.** `task check` (unit tests and lint) `BUILD SUCCESSFUL in 1m 53s`; `ScreensaverPickerTest` 17/17, 0 failures. |
| 2 Up/Down reach every entry; no page buttons; hold; boundaries; count and thumb | **PASS, except key repeat not testable.** All 10 entries (`J‑a88c845b3791`) and, after the removal, all 9 (`J‑6f99f658d854`) are reached with Down alone. "N of M" reads 1 of 10 … 10 of 10 (1 of 9 … 9 of 9). There are no Previous/Next buttons and no page reset across the old 4/5 and 8/9 boundaries. Down on the last row stays; Up on the first row goes to header Back; Down from Back returns to row 1. The scroll thumb appears once the list scrolls and moves with it. **Hold:** the coordinator's held keys produce no Android key repeat. A 700 ms hold and a 1500 ms hold each moved exactly one row and then stopped (`J‑a88c845b3791` h1/h2, `J‑478184f312c4`), so release does stop movement, but repeat browsing itself cannot be produced here. As a stand-in, 12 Downs 150 ms apart from row 1 ended on 9 of 9 and stayed there 3 s later: no overshoot and no wrap. |
| 3 Right/OK, Left, Back, header Back; returns | **PASS.** Right or OK on a row focuses its first action; Left from an inner action moves left inside the pane, and Left from the leftmost action returns to the row (`J‑9064f6100da8`, two captures per key). Back from actions returns to the row. Back or header Back from the list closes to Home settings. When the list was opened by holding OK on Start screensaver, Back goes to Home with Start focused (`J‑b810c89abb7c`). Returns land on the same entry, action and list position: the built-in Clock's Preview (`reason=cancel`), the built-in Options of Home background (2 of 10, Options focused), WF Clocks' app screen (9 of 10, Preview focused, identical scroll), and Use everywhere (Seahorse Grid 8 of 10). **External settings activity: none listed declares one.** No row offers "Open ‹app› settings". |
| 4 Browsing changes nothing; Use / Undo | **PASS.** Android's state is the same before (`J‑90804e8a2588`) and after (`J‑45873a4ed35f`): selected WF Clocks, enabled 1, activate on sleep 1, idle 600 000 ms, sleep −1, stay-on 0. The only differences come from elsewhere: `running_state` went from running (the overnight Dream) to not-running, and the removed package's provider left. Browsing ran in a `check` without foreground observation and completed, so no other app came to the front. **Use** on Home background moved the **On Home** badge in place; Undo (hold Back on Home: "Undid Screensaver: Home background") put Clock back. Use on WF Clocks gave one **In use** badge, and Undo restored Clock (`J‑218b87e8e591`, `J‑6f99f658d854`). |
| 5 Supported actions | **PASS.** WF Clocks (preview-capable): Use this screensaver, Preview. Aerial Views, Ambient Mode and the other Android-only apps: Use everywhere… only, with "No preview: Android gives apps no way to start another app's screensaver." Built-ins: Use this screensaver, Preview, Options. No dead-end buttons. Badges: Clock **On Home** and WF Clocks **In other apps** while the scopes differed; one **In use** when both were WF Clocks. |
| 6 Change with an app vs Clock | **PASS.** With WF Clocks chosen, Change opens **When it starts · WF Clocks**: only Start on Home after, In other apps and the Android note. Back returns to the list with Change focused. With Clock chosen, Change opens the full **Screensaver · Options** (Backdrop, Dim, Time format, Screen care, Sound, Preview, When it starts). |
| 7 Recreation and package refresh | **Not testable here.** No coordinator workflow recreates Homeboard's Activity, or installs or removes another package, while Homeboard's picker is open inside the same check. The coordinator's own removal of `net.biohack.homeboard` happened between two checks, not while the picker was open; the next check simply showed the nine-entry list at the saved choice. The pure logic is unit tested: `theBrowsedRowFollowsItsIdAcrossARefresh`, `aRemovedEntryGivesTheNearestRemainingRowNotTheFirst`. |

**Not run here:** Cast2 at native 4K (another session's device work), zero external apps and the
40-entry fixture, larger text (1.3× and 2×), long Thai names, narrow touch layouts, accessibility
traversal, permission refusal, and removed or failed-to-launch apps (the removed-app behaviour is
the later fix, not on 0.1.173).

## One finding

**After Use, focus lands differently for an app and a built-in.** On a built-in (Home
background), Use moves focus to Preview, the next action. On an app with a preview screen (WF
Clocks), focus falls back to the row ([zoom](2026-11-26-screensaver-list-cast1/remap-J-6f99f658d854/focus-after-use.jpg)).
The plan says "Keep focus on the action", but the Use button disappears once the entry is in use.
The cause is `MainActivity.java:3197` at `HEAD`:

```java
case USE: addScreensaverAction(actions, getString(R.string.hb_ss_use), action.tag, () -> useScreensaver(entry, entry.app() ? "act:use" : "act:preview")); break;
```

For an app the return tag is `act:use`, which no longer exists after the redraw, so the focus
falls back to the row; for a built-in it is `act:preview`. Not changed here: which control should
hold focus after Use is a design choice.

**Reading the first actions run.** In `J‑39e7e17a2f03` the first capture took 4.75 s, so the next
four captures ran 2.4–3.2 s late and each shows the state one key behind its label. The slower
re-run `J‑9064f6100da8`, with two captures per key, shows every move correct; the timing columns
are in each job's `input-captures.json`.

## Key paths (0.1.173, nine entries)

| Goal | Keys |
|---|---|
| The list | Back (Home), Menu, Down; it opens on Home's choice |
| WF Clocks | Down ×7 from Clock (8 of 9) |
| Change with an app in use | Left, Right (Preview), Down (Change), OK |
| Undo the last choice | close to Home, then hold Back 1.4 s |

The list on Cast1 after the removal: Clock, Home background (built in); Aerial Views, Ambient
Mode, Colors, Homeboard TV, Seahorse Grid, WF Clocks, World Foundry Aquarium Screensaver.

## Jobs

| Dir | Job | Start | What happened |
|---|---|---|---|
| `deploy-0.1.173.log` | `J‑faa4eebdd197` | 11‑26 22:43 | Bump, `task check`, build, install 0.1.173 |
| `list-J-90804e8a2588` | `J‑90804e8a2588` | 06:19 | Before: WF Clocks selected, idle 600 000 ms, Dream running |
| `state-J-ac716e844043` | `J‑ac716e844043` | 06:19 | Current state: 0.1.173 installed (exact match), list opens on Clock, 1 of 10 |
| `browse-J-a88c845b3791` | `J‑a88c845b3791` | 06:21 | Down through all 10; last; Up to header Back; 700 ms hold; Back to Home settings |
| `actions-J-39e7e17a2f03` | `J‑39e7e17a2f03` | 06:24 | Built-in Preview and Options returns, header Back, Use everywhere return (captures late, see above) |
| `focus-J-9064f6100da8` | `J‑9064f6100da8` | 06:33 | Right, Right, Left, Left, Right, Back, OK with two captures each |
| `returns-J-b810c89abb7c` | `J‑b810c89abb7c` | 07:01 | WF Clocks app-screen return; Back from list; list opened from Start, Back to Home |
| `use-change-J-218b87e8e591` | `J‑218b87e8e591` | 07:05 | Use and Undo; Clock's full Options |
| — | `J‑3a13c41a95bb` | 07:07 | Coordinator removed `net.biohack.homeboard` (not this session's job) |
| `hold-J-478184f312c4` | `J‑478184f312c4` | 07:07 | 1500 ms holds move one row each (9 entries) |
| `list-J-45873a4ed35f` | `J‑45873a4ed35f` | 07:08 | After: Android state unchanged |
| `remap-J-6f99f658d854` | `J‑6f99f658d854` | 07:09 | Nine entries; rapid Downs; Change with WF Clocks → When it starts only; Undo |
| `list-J-f367a20664cc` | `J‑f367a20664cc` | 07:13 | Final Android state |
| `capture-J-fa307314b5f6` | `J‑fa307314b5f6` | 07:14 | Final capture |

Recipes: `dev/device/screensaver-list-{state,browse,actions,focus,returns,use-change,hold,remap}.json`.
Focus is easiest to read in `focus-buttons.jpg` and `focus-after-use.jpg`, which crop the rows
and the action buttons from each frame.

## Final Cast1 state

- **Homeboard's choice:** Clock (built in), **On Home** ([frame](2026-11-26-screensaver-list-cast1/remap-J-6f99f658d854/w9-clock-on-home-again.jpg)).
- **Home timer:** "Android decides" (never changed; the When it starts screen shows it selected).
- **Language:** Follow device (never changed).
- **Android:** screensaver WF Clocks, enabled, activate on sleep, idle time 600 000 ms, not running (`J‑f367a20664cc`).
- **Screen:** Google TV Home after the coordinator's cleanup ([capture](2026-11-26-screensaver-list-cast1/capture-J-fa307314b5f6/final.jpg)).
- **Cast1 is free:** nothing of this session is queued or running.

Raw frames, logcat and score files stay on disk (see the folder's `.gitignore`); re-export with
`task chromecast:evidence JOB=<id> OUT=<dir>`.
