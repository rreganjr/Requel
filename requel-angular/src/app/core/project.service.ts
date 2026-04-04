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
import { firstValueFrom, Subject } from 'rxjs';
import { environment } from '../../environments/environment';
import { ProjectDto, ProjectPermissions, ProjectTreeNode } from '../models/project';
import { CommandResult } from '../models/command';
import { CommandService } from './command.service';

/**
 * Service for project query endpoints.
 */
@Injectable({ providedIn: 'root' })
export class ProjectService {

  /** Emits when any mutation changes project entity counts (add/delete stakeholder, goal, etc.) */
  private treeChanged$ = new Subject<void>();
  readonly onTreeChanged = this.treeChanged$.asObservable();

  notifyTreeChanged(): void {
    this.treeChanged$.next();
  }

  constructor(private http: HttpClient, private commandService: CommandService) {}

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

  async getMyPermissions(projectName: string): Promise<ProjectPermissions> {
    return firstValueFrom(
      this.http.get<ProjectPermissions>(
        `${environment.apiBaseUrl}/projects/${encodeURIComponent(projectName)}/my-permissions`
      )
    );
  }

  async getProjectTree(projectName: string): Promise<ProjectTreeNode[]> {
    return firstValueFrom(
      this.http.get<ProjectTreeNode[]>(
        `${environment.apiBaseUrl}/projects/${encodeURIComponent(projectName)}/tree`
      )
    );
  }

  getExportUrl(projectName: string): string {
    return `${environment.apiBaseUrl}/projects/${encodeURIComponent(projectName)}/export`;
  }

  async importProject(file: File, nameOverride?: string): Promise<CommandResult> {
    const input: Record<string, unknown> = {};
    if (nameOverride) {
      input['name'] = nameOverride;
    }
    return this.commandService.executeWithFile('ImportProject', input, file);
  }
}
