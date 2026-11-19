# Android app widgets

Requires Homeboard **0.1.95 or newer**.

Homeboard can host widgets supplied by other apps installed on the same Android
device. Install the provider app first, then open **Home settings → Widgets /
Restore → Add widget → Android app widgets**. Choose the app/widget, approve
Android's request when shown and finish the provider's setup. Review **Add to
Home** or **Cancel**; cancellation leaves the dashboard intact.

## Chromecast permission setup

The Chromecast HD and Chromecast 4K tested here have Android's widget service but
no widget approval screen. They need a one-time developer connection to allow
Homeboard to bind widgets. Homeboard's **Widget permission setup** shows the
command for the exact installed package. Public APKs use `net.biohack.homeboard`;
the separate development installation uses `net.biohack.homeboard.debug`.

On your own device, after connecting Android debugging from a computer, run
`adb shell appwidget grantbind --package net.biohack.homeboard --user 0` for the
public app. Return to the widget picker and choose the widget again. To stop
allowing new bindings, use `revokebind` in place of `grantbind`. This does not
uninstall Homeboard or erase its dashboard. Developer connection setup is separate
from ordinary APK installation; built-in Homeboard widgets need none of this.

For the shared development devices, agents use the coordinator's typed
`widget-binding` recipe with a frozen Homeboard APK and `enabled: true` or
`false`. Use the normal devices/queue/submit/watch/evidence workflow. Do not run
raw ADB against those devices. The job records the previous setting and an inverse
recipe in its evidence. It changes no default Home selection or widget IDs.

## Use, arrange and recover

- **Press OK / tap** the tile to open the provider widget plus app/version and
  saved-size details. **Interact with widget** enters the provider controls.
  Some providers support touch only; Homeboard does not simulate touch with a
  remote. Back exits controls, then restores Home. **Restore dashboard** is
  always available.
- **Hold OK / long press** for Move, Resize, Remove or Set up again. Configure
  appears only when the provider supports reconfiguring an existing instance.
  Provider setup and data changes happen in that app; Homeboard Undo restores
  the dashboard arrangement rather than reversing those external changes.
- The provider's minimum size and resize directions constrain explicit changes.
  If a denser Home grid leaves too little space, the tile keeps its saved place
  and shows a size message. Resize or change the grid to recover it.
- Remove keeps the original binding while the existing 20-entry Homeboard Undo
  history references it. Undo restores that instance's configuration. After
  history eviction, unreferenced bindings are released. Hidden instances have
  no live host view, though the provider can still update them.
- A missing provider or lost binding leaves an unavailable tile. Install the
  provider again and use **Set up again**. Cancelling replacement keeps the old
  tile. Homeboard does not promise to recover data deleted by the provider app.
- Interrupted setup offers Retry / Cancel on return. A pending operation is saved
  before Android consent/configuration opens and cleared with the dashboard save.

The paired phone browser can move, resize and remove configured widgets using
its usual preview, Update TV and Undo. Adding and provider setup happen on the
host TV/device. The browser shows the widget label and arrangement; it does not
stream the provider's widget UI. Up to four Android widget views are live on the
visible Home page. Additional labeled tiles open their live view full screen.

See the [testing record](../testing/2026-11-19-android-app-widgets.md) for actual
provider/device evidence. The disposable provider proved cross-app hosting on
both Chromecasts; hosting was checked on both TVs from 0.1.95. See the testing
record for current builds and required-configuration acceptance. The fixture rendered and expanded on
Cast1; integrated host and phone-Undo assertions passed on Cast2, and Breezy's unconfigured Current widget was added, expanded and undone through normal
TV menus. Configured weather data and provider controls remain unverified. Pixel's
provider/consent census passed; local hosting and tablet acceptance remain
pending. Compatibility is per provider and device, especially for remote input.
