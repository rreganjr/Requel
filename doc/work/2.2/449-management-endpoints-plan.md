# #449 — Management endpoint configuration

https://github.com/rreganjr/Requel/issues/449

Branch `449-management-endpoints` from `release/2.2`, PR to `release/2.2`. Size 3 (1 in the
issue; raised at review to cover filling in `info` and a server-info read for MCP and the CLI).

## Summary

`application.properties` exposes the Actuator endpoints `health`, `info`, `metrics` and
`prometheus`, and no security chain matches `/actuator/**`. Requel has four chains (`/oauth2` and
`/connect`, the OAuth login and consent pages, `/api/mcp` and `/api/gateway`, and `/api/**`), and
Spring Boot's default chain backs off once custom chains exist. So `/actuator/metrics` and
`/actuator/info` answer anyone, and any route outside those four chains passes through no
security filter at all.

This adds an Actuator chain (health open, the rest for a system admin), a last-resort fallback
chain, fills in `info` so an admin can see what an install is running, and drops `prometheus`,
which was listed without its registry and so never existed. It also adds a `getServerInfo`
gateway read, so MCP clients and the CLI (which can't reach `/actuator/**` with their OAuth
tokens) can tell which version of Requel they're talking to.

## What exists

- `management.endpoints.web.exposure.include=health,info,metrics,prometheus`
  (`application.properties`). `micrometer-registry-prometheus` is not a dependency.
- `/actuator/health` is polled anonymously by the e2e job in `ci.yml` and the smoke test in
  `container-publish.yml`. (The advisory says the Compose health check polls it too; the Compose
  `web` service has no health check.)
- Nothing reads `info` or `metrics`: not the Angular app, the CLI, the docs or CI. `info` returns
  `{}`: no `build-info.properties` or `git.properties` is generated.
- `SpaController` forwards extensionless paths outside `/api/` and `/actuator/` to `index.html`.
- Every POST, PUT and DELETE endpoint is under `/api/**` or the OAuth chains' paths.
- `JwtAuthenticationFilter` (on the `/api/**` chain) authenticates a login JWT or a personal
  access token and grants `ROLE_<role>` authorities loaded live from the account. It does not
  accept the authorization server's OAuth tokens, which MCP clients and `requel login` use on
  `/api/mcp/**` and `/api/gateway/**`.
- No API reports the server's version. The MCP handshake carries it (`serverInfo.version`), but
  that reaches the client application, not the model; `requel --version` prints only the CLI's
  own version.

## Locked decisions (2026-11-18)

1. **Exposed endpoints:** `health`, `info`, `metrics`. `prometheus` is removed from the list.
2. **`health` is anonymous.** Its details are shown to a system admin only
   (`management.endpoint.health.show-details=when-authorized`,
   `management.endpoint.health.roles=SystemAdminUserRole`); anyone else sees
   `{"status":"UP"}`.
3. **`info` and `metrics` (and any endpoint an operator adds) need `SystemAdminUserRole`**,
   authenticated by the same JWT or personal access token as `/api/admin/**`. No separate
   monitoring user.
4. **`info` is filled in:** build (version, artifact, build time) from Spring Boot's
   `build-info` goal; git (branch, short commit id, commit time) from
   `git-commit-id-maven-plugin`; Java and OS from `management.info.java.enabled` and
   `management.info.os.enabled`.
5. **A fallback chain** last in order, for anything no other chain matches:
   - forward and error dispatches allowed (the SPA's forward to `index.html`, and `/error` after
     a failed request on another chain, so that request's real error body still comes back);
   - GET and HEAD allowed (the SPA's pages and static files);
   - every other method refused with 403.
6. **A `getServerInfo` gateway read**, open to any signed-in caller: version, git commit, commit
   time and build time, from the same build and git data as `/actuator/info`. Java and OS details
   stay admin-only on `/actuator/info`. Exposed as an MCP tool `getServerInfo` and a CLI command
   `requel server-info`; `requel --version` is unchanged.

## Contracts

| Request | Anonymous | Signed-in non-admin | System admin |
|---|---|---|---|
| `GET /actuator/health` | 200 `{"status":"UP"}` | 200, no details | 200 with components |
| `GET /actuator/info` | 401 | 403 | 200 with build, git, java, os |
| `GET /actuator/metrics`, `/actuator/metrics/{name}` | 401 | 403 | 200 |
| `GET /actuator/prometheus` | 404 (not exposed) | 404 | 404 |
| `GET /` and SPA routes, static files | 200 | 200 | 200 |
| `POST`/`PUT`/`DELETE` to a path no chain matches | 403 | 403 | 403 |
| `GET /api/gateway/query/server-info` (MCP `getServerInfo`, `requel server-info`) | 401 | 200 | 200 |

`ServerInfoDto`: `version`, `commit` (short id), `commitTime`, `buildTime`. A field the build
didn't produce (no `.git` at build time, or a run from an IDE without `build-info`) is null,
except `version`, which falls back to the jar's `Implementation-Version` and then `"unknown"`.

## Steps

1. **`application.properties`:** exposure list becomes `health,info,metrics`; add
   `management.endpoint.health.show-details=when-authorized`,
   `management.endpoint.health.roles=SystemAdminUserRole`,
   `management.info.java.enabled=true`, `management.info.os.enabled=true`,
   `management.info.git.mode=simple`. A comment says how to add Prometheus later (the
   `micrometer-registry-prometheus` dependency, `prometheus` in the list, and a scraper sending
   an admin personal access token as a bearer token).
2. **`modules/requel-app/pom.xml`:**
   - `spring-boot-maven-plugin`: add the `build-info` execution.
   - `git-commit-id-maven-plugin` (version managed by the Spring Boot parent):
     `generateGitPropertiesFile=true`, `failOnNoGitDirectory=false` (a source build without
     `.git` still works, with no git section), `includeOnlyProperties` for branch, short commit
     id and commit time.
3. **Actuator chain** in `ApiSecurityConfig`, `@Order(5)`:
   `securityMatcher(EndpointRequest.toAnyEndpoint())`; `EndpointRequest.to(HealthEndpoint.class)`
   permitted; everything else `hasRole("SystemAdminUserRole")`; stateless, CSRF off, the same
   `JwtAuthenticationFilter` as the `/api/**` chain; 401 entry point. The filter construction is
   shared with the `/api/**` chain rather than duplicated. No `PasswordChangeRequiredFilter`
   (#381): a caller who must change their password holds only that authority, so the role check
   already refuses them, and health stays reachable for them.
4. **Fallback chain**, `@Order(Ordered.LOWEST_PRECEDENCE)`, its own small config class:
   `dispatcherTypeMatchers(FORWARD, ERROR).permitAll()`, GET and HEAD `permitAll()`,
   `anyRequest().denyAll()`; stateless, CSRF off.
5. **`AuthorizationServerConfig` javadoc:** the chain-layering list gains orders 5 and last.
6. **Server info read:**
   - `ServerInfoDto` record in `service-api`.
   - `QueryGateway.getServerInfo()` in `gateway-api`, with a caller-facing description in
     `QueryDescriptions` (#296).
   - `InProcessQueryGateway`: built from Spring Boot's optional `BuildProperties` and
     `GitProperties` beans (`ObjectProvider`, so a missing file just means null fields).
   - `GatewayQueryController`: `GET /api/gateway/query/server-info`.
   - `RestQueryGateway` (`gateway-rest-client`): the matching call.
   - `McpReadService`: tool `getServerInfo`, no arguments.
   - `requel-cli`: `ServerInfoCommand` (`requel server-info`, text and JSON output like the other
     reads), registered on `RequelCli`.
7. **`INSTALL.md`:** a short "Monitoring" note: health is open; info and metrics need an admin
   login or personal access token; how to add Prometheus; `requel server-info` and the MCP tool
   for the version.

## Test plan

- **`ManagementEndpointSecurityIT`** (MockMvc, test profile): every row of the contracts table
  for health, info, metrics and prometheus: anonymous, a non-admin user, an admin JWT, and an
  admin personal access token. Health details appear only for the admin. `info` contains `build`,
  `java` and `os` (`git` when the test runs from a checkout).
- **`FallbackSecurityIT`** (`RANDOM_PORT`, real HTTP so forward and error dispatches happen):
  - `GET /` and `GET /projects/x` return the SPA's `index.html`;
  - a static asset returns 200;
  - `POST /not-an-api-route` returns 403;
  - a failing `/api` POST (e.g. a validation error) still returns its own status and body, not a
    403 from the error dispatch.
- **Server info:** a gateway IT (`/api/gateway/query/server-info`: 401 anonymous, 200 for a
  non-admin, version equal to the build's), the MCP tool through the existing MCP test harness,
  `RestQueryGateway` against the endpoint, and a `requel-cli` test for `server-info` text and
  JSON output (as `ProjectsCommand` is tested). The tool and command appear in the MCP tool list
  and CLI help.
- **Existing suites:** `AuthorizationIT`, `ApiTokenIT`, the OAuth and MCP ITs, and the e2e run in
  CI (which polls health anonymously) pass unchanged.
- Verify gate: `mvn clean verify` (backend only; no Angular change).

## Acceptance criteria → where

| AC (advisory) | Covered by |
|---|---|
| Metrics and other non-public endpoints need an authorized identity | Decision 3, step 3, `ManagementEndpointSecurityIT` |
| Anonymous access denied except intended health | Decisions 1–2, `ManagementEndpointSecurityIT` |
| Explicit fallback protecting unmatched routes, SPA/static/OAuth intact | Decision 5, step 4, `FallbackSecurityIT`, existing OAuth ITs |
| Tests for anonymous and authorized monitoring | Both new ITs |
| (added at review) Version visible to MCP and the CLI | Decision 6, step 6, server info tests |

## Out of scope

- A Prometheus registry or a scrape config.
- A separate management port or a dedicated monitoring user.
- A health check on the Compose `web` service.

## Risks

- **Health roles matching.** Spring Boot checks `management.endpoint.health.roles` through the
  request's `isUserInRole`, which adds the `ROLE_` prefix. The admin-sees-details test confirms
  that `SystemAdminUserRole` matches `ROLE_SystemAdminUserRole`.
- **Error dispatch.** Without the explicit `ERROR` permit, the fallback's `denyAll` would turn
  any failed non-GET request's error page into a 403. `FallbackSecurityIT` covers it.
- **OAuth metadata.** `/.well-known/...` GETs not claimed by the authorization-server chain fall
  to the fallback and stay open as GETs; the existing OAuth/MCP ITs confirm discovery still works.
- **Build time in `build-info`** makes each jar build differ by timestamp. Acceptable; nothing
  depends on byte-identical jars.
