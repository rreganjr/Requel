# Smart goal reconciliation: find-best-match read tool

Part of the v2.0 MCP command-gateway series (ticket 4 of 5). Design doc:
`doc/local_mcp_bridge.md`. Depends on the issue-tracker→Goals workflow ticket (series ticket 3).

## Goal

Quarantine the hard part — matching an edited acceptance criterion back to the goal it already
produced — behind a single read tool, so the gateway and the v1 workflow can ship without it.
Provide `requel.findBestGoalMatch` returning the best-matching existing goal above a similarity
threshold, so a client can choose to update that goal rather than create a duplicate.

## Background

Requirements from an issue tracker are often free-text with no stable id (Jira ACs, GitHub
issue bodies, etc.), so the v1 workflow keys on exact goal name / criterion-text hash and
tolerates orphaning a goal when a requirement is edited. This ticket adds the similarity layer
that resolves edited requirements to their existing goals. Keeping it a **read** tool means it
never touches the write path; the client still applies updates through `requel.editGoal` (which
updates by goal id — there is no name-upsert).

Two current-code constraints this ticket must address:

- **No query returns full goal text today.** `ProjectQueryGateway.getProjectContext()` returns
  project summary, tree, glossary, and open issues; `searchProjectEntities` returns references by
  name, not full bodies. The matcher needs a query that returns candidate goal **ids, names,
  text, versions, and provenance** — this ticket adds or identifies it.
- **Provenance scoping requires structured parsing.** Source-ref scoping depends on the
  machine-parseable provenance-note format defined in ticket 3; there is no structured provenance
  model otherwise. This ticket reuses that parser (and its tests).

## Scope

In scope:

- **`requel.findBestGoalMatch(projectName, text, scope, threshold)`** returning a **ranked list**
  of candidate goals with similarity scores and matched fields (or empty when none clears the
  threshold). Each candidate uses the same stable `EntityReferenceDto`-style shape current MCP
  query tools use (`Goal` type + id/name), plus `score` and `matchedFields` — no parallel
  reference contract.
  - `scope`: optionally restrict to goals whose provenance note references the same source issue
    ref (`sourceSystem`/`sourceRef`). Behavior when `scope` is requested but no provenance notes
    are present must be defined — recommended: fall back to all project goals and flag that scope
    was not applied.
  - `threshold`: minimum score to return a match.
- **A query for candidate goals** (ids, names, text, versions, provenance) backing the matcher.
- **Scoring v1:** normalized-text token overlap (Jaccard / trigram), deterministic, in-process,
  with documented normalization (casing, punctuation, stop words, stemming, whitespace).
- **Pluggable scorer interface** so the implementation can be upgraded to embedding-based
  semantic similarity later without changing the tool contract or any write path.
- **Performance bound:** an in-process all-goals scan is acceptable for v1 but must have a
  documented project-size cap / pagination strategy.

Out of scope:

- Embedding/vector infrastructure (a later enhancement behind the same interface).
- Automatic updates — the tool only suggests; the client decides and writes via `editGoal`.

## Design

- The tool lives behind the `QueryGateway`; matching logic sits behind a `GoalMatcher` interface
  with a `TokenOverlapGoalMatcher` v1 implementation.
- Source-issue-ref scoping reuses the ticket-3 provenance parser.
- Returns stable goal references + scores + matched fields so the client can present or auto-apply
  by threshold.

## Workflow change it enables (client-side)

For each edited requirement: call `findBestGoalMatch`; if a candidate clears the threshold, the
**client** updates that goal via `editGoal` (preserving its id) and refreshes the provenance
note; otherwise it creates a new goal. The tool itself remains read-only; the create-vs-update
decision and the write are client behavior.

## Security & Privacy

- Read-only; bounded by the authenticated user's project access.
- No external calls in v1; if embeddings are added later, follow the AI data-handling policy
  (opt-in, redaction, retention) from `doc/ai-assistance-plan.md`.

## Implementation Steps

1. Add/identify a query returning candidate goal ids, names, text, versions, and provenance.
2. Define the `GoalMatcher` interface and `TokenOverlapGoalMatcher` v1 with documented
   normalization.
3. Add `requel.findBestGoalMatch` to the read tools via `QueryGateway`, with optional
   source-issue-ref scoping using the ticket-3 provenance parser and a defined no-provenance
   fallback.
4. Document a project-size/performance cap.
5. Update the issue-tracker→goals workflow docs to use match-then-update (client-side) for edited
   requirements.
6. Tests below.

## Testing Strategy

- Scoring tests over fixture goals (exact, minor-edit, unrelated) asserting ranking/threshold.
- Scope-filter test (same source ref vs whole project) and the no-provenance fallback case.
- Provenance-parser tests over the exact ticket-3 format.
- End-to-end (workflow): edit a requirement; assert the client uses the returned candidate id in
  `editGoal` and no duplicate is created.

## Acceptance Criteria

Split server vs workflow:

- **Server:** `requel.findBestGoalMatch` returns a ranked candidate list with scores and matched
  fields, respects `threshold`, applies `scope` (with the defined fallback), and uses the standard
  MCP reference shape.
- **Workflow (client):** a minor requirement edit yields a candidate above threshold, the client
  updates that goal id via `editGoal`, and the end-to-end run creates no duplicate.
- The matcher is swappable (interface) without changing the tool contract.

## Dependencies

- Series ticket 3 (provides the machine-parseable provenance notes and parser).
- Series ticket 1 (the candidate-goal query may extend the gateway's `QueryGateway`).

## Open Questions

- Default threshold, and confirmation that the client (not the server) owns create-vs-update.
- Best-match-only vs ranked list as the default return (scope says "candidate(s)"; recommend
  ranked list capped at N).
- Trigram vs Jaccard vs a small combined score for v1.
- No-provenance-under-scope behavior: fall back to all goals (recommended), fail closed, or
  return no match.
