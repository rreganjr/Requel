/*
 * $Id: Stakeholder.java,v 1.10 2009/01/08 05:49:23 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
