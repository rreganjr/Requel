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
import { Component, OnInit, OnDestroy, signal, computed } from '@angular/core';
import { PageHeaderComponent } from '../../shared/page-header';
import { AppCardComponent } from '../../shared/app-card';
import { ActivatedRoute, Router } from '@angular/router';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgTemplateOutlet } from '@angular/common';
import { InputText } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { MessageModule } from 'primeng/message';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService } from 'primeng/api';
import { Subscription } from 'rxjs';
import { DirtyCheckable } from '../../core/dirty-check.guard';
import { ProjectDto } from '../../models/project';
import { OrganizationDto } from '../../models/user';
import { ProjectService } from '../../core/project.service';
import { UserService } from '../../core/user.service';
import { CommandService } from '../../core/command.service';
import { PermissionService } from '../../core/permission.service';
import { TagSelectorComponent } from '../../shared/tag-selector';
import { LoadingStateComponent } from '../../shared/loading-state';
import { ErrorStateComponent } from '../../shared/error-state';
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
 * `EditProjectInput` carries the organization as either `organizationId` or
 * `organizationName` depending on whether the user picked an existing org or typed a new one;
 * both resolve to the single `organization` control. #176 deletes this map.
 */
const PROJECT_FIELD_MAP: Record<string, string> = {
  organizationId: 'organization',
  organizationName: 'organization',
};

/** Joins page-level violations that resolved to no control. */
const SEPARATOR = '; ';

@Component({
  selector: 'app-project-editor',
  standalone: true,
  imports: [PageHeaderComponent, AppCardComponent, NgTemplateOutlet, ReactiveFormsModule,
            InputText, TextareaModule, ButtonModule, SelectModule, MessageModule,
            ConfirmDialogModule, TagSelectorComponent, LoadingStateComponent, ErrorStateComponent,
            AppFieldComponent, AppFieldControlDirective,
            AppFormWizardComponent, AppWizardStepComponent],
  providers: [ConfirmationService],
  template: `
    <div class="project-editor" data-testid="project-editor">
      <div class="page-header">
        <app-page-header [title]="isNew() ? 'New Project' : 'Project: ' + originalName()" />
        <div class="page-actions">
          @if (!isNew()) {
            <p-button icon="pi pi-download" label="Export" [outlined]="true"
                      severity="secondary" size="small" data-testid="project-export"
                      (onClick)="onExport()" />
          }
        </div>
      </div>

      @if (errorMessage()) {
        <p-message severity="error" [text]="errorMessage()!" />
      }
      @if (successMessage()) {
        <p-message severity="success" [text]="successMessage()!" />
      }

      @if (loading()) {
        <app-card>
          <app-loading-state label="Loading project…" [lines]="4" testid="project-editor-loading" />
        </app-card>
      } @else if (loadError()) {
        <app-error-state [message]="loadError()!" testid="project-editor-load-error"
                         (retry)="retryLoad()" />
      } @else if (isNew()) {
        <!--
          Create runs as a wizard (#173) so Tags is reachable before the first save. Project is
          the odd one of the five: it is not created inside an existing project, so step 2 has
          no parent context to inherit - the tag selector keys off the project this wizard is
          in the middle of creating, which is why it cannot render until step 1 commits.
        -->
        <app-form-wizard
          [(activeKey)]="wizardStep"
          navLabel="New project steps"
          (stepCommit)="onStepCommit($event)"
          (cancelled)="onCancel()"
          (finished)="onWizardFinished()"
          data-testid="project-wizard"
        >
          <app-wizard-step key="details" label="Details" helper="Name, organization, description"
                           [form]="detailsForm">
            <ng-template>
              <ng-container [ngTemplateOutlet]="detailsFields" />
            </ng-template>
          </app-wizard-step>

          <app-wizard-step key="tags" label="Tags" helper="Categorise this project"
                           [optional]="true">
            <ng-template>
              <ng-container [ngTemplateOutlet]="tagsSection" />
            </ng-template>
          </app-wizard-step>
        </app-form-wizard>
      } @else {
        <app-card>
          <ng-container [ngTemplateOutlet]="detailsFields" />

          <div class="actions">
            <p-button label="Save" icon="pi pi-check" data-testid="project-save"
                      [loading]="saving()" [disabled]="!canSave()" (onClick)="onSave()" />
            <p-button label="Cancel" icon="pi pi-times" severity="secondary" data-testid="project-cancel"
                      (onClick)="onCancel()" [outlined]="true" />
          </div>
        </app-card>

        <ng-container [ngTemplateOutlet]="tagsSection" />
      }

      <!--
        Shared bodies, used by both the wizard step and the edit view so the two cannot drift.
        Controls bind [formControl], not formControlName: these are projected into the wizard,
        where formControlName would look for a parent formGroup that is not there.
      -->
      <ng-template #detailsFields>
        <app-field label="Project Name" controlId="name" [control]="detailsForm.controls.name"
                   [errorMessages]="nameErrors" [submitted]="submitted()">
          <input appFieldControl pInputText [formControl]="detailsForm.controls.name" id="name"
                 [attr.maxlength]="nameMaxLength"
                 placeholder="Project name" data-testid="project-name" />
        </app-field>

        <app-field label="Organization" controlId="projectOrgInput"
                   [control]="detailsForm.controls.organization" [submitted]="submitted()">
          <p-select appFieldControl inputId="projectOrgInput" data-testid="project-org"
                    [formControl]="detailsForm.controls.organization"
                    [options]="orgOptions()" optionLabel="label" optionValue="value"
                    [editable]="true" placeholder="Select or type organization" />
        </app-field>

        <app-field label="Description" controlId="description" [control]="detailsForm.controls.description"
                   [divider]="false" [submitted]="submitted()">
          <textarea appFieldControl pTextarea [formControl]="detailsForm.controls.description" id="description"
                    [rows]="5" [autoResize]="true" data-testid="project-description"></textarea>
        </app-field>
      </ng-template>

      <ng-template #tagsSection>
        @if (tagEntityId() != null) {
          <app-tag-selector
            [projectName]="originalName()"
            entityType="Project"
            [entityId]="tagEntityId()"
            [canEdit]="canEdit()" />
        } @else {
          <p class="empty-text">Save the project's details first to add tags.</p>
        }
      </ng-template>
    </div>

    <p-confirmDialog />
  `,
  styles: [`
    .project-editor { max-width: 800px; }
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
    .empty-text { color: var(--p-text-muted-color); font-style: italic; }
    .actions { display: flex; gap: 0.5rem; }
  `]
})
export class ProjectEditorComponent implements OnInit, OnDestroy, DirtyCheckable {

  readonly isNew = signal(true);
  readonly submitted = signal(false);
  readonly canEdit = signal(false);
  readonly tagEntityId = signal<number | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  // Load failures are tracked separately from save/inline errors so the
  // retryable error state replaces the form only when the initial load fails.
  readonly loadError = signal<string | null>(null);
  readonly originalName = signal('');
  private projectId: number | null = null;
  private projectVersion: number | null = null;
  private lastNameParam: string | null = null;

  /** Mirrors the backend `@Size(max = ValidationLimits.ARTIFACT_NAME_MAX)` (#171). */
  readonly nameMaxLength = ARTIFACT_NAME_MAX_LENGTH;

  /**
   * Details step / edit form. Replaces the template-driven `NgForm` and its three `ngModel`
   * bindings, which is also what retires the `setTimeout(() => markAsPristine())` after load -
   * `reset()`/`patchValue()` settle synchronously.
   *
   * `organization` holds either an `OrganizationDto` (picked from the list) or a raw string
   * (typed into the editable select). `onSave` splits those into `organizationId` /
   * `organizationName`, which is why one control backs two DTO fields in PROJECT_FIELD_MAP.
   */
  readonly detailsForm = new FormGroup({
    name: new FormControl('', {
      validators: [Validators.required, Validators.maxLength(ARTIFACT_NAME_MAX_LENGTH)],
      nonNullable: true,
    }),
    organization: new FormControl<OrganizationDto | string | null>(null),
    description: new FormControl('', { nonNullable: true }),
  });

  readonly nameErrors = { required: 'A project needs a name.' };

  /** Active wizard step key, two-way bound to `app-form-wizard`. */
  wizardStep = 'details';

  // Organization dropdown
  readonly organizations = signal<OrganizationDto[]>([]);
  readonly orgOptions = computed(() =>
    this.organizations().map(org => ({ label: org.name, value: org }))
  );

  private paramSub!: Subscription;
  private pendingNavName: string | null = null;
  private switchingProject = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private projectService: ProjectService,
    private userService: UserService,
    private commandService: CommandService,
    private permissionService: PermissionService,
    private confirmationService: ConfirmationService
  ) {}

  ngOnInit(): void {
    // Load orgs once
    this.userService.listOrganizations().then(orgs => this.organizations.set(orgs));

    // React to route param changes (handles sidebar project clicks)
    this.paramSub = this.route.paramMap.subscribe(params => {
      const nameParam = params.get('name');
      const newIsNew = nameParam === 'new' || !nameParam;

      // If form is dirty, confirm before switching
      if (this.detailsForm.dirty && !newIsNew) {
        this.pendingNavName = nameParam;
        this.confirmationService.confirm({
          message: 'You have unsaved changes. Save before switching?',
          header: 'Unsaved Changes',
          icon: 'pi pi-exclamation-triangle',
          acceptLabel: 'Save & Switch',
          rejectLabel: 'Cancel',
          accept: () => this.saveAndSwitch(),
          reject: () => {
            // Stay on current form — navigate back to original
            this.pendingNavName = null;
            this.router.navigate(['/projects', this.originalName()], { replaceUrl: true });
          }
        });
        return;
      }

      this.loadProject(nameParam);
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
  }

  /** Re-run the last attempted load; wired to the error state's (retry) output. */
  retryLoad(): void {
    void this.loadProject(this.lastNameParam);
  }

  private async loadProject(nameParam: string | null): Promise<void> {
    const newIsNew = nameParam === 'new' || !nameParam;
    this.isNew.set(newIsNew);
    this.lastNameParam = nameParam;
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.loadError.set(null);
    this.loading.set(true);

    try {
      if (!newIsNew && nameParam) {
        const [project] = await Promise.all([
          this.projectService.getProject(nameParam),
          this.permissionService.loadForProject(nameParam)
        ]);
        this.populateForm(project);
        this.canEdit.set(this.permissionService.canEdit('Project'));
      } else {
        this.projectId = null;
        this.projectVersion = null;
        this.tagEntityId.set(null);
        this.originalName.set('');
        this.wizardStep = 'details';
        this.submitted.set(false);
        this.detailsForm.reset({ name: '', organization: null, description: '' });
      }
    } catch (err: unknown) {
      // Previously uncaught: a failed load left a blank form with no feedback.
      // Surface a retryable error state instead.
      this.loadError.set(err instanceof Error ? err.message : 'Failed to load project.');
    } finally {
      this.loading.set(false);
    }
  }

  /**
   * Runs the commit for the wizard's current step. Only Details talks to the API - the Tags
   * step commits through `app-tag-selector` as the user works, and `AssignTag` mutates the Tag
   * rather than the project, so it does not spend the project's version (verified in #158 and
   * re-confirmed in doc/173-create-flow-wizards-plan.md §2). Project is the one wizard of the
   * five whose second step needs no version handling at all.
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
    request.fail(result.error ?? 'Save failed.');
  }

  /** Done on the last step: the project exists, so route to it by its saved name. */
  onWizardFinished(): void {
    const name = this.originalName();
    if (name) {
      void this.router.navigate(['/projects', name]);
    } else {
      this.onCancel();
    }
  }

  /**
   * Issues `EditProject` and, on success, adopts id/version/name.
   *
   * Deliberately does NOT navigate. The old create path routed to `/projects/{name}` the moment
   * the project saved, which is what made Tags unreachable without a second visit - and here it
   * also destroyed the wizard, since the route param is the project identity. The wizard keeps
   * the component alive and lets `onWizardFinished` do the navigating.
   */
  private async saveDetails(): Promise<CommandResult<unknown>> {
    this.saving.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);
    clearServerErrors(this.detailsForm);

    try {
      const { name, organization, description } = this.detailsForm.getRawValue();
      // Resolve organization: object = existing org by id, string = new org by name.
      const orgId = typeof organization === 'object' && organization ? organization.id : null;
      const orgName = typeof organization === 'string' && organization ? organization : null;

      const input: Record<string, unknown> = {
        id: this.projectId,
        version: this.projectVersion,
        projectName: this.isNew() ? null : this.originalName(),
        name,
        description: description || null,
        organizationId: orgId,
        organizationName: orgName,
      };

      const result = await this.commandService.execute('EditProject', input);
      if (!result.success) {
        const unresolved = applyCommandErrors(this.detailsForm, result.violations, PROJECT_FIELD_MAP);
        this.errorMessage.set(
          unresolved.length ? unresolved.join(SEPARATOR) : (result.error ?? 'Save failed.')
        );
        return result;
      }

      this.successMessage.set('Project saved successfully.');
      const saved = result.entity as ProjectDto | null;
      if (saved) {
        this.projectId = saved.id;
        this.projectVersion = saved.version;
        this.tagEntityId.set(saved.id);
      }
      this.originalName.set(name);
      this.detailsForm.markAsPristine();
      return result;
    } catch (err: unknown) {
      return {
        success: false,
        entityType: 'Project',
        entity: null,
        error: err instanceof Error ? err.message : 'Save failed.',
        violations: null,
      };
    } finally {
      this.saving.set(false);
    }
  }

  /**
   * Edit-mode Save. Renaming changes the route identity, so a successful rename navigates -
   * except while `saveAndSwitch` is driving, which is about to navigate somewhere else anyway.
   */
  async onSave(): Promise<void> {
    this.submitted.set(true);
    if (this.detailsForm.invalid) {
      this.detailsForm.markAllAsTouched();
      return;
    }

    const previousName = this.originalName();
    const result = await this.saveDetails();
    if (!result.success) {
      return;
    }
    const name = this.detailsForm.controls.name.value;
    if (name !== previousName && !this.switchingProject) {
      try {
        await this.router.navigate(['/projects', name]);
      } catch (err: unknown) {
        // The save succeeded; only the route change failed. Report it rather than rejecting
        // out of onSave - the original code had this navigation inside its try/catch and
        // relied on that, so letting it escape would be a behaviour change.
        this.errorMessage.set(
          err instanceof Error ? `Saved, but navigation failed: ${err.message}` : 'Saved, but navigation failed.'
        );
      }
    }
  }

  async onExport(): Promise<void> {
    const projectName = this.originalName();
    if (!projectName) {
      return;
    }
    try {
      // Fetch via HttpClient so the AuthInterceptor adds the JWT Bearer header.
      // A plain window.open() of the export URL would issue an unauthenticated
      // navigation and the JWT-protected endpoint would return 401 with no
      // Content-Disposition, so no download would ever fire.
      const blob = await this.projectService.downloadProjectXml(projectName);
      const sanitized = projectName.replace(/[^a-zA-Z0-9._-]/g, '_');
      this.triggerBlobDownload(blob, `${sanitized}.xml`);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to export project';
      this.errorMessage.set(`Export failed: ${message}`);
    }
  }

  private triggerBlobDownload(blob: Blob, filename: string): void {
    const objectUrl = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = objectUrl;
    a.download = filename;
    a.style.display = 'none';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    // Defer revocation. Setting timeout=0 (or revoking synchronously) is a
    // common bug: Chromium fires the download's "begin" event almost
    // immediately, but it hasn't necessarily copied the blob bytes yet.
    // Revoking too soon yields a captured download with an empty body
    // (Playwright's download.saveAs() then writes a 0-byte file). FileSaver.js
    // uses 40s for the same reason; 60s is a comfortable upper bound for any
    // realistic export size and gets GC'd on navigation either way.
    setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000);
  }

  onCancel(): void {
    this.router.navigate(['/projects']);
  }

  private populateForm(project: ProjectDto): void {
    this.projectId = project.id;
    this.tagEntityId.set(project.id);
    this.projectVersion = project.version;
    this.originalName.set(project.name);
    // Match loaded org name to an OrganizationDto from the dropdown list
    const matchedOrg = this.organizations().find(o => o.name === project.organizationName);
    this.detailsForm.reset({
      name: project.name,
      organization: matchedOrg ?? project.organizationName ?? null,
      description: project.description ?? '',
    });
  }

  private async saveAndSwitch(): Promise<void> {
    this.switchingProject = true;
    try {
      await this.onSave();
      if (!this.errorMessage() && this.pendingNavName) {
        const target = this.pendingNavName;
        this.pendingNavName = null;
        await this.loadProject(target);
      }
    } finally {
      this.switchingProject = false;
    }
  }
}
