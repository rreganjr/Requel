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
package com.rreganjr.requel.annotation.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.Note;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.DeleteIssueCommand;
import com.rreganjr.requel.annotation.command.DeleteNoteCommand;
import com.rreganjr.requel.annotation.command.RemoveAnnotationFromAnnotatableCommand;
import com.rreganjr.requel.user.User;

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
