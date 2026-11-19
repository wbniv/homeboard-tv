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

- [wip T4] **Android app widget hosting**: implement normal consent, saved instances, full-screen interaction, sizing and Undo; test the dedicated fixture, Fossify Calendar and Breezy Weather on Cast1/Cast2, Pixel and tablet. Stage-0 probe implemented and installed on both TVs; Both TVs lack normal binding consent, but the authorized developer-grant experiment proved separate-app binding/rendering on Cast1 and Cast2 with restored permissions and zero test IDs. Pixel census passed; tablet remains unverified. Product host and explicit one-time setup implemented; 0.1.95 also keeps setup/review explanations visible. Cast2 native fixture acceptance passes, including expansion/Restore/Back and phone Remove/Undo with the same binding. Both TVs have the identical 0.1.95 APK installed; Cast1 Add/expansion and Cast2 fixture/real-provider placeholder checks passed. Broader provider/mobile configuration and resource/24-hour checks remain open. [Plan](docs/plans/2026-11-19-android-app-widgets.md).

- [wip T4] <!-- agent:ad004e92406994819 --> **Opt-in ads that pay the user (Homeboard Rewards)**: the only kind of advertising Homeboard may carry. Will, 2026‑11‑19: "add TODO for ads which pay user. i don't otherwise want to have ads". The plan is written: commission share first, then GrabRewards, with sponsored offers last behind gates; PromptPay payouts with no wallet; off by default; and a `nocommerce` build. It is waiting on Will's seven decisions and on gate 0 (affiliate terms) and gate 1 (counsel) before any code. Tier T4: the payout, policy and legal risks make a wrong turn expensive. See [plan](docs/plans/2026-11-19-ads-that-pay-the-user.md) and [investigation](docs/investigations/2026-11-19-widget-monetization.md#ads-that-pay-the-user).
- [T4] **Shop this list: handoff and the commerce setting**. On the phone, a panel offers Copy for Grab (for its Shopping Agent), a pre-filled LINE message to the household's shop, a Grab checklist mode, and copy or share. The TV's full screen gets a QR code. The Off/Plain/Tagged setting adds disclosed tagged links: Plain needs no accounts, while Tagged waits for verified affiliate terms. It is a prerequisite for Rewards. Write the plan with mockups first, linking to the Groceries plan (owned by another session) rather than editing it. Tier T4: a new design across the phone controller, the TV full screen and consent. See [investigation](docs/investigations/2026-11-19-widget-monetization.md#recommendation-and-sequence).
- [T5] **Rewards gates 0–1: affiliate accounts, payout provider, counsel**. Apply to the Lazada TH programme and to Involve Asia, then read the Thai offer terms on app traffic, cashback and sub-IDs. Confirm 7‑Eleven ALL ONLINE's terms, choose and price a licensed PromptPay payout provider, and decide on Thai counsel for checklist items L1–L14. Do this after Will's Rewards decisions. Tier T5: it needs Will, vendor consoles and contracts. See [plan](docs/plans/2026-11-19-ads-that-pay-the-user.md).

## Watch

_Nothing being watched. Entries here are plain bullets: what to check, how often, and the
trigger that promotes it back to Open._

## Parked

- [T4] **Rewards stage 2: GrabRewards points payout.** It needs a Grab partnership. Unpark when Will authorizes approaching Grab and stage 1 has payees. See [plan](docs/plans/2026-11-19-ads-that-pay-the-user.md).
- [T4] **Rewards stage 3: sponsored offers, ad server and sponsor reporting.** Unpark only when at least 2,000 households are in Shared, a sponsor has asked, and counsel and Play review have cleared it. See [plan](docs/plans/2026-11-19-ads-that-pay-the-user.md).
- [T2] **SMS one-time-code check of payout numbers.** In v1 the provider's name check and the payee's confirmation are enough. Unpark if a misdirected payout happens in the pilot. See [plan](docs/plans/2026-11-19-ads-that-pay-the-user.md).

## Done

_Nothing recorded yet. Newest first, one line each:_
_`- [x] YYYY-MM-DD — [slug] what was done (~130 chars). See [plan](path).`_


## Inbox — auto-captured plan deferrals

_Auto-added from plan "Out of scope"/"Deferred" sections at commit time. Triage each into M1/M2/etc. and delete it here — it will not come back._

<!-- BEGIN auto-captured-deferrals (managed by audit-plan-deferrals.sh — triage these into the curated sections above; the fingerprint ledger means a deleted item is NOT re-added) -->
- [verify] **2026-11-19-cleanup-execution** — Verification section present but no PASS recorded — run + record the steps. _from [2026-11-19-cleanup-execution.md](docs/plans/2026-11-19-cleanup-execution.md)_  <!-- fp:973775ad1f11e4dc -->
<!-- END auto-captured-deferrals -->
