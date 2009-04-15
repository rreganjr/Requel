/*
 * $Id: EditSynsetDefinitionWordCommandImpl.java,v 1.1 2008/12/13 00:39:57 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.nlp.dictionary.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.nlp.ParseTag;
import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;
import edu.harvard.fas.rregan.nlp.dictionary.Sense;
import edu.harvard.fas.rregan.nlp.dictionary.Synset;
import edu.harvard.fas.rregan.nlp.dictionary.SynsetDefinitionWord;
import edu.harvard.fas.rregan.nlp.dictionary.command.EditSynsetDefinitionWordCommand;

/**
 * @author ron
 */
@Controller("editSynsetDefinitionWordCommand")
@Scope("prototype")
public class EditSynsetDefinitionWordCommandImpl extends AbstractDictionaryCommand implements
		EditSynsetDefinitionWordCommand {

	private SynsetDefinitionWord synsetDefinitionWord;
	private Synset synset;
	private Sense sense;
	private Integer index;
	private String text;
	private ParseTag parseTag;

	/**
	 * @param dictionaryRepository
	 */
	@Autowired
	public EditSynsetDefinitionWordCommandImpl(DictionaryRepository dictionaryRepository) {
		super(dictionaryRepository);
	}

	protected Synset getSynset() {
		return synset;
	}

	public void setSynset(Synset synset) {
		this.synset = synset;
	}

	protected Sense getSense() {
		return sense;
	}

	public void setSense(Sense sense) {
		this.sense = sense;
	}

	protected Integer getIndex() {
		return index;
	}

	public void setIndex(Integer index) {
		this.index = index;
	}

	protected String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	protected ParseTag getParseTag() {
		return parseTag;
	}

	public void setParseTag(ParseTag parseTag) {
		this.parseTag = parseTag;
	}

	public SynsetDefinitionWord getSynsetDefinitionWord() {
		return synsetDefinitionWord;
	}

	public void setSynsetDefinitionWord(SynsetDefinitionWord synsetDefinitionWord) {
		this.synsetDefinitionWord = synsetDefinitionWord;
	}

	/**
	 * @see edu.harvard.fas.rregan.command.Command#execute()
	 */
	@Override
	public void execute() throws Exception {
		SynsetDefinitionWord synsetDefinitionWord = getDictionaryRepository().get(
				getSynsetDefinitionWord());
		Synset synset = getDictionaryRepository().get(getSynset());
		Sense sense = getDictionaryRepository().get(getSense());

		if (synsetDefinitionWord == null) {
			if (sense != null) {
				synsetDefinitionWord = new SynsetDefinitionWord(synset, getIndex(), getText(),
						getParseTag(), sense);
			} else {
				synsetDefinitionWord = new SynsetDefinitionWord(synset, getIndex(), getText(),
						getParseTag());
			}
			synsetDefinitionWord = getDictionaryRepository().persist(synsetDefinitionWord);
		} else {
			if (!synsetDefinitionWord.getSynset().equals(synset)) {
				synsetDefinitionWord.getSynset().getWords().remove(synsetDefinitionWord);
				synsetDefinitionWord.setSynset(synset);
			}
			synsetDefinitionWord.setIndex(getIndex());
			synsetDefinitionWord.setSense(sense);
			synsetDefinitionWord.setText(getText());
			synsetDefinitionWord.setParseTag(getParseTag());
		}

		if (getIndex() < synset.getWords().size()) {
			synset.getWords().add(getIndex(), synsetDefinitionWord);
		} else {
			synset.getWords().add(synsetDefinitionWord);
		}
		setSynsetDefinitionWord(synsetDefinitionWord);
	}
}
