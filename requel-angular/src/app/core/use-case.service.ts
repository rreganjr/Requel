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
