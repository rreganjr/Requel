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
import {
  DefinitionDetailDto, DefinitionDraft, ProjectDefinitionDto,
} from '../models/assistant-definition';

/**
 * A project's assistant definitions (issue #264). Reading and writing them both need
 * AssistantDefinition[Edit]; writes go through the command endpoint, and edit, revert and delete
 * send back the lockVersion they read (a stale one is a 409).
 */
@Injectable({ providedIn: 'root' })
export class AssistantDefinitionsService {
  constructor(private http: HttpClient, private commandService: CommandService) {}

  list(projectName: string): Promise<ProjectDefinitionDto[]> {
    return firstValueFrom(this.http.get<ProjectDefinitionDto[]>(
      projectApiUrl(projectName, 'definitions')));
  }

  get(projectName: string, key: string): Promise<DefinitionDetailDto> {
    return firstValueFrom(this.http.get<DefinitionDetailDto>(
      projectApiUrl(projectName, 'definitions', key)));
  }

  create(projectName: string, draft: DefinitionDraft) {
    return this.commandService.execute<ProjectDefinitionDto>('CreateAssistantDefinition',
      { projectName, ...draft });
  }

  edit(projectName: string, version: number, draft: DefinitionDraft) {
    return this.commandService.execute<ProjectDefinitionDto>('EditAssistantDefinition',
      { projectName, version, ...draft });
  }

  /** Copy a bundled definition into the project to customize it. */
  fork(projectName: string, key: string) {
    return this.commandService.execute<ProjectDefinitionDto>('ForkAssistantDefinition',
      { projectName, key });
  }

  /** Drop the project's copy of a bundled definition; the bundled one runs again. */
  revert(projectName: string, key: string, version: number) {
    return this.commandService.execute('RevertAssistantDefinition', { projectName, key, version });
  }

  /** Delete a definition the project created. */
  delete(projectName: string, key: string, version: number) {
    return this.commandService.execute('DeleteAssistantDefinition', { projectName, key, version });
  }
}
