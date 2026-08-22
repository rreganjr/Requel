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
 * Shared inline submit/command error banner (issue #133, UI/UX review Finding 3.2).
 *
 * Sibling to `app-error-state` (#131) but a different job: `app-error-state`
 * REPLACES a panel whose load failed, while `app-submit-error` is a banner that
 * sits alongside a still-usable form or list to report a failed submit, command,
 * or action. It renders only while `message` is non-empty, so toggling it on
 * inserts the `role="alert"` region into the DOM and assistive tech announces it
 * immediately; clearing `message` removes it.
 *
 * Retry contract: the component does not know how to retry — it emits `(retry)`
 * and the host re-invokes its own action. Set `retryable` (default false) to
 * expose the button, used for network failures worth trying again.
 *
 * Colour reads from the `--rq-tag-danger-*` tint tokens; no colour literals.
 * Success/info confirmations are NOT this component — they stay inline as a
 * polite `p-message`.
 */
@Component({
  selector: 'app-submit-error',
  standalone: true,
  imports: [ButtonModule],
  template: `
    @if (message) {
      <div class="submit-error" role="alert" [attr.data-testid]="testid">
        <i class="submit-error__icon pi pi-times-circle" aria-hidden="true"></i>
        <p class="submit-error__message">{{ message }}</p>
        @if (retryable) {
          <p-button [label]="retryLabel" icon="pi pi-refresh" size="small" [text]="true"
                    severity="secondary" [attr.data-testid]="testid + '-retry'"
                    (onClick)="retry.emit()" />
        }
      </div>
    }
  `,
  styles: [`
    :host { display: block; }
    .submit-error {
      display: flex; align-items: flex-start; gap: var(--rq-space-3);
      padding: var(--rq-space-3) var(--rq-space-4);
      border-radius: var(--rq-radius-md);
      background: var(--rq-tag-danger-bg);
      color: var(--rq-tag-danger-fg);
    }
    .submit-error__icon { margin-top: 0.15rem; flex: 0 0 auto; }
    .submit-error__message {
      margin: 0; flex: 1 1 auto; min-width: 0;
      font-weight: var(--rq-font-weight-medium);
    }
  `]
})
export class SubmitErrorComponent {
  /** The blocking error message; when null/empty the banner does not render. */
  @Input() message: string | null = null;
  /** Whether to show the Retry button (used for retryable network failures). */
  @Input() retryable = false;
  /** Label for the Retry button. */
  @Input() retryLabel = 'Retry';
  /** data-testid for the wrapper element (retry button gets `${testid}-retry`). */
  @Input() testid = 'submit-error';

  /** Emitted when the user clicks Retry; the host re-runs its own action. */
  @Output() retry = new EventEmitter<void>();
}
