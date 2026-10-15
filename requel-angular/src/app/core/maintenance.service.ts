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

import { Injectable } from '@angular/core';
import { CommandResult } from '../models/command';
import { RepairProjectStakeholdersResult } from '../models/maintenance';
import { CommandService } from './command.service';

/**
 * Dispatches administrative maintenance commands through the CQRS command endpoint.
 *
 * Separate from ProjectService because these are operations on the estate rather than on
 * a project a user is working in: they are admin-only, they are not part of any project
 * workflow, and #302's follow-up repairs belong alongside this one rather than scattered
 * through the feature services.
 */
@Injectable({ providedIn: 'root' })
export class MaintenanceService {

  constructor(private commandService: CommandService) {}

  /**
   * Restore the creator's stakeholder row and full permission set on projects that lost
   * them (issue #256).
   *
   * @param projectName repair only this project; omit or pass null to repair every project
   */
  repairProjectStakeholders(projectName?: string | null):
      Promise<CommandResult<RepairProjectStakeholdersResult>> {
    return this.commandService.execute<RepairProjectStakeholdersResult>(
      'RepairProjectStakeholders',
      { projectName: projectName?.trim() || null }
    );
  }
}
