| Date | Change |
|------|--------|
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
