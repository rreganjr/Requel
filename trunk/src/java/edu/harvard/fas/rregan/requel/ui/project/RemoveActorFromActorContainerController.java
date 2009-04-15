/*
 * $Id: RemoveActorFromActorContainerController.java,v 1.2 2008/12/13 00:41:06 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.ui.project;

import nextapp.echo2.app.event.ActionEvent;
import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.project.ActorContainer;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.command.RemoveActorFromActorContainerCommand;
import edu.harvard.fas.rregan.requel.ui.AbstractRequelCommandController;
import edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher;
import edu.harvard.fas.rregan.uiframework.navigation.event.UpdateEntityEvent;

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
