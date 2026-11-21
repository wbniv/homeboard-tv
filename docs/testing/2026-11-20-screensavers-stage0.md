# Screensavers Stage 0: evidence so far

**Status:** read-only `screensaver-list` pass done on Cast1, Cast2 and the Pixel 8 Pro
(2026‑11‑21, 01:42 UTC). Nothing was changed on any device. The app-side probe
(`screensaver_probe`) and the `secure-settings-grant` workflow are not deployed, so
questions 2 to 5, 7, 8, 10 and 11 are still open. Recipe:
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

## Still open

Q2, Q3, Q4, Q5, Q7, Q8, Q10, Q11. They need the coordinator's `screensaver_probe` check option, and Q5 also needs
`secure-settings-grant` (commit `4b986f92`, not yet merged or deployed; the deploy needs Will at a sudo terminal).
