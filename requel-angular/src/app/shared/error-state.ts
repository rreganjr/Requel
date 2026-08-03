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
 * Shared failure state (issue #131, UI/UX review Finding 2.4). Two jobs, one
 * component:
 *
 *  - `severity="error"` (default): the blocking failure for a whole editor/page
 *    load — e.g. the artifact could not be fetched. Rendered as a `role="alert"`
 *    so assistive tech announces it immediately, with a Retry button that lets
 *    the host re-run its load method.
 *  - `severity="warn"`: a *non-blocking* inline warning for a supplemental
 *    section that failed to load (tags, annotations). Previously these failures
 *    were swallowed silently; now the section renders this warning in place so a
 *    lost capability is visible instead of hidden. Rendered as `role="status"`
 *    (polite) so it does not interrupt.
 *
 * Retry contract: the component does not know how to reload — it emits `(retry)`
 * and the host re-invokes its own existing load method (e.g.
 * `(retry)="loadProject(originalName())"`). Set `retryable=false` to hide the
 * button where a retry makes no sense (e.g. a permission denial).
 *
 * `detail` carries optional secondary/support text (a reason or a "contact your
 * admin" hint) shown muted under the message. All colour reads from the
 * `--rq-tag-danger-*` / `--rq-tag-warning-*` tint tokens; the component holds no
 * colour literals.
 */
@Component({
  selector: 'app-error-state',
  standalone: true,
  imports: [ButtonModule],
  template: `
    <div class="error-state" [class.error-state--warn]="severity === 'warn'"
         [attr.role]="ariaRole" [attr.data-testid]="testid">
      <i class="error-state__icon pi {{ iconClass }}" aria-hidden="true"></i>
      <div class="error-state__content">
        <p class="error-state__message">{{ message }}</p>
        @if (detail) {
          <p class="error-state__detail rq-caption" data-testid="error-state-detail">{{ detail }}</p>
        }
      </div>
      @if (retryable) {
        <p-button [label]="retryLabel" icon="pi pi-refresh" size="small" [text]="true"
                  severity="secondary" data-testid="error-state-retry"
                  (onClick)="retry.emit()" />
      }
    </div>
  `,
  styles: [`
    :host { display: block; }
    .error-state {
      display: flex; align-items: flex-start; gap: var(--rq-space-3);
      padding: var(--rq-space-3) var(--rq-space-4);
      border-radius: var(--rq-radius-md);
      background: var(--rq-tag-danger-bg);
      color: var(--rq-tag-danger-fg);
    }
    .error-state--warn {
      background: var(--rq-tag-warning-bg);
      color: var(--rq-tag-warning-fg);
    }
    .error-state__icon { margin-top: 0.15rem; flex: 0 0 auto; }
    .error-state__content { flex: 1 1 auto; min-width: 0; }
    .error-state__message { margin: 0; font-weight: var(--rq-font-weight-medium); }
    /* Detail inherits the state colour rather than the muted token so it stays
       legible on the tinted background. */
    .error-state__detail { margin: var(--rq-space-1) 0 0; color: inherit; opacity: 0.9; }
  `]
})
export class ErrorStateComponent {
  /** The primary failure message. */
  @Input() message = '';
  /** Optional secondary/support text (reason, "contact your admin", etc.). */
  @Input() detail = '';
  /** 'error' = blocking page/editor failure; 'warn' = non-blocking section warning. */
  @Input() severity: 'error' | 'warn' = 'error';
  /** Whether to show the Retry button. */
  @Input() retryable = true;
  /** Label for the Retry button. */
  @Input() retryLabel = 'Retry';
  /** data-testid for the wrapper element. */
  @Input() testid = 'error-state';

  /** Emitted when the user clicks Retry; the host re-runs its own load method. */
  @Output() retry = new EventEmitter<void>();

  /** Errors assert (interrupt); non-blocking warnings announce politely. */
  get ariaRole(): string {
    return this.severity === 'warn' ? 'status' : 'alert';
  }

  get iconClass(): string {
    return this.severity === 'warn' ? 'pi-exclamation-triangle' : 'pi-times-circle';
  }
}
