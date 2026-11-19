# TODO — homeboard-tv

**Status markers:** `[ ]` open · `[wip]` in progress · `[verify]` implemented, verification
not yet run+recorded (run the linked plan's steps, paste raw output + PASS/FAIL back into
the plan, then promote) · `[x]` done (`## Done` only).

**Delegation tier:** the bracket also carries a `T0`–`T5` rank, tier last — `[T4]`,
`[wip T2]`, `[verify T3]`. It says how much *thinking* the item needs, and `/next` uses it
to dispatch to the cheapest model that can do the work:

| Tier | Work | Goes to |
|---|---|---|
| `T0` | Mechanical lookup — grep, count, "which file defines X" | haiku, read-only |
| `T1` | Mechanical edit with a known recipe | sonnet @ medium |
| `T2` | Bounded implementation — one module, clear spec | sonnet @ high |
| `T3` | Multi-file work against a settled plan — **default when unranked** | opus @ medium |
| `T4` | Design *and* implementation; unknown root cause | opus @ high |
| `T5` | Do it yourself inline — or it needs a human, hardware, or a vendor console | *(no agent)* |

Ranking is itself `T5`: the orchestrator assigns tiers inline and never delegates that
step, at any scale. Only `## Open` carries tiers — Watch, Parked and Done items are not
dispatchable, and an item returning from Watch or Parked is ranked fresh. Full rubric:
`~/CLAUDE.md` — Delegation.

Plan-first: non-trivial work gets a `docs/plans/YYYY-MM-DD-<topic>.md` and an entry here.
Check conformance with `task todo:lint`.

## Open

- [T4] **Opt-in ads that pay the user**: the only kind of advertising Homeboard may carry. Will, 2026‑11‑19: "add TODO for ads which pay user. i don't otherwise want to have ads". Write a plan before building anything. It covers who funds the ads, the user's share and how it is paid in Thailand (cash, PromptPay or points), Play/PDPA/disclosure limits, an off-by-default consent flow and a no-ads guarantee for everyone else. Tier T4: the design is open and the payout, policy and legal risks make a wrong turn expensive. See [investigation](docs/investigations/2026-11-19-widget-monetization.md#ads-that-pay-the-user).

## Watch

_Nothing being watched. Entries here are plain bullets: what to check, how often, and the
trigger that promotes it back to Open._

## Parked

_Nothing parked. Entries here are plain bullets — intentionally shelved work, with the
condition that would unpark it. A parked bullet may keep its tier (`- [T3] …`) so the
ranking work is not thrown away; re-check it when the item is unparked._

## Done

_Nothing recorded yet. Newest first, one line each:_
_`- [x] YYYY-MM-DD — [slug] what was done (~130 chars). See [plan](path).`_


## Inbox — auto-captured plan deferrals

_Auto-added from plan "Out of scope"/"Deferred" sections at commit time. Triage each into M1/M2/etc. and delete it here — it will not come back._

<!-- BEGIN auto-captured-deferrals (managed by audit-plan-deferrals.sh — triage these into the curated sections above; the fingerprint ledger means a deleted item is NOT re-added) -->
- [verify] **2026-11-19-cleanup-execution** — Verification section present but no PASS recorded — run + record the steps. _from [2026-11-19-cleanup-execution.md](docs/plans/2026-11-19-cleanup-execution.md)_  <!-- fp:973775ad1f11e4dc -->
<!-- END auto-captured-deferrals -->
