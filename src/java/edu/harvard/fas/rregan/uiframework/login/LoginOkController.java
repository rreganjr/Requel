/*
 * $Id: LoginOkController.java,v 1.4 2008/08/24 04:13:43 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.login;

import nextapp.echo2.app.event.ActionEvent;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.uiframework.controller.AbstractAppAwareController;

/**
 * This controller listens for LoginOkEvents containing the user that logged in
 * and the name of the screen to display. When it recieves the event it sets the
 * user on the UIFrameworkApp and sets the current screen to the one supplied.
 * 
 * @author ron
 */
@Controller("loginOkController")
@Scope("prototype")
public class LoginOkController extends AbstractAppAwareController {
	static final long serialVersionUID = 0;

	/**
	 * 
	 */
	public LoginOkController() {
		super();
		addEventTypeToListenFor(LoginOkEvent.class);
	}

	/**
	 * @see nextapp.echo2.app.event.ActionListener#actionPerformed(nextapp.echo2.app.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		if (e instanceof LoginOkEvent) {
			LoginOkEvent event = (LoginOkEvent) e;
			getApp().setUser(event.getUser());
			getApp().setCurrentScreen(event.getScreenToDisplay());

			// tell the app to initialize itself
			fireEvent(new InitAppEvent(this, event.getUser()));
		}
	}
}
