/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * 
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
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


import com.rreganjr.platform.identity.User;

/**
 * @author ron
 */
public interface Stakeholder extends ProjectOrDomainEntity, GoalContainer, Comparable<Stakeholder> {

	/**
	 * @return true if this is a stakeholder user (basically getUser() != null)
	 */
	public boolean isUserStakeholder();

	/**
	 * @return the display name to use within the project context. Defaults to the stakeholder's
	 *         own name but user stakeholders may delegate to the underlying user.
	 */
	default String getDisplayName() {
		return getName();
	}

	/**
	 * @return the login name when the stakeholder wraps a user, otherwise {@code null}.
	 */
	default String getDisplayUsername() {
		return null;
	}

	/**
	 * @return the email address visible to project participants, delegating to the user when
	 *         applicable.
	 */
	default String getDisplayEmailAddress() {
		return null;
	}

	/**
	 * @return the phone number visible to project participants, delegating to the user when
	 *         applicable.
	 */
	default String getDisplayPhoneNumber() {
		return null;
	}

	 /**
	 * @return {@code true} when this stakeholder represents the supplied user.
	 */
	 default boolean matchesUser(User user) {
	 	return false;
	 }

	 /**
	 * Ensures any backing identity model reflects this stakeholder's project membership.
	 */
	 default void ensureProjectMembership() {
	 }

	 /**
	 * Remove this stakeholder from the supplied project, handling any cleanup such as team
	 * membership or associated user roles.
	 */
	 default void removeFromProject() {
	 	ProjectOrDomain project = getProjectOrDomain();
	 	if (project != null) {
	 		project.getStakeholders().remove(this);
	 	}
	 }
}
