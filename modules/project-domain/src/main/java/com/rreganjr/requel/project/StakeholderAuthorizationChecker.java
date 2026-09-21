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
package com.rreganjr.requel.project;

import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.platform.identity.User;

/**
 * The stakeholder-permission check, in one place (issue #305).
 * <p>
 * {@code AuthorizingCommandHandler} enforces this before a command runs, which is the right
 * place for a single command. A command that runs other commands needs the same answer
 * <em>before</em> it starts writing, so that it can refuse the whole operation rather than
 * getting halfway: resolving a spelling issue can edit several entities, and editing some of
 * them and then hitting a refusal is worse than refusing outright.
 * <p>
 * Both callers share this rather than keeping a copy each. Two copies would drift, and a stale
 * authorization check is worse than a verbose one.
 *
 * @author ron
 */
public final class StakeholderAuthorizationChecker {

	private StakeholderAuthorizationChecker() {
	}

	/**
	 * Throw unless the user holds {@code entityType[permissionType]} as a stakeholder on the
	 * project.
	 *
	 * @param project the project the command operates on; null fails as "not a stakeholder",
	 *        which is what a domain-scoped annotation resolves to
	 * @param user the user the command is attributed to
	 * @param entityType the domain entity type the permission is keyed on
	 * @param permissionType the permission type name, e.g. "Edit"
	 * @throws AuthorizationException if the user is not a stakeholder or lacks the permission
	 */
	public static void require(Project project, User user, Class<?> entityType,
			String permissionType) {
		// The catch-all is load-bearing, not defensive padding. A user who is not a
		// stakeholder on the project surfaces in more than one shape depending on the path:
		// getUserStakeholder returns null for some, throws a lookup exception for others, and
		// a domain-scoped annotation resolves no project at all. Every one of those means the
		// same thing here, so they all become a refusal. Narrowing this to the lookup alone
		// let the null case escape as a 500 instead of a 403.
		try {
			UserStakeholder stakeholder = project.getUserStakeholder(user);
			String permissionKey = entityType.getName() + "[" + permissionType + "]";
			boolean held = stakeholder.getStakeholderPermissions().stream()
					.anyMatch(p -> permissionKey.equals(p.getPermissionKey()));
			if (!held) {
				throw new AuthorizationException("Requires stakeholder permission: "
						+ entityType.getSimpleName() + "[" + permissionType + "]");
			}
		} catch (AuthorizationException e) {
			throw e;
		} catch (Exception e) {
			throw new AuthorizationException("User is not a stakeholder on the target project", e);
		}
	}

	/**
	 * The same check as {@link #require}, as a question rather than an assertion, for a caller
	 * deciding whether to start at all.
	 *
	 * @return true when the user holds the permission on that project
	 */
	public static boolean isSatisfied(Project project, User user, Class<?> entityType,
			String permissionType) {
		try {
			require(project, user, entityType, permissionType);
			return true;
		} catch (AuthorizationException e) {
			return false;
		}
	}
}
