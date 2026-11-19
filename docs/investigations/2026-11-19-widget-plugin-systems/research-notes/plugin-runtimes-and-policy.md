# Plugin runtimes, sandboxing, distribution and Google Play policy for an Android TV widget host

Retrieved 2026‑11‑19. **CONFIRMED** means read on the official page or primary source at the cited URL. **UNVERIFIED** means it came from a search snippet, secondary press, or memory and was not read on a primary source. GitHub figures (stars, last push, licence) came from `gh api repos/OWNER/REPO` on 2026‑11‑19. developer.android.com pages show a "Last updated" footer, which is recorded where shown. Play Console Help pages show no last‑updated date.

## 1. Execution models for third‑party widgets in a native Android (TV) app, with real examples

### Takeaway
There are six models. Projectivy, Muzei, DashClock, Wear complications and Tasker all ship the same one: a separate APK talks over IPC and supplies typed data, and the host renders it. An embedded interpreter is the only model that lets a Play‑distributed host run downloaded third‑party logic in‑process. Both an interpreter and WebView keep running code inside the host's footprint, and WebView adds a separate renderer process. Declarative templates carry the least risk but give authors the least power. RemoteViews depends on device widget support, which the CDD makes optional. DexClassLoader is banned on Play for downloaded code, and Play Feature Delivery is first‑party only.

### Cited Findings

#### Model 1: separate APK + IPC, where the plugin supplies typed data and the host renders it
- **Projectivy wallpaper plugins (CONFIRMED).** The sample project ships a `/api` module "used to communicate with Projectivy through AIDL (don't change it)". The plugin manifest declares `apiVersion` ("Projectivy will use it to detect if your plugin is compatible"), a `uuid`, a `name`, `itemsCacheDurationMillis`, and an `updateMode` bitmask: `TIME_ELAPSED`, `NOW_PLAYING_CHANGED`, `CARD_FOCUSED`, `PROGRAM_CARD_FOCUSED`, `LAUNCHER_IDLE_MODE_CHANGED`. The host caches results for `itemsCacheDurationMillis`. The README warns that "many Android Tv devices have less memory or cpu power than most smartphones" and that card‑focused events "might be sent each second or so". The repo is Apache‑2.0, has 40 stars and was last pushed 2026‑04‑28. — [projectivy-plugin-wallpaper-provider README](https://github.com/spocky/projectivy-plugin-wallpaper-provider)
- **Projectivy on Play (CONFIRMED).** The listing says "Plugin Support: Extend your wallpaper sources by using plugins or creating your own." A separate Play listing, "Overflight (Projectivy Plugin)", shows up among its related apps, so plugin APKs are distributed on Play as ordinary apps. — [Projectivy Launcher on Google Play](https://play.google.com/store/apps/details?id=com.spocky.projengmenu)
- **Muzei (CONFIRMED).** `MuzeiArtProvider` extends `ContentProvider`. The host discovers providers by the intent action `com.google.android.apps.muzei.api.MuzeiArtProvider`, and authors are "strongly recommended" to protect the provider with `com.google.android.apps.muzei.api.ACCESS_PROVIDER`, "a signature permission that only Muzei can hold". An optional `settingsActivity` meta‑data entry supplies the configuration UI. The repo is Apache‑2.0, has about 4,969 stars and was last pushed 2026‑11‑13. — [MuzeiArtProvider.kt](https://github.com/muzei/muzei/blob/main/muzei-api/src/main/java/com/google/android/apps/muzei/api/provider/MuzeiArtProvider.kt), [muzei/muzei](https://github.com/muzei/muzei)
- **DashClock (CONFIRMED).** Third‑party "extensions … expose status information using the DashClock API". The repo description now reads "DEPRECATED. Home screen widget for Android 4.2+"; it was last pushed 2017‑05‑22 and has 484 stars. — [romannurik/dashclock](https://github.com/romannurik/dashclock)
- **Wear OS complications (CONFIRMED).** A data source is a `ComplicationDataSourceService`. Its manifest entry is protected by `com.google.android.wearable.permission.BIND_COMPLICATION_PROVIDER` "to ensure that only the Wear OS system can bind to provider services". It is discovered by `ACTION_COMPLICATION_UPDATE_REQUEST` and declares `SUPPORTED_TYPES` and `UPDATE_PERIOD_SECONDS` meta‑data, and the system enforces a minimum update period "to preserve" battery. Page last updated 2026‑11‑10. — [Expose data to complications](https://developer.android.com/training/wearables/complications/exposing-data)
- **Tasker / Locale plugins (CONFIRMED).** "Tasker supports standard Android automation plugins", which appear as actions, states or events. "The base plugin protocol only supports asynchronous settings". A Tasker extension lets a plugin ask for synchronous execution with a timeout (`requestTimeoutMS(…, 3000)`), with results passed back through an ordered broadcast to a `FireReceiver`. From version 5.1.5b "Tasker will use IntentServices to call plugins if available", and from 4.8u2 it identifies plugins by "type, package name and class name". — [Tasker plugin developer page](https://tasker.joaoapps.com/plugins.html). The original Locale plugin API repo is archived (last push 2021‑07‑17). — [twofortyfouram/android-plugin-api-for-locale](https://github.com/twofortyfouram/android-plugin-api-for-locale)
- **Discovery on Android 11+ (CONFIRMED).** The host declares a `<queries>` element. An `<intent>` entry inside it must contain "exactly one `<action>` element", and `<provider>` authorities can also be declared there. Page last updated 2026‑11‑16. — [Declare package visibility needs](https://developer.android.com/training/package-visibility/declaring)
- **Reverse visibility is automatic (CONFIRMED).** The plugin sees the host without declaring anything, because "any app that starts or binds to a service in your app" and "any app that accesses a content provider in your app" are visible automatically. Page last updated 2026‑11‑16. — [Package visibility: automatic](https://developer.android.com/training/package-visibility/automatic)
- **`QUERY_ALL_PACKAGES` is restricted on Play (CONFIRMED).** "Permitted uses include device search, antivirus apps, file managers, and browsers." The developer "must be able to adequately justify why a less intrusive method of app visibility will not sufficiently enable" the core functionality. No date shown on the page. — [Play policy: QUERY_ALL_PACKAGES](https://support.google.com/googleplay/android-developer/answer/10158779)
- **Signature allow‑lists (CONFIRMED).** "Starting in Android 12 (API level 31), the `knownCerts` attribute for signature‑level permissions lets you refer to the digests of known signing certificates". With the `knownSigner` flag, "the system grants that permission to a requesting app if any signer in the requesting app's signing lineage … matches one of the digests". Page last updated 2026‑10‑28. — [Define a custom app permission](https://developer.android.com/guide/topics/permissions/defining)
- **Threading and ANR (CONFIRMED).** "Calls from a remote process are dispatched from a thread pool the platform maintains inside your own process". With the `oneway` keyword "a remote call does not block". If a call takes more than a few milliseconds, "don't call it from the activity's main thread", or the app risks an ANR dialog. A broken connection throws `DeadObjectException`. Page last updated 2026‑10‑23. — [AIDL](https://developer.android.com/develop/background-work/services/aidl)
- **Crash notification (CONFIRMED).** `onServiceDisconnected()` is "called when the connection with the service has been unexpectedly disconnected — that is, its process crashed". — [Bound services overview](https://developer.android.com/develop/background-work/services/bound-services)
- **TV memory budget (CONFIRMED).** On 1 GB low‑RAM TV devices, "the maximum total memory (Anon+Swap + Graphics + File) must not exceed … 280 MB", and it is "strongly recommended not to exceed 200 MB" of Anon+Swap+Graphics. The note says these limits "assume the app has no active bindings (except to processes which were already present…)". Device classes:
    - 1 GB with 720p UI: low‑RAM.
    - 1.5 GB with 1080p UI and 2160p video: low‑RAM.
    - ≥1.5 GB or ≥2 GB: "regular".

    Page last updated 2026‑09‑15. — [Optimize memory usage (TV)](https://developer.android.com/training/tv/playback/memory)
- **TV‑ME quality criterion (CONFIRMED).** On low‑RAM devices the foreground app's maximum memory usage "must remain within the limits … defined in Optimize memory usage". Page last updated 2026‑11‑17. — [TV app quality](https://developer.android.com/docs/quality-guidelines/tv-app-quality)

#### Model 2: separate APK supplying RemoteViews (the AppWidget model)
- **Hosting needs user consent (CONFIRMED).** A host declares `android.permission.BIND_APPWIDGET`, but "at runtime, the user must explicitly grant permission". If `bindAppWidgetIdIfAllowed()` returns false, the host shows the system dialog ("allow" or "always allow"). Page last updated 2026‑10‑28. — [Build a widget host](https://developer.android.com/develop/ui/views/appwidgets/host)
- **Widget support is optional per device (CONFIRMED).** CDD §3.8.2 reads "If device implementations support third‑party app widgets, they: [C‑1‑1] MUST declare … `android.software.app_widgets`". Support is conditional, so a TV device may omit it. CDD page last updated 2026‑08‑04. — [Android 16 CDD](https://source.android.com/docs/compatibility/16/android-16-cdd)
- **The TV docs say nothing about widgets (CONFIRMED).** The "Unsupported TV hardware features" list on the TV hardware page covers touchscreen, faketouch, telephony, camera, NFC, GPS, microphone, sensors and portrait orientation. It does not mention widgets either way. Page last updated 2026‑10‑26. — [Handle TV hardware](https://developer.android.com/training/tv/get-started/hardware)
- **RemoteViews APIs are App‑Widget‑bound and shrinking (CONFIRMED).** `RemoteViews.setRemoteAdapter(int, Intent)` is "deprecated in API level 35", replaced by `RemoteCollectionItems`, and "can only be used for App Widgets". — [RemoteViews reference](https://developer.android.com/reference/android/widget/RemoteViews)

#### Model 3: embedded script interpreter or VM in the host, with a host‑provided capability API

| Runtime | Licence | Stars / last push | Android status (label) | Source |
|---|---|---|---|---|
| QuickJS (Bellard) | MIT (LICENSE text; GitHub reports NOASSERTION) | 11,123 / 2026‑11‑10 | C engine. ES2025; "367 KiB of x86 code for a simple hello world"; "very low startup time" (CONFIRMED) | [bellard.org/quickjs](https://bellard.org/quickjs/), [LICENSE](https://github.com/bellard/quickjs/blob/master/LICENSE) |
| quickjs‑ng | MIT | 4,183 / 2026‑11‑19; release v0.18.0 2026‑11‑01 | Community fork of QuickJS (CONFIRMED) | [quickjs-ng/quickjs](https://github.com/quickjs-ng/quickjs) |
| Zipline (Cash App) | Apache‑2.0 | 2,320 / 2026‑11‑18 | Embeds QuickJS in Kotlin/JVM and Kotlin/Native; downloads, caches and loads code from a manifest; Ed25519 / ECDSA P‑256 signed manifests; **"does not offer a sandbox or process‑isolation and should not be used to execute untrusted code"** (CONFIRMED) | [Zipline README](https://github.com/cashapp/zipline) |
| quickjs‑kt | Apache‑2.0 | 162 / 2026‑11‑18 | QuickJS for Android, JVM and Native; coroutine `asyncFunction` bindings (CONFIRMED) | [dokar3/quickjs-kt](https://github.com/dokar3/quickjs-kt) |
| quickjs‑wrapper | Apache‑2.0 | 272 / 2025‑07‑28 | Android/JVM; "Support 16KB page size" (CONFIRMED) | [HarlonWang/quickjs-wrapper](https://github.com/HarlonWang/quickjs-wrapper) |
| quickjs‑android (taoweiji) | Apache‑2.0 | 226 / 2021‑10‑31 | Stale (CONFIRMED) | [taoweiji/quickjs-android](https://github.com/taoweiji/quickjs-android) |
| Rhino | MPL‑2.0 | 4,645 / 2026‑11‑18; 1.9.1 released 2026‑02‑15 | Pure Java; "Rhino runs on Java 17 and higher". On Android it "runs with some restrictions" and the Android test app "runs only in interpreted mode" (CONFIRMED) | [Rhino README](https://github.com/mozilla/rhino), [it-android README](https://github.com/mozilla/rhino/blob/master/it-android/README.md) |
| J2V8 | not detected by GitHub (EPL‑1.0 per memory, UNVERIFIED) | 2,631 / 2025‑11‑14 | V8 bindings; no GitHub releases listed (CONFIRMED for the stars and push date) | [eclipsesource/J2V8](https://github.com/eclipsesource/J2V8) |
| Javet | Apache‑2.0 | 967 / 2026‑11‑19 | Node.js and V8 builds for Android arm and arm64; Node v26.13.0 + V8 v15.7 (CONFIRMED) | [Javet README](https://github.com/caoccao/Javet) |
| Hermes | MIT | 11,447 / 2026‑11‑19 (repo moved to `meta/hermes`) | "optimized for fast start‑up of React Native apps … ahead‑of‑time static optimization and compact bytecode". The README documents only React Native integration (CONFIRMED) | [Hermes README](https://github.com/meta/hermes) |
| LuaJ | MIT | 1,052 / 2023‑04‑24 | Pure Java, Lua 5.2; "Android applications should use the JsePlatform". The `debug` and `luajava` libraries "give unfettered access to the luaj vm and java vm". A sample sandbox "limits libraries, limits bytecodes per script, and makes shared tables read‑only" (CONFIRMED) | [LuaJ README](https://github.com/luaj/luaj) |
| luajava (gudzpoz) | NOASSERTION | 262 / 2026‑08‑16 | Lua 5.1–5.5, LuaJ or LuaJIT, Android listed (CONFIRMED) | [gudzpoz/luajava](https://github.com/gudzpoz/luajava) |
| Starlark (spec) | Apache‑2.0 | 3,136 / 2026‑02‑06 | "Hermetic execution. Execution cannot access the file system, network, system clock. It is safe to execute untrusted code." Also "Deterministic evaluation" (CONFIRMED). The Java implementation lives inside Bazel; no standalone Android artifact was verified (UNVERIFIED) | [bazelbuild/starlark](https://github.com/bazelbuild/starlark) |
| starlark‑go (used by Pixlet) | BSD‑3‑Clause | 2,797 / 2026‑10‑21 | Go, so not directly embeddable in a Java app (inference) | [google/starlark-go](https://github.com/google/starlark-go) |
| Chicory | Apache‑2.0 | 1,147 / 2026‑07‑06; release 1.7.5 2026‑03‑24 | "zero native dependencies or JNI"; interpreter plus AOT engine; WASIp1. Its ecosystem includes "Quickjs4J — Sandboxed runtime for JavaScript". The README does not claim Android support (CONFIRMED that it is absent from the README) | [Chicory README](https://github.com/dylibso/chicory) |
| wasmtime‑java | Apache‑2.0 | 144 / 2023‑10‑07 | Stale binding (CONFIRMED) | [kawamuray/wasmtime-java](https://github.com/kawamuray/wasmtime-java) |
| GraalWasm (in oracle/graal) | NOASSERTION at repo level | 21,754 / 2026‑11‑19 | No Android/ART support found (UNVERIFIED) | [oracle/graal](https://github.com/oracle/graal) |

- **Pixlet as the closest existing "widget script" system (CONFIRMED).** Apps are Starlark scripts, starting from `load("render.star", "render")` and a `render.Root(...)` tree, and render to WebP/GIF. `pixlet serve` previews in a browser; `pixlet render` plus `pixlet push` send the image to a device. Rendering happens off‑device, not on the display. — [tidbyt/pixlet README](https://github.com/tidbyt/pixlet)
- **Figma's interpreter‑in‑a‑sandbox (CONFIRMED).** Since September 2019 Figma has run plugin logic in "QuickJS, a JavaScript VM written in C and cross‑compiled to WebAssembly", after vulnerabilities in the Realms shim. The VM approach rules out "the Realms shim confusing an object from outside the sandbox with an object from inside". — [Figma: An update on plugin security (2019‑10‑02)](https://www.figma.com/blog/an-update-on-plugin-security/)

#### Model 4: sandboxed WebView / iframe HTML+JS widgets
- **Process model and memory (CONFIRMED).** WebView uses a browser process (the app) and a renderer process. "Currently, apps just get one renderer process for all WebViews". `dumpsys meminfo <package>` shows only the browser process: "the memory used by the renderer process is accounted for separately". In Google's lab sample, renderer total PSS "jumped to ~55MB after a few allocations", measured on a Pixel 9 Pro Fold, not a TV. Apps "are expected to call `WebView.destroy()`". Page last updated 2026‑08‑25. — [WebView and memory](https://developer.android.com/topic/performance/memory/guide/webview-memory)
- **Renderer sandbox (CONFIRMED).** "WebView's renderer process also runs in the app's context, although this process is sandboxed so it actually has even fewer permissions." GPU and network services run in‑process "on all OS versions" to save memory. — [Chromium WebView architecture](https://chromium.googlesource.com/chromium/src/+/HEAD/android_webview/docs/architecture.md)
- **Out‑of‑process renderer conditions (UNVERIFIED).** The out‑of‑process renderer is enabled on 64‑bit devices, on high‑memory 32‑bit devices and on all devices from Android 11; low‑memory 32‑bit devices on API 26–29 keep an in‑process renderer. Source: search snippet of an older revision of the same doc. — [Chromium WebView architecture](https://chromium.googlesource.com/chromium/src/+/HEAD/android_webview/docs/architecture.md)
- **TV quality on web content (CONFIRMED).** "For web content, the app must only use WebView components. The app must not attempt to launch a web browser app." — [TV app quality](https://developer.android.com/docs/quality-guidelines/tv-app-quality)
- **Home Assistant Companion = WebView + third‑party JS cards (CONFIRMED).** The Android app contains `webview/WebViewActivity.kt`. The FAQ tells users to keep "Android System WebView" up to date and to report custom‑component issues "with the developer of that component". — [home-assistant/android](https://github.com/home-assistant/android), [Companion FAQ](https://companion.home-assistant.io/docs/troubleshooting/faqs/)
- **HA custom cards have the user's full privileges (CONFIRMED).** A card is a custom element (`class … extends HTMLElement`, `customElements.define`) that receives the `hass` object. That object exposes `callService`, `callWS`, `callApi` ("with the authentication of the user") and `fetchWithAuth` ("with the access token of the user"). — [Custom card](https://developers.home-assistant.io/docs/frontend/custom-ui/custom-card/), [hass object](https://developers.home-assistant.io/docs/frontend/data/)
- **Tasker on Play runs user JS (CONFIRMED).** Tasker "supports running JavaScript code in tasks or WebView scene elements. Most Tasker actions can be accessed direct from the JavaScript." Library code comes with a warning: it "will have access to local files, data providers etc." — [Tasker JavaScript guide](https://tasker.joaoapps.com/userguide/en/javascript.html)
- **Figma's iframe model (CONFIRMED).** In Figma's first design, a null‑origin `<iframe>` could talk to the host only "via message‑passing", and "any attempt to make a request to figma.com will be denied". The final design still uses a null‑origin iframe for plugin UI and network, alongside the VM for document access. — [Figma: How we built the Figma plugin system (2019‑08‑22)](https://www.figma.com/blog/how-we-built-the-figma-plugin-system/)

#### Model 5: declarative‑only plugins (template + data URL, no code)
- **TRMNL (CONFIRMED).** "TRMNL uses the Liquid templating library by Shopify to interpolate values into your template markup". Layouts are full, half vertical, half horizontal and quadrant, and custom Liquid filters are provided (`number_to_currency`, `days_ago`, `l_date`, …). Private plugins get "an inline editor, merge variable interpolation, and a live previewer". — [TRMNL templates docs](https://docs.trmnl.com/go/private-plugins/templates)
- **TRMNL recipe rules (CONFIRMED).** Recipe authors must "prioritize using our design Framework and Liquid markup for logic first, only followed by CSS and Javascript". — [TRMNL recipe checklist](https://trmnl.com/blog/plugin-recipe-publishing-tips)
- **TRMNL polling (UNVERIFIED, search snippets).** With polling, TRMNL "will fetch this content on your behalf". Multiple URLs map to `IDX_<n>` variables, refresh intervals are 15/60/360/720/1440 minutes, and the URL is itself Liquid‑templated. — [TRMNL private plugins](https://docs.trmnl.com/go/private-plugins), [Dynamic polling URLs](https://help.trmnl.com/en/articles/12689499-dynamic-polling-urls)
- **Glance `extension` widget (CONFIRMED).** It is "nothing more than an HTTP request to a server that returns a few special headers" (`Widget-Title`, `Widget-Content-Type`, `Widget-Content-Frameless`). HTML content "requires the user to have the `allow-potentially-dangerous-html` property set to `true`". The feature is marked as a work in progress: "You are responsible for maintaining your own extensions." Glance is AGPL‑3.0 with 38,306 stars. — [Glance extensions doc](https://github.com/glanceapp/glance/blob/main/docs/extensions.md), [glanceapp/glance](https://github.com/glanceapp/glance)
- **Glance `custom-api` (CONFIRMED).** Go templates read a JSON response, for example `{{ .JSON.String "title" }}` and `{{ range .JSON.Array "posts" }}`. — [Glance custom-api doc](https://github.com/glanceapp/glance/blob/main/docs/custom-api.md)
- **Adaptive Cards (CONFIRMED).** There is a native Android renderer, `io.adaptivecards:adaptivecards-android`; Maven Central's latest is 3.7.1‑beta01, dated 2025‑06‑18. Several v1.6 features (Carousel, Label Position, …) are marked not supported on Android. The README links the Android/iOS renderers to a separate `EULA-Non-Windows.txt`, even though the repo itself is MIT. — [AdaptiveCards README](https://github.com/microsoft/AdaptiveCards), [Maven Central search](https://search.maven.org/search?q=g:io.adaptivecards)
- **Shopify Remote DOM, a hybrid (CONFIRMED).** It "lets you take a tree of DOM elements created in a sandboxed JavaScript environment, and render them to the DOM in a different JavaScript environment", from a hidden iframe or a web worker. MIT licence. — [Shopify/remote-dom](https://github.com/Shopify/remote-dom)

#### Model 6: dynamic code loading (DexClassLoader / split APKs / Play Feature Delivery)
- **Play forbids downloaded executables (CONFIRMED).** An app "may not download executable code (such as dex, JAR, .so files) from a source other than Google Play." — [Device and Network Abuse](https://support.google.com/googleplay/android-developer/answer/9888379)
- **Android 14 read‑only rule (CONFIRMED).** If an app targets API 34+ and uses dynamic code loading, "all dynamically‑loaded files must be marked as read‑only. Otherwise, the system throws an exception. We recommend that apps avoid dynamically loading code whenever possible". Page last updated 2026‑10‑28. — [Android 14 behavior changes](https://developer.android.com/about/versions/14/behavior-changes-14)
- **Feature modules are first‑party (CONFIRMED).** "Each feature module depends on the base module". Modules are built into the app's own bundle and delivered at install time, on demand or conditionally through the Play Feature Delivery library. Page last updated 2026‑11‑13. — [Play Feature Delivery](https://developer.android.com/guide/playcore/feature-delivery)

#### Process isolation that applies across models
- **Isolated services (CONFIRMED).** `android:isolatedProcess="true"` makes a service run "under a special process that is isolated from the rest of the system and has no permissions of its own. The only communication with it is through the Service API". — [`<service>` element](https://developer.android.com/guide/topics/manifest/service-element)
- **WebView uses this mechanism (CONFIRMED).** The WebView renderer shows up as `org.chromium.content.app.SandboxedProcessService` / `com.google.android.webview:sandboxed_process0`. — [WebView and memory](https://developer.android.com/topic/performance/memory/guide/webview-memory)

### Inferences
- **Model 1 fits what Homeboard already does.** The Projectivy contract (AIDL, `apiVersion`, cache duration, event mask) is a working template for a "widget data provider" contract. The host keeps full control of rendering and of D‑pad focus.
- **Each plugin APK is a separate process.** Binding to it can raise that process's priority. On 1 GB low‑RAM TVs the 280 MB budget, plus the note excluding active bindings, means several bound plugin processes eat into a tight device total. The host should bind briefly to fetch data and cache the results, as Projectivy does, rather than holding long‑lived bindings.
- **The host should treat IPC as hostile and slow.** That means off‑main‑thread calls, `oneway` where possible, timeouts, and handling of `DeadObjectException` and `onServiceDisconnected()`. Muzei uses a signature permission and Wear uses a `BIND_*` permission to stop arbitrary apps binding; for a host pulling data from plugins, the mirror image is to verify plugin signers. `knownSigner` does this only on API 31+, so Android TV 9–11 devices need a runtime `PackageManager` signature check.
- **RemoteViews is a weak fit for a TV host.** Device support for app widgets is optional and unverified on Android TV, the bind‑consent dialog adds friction, and RemoteViews interaction is built around click `PendingIntent`s, not D‑pad focus.
- **Engine choice for Model 3.** QuickJS (MIT) and its Kotlin/Java bindings are the most active small engines with an Android story. Rhino and LuaJ need no native code but are interpreted‑only on Android; LuaJ has had no commits since 2023. Starlark is designed to be hermetic, but its Java implementation is inside Bazel and Android use is unverified. Chicory/Wasm is pure Java, but Android is unverified, and the AOT engine probably relies on JVM bytecode generation that ART lacks (unverified).
- **Zipline separates signing from sandboxing.** It provides signed code delivery but explicitly no sandbox. A plugin host for untrusted authors still needs to build its own capability layer, such as Figma's ~500‑line "membrane" (Section 3).
- **WebView widgets add a renderer process.** Its memory is not visible in the app's own meminfo, and all WebViews share one renderer, so one misbehaving widget can degrade every web widget. Home Assistant and Tasker show that Play accepts apps running third‑party JS in WebView.
- **Declarative plugins are the easiest to make TV‑native.** The host renders native Views, so it controls focus, theming and memory. Authors are limited to the template language's power, and both TRMNL and Glance cap it on purpose.
- **Model 6 is not viable for third‑party plugins on Play.** Play forbids downloaded dex/JAR/.so, and Feature Delivery modules come from the developer's own bundle.

### Gaps
- **Engine cost on TV SoCs.** No authoritative per‑engine APK‑size or RAM figures exist for QuickJS, Rhino, LuaJ or Chicory on 1–2 GB ARM TV SoCs. QuickJS's 367 KiB figure is x86 code size. These need measuring on a target device.
- **WebView renderer on TV.** No TV‑specific baseline was found; the ~55 MB figure is from a phone lab exercise.
- **D‑pad focus between native Views and WebView content.** No official guidance found.
- **App widget support on Android TV / Google TV.** Whether shipping devices declare `android.software.app_widgets` was not verified, nor whether Projectivy hosts AppWidgets.
- **Cash App's production use of Zipline.** Not stated in the README.
- **J2V8's current licence.** Not detected by GitHub.
- **DashClock's deprecation reason.** Not stated in the repo. Press says lock‑screen widgets were removed in Android 5.0 (UNVERIFIED — [Android Authority](https://androidauthority.com/android-15-lock-screen-widgets-3402342)).
- **Loading code from another installed APK.** `createPackageContext(…, CONTEXT_INCLUDE_CODE)` was not researched.

## 2. What Google Play policy allows and forbids for plugin hosts

### Takeaway
Play forbids downloading dex/JAR/.so from outside Play. It explicitly exempts "code that runs in a virtual machine or an interpreter where either provides indirect access to Android APIs (such as JavaScript in a webview or browser)". It also makes the host answerable for runtime‑loaded interpreted code: that code "must not allow potential violations of Google Play policies". No dedicated "plugin host" policy was found. Plugin APKs on Play are separate apps under their own review. A host that installs plugin APKs itself would need `REQUEST_INSTALL_PACKAGES`, whose permitted uses do not include launchers.

### Cited Findings
- **Device and Network Abuse policy, full text (CONFIRMED; no last‑updated date shown).** "An app distributed via Google Play may not modify, replace, or update itself using any method other than Google Play's update mechanism. Likewise, an app may not download executable code (such as dex, JAR, .so files) from a source other than Google Play. This restriction does not apply to code that runs in a virtual machine or an interpreter where either provides indirect access to Android APIs (such as JavaScript in a webview or browser)." — [Device and Network Abuse](https://support.google.com/googleplay/android-developer/answer/9888379)
- **Same page, interpreted code (CONFIRMED).** "Apps or third‑party code, like SDKs, with interpreted languages (JavaScript, Python, Lua, etc.) loaded at run time (for example, not packaged with the app) must not allow potential violations of Google Play policies." — [Device and Network Abuse](https://support.google.com/googleplay/android-developer/answer/9888379)
- **Same page, violation examples (CONFIRMED).**
    - "Apps or third party code (for example, SDKs) containing a webview with added JavaScript Interface that loads untrusted web content (for example, http:// URL) or unverified URLs obtained from untrusted sources".
    - "Apps that install other apps on a device without the user's prior consent".
    - "Apps that link to or facilitate the distribution or installation of malicious software".

    The Key Considerations table adds: "Don't use third‑party SDKs in your app that download executable code (like dex or .so files) from outside Google Play (except in VMs/interpreters)." — [Device and Network Abuse](https://support.google.com/googleplay/android-developer/answer/9888379)
- **Malware policy: Backdoors (CONFIRMED).** Defined as "code facilitating unwanted or harmful remote‑controlled operations". The guidance is to "Secure all remote execution endpoints against unauthorized access" and not to "obfuscate code to hide remote access functionality". — [Malware](https://support.google.com/googleplay/android-developer/answer/9888380)
- **Malware policy: Hostile Downloaders (CONFIRMED).** Code may be one if "at least 5% of apps downloaded by it are PHAs with a minimum threshold of 500 observed app downloads (25 observed PHA downloads)". — [Malware](https://support.google.com/googleplay/android-developer/answer/9888380)
- **`REQUEST_INSTALL_PACKAGES` (CONFIRMED).** The app must have "a core purpose to enable installation of packages". Permitted uses: "Web browsing or search; Communication services that support attachments; File sharing, transfer or management; Enterprise device management; Backup & restore; Device Migration / Phone Transfer". — [Play policy: REQUEST_INSTALL_PACKAGES](https://support.google.com/googleplay/android-developer/answer/12085295)
- **`QUERY_ALL_PACKAGES` (CONFIRMED).** Restricted to apps whose core purpose needs to see all installed apps. — [Play policy: QUERY_ALL_PACKAGES](https://support.google.com/googleplay/android-developer/answer/10158779)
- **TV app quality requirements that bear on plugins (CONFIRMED; page last updated 2026‑11‑17).**
    - Five‑way D‑pad navigation is required.
    - Web content must use WebView only.
    - TV‑ME memory limits apply on low‑RAM devices.

    — [TV app quality](https://developer.android.com/docs/quality-guidelines/tv-app-quality)
- **Precedents currently on Play (CONFIRMED, listings read 2026‑11‑19).**
    - Projectivy Launcher: wallpaper plugins, with plugin APKs as separate listings. — [Play](https://play.google.com/store/apps/details?id=com.spocky.projengmenu)
    - Tasker: plugins plus user JavaScript; the listing links its plugin list. — [Play](https://play.google.com/store/apps/details?id=net.dinglisch.android.taskerm)
    - KWGT: "Complex Logic: Functions, conditionals, and global variables"; "Dynamic Data: HTTP downloads, RSS, XML/XPATH parsing". — [Play](https://play.google.com/store/apps/details?id=org.kustom.widget)
    - Home Assistant Companion: a WebView over the user's HA frontend, including custom JS cards. — [Play](https://play.google.com/store/apps/details?id=io.homeassistant.companion.android)
    - Smart Launcher: "Powerful widgets, built in", and "See which apps have active notifications without installing an additional plugin". — [Play](https://play.google.com/store/apps/details?id=ginlemon.flowerfree)

### Inferences
- **Interpreter and WebView models fit the exception.** A host that downloads widget scripts (JS/Lua/Starlark/Wasm) or HTML widgets from a catalogue falls within the interpreter exception, provided the runtime gives only "indirect access to Android APIs". A capability API mediated by the host matches that wording; a raw reflection bridge to Java, such as LuaJ's `luajava` or Rhino's `LiveConnect`, arguably does not.
- **The host owns script behaviour.** Because of the "must not allow potential violations" clause, Play can hold the host responsible for what downloaded scripts do. Catalogue review, kill‑switches and capability limits reduce policy risk as well as security risk.
- **WebView JS bridge rule.** A JavaScript interface must not be exposed to http:// or unverified URLs. WebView widgets should be loaded from signed or pinned HTTPS sources or local bundles.
- **Separate‑APK plugins are the policy‑simplest route.** Each plugin is reviewed by Play itself, and the host only discovers, binds and renders. Installing plugin APKs from inside the host (sideload catalogue plus installer) would need `REQUEST_INSTALL_PACKAGES`, and a launcher/dashboard is not among its permitted uses. Linking out to a Play listing or to Obtainium avoids that permission.
- **Presence on Play is not endorsement.** The precedents show these patterns coexist with policy today; they do not show Google endorsing them.

### Gaps
- **No dedicated plugin‑host policy.** None was found. The "Spam and Minimum Functionality" and "Impersonation" policies were not read; they may matter for plugin APKs that are thin shells.
- **Enforcement record.** No public enforcement case against an interpreter‑based plugin host was found.
- **Android TV review details.** Policy text beyond the quality checklist (for example, launcher‑category rules on Google TV) is out of scope here.

## 3. Security and privacy: capability models and what a malicious widget could do in each model

### Takeaway
Popular plugin hosts split into two camps. In the first, plugins run with full host privileges and safety comes from marketplace review and scanning: VS Code, Obsidian and Home Assistant custom cards. In the second, the host builds a capability sandbox: Chrome extensions, Deno and Figma. Figma's history is the clearest lesson. Its same‑engine Realms sandbox was broken in 2019, it moved to a separate VM (QuickJS compiled to Wasm), and it built a single audited "membrane" so that every API did not need auditing.

### Cited Findings
- **VS Code (CONFIRMED).** "The extension host has the same permissions as VS Code itself." Since VS Code 1.97, installing from a new publisher prompts the user to trust that publisher. Marketplace safeguards:
    - malware scanning with "several antivirus engines" on every publish and update;
    - "dynamic detection … in a sandboxed environment (clean room VM)";
    - verified publishers (domain ownership plus good standing "for at least six months");
    - name‑squatting protection;
    - a block list, where a flagged extension that is already installed is "automatically uninstalled by VS Code";
    - secret scanning that blocks a publish.

    — [VS Code extension runtime security](https://code.visualstudio.com/docs/configure/extensions/extension-runtime-security)
- **Obsidian (CONFIRMED).** Restricted Mode is on by default. "Obsidian cannot reliably restrict plugins to specific permissions or access levels … plugins will inherit Obsidian's access levels": they "can access files on your computer", "connect to internet" and "install additional programs". "Obsidian automatically scans every plugin version for security vulnerabilities, code quality issues, and malware", showing a "safety scorecard". "Manual reviews continue for popular, featured, and flagged plugins." — [Obsidian plugin security](https://github.com/obsidianmd/obsidian-help/blob/master/en/Extending%20Obsidian/Plugin%20security.md)
- **Obsidian developer policies (CONFIRMED).**
    - Plugins must not: "Obfuscate code", "Insert dynamic ads that are loaded over the internet", "Include client‑side telemetry", or "Install or update themselves or their dependencies".
    - Themes "may not load assets from the network".
    - Plugins must disclose payment, account requirements and "Network use. Clearly explain which remote services are used and why".

    — [Obsidian Developer policies](https://github.com/obsidianmd/obsidian-developer-docs/blob/main/en/Community%20directory/Developer%20policies.md)
- **Home Assistant custom cards (CONFIRMED).** No sandbox: the card gets `hass.callService`, `callWS`, `callApi` "with the authentication of the user" and `fetchWithAuth` "with the access token of the user". — [hass object](https://developers.home-assistant.io/docs/frontend/data/)
- **Chrome extensions (CONFIRMED).** Permission manifest keys are `permissions`, `optional_permissions` ("Granted by the user at runtime"), `host_permissions` (match patterns) and `optional_host_permissions`. "Permissions help to limit damage if your extension is compromised by malware". Adding or changing host match patterns "will also trigger a warning". — [Chrome: Declare permissions](https://developer.chrome.com/docs/extensions/develop/concepts/declare-permissions)
- **Deno (CONFIRMED).** "Unless you specifically enable it, a program run with Deno has no access to sensitive APIs". Network access is scoped per host (`--allow-net=example.com`), and `--deny-*` overrides `--allow-*`. `--allow-run` "lets the program spawn subprocesses … outside the sandbox". For defence in depth Deno recommends OS sandboxing, a VM or a microVM. — [Deno security](https://docs.deno.com/runtime/fundamentals/security/)
- **Figma's three attempts (CONFIRMED).**
    1. A null‑origin `<iframe>`, rejected because of serialisation costs.
    2. "Duktape … compiled to WebAssembly": "plugin code can communicate with the outside world only through explicit whitelisted APIs by default".
    3. The Realms shim, wrapped in a low‑level interface. That interface is "~500 LOC in our case", and "once that is done, future APIs can be built on top of this interface without worrying about sandbox‑related security issues". Figma calls this "the membrane pattern".

    — [How we built the Figma plugin system (2019‑08‑22)](https://www.figma.com/blog/how-we-built-the-figma-plugin-system/)
- **Figma's 2019 incident (CONFIRMED).** "Several independent vulnerabilities were recently discovered with the Realms shim that could have allowed code inside the sandbox to escape". Figma's response:
    - It halted plugin reviews and "completely disabled publishing updates to existing plugins … as plugin code in Figma is live, which means updates to existing plugins instantly propagate to all open clients".
    - It switched to QuickJS compiled to Wasm in about 11 days (2019‑09‑14 to 2019‑09‑25).
    - "Our manual review process is about plugin user experience. We do not rely on human reviews to audit newly‑published plugins for security".

    — [An update on plugin security (2019‑10‑02)](https://www.figma.com/blog/an-update-on-plugin-security/)
- **Zipline (CONFIRMED).** Signatures authenticate code but are "not … a sandbox". The trust model rests on the host app and the build server, and there is not yet "a mechanism to outlaw older (signed) versions of executable code that have known problems". — [Zipline README](https://github.com/cashapp/zipline)
- **LuaJ sandboxing guidance (CONFIRMED).** It suggests limiting libraries, limiting bytecodes per script, read‑only shared tables, or "a heavier but strong sandbox where each script gets its own class loader". — [LuaJ README](https://github.com/luaj/luaj)
- **Starlark (CONFIRMED).** Described as hermetic: no file system, network or clock access by design. — [bazelbuild/starlark](https://github.com/bazelbuild/starlark)
- **Android primitives (CONFIRMED).**
    - Signature permissions and `knownSigner`. — [Define a custom app permission](https://developer.android.com/guide/topics/permissions/defining)
    - `isolatedProcess` services with "no permissions of its own". — [`<service>` element](https://developer.android.com/guide/topics/manifest/service-element)
    - The `BIND_*`‑permission‑protected service pattern. — [Wear complications](https://developer.android.com/training/wearables/complications/exposing-data), [MuzeiArtProvider.kt](https://github.com/muzei/muzei/blob/main/muzei-api/src/main/java/com/google/android/apps/muzei/api/provider/MuzeiArtProvider.kt)
- **Interpreted‑code liability (CONFIRMED).** Play's interpreted‑code clause makes runtime‑loaded scripts the host's policy responsibility. — [Device and Network Abuse](https://support.google.com/googleplay/android-developer/answer/9888379)

### Inferences
What a malicious or compromised widget could do in each model, derived from the findings above:

| Model | Runs with | What a malicious widget could do | Host‑side controls available |
|---|---|---|---|
| 1. Separate APK + IPC | Its own UID and whatever permissions it requested at install | Collect data within its own permissions (e.g. network, location if granted) and exfiltrate it; return misleading or offensive content; burn CPU, RAM and network; drain the host by slow IPC; read any household secrets the host sends it over IPC | Signer allow‑list (`knownSigner` / runtime check); timeouts and off‑main‑thread binder calls; host‑side caching; send each plugin only its own config; Play review of the plugin APK itself |
| 2. RemoteViews | Its own UID; the host inflates its layouts | Same as above, plus a `PendingIntent` on click that launches the provider's activities | Bind consent dialog; restricted to the RemoteViews‑supported view set |
| 3. Embedded interpreter | The host's process and UID; only host‑granted capabilities | Abuse granted capabilities (e.g. `fetch` to any host, unless allow‑listed); infinite loops or memory blow‑ups (needs interrupt handlers and memory limits); exploit an engine bug (native QuickJS memory safety) to escape into host privileges | Capability API with per‑widget network allow‑list (Deno/Chrome style); a membrane boundary (Figma); CPU and memory budgets (LuaJ‑style bytecode limits); optional `isolatedProcess` service host; signed catalogue plus kill‑switch |
| 4. WebView | Renderer: sandboxed, fewer permissions. JS bridge: whatever the host exposes | Fetch arbitrary origins and track the household; fingerprint; abuse any `addJavascriptInterface` bridge; exhaust the shared renderer and degrade other web widgets | No JS bridge, or a narrow message channel; `shouldInterceptRequest` filtering; HTTPS‑only signed bundles; one WebView per widget versus a shared one (memory trade‑off) |
| 5. Declarative | No code; the host fetches data URLs and renders | Track the household through data or image URLs (host IP, timing); push offensive or misleading content; inject markup if the template engine allows raw HTML (cf. Glance's `allow-potentially-dangerous-html`) | Host‑controlled fetch with allow‑list; escaping by default; no raw‑HTML mode |
| 6. Dynamic dex | The host's process with full host permissions | Anything the host can do | None meaningful; also barred by Play |

- **Reviews are not security audits.** Hosts without a real sandbox (VS Code, Obsidian, HA) rely on review, scanning, block‑lists and user trust prompts. Figma states plainly that human review is for UX, not security.
- **Live code needs a freeze switch.** Figma's incident shows that live‑updating plugin code needs a central way to freeze updates and publishing.
- **Privacy requires one capability rule.** For PDPA‑style minimisation, every model except 1 can route all network access through host code, which enables per‑widget allow‑lists and disclosure. Model 1 cannot: a plugin APK's own network use is invisible to the host.

### Gaps
- **Thai PDPA.** Specific PDPA obligations for plugin data flows were not researched; this was out of scope here.
- **WebView network control.** No official measure exists of how well `shouldInterceptRequest` can block all WebView network traffic (e.g. WebSockets, service workers); unverified.
- **Engine CVE history.** Recent CVEs for QuickJS or quickjs‑ng were not reviewed.

## 4. Distribution and trust for a small project

### Takeaway
Small ecosystems converge on one pattern: a GitHub‑hosted curated index that authors join by pull request, with automated validation, plus GitHub releases as the artefact source and a "custom repository" escape hatch. HACS, the Tidbyt/Tronbyt community repo and the Obsidian directory all work this way. The risks are slow review backlogs (HACS: "months") and the steward walking away (Tidbyt after Modal, which ended in a hard fork).

### Cited Findings
- **HACS publishing requirements (CONFIRMED).** A repository needs a GitHub description, topics, a README and a `hacs.json` manifest. "If the repository uses GitHub releases, the tag name from the latest release is used … Just publishing tags is not enough, you need to publish releases." — [HACS: Publish](https://www.hacs.xyz/docs/publish/start/)
- **HACS default list (CONFIRMED).** Inclusion is by PR to `hacs/default`. "Only the owner or a major contributor of a repository can submit". Automated checks (hacs‑validation, releases, …) "must pass". "New additions still take months to be reviewed and included." `hacs/integration` is MIT with 7,893 stars; `hacs/default` is MIT with 664 stars. — [HACS: Include default repositories](https://www.hacs.xyz/docs/publish/include/), [hacs/integration](https://github.com/hacs/integration), [hacs/default](https://github.com/hacs/default)
- **Obsidian directory (CONFIRMED).** It is backed by `obsidianmd/obsidian-releases`, with 23,390 stars, last pushed 2026‑11‑19 and no licence detected. Every version is scanned automatically, manual review covers popular, featured and flagged plugins, and plugins that break policy are removed. — [obsidian-releases](https://github.com/obsidianmd/obsidian-releases), [Obsidian plugin security](https://github.com/obsidianmd/obsidian-help/blob/master/en/Extending%20Obsidian/Plugin%20security.md), [Developer policies](https://github.com/obsidianmd/obsidian-developer-docs/blob/main/en/Community%20directory/Developer%20policies.md)
- **Tidbyt community repo (CONFIRMED).** Apps are published by PR, after a lint step. — [tidbyt/community](https://github.com/tidbyt/community)
- **Tidbyt joins Modal (CONFIRMED).** On 2024‑11‑07 Tidbyt announced it was joining Modal and said "we will continue to support our existing process for publishing apps". — [Tidbyt is joining Modal](https://tidbyt.com/blogs/tidbyt/tidbyt-is-joining-modal)
- **Tronbyt fork (CONFIRMED).** "Tronbyt‑Apps is a hard fork … This exists because the Tidbyt folks have stopped merging pull requests since their acquisition by Modal." — [tronbyt/apps](https://github.com/tronbyt/apps)
- **Tidbyt hardware (UNVERIFIED).** Tidbyt hardware production ended and the cloud services may eventually be switched off. Source: secondary press found via search. — [Hackaday](https://hackaday.com/2025/03/29/open-source-framework-aims-to-keep-tidbyt-afloat), [macwright.com](https://macwright.com/2025/04/12/tidbyt-second-life)
- **TRMNL recipes (CONFIRMED).** Review has "two steps": automated checks, then "manual feedback from a human". Policies cover duplication ("Collaboration over competition"), "Depth, not numbers", and "Family friendly" defaults, with "unlisted" publishing as an option. — [TRMNL recipe checklist](https://trmnl.com/blog/plugin-recipe-publishing-tips)
- **VS Code Marketplace trust signals (CONFIRMED).** Malware scanning, a clean‑room VM, verified publishers, a block list with automatic uninstall, and secret scanning. — [VS Code extension runtime security](https://code.visualstudio.com/docs/configure/extensions/extension-runtime-security)
- **Signed code delivery (CONFIRMED).** Zipline supports Ed25519 / ECDSA P‑256 manifest signatures with "multiple keys to support key rotation". — [Zipline README](https://github.com/cashapp/zipline)
- **Obtainium (CONFIRMED).** It "allows you to install and update apps directly from their releases pages" (GitHub and others) and integrates with "Verified Apps" (privacyguides). GPL‑3.0, 21,676 stars, last pushed 2026‑11‑01. — [Obtainium README](https://github.com/ImranR98/Obtainium)
- **Plugin APKs on Play (CONFIRMED).** Projectivy shows plugin APKs listed as their own Play apps. — [Projectivy on Play](https://play.google.com/store/apps/details?id=com.spocky.projengmenu)
- **Signer allow‑listing (CONFIRMED).** Android 12+ `knownSigner` lets a host allow‑list plugin signing certificates without sharing a key. — [Define a custom app permission](https://developer.android.com/guide/topics/permissions/defining)

### Licence compatibility with an MIT host (inferences from the licence data above; not legal advice)
- **MIT, Apache‑2.0, BSD‑3: compatible.** QuickJS, quickjs‑ng, Hermes, LuaJ, Remote DOM and the AdaptiveCards repo are MIT. Zipline, quickjs‑kt, quickjs‑wrapper, Javet, Chicory, wasmtime, the Starlark spec, the Muzei API, the Projectivy API and Pixlet are Apache‑2.0. starlark‑go is BSD‑3. All of these can be bundled in an MIT‑licensed APK if notices are kept (Apache‑2.0 `NOTICE` files).
- **MPL‑2.0 (Rhino): usable.** File‑level copyleft only. Modified Rhino files stay MPL, and the host's own code can remain MIT.
- **GPL‑3.0 (TaskerPluginSample, Obtainium): do not link.** Not to be linked into the MIT host. Recommending Obtainium as a separate app is fine.
- **AGPL‑3.0 (Glance): ideas only.** Do not copy its code; the design ideas are fine to reuse.
- **Adaptive Cards Android renderer: check the EULA.** The README points Android/iOS renderers to `EULA-Non-Windows.txt`, so read that EULA before bundling the binary.
- **No licence detected (J2V8, obsidian‑releases, usetrmnl/plugins): verify first.**

### Inferences
- **A GitHub index fits a project this size.** An in‑app catalogue read from a GitHub `index.json` in a repo that accepts PRs, validated by a GitHub Action (HACS/Tronbyt style), is feasible for a small project. It also gives an audit trail.
- **Signing must not hinge on one maintainer.** Signing catalogue entries (Zipline‑style manifest signatures, or `knownSigner` for plugin APKs) protects against a compromised GitHub account. Holding signing keys in one person's hands creates the Tidbyt‑style continuity risk; documented key rotation and a forkable index reduce it.
- **Build in a remote kill‑switch.** A pinned‑version update channel and a remote revoke list cover the "live code" risk Figma hit, and VS Code's block list with auto‑uninstall does the same job.

### Gaps
- **F‑Droid.** Its inclusion policy for plugin APKs, and how F‑Droid handles "plugin for app X" packages, was not researched.
- **Precedent for curated catalogues of signed scripts on Android TV.** None was found.

## 5. Developer experience: what attracts plugin authors and what killed past systems

### Takeaway
The systems that drew authors offer a one‑command preview loop with no device (`pixlet serve`, the TRMNL live previewer, Zipline's `--continuous`), a template generator, a typed contract with versioning, and a public gallery with clear review rules. The systems that faded lost their host or steward (DashClock deprecated, the Locale API archived, Tidbyt PRs stalled after the acquisition), or shipped an API marked as a moving target (Glance extensions are "a work in progress").

### Cited Findings
- **Pixlet (CONFIRMED).** "Hello, World!" is a few lines of Starlark; `pixlet serve /dev/stdin` previews it in a browser; `pixlet render` writes WebP and `pixlet push` sends it to a device. The tidbyt/pixlet repo is Apache‑2.0, has 848 stars and was pushed 2026‑11‑18; the tronbyt/pixlet fork has 61 stars and was pushed 2026‑11‑16. — [Pixlet README](https://github.com/tidbyt/pixlet), [tronbyt/pixlet](https://github.com/tronbyt/pixlet)
- **Tidbyt community tooling (CONFIRMED).** It provides a generator ("run the following to generate all the code you need"), a pre‑submission check, and an FAQ for tracking release status. — [tidbyt/community](https://github.com/tidbyt/community)
- **TRMNL (CONFIRMED).** Authors get an inline markup editor with a live previewer and a "Your variables" dropdown, plus a framework of layout classes. Recipe reviewers preview every view across device models and orientations. — [TRMNL templates](https://docs.trmnl.com/go/private-plugins/templates), [TRMNL recipe checklist](https://trmnl.com/blog/plugin-recipe-publishing-tips)
- **TRMNL local tool (UNVERIFIED, search snippet).** The open‑source `trmnlp` previewer fetches a polling URL "once, when the server starts". — [trmnlp on ecosyste.ms](https://awesome.ecosyste.ms/projects/github.com%2Fusetrmnl%2Ftrmnlp)
- **Zipline (CONFIRMED).** `--continuous` recompiles on change, and the served manifest is visible at `localhost:8080`. It "precompiles JavaScript into efficient QuickJS bytecode", "integrates Kotlin source maps into QuickJS bytecode", and includes "a sampling profiler". — [Zipline README](https://github.com/cashapp/zipline)
- **Glance (CONFIRMED).** To avoid restarting Glance after each change, set the widget cache to 1 second. The feature is still a work in progress: "The API may change in the future. You are responsible for maintaining your own extensions." — [Glance extensions doc](https://github.com/glanceapp/glance/blob/main/docs/extensions.md)
- **Home Assistant custom cards (CONFIRMED).** The docs give a documented lifecycle (`setConfig`, `set hass`, `getCardSize`, `getGridOptions`), locale‑aware formatters, and a visual "Card editor and picker" integration. — [Custom card](https://developers.home-assistant.io/docs/frontend/custom-ui/custom-card/)
- **Projectivy (CONFIRMED).** A sample plugin plus an API module, with guidance on caching and on events. "This sample is provided as is … should serve as a quick start." 40 stars. — [projectivy-plugin-wallpaper-provider](https://github.com/spocky/projectivy-plugin-wallpaper-provider)
- **Tasker (CONFIRMED).** "Save JS Library Template" gives dummy functions for off‑device testing, and a public plugin list is linked from the Play listing. — [Tasker JavaScript guide](https://tasker.joaoapps.com/userguide/en/javascript.html), [Tasker on Play](https://play.google.com/store/apps/details?id=net.dinglisch.android.taskerm)
- **Figma's DX trade‑off (CONFIRMED).** Figma first chose Realms because "the browser's built‑in debugger can debug plugins … with the same rich development experience". Switching sandboxes "is disruptive to us, and more importantly, disruptive to our developers". — [An update on plugin security](https://www.figma.com/blog/an-update-on-plugin-security/)
- **Declines (CONFIRMED).**
    - DashClock was marked "DEPRECATED", last pushed 2017. — [romannurik/dashclock](https://github.com/romannurik/dashclock)
    - The Locale plugin API repo is archived. — [android-plugin-api-for-locale](https://github.com/twofortyfouram/android-plugin-api-for-locale)
    - Tidbyt PRs stopped after the Modal acquisition, leading to a hard fork. — [tronbyt/apps](https://github.com/tronbyt/apps)
    - Figma halted publishing and updates during its 2019 incident. — [Figma](https://www.figma.com/blog/an-update-on-plugin-security/)
    - Runtime bindings went stale: taoweiji/quickjs‑android (2021), wasmtime‑java (2023), LuaJ (2023). — [GitHub API data in Section 1 table](https://github.com/taoweiji/quickjs-android)

### Inferences
- **A no‑hardware preview loop draws authors.** For a TV host, the equivalent of `pixlet serve` is a desktop or browser simulator that renders a widget at TV sizes without a TV. Homeboard's existing LAN phone editor is an obvious place for live preview. A template generator and a CI validator (Tidbyt/HACS‑style) lower the barrier further.
- **A typed, versioned contract eases upgrades.** Projectivy's `apiVersion`, Muzei's API library and HA's lifecycle methods each let authors target a stable surface. Glance's "may change" disclaimer pushes the upgrade burden onto authors.
- **Ecosystems live or die with the host.** DashClock and Tidbyt suggest that third‑party ecosystems decline when the host app or company stops investing, whatever the plugin API's quality. A forkable index under an open licence reduces that dependency.
- **Debuggability favours the browser engine.** Interpreter sandboxes (QuickJS‑in‑VM) cost debuggability compared with WebView/browser engines, as Figma noted. Zipline's source maps and profiler show what closing that gap takes.

### Gaps
- **Why DashClock declined.** No primary source states the cause; press attributes part of it to Android 5.0 removing lock‑screen widgets (UNVERIFIED).
- **Ecosystem size.** No figures were collected for the number of third‑party plugins in Projectivy, Muzei or DashClock.
- **Android‑TV‑specific plugin author communities.** None found beyond the Projectivy sample repo.
