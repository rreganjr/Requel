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
import { Component, OnDestroy, OnInit, computed, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { SlicePipe } from '@angular/common';
import { GoalDto } from '../../models/goal';
import { TagDto, tagLabel } from '../../models/tag';
import { GoalService } from '../../core/goal.service';
import { TagService } from '../../core/tag.service';
import { PermissionService } from '../../core/permission.service';
import { ListPageComponent } from '../../shared/list-page';

@Component({
  selector: 'app-goal-list',
  standalone: true,
  imports: [ListPageComponent, TableModule, ButtonModule, MessageModule, SelectModule, FormsModule, SlicePipe],
  template: `
    <app-list-page title="Goals" searchPlaceholder="Search goals..."
                   (search)="dt.filterGlobal($event, 'contains')">
      <ng-container actions>
        <p-select [options]="tagFilterOptions()" [ngModel]="selectedTagId()"
                  (ngModelChange)="selectedTagId.set($event)"
                  optionLabel="label" optionValue="value" placeholder="Filter by tag"
                  data-testid="goal-tag-filter" [showClear]="true" class="tag-filter" />
        @if (canEdit()) {
          <p-button label="New Goal" icon="pi pi-plus" (onClick)="onNewGoal()" />
        }
      </ng-container>

      @if (errorMessage()) {
        <p-message severity="error" [text]="errorMessage()!" />
      }

      <p-table #dt [value]="displayedGoals()" [loading]="loading()" [paginator]="true" [rows]="20"
               [rowHover]="true" selectionMode="single" (onRowSelect)="onRowSelect($event)"
               [globalFilterFields]="['name', 'text', 'createdBy']">
        <ng-template #header>
          <tr>
            <th pSortableColumn="name">Name <p-sortIcon field="name" /></th>
            <th>Text</th>
            <th>Tags</th>
            <th pSortableColumn="createdBy">Created By <p-sortIcon field="createdBy" /></th>
          </tr>
        </ng-template>
        <ng-template #body let-g>
          <tr [pSelectableRow]="g">
            <td>{{ g.name }}</td>
            <td class="text-preview">{{ g.text | slice:0:80 }}{{ g.text?.length > 80 ? '...' : '' }}</td>
            <td>
              <span class="chips" data-testid="goal-row-tags">
                @for (t of tagsForGoal(g.id); track t.id) {
                  <span class="tag-chip" [attr.data-tag]="label(t)">{{ label(t) }}</span>
                }
              </span>
            </td>
            <td>{{ g.createdBy }}</td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr><td colspan="4" class="text-center">No goals found.</td></tr>
        </ng-template>
      </p-table>
    </app-list-page>
  `,
  styles: [`
    .text-preview { max-width: 400px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .tag-filter { min-width: 200px; }
    .chips { display: inline-flex; flex-wrap: wrap; gap: 0.3rem; }
    .tag-chip { font-size: 0.7rem; font-weight: 600; padding: 0.1rem 0.45rem; border-radius: 12px;
      background: var(--p-primary-100, #dbeafe); color: var(--p-primary-700, #1d4ed8); white-space: nowrap; }
  `]
})
export class GoalListComponent implements OnInit, OnDestroy {
  goals = signal<GoalDto[]>([]);
  loading = signal(true);
  errorMessage = signal<string | null>(null);
  canEdit = signal(false);

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

  private projectName = '';
  private paramSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private goalService: GoalService,
    private tagService: TagService,
    private permissionService: PermissionService
  ) {}

  ngOnInit(): void {
    this.paramSub = this.route.paramMap.subscribe(async params => {
      const name = params.get('name') ?? '';
      if (name !== this.projectName) {
        this.projectName = name;
        await this.permissionService.loadForProject(name);
        this.canEdit.set(this.permissionService.canEdit('Goal'));
        await this.loadGoals();
        await this.loadTags();
      }
    });
  }

  ngOnDestroy(): void {
    this.paramSub?.unsubscribe();
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

  onRowSelect(event: { data?: GoalDto | GoalDto[] }): void {
    const g = Array.isArray(event.data) ? event.data[0] : event.data;
    if (!g) return;
    this.router.navigate(['/projects', this.projectName, 'goals', g.id]);
  }

  onNewGoal(): void {
    this.router.navigate(['/projects', this.projectName, 'goals', 'new']);
  }
}
