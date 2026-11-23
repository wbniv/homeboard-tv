# Worktree lifecycle and recovery

Keep one clean `main` checkout for serial work. An extra worktree is for a specific
unfinished task that needs isolation. After its source is merged, record remaining
acceptance in the plan/TODO, preserve any useful local files and evidence, then
remove the worktree and its merged branch. An old checkout is not a task tracker.

Before starting another feature, check `git status --short`, `git worktree list`
and `git branch -vv`. Finish or explicitly preserve loose changes first. Clear
stale TODO ownership markers when a session ends; implementation and verification
status must remain separate.

## 23 November 2026 consolidation

All 20 side-branch tips were already ancestors of main. Seven side worktrees still
contained local changes: favorite app sizes, Android widget census, favorites
ring, dream rename, screensaver stage 1, timer stage 1 and world clock. Those are
archived drafts or session records, not 20 unmerged features. Some old favorite
size draft classes have different names from the current implementation; their
absence from main does not justify merging an obsolete implementation blindly.

The private recovery directory is:

`/home/will/homeboard-tv/artifacts/worktree-archive/2026-11-23-cleanup/`

It contains `history.bundle` with the original refs/history, `before.json` with
the original branch/head/status inventory, and `verified-archives.json` with
archive hashes. Each side branch has a directory whose name replaces `/` with
`__`. Its complete `checkout.tar.gz` includes tracked files, loose files and
ignored device evidence/build outputs. Its manifest verifies archived file
bytes against the originals. `changes.patch`, `staged.patch` and `metadata.json`
retain the original Git state. `main/working-files.tar.gz` preserves main's loose
files before consolidation. `removed.json` records completed removals.

These archives are local recovery data and may contain private device/account
evidence. Keep them out of Git. Existing frozen APKs and current device evidence
remain in their original locations. The Git bundle preserves the original
history even after local side branches are deleted.

## Recover an old draft

Read its `metadata.json` for the original commit. Create a recovery branch at
that commit, then unpack the archive while excluding its obsolete `.git` pointer:

```sh
git worktree add -b recovery/favorite-sizes /tmp/homeboard-recovery 290e675de1e2f0bd309a0d3b54a24b1588670716
tar -xzf artifacts/worktree-archive/2026-11-23-cleanup/feature__home-favorite-app-sizes/checkout.tar.gz --strip-components=1 --exclude=checkout/.git -C /tmp/homeboard-recovery
git -C /tmp/homeboard-recovery status --short
```

This restores the old working files at their original base. Review and port only
the parts still needed. Do not overlay an old checkout on today's main. If the
original refs are needed elsewhere, first inspect `git bundle list-heads` on
`history.bundle` and fetch the specific ref into a named recovery branch.

## Current acceptance remains in project records

The [startup fix](../plans/2026-11-23-air-quality-startup.md) passed Cast1
acceptance as 0.1.130. Offline fleet installations remain coordinator jobs, not
reasons to retain development worktrees. The [4K investigation](../plans/2026-11-23-native-4k-dashboard.md)
and [Trello proposal](../plans/2026-11-23-trello-board-widget.md) retain their
explicit open gates. Consolidation adds no dashboard feature and does not turn
an unverified plan into accepted behavior.
