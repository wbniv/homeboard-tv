# Homeboard startup ANR: retained-trace finding

**Status: original blocked startup path identified; background-loading fix implemented at `2c370f4` and accepted on Cast1 as 0.1.130; other fleet installs pending.**

## Exact-exit evidence

On 23 November, coordinator 0.1.229 (`14899926`) completed owned passive Cast1 diagnostics `J‑f69f2e716471` for `net.biohack.homeboard.debug`, PID 10163, exit timestamp 1795436555006 (19:22:35.006 Asia/Bangkok). This is the original Homeboard 0.1.128 preflight ANR, not replacement PID 10662.

The system description is `Input dispatching timed out (Application does not have a focused window)`. Android returned an untruncated 58,787-byte retained trace; its header identifies PID 10163 and a dump at 19:22:30.824948. Independently verified SHA-256: `5661e7d0742a8bbb77cf28b7376119b542e156d8402c16671879c3286ef26013`.

Private evidence directory:
`/home/will/homeboard-tv/docs/diagnostics/homeboard-retained-anr-20261123/exact-exit-229/`

The trace is `app-diagnostics/retained-anr-trace.bin`. It contains text despite the generic artifact suffix. Keep the full trace and account/device records private.

## Observed dependency

The original process's main thread is Waiting in:

1. `SharedPreferencesImpl.awaitLoadedLocked` → `getString`
2. `AirQualityCard.legacyConfiguration` (installed line 254)
3. `AirQualityCard.<init>` (94)
4. `MainActivity.airFor` → `buildHome` → `render`
5. `MainActivity.onCreate` (358), before activity launch completes.

The `SharedPreferencesImpl-load` thread is Runnable inside XML map parsing (`KXmlParser`, `XmlUtils.readMapXml`, `SharedPreferencesImpl.loadFromDisk`). This establishes a synchronous startup dependency on preference loading. It is not a deadlock diagnosis: the loader was running when sampled. The trace does not establish whether the file was unusually large, storage was slow, data was damaged, or another load was contending.

The pre-fix source opened per-widget preferences and called `legacyConfiguration`, `readHistory` and `readCache` from the air-card constructor, while `MainActivity.onCreate` synchronously rendered the initial dashboard. Calling `PhoneConfigurationStore.readAir(..., legacyConfiguration())` also evaluated the fallback before the store could decide whether to use it. The blocking frame in the original trace specifically identifies `legacyConfiguration`.

The later replacement process's skipped frames and GPU warnings are not evidence of this earlier ANR's cause.

## Passive collection and limits

- [x] Exact process/PID/timestamp selected through the installed shared Taskfile.
- [x] Full trace exported and independently verified against its size/hash.
- [x] Request used `LAUNCH_APP=false`, no test settings and no system trace. Result reports foreground unchanged and no new exit records.
- [x] Job succeeded, health ready, no survivors and every journal effect restored. No app history/data was cleared or reset.
- [ ] Enabled accessibility-service state was not independently sampled before and after this passive request. No accessibility mutation or app force-stop was requested; do not turn that into a measured state comparison.
- [x] Move blocking air-card initialization/cache/preferences work off the UI startup path, preserving the user's selected source, location, station, cache/history and existing phone/Undo behavior. Show an appropriate loading state until initialization completes; do not silently use defaults as the final saved configuration.
- [x] A controlled delayed-loader regression proves the UI executor can handle input while saved preferences/configuration remain blocked. Seven startup regressions cover publication, close, retry and read-only station/model cache/history restoration; all 668 JVM tests passed in the isolated source run.
- [x] Cast1 0.1.130 cold launch completed and the applied screenshot shows the populated dashboard; no new exit records in the diagnostic interval. Home On / Working restored through the labelled helper. Full-screen model history and both dashboard-return paths passed. See the acceptance record below.

No storage corruption claim or preference deletion was made. Passive collection itself made no app change. The subsequent [startup fix](../plans/2026-11-23-air-quality-startup.md) loads a read-only snapshot in a worker, publishes it on the UI thread, and gates configuration until the saved state is ready. Deployment 0.1.130 passed container checks/lint/package and installed on Cast1 with a verified matching hash. Cast2, Pixel and tablet requests remain queued pending connectivity.

## Coordinator release history

This session's reviewed 0.1.227 / `586e020a` upgrade failed at 20:44 because the protected recovery snapshot changed during upgrade; its helper rolled back and retained maintenance. It was not a successful deployment. A separate newer 0.1.229 / `14899926` was subsequently installed and verified live. The automatic query was pinned to the old exact revision and did not run; its watcher has been stopped. The exact request above was then submitted explicitly on 0.1.229 at about 21:17 and completed normally.

Exact Home-release latency and flash duration remain parked, independently of this ANR finding.

## Fix acceptance and limits

[Implementation and complete acceptance record](../plans/2026-11-23-air-quality-startup.md). Cast1 install `J‑95ed96c67664`, cold launch `J‑d7bbeb06fb9a`, and Remote `J‑d27d7c5781c9` exported evidence normally. All 668 Java tests passed, including seven startup regressions; Android lint passed.

The updated dashboard retained the model source, 13.70°/100.50° location and populated trend/history views. The full-screen seven-day history contained 168 samples and retained Asia/Bangkok timezone. Back and Restore dashboard preserved air-card focus/size. Final Home status is On / Working; only the labelled test helper was enabled, with the other three listed services still Off. This run does not establish a new YouTube-to-Home acceptance or exact transition timing.

No new exit records occurred in the approximately seven-second post-launch diagnostic sample (collector overhead included). This is a successful cold launch and targeted regression evidence, not a guarantee against every possible ANR or a first-window latency measurement. Initial pre-update frames showed Google TV rather than the air card; no byte-for-byte device-data comparison is claimed.
