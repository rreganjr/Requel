/*
 * $Id: Organization.java,v 1.2 2009/01/07 09:50:38 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.user;

/**
 * An entity for grouping users and projects, such as a deparment, company,
 * customer.
 * 
 * @author ron
 */
public interface Organization extends Comparable<Organization> {
	/**
	 * @return the name of the organization.
	 */
	public String getName();
}
