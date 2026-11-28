| Date | Change |
|------|--------|
| [2026-11-27](https://github.com/wbniv/homeboard-tv/commit/bbf2777) | test: native 4K closes; Cast2 output transition and native provider probe pass |
| [2026-11-27](https://github.com/wbniv/homeboard-tv/commit/fb10849) | docs: native 4K closing round; Cast1 HD control passes on 0.1.177 |
| [2026-11-26](https://github.com/wbniv/homeboard-tv/commit/952dcb5) | Refresh stale 4K and screensaver documentation status |
| [2026-11-26](https://github.com/wbniv/homeboard-tv/commit/2f5340f) | Enable automatic native 4K with a native 100 percent workspace |
| [2026-11-26](https://github.com/wbniv/homeboard-tv/commit/1f208a4) | test: verify reduced-scale dashboard and native 4K widget hosting |
| [2026-11-25](https://github.com/wbniv/homeboard-tv/commit/734465b) | feat: add dashboard scale for a larger widget workspace |
| [2026-11-25](https://github.com/wbniv/homeboard-tv/commit/1ba18f5) | fix: keep PM2.5 map tiles stable at native 4K |
| [2026-11-25](https://github.com/wbniv/homeboard-tv/commit/cc4934f) | feat: show rendering resolution in About and refine US calendar band |
| [2026-11-25](https://github.com/wbniv/homeboard-tv/commit/289e060) | feat: add native 4K dashboard proof and fix All Apps grid |
| [2026-11-23](https://github.com/wbniv/homeboard-tv/commit/4a0017e) | Consolidate tested standalone resolution tester changes |
| [2026-11-23](https://github.com/wbniv/homeboard-tv/commit/c3234b6) | Add standalone 4K tester and screensaver shortcut |

<!--history-meta v1
bbf2777	author	Will Norris
bbf2777	added	49
bbf2777	deleted	19
bbf2777	files	1
bbf2777	body	Cast2 on frozen 0.1.179:\n- J-d55b317ebcd5 NativeOutputTransitionProbeActivity: 3840x2160@60 native\n  -> 1920x1080@60 ordinary -> 3840x2160@60 native, one recreation each,\n  layout/page/focus retained, UHD baseline kept; status pass.\n- J-d44b7b84b0e6 widget binding already on (before true, changed false).\n- J-5e55f777158f AndroidWidgetProbeActivity: pass on "native surface\n  3840x2160"; compact/expanded RemoteViews, Restore/Back, phone move/resize,\n  Remove/Undo same binding, household unchanged.\nhomeboard-tv registration restored to MainActivity after each run.\nGate 4 split to its own item (Will, 2026-11-27). Item promoted to Done;\npanel detail, screen reader, phone transport and providers stay on Watch.\n\nCo-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqP8zNDXFEqEEztS56o
fb10849	author	Will Norris
fb10849	added	144
fb10849	deleted	0
fb10849	files	1
fb10849	body	Plan gains a closing-round section: disposition of every open acceptance\npoint, the provider-probe defect, the output-transition probe design and\nthe five engineering gates with evidence and PASS/PARTIAL/OPEN results.\nCast1 HD rerun (J-99927171f81c scripted, J-f487348f74e2 Remote) reads\nAbout 0.1.177, rendering 1920x1080, output 1920x1080 at 59.94 Hz, scale\npreview 100% 8x4. Panel detail, screen reader, phone transport and\nbroader providers move to Watch; the two Cast2 runs remain on the item.\n\nCo-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqP8zNDXFEqEEztS56o
952dcb5	author	Will Norris
952dcb5	added	19
952dcb5	deleted	13
952dcb5	files	1
2f5340f	author	Will Norris
2f5340f	added	70
2f5340f	deleted	4
2f5340f	files	1
1f208a4	author	Will Norris
1f208a4	added	8
1f208a4	deleted	2
1f208a4	files	1
734465b	author	Will Norris
734465b	added	42
734465b	deleted	1
734465b	files	1
1ba18f5	author	Will Norris
1ba18f5	added	21
1ba18f5	deleted	0
1ba18f5	files	1
cc4934f	author	Will Norris
cc4934f	added	9
cc4934f	deleted	0
cc4934f	files	1
289e060	author	Will Norris
289e060	added	202
289e060	deleted	1
289e060	files	1
4a0017e	author	Will Norris
4a0017e	added	28
4a0017e	deleted	0
4a0017e	files	1
c3234b6	author	Will Norris
c3234b6	added	74
c3234b6	deleted	0
c3234b6	files	1
-->
