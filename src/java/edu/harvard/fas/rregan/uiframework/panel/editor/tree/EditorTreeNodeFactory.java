/*
 * $Id: EditorTreeNodeFactory.java,v 1.2 2008/10/29 07:42:41 rregan Exp $
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