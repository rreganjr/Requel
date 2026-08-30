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
import { Component, OnInit, TemplateRef, ViewChild, computed, signal, ChangeDetectionStrategy, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import { SelectModule } from 'primeng/select';
import { SlicePipe } from '@angular/common';
import { GoalDto } from '../../models/goal';
import { TagDto, tagLabel } from '../../models/tag';
import { GoalService } from '../../core/goal.service';
import { TagService } from '../../core/tag.service';
import { PermissionService } from '../../core/permission.service';
import { ListPageComponent } from '../../shared/list-page';
import { AppChipComponent } from '../../shared/app-chip';
import { EmptyStateComponent } from '../../shared/empty-state';
import { AppDataTableComponent, DataTableColumn, RowAction } from '../../shared/app-data-table';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-goal-list',
  standalone: true,
  imports: [ListPageComponent, AppDataTableComponent, ButtonModule, SubmitErrorComponent, SelectModule, FormsModule, SlicePipe, AppChipComponent, EmptyStateComponent],
  template: `
    <app-list-page title="Goals" [eyebrow]="projectContext()" [fill]="true" [showSearch]="false">
      <app-submit-error [message]="errorMessage()" testid="goal-list-error" [retryable]="true" (retry)="loadGoals()" />

      <app-data-table scrollHeight="flex" [value]="displayedGoals()" [columns]="columns" [loading]="loading()"
                      [rowActions]="rowActions" searchPlaceholder="Search goals..."
                      [globalFilterFields]="['name', 'text', 'createdBy']" testid="goal-list"
                      (rowClick)="openGoal($event)">
        <div toolbarActions class="goal-toolbar-actions">
          <p-select [options]="tagFilterOptions()" [ngModel]="selectedTagId()"
                    (ngModelChange)="selectedTagId.set($event)"
                    optionLabel="label" optionValue="value" placeholder="Filter by tag"
                    data-testid="goal-tag-filter" [showClear]="true" class="tag-filter" />
          @if (canEdit()) {
            <p-button label="New Goal" icon="pi pi-plus" (onClick)="onNewGoal()" />
          }
        </div>
        <ng-template #empty>
          @if (selectedTagId() != null) {
            <app-empty-state title="No goals match this tag"
                             message="Try clearing the tag filter to see all goals."
                             icon="pi-filter-slash" testid="goal-list-empty" />
          } @else {
            <app-empty-state title="No goals yet"
                             message="Capture the objectives this project needs to meet."
                             icon="pi-flag" actionLabel="New Goal" [showAction]="canEdit()"
                             testid="goal-list-empty" (action)="onNewGoal()" />
          }
        </ng-template>
      </app-data-table>
    </app-list-page>

    <ng-template #textCell let-g>
      <span class="text-preview">{{ g.text | slice:0:80 }}{{ g.text?.length > 80 ? '...' : '' }}</span>
    </ng-template>
    <ng-template #tagsCell let-g>
      <span class="chips" data-testid="goal-row-tags">
        @for (t of tagsForGoal(g.id); track t.id) {
          <app-chip [label]="label(t)" [tone]="'primary'" [attr.data-tag]="label(t)" />
        }
      </span>
    </ng-template>
  `,
  styles: [`
    /* Fill mode (#221): claim main-content's height so the data-table body
       scrolls between a pinned header and the paginator. */
    :host { display: flex; flex-direction: column; flex: 1; min-height: 0; }
    .goal-toolbar-actions { display: flex; align-items: center; gap: var(--rq-space-2); }
    .text-preview { display: inline-block; max-width: 400px; overflow: hidden;
      text-overflow: ellipsis; white-space: nowrap; vertical-align: bottom; }
    .tag-filter { min-width: 200px; }
    .chips { display: inline-flex; flex-wrap: wrap; gap: 0.3rem; }
  `]
})
export class GoalListComponent implements OnInit {
  goals = signal<GoalDto[]>([]);
  loading = signal(true);
  errorMessage = signal<string | null>(null);
  canEdit = signal(false);
  /** Project-name context shown as the page eyebrow (issue #127). */
  projectContext = signal('');

  /** goalId -> tags assigned to it (built from the project's tags). */
  tagsByGoal = signal<Map<number, TagDto[]>>(new Map());
  /** Options for the tag filter dropdown. */
  tagFilterOptions = signal<{ label: string; value: number | null }[]>([]);
  selectedTagId = signal<number | null>(null);

  /** Goals narrowed to the selected tag filter (or all when none selected). */
  displayedGoals = computed(() => {
    const tagId = this.selectedTagId();
    if (tagId == null) {
      return this.goals();
    }
    const byGoal = this.tagsByGoal();
    return this.goals().filter(g => (byGoal.get(g.id) ?? []).some(t => t.id === tagId));
  });

  /** Custom cell renderers (static — declared at the top level of the view). */
  @ViewChild('textCell', { static: true }) textCell!: TemplateRef<{ $implicit: GoalDto }>;
  @ViewChild('tagsCell', { static: true }) tagsCell!: TemplateRef<{ $implicit: GoalDto }>;

  /** Data-table column config, wired to the cell templates in ngOnInit. */
  columns: DataTableColumn<GoalDto>[] = [];

  /**
   * Row `⋯` menu. Goals expose only Open from the list (edit/delete live in the
   * editor), so we replace the default menu with a single Open action; whole-row
   * click opens too.
   */
  rowActions: RowAction<GoalDto>[] = [
    { label: 'Open', icon: 'pi pi-eye', command: g => this.openGoal(g) }
  ];

  private projectName = '';
  private readonly destroyRef = inject(DestroyRef);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private goalService: GoalService,
    private tagService: TagService,
    private permissionService: PermissionService
  ) {}

  ngOnInit(): void {
    this.columns = [
      { field: 'name', header: 'Name', sortable: true, link: g => ['/projects', this.projectName, 'goals', g.id] },
      { field: 'text', header: 'Text', cellTemplate: this.textCell, class: 'text-col' },
      { field: 'tags', header: 'Tags', cellTemplate: this.tagsCell },
      { field: 'createdBy', header: 'Created By', sortable: true }
    ];
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(async params => {
      const name = params.get('name') ?? '';
      if (name !== this.projectName) {
        this.projectName = name;
        this.projectContext.set(name);
        await this.permissionService.loadForProject(name);
        this.canEdit.set(this.permissionService.canEdit('Goal'));
        await this.loadGoals();
        await this.loadTags();
      }
    });
  }

  async loadGoals(): Promise<void> {
    this.loading.set(true);
    try {
      this.goals.set(await this.goalService.listGoals(this.projectName));
    } catch {
      this.errorMessage.set('Failed to load goals.');
    } finally {
      this.loading.set(false);
    }
  }

  /**
   * Build the goalId -> tags map and the filter options from the project's tags. One request
   * per tag definition (entities-with-tag), independent of the number of goals.
   */
  async loadTags(): Promise<void> {
    try {
      const projectTags = await this.tagService.getTagsForProject(this.projectName);
      this.tagFilterOptions.set([
        ...projectTags.map(t => ({ label: tagLabel(t), value: t.id as number | null })),
      ]);
      const map = new Map<number, TagDto[]>();
      await Promise.all(projectTags.map(async tag => {
        const refs = await this.tagService.getEntitiesWithTag(tag.id);
        for (const ref of refs) {
          if (ref.entityType === 'Goal') {
            const list = map.get(ref.entityId) ?? [];
            list.push(tag);
            map.set(ref.entityId, list);
          }
        }
      }));
      this.tagsByGoal.set(map);
    } catch {
      // Tags are supplemental — leave the map empty on failure.
    }
  }

  tagsForGoal(goalId: number): TagDto[] {
    return this.tagsByGoal().get(goalId) ?? [];
  }

  label(tag: TagDto): string {
    return tagLabel(tag);
  }

  openGoal(g: GoalDto): void {
    this.router.navigate(['/projects', this.projectName, 'goals', g.id]);
  }

  onNewGoal(): void {
    this.router.navigate(['/projects', this.projectName, 'goals', 'new']);
  }
}
