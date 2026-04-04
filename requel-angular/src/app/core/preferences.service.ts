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
import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { UserPreferencesDto } from '../models/preferences';

@Injectable({ providedIn: 'root' })
export class PreferencesService {

  readonly preferences = signal<UserPreferencesDto>({
    sidebarProjectLimit: 10,
    sidebarProjectStaleness: 'THREE_MONTHS'
  });

  private loaded = false;

  constructor(private http: HttpClient) {}

  async load(): Promise<UserPreferencesDto> {
    const prefs = await firstValueFrom(
      this.http.get<UserPreferencesDto>(`${environment.apiBaseUrl}/user-preferences`)
    );
    this.preferences.set(prefs);
    this.loaded = true;
    return prefs;
  }

  async save(prefs: UserPreferencesDto): Promise<UserPreferencesDto> {
    const updated = await firstValueFrom(
      this.http.put<UserPreferencesDto>(`${environment.apiBaseUrl}/user-preferences`, prefs)
    );
    this.preferences.set(updated);
    return updated;
  }

  isLoaded(): boolean {
    return this.loaded;
  }
}
