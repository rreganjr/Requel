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
import { InputText } from 'primeng/inputtext';
import { Password } from 'primeng/password';
import { ButtonModule } from 'primeng/button';
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import { AuthService } from '../../core/auth.service';
import { AppCardComponent } from '../../shared/app-card';
import { AppFieldComponent, AppFieldControlDirective } from '../../shared/app-field';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-login',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    InputText,
    Password,
    ButtonModule,
    SubmitErrorComponent,
    AppCardComponent,
    AppFieldComponent,
    AppFieldControlDirective,
  ],
  template: `
    <div class="login-container">
      <app-card class="login-card">
        <div class="login-brand">
          <img class="login-logo" src="images/logo_robot.png" alt="" />
          <h1 class="login-wordmark">Requel</h1>
        </div>
        <p class="login-tagline">Requirements Elicitation System</p>

        <app-submit-error [message]="errorMessage()" testid="login-error" />

        <form [formGroup]="form" (ngSubmit)="onLogin()">
          <!--
            No dividers: two rows in a login card read as one block, and a hairline
            between them is chrome this form does not need. The card is 400px, under
            app-field's 30rem container query, so each row stacks label above control
            on its own — which is what a login form should look like.
          -->
          <app-field
            label="Username"
            controlId="username"
            [control]="form.controls.username"
            [submitted]="submitted()"
            [divider]="false"
          >
            <input
              #usernameInput
              pInputText
              appFieldControl
              id="username"
              formControlName="username"
              autocomplete="username"
            />
          </app-field>

          <app-field
            label="Password"
            controlId="password"
            [control]="form.controls.password"
            [submitted]="submitted()"
            [divider]="false"
          >
            <p-password
              appFieldControl
              inputId="password"
              formControlName="password"
              [feedback]="false"
              [toggleMask]="true"
              autocomplete="current-password"
            />
          </app-field>

          <p-button
            type="submit"
            label="Login"
            data-testid="login-submit"
            [loading]="loading()"
            [disabled]="form.invalid || loading()"
            styleClass="w-full"
          />
        </form>
      </app-card>
    </div>
  `,
  styles: [
    `
      .login-container {
        display: flex;
        justify-content: center;
        align-items: center;
        box-sizing: border-box;
        /* Center in the viewport with breathing room. dvh (not vh) so mobile browser
           chrome doesn't push the content past the viewport and force a stray
           scrollbar (L1, L2); border-box keeps the padding inside that height.
           overflow:auto so once the window is shorter/narrower than the card + its
           min-width floor, the viewport scrolls instead of the card distorting (L3). */
        min-height: 100dvh;
        padding: var(--rq-space-6);
        overflow: auto;
        /* Muted, editor-style canvas so the white card reads as raised (L8). */
        background: var(--rq-canvas-bg);
      }
      /* The surface (bg, padding, border, radius, shadow) comes from app-card
         (issue #156); only the login-specific width constraints stay here. The
         min-width floor stops the card collapsing as the window shrinks (L3). */
      .login-card {
        display: block;
        width: 100%;
        min-width: 20rem;
        max-width: 400px;
      }
      /* Robot logo + "Requel" wordmark on one line, tagline beneath (L5, L7). */
      .login-brand {
        display: flex;
        /* baseline so the logo's bottom sits on the wordmark's baseline (its
           feet line up with the bottom of the letters), not floating centered. */
        align-items: baseline;
        justify-content: center;
        gap: var(--rq-space-3);
        margin-bottom: var(--rq-space-1);
      }
      .login-logo {
        height: 2.5rem;
        width: auto;
      }
      .login-wordmark {
        margin: 0;
        font-size: 2.75rem;
        font-weight: 700;
        line-height: 1;
        letter-spacing: 0.02em;
        color: var(--p-text-color);
      }
      .login-tagline {
        text-align: center;
        color: var(--p-text-muted-color);
        margin: 0 0 var(--rq-space-6);
      }
      /* The controls are this component's own nodes, projected through app-field, so
         they still carry this component's encapsulation attribute and an ordinary
         rule reaches them. app-field owns the row; the caller owns control width. */
      input,
      p-password {
        width: 100%;
      }
    `,
  ],
})
export class LoginComponent implements AfterViewInit {
  /**
   * Both fields are required, which is the whole client-side contract — the server
   * decides whether the credentials are *correct*, and that answer arrives as a
   * page-level error rather than a field one (see {@link onLogin}).
   */
  readonly form = new FormGroup({
    username: new FormControl('', { validators: Validators.required, nonNullable: true }),
    password: new FormControl('', { validators: Validators.required, nonNullable: true }),
  });

  readonly submitted = signal(false);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  @ViewChild('usernameInput') private usernameInput?: ElementRef<HTMLInputElement>;

  /** Land the cursor in the username field on load so a user can type straight away. */
  ngAfterViewInit(): void {
    this.usernameInput?.nativeElement.focus();
  }

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  async onLogin(): Promise<void> {
    this.submitted.set(true);
    if (this.form.invalid) {
      // Submit can still fire on Enter even with the button disabled.
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    // Disabled rather than left editable mid-request, matching the previous
    // `[disabled]="loading()"` on each input. Done here instead of in the template
    // because binding `disabled` on a reactive control is what Angular warns about.
    this.form.disable({ emitEvent: false });
    try {
      const { username, password } = this.form.getRawValue();
      await this.authService.login({ username, password });
      await this.router.navigate(['/']);
    } catch (err: unknown) {
      // A failed login is about the credential *pair*, not about one field — there is
      // no field to attach it to, so it stays page-level. This is also why login does
      // not use applyCommandErrors: /auth/login is not a command endpoint and returns
      // no field violations.
      this.errorMessage.set(
        err instanceof Error ? err.message : 'Login failed. Check your credentials.'
      );
    } finally {
      this.form.enable({ emitEvent: false });
      this.loading.set(false);
    }
  }
}
