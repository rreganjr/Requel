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
package com.rreganjr.nlp.dictionary.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.nlp.dictionary.DictionaryRepository;
import com.rreganjr.nlp.dictionary.Sense;
import com.rreganjr.nlp.dictionary.Synset;
import com.rreganjr.nlp.dictionary.Word;
import com.rreganjr.nlp.dictionary.command.EditSenseCommand;

/**
 * @author ron
 */
@Controller("editSenseCommand")
@Scope("prototype")
public class EditSenseCommandImpl extends AbstractDictionaryCommand implements EditSenseCommand {

	private Sense sense;
	private Word word;
	private Synset synset;
	private Integer rank;
	private Integer sampleFrequency;
	private String senseKey;

	/**
	 * @param dictionaryRepository
	 */
	@Autowired
	public EditSenseCommandImpl(DictionaryRepository dictionaryRepository) {
		super(dictionaryRepository);
	}

	/**
	 * @see com.rreganjr.nlp.dictionary.command.EditSenseCommand#getSense()
	 */
	@Override
	public Sense getSense() {
		return sense;
	}

	/**
	 * @see com.rreganjr.nlp.dictionary.command.EditSenseCommand#setSense(com.rreganjr.nlp.dictionary.Sense)
	 */
	@Override
	public void setSense(Sense sense) {
		this.sense = sense;
	}

	/**
	 * @see com.rreganjr.nlp.dictionary.command.EditSenseCommand#setRank(java.lang.Integer)
	 */
	@Override
	public void setRank(Integer rank) {
		this.rank = rank;
	}

	/**
	 * @see com.rreganjr.nlp.dictionary.command.EditSenseCommand#setSampleFrequency(java.lang.Integer)
	 */
	@Override
	public void setSampleFrequency(Integer sampleFrequency) {
		this.sampleFrequency = sampleFrequency;
	}

	/**
	 * @see com.rreganjr.nlp.dictionary.command.EditSenseCommand#setSenseKey(java.lang.String)
	 */
	@Override
	public void setSenseKey(String senseKey) {
		this.senseKey = senseKey;
	}

	/**
	 * @see com.rreganjr.nlp.dictionary.command.EditSenseCommand#setSynset(com.rreganjr.nlp.dictionary.Synset)
	 */
	@Override
	public void setSynset(Synset synset) {
		this.synset = synset;
	}

	/**
	 * @see com.rreganjr.nlp.dictionary.command.EditSenseCommand#setWord(com.rreganjr.nlp.dictionary.Word)
	 */
	@Override
	public void setWord(Word word) {
		this.word = word;
	}

	protected Word getWord() {
		return word;
	}

	protected Synset getSynset() {
		return synset;
	}

	protected Integer getRank() {
		return rank;
	}

	protected Integer getSampleFrequency() {
		return sampleFrequency;
	}

	protected String getSenseKey() {
		return senseKey;
	}

	/**
	 * @see com.rreganjr.command.Command#execute()
	 */
	@Override
	public void execute() throws Exception {
		Sense sense = getDictionaryRepository().get(getSense());
		Word word = getDictionaryRepository().get(getWord());
		Synset synset = getDictionaryRepository().get(getSynset());
		if (sense == null) {
			sense = getDictionaryRepository().persist(new Sense(word, synset));
			word.getSenses().add(sense);
		}
		if (getSampleFrequency() != null) {
			sense.setSampleFrequency(getSampleFrequency());
		}
		if (getRank() != null) {
			sense.setRank(getRank());
		}
		if (getSenseKey() != null) {
			sense.setSenseKey(getSenseKey());
		}
		setSense(sense);
	}

}
