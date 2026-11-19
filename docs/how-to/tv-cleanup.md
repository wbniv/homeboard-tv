# Inspect a TV before cleanup

The companion supports standalone connection, full inventory and a reviewed
cleanup/recovery workflow. Real-device execution remains gated by exact approved
profiles; no real TV profiles are supplied yet. The coordinator mode currently
supports read-only inspection, pending a reviewed apply/restore workflow.
See the [execution plan](../plans/2026-11-19-cleanup-execution.md).

## Connect a standalone TV

On a Linux setup computer with Docker and Task, run `task build-image` once, then:

```sh
task cleanup:standalone
```

Open the loopback address printed in the terminal. The tool container supplies
platform-tools; no host Android SDK or coordinator enrollment is needed. State,
pairing identity and inspection reports are private under
`~/.local/state/homeboard-cleanup-standalone/`. Keep the terminal open while using
the companion. This command uses Linux host networking; Docker Desktop networking
on macOS/Windows has not been verified.

On the TV, enable its developer options and **Wireless debugging**, then choose
**Pair using pairing code**. Enter the pairing address and six-digit code in the
companion's Pair section. After pairing, return to the main Wireless debugging
screen and enter its **debugging address** under Connect; these ports differ.
**Find wireless TVs** can fill discovered addresses, or type them manually.
This flow requires code-based wireless debugging support on the TV.

After connecting, check the model and serial identity, tick **This is the TV I want
to inspect**, then choose **Inspect this TV**. Changing the selected TV clears that
confirmation. **Disconnect** closes only that endpoint; it does not revoke pairing
or switch off TV debugging. To forget this computer, use the TV's paired-device
list. Reports remain read-only and do not approve a cleanup profile.

The standalone adapter has its own ADB server socket and identity. It never falls
back from coordinator jobs. Use the coordinator mode below for shared lab TVs.
Connection/browser verification uses controlled fixtures; physical standalone
pairing on the new hardware remains to be checked.

## Open the coordinator companion

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

## A new TV in coordinator mode

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

## Review, apply and recover

In standalone mode, connect and confirm the TV, then choose **Review cleanup
suggestions**. This gathers exact primary-user package state, Home providers and
active process ownership. Missing or ambiguous facts stop the review. Unknown
TVs show no eligible changes. Suggestions require a trusted exact device/firmware
profile with package-version-specific dependency review; nothing is preselected.

Select eligible apps and choose **Review selected changes**. The companion saves
and verifies both private recovery copies before offering **Apply these changes**.
Export a recovery copy from that screen. Confirm the target TV and listed apps;
confirmation expires after two minutes. Package changes are reversible per-user
disabling, preserving original states. **Stop after current item** prevents the
next change. Interrupted operations require **Check result**, not a blind retry.

**Saved sessions** remains available after companion restart. Reconnect and confirm
the same TV to check results, restore an individual item or restore the session.
This works independently of Homeboard running on the TV. Original default versus
explicitly enabled states are retained. Changed firmware/Home/package identity or
unresolved external state stops restoration for review. Recovery files are under
`/data/sessions` and `/data/recovery-copy` in the standalone container's private
host-mounted state directory; preserve that directory across restarts/updates.

The complete browser execution flow has controlled-fixture proof; it is not a
claim of cleaning/restore acceptance on physical new TVs. See
[execution verification](../testing/2026-11-19-cleanup-execution.md).
