# Disposable widget provider

Separate-package provider for the Android widget-binding experiment. No account,
network permission, periodic update or household data. Build/lint together with
`task widget-fixture`. Package `net.biohack.homeboard.widgetprovider`, provider
`.ProbeProvider`, launch activity `.ProviderActivity`, version 1 (`0.0.1`).

The RemoteViews body says `Widget bound across apps · id N`. The host must receive
and render this actual update before reporting success. Initial layout text says
Waiting for provider, so an inflated placeholder cannot pass the assertion.

Register/install through complete coordinator jobs. Launch-check the provider once
before the binding experiment. Remove only this disposable package via a supported
coordinator uninstall after acceptance; never clear user app data or Homeboard.
