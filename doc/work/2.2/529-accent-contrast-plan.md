# #529 — Accent colours: white text on emerald and amber is under AA contrast

https://github.com/rreganjr/Requel/issues/529

Branch `529-accent-contrast` from `release/2.2`, PR to `release/2.2`. Size 1.

## Summary

#519 made the accent picker recolour buttons and links, and moved light primary to the 600 stop.
With white text, emerald-600 is 3.8:1 and amber-600 3.2:1, under AA's 4.5:1; blue (5.2), violet
(5.7) and rose (4.7) pass. Dark mode (400 stop, dark text) and the primary tags pass for all five.

## Locked decisions (2026-11-22)

1. **A light primary stop per accent.** Emerald and amber use 700 (5.5:1 and 5.0:1), hover 800,
   active 900; blue, violet and rose stay on 600/700/800. Dark text on the 600 stop was the other
   option, but links use the same colour on white and would still fail.
2. **The picker swatch shows the stop buttons use**, not 500.

## Steps

1. `theme/primary-palettes.ts`: `LIGHT_PRIMARY_STOP` per accent, `PRIMARY_STOP_700_CLASS`, swatch
   from the stop.
2. `core/theme.service.ts`: when the accent changes, toggle `rq-primary-stop-700` on `<html>`
   for a 700 accent.
3. `styles.scss`: `html.rq-primary-stop-700:not(.rq-dark)` sets `--p-primary-color`,
   `--p-primary-hover-color` and `--p-primary-active-color` to the 700, 800 and 900 stops. PrimeNG
   declares its component tokens on `:root` as `var(--p-primary-color)`, so they follow.

Changing the preset at runtime (`updatePreset` with the stops) was tried first. It hung the full
unit suite, where ThemeService can run against the real theme module with no preset loaded, and
it regenerates every theme style on each accent change. The class does neither.

## Test plan

- `theme/primary-palettes.spec.ts`: every accent's light primary reaches 4.5:1 with white (button
  text on it, and it as link text on white); emerald and amber resolve to 700; each swatch is the
  stop buttons use.
- `theme/primary-palettes.spec.ts` also checks the `styles.scss` rule moves the three tokens.
- `core/theme.service.spec.ts`: the class is on for emerald and amber, off for the rest.
- Checked in a browser (headless Chrome on the dev build): with the class, `--p-primary-color`
  and `--p-button-primary-background` resolve to the 700 stop in light mode and stay on 400 in
  dark.
- Whole unit suite, typecheck, development build.

## Acceptance criteria → where

| AC | Covered by |
|---|---|
| Primary buttons and links reach 4.5:1 in light mode for every accent | Decision 1, `primary-palettes.spec.ts` |
| A unit test checks each accent's primary/contrast pair | `primary-palettes.spec.ts` |
