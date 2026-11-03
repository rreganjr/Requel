/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.annotation.impl.command;

import com.rreganjr.command.Command;
import com.rreganjr.platform.command.AuthorizableCommand;
import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.platform.command.AuthorizationRequirement.RequiresStakeholderPermission;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectScopedCommand;
import com.rreganjr.requel.project.StakeholderAuthorizationChecker;

/** The pre-check shared by the resolvers that edit an entity's text (#305, #459). */
final class TextEdits {

	private TextEdits() {
	}

	/**
	 * Refuse before anything is written if the user cannot make this edit.
	 * <p>
	 * Only {@code RequiresStakeholderPermission} is pre-checked, which is what every
	 * {@code Edit*Command} declares. Any other requirement is left to the command handler, so
	 * a new requirement type is enforced but not pre-flighted rather than silently skipped.
	 *
	 * @param what how the refusal describes the edit, e.g. "correct the spelling in"
	 */
	static void requireAuthorized(Command edit, Annotatable annotatable, Project project,
			User editedBy, String what) {
		if (!(edit instanceof AuthorizableCommand authorizable)) {
			return;
		}
		if (!(authorizable.getAuthorizationRequirement() instanceof RequiresStakeholderPermission requirement)) {
			return;
		}
		Project scope = (edit instanceof ProjectScopedCommand scoped) ? scoped.getProject() : project;
		if (!StakeholderAuthorizationChecker.isSatisfied(scope, editedBy,
				requirement.entityType(), requirement.permissionType())) {
			throw new AuthorizationException("Requires stakeholder permission: "
					+ requirement.entityType().getSimpleName() + "["
					+ requirement.permissionType() + "] to " + what + " "
					+ annotatable.getClass().getSimpleName() + " " + annotatable);
		}
	}
}
