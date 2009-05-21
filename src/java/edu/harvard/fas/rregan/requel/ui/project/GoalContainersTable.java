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
package edu.harvard.fas.rregan.requel.ui.project;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;

import nextapp.echo2.app.Alignment;
import nextapp.echo2.app.Component;
import nextapp.echo2.app.layout.ColumnLayoutData;
import nextapp.echo2.app.layout.RowLayoutData;
import edu.harvard.fas.rregan.ResourceBundleHelper;
import edu.harvard.fas.rregan.requel.project.Goal;
import edu.harvard.fas.rregan.requel.project.GoalContainer;
import edu.harvard.fas.rregan.requel.ui.AbstractRequelNavigatorTable;
import edu.harvard.fas.rregan.uiframework.navigation.NavigatorButton;
import edu.harvard.fas.rregan.uiframework.navigation.WorkflowDisposition;
import edu.harvard.fas.rregan.uiframework.navigation.event.NavigationEvent;
import edu.harvard.fas.rregan.uiframework.navigation.event.OpenPanelEvent;
import edu.harvard.fas.rregan.uiframework.navigation.table.NavigatorTable;
import edu.harvard.fas.rregan.uiframework.navigation.table.NavigatorTableCellValueFactory;
import edu.harvard.fas.rregan.uiframework.navigation.table.NavigatorTableColumnConfig;
import edu.harvard.fas.rregan.uiframework.navigation.table.NavigatorTableConfig;
import edu.harvard.fas.rregan.uiframework.navigation.table.NavigatorTableModel;
import edu.harvard.fas.rregan.uiframework.panel.Panel;
import edu.harvard.fas.rregan.uiframework.panel.PanelActionType;
import edu.harvard.fas.rregan.uiframework.panel.editor.EditMode;
import edu.harvard.fas.rregan.uiframework.panel.editor.manipulators.AbstractComponentManipulator;
import edu.harvard.fas.rregan.uiframework.panel.editor.manipulators.ComponentManipulators;

/**
 * Table displaying the goal containers of a goal
 * 
 * @author ron
 */
public class GoalContainersTable extends AbstractRequelNavigatorTable {
	static final long serialVersionUID = 0L;

	static {
		ComponentManipulators.setManipulator(GoalContainersTable.class,
				new GoalContainersTableManipulator());
	}

	/**
	 * The name to use in the properties file of the panel that includes the
	 * GoalContainersTable to define the label of the goal containers field. If
	 * the property is undefined the panel should use a sensible default such as
	 * "Goal Referers".
	 */
	public static final String PROP_LABEL_GOAL_CONTAINERS = "GoalContainers.Label";

	/**
	 * The name to use in the containing panels properties file to set the label
	 * of the view button in the goal containers edit table column. If the
	 * property is undefined "View" is used.
	 */
	public static final String PROP_VIEW_GOAL_CONTAINER_BUTTON_LABEL = "ViewGoalContainer.Label";

	/**
	 * The name to use in the containing panels properties file to set the label
	 * of the edit button in the goal container edit table column. If the
	 * property is undefined "Edit" is used.
	 */
	public static final String PROP_EDIT_GOAL_CONTAINER_BUTTON_LABEL = "EditGoalContainer.Label";

	private Goal goal;
	private final NavigatorTable table;

	/**
	 * @param editMode
	 * @param resourceBundleHelper
	 */
	public GoalContainersTable(EditMode editMode, ResourceBundleHelper resourceBundleHelper) {
		super(editMode, resourceBundleHelper);
		ColumnLayoutData layoutData = new ColumnLayoutData();
		layoutData.setAlignment(Alignment.ALIGN_CENTER);
		table = new NavigatorTable(getTableConfig());
		table.setLayoutData(layoutData);
		add(table);
	}

	protected Goal getGoal() {
		return goal;
	}

	protected void setGoal(Goal goal) {
		this.goal = goal;
		if (goal != null) {
			table.setModel(new NavigatorTableModel((Collection) goal.getReferers()));
		} else {
			table.setModel(new NavigatorTableModel(Collections.EMPTY_SET));
		}
	}

	private NavigatorTableConfig getTableConfig() {
		NavigatorTableConfig tableConfig = new NavigatorTableConfig();

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						GoalContainer goalContainer = (GoalContainer) model.getBackingObject(row);
						String buttonLabel = null;
						if (isReadOnlyMode()) {
							buttonLabel = getResourceBundleHelper(getLocale()).getString(
									PROP_VIEW_GOAL_CONTAINER_BUTTON_LABEL, "View");
						} else {
							buttonLabel = getResourceBundleHelper(getLocale()).getString(
									PROP_EDIT_GOAL_CONTAINER_BUTTON_LABEL, "Edit");
						}
						NavigationEvent openEditorEvent = new OpenPanelEvent(this,
								PanelActionType.Editor, goalContainer, goalContainer.getClass(),
								null, WorkflowDisposition.NewFlow);
						NavigatorButton openEditorButton = new NavigatorButton(buttonLabel,
								getEventDispatcher(), openEditorEvent);
						openEditorButton.setStyleName(Panel.STYLE_NAME_PLAIN);
						RowLayoutData rld = new RowLayoutData();
						rld.setAlignment(Alignment.ALIGN_CENTER);
						openEditorButton.setLayoutData(rld);
						return openEditorButton;
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Description",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						GoalContainer goalContainer = (GoalContainer) model.getBackingObject(row);
						return goalContainer.getDescription();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Created By",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						GoalContainer goalContainer = (GoalContainer) model.getBackingObject(row);
						return goalContainer.getCreatedBy().getUsername();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Date Created",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						GoalContainer goalContainer = (GoalContainer) model.getBackingObject(row);
						DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
						return formatter.format(goalContainer.getDateCreated());
					}
				}));

		return tableConfig;
	}

	private static class GoalContainersTableManipulator extends AbstractComponentManipulator {

		protected GoalContainersTableManipulator() {
			super();
		}

		@Override
		public Object getModel(Component component) {
			return getValue(component, Goal.class);
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
			return type.cast(getComponent(component).getGoal());
		}

		@Override
		public void setValue(Component component, Object value) {
			getComponent(component).setGoal((Goal) value);
		}

		private GoalContainersTable getComponent(Component component) {
			return (GoalContainersTable) component;
		}
	}
}
