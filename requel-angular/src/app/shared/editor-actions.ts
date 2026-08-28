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
import { RouterLink } from '@angular/router';

/**
 * Project-aware quick nav for artifact editor headers (#154, from #128): links
 * back to the project workspace overview and to the project's open issues, so a
 * deep-linked editor is navigable without expanding the sidebar. The editor's
 * own "Back" (to its list) and the top-bar breadcrumb cover the rest.
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-editor-actions',
  standalone: true,
  imports: [RouterLink],
  template: `
    <nav class="editor-actions" aria-label="Project navigation">
      <a [routerLink]="['/projects', projectName]" data-testid="editor-action-overview">
        <i class="pi pi-home" aria-hidden="true"></i> Overview
      </a>
      <a [routerLink]="['/projects', projectName, 'open-issues']" data-testid="editor-action-open-issues">
        <i class="pi pi-exclamation-circle" aria-hidden="true"></i> Open issues
      </a>
    </nav>
  `,
  styles: [`
    .editor-actions { display: inline-flex; align-items: center; gap: var(--rq-space-3, 0.75rem); }
    .editor-actions a {
      display: inline-flex; align-items: center; gap: 0.25rem;
      text-decoration: none; color: var(--p-primary-color);
      font-size: var(--rq-font-size-sm, 0.875rem);
    }
    .editor-actions a:hover { text-decoration: underline; }
    .editor-actions a:focus-visible {
      outline: 2px solid var(--p-primary-color); outline-offset: 2px;
      border-radius: var(--rq-radius-sm);
    }
  `]
})
export class EditorActionsComponent {
  @Input({ required: true }) projectName = '';
}
