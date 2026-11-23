| Date | Change |
|------|--------|
| [2026-11-20](https://github.com/wbniv/homeboard-tv/commit/ccd3f98) | Investigation: the Home key on Google TV |

<!--history-meta v1
ccd3f98	author	Will Norris
ccd3f98	added	84
ccd3f98	deleted	0
ccd3f98	files	1
ccd3f98	body	Choosing a Home app does not move the key on Google TV; disabling Google's\nlauncher does but breaks the Google Home panel. The route is the\naccessibility override Homeboard already inherits (HomeKeyService with\nstock_home_redirect), off today because the setting is empty. Adds the\nCast1 probe as an open item and links it from the Google Home\ninvestigation and the control interface plan.\n\nCo-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqN1KiiVNT63goz7fyo
-->
