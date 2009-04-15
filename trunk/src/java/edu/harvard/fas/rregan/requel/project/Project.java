/*
 * $Id: Project.java,v 1.6 2008/06/20 02:36:49 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
	 * 
	 * @param user
	 * @return the stakeholder representation of the supplied user.
	 */
	public Stakeholder getUserStakeholder(User user);
}
