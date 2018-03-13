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
package com.rreganjr.requel.ui.project;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.impl.assistant.UpdatedEntityNotifier;
import net.sf.echopm.navigation.event.EventDispatcher;

/**
 * An updated entity notifier that will send UpdateEvents through the UI
 * EventDispatcher to notify UI components that a project entity has been
 * updated.
 * 
 * @author ron
 */
@Component("updatedEntityNotifier")
@Scope("prototype")
public class RequelUpdatedEntityNotifier implements UpdatedEntityNotifier {
	private static final Log log = LogFactory.getLog(RequelUpdatedEntityNotifier.class);

	private final EventDispatcher eventDispatcher;

	// TODO: the event dispatcher is a session context object and can't be
	// assigned to a prototype object.
	// this is a prototype so that the assistant manager can be a prototype
	// because of an exception commented
	// on at the top of the class:
	// com.rreganjr.requel.project.impl.assistant.AssistantManager
	/**
	 * Create a new updated entity notifier with the event dispatcher for the
	 * current user session.
	 * 
	 * @param eventDispatcher
	 */
	// @Autowired
	// public RequelUpdatedEntityNotifier(EventDispatcher eventDispatcher) {
	// this.eventDispatcher = eventDispatcher;
	// }
	public RequelUpdatedEntityNotifier() {
		eventDispatcher = null;
	}

	/**
	 * @see com.rreganjr.requel.project.impl.assistant.UpdatedEntityNotifier#entityUpdated(com.rreganjr.requel.project.ProjectOrDomain)
	 */
	@Override
	public void entityUpdated(ProjectOrDomain pod) {
		// TODO: this happens on a separate thread when when the
		// ApplicationInstance activeInstance
		// is null causing an IllegalStateException "Attempt to update state of
		// application user
		// interface outside of user interface thread."
		// eventDispatcher.dispatchEvent(new UpdateEntityEvent(this, null,
		// pod));
		log.info("updated " + pod);
	}

	/**
	 * @see com.rreganjr.requel.project.impl.assistant.UpdatedEntityNotifier#entityUpdated(com.rreganjr.requel.project.ProjectOrDomainEntity)
	 */
	@Override
	public void entityUpdated(ProjectOrDomainEntity entity) {
		// TODO: this happens on a separate thread when when the
		// ApplicationInstance activeInstance
		// is null causing an IllegalStateException "Attempt to update state of
		// application user
		// interface outside of user interface thread."
		// eventDispatcher.dispatchEvent(new UpdateEntityEvent(this, null,
		// entity));
		log.info("updated " + entity);
	}

}
