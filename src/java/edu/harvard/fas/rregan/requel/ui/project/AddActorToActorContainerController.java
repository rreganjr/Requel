/*
 * $Id: AddActorToActorContainerController.java,v 1.3 2008/12/13 00:41:10 rregan Exp $
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
package edu.harvard.fas.rregan.requel.ui.project;

import nextapp.echo2.app.event.ActionEvent;
import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.project.Actor;
import edu.harvard.fas.rregan.requel.project.ActorContainer;
import edu.harvard.fas.rregan.requel.project.command.AddActorToActorContainerCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.ui.AbstractRequelCommandController;
import edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher;
import edu.harvard.fas.rregan.uiframework.navigation.event.SelectEntityEvent;
import edu.harvard.fas.rregan.uiframework.navigation.event.UpdateEntityEvent;

/**
 * This controller is to be used in a actor container entity editor where the
 * editor contains a ActorsTable and allows adding existing actors to the
 * entity.
 * 
 * @author ron
 */
public class AddActorToActorContainerController extends AbstractRequelCommandController {
	static final long serialVersionUID = 0;

	private final ActorContainer actorContainer;

	/**
	 * @param eventDispatcher
	 * @param commandFactory
	 * @param commandHandler
	 * @param actorContainer
	 */
	public AddActorToActorContainerController(EventDispatcher eventDispatcher,
			ProjectCommandFactory commandFactory, CommandHandler commandHandler,
			ActorContainer actorContainer) {
		super(eventDispatcher, commandFactory, commandHandler);
		this.actorContainer = actorContainer;
	}

	/**
	 * FIXME: there is a flaw in that the select entity event doesn't have an
	 * expected destination so any select entity event with a actor will trigger
	 * this controller and may produce unintended results.
	 */
	@Override
	public void actionPerformed(ActionEvent event) {
		if (event instanceof SelectEntityEvent) {
			SelectEntityEvent selectEntityEvent = (SelectEntityEvent) event;
			if (selectEntityEvent.getObject() instanceof Actor) {
				Actor actor = (Actor) selectEntityEvent.getObject();
				try {
					ProjectCommandFactory factory = getCommandFactory();
					AddActorToActorContainerCommand command = factory
							.newAddActorToActorContainerCommand();
					command.setEditedBy(null); // TODO: need user?
					command.setActor(actor);
					command.setActorContainer(actorContainer);
					command = getCommandHandler().execute(command);
					fireEvent(new UpdateEntityEvent(this, null, command.getActor()));
					// TODO: this may not be needed because the update listeners
					// for things that
					// are actor containers will probably pickup the previous
					// event and get the
					// updated actor container from the actor.
					fireEvent(new UpdateEntityEvent(this, null, command.getActorContainer()));
				} catch (Exception e) {
					// TODO: what can fail?
					log.error(e, e);
				}
			}
		}
	}
}