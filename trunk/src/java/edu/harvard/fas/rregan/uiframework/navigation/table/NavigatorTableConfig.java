/*
 * $Id: NavigatorTableConfig.java,v 1.2 2008/04/24 02:29:24 rregan Exp $
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
