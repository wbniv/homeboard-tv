| Date | Change |
|------|--------|
| [2026-11-27](https://github.com/wbniv/homeboard-tv/commit/982263f) | test: continuous Screensaver list on Cast1 (0.1.173) |
| [2026-11-27](https://github.com/wbniv/homeboard-tv/commit/f225564) | docs: a missing screensaver app keeps its place unless Homeboard manages the TV |
| [2026-11-26](https://github.com/wbniv/homeboard-tv/commit/3e1956f) | docs: the continuous Screensaver list and When-it-starts-only Change, as built |
| [2026-11-26](https://github.com/wbniv/homeboard-tv/commit/952dcb5) | Refresh stale 4K and screensaver documentation status |
| [2026-11-26](https://github.com/wbniv/homeboard-tv/commit/4e5d198) | Plan continuous screensaver browsing with visual mockups |

<!--history-meta v1
982263f	author	Will Norris
982263f	added	15
982263f	deleted	1
982263f	files	1
982263f	body	Acceptance on Cast1 at 1080p: every entry reached with Up/Down alone\n(10, then 9 after the coordinator removed the old Homeboard), no page\nbuttons, header Back above row 1, the last row stays, count and thumb\nright; Right/OK, Left, Back and header Back as the table; returns from\nthe built-in Preview, built-in Options, WF Clocks' app screen and Use\neverywhere keep entry, action and scroll; browsing changes no Android\nsetting; Use/Undo restores the choice; Change with WF Clocks opens When\nit starts alone, with Clock the full Options.\n\nNot testable here: key repeat (coordinator holds give one key event),\nActivity recreation and package refresh with the picker open, external\nsettings activities (no listed app declares one). Finding: after Use,\nfocus goes to Preview for a built-in but to the row for an app\n(return tag act:use, MainActivity.java:3197).\n\nCo-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqQzN1oGqYJXjCkzVJ1
f225564	author	Will Norris
f225564	added	8
f225564	deleted	0
f225564	files	1
f225564	body	Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqQzN1oGqYJXjCkzVJ1
3e1956f	author	Will Norris
3e1956f	added	15
3e1956f	deleted	1
3e1956f	files	1
3e1956f	body	Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqQzN1oGqYJXjCkzVJ1
952dcb5	author	Will Norris
952dcb5	added	3
952dcb5	deleted	2
952dcb5	files	1
4e5d198	author	Will Norris
4e5d198	added	174
4e5d198	deleted	0
4e5d198	files	1
-->
