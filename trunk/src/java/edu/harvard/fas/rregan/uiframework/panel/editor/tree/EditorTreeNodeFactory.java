/*
 * $Id: EditorTreeNodeFactory.java,v 1.2 2008/10/29 07:42:41 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.panel.editor.tree;

import echopointng.tree.MutableTreeNode;
import edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher;

/**
 * @author ron
 */
public interface EditorTreeNodeFactory {

	/**
	 * Return the type of entity object this factory builds TreeNodes for.
	 * 
	 * @return
	 */
	public Class<?> getTargetType();

	/**
	 * Given some object create an appropriate tree representation and return
	 * the root of that tree.
	 * 
	 * @param eventDispatcher
	 * @param tree
	 * @param object
	 * @return
	 */
	public MutableTreeNode createTreeNode(EventDispatcher eventDispatcher, EditorTree tree,
			Object object);
}
