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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;

import nextapp.echo2.app.Alignment;
import nextapp.echo2.app.Component;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.layout.ColumnLayoutData;
import nextapp.echo2.app.layout.RowLayoutData;
import net.sf.echopm.ResourceBundleHelper;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.GoalContainer;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.ui.AbstractRequelNavigatorTable;
import net.sf.echopm.navigation.NavigatorButton;
import net.sf.echopm.navigation.WorkflowDisposition;
import net.sf.echopm.navigation.event.NavigationEvent;
import net.sf.echopm.navigation.event.OpenPanelEvent;
import net.sf.echopm.navigation.event.SelectEntityEvent;
import net.sf.echopm.navigation.table.NavigatorTable;
import net.sf.echopm.navigation.table.NavigatorTableCellValueFactory;
import net.sf.echopm.navigation.table.NavigatorTableColumnConfig;
import net.sf.echopm.navigation.table.NavigatorTableConfig;
import net.sf.echopm.navigation.table.NavigatorTableModel;
import net.sf.echopm.panel.Panel;
import net.sf.echopm.panel.PanelActionType;
import net.sf.echopm.panel.editor.EditMode;
import net.sf.echopm.panel.editor.manipulators.AbstractComponentManipulator;
import net.sf.echopm.panel.editor.manipulators.ComponentManipulators;

/**
 * A component to add to Panels of goal container entity editors to enable
 * editing of the goals of the entity.
 * 
 * @author ron
 */
public class GoalsTable extends AbstractRequelNavigatorTable {
	static final long serialVersionUID = 0L;

	static {
		ComponentManipulators.setManipulator(GoalsTable.class, new GoalsTableManipulator());
	}

	/**
	 * The name to use in the properties file of the panel that includes the
	 * GoalsTable to define the label of the goals field. If the property is
	 * undefined the panel should use a sensible default such as "Goals".
	 */
	public static final String PROP_LABEL_GOALS = "Goals.Label";

	/**
	 * The name to use in the containing panels properties file to set the label
	 * of the view button in the goal edit table column. If the property is
	 * undefined "View" is used.
	 */
	public static final String PROP_VIEW_GOAL_BUTTON_LABEL = "ViewGoal.Label";

	/**
	 * The name to use in the containing panels properties file to set the label
	 * of the remove button in the goal edit table column. If the property is
	 * undefined "Remove" is used.
	 */
	public static final String PROP_REMOVE_GOAL_BUTTON_LABEL = "RemoveGoal.Label";

	/**
	 * The name to use in the containing panels properties file to set the label
	 * of the edit button in the goal edit table column. If the property is
	 * undefined "Edit" is used.
	 */
	public static final String PROP_EDIT_GOAL_BUTTON_LABEL = "EditGoal.Label";

	/**
	 * The name to use in the containing panels properties file to set the label
	 * of the new goal button under the goals table. If the property is
	 * undefined "New" is used.
	 */
	public static final String PROP_NEW_GOAL_BUTTON_LABEL = "NewGoal.Label";

	/**
	 * The name to use in the containing panels properties file to set the label
	 * of the find goal button under the goals table. If the property is
	 * undefined "Find" is used.
	 */
	public static final String PROP_FIND_GOAL_BUTTON_LABEL = "FindGoal.Label";

	private GoalContainer goalContainer;
	private final NavigatorTable table;
	private final NavigatorButton openGoalEditorButton;
	private final NavigatorButton openGoalSelectorButton;
	private final ProjectCommandFactory projectCommandFactory;
	private final CommandHandler commandHandler;
	private AddGoalToGoalContainerController addGoalController;
	private RemoveGoalFromGoalContainerController removeGoalController;

	/**
	 * @param editMode
	 * @param resourceBundleHelper
	 * @param projectCommandFactory -
	 *            passed to the add/remove goal to goal container controller
	 * @param commandHandler -
	 *            passed to the add/remove goal to goal container controller
	 */
	public GoalsTable(EditMode editMode, ResourceBundleHelper resourceBundleHelper,
			ProjectCommandFactory projectCommandFactory, CommandHandler commandHandler) {
		super(editMode, resourceBundleHelper);
		this.projectCommandFactory = projectCommandFactory;
		this.commandHandler = commandHandler;
		ColumnLayoutData layoutData = new ColumnLayoutData();
		layoutData.setAlignment(Alignment.ALIGN_CENTER);
		table = new NavigatorTable(getTableConfig());
		table.setLayoutData(layoutData);
		add(table);

		Row buttons = new Row();
		buttons.setLayoutData(layoutData);

		String buttonLabel = getResourceBundleHelper(getLocale()).getString(
				PROP_NEW_GOAL_BUTTON_LABEL, "New");
		openGoalEditorButton = new NavigatorButton(buttonLabel, getEventDispatcher());
		openGoalEditorButton.setStyleName(Panel.STYLE_NAME_DEFAULT);
		openGoalEditorButton.setVisible(false);
		buttons.add(openGoalEditorButton);

		buttonLabel = getResourceBundleHelper(getLocale()).getString(PROP_FIND_GOAL_BUTTON_LABEL,
				"Find");
		openGoalSelectorButton = new NavigatorButton(buttonLabel, getEventDispatcher());
		openGoalSelectorButton.setStyleName(Panel.STYLE_NAME_DEFAULT);
		openGoalSelectorButton.setVisible(false);
		buttons.add(openGoalSelectorButton);

		add(buttons);

	}

	protected GoalContainer getGoalContainer() {
		return goalContainer;
	}

	protected void setGoalContainer(GoalContainer goalContainer) {
		this.goalContainer = goalContainer;

		if (goalContainer != null) {
			table.setModel(new NavigatorTableModel((Collection) goalContainer.getGoals()));
			if (!isReadOnlyMode()) {
				NavigationEvent openEditorEvent = new OpenPanelEvent(this, PanelActionType.Editor,
						getGoalContainer(), Goal.class, null, WorkflowDisposition.NewFlow);
				openGoalEditorButton.setEventToFire(openEditorEvent);
				openGoalEditorButton.setVisible(true);

				ProjectOrDomain pod = null;
				if (getGoalContainer() instanceof ProjectOrDomain) {
					pod = (ProjectOrDomain) getGoalContainer();
				} else if (getGoalContainer() instanceof ProjectOrDomainEntity) {
					pod = ((ProjectOrDomainEntity) getGoalContainer()).getProjectOrDomain();
				}
				if (pod != null) {
					NavigationEvent openGoalSelectorEvent = new OpenPanelEvent(this,
							PanelActionType.Selector, pod, Project.class,
							ProjectManagementPanelNames.PROJECT_GOALS_SELECTOR_PANEL_NAME,
							WorkflowDisposition.ContinueFlow);

					openGoalSelectorButton.setEventToFire(openGoalSelectorEvent);
					openGoalSelectorButton.setVisible(true);
				} else {
					openGoalSelectorButton.setVisible(false);
				}

				// use the the goal table (this) as the destination because it
				// is used as the source to the open panel events created above
				if (addGoalController != null) {
					getEventDispatcher().removeEventTypeActionListener(SelectEntityEvent.class,
							addGoalController, this);
				}
				addGoalController = new AddGoalToGoalContainerController(getEventDispatcher(),
						projectCommandFactory, commandHandler, goalContainer);
				getEventDispatcher().addEventTypeActionListener(SelectEntityEvent.class,
						addGoalController, this);

				// use the the goal table (this) as the destination because it
				// is used as the source to the open panel events created above
				if (removeGoalController != null) {
					getEventDispatcher().removeEventTypeActionListener(
							RemoveGoalFromGoalContainerEvent.class, removeGoalController, this);
				}
				removeGoalController = new RemoveGoalFromGoalContainerController(
						getEventDispatcher(), projectCommandFactory, commandHandler, goalContainer);
				getEventDispatcher().addEventTypeActionListener(
						RemoveGoalFromGoalContainerEvent.class, removeGoalController, this);

			}
		} else {
			table.setModel(new NavigatorTableModel(Collections.EMPTY_SET));
			openGoalEditorButton.setVisible(false);
			openGoalSelectorButton.setVisible(false);
		}
	}

	private NavigatorTableConfig getTableConfig() {
		NavigatorTableConfig tableConfig = new NavigatorTableConfig();

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Row buttonsContainer = new Row();
						RowLayoutData buttonLayout = new RowLayoutData();
						buttonLayout.setAlignment(Alignment.ALIGN_CENTER);
						buttonLayout.setInsets(new Insets(5, 0));

						Goal goal = (Goal) model.getBackingObject(row);
						String buttonLabel = null;
						if (isReadOnlyMode()) {
							buttonLabel = getResourceBundleHelper(getLocale()).getString(
									PROP_VIEW_GOAL_BUTTON_LABEL, "View");
						} else {
							buttonLabel = getResourceBundleHelper(getLocale()).getString(
									PROP_EDIT_GOAL_BUTTON_LABEL, "Edit");
						}
						NavigationEvent openEditorEvent = new OpenPanelEvent(GoalsTable.this,
								PanelActionType.Editor, goal, goal.getClass(), null,
								WorkflowDisposition.NewFlow);
						NavigatorButton openEditorButton = new NavigatorButton(buttonLabel,
								getEventDispatcher(), openEditorEvent);
						openEditorButton.setStyleName(Panel.STYLE_NAME_PLAIN);
						openEditorButton.setLayoutData(buttonLayout);
						buttonsContainer.add(openEditorButton);

						if (!isReadOnlyMode()) {
							buttonLabel = getResourceBundleHelper(getLocale()).getString(
									PROP_REMOVE_GOAL_BUTTON_LABEL, "Remove");
							NavigationEvent removeGoalEvent = new RemoveGoalFromGoalContainerEvent(
									GoalsTable.this, goal, goalContainer, GoalsTable.this);
							NavigatorButton removeGoalButton = new NavigatorButton(buttonLabel,
									getEventDispatcher(), removeGoalEvent);
							removeGoalButton.setStyleName(Panel.STYLE_NAME_PLAIN);
							removeGoalButton.setLayoutData(buttonLayout);
							buttonsContainer.add(removeGoalButton);
						}
						return buttonsContainer;
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Name",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Goal goal = (Goal) model.getBackingObject(row);
						return goal.getName();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Created By",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Goal goal = (Goal) model.getBackingObject(row);
						return goal.getCreatedBy().getUsername();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Date Created",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Goal goal = (Goal) model.getBackingObject(row);
						DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
						return formatter.format(goal.getDateCreated());
					}
				}));

		return tableConfig;
	}

	private static class GoalsTableManipulator extends AbstractComponentManipulator {

		protected GoalsTableManipulator() {
			super();
		}

		@Override
		public Object getModel(Component component) {
			return getValue(component, GoalContainer.class);
		}

		@Override
		public void setModel(Component component, Object valueModel) {
			setValue(component, valueModel);
		}

		@Override
		public void addListenerToDetectChangesToInput(EditMode editMode, Component component) {
			// nothing to do.
		}

		@Override
		public <T> T getValue(Component component, Class<T> type) {
			return type.cast(getComponent(component).getGoalContainer());
		}

		@Override
		public void setValue(Component component, Object value) {
			getComponent(component).setGoalContainer((GoalContainer) value);
		}

		private GoalsTable getComponent(Component component) {
			return (GoalsTable) component;
		}
	}
}
