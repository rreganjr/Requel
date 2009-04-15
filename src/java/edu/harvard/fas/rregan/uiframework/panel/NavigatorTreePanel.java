/*
 * $Id: NavigatorTreePanel.java,v 1.4 2008/12/17 02:00:42 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.uiframework.panel;

import java.util.Set;

import edu.harvard.fas.rregan.uiframework.navigation.tree.NavigatorTree;
import edu.harvard.fas.rregan.uiframework.navigation.tree.NavigatorTreeNodeFactory;

/**
 * @author ron
 */
public class NavigatorTreePanel extends AbstractPanel {
	static final long serialVersionUID = 0L;

	private NavigatorTree tree;
	private Set<NavigatorTreeNodeFactory> treeNodeFactories;

	/**
	 * @param treeNodeFactories
	 * @param supportedContentType
	 */
	public NavigatorTreePanel(Set<NavigatorTreeNodeFactory> treeNodeFactories,
			Class<?> supportedContentType) {
		this(NavigatorTreePanel.class.getName(), treeNodeFactories, supportedContentType);
	}

	/**
	 * @param resourceBundleName
	 * @param treeNodeFactories
	 * @param supportedContentType
	 */
	public NavigatorTreePanel(String resourceBundleName,
			Set<NavigatorTreeNodeFactory> treeNodeFactories, Class<?> supportedContentType) {
		super(resourceBundleName, PanelActionType.Navigator, supportedContentType);
		setTreeNodeFactories(treeNodeFactories);
	}

	/**
	 * @param resourceBundleName
	 * @param treeNodeFactories
	 * @param supportedContentType
	 * @param panelName
	 */
	public NavigatorTreePanel(String resourceBundleName,
			Set<NavigatorTreeNodeFactory> treeNodeFactories, String panelName) {
		super(resourceBundleName, PanelActionType.Navigator, null, panelName);
		setTreeNodeFactories(treeNodeFactories);
	}

	@Override
	public void setup() {
		super.setup();
		setTree(new NavigatorTree(getEventDispatcher(), getTreeNodeFactories(), getTargetObject()));
		add(getTree());
	}

	@Override
	public void dispose() {
		super.dispose();
		NavigatorTree tree = getTree();
		if (tree != null) {
			remove(getTree());
			getTree().dispose();
			setTree(null);
		}
	}
	
	@Override
	public void setStyleName(String newValue) {
		super.setStyleName(newValue);
		getTree().setStyleName(newValue);
	}

	@Override
	public void setTargetObject(Object targetObject) {
		super.setTargetObject(targetObject);
		if (getTree() != null) {
			getTree().setRootObject(targetObject);
		}
	}

	protected Set<NavigatorTreeNodeFactory> getTreeNodeFactories() {
		return treeNodeFactories;
	}

	protected void setTreeNodeFactories(Set<NavigatorTreeNodeFactory> treeNodeFactories) {
		this.treeNodeFactories = treeNodeFactories;
	}

	protected NavigatorTree getTree() {
		return tree;
	}

	protected void setTree(NavigatorTree tree) {
		this.tree = tree;
	}
}
