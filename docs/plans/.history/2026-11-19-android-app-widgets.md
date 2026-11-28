| Date | Change |
|------|--------|
| [2026-11-28](https://github.com/wbniv/homeboard-tv/commit/06b7d1f) | test: real Fossify event renders as a widget row on Cast2 and clears on deletion; Android app widget hosting done |
| [2026-11-27](https://github.com/wbniv/homeboard-tv/commit/10feae2) | fix: Android widget minimums on the native 4K renderer, and Too small when a tile collapses |
| [2026-11-27](https://github.com/wbniv/homeboard-tv/commit/f4195e8) | test: Android widget close-out: Cast1 accepted at 0.1.179, two native 4K sizing defects on Cast2 |
| [2026-11-27](https://github.com/wbniv/homeboard-tv/commit/3deef1b) | test: Android widget lifecycle, survivor and resource probes; fixture provider 0.0.3 |
| [2026-11-20](https://github.com/wbniv/homeboard-tv/commit/299ae7d) | Android app widgets: record the v97 verification run and the how-to updates |
| [2026-11-20](https://github.com/wbniv/homeboard-tv/commit/2633196) | Fix Android 14 widget configuration handoff and add diagnostics |
| [2026-11-19](https://github.com/wbniv/homeboard-tv/commit/e44944d) | Host Android app widgets with TV setup, full-screen controls and Undo |

<!--history-meta v1
06b7d1f	author	Will Norris
06b7d1f	added	1
06b7d1f	deleted	1
06b7d1f	files	1
06b7d1f	body	Fossify's new-event screen opened by ACTION_INSERT with the title filled in;\nSave was reached through Back's save prompt (the toolbar is not D-pad\nreachable), deletion through selection mode. The widget row appeared inside\nHomeboard on the native renderer and cleared after deletion; the tile was\nremoved with the corrected banner wording. Remotes J-f505da983852 and\nJ-87662d36b1b0. The TODO item moves to Done.\n\nCo-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqP8zNDXFEqEEztS56o
10feae2	author	Will Norris
10feae2	added	9
10feae2	deleted	9
10feae2	files	1
10feae2	body	AppWidgetManager reports provider bounds in pixels at its own context's\ndensity, the Activity's base context (2 on Cast2). The native renderer gives\nthe Activity resources density 4, and the host divided by that, halving every\nprovider minimum: a 240x100 dp widget read as 120x50 and was given 168x78 dp\nat a 3x3 span. Bounds are now converted with the base context's density;\nCast2 picks 4x4 and gives 243x128 dp, matching Cast1.\n\nA tile below its provider's minimum no longer inflates a view. The decision\nis made when the tile is built, from the arithmetic Add and resize use: a\nToo small placeholder in the content area, or the message in the label row\nwhen no content area is left (the old placeholder lived inside the provider\nview and was invisible at zero height). Geometry moves to a pure\nAndroidWidgetGeometry with JVM tests for both cases; the lifecycle probe\nrecords host arithmetic beside the measured view and the size Android reports.\n\nCast2 J-61c3773a4911 and Cast1 J-76a157e4a2a7 pass on 0.1.181.\n\nCo-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqP8zNDXFEqEEztS56o
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
