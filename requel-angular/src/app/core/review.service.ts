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
import { RequirementReviewDto, ReviewDepartment, ReviewResult } from '../models/requirement';

@Injectable({ providedIn: 'root' })
export class ReviewService {
  private readonly base = '/api/reviews';

  constructor(private http: HttpClient) {}

  /** Get all reviews for a requirement. */
  async getReviews(projectName: string, requirementId: number): Promise<RequirementReviewDto[]> {
    const params = new HttpParams().set('projectName', projectName);
    return firstValueFrom(this.http.get<RequirementReviewDto[]>(`${this.base}/requirement/${requirementId}`, { params }));
  }

  /** Get the current user's review for a requirement and department (if any). */
  async getMyReview(projectName: string, requirementId: number, department: ReviewDepartment): Promise<RequirementReviewDto | null> {
    const params = new HttpParams().set('projectName', projectName).set('department', department);
    try {
      return await firstValueFrom(this.http.get<RequirementReviewDto>(`${this.base}/requirement/${requirementId}/mine`, { params }));
    } catch (e: any) {
      if (e.status === 404) return null;
      throw e;
    }
  }

  /** Submit or update a review. */
  async submitReview(projectName: string, requirementId: number, department: ReviewDepartment,
                     result: ReviewResult, comments: string, version: number): Promise<RequirementReviewDto> {
    const params = new HttpParams().set('projectName', projectName);
    return firstValueFrom(this.http.post<RequirementReviewDto>(`${this.base}/requirement/${requirementId}`, {
      department, result, comments, version
    }, { params }));
  }

  /** Get review statistics for a project (dashboard). */
  async getReviewStats(projectName: string): Promise<{
    total: number;
    draft: number;
    inReview: number;
    approved: number;
    published: number;
    byDepartment: Record<ReviewDepartment, { passed: number; failed: number; pending: number }>;
  }> {
    const params = new HttpParams().set('projectName', projectName);
    return firstValueFrom(this.http.get<any>(`${this.base}/stats`, { params }));
  }

  /** Get requirements assigned to the current user for review. */
  async getMyAssignedReviews(projectName: string): Promise<Array<{
    requirementId: number;
    requirementName: string;
    department: ReviewDepartment;
    status: 'PENDING' | 'COMPLETED';
    dueDate: string | null;
  }>> {
    const params = new HttpParams().set('projectName', projectName);
    return firstValueFrom(this.http.get<any[]>(`${this.base}/my-assigned`, { params }));
  }
}
