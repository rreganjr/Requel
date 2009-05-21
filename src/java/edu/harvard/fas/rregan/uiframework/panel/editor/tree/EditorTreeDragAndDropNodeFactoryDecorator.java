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
package edu.harvard.fas.rregan.uiframework.panel.editor.tree;

import echopointng.tree.MutableTreeNode;
import edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher;

/**
 * @author ron
 */
public class EditorTreeDragAndDropNodeFactoryDecorator implements EditorTreeNodeFactory {

	private final EditorTreeNodeFactory decoratedFactory;

	/**
	 * @param decoratedFactory
	 */
	public EditorTreeDragAndDropNodeFactoryDecorator(EditorTreeNodeFactory decoratedFactory) {
		super();
		this.decoratedFactory = decoratedFactory;
	}

	/**
	 * @see edu.harvard.fas.rregan.uiframework.panel.editor.tree.EditorTreeNodeFactory#createTreeNode(edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher,
	 *      edu.harvard.fas.rregan.uiframework.panel.editor.tree.EditorTree,
	 *      java.lang.Object)
	 */
	@Override
	public MutableTreeNode createTreeNode(EventDispatcher eventDispatcher, EditorTree tree,
			Object object) {
		return new EditorTreeNodeDragAndDropDecorator(decoratedFactory.createTreeNode(
				eventDispatcher, tree, object), tree);
	}

	/**
	 * @see edu.harvard.fas.rregan.uiframework.panel.editor.tree.EditorTreeNodeFactory#getTargetType()
	 */
	@Override
	public Class<?> getTargetType() {
		return decoratedFactory.getTargetType();
	}

}
