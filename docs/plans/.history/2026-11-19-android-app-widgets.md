| Date | Change |
|------|--------|
| [2026-11-27](https://github.com/wbniv/homeboard-tv/commit/f4195e8) | test: Android widget close-out: Cast1 accepted at 0.1.179, two native 4K sizing defects on Cast2 |
| [2026-11-27](https://github.com/wbniv/homeboard-tv/commit/3deef1b) | test: Android widget lifecycle, survivor and resource probes; fixture provider 0.0.3 |
| [2026-11-20](https://github.com/wbniv/homeboard-tv/commit/299ae7d) | Android app widgets: record the v97 verification run and the how-to updates |
| [2026-11-20](https://github.com/wbniv/homeboard-tv/commit/2633196) | Fix Android 14 widget configuration handoff and add diagnostics |
| [2026-11-19](https://github.com/wbniv/homeboard-tv/commit/e44944d) | Host Android app widgets with TV setup, full-screen controls and Undo |

<!--history-meta v1
f4195e8	author	Will Norris
f4195e8	added	31
f4195e8	deleted	13
f4195e8	files	1
f4195e8	body	Cast1 passes the lifecycle, survivor, in-place provider update (fixture 0.0.4,\nversion-only bump on the same key) and the coordinator-PSS resource budgets.\nCast2 passes the budgets and fails the lifecycle grid step: the minimum span\ngives the provider less than its declared minimum, and a collapsed provider\narea shows no Too small placeholder. The verification record lists every plan\nstep with its evidence. TODO: the item now names only what is left; Pixel and\ntablet hosting and the 24-hour exposure become their own items, and Breezy's\nremote-only location setup goes to Watch. Three Mermaid labels lost their\nsemicolons so the plan's diagrams render again.\n\nCo-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqP8zNDXFEqEEztS56o
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
