| Date | Change |
|------|--------|
| [2026-11-20](https://github.com/wbniv/homeboard-tv/commit/a163f01) | Google Home investigation: screensaver and settings as custom modes and toggles |
| [2026-11-20](https://github.com/wbniv/homeboard-tv/commit/b0f6606) | Google Home investigation: record the kitchen Nest display; add TODO items |
| [2026-11-20](https://github.com/wbniv/homeboard-tv/commit/1327db1) | Google Home investigation: weigh lasting costs, not engineering effort |
| [2026-11-20](https://github.com/wbniv/homeboard-tv/commit/0993cf1) | Investigation: options for integrating Homeboard TV with Google Home |

<!--history-meta v1
a163f01	author	Will Norris
a163f01	added	14
a163f01	deleted	1
a163f01	files	1
a163f01	body	Will asked about exposing the screensaver and settings with our own set of\ncontrols. Cloud-to-cloud's Modes and Toggles traits carry custom-named\nsettings by voice; Google Home offers no way to supply our own interface,\nand routines list neither trait. The Homeboard device carries only these,\nnot the stock TV traits. Matter appears unable to express named modes.\n\nCo-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqN1KiiVNT63goz7fyo
b0f6606	author	Will Norris
b0f6606	added	21
b0f6606	deleted	15
b0f6606	files	1
b0f6606	body	The chromecast-coordinator already records a kitchen Nest Hub or Hub Max\nand the TVs' Android versions (14 and 12). With a Google hub present, the\nMatter device is prototyped beside the bridge, and the Cast receiver is no\nlonger parked. Adds three open items: the Home APIs Stage 0 probe, the\ncontrol interface, and the paired direction 1 prototypes.\n\nCo-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqN1KiiVNT63goz7fyo
1327db1	author	Will Norris
1327db1	added	44
1327db1	deleted	25
1327db1	files	1
1327db1	body	Will: "engineering is practically free these days". Rank options by\nstanding obligations, outside gates and permanent app weight. The Matter\ndevice moves from parked to a contender beside the bridge where the home\nhas a Google hub; the Matter controller becomes the fallback if the Home\nAPIs do not run on a TV. The comparison table's Effort column is replaced\nby what gates each option.\n\nCo-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqN1KiiVNT63goz7fyo
0993cf1	author	Will Norris
0993cf1	added	308
0993cf1	deleted	0
0993cf1	files	1
0993cf1	body	Desk research across three directions: Homeboard appearing in Google Home\n(cloud-to-cloud, Matter, Home Assistant, Local Home SDK), Google Home data\non Homeboard (Home APIs, Home MCP, Nest Device Access, the built-in home\npanel) and Homeboard on other Google screens (Cast). Recommends a Stage 0\nprobe of the Home APIs on Cast1 before any design. Nothing device-tested.\n\nCo-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqN1KiiVNT63goz7fyo
-->
