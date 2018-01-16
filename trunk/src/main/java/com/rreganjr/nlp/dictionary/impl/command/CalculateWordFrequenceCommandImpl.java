/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
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
package com.rreganjr.nlp.dictionary.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.nlp.dictionary.DictionaryRepository;
import com.rreganjr.nlp.dictionary.SemcorFile;
import com.rreganjr.nlp.dictionary.SemcorSentence;
import com.rreganjr.nlp.dictionary.command.CalculateWordFrequenceCommand;

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
	 * @see com.rreganjr.command.Command#execute()
	 */
	@Override
	public void execute() throws Exception {
		for (SemcorFile file : getDictionaryRepository().findSemcorFiles()) {
			for (SemcorSentence sentence : file.getSentences()) {
			}
		}

	}

}
