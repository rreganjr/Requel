/*
 * $Id: StakeholderPermissionType.java,v 1.4 2009/02/20 10:26:17 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project;

/**
 * The types of permissions that can be granted to stakeholders for working with
 * project entities.
 * 
 * @author ron
 */
public enum StakeholderPermissionType {

	/**
	 * Allow a user to create and edit a project entity.
	 */
	Edit(),

	/**
	 * Allow a user to delete a project entity.
	 */
	Delete(),

	/**
	 * Allow a user to grant permissions for working with different project
	 * entities.
	 */
	Grant();

	private StakeholderPermissionType() {
	}
}
