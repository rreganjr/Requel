/*
 * $Id: NavigatorTableConfig.java,v 1.2 2008/04/24 02:29:24 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.navigation.table;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ron
 */
public class NavigatorTableConfig {

	private final List<NavigatorTableColumnConfig> columnConfigs = new ArrayList<NavigatorTableColumnConfig>();
	private boolean rowLevelSelection = false;
	
	public NavigatorTableConfig() {
		super();
	}

	public List<NavigatorTableColumnConfig> getColumnConfigs() {
		return columnConfigs;
	}

	/**
	 * Add a new columnConfig to the end of the column list.
	 * 
	 * @param columnConfig
	 */
	public void addColumnConfig(NavigatorTableColumnConfig columnConfig) {
		getColumnConfigs().add(columnConfig);
	}

	/**
	 * Add a new columnConfig at the specified location moving the columns from
	 * that location and after over by one.
	 * 
	 * @param index
	 * @param columnConfig
	 */
	public void addColumnConfig(int index, NavigatorTableColumnConfig columnConfig) {
		getColumnConfigs().add(index, columnConfig);
	}
	
	public void setRowLevelSelection(boolean rowLevelSelection) {
		this.rowLevelSelection = rowLevelSelection;
	}
	
	public boolean getRowLevelSelection() {
		return rowLevelSelection;
	}
}
