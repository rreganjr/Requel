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
package com.rreganjr.requel.project.command;

import com.rreganjr.platform.command.EditCommand;

/**
 * Repair projects whose creator has no {@code UserStakeholder} row, or holds one
 * without the full permission set (issue #256).
 *
 * <p>
 * Both project-creating paths in main code -
 * {@code EditProjectCommandImpl.createProject()} and
 * {@code ImportProjectStreamingCommandImpl.addUserAsStakeholder()} - already grant the
 * creator every available permission, so this exists for rows left behind by earlier
 * versions of those paths. On the dev instance 377 of 524 projects reported
 * {@code canDelete: false} for exactly this reason. It is a repair, not a policy: it
 * brings old rows up to what creation produces today and does nothing else.
 *
 * <p>
 * It also brings the {@code assistant} stakeholder to
 * {@code ProjectRepository.findAssistantStakeholderPermissions()} (issue #302), which is the
 * one case where a repair takes permissions away: a project imported before #302 had the
 * assistant granted the full matrix by the import loop, and the point of #302 is that the
 * assistant holds the same set however the project arrived. Nobody assigned the assistant
 * those permissions - a loop did - so narrowing them is still repair rather than policy. Human
 * stakeholders are only ever topped up.
 *
 * <p>
 * Idempotent, and safe to run against every project repeatedly. Projects whose
 * {@code createdBy} is null or no longer resolves to a user are skipped and counted,
 * not failed - and a project with no assistant stakeholder row still has its creator
 * repaired, and vice versa.
 *
 * @author ron
 */
public interface RepairProjectStakeholdersCommand extends EditCommand {

	/**
	 * Limit the repair to one project by name. Null (the default) repairs every
	 * project.
	 *
	 * @param projectName the project to repair, or null for all projects
	 */
	public void setProjectName(String projectName);

	/**
	 * @return how many projects were examined.
	 */
	public int getProjectsScanned();

	/**
	 * @return how many creator stakeholder rows were created because none existed.
	 */
	public int getStakeholdersCreated();

	/**
	 * @return how many individual permissions were granted across all repaired
	 *         stakeholders.
	 */
	public int getPermissionsGranted();

	/**
	 * @return how many individual permissions were revoked across all repaired stakeholders.
	 *         Only the assistant's row is ever narrowed (issue #302); a human stakeholder's
	 *         permissions are never taken away by a repair.
	 */
	public int getPermissionsRevoked();

	/**
	 * @return how many projects were skipped because their createdBy was null or did
	 *         not resolve to a user.
	 */
	public int getProjectsSkipped();
}
