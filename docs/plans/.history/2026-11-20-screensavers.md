| Date | Change |
|------|--------|
| [2026-11-20](https://github.com/wbniv/homeboard-tv/commit/046aa0a) | Screensavers stage 1 complete: the alarm check passes on Cast1 |
| [2026-11-20](https://github.com/wbniv/homeboard-tv/commit/e25250d) | An alarm that ends a screensaver opens the timer screen |
| [2026-11-20](https://github.com/wbniv/homeboard-tv/commit/7b10d99) | Screensavers stage 3: use everywhere, with Restore and Undo |
| [2026-11-20](https://github.com/wbniv/homeboard-tv/commit/00b8ad1) | Screensavers stage 1: Use with Undo and no-timer checks pass on Cast1 |
| [2026-11-20](https://github.com/wbniv/homeboard-tv/commit/9faedc8) | Screensavers stage 1: record the Cast1 Preview and idle-start results |
| [2026-11-20](https://github.com/wbniv/homeboard-tv/commit/19e13d0) | Rename ClockDream to ScreensaverDream and drop compatibility claims |
| [2026-11-20](https://github.com/wbniv/homeboard-tv/commit/e82a088) | Screensavers stage 1: choose and configure from Home settings |
| [2026-11-20](https://github.com/wbniv/homeboard-tv/commit/5070588) | Screensavers plan: role icons on every card, Aerial Views shown once as both |
| [2026-11-20](https://github.com/wbniv/homeboard-tv/commit/5d1a6b6) | Screensavers: record Play account, Play edition and copyright answers |
| [2026-11-20](https://github.com/wbniv/homeboard-tv/commit/161d3b6) | Screensavers: settle decisions and research store distribution |
| [2026-11-20](https://github.com/wbniv/homeboard-tv/commit/84fba83) | Screensaver plan: add the World Foundry bundling decision |
| [2026-11-20](https://github.com/wbniv/homeboard-tv/commit/5bb93a2) | Plan screensaver choice, configuration and installation |

<!--history-meta v1
046aa0a	author	Will Norris
046aa0a	added	17
046aa0a	deleted	0
046aa0a	files	1
046aa0a	body	Job J-ffad14b3013c on build 0.1.104: a ringing test timer ends the screensaver\n(reason=alarm) and the timer screen shows the Done timer; the probe cleans up\nand Home returns. Moves Stage 1 and the alarm change to Done. Cast2 refused\nthe 0.1.104 install over its foreground and still has 0.1.103.\n\nCo-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqRqz1gyQcJHUETGjcT
e25250d	author	Will Norris
e25250d	added	4
e25250d	deleted	2
e25250d	files	1
e25250d	body	Will: "seems like a good idea to open the timer screen". The screensaver\nactivity and service now open the screen the alarm notification opens, so a\nringing timer is seen and can be stopped. The screensaver, the notification\nand MainActivity share one action constant, with a test that pins it.\nOpening it from the dream service is best effort, since Android may refuse a\nstart from a service.\n\nCo-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqRqz1gyQcJHUETGjcT
7b10d99	author	Will Norris
7b10d99	added	75
7b10d99	deleted	2
7b10d99	files	1
7b10d99	body	Homeboard can make its own screensaver, or an installed Android screensaver\napp, this TV's screensaver. A one-time setup screen shows the exact grant\ncommand; after one question every choice applies in every app with Undo, and\nRestore returns the TV's original. All Android reads and writes sit in one\ngateway, and the rules (read first, keep the original once, write, read back,\nput back on any failure) are pure Java tested with a fake. Installed\nscreensaver apps appear as entries, and the remote's screensaver action plays\na Homeboard choice directly.\n\nNot run on a TV: reading and writing Android's settings, the start-now\nshortcut and a restart are Stage 0 questions that are still open.\n\nCo-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqRqz1gyQcJHUETGjcT
00b8ad1	author	Will Norris
00b8ad1	added	16
00b8ad1	deleted	7
00b8ad1	files	1
00b8ad1	body	Verification 5 now passes: Use moves "In use" to Home background, Home shows\nthe Undo banner, and holding Back undoes it. Verification 6's other half\npasses within its limit: with "Android decides" nothing starts in 145 s. Only\nthe alarm case (step 7) remains, after the alarm change.\n\nCo-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqRqz1gyQcJHUETGjcT
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
