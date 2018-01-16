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

import nextapp.echo2.app.event.ActionEvent;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.project.ActorContainer;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.command.RemoveActorFromActorContainerCommand;
import com.rreganjr.requel.ui.AbstractRequelCommandController;
import net.sf.echopm.navigation.event.EventDispatcher;
import net.sf.echopm.navigation.event.UpdateEntityEvent;

/**
 * This controller is to be used in a actor container entity editor where the
 * editor contains a ActorsTable and allows removing actors from the entity.
 * 
 * @author ron
 */
public class RemoveActorFromActorContainerController extends AbstractRequelCommandController {
	static final long serialVersionUID = 0;

	private final ActorContainer actorContainer;

	/**
	 * @param eventDispatcher
	 * @param commandFactory
	 * @param commandHandler
	 * @param actorContainer
	 */
	public RemoveActorFromActorContainerController(EventDispatcher eventDispatcher,
			ProjectCommandFactory commandFactory, CommandHandler commandHandler,
			ActorContainer actorContainer) {
		super(eventDispatcher, commandFactory, commandHandler);
		this.actorContainer = actorContainer;
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		if (event instanceof RemoveActorFromActorContainerEvent) {
			RemoveActorFromActorContainerEvent removeEvent = (RemoveActorFromActorContainerEvent) event;
			if (actorContainer.equals(removeEvent.getActorContainer())) {
				try {
					ProjectCommandFactory factory = getCommandFactory();
					RemoveActorFromActorContainerCommand command = factory
							.newRemoveActorFromActorContainerCommand();
					command.setEditedBy(null); // TODO: need user?
					command.setActor(removeEvent.getActor());
					command.setActorContainer(actorContainer);
					command = getCommandHandler().execute(command);
					fireEvent(new UpdateEntityEvent(this, null, command.getActor()));
					fireEvent(new UpdateEntityEvent(this, null, actorContainer));
				} catch (Exception e) {
					// TODO: what can fail?
					log.error(e, e);
				}
			}
		}
	}
}
