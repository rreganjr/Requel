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
import { CorpusMode, CorpusRunDto, CorpusSetKind } from '../models/corpus';

/**
 * Issue #266: explicit corpus runs over a project, or a goal or use case with what hangs off it.
 * Never triggered by an edit.
 */
@Injectable({ providedIn: 'root' })
export class CorpusService {
  constructor(private http: HttpClient) {}

  /** Queue a run; it finishes in the background. */
  request(projectId: number, set: CorpusSetKind, rootId: number | null, mode: CorpusMode): Promise<unknown> {
    return firstValueFrom(this.http.post('/api/ai/corpus', null, { params: this.params(projectId, set, rootId, mode) }));
  }

  /** The latest run over the set, or null when there is none. */
  async latest(projectId: number, set: CorpusSetKind, rootId: number | null, mode: CorpusMode): Promise<CorpusRunDto | null> {
    const run = await firstValueFrom(this.http.get<CorpusRunDto | null>('/api/ai/corpus',
      { params: this.params(projectId, set, rootId, mode) }));
    return run ?? null;
  }

  private params(projectId: number, set: CorpusSetKind, rootId: number | null, mode: CorpusMode): HttpParams {
    let params = new HttpParams().set('projectId', projectId).set('set', set).set('mode', mode);
    if (rootId != null) params = params.set('rootId', rootId);
    return params;
  }
}
