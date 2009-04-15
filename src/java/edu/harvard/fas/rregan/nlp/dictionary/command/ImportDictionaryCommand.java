/*
 * $Id $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.nlp.dictionary.command;

import java.io.InputStream;

import edu.harvard.fas.rregan.command.Command;

/**
 * Import a dictionary of categories, synsets, words, and senses 
 * from an XML file.
 *  
 * @author ron
 */
public interface ImportDictionaryCommand extends Command {

	/**
	 * 
	 * @param inputStream - the stream to the dictionary xml file.
	 */
	public void setInputStream(InputStream inputStream);

}