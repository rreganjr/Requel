/*
 * $Id: EditDictionaryWordCommandImpl.java,v 1.1 2008/12/13 00:39:53 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.nlp.dictionary.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;
import edu.harvard.fas.rregan.nlp.dictionary.command.EditDictionaryWordCommand;

/**
 * @author ron
 */
@Controller("editDictionaryWordCommand")
@Scope("prototype")
public class EditDictionaryWordCommandImpl extends AbstractDictionaryCommand implements
		EditDictionaryWordCommand {

	private String lemma;

	/**
	 * @param dictionaryRepository
	 */
	@Autowired
	public EditDictionaryWordCommandImpl(DictionaryRepository dictionaryRepository) {
		super(dictionaryRepository);
	}

	protected String getLemma() {
		return lemma;
	}

	public void setLemma(String lemma) {
		this.lemma = lemma;
	}

	/**
	 * @see edu.harvard.fas.rregan.command.Command#execute()
	 */
	@Override
	public void execute() {
		try {
			getDictionaryRepository().addToDictionary(getLemma());
		} catch (Exception e) {
			log.error(e, e);
		}
	}
}
