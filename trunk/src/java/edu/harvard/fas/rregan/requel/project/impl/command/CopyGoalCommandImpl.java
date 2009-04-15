/*
 * $Id: CopyGoalCommandImpl.java,v 1.6 2009/03/30 11:54:30 rregan Exp $
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
import edu.harvard.fas.rregan.requel.project.GlossaryTerm;
import edu.harvard.fas.rregan.requel.project.Goal;
import edu.harvard.fas.rregan.requel.project.GoalRelation;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.command.CopyGoalCommand;
import edu.harvard.fas.rregan.requel.project.command.EditGoalRelationCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.impl.GoalImpl;
import edu.harvard.fas.rregan.requel.project.impl.assistant.AssistantFacade;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.UserRepository;

/**
 * @author ron
 */
@Controller("copyGoalCommand")
@Scope("prototype")
public class CopyGoalCommandImpl extends AbstractEditProjectCommand implements CopyGoalCommand {

	private Goal originalGoal;
	private Goal newGoal;
	private String newGoalName;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 * @param projectCommandFactory
	 * @param annotationCommandFactory
	 * @param commandHandler
	 */
	@Autowired
	public CopyGoalCommandImpl(AssistantFacade assistantManager, UserRepository userRepository,
			ProjectRepository projectRepository, ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	protected Goal getOriginalGoal() {
		return originalGoal;
	}

	public void setOriginalGoal(Goal originalGoal) {
		this.originalGoal = originalGoal;
	}

	protected String getNewGoalName() {
		return newGoalName;
	}

	public void setNewGoalName(String newGoalName) {
		this.newGoalName = newGoalName;
	}

	@Override
	public Goal getNewGoal() {
		return newGoal;
	}

	protected void setNewGoal(Goal newGoal) {
		this.newGoal = newGoal;
	}

	@Override
	public void execute() throws Exception {
		User editedBy = getRepository().get(getEditedBy());
		GoalImpl originalGoal = (GoalImpl) getRepository().get(getOriginalGoal());

		String newName;
		if ((getNewGoalName() == null) || (getNewGoalName().length() == 0)) {
			newName = generateNewGoalName(originalGoal.getName());
		} else {
			newName = generateNewGoalName(getNewGoalName());
		}
		GoalImpl newGoal = getProjectRepository().persist(
				new GoalImpl(originalGoal.getProjectOrDomain(), editedBy, newName, originalGoal
						.getText()));

		for (GoalRelation goalRelation : originalGoal.getRelationsFromThisGoal()) {
			EditGoalRelationCommand editGoalRelationCommand = getProjectCommandFactory()
					.newEditGoalRelationCommand();
			editGoalRelationCommand.setEditedBy(editedBy);
			editGoalRelationCommand.setFromGoal(newGoal.getName());
			editGoalRelationCommand.setToGoal(goalRelation.getToGoal().getName());
			editGoalRelationCommand.setProjectOrDomain(newGoal.getProjectOrDomain());
			editGoalRelationCommand.setRelationType(goalRelation.getRelationType().name());
			editGoalRelationCommand = getCommandHandler().execute(editGoalRelationCommand);
			newGoal.getRelationsFromThisGoal().add(editGoalRelationCommand.getGoalRelation());
		}
		// TODO: this assumes that all annotations are appropriate for the new
		// goal
		for (Annotation annotation : originalGoal.getAnnotations()) {
			newGoal.getAnnotations().add(annotation);
			annotation.getAnnotatables().add(newGoal);
		}
		for (GlossaryTerm term : originalGoal.getGlossaryTerms()) {
			newGoal.getGlossaryTerms().add(term);
			term.getReferers().add(newGoal);
		}
		newGoal = getProjectRepository().merge(newGoal);
		setNewGoal(newGoal);
	}

	private String generateNewGoalName(String originalName) {
		String newName = originalName;
		try {
			getProjectRepository().findGoalByProjectOrDomainAndName(
					getOriginalGoal().getProjectOrDomain(), newName);
			int counter = 1;
			newName = originalName + " " + counter;
			while (true) {
				try {
					getProjectRepository().findGoalByProjectOrDomainAndName(
							getOriginalGoal().getProjectOrDomain(), newName);
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
