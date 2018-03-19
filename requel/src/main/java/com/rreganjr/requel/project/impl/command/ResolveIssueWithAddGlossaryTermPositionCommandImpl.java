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
package com.rreganjr.requel.project.impl.command;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.impl.LexicalIssue;
import com.rreganjr.requel.annotation.impl.command.ResolveIssueCommandImpl;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.command.EditGlossaryTermCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.AddGlossaryTermPosition;
import com.rreganjr.requel.user.User;

/**
 * This resolves an issue with the specified position by adding a word or phrase
 * to the project glossary.
 * 
 * @author ron
 */
@Controller("resolveIssueWithAddGlossaryTermPositionCommandImpl")
@Scope("prototype")
public class ResolveIssueWithAddGlossaryTermPositionCommandImpl extends ResolveIssueCommandImpl {

	private final ProjectCommandFactory projectCommandFactory;

	/**
	 * @param commandHandler
	 * @param annotationCommandFactory
	 * @param projectCommandFactory
	 * @param repository
	 */
	@Autowired
	public ResolveIssueWithAddGlossaryTermPositionCommandImpl(CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory,
			ProjectCommandFactory projectCommandFactory, AnnotationRepository repository) {
		super(commandHandler, annotationCommandFactory, repository);
		this.projectCommandFactory = projectCommandFactory;
	}

	@Override
	public LexicalIssue getIssue() {
		return (LexicalIssue) super.getIssue();
	}

	@Override
	protected AddGlossaryTermPosition getPosition() {
		return (AddGlossaryTermPosition) super.getPosition();
	}

	protected ProjectCommandFactory getProjectCommandFactory() {
		return projectCommandFactory;
	}

	@Override
	public void execute() throws Exception {
		LexicalIssue issue = getRepository().get(getIssue());
		User resolvedBy = getRepository().get(getEditedBy());

		EditGlossaryTermCommand command = getProjectCommandFactory().newEditGlossaryTermCommand();
		command.setName(issue.getWord());
		if (!issue.getAnnotatables().isEmpty()) {
			Set<ProjectOrDomainEntity> entities = new HashSet<ProjectOrDomainEntity>(issue
					.getAnnotatables().size());
			for (Annotatable annotatable : issue.getAnnotatables()) {
				if (annotatable instanceof ProjectOrDomainEntity) {
					entities.add((ProjectOrDomainEntity) annotatable);
				}
			}
			command.setAddReferers(entities);
		}
		command.setProjectOrDomain((ProjectOrDomain) issue.getGroupingObject());
		command.setEditedBy(resolvedBy);
		command = getCommandHandler().execute(command);
		super.execute();
	}
}