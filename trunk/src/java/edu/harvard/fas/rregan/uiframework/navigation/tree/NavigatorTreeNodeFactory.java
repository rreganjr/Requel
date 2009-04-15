/*
 * $Id: NavigatorTreeNodeFactory.java,v 1.3 2008/03/07 10:37:27 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.navigation.tree;

import echopointng.tree.MutableTreeNode;
import edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher;

/**
 * @author ron
 */
public interface NavigatorTreeNodeFactory {

	/**
	 * Return the type of entity object this factory builds TreeNodes for.
	 * 
	 * @return
	 */
	public Class<?> getTargetClass();

	/**
	 * Given some object create an appropriate tree representation and return
	 * the root of that tree.
	 * 
	 * @param eventDispatcher
	 * @param tree
	 * @param object
	 * @return
	 */
	public MutableTreeNode createTreeNode(EventDispatcher eventDispatcher, NavigatorTree tree,
			Object object);
}
