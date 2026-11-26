# Screensavers Stage 0 Q7 on Cast1: Android's screensaver over Homeboard's

**Date:** 2026‑11‑26, 10:48–11:12 Bangkok (03:48–04:12 UTC). **Device:** Cast1
(`chromecast-test-01`, Chromecast HD, Android 14). **Build:** Homeboard 0.1.163
(`net.biohack.homeboard.debug`, frozen APK sha256 `c6046f34…a0c03f`, reused as installed).
**Covers:** [Stage 0](2026-11-20-screensavers-stage0.md) question 7, as far as the
coordinator's `check` workflow can see it. Run by the coordinator only; no direct device access.

## Result

| Q7 part | Result |
|---|---|
| Does Android's screensaver start while Homeboard is in front? | **Yes.** With Homeboard's Home on screen and no input, Android started its selected screensaver (World Foundry Clocks) when its idle time ran out (run 2). |
| Does it start over Homeboard's **own** screensaver? | **Yes.** Homeboard's Clock played from about 1 min after the last key; Android's Clocks replaced it between 2 min 53 s and 3 min 33 s, with Android's idle time at 3 min (run 3). |
| What does Homeboard's activity see? | Its `ScreensaverActivity` is **paused** under the screensaver (`mLastPausedActivity`); whether it is also stopped was not recorded. Homeboard's process stayed alive. |
| Where does the TV return on wake? | **Not observed.** The `check` workflow ends as soon as another app is in front, and its cleanup force-stops Homeboard and reopens Google TV's home, so nothing after the screensaver starts is Homeboard's own behaviour. |

Run 3, from the [contact sheet](2026-11-26-screensaver-q7/run-3-check/contact-sheet.jpg):

| Time after the last key | On screen |
|---|---|
| 0 min 4 s | Home; Undo banner "Screensaver on Home: 1 min" |
| 0 min 43 s | Home |
| 1 min 13 s, 2 min 3 s | Homeboard's Clock screensaver |
| 2 min 53 s | Homeboard's Clock screensaver |
| 3 min 33 s, 4 min 33 s | World Foundry Clocks, Android's screensaver |

So in the Home‑only mode two timers run: Homeboard's screensaver plays first, and Android's
replaces it when Android's own idle time is reached. Homeboard holds no wake lock and does not
keep the screen on, which is consistent with this.

## Jobs

Android's idle time was set to 3 min for each run (`screensaver:configure IDLE_MINUTES=3`),
because a `check` job lasts at most 600 s, and put back to 10 min afterwards. Every run ended
with Homeboard's timer on "Android decides". The final read (04:11 UTC) shows idle time 600 000 ms
and World Foundry Clocks selected, as before.

| Dir | Job | What happened |
|---|---|---|
| `attempt-1` | `J‑1c533ea60867` | Failed before any key: see below |
| `reset-1` | `J‑5d0d51c28792` | Reset; the key path missed, nothing saved |
| `run-2-check` | `J‑fb2117c03663` | Key path missed the timer; Android's screensaver started over Homeboard's Home |
| `reset-2` | `J‑4d7b4f3973b3` | Reset; opened and closed the World clock full screen, nothing saved |
| `map-1`, `map-2` | `J‑b7032ded88dd`, `J‑7bf5795463a5` | Focus maps; found the path Menu → Down → Right → Right → Down to **Change** |
| `run-3-check` | `J‑614c522a316c` | The result above |
| `reset-3` | `J‑d5af3286f338` | Timer back to "Android decides" (Undo banner shown) |

Recipes: [`screensaver-q7-check.json`](../../dev/device/screensaver-q7-check.json),
[`screensaver-q7-restore.json`](../../dev/device/screensaver-q7-restore.json) and
[`screensaver-q7-map.json`](../../dev/device/screensaver-q7-map.json). The first key path
assumed where focus starts on Home, which changes between launches; the Menu key path does not.

## Attempt 1: launching Homeboard out of a screensaver

Cast1 was showing the Clocks screensaver when attempt 1 began (the configure job's read at
03:48:43 UTC says `running`). The check sent `KEYCODE_WAKEUP` and started Homeboard at once;
6 s later Google TV's home was in front and the job failed `target-left-foreground`. Android's
exit records show no crash: Homeboard's only exit is the cleanup's force stop. With a separate
key press first (`J‑8629868e6487`), the next launch worked. The order of events between the wake
and the launch was not recorded, so why Google TV's home ended up in front is not known.
Reported to the coordinator session.

## Still open

- Where the TV returns on wake, and what Homeboard does then. Needs a coordinator check that
  keeps watching after another app comes to the front, or a person with a remote. Requested
  from the coordinator session on 2026‑11‑26: an opt‑in that records leaving the foreground
  instead of failing, the top activity and Dream state at each capture, and the app's log for
  the whole run.
