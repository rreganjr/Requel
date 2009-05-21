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

import nextapp.echo2.app.Alignment;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.requel.annotation.Annotatable;
import edu.harvard.fas.rregan.requel.annotation.Annotation;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity;
import edu.harvard.fas.rregan.requel.project.Scenario;
import edu.harvard.fas.rregan.requel.project.Project;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.Stakeholder;
import edu.harvard.fas.rregan.requel.project.StakeholderPermissionType;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.uiframework.navigation.NavigatorButton;
import edu.harvard.fas.rregan.uiframework.navigation.event.ClosePanelEvent;
import edu.harvard.fas.rregan.uiframework.navigation.event.DeletedEntityEvent;
import edu.harvard.fas.rregan.uiframework.navigation.event.NavigationEvent;
import edu.harvard.fas.rregan.uiframework.navigation.event.SelectEntityEvent;
import edu.harvard.fas.rregan.uiframework.navigation.event.UpdateEntityEvent;
import edu.harvard.fas.rregan.uiframework.navigation.table.NavigatorTableCellValueFactory;
import edu.harvard.fas.rregan.uiframework.navigation.table.NavigatorTableColumnConfig;
import edu.harvard.fas.rregan.uiframework.navigation.table.NavigatorTableConfig;
import edu.harvard.fas.rregan.uiframework.navigation.table.NavigatorTableModel;
import edu.harvard.fas.rregan.uiframework.panel.NavigatorTableModelAdapter;
import edu.harvard.fas.rregan.uiframework.panel.SelectorTablePanel;

/**
 * @author ron
 */
public class ScenarioSelectorPanel extends SelectorTablePanel {
	private static final Logger log = Logger.getLogger(ScenarioSelectorPanel.class);
	static final long serialVersionUID = 0;

	private final ProjectRepository projectRepository;
	private UpdateListener updateListener;

	/**
	 * Property name to use in the ScenarioNavigatorPanel.properties to set the
	 * label for the text of the cancel/reset button.
	 */
	public static final String PROP_CANCEL_BUTTON_LABEL = "CancelButton.Label";

	/**
	 * @param projectRepository
	 */
	public ScenarioSelectorPanel(ProjectRepository projectRepository) {
		super(ScenarioSelectorPanel.class.getName(), Project.class,
				ProjectManagementPanelNames.PROJECT_SCENARIO_SELECTOR_PANEL_NAME);
		this.projectRepository = projectRepository;

		NavigatorTableConfig tableConfig = new NavigatorTableConfig();

		tableConfig.setRowLevelSelection(true);

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Name",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Scenario scenario = (Scenario) model.getBackingObject(row);
						return scenario.getName();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Top Level",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Scenario scenario = (Scenario) model.getBackingObject(row);
						return (scenario.getUsingScenarios().size() == 0) ? "Yes" : "No";
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Type",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Scenario scenario = (Scenario) model.getBackingObject(row);
						return scenario.getType().toString();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Created By",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Scenario scenario = (Scenario) model.getBackingObject(row);
						return scenario.getCreatedBy().getUsername();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Date Created",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Scenario scenario = (Scenario) model.getBackingObject(row);
						DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
						return format.format(scenario.getDateCreated());
					}
				}));

		setTableConfig(tableConfig);
	}

	/**
	 * Create a title for panel with dynamic information from the project or
	 * domain, by default the title is "Select Scenario"<br>
	 * 
	 * @see Panel.PROP_PANEL_TITLE
	 * @see edu.harvard.fas.rregan.uiframework.panel.AbstractPanel#getTitle()
	 */
	@Override
	public String getTitle() {
		return getResourceBundleHelper(getLocale()).getString(PROP_PANEL_TITLE, "Select Scenario");
	}

	@Override
	public void dispose() {
		super.dispose();
		removeAll();
		if (updateListener != null) {
			getEventDispatcher().removeEventTypeActionListener(UpdateEntityEvent.class,
					updateListener);
			updateListener = null;
		}
	}

	@Override
	public void setup() {
		super.setup();

		Row buttonsWrapper = new Row();
		buttonsWrapper.setInsets(new Insets(10, 5));
		buttonsWrapper.setAlignment(new Alignment(Alignment.CENTER, Alignment.DEFAULT));

		String closeButtonLabel = getResourceBundleHelper(getLocale()).getString(
				PROP_CANCEL_BUTTON_LABEL, "Close");
		NavigationEvent closeEvent = new ClosePanelEvent(this, this);
		NavigatorButton closeButton = new NavigatorButton(closeButtonLabel, getEventDispatcher(),
				closeEvent);
		closeButton.setStyleName(STYLE_NAME_DEFAULT);
		buttonsWrapper.add(closeButton);
		add(buttonsWrapper);

		if (updateListener != null) {
			getEventDispatcher().removeEventTypeActionListener(UpdateEntityEvent.class,
					updateListener);
		}
		updateListener = new UpdateListener(this);
		getEventDispatcher().addEventTypeActionListener(UpdateEntityEvent.class, updateListener);
	}

	protected boolean isReadOnlyMode() {
		User user = (User) getApp().getUser();
		if (getProjectOrDomain() instanceof Project) {
			Project project = (Project) getProjectOrDomain();
			Stakeholder stakeholder = project.getUserStakeholder(user);
			if (stakeholder != null) {
				return !stakeholder.hasPermission(Scenario.class, StakeholderPermissionType.Edit);
			}
		}
		return true;
	}

	/**
	 * This method should be overridden to return a collection when the target
	 * of the panel is not a collection.
	 * 
	 * @return an adapter to get the collection of items to select from from the
	 *         target object.
	 */
	@Override
	protected NavigatorTableModelAdapter getTargetNavigatorTableModelAdapter() {
		return new NavigatorTableModelAdapter() {
			private ProjectOrDomain targetObject;

			@Override
			public Collection<Object> getCollection() {
				return (Collection) targetObject.getScenarios();
			}

			@Override
			public void setTargetObject(Object targetObject) {
				this.targetObject = (ProjectOrDomain) targetObject;
			}
		};
	}

	protected ProjectOrDomain getProjectOrDomain() {
		return (ProjectOrDomain) getTargetObject();
	}

	protected ProjectRepository getProjectRepository() {
		return projectRepository;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// before returning, initialize the Scenario
		SelectEntityEvent selectEvent = new SelectEntityEvent(this, getTable().getSelectedObject(),
				getDestinationObject());
		getEventDispatcher().dispatchEvent(selectEvent);
	}

	private static class UpdateListener implements ActionListener {
		static final long serialVersionUID = 0L;

		private final ScenarioSelectorPanel panel;

		private UpdateListener(ScenarioSelectorPanel panel) {
			this.panel = panel;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e instanceof UpdateEntityEvent) {
				UpdateEntityEvent event = (UpdateEntityEvent) e;
				ProjectOrDomain updatedPod = null;
				if (event.getObject() instanceof ProjectOrDomain) {
					updatedPod = (ProjectOrDomain) event.getObject();
				} else if (event.getObject() instanceof ProjectOrDomainEntity) {
					ProjectOrDomainEntity updatedEntity = (ProjectOrDomainEntity) event.getObject();
					updatedPod = updatedEntity.getProjectOrDomain();
				} else if (event.getObject() instanceof Annotation) {
					if (!(event instanceof DeletedEntityEvent)) {
						Annotation updatedAnnotation = (Annotation) event.getObject();
						for (Annotatable annotatable : updatedAnnotation.getAnnotatables()) {
							if ((annotatable instanceof ProjectOrDomain)
									&& annotatable.equals(panel.getProjectOrDomain())) {
								updatedPod = (ProjectOrDomain) annotatable;
								break;
							} else if ((annotatable instanceof ProjectOrDomainEntity)) {
								ProjectOrDomainEntity entity = (ProjectOrDomainEntity) annotatable;
								if (entity.getProjectOrDomain().equals(panel.getProjectOrDomain())) {
									updatedPod = entity.getProjectOrDomain();
									break;
								}
							}
						}
					}
				}
				if (panel.getProjectOrDomain().equals(updatedPod)) {
					panel.setTargetObject(updatedPod);
				}
			}
		}
	}
}
