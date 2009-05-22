/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * 
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
