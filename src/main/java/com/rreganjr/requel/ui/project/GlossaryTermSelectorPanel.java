/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * 
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

import nextapp.echo2.app.Alignment;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;

import org.apache.log4j.Logger;

import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.StakeholderPermissionType;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.user.User;
import net.sf.echopm.navigation.NavigatorButton;
import net.sf.echopm.navigation.event.ClosePanelEvent;
import net.sf.echopm.navigation.event.DeletedEntityEvent;
import net.sf.echopm.navigation.event.NavigationEvent;
import net.sf.echopm.navigation.event.SelectEntityEvent;
import net.sf.echopm.navigation.event.UpdateEntityEvent;
import net.sf.echopm.navigation.table.NavigatorTableCellValueFactory;
import net.sf.echopm.navigation.table.NavigatorTableColumnConfig;
import net.sf.echopm.navigation.table.NavigatorTableConfig;
import net.sf.echopm.navigation.table.NavigatorTableModel;
import net.sf.echopm.panel.NavigatorTableModelAdapter;
import net.sf.echopm.panel.SelectorTablePanel;

/**
 * @author ron
 */
public class GlossaryTermSelectorPanel extends SelectorTablePanel {
	private static final Logger log = Logger.getLogger(GlossaryTermSelectorPanel.class);
	static final long serialVersionUID = 0;

	private final ProjectRepository projectRepository;
	private UpdateListener updateListener;

	/**
	 * @param projectRepository
	 */
	public GlossaryTermSelectorPanel(ProjectRepository projectRepository) {
		super(GlossaryTermSelectorPanel.class.getName(), Project.class,
				ProjectManagementPanelNames.PROJECT_GLOSSARY_TERM_SELECTOR_PANEL_NAME);
		this.projectRepository = projectRepository;

		NavigatorTableConfig tableConfig = new NavigatorTableConfig();

		tableConfig.setRowLevelSelection(true);

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Term",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						GlossaryTerm glossaryTerm = (GlossaryTerm) model.getBackingObject(row);
						return glossaryTerm.getName();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Definition",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						GlossaryTerm glossaryTerm = (GlossaryTerm) model.getBackingObject(row);
						return glossaryTerm.getText();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Created By",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						GlossaryTerm glossaryTerm = (GlossaryTerm) model.getBackingObject(row);
						return glossaryTerm.getCreatedBy().getUsername();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Date Created",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						GlossaryTerm glossaryTerm = (GlossaryTerm) model.getBackingObject(row);
						DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
						return format.format(glossaryTerm.getDateCreated());
					}
				}));

		setTableConfig(tableConfig);
	}

	/**
	 * Create a title for panel with dynamic information from the project or
	 * domain, by default the title is "Select Term"<br>
	 * 
	 * @see Panel.PROP_PANEL_TITLE
	 * @see net.sf.echopm.panel.AbstractPanel#getTitle()
	 */
	@Override
	public String getTitle() {
		return getResourceBundleHelper(getLocale()).getString(PROP_PANEL_TITLE, "Select Term");
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
			UserStakeholder stakeholder = project.getUserStakeholder(user);
			if (stakeholder != null) {
				return !stakeholder.hasPermission(GlossaryTerm.class,
						StakeholderPermissionType.Edit);
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
				return (Collection) targetObject.getGlossaryTerms();
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
		// before returning, initialize the goal
		SelectEntityEvent selectEvent = new SelectEntityEvent(this, getTable().getSelectedObject(),
				getDestinationObject());
		getEventDispatcher().dispatchEvent(selectEvent);
	}

	private static class UpdateListener implements ActionListener {
		static final long serialVersionUID = 0L;

		private final GlossaryTermSelectorPanel panel;

		private UpdateListener(GlossaryTermSelectorPanel panel) {
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
