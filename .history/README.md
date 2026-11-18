| Date | Change |
|------|--------|
| [2026-11-18](https://github.com/wbniv/homeboard-tv/commit/539e9fc) | Complete TV controls and wallpaper provider directory |
| [2026-11-17](https://github.com/wbniv/homeboard-tv/commit/67c700a) | Reveal wallpaper previews and align Appearance directions |
| [2026-11-17](https://github.com/wbniv/homeboard-tv/commit/01eb586) | Add dashboard-first phone controls and native world clocks |
| [2026-11-17](https://github.com/wbniv/homeboard-tv/commit/460eea9) | Add a TV-approved phone editor for widgets and dashboard layout |
| [2026-11-17](https://github.com/wbniv/homeboard-tv/commit/4fdbaf9) | Add full-screen widget drilldowns and dense dashboard browsing |
| [2026-11-17](https://github.com/wbniv/homeboard-tv/commit/d77eeab) | Keep the default dashboard on one screen and remove its preview trap |
| [2026-11-17](https://github.com/wbniv/homeboard-tv/commit/10cab3c) | Fix empty Home navigation and add the default dashboard |
| [2026-11-17](https://github.com/wbniv/homeboard-tv/commit/f30db99) | Use automatic Rain location and deploy dashboard updates to both TVs |
| [2026-11-16](https://github.com/wbniv/homeboard-tv/commit/f16c73d) | Add native rain forecast widget and review workflow |
| [2026-11-16](https://github.com/wbniv/homeboard-tv/commit/bce5426) | Add combined calendar ranges and visual grid previews |
| [2026-11-16](https://github.com/wbniv/homeboard-tv/commit/91a9669) | Record combined 0.1.19 deployment and 53 passing menu regressions |
| [2026-11-16](https://github.com/wbniv/homeboard-tv/commit/00d4796) | Add native English and Thai language selection |
| [2026-11-16](https://github.com/wbniv/homeboard-tv/commit/e1dae89) | Verify background-fill opacity on Cast1 in 0.1.14 |
| [2026-11-16](https://github.com/wbniv/homeboard-tv/commit/a6727ae) | Deploy combined wallpaper host and dashboard styles as 0.1.13 |
| [2026-11-16](https://github.com/wbniv/homeboard-tv/commit/ad798a2) | Host Projectivy wallpaper providers with independent UI opacity |
| [2026-11-16](https://github.com/wbniv/homeboard-tv/commit/bed1ea1) | Document dashboard styles and coordinated deployment handoff |
| [2026-11-16](https://github.com/wbniv/homeboard-tv/commit/531779b) | Match Google TV app focus and version each deployment |
| [2026-11-16](https://github.com/wbniv/homeboard-tv/commit/b2c5553) | homeboard: initialize TV dashboard project |

<!--history-meta v1
539e9fc	author	Will Norris
539e9fc	added	25
539e9fc	deleted	5
539e9fc	files	1
539e9fc	body	Add fixed configurable clock cards, per-TV phone shortcuts, hold-Back Undo and independent widget background opacity. Add the TV and phone provider directory with reviewed Play/publisher links, QR fallback and Weather v2.11 exclusion; fix first-launch widget identity initialization.\n\nUpdate user guides, plans and compatibility dispositions. Record seven reviewed Cast1 directory/dashboard captures, four phone captures, QR decoding and four provider baselines. Container verification passed 472 JVM tests and lint; 0.1.59 is verified on Cast1 and queued for unreachable Cast2. Store round trips and remaining backend/account-dependent checks stay explicit.
67c700a	author	Will Norris
67c700a	added	1
67c700a	deleted	1
67c700a	files	1
01eb586	author	Will Norris
01eb586	added	7
01eb586	deleted	2
01eb586	files	1
460eea9	author	Will Norris
460eea9	added	9
460eea9	deleted	0
460eea9	files	1
460eea9	body	Serve a temporary local web editor with QR pairing, per-instance configuration, draft preview and revision conflicts. Commit layout and Air settings together with composite Undo and operation receipts. Deploy 0.1.40 to both TVs. Container checks and native Android HTTP integration passed; external physical-phone LAN acceptance remains unverified after connection timeouts.
4fdbaf9	author	Will Norris
4fdbaf9	added	11
4fdbaf9	deleted	1
4fdbaf9	files	1
d77eeab	author	Will Norris
d77eeab	added	4
d77eeab	deleted	4
d77eeab	files	1
d77eeab	body	Apply the default immediately with Undo, fit all four cards even on two-row grids, and allow Right from populated settings, Undo and the final arrangement-preview edge. The actual retained Cast1 dashboard was arranged through the menu and verified with Right to All Apps and Back. Install identical 0.1.32 APKs on both TVs.
10cab3c	author	Will Norris
10cab3c	added	13
10cab3c	deleted	0
10cab3c	files	1
10cab3c	body	Keep saved empty layouts navigable, add a four-widget first-run layout and an explicit preset with preview, Cancel and Undo. Container checks pass 429 JVM tests and lint; the same 0.1.30 APK is installed on both TVs.
f30db99	author	Will Norris
f30db99	added	18
f30db99	deleted	3
f30db99	files	1
f16c73d	author	Will Norris
f16c73d	added	13
f16c73d	deleted	0
f16c73d	files	1
f16c73d	body	Add configurable hourly and following-day rain forecasts, location selection, cached data, animated clouds and adaptive widget layouts. Include English/Thai resources, model tests, an isolated debug review activity and implementation documentation.\n\nRecorded validation: 402 JVM tests, six calendar pipeline checks, lint, resource parity and APK/bundle packaging passed. Cast1 installation and visual acceptance remain pending coordinator transport recovery.
bce5426	author	Will Norris
bce5426	added	7
bce5426	deleted	3
bce5426	files	1
bce5426	body	Combine reviewed offline holiday providers into one event-only Calendar widget with 7/30/custom lookahead, Monday week ranges, source controls and flag-colored date cards. Add replayable JSON normalization and assembly, tests, English/Thai resources and representative grid row/column previews.\n\nValidated 394 JVM tests, six pipeline tests, lint and APK packaging; deployed and visually checked 0.1.21 on Cast1. Live emergency refresh and the validated lunar calculator remain pending.
91a9669	author	Will Norris
91a9669	added	12
91a9669	deleted	13
91a9669	files	1
00d4796	author	Will Norris
00d4796	added	6
00d4796	deleted	0
00d4796	files	1
e1dae89	author	Will Norris
e1dae89	added	4
e1dae89	deleted	2
e1dae89	files	1
a6727ae	author	Will Norris
a6727ae	added	5
a6727ae	deleted	4
a6727ae	files	1
ad798a2	author	Will Norris
ad798a2	added	6
ad798a2	deleted	0
ad798a2	files	1
bed1ea1	author	Will Norris
bed1ea1	added	12
bed1ea1	deleted	0
bed1ea1	files	1
531779b	author	Will Norris
531779b	added	5
531779b	deleted	0
531779b	files	1
531779b	body	Add circular adaptive artwork, independent icon and label focus, and a cached\nring and halo with lifecycle and reduced-motion handling. Preserve Home layout\nwhile allowing larger circular icons in All Apps.\n\nPolish compass label geometry and About source links. Generate APK versionName\nfrom major.minor.versionCode and add containerized bump/deploy tasks with frozen\nartifacts and coordinator evidence.\n\nValidation: 348 unit tests, lint with zero errors, and debug APK assembly.\nDeploy 0.1.4 on Cast1 and verify focus, halo cleanup and the installed About\nversion. Submit the same APK to Cast2; its coordinator preflight fails with\npersonal-install-unverified. Reference motion calibration remains open.
b2c5553	author	Will Norris
b2c5553	added	68
b2c5553	deleted	0
b2c5553	files	1
-->
