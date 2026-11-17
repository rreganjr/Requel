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
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Requel. If not, see <http://www.gnu.org/licenses/>.
 *
 */
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { RequirementDto, IdeaDto, ClarificationDto, OverlapComparisonDto, RequirementStatus } from '../models/requirement';

@Injectable({ providedIn: 'root' })
export class RequirementService {
  private readonly base = '/api/requirements';

  constructor(private http: HttpClient) {}

  /** List all requirements in a project. */
  async listRequirements(projectName: string, status?: RequirementStatus): Promise<RequirementDto[]> {
    let params = new HttpParams().set('projectName', projectName);
    if (status) params = params.set('status', status);
    return firstValueFrom(this.http.get<RequirementDto[]>(this.base, { params }));
  }

  /** Get a single requirement by ID. */
  async getRequirement(projectName: string, requirementId: number): Promise<RequirementDto> {
    const params = new HttpParams().set('projectName', projectName);
    return firstValueFrom(this.http.get<RequirementDto>(`${this.base}/${requirementId}`, { params }));
  }

  /** Create a new requirement from an idea (UC-03). */
  async createRequirement(projectName: string, ideaId: number | null, shallText: string, requirementId?: string): Promise<RequirementDto> {
    const params = new HttpParams().set('projectName', projectName);
    return firstValueFrom(this.http.post<RequirementDto>(this.base, { ideaId, shallText, requirementId }, { params }));
  }

  /** Update requirement text (resets reviews per BR-07/08). */
  async updateRequirement(projectName: string, requirementId: number, shallText: string, version: number, reference?: string): Promise<RequirementDto> {
    const params = new HttpParams().set('projectName', projectName);
    return firstValueFrom(this.http.put<RequirementDto>(`${this.base}/${requirementId}`, { shallText, version, requirementId: reference }, { params }));
  }

  /** Submit a review for a department (UC-06/07/08). */
  async submitReview(projectName: string, requirementId: number, department: 'QUALITY' | 'FEASIBILITY' | 'TESTABILITY',
                     result: 'PASSED' | 'FAILED', comments: string, version: number): Promise<RequirementDto> {
    const params = new HttpParams().set('projectName', projectName);
    return firstValueFrom(this.http.post<RequirementDto>(`${this.base}/${requirementId}/reviews`, {
      department, result, comments, version
    }, { params }));
  }

  /** Approve a requirement (UC-09, PM only). */
  async approveRequirement(projectName: string, requirementId: number, version: number): Promise<RequirementDto> {
    const params = new HttpParams().set('projectName', projectName);
    return firstValueFrom(this.http.post<RequirementDto>(`${this.base}/${requirementId}/approve`, { version }, { params }));
  }

  /** Reopen for review (PM only). */
  async reopenForReview(projectName: string, requirementId: number, version: number): Promise<RequirementDto> {
    const params = new HttpParams().set('projectName', projectName);
    return firstValueFrom(this.http.post<RequirementDto>(`${this.base}/${requirementId}/reopen`, { version }, { params }));
  }

  /** Publish to Jama Connect (UC-10, Analyst only). */
  async publishRequirement(projectName: string, requirementId: number, version: number): Promise<RequirementDto> {
    const params = new HttpParams().set('projectName', projectName);
    return firstValueFrom(this.http.post<RequirementDto>(`${this.base}/${requirementId}/publish`, { version }, { params }));
  }

  /** Delete a requirement (DRAFT only). */
  async deleteRequirement(projectName: string, requirementId: number, version: number): Promise<void> {
    const params = new HttpParams().set('projectName', projectName).set('version', version.toString());
    return firstValueFrom(this.http.delete<void>(`${this.base}/${requirementId}`, { params }));
  }

  // --- Idea operations (UC-01..03) ---

  /** List ideas in a project. */
  async listIdeas(projectName: string): Promise<IdeaDto[]> {
    const params = new HttpParams().set('projectName', projectName);
    return firstValueFrom(this.http.get<IdeaDto[]>(`${this.base}/ideas`, { params }));
  }

  /** Get an idea by ID. */
  async getIdea(projectName: string, ideaId: number): Promise<IdeaDto> {
    const params = new HttpParams().set('projectName', projectName);
    return firstValueFrom(this.http.get<IdeaDto>(`${this.base}/ideas/${ideaId}`, { params }));
  }

  /** Submit a new idea (UC-01). */
  async submitIdea(projectName: string, sourceText: string, file?: File): Promise<IdeaDto> {
    const params = new HttpParams().set('projectName', projectName);
    if (file) {
      const formData = new FormData();
      formData.append('sourceText', sourceText);
      formData.append('file', file);
      return firstValueFrom(this.http.post<IdeaDto>(`${this.base}/ideas`, formData, { params }));
    }
    return firstValueFrom(this.http.post<IdeaDto>(`${this.base}/ideas`, { sourceText }, { params }));
  }

  /** Answer a clarification question (UC-02). */
  async answerClarification(projectName: string, clarificationId: number, answer: string): Promise<ClarificationDto> {
    const params = new HttpParams().set('projectName', projectName);
    return firstValueFrom(this.http.post<ClarificationDto>(`${this.base}/ideas/clarifications/${clarificationId}/answer`, { answer }, { params }));
  }

  /** Review breakdown: keep/reject/revise derived requirements (UC-03). */
  async reviewBreakdown(projectName: string, ideaId: number, decisions: Array<{ requirementId: number; action: 'KEEP' | 'REJECT' | 'REVISE'; revisedText?: string }>): Promise<IdeaDto> {
    const params = new HttpParams().set('projectName', projectName);
    return firstValueFrom(this.http.post<IdeaDto>(`${this.base}/ideas/${ideaId}/breakdown-review`, { decisions }, { params }));
  }

  // --- Overlap comparison (UC-05, F-02/08) ---

  /** Compare a draft requirement against existing ones (F-02, F-08). */
  async compareRequirements(projectName: string, draftRequirementId: number, existingRequirementIds: number[]): Promise<OverlapComparisonDto[]> {
    const params = new HttpParams().set('projectName', projectName);
    return firstValueFrom(this.http.post<OverlapComparisonDto[]>(`${this.base}/${draftRequirementId}/compare`, { existingRequirementIds }, { params }));
  }

  /** Record overlap resolution (UC-05). */
  async recordOverlapResolution(projectName: string, comparisonId: number, resolution: 'ACCEPTED' | 'REJECTED' | 'MODIFIED', justification: string): Promise<OverlapComparisonDto> {
    const params = new HttpParams().set('projectName', projectName);
    return firstValueFrom(this.http.post<OverlapComparisonDto>(`${this.base}/comparisons/${comparisonId}/resolve`, { resolution, justification }, { params }));
  }

  /** Fetch existing requirements from Jama for comparison (F-08). */
  async fetchJamaRequirements(projectName: string, query: string): Promise<{ id: string; name: string; text: string }[]> {
    const params = new HttpParams().set('projectName', projectName).set('query', query);
    return firstValueFrom(this.http.get<{ id: string; name: string; text: string }[]>(`${this.base}/jama/search`, { params }));
  }
}
