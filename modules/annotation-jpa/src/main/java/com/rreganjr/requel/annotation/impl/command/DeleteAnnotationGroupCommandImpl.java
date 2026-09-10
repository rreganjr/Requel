/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
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

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.command.AuthorizationExemptable;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.Note;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.DeleteAnnotationGroupCommand;
import com.rreganjr.requel.annotation.command.DeleteIssueCommand;
import com.rreganjr.requel.annotation.command.DeleteNoteCommand;
import com.rreganjr.requel.annotation.impl.JpaAnnotationRepository;

/**
 * Delete every annotation grouped under an object (a project about to be deleted),
 * links first (issue #247). See {@link DeleteAnnotationGroupCommand}.
 *
 * @author ron
 */
@Controller("deleteAnnotationGroupCommand")
@Scope("prototype")
public class DeleteAnnotationGroupCommandImpl extends AbstractEditCommand implements
		DeleteAnnotationGroupCommand {
	private static final Logger log = Logger.getLogger(DeleteAnnotationGroupCommandImpl.class);

	private Object groupingObject;

	/**
	 * @param commandHandler
	 * @param annotationCommandFactory
	 * @param repository
	 */
	@Autowired
	public DeleteAnnotationGroupCommandImpl(CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory, AnnotationRepository repository) {
		super(commandHandler, annotationCommandFactory, repository);
	}

	@Override
	public Object getGroupingObject() {
		return groupingObject;
	}

	@Override
	public void setGroupingObject(Object groupingObject) {
		this.groupingObject = groupingObject;
	}

	@Override
	public void execute() throws Exception {
		Object groupingObject = getRepository().get(getGroupingObject());
		List<Long> annotationIds = getAnnotationRepository()
				.findAnnotationIdsByGroupingObject(groupingObject);
		if (annotationIds.isEmpty()) {
			return;
		}
		log.info("deleting " + annotationIds.size() + " annotation(s) still grouped under "
				+ groupingObject.getClass().getSimpleName() + " before its delete: " + annotationIds);

		for (Long annotationId : annotationIds) {
			Annotation annotation = getAnnotationRepository().findAnnotationById(annotationId);
			if (annotation == null) {
				continue; // deleted by an earlier step of the same cascade
			}
			// Drop the @ManyToAny link rows natively first: some may point at entity rows
			// that are already gone (the pre-#247 assistant race), which Hibernate cannot
			// resolve when it loads the collection to remove the annotation from its
			// annotatables. Then refresh so the session's view matches.
			getAnnotationRepository().unlinkAnnotation(annotationId);
			((JpaAnnotationRepository) getAnnotationRepository()).getEntityManager().refresh(annotation);

			if (annotation instanceof Issue issue) {
				DeleteIssueCommand deleteIssueCommand = getAnnotationCommandFactory()
						.newDeleteIssueCommand();
				deleteIssueCommand.setIssue(issue);
				deleteIssueCommand.setEditedBy(getEditedBy());
				// #69/#75: intrinsic sub-delete of an already-authorized parent delete.
				((AuthorizationExemptable) deleteIssueCommand).setAuthorizationExempt(true);
				getCommandHandler().execute(deleteIssueCommand);
			} else if (annotation instanceof Note note) {
				DeleteNoteCommand deleteNoteCommand = getAnnotationCommandFactory()
						.newDeleteNoteCommand();
				deleteNoteCommand.setNote(note);
				deleteNoteCommand.setEditedBy(getEditedBy());
				((AuthorizationExemptable) deleteNoteCommand).setAuthorizationExempt(true);
				getCommandHandler().execute(deleteNoteCommand);
			} else {
				log.warn("annotation " + annotationId + " of type " + annotation.getClass().getName()
						+ " has no delete command; removing directly");
				getRepository().delete(annotation);
			}
		}
	}
}
