/*
 * $Id: EditAddWordToGlossaryPositionCommandImpl.java,v 1.8 2009/02/13 12:07:55 rregan Exp $
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirments
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
package edu.harvard.fas.rregan.requel.project.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.annotation.AnnotationRepository;
import edu.harvard.fas.rregan.requel.annotation.NoSuchPositionException;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.annotation.impl.LexicalIssue;
import edu.harvard.fas.rregan.requel.annotation.impl.command.EditPositionCommandImpl;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.command.EditAddWordToGlossaryPositionCommand;
import edu.harvard.fas.rregan.requel.project.impl.AddGlossaryTermPosition;
import edu.harvard.fas.rregan.requel.user.User;

/**
 * @author ron
 */
@Controller("editAddWordToGlossaryPositionCommand")
@Scope("prototype")
public class EditAddWordToGlossaryPositionCommandImpl extends EditPositionCommandImpl implements
		EditAddWordToGlossaryPositionCommand {

	private final ProjectRepository projectRepository;
	private ProjectOrDomain projectOrDomain;

	/**
	 * @param commandHandler
	 * @param annotationCommandFactory
	 * @param annotationRepository
	 * @param projectRepository
	 */
	@Autowired
	public EditAddWordToGlossaryPositionCommandImpl(CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory,
			AnnotationRepository annotationRepository, ProjectRepository projectRepository) {
		super(commandHandler, annotationCommandFactory, annotationRepository);
		this.projectRepository = projectRepository;
	}

	@Override
	public void setProjectOrDomain(ProjectOrDomain projectOrDomain) {
		this.projectOrDomain = projectOrDomain;
	}

	protected ProjectOrDomain getProjectOrDomain() {
		return projectOrDomain;
	}

	@Override
	public AddGlossaryTermPosition getPosition() {
		return (AddGlossaryTermPosition) super.getPosition();
	}

	protected void setPosition(AddGlossaryTermPosition position) {
		super.setPosition(position);
	}

	protected ProjectRepository getProjectRepository() {
		return projectRepository;
	}

	@Override
	public void execute() throws Exception {
		ProjectOrDomain projectOrDomain = getRepository().get(getProjectOrDomain());
		User editedBy = getRepository().get(getEditedBy());
		LexicalIssue issue = (LexicalIssue) getRepository().get(getIssue());
		AddGlossaryTermPosition position = getPosition();
		if (position == null) {
			try {
				position = getProjectRepository().findAddGlossaryTermPosition(projectOrDomain,
						issue.getWord());
			} catch (NoSuchPositionException e) {
				position = getRepository()
						.persist(new AddGlossaryTermPosition(getText(), editedBy));
			}
		} else {
			position.setText(getText());
		}
		if (issue != null) {
			position.getIssues().add(issue);
		}
		position = getRepository().merge(position);
		setPosition(position);
		// add the position to the issue after it has been merged so that if it
		// is a proxy it will be unwrapped by the framework.
		if (issue != null) {
			issue.getPositions().add(position);
		}
		setIssue(issue);
	}
}
