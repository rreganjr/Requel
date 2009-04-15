/*
 * $Id $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.nlp.dictionary.command;

import java.io.OutputStream;

import edu.harvard.fas.rregan.command.Command;

/**
 * Export a dictionary of categories, synsets, words, and senses
 * to an XML file.
 * 
 * @author ron
 */
public interface ExportDictionaryCommand extends Command {

	public void setStartingFrom(String startingFrom);

	public void setEndingAt(String endingAt);

	public void setOutputStream(OutputStream outputStream);

}