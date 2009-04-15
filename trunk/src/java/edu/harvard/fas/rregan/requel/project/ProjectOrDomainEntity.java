/*
 * $Id: ProjectOrDomainEntity.java,v 1.9 2009/03/05 08:50:47 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
