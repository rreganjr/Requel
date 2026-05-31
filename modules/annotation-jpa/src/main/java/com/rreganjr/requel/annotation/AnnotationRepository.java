/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025, 2026 Ron Regan Jr. All Rights Reserved.
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

import com.rreganjr.repository.Repository;
import com.rreganjr.requel.annotation.impl.AddWordToDictionaryPosition;
import com.rreganjr.requel.annotation.impl.ChangeSpellingPosition;
import com.rreganjr.requel.annotation.impl.LexicalIssue;

/**
 * @author ron
 */
public interface AnnotationRepository extends Repository {

	/**
	 * @param groupingObject -
	 *            An object used as the "owner" of a group of annotations.
	 * @param text -
	 *            the text of the position to match.
	 * @return
	 * @throws NoSuchPositionException
	 */
	public Position findPosition(Object groupingObject, String text) throws NoSuchPositionException;

	/**
	 * Find an existing position adding a word to the dictionary.
	 * 
	 * @param groupingObject -
	 *            An object used as the "owner" of a group of annotations.
	 * @param word -
	 *            the word to be added to the dictionary
	 * @return the position
	 * @throws NoSuchPositionException -
	 *             if an add word to dictionary position doesn't exist for the
	 *             supplied issue.
	 */
	public AddWordToDictionaryPosition findAddWordToDictionaryPosition(Object groupingObject,
			String word) throws NoSuchPositionException;

	/**
	 * Find an existing position on a specific issue for changing the spelling
	 * of a word.
	 * 
	 * @param issue
	 * @param proposedWord
	 * @return
	 * @throws NoSuchPositionException
	 */
	public ChangeSpellingPosition findChangeSpellingPosition(LexicalIssue issue, String proposedWord)
			throws NoSuchPositionException;

	/**
	 * Find a lexical issue where the word (text) matches the supplied word.
	 * 
	 * @param groupingObject -
	 *            An object used as the "owner" of a group of annotations.
	 * @param annotatable -
	 *            the annotated entity that the issue is attached to.
	 * @param word -
	 *            the word in question.
	 * @return
	 * @throws NoSuchAnnotationException
	 */
	public LexicalIssue findLexicalIssue(Object groupingObject, Annotatable annotatable, String word)
			throws NoSuchAnnotationException;

	/**
	 * Find a lexical issue where the word (text) matches the supplied word and
	 * the property name of the issue matches the annotatableEntityPropertyName.
	 * 
	 * @param groupingObject -
	 *            An object used as the "owner" of a group of annotations.
	 * @param annotatable -
	 *            the annotated entity that the issue is attached to.
	 * @param word -
	 *            the word in question.
	 * @param annotatableEntityPropertyName -
	 *            the property of the annotatable entity the issue is
	 *            concerning.
	 * @return
	 * @throws NoSuchAnnotationException
	 */
	public LexicalIssue findLexicalIssue(Object groupingObject, Annotatable annotatable,
			String word, String annotatableEntityPropertyName) throws NoSuchAnnotationException;

	/**
	 * Find an issue with the supplied message.
	 * 
	 * @param groupingObject -
	 *            An object used as the "owner" of a group of annotations.
	 * @param annotatable -
	 *            the annotated entity that the issue is attached to.
	 * @param message
	 * @return
	 */
	public Issue findIssue(Object groupingObject, Annotatable annotatable, String message);

	/**
	 * Find an note with the supplied annotatable.
	 *
	 * @param groupingObject -
	 *            An object used as the "owner" of a group of annotations.
	 * @param annotatable -
	 *            the annotated entity that the note is attached to.
	 * @param message
	 * @return
	 */
	public Note findNote(Object groupingObject, Annotatable annotatable, String message);

	/**
	 * Find an annotation by its persistent id, or {@code null} if none exists.
	 * {@code entityType} is the domain interface (e.g. {@link Issue}, {@link Note});
	 * the lookup is polymorphic, so a subtype row (e.g. a lexical issue) is returned
	 * for {@code Issue.class}. Ids are stable, so this is the preferred lookup for
	 * assistant findings that hold an {@code applied_annotation_id} reference.
	 *
	 * @param <T>
	 *            the annotation type
	 * @param entityType
	 *            the domain interface class of the annotation to load.
	 * @param id
	 *            the persistent id.
	 * @return the annotation, or {@code null} if no annotation of that type has the id.
	 */
	public <T> T findById(Class<T> entityType, Long id);

	/**
	 * Remove a single row from the annotation_annotatable join table using a
	 * native query. Required to work around a Hibernate 6.5 bug where
	 * {@code @ManyToAny} collection removal generates invalid parameterized SQL.
	 *
	 * @param annotationId  the id of the annotation
	 * @param annotatableId the id of the annotatable entity to unlink
	 */
	void removeAnnotatableFromAnnotationJoinTable(Long annotationId, Long annotatableId);
}
