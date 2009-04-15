/*
 * $Id: EditorTreeDragAndDropNodeFactoryDecorator.java,v 1.2 2008/10/29 07:42:41 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
