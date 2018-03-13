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
package com.rreganjr.nlp.dictionary.impl.repository.init;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.rreganjr.AbstractSystemInitializer;
import com.rreganjr.nlp.dictionary.DictionaryRepository;
import com.rreganjr.nlp.dictionary.Word;

/**
 * Load the dictionary from SQL if no words exist.
 * 
 * @author ron
 */
@Component("dictionaryPhoneticCodeInitializer")
@Scope("prototype")
public class DictionaryPhoneticCodeInitializer extends AbstractSystemInitializer {

	private final DictionaryRepository dictionaryRepository;

	/**
	 * @param dictionaryRepository
	 * @param jdbcTemplate
	 */
	@Autowired
	public DictionaryPhoneticCodeInitializer(DictionaryRepository dictionaryRepository,
			JdbcTemplate jdbcTemplate) {
		super(2);
		this.dictionaryRepository = dictionaryRepository;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void initialize() {
		// TODO: this disables the initalizer and shouldn't be hard coded
		if (true) {
			return;
		}

		String phoneticCode = dictionaryRepository.findWord("a").getPhoneticCode();
		if ((phoneticCode == null) || (phoneticCode.length() == 0)) {
			try {
				log.info("initializing phonetic codes for word net words.");
				for (Word word : dictionaryRepository.findWords()) {
					word
							.setPhoneticCode(dictionaryRepository.generatePhoneticCode(word
									.getLemma()));
				}
			} catch (Exception e) {
				log.error("failed to initialize phonetic codes: " + e, e);
			}
		}
	}
}
