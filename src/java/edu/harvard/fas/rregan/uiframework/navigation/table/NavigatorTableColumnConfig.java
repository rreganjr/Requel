/*
 * $Id: NavigatorTableColumnConfig.java,v 1.1 2008/03/27 09:25:58 rregan Exp $
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

/**
 * A configuration for getting the title and values to display in a column of a
 * table.
 * 
 * @author ron
 */
public class NavigatorTableColumnConfig {

	private String title;
	private NavigatorTableCellValueFactory tableCellValueFactory;

	/**
	 * @param title
	 * @param tableCellValueFactory
	 */
	public NavigatorTableColumnConfig(String title,
			NavigatorTableCellValueFactory tableCellValueFactory) {
		setTitle(title);
		setTableCellValueFactory(tableCellValueFactory);
	}

	/**
	 * @return The column title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * set the title of a column.
	 * 
	 * @param title
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @return the factory for generating the value for a cell in this column
	 */
	public NavigatorTableCellValueFactory getTableCellValueFactory() {
		return tableCellValueFactory;
	}

	/**
	 * set the factory for generating the value for a cell in this column.
	 * 
	 * @param tableCellValueFactory
	 */
	public void setTableCellValueFactory(NavigatorTableCellValueFactory tableCellValueFactory) {
		this.tableCellValueFactory = tableCellValueFactory;
	}
}
