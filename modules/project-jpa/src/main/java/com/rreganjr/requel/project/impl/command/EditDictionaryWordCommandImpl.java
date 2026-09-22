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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.nlp.dictionary.DictionaryRepository;
import com.rreganjr.nlp.dictionary.impl.command.AbstractDictionaryCommand;
import com.rreganjr.platform.command.AuthorizableCommand;
import com.rreganjr.platform.command.AuthorizationRequirement;
import com.rreganjr.platform.command.AuthorizationRequirement.RequiresStakeholderPermission;
import com.rreganjr.platform.exception.EntityExceptionActionType;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectScopedCommand;
import com.rreganjr.requel.project.command.EditProjectDictionaryWordCommand;
import com.rreganjr.validator.EntityValidationException;

/**
 * Adds a word to one project's dictionary.
 * <p>
 * Lives in {@code project-jpa} rather than beside the other dictionary commands in
 * {@code dictionary-jpa} (issue #312). {@code RequiresStakeholderPermission} is only evaluated for
 * a command that implements {@link ProjectScopedCommand}, which hands back a {@link Project};
 * {@code dictionary-jpa} cannot see one, because {@code project-domain} depends on
 * {@code dictionary-jpa} and the reverse edge would be a cycle — which is why #313 keyed the word
 * to a bare {@code Long}. Two of the four resolve commands
 * ({@code ResolveIssueWithAddActorPositionCommandImpl},
 * {@code ResolveIssueWithAddGlossaryTermPositionCommandImpl}) live here for the same reason.
 * <p>
 * It extends {@link AbstractDictionaryCommand} deliberately, and <em>not</em>
 * {@code AbstractProjectCommand} or {@code AbstractEditCommand}: both of those implement
 * {@code AuthorizationExemptable}, and {@code AuthorizingCommandHandler} honours that flag and
 * returns <em>before</em> it reaches the {@code AuthorizableCommand} branch. Re-parenting this
 * class onto either would disable the gate below without a word in any test but
 * {@code AnnotationCommandTest}'s direct-execution case.
 *
 * @author ron
 */
@Controller("editDictionaryWordCommand")
@Scope("prototype")
public class EditDictionaryWordCommandImpl extends AbstractDictionaryCommand
		implements EditProjectDictionaryWordCommand, AuthorizableCommand {

	private String lemma;

	private Project project;

	private Long projectId;

	private User editedBy;

	/**
	 * @param dictionaryRepository
	 */
	@Autowired
	public EditDictionaryWordCommandImpl(DictionaryRepository dictionaryRepository) {
		super(dictionaryRepository);
	}

	protected String getLemma() {
		return lemma;
	}

	@Override
	public void setLemma(String lemma) {
		this.lemma = lemma;
	}

	protected Long getProjectId() {
		return projectId;
	}

	/**
	 * Setting the id alone leaves {@link #getProject()} null, and a command with no project is
	 * refused by {@code AuthorizingCommandHandler} — no project, no stakeholder. Callers that hold
	 * a {@link Project} use {@link #setProject(Project)}; this exists because
	 * {@code EditDictionaryWordCommand} is declared in a module that cannot name one.
	 */
	@Override
	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

	@Override
	public void setProject(Project project) {
		this.project = project;
		this.projectId = (project == null ? null : project.getId());
	}

	@Override
	public User getEditedBy() {
		return editedBy;
	}

	@Override
	public void setEditedBy(User editedBy) {
		this.editedBy = editedBy;
	}

	/**
	 * The project whose dictionary is being written, for the stakeholder check — the instance the
	 * caller passed, not one re-loaded here.
	 * <p>
	 * Re-loading it by id was the first attempt and it refused every caller, including the
	 * project's own creator. {@code Project.getStakeholders()} is {@code FetchType.LAZY} and the
	 * authorization check runs outside the command's transaction, so the freshly-loaded project
	 * could not initialize the collection the check walks, and the failure presented as "user is
	 * not a stakeholder". Every other {@code ProjectScopedCommand} here holds the project the
	 * caller resolved; this one does too.
	 * <p>
	 * Null when no project was set, which {@code AuthorizingCommandHandler} turns into a refusal.
	 */
	@Override
	public Project getProject() {
		return project;
	}

	/**
	 * Adding a word to a project's dictionary is an annotation write, so it reuses the permission
	 * the enclosing resolve already requires (issue #305) rather than introducing a permission type
	 * of its own. A stakeholder who can resolve the lexical issue can add the word; one who cannot,
	 * cannot — including when this command is executed on its own rather than through the resolver.
	 */
	@Override
	public AuthorizationRequirement getAuthorizationRequirement() {
		return new RequiresStakeholderPermission(Annotation.class, "Edit");
	}

	/**
	 * @see com.rreganjr.command.Command#execute()
	 */
	@Override
	public void execute() {
		validate();
		getDictionaryRepository().addToDictionary(getProjectId(), getLemma());
	}

	/**
	 * The exception is <em>not</em> swallowed (issue #312). The caller runs this command before
	 * marking the issue resolved, so letting a failed or refused write propagate is what keeps the
	 * issue unresolved; catching it here reported a write that never happened as a successful
	 * resolve.
	 */
	protected void validate() {
		if (getProjectId() == null) {
			throw EntityValidationException.emptyRequiredProperty(Project.class, null, "projectId",
					EntityExceptionActionType.Updating);
		}
	}
}
