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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.Command;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.exception.EntityExceptionActionType;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.impl.RewriteNamePosition;
import com.rreganjr.requel.annotation.spi.AnnotatableTextEditRegistry;
import com.rreganjr.requel.annotation.spi.AnnotatableTextEditor;
import com.rreganjr.validator.EntityValidationException;

/**
 * Issue #459: resolves an issue by setting the name of the entity it is on to the position's
 * proposed name, through that entity's own {@code Edit*Command} (the text-editor registry, as Fix
 * Spelling does), so the edit is authorized, versioned and analysed.
 * <p>
 * It is refused, before anything is written, when the name is no longer the one the proposal was
 * built from: a stale proposal would otherwise overwrite a name the user has since changed.
 */
@Controller("resolveIssueWithRewriteNamePositionCommand")
@Scope("prototype")
public class ResolveIssueWithRewriteNamePositionCommandImpl extends ResolveIssueCommandImpl {

	static final String PROPERTY = "Name";

	private final AnnotatableTextEditRegistry textEditRegistry;

	@Autowired
	public ResolveIssueWithRewriteNamePositionCommandImpl(CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory, AnnotationRepository repository,
			AnnotatableTextEditRegistry textEditRegistry) {
		super(commandHandler, annotationCommandFactory, repository);
		this.textEditRegistry = textEditRegistry;
	}

	@Override
	protected RewriteNamePosition getPosition() {
		return (RewriteNamePosition) org.hibernate.Hibernate.unproxy(super.getPosition());
	}

	@Override
	public void execute() throws Exception {
		validate();
		Issue issue = getAnnotationRepository().get(getIssue());
		RewriteNamePosition position = getPosition();

		List<Annotatable> targets = new ArrayList<Annotatable>();
		if (getAnnotatable() == null) {
			targets.addAll(issue.getAnnotatables());
		} else {
			targets.add(getRepository().get(getAnnotatable()));
		}

		List<Command> edits = new ArrayList<Command>();
		for (Annotatable annotatable : targets) {
			AnnotatableTextEditor editor = textEditRegistry
					.resolveTextEditor(annotatable.getClass())
					.orElseThrow(() -> new IllegalArgumentException("Cannot rename a "
							+ annotatable.getClass().getSimpleName()
							+ ": no text editor is registered for that entity type"));
			String current = editor.currentValue(annotatable, PROPERTY);
			if (!Objects.equals(current, position.getExpectedName())) {
				throw EntityValidationException.validationFailed(Issue.class, "positionId",
						"The name is now \"" + current + "\", not \"" + position.getExpectedName()
								+ "\", so this rewrite no longer applies. Re-run the analysis"
								+ " for a current one.");
			}
			if (Objects.equals(current, position.getProposedName())) {
				continue;
			}
			Command edit = editor.newEditCommand(annotatable, PROPERTY,
					position.getProposedName(), getEditedBy());
			TextEdits.requireAuthorized(edit, annotatable, getProject(), getEditedBy(),
					"rename");
			edits.add(edit);
		}

		for (Command edit : edits) {
			getCommandHandler().execute(edit);
		}
		super.execute();
	}

	@Override
	protected void validate() {
		super.validate();
		if (getPosition() == null || getPosition().getProposedName() == null
				|| getPosition().getProposedName().isBlank()) {
			throw EntityValidationException.emptyRequiredProperty(RewriteNamePosition.class,
					getPosition(), "proposedName", EntityExceptionActionType.Updating);
		}
	}
}
