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
package edu.harvard.fas.rregan.requel.ui.project;

import nextapp.echo2.app.event.ActionEvent;
import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.project.GoalContainer;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.command.RemoveGoalFromGoalContainerCommand;
import edu.harvard.fas.rregan.requel.ui.AbstractRequelCommandController;
import net.sf.echopm.navigation.event.EventDispatcher;
import net.sf.echopm.navigation.event.UpdateEntityEvent;

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
