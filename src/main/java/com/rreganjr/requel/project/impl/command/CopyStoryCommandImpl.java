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
import com.rreganjr.repository.EntityException;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.command.CopyStoryCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.StoryImpl;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;

/**
 * @author ron
 */
@Controller("copyStoryCommand")
@Scope("prototype")
public class CopyStoryCommandImpl extends AbstractEditProjectCommand implements CopyStoryCommand {

	private Story originalStory;
	private Story newStory;
	private String newStoryName;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 */
	@Autowired
	public CopyStoryCommandImpl(AssistantFacade assistantManager, UserRepository userRepository,
			ProjectRepository projectRepository, ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	protected Story getOriginalStory() {
		return originalStory;
	}

	public void setOriginalStory(Story originalStory) {
		this.originalStory = originalStory;
	}

	protected String getNewStoryName() {
		return newStoryName;
	}

	public void setNewStoryName(String newStoryName) {
		this.newStoryName = newStoryName;
	}

	@Override
	public Story getNewStory() {
		return newStory;
	}

	protected void setNewStory(Story newStory) {
		this.newStory = newStory;
	}

	@Override
	public void execute() {
		User editedBy = getRepository().get(getEditedBy());
		StoryImpl originalStory = (StoryImpl) getRepository().get(getOriginalStory());

		String newName;
		if ((getNewStoryName() == null) || (getNewStoryName().length() == 0)) {
			newName = generateNewStoryName(originalStory.getName());
		} else {
			newName = generateNewStoryName(getNewStoryName());
		}
		StoryImpl newStory = getProjectRepository().persist(
				new StoryImpl(originalStory.getProjectOrDomain(), editedBy, newName, originalStory
						.getText(), originalStory.getStoryType()));
		for (Actor actor : originalStory.getActors()) {
			newStory.getActors().add(actor);
			actor.getReferers().add(newStory);
		}
		for (Goal goal : originalStory.getGoals()) {
			newStory.getGoals().add(goal);
			goal.getReferers().add(newStory);
		}
		// TODO: this assumes that all annotations are appropriate for the new
		// story
		for (Annotation annotation : originalStory.getAnnotations()) {
			newStory.getAnnotations().add(annotation);
			annotation.getAnnotatables().add(newStory);
		}
		for (GlossaryTerm term : originalStory.getGlossaryTerms()) {
			newStory.getGlossaryTerms().add(term);
			term.getReferers().add(newStory);
		}
		newStory = getProjectRepository().merge(newStory);
		setNewStory(newStory);
	}

	private String generateNewStoryName(String originalName) {
		String newName = originalName;
		try {
			getProjectRepository().findStoryByProjectOrDomainAndName(
					getOriginalStory().getProjectOrDomain(), newName);
			int counter = 1;
			newName = originalName + " " + counter;
			while (true) {
				try {
					getProjectRepository().findStoryByProjectOrDomainAndName(
							getOriginalStory().getProjectOrDomain(), newName);
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
