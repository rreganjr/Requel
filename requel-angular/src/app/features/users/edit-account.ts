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
import { Component, OnInit, signal, computed, ViewChild } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { DirtyCheckable } from '../../core/dirty-check.guard';
import { InputText } from 'primeng/inputtext';
import { Password } from 'primeng/password';
import { SelectModule } from 'primeng/select';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { AuthService } from '../../core/auth.service';
import { CommandService } from '../../core/command.service';
import { UserService } from '../../core/user.service';

/**
 * Edit Account page — allows the current user to update their own profile.
 * Non-admin users can only change name, email, phone, password.
 * Role/permission editing is restricted to the admin user editor.
 */
@Component({
  selector: 'app-edit-account',
  standalone: true,
  imports: [FormsModule, InputText, Password, SelectModule, ButtonModule, MessageModule],
  template: `
    <div class="edit-account">
      <h2>Edit Account</h2>

      @if (errorMessage()) {
        <p-message severity="error" [text]="errorMessage()!" />
      }
      @if (successMessage()) {
        <p-message severity="success" [text]="successMessage()!" />
      }

      <form #accountForm="ngForm" (ngSubmit)="onSave()">
        <div class="field">
          <label for="username">Username</label>
          <input pInputText id="username" [ngModel]="username()" name="username" disabled />
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
          <label for="password">New Password (leave blank to keep current)</label>
          <p-password id="password" [(ngModel)]="password" name="password"
                      [feedback]="false" [toggleMask]="true" />
        </div>

        <div class="field">
          <label for="repassword">Confirm New Password</label>
          <p-password id="repassword" [(ngModel)]="repassword" name="repassword"
                      [feedback]="false" [toggleMask]="true" />
        </div>

        <div class="actions">
          <p-button type="submit" label="Save" icon="pi pi-check"
                    [loading]="saving()" [disabled]="!accountForm.dirty" />
        </div>
      </form>
    </div>
  `,
  styles: [`
    .edit-account { max-width: 500px; }
    .field { margin-bottom: 1rem; display: flex; flex-direction: column; gap: 0.5rem; }
    .field label { font-weight: 500; }
    .field input, .field p-password, .field p-select { width: 100%; }
    .actions { margin-top: 1rem; }
  `]
})
export class EditAccountComponent implements OnInit, DirtyCheckable {

  @ViewChild('accountForm') private viewAccountForm?: NgForm;

  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly username = computed(() => this.authService.user()?.username ?? '');
  readonly name = signal('');
  readonly emailAddress = signal('');
  readonly phoneNumber = signal('');
  readonly organizationName = signal('');
  readonly password = signal('');
  readonly repassword = signal('');

  readonly organizations = signal<{label: string; value: string}[]>([]);
  readonly orgOptions = computed(() => this.organizations());

  constructor(
    private authService: AuthService,
    private commandService: CommandService,
    private userService: UserService
  ) {}

  hasUnsavedChanges(): boolean {
    return this.viewAccountForm?.dirty ?? false;
  }

  async ngOnInit(): Promise<void> {
    const user = this.authService.user();
    if (user) {
      this.name.set(user.name ?? '');
      this.emailAddress.set(user.emailAddress ?? '');
      this.phoneNumber.set(user.phoneNumber ?? '');
      this.organizationName.set(user.organizationName ?? '');
    }
    try {
      const orgs = await this.userService.listOrganizations();
      this.organizations.set(orgs.map(o => ({ label: o.name, value: o.name })));
    } catch {
      // org list is optional — dropdown still works as editable text input
    }
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
        organizationName: this.organizationName() || null
      };

      if (this.password()) {
        input['password'] = this.password();
        input['repassword'] = this.repassword();
      }

      const result = await this.commandService.execute('EditUser', input);
      if (result.success) {
        this.successMessage.set('Account updated.');
        this.password.set('');
        this.repassword.set('');
        this.viewAccountForm?.form.markAsPristine();
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
}
