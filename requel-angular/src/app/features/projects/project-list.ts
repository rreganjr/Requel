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
import { Component, OnInit, signal, ViewChild, ElementRef } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { InputText } from 'primeng/inputtext';
import { ProjectDto } from '../../models/project';
import { ProjectService } from '../../core/project.service';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-project-list',
  standalone: true,
  imports: [FormsModule, TableModule, ButtonModule, MessageModule, InputText],
  template: `
    <div class="project-list">
      <div class="page-header">
        <h2>Projects</h2>
        <div class="page-actions">
          @if (canCreateProjects()) {
            <p-button label="New Project" icon="pi pi-plus" (onClick)="onNewProject()" />
            <p-button label="Import" icon="pi pi-upload" severity="secondary"
                      [outlined]="true" [loading]="importing()" (onClick)="fileInput.click()" />
            <input #fileInput type="file" accept=".xml" (change)="onImportFile($event)"
                   style="display: none" />
          }
        </div>
      </div>

      @if (errorMessage()) {
        <p-message severity="error" [text]="errorMessage()!" />
      }
      @if (successMessage()) {
        <p-message severity="success" [text]="successMessage()!" />
      }

      <div class="search-bar">
        <span class="p-input-icon-left">
          <i class="pi pi-search"></i>
          <input pInputText [(ngModel)]="searchText" placeholder="Search projects..."
                 (input)="dt.filterGlobal(searchText(), 'contains')" />
        </span>
      </div>

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
          <tr><td colspan="8">No projects found.</td></tr>
        </ng-template>
      </p-table>
    </div>
  `,
  styles: [`
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
    .page-header h2 { margin: 0; }
    .page-actions { display: flex; gap: 0.5rem; }
    .search-bar { margin-bottom: 1rem; }
    .search-bar input { width: 300px; }
  `]
})
export class ProjectListComponent implements OnInit {

  readonly projects = signal<ProjectDto[]>([]);
  readonly loading = signal(true);
  readonly importing = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly searchText = signal('');

  readonly canCreateProjects = signal(false);

  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  constructor(
    private projectService: ProjectService,
    private authService: AuthService,
    private router: Router
  ) {}

  async ngOnInit(): Promise<void> {
    const user = this.authService.user();
    this.canCreateProjects.set(user?.permissions?.includes('createProjects') ?? false);
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

  async onImportFile(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

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
      input.value = '';
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
