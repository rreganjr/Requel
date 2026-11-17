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
import { EntityReferenceDto } from './entity-reference';

/** Requirement status in the review workflow. */
export type RequirementStatus = 'DRAFT' | 'IN_REVIEW' | 'APPROVED' | 'PUBLISHED' | 'REJECTED';

/** Department that performs a review. */
export type ReviewDepartment = 'QUALITY' | 'FEASIBILITY' | 'TESTABILITY';

/** Result of a review. */
export type ReviewResult = 'NOT_REVIEWED' | 'PASSED' | 'FAILED';

/** Review record for a single department. */
export interface RequirementReviewDto {
  id: number;
  version: number;
  department: ReviewDepartment;
  result: ReviewResult;
  reviewer: string | null;
  reviewerRole: string | null;
  comments: string | null;
  reviewedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

/** A requirement in the Jama-style workflow. */
export interface RequirementDto {
  id: number;
  version: number;
  requirementId: string;           // Human-readable ID like "REQ-123"
  shallText: string;               // The "shall" statement
  status: RequirementStatus;
  ideaId: number | null;           // Link to originating idea
  ideaName: string | null;
  jamaItemId: string | null;       // Jama Connect item ID after publish
  createdBy: string | null;
  createdAt: string;
  updatedAt: string;
  reviews: RequirementReviewDto[];
  /** Derived: true when all three reviews have PASSED. */
  allReviewsPassed: boolean;
  /** Derived: true when status is APPROVED or PUBLISHED. */
  isApproved: boolean;
  /** Derived: true when status is PUBLISHED. */
  isPublished: boolean;
}

/** Idea that spawns requirements (UC-01..03). */
export interface IdeaDto {
  id: number;
  version: number;
  externalId: string | null;
  sourceText: string;
  status: 'SUBMITTED' | 'CLARIFYING' | 'BREAKDOWN_REVIEW' | 'COMPLETE';
  createdBy: string | null;
  createdAt: string;
  derivedRequirements: EntityReferenceDto[] | null;
}

/** Clarification question on an idea (UC-02). */
export interface ClarificationDto {
  id: number;
  version: number;
  question: string;
  answer: string | null;
  answeredBy: string | null;
  answeredAt: string | null;
  createdAt: string;
}

/** Overlap comparison between a draft and an existing requirement (UC-05, F-02/08). */
export interface OverlapComparisonDto {
  id: number;
  version: number;
  draftRequirementId: number;
  existingRequirementId: number;
  existingRequirementRef: EntityReferenceDto;
  aiFinding: string;
  resolution: 'ACCEPTED' | 'REJECTED' | 'MODIFIED' | null;
  justification: string | null;
  resolvedBy: string | null;
  resolvedAt: string | null;
  createdAt: string;
}

/** Department display info for UI. */
export const REVIEW_DEPARTMENTS: { value: ReviewDepartment; label: string; description: string; icon: string }[] = [
  { value: 'QUALITY', label: 'Analyst Review', description: 'Quality, completeness, ambiguity', icon: 'pi pi-search' },
  { value: 'FEASIBILITY', label: 'Engineer Review', description: 'Technical feasibility', icon: 'pi pi-cog' },
  { value: 'TESTABILITY', label: 'Test Engineer Review', description: 'Testability, verifiability', icon: 'pi pi-check-circle' },
];

/** Status display info for UI. */
export const REQUIREMENT_STATUS_OPTIONS: { value: RequirementStatus; label: string; severity: 'success' | 'info' | 'warn' | 'danger' | 'secondary' }[] = [
  { value: 'DRAFT', label: 'Draft', severity: 'secondary' },
  { value: 'IN_REVIEW', label: 'In Review', severity: 'info' },
  { value: 'APPROVED', label: 'Approved', severity: 'success' },
  { value: 'PUBLISHED', label: 'Published', severity: 'success' },
  { value: 'REJECTED', label: 'Rejected', severity: 'danger' },
];

/** Review result display info. */
export const REVIEW_RESULT_OPTIONS: { value: ReviewResult; label: string; severity: 'success' | 'warn' | 'danger' | 'secondary' }[] = [
  { value: 'NOT_REVIEWED', label: 'Not Reviewed', severity: 'secondary' },
  { value: 'PASSED', label: 'Passed', severity: 'success' },
  { value: 'FAILED', label: 'Failed', severity: 'danger' },
];

/** Get display label for a department. */
export function getDepartmentLabel(dept: ReviewDepartment): string {
  return REVIEW_DEPARTMENTS.find(d => d.value === dept)?.label ?? dept;
}

/** Get display label for a status. */
export function getStatusLabel(status: RequirementStatus): string {
  return REQUIREMENT_STATUS_OPTIONS.find(s => s.value === status)?.label ?? status;
}

/** Get display label for a review result. */
export function getReviewResultLabel(result: ReviewResult): string {
  return REVIEW_RESULT_OPTIONS.find(r => r.value === result)?.label ?? result;
}

/** Get severity for a status. */
export function getStatusSeverity(status: RequirementStatus): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
  return REQUIREMENT_STATUS_OPTIONS.find(s => s.value === status)?.severity ?? 'secondary';
}

/** Get severity for a review result. */
export function getReviewResultSeverity(result: ReviewResult): 'success' | 'warn' | 'danger' | 'secondary' {
  return REVIEW_RESULT_OPTIONS.find(r => r.value === result)?.severity ?? 'secondary';
}

/** Check if a user role can review a specific department. */
export function canReviewDepartment(userRoles: string[], department: ReviewDepartment): boolean {
  const roleMap: Record<ReviewDepartment, string[]> = {
    QUALITY: ['Analyst', 'ProductManager'],
    FEASIBILITY: ['Engineer', 'ProductManager'],
    TESTABILITY: ['TestEngineer', 'ProductManager'],
  };
  return userRoles.some(r => roleMap[department]?.includes(r));
}

/** Check if a user role can approve requirements. */
export function canApprove(userRoles: string[]): boolean {
  return userRoles.includes('ProductManager');
}

/** Check if a user role can publish to Jama. */
export function canPublish(userRoles: string[]): boolean {
  return userRoles.includes('Analyst') || userRoles.includes('ProductManager');
}
