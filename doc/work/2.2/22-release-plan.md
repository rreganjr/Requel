# Release Plan — Requel 2.2

Branch `release/2.2`, cut from `master` after `v2.1.0` (with the #493 hotfix), version `2.2.0-dev`.
Milestone `v2.2`, board "Requel 2.2". Process: `doc/guides/RELEASE_PROCESS.md`; each ticket
follows the lifecycle in `CLAUDE.md` (review, plan, branch from `release/2.2`, verify, one PR,
retro).

## Summary

24 open issues on `v2.2` as of 2026-11-18, about 92 points (one point is roughly one working day).
The theme was set when the milestone was created: modeling and specification, meaning turning
requirements into what a team or an AI agent builds from. The planning pass on 2026-11-16/18 added
the living-document features that make that useful: traceability, baselines, approval status,
document import, search and bulk issue actions. It also added the domains epic (project starters
with branching questions, #513) and a UI review.

The order puts hardening and the UI review first, then #468, which changes the step model that
several later tickets build on. Then come reading and navigation, the living-document work,
exports and MCP, AI import, and the domains epic last.

Total: about 92 points against 48 planned for 2.1. See "Scope" below.

**Updated 2026-11-22, after #519.** The UI review filed #525–#529 (11 points), bringing the release
to about 103. The two bugs and two cleanups go in a new Phase 2b before #468; the editor shell
(#525) goes first in Phase 5, before #501 adds approval controls to every editor header.

## Order

### Phase 1 — hardening and bugs

| # | Issue | Size | Notes |
|---|---|---|---|
| 1 | #449 Management endpoint configuration | 1 | Details are in the private advisory. |
| 2 | #450 Event stream: session and subscription handling | 2 | Details are in the private advisory. |
| 3 | #495 Assistant runs fail silently on races and deletes | 3 | Testcontainers MySQL IT; check the e2e log (deadlocks equal retries). |

### Phase 2 — UI baseline

| # | Issue | Size | Notes |
|---|---|---|---|
| 4 | #519 UI review and cleanup | 3 | First, so the five new pages start from cleaned-up patterns. Needs the jar running locally with the dev profile. Decides whether a shared editor shell gets its own ticket. |

### Phase 2b — UI review follow-ups

From the #519 report (`519-ui-review.md`). Done before #468 so the bugs are fixed early and the new
pages in Phase 4 start from the finished cleanup.

| # | Issue | Size | Notes |
|---|---|---|---|
| 5 | #529 Accent contrast: white text on emerald and amber | 1 | #519 made the accent recolour buttons, which exposed it. |
| 6 | #526 Report editor: annotations fail to load | 2 | Bug: `Unknown entity type: ReportGenerator`. Decide: register it as annotatable or drop the section. |
| 7 | #528 Admin pages in cards, full-width tables | 1 | |
| 8 | #527 Definition editor layout | 2 | |

### Phase 3 — step model

| # | Issue | Size | Notes |
|---|---|---|---|
| 9 | #468 A step's actor as a role, rendered per use case | 8 | Changes the model, a migration and `project.xsd`. Unlocks #496's When/Then and #505's role column. Decide at review: migrating existing steps, roles on scenario names, a named actor not in the use case. |

### Phase 4 — reading and navigation

| # | Issue | Size | Notes |
|---|---|---|---|
| 10 | #504 Project search | 3 | Extends the gateway's `searchProjectEntities` (see the issue comment); no second search read. |
| 11 | #505 Steps list page | 2 | |
| 12 | #506 Glossary terms used | 2 | |
| 13 | #497 Traceability page | 3 | Gaps shown only on the page; raising issues from them is a later decision. |
| 14 | #507 Bulk actions on Open Issues | 3 | New `BulkResolveIssues` command. |

### Phase 5 — living document

| # | Issue | Size | Notes |
|---|---|---|---|
| 15 | #525 Shared editor shell, with a not-found state | 5 | Before #501, so approval controls go in one header instead of eight. Fixes the not-found state in every editor (#519 F11). |
| 16 | #501 Review and approval status | 5 | Migration: `permission_type` enum to VARCHAR plus `Approve`. |
| 17 | #500 Baselines, diff, restore as a new project | 8 | Requel's first scheduled job. Attribution waits on #499 (no milestone). |

### Phase 6 — exports and MCP

| # | Issue | Size | Notes |
|---|---|---|---|
| 18 | #496 Gherkin export | 2 | After #468. |
| 19 | #508 MCP prompts and resources | 2 | Check prompt and resource-template support in Spring AI 1.1.7. |
| 20 | #509 Reverse-engineer a feature's code | 3 | After #508 (adds a prompt). Trial on `modules/mcp-server`; gaps found become issues. |

### Phase 7 — AI

| # | Issue | Size | Notes |
|---|---|---|---|
| 21 | #503 Import requirements from a document | 13 | Largest item. New `EXTRACT` definition kind, draft tables, PDFBox and POI. Consider splitting at review (e.g. formats or review UI). |
| 22 | #370 Organization-level assistant definitions | 5 | Moved from 2.1. Decide: who authors, what a fork forks from, locking. |

### Phase 8 — domains (epic #513)

| # | Issue | Size | Notes |
|---|---|---|---|
| 23 | #514 Domain type, file format and loader, browse-only view | 8 | Decide at review: name uniqueness across projects and domains; whether assistants see domain entities. |
| 24 | #517 File format guide and validation | 2 | Right after #514, while the format is fresh. |
| 25 | #515 Option position applies its bundle | 5 | Provenance (#272) records domain, version and option. |
| 26 | #516 Wizard checkboxes and Add domain | 3 | |
| 27 | #518 Starter domain "Web application" | 3 | Content needs the maintainer's review. |

### Modeling research

| # | Issue | Size | Notes |
|---|---|---|---|
| 28 | #383 Research modeling languages and plan the modeling epic | 3 | Can run alongside any phase; it's research, not product code. Its comment adds the EARS/requirements-spec mapping. |

## Scope

About 103 points (92 planned, plus #519's 11) is more than twice 2.1's plan. The modeling epic that #383 produces is meant to be filed
on `v2.2` too, which would add more. Options at the first checkpoint (after Phase 4):

- keep everything and accept a longer cycle;
- file #383's epic on `v2.3` and keep 2.2 to what's here;
- move the domains epic (#513, 21 points) to `v2.3`.

## Decisions needed before the ticket starts

| Issue | Decision |
|---|---|
| #526 | Register `ReportGenerator` as annotatable, or drop the report editor's Annotations section |
| #468 | Migrating existing steps; scenario-name roles; reporting a named actor not in the use case |
| #503 | Split or not |
| #370 | Author role; fork base; locking |
| #514 | Name uniqueness across projects and domains; assistants on domain entities |
| #383 | Whether its epic lands on `v2.2` or `v2.3` |

## Not on the milestone

- #499 Audit log coverage, and whether to move to Envers. Filed with no milestone; it decides how
  #500's diffs get attribution and in-between states later.

## Release mechanics

- **Migrations:** planned work takes the next integer after `V44` on `release/2.2`. A 2.1.x
  hotfix that needs one takes `V44.1` on `master`.
- **Hotfixes during 2.2:** branch from `master`, then merge `master` forward into
  `release/2.2` with a merge commit (`RELEASE_PROCESS.md`, "Forward merges").
- **Release candidates:** `2.2.0-rc1` once every issue is closed or moved; acceptance findings
  go in one ticket per rc.
- **Promotion:** `release/2.2` merges into `master` with a merge commit at `2.2.0`.
- **Release issue:** "Release 2.2.0" tracks the checklist and carries the version-bump commits.
