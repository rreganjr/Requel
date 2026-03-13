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
