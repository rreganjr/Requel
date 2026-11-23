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

/// <reference types="node" />
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import {
  LIGHT_PRIMARY_STOP,
  PRIMARY_OPTIONS,
  PRIMARY_PALETTES,
  PRIMARY_STOP_700_CLASS,
  PrimaryPaletteKey,
} from './primary-palettes';

/** WCAG relative luminance of a #rrggbb colour. */
function luminance(hex: string): number {
  const [r, g, b] = [1, 3, 5].map(i => {
    const c = parseInt(hex.slice(i, i + 2), 16) / 255;
    return c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
  });
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

function contrast(a: string, b: string): number {
  const [hi, lo] = [luminance(a), luminance(b)].sort((x, y) => y - x);
  return (hi + 0.05) / (lo + 0.05);
}

/** #529: every accent's light-mode primary is AA (4.5:1) with white, both ways round. */
describe('accent palettes', () => {
  const keys = Object.keys(PRIMARY_PALETTES) as PrimaryPaletteKey[];

  it.each(keys)('%s: white button text on the primary, and primary links on white, reach 4.5:1', key => {
    const primary = PRIMARY_PALETTES[key][LIGHT_PRIMARY_STOP[key]];
    expect(contrast(primary, '#ffffff')).toBeGreaterThanOrEqual(4.5);
  });

  it('draws emerald and amber from 700, the rest from 600', () => {
    expect(LIGHT_PRIMARY_STOP).toEqual({ blue: 600, emerald: 700, violet: 600, rose: 600, amber: 700 });
  });

  it('moves the light primary tokens one stop down for the 700 accents (styles.scss)', () => {
    const scss = readFileSync(join(process.cwd(), 'src', 'styles.scss'), 'utf-8');
    const rule = scss.match(new RegExp(`html\\.${PRIMARY_STOP_700_CLASS}:not\\(\\.rq-dark\\) \\{([^}]*)\\}`));
    expect(rule).not.toBeNull();
    expect(rule![1]).toContain('--p-primary-color: var(--p-primary-700)');
    expect(rule![1]).toContain('--p-primary-hover-color: var(--p-primary-800)');
    expect(rule![1]).toContain('--p-primary-active-color: var(--p-primary-900)');
  });

  it('shows the colour buttons use as the picker swatch', () => {
    for (const option of PRIMARY_OPTIONS) {
      expect(option.swatch).toBe(PRIMARY_PALETTES[option.key][LIGHT_PRIMARY_STOP[option.key]]);
    }
  });
});
