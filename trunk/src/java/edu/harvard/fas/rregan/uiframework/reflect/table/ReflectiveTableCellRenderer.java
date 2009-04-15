/*
 * $Id: ReflectiveTableCellRenderer.java,v 1.1 2008/02/15 21:40:31 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.reflect.table;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.uiframework.reflect.ReflectUtils;

import nextapp.echo2.app.Component;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Table;
import nextapp.echo2.app.table.TableCellRenderer;

/**
 *
 * @author rreganjr@acm.org 
 */
public class ReflectiveTableCellRenderer implements TableCellRenderer {
    private static final Logger log = Logger.getLogger(ReflectiveTableCellRenderer.class);
	private static final long serialVersionUID = 0;

	public Component getTableCellRendererComponent(Table table, Object value, int column, int row) {
		String labelString = "(null)";
		if (value != null) {
			labelString = ReflectUtils.getLabelForObject(value);
		}
		Label label = new Label(labelString);

        if (row % 2 == 0) {
            label.setStyleName("Table.EvenRowLabel");
        } else {
            label.setStyleName("Table.OddRowLabel");
        }
		return label;
	}


}
