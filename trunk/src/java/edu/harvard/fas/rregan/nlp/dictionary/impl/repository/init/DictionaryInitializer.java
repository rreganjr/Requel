/*
 * $Id: DictionaryInitializer.java,v 1.3 2009/01/26 10:19:01 rregan Exp $
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
import edu.harvard.fas.rregan.nlp.dictionary.command.ImportDictionaryCommand;

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
	 * the default file is "resources/nlp/dictionary/dictionary.xml.gz"
	 */
	public static final String PROP_DICTIONARY_XML_FILE = "DictionaryXMLFile";
	public static final String PROP_DICTIONARY_XML_FILE_DEFAULT = "resources/nlp/dictionary/dictionary.xml.gz";

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
