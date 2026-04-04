# UI Migration Codex Review

## Findings

1. High: The non-API Spring Security chain currently leaves everything outside `/api/**` publicly accessible and also registers a hard-coded in-memory `admin/admin` account. `Application.WebSecurityConfig` permits every request (`authorize.anyRequest().permitAll()`) while `application.properties` exposes `health`, `info`, `metrics`, and `prometheus`, so the current setup is not secure for production and does not establish a safe default posture for the mixed legacy + API application. References: `modules/requel-app/src/main/java/com/rreganjr/requel/Application.java:89-109`, `modules/requel-app/src/main/resources/application.properties:5-10`.

2. High: SSE sessions are not bound to the authenticated user and can be reattached, modified, or closed solely by presenting a `sessionId`. `StreamController` accepts `sessionId` and `X-Session-Id` without any ownership check, and `StreamService.createStream()` will reuse any supplied ID. This means any authenticated caller that learns another session ID can hijack that stream's subscriptions or close it. The same code path also disables server-side expiry scheduling by always passing `0L`, so live SSE connections are not forced closed when the JWT expires. References: `modules/service-impl/src/main/java/com/rreganjr/requel/service/stream/StreamController.java:28-67`, `modules/service-impl/src/main/java/com/rreganjr/requel/service/stream/StreamService.java:46-103`.

3. High: The user editor can silently over-grant permissions on save. `UserDto` exposes one flat `permissions` list across all roles, `UserDtoMapper` builds that list by concatenating granted permissions from every role, and `UserEditorComponent.populateForm()` then copies that entire flat list into every selected role. A user with multiple roles can therefore receive permissions on role B that originally came only from role A after an otherwise unrelated edit/save cycle. References: `modules/service-api/src/main/java/com/rreganjr/requel/service/api/dto/UserDto.java:8-18`, `modules/service-impl/src/main/java/com/rreganjr/requel/service/auth/UserDtoMapper.java:24-49`, `requel-angular/src/app/features/users/user-editor.ts:231-242`.

4. Medium: The sidebar implementation does not match the design guide's core behavior for project ordering and preferences. The guide requires recency-based ordering, user-configurable cap/staleness, and preferences loaded on login; the implementation always loads the same alphabetical project list for both the main table and sidebar, and the preferences flow is only wired into the Settings page. There is currently no code path where `sidebarProjectLimit` or `sidebarProjectStaleness` affects sidebar loading. References: `modules/service-impl/src/main/java/com/rreganjr/requel/service/query/ProjectQueryController.java:106-121`, `requel-angular/src/app/shared/sidebar-nav.ts:162-170`, `requel-angular/src/app/shared/sidebar-nav.ts:246-253`, `requel-angular/src/app/core/preferences.service.ts:19-38`, `requel-angular/src/app/features/users/settings.ts:72-99`.

5. Medium: The user editor is out of line with the guide's required dirty-form and same-route handling. The guide explicitly calls for `route.paramMap`-driven editors plus an unsaved-changes confirmation when switching records. `UserEditorComponent` uses `route.snapshot`, never reacts to same-route param changes, and has no unsaved-changes protection at all. This makes `/users/A -> /users/B` inconsistent with the project/entity editors and can drop edits if the route is reused. References: `requel-angular/src/app/features/users/user-editor.ts:147-179`, `doc/UI_DESIGN_GUIDE.md` section 7.11.

6. Medium: The user edit flow does not honor the branch's own optimistic-locking contract. `EditUserInput` documents `id` and `version` as required for updates, but `UserEditorComponent` never sends either field, `UserDtoMapper` hard-codes DTO version to `0`, and `UserCommandRegistrar` ignores `id/version` completely when wiring the command. That means concurrent user edits are not protected the way project edits are. References: `modules/service-api/src/main/java/com/rreganjr/requel/service/api/dto/EditUserInput.java:7-24`, `modules/service-impl/src/main/java/com/rreganjr/requel/service/auth/UserDtoMapper.java:39-50`, `requel-angular/src/app/features/users/user-editor.ts:191-201`, `modules/service-impl/src/main/java/com/rreganjr/requel/service/command/UserCommandRegistrar.java:38-65`.

7. Medium: The auth interceptor treats authorization failures as authentication failures. It logs the user out on both `401` and `403`, so a valid session that hits an admin-only or stakeholder-protected endpoint will be converted into a forced logout instead of an authorization message. That will make permission bugs and partial UI access look like random session expiry. References: `requel-angular/src/app/core/auth.interceptor.ts:18-25`.

8. Low: API URL handling is inconsistent across the Angular data services. Some services correctly `encodeURIComponent(projectName)` while others interpolate raw project names directly into the URL. Projects with spaces or reserved characters will work in some feature areas and fail in others. References: `requel-angular/src/app/core/use-case.service.ts:11-20`, `requel-angular/src/app/core/term.service.ts:13-18`, `requel-angular/src/app/core/report.service.ts:19-24`, `requel-angular/src/app/core/report.service.ts:39-44`, `requel-angular/src/app/features/open-issues/open-issues.ts:136-141`.

## Refactor Opportunities

- Extract a shared list-page scaffold for the repeated `page-header` + `page-actions` + `search-bar` pattern. The same markup and CSS are repeated across most list views, for example `requel-angular/src/app/features/projects/project-list.ts:18-29`, `requel-angular/src/app/features/users/user-list.ts:16-29`, `requel-angular/src/app/features/goals/goal-list.ts`, `requel-angular/src/app/features/stories/story-list.ts`, and several more.

- Extract a reusable dirty-editor helper/guard instead of keeping multiple hand-rolled versions. `project-editor`, `story-editor`, `scenario-editor`, `use-case-editor`, and `stakeholder-editor` all have local change-tracking or confirmation code, while `user-editor` and `edit-account` have none. This is a good place for one shared route-aware unsaved-changes service/pattern.

- Move the standalone route graph to `loadComponent`-based lazy loading. `app.routes.ts` eagerly imports every feature component today, which is not ideal Angular 21 usage for an app of this size and aligns with the current build warning about the initial bundle size. Reference: `requel-angular/src/app/app.routes.ts:1-64`.

- Normalize API URL construction in one place. Right now some services use `environment.apiBaseUrl`, some hard-code `'/api'`, and encoding is inconsistent. A small shared helper for project-scoped URLs would remove the drift and prevent the raw-name bugs noted above.

## Verification

- `npm run build` in `requel-angular` succeeds, but Angular reports that the initial bundle exceeds the configured `1.00 MB` budget with a `1.62 MB` initial bundle.

- `mvn -pl modules/service-impl,modules/service-api,modules/requel-app -am test -DskipITs` fails in `requel-app` with five `NoSuchUserException` test errors:
  `AnnotationAnyMappingTest.loadsAnnotatableViaAnyMapping`
  `GoalAssistantTest.testGoalAssistantGoalNameIssue`
  `EditProjectCommandImplTest.testProjectCreation`
  `ImportProjectStreamingCommandTest.importingUserGetsStakeholderPermissions`
  `ImportProjectStreamingCommandTest.streamingImportLoadsDocSample`
