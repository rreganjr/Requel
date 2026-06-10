/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.NoResultException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.nlp.dictionary.Dictionary;
import com.rreganjr.nlp.dictionary.DictionaryRepository;
import com.rreganjr.nlp.dictionary.UnmarshallerListener;
import com.rreganjr.nlp.dictionary.Word;
import com.rreganjr.nlp.dictionary.command.ImportDictionaryCommand;

/**
 * @author ron
 */
@Controller("importDictionaryCommand")
@Scope("prototype")
public class ImportDictionaryCommandImpl extends AbstractDictionaryCommand implements
		ImportDictionaryCommand {

	private InputStream inputStream;

	/**
	 * @param dictionaryRepository
	 */
	@Autowired
	public ImportDictionaryCommandImpl(DictionaryRepository dictionaryRepository) {
		super(dictionaryRepository);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.rreganjr.requel.dictionary.ImportDictionaryCommand#setInputStream(java.io.InputStream)
	 */
	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	protected InputStream getInputStream() {
		return inputStream;
	}

	/**
	 * @see com.rreganjr.command.Command#execute()
	 */
	@Override
	public void execute() {
		// NOTE: the annotation classes need to be explicitly supplied to
		// the newInstance or an IllegalAnnotationExceptions will occur for
		// AbstractProjectOrDomainEntity.getAnnotations()
		try {
			JAXBContext context = JAXBContext
					.newInstance(ExportDictionaryCommandImpl.CLASSES_FOR_JAXB);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			unmarshaller.setListener(new UnmarshallerListener(getDictionaryRepository()));
			Dictionary dictionary = (Dictionary) unmarshaller.unmarshal(getInputStream());
			// Idempotent import: skip lemmas already present so importing into a DB that already
			// holds an individual word (e.g. inserted by an add-word-to-dictionary resolution in a
			// test sharing the same context/DB) doesn't collide. Word uses IDENTITY id generation,
			// so create() forces an immediate INSERT and a duplicate would throw mid-loop and poison
			// the transaction — skipping avoids that entirely. The set is built from ONE query (not
			// a lookup per word — the dictionary is large), and is empty on a fresh DB so nothing is
			// skipped. It only ever holds the handful of words present before a full import, because
			// ensureDictionaryLoaded short-circuits once the dictionary is fully loaded.
			Set<String> existingLemmas = new HashSet<>();
			for (Word existing : getDictionaryRepository().findWords()) {
				existingLemmas.add(existing.getLemma());
			}
			for (Word word : dictionary.getWords()) {
				try {
					if (word.getLemma() != null && !existingLemmas.add(word.getLemma())) {
						// already present (pre-existing, or a duplicate lemma within this file)
						continue;
					}
					// Import always creates new rows (issue #76): persist() would route the
					// id-bearing, just-unmarshalled Word to merge(), which Hibernate 6.6 turns
					// into a stale 0-row update because the row is absent in a fresh DB.
					getDictionaryRepository().create(word);
				} catch (Exception e) {
					log.error("could not save word '" + word.getLemma() + "': " + e, e);
					throw e;
				}
			}
		} catch (Exception e) {
			// Propagate so callers (tests) fail fast with a useful stack trace
			throw new RuntimeException("Failed to import dictionary", e);
		}
	}
}
