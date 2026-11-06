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

export interface StepDto {
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
export interface StepDetailDto {
  id: number;
  version: number;
  name: string;
  text: string | null;
  scenarioType: string | null;
  createdBy: string | null;
  usedIn: EntityReferenceDto[] | null;
}

export interface ScenarioDto {
  id: number;
  version: number;
  name: string;
  text: string | null;
  scenarioType: string | null;
  createdBy: string | null;
  steps: StepDto[] | null;
  referencedBy: EntityReferenceDto[] | null;
}

export interface EditStepInput {
  stepId: number | null;
  name: string;
  text: string | null;
  scenarioTypeName: string | null;
  isScenario: boolean;
}
