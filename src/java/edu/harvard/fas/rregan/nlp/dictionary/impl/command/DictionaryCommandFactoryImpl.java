/*
 * $Id: DictionaryCommandFactoryImpl.java,v 1.3 2009/02/09 10:12:31 rregan Exp $
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirments
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
package edu.harvard.fas.rregan.nlp.dictionary.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.AbstractCommandFactory;
import edu.harvard.fas.rregan.command.CommandFactoryStrategy;
import edu.harvard.fas.rregan.nlp.dictionary.command.CalculateWordFrequenceCommand;
import edu.harvard.fas.rregan.nlp.dictionary.command.DictionaryCommandFactory;
import edu.harvard.fas.rregan.nlp.dictionary.command.EditDictionaryWordCommand;
import edu.harvard.fas.rregan.nlp.dictionary.command.EditSemlinkRefCommand;
import edu.harvard.fas.rregan.nlp.dictionary.command.EditSenseCommand;
import edu.harvard.fas.rregan.nlp.dictionary.command.EditSynsetCommand;
import edu.harvard.fas.rregan.nlp.dictionary.command.EditSynsetDefinitionWordCommand;
import edu.harvard.fas.rregan.nlp.dictionary.command.EditVerbNetSelectionRestrictionCommand;
import edu.harvard.fas.rregan.nlp.dictionary.command.ExportDictionaryCommand;
import edu.harvard.fas.rregan.nlp.dictionary.command.ImportDictionaryCommand;
import edu.harvard.fas.rregan.nlp.dictionary.command.ImportSemcorCommand;
import edu.harvard.fas.rregan.nlp.dictionary.command.ImportSemcorFileCommand;
import edu.harvard.fas.rregan.nlp.dictionary.command.SynsetHypernymWalkCommand;

/**
 * An implementation of DictionaryCommandFactory
 * 
 * @author ron
 */
@Controller("dictionaryCommandFactory")
@Scope("singleton")
public class DictionaryCommandFactoryImpl extends AbstractCommandFactory implements
		DictionaryCommandFactory {

	/**
	 * @param creationStrategy -
	 *            the strategy to use for creating new Command instances
	 */
	@Autowired
	public DictionaryCommandFactoryImpl(CommandFactoryStrategy creationStrategy) {
		super(creationStrategy);
	}

	@Override
	public ExportDictionaryCommand newExportDictionaryCommand() {
		return (ExportDictionaryCommand) getCreationStrategy().newInstance(
				ExportDictionaryCommandImpl.class);
	}

	@Override
	public ImportDictionaryCommand newImportDictionaryCommand() {
		return (ImportDictionaryCommand) getCreationStrategy().newInstance(
				ImportDictionaryCommandImpl.class);
	}

	@Override
	public EditDictionaryWordCommand newEditDictionaryWordCommand() {
		return (EditDictionaryWordCommand) getCreationStrategy().newInstance(
				EditDictionaryWordCommandImpl.class);
	}

	@Override
	public EditSynsetCommand newEditSynsetCommand() {
		return (EditSynsetCommand) getCreationStrategy().newInstance(EditSynsetCommandImpl.class);
	}

	@Override
	public EditSenseCommand newEditSenseCommand() {
		return (EditSenseCommand) getCreationStrategy().newInstance(EditSenseCommandImpl.class);
	}

	@Override
	public ImportSemcorCommand newImportSemcorCommand() {
		return (ImportSemcorCommand) getCreationStrategy().newInstance(
				ImportSemcorCommandImpl.class);
	}

	@Override
	public ImportSemcorFileCommand newImportSemcorFileCommand() {
		return (ImportSemcorFileCommand) getCreationStrategy().newInstance(
				ImportSemcorFileCommandImpl.class);
	}

	@Override
	public CalculateWordFrequenceCommand newCalculateWordFrequenceCommand() {
		return (CalculateWordFrequenceCommand) getCreationStrategy().newInstance(
				CalculateWordFrequenceCommandImpl.class);
	}

	@Override
	public SynsetHypernymWalkCommand newSynsetHypernymWalkCommand() {
		return (SynsetHypernymWalkCommand) getCreationStrategy().newInstance(
				SynsetHypernymWalkCommandImpl.class);
	}

	@Override
	public EditSemlinkRefCommand newEditSemlinkRefCommand() {
		return (EditSemlinkRefCommand) getCreationStrategy().newInstance(
				EditSemlinkRefCommandImpl.class);
	}

	@Override
	public EditSynsetDefinitionWordCommand newEditSynsetDefinitionWordCommand() {
		return (EditSynsetDefinitionWordCommand) getCreationStrategy().newInstance(
				EditSynsetDefinitionWordCommandImpl.class);
	}

	@Override
	public LoadWordNetTaggedGlossesCommandImpl newWordNetTaggedGlossaryDigester() {
		return (LoadWordNetTaggedGlossesCommandImpl) getCreationStrategy().newInstance(
				LoadWordNetTaggedGlossesCommandImpl.class);
	}

	@Override
	public EditVerbNetSelectionRestrictionCommand newEditVerbNetSelectionRestrictionCommand() {
		return (EditVerbNetSelectionRestrictionCommand) getCreationStrategy().newInstance(
				EditVerbNetSelectionRestrictionCommandImpl.class);
	}
}
