/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirements
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
package com.rreganjr.requel.user.ui;

import nextapp.echo2.app.Label;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import echopointng.tree.MutableTreeNode;
import com.rreganjr.requel.user.User;
import net.sf.echopm.navigation.WorkflowDisposition;
import net.sf.echopm.navigation.event.EventDispatcher;
import net.sf.echopm.navigation.event.NavigationEvent;
import net.sf.echopm.navigation.event.OpenPanelEvent;
import net.sf.echopm.navigation.tree.AbstractNavigatorTreeNodeFactory;
import net.sf.echopm.navigation.tree.NavigatorTree;
import net.sf.echopm.navigation.tree.NavigatorTreeNode;
import net.sf.echopm.panel.PanelActionType;

/**
 * @author ron
 */
@Component("userNavigatorTreeNodeFactory")
@Scope("singleton")
public class UserNavigatorTreeNodeFactory extends AbstractNavigatorTreeNodeFactory {

	/**
	 * Create a new NavigatorTreeNodeFactory appropriate for SystemAdminUserRole
	 * objects.
	 */
	public UserNavigatorTreeNodeFactory() {
		super(UserNavigatorTreeNodeFactory.class.getName(), User.class);
	}

	/**
	 * @see net.sf.echopm.navigation.tree.NavigatorTreeNodeFactory#createTreeNode(EventDispatcher, NavigatorTree, Object)
	 */
	@Override
	public MutableTreeNode createTreeNode(EventDispatcher eventDispatcher, NavigatorTree tree,
			Object object) {
		User user = (User) object;
		NavigationEvent openUserEditor = new OpenPanelEvent(tree, PanelActionType.Editor, user,
				User.class, null, WorkflowDisposition.NewFlow);
		// TODO: create a NavigatorTreeNodeUpdateListener to listen for
		// UpdateEvents for the user.
		return new NavigatorTreeNode(eventDispatcher, user, new Label(user.getUsername()),
				openUserEditor);
	}

}
