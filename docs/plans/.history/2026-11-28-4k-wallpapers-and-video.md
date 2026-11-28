| Date | Change |
|------|--------|
| [2026-11-28](https://github.com/wbniv/homeboard-tv/commit/558da13) | feat: 4K still wallpapers on the native renderer; prove 4K video output |

<!--history-meta v1
558da13	author	Will Norris
558da13	added	342
558da13	deleted	0
558da13	files	1
558da13	body	Under the native 4K renderer, still provider images now decode for up to\n3840x2160 into one hardware bitmap (blur thumbnail drawn first), admitted by\nActivityManager memory and falling back to today's 1080p decode on refusal,\nOutOfMemoryError or any decode failure. The ordinary renderer (Cast1 and every\nnon-native device) keeps the exact 1080p expressions; WallpaperTarget holds them\nwith JVM tests. Animated images keep the 1080p target: at 4K they cost about\n156 MiB more PSS on Cast2. Video needed no change.\n\nWallpaperResolutionProbeActivity (debug) reads the pixels back on the device:\none-pixel stripe fixtures survive only a full-resolution path. Cast2 0.1.185\nJ-6adff9a23f88: still 3840x2160 hardware, stripes 1.0/1.0, +10.4 MiB PSS over\nthe 1080p path, six swaps no growth; 4K HEVC TextureView readback crisp at\n3840x2160. Cast1 J-5d1813cdb96a: ordinary renderer, unchanged 1080p decode.\n\nPlan and verification: docs/plans/2026-11-28-4k-wallpapers-and-video.md\n\nCo-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>\nClaude-Session: https://claude.ai/code/session_01YEenqP8zNDXFEqEEztS56o
-->
