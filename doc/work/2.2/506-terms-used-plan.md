# #506 — Glossary terms used: chips with definitions under each editor's text

https://github.com/rreganjr/Requel/issues/506

Branch `506-terms-used` from `release/2.2`, PR to `release/2.2`. Size 2.

## Summary

Each editor gets a "Terms used" row under its text field: a chip for every glossary term found
in the name and text being edited, its definition on hover or focus, a click opening the term.
Matching is live against what is typed, so unsaved edits count.

Review against the tree: still needed, and the ACs stand. All eight editors exist (goal, story,
actor, use case, scenario, step page #476, stakeholder, glossary term). The project's terms come
from `GET /api/projects/{name}/terms`, which already answers the light summary DTO (name,
definition, canonical id and name; no referers). No backend change.

## Locked decisions

From the ticket (planning, 2026-11-17):

1. A row under the text field, not inline highlighting.
2. Live matching against the text as typed, not the stored referer links.

Settled in this review, with no open question:

3. **The same word rule as the server.** A term matches as a whole phrase, ignoring case. A
   boundary is any character that isn't a letter or a digit, as `TermTextMatcher` defines it, so
   "dry-run event's" matches "dry-run event" and "events" doesn't. The client and the
   server's glossary check then agree on what "uses a term" means.
4. **Longest match wins.** Terms are tried longest name first. A match overlapping one already
   taken is skipped, so "video room" suppresses "room" inside it, while a separate "room"
   elsewhere in the text still counts.
5. **Order.** Chips appear in order of each term's first appearance. Name matches come before
   text matches.
6. **Alternates.** An alternate term shows as "alternate → canonical" with the canonical's
   definition. Clicking it opens the canonical term. If the canonical also appears, it gets its
   own chip.
7. **Opening a term** navigates in the same tab. The existing `dirtyCheckGuard` already asks
   before leaving an editor with unsaved changes.
8. **Accessibility.** Each chip is a link, so it can be focused. The definition shows as a
   tooltip on hover and on focus (`tooltipEvent="both"`), and the link is described by a
   visually hidden copy of the definition, so screen readers read it too.

## Contract

`app-terms-used` (shared, standalone, OnPush):

- **Inputs:**
  - `projectName`;
  - `name` and `text`: what is being edited;
  - `excludeTermId`: the term editor passes its own id.
- **Behaviour:**
  - loads the project's terms once, through `TermService.listTerms`;
  - re-matches 250 ms after the last change;
  - renders nothing while no term matches.
- **Matcher:** `shared/term-matcher.ts`, a pure function
  `findTermsUsed(terms, name, text, excludeId)`, unit-tested on its own.

## Steps

1. `term-matcher.ts` and its spec: the ACs' examples, boundaries (punctuation, apostrophes,
   hyphens, digits), case, longest match, order, the exclusion, regex characters in a term name.
2. `TermsUsedComponent` and its spec, with axe.
3. Wire it under the text field in the goal, story, actor, use case, scenario, step,
   stakeholder and term editors. Each binds its form's `name` and `text` control values in the
   template, so a value loaded with `emitEvent: false` is seen as well as one typed.
4. User guide: a paragraph in the glossary section.

## Test plan

- `term-matcher.spec`: every matching rule above, and each AC example.
- `terms-used.spec`:
  - chips render with the definition in the tooltip and the hidden description;
  - an alternate renders as alternate → canonical, its link opens the canonical, and it has the
    canonical's definition;
  - the debounce works, using fake timers;
  - nothing renders with no match;
  - the excluded term is left out;
  - axe passes.
- One editor spec per editor, checking the row is there and follows the typed text. The goal
  editor covers the AC example end to end.
- e2e `terms-used-506`:
  - in the goal editor, typing "The Host ends the video room" shows Host and video room chips;
  - focusing a chip shows its definition;
  - clicking it opens the term.

## Out of scope

From the ticket: inline highlighting, plural or stemmed matching, and changing referer links.

## Risks

- **Many terms:** one regex per term, run over a few hundred characters each time the debounce
  fires, is negligible for any realistic glossary.
- **Editor layout:** eight editors change. Each gets the same one-line insertion under its text
  field, and its spec checks it.

## Acceptance criteria → where

| AC | Covered by |
|---|---|
| "The Host ends the video room" with Host, video room, room → Host and video room | matcher spec, goal editor spec, e2e |
| Case-insensitive, whole-word: "rooms" and "bedroom" don't match "room" | matcher spec |
| Typing a term into unsaved text adds its chip | terms-used spec, e2e |
| An alternate shows alternate → canonical with the canonical's definition | matcher and terms-used specs |
| A term editor doesn't list itself | matcher spec, term editor spec |
| No chips row when nothing matches | terms-used spec |
| Chips focusable, definition on focus | terms-used spec (axe), e2e |
