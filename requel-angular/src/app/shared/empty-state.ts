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
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ButtonModule } from 'primeng/button';

/**
 * Shared empty state (issue #131, UI/UX review Finding 2.4). Replaces bare "No X
 * found." text with a titled block that gives guidance and, when the user is
 * allowed to create, a call-to-action button.
 *
 * Permission gating: the host decides whether the action is shown by binding
 * `[showAction]` to its existing permission check (e.g.
 * `PermissionService.canEdit('Goal')` or `canCreateProjects`). A read-only user
 * sees the guidance without a dead/forbidden button. The action only renders when
 * `showAction` is true *and* an `actionLabel` is set.
 *
 * Heading: the title is a styled paragraph, not an `<h*>`, so dropping the block
 * into a list/table body does not inject an out-of-order heading (the page's
 * heading outline stays owned by `page-header`). Typography reads from the
 * `--rq-text-*` role tokens; the component holds no font/colour literals.
 *
 * API:
 * - `title` — short headline (e.g. "No goals yet").
 * - `message` — one-line guidance on what to do next.
 * - `icon` — optional PrimeNG icon name (e.g. "pi-flag"); omitted by default.
 * - `actionLabel` / `actionIcon` — the CTA button text/icon.
 * - `showAction` — host-provided permission gate for the CTA.
 * - `(action)` — emitted when the CTA is clicked.
 */
@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [ButtonModule],
  template: `
    <div class="empty-state" [attr.data-testid]="testid">
      @if (icon) {
        <i class="empty-state__icon pi {{ icon }}" aria-hidden="true"></i>
      }
      <p class="empty-state__title rq-section-title">{{ title }}</p>
      @if (message) {
        <p class="empty-state__message rq-caption">{{ message }}</p>
      }
      @if (showAction && actionLabel) {
        <p-button [label]="actionLabel" [icon]="actionIcon" size="small"
                  data-testid="empty-state-action" (onClick)="action.emit()" />
      }
    </div>
  `,
  styles: [`
    :host { display: block; }
    .empty-state {
      display: flex; flex-direction: column; align-items: center; text-align: center;
      gap: var(--rq-space-2);
      padding: var(--rq-space-8) var(--rq-space-4);
    }
    .empty-state__icon { font-size: 1.75rem; color: var(--rq-text-muted-color); }
    .empty-state__title { margin: 0; }
    .empty-state__message { margin: 0; max-width: 32rem; }
    .empty-state p-button { margin-top: var(--rq-space-2); }
  `]
})
export class EmptyStateComponent {
  /** Short headline for the empty state. */
  @Input() title = '';
  /** One-line guidance on what to do next. */
  @Input() message = '';
  /** Optional PrimeNG icon name (e.g. "pi-flag"). */
  @Input() icon = '';
  /** CTA button label; the button only renders when this is set and showAction is true. */
  @Input() actionLabel = '';
  /** CTA button icon. */
  @Input() actionIcon = 'pi pi-plus';
  /** Host-provided permission gate for the CTA. */
  @Input() showAction = false;
  /** data-testid for the wrapper element. */
  @Input() testid = 'empty-state';

  /** Emitted when the CTA button is clicked. */
  @Output() action = new EventEmitter<void>();
}
