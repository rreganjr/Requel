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
import { ChangeDetectionStrategy, Component, computed, inject, input, linkedSignal } from '@angular/core';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { MIN_SEARCH_LENGTH } from '../../models/project-search';

let nextId = 0;

/**
 * The project search box (#504): Enter or the button opens the results page for the text. Under
 * 2 characters the button is disabled and the hint says why, the same rule the server enforces.
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-project-search-box',
  standalone: true,
  imports: [ButtonModule, InputText],
  template: `
    <form class="search-box" role="search" [attr.aria-label]="'Search ' + projectName()"
          (submit)="$event.preventDefault(); submit()">
      <input pInputText type="search" [id]="inputId" [value]="text()" placeholder="Search this project"
             [attr.aria-label]="'Search ' + projectName()" [attr.aria-describedby]="hintId"
             autocomplete="off" data-testid="project-search-input"
             (input)="text.set($any($event.target).value)" />
      <p-button type="submit" icon="pi pi-search" [ariaLabel]="'Search'" [disabled]="!searchable()"
                severity="secondary" data-testid="project-search-submit" />
      <span class="search-hint" [id]="hintId" [class.rq-visually-hidden]="searchable() || !text()"
            data-testid="project-search-hint">Type at least {{ minLength }} characters.</span>
    </form>
  `,
  styles: [`
    :host { display: block; }
    .search-box { display: flex; flex-wrap: wrap; align-items: center; gap: var(--rq-space-2); }
    .search-box input { flex: 1 1 14rem; min-width: 10rem; }
    .search-hint { flex-basis: 100%; color: var(--p-text-secondary-color); font-size: var(--rq-font-size-sm); }
  `],
})
export class ProjectSearchBoxComponent {
  private readonly router = inject(Router);

  readonly projectName = input.required<string>();
  /** The text the box starts with (the results page passes its query). */
  readonly query = input('');
  /** The type filter to carry over, if any. */
  readonly type = input<string | null>(null);

  readonly minLength = MIN_SEARCH_LENGTH;
  /** What is typed; reset to {@link query} whenever that changes. */
  readonly text = linkedSignal(() => this.query());
  readonly searchable = computed(() => this.text().trim().length >= MIN_SEARCH_LENGTH);
  readonly inputId = `project-search-${++nextId}`;
  readonly hintId = `${this.inputId}-hint`;

  submit(): void {
    if (!this.searchable()) {
      return;
    }
    const queryParams: Record<string, string> = { q: this.text().trim() };
    if (this.type()) {
      queryParams['type'] = this.type()!;
    }
    void this.router.navigate(['/projects', this.projectName(), 'search'], { queryParams });
  }
}
