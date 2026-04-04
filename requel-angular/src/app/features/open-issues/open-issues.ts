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
import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { projectApiUrl } from '../../core/api-url';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { BadgeModule } from 'primeng/badge';
import { MessageModule } from 'primeng/message';
import { InputText } from 'primeng/inputtext';

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
  selector: 'app-open-issues',
  standalone: true,
  imports: [FormsModule, TableModule, ButtonModule, BadgeModule, MessageModule, InputText],
  template: `
    <div class="open-issues">
      <div class="page-header">
        <h2>Open Issues</h2>
        <p-badge [value]="mustResolveCount().toString()" severity="danger"
                 *ngIf="mustResolveCount() > 0" />
      </div>

      @if (errorMessage()) {
        <p-message severity="error" [text]="errorMessage()!" />
      }

      @if (!loading() && issues().length === 0) {
        <p-message severity="success" text="No open issues — all clear." />
      }

      <div class="search-bar">
        <span class="p-input-icon-left">
          <i class="pi pi-search"></i>
          <input pInputText [(ngModel)]="searchText" placeholder="Search issues..."
                 (input)="dt.filterGlobal(searchText, 'contains')" />
        </span>
      </div>

      <p-table #dt [value]="issues()" [loading]="loading()" [paginator]="true" [rows]="25"
               [rowHover]="true" [globalFilterFields]="['entityType', 'entityName', 'issueText']"
               [sortField]="'entityType'" [sortOrder]="1">
        <ng-template #header>
          <tr>
            <th pSortableColumn="entityType">Type <p-sortIcon field="entityType" /></th>
            <th pSortableColumn="entityName">Entity <p-sortIcon field="entityName" /></th>
            <th pSortableColumn="issueText">Issue <p-sortIcon field="issueText" /></th>
            <th>Required</th>
          </tr>
        </ng-template>
        <ng-template #body let-issue>
          <tr>
            <td>{{ issue.entityType }}</td>
            <td>
              <a class="entity-link" (click)="navigateTo(issue)">{{ issue.entityName }}</a>
            </td>
            <td>{{ issue.issueText }}</td>
            <td>
              @if (issue.mustBeResolved) {
                <span class="must-resolve">Yes</span>
              } @else {
                <span class="optional">—</span>
              }
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr><td colspan="4" class="text-center">No open issues.</td></tr>
        </ng-template>
      </p-table>
    </div>
  `,
  styles: [`
    .page-header { display: flex; align-items: center; gap: 1rem; margin-bottom: 1rem; }
    .search-bar { margin-bottom: 1rem; }
    .text-center { text-align: center; }
    .entity-link { color: var(--p-primary-color); cursor: pointer; text-decoration: underline; }
    .entity-link:hover { opacity: 0.8; }
    .must-resolve { color: var(--p-red-500); font-weight: 600; }
    .optional { color: var(--p-text-secondary-color); }
  `]
})
export class OpenIssuesComponent implements OnInit, OnDestroy {
  issues = signal<OpenIssueDto[]>([]);
  loading = signal(true);
  errorMessage = signal<string | null>(null);
  mustResolveCount = signal(0);
  searchText = '';

  private projectName = '';
  private paramSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.paramSub = this.route.paramMap.subscribe(params => {
      const name = params.get('name') ?? '';
      if (name !== this.projectName) {
        this.projectName = name;
        this.loadIssues();
      }
    });
  }

  ngOnDestroy(): void {
    this.paramSub?.unsubscribe();
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

  navigateTo(issue: OpenIssueDto): void {
    const segment = ENTITY_ROUTES[issue.entityType];
    if (segment) {
      this.router.navigate(['/projects', this.projectName, segment, issue.entityId]);
    }
  }
}
