# Codex review: issue_smart_goal_reconciliation.md

Review target: `doc/issue_smart_goal_reconciliation.md`

## Findings

### 1. Goal search/matching cannot rely on `getProjectContext` alone

The ticket says matching should run through `QueryGateway` and compare against existing goals.
Current MCP `ProjectQueryGateway.getProjectContext()` returns project summary, tree, glossary,
and open issues. It does not expose full goal text for all goals. The existing
`searchProjectEntities` method returns references by name, not full text bodies.

The design should add or identify a query method that returns candidate goal ids, names, text,
versions, and annotations/provenance as needed by the matcher.

### 2. Provenance-note scoping requires structured parsing that does not exist yet

The doc scopes matches to goals whose provenance note references the same issue
`sourceSystem`/`sourceRef`. Current annotations are plain `NoteDto` text; there is no structured
provenance model or parser. The preceding workflow ticket must define a machine-parseable
format, otherwise this matcher will either need brittle string matching or no reliable scope.

Acceptance criteria should require parser tests over the exact provenance-note format.

### 3. Entity type/name assumptions need to match current MCP schemas

Current MCP tools use entity type strings such as `Goal`, `Story`, `Actor`, `UseCase`,
`Scenario`, and `GlossaryTerm` for `getEntity`/`getAnnotations`. The reconciliation doc should
state that `findBestGoalMatch` returns the same stable `EntityReferenceDto`-style shape used by
current MCP query tools, plus score and matched fields. That avoids inventing a parallel
reference contract.

### 4. Threshold ownership is still open but acceptance criteria assume behavior

The open questions ask whether the client or server owns create-vs-update. The acceptance
criteria say a minor edit resolves to the existing goal and updates it instead of creating a
duplicate. Since this is a read-only tool, the update is client behavior, not server behavior.

The acceptance criteria should split server and workflow checks:

- server returns a candidate above threshold,
- client workflow uses that candidate id in `editGoal`,
- no duplicate is created in the end-to-end workflow.

## Completeness gaps

- Define normalization rules: casing, punctuation, stop words, stemming, and whitespace.
- Define whether the tool returns only the best match or a ranked list; the scope says
  "candidate goal(s)" but the goal names a single best match.
- Include performance bounds for large projects, since an in-process all-goals scan may be
  acceptable for v1 but should have a documented cap.
- Include behavior when `scope` is requested but no provenance notes are present: fail closed,
  fall back to all goals, or return no match.

