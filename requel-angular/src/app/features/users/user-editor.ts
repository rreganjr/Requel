import { Component, OnInit, signal, computed } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { InputText } from 'primeng/inputtext';
import { Password } from 'primeng/password';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { SelectModule } from 'primeng/select';
import { MessageModule } from 'primeng/message';
import { UserDto } from '../../models/user';
import { RoleDto } from '../../models/role';
import { UserService } from '../../core/user.service';
import { CommandService } from '../../core/command.service';

@Component({
  selector: 'app-user-editor',
  standalone: true,
  imports: [FormsModule, InputText, Password, ButtonModule, CheckboxModule, SelectModule, MessageModule],
  template: `
    <div class="user-editor">
      <h2>{{ isNew() ? 'New User' : 'Edit User: ' + username() }}</h2>

      @if (errorMessage()) {
        <p-message severity="error" [text]="errorMessage()!" />
      }
      @if (successMessage()) {
        <p-message severity="success" [text]="successMessage()!" />
      }

      <form #userForm="ngForm" (ngSubmit)="onSave()">
        <div class="form-grid">
          <div class="field">
            <label for="username">Username</label>
            <input pInputText id="username" [(ngModel)]="username" name="username"
                   [disabled]="!isNew()" />
          </div>

          <div class="field">
            <label for="name">Name</label>
            <input pInputText id="name" [(ngModel)]="name" name="name" />
          </div>

          <div class="field">
            <label for="email">Email</label>
            <input pInputText id="email" [(ngModel)]="emailAddress" name="email" type="email" />
          </div>

          <div class="field">
            <label for="phone">Phone</label>
            <input pInputText id="phone" [(ngModel)]="phoneNumber" name="phone" />
          </div>

          <div class="field">
            <label for="org">Organization</label>
            <p-select id="org" [(ngModel)]="organizationName" name="org"
                      [options]="orgOptions()" [editable]="true"
                      placeholder="Select or type organization" />
          </div>

          <div class="field">
            <label for="password">Password</label>
            <p-password id="password" [(ngModel)]="password" name="password"
                        [feedback]="false" [toggleMask]="true" />
          </div>

          <div class="field">
            <label for="repassword">Confirm Password</label>
            <p-password id="repassword" [(ngModel)]="repassword" name="repassword"
                        [feedback]="false" [toggleMask]="true" />
          </div>
        </div>

        <div class="roles-section">
          <h3>Roles &amp; Permissions</h3>
          @for (role of availableRoles(); track role.roleName) {
            <div class="role-group">
              <label class="checkbox-label">
                <p-checkbox [(ngModel)]="selectedRoleNames" [name]="'role_' + role.roleName"
                            [value]="role.roleName" />
                {{ role.displayName }}
              </label>
              @if (isRoleSelected(role.roleName)) {
                <div class="permissions">
                  @for (perm of role.availablePermissions; track perm.name) {
                    <label class="checkbox-label">
                      <p-checkbox [(ngModel)]="selectedPermissions[role.roleName]"
                                  [name]="'perm_' + role.roleName + '_' + perm.name"
                                  [value]="perm.name" />
                      {{ perm.name }}
                    </label>
                  }
                </div>
              }
            </div>
          }
        </div>

        <div class="actions">
          <p-button type="submit" label="Save" icon="pi pi-check"
                    [loading]="saving()" [disabled]="!userForm.dirty" />
          <p-button label="Cancel" icon="pi pi-times" severity="secondary"
                    (onClick)="onCancel()" [outlined]="true" />
        </div>
      </form>
    </div>
  `,
  styles: [`
    .user-editor { max-width: 800px; }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-bottom: 1.5rem; }
    .field { display: flex; flex-direction: column; gap: 0.5rem; }
    .field label { font-weight: 500; }
    .field input, .field p-password, .field p-select { width: 100%; }
    .roles-section { margin-bottom: 1.5rem; }
    .roles-section h3 { margin: 0 0 0.75rem; }
    .role-group { margin-bottom: 0.75rem; }
    .checkbox-label { display: inline-flex; align-items: center; gap: 0.5rem; cursor: pointer; }
    .permissions { margin-left: 2rem; margin-top: 0.25rem; display: flex; flex-wrap: wrap; gap: 0.5rem; }
    .actions { display: flex; gap: 0.5rem; }
  `]
})
export class UserEditorComponent implements OnInit {

  readonly isNew = signal(true);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  // Form fields
  readonly username = signal('');
  readonly name = signal('');
  readonly emailAddress = signal('');
  readonly phoneNumber = signal('');
  readonly organizationName = signal('');
  readonly password = signal('');
  readonly repassword = signal('');

  // Roles & permissions
  readonly availableRoles = signal<RoleDto[]>([]);
  selectedRoleNames: string[] = [];
  selectedPermissions: Record<string, string[]> = {};

  // Organization dropdown
  readonly organizations = signal<string[]>([]);
  readonly orgOptions = computed(() =>
    this.organizations().map(name => ({ label: name, value: name }))
  );

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private userService: UserService,
    private commandService: CommandService
  ) {}

  async ngOnInit(): Promise<void> {
    const usernameParam = this.route.snapshot.paramMap.get('username');
    this.isNew.set(usernameParam === 'new' || !usernameParam);

    try {
      // Load reference data in parallel
      const [roles, orgs] = await Promise.all([
        this.userService.listRoles(),
        this.userService.listOrganizations()
      ]);
      this.availableRoles.set(roles);
      this.organizations.set(orgs);

      // Initialize permissions map
      for (const role of roles) {
        this.selectedPermissions[role.roleName] = [];
      }

      // Load existing user if editing
      if (!this.isNew() && usernameParam) {
        const user = await this.userService.getUser(usernameParam);
        this.populateForm(user);
      }
    } finally {
      this.loading.set(false);
    }
  }

  isRoleSelected(roleName: string): boolean {
    return this.selectedRoleNames.includes(roleName);
  }

  async onSave(): Promise<void> {
    this.saving.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    try {
      const input: Record<string, unknown> = {
        username: this.username(),
        name: this.name(),
        emailAddress: this.emailAddress(),
        phoneNumber: this.phoneNumber(),
        organizationName: this.organizationName(),
        editable: true,
        userRoleNames: this.selectedRoleNames,
        userRolePermissionNames: this.selectedPermissions
      };

      // Only include password if set
      if (this.password()) {
        input['password'] = this.password();
        input['repassword'] = this.repassword();
      }

      const result = await this.commandService.execute('EditUser', input);
      if (result.success) {
        this.successMessage.set('User saved successfully.');
        if (this.isNew()) {
          await this.router.navigate(['/users', this.username()]);
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
    this.router.navigate(['/users']);
  }

  private populateForm(user: UserDto): void {
    this.username.set(user.username);
    this.name.set(user.name ?? '');
    this.emailAddress.set(user.emailAddress ?? '');
    this.phoneNumber.set(user.phoneNumber ?? '');
    this.organizationName.set(user.organizationName ?? '');
    this.selectedRoleNames = [...user.roles];
    // Permissions come as flat list — need to map back to per-role structure
    // For now, assign all permissions to all selected roles (refined in Phase 1 polish)
    for (const roleName of user.roles) {
      this.selectedPermissions[roleName] = [...user.permissions];
    }
  }
}
