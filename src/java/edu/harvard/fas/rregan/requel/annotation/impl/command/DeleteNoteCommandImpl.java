/*
 * $Id: DeleteNoteCommandImpl.java,v 1.1 2009/02/13 12:07:59 rregan Exp $
 * Copyright (c) 2009 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.annotation.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.annotation.Annotatable;
import edu.harvard.fas.rregan.requel.annotation.AnnotationRepository;
import edu.harvard.fas.rregan.requel.annotation.Note;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.annotation.command.DeleteNoteCommand;

/**
 * @author ron
 */
@Controller("deleteNoteCommand")
@Scope("prototype")
public class DeleteNoteCommandImpl extends AbstractEditCommand implements DeleteNoteCommand {

	private Note note;

	/**
	 * @param commandHandler
	 * @param annotationCommandFactory
	 * @param repository
	 */
	@Autowired
	public DeleteNoteCommandImpl(CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory, AnnotationRepository repository) {
		super(commandHandler, annotationCommandFactory, repository);
	}

	/**
	 * @see edu.harvard.fas.rregan.requel.annotation.command.DeleteNoteCommand#setNote(edu.harvard.fas.rregan.requel.annotation.Note)
	 */
	@Override
	public void setNote(Note note) {
		this.note = note;
	}

	protected Note getNote() {
		return note;
	}

	/**
	 * @see edu.harvard.fas.rregan.command.Command#execute()
	 */
	@Override
	public void execute() throws Exception {
		Note note = getRepository().get(getNote());
		for (Annotatable annotatable : note.getAnnotatables()) {
			annotatable.getAnnotations().remove(note);
		}
		getRepository().delete(note);
	}

}
