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
package edu.harvard.fas.rregan.requel.project.impl.command;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.NoSuchEntityException;
import edu.harvard.fas.rregan.requel.annotation.Annotation;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.annotation.command.DeletePositionCommand;
import edu.harvard.fas.rregan.requel.annotation.command.RemoveAnnotationFromAnnotatableCommand;
import edu.harvard.fas.rregan.requel.project.GlossaryTerm;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.command.DeleteGlossaryTermCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.impl.AddGlossaryTermPosition;
import edu.harvard.fas.rregan.requel.project.impl.GlossaryTermImpl;
import edu.harvard.fas.rregan.requel.project.impl.assistant.AssistantFacade;
import edu.harvard.fas.rregan.requel.user.UserRepository;

/**
 * Delete a glossaryTerm from a project, cleaning up references from other
 * project entities, glossaryTerm relations and annotations.
 * 
 * @author ron
 */
@Controller("deleteGlossaryTermCommand")
@Scope("prototype")
public class DeleteGlossaryTermCommandImpl extends AbstractEditProjectCommand implements
		DeleteGlossaryTermCommand {

	private GlossaryTerm glossaryTerm;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 * @param projectCommandFactory
	 * @param annotationCommandFactory
	 * @param commandHandler
	 */
	@Autowired
	public DeleteGlossaryTermCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	@Override
	public void setGlossaryTerm(GlossaryTerm glossaryTerm) {
		this.glossaryTerm = glossaryTerm;
	}

	protected GlossaryTerm getGlossaryTerm() {
		return glossaryTerm;
	}

	@Override
	public void execute() throws Exception {
		GlossaryTerm glossaryTerm = getRepository().get(getGlossaryTerm());
		Set<Annotation> annotations = new HashSet<Annotation>(glossaryTerm.getAnnotations());
		for (Annotation annotation : annotations) {
			RemoveAnnotationFromAnnotatableCommand removeAnnotationFromAnnotatableCommand = getAnnotationCommandFactory()
					.newRemoveAnnotationFromAnnotatableCommand();
			removeAnnotationFromAnnotatableCommand.setEditedBy(getEditedBy());
			removeAnnotationFromAnnotatableCommand.setAnnotatable(glossaryTerm);
			removeAnnotationFromAnnotatableCommand.setAnnotation(annotation);
			getCommandHandler().execute(removeAnnotationFromAnnotatableCommand);
		}
		glossaryTerm.getReferers().clear();
		// if this is the canonical term for other glossary terms, the
		// other terms must be updated to set the canonical term to null.
		for (GlossaryTerm alternateTerm : glossaryTerm.getAlternateTerms()) {
			((GlossaryTermImpl) alternateTerm).setCanonicalTerm(null);
		}
		try {
			AddGlossaryTermPosition addGlossaryTermPosition = getProjectRepository()
					.findAddGlossaryTermPosition(glossaryTerm.getProjectOrDomain(),
							glossaryTerm.getName());
			DeletePositionCommand deletePositionCommand = getAnnotationCommandFactory()
					.newDeletePositionCommand();
			deletePositionCommand.setEditedBy(getEditedBy());
			deletePositionCommand.setPosition(addGlossaryTermPosition);
			getCommandHandler().execute(deletePositionCommand);
		} catch (NoSuchEntityException e) {

		}
		glossaryTerm.getProjectOrDomain().getGlossaryTerms().remove(glossaryTerm);
		getRepository().delete(glossaryTerm);
	}

}
