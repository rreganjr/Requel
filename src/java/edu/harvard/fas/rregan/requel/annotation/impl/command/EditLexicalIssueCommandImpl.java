/*
 * $Id: EditLexicalIssueCommandImpl.java,v 1.12 2009/02/16 10:10:08 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.annotation.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.annotation.Annotatable;
import edu.harvard.fas.rregan.requel.annotation.AnnotationRepository;
import edu.harvard.fas.rregan.requel.annotation.NoSuchAnnotationException;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.annotation.command.EditLexicalIssueCommand;
import edu.harvard.fas.rregan.requel.annotation.impl.LexicalIssue;
import edu.harvard.fas.rregan.requel.user.User;

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
