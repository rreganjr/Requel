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
package com.rreganjr.requel.ui.login;

import nextapp.echo2.app.event.ActionEvent;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.requel.ui.AbstractRequelController;
import net.sf.echopm.login.LogoutEvent;
import net.sf.echopm.login.LogoutOkEvent;

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
