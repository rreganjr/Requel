/*
 * $Id: NavigatorTableModelAdapter.java,v 1.1 2008/04/30 21:03:15 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.panel;

import java.util.Collection;

/**
 * This interface adapts an entity to return a specific collection
 * of an attribute to display in a navigator table. For example if
 * you want to list the line items of an order entity in a navigator
 * table, the order would be the target of the panel and a
 * NavigatorTableModelAdapter can be used when creating the table to
 * return the line items without having to make a custom table that
 * extracts the line items from the order.
 * 
 * @author ron
 */
public interface NavigatorTableModelAdapter {

	/**
	 * set the object to get the collection from.
	 * @param targetObject
	 */
	public void setTargetObject(Object targetObject);
	
	/**
	 * 
	 * @return The collection of entities to display in the NavigatorTable.
	 */
	public Collection<Object> getCollection(); 
}
