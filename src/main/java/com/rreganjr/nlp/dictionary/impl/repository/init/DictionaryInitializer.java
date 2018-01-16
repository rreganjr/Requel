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

import com.rreganjr.AbstractSystemInitializer;
import net.sf.echopm.ResourceBundleHelper;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.nlp.dictionary.DictionaryRepository;
import com.rreganjr.nlp.dictionary.command.ImportDictionaryCommand;

/**
 * Load the dictionary from sql if no words exist.
 * 
 * @author ron
 */
@Component("dictionaryInitializer")
@Scope("prototype")
public class DictionaryInitializer extends AbstractSystemInitializer {

	/**
	 * The name of the property in the DictionaryInitializer.properties file
	 * that contains the path to the dictionary xml data file, if not supplied
	 * the default file is "nlp/dictionary/dictionary.xml.gz"
	 */
	public static final String PROP_DICTIONARY_XML_FILE = "DictionaryXMLFile";
	public static final String PROP_DICTIONARY_XML_FILE_DEFAULT = "nlp/dictionary/dictionary.xml.gz";

	private final DictionaryRepository dictionaryRepository;
	private final ImportDictionaryCommand command;
	private final CommandHandler commandHandler;

	/**
	 * @param dictionaryRepository
	 * @param commandHandler
	 * @param command
	 */
	@Autowired
	public DictionaryInitializer(DictionaryRepository dictionaryRepository,
			CommandHandler commandHandler, ImportDictionaryCommand command) {
		super(10);
		this.dictionaryRepository = dictionaryRepository;
		this.commandHandler = commandHandler;
		this.command = command;
	}

	@Override
	public void initialize() {
		try {
			// TODO: this disables the initalizer and shouldn't be hard coded
			if (true) {
				return;
			}

			// if no categories are defined assume the dictionary is
			// empty and load it from the xml file.
			if (dictionaryRepository.findCategories().isEmpty()) {
				ResourceBundleHelper resourceBundleHelper = new ResourceBundleHelper(
						DictionaryInitializer.class.getName());
				String dictionaryPath = resourceBundleHelper.getString(PROP_DICTIONARY_XML_FILE,
						PROP_DICTIONARY_XML_FILE_DEFAULT);
				log.info("loading dictionary: "
						+ getClass().getClassLoader().getResource(dictionaryPath).toExternalForm());
				command.setInputStream(getDataFileInputStream(dictionaryPath));
				commandHandler.execute(command);
			}
		} catch (Exception e) {
			log.error("failed to initialize dictionary from xml: " + e, e);
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
