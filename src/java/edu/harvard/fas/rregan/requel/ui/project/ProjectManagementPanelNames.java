/*
 * $Id: ProjectManagementPanelNames.java,v 1.6 2008/12/31 11:49:35 rregan Exp $
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

/**
 * This defines constants for all the panel names used in the project management
 * package. These project names will be referenced by OpenPanelEvents, the
 * panels that implement the named panels, and PanelFactories for creating the
 * panels (typically in the spring navigationContext.xml configuration file.)
 * 
 * @author ron
 */
public interface ProjectManagementPanelNames {

	/**
	 * The name of the panel to use for editing the project overview. A panel
	 * implementation that is to be used for the project overview should
	 * register itself with this name, the panel factory for creating it and
	 * events for opening it.
	 */
	public static final String PROJECT_OVERVIEW_PANEL_NAME = "projectOverview";

	/**
	 * The name of the panel for navigating project stakeholders.
	 */
	public static final String PROJECT_STAKEHOLDERS_NAVIGATOR_PANEL_NAME = "projectStakeholdersNavigator";

	/**
	 * The name of the panel for navigating project use cases.
	 */
	public static final String PROJECT_USE_CASES_NAVIGATOR_PANEL_NAME = "projectUseCasesNavigator";

	/**
	 * The name of the panel for selecting a use case in a project.
	 */
	public static final String PROJECT_USE_CASES_SELECTOR_PANEL_NAME = "projectUseCasesSelector";

	/**
	 * The name of the panel for navigating project scenarios.
	 */
	public static final String PROJECT_SCENARIOS_NAVIGATOR_PANEL_NAME = "projectScenariosNavigator";

	/**
	 * The name of the panel for selecting a scenario in a project.
	 */
	public static final String PROJECT_SCENARIO_SELECTOR_PANEL_NAME = "projectScenarioSelector";

	/**
	 * The name of the panel for navigating project stories.
	 */
	public static final String PROJECT_STORIES_NAVIGATOR_PANEL_NAME = "projectStoriesNavigator";

	/**
	 * The name of the panel for selecting a story in a project.
	 */
	public static final String PROJECT_STORY_SELECTOR_PANEL_NAME = "projectStorySelector";

	/**
	 * The name of the panel for navigating project actors.
	 */
	public static final String PROJECT_ACTORS_NAVIGATOR_PANEL_NAME = "projectActorsNavigator";

	/**
	 * The name of the panel for selecting an actor in a project.
	 */
	public static final String PROJECT_ACTORS_SELECTOR_PANEL_NAME = "projectActorsSelector";

	/**
	 * The name of the panel for navigating project goals.
	 */
	public static final String PROJECT_GOALS_NAVIGATOR_PANEL_NAME = "projectGoalsNavigator";

	/**
	 * The name of the panel for selecting a goal in a project.
	 */
	public static final String PROJECT_GOALS_SELECTOR_PANEL_NAME = "projectGoalsSelector";

	/**
	 * The name of the panel for navigating project stakeholders.
	 */
	public static final String PROJECT_GLOSSARY_TERMS_NAVIGATOR_PANEL_NAME = "projectGlossaryTermsNavigator";

	/**
	 * The name of the panel for navigating project goals.
	 */
	public static final String PROJECT_GLOSSARY_TERM_SELECTOR_PANEL_NAME = "projectGlossaryTermsSelector";

	/**
	 * The name of the panel for navigating project stakeholders.
	 */
	public static final String PROJECT_REPORTS_NAVIGATOR_PANEL_NAME = "projectReportsNavigator";

	/**
	 * The name of the panel for navigating the open issues for all entities in
	 * a project.
	 */
	public static final String PROJECT_OPEN_ISSUES_NAVIGATOR_PANEL_NAME = "projectOpenIssuesNavigator";

}
