| Date | Change |
|------|--------|
| [2026-11-27](https://github.com/wbniv/homeboard-tv/commit/aff3a55) | docs: round 2 layout fixes and the portrait note in the browsing plan's acceptance |
| [2026-11-27](https://github.com/wbniv/homeboard-tv/commit/815e3e3) | test: Screensaver list round 2 on Cast1 and the Pixel (0.1.175) |
| [2026-11-27](https://github.com/wbniv/homeboard-tv/commit/982263f) | test: continuous Screensaver list on Cast1 (0.1.173) |
| [2026-11-27](https://github.com/wbniv/homeboard-tv/commit/f225564) | docs: a missing screensaver app keeps its place unless Homeboard manages the TV |
| [2026-11-26](https://github.com/wbniv/homeboard-tv/commit/3e1956f) | docs: the continuous Screensaver list and When-it-starts-only Change, as built |
| [2026-11-26](https://github.com/wbniv/homeboard-tv/commit/952dcb5) | Refresh stale 4K and screensaver documentation status |
| [2026-11-26](https://github.com/wbniv/homeboard-tv/commit/4e5d198) | Plan continuous screensaver browsing with visual mockups |

<!--history-meta v1
aff3a55	author	Will Norris
aff3a55	added	2
aff3a55	deleted	0
aff3a55	files	1
aff3a55	body	Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqQzN1oGqYJXjCkzVJ1
815e3e3	author	Will Norris
815e3e3	added	8
815e3e3	deleted	0
815e3e3	files	1
815e3e3	body	Focus after Use now stays on Preview for an app and a built-in. Large\ntext on Cast1: at 1.3x actions stay reachable and the pane scrolls, but\nnames beside a badge break mid-word; at 2.0x Options is clipped, the\nlast row's name disappears behind its badge and When it starts values\nbreak mid-word. Pixel: Homeboard runs in landscape, so the stacked\nlayout is not reachable; taps inspect and act, a failed Open Clock\nsettings keeps its retry, but a row name breaks mid-word, the action row\nis cut and swipes change the inspected entry instead of scrolling.\n\nCast2 at 4K not run: the deploy naming chromecast-test-02 was refused by\nthe agent's permission check. Cast1 restored (Clock, Android decides,\nfont scale 1.0, Android WF Clocks at 10 min).\n\nCo-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqQzN1oGqYJXjCkzVJ1
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
