| Date | Change |
|------|--------|
| [2026-11-27](https://github.com/wbniv/homeboard-tv/commit/3deef1b) | test: Android widget lifecycle, survivor and resource probes; fixture provider 0.0.3 |
| [2026-11-20](https://github.com/wbniv/homeboard-tv/commit/299ae7d) | Android app widgets: record the v97 verification run and the how-to updates |
| [2026-11-20](https://github.com/wbniv/homeboard-tv/commit/2633196) | Fix Android 14 widget configuration handoff and add diagnostics |
| [2026-11-19](https://github.com/wbniv/homeboard-tv/commit/e44944d) | Host Android app widgets with TV setup, full-screen controls and Undo |

<!--history-meta v1
3deef1b	author	Will Norris
3deef1b	added	56
3deef1b	deleted	0
3deef1b	files	1
3deef1b	body	The close-out of the Android widget plan needs device evidence for the rows\nthe earlier probe never touched. AndroidWidgetLifecycleProbeActivity (debug)\nruns, with its own preferences and host ID per mode:\n\n- lifecycle: two instances, provider update and size response, Interact with\n  Press and nested Back, illegal resize rejection, 24x24 grid placeholder and\n  recovery, lost-binding placeholder with Remove/Undo, activity recreation,\n  Remove/Undo with the same ID, release after history eviction, four-view cap\n- survivor-keep/-reopen/-reinstalled: a binding kept across jobs, a provider\n  update, and Set up again after the provider is uninstalled\n- resource: zero/one/four views and 50 expand/restore cycles, timed for the\n  coordinator's app-diagnostics PSS timeline\n\nThe fixture provider's no-configuration widget now counts renders, shows the\nsize Android reports and offers a Press button. Recipes are under dev/device/.\n\nCo-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqP8zNDXFEqEEztS56o
299ae7d	author	Will Norris
299ae7d	added	6
299ae7d	deleted	2
299ae7d	files	1
299ae7d	body	Committed on Will's instruction; the edits come from the widget-hosting work,\nnot from the Favorites session.\n\nCo-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqNmqPnTDDnLftRVKDf
2633196	author	Will Norris
2633196	added	46
2633196	deleted	0
2633196	files	1
e44944d	author	Will Norris
e44944d	added	687
e44944d	deleted	0
e44944d	files	1
-->
