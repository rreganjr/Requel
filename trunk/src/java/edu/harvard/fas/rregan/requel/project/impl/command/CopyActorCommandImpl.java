/*
 * $Id: CopyActorCommandImpl.java,v 1.7 2009/03/30 11:54:28 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.repository.EntityException;
import edu.harvard.fas.rregan.requel.annotation.Annotation;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.project.Actor;
import edu.harvard.fas.rregan.requel.project.GlossaryTerm;
import edu.harvard.fas.rregan.requel.project.Goal;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.command.CopyActorCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.impl.ActorImpl;
import edu.harvard.fas.rregan.requel.project.impl.assistant.AssistantFacade;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.UserRepository;

/**
 * @author ron
 */
@Controller("copyActorCommand")
@Scope("prototype")
public class CopyActorCommandImpl extends AbstractEditProjectCommand implements CopyActorCommand {

	private Actor originalActor;
	private Actor newActor;
	private String newActorName;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 * @param projectCommandFactory
	 * @param annotationCommandFactory
	 * @param commandHandler
	 */
	@Autowired
	public CopyActorCommandImpl(AssistantFacade assistantManager, UserRepository userRepository,
			ProjectRepository projectRepository, ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	protected Actor getOriginalActor() {
		return originalActor;
	}

	public void setOriginalActor(Actor originalActor) {
		this.originalActor = originalActor;
	}

	protected String getNewActorName() {
		return newActorName;
	}

	public void setNewActorName(String newActorName) {
		this.newActorName = newActorName;
	}

	@Override
	public Actor getNewActor() {
		return newActor;
	}

	protected void setNewActor(Actor newActor) {
		this.newActor = newActor;
	}

	@Override
	public void execute() {
		User editedBy = getRepository().get(getEditedBy());
		ActorImpl originalActor = (ActorImpl) getRepository().get(getOriginalActor());

		String newName;
		if ((getNewActorName() == null) || (getNewActorName().length() == 0)) {
			newName = generateNewActorName(originalActor.getName());
		} else {
			newName = generateNewActorName(getNewActorName());
		}
		ActorImpl newActor = getProjectRepository().persist(
				new ActorImpl(originalActor.getProjectOrDomain(), editedBy, newName, originalActor
						.getText()));
		for (Goal goal : originalActor.getGoals()) {
			newActor.getGoals().add(goal);
			goal.getReferers().add(newActor);
		}
		// TODO: what other references should be added?

		// TODO: this assumes that all annotations are appropriate for the new
		// actor
		for (Annotation annotation : originalActor.getAnnotations()) {
			newActor.getAnnotations().add(annotation);
			annotation.getAnnotatables().add(newActor);
		}
		for (GlossaryTerm term : originalActor.getGlossaryTerms()) {
			newActor.getGlossaryTerms().add(term);
			term.getReferers().add(newActor);
		}
		newActor = getProjectRepository().merge(newActor);
		setNewActor(newActor);
	}

	private String generateNewActorName(String originalName) {
		String newName = originalName;
		try {
			getProjectRepository().findActorByProjectOrDomainAndName(
					getOriginalActor().getProjectOrDomain(), newName);
			int counter = 1;
			newName = originalName + " " + counter;
			while (true) {
				try {
					getProjectRepository().findActorByProjectOrDomainAndName(
							getOriginalActor().getProjectOrDomain(), newName);
					counter++;
					newName = originalName + " " + counter;
				} catch (EntityException e) {
					// new name is not in use
					break;
				}
			}
		} catch (EntityException e) {
			// new name is not in use
		}
		return newName;
	}
}
