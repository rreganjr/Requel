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

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.Command;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.command.AuthorizableCommand;
import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.platform.command.AuthorizationRequirement.RequiresStakeholderPermission;
import com.rreganjr.platform.exception.EntityExceptionActionType;
import com.rreganjr.validator.EntityValidationException;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.impl.AddWordToDictionaryPosition;
import com.rreganjr.requel.annotation.impl.ChangeSpellingPosition;
import com.rreganjr.requel.annotation.impl.LexicalIssue;
import com.rreganjr.requel.annotation.spi.AnnotatableTextEditRegistry;
import com.rreganjr.requel.annotation.spi.AnnotatableTextEditor;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectScopedCommand;
import com.rreganjr.requel.project.StakeholderAuthorizationChecker;

/**
 * Resolves a spelling issue by correcting the word in every entity the issue applies to.
 * <p>
 * The correction runs through each entity's own {@code Edit*Command}, resolved from
 * {@link AnnotatableTextEditRegistry} (issue #305). It used to invoke setters reflectively on
 * the managed instances, so the write landed through dirty checking with no permission check,
 * no optimistic-lock check and no audit.
 * <p>
 * The resolve is all-or-nothing. A lexical issue can span entities of different types &mdash;
 * a goal, a story and an actor at once &mdash; and a user may hold {@code Edit} on some of
 * them but not others. Every edit is therefore built and authorized before any of them runs,
 * so a refusal leaves nothing changed and the issue unresolved. The command handler checks
 * each edit again as it executes; that is deliberate belt-and-braces, not redundancy, because
 * the pre-flight is about atomicity and the handler's check is the actual gate.
 *
 * @author ron
 */
@Controller("resolveIssueWithChangeSpellingPositionCommand")
@Scope("prototype")
public class ResolveIssueWithChangeSpellingPositionCommandImpl extends ResolveIssueCommandImpl {

	private final AnnotatableTextEditRegistry textEditRegistry;

	/**
	 * @param commandHandler
	 * @param annotationCommandFactory
	 * @param repository
	 * @param textEditRegistry
	 */
	@Autowired
	public ResolveIssueWithChangeSpellingPositionCommandImpl(CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory, AnnotationRepository repository,
			AnnotatableTextEditRegistry textEditRegistry) {
		super(commandHandler, annotationCommandFactory, repository);
		this.textEditRegistry = textEditRegistry;
	}

	@Override
	public LexicalIssue getIssue() {
		return (LexicalIssue) super.getIssue();
	}

	@Override
	protected ChangeSpellingPosition getPosition() {
		return (ChangeSpellingPosition) super.getPosition();
	}

	@Override
	public void execute() throws Exception {
		validate();
		LexicalIssue issue = getAnnotationRepository().get(getIssue());
		String propertyName = issue.getAnnotatableEntityPropertyName();
		String fromWord = issue.getWord();
		String toWord = getPosition().getProposedWord();

		List<Annotatable> targets = new ArrayList<Annotatable>();
		if (getAnnotatable() == null) {
			// No specific entity was named, so correct the word everywhere the issue applies.
			// This is the normal path: the resolve request carries only an issue and a
			// position, so the UI never names one.
			targets.addAll(issue.getAnnotatables());
		} else {
			targets.add(getRepository().get(getAnnotatable()));
		}

		List<Command> edits = new ArrayList<Command>();
		for (Annotatable annotatable : targets) {
			Command edit = newCorrection(annotatable, propertyName, fromWord, toWord);
			if (edit != null) {
				requireAuthorized(edit, annotatable);
				edits.add(edit);
			}
		}

		for (Command edit : edits) {
			getCommandHandler().execute(edit);
		}
		super.execute();
	}

	@Override
	protected void validate() {
		if (getIssue() == null) {
			throw EntityValidationException.emptyRequiredProperty(LexicalIssue.class, getIssue(),
					"issue", EntityExceptionActionType.Updating);
		}
		if (getPosition() == null) {
			throw EntityValidationException.emptyRequiredProperty(
					AddWordToDictionaryPosition.class, getPosition(), "position",
					EntityExceptionActionType.Updating);
		}
	}

	/**
	 * Build the edit that corrects one entity, or null when that entity's text does not
	 * contain the misspelled word and so needs no edit.
	 *
	 * @throws IllegalArgumentException if no text editor is registered for the entity's type,
	 *         which means an assistant annotates a type that cannot be corrected
	 */
	private Command newCorrection(Annotatable annotatable, String propertyName, String fromWord,
			String toWord) {
		AnnotatableTextEditor editor = textEditRegistry
				.resolveTextEditor(annotatable.getClass())
				.orElseThrow(() -> new IllegalArgumentException(
						"Cannot correct the spelling on a "
								+ annotatable.getClass().getSimpleName()
								+ ": no text editor is registered for that entity type"));

		String currentText = editor.currentValue(annotatable, propertyName);
		if ((currentText == null) || !currentText.contains(fromWord)) {
			return null;
		}
		// String.replace works on the original string's positions, so it never re-scans
		// replaced text and cannot loop when the replacement contains the original
		// (e.g. "act" -> "action").
		String corrected = currentText.replace(fromWord, matchCase(fromWord, toWord));
		return editor.newEditCommand(annotatable, propertyName, corrected, getEditedBy());
	}

	/**
	 * Refuse before anything is written if the user cannot make this edit.
	 * <p>
	 * Only {@code RequiresStakeholderPermission} is pre-checked, which is what every
	 * {@code Edit*Command} declares. Any other requirement is left to the command handler, so
	 * a new requirement type is enforced but not pre-flighted rather than silently skipped.
	 */
	private void requireAuthorized(Command edit, Annotatable annotatable) {
		if (!(edit instanceof AuthorizableCommand authorizable)) {
			return;
		}
		if (!(authorizable.getAuthorizationRequirement() instanceof RequiresStakeholderPermission requirement)) {
			return;
		}
		Project project = (edit instanceof ProjectScopedCommand scoped) ? scoped.getProject()
				: getProject();
		if (!StakeholderAuthorizationChecker.isSatisfied(project, getEditedBy(),
				requirement.entityType(), requirement.permissionType())) {
			throw new AuthorizationException("Requires stakeholder permission: "
					+ requirement.entityType().getSimpleName() + "["
					+ requirement.permissionType() + "] to correct the spelling in "
					+ annotatable.getClass().getSimpleName() + " " + annotatable);
		}
	}

	/**
	 * Carry the leading capital of the word being replaced onto its replacement.
	 */
	private static String matchCase(String fromWord, String toWord) {
		if (!fromWord.isEmpty() && Character.isUpperCase(fromWord.charAt(0))
				&& !toWord.isEmpty()) {
			return toWord.substring(0, 1).toUpperCase() + toWord.substring(1);
		}
		return toWord;
	}
}
