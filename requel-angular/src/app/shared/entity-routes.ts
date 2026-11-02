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

/**
 * The project route segment for each entity type that has its own page, keyed by the type's
 * simple name as the API reports it. A type that isn't here (a scenario step) has no page.
 */
export const ENTITY_ROUTES: Readonly<Record<string, string>> = {
  Goal: 'goals',
  Story: 'stories',
  Actor: 'actors',
  Scenario: 'scenarios',
  UseCase: 'use-cases',
  UserStakeholder: 'stakeholders',
  NonUserStakeholder: 'stakeholders',
  GlossaryTerm: 'terms',
  ReportGenerator: 'reports',
};

/** The router link for an entity's page, or null when its type has none. */
export function entityRoute(projectName: string, entityType: string, id: number | null): string[] | null {
  const segment = ENTITY_ROUTES[entityType];
  return segment && id != null ? ['/projects', projectName, segment, String(id)] : null;
}
