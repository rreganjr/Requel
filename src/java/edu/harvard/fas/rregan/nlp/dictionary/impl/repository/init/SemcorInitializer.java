/*
 * $Id: SemcorInitializer.java,v 1.3 2009/01/26 10:19:01 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.nlp.dictionary.impl.repository.init;

import org.springframework.beans.factory.annotation.Autowired;

import edu.harvard.fas.rregan.AbstractSystemInitializer;
import edu.harvard.fas.rregan.ResourceBundleHelper;
import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;
import edu.harvard.fas.rregan.nlp.dictionary.command.DictionaryCommandFactory;
import edu.harvard.fas.rregan.nlp.dictionary.command.ImportSemcorFileCommand;
import edu.harvard.fas.rregan.requel.NoSuchEntityException;
import edu.mit.jsemcor.element.IContext;
import edu.mit.jsemcor.element.IContextID;
import edu.mit.jsemcor.main.IConcordance;
import edu.mit.jsemcor.main.IConcordanceSet;
import edu.mit.jsemcor.main.Semcor;

/**
 * Load the dictionary from sql if no words exist.
 * 
 * @author ron
 */
// @Component("semcorInitializer")
// @Scope("prototype")
public class SemcorInitializer extends AbstractSystemInitializer {

	/**
	 * The name of the property in the DictionaryInitializer.properties file
	 * that contains the path to the dictionary xml data file, if not supplied
	 * the default file is "resources/nlp/dictionary/dictionary.xml.gz"
	 */
	public static final String PROP_SEMCOR_BASE_DICTIONARY = "SemcorBaseDirectory";
	public static final String PROP_SEMCOR_BASE_DICTIONARY_DEFAULT = "resources/nlp/semcor/";

	private final DictionaryRepository dictionaryRepository;
	private final CommandHandler commandHandler;
	private final DictionaryCommandFactory dictionaryCommandFactory;

	/**
	 * @param dictionaryRepository
	 * @param commandHandler
	 * @param dictionaryCommandFactory
	 */
	@Autowired
	public SemcorInitializer(DictionaryRepository dictionaryRepository,
			CommandHandler commandHandler, DictionaryCommandFactory dictionaryCommandFactory) {
		super(10);
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
			// scan through the semcor files and load them if missing. assume if
			// any sentence
			// exists from a file then all of them do.
			ResourceBundleHelper resourceBundleHelper = new ResourceBundleHelper(
					SemcorInitializer.class.getName());
			String semcorBaseDirectory = resourceBundleHelper.getString(
					PROP_SEMCOR_BASE_DICTIONARY, PROP_SEMCOR_BASE_DICTIONARY_DEFAULT);
			log
					.info("loading semcor corpus from: "
							+ getClass().getClassLoader().getResource(semcorBaseDirectory)
									.toExternalForm());

			IConcordanceSet semcor = new Semcor(getClass().getClassLoader().getResource(
					semcorBaseDirectory));
			semcor.open();

			// TODO: add a semcore file that holds all the sentences in the file
			// and only load files that don't exist in the database.
			for (IConcordance section : semcor.values()) {
				for (IContextID id : section.getContextIDs()) {
					IContext context = section.getContext(id);
					try {
						dictionaryRepository.findSemcorFile(section.getName(), context
								.getFilename());
					} catch (NoSuchEntityException e) {
						try {
							log.debug("loading " + section.getName() + " " + context.getFilename());
							ImportSemcorFileCommand command = dictionaryCommandFactory
									.newImportSemcorFileCommand();
							command.setSection(section);
							command.setContext(context);
							commandHandler.execute(command);
						} catch (Exception ee) {
							log.error("failed to load semcor file " + section.getName() + " "
									+ context.getFilename(), e);
						}
					}
				}
			}
		} catch (Exception e) {
			log.error("failed to initialize semcor: " + e, e);
		}
	}
}
