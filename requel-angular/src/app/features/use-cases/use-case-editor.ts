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
import { Component, computed, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import { EditorActionsComponent } from '../../shared/editor-actions';
import { PageHeaderComponent } from '../../shared/page-header';
import { AppCardComponent } from '../../shared/app-card';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Location, NgTemplateOutlet } from '@angular/common';
import { Subscription } from 'rxjs';
import { DirtyCheckable } from '../../core/dirty-check.guard';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { SelectModule } from 'primeng/select';
import { ConfirmationService, MessageService } from 'primeng/api';
import { UseCaseDto } from '../../models/use-case';
import { GoalDto } from '../../models/goal';
import { ActorDto } from '../../models/actor';
import { StoryDto } from '../../models/story';
import { ScenarioDto } from '../../models/scenario';
import { EntityReferenceDto } from '../../models/entity-reference';
import { UseCaseService } from '../../core/use-case.service';
import { ActorService } from '../../core/actor.service';
import { ScenarioService } from '../../core/scenario.service';
import { CommandService, isNetworkError } from '../../core/command.service';
import { ProjectService } from '../../core/project.service';
import { PermissionService } from '../../core/permission.service';
import { EventStreamService } from '../../core/event-stream.service';
import { EntitySelectorDialogComponent } from '../../shared/entity-selector-dialog';
import { RelationshipSectionComponent } from '../../shared/app-relationship-section';
import { AnnotationsSectionComponent } from '../../shared/annotations-section';
import { AppFieldComponent, AppFieldControlDirective } from '../../shared/app-field';
import { LoadingStateComponent } from '../../shared/loading-state';
import { ErrorStateComponent } from '../../shared/error-state';
import {
  AppFormWizardComponent,
  AppWizardStepComponent,
  WizardCommitRequest,
} from '../../shared/app-form-wizard';
import { applyCommandErrors, clearServerErrors } from '../../shared/form-errors';
import { ARTIFACT_NAME_MAX_LENGTH } from '../../shared/validation-limits';
import { CommandResult } from '../../models/command';


/** Joins page-level violations that resolved to no control. */
const SEPARATOR = '; ';

/** Wording for the stale-version recovery path, so the 409 case reads as recoverable. */
const STALE_VERSION_MESSAGE =
  'This use case was changed elsewhere. Your copy has been refreshed - review the values and continue.';

@Component({
  selector: 'app-use-case-editor',
  standalone: true,
  imports: [EditorActionsComponent, PageHeaderComponent, AppCardComponent, RouterLink, NgTemplateOutlet, ReactiveFormsModule,
            ButtonModule, InputText, TextareaModule, SubmitErrorComponent,
            ConfirmDialogModule, TableModule, TooltipModule, SelectModule, RelationshipSectionComponent,
            EntitySelectorDialogComponent, AnnotationsSectionComponent,
            AppFieldComponent, AppFieldControlDirective,
            AppFormWizardComponent, AppWizardStepComponent,
            LoadingStateComponent, ErrorStateComponent],
  providers: [ConfirmationService],
  template: `
    <div class="use-case-editor" data-testid="use-case-editor">
      <div class="page-header">
        <app-page-header [title]="isNew() ? 'New Use Case' : useCaseName()" />
        <div class="page-actions">
          <app-editor-actions [projectName]="projectName" />
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

      <app-submit-error [message]="errorMessage()" testid="use-case-error" [retryable]="retryable()" (retry)="onSave()" />

      @if (loading()) {
        <app-card>
          <app-loading-state label="Loading use case…" [lines]="4" testid="use-case-editor-loading" />
        </app-card>
      } @else if (loadError()) {
        <app-error-state [message]="loadError()!" testid="use-case-editor-load-error"
                         (retry)="retryLoad()" />
      } @else if (isNew()) {
        <!--
          Create runs as a wizard (#173). Four steps because this editor gates five separate
          sections; grouping Goals with Stories keeps the count at four without putting six
          tables on one panel. Step 1 commits EditUseCase, which is what gives every later step
          the persisted useCaseId its selector needs.
        -->
        <app-form-wizard
          [(activeKey)]="wizardStep"
          navLabel="New use case steps"
          (stepCommit)="onStepCommit($event)"
          (cancelled)="onBack()"
          (finished)="onWizardFinished()"
          data-testid="use-case-wizard"
        >
          <app-wizard-step key="details" label="Details" helper="Name, actor and description"
                           [form]="detailsForm">
            <ng-template>
              <ng-container [ngTemplateOutlet]="detailsFields" />
            </ng-template>
          </app-wizard-step>

          <app-wizard-step key="scenarios" label="Scenarios" helper="Primary and additional"
                           [optional]="true">
            <ng-template>
              <ng-container [ngTemplateOutlet]="scenariosSection"
                            [ngTemplateOutletContext]="{ heading: false }" />
            </ng-template>
          </app-wizard-step>

          <app-wizard-step key="goals-stories" label="Goals & Stories" helper="What it satisfies"
                           [optional]="true">
            <ng-template>
              <ng-container [ngTemplateOutlet]="goalsSection"
                            [ngTemplateOutletContext]="{ heading: true }" />
              <ng-container [ngTemplateOutlet]="storiesSection"
                            [ngTemplateOutletContext]="{ heading: true }" />
            </ng-template>
          </app-wizard-step>

          <app-wizard-step key="actors" label="Actors" helper="Additional actors"
                           [optional]="true">
            <ng-template>
              <ng-container [ngTemplateOutlet]="actorsSection"
                            [ngTemplateOutletContext]="{ heading: false }" />
            </ng-template>
          </app-wizard-step>
        </app-form-wizard>
      } @else {
        <app-card>
          <ng-container [ngTemplateOutlet]="detailsFields" />

          <div class="form-actions">
            <p-button label="Save" icon="pi pi-check" data-testid="use-case-save"
                      [disabled]="!canSave()" [loading]="saving()" (onClick)="onSave()" />
          </div>
        </app-card>

        <ng-container [ngTemplateOutlet]="scenariosSection"
                      [ngTemplateOutletContext]="{ heading: true }" />
        <ng-container [ngTemplateOutlet]="goalsSection"
                      [ngTemplateOutletContext]="{ heading: true }" />
        <ng-container [ngTemplateOutlet]="storiesSection"
                      [ngTemplateOutletContext]="{ heading: true }" />
        <ng-container [ngTemplateOutlet]="actorsSection"
                      [ngTemplateOutletContext]="{ heading: true }" />
      }

      <!-- Primary scenario selector (shows only Primary-type scenarios) -->
      <app-entity-selector-dialog
        [visible]="showPrimaryScenarioSelector"
        [projectName]="projectName"
        entityType="Scenario"
        [excludeIds]="primaryScenarioExcludeIds()"
        [includeTypes]="['Primary']"
        (selected)="selectPrimaryScenario($event)"
        (closed)="showPrimaryScenarioSelector = false" />

      <!-- Additional scenario selector (excludes Primary-type and already-added scenarios) -->
      <app-entity-selector-dialog
        [visible]="showScenarioSelector"
        [projectName]="projectName"
        entityType="Scenario"
        [excludeIds]="additionalScenarioIds()"
        [excludeTypes]="['Primary']"
        (selected)="addScenario($event)"
        (closed)="showScenarioSelector = false" />

      <!-- Entity selector dialogs -->
      <app-entity-selector-dialog
        [visible]="showGoalSelector"
        [projectName]="projectName"
        entityType="Goal"
        [excludeIds]="goalIds()"
        (selected)="addGoal($event)"
        (closed)="showGoalSelector = false" />

      <app-entity-selector-dialog
        [visible]="showStorySelector"
        [projectName]="projectName"
        entityType="Story"
        [excludeIds]="storyIds()"
        (selected)="addStory($event)"
        (closed)="showStorySelector = false" />

      <app-entity-selector-dialog
        [visible]="showActorSelector"
        [projectName]="projectName"
        entityType="Actor"
        [excludeIds]="actorIds()"
        (selected)="addActorToList($event)"
        (closed)="showActorSelector = false" />

      <app-annotations-section
        [projectName]="projectName"
        entityType="UseCase"
        [entityId]="useCaseId"
        [canEdit]="canEdit()" />

      <p-confirmDialog />

      <!--
        Shared bodies, used by both the wizard steps and the edit view so the two cannot drift.
        Controls bind [formControl], not formControlName: these are projected into the wizard,
        where formControlName would look for a parent formGroup that is not there.

        Every association section guards on useCaseId: during create, step 1 has not committed
        yet on first render, and the selectors all key off the persisted use case.
      -->
      <ng-template #detailsFields>
        <app-field label="Name" controlId="name" [control]="detailsForm.controls.name"
                   [errorMessages]="nameErrors" [submitted]="submitted()">
          <input appFieldControl pInputText [formControl]="detailsForm.controls.name" id="name"
                 [attr.maxlength]="nameMaxLength"
                 placeholder="Use case name" data-testid="use-case-name" />
        </app-field>

        <app-field label="Primary Actor" controlId="useCasePrimaryActorInput"
                   [control]="detailsForm.controls.primaryActorName" [submitted]="submitted()">
          <p-select appFieldControl inputId="useCasePrimaryActorInput"
                    data-testid="use-case-primary-actor"
                    [formControl]="detailsForm.controls.primaryActorName"
                    [options]="actorOptions()" optionLabel="label" optionValue="value"
                    [showClear]="true"
                    [pt]="{ clearIcon: { 'data-testid': 'use-case-primary-actor-clear' } }"
                    placeholder="Select primary actor" styleClass="w-full" />
        </app-field>

        <app-field label="Description" controlId="text" [control]="detailsForm.controls.text" [divider]="false"
                   [submitted]="submitted()">
          <textarea appFieldControl pTextarea [formControl]="detailsForm.controls.text" id="text" rows="4"
                    placeholder="Use case description" data-testid="use-case-text"></textarea>
        </app-field>
      </ng-template>

      <ng-template #scenariosSection let-heading="heading">
        <div class="section">
          <div class="section-header">
            @if (heading) {
              <h2 class="rq-section-title">Primary Scenario</h2>
            }
          </div>
          @if (useCaseId == null) {
            <p class="no-scenario-hint">Save the use case's details first to add scenarios.</p>
          } @else if (!useCase()?.scenarioId) {
            <p class="no-scenario-hint">No primary scenario yet.</p>
            @if (canEdit()) {
              <div class="primary-scenario-actions">
                <p-button label="Create New" icon="pi pi-plus" size="small"
                          data-testid="use-case-create-primary-scenario"
                          pTooltip="Create a primary scenario using the use case name"
                          (onClick)="createPrimaryScenario()" [loading]="saving()" />
                <p-button label="Select Existing" icon="pi pi-search" size="small"
                          severity="secondary" [outlined]="true"
                          pTooltip="Pick an existing Primary-type scenario"
                          (onClick)="showPrimaryScenarioSelector = true" />
              </div>
            }
          } @else {
            <div class="primary-scenario-card" data-testid="use-case-primary-scenario-card">
              <div class="primary-scenario-name" data-testid="use-case-primary-scenario-name">
                <a class="entity-link" data-testid="use-case-primary-scenario-link"
                   [routerLink]="['/projects', projectName, 'scenarios', useCase()!.scenarioId!]">
                  {{ useCase()!.scenarioName ?? 'Primary Scenario' }}
                </a>
                <span class="step-count">({{ useCase()!.scenarioStepCount ?? 0 }} steps)</span>
                <p-button label="Open in Editor" icon="pi pi-arrow-right" size="small"
                          data-testid="use-case-open-primary-scenario"
                          severity="secondary" [outlined]="true"
                          (onClick)="navigateTo('scenarios', useCase()!.scenarioId!)" />
              </div>
              @if (primaryScenario()?.steps?.length) {
                <p-table [value]="primaryScenario()!.steps!" styleClass="p-datatable-sm" [rowHover]="false">
                  <ng-template pTemplate="header">
                    <tr><th class="col-num">#</th><th>Step</th><th class="col-kind">Type</th></tr>
                  </ng-template>
                  <ng-template pTemplate="body" let-step let-i="rowIndex">
                    <tr>
                      <td>{{ i + 1 }}</td>
                      <td>{{ step.name }}</td>
                      <td>{{ step.scenarioType }}</td>
                    </tr>
                  </ng-template>
                </p-table>
              } @else {
                <p class="no-scenario-hint">No steps yet — open in editor to add them.</p>
              }
            </div>
          }
        </div>

        @if (useCaseId != null) {
          <app-relationship-section #ucScenariosSection
            title="Additional Scenarios" [showHeading]="true" [headingLevel]="3"
            [items]="additionalScenarios()" [headers]="['Name', 'Type']"
            [canAdd]="canEdit()"
            addLabel="Add Scenario" addTestid="use-case-add-scenario"
            removeTestid="use-case-remove-scenario" rowTestid="use-case-scenario-row" testid="use-case-scenarios"
            emptyText="No additional scenarios."
            [removeAriaLabel]="scenarioRemoveAria" [trackBy]="refTrackBy"
            (add)="showScenarioSelector = true" (remove)="removeScenario($event)">
            <ng-template #row let-s>
              <td><a class="entity-link" data-testid="use-case-scenario-link"
                     [routerLink]="['/projects', projectName, 'scenarios', s.id]">{{ s.name }}</a></td>
              <td>{{ s.scenarioType }}</td>
            </ng-template>
          </app-relationship-section>
        }
      </ng-template>

      <ng-template #goalsSection let-heading="heading">
        <app-relationship-section #ucGoalsSection
          title="Goals" [showHeading]="heading" [headingLevel]="3"
          [items]="goals()" [headers]="['Name']"
          [canAdd]="canEdit() && useCaseId != null"
          addLabel="Add Goal" addTestid="use-case-add-goal"
          removeTestid="use-case-remove-goal" rowTestid="use-case-goal-row" testid="use-case-goals"
          emptyText="No goals."
          unsavedHint="Save the use case's details first to add goals."
          [removeAriaLabel]="ucGoalRemoveAria" [trackBy]="refTrackBy"
          (add)="showGoalSelector = true" (remove)="removeGoal($event)">
          <ng-template #row let-goal>
            <td>
              <a class="entity-link" data-testid="use-case-goal-link"
                 [routerLink]="['/projects', projectName, 'goals', goal.id]">{{ goal.name }}</a>
            </td>
          </ng-template>
        </app-relationship-section>
      </ng-template>

      <ng-template #storiesSection let-heading="heading">
        <app-relationship-section #ucStoriesSection
          title="Stories" [showHeading]="heading" [headingLevel]="3"
          [items]="stories()" [headers]="['Name', 'Type']"
          [canAdd]="canEdit() && useCaseId != null"
          addLabel="Add Story" addTestid="use-case-add-story"
          removeTestid="use-case-remove-story" rowTestid="use-case-story-row" testid="use-case-stories"
          emptyText="No stories."
          unsavedHint="Save the use case's details first to add stories."
          [removeAriaLabel]="ucStoryRemoveAria" [trackBy]="refTrackBy"
          (add)="showStorySelector = true" (remove)="removeStory($event)">
          <ng-template #row let-story>
            <td>
              <a class="entity-link" data-testid="use-case-story-link"
                 [routerLink]="['/projects', projectName, 'stories', story.id]">{{ story.name }}</a>
            </td>
            <td>{{ story.storyType }}</td>
          </ng-template>
        </app-relationship-section>
      </ng-template>

      <ng-template #actorsSection let-heading="heading">
        <app-relationship-section #ucActorsSection
          title="Additional Actors" [showHeading]="heading" [headingLevel]="3"
          [items]="actors()" [headers]="['Name']"
          [canAdd]="canEdit() && useCaseId != null"
          addLabel="Add Actor" addTestid="use-case-add-actor"
          removeTestid="use-case-remove-actor" rowTestid="use-case-actor-row" testid="use-case-actors"
          emptyText="No additional actors."
          unsavedHint="Save the use case's details first to add actors."
          [removeAriaLabel]="ucActorRemoveAria" [trackBy]="refTrackBy"
          (add)="showActorSelector = true" (remove)="removeActor($event)">
          <ng-template #row let-actor>
            <td>
              <a class="entity-link" data-testid="use-case-actor-link"
                 [routerLink]="['/projects', projectName, 'actors', actor.id]">{{ actor.name }}</a>
            </td>
          </ng-template>
        </app-relationship-section>
      </ng-template>
    </div>
  `,
  styles: [`
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
    .page-actions { display: flex; gap: 0.5rem; }
    .form-grid { display: grid; grid-template-columns: 130px 1fr; gap: 0.75rem 1rem; align-items: start; max-width: 700px; }
    .form-actions { margin-top: 1rem; }
    .section { margin-top: 1.5rem; }
    .section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem; }
    .section-header h3 { margin: 0; }
    .entity-link { cursor: pointer; color: var(--p-primary-color); text-decoration: underline; }
    .no-scenario-hint { color: var(--p-text-muted-color); font-style: italic; margin: 0.5rem 0; }
    .primary-scenario-actions { display: flex; gap: 0.5rem; margin-top: 0.25rem; }
    .primary-scenario-card { display: flex; flex-direction: column; gap: 0.5rem; }
    .primary-scenario-name { display: flex; align-items: center; gap: 0.75rem; flex-wrap: wrap; }
    .step-count { color: var(--p-text-muted-color); font-size: 0.875rem; }
  `]
})
export class UseCaseEditorComponent implements OnInit, OnDestroy, DirtyCheckable {
  isNew = signal(true);
  useCaseName = signal('');
  useCase = signal<UseCaseDto | null>(null);
  errorMessage = signal<string | null>(null);
  retryable = signal(false);
  /** Sets the inline submit error and, by default, marks it non-retryable. */
  private showError(message: string | null): void {
    this.errorMessage.set(message);
    this.retryable.set(false);
  }
  /**
   * #185. The edit form renders only once the detail GET resolves, so there is no window in which
   * a user can type into a form the load is about to reset. Starts true: an edit route is loading
   * from the first frame, and the create path clears it as soon as it knows there is nothing to
   * load.
   */
  loading = signal(true);
  loadError = signal<string | null>(null);
  saving = signal(false);
  canEdit = signal(false);
  canDelete = signal(false);
  goals = signal<GoalDto[]>([]);
  stories = signal<StoryDto[]>([]);
  actors = signal<ActorDto[]>([]);
  additionalScenarios = signal<ScenarioDto[]>([]);
  primaryScenario = signal<ScenarioDto | null>(null);
  actorOptions = signal<{label: string, value: string}[]>([]);

  // Derived id sets used as excludeIds for the entity selector dialogs
  goalIds = computed(() => this.goals().map(g => g.id).filter((id): id is number => id != null));
  storyIds = computed(() => this.stories().map(s => s.id).filter((id): id is number => id != null));
  actorIds = computed(() => this.actors().map(a => a.id).filter((id): id is number => id != null));
  // Exclude already-selected primary ID from the primary selector
  primaryScenarioExcludeIds = computed(() => {
    const id = this.useCase()?.scenarioId;
    return id != null ? [id] : [];
  });
  // Exclude primary ID + additional scenario IDs from the additional selector
  additionalScenarioIds = computed(() => [
    ...this.primaryScenarioExcludeIds(),
    ...this.additionalScenarios().map(s => s.id).filter((id): id is number => id != null)
  ]);

  showGoalSelector = false;
  showStorySelector = false;
  showActorSelector = false;
  showScenarioSelector = false;
  showPrimaryScenarioSelector = false;

  submitted = signal(false);

  /** Mirrors the backend `@Size(max = ValidationLimits.ARTIFACT_NAME_MAX)` (#171). */
  readonly nameMaxLength = ARTIFACT_NAME_MAX_LENGTH;

  /**
   * Details step / edit form. Replaces the three `ngModel` fields and the `trackChanges()` +
   * `original*` comparison.
   */
  readonly detailsForm = new FormGroup({
    name: new FormControl('', {
      validators: [Validators.required, Validators.maxLength(ARTIFACT_NAME_MAX_LENGTH)],
      nonNullable: true,
    }),
    primaryActorName: new FormControl('', { nonNullable: true }),
    text: new FormControl('', { nonNullable: true }),
  });

  readonly nameErrors = { required: 'A use case needs a name.' };

  /** Active wizard step key, two-way bound to `app-form-wizard`. */
  wizardStep = 'details';

  projectName = '';
  useCaseId: number | null = null;
  private version: number | null = null;
  private paramSub?: Subscription;
  private sseSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private location: Location,
    private useCaseService: UseCaseService,
    private actorService: ActorService,
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
      this.canEdit.set(this.permissionService.canEdit('UseCase'));
      this.canDelete.set(this.permissionService.canDelete('UseCase'));

      // Load all actors for the primary actor dropdown
      const actors = await this.actorService.listActors(this.projectName);
      this.actorOptions.set(actors.map(a => ({ label: a.name, value: a.name })));

      const idParam = params.get('useCaseId') ?? '';
      if (idParam === 'new') {
        this.isNew.set(true);
        this.useCaseId = null;
        this.version = null;
        this.wizardStep = 'details';
        this.submitted.set(false);
        this.detailsForm.reset({ name: '', primaryActorName: '', text: '' });
        this.goals.set([]);
        this.stories.set([]);
        this.actors.set([]);
        this.additionalScenarios.set([]);
        this.primaryScenario.set(null);
        // Nothing to load, so resolve the gate (#185) - otherwise the create wizard sits behind
        // the skeleton forever and create becomes unreachable.
        this.loading.set(false);
        this.loadError.set(null);
      } else {
        this.isNew.set(false);
        this.useCaseId = +idParam;
        await this.loadUseCase();
      }
    });
  }

  hasUnsavedChanges(): boolean {
    return this.detailsForm.dirty;
  }

  /** Edit-mode Save: blocked on invalid, unchanged, or in-flight. */
  canSave(): boolean {
    return this.detailsForm.valid && this.detailsForm.dirty && !this.saving();
  }

  ngOnDestroy(): void {
    this.paramSub?.unsubscribe();
    if (this.useCaseId) {
      void this.eventStreamService.removeSubscription('UseCase', this.useCaseId);
    }
    this.sseSub?.unsubscribe();
  }

  /**
   * Reads the use case and applies it in two parts: server state always, form state only when the
   * user has nothing unsaved.
   *
   * This editor had no guard of any kind, so every caller reset the form - including the SSE
   * subscription below, which meant a remote change discarded whatever the user was typing. The
   * initial load was worse still: `ngOnInit` awaits `loadForProject()` *and* `listActors()` before
   * this fetch is even issued, so the window between the form rendering and the reset landing is
   * the widest of any editor here (#185).
   *
   * The check sits after the await on purpose, so it catches edits made while the request was
   * still in flight. Applying it to every caller also means a 409 recovery keeps the edit the user
   * is retrying rather than throwing it away, matching #184.
   *
   * The five collections stay unconditional: they render their own tables, so a refresh after an
   * association still lands even with a rename sitting in the Name field - the stale-table trap
   * #184 found in `actor-editor`. `useCaseName` is the *persisted* name, so it moves only with the
   * form.
   */
  /** Re-run the initial load; wired to the error state's (retry) output. */
  retryLoad(): void {
    void this.loadUseCase();
  }

  /**
   * @param skeleton show the loading skeleton and the retryable error state. Suppressed for every
   *                 background caller - SSE refresh, post-save refetch and 409 recovery - where
   *                 blanking the form the user is looking at would be worse than a stale moment,
   *                 and where a failure belongs in the inline message rather than in place of the
   *                 form. Mirrors `scenario-editor`.
   */
  private async loadUseCase(skeleton = true): Promise<void> {
    if (skeleton) {
      this.loading.set(true);
      this.loadError.set(null);
    }
    try {
      const uc = await this.useCaseService.getUseCase(this.projectName, this.useCaseId!);
      // Always take the version. The entity moved on, and holding the stale one guarantees a
      // 409 on the user's next save.
      this.useCase.set(uc);
      this.version = uc.version;
      if (!this.hasUnsavedChanges()) {
        this.useCaseName.set(uc.name);
        this.detailsForm.reset({
          name: uc.name,
          primaryActorName: uc.primaryActorName ?? '',
          text: uc.text ?? '',
        });
      }
      this.goals.set(uc.goals ?? []);
      this.stories.set(uc.stories ?? []);
      this.actors.set(uc.actors ?? []);
      this.additionalScenarios.set(uc.additionalScenarios ?? []);
      if (uc.scenarioId) {
        this.primaryScenario.set(await this.scenarioService.getScenario(this.projectName, uc.scenarioId));
      } else {
        this.primaryScenario.set(null);
      }
    } catch {
      if (skeleton) {
        this.loadError.set('Failed to load use case.');
      } else {
        this.showError('Failed to load use case.');
      }
    } finally {
      if (skeleton) {
        this.loading.set(false);
      }
    }
    if (this.useCaseId && !this.sseSub) {
      void this.eventStreamService.addSubscription('UseCase', this.useCaseId);
      this.sseSub = this.eventStreamService.events$.subscribe(envelope => {
        if (envelope.targetType === 'UseCase' && envelope.targetId === this.useCaseId) {
          void this.loadUseCase(false);
        }
      });
    }
  }

  /**
   * Runs the commit for the wizard's current step. Only Details talks to the API; the other
   * three steps' associations commit through their selectors as the user works, and each one
   * refreshes the held version via refreshCollections().
   */
  async onStepCommit(request: WizardCommitRequest): Promise<void> {
    if (request.step.key !== 'details') {
      request.complete();
      return;
    }

    this.submitted.set(true);
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

  /** Done on the last step: the use case is already saved, so just go to it. */
  onWizardFinished(): void {
    if (this.useCaseId != null) {
      this.router.navigate(['/projects', this.projectName, 'use-cases', this.useCaseId]);
    } else {
      this.onBack();
    }
  }

  /**
   * Issues `EditUseCase` and, on success, adopts the id and version from the response.
   *
   * Deliberately does not navigate on create - the old path routed to the saved use case
   * immediately, which is what made all five association sections unreachable until a second
   * visit. The wizard captures the id and advances instead.
   */
  private async saveDetails(): Promise<CommandResult<unknown>> {
    this.saving.set(true);
    this.showError(null);
    clearServerErrors(this.detailsForm);
    try {
      const { name, primaryActorName, text } = this.detailsForm.getRawValue();
      const input: Record<string, unknown> = {
        projectName: this.projectName,
        name,
        text: text || null,
        primaryActorName: primaryActorName || null,
      };
      if (this.useCaseId != null) input['useCaseId'] = this.useCaseId;
      if (this.version != null) input['version'] = this.version;

      const result = await this.commandService.execute('EditUseCase', input);
      if (!result.success) {
        const unresolved = applyCommandErrors(this.detailsForm, result.violations);
        if (unresolved.length) {
          this.showError(unresolved.join(SEPARATOR));
        }
        return result;
      }

      const wasCreate = this.useCaseId == null;
      if (wasCreate) {
        this.projectService.notifyTreeChanged();
      }

      const saved = result.entity as UseCaseDto | null;
      if (saved) {
        this.useCaseId = saved.id;
        this.version = saved.version;
        this.useCaseName.set(saved.name);
      }
      this.detailsForm.markAsPristine();
      this.messageService.add({ severity: 'success', summary: 'Saved', detail: 'Use case saved.' });

      if (wasCreate && this.useCaseId != null) {
        await this.loadUseCase(false);
      }
      return result;
    } catch {
      return {
        success: false,
        entityType: 'UseCase',
        entity: null,
        error: 'An unexpected error occurred.',
        violations: null,
      };
    } finally {
      this.saving.set(false);
    }
  }

  /**
   * If `result` is an optimistic-lock conflict (HTTP 409), refetch so the held version is
   * current and the user can retry. Returns whether it handled the result.
   */
  private async recoverFromStaleVersion(result: CommandResult<unknown>): Promise<boolean> {
    if (result.status !== 409 || this.useCaseId == null) {
      return false;
    }
    await this.loadUseCase(false);
    return true;
  }

  /** Edit-mode Save. */
  async onSave(): Promise<void> {
    this.submitted.set(true);
    if (this.detailsForm.invalid) {
      this.detailsForm.markAllAsTouched();
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

  private async refreshCollections(): Promise<void> {
    try {
      const uc = await this.useCaseService.getUseCase(this.projectName, this.useCaseId!);
      // Take the version. Every association command here merges the use case and bumps its
      // @Version, and none of them returns an entity (they register no result extractor), so
      // this refetch is the only place the new value can come from. It was refetching
      // everything EXCEPT this, which made the bug invisible: the refresh looked complete.
      // Without it, adding a goal and then saving the name 409s. See #178/#180.
      this.version = uc.version;
      this.useCase.set(uc);
      this.goals.set(uc.goals ?? []);
      this.stories.set(uc.stories ?? []);
      this.actors.set(uc.actors ?? []);
      this.additionalScenarios.set(uc.additionalScenarios ?? []);
      if (uc.scenarioId) {
        this.primaryScenario.set(await this.scenarioService.getScenario(this.projectName, uc.scenarioId));
      } else {
        this.primaryScenario.set(null);
      }
    } catch {
      this.showError('Failed to refresh.');
    }
  }

  /**
   * Apply the merged use case an association command returns (#180). Every goal/story/actor
   * association here merges the use case and returns it — version, goals, stories, actors and
   * additional scenarios — as `result.entity`, so we consume that instead of refetching.
   * Associations never change the primary scenario, so unlike `refreshCollections()` this issues no
   * scenario GET either. (The scenario commands keep `refreshCollections()` precisely because they
   * do change the primary scenario.)
   *
   * Guarded on `version`: concurrent associations can resolve out of order, and since every merge
   * increments `@Version`, a response older than what we hold would restore stale lists, so we
   * ignore it. A skipped version self-corrects through the next-save 409 recovery.
   */
  private applyAssociationResult(entity: UseCaseDto | null): void {
    if (!entity) {
      return;
    }
    if (this.version != null && entity.version <= this.version) {
      return;
    }
    this.version = entity.version;
    this.useCase.set(entity);
    this.goals.set(entity.goals ?? []);
    this.stories.set(entity.stories ?? []);
    this.actors.set(entity.actors ?? []);
    this.additionalScenarios.set(entity.additionalScenarios ?? []);
  }

  @ViewChild('ucScenariosSection') ucScenariosSection?: RelationshipSectionComponent<ScenarioDto>;
  @ViewChild('ucGoalsSection') ucGoalsSection?: RelationshipSectionComponent<GoalDto>;
  @ViewChild('ucStoriesSection') ucStoriesSection?: RelationshipSectionComponent<StoryDto>;
  @ViewChild('ucActorsSection') ucActorsSection?: RelationshipSectionComponent<ActorDto>;
  /** Accessible names + row identity for the relationship lists. */
  scenarioRemoveAria = (x: { name: string }) => 'Remove scenario ' + x.name;
  ucGoalRemoveAria = (x: { name: string }) => 'Remove goal ' + x.name;
  ucStoryRemoveAria = (x: { name: string }) => 'Remove story ' + x.name;
  ucActorRemoveAria = (x: { name: string }) => 'Remove actor ' + x.name;
  refTrackBy = (x: { id: number | null }) => x.id;

  async addGoal(ref: EntityReferenceDto): Promise<void> {
    this.showGoalSelector = false;
    const result = await this.commandService.execute('AddGoalToGoalContainer', {
      projectName: this.projectName, goalContainerId: this.useCaseId, goalId: ref.id,
      containerType: 'UseCase'
    });
    if (result.success) {
      this.applyAssociationResult(result.entity as UseCaseDto | null);
      this.ucGoalsSection?.announceAdded(ref.name);
    } else this.showError(result.error ?? 'Failed to add goal.');
  }

  async removeGoal(goal: GoalDto): Promise<void> {
    const result = await this.commandService.execute('RemoveGoalFromGoalContainer', {
      projectName: this.projectName, goalContainerId: this.useCaseId, goalId: goal.id,
      containerType: 'UseCase'
    });
    if (result.success) {
      this.applyAssociationResult(result.entity as UseCaseDto | null);
      this.ucGoalsSection?.announceRemoved(goal.name);
    } else this.showError(result.error ?? 'Failed to remove goal.');
  }

  async addStory(ref: EntityReferenceDto): Promise<void> {
    this.showStorySelector = false;
    const result = await this.commandService.execute('AddStoryToStoryContainer', {
      projectName: this.projectName, storyContainerId: this.useCaseId, storyId: ref.id,
      containerType: 'UseCase'
    });
    if (result.success) {
      this.applyAssociationResult(result.entity as UseCaseDto | null);
      this.ucStoriesSection?.announceAdded(ref.name);
    } else this.showError(result.error ?? 'Failed to add story.');
  }

  async removeStory(story: StoryDto): Promise<void> {
    const result = await this.commandService.execute('RemoveStoryFromStoryContainer', {
      projectName: this.projectName, storyContainerId: this.useCaseId, storyId: story.id,
      containerType: 'UseCase'
    });
    if (result.success) {
      this.applyAssociationResult(result.entity as UseCaseDto | null);
      this.ucStoriesSection?.announceRemoved(story.name);
    } else this.showError(result.error ?? 'Failed to remove story.');
  }

  async addActorToList(ref: EntityReferenceDto): Promise<void> {
    this.showActorSelector = false;
    const result = await this.commandService.execute('AddActorToActorContainer', {
      projectName: this.projectName, actorContainerId: this.useCaseId, actorId: ref.id,
      containerType: 'UseCase'
    });
    if (result.success) {
      this.applyAssociationResult(result.entity as UseCaseDto | null);
      this.ucActorsSection?.announceAdded(ref.name);
    } else this.showError(result.error ?? 'Failed to add actor.');
  }

  async removeActor(actor: ActorDto): Promise<void> {
    const result = await this.commandService.execute('RemoveActorFromActorContainer', {
      projectName: this.projectName, actorContainerId: this.useCaseId, actorId: actor.id,
      containerType: 'UseCase'
    });
    if (result.success) {
      this.applyAssociationResult(result.entity as UseCaseDto | null);
      this.ucActorsSection?.announceRemoved(actor.name);
    } else this.showError(result.error ?? 'Failed to remove actor.');
  }

  /** Save the use case — the backend auto-creates a primary scenario with the use case name. */
  async createPrimaryScenario(): Promise<void> {
    this.saving.set(true);
    this.showError(null);
    try {
      const input: Record<string, unknown> = {
        projectName: this.projectName,
        name: this.detailsForm.controls.name.value,
        text: this.detailsForm.controls.text.value || null,
        primaryActorName: this.detailsForm.controls.primaryActorName.value || null,
        useCaseId: this.useCaseId,
        version: this.version
      };
      const result = await this.commandService.execute('EditUseCase', input);
      if (result.success) {
        const saved = result.entity as UseCaseDto;
        await this.refreshCollections();
        if (saved.scenarioId) {
          this.router.navigate(['/projects', this.projectName, 'scenarios', saved.scenarioId]);
        }
      } else {
        this.showError(result.error ?? 'Failed to create primary scenario.');
      }
    } catch {
      this.showError('An unexpected error occurred.');
    } finally {
      this.saving.set(false);
    }
  }

  async selectPrimaryScenario(ref: EntityReferenceDto): Promise<void> {
    this.showPrimaryScenarioSelector = false;
    const result = await this.commandService.execute('SetPrimaryScenarioOnUseCase', {
      projectName: this.projectName,
      useCaseId: this.useCaseId,
      scenarioId: ref.id
    });
    if (result.success) await this.refreshCollections();
    else this.showError(result.error ?? 'Failed to set primary scenario.');
  }

  async addScenario(ref: EntityReferenceDto): Promise<void> {
    this.showScenarioSelector = false;
    const result = await this.commandService.execute('AddScenarioToUseCase', {
      projectName: this.projectName,
      useCaseId: this.useCaseId,
      scenarioId: ref.id
    });
    if (result.success) {
      await this.refreshCollections();
      this.ucScenariosSection?.announceAdded(ref.name);
    } else this.showError(result.error ?? 'Failed to add scenario.');
  }

  async removeScenario(scenario: ScenarioDto): Promise<void> {
    const result = await this.commandService.execute('RemoveScenarioFromUseCase', {
      projectName: this.projectName,
      useCaseId: this.useCaseId,
      scenarioId: scenario.id
    });
    if (result.success) {
      await this.refreshCollections();
      this.ucScenariosSection?.announceRemoved(scenario.name);
    } else this.showError(result.error ?? 'Failed to remove scenario.');
  }

  navigateTo(type: string, id: number): void {
    this.router.navigate(['/projects', this.projectName, type, id]);
  }

  onCopy(): void {
    this.confirmationService.confirm({
      message: 'Create a copy of this use case?',
      accept: async () => {
        const result = await this.commandService.execute('CopyUseCase', {
          projectName: this.projectName, useCaseId: this.useCaseId
        });
        if (result.success && result.entity) {
          this.projectService.notifyTreeChanged();
          const copy = result.entity as UseCaseDto;
          this.router.navigate(['/projects', this.projectName, 'use-cases', copy.id]);
        } else {
          this.showError(result.error ?? 'Copy failed.');
        }
      }
    });
  }

  onDelete(): void {
    this.confirmationService.confirm({
      message: 'Are you sure you want to delete this use case?',
      accept: async () => {
        const result = await this.commandService.execute('DeleteUseCase', {
          projectName: this.projectName, useCaseId: this.useCaseId, version: this.version
        });
        if (result.success) {
          this.projectService.notifyTreeChanged();
          this.router.navigate(['/projects', this.projectName, 'use-cases']);
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
