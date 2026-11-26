# Screensavers Stage 0: evidence so far

**Status:** read-only `screensaver-list` pass done on Cast1, Cast2 and the Pixel 8 Pro
(2026‑11‑21, 01:42 UTC). Nothing was changed on any device. The coordinator's
`screensaver_probe` option and the Homeboard probe screen (0.1.117) now run on Cast1; see
the Cast1 probe results below. Q7 is answered on Cast1, including where wake returns
([record](2026-11-26-screensaver-q7.md)). Recipe:
[2026-11-20-screensavers-stage0-probe.md](../plans/2026-11-20-screensavers-stage0-probe.md).

| Device | Job | Evidence |
|---|---|---|
| Cast1 (`chromecast-test-01`) | J‑79de628b46f1 | [screensaver.json](2026-11-20-screensavers-stage0/cast1-list/screensaver.json) |
| Cast2 (`chromecast-test-02`) | J‑8fd84c39b1c3 | [screensaver.json](2026-11-20-screensavers-stage0/cast2-list/screensaver.json) |
| Pixel 8 Pro (`phone-pixel-8-pro`) | J‑d9c76acb0f1a | [screensaver.json](2026-11-20-screensavers-stage0/pixel-list/screensaver.json) |

## Q1: current values of the four settings

| Setting | Cast1 | Cast2 | Pixel |
|---|---|---|---|
| Selected screensaver | World Foundry Seahorse Grid | World Foundry Seahorse Grid | World Foundry Planted Tank |
| `screensaver_enabled` | 1 | 1 | 1 |
| `screensaver_activate_on_sleep` | 1 | 1 | 1 |
| Idle timeout (`screen_off_timeout`) | 600 000 ms (10 min) | 600 000 ms (10 min) | 300 000 ms (5 min) |
| `sleep_timeout` | −1 (never) | 3 600 000 ms (60 min) | −1 (never) |
| `stay_on_while_plugged_in` | 0 | 3 | 0 |

`sleep_timeout` is returned by `screensaver-list`, so no change to `platform-facts` is needed.
Cast1 had a screensaver **running** when it was read (`running_state: running`).

## Q6: installed screensavers and settings documents

- Both TVs list the Google Backdrop, Android Colors, Homeboard's debug `ScreensaverDream` and three World Foundry
  savers (Aquarium, Planted Tank, Seahorse Grid). Cast1 also has Aerial Views and the release `ClockDream`.
- Each World Foundry saver offers a settings document. On the TVs it has one setting, *Orientation*
  (Landscape, Sensor landscape, Follow device).
- On the Pixel the Planted Tank settings document is schema 2 and has many settings (seed, water type, growth speed).

## Q9: Pixel lists screensavers installed from outside the system

**Yes.** The Pixel lists all three World Foundry savers beside the system ones, so sideloaded or Play-installed
screensavers show up in `screensaver-list` on Android 17.

## Cast1 probe results (0.1.117, 2026‑11‑21)

Jobs J‑b5722b5b02ce (read-only), J‑6bceca4edf0c (`dream_settings`) and J‑527ebec5f1fa (`screensaver_now`);
evidence under [2026-11-21-screensaver-probe](2026-11-21-screensaver-probe/).

| Q | Result on Cast1 |
|---|---|
| Q2 | **No.** `android.settings.DREAM_SETTINGS` has no handler (`ActivityNotFoundException`), so Homeboard cannot open a system screensaver picker. |
| Q3 | **No.** `com.android.systemui/.Somnambulator` is not present, so there is no explicit "start screensaver now" component on this Google TV. |
| Q4 | **Yes.** All screensaver keys are readable without any extra permission. |
| Q5 | **Yes.** With the grant, `screensaver_components` and `screen_off_timeout` both write and read back, and restore to the originals (J‑99ee751a69d3 grant, J‑f4759881b86a write test, J‑da27c5b14236 revoke; grant confirmed `false` afterwards). |
| Q10 | Pass. |
| Q11 | Partial (`can-request`, `manage-sources` pass; `offer` not run, no frozen test APK). |

## Q7 on Cast1 (0.1.163, 2026‑11‑26)

With Android's idle time shortened to 3 min for the run, Android started its own screensaver
over Homeboard's Home and over Homeboard's own Clock screensaver. Homeboard's Clock closes
itself as Android's comes to the front, so a key wakes the TV to Homeboard's Home where it was
left, and Homeboard's Home timer then runs again (run 4, with the coordinator's
`"foreground": "observe"`). Jobs and frames are in the [Q7 record](2026-11-26-screensaver-q7.md).

## Still open

Q8 (not built in the probe) and Q11 `offer`.
