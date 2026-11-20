| Date | Change |
|------|--------|
| [2026-11-20](https://github.com/wbniv/homeboard-tv/commit/9faedc8) | Screensavers stage 1: record the Cast1 Preview and idle-start results |
| [2026-11-20](https://github.com/wbniv/homeboard-tv/commit/19e13d0) | Rename ClockDream to ScreensaverDream and drop compatibility claims |
| [2026-11-20](https://github.com/wbniv/homeboard-tv/commit/e82a088) | Screensavers stage 1: choose and configure from Home settings |
| [2026-11-20](https://github.com/wbniv/homeboard-tv/commit/5070588) | Screensavers plan: role icons on every card, Aerial Views shown once as both |
| [2026-11-20](https://github.com/wbniv/homeboard-tv/commit/5d1a6b6) | Screensavers: record Play account, Play edition and copyright answers |
| [2026-11-20](https://github.com/wbniv/homeboard-tv/commit/161d3b6) | Screensavers: settle decisions and research store distribution |
| [2026-11-20](https://github.com/wbniv/homeboard-tv/commit/84fba83) | Screensaver plan: add the World Foundry bundling decision |
| [2026-11-20](https://github.com/wbniv/homeboard-tv/commit/5bb93a2) | Plan screensaver choice, configuration and installation |

<!--history-meta v1
9faedc8	author	Will Norris
9faedc8	added	43
9faedc8	deleted	0
9faedc8	files	1
9faedc8	body	Job J-19778f0ac74b on build 0.1.102: Preview plays and Back cancels, the\nOptions screen sets 1 min, the screensaver starts 59 s after the last input\nand a key ends it, and the setting is restored. Adds the recipe, the testing\nrecord and the results under the plan's Verification steps 4 to 6.\n\nCo-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqRqz1gyQcJHUETGjcT
19e13d0	author	Will Norris
19e13d0	added	7
19e13d0	deleted	7
19e13d0	files	1
19e13d0	body	Will: "no backwards compatability needed (or desired) until v1.0.0 public\nrelease". The Android screensaver service no longer keeps its old component\nname for TVs that named it, its label is the app's name instead of "Clock",\nand the manifest, comments, how-to and plan say so.\n\nCo-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqRqz1gyQcJHUETGjcT
e82a088	author	Will Norris
e82a088	added	47
e82a088	deleted	1
e82a088	files	1
e82a088	body	Adds Home settings → Screensaver with Clock and Home background, their\noptions (backdrop, dim, clock on or off) and "Start on Home after", Preview\n(OK uses, Back saves nothing) and Undo. ScreensaverSettings lives in\nHomeLayout and replaces the dead idle-clock, back-opens-clock and clock_*\ntheme values. ScreensaverActivity replaces ClockActivity; ClockDream keeps\nits component name and plays the chosen scene. Home background now shows\nHomeboard's own background, not the inherited theme files. A ringing timer\nor alarm ends a screensaver. Built-in entries are a registry whose first row\nis the default slot. Unit tests cover settings, registry and idle timer.\n\nNothing has run on a TV yet.\n\nCo-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqRqz1gyQcJHUETGjcT
5070588	author	Will Norris
5070588	added	5
5070588	deleted	0
5070588	files	1
5070588	body	Cards name each role with a small icon (⌂ plays inside Homeboard, ☾ also an\nAndroid screensaver app, ◆ built in). Aerial Views carries both roles stacked.\nGet screensavers cards gain placeholder app icons. PNGs re-rendered.\n\nCo-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqN1KiiVNT63goz7fyo
5d1a6b6	author	Will Norris
5d1a6b6	added	8
5d1a6b6	deleted	8
5d1a6b6	files	1
5d1a6b6	body	The 14-day closed test applies to each new app; Homeboard will have a\nPlay edition that hands installs to Play; Will owns the World Foundry\ncopyright, so compiling the 3D clock default into Homeboard is viable.\nAdds TODO items for the Play edition and the bundled default.\n\nCo-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqRqz1gyQcJHUETGjcT
161d3b6	author	Will Norris
161d3b6	added	89
161d3b6	deleted	61
161d3b6	files	1
161d3b6	body	Records Will's four decisions in the plan (everywhere is the goal, a\nreplaceable default, the first directory, own apps), adds Install for\nHomeboard's own publisher's apps and a companion-apps stage, and updates\nthe two affected mockups. Adds the distribution research: one World\nFoundry app, Play and other stores, installing from Homeboard, and the\ncoordinator's settings contract at app level.\n\nCo-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqRqz1gyQcJHUETGjcT
84fba83	author	Will Norris
84fba83	added	20
84fba83	deleted	1
84fba83	files	1
84fba83	body	Records the measured licence, size and coupling costs of compiling the\nWorld Foundry tanks into Homeboard, and the recommended alternative of\nstarting their existing preview activity.\n\nCo-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqRqz1gyQcJHUETGjcT
5bb93a2	author	Will Norris
5bb93a2	added	439
5bb93a2	deleted	0
5bb93a2	files	1
5bb93a2	body	One Screensaver screen for built-in, provider-hosted and Android\nscreensavers, with five mockups, five diagrams, a Stage 0 device probe\nand three decisions for review. Adds a generic plan mockup check and\nthe staged TODO items. Nothing is implemented.\n\nCo-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqRqz1gyQcJHUETGjcT
-->
