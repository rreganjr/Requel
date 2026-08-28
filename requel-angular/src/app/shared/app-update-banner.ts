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
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { ButtonModule } from 'primeng/button';

/**
 * Non-modal "a newer version is available" banner (issue #140, AC3). Shown by an editor when a
 * background (cross-session SSE) update lands while the form has unsaved edits, so the form was
 * not reset. `role="status"` (not a dialog) so it never steals focus; a "Reload" action re-fetches
 * and a close button dismisses. The editor also announces via AnnouncerService — this is the
 * visible affordance, the announcer is the reliable spoken path.
 */
@Component({
  selector: 'app-update-banner',
  standalone: true,
  imports: [ButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="rq-update-banner" role="status" [attr.data-testid]="testid">
      <i class="pi pi-info-circle rq-update-banner__icon" aria-hidden="true"></i>
      <span class="rq-update-banner__msg">{{ message }}</span>
      <span class="rq-update-banner__actions">
        <p-button [label]="reloadLabel" icon="pi pi-refresh" size="small" [text]="true"
                  [attr.data-testid]="testid + '-reload'" (onClick)="reload.emit()" />
        <p-button icon="pi pi-times" [text]="true" [rounded]="true" size="small"
                  severity="secondary" ariaLabel="Dismiss"
                  [attr.data-testid]="testid + '-dismiss'" (onClick)="dismiss.emit()" />
      </span>
    </div>
  `,
  styles: [`
    .rq-update-banner {
      display: flex; align-items: center; gap: var(--rq-space-2, 0.5rem);
      padding: var(--rq-space-2, 0.5rem) var(--rq-space-3, 0.75rem);
      margin-bottom: var(--rq-space-3, 0.75rem);
      border: 1px solid var(--p-blue-200, #bfdbfe);
      background: var(--p-blue-50, #eff6ff);
      color: var(--p-blue-800, #1e40af);
      border-radius: var(--rq-radius-sm, 4px);
    }
    .rq-update-banner__icon { flex-shrink: 0; }
    .rq-update-banner__msg { flex: 1; }
    .rq-update-banner__actions { display: inline-flex; align-items: center; gap: var(--rq-space-1, 0.25rem); }
  `]
})
export class UpdateBannerComponent {
  /** The status message, e.g. "This goal was changed elsewhere." */
  @Input() message = '';
  /** Reload button label. */
  @Input() reloadLabel = 'Reload';
  /** data-testid stem for the banner + its reload/dismiss buttons. */
  @Input() testid = 'update-banner';

  /** Reload requested — the editor re-fetches and resets the form. */
  @Output() reload = new EventEmitter<void>();
  /** Dismiss requested — the editor hides the banner. */
  @Output() dismiss = new EventEmitter<void>();
}
