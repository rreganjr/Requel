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
package com.rreganjr.requel.project.impl.command;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.AbstractCommand;
import com.rreganjr.command.Command;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.command.AuthorizableCommand;
import com.rreganjr.platform.command.AuthorizationRequirement;
import com.rreganjr.platform.command.AuthorizationRequirement.RequiresStakeholderPermission;
import com.rreganjr.platform.exception.EntityExceptionActionType;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.spi.AnnotatableTextEditRegistry;
import com.rreganjr.requel.annotation.spi.AnnotatableTextEditor;
import com.rreganjr.requel.command.AnalyzableEditCommand;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.StakeholderAuthorizationChecker;
import com.rreganjr.requel.project.TermTextMatcher;
import com.rreganjr.requel.project.command.ReplaceTermTextCommand;
import com.rreganjr.requel.project.impl.assistant.ProjectOrDomainEntityAssistant;
import com.rreganjr.validator.EntityValidationException;

/**
 * Issue #348: rewrite a glossary term's synonym to the term's name in the term's referers, each
 * through its own {@code Edit*} command (built by the same text-editor registry the spelling fix
 * uses). The command's own authorization is none: every change is an edit command checked here
 * before anything is written (a refusal is recorded, not thrown, so the referers the caller may
 * edit are still changed) and again by the command chain when it runs.
 */
@Controller("replaceTermTextCommand")
@Scope("prototype")
public class ReplaceTermTextCommandImpl extends AbstractCommand
		implements ReplaceTermTextCommand, AuthorizableCommand {

	private static final List<String> PROPERTIES = List.of(ProjectOrDomainEntityAssistant.PROP_NAME,
			ProjectOrDomainEntityAssistant.PROP_TEXT);

	private final ProjectRepository projectRepository;
	private final AnnotatableTextEditRegistry textEditRegistry;
	private final CommandHandler commandHandler;

	private Project project;
	private GlossaryTerm glossaryTerm;
	private GlossaryTerm synonym;
	private List<Target> targets = List.of();
	private User editedBy;

	private final List<Outcome> applied = new ArrayList<>();
	private final List<Outcome> refused = new ArrayList<>();
	private final List<Outcome> unchanged = new ArrayList<>();

	@Autowired
	public ReplaceTermTextCommandImpl(ProjectRepository projectRepository,
			AnnotatableTextEditRegistry textEditRegistry, CommandHandler commandHandler) {
		super(projectRepository);
		this.projectRepository = projectRepository;
		this.textEditRegistry = textEditRegistry;
		this.commandHandler = commandHandler;
	}

	@Override
	public void setProject(Project project) {
		this.project = project;
	}

	@Override
	public Project getProject() {
		return project;
	}

	@Override
	public void setGlossaryTerm(GlossaryTerm term) {
		this.glossaryTerm = term;
	}

	@Override
	public void setSynonym(GlossaryTerm synonym) {
		this.synonym = synonym;
	}

	@Override
	public void setTargets(List<Target> targets) {
		this.targets = (targets == null) ? List.of() : List.copyOf(targets);
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
	public List<Outcome> getApplied() {
		return applied;
	}

	@Override
	public List<Outcome> getRefused() {
		return refused;
	}

	@Override
	public List<Outcome> getUnchanged() {
		return unchanged;
	}

	/** None of its own: each change is an edit command, authorized on its own. */
	@Override
	public AuthorizationRequirement getAuthorizationRequirement() {
		return null;
	}

	@Override
	public void execute() throws Exception {
		if (glossaryTerm == null) {
			throw EntityValidationException.emptyRequiredProperty(GlossaryTerm.class, null, "term",
					EntityExceptionActionType.Updating);
		}
		GlossaryTerm term = projectRepository.get(glossaryTerm);
		GlossaryTerm oldTerm = (synonym == null) ? null : projectRepository.get(synonym);
		if (oldTerm == null || !term.getAlternateTerms().contains(oldTerm)) {
			throw EntityValidationException.validationFailed(GlossaryTerm.class, "synonymId",
					"The synonym must be one of the term's synonyms.");
		}
		String from = oldTerm.getName();
		String to = term.getName();

		// Referers of the term, and of the synonym: the glossary check links text that uses the
		// old name to the synonym itself.
		Map<String, ProjectOrDomainEntity> referers = new LinkedHashMap<>();
		for (Set<ProjectOrDomainEntity> linked : List.of(term.getReferers(), oldTerm.getReferers())) {
			for (ProjectOrDomainEntity referer : linked) {
				if (referer != null) {
					referers.putIfAbsent(key(type(referer), referer.getId()), referer);
				}
			}
		}

		List<Command> edits = new ArrayList<>();
		for (Target target : new LinkedHashSet<>(targets)) {
			ProjectOrDomainEntity referer = referers.get(key(target.entityType(), target.entityId()));
			if (referer == null) {
				unchanged.add(new Outcome(target.entityType(), target.entityId(), null));
				continue;
			}
			Outcome outcome = new Outcome(type(referer), referer.getId(), referer.getName());
			List<Command> forReferer = editsFor(referer, from, to);
			if (forReferer.isEmpty()) {
				unchanged.add(outcome);
			} else if (!authorized(forReferer)) {
				refused.add(outcome);
			} else {
				edits.addAll(forReferer);
				applied.add(outcome);
			}
		}
		for (Command edit : edits) {
			commandHandler.execute(edit);
		}
	}

	/** One edit per property (Name, Text) that uses the synonym. */
	private List<Command> editsFor(ProjectOrDomainEntity referer, String from, String to) {
		Optional<AnnotatableTextEditor> editor = textEditRegistry.resolveTextEditor(referer.getClass());
		if (editor.isEmpty()) {
			return List.of();
		}
		List<Command> edits = new ArrayList<>();
		for (String property : PROPERTIES) {
			String current = editor.get().currentValue(referer, property);
			String replaced = TermTextMatcher.replace(current, from, to);
			if (replaced != null && !replaced.equals(current)) {
				Command edit = editor.get().newEditCommand(referer, property, replaced, editedBy);
				// The registry's edits skip analysis (a spelling fix is itself the outcome of
				// one); a rewrite should be analysed like any other edit.
				if (edit instanceof AnalyzableEditCommand analyzable) {
					analyzable.setAnalysisEnabled(true);
				}
				edits.add(edit);
			}
		}
		return edits;
	}

	/**
	 * Whether the caller may make every one of these edits. Only stakeholder permissions are
	 * pre-checked, which is what every {@code Edit*} command declares; anything else is left to
	 * the command chain, which still enforces it.
	 */
	private boolean authorized(List<Command> edits) {
		Set<String> checked = new LinkedHashSet<>();
		for (Command edit : edits) {
			if (edit instanceof AuthorizableCommand authorizable
					&& authorizable.getAuthorizationRequirement() instanceof RequiresStakeholderPermission req
					&& checked.add(req.entityType().getName() + "/" + req.permissionType())
					&& !StakeholderAuthorizationChecker.isSatisfied(project, editedBy,
							req.entityType(), req.permissionType())) {
				return false;
			}
		}
		return true;
	}

	private static String type(ProjectOrDomainEntity entity) {
		return entity.getProjectOrDomainEntityInterface().getSimpleName();
	}

	private static String key(String type, Long id) {
		return type + "#" + id;
	}
}
