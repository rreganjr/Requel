/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
 *
 * Requel is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Requel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Requel. If not, see <http://www.gnu.org/licenses/>.
 *
 */
import { definePreset } from '@primeuix/themes';
import Aura from '@primeuix/themes/aura';

/**
 * RequelPreset — the Requel brand layer over PrimeNG's Aura preset.
 *
 * This is the single source of truth for the app's PrimeNG design tokens.
 * App-level (non-PrimeNG) tokens live in `src/styles.scss` as `--rq-*` CSS
 * variables. Components must read colors/radius/type from these tokens, never
 * from hard-coded literals. See `src/app/theme/README.md`.
 *
 * Locked look-and-feel (GitHub #125, doc/124-lookandfeel-plan.md §1.1):
 *  - Primary:  Tailwind Blue — 500 = #3b82f6, hover 600 = #2563eb.
 *  - Surface:  Tailwind Slate, adopted verbatim — a validated, accessible
 *              cool blue-gray ramp (doc/124-lookandfeel-plan.md §1.1.1).
 *              surface-0 = white cards; surface-50 = light blue-gray canvas.
 *  - Radius:   6px content radius (inputs, buttons, chips, cards).
 *  - Font:     Figtree at 14px base (bundled via @fontsource-variable/figtree,
 *              applied in styles.scss).
 *
 * Provenance: primary = Tailwind Blue verbatim; surface = Tailwind Slate
 * verbatim. To reshape either ramp later, regenerate deterministically
 * (e.g. PrimeNG `palette('#1e3a8a')` or an OKLCH lightness sweep), paste the
 * resulting literals here, and record the generator + input hex — never a
 * runtime mix.
 *
 * Dark mode: this preset defines the LIGHT color scheme only. The dark token
 * set is deferred to #159 (N6). `app.config.ts` wires a `.rq-dark`
 * darkModeSelector hook so dark mode can be added later without re-theming;
 * surface-950 (#020617) is reserved as the eventual dark base.
 */
export const RequelPreset = definePreset(Aura, {
  primitive: {
    // Tailwind Blue (verbatim) — the Requel primary/accent ramp.
    // blue-900 (#1e3a8a) is the "cool blue base" accent hue.
    blue: {
      50: '#eff6ff',
      100: '#dbeafe',
      200: '#bfdbfe',
      300: '#93c5fd',
      400: '#60a5fa',
      500: '#3b82f6',
      600: '#2563eb',
      700: '#1d4ed8',
      800: '#1e40af',
      900: '#1e3a8a',
      950: '#172554'
    },
    // Tailwind Slate (verbatim) — the locked 12-stop surface ramp (§1.1.1).
    slate: {
      0: '#ffffff',
      50: '#f8fafc',
      100: '#f1f5f9',
      200: '#e2e8f0',
      300: '#cbd5e1',
      400: '#94a3b8',
      500: '#64748b',
      600: '#475569',
      700: '#334155',
      800: '#1e293b',
      900: '#0f172a',
      950: '#020617'
    }
  },
  semantic: {
    primary: {
      50: '{blue.50}',
      100: '{blue.100}',
      200: '{blue.200}',
      300: '{blue.300}',
      400: '{blue.400}',
      500: '{blue.500}',
      600: '{blue.600}',
      700: '{blue.700}',
      800: '{blue.800}',
      900: '{blue.900}',
      950: '{blue.950}'
    },
    // 6px content radius applied to form fields; component radii below.
    formField: {
      borderRadius: '6px'
    },
    focusRing: {
      width: '2px',
      style: 'solid',
      color: '{primary.500}',
      offset: '2px'
    },
    colorScheme: {
      light: {
        primary: {
          color: '{blue.500}',
          contrastColor: '#ffffff',
          hoverColor: '{blue.600}',
          activeColor: '{blue.700}'
        },
        surface: {
          0: '{slate.0}',
          50: '{slate.50}',
          100: '{slate.100}',
          200: '{slate.200}',
          300: '{slate.300}',
          400: '{slate.400}',
          500: '{slate.500}',
          600: '{slate.600}',
          700: '{slate.700}',
          800: '{slate.800}',
          900: '{slate.900}',
          950: '{slate.950}'
        },
        text: {
          color: '{slate.700}',
          hoverColor: '{slate.800}',
          mutedColor: '{slate.500}',
          hoverMutedColor: '{slate.600}',
          // Secondary text (issue #141). Components reference
          // `--p-text-secondary-color` for de-emphasized text (empty-state hints,
          // creator names, neutral/position badges). It was never a defined Aura
          // token, so it silently inherited `--p-text-color`. Define it explicitly
          // at slate-600: muted vs body slate-700 yet AA-safe on every surface it
          // is used over (>=6.1:1 up to surface-200; slate-500 would fail there).
          secondaryColor: '{slate.600}'
        },
        content: {
          background: '{slate.0}',
          borderColor: '{slate.200}',
          hoverBackground: '{slate.50}'
        }
      },
      // Dark scheme (#159 / N6). Activated by `.rq-dark` (darkModeSelector in
      // app.config.ts). Surfaces map the slate ramp so backgrounds are dark and
      // ELEVATION is preserved — card (surface-0 = slate-900) sits a step lighter
      // than the canvas (surface-50 = slate-950), mirroring light's white-card-on-
      // gray-canvas. Primary lightens to blue-400 with dark contrast text so the
      // button stays AA on dark. Text inverts to the light slate stops; secondary
      // holds at slate-300 to stay AA on the dark surfaces it is used over.
      dark: {
        primary: {
          color: '{blue.400}',
          contrastColor: '{slate.950}',
          hoverColor: '{blue.300}',
          activeColor: '{blue.200}'
        },
        surface: {
          0: '{slate.900}',
          50: '{slate.950}',
          100: '{slate.800}',
          200: '{slate.700}',
          300: '{slate.600}',
          400: '{slate.500}',
          500: '{slate.400}',
          600: '{slate.300}',
          700: '{slate.200}',
          800: '{slate.100}',
          900: '{slate.50}',
          950: '{slate.0}'
        },
        text: {
          color: '{slate.100}',
          hoverColor: '{slate.0}',
          mutedColor: '{slate.400}',
          hoverMutedColor: '{slate.300}',
          secondaryColor: '{slate.300}'
        },
        content: {
          background: '{slate.900}',
          borderColor: '{slate.700}',
          hoverBackground: '{slate.800}'
        }
      }
    }
  },
  components: {
    // 6px content radius across the interactive/content surfaces.
    button: { root: { borderRadius: '6px' } },
    card: { root: { borderRadius: '6px' } },
    chip: { root: { borderRadius: '6px' } },
    tag: { root: { borderRadius: '6px' } }
  }
});
