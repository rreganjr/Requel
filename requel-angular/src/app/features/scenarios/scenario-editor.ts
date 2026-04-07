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
import { Location } from '@angular/common';
import { Subscription } from 'rxjs';
import { DirtyCheckable } from '../../core/dirty-check.guard';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { SelectModule } from 'primeng/select';
import { MessageModule } from 'primeng/message';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { TooltipModule } from 'primeng/tooltip';
import { DragDropModule, CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ScenarioDto, StepDto, EditStepInput } from '../../models/scenario';
import { ScenarioService } from '../../core/scenario.service';
import { CommandService } from '../../core/command.service';
import { ProjectService } from '../../core/project.service';
import { PermissionService } from '../../core/permission.service';
import { EventStreamService } from '../../core/event-stream.service';
import { ScenarioSelectorDialogComponent, ScenarioRef } from '../../shared/scenario-selector-dialog';
import { AnnotationsSectionComponent } from '../../shared/annotations-section';

const SCENARIO_TYPE_OPTIONS = [
  { label: 'Primary', value: 'Primary' },
  { label: 'PreCondition', value: 'PreCondition' },
  { label: 'Optional', value: 'Optional' },
  { label: 'Alternative', value: 'Alternative' },
  { label: 'Exception', value: 'Exception' },
];

interface StepNodeData {
  stepId: number | null;
  name: string;
  text: string | null;
  scenarioType: string;
  isScenario: boolean;
  isNew: boolean;
}

@Component({
  selector: 'app-scenario-editor',
  standalone: true,
  imports: [FormsModule, ButtonModule, InputText, TextareaModule, SelectModule,
            MessageModule, ConfirmDialogModule, TooltipModule, DragDropModule,
            ScenarioSelectorDialogComponent, AnnotationsSectionComponent],
  providers: [ConfirmationService],
  template: `
    <div class="scenario-editor">
      <div class="page-header">
        <h2>{{ isNew() ? 'New Scenario' : scenarioName() }}</h2>
        <div class="page-actions">
          <p-button label="Back" icon="pi pi-arrow-left" severity="secondary"
                    [outlined]="true" (onClick)="onBack()" />
          @if (!isNew()) {
            @if (canEdit()) {
              <p-button label="Copy" icon="pi pi-copy" severity="secondary"
                        [outlined]="true" (onClick)="onCopy()" />
            }
            @if (canDelete()) {
              <p-button label="Delete" icon="pi pi-trash" severity="danger"
                        [outlined]="true" (onClick)="onDelete()" />
            }
          }
        </div>
      </div>

      @if (errorMessage()) {
        <p-message severity="error" [text]="errorMessage()!" />
      }

      <div class="form-grid">
        <label for="name">Name</label>
        <input id="name" pInputText [(ngModel)]="name" placeholder="Scenario name"
               (ngModelChange)="trackChanges()" />

        <label for="type">Type</label>
        <p-select id="type" [(ngModel)]="scenarioType" [options]="typeOptions"
                  optionLabel="label" optionValue="value"
                  (ngModelChange)="trackChanges()" />

        <label for="text">Description</label>
        <textarea id="text" pTextarea [(ngModel)]="text" rows="4"
                  placeholder="Scenario description"
                  (ngModelChange)="trackChanges()"></textarea>
      </div>

      <div class="form-actions">
        <p-button label="Save" icon="pi pi-check" (onClick)="onSave()" [loading]="saving()"
                  [disabled]="!isNew() && !hasChanges()" />
      </div>

      <!-- Steps section -->
      @if (!isNew()) {
        <div class="section">
          <div class="section-header">
            <h3>Steps</h3>
            @if (canEdit()) {
              <div class="section-actions">
                <p-button label="Add Sub-scenario" icon="pi pi-sitemap" size="small"
                          severity="secondary" [outlined]="true"
                          (onClick)="showScenarioSelector = true" />
              </div>
            }
          </div>

          <div cdkDropList [cdkDropListDisabled]="!canEdit()"
               (cdkDropListDropped)="onDrop($event)"
               class="step-list">
            @if (canEdit()) {
              <div class="add-step-row" (click)="addStepAt(0)">
                <i class="pi pi-plus"></i> Add step
              </div>
            }
            @for (step of stepNodes(); track step) {
              <div cdkDrag class="step-row">
                  @if (canEdit()) {
                    <span cdkDragHandle class="drag-handle" pTooltip="Drag to reorder" tooltipPosition="left">
                      <i class="pi pi-bars"></i>
                    </span>
                  }
                  @if (step.isScenario) {
                    <i class="pi pi-sitemap step-icon"></i>
                    <a class="entity-link step-name"
                       (click)="navigateToScenario(step.stepId!)">{{ step.name }}</a>
                    <span class="step-type-badge">{{ step.scenarioType }}</span>
                    @if (canEdit()) {
                      <p-button icon="pi pi-times" severity="danger" [text]="true"
                                size="small" pTooltip="Remove from scenario"
                                (onClick)="removeStep(step)" />
                    }
                  } @else {
                    <input pInputText [(ngModel)]="step.name"
                           class="step-name-input"
                           placeholder="Step description..."
                           [disabled]="!canEdit()"
                           (keydown)="$event.stopPropagation()"
                           (blur)="onStepNameChange()" />
                    @if (canEdit()) {
                      <p-button icon="pi pi-pencil" [text]="true" size="small"
                                pTooltip="Edit details" tooltipPosition="top"
                                (onClick)="openStepEdit(step)" />
                      <p-button icon="pi pi-plus" severity="secondary" [text]="true"
                                size="small" pTooltip="Add step below" tooltipPosition="top"
                                (onClick)="addStepBelow(step)" />
                      <p-button icon="pi pi-times" severity="danger" [text]="true"
                                size="small" pTooltip="Remove step" tooltipPosition="top"
                                (onClick)="removeStep(step)" />
                    }
                  }
                  <!-- CDK drag placeholder styling -->
                  <div *cdkDragPlaceholder class="step-row-placeholder"></div>
                </div>
              }
            @if (canEdit()) {
              <div class="add-step-row" (click)="addStep()">
                <i class="pi pi-plus"></i> Add step
              </div>
            }
          </div>

          @if (stepsSaveNeeded()) {
            <div class="steps-save-note">
              <p-message severity="info" text="Steps have unsaved changes. Click Save to apply." />
            </div>
          }
        </div>
      }

      <!-- Step detail edit popup -->
      @if (editingStep()) {
        <div class="edit-popup-overlay" (click)="closeStepEdit()">
          <div class="edit-popup-content" (click)="$event.stopPropagation()">
            <h4>Step Details</h4>
            <div class="edit-popup-grid">
              <label>Name</label>
              <input pInputText [(ngModel)]="editingName" placeholder="Step description..." />
              <label>Type</label>
              <p-select [(ngModel)]="editingType" [options]="typeOptions"
                        optionLabel="label" optionValue="value" />
              <label>Notes</label>
              <textarea pTextarea [(ngModel)]="editingText" rows="4"
                        placeholder="Additional details or notes..."></textarea>
            </div>
            <div class="edit-popup-actions">
              <p-button label="Apply" icon="pi pi-check" size="small"
                        (onClick)="applyStepEdit()" />
              <p-button label="Cancel" severity="secondary" [outlined]="true" size="small"
                        (onClick)="closeStepEdit()" />
            </div>
          </div>
        </div>
      }

      <app-scenario-selector-dialog
        [visible]="showScenarioSelector"
        [projectName]="projectName"
        [excludeIds]="excludeScenarioIds()"
        (selected)="onSubScenarioSelected($event)"
        (closed)="showScenarioSelector = false" />

      <app-annotations-section
        [projectName]="projectName"
        entityType="Scenario"
        [entityId]="scenarioId"
        [canEdit]="canEdit()" />

      <p-confirmDialog />
    </div>
  `,
  styles: [`
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
    .page-actions { display: flex; gap: 0.5rem; }
    .form-grid { display: grid; grid-template-columns: 120px 1fr; gap: 0.75rem 1rem; align-items: start; max-width: 700px; }
    .form-actions { margin-top: 1rem; }
    .section { margin-top: 1.5rem; }
    .section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem; }
    .section-header h3 { margin: 0; }
    .section-actions { display: flex; gap: 0.5rem; }
    .steps-save-note { margin-top: 0.5rem; }
    .empty-text { color: var(--p-text-secondary-color); font-style: italic; }
    .entity-link { cursor: pointer; color: var(--p-primary-color); text-decoration: underline; }

    /* Step list */
    .step-list { border: 1px solid var(--p-surface-200); border-radius: 6px; }
    .step-row {
      display: flex; align-items: center; gap: 0.5rem;
      padding: 0.4rem 0.5rem;
      border-bottom: 1px solid var(--p-surface-100);
      background: var(--p-surface-0);
    }
    .step-row:last-child { border-bottom: none; }
    .step-row.cdk-drag-animating { transition: transform 250ms cubic-bezier(0,0,0.2,1); }
    .step-list.cdk-drop-list-dragging .step-row:not(.cdk-drag-placeholder) { transition: transform 250ms cubic-bezier(0,0,0.2,1); }
    .step-row-placeholder {
      height: 41px; background: var(--p-primary-50, #e8f0fe);
      border: 2px dashed var(--p-primary-300, #93b4fb);
      border-radius: 4px; flex: 1;
    }

    .add-step-row {
      display: flex; align-items: center; justify-content: center; gap: 0.35rem;
      padding: 0.3rem; cursor: pointer;
      font-size: 0.8rem; color: var(--p-text-secondary-color);
      border-bottom: 1px dashed var(--p-surface-200);
      transition: background 0.15s, color 0.15s;
    }
    .add-step-row:last-child { border-bottom: none; border-top: 1px dashed var(--p-surface-200); }
    .add-step-row:hover { background: var(--p-surface-50); color: var(--p-primary-color); }

    .drag-handle {
      cursor: grab; color: var(--p-text-secondary-color);
      padding: 0.25rem; flex-shrink: 0; line-height: 1;
    }
    .drag-handle:active { cursor: grabbing; }

    .step-name-input { flex: 1; min-width: 0; font-size: 0.9rem; }
    .step-name { flex: 1; font-size: 0.9rem; }
    .step-icon { color: var(--p-text-secondary-color); font-size: 0.85rem; flex-shrink: 0; }
    .step-type-badge {
      font-size: 0.75rem; padding: 0.1rem 0.4rem;
      background: var(--p-surface-200); border-radius: 4px;
      white-space: nowrap; flex-shrink: 0;
    }

    /* Step edit popup */
    .edit-popup-overlay {
      position: fixed; top: 0; left: 0; right: 0; bottom: 0;
      z-index: 1000; display: flex; align-items: center; justify-content: center;
      background: rgba(0,0,0,0.3);
    }
    .edit-popup-content {
      background: var(--p-surface-0); padding: 1.5rem; border-radius: 8px;
      min-width: 380px; max-width: 500px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);
    }
    .edit-popup-content h4 { margin: 0 0 1rem; }
    .edit-popup-grid { display: grid; grid-template-columns: 80px 1fr; gap: 0.5rem; align-items: start; }
    .edit-popup-actions { display: flex; gap: 0.5rem; margin-top: 1rem; }
  `]
})
export class ScenarioEditorComponent implements OnInit, OnDestroy, DirtyCheckable {
  isNew = signal(true);
  scenarioName = signal('');
  scenario = signal<ScenarioDto | null>(null);
  errorMessage = signal<string | null>(null);
  saving = signal(false);
  canEdit = signal(false);
  canDelete = signal(false);
  hasChanges = signal(false);
  stepsSaveNeeded = signal(false);
  stepNodes = signal<StepNodeData[]>([]);
  editingStep = signal<StepNodeData | null>(null);

  name = '';
  text = '';
  scenarioType = 'Primary';
  typeOptions = SCENARIO_TYPE_OPTIONS;
  showScenarioSelector = false;
  editingName = '';
  editingType = 'Primary';
  editingText = '';

  projectName = '';
  scenarioId: number | null = null;
  private version: number | null = null;
  private originalName = '';
  private originalText = '';
  private originalScenarioType = 'Primary';
  private paramSub?: Subscription;
  private sseSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private location: Location,
    private scenarioService: ScenarioService,
    private commandService: CommandService,
    private projectService: ProjectService,
    private permissionService: PermissionService,
    private confirmationService: ConfirmationService,
    private messageService: MessageService,
    private eventStreamService: EventStreamService
  ) {}

  ngOnInit(): void {
    this.paramSub = this.route.paramMap.subscribe(async params => {
      this.projectName = params.get('name') ?? '';
      await this.permissionService.loadForProject(this.projectName);
      this.canEdit.set(this.permissionService.canEdit('Scenario'));
      this.canDelete.set(this.permissionService.canDelete('Scenario'));

      const idParam = params.get('scenarioId') ?? '';
      if (idParam === 'new') {
        this.isNew.set(true);
        this.scenario.set(null);
        this.name = '';
        this.text = '';
        this.scenarioType = 'Primary';
        this.version = null;
        this.stepNodes.set([]);
      } else {
        this.isNew.set(false);
        this.scenarioId = +idParam;
        this.loadScenario();
      }
    });
  }

  hasUnsavedChanges(): boolean {
    return this.hasChanges();
  }

  ngOnDestroy(): void {
    this.paramSub?.unsubscribe();
    if (this.scenarioId) {
      void this.eventStreamService.removeSubscription('Scenario', this.scenarioId);
    }
    this.sseSub?.unsubscribe();
  }

  private async loadScenario(): Promise<void> {
    try {
      const s = await this.scenarioService.getScenario(this.projectName, this.scenarioId!);
      this.scenario.set(s);
      this.scenarioName.set(s.name);
      this.name = s.name;
      this.text = s.text ?? '';
      this.scenarioType = s.scenarioType ?? 'Primary';
      this.version = s.version;
      this.originalName = s.name;
      this.originalText = s.text ?? '';
      this.originalScenarioType = s.scenarioType ?? 'Primary';
      this.hasChanges.set(false);
      this.stepsSaveNeeded.set(false);
      this.stepNodes.set(this.stepsToNodes(s.steps ?? []));
    } catch {
      this.errorMessage.set('Failed to load scenario.');
    }
    if (this.scenarioId && !this.sseSub) {
      void this.eventStreamService.addSubscription('Scenario', this.scenarioId);
      this.sseSub = this.eventStreamService.events$.subscribe(envelope => {
        if (envelope.targetType === 'Scenario' && envelope.targetId === this.scenarioId) {
          void this.loadScenario();
        }
      });
    }
  }

  private stepsToNodes(steps: StepDto[]): StepNodeData[] {
    return steps.map(step => ({
      stepId: step.id,
      name: step.name,
      text: step.text ?? null,
      scenarioType: step.scenarioType ?? this.scenarioType,
      isScenario: step.isScenario,
      isNew: false
    }));
  }

  trackChanges(): void {
    this.hasChanges.set(
      this.name !== this.originalName ||
      this.text !== this.originalText ||
      this.scenarioType !== this.originalScenarioType ||
      this.stepsSaveNeeded()
    );
  }

  onStepNameChange(): void {
    this.stepsSaveNeeded.set(true);
    this.hasChanges.set(true);
  }

  excludeScenarioIds(): number[] {
    const ids: number[] = [];
    if (this.scenarioId) ids.push(this.scenarioId);
    for (const step of this.stepNodes()) {
      if (step.isScenario && step.stepId != null) ids.push(step.stepId);
    }
    return ids;
  }

  addStep(): void {
    this.stepNodes.update(steps => [...steps, {
      stepId: null, name: '', text: null,
      scenarioType: this.scenarioType, isScenario: false, isNew: true
    }]);
    this.stepsSaveNeeded.set(true);
    this.hasChanges.set(true);
  }

  addStepAt(index: number): void {
    const steps = [...this.stepNodes()];
    steps.splice(index, 0, {
      stepId: null, name: '', text: null,
      scenarioType: this.scenarioType, isScenario: false, isNew: true
    });
    this.stepNodes.set(steps);
    this.stepsSaveNeeded.set(true);
    this.hasChanges.set(true);
  }

  addStepBelow(step: StepNodeData): void {
    const steps = [...this.stepNodes()];
    const idx = steps.indexOf(step);
    steps.splice(idx + 1, 0, {
      stepId: null, name: '', text: null,
      scenarioType: this.scenarioType, isScenario: false, isNew: true
    });
    this.stepNodes.set(steps);
    this.stepsSaveNeeded.set(true);
    this.hasChanges.set(true);
  }

  removeStep(step: StepNodeData): void {
    this.stepNodes.update(steps => steps.filter(s => s !== step));
    this.stepsSaveNeeded.set(true);
    this.hasChanges.set(true);
  }

  onDrop(event: CdkDragDrop<StepNodeData[]>): void {
    const steps = [...this.stepNodes()];
    moveItemInArray(steps, event.previousIndex, event.currentIndex);
    this.stepNodes.set(steps);
    this.stepsSaveNeeded.set(true);
    this.hasChanges.set(true);
  }

  openStepEdit(step: StepNodeData): void {
    this.editingStep.set(step);
    this.editingName = step.name;
    this.editingType = step.scenarioType;
    this.editingText = step.text ?? '';
  }

  applyStepEdit(): void {
    const step = this.editingStep();
    if (!step) return;
    step.name = this.editingName;
    step.scenarioType = this.editingType;
    step.text = this.editingText || null;
    this.stepNodes.set([...this.stepNodes()]);
    this.editingStep.set(null);
    this.stepsSaveNeeded.set(true);
    this.hasChanges.set(true);
  }

  closeStepEdit(): void {
    this.editingStep.set(null);
  }

  onSubScenarioSelected(ref: ScenarioRef): void {
    this.showScenarioSelector = false;
    this.stepNodes.update(steps => [...steps, {
      stepId: ref.id, name: ref.name, text: null,
      scenarioType: ref.scenarioType ?? this.scenarioType,
      isScenario: true, isNew: false
    }]);
    this.stepsSaveNeeded.set(true);
    this.hasChanges.set(true);
  }

  navigateToScenario(scenarioId: number): void {
    this.router.navigate(['/projects', this.projectName, 'scenarios', scenarioId]);
  }

  private buildStepInputs(): EditStepInput[] {
    return this.stepNodes().map(step => ({
      stepId: step.stepId,
      name: step.name,
      text: step.text || null,
      scenarioTypeName: step.scenarioType,
      isScenario: step.isScenario
    }));
  }

  async onSave(): Promise<void> {
    this.saving.set(true);
    this.errorMessage.set(null);
    try {
      const input: Record<string, unknown> = {
        projectName: this.projectName,
        name: this.name,
        text: this.text || null,
        scenarioTypeName: this.scenarioType,
        steps: this.buildStepInputs()
      };
      if (this.version != null) input['version'] = this.version;
      if (this.scenarioId != null) input['scenarioId'] = this.scenarioId;

      const result = await this.commandService.execute('EditScenario', input);
      if (result.success) {
        this.messageService.add({ severity: 'success', summary: 'Saved', detail: 'Scenario saved.' });
        if (this.isNew()) {
          this.projectService.notifyTreeChanged();
          if (result.entity) {
            const saved = result.entity as ScenarioDto;
            this.hasChanges.set(false);
            this.router.navigate(['/projects', this.projectName, 'scenarios', saved.id]);
          }
        } else {
          this.originalName = this.name;
          this.originalText = this.text;
          this.originalScenarioType = this.scenarioType;
          this.hasChanges.set(false);
          this.stepsSaveNeeded.set(false);
          await this.loadScenario();
        }
      } else {
        this.errorMessage.set(result.error ?? 'Save failed.');
      }
    } catch {
      this.errorMessage.set('An unexpected error occurred.');
    } finally {
      this.saving.set(false);
    }
  }

  onCopy(): void {
    this.confirmationService.confirm({
      message: 'Create a copy of this scenario?',
      accept: async () => {
        const result = await this.commandService.execute('CopyScenario', {
          projectName: this.projectName,
          scenarioId: this.scenarioId
        });
        if (result.success && result.entity) {
          this.projectService.notifyTreeChanged();
          const copy = result.entity as ScenarioDto;
          this.router.navigate(['/projects', this.projectName, 'scenarios', copy.id]);
        } else {
          this.errorMessage.set(result.error ?? 'Copy failed.');
        }
      }
    });
  }

  onDelete(): void {
    this.confirmationService.confirm({
      message: 'Are you sure you want to delete this scenario?',
      accept: async () => {
        const result = await this.commandService.execute('DeleteScenario', {
          projectName: this.projectName,
          scenarioId: this.scenarioId,
          version: this.version
        });
        if (result.success) {
          this.projectService.notifyTreeChanged();
          this.router.navigate(['/projects', this.projectName, 'scenarios']);
        } else {
          this.errorMessage.set(result.error ?? 'Delete failed.');
        }
      }
    });
  }

  onBack(): void {
    this.location.back();
  }
}
