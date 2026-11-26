# Screensavers Stage 0 Q7 on Cast1: Android's screensaver over Homeboard's

**Date:** 2026‑11‑26, 10:48–11:12 Bangkok (03:48–04:12 UTC); the wake, run 4, 20:18–20:24
Bangkok (13:18–13:24 UTC). **Device:** Cast1 (`chromecast-test-01`, Chromecast HD, Android 14).
**Build:** Homeboard 0.1.163 (`net.biohack.homeboard.debug`, frozen APK sha256 `c6046f34…a0c03f`,
reused as installed). For run 4 Android's screensaver was WF Clocks built from WorldFoundry
`e4c3bd4ec` (sha256 `89a31982…`, installed by job `J‑664b26cd81ec`). **Covers:**
[Stage 0](2026-11-20-screensavers-stage0.md) question 7 in full: runs 1–3 with the coordinator's
`check`, run 4 with its `"foreground": "observe"` option (coordinator 0.1.240). Run by the
coordinator only; no direct device access.

## Result

| Q7 part | Result |
|---|---|
| Does Android's screensaver start while Homeboard is in front? | **Yes.** With Homeboard's Home on screen and no input, Android started its selected screensaver (World Foundry Clocks) when its idle time ran out (run 2). |
| Does it start over Homeboard's **own** screensaver? | **Yes.** Homeboard's Clock played from about 1 min after the last key; Android's Clocks replaced it between 2 min 53 s and 3 min 33 s, with Android's idle time at 3 min (run 3). |
| What does Homeboard's activity see? | Its `ScreensaverActivity` is paused under the screensaver (run 3) and then **closes itself**: run 4 logs `screensaver stopped mode=idle entry=clock reason=left` at 212.3 s, as Android's screensaver came to the front at 213.7 s. Homeboard's Home is left underneath. Homeboard's process stayed alive (pid 12670 throughout). |
| Where does the TV return on wake? | **Homeboard's Home, where it was left** (run 4). The wake key's press went to Android's screensaver, which ended; its release reached Homeboard's Home and changed nothing on screen. Homeboard's own screensaver does not come back at wake. |
| What does Homeboard do then? | **Its Home timer starts again.** Homeboard's Clock played 64 s after the next key (run 4: second key at 280 s, `screensaver started` at 344.0 s). |

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

## Run 4: the wake

Same key path and timer as run 3, with `"foreground": "observe"` so the job kept watching after
Android's screensaver came to the front, a wake key at 245 s and a second key at 280 s
([recipe](../../dev/device/screensaver-q7-wake.json),
[contact sheet](2026-11-26-screensaver-q7/run-4-wake/contact-sheet.jpg)). Times are seconds
after the job started; the last key of the setup path was at 28 s.

| Time | What happened | Evidence |
|---|---|---|
| 92.1 s | Homeboard's Clock starts (`screensaver started mode=idle entry=clock`) | `logcat.txt`; frames 05 and 06 |
| 212.3 s | Homeboard's Clock closes itself (`reason=left`) | `logcat.txt` |
| 213.7 s | Android's screensaver, WF Clocks' `DreamActivity`, is in front | `foreground-timeline.json`; frame 07 |
| 245.0 s | Wake key pressed: its press goes to WF Clocks, its release to Homeboard | `input-timeline.json` (`focus`) |
| 247–270 s | Homeboard's Home (`MainActivity`), unchanged from before | frames 08–11 |
| 280 s | Second key goes to Homeboard's Home; nothing visible changes | `input-timeline.json`; frames 12–14 |
| 344.0 s | Homeboard's Clock starts again, 64 s after that key | `logcat.txt`; frame 15 |

The job's `foreground_left: true` records that Homeboard was not in front the whole time; the
job itself completed. The log lines, foreground transitions and wake-key rows are excerpted in
[`key-evidence.txt`](2026-11-26-screensaver-q7/run-4-wake/key-evidence.txt); the raw files are
re-exported with `task chromecast:evidence JOB=J-9057cc5effc7 OUT=<dir>`.

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
| `run-4-wake` | `J‑9057cc5effc7` | The wake, above. Before it, a key press (`J‑2ee733fc1cf2`) in case Cast1 was dreaming; after it, a press (`J‑f462f35676a6`) and the reset (`J‑f829c589a398`) |

Android's idle time was put back to 10 min after run 4 as well; the final read (13:29 UTC)
shows idle time 600 000 ms, WF Clocks selected and not running.

Recipes: [`screensaver-q7-check.json`](../../dev/device/screensaver-q7-check.json),
[`screensaver-q7-wake.json`](../../dev/device/screensaver-q7-wake.json),
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
Reported to the coordinator session, which fixed the launch in coordinator 0.1.239 and verified
it on Cast1 with check `J‑5527a436fd1c`, started during the Clocks Dream (2026‑11‑26).

## Still open

Nothing for Q7. The wake half needed a check that keeps watching after another app comes to the
front; the coordinator added it on request (`"foreground": "observe"`, 0.1.240) and run 4 used it.
