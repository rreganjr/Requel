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
import { Component, Input } from '@angular/core';

/**
 * Shared loading placeholder (issue #131, UI/UX review Finding 2.4). Renders a
 * skeleton — a set of faint shimmering bars that stand in for content while an
 * async load is in flight — so editors show the shape of the form instead of a
 * blank surface or stale data.
 *
 * Scope: this is the *editor* loading affordance. List pages keep PrimeNG's own
 * `p-table [loading]` overlay, which already covers the tabular case.
 *
 * Accessibility: the skeleton bars are decorative (`aria-hidden`); the readable
 * status lives in a visually-hidden `role="status"` region so assistive tech
 * announces "Loading …" without the bars polluting the accessibility tree
 * (satisfies the AC "loading states expose readable labels for assistive tech").
 *
 * All colour/radius reads from the `--rq-skeleton-*` tokens in `styles.scss`; the
 * component holds no literals.
 *
 * API:
 * - `label` — the accessible status text (default "Loading…").
 * - `lines` — number of skeleton bars (default 3). The last bar is rendered
 *   shorter to suggest a trailing/partial line.
 * - `testid` — `data-testid` for the wrapper (default "loading-state").
 */
@Component({
  selector: 'app-loading-state',
  standalone: true,
  template: `
    <div class="loading-state" [attr.data-testid]="testid">
      <span class="rq-visually-hidden" role="status" aria-live="polite">{{ label }}</span>
      <div class="skeleton" aria-hidden="true">
        @for (w of barWidths(); track $index) {
          <div class="skeleton-bar" [style.width]="w"></div>
        }
      </div>
    </div>
  `,
  styles: [`
    :host { display: block; }
    .skeleton { display: flex; flex-direction: column; gap: var(--rq-space-3); }
    .skeleton-bar {
      height: 1rem;
      border-radius: var(--rq-skeleton-radius);
      background: linear-gradient(
        90deg,
        var(--rq-skeleton-base) 25%,
        var(--rq-skeleton-sheen) 37%,
        var(--rq-skeleton-base) 63%
      );
      background-size: 400% 100%;
      animation: rq-skeleton-shimmer 1.4s ease-in-out infinite;
    }
    @keyframes rq-skeleton-shimmer {
      0% { background-position: 100% 50%; }
      100% { background-position: 0 50%; }
    }
    /* Respect users who prefer reduced motion — hold a static placeholder. */
    @media (prefers-reduced-motion: reduce) {
      .skeleton-bar { animation: none; }
    }
  `]
})
export class LoadingStateComponent {
  /** Accessible status text announced to assistive tech while loading. */
  @Input() label = 'Loading…';
  /** Number of skeleton bars to render. */
  @Input() lines = 3;
  /** data-testid for the wrapper element. */
  @Input() testid = 'loading-state';

  /** Bar widths; the final bar is shortened to hint at a trailing line. */
  barWidths(): string[] {
    const count = Math.max(1, this.lines);
    return Array.from({ length: count }, (_, i) => (i === count - 1 ? '60%' : '100%'));
  }
}
