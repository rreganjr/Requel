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
package edu.harvard.fas.rregan.nlp.ui;

import java.util.Set;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.uiframework.navigation.tree.NavigatorTreeNodeFactory;
import edu.harvard.fas.rregan.uiframework.panel.NavigatorTreePanel;

/**
 * @author ron
 */
public class NLPNavigatorPanel extends NavigatorTreePanel {
	private static final Logger log = Logger.getLogger(NLPNavigatorPanel.class);
	static final long serialVersionUID = 0;

	/**
	 * @param treeNodeFactories
	 */
	public NLPNavigatorPanel(Set<NavigatorTreeNodeFactory> treeNodeFactories) {
		super(NLPNavigatorPanel.class.getName(), treeNodeFactories,
				NLPPanelNames.NLP_NAVIGATOR_PANEL_NAME);
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
}
