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
import { StakeholderDto, StakeholderPermissionDto } from '../models/stakeholder';

/**
 * Service for stakeholder query endpoints.
 */
@Injectable({ providedIn: 'root' })
export class StakeholderService {

  constructor(private http: HttpClient) {}

  async listStakeholders(projectName: string): Promise<StakeholderDto[]> {
    return firstValueFrom(
      this.http.get<StakeholderDto[]>(
        `${environment.apiBaseUrl}/projects/${encodeURIComponent(projectName)}/stakeholders`
      )
    );
  }

  async getStakeholder(projectName: string, stakeholderId: number): Promise<StakeholderDto> {
    return firstValueFrom(
      this.http.get<StakeholderDto>(
        `${environment.apiBaseUrl}/projects/${encodeURIComponent(projectName)}/stakeholders/${stakeholderId}`
      )
    );
  }

  async getAvailablePermissions(): Promise<StakeholderPermissionDto[]> {
    return firstValueFrom(
      this.http.get<StakeholderPermissionDto[]>(
        `${environment.apiBaseUrl}/projects/stakeholder-permissions`
      )
    );
  }
}
