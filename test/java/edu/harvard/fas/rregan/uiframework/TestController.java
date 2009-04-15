/*
 * $Id: TestController.java,v 1.1 2008/03/08 11:05:15 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.uiframework;

import nextapp.echo2.app.event.ActionEvent;
import edu.harvard.fas.rregan.uiframework.controller.AbstractController;

/**
 * @author ron
 */
public class TestController extends AbstractController {
	static final long serialVersionUID = 0;

	private ActionEvent eventReceived;

	public void actionPerformed(ActionEvent e) {
		eventReceived = e;
	}

	public ActionEvent getEventReceived() {
		return eventReceived;
	}
}
