# Inspect a TV before cleanup

This pilot provides **read-only inspection** and a tested recovery/policy
foundation. It does not yet clean packages on real TVs. There are no approved
execution profiles and no package-change, restore or shell route in the running
companion. The full cleanup integration remains tracked in the
[plan](../plans/2026-11-18-tv-cleanup-and-restore.md).

## Open the companion

On the TV, open **Home settings → More → TV cleanup · Inspect only**. This shows
instructions and returns to Settings with Back. Opening it changes no packages.

On the trusted development computer, from the Homeboard checkout, run:

```sh
task cleanup:serve
```

The companion opens at a temporary `http://127.0.0.1:<port>` address on that
computer. Choose the enrolled TV and select **Inspect this TV**. It checks devices
and queue, submits an `inspect` job, watches it and downloads its evidence. It
uses the shared coordinator; neither the browser nor the TV app gains an ADB or
general shell interface. Keep the terminal running until the inspection finishes.

The report shows the target's manufacturer, model, product, board, Android build,
firmware fingerprint, reported package count, capture time and job reference.
That hardware report is not an exact enabled-state/dependency audit. **Apply
unavailable** stays disabled; existing apps, launchers and settings are retained.
English and Thai are available, with BE years in Thai report timestamps.

This initial companion is for the current Linux development setup with Python 3,
Task and an authorized coordinator client. It is a separate computer tool; the
existing TV-hosted phone widget editor still edits the dashboard only. No public
standalone cleaner or mobile cleanup application is claimed.

## A new TV

1. Connect it normally and complete the manufacturer's setup.
2. Enroll it through the coordinator's **Add a device** workflow. Keep pairing
   codes in its secure prompts. [Coordinator enrollment guide](../../../chromecast-coordinator/docs/tray-user-guide.md#add-a-device).
3. Refresh **Find TVs** in the companion and choose its enrolled identity.
4. Inspect it before selecting an upstream profile or considering changes.

An unrecognized model is expected to stay scan-only. A package list for a similar
brand is not enough. The next cleanup stage requires exact identity/firmware,
reviewed dependencies, an approved small package list and independent restore
proof. We will prepare and review that concrete list before any real disabling.

## Evidence and checks

Private job references/evidence are saved under
`~/.local/state/homeboard-cleanup/`, with owner-only directories. Override this
with `task cleanup:serve -- --data /absolute/private/directory`. Do not publish
raw job artifacts: a hardware inspection can contain household/device details.
Stop the companion with Ctrl+C after any accepted job finishes. Closing a browser
tab does not cancel a coordinator job.

Run `task cleanup:check` for the copied upstream tests and Homeboard policy,
durability and HTTP security tests. Browser verification uses the same pinned
Playwright container and dependencies as `task phone:setup`:

```sh
task cleanup:browser-check
```

The browser screenshots use a controlled fake TV. Real-device reports and native
TV menu captures are recorded separately in
[the verification record](../testing/2026-11-18-tv-cleanup.md).
