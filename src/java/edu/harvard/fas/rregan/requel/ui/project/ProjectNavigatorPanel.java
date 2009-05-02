/*
 * $Id: ProjectNavigatorPanel.java,v 1.2 2008/12/31 11:49:35 rregan Exp $
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
package edu.harvard.fas.rregan.requel.ui.project;

import java.util.Set;

import nextapp.echo2.app.Alignment;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Row;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.requel.project.Project;
import edu.harvard.fas.rregan.requel.project.ProjectUserRole;
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
public class ProjectNavigatorPanel extends NavigatorTreePanel {
	private static final Logger log = Logger.getLogger(ProjectNavigatorPanel.class);
	static final long serialVersionUID = 0;

	/**
	 * Property name to use in the ProjectNavigatorPanel.properties to set the
	 * lable on the new project button.
	 */
	public static final String PROP_NEW_PROJECT_BUTTON_LABEL = "NewProjectButton.Label";

	/**
	 * Property name to use in the ProjectNavigatorPanel.properties to set the
	 * lable on the find/open projects button.
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
			String newProjectButtonLabel = getResourceBundleHelper(getLocale()).getString(
					PROP_NEW_PROJECT_BUTTON_LABEL, "New Project");

			NavigationEvent openProjectOverview = new OpenPanelEvent(this, PanelActionType.Editor,
					null, Project.class, "projectOverview", WorkflowDisposition.NewFlow);

			NavigatorButton newProjectButton = new NavigatorButton(newProjectButtonLabel,
					getEventDispatcher(), openProjectOverview);

			newProjectButton.setStyleName(STYLE_NAME_DEFAULT);
			Row newProjectButtonWrapper = new Row();
			newProjectButtonWrapper.setInsets(new Insets(5));
			newProjectButtonWrapper
					.setAlignment(new Alignment(Alignment.CENTER, Alignment.DEFAULT));
			newProjectButtonWrapper.add(newProjectButton);
			add(newProjectButtonWrapper);
		}

		String findProjectButtonLabel = getResourceBundleHelper(getLocale()).getString(
				PROP_FIND_PROJECT_BUTTON_LABEL, "Find Project");

		NavigationEvent openProjectSearch = new OpenPanelEvent(this, PanelActionType.Selector,
				getApp().getUser(), Project.class, "projectSearch", WorkflowDisposition.NewFlow);

		NavigatorButton findProjectButton = new NavigatorButton(findProjectButtonLabel,
				getEventDispatcher(), openProjectSearch);

		findProjectButton.setStyleName(STYLE_NAME_DEFAULT);
		Row findProjectButtonWrapper = new Row();
		findProjectButtonWrapper.setInsets(new Insets(5));
		findProjectButtonWrapper.setAlignment(new Alignment(Alignment.CENTER, Alignment.DEFAULT));
		findProjectButtonWrapper.add(findProjectButton);
		// TODO: project search is not implemented.
		// add(findProjectButtonWrapper);

		super.setup();
	}
}
