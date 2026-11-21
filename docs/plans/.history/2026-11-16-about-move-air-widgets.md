| Date | Change |
|------|--------|
| [2026-11-16](https://github.com/wbniv/homeboard-tv/commit/531779b) | Match Google TV app focus and version each deployment |
| [2026-11-16](https://github.com/wbniv/homeboard-tv/commit/b2c5553) | homeboard: initialize TV dashboard project |

<!--history-meta v1
531779b	author	Will Norris
531779b	added	7
531779b	deleted	4
531779b	files	1
531779b	body	Add circular adaptive artwork, independent icon and label focus, and a cached\nring and halo with lifecycle and reduced-motion handling. Preserve Home layout\nwhile allowing larger circular icons in All Apps.\n\nPolish compass label geometry and About source links. Generate APK versionName\nfrom major.minor.versionCode and add containerized bump/deploy tasks with frozen\nartifacts and coordinator evidence.\n\nValidation: 348 unit tests, lint with zero errors, and debug APK assembly.\nDeploy 0.1.4 on Cast1 and verify focus, halo cleanup and the installed About\nversion. Submit the same APK to Cast2; its coordinator preflight fails with\npersonal-install-unverified. Reference motion calibration remains open.
b2c5553	author	Will Norris
b2c5553	added	104
b2c5553	deleted	0
b2c5553	files	1
-->
