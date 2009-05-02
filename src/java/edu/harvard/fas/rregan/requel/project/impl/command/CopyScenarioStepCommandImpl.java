/*
 * $Id: CopyScenarioStepCommandImpl.java,v 1.5 2009/03/30 11:54:28 rregan Exp $
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
import edu.harvard.fas.rregan.requel.project.GlossaryTerm;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.Step;
import edu.harvard.fas.rregan.requel.project.command.CopyScenarioStepCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.impl.StepImpl;
import edu.harvard.fas.rregan.requel.project.impl.assistant.AssistantFacade;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.UserRepository;

/**
 * @author ron
 */
@Controller("copyScenarioStepCommand")
@Scope("prototype")
public class CopyScenarioStepCommandImpl extends AbstractEditProjectCommand implements
		CopyScenarioStepCommand {

	private Step originalScenarioStep;
	private Step newScenarioStep;
	private String newScenarioStepName;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 * @param projectCommandFactory
	 * @param commandHandler
	 */
	@Autowired
	public CopyScenarioStepCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	protected Step getOriginalScenarioStep() {
		return originalScenarioStep;
	}

	public void setOriginalScenarioStep(Step originalScenarioStep) {
		this.originalScenarioStep = originalScenarioStep;
	}

	protected String getNewScenarioStepName() {
		return newScenarioStepName;
	}

	public void setNewScenarioStepName(String newScenarioStepName) {
		this.newScenarioStepName = newScenarioStepName;
	}

	@Override
	public Step getNewScenarioStep() {
		return newScenarioStep;
	}

	protected void setNewScenarioStep(Step newScenarioStep) {
		this.newScenarioStep = newScenarioStep;
	}

	@Override
	public void execute() {
		User editedBy = getRepository().get(getEditedBy());
		StepImpl originalScenarioStep = (StepImpl) getRepository().get(getOriginalScenarioStep());

		String newName;
		if ((getNewScenarioStepName() == null) || (getNewScenarioStepName().length() == 0)) {
			newName = generateNewScenarioStepName(originalScenarioStep.getName());
		} else {
			newName = generateNewScenarioStepName(getNewScenarioStepName());
		}
		StepImpl newScenarioStep = getProjectRepository().persist(
				new StepImpl(originalScenarioStep.getProjectOrDomain(), editedBy, newName,
						originalScenarioStep.getText(), originalScenarioStep.getType()));

		// TODO: this assumes that all annotations are appropriate for the new
		// scenario
		for (Annotation annotation : originalScenarioStep.getAnnotations()) {
			newScenarioStep.getAnnotations().add(annotation);
			annotation.getAnnotatables().add(newScenarioStep);
		}
		for (GlossaryTerm term : originalScenarioStep.getGlossaryTerms()) {
			newScenarioStep.getGlossaryTerms().add(term);
			term.getReferers().add(newScenarioStep);
		}
		newScenarioStep = getProjectRepository().merge(newScenarioStep);
		setNewScenarioStep(newScenarioStep);
	}

	private String generateNewScenarioStepName(String originalName) {
		String newName = originalName;
		try {
			getProjectRepository().findScenarioByProjectOrDomainAndName(
					getOriginalScenarioStep().getProjectOrDomain(), newName);
			int counter = 1;
			newName = originalName + " " + counter;
			while (true) {
				try {
					getProjectRepository().findScenarioByProjectOrDomainAndName(
							getOriginalScenarioStep().getProjectOrDomain(), newName);
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
