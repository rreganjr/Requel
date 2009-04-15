/*
 * $Id: RequelUpdatedEntityNotifier.java,v 1.2 2009/01/23 09:54:27 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.ui.project;

import org.apache.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity;
import edu.harvard.fas.rregan.requel.project.impl.assistant.UpdatedEntityNotifier;
import edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher;

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
	private static final Logger log = Logger.getLogger(RequelUpdatedEntityNotifier.class);

	private final EventDispatcher eventDispatcher;

	// TODO: the event dispatcher is a session context object and can't be
	// assigned to a prototype object.
	// this is a prototype so that the assistant manager can be a prototype
	// because of an exception commented
	// on at the top of the class:
	// edu.harvard.fas.rregan.requel.project.impl.assistant.AssistantManager
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
	 * @see edu.harvard.fas.rregan.requel.project.impl.assistant.UpdatedEntityNotifier#entityUpdated(edu.harvard.fas.rregan.requel.project.ProjectOrDomain)
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
	 * @see edu.harvard.fas.rregan.requel.project.impl.assistant.UpdatedEntityNotifier#entityUpdated(edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity)
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
