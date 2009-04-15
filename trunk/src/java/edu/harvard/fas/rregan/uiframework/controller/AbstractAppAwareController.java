/*
 * $Id: AbstractAppAwareController.java,v 1.3 2008/05/01 08:10:17 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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