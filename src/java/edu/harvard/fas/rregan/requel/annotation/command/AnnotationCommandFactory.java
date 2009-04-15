/*
 * $Id: AnnotationCommandFactory.java,v 1.15 2009/02/23 08:49:49 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.annotation.command;

import edu.harvard.fas.rregan.command.CommandFactory;
import edu.harvard.fas.rregan.requel.annotation.Position;

/**
 * @author ron
 */
public interface AnnotationCommandFactory extends CommandFactory {

	/**
	 * @return a new EditNoteCommand for creating or editing a note annotation.
	 */
	public EditNoteCommand newEditNoteCommand();

	/**
	 * @return a new EditIssueCommand for creating or editing an issue
	 *         annotation.
	 */
	public EditIssueCommand newEditIssueCommand();

	/**
	 * @return a new EditLexicalIssueCommand for creating or editing an issue
	 *         annotation related to word issues, like spelling, glossary
	 *         entries etc.
	 */
	public EditLexicalIssueCommand newEditLexicalIssueCommand();

	/**
	 * @param position -
	 *            the position used to determine the type of ResolveIssueCommand
	 *            to return.
	 * @return a new ResolveIssueCommand for resolving an issue annotation.
	 */
	public ResolveIssueCommand newResolveIssueCommand(Position position);

	/**
	 * @return a new EditPositionCommand for creating or editing a position of
	 *         an issue.
	 */
	public EditPositionCommand newEditPositionCommand();

	/**
	 * @return a new EditChangeSpellingPositionCommand for creating or editing a
	 *         position that indicates a possible spelling correction.
	 */
	public EditChangeSpellingPositionCommand newEditChangeSpellingPositionCommand();

	/**
	 * @return a new EditAddWordToDictionaryPositionCommand for adding
	 */
	public EditAddWordToDictionaryPositionCommand newEditAddWordToDictionaryPositionCommand();

	/**
	 * @return a new EditArgumentCommand for creating or editing an argument for
	 *         or against a position of an issue.
	 */
	public EditArgumentCommand newEditArgumentCommand();

	/**
	 * @return a new RemoveAnnotationFromAnnotatableCommand for removing an
	 *         annotation from an annotatable object and cleaning up all
	 *         references.
	 */
	public RemoveAnnotationFromAnnotatableCommand newRemoveAnnotationFromAnnotatableCommand();

	/**
	 * @return a new DeleteNoteCommand for deleting a note from the system and
	 *         cleaning up all references from annotatable objects.
	 */
	public DeleteNoteCommand newDeleteNoteCommand();

	/**
	 * @return a new DeleteIssueCommand for deleting an issue and its components
	 *         from the system and cleaning up all references from annotatable
	 *         objects.
	 */
	public DeleteIssueCommand newDeleteIssueCommand();

	/**
	 * @return a new DeletePositionCommand for deleting a position from the
	 *         system and cleaning up all references from issues.
	 */
	public DeletePositionCommand newDeletePositionCommand();

	/**
	 * @return a new DeleteArgumentCommand for deleting an argument from the
	 *         system and cleaning up all references from positions.
	 */
	public DeleteArgumentCommand newDeleteArgumentCommand();

}
