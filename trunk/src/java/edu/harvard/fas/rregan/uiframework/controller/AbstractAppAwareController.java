/*
 * $Id: AbstractAppAwareController.java,v 1.3 2008/05/01 08:10:17 rregan Exp $
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
package edu.harvard.fas.rregan.uiframework.controller;

import edu.harvard.fas.rregan.uiframework.UIFrameworkApp;
import edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher;

/**
 * An abstract controller that makes the current UIFrameworkApp available to
 * sub-classes.
 * 
 * @author ron
 */
public abstract class AbstractAppAwareController extends AbstractController implements
		AppAwareController {

	private UIFrameworkApp app;

	protected AbstractAppAwareController() {
		super();
	}

	protected AbstractAppAwareController(UIFrameworkApp app) {
		super();
		setApp(app);
	}

	protected AbstractAppAwareController(EventDispatcher eventDispatcher, UIFrameworkApp app) {
		super(eventDispatcher);
		setApp(app);
	}

	/**
	 * This method is public so that the UIFrameworkApp can pass itself to the
	 * controller when initializing it.
	 * 
	 * @see edu.harvard.fas.rregan.uiframework.controller.AppAwareController#setApp(edu.harvard.fas.rregan.uiframework.UIFrameworkApp)
	 */
	public void setApp(UIFrameworkApp app) {
		this.app = app;
	}

	protected UIFrameworkApp getApp() {
		return app;
	}
}