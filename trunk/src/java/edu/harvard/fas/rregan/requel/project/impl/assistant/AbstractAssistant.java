/*
 * $Id: AbstractAssistant.java,v 1.8 2009/02/12 11:01:36 rregan Exp $
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
package edu.harvard.fas.rregan.requel.project.impl.assistant;

import java.text.FieldPosition;
import java.text.MessageFormat;
import java.util.Locale;

import edu.harvard.fas.rregan.ApplicationException;
import edu.harvard.fas.rregan.ResourceBundleHelper;
import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.annotation.Annotatable;
import edu.harvard.fas.rregan.requel.annotation.Annotation;
import edu.harvard.fas.rregan.requel.annotation.AnnotationRepository;
import edu.harvard.fas.rregan.requel.annotation.Issue;
import edu.harvard.fas.rregan.requel.annotation.NoSuchAnnotationException;
import edu.harvard.fas.rregan.requel.annotation.NoSuchPositionException;
import edu.harvard.fas.rregan.requel.annotation.Note;
import edu.harvard.fas.rregan.requel.annotation.Position;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.annotation.command.EditIssueCommand;
import edu.harvard.fas.rregan.requel.annotation.command.EditNoteCommand;
import edu.harvard.fas.rregan.requel.annotation.command.EditPositionCommand;
import edu.harvard.fas.rregan.requel.annotation.command.RemoveAnnotationFromAnnotatableCommand;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity;
import edu.harvard.fas.rregan.requel.user.User;

/**
 * A base class for assistants that supports working with simple annotations.
 * 
 * @author ron
 */
public abstract class AbstractAssistant {

	private final AnnotationCommandFactory annotationCommandFactory;
	private final AnnotationRepository annotationRepository;
	private final CommandHandler commandHandler;
	private final ResourceBundleHelper resourceBundleHelper;

	protected AbstractAssistant(String resourceBundleName, CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory,
			AnnotationRepository annotationRepository) {
		this.commandHandler = commandHandler;
		this.annotationCommandFactory = annotationCommandFactory;
		this.annotationRepository = annotationRepository;
		this.resourceBundleHelper = new ResourceBundleHelper(resourceBundleName);
	}

	protected AnnotationCommandFactory getAnnotationCommandFactory() {
		return annotationCommandFactory;
	}

	protected AnnotationRepository getAnnotationRepository() {
		return annotationRepository;
	}

	protected CommandHandler getCommandHandler() {
		return commandHandler;
	}

	protected Note addNote(Object groupingObject, User assistantUser,
			Annotatable thingBeingAnalyzed, String noteText) throws Exception {
		EditNoteCommand editNoteCommand = annotationCommandFactory.newEditNoteCommand();
		editNoteCommand.setGroupingObject(groupingObject);
		editNoteCommand.setText(noteText);
		editNoteCommand.setAnnotatable(thingBeingAnalyzed);
		editNoteCommand.setEditedBy(assistantUser);
		editNoteCommand = commandHandler.execute(editNoteCommand);
		return editNoteCommand.getNote();
	}

	protected void addSimpleIssue(ProjectOrDomain projectOrDomain, User assistantUser,
			ProjectOrDomainEntity thingBeingAnalyzed, String text) throws Exception {
		EditIssueCommand editIssueCommand = getAnnotationCommandFactory().newEditIssueCommand();
		try {
			Issue issue = getAnnotationRepository().findIssue(projectOrDomain, thingBeingAnalyzed,
					text);
			editIssueCommand.setIssue(issue);
			editIssueCommand.setAnnotatable(thingBeingAnalyzed);
			editIssueCommand.setEditedBy(assistantUser);
			// don't mess up the existing properties when only adding an
			// annotatable
			editIssueCommand.setText(issue.getText());
			editIssueCommand.setMustBeResolved(issue.isMustBeResolved());
			editIssueCommand = getCommandHandler().execute(editIssueCommand);
		} catch (NoSuchAnnotationException e) {
			editIssueCommand.setGroupingObject(projectOrDomain);
			editIssueCommand.setText(text);
			editIssueCommand.setMustBeResolved(true);
			editIssueCommand.setAnnotatable(thingBeingAnalyzed);
			editIssueCommand.setEditedBy(assistantUser);
			editIssueCommand = getCommandHandler().execute(editIssueCommand);
			Issue issue = editIssueCommand.getIssue();
			addSimplePositionToIssue(projectOrDomain, assistantUser, issue, "Do nothing.");
		}
	}

	protected Position addSimplePositionToIssue(ProjectOrDomain projectOrDomain,
			User assistantUser, Issue issue, String positionText) throws Exception {
		EditPositionCommand editPositionCommand = getAnnotationCommandFactory()
				.newEditPositionCommand();
		try {
			editPositionCommand.setPosition(getAnnotationRepository().findPosition(projectOrDomain,
					positionText));
		} catch (NoSuchPositionException e) {
		}
		editPositionCommand.setIssue(issue);
		editPositionCommand.setEditedBy(assistantUser);
		editPositionCommand.setText(positionText);
		editPositionCommand = getCommandHandler().execute(editPositionCommand);
		return editPositionCommand.getPosition();
	}

	protected void removeAnnotation(Annotation annotation, Annotatable annotatable)
			throws Exception {
		RemoveAnnotationFromAnnotatableCommand command = getAnnotationCommandFactory()
				.newRemoveAnnotationFromAnnotatableCommand();
		command.setAnnotatable(annotatable);
		command.setAnnotation(annotation);
		command = getCommandHandler().execute(command);
	}

	protected ResourceBundleHelper getResourceBundleHelper() {
		return resourceBundleHelper;
	}

	/**
	 * @param locale
	 * @throws ApplicationException
	 *             if a resource bundle is not found for the locale
	 */
	public void setResourceLocale(Locale locale) {
		resourceBundleHelper.setLocale(locale);
	}

	private final StringBuffer messageBuffer = new StringBuffer(100);

	protected String createMessage(String propName, String defaultMsg, Object... arguments) {
		String msgPattern = getResourceBundleHelper().getString(propName, defaultMsg);
		MessageFormat msgFormat = new MessageFormat(msgPattern);
		messageBuffer.setLength(0);
		return msgFormat.format(arguments, messageBuffer, new FieldPosition(0)).toString();
	}
}
