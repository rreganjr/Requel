/*
 * $Id$
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

import java.util.Collection;

import nextapp.echo2.app.Border;
import nextapp.echo2.app.Color;
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.Table;
import nextapp.echo2.app.table.TableColumnModel;

import org.apache.log4j.Logger;

/**
 * A table that reflectively creates columns from a supplied class as a JavaBean
 * or via annotations. see
 * 
 * @ReflectiveTableModel and
 * @ReflectiveTableColumnModel for details of how the columns are created.
 * @author ron
 */
public class ReflectiveTable extends Table {
	private static final Logger log = Logger.getLogger(ReflectiveTable.class);
	private static final long serialVersionUID = 0;

	public ReflectiveTable(Class<?> clazz, int detailLevel, String confineToPackagesStartingWith) {
		this(clazz, detailLevel, confineToPackagesStartingWith, null);
	}

	public ReflectiveTable(Class<?> clazz, int detailLevel, String confineToPackagesStartingWith,
			Collection<?> options) {
		super(new ReflectiveTableModel(clazz, confineToPackagesStartingWith, detailLevel, options),
				new ReflectiveTableColumnModel(clazz, detailLevel, confineToPackagesStartingWith));
		setStyleName("Table");
		setDefaultRenderer(Object.class, new ReflectiveTableCellRenderer());
		setBorder(new Border(new Extent(1, Extent.PX), Color.BLACK, Border.STYLE_SOLID));
		TableColumnModel columnModel = getColumnModel();
		for (int i = 0; i < columnModel.getColumnCount() - 1; i++) {
			columnModel.getColumn(i).setWidth(
					new Extent(100 / columnModel.getColumnCount(), Extent.PERCENT));
		}
		int totalWidth = 0;
		for (int i = 0; i < columnModel.getColumnCount() - 1; i++) {
			totalWidth += columnModel.getColumn(i).getWidth().getValue();
		}
		columnModel.getColumn(columnModel.getColumnCount() - 1).setWidth(
				new Extent(100 - totalWidth, Extent.PERCENT));
	}

	public void setRowData(Collection<?> options) {
		((ReflectiveTableModel) getModel()).setRowData(options);
	}
}
