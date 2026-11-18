# Weather Wallpaper 2.11: Play Protect block and source review

Date: 18 November 2026. Decision: **keep this provider blocked; do not bypass Play
Protect or retry its TV installation**. This is a limited static/source review,
not a malware clearance or a reproduction of Google's detection system.

## What the warning means

Will reported Cast1 displaying **Harmful app blocked — Weather Wallpaper — This
app may be harmful…** during the installation test. Google's
[warning guidance](https://developers.google.com/android/play-protect/warning-dev-guidance)
distinguishes that warning from the separate unknown-app scanning prompt. It
describes the harmful-app warning as a potentially harmful application / unwanted
software classification. False classifications can be appealed by the developer,
but this research has not established that this is one.

The fresh coordinator screenshot `J‑371082ca27bb` completed after recovery and
shows Google TV's stock launcher, Homeboard selected. It does not show the prior
warning; Will's report is the evidence of its wording. Capture:
`docs/diagnostics/chromecast-capture-J-371082ca27bb/screenshot.png`.

## Exact artifact and provenance

- Package `tv.projectivy.plugin.wallpaperprovider.weather`, version 2.11/code 34.
- [Official release v2.11](https://github.com/shortusername/projectivy-weather-wallpaper/releases/tag/v2.11),
  published 13 November 2026. Its versioned and fixed-name APK assets have the same
  published digest, matching the downloaded 10,046,328-byte APK exactly:
  `a1d5da3ea9374efcaf36e54db00beebc48fbce1d951e9135d2b537b81aa0fed6`.
- APK signing certificate SHA-256:
  `898639f9c22f9ef4ca946f00d63fc3e2f83ce255b43dd061ae4a8146122bf753`.
  Subject: `CN=Weather Wallpaper, O=shortusername, C=US`.
- Exact source tag resolves to `7d28ae3`; downloaded source ZIP SHA-256:
  `d652f5b23fcd84688fc71a5d3f7a7b72a60a7c98046f73f23bc73da5067cd997`.

This establishes that the blocked file is the publisher's release artifact,
not that the publisher or artifact is safe. No independent malware scan result
for this hash was established: public hash searches found none, and VirusTotal's
report could not be retrieved. The file was not uploaded to a scanning service.

## Findings from the pinned source and APK manifest

1. **A genuine privacy concern in source:** the service is exported with no
   caller permission, `onBind` returns its binder without a caller check, and
   `getPreferences()` returns saved latitude/longitude, place labels and saved
   locations. An installed app able to bind explicitly could therefore request
   those settings without holding a location permission. The optional API key is
   deliberately excluded from the exported preferences; do not claim it leaks.
   `setPreferences()` similarly accepts callers without authentication, allowing
   settings modification. This is a source-level finding, not an exploit run on
   Cast1, and Google's actual detection reason is unknown.
   [Service](https://github.com/shortusername/projectivy-weather-wallpaper/blob/v2.11/weather/src/main/java/tv/projectivy/plugin/wallpaperprovider/weather/WallpaperProviderService.kt),
   [preferences](https://github.com/shortusername/projectivy-weather-wallpaper/blob/v2.11/weather/src/main/java/tv/projectivy/plugin/wallpaperprovider/weather/PreferencesManager.kt),
   [manifest](https://github.com/shortusername/projectivy-weather-wallpaper/blob/v2.11/weather/src/main/AndroidManifest.xml).
2. **Development build distributed to users:** the APK is debuggable. The official
   CI explicitly builds `assembleDebug`, optionally using a stable private signing
   key. A debug build is not evidence of malware, but it is a security hardening
   weakness in a distributed app. This APK is signed with the custom certificate
   above; calling it an ordinary generated Android debug certificate would be
   inaccurate.
   [Build workflow](https://github.com/shortusername/projectivy-weather-wallpaper/blob/v2.11/.github/workflows/build.yml).
3. **In-app APK updater:** manifest permissions include Internet and
   `REQUEST_INSTALL_PACKAGES` plus the app's signature-protected receiver permission.
   No SMS, microphone, contacts, accessibility or device-administration permission
   is declared. The reviewed updater checks this repository's GitHub release API,
   downloads an APK, checks its size and invokes Android's installer. Scheduled
   checks only check availability; Android/user confirmation is still required
   for installation. The updater does not verify a published APK digest itself;
   Android enforces matching package signatures for updates. None of this proves
   that the updater caused Google's warning.
   [Updater](https://github.com/shortusername/projectivy-weather-wallpaper/blob/v2.11/weather/src/main/java/tv/projectivy/plugin/wallpaperprovider/weather/UpdateChecker.kt).
4. **No clear malicious payload identified in this limited review.** The APK has
   six DEX files and no bundled native libraries. Extracted literal URL hosts align
   with those in the tagged source: weather/map/data services, GitHub, Unsplash,
   IP geolocation, a CDN and donation link, plus library/documentation domains.
   The source's `Runtime.exec` use is a diagnostic logcat command, not by itself
   evidence of arbitrary shell execution. String searches are not a complete
   behavioral analysis and cannot establish safety.
5. **Homeboard incompatibility in source:** image read grants target only
   `com.spocky.projengmenu`. Homeboard uses another package. Even if Play Protect
   were cleared, those grants need compatibility work before expecting rendered
   images to load. This is separate from the malware classification.

The maintainer's public issue list (all states, retrieved through GitHub's API)
contains two bug reports and three translation PRs, with no Play Protect report
or resolution found. Broad searches did not establish a verified false-positive
report for this specific provider/version. Reports about other plugins are not
evidence about Weather Wallpaper.

## Disposition and remaining evidence

The original Weather install `J‑48a28c785c0b` is **unverified**, with an unresolved
installer commit during cleanup. Do not call it installed or successfully tested.
The automated provider install batch was stopped; no protection was disabled and
no Install anyway action was selected.

A container build of the unmodified pinned source completed. All six DEX files,
the compiled manifest, resources and every other common ZIP entry matched the
official APK byte-for-byte except `META-INF/CERT.RSA` (the signing certificate).
There were 1,218 identical entries, one differing entry, and no entries unique to
either APK. Evidence: `weather/source-build-comparison.json` under the artifact
root. This strongly establishes source correspondence for the executable payload;
it is not a clean malware verdict. The rebuilt APK will not be installed to evade
the warning.

The exposed saved-location/settings interface is therefore a concrete privacy /
integrity concern in the matching code, although no exploit was run. It does not
establish Google's detection reason or deliberate malicious intent. Recommendation:
exclude this provider from the campaign. An independent security review and/or a
documented maintainer/Google resolution would be needed before reconsidering this
release. Will does not have further warning details; no more TV investigation is
required from him. No maintainer contact or appeal was sent.
