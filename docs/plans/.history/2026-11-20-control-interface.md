| Date | Change |
|------|--------|
| [2026-11-20](https://github.com/wbniv/homeboard-tv/commit/faf1bc3) | Control interface: plan reviewed, stages tracked, lifetime probe wired |
| [2026-11-20](https://github.com/wbniv/homeboard-tv/commit/d5e8a07) | Control interface: plan with mockups and diagrams |

<!--history-meta v1
faf1bc3	author	Will Norris
faf1bc3	added	6
faf1bc3	deleted	8
faf1bc3	files	1
faf1bc3	body	The plan is done; its stages are open TODO items (stage 4 parked) and the\nplan's deferrals section takes the conventional heading. Taskfile maps\nRESUME_AFTER and POKE for the Stage 0 lifetime probe. Investigation: the\nkitchen display is a Nest Hub Max, enrolled; both TVs report Android 14\nand 32-bit ARM, per the coordinator's live device list.\n\nCo-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqN1KiiVNT63goz7fyo
d5e8a07	author	Will Norris
d5e8a07	added	834
d5e8a07	deleted	0
d5e8a07	files	1
d5e8a07	body	One command and state model (ControlHub) shared by four transports:\nnavigation-only homeboard:// links, a shell-only broadcast receiver for\nADB and the coordinator, an opt-in authenticated LAN listener with\nper-device keys approved on the TV, and an in-process interface for the\nMatter device and bridge client. Six mockups, six diagrams, staged\nimplementation, a lifetime device test and four decisions for Will.\n\nAdds task control:plan-check for the mockup bundle. No launcher code.\n\nCo-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqN1KiiVNT63goz7fyo
-->
