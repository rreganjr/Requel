/*
 * $Id: EditScenarioCommandImpl.java,v 1.11 2009/03/30 11:54:28 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project.impl.command;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.repository.EntityException;
import edu.harvard.fas.rregan.repository.EntityExceptionActionType;
import edu.harvard.fas.rregan.requel.NoSuchEntityException;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.Scenario;
import edu.harvard.fas.rregan.requel.project.Step;
import edu.harvard.fas.rregan.requel.project.command.EditScenarioCommand;
import edu.harvard.fas.rregan.requel.project.command.EditScenarioStepCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.impl.ScenarioImpl;
import edu.harvard.fas.rregan.requel.project.impl.assistant.AssistantFacade;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.UserRepository;

/**
 * @author ron
 */
@Controller("editScenarioCommand")
@Scope("prototype")
public class EditScenarioCommandImpl extends EditScenarioStepCommandImpl implements
		EditScenarioCommand {

	private List<EditScenarioStepCommand> editStepCommands;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 * @param projectCommandFactory
	 * @param annotationCommandFactory
	 * @param commandHandler
	 */
	@Autowired
	public EditScenarioCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	@Override
	public Scenario getScenario() {
		return (Scenario) getStep();
	}

	@Override
	public void setScenario(Scenario scenario) {
		setStep(scenario);
	}

	@Override
	public void setStepCommands(List<EditScenarioStepCommand> editStepCommands) {
		this.editStepCommands = editStepCommands;
	}

	protected List<EditScenarioStepCommand> getStepCommands() {
		return editStepCommands;
	}

	@Override
	public void execute() throws Exception {
		List<EditScenarioStepCommand> stepEditCommands = new ArrayList<EditScenarioStepCommand>(
				getStepCommands());
		for (int index = 0; index < stepEditCommands.size(); index++) {
			EditScenarioStepCommand stepCommand = stepEditCommands.get(index);
			// disable analysis of the steps as they will get analyzed
			// when the whole scenario is analyzed.
			stepCommand.setAnalysisEnabled(false);
			stepEditCommands.set(index, getCommandHandler().execute(stepCommand));
		}
		User editedBy = getProjectRepository().get(getEditedBy());
		ScenarioImpl scenarioImpl = (ScenarioImpl) getScenario();
		ProjectOrDomain projectOrDomain = getRepository().get(getProjectOrDomain());

		// check for uniqueness
		try {
			Scenario existing = getProjectRepository().findScenarioByProjectOrDomainAndName(
					projectOrDomain, getName());
			if (scenarioImpl == null) {
				throw EntityException.uniquenessConflict(Scenario.class, existing, FIELD_NAME,
						EntityExceptionActionType.Creating);
			} else if (!existing.equals(scenarioImpl)) {
				throw EntityException.uniquenessConflict(Scenario.class, existing, FIELD_NAME,
						EntityExceptionActionType.Updating);
			}
		} catch (NoSuchEntityException e) {
		}

		if (scenarioImpl == null) {
			scenarioImpl = getProjectRepository().persist(
					new ScenarioImpl(projectOrDomain, editedBy, getName(), getText(),
							getScenarioType()));
		} else {
			scenarioImpl.setName(getName());
			scenarioImpl.setText(getText());
			scenarioImpl.setType(getScenarioType());
		}
		// TODO: merge is failing because the use cases in the
		// usedByUseCases property are my proxies and hibernate throws an
		// IllegalArgumentException: Unknown entity:
		// edu.harvard.fas.rregan.requel.project.impl.UseCaseImpl$$EnhancerByCGLIB$$b1ba12b8
		for (Scenario usingScenario : scenarioImpl.getUsingScenarios()) {
			usingScenario = getRepository().get(usingScenario);
		}

		scenarioImpl = getProjectRepository().merge(scenarioImpl);
		scenarioImpl.getSteps().clear();
		for (EditScenarioStepCommand executedCommand : stepEditCommands) {
			Step step = executedCommand.getStep();
			scenarioImpl.getSteps().add(step);
			step.getUsingScenarios().add(scenarioImpl);
		}
		setScenario(getProjectRepository().merge(scenarioImpl));
	}

	@Override
	public void invokeAnalysis() {
		if (isAnalysisEnabled()) {
			getAssistantManager().analyzeScenario(getScenario());
		}
	}
}
