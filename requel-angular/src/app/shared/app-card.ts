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
import { Component, Input, ChangeDetectionStrategy } from '@angular/core';

/**
 * Shared card / content-surface primitive (issue #156). The single white
 * rounded card that every content block (list tables, editor forms, the
 * annotations panel, the auth card) renders inside, so the surface look
 * (hairline border, 6px radius, very soft shadow, generous padding) lives in
 * one place instead of being duplicated per view.
 *
 * All surface styling reads from the `--rq-card-*` tokens in `styles.scss`; the
 * component holds no color/radius/shadow literals.
 *
 * API (mirrors `list-page`/`page-header` for consistency):
 * - `title` — optional section heading (an `<h2>`, section-title type role). The
 *   optional card header only renders when a title is set; most cards leave it
 *   empty because the page's single `<h1>` is owned by `page-header` above the
 *   card, avoiding a competing heading. Use `title` only for a genuinely titled
 *   sub-surface.
 * - `[actions]` slot — inline actions shown at the right of the card header
 *   (only rendered alongside a title; actions-without-title is intentionally out
 *   of scope for the #156 sweep).
 * - default content — the card body.
 *
 * Nesting: a surface is a single `app-card`. Inner bordered blocks (e.g. the
 * annotations rows, the scenario step list) stay plain bordered elements inside
 * the one card — they must not become nested `app-card`s.
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-card',
  standalone: true,
  template: `
    <section class="app-card">
      @if (title) {
        <header class="app-card-header">
          <h2 class="rq-section-title app-card-title">{{ title }}</h2>
          <div class="app-card-actions"><ng-content select="[actions]" /></div>
        </header>
      }
      <div class="app-card-body"><ng-content /></div>
    </section>
  `,
  styles: [`
    :host { display: block; }
    .app-card {
      background: var(--rq-card-bg);
      border: 1px solid var(--rq-card-border);
      border-radius: var(--rq-card-radius);
      box-shadow: var(--rq-card-shadow);
      padding: var(--rq-card-pad);
    }
    .app-card-header {
      display: flex; justify-content: space-between; align-items: center;
      gap: var(--rq-space-4); margin-bottom: var(--rq-space-4);
    }
    .app-card-title { margin: 0; }
    .app-card-actions { display: flex; align-items: center; gap: var(--rq-space-2); }
  `]
})
export class AppCardComponent {
  /** Optional section heading for the card (rendered as an <h2>). */
  @Input() title = '';
}
