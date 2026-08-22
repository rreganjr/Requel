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
import { Component, OnDestroy, OnInit, signal, computed } from '@angular/core';
import { PageHeaderComponent } from '../../shared/page-header';
import { AppCardComponent } from '../../shared/app-card';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { DirtyCheckable } from '../../core/dirty-check.guard';
import { InputText } from 'primeng/inputtext';
import { Password } from 'primeng/password';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { SelectModule } from 'primeng/select';
import { MessageModule } from 'primeng/message';
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import { UserDto } from '../../models/user';
import { RoleDto } from '../../models/role';
import { UserService } from '../../core/user.service';
import { CommandService, isNetworkError } from '../../core/command.service';
import { LoadingStateComponent } from '../../shared/loading-state';
import { ErrorStateComponent } from '../../shared/error-state';
import { AppFieldComponent, AppFieldControlDirective } from '../../shared/app-field';
import { AppFieldGroupComponent } from '../../shared/app-field-group';
import {
  applyCommandErrors,
  atLeastOne,
  clearServerErrors,
  firstErrorMessage,
  passwordsMatch,
} from '../../shared/form-errors';
import { ARTIFACT_NAME_MAX_LENGTH, PASSWORD_MAX_LENGTH } from '../../shared/validation-limits';


/**
 * Separator for several command-level messages sharing the one page-level banner.
 * Semicolons, not spaces: two sentence fragments run together ("Email is invalid Phone
 * is required") read as one broken sentence. This is the separator the pre-#132 code
 * used and e2e/account.e2e.ts asserts.
 */
const SEPARATOR = '; ';

@Component({
  selector: 'app-user-editor',
  standalone: true,
  imports: [
    PageHeaderComponent,
    AppCardComponent,
    ReactiveFormsModule,
    InputText,
    Password,
    ButtonModule,
    CheckboxModule,
    SelectModule,
    MessageModule,
    SubmitErrorComponent,
    LoadingStateComponent,
    ErrorStateComponent,
    AppFieldComponent,
    AppFieldControlDirective,
    AppFieldGroupComponent,
  ],
  template: `
    <div class="user-editor" data-testid="user-editor">
      <app-page-header [title]="isNew() ? 'New User' : 'Edit User: ' + form.controls.username.value" />

      <app-submit-error
        [message]="errorMessage()"
        [retryable]="retryable()"
        (retry)="onSave()"
        testid="user-editor-error" />
      <div role="status" aria-live="polite">
        @if (successMessage()) {
          <p-message severity="success" [text]="successMessage()!" />
        }
      </div>

      @if (loading()) {
        <app-card>
          <app-loading-state label="Loading user…" [lines]="5" testid="user-editor-loading" />
        </app-card>
      } @else if (loadError()) {
        <app-error-state [message]="loadError()!" testid="user-editor-load-error"
                         (retry)="retryLoad()" />
      } @else {
      <app-card>
        <form [formGroup]="form" (ngSubmit)="onSave()">
          <!--
            The two-column group from issue #172 keeps this form dense; migrating seven
            fields to single-column rows would have turned it into a long scroll. Seven
            rows over two columns leaves a partial final row, which is exactly the case
            app-field-group suppresses the trailing divider for.

            controlId values match the ids the e2e page objects locate. For #password and
            #repassword the id now lands on the INNER input rather than the p-password
            host, so UserEditorPage's .locator('#password').locator('input') became
            .locator('#password').
          -->
          <app-field-group [columns]="2">
            <app-field label="Username" controlId="username" [control]="form.controls.username"
                       [submitted]="submitted()">
              <input pInputText appFieldControl id="username" formControlName="username"
                     [attr.maxlength]="nameMaxLength" />
            </app-field>

            <app-field label="Name" controlId="name" [control]="form.controls.name"
                       [submitted]="submitted()">
              <input pInputText appFieldControl id="name" formControlName="name"
                     [attr.maxlength]="nameMaxLength" />
            </app-field>

            <app-field label="Email" controlId="email" [control]="form.controls.emailAddress"
                       [submitted]="submitted()">
              <input pInputText appFieldControl id="email" type="email" formControlName="emailAddress" />
            </app-field>

            <app-field label="Phone" controlId="phone" [control]="form.controls.phoneNumber"
                       [submitted]="submitted()">
              <input pInputText appFieldControl id="phone" formControlName="phoneNumber" />
            </app-field>

            <app-field label="Organization" controlId="userOrgInput"
                       [control]="form.controls.organizationName" [submitted]="submitted()">
              <p-select appFieldControl inputId="userOrgInput" data-testid="user-organization"
                        formControlName="organizationName"
                        [options]="orgOptions()" [editable]="true" appendTo="body"
                        placeholder="Select or type organization" />
            </app-field>

            <app-field label="Password" controlId="password" [control]="form.controls.password"
                       [submitted]="submitted()"
                       [helper]="isNew() ? '' : 'Leave blank to keep the current password.'">
              <p-password appFieldControl inputId="password" formControlName="password"
                          [feedback]="false" [toggleMask]="true" autocomplete="new-password" />
            </app-field>

            <app-field label="Confirm Password" controlId="repassword"
                       [control]="form.controls.repassword" [submitted]="submitted()">
              <p-password appFieldControl inputId="repassword" formControlName="repassword"
                          [feedback]="false" [toggleMask]="true" autocomplete="new-password" />
            </app-field>
          </app-field-group>

          <div class="roles-section" data-testid="user-roles-section">
            <!-- h2, not h3: sibling of the section headings #158 standardised, and an h3
                 straight after the page h1 is an axe heading-order violation. -->
            <h2 class="rq-section-title">Roles &amp; Permissions</h2>

            @for (role of availableRoles(); track role.roleName) {
              <div class="role-group" data-testid="user-role-group" [attr.data-role-name]="role.roleName">
                <label class="checkbox-label" data-testid="user-role-label">
                  <p-checkbox [formControl]="form.controls.userRoleNames" [value]="role.roleName" />
                  {{ role.displayName }}
                </label>
                @if (isRoleSelected(role.roleName)) {
                  <div class="permissions">
                    @for (perm of role.availablePermissions; track perm.name) {
                      <label class="checkbox-label">
                        <p-checkbox [formControl]="permissionsControl(role.roleName)" [value]="perm.name" />
                        {{ perm.name }}
                      </label>
                    }
                  </div>
                }
              </div>
            }

            <!--
              The roles error has no single control to sit under — it is about the group —
              so it renders here, next to the checkboxes it concerns, with the same
              role="alert" treatment an app-field error gets.
            -->
            @if (showRolesError()) {
              <p class="roles-error" role="alert" data-testid="user-roles-error">
                {{ rolesErrorMessage() }}
              </p>
            }
          </div>

          <div class="actions">
            <p-button type="submit" label="Save" icon="pi pi-check" data-testid="user-save"
                      [loading]="saving()"
                      [disabled]="form.invalid || form.pristine || saving()" />
            <p-button type="button" label="Cancel" icon="pi pi-times" severity="secondary"
                      (onClick)="onCancel()" [outlined]="true" />
          </div>
        </form>
      </app-card>
      }
    </div>
  `,
  styles: [`
    .user-editor { max-width: 800px; }
    /* The local .form-grid { 1fr 1fr } is gone — app-field-group owns the columns now
       (issue #172), and app-field owns each row. */
    app-field input, app-field p-password, app-field p-select { width: 100%; }
    .roles-section { margin-block: var(--rq-space-6); }
    .roles-section h2 { margin: 0 0 var(--rq-space-3); }
    .role-group { margin-bottom: var(--rq-space-3); }
    .checkbox-label { display: inline-flex; align-items: center; gap: var(--rq-space-2); cursor: pointer; }
    .permissions { margin-left: var(--rq-space-8); margin-top: var(--rq-space-1); display: flex; flex-wrap: wrap; gap: var(--rq-space-2); }
    .roles-error {
      margin: var(--rq-space-1) 0 0;
      color: var(--rq-field-error-fg);
      font-size: var(--rq-text-helper-size);
      line-height: var(--rq-text-helper-line);
    }
    .actions { display: flex; gap: var(--rq-space-2); }
  `]
})
export class UserEditorComponent implements OnInit, OnDestroy, DirtyCheckable {

  readonly isNew = signal(true);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly submitted = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly retryable = signal(false);
  readonly successMessage = signal<string | null>(null);
  // Load failures tracked separately from save/inline errors so the retryable
  // error state replaces the form only when the initial load fails.
  readonly loadError = signal<string | null>(null);
  private lastUsernameParam: string | null = null;

  /**
   * Mirrors the backend `@Size(max = ValidationLimits.ARTIFACT_NAME_MAX)` (#171). Bound with
   * `[attr.maxlength]` rather than `maxlength` on purpose: Angular's MaxLengthValidator directive
   * matches `[maxlength][formControl]`, so the plain binding would register a SECOND maxlength
   * validator on top of the one in the form definition. `attr.` sets the HTML attribute only, which
   * is all that is wanted here — the browser stops the typing, the form owns the validation.
   */
  readonly nameMaxLength = ARTIFACT_NAME_MAX_LENGTH;

  /**
   * `permissions` is a nested group with one `string[]` control per role, filled in once
   * the role list arrives. Keeping it inside the form is what makes ticking a permission
   * mark the form dirty — with the checkboxes outside the form, Save would have stayed
   * disabled after a permission-only change.
   *
   * Password is required on create and optional on edit; {@link applyPasswordRules} sets
   * that once `isNew` is known. `minLength(1)` is a no-op on an empty value, so the edit
   * case needs no conditional branch beyond dropping `required`.
   */
  readonly form = new FormGroup(
    {
      username: new FormControl('', {
        validators: [Validators.required, Validators.maxLength(ARTIFACT_NAME_MAX_LENGTH)],
        nonNullable: true,
      }),
      name: new FormControl('', {
        validators: [Validators.required, Validators.maxLength(ARTIFACT_NAME_MAX_LENGTH)],
        nonNullable: true,
      }),
      emailAddress: new FormControl('', { validators: Validators.email, nonNullable: true }),
      phoneNumber: new FormControl('', { nonNullable: true }),
      organizationName: new FormControl('', { nonNullable: true }),
      password: new FormControl('', { nonNullable: true }),
      repassword: new FormControl('', { nonNullable: true }),
      userRoleNames: new FormControl<string[]>([], { validators: atLeastOne(), nonNullable: true }),
      permissions: new FormGroup<Record<string, FormControl<string[]>>>({}),
    },
    { validators: passwordsMatch('password', 'repassword') }
  );

  // Identity for optimistic locking
  private userId: number | null = null;
  private userVersion: number = 0;

  readonly availableRoles = signal<RoleDto[]>([]);

  // Organization dropdown
  readonly organizations = signal<{label: string; value: string}[]>([]);
  readonly orgOptions = computed(() => this.organizations());

  private paramSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private userService: UserService,
    private commandService: CommandService
  ) {}

  ngOnInit(): void {
    this.paramSub = this.route.paramMap.subscribe(params => {
      const usernameParam = params.get('username');
      this.isNew.set(usernameParam === 'new' || !usernameParam);
      this.loadData(usernameParam);
    });
  }

  /** Derived from the form, so there is no NgForm ViewChild to reach through (#132). */
  hasUnsavedChanges(): boolean {
    return this.form.dirty;
  }

  ngOnDestroy(): void {
    this.paramSub?.unsubscribe();
  }

  /** Re-run the last attempted load; wired to the error state's (retry) output. */
  retryLoad(): void {
    void this.loadData(this.lastUsernameParam);
  }

  /** The per-role permission control, created lazily so the template can bind it. */
  permissionsControl(roleName: string): FormControl<string[]> {
    const group = this.form.controls.permissions;
    let control = group.controls[roleName];
    if (!control) {
      control = new FormControl<string[]>([], { nonNullable: true });
      group.addControl(roleName, control);
    }
    return control;
  }

  /**
   * Whether to show the "select at least one role" message. Same visibility rule
   * `app-field` applies to a control-level error: only after the user has engaged, or
   * after a save attempt — never on a create form nobody has filled in yet.
   */
  showRolesError(): boolean {
    const control = this.form.controls.userRoleNames;
    return control.invalid && (control.touched || this.submitted());
  }

  /**
   * The roles message, resolved through the shared map rather than written here — the
   * wording for `atLeastOne` (and for a `server` violation mapped onto this control)
   * lives in form-errors.ts with every other validation message.
   */
  rolesErrorMessage(): string | null {
    return firstErrorMessage(this.form.controls.userRoleNames);
  }

  private applyPasswordRules(): void {
    const password = this.form.controls.password;
    password.setValidators(
      this.isNew()
        ? [Validators.required, Validators.maxLength(PASSWORD_MAX_LENGTH)]
        : [Validators.minLength(1), Validators.maxLength(PASSWORD_MAX_LENGTH)]
    );
    password.updateValueAndValidity({ emitEvent: false });
  }

  private async loadData(usernameParam: string | null): Promise<void> {
    this.loading.set(true);
    this.loadError.set(null);
    this.submitted.set(false);
    this.lastUsernameParam = usernameParam;
    this.userId = null;
    this.userVersion = 0;

    // Username is the identity of an existing user and cannot be changed; disabling the
    // control both matches the previous [disabled]="!isNew()" markup and keeps it out of
    // the validity calculation, which getRawValue() then reads past.
    if (this.isNew()) {
      this.form.controls.username.enable({ emitEvent: false });
    } else {
      this.form.controls.username.disable({ emitEvent: false });
    }
    this.applyPasswordRules();

    try {
      // Load reference data in parallel
      const [roles, orgs] = await Promise.all([
        this.userService.listRoles(),
        this.userService.listOrganizations()
      ]);
      this.availableRoles.set(roles);
      this.organizations.set(orgs.map(o => ({ label: o.name, value: o.name })));

      // A control per role, so a permission tick is a form change like any other.
      for (const role of roles) {
        this.permissionsControl(role.roleName).setValue([], { emitEvent: false });
      }

      // Load existing user if editing
      if (!this.isNew() && usernameParam) {
        const user = await this.userService.getUser(usernameParam);
        this.populateForm(user);
      }
      // The ChangeDetectorRef.detectChanges() that used to be needed here is gone: the
      // p-checkbox and p-select value accessors are written to directly by setValue, so
      // they no longer race the empty-then-populate sequence that plain properties had.
      this.form.markAsPristine();
    } catch (err: unknown) {
      // Previously uncaught: a failed load left a blank form with no feedback.
      this.loadError.set(err instanceof Error ? err.message : 'Failed to load user.');
    } finally {
      this.loading.set(false);
    }
  }

  isRoleSelected(roleName: string): boolean {
    return this.form.controls.userRoleNames.value.includes(roleName);
  }

  async onSave(): Promise<void> {
    this.submitted.set(true);
    // Before the validity check — see term-editor: a standing server error would
    // otherwise make the form permanently unsubmittable.
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
        id: this.userId,
        version: this.userVersion,
        username: value.username,
        name: value.name,
        emailAddress: value.emailAddress,
        phoneNumber: value.phoneNumber,
        organizationName: value.organizationName,
        editable: true,
        userRoleNames: value.userRoleNames,
        // Only the selected roles' permissions, so a deselected role does not ship a
        // stale permission list the server would have to ignore.
        userRolePermissionNames: this.selectedPermissionsPayload(value.userRoleNames),
      };

      // Only include password if set
      if (value.password) {
        input['password'] = value.password;
        input['repassword'] = value.repassword;
      }

      const result = await this.commandService.execute('EditUser', input);
      if (result.success) {
        this.successMessage.set('User saved successfully.');
        this.form.markAsPristine();
        this.submitted.set(false);
        if (this.isNew()) {
          await this.router.navigate(['/users', value.username]);
        }
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

  /** `{ roleName: permissionName[] }` for the selected roles only. */
  private selectedPermissionsPayload(roleNames: string[]): Record<string, string[]> {
    const payload: Record<string, string[]> = {};
    for (const roleName of roleNames) {
      payload[roleName] = this.permissionsControl(roleName).value;
    }
    return payload;
  }

  onCancel(): void {
    this.router.navigate(['/users']);
  }

  private populateForm(user: UserDto): void {
    this.userId = user.id ?? null;
    this.userVersion = user.version ?? 0;
    this.form.patchValue(
      {
        username: user.username,
        name: user.name ?? '',
        emailAddress: user.emailAddress ?? '',
        phoneNumber: user.phoneNumber ?? '',
        organizationName: user.organizationName ?? '',
        userRoleNames: [...user.roles],
      },
      { emitEvent: false }
    );
    // Seed per-role permissions from the server's permissionsByRole map
    for (const roleName of user.roles) {
      this.permissionsControl(roleName).setValue([...(user.permissionsByRole?.[roleName] ?? [])], {
        emitEvent: false,
      });
    }
  }
}
