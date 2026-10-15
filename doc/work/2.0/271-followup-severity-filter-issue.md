Follow-on to #271, which adds `LOW | MEDIUM | HIGH` severity to issues and orders every issue list by it, but adds no way to filter.

### Why

On the roundtable project most open issues are lexical output that #271 defaults to `LOW`. Ordering puts real findings on top, but a reviewer or an MCP client still receives, and pages through, everything. A minimum-severity filter lets a caller ask for "`MEDIUM` and up" and get only the findings worth reading.

### Work

- `GET /api/projects/{name}/open-issues` takes an optional `minSeverity` (case-insensitive `LOW | MEDIUM | HIGH`). Absent means everything, as today. An unknown value is a 400 naming the parameter.
- Pass it through `QueryGateway.getOpenIssues`, the REST client, the in-process gateway and the gateway query controller.
- MCP: `getOpenIssues` and `getProjectContext` accept an optional `minSeverity`, with the enum in their schemas.
- CLI: `open-issues --min-severity <level>`.
- Angular Open Issues: a severity filter (multi-select or "at least") in the table toolbar. Optionally the same on the per-entity annotations list.

### AC

- `minSeverity=MEDIUM` returns `MEDIUM` and `HIGH` issues only, still in severity order; absent returns all.
- An unknown `minSeverity` is rejected with an error naming the parameter, on REST, MCP and CLI.
- `getProjectContext` with `minSeverity` filters `openIssues` only; the rest of the context is unchanged.
- The Angular filter is covered by unit specs and an e2e case.

### Not in scope

- Filtering by anything other than severity.
- Changing the default (unfiltered) behavior of any endpoint.
