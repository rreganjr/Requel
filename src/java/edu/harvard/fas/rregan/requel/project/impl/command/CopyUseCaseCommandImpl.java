/*
 * $Id: CopyUseCaseCommandImpl.java,v 1.8 2009/03/30 11:54:27 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.repository.EntityException;
import edu.harvard.fas.rregan.requel.annotation.Annotation;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.project.Actor;
import edu.harvard.fas.rregan.requel.project.GlossaryTerm;
import edu.harvard.fas.rregan.requel.project.Goal;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.Story;
import edu.harvard.fas.rregan.requel.project.UseCase;
import edu.harvard.fas.rregan.requel.project.command.CopyScenarioCommand;
import edu.harvard.fas.rregan.requel.project.command.CopyUseCaseCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.impl.UseCaseImpl;
import edu.harvard.fas.rregan.requel.project.impl.assistant.AssistantFacade;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.UserRepository;

/**
 * @author ron
 */
@Controller("copyUseCaseCommand")
@Scope("prototype")
public class CopyUseCaseCommandImpl extends AbstractEditProjectCommand implements
		CopyUseCaseCommand {

	private UseCase originalUseCase;
	private UseCase newUseCase;
	private String newUseCaseName;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 */
	@Autowired
	public CopyUseCaseCommandImpl(AssistantFacade assistantManager, UserRepository userRepository,
			ProjectRepository projectRepository, ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	protected UseCase getOriginalUseCase() {
		return originalUseCase;
	}

	public void setOriginalUseCase(UseCase originalUseCase) {
		this.originalUseCase = originalUseCase;
	}

	protected String getNewUseCaseName() {
		return newUseCaseName;
	}

	public void setNewUseCaseName(String newUseCaseName) {
		this.newUseCaseName = newUseCaseName;
	}

	@Override
	public UseCase getNewUseCase() {
		return newUseCase;
	}

	protected void setNewUseCase(UseCase newUseCase) {
		this.newUseCase = newUseCase;
	}

	@Override
	public void execute() throws Exception {
		User editedBy = getRepository().get(getEditedBy());
		UseCaseImpl originalUseCase = (UseCaseImpl) getRepository().get(getOriginalUseCase());

		String newName;
		if ((getNewUseCaseName() == null) || (getNewUseCaseName().length() == 0)) {
			newName = generateNewUseCaseName(originalUseCase.getName());
		} else {
			newName = generateNewUseCaseName(getNewUseCaseName());
		}
		CopyScenarioCommand copyScenarioCommand = getProjectCommandFactory()
				.newCopyScenarioCommand();
		copyScenarioCommand.setEditedBy(editedBy);
		copyScenarioCommand.setOriginalScenario(originalUseCase.getScenario());
		getCommandHandler().execute(copyScenarioCommand);

		UseCaseImpl newUseCase = getProjectRepository().persist(
				new UseCaseImpl(originalUseCase.getProjectOrDomain(), originalUseCase
						.getPrimaryActor(), editedBy, newName, originalUseCase.getText(),
						copyScenarioCommand.getNewScenario()));
		for (Actor actor : originalUseCase.getActors()) {
			newUseCase.getActors().add(actor);
			actor.getReferers().add(newUseCase);
		}
		for (Story story : originalUseCase.getStories()) {
			newUseCase.getStories().add(story);
			story.getReferers().add(newUseCase);
		}
		for (Goal goal : originalUseCase.getGoals()) {
			newUseCase.getGoals().add(goal);
			goal.getReferers().add(newUseCase);
		}
		// TODO: this assumes that all annotations are appropriate for the new
		// use case
		for (Annotation annotation : originalUseCase.getAnnotations()) {
			newUseCase.getAnnotations().add(annotation);
			annotation.getAnnotatables().add(newUseCase);
		}
		for (GlossaryTerm term : originalUseCase.getGlossaryTerms()) {
			newUseCase.getGlossaryTerms().add(term);
			term.getReferers().add(newUseCase);
		}
		newUseCase = getProjectRepository().merge(newUseCase);
		setNewUseCase(newUseCase);
	}

	private String generateNewUseCaseName(String originalName) {
		String newName = originalName;
		try {
			getProjectRepository().findUseCaseByProjectOrDomainAndName(
					getOriginalUseCase().getProjectOrDomain(), newName);
			int counter = 1;
			newName = originalName + " " + counter;
			while (true) {
				try {
					getProjectRepository().findUseCaseByProjectOrDomainAndName(
							getOriginalUseCase().getProjectOrDomain(), newName);
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
