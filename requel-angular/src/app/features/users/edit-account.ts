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
import { Component, OnInit, signal, computed, ChangeDetectionStrategy } from '@angular/core';
import { PageHeaderComponent } from '../../shared/page-header';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { DirtyCheckable } from '../../core/dirty-check.guard';
import { InputText } from 'primeng/inputtext';
import { Password } from 'primeng/password';
import { SelectModule } from 'primeng/select';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import { AuthService } from '../../core/auth.service';
import { CommandService, isNetworkError } from '../../core/command.service';
import { UserService } from '../../core/user.service';
import { AppFieldComponent, AppFieldControlDirective } from '../../shared/app-field';
import { applyCommandErrors, clearServerErrors, passwordsMatch } from '../../shared/form-errors';
import { ARTIFACT_NAME_MAX_LENGTH, PASSWORD_MAX_LENGTH } from '../../shared/validation-limits';


/**
 * Separator for several command-level messages sharing the one page-level banner.
 * Semicolons, not spaces: two sentence fragments run together ("Email is invalid Phone
 * is required") read as one broken sentence. This is the separator the pre-#132 code
 * used and e2e/account.e2e.ts asserts.
 */
const SEPARATOR = '; ';

/**
 * Edit Account page — allows the current user to update their own profile.
 * Non-admin users can only change name, email, phone, password.
 * Role/permission editing is restricted to the admin user editor.
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-edit-account',
  standalone: true,
  imports: [
    PageHeaderComponent,
    ReactiveFormsModule,
    InputText,
    Password,
    SelectModule,
    ButtonModule,
    MessageModule,
    SubmitErrorComponent,
    AppFieldComponent,
    AppFieldControlDirective,
  ],
  template: `
    <div class="edit-account" data-testid="account-editor">
      <app-page-header title="Edit Account" />

      <app-submit-error
        [message]="errorMessage()"
        [retryable]="retryable()"
        (retry)="onSave()"
        testid="account-editor-error" />
      <div role="status" aria-live="polite">
        @if (successMessage()) {
          <p-message severity="success" [text]="successMessage()!" />
        }
      </div>

      <form [formGroup]="form" (ngSubmit)="onSave()">
        <!--
          controlId values match the ids the e2e page objects locate (#username, #name,
          #email, #password, #repassword). For the two p-password rows that means the id
          now lands on the INNER input rather than the p-password host, so the page
          objects' .locator('#password').locator('input') became .locator('#password') —
          updated in e2e/pages/UserEditorPage.ts and e2e/account.e2e.ts.
        -->
        <app-field label="Username" controlId="username" [control]="form.controls.username">
          <input pInputText appFieldControl id="username" formControlName="username" autocomplete="username" />
        </app-field>

        <app-field
          label="Name"
          controlId="name"
          [control]="form.controls.name"
          [submitted]="submitted()"
        >
          <input pInputText appFieldControl id="name" formControlName="name" autocomplete="name"
                 [attr.maxlength]="nameMaxLength" />
        </app-field>

        <app-field
          label="Email"
          controlId="email"
          [control]="form.controls.emailAddress"
          [submitted]="submitted()"
        >
          <input pInputText appFieldControl id="email" type="email" formControlName="emailAddress"
                 autocomplete="email" />
        </app-field>

        <app-field
          label="Phone"
          controlId="phone"
          [control]="form.controls.phoneNumber"
          [submitted]="submitted()"
        >
          <input pInputText appFieldControl id="phone" formControlName="phoneNumber" autocomplete="tel" />
        </app-field>

        <app-field
          label="Organization"
          controlId="account-org-input"
          [control]="form.controls.organizationName"
          [submitted]="submitted()"
        >
          <p-select
            appFieldControl
            inputId="account-org-input"
            data-testid="account-organization"
            formControlName="organizationName"
            [options]="orgOptions()"
            [editable]="true"
            placeholder="Select or type organization"
          />
        </app-field>

        <app-field
          label="New Password"
          helper="Leave blank to keep your current password."
          controlId="password"
          [control]="form.controls.password"
          [submitted]="submitted()"
        >
          <p-password
            appFieldControl
            inputId="password"
            formControlName="password"
            [feedback]="false"
            [toggleMask]="true"
            autocomplete="new-password"
          />
        </app-field>

        <app-field
          label="Confirm New Password"
          controlId="repassword"
          [control]="form.controls.repassword"
          [submitted]="submitted()"
          [divider]="false"
        >
          <p-password
            appFieldControl
            inputId="repassword"
            formControlName="repassword"
            [feedback]="false"
            [toggleMask]="true"
            autocomplete="new-password"
          />
        </app-field>

        <div class="actions">
          <p-button type="submit" label="Save" icon="pi pi-check" data-testid="account-save"
                    [loading]="saving()"
                    [disabled]="form.invalid || form.pristine || saving()" />
        </div>
      </form>
    </div>
  `,
  styles: [`
    .edit-account { max-width: 500px; }
    /* app-field owns the rows (issue #132); the caller keeps control width. */
    app-field input, app-field p-password, app-field p-select { width: 100%; }
    .actions { margin-top: var(--rq-space-4); }
  `]
})
export class EditAccountComponent implements OnInit, DirtyCheckable {

  readonly saving = signal(false);
  readonly submitted = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly retryable = signal(false);
  readonly successMessage = signal<string | null>(null);

  readonly username = computed(() => this.authService.user()?.username ?? '');

  readonly organizations = signal<{label: string; value: string}[]>([]);
  readonly orgOptions = computed(() => this.organizations());

  /**
   * Mirrors the backend `@Size(max = ValidationLimits.ARTIFACT_NAME_MAX)` (#171). Bound with
   * `[attr.maxlength]` rather than `maxlength` on purpose: Angular's MaxLengthValidator directive
   * matches `[maxlength][formControl]`, so the plain binding would register a SECOND maxlength
   * validator on top of the one in the form definition. `attr.` sets the HTML attribute only, which
   * is all that is wanted here — the browser stops the typing, the form owns the validation.
   */
  readonly nameMaxLength = ARTIFACT_NAME_MAX_LENGTH;

  /**
   * The password rows are optional here — blank means "keep the current one" — and the
   * validators express that without a conditional branch: Angular's `minLength` returns
   * null for an empty value, so `minLength(1)` only bites once something is typed, and
   * `passwordsMatch` is satisfied when both rows are empty. `maxLength` mirrors
   * `UserImpl.MAX_PASSWORD_LENGTH`, the only password bound the server enforces.
   *
   * `username` is disabled rather than read-only, matching the previous markup and the
   * e2e assertion that it is disabled. Disabled controls are excluded from `form.value`,
   * so the save path reads `getRawValue()`.
   */
  readonly form = new FormGroup(
    {
      username: new FormControl('', { nonNullable: true }),
      name: new FormControl('', {
        validators: [Validators.required, Validators.maxLength(ARTIFACT_NAME_MAX_LENGTH)],
        nonNullable: true,
      }),
      emailAddress: new FormControl('', { validators: Validators.email, nonNullable: true }),
      phoneNumber: new FormControl('', { nonNullable: true }),
      organizationName: new FormControl('', { nonNullable: true }),
      password: new FormControl('', {
        validators: [Validators.minLength(1), Validators.maxLength(PASSWORD_MAX_LENGTH)],
        nonNullable: true,
      }),
      repassword: new FormControl('', { nonNullable: true }),
    },
    { validators: passwordsMatch('password', 'repassword') }
  );

  constructor(
    private authService: AuthService,
    private commandService: CommandService,
    private userService: UserService
  ) {
    this.form.controls.username.disable();
  }

  /** Derived from the form, so there is no NgForm ViewChild to reach through (#132). */
  hasUnsavedChanges(): boolean {
    return this.form.dirty;
  }

  async ngOnInit(): Promise<void> {
    const user = this.authService.user();
    if (user) {
      this.form.patchValue({
        username: user.username ?? '',
        name: user.name ?? '',
        emailAddress: user.emailAddress ?? '',
        phoneNumber: user.phoneNumber ?? '',
        organizationName: user.organizationName ?? '',
      });
      this.form.markAsPristine();
    }
    try {
      const orgs = await this.userService.listOrganizations();
      this.organizations.set(orgs.map(o => ({ label: o.name, value: o.name })));
    } catch {
      // org list is optional — dropdown still works as editable text input
    }
  }

  async onSave(): Promise<void> {
    this.submitted.set(true);
    // Before the validity check: a server error from the last attempt makes its control
    // invalid, so clearing afterwards would leave the form permanently unsubmittable.
    clearServerErrors(this.form);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.retryable.set(false);

    try {
      const value = this.form.getRawValue();
      const input: Record<string, unknown> = {
        username: this.username(),
        name: value.name,
        emailAddress: value.emailAddress,
        phoneNumber: value.phoneNumber,
        organizationName: value.organizationName || null
      };

      if (value.password) {
        input['password'] = value.password;
        input['repassword'] = value.repassword;
      }

      const result = await this.commandService.execute('EditUser', input);
      if (result.success) {
        this.successMessage.set('Account updated.');
        this.form.patchValue({ password: '', repassword: '' });
        this.form.markAsPristine();
        this.submitted.set(false);
        return;
      }

      const unresolved = applyCommandErrors(this.form, result.violations);
      this.retryable.set(isNetworkError(result));
      if (unresolved.length) {
        this.errorMessage.set(unresolved.join(SEPARATOR));
      } else if (!result.violations?.length) {
        this.errorMessage.set(result.error ?? 'Save failed.');
      }
    } catch (err: unknown) {
      this.errorMessage.set(err instanceof Error ? err.message : 'Save failed.');
    } finally {
      this.saving.set(false);
    }
  }
}
