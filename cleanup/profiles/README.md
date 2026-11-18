# Reviewed cleanup profiles

There are no approved execution profiles yet. The upstream Chromecast files in
`vendor/cleanyourtv-cleanup/examples/` are reference inputs, not active profiles.
The companion currently performs coordinator-owned read-only inspection. Its
HTTP API has no package-change, restore or shell endpoint.

A future execution profile must match the stable enrolled device identity,
manufacturer, model, product, board and full firmware fingerprint; include an
exact reviewed upstream revision; and contain an explicitly approved small
allowlist with per-package labels, reasons and dependency review. No brand-only
fallback is accepted. New TVs remain scan-only until this review is complete.
