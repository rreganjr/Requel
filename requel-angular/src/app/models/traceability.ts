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
import { EntityReferenceDto } from './entity-reference';

/** #497: one goal's row in the traceability matrix; each list is sorted by name. */
export interface GoalTraceDto {
  id: number;
  name: string;
  /** Whether a story or a use case references the goal. */
  realized: boolean;
  stakeholders: EntityReferenceDto[];
  actors: EntityReferenceDto[];
  stories: EntityReferenceDto[];
  useCases: EntityReferenceDto[];
}

/** #497: one requirement nothing traces to or from. */
export interface TraceGapDto {
  kind: TraceGapKind;
  entity: EntityReferenceDto;
}

/** #497: every goal with what references it, and the gaps in kind order. */
export interface TraceabilityDto {
  goals: GoalTraceDto[];
  gaps: TraceGapDto[];
}

/** The gap kinds in the order they are listed, with their headings. */
export const TRACE_GAP_KINDS = [
  { kind: 'GOAL_NOT_REALIZED', label: 'Goals no story or use case realizes' },
  { kind: 'ACTOR_NOT_USED', label: 'Actors nothing uses' },
  { kind: 'STORY_WITHOUT_GOAL', label: 'Stories with no goal' },
  { kind: 'STORY_NOT_IN_USE_CASE', label: 'Stories in no use case' },
  { kind: 'USE_CASE_WITHOUT_GOAL', label: 'Use cases with no goal' },
  { kind: 'SCENARIO_NOT_IN_USE_CASE', label: 'Scenarios no use case reaches' },
  { kind: 'STAKEHOLDER_WITHOUT_GOAL', label: 'Stakeholders with no goal' },
] as const;

export type TraceGapKind = typeof TRACE_GAP_KINDS[number]['kind'];
