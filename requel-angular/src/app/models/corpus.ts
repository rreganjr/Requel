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

/** Issue #266: what a corpus run does. CANDIDATES is "Find overlaps" (no AI). */
export type CorpusMode = 'CANDIDATES' | 'ANALYSIS';

/** Issue #266: the set a corpus run covers. */
export type CorpusSetKind = 'PROJECT' | 'GOAL' | 'USE_CASE';

/** One finding row of a corpus run: one per participant of each relationship. */
export interface CorpusFindingDto {
  findingType: string;
  kind: string;
  severity: string | null;
  text: string;
  state: string;
  annotationId: number | null;
}

/** The latest corpus run (the server's AiReviewDto). */
export interface CorpusRunDto {
  runId: string;
  status: string;
  createdAt: string;
  completedAt: string | null;
  errorKind: string | null;
  errorSummary: string | null;
  summary: string | null;
  findings: CorpusFindingDto[];
}
