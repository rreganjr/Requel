/*
 * $Id: EditSenseCommandImpl.java,v 1.1 2008/12/13 00:39:59 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.dictionary.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;
import edu.harvard.fas.rregan.nlp.dictionary.Sense;
import edu.harvard.fas.rregan.nlp.dictionary.Synset;
import edu.harvard.fas.rregan.nlp.dictionary.Word;
import edu.harvard.fas.rregan.nlp.dictionary.command.EditSenseCommand;

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
	 * @see edu.harvard.fas.rregan.nlp.dictionary.command.EditSenseCommand#getSense()
	 */
	@Override
	public Sense getSense() {
		return sense;
	}

	/**
	 * @see edu.harvard.fas.rregan.nlp.dictionary.command.EditSenseCommand#setSense(edu.harvard.fas.rregan.nlp.dictionary.Sense)
	 */
	@Override
	public void setSense(Sense sense) {
		this.sense = sense;
	}

	/**
	 * @see edu.harvard.fas.rregan.nlp.dictionary.command.EditSenseCommand#setRank(java.lang.Integer)
	 */
	@Override
	public void setRank(Integer rank) {
		this.rank = rank;
	}

	/**
	 * @see edu.harvard.fas.rregan.nlp.dictionary.command.EditSenseCommand#setSampleFrequency(java.lang.Integer)
	 */
	@Override
	public void setSampleFrequency(Integer sampleFrequency) {
		this.sampleFrequency = sampleFrequency;
	}

	/**
	 * @see edu.harvard.fas.rregan.nlp.dictionary.command.EditSenseCommand#setSenseKey(java.lang.String)
	 */
	@Override
	public void setSenseKey(String senseKey) {
		this.senseKey = senseKey;
	}

	/**
	 * @see edu.harvard.fas.rregan.nlp.dictionary.command.EditSenseCommand#setSynset(edu.harvard.fas.rregan.nlp.dictionary.Synset)
	 */
	@Override
	public void setSynset(Synset synset) {
		this.synset = synset;
	}

	/**
	 * @see edu.harvard.fas.rregan.nlp.dictionary.command.EditSenseCommand#setWord(edu.harvard.fas.rregan.nlp.dictionary.Word)
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
	 * @see edu.harvard.fas.rregan.command.Command#execute()
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
