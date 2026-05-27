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
package com.rreganjr.requel.project.impl.assistant;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;

import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.UseCase;

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
// 'com.rreganjr.requel.project.impl.command.EditGlossaryTermCommandImpl':
// Unsatisfied dependency expressed through constructor argument with
// index 0 of type
// [com.rreganjr.requel.project.impl.assistant.AssistantManager]:
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
	private final AssistantTaskRunner assistantTaskRunner;
	private final UpdatedEntityNotifier updatedEntityNotifier;

	/**
	 * @param taskExecutor
	 * @param assistantTaskRunner
	 * @param updatedEntityNotifier -
	 *            after an entity is analyzed it is passed to the notifier to
	 *            tell the UI components that reference the entity to refresh
	 */
	@Autowired
	public AssistantFacade(@Qualifier("assistantTaskExecutor") TaskExecutor taskExecutor,
			AssistantTaskRunner assistantTaskRunner,
			UpdatedEntityNotifier updatedEntityNotifier) {
		this.taskExecutor = taskExecutor;
		this.assistantTaskRunner = assistantTaskRunner;
		this.updatedEntityNotifier = updatedEntityNotifier;
	}

	protected TaskExecutor getTaskExecutor() {
		return taskExecutor;
	}

	protected AssistantTaskRunner getAssistantTaskRunner() {
		return assistantTaskRunner;
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
		submitAnalysis(project, "project", new Runnable() {
			@Override
			public void run() {
				getAssistantTaskRunner().analyzeProject(project);
			}
		});
	}

	/**
	 * Execute goal analysis in the assistant task runner.
	 * 
	 * @param updatedGoal -
	 *            the goal that has been edited
	 * @param originalGoal -
	 *            a copy of the same goal before it was edited, this is used to
	 *            limit analysis to only things that have changed. If it is not
	 *            supplied everything in updatedGoal will be analyzed.
	 */
	public void analyzeGoal(final Goal updatedGoal) {
		submitAnalysis(updatedGoal, "goal", new Runnable() {
			@Override
			public void run() {
				getAssistantTaskRunner().analyzeGoal(updatedGoal);
			}
		});
	}

	/**
	 * @param updatedStory
	 * @param originalStory
	 */
	public void analyzeStory(final Story updatedStory) {
		submitAnalysis(updatedStory, "story", new Runnable() {
			@Override
			public void run() {
				getAssistantTaskRunner().analyzeStory(updatedStory);
			}
		});
	}

	/**
	 * @param updatedActor
	 * @param originalActor
	 */
	public void analyzeActor(final Actor updatedActor) {
		submitAnalysis(updatedActor, "actor", new Runnable() {
			@Override
			public void run() {
				getAssistantTaskRunner().analyzeActor(updatedActor);
			}
		});
	}

	/**
	 * @param updatedUseCase
	 */
	public void analyzeUseCase(final UseCase updatedUseCase) {
		submitAnalysis(updatedUseCase, "use case", new Runnable() {
			@Override
			public void run() {
				getAssistantTaskRunner().analyzeUseCase(updatedUseCase);
			}
		});
	}

	/**
	 * @param updatedScenarioStep
	 */
	public void analyzeScenarioStep(final Step updatedScenarioStep) {
		submitAnalysis(updatedScenarioStep, "scenario step", new Runnable() {
			@Override
			public void run() {
				getAssistantTaskRunner().analyzeScenarioStep(updatedScenarioStep);
			}
		});
	}

	/**
	 * @param updatedScenario
	 */
	public void analyzeScenario(final Scenario updatedScenario) {
		analyzeScenarioStep(updatedScenario);
	}

	private void submitAnalysis(final ProjectOrDomain updatedEntity, String targetType,
			final Runnable analysisTask) {
		try {
			getTaskExecutor().execute(new Runnable() {
				@Override
				public void run() {
					try {
						analysisTask.run();
						getUpdatedEntityNotifier().entityUpdated(updatedEntity);
					} catch (Exception e) {
						log.error("exception in " + targetType + " assistant: " + e, e);
					}
				}
			});
			log.info("started analysis of " + updatedEntity);
		} catch (TaskRejectedException e) {
			log.error("failed to execute analysis on '" + updatedEntity + "' ", e);
		}
	}

	private void submitAnalysis(final ProjectOrDomainEntity updatedEntity, String targetType,
			final Runnable analysisTask) {
		try {
			getTaskExecutor().execute(new Runnable() {
				@Override
				public void run() {
					try {
						analysisTask.run();
						getUpdatedEntityNotifier().entityUpdated(updatedEntity);
					} catch (Exception e) {
						log.error("exception in " + targetType + " assistant: " + e, e);
					}
				}
			});
			log.info("started analysis of " + updatedEntity);
		} catch (TaskRejectedException e) {
			log.error("failed to execute analysis on '" + updatedEntity + "' ", e);
		}
	}
}
