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
import { ChangeDetectorRef, Component, OnDestroy, OnInit, signal, computed, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { FormsModule, NgForm } from '@angular/forms';
import { DirtyCheckable } from '../../core/dirty-check.guard';
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
    <div class="user-editor" data-testid="user-editor">
      <h2>{{ isNew() ? 'New User' : 'Edit User: ' + username }}</h2>

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
            <p-select id="org" inputId="userOrgInput" data-testid="user-organization"
                      [(ngModel)]="organizationName" name="org"
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

        <div class="roles-section" data-testid="user-roles-section">
          <h3>Roles &amp; Permissions</h3>
          @for (role of availableRoles(); track role.roleName) {
            <div class="role-group" data-testid="user-role-group" [attr.data-role-name]="role.roleName">
              <label class="checkbox-label" data-testid="user-role-label">
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
          <p-button type="submit" label="Save" icon="pi pi-check" data-testid="user-save"
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
export class UserEditorComponent implements OnInit, OnDestroy, DirtyCheckable {

  @ViewChild('userForm') private viewUserForm?: NgForm;

  readonly isNew = signal(true);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  // Form fields — plain properties so [(ngModel)] two-way binding works correctly.
  // readonly signals don't update via the (ngModelChange)="name=$event" write path.
  username = '';
  name = '';
  emailAddress = '';
  phoneNumber = '';
  organizationName = '';
  password = '';
  repassword = '';

  // Identity for optimistic locking
  private userId: number | null = null;
  private userVersion: number = 0;

  // Roles & permissions
  readonly availableRoles = signal<RoleDto[]>([]);
  selectedRoleNames: string[] = [];
  selectedPermissions: Record<string, string[]> = {};

  // Organization dropdown
  readonly organizations = signal<{label: string; value: string}[]>([]);
  readonly orgOptions = computed(() => this.organizations());

  private paramSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private userService: UserService,
    private commandService: CommandService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.paramSub = this.route.paramMap.subscribe(params => {
      const usernameParam = params.get('username');
      this.isNew.set(usernameParam === 'new' || !usernameParam);
      this.loadData(usernameParam);
    });
  }

  hasUnsavedChanges(): boolean {
    return this.viewUserForm?.dirty ?? false;
  }

  ngOnDestroy(): void {
    this.paramSub?.unsubscribe();
  }

  private async loadData(usernameParam: string | null): Promise<void> {
    this.loading.set(true);
    this.userId = null;
    this.userVersion = 0;
    try {
      // Load reference data in parallel
      const [roles, orgs] = await Promise.all([
        this.userService.listRoles(),
        this.userService.listOrganizations()
      ]);
      this.availableRoles.set(roles);
      this.organizations.set(orgs.map(o => ({ label: o.name, value: o.name })));

      // Initialize permissions map
      for (const role of roles) {
        this.selectedPermissions[role.roleName] = [];
      }

      // Load existing user if editing
      if (!this.isNew() && usernameParam) {
        const user = await this.userService.getUser(usernameParam);
        this.populateForm(user);
        // Force CD so the @if(isRoleSelected) block and p-checkbox components
        // initialize from the already-populated values in a single pass.
        // Without this, PrimeNG checkboxes can initialize from the empty
        // selectedPermissions set by the init loop above before populateForm runs.
        this.cdr.detectChanges();
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
        id: this.userId,
        version: this.userVersion,
        username: this.username,
        name: this.name,
        emailAddress: this.emailAddress,
        phoneNumber: this.phoneNumber,
        organizationName: this.organizationName,
        editable: true,
        userRoleNames: this.selectedRoleNames,
        userRolePermissionNames: this.selectedPermissions
      };

      // Only include password if set
      if (this.password) {
        input['password'] = this.password;
        input['repassword'] = this.repassword;
      }

      const result = await this.commandService.execute('EditUser', input);
      if (result.success) {
        this.successMessage.set('User saved successfully.');
        this.viewUserForm?.form.markAsPristine();
        if (this.isNew()) {
          await this.router.navigate(['/users', this.username]);
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

  onCancel(): void {
    this.router.navigate(['/users']);
  }

  private populateForm(user: UserDto): void {
    this.userId = user.id ?? null;
    this.userVersion = user.version ?? 0;
    this.username = user.username;
    this.name = user.name ?? '';
    this.emailAddress = user.emailAddress ?? '';
    this.phoneNumber = user.phoneNumber ?? '';
    this.organizationName = user.organizationName ?? '';
    this.selectedRoleNames = [...user.roles];
    // Seed per-role permissions from the server's permissionsByRole map
    for (const roleName of user.roles) {
      this.selectedPermissions[roleName] = [...(user.permissionsByRole?.[roleName] ?? [])];
    }
  }
}
