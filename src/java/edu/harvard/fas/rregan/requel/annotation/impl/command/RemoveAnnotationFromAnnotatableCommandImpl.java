/*
 * $Id: RemoveAnnotationFromAnnotatableCommandImpl.java,v 1.5 2009/02/13 12:07:59 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.annotation.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.annotation.Annotatable;
import edu.harvard.fas.rregan.requel.annotation.Annotation;
import edu.harvard.fas.rregan.requel.annotation.AnnotationRepository;
import edu.harvard.fas.rregan.requel.annotation.Issue;
import edu.harvard.fas.rregan.requel.annotation.Note;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.annotation.command.DeleteIssueCommand;
import edu.harvard.fas.rregan.requel.annotation.command.DeleteNoteCommand;
import edu.harvard.fas.rregan.requel.annotation.command.RemoveAnnotationFromAnnotatableCommand;
import edu.harvard.fas.rregan.requel.user.User;

/**
 * @author ron
 */
@Controller("removeAnnotationFromAnnotatableCommand")
@Scope("prototype")
public class RemoveAnnotationFromAnnotatableCommandImpl extends AbstractEditCommand implements
		RemoveAnnotationFromAnnotatableCommand {

	private Annotatable annotatable;
	private Annotation annotation;
	private User editedBy;

	/**
	 * @param commandHandler
	 * @param annotationCommandFactory
	 * @param repository
	 */
	@Autowired
	public RemoveAnnotationFromAnnotatableCommandImpl(CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory, AnnotationRepository repository) {
		super(commandHandler, annotationCommandFactory, repository);
	}

	@Override
	public Annotatable getAnnotatable() {
		return annotatable;
	}

	@Override
	public void setAnnotatable(Annotatable annotatable) {
		this.annotatable = annotatable;
	}

	@Override
	public Annotation getAnnotation() {
		return annotation;
	}

	@Override
	public void setAnnotation(Annotation annotation) {
		this.annotation = annotation;
	}

	@Override
	protected User getEditedBy() {
		return editedBy;
	}

	@Override
	public void setEditedBy(User editedBy) {
		this.editedBy = editedBy;

	}

	@Override
	public void execute() throws Exception {
		Annotation annotation = getRepository().get(getAnnotation());
		Annotatable annotatable = getRepository().get(getAnnotatable());

		annotatable.getAnnotations().remove(annotation);
		annotation.getAnnotatables().remove(annotatable);

		// if an annotation has no annotatables it is deleted
		if (annotation.getAnnotatables().isEmpty()) {
			if (annotation instanceof Issue) {
				DeleteIssueCommand deleteIssueCommand = getAnnotationCommandFactory()
						.newDeleteIssueCommand();
				deleteIssueCommand.setIssue((Issue) annotation);
				deleteIssueCommand.setEditedBy(getEditedBy());
				getCommandHandler().execute(deleteIssueCommand);
			} else if (annotation instanceof Note) {
				DeleteNoteCommand deleteNoteCommand = getAnnotationCommandFactory()
						.newDeleteNoteCommand();
				deleteNoteCommand.setNote((Note) annotation);
				deleteNoteCommand.setEditedBy(getEditedBy());
				getCommandHandler().execute(deleteNoteCommand);
			}
		}
	}
}
