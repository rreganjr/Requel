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
import { Component, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import { ProjectDto } from '../../models/project';
import { ProjectService } from '../../core/project.service';
import { AuthService } from '../../core/auth.service';
import { ListPageComponent } from '../../shared/list-page';
import { FileUploadButtonComponent } from '../../shared/file-upload-button';
import { AppDataTableComponent, DataTableColumn, RowAction } from '../../shared/app-data-table';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-project-list',
  standalone: true,
  imports: [ListPageComponent, AppDataTableComponent, ButtonModule, MessageModule, SubmitErrorComponent, FileUploadButtonComponent],
  template: `
    <app-list-page title="Projects" [fill]="true" [showSearch]="false">
      <app-submit-error [message]="errorMessage()" testid="project-list-error" />
      <div role="status" aria-live="polite">@if (successMessage()) {
        <p-message severity="success" [text]="successMessage()!" data-testid="project-list-success" />
      }</div>

      <app-data-table scrollHeight="flex" [value]="projects()" [columns]="columns" [loading]="loading()"
                      [rowActions]="rowActions" searchPlaceholder="Search projects..."
                      [globalFilterFields]="['name', 'organizationName', 'status', 'createdBy']"
                      testid="project-list" (rowClick)="onRowSelect({ data: $event })"
                      emptyTitle="No projects yet"
                      emptyMessage="Create a project to start capturing goals, stories, and use cases — or import an existing one."
                      emptyIcon="pi-folder-open" emptyActionLabel="New Project"
                      [showEmptyAction]="canCreateProjects()" (emptyAction)="onNewProject()">
        <div toolbarActions class="project-toolbar-actions">
          @if (canCreateProjects()) {
            <p-button label="New Project" icon="pi pi-plus" data-testid="project-list-new-project" (onClick)="onNewProject()" />
            <app-file-upload-button label="Import" [outlined]="true" [loading]="importing()"
                                    accept=".xml" buttonTestid="project-list-import-button"
                                    inputTestid="project-list-import-input"
                                    (fileSelected)="onImportFile($event)" />
          }
        </div>
      </app-data-table>
    </app-list-page>
  `,
  styles: [`
    /* Fill mode (#221): claim main-content's height so the data-table body
       scrolls between a pinned header and the paginator. */
    :host { display: flex; flex-direction: column; flex: 1; min-height: 0; }
    .project-toolbar-actions { display: flex; align-items: center; gap: var(--rq-space-2); }
  `]
})
export class ProjectListComponent implements OnInit {

  readonly projects = signal<ProjectDto[]>([]);
  readonly loading = signal(true);
  readonly importing = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly canCreateProjects = signal(false);

  columns: DataTableColumn<ProjectDto>[] = [
    { field: 'name', header: 'Name', sortable: true, link: p => ['/projects', p.name] },
    { field: 'organizationName', header: 'Organization', sortable: true },
    { field: 'status', header: 'Status', sortable: true },
    { field: 'createdBy', header: 'Created By', sortable: true },
    { field: 'stakeholderCount', header: 'Stakeholders' },
    { field: 'goalCount', header: 'Goals' },
    { field: 'storyCount', header: 'Stories' },
    { field: 'useCaseCount', header: 'Use Cases' }
  ];
  rowActions: RowAction<ProjectDto>[] = [
    { label: 'Open', icon: 'pi pi-eye', command: p => this.onRowSelect({ data: p }) }
  ];

  constructor(
    private projectService: ProjectService,
    private authService: AuthService,
    private router: Router
  ) {}

  async ngOnInit(): Promise<void> {
    const user = this.authService.user();
    // System admins can create projects at the command level regardless of the
    // createProjects role-permission (which is a gate for regular project users).
    const isAdmin = user?.roles?.includes('SystemAdminUserRole') ?? false;
    this.canCreateProjects.set(isAdmin || (user?.permissions?.includes('createProjects') ?? false));
    await this.loadProjects();
  }

  onNewProject(): void {
    this.router.navigate(['/projects', 'new']);
  }

  onRowSelect(event: { data?: ProjectDto | ProjectDto[] }): void {
    const project = Array.isArray(event.data) ? event.data[0] : event.data;
    if (project) {
      this.router.navigate(['/projects', project.name]);
    }
  }

  async onImportFile(file: File): Promise<void> {
    this.importing.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    try {
      const result = await this.projectService.importProject(file);
      if (result.success) {
        this.successMessage.set('Project imported successfully.');
        await this.loadProjects();
      } else {
        this.errorMessage.set(result.error ?? 'Import failed.');
      }
    } catch (err: unknown) {
      this.errorMessage.set(err instanceof Error ? err.message : 'Import failed.');
    } finally {
      this.importing.set(false);
    }
  }

  private async loadProjects(): Promise<void> {
    this.loading.set(true);
    try {
      const projects = await this.projectService.listProjects();
      this.projects.set(projects);
    } finally {
      this.loading.set(false);
    }
  }
}
