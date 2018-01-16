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
package com.rreganjr.requel.annotation;

import com.rreganjr.repository.EntityExceptionActionType;
import com.rreganjr.requel.NoSuchEntityException;
import com.rreganjr.requel.annotation.impl.LexicalIssue;
import com.rreganjr.requel.project.ProjectOrDomain;

/**
 * @author ron
 */
public class NoSuchPositionException extends NoSuchEntityException {
	static final long serialVersionUID = 0;

	protected static String MSG_FOR_TEXT = "No position exists with text '%s'";
	protected static String MSG_FOR_ADDING_WORD_TO_DICTIONARY = "No 'add word to dictionary' position exists for word '%s'";
	protected static String MSG_FOR_ADDING_WORD_TO_GLOSSARY = "No 'add word to glossary of %s' position exists for word '%s'";
	protected static String MSG_FOR_ADDING_ACTOR_TO_PROJECT = "No 'add actor to project %s' position exists for name '%s'";
	protected static String MSG_FOR_CHANGING_SPELLING = "No 'change spelling' position exists for unknown word '%s' and proposed word '%s'";

	/**
	 * @param text -
	 *            the text used to search for a position that didn't exist
	 * @return
	 */
	public static NoSuchPositionException forText(String text) {
		return new NoSuchPositionException(Position.class, null, "text", text,
				EntityExceptionActionType.Reading, MSG_FOR_TEXT, text);
	}

	/**
	 * @param word -
	 *            the word used to search for a position that didn't exist
	 * @return
	 */
	public static NoSuchPositionException forAddingWordToDictionary(String word) {
		return new NoSuchPositionException(Position.class, null, "word", word,
				EntityExceptionActionType.Reading, MSG_FOR_ADDING_WORD_TO_DICTIONARY, word);
	}

	/**
	 * TODO: this violates project ref in annotation package
	 * 
	 * @param pod -
	 *            the project or domain
	 * @param word -
	 *            the word used to search for a position that didn't exist
	 * @return
	 */
	public static NoSuchPositionException forAddingWordToGlossary(ProjectOrDomain pod, String word) {
		return new NoSuchPositionException(Position.class, null, "word", word,
				EntityExceptionActionType.Reading, MSG_FOR_ADDING_WORD_TO_GLOSSARY, pod.getName(),
				word);
	}

	/**
	 * @param pod
	 * @param actorName
	 * @return
	 */
	public static NoSuchPositionException forAddingActorToProject(ProjectOrDomain pod,
			String actorName) {
		return new NoSuchPositionException(Position.class, null, "actorName", actorName,
				EntityExceptionActionType.Reading, MSG_FOR_ADDING_ACTOR_TO_PROJECT, pod.getName(),
				actorName);
	}

	/**
	 * @param issue
	 * @param misspelledWord
	 * @param proposedWord
	 * @return
	 */
	public static NoSuchPositionException forChangeSpelling(LexicalIssue issue, String proposedWord) {
		return new NoSuchPositionException(Position.class, null, null, null,
				EntityExceptionActionType.Reading, MSG_FOR_CHANGING_SPELLING, issue.getWord(),
				proposedWord);
	}

	/**
	 * @param format
	 * @param args
	 */
	protected NoSuchPositionException(Class<?> entityType, Object entity,
			String entityPropertyName, Object entityValue, EntityExceptionActionType actionType,
			String format, Object... messageArgs) {
		super(entityType, entity, entityPropertyName, entityValue, actionType, format, messageArgs);
	}

	/**
	 * @param cause
	 * @param format
	 * @param args
	 */
	protected NoSuchPositionException(Throwable cause, Class<?> entityType, Object entity,
			String entityPropertyName, Object entityValue, EntityExceptionActionType actionType,
			String format, Object... messageArgs) {
		super(cause, entityType, entity, entityPropertyName, entityValue, actionType, format,
				messageArgs);
	}
}
