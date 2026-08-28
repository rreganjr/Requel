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
import { NgTemplateOutlet } from '@angular/common';
import { Subscription } from 'rxjs';
import { DirtyCheckable } from '../../core/dirty-check.guard';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { TableModule } from 'primeng/table';
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import { UpdateBannerComponent } from '../../shared/app-update-banner';
import { AnnouncerService } from '../../core/announcer.service';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ActorDto } from '../../models/actor';
import { EntityReferenceDto } from '../../models/entity-reference';
import { ActorService } from '../../core/actor.service';
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
  'This actor was changed elsewhere. Your copy has been refreshed - review the values and continue.';

@Component({
  selector: 'app-actor-editor',
  standalone: true,
  imports: [EditorActionsComponent, PageHeaderComponent, AppCardComponent, RouterLink, NgTemplateOutlet, ReactiveFormsModule,
            ButtonModule, InputText, TextareaModule, TableModule, RelationshipSectionComponent,
            SubmitErrorComponent, UpdateBannerComponent, ConfirmDialogModule, EntitySelectorDialogComponent,
            AnnotationsSectionComponent, AppFieldComponent, AppFieldControlDirective,
            AppFormWizardComponent, AppWizardStepComponent,
            LoadingStateComponent, ErrorStateComponent],
  providers: [ConfirmationService],
  template: `
    <div class="actor-editor" data-testid="actor-editor">
      <div class="page-header">
        <app-page-header [title]="isNew() ? 'New Actor' : actorName()" />
        <div class="page-actions">
          <app-editor-actions [projectName]="projectName" />
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

      <app-submit-error [message]="errorMessage()" testid="actor-error" [retryable]="retryable()" (retry)="onSave()" />
      @if (updateAvailable()) {
        <app-update-banner message="This actor was changed elsewhere. Your unsaved changes are preserved."
                           testid="actor-update-banner"
                           (reload)="reloadFromExternalChange()" (dismiss)="updateAvailable.set(false)" />
      }

      @if (loading()) {
        <app-card>
          <app-loading-state label="Loading actor…" [lines]="4" testid="actor-editor-loading" />
        </app-card>
      } @else if (loadError()) {
        <app-error-state [message]="loadError()!" testid="actor-editor-load-error"
                         (retry)="retryLoad()" />
      } @else if (isNew()) {
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
      <app-relationship-section
        title="Goals" [showHeading]="heading"
        [items]="goals()" [headers]="['Name']"
        [canAdd]="canEdit() && actorId != null"
        addLabel="Add Goal" addTestid="actor-add-goal"
        removeTestid="actor-remove-goal" rowTestid="actor-goal-row" testid="actor-goals"
        emptyText="No goals associated."
        unsavedHint="Save the actor's details first to add goals."
        [removeAriaLabel]="goalRemoveAria" [trackBy]="goalTrackBy"
        (add)="showGoalSelector = true" (remove)="onRemoveGoal($event)">
        <ng-template #row let-g>
          <td>
            <a class="entity-link" data-testid="actor-goal-link"
               [routerLink]="['/projects', projectName, 'goals', g.id]">{{ g.name }}</a>
          </td>
        </ng-template>
      </app-relationship-section>
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
  retryable = signal(false);
  /** Sets the inline submit error and, by default, marks it non-retryable. */
  private showError(message: string | null): void {
    this.errorMessage.set(message);
    this.retryable.set(false);
  }
  /**
   * #185. The edit form renders only once the detail GET resolves, so there is no window in which
   * a user can type into a form the load is about to reset. Starts true: an edit route is loading
   * from the first frame, and the create path clears it synchronously in ngOnInit.
   */
  loading = signal(true);
  loadError = signal<string | null>(null);
  goals = signal<EntityReferenceDto[]>([]);
  goalIds = computed(() => this.goals().map(g => g.id).filter((id): id is number => id !== null));
  referencedByUseCases = signal<EntityReferenceDto[]>([]);
  referencedByStories = signal<EntityReferenceDto[]>([]);
  showGoalSelector = false;
  @ViewChild(RelationshipSectionComponent) goalsSection?: RelationshipSectionComponent<EntityReferenceDto>;
  /** Accessible name for each goal's remove button. */
  goalRemoveAria = (g: EntityReferenceDto): string => 'Remove goal ' + g.name;
  /** Row identity for the goals list. */
  goalTrackBy = (g: EntityReferenceDto) => g.id;

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
    private eventStreamService: EventStreamService,
    private announcer: AnnouncerService
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
        // Nothing to load, so resolve the gate synchronously (#185) - otherwise the create wizard
        // would sit behind the skeleton forever. Same reason the reset above is synchronous.
        this.loading.set(false);
        this.loadError.set(null);
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

  /** A cross-session update arrived while the form was dirty (#140): show the reload banner. */
  updateAvailable = signal(false);

  /** Discard local edits and re-apply the latest server state (from the update banner, #140). */
  async reloadFromExternalChange(): Promise<void> {
    this.updateAvailable.set(false);
    this.detailsForm.markAsPristine();
    await this.loadActor(false);
    this.announcer.announce('Actor reloaded.');
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

  /**
   * Reads the actor and applies it in two parts: server state always, form state only when the
   * user has nothing unsaved.
   *
   * The guard used to be `fromSSE && hasUnsavedChanges()`, which left the *initial* load free to
   * reset the form. `page.goto()` on the edit route returns long before this fetch does, so
   * anything typed in that gap was silently discarded and the form went back to pristine —
   * Save then stayed disabled with no explanation. `ngOnInit`'s create path already resets
   * synchronously to dodge exactly this; the edit path had no equivalent. It applies to every
   * caller now, which also means a 409 recovery keeps the edit the user is retrying instead of
   * throwing it away.
   *
   * The three collections stay unconditional. Previously the early return skipped them, so a
   * refresh arriving while the form was dirty left the goals and referenced-by tables stale.
   */
  /** Re-run the initial load; wired to the error state's (retry) output. */
  retryLoad(): void {
    void this.loadActor();
  }

  /**
   * @param skeleton show the loading skeleton and the retryable error state. Suppressed for every
   *                 background caller - SSE refresh, post-save refetch and 409 recovery - where
   *                 blanking the form the user is looking at would be worse than a stale moment,
   *                 and where a failure belongs in the inline message rather than in place of the
   *                 form. Mirrors `scenario-editor`.
   */
  private async loadActor(skeleton = true): Promise<void> {
    if (skeleton) {
      this.loading.set(true);
      this.loadError.set(null);
    }
    try {
      const a = await this.actorService.getActor(this.projectName, this.actorId!);
      // Always take the version. The entity moved on, and holding the stale one guarantees a
      // 409 on the user's next save.
      this.actor.set(a);
      this.version = a.version;
      this.goals.set(a.goals ?? []);
      this.referencedByUseCases.set(a.referencedByUseCases ?? []);
      this.referencedByStories.set(a.referencedByStories ?? []);
      if (!this.hasUnsavedChanges()) {
        this.actorName.set(a.name);
        this.detailsForm.reset({ name: a.name, text: a.text ?? '' });
      }
    } catch {
      if (skeleton) {
        this.loadError.set('Failed to load actor.');
      } else {
        this.showError('Failed to load actor.');
      }
    } finally {
      if (skeleton) {
        this.loading.set(false);
      }
    }
    if (this.actorId && !this.sseSub) {
      void this.eventStreamService.addSubscription('Actor', this.actorId);
      this.sseSub = this.eventStreamService.events$.subscribe(envelope => {
        if (envelope.targetType !== 'Actor' || envelope.targetId !== this.actorId) return;
        if (envelope.eventType === 'TargetDeleted') {
          this.announcer.announce('This actor was deleted in another session.');
          return;
        }
        const dirty = this.hasUnsavedChanges();
        void this.loadActor(false);
        if (dirty) {
          this.updateAvailable.set(true);
          this.announcer.announce('This actor was changed elsewhere. Your unsaved changes are preserved.');
        } else {
          this.announcer.announceThrottled('Actor:' + this.actorId, 'This actor was updated.');
        }
      });
    }
  }

  /**
   * Apply the merged container an association command returns (#180). Add/RemoveGoalFromGoalContainer
   * end by merging the container — which IS this actor — so each returns the actor with its bumped
   * `@Version` and refreshed goals as `result.entity`. Taking both from the response removes the
   * follow-up GET this used to do.
   *
   * Guarded on `version`: two associations can be in flight at once (e.g. two quick removes) and
   * their responses can arrive out of order. Since every successful merge increments `@Version`, a
   * response older than what we already hold would restore a stale goals list, so we ignore it.
   * A skipped version is self-correcting — the next save 409s into the existing recovery path.
   *
   * Never touches `detailsForm`, so an in-progress Name/Description edit survives.
   */
  private applyAssociationResult(entity: ActorDto | null): void {
    if (!entity) {
      return;
    }
    if (this.version != null && entity.version <= this.version) {
      return;
    }
    this.version = entity.version;
    this.goals.set(entity.goals ?? []);
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
   * re-read from `result.entity` each time. The form control and entity property are both `text`
   * while the DTO field is `description`; no server constraint targets that field today, so nothing
   * needs routing (#176 removed the old ACTOR_FIELD_MAP).
   */
  private async saveDetails(): Promise<CommandResult<unknown>> {
    this.saving.set(true);
    this.showError(null);
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
        const unresolved = applyCommandErrors(this.detailsForm, result.violations);
        if (unresolved.length) {
          this.showError(unresolved.join(SEPARATOR));
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
        await this.loadActor(false);
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
    await this.loadActor(false);
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
          this.showError(result.error ?? 'Copy failed.');
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
          this.showError(result.error ?? 'Delete failed.');
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
        this.applyAssociationResult(result.entity as ActorDto | null);
        this.messageService.add({ severity: 'success', summary: 'Goal added', detail: `"${goal.name}" added successfully.` });
        this.goalsSection?.announceAdded(goal.name);
      } else {
        this.showError(result.error ?? 'Failed to add goal.');
      }
    } catch {
      this.showError('Failed to add goal.');
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
        this.applyAssociationResult(result.entity as ActorDto | null);
        this.messageService.add({ severity: 'info', summary: 'Goal removed', detail: `"${goal.name}" removed.` });
        this.goalsSection?.announceRemoved(goal.name);
      } else {
        this.showError(result.error ?? 'Failed to remove goal.');
      }
    } catch {
      this.showError('Failed to remove goal.');
    }
  }

  onBack(): void {
    this.router.navigate(['/projects', this.projectName, 'actors']);
  }
}
