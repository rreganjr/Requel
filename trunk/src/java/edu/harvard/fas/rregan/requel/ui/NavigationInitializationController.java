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
package edu.harvard.fas.rregan.requel.ui;

import nextapp.echo2.app.event.ActionEvent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.nlp.ui.NLPPanelNames;
import edu.harvard.fas.rregan.requel.user.SystemAdminUserRole;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.UserRepository;
import edu.harvard.fas.rregan.requel.user.UserRole;
import edu.harvard.fas.rregan.uiframework.login.InitAppEvent;
import edu.harvard.fas.rregan.uiframework.navigation.WorkflowDisposition;
import edu.harvard.fas.rregan.uiframework.navigation.event.OpenPanelEvent;
import edu.harvard.fas.rregan.uiframework.panel.PanelActionType;

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
