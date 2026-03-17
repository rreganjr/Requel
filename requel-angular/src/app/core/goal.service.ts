import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { GoalDto } from '../models/goal';

@Injectable({ providedIn: 'root' })
export class GoalService {

  constructor(private http: HttpClient) {}

  async listGoals(projectName: string): Promise<GoalDto[]> {
    return firstValueFrom(
      this.http.get<GoalDto[]>(
        `${environment.apiBaseUrl}/projects/${encodeURIComponent(projectName)}/goals`
      )
    );
  }

  async getGoal(projectName: string, goalId: number): Promise<GoalDto> {
    return firstValueFrom(
      this.http.get<GoalDto>(
        `${environment.apiBaseUrl}/projects/${encodeURIComponent(projectName)}/goals/${goalId}`
      )
    );
  }
}
