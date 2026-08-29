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
import { Component, ChangeDetectionStrategy, ViewEncapsulation, inject, signal } from '@angular/core';
import { Dialog } from 'primeng/dialog';
import { ThemeService, ThemeMode } from '../core/theme.service';
import { PRIMARY_OPTIONS } from '../theme/primary-palettes';

/**
 * Appearance panel (#159 / N6). A palette button in the top bar opens a modal to
 * choose the theme mode (Light / Dark / System) and the primary accent. Both are
 * owned and persisted by ThemeService; this component is pure presentation.
 * Controls are native radios inside labelled fieldsets, so grouping, accessible
 * names, and arrow-key navigation are native and axe-clean. The dialog reuses the
 * app's modal-a11y contract (#139: role, aria-modal, focus trap, Escape).
 *
 * ViewEncapsulation.None: the dialog renders to a body portal, so the panel
 * styles must be global to reach it. Class names are prefixed to avoid clashes.
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
  selector: 'app-appearance-menu',
  standalone: true,
  imports: [Dialog],
  template: `
    <button
      type="button"
      class="icon-btn"
      aria-label="Appearance settings"
      title="Appearance"
      data-testid="appearance-toggle"
      (click)="open.set(true)"
    >
      <i class="pi pi-palette" aria-hidden="true"></i>
    </button>

    <p-dialog
      header="Appearance"
      [visible]="open()"
      (visibleChange)="open.set($event)"
      [modal]="true"
      [draggable]="false"
      [resizable]="false"
      [dismissableMask]="true"
      [focusOnShow]="true"
      closeAriaLabel="Close"
      appendTo="body"
      [style]="{ width: '20rem' }"
      [breakpoints]="{ '480px': '90vw' }"
    >
      <div class="rq-appearance-panel">
        <fieldset class="rq-appearance-group">
          <legend>Theme</legend>
          <div class="rq-appearance-modes">
            @for (opt of modes; track opt.value) {
              <label class="rq-appearance-mode">
                <input
                  type="radio"
                  name="rq-theme-mode"
                  [value]="opt.value"
                  [checked]="theme.mode() === opt.value"
                  (change)="theme.setMode(opt.value)"
                  [attr.data-testid]="'theme-mode-' + opt.value"
                />
                <span>{{ opt.label }}</span>
              </label>
            }
          </div>
        </fieldset>

        <fieldset class="rq-appearance-group">
          <legend>Primary color</legend>
          <div class="rq-appearance-colors">
            @for (opt of colors; track opt.key) {
              <label class="rq-appearance-swatch">
                <input
                  type="radio"
                  name="rq-primary"
                  class="rq-visually-hidden"
                  [value]="opt.key"
                  [checked]="theme.primary() === opt.key"
                  (change)="theme.setPrimary(opt.key)"
                  [attr.data-testid]="'primary-' + opt.key"
                />
                <span class="rq-appearance-dot" [style.background]="opt.swatch" aria-hidden="true"></span>
                <span class="rq-visually-hidden">{{ opt.label }}</span>
              </label>
            }
          </div>
        </fieldset>
      </div>
    </p-dialog>
  `,
  styles: [`
    .rq-appearance-panel {
      display: flex;
      flex-direction: column;
      gap: var(--rq-space-4);
    }
    .rq-appearance-group {
      border: 0;
      margin: 0;
      padding: 0;
    }
    .rq-appearance-group legend {
      padding: 0 0 var(--rq-space-2);
      font-size: var(--rq-font-size-sm);
      font-weight: var(--rq-font-weight-semibold);
      color: var(--rq-text-heading-color);
    }
    .rq-appearance-modes {
      display: flex;
      flex-direction: column;
      gap: var(--rq-space-1);
    }
    .rq-appearance-mode {
      display: flex;
      align-items: center;
      gap: var(--rq-space-2);
      padding: var(--rq-space-1) var(--rq-space-2);
      border-radius: var(--rq-radius-sm);
      cursor: pointer;
    }
    .rq-appearance-mode:hover {
      background: var(--p-content-hover-background);
    }
    .rq-appearance-colors {
      display: flex;
      gap: var(--rq-space-3);
    }
    .rq-appearance-swatch {
      cursor: pointer;
      line-height: 0;
    }
    .rq-appearance-dot {
      display: inline-block;
      width: 1.5rem;
      height: 1.5rem;
      border-radius: 999px;
      border: 1px solid var(--p-content-border-color);
    }
    .rq-appearance-swatch input:checked + .rq-appearance-dot {
      box-shadow: 0 0 0 2px var(--p-content-background), 0 0 0 4px var(--p-text-color);
    }
    .rq-appearance-swatch input:focus-visible + .rq-appearance-dot {
      outline: var(--rq-focus-ring-width) solid var(--rq-focus-ring-color);
      outline-offset: var(--rq-focus-ring-offset);
    }
  `],
})
export class AppearanceMenuComponent {
  protected readonly theme = inject(ThemeService);
  protected readonly open = signal(false);

  protected readonly modes: { value: ThemeMode; label: string }[] = [
    { value: 'light', label: 'Light' },
    { value: 'dark', label: 'Dark' },
    { value: 'system', label: 'System' },
  ];

  protected readonly colors = PRIMARY_OPTIONS;
}
