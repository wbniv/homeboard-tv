# Home button: Cast1 recovery checks, 23 November 2026

Cast1 still runs debug **0.1.117**, confirmed in About. No app code changed and
no APK was installed. Coordinator **0.1.224 (`cf5700f9`)** owned all device input,
force-stop, relaunch and cleanup. This continues the
[November 21 record](2026-11-21-home-button.md).

## Results

| Check | Result |
|---|---|
| Initial Home-button preference | **Off**, captured before changing it. |
| Select On with the manually enabled test helper | **Working**, without a new grant or accessibility change. |
| Undo the latest Home-button choice through Undo history | **Pass:** restored Off; selecting On again restored Working. Long-Back Undo was not tested. |
| Force-stop and relaunch without reinstall | **Automatic recovery failed:** choice remained On, but status became **Setup needed** and the test helper was Off in Android accessibility. |
| Manual recovery | **Pass:** enabled **Home button · Homeboard TV (test)** through Android's normal consent. Home from Android settings returned to Homeboard; status showed **On · Working**. |
| Restart / app update | Helper retained On / Working and Home eventually returned from YouTube after restart; Google TV was visible first. App update not run. |
| Permission-assisted setup and repair | Follow-up passed; see below. The earlier manual-setup run had no grant. |

Will reported that holding Up or Down continued scrolling, while pressing YouTube
or Netflix during that hold did nothing. He separately confirmed that YouTube
opens when no direction is held. The captured Home-button preference was Off
during these earlier physical checks: these observations establish an Off-state
baseline, not a physical-repeat pass with the feature On. Netflix pressed alone
is unconfirmed. No cause is established for the simultaneous-button behavior.

The force-stop result must not be reported as a permission-assisted repair
failure: this run used manual accessibility setup without the one-time grant.
It does establish that reopening the app alone did not restore that setup.

Cast2 was recovery-required and in Personal use; no foreground acceptance ran.
No restart, update or Cast2 pass is inferred from the Cast1 results.

## Evidence and session cleanup

Evidence is local under `docs/diagnostics/home-button-2026-11-23/`:

- Baseline and Undo Remote **J‑0465b48ea2b8**, exported to `baseline-remote/`.
  Relevant frames: `helper-state-confirmed.png`, `on-initial.png`,
  `undo-history.png`, `undo-result.png` and `about.png`.
- Force-stop/relaunch record **J‑fffed610d15f**, exported to `force-stop-job/`.
  `commands.json` records `am force-stop net.biohack.homeboard.debug`, followed
  by `am start -W -n net.biohack.homeboard.debug/com.gathin.tvhome.MainActivity`.
  The job reused the installed frozen APK and completed successfully. Job
  completion is separate from the failed automatic-recovery assertion.
- Recovery Remote **J‑7301670bb519**, exported to `recovery-remote/`.
  `post-force-stop-home-button.png` shows On / Setup needed;
  `helper-restore-detail.png` shows the labelled test helper Off;
  `helper-restore-consent.png` records consent; `home-after-helper-restore.png`
  shows On / Working after Home from Android settings.

Both Remote sessions were stopped through coordinator cleanup. The recovery
Remote's final receipt has no unresolved effects or recovery fences. The record
job was finished through `input-finish`. No initial-check device job was left open.

The reusable [observation recipe](../../dev/device/home-button-recovery-observe.json)
requests no reinstall. The executed job omitted `warmup`, so it used the default
15 seconds; the saved recipe now explicitly requests zero warmup. Its launch
force-stops the app, so it must not be used as a passive inspection recipe.

## Next checks

1. Permission-assisted force-stop recovery passed below. Exact accessibility-list
   and global-switch preservation still need a supported readback check.
2. Check restart and a newly numbered app update, preserving data and settings.
3. With On / Working confirmed, verify physical held-key repeat and the branded
   buttons separately. Investigate simultaneous-button behavior only against a
   controlled baseline; do not assume it is caused by Homeboard.
4. Complete Cast2 setup and acceptance when coordinator admission permits it.

## Follow-up: permission-assisted recovery

The coordinator's narrow `secure-settings-grant` workflow **J‑1cf7c7c00680**
verified the exact frozen 0.1.117 APK and recorded the permission as originally
absent. It granted WRITE_SECURE_SETTINGS to the debug package for Android user 0
and verified readback. No other permission was requested.

Record **J‑66c283395a4c** then reused that installed APK, performed an actual
force-stop and relaunch, and completed without reinstall. Remote
**J‑d3fb093fe6e9** captured **On · Working** without manual accessibility repair.
Selecting Off showed Off; selecting On again showed Working. Launching YouTube
from Favorites reached its profile chooser, and a short Home returned to
Homeboard. This verifies permission-assisted force-stop recovery and cross-app
Home afterward; it does not verify the physical YouTube shortcut button.

The inverse grant job **J‑e6ffb984764b** verified restoration to **not granted**,
matching the original permission state. The Home-button choice was left On. The
record was finished and Remote was stopped with no unresolved effects or fences.
Evidence directories are `grant-on/`, `granted-force-stop/`, `granted-remote/`
and `grant-restored/` under the same diagnostics root. Screenshots include
`granted-recovery-status.png`, `granted-off.png`, `granted-on.png`,
`granted-youtube.png` and `granted-home-from-youtube.png`.

The coordinator's supported workflows did not supply an exact enabled-service
list/global accessibility-switch before/after comparison. Do not claim that
separate acceptance check passed from the UI status or permission readback.


## Restart attempt

After restoring the original permission state, coordinator reboot/readd
**J‑5bfb246ce49c** issued the restart successfully. Its receipt marked boot
unverified and `post-reboot-access-required` after only 13.8 seconds, with no
post-boot transport attempts recorded. Will reported that the TV was still
restarting at that point. Do not interpret that early diagnostic as proof that
wireless debugging switched Off. Automatic readd **J‑7342188fd961** is waiting
for the device to advertise again. Boot behavior and Home-button acceptance
remain pending. The reboot receipt is exported under `restart/`.

Will subsequently confirmed that Cast1 had finished restarting. Coordinator
connection-check still refused because recovery needs administrator attention;
the automatic readd remained waiting for the device. Access had not returned at
that check. Wireless debugging needs to be checked on the TV; the setting itself
was not remotely readable. No Home-button result after boot is claimed.


## Restart follow-up after Will enabled wireless debugging

Will enabled wireless debugging manually. Automatic readd **J‑7342188fd961**
then completed, cleared its recovery obligation and reported Cast1 ready on
`192.168.4.61:40383`. No new pairing was needed. Its cleanup commands verified
`sys.boot_completed`; evidence is in `restart-reconnected/`. This was recovery
with physical assistance, not unattended debugging persistence across reboot.

Remote **J‑01d602c432bf** found Homeboard already in front. A Home tap left it
in front, and Home settings showed **On · Working**, without changing the Home
choice, the helper's accessibility switch or its permission after boot. The
one-time permission had been restored to absent before reboot.

YouTube was opened through Favorites and reached its app UI. A short Home
produced a Google TV Home capture at 09:33:31, followed by Homeboard in a second
capture at 09:33:50 with no intervening input. This confirms eventual cross-app
redirection after restart. It does not pass a seamless-transition or 1-second
latency assertion; these captures do not measure the exact redirect delay.
The separate route from Google TV already in front was not exercised.

Relevant frames: `post-boot-baseline.png`, `post-boot-helper-status.png`,
`post-boot-youtube-ready.png`, `post-boot-home-from-youtube.png` (Google TV), and
`post-boot-home-settled.png` (Homeboard). Remote was stopped; its receipt has no
unresolved effects or recovery fences. Evidence is in `post-boot-remote/`.

Still pending: app-update recovery, physical repeat and separate branded
buttons with On / Working, exact accessibility-list preservation, and Cast2
acceptance. The redirect's visible transition/latency also needs a timed check.

## Timed screenshot check

Remote **J‑a3a43503990b** repeated YouTube → short Home three times on the same
0.1.117 installation, without changing settings or permissions. Each trial began
with YouTube's app UI captured and visually reviewed; every first screenshot
after Home showed Homeboard. Later screenshots remained on Homeboard.

| Trial | Conservative upper bound to Homeboard being visible |
|---|---|
| 1 | 4.15 seconds |
| 2 | 3.69 seconds |
| 3 | 4.02 seconds |

These bounds subtract the coordinator's Home-request creation timestamp from the
completion timestamp of the first screenshot command. They deliberately include
queue, key transport, client turnaround and screenshot transfer time. They are
**upper bounds, not measured redirect durations**. The screenshot commands alone
took about 1.5–1.7 seconds. All three returns were visible within 4.2 seconds of
the request; this sample does not establish a 1-second response or rule out a
Google TV flash between screenshots. The earlier visible Google TV transition
remains a real observation. No continuous video or exact flash-duration result
is claimed: the Remote workflow exports screenshots and a command timeline.

Evidence is under `timing-remote-final/`, with calculations in
`timing-summary.json`, beneath the diagnostics root. Home sequences were 5, 12
and 17; their first-frame sequences were 6, 13 and 18. Local copies are
`timing-run1-1.png`, `timing-run2-1.png` and `timing-run3-1.png`.
Remote cleanup finished with no unresolved effects or recovery fences; no app
code or APK changed. Exact transition timing remains a separate finer-resolution
check; the next available functional check is app-update recovery.

## Unrelated accessibility services: UI preservation check

**Pass for the observed Cast1 service states.** Android's accessibility UI was
captured before and after a permission-assisted Home-button cycle on 0.1.117.

| Android service label | Before | Home button Off | Restored On |
|---|---|---|---|
| TalkBack (Screen reader) | Off | Off | Off |
| Switch Access | Off | Off | Off |
| Home button (public helper) | Off | Off | Off |
| Homeboard TV (first entry) | Off | Off | Off |
| Homeboard TV (second entry) | Off | Off | Off |
| Home button · Homeboard TV (test) | On | Off | On |

Baseline Remote **J‑822376351d9d** captured TalkBack, Switch Access and all four
entries in the Services section. It then disabled only the labelled test Home
helper through Android's confirmation, allowing Homeboard itself to enable that
service during the test. The Home-button preference followed the manual disable
to Off, as intended.

Grant **J‑ec1adc178c17** verified that WRITE_SECURE_SETTINGS was originally absent
and temporarily granted it. Remote **J‑b915653368aa** selected On and captured
Working, selected Off, then inspected Android accessibility. The test helper was
Off and the other five services remained Off. Selecting On again restored
Working; Android's final service list showed only the test helper On.

The complete final screenshots for TalkBack, Switch Access and Services are
byte-identical to their corresponding baseline PNGs. Off-state TalkBack and
Switch Access screenshots are also byte-identical to baseline; Services was
visually reviewed to confirm only the test helper changed. Duplicate Homeboard
TV labels are identified by their list positions, not guessed package identity.
All unrelated services happened to be Off: preserving an already-On unrelated
service was not tested.

The inverse grant **J‑799cb1df3121** completed and verified **not granted**, matching
the original permission. Homeboard was left On with its test helper enabled.
Because this cycle explicitly let Homeboard add the helper, it now records that
helper as added by the Home-button setting. No unrelated switch was changed.
Both Remote sessions completed cleanup without unresolved effects or fences.

Local evidence under the same diagnostics root: `a11y-baseline-remote/`,
`a11y-cycle-remote/`, `a11y-grant/`, `a11y-grant-restored/` and
`a11y-comparison.json`. The comparable frames are `a11y-before-*.png`,
`a11y-off-*.png` and `a11y-final-*.png` for `talkback`, `switch-access` and
`services`; `a11y-auto-on.png`, `a11y-auto-off.png` and
`a11y-restored-working.png` capture Homeboard's choices/status.

This closes the UI service-state comparison. The coordinator still has no
supported raw readback for `enabled_accessibility_services` or
`accessibility_enabled`, so exact component spelling/order and the global
switch's value are **not verified** by this check. No raw-device fallback was used.

## Physical remote: what Will needs to check

The remaining ordinary-use check takes about a minute on Cast1. First open
**Home settings → Home button** and confirm **On · Working**. Earlier physical
scrolling and standalone YouTube observations were made with this preference
Off, so they cannot establish behavior with Home redirection active. There is
no need to repeat held Home: its Google settings-panel behavior is already recorded.

1. Return to Homeboard and release every remote button. Press the physical
   **YouTube** button once, with no direction held. YouTube should open. Press
   Home to return to Homeboard and release it. Press **Netflix** once, with no
   direction held. Netflix should open; its sign-in screen counts as opening.
   A missing/unavailable app or a system error is a separate result to record.
2. Open YouTube and choose a browsing screen with enough rows to scroll. From
   near the top, hold **Down alone for about two seconds**, then release it.
   Focus should move repeatedly through several items/rows, rather than moving
   once and stopping. Hold **Up alone for about two seconds** from lower in the
   list, then release it. Report whether scrolling repeats and stops on release.
   Do not press an app button during either hold. Do not judge repeat at the end
   of a list, where there may be nowhere else to move.
3. Report: `On / Working confirmed; YouTube button: …; Netflix button: …;
   held Down: …; held Up: …; stops on release: …`. Approximate movement is enough;
   no stopwatch or exact tile count is required.

The expected standalone YouTube behavior is ordinary app launch. We are checking
that enabling Home redirection preserves it, not asking for a new shortcut
behavior. The coordinator's injected keys are not an adequate substitute for
these physical-remote checks, and it cannot inject the branded app buttons.

### Separate investigation: direction held while pressing an app button

Will reported that holding Up/Down and then pressing YouTube/Netflix continued
scrolling without opening the app. That is a two-button combination. It is
separate from the normal-use checks above, and concurrent buttons are not an
established Homeboard requirement. The observation alone does not identify the
cause: the remote might not transmit the second press while holding a direction,
or Android, a helper or an app might handle it differently. No event trace has
established which happened.

If investigating this behavior, compare the same physical remote, app, browsing
position and button sequence with the test helper **enabled and On / Working**,
then with that helper **disabled in Android accessibility**. The agent should
coordinate those setting changes, verify each state and restore On / Working;
Will only needs to perform the physical presses. Turning the preference Off
inside Homeboard alone does not isolate the accessibility service, which may
remain enabled and use a different key-filtering configuration.

For each state: verify each app button alone, then separately hold Down and tap
YouTube, release both, and repeat with Up. Netflix can be checked the same way.
Record whether the app opens while the direction is held, after it is released,
or only on a fresh app-button press. If the combination fails in both helper
states, Homeboard is not shown to have introduced it; if it works only with the
helper disabled, investigate the helper's input handling. Neither result alone
proves a remote-hardware cause. This comparison has **not yet run**.

## Physical Home and standalone branded-button results

Will performed the physical presses; coordinator capture jobs sent no input.
The starting Android accessibility capture **J‑a498ea241686** shows the public
Home button helper Off and **Home button · Homeboard TV (test)** On.

- Will pressed Home from Android accessibility. Capture **J‑c9eef653c788** shows
  Homeboard's dashboard: physical Home redirection passed in this state.
- Will pressed YouTube alone. Capture **J‑b869725b8f4d** shows YouTube's browsing
  UI: the physical standalone YouTube shortcut passed.
- Will pressed Netflix alone. Capture **J‑64228abe0ec7** shows Netflix's Google
  Play details page with **Install**, not the Netflix app. The shortcut visibly
  responded; Netflix was not available to launch in this state. This does not
  pass installed-Netflix launch acceptance, and it is not evidence that the
  Home helper swallowed the button. Netflix was not installed for this test.

Each capture completed with health ready and six verified artifacts under
`docs/diagnostics/chromecast-capture-<ASCII-job-id>/`. Their `screenshot.png`
files are the visual evidence. Homeboard's preference was not reopened between
these physical presses; the enabled helper and successful Home redirection
provide the contemporaneous setup evidence. Physical held-Up/Down repeat and
the separate simultaneous-button comparison remain pending.
