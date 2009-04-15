/*
 * $Id: DictionaryRepositoryTest.java,v 1.1 2008/12/13 00:41:47 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.dictionary;

import java.io.File;
import java.io.FileOutputStream;

import edu.harvard.fas.rregan.AbstractIntegrationTestCase;
import edu.harvard.fas.rregan.nlp.dictionary.Dictionary;
import edu.harvard.fas.rregan.nlp.dictionary.Sense;
import edu.harvard.fas.rregan.nlp.dictionary.Word;
import edu.harvard.fas.rregan.nlp.dictionary.command.ExportDictionaryCommand;
import edu.harvard.fas.rregan.nlp.dictionary.impl.command.ExportDictionaryCommandImpl;
import edu.harvard.fas.rregan.nlp.dictionary.impl.command.ImportDictionaryCommandImpl;

/**
 * @author ron
 */
public class DictionaryRepositoryTest extends AbstractIntegrationTestCase {

	/**
	 * Test method for
	 * {@link edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository#findWord(java.lang.String)}.
	 */
	public void testGetWord() {
		Word word = getDictionaryRepository().findWord("search");
		assertEquals("search", word.getLemma());
		for (Sense sense : word.getSenses()) {
			assertEquals("search", sense.getWord().getLemma());
		}
	}

	public void testGetDictionary() {
		Dictionary dictionary = getDictionaryRepository().getDictionary("a", "b");
		assertTrue(dictionary.getWords().first().getLemma().compareTo("a") >= 0);
		assertTrue(dictionary.getWords().last().getLemma().compareTo("b") < 0);
	}

	public void testRangedExportDictionaryCommand() throws Exception {
		ExportDictionaryCommand exportDictionary = new ExportDictionaryCommandImpl(
				getDictionaryRepository());
		exportDictionary.setStartingFrom("a");
		exportDictionary.setEndingAt("b");
		File file = File.createTempFile("dictionary_a", ".xml");
		FileOutputStream outputStream = new FileOutputStream(file);
		exportDictionary.setOutputStream(outputStream);
		getCommandHandler().execute(exportDictionary);
		outputStream.flush();
		outputStream.close();
		System.out.println("export file: " + file.getAbsolutePath());
	}

	public void testFullExportDictionaryCommand() throws Exception {
		ExportDictionaryCommand exportDictionary = new ExportDictionaryCommandImpl(
				getDictionaryRepository());
		File file = File.createTempFile("dictionary", ".xml");
		FileOutputStream outputStream = new FileOutputStream(file);
		exportDictionary.setOutputStream(outputStream);
		getCommandHandler().execute(exportDictionary);
		outputStream.flush();
		outputStream.close();
		System.out.println("export file: " + file.getAbsolutePath());
	}

	public void testImportDictionaryCommand() throws Exception {
		ImportDictionaryCommandImpl importDictionary = new ImportDictionaryCommandImpl(
				getDictionaryRepository());

		String wordNetDictionaryPath = "resources/nlp/wordnet/dictionary.xml";
		importDictionary.setInputStream(getClass().getClassLoader().getResourceAsStream(
				wordNetDictionaryPath));
		getCommandHandler().execute(importDictionary);
	}
	
	public void testGetLowestCommonHypernyms() throws Exception {
		
	}
}
