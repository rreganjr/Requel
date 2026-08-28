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

/**
 * Selectable primary-color ramps for the appearance panel (#159 / N6).
 *
 * Each ramp is a Tailwind palette adopted verbatim - the same provenance rule as
 * requel-preset.ts (deterministic, accessible, no runtime mixing). `blue` is the
 * app default and matches the preset's built-in primary, so choosing it is a
 * no-op reset. Applied at runtime via updatePrimaryPalette in ThemeService; the
 * 500 stop doubles as the swatch shown in the panel.
 */
export type PrimaryPaletteKey = 'blue' | 'emerald' | 'violet' | 'rose' | 'amber';

export type ColorRamp = Record<
  '50' | '100' | '200' | '300' | '400' | '500' | '600' | '700' | '800' | '900' | '950',
  string
>;

export const PRIMARY_PALETTES: Record<PrimaryPaletteKey, ColorRamp> = {
  blue: {
    50: '#eff6ff', 100: '#dbeafe', 200: '#bfdbfe', 300: '#93c5fd', 400: '#60a5fa',
    500: '#3b82f6', 600: '#2563eb', 700: '#1d4ed8', 800: '#1e40af', 900: '#1e3a8a', 950: '#172554',
  },
  emerald: {
    50: '#ecfdf5', 100: '#d1fae5', 200: '#a7f3d0', 300: '#6ee7b7', 400: '#34d399',
    500: '#10b981', 600: '#059669', 700: '#047857', 800: '#065f46', 900: '#064e3b', 950: '#022c22',
  },
  violet: {
    50: '#f5f3ff', 100: '#ede9fe', 200: '#ddd6fe', 300: '#c4b5fd', 400: '#a78bfa',
    500: '#8b5cf6', 600: '#7c3aed', 700: '#6d28d9', 800: '#5b21b6', 900: '#4c1d95', 950: '#2e1065',
  },
  rose: {
    50: '#fff1f2', 100: '#ffe4e6', 200: '#fecdd3', 300: '#fda4af', 400: '#fb7185',
    500: '#f43f5e', 600: '#e11d48', 700: '#be123c', 800: '#9f1239', 900: '#881337', 950: '#4c0519',
  },
  amber: {
    50: '#fffbeb', 100: '#fef3c7', 200: '#fde68a', 300: '#fcd34d', 400: '#fbbf24',
    500: '#f59e0b', 600: '#d97706', 700: '#b45309', 800: '#92400e', 900: '#78350f', 950: '#451a03',
  },
};

/** The palette that ships as the app default; selecting it resets the primary. */
export const DEFAULT_PRIMARY: PrimaryPaletteKey = 'blue';

/** Panel-facing option list: key, human label, and the swatch color (500 stop). */
export const PRIMARY_OPTIONS: { key: PrimaryPaletteKey; label: string; swatch: string }[] = [
  { key: 'blue', label: 'Blue', swatch: PRIMARY_PALETTES.blue[500] },
  { key: 'emerald', label: 'Emerald', swatch: PRIMARY_PALETTES.emerald[500] },
  { key: 'violet', label: 'Violet', swatch: PRIMARY_PALETTES.violet[500] },
  { key: 'rose', label: 'Rose', swatch: PRIMARY_PALETTES.rose[500] },
  { key: 'amber', label: 'Amber', swatch: PRIMARY_PALETTES.amber[500] },
];

/** Type guard for values read back from localStorage. */
export function isPrimaryPaletteKey(v: unknown): v is PrimaryPaletteKey {
  return typeof v === 'string' && v in PRIMARY_PALETTES;
}
