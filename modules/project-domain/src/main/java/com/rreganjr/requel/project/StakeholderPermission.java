/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
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
