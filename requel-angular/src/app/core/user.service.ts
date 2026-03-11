import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { UserDto } from '../models/user';
import { RoleDto } from '../models/role';

/**
 * Service for user administration query endpoints.
 */
@Injectable({ providedIn: 'root' })
export class UserService {

  constructor(private http: HttpClient) {}

  async listUsers(): Promise<UserDto[]> {
    return firstValueFrom(
      this.http.get<UserDto[]>(`${environment.apiBaseUrl}/users`)
    );
  }

  async getUser(username: string): Promise<UserDto> {
    return firstValueFrom(
      this.http.get<UserDto>(`${environment.apiBaseUrl}/users/${username}`)
    );
  }

  async listOrganizations(): Promise<string[]> {
    return firstValueFrom(
      this.http.get<string[]>(`${environment.apiBaseUrl}/users/organizations`)
    );
  }

  async listRoles(): Promise<RoleDto[]> {
    return firstValueFrom(
      this.http.get<RoleDto[]>(`${environment.apiBaseUrl}/users/roles`)
    );
  }
}
