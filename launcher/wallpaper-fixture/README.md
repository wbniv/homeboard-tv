# Offline Projectivy wallpaper provider fixture

Build with `task wallpaper-fixture`. This library is bundled only in Homeboard debug APKs;
it is not a separate installed app. Install Homeboard only through the coordinator.
The service runs in a separate process and serves color, PNG, drawable, animated drawable,
Lottie and muted video to the host probe. It exposes no public preferences UI.
`getPreferences` / `setPreferences` select deterministic test modes; the debug-only
Homeboard probe restores `{}` afterward. It never edits the saved Homeboard layout.

`gradient.json` and `light.mp4` come from Spocky's public sample at
`60ec7b4306ab16dac83ea10ed9b81f5670b51ee3` under Apache-2.0, packaged in assets/licenses.
Other fixture code/resources are Homeboard test materials. The provider requests timer,
app-focus and launcher-idle events (mask 21). This fixture is absent from release APKs.
