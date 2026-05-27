/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.nlp.dictionary.DictionaryRepository;
import com.rreganjr.nlp.dictionary.NLPProcessorFactory;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.user.UserRepository;

/**
 * Runs assistant analysis inside a Spring-managed transaction.
 *
 * AssistantFacade submits work from a background executor. Keeping the analysis
 * body on this separate bean ensures calls cross a Spring proxy, opening a new
 * Hibernate session for lazy collections used by the NLP pipeline.
 */
@Component
public class AssistantTaskRunner {

	private final CommandHandler commandHandler;
	private final ProjectCommandFactory projectCommandFactory;
	private final AnnotationCommandFactory annotationCommandFactory;
	private final ProjectRepository projectRepository;
	private final UserRepository userRepository;
	private final DictionaryRepository dictionaryRepository;
	private final AnnotationRepository annotationRepository;
	private final NLPProcessorFactory nlpProcessorFactory;

	@Autowired
	public AssistantTaskRunner(CommandHandler commandHandler,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory,
			ProjectRepository projectRepository,
			UserRepository userRepository,
			DictionaryRepository dictionaryRepository,
			AnnotationRepository annotationRepository,
			NLPProcessorFactory nlpProcessorFactory) {
		this.commandHandler = commandHandler;
		this.projectCommandFactory = projectCommandFactory;
		this.annotationCommandFactory = annotationCommandFactory;
		this.projectRepository = projectRepository;
		this.userRepository = userRepository;
		this.dictionaryRepository = dictionaryRepository;
		this.annotationRepository = annotationRepository;
		this.nlpProcessorFactory = nlpProcessorFactory;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void analyzeProject(Project project) {
		ProjectAssistant projectAssistant = new ProjectAssistant(newLexicalAssistant(),
				assistantUser());
		projectAssistant.analyze(projectRepository.get(project));
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void analyzeGoal(Goal updatedGoal) {
		GoalAssistant goalAssistant = new GoalAssistant(newLexicalAssistant(), assistantUser());
		goalAssistant.setEntity(projectRepository.get(updatedGoal));
		goalAssistant.analyze();
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void analyzeStory(Story updatedStory) {
		StoryAssistant storyAssistant = new StoryAssistant(newLexicalAssistant(), assistantUser());
		storyAssistant.setEntity(projectRepository.get(updatedStory));
		storyAssistant.analyze();
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void analyzeActor(Actor updatedActor) {
		ActorAssistant actorAssistant = new ActorAssistant(newLexicalAssistant(), assistantUser());
		actorAssistant.setEntity(projectRepository.get(updatedActor));
		actorAssistant.analyze();
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void analyzeUseCase(UseCase updatedUseCase) {
		LexicalAssistant lexicalAssistant = newLexicalAssistant();
		User assistantUser = assistantUser();
		ScenarioAssistant scenarioAssistant = new ScenarioAssistant(lexicalAssistant,
				assistantUser);
		ActorAssistant actorAssistant = new ActorAssistant(lexicalAssistant, assistantUser);
		UseCaseAssistant useCaseAssistant = new UseCaseAssistant(lexicalAssistant,
				scenarioAssistant, actorAssistant, assistantUser);
		useCaseAssistant.setEntity(projectRepository.get(updatedUseCase));
		useCaseAssistant.analyze();
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void analyzeScenarioStep(Step updatedScenarioStep) {
		ScenarioAssistant scenarioAssistant = new ScenarioAssistant(newLexicalAssistant(),
				assistantUser());
		scenarioAssistant.setEntity(projectRepository.get(updatedScenarioStep));
		scenarioAssistant.analyze();
	}

	private User assistantUser() {
		return userRepository.findUserByUsername("assistant");
	}

	private LexicalAssistant newLexicalAssistant() {
		return new LexicalAssistant(commandHandler, projectCommandFactory,
				annotationCommandFactory, annotationRepository, projectRepository,
				dictionaryRepository, nlpProcessorFactory);
	}
}
