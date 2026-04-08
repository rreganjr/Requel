# Review of `RELEASE_20_TEST_PLAN.md`

## Overall assessment

The plan is directionally strong: it correctly prioritizes JUnit 5 migration, Java command tests,
Angular unit coverage, and Playwright for browser automation. It is not yet complete enough for a
release test plan, though, because parts of the proposed test matrix do not match the current
implementation and several shipped Angular/Java features are outside the plan.

## Findings

### 1. High: the plan does not cover the full implemented Angular and API surface

The Angular sections focus on goals, stories, actors, use cases, scenarios, stakeholders, and open
issues, but the current app also ships routes for account editing, settings, terms, reports, and
admin user pages. Those routes are present in `requel-angular/src/app/app.routes.ts:35-58`, but
the plan only proposes editor/list specs for a subset of them in
`doc/RELEASE_20_TEST_PLAN.md:206-267`.

The same gap exists in the service layer. The plan calls out service specs for auth, commands,
projects, goals, stories, actors, use cases, scenarios, permissions, and event streaming in
`doc/RELEASE_20_TEST_PLAN.md:206-225`, but there are active services for users
(`requel-angular/src/app/core/user.service.ts:31-58`), preferences
(`requel-angular/src/app/core/preferences.service.ts:27-58`), annotations
(`requel-angular/src/app/core/annotation.service.ts:27-75`), stakeholders
(`requel-angular/src/app/core/stakeholder.service.ts`), terms
(`requel-angular/src/app/core/term.service.ts:28-46`), and reports
(`requel-angular/src/app/core/report.service.ts:29-73`) that are not in scope.

On the Java side, `ProjectQueryController` exposes terms and reports endpoints
(`modules/service-impl/src/main/java/com/rreganjr/requel/service/query/ProjectQueryController.java:795-907`)
plus stakeholder permissions and tree endpoints
(`modules/service-impl/src/main/java/com/rreganjr/requel/service/query/ProjectQueryController.java:145-214`),
but `doc/RELEASE_20_TEST_PLAN.md:127-132` does not explicitly include them in the REST coverage.

This matters because the document claims to be a comprehensive release plan. As written, it leaves
implemented release scope untested.

### 2. High: multiple planned tests target interfaces that do not exist as written

Several entries in the plan are aimed at APIs or component contracts that are different in the
actual codebase:

- `AuthControllerTest` includes `POST /api/auth/logout` in
  `doc/RELEASE_20_TEST_PLAN.md:130`, but the controller only exposes `POST /api/auth/login` and
  `GET /api/auth/me`
  (`modules/service-impl/src/main/java/com/rreganjr/requel/service/auth/AuthController.java:58-88`).
- `EventStreamControllerTest` only covers the initial `GET /api/events/stream` call in
  `doc/RELEASE_20_TEST_PLAN.md:131`, but the real controller also includes `POST` and `DELETE`
  subscription endpoints and `DELETE /connection`
  (`modules/service-impl/src/main/java/com/rreganjr/requel/service/stream/StreamController.java:46-101`).
- The stakeholder command matrix names `DeleteUserStakeholder` and `DeleteNonUserStakeholder` in
  `doc/RELEASE_20_TEST_PLAN.md:109`, but the registered command is `DeleteStakeholder`
  (`modules/service-impl/src/main/java/com/rreganjr/requel/service/command/ProjectCommandRegistrar.java:200-248`).
- `auth.service.spec.ts` calls for `isLoggedIn()` and token-expiry behavior in
  `doc/RELEASE_20_TEST_PLAN.md:213`, but `AuthService` exposes `isAuthenticated` and does not
  parse JWT expiry
  (`requel-angular/src/app/core/auth.service.ts:31-75`).
- `project.service.spec.ts` calls for cache invalidation in
  `doc/RELEASE_20_TEST_PLAN.md:215`, but `ProjectService` currently has an event subject, not a
  query cache (`requel-angular/src/app/core/project.service.ts:33-83`).
- `scenario-selector-dialog.spec.ts` calls for `[includeTypes]` coverage in
  `doc/RELEASE_20_TEST_PLAN.md:235`, but `ScenarioSelectorDialogComponent` only accepts
  `visible`, `projectName`, and `excludeIds`
  (`requel-angular/src/app/shared/scenario-selector-dialog.ts:119-195`).

These mismatches should be corrected before test work starts; otherwise the plan will send effort
into non-existent endpoints and behaviors.

### 3. High: the Java test matrix omits release-critical command and contract paths that the Angular app already uses

The command section in `doc/RELEASE_20_TEST_PLAN.md:102-117` omits glossary and report generator
commands even though both are registered API commands:

- `EditGlossaryTerm` / `DeleteGlossaryTerm`
- `EditReportGenerator` / `DeleteReportGenerator`

Those registrations are in
`modules/service-impl/src/main/java/com/rreganjr/requel/service/command/ProjectCommandRegistrar.java:620-677`.

The REST section also misses contract tests for:

- `GET /api/annotations`
  (`modules/service-impl/src/main/java/com/rreganjr/requel/service/query/AnnotationQueryController.java:60-93`)
- `GET /api/users/organizations` and `GET /api/users/roles`
  (`modules/service-impl/src/main/java/com/rreganjr/requel/service/query/UserQueryController.java:77-109`)
- `GET /api/auth/me`
  (`modules/service-impl/src/main/java/com/rreganjr/requel/service/auth/AuthController.java:81-88`)
- `GET /api/projects/{name}/tree`
- `GET /api/projects/stakeholder-permissions`
- `GET /api/projects/{name}/terms`
- `GET /api/projects/{name}/reports`
- `GET /api/projects/{name}/reports/{reportId}/run`

Those project endpoints are present in
`modules/service-impl/src/main/java/com/rreganjr/requel/service/query/ProjectQueryController.java:145-214`
and
`modules/service-impl/src/main/java/com/rreganjr/requel/service/query/ProjectQueryController.java:795-907`.

Because the Angular app already depends on these endpoints and commands, they belong in the
release-plan contract suite, not as optional follow-up work.

### 4. Medium: the E2E section is not complete for the features currently exposed in the UI

The Playwright section covers the major entity workflows, but it does not include end-to-end flows
for:

- glossary term create/edit/delete
- report create/edit/run/download
- settings/preferences save and reload
- account self-edit flow
- admin user list/edit flow
- sidebar import path and project tree refresh behavior
- explicit forbidden-state UX for 401/403 handling outside login

Those are all reachable from the current route table in
`requel-angular/src/app/app.routes.ts:35-58` and from the corresponding Angular services in
`requel-angular/src/app/core/user.service.ts:31-58`,
`requel-angular/src/app/core/preferences.service.ts:27-58`,
`requel-angular/src/app/core/term.service.ts:28-46`, and
`requel-angular/src/app/core/report.service.ts:29-73`.

For a release plan, the current E2E matrix in `doc/RELEASE_20_TEST_PLAN.md:327-419` is a good
start, but it is still centered on the core requirements entities rather than the full shipped UI.

### 5. Medium: the E2E environment instructions are internally inconsistent

The sample Playwright config uses `baseURL: 'http://localhost:8081'` and the run instructions say
the backend must be on `:8081` (`doc/RELEASE_20_TEST_PLAN.md:316` and `doc/RELEASE_20_TEST_PLAN.md:482`),
but the CI health check waits on `http://localhost:8080/actuator/health`
(`doc/RELEASE_20_TEST_PLAN.md:429-443`).

That can be correct only if the document is distinguishing between dev mode and packaged mode, but
it does not say so. The Angular app itself uses same-origin `/api` in both environments
(`requel-angular/src/environments/environment.ts:21-24`,
`requel-angular/src/environments/environment.prod.ts:21-24`) and only the Angular dev proxy points
to `8081` (`requel-angular/proxy.conf.json:1-6`).

The plan should make one of these execution models explicit:

- Playwright runs against the Spring Boot app directly on `8080`, or
- Playwright runs against the Angular dev server, which proxies `/api` to Spring Boot on `8081`.

Without that clarification, CI setup is likely to drift from local execution.

## Recommended changes before approving the test plan

1. Expand the Angular and E2E sections to cover terms, reports, preferences/settings, account edit,
   and admin user flows.
2. Rewrite the REST and command matrices so they match the current controllers and registered
   command names exactly.
3. Add explicit Java contract tests for `/api/auth/me`, `/api/annotations`, `/api/users/roles`,
   `/api/users/organizations`, project tree, stakeholder permissions, terms, reports, and report
   run/download.
4. Remove or rename tests that target non-existent behaviors (`/api/auth/logout`,
   `DeleteUserStakeholder`, `DeleteNonUserStakeholder`, `isLoggedIn()`, scenario-selector
   `includeTypes`, project-service cache invalidation).
5. Split the E2E setup section into "dev-server mode" and "packaged-app mode" or standardize on
   one port model throughout the document.

## Bottom line

The test plan is a solid draft, but it is not yet complete or fully implementation-aligned. I
would not treat it as release-ready until the missing Angular/Java feature areas and the endpoint
/ command-name mismatches are corrected.
