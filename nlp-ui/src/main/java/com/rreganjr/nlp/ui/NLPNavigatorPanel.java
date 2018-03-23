/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirements
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
package com.rreganjr.nlp.ui;

import java.util.Set;

import net.sf.echopm.navigation.NavigatorButton;
import net.sf.echopm.navigation.WorkflowDisposition;
import net.sf.echopm.navigation.event.NavigationEvent;
import net.sf.echopm.navigation.event.OpenPanelEvent;
import net.sf.echopm.panel.PanelActionType;
import nextapp.echo2.app.Alignment;
import nextapp.echo2.app.Component;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.echopm.navigation.tree.NavigatorTreeNodeFactory;
import net.sf.echopm.panel.NavigatorTreePanel;

/**
 * @author ron
 */
public class NLPNavigatorPanel extends NavigatorTreePanel {
	private static final Log log = LogFactory.getLog(NLPNavigatorPanel.class);
	static final long serialVersionUID = 0;

	/**
	 * Property name to use in the NLPNavigatorPanel.properties to set the
	 * label on the new parser button.
	 */
	public static final String PROP_NEW_PARSER_BUTTON_LABEL = "NewParserButton.Label";

	/**
	 * @param treeNodeFactories
	 */
	public NLPNavigatorPanel(Set<NavigatorTreeNodeFactory> treeNodeFactories) {
		super(NLPNavigatorPanel.class.getName(), treeNodeFactories,
				NLPPanelNames.NLP_NAVIGATOR_PANEL_NAME);
	}

	@Override
	public void setup() {
		add(createButton(PROP_NEW_PARSER_BUTTON_LABEL, "New Parser", NLPPanelNames.PARSER_PANEL_NAME));
		super.setup();
	}

	@Override
	public void dispose() {
		super.dispose();
		removeAll();
	}

	@Override
	public void setTargetObject(Object targetObject) {
		// this panel doesn't support a target class
	}

	@Override
	public Object getTargetObject() {
		return this.getClass();
	}

	private Component createButton(String labelResourceName, String labelDefault, String panelName) {
		String buttonLabel = getResourceBundleHelper(getLocale()).getString(labelResourceName, labelDefault);
		NavigationEvent event = new OpenPanelEvent(this, PanelActionType.Unspecified, null, null, panelName, WorkflowDisposition.NewFlow);
		NavigatorButton button = new NavigatorButton(buttonLabel, getEventDispatcher(), event);
		button.setStyleName(STYLE_NAME_DEFAULT);
		Row wrapper = new Row();
		wrapper.setInsets(new Insets(5));
		wrapper.setAlignment(new Alignment(Alignment.CENTER, Alignment.DEFAULT));
		wrapper.add(button);
		return wrapper;
	}
}
