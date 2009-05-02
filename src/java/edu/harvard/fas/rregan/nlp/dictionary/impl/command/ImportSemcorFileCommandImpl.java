/*
 * $Id: ImportSemcorFileCommandImpl.java,v 1.1 2008/12/13 00:39:54 rregan Exp $
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.nlp.NLPProcessor;
import edu.harvard.fas.rregan.nlp.NLPText;
import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;
import edu.harvard.fas.rregan.nlp.dictionary.command.ImportSemcorFileCommand;
import edu.mit.jsemcor.element.IContext;
import edu.mit.jsemcor.main.IConcordance;

/**
 * @author ron
 */
@Controller("importSemcorFileCommand")
@Scope("prototype")
public class ImportSemcorFileCommandImpl extends ImportSemcorCommandImpl implements
		ImportSemcorFileCommand {

	private IConcordance section;
	private IContext context;

	/**
	 * @param dictionaryRepository
	 * @param lemmatizer
	 */
	@Autowired
	public ImportSemcorFileCommandImpl(DictionaryRepository dictionaryRepository,
			@Qualifier("lemmatizer")
			NLPProcessor<NLPText> lemmatizer) {
		super(dictionaryRepository, lemmatizer);
	}

	protected IConcordance getSection() {
		return section;
	}

	public void setSection(IConcordance section) {
		this.section = section;
	}

	protected IContext getContext() {
		return context;
	}

	public void setContext(IContext context) {
		this.context = context;
	}

	/**
	 * @see edu.harvard.fas.rregan.command.Command#execute()
	 */
	@Override
	public void execute() {
		loadSemcorFile(getSection(), getContext());
	}
}
