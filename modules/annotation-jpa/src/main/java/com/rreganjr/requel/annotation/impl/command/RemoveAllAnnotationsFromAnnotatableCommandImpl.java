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
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.Note;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.DeleteIssueCommand;
import com.rreganjr.requel.annotation.command.DeleteNoteCommand;
import com.rreganjr.requel.annotation.command.RemoveAllAnnotationsFromAnnotatableCommand;
import com.rreganjr.requel.annotation.impl.JpaAnnotationRepository;

/**
 * Unlink an entity about to be deleted from every annotation with index-backed
 * native deletes, then delete the annotations left with no annotatables (issue
 * #247). See {@link AnnotationRepository#unlinkAllAnnotations(Annotatable)} for
 * why this has to read database state instead of the session's snapshot.
 *
 * @author ron
 */
@Controller("removeAllAnnotationsFromAnnotatableCommand")
@Scope("prototype")
public class RemoveAllAnnotationsFromAnnotatableCommandImpl extends AbstractEditCommand implements
		RemoveAllAnnotationsFromAnnotatableCommand {
	private static final Logger log = Logger
			.getLogger(RemoveAllAnnotationsFromAnnotatableCommandImpl.class);

	private Annotatable annotatable;

	/**
	 * @param commandHandler
	 * @param annotationCommandFactory
	 * @param repository
	 */
	@Autowired
	public RemoveAllAnnotationsFromAnnotatableCommandImpl(CommandHandler commandHandler,
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
	public void execute() throws Exception {
		Annotatable annotatable = getRepository().get(getAnnotatable());

		// 1) Native, index-backed unlink of both halves of the relationship, against the
		// current database state (auto-flushes the session first).
		List<Long> linkedAnnotationIds = getAnnotationRepository().unlinkAllAnnotations(annotatable);
		if (linkedAnnotationIds.isEmpty()) {
			return;
		}
		log.info("unlinked " + linkedAnnotationIds.size() + " late annotation(s) from "
				+ annotatable.getClass().getSimpleName() + " before delete: " + linkedAnnotationIds);

		// 2) Reload every annotation that was linked - shared or not - so a @ManyToAny
		// collection loaded earlier in the cascade no longer holds the entity about to be
		// removed (a stale element fails the commit with "persistent instance references an
		// unsaved transient instance"), then delete the ones this was the last annotatable
		// of. With an empty loaded snapshot Hibernate also skips its own (broken for
		// @ManyToAny) join-table delete when the annotation row goes.
		for (Long annotationId : linkedAnnotationIds) {
			Annotation annotation = getAnnotationRepository().findAnnotationById(annotationId);
			if (annotation == null) {
				continue; // already gone (e.g. deleted by the per-annotation loop)
			}
			((JpaAnnotationRepository) getAnnotationRepository()).getEntityManager().refresh(annotation);
			if (!annotation.getAnnotatables().isEmpty()) {
				continue; // still annotates something else
			}
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
			}
		}
	}
}
