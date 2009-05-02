/*
 * $Id: ReflectiveTableCellRenderer.java,v 1.1 2008/02/15 21:40:31 rregan Exp $
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
package edu.harvard.fas.rregan.uiframework.reflect.table;

import nextapp.echo2.app.Component;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Table;
import nextapp.echo2.app.table.TableCellRenderer;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.uiframework.reflect.ReflectUtils;

/**
 * @author rreganjr@users.sourceforge.net
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
