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

/**
 * Build a project-scoped API URL, encoding the project name so that names
 * with spaces or reserved characters produce valid URLs.
 *
 * Examples:
 *   projectApiUrl('My Project', 'goals')         → '/api/projects/My%20Project/goals'
 *   projectApiUrl('My Project', 'goals', '42')   → '/api/projects/My%20Project/goals/42'
 */
export function projectApiUrl(projectName: string, ...segments: (string | number)[]): string {
  const base = `/api/projects/${encodeURIComponent(projectName)}`;
  return segments.length > 0
    ? `${base}/${segments.map(s => encodeURIComponent(String(s))).join('/')}`
    : base;
}
