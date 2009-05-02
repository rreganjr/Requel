/*
 * $Id: ReplaceGlossaryTermCommandImpl.java,v 1.10 2009/03/30 11:54:28 rregan Exp $
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

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.project.GlossaryTerm;
import edu.harvard.fas.rregan.requel.project.Goal;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.Story;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.command.ReplaceGlossaryTermCommand;
import edu.harvard.fas.rregan.requel.project.impl.ActorImpl;
import edu.harvard.fas.rregan.requel.project.impl.GlossaryTermImpl;
import edu.harvard.fas.rregan.requel.project.impl.GoalImpl;
import edu.harvard.fas.rregan.requel.project.impl.StoryImpl;
import edu.harvard.fas.rregan.requel.project.impl.assistant.AssistantFacade;
import edu.harvard.fas.rregan.requel.user.UserRepository;

/**
 * @author ron
 */
@Controller("replaceGlossaryTermCommand")
@Scope("prototype")
public class ReplaceGlossaryTermCommandImpl extends AbstractEditProjectCommand implements
		ReplaceGlossaryTermCommand {

	private final Set<ProjectOrDomainEntity> updatedEntities = new HashSet<ProjectOrDomainEntity>();
	private GlossaryTerm glossaryTerm;
	private GlossaryTerm canonicalTerm;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 * @param projectCommandFactory
	 * @param annotationCommandFactory
	 * @param commandHandler
	 */
	@Autowired
	public ReplaceGlossaryTermCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	protected GlossaryTerm getCanonicalTerm() {
		return canonicalTerm;
	}

	@Override
	public void setCanonicalTerm(GlossaryTerm canonicalTerm) {
		this.canonicalTerm = canonicalTerm;
	}

	@Override
	public GlossaryTerm getGlossaryTerm() {
		return glossaryTerm;
	}

	@Override
	public void setGlossaryTerm(GlossaryTerm glossaryTerm) {
		this.glossaryTerm = glossaryTerm;
	}

	@Override
	public Set<ProjectOrDomainEntity> getUpdatedEntities() {
		return updatedEntities;
	}

	@Override
	public void execute() {
		GlossaryTermImpl glossaryTerm = (GlossaryTermImpl) getProjectRepository().get(
				getGlossaryTerm());
		GlossaryTerm canonicalTerm = getProjectRepository().get(getCanonicalTerm());
		if (canonicalTerm == null) {
			canonicalTerm = glossaryTerm.getCanonicalTerm();
		} else {
			glossaryTerm.setCanonicalTerm(canonicalTerm);
			canonicalTerm.getAlternateTerms().add(glossaryTerm);
		}

		for (ProjectOrDomainEntity entity : glossaryTerm.getReferers()) {
			if (entity instanceof Goal) {
				GoalImpl goal = (GoalImpl) entity;
				log.debug("replacing '" + glossaryTerm.getName() + "' with '"
						+ canonicalTerm.getName() + "' in the goal name: " + goal.getName());
				goal.setName(replaceText(goal.getName(), glossaryTerm.getName(), canonicalTerm
						.getName()));
				log.debug("replacing '" + glossaryTerm.getName() + "' with '"
						+ canonicalTerm.getName() + "' in the goal text: " + goal.getText());
				goal.setText(replaceText(goal.getText(), glossaryTerm.getName(), canonicalTerm
						.getName()));
				glossaryTerm.getReferers().remove(goal);
				canonicalTerm.getReferers().add(goal);
			} else if (entity instanceof Story) {
				StoryImpl story = (StoryImpl) entity;
				log.debug("replacing '" + glossaryTerm.getName() + "' with '"
						+ canonicalTerm.getName() + "' in the story name: " + story.getName());
				story.setName(replaceText(story.getName(), glossaryTerm.getName(), canonicalTerm
						.getName()));
				log.debug("replacing '" + glossaryTerm.getName() + "' with '"
						+ canonicalTerm.getName() + "' in the story text: " + story.getText());
				story.setText(replaceText(story.getText(), glossaryTerm.getName(), canonicalTerm
						.getName()));
				glossaryTerm.getReferers().remove(story);
				canonicalTerm.getReferers().add(story);
			} else if (entity instanceof Story) {
				ActorImpl actor = (ActorImpl) entity;
				log.debug("replacing '" + glossaryTerm.getName() + "' with '"
						+ canonicalTerm.getName() + "' in the actor name: " + actor.getName());
				actor.setName(replaceText(actor.getName(), glossaryTerm.getName(), canonicalTerm
						.getName()));
				log.debug("replacing '" + glossaryTerm.getName() + "' with '"
						+ canonicalTerm.getName() + "' in the actor text: " + actor.getText());
				actor.setText(replaceText(actor.getText(), glossaryTerm.getName(), canonicalTerm
						.getName()));
				glossaryTerm.getReferers().remove(actor);
				canonicalTerm.getReferers().add(actor);
			} else {
				throw new RuntimeException("Unsupported entity type " + entity.getClass());
			}
			updatedEntities.add(entity);
		}
	}

	// TODO: copied from ResolveIssueWithChangeSpellingPositionCommandImpl,
	// should move to a utility class and shared.
	protected String replaceText(String textToChange, String fromText, String toText) {

		if (textToChange.contains(fromText)) {
			StringBuilder sb = new StringBuilder(textToChange.length() - fromText.length()
					+ toText.length());
			// TODO: what about white space or punctuation between the words?
			// TODO: what about the case of the words being added, for example
			// "The System" being added in the middle of a sentence.
			int start = textToChange.toLowerCase().indexOf(fromText.toLowerCase());
			while (start != -1) {
				sb.setLength(0);
				int end = start + fromText.length();
				sb.append(textToChange.substring(0, start));
				sb.append(toText);
				sb.append(textToChange.substring(end));
				textToChange = sb.toString();
				start = textToChange.indexOf(fromText);
			}
		}
		return textToChange;
	}
}
