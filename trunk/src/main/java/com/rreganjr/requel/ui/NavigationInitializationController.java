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
package com.rreganjr.requel.ui;

import nextapp.echo2.app.event.ActionEvent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.nlp.ui.NLPPanelNames;
import com.rreganjr.requel.user.SystemAdminUserRole;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.UserRole;
import net.sf.echopm.login.InitAppEvent;
import net.sf.echopm.navigation.WorkflowDisposition;
import net.sf.echopm.navigation.event.OpenPanelEvent;
import net.sf.echopm.panel.PanelActionType;

/**
 * @author ron
 */
@Controller
@Scope("prototype")
public class NavigationInitializationController extends AbstractRequelController {
	static final long serialVersionUID = 0;

	private final UserRepository userRepository;

	/**
	 * @param userRepository
	 */
	@Autowired
	public NavigationInitializationController(UserRepository userRepository) {
		super();
		this.userRepository = userRepository;
		addEventTypeToListenFor(InitAppEvent.class);
	}

	/**
	 * @see nextapp.echo2.app.event.ActionListener#actionPerformed(nextapp.echo2.app.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		if (e instanceof InitAppEvent) {
			InitAppEvent event = (InitAppEvent) e;
			User user = (User) event.getUser();
			for (UserRole userRole : user.getUserRoles()) {
				if (userRole instanceof SystemAdminUserRole) {
					fireEvent(new OpenPanelEvent(this, PanelActionType.Navigator,
							getUserRepository().findUsers(), userRole.getClass(), null,
							WorkflowDisposition.NewFlow));
				} else {
					fireEvent(new OpenPanelEvent(this, PanelActionType.Navigator, user, userRole
							.getClass(), null, WorkflowDisposition.NewFlow));
				}
			}
			fireEvent(new OpenPanelEvent(this, PanelActionType.Navigator, null, null,
					NLPPanelNames.NLP_NAVIGATOR_PANEL_NAME, WorkflowDisposition.NewFlow));
		}
	}

	protected UserRepository getUserRepository() {
		return userRepository;
	}
}
