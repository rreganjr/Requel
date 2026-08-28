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
import { Component, EventEmitter, Input, Output, ChangeDetectionStrategy } from '@angular/core';
import { RqTone } from './severity';

/**
 * Shared chip primitive (issue #155, N2). A rounded pill for entity labels/tags.
 * Colours come from the `--rq-chip-*` (neutral default) and `--rq-tag-{tone}-*`
 * (tone-coloured) tokens in `styles.scss` — the component holds no colour literals.
 *
 * Leading visual (first one set wins): `imageUrl` > `avatarUrl` > `icon` >
 * `dotColor` (an arbitrary per-tag swatch, e.g. a user-defined tag colour). A
 * chip may be `removable`, which renders a real `<button>` remove control with an
 * accessible name (`removeAriaLabel`) and a >=24px hit area (issue #141,
 * WCAG 2.5.8) while keeping the × glyph visually small.
 *
 * API:
 * - `label` — visible chip text.
 * - `tone` — `neutral` (default pill) | `primary | success | info | warning | danger`.
 * - `icon` — leading PrimeIcon class.
 * - `avatarUrl` / `imageUrl` — leading circular avatar / square media.
 * - `dotColor` — leading colour swatch (overridden by icon/avatar/image if set).
 * - `removable` + `remove` output — trailing × that emits `remove`.
 * - `removeAriaLabel` — accessible name for the remove button.
 * - `removeTestid` — optional data-testid forwarded to the remove button.
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-chip',
  standalone: true,
  template: `
    <span class="rq-chip" [class]="toneClass" [attr.data-tone]="tone">
      @if (imageUrl) {
        <img class="rq-chip-media" [src]="imageUrl" alt="" />
      } @else if (avatarUrl) {
        <img class="rq-chip-avatar" [src]="avatarUrl" alt="" />
      } @else if (icon) {
        <i class="rq-chip-icon" [class]="icon" aria-hidden="true"></i>
      } @else if (dotColor) {
        <span class="rq-chip-dot" [style.background]="dotColor"></span>
      }
      <span class="rq-chip-label">{{ label }}</span>
      @if (removable) {
        <button type="button" class="rq-chip-remove" [attr.aria-label]="removeAriaLabel"
                [attr.data-testid]="removeTestid" (click)="remove.emit()">
          <span aria-hidden="true">&times;</span>
        </button>
      }
    </span>
  `,
  styles: [`
    .rq-chip { display: inline-flex; align-items: center; gap: 0.3rem;
      font-size: var(--rq-text-caption-size); font-weight: var(--rq-font-weight-semibold);
      line-height: 1.3; padding: 0.15rem 0.5rem; border-radius: 999px; white-space: nowrap; }
    .rq-chip--neutral { background: var(--rq-chip-bg); color: var(--rq-chip-fg); }
    .rq-chip--primary { background: var(--rq-tag-primary-bg); color: var(--rq-tag-primary-fg); }
    .rq-chip--success { background: var(--rq-tag-success-bg); color: var(--rq-tag-success-fg); }
    .rq-chip--info    { background: var(--rq-tag-info-bg);    color: var(--rq-tag-info-fg); }
    .rq-chip--warning { background: var(--rq-tag-warning-bg); color: var(--rq-tag-warning-fg); }
    .rq-chip--danger  { background: var(--rq-tag-danger-bg);  color: var(--rq-tag-danger-fg); }
    .rq-chip-icon { font-size: 0.85em; line-height: 1; }
    .rq-chip-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
    .rq-chip-avatar { width: 1.1rem; height: 1.1rem; border-radius: 50%; object-fit: cover; }
    .rq-chip-media { width: 1.1rem; height: 1.1rem; border-radius: 3px; object-fit: cover; }
    /* Packed inline control: 24px hit-area floor (issue #141, WCAG 2.5.8) via a
       centred min-box; the glyph stays small so the chip keeps its dense look. */
    .rq-chip-remove { display: inline-flex; align-items: center; justify-content: center;
      min-width: var(--rq-target-min); min-height: var(--rq-target-min);
      border: none; background: transparent; cursor: pointer; font-size: 0.9rem;
      line-height: 1; color: inherit; padding: 0; margin: -0.15rem -0.35rem -0.15rem 0; }
  `]
})
export class AppChipComponent {
  @Input() label = '';
  @Input() tone: RqTone = 'neutral';
  @Input() icon?: string;
  @Input() avatarUrl?: string;
  @Input() imageUrl?: string;
  @Input() dotColor?: string | null;
  @Input() removable = false;
  @Input() removeAriaLabel = 'Remove';
  @Input() removeTestid?: string;

  @Output() remove = new EventEmitter<void>();

  get toneClass(): string {
    return 'rq-chip--' + this.tone;
  }
}
