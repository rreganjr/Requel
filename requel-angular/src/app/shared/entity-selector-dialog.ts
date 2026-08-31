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
import { Component, computed, EventEmitter, Input, OnChanges, Output, signal, SimpleChanges, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { SelectModule } from 'primeng/select';
import { EntityReferenceDto } from '../models/entity-reference';
import { GoalService } from '../core/goal.service';
import { StoryService } from '../core/story.service';
import { ActorService } from '../core/actor.service';
import { ScenarioService } from '../core/scenario.service';

/**
 * Shared dialog for selecting an entity from a project.
 * Supports multiple entity types — extend the loadEntities() switch
 * as new types are added in later phases.
 *
 * Usage:
 *   <app-entity-selector-dialog
 *     [visible]="showDialog"
 *     [projectName]="projectName"
 *     [entityType]="'Goal'"
 *     [excludeIds]="[1, 2, 3]"
 *     (selected)="onEntitySelected($event)"
 *     (closed)="showDialog = false" />
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-entity-selector-dialog',
  standalone: true,
  imports: [FormsModule, DialogModule, TableModule, ButtonModule, InputText, IconField, InputIcon, SelectModule],
  template: `
    <p-dialog [header]="'Select ' + entityType" [(visible)]="visible" data-testid="entity-selector-dialog"
              [modal]="true" [focusOnShow]="true" closeAriaLabel="Close" appendTo="body"
              [style]="{ width: '500px' }" (onHide)="closed.emit()">
      <div class="search-bar">
        <p-iconfield>
          <p-inputicon styleClass="pi pi-search" />
          <input pInputText [(ngModel)]="searchText" placeholder="Search..."
                 [attr.aria-label]="'Search ' + entityType.toLowerCase() + 's'"
                 data-testid="entity-selector-search"
                 (input)="dt.filterGlobal(searchText(), 'contains')" />
        </p-iconfield>
        @if (hasTypes()) {
          <p-select [ngModel]="typeFilter()" (ngModelChange)="typeFilter.set($event)"
                    [options]="typeOptions()" optionLabel="label" optionValue="value"
                    placeholder="All Types" styleClass="type-filter-select"
                    ariaLabel="Filter by type" />
        }
      </div>

      <p-table #dt [value]="displayedEntities()" [loading]="loading()" [paginator]="true" [rows]="10"
               [rowHover]="true" selectionMode="single" (onRowSelect)="onSelect($event)"
               [globalFilterFields]="hasTypes() ? ['name', 'typeName'] : ['name']">
        <ng-template #header>
          <tr>
            <th pSortableColumn="name">Name <p-sortIcon field="name" /></th>
            @if (hasTypes()) {
              <th pSortableColumn="typeName">Type <p-sortIcon field="typeName" /></th>
            }
          </tr>
        </ng-template>
        <ng-template #body let-e>
          <tr [pSelectableRow]="e" data-testid="entity-selector-row">
            <td>{{ e.name }}</td>
            @if (hasTypes()) {
              <td>{{ e.typeName }}</td>
            }
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr><td [attr.colspan]="hasTypes() ? 2 : 1" class="text-center">No {{ entityType.toLowerCase() }}s found.</td></tr>
        </ng-template>
      </p-table>
    </p-dialog>
  `,
  styles: [`
    .search-bar { display: flex; gap: 0.5rem; align-items: center; margin-bottom: 0.75rem; }
    .search-bar span { flex: 1; }
    /* .type-filter-select min-width and .text-center live in global styles.scss (#126). */
  `]
})
export class EntitySelectorDialogComponent implements OnChanges {
  @Input() visible = false;
  @Input() projectName = '';
  @Input() entityType = 'Goal';
  @Input() excludeIds: number[] = [];
  /** typeName values to hide entirely from the list (e.g. ['Primary'] when one already exists). */
  @Input() excludeTypes: string[] = [];
  /** When non-empty, show ONLY entities whose typeName is in this list (e.g. ['Primary'] for the primary selector). */
  @Input() includeTypes: string[] = [];
  @Output() selected = new EventEmitter<EntityReferenceDto>();
  @Output() closed = new EventEmitter<void>();

  entities = signal<EntityReferenceDto[]>([]);
  loading = signal(false);
  searchText = signal('');
  typeFilter = signal('');

  hasTypes = computed(() => this.entities().some(e => e.typeName != null));

  /** Unique type options derived from the loaded entities, for the filter dropdown. */
  typeOptions = computed(() => {
    const types = [...new Set(this.entities().map(e => e.typeName).filter((t): t is string => t != null))].sort();
    return [{ label: 'All Types', value: '' }, ...types.map(t => ({ label: t, value: t }))];
  });

  /** entities after applying the type filter dropdown selection. */
  displayedEntities = computed(() => {
    const filter = this.typeFilter();
    return filter ? this.entities().filter(e => e.typeName === filter) : this.entities();
  });

  constructor(
    private goalService: GoalService,
    private storyService: StoryService,
    private actorService: ActorService,
    private scenarioService: ScenarioService
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (this.visible && (changes['visible'] || changes['projectName'] || changes['entityType'])) {
      this.loadEntities();
    }
  }

  private async loadEntities(): Promise<void> {
    this.loading.set(true);
    this.searchText.set('');
    this.typeFilter.set('');
    try {
      let refs: EntityReferenceDto[] = [];
      const excludeSet = new Set(this.excludeIds);
      const excludeTypeSet = new Set(this.excludeTypes);

      switch (this.entityType) {
        case 'Goal': {
          const goals = await this.goalService.listGoals(this.projectName);
          refs = goals.map(g => ({ entityType: 'Goal', id: g.id, name: g.name }));
          break;
        }
        case 'Story': {
          const stories = await this.storyService.listStories(this.projectName);
          refs = stories.map(s => ({ entityType: 'Story', id: s.id, name: s.name }));
          break;
        }
        case 'Actor': {
          const actors = await this.actorService.listActors(this.projectName);
          refs = actors.map(a => ({ entityType: 'Actor', id: a.id, name: a.name }));
          break;
        }
        case 'Scenario': {
          const scenarios = await this.scenarioService.listScenarios(this.projectName);
          refs = scenarios.map(s => ({ entityType: 'Scenario', id: s.id, name: s.name, typeName: s.scenarioType ?? undefined }));
          break;
        }
      }

      const includeTypeSet = new Set(this.includeTypes);
      this.entities.set(refs.filter(r =>
        r.id != null &&
        !excludeSet.has(r.id) &&
        (excludeTypeSet.size === 0 || r.typeName == null || !excludeTypeSet.has(r.typeName)) &&
        (includeTypeSet.size === 0 || (r.typeName != null && includeTypeSet.has(r.typeName)))
      ));
    } finally {
      this.loading.set(false);
    }
  }

  onSelect(event: { data?: EntityReferenceDto | EntityReferenceDto[] }): void {
    const entity = Array.isArray(event.data) ? event.data[0] : event.data;
    if (entity) {
      this.selected.emit(entity);
      this.visible = false;
      this.closed.emit();
    }
  }
}
