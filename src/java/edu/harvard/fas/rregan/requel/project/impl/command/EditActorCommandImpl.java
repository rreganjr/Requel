/*
 * $Id: EditActorCommandImpl.java,v 1.13 2009/03/30 11:54:30 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project.impl.command;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.repository.EntityException;
import edu.harvard.fas.rregan.repository.EntityExceptionActionType;
import edu.harvard.fas.rregan.requel.NoSuchEntityException;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.project.Actor;
import edu.harvard.fas.rregan.requel.project.ActorContainer;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.command.EditActorCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.impl.ActorImpl;
import edu.harvard.fas.rregan.requel.project.impl.assistant.AssistantFacade;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.UserRepository;

/**
 * @author ron
 */
@Controller("editActorCommand")
@Scope("prototype")
public class EditActorCommandImpl extends AbstractEditProjectOrDomainEntityCommand implements
		EditActorCommand {

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

	@Override
	public void invokeAnalysis() {
		if (isAnalysisEnabled()) {
			// TODO: because getActor() is being called locally in the
			// command, the DomainObjectWrappingAdvice that wraps persistence
			// objects with a DomainObjectWrapper isn't getting invoked.
			// Reloading the object through a repository solves the problem, but
			// using aspectj may be better.
			getAssistantManager().analyzeActor(getRepository().get(getActor()));
		}
	}
}
