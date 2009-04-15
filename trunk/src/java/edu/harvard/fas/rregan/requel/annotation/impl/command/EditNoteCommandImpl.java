/*
 * $Id: EditNoteCommandImpl.java,v 1.14 2009/02/17 11:50:46 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.annotation.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.repository.EntityExceptionActionType;
import edu.harvard.fas.rregan.requel.EntityValidationException;
import edu.harvard.fas.rregan.requel.annotation.Annotatable;
import edu.harvard.fas.rregan.requel.annotation.AnnotationRepository;
import edu.harvard.fas.rregan.requel.annotation.NoSuchAnnotationException;
import edu.harvard.fas.rregan.requel.annotation.Note;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.annotation.command.EditNoteCommand;
import edu.harvard.fas.rregan.requel.annotation.impl.NoteImpl;
import edu.harvard.fas.rregan.requel.user.User;

/**
 * Create or edit a note annotation on an annotatable entity.
 * 
 * @author ron
 */
@Controller("editNoteCommand")
@Scope("prototype")
public class EditNoteCommandImpl extends AbstractAnnotationCommand implements EditNoteCommand {

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
			annotatable.getAnnotations().add(noteImpl);
			setAnnotatable(annotatable);
		}
	}

	protected void validate() {
		if ((getText() == null) || "".equals(getText().trim())) {
			throw EntityValidationException.emptyRequiredProperty(Note.class, getNote(), "text",
					EntityExceptionActionType.Updating);
		}
	}
}
