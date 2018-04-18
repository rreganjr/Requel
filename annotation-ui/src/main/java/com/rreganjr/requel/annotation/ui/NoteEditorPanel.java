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
package com.rreganjr.requel.annotation.ui;

import java.text.MessageFormat;

import nextapp.echo2.app.TextArea;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.validator.InvalidStateException;
import org.hibernate.validator.InvalidValue;

import echopointng.text.StringDocumentEx;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.EntityException;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.Note;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.DeleteNoteCommand;
import com.rreganjr.requel.annotation.command.EditNoteCommand;
import net.sf.echopm.navigation.event.DeletedEntityEvent;
import net.sf.echopm.navigation.event.UpdateEntityEvent;

/**
 * @author ron
 */
public class NoteEditorPanel extends AbstractRequelAnnotationEditorPanel {
	private static final Log log = LogFactory.getLog(NoteEditorPanel.class);

	static final long serialVersionUID = 0L;

	/**
	 * The name to use in the NoteEditorPanel.properties file to set the label
	 * of the note text field. If the property is undefined "Note" is used.
	 */
	public static final String PROP_LABEL_TEXT = "Text.Label";

	private final AnnotationCommandFactory annotationCommandFactory;
	private UpdateListener updateListener;

	// this is set by the DeleteListener so that the UpdateListener can ignore
	// events between when the object was deleted and the panel goes away.
	private boolean deleted = false;

	/**
	 * @param commandHandler
	 * @param annotationCommandFactory
	 * @param annotationRepository
	 */
	public NoteEditorPanel(CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory,
			AnnotationRepository annotationRepository) {
		this(NoteEditorPanel.class.getName(), commandHandler, annotationCommandFactory,
				annotationRepository);
	}

	/**
	 * @param resourceBundleName
	 * @param commandHandler
	 * @param annotationCommandFactory
	 * @param annotationRepository
	 */
	public NoteEditorPanel(String resourceBundleName, CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory,
			AnnotationRepository annotationRepository) {
		super(resourceBundleName, Note.class, commandHandler, annotationRepository);
		this.annotationCommandFactory = annotationCommandFactory;
	}

	/**
	 * If the editor is editing an existing note the title specified in the
	 * properties file as PROP_EXISTING_OBJECT_PANEL_TITLE If that property is
	 * not set it then tries the standard PROP_PANEL_TITLE and if that does not
	 * exist it defaults to:<br>
	 * "Edit Note"<br>
	 * For a new note it first tries PROP_NEW_OBJECT_PANEL_TITLE, then
	 * PROP_PANEL_TITLE and finally defaults to:<br>
	 * "New Note"<br>
	 * 
	 * @see net.sf.echopm.panel.editor.AbstractEditorPanel#PROP_EXISTING_OBJECT_PANEL_TITLE
	 * @see net.sf.echopm.panel.editor.AbstractEditorPanel#PROP_NEW_OBJECT_PANEL_TITLE
	 * @see net.sf.echopm.panel.Panel#PROP_PANEL_TITLE
	 * @see net.sf.echopm.panel.AbstractPanel#getTitle()
	 */
	@Override
	public String getTitle() {
		if (getNote() != null) {
			String msgPattern = getResourceBundleHelper(getLocale()).getString(
					PROP_EXISTING_OBJECT_PANEL_TITLE,
					getResourceBundleHelper(getLocale()).getString(PROP_PANEL_TITLE, "Edit Note"));
			return MessageFormat.format(msgPattern, getNote().toString());
		} else {
			String msg = getResourceBundleHelper(getLocale()).getString(
					PROP_NEW_OBJECT_PANEL_TITLE,
					getResourceBundleHelper(getLocale()).getString(PROP_PANEL_TITLE, "New Note"));
			return msg;
		}
	}

	@Override
	public void setup() {
		super.setup();
		Note note = getNote();
		if (note != null) {
			addInput("text", PROP_LABEL_TEXT, "Note", new TextArea(), new StringDocumentEx(note
					.getText()));
			addMultiRowInput("annotatables", AnnotationRefererTable.PROP_ANNOTATABLES_LABEL,
					"Referring Entities", new AnnotationRefererTable(this,
							getResourceBundleHelper(getLocale())), note);
		} else {
			addInput("text", PROP_LABEL_TEXT, "Note", new TextArea(), new StringDocumentEx(""));
			addMultiRowInput("annotatables", AnnotationRefererTable.PROP_ANNOTATABLES_LABEL,
					"Referring Entities", new AnnotationRefererTable(this,
							getResourceBundleHelper(getLocale())), null);
		}

		if (updateListener != null) {
			getEventDispatcher().removeEventTypeActionListener(UpdateEntityEvent.class,
					updateListener);
		}
		updateListener = new UpdateListener(this);
		getEventDispatcher().addEventTypeActionListener(UpdateEntityEvent.class, updateListener);

	}

	@Override
	public void dispose() {
		super.dispose();
		removeAll();
		if (updateListener != null) {
			getEventDispatcher().removeEventTypeActionListener(UpdateEntityEvent.class,
					updateListener);
			updateListener = null;
		}
	}

	@Override
	public void cancel() {
		super.cancel();
		if (updateListener != null) {
			getEventDispatcher().removeEventTypeActionListener(UpdateEntityEvent.class,
					updateListener);
		}
	}

	@Override
	public void save() {
		try {
			super.save();
			EditNoteCommand command = getAnnotationCommandFactory().newEditNoteCommand();
			command.setGroupingObject(getGroupingObject());
			command.setNote(getNote());
			command.setAnnotatable(getAnnotatable());
			command.setEditedBy(getCurrentUser());
			command.setText(getInputValue("text", String.class));
			command = getCommandHandler().execute(command);
			setValid(true);
			if (updateListener != null) {
				getEventDispatcher().removeEventTypeActionListener(UpdateEntityEvent.class,
						updateListener);
			}
			Note note = command.getNote();
			getEventDispatcher().dispatchEvent(new UpdateEntityEvent(this, note));
		} catch (EntityException e) {
			if ((e.getEntityPropertyNames() != null) && (e.getEntityPropertyNames().length > 0)) {
				for (String propertyName : e.getEntityPropertyNames()) {
					setValidationMessage(propertyName, e.getMessage());
				}
			} else if ((e.getCause() != null) && (e.getCause() instanceof InvalidStateException)) {
				InvalidStateException ise = (InvalidStateException) e.getCause();
				for (InvalidValue invalidValue : ise.getInvalidValues()) {
					String propertyName = invalidValue.getPropertyName();
					setValidationMessage(propertyName, invalidValue.getMessage());
				}
			} else {
				setGeneralMessage(e.toString());
			}
		} catch (Exception e) {
			log.error("could not save the goal: " + e, e);
			setGeneralMessage("Could not save: " + e);
		}
	}

	@Override
	public void delete() {
		try {
			Note note = getNote();
			DeleteNoteCommand deleteNoteCommand = getAnnotationCommandFactory()
					.newDeleteNoteCommand();
			deleteNoteCommand.setNote(note);
			deleteNoteCommand.setEditedBy(getCurrentUser());
			deleteNoteCommand = getCommandHandler().execute(deleteNoteCommand);
			deleted = true;
			getEventDispatcher().dispatchEvent(new DeletedEntityEvent(this, note));
		} catch (Exception e) {
			setGeneralMessage("Could not delete entity: " + e);
		}
	}

	private Note getNote() {
		if (getTargetObject() instanceof Note) {
			return (Note) getTargetObject();
		}
		return null;
	}

	private AnnotationCommandFactory getAnnotationCommandFactory() {
		return annotationCommandFactory;
	}

	// TODO: it may be better to have different standardized update listeners
	// for different types of updated or deleted objects associated with the
	// input controls like an annotatables table.
	private static class UpdateListener implements ActionListener {
		static final long serialVersionUID = 0L;

		private final NoteEditorPanel panel;

		private UpdateListener(NoteEditorPanel panel) {
			this.panel = panel;
		}

		public void actionPerformed(ActionEvent e) {
			if (panel.deleted) {
				return;
			}
			Note existingNote = panel.getNote();
			if ((e instanceof UpdateEntityEvent) && (existingNote != null)) {
				UpdateEntityEvent event = (UpdateEntityEvent) e;
				Note updatedNote = null;
				if (event.getObject() instanceof Note) {
					updatedNote = (Note) event.getObject();
					if ((event instanceof DeletedEntityEvent) && existingNote.equals(updatedNote)) {
						panel.deleted = true;
						panel.getEventDispatcher().dispatchEvent(
								new DeletedEntityEvent(this, panel, existingNote));
					}
				} else if ((event instanceof DeletedEntityEvent)
						&& (event.getObject() instanceof Annotatable)) {
					Annotatable annotatable = (Annotatable) event.getObject();
					if (existingNote.getAnnotatables().contains(annotatable)) {
						existingNote.getAnnotatables().remove(annotatable);
					}
					updatedNote = existingNote;
				}
				if ((updatedNote != null) && updatedNote.equals(existingNote)) {
					panel.setInputValue("text", updatedNote.getText());
					panel.setInputValue("annotatables", updatedNote);
					panel.setTargetObject(updatedNote);
				}
			}
		}
	}
}
