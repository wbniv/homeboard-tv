# Screensavers Stage 5 on Cast1: a World Foundry app's own preview screen

**Date:** 2026‑11‑26, about 20:50–21:20 Bangkok (13:50–14:20 UTC). **Device:** Cast1
(`chromecast-test-01`, Chromecast HD, Android 14). **Build:** Homeboard 0.1.165
(`net.biohack.homeboard.debug`, frozen APK sha256 `bb48861e…`, from `main` with Stage 5 at
`2395376` and `a847020`, reused as installed). WF Clocks sha256 `89a31982…` from WorldFoundry
`e4c3bd4ec`, Android's selected screensaver; Aquarium and Seahorse Grid are older builds without
the metadata. **Covers:** Verification 3 of the
[companion contract](../plans/2026-11-26-screensaver-companion-contract.md). Run by the
coordinator only; no direct device access.

## Result

| Part of Verification 3 | Result |
|---|---|
| A World Foundry entry previews through the app's preview screen | **FAIL.** Homeboard lists WF Clocks as an app without a preview screen: "Needs Use everywhere", "Android runs it, in every app", "No preview: Android gives apps no way to start another app's screensaver", and only **Use everywhere…** ([frame 07](2026-11-26-screensaver-stage5-cast1/map-1/07-wf-clocks-entry.png)). |
| It plays on Home through that screen | **Not run.** Without a preview screen the entry cannot be chosen for Home (it routes to Use everywhere). |
| **Open ‹app› settings** opens the chooser | **Not testable.** WF Clocks declares no `settingsActivity` yet (WorldFoundry chooser, separate T4 item); the button is absent in frame 07, as the code shows it only for a declared settings link. |
| A ringing timer brings Homeboard back while the app's screen is in front | **Not run**, for the same reason as the first row. |

Stopped after the mapping run, as the first behaviour already failed and the cause has to be
found before another device run is worth it.

### What was ruled out

- **The installed WF Clocks has the keys.** `/home/will/tmp/clocks-release-89a31982.apk` (the
  hash the coordinator installed) was dumped with `aapt2 dump xmltree` in the build container: the
  `ClocksDreamService` `<service>` carries `org.worldfoundry.screensaver.PREVIEW_ACTIVITY` =
  `org.worldfoundry.screensaver.ScreensaverPreviewActivity`, and that activity is declared with
  `android:exported="true"` (process `:preview`). WorldFoundry `e4c3bd4ec` contains `5a117e85d`.
- **The installed Homeboard has Stage 5.** The job reused APK `bb48861e…` (exact match), whose dex
  contains `org.worldfoundry.screensaver.PREVIEW_ACTIVITY`, `PLAY_SCREENSAVER` and `player=app`.

So `SystemScreensaver.appScreen` returns null on the device for a manifest it should accept. It
logs nothing when it drops a screen, so this run cannot say which check failed: `si.metaData`
missing from `queryIntentServices(…, GET_META_DATA)`, `getActivityInfo` refusing or reporting the
activity as not exported or disabled, or a permission check. Next step: log the reason in
`appScreen` (a tagged line per dropped screen), rebuild, and run this check again.

## Key path (for the rerun)

From [map‑1](2026-11-26-screensaver-stage5-cast1/map-1/contact-sheet.jpg): Menu, Down opens
**Home settings · Screensaver** with focus on the in‑use entry (Clock). The list on Cast1 reads,
4 per page:

| Page | Entries |
|---|---|
| 1 | Clock (built in, In use), Home background, Aerial Views, Ambient Mode |
| 2 | Clock (**WF Clocks**), Colors, Homeboard TV, Seahorse Grid |

From Clock, Down ×4 focuses **Next ›**; OK there opens page 2 with focus on WF Clocks.
Aquarium did not appear on pages 1–2 (not reached in this run).

## Jobs

| Dir | Job | What happened |
|---|---|---|
| `list-J-319257867f88` | `J‑319257867f88` | Before: WF Clocks selected, idle time 600 000 ms, not running |
| `map-1` | `J‑f43c1d1151ff` | Focus map of the Screensaver list, pages 1–2; nothing saved |
| — | `J‑4fdf5684a701` | Inspect: WF Clocks installed (versionCode 1), its Dream listed |
| `list-J-361f2ba3ceb1` | `J‑361f2ba3ceb1` | After: WF Clocks selected, idle time 600 000 ms, not running |

## Final Cast1 state

Unchanged by this run, which saved nothing: Homeboard's choice is Clock (**In use**, frame 12 of
map‑1), its Home timer "Android decides" ("Homeboard runs no timer of its own", same frame), no
kitchen timer running (the Home timer cards read **Done**, frame 15), Android's idle time
600 000 ms and Android's screensaver WF Clocks (job `J‑361f2ba3ceb1`).

Recipe: [`screensaver-stage5-map.json`](../../dev/device/screensaver-stage5-map.json). Raw frames,
logcat and score files stay on disk (see the folder's `.gitignore`); re-export with
`task chromecast:evidence JOB=<id> OUT=<dir>`.
