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
import { Component, EventEmitter, Input, OnChanges, Output, signal, SimpleChanges, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule, FormGroup, FormControl, ReactiveFormsModule } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { ScenarioDto } from '../models/scenario';
import { ScenarioService } from '../core/scenario.service';
import { CommandService } from '../core/command.service';
import { SubmitErrorComponent } from './app-submit-error';
import { InlineErrorComponent } from './app-inline-error';
import { notBlank } from './form-errors';

export interface ScenarioRef {
  id: number;
  name: string;
  scenarioType: string | null;
}

const SCENARIO_TYPE_OPTIONS = [
  { label: 'Primary', value: 'Primary' },
  { label: 'PreCondition', value: 'PreCondition' },
  { label: 'Optional', value: 'Optional' },
  { label: 'Alternative', value: 'Alternative' },
  { label: 'Exception', value: 'Exception' },
];

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-scenario-selector-dialog',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule, DialogModule, TableModule, ButtonModule, InputText, SelectModule, SubmitErrorComponent, InlineErrorComponent],
  template: `
    <p-dialog header="Add Sub-scenario" [(visible)]="visible" [modal]="true" [focusOnShow]="true"
              closeAriaLabel="Close" appendTo="body" [style]="{ width: '560px' }" (onHide)="onHide()"
              styleClass="scenario-selector-dialog">

      <!-- Inline new-scenario creation form -->
      @if (showCreateForm) {
        <div class="create-form" data-testid="scenario-selector-create-form" [formGroup]="createForm">
          <h4>New Scenario</h4>
          <app-submit-error [message]="createError()" testid="scenario-selector-create-error" />
          <div class="create-grid">
            <label for="newScenarioName">Name</label>
            <input id="newScenarioName" pInputText formControlName="name"
                   data-testid="scenario-selector-name-input"
                   placeholder="Scenario name"
                   [attr.aria-invalid]="nameErr.message() ? 'true' : null"
                   [attr.aria-describedby]="nameErr.message() ? 'scenario-selector-name-error' : null" />
            <label for="newScenarioType">Type</label>
            <p-select inputId="newScenarioType" formControlName="scenarioType" [options]="typeOptions"
                      optionLabel="label" optionValue="value" />
          </div>
          <app-inline-error #nameErr [control]="createForm.controls.name" id="scenario-selector-name-error"
                            [submitted]="submitted()" [overrides]="{ required: 'Name is required.' }"
                            testid="scenario-selector-name-error" />
          <div class="create-actions">
            <p-button label="Create & Add" icon="pi pi-check" size="small"
                      data-testid="scenario-selector-create-add"
                      (onClick)="onCreateAndAdd()" />
            <p-button label="Cancel" severity="secondary" [outlined]="true" size="small"
                      data-testid="scenario-selector-cancel-create"
                      (onClick)="cancelCreate()" />
          </div>
        </div>
        <hr />
      }

      <div class="dialog-toolbar">
        <input pInputText [(ngModel)]="searchText" placeholder="Search..."
               aria-label="Search scenarios"
               data-testid="scenario-selector-search"
               (input)="dt.filterGlobal(searchText, 'contains')" class="w-full" />
        @if (!showCreateForm) {
          <p-button label="New Scenario" icon="pi pi-plus" size="small" severity="secondary"
                    [outlined]="true" data-testid="scenario-selector-new-button"
                    (onClick)="startCreate()" />
        }
      </div>

      <p-table #dt [value]="scenarios()" [loading]="loading()" [paginator]="true" [rows]="8"
               [rowHover]="true" selectionMode="single" (onRowSelect)="onSelect($event)"
               [globalFilterFields]="['name']"
               styleClass="scenario-selector-table">
        <ng-template #header>
          <tr>
            <th pSortableColumn="name">Name <p-sortIcon field="name" /></th>
            <th pSortableColumn="scenarioType">Type <p-sortIcon field="scenarioType" /></th>
          </tr>
        </ng-template>
        <ng-template #body let-s>
          <tr [pSelectableRow]="s" data-testid="scenario-selector-row">
            <td>{{ s.name }}</td>
            <td>{{ s.scenarioType }}</td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr><td colspan="2" class="text-center">No scenarios available.</td></tr>
        </ng-template>
      </p-table>
    </p-dialog>
  `,
  styles: [`
    .dialog-toolbar { display: flex; gap: 0.5rem; align-items: center; margin-bottom: 0.75rem; }
    .create-form { margin-bottom: 0.75rem; }
    .create-form h4 { margin: 0 0 0.75rem; }
    .create-grid { display: grid; grid-template-columns: 80px 1fr; gap: 0.5rem; align-items: center; }
    .create-actions { display: flex; gap: 0.5rem; margin-top: 0.75rem; }
    .create-error { color: var(--p-red-500); font-size: 0.85rem; margin-top: 0.5rem; }
    hr { margin: 0.75rem 0; border: none; border-top: 1px solid var(--p-surface-200); }
  `]
})
export class ScenarioSelectorDialogComponent implements OnChanges {
  @Input() visible = false;
  @Input() projectName = '';
  @Input() excludeIds: number[] = [];
  @Output() selected = new EventEmitter<ScenarioRef>();
  @Output() closed = new EventEmitter<void>();

  scenarios = signal<ScenarioDto[]>([]);
  loading = signal(false);
  createError = signal<string | null>(null);

  searchText = '';
  showCreateForm = false;
  readonly submitted = signal(false);
  readonly createForm = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [notBlank()] }),
    scenarioType: new FormControl('Primary', { nonNullable: true }),
  });
  typeOptions = SCENARIO_TYPE_OPTIONS;

  constructor(
    private scenarioService: ScenarioService,
    private commandService: CommandService
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (this.visible && (changes['visible'] || changes['projectName'])) {
      this.showCreateForm = false;
      this.createForm.reset({ name: '', scenarioType: 'Primary' });
      this.submitted.set(false);
      this.createError.set(null);
      this.searchText = '';
      this.loadScenarios();
    }
  }

  private async loadScenarios(): Promise<void> {
    this.loading.set(true);
    try {
      const all = await this.scenarioService.listScenarios(this.projectName);
      const excludeSet = new Set(this.excludeIds);
      this.scenarios.set(all.filter(s => !excludeSet.has(s.id)));
    } finally {
      this.loading.set(false);
    }
  }

  onSelect(event: { data?: ScenarioDto | ScenarioDto[] }): void {
    const s = Array.isArray(event.data) ? event.data[0] : event.data;
    if (s) {
      this.selected.emit({ id: s.id, name: s.name, scenarioType: s.scenarioType });
      this.visible = false;
      this.closed.emit();
    }
  }

  startCreate(): void {
    this.createForm.reset({ name: '', scenarioType: 'Primary' });
    this.submitted.set(false);
    this.createError.set(null);
    this.showCreateForm = true;
  }

  cancelCreate(): void {
    this.showCreateForm = false;
    this.submitted.set(false);
    this.createError.set(null);
    this.createForm.reset({ name: '', scenarioType: 'Primary' });
  }

  async onCreateAndAdd(): Promise<void> {
    this.submitted.set(true);
    if (this.createForm.invalid) {
      this.createForm.controls.name.markAsTouched();
      return;
    }
    this.createError.set(null);
    try {
      const raw = this.createForm.getRawValue();
      const result = await this.commandService.execute('EditScenario', {
        projectName: this.projectName,
        name: raw.name.trim(),
        scenarioTypeName: raw.scenarioType,
        steps: []
      });
      if (result.success && result.entity) {
        const created = result.entity as ScenarioDto;
        this.selected.emit({ id: created.id!, name: created.name, scenarioType: created.scenarioType });
        this.visible = false;
        this.closed.emit();
      } else {
        this.createError.set(result.error ?? 'Failed to create scenario.');
      }
    } catch {
      this.createError.set('Failed to create scenario.');
    }
  }

  onHide(): void {
    this.closed.emit();
  }
}
