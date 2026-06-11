# Upgrade Spring Boot 3.3.4 → 3.5.14 (baseline for Spring AI 1.1.7)

Prerequisite for the AI/MCP work (#69 series + the Spring AI provider-client port). Branch off
`release/2.0`. Background: `doc/port-tospring-boot-ai.md` (and its codex review).

## Goal

Bump the project from Spring Boot **3.3.4** to **3.5.14** (Java stays **17**), and establish the
Spring AI **1.1.7** dependency baseline, so the AI provider-client port and the MCP transport
migration can build on a supported combination. Spring AI 1.x requires Boot 3.4.x/3.5.x, so this
upgrade is a hard prerequisite — Spring AI is **not** a no-upgrade drop-in on 3.3.4.

This ticket is the version/foundation bump only. It does **not** add Spring AI usage; it just makes
the platform ready and pins the BOM. The actual `ChatClient`/MCP adoption lands in the follow-on
tickets.

## Scope

In scope:

- Bump `spring-boot-starter-parent` to `3.5.14` in the root `pom.xml`; keep `java.version=17`.
- Add the Spring AI BOM pinned to `1.1.7` to dependency management (no starters wired/used yet, or
  add `noop`-only wiring so nothing activates).
- Resolve breaking changes / deprecations introduced between Boot 3.3 → 3.4 → 3.5 (property
  renames, autoconfiguration changes, Hibernate 6.x / Flyway / Jackson / Spring Security minor
  bumps that ride along with the parent).
- Verify the existing stack still works on 3.5.14: Flyway migrations, Hibernate (MySQL prod / H2
  test), the JWT security chain, SSE, and the full test suite (including the per-context H2
  isolation fix from the #43 branch).
- Confirm `mvn clean verify` is green and the Docker image still builds.

Out of scope:

- Any Spring AI `ChatClient` adoption or MCP transport migration (follow-on tickets).
- Spring Boot 4 / Spring AI 2.x.

## Acceptance Criteria

- Root POM on Spring Boot `3.5.14`, Java `17`; Spring AI BOM resolvable at `1.1.7`.
- `mvn clean verify` passes across all modules on a clean environment.
- Docker image builds (`-Pdocker-image`).
- No behavioral regressions in auth, persistence, Flyway, or SSE; `AuthorizationIT` and the
  integration tests pass.
- A short upgrade note in `doc/` listing any property/API changes made.

## Dependencies / sequencing

- **Branch off `release/2.0`.** Merge before resuming the AI/MCP tickets.
- **Blocks:** the Spring AI provider-client port (`doc/issue_spring_ai_provider_clients.md`) and
  #69 Slices 4/6/7 (MCP transport on Spring AI), all of which assume Boot 3.5.14 + Spring AI 1.1.7.

## Notes

- Spring AI `1.1.7` (not `1.0.8`) is the target because the MCP identity/security APIs the AI/MCP
  plan relies on (`@McpTool`, `McpTransportContext`, the MCP security module) are 1.1 behavior.
- Spring AI `2.0.x` requires Boot 4.0.x/4.1.x — explicitly out of scope.
