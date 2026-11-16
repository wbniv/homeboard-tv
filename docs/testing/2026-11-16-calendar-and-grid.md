# Combined Calendar and visual grid selectors · 16 November 2026

Candidate **0.1.21**, version code **21**, installed on Cast1
(`chromecast-test-01`) through the shared coordinator. Existing app data and the
default Home selection were preserved.

- Base source revision: `b2f23c89814b24c31e4d90d21a0d4a2a58d90430` plus local
  Calendar/provider/settings and grid-preview changes recorded in this worktree.
- Frozen APK: `artifacts/deployments/0.1.21/homeboard.apk`.
- SHA-256: `7c5606d561b37d0e1e5ff10af296ccae68fe94e7a37a0f259348f9233eebaf9e`.
- Coordinator job: `J‑2715b06a214b`, **succeeded**, final device health **ready**.
- Installation evidence reports that exact hash as installed and verified.
- Receipt, captures and input timeline:
  `artifacts/deployments/0.1.21/evidence/` (local, ignored artifacts).
- Recipe: [calendar-and-grid-check.json](../../dev/device/calendar-and-grid-check.json).

## Container checks

394 JVM tests, zero failures/errors, including 14 Calendar tests. Six Python
pipeline tests passed. Android lint and debug APK packaging passed. English/Thai
resource parity passed for 379 resources. The build image now contains Python
for offline JSON normalization/assembly only; the APK remains native Java.

## Screenshots inspected independently of job execution

| Capture | Finding |
|---|---|
| `input-frame-001.png` | One Calendar widget, event-only dates, grouped flower/Buddha icons and country-color edges. |
| `input-frame-002.png` | Holiday lookahead changed to 7; flower/week dates stayed in their independent two-week range. US/December holidays disappeared from the shorter holiday window. |
| `input-frame-003.png` | Switching back to 30 restores the US/December holiday dates. |
| `input-frame-004.png` | Custom numeric editor opens with the current value selected and Save/Cancel controls. |
| `input-frame-005.png` | Back cancels the custom edit; the widget still shows 30 days. |
| `input-frame-006.png` | Monday is visibly fixed, with an explanation of upcoming-only Monday–Sunday windows. |
| `input-frame-008.png` | 17 November detail includes the selling block, preparation block, approximate break and separate Wan Phra event with source references. |
| `input-frame-010.png` | Grid columns shows Auto resolved to 12×3 and a neighboring four-column preview. |
| `input-frame-011.png` | Moving right selects a visibly different four-column schematic; the opposite dimension stays at three rows. |
| `input-frame-012.png` | Grid rows shows the saved three-row candidate and neighboring two-row schematic, emphasizing horizontal groups. |
| `input-frame-013.png` | Right remains at the maximum three-row value, consistent with the displayed End boundary. |
| `input-frame-014.png` | Back cancellation preserves the grid settings and the final Calendar remains at 30 days. |

The recipe opened Calendar settings and returned through the same-window menus.
No dates were added for empty days. Existing Home items remained on their saved
pages; adding Calendar placed it on Home page 2. The temporary Undo banner covers
part of the lower widget during the save confirmation and clears afterward.

## Earlier attempts

APK 0.1.20 (`a668eaa21391b3bb9dc05ad5a134ec8483794748a3a928973bb39f2d6347d588`)
was installed and hash-verified by `J‑528f92e0279c`, but the initial foreground
launch check returned `target-left-foreground`. Retry `J‑51c92c318ce3` navigated
to the source-link screen and the later long OK action opened an external
handler, stopping the foreground guard. The coordinator performed automatic
recovery/readd. Neither run establishes Calendar visual acceptance.

A shorter navigation probe, `J‑81519268f5d9`, completed and showed each expected
menu followed by the actual combined Calendar. Version 0.1.21 includes the grid
selector changes and uses a fresh deployment number; its complete check above
passed. The original deploy Task's watch process terminated after submission;
a fresh coordinator watch and evidence export confirmed completion, rather
than assuming that process exit meant the device job failed.

## Remaining limits

- Custom arbitrary-value parsing, persistence, invalid input and Undo restoration
  are covered by unit tests. The TV recipe verified editor opening/cancellation;
  it did not enter an arbitrary number using the software keyboard.
- UI language here was English. Thai resource parity passed; independent Thai
  linguistic review and device typography review remain pending.
- Multi-country grouping and deterministic country ordering are unit-tested,
  including all four countries on New Year. This run did not visually inspect
  three/four-country cards, larger fonts or every resize/page configuration.
- The six provider files are reviewed **offline dates**, with coverage/status
  shown. Live twice-daily emergency closure ingestion and the validated on-device
  Buddhist calendar algorithm remain pending; this is not live-update evidence.
