# Issue #141 — Color contrast, color-only meaning, reduced motion, and target size

Source: `doc/UI_UX_REVIEW.md` Finding 4.7.
Issue: https://github.com/rreganjr/Requel/issues/141 (Priority Medium; Effort Medium).

WCAG addressed: 1.4.1 Use of Color, 1.4.3 Contrast (Minimum), 1.4.11 Non-text Contrast,
2.3.3 Animation from Interactions, 2.5.8 Target Size (Minimum, WCAG 2.2 AA).

## Summary

Finding 4.7 bundles four loosely-related a11y concerns. After reviewing the cited code against the
current tree, most of the surface is already in better shape than the finding implies (the header is
already tokenized; status/argument badges already carry text labels). This plan scopes #141 to a
small, bounded set of policy pieces plus fixes to the five named files, and defers an app-wide audit
to a follow-up. Each decision below was reviewed and agreed before writing.

## Scope decision

In scope: the **policy pieces** (reduced-motion CSS, target-size tokens, contrast-pair verification
of the tokens these components use) plus fixes to the **five named files** — `annotations-section.ts`,
`open-issues.ts`, `scenario-editor.ts`, `tag-selector.ts`, `layout.ts`.

Out of scope (follow-up): an app-wide contrast / target-size / axe audit of every remaining view.
The global policy pieces (reduced-motion, target-size tokens) benefit the whole app anyway, so most
of the value lands without the open-ended sweep.

## Decisions

### 1. Target size — split token

Two new tokens in `src/styles.scss`:

- `--rq-target-min: 24px` — hard WCAG 2.2 SC 2.5.8 floor. Applied to **packed/inline** controls where
  36px would break layout: the tag chip remove `×` (`tag-selector.ts` `.chip-x`), small inline text
  buttons inside badge/annotation rows.
- `--rq-target-comfortable: 36px` — applied to **standalone** actions that sit alone in a row/toolbar:
  the scenario add-step rows (`scenario-editor.ts` `.add-step-row`), primary form action buttons.

Implement hit-area growth as **transparent padding** so the dense 14px visual design is preserved —
the glyph stays small, the target grows. (40px was rejected as too heavy for a 14px-base app.)

Rule of thumb: standalone/isolated action → 36px; control packed among siblings → 24px floor.

### 2. Color-only meaning (SC 1.4.1)

Most cited cases are already compliant — no icon vocabulary needed:

- Status badges (note/issue/resolved) already render uppercase **text labels**. No change.
- Argument stance already renders a text label via `formatSupportLevel` for all **5** IBIS levels
  (Strongly For / For / Neutral / Against / Strongly Against). Already compliant — **keep all 5**.
  (Collapsing 5→3 is a separate domain/data-model change — backend enum + persisted data — and is
  explicitly out of scope here.)
- `.tag-dot` (8×8 swatch) is decorative and sits next to the tag's text name. Fine.

Only real fix: **open-issues optional cell**. `open-issues.ts:100–102` shows `Yes` (red) for required
and a bare `—` (gray em-dash) for optional. Replace the `—` with a word (`No` or `Optional`) so the
optional state has a meaningful text label, not just gray-vs-red.

### 3. Reduced motion (SC 2.3.3)

Add a single global block to `src/styles.scss` (the standard css-remedy pattern):

```scss
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
    scroll-behavior: auto !important;
  }
}
```

Future-proof and covers PrimeNG + CDK automatically. Implementation-time check: confirm scenario-editor
drag/drop still works — it will, because only the animated settle is shortened, not the drag transform.

### 4. Header contrast / governance — already done (no-op)

Finding 4.7 cites white-on-`#1a1a7e` "outside token governance." That literal no longer exists.
`layout.ts` already reads `--rq-header-bg` (→ `--p-primary-900` = #1e3a8a) and `--rq-header-fg`
(→ `--p-surface-0` = #ffffff), refactored during the #127 typography/token work. White on #1e3a8a is
~10.4:1 — passes AA and AAA. Drop from scope; note "already tokenized" and include it in the contrast pass.

### 5. Contrast verification

Verify the token color pairs used by the five components with axe (below) plus manual contrast tooling.
Because the badge/state colors pull from PrimeNG palette tokens (`--p-green-100/700`, `--p-orange-100/700`,
`--p-red-100/700`, etc.), verifying those pairs blesses them everywhere they're used — slightly broader
than the five files, which is fine.

## Contrast verification results

Computed WCAG contrast ratios for every color pair used by the five components, using the exact Aura
palette hex values (`@primeuix/themes/dist/aura/base`). Small text → 4.5:1 AA threshold.

| Pair | Ratio | AA |
| --- | --- | --- |
| note-badge (blue-700 on blue-100) | 5.49 | pass |
| issue-badge (orange-700 on orange-100) | 4.52 | pass |
| resolved-badge / arg-for (green-700 on green-100) | 4.57 | pass |
| arg-against / must-resolve-badge (red-700 on red-100) | 5.30 | pass |
| tag-chip (blue-700 on blue-100) | 5.49 | pass |
| header (white on blue-900) | 10.36 | pass |
| resolution-label (green-700 on white) | 5.02 | pass |
| open-issues optional "No" (inherited slate-700 on white) | 4.76+ | pass |
| neutral/position badge (inherited slate-700 on surface-200) | 8.40 | pass |
| **open-issues "Yes" (red-500 on white)** | **3.76** | **FAIL → fixed** |

Fix: open-issues `.must-resolve` changed from `--p-red-500` to `--p-red-700` (→ 6.47:1).

### `--p-text-secondary-color` — defined explicitly

`--p-text-secondary-color` was never a defined Aura token, so the ~16 components referencing it (empty-state
hints, creator names, neutral/position badges) silently inherited `--p-text-color` (slate-700). Rather than
leave a semantic token undefined, it is now defined in the preset (`requel-preset.ts` → `semantic.text.secondaryColor`).

Value chosen: **slate-600 (#475569)**. It reads as muted/secondary against body slate-700, and — unlike the
`mutedColor` slate-500 — it stays AA-safe on every surface it is used over:

| secondary text on | slate-500 | slate-600 (chosen) |
| --- | --- | --- |
| white | 4.76 | 7.58 |
| surface-50 | 4.55 | 7.24 |
| surface-100 | 4.34 (fail) | 6.92 |
| surface-200 | 3.86 (fail) | 6.15 |

Verified the token emits: `toVariables({text:{secondaryColor:'#475569'}})` →
`--p-text-secondary-color:#475569;`. Net effect app-wide: secondary text shifts from inadvertent slate-700
to intentional, still-compliant slate-600.

## Acceptance criteria

- [ ] `--rq-target-min: 24px` and `--rq-target-comfortable: 36px` tokens added; custom controls
      (chip-remove, add-step rows, small inline text buttons) meet them via transparent hit-area padding.
- [ ] Global `prefers-reduced-motion: reduce` block added to `styles.scss`; scenario drag/drop still works.
- [ ] open-issues optional cell renders a text label (`No`/`Optional`) instead of `—`.
- [ ] Argument stance kept at 5 labeled levels (no change); header noted as already tokenized/passing.
- [ ] Contrast pairs for the affected components verified (axe + manual).
- [ ] a11y spec per affected view + an axe run with tags `wcag2a`, `wcag2aa`, `wcag21aa`, `wcag22aa`
      on the five named views, gating on **no serious or critical** violations.

## Testing / gate

Matches the existing `*.a11y.spec.ts` + `axe-core` pattern. One axe run per affected view scoped to the
five named views (annotations-section, open-issues, scenario-editor, tag-selector, layout). Gate:
**no serious or critical** violations (stricter than the review doc's "critical only" example).

## Sequencing

Keep #141 whole (do not split). **All four sub-tasks can proceed now** — the earlier dependency is
resolved: #136 (Finding 4.2 — real links/buttons; 4.3 folded in) has **landed in `release/2.0`**
(commit `58d55b0`, merged via #153). Verified in the current tree: the scenario add-step controls are
now `<button type="button" class="add-step-row">` and the tag chip remove is
`<button type="button" class="chip-x" aria-label="Remove tag …">`, so the target-size padding applies
directly to genuine buttons — no markup churn to wait on.

Related work already merged to `release/2.0` and relevant to the contrast pass: #161 (PrimeNG preset +
design tokens), #126 (reduced component-local CSS), #127 (typography/tokens — header tokenization),
#139 (modal a11y). Tracked in `doc/124-remediation-rollup.md` (Phase 1). No token collision expected.
