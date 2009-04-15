/*
 * $Id $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.nlp.dictionary.command;

import edu.harvard.fas.rregan.command.Command;

/**
 * Create or Edit a word in the dictionary.
 * 
 * @author ron
 */
public interface EditDictionaryWordCommand extends Command {

	/**
	 * The text of the word.
	 * 
	 * @param lemma
	 */
	public void setLemma(String lemma);
}