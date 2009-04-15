/*
 * $Id: OddEvenTableCellRenderer.java,v 1.2 2008/03/28 11:02:34 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.navigation.table;

import nextapp.echo2.app.Component;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.Table;
import nextapp.echo2.app.table.DefaultTableCellRenderer;

/**
 * Render table cell components wrapped in Row components with alternating odd
 * and even styling.
 * 
 * @author ron
 */
public class OddEvenTableCellRenderer extends DefaultTableCellRenderer {
	static final long serialVersionUID = 0;

	private final String evenCellStyleName;
	private final String oddCellStyleName;

	/**
	 * @param oddCellStyleName
	 * @param evenCellStyleName
	 */
	public OddEvenTableCellRenderer(String oddCellStyleName, String evenCellStyleName) {
		this.oddCellStyleName = oddCellStyleName;
		this.evenCellStyleName = evenCellStyleName;
	}

	@Override
	public Component getTableCellRendererComponent(Table table, Object value, int column, int row) {
		Component component = getRendererComponent(value);
		Row wrapper = new Row();
		if (row % 2 == 0) {
			wrapper.setStyleName(evenCellStyleName);
		} else {
			wrapper.setStyleName(oddCellStyleName);
		}
		wrapper.add(component);
		return wrapper;
	}

	protected Component getRendererComponent(Object value) {
		Component component;
		if (value instanceof Component) {
			component = (Component) value;
		} else {
			component = new Label((value == null ? "" : value.toString()));
		}
		return component;
	}
}
