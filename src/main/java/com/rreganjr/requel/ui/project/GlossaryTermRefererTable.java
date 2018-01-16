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
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;

import nextapp.echo2.app.Alignment;
import nextapp.echo2.app.Component;
import nextapp.echo2.app.layout.ColumnLayoutData;
import nextapp.echo2.app.layout.RowLayoutData;
import net.sf.echopm.ResourceBundleHelper;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.ui.AbstractRequelNavigatorTable;
import net.sf.echopm.navigation.NavigatorButton;
import net.sf.echopm.navigation.WorkflowDisposition;
import net.sf.echopm.navigation.event.NavigationEvent;
import net.sf.echopm.navigation.event.OpenPanelEvent;
import net.sf.echopm.navigation.table.NavigatorTable;
import net.sf.echopm.navigation.table.NavigatorTableCellValueFactory;
import net.sf.echopm.navigation.table.NavigatorTableColumnConfig;
import net.sf.echopm.navigation.table.NavigatorTableConfig;
import net.sf.echopm.navigation.table.NavigatorTableModel;
import net.sf.echopm.panel.Panel;
import net.sf.echopm.panel.PanelActionType;
import net.sf.echopm.panel.editor.EditMode;
import net.sf.echopm.panel.editor.manipulators.AbstractComponentManipulator;
import net.sf.echopm.panel.editor.manipulators.ComponentManipulators;

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
