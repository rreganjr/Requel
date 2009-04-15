/*
 * $Id: AnnotationRefererTable.java,v 1.1 2009/02/15 09:31:34 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.ui.annotation;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;

import nextapp.echo2.app.Alignment;
import nextapp.echo2.app.Component;
import nextapp.echo2.app.layout.ColumnLayoutData;
import nextapp.echo2.app.layout.RowLayoutData;
import edu.harvard.fas.rregan.ResourceBundleHelper;
import edu.harvard.fas.rregan.requel.CreatedEntity;
import edu.harvard.fas.rregan.requel.Describable;
import edu.harvard.fas.rregan.requel.annotation.Annotation;
import edu.harvard.fas.rregan.requel.ui.AbstractRequelNavigatorTable;
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
 * A table of domain entities that refer to an annotation (note or annotation.)
 * 
 * @author ron
 */
public class AnnotationRefererTable extends AbstractRequelNavigatorTable {
	static final long serialVersionUID = 0L;

	static {
		ComponentManipulators.setManipulator(AnnotationRefererTable.class,
				new AnnotatablesTableManipulator());
	}

	/**
	 * The name to use in the properties file of the panel that includes the
	 * AnnotationContainersTable to define the label of the annotation
	 * containers field. If the property is undefined the panel should use a
	 * sensible default such as "Referers".
	 */
	public static final String PROP_ANNOTATABLES_LABEL = "AnnotationReferers.Label";

	/**
	 * The name to use in the containing panels properties file to set the label
	 * of the view button in the glossary term containers edit table column. If
	 * the property is undefined "View" is used.
	 */
	public static final String PROP_VIEW_CONTAINER_BUTTON_LABEL = "ViewAnnotationContainer.Label";

	/**
	 * The name to use in the containing panels properties file to set the label
	 * of the edit button in the annotation container edit table column. If the
	 * property is undefined "Edit" is used.
	 */
	public static final String PROP_EDIT_CONTAINER_BUTTON_LABEL = "EditAnnotationContainer.Label";

	private Annotation annotation;
	private final NavigatorTable table;

	/**
	 * @param editMode
	 * @param resourceBundleHelper
	 */
	public AnnotationRefererTable(EditMode editMode, ResourceBundleHelper resourceBundleHelper) {
		super(editMode, resourceBundleHelper);
		ColumnLayoutData layoutData = new ColumnLayoutData();
		layoutData.setAlignment(Alignment.ALIGN_CENTER);
		table = new NavigatorTable(getTableConfig());
		table.setLayoutData(layoutData);
		add(table);
	}

	protected Annotation getAnnotation() {
		return annotation;
	}

	protected void setAnnotation(Annotation annotation) {
		this.annotation = annotation;
		if (annotation != null) {
			table.setModel(new NavigatorTableModel((Collection) annotation.getAnnotatables()));
		} else {
			table.setModel(new NavigatorTableModel(Collections.EMPTY_SET));
		}
	}

	private NavigatorTableConfig getTableConfig() {
		NavigatorTableConfig tableConfig = new NavigatorTableConfig();

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Object annotationReferer = model.getBackingObject(row);
						String buttonLabel = null;
						if (isReadOnlyMode()) {
							buttonLabel = getResourceBundleHelper(getLocale()).getString(
									PROP_VIEW_CONTAINER_BUTTON_LABEL, "View");
						} else {
							buttonLabel = getResourceBundleHelper(getLocale()).getString(
									PROP_EDIT_CONTAINER_BUTTON_LABEL, "Edit");
						}
						NavigationEvent openEditorEvent = new OpenPanelEvent(this,
								PanelActionType.Editor, annotationReferer, annotationReferer
										.getClass(), null, WorkflowDisposition.NewFlow);
						NavigatorButton openEditorButton = new NavigatorButton(buttonLabel,
								getEventDispatcher(), openEditorEvent);
						openEditorButton.setStyleName(Panel.STYLE_NAME_PLAIN);
						RowLayoutData rld = new RowLayoutData();
						rld.setAlignment(Alignment.ALIGN_CENTER);
						openEditorButton.setLayoutData(rld);
						return openEditorButton;
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Description",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Object annotationReferer = model.getBackingObject(row);
						if (annotationReferer instanceof Describable) {
							return ((Describable) annotationReferer).getDescription();
						}
						return annotationReferer.toString();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Created By",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Object annotationReferer = model.getBackingObject(row);
						if (annotationReferer instanceof CreatedEntity) {
							return ((CreatedEntity) annotationReferer).getCreatedBy().getUsername();
						}
						return "";
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Date Created",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Object object = model.getBackingObject(row);
						DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
						if (object instanceof CreatedEntity) {
							return formatter.format(((CreatedEntity) object).getDateCreated());
						}
						return "";
					}
				}));

		return tableConfig;
	}

	private static class AnnotatablesTableManipulator extends AbstractComponentManipulator {

		protected AnnotatablesTableManipulator() {
			super();
		}

		@Override
		public Object getModel(Component component) {
			return getValue(component, Annotation.class);
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
			return type.cast(getComponent(component).getAnnotation());
		}

		@Override
		public void setValue(Component component, Object value) {
			getComponent(component).setAnnotation((Annotation) value);
		}

		private AnnotationRefererTable getComponent(Component component) {
			return (AnnotationRefererTable) component;
		}
	}
}