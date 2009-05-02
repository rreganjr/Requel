/*
 * $Id: OddEvenTableCellRenderer.java,v 1.2 2008/03/28 11:02:34 rregan Exp $
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
