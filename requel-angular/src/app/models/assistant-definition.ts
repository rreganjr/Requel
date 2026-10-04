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
/** A finding type an assistant definition may report (issue #264). */
export interface DefinitionVocabulary {
  type: string;
  description: string | null;
  /** `quality` (the default) or `extraction`. */
  category: string | null;
}

export type DefinitionKind = 'REVIEW' | 'POLICY' | 'CORPUS';

/** One of a project's assistant definitions, as GET /api/projects/{name}/definitions lists it. */
export interface ProjectDefinitionDto {
  key: string;
  displayName: string;
  kind: DefinitionKind;
  taskType: string;
  scope: string[];
  contextProviders: string[];
  contextBudgets: Record<string, number>;
  instructions: string;
  vocabulary: DefinitionVocabulary[];
  localOnly: boolean;
  /** BUNDLED or PROJECT. */
  source: 'BUNDLED' | 'PROJECT';
  /** The content version, recorded on each run. */
  version: number;
  /** The bundled version a project copy was made from. */
  forkedFromVersion: number | null;
  /** The bundled key's current version; above forkedFromVersion means the bundled one is newer. */
  bundledVersion: number | null;
  /** Send back on edit, revert and delete. */
  lockVersion: number;
  /** The entity types (set kinds for a corpus analysis) it covers in the project. */
  inEffectFor: string[];
  /** The project's switch (#268); null where not read. */
  enabled: boolean | null;
}

/** GET /api/projects/{name}/definitions/{key}: the definition and its bundled baseline. */
export interface DefinitionDetailDto {
  definition: ProjectDefinitionDto;
  bundled: ProjectDefinitionDto | null;
}

/** What an author writes; the server derives the task type and output schema from the kind. */
export interface DefinitionDraft {
  kind: DefinitionKind;
  key: string;
  displayName: string;
  scope: string[];
  contextProviders: string[];
  contextBudgets: Record<string, number>;
  instructions: string;
  vocabulary: DefinitionVocabulary[];
  localOnly: boolean;
}

/** The entity types a review or policy may name in its scope (server: REVIEWABLE_TYPES). */
export const REVIEWABLE_TYPES = ['Goal', 'Story', 'Actor', 'UseCase', 'Scenario', 'Step',
  'GlossaryTerm'];

/** The set kinds a corpus analysis may name (server: CORPUS_SET_KINDS). */
export const CORPUS_SET_KINDS = ['PROJECT', 'GOAL', 'USE_CASE'];

/** The context a review or policy may read (server: the context provider registry). */
export const CONTEXT_PROVIDERS = ['entity', 'goal-relations', 'goal-siblings', 'goal-stakeholders',
  'usecase-scenarios', 'scenario-usecases', 'step-sequence', 'story-actors', 'actor-references',
  'glossary-related', 'project-names', 'project-glossary'];

/** The context a corpus analysis reads. */
export const CORPUS_PROVIDERS = ['corpus-index', 'corpus-candidates'];

/** The heading a definition shows under, as on the Assistants panel. */
export function kindLabel(kind: DefinitionKind): string {
  return kind === 'POLICY' ? 'Policies' : kind === 'CORPUS' ? 'Corpus analysis' : 'AI review';
}

/** The risk of authoring, said wherever it is done (issue #264). */
export const DEFINITION_RISK = 'A definition is instructions a project member writes for the AI'
  + ' model. Requel still checks what comes back: the output format is fixed, findings land only'
  + ' on the entities a run analyzed, and a definition can\'t name code to run. A badly written'
  + ' one can still raise misleading issues in this project.';
