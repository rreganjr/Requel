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
package com.rreganjr.requel.annotation.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.NoSuchAnnotationException;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.EditLexicalIssueCommand;
import com.rreganjr.requel.annotation.impl.LexicalIssue;
import com.rreganjr.requel.user.User;

/**
 * @author ron
 */
@Controller("editLexicalIssueCommand")
@Scope("prototype")
public class EditLexicalIssueCommandImpl extends EditIssueCommandImpl implements
		EditLexicalIssueCommand {

	private String annotatableEntityPropertyName;
	private String word;

	/**
	 * @param commandHandler
	 * @param annotationCommandFactory
	 * @param repository
	 */
	@Autowired
	public EditLexicalIssueCommandImpl(CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory, AnnotationRepository repository) {
		super(commandHandler, annotationCommandFactory, repository);
	}

	protected String getAnnotatableEntityPropertyName() {
		return annotatableEntityPropertyName;
	}

	public void setAnnotatableEntityPropertyName(String annotatableEntityPropertyName) {
		this.annotatableEntityPropertyName = annotatableEntityPropertyName;
	}

	protected String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}

	@Override
	public void execute() {
		User editedBy = getRepository().get(getEditedBy());
		Object groupingObject = getRepository().get(getGroupingObject());
		LexicalIssue issue = (LexicalIssue) getIssue();
		Annotatable annotatable = getRepository().get(getAnnotatable());
		if (issue == null) {
			try {
				// search for an existing issue
				if (getAnnotatableEntityPropertyName() == null) {
					issue = getAnnotationRepository().findLexicalIssue(getGroupingObject(),
							annotatable, getWord());
				} else {
					issue = getAnnotationRepository().findLexicalIssue(getGroupingObject(),
							annotatable, getWord(), getAnnotatableEntityPropertyName());
				}
			} catch (NoSuchAnnotationException e) {
				issue = getRepository().persist(
						new LexicalIssue(groupingObject, getText(), getMustBeResolved(), editedBy,
								getAnnotatableEntityPropertyName(), getWord()));
			}
		} else {
			if (getText() != null) {
				issue.setText(getText());
			}
			if (getWord() != null) {
				issue.setWord(getWord());
			}
			issue = getRepository().merge(issue);
		}
		if (annotatable != null) {
			issue.getAnnotatables().add(annotatable);
		}
		setIssue(issue);
		// add the issue to the annotatable after it has been merged so that if
		// it is a proxy it will be unwrapped by the framework.
		if (annotatable != null) {
			annotatable.getAnnotations().add(issue);
			setAnnotatable(annotatable);
		}
	}
}
