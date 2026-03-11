import { Component, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { ProjectDto } from '../../models/project';
import { ProjectService } from '../../core/project.service';

@Component({
  selector: 'app-project-list',
  standalone: true,
  imports: [TableModule, ButtonModule],
  template: `
    <div class="project-list">
      <div class="header">
        <h2>Projects</h2>
        <div class="actions">
          <p-button label="New Project" icon="pi pi-plus" (onClick)="onNewProject()" />
        </div>
      </div>

      <p-table [value]="projects()" [loading]="loading()" [paginator]="true" [rows]="20"
               [rowHover]="true" selectionMode="single" (onRowSelect)="onRowSelect($event)">
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
    .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
    .actions { display: flex; gap: 0.5rem; }
    h2 { margin: 0; }
  `]
})
export class ProjectListComponent implements OnInit {

  readonly projects = signal<ProjectDto[]>([]);
  readonly loading = signal(true);

  constructor(private projectService: ProjectService, private router: Router) {}

  async ngOnInit(): Promise<void> {
    try {
      const projects = await this.projectService.listProjects();
      this.projects.set(projects);
    } finally {
      this.loading.set(false);
    }
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
}
