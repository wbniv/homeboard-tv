# Home's top row on Cast1: Start screensaver and Homeboard settings

**Date:** 2026‑11‑26, 22:14–22:37 Bangkok (15:14–15:37 UTC). **Device:** Cast1
(`chromecast-test-01`, Chromecast HD, Android 14). **Build:** Homeboard **0.1.172**
(`net.biohack.homeboard.debug`, frozen APK sha256 `d54de17d…`), the first deployed build with
the top‑row change (`d74d9b8`, `18b5fae`, `a08cf4e`). It was built from the shared `main` working
tree, which also held another session's uncommitted work (a combined build, approved for Cast1).
Deploy job `J‑129e4655aaea`. WF Clocks `4ab6bddd…` is installed and is Android's selected
screensaver. **Covers:** Verification 1–8 of the
[top‑row plan](../plans/2026-11-26-top-row-start-screensaver.md). Run by the coordinator only; no
direct device access.

## Result

| Step | Result |
|---|---|
| 1 `task check` and the two tests | **PASS.** `StartScreensaverTest` (5 cases) and `HomeSettingsMenuTest` (3 cases) pass, with 726 unit tests, 0 failures, plus lint ([deploy log](2026-11-26-top-row-cast1/deploy-0.1.172.log)). |
| 2 Two buttons, focus order on Home | **PASS.** Exactly two round buttons right of the world clock, the moon with a play mark and the sliders. Clock → Right → Start screensaver → Right → Homeboard settings → Right → All Apps; Left walks Homeboard settings → Start screensaver → clock. |
| 3 Start with Homeboard's Clock | **PASS.** `screensaver started mode=now entry=clock`, the Clock in front; one Back gives `reason=key` and Home with Start screensaver focused. |
| 4 Start with WF Clocks | **PASS.** WF Clocks' preview screen in front (`… player=app`); Back gives `reason=left` and Home with Start screensaver focused. |
| 5 Hold OK on Start (D1) | **PASS.** Hold (1.1 s) opens Home settings · Screensaver; Back returns to Home with Start screensaver focused. |
| 6 Device settings on page 1 | **PASS.** Device settings is the left arm of Home settings page 1. Choosing it opens `com.android.tv.settings/.MainSettings` in front; Back returns to Homeboard. The radial menu chooses by direction, so it was chosen with Left, not OK. |
| 7 All Apps | **PASS.** The same two buttons. Up from the apps reaches Homeboard settings; Left → Start screensaver → clock; Right → Start screensaver → Homeboard settings; Left from the clock returns to Home. One difference by design: Right on Homeboard settings stays put in All Apps (the code advances only on Home). |
| 8 Thai | **PARTIAL.** With ไทย selected, the Device settings entry reads **การตั้งค่าอุ…** (truncated like "Device se…" in English), and Home settings is **การตั้งค่าหน้าหลัก**. The spoken descriptions could not be read on the device: no coordinator workflow returns a TV view's `contentDescription` (the only `uiautomator dump`, `window_dump`, runs for touch‑interactive phone jobs and is not saved). Instead, the APK carries the Thai strings the buttons use for `setContentDescription`: เริ่มโปรแกรมรักษาหน้าจอ and การตั้งค่า Homeboard. |

The read‑only short menu (Device settings and About only, for a dashboard saved by a newer
Homeboard) is not testable on Cast1 without a layout from a newer build. It is covered only by
`HomeSettingsMenuTest.aNewerDashboardOpensOnlyDeviceSettingsAndAbout`.

## Evidence

Logs from 0.1.172 (`logcat.txt` of each job, git‑ignored):

```
11-26 22:22:46.909 21418 21418 I HomeboardScreensaver: screensaver started mode=now entry=clock
11-26 22:23:00.254 21418 21418 I HomeboardScreensaver: screensaver stopped mode=now entry=clock reason=key
11-26 22:25:04.120 22054 22054 I HomeboardScreensaver: screensaver started mode=now entry=org.worldfoundry.game.clocks/org.worldfoundry.screensaver.ClocksDreamService player=app
11-26 22:25:17.039 22054 22054 I HomeboardScreensaver: screensaver stopped mode=now entry=org.worldfoundry.game.clocks/org.worldfoundry.screensaver.ClocksDreamService player=app reason=left
```

Step 6 foreground (`device-settings-J-2423c5943d8e/foreground-timeline.json`):

```
topResumedActivity=… net.biohack.homeboard.debug/com.gathin.tvhome.MainActivity t5724
topResumedActivity=… com.android.tv.settings/.MainSettings t5724
topResumedActivity=… net.biohack.homeboard.debug/com.gathin.tvhome.MainActivity t5724
```

Step 8 strings in the deployed APK (`aapt2 dump resources`):

```
resource 0x7f11012f string/hb_device_settings        () "Device settings"     (th) "การตั้งค่าอุปกรณ์"
resource 0x7f110202 string/hb_homeboard_settings     () "Homeboard settings"  (th) "การตั้งค่า Homeboard"
resource 0x7f11032e string/hb_ss_start_screensaver   () "Start screensaver"   (th) "เริ่มโปรแกรมรักษาหน้าจอ"
```

Focus is easiest to see in each job's `top-row-strip.jpg` (the top row cut from every frame).

| Step | Frames |
|---|---|
| 2 | [focus contact sheet](2026-11-26-top-row-cast1/focus-J-7d3be1def917/contact-sheet.jpg), [strip](2026-11-26-top-row-cast1/focus-J-7d3be1def917/top-row-strip.jpg); [clock](2026-11-26-top-row-cast1/start-clock-J-8e1e823b8703/01-world-clock.jpg) → [Start](2026-11-26-top-row-cast1/start-clock-J-8e1e823b8703/02-start-focused.jpg), [strip](2026-11-26-top-row-cast1/start-clock-J-8e1e823b8703/top-row-strip.jpg) |
| 3 | [Clock in front](2026-11-26-top-row-cast1/start-clock-J-8e1e823b8703/05-clock-screensaver.jpg), [Home, Start focused](2026-11-26-top-row-cast1/start-clock-J-8e1e823b8703/07-home-start-focused.jpg) |
| 4 | [WF Clocks in use](2026-11-26-top-row-cast1/start-app-J-8b0200787a5e/02-wf-clocks-in-use.jpg), [app preview screen](2026-11-26-top-row-cast1/start-app-J-8b0200787a5e/04-app-preview-screen.jpg), [after Back](2026-11-26-top-row-cast1/start-app-J-8b0200787a5e/06-home-after-back.jpg) |
| 5 | [after hold](2026-11-26-top-row-cast1/hold-allapps-J-12df26e9b58c/02-screensaver-settings-after-hold.jpg), [after Back](2026-11-26-top-row-cast1/hold-allapps-J-12df26e9b58c/03-home-start-focused.jpg) |
| 6 | [page 1](2026-11-26-top-row-cast1/device-settings-J-2423c5943d8e/01-device-settings-left.jpg), [Android Settings](2026-11-26-top-row-cast1/device-settings-J-2423c5943d8e/02-android-settings.jpg), [after Back](2026-11-26-top-row-cast1/device-settings-J-2423c5943d8e/03-back-in-homeboard.jpg) |
| 7 | [contact sheet](2026-11-26-top-row-cast1/hold-allapps-J-12df26e9b58c/contact-sheet.jpg), [strip](2026-11-26-top-row-cast1/hold-allapps-J-12df26e9b58c/top-row-strip.jpg) |
| 8 | [Home in Thai](2026-11-26-top-row-cast1/thai-J-d139082a13b5/03-home-thai.jpg), [page 1 in Thai](2026-11-26-top-row-cast1/thai-J-d139082a13b5/04-device-settings-thai.jpg), [crop](2026-11-26-top-row-cast1/thai-J-d139082a13b5/04-device-settings-thai-crop.jpg), [English again](2026-11-26-top-row-cast1/thai-J-d139082a13b5/07-english-again.jpg) |

## Key paths (0.1.172 on Cast1)

| Goal | Keys |
|---|---|
| Start screensaver from anywhere on Home | Back; Up ×4; Left ×3 (world clock); Right |
| Hold OK on Start | OK held 1.1 s (Homeboard's hold is 600 ms) |
| Device settings | Menu, Left |
| Language | Menu, OK (page 2), Right; then Up English, Right ไทย, Down Follow device, Left Back |

Before the test, the language was **Follow device** (marked Current), not English, and the
restore put it back to Follow device, which shows English on Cast1.

## Jobs

| Dir | Job | What happened |
|---|---|---|
| `deploy-0.1.172.log` | `J‑129e4655aaea` | Bump, `task check`, build, install 0.1.172 |
| `focus-J-7d3be1def917` | `J‑7d3be1def917` | Step 2 map: Up ×3 reaches Homeboard settings; Right advances; Left walks back |
| `start-clock-J-8e1e823b8703` | `J‑8e1e823b8703` | Steps 2–3: clock → Start → settings → Start; OK; Clock; Back |
| `start-app-J-8b0200787a5e` | `J‑8b0200787a5e` | Step 4: WF Clocks chosen; OK on Start; app screen; Back |
| `hold-allapps-J-12df26e9b58c` | `J‑12df26e9b58c` | Steps 5 and 7 |
| `device-settings-J-2423c5943d8e` | `J‑2423c5943d8e` | Step 6 |
| `language-map-J-46dd5fd3b6ed` | `J‑46dd5fd3b6ed` | Map of the Language menu (Down re‑chose Follow device, already current) |
| `thai-J-d139082a13b5` | `J‑d139082a13b5` | Step 8: ไทย, Home and page 1 in Thai, back to Follow device |
| `restore-J-34f147c1aa2e` | `J‑34f147c1aa2e` | Homeboard's choice back to the built‑in Clock |
| `list-J-a13eaab54922` | `J‑a13eaab54922` | After: WF Clocks selected, idle time 600 000 ms, not running |
| `capture-J-97bfb5348cd7` | `J‑97bfb5348cd7` | Final capture: Google TV Home, nothing playing |

Recipes: `dev/device/top-row-{focus,start-clock,start-app,hold-allapps,device-settings,language-map,thai,restore}.json`.

## Final Cast1 state

- **Homeboard's choice:** Clock (built in), **In use** ([frame](2026-11-26-top-row-cast1/restore-J-34f147c1aa2e/04-clock-in-use.jpg)).
- **Home timer:** "Android decides · Homeboard runs no timer of its own" (same frame; never changed).
- **Language:** Follow device, as found (English on Cast1; [frame](2026-11-26-top-row-cast1/thai-J-d139082a13b5/07-english-again.jpg)).
- **Kitchen timers:** none running (no timer was started).
- **Android:** screensaver WF Clocks (`org.worldfoundry.game.clocks/org.worldfoundry.screensaver.ClocksDreamService`),
  enabled, activate on sleep, idle time 600 000 ms, not running (`J‑a13eaab54922`); the idle time was never changed.
- **Screen:** Google TV Home after the coordinator's cleanup ([capture](2026-11-26-top-row-cast1/capture-J-97bfb5348cd7/final.jpg)).

Raw frames, logcat and score files stay on disk (see the folder's `.gitignore`); re-export with
`task chromecast:evidence JOB=<id> OUT=<dir>`.
