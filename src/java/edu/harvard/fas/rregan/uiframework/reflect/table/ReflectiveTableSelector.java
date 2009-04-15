/*
 * $Id: ReflectiveTableSelector.java,v 1.1 2008/02/15 21:40:34 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.reflect.table;

import java.util.Collection;

import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;

import org.apache.log4j.Logger;

/**
 * A table for selecting a single object from a set of objects. The columns
 * of the table are created by reflecting the supplied class to extract 
 * properties. see @ReflectiveTableModel and @ReflectiveTableColumnModel
 * for details of how the columns are created.
 * 
 * @author ron
 */
public class ReflectiveTableSelector extends ReflectiveTable implements ActionListener {
    private static final Logger log = Logger.getLogger(ReflectiveTableSelector.class);
	private static final long serialVersionUID = 0;

	private Object selectedOption;

	public ReflectiveTableSelector(Class<?> clazz, int selectorLevel, String confineToPackagesStartingWith) {
		this(clazz, selectorLevel, confineToPackagesStartingWith, null);
	}
	
    public ReflectiveTableSelector(Class<?> clazz, int selectorLevel, String confineToPackagesStartingWith, Collection<?> options) {
        super(clazz, selectorLevel, confineToPackagesStartingWith, options);
        addActionListener(this);
    }

    public Object getSelectedOption() {
    	return this.selectedOption;
    }

    public void actionPerformed(ActionEvent arg0) {
        int selectedRow = getSelectionModel().getMinSelectedIndex();
        if (selectedRow > -1) {
        	selectedOption = ((ReflectiveTableModel)getModel()).getRowData().get(selectedRow);
        }
	}
}
