/*
 * $Id: ResolveIssueWithAddGlossaryTermPositionCommandImpl.java,v 1.7 2009/02/13 12:07:55 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project.impl.command;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.annotation.Annotatable;
import edu.harvard.fas.rregan.requel.annotation.AnnotationRepository;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.annotation.impl.LexicalIssue;
import edu.harvard.fas.rregan.requel.annotation.impl.command.ResolveIssueCommandImpl;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity;
import edu.harvard.fas.rregan.requel.project.command.EditGlossaryTermCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.impl.AddGlossaryTermPosition;
import edu.harvard.fas.rregan.requel.user.User;

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