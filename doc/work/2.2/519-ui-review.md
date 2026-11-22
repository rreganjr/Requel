# #519 — UI review report

https://github.com/rreganjr/Requel/issues/519 · plan: [519-ui-review-plan.md](519-ui-review-plan.md)

## Summary

Every route was walked in light and dark, at 1440×900 and 390×844, with axe run on each view:
49 page states × 4 views = 196 views, none unreached. The first walk found 2 critical and 2
serious axe rules on almost every page; after the fixes here the second walk has no critical
violations. The serious violations it still reported (light-mode outlined and text severity
buttons) are fixed in the preset afterwards, and checked in the running app (see F4). 21 findings
are fixed here, 5 filed, 2 won't fix. The report recommends extracting a shared editor shell
(filed as #525).

## How the review ran

- `npm run ui-review` (`requel-angular/e2e/ui-review/ui-review.walk.ts`, its own Playwright
  config, not in CI) against the jar with the dev profile. It imports `doc/samples/Requel.xml`
  fresh, creates an empty project, visits every route in four views, runs axe in each, and writes
  full-height screenshots, axe JSON and `summary.md` to `requel-angular/ui-review/` (gitignored).
  Both projects are deleted at the end. `UI_REVIEW_ONLY=<page>` narrows a run.
- States: populated (every route), empty (the list pages and the workspace, on the empty
  project), error (a goal id that doesn't exist).
- Walk 1 (before fixes) and walk 2 (after) are compared below. Screenshots aren't checked in
  (plan decision 2); each finding names the page, state and view.

## Acceptance criteria

| AC | Result |
|---|---|
| Every route in the report, both themes and both widths | All 37 routes, 49 states, 4 views; none unreached |
| No hard-coded colours outside `theme/` | Done for 15 files; `theme/no-hard-coded-colours.spec.ts` keeps it so |
| API tokens and the relationship section on `app-data-table` | API tokens done. Relationship section: won't fix, see F26 |
| No serious or critical axe violation left, or each filed | Critical 0. Serious: F4 fixed after walk 2 and checked live; the editor not-found state's minor `empty-heading` is filed (F11) |
| Every finding has an outcome | Below |

## Axe, walk 1 → walk 2

| Rule | Impact | Walk 1 (nodes / pages) | Walk 2 | Finding |
|---|---|---|---|---|
| button-name | critical | 2,446 / 47 | 0 | F1, F2 |
| aria-dialog-name | serious | 60 / 13 | 0 | F6 |
| color-contrast | serious | 416 / 48 | 168 / 17, light only | F3, F4, F5 |
| heading-order | moderate | 12 / 3 | 0 | F8 |
| landmark-one-main, region | moderate | 56 / 2 | 0 | F7 |
| empty-table-header | minor | 104 / 24 | 0 | F9 |
| image-redundant-alt | minor | 188 / 47 | 0 | F10 |
| empty-heading | minor | 4 / 1 | 4 / 1 | F11 |

## Findings

Severity: axe's impact for accessibility findings; **major** (blocks or misleads) or **minor**
(looks wrong, works) for the rest.

| # | Page · state · view | Severity | Finding | Outcome |
|---|---|---|---|---|
| F1 | All pages · all | critical | Sidebar tree expand/collapse buttons have no name (PrimeNG renders them with tabindex -1). | Fixed: hidden from assistive tech (`aria-hidden`); the treeitem expands from the keyboard. |
| F2 | Every list · populated · all | critical | The row `⋯` button had no name: its `aria-label` sat on the `p-button` wrapper, not the `<button>`. The unit test checked the wrapper. | Fixed: `[ariaLabel]`; test now checks the button. |
| F3 | 48 pages · all · light | serious | Primary blue-500 as link text and as button background with white text: 3.7:1. | Fixed: light primary 600 (5.2:1), hover 700, active 800. |
| F4 | 17 pages · populated · light | serious | Outlined and text severity buttons (Add Issue, Delete, Add as Actor, Resolve, …) use Aura's 500 stops: 2.3–3.6:1. | Fixed: 700 stops in the preset's light button tokens. Checked in the running app with those values: all 56 button labels on a goal editor pass. Not re-walked. |
| F5 | Open issues, Settings · populated · dark | serious | "Yes" (must resolve) and "REVOKED" used fixed red shades. | Fixed: `--rq-tag-danger-fg`. |
| F6 | 13 pages · all | serious | Closed confirm dialogs left `role="alertdialog"` on PrimeNG's empty host element (also the dictionary dialog). Previously excluded in unit specs as a PrimeNG defect (#139). | Fixed: global `pt` removes the host role; the open dialog's container keeps its role and `aria-labelledby` (unit-tested). |
| F7 | Login, Change password · all | moderate | No main landmark. | Fixed: `<main>`. |
| F8 | Settings, Project editor, Stakeholder editor · all | moderate | An h3 directly under the h1 (token list, Tags, Permissions). | Fixed: h2 section titles. |
| F9 | Every list · all | minor | Empty actions column header. | Fixed: visually hidden "Actions". |
| F10 | All pages · all | minor | Header logo alt "Requel" beside the word "Requel". | Fixed: `alt=""`. |
| F11 | Goal editor (all 8 editors) · error · all | major | A missing id shows "Failed to load" with Retry, still offers Copy/Delete, an empty h1, and Annotations/Sources with their own errors. | Filed: #525 (part of the editor shell). |
| F12 | All pages · all | major | The accent picker never recoloured buttons or links: the preset pinned primary to blue, so only the ramp changed. | Fixed: primary from `{primary.*}`. |
| F13 | All pages · light, emerald or amber accent | serious | With F12 fixed, white text on emerald-600 is 3.8:1 and on amber-600 3.2:1. | Filed: #529. |
| F14 | 15 components | minor | Hex fallbacks and fixed palette stops that don't change in dark mode (drop target, update banner, status colours, …). | Fixed: semantic tokens (new `--rq-tag-info-border`, `--rq-drop-target-*`, `--rq-header-hover-bg`); guard spec. |
| F15 | All editors · populated · narrow | major | Title and Back/Copy/Delete share one row; Delete is cut off at 390px. | Fixed: actions wrap (same edit in 10 editors; see the shell recommendation). |
| F16 | All pages · narrow | minor | The breadcrumb shrinks to "› u… › P…". | Fixed: hidden below 768px; back button and title remain. |
| F17 | Lists · empty, and one page | minor | Paginator with nothing to page through. | Fixed: shown only when rows exceed a page. |
| F18 | All editors · populated · narrow | major | Annotation text squeezed to a word per line beside the creator and actions; section header buttons wrap their labels. | Fixed: text keeps a 16rem basis, meta wraps below; header actions wrap. |
| F19 | Account · populated · all | minor | Form sat on the canvas without a card. | Fixed: `app-card`. |
| F20 | Project workspace · all | minor | "Edit project" (a link styled as a button) underlined, unlike "Delete project". | Fixed. |
| F21 | Definition editor · populated · all | minor | Finding-type rows truncate code and description; labels above fields unlike other editors; rows overflow the card when narrow. | Filed: #527. |
| F22 | Report editor · populated · all | major | Annotations always fail: `Unknown entity type: ReportGenerator`. | Filed: #526. |
| F23 | Global tags, Tag categories, Installation dictionary, Maintenance · all | minor | No card; tables stop at a fixed narrow width. | Filed: #528. |
| F24 | Projects, Users, Settings (tokens) · populated · narrow | minor | Wide tables are cut at the window edge. | Won't fix: the table container scrolls sideways and every column stays reachable; a per-row card layout would be a redesign (out of scope). |
| F25 | Settings · populated · all | minor | API tokens in a raw `<table>`. | Fixed: `app-data-table` (unit and axe test added). |
| F26 | 8 editors, 16 sections | minor | Relationship sections use their own `<table>`. | Won't fix: each editor passes a row template that renders whole cells, while `app-data-table` takes a template per column, so moving it means reworking 16 sections or adding a second rendering mode, and losing the aligned column widths between sibling sections — for sorting and paging that short linked lists don't need. It already has empty states, a named actions column and passes axe. |
| F27 | Walk | — | Screenshots stopped at the window height (the layout scrolls inside its content area). | Fixed: the walk grows the window to the content first. |
| F28 | e2e | — | `e2e/projects.e2e.ts` failed `tsc -p tsconfig.e2e.json` (index-signature access). | Fixed. |

## Shared editor shell: recommended

Extract it (#525, size 5). The eight editors are 724–1,465 lines and repeat the header,
actions, unsaved-changes guard, Save, load/error states, Annotations and Sources. This review hit
that repetition twice: F15 needed the same style edit in ten files, and F11 is wrong identically in
all eight because each editor builds its own error state. A shell fixes F11 once and makes the
next editor (and the 2.2 pages that edit things) start from the shared frame.
