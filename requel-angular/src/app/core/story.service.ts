import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { StoryDto } from '../models/story';

@Injectable({ providedIn: 'root' })
export class StoryService {

  constructor(private http: HttpClient) {}

  async listStories(projectName: string): Promise<StoryDto[]> {
    return firstValueFrom(
      this.http.get<StoryDto[]>(
        `${environment.apiBaseUrl}/projects/${encodeURIComponent(projectName)}/stories`
      )
    );
  }

  async getStory(projectName: string, storyId: number): Promise<StoryDto> {
    return firstValueFrom(
      this.http.get<StoryDto>(
        `${environment.apiBaseUrl}/projects/${encodeURIComponent(projectName)}/stories/${storyId}`
      )
    );
  }
}
