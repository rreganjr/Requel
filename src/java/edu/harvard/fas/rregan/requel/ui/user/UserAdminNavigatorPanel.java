/*
 * $Id: UserAdminNavigatorPanel.java,v 1.1 2008/09/12 22:44:14 rregan Exp $
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
package edu.harvard.fas.rregan.requel.ui.user;

import java.util.Set;

import nextapp.echo2.app.Alignment;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Row;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.requel.user.SystemAdminUserRole;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.uiframework.navigation.NavigatorButton;
import edu.harvard.fas.rregan.uiframework.navigation.WorkflowDisposition;
import edu.harvard.fas.rregan.uiframework.navigation.event.NavigationEvent;
import edu.harvard.fas.rregan.uiframework.navigation.event.OpenPanelEvent;
import edu.harvard.fas.rregan.uiframework.navigation.tree.NavigatorTreeNodeFactory;
import edu.harvard.fas.rregan.uiframework.panel.NavigatorTreePanel;
import edu.harvard.fas.rregan.uiframework.panel.PanelActionType;

/**
 * @author ron
 */
public class UserAdminNavigatorPanel extends NavigatorTreePanel {
	private static final Logger log = Logger.getLogger(UserAdminNavigatorPanel.class);
	static final long serialVersionUID = 0;

	/**
	 * Property name to use in the UserAdminNavigatorPanel.properties to set the
	 * lable on the new user button.
	 */
	public static final String PROP_NEW_USER_BUTTON_LABEL = "NewUserButton.Label";

	/**
	 * @param treeNodeFactories
	 */
	public UserAdminNavigatorPanel(Set<NavigatorTreeNodeFactory> treeNodeFactories) {
		super(UserAdminNavigatorPanel.class.getName(), treeNodeFactories, SystemAdminUserRole.class);
	}

	@Override
	public void dispose() {
		super.dispose();
		removeAll();
	}

	@Override
	public void setup() {
		String newUserButtonLabel = getResourceBundleHelper(getLocale()).getString(
				PROP_NEW_USER_BUTTON_LABEL, "New User");

		NavigationEvent openUserEditor = new OpenPanelEvent(this, PanelActionType.Editor, null,
				User.class, null, WorkflowDisposition.NewFlow);
		NavigatorButton newUserButton = new NavigatorButton(newUserButtonLabel,
				getEventDispatcher(), openUserEditor);
		newUserButton.setStyleName(STYLE_NAME_DEFAULT);
		Row newUserButtonWrapper = new Row();
		newUserButtonWrapper.setInsets(new Insets(5));
		newUserButtonWrapper.setAlignment(new Alignment(Alignment.CENTER, Alignment.DEFAULT));
		newUserButtonWrapper.add(newUserButton);
		add(newUserButtonWrapper);
		super.setup();
	}

}
