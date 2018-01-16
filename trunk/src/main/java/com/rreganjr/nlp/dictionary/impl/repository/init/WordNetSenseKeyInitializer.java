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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import net.didion.jwnl.data.POS;

import org.springframework.beans.factory.annotation.Autowired;

import com.rreganjr.AbstractSystemInitializer;
import net.sf.echopm.ResourceBundleHelper;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.nlp.PartOfSpeech;
import com.rreganjr.nlp.dictionary.DictionaryRepository;
import com.rreganjr.nlp.dictionary.Sense;
import com.rreganjr.nlp.dictionary.command.DictionaryCommandFactory;
import com.rreganjr.nlp.dictionary.command.EditSenseCommand;
import com.rreganjr.requel.NoSuchEntityException;

/**
 * Create SynsetDefinitionWords from the wsd field in the synsets.
 * 
 * @author ron
 */
// @Component("wordNetSenseKeyInitializer")
// @Scope("prototype")
public class WordNetSenseKeyInitializer extends AbstractSystemInitializer {

	/**
	 * The name of the property in the WordNetSenseKeyInitializer.properties
	 * file that contains the path of the sense.index file, if not supplied the
	 * default file is "nlp/jwnl/wn30/index.sense"
	 */
	public static final String PROP_WORDNET_SENSE_INDEX_FILE = "WordNetSenseIndexFile";
	public static final String PROP_WORDNET_SENSE_INDEX_FILE_DEFAULT = "nlp/jwnl/wn30/index.sense";

	private final DictionaryRepository dictionaryRepository;
	private final CommandHandler commandHandler;
	private final DictionaryCommandFactory dictionaryCommandFactory;
	private net.didion.jwnl.dictionary.Dictionary jwnlDictionary;

	/**
	 * @param dictionaryRepository
	 * @param commandHandler
	 * @param dictionaryCommandFactory
	 */
	@Autowired
	public WordNetSenseKeyInitializer(DictionaryRepository dictionaryRepository,
			CommandHandler commandHandler, DictionaryCommandFactory dictionaryCommandFactory) {
		super(15);
		this.dictionaryRepository = dictionaryRepository;
		this.commandHandler = commandHandler;
		this.dictionaryCommandFactory = dictionaryCommandFactory;
	}

	@Override
	public void initialize() {
		// TODO: this disables the initalizer and shouldn't be hard coded
		if (true) {
			return;
		}
		log.info("initializing WordNet sense sense keys...");
		try {
			if (dictionaryRepository.buildSenseKeys()) {
				ResourceBundleHelper resourceBundleHelper = new ResourceBundleHelper(
						WordNetSenseKeyInitializer.class.getName());
				String senseIndexPath = resourceBundleHelper.getString(
						PROP_WORDNET_SENSE_INDEX_FILE, PROP_WORDNET_SENSE_INDEX_FILE_DEFAULT);

				BufferedReader reader = new BufferedReader(new InputStreamReader(
						getDataFileInputStream(senseIndexPath)));
				String indexLine;
				while ((indexLine = reader.readLine()) != null) {
					String[] indexEnties = indexLine.split(" ");
					String senseKey = indexEnties[0];
					String lemma = senseKey.substring(0, senseKey.indexOf('%'));
					String synsetTypeCode = senseKey.substring(lemma.length() + 1, senseKey
							.indexOf(':'));
					String offset = indexEnties[1];
					// TODO: map the type code to the base value like 5 - > 3
					if (synsetTypeCode.equals("5")) {
						synsetTypeCode = "3";
					}
					Long synsetId = new Long(synsetTypeCode + offset);
					try {
						Sense sense = dictionaryRepository.findSensesByLemmaAndSynsetId(lemma
								.replace('_', ' '), synsetId);
						EditSenseCommand command = dictionaryCommandFactory.newEditSenseCommand();
						command.setSense(sense);
						command.setSenseKey(senseKey);
						command = commandHandler.execute(command);
						log.info(sense + " -> " + senseKey);
					} catch (NoSuchEntityException e) {
						log.warn("no sense for " + lemma + " " + synsetId);
					}
				}
			}
		} catch (Exception e) {
			log.error("failed to initialize semcor: " + e, e);
		}
	}

	private InputStream getDataFileInputStream(String dataFilePath) throws IOException {
		log.debug("loading data file " + dataFilePath);
		InputStream dataInputStream = getClass().getClassLoader().getResourceAsStream(dataFilePath);
		if (dataFilePath.endsWith(".gz")) {
			dataInputStream = new GZIPInputStream(dataInputStream);
		}
		return dataInputStream;
	}

	private POS getWordNetPOSFromPartOfSpeech(PartOfSpeech partOfSpeech) {
		if (partOfSpeech.in(PartOfSpeech.NOUN)) {
			return POS.NOUN;
		} else if (partOfSpeech.in(PartOfSpeech.VERB)) {
			return POS.VERB;
		} else if (partOfSpeech.in(PartOfSpeech.ADVERB)) {
			return POS.ADVERB;
		} else if (partOfSpeech.in(PartOfSpeech.ADJECTIVE)) {
			return POS.ADJECTIVE;
		}
		return null;
	}
}
