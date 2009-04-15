/*
 * $Id: NavigatorTableColumnConfig.java,v 1.1 2008/03/27 09:25:58 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
