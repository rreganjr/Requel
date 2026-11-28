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
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { projectApiUrl } from './api-url';
import { TraceabilityDto } from '../models/traceability';

/** #497: a project's goal matrix and gaps. */
@Injectable({ providedIn: 'root' })
export class TraceabilityService {
  private readonly http = inject(HttpClient);

  getTraceability(projectName: string): Promise<TraceabilityDto> {
    return firstValueFrom(this.http.get<TraceabilityDto>(projectApiUrl(projectName, 'traceability')));
  }
}
