/*
 * $Id: ProjectNavigatorPanel.java,v 1.2 2008/12/31 11:49:35 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
