/*
 * $Id: LoginController.java,v 1.7 2008/12/13 00:42:02 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
import edu.harvard.fas.rregan.uiframework.login.LoginEvent;
import edu.harvard.fas.rregan.uiframework.login.LoginFailedEvent;
import edu.harvard.fas.rregan.uiframework.login.LoginOkEvent;

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
				fireEvent(new LoginFailedEvent(this, "The system encountered an internal error. contact the system administrator."));
			}
		}
	}
}
