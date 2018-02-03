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
package com.rreganjr.requel.ui.project;

import nextapp.echo2.app.Label;
import nextapp.echo2.app.event.ActionEvent;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import echopointng.tree.MutableTreeNode;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import net.sf.echopm.navigation.WorkflowDisposition;
import net.sf.echopm.navigation.event.DeletedEntityEvent;
import net.sf.echopm.navigation.event.EventDispatcher;
import net.sf.echopm.navigation.event.NavigationEvent;
import net.sf.echopm.navigation.event.OpenPanelEvent;
import net.sf.echopm.navigation.event.UpdateEntityEvent;
import net.sf.echopm.navigation.tree.AbstractNavigatorTreeNodeFactory;
import net.sf.echopm.navigation.tree.NavigatorTree;
import net.sf.echopm.navigation.tree.NavigatorTreeNode;
import net.sf.echopm.navigation.tree.NavigatorTreeNodeUpdateListener;
import net.sf.echopm.panel.PanelActionType;

/**
 * @author ron
 */
@Component("projectNavigatorTreeNodeFactory")
@Scope("singleton")
public class ProjectNavigatorTreeNodeFactory extends AbstractNavigatorTreeNodeFactory {

	/**
	 * The property name to use to control the label on the stakeholders node
	 * generated by the factory.
	 */
	public final static String PROP_STAKEHOLDERS_NODE_LABEL = "StakeholdersNodeLabel";

	/**
	 * The property name to use to control the label on the goals node generated
	 * by the factory.
	 */
	public final static String PROP_GOALS_NODE_LABEL = "GoalsNodeLabel";

	/**
	 * The property name to use to control the label on the stories node
	 * generated by the factory.
	 */
	public final static String PROP_STORIES_NODE_LABEL = "StoriesNodeLabel";

	/**
	 * The property name to use to control the label on the stories node
	 * generated by the factory.
	 */
	public final static String PROP_ACTORS_NODE_LABEL = "ActorsNodeLabel";

	/**
	 * The property name to use to control the label on the stories node
	 * generated by the factory.
	 */
	public final static String PROP_USE_CASES_NODE_LABEL = "UseCasesNodeLabel";

	/**
	 * The property name to use to control the label on the stories node
	 * generated by the factory.
	 */
	public final static String PROP_SCENARIOS_NODE_LABEL = "ScenariosNodeLabel";

	/**
	 * The property name to use to control the label on the glossary terms node
	 * generated by the factory.
	 */
	public final static String PROP_GLOSSARY_TERMS_NODE_LABEL = "GlossaryTermNodeLabel";

	/**
	 * The property name to use to control the label on the glossary terms node
	 * generated by the factory.
	 */
	public final static String PROP_REPORT_GENERATORS_NODE_LABEL = "ReportGeneratorsNodeLabel";

	/**
	 * The property name to use to control the label on the glossary terms node
	 * generated by the factory.
	 */
	public final static String PROP_OPEN_ISSUES_NODE_LABEL = "OpenIssuesNodeLabel";

	/**
	 * @param eventDispatcher
	 */
	public ProjectNavigatorTreeNodeFactory() {
		super(ProjectNavigatorTreeNodeFactory.class.getName(), Project.class);
	}

	/**
	 * @see net.sf.echopm.navigation.tree.NavigatorTreeNodeFactory#createTreeNode(net.sf.echopm.navigation.tree.NavigatorTree,
	 *      java.lang.Object)
	 */
	public MutableTreeNode createTreeNode(EventDispatcher eventDispatcher, NavigatorTree tree,
			Object object) {
		Project project = (Project) object;

		NavigationEvent openProjectOverview = new OpenPanelEvent(tree, PanelActionType.Editor,
				project, Project.class, ProjectManagementPanelNames.PROJECT_OVERVIEW_PANEL_NAME,
				WorkflowDisposition.NewFlow);

		NavigatorTreeNode projectTreeNode = new NavigatorTreeNode(eventDispatcher, project,
				new Label(project.getName()), openProjectOverview);

		// stakeholders
		String stakeholdersNodeLabel = getResourceBundleHelper(tree.getLocale()).getString(
				PROP_STAKEHOLDERS_NODE_LABEL, "Stakeholders");

		NavigationEvent openProjectStakeholders = new OpenPanelEvent(tree,
				PanelActionType.Navigator, project, Project.class,
				ProjectManagementPanelNames.PROJECT_STAKEHOLDERS_NAVIGATOR_PANEL_NAME,
				WorkflowDisposition.NewFlow);

		NavigatorTreeNode stakeholdersTreeNode = new NavigatorTreeNode(eventDispatcher, project,
				new Label(stakeholdersNodeLabel), openProjectStakeholders);

		projectTreeNode.add(stakeholdersTreeNode);

		// goals
		String goalsNodeLabel = getResourceBundleHelper(tree.getLocale()).getString(
				PROP_GOALS_NODE_LABEL, "Goals");

		NavigationEvent openProjectGoals = new OpenPanelEvent(tree, PanelActionType.Navigator,
				project, Project.class,
				ProjectManagementPanelNames.PROJECT_GOALS_NAVIGATOR_PANEL_NAME,
				WorkflowDisposition.NewFlow);

		NavigatorTreeNode goalsTreeNode = new NavigatorTreeNode(eventDispatcher, project,
				new Label(goalsNodeLabel), openProjectGoals);

		projectTreeNode.add(goalsTreeNode);

		// stories
		String storiesNodeLabel = getResourceBundleHelper(tree.getLocale()).getString(
				PROP_STORIES_NODE_LABEL, "Stories");

		NavigationEvent openProjectStories = new OpenPanelEvent(tree, PanelActionType.Navigator,
				project, Project.class,
				ProjectManagementPanelNames.PROJECT_STORIES_NAVIGATOR_PANEL_NAME,
				WorkflowDisposition.NewFlow);

		NavigatorTreeNode storiesTreeNode = new NavigatorTreeNode(eventDispatcher, project,
				new Label(storiesNodeLabel), openProjectStories);

		projectTreeNode.add(storiesTreeNode);

		// actors
		String actorsNodeLabel = getResourceBundleHelper(tree.getLocale()).getString(
				PROP_ACTORS_NODE_LABEL, "Actors");

		NavigationEvent openProjectActors = new OpenPanelEvent(tree, PanelActionType.Navigator,
				project, Project.class,
				ProjectManagementPanelNames.PROJECT_ACTORS_NAVIGATOR_PANEL_NAME,
				WorkflowDisposition.NewFlow);

		NavigatorTreeNode actorsTreeNode = new NavigatorTreeNode(eventDispatcher, project,
				new Label(actorsNodeLabel), openProjectActors);

		projectTreeNode.add(actorsTreeNode);

		// useCases
		String useCasesNodeLabel = getResourceBundleHelper(tree.getLocale()).getString(
				PROP_USE_CASES_NODE_LABEL, "Use Cases");

		NavigationEvent openProjectUseCases = new OpenPanelEvent(tree, PanelActionType.Navigator,
				project, Project.class,
				ProjectManagementPanelNames.PROJECT_USE_CASES_NAVIGATOR_PANEL_NAME,
				WorkflowDisposition.NewFlow);

		NavigatorTreeNode useCasesTreeNode = new NavigatorTreeNode(eventDispatcher, project,
				new Label(useCasesNodeLabel), openProjectUseCases);

		projectTreeNode.add(useCasesTreeNode);

		// scenarios
		String scenariosNodeLabel = getResourceBundleHelper(tree.getLocale()).getString(
				PROP_SCENARIOS_NODE_LABEL, "Scenarios");

		NavigationEvent openProjectScenarios = new OpenPanelEvent(tree, PanelActionType.Navigator,
				project, Project.class,
				ProjectManagementPanelNames.PROJECT_SCENARIOS_NAVIGATOR_PANEL_NAME,
				WorkflowDisposition.NewFlow);

		NavigatorTreeNode scenariosTreeNode = new NavigatorTreeNode(eventDispatcher, project,
				new Label(scenariosNodeLabel), openProjectScenarios);

		projectTreeNode.add(scenariosTreeNode);

		// glossary terms
		String termsNodeLabel = getResourceBundleHelper(tree.getLocale()).getString(
				PROP_GLOSSARY_TERMS_NODE_LABEL, "Terms");

		NavigationEvent openProjectTerms = new OpenPanelEvent(tree, PanelActionType.Navigator,
				project, Project.class,
				ProjectManagementPanelNames.PROJECT_GLOSSARY_TERMS_NAVIGATOR_PANEL_NAME,
				WorkflowDisposition.NewFlow);

		NavigatorTreeNode termsTreeNode = new NavigatorTreeNode(eventDispatcher, project,
				new Label(termsNodeLabel), openProjectTerms);

		projectTreeNode.add(termsTreeNode);

		// reports
		String reportsNodeLabel = getResourceBundleHelper(tree.getLocale()).getString(
				PROP_REPORT_GENERATORS_NODE_LABEL, "Documents");

		NavigationEvent openProjectReports = new OpenPanelEvent(tree, PanelActionType.Navigator,
				project, Project.class,
				ProjectManagementPanelNames.PROJECT_REPORTS_NAVIGATOR_PANEL_NAME,
				WorkflowDisposition.NewFlow);

		NavigatorTreeNode reportsTreeNode = new NavigatorTreeNode(eventDispatcher, project,
				new Label(reportsNodeLabel), openProjectReports);

		projectTreeNode.add(reportsTreeNode);

		// open issues
		String openIssuesNodeLabel = getResourceBundleHelper(tree.getLocale()).getString(
				PROP_OPEN_ISSUES_NODE_LABEL, "Open Issues");

		NavigationEvent openProjectIssues = new OpenPanelEvent(tree, PanelActionType.Navigator,
				project, Project.class,
				ProjectManagementPanelNames.PROJECT_OPEN_ISSUES_NAVIGATOR_PANEL_NAME,
				WorkflowDisposition.NewFlow);

		NavigatorTreeNode openIssuesTreeNode = new NavigatorTreeNode(eventDispatcher, project,
				new Label(openIssuesNodeLabel), openProjectIssues);

		projectTreeNode.add(openIssuesTreeNode);

		// TODO: when adding a new child node, update the update listener below
		projectTreeNode.setUpdateListener(new UpdateListener(projectTreeNode));
		return projectTreeNode;
	}

	private static class UpdateListener implements NavigatorTreeNodeUpdateListener {
		static final long serialVersionUID = 0L;

		private final NavigatorTreeNode projectTreeNode;

		private UpdateListener(NavigatorTreeNode projectTreeNode) {
			this.projectTreeNode = projectTreeNode;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e instanceof UpdateEntityEvent) {
				UpdateEntityEvent event = (UpdateEntityEvent) e;
				ProjectOrDomain updatedProject = null;
				ProjectOrDomain existingProject = (ProjectOrDomain) projectTreeNode
						.getTargetObject();

				if (event.getObject() instanceof ProjectOrDomainEntity) {
					ProjectOrDomainEntity entity = (ProjectOrDomainEntity) event.getObject();
					updatedProject = entity.getProjectOrDomain();
				} else if (event.getObject() instanceof ProjectOrDomain) {
					updatedProject = (ProjectOrDomain) event.getObject();
				} else if (event.getObject() instanceof Annotation) {
					Annotation updatedAnnotation = (Annotation) event.getObject();
					if (event instanceof DeletedEntityEvent) {
						if (existingProject instanceof Project) {
							updatedProject = existingProject;
							if (((Project) updatedProject).getAnnotations().contains(
									updatedAnnotation)) {
								((Project) updatedProject).getAnnotations().remove(
										updatedAnnotation);
							}
						}
					} else {
						for (Annotatable annotatable : updatedAnnotation.getAnnotatables()) {
							if (annotatable instanceof ProjectOrDomainEntity) {
								ProjectOrDomainEntity entity = (ProjectOrDomainEntity) annotatable;
								updatedProject = entity.getProjectOrDomain();
								break;
							} else if (annotatable instanceof ProjectOrDomain) {
								updatedProject = (ProjectOrDomain) annotatable;
								break;
							}
						}
					}
				}
				if ((updatedProject != null) && updatedProject.equals(existingProject)) {
					for (int i = 0; i < projectTreeNode.getChildCount(); i++) {
						NavigatorTreeNode child = (NavigatorTreeNode) projectTreeNode.getChildAt(i);
						OpenPanelEvent eventToFire = (OpenPanelEvent) child.getEventToFire();
						if (ProjectManagementPanelNames.PROJECT_OVERVIEW_PANEL_NAME
								.equals(eventToFire.getPanelName())) {
							NavigationEvent openProjectOverview = new OpenPanelEvent(
									projectTreeNode, PanelActionType.Editor, updatedProject,
									Project.class,
									ProjectManagementPanelNames.PROJECT_OVERVIEW_PANEL_NAME,
									WorkflowDisposition.NewFlow);
							child.setEventToFire(openProjectOverview);
						} else if (ProjectManagementPanelNames.PROJECT_STAKEHOLDERS_NAVIGATOR_PANEL_NAME
								.equals(eventToFire.getPanelName())) {
							NavigationEvent openProjectStakeholders = new OpenPanelEvent(
									projectTreeNode,
									PanelActionType.Navigator,
									updatedProject,
									Project.class,
									ProjectManagementPanelNames.PROJECT_STAKEHOLDERS_NAVIGATOR_PANEL_NAME,
									WorkflowDisposition.NewFlow);
							child.setEventToFire(openProjectStakeholders);
						} else if (ProjectManagementPanelNames.PROJECT_GOALS_NAVIGATOR_PANEL_NAME
								.equals(eventToFire.getPanelName())) {
							NavigationEvent openProjectGoals = new OpenPanelEvent(projectTreeNode,
									PanelActionType.Navigator, updatedProject, Project.class,
									ProjectManagementPanelNames.PROJECT_GOALS_NAVIGATOR_PANEL_NAME,
									WorkflowDisposition.NewFlow);
							child.setEventToFire(openProjectGoals);
						} else if (ProjectManagementPanelNames.PROJECT_STORIES_NAVIGATOR_PANEL_NAME
								.equals(eventToFire.getPanelName())) {
							NavigationEvent openProjectGoals = new OpenPanelEvent(
									projectTreeNode,
									PanelActionType.Navigator,
									updatedProject,
									Project.class,
									ProjectManagementPanelNames.PROJECT_STORIES_NAVIGATOR_PANEL_NAME,
									WorkflowDisposition.NewFlow);
							child.setEventToFire(openProjectGoals);
						} else if (ProjectManagementPanelNames.PROJECT_GLOSSARY_TERMS_NAVIGATOR_PANEL_NAME
								.equals(eventToFire.getPanelName())) {
							NavigationEvent openProjectGoals = new OpenPanelEvent(
									projectTreeNode,
									PanelActionType.Navigator,
									updatedProject,
									Project.class,
									ProjectManagementPanelNames.PROJECT_GLOSSARY_TERMS_NAVIGATOR_PANEL_NAME,
									WorkflowDisposition.NewFlow);
							child.setEventToFire(openProjectGoals);
						} else if (ProjectManagementPanelNames.PROJECT_ACTORS_NAVIGATOR_PANEL_NAME
								.equals(eventToFire.getPanelName())) {
							NavigationEvent openProjectGoals = new OpenPanelEvent(
									projectTreeNode,
									PanelActionType.Navigator,
									updatedProject,
									Project.class,
									ProjectManagementPanelNames.PROJECT_ACTORS_NAVIGATOR_PANEL_NAME,
									WorkflowDisposition.NewFlow);
							child.setEventToFire(openProjectGoals);
						} else if (ProjectManagementPanelNames.PROJECT_USE_CASES_NAVIGATOR_PANEL_NAME
								.equals(eventToFire.getPanelName())) {
							NavigationEvent openProjectUseCases = new OpenPanelEvent(
									projectTreeNode,
									PanelActionType.Navigator,
									updatedProject,
									Project.class,
									ProjectManagementPanelNames.PROJECT_USE_CASES_NAVIGATOR_PANEL_NAME,
									WorkflowDisposition.NewFlow);
							child.setEventToFire(openProjectUseCases);
						} else if (ProjectManagementPanelNames.PROJECT_SCENARIOS_NAVIGATOR_PANEL_NAME
								.equals(eventToFire.getPanelName())) {
							NavigationEvent openProjectUseCases = new OpenPanelEvent(
									projectTreeNode,
									PanelActionType.Navigator,
									updatedProject,
									Project.class,
									ProjectManagementPanelNames.PROJECT_SCENARIOS_NAVIGATOR_PANEL_NAME,
									WorkflowDisposition.NewFlow);
							child.setEventToFire(openProjectUseCases);
						} else if (ProjectManagementPanelNames.PROJECT_REPORTS_NAVIGATOR_PANEL_NAME
								.equals(eventToFire.getPanelName())) {
							NavigationEvent openProjectReportGenerators = new OpenPanelEvent(
									projectTreeNode,
									PanelActionType.Navigator,
									updatedProject,
									Project.class,
									ProjectManagementPanelNames.PROJECT_REPORTS_NAVIGATOR_PANEL_NAME,
									WorkflowDisposition.NewFlow);
							child.setEventToFire(openProjectReportGenerators);
						} else if (ProjectManagementPanelNames.PROJECT_OPEN_ISSUES_NAVIGATOR_PANEL_NAME
								.equals(eventToFire.getPanelName())) {
							NavigationEvent openProjectReportGenerators = new OpenPanelEvent(
									projectTreeNode,
									PanelActionType.Navigator,
									updatedProject,
									Project.class,
									ProjectManagementPanelNames.PROJECT_OPEN_ISSUES_NAVIGATOR_PANEL_NAME,
									WorkflowDisposition.NewFlow);
							child.setEventToFire(openProjectReportGenerators);
						} else {
							throw new RuntimeException(
									"The ProjectNavigatorTreeNodeFactory UpdateListener is missing configuration for node with event: "
											+ eventToFire);
						}
					}
				}
			}
		}
	}
}