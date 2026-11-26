| Date | Change |
|------|--------|
| [2026-11-26](https://github.com/wbniv/homeboard-tv/commit/c65f49e) | test: Stage 5 on Cast1 fails at discovery; WF Clocks listed without its preview screen |
| [2026-11-26](https://github.com/wbniv/homeboard-tv/commit/79c4d9f) | docs: no remote has a screensaver button |
| [2026-11-26](https://github.com/wbniv/homeboard-tv/commit/d69822e) | docs: Stage 5 built and merged; awaiting the Cast1 check |
| [2026-11-26](https://github.com/wbniv/homeboard-tv/commit/a847020) | docs: screensaver apps with their own preview screen; Stage 5 status in the contract |
| [2026-11-26](https://github.com/wbniv/homeboard-tv/commit/ef2fb21) | docs: WorldFoundry declares the preview metadata; Stage 5 unblocked |
| [2026-11-26](https://github.com/wbniv/homeboard-tv/commit/38d5be9) | docs: record Will's four screensaver integration decisions |
| [2026-11-26](https://github.com/wbniv/homeboard-tv/commit/011c5f1) | docs: map screensaver ownership across Homeboard, apps and Android |
| [2026-11-26](https://github.com/wbniv/homeboard-tv/commit/569a077) | docs: put the companion contract on hold for re-evaluation |
| [2026-11-26](https://github.com/wbniv/homeboard-tv/commit/28deabd) | docs: accept certificate admission for the companion contract |
| [2026-11-26](https://github.com/wbniv/homeboard-tv/commit/12e5c64) | docs: define the screensaver companion app contract; Stage 5 is blocked |

<!--history-meta v1
c65f49e	author	Will Norris
c65f49e	added	21
c65f49e	deleted	0
c65f49e	files	1
c65f49e	body	Verification 3 of the screensaver companion contract, run through the\ncoordinator: Homeboard 0.1.165 lists WF Clocks (89a31982, whose manifest\ndeclares PREVIEW_ACTIVITY and an exported ScreensaverPreviewActivity) as\nan app with no preview screen, so Preview, Home play and the ringing-timer\ncheck could not run. Open settings is not testable (no settingsActivity).\nCast1 left unchanged.\n\nCo-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqQzN1oGqYJXjCkzVJ1
79c4d9f	author	Will Norris
79c4d9f	added	2
79c4d9f	deleted	2
79c4d9f	files	1
79c4d9f	body	The screensaver action word only runs from a spare remote button that a build\nor theme maps through app_keys, which ships empty. The top-row plan, its\nmockup, the user guide, the contract, the investigation and the Stage 5 TODO\nsaid "the remote" as if a button existed.\n\nCo-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqQzN1oGqYJXjCkzVJ1
d69822e	author	Will Norris
d69822e	added	1
d69822e	deleted	1
d69822e	files	1
d69822e	body	The contract, the integration investigation and the original plan's Stage 5\nrow now say what is built (2395376) and what is left. TODO: Stage 5 moves to\nverify; the device check needs the WF Clocks APK and a Homeboard deploy.\n\nCo-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqQzN1oGqYJXjCkzVJ1
a847020	author	Will Norris
a847020	added	7
a847020	deleted	1
a847020	files	1
a847020	body	Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqQzN1oGqYJXjCkzVJ1
ef2fb21	author	Will Norris
ef2fb21	added	25
ef2fb21	deleted	6
ef2fb21	files	1
ef2fb21	body	WorldFoundry 5a117e85d puts PREVIEW_ACTIVITY (and PREVIEW_EXTRA/VALUE where\nthe registry has them) on Aquarium, Clocks and Seahorse Grid. The contract\ngains the Seahorse Grid column and Verification 1's evidence (partial:\nsettingsActivity comes with the chooser).\n\nCo-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqQzN1oGqYJXjCkzVJ1
38d5be9	author	Will Norris
38d5be9	added	40
38d5be9	deleted	67
38d5be9	files	1
38d5be9	body	The app chooses its own tank or clock, Homeboard plays installed\nWorld Foundry screensavers on Home and in Preview, Clock stays the\ndefault, and Homeboard does not keep the screen on. The companion\ncontract shrinks to preview metadata and the app's settings link.\n\nCo-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqQzN1oGqYJXjCkzVJ1
011c5f1	author	Will Norris
011c5f1	added	3
011c5f1	deleted	2
011c5f1	files	1
011c5f1	body	Who decides when a screensaver starts, which one runs and what it\nshows; nobody a person can reach decides the last today. Three\ndecisions are left for Will.\n\nCo-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqQzN1oGqYJXjCkzVJ1
569a077	author	Will Norris
569a077	added	4
569a077	deleted	3
569a077	files	1
569a077	body	The certificate choice was made without weighing whether Homeboard\nshould change another app's settings at all.\n\nCo-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqQzN1oGqYJXjCkzVJ1
28deabd	author	Will Norris
28deabd	added	15
28deabd	deleted	6
28deabd	files	1
28deabd	body	Record both Homeboard certificates. The debug one must be admitted by\nrelease builds of the app, the only builds fast enough to use.\n\nCo-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqQzN1oGqYJXjCkzVJ1
12e5c64	author	Will Norris
12e5c64	added	81
12e5c64	deleted	0
12e5c64	files	1
12e5c64	body	The Aquarium's settings provider refuses Homeboard and its manifest\ndoes not declare the preview names Stage 5 must read.\n\nCo-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqQzN1oGqYJXjCkzVJ1
-->
