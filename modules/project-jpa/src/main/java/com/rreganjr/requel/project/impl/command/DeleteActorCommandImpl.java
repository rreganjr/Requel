/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.project.impl.command;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.command.AuthorizableCommand;
import com.rreganjr.platform.command.AuthorizationRequirement;
import com.rreganjr.platform.command.AuthorizationRequirement.RequiresStakeholderPermission;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.NoSuchPositionException;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.DeletePositionCommand;
import com.rreganjr.requel.annotation.command.RemoveAnnotationFromAnnotatableCommand;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.ActorContainer;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ProjectScopedCommand;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.command.DeleteActorCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.command.RemoveGoalFromGoalContainerCommand;
import com.rreganjr.requel.project.command.RemoveActorFromActorContainerCommand;
import com.rreganjr.requel.project.impl.AddActorPosition;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.user.UserRepository;

/**
 * Delete a actor from a project, cleaning up references from other project
 * entities, actor relations and annotations.
 * 
 * @author ron
 */
@Controller("deleteActorCommand")
@Scope("prototype")
public class DeleteActorCommandImpl extends AbstractEditProjectCommand implements
		DeleteActorCommand, AuthorizableCommand, ProjectScopedCommand {

	private Actor actor;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 * @param projectCommandFactory
	 * @param annotationCommandFactory
	 * @param commandHandler
	 */
	@Autowired
	public DeleteActorCommandImpl(AssistantFacade assistantManager, UserRepository userRepository,
			ProjectRepository projectRepository, ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	@Override
	public void setActor(Actor actor) {
		this.actor = actor;
	}

	protected Actor getActor() {
		return actor;
	}

	@Override
	public void execute() throws Exception {
		Actor actor = getRepository().get(getActor());
		User editedBy = getRepository().get(getEditedBy());

		// #247: the former "proactive" native DELETE FROM annotation_annotatable WHERE
		// annotatable_id = ? that lived here was removed. It full-scanned and X-locked the
		// whole join table (no index on annotatable_id; two concurrent actor deletes
		// deadlocked each other in the e2e suite) and, lacking annotatable_type, also unlinked
		// every other entity type sharing the actor's numeric id. The typed, index-backed
		// sweep now runs in removeAllAnnotationsBeforeDelete() right before delete(actor).

		Set<Annotation> annotations = new HashSet<Annotation>(actor.getAnnotations());
		for (Annotation annotation : annotations) {
			RemoveAnnotationFromAnnotatableCommand removeAnnotationFromAnnotatableCommand = getAnnotationCommandFactory()
					.newRemoveAnnotationFromAnnotatableCommand();
			removeAnnotationFromAnnotatableCommand.setEditedBy(editedBy);
			removeAnnotationFromAnnotatableCommand.setAnnotatable(actor);
			removeAnnotationFromAnnotatableCommand.setAnnotation(annotation);
			getCommandHandler().execute(removeAnnotationFromAnnotatableCommand);
		}
		// remove this entity as a referer to any terms
		for (GlossaryTerm term : actor.getProjectOrDomain().getGlossaryTerms()) {
			if (term.getReferers().contains(actor)) {
				term.getReferers().remove(actor);
			}
		}
		// #247: an actor is a GoalContainer (actor_goals). Detach its goals first, or each goal's
		// @ManyToAny referers (goals_goalcontainers, no DB FK) keeps pointing at the deleted
		// actor and a later DeleteGoal merge()s a removed container.
		Set<Goal> heldGoals = new HashSet<Goal>(actor.getGoals());
		for (Goal goal : heldGoals) {
			RemoveGoalFromGoalContainerCommand removeGoalFromGoalContainerCommand = getProjectCommandFactory()
					.newRemoveGoalFromGoalContainerCommand();
			removeGoalFromGoalContainerCommand.setEditedBy(editedBy);
			((com.rreganjr.platform.command.AuthorizationExemptable) removeGoalFromGoalContainerCommand).setAuthorizationExempt(true);
			removeGoalFromGoalContainerCommand.setGoal(goal);
			removeGoalFromGoalContainerCommand.setGoalContainer(actor);
			getCommandHandler().execute(removeGoalFromGoalContainerCommand);
		}
		Set<ActorContainer> actorReferers = new HashSet<ActorContainer>(actor.getReferers());
		for (ActorContainer actorContainer : actorReferers) {
			RemoveActorFromActorContainerCommand removeActorFromActorContainerCommand = getProjectCommandFactory()
					.newRemoveActorFromActorContainerCommand();
			removeActorFromActorContainerCommand.setEditedBy(editedBy);
			// TODO(#75): part of an authorized delete; exempt the detach sub-command from
			// re-auth (see https://github.com/rreganjr/Requel/issues/75)
			((com.rreganjr.platform.command.AuthorizationExemptable) removeActorFromActorContainerCommand).setAuthorizationExempt(true);
			removeActorFromActorContainerCommand.setActor(actor);
			removeActorFromActorContainerCommand.setActorContainer(actorContainer);
			getCommandHandler().execute(removeActorFromActorContainerCommand);
			if (actorContainer instanceof UseCase) {
				UseCase useCase = (UseCase) actorContainer;
				if (useCase.getPrimaryActor().equals(actor)) {
					throw new RuntimeException("The actor \"" + actor.getDescription()
							+ "\" is the primary actor for \"" + useCase.getDescription()
							+ "\" and cannot be deleted unless the use case is deleted first.");
				}
			}
		}
		try {
			AddActorPosition actorPosition = getProjectRepository().findAddActorPosition(
					actor.getProjectOrDomain(), actor.getName());
			DeletePositionCommand deletePositionCommand = getAnnotationCommandFactory()
					.newDeletePositionCommand();
			deletePositionCommand.setEditedBy(getEditedBy());
			deletePositionCommand.setPosition(actorPosition);
			// #69/#75: this DeletePosition is an intrinsic sub-step of deleting the parent
			// entity; exempt it so a Delete-only stakeholder isn't re-checked for Annotation[Delete].
			((com.rreganjr.platform.command.AuthorizationExemptable) deletePositionCommand)
					.setAuthorizationExempt(true);
			getCommandHandler().execute(deletePositionCommand);
		} catch (NoSuchPositionException e) {
		}

		// Ensure all relationship rows are flushed before deleting the actor so FK
		// constraints on annotation_annotatable (and others) don't block the delete.
		getRepository().flush();

		actor.getProjectOrDomain().getActors().remove(actor);
		// #247: clear any annotation link committed since this entity was loaded.
		removeAllAnnotationsBeforeDelete(actor, editedBy);
		getRepository().delete(actor);
	}

	@Override
	public Project getProject() {
		if (actor != null && actor.getProjectOrDomain() instanceof Project project) return project;
		return null;
	}

	@Override
	public AuthorizationRequirement getAuthorizationRequirement() {
		return new RequiresStakeholderPermission(Actor.class, "Delete");
	}
}
