/*
 * $Id: DictionaryPhoneticCodeInitializer.java,v 1.3 2009/01/26 10:19:01 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.nlp.dictionary.impl.repository.init;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import edu.harvard.fas.rregan.AbstractSystemInitializer;
import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;
import edu.harvard.fas.rregan.nlp.dictionary.Word;

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
