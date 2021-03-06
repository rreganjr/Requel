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
import java.text.MessageFormat;
import java.text.SimpleDateFormat;

import nextapp.echo2.app.Alignment;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.app.layout.RowLayoutData;

import org.apache.log4j.Logger;

import com.rreganjr.requel.project.NonUserStakeholder;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.project.StakeholderPermissionType;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.project.impl.AbstractProjectOrDomainEntity;
import com.rreganjr.requel.user.User;
import net.sf.echopm.navigation.NavigatorButton;
import net.sf.echopm.navigation.WorkflowDisposition;
import net.sf.echopm.navigation.event.ClosePanelEvent;
import net.sf.echopm.navigation.event.NavigationEvent;
import net.sf.echopm.navigation.event.OpenPanelEvent;
import net.sf.echopm.navigation.event.UpdateEntityEvent;
import net.sf.echopm.navigation.table.NavigatorTableCellValueFactory;
import net.sf.echopm.navigation.table.NavigatorTableColumnConfig;
import net.sf.echopm.navigation.table.NavigatorTableConfig;
import net.sf.echopm.navigation.table.NavigatorTableModel;
import net.sf.echopm.panel.NavigatorTablePanel;
import net.sf.echopm.panel.PanelActionType;

/**
 * @author ron
 */
public class StakeholderNavigatorPanel extends NavigatorTablePanel {
	private static final Logger log = Logger.getLogger(StakeholderNavigatorPanel.class);
	static final long serialVersionUID = 0;

	private StakeholderUpdateListener updateListener;
	private ProjectOrDomain pod;

	/**
	 * Property name to use in the StakeholderNavigatorPanel.properties to set
	 * the label on the new non-user stakeholder button.
	 */
	public static final String PROP_NEW_NON_USER_STAKEHOLDER_BUTTON_LABEL = "NewNonUserStakeholderButton.Label";

	/**
	 * Property name to use in the StakeholderNavigatorPanel.properties to set
	 * the label on the new user stakeholder button.
	 */
	public static final String PROP_NEW_USER_STAKEHOLDER_BUTTON_LABEL = "NewUserStakeholderButton.Label";

	/**
	 * Property name to use in the StakeholderNavigatorPanel.properties to set
	 * the label on the edit stakeholder button in each row of the table.
	 */
	public static final String PROP_EDIT_STAKEHOLDER_BUTTON_LABEL = "EditStakeholderButton.Label";

	/**
	 * Property name to use in the StakeholderNavigatorPanel.properties to set
	 * the label on the view goal button in each row of the table when the user
	 * doesn't have edit permission.
	 */
	public static final String PROP_VIEW_STAKEHOLDER_BUTTON_LABEL = "ViewStakeholderButton.Label";

	/**
	 */
	public StakeholderNavigatorPanel() {
		super(StakeholderNavigatorPanel.class.getName(), Project.class,
				ProjectManagementPanelNames.PROJECT_STAKEHOLDERS_NAVIGATOR_PANEL_NAME);
		NavigatorTableConfig tableConfig = new NavigatorTableConfig();

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Stakeholder stakeholder = (Stakeholder) model.getBackingObject(row);
						String editStakeholderButtonLabel = null;
						if (isReadOnlyMode()) {
							editStakeholderButtonLabel = getResourceBundleHelper(getLocale())
									.getString(PROP_VIEW_STAKEHOLDER_BUTTON_LABEL, "View");
						} else {
							editStakeholderButtonLabel = getResourceBundleHelper(getLocale())
									.getString(PROP_EDIT_STAKEHOLDER_BUTTON_LABEL, "Edit");
						}

						NavigationEvent openStakeholderEditorEvent = null;
						if (stakeholder.isUserStakeholder()) {
							openStakeholderEditorEvent = new OpenPanelEvent(this,
									PanelActionType.Editor, stakeholder, UserStakeholder.class, null,
									WorkflowDisposition.NewFlow);
						} else {
							openStakeholderEditorEvent = new OpenPanelEvent(this,
									PanelActionType.Editor, stakeholder, NonUserStakeholder.class, null,
									WorkflowDisposition.NewFlow);
						}
						NavigatorButton editStakeholderButton = new NavigatorButton(
								editStakeholderButtonLabel, getEventDispatcher(),
								openStakeholderEditorEvent);
						editStakeholderButton.setStyleName(STYLE_NAME_PLAIN);
						RowLayoutData rld = new RowLayoutData();
						rld.setAlignment(Alignment.ALIGN_CENTER);
						editStakeholderButton.setLayoutData(rld);
						return editStakeholderButton;
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Name",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Stakeholder stakeholder = (Stakeholder) model.getBackingObject(row);
						String displayValue = null;
						if (stakeholder.isUserStakeholder()) {
							User user = ((UserStakeholder) stakeholder).getUser();
							if ((user.getName() != null) && (user.getName().length() > 0)) {
								displayValue = user.getName() + " [ " + user.getUsername() + " ]";
							} else {
								displayValue = user.getUsername();
							}
						} else {
							displayValue = stakeholder.getName();
						}
						return displayValue;
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("User?",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Stakeholder stakeholder = (Stakeholder) model.getBackingObject(row);
						return (stakeholder.isUserStakeholder() ? "yes" : "no");
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Team",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Stakeholder stakeholder = (Stakeholder) model.getBackingObject(row);
						if (stakeholder.isUserStakeholder()) {
							return (((UserStakeholder) stakeholder).getTeam() != null ? ((UserStakeholder) stakeholder)
									.getTeam().getName()
									: "");
						}
						return "";
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Email Address",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Stakeholder stakeholder = (Stakeholder) model.getBackingObject(row);
						String displayValue = null;
						if (stakeholder.isUserStakeholder()) {
							User user = ((UserStakeholder) stakeholder).getUser();
							displayValue = user.getEmailAddress();
						} else {
							displayValue = "";
						}
						return displayValue;
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Phone Number",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Stakeholder stakeholder = (Stakeholder) model.getBackingObject(row);
						String displayValue = null;
						if (stakeholder.isUserStakeholder()) {
							User user = ((UserStakeholder) stakeholder).getUser();
							displayValue = user.getPhoneNumber();
						} else {
							displayValue = "";
						}
						return displayValue;
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Created By",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						AbstractProjectOrDomainEntity entity = (AbstractProjectOrDomainEntity) model
								.getBackingObject(row);
						return entity.getCreatedBy().getUsername();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Date Created",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						AbstractProjectOrDomainEntity entity = (AbstractProjectOrDomainEntity) model
								.getBackingObject(row);
						DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
						return format.format(entity.getDateCreated());
					}
				}));
		setTableConfig(tableConfig);
	}

	/**
	 * Create a title for panel with dynamic information from the project or
	 * domain, by default the pattern is "Stakeholders: {0}"<br>
	 * Valid variables are:<br>
	 * {0} - project/domain name<br>
	 * 
	 * @see Panel.PROP_PANEL_TITLE
	 * @see net.sf.echopm.panel.AbstractPanel#getTitle()
	 */
	@Override
	public String getTitle() {
		String name = "";
		String msgPattern = getResourceBundleHelper(getLocale()).getString(PROP_PANEL_TITLE,
				"Stakeholders: {0}");
		ProjectOrDomain pod = getProjectOrDomain();
		if (pod != null) {
			name = pod.getName();
		}
		return MessageFormat.format(msgPattern, name);
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

		if (!isReadOnlyMode()) {
			// add non-user stakeholder button
			String newStakeholderButtonLabel = getResourceBundleHelper(getLocale()).getString(
					PROP_NEW_NON_USER_STAKEHOLDER_BUTTON_LABEL, "Add Non-User");
			NavigationEvent openStakeholderEditor = new OpenPanelEvent(this,
					PanelActionType.Editor, getProjectOrDomain(), NonUserStakeholder.class, null,
					WorkflowDisposition.NewFlow);
			NavigatorButton newStakeholderButton = new NavigatorButton(newStakeholderButtonLabel,
					getEventDispatcher(), openStakeholderEditor);
			newStakeholderButton.setStyleName(STYLE_NAME_DEFAULT);
			buttonsWrapper.add(newStakeholderButton);
			
			// add user stakeholder button
			newStakeholderButtonLabel = getResourceBundleHelper(getLocale()).getString(
					PROP_NEW_USER_STAKEHOLDER_BUTTON_LABEL, "Add User");
			openStakeholderEditor = new OpenPanelEvent(this,
					PanelActionType.Editor, getProjectOrDomain(), UserStakeholder.class, null,
					WorkflowDisposition.NewFlow);
			newStakeholderButton = new NavigatorButton(newStakeholderButtonLabel,
					getEventDispatcher(), openStakeholderEditor);
			newStakeholderButton.setStyleName(STYLE_NAME_DEFAULT);
			buttonsWrapper.add(newStakeholderButton);
		}

		add(buttonsWrapper);

		if (updateListener != null) {
			getEventDispatcher().removeEventTypeActionListener(UpdateEntityEvent.class,
					updateListener);
		}
		updateListener = new StakeholderUpdateListener(this);
		getEventDispatcher().addEventTypeActionListener(UpdateEntityEvent.class, updateListener);
	}

	protected boolean isReadOnlyMode() {
		User user = (User) getApp().getUser();
		if (getProjectOrDomain() instanceof Project) {
			Project project = (Project) getProjectOrDomain();
			UserStakeholder stakeholder = project.getUserStakeholder(user);
			if (stakeholder != null) {
				return !stakeholder
						.hasPermission(Stakeholder.class, StakeholderPermissionType.Edit);
			}
		}
		return true;
	}

	@Override
	public void setTargetObject(Object targetObject) {
		if (targetObject instanceof ProjectOrDomain) {
			pod = (ProjectOrDomain) targetObject;
			super.setTargetObject(((ProjectOrDomain) targetObject).getStakeholders());
		} else {
			log.error("unexpected target object " + targetObject);
		}
	}

	protected ProjectOrDomain getProjectOrDomain() {
		return pod;
	}

	private static class StakeholderUpdateListener implements ActionListener {
		static final long serialVersionUID = 0L;

		private final StakeholderNavigatorPanel panel;

		private StakeholderUpdateListener(StakeholderNavigatorPanel panel) {
			this.panel = panel;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e instanceof UpdateEntityEvent) {
				UpdateEntityEvent event = (UpdateEntityEvent) e;
				if (event.getObject() instanceof ProjectOrDomain) {
					ProjectOrDomain updatedPod = (ProjectOrDomain) event.getObject();
					if (panel.getProjectOrDomain().equals(updatedPod)) {
						panel.setTargetObject(updatedPod);
					}
				} else if (event.getObject() instanceof Stakeholder) {
					Stakeholder updatedStakeholder = (Stakeholder) event.getObject();
					ProjectOrDomain updatedPod = updatedStakeholder.getProjectOrDomain();
					if (panel.getProjectOrDomain().equals(updatedPod)) {
						panel.setTargetObject(updatedPod);
					}
				}
			}
		}
	}
}
