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
package com.rreganjr.nlp.dictionary.command;

import com.rreganjr.command.CommandFactory;
import com.rreganjr.nlp.dictionary.Synset;
import com.rreganjr.nlp.dictionary.SynsetDefinitionWord;
import com.rreganjr.nlp.dictionary.VerbNetRoleRef;
import com.rreganjr.nlp.dictionary.VerbNetSelectionRestriction;
import com.rreganjr.nlp.dictionary.impl.command.LoadWordNetTaggedGlossesCommandImpl;

/**
 * @author ron
 */
public interface DictionaryCommandFactory extends CommandFactory {

	/**
	 * @return a new ImportDictionaryCommand for loading the dictionary from an
	 *         xml file.
	 */
	public ImportDictionaryCommand newImportDictionaryCommand();

	/**
	 * @return a new ExportDictionaryCommand for saving the dictionary to an xml
	 *         file.
	 */
	public ExportDictionaryCommand newExportDictionaryCommand();

	/**
	 * @return a new EditDictionaryWordCommand for add/editing a word in the
	 *         dictionary
	 */
	public EditDictionaryWordCommand newEditDictionaryWordCommand();

	/**
	 * @return a new EditSynsetCommand for add/editing a synset
	 *         (meaning/concept) in the dictionary
	 */
	public EditSynsetCommand newEditSynsetCommand();

	/**
	 * @return a new EditSenseCommand for add/editing a sense in the dictionary
	 */
	public EditSenseCommand newEditSenseCommand();

	/**
	 * @return a new ImportSemcorCommand for importing the SemCor semantically
	 *         annotated corpus into the database.
	 */
	public ImportSemcorCommand newImportSemcorCommand();

	/**
	 * @return a new CalculateWordFrequenceCommand for calculating the word
	 *         frequency of Semcor words tagged with a sense and setting the
	 *         frequency on the Sense.
	 */
	public CalculateWordFrequenceCommand newCalculateWordFrequenceCommand();

	/**
	 * @return a new SynsetHypernymWalkCommand for calculating the count of
	 *         hyponym subsumers.
	 */
	public SynsetHypernymWalkCommand newSynsetHypernymWalkCommand();

	/**
	 * @return a new EditSemlinkRefCommand for editing a SemlinkRef.
	 */
	public EditSemlinkRefCommand newEditSemlinkRefCommand();

	/**
	 * @return a new WordNetTaggedGlossaryDigester for processing the WordNet
	 *         synset merged tagged gloss word sense files.<br>
	 * @see {http://wordnet.princeton.edu/glosstag-files/}
	 */
	public LoadWordNetTaggedGlossesCommandImpl newWordNetTaggedGlossaryDigester();

	/**
	 * @return a new EditSynsetDefinitionWordCommand for adding a
	 *         {@link SynsetDefinitionWord} to a {@link Synset}
	 */
	public EditSynsetDefinitionWordCommand newEditSynsetDefinitionWordCommand();

	/**
	 * @return a new EditVerbNetSelectionRestriction for adding/editing a
	 *         {@link VerbNetSelectionRestriction} of a {@link VerbNetRoleRef}
	 */
	public EditVerbNetSelectionRestrictionCommand newEditVerbNetSelectionRestrictionCommand();
}