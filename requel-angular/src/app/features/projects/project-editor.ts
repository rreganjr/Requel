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
import { Component, OnInit, OnDestroy, signal, computed, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule, NgForm } from '@angular/forms';
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

@Component({
  selector: 'app-project-editor',
  standalone: true,
  imports: [FormsModule, InputText, TextareaModule, ButtonModule, SelectModule, MessageModule, ConfirmDialogModule],
  providers: [ConfirmationService],
  template: `
    <div class="project-editor" data-testid="project-editor">
      <div class="page-header">
        <h2>{{ isNew() ? 'New Project' : 'Project: ' + originalName() }}</h2>
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

      <form #projectForm="ngForm" (ngSubmit)="onSave()">
        <div class="form-grid">
          <div class="field">
            <label for="name">Project Name</label>
            <input pInputText id="name" [(ngModel)]="name" name="name" required />
          </div>

          <div class="field">
            <label for="org">Organization</label>
            <p-select id="org" [(ngModel)]="selectedOrg" name="org"
                      [options]="orgOptions()" optionLabel="label" optionValue="value"
                      [editable]="true" placeholder="Select or type organization" />
          </div>

          <div class="field full-width">
            <label for="description">Description</label>
            <textarea pTextarea id="description" [(ngModel)]="description" name="description"
                      [rows]="5" [autoResize]="true"></textarea>
          </div>
        </div>

        <div class="actions">
          <p-button type="submit" label="Save" icon="pi pi-check" data-testid="project-save"
                    [loading]="saving()" [disabled]="!projectForm.dirty" />
          <p-button label="Cancel" icon="pi pi-times" severity="secondary" data-testid="project-cancel"
                    (onClick)="onCancel()" [outlined]="true" />
        </div>
      </form>
    </div>

    <p-confirmDialog />
  `,
  styles: [`
    .project-editor { max-width: 800px; }
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
    .page-header h2 { margin: 0; }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-bottom: 1.5rem; }
    .field { display: flex; flex-direction: column; gap: 0.5rem; }
    .field label { font-weight: 500; }
    .field input, .field p-select, .field textarea { width: 100%; }
    .full-width { grid-column: 1 / -1; }
    .actions { display: flex; gap: 0.5rem; }
  `]
})
export class ProjectEditorComponent implements OnInit, OnDestroy, DirtyCheckable {

  @ViewChild('projectForm') projectForm!: NgForm;

  readonly isNew = signal(true);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly originalName = signal('');
  private projectId: number | null = null;
  private projectVersion: number | null = null;

  // Form fields
  name = '';
  description = '';
  selectedOrg: OrganizationDto | string | null = null;

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
      if (this.projectForm?.dirty && !newIsNew) {
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
    return this.projectForm?.dirty ?? false;
  }

  ngOnDestroy(): void {
    this.paramSub?.unsubscribe();
  }

  private async loadProject(nameParam: string | null): Promise<void> {
    const newIsNew = nameParam === 'new' || !nameParam;
    this.isNew.set(newIsNew);
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.loading.set(true);

    try {
      if (!newIsNew && nameParam) {
        const [project] = await Promise.all([
          this.projectService.getProject(nameParam),
          this.permissionService.loadForProject(nameParam)
        ]);
        this.populateForm(project);
      } else {
        this.projectId = null;
        this.projectVersion = null;
        this.originalName.set('');
        this.name = '';
        this.description = '';
        this.selectedOrg = null;
      }
    } finally {
      this.loading.set(false);
      // Reset form dirty state after load
      setTimeout(() => this.projectForm?.form.markAsPristine());
    }
  }

  async onSave(): Promise<void> {
    this.saving.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    try {
      // Resolve organization: object = existing org by id, string = new org by name
      const orgId = typeof this.selectedOrg === 'object' && this.selectedOrg ? this.selectedOrg.id : null;
      const orgName = typeof this.selectedOrg === 'string' && this.selectedOrg ? this.selectedOrg : null;

      const input: Record<string, unknown> = {
        id: this.projectId,
        version: this.projectVersion,
        projectName: this.isNew() ? null : this.originalName(),
        name: this.name,
        description: this.description || null,
        organizationId: orgId,
        organizationName: orgName
      };

      const result = await this.commandService.execute('EditProject', input);
      if (result.success) {
        this.successMessage.set('Project saved successfully.');
        this.projectForm?.form.markAsPristine();
        if (this.isNew()) {
          await this.router.navigate(['/projects', this.name]);
        } else {
          if (this.name !== this.originalName() && !this.switchingProject) {
            await this.router.navigate(['/projects', this.name]);
          }
          this.originalName.set(this.name);
        }
      } else if (result.violations?.length) {
        this.errorMessage.set(result.violations.map(v => v.message).join('; '));
      } else {
        this.errorMessage.set(result.error ?? 'Save failed.');
      }
    } catch (err: unknown) {
      this.errorMessage.set(err instanceof Error ? err.message : 'Save failed.');
    } finally {
      this.saving.set(false);
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
    this.projectVersion = project.version;
    this.originalName.set(project.name);
    this.name = project.name;
    this.description = project.description ?? '';
    // Match loaded org name to an OrganizationDto from the dropdown list
    const matchedOrg = this.organizations().find(o => o.name === project.organizationName);
    this.selectedOrg = matchedOrg ?? project.organizationName ?? null;
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
