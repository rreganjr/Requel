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
package com.rreganjr.requel.project.ui;

import java.util.Set;

import net.sf.echopm.panel.Panel;
import nextapp.echo2.app.Alignment;
import nextapp.echo2.app.Component;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Row;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectUserRole;
import com.rreganjr.requel.user.User;
import net.sf.echopm.navigation.NavigatorButton;
import net.sf.echopm.navigation.WorkflowDisposition;
import net.sf.echopm.navigation.event.NavigationEvent;
import net.sf.echopm.navigation.event.OpenPanelEvent;
import net.sf.echopm.navigation.tree.NavigatorTreeNodeFactory;
import net.sf.echopm.panel.NavigatorTreePanel;
import net.sf.echopm.panel.PanelActionType;

/**
 * @author ron
 */
public class ProjectNavigatorPanel extends NavigatorTreePanel {
	private static final Log log = LogFactory.getLog(ProjectNavigatorPanel.class);
	static final long serialVersionUID = 0;

	/**
	 * Property name to use in the ProjectNavigatorPanel.properties to set the
	 * label on the new project button.
	 */
	public static final String PROP_NEW_PROJECT_BUTTON_LABEL = "NewProjectButton.Label";

	/**
	 * Property name to use in the ProjectNavigatorPanel.properties to set the
	 * label on the import project button.
	 */
	public static final String PROP_IMPORT_PROJECT_BUTTON_LABEL = "ImportProjectButton.Label";

	/**
	 * Property name to use in the ProjectNavigatorPanel.properties to set the
	 * label on the find/open projects button.
	 */
	public static final String PROP_FIND_PROJECT_BUTTON_LABEL = "FindProjectButton.Label";

	/**
	 * @param treeNodeFactories
	 */
	public ProjectNavigatorPanel(Set<NavigatorTreeNodeFactory> treeNodeFactories) {
		super(ProjectNavigatorPanel.class.getName(), treeNodeFactories, ProjectUserRole.class);
	}

	@Override
	public void dispose() {
		super.dispose();
		removeAll();
	}

	@Override
	public void setup() {
		ProjectUserRole projectUserRole = ((User) getTargetObject())
				.getRoleForType(ProjectUserRole.class);

		if (projectUserRole.canCreateProjects()) {
			add(createButton(PROP_NEW_PROJECT_BUTTON_LABEL, "New Project", ProjectManagementPanelNames.PROJECT_OVERVIEW_PANEL_NAME, null));
			add(createButton(PROP_IMPORT_PROJECT_BUTTON_LABEL, "Import Project", ProjectManagementPanelNames.PROJECT_IMPORT_PANEL_NAME, null));
		}
//		add(createButton(PROP_FIND_PROJECT_BUTTON_LABEL, "Find Project", ProjectManagementPanelNames.PROJECT_SEARCH_PANEL_NAME, getApp().getUser()));
		super.setup();
	}
	
	private Component createButton(String labelResourceName, String labelDefault, String panelName, Object target) {
		String buttonLabel = getResourceBundleHelper(getLocale()).getString(labelResourceName, labelDefault);
		NavigationEvent event = new OpenPanelEvent(this, PanelActionType.Editor, target, Project.class, panelName, WorkflowDisposition.NewFlow);
		NavigatorButton button = new NavigatorButton(buttonLabel, getEventDispatcher(), event);
		button.setStyleName(Panel.STYLE_NAME_DEFAULT);
		Row wrapper = new Row();
		wrapper.setInsets(new Insets(5));
		wrapper.setAlignment(new Alignment(Alignment.CENTER, Alignment.DEFAULT));
		wrapper.add(button);
		return wrapper;
	}
}
