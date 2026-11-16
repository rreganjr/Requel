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

/** Issue #387: whether an AI review would run for an entity, and why not. */
export interface AiReviewAvailability {
  available: boolean;
  reason: string | null;
}

/** Issue #387: the runs a review request queued, or found already in flight. */
export interface AiReviewRequest {
  runId: string;
  policyRunId: string | null;
}

/** One finding of a review run (the server's AiReviewDto.Finding). */
export interface AiReviewFinding {
  findingType: string;
  kind: string;
  severity: string | null;
  text: string;
  state: string;
  annotationId: number | null;
}

/** An entity's latest review run (the server's AiReviewDto). */
export interface AiReviewRun {
  runId: string;
  status: 'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | string;
  createdAt: string;
  completedAt: string | null;
  errorKind: string | null;
  errorSummary: string | null;
  summary: string | null;
  findings: AiReviewFinding[];
}

/**
 * The types the editors offer "Review with AI" on. Steps are reviewable over the API, MCP and
 * the CLI, but have no editor of their own (they are edited in a dialog in the scenario editor).
 */
export const EDITOR_REVIEWABLE_TYPES = ['Goal', 'Story', 'Actor', 'UseCase', 'Scenario', 'GlossaryTerm'];
