# Homeboard TV

A personal Google TV home screen, with scheduled dashboard scenes planned, built from CleanYourTV's
Java launcher. Start with small native cards backed directly by public APIs; evaluate
a dashboard engine or custom bridge when a feature actually needs one.

**Current state: native widget Home and All Apps.** A Bangkok air-quality card reads AirBKK's
public station feed directly, with no account, Home Assistant, MagicMirror or custom
server. PM2.5 is labeled as a 24-hour station average. Choose from five Bangkok
stations using the remote; Lumpini Park is the default. See the
[air-quality implementation plan](docs/plans/2026-11-15-air-quality.md).

The broader dashboard engine remains undecided. Calendar, traffic and other
integrations are future work. Device validation status is recorded in the plan.

## Home interaction

[Widgets-first Home plan and mockups](docs/plans/2026-11-15-widget-home.md):
custom widgets and app favorites, including an entirely empty dashboard; moving
right reveals All Apps. Hold OK offers Move, Favorite, Delete tile and Uninstall app.
The native implementation supports saved empty Home, app favorites, separate app ordering,
move previews, tile deletion/restoration, Android uninstall confirmation, palettes and reduced motion.
See the plan for device validation and remaining limits.

[Google TV app icons and glowing focus](docs/plans/2026-11-16-google-tv-app-focus.md)
documents the circular artwork, focus ring and halo reference, implementation
and device evidence. Use `task deploy` for a newly numbered APK deployment;
the About dialog displays the installed APK version.

Wallpaper-provider hosting, UI opacity and dense All Apps are described in the
[Projectivy wallpaper how-to](docs/how-to/projectivy-wallpapers.md). The host uses
the public v1 API; it does not need Projectivy Launcher. See the
[implementation plan](docs/plans/2026-11-16-projectivy-plugin-host.md) for
compatibility scope and validation status.

[Home favorite sizes and dense layouts](docs/plans/2026-11-16-home-favorite-app-sizes.md)
adds **Home settings → Appearance → Icons → Home favorites → Small icons / Banners**.
Home and All Apps each use a compact icon grid or a separate banner grid with
wider cards and taller rows. Their artwork settings are independent. Auto fits
the available space while retaining readable labels and focus clearance; Undo
restores Home's previous style and placement.

The combined **0.1.14** deployment includes Home favorite sizes, dense icon/banner
grids, Projectivy wallpaper hosting and independent UI opacity. Cast1 checks
confirmed both Home styles and Undo, All Apps density, Appearance controls and
the six-media wallpaper probe. UI opacity now changes colored backgrounds only;
text and artwork remain opaque. The 0.1.14 comparison verifies 0%, 75% and 100%
on both Home and All Apps. See the [combined validation record](docs/testing/2026-11-16-projectivy-plugin-host.md),
including the recorded cold-launch timeout and successful retry.

## Start here

- [Product direction: our brand and the CleanYourTV community](docs/product-direction.md)

- [User quick start with illustrations](docs/how-to/quick-start.md)

- [Existing dashboard integration comparison](docs/investigations/2026-11-15-dashboard-systems-for-homeboard.md)

- [Project plan and mockups](docs/plans/2026-11-15-project-foundation.md)
- [Investigation and community strategy](docs/investigations/2026-11-15-tv-dashboard-launcher-comparison.md)
- [Build instructions](docs/how-to/build.md)
- [Shared Chromecast workflow](docs/how-to/chromecast.md)
- [Upstream source and contribution strategy](docs/upstream/README.md)
- [Bridge boundary](bridge/README.md)

```sh
task build-image
task check
task build
task --list
```

Docker with Compose and Task are the host requirements. Java 17 and Android SDK
platform 34/build-tools 34.0.0 live in `Dockerfile.build`; compilation, tests and
packaging run in that container locally and in CI. Dependencies persist in a named
volume. See the build how-to for image overrides and rootless Docker behavior.

The application ID is `net.biohack.homeboard`; debug builds use
`net.biohack.homeboard.debug`. The imported Java namespace remains `com.gathin.tvhome`
to keep upstream diffs focused. Release output is unsigned until production signing is designed.
The name and package identity are provisional until publication.

## Community contributions

Keep Homeboard-specific features here. Contribute independently reproducible launcher
fixes upstream; prepare a showcase entry once a public, usable project is ready.
The Hebrew-layout correction fixes our card addition and needs no upstream patch.
See the [contribution strategy](docs/upstream/README.md).

## License

The imported CleanYourTV source is MIT-licensed, copyright 2026 Gathin; see [LICENSE](LICENSE).
New Homeboard TV code is offered under the same MIT terms. Bundled font terms and other notices
remain in their original files; see [third-party notices](THIRD_PARTY_NOTICES.md).
