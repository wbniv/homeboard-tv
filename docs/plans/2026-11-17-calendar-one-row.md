# Calendar one-row resize

The calendar currently clamps both dimensions to two cells in the remote resize
handler and in persisted layout validation. Will needs a one-row calendar.

- [x] Share a minimum span of **2 columns × 1 row** between resizing and saved
  layout validation. Preserve the existing default size and saved larger sizes.
- [x] Use a compact horizontal strip when the available height is below 190 dp:
  one heading with page count, square date cards, and one status footer. Keep
  the date number, icons, flag bands, month/year and one readable event label;
  full schedules and additional events remain available through date details.
- [x] Preserve paging, source ranges, empty/incomplete/offline states and the
  normal layout at taller sizes. Resize Save/Cancel/Undo remain unchanged.
- [x] Test persistence and packing at one-row sizes and bounded card geometry.
  Run container checks and build; install the same frozen build on both TVs.
- [x] Verify actual resize and compact rendering on Cast1 through coordinator ownership.

![One-row calendar with square date cards](2026-11-17-calendar-one-row.svg)

```mermaid
flowchart LR
    A[Calendar: hold OK] --> B[Resize]
    B --> C[Up reduces height to one row]
    C --> D[Compact date strip with paging and status]
    D --> E{Finish}
    E -->|OK| F[Save one-row span; survives reload]
    E -->|Back| G[Restore previous dimensions]
```

Build 0.1.27 is installed on both TVs. Container checks and actual TV acceptance
are tracked in the [validation record](../testing/2026-11-17-calendar-one-row.md).

## US country band correction · 25 November

Will requests equal-width red/white stripes across the left two-thirds and a blue
right third. [Enlarged reference](2026-11-25-calendar-us-band.svg). Draw thirteen
alternating red/white stripes in the existing 7-unit band; retain its size and all
calendar content/navigation. Apply the [shared Cooper filter](/home/will/docs/howtos/alan-cooper-design-filter.md):
make the small country marker recognizable from the sofa while preserving the
written country label. No settings, new workflow or migration is needed. Verify
the Thanksgiving US marker in a real dashboard capture after container checks,
then deploy with the About resolution update.
