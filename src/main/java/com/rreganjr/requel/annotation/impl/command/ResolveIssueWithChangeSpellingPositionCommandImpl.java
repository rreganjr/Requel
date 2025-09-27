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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jakarta.persistence.Transient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.repository.EntityExceptionActionType;
import com.rreganjr.requel.EntityValidationException;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.impl.AddWordToDictionaryPosition;
import com.rreganjr.requel.annotation.impl.ChangeSpellingPosition;
import com.rreganjr.requel.annotation.impl.LexicalIssue;

/**
 * @author ron
 */
@Controller("resolveIssueWithChangeSpellingPositionCommand")
@Scope("prototype")
public class ResolveIssueWithChangeSpellingPositionCommandImpl extends ResolveIssueCommandImpl {

	/**
	 * @param commandHandler
	 * @param annotationCommandFactory
	 * @param repository
	 */
	@Autowired
	public ResolveIssueWithChangeSpellingPositionCommandImpl(CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory, AnnotationRepository repository) {
		super(commandHandler, annotationCommandFactory, repository);
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
		if (getAnnotatable() == null) {
			// if a specific entity isn't specified fix the spelling in all of
			// them.
			for (Annotatable annotatable : issue.getAnnotatables()) {
				fixSpelling(annotatable, issue.getWord(), getPosition().getProposedWord());
			}
		} else {
			Annotatable annotatable = getRepository().get(getAnnotatable());
			fixSpelling(annotatable, issue.getWord(), getPosition().getProposedWord());
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

	protected void fixSpelling(Annotatable annotatable, String fromWord, String toWord)
			throws Exception {
		String textToChange = getTextToChange(annotatable);

		if (textToChange.contains(fromWord)) {
			// TODO: need better determination for proper case
			// of word changing to.
			if (Character.isUpperCase(fromWord.charAt(0))) {
				toWord = toWord.substring(0, 1).toUpperCase() + toWord.substring(1);
			}

			StringBuilder sb = new StringBuilder(textToChange.length() - fromWord.length()
					+ toWord.length());
			int start = textToChange.indexOf(fromWord);
			while (start != -1) {
				sb.setLength(0);
				int end = start + fromWord.length();
				sb.append(textToChange.substring(0, start));
				sb.append(toWord);
				sb.append(textToChange.substring(end));
				textToChange = sb.toString();
				start = textToChange.indexOf(fromWord);
			}
			// TODO: should this use a command or will it always be invoked from
			// inside a command?
			setChangedText(annotatable, textToChange);
		}
	}

	@Transient
	private String getTextToChange(Annotatable annotatable) throws IllegalAccessException,
			InvocationTargetException {
		Method getter = getPropertyGetter(annotatable, getIssue()
				.getAnnotatableEntityPropertyName());
		getter.setAccessible(true);
		return (String) getter.invoke(annotatable, new Object[] {});
	}

	@Transient
	private void setChangedText(Annotatable annotatable, String changedText)
			throws IllegalAccessException, InvocationTargetException {
		Method setter = getPropertySetter(annotatable, getIssue()
				.getAnnotatableEntityPropertyName());
		setter.setAccessible(true);
		setter.invoke(annotatable, changedText);
	}

	@Transient
	private Method getPropertyGetter(Annotatable annotatable, String propertyName) {
		Class<?> annotatableType = annotatable.getClass();
		while (annotatableType != null) {
			try {
				return annotatableType.getDeclaredMethod("get" + propertyName);
			} catch (NoSuchMethodException e) {
				annotatableType = annotatableType.getSuperclass();
			}
		}
		return null;
	}

	@Transient
	private Method getPropertySetter(Annotatable annotatable, String propertyName) {
		Class<?> annotatableType = annotatable.getClass();
		while (annotatableType != null) {
			try {
				return annotatableType.getDeclaredMethod("set" + propertyName, String.class);
			} catch (NoSuchMethodException e) {
				annotatableType = annotatableType.getSuperclass();
			}
		}
		return null;
	}
}