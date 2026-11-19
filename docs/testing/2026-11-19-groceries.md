# Groceries Phase 1/2 verification

Implementation branch: `feature/grocery-list`, worktree `.worktrees/grocery-list`,
based on `eb9adf6`. This branch does not include the main checkout's unrelated
uncommitted publication, cleanup or default-favorite changes. It is not merged.

## Container checks

- `task check`: 506 Java tests, 7 calendar pipeline tests, 60 cleanup/recovery
  tests, 2 deployment tests and Android lint passed. English/Thai resource parity
  validated 692 Homeboard resources.
- `task phone:check`: shared draft/rebase logic, Chromium/Android and WebKit/iPhone
  controller workflows and World clock regression checks passed. The existing
  preview check now waits for the asynchronous preview response before asserting.
- `task grocery:check`: exact decimal conversion, confirmed package sizes,
  threshold boundaries, whole-package/loose-increment rounding, unknown/manual
  stock, free-text preservation, per-list conflicts and independent-list merge
  passed. Real Chromium/WebKit phone checks cover quick Add/Enter, batch preview,
  Thai input, duplicate review, staple reuse, keyboard reorder, confirmed units,
  low-stock suggestions, atomic Update TV and large text. An additional open-editor
  rebase check verifies explicit conflict resolution and clearing local draft Undo
  after a committed update.
- The first new Java check exposed snapshot aliasing; the phone snapshot now
  copies the grocery book. Both validation paths reject missing product links,
  duplicate product needs, invalid units/stock, unknown schema fields on the server,
  oversized text and over-budget data without partial persistence.

Phone fixture captures are under `artifacts/phone-browser/grocery-*.png`. They
establish rendered browser behavior against controlled responses, not physical
phone-to-TV LAN reachability.

## Deployments and native evidence

Build `0.1.75` is superseded: artifact inspection caught the final phone Undo fix
missing from its packaged asset. It must not be used as final acceptance evidence.
Its frozen APK SHA-256 is
`5ba1c3815af733fa158ab7c0fc0b52fff564e1cec6e4c1789995d44345a8dff6`.
Cast1 install `J‑ef3c91a6eddd` succeeded; the identical Cast2 install
`J‑9687c19ce309` failed `personal-install-in-use` and is unconfirmed.

The `0.1.75` isolated native proof `J‑e36bb5de09d6` encountered coordinator
`plugin-error` / recovery-required before acceptance completed. Automatic recovery
`J‑b3cff08b419c` completed. This establishes no grocery UI pass. Exported evidence
belongs under `artifacts/grocery-proof/0.1.75/`.

Build `0.1.76` passed `task deploy` container checks and packaging. Its frozen
source hashes and every packaged phone asset matched the tested source. SHA-256:
`c59a50d4af7294c315224f83aa26852fca4d9991458ab2ca54f4bfcadd308bf8`.
Cast1 `J‑d08678ee5c90` succeeded; Cast2 `J‑e14a865782e9` failed
`personal-install-in-use`. Native proof `J‑a932da58285f` completed at the
coordinator, but its app assertion ran before content mounted and reported
`Expanded notes unavailable`. Its physical screenshot shows those notes, package
conversion and stock detail correctly; this is partial rendering evidence, not a
passed restoration check.

Build `0.1.77` replaces the probe's fixed mount delay with content-readiness polling,
cleans the grocery deletion label and removes a whitespace-only asset artifact.
It uses the same container build/freeze/both-device deployment workflow. Final
receipts and native results will be recorded below when available.

The debug-only `GroceryProbeActivity` uses isolated preferences and sample data,
checks the real aggregate phone Apply/retry/Undo path, real short/held OK dispatch,
nested Back, visible Restore, exact page/focus/geometry preservation and richer
product/stock detail. Run it only through the coordinator:

```sh
python3 dev/grocery-device-proof.py --version 0.1.77
```

A coordinator completion alone is not a pass: inspect its `wf.log`, version and
screenshots. No shared-device settings, default Home application or household
list is changed by the fixture.

## Remaining limits

- Real physical phone LAN pairing/connectivity and English/Thai TV rendering with
  larger text require independent device acceptance.
- The remote editor provides basic list entry/editing and list selection. Product,
  packaging and stock configuration is phone-first; expanded TV views expose the
  saved details. Category metadata is retained; store/category grouping remains
  a follow-up.
- Phase 3 purchase completion, offline shopping and remote/cross-TV sync are absent.
  Stock changes only from explicit reports. Review schedules and natural-language
  Smart Add interpretation from the Remember The Milk survey remain later work.
