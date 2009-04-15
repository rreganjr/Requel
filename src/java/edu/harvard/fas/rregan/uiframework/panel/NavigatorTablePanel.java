/*
 * $Id: NavigatorTablePanel.java,v 1.4 2008/10/30 05:55:07 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.uiframework.panel;

import java.util.Collection;

import nextapp.echo2.app.Label;
import edu.harvard.fas.rregan.uiframework.MessageHandler;
import edu.harvard.fas.rregan.uiframework.navigation.table.NavigatorTable;
import edu.harvard.fas.rregan.uiframework.navigation.table.NavigatorTableConfig;
import edu.harvard.fas.rregan.uiframework.navigation.table.NavigatorTableModel;

/**
 * @author ron
 */
public class NavigatorTablePanel extends AbstractPanel implements MessageHandler {
	static final long serialVersionUID = 0L;

	private NavigatorTable table;
	private NavigatorTableConfig tableConfig;
	private Label generalMessage;

	/**
	 * @param tableConfig
	 * @param supportedContentType
	 * @param panelName
	 */
	public NavigatorTablePanel(Class<?> supportedContentType, String panelName) {
		this(NavigatorTablePanel.class.getName(), supportedContentType, panelName);
	}

	/**
	 * @param resourceBundleName
	 * @param supportedContentType
	 * @param panelName
	 */
	public NavigatorTablePanel(String resourceBundleName, Class<?> supportedContentType,
			String panelName) {
		super(resourceBundleName, PanelActionType.Navigator, supportedContentType, panelName);
	}

	@Override
	public void setup() {
		super.setup();
		setTable(new NavigatorTable(getTableConfig()));
		add(getTable());
		if (getTargetObject() != null) {
			getTable().setModel(new NavigatorTableModel((Collection) getTargetObject()));
		}
		generalMessage = new Label();
		add(generalMessage);
	}

	@Override
	public void setTargetObject(Object targetObject) {
		super.setTargetObject(targetObject);
		if (getTable() != null) {
			getTable().setModel(new NavigatorTableModel((Collection) targetObject));
		}
	}

	@Override
	public void dispose() {
		super.dispose();
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
	public void setGeneralMessage(String message) {
		generalMessage.setText(message);
	}
}
