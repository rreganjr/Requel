# Ticket #159 (N6) — Theme switcher + dark mode via config panel — implementation plan

Optional look-and-feel ticket, but Ron has scoped it **into v2.0** (AC4). Modes:
**light / dark / system**; the config panel also includes the optional
**primary-color** selector (Ron's call).

## Why this is well-teed-up
The groundwork was laid deliberately in #125/#141:
- `app.config.ts` already sets `providePrimeNG({ theme: { options: { darkModeSelector: '.rq-dark' } } })`.
- `RequelPreset` defines the **light** `colorScheme` only; the dark set was
  explicitly deferred here. `surface-950 (#020617)` reserved as the dark base.
- 30 of the app's `--rq-*` tokens are `var(--p-*)` aliases, so they flip for
  free once the preset's dark tokens exist. Only tokens carrying **literal**
  colors (shadows, any embedded rgba) need explicit `.rq-dark` overrides.

## Token architecture (AC1 — "dark reads entirely from tokens")
1. **Preset dark colorScheme** (`theme/requel-preset.ts`): add a `dark` block
   mirroring `light`'s structure with the slate/blue ramps mapped for dark:
   - `surface`: dark base with **elevation preserved** — card (`surface-0`)
     slightly lighter than canvas (`surface-50`) so the light-mode relationship
     holds (light: card=white over canvas=slate-50; dark: card≈slate-900 over
     canvas=slate-950).
   - `primary`: keep blue, but shift color/hover to lighter stops
     (`blue-400`/`blue-500`) that read AA on dark surfaces; contrastColor stays
     readable.
   - `text`: light ramp (body ≈ slate-100, muted ≈ slate-400, secondary tuned
     to stay AA on the dark surfaces it's used over — mirror the #141 note).
   - `content`: dark background + a visible dark border.
2. **`.rq-dark` app-token overrides** (`src/styles.scss`): only for `--rq-*`
   tokens with literal colors — audit turned up the shadow tokens
   (`--rq-card-shadow` etc., baked `rgba(15,23,42,…)`) which need a
   dark-appropriate shadow; everything else rides the `--p-*` aliases. No
   component-local overrides (AC1).

## Primary-color selector (Ron: include now)
- `theme/primary-palettes.ts`: a small set of accent ramps (Blue = default/
  current, plus Emerald, Violet, Rose, Amber) — Tailwind ramps verbatim, same
  provenance rule as the preset (deterministic, accessible).
- Applied at runtime with `@primeuix/themes` `updatePrimaryPalette(...)`; the
  chosen key persists and is restored (below). Blue is the no-op default.

## ThemeService (`core/theme.service.ts`) — AC2
Signal-based, `providedIn: 'root'`:
- `mode = signal<'light'|'dark'|'system'>(...)`, `primary = signal<PaletteKey>(...)`.
- `effectiveDark = computed(...)`: `mode==='dark'` OR (`mode==='system'` AND the
  `prefers-color-scheme: dark` media query matches).
- an `effect` toggles `.rq-dark` on `document.documentElement` and applies the
  primary palette.
- persists `mode`→`localStorage['requel_theme']` and `primary`→
  `localStorage['requel_primary']`; restores both in the constructor.
- when `mode==='system'`, a `matchMedia('(prefers-color-scheme: dark)')` listener
  re-derives on OS change (cleaned up via `takeUntilDestroyed`/removeListener).

**FOUC guard:** a tiny inline script in `src/index.html` reads the two
localStorage keys and sets `.rq-dark` (and, if needed, the primary vars) on
`<html>` **before** Angular boots, so a dark-preferring reload never flashes
light. The service is the source of truth thereafter.

## Config panel (gear in the top bar)
- New standalone `shared/appearance-menu.ts` (OnPush, tree-shakeable PrimeNG
  imports): an `icon-btn` gear button in the top-bar right cluster (next to
  `header-search`/`account-trigger`) opening a `p-popover` "Appearance" panel:
  - **Theme**: a labelled radio group / segmented control — Light / Dark / System.
  - **Primary color**: a `radiogroup` of color swatches, each an accessible
    labelled option.
- Wired into `features/auth/layout.ts` top bar. Fully keyboard-navigable, proper
  `aria-label`s, focus management, `data-testid`s for e2e.

## Contrast (AC3)
Verify AA (≥4.5 normal / ≥3 large & UI) for both modes on core pairs: body/muted/
secondary text over surface-0/50/100/200, primary button (contrastColor on
primary), borders, focus ring. Slate+Blue ramps are chosen for this; record the
checked pairs. Add a dark-mode axe spec (render the shell under `.rq-dark`).

## Tests
- `core/theme.service.spec.ts`: default = system; toggling to dark adds `.rq-dark`;
  mode + primary persist to and restore from localStorage; `system` follows a
  mocked matchMedia and reacts to its change event; `updatePrimaryPalette` invoked
  on primary change.
- `shared/appearance-menu.spec.ts` + `.a11y.spec.ts`: renders the three modes and
  the swatches; selecting a mode/color calls the service; no axe violations
  (panel open, both themes).

## AC4 — release-scope decision
Documented as **included in v2.0** in `doc/124-lookandfeel-plan.md` (N6) and the
theme README, with the persistence + token contract.

## Verification gate (frontend-only; no modules/**)
```
cd requel-angular
CI=1 npm test -- --watch=false
npx tsc -p tsconfig.app.json --noEmit && npx tsc -p tsconfig.spec.json --noEmit
npx ng build --configuration development
npm run lint            # #147 guard — new PrimeNG imports must stay per-component
```
Plus a manual smoke: toggle Light/Dark/System and a couple of colors; reload to
confirm persistence and no flash; sanity a couple of screens (list, editor,
dialog) in dark.

## Out of scope
- No server-persisted preference (localStorage only — matches the #154
  sidebar-collapse pattern; a server-synced pref could be a follow-up).
- No per-component dark CSS; everything routes through tokens (AC1).

## Risks
- **Dark token quality / contrast** — the real work; mitigated by the slate/blue
  ramps + explicit AA checks + the dark axe spec.
- **FOUC** on reload — handled by the pre-boot inline script.
- **PrimeNG overlays** (dialog/popover/table) in dark — they read `--p-*`, so
  they flip with the preset; verify in the smoke.
- **`updatePrimaryPalette` global effect** — applies to the whole app for that
  browser; that is the intent, persisted per browser.

## AC mapping
| AC | Coverage |
|----|----------|
| AC1 dark reads entirely from tokens | preset dark colorScheme + `.rq-dark` literal-token overrides; no component CSS |
| AC2 preference persists + restored | ThemeService + localStorage + pre-boot FOUC guard |
| AC3 light & dark meet AA | slate/blue ramps + recorded contrast checks + dark axe spec |
| AC4 clear optional/release decision | documented as included in v2.0 |
