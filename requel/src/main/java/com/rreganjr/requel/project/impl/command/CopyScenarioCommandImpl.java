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
package com.rreganjr.requel.project.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.EntityException;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.command.CopyScenarioCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.ScenarioImpl;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;

/**
 * @author ron
 */
@Controller("copyScenarioCommand")
@Scope("prototype")
public class CopyScenarioCommandImpl extends AbstractEditProjectCommand implements
		CopyScenarioCommand {

	private Scenario originalScenario;
	private Scenario newScenario;
	private String newScenarioName;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 */
	@Autowired
	public CopyScenarioCommandImpl(AssistantFacade assistantManager, UserRepository userRepository,
			ProjectRepository projectRepository, ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	protected Scenario getOriginalScenario() {
		return originalScenario;
	}

	public void setOriginalScenario(Scenario originalScenario) {
		this.originalScenario = originalScenario;
	}

	protected String getNewScenarioName() {
		return newScenarioName;
	}

	public void setNewScenarioName(String newScenarioName) {
		this.newScenarioName = newScenarioName;
	}

	@Override
	public Scenario getNewScenario() {
		return newScenario;
	}

	protected void setNewScenario(Scenario newScenario) {
		this.newScenario = newScenario;
	}

	@Override
	public void execute() {
		User editedBy = getRepository().get(getEditedBy());
		ScenarioImpl originalScenario = (ScenarioImpl) getRepository().get(getOriginalScenario());

		String newName;
		if ((getNewScenarioName() == null) || (getNewScenarioName().length() == 0)) {
			newName = generateNewScenarioName(originalScenario.getName());
		} else {
			newName = generateNewScenarioName(getNewScenarioName());
		}
		ScenarioImpl newScenario = getProjectRepository().persist(
				new ScenarioImpl(originalScenario.getProjectOrDomain(), editedBy, newName,
						originalScenario.getText(), originalScenario.getType()));

		// TODO: this assumes that all annotations are appropriate for the new
		// scenario
		for (Annotation annotation : originalScenario.getAnnotations()) {
			newScenario.getAnnotations().add(annotation);
			annotation.getAnnotatables().add(newScenario);
		}
		for (GlossaryTerm term : originalScenario.getGlossaryTerms()) {
			newScenario.getGlossaryTerms().add(term);
			term.getReferers().add(newScenario);
		}
		newScenario = getProjectRepository().merge(newScenario);
		setNewScenario(newScenario);
	}

	private String generateNewScenarioName(String originalName) {
		String newName = originalName;
		try {
			getProjectRepository().findScenarioByProjectOrDomainAndName(
					getOriginalScenario().getProjectOrDomain(), newName);
			int counter = 1;
			newName = originalName + " " + counter;
			while (true) {
				try {
					getProjectRepository().findScenarioByProjectOrDomainAndName(
							getOriginalScenario().getProjectOrDomain(), newName);
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
