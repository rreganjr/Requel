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
import { InputText } from 'primeng/inputtext';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { PageHeaderComponent } from './page-header';
import { AppCardComponent } from './app-card';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-list-page',
  standalone: true,
  host: { '[class.lp-fill]': 'fill' },
  imports: [PageHeaderComponent, InputText, IconField, InputIcon, AppCardComponent],
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
          <p-iconfield>
            <p-inputicon styleClass="pi pi-search" />
            <input pInputText [value]="searchText" [placeholder]="searchPlaceholder"
                   [attr.aria-label]="searchAriaLabel || searchPlaceholder"
                   (input)="search.emit($any($event.target).value)" />
          </p-iconfield>
        </div>
      }
      <app-card [fill]="fill"><ng-content /></app-card>
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
    /* Fill mode (#221): page + wrap flex-column so the card can claim height. */
    :host.lp-fill { display: flex; flex-direction: column; min-height: 0; flex: 1 1 auto; }
    :host.lp-fill .list-page-wrap { display: flex; flex-direction: column; min-height: 0; flex: 1 1 auto; }
  `]
})
export class ListPageComponent {
  @Input() title = '';
  /** Optional context line (project name / artifact type) shown above the title. */
  @Input() eyebrow = '';
  @Input() showSearch = true;
  /**
   * Opt-in fill mode (#221): make the page a bounded-height flex column so the
   * card (and a scrollable table inside it) fills the viewport and scrolls its
   * own body. Forwarded to app-card. Default false = normal auto-height page.
   */
  @Input() fill = false;
  @Input() searchText = '';
  @Input() searchPlaceholder = 'Search...';
  /** Accessible name for the search box; falls back to the placeholder. Set a
   * page-specific value (e.g. "Search goals") when a list enables search (#138). */
  @Input() searchAriaLabel = '';
  @Output() search = new EventEmitter<string>();
}
