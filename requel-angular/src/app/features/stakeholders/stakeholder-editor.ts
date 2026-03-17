import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { SelectModule } from 'primeng/select';
import { CheckboxModule } from 'primeng/checkbox';
import { MessageModule } from 'primeng/message';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService } from 'primeng/api';
import { StakeholderDto } from '../../models/stakeholder';
import { StakeholderService } from '../../core/stakeholder.service';
import { CommandService } from '../../core/command.service';
import { ProjectService } from '../../core/project.service';
import { UserService } from '../../core/user.service';
import { PermissionService } from '../../core/permission.service';

@Component({
  selector: 'app-stakeholder-editor',
  standalone: true,
  imports: [FormsModule, ButtonModule, InputText, TextareaModule, SelectModule,
            CheckboxModule, MessageModule, ConfirmDialogModule],
  providers: [ConfirmationService],
  template: `
    <div class="stakeholder-editor">
      <div class="page-header">
        <h2>{{ isNew() ? (isUserType() ? 'Add User Stakeholder' : 'Add Non-User Stakeholder') : stakeholderName() }}</h2>
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
      @if (successMessage()) {
        <p-message severity="success" [text]="successMessage()!" />
      }

      <div class="form-grid">
        @if (isUserType()) {
          <label for="username">User</label>
          <p-select id="username" [(ngModel)]="username" [options]="userOptions()"
                    optionLabel="label" optionValue="value"
                    placeholder="Select a user" [filter]="true" />

          <label for="team">Team</label>
          <input id="team" pInputText [(ngModel)]="teamName" placeholder="Team name" />
        } @else {
          <label for="name">Name</label>
          <input id="name" pInputText [(ngModel)]="stakeholderName" placeholder="Stakeholder name" />

          <label for="text">Description</label>
          <textarea id="text" pTextarea [(ngModel)]="text" rows="4"
                    placeholder="Description of this stakeholder"></textarea>
        }
      </div>

      <div class="form-actions">
        <p-button label="Save" icon="pi pi-check" (onClick)="onSave()" [loading]="saving()" />
      </div>

      <p-confirmDialog />
    </div>
  `,
  styles: [`
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
    .page-actions { display: flex; gap: 0.5rem; }
    .form-grid { display: grid; grid-template-columns: 120px 1fr; gap: 0.75rem 1rem; align-items: center; max-width: 600px; }
    .form-actions { margin-top: 1rem; }
  `]
})
export class StakeholderEditorComponent implements OnInit, OnDestroy {
  isNew = signal(true);
  isUserType = signal(true);
  stakeholderName = signal('');
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  saving = signal(false);
  canDelete = signal(false);
  userOptions = signal<{ label: string; value: string }[]>([]);

  username = '';
  teamName = '';
  text = '';

  private projectName = '';
  private stakeholderId: number | null = null;
  private version: number | null = null;
  private paramSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private stakeholderService: StakeholderService,
    private commandService: CommandService,
    private projectService: ProjectService,
    private userService: UserService,
    private permissionService: PermissionService,
    private confirmationService: ConfirmationService
  ) {}

  ngOnInit(): void {
    this.paramSub = this.route.paramMap.subscribe(params => {
      this.projectName = params.get('name') ?? '';
      this.canDelete.set(this.permissionService.canDelete('Stakeholder'));

      const idParam = params.get('stakeholderId') ?? '';
      if (idParam === 'new-user') {
        this.isNew.set(true);
        this.isUserType.set(true);
        this.loadUsers();
      } else if (idParam === 'new-nonuser') {
        this.isNew.set(true);
        this.isUserType.set(false);
      } else {
        this.isNew.set(false);
        this.stakeholderId = +idParam;
        this.loadStakeholder();
      }
    });
  }

  ngOnDestroy(): void {
    this.paramSub?.unsubscribe();
  }

  private async loadUsers(): Promise<void> {
    try {
      const users = await this.userService.listUsers();
      this.userOptions.set(users.map(u => ({ label: u.name || u.username, value: u.username })));
    } catch {
      this.errorMessage.set('Failed to load users.');
    }
  }

  private async loadStakeholder(): Promise<void> {
    try {
      const s = await this.stakeholderService.getStakeholder(this.projectName, this.stakeholderId!);
      this.stakeholderName.set(s.name);
      this.version = s.version;
      this.isUserType.set(s.type === 'user');

      if (s.userDetails) {
        this.username = s.userDetails.username;
        this.teamName = s.userDetails.teamName ?? '';
        await this.loadUsers();
      } else if (s.nonUserDetails) {
        this.text = s.nonUserDetails.text;
      }
    } catch {
      this.errorMessage.set('Failed to load stakeholder.');
    }
  }

  async onSave(): Promise<void> {
    this.saving.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    try {
      if (this.isUserType()) {
        const input: Record<string, unknown> = {
          projectName: this.projectName,
          username: this.username,
          teamName: this.teamName || null,
          permissionKeys: [], // TODO: permission editor
        };
        if (this.version != null) input['version'] = this.version;
        const result = await this.commandService.execute('EditUserStakeholder', input);
        if (result.success) {
          this.successMessage.set('Stakeholder saved.');
          if (this.isNew()) {
            this.projectService.notifyTreeChanged();
            if (result.entity) {
              const saved = result.entity as StakeholderDto;
              this.router.navigate(['..', saved.id], { relativeTo: this.route });
            }
          }
        } else {
          this.errorMessage.set(result.error ?? 'Save failed.');
        }
      } else {
        const input: Record<string, unknown> = {
          projectName: this.projectName,
          name: this.stakeholderName(),
          text: this.text,
        };
        if (this.version != null) input['version'] = this.version;
        const result = await this.commandService.execute('EditNonUserStakeholder', input);
        if (result.success) {
          this.successMessage.set('Stakeholder saved.');
          if (this.isNew()) {
            this.projectService.notifyTreeChanged();
            if (result.entity) {
              const saved = result.entity as StakeholderDto;
              this.router.navigate(['..', saved.id], { relativeTo: this.route });
            }
          }
        } else {
          this.errorMessage.set(result.error ?? 'Save failed.');
        }
      }
    } catch {
      this.errorMessage.set('An unexpected error occurred.');
    } finally {
      this.saving.set(false);
    }
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
