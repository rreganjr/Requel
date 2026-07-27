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
import { HttpClient, HttpParams } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { TagDto, TagEntityRef } from '../models/tag';
import { CommandService } from './command.service';

/**
 * Reads tag data from /api/tags and dispatches tag mutations through the CQRS command
 * endpoint. Mirrors {@link AnnotationService}.
 */
@Injectable({ providedIn: 'root' })
export class TagService {
  private readonly base = '/api/tags';

  constructor(private http: HttpClient, private commandService: CommandService) {}

  /** Project-scoped tags plus global tags. Omit projectName for global only. */
  getTagsForProject(projectName?: string | null): Promise<TagDto[]> {
    let params = new HttpParams();
    if (projectName) {
      params = params.set('projectName', projectName);
    }
    return firstValueFrom(this.http.get<TagDto[]>(this.base, { params }));
  }

  /** Tags assigned to a single entity. */
  getTagsOnEntity(entityType: string, entityId: number): Promise<TagDto[]> {
    const params = new HttpParams()
      .set('entityType', entityType)
      .set('entityId', entityId.toString());
    return firstValueFrom(this.http.get<TagDto[]>(`${this.base}/on-entity`, { params }));
  }

  /** Distinct categories in scope, for autocomplete. */
  getCategories(projectName?: string | null): Promise<string[]> {
    let params = new HttpParams();
    if (projectName) {
      params = params.set('projectName', projectName);
    }
    return firstValueFrom(this.http.get<string[]>(`${this.base}/categories`, { params }));
  }

  /** The entities a tag is assigned to. */
  getEntitiesWithTag(tagId: number): Promise<TagEntityRef[]> {
    return firstValueFrom(this.http.get<TagEntityRef[]>(`${this.base}/${tagId}/entities`));
  }

  /** Create or update a tag. projectName null/blank => global tag. */
  editTag(projectName: string | null, category: string | null, value: string,
          opts: { tagId?: number; color?: string | null } = {}) {
    return this.commandService.execute<TagDto>('EditTag', {
      tagId: opts.tagId ?? null,
      projectName: projectName ?? null,
      category: category ?? null,
      value,
      color: opts.color ?? null,
    });
  }

  assignTag(tagId: number, entityType: string, entityId: number) {
    return this.commandService.execute<TagDto>('AssignTag', { tagId, entityType, entityId });
  }

  unassignTag(tagId: number, entityType: string, entityId: number) {
    return this.commandService.execute('UnassignTag', { tagId, entityType, entityId });
  }

  deleteTag(tagId: number) {
    return this.commandService.execute('DeleteTag', { tagId });
  }
}
