/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025, 2026 Ron Regan Jr. All Rights Reserved.
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
import com.rreganjr.requel.annotation.impl.JpaAnnotationRepository;
import com.rreganjr.platform.identity.User;

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
	public User getEditedBy() {
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

		// Hibernate 6.5 bug: @ManyToAny collection removal generates invalid SQL for the
		// annotation_annotatable join table.  Work around by deleting the join-table row via
		// a native query (below) and never mutating the @ManyToAny collection in Java.
		JpaAnnotationRepository jpaRepo = (JpaAnnotationRepository) getAnnotationRepository();
		jakarta.persistence.PersistenceUnitUtil puu = jpaRepo.getEntityManager()
				.getEntityManagerFactory().getPersistenceUnitUtil();
		Long annotationId = (Long) puu.getIdentifier(annotation);
		Long annotatableId = (Long) puu.getIdentifier(annotatable);
		jpaRepo.removeAnnotatableFromAnnotationJoinTable(annotationId, annotatableId);
		// #247: keep the annotation MANAGED after the native join-table delete instead of
		// detaching it. Detaching left a detached IssueImpl/NoteImpl still reachable through
		// other managed cascade-PERSIST collections during a multi-entity delete (e.g.
		// DeleteProject/DeleteUseCase), so the next auto-flush threw "detached entity passed
		// to persist: IssueImpl". refresh() reloads the annotation's @ManyToAny state from the
		// DB (join row now gone) while keeping it attached, mirroring the working
		// RemoveActorFromActorContainerCommandImpl.
		jpaRepo.getEntityManager().refresh(annotation);

		// if an annotation has no annotatables it is deleted
		if (annotation.getAnnotatables().isEmpty()) {
			if (annotation instanceof Issue) {
				DeleteIssueCommand deleteIssueCommand = getAnnotationCommandFactory()
						.newDeleteIssueCommand();
				deleteIssueCommand.setIssue((Issue) annotation);
				deleteIssueCommand.setEditedBy(getEditedBy());
				// #69/#75: intrinsic sub-delete of an already-authorized parent delete; exempt so a
				// Delete-only stakeholder isn't re-checked for Annotation[Delete] mid-cascade.
				((com.rreganjr.platform.command.AuthorizationExemptable) deleteIssueCommand).setAuthorizationExempt(true);
				getCommandHandler().execute(deleteIssueCommand);
			} else if (annotation instanceof Note) {
				DeleteNoteCommand deleteNoteCommand = getAnnotationCommandFactory()
						.newDeleteNoteCommand();
				deleteNoteCommand.setNote((Note) annotation);
				deleteNoteCommand.setEditedBy(getEditedBy());
				// #69/#75: intrinsic sub-delete of an already-authorized parent delete; exempt so a
				// Delete-only stakeholder isn't re-checked for Annotation[Delete] mid-cascade.
				((com.rreganjr.platform.command.AuthorizationExemptable) deleteNoteCommand).setAuthorizationExempt(true);
				getCommandHandler().execute(deleteNoteCommand);
			}
		}
	}
}
