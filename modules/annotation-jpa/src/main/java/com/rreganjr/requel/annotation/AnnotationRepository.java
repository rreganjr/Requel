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
	 * Find any annotation (note, issue, position, argument, ...) by its persistent
	 * id, or {@code null} if none exists. Use when the concrete annotation type is
	 * not known up front (e.g. reconciling a finding's linked annotation).
	 *
	 * @param id
	 *            the persistent id.
	 * @return the annotation, or {@code null}.
	 */
	public Annotation findAnnotationById(Long id);

	/**
	 * Remove a single row from the annotation_annotatable join table using a
	 * native query. Required to work around a Hibernate 6.5 bug where
	 * {@code @ManyToAny} collection removal generates invalid parameterized SQL.
	 *
	 * @param annotationId  the id of the annotation
	 * @param annotatableId the id of the annotatable entity to unlink
	 */
	void removeAnnotatableFromAnnotationJoinTable(Long annotationId, Long annotatableId);

	/**
	 * #247: unlink an annotatable entity from <em>every</em> annotation with two
	 * index-backed native deletes, executed in the current transaction against the
	 * database's <em>current</em> state rather than the session's snapshot.
	 * <p>
	 * Clears both halves of the (doubly-mapped) relationship: the entity-owned
	 * {@code <table>_annotations} join table (derived from the entity's
	 * {@code annotations} collection mapping) and the annotation-owned
	 * {@code annotation_annotatable} {@code @ManyToAny} table, restricted to the
	 * entity's registered discriminator(s) so an Actor and a Goal that share a
	 * numeric id never clear each other's links.
	 * <p>
	 * Why native: when an entity is deleted, Hibernate skips the join-table delete
	 * for a collection whose loaded snapshot was empty. A background assistant that
	 * commits an annotation link between the entity's load and its delete therefore
	 * leaves a row that fails the FK ({@code goals_annotations}, ...) - the e2e 409s.
	 * Call this immediately before {@code delete(entity)}, after the Java-side
	 * annotation removal loop has run.
	 *
	 * @param annotatable
	 *            the managed annotatable entity about to be deleted.
	 * @return the ids of the annotations that were still linked to the entity in
	 *         {@code annotation_annotatable}; the caller decides whether any of them
	 *         are now orphans (no remaining annotatables) and should be deleted.
	 */
	java.util.List<Long> unlinkAllAnnotations(Annotatable annotatable);

	/**
	 * #247: every annotation whose {@code groupingObject} is the given object (a
	 * project), whether or not it still annotates anything.
	 *
	 * @param groupingObject
	 *            the grouping object.
	 * @return the ids of the annotations grouped under it, from the database.
	 */
	java.util.List<Long> findAnnotationIdsByGroupingObject(Object groupingObject);

	/**
	 * #247: delete every {@code annotation_annotatable} row of an annotation natively,
	 * against the database's current state, so the annotation can be deleted without
	 * Hibernate resolving links whose target rows may already be gone.
	 *
	 * @param annotationId
	 *            the annotation's id.
	 * @return the number of link rows deleted.
	 */
	int unlinkAnnotation(Long annotationId);
}
