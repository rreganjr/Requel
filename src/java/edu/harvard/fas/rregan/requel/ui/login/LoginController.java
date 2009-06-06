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
package edu.harvard.fas.rregan.requel.ui.login;

import nextapp.echo2.app.event.ActionEvent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.ui.AbstractRequelCommandController;
import edu.harvard.fas.rregan.requel.ui.RequelMainScreen;
import edu.harvard.fas.rregan.requel.user.command.LoginCommand;
import edu.harvard.fas.rregan.requel.user.command.UserCommandFactory;
import edu.harvard.fas.rregan.requel.user.exception.NoSuchUserException;
import net.sf.echopm.login.LoginEvent;
import net.sf.echopm.login.LoginFailedEvent;
import net.sf.echopm.login.LoginOkEvent;

/**
 * Command for processing LoginEvents.
 * 
 * @author ron
 */
@Controller("loginController")
@Scope("prototype")
public class LoginController extends AbstractRequelCommandController {
	static final long serialVersionUID = 0;

	/**
	 * @param commandFactory
	 * @param commandHandler
	 */
	@Autowired
	public LoginController(UserCommandFactory commandFactory, CommandHandler commandHandler) {
		super(commandFactory, commandHandler);
		addEventTypeToListenFor(LoginEvent.class);
	}

	public void actionPerformed(ActionEvent event) {
		if (event instanceof LoginEvent) {
			LoginEvent loginEvent = (LoginEvent) event;
			try {
				UserCommandFactory factory = getCommandFactory();
				LoginCommand command = factory.newLoginCommand();
				command.setUsername(loginEvent.getUsername());
				command.setPassword(loginEvent.getPassword());
				command = getCommandHandler().execute(command);
				fireEvent(new LoginOkEvent(this, command.getUser(), RequelMainScreen.screenName));
			} catch (NoSuchUserException e) {
				fireEvent(new LoginFailedEvent(this, "The username and password are not valid."));
			} catch (Exception e) {
				fireEvent(new LoginFailedEvent(this,
						"The system encountered an internal error. contact the system administrator."));
			}
		}
	}
}
