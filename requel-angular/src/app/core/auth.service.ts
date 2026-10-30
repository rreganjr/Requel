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
import { Injectable, signal, computed } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ChangePasswordRequest, LoginRequest, LoginResponse, UserDto } from '../models/user';
import { CommandResult } from '../models/command';

const TOKEN_KEY = 'requel_token';
const USER_KEY = 'requel_user';

@Injectable({ providedIn: 'root' })
export class AuthService {

  readonly token = signal<string | null>(localStorage.getItem(TOKEN_KEY));
  readonly user = signal<UserDto | null>(this.loadStoredUser());
  readonly isAuthenticated = computed(() => this.token() !== null);
  /** #381: signed in, but nothing works until the password is changed. */
  readonly mustChangePassword = computed(() => this.user()?.mustChangePassword === true);

  constructor(private http: HttpClient, private router: Router) {}

  async login(request: LoginRequest): Promise<void> {
    const response = await firstValueFrom(
      this.http.post<LoginResponse>(`${environment.apiBaseUrl}/auth/login`, request)
    );
    this.setSession(response.token, response.user);
  }

  /**
   * #381: change the signed-in user's password. On success the server returns a fresh token (the
   * old one may carry the password-change restriction), which replaces the session. A refusal
   * (422) comes back as the CommandResult so the form can show its field violations; anything
   * else throws.
   */
  async changePassword(request: ChangePasswordRequest): Promise<CommandResult | null> {
    try {
      const response = await firstValueFrom(
        this.http.post<LoginResponse>(`${environment.apiBaseUrl}/auth/password`, request)
      );
      this.setSession(response.token, response.user);
      return null;
    } catch (err: unknown) {
      if (err instanceof HttpErrorResponse && err.status === 422 && err.error) {
        return err.error as CommandResult;
      }
      throw err;
    }
  }

  /**
   * #381: the server said this session must change its password (a 403 PASSWORD_CHANGE_REQUIRED),
   * so the stored user is out of date. Mark it, so the guard keeps the user on the change page.
   */
  markPasswordChangeRequired(): void {
    const user = this.user();
    if (user && !user.mustChangePassword) {
      const updated = { ...user, mustChangePassword: true };
      localStorage.setItem(USER_KEY, JSON.stringify(updated));
      this.user.set(updated);
    }
  }

  logout(): void {
    this.clearSession();
    this.router.navigate(['/login']);
  }

  private setSession(token: string, user: UserDto): void {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    this.token.set(token);
    this.user.set(user);
  }

  private clearSession(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.token.set(null);
    this.user.set(null);
  }

  private loadStoredUser(): UserDto | null {
    const json = localStorage.getItem(USER_KEY);
    if (!json) return null;
    try {
      return JSON.parse(json);
    } catch {
      return null;
    }
  }
}
