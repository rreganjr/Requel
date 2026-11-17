import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AcceptedIdea, IdeaView, StartedIdea } from '../../models/idea-view';

/** Thrown by [IdeaService] with a message that is fit to show the user. */
export class IdeaServiceError extends Error {}

/**
 * The idea-to-new-project flow: start a project from an idea, read the generated drafts, and
 * accept the chosen ones as the project's requirements.
 */
@Injectable({ providedIn: 'root' })
export class IdeaService {
  constructor(private http: HttpClient) {}

  /** Creates the project, saves the idea and queues generation of its requirement drafts. */
  startNewProject(projectName: string, text: string): Promise<StartedIdea> {
    const form = new FormData();
    form.append('projectName', projectName);
    form.append('text', text);
    return this.call(this.http.post<StartedIdea>(`${environment.apiBaseUrl}/ideas/new-project`, form));
  }

  getIdea(projectName: string, ideaId: string): Promise<IdeaView> {
    return this.call(this.http.get<IdeaView>(this.url(projectName, ideaId)));
  }

  /** Generates again an idea whose generation failed or was skipped. */
  retry(projectName: string, ideaId: string): Promise<unknown> {
    return this.call(this.http.post(`${this.url(projectName, ideaId)}/retry`, {}));
  }

  /** Creates the drafts at the given positions as the project's requirements. */
  accept(projectName: string, ideaId: string, selected: number[]): Promise<AcceptedIdea> {
    return this.call(this.http.post<AcceptedIdea>(`${this.url(projectName, ideaId)}/accept`, { selected }));
  }

  private url(projectName: string, ideaId: string): string {
    return `${environment.apiBaseUrl}/projects/${encodeURIComponent(projectName)}/ideas/${encodeURIComponent(ideaId)}`;
  }

  private async call<T>(request: import('rxjs').Observable<T>): Promise<T> {
    try {
      return await firstValueFrom(request);
    } catch (err) {
      throw new IdeaServiceError(this.messageFor(err));
    }
  }

  private messageFor(err: unknown): string {
    if (!(err instanceof HttpErrorResponse)) {
      return 'Something went wrong. Please try again.';
    }
    if (err.status === 0) {
      return 'Network error. Check your connection and try again.';
    }
    const message = (err.error as { message?: string } | null)?.message;
    if (message) {
      return message;
    }
    if (err.status === 404) {
      return 'Not found. AI assistance may not be switched on for this server.';
    }
    if (err.status === 403) {
      return 'You do not have permission to do that.';
    }
    return `The request failed (${err.status}).`;
  }
}
