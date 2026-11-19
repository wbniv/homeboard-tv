# Standalone cleanup connection verification

The standalone companion adds mDNS discovery, code pairing, explicit connection,
TV identity display/confirmation, read-only inspection and targeted disconnect.
No real TV was contacted for this record. Cast1/Cast2 remain coordinator-managed.
No APK changed or was rebuilt for this computer-companion-only implementation.

## Container checks

`task cleanup:check` passed **60 tests**. It runs copied upstream recovery tests, Homeboard policy,
connection backend and loopback HTTP tests. Connection cases cover invalid
endpoints, pairing-code secrecy, isolated identity/server environment, targeted
read/disconnect, mDNS parsing without connection, non-TV rejection, identity
changes, errors/timeouts and no published report after rejected identity.
HTTP cases cover same-origin authorization, blocked package-change routes,
confirmation before inspection and concurrent connection request refusal.

The existing coordinator-mode browser suite also passed in Chromium and WebKit.

## Browser proof

`task cleanup:connection-browser-check` passed in Chromium and WebKit using the
real HTTP companion with controlled transport. Both exercised Pair, cleared code,
Connect, displayed identity, confirmation gating, inspection and Disconnect.
Thai mobile pages have no horizontal overflow. These screenshots contain fixture
identities and addresses, not household devices or real pairing credentials.

| Browser | English connected controls | Thai mobile inspection |
|---|---|---|
| Chromium | [Screenshot](2026-11-19-cleanup-connections/chromium-en.png) | [Screenshot](2026-11-19-cleanup-connections/chromium-th.png) |
| WebKit | [Screenshot](2026-11-19-cleanup-connections/webkit-en.png) | [Screenshot](2026-11-19-cleanup-connections/webkit-th.png) |

Physical new-TV discovery/pairing, the TV's authorization prompts, host-network
Docker connectivity and reconnect across computer restarts remain unverified.
Package changes and restoration are separate integration work. See the
[plan](../plans/2026-11-19-cleanup-connections.md) and
[setup guide](../how-to/tv-cleanup.md).
