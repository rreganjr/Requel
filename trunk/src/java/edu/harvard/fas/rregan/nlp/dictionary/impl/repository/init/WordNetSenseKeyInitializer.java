/*
 * $Id: WordNetSenseKeyInitializer.java,v 1.3 2009/01/26 10:19:01 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.nlp.dictionary.impl.repository.init;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import net.didion.jwnl.data.POS;

import org.springframework.beans.factory.annotation.Autowired;

import edu.harvard.fas.rregan.AbstractSystemInitializer;
import edu.harvard.fas.rregan.ResourceBundleHelper;
import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.nlp.PartOfSpeech;
import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;
import edu.harvard.fas.rregan.nlp.dictionary.Sense;
import edu.harvard.fas.rregan.nlp.dictionary.command.DictionaryCommandFactory;
import edu.harvard.fas.rregan.nlp.dictionary.command.EditSenseCommand;
import edu.harvard.fas.rregan.requel.NoSuchEntityException;

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
	 * default file is "resources/nlp/jwnl/wn30/index.sense"
	 */
	public static final String PROP_WORDNET_SENSE_INDEX_FILE = "WordNetSenseIndexFile";
	public static final String PROP_WORDNET_SENSE_INDEX_FILE_DEFAULT = "resources/nlp/jwnl/wn30/index.sense";

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
