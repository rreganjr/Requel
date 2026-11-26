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
import { ChangeDetectionStrategy, Component, OnInit, TemplateRef, ViewChild, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { SelectModule } from 'primeng/select';
import { CheckboxModule } from 'primeng/checkbox';
import { ListPageComponent } from '../../shared/list-page';
import { AppDataTableComponent, DataTableColumn, RowAction } from '../../shared/app-data-table';
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import { AppTagComponent } from '../../shared/app-tag';
import { issueSeverityIcon, issueSeverityTone } from '../../shared/severity';
import { PRIMARY_ACTOR_LABEL } from '../../shared/actor-role';
import { severityLabel } from '../../models/annotation';
import { StepSummaryDto } from '../../models/scenario';
import { ScenarioService } from '../../core/scenario.service';

/** The navigation state a step page reads to send Back to this list (#505). */
export const FROM_STEP_LIST_STATE = { fromStepList: true } as const;

/**
 * A project's plain steps (#505): usage, open issues and who does each, filtered by name, type,
 * open issues and shared. The filters live in the query string, so Back from a step returns to
 * the same view.
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-step-list',
  standalone: true,
  imports: [ListPageComponent, AppDataTableComponent, SubmitErrorComponent, AppTagComponent, FormsModule,
    SelectModule, CheckboxModule],
  template: `
    <app-list-page title="Steps" [fill]="true" [showSearch]="false">
      <app-submit-error [message]="errorMessage()" testid="step-list-error" />

      <app-data-table scrollHeight="flex" [value]="visible()" [columns]="columns" [loading]="loading()"
                      [rowActions]="rowActions" searchPlaceholder="Search steps..."
                      [globalFilterFields]="['name', 'displayName', 'actorName']"
                      [searchText]="query()" (searchChange)="setQuery($event)"
                      testid="step-list" rowTestid="step-row" (rowClick)="open($event)"
                      [emptyTitle]="steps().length ? 'No steps match' : 'No steps yet'"
                      [emptyMessage]="steps().length ? 'Clear a filter to see more steps.' : 'Steps are added in a scenario.'"
                      emptyIcon="pi-step-forward">
        <div toolbarActions class="step-filters">
          <p-select [options]="typeOptions()" [ngModel]="type()" (ngModelChange)="setType($event)"
                    placeholder="All types" [showClear]="true" ariaLabel="Filter by type"
                    data-testid="step-type-filter" class="type-filter" />
          <span class="filter-check">
            <p-checkbox inputId="step-filter-issues" [binary]="true" [ngModel]="withIssues()"
                        (ngModelChange)="setWithIssues($event)" data-testid="step-filter-issues" />
            <label for="step-filter-issues">Has open issues</label>
          </span>
          <span class="filter-check">
            <p-checkbox inputId="step-filter-shared" [binary]="true" [ngModel]="shared()"
                        (ngModelChange)="setShared($event)" data-testid="step-filter-shared" />
            <label for="step-filter-shared">Shared</label>
          </span>
        </div>
      </app-data-table>
    </app-list-page>

    <ng-template #whoCell let-step>{{ who(step) }}</ng-template>
    <ng-template #issuesCell let-step>
      @if (step.openIssues > 0) {
        <span class="issues" data-testid="step-open-issues">
          {{ step.openIssues }}
          <app-tag [tone]="tone(step.highestOpenSeverity)" [icon]="icon(step.highestOpenSeverity)"
                   [label]="label(step.highestOpenSeverity)" data-testid="step-severity" />
        </span>
      } @else {
        <span class="none" data-testid="step-open-issues">0</span>
      }
    </ng-template>
  `,
  styles: [`
    /* Fill mode (#221), as the other list pages. */
    :host { display: flex; flex-direction: column; flex: 1; min-height: 0; }
    .step-filters { display: flex; flex-wrap: wrap; align-items: center; gap: var(--rq-space-3); }
    .type-filter { min-width: 10rem; }
    .filter-check { display: inline-flex; align-items: center; gap: var(--rq-space-2); }
    .issues { display: inline-flex; align-items: center; gap: var(--rq-space-2); }
    .none { color: var(--p-text-secondary-color); }
  `],
})
export class StepListComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly scenarioService = inject(ScenarioService);

  readonly steps = signal<StepSummaryDto[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly query = signal('');
  readonly type = signal<string | null>(null);
  readonly withIssues = signal(false);
  readonly shared = signal(false);

  /** The types the project's steps have, for the type filter. */
  readonly typeOptions = computed(() =>
    [...new Set(this.steps().map(s => s.scenarioType).filter((t): t is string => !!t))].sort());

  readonly visible = computed(() => {
    const q = this.query().trim().toLowerCase();
    return this.steps().filter(s =>
      (!q || [s.name, s.displayName, s.actorName].some(v => v?.toLowerCase().includes(q)))
      && (!this.type() || s.scenarioType === this.type())
      && (!this.withIssues() || s.openIssues > 0)
      && (!this.shared() || s.usedIn > 1));
  });

  @ViewChild('whoCell', { static: true }) whoCell!: TemplateRef<{ $implicit: StepSummaryDto }>;
  @ViewChild('issuesCell', { static: true }) issuesCell!: TemplateRef<{ $implicit: StepSummaryDto }>;
  columns: DataTableColumn<StepSummaryDto>[] = [];
  readonly rowActions: RowAction<StepSummaryDto>[] = [
    { label: 'Open', icon: 'pi pi-eye', command: step => this.open(step) },
  ];

  private projectName = '';

  async ngOnInit(): Promise<void> {
    this.projectName = this.route.snapshot.paramMap.get('name') ?? '';
    const params = this.route.snapshot.queryParamMap;
    this.query.set(params.get('q') ?? '');
    this.type.set(params.get('type') || null);
    this.withIssues.set(params.get('issues') === '1');
    this.shared.set(params.get('shared') === '1');
    this.columns = [
      { field: 'name', header: 'Name', sortable: true, linkState: { ...FROM_STEP_LIST_STATE },
        link: step => ['/projects', this.projectName, 'steps', step.id] },
      { field: 'actorName', header: 'Who', cellTemplate: this.whoCell },
      { field: 'scenarioType', header: 'Type', sortable: true },
      { field: 'usedIn', header: 'Used in', sortable: true },
      { field: 'openIssues', header: 'Open issues', sortable: true, cellTemplate: this.issuesCell },
    ];
    try {
      this.steps.set(await this.scenarioService.listSteps(this.projectName));
    } catch {
      this.errorMessage.set('Failed to load steps.');
    } finally {
      this.loading.set(false);
    }
  }

  open(step: StepSummaryDto): void {
    void this.router.navigate(['/projects', this.projectName, 'steps', step.id],
      { state: { ...FROM_STEP_LIST_STATE } });
  }

  /** Who does the step: the primary actor, the system, the named actor, or nobody said. */
  who(step: StepSummaryDto): string {
    switch (step.actorRole) {
      case 'PRIMARY_ACTOR': return PRIMARY_ACTOR_LABEL;
      case 'SYSTEM': return 'System';
      case 'NAMED': return step.actorName ?? '';
      default: return '';
    }
  }

  tone = issueSeverityTone;
  icon = issueSeverityIcon;
  label = severityLabel;

  setQuery(value: string): void { this.query.set(value); this.syncUrl(); }
  setType(value: string | null): void { this.type.set(value || null); this.syncUrl(); }
  setWithIssues(value: boolean): void { this.withIssues.set(value); this.syncUrl(); }
  setShared(value: boolean): void { this.shared.set(value); this.syncUrl(); }

  /** Keeps the filters in the query string, replacing the entry so filtering adds no history. */
  private syncUrl(): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      replaceUrl: true,
      queryParams: {
        q: this.query().trim() || null,
        type: this.type(),
        issues: this.withIssues() ? '1' : null,
        shared: this.shared() ? '1' : null,
      },
    });
  }
}
