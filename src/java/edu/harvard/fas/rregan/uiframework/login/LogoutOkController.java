/*
 * $Id: LogoutOkController.java,v 1.4 2008/08/20 08:29:01 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.login;

import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.webcontainer.command.BrowserRedirectCommand;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.uiframework.UIFrameworkApp;
import edu.harvard.fas.rregan.uiframework.controller.AbstractAppAwareController;

/**
 * This controller listens for LogoutOkEvents and removes the
 * previously logged in user from the UIFrameworkApp state and
 * sets the current screen to the supplied screen.
 * 
 * @author ron
 */
@Controller
@Scope("prototype")
public class LogoutOkController extends AbstractAppAwareController {
	static final long serialVersionUID = 0;

	/**
	 * 
	 */
	public LogoutOkController() {
		this(null);
	}

	/**
	 * @param app
	 */
	public LogoutOkController(UIFrameworkApp app) {
		super(app);
		addEventTypeToListenFor(LogoutOkEvent.class);
	}

	/**
	 * @see nextapp.echo2.app.event.ActionListener#actionPerformed(nextapp.echo2.app.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		if (e instanceof LogoutOkEvent) {
			getApp().enqueueCommand(new BrowserRedirectCommand("logout"));
		}
	}
}
