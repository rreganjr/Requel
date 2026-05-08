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
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.GoalContainer;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ProjectScopedCommand;
import com.rreganjr.requel.project.command.AddGoalToGoalContainerCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.ActorImpl;
import com.rreganjr.requel.project.impl.NonUserStakeholderImpl;
import com.rreganjr.requel.project.impl.ProjectImpl;
import com.rreganjr.requel.project.impl.ScenarioImpl;
import com.rreganjr.requel.project.impl.StoryImpl;
import com.rreganjr.requel.project.impl.UseCaseImpl;
import com.rreganjr.requel.project.impl.UserStakeholderImpl;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.project.impl.repository.jpa.JpaProjectRepository;
import com.rreganjr.requel.user.UserRepository;

/**
 * @author ron
 */
@Controller("addGoalToGoalContainerCommand")
@Scope("prototype")
public class AddGoalToGoalContainerCommandImpl extends AbstractEditProjectCommand implements
		AddGoalToGoalContainerCommand, ProjectScopedCommand {

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 */
	@Autowired
	public AddGoalToGoalContainerCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	private Goal goal;
	private GoalContainer goalContainer;

	@Override
	public Goal getGoal() {
		return goal;
	}

	@Override
	public void setGoal(Goal goal) {
		this.goal = goal;
	}

	@Override
	public GoalContainer getGoalContainer() {
		return goalContainer;
	}

	@Override
	public void setGoalContainer(GoalContainer goalContainer) {
		this.goalContainer = goalContainer;
	}

	@Override
	public void execute() {
		Goal addedGoal = getProjectRepository().get(getGoal());
		GoalContainer addingContainer = getProjectRepository().get(getGoalContainer());

		// Hibernate 6.5 bug: @ManyToAny collection insertion generates invalid SQL for the
		// goals_goalcontainers join table. Use a native INSERT instead of
		// addedGoal.getReferers().add(addingContainer).
		// Then refresh so the referers collection is reloaded on the managed instance.
		JpaProjectRepository jpaRepo = (JpaProjectRepository) getProjectRepository();
		jakarta.persistence.PersistenceUnitUtil puu = jpaRepo.getEntityManager()
				.getEntityManagerFactory().getPersistenceUnitUtil();
		Long goalId = (Long) puu.getIdentifier(addedGoal);
		Long containerId = (Long) puu.getIdentifier(addingContainer);
		String containerType = goalContainerDiscriminator(addingContainer);
		jpaRepo.addGoalContainerToGoalJoinTable(goalId, containerId, containerType);
		jpaRepo.getEntityManager().refresh(addedGoal);

		addingContainer.getGoals().add(addedGoal);
		addingContainer = getRepository().merge(addingContainer);
		setGoal(addedGoal);
		setGoalContainer(addingContainer);
	}

	@Override
	public Project getProject() {
		// GoalContainer can be Project, or any of UseCase/Scenario/Story/Actor/
		// NonUserStakeholder — the latter all extend ProjectOrDomainEntity.
		// Walk up to the Project either way so the SSE broadcaster recognises
		// this command as project-scoped and the sidebar refreshes counts.
		if (goalContainer instanceof Project project) return project;
		if (goalContainer instanceof ProjectOrDomainEntity pode
				&& pode.getProjectOrDomain() instanceof Project project) return project;
		if (goal != null && goal.getProjectOrDomain() instanceof Project project) return project;
		return null;
	}

	/**
	 * Returns the @AnyDiscriminatorValue string for the given GoalContainer instance,
	 * matching the discriminator values declared on GoalImpl.getReferers().
	 */
	private static String goalContainerDiscriminator(GoalContainer container) {
		if (container instanceof ProjectImpl)            return "com.rreganjr.requel.project.Project";
		if (container instanceof UseCaseImpl)            return "com.rreganjr.requel.project.UseCase";
		if (container instanceof ScenarioImpl)           return "com.rreganjr.requel.project.Scenario";
		if (container instanceof StoryImpl)              return "com.rreganjr.requel.project.Story";
		if (container instanceof ActorImpl)              return "com.rreganjr.requel.project.Actor";
		if (container instanceof NonUserStakeholderImpl) return "com.rreganjr.requel.project.NonUserStakeholder";
		if (container instanceof UserStakeholderImpl)    return "com.rreganjr.requel.project.UserStakeholder";
		throw new IllegalStateException("Unknown GoalContainer type: " + container.getClass().getName());
	}
}
