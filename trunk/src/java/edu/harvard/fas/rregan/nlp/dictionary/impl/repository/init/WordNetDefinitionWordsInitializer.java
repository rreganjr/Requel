/*
 * $Id: WordNetDefinitionWordsInitializer.java,v 1.3 2009/01/26 10:19:01 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.nlp.dictionary.impl.repository.init;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import edu.harvard.fas.rregan.AbstractSystemInitializer;
import edu.harvard.fas.rregan.ResourceBundleHelper;
import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;
import edu.harvard.fas.rregan.nlp.dictionary.command.DictionaryCommandFactory;
import edu.harvard.fas.rregan.nlp.dictionary.command.LoadWordNetTaggedGlossesCommand;

/**
 * Create SynsetDefinitionWords from the wsd field in the synsets.
 * 
 * @author ron
 */
@Component("wordNetDefinitionWordsInitializer")
@Scope("prototype")
public class WordNetDefinitionWordsInitializer extends AbstractSystemInitializer {

	/**
	 * The name of the property in the
	 * WordNetDefinitionWordsInitializer.properties file that contains the path
	 * to the directory containing all the WordNet merged glosstag files, if not
	 * supplied the default file is "resources/nlp/jwnl/wn30/"
	 */
	public static final String PROP_WORDNET_MERGED_GLOSS_FILES_DIRECTORY = "WordNetMergedGlossFilesDirectory";
	public static final String PROP_WORDNET_MERGED_GLOSS_FILES_DIRECTORY_DEFAULT = "resources/nlp/jwnl/wn30/";

	/**
	 * The name of the property in the
	 * WordNetDefinitionWordsInitializer.properties file that contains a comma
	 * delimited list of merged glosstag xml file names. The files are expected
	 * to be in the directory defined by
	 * PROP_WORDNET_MERGED_GLOSS_FILES_DIRECTORY. If not supplied the default
	 * list is "categorydef.sql.gz, word.sql.gz, synset.sql.gz, sense.sql.gz"
	 * NOTE: the files may be plain sql files or zipped sql files.
	 */
	public static final String PROP_WORDNET_MERGED_GLOSS_FILES = "WordNetMergedGlossFiles";
	public static final String PROP_DWORDNET_MERGED_GLOSS_FILES_DEFAULT = "adj.xml.gz, adv.xml.gz, noun.xml.gz, verb.xml.gz";

	private final DictionaryRepository dictionaryRepository;
	private final CommandHandler commandHandler;
	private final DictionaryCommandFactory dictionaryCommandFactory;

	/**
	 * @param dictionaryRepository
	 * @param commandHandler
	 * @param dictionaryCommandFactory
	 */
	@Autowired
	public WordNetDefinitionWordsInitializer(DictionaryRepository dictionaryRepository,
			CommandHandler commandHandler, DictionaryCommandFactory dictionaryCommandFactory) {
		super(16);
		this.dictionaryRepository = dictionaryRepository;
		this.commandHandler = commandHandler;
		this.dictionaryCommandFactory = dictionaryCommandFactory;
	}

	@Override
	public void initialize() {
		try {
			// TODO: this disables the initalizer and shouldn't be hard coded
			if (true) {
				return;
			}
			log.info("initializing WordNet tagged definitions...");
			if (dictionaryRepository.buildSynsetDefinitionWords()) {
				ResourceBundleHelper resourceBundleHelper = new ResourceBundleHelper(
						WordNetDefinitionWordsInitializer.class.getName());
				String directory = resourceBundleHelper.getString(
						PROP_WORDNET_MERGED_GLOSS_FILES_DIRECTORY,
						PROP_WORDNET_MERGED_GLOSS_FILES_DIRECTORY_DEFAULT);
				String files = resourceBundleHelper.getString(PROP_WORDNET_MERGED_GLOSS_FILES,
						PROP_DWORDNET_MERGED_GLOSS_FILES_DEFAULT);
				for (String file : files.split(",")) {
					file = file.trim();
					LoadWordNetTaggedGlossesCommand command = dictionaryCommandFactory
							.newWordNetTaggedGlossaryDigester();
					command.setInputStream(getDataFileInputStream(directory + file));
					// execute the LoadWordNetTaggedGlossesCommand directly
					// (without the command handler)
					// so that a transaction isn't started. Each inner command
					// (batch for one synset) will be
					// executed in its own transaction.
					command.execute();
					// commandHandler.execute(command);
				}
			}
		} catch (Exception e) {
			log.error("failed to load WordNet tagged gloss files: " + e, e);
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
}
