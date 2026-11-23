# Repository consolidation, 23 November 2026

**PASS — 21 registered worktrees and 21 local branches reduced to one checkout
and one local branch, `main`.** Will confirmed no other session was working on
the project and required consolidation before another feature.

All 20 side-branch tips were ancestors of main. Seven side worktrees contained
local drafts or session records. Before removal, a Git bundle preserved all refs
and history; complete per-worktree archives preserved source, loose files and
ignored evidence/build outputs. Every archived regular file was hashed against
its original, each completed archive was hashed, and those hashes were checked
again immediately before removal. Branch/head/status and ancestry were rechecked
before each checkout was removed. The one locked checkout belonged to a process
that no longer existed; its stale lock was released. Only merged branches were
deleted. No remote branch or upstream ref was removed.

Private recovery root:
`/home/will/homeboard-tv/artifacts/worktree-archive/2026-11-23-cleanup/`

The archive set is approximately 1.4 GiB. `before.json`,
`verified-archives.json`, per-archive manifests and `removed.json` provide the
inventory and checksums. Main's original loose files also have a verified
snapshot. See [recovery instructions](../how-to/worktrees.md). These local
archives contain private evidence and are not published in Git.

## Main consolidation

- Existing standalone resolution-tester source, regression tests and mockups were
  committed after container verification. Four mode-selection/orientation tests,
  Android lint and debug packaging passed. Its source matches the earlier frozen
  0.1.130 source manifest; the main dashboard was not changed by this cleanup.
- Existing Trello proposal/mockups and coordinator customer/ANR records were
  retained as documentation. The Trello source/authentication choices and native
  4K dashboard remain explicitly unimplemented or unverified where stated.
- Remaining Home-button and ANR evidence was reconciled with Cast1 0.1.130 startup
  acceptance. Exact transition timing stays parked; offline installation jobs
  remain coordinator-owned work.
- Local worktree folders, deployment-tool cache, private session transcripts and
  the two existing raw coordinator-export directories are explicitly ignored.
  Their files remain available locally; already tracked acceptance images remain
  tracked. The ignore rules do not hide unfinished application source.
- Stale agent ownership markers were cleared from TODO, current worktree pointers
  retired, and all four missing plan-index rows added. No feature is assigned.

## Verification

The tester container run completed `:tester:testDebugUnitTest`,
`:tester:lintDebug` and `:tester:assembleDebug` successfully. The shared TODO
linter ran in the build container; the plan-index check reports all 49 plans
indexed. `git diff --check` passed. The earlier application startup fix already
passed all 668 Java tests and Android lint; cleanup changes no application source
outside the separately verified tester.

Completion requires clean `git status --porcelain`, exactly one registered
worktree/local branch, and main synchronized with origin after the consolidation
commits. Pending device acceptance and future feature decisions remain in their
plans; they are not grounds to retain old development checkouts.
