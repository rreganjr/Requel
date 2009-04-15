/*
 * $Id: NavigatorTableCellValueFactory.java,v 1.1 2008/03/27 09:25:58 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.navigation.table;

/**
 * @author ron
 */
public interface NavigatorTableCellValueFactory {

	/**
	 * @param model
	 * @param column
	 * @param row
	 * @return
	 */
	public Object getValueAt(NavigatorTableModel model, final int column, final int row);

}
