# UI Module Migration Plan

## Goals
- Decouple UI code by feature area so we can evolve/replace pieces independently (and eventually swap authentication).
- Keep requel-app lean; shared UI infra moves to a small `ui-core`.
- No functional changes—initial move should be package/path only.

## Current layout (requel-app)
- `com.rreganjr.requel.ui.*` (mixed shared bits)
- `com.rreganjr.requel.ui.annotation.*`
- `com.rreganjr.requel.ui.project.*`
- `com.rreganjr.requel.ui.user.*`
- `com.rreganjr.nlp.ui.*`
- `com.rreganjr.requel.ui.login.*` (leave for now)

## Target modules
- **ui-core**: shared base controllers/beans, common DTOs/view helpers, templates/layout fragments (keep login wiring here for now).
- **annotation-ui**: UI for annotations, issues, positions.
- **project-ui**: UI for projects/goals/stories/use cases/scenarios, project dashboards.
- **user-ui**: user/stakeholder admin screens.
- **nlp-ui**: NLP-facing UI (lexical analysis pages, NLP admin).
- **login-ui**: stay in requel-app temporarily; revisit when moving to Spring Security.

## Dependency rules
- `ui-core` depends only on web framework + platform-core/identity APIs (no JPA).
- Feature UI modules depend on their domain/service modules (e.g., project-ui -> project-jpa, annotation-ui -> annotation-jpa).
- Cross-UI sharing should go via `ui-core`; avoid feature-to-feature coupling.
- requel-app depends on all UI modules to assemble the WAR.

## Migration steps
1) **Create modules**: add Maven modules `ui-core`, `annotation-ui`, `project-ui`, `user-ui`, `nlp-ui` under `modules/`; wire into parent POM.
2) **Move classes**:
   - Move packages wholesale to matching modules (maintain package names to avoid refactor churn).
   - Shared helpers/layout beans go to `ui-core`.
3) **Resources**:
   - If views/templates/js/css live under `src/main/webapp` or `resources`, move feature-specific assets with their modules.
   - Keep shared layouts/assets in `ui-core`.
   - Move shared web infra helpers (e.g., `nextapp/echo2/webcontainer/filetransfer/CommonsFileUpload2JakartaProvider`) into `ui-core` since multiple features rely on upload support.
   - Relocate resource trees: `modules/requel-app/src/main/resources/com/rreganjr/nlp/ui/**` → `nlp-ui`; `modules/requel-app/src/main/resources/com/rreganjr/requel/ui/**` split into `annotation-ui`, `project-ui`, `user-ui`, `ui-core` as appropriate (shared layouts go to `ui-core`).
   - Images under `modules/requel-app/src/main/resources/images/**`: move shared/logos/buttons/backgrounds referenced from `Default.stylesheet` into `ui-core`; move any feature-specific editor icons (e.g., scenario/actor editors) into the corresponding feature UI module.
4) **Spring config**:
   - Each UI module exports a `@Configuration` (or component scan) class.
   - requel-app imports those configs; remove feature scans from requel-app once wired.
5) **Build/packaging**:
   - UI modules as `jar` with classes/resources; requel-app WAR pulls them in.
   - Adjust surefire/failsafe if there are UI tests; ensure test contexts import the new configs.
6) **Login/auth**:
   - Leave `login` package in requel-app for now; note future move to Spring Security.
7) **Smoke test**:
   - Run existing UI tests; manual smoke of key pages to ensure component scanning/resources resolve.

## Execution task list (practical steps)
1) Add new Maven modules (`ui-core`, `annotation-ui`, `project-ui`, `user-ui`, `nlp-ui`, `ui-assets`) to parent POM.
2) Create skeletal module POMs (jar packaging) with Spring/web deps, and stub `@Configuration` classes for each UI module; add a simple autoconfig to ui-core.
3) Move Java packages:
   - `com.rreganjr.requel.ui.annotation.*` → annotation-ui
   - `com.rreganjr.requel.ui.project.*` → project-ui
   - `com.rreganjr.requel.ui.user.*` → user-ui
   - `com.rreganjr.nlp.ui.*` → nlp-ui
   - shared `com.rreganjr.requel.ui.*` helpers/layouts → ui-core
   - keep `com.rreganjr.requel.ui.login.*` in requel-app for now.
4) Resources:
   - Move `com/rreganjr/nlp/ui/**` resources → nlp-ui; `com/rreganjr/requel/ui/**` split to feature UI modules; shared layouts → ui-core.
   - Move `/images/**` shared/logos/buttons/backgrounds → ui-assets; feature editor icons → respective feature UI modules.
5) Web infra helpers: move `nextapp/echo2/webcontainer/filetransfer/CommonsFileUpload2JakartaProvider` to ui-core.
6) Wire Spring: requel-app imports the new UI module configs; remove redundant scans.
7) Adjust build: ensure UI modules are on the requel-app classpath; fix any test context imports.
8) Smoke tests: run UI-related tests (if any) and manual launch to verify resource resolution.

## Risks / mitigations
- **ClassPath/resource resolution**: verify template/resource paths after moves; add tests for view resolution.
- **Component scanning gaps**: add explicit `@Import` per module to avoid missing beans.
- **Circular deps**: watch for feature UI pulling in other feature modules; refactor shared bits to `ui-core`.

## Open questions / decisions
- View tech: all Echo2; templates/resources are classpath-based (not Thymeleaf/JSF). Keep that in mind when relocating assets.
- Static assets: create a `ui-assets` module for shared images/buttons/backgrounds; keep feature-specific icons with their feature UI modules.
- Test fixtures: if/when we add UI test scaffolding, put shared fixtures in `ui-core` (or a `ui-test-support` helper) and feature-specific fixtures alongside the feature modules.
