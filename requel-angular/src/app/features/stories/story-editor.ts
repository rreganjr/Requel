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
import { Component, OnDestroy, OnInit, signal, ViewChild, ChangeDetectionStrategy, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NgTemplateOutlet } from '@angular/common';
import { PageHeaderComponent } from '../../shared/page-header';
import { AppCardComponent } from '../../shared/app-card';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DirtyCheckable } from '../../core/dirty-check.guard';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { SelectModule } from 'primeng/select';
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import { UpdateBannerComponent } from '../../shared/app-update-banner';
import { AnnouncerService } from '../../core/announcer.service';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { CommandResult } from '../../models/command';
import { StoryDto } from '../../models/story';
import { EntityReferenceDto } from '../../models/entity-reference';
import { StoryService } from '../../core/story.service';
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
import { ARTIFACT_NAME_MAX_LENGTH } from '../../shared/validation-limits';

/** Wording for the stale-version recovery path, so the 409 case reads as recoverable. */
const STALE_VERSION_MESSAGE =
  'This story was changed elsewhere. Your copy has been refreshed — review the values and continue.';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-story-editor',
  standalone: true,
  imports: [PageHeaderComponent, AppCardComponent, RouterLink, ReactiveFormsModule, NgTemplateOutlet,
            ButtonModule, InputText, TextareaModule, SelectModule,
            SubmitErrorComponent, UpdateBannerComponent, ConfirmDialogModule, EntitySelectorDialogComponent,
            RelationshipSectionComponent,
            AnnotationsSectionComponent, AppFieldComponent, AppFieldControlDirective,
            AppFormWizardComponent, AppWizardStepComponent,
            LoadingStateComponent, ErrorStateComponent],
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

      <app-submit-error [message]="errorMessage()" testid="story-editor-error" [retryable]="retryable()" (retry)="onSave()" />
      @if (updateAvailable()) {
        <app-update-banner message="This story was changed elsewhere. Your unsaved changes are preserved."
                           testid="story-update-banner"
                           (reload)="reloadFromExternalChange()" (dismiss)="updateAvailable.set(false)" />
      }

      @if (loading()) {
        <app-card>
          <app-loading-state label="Loading story…" [lines]="4" testid="story-editor-loading" />
        </app-card>
      } @else if (loadError()) {
        <app-error-state [message]="loadError()!" testid="story-editor-load-error"
                         (retry)="retryLoad()" />
      } @else if (isNew()) {
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
        <app-relationship-section #storyGoalsSection
          title="Goals" [showHeading]="heading"
          [items]="story()?.goals ?? []" [headers]="['Name']"
          [canAdd]="canEdit() && storyId != null"
          addLabel="Add Goal" addTestid="story-add-goal"
          removeTestid="story-remove-goal" rowTestid="story-goal-row" testid="story-goals"
          emptyText="No goals associated."
          unsavedHint="Save the story's details first to add goals."
          [removeAriaLabel]="goalRemoveAria" [trackBy]="refTrackBy"
          (add)="showGoalSelector = true" (remove)="onRemoveGoal($event)">
          <ng-template #row let-g>
            <td><a class="entity-link" data-testid="story-goal-link" [routerLink]="['/projects', projectName, 'goals', g.id]">{{ g.name }}</a></td>
          </ng-template>
        </app-relationship-section>
      </ng-template>

      <ng-template #actorsSection let-heading="heading">
        <app-relationship-section #storyActorsSection
          title="Additional Actors" [showHeading]="heading"
          [items]="story()?.actors ?? []" [headers]="['Name']"
          [canAdd]="canEdit() && storyId != null"
          addLabel="Add Actor" addTestid="story-add-actor"
          removeTestid="story-remove-additional-actor" rowTestid="story-additional-actor-row" testid="story-additional-actors"
          emptyText="No actors associated."
          unsavedHint="Save the story's details first to add actors."
          [removeAriaLabel]="actorRemoveAria" [trackBy]="refTrackBy"
          (add)="showActorSelector = true" (remove)="onRemoveActor($event)">
          <ng-template #row let-a>
            <td><a class="entity-link" data-testid="story-additional-actor-link" [routerLink]="['/projects', projectName, 'actors', a.id]">{{ a.name }}</a></td>
          </ng-template>
        </app-relationship-section>
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
  private readonly destroyRef = inject(DestroyRef);
  private sseBound = false;

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
    private eventStreamService: EventStreamService,
    private announcer: AnnouncerService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(async params => {
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
        // Nothing to load, so resolve the gate synchronously (#185) - otherwise the create wizard
        // would sit behind the skeleton forever. Same reason the reset above is synchronous.
        this.loading.set(false);
        this.loadError.set(null);
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

  /** A cross-session update arrived while the form was dirty (#140): show the reload banner. */
  updateAvailable = signal(false);

  /** Discard local edits and re-apply the latest server state (from the update banner, #140). */
  async reloadFromExternalChange(): Promise<void> {
    this.updateAvailable.set(false);
    this.detailsForm.markAsPristine();
    await this.loadStory(false);
    this.announcer.announce('Story reloaded.');
  }

  hasUnsavedChanges(): boolean {
    return this.detailsForm.dirty;
  }

  /** Edit-mode Save: blocked on invalid, unchanged, or in-flight. */
  canSave(): boolean {
    return this.detailsForm.valid && this.detailsForm.dirty && !this.saving();
  }

  ngOnDestroy(): void {
    if (this.storyId) {
      void this.eventStreamService.removeSubscription('Story', this.storyId);
    }
  }

  /**
   * Reads the story and applies it in two parts: server state always, form state only when the
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
   * Server state stays unconditional so a refresh triggered while the form is dirty still
   * refreshes what it was called for.
   */
  /** Re-run the initial load; wired to the error state's (retry) output. */
  retryLoad(): void {
    void this.loadStory();
  }

  /**
   * @param skeleton show the loading skeleton and the retryable error state. Suppressed for every
   *                 background caller - SSE refresh, post-save refetch and 409 recovery - where
   *                 blanking the form the user is looking at would be worse than a stale moment,
   *                 and where a failure belongs in the inline message rather than in place of the
   *                 form. Mirrors `scenario-editor`.
   */
  private async loadStory(skeleton = true): Promise<void> {
    if (skeleton) {
      this.loading.set(true);
      this.loadError.set(null);
    }
    try {
      const s = await this.storyService.getStory(this.projectName, this.storyId!);
      // Always take the version. The entity moved on, and holding the stale one guarantees a
      // 409 on the next save.
      this.story.set(s);
      this.version = s.version;
      if (!this.hasUnsavedChanges()) {
        this.storyName.set(s.name);
        this.detailsForm.reset({
          name: s.name,
          storyType: s.storyType,
          primaryActorName: s.primaryActorName ?? '',
          text: s.text,
        });
      }
    } catch {
      if (skeleton) {
        this.loadError.set('Failed to load story.');
      } else {
        this.showError('Failed to load story.');
      }
    } finally {
      if (skeleton) {
        this.loading.set(false);
      }
    }
    if (this.storyId && !this.sseBound) {
      void this.eventStreamService.addSubscription('Story', this.storyId);
      this.sseBound = true;
      this.eventStreamService.events$
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(envelope => {
        if (envelope.targetType !== 'Story' || envelope.targetId !== this.storyId) return;
        if (envelope.eventType === 'TargetDeleted') {
          this.announcer.announce('This story was deleted in another session.');
          return;
        }
        const dirty = this.hasUnsavedChanges();
        void this.loadStory(false);
        if (dirty) {
          this.updateAvailable.set(true);
          this.announcer.announce('This story was changed elsewhere. Your unsaved changes are preserved.');
        } else {
          this.announcer.announceThrottled('Story:' + this.storyId, 'This story was updated.');
        }
      });
    }
  }

  /**
   * Apply the merged story an association command returns (#180). Add/Remove Goal/Actor merge the
   * container — this story — so each returns it with its bumped `@Version` and refreshed goals /
   * actors as `result.entity`. Consuming that removes the follow-up GET this used to do.
   *
   * Guarded on `version`: two associations can be in flight at once (easiest by clicking two remove
   * buttons) and resolve out of order. Since every merge increments `@Version`, a response older
   * than what we already hold would restore a stale list, so we ignore it — this replaces the old
   * `storyReadSeq` ticket and keys off the server's commit order rather than client issue order. A
   * skipped version self-corrects through the next-save 409 recovery.
   *
   * Never touches `detailsForm`, so an in-progress edit survives.
   */
  private applyAssociationResult(entity: StoryDto | null): void {
    if (!entity) {
      return;
    }
    if (this.version != null && entity.version <= this.version) {
      return;
    }
    this.version = entity.version;
    this.story.set(entity);
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
    this.showError(null);
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
        await this.loadStory(false);
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
    await this.loadStory(false);
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
          this.showError(result.error ?? 'Copy failed.');
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
          this.showError(result.error ?? 'Delete failed.');
        }
      }
    });
  }

  @ViewChild('storyGoalsSection') storyGoalsSection?: RelationshipSectionComponent<EntityReferenceDto>;
  @ViewChild('storyActorsSection') storyActorsSection?: RelationshipSectionComponent<EntityReferenceDto>;
  /** Accessible names + row identity for the relationship lists. */
  goalRemoveAria = (g: EntityReferenceDto): string => 'Remove goal ' + g.name;
  actorRemoveAria = (a: EntityReferenceDto): string => 'Remove actor ' + a.name;
  refTrackBy = (x: EntityReferenceDto) => x.id;

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
        this.applyAssociationResult(result.entity as StoryDto | null);
        this.messageService.add({ severity: 'success', summary: 'Goal added', detail: 'Goal added.' });
        this.storyGoalsSection?.announceAdded(ref.name);
      } else {
        this.showError(result.error ?? 'Failed to add goal.');
      }
    } catch {
      this.showError('Failed to add goal.');
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
        this.applyAssociationResult(result.entity as StoryDto | null);
        this.messageService.add({ severity: 'success', summary: 'Goal removed', detail: 'Goal removed.' });
        this.storyGoalsSection?.announceRemoved(goalRef.name);
      } else {
        this.showError(result.error ?? 'Failed to remove goal.');
      }
    } catch {
      this.showError('Failed to remove goal.');
    }
  }

  async onActorSelected(ref: EntityReferenceDto): Promise<void> {
    this.showActorSelector = false;
    try {
      const result = await this.commandService.execute('AddActorToActorContainer', {
        projectName: this.projectName,
        actorContainerId: this.storyId,
        actorId: ref.id,
        containerType: 'Story'
      });
      if (result.success) {
        this.applyAssociationResult(result.entity as StoryDto | null);
        this.messageService.add({ severity: 'success', summary: 'Actor added', detail: 'Actor added.' });
        this.storyActorsSection?.announceAdded(ref.name);
      } else {
        this.showError(result.error ?? 'Failed to add actor.');
      }
    } catch {
      this.showError('Failed to add actor.');
    }
  }

  async onRemoveActor(actorRef: EntityReferenceDto): Promise<void> {
    try {
      const result = await this.commandService.execute('RemoveActorFromActorContainer', {
        projectName: this.projectName,
        actorContainerId: this.storyId,
        actorId: actorRef.id,
        containerType: 'Story'
      });
      if (result.success) {
        this.applyAssociationResult(result.entity as StoryDto | null);
        this.messageService.add({ severity: 'success', summary: 'Actor removed', detail: 'Actor removed.' });
        this.storyActorsSection?.announceRemoved(actorRef.name);
      } else {
        this.showError(result.error ?? 'Failed to remove actor.');
      }
    } catch {
      this.showError('Failed to remove actor.');
    }
  }

  onBack(): void {
    this.router.navigate(['/projects', this.projectName, 'stories']);
  }
}
