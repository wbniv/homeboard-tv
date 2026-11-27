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

## Round 2 (2026‑11‑27, 08:15–08:42 Bangkok)

**Build:** Homeboard **0.1.175** (sha256 `7c5bdd88…`), with the removed-app fix and the focus fix
(`cf38dfb`). It was deployed with `task deploy DEVICE=chromecast-test-01,phone-pixel-8-pro`
(Cast1 `J‑07ec83309871`, Pixel `J‑0b9b3dcb2df5`; [log](2026-11-26-screensaver-list-cast1/deploy-0.1.175.log),
`task check` `BUILD SUCCESSFUL in 1m 46s`). As before, it was a combined build from the shared
tree. **Cast2 was not deployed:** the agent's permission check refused the deploy command that
named `chromecast-test-02`, so the Cast2 part needs Will's approval and was not run.

| Item | Result |
|---|---|
| 1 Deploy | **PASS for Cast1 and the Pixel; Cast2 not deployed** (permission refused, see above). The tablet was left out (recovery-required). |
| 2 Cast2 at native 4K | **Not run.** No deploy to Cast2 was possible; nothing was changed on Cast2. |
| 3 Large text (Cast1) | **1.3×: PASS with one defect. 2.0×: FAIL.** See below. Font scale read first as `1.0` (`J‑686e99382811`); set to 1.3 (`J‑36df3f6c51df`) and 2.0 (`J‑f74606ba0f7e`) persistently, because `TEMPORARY=true` restores inside the same job, before a separate check could open the list; set back to `1.0` (`J‑5bde02d8600e`; readback 1.0, and 1.0 again on a final read). |
| 4 Narrow layout (Pixel) | **Partly testable; the stacked layout is not reachable.** Homeboard runs in landscape on the Pixel (frame 2244×1008, rotation 1, content 2131 px wide), so the list and detail stay side by side and the stacked layout below 720 dp never appears. Touch works: a tap on a row inspects it (2 of 11, detail updated, nothing launched); a tap on Options opens Options; header Back returns to the same row. Defects below. (`J‑721ea550e5ae`) |
| 5 Focus after Use (Cast1) | **PASS.** Use on WF Clocks leaves focus on **Preview**; Use on Home background leaves focus on **Preview**; Undo restored Clock both times (`J‑2857aaaa029e`, [zoom](2026-11-26-screensaver-list-cast1/r2-focus-use-J-2857aaaa029e/focus-after-use.jpg)). |

### Large text (Cast1, `J‑eb788c478c96` at 1.3, `J‑4878341483f5` at 2.0)

- **1.3×:** about four rows show; every action stays reachable and its label whole; the detail
  pane scrolls to reveal When it starts and In other apps
  ([t8](2026-11-26-screensaver-list-cast1/r2-text13-J-eb788c478c96/t8-pane-scrolled-1.3.jpg)).
  **Defect:** names beside a "Needs Use everywhere" badge break mid-word and are cut with an
  ellipsis ("Amb / ient …", "Seah / orse…", "World / Fou…",
  [t10](2026-11-26-screensaver-list-cast1/r2-text13-J-eb788c478c96/t10-last-row-1.3.jpg)), instead of
  wrapping whole words to two lines.
- **2.0×:** about two rows show and the pane scrolls, but labels are clipped:
  - **Options** on a built-in is cut off at the pane's right edge, a sliver, though it still takes
    focus ([t5](2026-11-26-screensaver-list-cast1/r2-text20-J-4878341483f5/t5-options-clipped-2.0.jpg));
  - the last row's name disappears entirely behind its badge, and "WF Clocks" breaks as "WF Cl / ocks"
    ([t10](2026-11-26-screensaver-list-cast1/r2-text20-J-4878341483f5/t10-last-row-name-hidden-2.0.jpg));
  - the When it starts values break mid-word into a narrow column ("Andr / oid's", "Cloc / ks"),
    and the footer hint and "In other apps" are cut at the bottom.
  No text was shrunk; focus never left the picker.

### Pixel touch (`J‑721ea550e5ae`, interactive touch session)

- Path: tap Homeboard settings (top row), tap the Screensaver arm. The list showed **11** entries
  on the Pixel ([frame 05](2026-11-26-screensaver-list-cast1/r2-pixel-J-721ea550e5ae/05-list-side-by-side.jpg)).
- **Defect, clipping:** the third row's name breaks mid-word, "Cloc / k", beside its badge, and the
  detail pane's action row is cut at the bottom edge, with **Options** touching the pane's right
  edge ([frame 07](2026-11-26-screensaver-list-cast1/r2-pixel-J-721ea550e5ae/07-tap-inspects-row.jpg)).
- **Defect, touch scrolling:** two upward swipes in the list did not scroll it naturally. The first
  moved nothing and reset the browsed entry to Clock (1 of 11); the second, slower and longer,
  scrolled about one row and changed the browsed entry to row 3
  ([frame 15](2026-11-26-screensaver-list-cast1/r2-pixel-J-721ea550e5ae/15-after-swipe-2.jpg)). A swipe
  should scroll without changing what is inspected.
- **A failed external launch:** the Pixel lists Android's own Clock screensaver with **Open Clock
  settings**. Tapping it showed "Android could not open that action" and left the entry and the
  action in place for a retry
  ([frame](2026-11-26-screensaver-list-cast1/r2-pixel-J-721ea550e5ae/16-open-clock-settings-failed-toast.jpg)),
  which is the plan's failed-to-launch behaviour.
- Nothing was chosen or saved on the Pixel; the session ended with `input:finish`, and the
  coordinator's cleanup closed Homeboard.

### Round 2 jobs

| Dir | Job | What happened |
|---|---|---|
| `deploy-0.1.175.log` | `J‑07ec83309871`, `J‑0b9b3dcb2df5` | 0.1.175 to Cast1 and the Pixel |
| `r2-focus-use-J-2857aaaa029e` | `J‑2857aaaa029e` | Focus after Use, WF Clocks and Home background; Undo |
| `r2-pixel-J-721ea550e5ae` | `J‑721ea550e5ae` | Pixel touch session |
| `r2-font-scale/` | `J‑686e99382811`, `J‑36df3f6c51df`, `J‑f74606ba0f7e`, `J‑5bde02d8600e` | Font scale read 1.0; 1.3; 2.0; back to 1.0 |
| `r2-text13-J-eb788c478c96` | `J‑eb788c478c96` | The list at 1.3× |
| `r2-text20-J-4878341483f5` | `J‑4878341483f5` | The list at 2.0× |
| `r2-list-J-a0c18d47f45b` | `J‑a0c18d47f45b` | Final Android state |
| `r2-capture-J-2a6873a76d92` | `J‑2a6873a76d92` | Final capture |

Recipes: `dev/device/screensaver-list-r2-{focus-use,large-text,pixel-session}.json`.

### Round 2 final device states

- **Cast1:** Homeboard's choice Clock (On Home, after Undo,
  [frame](2026-11-26-screensaver-list-cast1/r2-focus-use-J-2857aaaa029e/f9-clock-on-home-after-undo.jpg));
  Home timer "Android decides" and language "Follow device", never changed; font scale 1.0;
  Android's screensaver WF Clocks, enabled, activate on sleep, idle 600 000 ms, not running
  (`J‑a0c18d47f45b`); Google TV Home in front
  ([capture](2026-11-26-screensaver-list-cast1/r2-capture-J-2a6873a76d92/final.jpg)).
- **Pixel:** 0.1.175 installed; nothing saved; Homeboard closed by cleanup.
- **Cast2:** untouched (no deploy, no jobs).

## Round 3 (2026‑11‑27, 09:00–09:20 Bangkok)

**Build:** Homeboard **0.1.176** (sha256 `3872d589…`), with the layout and touch fixes (`c639eee`,
`aff3a55`), deployed with `task deploy DEVICE=chromecast-test-01,phone-pixel-8-pro`
([log](2026-11-26-screensaver-list-cast1/deploy-0.1.176.log); `task check` `BUILD SUCCESSFUL in 1m 36s`).
Cast1 `J‑a3f284462e41` succeeded. On the Pixel, `J‑e4a50591648b` **installed and verified** 0.1.176
but the job then failed its launch check with `device-locked`; the touch session that followed
reused the installed APK ("exact-match"), so the Pixel does run 0.1.176. Cast2 untouched.

| Item | Result | Jobs |
|---|---|---|
| 1 Deploy | **PASS** on Cast1; **Pixel installed** (launch check failed: phone locked at that moment). | `J‑a3f284462e41`, `J‑e4a50591648b` |
| 2 Cast1 1.0× | **PASS.** Close to round 1; the visible change is that "Needs Use everywhere" badges now sit under the names, and Change sits under the On Home value. The pane scrolls to In other apps · Use everywhere…; footer whole. | `J‑f79e3066bd11` |
| 2 Cast1 1.3× | **PASS.** Names whole on one line ("Ambient Mode", "World Foundry Aquarium Screensaver" on two lines), badges below, all three actions on one line, pane scrolls to its end, footer whole on two lines. | `J‑bef7b1ddad65` |
| 2 Cast1 2.0× | **PASS with two small defects.** Names wrap at whole words ("Home / background"), actions wrap onto a second line (Options below Use and Preview) and none is clipped, the pane scrolls to its end, the footer is whole on three lines. Defects: the badge text is cut at the row's right edge ("Needs Use everywher") and the status line is ellipsized ("Android runs it, in e…"); and **Right from Preview, when Options has wrapped to the next line, moves focus to header Back** instead of Options (Down from Back then returns to the row), so Options is reached only by Down from Use. | `J‑0b1d82f2887f`, [t2](2026-11-26-screensaver-list-cast1/r3-text20-J-0b1d82f2887f/t2-actions-wrap-2.0.jpg), [zoom](2026-11-26-screensaver-list-cast1/r3-text20-J-0b1d82f2887f/options-down.jpg) |
| 2 Font scale | Read `1.0` before (round 2), set 1.3 `J‑2c5a55b56f07` and 2.0 `J‑8d8a6ed9c42b`, back to 1.0 `J‑b3bacd726cf2`; read 1.0 at the end (`J‑aa5e459b07b4`). | |
| 3 Pixel | **PASS.** The action row (Preview, Options) is in full view, with When it starts below it. A swipe scrolled the list by about two rows and the inspected entry stayed Clock, 1 of 11. A tap on Colors inspected it (4 of 11, Use everywhere…). Options round trip: tap Options opens Clock's options; header Back returns to the same scroll and entry. Nothing clipped; names fit and badges sit below. | `J‑bf4e4c7a7a5c` |
| 4 Regression | **PASS.** Down reached all 9 entries with "1 of 9 … 9 of 9", Down on the last stays, Up ×8 back to row 1; the built-in Clock's Preview (`screensaver started mode=preview entry=clock` … `reason=cancel`) returned to Clock with Preview focused. | `J‑9ac4f8a6b813` |

Frames: [1.0 last row](2026-11-26-screensaver-list-cast1/r3-text10-J-f79e3066bd11/t8-last-row-1.0.jpg),
[1.3 open](2026-11-26-screensaver-list-cast1/r3-text13-J-bef7b1ddad65/t1-open-1.3.jpg),
[1.3 pane end](2026-11-26-screensaver-list-cast1/r3-text13-J-bef7b1ddad65/t6-pane-end-1.3.jpg),
[2.0 pane end](2026-11-26-screensaver-list-cast1/r3-text20-J-0b1d82f2887f/t10-pane-end-2.0.jpg),
[2.0 badge cut](2026-11-26-screensaver-list-cast1/r3-text20-J-0b1d82f2887f/t6-badge-cut-2.0.jpg),
[Pixel](2026-11-26-screensaver-list-cast1/r3-pixel-J-bf4e4c7a7a5c/contact-sheet.jpg).

Recipes: `dev/device/screensaver-list-r3-{text,regression,pixel-session}.json`.

### Round 3 final device states

- **Cast1:** Homeboard's choice Clock (On Home; nothing was chosen this round); Home timer "Android
  decides" and language "Follow device", never changed; font scale 1.0 (`J‑aa5e459b07b4`);
  Android's screensaver WF Clocks, enabled, activate on sleep, idle 600 000 ms, not running
  (`J‑52227960799a`); Google TV Home in front ([capture](2026-11-26-screensaver-list-cast1/r3-capture-J-e37e050227e3/final.jpg)).
- **Pixel:** 0.1.176 installed; nothing chosen or saved; Homeboard closed by the coordinator's cleanup.
- **Cast2:** untouched. Both Cast1 and the Pixel are free.

## Round 4 (2026‑11‑27, 11:28–11:40 Bangkok)

**Build:** Homeboard **0.1.177**, with the 2.0× fixes (`2d79f5f`). The orchestrator deployed it,
with Will's explicit approval for Cast2 ("deploy to Cast2"): Cast1 `J‑f233a211dc68`, Cast2
`J‑be76353e807a`. This session did not deploy; every check reused the installed APK ("exact-match").

| Item | Result | Jobs |
|---|---|---|
| 1 Cast2 start state | Android: Seahorse Grid selected, enabled, activate on sleep, idle 600 000 ms, sleep 3 600 000 ms, stay‑on 3, not running. Homeboard: choice Clock (On Home), 8 entries, "1 of 8". YouTube was in front before the checks. | `J‑b15d4ac5b5b1`, `J‑935f30f8c2ce` |
| 1 Cast2 at 4K: browse and keys | **PASS.** Down reached all 8 entries ("1 of 8 … 8 of 8"); Down on the last stays; the thumb tracks. Right from a row focuses its first action, Left returns to the row, OK enters the action, Back returns to the row; Up ×7 returns to row 1, Up again reaches header Back, Down from Back returns to row 1. Built-in Clock Preview (`mode=preview entry=clock` … `reason=cancel`) returned with Preview focused. Right → Options, Down → Change, Down → the pane scrolls to In other apps · Use everywhere…; Left returns to the row; header Back closes to Home settings. | `J‑cb4327ef7b35` |
| 1 Cast2 fit at 4K | **PASS, at the resolution available.** Homeboard logs `dashboard=3840x2160 surface=3840x2160 hardware=true` and `root=3840x2160`, so it laid out on the native 4K surface; the picker sits inside the dashboard with nothing clipped: rows, badges, actions, footer and header Back all whole, and the detail pane's lower lines are reachable by scrolling. **Not native-resolution captures:** Cast2's HDMI output is 3840x2160@60, but every coordinator capture (check frames and `task chromecast:capture`, `J‑14fa05c1d3e5`) returns 1920×1080, Android's window size (`DisplayMetrics width=1920 height=1080 density=4.0`), so 4K pixel detail cannot be inspected from here. | `J‑acbe88d1c1d4` (display modes) |
| 1 Cast2 end state | **Matches the start.** Android settings identical (`J‑c37510fc93f4`); Homeboard's choice Clock and timer "Android decides" never changed; the coordinator's cleanup put YouTube back in front, as it was. | `J‑c37510fc93f4` |
| 2 Cast1 2.0× | **PASS.** Badges wrap below names at words ("Needs Use / everywhere"), status lines run to two lines, nothing is clipped. Right from Preview reaches the wrapped Options on the next line, Left goes back to Preview, Down from Preview moves to the Options line and Up from there returns to the first action line; Down then scrolls the pane to its end; Back returns to the row. Only the longest name is cut after two lines ("World Foundry Aquarium Scree…"). | `J‑87641b26e237` |
| 2 Font scale | Read 1.0 (`J‑ef90ec4c014a`), set 2.0 (`J‑76bede47c128`), back to 1.0 (`J‑ddac6938081b`), read 1.0 (`J‑4544840990a2`). | |
| 2 Cast1 1.0× | **PASS, no regression.** Same as round 3: Right/Left across the three actions on one line, Down to Change and Use everywhere…, pane end, last row, footer whole. | `J‑5e873b2457bb` |

Frames: [Cast2 start](2026-11-26-screensaver-list-cast1/r4-cast2-state-J-935f30f8c2ce/02-cast2-start-1-of-8.jpg),
[Cast2 sheet](2026-11-26-screensaver-list-cast1/r4-cast2-keys-J-cb4327ef7b35/contact-sheet.jpg),
[Cast1 2.0× Right to Options](2026-11-26-screensaver-list-cast1/r4-cast1-text20-J-87641b26e237/w4-right-reaches-wrapped-options-2.0.jpg),
[2.0× badge and status wrap](2026-11-26-screensaver-list-cast1/r4-cast1-text20-J-87641b26e237/w13-badge-and-status-wrap-2.0.jpg),
[1.0× sheet](2026-11-26-screensaver-list-cast1/r4-cast1-text10-J-5e873b2457bb/contact-sheet.jpg).

Recipes: `dev/device/screensaver-list-r4-{state,cast2-keys,cast1-text}.json`.

### Round 4 final device states

- **Cast1:** Homeboard choice Clock (On Home); timer "Android decides"; language "Follow device";
  font scale 1.0 (`J‑4544840990a2`); Android: WF Clocks, enabled, activate on sleep, idle
  600 000 ms, not running (`J‑bad79c4804f7`); [capture](2026-11-26-screensaver-list-cast1/r4-cast1-capture-J-37d8d34f3a01/final.jpg) `J‑37d8d34f3a01`.
- **Cast2:** as found. Homeboard choice Clock and timer "Android decides", unchanged; Android:
  Seahorse Grid, enabled, activate on sleep, idle 600 000 ms, sleep 3 600 000 ms, not running
  (`J‑c37510fc93f4`); YouTube in front (`J‑14fa05c1d3e5`).
- Both are free.
