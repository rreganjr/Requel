/*
 * $Id: CopyStoryCommandImpl.java,v 1.6 2009/03/30 11:54:26 rregan Exp $
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
import edu.harvard.fas.rregan.requel.project.Story;
import edu.harvard.fas.rregan.requel.project.command.CopyStoryCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.impl.StoryImpl;
import edu.harvard.fas.rregan.requel.project.impl.assistant.AssistantFacade;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.UserRepository;

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
