# Calendar Today header

Build **0.1.65** adds today's TV-local weekday/date/year before the holiday range
in both compact and normal widget headers and the accessibility description.
It uses the existing localized date formatter, including Thai Buddhist Era years,
and the widget's existing periodic refresh. English and Thai page indicators now
show only the page count; the redundant full-screen OK instruction is removed.

## Build and installation

Container checks passed: calendar pipeline, 52 cleanup/recovery tests, deployment
helper tests, 478 JVM tests, Android lint and 640 English/Thai resource checks.
The frozen APK SHA-256 is
`b84f8bd75d16500d1517a912bdf870f10ce6c5ccd06c7bc8c6617e0fb9434316`.

- Cast1: **J‑d33a63f4a734**, installed and launch-verified.
- Cast2: independent install **J‑12c96ea35fd3** was granted, then failed
  `install-over-foreground`. Installation is unconfirmed; the coordinator reported
  the app was showing and made no change. No personal-use marker or Home app
  assignment was altered to obtain installation.

## Screenshot acceptance

The isolated Thai dashboard probe additionally asserts that each Calendar widget's
accessibility description starts with today's localized date. Physical screenshot
verification passed in **J‑0ffff6efdb71** on Cast1, with a successful coordinator
receipt and passing native report for 0.1.65. The
[real TV screenshot](2026-11-19-calendar-today/cast1-thai-dashboard.png) was visually
reviewed: today's Thai weekday/date/BE year is visible, and the footer shows
`1/2` without the OK instruction. It preserves household preferences
and the saved app language. This is header/presentation acceptance, not proof of
a client-side lunar calculation; that remains a
[separate proposed plan](../plans/2026-11-19-client-side-wan-phra.md).
