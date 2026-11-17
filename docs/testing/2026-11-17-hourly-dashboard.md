# Shallow hourly Rain and bottom Favorites dashboard

Status: implemented, installed on both TVs as **0.1.35**, and applied/visually
verified on Cast1. Cast2 preset application is queued during Personal use.

## Changes

The explicit Default dashboard preset and fresh-install layout use 8×4 logical
placement: Rain 8×1, Calendar 5×2, Air quality 3×2 and Favorites 8×1 at the bottom.
Applying the preset saves grid choice and geometry through the existing Undo
snapshot. Existing saved layouts are not repeatedly rearranged on launch.

Widget dashboards can use the explicit widget grid independently of standalone
app icon pitch. The wide shallow Rain presentation labels every interval's hour
and chance, uses amount bars with a stated scale and keeps a concise daily strip.
It chooses two 12-hour rows when measured width and height permit; manually tiny
cards retain adaptive glance rendering. Existing configured forecast ranges remain.

Air retains both 24-hour and 30-day traces in a taller 64dp chart region within
its smaller outer rectangle. The traces use elapsed time and leave history gaps
longer than two hours disconnected. Missing history is explicitly unavailable.
Favorites reuses the horizontal one-row container, passive wedges and edge exit.
The combined source includes the separately developed richer full-screen widgets.

## Container checks

`task check` passed 437 JVM tests, six calendar checks, two deployment-workflow
checks and English/Thai parity for 535 resources. Debug lint passed. `task deploy`
runs the required checks and assembles the newly numbered APK in the container.

## Device acceptance

The initial 0.1.34 background install on Cast1 was refused with
`install-over-foreground`; the coordinator-owned foreground check then installed
it with verified hash. Cast2's background installation was also verified.
The ordinary MainActivity menu applied the new preset on Cast1, retaining the
Lumpini Park AirBKK source and all nine favorite apps. Three actual captures show
all 24 hourly labels and chance values, six daily summaries, both 24H/30D Air
sparklines, Calendar and the full-width one-row Favorites panel, plus All Apps and
Back. The script later activated Android settings because header focus had not
been moved back to Rain. Its guard stopped the run; cleanup restored MainActivity
safely and automatic coordinator recovery returned the TV to ready. This was a
script error; that whole job is not counted as a passing regression.

A newer combined 0.1.35 deployment appeared in the shared workspace. Its complete
380-file launcher source manifest matches 0.1.34's manifest. Follow-up checks use
that newest frozen build, without reinstalling an older number.

**Passing follow-up:** J‑0f70faefb3e1 completed with healthy device and safe cleanup.
The exported captures were inspected individually:

1. Rain full screen, with hourly amount/chance detail and Restore dashboard.
2. Back restores the shallow Rain card and all four-widget layout.
3. Last of nine favorites selected after internal horizontal scrolling; the left
   wedge is visible, no scrollbar and no second app row.
4. Right from the final favorite opens All Apps.
5. Back restores the same final favorite and its scroll position on Home 1/1.

Private evidence: `artifacts/deployments/0.1.35/hourly-followup/`.
The initial layout captures are under
`artifacts/deployments/0.1.34/dashboard-acceptance/`.

Both TVs have verified installed SHA-256
`b77e9129e5d89b8bf77e24afe467b4fbc841af4684f91e4aae7d072884e92d87`
for **0.1.35**. Cast1 proof is in the passing follow-up's `apk-installations.json`;
Cast2 proof is under `artifacts/deployments/0.1.35/cast2-hourly-install-proof/`
from J‑889491fc2b4d. Background installation succeeded during Personal use;
foreground launch verification remains deferred.

J‑720a69f46fd1 queues the ordinary preset application/check on Cast2 until the
coordinator admits foreground work. No personal-use marker, default Home app or
household data was cleared. The old 0.1.34/0.1.32 verification continuations owned
by this work were canceled as superseded.

## Remaining limits

Actual acceptance covers the normal-text TV composition shown in the captures.
The two-by-twelve narrow-width fallback, large-text reflow, Thai visual layout and
missing-history rendering have not been independently accepted on-device in this
run. The measured-height guard keeps manually undersized Rain cards in their
adaptive presentation; it does not yet borrow height from the middle band as
proposed for a constrained large-text default. Full-screen source/settings features
have their own separate validation record. Grid/geometry, serialization and
configuration/favorites preservation are covered by the JVM tests; device Undo
was not replayed in this acceptance session.
