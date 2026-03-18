import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ActorDto } from '../models/actor';

@Injectable({ providedIn: 'root' })
export class ActorService {

  constructor(private http: HttpClient) {}

  async listActors(projectName: string): Promise<ActorDto[]> {
    return firstValueFrom(
      this.http.get<ActorDto[]>(
        `${environment.apiBaseUrl}/projects/${encodeURIComponent(projectName)}/actors`
      )
    );
  }

  async getActor(projectName: string, actorId: number): Promise<ActorDto> {
    return firstValueFrom(
      this.http.get<ActorDto>(
        `${environment.apiBaseUrl}/projects/${encodeURIComponent(projectName)}/actors/${actorId}`
      )
    );
  }
}
