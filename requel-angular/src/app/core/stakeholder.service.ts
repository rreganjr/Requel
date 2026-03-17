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
