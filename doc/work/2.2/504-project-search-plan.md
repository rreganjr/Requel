# #504 — Project search: requirements and issues

https://github.com/rreganjr/Requel/issues/504

Branch `504-project-search` from `release/2.2`, PR to `release/2.2`. Size 3.

## Summary

Search a project's requirements and issues from one box. Each list page filters only itself, and
the app-wide search box was removed in #223 because it was a placeholder. The gateway already has
a name-only search, `searchProjectEntities`, on REST, MCP and the CLI. Per the comment on the
issue, this ticket extends that read rather than adding a second one: it adds steps, stakeholders
and issue text, matches text as well as names, and returns which field matched and a snippet.

## Locked decisions

From planning (2026-11-17, in the issue):
- One project at a time; no search across projects.
- SQL `LIKE`, ignoring case, with `%` and `_` in the search text matched literally. No full-text
  index.
- Issues are included (open and resolved); positions, arguments and notes are not.

At review (2026-11-25):
1. **Searches shorter than 2 characters are refused on every surface**: REST, MCP, CLI and UI.
   The old "an empty query matches everything" behaviour goes. The goal upserter, which used the
   search to find a goal by its exact name, looks the goal up in the project tree instead.
2. **Limit and paging**: 200 by default, 1000 at most. The order is stable, so `offset` + `limit`
   pages. The gateway, MCP and the CLI keep a plain list and gain an optional `offset`. The UI's
   endpoint, new and with no other callers, returns a page instead: `results`, `total`,
   `countsByType`, `offset` and `limit`. The search matches everything first and pages after,
   so the total and the counts are always complete. The results page loads 200, has "Load more" for the next 200 ("212
   of 412"), and filters to a type when its count is clicked.
3. **Steps and scenarios are searched on their stored name and text.** A step with an actor role
   (#468) stores only its action, so searching "Operator" doesn't find a primary-actor step. Each
   result shows the name as it reads (`displayName`).

## Contract

- **Result** — `ProjectSearchResultDto`. It keeps `EntityReferenceDto`'s fields, so existing JSON
  readers don't break:
  - `entityType`, `id`, `name`;
  - `matchedField`: `name` or `text`;
  - `snippet`: up to about 160 characters around the first match, with "…" where it was cut;
  - `matchStart` and `matchLength`: where the match falls in the snippet, for highlighting;
  - for an issue: `name` is the start of its text, `resolved` says whether it is resolved, and
    `onEntityType`, `onEntityId` and `onEntityName` give the first entity it is on.
- **Entity types** — Goal, Story, Actor, UseCase, Scenario, Step, GlossaryTerm, Stakeholder and
  Issue. Stakeholders are matched on their name (a user stakeholder's user name) and text.
- **Order** — name matches before text matches, then by entity type in that order, then by
  creation (id).
- **Reads**:
  - REST (UI): `GET /api/projects/{name}/search?q=&type=&offset=&limit=` on
    `ProjectQueryController`, gated by project read access like every other project read (403
    without it). It answers `ProjectSearchPageDto` (`results`, `total`, `countsByType`, `offset`,
    `limit`), or 400 with a message when the search is under 2 characters or the type, offset or
    limit is invalid.
  - Gateway: `GET /api/gateway/projects/{name}/search` and `QueryGateway.searchProjectEntities`,
    which stay a list. The existing `(project, query)` method delegates to a new
    `(project, query, entityType, offset, limit)` one.
  - MCP: same tool name, with optional `entityType`, `offset` and `limit` added.
  - CLI: `requel search <project> <query> [--type T] [--offset N] [--limit N]`, and the text
    output gains the matched field and the snippet.
  - `QueryDescriptions.SEARCH_PROJECT_ENTITIES` and `SEARCH_QUERY` are rewritten to match.

## Steps

1. **Service**: a `ProjectSearchService` in `service-impl` runs one JPQL query per entity type
   (`lower(name) like :p escape '!'`, then the same on text), plus issues by grouping object. It
   escapes `!`, `%` and `_`, builds the snippets, then orders the results and takes the page. It
   also counts the matches per type.
   `ProjectQueryController` adds the endpoint, and `InProcessQueryGateway` delegates to the
   service.
2. **Gateway**: `QueryGateway`, `RestQueryGateway`, `GatewayQueryController`, the MCP schema and
   dispatch, and the CLI command and its options. `RequirementGoalUpserter` stops using search
   (decision 1). The test stubs (`InMemoryGateway`, `StubProjectQueryGateway`) change to match.
3. **Angular**:
   - a `ProjectSearchService` client;
   - a search box in the project workspace header, which goes to
     `/projects/:name/search?q=`;
   - a results page on `app-list-page`, grouped by type with counts, the match highlighted in
     each snippet, each result linking to its entity (an issue links to the entity it is on),
     "Load more" with "N of total", and an empty state;
   - a type filter, and the 2-character rule shown on the box.

## Test plan

- **`ProjectSearchIT`** (requel-app, H2), one test per AC, and **`ProjectSearchMySqlIT`**, the
  same cases on MySQL 8.4 with the Flyway schema (`text` is LONGTEXT, lower-cased through a
  cast):
  - the fixture term in one goal's name, one story's text and one issue's text returns all
    three, the goal first;
  - text in a position isn't returned;
  - case is ignored, and `50%` matches only a literal `50%`;
  - a one-character search is refused with a clear message;
  - another project's matches never appear, and the read is 403 without access
    (`ProjectQueryControllerTest`);
  - the type filter keeps only that type.
  - It also covers paging (offset/limit, total, complete per-type counts), the snippet window,
    steps and stakeholders.
- Gateway, MCP and CLI tests for the new fields and options; the upserter test for decision 1;
  `McpToolCatalogLockstepIT`.
- Angular specs for the header box (the 2-character rule, navigation) and the results page
  (grouping and counts, highlighting, links, empty state, "Load more"), with axe.
- One e2e test: search from the header and open a result.
- Gate: `mvn clean verify`, the whole unit suite, typecheck and dev build.

## Out of scope

- Search across projects.
- Position, argument and note text.
- Fuzzy matching or stemming.
- Matching a role step's rendered actor (decision 3).

## Risks

- `LIKE` scans every row. That's fine at project scale (hundreds of entities), but each search
  runs about ten small queries.
- `text` is a `@Lob`, and Hibernate 6 won't `lower()` a CLOB, so the query casts it to a string
  first. `ProjectSearchMySqlIT` checks that this renders on MySQL.
- MySQL's `utf8mb4_0900_ai_ci` collation also ignores accents, so on MySQL "cafe" finds "café"
  and H2 doesn't. When Java can't place that match, the snippet shows the start of the field
  with no highlight.
- Searches shorter than 2 characters now fail where they used to succeed. External scripts that
  relied on an empty query get a 400 with a message.

## Acceptance criteria → where

| AC | Covered by |
|---|---|
| Term in a goal name, a story's text and an issue's text returns all three, goal first | Step 1 order; `ProjectSearchIT` |
| Position text not returned | Step 1 (issues only); `ProjectSearchIT` |
| Case-insensitive; `50%` matches only a literal `50%` | Step 1 escaping; `ProjectSearchIT` |
| A one-character search is refused with a clear message | Decision 1; `ProjectSearchIT`, UI spec |
| Other projects never appear; 403 without read access | Project-scoped queries, `requireProjectAccess`; `ProjectSearchIT` |
| The type filter limits results to that type | Step 1; `ProjectSearchIT` |
