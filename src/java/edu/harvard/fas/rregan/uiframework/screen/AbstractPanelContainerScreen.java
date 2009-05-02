/*
 * $Id: AbstractPanelContainerScreen.java,v 1.2 2008/02/29 19:36:08 rregan Exp $
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
package edu.harvard.fas.rregan.uiframework.screen;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.uiframework.PanelContainer;
import edu.harvard.fas.rregan.uiframework.panel.PanelManager;

/**
 * @author ron
 */
public abstract class AbstractPanelContainerScreen extends AbstractScreen implements PanelContainer {
	private static final Logger log = Logger.getLogger(AbstractPanelContainerScreen.class);
	static final long serialVersionUID = 0;

	private final PanelManager panelManager;

	/**
	 * 
	 */
	protected AbstractPanelContainerScreen(String resourceBundleName, PanelManager panelManager) {
		super(resourceBundleName);
		panelManager.setPanelContainer(this);
		this.panelManager = panelManager;
	}

	public PanelManager getPanelManager() {
		return panelManager;
	}
}
