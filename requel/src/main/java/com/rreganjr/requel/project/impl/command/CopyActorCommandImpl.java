/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirements
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
package com.rreganjr.requel.project.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.EntityException;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.command.CopyActorCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.ActorImpl;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;

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
