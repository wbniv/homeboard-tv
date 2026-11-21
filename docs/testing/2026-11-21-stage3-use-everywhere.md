# Screensavers Stage 3: Use everywhere on Cast1

**Date:** 2026‑11‑21, 11:00–11:20 Bangkok (04:00–04:20 UTC). **Device:** Cast1
(`chromecast-test-01`, Chromecast HD, Android 14). **Build:** Homeboard 0.1.116
(`net.biohack.homeboard.debug`, frozen APK sha256 `c7de6554…6785aa`). **Coordinator:** 0.1.211.
**Covers:** [plan](../plans/2026-11-20-screensavers.md) Verification 10 (Cast1 half) and the
Cast1 part of Verification 12. Approved by Will on 2026‑11‑21 ("sure"), including the grant and
the revoke.

Every job ran through the coordinator; no direct device access. Evidence is in
[`2026-11-21-stage3-use-everywhere/`](2026-11-21-stage3-use-everywhere/), one directory per job.
Raw frames, logcat and score files stay on disk only (see that directory's `.gitignore`); each
check job keeps its `receipt.json`, `input-captures.json`, a `contact-sheet.jpg` of every capture
and the named key frames listed below.

## Verification 10 on Cast1

| Clause | Evidence | Result |
|---|---|---|
| Without the permission, Use everywhere shows the setup screen | `02b` `n2-setup-screen.png`: "This TV needs one-time setup", the command `adb shell pm grant net.biohack.homeboard.debug android.permission.WRITE_SECURE_SETTINGS`, Retry, Show QR code | **PASS** |
| … and Android's setting is unchanged | `01` and `02c` read the same six values (table below) | **PASS** |
| With it, the one‑time question changes the setting | `04c` `c1-question.png` ("Let Homeboard manage this TV's screensaver?", Now Seahorse Grid, After Ambient Mode); after "Let Homeboard manage it", `c2` shows "Ambient Mode · This TV's screensaver", Original "Seahorse Grid · kept" | **PASS** |
| … and reads it back | `04d` coordinator read: `selected_component` is Google's Ambient Mode (`com.google.android.apps.tv.dreamx/…Backdrop`) | **PASS** |
| A second choice applies with no question | `04f` `u1` goes straight back to the list with Aerial Views "In use"; Home banner "This TV's screensaver: Aerial Views" with Undo (`u3`); `04g` reads `com.neilturner.aerialviews/…DreamActivity` | **PASS** |
| Restore returns the recorded original | `04i` "Restore Seahorse Grid" (`x0`); banner "Restored this TV's screensaver: Seahorse Grid" (`x3`); list shows Clock "In use", Use everywhere… and "unchanged by Homeboard" again (`x4`); `04j` equals `01` in all six fields | **PASS** |
| Each value shown before and after from coordinator evidence | table below, from each job's `screensaver.json` | **PASS** |

Cast2 is not part of this run, so Verification 10 stays open for Cast2.

**Verification 12 (Cast1):** the final read `06`, taken after the grant was revoked, equals the
original in all six fields. **PASS** for Cast1.

## Android's values, read by the coordinator

`enabled` 1, `activate_on_sleep` 1, `idle_timeout_ms` 600000 (10&nbsp;min), `sleep_timeout_ms`
‑1 and `stay_on_while_plugged_in` 0 did not change in any read. Only `selected_component` moved:

| Step | Job | When (UTC) | `selected_component` | vs original |
|---|---|---|---|---|
| 01 original | `J‑6204cd04067d` | 04:00 | `org.worldfoundry.seahorsegrid/…SeahorseGridDreamService` | — |
| 02c after the setup screen, no grant | `J‑02b430c2eb3a` | 04:04 | Seahorse Grid | equal |
| 04b after opening the question and pressing Back, grant on | `J‑be807e8826fd` | 04:06 | Seahorse Grid | equal |
| 04d after "Let Homeboard manage it" | `J‑9e8302cbb620` | 04:09 | `com.google.android.apps.tv.dreamx/…Backdrop` (Ambient Mode) | changed |
| 04g after "Use this screensaver" on Aerial Views | `J‑f416bee9d3e2` | 04:12 | `com.neilturner.aerialviews/…DreamActivity` | changed |
| 04j after Restore | `J‑ecfc115180aa` | 04:16 | Seahorse Grid | equal |
| 06 final, after revoke | `J‑0e2e75b35ce5` | 04:17 | Seahorse Grid | equal |

## Every job

All 17 jobs completed with health `ready` and cleanup `restored`.

| Dir | Job | What | Recipe |
|---|---|---|---|
| `01-original-list` | `J‑6204cd04067d` | screensaver-list (original) | `WORKFLOW=screensaver-list` |
| `02a-map-nogrant` | `J‑dd818fc23f3b` | captures-only focus map of the Screensaver page | `dev/device/screensaver-stage3-map.json` |
| `02b-nogrant-setup-screen` | `J‑78a17640668c` | In other apps → Use everywhere…, setup screen | `screensaver-stage3-nogrant.json` |
| `02c-nogrant-list-after` | `J‑02b430c2eb3a` | screensaver-list | |
| `03-grant` | `J‑de2151a6176a` | grant: before false, granted true, verified true | `secure-settings-grant-enable.json` |
| `04a-question-map` | `J‑9e217032bc2c` | opens the question, moves focus, leaves with Back | `screensaver-stage3-question-map.json` |
| `04b-list-after-question-back` | `J‑be807e8826fd` | screensaver-list | |
| `04c-accept-question` | `J‑9933fa44e1ec` | "Let Homeboard manage it", then maps the managed list | `screensaver-stage3-accept.json` |
| `04d-list-after-accept` | `J‑9e8302cbb620` | screensaver-list | |
| `04e-second-choice-map` | `J‑7e7310a37e19` | captures-only focus map for Aerial Views | `screensaver-stage3-second-map.json` |
| `04f-second-choice` | `J‑8c601ddb21e7` | Aerial Views → Use this screensaver | `screensaver-stage3-second.json` |
| `04g-list-after-second` | `J‑f416bee9d3e2` | screensaver-list | |
| `04h-restore-map` | `J‑99e7918a6a4e` | captures-only map of This TV's screensaver… | `screensaver-stage3-restore-map.json` |
| `04i-restore` | `J‑82db9411ff21` | This TV's screensaver… → Restore Seahorse Grid | `screensaver-stage3-restore.json` |
| `04j-list-after-restore` | `J‑ecfc115180aa` | screensaver-list | |
| `05-revoke` | `J‑40fcf8b9e358` | revoke: before true, granted false, verified true | `secure-settings-grant-disable.json` |
| `06-final-list` | `J‑0e2e75b35ce5` | screensaver-list after revoke, plus `devices-after.txt` (Cast1 `ready`) | |

The focus path, from the maps: Home settings (Menu) → Down opens Screensaver with the "In use"
entry focused. Before management, Down ×3 reaches Ambient Mode (fourth card) and Right lands on
the **Use everywhere…** button beside In other apps. On the question, "Let Homeboard manage it" has
focus first. Once managed, the list opens on the "In use" card; from Aerial Views (third card)
Right reaches **Use this screensaver**, and from the "In use" card Right reaches **This TV's
screensaver…**, whose screen opens on Start now with Restore one step to the right.

## Observations (not fixed here)

1. **The In other apps button acts on the focused entry, not the one in use.** With Clock "In
   use" and Ambient Mode focused, **Use everywhere…** beside In other apps asked "After: your
   choice, now Ambient Mode", and confirming made Ambient Mode the Homeboard choice as well
   (`04a` `q1-question.png`, `04c` `c6-list-managed.png`). It is labelled "your choice", so this
   may be intended, but the row sits under "When it starts", which reads as page-wide rather than
   per entry. Worth a design-filter look.
2. **No "Open Aerial Views settings" button.** This TV's screensaver… for Aerial Views says "Its
   own settings are in Aerial Views." but offers only Start now and Restore (`04h`
   `r1-this-tv-screen.png`). Not established: whether Aerial Views' dream service declares a
   `settingsActivity`; `screensaver-list` does not report it per provider.
3. **Leaving the setup screen with its Back button** returned focus to the Ambient Mode card, not to the button that
   opened it (`02b` contact sheet, `n5`).

## Not covered

Retry and Show QR code on the setup screen were not pressed (Up from Retry went to Back); Undo of
the second choice and Undo of Restore; Start now; persistence across a restart; Cast2.
