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
import { environment } from '../../environments/environment';
import { UseCaseDto } from '../models/use-case';

@Injectable({ providedIn: 'root' })
export class UseCaseService {
  constructor(private http: HttpClient) {}

  listUseCases(projectName: string): Promise<UseCaseDto[]> {
    return firstValueFrom(
      this.http.get<UseCaseDto[]>(`${environment.apiBaseUrl}/projects/${projectName}/use-cases`)
    );
  }

  getUseCase(projectName: string, useCaseId: number): Promise<UseCaseDto> {
    return firstValueFrom(
      this.http.get<UseCaseDto>(`${environment.apiBaseUrl}/projects/${projectName}/use-cases/${useCaseId}`)
    );
  }
}
