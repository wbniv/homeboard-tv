# Translation procedure and commit/deployment gates

**Design contract · 16 November 2026.** This procedure accompanies the
[language-setting plan](../plans/2026-11-16-language-setting-and-translations.md).
The structural `task i18n:check` gate is implemented in the combined launcher. The staged-index, review-ledger and artifact-acceptance enforcement
described here remain **planned**; see the implementation status at the end.

## Reusable contract

Treat translation as part of each UI feature. Source copy, all supported
translations, consuming UI and review records travel together. A translation is
complete only when its meaning is reviewed and the reachable interface renders
correctly. The workflow applies across projects; file formats and tests stay native:

| Concern | Homeboard TV | Parking Space equivalent |
|---|---|---|
| Catalog source | Android default English XML and `values-th` | English/Thai ARB |
| Typed access | Android `R`, indexed format arguments, quantity resources | Generated `gen_l10n` accessors and ICU |
| App locale | One Android language owner | One Flutter locale-state owner |
| Build/check adapter | Task → build container → checker, lint, Gradle | Pinned Flutter environment → catalog/generation checks and tests |
| Render evidence | Coordinator-owned TV candidate and container emulator | Representative web/mobile candidate, roles and routes |

Do not create a shared runtime, merge project catalogs or translate at runtime.
Initially Homeboard supports English (`en`) and Thai (`th`); additional languages
must pass the same inventory, copy and rendering requirements before advertising
them in the picker or Android locale configuration.

## 1. Inventory and specify the change

- [ ] Identify all affected screens and states: normal, empty, loading, stale,
  offline, error, validation, confirmation, Undo and accessibility labels.
- [ ] Classify text as app-owned copy, technical identifier, brand/endonym,
  user-entered content or attributed external content. Only explicit, narrow
  exemptions may bypass translation checks.
- [ ] Give each complete message a stable semantic key and translator context:
  screenshot/surface, intended meaning, placeholder types/example values and
  available space. Add glossary entries for recurring terms.
- [ ] Update screen mockups and the flow diagram when changing navigation.

Keep source and translations in one feature change. Prefer complete messages
over concatenation; translate action verbs and outcome descriptions consistently.
Action IDs, enum values, widget/station IDs and protocol fields never depend on
translated text. Do not use English string comparisons to dispatch actions.

## 2. Author and translate

- [ ] Add or edit the English source and every supported translation together.
- [ ] Preserve indexed placeholder identity and type; allow natural word order.
  Use native plural resources/ICU categories rather than hand-written suffixes.
- [ ] Provide localized app-owned error fallbacks and keep raw diagnostics in
  logs. Do not expose an untranslated exception inside a translated sentence.
- [ ] Use the effective UI locale for display; retain invariant serialization,
  cache keys, parsers, coordinate/wire values and stored timestamps.
- [ ] Preserve user text and original provider names unless a reviewed localized
  name exists. Distinguish missing external translations from missing app copy.
- [ ] Keep language endonyms recognizable and stable; do not use flags.

For Homeboard, use English default resources, Thai resources, 24-hour time and
the formatting/calendar decisions in the plan. Context comments belong beside
the source resource. Do not translate technical symbols such as PM2.5 or version
codes, or falsely label measured station averages as model forecasts.

## 3. Validate automatically

Implement one read-only `task i18n:check` inside the build container. Pin any
checker dependencies in that image. Do not install a host SDK or run Gradle on
the host. The task writes a deterministic structured report of:

- Catalog parse errors, duplicate/empty keys and missing required translations.
- Default-resource completeness, aliases, arrays and required plural categories.
- Typed/indexed argument compatibility, escaping and literal percent handling.
- Reachable UI coverage and new hardcoded app-owned text, including Java sinks,
  XML, accessibility, toasts, validation and persisted presentation strings.
- Supported-locale agreement across policy, picker, manifest and packaged resources.
- Changed-source/translation/context fingerprints and missing/stale review records.
- Applied exemptions with exact key/path, reason and scope; newly reachable keys
  lose an unreachable-resource exemption.

Use Android lint plus a source-aware detector for Java UI. A regex is a useful
audit aid, not sufficient proof of extraction. Avoid ignoring all Java literals
or globally disabling MissingTranslation to silence inherited languages. Test
the checker with deliberate failures, not just a clean catalog. Distinguish
structural validation from copy approval in the report.

Android compiles resources during the build; do not hand-edit generated `R`
output. In projects that track generated translation sources, regenerate with
the pinned toolchain and fail on drift, or deliberately stop tracking that output.

## 4. Review meaning and rendering

Maintain proposed `docs/translations/reviews.json`, keyed by message and target
locale, with fingerprints for source text, placeholder schema, translator context
and target text, plus reviewer, date and outcome. Changes in any of those inputs
invalidate that review. Reviews must cover the copy actually staged or built.
Renaming a key carries a review only when the content and context are unchanged
and the transfer is explicit. Deletions remove obsolete review records.

Record English copy review and competent Thai linguistic review for changed
messages. Machine/AI assistance may draft translations and flag inconsistencies;
it does not create a fictitious human signoff. Record pending reviews honestly.
Unchanged fingerprints reuse existing reviews, so every deployment does not ask
for fresh approval of the entire catalog.

Inspect representative screens with real layout constraints and longest useful
values: Thai shaping/diacritics, wrapping, label height, truncation, larger text,
contrast, focus and accessibility. Verify navigation, persistence, cancel/Back,
Undo and recovery after recreation. Keep translation review and device rendering
review distinct; one cannot substitute for the other.

## 5. Commit and CI gates

| Boundary | Required check | Failure behavior |
|---|---|---|
| Translation-affecting commit | Full relevant catalog/source validation and fresh copy reviews against staged contents | Refuse the checked commit; unstaged translations cannot satisfy it |
| Unrelated docs-only commit | Documentation/link/index checks | Do not require Android builds or new translation reviews |
| Required CI | Whole-tree translation validation, tests/lint, build and release-resource checks | Fail independently of local hooks |
| Checker/policy changes | Gate negative fixtures and enforcement tests | Reject a weakened/broken gate |

Create an isolated snapshot of the **index**, including staged additions/deletions,
for commit checks. Run the same container task there; never silently stage files
or mutate source/review records to make it pass. Ensure partially staged files
are handled correctly. Report actionable resource keys/paths and reasons.

Integrate a repository adapter with the existing shared hook dispatcher when
supported. Do not overwrite `/home/will/python-tui-lib/git-hooks` or global
`core.hooksPath` as part of feature work. If no dispatcher exists, ship an explicit
staged-check/commit task and document that automatic pre-commit enforcement is
pending; required CI remains authoritative. Translation-affecting paths include
UI consumers and context/policy files, not only catalog XML.

Update the current CI's direct Gradle command as well as `task check`, or attach
the checker to required Gradle verification tasks, so neither path silently
skips it. CI must not rely on an earlier developer's local report. Review
requirements are content-based; unchanged copy uses the existing valid ledger.

## 6. Deploy a candidate, then accept the artifact

Separate checks that can run before an install from checks requiring the installed
candidate. This avoids an impossible requirement for screenshots of an APK that
has not yet been installed.

1. Merge intended features and select one deployment owner. Inventory the actual
   combined tree. Run whole-candidate automated and copy checks before spending a
   version number. Capture exact revision plus any working-tree patch/content hash.
2. Use `task deploy` to bump, check and build in the container, freeze the APK and
   run the shared coordinator workflow. Never reuse a number from a failed
   attempt. Before submission check devices/queue; use Cast1 until Will declares
   Homeboard usable. Retain app data/settings and the device's default Home app.
3. Freeze version, APK SHA-256, source digest, catalog/context/review hashes,
   checker/tool versions and results in the deployment record. A mismatch fails
   before installation; changes after freeze require a new deployment.
4. Submit complete reviewed language workflow(s); watch to terminal states and
   export evidence. Let the coordinator admit background installs or defer
   foreground verification during personal use. Service downtime has no direct
   device fallback. Additional targets of this deployment use the same frozen APK.
5. Inspect English and Thai rendering, switching, restart/persistence, focus,
   Back/Undo, errors and settings preservation. Test OS-locale variants through
   supported coordinator profiles or a container emulator without changing shared
   system settings ad hoc. Use locale-aware assertions; ASCII-only English OCR
   does not prove Thai rendering.
6. Record installation, launch, linguistic, visual and functional outcomes
   separately. Link screenshots/jobs and acceptance to the frozen artifact.
   Deferred/failed required checks block promotion/broader rollout, while the
   installed test candidate remains explicitly unaccepted. Fix and deploy a
   newly numbered candidate; do not erase an earlier failure record.

Keep `task check`, `task build` and `task bundle` as container verification entry
points. Proposed extensions add these gates; current commands do not yet prove
translation completeness. See [build instructions](build.md) and
[coordinator/deployment instructions](chromecast.md).

## Evidence and completion

A translation feature is done when the reviewed message inventory covers the
shipped UI, all copy fingerprints are current, the commit/CI/deploy failure paths
are demonstrated, and bilingual acceptance is recorded for the exact APK.
Passing catalog parity, a successful build, a completed coordinator job or an
About version number alone is insufficient. Keep the plan's remaining checkboxes
open for any unverified state.

## Implementation status — 16 November 2026

The Thai-language implementation adds `task i18n:check`, backed by Gradle's
`checkTranslations` in the container. It checks the paired `homeboard*.xml`
resources for duplicate/empty entries, key parity, format arguments and array
lengths. Test/lint/APK/bundle verification tasks depend on it, so the existing CI
and `task deploy` check/build steps inherit that structural gate.

Source-aware lint, staged-index checks, copy-review fingerprints and bilingual
artifact acceptance enforcement remain planned. Do not report the structural
gate as proof of those checks or of competent Thai review.
