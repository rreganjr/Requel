/*
 * $Id: AbstractDictionaryCommand.java,v 1.1 2008/12/13 00:39:55 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.dictionary.impl.command;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.command.AbstractCommand;
import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;

/**
 * @author ron
 */
public abstract class AbstractDictionaryCommand extends AbstractCommand {
	protected static final Logger log = Logger.getLogger(AbstractDictionaryCommand.class);

	/**
	 * @param dictionaryRepository
	 */
	public AbstractDictionaryCommand(DictionaryRepository dictionaryRepository) {
		super(dictionaryRepository);
	}

	/**
	 * @return
	 */
	protected DictionaryRepository getDictionaryRepository() {
		return (DictionaryRepository) getRepository();
	}

}
