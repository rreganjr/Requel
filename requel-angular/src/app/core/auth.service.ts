import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { LoginRequest, LoginResponse, UserDto } from '../models/user';

const TOKEN_KEY = 'requel_token';
const USER_KEY = 'requel_user';

@Injectable({ providedIn: 'root' })
export class AuthService {

  readonly token = signal<string | null>(localStorage.getItem(TOKEN_KEY));
  readonly user = signal<UserDto | null>(this.loadStoredUser());
  readonly isAuthenticated = computed(() => this.token() !== null);

  constructor(private http: HttpClient, private router: Router) {}

  async login(request: LoginRequest): Promise<void> {
    const response = await firstValueFrom(
      this.http.post<LoginResponse>(`${environment.apiBaseUrl}/auth/login`, request)
    );
    this.setSession(response.token, response.user);
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
