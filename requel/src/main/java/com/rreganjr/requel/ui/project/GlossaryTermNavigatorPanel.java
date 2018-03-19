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
import java.text.MessageFormat;
import java.text.SimpleDateFormat;

import nextapp.echo2.app.Alignment;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.app.layout.RowLayoutData;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.StakeholderPermissionType;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.project.impl.AbstractProjectOrDomainEntity;
import com.rreganjr.requel.user.User;
import net.sf.echopm.navigation.NavigatorButton;
import net.sf.echopm.navigation.WorkflowDisposition;
import net.sf.echopm.navigation.event.ClosePanelEvent;
import net.sf.echopm.navigation.event.DeletedEntityEvent;
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
 * Panel for viewing and editing glossary terms for a project or domain.
 * 
 * @author ron
 */
public class GlossaryTermNavigatorPanel extends NavigatorTablePanel {
	private static final Log log = LogFactory.getLog(GlossaryTermNavigatorPanel.class);
	static final long serialVersionUID = 0;

	private GlossaryTermUpdateListener updateListener;
	private ProjectOrDomain pod;

	/**
	 * Property name to use in the GlossaryTermNavigatorPanel.properties to set
	 * the label on the new GlossaryTerm button.
	 */
	public static final String PROP_NEW_GLOSSARY_TERM_BUTTON_LABEL = "NewGlossaryTermButton.Label";

	/**
	 * Property name to use in the GlossaryTermNavigatorPanel.properties to set
	 * the label on the edit GlossaryTerm button in each row of the table.
	 */
	public static final String PROP_EDIT_GLOSSARY_TERM_BUTTON_LABEL = "EditGlossaryTermButton.Label";

	/**
	 * Property name to use in the GlossaryTermNavigatorPanel.properties to set
	 * the label on the view goal button in each row of the table when the user
	 * doesn't have edit permission.
	 */
	public static final String PROP_VIEW_GLOSSARY_TERM_BUTTON_LABEL = "ViewGlossaryTermButton.Label";

	/**
	 */
	public GlossaryTermNavigatorPanel() {
		super(GlossaryTermNavigatorPanel.class.getName(), Project.class,
				ProjectManagementPanelNames.PROJECT_GLOSSARY_TERMS_NAVIGATOR_PANEL_NAME);
		NavigatorTableConfig tableConfig = new NavigatorTableConfig();

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						GlossaryTerm glossaryTerm = (GlossaryTerm) model.getBackingObject(row);
						String editGlossaryTermButtonLabel = null;
						if (isReadOnlyMode()) {
							editGlossaryTermButtonLabel = getResourceBundleHelper(getLocale())
									.getString(PROP_VIEW_GLOSSARY_TERM_BUTTON_LABEL, "View");
						} else {
							editGlossaryTermButtonLabel = getResourceBundleHelper(getLocale())
									.getString(PROP_EDIT_GLOSSARY_TERM_BUTTON_LABEL, "Edit");
						}

						NavigationEvent openGlossaryTermEditor = new OpenPanelEvent(this,
								PanelActionType.Editor, glossaryTerm, GlossaryTerm.class, null,
								WorkflowDisposition.NewFlow);
						NavigatorButton editGlossaryTermButton = new NavigatorButton(
								editGlossaryTermButtonLabel, getEventDispatcher(),
								openGlossaryTermEditor);
						editGlossaryTermButton.setStyleName(STYLE_NAME_PLAIN);
						RowLayoutData rld = new RowLayoutData();
						rld.setAlignment(Alignment.ALIGN_CENTER);
						editGlossaryTermButton.setLayoutData(rld);
						return editGlossaryTermButton;
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Name",
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

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Canonical Term",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						GlossaryTerm glossaryTerm = (GlossaryTerm) model.getBackingObject(row);
						return glossaryTerm.getCanonicalTerm() == null ? "" : glossaryTerm
								.getCanonicalTerm().getName();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Created By",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						AbstractProjectOrDomainEntity entity = (AbstractProjectOrDomainEntity) model
								.getBackingObject(row);
						return entity.getCreatedBy() == null ? "" : entity.getCreatedBy()
								.getUsername();
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
	 * domain, by default the pattern is "GlossaryTerms: {0}"<br>
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
				"GlossaryTerms: {0}");
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
			String newGlossaryTermButtonLabel = getResourceBundleHelper(getLocale()).getString(
					PROP_NEW_GLOSSARY_TERM_BUTTON_LABEL, "Add");
			NavigationEvent openGlossaryTermEditor = new OpenPanelEvent(this,
					PanelActionType.Editor, getProjectOrDomain(), GlossaryTerm.class, null,
					WorkflowDisposition.NewFlow);
			NavigatorButton newGlossaryTermButton = new NavigatorButton(newGlossaryTermButtonLabel,
					getEventDispatcher(), openGlossaryTermEditor);
			newGlossaryTermButton.setStyleName(STYLE_NAME_DEFAULT);
			buttonsWrapper.add(newGlossaryTermButton);
		}

		add(buttonsWrapper);

		if (updateListener != null) {
			getEventDispatcher().removeEventTypeActionListener(UpdateEntityEvent.class,
					updateListener);
		}
		updateListener = new GlossaryTermUpdateListener(this);
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

	@Override
	public void setTargetObject(Object targetObject) {
		if (targetObject instanceof ProjectOrDomain) {
			pod = (ProjectOrDomain) targetObject;
			super.setTargetObject(pod.getGlossaryTerms());
		} else {
			log.error("unexpected target object " + targetObject);
		}
	}

	protected ProjectOrDomain getProjectOrDomain() {
		return pod;
	}

	private static class GlossaryTermUpdateListener implements ActionListener {
		static final long serialVersionUID = 0L;

		private final GlossaryTermNavigatorPanel panel;

		private GlossaryTermUpdateListener(GlossaryTermNavigatorPanel panel) {
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