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
