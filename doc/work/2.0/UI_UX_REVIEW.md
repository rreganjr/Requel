# Requel Angular UI/UX, Accessibility, and Front-End Architecture Review

Review scope: `requel-angular/src` for the Angular 21 standalone SPA using PrimeNG 21, `@primeuix/themes` Aura, and the CQRS `/api/**` backend. This is a review-only report; it contains findings and recommendations, not implementation changes.

## Summary

The highest-impact changes are:

1. Establish a Requel design system instead of the stock Aura preset. The app currently uses `providePrimeNG({ theme: { preset: Aura } })` with only nine lines of global CSS, then compensates with many component-local styles and hard-coded colors (`requel-angular/src/app/app.config.ts:25`, `requel-angular/src/app/app.config.ts:36`, `requel-angular/src/styles.scss:1`, `requel-angular/src/styles.scss:3`). Create a custom `definePreset`, global design tokens, and shared UI primitives.
2. Replace repeated page/editor layouts with shared templates: `app-list-page`, `app-page-shell`, `app-entity-editor-shell`, `app-field`, `app-empty-state`, and `app-inline-message`. Today the same header/action/form/section CSS is duplicated across project, goal, story, actor, stakeholder, scenario, use-case, term, report, settings, and admin views (`project-editor.ts:48`, `goal-editor.ts:54`, `story-editor.ts:54`, `actor-editor.ts:52`, `scenario-editor.ts:71`, `use-case-editor.ts:61`, `term-editor.ts:48`, `report-editor.ts:45`).
3. Move forms to reactive forms with a common validation and command-error adapter. Most forms are template-driven `[(ngModel)]` with minimal client validation and page-level error strings (`goal-editor.ts:76`, `story-editor.ts:79`, `scenario-editor.ts:93`, `use-case-editor.ts:83`, `stakeholder-editor.ts:75`, `user-editor.ts:52`). This causes inconsistent disabled states, missing inline errors, and weak assistive technology support.
4. Fix critical WCAG 2.2 AA gaps: add skip links, robust landmarks/headings, keyboard-operable custom clickable elements, accessible names for icon-only buttons, error live regions, field error associations, reduced-motion handling, and target-size consistency. Examples include click-only anchors without `href` (`goal-editor.ts:111`, `story-editor.ts:137`, `use-case-editor.ts:135`), custom fixed overlays instead of accessible dialogs (`goal-editor.ts:178`, `scenario-editor.ts:203`), and click-only div controls for scenario step insertion (`scenario-editor.ts:134`, `scenario-editor.ts:187`).
5. Make primary task flows project-aware and reduce sidebar dependence. The route model supports deep project sections (`app.routes.ts:43` through `app.routes.ts:60`), but the page header does not show project context, breadcrumbs, related entity summaries, or next actions, so users must mentally connect project -> goals/actors/stories/scenarios/use-cases from the sidebar tree (`sidebar-nav.ts:361` through `sidebar-nav.ts:379`).
6. Standardize loading, empty, and error states. List pages have table `emptymessage`, but editors often have no visible loading state despite `loading` signals, and many supplemental loads fail silently (`project-editor.ts:125`, `project-editor.ts:203`, `tag-selector.ts:171`, `annotations-section.ts:298`).
7. Improve Angular 21 idioms: add `ChangeDetectionStrategy.OnPush`, consider zoneless change detection after PrimeNG verification, prefer `takeUntilDestroyed`/`DestroyRef` over manual subscription fields, and stop mixing signal state with mutable class fields in forms. The code uses many signals but no OnPush, no reactive forms, and manual subscriptions across many components (`event-stream.service.ts:38`, `story-editor.ts:253`, `scenario-editor.ts:341`, `use-case-editor.ts:436`).
8. Add automated accessibility regression tests with Playwright + axe. Playwright is already present (`package.json:10`, `package.json:36`), so add `@axe-core/playwright` and scan the login page, layout, every list page shell, one representative editor, dialogs, and the scenario step editor.

## 1. Visual Design and Theming

### Finding 1.1 - The app uses stock Aura with no Requel brand layer

Priority: High. Effort: Medium, 3-5 days for first usable theme and token pass.

What exists today:

- PrimeNG is configured with uncustomized Aura (`app.config.ts:25`, `app.config.ts:36`).
- Global CSS only imports PrimeIcons and applies PrimeNG font/background/text variables to `html, body` (`styles.scss:1`, `styles.scss:3` through `styles.scss:8`).
- The header is hard-coded to `#1a1a7e` and white (`layout.ts:73` through `layout.ts:80`), outside PrimeNG semantic tokens.
- Chip/badge colors are repeatedly hard-coded as token fallbacks, for example goal tags (`goal-list.ts:93` through `goal-list.ts:95`), annotations (`annotations-section.ts:241` through `annotations-section.ts:249`), and tags (`tag-selector.ts:90` through `tag-selector.ts:92`).

Problems:

- The UI inherits a generic PrimeNG look; it has no recognizable Requel visual identity.
- Color, radius, density, and typography are not governed by one source of truth.
- Dark mode is not defined, so any future dark mode would inherit whatever Aura provides plus conflicting custom hard-coded colors.
- Component-local colors make contrast and semantic consistency hard to audit.

Recommendations:

- Add `src/app/theme/requel-preset.ts` and replace the raw Aura preset with a Requel preset.
- Define semantic colors for primary actions, content, success/warn/error/info, focus rings, form fields, chips, and panels.
- Add app-level CSS design tokens in `styles.scss` for spacing, radius, type scale, and layout widths. Keep PrimeNG token overrides in the preset and app layout tokens in CSS variables.
- Use theme tokens in component styles only through semantic variables such as `--rq-space-3`, `--rq-radius-sm`, `--rq-chip-bg`, `--rq-chip-fg`.

Starter theme example:

```ts
// src/app/theme/requel-preset.ts
import { definePreset } from '@primeuix/themes';
import Aura from '@primeuix/themes/aura';

export const RequelPreset = definePreset(Aura, {
  primitive: {
    requel: {
      50: '#eef6ff',
      100: '#d8ecff',
      200: '#b7dcff',
      300: '#86c5ff',
      400: '#4da4f2',
      500: '#287fcf',
      600: '#1f64a8',
      700: '#1c5086',
      800: '#1b456f',
      900: '#17365a',
      950: '#0f233d'
    }
  },
  semantic: {
    primary: {
      50: '{requel.50}',
      100: '{requel.100}',
      200: '{requel.200}',
      300: '{requel.300}',
      400: '{requel.400}',
      500: '{requel.500}',
      600: '{requel.600}',
      700: '{requel.700}',
      800: '{requel.800}',
      900: '{requel.900}',
      950: '{requel.950}'
    },
    colorScheme: {
      light: {
        surface: {
          0: '#ffffff',
          50: '#f8fafc',
          100: '#eef2f7',
          200: '#dbe3ee',
          300: '#c3cfdd',
          400: '#94a3b8',
          500: '#64748b',
          600: '#475569',
          700: '#334155',
          800: '#1f2937',
          900: '#111827',
          950: '#07111f'
        },
        text: {
          color: '#182230',
          hoverColor: '#0f233d',
          mutedColor: '#5b687a'
        },
        content: {
          background: '#ffffff',
          borderColor: '#d8e0ea',
          hoverBackground: '#f2f6fb'
        },
        focusRing: {
          color: '{primary.500}',
          width: '3px',
          style: 'solid',
          offset: '2px'
        }
      },
      dark: {
        surface: {
          0: '#0b1220',
          50: '#111827',
          100: '#172033',
          200: '#22304a',
          300: '#31415f',
          400: '#64748b',
          500: '#94a3b8',
          600: '#cbd5e1',
          700: '#e2e8f0',
          800: '#f1f5f9',
          900: '#f8fafc',
          950: '#ffffff'
        },
        text: {
          color: '#f8fafc',
          mutedColor: '#b6c2d2'
        },
        content: {
          background: '#111827',
          borderColor: '#334155',
          hoverBackground: '#172033'
        },
        focusRing: {
          color: '{primary.300}',
          width: '3px',
          style: 'solid',
          offset: '2px'
        }
      }
    }
  },
  components: {
    button: {
      borderRadius: '6px'
    },
    card: {
      borderRadius: '8px'
    },
    datatable: {
      headerCellPadding: '0.625rem 0.75rem',
      bodyCellPadding: '0.55rem 0.75rem'
    }
  }
});
```

```ts
// src/app/app.config.ts
import { RequelPreset } from './theme/requel-preset';

providePrimeNG({
  theme: {
    preset: RequelPreset,
    options: {
      darkModeSelector: '.rq-dark'
    }
  }
});
```

```scss
/* src/styles.scss */
:root {
  --rq-space-1: 0.25rem;
  --rq-space-2: 0.5rem;
  --rq-space-3: 0.75rem;
  --rq-space-4: 1rem;
  --rq-space-6: 1.5rem;
  --rq-space-8: 2rem;
  --rq-radius-sm: 4px;
  --rq-radius-md: 6px;
  --rq-radius-lg: 8px;
  --rq-page-max: 76rem;
  --rq-editor-max: 48rem;
  --rq-font-size-xs: 0.75rem;
  --rq-font-size-sm: 0.875rem;
  --rq-font-size-md: 1rem;
  --rq-font-size-lg: 1.125rem;
}

.rq-page {
  max-width: var(--rq-page-max);
  margin: 0 auto;
}
```

### Finding 1.2 - Component-local CSS fights PrimeNG and fragments visual consistency

Priority: High. Effort: Medium, 4-7 days to extract common patterns without redesigning all pages.

What exists today:

- `:host ::ng-deep` is used for account-trigger color and tree styling (`layout.ts:107`, `sidebar-nav.ts:178`, `sidebar-nav.ts:182`, `entity-selector-dialog.ts:98`).
- Forms repeat custom grid definitions with different label widths and max widths (`goal-editor.ts:213`, `story-editor.ts:214`, `actor-editor.ts:191`, `scenario-editor.ts:246`, `use-case-editor.ts:377`, `term-editor.ts:138`, `report-editor.ts:102`, `stakeholder-editor.ts:185`).
- Inline styles appear in templates for hidden file inputs and table widths (`project-list.ts:43` through `project-list.ts:45`, `report-editor.ts:76` through `report-editor.ts:77`, `goal-editor.ts:106`, `use-case-editor.ts:148`).
- Some tables use `ng-template #header`, others use `pTemplate="header"` (`project-list.ts:62`, `use-case-list.ts:49`). Both can work, but the inconsistency makes local patterns harder to maintain.

Problems:

- The same concept looks slightly different depending on which editor the user opens.
- `ng-deep` and inline styles are brittle and make PrimeNG upgrades riskier.
- PrimeNG can carry much of the styling if wrapped through a small shared layer; the current implementation duplicates CSS instead.

Recommendations:

- Introduce shared CSS utilities and wrapper components:
  - `app-page-header` with title, subtitle/project context, and action slots.
  - `app-form-grid` with `labelWidth`, `density`, and responsive behavior.
  - `app-section` for `h3` + action area + empty state.
  - `app-entity-link` rendering a real `a [routerLink]`.
  - `app-chip` or PrimeNG `p-tag` for tags/status instead of hand-rolled chips.
- Replace `:host ::ng-deep` with PrimeNG `pt` pass-through config or `styleClass` and global classes in `styles.scss`.
- Replace hidden file inputs with a shared `app-file-upload-button` that owns labeling and focus behavior.

### Finding 1.3 - Typography and hierarchy are too flat

Priority: Medium. Effort: Small, 1-2 days for tokens and shell updates; larger to apply everywhere.

What exists today:

- Layout header title uses `font-size: 1.25rem` and letter spacing (`layout.ts:96` through `layout.ts:100`).
- Most pages use bare `h2` in a local `.page-header` without subtitles, breadcrumbs, metadata, or consistent body max width (`list-page.ts:30` through `list-page.ts:35`; `dashboard.ts:32` through `dashboard.ts:33`).
- The login page has a simple card with `h2` and subtitle (`login.ts:35` through `login.ts:39`).

Problems:

- Artifact editors do not convey where the user is in the requirements model.
- Dense tables and multi-section editors lack scan-friendly hierarchy beyond repeated `h3`s.
- Search/filter/action density differs across list pages.

Recommendations:

- Standardize:
  - Page title: artifact or collection name.
  - Eyebrow/context: project name and artifact type.
  - Metadata: status, counts, permissions, unsaved state.
  - Primary action: right-aligned, consistent severity.
- Add a compact toolbar pattern for filters and list actions.

## 2. Layout, Flow, and Information Architecture

### Finding 2.1 - Navigation is technically complete but project context is hidden in the sidebar

Priority: High. Effort: Medium, 3-5 days.

What exists today:

- The authenticated shell has a fixed header, sidebar, and main area (`layout.ts:42` through `layout.ts:63`).
- Project artifact routes are nested under `/projects/:name/...` (`app.routes.ts:43` through `app.routes.ts:60`).
- The sidebar tree maps each project to Stakeholders, Goals, Stories, Actors, Scenarios, Use Cases, Glossary, Reports, and Open Issues (`sidebar-nav.ts:361` through `sidebar-nav.ts:379`).
- The dashboard is only a placeholder telling users to select a project from the sidebar (`dashboard.ts:24` through `dashboard.ts:33`).

Problems:

- Main pages do not show breadcrumbs or the active project, so a user landing on a deep link has weak context.
- The primary model flow is split across sidebar groups, tables, and editor sub-tables without a clear project workspace.
- The dashboard does not help resume work, see recent projects, or surface open issues.

Recommendations:

- Add a project workspace route at `/projects/:name` that shows a compact overview: counts, open issues, recent changes, and next actions.
- Add breadcrumbs to all project-scoped pages: `Projects / {project} / Goals / {goal}`.
- Add project-aware action groups in editor headers: `Back to Goals`, `Open Issues`, `Related Stories`, `Related Use Cases`.
- Keep the sidebar as navigation, not the only IA surface.

Example page shell:

```html
<app-page-shell
  [title]="isNew() ? 'New Goal' : goalName()"
  [eyebrow]="projectName"
  [breadcrumbs]="[
    { label: 'Projects', routerLink: ['/projects'] },
    { label: projectName, routerLink: ['/projects', projectName] },
    { label: 'Goals', routerLink: ['/projects', projectName, 'goals'] }
  ]"
>
  <ng-container pageActions>
    <p-button label="Back" icon="pi pi-arrow-left" severity="secondary" [outlined]="true" />
    <p-button label="Save" icon="pi pi-check" [loading]="saving()" />
  </ng-container>

  <!-- editor form -->
</app-page-shell>
```

### Finding 2.2 - List/detail patterns are inconsistent and over-rely on row selection

Priority: High. Effort: Medium, 3-5 days.

What exists today:

- Many lists use `p-table` with row selection to navigate (`project-list.ts:59` through `project-list.ts:61`, `goal-list.ts:58` through `goal-list.ts:60`, `story-list.ts:50` through `story-list.ts:52`, `stakeholder-list.ts:52` through `stakeholder-list.ts:54`).
- Reports instead use explicit Edit/Run actions (`report-list.ts:62` through `report-list.ts:69`).
- Use Cases and Scenarios disable search (`use-case-list.ts:36`, `scenario-list.ts:37`), while most other lists use global search (`list-page.ts:36` through `list-page.ts:43`).
- Empty states are plain table messages (`project-list.ts:86` through `project-list.ts:88`, `goal-list.ts:83` through `goal-list.ts:85`, `use-case-list.ts:63` through `use-case-list.ts:65`).

Problems:

- Row click/select is discoverable for mouse users only after trial and error, and not consistently represented as a link.
- Some lists are searchable, others are not, without visible rationale.
- Empty states do not guide users to create the first artifact or explain prerequisites.

Recommendations:

- Use explicit link cells for names, with row hover as a secondary affordance.
- Standardize list actions:
  - Name column is a real link.
  - Optional row actions column for secondary operations.
  - Search available by default; disable only with a visible reason.
  - Empty state includes title, short guidance, and primary action if permitted.
- Add a shared `EntityListPageComponent` over `p-table`.

### Finding 2.3 - Dialog and relationship flows need clearer progression

Priority: Medium. Effort: Medium, 3-6 days.

What exists today:

- Relationships are added via entity selector dialogs (`entity-selector-dialog.ts:53` through `entity-selector-dialog.ts:93`).
- Scenario sub-scenarios use a specialized dialog that can create-and-add inline (`scenario-selector-dialog.ts:51` through `scenario-selector-dialog.ts:115`).
- Goal relation type is collected in a custom overlay rather than `p-dialog` (`goal-editor.ts:177` through `goal-editor.ts:190`).
- Scenario step details use a custom fixed overlay (`scenario-editor.ts:201` through `scenario-editor.ts:224`).

Problems:

- Some flows can create new entities inline; others force users to leave the editor.
- Custom overlays miss PrimeNG dialog behavior and accessibility hooks.
- Add/remove relationship sections repeat across actors, stories, stakeholders, use cases, goals, and scenarios with inconsistent visual structure.

Recommendations:

- Build a reusable `app-relationship-section` with `title`, `items`, `addLabel`, `emptyText`, `linkFactory`, `remove`.
- Use `p-dialog` for all modal/popup content.
- Add "Create new" support to entity selector dialogs where the next step is obvious.
- After add/remove, keep focus near the action and announce status.

### Finding 2.4 - Loading, empty, and failure states are under-specified

Priority: High. Effort: Small, 2-3 days for common components and first pass.

What exists today:

- Lists usually bind `[loading]` to tables (`project-list.ts:59`, `goal-list.ts:58`, `stakeholder-list.ts:52`).
- Editors often define `loading` signals but do not render skeletons or loading affordances (`project-editor.ts:125`, `user-editor.ts:150`, `scenario-editor.ts:317`).
- Supplemental loads silently fail for tags and annotations (`tag-selector.ts:171` through `tag-selector.ts:172`, `annotations-section.ts:298` through `annotations-section.ts:299`).

Problems:

- Users can see blank forms or stale sections during asynchronous loading.
- Silent failures hide lost capabilities, especially tags/annotations that are central to requirements triage.
- Empty states lack calls to action.

Recommendations:

- Create shared states:
  - `app-loading-state`: skeleton or spinner with label.
  - `app-error-state`: message, retry button, support detail.
  - `app-empty-state`: title, body, optional action.
- Use inline warnings for supplemental section failures instead of silent ignores.

## 3. Forms, Validation, and Error Messaging

### Finding 3.1 - Forms are mostly template-driven and do not provide consistent validation

Priority: High. Effort: Large, 1-2 weeks for high-risk forms, 3-4 weeks for full migration.

What exists today:

- Login uses `FormsModule`, signals, `[(ngModel)]`, and only disables submit when fields are empty (`login.ts:44` through `login.ts:59`).
- Project and user editors are template-driven `NgForm` forms (`project-editor.ts:66` through `project-editor.ts:93`, `user-editor.ts:52` through `user-editor.ts:128`).
- Most artifact editors use loose `div.form-grid` plus `[(ngModel)]`, not actual `<form>` submission (`goal-editor.ts:76` through `goal-editor.ts:87`, `story-editor.ts:79` through `story-editor.ts:113`, `scenario-editor.ts:93` through `scenario-editor.ts:113`, `use-case-editor.ts:83` through `use-case-editor.ts:111`, `term-editor.ts:64` through `term-editor.ts:82`, `report-editor.ts:65` through `report-editor.ts:86`, `stakeholder-editor.ts:75` through `stakeholder-editor.ts:134`).
- Only a few fields have native validators; project name has `required` (`project-editor.ts:70`), email fields use `type="email"` but no displayed field errors (`user-editor.ts:67`, `edit-account.ts:66`).
- Required-name validation is sometimes imperative in save handlers (`term-editor.ts:277` through `term-editor.ts:280`, `report-editor.ts:199` through `report-editor.ts:202`), while many editors submit empty names to the server (`goal-editor.ts:348` through `goal-editor.ts:359`, `story-editor.ts:365` through `story-editor.ts:378`, `scenario-editor.ts:564` through `scenario-editor.ts:578`).

Problems:

- Users discover validation problems after save, not inline.
- Assistive technologies are not told which fields are invalid.
- Disabled Save states vary: dirty-only, change-tracked, required-only, or always enabled.
- Mutable class fields and signals are mixed, leading to manual `trackChanges()` and comments about timing issues (`story-editor.ts:344` through `story-editor.ts:350`, `settings.ts:122` through `settings.ts:125`, `user-editor.ts:155` through `user-editor.ts:156`).

Recommendations:

- Standardize on reactive forms for all editors.
- Use a common validation helper:
  - required name validation for all named artifacts.
  - max length where backend constraints exist.
  - email format for user/account.
  - password confirmation validator.
  - at least one role for admin-created users if required by backend.
- Disable Save when `form.invalid || form.pristine || saving()`.
- Render field-level errors with `aria-describedby` and `aria-invalid`.
- Convert command `violations` into field errors when field names are available; otherwise show a page-level alert.

Before example from current style:

```html
<!-- requel-angular/src/app/features/goals/goal-editor.ts:76-87 -->
<div class="form-grid">
  <label for="name">Name</label>
  <input id="name" pInputText [(ngModel)]="name" placeholder="Goal name" />

  <label for="text">Description</label>
  <textarea id="text" pTextarea [(ngModel)]="text" rows="6"
            placeholder="Goal description"></textarea>
</div>
<div class="form-actions">
  <p-button label="Save" icon="pi pi-check" (onClick)="onSave()" [loading]="saving()" />
</div>
```

After example:

```ts
readonly form = this.fb.nonNullable.group({
  name: ['', [Validators.required, Validators.maxLength(120)]],
  text: ['', [Validators.maxLength(8000)]]
});

readonly saving = signal(false);
readonly submitError = signal<string | null>(null);

async save(): Promise<void> {
  this.submitError.set(null);
  this.form.markAllAsTouched();
  if (this.form.invalid) return;

  this.saving.set(true);
  const result = await this.commandService.execute('EditGoal', {
    projectName: this.projectName,
    goalId: this.goalId,
    version: this.version,
    ...this.form.getRawValue()
  });
  this.saving.set(false);

  if (!result.success) {
    applyCommandErrors(this.form, result.violations);
    this.submitError.set(result.error ?? 'Goal could not be saved.');
    return;
  }
}
```

```html
<form class="rq-form" [formGroup]="form" (ngSubmit)="save()" novalidate>
  <app-field label="Name" for="goal-name" [control]="form.controls.name">
    <input
      id="goal-name"
      pInputText
      formControlName="name"
      autocomplete="off"
      [attr.aria-invalid]="form.controls.name.invalid && form.controls.name.touched"
      aria-describedby="goal-name-error" />
    <small id="goal-name-error" class="rq-field-error" aria-live="polite">
      @if (form.controls.name.hasError('required') && form.controls.name.touched) {
        Goal name is required.
      }
      @if (form.controls.name.hasError('maxlength')) {
        Use 120 characters or fewer.
      }
    </small>
  </app-field>

  <app-field label="Description" for="goal-text" [control]="form.controls.text">
    <textarea id="goal-text" pTextarea formControlName="text" rows="6"></textarea>
  </app-field>

  @if (submitError()) {
    <p-message severity="error" [text]="submitError()!" role="alert" />
  }

  <p-button
    type="submit"
    label="Save"
    icon="pi pi-check"
    [loading]="saving()"
    [disabled]="form.invalid || form.pristine || saving()" />
</form>
```

### Finding 3.2 - API and command errors are surfaced inconsistently

Priority: High. Effort: Medium, 3-5 days.

What exists today:

- A root `<p-toast />` exists in the authenticated layout (`layout.ts:41`).
- Some components set inline `p-message` errors (`project-list.ts:49` through `project-list.ts:57`, `project-editor.ts:59` through `project-editor.ts:64`).
- Some components use toasts for success or nested action errors (`goal-editor.ts:361`, `tag-selector.ts:183`, `annotations-section.ts:311`).
- `ProjectEditor`, `UserEditor`, and `EditAccount` join backend violations into one semicolon-delimited error string (`project-editor.ts:260` through `project-editor.ts:264`, `user-editor.ts:273` through `user-editor.ts:277`, `edit-account.ts:178` through `edit-account.ts:181`).
- `CommandService` normalizes HTTP failures, but only to command-level errors, not field-level errors (`command.service.ts:72` through `command.service.ts:93`).

Problems:

- Users must hunt for whether a result appears inline or as a toast.
- Toasts can disappear before screen reader users or keyboard users notice them.
- Field-specific backend violations are lost when concatenated.

Recommendations:

- Use inline messages for blocking page/form errors.
- Use toasts for non-blocking confirmations only.
- Add a command error adapter:
  - `violations` with field path -> `form.controls[field].setErrors({ server: message })`.
  - command-level error -> `submitError`.
  - unexpected network failure -> retryable inline alert.
- Add `role="alert"` or `aria-live="assertive"` for blocking errors and `aria-live="polite"` for success.

### Finding 3.3 - Mini-forms in annotations, tags, admin tools, and dialogs need the same validation contract

Priority: Medium. Effort: Medium, 4-6 days.

What exists today:

- Tag add row has category/value inputs and silently returns when value is empty (`tag-selector.ts:62` through `tag-selector.ts:80`, `tag-selector.ts:176` through `tag-selector.ts:179`).
- Annotation note/issue/position/argument forms silently return when text is blank (`annotations-section.ts:303` through `annotations-section.ts:305`, `annotations-section.ts:320` through `annotations-section.ts:322`, `annotations-section.ts:357` through `annotations-section.ts:359`, `annotations-section.ts:383` through `annotations-section.ts:385`).
- Global tags and tag categories silently return for missing value/name (`global-tags.ts:119` through `global-tags.ts:122`, `tag-categories.ts:132` through `tag-categories.ts:135`).
- Scenario selector create form disables create until name exists and shows a simple create error paragraph (`scenario-selector-dialog.ts:69` through `scenario-selector-dialog.ts:78`).
- API token creation disables create until name exists, but create errors appear outside the dialog in the parent message slot (`api-tokens.ts:91` through `api-tokens.ts:107`, `api-tokens.ts:198` through `api-tokens.ts:213`).

Problems:

- Blank submissions sometimes do nothing with no explanation.
- Error messages are not associated with their fields.
- Dialog-level errors can be visually separated from the dialog where the failure occurred.

Recommendations:

- Use a shared `InlineCreateForm` pattern for add-row forms.
- On blank submit, mark the field touched and show "Value is required."
- Keep dialog errors inside the dialog.
- Use `aria-describedby` for helper and error text.

## 4. Accessibility - WCAG 2.2 Level AA

### Finding 4.1 - Landmarks exist, but skip navigation and heading structure are incomplete

Priority: High. Effort: Small, 1 day.

WCAG: 2.4.1 Bypass Blocks, 2.4.6 Headings and Labels, 1.3.1 Info and Relationships.

What exists today:

- The shell uses semantic `header`, `aside`, and `main` (`layout.ts:43`, `layout.ts:57`, `layout.ts:60`).
- There is no skip link before the header/sidebar (`layout.ts:41` through `layout.ts:63`).
- Page titles are usually `h2`, not `h1`, and the dashboard starts with `h2` (`list-page.ts:31`, `dashboard.ts:32`, `login.ts:37`).

Problems:

- Keyboard and screen reader users must tab through header/sidebar before content on every route.
- Pages lack a consistent `h1`.

Recommendations:

- Add a skip link as the first focusable element in the layout.
- Give `<main>` an `id="main-content"` and `tabindex="-1"`.
- Standardize one `h1` per route in the page shell.

```html
<a class="skip-link" href="#main-content">Skip to content</a>
<header class="app-header">...</header>
<main id="main-content" class="main-content" tabindex="-1">
  <router-outlet />
</main>
```

### Finding 4.2 - Several interactive elements are mouse-only or not real links/buttons

Priority: High. Effort: Medium, 3-5 days.

WCAG: 2.1.1 Keyboard, 2.1.3 Keyboard No Exception, 2.4.7 Focus Visible, 4.1.2 Name, Role, Value.

What exists today:

- Many navigation affordances are `<a>` without `href`/`routerLink` and only `(click)`, for example goal relation links (`goal-editor.ts:111`, `goal-editor.ts:137`), story links (`story-editor.ts:137`, `story-editor.ts:171`), actor links (`actor-editor.ts:116`, `actor-editor.ts:156`, `actor-editor.ts:169`), use-case links (`use-case-editor.ts:135`, `use-case-editor.ts:183`, `use-case-editor.ts:243`, `use-case-editor.ts:281`, `use-case-editor.ts:320`), open issues (`open-issues.ts:91`), and scenario sub-scenario links (`scenario-editor.ts:149` through `scenario-editor.ts:151`).
- Scenario add-step controls are clickable `div`s (`scenario-editor.ts:134` through `scenario-editor.ts:136`, `scenario-editor.ts:187` through `scenario-editor.ts:189`).
- Tag remove uses a custom button with a multiply character but no PrimeNG sizing/target pattern (`tag-selector.ts:51` through `tag-selector.ts:52`, `tag-selector.ts:94` through `tag-selector.ts:95`).

Problems:

- Click-only anchors are not keyboard-operable by default and do not expose proper link destination.
- Clickable `div`s lack role, accessible name, and keyboard activation.
- Focus styling for custom controls is not defined.

Recommendations:

- Replace click-only anchors with `[routerLink]`:

```html
<a class="entity-link" [routerLink]="['/projects', projectName, 'goals', g.id]">
  {{ g.name }}
</a>
```

- Replace clickable `div` controls with real buttons:

```html
<button type="button" class="add-step-row" (click)="addStepAt(0)">
  <i class="pi pi-plus" aria-hidden="true"></i>
  <span>Add step</span>
</button>
```

- Use `aria-hidden="true"` on decorative icons when needed and let button labels provide the name.

### Finding 4.3 - Icon-only buttons often lack accessible names

Priority: High. Effort: Small, 1-2 days.

WCAG: 4.1.2 Name, Role, Value, 2.5.3 Label in Name.

What exists today:

- Many icon-only `p-button`s omit `ariaLabel`, including delete/remove buttons in annotations (`annotations-section.ts:95`, `annotations-section.ts:118`, `annotations-section.ts:147`, `annotations-section.ts:163`), story relationships (`story-editor.ts:139`, `story-editor.ts:173`), use-case relationships (`use-case-editor.ts:188`, `use-case-editor.ts:248`, `use-case-editor.ts:287`, `use-case-editor.ts:325`), actor relationships (`actor-editor.ts:121`), admin rows (`global-tags.ts:73`, `tag-categories.ts:81`), and scenario steps (`scenario-editor.ts:154`, `scenario-editor.ts:168`, `scenario-editor.ts:172`, `scenario-editor.ts:176`).
- Some buttons have tooltip text but tooltips are not a substitute for accessible names (`scenario-editor.ts:156`, `scenario-editor.ts:170`, `use-case-editor.ts:190`).

Problems:

- Screen readers may announce only "button" or the icon class, making destructive actions unclear.

Recommendations:

- Add `ariaLabel` to every icon-only button, with row context when possible:

```html
<p-button
  icon="pi pi-times"
  severity="danger"
  [text]="true"
  [ariaLabel]="'Remove goal ' + goal.name"
  (onClick)="removeGoal(goal)" />
```

### Finding 4.4 - Form labels and error associations are incomplete

Priority: High. Effort: Large, tied to reactive forms migration.

WCAG: 1.3.1 Info and Relationships, 3.3.1 Error Identification, 3.3.2 Labels or Instructions, 3.3.3 Error Suggestion, 4.1.3 Status Messages.

What exists today:

- Most primary fields have visible labels (`goal-editor.ts:77`, `story-editor.ts:80`, `scenario-editor.ts:94`, `use-case-editor.ts:84`).
- Some mini-form inputs use `aria-label` with no visible label (`tag-selector.ts:63` through `tag-selector.ts:72`, `global-tags.ts:49` through `global-tags.ts:52`, `tag-categories.ts:50` through `tag-categories.ts:61`).
- Page-level `p-message` displays errors but fields do not set `aria-invalid`/`aria-describedby`.
- Search fields use generic `aria-label="Search"` in `list-page` and `entity-selector-dialog` (`list-page.ts:40` through `list-page.ts:42`, `entity-selector-dialog.ts:58` through `entity-selector-dialog.ts:60`).

Problems:

- Screen reader users hear that a form has an error but not which field caused it.
- Multiple "Search" controls can be ambiguous.
- Placeholder text is used as instruction in many fields and disappears as users type.

Recommendations:

- Use visible labels for add-row mini-forms or group them under a labeled fieldset/legend.
- Add specific search labels: "Search goals", "Search entities", "Search open issues".
- Add field errors with `aria-describedby`.
- Add `role="alert"` on blocking `p-message` or wrap with a live region.

### Finding 4.5 - Custom dialogs and overlays miss modal accessibility guarantees

Priority: High. Effort: Medium, 2-4 days.

WCAG: 2.1.2 No Keyboard Trap, 2.4.3 Focus Order, 2.4.7 Focus Visible, 4.1.2 Name, Role, Value.

What exists today:

- PrimeNG `p-dialog` is used for entity selectors and PAT creation (`entity-selector-dialog.ts:53`, `scenario-selector-dialog.ts:51`, `api-tokens.ts:91`).
- Goal relation type uses a handcrafted `.relation-type-dialog` fixed overlay (`goal-editor.ts:177` through `goal-editor.ts:190`).
- Scenario step details use a handcrafted `.edit-popup-overlay` fixed overlay (`scenario-editor.ts:201` through `scenario-editor.ts:224`).

Problems:

- Custom overlays do not declare `role="dialog"`, `aria-modal`, labelled-by relationships, focus trap, Escape behavior, or focus restore.
- Outside click closes the dialog, but keyboard users may not have equivalent behavior.

Recommendations:

- Replace custom overlays with `p-dialog [modal]="true" [focusOnShow]="true"`.
- Add `ariaLabelledBy`/header and return focus to the opener after close.
- For destructive confirmations, keep using PrimeNG ConfirmDialog, but ensure accept/reject labels are explicit.

### Finding 4.6 - Async and SSE updates are not announced

Priority: Medium. Effort: Medium, 3-5 days.

WCAG: 4.1.3 Status Messages, 2.2.2 Pause, Stop, Hide where continuous updates become distracting.

What exists today:

- The layout opens the SSE connection on init (`layout.ts:157` through `layout.ts:160`).
- Sidebar reloads project counts on Project stream events (`sidebar-nav.ts:268` through `sidebar-nav.ts:272`).
- Editors subscribe to entity streams and reload data (`goal-editor.ts:328` through `goal-editor.ts:334`, `actor-editor.ts:302` through `actor-editor.ts:308`, `scenario-editor.ts:426` through `scenario-editor.ts:432`, `use-case-editor.ts:513` through `use-case-editor.ts:519`).
- The event stream exposes connection state signals (`event-stream.service.ts:38` through `event-stream.service.ts:40`), but no UI displays or announces it.

Problems:

- Users do not know when a background update changed counts/content.
- Screen reader users do not receive status messages.
- Some reload paths avoid overwriting unsaved edits, but the user is not told a newer version exists (`goal-editor.ts:314` through `goal-editor.ts:317`, `scenario-editor.ts:404` through `scenario-editor.ts:409`).

Recommendations:

- Add a global live region service for status messages:

```html
<div class="sr-only" aria-live="polite" aria-atomic="true">{{ liveMessage() }}</div>
```

- Announce "Project list updated", "Goal updated by another user", or "New version available; save or reload."
- Show non-modal inline update banners in editors when SSE updates are skipped due to local edits.

### Finding 4.7 - Color contrast, color-only meaning, reduced motion, and target size need policy

Priority: Medium. Effort: Medium, 3-5 days for policy and fixes; ongoing in design system.

WCAG: 1.4.1 Use of Color, 1.4.3 Contrast Minimum, 1.4.11 Non-text Contrast, 2.3.3 Animation from Interactions, 2.5.8 Target Size (Minimum).

What exists today:

- Hard-coded and fallback badge colors encode meaning (`annotations-section.ts:241` through `annotations-section.ts:249`, `open-issues.ts:113` through `open-issues.ts:114`).
- Drag/drop transitions exist in scenario steps (`scenario-editor.ts:265` through `scenario-editor.ts:266`, `scenario-editor.ts:278`).
- Header white-on-`#1a1a7e` likely passes contrast, but it is outside semantic token governance (`layout.ts:79` through `layout.ts:80`).
- Some small text/icon controls are likely below comfortable target size, such as chip remove and small text buttons (`tag-selector.ts:90` through `tag-selector.ts:95`, `annotations-section.ts:240`).

Problems:

- Without tokenized contrast pairs, future theme changes can break AA.
- Some meanings rely partly on color (issue/resolved/argument support).
- Reduced-motion users get drag/drop and hover transitions without a motion policy.

Recommendations:

- Verify token color pairs with axe and manual contrast tooling.
- Add labels/icons in addition to color for state.
- Add reduced-motion CSS:

```scss
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    scroll-behavior: auto !important;
    transition-duration: 0.01ms !important;
  }
}
```

- Set minimum hit areas for custom controls: at least 24 by 24 CSS pixels under WCAG 2.2 AA, preferably 36-40 px for app ergonomics.

### PrimeNG accessibility notes

PrimeNG helps with:

- `p-dialog` modal semantics and focus behavior when used correctly (`entity-selector-dialog.ts:53`, `api-tokens.ts:91`).
- `p-table` keyboard and ARIA support better than hand-rolled tables when selection/sorting are configured (`project-list.ts:59` through `project-list.ts:61`).
- `p-button`, `p-select`, `p-password`, `p-checkbox`, `p-inputNumber`, and `p-message` provide baseline roles/states.

Requel must still add:

- Correct labels, `inputId`, `ariaLabel`, and descriptions for PrimeNG controls.
- Real links/buttons for custom navigation.
- Field error associations and live regions.
- Tokenized contrast and reduced-motion policy.
- Automated tests; component libraries do not guarantee app-level WCAG compliance.

Automated test setup recommendation:

```bash
npm install -D @axe-core/playwright
```

```ts
// e2e/accessibility.spec.ts
import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

test('goals list has no critical accessibility violations', async ({ page }) => {
  await page.goto('/projects/demo/goals');
  const results = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'])
    .analyze();
  expect(results.violations.filter(v => v.impact === 'critical')).toEqual([]);
});
```

Highest-impact a11y fixes:

1. Add skip link and `h1` page shell.
2. Replace click-only anchors/divs with real links/buttons.
3. Add `ariaLabel` to icon-only buttons.
4. Add reactive form field errors with `aria-invalid` and `aria-describedby`.
5. Replace custom overlays with `p-dialog`.
6. Add a global live region for save/error/SSE status.

## 5. Front-End Architecture and Efficiency

### Finding 5.1 - The app uses standalone/lazy routes well, but route groups need structure

Priority: Medium. Effort: Medium, 3-5 days.

What exists today:

- The app uses standalone components and route-level `loadComponent` for most features (`app.routes.ts:37` through `app.routes.ts:61`).
- Login, layout, and dashboard are eager imports (`app.routes.ts:25` through `app.routes.ts:27`, `app.routes.ts:30` through `app.routes.ts:36`).
- Build budgets are configured (`angular.json:39` through `angular.json:49`).

Problems:

- Flat route configuration is long and hard to scan.
- Feature modules are not NgModules, appropriately, but route constants per feature would improve ownership.
- There is no route data for titles, breadcrumbs, or required permissions that a page shell could consume.

Recommendations:

- Split route arrays by domain (`projectRoutes`, `adminRoutes`, `accountRoutes`).
- Add route `data` for title, section, artifact type, and breadcrumb metadata.
- Keep lazy `loadComponent`; consider lazy route children if sections grow.

### Finding 5.2 - Signals are used, but form/state hygiene is mixed

Priority: High. Effort: Large, paired with form migration.

What exists today:

- Many components use signals for loading, errors, permissions, and entity state (`goal-editor.ts:229` through `goal-editor.ts:236`, `use-case-editor.ts:391` through `use-case-editor.ts:404`).
- Many form fields are mutable class fields so `[(ngModel)]` works (`user-editor.ts:155` through `user-editor.ts:163`, `goal-editor.ts:238` through `goal-editor.ts:239`).
- Some components force `detectChanges()` due to PrimeNG timing and mutable state (`user-editor.ts:226` through `user-editor.ts:230`, `settings.ts:122` through `settings.ts:125`).

Problems:

- State is split across signals, plain fields, `NgForm`, and manual dirty flags.
- Dirty checking is implemented differently per editor.
- Signal benefits are limited when templates bind to mutable fields.

Recommendations:

- Use reactive forms for mutable form state.
- Keep server entity and permissions in signals.
- Derive dirty/valid/submittable state from form controls.
- Remove manual `trackChanges()` where the form can provide `dirty`.

### Finding 5.3 - Change detection and subscriptions are not modernized

Priority: Medium. Effort: Medium, 4-7 days.

What exists today:

- No `ChangeDetectionStrategy.OnPush` appears in source; `rg` found none.
- No `provideZoneChangeDetection` or zoneless configuration appears in app config (`app.config.ts:30` through `app.config.ts:37`).
- Components manually store and unsubscribe `Subscription`s (`sidebar-nav.ts:252` through `sidebar-nav.ts:253`, `goal-editor.ts:252` through `goal-editor.ts:253`, `scenario-editor.ts:341` through `scenario-editor.ts:342`, `use-case-editor.ts:436` through `use-case-editor.ts:437`).

Problems:

- Default change detection is simpler but less efficient for a data-heavy table/editor app.
- Manual subscription cleanup repeats boilerplate and can be missed in future code.
- Zoneless readiness is blocked by mutable forms and manual `detectChanges()`.

Recommendations:

- Add `changeDetection: ChangeDetectionStrategy.OnPush` to leaf components first.
- Replace manual subscriptions with `takeUntilDestroyed(inject(DestroyRef))`.
- Consider `toSignal()` for route params and stream-derived state where practical.
- Evaluate zoneless only after OnPush + reactive forms + PrimeNG behavior are stable.

Example:

```ts
private readonly destroyRef = inject(DestroyRef);
private readonly route = inject(ActivatedRoute);

ngOnInit(): void {
  this.route.paramMap
    .pipe(takeUntilDestroyed(this.destroyRef))
    .subscribe(params => {
      this.projectName.set(params.get('name') ?? '');
    });
}
```

### Finding 5.4 - SSE service is thoughtful but disconnected from UX and app-level state

Priority: Medium. Effort: Medium, 4-6 days.

What exists today:

- `EventStreamService` uses `fetch` with authorization header because native `EventSource` cannot send JWT headers (`event-stream.service.ts:27` through `event-stream.service.ts:33`).
- It tracks connection state and session id with signals (`event-stream.service.ts:38` through `event-stream.service.ts:40`).
- It handles reconnect with exponential backoff (`event-stream.service.ts:246` through `event-stream.service.ts:260`).
- Editors reload on stream events; some avoid overwriting unsaved changes (`goal-editor.ts:314` through `goal-editor.ts:317`, `scenario-editor.ts:404` through `scenario-editor.ts:409`).

Problems:

- Subscription requests do not check response status (`event-stream.service.ts:90` through `event-stream.service.ts:123`).
- The reconnect path retains only the initial subscription list, while dynamic additions depend on server session continuity; if a new session is created, dynamic subscriptions may be lost unless the server preserves them.
- UI does not surface connection problems or skipped updates.

Recommendations:

- Track active subscriptions client-side in a signal/set and replay them on reconnect.
- Check add/remove subscription response status and expose recoverable errors.
- Add a stream status badge in the layout only when degraded.
- Add live region announcements for background updates.

### Finding 5.5 - Shared components exist but are too thin for the app's repeated patterns

Priority: High. Effort: Large, 2-4 weeks incrementally.

What exists today:

- `ListPageComponent` wraps title, actions, and search (`list-page.ts:28` through `list-page.ts:47`).
- Entity selector, scenario selector, tag selector, and annotations section are shared (`entity-selector-dialog.ts:48`, `scenario-selector-dialog.ts:46`, `tag-selector.ts:34`, `annotations-section.ts:32`).
- Editors still repeat page headers, form grids, action rows, relationship tables, errors, and confirmation behavior.

Problems:

- The shared layer does not enforce accessibility or visual consistency.
- Repeated sections increase bug surface area and slow down design changes.

Recommendations:

- Build a shared UI/pattern layer:
  - `PageShellComponent`
  - `EntityListComponent`
  - `EntityEditorShellComponent`
  - `FieldComponent`
  - `RelationshipSectionComponent`
  - `EmptyStateComponent`
  - `CommandMessageComponent`
  - `StatusLiveRegionService`
- Make the shared layer responsible for:
  - headings and breadcrumbs,
  - action placement,
  - responsive layout,
  - field error markup,
  - empty/loading/error states,
  - icon-button labels.

### Finding 5.6 - Bundle and dependency posture is reasonable but should be measured

Priority: Low. Effort: Small, 1 day.

What exists today:

- Dependencies are limited: Angular, CDK, PrimeNG, PrimeIcons, RxJS (`package.json:17` through `package.json:30`).
- Playwright and testing libraries are already available (`package.json:36` through `package.json:46`).
- Production build budgets exist (`angular.json:39` through `angular.json:49`).

Problems:

- No visible bundle analyzer script.
- PrimeNG imports are per component, which is good, but repeated component code may increase compiled template size.

Recommendations:

- Add `npm run build -- --stats-json` or Angular equivalent and inspect output periodically.
- Add a release gate for production budget warnings.
- Prefer shared components/patterns to reduce template duplication.

## Proposed Phased Roadmap for `release/2.0`

### Phase 1 - Quick wins and a11y blockers

Size: 5-8 small issues, 1 sprint.

Suggested GitHub issues:

1. Add app skip link, `main#main-content`, and one `h1` per route via a page shell. Priority High, effort Small.
2. Replace click-only anchors/div controls with `routerLink` anchors or buttons in artifact editors. Priority High, effort Medium.
3. Add `ariaLabel` to every icon-only `p-button`. Priority High, effort Small.
4. Replace custom goal relation and scenario step overlays with `p-dialog`. Priority High, effort Medium.
5. Add reduced-motion CSS and minimum custom-control target sizing. Priority Medium, effort Small.
6. Add `@axe-core/playwright` smoke tests for login, layout, one list, one editor, and dialogs. Priority High, effort Medium.

### Phase 2 - Design-system foundation

Size: 5-7 medium issues, 1-2 sprints.

Suggested GitHub issues:

1. Add `RequelPreset` via `definePreset` with semantic tokens and dark-mode selector. Priority High, effort Medium.
2. Add global Requel design tokens for spacing, radius, type scale, layout widths, and focus. Priority High, effort Small.
3. Replace hard-coded header and chip/status colors with theme tokens. Priority High, effort Medium.
4. Create `PageShellComponent`, `SectionComponent`, `EmptyStateComponent`, and `CommandMessageComponent`. Priority High, effort Medium.
5. Convert `ListPageComponent` to include project context, specific search labels, loading/empty/error slots, and responsive toolbar. Priority Medium, effort Medium.

### Phase 3 - Forms and validation remediation

Size: 6-10 medium/large issues, 2-4 sprints.

Suggested GitHub issues:

1. Add shared reactive form field component with `aria-invalid`, `aria-describedby`, helper text, and error text. Priority High, effort Medium.
2. Add command violation adapter to map backend field violations to controls. Priority High, effort Medium.
3. Migrate login, project editor, account editor, and user editor to reactive forms. Priority High, effort Large.
4. Migrate named artifact editors: goal, story, actor, stakeholder, scenario, use-case, term, report. Priority High, effort Large.
5. Migrate tag/annotation/admin mini-forms to a shared inline-create pattern. Priority Medium, effort Medium.
6. Standardize save/cancel/dirty/submitting behavior across editors. Priority High, effort Medium.

### Phase 4 - IA and workflow polish

Size: 5-8 medium issues, 1-2 sprints.

Suggested GitHub issues:

1. Build a project workspace dashboard with artifact counts, open issues, recent activity, and quick actions. Priority High, effort Medium.
2. Add breadcrumbs and project context to all project-scoped pages. Priority High, effort Medium.
3. Create reusable relationship sections for goals/actors/stories/scenarios/use-cases/stakeholders. Priority Medium, effort Large.
4. Add create-and-link flows to entity selector dialogs where safe. Priority Medium, effort Medium.
5. Standardize list-name links, row actions, search/filter behavior, and empty states. Priority High, effort Medium.

### Phase 5 - Deeper Angular architecture refactors

Size: 5-7 medium/large issues, 2+ sprints.

Suggested GitHub issues:

1. Add `ChangeDetectionStrategy.OnPush` to shared components and low-risk leaf pages. Priority Medium, effort Medium.
2. Replace manual `Subscription` fields with `takeUntilDestroyed`. Priority Medium, effort Medium.
3. Track and replay SSE active subscriptions on reconnect; expose stream degraded state. Priority Medium, effort Medium.
4. Evaluate zoneless change detection after forms and PrimeNG behavior are stable. Priority Low, effort Medium.
5. Add bundle stats reporting and keep budgets visible in CI. Priority Low, effort Small.

## Appendix - Source Coverage Notes

Reviewed files included:

- App bootstrap, config, routes, styles: `main.ts`, `app.config.ts`, `app.routes.ts`, `styles.scss`, `index.html`.
- Shell/auth: `layout.ts`, `login.ts`, `dashboard.ts`.
- Shared UI: `list-page.ts`, `sidebar-nav.ts`, `entity-selector-dialog.ts`, `scenario-selector-dialog.ts`, `tag-selector.ts`, `annotations-section.ts`.
- Project artifacts: projects, stakeholders, goals, stories, actors, scenarios, use cases, terms, reports, open issues.
- User/admin/settings: user list/editor, edit account, settings, API tokens, global tags, tag categories.
- Core architecture: command service, event stream service, route guards/services as needed.

