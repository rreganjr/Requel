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
package com.rreganjr.requel.project.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.platform.exception.EntityException;
import com.rreganjr.platform.exception.NoSuchEntityException;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.ScenarioType;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.command.AnalysisRequestSource;
import com.rreganjr.requel.project.command.EditScenarioStepCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.StepImpl;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.user.UserRepository;

/**
 * @author ron
 */
@Controller("editScenarioStepCommand")
@Scope("prototype")
public class EditScenarioStepCommandImpl extends AbstractEditProjectOrDomainEntityCommand implements
		EditScenarioStepCommand, AnalysisRequestSource, com.rreganjr.requel.project.ProjectScopedCommand, com.rreganjr.platform.command.AuthorizableCommand {

	@Override
	public com.rreganjr.requel.project.Project getProject() {
		return (getProjectOrDomain() instanceof com.rreganjr.requel.project.Project p) ? p : null;
	}

	@Override
	public com.rreganjr.platform.command.AuthorizationRequirement getAuthorizationRequirement() {
		return new com.rreganjr.platform.command.AuthorizationRequirement.RequiresStakeholderPermission(com.rreganjr.requel.project.Scenario.class, "Edit");
	}

	private Step step;
	private String text;
	private String scenarioTypeName;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 * @param projectCommandFactory
	 * @param annotationCommandFactory
	 * @param commandHandler
	 */
	@Autowired
	public EditScenarioStepCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	@Override
	public Step getStep() {
		return step;
	}

	@Override
	public void setStep(Step step) {
		this.step = step;
	}

	@Override
	public void setText(String text) {
		this.text = text;
	}

	protected String getText() {
		return text;
	}

	protected ScenarioType getScenarioType() {
		if (scenarioTypeName != null) {
			return ScenarioType.valueOf(scenarioTypeName);
		}
		return null;
	}

	@Override
	public void setScenarioTypeName(String scenarioTypeName) {
		this.scenarioTypeName = scenarioTypeName;
	}

	@Override
	public void execute() throws Exception {
		User editedBy = getProjectRepository().get(getEditedBy());
		StepImpl stepImpl = (StepImpl) getStep();
		ProjectOrDomain projectOrDomain = getProjectRepository().get(getProjectOrDomain());

		refuseNameCollision(projectOrDomain, stepImpl);

		if (stepImpl == null) {
			stepImpl = getProjectRepository()
					.persist(
							new StepImpl(projectOrDomain, editedBy, getName(), getText(),
									getScenarioType()));
		} else {
			// Null leaves a property as it is; "" clears the text (issue #316).
			if (getName() != null) {
				stepImpl.setName(getName());
			}
			if (getText() != null) {
				stepImpl.setText(getText());
			}
			if (getScenarioType() != null) {
				stepImpl.setType(getScenarioType());
			}
		}
		setStep(getProjectRepository().merge(stepImpl));
	}

	/**
	 * Issue #254: refuse a name that already belongs to another step or scenario in this project,
	 * and say which id to send instead.
	 *
	 * <p>Steps and scenarios share one table under a discriminator with a single
	 * {@code (projectordomain_id, name)} unique key, so a step name collides with a scenario name
	 * as readily as with another step's. Without this guard the write reached the database and the
	 * constraint violation was adapted by an exception adapter registered against
	 * {@code ProjectOrDomainEntity.class} — which is why the caller saw "The name conflicts with an
	 * existing ProjectOrDomainEntity", naming neither the step nor what it collided with.
	 *
	 * <p>The message tells the caller which id to send rather than offering to reuse the entity
	 * silently. Steps are shared across scenarios ({@link Step#getUsingScenarios()}) and a scenario
	 * can itself be a step, so a bare name match is ambiguous between "link the existing one" and
	 * "I did not realise this name was taken" — and guessing the first would apply the caller's
	 * text to a step that every other scenario using it shares. Both link paths already exist in
	 * {@code EditScenario}'s steps array; only the signposting was missing.
	 */
	protected void refuseNameCollision(ProjectOrDomain projectOrDomain, Step self) {
		String name = getName();
		if ((name == null) || name.trim().isEmpty()) {
			// A null name means "leave it as it is" (issue #251).
			return;
		}
		Step existing;
		try {
			existing = getProjectRepository().findStepByProjectOrDomainAndName(projectOrDomain,
					name);
		} catch (NoSuchEntityException e) {
			return;
		}
		if ((self != null) && existing.equals(self)) {
			return;
		}
		String quoted = "'" + name.trim() + "'";
		if (existing instanceof Scenario) {
			throw EntityException.uniquenessConflict(null,
					"The name conflicts with an existing Scenario: scenario " + existing.getId()
							+ " in this project already uses the name " + quoted
							+ ". To nest that scenario as a step, send its id as stepId with"
							+ " isScenario true; otherwise choose a different name.");
		}
		throw EntityException.uniquenessConflict(null,
				"The name conflicts with an existing Step: step " + existing.getId()
						+ " in this project already uses the name " + quoted
						+ ". To reuse that step, send its id as stepId; otherwise choose a"
						+ " different name.");
	}

	/**
	 * Legacy fallback. The Step / Scenario paths are migrated to the assistant SPI
	 * (issue #43): the command-handler layer detects {@link AnalysisRequestSource} and
	 * dispatches through {@code AnalysisRequestDispatcher}, so this method is normally
	 * not invoked. It is retained as a safe fallback. {@code EditScenarioCommandImpl}
	 * inherits {@link #getAnalysisTarget()} (its {@code getStep()} returns the scenario,
	 * which is itself a {@link Step}).
	 */
	@Override
	public void invokeAnalysis() {
		if (isAnalysisEnabled()) {
			getAssistantManager().analyzeScenarioStep(getStep());
		}
	}

	@Override
	public ProjectOrDomainEntity getAnalysisTarget() {
		return getStep();
	}

	@Override
	public User getAnalysisTriggeredBy() {
		return getEditedBy();
	}
}
