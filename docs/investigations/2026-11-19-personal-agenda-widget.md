# Personal agenda: widget implementation proposal

**Purpose:** Show the next real appointment and a useful view of the household day alongside holidays.

Larger integration: extend the shared Calendar repository; offer a separate agenda presentation. This is a separate implementation proposal, stored under
`docs/investigations/` as requested. All mockup values are illustrative. The proposed first release and follow-on
work are separated below.

[All seven proposals](2026-11-19-widget-proposals/index.html) ·
[Desktop preview](2026-11-19-widget-proposals/personal-agenda.html) ·
[Phone preview](2026-11-19-widget-proposals/personal-agenda.html?view=phone)

## Goals and first release

Read-only personal-event subscriptions and an Agenda presentation. Reuse holiday/local-observance services and the shared full-screen shell. Do not replace the existing Calendar widget or create a second competing event store.

## Mockups and size behavior

![Personal agenda: 1×1, recommended, wide, full-screen and phone mockups](2026-11-19-widget-proposals/personal-agenda-mockups.svg)

| Presentation | Content and interaction |
|---|---|
| 1×1 | One headline: **Next · 15:30**. A readable glance, never tiny body text or hidden controls. Short OK opens detail. |
| 2×1 / shallow | Headline plus one next item or context label; truncate only presentation. |
| Recommended 4×2 | Headline, the most useful few records/values and saved/source status. |
| Wide / tall | Wide adds comparison/trend or columns; tall adds records. Keep remote navigation predictable. |
| Fill Home | More visible records and context, with full-screen detail still available by short OK. |
| Full screen | Today · appointments and all-day events; additional records, selected-item detail and meaningful actions, not a magnified card. |

Add through the widget picker. Do not rearrange the default dashboard automatically.
Each instance keeps its own size, placement and configuration; shared records are
not duplicated per instance. Overflow uses directional wedges, never visible
scrollbars on TV. Minimum-size feasibility must be checked against the actual grid
and text scaling; do not claim 1×1 usability until demonstrated on-device.

<details>
<summary>Phone controls at a phone viewport</summary>

<img src="2026-11-19-widget-proposals/personal-agenda-phone.png" width="320" alt="Phone configuration mockup for personal agenda">

</details>

## Workflow and editing

![Personal agenda workflow and failure-retention branch](2026-11-19-widget-proposals/personal-agenda-workflow.svg)

Short OK/click opens the shared full-screen shell. A visible **Restore dashboard**
and short Back restore the originating Home page, focused widget, position and saved
size; nested detail closes first. Hold OK opens Configure / Move / Resize actions.
Do not overload short OK with a destructive or state-changing action.

The existing approved phone editor is the primary setup/edit surface. Its controls
are **Calendar source · HTTPS ICS; Display name and visibility; Calendars / time zone**. Review changes before **Update TV**. Cancel leaves
the TV untouched. Reuse stable widget IDs, generation/conflict checks, operation
receipts and composite Undo. Keep credentials out of ordinary draft exports.
Remote-accessible controls remain available for routine viewing and explicit actions.

## Data, persistence and integration

Start with an explicit HTTPS iCalendar subscription or local import. Source URLs may contain secrets: store privately, mask in UI and omit from logs/screenshots. A private calendar requires a supported authenticated path; Google/Microsoft OAuth, write-back and invitations are separate adapter work.

Source ID plus UID and recurrence-instance identity, source revision, title, start/end with DATE versus DATE-TIME distinction, original time zone and optional location. Preserve cancelled occurrences and recurrence exceptions. Cache a defined past/future window, not an unbounded series expansion.

Use a native Java repository/presentation with small adapters. Reuse existing
location, formatting, cache and expansion code where the data contract matches;
do not introduce a dashboard framework or required household server for local widgets.
Persist accepted changes atomically. Last-good external data remains visible with
source/valid-time and retrieval status; missing data is never a numeric zero or a
claim that there is nothing scheduled.

## Domain rules and failure states

Use the event source zone for recurrence and the chosen display zone for presentation. Floating times require an explicit source-zone choice. Treat all-day end dates as exclusive. A timeout does not mean an empty calendar. Screen privacy offers title-only, busy-only and explicit visibility; hide descriptions and attendee details by default.

Provide distinct first-use, empty, loading, offline/stale, partial and error states
where applicable. Local widgets distinguish an unsaved draft from a saved empty
list. In full screen, explain recovery and offer a specific retry/edit action.
Use English/Thai, BE date years for Thai, named time zones, existing time/duration
formatting and selective timestamp/age icons. Do not add clock icons to clock faces.
Support large text, Reduced motion, focus outlines and screen-reader labels.

## Implementation sequence

1. Spike representative ICS feeds and select a maintained parser only after container compatibility and license review.
2. Implement recurrence-window normalization and last-good per-source cache behind the shared Calendar data layer.
3. Add source setup, preview and privacy controls to phone settings, then agenda glance/week/detail.
4. Verify cancellation, exceptions, zones and offline operation before authenticated cloud adapters.

## Acceptance and screenshot proof

- Import duplicate UID occurrences without duplication; apply moved, cancelled and revised occurrences correctly.
- Test all-day exclusive end, floating times, DST, overlapping events and Thai BE display without changing dates.
- Bound recurrence/response size; malformed or empty replacement must not erase valid cached appointments.
- Container tests cover the data model, persistence and domain edge cases; UI tests
  exercise actual controls and request failure/retry behavior, not only rendering.
- Capture and visually inspect 1×1, recommended 4×2, wide, tall, Fill Home and
  full-screen presentations. Record English/Thai and large-text cases with sample
  values labeled as fixtures when applicable.
- Capture phone overview/edit/review and one error/conflict state in Chromium and
  WebKit. Verify no horizontal page overflow and that long content remains reachable.
- On Cast1, prove short OK expansion, nested Back, visible Restore, original
  page/focus/size, and Undo after saved changes. Preserve household settings using
  isolated test preferences for proof captures.
- Store APK/source hashes, coordinator job/receipt, screenshots and native assertions
  in `docs/testing/`. Mockups are not device evidence. Every working APK uses a new
  version and is submitted to Cast1/Cast2 via `task deploy`; foreground verification
  remains separately subject to coordinator admission.

## Decisions and release gates

The recommendation above is a proposed default, not an instruction to replace an
existing layout. Confirm real-world use with a small prototype before expanding
scope. Network integrations require a verified actual source and a permission,
coverage, attribution and quota review. Choose the first-release boundary before coding; later integration work should
follow the domain-specific sequence above rather than expanding the initial widget.

## Evidence and source notes

- [iCalendar RFC 5545](https://www.rfc-editor.org/rfc/rfc5545.html) — UID and RECURRENCE-ID distinguish recurrence instances; DATE, DATE-TIME, time zones and recurrence exceptions require explicit handling. Accessed 19 November 2026.

Repository design contracts: [full-screen widgets](../plans/2026-11-17-widget-full-screen.md),
[phone editor](../how-to/phone-widget-editor.md), [Undo](../how-to/undo.md) and
[local date/time work](../plans/2026-11-18-thai-years-and-time-presentation.md).
External capabilities above are source assertions; this proposal's UI, sizes,
refresh/error policy and implementation sequence are design decisions. Actual
provider access and physical acceptance are not inferred from API documentation.
