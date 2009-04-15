/*
 * $Id: NavigatorTableModel.java,v 1.4 2008/04/30 21:03:15 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.navigation.table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.harvard.fas.rregan.uiframework.panel.NavigatorTableModelAdapter;

import nextapp.echo2.app.table.AbstractTableModel;

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

	@Override
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
