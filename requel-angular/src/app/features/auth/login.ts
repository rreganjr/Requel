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
