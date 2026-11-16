# #371 — Carry project assistant definitions in project XML export/import

https://github.com/rreganjr/Requel/issues/371

Branch `371-definitions-in-project-xml` from `release/2.1`, PR to `release/2.1`. Size 3.

## Summary

A project's own assistant definitions (#264) don't travel with its XML. This exports them and
imports them through the same validator as `CreateAssistantDefinition`. Imported definitions
arrive switched off, because their instructions come from a file.

## Locked decisions (2026-11-03)

1. **Only the project's own definitions travel:** its customized copies of bundled definitions,
   and the definitions it created. These stay behind:
   - Bundled definitions.
   - The per-project on/off switches. #268 already decided assistant settings don't go in
     the XML.
   - The data-handling settings (#262). A file must not be able to switch off redaction or
     allow egress on the install that imports it, so that install's defaults apply.
2. **Imported definitions arrive switched off** in the new project, through #268's per-project
   switch (`ProjectAssistantSettingsStore.setEnabled(project, key, false, importer)`). Someone
   turns each one on before it runs.
3. **An invalid definition is skipped with a WARN, and the rest of the import goes ahead**, the
   way a bad source or ignored finding already is. Import has no warning channel to the user, so
   the skip shows only in the log.
4. **Schema:** the definitions element is added to `doc/samples/project.xsd` and to the published
   `website/integration/2.0/project.xsd`, as #459 did. It is an addition, which a minor release
   allows (RELEASE_PROCESS); whether to publish a separate 2.1 schema is a release-time decision.
5. **A customized copy whose bundled definition isn't on the importing install** comes in as a
   project-only definition with the same key and content, and no `forkedFromVersion`.

## Contracts

XML, inside `<project>` after `externalSources`, both optional:

```xml
<assistantDefinitions>
  <assistantDefinition key="ai-review-goal" kind="REVIEW" forkedFromVersion="2" localOnly="false">
    <displayName>…</displayName>
    <scope><type>Goal</type></scope>
    <contextProviders><provider>entity</provider></contextProviders>
    <contextBudgets><budget provider="goal-siblings" characters="4000"/></contextBudgets>
    <instructions>…</instructions>
    <vocabulary><entry type="…" category="…">description</entry></vocabulary>
  </assistantDefinition>
</assistantDefinitions>
```

Task type, output schema and its version follow from `kind`, as on create. `executorBean` isn't
written; if a file carries one, validation rejects it.

## Changes

project-domain
- `ProjectAssistantDefinitions`:
  - `own(projectId)` lists the project's own rows as `View`s (forks and project-only).
  - `importDefinition(projectId, Draft, Integer forkedFromVersion, String by)` returns the
    problems and writes nothing if there are any.

assistant-core
- `AssistantDefinitionStore`:
  - `own`.
  - `importDefinition`:
    - The key of a bundled definition on this install, with `forkedFromVersion`, becomes a
      fork with the file's content, keeping the file's `forkedFromVersion`. A different bundled
      version then shows as out of date, as usual.
    - Otherwise it is a project-only definition (decision 5).
    - It runs the same checks as create, fork and `write`: the field validator, reserved keys,
      no `executorBean`, the length cap, collisions within a task type, and the per-project
      maximum.
    - Everything is checked before anything is written. The store throws on an invalid save,
      and that would mark the import's transaction rollback-only.

project-jpa
- `AssistantDefinitionXml` (new, the export and import carrier) and
  `ProjectImpl.exportAssistantDefinitions` (`@Transient`, like `exportIgnoredFindings`).
- `ExportProjectCommandImpl` fills it from `own(projectId)` and adds the carrier to the JAXB
  classes.
- `ImportProjectStreamingCommandImpl.importAssistantDefinitions`:
  - read with a StAX importer (the `IgnoredFindingStaxImporter` pattern);
  - for each definition, `importDefinition`;
  - on problems, `log.warn` with the key and the problems;
  - on success, the switch off.
- `AbstractProjectCommand`: the `ProjectAssistantDefinitions` and `ProjectAssistantSettingsStore`
  accessors, injected like the ignored-finding store.

utils-jaxb
- `AssistantDefinitionStaxImporter` and its import XML class.

schema
- `doc/samples/project.xsd` and `website/integration/2.0/project.xsd` (identical).

## Steps

1. Store methods and their unit tests.
2. Carrier, export, schema.
3. StAX importer, import.
4. `tmp/371-verify.sh`, round-trip IT.

## Test plan

`tmp/371-verify.sh`: backend `mvn clean verify -pl modules/coverage-report -am` and the coverage
check. Angular isn't touched.

- `AssistantDefinitionStoreTest` (or the existing authoring test), `importDefinition`:
  - a fork keeps its `forkedFromVersion`;
  - a project-only definition;
  - a fork of a key this install doesn't bundle becomes project-only;
  - each rejection writes nothing: `executorBean`, a bad key, a reserved key, over the length
    cap, an unknown type or provider, a collision within a task type, over the per-project
    maximum.
- `ProjectXmlStreamingRoundTripIT`, assistant definitions:
  - export, then import, reproduces the forks and project-only definitions;
  - the exported XML validates against the schema;
  - the imported definitions are switched off in the new project;
  - an imported fork whose bundled version differs shows as out of date (`bundledVersion`
    differs from `forkedFromVersion`);
  - one invalid definition in the file is skipped, and the rest of the project and the other
    definitions import.
- A file without definitions imports as before (the existing round-trip ITs).
- `ProjectSchemaNameLengthTest` keeps the two schema copies identical.

## Out of scope

- Organization-level definitions (#370).
- Exporting bundled definitions.
- Signing or provenance checks on import files.
- A warning channel from import to the user.

## Risks

- **Rollback-only.** Any exception inside the store during import would mark the whole import's
  transaction rollback-only. Validation runs first, so an invalid definition never reaches a save.
- **Collisions within a file.** Two definitions in one file that collide with each other: the
  second is rejected, as it would be if created by hand.

## AC mapping

| AC | Covered by |
|---|---|
| Export then import reproduces forks and project-only definitions | ProjectXmlStreamingRoundTripIT |
| A file without definitions imports as before | existing round-trip ITs |
| Invalid definitions are rejected (skipped, decision 3) and never stored | AssistantDefinitionStoreTest, ProjectXmlStreamingRoundTripIT |
| An imported fork with a different bundled version shows as out of date | ProjectXmlStreamingRoundTripIT |
| Exported XML validates against the published schema | ProjectXmlStreamingRoundTripIT, ProjectSchemaNameLengthTest |
