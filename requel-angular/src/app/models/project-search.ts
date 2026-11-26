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
/** One match from `GET /api/projects/{name}/search` (#504). */
export interface ProjectSearchResult {
  entityType: string;
  id: number;
  /** As it reads; for an issue, the start of its text. */
  name: string;
  matchedField: 'name' | 'text';
  /** About 160 characters around the first match, with "…" where it was cut. */
  snippet: string;
  /** Where the match falls in the snippet; -1 when it can't be placed (no highlight). */
  matchStart: number;
  matchLength: number;
  /** Issues only. */
  resolved?: boolean;
  onEntityType?: string;
  onEntityId?: number;
  onEntityName?: string;
}

/** One page of matches; the total and the per-type counts cover every match (#504). */
export interface ProjectSearchPage {
  results: ProjectSearchResult[];
  total: number;
  countsByType: Record<string, number>;
  offset: number;
  limit: number;
}

/** The fewest characters the server will search for. */
export const MIN_SEARCH_LENGTH = 2;

/** The page size the results page loads at a time. */
export const SEARCH_PAGE_SIZE = 200;

/** The searchable types, in the order the server ranks them, with their plural labels. */
export const SEARCH_TYPES: readonly { type: string; label: string }[] = [
  { type: 'Goal', label: 'Goals' },
  { type: 'Story', label: 'Stories' },
  { type: 'Actor', label: 'Actors' },
  { type: 'UseCase', label: 'Use cases' },
  { type: 'Scenario', label: 'Scenarios' },
  { type: 'Step', label: 'Steps' },
  { type: 'GlossaryTerm', label: 'Glossary terms' },
  { type: 'Stakeholder', label: 'Stakeholders' },
  { type: 'Issue', label: 'Issues' },
];
