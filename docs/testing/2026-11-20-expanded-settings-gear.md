# Expanded widget settings gear — November 20, 2026

Implemented in **0.1.116**. The top-right Configure text is now the existing
settings gear in the shared `WidgetExpansion` header. Its 48 dp target retains
the localized Configure accessibility name and tooltip, palette tint, disabled
state while loading and visible remote-focus outline. The settings callbacks and
Android provider configurability condition are preserved.

## Sweep and checks

Source inspection of all eight `MainActivity.expandWidget` factories confirms
Calendar, Rain, Air quality, Favorites, World clock (including the fixed header
clock), Timers & alarms, Groceries and Android widgets share this header. No
separate top-right Configure text control remains in those implementations.
Android providers without reconfiguration still hide the action.

`task deploy` completed container checks, **648 JVM tests**, Android lint and APK
assembly. Both updated SVG mockups parse as XML. Four added Rain tests cover
Bangkok's last local hour, partial today and explicit future days, expired data,
and a daylight-saving boundary. Rain previously requested an empty interval after
23:00; the expanded model now selects the first day with a future forecast hour.

Cast1 owned interactive job **J‑6129e04ae8d8** completed successfully, with the
device ready after cleanup. Observed checks:

- [World clock](2026-11-20-expanded-settings-gear/world-clock.png): rendered gear.
- [Gear focus](2026-11-20-expanded-settings-gear/gear-focus.png): clear outline;
  short OK opens [clock settings](2026-11-20-expanded-settings-gear/clock-settings.png).
  Back cancels the draft and [returns to expanded content](2026-11-20-expanded-settings-gear/settings-return.png).
- [Rain](2026-11-20-expanded-settings-gear/rain.png): gear and complete expanded
  content near 23:52 Bangkok, starting tomorrow at 00:00 without the earlier crash.
- [Calendar](2026-11-20-expanded-settings-gear/calendar.png) and
  [Timers](2026-11-20-expanded-settings-gear/timers.png): rendered gear.
- Back from each expansion restores Home; the final
  [restored dashboard](2026-11-20-expanded-settings-gear/restored.png) retains timer
  focus, widget positions and sizes. No timer or saved configuration was changed.

Air, Favorites, Groceries and Android-provider expansions were source-swept;
this run does not claim separate on-device visual checks for them. Screen-reader
speech and loading-time disabled tint were not physically exercised.

The committed recipe is adaptive: observe `input-frame` before selecting a
widget, then use bounded owned input and finish the complete session. The first
0.1.115 scripted attempt **J‑eff1f3c61d5a** assumed Timer focus, but started on the
TV Media favorite and launched it; the foreground guard stopped the check.
Automatic recovery **J‑d70f692d5f61** succeeded. The intervening Remote
**J‑9f55f4ccb7a0** returned black captures and was stopped with cleanup complete;
those frames are not visual acceptance evidence.

## Installation evidence

Both 0.1.116 installation and launch verification jobs succeeded:

| Device | Job | Installed APK |
|---|---|---|
| Cast1 | J‑f32d44a8c343 | verified matching frozen SHA-256 |
| Cast2 | J‑564292b45441 | verified matching frozen SHA-256 |

Frozen SHA-256:
`c7de6554f51aa291c4e54156e4b329e7caa230b698648c58a145ab560b6785aa`.
Private raw receipts are under `artifacts/deployments/0.1.116/`, including each
device's `evidence/apk-installations.json`; visual session evidence is under
`docs/diagnostics/expanded-settings-gear/116-visual-evidence`.

Earlier 0.1.115 jobs: Cast1 background **J‑c7d1a870d6ee** refused install over
foreground; the subsequent scripted check installed its APK but failed as above.
Cast2 **J‑16713668b153** verified installation but failed its background foreground
postcondition when the public Homeboard app became foreground; recovery
**J‑bb3cd9e0ac1b** succeeded. Version 0.1.116 supersedes that APK on both TVs.
