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
import { NgTemplateOutlet } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ListPageComponent } from '../../shared/list-page';
import { AppDataTableComponent, DataTableColumn } from '../../shared/app-data-table';
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import { EmptyStateComponent } from '../../shared/empty-state';
import { LoadingStateComponent } from '../../shared/loading-state';
import { AppTagComponent } from '../../shared/app-tag';
import { ENTITY_ROUTES } from '../../shared/entity-routes';
import { TraceabilityService } from '../../core/traceability.service';
import { EntityReferenceDto } from '../../models/entity-reference';
import { GoalTraceDto, TRACE_GAP_KINDS, TraceGapDto } from '../../models/traceability';

/**
 * #497: what references each goal, and the requirements nothing traces to or from. Gaps are
 * shown here, not raised as issues.
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-traceability',
  standalone: true,
  imports: [ListPageComponent, AppDataTableComponent, SubmitErrorComponent, EmptyStateComponent,
    LoadingStateComponent, AppTagComponent, RouterLink, NgTemplateOutlet],
  template: `
    <app-list-page title="Traceability" [eyebrow]="projectName" [showSearch]="false">
      <app-submit-error [message]="errorMessage()" testid="traceability-error" [retryable]="true" (retry)="load()" />

      @if (loading()) {
        <app-loading-state testid="traceability-loading" />
      } @else if (!errorMessage()) {
        <section class="trace-section" aria-labelledby="trace-goals-title">
          <h2 id="trace-goals-title" class="section-title">Goals and what references them</h2>
          @if (goals().length) {
            <app-data-table [value]="goals()" [columns]="columns" [showToolbar]="false"
                            [paginator]="false" testid="trace-matrix" rowTestid="trace-goal-row" />
          } @else {
            <app-empty-state icon="pi-flag" title="No goals yet" testid="trace-no-goals"
                             message="Add goals, then link stories and use cases to them." />
          }
        </section>

        <section class="trace-section" aria-labelledby="trace-gaps-title">
          <h2 id="trace-gaps-title" class="section-title">Gaps <span class="count">{{ gapCount() }}</span></h2>
          @if (gapGroups().length) {
            @for (group of gapGroups(); track group.kind) {
              <section class="gap-group" [attr.aria-labelledby]="'gap-' + group.kind"
                       [attr.data-testid]="'trace-gap-' + group.kind">
                <h3 class="gap-title" [id]="'gap-' + group.kind">
                  {{ group.label }} <span class="count">{{ group.gaps.length }}</span>
                </h3>
                <ul class="gap-list">
                  @for (gap of group.gaps; track gap.entity.entityType + gap.entity.id) {
                    <li>
                      @if (linkFor(gap.entity); as link) {
                        <a [routerLink]="link" data-testid="trace-gap-link">{{ gap.entity.name }}</a>
                      } @else {
                        {{ gap.entity.name }}
                      }
                    </li>
                  }
                </ul>
              </section>
            }
          } @else {
            <app-empty-state icon="pi-check-circle" title="No gaps" testid="trace-no-gaps"
                             message="Every requirement traces to and from something." />
          }
        </section>
      }
    </app-list-page>

    <ng-template #goalCell let-goal>
      <span class="goal-cell">
        <a [routerLink]="linkFor({ entityType: 'Goal', id: goal.id, name: goal.name })"
           data-testid="trace-goal-link">{{ goal.name }}</a>
        @if (!goal.realized) {
          <app-tag tone="warning" icon="pi pi-exclamation-triangle" label="Not realized"
                   data-testid="trace-not-realized" />
        }
      </span>
    </ng-template>
    <ng-template #stakeholdersCell let-goal><ng-container *ngTemplateOutlet="refsList; context: { \$implicit: goal.stakeholders }" /></ng-template>
    <ng-template #actorsCell let-goal><ng-container *ngTemplateOutlet="refsList; context: { \$implicit: goal.actors }" /></ng-template>
    <ng-template #storiesCell let-goal><ng-container *ngTemplateOutlet="refsList; context: { \$implicit: goal.stories }" /></ng-template>
    <ng-template #useCasesCell let-goal><ng-container *ngTemplateOutlet="refsList; context: { \$implicit: goal.useCases }" /></ng-template>
    <ng-template #refsList let-refs>
      @if (refs.length) {
        <ul class="refs">
          @for (ref of refs; track ref.entityType + ref.id) {
            <li><a [routerLink]="linkFor(ref)" data-testid="trace-ref-link">{{ ref.name }}</a></li>
          }
        </ul>
      } @else {
        <span class="none" aria-label="None">&mdash;</span>
      }
    </ng-template>
  `,
  styles: [`
    :host { display: block; }
    .trace-section + .trace-section { margin-top: var(--rq-space-6, 2rem); }
    .section-title { font-size: var(--rq-font-size-lg); margin: 0 0 var(--rq-space-3); }
    .gap-title { font-size: var(--rq-font-size-base, 1rem); margin: var(--rq-space-3) 0 var(--rq-space-2); }
    .count { color: var(--p-text-secondary-color); font-weight: normal; }
    .gap-list, .refs { margin: 0; padding-left: var(--rq-space-4); }
    .refs { list-style: none; padding: 0; display: flex; flex-direction: column; gap: var(--rq-space-1); }
    .goal-cell { display: inline-flex; flex-wrap: wrap; align-items: center; gap: var(--rq-space-2); }
    .none { color: var(--p-text-secondary-color); }
  `],
})
export class TraceabilityComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(TraceabilityService);

  projectName = '';
  readonly goals = signal<GoalTraceDto[]>([]);
  readonly gaps = signal<TraceGapDto[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  readonly gapCount = computed(() => this.gaps().length);
  /** The gaps by kind, in the server's kind order, kinds without gaps left out. */
  readonly gapGroups = computed(() => TRACE_GAP_KINDS
    .map(k => ({ ...k, gaps: this.gaps().filter(g => g.kind === k.kind) }))
    .filter(g => g.gaps.length > 0));

  @ViewChild('goalCell', { static: true }) goalCell!: TemplateRef<{ $implicit: GoalTraceDto }>;
  @ViewChild('stakeholdersCell', { static: true }) stakeholdersCell!: TemplateRef<{ $implicit: GoalTraceDto }>;
  @ViewChild('actorsCell', { static: true }) actorsCell!: TemplateRef<{ $implicit: GoalTraceDto }>;
  @ViewChild('storiesCell', { static: true }) storiesCell!: TemplateRef<{ $implicit: GoalTraceDto }>;
  @ViewChild('useCasesCell', { static: true }) useCasesCell!: TemplateRef<{ $implicit: GoalTraceDto }>;
  columns: DataTableColumn<GoalTraceDto>[] = [];

  ngOnInit(): void {
    this.projectName = this.route.snapshot.paramMap.get('name') ?? '';
    this.columns = [
      { field: 'name', header: 'Goal', cellTemplate: this.goalCell },
      { field: 'stakeholders', header: 'Stakeholders', cellTemplate: this.stakeholdersCell },
      { field: 'actors', header: 'Actors', cellTemplate: this.actorsCell },
      { field: 'stories', header: 'Stories', cellTemplate: this.storiesCell },
      { field: 'useCases', header: 'Use cases', cellTemplate: this.useCasesCell },
    ];
    void this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.errorMessage.set(null);
    try {
      const trace = await this.service.getTraceability(this.projectName);
      this.goals.set(trace.goals);
      this.gaps.set(trace.gaps);
    } catch {
      this.errorMessage.set('Failed to load traceability.');
    } finally {
      this.loading.set(false);
    }
  }

  linkFor(ref: EntityReferenceDto): (string | number)[] | null {
    const segment = ENTITY_ROUTES[ref.entityType];
    return segment && ref.id != null ? ['/projects', this.projectName, segment, ref.id] : null;
  }
}
