/*
 * $Id: AbstractSystemInitializer.java,v 1.1 2009/01/26 10:19:05 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan;

import org.apache.log4j.Logger;


/**
 * Manages the ordering of entity initializers.
 * 
 * @author ron
 */
public abstract class AbstractSystemInitializer implements SystemInitializer {
	protected static final Logger log = Logger.getLogger(SystemInitializer.class);

	private final int order;

	protected AbstractSystemInitializer(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return order;
	}

	@Override
	public int compareTo(SystemInitializer o) {
		int orderCompare = (getOrder() - o.getOrder());
		int arbitraryCompare = (getClass().getName().compareToIgnoreCase(o.getClass().getName()));
		return (orderCompare == 0 ? arbitraryCompare : orderCompare);
	}
}
