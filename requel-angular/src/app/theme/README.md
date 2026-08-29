# Requel theme

The Requel brand layer over PrimeNG's Aura preset. This is the single source of
truth for the app's visual language — colors, radius, typography, and spacing.
No component may hard-code these values; every component reads from the tokens
documented here. (GitHub #125; see `doc/124-lookandfeel-plan.md` §1.1.)

## Two token layers

1. **PrimeNG design tokens** — `requel-preset.ts` (`definePreset` over Aura).
   Governs everything PrimeNG components render (`--p-*` CSS variables). Wired in
   `app.config.ts` via `providePrimeNG({ theme: { preset: RequelPreset, ... } })`.
2. **App-level tokens** — `src/styles.scss` `:root` (`--rq-*` CSS variables).
   Governs app chrome and layout that isn't a PrimeNG component (spacing, radius
   scale, type scale, layout widths, focus ring, header bar).

App tokens may reference PrimeNG tokens (e.g. `--rq-header-bg: var(--p-primary-900)`),
but not the reverse.

## Locked look-and-feel

| Aspect | Value | Source |
|---|---|---|
| Primary | Tailwind Blue — `500 = #3b82f6`, hover `600 = #2563eb` | verbatim |
| Surface | Tailwind Slate, 12 stops (`0 = #ffffff` … `950 = #020617`) | verbatim, `doc/124-lookandfeel-plan.md` §1.1.1 |
| Content radius | `6px` (inputs, buttons, chips, cards) | preset + `--rq-radius-md` |
| Font | Figtree Variable @ 14px base | bundled, self-hosted |
| Canvas | light blue-gray (`surface-50 = #f8fafc`), white cards | preset |
| Text | body `slate-700`, muted `slate-500`, headings `slate-800` | preset |

**Provenance.** The primary ramp is Tailwind Blue and the surface ramp is
Tailwind Slate, both adopted verbatim. To reshape either later, regenerate
deterministically (e.g. PrimeNG `palette('#1e3a8a')` or an OKLCH lightness
sweep), paste the resulting literals into `requel-preset.ts`, and record the
generator + input hex — never leave a runtime "mix toward white" rule.

## Font

Figtree is bundled via `@fontsource-variable/figtree` (a dependency in
`package.json`) and wired into the build through `angular.json` → `styles`. The
Angular bundler emits the `.woff2` into the app output, so there is **no runtime
Google Fonts / CDN request**. The family name is `Figtree Variable`; PrimeNG
components inherit it because `styles.scss` sets `--p-font-family: var(--rq-font-family)`.

## App tokens (`--rq-*`)

Defined in `src/styles.scss`:

- **Font:** `--rq-font-family`, `--rq-font-size-base` (14px).
- **Spacing:** `--rq-space-1|2|3|4|6|8` (0.25rem → 2rem).
- **Radius:** `--rq-radius-sm` (4px), `--rq-radius-md` (6px, content radius),
  `--rq-radius-lg` (8px).
- **Type scale (raw steps):** `--rq-font-size-xs|sm|md|lg|xl`.
- **Font weights:** `--rq-font-weight-normal|medium|semibold|bold`.
- **Semantic type scale (#127):** role tokens that bundle size + weight +
  line-height — `--rq-text-{page-title,section-title,body,label,helper,caption}-{size,weight,line}`
  — plus `--rq-text-heading-color` (slate-800) and `--rq-text-muted-color`
  (slate-500). Apply them with the role classes `.rq-page-title`,
  `.rq-section-title`, `.rq-eyebrow`, `.rq-label`, `.rq-helper`, `.rq-caption`
  (defined in `styles.scss`), or read the tokens directly. `.rq-page-title` is
  reserved for the single `<h1>` rendered by `app-page-header`. Route pages and
  shared primitives must use these instead of per-view font-size/weight literals.
- **Layout widths:** `--rq-page-max` (76rem), `--rq-editor-max` (48rem).
  Helper class `.rq-page` centers content at `--rq-page-max`.
- **Focus ring:** `--rq-focus-ring-width|color|offset` (mirror the PrimeNG focus
  tokens for custom controls).
- **Header chrome:** `--rq-header-bg` (= `--p-primary-900`), `--rq-header-fg`
  (= `--p-surface-0`). Used by the app header/skip-link in
  `features/auth/layout.ts`.

## Dark mode

Both **light and dark** color schemes are defined here (#159 / N6).
`app.config.ts` wires `darkModeSelector: '.rq-dark'`; `ThemeService`
(`core/theme.service.ts`) toggles that class from the user's mode
(light / dark / **system**, following `prefers-color-scheme`) and swaps the
primary accent, persisting both to `localStorage` and restoring on load. A
pre-boot script in `index.html` applies the class before Angular starts, so a
dark reload never flashes light. The dark surface ramp keeps light's elevation
(card a step lighter than canvas); primary lightens to `blue-400` with dark
contrast text to stay AA. The few app tokens that carry literal colors or assume
a light surface (`--rq-header-fg`, the card shadow, the severity chips) get
explicit `.rq-dark` overrides in `styles.scss`; everything else rides the
`--p-*` aliases. The appearance panel lives in `shared/appearance-menu.ts`.
**Shipped in v2.0.**

## Scope note

#125 establishes the preset, tokens, font, and dark-mode hook, and converts the
app header literal as the proof case. Replacing the remaining component-local
color literals (chips, badges, cards in `goal-list`, `annotations-section`,
`tag-selector`, …) is tracked in #126, #155 (N2 tag/chip system), and
#156 (N3 `app-card`).
