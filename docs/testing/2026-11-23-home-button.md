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
| Restart / app update | Restart requested; boot acceptance pending coordinator reconnect. App update not run. |
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
