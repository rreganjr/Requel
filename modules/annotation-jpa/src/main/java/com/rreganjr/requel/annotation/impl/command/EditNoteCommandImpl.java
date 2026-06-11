/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
import com.rreganjr.platform.exception.EntityExceptionActionType;
import com.rreganjr.validator.EntityValidationException;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.NoSuchAnnotationException;
import com.rreganjr.requel.annotation.Note;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.EditNoteCommand;
import com.rreganjr.requel.annotation.impl.NoteImpl;
import com.rreganjr.platform.identity.User;

/**
 * Create or edit a note annotation on an annotatable entity.
 * 
 * @author ron
 */
@Controller("editNoteCommand")
@Scope("prototype")
public class EditNoteCommandImpl extends AbstractAnnotationCommand implements EditNoteCommand, com.rreganjr.requel.project.ProjectScopedCommand,
		com.rreganjr.platform.command.AuthorizableCommand {

	private Note note;

	/**
	 * @param commandHandler
	 * @param annotationCommandFactory
	 * @param repository
	 */
	@Autowired
	public EditNoteCommandImpl(CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory, AnnotationRepository repository) {
		super(commandHandler, annotationCommandFactory, repository);
	}

	public Note getNote() {
		return note;
	}

	public void setNote(Note note) {
		this.note = note;
	}

	@Override
	public void execute() {
		validate();
		User editedBy = getRepository().get(getEditedBy());
		Annotatable annotatable = getRepository().get(getAnnotatable());
		Object groupingObject = getRepository().get(getGroupingObject());

		NoteImpl noteImpl = (NoteImpl) getNote();
		if (noteImpl == null) {
			try {
				// see if an existing note exists for the given text
				noteImpl = (NoteImpl) getAnnotationRepository().findNote(groupingObject,
						annotatable, getText());
			} catch (NoSuchAnnotationException e) {
				noteImpl = getRepository().persist(
						new NoteImpl(groupingObject, getText(), editedBy));
			}
		} else {
			noteImpl.setText(getText());
			noteImpl = getRepository().merge(noteImpl);
		}
		if (annotatable != null) {
			noteImpl.getAnnotatables().add(annotatable);
		}
		setNote(noteImpl);
		// add the note to the annotatable after it has been merged so that if
		// it is a proxy it will be unwrapped by the framework.
		if (annotatable != null) {
			try {
				annotatable.getAnnotations().add(noteImpl);
				setAnnotatable(annotatable);
			} catch (Exception e) {
				// The annotatable may have been deleted concurrently (e.g. by async NLP
				// analysis running after an E2E test cleanup removed the entity).
				log.warn("could not attach note to annotatable (entity may have been deleted): "
						+ annotatable + " — " + e.getMessage());
			}
		}
	}

	protected void validate() {
		if ((getText() == null) || "".equals(getText().trim())) {
			throw EntityValidationException.emptyRequiredProperty(Note.class, getNote(), "text",
					EntityExceptionActionType.Updating);
		}
	}

	@Override
	public com.rreganjr.requel.project.Project getProject() {
		if (getGroupingObject() instanceof com.rreganjr.requel.project.Project gp) {
			return gp;
		}
		com.rreganjr.requel.project.Project p = AnnotationCommandProjectResolver.ofAnnotatable(getAnnotatable());
		if (p != null) {
			return p;
		}
		return AnnotationCommandProjectResolver.of(note);
	}

	@Override
	public com.rreganjr.platform.command.AuthorizationRequirement getAuthorizationRequirement() {
		return new com.rreganjr.platform.command.AuthorizationRequirement.RequiresStakeholderPermission(com.rreganjr.requel.annotation.Annotation.class, "Edit");
	}
}
