import { Component, EventEmitter, Input, OnChanges, Output, signal, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { EntityReferenceDto } from '../models/entity-reference';
import { GoalService } from '../core/goal.service';
import { StoryService } from '../core/story.service';

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
  selector: 'app-entity-selector-dialog',
  standalone: true,
  imports: [FormsModule, DialogModule, TableModule, ButtonModule, InputText],
  template: `
    <p-dialog [header]="'Select ' + entityType" [(visible)]="visible"
              [modal]="true" appendTo="body" [style]="{ width: '500px' }" (onHide)="closed.emit()">
      <div class="search-bar">
        <span class="p-input-icon-left">
          <i class="pi pi-search"></i>
          <input pInputText [(ngModel)]="searchText" placeholder="Search..."
                 (input)="dt.filterGlobal(searchText(), 'contains')" />
        </span>
      </div>

      <p-table #dt [value]="entities()" [loading]="loading()" [paginator]="true" [rows]="10"
               [rowHover]="true" selectionMode="single" (onRowSelect)="onSelect($event)"
               [globalFilterFields]="['name']">
        <ng-template #header>
          <tr>
            <th pSortableColumn="name">Name <p-sortIcon field="name" /></th>
          </tr>
        </ng-template>
        <ng-template #body let-e>
          <tr [pSelectableRow]="e">
            <td>{{ e.name }}</td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr><td class="text-center">No {{ entityType.toLowerCase() }}s found.</td></tr>
        </ng-template>
      </p-table>
    </p-dialog>
  `,
  styles: [`
    .search-bar { margin-bottom: 0.75rem; }
    .text-center { text-align: center; }
  `]
})
export class EntitySelectorDialogComponent implements OnChanges {
  @Input() visible = false;
  @Input() projectName = '';
  @Input() entityType = 'Goal';
  @Input() excludeIds: number[] = [];
  @Output() selected = new EventEmitter<EntityReferenceDto>();
  @Output() closed = new EventEmitter<void>();

  entities = signal<EntityReferenceDto[]>([]);
  loading = signal(false);
  searchText = signal('');

  constructor(
    private goalService: GoalService,
    private storyService: StoryService
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (this.visible && (changes['visible'] || changes['projectName'] || changes['entityType'])) {
      this.loadEntities();
    }
  }

  private async loadEntities(): Promise<void> {
    this.loading.set(true);
    this.searchText.set('');
    try {
      let refs: EntityReferenceDto[] = [];
      const excludeSet = new Set(this.excludeIds);

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
      }

      this.entities.set(refs.filter(r => r.id != null && !excludeSet.has(r.id)));
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
