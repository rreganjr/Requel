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
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { projectApiUrl } from './api-url';
import { ProjectSearchPage } from '../models/project-search';

/** Project search (#504): one page of matches with the total and per-type counts. */
@Injectable({ providedIn: 'root' })
export class ProjectSearchService {
  private readonly http = inject(HttpClient);

  search(projectName: string, query: string,
      options: { type?: string | null; offset?: number; limit?: number } = {}): Promise<ProjectSearchPage> {
    let params = new HttpParams().set('q', query);
    if (options.type) {
      params = params.set('type', options.type);
    }
    if (options.offset != null) {
      params = params.set('offset', options.offset);
    }
    if (options.limit != null) {
      params = params.set('limit', options.limit);
    }
    return firstValueFrom(this.http.get<ProjectSearchPage>(projectApiUrl(projectName, 'search'), { params }));
  }
}
