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
import { Component, signal, ChangeDetectionStrategy, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Password } from 'primeng/password';
import { ButtonModule } from 'primeng/button';
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import { AuthService } from '../../core/auth.service';
import { AppCardComponent } from '../../shared/app-card';
import { AppFieldComponent, AppFieldControlDirective } from '../../shared/app-field';
import { applyCommandErrors, clearServerErrors, passwordsMatch } from '../../shared/form-errors';
import { PASSWORD_MAX_LENGTH } from '../../shared/validation-limits';

/**
 * Issue #381: change the signed-in user's password. A user who must change it (the built-in admin
 * on its default password, or anyone an admin flagged) is sent here by `authGuard` and can go
 * nowhere else until it is done; the server refuses every other call meanwhile. Outside the app
 * shell, like the login page, so nothing else on screen fires requests that would be refused.
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-change-password',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    Password,
    ButtonModule,
    SubmitErrorComponent,
    AppCardComponent,
    AppFieldComponent,
    AppFieldControlDirective,
  ],
  template: `
    <div class="change-password-container">
      <app-card class="change-password-card">
        <h1 class="change-password-title">Change your password</h1>
        @if (required) {
          <p class="change-password-lead" data-testid="change-password-required">
            You must choose a new password before you can continue.
          </p>
        }

        <app-submit-error [message]="errorMessage()" testid="change-password-error" />

        <form [formGroup]="form" (ngSubmit)="onSubmit()">
          <app-field
            label="Current Password"
            controlId="currentPassword"
            [control]="form.controls.currentPassword"
            [submitted]="submitted()"
            [divider]="false"
          >
            <p-password
              #currentInput
              appFieldControl
              inputId="currentPassword"
              formControlName="currentPassword"
              [feedback]="false"
              [toggleMask]="true"
              autocomplete="current-password"
            />
          </app-field>

          <app-field
            label="New Password"
            controlId="newPassword"
            [control]="form.controls.newPassword"
            [submitted]="submitted()"
            [divider]="false"
          >
            <p-password
              appFieldControl
              inputId="newPassword"
              formControlName="newPassword"
              [feedback]="false"
              [toggleMask]="true"
              autocomplete="new-password"
            />
          </app-field>

          <app-field
            label="Confirm New Password"
            controlId="confirmPassword"
            [control]="form.controls.confirmPassword"
            [submitted]="submitted()"
            [divider]="false"
          >
            <p-password
              appFieldControl
              inputId="confirmPassword"
              formControlName="confirmPassword"
              [feedback]="false"
              [toggleMask]="true"
              autocomplete="new-password"
            />
          </app-field>

          <div class="change-password-actions">
            <p-button
              type="button"
              label="Sign out"
              severity="secondary"
              [text]="true"
              data-testid="change-password-sign-out"
              (onClick)="onSignOut()"
            />
            <p-button
              type="submit"
              label="Change Password"
              data-testid="change-password-submit"
              [loading]="saving()"
              [disabled]="saving()"
            />
          </div>
        </form>
      </app-card>
    </div>
  `,
  styles: [
    `
      /* Same canvas and card sizing as the login page, which this replaces until it's done. */
      .change-password-container {
        display: flex;
        justify-content: center;
        align-items: center;
        box-sizing: border-box;
        min-height: 100dvh;
        padding: var(--rq-space-6);
        overflow: auto;
        background: var(--rq-canvas-bg);
      }
      .change-password-card {
        display: block;
        width: 100%;
        min-width: 20rem;
        max-width: 420px;
      }
      .change-password-title {
        margin: 0 0 var(--rq-space-2);
        font-size: 1.5rem;
        font-weight: 700;
        color: var(--p-text-color);
      }
      .change-password-lead {
        margin: 0 0 var(--rq-space-4);
        color: var(--p-text-muted-color);
      }
      .change-password-actions {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: var(--rq-space-3);
        margin-top: var(--rq-space-4);
      }
      p-password {
        width: 100%;
      }
    `,
  ],
})
export class ChangePasswordComponent implements AfterViewInit {
  /**
   * The client checks only what it can know: all three filled, the two new ones equal, and the
   * server's length bound. Whether the current password is right, and that the new one differs
   * from it, come back from the server as field violations.
   */
  readonly form = new FormGroup(
    {
      currentPassword: new FormControl('', { validators: Validators.required, nonNullable: true }),
      newPassword: new FormControl('', {
        validators: [Validators.required, Validators.maxLength(PASSWORD_MAX_LENGTH)],
        nonNullable: true,
      }),
      confirmPassword: new FormControl('', { validators: Validators.required, nonNullable: true }),
    },
    { validators: passwordsMatch('newPassword', 'confirmPassword') }
  );

  readonly submitted = signal(false);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);

  /** Read once: true when the user was sent here rather than choosing to come. */
  readonly required: boolean;

  @ViewChild('currentInput', { read: ElementRef }) private currentInput?: ElementRef<HTMLElement>;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {
    this.required = authService.mustChangePassword();
  }

  ngAfterViewInit(): void {
    this.currentInput?.nativeElement.querySelector('input')?.focus();
  }

  async onSubmit(): Promise<void> {
    this.submitted.set(true);
    clearServerErrors(this.form);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.errorMessage.set(null);
    try {
      const refusal = await this.authService.changePassword(this.form.getRawValue());
      if (!refusal) {
        await this.router.navigate(['/']);
        return;
      }
      const unresolved = applyCommandErrors(this.form, refusal.violations);
      if (unresolved.length) {
        this.errorMessage.set(unresolved.join('; '));
      } else if (!refusal.violations?.length) {
        this.errorMessage.set(refusal.error ?? 'Password not changed.');
      }
    } catch (err: unknown) {
      this.errorMessage.set(err instanceof Error ? err.message : 'Password not changed.');
    } finally {
      this.saving.set(false);
    }
  }

  onSignOut(): void {
    this.authService.logout();
  }
}
