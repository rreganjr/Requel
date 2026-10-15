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
import { projectApiUrl } from './api-url';
import { EntitySourceLinkDto, ProjectSourcesDto } from '../models/provenance';

/**
 * Entity provenance (issue #272): which external sources an entity was built from, and (issue
 * #273) the project's references and the precedence between them. Read-only in the UI; sources,
 * links and precedence are written through the gateway.
 */
@Injectable({ providedIn: 'root' })
export class ProvenanceService {
  constructor(private http: HttpClient) {}

  getEntitySources(projectName: string, entityType: string, entityId: number):
      Promise<EntitySourceLinkDto[]> {
    return firstValueFrom(this.http.get<EntitySourceLinkDto[]>(
      projectApiUrl(projectName, 'entities', entityType, entityId, 'sources')));
  }

  getProjectSources(projectName: string): Promise<ProjectSourcesDto> {
    return firstValueFrom(this.http.get<ProjectSourcesDto>(
      projectApiUrl(projectName, 'sources')));
  }
}
