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

@Component({
  selector: 'app-list-page',
  standalone: true,
  imports: [InputText],
  template: `
    <div class="list-page-wrap">
      <div class="page-header">
        <h2>{{ title }}</h2>
        <div class="page-actions">
          <ng-content select="[actions]" />
        </div>
      </div>
      @if (showSearch) {
        <div class="search-bar">
          <span class="p-input-icon-left">
            <i class="pi pi-search"></i>
            <input pInputText [value]="searchText" [placeholder]="searchPlaceholder"
                   (input)="search.emit($any($event.target).value)" />
          </span>
        </div>
      }
      <ng-content />
    </div>
  `,
  styles: [`
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
    .page-header h2 { margin: 0; }
    .page-actions { display: flex; gap: 0.5rem; }
    .search-bar { margin-bottom: 1rem; }
  `]
})
export class ListPageComponent {
  @Input() title = '';
  @Input() showSearch = true;
  @Input() searchText = '';
  @Input() searchPlaceholder = 'Search...';
  @Output() search = new EventEmitter<string>();
}
