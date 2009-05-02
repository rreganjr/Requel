/*
 * $Id: EditScenarioStepCommandImpl.java,v 1.5 2009/03/30 11:54:30 rregan Exp $
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
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.ScenarioType;
import edu.harvard.fas.rregan.requel.project.Step;
import edu.harvard.fas.rregan.requel.project.command.EditScenarioStepCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.impl.StepImpl;
import edu.harvard.fas.rregan.requel.project.impl.assistant.AssistantFacade;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.UserRepository;

/**
 * @author ron
 */
@Controller("editScenarioStepCommand")
@Scope("prototype")
public class EditScenarioStepCommandImpl extends AbstractEditProjectOrDomainEntityCommand implements
		EditScenarioStepCommand {

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

		if (stepImpl == null) {
			stepImpl = getProjectRepository()
					.persist(
							new StepImpl(projectOrDomain, editedBy, getName(), getText(),
									getScenarioType()));
		} else {
			stepImpl.setName(getName());
			stepImpl.setText(getText());
			stepImpl.setType(getScenarioType());
		}
		setStep(getProjectRepository().merge(stepImpl));
	}

	@Override
	public void invokeAnalysis() {
		if (isAnalysisEnabled()) {
			getAssistantManager().analyzeScenarioStep(getStep());
		}
	}
}
