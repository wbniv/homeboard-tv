| Date | Change |
|------|--------|
| [2026-11-26](https://github.com/wbniv/homeboard-tv/commit/38d5be9) | docs: record Will's four screensaver integration decisions |
| [2026-11-26](https://github.com/wbniv/homeboard-tv/commit/6847440) | test: answer screensaver Q7 on Cast1 |
| [2026-11-26](https://github.com/wbniv/homeboard-tv/commit/9189502) | docs: say what still works once debugging is switched off |
| [2026-11-26](https://github.com/wbniv/homeboard-tv/commit/011c5f1) | docs: map screensaver ownership across Homeboard, apps and Android |

<!--history-meta v1
38d5be9	author	Will Norris
38d5be9	added	29
38d5be9	deleted	30
38d5be9	files	1
38d5be9	body	The app chooses its own tank or clock, Homeboard plays installed\nWorld Foundry screensavers on Home and in Preview, Clock stays the\ndefault, and Homeboard does not keep the screen on. The companion\ncontract shrinks to preview metadata and the app's settings link.\n\nCo-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqQzN1oGqYJXjCkzVJ1
6847440	author	Will Norris
6847440	added	16
6847440	deleted	8
6847440	files	1
6847440	body	Android's own screensaver starts over Homeboard's Home and over\nHomeboard's screensaver when Android's idle time runs out, leaving\nHomeboard's paused underneath. Where wake returns is not observable\nwith coordinator check jobs. Recipes, contact sheets and job\nevidence included.\n\nCo-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqQzN1oGqYJXjCkzVJ1
9189502	author	Will Norris
9189502	added	15
9189502	deleted	0
9189502	files	1
9189502	body	The secure-settings grant belongs to the install, not the adb\nconnection. Not yet tried on our devices.\n\nCo-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqQzN1oGqYJXjCkzVJ1
011c5f1	author	Will Norris
011c5f1	added	95
011c5f1	deleted	0
011c5f1	files	1
011c5f1	body	Who decides when a screensaver starts, which one runs and what it\nshows; nobody a person can reach decides the last today. Three\ndecisions are left for Will.\n\nCo-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqQzN1oGqYJXjCkzVJ1
-->
