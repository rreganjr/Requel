/*
 * $Id: DeleteGlossaryTermCommandImpl.java,v 1.5 2009/03/30 11:54:31 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
