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
import { ActivatedRoute, Router } from '@angular/router';
import { NgTemplateOutlet } from '@angular/common';
import { Subscription } from 'rxjs';
import { DirtyCheckable } from '../../core/dirty-check.guard';
import { FormControl, FormGroup, FormRecord, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { SelectModule } from 'primeng/select';
import { CheckboxModule } from 'primeng/checkbox';
import { TableModule } from 'primeng/table';
import { MessageModule } from 'primeng/message';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { StakeholderDto, StakeholderPermissionDto, UserStakeholderDetails } from '../../models/stakeholder';
import { EntityReferenceDto } from '../../models/entity-reference';
import { StakeholderService } from '../../core/stakeholder.service';
import { CommandService } from '../../core/command.service';
import { ProjectService } from '../../core/project.service';
import { UserService } from '../../core/user.service';
import { PermissionService } from '../../core/permission.service';
import { EventStreamService } from '../../core/event-stream.service';
import { EntitySelectorDialogComponent } from '../../shared/entity-selector-dialog';
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
  'This stakeholder was changed elsewhere. Your copy has been refreshed - review the values and continue.';

interface PermissionGroup {
  entityType: string;
  permissions: { key: string; type: string; checked: boolean }[];
}

@Component({
  selector: 'app-stakeholder-editor',
  standalone: true,
  imports: [PageHeaderComponent, AppCardComponent, NgTemplateOutlet, ReactiveFormsModule,
            ButtonModule, InputText, TextareaModule, SelectModule,
            CheckboxModule, MessageModule, ConfirmDialogModule, TableModule,
            EntitySelectorDialogComponent, AppFieldComponent, AppFieldControlDirective,
            AppFormWizardComponent, AppWizardStepComponent,
            LoadingStateComponent, ErrorStateComponent],
  providers: [ConfirmationService],
  template: `
    <div class="stakeholder-editor">
      <div class="page-header">
        <app-page-header [title]="isNew() ? (isUserType() ? 'Add User Stakeholder' : 'Add Non-User Stakeholder') : stakeholderName()" />
        <div class="page-actions">
          <p-button label="Back" icon="pi pi-arrow-left" severity="secondary"
                    [outlined]="true" (onClick)="onBack()" />
          @if (canDelete() && !isNew()) {
            <p-button label="Delete" icon="pi pi-trash" severity="danger"
                      [outlined]="true" (onClick)="onDelete()" />
          }
        </div>
      </div>

      @if (errorMessage()) {
        <p-message severity="error" [text]="errorMessage()!" />
      }

      @if (loading()) {
        <app-card>
          <app-loading-state label="Loading stakeholder…" [lines]="4" testid="stakeholder-editor-loading" />
        </app-card>
      } @else if (loadError()) {
        <app-error-state [message]="loadError()!" testid="stakeholder-editor-load-error"
                         (retry)="retryLoad()" />
      } @else if (isNew()) {
        <!--
          Create runs as a wizard (#173) so Goals is reachable before the first save. The User
          select stays on step 1 and stays [disabled]="!isNew()" - it is the mode selector, and
          EditUserStakeholder is keyed by username, so it can never change after creation.
        -->
        <app-form-wizard
          [(activeKey)]="wizardStep"
          navLabel="New stakeholder steps"
          (stepCommit)="onStepCommit($event)"
          (cancelled)="onBack()"
          (finished)="onWizardFinished()"
          data-testid="stakeholder-wizard"
        >
          <app-wizard-step key="details" label="Details"
                           [helper]="isUserType() ? 'User, team and permissions' : 'Name and description'"
                           [form]="detailsForm">
            <ng-template>
              <ng-container [ngTemplateOutlet]="detailsFields" />
            </ng-template>
          </app-wizard-step>

          <app-wizard-step key="goals" label="Goals" helper="Link goals to this stakeholder"
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

          <div class="form-actions">
            <p-button label="Save" icon="pi pi-check" data-testid="stakeholder-save"
                      [disabled]="!canSave()" [loading]="saving()" (onClick)="onSave()" />
          </div>
        </app-card>

        <ng-container [ngTemplateOutlet]="goalsSection"
                      [ngTemplateOutletContext]="{ heading: true }" />
      }

      <app-entity-selector-dialog
        [visible]="showGoalSelector"
        [projectName]="projectName"
        entityType="Goal"
        [excludeIds]="goalIds()"
        (selected)="onGoalSelected($event)"
        (closed)="showGoalSelector = false" />

      <p-confirmDialog />

      <!--
        Shared bodies, used by both the wizard step and the edit view so the two cannot drift.
        Controls bind [formControl], not formControlName: these are projected into the wizard,
        where formControlName would look for a parent formGroup that is not there.
      -->
      <ng-template #detailsFields>
        @if (isUserType()) {
          <app-field label="User" controlId="stakeholderUserInput"
                     [control]="detailsForm.controls.username"
                     [errorMessages]="usernameErrors" [submitted]="submitted()">
            <p-select appFieldControl inputId="stakeholderUserInput" data-testid="stakeholder-user"
                      [formControl]="detailsForm.controls.username" [options]="userOptions()"
                      optionLabel="label" optionValue="value"
                      placeholder="Select a user" [filter]="true" />
          </app-field>

          @if (loadedUserDetails(); as ud) {
            <app-field label="Email">
              <span class="readonly-field">{{ ud.emailAddress || '—' }}</span>
            </app-field>
            <app-field label="Phone">
              <span class="readonly-field">{{ ud.phoneNumber || '—' }}</span>
            </app-field>
          }

          <app-field label="Team" controlId="team" [control]="detailsForm.controls.teamName"
                     [divider]="permissionGroups().length === 0" [submitted]="submitted()">
            <input appFieldControl pInputText [formControl]="detailsForm.controls.teamName" id="team"
                   placeholder="Team name" data-testid="stakeholder-team" />
          </app-field>

          @if (permissionGroups().length > 0) {
            <div class="permissions-section">
              <!-- h3 under the card/panel heading, not a page-level section title. -->
              <h3 id="stakeholder-permissions-heading">Permissions</h3>
              <div class="permission-grid" role="group"
                   aria-labelledby="stakeholder-permissions-heading">
                <div class="permission-header"></div>
                <div class="permission-header">Edit</div>
                <div class="permission-header">Delete</div>
                <div class="permission-header">Grant</div>
                @for (group of permissionGroups(); track group.entityType) {
                  <div class="permission-entity">{{ group.entityType }}</div>
                  @for (type of permissionTypes; track type) {
                    <div class="permission-check">
                      @if (getPermission(group, type); as perm) {
                        <p-checkbox [formControl]="permissionControl(perm.key)" [binary]="true"
                                    [inputId]="'perm-' + perm.key"
                                    [attr.data-testid]="'stakeholder-perm-' + perm.key" />
                        <!--
                          Each checkbox needs its own name; the column header alone is not an
                          accessible name. Visually hidden so the grid still reads as a matrix.
                        -->
                        <label class="rq-visually-hidden" [attr.for]="'perm-' + perm.key">
                          {{ type }} {{ group.entityType }}
                        </label>
                      }
                    </div>
                  }
                }
              </div>
            </div>
          }
        } @else {
          <app-field label="Name" controlId="name" [control]="detailsForm.controls.name"
                     [errorMessages]="nameErrors" [submitted]="submitted()">
            <input appFieldControl pInputText [formControl]="detailsForm.controls.name" id="name"
                   [attr.maxlength]="nameMaxLength"
                   placeholder="Stakeholder name" data-testid="stakeholder-name" />
          </app-field>

          <app-field label="Description" controlId="text" [control]="detailsForm.controls.text" [divider]="false"
                     [submitted]="submitted()">
            <textarea appFieldControl pTextarea [formControl]="detailsForm.controls.text" id="text" rows="4"
                      placeholder="Description of this stakeholder"
                      data-testid="stakeholder-text"></textarea>
          </app-field>
        }
      </ng-template>

      <ng-template #goalsSection let-heading="heading">
        <div class="section">
          <div class="section-header">
            @if (heading) {
              <h2 class="rq-section-title">Goals</h2>
            }
            @if (canEditGoals() && stakeholderId != null) {
              <p-button label="Add Goal" icon="pi pi-plus" size="small"
                        data-testid="stakeholder-add-goal"
                        [text]="true" (onClick)="showGoalSelector = true" />
            }
          </div>

          @if (stakeholderId == null) {
            <p class="empty-text">Save the stakeholder's details first to add goals.</p>
          } @else {
            <p-table [value]="goals()" [rowHover]="true">
              <ng-template #header>
                <tr>
                  <th>Name</th>
                  @if (canEditGoals()) {
                    <!-- An empty <th> is an axe empty-table-header violation. -->
                    <th class="col-actions"><span class="rq-visually-hidden">Actions</span></th>
                  }
                </tr>
              </ng-template>
              <ng-template #body let-g>
                <tr>
                  <td class="entity-link" (click)="onGoalClick(g)">{{ g.name }}</td>
                  @if (canEditGoals()) {
                    <td>
                      <p-button icon="pi pi-times" severity="danger" [text]="true"
                                data-testid="stakeholder-remove-goal"
                                [ariaLabel]="'Remove goal ' + g.name"
                                size="small" (onClick)="onRemoveGoal(g)" />
                    </td>
                  }
                </tr>
              </ng-template>
              <ng-template #emptymessage>
                <tr><td [attr.colspan]="canEditGoals() ? 2 : 1" class="empty-text">No goals assigned.</td></tr>
              </ng-template>
            </p-table>
          }
        </div>
      </ng-template>
    </div>
  `,
  styles: [`
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
    .page-actions { display: flex; gap: 0.5rem; }
    .readonly-field { color: var(--p-text-color); padding: 0.5rem 0; }
    .form-actions { margin-top: 1rem; }
    .permissions-section { grid-column: 1 / -1; margin-top: 0.5rem; }
    .permissions-section h3 { margin: 0 0 0.5rem 0; font-size: 1rem; }
    .permission-grid { display: grid; grid-template-columns: 140px repeat(3, 60px); gap: 0.25rem 0.5rem; align-items: center; }
    .permission-header { font-weight: 600; font-size: 0.85rem; text-align: center; }
    .permission-entity { font-size: 0.9rem; }
    .permission-check { text-align: center; }
    .section { margin-top: 1.5rem; }
    .section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem; }
    .section-header h3 { margin: 0; }
    .entity-link { cursor: pointer; color: var(--p-primary-color); text-decoration: underline; }
    .empty-text { color: var(--p-text-secondary-color); font-style: italic; }
  `]
})
export class StakeholderEditorComponent implements OnInit, OnDestroy, DirtyCheckable {
  isNew = signal(true);
  isUserType = signal(true);
  stakeholderName = signal('');
  errorMessage = signal<string | null>(null);
  /**
   * #185. The edit form renders only once the detail GET resolves, so there is no window in which
   * a user can type into a form the load is about to reset. Starts true: an edit route is loading
   * from the first frame, and the create path clears it as soon as it knows there is nothing to
   * load.
   */
  loading = signal(true);
  loadError = signal<string | null>(null);
  saving = signal(false);
  canDelete = signal(false);
  userOptions = signal<{ label: string; value: string }[]>([]);
  loadedUserDetails = signal<UserStakeholderDetails | null>(null);
  permissionGroups = signal<PermissionGroup[]>([]);
  goals = signal<EntityReferenceDto[]>([]);
  goalIds = computed(() => this.goals().map(g => g.id).filter((id): id is number => id != null));
  submitted = signal(false);

  showGoalSelector = false;

  /** Column order of the permission matrix; was an inline array literal in the template. */
  readonly permissionTypes = ['Edit', 'Delete', 'Grant'];

  /** Mirrors the backend `@Size(max = ValidationLimits.ARTIFACT_NAME_MAX)` (#171). */
  readonly nameMaxLength = ARTIFACT_NAME_MAX_LENGTH;

  /**
   * Details step / edit form for BOTH modes. The irrelevant half is disabled rather than
   * omitted: disabled controls are excluded from `value` and from validity, so one form can
   * drive the wizard's `[form]` binding whichever mode the route asked for, and `getRawValue`
   * still reaches whatever a mode needs.
   */
  readonly detailsForm = new FormGroup({
    username: new FormControl('', { validators: [Validators.required], nonNullable: true }),
    teamName: new FormControl('', { nonNullable: true }),
    name: new FormControl('', {
      validators: [Validators.required, Validators.maxLength(ARTIFACT_NAME_MAX_LENGTH)],
      nonNullable: true,
    }),
    text: new FormControl('', { nonNullable: true }),
  });

  /**
   * One boolean control per permission key, rebuilt whenever the matrix loads. Keeping these in
   * a form rather than mutating `perm.checked` is what lets `hasUnsavedChanges()` be pure form
   * state instead of the old string-join comparison against `originalPermissionKeys`.
   */
  readonly permissionsForm = new FormRecord<FormControl<boolean>>({});

  readonly nameErrors = { required: 'A stakeholder needs a name.' };
  readonly usernameErrors = { required: 'Select a user.' };

  /** Active wizard step key, two-way bound to `app-form-wizard`. */
  wizardStep = 'details';

  projectName = '';
  stakeholderId: number | null = null;
  private version: number | null = null;
  private paramSub?: Subscription;
  private sseSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private stakeholderService: StakeholderService,
    private commandService: CommandService,
    private projectService: ProjectService,
    private userService: UserService,
    private permissionService: PermissionService,
    private confirmationService: ConfirmationService,
    private messageService: MessageService,
    private eventStreamService: EventStreamService
  ) {}

  ngOnInit(): void {
    this.paramSub = this.route.paramMap.subscribe(async params => {
      this.projectName = params.get('name') ?? '';
      await this.permissionService.loadForProject(this.projectName);
      this.canDelete.set(this.permissionService.canDelete('Stakeholder'));

      const idParam = params.get('stakeholderId') ?? '';
      if (idParam === 'new-user') {
        this.resetForCreate(true);
        this.loadUsers();
        this.loadPermissions([]);
      } else if (idParam === 'new-nonuser') {
        this.resetForCreate(false);
      } else {
        this.isNew.set(false);
        this.stakeholderId = +idParam;
        this.loadStakeholder();
      }
    });
  }

  private resetForCreate(isUser: boolean): void {
    this.isNew.set(true);
    this.isUserType.set(isUser);
    this.stakeholderId = null;
    this.version = null;
    this.wizardStep = 'details';
    this.submitted.set(false);
    this.goals.set([]);
    this.detailsForm.reset({ username: '', teamName: '', name: '', text: '' });
    this.applyMode(isUser);
    // Nothing to load, so resolve the gate (#185) - otherwise the create wizard sits behind the
    // skeleton forever and create becomes unreachable. Both create routes - new-user and
    // new-nonuser - come through here.
    this.loading.set(false);
    this.loadError.set(null);
  }

  /**
   * Permissions live in their own form, so dirtiness is either half. `detailsForm.dirty` alone
   * would miss a user who only ticked a permission box.
   */
  hasUnsavedChanges(): boolean {
    return this.detailsForm.dirty || this.permissionsForm.dirty;
  }

  /** Edit-mode Save: blocked on invalid, unchanged, or in-flight. */
  canSave(): boolean {
    return this.detailsForm.valid && this.hasUnsavedChanges() && !this.saving();
  }

  /**
   * Enables the half of the form the current mode uses and disables the other, so a disabled
   * control's `required` cannot block Continue for a mode that does not show it.
   */
  private applyMode(isUser: boolean): void {
    const { username, teamName, name, text } = this.detailsForm.controls;
    if (isUser) {
      username.enable({ emitEvent: false });
      teamName.enable({ emitEvent: false });
      name.disable({ emitEvent: false });
      text.disable({ emitEvent: false });
    } else {
      username.disable({ emitEvent: false });
      teamName.disable({ emitEvent: false });
      name.enable({ emitEvent: false });
      text.enable({ emitEvent: false });
    }
  }

  /** The control backing one permission checkbox. */
  permissionControl(key: string): FormControl<boolean> {
    return this.permissionsForm.controls[key];
  }

  ngOnDestroy(): void {
    this.paramSub?.unsubscribe();
    if (this.stakeholderId) {
      void this.eventStreamService.removeSubscription('Stakeholder', this.stakeholderId);
    }
    this.sseSub?.unsubscribe();
  }

  private async loadUsers(): Promise<void> {
    try {
      const users = await this.userService.listUsers();
      this.userOptions.set(users.map(u => ({ label: u.name || u.username, value: u.username })));
    } catch {
      this.errorMessage.set('Failed to load users.');
    }
  }

  /**
   * Reads the stakeholder and applies it in two parts: server state always, form state only when
   * the user has nothing unsaved.
   *
   * This editor had no guard, so every caller - initial load, SSE refresh, 409 recovery - patched
   * over whatever the user was typing and then marked both forms pristine, leaving Save disabled
   * with nothing on screen explaining why (#185). The window here is unusually wide even before
   * the detail GET: `loadUsers()` and `loadPermissions()` are awaited *inside* this method, so on
   * the user-stakeholder path the form is on screen and typeable across three round trips.
   *
   * The check therefore sits after those awaits rather than immediately after the detail GET, so
   * it catches edits made at any point while the load was still running.
   *
   * `hasUnsavedChanges()` is `detailsForm.dirty || permissionsForm.dirty`, so a user who has only
   * ticked a permission box is protected too - which is why `loadPermissions()` moved inside the
   * guard. It rebuilds the checkbox controls from the server's key set, so running it over a
   * half-ticked matrix would silently revert the ticks.
   *
   * Unconditional, because none of it is user-editable state: `version` (holding a stale one
   * guarantees a 409 on the next save), the user/non-user mode and the enable/disable pattern that
   * follows from it, the goals table, `loadedUserDetails` (template display only), and the user
   * dropdown options. Skipping the goals table behind a dirty form is the stale-table trap #184
   * found in `actor-editor`. `stakeholderName` is the *persisted* name, so it moves with the form.
   */
  /** Re-run the initial load; wired to the error state's (retry) output. */
  retryLoad(): void {
    void this.loadStakeholder();
  }

  /**
   * @param skeleton show the loading skeleton and the retryable error state. Suppressed for every
   *                 background caller - SSE refresh, post-save refetch and 409 recovery and the association refreshes - where
   *                 blanking the form the user is looking at would be worse than a stale moment,
   *                 and where a failure belongs in the inline message rather than in place of the
   *                 form. Mirrors `scenario-editor`.
   */
  private async loadStakeholder(skeleton = true): Promise<void> {
    if (skeleton) {
      this.loading.set(true);
      this.loadError.set(null);
    }
    try {
      const s = await this.stakeholderService.getStakeholder(this.projectName, this.stakeholderId!);
      // Server state, always.
      this.version = s.version;
      const isUser = s.type === 'user';
      this.isUserType.set(isUser);
      this.applyMode(isUser);
      this.goals.set(s.goals ?? []);
      if (s.userDetails) {
        this.loadedUserDetails.set(s.userDetails);
        await this.loadUsers();
      }

      // Form state, only when the user has nothing unsaved - in either form.
      if (!this.hasUnsavedChanges()) {
        this.stakeholderName.set(s.name);
        if (s.userDetails) {
          this.detailsForm.patchValue({
            username: s.userDetails.username,
            teamName: s.userDetails.teamName ?? '',
          }, { emitEvent: false });
          await this.loadPermissions(s.userDetails.permissionKeys);
        } else if (s.nonUserDetails) {
          this.detailsForm.patchValue({
            name: s.name,
            text: s.nonUserDetails.text,
          }, { emitEvent: false });
        }
        this.detailsForm.markAsPristine();
        this.permissionsForm.markAsPristine();
      }
    } catch {
      if (skeleton) {
        this.loadError.set('Failed to load stakeholder.');
      } else {
        this.errorMessage.set('Failed to load stakeholder.');
      }
    } finally {
      if (skeleton) {
        this.loading.set(false);
      }
    }
    if (this.stakeholderId && !this.sseSub) {
      void this.eventStreamService.addSubscription('Stakeholder', this.stakeholderId);
      this.sseSub = this.eventStreamService.events$.subscribe(envelope => {
        if (envelope.targetType === 'Stakeholder' && envelope.targetId === this.stakeholderId) {
          void this.loadStakeholder(false);
        }
      });
    }
  }

  canEditGoals(): boolean {
    return this.permissionService.canEdit('Goal');
  }

  async onGoalSelected(goal: EntityReferenceDto): Promise<void> {
    this.showGoalSelector = false;
    try {
      const result = await this.commandService.execute('AddGoalToGoalContainer', {
        projectName: this.projectName,
        goalContainerId: this.stakeholderId,
        goalId: goal.id,
        containerType: 'Stakeholder'
      });
      if (result.success) {
        this.applyAssociationResult(result.entity as StakeholderDto | null);
        this.messageService.add({ severity: 'success', summary: 'Goal added', detail: 'Goal added.' });
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
        goalContainerId: this.stakeholderId,
        goalId: goal.id,
        containerType: 'Stakeholder'
      });
      if (result.success) {
        this.applyAssociationResult(result.entity as StakeholderDto | null);
        this.messageService.add({ severity: 'success', summary: 'Goal removed', detail: 'Goal removed.' });
      } else {
        this.errorMessage.set(result.error ?? 'Failed to remove goal.');
      }
    } catch {
      this.errorMessage.set('Failed to remove goal.');
    }
  }

  /**
   * Apply the merged container an association command returns (#180). Add/RemoveGoalFromGoalContainer
   * end by merging the container — this stakeholder — so each returns it with its bumped `@Version`
   * and refreshed goals as `result.entity`. Taking both from the response removes the follow-up GET.
   *
   * Guarded on `version`: two associations can be in flight at once and resolve out of order; since
   * every merge increments `@Version`, a response older than what we hold would restore a stale
   * goals list, so we ignore it. A skipped version self-corrects through the next-save 409 path.
   *
   * Never touches the form, so an in-progress edit survives.
   */
  private applyAssociationResult(entity: StakeholderDto | null): void {
    if (!entity) {
      return;
    }
    if (this.version != null && entity.version <= this.version) {
      return;
    }
    this.version = entity.version;
    this.goals.set(entity.goals ?? []);
  }

  onGoalClick(goal: EntityReferenceDto): void {
    this.router.navigate(['/projects', this.projectName, 'goals', goal.id]);
  }

  private async loadPermissions(selectedKeys: string[]): Promise<void> {
    try {
      const available = await this.stakeholderService.getAvailablePermissions();
      const selectedSet = new Set(selectedKeys);

      // Group by entity type
      const groupMap = new Map<string, { key: string; type: string; checked: boolean }[]>();
      for (const p of available) {
        let group = groupMap.get(p.entityType);
        if (!group) {
          group = [];
          groupMap.set(p.entityType, group);
        }
        group.push({ key: p.permissionKey, type: p.permissionType, checked: selectedSet.has(p.permissionKey) });
      }

      const groups: PermissionGroup[] = [...groupMap.entries()]
          .map(([entityType, permissions]) => ({ entityType, permissions }))
          .sort((a, b) => a.entityType.localeCompare(b.entityType));
      this.permissionGroups.set(groups);

      // Rebuild the checkbox controls to match, then mark the whole set pristine: loading is
      // not a user edit, and leaving it dirty would arm the unsaved-changes guard on open.
      for (const key of Object.keys(this.permissionsForm.controls)) {
        this.permissionsForm.removeControl(key, { emitEvent: false });
      }
      for (const perm of groups.flatMap(g => g.permissions)) {
        this.permissionsForm.addControl(
          perm.key,
          new FormControl(perm.checked, { nonNullable: true }),
          { emitEvent: false }
        );
      }
      this.permissionsForm.markAsPristine();
    } catch {
      this.errorMessage.set('Failed to load permissions.');
    }
  }

  getPermission(group: PermissionGroup, type: string): { key: string; type: string; checked: boolean } | null {
    return group.permissions.find(p => p.type === type) ?? null;
  }

  getSelectedPermissionKeys(): string[] {
    return Object.entries(this.permissionsForm.controls)
        .filter(([, control]) => control.value)
        .map(([key]) => key);
  }

  /**
   * Runs the commit for the wizard's current step. Only Details talks to the API; the Goals
   * step's associations commit through the selector as the user works.
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

  /** Done on the last step: the stakeholder is already saved, so just go to it. */
  onWizardFinished(): void {
    if (this.stakeholderId != null) {
      this.router.navigate(['..', this.stakeholderId], { relativeTo: this.route });
    } else {
      this.onBack();
    }
  }

  /**
   * Issues whichever save command the mode calls for and, on success, adopts the id and
   * version from the response.
   *
   * The two commands are not symmetric: `EditUserStakeholder` is keyed by `username` and takes
   * no id, while `EditNonUserStakeholder` takes `stakeholderId` once one exists. That is why
   * the payload is built per branch rather than shared.
   *
   * Note this no longer navigates on create. The old code routed away the moment a new
   * stakeholder saved, which is exactly what made Goals unreachable until a second visit -
   * the wizard captures the id instead and moves to step 2.
   */
  private async saveDetails(): Promise<CommandResult<unknown>> {
    this.saving.set(true);
    this.errorMessage.set(null);
    clearServerErrors(this.detailsForm);
    try {
      const raw = this.detailsForm.getRawValue();
      const isUser = this.isUserType();

      const input: Record<string, unknown> = isUser
        ? {
            projectName: this.projectName,
            username: raw.username,
            teamName: raw.teamName || null,
            permissionKeys: this.getSelectedPermissionKeys(),
          }
        : {
            projectName: this.projectName,
            name: raw.name,
            text: raw.text,
          };
      if (!isUser && this.stakeholderId != null) input['stakeholderId'] = this.stakeholderId;
      if (this.version != null) input['version'] = this.version;

      const command = isUser ? 'EditUserStakeholder' : 'EditNonUserStakeholder';
      const result = await this.commandService.execute(command, input);
      if (!result.success) {
        const unresolved = applyCommandErrors(this.detailsForm, result.violations);
        if (unresolved.length) {
          this.errorMessage.set(unresolved.join(SEPARATOR));
        }
        return result;
      }

      const wasCreate = this.stakeholderId == null;
      if (wasCreate) {
        this.projectService.notifyTreeChanged();
      }

      const saved = result.entity as StakeholderDto | null;
      if (saved) {
        this.stakeholderId = saved.id;
        this.version = saved.version;
        this.stakeholderName.set(saved.name);
      }
      this.detailsForm.markAsPristine();
      this.permissionsForm.markAsPristine();
      this.messageService.add({ severity: 'success', summary: 'Saved', detail: 'Stakeholder saved.' });

      if (wasCreate && this.stakeholderId != null) {
        await this.loadStakeholder(false);
      }
      return result;
    } catch {
      return {
        success: false,
        entityType: 'Stakeholder',
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
    if (result.status !== 409 || this.stakeholderId == null) {
      return false;
    }
    await this.loadStakeholder(false);
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

  onDelete(): void {
    this.confirmationService.confirm({
      message: 'Are you sure you want to delete this stakeholder?',
      accept: () => this.doDelete()
    });
  }

  private async doDelete(): Promise<void> {
    try {
      const result = await this.commandService.execute('DeleteStakeholder', {
        projectName: this.projectName,
        stakeholderId: this.stakeholderId,
        version: this.version
      });
      if (result.success) {
        this.projectService.notifyTreeChanged();
        this.router.navigate(['..'], { relativeTo: this.route });
      } else {
        this.errorMessage.set(result.error ?? 'Delete failed.');
      }
    } catch {
      this.errorMessage.set('An unexpected error occurred.');
    }
  }

  onBack(): void {
    this.router.navigate(['..'], { relativeTo: this.route });
  }
}
