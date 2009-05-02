/*
 * $Id: EditorTree.java,v 1.15 2009/01/23 09:54:25 rregan Exp $
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

import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import nextapp.echo2.app.Component;
import nextapp.echo2.app.Label;
import nextapp.echo2.extras.app.DragSource;
import nextapp.echo2.extras.app.event.DropEvent;
import nextapp.echo2.extras.app.event.DropListener;

import org.apache.log4j.Logger;

import echopointng.Tree;
import echopointng.tree.DefaultMutableTreeNode;
import echopointng.tree.DefaultTreeModel;
import echopointng.tree.MutableTreeNode;
import echopointng.tree.TreeModel;
import echopointng.tree.TreeNode;
import echopointng.tree.TreePath;
import edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher;
import edu.harvard.fas.rregan.uiframework.panel.editor.EditMode;
import edu.harvard.fas.rregan.uiframework.panel.editor.manipulators.ComponentManipulators;

/**
 * A component for building an editor tree based on entity objects. The tree
 * nodes are created by NavigatorTreeNodeFactories assigned for different types
 * of entities.
 * 
 * @author ron
 */
public class EditorTree extends Tree implements EditMode {
	private static final Logger log = Logger.getLogger(EditorTree.class);
	static final long serialVersionUID = 0L;

	/**
	 * The style name for the drag source label. The label should be styled with
	 * a background image
	 */
	public static final String STYLE_NAME_DRAG_SOURCE = "EditorTree.DragSource";

	/**
	 * The style name for the drag source label. The label should be styled with
	 * a background image
	 */
	public static final String STYLE_NAME_DROP_TARGET = "EditorTree.DropTarget";

	static {
		ComponentManipulators.setManipulator(EditorTree.class, new EditorTreeManipulator());
	}

	private final Map<Class<?>, EditorTreeNodeFactory> treeNodeFactoryMap = new HashMap<Class<?>, EditorTreeNodeFactory>();
	private final EventDispatcher eventDispatcher;
	private final EditMode editMode;
	private final Map<MutableTreeNode, Component> nodeToDropTargetMap = new HashMap<MutableTreeNode, Component>();
	private final Map<MutableTreeNode, DragSource> nodeToDragSourceMap = new HashMap<MutableTreeNode, DragSource>();
	private final Map<Component, MutableTreeNode> dropTargetToNodeMap = new HashMap<Component, MutableTreeNode>();
	private final Map<DragSource, MutableTreeNode> dragSourceToNodeMap = new HashMap<DragSource, MutableTreeNode>();
	private final EditorTreeDragAndDropHandler dropHandler;
	private Object rootObject;

	/**
	 * Build a tree starting with the rootObject as the root of the tree, adding
	 * nodes based on the supplied tree node factories, starting with the
	 * factory for the rootObject.
	 * 
	 * @param editMode -
	 *            an object controlling the edit mode of this tree.
	 * @param eventDispatcher
	 * @param treeNodeFactories
	 * @param rootObject -
	 *            the root entity object being edited by the tree.
	 * @param disableSelection -
	 *            set to true to turn off tree node selection so that editor
	 *            fields at the tree node level will work properly.
	 * @param showRootNode -
	 *            set to false to not show a node for the root object being
	 *            edited.
	 * @param enableDragAndDrop -
	 *            a EditorTreeDragAndDropHandler if the tree should support
	 *            dragging and dropping of tree nodes, or null if drag and drop
	 *            should not be enabled.
	 */
	public EditorTree(EditMode editMode, EventDispatcher eventDispatcher,
			Set<EditorTreeNodeFactory> treeNodeFactories, boolean disableSelection,
			boolean showRootNode, boolean enableDragAndDrop) {
		super();
		this.eventDispatcher = eventDispatcher;
		this.editMode = editMode;
		if (disableSelection) {
			setSelectionModel(null);
		}
		if (enableDragAndDrop) {
			this.dropHandler = new EditorTreeDragAndDropHandler(this);
		} else {
			this.dropHandler = null;
		}
		setRootVisible(showRootNode);
		setCellRenderer(new EditorTreeCellRenderer());
		for (EditorTreeNodeFactory factory : treeNodeFactories) {
			// if the dropHandler is not null decorate each factory with a
			// factory that decorates each node with drag and drop enabled
			// nodes.
			if (dropHandler != null) {
				treeNodeFactoryMap.put(factory.getTargetType(),
						new EditorTreeDragAndDropNodeFactoryDecorator(factory));
			} else {
				treeNodeFactoryMap.put(factory.getTargetType(), factory);
			}
		}
	}

	/**
	 * @return the object that defines the edit mode and state.
	 */
	public EditMode getEditMode() {
		return editMode;
	}

	@Override
	public boolean isReadOnlyMode() {
		return editMode.isReadOnlyMode();
	}

	@Override
	public boolean isStateEdited() {
		return editMode.isStateEdited();
	}

	@Override
	public void setStateEdited(boolean stateEdited) {
		editMode.setStateEdited(stateEdited);
	}

	/**
	 * @param node
	 * @return return the drop target component for dropping another tree node
	 *         to be inserted where the supplied tree node is located.
	 */
	public Component getDropTarget(MutableTreeNode node) {
		if (nodeToDropTargetMap.containsKey(node)) {
			return nodeToDropTargetMap.get(node);
		} else {
			Component dropTarget = new Label();
			dropTarget.setStyleName(STYLE_NAME_DROP_TARGET);
			nodeToDropTargetMap.put(node, dropTarget);
			dropTargetToNodeMap.put(dropTarget, node);
			for (DragSource source : nodeToDragSourceMap.values()) {
				source.addDropTarget(dropTarget);
			}
			return dropTarget;
		}
	}

	/**
	 * @param dropTarget
	 * @return The tree node that has the supplied dropTarget attached for drag
	 *         and drop.
	 */
	public MutableTreeNode getNodeForDropTarget(Component dropTarget) {
		return dropTargetToNodeMap.get(dropTarget);
	}

	/**
	 * @param node
	 * @return the drag source for grabbing and dragging the supplied node to a
	 *         new location in the tree.
	 */
	public DragSource getDragSource(MutableTreeNode node) {
		if (nodeToDragSourceMap.containsKey(node)) {
			return nodeToDragSourceMap.get(node);
		} else {
			Component dragSourceLabel = new Label();
			dragSourceLabel.setStyleName(STYLE_NAME_DRAG_SOURCE);
			DragSource dragSource = new DragSource(dragSourceLabel);
			dragSource.addDropTargetListener(dropHandler);
			nodeToDragSourceMap.put(node, dragSource);
			dragSourceToNodeMap.put(dragSource, node);
			for (Component target : nodeToDropTargetMap.values()) {
				dragSource.addDropTarget(target);
			}
			return dragSource;
		}
	}

	public MutableTreeNode getNodeForDragSource(DragSource dragSource) {
		return dragSourceToNodeMap.get(dragSource);
	}

	public void moveTreeNodeToBeforeNode(MutableTreeNode nodeToMove, MutableTreeNode targetNode) {
		TreeNode parent = targetNode.getParent();
		if ((parent != null) && !isDecendantOf(nodeToMove, targetNode)) {
			getModel().insertNodeInto(nodeToMove, (MutableTreeNode) parent,
					parent.getIndex(targetNode));
			getModel().nodeStructureChanged(parent);
			if ((getRootNode() != null) && (getRootNode().getChildCount() > 0)) {
				// if the parent is the root keep the tree expanded
				expandPath(new TreePath(getModel().getPathToRoot(getRootNode())));
			}
		}
	}

	private boolean isDecendantOf(TreeNode node1, TreeNode node2) {
		boolean result = false;
		if (node1.equals(node2)) {
			result = true;
		} else if (node2.getParent() != null) {
			result = isDecendantOf(node1, node2.getParent());
		}
		return result;
	}

	/**
	 * 
	 */
	private void buildTree() {
		nodeToDropTargetMap.clear();
		nodeToDragSourceMap.clear();
		dropTargetToNodeMap.clear();
		dragSourceToNodeMap.clear();
		TreeNode rootNode;
		if (getRootObject() != null) {
			EditorTreeNodeFactory factory = getEditorTreeNodeFactory(getRootObject());
			if (factory != null) {
				rootNode = factory.createTreeNode(eventDispatcher, this, getRootObject());
			} else {
				rootNode = new DefaultMutableTreeNode(getRootObject());
			}
		} else {
			rootNode = new DefaultMutableTreeNode();
		}
		setModel(new DefaultTreeModel(rootNode));
		expandAll();
	}

	@Override
	public void dispose() {
		setModel(null);
		setRootObject(null);
		super.dispose();
	}

	private void removeListeners(DefaultTreeModel treeModel) {
		if (treeModel != null) {
			MutableTreeNode rootNode = (MutableTreeNode) treeModel.getRoot();
			if (rootNode != null) {
				Enumeration<MutableTreeNode> enm = new PostorderEnumeration(rootNode);
				while (enm.hasMoreElements()) {
					MutableTreeNode node = enm.nextElement();
					if (node instanceof EditorTreeNode) {
						((EditorTreeNode) node).dispose();
					}
				}
			}
		}
	}

	/**
	 * @return
	 */
	public Object getRootObject() {
		return rootObject;
	}

	/**
	 * @return the root node in the tree.
	 */
	public MutableTreeNode getRootNode() {
		return (MutableTreeNode) getModel().getRoot();
	}

	/**
	 * @param rootObject
	 */
	public void setRootObject(Object rootObject) {
		// this causes invalidate() to be called.
		setModel(new DefaultTreeModel(new DefaultMutableTreeNode(rootObject)));
		this.rootObject = rootObject;
		if (this.rootObject != null) {
			buildTree();
		}
	}

	@Override
	public DefaultTreeModel getModel() {
		return (DefaultTreeModel) super.getModel();
	}

	@Override
	public void setModel(TreeModel newTreeModel) {
		DefaultTreeModel oldModel = getModel();
		if (oldModel != null) {
			removeListeners(oldModel);
		}
		super.setModel(newTreeModel);
	}

	/**
	 * @param target
	 * @return An EditorTreeNodeFactory for creating a node to edit the supplied
	 *         target object.
	 */
	public EditorTreeNodeFactory getEditorTreeNodeFactory(Object target) {
		Class<?> targetType = ((target instanceof Class<?>) ? (Class<?>) target : target.getClass());
		EditorTreeNodeFactory factory = null;
		factory = treeNodeFactoryMap.get(target);
		if (factory != null) {
			return factory;
		}
		if (targetType.getInterfaces() != null) {
			for (Class<?> targetInterface : targetType.getInterfaces()) {
				factory = getEditorTreeNodeFactory(targetInterface);
				if (factory != null) {
					return factory;
				}
			}
		}
		if ((targetType.getSuperclass() != null)
				&& !Object.class.equals(targetType.getSuperclass())) {
			factory = getEditorTreeNodeFactory(targetType.getSuperclass());
		}
		return factory;
	}

	private static class EditorTreeDragAndDropHandler implements DropListener {
		static final long serialVersionUID = 0L;
		private final EditorTree editorTree;

		/**
		 * @param editorTree
		 */
		public EditorTreeDragAndDropHandler(EditorTree editorTree) {
			super();
			this.editorTree = editorTree;
		}

		/**
		 * @see nextapp.echo2.extras.app.event.DropListener#dropPerformed(nextapp.echo2.extras.app.event.DropEvent)
		 */
		@Override
		public void dropPerformed(DropEvent event) {
			DragSource dragSource = (DragSource) event.getSource();
			Component dropTarget = (Component) event.getTarget();
			MutableTreeNode nodeToMove = editorTree.getNodeForDragSource(dragSource);
			MutableTreeNode targetNode = editorTree.getNodeForDropTarget(dropTarget);
			editorTree.moveTreeNodeToBeforeNode(nodeToMove, targetNode);
		}
	}

	// the following are copied from DefaultMutableTreeNode

	public final class PostorderEnumeration implements Enumeration, Serializable {
		protected TreeNode root;
		protected Enumeration children;
		protected Enumeration subtree;

		public PostorderEnumeration(TreeNode rootNode) {
			super();
			root = rootNode;
			children = root.children();
			subtree = EMPTY_ENUMERATION;
		}

		public boolean hasMoreElements() {
			return root != null;
		}

		public Object nextElement() {
			Object retval;

			if (subtree.hasMoreElements()) {
				retval = subtree.nextElement();
			} else if (children.hasMoreElements()) {
				subtree = new PostorderEnumeration((TreeNode) children.nextElement());
				retval = subtree.nextElement();
			} else {
				retval = root;
				root = null;
			}

			return retval;
		}
	}

	public static final Enumeration EMPTY_ENUMERATION = new Enumeration() {
		public boolean hasMoreElements() {
			return false;
		}

		public Object nextElement() {
			throw new NoSuchElementException("No more elements");
		}
	};

}
