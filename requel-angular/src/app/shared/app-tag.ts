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
import { RQ_TONE_ICON, RqTagVariant, RqTone } from './severity';

/**
 * Shared severity tag primitive (issue #155, N2). A soft-tinted status tag whose
 * background/text colour comes entirely from the `--rq-tag-{tone}-*` tokens in
 * `styles.scss` — the component holds no colour literals. It is a token-driven
 * element rather than a pass-through to PrimeNG `p-tag` on purpose: `p-tag`'s
 * severity styling does not offer the soft-tint look or the sixth `neutral` tone
 * the Requel design calls for, so the tint system lives here while radius/spacing
 * still read the shared `--rq-*` scale for consistency with the rest of Aura.
 *
 * Accessibility (issue #141): a tag always carries a visible text {@link label},
 * so colour is never the only signal. The `icon` variant adds a leading icon; any
 * variant may also be given an explicit {@link icon} (used for domain meanings
 * like Note = `pi pi-comment`). Icons are decorative (`aria-hidden`) — the label
 * is the accessible text.
 *
 * API:
 * - `tone` — one of `primary | success | info | warning | danger | neutral`.
 * - `variant` — `default` (rounded rect) | `pill` (fully rounded) | `icon`
 *   (leading icon; falls back to the tone's default icon when `icon` is unset).
 * - `icon` — explicit PrimeIcon class (e.g. `pi pi-comment`); shown on any variant.
 * - `label` — the visible tag text (required for the colour-only-signal guarantee).
 */
@Component({
  selector: 'app-tag',
  standalone: true,
  template: `
    <span class="rq-tag" [class]="toneClass" [class.rq-tag--pill]="variant === 'pill'"
          [attr.data-tone]="tone">
      @if (displayIcon) {
        <i class="rq-tag-icon" [class]="displayIcon" aria-hidden="true"></i>
      }
      <span class="rq-tag-label">{{ label }}</span>
    </span>
  `,
  styles: [`
    .rq-tag { display: inline-flex; align-items: center; gap: var(--rq-space-1);
      font-size: var(--rq-text-caption-size); font-weight: var(--rq-font-weight-semibold);
      line-height: 1.3; padding: 0.1rem 0.45rem; border-radius: var(--rq-radius-md);
      white-space: nowrap; }
    .rq-tag--pill { border-radius: 999px; }
    .rq-tag-icon { font-size: 0.85em; line-height: 1; }
    .rq-tag--primary { background: var(--rq-tag-primary-bg); color: var(--rq-tag-primary-fg); }
    .rq-tag--success { background: var(--rq-tag-success-bg); color: var(--rq-tag-success-fg); }
    .rq-tag--info    { background: var(--rq-tag-info-bg);    color: var(--rq-tag-info-fg); }
    .rq-tag--warning { background: var(--rq-tag-warning-bg); color: var(--rq-tag-warning-fg); }
    .rq-tag--danger  { background: var(--rq-tag-danger-bg);  color: var(--rq-tag-danger-fg); }
    .rq-tag--neutral { background: var(--rq-tag-neutral-bg); color: var(--rq-tag-neutral-fg); }
  `]
})
export class AppTagComponent {
  @Input() tone: RqTone = 'primary';
  @Input() variant: RqTagVariant = 'default';
  @Input() icon?: string;
  @Input() label = '';

  get toneClass(): string {
    return 'rq-tag--' + this.tone;
  }

  /** Explicit icon wins; otherwise the `icon` variant falls back to the tone default. */
  get displayIcon(): string | null {
    return this.icon ?? (this.variant === 'icon' ? RQ_TONE_ICON[this.tone] : null);
  }
}
