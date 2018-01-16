/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
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

/**
 * A SystemInitializer does some initialization on system start up such as
 * creating and configuring built in entities such as users or permissions; load
 * database data etc.
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
