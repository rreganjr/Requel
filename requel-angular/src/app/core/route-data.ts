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
import { Data } from '@angular/router';

export type RouteSection = 'dashboard' | 'account' | 'admin' | 'project';

export type ArtifactType =
  | 'project'
  | 'goal'
  | 'story'
  | 'actor'
  | 'scenario'
  | 'use-case'
  | 'term'
  | 'report'
  | 'stakeholder'
  | 'open-issue'
  | 'user';

/**
 * Typed route `data` the app shell (#154) consumes for section, breadcrumbs, and page chrome (#142).
 * `Route.data` is the loosely-typed Angular `Data`; author route data through `routeData()` so a bad
 * `section` fails the compiler instead of silently rendering nothing in the shell.
 */
export interface RequelRouteData {
  section: RouteSection;
  artifactType?: ArtifactType;
  /** Static breadcrumb label. Param-derived labels (project / artifact name) are #154. */
  breadcrumb?: string;
}

/** Identity helper that type-checks route `data` at the authoring site. */
export function routeData(data: RequelRouteData): RequelRouteData & Data {
  return data;
}
