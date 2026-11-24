# #526 — Report editor: annotations fail to load (unknown entity type ReportGenerator)

https://github.com/rreganjr/Requel/issues/526

Branch `526-report-annotations` from `release/2.2`, PR to `release/2.2`. Size 2 (likely 1).

## Summary

Found in the #519 UI review: the report editor's Annotations section always shows "Annotations
could not be loaded." `GET /api/annotations?entityType=ReportGenerator` is rejected with
`Unknown entity type: ReportGenerator`.

`ReportGenerator` is the only annotatable entity missing from
`ProjectAnnotatableRegistryConfiguration`. Everything else already treats it as annotatable: the
entity carries the annotations collection (`AbstractProjectOrDomainEntity`), the
`reports_annotations` join table has existed since V1 and V17/V26 clean it up,
`DeleteReportGeneratorCommand` removes a report's annotations, and the Angular
`ENTITY_ROUTES` maps it to `reports`. An omission, not a decision.

## Locked decision (2026-11-23)

Register `ReportGenerator` rather than drop the section. This enables notes and issues users add
by hand. Assistants are unchanged: the lexical assistant skips report templates on purpose, and AI
review stays off for them.

## Steps

1. `ProjectAnnotatableRegistryConfiguration`: map `ReportGenerator` to `ReportGeneratorImpl`
   (registry and the `@Any` discriminator both come from this map).

2. `QueryDescriptions.ANNOTATABLE_ENTITY_TYPES` and the annotatable-type text there and on
   `EditNoteInput`/`EditIssueInput`: add `ReportGenerator`. The MCP read tools advertise that list
   as an enum, and `McpToolCatalogLockstepIT` requires it to equal the registry's keys (found by
   the first verify run).

## Test plan

- `ProjectAnnotatableRegistryConfigurationTest`: resolves `ReportGenerator` both ways.
- `ReportAnnotationsApiIT` (new, MockMvc over HTTP like the editor): a note and an issue added to
  a report are listed back; a report with none lists none; deleting the report removes its
  `annotation_annotatable` and `reports_annotations` rows. All three fail without the fix.
- `mvn clean verify`.

## Acceptance criteria → where

| AC | Covered by |
|---|---|
| The report editor no longer shows a load error for annotations | Step 1; `ReportAnnotationsApiIT` (the editor's own request) |
| A test covers the chosen path | `ReportAnnotationsApiIT`, registry test |
