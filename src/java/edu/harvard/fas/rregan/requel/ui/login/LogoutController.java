/*
 * $Id: LogoutController.java,v 1.4 2008/05/01 08:10:18 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.ui.login;

import nextapp.echo2.app.event.ActionEvent;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.requel.ui.AbstractRequelController;
import edu.harvard.fas.rregan.uiframework.login.LogoutEvent;
import edu.harvard.fas.rregan.uiframework.login.LogoutOkEvent;

/**
 * Controller for processing LogoutEvents.
 * 
 * @author ron
 */
@Controller
@Scope("prototype")
public class LogoutController extends AbstractRequelController {
	static final long serialVersionUID = 0;

	/**
	 * 
	 */
	public LogoutController() {
		super();
		addEventTypeToListenFor(LogoutEvent.class);
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		if (event instanceof LogoutEvent) {
			// TODO: any work needed when a user logs out
			fireEvent(new LogoutOkEvent(this));
		}
	}
}
