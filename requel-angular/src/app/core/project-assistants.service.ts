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
import { CommandService } from './command.service';
import { projectApiUrl } from './api-url';
import { ProjectAssistantDto } from '../models/project-assistant';
import { ProjectDataHandlingDto } from '../models/project-data-handling';

/**
 * A project's assistants (issue #268): which of the lexical checks run, switching them on and
 * off (Project[Edit]), and re-running analysis on the whole project (Annotation[Edit]). Reads
 * are plain GETs; writes go through the command endpoint.
 */
@Injectable({ providedIn: 'root' })
export class ProjectAssistantsService {
  constructor(private http: HttpClient, private commandService: CommandService) {}

  list(projectName: string): Promise<ProjectAssistantDto[]> {
    return firstValueFrom(this.http.get<ProjectAssistantDto[]>(
      projectApiUrl(projectName, 'assistants')));
  }

  setEnabled(projectName: string, assistantId: string, enabled: boolean) {
    return this.commandService.execute('EditProjectAssistantSetting',
      { projectName, assistantId, enabled });
  }

  /** Issue #262: whether remote AI providers are allowed and which categories are masked. */
  dataHandling(projectName: string): Promise<ProjectDataHandlingDto> {
    return firstValueFrom(this.http.get<ProjectDataHandlingDto>(
      projectApiUrl(projectName, 'data-handling')));
  }

  /** Issue #262: `egress.external` or `redaction.<category>`; Project[Edit]. */
  setDataHandling(projectName: string, key: string, enabled: boolean) {
    return this.commandService.execute('EditProjectDataHandlingSetting',
      { projectName, key, enabled });
  }

  /** Queue analysis of every text entity in the project; the runs finish in the background. */
  analyzeProject(projectName: string) {
    return this.commandService.execute('AnalyzeProject', { projectName });
  }
}
