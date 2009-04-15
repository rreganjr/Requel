/*
 * $Id: NavigationInitializationController.java,v 1.7 2008/12/16 23:03:17 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
					fireEvent(new OpenPanelEvent(this, PanelActionType.Navigator, getUserRepository()
							.findUsers(), userRole.getClass(), null, WorkflowDisposition.NewFlow));
				} else {
					fireEvent(new OpenPanelEvent(this, PanelActionType.Navigator, user, userRole
							.getClass(), null, WorkflowDisposition.NewFlow));
				}
			}
			fireEvent(new OpenPanelEvent(this, PanelActionType.Navigator, null, null, NLPPanelNames.NLP_NAVIGATOR_PANEL_NAME, WorkflowDisposition.NewFlow));
		}
	}

	protected UserRepository getUserRepository() {
		return userRepository;
	}
}
