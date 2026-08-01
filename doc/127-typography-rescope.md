# #127 — Typography & Hierarchy: Scope Refinement and Ticket Hand-off

Planning note only. This document does **not** change any GitHub state; apply the `gh`
commands in §7 when ready (Claude runs them only when explicitly told).

- Source finding: `doc/UI_UX_REVIEW.md` §1.3 — "Typography and hierarchy are too flat"
- Target look-and-feel: `doc/124-lookandfeel-plan.md`
- Phase order: `doc/124-remediation-rollup.md` — Phase 2 (design-system foundation)
- Epic: [#124](https://github.com/rreganjr/Requel/issues/124); milestone `v2.0`;
  project **Requel 2.0**; labels `ui-ux-review` + `priority:medium`

## 1. Summary

Issue #127 is well-formed but three of its acceptance criteria reach into work that other
sub-issues own — specifically project/entity context (breadcrumbs, top-bar, workspace
route) and a list toolbar component. This note pins #127 to the **presentation primitive
plus type tokens**, and hands the surrounding chrome to #128 and #154, which *consume*
the #127 primitive rather than rebuild it. This keeps the epic's dependency direction
intact: Phase 2 builds tokens and primitives; Phase 4 wires them into the shell and IA.

Doing "full context" inside #127 would **not** close #128 or #154 — each carries
substantial non-typography work — and would build the top bar and breadcrumb twice.

## 2. Scope split across the three tickets

| Ticket | Phase | Owns | Relationship to #127 |
|---|---|---|---|
| **#127** (1.3) | 2 | Semantic type-scale tokens; extend the shared page/header primitive with `eyebrow` + `metadata` slots; restyle the existing `list-page` toolbar | Produces the primitive |
| **#128** (2.1) | 4 | Breadcrumb route/data wiring; `/projects/:name` workspace overview (counts, open issues, recent changes, next actions); editor-header action groups | Consumes the primitive (fills eyebrow/metadata, adds breadcrumb data) |
| **#154** (N1) | 4 | Top-bar chrome (back button, right cluster: search/notifications/account/collapse toggle); grouped collapsible sidebar; collapse-state persistence | Consumes the primitive (renders it inside the new shell) |

The look-and-feel plan already encodes the #128 vs #154 split: N1 is "the shell-chrome
half; #128 keeps the project-context/IA half."

## 3. Already satisfied by adjacent work (verify, do not rebuild)

- **Figtree loaded + base 14px** — done in `requel-angular/src/styles.scss` (self-hosted
  `@fontsource-variable/figtree`); also pinned by #125. Drop "Load Figtree" from the
  net-new list; just confirm it.
- **Single `<h1>` per page** — `app-page-header` already renders one `<h1>` and is
  adopted across editors and lists (from #135). The "one h1 per page" criterion is
  largely met; #127 confirms and restyles it via tokens.
- **List search / actions bar** — `app-list-page` already provides a search bar + actions
  slot used by ~10 list pages. #127 restyles it with the new tokens instead of adding a
  new component. The reusable table toolbar is #157 (`app-data-table`) / #129.

## 4. Net-new work for #127

1. **Semantic type-scale tokens** in `styles.scss` — role tokens (not just size steps),
   each carrying font-size + weight + line-height (+ color where it differs):
   `--rq-text-page-title`, `--rq-text-section-title`, `--rq-text-body`,
   `--rq-text-label`, `--rq-text-helper`, `--rq-text-caption`. Titles are bold slate
   (surface-800/900); helper and caption are muted (surface-500).
2. **Extend the page/header primitive** with an optional `eyebrow` (project name +
   artifact type) and a `metadata` content slot (status / counts / unsaved state).
   Breadcrumbs and top-bar stay out of scope.
3. **Apply the type tokens** through the shared primitives (`page-header`, `list-page`)
   so titles, labels, and helper text read from tokens, not local literals.
4. **Restyle the existing `list-page` toolbar** (search + right-aligned actions) with the
   new density/type tokens and standardize action placement on the two exemplar pages
   below. No new toolbar component.

Exemplar list pages for the toolbar consistency check: **Goals** (`goal-list.ts`) and
**Stories** (`story-list.ts`).

## 5. Revised acceptance criteria for #127

- A tokenized **semantic** type scale exists (page title, section title, body, label,
  helper, caption), each carrying size + weight + line-height, defined in `styles.scss`
  and consumed by shared primitives — no per-view font-size/weight literals in the
  migrated pages.
- The shared page/header primitive exposes `eyebrow` (project + artifact type) and a
  `metadata` slot; route pages keep a single `<h1>`. Breadcrumb/top-bar context is
  deferred to #128 / #154, which consume this primitive.
- The `list-page` toolbar (search + right-aligned actions) reads density/type from
  tokens and uses consistent action placement across **Goals** and **Stories**.
- No regression to the single-`<h1>`-per-page heading structure from #135.

## 6. Dependencies / coordination

- Depends on **#125** (preset tokens: Figtree, surface ramp) landing first — Phase 2
  order.
- Provides the page/header primitive that **#128** and **#154** build on. Both must wire
  breadcrumbs/chrome *into* this primitive rather than replacing it.

## 7. `gh` commands to update the three tickets

Run from the repo root. `commit.md` conventions do not apply — these only edit issue
text. Nothing here touches branches, commits, or PRs.

### 7.1 #127 — replace the body with the refined scope + AC

```bash
gh issue edit 127 --repo rreganjr/Requel --body-file - <<'EOF'
Part of the UI/UX remediation epic (#124). Source: `doc/UI_UX_REVIEW.md` Finding 1.3.
Scope refinement: `doc/127-typography-rescope.md`.

**Priority:** Medium. **Effort:** Small (tokens + primitive + two list-page restyles).
**Phase:** 2 (design-system foundation).

## Scope

Delivers the **type tokens + presentation primitive** only. Project context chrome
(breadcrumbs, top bar, workspace route) is **out of scope** and owned by #128 (context/IA)
and #154 (shell chrome), which consume the primitive this issue produces. See
`doc/127-typography-rescope.md` §2.

## Already in place (confirm, do not rebuild)

- Figtree bundled + base 14px in `styles.scss` (also pinned by #125).
- Single `<h1>` per page via `app-page-header` (from #135).
- `app-list-page` search + actions bar (used by ~10 list pages).

## Net-new work

- Semantic type-scale tokens in `styles.scss`: `--rq-text-{page-title,section-title,body,label,helper,caption}`, each with size + weight + line-height.
- Extend the page/header primitive with an `eyebrow` (project + artifact type) and a `metadata` slot (status / counts / unsaved).
- Apply the tokens through `page-header` and `list-page` (no per-view literals).
- Restyle the existing `list-page` toolbar with the new density/type tokens; standardize action placement on **Goals** (`goal-list.ts`) and **Stories** (`story-list.ts`). No new toolbar component (that is #157 / #129).

## Acceptance criteria

- Tokenized **semantic** type scale (page title, section title, body, label, helper, caption), each with size + weight + line-height, defined in `styles.scss` and consumed by shared primitives — no per-view font-size/weight literals in the migrated pages.
- The shared page/header primitive exposes `eyebrow` and a `metadata` slot; route pages keep a single `<h1>`. Breadcrumb/top-bar context deferred to #128 / #154.
- The `list-page` toolbar reads density/type from tokens with consistent action placement across **Goals** and **Stories**.
- No regression to the single-`<h1>`-per-page structure from #135.

Depends on #125. Provides the primitive consumed by #128 and #154.
EOF
```

### 7.2 #128 — comment: consume the #127 primitive, do not rebuild it

```bash
gh issue comment 128 --repo rreganjr/Requel --body "Scope coordination (see \`doc/127-typography-rescope.md\`): #127 delivers the page/header primitive with \`eyebrow\` + \`metadata\` slots and the semantic type tokens. This issue owns the **context/IA half** — breadcrumb route/data wiring, the \`/projects/:name\` workspace overview, and editor-header action groups — and should fill the #127 primitive's slots rather than introduce a parallel header. Shell chrome (top bar, sidebar) is #154."
```

### 7.3 #154 — comment: render inside the #127 primitive

```bash
gh issue comment 154 --repo rreganjr/Requel --body "Scope coordination (see \`doc/127-typography-rescope.md\`): #127 delivers the page/header primitive (\`eyebrow\` + \`metadata\` slots) and semantic type tokens. This issue owns the **shell-chrome half** — top bar (back button + right cluster) and grouped collapsible sidebar — and should render the #127 primitive inside the new shell rather than re-implement page titles/eyebrows. Breadcrumb route/data + workspace route stay with #128."
```

### 7.4 Optional — link the issues as related

```bash
# Record the dependency direction as issue references (optional; comments above already cross-link).
gh issue comment 127 --repo rreganjr/Requel --body "Provides the page/header primitive consumed by #128 (context/IA) and #154 (shell chrome). Depends on #125 (preset tokens)."
```
