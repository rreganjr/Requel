/*
 * $Id: ProjectOpenIssuesNavigatorPanel.java,v 1.5 2009/02/23 08:49:50 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.ui.project;

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

import edu.harvard.fas.rregan.requel.Describable;
import edu.harvard.fas.rregan.requel.annotation.Annotatable;
import edu.harvard.fas.rregan.requel.annotation.Annotation;
import edu.harvard.fas.rregan.requel.annotation.Issue;
import edu.harvard.fas.rregan.requel.project.Project;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity;
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
import edu.harvard.fas.rregan.uiframework.panel.Panel;
import edu.harvard.fas.rregan.uiframework.panel.PanelActionType;

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
	 * label for the text of the cancel/reset button.
	 */
	public static final String PROP_CANCEL_BUTTON_LABEL = "CancelButton.Label";

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
	 * @see edu.harvard.fas.rregan.uiframework.panel.AbstractPanel#getTitle()
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
