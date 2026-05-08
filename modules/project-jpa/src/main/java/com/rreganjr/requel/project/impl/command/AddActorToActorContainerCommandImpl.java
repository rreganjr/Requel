/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025, 2026 Ron Regan Jr. All Rights Reserved.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.ActorContainer;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ProjectScopedCommand;
import com.rreganjr.requel.project.command.AddActorToActorContainerCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.GoalImpl;
import com.rreganjr.requel.project.impl.ProjectImpl;
import com.rreganjr.requel.project.impl.StoryImpl;
import com.rreganjr.requel.project.impl.UseCaseImpl;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.project.impl.repository.jpa.JpaProjectRepository;
import com.rreganjr.requel.user.UserRepository;

/**
 * @author ron
 */
@Controller("addActorToActorContainerCommand")
@Scope("prototype")
public class AddActorToActorContainerCommandImpl extends AbstractEditProjectCommand implements
		AddActorToActorContainerCommand, ProjectScopedCommand {

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 */
	@Autowired
	public AddActorToActorContainerCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	private Actor actor;
	private ActorContainer actorContainer;

	@Override
	public Actor getActor() {
		return actor;
	}

	@Override
	public void setActor(Actor actor) {
		this.actor = actor;
	}

	@Override
	public ActorContainer getActorContainer() {
		return actorContainer;
	}

	@Override
	public void setActorContainer(ActorContainer actorContainer) {
		this.actorContainer = actorContainer;
	}

	@Override
	public void execute() {
		Actor addedActor = getProjectRepository().get(getActor());
		ActorContainer addingContainer = getProjectRepository().get(getActorContainer());

		// Hibernate 6.5 bug: @ManyToAny collection insertion generates invalid SQL for the
		// actor_actorcontainers join table. Use a native INSERT instead.
		JpaProjectRepository jpaRepo = (JpaProjectRepository) getProjectRepository();
		jakarta.persistence.PersistenceUnitUtil puu = jpaRepo.getEntityManager()
				.getEntityManagerFactory().getPersistenceUnitUtil();
		Long actorId = (Long) puu.getIdentifier(addedActor);
		Long containerId = (Long) puu.getIdentifier(addingContainer);
		jpaRepo.addActorContainerToActorJoinTable(actorId, containerId, actorContainerDiscriminator(addingContainer));
		jpaRepo.getEntityManager().refresh(addedActor);

		addingContainer.getActors().add(addedActor);
		addingContainer = getRepository().merge(addingContainer);
		setActor(addedActor);
		setActorContainer(addingContainer);
	}

	@Override
	public Project getProject() {
		// ActorContainer can be a Project (most common), or UseCase/Goal/Story —
		// the latter three are ProjectOrDomainEntity, so walk up via getProjectOrDomain().
		// Either path lets the SSE broadcaster recognise this command as
		// project-scoped and refresh the sidebar.
		if (actorContainer instanceof Project project) return project;
		if (actorContainer instanceof ProjectOrDomainEntity pode
				&& pode.getProjectOrDomain() instanceof Project project) return project;
		if (actor != null && actor.getProjectOrDomain() instanceof Project project) return project;
		return null;
	}

	private static String actorContainerDiscriminator(ActorContainer container) {
		if (container instanceof ProjectImpl)  return "com.rreganjr.requel.project.Project";
		if (container instanceof UseCaseImpl)  return "com.rreganjr.requel.project.UseCase";
		if (container instanceof GoalImpl)     return "com.rreganjr.requel.project.Goal";
		if (container instanceof StoryImpl)    return "com.rreganjr.requel.project.Story";
		throw new IllegalStateException("Unknown ActorContainer type: " + container.getClass().getName());
	}
}
