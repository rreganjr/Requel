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
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.layout.ColumnLayoutData;
import nextapp.echo2.app.layout.RowLayoutData;
import edu.harvard.fas.rregan.ResourceBundleHelper;
import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.project.Project;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity;
import edu.harvard.fas.rregan.requel.project.Story;
import edu.harvard.fas.rregan.requel.project.StoryContainer;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.ui.AbstractRequelNavigatorTable;
import edu.harvard.fas.rregan.uiframework.navigation.NavigatorButton;
import edu.harvard.fas.rregan.uiframework.navigation.WorkflowDisposition;
import edu.harvard.fas.rregan.uiframework.navigation.event.NavigationEvent;
import edu.harvard.fas.rregan.uiframework.navigation.event.OpenPanelEvent;
import edu.harvard.fas.rregan.uiframework.navigation.event.SelectEntityEvent;
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
 * A component to add to Panels of story container entity editors to enable
 * editing of the Stories of the entity.
 * 
 * @author ron
 */
public class StoriesTable extends AbstractRequelNavigatorTable {
	static final long serialVersionUID = 0L;

	static {
		ComponentManipulators.setManipulator(StoriesTable.class, new StoriesTableManipulator());
	}

	/**
	 * The name to use in the properties file of the panel that includes the
	 * StoriesTable to define the label of the Stories field. If the property is
	 * undefined the panel should use a sensible default such as "Stories".
	 */
	public static final String PROP_LABEL_STORIES = "Stories.Label";

	/**
	 * The name to use in the containing panels properties file to set the label
	 * of the view button in the story edit table column. If the property is
	 * undefined "View" is used.
	 */
	public static final String PROP_VIEW_STORY_BUTTON_LABEL = "ViewStory.Label";

	/**
	 * The name to use in the containing panels properties file to set the label
	 * of the remove button in the story edit table column. If the property is
	 * undefined "Remove" is used.
	 */
	public static final String PROP_REMOVE_STORY_BUTTON_LABEL = "RemoveStory.Label";

	/**
	 * The name to use in the containing panels properties file to set the label
	 * of the edit button in the story edit table column. If the property is
	 * undefined "Edit" is used.
	 */
	public static final String PROP_EDIT_STORY_BUTTON_LABEL = "EditStory.Label";

	/**
	 * The name to use in the containing panels properties file to set the label
	 * of the new story button under the Stories table. If the property is
	 * undefined "New" is used.
	 */
	public static final String PROP_NEW_STORY_BUTTON_LABEL = "NewStory.Label";

	/**
	 * The name to use in the containing panels properties file to set the label
	 * of the find story button under the Stories table. If the property is
	 * undefined "Find" is used.
	 */
	public static final String PROP_FIND_STORY_BUTTON_LABEL = "FindStory.Label";

	private StoryContainer storyContainer;
	private final NavigatorTable table;
	private final NavigatorButton openStoryEditorButton;
	private final NavigatorButton openStoriesSelectorButton;
	private final ProjectCommandFactory projectCommandFactory;
	private final CommandHandler commandHandler;
	private AddStoryToStoryContainerController addStoryController;
	private RemoveStoryFromStoryContainerController removeStoryController;

	/**
	 * @param editMode
	 * @param resourceBundleHelper
	 * @param projectCommandFactory -
	 *            passed to the add/remove story to story container controller
	 * @param commandHandler -
	 *            passed to the add/remove story to story container controller
	 */
	public StoriesTable(EditMode editMode, ResourceBundleHelper resourceBundleHelper,
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
				PROP_NEW_STORY_BUTTON_LABEL, "New");
		openStoryEditorButton = new NavigatorButton(buttonLabel, getEventDispatcher());
		openStoryEditorButton.setStyleName(Panel.STYLE_NAME_DEFAULT);
		openStoryEditorButton.setVisible(false);
		buttons.add(openStoryEditorButton);

		buttonLabel = getResourceBundleHelper(getLocale()).getString(PROP_FIND_STORY_BUTTON_LABEL,
				"Find");
		openStoriesSelectorButton = new NavigatorButton(buttonLabel, getEventDispatcher());
		openStoriesSelectorButton.setStyleName(Panel.STYLE_NAME_DEFAULT);
		openStoriesSelectorButton.setVisible(false);
		buttons.add(openStoriesSelectorButton);

		add(buttons);
	}

	protected StoryContainer getStoryContainer() {
		return storyContainer;
	}

	protected void setStoryContainer(StoryContainer storyContainer) {
		this.storyContainer = storyContainer;

		if (storyContainer != null) {
			table.setModel(new NavigatorTableModel((Collection) storyContainer.getStories()));
			if (!isReadOnlyMode()) {
				NavigationEvent openEditorEvent = new OpenPanelEvent(this, PanelActionType.Editor,
						getStoryContainer(), Story.class, null, WorkflowDisposition.NewFlow);
				openStoryEditorButton.setEventToFire(openEditorEvent);
				openStoryEditorButton.setVisible(true);

				ProjectOrDomain pod = null;
				if (getStoryContainer() instanceof ProjectOrDomain) {
					pod = (ProjectOrDomain) getStoryContainer();
				} else if (getStoryContainer() instanceof ProjectOrDomainEntity) {
					pod = ((ProjectOrDomainEntity) getStoryContainer()).getProjectOrDomain();
				}
				if (pod != null) {
					NavigationEvent openStoriesSelectorEvent = new OpenPanelEvent(this,
							PanelActionType.Selector, pod, Project.class,
							ProjectManagementPanelNames.PROJECT_STORY_SELECTOR_PANEL_NAME,
							WorkflowDisposition.ContinueFlow);

					openStoriesSelectorButton.setEventToFire(openStoriesSelectorEvent);
					openStoriesSelectorButton.setVisible(true);
				} else {
					openStoriesSelectorButton.setVisible(false);
				}

				// use the the story table (this) as the destination because it
				// is used as the source to the open panel events created above
				if (addStoryController != null) {
					getEventDispatcher().removeEventTypeActionListener(SelectEntityEvent.class,
							addStoryController, this);
				}
				addStoryController = new AddStoryToStoryContainerController(getEventDispatcher(),
						projectCommandFactory, commandHandler, storyContainer);
				getEventDispatcher().addEventTypeActionListener(SelectEntityEvent.class,
						addStoryController, this);

				// use the the story table (this) as the destination because it
				// is used as the source to the open panel events created above
				if (removeStoryController != null) {
					getEventDispatcher().removeEventTypeActionListener(
							RemoveStoryFromStoryContainerEvent.class, removeStoryController, this);
				}
				removeStoryController = new RemoveStoryFromStoryContainerController(
						getEventDispatcher(), projectCommandFactory, commandHandler, storyContainer);
				getEventDispatcher().addEventTypeActionListener(
						RemoveStoryFromStoryContainerEvent.class, removeStoryController, this);

			}
		} else {
			table.setModel(new NavigatorTableModel(Collections.EMPTY_SET));
			openStoryEditorButton.setVisible(false);
			openStoriesSelectorButton.setVisible(false);
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

						Story story = (Story) model.getBackingObject(row);
						String buttonLabel = null;
						if (isReadOnlyMode()) {
							buttonLabel = getResourceBundleHelper(getLocale()).getString(
									PROP_VIEW_STORY_BUTTON_LABEL, "View");
						} else {
							buttonLabel = getResourceBundleHelper(getLocale()).getString(
									PROP_EDIT_STORY_BUTTON_LABEL, "Edit");
						}
						NavigationEvent openEditorEvent = new OpenPanelEvent(StoriesTable.this,
								PanelActionType.Editor, story, story.getClass(), null,
								WorkflowDisposition.NewFlow);
						NavigatorButton openEditorButton = new NavigatorButton(buttonLabel,
								getEventDispatcher(), openEditorEvent);
						openEditorButton.setStyleName(Panel.STYLE_NAME_PLAIN);
						openEditorButton.setLayoutData(buttonLayout);
						buttonsContainer.add(openEditorButton);

						if (!isReadOnlyMode()) {
							buttonLabel = getResourceBundleHelper(getLocale()).getString(
									PROP_REMOVE_STORY_BUTTON_LABEL, "Remove");
							NavigationEvent removeStoryEvent = new RemoveStoryFromStoryContainerEvent(
									StoriesTable.this, story, storyContainer, StoriesTable.this);
							NavigatorButton removeStoryButton = new NavigatorButton(buttonLabel,
									getEventDispatcher(), removeStoryEvent);
							openEditorButton.setStyleName(Panel.STYLE_NAME_PLAIN);
							openEditorButton.setLayoutData(buttonLayout);
							buttonsContainer.add(removeStoryButton);
						}
						return buttonsContainer;
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Name",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Story story = (Story) model.getBackingObject(row);
						return story.getName();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Created By",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Story story = (Story) model.getBackingObject(row);
						return story.getCreatedBy().getUsername();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Date Created",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Story story = (Story) model.getBackingObject(row);
						DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
						return formatter.format(story.getDateCreated());
					}
				}));

		return tableConfig;
	}

	private static class StoriesTableManipulator extends AbstractComponentManipulator {

		protected StoriesTableManipulator() {
			super();
		}

		@Override
		public Object getModel(Component component) {
			return getValue(component, StoryContainer.class);
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
			return type.cast(getComponent(component).getStoryContainer());
		}

		@Override
		public void setValue(Component component, Object value) {
			getComponent(component).setStoryContainer((StoryContainer) value);
		}

		private StoriesTable getComponent(Component component) {
			return (StoriesTable) component;
		}
	}
}
