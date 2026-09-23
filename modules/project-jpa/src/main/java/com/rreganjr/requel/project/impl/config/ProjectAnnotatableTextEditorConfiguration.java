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
package com.rreganjr.requel.project.impl.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.rreganjr.command.Command;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.spi.AnnotatableTextEditRegistry;
import com.rreganjr.requel.annotation.spi.AnnotatableTextEditor;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.TextEntity;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.command.EditActorCommand;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditScenarioCommand;
import com.rreganjr.requel.project.command.EditScenarioStepCommand;
import com.rreganjr.requel.project.command.EditStoryCommand;
import com.rreganjr.requel.project.command.EditTextEntityCommand;
import com.rreganjr.requel.project.command.EditUseCaseCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.ActorImpl;
import com.rreganjr.requel.project.impl.GoalImpl;
import com.rreganjr.requel.project.impl.ScenarioImpl;
import com.rreganjr.requel.project.impl.StepImpl;
import com.rreganjr.requel.project.impl.StoryImpl;
import com.rreganjr.requel.project.impl.UseCaseImpl;
import com.rreganjr.requel.project.impl.assistant.ProjectOrDomainEntityAssistant;

/**
 * Registers how each analysable project entity has its text corrected when a spelling issue
 * is resolved (issue #305), so the correction runs as that entity's own authorized edit
 * instead of a reflective setter.
 * <p>
 * Six entries, matching exactly what {@code AssistantTaskRunner} analyses: goals, stories,
 * actors, use cases, scenarios and steps. A lexical issue cannot attach to anything else, so
 * nothing else needs an editor &mdash; {@code ProjectAssistant} fans out to these types
 * rather than annotating the project itself, and no assistant touches project teams, goal
 * relations, glossary terms or stakeholders. Adding an assistant for a new entity type means
 * adding its editor here; {@code ProjectAnnotatableTextEditorConfigurationTest} is what
 * catches forgetting to.
 */
@Configuration
public class ProjectAnnotatableTextEditorConfiguration {

	@Bean
	InitializingBean projectAnnotatableTextEditorInitializer(
			AnnotatableTextEditRegistry registry, ProjectCommandFactory commandFactory) {
		return () -> {
			registry.registerTextEditor(GoalImpl.class, editor(annotatable -> {
				EditGoalCommand command = commandFactory.newEditGoalCommand();
				command.setGoal((Goal) annotatable);
				return command;
			}));
			registry.registerTextEditor(StoryImpl.class, editor(annotatable -> {
				EditStoryCommand command = commandFactory.newEditStoryCommand();
				command.setStory((Story) annotatable);
				return command;
			}));
			registry.registerTextEditor(ActorImpl.class, editor(annotatable -> {
				EditActorCommand command = commandFactory.newEditActorCommand();
				command.setActor((Actor) annotatable);
				return command;
			}));
			registry.registerTextEditor(UseCaseImpl.class, editor(annotatable -> {
				EditUseCaseCommand command = commandFactory.newEditUseCaseCommand();
				command.setUseCase((UseCase) annotatable);
				return command;
			}));
			// Registered ahead of StepImpl on purpose: ScenarioImpl extends StepImpl, and a
			// scenario edited through EditScenarioStepCommand would lose its step list.
			registry.registerTextEditor(ScenarioImpl.class, editor(annotatable -> {
				EditScenarioCommand command = commandFactory.newEditScenarioCommand();
				command.setScenario((Scenario) annotatable);
				return command;
			}));
			registry.registerTextEditor(StepImpl.class, editor(annotatable -> {
				EditScenarioStepCommand command = commandFactory.newEditScenarioStepCommand();
				command.setStep((Step) annotatable);
				return command;
			}));
		};
	}

	/**
	 * Wrap a per-type command supplier as an editor. Reading a property is identical for every
	 * analysable entity &mdash; they are all {@link TextEntity} and the property is always the
	 * name or the text &mdash; so only building the command differs, and that is all each
	 * registration has to supply.
	 */
	private static AnnotatableTextEditor editor(CommandSupplier supplier) {
		return new AnnotatableTextEditor() {

			@Override
			public String currentValue(Annotatable annotatable, String propertyName) {
				TextEntity entity = (TextEntity) annotatable;
				return isName(propertyName, entity) ? entity.getName() : entity.getText();
			}

			@Override
			public Command newEditCommand(Annotatable annotatable, String propertyName,
					String newValue, User editedBy) {
				return configure(supplier.newCommandFor(annotatable), (TextEntity) annotatable,
						propertyName, newValue, editedBy);
			}
		};
	}

	/**
	 * Builds the right {@code Edit*Command} for one entity type and points it at the entity.
	 * Everything else about the edit is filled in by {@link #configure}.
	 */
	@FunctionalInterface
	private interface CommandSupplier {
		EditTextEntityCommand newCommandFor(Annotatable annotatable);
	}

	/**
	 * Whether the named property is the entity's name, rejecting anything that is neither the
	 * name nor the text property the analysing assistants use.
	 */
	private static boolean isName(String propertyName, TextEntity entity) {
		if (ProjectOrDomainEntityAssistant.PROP_NAME.equals(propertyName)) {
			return true;
		}
		if (ProjectOrDomainEntityAssistant.PROP_TEXT.equals(propertyName)) {
			return false;
		}
		throw new IllegalArgumentException("Cannot correct property '" + propertyName + "' on "
				+ entity.getClass().getName() + "; expected "
				+ ProjectOrDomainEntityAssistant.PROP_NAME + " or "
				+ ProjectOrDomainEntityAssistant.PROP_TEXT);
	}

	/**
	 * Fill in the command for a one-property correction.
	 * <p>
	 * Only the corrected property is supplied. The other is left null, which every
	 * {@code Edit*Command} update treats as "leave it as it is" (issue #316), so a name-only
	 * correction cannot touch the entity's text and vice versa.
	 * <p>
	 * Analysis is disabled because the replacement word is the dictionary's own suggestion.
	 * Re-analysing it is wasted work, and it risks the lexical assistant raising a fresh issue
	 * on text it just advised correcting. The optimistic-lock version is deliberately left
	 * null: the resolve request carries no version, so there is nothing to assert. The command
	 * still reloads the entity, so a concurrent change surfaces rather than being overwritten
	 * silently.
	 */
	private static Command configure(EditTextEntityCommand command, TextEntity entity,
			String propertyName, String newValue, User editedBy) {
		boolean name = isName(propertyName, entity);
		command.setProjectOrDomain(entity.getProjectOrDomain());
		if (name) {
			command.setName(newValue);
		} else {
			command.setText(newValue);
		}
		command.setEditedBy(editedBy);
		command.setAnalysisEnabled(false);
		return command;
	}
}
