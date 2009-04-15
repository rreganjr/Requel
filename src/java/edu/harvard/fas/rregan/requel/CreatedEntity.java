/*
 * $Id: CreatedEntity.java,v 1.1 2008/04/09 10:33:30 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel;

import java.util.Date;

import edu.harvard.fas.rregan.requel.user.User;

/**
 * @author ron
 */
public interface CreatedEntity {

	/**
	 * @return the user that created the entity
	 */
	public User getCreatedBy();

	/**
	 * @return the date the entity was created
	 */
	public Date getDateCreated();
}
