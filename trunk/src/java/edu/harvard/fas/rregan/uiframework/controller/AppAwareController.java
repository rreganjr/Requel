/*
 * $Id: AppAwareController.java,v 1.1 2008/02/20 11:36:29 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.uiframework.controller;

import edu.harvard.fas.rregan.uiframework.UIFrameworkApp;

/**
 * @author ron
 */
public interface AppAwareController extends Controller {

	/**
	 * Set the app this command is aware of
	 * 
	 * @param app
	 */
	public void setApp(UIFrameworkApp app);
}
