# MCP Command Gateway & Bridges — Project Authoring + Issue Tracker → Goal Generation

> The tracker→goals workflow is **source-agnostic**; Jira is used throughout as the example,
> but GitHub Issues, Linear, or any tracker works the same way. Requel never talks to the
> tracker — the client reads the issue via its own connector and hands Requel discrete
> requirement statements plus a generic source descriptor (`sourceSystem`/`sourceRef`/`sourceUrl`).

> Draft design doc for a set of related issues. Builds on `doc/assistant-spi-plan.md`,
> `doc/ai-assistance-plan.md`, and the existing `mcp-server` module.

## Goal

Give external clients — AI (Claude Desktop, Claude Code, Codex CLI, Cowork connector) and
plain scripts — a controlled, audited way to do **anything a project user can do** in Requel
through a single command gateway: create and edit projects and every project entity, and
manage the associations between them. The motivating end-to-end workflow is reading a Jira
ticket, extracting its acceptance criteria, and turning them into goals on a project — but that
is one consumer of a general capability, not the whole feature.

Boundary: **no user/identity management.** The gateway must not create or mutate system users,
roles, or any user-coupled stakeholder. It may create/edit *non-user* stakeholders (e.g. a
standards body or customer organization referenced by a ticket), and it relies on the project's
existing auto-creation of the creator's user-stakeholder at project creation. See the policy
section.

Every mutation goes through the existing CQRS command dispatch
(`POST /api/commands/{commandType}` → `AuthorizingCommandHandler` → command → repository), so
authorization, validation, audit, optimistic locking, and SSE behave exactly as they do for
the Angular UI. The gateway adds reach, not a new write path.

## Decisions captured

- **Scope: full project authoring.** Create/edit projects and all project entities, plus
  associations (goal/actor/story into story/use-case/actor containers, scenario↔use-case, goal
  relations). Non-user stakeholders included; user-coupled stakeholder management excluded.
- **Gateway is its own abstraction**, more abstract than `mcp-server`. A pure `gateway-api`
  module defines the gateway; `requel-cli`, `mcp-server`, and `mcp-bridge` are all front-ends
  that consume it.
- **Three front-ends:** local stdio MCP bridge, remote MCP connector, and a CLI. The CLI is
  independently useful for scripting/bulk authoring, not just AI.
- **Write posture:** direct create + provenance **note** for the Jira→goals workflow.
- **AC mapping:** one goal per acceptance criterion, flat. Grouping/relating deferred to a later
  assistant pass.
- **Reconciliation split out.** Id-based update via lookup ships first (EditGoal is not a
  name-upsert); smart similarity matching is a
  separate ticket. Our Jira ACs are free-text (no stable per-item id), so v1 tolerates
  staleness on edited ACs.
- **Ticket #1 includes the stdio bridge** so the capability is demonstrable end-to-end; CLI,
  remote connector, Jira-workflow conventions, and smart matching are follow-ons.

## Why the expansion does NOT blow up backend scope

The command layer already exposes everything the expanded scope needs. The gateway dispatches
existing command types by name; it adds no domain or command code.

Entity create/edit (existing `Edit*` commands, create-or-update by id): `EditProject`,
`EditGoal`, `EditStory`, `EditActor`, `EditUseCase`, `EditScenario`, `EditScenarioStep`,
`EditGlossaryTerm`, `EditReportGenerator`, `EditNonUserStakeholder`.

Associations — container interfaces are polymorphic, so one command covers many "add X to Y":

```
Story    : GoalContainer, ActorContainer                        (goals, actors → story)
UseCase  : GoalContainer, ActorContainer, StoryContainer        (goals, actors, stories → use-case)
Actor    : GoalContainer                                        (goals → actor)
Stakeholder : GoalContainer                                     (goals → stakeholder)
ProjectOrDomain : GoalContainer, StoryContainer, ActorContainer (entities → project)
```

So the requested associations map onto existing commands with no new code:

- `AddGoalToGoalContainer` / `RemoveGoalFromGoalContainer` — goal → story, use-case, actor,
  stakeholder, or project (the container is just an `EntityRef`).
- `AddActorToActorContainer` / `RemoveActorFromActorContainer` — actor → story, use-case, project.
- `AddStoryToStoryContainer` / `RemoveStoryFromStoryContainer` — story → use-case, project.
- `AddScenarioToUseCase` / `RemoveScenarioFromUseCase` / `SetPrimaryScenarioOnUseCase`.
- `EditGoalRelation` (goal↔goal relations); `ConvertStepToScenario`; `Copy*` / `Delete*`.

Net: entity + association coverage is **MCP tool-surface and policy work**, not backend work.
The genuinely scope-growing part is reconciliation, which gets its own ticket.

### Resolved backend facts

- **Project creation auth.** `EditProjectCommandImpl.getAuthorizationRequirement()` returns
  `null` when `project == null` (create) and defers to a role-permission check inside
  `execute()`. Creation also auto-persists a `UserStakeholder` for the *current authenticated
  user* (not a new login). So the gateway's integration user just needs the project-creation
  role permission; nothing new to build, and it stays within the no-new-users boundary.
- **Stakeholders vs. users.** A stakeholder is a project association, distinct from a system
  user. `EditUserStakeholder` (`RequiresStakeholderPermission(Stakeholder.class, "Edit")`)
  associates/provisions a user and is excluded because mapping external (e.g. Jira) users to
  Requel users — and handling non-existent users — is out of scope. `EditNonUserStakeholder`
  has no such coupling and is included.

## Architecture

```
   Jira ticket ──► AI client (reads Jira via its own connector, extracts ACs)
   (or a human    │
    at the CLI)   │   MCP (stdio)        MCP (remote/HTTPS)         CLI
                  ▼        ▼                    ▼                    ▼
              mcp-bridge   │               mcp-server            requel-cli
                  └────────┴───────────────────┴───────────────────┘
                                       │ all depend on
                                       ▼
        ┌──────────────────────────────────────────────────────────────────┐
        │  gateway-api  (pure: interfaces + policy + value types)            │
        │   ├─ CommandGateway   (write dispatch by command type)            │
        │   ├─ QueryGateway     (reads)                                     │
        │   └─ command allow/deny policy + input validation                │
        └───────────────┬───────────────────────────────┬───────────────────┘
            in-process   │ (impl in service-impl)        │ REST-backed impl
                         ▼                               ▼  (client lib)
        ApiCommandFactory + CommandHandler      POST /api/commands/*  + GET /api/*
                         │                               │
                         └───────────────┬───────────────┘
                                         ▼
                  AuthorizingCommandHandler → Edit*/Add*/Remove* commands
                  → repository → audit + SSE  (+ assistant analysis dispatch)
```

Modules:

- **`gateway-api`** — pure abstraction: `CommandGateway`, `QueryGateway`, allow/deny policy,
  command/input value types. Depends only on `service-api`. No Spring MVC, no JPA, no provider
  SDKs.
- **In-process gateway impl** — lives in `service-impl` (needs `ApiCommandFactory` +
  `CommandHandler`). Used when the front-end runs in the same JVM as `requel-app` (the remote
  connector).
- **REST-backed gateway impl** — a small client lib used by out-of-process front-ends
  (`requel-cli`, `mcp-bridge`); calls `/api/commands/*` and `/api/*` with a JWT.
- **`mcp-server`** — existing in-process MCP endpoint; refactored so its tools call
  `gateway-api` (subsuming today's `ProjectQueryGateway`). Remote connector front-end.
- **`mcp-bridge`** — new: standalone stdio MCP server; REST-backed gateway. Local front-end.
- **`requel-cli`** — new: command-line front-end over the same gateway; does not speak MCP.

Front-ends are deliberately thin; the gateway core (dispatch + policy + auth exchange) is the
real work, so adding a front-end is cheap.

## Authentication & identity

- Writes execute as the **authenticated user carried by the JWT** — the real person operating
  the client — consistent with the existing API and with `McpCallAudit`, which already records
  `triggeringUserId` (resolved via `CurrentUserResolver.resolve()`) alongside the assistant
  `assistantUserId` and `runId`. There is no shared service account; the JWT identifies the
  user, who is the owning/stakeholder identity for created entities.
- Out-of-process front-ends send `Authorization: Bearer <jwt>` on every call (login→JWT today;
  see future work). The remote connector uses OAuth resolving to the same user.
- Authorization for every write is that user's, enforced by `AuthorizingCommandHandler`
  (e.g. `EditGoal` → `RequiresStakeholderPermission(Goal.class, "Edit")`). The gateway can
  never cause a write the user could not perform in the UI — the core safety property behind
  "anything a project user can do."
- **Audit attribution.** A per-client external pseudo-user (`claude-desktop`, `codex-cli`,
  `requel-cli`, …) is carried alongside the triggering user (per the SPI MCP-identity decision
  and the existing `McpCallAudit` shape) for attribution and per-client rate limits.
- **Future work (separate ticket): user-mintable API JWTs.** Let a user create API-style JWTs /
  personal access tokens to pass into remote tools to identify themselves, with revocation
  (delete a token to cancel that authentication). Until then, the existing login→JWT path is
  used.

## Tool surface

Two complementary styles, both routed through `CommandGateway`:

1. **Generic command tool** — `requel.runCommand(commandType, input)`. Dispatches any
   allowlisted command type; near-zero marginal cost to cover the full set; ideal for
   CLI/scripting. Guarded by the policy below.
2. **Typed tools** — one per command in the shared `GatewayCommandCatalog` (issue #104). Each is
   **named after its command type** (`EditGoal`, `EditStory`, `EditActor`, `EditUseCase`,
   `EditScenario`, `EditGlossaryTerm`, `EditNonUserStakeholder`, `AddGoalToGoalContainer`,
   `EditNote`, `EditIssue`, `Delete*`, …) and its JSON schema is derived from the command's
   registered input DTO. Because the catalog is built from the same allowlist the policy enforces,
   the typed tool set can't drift from what the gateway permits. (There is no separate
   `create*`/`edit*` split — a single `Edit*` tool creates when no id is supplied and updates
   otherwise.)

Ship both: generic for coverage/scripting, typed for AI affordances. Typed tools are sugar over
the same dispatch path, not a second write path.

### Command allow/deny policy

- **Denylist (never exposed):** any system user/role command; `EditUserStakeholder`.
- **Delete guard:** `DeleteStakeholder` is allowed only for **non-user** stakeholders; the
  gateway must reject deletion of user stakeholders (deleting one severs a person's project
  access).
- **Allowlist:** project + entity `Edit*`; `Add*/Remove*` association commands;
  `SetPrimaryScenarioOnUseCase`; `EditGoalRelation`; `ConvertStepToScenario`; `Copy*`; entity
  `Delete*`; `EditGlossaryTerm`; `EditNonUserStakeholder`; annotation commands (`EditNote`,
  `EditIssue`, …); report commands if desired.
- Writes are **opt-in** per deployment and ideally per project (via `AssistantProjectSettings`).
  Default off.
- No tool reaches repositories, SQL, the filesystem, or arbitrary execution — only command
  dispatch.

### Read tools (reuse existing)

`requel.listProjects`, `requel.getProject`, `requel.getProjectContext`, `requel.getProjectTree`,
`requel.getEntity`, `requel.getEntityNeighbors`, `requel.getAnnotations`,
`requel.searchProjectEntities`, plus the non-persisting `requel.draftAnnotation`.

## The issue tracker → Goals workflow (end to end)

Source-agnostic; Jira shown as the example.

1. **Client reads the issue** via its own connector (Jira, GitHub, Linear, …) and extracts the
   acceptance criteria — from an explicit AC section if present, else inferred discrete
   requirements from the body. (No Requel involvement.)
2. **Resolve the target project** (`requel.getProject` / `requel.listProjects`).
3. **Load existing goals** (`requel.getProjectContext`) to compare against the requirements.
4. **For each requirement:** `EditGoal` (create/update), then `EditNote` on the
   returned goal id with provenance: client, `sourceSystem`/`sourceRef`/`sourceUrl`, criterion
   reference.
5. **Report** goals created vs. updated, with ids and the source ref, for review.

Relevant input shapes (today):

```java
// service-api
public record EditGoalInput(String projectName, Long goalId, String name,
                            String text, Integer version) {}
public record EditNoteInput(String entityType, Long entityId, String text, Long noteId) {}
```

`EditGoalCommandImpl` is `AuthorizableCommand` (`RequiresStakeholderPermission(Goal.class,
"Edit")`) and `AnalysisRequestSource`, so a gateway-created goal also triggers the normal
post-edit assistant analysis — same as a UI edit.

## Reconciliation (upsert / matching) — its own ticket

The hard part, separated so it does not block the gateway.

Problem: re-running after the source issue changes must update the right goals, not duplicate
them. Candidate keys and their failure modes:

**Important: `EditGoal` is not an upsert-by-name.** `EditGoalCommandImpl` throws a uniqueness
conflict when creating a goal whose name already exists, and updates only when a goal id is
supplied. So reconciliation cannot rely on "call EditGoal by name"; the workflow must resolve an
existing goal id first. Candidate keys and their failure modes:

- **Exact goal name match (via a goal query) → update by id** — breaks when the requirement text
  (and a name derived from it) is edited.
- **Ticket id + AC index** — breaks when an AC is inserted/removed/reordered.
- **Stable external correlation id in the provenance note** — robust *only if* the source has a
  stable per-item id. **Free-text ACs (Jira descriptions, GitHub issue bodies, …) have no stable
  per-item id**, which is the common case here.

Phased plan:

- **v1 (workflow ticket): id-based update via lookup + provenance note.** The convenience tool
  resolves an existing goal id (exact name match and/or a provenance match on
  `sourceSystem`+`sourceRef`+`criterionHash`) and calls `EditGoal` with that id to update;
  otherwise it creates a new goal. The provenance note stores the source ref + a
  normalized-criterion-text hash in a machine-parseable block. **Known limitation:** a minor edit
  to a free-text requirement changes both name and hash, so v1 creates a new goal and leaves the
  prior one as a discoverable orphan (same `sourceRef`, different hash). Accepted for v1.
- **Separate ticket: similarity read tool** `requel.findBestGoalMatch(projectName, text, scope,
  threshold)` returning ranked candidates above a threshold so the client can update by id. Start
  with normalized-text / token-overlap (Jaccard, trigram) scoring, optionally scoped to goals
  whose provenance note references the same source ref; upgrade to embeddings later without
  changing the contract or any write path. Note current queries don't expose full goal text, so
  this ticket also adds a candidate-goal query.

## Security & governance

- External writes opt-in, off by default; prefer per-project enablement.
- Authorization is always the authenticated user's; no elevation. The denylist + delete guard
  enforce the no-user-management boundary independently of authorization. Note: only commands
  implementing `AuthorizableCommand` are enforced by `AuthorizingCommandHandler`, so each
  allowlisted command needs an authorization audit (see ticket 1).
- Per-client pseudo-user gives independent audit attribution and rate/concurrency caps.
- Provenance is mandatory for auto-generated entities (a `NOTE`).
- Treat client-supplied text as untrusted: cap name/text/note lengths; reject oversized payloads
  at the tool boundary before dispatch.
- Remote connector front-end only: TLS host, Anthropic IP allowlist, Owner registration. None of
  this applies to the local stdio bridge or CLI.

## Suggested ticket split

1. **Command gateway + write tools + stdio bridge.** `gateway-api`, in-process + REST-backed
   impls, allow/deny policy + delete guard, generic `requel.runCommand` + typed tools,
   per-client pseudo-user, opt-in flags, and the `mcp-bridge` stdio front-end for e2e demo.
   The bulk of the value. **(This doc's ticket #1.)**
2. **CLI front-end** (`requel-cli`) over the same gateway, plus optionally exposing the remote
   connector. Transport/packaging on top of #1.
3. **Issue-tracker → goals workflow + v1 reconciliation.** Provenance-note convention, goal-id
   lookup (not name-upsert), source ref + text-hash in notes. Depends on #1.
4. **Smart reconciliation.** `requel.findBestGoalMatch` similarity tool; token-overlap first,
   embeddings later. Independent; lands after #3.

## Resolved decisions

- Gateway is a pure `gateway-api` module; front-ends (`mcp-server`, `mcp-bridge`, `requel-cli`)
  consume it. In-process impl in `service-impl`; REST impl in a client lib.
- Stakeholders: include `EditNonUserStakeholder`; exclude `EditUserStakeholder`; `DeleteStakeholder`
  gated to non-user only. Project creation auto-creates the creator's user-stakeholder, which is
  fine.
- Identity: the JWT's authenticated user is the triggering/owning user (consistent with the
  existing API and `McpCallAudit`'s `triggeringUserId`); per-client pseudo-user for attribution.
  No shared service account. Future: user-mintable, revocable API JWTs.
- Provenance is a `NOTE`.
- `requel.runCommand` is exposed to **all** front-ends, AI included. Authorization already
  bounds it to what the authenticated user could do in the UI, so no extra front-end gating is
  needed.
- Jira ACs are free-text → no stable id → v1 reconciliation tolerates staleness; smart matching
  is ticket #4.
- Ticket #1 includes the stdio bridge for end-to-end demonstrability.
- Tool surface: both generic `runCommand` and typed convenience tools.

## Remaining open questions

None outstanding — all design decisions resolved.
