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
import { Component, OnDestroy, OnInit, signal, ChangeDetectionStrategy, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { toSignal } from '@angular/core/rxjs-interop';
import { PageHeaderComponent } from '../../shared/page-header';
import { AppCardComponent } from '../../shared/app-card';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Location, NgTemplateOutlet } from '@angular/common';
import { map } from 'rxjs/operators';
import { DirtyCheckable } from '../../core/dirty-check.guard';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { SelectModule } from 'primeng/select';
import { MessageModule } from 'primeng/message';
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import { UpdateBannerComponent } from '../../shared/app-update-banner';
import { AnnouncerService } from '../../core/announcer.service';
import { DialogModule } from 'primeng/dialog';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { TooltipModule } from 'primeng/tooltip';
import { DragDropModule, CdkDragDrop } from '@angular/cdk/drag-drop';
import { ConfirmationService, MessageService } from 'primeng/api';
import { CommandResult } from '../../models/command';
import { ScenarioDto, StepDto, EditStepInput } from '../../models/scenario';
import { ScenarioService } from '../../core/scenario.service';
import { CommandService, isNetworkError } from '../../core/command.service';
import { ProjectService } from '../../core/project.service';
import { PermissionService } from '../../core/permission.service';
import { EventStreamService } from '../../core/event-stream.service';
import { ScenarioSelectorDialogComponent, ScenarioRef } from '../../shared/scenario-selector-dialog';
import { AnnotationsSectionComponent } from '../../shared/annotations-section';
import { LoadingStateComponent } from '../../shared/loading-state';
import { ErrorStateComponent } from '../../shared/error-state';
import { AppFieldComponent, AppFieldControlDirective } from '../../shared/app-field';
import { InlineErrorComponent } from '../../shared/app-inline-error';
import {
  AppFormWizardComponent,
  AppWizardStepComponent,
  WizardCommitRequest,
} from '../../shared/app-form-wizard';
import { applyCommandErrors, clearServerErrors } from '../../shared/form-errors';
import { ARTIFACT_NAME_MAX_LENGTH } from '../../shared/validation-limits';

const SCENARIO_TYPE_OPTIONS = [
  { label: 'Primary', value: 'Primary' },
  { label: 'PreCondition', value: 'PreCondition' },
  { label: 'Optional', value: 'Optional' },
  { label: 'Alternative', value: 'Alternative' },
  { label: 'Exception', value: 'Exception' },
];


/** Joins page-level violations that resolved to no control. */
const SEPARATOR = '; ';

/** Wording for the stale-version recovery path, so the 409 case reads as recoverable. */
const STALE_VERSION_MESSAGE =
  'This scenario was changed elsewhere. Your copy has been refreshed - review the values and continue.';

/**
 * One scenario step as a reactive group (#143). The editable fields (`name`, `scenarioType`,
 * `text`) carry validators; `stepId` / `isScenario` / `isNew` are metadata controls that ride
 * inside the group so a reorder moves a step's identity with it. `getRawValue()` emits all six —
 * including the disabled `name` on sub-scenario reference rows — so the submit payload is unchanged.
 */
type StepGroup = FormGroup<{
  stepId: FormControl<number | null>;
  name: FormControl<string>;
  text: FormControl<string | null>;
  scenarioType: FormControl<string>;
  isScenario: FormControl<boolean>;
  isNew: FormControl<boolean>;
}>;

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-scenario-editor',
  standalone: true,
  imports: [PageHeaderComponent, AppCardComponent, RouterLink, NgTemplateOutlet,
            ReactiveFormsModule, ButtonModule, InputText, TextareaModule, SelectModule,
            MessageModule, SubmitErrorComponent, UpdateBannerComponent, DialogModule, ConfirmDialogModule, TooltipModule, DragDropModule,
            ScenarioSelectorDialogComponent, AnnotationsSectionComponent, LoadingStateComponent,
            ErrorStateComponent, AppFieldComponent, AppFieldControlDirective,
            AppFormWizardComponent, AppWizardStepComponent, InlineErrorComponent],
  providers: [ConfirmationService],
  template: `
    <div class="scenario-editor" data-testid="scenario-editor">
      <div class="page-header">
        <app-page-header [title]="isNew() ? 'New Scenario' : scenarioName()" />
        <div class="page-actions">
          <p-button label="Back" icon="pi pi-arrow-left" severity="secondary" data-testid="scenario-back"
                    [outlined]="true" (onClick)="onBack()" />
          @if (!isNew()) {
            @if (canEdit()) {
              <p-button label="Copy" icon="pi pi-copy" severity="secondary" data-testid="scenario-copy"
                        [outlined]="true" (onClick)="onCopy()" />
            }
            @if (canDelete()) {
              <p-button label="Delete" icon="pi pi-trash" severity="danger" data-testid="scenario-delete"
                        [outlined]="true" (onClick)="onDelete()" />
            }
          }
        </div>
      </div>

      <app-submit-error [message]="errorMessage()" testid="scenario-error" [retryable]="retryable()" (retry)="onSave()" />
      @if (updateAvailable()) {
        <app-update-banner message="This scenario was changed elsewhere. Your unsaved changes are preserved."
                           testid="scenario-update-banner"
                           (reload)="reloadFromExternalChange()" (dismiss)="updateAvailable.set(false)" />
      }

      @if (loading()) {
        <app-card>
          <app-loading-state label="Loading scenario…" [lines]="4" testid="scenario-editor-loading" />
        </app-card>
      } @else if (loadError()) {
        <app-error-state [message]="loadError()!" testid="scenario-editor-load-error"
                         (retry)="retryLoad()" />
      } @else if (isNew()) {
        <!--
          Create runs as a wizard (#173). Steps *are* the scenario - gating them behind a
          first save is what made the old create flow produce an empty scenario the user had
          to navigate back into.

          Unlike goal/story, both steps commit the SAME command: EditScenario carries the
          whole step list and rebuilds scenario.getSteps() server-side, so there is no
          per-association command to issue. Step 2 therefore re-sends name/type/text, which
          is why saveDetails() always reads them from the form rather than from a snapshot
          taken at step 1 - otherwise a back-navigation edit to the name would be silently
          discarded on Done.
        -->
        <app-form-wizard
          [(activeKey)]="wizardStep"
          navLabel="New scenario steps"
          (stepCommit)="onStepCommit($event)"
          (cancelled)="onBack()"
          (finished)="onWizardFinished()"
          data-testid="scenario-wizard"
        >
          <app-wizard-step key="details" label="Details" helper="Name, type and description"
                           [form]="detailsForm">
            <ng-template>
              <ng-container [ngTemplateOutlet]="detailsFields" />
            </ng-template>
          </app-wizard-step>

          <app-wizard-step key="steps" label="Steps" helper="Write the scenario">
            <ng-template>
              <!-- heading: false - the wizard panel's own h2 already reads "Steps". -->
              <ng-container [ngTemplateOutlet]="stepsSection"
                            [ngTemplateOutletContext]="{ heading: false }" />
            </ng-template>
          </app-wizard-step>
        </app-form-wizard>
      } @else {
        <app-card>
          <ng-container [ngTemplateOutlet]="detailsFields" />

          <div class="form-actions">
            <p-button label="Save" icon="pi pi-check" data-testid="scenario-save"
                      [disabled]="!canSave()" [loading]="saving()" (onClick)="onSave()" />
          </div>
        </app-card>

        <ng-container [ngTemplateOutlet]="stepsSection"
                      [ngTemplateOutletContext]="{ heading: true }" />
      }

      <!-- Step detail edit dialog -->
      <p-dialog [visible]="editingStep() !== null" (visibleChange)="onStepDialogVisibleChange($event)"
                [modal]="true" [focusOnShow]="true" [dismissableMask]="true" closeAriaLabel="Close"
                [style]="{ width: '32rem' }" appendTo="body" header="Step Details"
                data-testid="scenario-step-edit-dialog">
        <app-field label="Name" controlId="stepEditName" [control]="editForm.controls.name"
                   [errorMessages]="stepNameErrors" [submitted]="editSubmitted()">
          <input appFieldControl pInputText [formControl]="editForm.controls.name" id="stepEditName"
                 [attr.maxlength]="nameMaxLength" data-testid="scenario-step-edit-name"
                 placeholder="Step description..." />
        </app-field>

        <app-field label="Type" controlId="stepEditType" [control]="editForm.controls.scenarioType"
                   [submitted]="editSubmitted()">
          <p-select appFieldControl inputId="stepEditType" data-testid="scenario-step-edit-type"
                    [formControl]="editForm.controls.scenarioType"
                    [options]="typeOptions" optionLabel="label" optionValue="value" appendTo="body" />
        </app-field>

        <app-field label="Notes" controlId="stepEditText" [control]="editForm.controls.text" [divider]="false"
                   [submitted]="editSubmitted()">
          <textarea appFieldControl pTextarea [formControl]="editForm.controls.text" id="stepEditText" rows="4"
                    data-testid="scenario-step-edit-text"
                    placeholder="Additional details or notes..."></textarea>
        </app-field>
        <div class="dialog-actions">
          <p-button label="Apply" icon="pi pi-check" size="small" data-testid="scenario-step-edit-apply"
                    (onClick)="applyStepEdit()" />
          <p-button label="Cancel" severity="secondary" [outlined]="true" size="small"
                    (onClick)="closeStepEdit()" />
        </div>
      </p-dialog>

      <app-scenario-selector-dialog
        [visible]="showScenarioSelector"
        [projectName]="projectName"
        [excludeIds]="excludeScenarioIds()"
        (selected)="onSubScenarioSelected($event)"
        (closed)="showScenarioSelector = false" />

      <!--
        Annotations render against a persisted entity, so they stay outside the wizard and
        appear once the scenario exists rather than as a dead panel during create.
      -->
      @if (scenarioId != null) {
        <app-annotations-section
          [projectName]="projectName"
          entityType="Scenario"
          [entityId]="scenarioId"
          [canEdit]="canEdit()" />
      }

      <p-confirmDialog />

      <!--
        Shared bodies. Each is used by both the wizard step and the edit view, so the two
        modes cannot drift apart. Controls bind with [formControl], not formControlName:
        these templates are projected into the wizard, where formControlName would look for
        a parent formGroup that is not there.
      -->
      <ng-template #detailsFields>
        <app-field label="Name" helper="What happens in this scenario."
                   controlId="name" [control]="detailsForm.controls.name"
                   [errorMessages]="nameErrors"
                   [submitted]="submitted()">
          <input appFieldControl pInputText [formControl]="detailsForm.controls.name" id="name"
                 [attr.maxlength]="nameMaxLength"
                 placeholder="Scenario name" data-testid="scenario-name" />
        </app-field>

        <app-field label="Type" controlId="scenarioTypeInput"
                   [control]="detailsForm.controls.scenarioType"
                   [submitted]="submitted()">
          <p-select appFieldControl inputId="scenarioTypeInput" data-testid="scenario-type"
                    [formControl]="detailsForm.controls.scenarioType"
                    [options]="typeOptions" optionLabel="label" optionValue="value" />
        </app-field>

        <app-field label="Description" controlId="text" [control]="detailsForm.controls.text" [divider]="false"
                   [submitted]="submitted()">
          <textarea appFieldControl pTextarea [formControl]="detailsForm.controls.text" id="text" rows="4"
                    placeholder="Scenario description" data-testid="scenario-text"></textarea>
        </app-field>
      </ng-template>

      <ng-template #stepsSection let-heading="heading">
        <div class="section">
          <div class="section-header">
            @if (heading) {
              <h2 class="rq-section-title">Steps</h2>
            }
            @if (canEdit()) {
              <div class="section-actions">
                <p-button label="Add Sub-scenario" icon="pi pi-sitemap" size="small"
                          severity="secondary" [outlined]="true"
                          data-testid="scenario-add-sub"
                          (onClick)="showScenarioSelector = true" />
              </div>
            }
          </div>

          <div cdkDropList data-testid="scenario-step-list" [cdkDropListDisabled]="!canEdit()"
               (cdkDropListDropped)="onDrop($event)"
               class="step-list">
            @if (canEdit()) {
              <button type="button" class="add-step-row" data-testid="scenario-add-step-top" (click)="addStepAt(0)">
                <i class="pi pi-plus" aria-hidden="true"></i> Add step
              </button>
            }
            @for (group of stepsForm.controls; track group; let stepIndex = $index) {
              <div cdkDrag class="step-row" data-testid="scenario-step-row"
                   [attr.data-step-index]="stepIndex">
                @if (canEdit()) {
                  <span cdkDragHandle class="drag-handle" data-testid="scenario-step-drag-handle"
                        pTooltip="Drag to reorder" tooltipPosition="left">
                    <i class="pi pi-bars"></i>
                  </span>
                }
                @if (group.controls.isScenario.value) {
                  <i class="pi pi-sitemap step-icon"></i>
                  <a class="entity-link step-name"
                     data-testid="scenario-step-link"
                     [routerLink]="['/projects', projectName, 'scenarios', group.controls.stepId.value!]">{{ group.controls.name.value }}</a>
                  <span class="step-type-badge">{{ group.controls.scenarioType.value }}</span>
                  @if (canEdit()) {
                    <p-button icon="pi pi-times" severity="danger" [text]="true"
                              data-testid="scenario-step-remove" [ariaLabel]="'Remove ' + group.controls.name.value + ' from scenario'"
                              size="small" pTooltip="Remove from scenario"
                              (onClick)="removeStep(group)" />
                  }
                } @else {
                  <input pInputText [formControl]="group.controls.name"
                         class="step-name-input"
                         data-testid="scenario-step-name"
                         placeholder="Step description..."
                         [attr.aria-label]="'Step ' + (stepIndex + 1) + ' description'"
                         [attr.maxlength]="nameMaxLength"
                         [readonly]="!canEdit()"
                         [attr.aria-invalid]="stepNameErr.message() ? 'true' : null"
                         [attr.aria-describedby]="stepNameErr.message() ? 'scenario-step-name-error-' + stepIndex : null"
                         (keydown)="$event.stopPropagation()"
                         (blur)="onStepNameChange()" />
                  @if (canEdit()) {
                    <p-button icon="pi pi-pencil" [text]="true" size="small"
                              data-testid="scenario-step-edit" ariaLabel="Edit step details"
                              pTooltip="Edit details" tooltipPosition="top"
                              (onClick)="openStepEdit(group)" />
                    <p-button icon="pi pi-plus" severity="secondary" [text]="true"
                              data-testid="scenario-step-add-below" ariaLabel="Add step below"
                              size="small" pTooltip="Add step below" tooltipPosition="top"
                              (onClick)="addStepBelow(group)" />
                    <p-button icon="pi pi-times" severity="danger" [text]="true"
                              data-testid="scenario-step-remove" ariaLabel="Remove step"
                              size="small" pTooltip="Remove step" tooltipPosition="top"
                              (onClick)="removeStep(group)" />
                  }
                  <!-- Shared #134 inline contract: shows once the control is touched or a submit was
                       attempted. app-inline-error reads touched inside its OWN change detection, so
                       the touched read stays out of this template's checkNoChanges pass. -->
                  <app-inline-error #stepNameErr [control]="group.controls.name"
                                    [id]="'scenario-step-name-error-' + stepIndex"
                                    [submitted]="submitted()"
                                    [overrides]="{ required: 'A step needs a name.' }"
                                    testid="scenario-step-name-error" />
                }
                <!-- CDK drag placeholder styling -->
                <div *cdkDragPlaceholder class="step-row-placeholder"></div>
              </div>
            }
            @if (canEdit()) {
              <button type="button" class="add-step-row" data-testid="scenario-add-step-bottom" (click)="addStep()">
                <i class="pi pi-plus" aria-hidden="true"></i> Add step
              </button>
            }
          </div>

          @if (stepsDirty()) {
            <div class="steps-save-note">
              <p-message severity="info"
                         [text]="isNew()
                           ? 'Steps have unsaved changes. Press Done to apply.'
                           : 'Steps have unsaved changes. Click Save to apply.'" />
            </div>
          }
        </div>
      </ng-template>
    </div>
  `,
  styles: [`
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
    .page-actions { display: flex; gap: 0.5rem; }
    .form-actions { margin-top: 1rem; }
    .section { margin-top: 1.5rem; }
    .section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem; }
    .section-actions { display: flex; gap: 0.5rem; }
    .steps-save-note { margin-top: 0.5rem; }
    .empty-text { color: var(--p-text-secondary-color); font-style: italic; }
    .entity-link { cursor: pointer; color: var(--p-primary-color); text-decoration: underline; }

    /* Step list */
    .step-list { border: 1px solid var(--p-surface-200); border-radius: 6px; }
    .step-row {
      display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap;
      padding: 0.4rem 0.5rem;
      border-bottom: 1px solid var(--p-surface-100);
      background: var(--p-surface-0);
    }
    /* Inline per-step required message drops to its own line under the input. */
    app-inline-error { display: block; }
    .step-row app-inline-error:has(p) { flex-basis: 100%; margin-left: 1.75rem; }
    .step-row:last-child { border-bottom: none; }
    .step-row.cdk-drag-animating { transition: transform 250ms cubic-bezier(0,0,0.2,1); }
    .step-list.cdk-drop-list-dragging .step-row:not(.cdk-drag-placeholder) { transition: transform 250ms cubic-bezier(0,0,0.2,1); }
    .step-row-placeholder {
      height: 41px; background: var(--p-primary-50, #e8f0fe);
      border: 2px dashed var(--p-primary-300, #93b4fb);
      border-radius: 4px; flex: 1;
    }

    /* Standalone action row: 36px comfortable target (issue #141, WCAG 2.5.8). */
    .add-step-row {
      display: flex; align-items: center; justify-content: center; gap: 0.35rem;
      width: 100%; min-height: var(--rq-target-comfortable);
      border: none; background: transparent; font-family: inherit;
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

    /* Step edit dialog */
    .dialog-actions { display: flex; gap: 0.5rem; justify-content: flex-end; margin-top: 1rem; }
  `]
})
export class ScenarioEditorComponent implements OnInit, OnDestroy, DirtyCheckable {
  isNew = signal(true);
  scenarioName = signal('');
  scenario = signal<ScenarioDto | null>(null);
  errorMessage = signal<string | null>(null);
  retryable = signal(false);
  /** Sets the inline submit error and, by default, marks it non-retryable. */
  private showError(message: string | null): void {
    this.errorMessage.set(message);
    this.retryable.set(false);
  }
  loading = signal(true);
  // Load failures tracked separately from save/SSE errors so the retryable
  // error state replaces the form only when the initial load fails.
  loadError = signal<string | null>(null);
  saving = signal(false);
  submitted = signal(false);
  canEdit = signal(false);
  canDelete = signal(false);
  /**
   * The scenario's step list as a reactive array (#143). Replaces the `stepNodes` signal and the
   * manual `stepsSaveNeeded` flag: dirty/valid now come from the form itself. Kept a sibling of
   * `detailsForm` (not nested) so the create wizard's Details step gates on `detailsForm` alone.
   */
  readonly stepsForm = new FormArray<StepGroup>([]);

  /**
   * `stepsForm.dirty` projected as a signal, for the template's unsaved-steps note only. Reading
   * the raw getter in an `@if` tripped NG0100 in the create wizard — the value settles a tick
   * after a step is added — and a signal is glitch-free across change detection. All logic
   * (hasUnsavedChanges / canSave) still reads `stepsForm` directly.
   */
  protected readonly stepsDirty = toSignal(
    this.stepsForm.events.pipe(map(() => this.stepsForm.dirty)),
    { initialValue: false },
  );
  editingStep = signal<StepGroup | null>(null);

  /**
   * Mirrors the backend `@Size(max = ValidationLimits.ARTIFACT_NAME_MAX)` (#171). Bound with
   * `[attr.maxlength]` rather than `maxlength` on purpose: Angular's MaxLengthValidator directive
   * matches `[maxlength][formControl]`, so the plain binding would register a SECOND maxlength
   * validator on top of the one in the form definition. `attr.` sets the HTML attribute only, which
   * is all that is wanted here - the browser stops the typing, the form owns the validation.
   */
  readonly nameMaxLength = ARTIFACT_NAME_MAX_LENGTH;

  /**
   * Details step / edit form. Replaces the `name` + `scenarioType` + `text` ngModel fields and
   * the hand-rolled `trackChanges()` + `original*` comparison, which the form's own dirty state
   * now covers.
   *
   * `text` carries no maxLength: `AbstractTextEntity.getText()` is `@Lob` server-side, so there
   * is no bound to mirror and inventing one would reject content the server accepts.
   */
  readonly detailsForm = new FormGroup({
    name: new FormControl('', {
      validators: [Validators.required, Validators.maxLength(ARTIFACT_NAME_MAX_LENGTH)],
      nonNullable: true,
    }),
    scenarioType: new FormControl('Primary', {
      validators: [Validators.required],
      nonNullable: true,
    }),
    text: new FormControl('', { nonNullable: true }),
  });

  readonly nameErrors = { required: 'A scenario needs a name.' };

  /** Active wizard step key, two-way bound to `app-form-wizard`. */
  wizardStep = 'details';

  typeOptions = SCENARIO_TYPE_OPTIONS;
  showScenarioSelector = false;
  /**
   * Reactive form backing the step-detail edit dialog (#202). Replaces the `editingName` /
   * `editingType` / `editingText` ngModel scratch fields: reset from the target group on open,
   * applied back to it on Apply. Kept separate from `stepsForm` — it's a scratch buffer, so only
   * Apply (via markStepsDirty) touches the persisted list.
   */
  readonly editForm = new FormGroup({
    name: new FormControl('', {
      validators: [Validators.required, Validators.maxLength(ARTIFACT_NAME_MAX_LENGTH)],
      nonNullable: true,
    }),
    scenarioType: new FormControl('Primary', { nonNullable: true }),
    text: new FormControl<string | null>(null),
  });
  /** Set true on an Apply attempt so the dialog's required message shows on an untouched field. */
  readonly editSubmitted = signal(false);
  readonly stepNameErrors = { required: 'A step needs a name.' };

  projectName = '';
  scenarioId: number | null = null;
  private version: number | null = null;
  private readonly destroyRef = inject(DestroyRef);
  private sseBound = false;

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
    private eventStreamService: EventStreamService,
    private announcer: AnnouncerService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(async params => {
      this.projectName = params.get('name') ?? '';
      const idParam = params.get('scenarioId') ?? '';
      const newIsNew = idParam === 'new';

      // Reset the form synchronously for the new-scenario path BEFORE the
      // loadForProject await. Otherwise typing during the yield would later
      // be clobbered by the reset and Angular change-detection would clear
      // the input. See term-editor.ts for the same pattern.
      if (newIsNew) {
        this.isNew.set(true);
        this.scenario.set(null);
        this.scenarioId = null;
        this.version = null;
        this.wizardStep = 'details';
        this.submitted.set(false);
        this.detailsForm.reset({ name: '', scenarioType: 'Primary', text: '' });
        this.stepsForm.clear();
        this.stepsForm.markAsPristine();
        // New scenarios don't load — resolve the loading/error state so the
        // form renders immediately instead of the skeleton.
        this.loading.set(false);
        this.loadError.set(null);
      }

      await this.permissionService.loadForProject(this.projectName);
      this.canEdit.set(this.permissionService.canEdit('Scenario'));
      this.canDelete.set(this.permissionService.canDelete('Scenario'));

      if (!newIsNew) {
        this.isNew.set(false);
        this.scenarioId = +idParam;
        this.loadScenario();
      }
    });
  }

  /**
   * Dirty check across both reactive forms (#143). The step list is a sibling `FormArray`, so an
   * add / edit / remove / reorder shows up as `stepsForm.dirty` — no separate flag to keep in sync.
   */
  /** A cross-session update arrived while dirty (#140): show the reload banner. */
  updateAvailable = signal(false);

  /** Discard local edits and re-apply the latest server state (from the update banner, #140). */
  async reloadFromExternalChange(): Promise<void> {
    this.updateAvailable.set(false);
    this.detailsForm.markAsPristine();
    this.stepsForm.markAsPristine();
    await this.loadScenario(false);
    this.announcer.announce('Scenario reloaded.');
  }

  hasUnsavedChanges(): boolean {
    return this.detailsForm.dirty || this.stepsForm.dirty;
  }

  /** Edit-mode Save: blocked on invalid details OR an invalid step, unchanged, or in-flight. */
  canSave(): boolean {
    return this.detailsForm.valid && this.stepsForm.valid
      && this.hasUnsavedChanges() && !this.saving();
  }

  ngOnDestroy(): void {
    if (this.scenarioId) {
      void this.eventStreamService.removeSubscription('Scenario', this.scenarioId);
    }
  }

  /** Re-run the initial load; wired to the error state's (retry) output. */
  retryLoad(): void {
    void this.loadScenario();
  }

  /**
   * Reads the scenario and applies it in two parts: server state always, form and step state only
   * when the user has nothing in progress.
   *
   * The guard used to be `fromSSE && (...)`, so only a background refresh protected the user's
   * work. Unlike the other editors in #185 the initial load was never the problem here - this
   * editor is render-gated (`loading = signal(true)` with the form behind `@if (loading())`), so
   * there is no window to type into before the fetch returns. The unprotected callers were the
   * *other* two: the post-save refetch and the 409 recovery, both of which passed `fromSSE = false`
   * and so reset unconditionally. The 409 one is the live bug - it threw away the edit the user
   * was retrying, which is the opposite of what #184 established for goal/story/actor.
   *
   * `fromSSE` is gone; the guard no longer varies by caller. `skeleton` keeps its own default.
   *
   * The three conditions are unchanged. `saving()` matters more than it looks: the post-save
   * refetch runs while `saving()` is still true, so it now deterministically takes the merge path
   * below - which is exactly the path it wants, since the form was marked pristine and the step
   * list settled before it was issued. `editingStep() !== null` means the step-detail popup is
   * open, and replacing `stepNodes` would orphan the object it points at.
   *
   * @param skeleton show the loading skeleton. Suppressed for background refreshes, where blanking
   *                 the wizard panel the user is standing in would be worse than a stale moment.
   */
  private async loadScenario(skeleton = true): Promise<void> {
    // Only the user-initiated load drives the skeleton / retryable error state; a background
    // refresh must not blank the form the user is looking at.
    if (skeleton) {
      this.loading.set(true);
      this.loadError.set(null);
    }
    try {
      const s = await this.scenarioService.getScenario(this.projectName, this.scenarioId!);
      // Always take the version. The entity moved on, and holding the stale one guarantees a
      // 409 on the user's next save.
      this.version = s.version;
      this.scenario.set(s);
      // Checked after the fetch so it catches edits made while the request was in flight.
      if (this.hasUnsavedChanges() || this.saving() || this.editingStep() !== null) {
        this.mergeStepIds(s.steps ?? []);
        return;
      }
      this.scenarioName.set(s.name);
      this.detailsForm.reset({
        name: s.name,
        scenarioType: s.scenarioType ?? 'Primary',
        text: s.text ?? '',
      });
      this.setStepsFromServer(s.steps ?? []);
    } catch {
      // A background refresh failure shows a non-blocking message; an initial
      // load failure shows the retryable error state in place of the form.
      if (skeleton) {
        this.loadError.set('Failed to load scenario.');
      } else {
        this.showError('Failed to load scenario.');
      }
    } finally {
      if (skeleton) {
        this.loading.set(false);
      }
    }
    if (this.scenarioId && !this.sseBound) {
      void this.eventStreamService.addSubscription('Scenario', this.scenarioId);
      this.sseBound = true;
      this.eventStreamService.events$
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(envelope => {
        if (envelope.targetType !== 'Scenario' || envelope.targetId !== this.scenarioId) return;
        if (envelope.eventType === 'TargetDeleted') {
          this.announcer.announce('This scenario was deleted in another session.');
          return;
        }
        const dirty = this.hasUnsavedChanges();
        void this.loadScenario(false);
        if (dirty) {
          this.updateAvailable.set(true);
          this.announcer.announce('This scenario was changed elsewhere. Your unsaved changes are preserved.');
        } else {
          this.announcer.announceThrottled('Scenario:' + this.scenarioId, 'This scenario was updated.');
        }
      });
    }
  }

  /**
   * Copy server-assigned ids onto the nodes the user is already holding, instead of replacing the
   * array wholesale.
   *
   * `stepsForm` is not display state like `actor-editor`'s tables - it is editable state submitted
   * with `EditScenario`, so it can go neither inside the guard nor outside it. The post-save
   * refetch exists precisely so newly created steps pick up their ids; if the user types during it
   * the guard skips `setStepsFromServer(...)` and those groups keep `stepId: null` / `isNew: true`, and
   * the next `EditScenario` sends them as new again - duplicating them server-side.
   *
   * Only id-less nodes are filled in, and matching is by position, which is how `EditScenario`
   * submits the list: sent and rebuilt wholesale, in order. Known gap: if the user reorders steps
   * between the save and this refetch, an id-less node can sit at a position now held by a
   * different server step and take the wrong id. Pinned by a test rather than fixed - the ids are
   * only unknown for the brief window just after a create, and an identity match would need a key
   * the client does not have for a step the server has never seen.
   */
  private mergeStepIds(steps: StepDto[]): void {
    this.stepsForm.controls.forEach((group, i) => {
      const step = steps[i];
      if (!step || group.controls.stepId.value != null) {
        return;
      }
      // Reconciliation, not a user edit: patch quietly and do NOT touch pristine/dirty, so a
      // mid-refetch edit is neither lost nor made to look already-saved.
      group.patchValue({ stepId: step.id, isNew: false }, { emitEvent: false });
    });
  }

  /** Rebuild the step array from server state and settle it to pristine. */
  private setStepsFromServer(steps: StepDto[]): void {
    this.stepsForm.clear();
    for (const step of steps) {
      this.stepsForm.push(this.makeStepGroup({
        stepId: step.id,
        name: step.name,
        text: step.text ?? null,
        scenarioType: step.scenarioType ?? this.detailsForm.controls.scenarioType.value,
        isScenario: step.isScenario,
        isNew: false,
      }));
    }
    this.stepsForm.markAsPristine();
  }

  onStepNameChange(): void {
    this.markStepsDirty();
  }

  excludeScenarioIds(): number[] {
    const ids: number[] = [];
    if (this.scenarioId) ids.push(this.scenarioId);
    for (const step of this.stepsForm.getRawValue()) {
      if (step.isScenario && step.stepId != null) ids.push(step.stepId);
    }
    return ids;
  }

  /**
   * Build a step group. Sub-scenario reference rows render as a link, not an input, and their name
   * is server-provided — disable the `name` control so a reference can never make `stepsForm`
   * invalid; `getRawValue()` still emits it, so the submit payload is unchanged.
   */
  private makeStepGroup(data: {
    stepId: number | null; name: string; text: string | null;
    scenarioType: string; isScenario: boolean; isNew: boolean;
  }): StepGroup {
    const name = new FormControl(data.name, {
      validators: [Validators.required, Validators.maxLength(ARTIFACT_NAME_MAX_LENGTH)],
      nonNullable: true,
    });
    if (data.isScenario) name.disable();
    return new FormGroup({
      stepId: new FormControl<number | null>(data.stepId),
      name,
      text: new FormControl<string | null>(data.text),
      scenarioType: new FormControl(data.scenarioType, { nonNullable: true }),
      isScenario: new FormControl(data.isScenario, { nonNullable: true }),
      isNew: new FormControl(data.isNew, { nonNullable: true }),
    });
  }

  private newStepGroup(): StepGroup {
    return this.makeStepGroup({
      stepId: null, name: '', text: null,
      scenarioType: this.detailsForm.controls.scenarioType.value,
      isScenario: false, isNew: true,
    });
  }

  /**
   * Structural changes to a `FormArray` (push / insert / removeAt / reorder) do not mark it dirty
   * on their own, so add / remove / reorder call this. Inline field edits dirty their control.
   */
  private markStepsDirty(): void {
    this.stepsForm.markAsDirty();
  }

  addStep(): void {
    this.stepsForm.push(this.newStepGroup());
    this.markStepsDirty();
  }

  addStepAt(index: number): void {
    this.stepsForm.insert(index, this.newStepGroup());
    this.markStepsDirty();
  }

  addStepBelow(group: StepGroup): void {
    const idx = this.stepsForm.controls.indexOf(group);
    this.stepsForm.insert(idx + 1, this.newStepGroup());
    this.markStepsDirty();
  }

  removeStep(group: StepGroup): void {
    const idx = this.stepsForm.controls.indexOf(group);
    if (idx >= 0) this.stepsForm.removeAt(idx);
    this.markStepsDirty();
  }

  onDrop(event: CdkDragDrop<StepGroup[]>): void {
    const group = this.stepsForm.at(event.previousIndex);
    this.stepsForm.removeAt(event.previousIndex);
    this.stepsForm.insert(event.currentIndex, group);
    this.markStepsDirty();
  }

  openStepEdit(group: StepGroup): void {
    this.editSubmitted.set(false);
    this.editForm.reset({
      name: group.controls.name.value,
      scenarioType: group.controls.scenarioType.value,
      text: group.controls.text.value,
    });
    this.editingStep.set(group);
  }

  applyStepEdit(): void {
    const group = this.editingStep();
    if (!group) return;
    // Validate on click: a blank / too-long name keeps the dialog open with the message shown,
    // rather than silently applying (mirrors onSave marking the details form touched).
    this.editSubmitted.set(true);
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }
    const { name, scenarioType, text } = this.editForm.getRawValue();
    group.patchValue({ name, scenarioType, text: text || null });
    this.markStepsDirty();
    this.editingStep.set(null);
  }

  closeStepEdit(): void {
    this.editingStep.set(null);
  }

  /**
   * Bridges PrimeNG's user-initiated closes (Escape, mask click, close icon) back to our
   * single source of truth. PrimeNG emits visibleChange(false) on those paths; clearing
   * editingStep drives [visible] false and lets the dialog restore focus to the opener.
   * (Apply/Cancel already clear editingStep directly.)
   */
  onStepDialogVisibleChange(visible: boolean): void {
    if (!visible && this.editingStep() !== null) {
      this.closeStepEdit();
    }
  }

  onSubScenarioSelected(ref: ScenarioRef): void {
    this.showScenarioSelector = false;
    this.stepsForm.push(this.makeStepGroup({
      stepId: ref.id, name: ref.name, text: null,
      scenarioType: ref.scenarioType ?? this.detailsForm.controls.scenarioType.value,
      isScenario: true, isNew: false,
    }));
    this.markStepsDirty();
  }

  private buildStepInputs(): EditStepInput[] {
    return this.stepsForm.getRawValue().map(step => ({
      stepId: step.stepId,
      name: step.name,
      text: step.text || null,
      scenarioTypeName: step.scenarioType,
      isScenario: step.isScenario
    }));
  }

  /**
   * Runs the commit for the wizard's current step.
   *
   * Both steps issue `EditScenario`, because steps are part of the scenario's own save rather
   * than separate association commands. Step 1 creates and yields the id/version; step 2
   * re-sends the same details plus the step list against the refreshed version.
   */
  async onStepCommit(request: WizardCommitRequest): Promise<void> {
    this.submitted.set(true);
    if (this.detailsForm.invalid || this.stepsForm.invalid) {
      this.detailsForm.markAllAsTouched();
      this.stepsForm.markAllAsTouched();
      request.fail('Please fix the highlighted fields.');
      return;
    }
    const result = await this.saveDetails();

    if (result.success) {
      request.complete();
      return;
    }
    if (await this.recoverFromStaleVersion(result)) {
      request.fail(STALE_VERSION_MESSAGE);
      return;
    }
    request.fail(result.error ?? 'Save failed.');
  }

  /** Done on the last step: the scenario is already saved, so just go to it. */
  onWizardFinished(): void {
    if (this.scenarioId != null) {
      this.router.navigate(['/projects', this.projectName, 'scenarios', this.scenarioId]);
    } else {
      this.onBack();
    }
  }

  /**
   * Issues `EditScenario` for the Details values plus the current step list and, on success,
   * adopts the id and version from the response.
   *
   * The version is **spent on use**: `EditScenarioCommandImpl` calls `checkExpectedVersion` and
   * then merges the scenario twice, so every accepted save bumps it. It is re-read from
   * `result.entity` each time - holding the value captured at create and sending it again,
   * which is exactly what happens when the user steps back to Details and continues a second
   * time, is a guaranteed 409.
   *
   * Name/type/text come from the form on every call, never from a snapshot: step 2 re-sends
   * them, so a back-navigation edit has to be picked up here or it is silently dropped.
   */
  private async saveDetails(): Promise<CommandResult<unknown>> {
    this.saving.set(true);
    this.showError(null);
    clearServerErrors(this.detailsForm);
    try {
      const { name, scenarioType, text } = this.detailsForm.getRawValue();
      const input: Record<string, unknown> = {
        projectName: this.projectName,
        name,
        text: text || null,
        scenarioTypeName: scenarioType,
        steps: this.buildStepInputs(),
      };
      if (this.scenarioId != null) input['scenarioId'] = this.scenarioId;
      if (this.version != null) input['version'] = this.version;

      const result = await this.commandService.execute('EditScenario', input);
      if (!result.success) {
        const unresolved = applyCommandErrors(this.detailsForm, result.violations);
        if (unresolved.length) {
          this.showError(unresolved.join(SEPARATOR));
        }
        return result;
      }

      const wasCreate = this.scenarioId == null;
      if (wasCreate) {
        this.projectService.notifyTreeChanged();
      }

      const saved = result.entity as ScenarioDto | null;
      if (saved) {
        this.scenarioId = saved.id;
        this.version = saved.version;
        this.scenarioName.set(saved.name);
      }
      this.detailsForm.markAsPristine();
      this.stepsForm.markAsPristine();
      this.messageService.add({ severity: 'success', summary: 'Saved', detail: 'Scenario saved.' });

      // Refetch so new steps pick up their server-assigned ids, and so the SSE subscription
      // starts the first time the scenario exists. No skeleton: in the wizard that would
      // blank the panel the user is standing in.
      if (this.scenarioId != null) {
        await this.loadScenario(false);
      }
      return result;
    } catch {
      return {
        success: false,
        entityType: 'Scenario',
        entity: null,
        error: 'An unexpected error occurred.',
        violations: null,
      };
    } finally {
      this.saving.set(false);
    }
  }

  /**
   * If `result` is an optimistic-lock conflict (HTTP 409 from
   * `EntityLockException.staleEntity`), refetch so the held version is current and the user
   * can retry. Returns whether it handled the result.
   */
  private async recoverFromStaleVersion(result: CommandResult<unknown>): Promise<boolean> {
    if (result.status !== 409 || this.scenarioId == null) {
      return false;
    }
    await this.loadScenario(false);
    return true;
  }

  /** Edit-mode Save. */
  async onSave(): Promise<void> {
    this.submitted.set(true);
    if (this.detailsForm.invalid || this.stepsForm.invalid) {
      this.detailsForm.markAllAsTouched();
      this.stepsForm.markAllAsTouched();
      return;
    }

    const result = await this.saveDetails();
    if (result.success) {
      return;
    }
    if (await this.recoverFromStaleVersion(result)) {
      this.showError(STALE_VERSION_MESSAGE);
      return;
    }
    this.showError(result.error ?? 'Save failed.');
    this.retryable.set(isNetworkError(result));
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
          this.showError(result.error ?? 'Copy failed.');
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
          // Nothing left to guard against - don't let the dirty check block the exit.
          this.detailsForm.markAsPristine();
          this.stepsForm.markAsPristine();
          this.router.navigate(['/projects', this.projectName, 'scenarios']);
        } else {
          this.showError(result.error ?? 'Delete failed.');
        }
      }
    });
  }

  onBack(): void {
    this.location.back();
  }
}
