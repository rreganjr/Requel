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
package com.rreganjr.requel.project.impl.assistant;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.TextEntity;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.user.User;

/**
 * The Project Assistant analyzes the entities of a project via entity specific
 * assistants.
 * 
 * @author ron
 */
public class ProjectAssistant {
	private static final Log log = LogFactory.getLog(ProjectAssistant.class);

	private final LexicalAssistant lexicalAssistant;
	private final User user;

	/**
	 * @param lexicalAssistant -
	 *            assistant for analyzing text for spelling, terms and other
	 *            word oriented analysis.
	 * @param user -
	 *            the user to user as the creator of the annotation entities.
	 */
	public ProjectAssistant(LexicalAssistant lexicalAssistant, User user) {
		this.lexicalAssistant = lexicalAssistant;
		this.user = user;
	}

	/**
	 * Analyze all the entities in the Project.
	 * 
	 * @param project
	 */
	public void analyze(Project project) {
		log.debug("analyzing Project: " + project);
		if (project != null) {
			analyzeGoals(project.getGoals());
			analyzeStories(project.getStories());
			analyzeActors(project.getActors());
			analyzeUseCases(project.getUseCases());
		}
	}

	private void analyzeGoals(Collection<Goal> entities) {
		GoalAssistant assistant = new GoalAssistant(lexicalAssistant, user);
		for (TextEntity entity : entities) {
			try {
				assistant.setEntity(entity);
				assistant.analyze();
			} catch (Exception e) {
				log.error("Exception during analysis: " + e, e);
			}
		}
	}

	private void analyzeStories(Collection<Story> entities) {
		StoryAssistant assistant = new StoryAssistant(lexicalAssistant, user);
		for (TextEntity entity : entities) {
			try {
				assistant.setEntity(entity);
				assistant.analyze();
			} catch (Exception e) {
				log.error("Exception during analysis: " + e, e);
			}
		}
	}

	private void analyzeActors(Collection<Actor> entities) {
		ActorAssistant assistant = new ActorAssistant(lexicalAssistant, user);
		for (TextEntity entity : entities) {
			try {
				assistant.setEntity(entity);
				assistant.analyze();
			} catch (Exception e) {
				log.error("Exception during analysis: " + e, e);
			}
		}
	}

	private void analyzeUseCases(Collection<UseCase> entities) {
		ActorAssistant actorAssistant = new ActorAssistant(lexicalAssistant, user);
		ScenarioAssistant scenarioAssistant = new ScenarioAssistant(lexicalAssistant, user);
		UseCaseAssistant assistant = new UseCaseAssistant(lexicalAssistant, scenarioAssistant,
				actorAssistant, user);
		for (TextEntity entity : entities) {
			try {
				assistant.setEntity(entity);
				assistant.analyze();
			} catch (Exception e) {
				log.error("Exception during analysis: " + e, e);
			}
		}
	}
}
