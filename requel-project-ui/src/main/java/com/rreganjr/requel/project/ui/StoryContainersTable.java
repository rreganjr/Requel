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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;

import nextapp.echo2.app.Alignment;
import nextapp.echo2.app.Component;
import nextapp.echo2.app.layout.ColumnLayoutData;
import nextapp.echo2.app.layout.RowLayoutData;
import net.sf.echopm.ResourceBundleHelper;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.StoryContainer;
import com.rreganjr.requel.ui.AbstractRequelNavigatorTable;
import net.sf.echopm.navigation.NavigatorButton;
import net.sf.echopm.navigation.WorkflowDisposition;
import net.sf.echopm.navigation.event.NavigationEvent;
import net.sf.echopm.navigation.event.OpenPanelEvent;
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
 * A component to add to Panels of Story container entity editors to enable
 * editing of the Stories of the entity.
 * 
 * @author ron
 */
public class StoryContainersTable extends AbstractRequelNavigatorTable {
	static final long serialVersionUID = 0L;

	static {
		ComponentManipulators.setManipulator(StoryContainersTable.class,
				new StoryContainersTableManipulator());
	}

	/**
	 * The name to use in the properties file of the panel that includes the
	 * StoryContainersTable to define the label of the Story containers field.
	 * If the property is undefined the panel should use a sensible default such
	 * as "Story Referers".
	 */
	public static final String PROP_LABEL_STORY_CONTAINERS = "StoryContainers.Label";

	/**
	 * The name to use in the containing panels properties file to set the label
	 * of the view button in the Story containers edit table column. If the
	 * property is undefined "View" is used.
	 */
	public static final String PROP_VIEW_STORY_CONTAINER_BUTTON_LABEL = "ViewStoryContainer.Label";

	/**
	 * The name to use in the containing panels properties file to set the label
	 * of the edit button in the Story container edit table column. If the
	 * property is undefined "Edit" is used.
	 */
	public static final String PROP_EDIT_STORY_CONTAINER_BUTTON_LABEL = "EditStoryContainer.Label";

	private Story story;
	private final NavigatorTable table;

	/**
	 * @param editMode
	 * @param resourceBundleHelper
	 */
	public StoryContainersTable(EditMode editMode, ResourceBundleHelper resourceBundleHelper) {
		super(editMode, resourceBundleHelper);
		ColumnLayoutData layoutData = new ColumnLayoutData();
		layoutData.setAlignment(Alignment.ALIGN_CENTER);
		table = new NavigatorTable(getTableConfig());
		table.setLayoutData(layoutData);
		add(table);
	}

	protected Story getStory() {
		return story;
	}

	protected void setStory(Story story) {
		this.story = story;
		if (story != null) {
			table.setModel(new NavigatorTableModel((Collection) story.getReferrers()));
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
						StoryContainer storyContainer = (StoryContainer) model
								.getBackingObject(row);
						String buttonLabel = null;
						if (isReadOnlyMode()) {
							buttonLabel = getResourceBundleHelper(getLocale()).getString(
									PROP_VIEW_STORY_CONTAINER_BUTTON_LABEL, "View");
						} else {
							buttonLabel = getResourceBundleHelper(getLocale()).getString(
									PROP_EDIT_STORY_CONTAINER_BUTTON_LABEL, "Edit");
						}
						NavigationEvent openEditorEvent = new OpenPanelEvent(this,
								PanelActionType.Editor, storyContainer, storyContainer.getClass(),
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
						StoryContainer StoryContainer = (StoryContainer) model
								.getBackingObject(row);
						return StoryContainer.getDescription();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Created By",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						StoryContainer StoryContainer = (StoryContainer) model
								.getBackingObject(row);
						return StoryContainer.getCreatedBy().getUsername();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Date Created",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						StoryContainer StoryContainer = (StoryContainer) model
								.getBackingObject(row);
						DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
						return formatter.format(StoryContainer.getDateCreated());
					}
				}));

		return tableConfig;
	}

	private static class StoryContainersTableManipulator extends AbstractComponentManipulator {

		protected StoryContainersTableManipulator() {
			super();
		}

		@Override
		public Object getModel(Component component) {
			return getValue(component, Story.class);
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
			return type.cast(getComponent(component).getStory());
		}

		@Override
		public void setValue(Component component, Object value) {
			getComponent(component).setStory((Story) value);
		}

		private StoryContainersTable getComponent(Component component) {
			return (StoryContainersTable) component;
		}
	}
}
