# Projectivy wallpaper providers in Homeboard

Homeboard implements the public Projectivy wallpaper-provider v1 interface.
Projectivy Launcher itself is not required. Open **Home settings → Appearance →
Background → Get providers** for Google Play links, community publisher pages and
phone QR codes. Providers are separate apps; Homeboard does not bundle or install
their APKs. [Installation guide and exclusions](provider-directory.md).

After installing a provider, use **Home settings → Appearance →
Background → Images & videos · Projectivy**. Choose a provider, select **Preview**, and
choose **Use background** after usable media appears. Back discards the preview.
Undo restores the previous Homeboard background selection.

Preview temporarily hides dashboard widgets and the palette backdrop, removes the
menu scrim, and puts directional controls on the right so the wallpaper remains
visible on the left. Saved widget opacity stays unchanged. Back cancels the draft;
Down chooses Use background and restores the dashboard.
In Appearance, **Up opens Icons** and **Down opens Background**.

**Background → Solid color** opens the palette picker. Move to preview a solid
color; OK or Back returns to the invoking screen. Source and color changes share
one Undo entry, so Undo can restore a previous provider and its palette together.
**Background status & credits** shows the active source, palette or provider status/credits,
and widget-background opacity. Its Choose color and Choose provider buttons open
those screens; Back returns to Details and then the invoking Background menu.
**Retry provider** appears only with a saved provider. It refreshes that source
and opens Details, which updates when loading succeeds or fails.

**Plugin settings** opens the provider's own exported settings activity when it
has one. Homeboard refreshes on return. Provider preferences belong to that app;
Homeboard does not import, reset or back them up. **More options** controls
wallpaper rotation (default 15 minutes) and wallpaper dimming (default 35%).
These settings are separate from the cache lifetime requested by the provider.

**Appearance → Widget background opacity** changes only widget card fills on
Home and the fixed World clock on All Apps. Text, artwork, focus borders and
Settings controls stay opaque. **0%** makes card interiors transparent; **100%**
makes cards opaque. Wallpaper/video stays visible at its configured brightness
between cards at every setting. The slider does not control the wallpaper or its
independent dimming. Done saves one undoable change; Back cancels the draft.
On Home/All Apps, hold Back for one second to undo the saved change. Pie controls
keep their own 95% background opacity.

**Appearance → Icons** contains **Home favorites**, **All Apps artwork** and **Circular icon mask**.
New layouts use dense icons: a typical TV viewport fits at least 36 icon slots, with the exact count
adapting to font metrics. Banners use their own wider, taller responsive grid. An explicitly saved banner preference is retained;
choose Launcher icons under All Apps artwork to change it. Grid dimensions adapt
to the viewport and artwork shape. Saved Home widgets and app ordering are retained.

## Supported contract and limits

The pinned public API is in `launcher/projectivy-api/`, with its upstream revision,
file hashes, original Kotlin parcel classes, AIDL and Apache-2.0 license. Host code
remains Java. Kotlin 2.1.21 is scoped to the API module and is compatible with the
existing Gradle 8.9 / AGP 8.5.2 combination according to the
[Kotlin compatibility table](https://kotlinlang.org/docs/gradle-configure-project.html).

- Timer batches, app-focus events and launcher-visible idle events are supported.
  Idle begins after 60 seconds without interaction. Actual app artwork colors,
  label and package are supplied on app focus; native widgets are not program cards.
- Now-playing and program-focus sources are unavailable. Providers requesting
  these show partial support; providers without a usable trigger are unavailable.
- Color, image, drawable, animated drawable, Lottie and muted video renderers are
  included. On Android 8.0/8.1, bitmap animations display a still image. Reduced
  motion stops animation; video falls back to a solid background.
- Static display modes cover crop/default, stretch, fit, low-resolution soft blur,
  and fit with a softened background. Lottie/video support crop/default, stretch
  and fit. Full blur for motion requires Android 12; motion with a blurred fit
  background is currently reported as unsupported and the next usable item is tried.
- Resources in the provider APK, accessible content URIs and HTTPS are supported.
  HTTP media URLs are upgraded to HTTPS, except for the public Apple media
  routes used by Overflight on exactly `sylvan.apple.com` and `a1.phobos.apple.com`;
  a default port of 80 is removed and escaped paths/queries are preserved. Image
  redirects remain on HTTPS and have a fixed limit. There is no plaintext
  downgrade if TLS fails. The two Apple domains have a scoped Android cleartext
  allowance; all other domains remain denied. Private file paths, plaintext-only
  media on other hosts and inaccessible content-provider data are unsupported.
- Metadata credit appears in **Background status & credits**. Wallpaper links open only
  after **Open wallpaper link**; local-file, content, script, data and raw intent
  links are rejected. App links use a fresh browsable VIEW intent without grants.
- Wallpaper backgrounds do not provide interactive grid widgets or receive
  dashboard layout geometry. Leave room for information drawn into backgrounds.

Requests have a five-second binding deadline and a ten-second IPC deadline.
A remote Binder call cannot be cancelled by a Java timeout. The host uses one
process-wide IPC worker and refuses new calls while it remains stuck; navigation
and solid backgrounds still work. Retry after the provider recovers, or restart
Homeboard. Image/resource decoding also uses a bounded process-wide worker and a
ten-second display deadline. No additional worker threads accumulate on repeated
retry. Providers are not force-stopped.

The host accepts up to 1,000 descriptors per batch, matching Overflight's published
maximum, and loads one item at a time. Byte-backed images/animations have a 16 MiB
input limit and images are decoded for at most a 1920 × 1080 target. Unsupported
or failing items are tried in order until a usable one is found. Empty lists retain
the previous wallpaper. No disk media cache or background wallpaper service runs
while Homeboard is hidden. Package changes and refresh hints invalidate selection
state; missing providers use the palette fallback.

## Build and device verification

Run `task check`, `task build`, and `task run -- ./gradlew --no-daemon
:app:assembleRelease` in the established container workflow. The minified build
uses the original API consumer keep rules. `task wallpaper-fixture` builds the
provider library used only by debug APKs. Release APKs exclude its service, raw
sample media and the debug probe activities.

Debug builds include `WallpaperProbeActivity`, an offline assertion screen that
uses a provider in another process. It checks API parcel fields and preferences,
timer/app-focus/idle events, all six media types, UI/pie alpha separation and a
hung-provider timeout. It restores fixture preferences and never changes the
saved dashboard layout. `AllAppsProbeActivity` opens the actual All Apps surface
for remote-driven visual checks. `OverflightProbeActivity` checks an independently
installed Overflight provider without changing its configuration.

Run probes only through complete jobs in the [shared coordinator workflow](chromecast.md).
Use the same frozen APK and version for all checks of a deployment; register each
probe activity sequentially under `APP=homeboard-tv`, with `REPLACE=true`, then
restore the normal MainActivity registration. The catalog permits one identifier
per package. Observe devices/queue, submit,
watch and export evidence. A completed job is not an app assertion: inspect the
probe screenshot for **ALL CHECKS PASSED** or an explicit failure and inspect the
actual menu screenshots. Keep iterative testing on Cast1 until Will says Homeboard
is usable.

A worktree's Gradle cache can have a different debug signing key. Before the first
installation from a new worktree, copy the existing Homeboard debug key between
build-cache volumes without exporting it to the repository. Never uninstall the
user's Homeboard to work around a signing mismatch.

Check the offline probe assertion screen with
`python3 dev/verify-wallpaper-evidence.py /absolute/evidence/directory`.
The oracle verifies both a successful coordinator receipt and the expected text
in the exported screenshot. `--expect-text unobtainium` must fail, proving that a
completed job alone is insufficient. Passive probes use timed checks without a
capture-only input script, which the installed key helper rejects.

Video preview readiness is reported only after Android signals the first rendered
video frame, rather than merely finishing header preparation. The initial frame
also has a ten-second deadline. Use provider quality settings appropriate to the
device; a Chromecast HD cannot be assumed to decode a provider's 4K HDR selection.

`UiBackgroundProbeActivity` draws the actual saved Home and All Apps at 0%, 75%
and 100% over an offline image. Its comparison screen checks the rendered
backdrop pixels, panel fills, foreground alpha, focus borders and inactive focus
state without saving an opacity or layout draft. Run `dev/device/ui-background-probe.json`
with this debug entry point, inspect **ALL CHECKS PASSED**, export evidence, and
restore the normal MainActivity registration.

`BackgroundMenuProbeActivity` exercises the actual Home/All Apps menus using
Activity key dispatch, Android focus search for unhandled native navigation, and
the offline provider. It isolates launcher and air-widget preferences, leaving
Will's saved configuration and real providers untouched. The suite covers the
color picker, native Details buttons and child screens, grouped Undo, provider
preview/apply, rotation/dimming return paths and Retry status. Use
`dev/device/background-menu-probe.json`, export evidence, check the assertion
screen and restore MainActivity registration. Physical remote recipes separately
verify the visible menu destinations.
