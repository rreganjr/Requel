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
import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, OnInit, signal, effect } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { SelectModule } from 'primeng/select';
import { MessageService } from 'primeng/api';
import { PageHeaderComponent } from '../../shared/page-header';
import { AppCardComponent } from '../../shared/app-card';
import { DirtyCheckable } from '../../core/dirty-check.guard';
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import { CommandService, isNetworkError } from '../../core/command.service';
import { ScenarioService } from '../../core/scenario.service';
import { PermissionService } from '../../core/permission.service';
import { EventStreamService } from '../../core/event-stream.service';
import { AnnouncerService } from '../../core/announcer.service';
import { AnnotationsSectionComponent } from '../../shared/annotations-section';
import { SourcesSectionComponent } from '../../shared/sources-section';
import { AppFieldComponent, AppFieldControlDirective } from '../../shared/app-field';
import { LoadingStateComponent } from '../../shared/loading-state';
import { ErrorStateComponent } from '../../shared/error-state';
import { applyCommandErrors, clearServerErrors } from '../../shared/form-errors';
import { ARTIFACT_NAME_MAX_LENGTH } from '../../shared/validation-limits';
import { StepDetailDto } from '../../models/scenario';
import { ArtifactNameService } from '../../core/artifact-name.service';

const STEP_TYPE_OPTIONS = [
  { label: 'Primary', value: 'Primary' },
  { label: 'PreCondition', value: 'PreCondition' },
  { label: 'Optional', value: 'Optional' },
  { label: 'Alternative', value: 'Alternative' },
  { label: 'Exception', value: 'Exception' },
];

/**
 * Issue #476: a plain step on its own page. The scenario editor's step popup has no annotations,
 * so a step's issues (#349, #459) had nowhere to be seen or resolved. This page edits the step's
 * name, type and notes with EditScenarioStep (saved on its own, not with a scenario), lists the
 * scenarios that use it, and shows its annotations and sources.
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-step-editor',
  standalone: true,
  imports: [
    PageHeaderComponent, AppCardComponent, ReactiveFormsModule, ButtonModule, InputText,
    TextareaModule, SelectModule, SubmitErrorComponent, AnnotationsSectionComponent,
    SourcesSectionComponent, AppFieldComponent, AppFieldControlDirective, LoadingStateComponent,
    ErrorStateComponent, RouterLink,
  ],
  template: `
    <div class="step-editor" data-testid="step-editor">
      <div class="page-header">
        <app-page-header eyebrow="Step" [title]="stepName()" />
        <div class="page-actions">
          <p-button label="Back" icon="pi pi-arrow-left" severity="secondary" data-testid="step-back"
                    [outlined]="true" (onClick)="onBack()" />
        </div>
      </div>

      <app-submit-error [message]="errorMessage()" [retryable]="retryable()" (retry)="onSave()"
                        testid="step-error" />

      @if (loading()) {
        <app-card>
          <app-loading-state label="Loading step…" [lines]="3" testid="step-editor-loading" />
        </app-card>
      } @else if (loadError()) {
        <app-error-state [message]="loadError()!" testid="step-editor-load-error" (retry)="retryLoad()" />
      } @else {
        <app-card>
          @if (usedIn().length > 1) {
            <p class="shared-hint" data-testid="step-shared-hint">
              This step is used by {{ usedIn().length }} scenarios. A change here changes it in all of them.
            </p>
          }
          <form [formGroup]="form" (ngSubmit)="onSave()">
            <app-field label="Name" controlId="stepName" [control]="form.controls.name" [submitted]="submitted()">
              <input id="stepName" pInputText appFieldControl formControlName="name"
                     data-testid="step-name" [attr.maxlength]="nameMaxLength" [readonly]="!canEdit()"
                     placeholder="Step description..." />
            </app-field>
            <app-field label="Type" controlId="stepType" [control]="form.controls.scenarioType" [submitted]="submitted()">
              <p-select appFieldControl inputId="stepType" data-testid="step-type"
                        formControlName="scenarioType" [options]="typeOptions"
                        optionLabel="label" optionValue="value" />
            </app-field>
            <app-field label="Notes" controlId="stepText" [control]="form.controls.text" [submitted]="submitted()"
                       [divider]="false">
              <textarea id="stepText" pTextarea appFieldControl formControlName="text" rows="4"
                        [autoResize]="true" data-testid="step-text" [readonly]="!canEdit()"
                        placeholder="Additional details or notes..."></textarea>
            </app-field>
            @if (canEdit()) {
              <div class="form-actions">
                <p-button type="submit" label="Save" icon="pi pi-check" data-testid="step-save"
                          [loading]="saving()" [disabled]="form.invalid || form.pristine || saving()" />
              </div>
            }
          </form>
        </app-card>

        <app-card title="Used in">
          <ul class="used-in" data-testid="step-used-in">
            @for (s of usedIn(); track s.id) {
              <li data-testid="step-used-in-row">
                <a class="entity-link" data-testid="step-used-in-link"
                   [routerLink]="['/projects', projectName, 'scenarios', s.id]">{{ s.name }}</a>
              </li>
            } @empty {
              <li class="empty">Not used by any scenario.</li>
            }
          </ul>
        </app-card>

        @if (stepId() != null) {
          <app-annotations-section [projectName]="projectName" entityType="Step" [entityId]="stepId()"
                                   (resolved)="onIssueResolved()" />
          <app-sources-section [projectName]="projectName" entityType="Step" [entityId]="stepId()" />
        }
      }
    </div>
  `,
  styles: [`
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--rq-space-4); }
    .page-actions { display: flex; gap: var(--rq-space-2); }
    app-field input, app-field textarea { width: 100%; }
    .shared-hint { font-size: var(--rq-font-size-sm); color: var(--p-text-secondary-color); margin: 0 0 var(--rq-space-3); }
    .form-actions { margin-block: var(--rq-space-4) var(--rq-space-2); }
    .used-in { margin: 0; padding-left: var(--rq-space-5); }
    .used-in .empty { list-style: none; color: var(--p-text-secondary-color); }
  `],
})
export class StepEditorComponent implements OnInit, DirtyCheckable {
  readonly typeOptions = STEP_TYPE_OPTIONS;
  readonly nameMaxLength = ARTIFACT_NAME_MAX_LENGTH;

  step = signal<StepDetailDto | null>(null);
  stepId = signal<number | null>(null);
  stepName = signal('Step');
  /** #486: the breadcrumb and tab title follow this entity's current name (a rename, a rewrite). */
  private readonly artifactName = inject(ArtifactNameService);
  private readonly leafNameSync = effect(() => {
    const name = this.stepName();
    if (name) {
      this.artifactName.set(name);
    }
  });
  usedIn = computed(() => this.step()?.usedIn ?? []);
  loading = signal(true);
  loadError = signal<string | null>(null);
  saving = signal(false);
  submitted = signal(false);
  errorMessage = signal<string | null>(null);
  retryable = signal(false);
  // Steps are edited under the Scenario permission (EditScenarioStep requires Scenario[Edit]).
  canEdit = computed(() => this.permissionService.canEdit('Scenario'));

  readonly form = new FormGroup({
    name: new FormControl('', {
      validators: [Validators.required, Validators.maxLength(ARTIFACT_NAME_MAX_LENGTH)],
      nonNullable: true,
    }),
    scenarioType: new FormControl('Primary', { nonNullable: true }),
    text: new FormControl('', { nonNullable: true }),
  });

  projectName = '';
  private readonly destroyRef = inject(DestroyRef);
  private sseBound = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private scenarioService: ScenarioService,
    private commandService: CommandService,
    private permissionService: PermissionService,
    private messageService: MessageService,
    private eventStreamService: EventStreamService,
    private announcer: AnnouncerService,
  ) {}

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(async params => {
      this.projectName = params.get('name') ?? '';
      const id = Number(params.get('stepId'));
      await this.permissionService.loadForProject(this.projectName);
      await this.loadStep(id);
      this.bindEvents();
    });
  }

  hasUnsavedChanges(): boolean {
    return this.form.dirty;
  }

  retryLoad(): void {
    const id = this.stepId();
    if (id != null) {
      void this.loadStep(id);
    }
  }

  /** Adopts the server state into the form only when nothing is unsaved, as the other editors do. */
  private async loadStep(id: number, skeleton = true): Promise<void> {
    this.stepId.set(id);
    if (skeleton) {
      this.loading.set(true);
      this.loadError.set(null);
    }
    try {
      const step = await this.scenarioService.getStep(this.projectName, id);
      this.step.set(step);
      if (!this.hasUnsavedChanges()) {
        this.stepName.set(step.name);
        this.form.reset({
          name: step.name,
          scenarioType: step.scenarioType ?? 'Primary',
          text: step.text ?? '',
        });
        if (!this.canEdit()) {
          this.form.controls.scenarioType.disable({ emitEvent: false });
        }
        this.submitted.set(false);
      }
    } catch {
      if (skeleton) {
        this.loadError.set('Failed to load step.');
      } else {
        this.errorMessage.set('Failed to load step.');
      }
    } finally {
      if (skeleton) {
        this.loading.set(false);
      }
    }
  }

  private bindEvents(): void {
    const id = this.stepId();
    if (id == null || this.sseBound) {
      return;
    }
    this.sseBound = true;
    void this.eventStreamService.addSubscription('Step', id);
    this.eventStreamService.events$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(envelope => {
      if (envelope.targetType !== 'Step' || envelope.targetId !== this.stepId()) return;
      if (envelope.eventType === 'TargetDeleted') {
        this.announcer.announce('This step was deleted in another session.');
        return;
      }
      void this.loadStep(this.stepId()!, false);
    });
  }


  /**
   * #486: an issue was resolved below; a rewrite renames this step. Reload it, keeping unsaved
   * edits (the change events skip the session that made the change).
   */
  onIssueResolved(): void {
    const id = this.stepId();
    if (id == null) return;
    if (this.hasUnsavedChanges()) {
      this.announcer.announce('Resolving the issue changed this step. Your unsaved changes are preserved.');
    }
    void this.loadStep(id, false);
  }
  async onSave(): Promise<void> {
    this.submitted.set(true);
    clearServerErrors(this.form);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.errorMessage.set(null);
    this.retryable.set(false);
    const { name, scenarioType, text } = this.form.getRawValue();
    const result = await this.commandService.execute<StepDetailDto>('EditScenarioStep', {
      projectName: this.projectName,
      stepId: this.stepId(),
      version: this.step()?.version,
      name: name.trim(),
      scenarioTypeName: scenarioType,
      // '' clears; null would leave the notes as they are (#316).
      text,
    });
    this.saving.set(false);
    if (result.success) {
      this.messageService.add({ severity: 'success', summary: 'Step saved', life: 3000 });
      this.form.markAsPristine();
      await this.loadStep(this.stepId()!, false);
      return;
    }
    const unresolved = applyCommandErrors(this.form, result.violations);
    this.retryable.set(isNetworkError(result));
    if (unresolved.length) {
      this.errorMessage.set(unresolved.join('; '));
    } else if (!result.violations?.length) {
      this.errorMessage.set(result.error ?? 'Save failed.');
    }
  }

  /** Back to the scenario the user most likely came from: the only one using it, else history. */
  onBack(): void {
    const used = this.usedIn();
    if (used.length === 1) {
      this.router.navigate(['/projects', this.projectName, 'scenarios', used[0].id]);
    } else {
      history.back();
    }
  }
}
