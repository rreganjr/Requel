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
package edu.harvard.fas.rregan.requel.ui.annotation;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;

import nextapp.echo2.app.Alignment;
import nextapp.echo2.app.Component;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.layout.ColumnLayoutData;
import nextapp.echo2.app.layout.RowLayoutData;
import edu.harvard.fas.rregan.ResourceBundleHelper;
import edu.harvard.fas.rregan.requel.annotation.Annotatable;
import edu.harvard.fas.rregan.requel.annotation.Annotation;
import edu.harvard.fas.rregan.requel.annotation.Issue;
import edu.harvard.fas.rregan.requel.annotation.Note;
import edu.harvard.fas.rregan.requel.project.Project;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity;
import edu.harvard.fas.rregan.requel.project.Stakeholder;
import edu.harvard.fas.rregan.requel.project.StakeholderPermissionType;
import edu.harvard.fas.rregan.requel.ui.AbstractRequelNavigatorTable;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.uiframework.navigation.NavigatorButton;
import edu.harvard.fas.rregan.uiframework.navigation.WorkflowDisposition;
import edu.harvard.fas.rregan.uiframework.navigation.event.NavigationEvent;
import edu.harvard.fas.rregan.uiframework.navigation.event.OpenPanelEvent;
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
 * A component to add to Panels of annotatable entity editors to enable editing
 * of the annotations of the entity.
 * 
 * @author ron
 */
public class AnnotationsTable extends AbstractRequelNavigatorTable {
	static final long serialVersionUID = 0L;

	static {
		ComponentManipulators.setManipulator(AnnotationsTable.class,
				new AnnotationsTableManipulator());
	}

	/**
	 * The name to use in the properties file of the panel that includes the
	 * AnnotationsTable to define the label of the annotations field. If the
	 * property is undefined the panel should use a sensible default such as
	 * "Annotations".
	 */
	public static final String PROP_LABEL_ANNOTATIONS = "Annotations.Label";

	/**
	 * The name to use in the containing panels properties file to set the label
	 * of the view button in the annotation edit table column. If the property
	 * is undefined "View" is used.
	 */
	public static final String PROP_VIEW_ANNOTATION_BUTTON_LABEL = "ViewAnnotation.Label";

	/**
	 * The name to use in the containing panels properties file to set the label
	 * of the edit button in the annotation edit table column. If the property
	 * is undefined "Edit" is used.
	 */
	public static final String PROP_EDIT_ANNOTATION_BUTTON_LABEL = "EditAnnotation.Label";

	/**
	 * The name to use in the containing panels properties file to set the label
	 * of the add issue button under the annotations table. If the property is
	 * undefined "Add Issue" is used.
	 */
	public static final String PROP_ADD_ISSUE_BUTTON_LABEL = "AddIssue.Label";

	/**
	 * The name to use in the containing panels properties file to set the label
	 * of the add note button under the annotations table. If the property is
	 * undefined "Add Note" is used.
	 */
	public static final String PROP_ADD_NOTE_BUTTON_LABEL = "AddNote.Label";

	private Annotatable annotatable;
	private final NavigatorTable table;
	private final NavigatorButton openIssueEditorButton;
	private final NavigatorButton openNoteEditorButton;

	/**
	 * @param editMode
	 * @param resourceBundleHelper
	 */
	public AnnotationsTable(EditMode editMode, ResourceBundleHelper resourceBundleHelper) {
		super(editMode, resourceBundleHelper);
		ColumnLayoutData layoutData = new ColumnLayoutData();
		layoutData.setAlignment(Alignment.ALIGN_CENTER);
		table = new NavigatorTable(getTableConfig());
		table.setLayoutData(layoutData);
		add(table);

		Row buttons = new Row();
		buttons.setLayoutData(layoutData);
		String buttonLabel = getResourceBundleHelper(getLocale()).getString(
				PROP_ADD_ISSUE_BUTTON_LABEL, "Add Issue");
		openIssueEditorButton = new NavigatorButton(buttonLabel, getEventDispatcher());
		openIssueEditorButton.setStyleName(Panel.STYLE_NAME_DEFAULT);
		buttons.add(openIssueEditorButton);

		buttonLabel = getResourceBundleHelper(getLocale()).getString(PROP_ADD_NOTE_BUTTON_LABEL,
				"Add Note");
		openNoteEditorButton = new NavigatorButton(buttonLabel, getEventDispatcher());
		openNoteEditorButton.setStyleName(Panel.STYLE_NAME_DEFAULT);
		buttons.add(openNoteEditorButton);
		add(buttons);
	}

	protected Annotatable getAnnotatable() {
		return annotatable;
	}

	protected void setAnnotatable(Annotatable annotatable) {
		this.annotatable = annotatable;
		if (annotatable != null) {
			table.setModel(new NavigatorTableModel((Collection) annotatable.getAnnotations()));
			if (!isReadOnlyMode()) {
				NavigationEvent openIssueEditorEvent = new OpenPanelEvent(this,
						PanelActionType.Editor, getAnnotatable(), Issue.class, null,
						WorkflowDisposition.NewFlow);
				openIssueEditorButton.setEventToFire(openIssueEditorEvent);
				openIssueEditorButton.setEnabled(true);
				openIssueEditorButton.setVisible(true);

				NavigationEvent openNoteEditorEvent = new OpenPanelEvent(this,
						PanelActionType.Editor, getAnnotatable(), Note.class, null,
						WorkflowDisposition.NewFlow);
				openNoteEditorButton.setEventToFire(openNoteEditorEvent);
				openNoteEditorButton.setEnabled(true);
				openNoteEditorButton.setVisible(true);
			} else {
				openIssueEditorButton.setVisible(false);
				openNoteEditorButton.setVisible(false);
				openIssueEditorButton.setEnabled(false);
				openNoteEditorButton.setEnabled(false);
			}
		} else {
			table.setModel(new NavigatorTableModel(Collections.EMPTY_SET));
			openIssueEditorButton.setVisible(false);
			openNoteEditorButton.setVisible(false);
			openIssueEditorButton.setEnabled(false);
			openNoteEditorButton.setEnabled(false);
		}
	}

	@Override
	public boolean isReadOnlyMode() {
		// TODO: this causes a project package dependency
		// project entities are subject to stakeholder permissions, so if this
		// note is concerned with a project or project entity check the
		// stakeholder permissions for editing annotations.
		boolean projectEntity = false;
		User user = (User) getApp().getUser();
		Annotatable annotatable = getAnnotatable();
		if (annotatable != null) {
			Stakeholder stakeholder = null;
			if (annotatable instanceof Project) {
				projectEntity = true;
				Project project = (Project) annotatable;
				stakeholder = project.getUserStakeholder(user);
			} else if (annotatable instanceof ProjectOrDomainEntity) {
				ProjectOrDomainEntity podEntity = (ProjectOrDomainEntity) annotatable;
				if (podEntity.getProjectOrDomain() instanceof Project) {
					projectEntity = true;
					Project project = (Project) podEntity.getProjectOrDomain();
					stakeholder = project.getUserStakeholder(user);
				}
			}
			if (stakeholder != null) {
				return !stakeholder.hasPermission(Annotation.class, StakeholderPermissionType.Edit);
			}
		}
		return projectEntity;
	}

	private NavigatorTableConfig getTableConfig() {
		NavigatorTableConfig tableConfig = new NavigatorTableConfig();

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Annotation annotation = (Annotation) model.getBackingObject(row);
						String buttonLabel = null;
						if (isReadOnlyMode()) {
							buttonLabel = getResourceBundleHelper(getLocale()).getString(
									PROP_VIEW_ANNOTATION_BUTTON_LABEL, "View");
						} else {
							buttonLabel = getResourceBundleHelper(getLocale()).getString(
									PROP_EDIT_ANNOTATION_BUTTON_LABEL, "Edit");
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

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Type",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Annotation annotation = (Annotation) model.getBackingObject(row);
						return annotation.getTypeName();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Status",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Annotation annotation = (Annotation) model.getBackingObject(row);
						// TODO: replace with resource strings
						return annotation.getStatusMessage();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Must Be Resolved?",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Annotation annotation = (Annotation) model.getBackingObject(row);
						// TODO: replace with resource strings
						return (annotation.isMustBeResolved() ? "Yes" : "No");
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

	private static class AnnotationsTableManipulator extends AbstractComponentManipulator {

		protected AnnotationsTableManipulator() {
			super();
		}

		@Override
		public Object getModel(Component component) {
			return getValue(component, Annotatable.class);
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
			return type.cast(getComponent(component).getAnnotatable());
		}

		@Override
		public void setValue(Component component, Object value) {
			getComponent(component).setAnnotatable((Annotatable) value);
		}

		private AnnotationsTable getComponent(Component component) {
			return (AnnotationsTable) component;
		}
	}
}
