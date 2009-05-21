/*
 * $Id$
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
package edu.harvard.fas.rregan.requel.project.impl.assistant;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.nlp.NLPProcessorFactory;
import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;
import edu.harvard.fas.rregan.requel.annotation.AnnotationRepository;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.project.Actor;
import edu.harvard.fas.rregan.requel.project.Goal;
import edu.harvard.fas.rregan.requel.project.Project;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.Scenario;
import edu.harvard.fas.rregan.requel.project.Step;
import edu.harvard.fas.rregan.requel.project.Story;
import edu.harvard.fas.rregan.requel.project.UseCase;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.UserRepository;

/**
 * A Facade for applying assistants to projects and project entities.
 * 
 * @author ron
 */
@Component("assistantFacade")
@Scope("singleton")
// TODO: changed this to a singleton because of the following exception
// Exception during analysis of goal text:
// org.springframework.beans.factory.UnsatisfiedDependencyException: Error
// creating bean with name
// 'edu.harvard.fas.rregan.requel.project.impl.command.EditGlossaryTermCommandImpl':
// Unsatisfied dependency expressed through constructor argument with
// index 0 of type
// [edu.harvard.fas.rregan.requel.project.impl.assistant.AssistantManager]:
// Error creating bean with name 'assistantManager': Scope 'session' is not
// active for the current thread; consider defining a scoped proxy for this bean
// if you intend to refer to it from a singleton; nested exception is
// java.lang.IllegalStateException: No thread-bound request found: Are you
// referring to request attributes outside of an actual web request? If you are
// actually operating within a web request and still receive this message,your
// code is probably running outside of DispatcherServlet/DispatcherPortlet: In
// this case, use RequestContextListener or RequestContextFilter to expose the
// current request.
public class AssistantFacade {
	private static final Logger log = Logger.getLogger(AssistantFacade.class);

	private final TaskExecutor taskExecutor;
	private final CommandHandler commandHandler;
	private final AnnotationCommandFactory annotationCommandFactory;
	private final ProjectCommandFactory projectCommandFactory;
	private final UserRepository userRepository;
	private final ProjectRepository projectRepository;
	private final AnnotationRepository annotationRepository;
	private final DictionaryRepository dictionaryRepository;
	private final NLPProcessorFactory nlpProcessorFactory;
	private final UpdatedEntityNotifier updatedEntityNotifier;

	/**
	 * @param taskExecutor
	 * @param commandHandler
	 * @param projectCommandFactory
	 * @param annotationCommandFactory
	 * @param projectRepository
	 * @param userRepository
	 * @param dictionaryRepository
	 * @param annotationRepository
	 * @param nlpProcessorFactory
	 * @param updatedEntityNotifier -
	 *            after an entity is analyzed it is passed to the notifier to
	 *            tell the UI components that reference the entity to refresh
	 */
	@Autowired
	public AssistantFacade(TaskExecutor taskExecutor, CommandHandler commandHandler,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, ProjectRepository projectRepository,
			UserRepository userRepository, DictionaryRepository dictionaryRepository,
			AnnotationRepository annotationRepository, NLPProcessorFactory nlpProcessorFactory,
			UpdatedEntityNotifier updatedEntityNotifier) {
		this.taskExecutor = taskExecutor;
		this.commandHandler = commandHandler;
		this.projectCommandFactory = projectCommandFactory;
		this.annotationCommandFactory = annotationCommandFactory;
		this.projectRepository = projectRepository;
		this.userRepository = userRepository;
		this.annotationRepository = annotationRepository;
		this.dictionaryRepository = dictionaryRepository;
		this.nlpProcessorFactory = nlpProcessorFactory;
		this.updatedEntityNotifier = updatedEntityNotifier;
	}

	protected CommandHandler getCommandHandler() {
		return commandHandler;
	}

	protected ProjectCommandFactory getProjectCommandFactory() {
		return projectCommandFactory;
	}

	protected AnnotationCommandFactory getAnnotationCommandFactory() {
		return annotationCommandFactory;
	}

	protected UserRepository getUserRepository() {
		return userRepository;
	}

	protected ProjectRepository getProjectRepository() {
		return projectRepository;
	}

	protected AnnotationRepository getAnnotationRepository() {
		return annotationRepository;
	}

	protected DictionaryRepository getDictionaryRepository() {
		return dictionaryRepository;
	}

	protected TaskExecutor getTaskExecutor() {
		return taskExecutor;
	}

	protected NLPProcessorFactory getNlpProcessorFactory() {
		return nlpProcessorFactory;
	}

	protected UpdatedEntityNotifier getUpdatedEntityNotifier() {
		return updatedEntityNotifier;
	}

	/**
	 * Analyze all the entities in a project.
	 * 
	 * @param project
	 */
	public void analyzeProject(final Project project) {
		try {
			getTaskExecutor().execute(new Runnable() {
				@Override
				public void run() {
					try {
						User assistantUser = getUserRepository().findUserByUsername("assistant");
						LexicalAssistant lexicalAssistant = new LexicalAssistant(
								getCommandHandler(), getProjectCommandFactory(),
								getAnnotationCommandFactory(), getAnnotationRepository(),
								getProjectRepository(), getDictionaryRepository(),
								getNlpProcessorFactory());
						ProjectAssistant projectAssistant = new ProjectAssistant(lexicalAssistant,
								assistantUser);

						// TODO: the DomainObjectWrappingAdvice that wraps
						// persistence objects with a DomainObjectWrapper isn't
						// getting invoked in the command's invokeAnalysis()
						// method because the get() method for the domain object
						// is called locally in the object. Reloading the object
						// through a repository solves the problem, but using
						// aspectj may be better.
						projectAssistant.analyze(getProjectRepository().get(project));
						getUpdatedEntityNotifier().entityUpdated(project);
					} catch (Exception e) {
						log.error("exception in project assistant: " + e, e);
					}
				}
			});
			log.info("started analysis of " + project);
		} catch (TaskRejectedException e) {
			log.error("failed to execute analysis on '" + project + "' ", e);
		}
	}

	/**
	 * TODO: this executes the analysis in a seperate thread using resources
	 * originating in this thread. That may cause problems in a multi-user
	 * system.
	 * 
	 * @param updatedGoal -
	 *            the goal that has been edited
	 * @param originalGoal -
	 *            a copy of the same goal before it was edited, this is used to
	 *            limit analysis to only things that have changed. If it is not
	 *            supplied everything in updatedGoal will be analyzed.
	 */
	public void analyzeGoal(final Goal updatedGoal) {
		try {
			getTaskExecutor().execute(new Runnable() {
				@Override
				public void run() {
					User assistantUser = getUserRepository().findUserByUsername("assistant");
					LexicalAssistant lexicalAssistant = new LexicalAssistant(getCommandHandler(),
							getProjectCommandFactory(), getAnnotationCommandFactory(),
							getAnnotationRepository(), getProjectRepository(),
							getDictionaryRepository(), getNlpProcessorFactory());
					GoalAssistant goalAssistant = new GoalAssistant(lexicalAssistant, assistantUser);

					// TODO: the DomainObjectWrappingAdvice that wraps
					// persistence objects with a DomainObjectWrapper isn't
					// getting invoked in the command's invokeAnalysis()
					// method because the get() method for the domain object
					// is called locally in the object. Reloading the object
					// through a repository solves the problem, but using
					// aspectj may be better.
					goalAssistant.setEntity(getProjectRepository().get(updatedGoal));
					goalAssistant.analyze();
					getUpdatedEntityNotifier().entityUpdated(updatedGoal);
				}
			});
			log.info("started analysis of " + updatedGoal);
		} catch (TaskRejectedException e) {
			log.error("failed to execute analysis on '" + updatedGoal + "' ", e);
		}
	}

	/**
	 * @param updatedStory
	 * @param originalStory
	 */
	public void analyzeStory(final Story updatedStory) {
		try {
			getTaskExecutor().execute(new Runnable() {
				@Override
				public void run() {
					User assistantUser = getUserRepository().findUserByUsername("assistant");
					LexicalAssistant lexicalAssistant = new LexicalAssistant(getCommandHandler(),
							getProjectCommandFactory(), getAnnotationCommandFactory(),
							getAnnotationRepository(), getProjectRepository(),
							getDictionaryRepository(), getNlpProcessorFactory());
					StoryAssistant storyAssistant = new StoryAssistant(lexicalAssistant,
							assistantUser);

					// TODO: the DomainObjectWrappingAdvice that wraps
					// persistence objects with a DomainObjectWrapper isn't
					// getting invoked in the command's invokeAnalysis()
					// method because the get() method for the domain object
					// is called locally in the object. Reloading the object
					// through a repository solves the problem, but using
					// aspectj may be better.
					storyAssistant.setEntity(getProjectRepository().get(updatedStory));
					storyAssistant.analyze();
					getUpdatedEntityNotifier().entityUpdated(updatedStory);
				}
			});
			log.info("started analysis of " + updatedStory);
		} catch (TaskRejectedException e) {
			log.error("failed to execute analysis on '" + updatedStory + "' ", e);
		}
	}

	/**
	 * @param updatedActor
	 * @param originalActor
	 */
	public void analyzeActor(final Actor updatedActor) {

		try {
			getTaskExecutor().execute(new Runnable() {
				@Override
				public void run() {
					User assistantUser = getUserRepository().findUserByUsername("assistant");
					LexicalAssistant lexicalAssistant = new LexicalAssistant(getCommandHandler(),
							getProjectCommandFactory(), getAnnotationCommandFactory(),
							getAnnotationRepository(), getProjectRepository(),
							getDictionaryRepository(), getNlpProcessorFactory());
					ActorAssistant actorAssistant = new ActorAssistant(lexicalAssistant,
							assistantUser);
					// TODO: the DomainObjectWrappingAdvice that wraps
					// persistence objects with a DomainObjectWrapper isn't
					// getting invoked in the command's invokeAnalysis()
					// method because the get() method for the domain object
					// is called locally in the object. Reloading the object
					// through a repository solves the problem, but using
					// aspectj may be better.
					actorAssistant.setEntity(getProjectRepository().get(updatedActor));
					actorAssistant.analyze();
					getUpdatedEntityNotifier().entityUpdated(updatedActor);
				}
			});
			log.info("started analysis of " + updatedActor);
		} catch (TaskRejectedException e) {
			log.error("failed to execute analysis on '" + updatedActor + "' ", e);
		}
	}

	/**
	 * @param updatedUseCase
	 */
	public void analyzeUseCase(final UseCase updatedUseCase) {

		try {
			getTaskExecutor().execute(new Runnable() {
				@Override
				public void run() {
					User assistantUser = getUserRepository().findUserByUsername("assistant");
					LexicalAssistant lexicalAssistant = new LexicalAssistant(getCommandHandler(),
							getProjectCommandFactory(), getAnnotationCommandFactory(),
							getAnnotationRepository(), getProjectRepository(),
							getDictionaryRepository(), getNlpProcessorFactory());
					ScenarioAssistant scenarioAssistant = new ScenarioAssistant(lexicalAssistant,
							assistantUser);
					ActorAssistant actorAssistant = new ActorAssistant(lexicalAssistant,
							assistantUser);
					UseCaseAssistant useCaseAssistant = new UseCaseAssistant(lexicalAssistant,
							scenarioAssistant, actorAssistant, assistantUser);

					// TODO: the DomainObjectWrappingAdvice that wraps
					// persistence objects with a DomainObjectWrapper isn't
					// getting invoked in the command's invokeAnalysis()
					// method because the get() method for the domain object
					// is called locally in the object. Reloading the object
					// through a repository solves the problem, but using
					// aspectj may be better.
					useCaseAssistant.setEntity(getProjectRepository().get(updatedUseCase));
					useCaseAssistant.analyze();
					getUpdatedEntityNotifier().entityUpdated(updatedUseCase);
				}
			});
			log.info("started analysis of " + updatedUseCase);
		} catch (TaskRejectedException e) {
			log.error("failed to execute analysis on '" + updatedUseCase + "' ", e);
		}
	}

	/**
	 * @param updatedScenarioStep
	 */
	public void analyzeScenarioStep(final Step updatedScenarioStep) {
		try {
			getTaskExecutor().execute(new Runnable() {
				@Override
				public void run() {
					User assistantUser = getUserRepository().findUserByUsername("assistant");
					LexicalAssistant lexicalAssistant = new LexicalAssistant(getCommandHandler(),
							getProjectCommandFactory(), getAnnotationCommandFactory(),
							getAnnotationRepository(), getProjectRepository(),
							getDictionaryRepository(), getNlpProcessorFactory());
					ScenarioAssistant scenarioAssistant = new ScenarioAssistant(lexicalAssistant,
							assistantUser);
					// TODO: the DomainObjectWrappingAdvice that wraps
					// persistence objects with a DomainObjectWrapper isn't
					// getting invoked in the command's invokeAnalysis()
					// method because the get() method for the domain object
					// is called locally in the object. Reloading the object
					// through a repository solves the problem, but using
					// aspectj may be better.
					scenarioAssistant.setEntity(getProjectRepository().get(updatedScenarioStep));
					scenarioAssistant.analyze();
					getUpdatedEntityNotifier().entityUpdated(updatedScenarioStep);
				}
			});
			log.info("started analysis of " + updatedScenarioStep);
		} catch (TaskRejectedException e) {
			log.error("failed to execute analysis on '" + updatedScenarioStep + "' ", e);
		}
	}

	/**
	 * @param updatedScenario
	 */
	public void analyzeScenario(final Scenario updatedScenario) {
		analyzeScenarioStep(updatedScenario);
	}
}
