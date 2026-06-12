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
import { ApiTokenDto, CreateApiTokenRequest, CreateApiTokenResponse } from '../models/api-token';

/**
 * Personal access token self-service (#73): list, create, and revoke the current user's tokens via
 * /api/auth/tokens. The auth interceptor attaches the bearer credential.
 */
@Injectable({ providedIn: 'root' })
export class TokenService {

  constructor(private http: HttpClient) {}

  async list(): Promise<ApiTokenDto[]> {
    return firstValueFrom(
      this.http.get<ApiTokenDto[]>(`${environment.apiBaseUrl}/auth/tokens`)
    );
  }

  async create(request: CreateApiTokenRequest): Promise<CreateApiTokenResponse> {
    return firstValueFrom(
      this.http.post<CreateApiTokenResponse>(`${environment.apiBaseUrl}/auth/tokens`, request)
    );
  }

  async revoke(id: number): Promise<void> {
    return firstValueFrom(
      this.http.delete<void>(`${environment.apiBaseUrl}/auth/tokens/${id}`)
    );
  }
}
