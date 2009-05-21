/*
 * $Id$
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

import edu.harvard.fas.rregan.requel.OrganizedEntity;
import edu.harvard.fas.rregan.requel.annotation.Annotatable;
import edu.harvard.fas.rregan.requel.user.User;

/**
 * @author ron
 */
public interface Project extends ProjectOrDomain, Annotatable, Comparable<Project>, OrganizedEntity {

	/**
	 * @return The current status of the project.
	 */
	public String getStatus();

	/**
	 * @param user
	 * @return the stakeholder representation of the supplied user.
	 */
	public Stakeholder getUserStakeholder(User user);
}
