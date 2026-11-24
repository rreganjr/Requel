# #528 — Admin pages: cards and full-width tables like the other pages

https://github.com/rreganjr/Requel/issues/528

Branch `528-admin-pages` from `release/2.2`, PR to `release/2.2`. Size 1.

## Summary

Found in the #519 UI review (F23): Global Tags, Tag Categories, Installation Dictionary and
Maintenance each built their own frame (a header div, no card) inside a wrapper capped at 800px
(960px for Tag Categories), so they sat on the canvas and their tables stopped short.

Why they were left out: the tag pages (#112) had their own frame when `app-list-page` only framed
a table; #157 kept it to stay in scope, and #221 listed them as out of scope ("different layout;
follow-up if needed"), a follow-up that was never filed. Maintenance (#303) and Installation
Dictionary (#319) copied the tag pages. Project → Dictionary (#319) already puts an intro, an add
box and a table in `app-list-page`'s card, so the add-form layout is no obstacle.

## Locked decision (2026-11-24)

All four pages use `app-list-page` with `[showSearch]="false"`: the shared title and card, the add
form (or task form) and the table inside the card, no width cap, so tables fill the content area.
Inputs keep their own widths. Maintenance takes the same frame for consistency.

## Steps

1. `global-tags`, `tag-categories`, `install-dictionary`, `maintenance`: replace the wrapper div
   and its page-header with `<app-list-page>`; drop the max-width and page-header styles; swap
   `PageHeaderComponent` for `ListPageComponent`. The page-level `data-testid`s stay on the host.

## Test plan

- Each page's spec: the title renders through `app-list-page`, and its form and table (or word
  list, or task) render inside the card.
- Existing unit and axe specs; e2e in CI (it locates forms, buttons and rows by their own test ids,
  which don't change).
- Whole unit suite, typecheck, development build.

## Acceptance criteria → where

| AC | Covered by |
|---|---|
| The four pages use the same card and list layout as the other pages | Step 1; the per-page specs |
| Their tables fill the content width | Width caps removed |
| Existing unit and e2e tests pass | Unit suite; CI e2e |
