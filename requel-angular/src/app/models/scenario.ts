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
import { ActorRole } from '../shared/actor-role';

/**
 * #468: a step's or scenario's actor role, on every step and scenario DTO. With a role, `name` is
 * only the action; `displayName` is how it reads with the actor filled in (the server renders it
 * for the use cases it is in).
 */
export interface ActorRoleDto {
  actorRole?: ActorRole | null;
  actorId?: number | null;
  actorName?: string | null;
  displayName?: string | null;
}

/** #468: how a name reads in one use case it is in. */
export interface UseCaseReadingDto {
  useCaseId: number;
  useCaseName: string;
  primaryActorName: string | null;
  readsAs: string;
}

export interface StepDto extends ActorRoleDto {
  id: number;
  version: number;
  name: string;
  text: string | null;
  scenarioType: string | null;
  isScenario: boolean;
  scenarioId: number | null;
  /** #476: the step's (or sub-scenario's) open issues, for the row's warning icon. */
  openIssues?: number;
  /** #476: LOW, MEDIUM or HIGH, the highest of those; absent when there are none. */
  highestOpenSeverity?: string | null;
}

/** #476: a plain step on its own page, with the scenarios that use it. */
export interface StepDetailDto extends ActorRoleDto {
  id: number;
  version: number;
  name: string;
  text: string | null;
  scenarioType: string | null;
  createdBy: string | null;
  usedIn: EntityReferenceDto[] | null;
  /** #468: how the name reads in each use case the step is in. */
  readsIn?: UseCaseReadingDto[] | null;
}

export interface ScenarioDto extends ActorRoleDto {
  id: number;
  version: number;
  name: string;
  text: string | null;
  scenarioType: string | null;
  createdBy: string | null;
  steps: StepDto[] | null;
  referencedBy: EntityReferenceDto[] | null;
  /** #468: how the name reads in each use case the scenario is in (detail read only). */
  readsIn?: UseCaseReadingDto[] | null;
  /** #468: the one primary actor of the scenario's use cases, if they agree. */
  primaryActorName?: string | null;
}

export interface EditStepInput {
  stepId: number | null;
  name: string;
  text: string | null;
  scenarioTypeName: string | null;
  isScenario: boolean;
  /** #468: PRIMARY_ACTOR, SYSTEM, NAMED (with actorName), or NONE to clear. */
  actorRole?: 'NONE' | ActorRole;
  actorName?: string;
}

/**
 * Issue #483: a plain step in the project's step list (`GET /api/projects/{name}/steps`), which
 * the scenario editor suggests when a new step is named. `usedIn` is how many scenarios use it.
 */
export interface StepSummaryDto extends ActorRoleDto {
  id: number;
  name: string;
  text?: string | null;
  scenarioType?: string | null;
  usedIn: number;
  /** #505: unresolved issues on the step. */
  openIssues: number;
  /** #505: the highest severity among them; absent when there are none. */
  highestOpenSeverity?: string | null;
}
