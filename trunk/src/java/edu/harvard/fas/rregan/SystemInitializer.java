/*
 * $Id: SystemInitializer.java,v 1.1 2009/01/26 10:19:04 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan;

/**
 * A SystemInitializer does some initialization on system start up such as 
 * creating and configuring built in entities such as users or permissions;
 * load database data etc.
 * 
 * @author ron
 */
public interface SystemInitializer extends Comparable<SystemInitializer> {
	/**
	 * The order is a general order value indicating the order that the
	 * initializer should be run relative to other initializers. In general
	 * initializers that have no dependencies should use an order of 100,
	 * dependent entites such as specific users should use a value of 1000.
	 * 
	 * @return the order to run the initializer
	 */
	public int getOrder();

	/**
	 * create and persist the entities.
	 */
	public void initialize();

}
