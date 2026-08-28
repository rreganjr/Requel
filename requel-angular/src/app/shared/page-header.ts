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
 * Shared page-title primitive. Renders the single <h1> for a route so every page
 * has a consistent, unique top-level heading (WCAG 2.4.6, 1.3.1 - see issue #135),
 * plus optional context around it (issue #127):
 *
 * - `eyebrow` — a short context line above the title (e.g. project name, or
 *   "Project · Artifact type"). Rendered muted/uppercase via the caption role.
 * - `[metadata]` slot — projected content below the title for status tags,
 *   counts, permissions, or unsaved-state indicators.
 *
 * Typography comes entirely from the semantic type-scale tokens/role classes in
 * `styles.scss`; the component holds no font literals. The single `<h1>` is
 * preserved regardless of whether eyebrow/metadata are present.
 *
 * Downstream context chrome (breadcrumbs, top bar, project workspace route) is
 * intentionally out of scope here and lives in #128 (context/IA) and #154 (app
 * shell), which compose this primitive.
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-page-header',
  standalone: true,
  template: `
    <div class="page-header-block">
      @if (eyebrow) {
        <p class="rq-eyebrow page-eyebrow" data-testid="page-eyebrow">{{ eyebrow }}</p>
      }
      <h1 class="rq-page-title page-title">{{ title }}</h1>
      <div class="page-meta"><ng-content select="[metadata]" /></div>
    </div>
  `,
  styles: [`
    :host { display: block; }
    .page-header-block { display: flex; flex-direction: column; }
    /* Margins (not a column gap) so a title-only header adds no stray spacing. */
    .page-eyebrow { margin: 0 0 var(--rq-space-1); }
    .page-title { margin: 0; }
    .page-meta { display: flex; flex-wrap: wrap; align-items: center; gap: var(--rq-space-2); }
    .page-meta:not(:empty) { margin-top: var(--rq-space-2); }
  `]
})
export class PageHeaderComponent {
  /** The route's top-level heading text (the single <h1>). */
  @Input() title = '';
  /** Optional context line above the title (project name / artifact type). */
  @Input() eyebrow = '';
}
