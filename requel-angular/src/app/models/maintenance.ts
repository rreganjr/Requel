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
 * Result of the RepairProjectStakeholders maintenance command (issue #256).
 * Mirrors RepairProjectStakeholdersResultDto on the server.
 */
export interface RepairProjectStakeholdersResult {
  /** Projects examined. */
  projectsScanned: number;
  /** Creator stakeholder rows created because none existed. */
  stakeholdersCreated: number;
  /** Individual permissions granted across all repaired rows. */
  permissionsGranted: number;
  /** Projects whose createdBy was null or did not resolve to a user. */
  projectsSkipped: number;
}
