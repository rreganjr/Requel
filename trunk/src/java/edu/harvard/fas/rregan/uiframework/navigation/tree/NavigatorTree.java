/*
 * $Id: NavigatorTree.java,v 1.9 2008/04/01 06:46:26 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.navigation.tree;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import nextapp.echo2.app.event.ActionEvent;
import echopointng.Tree;
import echopointng.tree.DefaultMutableTreeNode;
import echopointng.tree.DefaultTreeModel;
import echopointng.tree.TreeModel;
import echopointng.tree.TreeNode;
import echopointng.tree.TreeSelectionEvent;
import echopointng.tree.TreeSelectionListener;
import edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher;

/**
 * A component for building a navigation tree based on entity objects. The tree
 * nodes are created by NavigatorTreeNodeFactories assigned for different types
 * of entities.
 * 
 * @author ron
 */
public class NavigatorTree extends Tree implements TreeSelectionListener {
	static final long serialVersionUID = 0L;

	private final Map<Class<?>, NavigatorTreeNodeFactory> treeNodeFactoryMap = new HashMap<Class<?>, NavigatorTreeNodeFactory>();
	private final EventDispatcher eventDispatcher;
	private Object rootObject;

	/**
	 * Build a tree starting with the rootObject as the root of the tree, adding
	 * nodes based on the supplied tree node factories, starting with the
	 * factory for the rootObject.
	 * 
	 * @param eventDispatcher
	 * @param treeNodeFactories
	 * @param rootObject
	 */
	public NavigatorTree(EventDispatcher eventDispatcher,
			Set<NavigatorTreeNodeFactory> treeNodeFactories, Object rootObject) {
		super();
		this.eventDispatcher = eventDispatcher;

		for (NavigatorTreeNodeFactory factory : treeNodeFactories) {
			treeNodeFactoryMap.put(factory.getTargetClass(), factory);
		}
		setRootObject(rootObject);
		addTreeSelectionListener(this);
	}

	/**
	 * 
	 */
	private void buildTree() {
		TreeNode node = new DefaultMutableTreeNode();
		if (getRootObject() != null) {
			NavigatorTreeNodeFactory factory = getNavigatorTreeNodeFactory(getRootObject());
			if (factory != null) {
				node = factory.createTreeNode(eventDispatcher, this, getRootObject());
			} else {
				node = new DefaultMutableTreeNode(getRootObject());
			}
		}
		// this causes invalidate() to be called.
		setModel(new DefaultTreeModel(node));
	}

	@Override
	public void dispose() {
		setModel(null);
		setRootObject(null);
		super.dispose();
	}

	private void removeListeners(DefaultTreeModel treeModel) {
		if (treeModel != null) {
			DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) treeModel.getRoot();
			if (rootNode != null) {
				Enumeration<DefaultMutableTreeNode> enm = rootNode.depthFirstEnumeration();
				while (enm.hasMoreElements()) {
					DefaultMutableTreeNode node = enm.nextElement();
					if (node instanceof NavigatorTreeNode) {
						((NavigatorTreeNode) node).dispose();
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
	 * @param rootObject
	 */
	public void setRootObject(Object rootObject) {
		setModel(null); // this causes invalidate() to be called.
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
	 * @return
	 */
	public NavigatorTreeNodeFactory getNavigatorTreeNodeFactory(Object target) {
		Class<?> targetType = ((target instanceof Class<?>) ? (Class<?>) target : target.getClass());
		NavigatorTreeNodeFactory factory = null;
		factory = treeNodeFactoryMap.get(target);
		if (factory != null) {
			return factory;
		}
		if (targetType.getInterfaces() != null) {
			for (Class<?> targetInterface : targetType.getInterfaces()) {
				factory = getNavigatorTreeNodeFactory(targetInterface);
				if (factory != null) {
					return factory;
				}
			}
		}
		if ((targetType.getSuperclass() != null)
				&& !Object.class.equals(targetType.getSuperclass())) {
			factory = getNavigatorTreeNodeFactory(targetType.getSuperclass());
		}
		return factory;
	}

	/**
	 * Listen for tree selection changes and if they occur on a navigator node
	 * fire the event for that node.
	 * 
	 * @see echopointng.tree.TreeSelectionListener#valueChanged(echopointng.tree.TreeSelectionEvent)
	 */
	public void valueChanged(TreeSelectionEvent e) {
		TreeNode node = (TreeNode) e.getPath().getLastPathComponent();
		if (node instanceof NavigatorTreeNode) {
			ActionEvent eventToFire = ((NavigatorTreeNode) node).getEventToFire();
			if (eventToFire != null) {
				eventDispatcher.dispatchEvent(eventToFire);
			}
		}
	}
}
