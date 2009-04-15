/*
 * $Id: StakeholderPermission.java,v 1.3 2008/09/06 09:31:59 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project;

/**
 * Permissions at the project level assigned to stakeholders for acting on
 * project elements.
 * 
 * @author ron
 */
public interface StakeholderPermission extends Comparable<StakeholderPermission> {

	/**
	 * @return
	 */
	public String getPermissionKey();

	/**
	 * @return The class of the project entity the permission applies to.
	 */
	public Class<?> getEntityType();

	/**
	 * @return The type of permissions like Edit or Grant
	 */
	public StakeholderPermissionType getPermissionType();
}
