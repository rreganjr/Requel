# UI Design Guide — Requel Angular Frontend

This document defines the layout, navigation, visual design, and component patterns
for the Angular replacement of the Echo2 UI. It is the source of truth for all UI
implementation decisions.

---

## 1. Page Layout

The application uses a fixed **header + sidebar + main content** layout.

```
+--------------------------------------------------------------+
|  [Logo] REQUEL                          [Account Menu (...)] |
+-------------+------------------------------------------------+
|             |                                                |
|  Sidebar    |  Main Content Area                             |
|  (accordion |                                                |
|   nav)      |  - List tables                                 |
|             |  - Editor forms                                |
|             |  - Detail views                                |
|             |                                                |
|             |                                                |
+-------------+------------------------------------------------+
```

- **Header**: full width, fixed top, ~48px height
- **Sidebar**: fixed left, ~280px width, scrolls independently, collapsible (future)
- **Main content**: fills remaining space, scrolls independently

---

## 2. Header

### Structure
- **Left**: Logo image + "REQUEL" app name text
  - Logo: `logo_fembot_demun_mini.png` (robot figure + styled text)
  - Clicking logo/app name navigates to the default landing page
- **Right**: Account menu button
  - Shows a compact trigger (user icon + "Menu" label or hamburger icon)
  - Dropdown contains: Edit Account, separator, Logout
  - Future: Help link in dropdown

### Design Notes
- No top-level navigation links in the header — all navigation lives in the sidebar
- Header background uses the primary brand color (dark blue, see Color section)
- Logo and text are white/light against the dark header

---

## 3. Sidebar Navigation

The sidebar uses an **accordion** layout with collapsible panels. Panel visibility
is role-based.

### Accordion Panels (top to bottom)

#### 3.1 Admin Panel (admin users only)
Visible when the user has `SystemAdminUserRole`.

- **Header**: "Admin" with a settings/cog icon
- **Content**:
  - **List Users** link — opens the user list table in main content
  - **Create User** link — opens the user editor in create mode

#### 3.2 Projects Panel (project users only)
Visible when the user has `ProjectUserRole`.

- **Header**: "Projects" with a folder icon
- **Content** (top section — actions):
  - **New** button — opens the project editor in create mode; **only shown** if the user
    has the `createProjects` permission on their `ProjectUserRole`
  - **Import** button — triggers XML file upload; **only shown** if the user
    has the `createProjects` permission (import creates a new project)
  - **List** link — opens the searchable project list table in main content
- **Content** (below actions — project tree):
  - Only projects where the user is a `UserStakeholder` appear in the tree
    (admins with `SystemAdminUserRole` see all projects)
  - Projects are ordered by **recent activity** (most recent first), not
    alphabetically. The **List** view (main content) is the place for full
    alphabetical/searchable access; the sidebar tree is a *working set*.
  - Each project the user has access to appears as a collapsible tree node
  - **Project name** (clickable) — opens the project detail/editor in main content
  - **Expand arrow** — toggles showing child entity groups:
    - Stakeholders (count)
    - Goals (count)
    - Stories (count)
    - Actors (count)
    - Use Cases (count)
    - Glossary (count)
  - Each child group is a single non-expandable item showing the entity type name
    and count badge. Clicking opens a searchable list table for that entity type
    in main content.

#### 3.2.1 Project Activity & Recency (TODO — design decisions needed)

The sidebar tree should surface the projects the user is most likely to care about
right now. This requires tracking "activity" per project, but what counts as
activity and for whom is an open question.

**Activity sources (candidates):**

| Activity | Who cares? | Notes |
|----------|-----------|-------|
| User's own edits to project entities | The editing user | Strongest signal — "I was just working on this" |
| Edits by *other* stakeholders on the same project | All stakeholders | Weaker signal — may cause noise if a project has many active editors |
| Annotation responses (future) — someone replies to your issue, position, or argument | The original author | High-value signal — this is a direct interaction with *you* |
| New annotations/issues on entities you edited | The entity editor | Medium signal — someone is questioning or discussing your work |
| Being added as a stakeholder to a new project | The new stakeholder | One-time event — the project should appear prominently at first |

**Decisions:**

1. **Whose activity?** Recent means "recently edited by anyone" — any edit to
   any entity within the project counts. This is a collaborative tool; if other
   stakeholders are actively working on a project you're part of, that project
   should float to the top.

2. **Activity scope:** A user only cares about activity on entity types they can
   see. If a stakeholder has no permissions for Scenarios, then Scenario edits
   should not affect that project's recency for them. Activity that's invisible
   to a user shouldn't drive their sidebar ordering.

3. **Sidebar tree cap:** The tree caps at a configurable number of projects
   (default 10). When the user has more projects than the cap, show the N most
   recent with a "Show all" link that opens the full project list table in main
   content. The cap is stored in user preferences (see below).

4. **Staleness:** Projects with no recent activity are hidden from the sidebar
   tree. The staleness threshold is configurable per user (see `sidebarProjectStaleness`
   in §3.2.3). Options: 1, 3, 6, 9, 12 months, or "Always show". Default: 3 months.
   Projects hidden by staleness still appear in the full project list table
   (the "Show all" link and List view).

#### 3.2.3 User Preferences (separate from User entity)

UI configuration like the sidebar project cap does not belong on the `User` class.
`User` is concerned with identity, authentication, and contact info. UI preferences
are a different concern with a different change cadence — adding a new preference
shouldn't touch the auth model.

**Design:**
- A separate `UserPreferences` entity/class, associated 1:1 with a `User`
- The `User` class does not reference or depend on `UserPreferences` — the
  relationship is looked up separately, not navigated from User
- `UserPreferences` is its own aggregate — persisted in its own table
  (`user_preferences`), loaded by its own repository or service

**Initial preference fields:**

| Preference | Type | Default | Description |
|------------|------|---------|-------------|
| `sidebarProjectLimit` | int | 10 | Max projects shown in sidebar tree |
| `sidebarProjectStaleness` | enum | `3_MONTHS` | Hide projects with no activity older than this. Values: `1_MONTH`, `3_MONTHS`, `6_MONTHS`, `9_MONTHS`, `12_MONTHS`, `ALWAYS` |

> Additional preferences will be added as the UI evolves (e.g., sidebar panel
> collapse state, default sort orders, theme). The structure should accommodate
> key-value pairs or typed fields — decide when more preferences emerge.

**API surface:**
- `GET /api/user-preferences` — current user's preferences
- `PUT /api/user-preferences` — update current user's preferences
- Preferences loaded once on login, cached client-side

See `doc/UI_REFACTOR_PLAN.md` for implementation phasing.

**Tracking mechanism:**

The per-user activity scope decision rules out a single `lastActivityAt` column
on the project — different stakeholders with different permissions would compute
different recency for the same project. Instead:

- **Per-entity-type activity timestamps on the project.** Track a
  `lastActivityAt` per entity type within the project. This could be:
  - A `project_entity_activity` table: `(project_id, entity_type, last_activity_at)`
    — one row per project × entity type, updated on any create/edit/delete of
    that entity type within the project (~10 entity types × N projects)
  - Or columns on the project itself (`lastGoalActivityAt`,
    `lastStoryActivityAt`, etc.) — simpler but rigid
- **Query computes per-user recency.** When loading the sidebar tree, the
  server joins the user's stakeholder permissions against the activity timestamps
  and computes `MAX(last_activity_at)` across only the entity types the user has
  at least one permission for. This becomes the sort key.
- **Update mechanism.** After any command that creates/edits/deletes a project
  entity, upsert the `project_entity_activity` row for that entity type. This
  can be done in the command itself, or as a lightweight post-commit hook in the
  command handler chain (similar to how `AnalysisInvokingCommandHandler` triggers
  NLP after writes).

**Example:** Stakeholder A has permissions for Goals and Stories. Stakeholder B
has permissions for Goals and Scenarios. A Goal edit at 3pm and a Scenario edit
at 4pm would give: A sees the project with recency 3pm (Goals), B sees recency
4pm (Scenarios). Both see the Goal edit, but only B sees the Scenario edit for
sorting purposes.

#### 3.2.2 Notification Indicators (future — with annotation/discussion feature)

When the annotation/IBIS discussion feature is implemented (see Section 9), the
sidebar should indicate that a project has **unread responses** directed at the
current user.

**What triggers a notification indicator:**
- Someone adds a Position to an Issue you created
- Someone adds an Argument to a Position you created
- Someone adds an Issue on an entity you created or last edited
- (Future) explicit @-mention in annotation text

**Visual treatment:**
- A small badge or dot on the project name in the sidebar tree (e.g., PrimeNG
  `p-badge` with severity `danger`, or a simple colored dot)
- The project should sort to the top of the tree (or at least above projects
  with no notifications)
- Clicking into the project clears the indicator (or navigating to the specific
  annotation does)

**Data model implications:**
- Requires a lightweight notification/unread-tracking mechanism — likely a
  `user_notifications` table with (user, project, annotation, read/unread, timestamp)
- The sidebar tree API (`GET /api/projects` or a new endpoint) would include a
  `hasUnread: boolean` or `unreadCount: number` field per project
- Polling or SSE push to keep the indicator current without manual refresh

> **TODO:** Design the notification data model and API surface when the
> annotation feature (Section 9) is being implemented. The sidebar indicator
> is a consumer of that system, not the driver.

### Sidebar Behavior
- Accordion allows multiple panels open simultaneously
- Panel state persists during session (future: localStorage)
- Project tree loads on login/navigation and refreshes after mutations
- Sidebar has a subtle right border separating it from main content

---

## 4. Main Content Area

The main content area displays one view at a time, driven by route navigation
from sidebar clicks or direct URL entry.

### View Types

#### 4.1 List Table View
Used for: Users, Projects, Stakeholders, Goals, Stories, Actors, Use Cases, Glossary Terms

- **Page header**: Entity type name (e.g., "Users", "Goals for [Project Name]")
- **Search bar**: Text input for filtering the table (searches across visible columns)
- **Table**: PrimeNG DataTable with:
  - Sortable column headers
  - Row hover highlight
  - Row click navigates to editor
  - Pagination (20 rows default)
- **Action buttons**: Contextual (e.g., "New Goal" for goals within a project)

#### 4.2 Editor/Detail View
Used for: User editor, Project editor, Goal editor, Story editor, etc.

- **Page header**: "[Entity Type]: [Name]" or "New [Entity Type]"
- **Form layout**: Two-column grid for short fields, full-width for text areas
- **Action bar**: Save and Cancel buttons at the bottom of the form
- **Feedback**: Success/error messages displayed above the form using PrimeNG Message
- **Unsaved changes guard**: All editors must protect against accidental data loss
  when the user navigates away from a dirty form (see §7.10 Unsaved Changes Guard)

#### 4.3 Annotation/Discussion View (future)
Used for: IBIS-style issues, positions, arguments on any entity

- Will be a standard panel/sidebar that attaches to any editor view
- Described in Section 9

---

## 5. Color Palette

> **TODO**: Finalize palette. Starting values derived from the Echo2 logo/branding.

### Brand Colors
| Token                 | Value       | Usage                                    |
|-----------------------|-------------|------------------------------------------|
| `--rq-brand-primary`  | `#1a1a7e`   | Header background, primary actions        |
| `--rq-brand-light`    | `#3b3bbf`   | Hover states on primary elements          |
| `--rq-brand-dark`     | `#0e0e52`   | Active/pressed states                     |

### Semantic Colors
| Token                 | Value       | Usage                                    |
|-----------------------|-------------|------------------------------------------|
| `--rq-success`        | (PrimeNG)   | Success messages, save confirmation       |
| `--rq-warning`        | (PrimeNG)   | Validation warnings                       |
| `--rq-danger`         | (PrimeNG)   | Error messages, delete actions            |
| `--rq-info`           | (PrimeNG)   | Informational badges, hints               |

### Surface Colors
| Token                 | Value       | Usage                                    |
|-----------------------|-------------|------------------------------------------|
| `--rq-sidebar-bg`     | (TBD)       | Sidebar background                        |
| `--rq-header-bg`      | `#1a1a7e`   | Header background (= brand primary)       |
| `--rq-header-text`    | `#ffffff`   | Header text and icons                     |
| `--rq-content-bg`     | (PrimeNG)   | Main content background (surface-ground)  |
| `--rq-border`         | (PrimeNG)   | Borders, dividers (surface-200)           |

### Notes
- Defer to PrimeNG Aura preset tokens where possible (`--p-*` variables)
- Custom tokens (`--rq-*`) are only for Requel-specific branding
- Dark mode is out of scope for initial release

---

## 6. Typography

> **TODO**: Finalize font choices.

| Element               | Font                          | Size / Weight            |
|-----------------------|-------------------------------|--------------------------|
| Body text             | PrimeNG default (system font) | 14px / 400               |
| Page header (h2)      | PrimeNG default               | 24px / 600               |
| Section header (h3)   | PrimeNG default               | 18px / 600               |
| Sidebar panel header  | PrimeNG default               | 14px / 600               |
| Sidebar tree item     | PrimeNG default               | 13px / 400               |
| Table header          | PrimeNG default               | 13px / 600               |
| Table body            | PrimeNG default               | 14px / 400               |
| Form label            | PrimeNG default               | 14px / 500               |
| Button label          | PrimeNG default               | 14px / 500               |

### Notes
- Rely on PrimeNG's `--p-font-family` for consistency
- No custom web fonts in initial release — system font stack is fine

---

## 7. Component Patterns

### 7.1 Buttons

| Variant       | PrimeNG Severity | Usage                                     |
|---------------|------------------|-------------------------------------------|
| Primary       | (default)        | Save, Submit, primary action per view      |
| Secondary     | `secondary`      | Cancel, Back, alternative actions          |
| Danger        | `danger`         | Delete, destructive actions                |
| Text/Link     | `[text]="true"`  | Inline actions, sidebar links, icon-only   |
| Outlined      | `[outlined]`     | Secondary emphasis (Import, Export)         |

**Rules:**
- One primary button per view (the main action)
- Destructive actions require confirmation dialog
- Loading state on buttons during async operations
- Disabled when form is pristine (no changes to save)

### 7.2 Sidebar Accordion

**PrimeNG component**: `p-accordion` with `[multiple]="true"`

- Panels have icon + label in header
- Content area has action links/buttons at top, then scrollable content
- Project tree items use `p-tree` within the Projects panel

### 7.3 Data Tables (List Views)

**PrimeNG component**: `p-table`

- Always include:
  - Search/filter input above the table
  - Sortable column headers
  - Row hover + selectable rows (click to navigate)
  - Pagination (default 20 rows)
  - Empty state message
- Optional:
  - Column-level filters for large datasets
  - Multi-select for bulk actions (future)

### 7.4 Editor Forms

- Two-column grid layout for short fields (name, organization, status)
- Full-width rows for text areas (description, body text)
- Labels above inputs (not inline/floating)
- Required fields marked with PrimeNG validation styling
- Action buttons (Save, Cancel) in a row below the form

### 7.5 Page Headers

Every main content view starts with a page header:

```html
<div class="page-header">
  <h2>Page Title</h2>
  <div class="page-actions">
    <!-- contextual action buttons -->
  </div>
</div>
```

- Title on the left, action buttons on the right
- Consistent spacing below before content starts

### 7.6 Section Headers

Used within editors or detail views to group related fields:

```html
<h3 class="section-header">Section Name</h3>
```

- Subtle bottom border or spacing to separate from content
- No excessive visual weight

### 7.7 Search Bar

Standard search input used above list tables:

```html
<div class="search-bar">
  <span class="p-input-icon-left">
    <i class="pi pi-search"></i>
    <input pInputText placeholder="Search..." />
  </span>
</div>
```

- Filters the table client-side by searching across all visible text columns
- Debounced input (200-300ms) to avoid excessive re-renders

### 7.8 Count Badges

Used in sidebar tree items to show entity counts:

```html
<span class="entity-count">{{ count }}</span>
```

- Small, muted text or PrimeNG `p-badge` with `severity="secondary"`
- Appears next to entity type name: "Goals (12)"

### 7.9 Feedback Messages

**PrimeNG component**: `p-message`

- Displayed above the form/table content area
- Severity: `success`, `error`, `warn`, `info`
- Auto-dismiss success messages after 5 seconds (future)
- Error messages persist until user action

### 7.10 Confirmation Dialogs

**PrimeNG component**: `p-confirmDialog`

- Required for: delete operations, discarding unsaved changes
- Shows clear description of the action and its consequences
- Primary button matches the action (e.g., "Delete" with danger severity)

### 7.11 Unsaved Changes Guard

All editor forms must handle navigation away from a dirty (modified) form. This
applies to sidebar clicks, route changes, and any action that would replace the
current editor content.

**Behavior:**
- **Form is clean** (no unsaved changes): navigation proceeds immediately — the
  editor reloads with the new entity
- **Form is dirty** (user has made changes): a confirmation dialog appears:
  - **Header**: "Unsaved Changes"
  - **Message**: "You have unsaved changes. Save before switching?"
  - **"Save & Switch"** button: saves the current form, then navigates to the
    new entity on success
  - **"Cancel"** button: dismisses the dialog and returns the user to the
    current (unsaved) form, restoring the original URL

**Implementation pattern:**
- Subscribe to `route.paramMap` (not `route.snapshot`) so the component reacts
  to same-route param changes (e.g., `/projects/A` → `/projects/B` reuses the
  component)
- Track dirty state via Angular's `NgForm.dirty`
- Use PrimeNG `ConfirmationService.confirm()` for the dialog
- On cancel, navigate back to the original entity URL with `replaceUrl: true`
  to clean up the browser history entry created by the sidebar click
- After loading new data, call `form.markAsPristine()` to reset dirty state

**Applies to:** Project editor, User editor, Stakeholder editor, Goal editor,
Story editor, Actor editor, Use Case editor, Scenario editor, Glossary Term
editor — all forms that allow editing and can be reached via sidebar or in-page
navigation.

---

## 8. Spacing and Layout Tokens

> **TODO**: Finalize specific values. Use PrimeNG spacing scale as baseline.

| Token                   | Value   | Usage                                  |
|-------------------------|---------|----------------------------------------|
| `--rq-spacing-xs`       | 0.25rem | Tight gaps (badge padding)             |
| `--rq-spacing-sm`       | 0.5rem  | Compact gaps (between inline elements) |
| `--rq-spacing-md`       | 1rem    | Standard gap (form fields, sections)   |
| `--rq-spacing-lg`       | 1.5rem  | Between major sections                 |
| `--rq-spacing-xl`       | 2rem    | Page-level padding                     |
| `--rq-sidebar-width`    | 280px   | Sidebar panel width                    |
| `--rq-header-height`    | 48px    | Header bar height                      |

---

## 9. Annotation/Discussion UI (Future — IBIS Model)

Requel supports an IBIS-style discussion layer on any project entity. This will be
a reusable component attached to editor views.

### Structure
```
+------------------------------------------+
| Annotations for: [Entity Name]           |
+------------------------------------------+
| > Issue: "Is this goal testable?"        |
|   > Position: "Reword to include..."     |
|     > Argument (supports): "This wou..." |
|     > Argument (opposes): "Too specif.." |
|   > Position: "Add acceptance criteria"  |
| > Note: "Discussed in meeting 3/10"      |
+------------------------------------------+
| [+ Add Issue]  [+ Add Note]              |
+------------------------------------------+
```

### Design Notes
- Collapsible tree of Issues > Positions > Arguments
- Visual indicators for argument type (supports = green accent, opposes = red accent)
- Inline editing of annotation text
- Issue status indicator (open, resolved)
- Standard component that appears as a panel/section within any entity editor

> **TODO**: Define specific PrimeNG components and layout for annotation UI.

---

## 10. Login Page

- Centered card layout on a full-page background
- Logo prominently displayed above the form
- Username and password fields
- Login button (primary)
- Error message for invalid credentials
- No registration link (admin creates accounts)

---

## 11. Responsive Behavior (Future)

> Not in scope for initial release. The app targets desktop/laptop browsers.

- Sidebar collapses to icon-only or overlay on smaller screens
- Tables switch to card layout on mobile
- Forms stack to single column

---

## 12. PrimeNG Configuration

- **Theme preset**: Aura (via `@primeuix/themes/aura`)
- **Icon library**: PrimeIcons (`primeicons/primeicons.css`)
- **Key components used**: Accordion, Tree, DataTable, InputText, Textarea, Select,
  Password, Button, Checkbox, Message, ConfirmDialog, Badge, Menu/Menubar

### Customization Approach
- Override PrimeNG design tokens via CSS custom properties where needed
- Prefer PrimeNG's built-in `--p-*` variables over hardcoded values
- Define Requel-specific tokens (`--rq-*`) only for branding not covered by PrimeNG

---

## 13. File Organization

```
requel-angular/src/
  app/
    core/              — services, guards, interceptors
    models/            — TypeScript interfaces (DTOs)
    features/
      auth/            — login, layout shell, dashboard
      users/           — user list, user editor, edit account
      projects/        — project list, project editor, project tree sidebar
      (future)
      goals/           — goal list, goal editor
      stories/         — story list, story editor
      actors/          — actor list, actor editor
      usecases/        — use case list, use case editor
      glossary/        — glossary term list, glossary editor
      annotations/     — shared annotation/discussion component
    shared/            — reusable components (search-bar, page-header, etc.)
  assets/
    images/            — logo, icons
  environments/        — API base URL config
```

---

## 14. Implementation Notes

- All components use Angular standalone component pattern
- Two-way binding via `[(ngModel)]` with `FormsModule` (not reactive forms)
- Signals for component state (Angular 17+ signals API)
- Navigation drives content — sidebar clicks update the route, main content
  renders the routed component
- Entity editors receive context from route params (e.g., `/projects/:name`,
  `/projects/:projectName/goals/:goalName`)

### Entity References: ID-Based, Not Name-Based

The shift from name-based to id-based references is a recurring pattern when
moving from server-side UI (Echo2) to client-server API (Angular + REST). Echo2
worked with live Hibernate-managed entity references — identity was implicit in
the object graph. The API layer serializes to JSON, so identity must be explicit.

**Rule:** Every entity reference crossing the API boundary should use `id` for
unambiguous identification. Use `name` only for display or when creating new
entities that don't yet have an id.

- Dropdowns that select existing entities should bind to the entity object (or
  its `id`), not just the display name
- Save payloads send `organizationId`, `projectId`, etc. — not name strings
- DTOs always include `id` and `version` so the client can reference and
  optimistically lock any entity it displays
- When an entity might be new (e.g., a user types a new organization name into
  an editable dropdown), the backend resolves `id` first, falling back to
  find-or-create by name

### Polymorphic DTOs: Nested Type-Specific Objects

When a domain entity has subtypes with divergent fields (e.g., `UserStakeholder`
vs `NonUserStakeholder`), use a shared DTO with a `type` discriminator and nested
type-specific detail objects — not a flat DTO with many nullable fields.

**Pattern:**
```typescript
interface StakeholderDto {
  id: number;
  version: number;
  name: string;
  type: 'user' | 'non-user';
  userDetails: UserStakeholderDetails | null;
  nonUserDetails: NonUserStakeholderDetails | null;
}
interface UserStakeholderDetails {
  username: string;          // all fields non-null within the nested type
  emailAddress: string;
  phoneNumber: string;
  teamName: string | null;
  permissionKeys: string[];
}
interface NonUserStakeholderDetails {
  text: string;
}
```

**Why nested over flat:**

- **TypeScript narrowing works.** Checking `stakeholder.userDetails != null`
  narrows the entire nested object. With a flat DTO, checking
  `stakeholder.type === 'user'` does *not* narrow individual fields —
  `emailAddress` remains `string | null` and requires `!` assertions or casts.
- **One null boundary instead of many.** The null check happens at the nested
  object level, not per-field. Inside the nested type, fields are non-null and
  well-typed.
- **Angular templates use `?.` naturally.** `stakeholder.userDetails?.email`
  renders nothing when null — no explicit `*ngIf` or ternary needed for each
  field in list views.
- **Editors receive strongly-typed data.** A user-stakeholder editor component
  receives `UserStakeholderDetails` as input — no bag of nullable fields to
  filter through.

**When to apply:** Any entity with inheritance or subtypes that cross the API
boundary with different field sets. Input DTOs can remain separate per subtype
since create/edit flows are inherently different.

**Java side:** Use `@JsonInclude(JsonInclude.Include.NON_NULL)` on the outer
record so the null nested object is omitted from JSON entirely.
