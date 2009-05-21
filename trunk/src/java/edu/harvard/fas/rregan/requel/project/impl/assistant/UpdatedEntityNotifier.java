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
package edu.harvard.fas.rregan.requel.project.impl.assistant;

import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity;

/**
 * Used by assistants to indicate that an entity was changed.
 * 
 * @author ron
 */
public interface UpdatedEntityNotifier {

	/**
	 * An Assistant calls this method if it makes changes to a project,
	 * including adding annotations to the project.
	 * 
	 * @param pod -
	 *            the changed project or domain
	 */
	public void entityUpdated(ProjectOrDomain pod);

	/**
	 * An Assistant calls this method if it makes changes to a project entity,
	 * such as a goal, including adding annotations to the entity.
	 * 
	 * @param entity -
	 *            the changed project entity
	 */
	public void entityUpdated(ProjectOrDomainEntity entity);
}
