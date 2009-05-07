/*
 * $Id: AddGoalToGoalContainerController.java,v 1.3 2008/12/13 00:41:07 rregan Exp $
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
import edu.harvard.fas.rregan.requel.project.Goal;
import edu.harvard.fas.rregan.requel.project.GoalContainer;
import edu.harvard.fas.rregan.requel.project.command.AddGoalToGoalContainerCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.ui.AbstractRequelCommandController;
import edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher;
import edu.harvard.fas.rregan.uiframework.navigation.event.SelectEntityEvent;
import edu.harvard.fas.rregan.uiframework.navigation.event.UpdateEntityEvent;

/**
 * This controller is to be used in a goal container entity editor where the
 * editor contains a GoalsTable and allows adding existing goals to the entity.
 * 
 * @author ron
 */
public class AddGoalToGoalContainerController extends AbstractRequelCommandController {
	static final long serialVersionUID = 0;

	private final GoalContainer goalContainer;

	/**
	 * @param eventDispatcher
	 * @param commandFactory
	 * @param commandHandler
	 * @param goalContainer
	 */
	public AddGoalToGoalContainerController(EventDispatcher eventDispatcher,
			ProjectCommandFactory commandFactory, CommandHandler commandHandler,
			GoalContainer goalContainer) {
		super(eventDispatcher, commandFactory, commandHandler);
		this.goalContainer = goalContainer;
	}

	/**
	 * add the goal to the container via an AddGoalToGoalContainerCommand
	 */
	@Override
	public void actionPerformed(ActionEvent event) {
		if (event instanceof SelectEntityEvent) {
			SelectEntityEvent selectEntityEvent = (SelectEntityEvent) event;
			// the destination of the event should be the object that created
			// this
			// controller. how can we check?
			if (selectEntityEvent.getObject() instanceof Goal) {
				Goal goal = (Goal) selectEntityEvent.getObject();
				try {
					ProjectCommandFactory factory = getCommandFactory();
					AddGoalToGoalContainerCommand command = factory
							.newAddGoalToGoalContainerCommand();
					command.setEditedBy(null); // TODO: need user?
					command.setGoal(goal);
					command.setGoalContainer(goalContainer);
					command = getCommandHandler().execute(command);
					fireEvent(new UpdateEntityEvent(this, null, command.getGoal()));
					// TODO: this may not be needed because the update listeners
					// for things that
					// are goal containers will probably pickup the previous
					// event and get the
					// updated goal container from the goal.
					fireEvent(new UpdateEntityEvent(this, null, command.getGoalContainer()));
				} catch (Exception e) {
					// TODO: what can fail?
					log.error(e, e);
				}
			}
		}
	}
}