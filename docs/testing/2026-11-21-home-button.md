# Home button: fresh device validation

Revalidated the older 0.1.108–110 handoff on 21 November 2026 using coordinator
0.1.212 (`ad4cc2da`). No build or installation is needed merely to deliver
`d7b2ad4`: it is an ancestor of current main, and both TVs already run later builds.

| Device | Fresh inspect job | Installed debug version | Public version |
|---|---|---|---|
| Cast1 | `J‑2d151c352c27` | 0.1.117 | 0.1.92 |
| Cast2 | `J‑77ee51a99a92` | 0.1.116 | 0.1.92 |

Frozen source manifests for 0.1.114, 0.1.116 and 0.1.117 agree with current
`HomeButtonRules.java` (SHA-256
`b6369aec7f4b183e5af4e4a01aca56b6a314be11a8333d9056732833e2612bce`)
and `HomeKeyService.java` (SHA-256
`13393e43338bae43bfad8c453cd28cb3de3b764304e5adfa2ac2e1181ea1c549`).
These contain the key-filtering correction. Version numbers alone are not the
evidence for inclusion.

Evidence is exported under `docs/diagnostics/home-button-2026-11-21/`.
Both inspect jobs succeeded with foreground unchanged; Cast1's inventory
collection was partial, so it is not evidence for accessibility secure settings.
Cast2 was in Personal use; only passive inspection was performed there.

## Cast1 starting behavior

Remote `J‑7688cedb847c` captured Google TV before and after a Home tap. It was
stopped with coordinator cleanup complete and no unresolved effects; evidence is
in `cast1-baseline-remote/`.

Remote `J‑724ca3571f37` opened Homeboard's Home button screen. It showed **On,
Setup needed**, with the explicit **Home button · Homeboard TV (test)** label in
the setup instructions (`cast1-home-button-state.png`). Thus the old claim that
Cast1 currently has the test helper enabled is stale. The Homeboard preference
was already On; this session did not turn it on.

From Android Accessibility, a 1500 ms held Down moved from TalkBack to Text to
speech, one row (`cast1-accessibility.png` →
`cast1-accessibility-services.png`, Remote command 29). This observes no repeat
under injected input in the starting configuration, not proof of physical-remote
impairment or that the fixed helper causes it.
The active services must be identified before attributing the result.

Android's refreshed list (`cast1-helper-list.png`) identified the plain **Home
button** public service as **On**, and **Home button · Homeboard TV (test)** as
**Off**. The intervening **Homeboard TV** service was also Off. This establishes
identity by the corrected label, not list order. The older public 0.1.92 helper
does not contain the repeat fix.

The session disabled only that public Home button helper through Android's
**Stop Home button? → OK** confirmation (`cast1-public-disable.png`).
`cast1-public-off.png` verifies its Off state before enabling the test helper.
This deliberately replaces the obsolete active helper; restoring that public
helper would restore the old key-filtering configuration.

## Fixed test helper enabled

Android admitted the test helper with its ordinary **Use Home button · Homeboard
TV (test)? → OK** consent, with no Restricted setting block
(`cast1-test-enable-consent.png`). `cast1-test-on.png` shows the test helper On,
public helper Off, and the two other Homeboard TV services Off.

Home from Android settings returned to Homeboard, which reported **On, Working**
(`cast1-home-from-settings.png`). Home from Homeboard restored the dashboard
(`cast1-home-from-homeboard.png`). Home also returned from the installed **TV
Media phase 0** app (`cast1-youtube.png` is actually that media app; the filename
does not establish YouTube coverage), preserving Favorites focus
(`cast1-home-from-media-app.png`). These are observed routing passes; they do not
establish absence of a brief Google TV flash or exact 1 s/3 s timing.

**Repeat remains unresolved:** a 1500 ms held Up from the enabled test helper's
row moved only to the preceding Homeboard TV row (`cast1-repeat-after.png`,
Remote command 59), despite the public helper being Off. The existing unit test
and frozen-source inclusion therefore do not establish on-device repeat
acceptance. The comparison below repeats the check with both Home button
services disabled on this same TV.

YouTube's profile chooser was then visibly in front
(`cast1-youtube-actual.png`), and Home returned to Homeboard
(`cast1-home-from-youtube.png`). No account selection or playback was needed.

A longer 4000 ms held Down in Android's top-level settings also moved only from
Display & Sound to Network & Internet (`cast1-system-settings-return.png` →
`cast1-repeat-4s-test-on.png`, Remote command 88). No network setting was changed.

## Same-device comparison: repeat is inconclusive

The test helper was stopped through its labelled Android confirmation.
`cast1-both-helpers-off.png` shows both Home button helpers and both other
Homeboard TV services Off. A 1500 ms held Up from the same test-helper row again
moved exactly one row (`cast1-repeat-both-off.png`, command 128). A subsequent
4000 ms held Up also moved one row, from Homeboard TV to the public Home button
row (`cast1-repeat-4s-both-off.png`, command 130).

The coordinator's injected holds did not repeat with the helper disabled either.
This is **inconclusive for physical-remote key repeat**, not a regression pass or
proof the fixed helper still causes the problem. A physical remote comparison
with the current labelled helper On/Off is required. No speculative code change
or extra deployment was made on the strength of this test.

## Final state and remaining acceptance

The test helper was re-enabled through its labelled Android consent. The final
Android list (`cast1-services-final.png`) shows the public Home button Off, test
Home button On, and both other Homeboard TV services Off. Turning the test helper
off in Android had correctly followed through to Homeboard's setting being Off
(`cast1-preference-after-android-disable.png`); enabling the service alone did not
override that choice. The session explicitly selected On in Homeboard again.

`cast1-working-final.png` confirms **On, Working**, with an Undo banner.
`cast1-home-final.png` confirms a final Home tap returns to the dashboard.
No secure-settings grant, default Home change, app-data reset, build or install
was performed. The final configuration is intentional for Will's request to make
Home open Homeboard; the obsolete public helper stays Off.

Remote `J‑724ca3571f37` was stopped normally through `remote-stop` (the coordinator
records this as owner-cancelled), with health ready, no fences and no unresolved
effects. Watch confirmed termination; the session was not left running.
Evidence export verified 51 artifacts in `cast1-acceptance-remote/`.

Still open:

- Physical-remote repeat with the current test helper On/Off; held Home; the
  branded YouTube and Netflix remote buttons. The coordinator accepts held
  D-pad/select keys only, not held Home or the branded app keys.
- Restart, force-stop and app-update recovery; the one-time grant/automatic repair
  path and complete Undo acceptance. Disabling the helper through Android did
  successfully make Homeboard's preference follow Off in this run.
- Cast2 setup and foreground acceptance when the coordinator admits that work.
  Its installed fixed build was inspected during Personal use; its current
  accessibility switches and routing were not inferred from the stale handoff.
  A subsequent `task chromecast:remote-start DEVICE=chromecast-test-02` was
  explicitly refused by coordinator admission: `available: false`, **Device
  paused for personal use by another session**. No Cast2 Remote job was created.

No compilation/test rerun was needed for these documentation and device-setting
changes. `git diff --check` passed. Existing build/test claims remain attributed
to the earlier deployment records, rather than represented as new test runs.

## Physical held Home and blocked recovery follow-up, 15:30–15:34

Will made Cast1 available for the recovery checks, then reported from the
physical remote: **“held home brings up settings panel”**, and requested a
current-screen capture. Record held Home as a **user-observed pass** for opening
Google's settings panel. No screenshot was obtained, and the agent did not
independently inspect that panel.

Coordinator 0.1.213 (`d322b2b8`) reported Cast1 unowned but **recovery-required**.
The fence originated in another session's Clocks install/launcher check
`J‑a3bc3f65f6aa`: its cleanup failed to verify restoration of
`org.worldfoundry.plantedtank/org.worldfoundry.screensaver.PlantedTankActivity`.
The subsequent automatic recovery `J‑f8f7fefb9363` connected successfully but
again failed cleanup verification (`RestoreUnverified`, `cleanup-unverified`).
This is not evidence of a Homeboard lifecycle failure. Its cause has not been
established by this follow-up.

Both receipts were exported beneath
`docs/diagnostics/home-button-recovery-2026-11-21/` (`coordinator-origin/` and
`coordinator-readd/`). Support reported capture, Remote, profile, install and
launch unavailable because of the recovery fence.

The ordinary capture request `J‑0c0da8679e1a` waited behind that fence and was
cancelled before admission; its export confirms zero artifacts. The coordinator's
current-screen screenshot path separately refused **Screenshot unavailable:
Device is recovery-required**. No screenshot is claimed, and no capture was left
queued to run after the panel disappears. The failed automatic recovery was not
blindly repeated, and no direct transport or manual fence clearing was used.

Restart, force-stop and update-recovery checks could not start. The next step is
to resolve the coordinator's foreground-cleanup fence, then resume those checks.
Physical held-key repeat and the branded app buttons remain open; held Home now
has Will's physical-remote observation above.
