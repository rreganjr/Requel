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

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.rreganjr.initializer.AbstractSystemInitializer;
import net.sf.echopm.ResourceBundleHelper;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.nlp.dictionary.DictionaryRepository;
import com.rreganjr.nlp.dictionary.command.DictionaryCommandFactory;
import com.rreganjr.nlp.dictionary.command.LoadWordNetTaggedGlossesCommand;

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
	 * supplied the default file is "nlp/jwnl/wn30/"
	 */
	public static final String PROP_WORDNET_MERGED_GLOSS_FILES_DIRECTORY = "WordNetMergedGlossFilesDirectory";
	public static final String PROP_WORDNET_MERGED_GLOSS_FILES_DIRECTORY_DEFAULT = "nlp/jwnl/wn30/";

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
			AbstractSystemInitializer.log.info("initializing WordNet tagged definitions...");
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
			AbstractSystemInitializer.log.error("failed to load WordNet tagged gloss files: " + e, e);
		}
	}

	private InputStream getDataFileInputStream(String dataFilePath) throws IOException {
		AbstractSystemInitializer.log.debug("loading data file " + dataFilePath);
		InputStream dataInputStream = getClass().getClassLoader().getResourceAsStream(dataFilePath);
		if (dataFilePath.endsWith(".gz")) {
			dataInputStream = new GZIPInputStream(dataInputStream);
		}
		return dataInputStream;
	}
}
