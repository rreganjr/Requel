/*
 * $Id: OrganizedEntity.java,v 1.2 2008/12/13 00:41:37 rregan Exp $
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
package edu.harvard.fas.rregan.requel;

import edu.harvard.fas.rregan.requel.user.Organization;

/**
 * An entity that has an organization assigned.
 * 
 * @author ron
 */
public interface OrganizedEntity {

	/**
	 * @return the default organzation of this user.
	 */
	public Organization getOrganization();

	/**
	 * set the default organization of this user.
	 * 
	 * @param organization
	 */
	public void setOrganization(Organization organization);
}
