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
package edu.harvard.fas.rregan.uiframework.panel;

import java.util.Collection;

import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import edu.harvard.fas.rregan.uiframework.navigation.event.SelectEntityEvent;
import edu.harvard.fas.rregan.uiframework.navigation.table.NavigatorTable;
import edu.harvard.fas.rregan.uiframework.navigation.table.NavigatorTableConfig;
import edu.harvard.fas.rregan.uiframework.navigation.table.NavigatorTableModel;

/**
 * @author ron
 */
public class SelectorTablePanel extends AbstractPanel implements ActionListener {
	static final long serialVersionUID = 0L;

	private NavigatorTable table;
	private NavigatorTableConfig tableConfig;

	/**
	 * @param tableConfig
	 * @param supportedContentType
	 * @param panelName
	 */
	protected SelectorTablePanel(Class<?> supportedContentType, String panelName) {
		this(SelectorTablePanel.class.getName(), supportedContentType, panelName);
	}

	/**
	 * @param resourceBundleName
	 * @param supportedContentType
	 * @param panelName
	 */
	protected SelectorTablePanel(String resourceBundleName, Class<?> supportedContentType,
			String panelName) {
		super(resourceBundleName, PanelActionType.Selector, supportedContentType, panelName);
	}

	@Override
	public void setup() {
		super.setup();
		setTable(new NavigatorTable(getTableConfig()));
		add(getTable());
		setTableModel(getTargetObject());
		getTable().addSelectionListener(this);
	}

	@Override
	public void setTargetObject(Object targetObject) {
		super.setTargetObject(targetObject);
		if (getTable() != null) {
			setTableModel(targetObject);
		}
	}

	private void setTableModel(Object targetObject) {
		if (targetObject != null) {
			NavigatorTableModelAdapter adapter = getTargetNavigatorTableModelAdapter();
			adapter.setTargetObject(targetObject);
			getTable().setModel(new NavigatorTableModel(adapter.getCollection()));
		}
	}

	/**
	 * This method should be overridden to return a collection when the target
	 * of the panel is not a collection.
	 * 
	 * @return an adapter to get the collection of items to select from from the
	 *         target object.
	 */
	protected NavigatorTableModelAdapter getTargetNavigatorTableModelAdapter() {
		return new NavigatorTableModelAdapter() {
			private Collection<Object> targetObject;

			@Override
			public Collection<Object> getCollection() {
				return targetObject;
			}

			@Override
			public void setTargetObject(Object targetObject) {
				this.targetObject = (Collection) targetObject;
			}
		};
	}

	@Override
	public void dispose() {
		super.dispose();
		getTable().removeSelectionListener(this);
		setTable(null);
		setTableConfig(null);
	}

	@Override
	public void setStyleName(String newValue) {
		super.setStyleName(newValue);
		getTable().setStyleName(newValue);
	}

	protected NavigatorTableConfig getTableConfig() {
		return tableConfig;
	}

	protected void setTableConfig(NavigatorTableConfig tableConfig) {
		this.tableConfig = tableConfig;
	}

	protected NavigatorTable getTable() {
		return table;
	}

	protected void setTable(NavigatorTable table) {
		this.table = table;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		SelectEntityEvent selectEvent = new SelectEntityEvent(this, getTable().getSelectedObject(),
				getDestinationObject());
		getEventDispatcher().dispatchEvent(selectEvent);
	}
}
