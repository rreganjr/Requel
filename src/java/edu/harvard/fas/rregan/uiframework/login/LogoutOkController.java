/*
 * $Id: LogoutOkController.java,v 1.4 2008/08/20 08:29:01 rregan Exp $
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
package edu.harvard.fas.rregan.uiframework.login;

import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.webcontainer.command.BrowserRedirectCommand;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.uiframework.UIFrameworkApp;
import edu.harvard.fas.rregan.uiframework.controller.AbstractAppAwareController;

/**
 * This controller listens for LogoutOkEvents and removes the previously logged
 * in user from the UIFrameworkApp state and sets the current screen to the
 * supplied screen.
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
