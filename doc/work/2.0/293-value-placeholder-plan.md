# #293 `@Value` placeholders ignore `@TestPropertySource` and `@DynamicPropertySource` — implementation plan

## Summary

The ticket describes the behaviour; the cause is one line of legacy XML. `application-config.xml`
declares `<context:property-placeholder ... system-properties-mode="OVERRIDE" ...>`, and an explicit
`system-properties-mode` makes Spring register the deprecated `PropertyPlaceholderConfigurer`, which
never reads the `Environment`. Deleting the element hands `@Value` to Boot's
`PropertySourcesPlaceholderConfigurer`. The defect reaches production as well as tests, which is why
the ticket stays on v2.0.

## Review against the tree (`release/2.0` @ `caa8c45a`)

**The cause (AC 1).** `modules/requel-app/src/main/resources/application-config.xml:9`, loaded by
`@ImportResource("classpath:application-config.xml")` on `Application`:

```xml
<context:property-placeholder location="classpath*:*.properties" system-properties-mode="OVERRIDE"
    ignore-unresolvable="false" local-override="true" />
```

`PropertyPlaceholderBeanDefinitionParser.getBeanClass` (spring-context 6.2) returns
`PropertySourcesPlaceholderConfigurer` only when `system-properties-mode` is the default
`ENVIRONMENT`; any explicit value "reverts to PropertyPlaceholderConfigurer to ensure backward
compatibility with 3.0 and earlier". That class resolves a placeholder from:

1. the properties it loaded itself — every `*.properties` at the root of every classpath entry,
   later files overriding earlier ones (`local-override="true"`);
2. `System.getProperty(key)`, then `System.getenv(key)` with the key spelled exactly as written.

It never consults the `Environment`, so `@TestPropertySource`, `@DynamicPropertySource`,
command-line arguments, `SPRING_APPLICATION_JSON` and relaxed-name environment variables are all
invisible to it. It also resolves `${key:default}` itself, so Boot's own
`PropertySourcesPlaceholderConfigurer` — still registered, because the XML bean is not one — never
sees an unresolved placeholder. The ticket looked for a `@Bean`; the configurer is declared in XML.

**Why the probe saw what it saw.** Test-classes precede main classes on the test classpath, so
`application-test.properties` and `db.properties` load first and main's `application.properties`
overrides them: `requel.jwt.expiry-hours` came back `8`. Keys the files do not define fall to the
placeholder default: `requel.probe.unrelated-key`, `requel.dictionary.sql-files`.

**Production impact the ticket does not list.**

- *Every profile's file is loaded in every run.* `classpath*:*.properties` picks up
  `application-dev.properties` and each `application-ai-*.properties` whatever the active profile.
  `spring.cors.allowed-origins=http://localhost:4200` exists only in the dev file, so
  `ApiSecurityConfig` allows that origin on `/api/**` in production — its javadoc says "Empty in
  production".
- *Operators cannot override a `@Value` key the normal way.* `--requel.gateway.write.enabled=false`
  and `REQUEL_GATEWAY_WRITE_ENABLED=false` are both ignored; only `-D` works. The read-only switch
  that `application.properties:109-112` documents cannot be thrown from docker-compose.
  `SPRING_CORS_ALLOWED_ORIGINS` is likewise ignored, although `ApiSecurityConfig`'s javadoc offers
  it. `requel.jwt.secret` works only because `application.properties:91` nests
  `${REQUEL_JWT_SECRET:…}`, which this configurer resolves by exact environment name.

**Consumers.** The imported `spring/*.xml` files contain no `${…}`. The only consumers are eight
`@Value` sites on seven keys (the ticket says nine):

| Key | Site(s) | Where the value lives |
|---|---|---|
| `requel.gateway.write.enabled` | `McpWriteService`, `GatewayCommandController` | `application.properties:113` |
| `requel.jwt.secret`, `requel.jwt.expiry-hours` | `JwtService` | `application.properties:91-92` |
| `spring.cors.allowed-origins` | `ApiSecurityConfig` | `application-dev.properties:3` only |
| `requel.audit.sensitive-keys` | `SensitiveFieldRedactor` | default only |
| `requel.admin.password` | `DevAdminResetController` | default only |
| `requel.project.password` | `DevProjectResetController` | default only |

No `@Value` key is set in `application-test.properties`, so the test suite's values do not move.

**The MCP ITs.** `McpToolCatalogLockstepIT` and `RequelMcpEndToEndIT` share one cached context
(identical inlined properties on the same base class). Lockstep asserts `isWriteEnabled()`; the
end-to-end IT does not, and its javadoc claims an independence from the shipped default that it
does not have.

**Dead config.** `modules/requel-app/src/main/resources/spring/testConfig.xml` is imported by
nothing, yet declares a second `<context:property-placeholder location="db.properties"/>`.

## Locked decisions

1. **Delete the element**, rather than dropping `system-properties-mode`. Without that attribute
   the parser would register a `PropertySourcesPlaceholderConfigurer`, but
   `location="classpath*:*.properties"` with `local-override="true"` would still let every root
   properties file — every profile's — override the `Environment`. Boot's auto-configured
   configurer already covers what the element was for.
2. **`localhost:4200` becomes dev-only.** It is a CORS origin only with the dev profile, as
   `ApiSecurityConfig` documents. CLAUDE.md already requires `--spring.profiles.active=dev` locally,
   and `docker-compose.e2e-coverage.yml` passes it.
3. **No house rule against `@Value`.** AC 2's fallback is not needed once the cause is fixed.
   `DictionarySQLInitializer` keeps reading the `Environment`; only its comment changes.
4. **Regression: a new IT plus a guard**, at the cost of one extra Spring context in CI.
5. **Fold in** deleting `spring/testConfig.xml`.
6. **Milestone** stays v2.0.
7. **Fold in gating REST dispatch on the write flag.** Found while planning:
   `GatewayCommandController.dispatch` ran any command whatever `requel.gateway.write.enabled`
   said; only `descriptors()` checked it, while `McpWriteService.call` refuses. With this ticket
   making the flag settable by operators, a read-only deployment would still take REST writes.
   `dispatch` now refuses with `NOT_ALLOWED` (403), matching the `GatewayErrorBody` envelope
   clients already parse.

## Behaviour contract

After this change a `@Value("${k:d}")` resolves `k` exactly as `Environment.getProperty("k")` does,
falling back to `d`:

- test-supplied properties (`@TestPropertySource`, `@DynamicPropertySource`) win over the
  application files;
- command-line arguments and environment variables, including relaxed names
  (`REQUEL_GATEWAY_WRITE_ENABLED`), win over the application files;
- a profile-specific file contributes only when its profile is active.

## Steps

1. `application-config.xml` — delete line 9, the `<context:property-placeholder>` element. The
   `context` namespace stays: `<context:component-scan>` still uses it.
2. Delete `modules/requel-app/src/main/resources/spring/testConfig.xml`.
3. `ApiSecurityConfig` — correct both comments on `additionalAllowedOrigins` and
   `corsConfigurationSource()`: the dev profile's file or `SPRING_CORS_ALLOWED_ORIGINS` supplies the
   list, and it is empty otherwise. Both statements become true with step 1.
4. `RequelMcpEndToEndIT` — add the `assertThat(writeService.isWriteEnabled())` check lockstep
   already has (inject `McpWriteService`), so the IT fails rather than drifting to the read-only
   path. Its javadoc's independence claim is now true and stays; add a pointer to the regression IT
   below.
5. `DictionarySQLInitializer` constructor javadoc — say that `@Value` did not see test properties
   until #293 removed the legacy XML placeholder configurer; the class reads the `Environment`
   directly, which remains correct.
6. New `modules/requel-app/src/test/java/com/rreganjr/requel/PropertyPlaceholderResolutionIT.java`
   (test plan below).
7. `GatewayCommandController.dispatch` — refuse with `NOT_ALLOWED` when writes are disabled
   (locked decision 7); unit test `dispatchIsRefusedWhenWritesDisabled` with a gateway that fails if
   called.
8. `tmp/293-verify.sh` — the gate.

## Test plan

**`PropertyPlaceholderResolutionIT extends AbstractIntegrationTestCase`**, with
`@TestPropertySource(properties = "requel.gateway.write.enabled=false")`. It inherits the base
class's `test` profile and AI properties, and gets its own context because its inlined properties
differ from every other IT's.

- **Test property reaches `@Value` (AC 3, AC 4).** `McpWriteService.isWriteEnabled()` is `false`
  and `toolDescriptors()` is empty; `GatewayCommandController.descriptors()` is empty. Today both
  read `true` from `application.properties:113`, so these fail before step 1 and pass after it.
- **REST gateway refuses writes.** `dispatch` returns 403 when the flag is off.
- **Profile isolation.** The `CorsConfigurationSource` bean's configuration for a `/api/**`
  request has no allowed origins, since the dev profile is not active. Today it holds
  `http://localhost:4200`.
- **Guard.** Among the context's `PlaceholderConfigurerSupport` beans, every one is a
  `PropertySourcesPlaceholderConfigurer`. If anyone re-adds an XML placeholder with a
  `system-properties-mode`, this fails naming the class.

**Existing suites.** `McpToolCatalogLockstepIT` and `RequelMcpEndToEndIT` now take
`write.enabled=true` from their own pin. Every other IT still resolves `true` from
`application.properties`, so nothing else moves.

**Gate.** `mvn clean verify` green. No frontend change, so no vitest, tsc or e2e run is required.

```bash
# tmp/293-verify.sh
mvn clean verify 2>&1 | tee tmp/293-verify.log
grep -q "Tests run:.*PropertyPlaceholderResolutionIT" tmp/293-verify.log
```

## Out of scope

- **`DatabaseCreationListener`.** Also legacy and `db.properties`-based, but
  `DocumentationInitializationListener` borrows its class for a logger, so removing it touches
  unrelated code.
- **The redundant `<context:component-scan base-package="com.rreganjr"/>`**, which duplicates
  `@SpringBootApplication`'s scan and keeps `@WebMvcTest` slices from working
  (`CommandControllerTest`'s note).
- **Aligning the two JWT dev-secret defaults**: `application.properties:91` ends `…-min-32-chars`,
  `JwtService` ends `…-min-32-chars!!`. Only the properties value is ever used.

## Risks

- **An undiscovered consumer of the XML configurer's file pile**: a bean relying on a key that only
  a non-active profile's file defines. Only the CORS key was found, by searching every `@Value` and
  every `${` in main XML. A miss surfaces as a startup failure (the configurer does not ignore
  unresolvable placeholders), not as a silent default.
- **Local habit.** Anyone serving `ng serve` against a jar started without the dev profile loses
  CORS for :4200. CLAUDE.md already forbids that; the commit message will say it.
- **CI time.** The new IT adds one Spring context start.

## AC mapping

The ACs as revised on the issue on 2026-09-25.

| AC | Covered by |
|---|---|
| 1. Cause recorded | Review above; the issue comment; the commit message |
| 2. Element removed; `@Value` sees the `Environment` | Step 1; locked decisions 1 and 3 |
| 3. A test pins it | `PropertyPlaceholderResolutionIT`: test override, profile isolation, bean-type guard |
| 4. Both MCP ITs assert `isWriteEnabled()`; javadoc true | Step 1 makes the pin effective; step 4 |
| 5. `localhost:4200` dev-only; `ApiSecurityConfig` comments | Step 1; step 3; `nonActiveProfileFileDoesNotReachValueInjectedBeans` |
| 6. `spring/testConfig.xml` deleted | Step 2 |
| — REST dispatch honours the write flag (folded in) | Step 7; `PropertyPlaceholderResolutionIT`, `GatewayCommandControllerTest` |
