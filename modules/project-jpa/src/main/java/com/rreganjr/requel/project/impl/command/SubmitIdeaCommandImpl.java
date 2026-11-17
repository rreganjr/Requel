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

import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.command.AuthorizableCommand;
import com.rreganjr.platform.command.AuthorizationRequirement;
import com.rreganjr.platform.command.AuthorizationRequirement.RequiresStakeholderPermission;
import com.rreganjr.platform.exception.EntityExceptionActionType;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.project.Idea;
import com.rreganjr.requel.project.IdeaStore;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.command.SubmitIdeaCommand;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.validator.EntityValidationException;

/**
 * See {@link SubmitIdeaCommand}. The saved text is the typed text, then the file's text after a
 * blank line when both were given.
 */
@Controller("submitIdeaCommand")
@Scope("prototype")
public class SubmitIdeaCommandImpl extends AbstractProjectCommand
		implements SubmitIdeaCommand, AuthorizableCommand {

	static final int MIN_TEXT_LENGTH = 10;

	private Project project;
	private String text;
	private String fileText;
	private String fileName;
	private User editedBy;
	private Idea idea;
	private IdeaStore ideaStore;

	@Autowired
	public SubmitIdeaCommandImpl(AssistantFacade assistantManager, UserRepository userRepository,
			ProjectRepository projectRepository, ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler,
			IdeaStore ideaStore) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
		this.ideaStore = ideaStore;
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
	public void setText(String text) {
		this.text = text;
	}

	@Override
	public void setFileText(String fileText, String fileName) {
		this.fileText = fileText;
		this.fileName = fileName;
	}

	@Override
	public Idea getIdea() {
		return idea;
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
	public AuthorizationRequirement getAuthorizationRequirement() {
		return new RequiresStakeholderPermission(Project.class, "Edit");
	}

	@Override
	public void execute() throws Exception {
		if (project == null) {
			throw EntityValidationException.emptyRequiredProperty(Project.class, null, "project",
					EntityExceptionActionType.Updating);
		}
		String combined = combine(text, fileText);
		if (combined.isEmpty()) {
			throw new IllegalArgumentException("An idea needs some text.");
		}
		// A typed-only idea is held to a minimum length; text from a file is taken as given.
		if (fileText == null && combined.length() < MIN_TEXT_LENGTH) {
			throw new IllegalArgumentException(
					"The idea must be at least " + MIN_TEXT_LENGTH + " characters.");
		}
		idea = ideaStore.record(project.getId(), combined, fileName,
				getRepository().get(editedBy));
	}

	static String combine(String typed, String fromFile) {
		String a = typed == null ? "" : typed.strip();
		String b = fromFile == null ? "" : fromFile.strip();
		if (a.isEmpty()) {
			return b;
		}
		return b.isEmpty() ? a : a + "\n\n" + b;
	}
}
