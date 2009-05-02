/*
 * $Id: Stakeholder.java,v 1.10 2009/01/08 05:49:23 rregan Exp $
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirments
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
package edu.harvard.fas.rregan.requel.project;

import java.util.Set;

import edu.harvard.fas.rregan.requel.user.User;

/**
 * @author ron
 */
public interface Stakeholder extends ProjectOrDomainEntity, GoalContainer, Comparable<Stakeholder> {

	/**
	 * @return true if this is a stakeholder user (basically getUser() != null)
	 */
	public boolean isUserStakeholder();

	/**
	 * @return if the stakeholder is associated with a user, this will return
	 *         that user, otherwise it will return null.
	 */
	public User getUser();

	/**
	 * @return the team that this stakeholder is assigned for the project.
	 */
	public ProjectTeam getTeam();

	/**
	 * @return The set of permissions that the stakeholder has for a project.
	 */
	public Set<StakeholderPermission> getStakeholderPermissions();

	/**
	 * @param entityType
	 * @param permissionType
	 * @return true if the user has the specified permission type on the
	 *         specified entity type.
	 */
	public boolean hasPermission(Class<?> entityType, StakeholderPermissionType permissionType);

	/**
	 * @param stakeholderPermission
	 * @return true if the user has the specified permission.
	 */
	public boolean hasPermission(StakeholderPermission stakeholderPermission);

	/**
	 * Grant the stakeholder the specified permission.
	 * 
	 * @param stakeholderPermission
	 */
	public void grantStakeholderPermission(StakeholderPermission stakeholderPermission);

	/**
	 * Revoke the specified permission from the stakeholder.
	 * 
	 * @param stakeholderPermission
	 */
	public void revokeStakeholderPermission(StakeholderPermission stakeholderPermission);
}
