# Reviewed cleanup profiles

There are no approved execution profiles yet. The upstream Chromecast files in
`vendor/cleanyourtv-cleanup/examples/` are reference inputs, not active profiles.
Standalone mode loads reviewed profiles from this directory and exposes narrowly
scoped review/apply/restore actions through the existing journaled recovery engine.
Coordinator mode remains read-only until its execution workflow is integrated.
There is no general shell endpoint. Empty profiles mean no real-app candidates.

A future execution profile must match the stable enrolled device identity,
manufacturer, model, product, board and full firmware fingerprint; include an
exact reviewed upstream revision; and contain an explicitly approved small
allowlist with per-package labels, reasons and dependency review. No brand-only
fallback is accepted. New TVs remain scan-only until this review is complete.
