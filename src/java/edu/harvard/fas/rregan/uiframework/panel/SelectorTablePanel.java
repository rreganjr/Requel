/*
 * $Id: SelectorTablePanel.java,v 1.5 2008/09/11 09:47:00 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.uiframework.panel;

import java.util.Collection;

import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import edu.harvard.fas.rregan.uiframework.navigation.event.SelectEntityEvent;
import edu.harvard.fas.rregan.uiframework.navigation.table.NavigatorTable;
import edu.harvard.fas.rregan.uiframework.navigation.table.NavigatorTableConfig;
import edu.harvard.fas.rregan.uiframework.navigation.table.NavigatorTableModel;

/**
 * @author ron
 */
public class SelectorTablePanel extends AbstractPanel implements ActionListener {
	static final long serialVersionUID = 0L;

	private NavigatorTable table;
	private NavigatorTableConfig tableConfig;

	/**
	 * @param tableConfig
	 * @param supportedContentType
	 * @param panelName
	 */
	protected SelectorTablePanel(Class<?> supportedContentType, String panelName) {
		this(SelectorTablePanel.class.getName(), supportedContentType, panelName);
	}

	/**
	 * @param resourceBundleName
	 * @param supportedContentType
	 * @param panelName
	 */
	protected SelectorTablePanel(String resourceBundleName, Class<?> supportedContentType,
			String panelName) {
		super(resourceBundleName, PanelActionType.Selector, supportedContentType, panelName);
	}

	@Override
	public void setup() {
		super.setup();
		setTable(new NavigatorTable(getTableConfig()));
		add(getTable());
		setTableModel(getTargetObject());
		getTable().addSelectionListener(this);
	}

	@Override
	public void setTargetObject(Object targetObject) {
		super.setTargetObject(targetObject);
		if (getTable() != null) {
			setTableModel(targetObject);
		}
	}

	private void setTableModel(Object targetObject) {
		if (targetObject != null) {
			NavigatorTableModelAdapter adapter = getTargetNavigatorTableModelAdapter();
			adapter.setTargetObject(targetObject);
			getTable().setModel(new NavigatorTableModel(adapter.getCollection()));
		}
	}

	/**
	 * This method should be overridden to return a collection when the target
	 * of the panel is not a collection.
	 * 
	 * @return an adapter to get the collection of items to select from from the
	 *         target object.
	 */
	protected NavigatorTableModelAdapter getTargetNavigatorTableModelAdapter() {
		return new NavigatorTableModelAdapter() {
			private Collection<Object> targetObject;

			@Override
			public Collection<Object> getCollection() {
				return targetObject;
			}

			@Override
			public void setTargetObject(Object targetObject) {
				this.targetObject = (Collection) targetObject;
			}
		};
	}

	@Override
	public void dispose() {
		super.dispose();
		getTable().removeSelectionListener(this);
		setTable(null);
		setTableConfig(null);
	}

	@Override
	public void setStyleName(String newValue) {
		super.setStyleName(newValue);
		getTable().setStyleName(newValue);
	}

	protected NavigatorTableConfig getTableConfig() {
		return tableConfig;
	}

	protected void setTableConfig(NavigatorTableConfig tableConfig) {
		this.tableConfig = tableConfig;
	}

	protected NavigatorTable getTable() {
		return table;
	}

	protected void setTable(NavigatorTable table) {
		this.table = table;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		SelectEntityEvent selectEvent = new SelectEntityEvent(this, getTable().getSelectedObject(),
				getDestinationObject());
		getEventDispatcher().dispatchEvent(selectEvent);
	}
}
