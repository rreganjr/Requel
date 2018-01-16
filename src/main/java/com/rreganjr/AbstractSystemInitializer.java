/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * 
 * This file is part of Requel - the Collaborative Requirements
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
package com.rreganjr;

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
