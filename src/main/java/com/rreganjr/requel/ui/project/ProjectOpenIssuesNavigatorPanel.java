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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import nextapp.echo2.app.Alignment;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.app.layout.RowLayoutData;

import org.apache.log4j.Logger;

import com.rreganjr.requel.Describable;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
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
import net.sf.echopm.panel.Panel;
import net.sf.echopm.panel.PanelActionType;

/**
 * A panel listing the open issues for all the entities in a project.
 * 
 * @author ron
 */
public class ProjectOpenIssuesNavigatorPanel extends NavigatorTablePanel {
	private static final Logger log = Logger.getLogger(ProjectOpenIssuesNavigatorPanel.class);
	static final long serialVersionUID = 0;

	private UpdateListener updateListener;
	private Project project;

	/**
	 * Property name to use in the GoalNavigatorPanel.properties to set the
	 * label on the edit goal button in each row of the table.
	 */
	public static final String PROP_EDIT_BUTTON_LABEL = "EditButton.Label";

	/**
	 * Property name to use in the GoalNavigatorPanel.properties to set the
	 * label on the view goal button in each row of the table when the user
	 * doesn't have edit permission.
	 */
	public static final String PROP_VIEW_BUTTON_LABEL = "ViewButton.Label";

	/**
	 */
	public ProjectOpenIssuesNavigatorPanel() {
		super(ProjectOpenIssuesNavigatorPanel.class.getName(), Project.class,
				ProjectManagementPanelNames.PROJECT_OPEN_ISSUES_NAVIGATOR_PANEL_NAME);
		setTableConfig(createTableConfig());
	}

	/**
	 * Create a title for panel with dynamic information from the project or
	 * domain, by default the pattern is "Open Issues: {0}"<br>
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
				"Open Issues: {0}");
		ProjectOrDomain pod = getProject();
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
		add(buttonsWrapper);

		if (updateListener != null) {
			getEventDispatcher().removeEventTypeActionListener(UpdateEntityEvent.class,
					updateListener);
		}
		updateListener = new UpdateListener(this);
		getEventDispatcher().addEventTypeActionListener(UpdateEntityEvent.class, updateListener);
	}

	protected boolean isReadOnlyMode() {
		return true;
	}

	@Override
	public void setTargetObject(Object targetObject) {
		if (targetObject instanceof Project) {
			project = (Project) targetObject;
			Set<Annotation> openIssues = new HashSet<Annotation>();
			openIssues.addAll((project).getAnnotations());
			for (ProjectOrDomainEntity entity : project.getProjectEntities()) {
				openIssues.addAll(entity.getAnnotations());
			}
			Iterator<Annotation> iter = openIssues.iterator();
			while (iter.hasNext()) {
				Annotation annotation = iter.next();
				if (!(annotation instanceof Issue) || ((Issue) annotation).isResolved()) {
					iter.remove();
				}
			}
			super.setTargetObject(openIssues);
		} else {
			log.error("unexpected target object " + targetObject);
		}
	}

	protected Project getProject() {
		return project;
	}

	private NavigatorTableConfig createTableConfig() {
		NavigatorTableConfig tableConfig = new NavigatorTableConfig();

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Annotation annotation = (Annotation) model.getBackingObject(row);
						String buttonLabel = null;
						if (isReadOnlyMode()) {
							buttonLabel = getResourceBundleHelper(getLocale()).getString(
									PROP_VIEW_BUTTON_LABEL, "View");
						} else {
							buttonLabel = getResourceBundleHelper(getLocale()).getString(
									PROP_EDIT_BUTTON_LABEL, "Edit");
						}
						NavigationEvent openEditorEvent = new OpenPanelEvent(this,
								PanelActionType.Editor, annotation, annotation.getClass(), null,
								WorkflowDisposition.NewFlow);
						NavigatorButton openEditorButton = new NavigatorButton(buttonLabel,
								getEventDispatcher(), openEditorEvent);
						openEditorButton.setStyleName(Panel.STYLE_NAME_PLAIN);
						RowLayoutData rld = new RowLayoutData();
						rld.setAlignment(Alignment.ALIGN_CENTER);
						openEditorButton.setLayoutData(rld);
						return openEditorButton;
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Annotatables",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Annotation annotation = (Annotation) model.getBackingObject(row);
						StringBuilder sb = new StringBuilder();
						Iterator<Annotatable> iter = annotation.getAnnotatables().iterator();
						while (iter.hasNext()) {
							Annotatable annotatable = iter.next();
							if (annotatable instanceof Describable) {
								sb.append(((Describable) annotatable).getDescription());
								if (iter.hasNext()) {
									sb.append("; ");
								}
							}
						}
						return sb.toString();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Text",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Annotation annotation = (Annotation) model.getBackingObject(row);
						return annotation.getText();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Created By",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Annotation annotation = (Annotation) model.getBackingObject(row);
						return annotation.getCreatedBy().getUsername();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Date Created",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Annotation annotation = (Annotation) model.getBackingObject(row);
						DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
						return formatter.format(annotation.getDateCreated());
					}
				}));

		return tableConfig;
	}

	private static class UpdateListener implements ActionListener {
		static final long serialVersionUID = 0L;

		private final ProjectOpenIssuesNavigatorPanel panel;

		private UpdateListener(ProjectOpenIssuesNavigatorPanel panel) {
			this.panel = panel;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e instanceof UpdateEntityEvent) {
				UpdateEntityEvent event = (UpdateEntityEvent) e;
				if (event.getObject() instanceof Project) {
					Project updatedProject = (Project) event.getObject();
					if (panel.getProject().equals(updatedProject)) {
						panel.setTargetObject(updatedProject);
					}
				} else if (event.getObject() instanceof ProjectOrDomainEntity) {
					ProjectOrDomainEntity updatedProjectEntity = (ProjectOrDomainEntity) event
							.getObject();
					if (updatedProjectEntity.getProjectOrDomain() instanceof Project) {
						Project updatedProject = (Project) updatedProjectEntity
								.getProjectOrDomain();
						if (panel.getProject().equals(updatedProject)) {
							panel.setTargetObject(updatedProject);
						}
					}
				} else if (event.getObject() instanceof Annotation) {
					Annotation updatedAnnotation = (Annotation) event.getObject();
					if (event instanceof DeletedEntityEvent) {
						((Set<Annotation>) panel.getTargetObject()).remove(updatedAnnotation);
					} else {
						for (Annotatable annotatable : updatedAnnotation.getAnnotatables()) {
							if (annotatable instanceof Project) {
								Project updatedProject = (Project) annotatable;
								if (panel.getProject().equals(updatedProject)) {
									panel.setTargetObject(updatedProject);
								}
							} else if (annotatable instanceof ProjectOrDomainEntity) {
								ProjectOrDomainEntity entity = (ProjectOrDomainEntity) annotatable;
								if (panel.getProject().equals(entity.getProjectOrDomain())) {
									panel.setTargetObject(entity.getProjectOrDomain());
								}
							}
						}
					}
				}
			}
		}
	}
}
