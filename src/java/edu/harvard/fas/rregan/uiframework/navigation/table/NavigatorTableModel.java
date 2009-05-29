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
package edu.harvard.fas.rregan.uiframework.navigation.table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import nextapp.echo2.app.table.AbstractTableModel;
import edu.harvard.fas.rregan.uiframework.panel.NavigatorTableModelAdapter;

/**
 * @author ron
 */
public class NavigatorTableModel extends AbstractTableModel {
	static final long serialVersionUID = 0;

	private List<Object> entities;
	private NavigatorTableConfig tableConfig;

	public NavigatorTableModel(NavigatorTableModelAdapter entityAdaptor) {
		super();
		setEntities(entityAdaptor.getCollection());
		fireTableDataChanged();
	}

	/**
	 * @param entities
	 * @param tableModelConfig
	 */
	public NavigatorTableModel(Collection<Object> entities) {
		super();
		setEntities(entities);
		fireTableDataChanged();
	}

	public int getColumnCount() {
		if (getTableConfig() != null) {
			return getTableConfig().getColumnConfigs().size();
		}
		return 0;
	}

	public int getRowCount() {
		return getEntities().size();
	}

	/**
	 * Return the object used for supplying data for a specific row if
	 * applicable.
	 * 
	 * @param row
	 * @return
	 */
	public Object getBackingObject(int row) {
		return getEntities().get(row);
	}

	public Object getValueAt(int column, int row) {
		return getTableConfig().getColumnConfigs().get(column).getTableCellValueFactory()
				.getValueAt(this, column, row);
	}

	@Override
	public String getColumnName(int column) {
		return getTableConfig().getColumnConfigs().get(column).getTitle();
	}

	/**
	 * @return
	 */
	protected NavigatorTableConfig getTableConfig() {
		return tableConfig;
	}

	public void setTableConfig(NavigatorTableConfig tableConfig) {
		this.tableConfig = tableConfig;
	}

	public List<Object> getEntities() {
		return entities;
	}

	protected void setEntities(Collection<Object> entities) {
		this.entities = new ArrayList<Object>(entities);
	}
}
