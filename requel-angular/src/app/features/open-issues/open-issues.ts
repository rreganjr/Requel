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
import { Component, OnInit, TemplateRef, ViewChild, signal, ChangeDetectionStrategy, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { projectApiUrl } from '../../core/api-url';
import { ButtonModule } from 'primeng/button';
import { BadgeModule } from 'primeng/badge';
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import { ListPageComponent } from '../../shared/list-page';
import { AppDataTableComponent, DataTableColumn } from '../../shared/app-data-table';

interface OpenIssueDto {
  issueId: number;
  issueText: string;
  mustBeResolved: boolean;
  entityType: string;
  entityId: number;
  entityName: string;
}

/** Maps entity type simple names to their Angular route segment. */
const ENTITY_ROUTES: Record<string, string> = {
  Goal: 'goals',
  Story: 'stories',
  Actor: 'actors',
  Scenario: 'scenarios',
  UseCase: 'use-cases',
  UserStakeholder: 'stakeholders',
  NonUserStakeholder: 'stakeholders',
  GlossaryTerm: 'terms',
  ReportGenerator: 'reports',
};

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-open-issues',
  standalone: true,
  imports: [ListPageComponent, AppDataTableComponent, RouterLink, ButtonModule, BadgeModule, SubmitErrorComponent],
  template: `
    <app-list-page title="Open Issues" [fill]="true" [showSearch]="false">
      <app-submit-error [message]="errorMessage()" testid="open-issues-error" [retryable]="true" (retry)="loadIssues()" />

      <app-data-table scrollHeight="flex" [value]="issues()" [columns]="columns" [loading]="loading()"
                      [rowClickable]="false" [defaultActions]="false" [rows]="25"
                      sortField="entityType" [sortOrder]="1" searchPlaceholder="Search issues..."
                      [globalFilterFields]="['entityType', 'entityName', 'issueText']"
                      testid="open-issues" emptyTitle="No open issues"
                      emptyMessage="All clear — everything in this project is resolved." emptyIcon="pi-check-circle">
        <div toolbarActions>
          @if (mustResolveCount() > 0) {
            <p-badge [value]="mustResolveCount().toString()" severity="danger" data-testid="open-issues-badge" />
          }
        </div>
      </app-data-table>
    </app-list-page>

    <ng-template #entityCell let-issue>
      @if (routeFor(issue); as route) {
        <a class="entity-link" data-testid="open-issue-entity-link" [routerLink]="route">{{ issue.entityName }}</a>
      } @else {
        <span data-testid="open-issue-entity-name">{{ issue.entityName }}</span>
      }
    </ng-template>
    <ng-template #requiredCell let-issue>
      @if (issue.mustBeResolved) {
        <span class="must-resolve" data-testid="open-issue-required">Yes</span>
      } @else {
        <span class="optional" data-testid="open-issue-optional">No</span>
      }
    </ng-template>
  `,
  styles: [`
    /* Fill mode (#221): claim main-content's height so the data-table body
       scrolls between a pinned header and the paginator. */
    :host { display: flex; flex-direction: column; flex: 1; min-height: 0; }
    .entity-link { color: var(--p-primary-color); cursor: pointer; text-decoration: underline; }
    .entity-link:hover { opacity: 0.8; }
    /* red-700 (not red-500) so the small bold text clears WCAG AA 4.5:1 on the
       white table cell (issue #141: red-500 on white is only 3.76:1). */
    .must-resolve { color: var(--p-red-700); font-weight: 600; }
    .optional { color: var(--p-text-secondary-color); }
  `]
})
export class OpenIssuesComponent implements OnInit {
  issues = signal<OpenIssueDto[]>([]);
  loading = signal(true);
  errorMessage = signal<string | null>(null);
  mustResolveCount = signal(0);

  @ViewChild('entityCell', { static: true }) entityCell!: TemplateRef<{ $implicit: OpenIssueDto }>;
  @ViewChild('requiredCell', { static: true }) requiredCell!: TemplateRef<{ $implicit: OpenIssueDto }>;
  columns: DataTableColumn<OpenIssueDto>[] = [];

  private projectName = '';
  private readonly destroyRef = inject(DestroyRef);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.columns = [
      { field: 'entityType', header: 'Type', sortable: true },
      { field: 'entityName', header: 'Entity', sortable: true, cellTemplate: this.entityCell },
      { field: 'issueText', header: 'Issue', sortable: true },
      { field: 'mustBeResolved', header: 'Required', cellTemplate: this.requiredCell }
    ];
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      const name = params.get('name') ?? '';
      if (name !== this.projectName) {
        this.projectName = name;
        this.loadIssues();
      }
    });
  }

  async loadIssues(): Promise<void> {
    this.loading.set(true);
    try {
      const data = await firstValueFrom(
        this.http.get<OpenIssueDto[]>(projectApiUrl(this.projectName, 'open-issues'))
      );
      this.issues.set(data);
      this.mustResolveCount.set(data.filter(i => i.mustBeResolved).length);
    } catch {
      this.errorMessage.set('Failed to load open issues.');
    } finally {
      this.loading.set(false);
    }
  }

  /** Router link array for the issue's entity, or null when the type has no route. */
  routeFor(issue: OpenIssueDto): (string | number)[] | null {
    const segment = ENTITY_ROUTES[issue.entityType];
    return segment ? ['/projects', this.projectName, segment, issue.entityId] : null;
  }
}
