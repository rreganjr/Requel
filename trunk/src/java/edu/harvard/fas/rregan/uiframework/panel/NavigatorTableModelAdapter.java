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
package edu.harvard.fas.rregan.uiframework.panel;

import java.util.Collection;

/**
 * This interface adapts an entity to return a specific collection of an
 * attribute to display in a navigator table. For example if you want to list
 * the line items of an order entity in a navigator table, the order would be
 * the target of the panel and a NavigatorTableModelAdapter can be used when
 * creating the table to return the line items without having to make a custom
 * table that extracts the line items from the order.
 * 
 * @author ron
 */
public interface NavigatorTableModelAdapter {

	/**
	 * set the object to get the collection from.
	 * 
	 * @param targetObject
	 */
	public void setTargetObject(Object targetObject);

	/**
	 * @return The collection of entities to display in the NavigatorTable.
	 */
	public Collection<Object> getCollection();
}
