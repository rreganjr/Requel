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
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { OrganizationDto, UserDto } from '../models/user';
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

  async listOrganizations(): Promise<OrganizationDto[]> {
    return firstValueFrom(
      this.http.get<OrganizationDto[]>(`${environment.apiBaseUrl}/users/organizations`)
    );
  }

  async listRoles(): Promise<RoleDto[]> {
    return firstValueFrom(
      this.http.get<RoleDto[]>(`${environment.apiBaseUrl}/users/roles`)
    );
  }
}
