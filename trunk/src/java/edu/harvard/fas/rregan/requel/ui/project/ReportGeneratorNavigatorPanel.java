/*
 * $Id: ReportGeneratorNavigatorPanel.java,v 1.5 2009/03/27 13:43:14 rregan Exp $
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
import java.text.MessageFormat;
import java.text.SimpleDateFormat;

import nextapp.echo2.app.Alignment;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.app.layout.RowLayoutData;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.annotation.Annotatable;
import edu.harvard.fas.rregan.requel.annotation.Annotation;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity;
import edu.harvard.fas.rregan.requel.project.ReportGenerator;
import edu.harvard.fas.rregan.requel.project.Project;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.Stakeholder;
import edu.harvard.fas.rregan.requel.project.StakeholderPermissionType;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.impl.AbstractProjectOrDomainEntity;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.uiframework.navigation.DownloadButton;
import edu.harvard.fas.rregan.uiframework.navigation.NavigatorButton;
import edu.harvard.fas.rregan.uiframework.navigation.WorkflowDisposition;
import edu.harvard.fas.rregan.uiframework.navigation.event.ClosePanelEvent;
import edu.harvard.fas.rregan.uiframework.navigation.event.DeletedEntityEvent;
import edu.harvard.fas.rregan.uiframework.navigation.event.NavigationEvent;
import edu.harvard.fas.rregan.uiframework.navigation.event.OpenPanelEvent;
import edu.harvard.fas.rregan.uiframework.navigation.event.UpdateEntityEvent;
import edu.harvard.fas.rregan.uiframework.navigation.table.NavigatorTableCellValueFactory;
import edu.harvard.fas.rregan.uiframework.navigation.table.NavigatorTableColumnConfig;
import edu.harvard.fas.rregan.uiframework.navigation.table.NavigatorTableConfig;
import edu.harvard.fas.rregan.uiframework.navigation.table.NavigatorTableModel;
import edu.harvard.fas.rregan.uiframework.panel.NavigatorTablePanel;
import edu.harvard.fas.rregan.uiframework.panel.PanelActionType;

/**
 * @author ron
 */
public class ReportGeneratorNavigatorPanel extends NavigatorTablePanel {
	private static final Logger log = Logger.getLogger(ReportGeneratorNavigatorPanel.class);
	static final long serialVersionUID = 0;

	private final CommandHandler commandHandler;
	private final ProjectCommandFactory projectCommandFactory;
	private UpdateListener updateListener;
	private ProjectOrDomain pod;

	/**
	 * Property name to use in the ReportGeneratorNavigatorPanel.properties to
	 * set the label on the new ReportGenerator button.
	 */
	public static final String PROP_NEW_REPORT_BUTTON_LABEL = "NewReportButton.Label";

	/**
	 * Property name to use in the ReportGeneratorNavigatorPanel.properties to
	 * set the label for the text of the cancel/reset button.
	 */
	public static final String PROP_CANCEL_BUTTON_LABEL = "CancelButton.Label";

	/**
	 * Property name to use in the ReportGeneratorNavigatorPanel.properties to
	 * set the label on the edit report button in each row of the table.
	 */
	public static final String PROP_EDIT_REPORT_BUTTON_LABEL = "EditReportButton.Label";

	/**
	 * Property name to use in the ReportGeneratorNavigatorPanel.properties to
	 * set the label on the view report button in each row of the table when the
	 * user doesn't have edit permission.
	 */
	public static final String PROP_VIEW_REPORT_BUTTON_LABEL = "ViewReportButton.Label";

	/**
	 * Property name to use in the ReportGeneratorNavigatorPanel.properties to
	 * set the label on the run report button in each row of the table when the
	 * user doesn't have edit permission.
	 */
	public static final String PROP_RUN_REPORT_BUTTON_LABEL = "RunReportButton.Label";

	/**
	 * @param commandHandler
	 * @param projectCommandFactory
	 */
	public ReportGeneratorNavigatorPanel(CommandHandler commandHandler,
			ProjectCommandFactory projectCommandFactory) {
		super(ReportGeneratorNavigatorPanel.class.getName(), Project.class,
				ProjectManagementPanelNames.PROJECT_REPORTS_NAVIGATOR_PANEL_NAME);
		this.commandHandler = commandHandler;
		this.projectCommandFactory = projectCommandFactory;
		NavigatorTableConfig tableConfig = new NavigatorTableConfig();

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						ReportGenerator reportGenerator = (ReportGenerator) model
								.getBackingObject(row);
						String buttonLabel = null;
						if (isReadOnlyMode()) {
							buttonLabel = getResourceBundleHelper(getLocale()).getString(
									PROP_VIEW_REPORT_BUTTON_LABEL, "View");
						} else {
							buttonLabel = getResourceBundleHelper(getLocale()).getString(
									PROP_EDIT_REPORT_BUTTON_LABEL, "Edit");
						}
						NavigationEvent openEditorEvent = new OpenPanelEvent(this,
								PanelActionType.Editor, reportGenerator, ReportGenerator.class,
								null, WorkflowDisposition.NewFlow);
						NavigatorButton openEditorButton = new NavigatorButton(buttonLabel,
								getEventDispatcher(), openEditorEvent);
						openEditorButton.setStyleName(STYLE_NAME_PLAIN);
						RowLayoutData rld = new RowLayoutData();
						rld.setAlignment(Alignment.ALIGN_CENTER);
						openEditorButton.setLayoutData(rld);
						return openEditorButton;
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						ReportGenerator reportGenerator = (ReportGenerator) model
								.getBackingObject(row);
						String buttonLabel = null;
						buttonLabel = getResourceBundleHelper(getLocale()).getString(
								PROP_RUN_REPORT_BUTTON_LABEL, "Run");
						DownloadButton runReportButton = new DownloadButton(buttonLabel,
								new ReportDownloadProvider(ReportGeneratorNavigatorPanel.this,
										getProjectCommandFactory(), getCommandHandler(),
										reportGenerator));
						runReportButton.setStyleName(STYLE_NAME_PLAIN);
						RowLayoutData rld = new RowLayoutData();
						rld.setAlignment(Alignment.ALIGN_CENTER);
						runReportButton.setLayoutData(rld);
						return runReportButton;
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Name",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						ReportGenerator reportGenerator = (ReportGenerator) model
								.getBackingObject(row);
						return reportGenerator.getName();
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
	 * domain, by default the pattern is "Documents: {0}"<br>
	 * Valid variables are:<br>
	 * {0} - project/domain name<br>
	 * 
	 * @see Panel.PROP_PANEL_TITLE
	 * @see edu.harvard.fas.rregan.uiframework.panel.AbstractPanel#getTitle()
	 */
	@Override
	public String getTitle() {
		String name = "";
		String msgPattern = getResourceBundleHelper(getLocale()).getString(PROP_PANEL_TITLE,
				"Documents: {0}");
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
			String newReportGeneratorButtonLabel = getResourceBundleHelper(getLocale()).getString(
					PROP_NEW_REPORT_BUTTON_LABEL, "Add");
			NavigationEvent openReportGeneratorEditor = new OpenPanelEvent(this,
					PanelActionType.Editor, getProjectOrDomain(), ReportGenerator.class, null,
					WorkflowDisposition.NewFlow);
			NavigatorButton newReportGeneratorButton = new NavigatorButton(
					newReportGeneratorButtonLabel, getEventDispatcher(), openReportGeneratorEditor);
			newReportGeneratorButton.setStyleName(STYLE_NAME_DEFAULT);
			buttonsWrapper.add(newReportGeneratorButton);
		}

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
				return !stakeholder.hasPermission(ReportGenerator.class,
						StakeholderPermissionType.Edit);
			}
		}
		return true;
	}

	@Override
	public void setTargetObject(Object targetObject) {
		if (targetObject instanceof ProjectOrDomain) {
			pod = (ProjectOrDomain) targetObject;
			super.setTargetObject(((ProjectOrDomain) targetObject).getReportGenerators());
		} else {
			log.error("unexpected target object " + targetObject);
		}
	}

	protected ProjectOrDomain getProjectOrDomain() {
		return pod;
	}

	/**
	 * @return
	 */
	public CommandHandler getCommandHandler() {
		return commandHandler;
	}

	/**
	 * @return
	 */
	public ProjectCommandFactory getProjectCommandFactory() {
		return projectCommandFactory;
	}

	private static class UpdateListener implements ActionListener {
		static final long serialVersionUID = 0L;

		private final ReportGeneratorNavigatorPanel panel;

		private UpdateListener(ReportGeneratorNavigatorPanel panel) {
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
