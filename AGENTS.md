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
coordinator and preserve app data and settings. Do not use Cast2 while it is
reserved for personal use. Do not change the default Home app merely to install
a build.
