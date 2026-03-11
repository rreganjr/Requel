import { Component, OnInit, signal, computed } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { InputText } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { MessageModule } from 'primeng/message';
import { ProjectDto } from '../../models/project';
import { ProjectService } from '../../core/project.service';
import { UserService } from '../../core/user.service';
import { CommandService } from '../../core/command.service';

@Component({
  selector: 'app-project-editor',
  standalone: true,
  imports: [FormsModule, InputText, TextareaModule, ButtonModule, SelectModule, MessageModule],
  template: `
    <div class="project-editor">
      <h2>{{ isNew() ? 'New Project' : 'Edit Project: ' + originalName() }}</h2>

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
            <p-select id="org" [(ngModel)]="organizationName" name="org"
                      [options]="orgOptions()" [editable]="true"
                      placeholder="Select or type organization" />
          </div>

          <div class="field full-width">
            <label for="description">Description</label>
            <textarea pTextarea id="description" [(ngModel)]="description" name="description"
                      [rows]="5" [autoResize]="true"></textarea>
          </div>
        </div>

        <div class="actions">
          <p-button type="submit" label="Save" icon="pi pi-check"
                    [loading]="saving()" [disabled]="!projectForm.dirty" />
          <p-button label="Cancel" icon="pi pi-times" severity="secondary"
                    (onClick)="onCancel()" [outlined]="true" />
        </div>
      </form>
    </div>
  `,
  styles: [`
    .project-editor { max-width: 800px; }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-bottom: 1.5rem; }
    .field { display: flex; flex-direction: column; gap: 0.5rem; }
    .field label { font-weight: 500; }
    .field input, .field p-select, .field textarea { width: 100%; }
    .full-width { grid-column: 1 / -1; }
    .actions { display: flex; gap: 0.5rem; }
  `]
})
export class ProjectEditorComponent implements OnInit {

  readonly isNew = signal(true);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  // The original project name (used as the identifier for updates)
  readonly originalName = signal('');

  // Form fields
  readonly name = signal('');
  readonly description = signal('');
  readonly organizationName = signal('');

  // Organization dropdown
  readonly organizations = signal<string[]>([]);
  readonly orgOptions = computed(() =>
    this.organizations().map(name => ({ label: name, value: name }))
  );

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private projectService: ProjectService,
    private userService: UserService,
    private commandService: CommandService
  ) {}

  async ngOnInit(): Promise<void> {
    const nameParam = this.route.snapshot.paramMap.get('name');
    this.isNew.set(nameParam === 'new' || !nameParam);

    try {
      const orgs = await this.userService.listOrganizations();
      this.organizations.set(orgs);

      if (!this.isNew() && nameParam) {
        const project = await this.projectService.getProject(nameParam);
        this.populateForm(project);
      }
    } finally {
      this.loading.set(false);
    }
  }

  async onSave(): Promise<void> {
    this.saving.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    try {
      const input: Record<string, unknown> = {
        projectName: this.isNew() ? null : this.originalName(),
        name: this.name(),
        description: this.description() || null,
        organizationName: this.organizationName() || null
      };

      const result = await this.commandService.execute('EditProject', input);
      if (result.success) {
        this.successMessage.set('Project saved successfully.');
        if (this.isNew()) {
          await this.router.navigate(['/projects', this.name()]);
        } else {
          // If name changed, update the route
          if (this.name() !== this.originalName()) {
            await this.router.navigate(['/projects', this.name()]);
          }
          this.originalName.set(this.name());
        }
      } else if (result.violations?.length) {
        this.errorMessage.set(result.violations.map(v => v.message).join('; '));
      } else {
        this.errorMessage.set(result.message ?? 'Save failed.');
      }
    } catch (err: unknown) {
      this.errorMessage.set(err instanceof Error ? err.message : 'Save failed.');
    } finally {
      this.saving.set(false);
    }
  }

  onCancel(): void {
    this.router.navigate(['/projects']);
  }

  private populateForm(project: ProjectDto): void {
    this.originalName.set(project.name);
    this.name.set(project.name);
    this.description.set(project.description ?? '');
    this.organizationName.set(project.organizationName ?? '');
  }
}
