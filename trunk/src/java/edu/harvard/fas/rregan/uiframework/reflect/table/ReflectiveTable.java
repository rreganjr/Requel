/*
 * $Id: ReflectiveTable.java,v 1.1 2008/02/15 21:40:33 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.reflect.table;

import java.util.Collection;

import org.apache.log4j.Logger;

import nextapp.echo2.app.Border;
import nextapp.echo2.app.Color;
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.Table;
import nextapp.echo2.app.table.TableColumnModel;

/**
 * A table that reflectively creates columns from a supplied class as
 * a JavaBean or via annotations.
 * 
 * see @ReflectiveTableModel and @ReflectiveTableColumnModel for details
 * of how the columns are created.
 * 
 * @author ron
 */
public class ReflectiveTable extends Table {
    private static final Logger log = Logger.getLogger(ReflectiveTable.class);
	private static final long serialVersionUID = 0;

	public ReflectiveTable(Class<?> clazz, int detailLevel, String confineToPackagesStartingWith) {
		this(clazz, detailLevel, confineToPackagesStartingWith, null);
	}
	
    public ReflectiveTable(Class<?> clazz, int detailLevel, String confineToPackagesStartingWith, Collection<?> options) {
        super(new ReflectiveTableModel(clazz, confineToPackagesStartingWith, detailLevel, options),
        		new ReflectiveTableColumnModel(clazz, detailLevel, confineToPackagesStartingWith));
        setStyleName("Table");
        setDefaultRenderer(Object.class, new ReflectiveTableCellRenderer());
        setBorder(new Border(new Extent(1, Extent.PX), Color.BLACK, Border.STYLE_SOLID));
        TableColumnModel columnModel = getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount() - 1; i++) {
            columnModel.getColumn(i).setWidth(new Extent(100/columnModel.getColumnCount(), Extent.PERCENT));
        }
        int totalWidth = 0;
        for (int i = 0; i < columnModel.getColumnCount() - 1; i++) {
        	totalWidth += columnModel.getColumn(i).getWidth().getValue();
        }
        columnModel.getColumn(columnModel.getColumnCount() - 1).setWidth(new Extent(100 - totalWidth, Extent.PERCENT));
    }

    public void setRowData(Collection<?> options) {
    	((ReflectiveTableModel)getModel()).setRowData(options);
    }
}
