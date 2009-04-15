/*
 * $Id: UpdatedEntityNotifier.java,v 1.1 2008/07/31 08:11:11 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
