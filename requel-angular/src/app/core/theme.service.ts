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
import { Injectable, signal, computed, effect, inject, DestroyRef } from '@angular/core';
import { updatePrimaryPalette } from '@primeuix/themes';
import {
  PRIMARY_PALETTES,
  DEFAULT_PRIMARY,
  PrimaryPaletteKey,
  isPrimaryPaletteKey,
} from '../theme/primary-palettes';

export type ThemeMode = 'light' | 'dark' | 'system';

const MODE_KEY = 'requel_theme';
const PRIMARY_KEY = 'requel_primary';
const DARK_CLASS = 'rq-dark';

/**
 * Owns the app's theme (#159 / N6): light / dark / system mode and the primary
 * accent, both persisted to localStorage and restored on start. `system` follows
 * the OS `prefers-color-scheme` and reacts to it live. The dark scheme itself is
 * defined entirely in tokens (requel-preset dark colorScheme + `.rq-dark`
 * overrides in styles.scss); this service only toggles the `.rq-dark` root class
 * and swaps the primary palette. A pre-boot inline script in index.html applies
 * the same class before Angular loads, so a dark reload never flashes light —
 * this service is the source of truth thereafter.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly destroyRef = inject(DestroyRef);

  private readonly darkQuery =
    typeof window !== 'undefined' && typeof window.matchMedia === 'function'
      ? window.matchMedia('(prefers-color-scheme: dark)')
      : null;

  /** Live OS dark-mode preference (only consulted when mode === 'system'). */
  private readonly systemDark = signal(this.darkQuery?.matches ?? false);

  /** User-chosen mode. */
  readonly mode = signal<ThemeMode>(this.readMode());

  /** User-chosen primary accent. */
  readonly primary = signal<PrimaryPaletteKey>(this.readPrimary());

  /** Whether dark is currently effective (explicit dark, or system + OS dark). */
  readonly isDark = computed(
    () => this.mode() === 'dark' || (this.mode() === 'system' && this.systemDark()),
  );

  constructor() {
    // Toggle the root dark class from the effective theme.
    effect(() => {
      if (typeof document !== 'undefined') {
        document.documentElement.classList.toggle(DARK_CLASS, this.isDark());
      }
    });

    // Persist mode.
    effect(() => this.persist(MODE_KEY, this.mode()));

    // Apply + persist the primary accent.
    effect(() => {
      const key = this.primary();
      updatePrimaryPalette(PRIMARY_PALETTES[key]);
      this.persist(PRIMARY_KEY, key);
    });

    // Follow the OS when in system mode.
    if (this.darkQuery) {
      const listener = (e: MediaQueryListEvent) => this.systemDark.set(e.matches);
      this.darkQuery.addEventListener('change', listener);
      this.destroyRef.onDestroy(() => this.darkQuery?.removeEventListener('change', listener));
    }
  }

  setMode(mode: ThemeMode): void {
    this.mode.set(mode);
  }

  setPrimary(key: PrimaryPaletteKey): void {
    this.primary.set(key);
  }

  private readMode(): ThemeMode {
    const v = safeGet(MODE_KEY);
    return v === 'light' || v === 'dark' || v === 'system' ? v : 'system';
  }

  private readPrimary(): PrimaryPaletteKey {
    const v = safeGet(PRIMARY_KEY);
    return isPrimaryPaletteKey(v) ? v : DEFAULT_PRIMARY;
  }

  private persist(key: string, value: string): void {
    try {
      localStorage.setItem(key, value);
    } catch {
      // Private-mode / storage-disabled: keep the in-memory signal as the source
      // of truth for this session; nothing to restore next load.
    }
  }
}

function safeGet(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}
