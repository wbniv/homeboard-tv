# Native widget and plugin host systems: prior art for Homeboard TV

Researched 2026‑11‑19. **CONFIRMED** means read on the official page or in source code during this session. **UNVERIFIED** means a search snippet, forum post, store listing, or press summary. GitHub stars, last-push dates and licences come from `gh api repos/OWNER/REPO` on 2026‑11‑19. Anything older than about 2 years is flagged **STALE**.

## 1. Can a third-party Android TV launcher host ordinary Android app widgets (AppWidgetHost, RemoteViews, Glance), and which TV launchers do?

### Takeaway
The host contract is well documented. A launcher allocates per-instance widget IDs, gets bind permission, runs the provider's configure activity, persists size and position against each ID, and inflates `RemoteViews` built from a fixed whitelist of view classes. On Android TV, the user-facing bind step is the weak point: older reports say the system bind and pick dialogs are missing on TV builds, so a launcher needs `adb shell appwidget grantbind` or root. ATV Launcher is the only TV launcher found that advertises hosting standard Android widgets, "as supported by your device firmware". Projectivy, FLauncher and LtvLauncher show no sign of hosting them. No source was found saying Google TV's own home screen hosts third-party widgets.

### Cited Findings

**Host contract (Android framework)**
- CONFIRMED: A home-screen replacement embeds widgets by implementing `AppWidgetHost`. Google calls the host's duties "contractual obligations a host implicitly agrees to", and points to Launcher3's `LauncherAppWidgetHost` as the reference implementation. — [Build a widget host](https://developer.android.com/develop/ui/views/appwidgets/host)
- CONFIRMED: Each `AppWidgetHost` has an ID that is unique within the host's package and persists across uses. Each widget instance gets an app widget ID from `allocateAppWidgetId()`, which lasts until the widget is deleted. "Any host-specific state—such as the size and location of the widget—must be persisted by the hosting package and associated with the app widget ID." `AppWidgetHostView` is the frame the widget is inflated into, and hosts may subclass it. — [Build a widget host](https://developer.android.com/develop/ui/views/appwidgets/host)
- CONFIRMED: Binding requires `<uses-permission android:name="android.permission.BIND_APPWIDGET"/>` in the manifest, and the user must also grant permission at runtime. The host calls `bindAppWidgetIdIfAllowed()`. If that returns false, the host fires `AppWidgetManager.ACTION_APPWIDGET_BIND` with `EXTRA_APPWIDGET_ID`, `EXTRA_APPWIDGET_PROVIDER` and `EXTRA_APPWIDGET_OPTIONS`, and the user picks "allow" or "always allow". — [Build a widget host](https://developer.android.com/develop/ui/views/appwidgets/host)
- CONFIRMED: Host responsibilities:
    - Call `deleteAppWidgetId()` on removal.
    - Launch the provider's configuration activity unless the provider marks it optional with both the `configuration_optional` and `reconfigurable` flags, and "handle irregular cases where the configuration activity doesn't return or finishes with errors".
    - Lay the widget out at least at its default size, which is given in cells (`targetCellWidth`/`targetCellHeight`, Android 12+) or in dp (`minWidth`/`minHeight`).
    — [Build a widget host](https://developer.android.com/develop/ui/views/appwidgets/host)
- CONFIRMED: Sizing:
    - The host passes an options bundle that can contain a `List<SizeF>` of the dp sizes the instance may take. Phones typically send 2, foldables 4.
    - Providers may return at most `MAX_INIT_VIEW_COUNT` (16) size-mapped `RemoteViews`.
    - Hosts should respect `maxResizeWidth`/`maxResizeHeight`.
    — [Build a widget host](https://developer.android.com/develop/ui/views/appwidgets/host)
- CONFIRMED: `AppWidgetProviderInfo` details:
    - `targetCellWidth`/`targetCellHeight` (API 31) are "in units of launcher grid cells".
    - "Updates requested with updatePeriodMillis will not be delivered more than once every 30 minutes."
    - `widgetCategory` distinguishes home screen and keyguard.
    - `WIDGET_FEATURE_CONFIGURATION_OPTIONAL` means "the host may choose not to launch the provided configuration activity".
    - `WIDGET_FEATURE_HIDE_FROM_PICKER` exists.
    - `previewLayout` (API 31) gives a live preview layout.
    — [AppWidgetProviderInfo reference](https://developer.android.com/reference/android/appwidget/AppWidgetProviderInfo)
- CONFIRMED: `RemoteViews` supports only these layouts: AdapterViewFlipper, FrameLayout, GridLayout, GridView, LinearLayout, ListView, RelativeLayout, StackView and ViewFlipper. It supports only these widgets: AnalogClock, Button, Chronometer, ImageButton, ImageView, ProgressBar, TextClock and TextView, plus CheckBox, RadioButton, RadioGroup and Switch from API 31. "Descendants of these classes are not supported." — [RemoteViews reference](https://developer.android.com/reference/android/widget/RemoteViews)
- CONFIRMED: Jetpack Glance lets developers "build app widgets using Compose, but is restricted by the limitations of AppWidgets and RemoteViews". — [Create an app widget with Glance](https://developer.android.com/develop/ui/compose/glance/create-app-widget)
- CONFIRMED: `PackageManager.FEATURE_APP_WIDGETS` ("android.software.app_widgets") declares that "the device supports app widgets". — [PackageManager reference](https://developer.android.com/reference/android/content/pm/PackageManager)
- CONFIRMED (absence): The Android TV hardware-features page contains no mention of widgets or `app_widgets`, so the official TV docs are silent on whether TV devices support app widgets. — [TV hardware](https://developer.android.com/training/tv/start/hardware)
- CONFIRMED: Apps targeting Android 11 (API 30) or higher see a filtered view of installed apps. This affects `queryIntentActivities()` and similar calls, and is addressed with a `<queries>` element. Google Play treats the installed-app list as personal and sensitive user data, and `QUERY_ALL_PACKAGES` needs Play approval. This applies to any intent-action plugin discovery, Projectivy-style included. — [Package visibility](https://developer.android.com/training/package-visibility)

**Real-world Android TV behaviour**
- UNVERIFIED, **STALE** (Stack Overflow answer, 2018‑11‑11): "ACTION_APPWIDGET_PICK and ACTION_APPWIDGET_BIND are not available in AndroidTV. The only way … is to use adb shell and execute `appwidget grantbind --package <your_package_name> --user 0`". The answerer used a custom picker, then reflection, then root as fallbacks. — [SO 50968434 via Stack Exchange API](https://stackoverflow.com/questions/50968434)
- UNVERIFIED (forum, custom ATV firmware, undated): On a Khadas Android TV build, `su appwidget grantbind --package app.lawnchair.nightly` made widgets work in Lawnchair. Running it automatically at boot did not stick; only manual runs worked. — [Khadas forum](https://forum.khadas.com/t/support-widgets-on-atv-firmware/24193)
- UNVERIFIED (search summary of a 2023‑06 Stack Overflow question; the answer body could not be retrieved): A CommonsWare answer reportedly says app-widget support on Android TV is up to each launcher, and that Google chose not to add it to its default TV launcher. — [SO 76549233 (kiwix mirror)](https://kiwix.lema.org/stackoverflow.com_en_all_2023-11/questions/76549233/are-appwidgets-possible-on-androidtv)

**Which TV launchers host app widgets**
- CONFIRMED (press article, read in full; not the developer's own notes): ATV Launcher v0.1.41 lets users place widgets inside app rows from a tile's context menu. Options are "Weather, Digital Clock, System Info, Favorite App, Watch Next, Recommended, and standard Android system widgets (as supported by your device firmware)". A separate free companion app, "ATV Launcher Digital Clock Widgets", supplies 15 clock styles. — [Android Authority, ATV Launcher 0.1.41](https://www.androidauthority.com/atv-launcher-v0-1-41-update-3715420/)
- UNVERIFIED (search summary): ATV Launcher v0.1.40 (2026‑08) added a layout editor, screen breaks, icon packs, video wallpapers and new widgets. The publisher appears as "dstudio-canada" on APKMirror. — [Android Authority, 0.1.40](https://androidauthority.com/atv-launcher-pro-0-1-40-update-3703254); [APKMirror](https://www.apkmirror.com/apk/dstudio-canada/atv-launcher/)
- UNVERIFIED (Reddit threads via a mirror, around 2025‑03): Projectivy Launcher users say it "doesn't currently have widgets support". The workaround they suggest is the TV Quick Actions app. — [r/Projectivy_Launcher "widgets"](https://axebps-redlib.hf.space/r/Projectivy_Launcher/comments/1jmvjxx/widgets); [r/Projectivy_Launcher VPN widget thread](https://axebps-redlib.hf.space/r/Projectivy_Launcher/comments/1j37878/proton_vpn_widget_shortcut)
- UNVERIFIED (Play listing summary): Monet Launcher lists its own home-screen widgets (weather, clock and date, Now Playing, Bluetooth and network status) on Android TV 8.0+ and Google TV. These are built-in panels, not hosted app widgets. — [Monet Launcher on Play](https://play.google.com/store/apps/details?id=com.klevico.monet)
- CONFIRMED (gh): `Klevico/Monet-Launcher` has 334 stars, no licence, and a push on 2026‑11‑13. GitHub code search finds 0 `AppWidgetHost` references, so the repo may hold releases only. — [Klevico/Monet-Launcher](https://github.com/Klevico/Monet-Launcher)
- CONFIRMED (gh): FLauncher forks are GPL-3.0 and written in Flutter. `osrosal/flauncher` has 314 stars and a last push on 2025‑07‑27; `svrooij/flauncher` was last pushed 2021‑12‑12 (**STALE**). Code search finds 0 `AppWidgetHost` references in `osrosal/flauncher`. — [osrosal/flauncher](https://github.com/osrosal/flauncher)
- CONFIRMED (gh): `leanbitlab-org/LtvLauncher` is GPL-3.0, has 481 stars and was pushed 2026‑11‑19. It advertises its own "WiFi data usage widget" and has 0 `AppWidgetHost` references. — [LtvLauncher](https://github.com/leanbitlab-org/LtvLauncher)
- CONFIRMED (gh): Leanback on Fire (`tsynik/LeanbackLauncher`, "Google Leanback Launcher on steroids") has 690 stars, no licence detected, a last push on 2026‑02‑07, and 0 `AppWidgetHost` references. — [tsynik/LeanbackLauncher](https://github.com/tsynik/LeanbackLauncher)
- UNVERIFIED (search summary): Google TV's ambient-mode "glanceable cards" (sports scores, weather, news, podcasts) are Google's own. No third-party card or widget API for them was found in 2025–26 coverage. — [Chrome Unboxed](https://chromeunboxed.com/google-tvs-new-ambient-mode-with-proactive-personal-results-rolling-out-for-some/); [9to5Google, Oct 2026 features](https://9to5google.com/2026/10/19/google-tv-new-features-october-2026/)

### Inferences
- The Android host contract maps closely onto Homeboard's existing model. A per-instance app widget ID corresponds to keys like `widget:rain:2`, the host persists size and position the way Homeboard's JSON layout does, and cell-unit default sizes match its span model. Hosting real app widgets would add a second, OS-managed instance-ID namespace that must be kept in sync with Homeboard's JSON, including `deleteAppWidgetId` on remove or undo.
- The bind step is the main TV-specific risk. If the TV build lacks the `ACTION_APPWIDGET_BIND` dialog (reported only in 2018 and on custom firmware), a launcher can bind only after an adb or root grant. This needs on-device testing on current Google TV and Android TV builds before any conclusion is drawn.
- D-pad fit: `RemoteViews` layouts are written for phone touch. Click targets are `PendingIntent`s on whitelisted views, and nothing in the docs read promises that a third-party widget's internal views are focusable or show a focus state. A host would probably have to treat each hosted widget as one focusable tile, with OK firing the widget's root click, rather than navigating inside it.
- Provider configure activities run as the provider's own UI on the TV. They cannot take part in Homeboard's phone-QR draft, Apply and Undo flow.

### Gaps
- No current (2025–26) primary source states whether Google TV or Android TV system images declare `android.software.app_widgets`, or ship the Settings activity that handles `ACTION_APPWIDGET_BIND`. This needs `adb shell pm list features` and an intent-resolution test on real devices.
- No official guidance was found on D-pad focus inside hosted `RemoteViews`.
- Wolf Launcher, Dispatch and Sideload Launcher: no public evidence was found either way on app-widget hosting. Most of these are closed source.
- ATV Launcher's actual widget-hosting quality (bind flow, resize, focus) is known only from press summaries. Its source is not public.

## 2. Classic provider APIs where plugins are separate APKs feeding a host that renders

### Takeaway
DashClock, Muzei, Projectivy wallpaper, Wear OS complications and Smartspacer share one shape. An exported Android component (service or content provider) declares a well-known intent action and is protected by a permission. Manifest `<meta-data>` carries the name, protocol version and a settings-activity class name. The host pulls or binds for typed data and renders it itself, and the plugin can push "data changed" signals. Locale/Tasker inverts this: the plugin owns an edit activity, and the host stores an opaque bundle. Kustom has no documented third-party data-source SDK. Of the APIs examined, Muzei, Projectivy, Wear complications and Smartspacer are actively maintained; DashClock and the Locale API are dead.

### Cited Findings

**DashClock extension API** (`romannurik/dashclock`: Apache-2.0, 484 stars, last push 2017‑05‑22, not archived but **STALE / abandoned**) — [repo](https://github.com/romannurik/dashclock)
- CONFIRMED: Discovery:
    - The extension is a `Service` with intent-filter action `com.google.android.apps.dashclock.Extension`, protected by permission `com.google.android.apps.dashclock.permission.READ_EXTENSION_DATA`.
    - Required meta-data: `protocolVersion` (1 or 2) and `description`.
    - Optional meta-data: `settingsActivity`, an exported activity the host can open, and `worldReadable`, which lets "other apps besides DashClock" read its data.
    — [DashClockExtension.java](https://github.com/romannurik/dashclock/blob/master/api/src/main/java/com/google/android/apps/dashclock/api/DashClockExtension.java)
- CONFIRMED: Data model. The plugin returns `ExtensionData` and the host renders it. Fields are `visible`, `icon` (resource id), `iconUri`, `status` (max 32 characters), `expandedTitle` (max 100), `expandedBody` (max 1000), `clickIntent` and `contentDescription`. — [ExtensionData.java](https://github.com/romannurik/dashclock/blob/master/api/src/main/java/com/google/android/apps/dashclock/api/ExtensionData.java)
- CONFIRMED: Refresh:
    - `onUpdateData(reason)` is called "roughly once per hour" by default.
    - Extensions can ask for more with `setUpdateWhenScreenOn(true)` and `addWatchContentUris()`.
    - Update reasons include `UPDATE_REASON_UNKNOWN`=0, `INITIAL`=1, `PERIODIC`=2, a settings-changed reason, `CONTENT_CHANGED`=4 and `SCREEN_ON`=5.
    — [DashClockExtension.java](https://github.com/romannurik/dashclock/blob/master/api/src/main/java/com/google/android/apps/dashclock/api/DashClockExtension.java)
- CONFIRMED: IPC is versioned AIDL.
    - `IExtension`: `onInitialize(host, isReconnect)` and `onUpdate(reason)`.
    - `IExtensionHost`: `publishUpdate(ExtensionData)`, `addWatchContentUris`, `setUpdateWhenScreenOn`, and `removeAllWatchContentUris` (protocol 3).
    - A separate multi-host broker API lets other apps consume extension data through DashClock. `IDataConsumerHost` provides `listenTo(List<ComponentName>, cb)`, `showExtensionSettings`, `getAvailableExtensions` and a world-readable opt-in. `IDataConsumerHostCallback` provides `notifyUpdate(source, ExtensionData)` and `notifyAvailableExtensionChanged`.
    — [AIDL directory](https://github.com/romannurik/dashclock/tree/master/api/src/main/aidl/com/google/android/apps/dashclock/api)
- UNVERIFIED (APKMirror and XDA snippets): Chronus ships its own host for DashClock extensions (release 12.0.2). A Chronus release note says DashClock "has not been updated in 2+ years and it's extensions will no longer work properly on devices running/apps targeting Android 8+". No Chronus-specific extension SDK was found. — [Chronus on APKMirror](https://www.apkmirror.com/apk/dvtonder/chronus-home-lock-widget/chronus-home-lock-widget-12-0-2-release/chronus-home-lock-widgets-12-0-2-android-apk-download/); [Chronus XDA thread](https://forum.xda-developers.com/t/app-4-2-chronus-stylish-clock-weather-news-tasks-stocks-and-calendar-widgets.2186734/post-39048598)

**Muzei MuzeiArtProvider API** (`muzei/muzei`: Apache-2.0, 4969 stars, last push 2026‑11‑13, active) — [repo](https://github.com/muzei/muzei)
- CONFIRMED: Discovery:
    - `MuzeiArtProvider` extends `ContentProvider`.
    - The plugin declares `<provider>` with intent-filter action `com.google.android.apps.muzei.api.MuzeiArtProvider`, and is strongly recommended to require permission `com.google.android.apps.muzei.api.ACCESS_PROVIDER`.
    - Optional meta-data: `settingsActivity` (exported) and `setupActivity`, which "must be ran before the provider can be activated".
    — [MuzeiArtProvider.kt](https://github.com/muzei/muzei/blob/main/muzei-api/src/main/java/com/google/android/apps/muzei/api/provider/MuzeiArtProvider.kt)
- CONFIRMED: Data and refresh:
    - The plugin implements `onLoadRequested(initial)`, which is called when Muzei has shown all available artwork, and calls `addArtwork`/`setArtwork`.
    - "Muzei controls how often the artwork changes and the order", so the host owns scheduling.
    - Custom per-artwork actions are returned as `RemoteActionCompat` from `getCommandActions`.
    — [MuzeiArtProvider.kt](https://github.com/muzei/muzei/blob/main/muzei-api/src/main/java/com/google/android/apps/muzei/api/provider/MuzeiArtProvider.kt)
- CONFIRMED: `Artwork` fields: `title`, `byline`, `attribution`, `token`, `persistentUri`, `webUri` and `metadata`, plus id, data file and dates. — [Artwork.kt](https://github.com/muzei/muzei/blob/main/muzei-api/src/main/java/com/google/android/apps/muzei/api/provider/Artwork.kt)

**Projectivy plugin API** (`spocky/projectivy-plugin-wallpaper-provider`: Apache-2.0, 40 stars, last push 2026‑04‑28) — [repo](https://github.com/spocky/projectivy-plugin-wallpaper-provider)
- CONFIRMED (gh listing of all `spocky` repos): The only public Projectivy plugin family is the **wallpaper provider**: the template, `-overflight` (105 stars) and `-bingwallpaper` (25 stars), all Apache-2.0. No widget, row or other plugin-family repo exists under the author's account. — [github.com/spocky](https://github.com/spocky?tab=repositories)
- CONFIRMED (gh search "projectivy"): Third-party plugins are also wallpaper providers. Examples are `shortusername/projectivy-weather-wallpaper` (a weather panel drawn as a wallpaper, Apache-2.0), `raizer88/immich-projectivy-provider`, `Dak0r/Movie-Art-For-Projectivy-Plugin` (MIT) and `rfmarcos/sofadrop`. — [weather wallpaper plugin](https://github.com/shortusername/projectivy-weather-wallpaper); [GitHub search](https://github.com/search?q=projectivy&type=repositories)
- CONFIRMED: Manifest contract:
    - A service with intent-filter action `tv.projectivy.plugin.WALLPAPER_PROVIDER`.
    - Meta-data: `apiVersion` (1), `uuid` (UUID v4), `name`, `settingsActivity`, `itemsCacheDurationMillis`, and `updateMode`, a bitmask of `TIME_ELAPSED`=1, `NOW_PLAYING_CHANGED`=2, `CARD_FOCUSED`=4, `PROGRAM_CARD_FOCUSED`=8 and `LAUNCHER_IDLE_MODE_CHANGED`=16.
    — [README](https://github.com/spocky/projectivy-plugin-wallpaper-provider/blob/main/README.md); [sample manifest](https://github.com/spocky/projectivy-plugin-wallpaper-provider/blob/main/sample/src/main/AndroidManifest.xml); [Event.kt](https://github.com/spocky/projectivy-plugin-wallpaper-provider/blob/main/api/src/main/java/tv/projectivy/plugin/wallpaperprovider/api/Event.kt)
- CONFIRMED: The AIDL service is `IWallpaperProviderService { List<Wallpaper> getWallpapers(in Event event); String getPreferences(); void setPreferences(String params); }`. — [IWallpaperProviderService.aidl](https://github.com/spocky/projectivy-plugin-wallpaper-provider/blob/main/api/src/main/aidl/tv/projectivy/plugin/wallpaperprovider/api/IWallpaperProviderService.aidl)
- CONFIRMED: The plugin pushes by broadcasting `tv.projectivy.plugin.action.WALLPAPER_PROVIDER_UPDATED` with `EXTRA_PROVIDER_ID` (UUID) and optional `EXTRA_UPDATE_REASON` (`PREFS_CHANGED`=1, `DATA_CHANGED`=2). — [WallpaperProviderContract.kt](https://github.com/spocky/projectivy-plugin-wallpaper-provider/blob/main/api/src/main/java/tv/projectivy/plugin/wallpaperprovider/api/WallpaperProviderContract.kt)
- CONFIRMED: Caching and events:
    - Projectivy caches returned wallpapers for `itemsCacheDurationMillis` and rotates randomly through the cache. The stock Reddit provider uses 12 h.
    - Focus and now-playing events are uncached, and card-focused events "might be sent each second or so when a user navigates".
    - `Wallpaper` display modes include `DEFAULT`, `CROP`, `STRETCH`, `FIT_CENTER`, `FIT_CENTER_BLURRED_BG` and `BLUR`; types include `COLOR` and `ANIMATED_DRAWABLE`.
    — [README](https://github.com/spocky/projectivy-plugin-wallpaper-provider/blob/main/README.md); [Wallpaper.kt](https://github.com/spocky/projectivy-plugin-wallpaper-provider/blob/main/api/src/main/java/tv/projectivy/plugin/wallpaperprovider/api/Wallpaper.kt)

**Wear OS complications** (official, current)
- CONFIRMED: Data sources "supply raw data" to watch faces, and the watch face renders it. — [About complications](https://developer.android.com/training/wearables/complications)
- CONFIRMED: Provider contract:
    - The data source extends `SuspendingComplicationDataSourceService` and declares an update-request action intent filter.
    - The service is protected by `BIND_COMPLICATION_PROVIDER` "to ensure that only the Wear OS system can bind", and needs an `android:icon`.
    - Meta-data: `android.support.wearable.complications.SUPPORTED_TYPES` (for example `SHORT_TEXT`), `UPDATE_PERIOD_SECONDS` (for example 300) and `PROVIDER_CONFIG_ACTION` for an optional configuration activity.
    — [Expose data to complications](https://developer.android.com/training/wearables/complications/exposing-data)
- CONFIRMED: Refresh:
    - The system calls `onComplicationRequest()` when it needs data.
    - The provider can push with `ComplicationDataSourceUpdateRequester.requestUpdate()`; the docs warn against calling it often, for battery reasons.
    - `SuspendingTimelineComplicationDataSourceService` supplies a timeline of values at preset times.
    — [Expose data to complications](https://developer.android.com/training/wearables/complications/exposing-data)
- CONFIRMED: `ComplicationType` values: EMPTY, GOAL_PROGRESS, LONG_TEXT, MONOCHROMATIC_IMAGE, NOT_CONFIGURED, NO_DATA, NO_PERMISSION, PHOTO_IMAGE, RANGED_VALUE, SHORT_TEXT, SMALL_IMAGE and WEIGHTED_ELEMENTS. — [ComplicationType reference](https://developer.android.com/reference/androidx/wear/watchface/complications/data/ComplicationType)

**Locale / Tasker plugin API** (`twofortyfouram/android-plugin-api-for-locale`: **archived**, last push 2021‑07‑17, licence NOASSERTION, 19 stars, **STALE**) — [repo](https://github.com/twofortyfouram/android-plugin-api-for-locale)
- CONFIRMED: The plugin supplies an edit activity for `ACTION_EDIT_SETTING` or `ACTION_EDIT_CONDITION`, and a `BroadcastReceiver` for `ACTION_FIRE_SETTING` or `ACTION_QUERY_CONDITION`. — [README](https://github.com/twofortyfouram/android-plugin-api-for-locale/blob/master/README.md)
- CONFIRMED: `EXTRA_BUNDLE` "represents the entire state" and "MUST be less than 25 kilobytes" when serialized by the host. `EXTRA_STRING_BLURB` is a concise, human-readable summary. Plugins that need a background service "are not recommended". — [README](https://github.com/twofortyfouram/android-plugin-api-for-locale/blob/master/README.md)

**Kustom (KWGT/KLWP)** (closed source)
- CONFIRMED: Since v3.82, Kustom exposes a ContentProvider at `content://org.kustom.{app}.content/{variant}/{id}/info` (authorities `org.kustom.kwgt.content` and `org.kustom.klwp.content`) for *querying* configured widgets and wallpapers. Data flows out of Kustom, not into it. — [Kustom docs: space info provider](https://docs.kustom.rocks/docs/developers/space_info_provider/)
- UNVERIFIED (search summary of Kustom docs and forum): Third-party data reaches Kustom through a Tasker plugin action or broadcast that sets Kustom variables, through Kustom Flows fetching JSON on a schedule, or through the separate "Kustom Weather Plugin" APK, which requires KWGT or KLWP. No SDK was found for a third-party app to register as a komponent data source. — [Kustom docs: send variable](https://docs.kustom.rocks/docs/recipes/flows/sendvar/); [Kustom Weather Plugin](https://appgoblin.info/apps/org.kustom.weather)

**Smartspacer** (app GPL-3.0, **SDK Apache-2.0**, 3603 stars, last push 2026‑11‑06, active) — [repo](https://github.com/KieronQuinn/Smartspacer)
- CONFIRMED: The README describes an "extensive plugin system", an in-app plugin repository, and a client SDK for "integrating Smartspacer into your launcher". The app is GPL v3 and the SDK is Apache 2.0. — [README](https://github.com/KieronQuinn/Smartspacer/blob/main/README.md)
- CONFIRMED: A plugin "target" is a `SmartspacerTargetProvider`, a ContentProvider that must be exported. It requires `android:permission="com.kieronquinn.app.smartspacer.permission.ACCESS_SMARTSPACER_TARGETS"` and intent-filter action `com.kieronquinn.app.smartspacer.TARGET`. It implements `getSmartspaceTargets(smartspacerId): List<SmartspaceTarget>` and `getConfig()`. The per-instance `smartspacerId` is passed in. — [SDK: Creating a Target](https://github.com/KieronQuinn/Smartspacer/wiki/3.-SDK:-Creating-a-Target)

**Phone launchers**
- CONFIRMED (gh): Lawnchair has 13969 stars and was pushed 2026‑11‑19; GitHub reports its licence as NOASSERTION. — [Lawnchair](https://github.com/LawnchairLauncher/lawnchair)
- UNVERIFIED: A Smart Launcher docs page titled "Third-Party Widget Devs APIs" (`/work-with-smart-launcher/klwp-kwgt-apis`) returned "Page Not Found". — [Smart Launcher docs (404)](https://docs.smartlauncher.net/work-with-smart-launcher/klwp-kwgt-apis)

### Inferences
- Projectivy, Muzei and DashClock are near-identical "intent action + meta-data + settingsActivity" contracts. Homeboard already implements one of them (Projectivy wallpaper). A widget plugin family in the same style (for example `tv.projectivy.plugin.WIDGET_PROVIDER`, or a Homeboard-namespaced action) would reuse the discovery, `<queries>`, bind and settings-launch code Homeboard already has.
- Of the APIs read, only Wear complications publish a closed, typed vocabulary (SHORT_TEXT, RANGED_VALUE and so on) that hosts of many shapes can render. DashClock's single fixed schema (status of 32 characters plus title and body) is the simplest version of the same idea. Both let the host keep full control of typography, focus and theming, which matters for Thai text and a 10-foot UI.
- Instance identity differs across these APIs. DashClock and Projectivy key on the plugin (UUID or component), with no per-instance id. Smartspacer passes `smartspacerId`, and the Android and Windows widget APIs pass per-instance ids. Homeboard's `widget:kind:n` model needs the per-instance variant, so the same plugin can run twice with different settings.
- Settings ownership falls into two camps. In DashClock, Muzei, Projectivy and Wear, the plugin's own activity edits settings. Locale/Tasker, and Projectivy's `getPreferences`/`setPreferences` string, give the host an opaque serialized blob. Neither lets the host render the settings UI itself, so neither works directly with Homeboard's phone-browser draft editor unless the blob has a published schema.

### Gaps
- Tasker-specific extensions beyond the Locale API (variable passing, the TaskerPluginLibrary) were not researched.
- No first-party documentation was found for Niagara or Smart Launcher plugin systems. The Smart Launcher API page is a 404. Lawnchair's plugin or Smartspace-provider features were not confirmed.
- No Chronus extension SDK was found; Chronus appears only to host DashClock extensions.
- No licence was confirmed for the Wear complications AndroidX libraries in this session.

## 3. Other platforms' widget hosts relevant to a TV or ambient dashboard

### Takeaway
Apple WidgetKit has no tvOS support; tvOS offers only Top Shelf, a host-rendered content provider. WidgetKit's timeline model (entries, a reload policy, and a relevance score for Smart Stack rotation) is the most developed scheduling and ranking design found. Windows Widgets separate a declarative Adaptive Card template from JSON data, with a COM-activated provider and fixed small, medium and large sizes. Amazon's APL widgets run on Echo Show; no confirmed Fire TV widget surface was found. Google's Engage SDK and Watch Next are publish-to-system content feeds for Google surfaces, not a widget host contract. No public third-party widget API was found for Samsung Tizen ambient mode or LG webOS.

### Cited Findings

**Apple WidgetKit and tvOS**
- CONFIRMED: WidgetKit platforms are iOS 14+, iPadOS 14+, Mac Catalyst 14+, macOS 11+, visionOS 26+ and watchOS 9+. **tvOS is not listed.** — [WidgetKit](https://developer.apple.com/documentation/widgetkit)
- CONFIRMED: `WidgetFamily` sizes are systemSmall, systemMedium, systemLarge, systemExtraLarge, systemExtraLargePortrait, accessoryCircular, accessoryRectangular, accessoryInline and accessoryCorner. — [WidgetFamily](https://developer.apple.com/documentation/widgetkit/widgetfamily)
- CONFIRMED: `TimelineReloadPolicy` has three values. `atEnd` requests a new timeline after the last entry date. `after(_:)` names a future date. `never` means "the app prompts WidgetKit when a new timeline is available". — [TimelineReloadPolicy](https://developer.apple.com/documentation/widgetkit/timelinereloadpolicy)
- CONFIRMED: `TimelineEntryRelevance(score:duration:)` gives each entry a relevance `score`, plus a `duration` in seconds during which "WidgetKit considers the widget for rotation to the top of the stack". This is the Smart Stack ranking input. — [TimelineEntryRelevance](https://developer.apple.com/documentation/widgetkit/timelineentryrelevance)
- UNVERIFIED (not re-read this session): `TimelineProvider` requires `placeholder(in:)`, `getSnapshot(in:completion:)` and `getTimeline(in:completion:)`. Widgets are SwiftUI views that the system renders from timeline entries. `AppIntentConfiguration` lets the system render the configuration UI from declared intent parameters. — [TimelineProvider](https://developer.apple.com/documentation/widgetkit/timelineprovider); [AppIntentConfiguration](https://developer.apple.com/documentation/widgetkit/appintentconfiguration)
- CONFIRMED: TVServices (tvOS 9.0+) provides `TVTopShelfContentProvider`, "the main interface for your Top Shelf app extension". Content comes in three styles, `TVTopShelfCarouselContent`, `TVTopShelfInsetContent` and `TVTopShelfSectionedContent`, and the system renders the supplied items. — [TVServices](https://developer.apple.com/documentation/tvservices)

**Windows Widgets Board**
- CONFIRMED: Widgets display content from a Windows app or PWA. A provider supplies either an **Adaptive Card template plus data**, or a web widget that loads HTML from a URL; web widgets still send an Adaptive Card JSON payload naming the URL. A packaged desktop app implements a widget provider that "responds to requests from the widget host and sends updates through `WidgetManager`". A PWA declares widgets in its web app manifest. — [Widget providers](https://learn.microsoft.com/en-us/windows/apps/develop/widgets/widget-providers)
- CONFIRMED: `IWidgetProvider` covers creation, deletion, activation, context changes and user actions.
    - `CreateWidget` is called when the user pins a widget.
    - `OnActionInvoked` is called when the user triggers an action defined in the template.
    - `OnWidgetContextChanged` is currently called only on resize, and the provider may return a different JSON template for the new size.
    - `Activate`/`Deactivate` gate updates.
    - Optional interfaces add header customization.
    — [Implement a widget provider (C#)](https://learn.microsoft.com/en-us/windows/apps/develop/widgets/implement-widget-provider-cs)
- CONFIRMED: Registration is a package-manifest `uap3:AppExtension Name="com.microsoft.windows.widgets"` with `<Definitions><Definition Id=… DisplayName=… Description=… AllowMultiple="true">`, and `<Capabilities>` listing `Size Name="small" | "medium" | "large"`. — [Implement a widget provider (C#)](https://learn.microsoft.com/en-us/windows/apps/develop/widgets/implement-widget-provider-cs)

**Amazon Echo Show and Fire TV**
- CONFIRMED: Alexa skills can include APL widgets. Users install them from a Widget Gallery. "The Echo Show 15 displays a persistent Widget Panel on the home screen. On smaller devices, such as the Echo Show 8, users access widgets from the Widget Shortcuts." APL has a Data Store extension used by widgets. — [About Widgets and APL](https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/about-widgets-and-apl.html)
- CONFIRMED (absence): The Amazon widgets page does not mention Fire TV. — [About Widgets and APL](https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/about-widgets-and-apl.html)
- UNVERIFIED, **STALE** (2021 blog summary): APL widgets were announced at Alexa Live 2021 as a developer preview. — [Alexa blog, 2021](https://developer.amazon.com/en-US/blogs/alexa/alexa-skills-kit/2021/07/more-than-50-features-to-build-ambient-experiences)

**Google TV discovery APIs**
- CONFIRMED: The Engage SDK delivers "personalized recommendations and continuation content" to Google surfaces (Collections, Entertainment Space, Play Store), and "is also available for TV". — [Engage SDK](https://developer.android.com/guide/playcore/engage)
- CONFIRMED: The watch vertical has three cluster types: Recommendation, Continuation and Featured. The docs say to "check the TV integration guide to learn more about the Continue Watching experience on Google TV". — [Engage SDK watch guide](https://developer.android.com/guide/playcore/engage/watch)
- CONFIRMED: The Watch Next channel is managed with `WatchNextProgram`/`WatchNextPrograms`. Apps cannot move, remove or hide the channel. — [Add programs to Watch Next](https://developer.android.com/training/tv/discovery/watch-next-add-programs)

**Samsung Tizen and LG webOS**
- UNVERIFIED, likely **STALE** (Samsung developer forum, around 2020): Samsung Developer Relations said Tizen docs call the idle feature "screensaver" rather than "ambient", and that Ambient is a manually launched catalog feature. No published Ambient API was given. — [Samsung forum: Developing Ambient mode](https://forum.developer.samsung.com/t/developing-ambient-mode/28315)

### Inferences
- Of all the contracts surveyed, WidgetKit's design addresses scheduling and ranking most explicitly. The provider hands the host a dated list of entries plus a reload policy and relevance scores, and the host decides what to show and when, without waking the provider each time. This suits a TV that is on all day and drawing on low-power hardware.
- Windows Adaptive Cards are the clearest "plugin supplies a declarative template plus data, host renders natively" design. Because the host owns rendering, it can apply its own theme and input model. For Homeboard this is a middle path between fixed typed data (complications) and arbitrary views (`RemoteViews`).
- Engage and Watch Next are content feeds that apps publish to Google's system surfaces. ATV Launcher's "Watch Next" widget suggests a third-party launcher can read this data, but the read-side permission for third-party launchers was not confirmed.

### Gaps
- Fire TV: no confirmed developer widget API, and the Fire TV "Ambient Experience" widget surface was not documented in sources found.
- LG webOS: no search results on third-party home-screen widgets or ambient apps.
- Samsung: no official Tizen screensaver or ambient API page was read.
- The read permissions a third-party launcher needs for Watch Next or TvProvider data were not confirmed.
- WidgetKit's `TimelineProvider` and `AppIntentConfiguration` details were not re-verified this session.

## 4. Which contracts separate data provider from renderer, which let the plugin draw, and which suit a 10-foot D-pad UI?

### Takeaway
The contracts fall into three rendering models plus one settings model. **Typed data, host renders:** Wear complications, DashClock, Muzei, Projectivy wallpaper, Smartspacer, tvOS Top Shelf, Engage and Watch Next. **Declarative template plus data, host renders:** Windows Adaptive Cards, Amazon APL widgets, and, at a stretch, WidgetKit's system-rendered SwiftUI. **Plugin draws its own views:** Android `RemoteViews`/Glance app widgets, Kustom (itself an app-widget provider), and Windows web widgets. For settings, plugins either own an activity or hand the host an opaque blob. Only the template and typed-data models let the host guarantee focus rings, sizing and theming on a D-pad TV.

### Cited Findings
- CONFIRMED: Wear complication data sources "supply raw data" and watch faces render it. — [About complications](https://developer.android.com/training/wearables/complications)
- CONFIRMED: DashClock `ExtensionData` is a fixed schema with length caps (status 32, title 100, body 1000 characters). — [ExtensionData.java](https://github.com/romannurik/dashclock/blob/master/api/src/main/java/com/google/android/apps/dashclock/api/ExtensionData.java)
- CONFIRMED: Projectivy plugins return `Wallpaper` parcels, and Projectivy applies display modes and caching. — [README](https://github.com/spocky/projectivy-plugin-wallpaper-provider/blob/main/README.md)
- CONFIRMED: Windows providers send an Adaptive Card template plus data, or a web widget URL. — [Widget providers](https://learn.microsoft.com/en-us/windows/apps/develop/widgets/widget-providers)
- CONFIRMED: Android widgets ship `RemoteViews` restricted to a view whitelist, and Glance is "restricted by the limitations of AppWidgets and RemoteViews". — [RemoteViews](https://developer.android.com/reference/android/widget/RemoteViews); [Glance](https://developer.android.com/develop/ui/compose/glance/create-app-widget)
- CONFIRMED: Windows `OnWidgetContextChanged` lets the provider return a different template per size. Android providers may map up to 16 `RemoteViews` to sizes. — [Windows C# provider](https://learn.microsoft.com/en-us/windows/apps/develop/widgets/implement-widget-provider-cs); [Build a widget host](https://developer.android.com/develop/ui/views/appwidgets/host)
- CONFIRMED: Projectivy sends focus events to plugins (`CARD_FOCUSED`, about once per second while navigating). This is the only TV-native contract found that feeds D-pad focus state to a plugin. — [README](https://github.com/spocky/projectivy-plugin-wallpaper-provider/blob/main/README.md)

### Inferences
**Summary table** (synthesised from the confirmed findings above):

| Contract | Discovery | Payload | Who renders | Per-instance id | Config handoff | Refresh | Status |
|---|---|---|---|---|---|---|---|
| Android AppWidget / Glance | `AppWidgetManager` provider list | `RemoteViews` (view whitelist) | Plugin layout, inflated in host | yes (appWidgetId) | provider configure activity | ≥ 30 min periodic, or push | active |
| Wear complications | action + `BIND_COMPLICATION_PROVIDER` | typed `ComplicationData` | host (watch face) | yes | `PROVIDER_CONFIG_ACTION` activity | `UPDATE_PERIOD_SECONDS`, push, timeline | active |
| DashClock | action + read permission | `ExtensionData` fixed schema | host | no | `settingsActivity` | ~1 h, screen-on, content URI | dead (2017) |
| Muzei | ContentProvider + action | `Artwork` rows | host | no | `settingsActivity`/`setupActivity` | host-scheduled `onLoadRequested` | active |
| Projectivy wallpaper | action + meta-data | `Wallpaper` parcels | host | no (UUID per plugin) | `settingsActivity` + prefs string | host cache + event mask + broadcast | active |
| Smartspacer | ContentProvider + action + permission | `SmartspaceTarget` | host | yes (`smartspacerId`) | plugin config | plugin-notified | active |
| Locale/Tasker | edit activity + receiver | opaque bundle < 25 KB | n/a (actions) | per use | plugin edit activity | host fires | archived |
| Windows Widgets | appx extension + COM | Adaptive Card JSON template + data | host | yes (widget id) | customization interface | push via `WidgetManager` | active |
| Apple WidgetKit | app extension | SwiftUI view per timeline entry | system | yes | App Intents (system UI) | timeline + reload policy + relevance | active, no tvOS |
| tvOS Top Shelf | app extension | carousel/inset/sectioned items | host | no | none | system pulls | active |
| Amazon APL widgets | Alexa skill | APL document + datasources | host (APL runtime) | yes | skill | Data Store extension | Echo Show only |

- **10-foot D-pad fit.** When the host renders typed data or a declarative template, it keeps focus traversal, focus rings, overscan-safe sizing, font scale and Thai shaping under its own control. A plugin then cannot create focus traps or touch-only targets. When the plugin draws (`RemoteViews`), the host gets phone-designed layouts with no guaranteed focus behaviour, and its realistic option is to make each widget a single focusable tile.
- **Settings fit with Homeboard's phone-QR editor.** Only contracts where the host renders the settings UI from a declared schema can join a host-owned draft, Apply and Undo flow. Of the confirmed contracts, none has such a schema; Apple's App Intents parameters are the closest, but are unverified. Every Android plugin API read hands configuration to a plugin-owned activity on the TV itself.
- **Expanded view.** Homeboard's compact tile plus full-screen view resembles DashClock's collapsed status versus expanded title and body, and Windows' per-size templates. No surveyed contract defines a separate "expanded full-screen" payload; all of them treat a click as an intent or deep link into the app.

### Gaps
- No published usability study comparing these models on a D-pad TV was found.
- Real-world focus behaviour of hosted `RemoteViews` in ATV Launcher is undocumented.

## 5. Open-source Android widget hosts or launcher grids Homeboard could reuse (licence-compatible with MIT)

### Takeaway
Most maintained open-source launchers that host widgets are GPL-3.0: Fossify Launcher, Kvaesitso, KISS, Neo Launcher, Simple Launcher, the FLauncher forks and LtvLauncher. Their code cannot be copied into an MIT codebase without relicensing the combined work. The permissive options found are Google's Launcher3 `LauncherAppWidgetHost` reference (AOSP), `farmerbb/Taskbar` (Apache-2.0, has an `AppWidgetHost`-based "Dashboard", last push 2024‑11), and the Smartspacer SDK (Apache-2.0, a plugin protocol rather than a host). No open-source TV launcher found hosts Android app widgets.

### Cited Findings
- CONFIRMED: Google names Launcher3's `LauncherAppWidgetHost` as the example host implementation. — [Build a widget host](https://developer.android.com/develop/ui/views/appwidgets/host); [LauncherAppWidgetHost source](https://cs.android.com/android/platform/superproject/+/master:packages/apps/Launcher3/src/com/android/launcher3/widget/LauncherAppWidgetHost.java)
- CONFIRMED (gh): `farmerbb/Taskbar` is Apache-2.0, has 1303 stars, was last pushed 2024‑11‑21 (about 2 years ago, borderline **STALE**), and is not archived. Code search finds `AppWidgetHost` in `activity/DashboardActivity.java` and `ui/DashboardController.java`. — [farmerbb/Taskbar](https://github.com/farmerbb/Taskbar)
- CONFIRMED (gh): GPL-3.0 launchers with an `AppWidgetHost` implementation:
    - `FossifyOrg/Launcher`: 739 stars, pushed 2026‑11‑18, `MyAppWidgetHost.kt`.
    - `MM2-0/Kvaesitso`: 5402 stars, pushed 2026‑11‑13, `AppWidgetHost.kt`.
    - `SimpleMobileTools/Simple-Launcher`: 150 stars, pushed 2024‑06‑11, `MyAppWidgetHost.kt` and `HomeScreenGrid.kt`.
    - `NeoApplications/Neo-Launcher`: 2166 stars, Launcher3-derived.
    - `Neamar/KISS`: 3547 stars.
    — [Fossify Launcher](https://github.com/FossifyOrg/Launcher); [Kvaesitso](https://github.com/MM2-0/Kvaesitso); [Simple Launcher](https://github.com/SimpleMobileTools/Simple-Launcher); [Neo Launcher](https://github.com/NeoApplications/Neo-Launcher); [KISS](https://github.com/Neamar/KISS)
- CONFIRMED (gh): Smartspacer's app is GPL-3.0 and its SDK is Apache-2.0. — [Smartspacer README](https://github.com/KieronQuinn/Smartspacer/blob/main/README.md)
- CONFIRMED (gh): Small `AppWidgetHost` samples exist, but all lack a licence: `lgfischer/WidgetHostExample` (39 stars, 2012), `joakime/android-appwidget-toy` (2011) and `halifox/android_app_widget_host` (2023). All are **STALE**, and code without a licence is not reusable. — [WidgetHostExample](https://github.com/lgfischer/WidgetHostExample); [android-appwidget-toy](https://github.com/joakime/android-appwidget-toy); [android_app_widget_host](https://github.com/halifox/android_app_widget_host)
- CONFIRMED (gh): Permissively licensed TV launchers show no app-widget hosting. `CVasilakis/luncher` (MIT, 48 stars, pushed 2026‑11‑08) and `0x006E/cleantv` (Apache-2.0, 7 stars) both have 0 `AppWidgetHost` hits. `tsynik/LeanbackLauncher` has no detected licence. — [luncher](https://github.com/CVasilakis/luncher); [cleantv](https://github.com/0x006E/cleantv)
- CONFIRMED (gh): Wallpaper plugins in the Projectivy ecosystem are mostly Apache-2.0 or MIT, for example `spocky/*` (Apache-2.0) and `Dak0r/Movie-Art-For-Projectivy-Plugin` (MIT). — [spocky repos](https://github.com/spocky?tab=repositories); [Movie-Art plugin](https://github.com/Dak0r/Movie-Art-For-Projectivy-Plugin)
- CONFIRMED (gh): Other plugin API sources: DashClock API and Muzei API are both Apache-2.0. The Locale plugin API reports NOASSERTION and is archived. — [dashclock](https://github.com/romannurik/dashclock); [muzei](https://github.com/muzei/muzei); [locale API](https://github.com/twofortyfouram/android-plugin-api-for-locale)

### Inferences
- These are general licence principles, not legal advice. Apache-2.0 code (AOSP Launcher3, Taskbar, the Muzei, DashClock and Smartspacer SDKs, Projectivy's API module) can be included in an MIT-licensed APK if its licence and NOTICE text are kept. Copying GPL-3.0 code (Fossify, Kvaesitso, FLauncher and others) would make the distributed combination GPL-3.0. GPL projects can still be read as design references without copying code. Repos with no licence are all-rights-reserved.
- Being compatible with an existing plugin ecosystem, rather than reusing its code, raises no licence issue when the API module is Apache-2.0 (Projectivy, Muzei, DashClock, Smartspacer SDK). Homeboard already depends on the Projectivy API in this way.
- Homeboard's own paged grid, D-pad nearest-cell focus and Move/Resize/Undo already cover what launcher-grid libraries provide. The reusable piece, if Android app widgets were hosted, would be a small `AppWidgetHost` wrapper of the Launcher3 or Taskbar kind, not a grid.

### Gaps
- The AOSP Launcher3 licence was not re-read this session; the LineageOS Trebuchet mirror shows "none" in GitHub's licence detector. The Apache-2.0 status of AOSP packages is assumed, not verified here.
- Taskbar's Dashboard widget hosting was confirmed only by code-search hits, not by running it or reading the code. Its D-pad and TV behaviour is unknown.
- No open-source Android TV launcher with app-widget hosting was found. ATV Launcher, the one TV launcher that advertises it, is closed source.
