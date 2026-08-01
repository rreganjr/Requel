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
import { InputText } from 'primeng/inputtext';
import { PageHeaderComponent } from './page-header';

@Component({
  selector: 'app-list-page',
  standalone: true,
  imports: [PageHeaderComponent, InputText],
  template: `
    <div class="list-page-wrap">
      <div class="page-header">
        <app-page-header [title]="title" [eyebrow]="eyebrow" />
        <div class="page-actions">
          <ng-content select="[actions]" />
        </div>
      </div>
      @if (showSearch) {
        <div class="list-toolbar">
          <span class="p-input-icon-left search-field">
            <i class="pi pi-search"></i>
            <input pInputText [value]="searchText" [placeholder]="searchPlaceholder"
                   aria-label="Search"
                   (input)="search.emit($any($event.target).value)" />
          </span>
        </div>
      }
      <ng-content />
    </div>
  `,
  // Compact, consistent list-page toolbar (issue #127): the title/actions row
  // and the search toolbar share the same token-driven spacing so density and
  // action placement match across migrated list pages (Goals, Stories, …).
  styles: [`
    .page-header {
      display: flex; justify-content: space-between; align-items: center;
      gap: var(--rq-space-4); margin-bottom: var(--rq-space-4);
    }
    .page-actions { display: flex; align-items: center; gap: var(--rq-space-2); }
    .list-toolbar {
      display: flex; align-items: center; gap: var(--rq-space-2);
      margin-bottom: var(--rq-space-4);
    }
    .search-field { display: inline-flex; align-items: center; }
  `]
})
export class ListPageComponent {
  @Input() title = '';
  /** Optional context line (project name / artifact type) shown above the title. */
  @Input() eyebrow = '';
  @Input() showSearch = true;
  @Input() searchText = '';
  @Input() searchPlaceholder = 'Search...';
  @Output() search = new EventEmitter<string>();
}
