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
import { Component, computed, OnDestroy, OnInit, signal } from '@angular/core';
import { PageHeaderComponent } from '../../shared/page-header';
import { AppCardComponent } from '../../shared/app-card';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NgTemplateOutlet } from '@angular/common';
import { Subscription } from 'rxjs';
import { DirtyCheckable } from '../../core/dirty-check.guard';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { TableModule } from 'primeng/table';
import { MessageModule } from 'primeng/message';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ActorDto } from '../../models/actor';
import { EntityReferenceDto } from '../../models/entity-reference';
import { ActorService } from '../../core/actor.service';
import { CommandService } from '../../core/command.service';
import { ProjectService } from '../../core/project.service';
import { PermissionService } from '../../core/permission.service';
import { EventStreamService } from '../../core/event-stream.service';
import { EntitySelectorDialogComponent } from '../../shared/entity-selector-dialog';
import { AnnotationsSectionComponent } from '../../shared/annotations-section';
import { AppFieldComponent, AppFieldControlDirective } from '../../shared/app-field';
import {
  AppFormWizardComponent,
  AppWizardStepComponent,
  WizardCommitRequest,
} from '../../shared/app-form-wizard';
import { applyCommandErrors, clearServerErrors } from '../../shared/form-errors';
import { ARTIFACT_NAME_MAX_LENGTH } from '../../shared/validation-limits';
import { CommandResult } from '../../models/command';

/**
 * JPA entity property name -> form control name, for {@link applyCommandErrors}.
 *
 * `CommandController` reports violations using the entity's property names, so `ActorImpl`'s
 * inherited `text` is the key; `EditActorInput` spells the same field `description`, mapped
 * here too so the control resolves whichever name arrives. #176 deletes this map.
 */
const ACTOR_FIELD_MAP: Record<string, string> = {
  description: 'text',
};

/** Joins page-level violations that resolved to no control. */
const SEPARATOR = '; ';

/** Wording for the stale-version recovery path, so the 409 case reads as recoverable. */
const STALE_VERSION_MESSAGE =
  'This actor was changed elsewhere. Your copy has been refreshed - review the values and continue.';

@Component({
  selector: 'app-actor-editor',
  standalone: true,
  imports: [PageHeaderComponent, AppCardComponent, RouterLink, NgTemplateOutlet, ReactiveFormsModule,
            ButtonModule, InputText, TextareaModule, TableModule,
            MessageModule, ConfirmDialogModule, EntitySelectorDialogComponent,
            AnnotationsSectionComponent, AppFieldComponent, AppFieldControlDirective,
            AppFormWizardComponent, AppWizardStepComponent],
  providers: [ConfirmationService],
  template: `
    <div class="actor-editor" data-testid="actor-editor">
      <div class="page-header">
        <app-page-header [title]="isNew() ? 'New Actor' : actorName()" />
        <div class="page-actions">
          <p-button label="Back" icon="pi pi-arrow-left" severity="secondary" data-testid="actor-back"
                    [outlined]="true" (onClick)="onBack()" />
          @if (!isNew()) {
            @if (canEdit()) {
              <p-button label="Copy" icon="pi pi-copy" severity="secondary" data-testid="actor-copy"
                        [outlined]="true" (onClick)="onCopy()" />
            }
            @if (canDelete()) {
              <p-button label="Delete" icon="pi pi-trash" severity="danger" data-testid="actor-delete"
                        [outlined]="true" (onClick)="onDelete()" />
            }
          }
        </div>
      </div>

      @if (errorMessage()) {
        <p-message severity="error" [text]="errorMessage()!" data-testid="actor-error" />
      }

      @if (isNew()) {
        <!--
          Create runs as a wizard (#173) so Goals is reachable before the first save. Step 1
          commits EditActor on Continue, which is what gives step 2 the persisted actorId the
          goal selector needs.
        -->
        <app-form-wizard
          [(activeKey)]="wizardStep"
          navLabel="New actor steps"
          (stepCommit)="onStepCommit($event)"
          (cancelled)="onBack()"
          (finished)="onWizardFinished()"
          data-testid="actor-wizard"
        >
          <app-wizard-step key="details" label="Details" helper="Name and description"
                           [form]="detailsForm">
            <ng-template>
              <ng-container [ngTemplateOutlet]="detailsFields" />
            </ng-template>
          </app-wizard-step>

          <app-wizard-step key="goals" label="Goals" helper="Link goals to this actor"
                           [optional]="true">
            <ng-template>
              <!-- heading: false - the wizard panel's own h2 already reads "Goals". -->
              <ng-container [ngTemplateOutlet]="goalsSection"
                            [ngTemplateOutletContext]="{ heading: false }" />
            </ng-template>
          </app-wizard-step>
        </app-form-wizard>
      } @else {
        <app-card>
          <ng-container [ngTemplateOutlet]="detailsFields" />

          @if (canEdit()) {
            <div class="form-actions">
              <p-button label="Save" icon="pi pi-check" data-testid="actor-save"
                        [disabled]="!canSave()" (onClick)="onSave()" />
            </div>
          }
        </app-card>

        <ng-container [ngTemplateOutlet]="goalsSection"
                      [ngTemplateOutletContext]="{ heading: true }" />

        <!-- Referenced By -->
        <div class="goals-section">
          <div class="section-header">
            <h3>Referenced By</h3>
          </div>
          @if (referencedByUseCases().length === 0 && referencedByStories().length === 0) {
            <p class="empty-text">Not referenced by any use case or story.</p>
          }
          @if (referencedByUseCases().length > 0) {
            <p class="ref-label">Use Cases</p>
            <p-table [value]="referencedByUseCases()" styleClass="p-datatable-sm">
              <ng-template pTemplate="body" let-ref>
                <tr data-testid="actor-refby-usecase-row">
                  <td>
                    <a class="entity-link" data-testid="actor-refby-usecase-link"
                       [routerLink]="['/projects', projectName, 'use-cases', ref.id]">{{ ref.name }}</a>
                  </td>
                </tr>
              </ng-template>
            </p-table>
          }
          @if (referencedByStories().length > 0) {
            <p class="ref-label">Stories</p>
            <p-table [value]="referencedByStories()" styleClass="p-datatable-sm">
              <ng-template pTemplate="body" let-ref>
                <tr data-testid="actor-refby-story-row">
                  <td>
                    <a class="entity-link" data-testid="actor-refby-story-link"
                       [routerLink]="['/projects', projectName, 'stories', ref.id]">{{ ref.name }}</a>
                  </td>
                </tr>
              </ng-template>
            </p-table>
          }
        </div>
      }
    </div>

    <app-entity-selector-dialog
      entityType="Goal"
      [projectName]="projectName"
      [excludeIds]="goalIds()"
      [visible]="showGoalSelector"
      (selected)="onGoalSelected($event)"
      (closed)="showGoalSelector = false" />

    <!--
      Annotations render against a persisted entity, so they stay outside the wizard and appear
      once the actor exists rather than as a dead panel during create.
    -->
    @if (actorId != null) {
      <app-annotations-section
        [projectName]="projectName"
        entityType="Actor"
        [entityId]="actorId"
        [canEdit]="canEdit()" />
    }

    <p-confirmDialog />

    <!--
      Shared bodies, used by both the wizard step and the edit view so the two cannot drift.
      Controls bind [formControl], not formControlName: these are projected into the wizard,
      where formControlName would look for a parent formGroup that is not there.
    -->
    <ng-template #detailsFields>
      <app-field label="Name" controlId="name" [control]="detailsForm.controls.name"
                 [errorMessages]="nameErrors" [submitted]="submitted()">
        <input appFieldControl pInputText [formControl]="detailsForm.controls.name" id="name"
               [attr.maxlength]="nameMaxLength"
               placeholder="Actor name" data-testid="actor-name" />
      </app-field>

      <app-field label="Description" controlId="text" [control]="detailsForm.controls.text" [divider]="false"
                 [submitted]="submitted()">
        <textarea appFieldControl pTextarea [formControl]="detailsForm.controls.text" id="text" rows="4"
                  placeholder="Actor description" data-testid="actor-text"></textarea>
      </app-field>
    </ng-template>

    <ng-template #goalsSection let-heading="heading">
      <div class="goals-section">
        <div class="section-header">
          @if (heading) {
            <h2 class="rq-section-title">Goals</h2>
          }
          @if (canEdit() && actorId != null) {
            <p-button label="Add Goal" icon="pi pi-plus" severity="secondary"
                      data-testid="actor-add-goal"
                      [outlined]="true" (onClick)="showGoalSelector = true" />
          }
        </div>
        @if (actorId == null) {
          <p class="empty-text">Save the actor's details first to add goals.</p>
        } @else {
          <p-table [value]="goals()" [rows]="10">
            <ng-template #header>
              <tr>
                <th>Name</th>
                @if (canEdit()) {
                  <!--
                    An actions column still needs a header for screen readers; an empty <th>
                    is an axe empty-table-header violation. Visually hidden so the column
                    stays icon-only. Caught by actor-editor.a11y.spec.ts.
                  -->
                  <th class="col-actions"><span class="rq-visually-hidden">Actions</span></th>
                }
              </tr>
            </ng-template>
            <ng-template #body let-g>
              <tr data-testid="actor-goal-row">
                <td>
                  <a class="entity-link" data-testid="actor-goal-link"
                     [routerLink]="['/projects', projectName, 'goals', g.id]">{{ g.name }}</a>
                </td>
                @if (canEdit()) {
                  <td>
                    <p-button icon="pi pi-times" severity="danger" [text]="true"
                              data-testid="actor-remove-goal" [ariaLabel]="'Remove goal ' + g.name"
                              [rounded]="true" (onClick)="onRemoveGoal(g)" />
                  </td>
                }
              </tr>
            </ng-template>
            <ng-template #emptymessage>
              <tr><td colspan="2" class="empty-text">No goals associated.</td></tr>
            </ng-template>
          </p-table>
        }
      </div>
    </ng-template>
  `,
  styles: [`
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
    .page-actions { display: flex; gap: 0.5rem; }
    .form-actions { margin-top: 1rem; max-width: 700px; }
    .goals-section { margin-top: 2rem; }
    .section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem; }
    .section-header h3 { margin: 0; }
    .entity-link { color: var(--p-primary-color); cursor: pointer; text-decoration: underline; }
    .empty-text { color: var(--p-text-muted-color); font-style: italic; }
    .ref-label { font-weight: 600; font-size: 0.85rem; margin: 0.5rem 0 0.25rem; color: var(--p-text-secondary-color); }
  `]
})
export class ActorEditorComponent implements OnInit, OnDestroy, DirtyCheckable {
  actor = signal<ActorDto | null>(null);
  actorName = signal('');
  isNew = signal(false);
  canEdit = signal(false);
  canDelete = signal(false);
  errorMessage = signal<string | null>(null);
  goals = signal<EntityReferenceDto[]>([]);
  goalIds = computed(() => this.goals().map(g => g.id).filter((id): id is number => id !== null));
  referencedByUseCases = signal<EntityReferenceDto[]>([]);
  referencedByStories = signal<EntityReferenceDto[]>([]);
  showGoalSelector = false;

  saving = signal(false);
  submitted = signal(false);
  version: number | null = null;
  projectName = '';

  /**
   * Mirrors the backend `@Size(max = ValidationLimits.ARTIFACT_NAME_MAX)` (#171). Bound with
   * `[attr.maxlength]` rather than `maxlength`: the latter matches Angular's MaxLengthValidator
   * directive selector and would register a second validator on top of the form's own.
   */
  readonly nameMaxLength = ARTIFACT_NAME_MAX_LENGTH;

  /**
   * Details step / edit form. Replaces the `name` + `text` ngModel fields and the hand-rolled
   * `trackChanges()` + `original*` comparison, which the form's own dirty state now covers.
   */
  readonly detailsForm = new FormGroup({
    name: new FormControl('', {
      validators: [Validators.required, Validators.maxLength(ARTIFACT_NAME_MAX_LENGTH)],
      nonNullable: true,
    }),
    text: new FormControl('', { nonNullable: true }),
  });

  readonly nameErrors = { required: 'An actor needs a name.' };

  /** Active wizard step key, two-way bound to `app-form-wizard`. */
  wizardStep = 'details';

  actorId: number | null = null;
  private paramSub?: Subscription;
  private sseSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private actorService: ActorService,
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
      const idParam = params.get('actorId') ?? '';
      const newIsNew = idParam === 'new';

      // Reset the form synchronously for the new-actor path BEFORE the
      // loadForProject await. Otherwise typing during the yield would later
      // be clobbered by the reset and Angular change-detection would clear
      // the input. See term-editor.ts for the same pattern.
      if (newIsNew) {
        this.isNew.set(true);
        this.actor.set(null);
        this.actorId = null;
        this.version = null;
        this.wizardStep = 'details';
        this.submitted.set(false);
        this.detailsForm.reset({ name: '', text: '' });
        this.goals.set([]);
      }

      await this.permissionService.loadForProject(this.projectName);
      this.canEdit.set(this.permissionService.canEdit('Actor'));
      this.canDelete.set(this.permissionService.canDelete('Actor'));

      if (!newIsNew) {
        this.isNew.set(false);
        this.actorId = +idParam;
        this.loadActor();
      }
    });
  }

  hasUnsavedChanges(): boolean {
    return this.detailsForm.dirty;
  }

  ngOnDestroy(): void {
    this.paramSub?.unsubscribe();
    if (this.actorId) {
      void this.eventStreamService.removeSubscription('Actor', this.actorId);
    }
    this.sseSub?.unsubscribe();
  }

  private async loadActor(fromSSE = false): Promise<void> {
    try {
      const a = await this.actorService.getActor(this.projectName, this.actorId!);
      // Don't overwrite unsaved user edits when called from an SSE notification.
      if (fromSSE && this.hasUnsavedChanges()) {
        // Still take the new version: the entity moved on, and holding the stale one
        // guarantees a 409 on the user's next save.
        this.version = a.version;
        this.actor.set(a);
        return;
      }
      this.actor.set(a);
      this.actorName.set(a.name);
      this.detailsForm.reset({ name: a.name, text: a.text ?? '' });
      this.version = a.version;
      this.goals.set(a.goals ?? []);
      this.referencedByUseCases.set(a.referencedByUseCases ?? []);
      this.referencedByStories.set(a.referencedByStories ?? []);
    } catch {
      this.errorMessage.set('Failed to load actor.');
    }
    if (this.actorId && !this.sseSub) {
      void this.eventStreamService.addSubscription('Actor', this.actorId);
      this.sseSub = this.eventStreamService.events$.subscribe(envelope => {
        if (envelope.targetType === 'Actor' && envelope.targetId === this.actorId) {
          void this.loadActor(true);
        }
      });
    }
  }

  /**
   * Re-reads the actor's optimistic-lock version after a goal association changes.
   *
   * `AddGoalToGoalContainerCommandImpl` / `RemoveGoalFromGoalContainerCommandImpl` end by
   * merging the container - which IS this actor - so every add or remove bumps its
   * `@Version`. The client cannot read the new value from the response: both commands are
   * registered through the 4-arg `CommandRegistry.register` overload, which leaves
   * `resultExtractor` null, so `ApiCommandFactory.extractResult` returns null and
   * `result.entity` is empty. A refetch is the only way to see it.
   *
   * Without this, adding a goal and then saving the name fails with a 409 the user did
   * nothing to deserve. Pre-existing bug; #173's wizard makes it reachable in the create
   * flow too, which is what surfaced it.
   *
   * Deliberately narrow — it takes `version` and nothing else. A full `loadActor()` here
   * would overwrite whatever the user had typed into Name or Description before touching
   * the Goals table.
   */
  private async refreshVersionAfterAssociation(): Promise<void> {
    if (this.actorId == null) {
      return;
    }
    try {
      const a = await this.actorService.getActor(this.projectName, this.actorId);
      this.version = a.version;
    } catch {
      // Leave the held version as-is: a failed refresh is not worth blocking the user, and
      // the next save reports the 409 with the existing recovery path.
    }
  }


  /** Edit-mode Save: blocked on invalid, unchanged, or in-flight. */
  canSave(): boolean {
    return this.detailsForm.valid && this.detailsForm.dirty && !this.saving();
  }

  /**
   * Runs the commit for the wizard's current step.
   *
   * Only Details talks to the API. The Goals step's associations commit through the selector
   * as the user works, so its Continue just advances.
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

  /** Done on the last step: the actor is already saved, so just go to it. */
  onWizardFinished(): void {
    if (this.actorId != null) {
      this.router.navigate(['/projects', this.projectName, 'actors', this.actorId]);
    } else {
      this.onBack();
    }
  }

  /**
   * Issues `EditActor` and, on success, adopts the id and version from the response.
   *
   * The version is spent on use: every accepted `EditActor` bumps it server-side, so it is
   * re-read from `result.entity` each time. Note the DTO spells the description field
   * `description` while the entity property is `text` - hence ACTOR_FIELD_MAP.
   */
  private async saveDetails(): Promise<CommandResult<unknown>> {
    this.saving.set(true);
    this.errorMessage.set(null);
    clearServerErrors(this.detailsForm);
    try {
      const { name, text } = this.detailsForm.getRawValue();
      const input: Record<string, unknown> = {
        projectName: this.projectName,
        actorId: this.actorId,
        name,
        description: text || null,
      };
      if (this.version != null) input['version'] = this.version;

      const result = await this.commandService.execute('EditActor', input);
      if (!result.success) {
        const unresolved = applyCommandErrors(this.detailsForm, result.violations, ACTOR_FIELD_MAP);
        if (unresolved.length) {
          this.errorMessage.set(unresolved.join(SEPARATOR));
        }
        return result;
      }

      const wasCreate = this.actorId == null;
      if (wasCreate) {
        this.projectService.notifyTreeChanged();
      }

      const saved = result.entity as ActorDto | null;
      if (saved) {
        this.actorId = saved.id;
        this.version = saved.version;
        this.actorName.set(saved.name);
      }
      this.detailsForm.markAsPristine();
      this.messageService.add({ severity: 'success', summary: 'Saved', detail: 'Actor saved.' });

      // Hydrate goals / referencedBy and start the SSE subscription the first time the actor
      // exists, so step 2 has something to render.
      if (wasCreate && this.actorId != null) {
        await this.loadActor();
      }
      return result;
    } catch {
      return {
        success: false,
        entityType: 'Actor',
        entity: null,
        error: 'Save failed.',
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
    if (result.status !== 409 || this.actorId == null) {
      return false;
    }
    await this.loadActor();
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
      this.errorMessage.set(STALE_VERSION_MESSAGE);
      return;
    }
    this.errorMessage.set(result.error ?? 'Save failed.');
  }

  onCopy(): void {
    this.confirmationService.confirm({
      message: 'Create a copy of this actor?',
      accept: async () => {
        const result = await this.commandService.execute('CopyActor', {
          projectName: this.projectName,
          actorId: this.actorId
        });
        if (result.success && result.entity) {
          this.projectService.notifyTreeChanged();
          const copy = result.entity as ActorDto;
          this.router.navigate(['/projects', this.projectName, 'actors', copy.id]);
        } else {
          this.errorMessage.set(result.error ?? 'Copy failed.');
        }
      }
    });
  }

  onDelete(): void {
    this.confirmationService.confirm({
      message: 'Are you sure you want to delete this actor?',
      accept: async () => {
        const result = await this.commandService.execute('DeleteActor', {
          projectName: this.projectName,
          actorId: this.actorId,
          version: this.version
        });
        if (result.success) {
          this.projectService.notifyTreeChanged();
          // Nothing left to guard against - don't let the dirty check block the exit.
          this.detailsForm.markAsPristine();
          this.router.navigate(['/projects', this.projectName, 'actors']);
        } else {
          this.errorMessage.set(result.error ?? 'Delete failed.');
        }
      }
    });
  }

  async onGoalSelected(goal: EntityReferenceDto): Promise<void> {
    this.showGoalSelector = false;
    try {
      const result = await this.commandService.execute('AddGoalToGoalContainer', {
        projectName: this.projectName,
        goalContainerId: this.actorId,
        goalId: goal.id,
        containerType: 'Actor'
      });
      if (result.success) {
        this.goals.update(list => [...list, goal].sort((a, b) => a.name.localeCompare(b.name)));
        await this.refreshVersionAfterAssociation();
        this.messageService.add({ severity: 'success', summary: 'Goal added', detail: `"${goal.name}" added successfully.` });
      } else {
        this.errorMessage.set(result.error ?? 'Failed to add goal.');
      }
    } catch {
      this.errorMessage.set('Failed to add goal.');
    }
  }

  async onRemoveGoal(goal: EntityReferenceDto): Promise<void> {
    try {
      const result = await this.commandService.execute('RemoveGoalFromGoalContainer', {
        projectName: this.projectName,
        goalContainerId: this.actorId,
        goalId: goal.id,
        containerType: 'Actor'
      });
      if (result.success) {
        this.goals.update(list => list.filter(g => g.id !== goal.id));
        await this.refreshVersionAfterAssociation();
        this.messageService.add({ severity: 'info', summary: 'Goal removed', detail: `"${goal.name}" removed.` });
      } else {
        this.errorMessage.set(result.error ?? 'Failed to remove goal.');
      }
    } catch {
      this.errorMessage.set('Failed to remove goal.');
    }
  }

  onBack(): void {
    this.router.navigate(['/projects', this.projectName, 'actors']);
  }
}
