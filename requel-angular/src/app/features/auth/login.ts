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
import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { InputText } from 'primeng/inputtext';
import { Password } from 'primeng/password';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, InputText, Password, ButtonModule, MessageModule],
  template: `
    <div class="login-container">
      <div class="login-card">
        <h2>Requel</h2>
        <p class="subtitle">Requirements Elicitation System</p>

        @if (errorMessage()) {
          <p-message severity="error" [text]="errorMessage()!" />
        }

        <form (ngSubmit)="onLogin()">
          <div class="field">
            <label for="username">Username</label>
            <input pInputText id="username" [(ngModel)]="username" name="username"
                   [disabled]="loading()" autocomplete="username" />
          </div>

          <div class="field">
            <label for="password">Password</label>
            <p-password id="password" [(ngModel)]="password" name="password"
                        [disabled]="loading()" [feedback]="false" [toggleMask]="true"
                        autocomplete="current-password" />
          </div>

          <p-button type="submit" label="Login" [loading]="loading()"
                    [disabled]="!username() || !password()" styleClass="w-full" />
        </form>
      </div>
    </div>
  `,
  styles: [`
    .login-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      background: var(--p-surface-ground);
    }
    .login-card {
      background: var(--p-surface-card);
      padding: 2rem;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      width: 100%;
      max-width: 400px;
    }
    h2 { margin: 0 0 0.25rem; text-align: center; }
    .subtitle { text-align: center; color: var(--p-text-muted-color); margin: 0 0 1.5rem; }
    .field { margin-bottom: 1rem; }
    .field label { display: block; margin-bottom: 0.5rem; font-weight: 500; }
    .field input, .field p-password { width: 100%; }
  `]
})
export class LoginComponent {

  readonly username = signal('');
  readonly password = signal('');
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  constructor(private authService: AuthService, private router: Router) {}

  async onLogin(): Promise<void> {
    this.loading.set(true);
    this.errorMessage.set(null);
    try {
      await this.authService.login({
        username: this.username(),
        password: this.password()
      });
      await this.router.navigate(['/']);
    } catch (err: unknown) {
      this.errorMessage.set(
        err instanceof Error ? err.message : 'Login failed. Check your credentials.'
      );
    } finally {
      this.loading.set(false);
    }
  }
}
