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
import {
  StakeholderCandidate, StakeholderDto, StakeholderPermissionDto, StakeholderPermissionRules
} from '../models/stakeholder';

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

  /**
   * #390: users who could be added to this project as user stakeholders. Needs Stakeholder Edit on
   * the project rather than the admin-only user list, and carries username and name only.
   */
  async listCandidates(projectName: string): Promise<StakeholderCandidate[]> {
    return firstValueFrom(
      this.http.get<StakeholderCandidate[]>(
        `${environment.apiBaseUrl}/projects/${encodeURIComponent(projectName)}/stakeholder-candidates`
      )
    );
  }

  /** Issue #75: the implied permissions, owned deletes and Grant rules for the permission grid. */
  async getPermissionRules(): Promise<StakeholderPermissionRules> {
    return firstValueFrom(
      this.http.get<StakeholderPermissionRules>(
        `${environment.apiBaseUrl}/projects/stakeholder-permission-rules`
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
