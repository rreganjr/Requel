import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ProjectDto } from '../models/project';

/**
 * Service for project query endpoints.
 */
@Injectable({ providedIn: 'root' })
export class ProjectService {

  constructor(private http: HttpClient) {}

  async listProjects(): Promise<ProjectDto[]> {
    return firstValueFrom(
      this.http.get<ProjectDto[]>(`${environment.apiBaseUrl}/projects`)
    );
  }

  async getProject(name: string): Promise<ProjectDto> {
    return firstValueFrom(
      this.http.get<ProjectDto>(`${environment.apiBaseUrl}/projects/${encodeURIComponent(name)}`)
    );
  }
}
