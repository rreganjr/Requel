/*
 * $Id: ImportSemcorFileCommandImpl.java,v 1.1 2008/12/13 00:39:54 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
