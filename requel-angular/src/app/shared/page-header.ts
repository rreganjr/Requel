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
 * Shared page title. Renders the single <h1> for a route so every page has a
 * consistent, unique top-level heading (WCAG 2.4.6, 1.3.1 - see issue #135).
 *
 * The host uses `display: contents` so the <h1> participates directly in the
 * parent layout (e.g. an existing `.page-header` flex row), preserving the
 * previous <h2>-based markup and spacing. The h1 is pinned to the former h2
 * size so converting the tag does not change the visual size.
 */
@Component({
  selector: 'app-page-header',
  standalone: true,
  template: `<h1 class="page-title">{{ title }}</h1>`,
  styles: [`
    :host { display: contents; }
    .page-title {
      margin: 0;
      font-size: 1.5rem;
      font-weight: 700;
    }
  `]
})
export class PageHeaderComponent {
  @Input() title = '';
}
