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
import { NgTemplateOutlet } from '@angular/common';
import { PageHeaderComponent } from '../../shared/page-header';
import { AppCardComponent } from '../../shared/app-card';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { DirtyCheckable } from '../../core/dirty-check.guard';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { MessageModule } from 'primeng/message';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { CommandResult } from '../../models/command';
import { StoryDto } from '../../models/story';
import { EntityReferenceDto } from '../../models/entity-reference';
import { StoryService } from '../../core/story.service';
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
import { ARTIFACT_NAME_MAX_LENGTH } from '../../shared/validation-limits';

/** Wording for the stale-version recovery path, so the 409 case reads as recoverable. */
const STALE_VERSION_MESSAGE =
  'This story was changed elsewhere. Your copy has been refreshed — review the values and continue.';

@Component({
  selector: 'app-story-editor',
  standalone: true,
  imports: [PageHeaderComponent, AppCardComponent, RouterLink, ReactiveFormsModule, NgTemplateOutlet,
            ButtonModule, InputText, TextareaModule, SelectModule,
            TableModule, MessageModule, ConfirmDialogModule, EntitySelectorDialogComponent,
            AnnotationsSectionComponent, AppFieldComponent, AppFieldControlDirective,
            AppFormWizardComponent, AppWizardStepComponent],
  providers: [ConfirmationService],
  template: `
    <div class="story-editor" data-testid="story-editor">
      <div class="page-header">
        <app-page-header [title]="isNew() ? 'New Story' : storyName()" />
        <div class="page-actions">
          <p-button label="Back" icon="pi pi-arrow-left" severity="secondary"
                    data-testid="story-back"
                    [outlined]="true" (onClick)="onBack()" />
          @if (!isNew()) {
            @if (canEdit()) {
              <p-button label="Copy" icon="pi pi-copy" severity="secondary"
                        data-testid="story-copy"
                        [outlined]="true" (onClick)="onCopy()" />
            }
            @if (canDelete()) {
              <p-button label="Delete" icon="pi pi-trash" severity="danger"
                        data-testid="story-delete"
                        [outlined]="true" (onClick)="onDelete()" />
            }
          }
        </div>
      </div>

      @if (errorMessage()) {
        <p-message severity="error" [text]="errorMessage()!" />
      }

      @if (isNew()) {
        <!--
          Create runs as a wizard so Goals and Additional Actors are reachable before
          the first save. Step 1 commits EditStory on Continue, which is what gives the
          later steps the persisted storyId their association commands need.
        -->
        <app-form-wizard
          [(activeKey)]="wizardStep"
          navLabel="New story steps"
          (stepCommit)="onStepCommit($event)"
          (cancelled)="onBack()"
          (finished)="onWizardFinished()"
          data-testid="story-wizard"
        >
          <app-wizard-step key="details" label="Details" helper="Name, type and text"
                           [form]="detailsForm">
            <ng-template>
              <ng-container [ngTemplateOutlet]="detailsFields" />
            </ng-template>
          </app-wizard-step>

          <app-wizard-step key="goals" label="Goals" helper="Goals this story serves"
                           [optional]="true">
            <ng-template>
              <ng-container [ngTemplateOutlet]="goalsSection"
                            [ngTemplateOutletContext]="{ heading: false }" />
            </ng-template>
          </app-wizard-step>

          <app-wizard-step key="actors" label="Additional Actors"
                           helper="Actors beyond the primary" [optional]="true">
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
            <p-button label="Save" icon="pi pi-check" data-testid="story-save"
                      [disabled]="!canSave()" [loading]="saving()" (onClick)="onSave()" />
          </div>
        </app-card>

        <ng-container [ngTemplateOutlet]="goalsSection"
                      [ngTemplateOutletContext]="{ heading: true }" />
        <ng-container [ngTemplateOutlet]="actorsSection"
                      [ngTemplateOutletContext]="{ heading: true }" />
      }

      <app-entity-selector-dialog
        [visible]="showGoalSelector"
        [projectName]="projectName"
        entityType="Goal"
        [excludeIds]="existingGoalIds()"
        (selected)="onGoalSelected($event)"
        (closed)="showGoalSelector = false" />

      <app-entity-selector-dialog
        [visible]="showActorSelector"
        [projectName]="projectName"
        entityType="Actor"
        [excludeIds]="existingActorIds()"
        (selected)="onActorSelected($event)"
        (closed)="showActorSelector = false" />

      <!--
        Annotations render against a persisted entity, so they stay outside the wizard
        and appear once the story exists rather than as a dead panel during create.
      -->
      @if (storyId != null) {
        <app-annotations-section
          [projectName]="projectName"
          entityType="Story"
          [entityId]="storyId"
          [canEdit]="canEdit()" />
      }

      <p-confirmDialog />

      <!--
        Shared bodies, used by both the wizard step and the edit view so the two modes
        cannot drift apart. Controls bind with [formControl], not formControlName: these
        templates are projected into the wizard, where formControlName would look for a
        parent formGroup that is not there.

        The two p-selects pass controlId matching their own inputId, so app-field's
        <label for> targets the input PrimeNG renders inside its wrapper rather than
        depending on DOM-probe timing.
      -->
      <ng-template #detailsFields>
        <app-field label="Name" helper="What the story is called."
                   [control]="detailsForm.controls.name"
                   [errorMessages]="nameErrors"
                   [submitted]="submitted()">
          <input appFieldControl pInputText [formControl]="detailsForm.controls.name"
                 [attr.maxlength]="nameMaxLength"
                 placeholder="Story name" data-testid="story-name" />
        </app-field>

        <app-field label="Type" controlId="storyTypeInput"
                   [control]="detailsForm.controls.storyType"
                   [submitted]="submitted()">
          <p-select appFieldControl inputId="storyTypeInput" data-testid="story-type"
                    [formControl]="detailsForm.controls.storyType"
                    [options]="storyTypeOptions"
                    optionLabel="label" optionValue="value" />
        </app-field>

        <app-field label="Primary Actor" controlId="storyPrimaryActorInput"
                   helper="The actor this story is told from."
                   [control]="detailsForm.controls.primaryActorName"
                   [submitted]="submitted()">
          <p-select appFieldControl inputId="storyPrimaryActorInput"
                    data-testid="story-primary-actor"
                    [formControl]="detailsForm.controls.primaryActorName"
                    [options]="actorOptions()"
                    optionLabel="label"
                    optionValue="value"
                    [showClear]="true"
                    [pt]="{ clearIcon: { 'data-testid': 'story-primary-actor-clear' } }"
                    placeholder="Select primary actor"
                    styleClass="w-full" />
        </app-field>

        <app-field label="Text" [control]="detailsForm.controls.text" [divider]="false"
                   [submitted]="submitted()">
          <textarea appFieldControl pTextarea [formControl]="detailsForm.controls.text" rows="8"
                    placeholder="Story text" data-testid="story-text"></textarea>
        </app-field>
      </ng-template>

      <ng-template #goalsSection let-heading="heading">
        <div class="section">
          <div class="section-header">
            @if (heading) {
              <h2 class="rq-section-title">Goals</h2>
            }
            @if (canEdit() && storyId != null) {
              <p-button label="Add Goal" icon="pi pi-plus" size="small"
                        data-testid="story-add-goal"
                        (onClick)="showGoalSelector = true" />
            }
          </div>

          @if (storyId == null) {
            <p class="empty-text">Save the story's details first to add goals.</p>
          } @else if (story()?.goals?.length) {
            <p-table [value]="story()!.goals!" [rows]="10" data-testid="story-goals-table">
              <ng-template #header>
                <tr>
                  <th>Name</th>
                  @if (canEdit()) { <th class="col-actions"></th> }
                </tr>
              </ng-template>
              <ng-template #body let-g>
                <tr data-testid="story-goal-row">
                  <td><a class="entity-link" data-testid="story-goal-link" [routerLink]="['/projects', projectName, 'goals', g.id]">{{ g.name }}</a></td>
                  @if (canEdit()) {
                    <td><p-button icon="pi pi-trash" severity="danger" [text]="true" size="small"
                                  data-testid="story-remove-goal" [ariaLabel]="'Remove goal ' + g.name"
                                  (onClick)="onRemoveGoal(g)" /></td>
                  }
                </tr>
              </ng-template>
            </p-table>
          } @else {
            <p class="empty-text">No goals associated.</p>
          }
        </div>
      </ng-template>

      <ng-template #actorsSection let-heading="heading">
        <div class="section">
          <div class="section-header">
            @if (heading) {
              <h2 class="rq-section-title">Additional Actors</h2>
            }
            @if (canEdit() && storyId != null) {
              <p-button label="Add Actor" icon="pi pi-plus" size="small"
                        data-testid="story-add-actor"
                        (onClick)="showActorSelector = true" />
            }
          </div>

          @if (storyId == null) {
            <p class="empty-text">Save the story's details first to add actors.</p>
          } @else if (story()?.actors?.length) {
            <p-table [value]="story()!.actors!" [rows]="10" data-testid="story-additional-actors-table">
              <ng-template #header>
                <tr>
                  <th>Name</th>
                  @if (canEdit()) { <th class="col-actions"></th> }
                </tr>
              </ng-template>
              <ng-template #body let-a>
                <tr data-testid="story-additional-actor-row">
                  <td><a class="entity-link" data-testid="story-additional-actor-link" [routerLink]="['/projects', projectName, 'actors', a.id]">{{ a.name }}</a></td>
                  @if (canEdit()) {
                    <td><p-button icon="pi pi-trash" severity="danger" [text]="true" size="small"
                                  data-testid="story-remove-additional-actor" [ariaLabel]="'Remove actor ' + a.name"
                                  (onClick)="onRemoveActor(a)" /></td>
                  }
                </tr>
              </ng-template>
            </p-table>
          } @else {
            <p class="empty-text">No actors associated.</p>
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
    .section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem; gap: 0.5rem; }
    .section-header h2 { margin: 0; }
    .empty-text { color: var(--p-text-secondary-color); font-style: italic; }
    .entity-link { cursor: pointer; color: var(--p-primary-color); text-decoration: underline; }
  `]
})
export class StoryEditorComponent implements OnInit, OnDestroy, DirtyCheckable {
  isNew = signal(true);
  storyName = signal('');
  story = signal<StoryDto | null>(null);
  errorMessage = signal<string | null>(null);
  saving = signal(false);
  canEdit = signal(false);
  canDelete = signal(false);
  /** True once a save/commit has been attempted, so untouched invalid fields explain themselves. */
  submitted = signal(false);

  actorOptions = signal<{label: string, value: string}[]>([]);

  /**
   * Mirrors the backend `@Size(max = ValidationLimits.ARTIFACT_NAME_MAX)` (#171). Bound with
   * `[attr.maxlength]` rather than `maxlength` on purpose: Angular's MaxLengthValidator directive
   * matches `[maxlength][formControl]`, so the plain binding would register a SECOND maxlength
   * validator on top of the one in the form definition. `attr.` sets the HTML attribute only, which
   * is all that is wanted here — the browser stops the typing, the form owns the validation.
   */
  readonly nameMaxLength = ARTIFACT_NAME_MAX_LENGTH;

  /**
   * Details step / edit form. Replaces the previous `name` / `text` / `storyType` /
   * `primaryActorName` ngModel fields and the hand-rolled `trackChanges()` +
   * `original*` comparison, which the form's own dirty state now covers.
   */
  readonly detailsForm = new FormGroup({
    name: new FormControl('', {
      validators: [Validators.required, Validators.maxLength(ARTIFACT_NAME_MAX_LENGTH)],
      nonNullable: true,
    }),
    storyType: new FormControl('Success', { validators: [Validators.required], nonNullable: true }),
    primaryActorName: new FormControl('', { nonNullable: true }),
    text: new FormControl('', { nonNullable: true }),
  });

  readonly nameErrors = { required: 'A story needs a name.' };

  /** Active wizard step key, two-way bound to `app-form-wizard`. */
  wizardStep = 'details';

  showGoalSelector = false;
  showActorSelector = false;
  storyTypeOptions = [
    { label: 'Success', value: 'Success' },
    { label: 'Exception', value: 'Exception' }
  ];

  projectName = '';
  storyId: number | null = null;
  private version: number | null = null;
  private paramSub?: Subscription;
  private sseSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private storyService: StoryService,
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
      const idParam = params.get('storyId') ?? '';
      const newIsNew = idParam === 'new';

      // Reset the form synchronously for the new-story path BEFORE the two
      // awaits below (loadForProject and listActors). Otherwise typing during
      // either yield would later be clobbered by the reset and Angular
      // change-detection would clear the input. See term-editor.ts for the
      // same pattern.
      if (newIsNew) {
        this.isNew.set(true);
        this.story.set(null);
        this.storyId = null;
        this.version = null;
        this.wizardStep = 'details';
        this.submitted.set(false);
        this.detailsForm.reset({ name: '', storyType: 'Success', primaryActorName: '', text: '' });
      }

      await this.permissionService.loadForProject(this.projectName);
      this.canEdit.set(this.permissionService.canEdit('Story'));
      this.canDelete.set(this.permissionService.canDelete('Story'));

      const actors = await this.actorService.listActors(this.projectName);
      this.actorOptions.set(actors.map(a => ({ label: a.name, value: a.name })));

      if (!newIsNew) {
        this.isNew.set(false);
        this.storyId = +idParam;
        this.loadStory();
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
    if (this.storyId) {
      void this.eventStreamService.removeSubscription('Story', this.storyId);
    }
    this.sseSub?.unsubscribe();
  }

  private async loadStory(fromSSE = false): Promise<void> {
    try {
      const s = await this.storyService.getStory(this.projectName, this.storyId!);
      if (fromSSE && this.hasUnsavedChanges()) {
        // Don't overwrite unsaved user edits, but still take the new version: the
        // entity moved on, and holding the stale one guarantees a 409 on the next
        // save. (The previous implementation had no such guard and clobbered edits.)
        this.version = s.version;
        this.story.set(s);
        return;
      }
      this.story.set(s);
      this.storyName.set(s.name);
      this.detailsForm.reset({
        name: s.name,
        storyType: s.storyType,
        primaryActorName: s.primaryActorName ?? '',
        text: s.text,
      });
      this.version = s.version;
    } catch {
      this.errorMessage.set('Failed to load story.');
    }
    if (this.storyId && !this.sseSub) {
      void this.eventStreamService.addSubscription('Story', this.storyId);
      this.sseSub = this.eventStreamService.events$.subscribe(envelope => {
        if (envelope.targetType === 'Story' && envelope.targetId === this.storyId) {
          void this.loadStory(true);
        }
      });
    }
  }

  existingGoalIds(): number[] {
    return (this.story()?.goals ?? [])
      .filter(g => g.id != null)
      .map(g => g.id!);
  }

  existingActorIds(): number[] {
    return (this.story()?.actors ?? [])
      .filter(a => a.id != null)
      .map(a => a.id!);
  }

  /**
   * Runs the commit for the wizard's current step.
   *
   * Only Details talks to the API — Goals and Additional Actors commit through their
   * own association commands as the user works, so their Continue just advances.
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

  /** Done on the last step: the story is already saved, so just go to it. */
  onWizardFinished(): void {
    if (this.storyId != null) {
      this.router.navigate(['/projects', this.projectName, 'stories', this.storyId]);
    } else {
      this.onBack();
    }
  }

  /**
   * Issues `EditStory` for the Details values and, on success, adopts the id and
   * version from the response.
   *
   * The version is **spent on use**: every accepted `EditStory` bumps it server-side,
   * so it is re-read from `result.entity` each time. Holding the value captured at
   * create and sending it again — which is what happens if the user steps back to
   * Details and presses Continue a second time — is a guaranteed 409.
   */
  private async saveDetails(): Promise<CommandResult<unknown>> {
    this.saving.set(true);
    this.errorMessage.set(null);
    try {
      const { name, storyType, primaryActorName, text } = this.detailsForm.getRawValue();
      const input: Record<string, unknown> = {
        projectName: this.projectName,
        name,
        text,
        storyTypeName: storyType,
        primaryActorName: primaryActorName || null,
      };
      if (this.storyId != null) input['storyId'] = this.storyId;
      if (this.version != null) input['version'] = this.version;

      const result = await this.commandService.execute('EditStory', input);
      if (!result.success) {
        return result;
      }

      const wasCreate = this.storyId == null;
      if (wasCreate) {
        this.projectService.notifyTreeChanged();
      }

      const saved = result.entity as StoryDto | null;
      if (saved) {
        this.storyId = saved.id;
        this.version = saved.version;
        this.storyName.set(saved.name);
      }
      this.detailsForm.markAsPristine();
      this.messageService.add({ severity: 'success', summary: 'Saved', detail: 'Story saved.' });

      // Hydrate goals / actors (and start the SSE subscription) the first time the
      // story exists, so the later steps have something to render.
      if (wasCreate && this.storyId != null) {
        await this.loadStory();
      }
      return result;
    } catch {
      return {
        success: false,
        entityType: 'Story',
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
   * `EntityLockException.staleEntity`), refetch so the held version is current and
   * the user can retry. Returns whether it handled the result.
   */
  private async recoverFromStaleVersion(result: CommandResult<unknown>): Promise<boolean> {
    if (result.status !== 409 || this.storyId == null) {
      return false;
    }
    await this.loadStory();
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
      message: 'Create a copy of this story?',
      accept: async () => {
        const result = await this.commandService.execute('CopyStory', {
          projectName: this.projectName,
          storyId: this.storyId
        });
        if (result.success && result.entity) {
          this.projectService.notifyTreeChanged();
          const copy = result.entity as StoryDto;
          this.router.navigate(['/projects', this.projectName, 'stories', copy.id]);
        } else {
          this.errorMessage.set(result.error ?? 'Copy failed.');
        }
      }
    });
  }

  onDelete(): void {
    this.confirmationService.confirm({
      message: 'Are you sure you want to delete this story?',
      accept: async () => {
        const result = await this.commandService.execute('DeleteStory', {
          projectName: this.projectName,
          storyId: this.storyId,
          version: this.version
        });
        if (result.success) {
          this.projectService.notifyTreeChanged();
          // Nothing left to guard against — don't let the dirty check block the exit.
          this.detailsForm.markAsPristine();
          this.router.navigate(['/projects', this.projectName, 'stories']);
        } else {
          this.errorMessage.set(result.error ?? 'Delete failed.');
        }
      }
    });
  }

  async onGoalSelected(ref: EntityReferenceDto): Promise<void> {
    this.showGoalSelector = false;
    try {
      const result = await this.commandService.execute('AddGoalToGoalContainer', {
        projectName: this.projectName,
        goalContainerId: this.storyId,
        goalId: ref.id,
        containerType: 'Story'
      });
      if (result.success) {
        this.messageService.add({ severity: 'success', summary: 'Goal added', detail: 'Goal added.' });
        this.story.update(s => s ? {
          ...s,
          goals: [...(s.goals ?? []), ref].sort((a, b) => a.name.localeCompare(b.name))
        } : s);
      } else {
        this.errorMessage.set(result.error ?? 'Failed to add goal.');
      }
    } catch {
      this.errorMessage.set('Failed to add goal.');
    }
  }

  async onRemoveGoal(goalRef: EntityReferenceDto): Promise<void> {
    try {
      const result = await this.commandService.execute('RemoveGoalFromGoalContainer', {
        projectName: this.projectName,
        goalContainerId: this.storyId,
        goalId: goalRef.id,
        containerType: 'Story'
      });
      if (result.success) {
        this.messageService.add({ severity: 'success', summary: 'Goal removed', detail: 'Goal removed.' });
        this.story.update(s => s ? {
          ...s,
          goals: (s.goals ?? []).filter(g => g.id !== goalRef.id)
        } : s);
      } else {
        this.errorMessage.set(result.error ?? 'Failed to remove goal.');
      }
    } catch {
      this.errorMessage.set('Failed to remove goal.');
    }
  }

  async onActorSelected(ref: EntityReferenceDto): Promise<void> {
    this.showActorSelector = false;
    try {
      const result = await this.commandService.execute('AddActorToActorContainer', {
        projectName: this.projectName,
        actorContainerId: this.storyId,
        actorId: ref.id
      });
      if (result.success) {
        this.messageService.add({ severity: 'success', summary: 'Actor added', detail: 'Actor added.' });
        this.story.update(s => s ? {
          ...s,
          actors: [...(s.actors ?? []), ref].sort((a, b) => a.name.localeCompare(b.name))
        } : s);
      } else {
        this.errorMessage.set(result.error ?? 'Failed to add actor.');
      }
    } catch {
      this.errorMessage.set('Failed to add actor.');
    }
  }

  async onRemoveActor(actorRef: EntityReferenceDto): Promise<void> {
    try {
      const result = await this.commandService.execute('RemoveActorFromActorContainer', {
        projectName: this.projectName,
        actorContainerId: this.storyId,
        actorId: actorRef.id
      });
      if (result.success) {
        this.messageService.add({ severity: 'success', summary: 'Actor removed', detail: 'Actor removed.' });
        this.story.update(s => s ? {
          ...s,
          actors: (s.actors ?? []).filter(a => a.id !== actorRef.id)
        } : s);
      } else {
        this.errorMessage.set(result.error ?? 'Failed to remove actor.');
      }
    } catch {
      this.errorMessage.set('Failed to remove actor.');
    }
  }

  onBack(): void {
    this.router.navigate(['/projects', this.projectName, 'stories']);
  }
}
