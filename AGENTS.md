# Homeboard TV contributor instructions

Homeboard TV is an independent dashboard project based on CleanYourTV. Read README.md,
docs/plans/2026-11-15-project-foundation.md and docs/upstream/README.md first.

- Preserve the Java launcher and prefer small native integrations with minimal setup.
  AirBKK is fetched directly; dashboard systems and a custom bridge are optional future choices.
  Kotlin is a language, not a dashboard product. Do not embed household credentials.
- Preserve upstream licenses and track changes against the pinned import.
- No legacy compatibility layers without a concrete requirement.
- All compilation, tests, lint and packaging run in containers. Follow
  [the build how-to](docs/how-to/build.md) and the workspace standard at
  `/home/will/docs/howtos/containerized-build-environment.md`.
- Use `task build-image`, `task check`, `task build` and `task bundle`. Never install
  Java/Android tooling on the host or run Gradle there. Never install via Gradle or raw ADB.
- Shared device sessions must follow [the Chromecast how-to](docs/how-to/chromecast.md):
  devices, queue, complete submitted job, watch, evidence. No direct-device fallback.
- The native widget Home, All Apps and public Bangkok air-quality card are implemented.
  Wallpaper-provider v1 hosting and independent UI opacity are implemented; consult
  [the wallpaper how-to](docs/how-to/projectivy-wallpapers.md) and testing record for
  verified behavior and remaining compatibility limits.
  Other network integrations, scheduled scenes and Play release signing remain future work.
- Keep mockups and the workflow diagram current before implementing a new UI workflow.
- Upstream contributions need their own narrowly scoped patch and upstream requirements review.
  Do not send issues, PRs or messages unless authorized.
- If AI attribution is required for a contribution, obtain actual tool/model/effort metadata;
  never guess versions. This independent project does not require guessed attribution trailers.

## Will's iterative TV testing

Keep iterative Homeboard testing on Cast1 (`chromecast-test-01`) while Will is
still testing the app. After Will says it is usable, move iterative testing to
Cast2 (`chromecast-test-02`). Install each working APK through the shared
coordinator and preserve app data and settings. Submit requested installs after
the devices/queue checks even when a personal-use marker is present: the
coordinator owns admission and supports background installs during personal use.
Let it defer any foreground launcher verification. Do not add an agent-side
reservation gate or change the default Home app merely to install a build.

Every new deployment must advance the Android APK version code. Use `task deploy`
for the bump/check/build/freeze/coordinator workflow; `task bump` increments the
integer in `version.properties` through the build container. The APK version name
and About dialog must use `major.minor.version_code`. Never reuse an earlier
deployment number, including after a failed attempt. See the Chromecast how-to.
Additional target devices in the same deployment use the identical frozen APK
and version; a new build or source revision requires a new number.

Parallel worktrees replace the same installed application. Merge the intended
features before a combined build and use one deployment owner; compare version
codes across all participating worktrees. See the parallel-worktree and
background-install sections in the Chromecast how-to. A higher About version
does not establish that another branch's features were included.
