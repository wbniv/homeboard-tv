# PM2.5 30-day history and sparklines

**Status: implemented and captured on Cast1 · 16 November 2026**

Add a 30-day PM2.5 trend alongside the current 24-hour sparkline. Backfill from
AirBKK's public report when the panel first loads, then retain new hourly samples
in a fixed-capacity circular buffer.

## User-facing design

- Keep two independent, labeled sparklines on the PM2.5 panel: **24H** and **30D**.
- The 24H chart shows the last 24 hours. The 30D chart shows up to 720 hourly
  samples from the last 30 days. Both plot AirBKK's rolling 24-hour PM2.5 average,
  so the graphs never imply that each point is an instantaneous PM2.5 reading.
- Use the current Thai PCD condition color for both traces and the full card
  background. Keep the current numeric value, text condition, and high-reading cue.
- If history is unavailable, leave the corresponding trace empty and keep the
  live reading usable. Do not fill missing hours with interpolated values.
- Keep the existing panel size choices. The paired charts fit in one chart area;
  Medium remains useful and Large adds breathing room.

## Mockup

![PM2.5 panel with separate 24-hour and 30-day sparklines](../2026-11-15-flexible-home-grid/air-quality-mockup.svg)

The labels sit beside each line, so each time range is clear on a TV from a
distance. The high-state example uses orange for both the chart and card surface.

## Data and storage

The app requests the selected station's latest 30-day interval from the [official
AirBKK report](https://official.airbkk.com/airbkk/th/report). Its hourly result
contains both raw hourly values and a separate `mvavgData` series. Store only
`mvavgData`, which matches the live card's rolling 24-hour average. A 30-day probe
returned 719 raw hourly records and 721 moving-average timestamps for the inclusive
range; the ring retains the newest 720 unique timestamps.

`AirQualityHistory` uses a fixed 720-slot circular buffer. It stores samples in
chronological order, replaces a duplicate timestamp, merges out-of-order backfill,
and evicts the oldest point when a newer sample fills the buffer. JSON persistence
remains bounded to those 720 samples per selected station. Old 24-point caches are
accepted and expanded by the first successful backfill.

```mermaid
flowchart LR
    A[Panel opens] --> B[Load cached ring]
    B --> C[Show live value and any cached history]
    C --> D[Fetch live AirBKK reading]
    D --> E[Request official 30-day moving-average series]
    E --> F[Merge timestamps into 720-slot ring]
    F --> G[Draw 24H window and 30D window]
    G --> H[Persist bounded history]
    H --> I[Append future station updates]
    I --> F
```

## Alan Cooper design review

- Answer “what is it now?” first. Keep the current value and status above the
  historical context.
- Label both ranges in the graph itself and in accessibility content.
- Plot one official averaging method for both horizons; do not mix raw hourly
  concentration into the 30-day line.
- Keep the card operational if historical backfill fails. Surface the live value
  and retry history on a later refresh without fabricating points.
- Preserve the remote focus outline and condition-colored background while the
  trend is passive and noninteractive.

## Acceptance

- [x] A 720-entry fixed circular buffer persists chronological unique samples,
      replaces duplicates, merges out-of-order history and evicts oldest points.
- [x] AirBKK backfill requests 30 days of hourly data and uses only its 24-hour
      moving-average series.
- [x] UI renders separate 24H and 30D sparklines with distinct visible and
      accessible labels.
- [x] Container unit tests, lint and APK build pass.
- [x] Cast1 shows both traces and keeps Homeboard installed with the new APK.
- [x] Inspect the Medium panel screenshot: both traces, condition background,
      labels and data detail are visible. Large still needs visual review.

## Verification record

`task check && task build` passed in the build container. The 30-day debug APK
SHA-256 is `915acb04082c4367688ab6c10a619c106edc3d3c5bd203fcb340095d1babc360`.
Coordinator job `J‑404d82ded33d` verified that hash installed on Cast1; its
scripted-input capture hit a plugin error and automatic recovery completed.
Standalone coordinator capture `J‑fe9ebca377eb` then succeeded. OCR passed for
PM2.5, Bangkok and AirBKK. The screenshot is in
`evidence/cast1-pm25-30day-capture-2026-11-16/` and is embedded in the quick-start.
