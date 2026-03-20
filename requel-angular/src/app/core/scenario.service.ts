import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { ScenarioDto } from '../models/scenario';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ScenarioService {
  private base = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}

  listScenarios(projectName: string): Promise<ScenarioDto[]> {
    return firstValueFrom(
      this.http.get<ScenarioDto[]>(`${this.base}/projects/${encodeURIComponent(projectName)}/scenarios`)
    );
  }

  getScenario(projectName: string, scenarioId: number): Promise<ScenarioDto> {
    return firstValueFrom(
      this.http.get<ScenarioDto>(`${this.base}/projects/${encodeURIComponent(projectName)}/scenarios/${scenarioId}`)
    );
  }
}
