# Screensavers Stage 5 on Cast1: a World Foundry app's own preview screen

**Date:** 2026‑11‑26, two sessions: 20:50–21:20 Bangkok (13:50–14:20 UTC, the first run, which
read the wrong entry) and 21:10–22:08 Bangkok (14:10–15:08 UTC, the diagnosis and Verification 3).
**Device:** Cast1 (`chromecast-test-01`, Chromecast HD, Android 14). **Builds:** Homeboard 0.1.165
for the first run; 0.1.167 and 0.1.169 for the diagnosis; **0.1.171** (`net.biohack.homeboard.debug`,
frozen APK sha256 `a12f4006…`, with the permanent diagnostic `a636047`) for Verification 3. All
three later builds came from the shared `main` working tree, which also held another session's
uncommitted dashboard work (a combined build, approved for Cast1). None of them carries the
top‑row change (`d74d9b8`, `18b5fae`, `a08cf4e`, merged at 22:04, after 0.1.171 was built at
21:34); Home's top row in these frames still has the moon and the gear. WF Clocks is the build the
coordinator installed last (sha256 `4ab6bddd…` from WorldFoundry `42533e702`, which contains
`5a117e85d`); its manifest carries the same `PREVIEW_ACTIVITY` as `89a31982…`. It is Android's
selected screensaver. **Covers:** Verification 3 of the
[companion contract](../plans/2026-11-26-screensaver-companion-contract.md). Run by the
coordinator only; no direct device access.

## Result

| Part of Verification 3 | Result |
|---|---|
| A World Foundry entry previews through the app's preview screen | **PASS.** WF Clocks, on page 3 of the list, reads "Plays on Home, or in every app" with **Use this screensaver** and **Preview**. Preview plays WF Clocks' own `ScreensaverPreviewActivity` (`screensaver started mode=preview … player=app`), and Back returns to the list (`reason=left`). [Frame 04](2026-11-26-screensaver-stage5-cast1/preview-J-4f2448999362/04-preview-playing.jpg). |
| It plays on Home through that screen | **PASS.** Chosen with **Use this screensaver** ([frame 08](2026-11-26-screensaver-stage5-cast1/preview-J-4f2448999362/08-wf-clocks-chosen.jpg)), Home timer 1 min ([frame 04](2026-11-26-screensaver-stage5-cast1/idle-J-7aa301eabf78/04-one-minute-set.jpg)); Home idled and played the app's screen 60.4 s after the last key (`mode=idle … player=app`, [frame 07](2026-11-26-screensaver-stage5-cast1/idle-J-7aa301eabf78/07-app-screen-idle.jpg)). |
| **Open ‹app› settings** opens the chooser | **Not testable.** WF Clocks declares no `settingsActivity` yet (WorldFoundry chooser, separate T4 item), so the button is absent. |
| A ringing timer brings Homeboard back while the app's screen is in front | **PASS.** A 1‑minute kitchen timer was started, then WF Clocks' Preview; 61.6 s after the timer started, Homeboard closed the app's screen (`reason=alarm`) and showed **Timers & alarms** ([frame 06](2026-11-26-screensaver-stage5-cast1/alarm-J-a7f7ee6a393a/06-app-screen-before-ring.jpg) → [frame 07](2026-11-26-screensaver-stage5-cast1/alarm-J-a7f7ee6a393a/07-timer-screen-after-ring.jpg)). |

The ringing timer was checked over Preview, not over Home idle: with a 1‑minute Home timer a
1‑minute kitchen timer started from Home always rings before Home's idle minute is up, and both
paths close the app's screen through the same receiver (`MainActivity.screensaverAlarm`).

## Root cause of the first run's FAIL

**There was no Homeboard defect. The first run read the wrong row.** Page 2's "Clock" is the old
release Homeboard's Dream (`net.biohack.homeboard/com.gathin.tvhome.ClockDream`, versionCode 92,
which declares no preview screen), not WF Clocks. WF Clocks' Dream is labelled **"WF Clocks"**
(`string/clocks_dream_name` in both APKs, read with `aapt2 dump resources`) and sorts onto page 3,
after Seahorse Grid. The "label Clock" in the first run came from Clocks' content settings
(`"label": "Clock"` is a settings field in `screensaver-list`), not from the Dream.

How it was found, in three device runs:

1. **Diagnostic build 0.1.167** (job `J‑f0c1d8cc4a3a`) logged a `preview screen dropped … reason=`
   line for every rejected screen, and kept quiet for services without the key. Opening the list
   produced **no** line at all. So either no service carried the key, or the screen was accepted.
2. **Probe build 0.1.169** (job `J‑44c1d84a80ab`, a temporary line, since removed) logged each
   Dream's metadata key set from `queryIntentServices` and from `getServiceInfo(GET_META_DATA)`.
   For Clocks the two agree, and the key set's hash is `0x10f495fc`, the sum of the Java hashes of
   exactly `android.service.dream`, `…SETTINGS_AUTHORITY` and `…PREVIEW_ACTIVITY`. The metadata
   reached Homeboard and was accepted. `net.biohack.homeboard`, `dreamx` and Aerial Views have
   `metaData == null`.
3. **Page 3** (job `J‑4f3caffeca1e`): WF Clocks listed with its preview screen
   ([frame 06](2026-11-26-screensaver-stage5-cast1/page3-J-4f3caffeca1e/06-page3-wf-clocks.jpg)).

With the permanent diagnostic, build 0.1.171 now says so in the log:

```
11-26 21:36:52.022  5931  5931 I HomeboardScreensaver: preview screen kept package=org.worldfoundry.game.clocks service=org.worldfoundry.screensaver.ClocksDreamService screen=org.worldfoundry.game.clocks/org.worldfoundry.screensaver.ScreensaverPreviewActivity
```

No unit test was added: nothing in Homeboard's logic was wrong, so there is no regression to
guard. The permanent `kept` / `dropped` line is the guard against the next misreading.

**For a design look (not changed):** two list rows read "Clock", the built‑in one and the old
release Homeboard's Dream, which is how the wrong row was read. Separately, **Change** under "On
Home" while WF Clocks is in use opens **Screensaver · Options** headed "Clock · Built in", with
the built‑in clock's backdrop settings above the Home timer ([frame 04](2026-11-26-screensaver-stage5-cast1/idle-J-7aa301eabf78/04-one-minute-set.jpg)).

## Key paths (0.1.171 on Cast1)

The list, 4 per page: page 1 Clock (built in), Home background, Aerial Views, Ambient Mode;
page 2 Clock (old release Homeboard), Colors, Homeboard TV (dashboard proof), Seahorse Grid;
page 3 **WF Clocks**, World Foundry Aquarium Screensaver.

| Goal | Keys |
|---|---|
| WF Clocks, from Home (built‑in Clock in use) | Menu, Down; Down ×4 (Next ›), OK; Down ×4 (‹ Previous), Right (Next ›), OK |
| Preview / Use, from WF Clocks | Right (Use this screensaver), Right (Preview), OK |
| Home timer, WF Clocks in use | Menu, Down, Right (Preview), Down (Change), OK; Right (1), OK; Left (Android decides), OK |
| 1‑minute kitchen timer | Back (Home), Down (Timers widget), OK; Down, Right, Up (1 Minutes), OK |
| Back to the built‑in Clock, WF Clocks in use | Menu, Down; Down ×2 (‹ Previous), OK; Down ×4 (‹ Previous), OK; Right (Use this screensaver), OK |

A check can start on All Apps instead of Home; a first Back returns to Home.

## Jobs

| Dir | Job | What happened |
|---|---|---|
| `list-J-319257867f88` | `J‑319257867f88` | First run, before: WF Clocks selected, idle time 600 000 ms, not running |
| `map-1` | `J‑f43c1d1151ff` | First run: focus map of pages 1–2 (page 2's "Clock" misread as WF Clocks) |
| — | `J‑4fdf5684a701` | Inspect: WF Clocks installed (versionCode 1), its Dream listed |
| `list-J-361f2ba3ceb1` | `J‑361f2ba3ceb1` | First run, after: unchanged |
| — | `J‑544da0e96aef` | Deploy 0.1.167 (diagnostic) |
| `diag-J-f0c1d8cc4a3a` | `J‑f0c1d8cc4a3a` | Hypothesis 1: no `dropped` line for any Dream |
| `list-J-3b525adbe466` | `J‑3b525adbe466` | WF Clocks selected, idle time 600 000 ms, 9 Dream providers |
| — | `J‑855cc9a3cd76` | Deploy 0.1.169 (metadata probe) |
| `probe-J-44c1d84a80ab` | `J‑44c1d84a80ab` | Hypothesis 2: Clocks' metadata carries `PREVIEW_ACTIVITY` |
| `page3-J-4f3caffeca1e` | `J‑4f3caffeca1e` | Hypothesis 3: WF Clocks on page 3 with Preview |
| — | `J‑6aab1ea2ad3f` | Deploy 0.1.171 (final diagnostic) |
| `preview-J-4f2448999362` | `J‑4f2448999362` | Preview plays the app's screen; WF Clocks chosen for Home |
| `change-map-J-19707368dd2b` | `J‑19707368dd2b` | Map: Right, Down reaches Change |
| — | `J‑738cb7549311` | Map attempt; Right ×2 on All Apps then OK opened another app, so the key helper stopped (`plugin-error`); automatic readd `J‑4418ed4bf5b3` returned Cast1 to ready |
| `idle-J-7aa301eabf78` | `J‑7aa301eabf78` | Home timer 1 min, Home idle plays the app's screen, Home timer back to Android decides |
| `timer-map-J-7152e050345b`, `timer-map2-J-2c8b7dac8922` | `J‑7152e050345b`, `J‑2c8b7dac8922` | Maps: Home focus and the Timers screen |
| `alarm-J-a7f7ee6a393a` | `J‑a7f7ee6a393a` | 1‑minute timer rings over WF Clocks' Preview; `reason=alarm`, timer screen |
| `restore-map-J-d99fa2b0a6af` | `J‑d99fa2b0a6af` | Map: ‹ Previous lands on the page's first entry |
| `restore-J-d08b70731e83` | `J‑d08b70731e83` | Homeboard's choice back to the built‑in Clock |
| `list-J-3120a242236a` | `J‑3120a242236a` | After: WF Clocks selected, idle time 600 000 ms, not running |
| `capture-J-07229c1d2c7b` | `J‑07229c1d2c7b` | Final capture: Google TV Home, no screensaver or timer screen up |

Recipes: `dev/device/screensaver-stage5-{diag,page3,preview,change-map,idle,timer-map,timer-map2,alarm,restore-map,restore}.json`.

## Final Cast1 state

- **Homeboard's choice:** Clock (built in), **In use** ([frame 04](2026-11-26-screensaver-stage5-cast1/restore-J-d08b70731e83/04-clock-in-use.jpg); Undo banner "Screensaver: Clock" in frame 99).
- **Home timer:** "Android decides · Homeboard runs no timer of its own" (same frame; set back in `J‑7aa301eabf78` frame 11).
- **Kitchen timers:** none running. The test's 1‑minute timer remains as a fourth **Done** entry
  (Home's timer card reads "Done · 1 more"), like the three Done entries already there.
- **Android:** screensaver WF Clocks (`org.worldfoundry.game.clocks/org.worldfoundry.screensaver.ClocksDreamService`),
  enabled, activate on sleep, idle time 600 000 ms, not running (`J‑3120a242236a`). Idle time was never changed.

Raw frames, logcat and score files stay on disk (see the folder's `.gitignore`); re-export with
`task chromecast:evidence JOB=<id> OUT=<dir>`.
