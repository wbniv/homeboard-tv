# Tasks and chores: widget implementation proposal

**Purpose:** Answer what needs doing today, who is doing it, and what was already completed.

Second local-data widget; share editing infrastructure with Notes, not their records. This is a separate implementation proposal, stored under
`docs/investigations/` as requested. All mockup values are illustrative. The proposed first release and follow-on
work are separated below.

[All seven proposals](2026-11-19-widget-proposals/index.html) ·
[Desktop preview](2026-11-19-widget-proposals/tasks-and-chores.html) ·
[Phone preview](2026-11-19-widget-proposals/tasks-and-chores.html?view=phone)

## Goals and first release

One-off and recurring household tasks, optional assignee labels, explicit Complete and Undo, and completion history. Keep shopping needs and stock in Groceries; do not copy the grocery data model into chores.

## Mockups and size behavior

![Tasks and chores: 1×1, recommended, wide, full-screen and phone mockups](2026-11-19-widget-proposals/tasks-and-chores-mockups.svg)

| Presentation | Content and interaction |
|---|---|
| 1×1 | One headline: **3 due today**. A readable glance, never tiny body text or hidden controls. Short OK opens detail. |
| 2×1 / shallow | Headline plus one next item or context label; truncate only presentation. |
| Recommended 3×2 | Headline, the most useful few records/values and saved/source status. |
| Wide / tall | Wide adds comparison/trend or columns; tall adds records. Keep remote navigation predictable. |
| Fill Home | More visible records and context, with full-screen detail still available by short OK. |
| Full screen | Today · 3 tasks · 1 completed; additional records, selected-item detail and meaningful actions, not a magnified card. |

Add through the widget picker. Do not rearrange the default dashboard automatically.
Each instance keeps its own size, placement and configuration; shared records are
not duplicated per instance. Overflow uses directional wedges, never visible
scrollbars on TV. Minimum-size feasibility must be checked against the actual grid
and text scaling; do not claim 1×1 usability until demonstrated on-device.

<details>
<summary>Phone controls at a phone viewport</summary>

<img src="2026-11-19-widget-proposals/tasks-and-chores-phone.png" width="320" alt="Phone configuration mockup for tasks and chores">

</details>

## Workflow and editing

![Tasks and chores workflow and failure-retention branch](2026-11-19-widget-proposals/tasks-and-chores-workflow.svg)

Short OK/click opens the shared full-screen shell. A visible **Restore dashboard**
and short Back restore the originating Home page, focused widget, position and saved
size; nested detail closes first. Hold OK opens Configure / Move / Resize actions.
Do not overload short OK with a destructive or state-changing action.

The existing approved phone editor is the primary setup/edit surface. Its controls
are **Task and household label; Due date / repeat rule; Assigned to · optional**. Review changes before **Update TV**. Cancel leaves
the TV untouched. Reuse stable widget IDs, generation/conflict checks, operation
receipts and composite Undo. Keep credentials out of ordinary draft exports.
Remote-accessible controls remain available for routine viewing and explicit actions.

## Data, persistence and integration

Local TV-owned repository and existing paired phone editor. Household labels are names, not authenticated accounts or proof of who completed a chore. No push delivery or across-TV synchronization in the first release.

Task template UUID plus immutable occurrence identity, due civil date/time and zone, assignee label and completion events. Completion and reversal are events, not deletion. Persist a rule for calendar-based recurrence separately from after-completion recurrence.

Use a native Java repository/presentation with small adapters. Reuse existing
location, formatting, cache and expansion code where the data contract matches;
do not introduce a dashboard framework or required household server for local widgets.
Persist accepted changes atomically. Last-good external data remains visible with
source/valid-time and retrieval status; missing data is never a numeric zero or a
claim that there is nothing scheduled.

## Domain rules and failure states

Every Monday means calendar recurrence; seven days after completion means completion-relative recurrence. Show the next due date before saving. Decide explicitly whether a missed recurring task stays one overdue occurrence or creates multiple; recommend one overdue occurrence with history of missed periods. Editing a series must offer this occurrence versus future occurrences.

Provide distinct first-use, empty, loading, offline/stale, partial and error states
where applicable. Local widgets distinguish an unsaved draft from a saved empty
list. In full screen, explain recovery and offer a specific retry/edit action.
Use English/Thai, BE date years for Thai, named time zones, existing time/duration
formatting and selective timestamp/age icons. Do not add clock icons to clock faces.
Support large text, Reduced motion, focus outlines and screen-reader labels.

## Implementation sequence

1. Define occurrence IDs, repeat previews and completion/Undo semantics with pure date tests.
2. Implement phone task editor and TV due/upcoming views with no implicit completion on short OK.
3. Add full-screen task detail, explicit Complete, assignee filter and completed history.
4. Verify recurrence, missed dates and restart recovery before considering reminders.

## Acceptance and screenshot proof

- Complete twice or retry a lost response without duplicating completion or advancing twice.
- Test calendar repeats, completion-relative repeats, leap day, DST, missed periods and Undo.
- Archive a task without erasing its history; short OK expands and does not accidentally complete it.
- Container tests cover the data model, persistence and domain edge cases; UI tests
  exercise actual controls and request failure/retry behavior, not only rendering.
- Capture and visually inspect 1×1, recommended 3×2, wide, tall, Fill Home and
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

- [Recurrence reference](https://www.rfc-editor.org/rfc/rfc5545.html) — Reference for recurrence concepts; adopting selected task semantics does not imply implementing all VTODO or RRULE features. Accessed 19 November 2026.

Repository design contracts: [full-screen widgets](../plans/2026-11-17-widget-full-screen.md),
[phone editor](../how-to/phone-widget-editor.md), [Undo](../how-to/undo.md) and
[local date/time work](../plans/2026-11-18-thai-years-and-time-presentation.md).
External capabilities above are source assertions; this proposal's UI, sizes,
refresh/error policy and implementation sequence are design decisions. Actual
provider access and physical acceptance are not inferred from API documentation.
