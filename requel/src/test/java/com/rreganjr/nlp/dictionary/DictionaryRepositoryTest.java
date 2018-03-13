/*
 * $Id: DictionaryRepositoryTest.java,v 1.1 2008/12/13 00:41:47 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package com.rreganjr.nlp.dictionary;

import java.io.File;
import java.io.FileOutputStream;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.nlp.dictionary.Dictionary;
import com.rreganjr.nlp.dictionary.Sense;
import com.rreganjr.nlp.dictionary.Word;
import com.rreganjr.nlp.dictionary.command.ExportDictionaryCommand;
import com.rreganjr.nlp.dictionary.impl.command.ExportDictionaryCommandImpl;
import com.rreganjr.nlp.dictionary.impl.command.ImportDictionaryCommandImpl;
import org.junit.Assert;

/**
 * @author ron
 */
public class DictionaryRepositoryTest extends AbstractIntegrationTestCase {

	/**
	 * Test method for
	 * {@link com.rreganjr.nlp.dictionary.DictionaryRepository#findWord(java.lang.String)}.
	 */
	public void testGetWord() {
		Word word = getDictionaryRepository().findWord("search");
		Assert.assertEquals("search", word.getLemma());
		for (Sense sense : word.getSenses()) {
			Assert.assertEquals("search", sense.getWord().getLemma());
		}
	}

	public void testGetDictionary() {
		Dictionary dictionary = getDictionaryRepository().getDictionary("a", "b");
		Assert.assertTrue(dictionary.getWords().first().getLemma().compareTo("a") >= 0);
		Assert.assertTrue(dictionary.getWords().last().getLemma().compareTo("b") < 0);
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

		String wordNetDictionaryPath = "nlp/wordnet/dictionary.xml";
		importDictionary.setInputStream(getClass().getClassLoader().getResourceAsStream(
				wordNetDictionaryPath));
		getCommandHandler().execute(importDictionary);
	}
	
	public void testGetLowestCommonHypernyms() throws Exception {
		
	}
}
