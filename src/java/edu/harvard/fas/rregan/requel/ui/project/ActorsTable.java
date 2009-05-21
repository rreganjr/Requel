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
import edu.harvard.fas.rregan.requel.project.Actor;
import edu.harvard.fas.rregan.requel.project.ActorContainer;
import edu.harvard.fas.rregan.requel.project.Project;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity;
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
 * A component to add to Panels of actor container entity editors to enable
 * editing of the actors of the entity.
 * 
 * @author ron
 */
public class ActorsTable extends AbstractRequelNavigatorTable {
	static final long serialVersionUID = 0L;

	static {
		ComponentManipulators.setManipulator(ActorsTable.class, new ActorsTableManipulator());
	}

	/**
	 * The name to use in the properties file of the panel that includes the
	 * ActorsTable to define the label of the actors field. If the property is
	 * undefined the panel should use a sensible default such as "Actors".
	 */
	public static final String PROP_LABEL_ACTORS = "Actors.Label";

	/**
	 * The name to use in the containing panels properties file to set the label
	 * of the view button in the actor edit table column. If the property is
	 * undefined "View" is used.
	 */
	public static final String PROP_VIEW_ACTOR_BUTTON_LABEL = "ViewActor.Label";

	/**
	 * The name to use in the containing panels properties file to set the label
	 * of the remove button in the actor edit table column. If the property is
	 * undefined "Remove" is used.
	 */
	public static final String PROP_REMOVE_ACTOR_BUTTON_LABEL = "RemoveActor.Label";

	/**
	 * The name to use in the containing panels properties file to set the label
	 * of the edit button in the actor edit table column. If the property is
	 * undefined "Edit" is used.
	 */
	public static final String PROP_EDIT_ACTOR_BUTTON_LABEL = "EditActor.Label";

	/**
	 * The name to use in the containing panels properties file to set the label
	 * of the new actor button under the actors table. If the property is
	 * undefined "New" is used.
	 */
	public static final String PROP_NEW_ACTOR_BUTTON_LABEL = "NewActor.Label";

	/**
	 * The name to use in the containing panels properties file to set the label
	 * of the find actor button under the actors table. If the property is
	 * undefined "Find" is used.
	 */
	public static final String PROP_FIND_ACTOR_BUTTON_LABEL = "FindActor.Label";

	private ActorContainer actorContainer;
	private final NavigatorTable table;
	private final NavigatorButton openActorEditorButton;
	private final NavigatorButton openActorSelectorButton;
	private final ProjectCommandFactory projectCommandFactory;
	private final CommandHandler commandHandler;
	private AddActorToActorContainerController addActorController;
	private RemoveActorFromActorContainerController removeActorController;

	/**
	 * @param editMode
	 * @param resourceBundleHelper
	 * @param projectCommandFactory -
	 *            passed to the add/remove actor to actor container controller
	 * @param commandHandler -
	 *            passed to the add/remove actor to actor container controller
	 */
	public ActorsTable(EditMode editMode, ResourceBundleHelper resourceBundleHelper,
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
				PROP_NEW_ACTOR_BUTTON_LABEL, "New");
		openActorEditorButton = new NavigatorButton(buttonLabel, getEventDispatcher());
		openActorEditorButton.setStyleName(Panel.STYLE_NAME_DEFAULT);
		openActorEditorButton.setVisible(false);
		buttons.add(openActorEditorButton);

		buttonLabel = getResourceBundleHelper(getLocale()).getString(PROP_FIND_ACTOR_BUTTON_LABEL,
				"Find");
		openActorSelectorButton = new NavigatorButton(buttonLabel, getEventDispatcher());
		openActorSelectorButton.setStyleName(Panel.STYLE_NAME_DEFAULT);
		openActorSelectorButton.setVisible(false);
		buttons.add(openActorSelectorButton);

		add(buttons);
	}

	protected ActorContainer getActorContainer() {
		return actorContainer;
	}

	protected void setActorContainer(ActorContainer actorContainer) {
		this.actorContainer = actorContainer;

		if (actorContainer != null) {
			table.setModel(new NavigatorTableModel((Collection) actorContainer.getActors()));
			if (!isReadOnlyMode()) {
				NavigationEvent openEditorEvent = new OpenPanelEvent(this, PanelActionType.Editor,
						getActorContainer(), Actor.class, null, WorkflowDisposition.NewFlow);
				openActorEditorButton.setEventToFire(openEditorEvent);
				openActorEditorButton.setVisible(true);

				ProjectOrDomain pod = null;
				if (getActorContainer() instanceof ProjectOrDomain) {
					pod = (ProjectOrDomain) getActorContainer();
				} else if (getActorContainer() instanceof ProjectOrDomainEntity) {
					pod = ((ProjectOrDomainEntity) getActorContainer()).getProjectOrDomain();
				}
				if (pod != null) {
					NavigationEvent openActorSelectorEvent = new OpenPanelEvent(this,
							PanelActionType.Selector, pod, Project.class,
							ProjectManagementPanelNames.PROJECT_ACTORS_SELECTOR_PANEL_NAME,
							WorkflowDisposition.ContinueFlow);

					openActorSelectorButton.setEventToFire(openActorSelectorEvent);
					openActorSelectorButton.setVisible(true);
				} else {
					openActorSelectorButton.setVisible(false);
				}

				// use the the story table (this) as the destination because it
				// is used as the source to the open panel events created above
				if (addActorController != null) {
					getEventDispatcher().removeEventTypeActionListener(SelectEntityEvent.class,
							addActorController, this);
				}
				addActorController = new AddActorToActorContainerController(getEventDispatcher(),
						projectCommandFactory, commandHandler, actorContainer);
				getEventDispatcher().addEventTypeActionListener(SelectEntityEvent.class,
						addActorController, this);

				// use the the story table (this) as the destination because it
				// is used as the source to the open panel events created above
				if (removeActorController != null) {
					getEventDispatcher().removeEventTypeActionListener(
							RemoveActorFromActorContainerEvent.class, removeActorController, this);
				}
				removeActorController = new RemoveActorFromActorContainerController(
						getEventDispatcher(), projectCommandFactory, commandHandler, actorContainer);
				getEventDispatcher().addEventTypeActionListener(
						RemoveActorFromActorContainerEvent.class, removeActorController, this);

			}
		} else {
			table.setModel(new NavigatorTableModel(Collections.EMPTY_SET));
			openActorEditorButton.setVisible(false);
			openActorSelectorButton.setVisible(false);
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

						Actor actor = (Actor) model.getBackingObject(row);
						String buttonLabel = null;
						if (isReadOnlyMode()) {
							buttonLabel = getResourceBundleHelper(getLocale()).getString(
									PROP_VIEW_ACTOR_BUTTON_LABEL, "View");
						} else {
							buttonLabel = getResourceBundleHelper(getLocale()).getString(
									PROP_EDIT_ACTOR_BUTTON_LABEL, "Edit");
						}
						NavigationEvent openEditorEvent = new OpenPanelEvent(ActorsTable.this,
								PanelActionType.Editor, actor, actor.getClass(), null,
								WorkflowDisposition.NewFlow);
						NavigatorButton openEditorButton = new NavigatorButton(buttonLabel,
								getEventDispatcher(), openEditorEvent);
						openEditorButton.setStyleName(Panel.STYLE_NAME_PLAIN);
						openEditorButton.setLayoutData(buttonLayout);
						buttonsContainer.add(openEditorButton);

						if (!isReadOnlyMode()) {
							buttonLabel = getResourceBundleHelper(getLocale()).getString(
									PROP_REMOVE_ACTOR_BUTTON_LABEL, "Remove");
							NavigationEvent removeActorEvent = new RemoveActorFromActorContainerEvent(
									ActorsTable.this, actor, actorContainer, ActorsTable.this);
							NavigatorButton removeActorButton = new NavigatorButton(buttonLabel,
									getEventDispatcher(), removeActorEvent);
							openEditorButton.setStyleName(Panel.STYLE_NAME_PLAIN);
							openEditorButton.setLayoutData(buttonLayout);
							buttonsContainer.add(removeActorButton);
						}
						return buttonsContainer;
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Name",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Actor actor = (Actor) model.getBackingObject(row);
						return actor.getName();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Created By",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Actor actor = (Actor) model.getBackingObject(row);
						return actor.getCreatedBy().getUsername();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Date Created",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Actor actor = (Actor) model.getBackingObject(row);
						DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
						return formatter.format(actor.getDateCreated());
					}
				}));

		return tableConfig;
	}

	private static class ActorsTableManipulator extends AbstractComponentManipulator {

		protected ActorsTableManipulator() {
			super();
		}

		@Override
		public Object getModel(Component component) {
			return getValue(component, ActorContainer.class);
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
			return type.cast(getComponent(component).getActorContainer());
		}

		@Override
		public void setValue(Component component, Object value) {
			getComponent(component).setActorContainer((ActorContainer) value);
		}

		private ActorsTable getComponent(Component component) {
			return (ActorsTable) component;
		}
	}
}
