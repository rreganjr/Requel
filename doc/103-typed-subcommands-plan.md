# requel-cli — dynamic typed write subcommands from command descriptors (issue #103)

Follow-up to #70 Phase A (Milestone 4). M4 shipped the generic `requel run <Type> --input '{...}'`
plus the `requel commands` discovery listing, and explicitly **deferred** the richer form: a picocli
subcommand *per command* with per-field flags (e.g. `requel edit-goal --project Demo --name G`)
generated at runtime from each command's input DTO schema. This plan implements that deferred form.

Prerequisites are in place: #70 Phase A (CLI + gateway REST client), #101 (read subcommands), and
#104 (MCP write tools generated from the shared `GatewayCommandCatalog`, which also built a working 
record-DTO → JSON-schema generator we will reuse).

## Status

Implemented on branch `103-typed-subcommands`: M1 (shared `CommandInputSchema` in `gateway-api`, MCP
refactored onto it), M2 (`schema` on the descriptor endpoint `DescriptorView` + client `CommandInfo`),
and M3 (`TypedCommands` — runtime-registered typed write subcommands with schema-derived flags,
raw-JSON for nested fields, offline fallback). M4 here is docs. The end-to-end CLI-against-live-server
integration test from the testing strategy remains an optional follow-up (the generation, dispatch,
required-flag, raw-JSON, and fetch-gating paths are covered by `TypedCommandsTest`; endpoint schema by
`GatewayCommandControllerTest`/`RestGatewayCatalogTest`; the shared generator by
`CommandInputSchemaTest`).

## Summary

Two moves, server then client:

1. **Server:** extend the descriptor endpoint (`GET /api/gateway/commands/descriptors`) so each
   descriptor carries a **JSON schema** for its `inputType`, derived from the command's registered
   input DTO record (types + `required` array). The schema-generation logic already exists inside
   `McpWriteService` (from #104); extract it into a shared class in `gateway-api` and have both the
   MCP server and the descriptor endpoint use it, so MCP, the CLI, and the gateway policy stay in
   lockstep on one source of truth.
2. **Client:** at startup the CLI fetches the catalog and **registers a typed picocli subcommand per
   write command**, mapping each schema field to a typed `--flag`, with `required` flags taken from
   the schema. Each typed subcommand assembles the input map and dispatches through the REST
   `CommandGateway`. When the server is unreachable, no typed subcommands are registered and the
   generic `run` remains the fallback.

## Grounding: what actually exists today (verified in-tree)

- **`CommandDescriptor`** (`gateway-api`, record): `commandType`, `inputType` (the input DTO
  `Class<?>`, `Void.class` when none), `title`, `description`, `write`, `authorizationHint`. Its
  javadoc already anticipates this work: *"the `inputType` is the command's registered input DTO
  class, from which a JSON schema can be derived."*
- **`GatewayCommandCatalog`** (`gateway-api`, interface): `descriptors()` / `find(commandType)` /
  `empty()`. The single source of truth, built in `service-impl` from the same `ALLOWED` set the
  gateway policy enforces.
- **`GatewayCommandController`** (`service-impl`): `GET /api/gateway/commands/descriptors` returns
  `List<DescriptorView>`; `DescriptorView(commandType, inputType-as-simpleName, title, description,
  write, authorizationHint)` — **no schema field yet.** Write-flag-gated (empty list when writes
  off).
- **`McpWriteService`** (`mcp-server`): generates one typed tool per catalog descriptor. Contains
  the schema generator we will extract — all currently `private static`:
  - `schemaFor(Class<?>)` → `{type:object, properties, required, additionalProperties:false}`.
  - `jsonType(Class<?>)` → string/integer/number/boolean/array/object (enums → string).
  - `isRequired(RecordComponent)` / `hasRequiredAnnotation(...)` — a field is required when the
    component **or** its accessor carries `jakarta.validation` `@NotNull`/`@NotBlank`, matched by
    fully-qualified name so the module needs no compile-time dependency on the validation API.
  - `objectSchema` / `stringType` / `integerType` / `booleanType`, and `fieldNames(Class<?>)`.
  - Covered today by `McpWriteServiceSchemaTest`, `McpWriteCatalogLockstepTest`, and the
    full-context `McpToolCatalogLockstepIT`.
- **`RestGatewayCatalog`** + **`CommandInfo`** (`gateway-rest-client`): client fetches the endpoint
  into `CommandInfo(commandType, inputType, title, description, write, authorizationHint)` — **no
  schema field yet.**
- **`CommandsCommand`** (`requel-cli`): prints the catalog (text / `--output json`).
- **`RunCommand`** (`requel-cli`): generic `run <CommandType> --input`.
- **`UpsertGoalCommand`** (`requel-cli`): a **hand-written typed subcommand** — the concrete template
  this work generalizes. `implements Callable<Integer>`, `@ParentCommand RequelCli parent`, one
  `@Option` per field, builds a `RestCommandGateway` from `parent.url` + `parent.tokenSource()`. Note
  it is a *composite* orchestration (create-or-update via `RequirementGoalUpserter`), not a single
  command dispatch, so it stays bespoke and is **not** one of the generated subcommands.
- **`RequelCli`** (`requel-cli`): `@Command(subcommands = { ... })` static list; `main()` does
  `new CommandLine(new RequelCli()).execute(args)`. Typed subcommands must be added to that
  `CommandLine` **before** `execute()`.
- **Module deps:** `mcp-server`, `service-impl`, `requel-cli`, and `gateway-rest-client` all depend
  on `gateway-api` — so `gateway-api` is the correct shared home for the extracted generator (it adds
  only `java.lang.reflect` + `java.util`, no new dependencies).

## Decided

- **Schema generator extracted to `gateway-api` (decision 1a).** New pure-JDK class (working name
  `CommandInputSchema`) exposing `static Map<String,Object> of(Class<?> inputType)`, moved verbatim
  from `McpWriteService` (reflection over record components, validation annotations matched by FQN).
  `McpWriteService` is refactored to delegate to it — behavior identical, existing MCP schema/lockstep
  tests stay green. This preserves the single-source-of-truth #104 established rather than
  duplicating schema logic.
- **The schema is computed server-side and embedded in the descriptor JSON.** The endpoint carries
  it; the CLI consumes it. So `DescriptorView` (server) and `CommandInfo` (client) each gain a
  `schema` field (a JSON object). `inputType` (simple name) is kept for humans.
- **Typed subcommands are registered at runtime from the fetched catalog.** No build-time codegen.
  Offline / server-unreachable: register none; `run` + read subcommands + built-ins still work, and
  `--help` reflects only what is available. (Mirrors the #70 M4 decision.)
- **Generated subcommands are named in kebab-case** derived from the command type
  (`EditGoal` → `edit-goal`), matching the existing CLI subcommand style (`upsert-goal`,
  `open-issues`). The canonical form stays available as `run EditGoal`.
- **Lockstep guard.** A test asserts the CLI's generated flags for a command are exactly the schema's
  property names (CLI flags == schema properties), analogous to #104's `MCP tools ⊆ catalog`.

## Server-side changes (`gateway-api`, `service-impl`, `mcp-server`)

1. **`gateway-api`:** add `CommandInputSchema.of(Class<?>)` (extracted generator). Optionally expose a
   convenience on `CommandDescriptor` (e.g. `Map<String,Object> inputSchema()` delegating to it) so
   callers don't re-derive it.
2. **`mcp-server`:** replace `McpWriteService`'s private `schemaFor`/`jsonType`/`isRequired`/… with
   calls to `CommandInputSchema`. Delete the now-dead privates. No behavior change; keep
   `McpWriteServiceSchemaTest` / `McpWriteCatalogLockstepTest` / `McpToolCatalogLockstepIT` green.
3. **`service-impl`:** add `schema` to `GatewayCommandController.DescriptorView` (populated from
   `CommandInputSchema.of(d.inputType())`); update the controller/catalog tests
   (`GatewayCommandCatalogImplTest` and the controller MockMvc test) to assert the schema is present
   and its `required` array matches the DTO's `@NotNull`/`@NotBlank` fields.

## Client-side changes (`gateway-rest-client`, `requel-cli`)

4. **`gateway-rest-client`:** add `schema` (e.g. `Map<String,Object>`) to `CommandInfo` so
   `RestGatewayCatalog.descriptors()` deserializes it.
5. **`requel-cli`:** dynamic typed-subcommand registration.
   - **Two-phase startup in `main()`:** resolve `--url` / `--token` (flags → `REQUEL_URL` /
     `REQUEL_TOKEN` env → stored creds) enough to fetch the catalog; build the `CommandLine`, then for
     each **write** descriptor `addSubcommand(kebab(commandType), buildTypedSubcommand(descriptor))`;
     finally `execute(args)`. If the fetch fails (offline / auth / writes-disabled → empty list),
     register nothing and proceed.
   - **Generated subcommand** (a small `CommandSpec` built programmatically, or a reusable
     `Callable<Integer>` holder configured from the descriptor): one `--flag` per schema property,
     `--kebab-case` of the field name; type mapped from the schema (`string`→String, `integer`→
     Integer/Long, `number`→Double, `boolean`→Boolean; `array`/`object`→raw JSON string, documented);
     `required(true)` when the field is in the schema's `required` array. `call()` assembles the input
     map (parsing raw-JSON flags with Jackson), dispatches via `RestCommandGateway`
     (`parent.url` + `parent.tokenSource()`), and reuses `parent.printResult` / `printError` /
     `ExitCode.forKind`.
   - Keep `run`, `commands`, the read subcommands, `login`/`logout`, and the bespoke `upsert-goal`
     unchanged.

## Milestones (incremental, each independently testable)

1. **Extract + refactor.** `CommandInputSchema` in `gateway-api`; `McpWriteService` delegates to it.
   `mvn clean verify` green with existing MCP tests unchanged. *(No user-visible change.)*
2. **Endpoint carries schema.** `DescriptorView.schema` + `CommandInfo.schema`; controller/catalog
   tests assert schema + `required`. `requel commands --output json` now shows schemas.
3. **Typed subcommands.** Runtime registration + generated dispatch; `requel edit-goal --project …`
   works against a live server.
4. **Offline fallback + docs.** No typed subcommands when unreachable; `--help` correct. Update
   `doc/70-requel-cli-plan.md` (mark the deferred item done), `doc/70-requel-cli-verification.md`
   runbook, and the README/CLI docs with typed-subcommand examples.

## Testing strategy

- **Unit — schema generator (`gateway-api`):** record → schema (types, `additionalProperties:false`);
  `required` from `@NotNull`/`@NotBlank` on component or accessor; primitives, boxed, `char`, enums
  (→ string), collections/arrays (→ array), nested record (→ object); `Void`/non-record → empty
  object. (Port the intent of `McpWriteServiceSchemaTest`.)
- **Unit — endpoint:** `descriptors()` includes a correct schema per command; empty list when writes
  disabled.
- **Unit — CLI generation:** mirror `CommandsCommandTest` / `UpsertGoalCommandTest` with a stubbed
  catalog + gateway: assert a descriptor produces the expected flags (names, types, required), that
  invoking a generated subcommand sends the expected input map to the gateway, and the
  flags-==-schema-properties lockstep. Offline: fetch failure → only built-ins registered, `run`
  still works.
- **Integration:** `@SpringBootTest(webEnvironment = RANDOM_PORT)` driving CLI `main()` (PAT auth): a
  generated typed **create** then **edit** on a project the test user can edit, and a **denied** write
  → assert output + exit codes; assert typed subcommands appear when
  `requel.gateway.write.enabled=true` and are absent when `false`.

## Sub-decisions (resolved during implementation)

- **Nested object/array fields → raw JSON (as recommended).** Array/object schema types map to a
  `String` option, parsed with Jackson at dispatch (invalid JSON → usage error, exit 2). Documented in
  the flag description, which appends `" (array as raw JSON)"` / `"(object as raw JSON)"` so it shows
  in `--help`. Dotted sub-flags remain a later enhancement.
- **`Long` for the `integer` schema type.** `javaType("integer")` returns `Long.class` (headroom for
  ids). The "derive from the DTO component's actual type" alternative was **not** taken: the wire
  schema only carries `"integer"` (not `Integer`/`Long`/`Short`), and the CLI has no access to the DTO
  class — surfacing the precise numeric type would widen the schema for little gain. `Long` is safe
  because Jackson narrows it to the target DTO field on the server when binding.
- **Subcommand help text: `title` + `description`; `authorizationHint` omitted.** `describe()` builds
  the usage from `title` (falling back to `commandType`) plus `description` when present.
  `authorizationHint` is deliberately not surfaced in `--help` — it is an informational permission
  hint, not enforcement, and would clutter the usage. Trivial to add later if wanted.
- **Startup cost: gate the fetch; no cache.** `probe(...)`/`wantsTypedSubcommand(...)` skips the
  catalog round-trip for built-in subcommands (`run`, `login`, `logout`, reads, `commands`),
  `--help`/`--version`, and empty invocations, fetching only when the args look like a typed
  candidate. The on-disk URL-keyed cache was **not** added — the gating already removes the round-trip
  from the common paths; revisit if typed invocations feel slow.

## Out of scope

- Build-time static generation of subcommands (still deferred; only matters for offline typed help).
- Any change to the MCP tool surface (done in #104) beyond the internal delegation refactor.
- Remote MCP connector ops (#100) and the persistent AS signing key (#105).
