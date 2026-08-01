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
import { Component, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { ProjectDto } from '../../models/project';
import { ProjectService } from '../../core/project.service';
import { AuthService } from '../../core/auth.service';
import { ListPageComponent } from '../../shared/list-page';
import { FileUploadButtonComponent } from '../../shared/file-upload-button';

@Component({
  selector: 'app-project-list',
  standalone: true,
  imports: [ListPageComponent, TableModule, ButtonModule, MessageModule, FileUploadButtonComponent],
  template: `
    <app-list-page title="Projects" searchPlaceholder="Search projects..."
                   (search)="dt.filterGlobal($event, 'contains')">
      <ng-container actions>
        @if (canCreateProjects()) {
          <p-button label="New Project" icon="pi pi-plus" data-testid="project-list-new-project" (onClick)="onNewProject()" />
          <app-file-upload-button label="Import" [outlined]="true" [loading]="importing()"
                                  accept=".xml" buttonTestid="project-list-import-button"
                                  inputTestid="project-list-import-input"
                                  (fileSelected)="onImportFile($event)" />
        }
      </ng-container>

      @if (errorMessage()) {
        <p-message severity="error" [text]="errorMessage()!" data-testid="project-list-error" />
      }
      @if (warningMessage()) {
        <p-message severity="warn" [text]="warningMessage()!" data-testid="project-list-warning" />
      }
      @if (successMessage()) {
        <p-message severity="success" [text]="successMessage()!" data-testid="project-list-success" />
      }

      <p-table #dt [value]="projects()" [loading]="loading()" [paginator]="true" [rows]="20"
               [rowHover]="true" selectionMode="single" (onRowSelect)="onRowSelect($event)"
               [globalFilterFields]="['name', 'organizationName', 'status', 'createdBy']">
        <ng-template #header>
          <tr>
            <th pSortableColumn="name">Name <p-sortIcon field="name" /></th>
            <th pSortableColumn="organizationName">Organization <p-sortIcon field="organizationName" /></th>
            <th pSortableColumn="status">Status <p-sortIcon field="status" /></th>
            <th pSortableColumn="createdBy">Created By <p-sortIcon field="createdBy" /></th>
            <th>Stakeholders</th>
            <th>Goals</th>
            <th>Stories</th>
            <th>Use Cases</th>
          </tr>
        </ng-template>
        <ng-template #body let-project>
          <tr [pSelectableRow]="project">
            <td>{{ project.name }}</td>
            <td>{{ project.organizationName }}</td>
            <td>{{ project.status }}</td>
            <td>{{ project.createdBy }}</td>
            <td>{{ project.stakeholderCount }}</td>
            <td>{{ project.goalCount }}</td>
            <td>{{ project.storyCount }}</td>
            <td>{{ project.useCaseCount }}</td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr data-testid="project-list-empty"><td colspan="8">No projects found.</td></tr>
        </ng-template>
      </p-table>
    </app-list-page>
  `,
  styles: []
})
export class ProjectListComponent implements OnInit {

  readonly projects = signal<ProjectDto[]>([]);
  readonly loading = signal(true);
  readonly importing = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly warningMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly canCreateProjects = signal(false);


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
    this.warningMessage.set(null);
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
