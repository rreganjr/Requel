/*
 * $Id: UserNavigatorTreeNodeFactory.java,v 1.1 2008/09/12 22:44:15 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.ui.user;

import nextapp.echo2.app.Label;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import echopointng.tree.MutableTreeNode;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.uiframework.navigation.WorkflowDisposition;
import edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher;
import edu.harvard.fas.rregan.uiframework.navigation.event.NavigationEvent;
import edu.harvard.fas.rregan.uiframework.navigation.event.OpenPanelEvent;
import edu.harvard.fas.rregan.uiframework.navigation.tree.AbstractNavigatorTreeNodeFactory;
import edu.harvard.fas.rregan.uiframework.navigation.tree.NavigatorTree;
import edu.harvard.fas.rregan.uiframework.navigation.tree.NavigatorTreeNode;
import edu.harvard.fas.rregan.uiframework.panel.PanelActionType;

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
	 * @see edu.harvard.fas.rregan.uiframework.navigation.tree.NavigatorTreeNodeFactory#createTreeNode(edu.harvard.fas.rregan.uiframework.navigation.tree.NavigatorTree,
	 *      java.lang.Object)
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
