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
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, Subject, firstValueFrom } from 'rxjs';
import { AiReviewAvailability, AiReviewRequest, AiReviewRun } from '../models/ai-review';

/** An entity, as the review calls name it. */
export interface AiReviewTarget {
  entityType: string;
  entityId: number;
}

/**
 * Issue #387: an entity's AI review - whether one would run, requesting one, and its latest
 * run. When the editor's button sees a review finish it reports it here, so the entity's
 * Annotations section can reload the issues and notes the review added.
 */
@Injectable({ providedIn: 'root' })
export class AiReviewService {
  private readonly completedSubject = new Subject<AiReviewTarget>();
  /** A review of the entity finished successfully: its findings are on it now. */
  readonly completed$: Observable<AiReviewTarget> = this.completedSubject.asObservable();

  constructor(private http: HttpClient) {}

  availability(target: AiReviewTarget): Promise<AiReviewAvailability> {
    return firstValueFrom(this.http.get<AiReviewAvailability>('/api/ai/reviews/availability',
      { params: this.params(target) }));
  }

  /** Queue a review (or get the one in flight). A 409 means it can't run; its body says why. */
  request(target: AiReviewTarget): Promise<AiReviewRequest> {
    return firstValueFrom(this.http.post<AiReviewRequest>('/api/ai/reviews', null,
      { params: this.params(target) }));
  }

  /** The entity's latest review run, or null when it has never been reviewed (204). */
  async latest(target: AiReviewTarget): Promise<AiReviewRun | null> {
    const run = await firstValueFrom(this.http.get<AiReviewRun | null>('/api/ai/reviews',
      { params: this.params(target) }));
    return run ?? null;
  }

  notifyCompleted(target: AiReviewTarget): void {
    this.completedSubject.next(target);
  }

  private params(target: AiReviewTarget): HttpParams {
    return new HttpParams().set('entityType', target.entityType).set('entityId', target.entityId);
  }
}
