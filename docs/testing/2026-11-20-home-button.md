# Identify the Home button helper

**Historical record (20 November 2026).** Version numbers and pending checks below
refer to that session. The [November 23 summary](2026-11-23-home-button.md#current-summary-23-november-2026)
supersedes them for current status, including physical repeat, recovery and
Cast2's verified 0.1.119 installation.

Read-only Cast1 inspection `J‑7ffd7a5603a3` confirmed installed packages:

| Package | Installed version code at inspection | Identity |
|---|---|---|
| `net.biohack.homeboard` | 92 | Older public app |
| `net.biohack.homeboard.debug` | 113 | Current test app before this label correction |

Both exposed the inherited **Home button** label. The raw inspection did not
establish the upper/lower order in Android's accessibility list. No position-based
identification is claimed. Evidence: `docs/diagnostics/home-button-identify/evidence`.

The current debug app now overrides only its helper label:

- English: **Home button · Homeboard TV (test)**.
- Thai: **ปุ่มโฮม · Homeboard TV (ทดสอบ)**.
- Component: `net.biohack.homeboard.debug/com.gathin.tvhome.HomeKeyService`.

The public app retains **Home button**. Android's disabled Configuration row is
expected because the service metadata does not declare a settingsActivity. It does
not indicate that the Enable switch is blocked.

`task deploy` built **0.1.114**, passing 644 JVM tests and Android lint plus the
existing pipeline/cleanup/deployment checks. Container `aapt2 dump resources`
verified both labels in the frozen APK. Its SHA-256 is
`902f9b0c26218496065f1013476b8f1b5d72c611d42ea7b8e4b7d84795b3e616`.

| Device / operation | Job | Outcome |
|---|---|---|
| Cast1 background install | `J‑73190089cc9d` | Refused `install-over-foreground` |
| Cast1 independent foreground install/check | `J‑e4e5494a6208` | Passed; new installed hash verified |
| Cast2 install and launcher verification | `J‑8e54ae846147` | Passed |

Both targets received the same numbered APK with app data preserved. Per-device
evidence is under `artifacts/deployments/0.1.114`; Cast1's successful follow-up is
under `chromecast-test-01/foreground-check/evidence`.

No service switch was toggled during this identification/deployment step. Reopen
Android's accessibility settings to refresh its labels and use the explicitly
test-labelled entry for the current app. The refreshed list has not been captured
yet, and working Home routing is not claimed by this record. Earlier overlapping
remote input and restoration are recorded separately in the project plan.
