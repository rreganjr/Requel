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
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { combineLatest } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { ListPageComponent } from '../../shared/list-page';
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import { EmptyStateComponent } from '../../shared/empty-state';
import { LoadingStateComponent } from '../../shared/loading-state';
import { AppTagComponent } from '../../shared/app-tag';
import { ENTITY_ROUTES } from '../../shared/entity-routes';
import { AnnouncerService } from '../../core/announcer.service';
import { ProjectSearchService } from '../../core/project-search.service';
import {
  MIN_SEARCH_LENGTH, ProjectSearchResult, SEARCH_PAGE_SIZE, SEARCH_TYPES,
} from '../../models/project-search';
import { ProjectSearchBoxComponent } from './project-search-box';

/** A snippet split around its match, for highlighting. */
interface SnippetParts { before: string; match: string; after: string }

/**
 * Project search results (#504), at `/projects/:name/search?q=&type=`. Grouped by type with
 * every match counted, the match highlighted in each snippet, 200 at a time with "Load more".
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-project-search',
  standalone: true,
  imports: [ListPageComponent, ProjectSearchBoxComponent, SubmitErrorComponent, EmptyStateComponent,
    LoadingStateComponent, AppTagComponent, RouterLink, ButtonModule],
  template: `
    <app-list-page title="Search" [eyebrow]="projectName()" [showSearch]="false">
      <app-project-search-box [projectName]="projectName()" [query]="query()" [type]="type()" />

      <app-submit-error [message]="errorMessage()" testid="search-error" [retryable]="true" (retry)="load()" />

      @if (!searchable()) {
        <app-empty-state icon="pi-search" title="Search this project" testid="search-prompt"
                         [message]="'Type at least ' + minLength + ' characters to search names and text.'" />
      } @else if (loading()) {
        <app-loading-state testid="search-loading" />
      } @else if (!errorMessage()) {
        @if (allTotal() > 0) {
          <nav class="type-filter" aria-label="Filter by type" data-testid="search-type-filter">
            <a [routerLink]="[]" [queryParams]="{ q: query() }" [attr.aria-current]="type() ? null : 'page'"
               class="type-chip" [class.active]="!type()" data-testid="search-filter-all">
              All <span class="chip-count">{{ allTotal() }}</span>
            </a>
            @for (f of typeCounts(); track f.type) {
              <a [routerLink]="[]" [queryParams]="{ q: query(), type: f.type }"
                 [attr.aria-current]="type() === f.type ? 'page' : null"
                 class="type-chip" [class.active]="type() === f.type" [attr.data-testid]="'search-filter-' + f.type">
                {{ f.label }} <span class="chip-count">{{ f.count }}</span>
              </a>
            }
          </nav>
        }

        @if (total() === 0) {
          <app-empty-state icon="pi-filter-slash" testid="search-empty"
                           [title]="'No matches for \u201c' + query() + '\u201d'"
                           [message]="type() ? 'Nothing of this type matches. Try All.' : 'Nothing in this project matches. Try other words.'" />
        } @else {
          <p class="search-summary" data-testid="search-summary">
            Showing {{ results().length }} of {{ total() }} {{ total() === 1 ? 'match' : 'matches' }}
          </p>
          @for (group of groups(); track group.type) {
            <section class="result-group" [attr.aria-labelledby]="'search-group-' + group.type"
                     [attr.data-testid]="'search-group-' + group.type">
              <h2 class="group-title" [id]="'search-group-' + group.type">
                {{ group.label }} <span class="group-count">{{ group.count }}</span>
              </h2>
              <ul class="result-list">
                @for (r of group.results; track r.entityType + r.id) {
                  <li class="result" data-testid="search-result">
                    <div class="result-head">
                      @if (routeFor(r); as link) {
                        <a class="result-name" [routerLink]="link" data-testid="search-result-link">{{ r.name }}</a>
                      } @else {
                        <span class="result-name">{{ r.name }}</span>
                      }
                      @if (r.entityType === 'Issue') {
                        <app-tag [tone]="r.resolved ? 'neutral' : 'warning'" [label]="r.resolved ? 'Resolved' : 'Open'"
                                 data-testid="search-issue-state" />
                        @if (r.onEntityName) {
                          <span class="result-on">on {{ r.onEntityName }}</span>
                        }
                      }
                    </div>
                    @if (parts(r); as p) {
                      <p class="result-snippet" data-testid="search-result-snippet">
                        <span class="field-label">{{ r.matchedField === 'name' ? 'Name' : 'Text' }}:</span>
                        {{ p.before }}<mark>{{ p.match }}</mark>{{ p.after }}
                      </p>
                    } @else if (r.snippet) {
                      <p class="result-snippet" data-testid="search-result-snippet">
                        <span class="field-label">{{ r.matchedField === 'name' ? 'Name' : 'Text' }}:</span>
                        {{ r.snippet }}
                      </p>
                    }
                  </li>
                }
              </ul>
            </section>
          }
          @if (results().length < total()) {
            <div class="load-more">
              <p-button label="Load more" icon="pi pi-angle-down" severity="secondary" [outlined]="true"
                        [loading]="loadingMore()" data-testid="search-load-more" (onClick)="loadMore()" />
            </div>
          }
        }
      }
    </app-list-page>
  `,
  styles: [`
    :host { display: block; }
    app-project-search-box { margin-bottom: var(--rq-space-4); max-width: 40rem; }
    .type-filter { display: flex; flex-wrap: wrap; gap: var(--rq-space-2); margin-bottom: var(--rq-space-4); }
    .type-chip {
      display: inline-flex; align-items: center; gap: var(--rq-space-1);
      padding: var(--rq-space-1) var(--rq-space-3); border-radius: 999px;
      border: 1px solid var(--rq-card-border); color: var(--p-text-color); text-decoration: none;
      font-size: var(--rq-font-size-sm);
    }
    .type-chip:hover { border-color: var(--p-primary-color); }
    .type-chip.active { border-color: var(--p-primary-color); color: var(--p-primary-color); font-weight: 600; }
    .chip-count, .group-count { color: var(--p-text-secondary-color); font-weight: normal; }
    .search-summary { color: var(--p-text-secondary-color); margin: 0 0 var(--rq-space-3); }
    .result-group + .result-group { margin-top: var(--rq-space-4); }
    .group-title { font-size: var(--rq-font-size-lg); margin: 0 0 var(--rq-space-2); }
    .result-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: var(--rq-space-3); }
    .result-head { display: flex; flex-wrap: wrap; align-items: center; gap: var(--rq-space-2); }
    .result-name { font-weight: 600; overflow-wrap: anywhere; }
    a.result-name { color: var(--p-primary-color); }
    .result-on { color: var(--p-text-secondary-color); font-size: var(--rq-font-size-sm); }
    .result-snippet { margin: var(--rq-space-1) 0 0; overflow-wrap: anywhere; }
    .field-label { color: var(--p-text-secondary-color); font-size: var(--rq-font-size-sm); }
    mark { background: var(--rq-highlight-bg); color: var(--rq-highlight-fg); padding: 0 1px; border-radius: 2px; }
    .load-more { display: flex; justify-content: center; margin-top: var(--rq-space-4); }
  `],
})
export class ProjectSearchComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly searchService = inject(ProjectSearchService);
  private readonly announcer = inject(AnnouncerService);
  private readonly destroyRef = inject(DestroyRef);

  readonly minLength = MIN_SEARCH_LENGTH;
  readonly projectName = signal('');
  readonly query = signal('');
  readonly type = signal<string | null>(null);
  readonly results = signal<ProjectSearchResult[]>([]);
  readonly total = signal(0);
  /** Every match's count by type, unfiltered, so the filter keeps its counts while one type shows. */
  readonly allCounts = signal<Record<string, number>>({});
  readonly loading = signal(false);
  readonly loadingMore = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly searchable = computed(() => this.query().trim().length >= MIN_SEARCH_LENGTH);
  readonly allTotal = computed(() => Object.values(this.allCounts()).reduce((a, b) => a + b, 0));
  readonly typeCounts = computed(() => SEARCH_TYPES
    .map(t => ({ ...t, count: this.allCounts()[t.type] ?? 0 }))
    .filter(t => t.count > 0));
  /** The loaded results by type, in the server's type order; each keeps the server's order. */
  readonly groups = computed(() => SEARCH_TYPES
    .map(t => ({ ...t, count: this.allCounts()[t.type] ?? 0,
      results: this.results().filter(r => r.entityType === t.type) }))
    .filter(g => g.results.length > 0));

  /** Bumped by every new search, so a slow older answer can't overwrite a newer one. */
  private generation = 0;

  ngOnInit(): void {
    combineLatest([this.route.paramMap, this.route.queryParamMap])
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(([params, query]) => {
        this.projectName.set(params.get('name') ?? '');
        this.query.set(query.get('q') ?? '');
        this.type.set(query.get('type') || null);
        void this.load();
      });
  }

  async load(): Promise<void> {
    const generation = ++this.generation;
    this.errorMessage.set(null);
    this.results.set([]);
    this.total.set(0);
    if (!this.searchable()) {
      this.allCounts.set({});
      this.loading.set(false);
      return;
    }
    this.loading.set(true);
    const name = this.projectName();
    const q = this.query().trim();
    const type = this.type();
    try {
      // A filtered page only counts its own type; a one-result unfiltered search counts the rest.
      const [page, unfiltered] = await Promise.all([
        this.searchService.search(name, q, { type, limit: SEARCH_PAGE_SIZE }),
        type ? this.searchService.search(name, q, { limit: 1 }) : Promise.resolve(null),
      ]);
      if (generation !== this.generation) {
        return;
      }
      this.results.set(page.results);
      this.total.set(page.total);
      this.allCounts.set((unfiltered ?? page).countsByType);
      this.announcer.announce(page.total === 1 ? '1 match' : `${page.total} matches`);
    } catch (e) {
      if (generation === this.generation) {
        this.errorMessage.set(messageOf(e));
      }
    } finally {
      if (generation === this.generation) {
        this.loading.set(false);
      }
    }
  }

  async loadMore(): Promise<void> {
    const generation = this.generation;
    this.loadingMore.set(true);
    try {
      const page = await this.searchService.search(this.projectName(), this.query().trim(),
        { type: this.type(), offset: this.results().length, limit: SEARCH_PAGE_SIZE });
      if (generation !== this.generation) {
        return;
      }
      this.results.update(r => [...r, ...page.results]);
      this.total.set(page.total);
      this.announcer.announce(`Showing ${this.results().length} of ${page.total} matches`);
    } catch (e) {
      if (generation === this.generation) {
        this.errorMessage.set(messageOf(e));
      }
    } finally {
      this.loadingMore.set(false);
    }
  }

  /** The snippet around its highlighted match, or null when the match couldn't be placed. */
  parts(r: ProjectSearchResult): SnippetParts | null {
    if (r.matchStart < 0 || r.matchLength <= 0) {
      return null;
    }
    const end = r.matchStart + r.matchLength;
    return { before: r.snippet.slice(0, r.matchStart), match: r.snippet.slice(r.matchStart, end),
      after: r.snippet.slice(end) };
  }

  /** Where a result opens: its own page, or for an issue the page of the entity it is on. */
  routeFor(r: ProjectSearchResult): (string | number)[] | null {
    const [type, id] = r.entityType === 'Issue' ? [r.onEntityType, r.onEntityId] : [r.entityType, r.id];
    if (type === 'Project') {
      return ['/projects', this.projectName()];
    }
    const segment = type ? ENTITY_ROUTES[type] : undefined;
    return segment && id != null ? ['/projects', this.projectName(), segment, id] : null;
  }
}

/** The server's message for a refused search (400), or a generic one. */
function messageOf(e: unknown): string {
  if (e instanceof HttpErrorResponse && e.status === 400 && typeof e.error?.message === 'string') {
    return e.error.message;
  }
  return 'Search failed.';
}
