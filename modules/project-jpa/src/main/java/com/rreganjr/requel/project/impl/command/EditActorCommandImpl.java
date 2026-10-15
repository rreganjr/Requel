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
import com.rreganjr.platform.exception.EntityException;
import com.rreganjr.platform.exception.EntityExceptionActionType;
import com.rreganjr.platform.exception.NoSuchEntityException;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.ActorContainer;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ProjectScopedCommand;
import com.rreganjr.requel.project.command.AnalysisRequestSource;
import com.rreganjr.requel.project.command.EditActorCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.ActorImpl;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.user.UserRepository;

/**
 * @author ron
 */
@Controller("editActorCommand")
@Scope("prototype")
public class EditActorCommandImpl extends AbstractEditProjectOrDomainEntityCommand implements
		EditActorCommand, AuthorizableCommand, ProjectScopedCommand, AnalysisRequestSource {

	private Set<ActorContainer> actorContainers;
	private Set<ActorContainer> addActorContainers;
	private Actor actor;
	private String text;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 * @param projectCommandFactory
	 * @param annotationCommandFactory
	 * @param commandHandler
	 */
	@Autowired
	public EditActorCommandImpl(AssistantFacade assistantManager, UserRepository userRepository,
			ProjectRepository projectRepository, ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	public Actor getActor() {
		return actor;
	}

	public void setActor(Actor actor) {
		this.actor = actor;
	}

	public void setActorContainer(ActorContainer actorContainer) {
		if (actorContainer != null) {
			Set<ActorContainer> actorContainers = new HashSet<ActorContainer>();
			actorContainers.add(actorContainer);
			setActorContainers(actorContainers);
		}
	}

	@Override
	public void setActorContainers(Set<ActorContainer> actorContainers) {
		this.actorContainers = actorContainers;
	}

	protected Set<ActorContainer> getActorContainers() {
		return actorContainers;
	}

	protected Set<ActorContainer> getAddActorContainers() {
		return addActorContainers;
	}

	@Override
	public void setAddActorContainers(Set<ActorContainer> addActorContainers) {
		this.addActorContainers = addActorContainers;
	}

	@Override
	public void setText(String text) {
		this.text = text;
	}

	protected String getText() {
		return text;
	}

	@Override
	public void execute() {
		User editedBy = getProjectRepository().get(getEditedBy());
		Set<ActorContainer> containers = null;
		ActorImpl actorImpl = (ActorImpl) getActor();
		ProjectOrDomain projectOrDomain = null;
		if (getProjectOrDomain() != null) {
			projectOrDomain = getProjectOrDomain();
		} else if ((getActorContainers() != null) && (getActorContainers().size() > 0)) {
			containers = getActorContainers();
			Object container = getActorContainers().iterator().next();
			if (container instanceof ProjectOrDomain) {
				projectOrDomain = (ProjectOrDomain) container;
			} else if (container instanceof ProjectOrDomainEntity) {
				projectOrDomain = ((ProjectOrDomainEntity) container).getProjectOrDomain();
			} else {
				// TODO: throw an exception?
			}
		} else if ((getAddActorContainers() != null) && (getAddActorContainers().size() > 0)) {
			containers = getAddActorContainers();
			Object container = getAddActorContainers().iterator().next();
			if (container instanceof ProjectOrDomain) {
				projectOrDomain = (ProjectOrDomain) container;
			} else if (container instanceof ProjectOrDomainEntity) {
				projectOrDomain = ((ProjectOrDomainEntity) container).getProjectOrDomain();
			} else {
				// TODO: throw an exception?
			}
		} else if (getActor() != null) {
			projectOrDomain = getActor().getProjectOrDomain();
		}
		projectOrDomain = getRepository().get(projectOrDomain);

		// check for uniqueness
		try {
			Actor existing = getProjectRepository().findActorByProjectOrDomainAndName(
					projectOrDomain, getName());
			if (actorImpl == null) {
				throw EntityException.uniquenessConflict(Actor.class, existing, FIELD_NAME,
						EntityExceptionActionType.Creating);
			} else if (!existing.equals(actorImpl)) {
				throw EntityException.uniquenessConflict(Actor.class, existing, FIELD_NAME,
						EntityExceptionActionType.Updating);
			}
		} catch (NoSuchEntityException e) {
		}

		// Enforce the caller-supplied optimistic-lock version on update (issue #108).
		actorImpl = checkExpectedVersion(actorImpl);

		if (actorImpl == null) {
			actorImpl = getProjectRepository().persist(
					new ActorImpl(projectOrDomain, editedBy, getName(), ""));
			projectOrDomain.getActors().add(actorImpl);
		} else if (getName() != null) {
			actorImpl.setName(getName());
		}

		if (actorImpl == null) {
			actorImpl = getProjectRepository().persist(
					new ActorImpl(projectOrDomain, editedBy, getName(), getText()));
		} else {
			if (getName() != null) {
				actorImpl.setName(getName());
			}
			if (getText() != null) {
				actorImpl.setText(getText());
			}
		}

		actorImpl.getReferers().add(projectOrDomain);
		projectOrDomain.getActors().add(actorImpl);

		if (containers != null) {
			// TODO: equals on a set seems weird
			if (containers.equals(getActorContainers())) {
				actorImpl.getReferers().clear();
			}
			for (ActorContainer container : containers) {
				container = getRepository().get(container);
				actorImpl.getReferers().add(container);
			}
		}
		setActor(getProjectRepository().merge(actorImpl));

		if (containers != null) {
			// TODO: equals on a set seems weird
			if (containers.equals(getActorContainers())) {
				actorImpl.getReferers().clear();
			}
			for (ActorContainer container : containers) {
				container = getRepository().get(container);
				container.getActors().add(actorImpl);
			}
		}
	}

	/**
	 * Legacy fallback. The Actor path is migrated to the assistant SPI (issue #43):
	 * the command-handler layer detects {@link AnalysisRequestSource} and dispatches
	 * through {@code AnalysisRequestDispatcher}, so this method is normally not
	 * invoked for actors. It is retained as a safe fallback.
	 */
	@Override
	public void invokeAnalysis() {
		if (isAnalysisEnabled()) {
			getAssistantManager().analyzeActor(getRepository().get(getActor()));
		}
	}

	@Override
	public ProjectOrDomainEntity getAnalysisTarget() {
		return getActor();
	}

	@Override
	public User getAnalysisTriggeredBy() {
		return getEditedBy();
	}

	@Override
	public Project getProject() {
		// The registrar always sets projectOrDomain for actors; fall back to containers/entity
		if (getProjectOrDomain() instanceof Project project) return project;
		if (actorContainers != null && !actorContainers.isEmpty()) {
			Object first = actorContainers.iterator().next();
			if (first instanceof Project project) return project;
		}
		if (actor != null && actor.getProjectOrDomain() instanceof Project project) return project;
		return null;
	}

	@Override
	public AuthorizationRequirement getAuthorizationRequirement() {
		return new RequiresStakeholderPermission(Actor.class, "Edit");
	}
}
