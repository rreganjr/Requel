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

import java.util.Comparator;
import java.util.Set;

import edu.harvard.fas.rregan.requel.CreatedEntity;
import edu.harvard.fas.rregan.requel.Describable;
import edu.harvard.fas.rregan.requel.NamedEntity;
import edu.harvard.fas.rregan.requel.annotation.Annotatable;

/**
 * @author ron
 */
public interface ProjectOrDomainEntity extends NamedEntity, Annotatable, CreatedEntity, Describable {

	/**
	 * @return the project or domain that the entity is attached to.
	 */
	public ProjectOrDomain getProjectOrDomain();

	/**
	 * This is a hack because there is no way to generate a sensibly unique id
	 * for entities from the AbstractProjectOrDomainEntity.
	 * 
	 * @return a string appropriate for the xml id
	 */
	public String getXmlId();

	/**
	 * @return The terms in the glossary that this entity refers to
	 */
	public Set<GlossaryTerm> getGlossaryTerms();

	/**
	 * This is a hack to get the most specific entity interface of a domain
	 * object, even when it is proxied.
	 * 
	 * @return
	 */
	public Class<?> getProjectOrDomainEntityInterface();

	/**
	 * Compare two project entities by their description.
	 */
	public static class ProjectOrDomainEntityComparator implements
			Comparator<ProjectOrDomainEntity> {

		@Override
		public int compare(ProjectOrDomainEntity o1, ProjectOrDomainEntity o2) {
			if ((o1 == null) || (o2 == null)) {
				if (o1 == null) {
					return -1;
				} else {
					return 1;
				}
			}
			return o1.getDescription().compareTo(o2.getDescription());
		}
	}
}
