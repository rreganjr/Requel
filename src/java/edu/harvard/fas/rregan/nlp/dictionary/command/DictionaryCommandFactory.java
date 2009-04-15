/*
 * $Id: DictionaryCommandFactory.java,v 1.3 2009/02/09 10:12:30 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.nlp.dictionary.command;

import edu.harvard.fas.rregan.command.CommandFactory;
import edu.harvard.fas.rregan.nlp.dictionary.Synset;
import edu.harvard.fas.rregan.nlp.dictionary.SynsetDefinitionWord;
import edu.harvard.fas.rregan.nlp.dictionary.VerbNetRoleRef;
import edu.harvard.fas.rregan.nlp.dictionary.VerbNetSelectionRestriction;
import edu.harvard.fas.rregan.nlp.dictionary.impl.command.LoadWordNetTaggedGlossesCommandImpl;

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
	 * @return a new ImportSemcorFileCommand for importing a single file of the
	 *         SemCor semantically annotated corpus into the database.
	 */
	public ImportSemcorFileCommand newImportSemcorFileCommand();

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