/*
 * $Id: EditLexicalIssueCommand.java,v 1.2 2008/07/31 00:58:15 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.annotation.command;

/**
 * @author ron
 */
public interface EditLexicalIssueCommand extends EditIssueCommand {

	/**
	 * @param word
	 */
	public void setWord(String word);

	/**
	 * @param annotatableEntityPropertyName
	 */
	public void setAnnotatableEntityPropertyName(String annotatableEntityPropertyName);

}
