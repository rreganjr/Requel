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
import { Component, computed, OnDestroy, OnInit, signal, ViewChild, ChangeDetectionStrategy, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NgTemplateOutlet } from '@angular/common';
import { PageHeaderComponent } from '../../shared/page-header';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DirtyCheckable } from '../../core/dirty-check.guard';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import { UpdateBannerComponent } from '../../shared/app-update-banner';
import { AnnouncerService } from '../../core/announcer.service';
import { DialogModule } from 'primeng/dialog';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { CommandResult } from '../../models/command';
import { GoalDto, GoalRelationDto } from '../../models/goal';
import { EntityReferenceDto } from '../../models/entity-reference';
import { GoalService } from '../../core/goal.service';
import { CommandService, isNetworkError } from '../../core/command.service';
import { ProjectService } from '../../core/project.service';
import { PermissionService } from '../../core/permission.service';
import { EventStreamService } from '../../core/event-stream.service';
import { EntitySelectorDialogComponent } from '../../shared/entity-selector-dialog';
import { RelationshipSectionComponent } from '../../shared/app-relationship-section';
import { AnnotationsSectionComponent } from '../../shared/annotations-section';
import { TagSelectorComponent } from '../../shared/tag-selector';
import { AppCardComponent } from '../../shared/app-card';
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
  'This goal was changed elsewhere. Your copy has been refreshed — review the values and continue.';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-goal-editor',
  standalone: true,
  imports: [PageHeaderComponent, RouterLink, FormsModule, ReactiveFormsModule, NgTemplateOutlet,
            ButtonModule, InputText, TextareaModule, SelectModule,
            TableModule, SubmitErrorComponent, DialogModule, ConfirmDialogModule, EntitySelectorDialogComponent,
            RelationshipSectionComponent,
            AnnotationsSectionComponent, TagSelectorComponent, AppCardComponent, AppFieldComponent,
            AppFieldControlDirective, AppFormWizardComponent, AppWizardStepComponent,
            LoadingStateComponent, ErrorStateComponent, UpdateBannerComponent],
  providers: [ConfirmationService],
  template: `
    <div class="goal-editor" data-testid="goal-editor">
      <div class="page-header">
        <app-page-header [title]="isNew() ? 'New Goal' : goalName()" />
        <div class="page-actions">
          <p-button label="Back" icon="pi pi-arrow-left" severity="secondary" data-testid="goal-back"
                    [outlined]="true" (onClick)="onBack()" />
          @if (!isNew()) {
            @if (canEdit()) {
              <p-button label="Copy" icon="pi pi-copy" severity="secondary" data-testid="goal-copy"
                        [outlined]="true" (onClick)="onCopy()" />
            }
            @if (canDelete()) {
              <p-button label="Delete" icon="pi pi-trash" severity="danger" data-testid="goal-delete"
                        [outlined]="true" (onClick)="onDelete()" />
            }
          }
        </div>
      </div>

      <app-submit-error [message]="errorMessage()" testid="goal-editor-error" [retryable]="retryable()" (retry)="onSave()" />
      @if (updateAvailable()) {
        <app-update-banner message="This goal was changed elsewhere. Your unsaved changes are preserved."
                           testid="goal-update-banner"
                           (reload)="reloadFromExternalChange()" (dismiss)="updateAvailable.set(false)" />
      }

      @if (loading()) {
        <app-card>
          <app-loading-state label="Loading goal…" [lines]="4" testid="goal-editor-loading" />
        </app-card>
      } @else if (loadError()) {
        <app-error-state [message]="loadError()!" testid="goal-editor-load-error"
                         (retry)="retryLoad()" />
      } @else if (isNew()) {
        <!--
          Create runs as a wizard so Tags and Relations are reachable before the first
          save. Step 1 commits EditGoal on Continue, which is what gives steps 2 and 3
          the persisted goalId they need.
        -->
        <app-form-wizard
          [(activeKey)]="wizardStep"
          navLabel="New goal steps"
          (stepCommit)="onStepCommit($event)"
          (cancelled)="onBack()"
          (finished)="onWizardFinished()"
          data-testid="goal-wizard"
        >
          <app-wizard-step key="details" label="Details" helper="Name and description"
                           [form]="detailsForm">
            <ng-template>
              <ng-container [ngTemplateOutlet]="detailsFields" />
            </ng-template>
          </app-wizard-step>

          <app-wizard-step key="tags" label="Tags" helper="Categorise this goal" [optional]="true">
            <ng-template>
              <ng-container [ngTemplateOutlet]="tagsSection" />
            </ng-template>
          </app-wizard-step>

          <app-wizard-step key="relations" label="Relations" helper="Link to other goals"
                           [optional]="true">
            <ng-template>
              <!-- heading: false — the wizard panel's own h2 already reads "Relations". -->
              <ng-container [ngTemplateOutlet]="relationsSection"
                            [ngTemplateOutletContext]="{ heading: false }" />
            </ng-template>
          </app-wizard-step>
        </app-form-wizard>
      } @else {
        <app-card>
          <ng-container [ngTemplateOutlet]="detailsFields" />

          <div class="form-actions">
            <p-button label="Save" icon="pi pi-check" data-testid="goal-save"
                      [disabled]="!canSave()" [loading]="saving()" (onClick)="onSave()" />
          </div>
        </app-card>

        <ng-container [ngTemplateOutlet]="relationsSection"
                      [ngTemplateOutletContext]="{ heading: true }" />

        @if (goal()?.referencedBy?.length) {
          <div class="section">
            <!-- h2: these sections sit directly under the page's single h1. -->
            <h2 class="rq-section-title">Referenced By</h2>
            <p-table [value]="goal()!.referencedBy!" [rows]="10">
              <ng-template #header>
                <tr>
                  <th>Type</th>
                  <th>Name</th>
                </tr>
              </ng-template>
              <ng-template #body let-ref>
                <tr>
                  <td>{{ ref.entityType }}</td>
                  <td>{{ ref.name }}</td>
                </tr>
              </ng-template>
            </p-table>
          </div>
        }

        <ng-container [ngTemplateOutlet]="tagsSection" />
      }

      <!--
        Annotations render against a persisted entity, so they stay outside the wizard
        and appear once the goal exists rather than as a dead panel during create.
      -->
      @if (goalId != null) {
        <app-annotations-section
          [projectName]="projectName"
          entityType="Goal"
          [entityId]="goalId"
          [canEdit]="canEdit()" />
      }

      <!-- Add Relation Dialog -->
      <app-entity-selector-dialog
        [visible]="showRelationSelector"
        [projectName]="projectName"
        entityType="Goal"
        [excludeIds]="excludeGoalIds()"
        (selected)="onRelationGoalSelected($event)"
        (closed)="showRelationSelector = false" />

      <!-- Relation Type Dialog -->
      <p-dialog [visible]="relationDialogVisible()" (visibleChange)="relationDialogVisible.set($event)"
                (onHide)="onRelationDialogHide()" [modal]="true" [focusOnShow]="true" [dismissableMask]="true"
                closeAriaLabel="Close" [style]="{ width: '25rem' }" appendTo="body" [header]="relationDialogHeader()"
                data-testid="goal-relation-type-dialog">
        <div class="dialog-body">
          <p-select [(ngModel)]="newRelationType" data-testid="goal-relation-type-select" [options]="relationTypeOptions"
                    placeholder="Select relation type" appendTo="body" />
          <div class="dialog-actions">
            <p-button label="Add" icon="pi pi-check" data-testid="goal-relation-add" (onClick)="onConfirmRelation()" />
            <p-button label="Cancel" severity="secondary" [outlined]="true"
                      (onClick)="relationDialogVisible.set(false)" />
          </div>
        </div>
      </p-dialog>

      <p-confirmDialog />

      <!--
        Shared bodies. Each is used by both the wizard step and the edit view, so the
        two modes cannot drift apart. Controls bind with [formControl], not
        formControlName: these templates are projected into the wizard, where
        formControlName would look for a parent formGroup that is not there.
      -->
      <ng-template #detailsFields>
        <app-field label="Name" helper="Short and outcome-focused."
                   [control]="detailsForm.controls.name"
                   [errorMessages]="nameErrors"
                   [submitted]="submitted()">
          <input appFieldControl pInputText [formControl]="detailsForm.controls.name"
                 [attr.maxlength]="nameMaxLength"
                 placeholder="Goal name" data-testid="goal-name" />
        </app-field>

        <app-field label="Description" [control]="detailsForm.controls.text" [divider]="false"
                   [submitted]="submitted()">
          <textarea appFieldControl pTextarea [formControl]="detailsForm.controls.text" rows="6"
                    placeholder="Goal description" data-testid="goal-text"></textarea>
        </app-field>
      </ng-template>

      <ng-template #tagsSection>
        @if (goalId != null) {
          <app-tag-selector
            [projectName]="projectName"
            entityType="Goal"
            [entityId]="goalId"
            [canEdit]="canEdit()" />
        } @else {
          <p class="empty-text">Save the goal's details first to add tags.</p>
        }
      </ng-template>

      <ng-template #relationsSection let-heading="heading">
        <app-relationship-section
          title="This Goal's Relations" [showHeading]="heading"
          [items]="goal()?.relationsFromThisGoal ?? []" [headers]="['Goal', 'Type']"
          [canAdd]="canEdit() && goalId != null"
          addLabel="Add Relation" addTestid="goal-add-relation"
          removeTestid="goal-remove-relation" rowTestid="goal-relation-row" testid="goal-relations"
          emptyText="No relations defined."
          unsavedHint="Save the goal's details first to add relations."
          [removeAriaLabel]="relationRemoveAria" [trackBy]="relationTrackBy"
          (add)="showRelationSelector = true" (remove)="onDeleteRelation($event)">
          <ng-template #row let-r>
            <td><a class="entity-link" [routerLink]="['/projects', projectName, 'goals', r.goalId]">{{ r.goalName }}</a></td>
            <td>{{ r.relationType }}</td>
          </ng-template>
        </app-relationship-section>

        @if (goal()?.relationsToThisGoal?.length) {
          <div class="section">
            <h3>Related To This Goal</h3>
            <p-table [value]="goal()!.relationsToThisGoal!" [rows]="10">
              <ng-template #header>
                <tr>
                  <th>Goal</th>
                  <th>Type</th>
                </tr>
              </ng-template>
              <ng-template #body let-r>
                <tr>
                  <td><a class="entity-link" [routerLink]="['/projects', projectName, 'goals', r.goalId]">{{ r.goalName }}</a></td>
                  <td>{{ r.relationType }}</td>
                </tr>
              </ng-template>
            </p-table>
          </div>
        }
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
    .section h3 { margin: 1rem 0 0.5rem; }
    .empty-text { color: var(--p-text-secondary-color); font-style: italic; }
    .entity-link { cursor: pointer; color: var(--p-primary-color); text-decoration: underline; }
    .dialog-body { display: flex; flex-direction: column; }
    .dialog-actions { display: flex; gap: 0.5rem; justify-content: flex-end; margin-top: 1rem; }
  `]
})
export class GoalEditorComponent implements OnInit, OnDestroy, DirtyCheckable {
  isNew = signal(true);
  goalName = signal('');
  goal = signal<GoalDto | null>(null);
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
  /** A cross-session update arrived while the form was dirty (#140): show the reload banner. */
  updateAvailable = signal(false);
  pendingRelationGoal = signal<EntityReferenceDto | null>(null);
  relationDialogVisible = signal(false);
  relationDialogHeader = computed(() => {
    const ref = this.pendingRelationGoal();
    return ref ? `Relation to "${ref.name}"` : '';
  });

  @ViewChild(RelationshipSectionComponent) relationSection?: RelationshipSectionComponent<GoalRelationDto>;
  /** Accessible name for each relation's remove button. */
  relationRemoveAria = (r: GoalRelationDto): string => 'Remove relation to ' + r.goalName;
  /** Row identity for the relations list. */
  relationTrackBy = (r: GoalRelationDto): number => r.id;

  /**
   * Mirrors the backend `@Size(max = ValidationLimits.ARTIFACT_NAME_MAX)` (#171). Bound with
   * `[attr.maxlength]` rather than `maxlength` on purpose: Angular's MaxLengthValidator directive
   * matches `[maxlength][formControl]`, so the plain binding would register a SECOND maxlength
   * validator on top of the one in the form definition. `attr.` sets the HTML attribute only, which
   * is all that is wanted here — the browser stops the typing, the form owns the validation.
   */
  readonly nameMaxLength = ARTIFACT_NAME_MAX_LENGTH;

  /** Details step / edit form. Replaces the previous `name` + `text` ngModel fields. */
  readonly detailsForm = new FormGroup({
    name: new FormControl('', {
      validators: [Validators.required, Validators.maxLength(ARTIFACT_NAME_MAX_LENGTH)],
      nonNullable: true,
    }),
    text: new FormControl('', { nonNullable: true }),
  });

  readonly nameErrors = { required: 'A goal needs a name.' };

  /** Active wizard step key, two-way bound to `app-form-wizard`. */
  wizardStep = 'details';

  showRelationSelector = false;
  newRelationType = 'Supports';
  relationTypeOptions = [
    { label: 'Supports', value: 'Supports' },
    { label: 'Conflicts', value: 'Conflicts' }
  ];

  projectName = '';
  goalId: number | null = null;
  private version: number | null = null;
  private readonly destroyRef = inject(DestroyRef);
  private sseBound = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private goalService: GoalService,
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
      const idParam = params.get('goalId') ?? '';
      const newIsNew = idParam === 'new';

      // Reset the form synchronously for the new-goal path BEFORE the
      // loadForProject await. Otherwise typing during the yield would later
      // be clobbered by the reset and Angular change-detection would clear
      // the input. See term-editor.ts for the same pattern.
      if (newIsNew) {
        this.isNew.set(true);
        this.goal.set(null);
        this.goalId = null;
        this.version = null;
        this.wizardStep = 'details';
        this.submitted.set(false);
        this.detailsForm.reset({ name: '', text: '' });
        // Nothing to load, so resolve the gate synchronously (#185) - otherwise the create wizard
        // would sit behind the skeleton forever. Same reason the reset above is synchronous.
        this.loading.set(false);
        this.loadError.set(null);
      }

      await this.permissionService.loadForProject(this.projectName);
      this.canEdit.set(this.permissionService.canEdit('Goal'));
      this.canDelete.set(this.permissionService.canDelete('Goal'));

      if (!newIsNew) {
        this.isNew.set(false);
        this.goalId = +idParam;
        this.loadGoal();
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
    if (this.goalId) {
      void this.eventStreamService.removeSubscription('Goal', this.goalId);
    }
  }

  /**
   * Reads the goal and applies it in two parts: server state always, form state only when the
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
   * `goal` stays unconditional because the relations tables render from it, so the refresh after
   * adding or deleting a relation still lands even with a rename sitting in the Name field.
   * `goalName` is the *persisted* name used to address relation commands, so it deliberately
   * moves only with the form.
   */
  /** Re-run the initial load; wired to the error state's (retry) output. */
  retryLoad(): void {
    void this.loadGoal();
  }

  /** Discard local edits and re-apply the latest server state (from the update banner, #140). */
  async reloadFromExternalChange(): Promise<void> {
    this.updateAvailable.set(false);
    this.detailsForm.markAsPristine();
    await this.loadGoal(false);
    this.announcer.announce('Goal reloaded.');
  }

  /**
   * @param skeleton show the loading skeleton and the retryable error state. Suppressed for every
   *                 background caller - SSE refresh, post-save refetch, 409 recovery and the
   *                 relation-table refreshes - where blanking the form the user is looking at
   *                 would be worse than a stale moment, and where a failure belongs in the inline
   *                 message rather than in place of the form. Mirrors `scenario-editor`.
   */
  private async loadGoal(skeleton = true): Promise<void> {
    if (skeleton) {
      this.loading.set(true);
      this.loadError.set(null);
    }
    try {
      const g = await this.goalService.getGoal(this.projectName, this.goalId!);
      // Always take the version. The entity moved on, and holding the stale one guarantees a
      // 409 on the user's next save.
      this.goal.set(g);
      this.version = g.version;
      if (!this.hasUnsavedChanges()) {
        this.goalName.set(g.name);
        this.detailsForm.reset({ name: g.name, text: g.text });
      }
    } catch {
      if (skeleton) {
        this.loadError.set('Failed to load goal.');
      } else {
        this.showError('Failed to load goal.');
      }
    } finally {
      if (skeleton) {
        this.loading.set(false);
      }
    }
    if (this.goalId && !this.sseBound) {
      void this.eventStreamService.addSubscription('Goal', this.goalId);
      this.sseBound = true;
      this.eventStreamService.events$
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(envelope => {
        if (envelope.targetType !== 'Goal' || envelope.targetId !== this.goalId) return;
        if (envelope.eventType === 'TargetDeleted') {
          this.announcer.announce('This goal was deleted in another session.');
          return;
        }
        // Server suppresses self-echo (X-Session-Id), so this is a cross-session change.
        const dirty = this.hasUnsavedChanges();
        void this.loadGoal(false);
        if (dirty) {
          this.updateAvailable.set(true);
          this.announcer.announce('This goal was changed elsewhere. Your unsaved changes are preserved.');
        } else {
          this.announcer.announceThrottled('Goal:' + this.goalId, 'This goal was updated.');
        }
      });
    }
  }

  excludeGoalIds(): number[] {
    const ids: number[] = [];
    if (this.goalId) ids.push(this.goalId);
    const g = this.goal();
    if (g?.relationsFromThisGoal) {
      ids.push(...g.relationsFromThisGoal.map(r => r.goalId));
    }
    return ids;
  }

  /**
   * Runs the commit for the wizard's current step.
   *
   * Only Details talks to the API — Tags and Relations commit through their own
   * widgets as the user works, so their Continue just advances.
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

  /** Done on the last step: the goal is already saved, so just go to it. */
  onWizardFinished(): void {
    if (this.goalId != null) {
      this.router.navigate(['/projects', this.projectName, 'goals', this.goalId]);
    } else {
      this.onBack();
    }
  }

  /**
   * Issues `EditGoal` for the Details values and, on success, adopts the id and
   * version from the response.
   *
   * The version is **spent on use**: every accepted `EditGoal` bumps it server-side,
   * so it is re-read from `result.entity` each time. Holding the value captured at
   * create and sending it again — which is what happens if the user steps back to
   * Details and presses Continue a second time — is a guaranteed 409.
   */
  private async saveDetails(): Promise<CommandResult<unknown>> {
    this.saving.set(true);
    this.showError(null);
    try {
      const { name, text } = this.detailsForm.getRawValue();
      const input: Record<string, unknown> = { projectName: this.projectName, name, text };
      if (this.goalId != null) input['goalId'] = this.goalId;
      if (this.version != null) input['version'] = this.version;

      const result = await this.commandService.execute('EditGoal', input);
      if (!result.success) {
        return result;
      }

      const wasCreate = this.goalId == null;
      if (wasCreate) {
        this.projectService.notifyTreeChanged();
      }

      const saved = result.entity as GoalDto | null;
      if (saved) {
        this.goalId = saved.id;
        this.version = saved.version;
        this.goalName.set(saved.name);
      }
      this.detailsForm.markAsPristine();
      this.messageService.add({ severity: 'success', summary: 'Saved', detail: 'Goal saved.' });

      // Hydrate relations / referencedBy (and start the SSE subscription) the first
      // time the goal exists, so steps 2 and 3 have something to render.
      if (wasCreate && this.goalId != null) {
        await this.loadGoal(false);
      }
      return result;
    } catch {
      return {
        success: false,
        entityType: 'Goal',
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
    if (result.status !== 409 || this.goalId == null) {
      return false;
    }
    await this.loadGoal(false);
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
      message: 'Create a copy of this goal?',
      accept: async () => {
        const result = await this.commandService.execute('CopyGoal', {
          projectName: this.projectName,
          goalId: this.goalId
        });
        if (result.success && result.entity) {
          this.projectService.notifyTreeChanged();
          const copy = result.entity as GoalDto;
          this.router.navigate(['/projects', this.projectName, 'goals', copy.id]);
        } else {
          this.showError(result.error ?? 'Copy failed.');
        }
      }
    });
  }

  onDelete(): void {
    this.confirmationService.confirm({
      message: 'Are you sure you want to delete this goal?',
      accept: async () => {
        const result = await this.commandService.execute('DeleteGoal', {
          projectName: this.projectName,
          goalId: this.goalId,
          version: this.version
        });
        if (result.success) {
          this.projectService.notifyTreeChanged();
          // Nothing left to guard against — don't let the dirty check block the exit.
          this.detailsForm.markAsPristine();
          this.router.navigate(['/projects', this.projectName, 'goals']);
        } else {
          this.showError(result.error ?? 'Delete failed.');
        }
      }
    });
  }

  onRelationGoalSelected(ref: EntityReferenceDto): void {
    this.pendingRelationGoal.set(ref);
    this.newRelationType = 'Supports';
    this.relationDialogVisible.set(true);
  }

  /**
   * Called after the relation-type dialog closes by any path (Add, Cancel, Escape, mask).
   * Clears the pending goal and restores focus to the "Add Relation" opener — the dialog is
   * opened programmatically after the entity selector closes, so PrimeNG's default
   * restore-to-last-focused-element has no stable target here.
   */
  onRelationDialogHide(): void {
    this.pendingRelationGoal.set(null);
    this.relationSection?.focusAdd();
  }

  async onConfirmRelation(): Promise<void> {
    const ref = this.pendingRelationGoal();
    if (!ref) return;
    this.relationDialogVisible.set(false);

    const result = await this.commandService.execute('EditGoalRelation', {
      projectName: this.projectName,
      // The persisted name, not the form value: an unsaved rename in the Name field
      // would otherwise be sent as the relation's from-goal and not resolve.
      fromGoalName: this.goalName(),
      toGoalName: ref.name,
      relationType: this.newRelationType
    });
    if (result.success) {
      this.messageService.add({ severity: 'success', summary: 'Relation added', detail: 'Goal relation added.' });
      await this.loadGoal(false);
      this.relationSection?.announceAdded(ref.name);
    } else {
      this.showError(result.error ?? 'Failed to add relation.');
    }
  }

  async onDeleteRelation(relation: GoalRelationDto): Promise<void> {
    const result = await this.commandService.execute('DeleteGoalRelation', {
      projectName: this.projectName,
      goalRelationId: relation.id,
      version: relation.version
    });
    if (result.success) {
      this.messageService.add({ severity: 'success', summary: 'Relation removed', detail: 'Goal relation removed.' });
      await this.loadGoal(false);
      this.relationSection?.announceRemoved(relation.goalName);
    } else {
      this.showError(result.error ?? 'Failed to delete relation.');
    }
  }

  onBack(): void {
    this.router.navigate(['/projects', this.projectName, 'goals']);
  }
}
