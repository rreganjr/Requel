/*
 * $Id: CalculateWordFrequenceCommandImpl.java,v 1.1 2008/12/13 00:39:53 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.nlp.dictionary.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;
import edu.harvard.fas.rregan.nlp.dictionary.SemcorFile;
import edu.harvard.fas.rregan.nlp.dictionary.SemcorSentence;
import edu.harvard.fas.rregan.nlp.dictionary.command.CalculateWordFrequenceCommand;

/**
 * @author ron
 */
@Controller("calculateWordFrequenceCommand")
@Scope("prototype")
public class CalculateWordFrequenceCommandImpl extends AbstractDictionaryCommand implements
		CalculateWordFrequenceCommand {

	/**
	 * @param dictionaryRepository
	 * @param lemmatizer
	 */
	@Autowired
	public CalculateWordFrequenceCommandImpl(DictionaryRepository dictionaryRepository) {
		super(dictionaryRepository);
	}

	/**
	 * @see edu.harvard.fas.rregan.command.Command#execute()
	 */
	@Override
	public void execute() throws Exception {
		for (SemcorFile file : getDictionaryRepository().findSemcorFiles()) {
			for (SemcorSentence sentence : file.getSentences()) {
			}
		}

	}

}
