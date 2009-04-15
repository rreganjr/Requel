/*
 * $Id: GlossaryTermRefererTable.java,v 1.4 2009/01/08 06:48:45 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.ui.project;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;

import nextapp.echo2.app.Alignment;
import nextapp.echo2.app.Component;
import nextapp.echo2.app.layout.ColumnLayoutData;
import nextapp.echo2.app.layout.RowLayoutData;
import edu.harvard.fas.rregan.ResourceBundleHelper;
import edu.harvard.fas.rregan.requel.project.GlossaryTerm;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity;
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
 * A table of domain entities that refer to glossary terms.
 * 
 * @author ron
 */
public class GlossaryTermRefererTable extends AbstractRequelNavigatorTable {
	static final long serialVersionUID = 0L;

	static {
		ComponentManipulators.setManipulator(GlossaryTermRefererTable.class,
				new GlossaryTermContainersTableManipulator());
	}

	/**
	 * The name to use in the properties file of the panel that includes the
	 * GlossaryTermContainersTable to define the label of the glossary term
	 * containers field. If the property is undefined the panel should use a
	 * sensible default such as "Glossary Term Referers".
	 */
	public static final String PROP_LABEL_GLOSSARY_TERM_CONTAINERS = "GlossaryTermReferers.Label";

	/**
	 * The name to use in the containing panels properties file to set the label
	 * of the view button in the glossary term containers edit table column. If
	 * the property is undefined "View" is used.
	 */
	public static final String PROP_VIEW_GLOSSARY_TERM_CONTAINER_BUTTON_LABEL = "ViewGlossaryTermContainer.Label";

	/**
	 * The name to use in the containing panels properties file to set the label
	 * of the edit button in the glossary term container edit table column. If
	 * the property is undefined "Edit" is used.
	 */
	public static final String PROP_EDIT_GLOSSARY_TERM_CONTAINER_BUTTON_LABEL = "EditGlossaryTermContainer.Label";

	private GlossaryTerm glossaryTerm;
	private final NavigatorTable table;

	/**
	 * @param editMode
	 * @param resourceBundleHelper
	 */
	public GlossaryTermRefererTable(EditMode editMode, ResourceBundleHelper resourceBundleHelper) {
		super(editMode, resourceBundleHelper);
		ColumnLayoutData layoutData = new ColumnLayoutData();
		layoutData.setAlignment(Alignment.ALIGN_CENTER);
		table = new NavigatorTable(getTableConfig());
		table.setLayoutData(layoutData);
		add(table);
	}

	protected GlossaryTerm getGlossaryTerm() {
		return glossaryTerm;
	}

	protected void setGlossaryTerm(GlossaryTerm glossaryTerm) {
		this.glossaryTerm = glossaryTerm;
		if (glossaryTerm != null) {
			table.setModel(new NavigatorTableModel((Collection) glossaryTerm.getReferers()));
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
						ProjectOrDomainEntity glossaryTermReferer = (ProjectOrDomainEntity) model
								.getBackingObject(row);
						String buttonLabel = null;
						if (isReadOnlyMode()) {
							buttonLabel = getResourceBundleHelper(getLocale()).getString(
									PROP_VIEW_GLOSSARY_TERM_CONTAINER_BUTTON_LABEL, "View");
						} else {
							buttonLabel = getResourceBundleHelper(getLocale()).getString(
									PROP_EDIT_GLOSSARY_TERM_CONTAINER_BUTTON_LABEL, "Edit");
						}
						NavigationEvent openEditorEvent = new OpenPanelEvent(this,
								PanelActionType.Editor, glossaryTermReferer, glossaryTermReferer
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
						ProjectOrDomainEntity glossaryTermReferer = (ProjectOrDomainEntity) model
								.getBackingObject(row);
						return glossaryTermReferer.getDescription();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Created By",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						ProjectOrDomainEntity glossaryTermReferer = (ProjectOrDomainEntity) model
								.getBackingObject(row);
						return glossaryTermReferer.getCreatedBy().getUsername();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Date Created",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						ProjectOrDomainEntity glossaryTermReferer = (ProjectOrDomainEntity) model
								.getBackingObject(row);
						DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
						return formatter.format(glossaryTermReferer.getDateCreated());
					}
				}));

		return tableConfig;
	}

	private static class GlossaryTermContainersTableManipulator extends
			AbstractComponentManipulator {

		protected GlossaryTermContainersTableManipulator() {
			super();
		}

		@Override
		public Object getModel(Component component) {
			return getValue(component, GlossaryTerm.class);
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
			return type.cast(getComponent(component).getGlossaryTerm());
		}

		@Override
		public void setValue(Component component, Object value) {
			getComponent(component).setGlossaryTerm((GlossaryTerm) value);
		}

		private GlossaryTermRefererTable getComponent(Component component) {
			return (GlossaryTermRefererTable) component;
		}
	}
}
