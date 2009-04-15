/*
 * $Id: RemoveGoalFromGoalContainerController.java,v 1.2 2008/12/13 00:41:07 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.ui.project;

import nextapp.echo2.app.event.ActionEvent;
import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.project.GoalContainer;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.command.RemoveGoalFromGoalContainerCommand;
import edu.harvard.fas.rregan.requel.ui.AbstractRequelCommandController;
import edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher;
import edu.harvard.fas.rregan.uiframework.navigation.event.UpdateEntityEvent;

/**
 * This controller is to be used in a goal container entity editor where the
 * editor contains a GoalsTable and allows removing goals from the entity.
 * 
 * @author ron
 */
public class RemoveGoalFromGoalContainerController extends AbstractRequelCommandController {
	static final long serialVersionUID = 0;

	private final GoalContainer goalContainer;

	/**
	 * @param eventDispatcher
	 * @param commandFactory
	 * @param commandHandler
	 * @param goalContainer
	 */
	public RemoveGoalFromGoalContainerController(EventDispatcher eventDispatcher,
			ProjectCommandFactory commandFactory, CommandHandler commandHandler, 
			GoalContainer goalContainer) {
		super(eventDispatcher, commandFactory, commandHandler);
		this.goalContainer = goalContainer;
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		if (event instanceof RemoveGoalFromGoalContainerEvent) {
			RemoveGoalFromGoalContainerEvent removeEvent = (RemoveGoalFromGoalContainerEvent) event;
			if (goalContainer.equals(removeEvent.getGoalContainer())) {
				try {
					ProjectCommandFactory factory = getCommandFactory();
					RemoveGoalFromGoalContainerCommand command = factory
							.newRemoveGoalFromGoalContainerCommand();
					command.setEditedBy(null); // TODO: need user?
					command.setGoal(removeEvent.getGoal());
					command.setGoalContainer(goalContainer);
					command = getCommandHandler().execute(command);
					fireEvent(new UpdateEntityEvent(this, null, command.getGoal()));
					fireEvent(new UpdateEntityEvent(this, null, goalContainer));
				} catch (Exception e) {
					// TODO: what can fail?
					log.error(e, e);
				}
			}
		}
	}
}
