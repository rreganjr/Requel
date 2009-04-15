/*
 * $Id: AnnotationRepository.java,v 1.11 2009/01/09 09:56:16 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.annotation;

import edu.harvard.fas.rregan.repository.Repository;
import edu.harvard.fas.rregan.requel.annotation.impl.AddWordToDictionaryPosition;
import edu.harvard.fas.rregan.requel.annotation.impl.ChangeSpellingPosition;
import edu.harvard.fas.rregan.requel.annotation.impl.LexicalIssue;

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
}
