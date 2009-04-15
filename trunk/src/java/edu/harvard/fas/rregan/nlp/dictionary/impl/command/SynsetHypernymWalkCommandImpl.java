/*
 * $Id: SynsetHypernymWalkCommandImpl.java,v 1.1 2008/12/13 00:39:56 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.dictionary.impl.command;

import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;
import edu.harvard.fas.rregan.nlp.dictionary.Synset;
import edu.harvard.fas.rregan.nlp.dictionary.command.SynsetHypernymWalkCommand;
import edu.harvard.fas.rregan.nlp.dictionary.impl.repository.init.WordNetHyponymCountInitializer.Counter;

/**
 * @author ron
 */
@Controller("synsetHypernymWalkCommand")
@Scope("prototype")
public class SynsetHypernymWalkCommandImpl extends AbstractDictionaryCommand implements
		SynsetHypernymWalkCommand {

	private Synset synset;
	private Map<Synset, Set<Synset>> hyponymAncestors;
	private Map<Synset, Counter> hyponymCounts;

	/**
	 * @param dictionaryRepository
	 */
	@Autowired
	public SynsetHypernymWalkCommandImpl(DictionaryRepository dictionaryRepository) {
		super(dictionaryRepository);
	}

	/**
	 * @see edu.harvard.fas.rregan.nlp.dictionary.command.SynsetHypernymWalkCommand#setHyponymAncestors(java.util.Map)
	 */
	@Override
	public void setHyponymAncestors(Map<Synset, Set<Synset>> hyponymAncestors) {
		this.hyponymAncestors = hyponymAncestors;
	}

	/**
	 * @see edu.harvard.fas.rregan.nlp.dictionary.command.SynsetHypernymWalkCommand#setHyponymCounts(java.util.Map)
	 */
	@Override
	public void setHyponymCounts(Map<Synset, Counter> hyponymCounts) {
		this.hyponymCounts = hyponymCounts;
	}

	/**
	 * @see edu.harvard.fas.rregan.nlp.dictionary.command.SynsetHypernymWalkCommand#setSynset(edu.harvard.fas.rregan.nlp.dictionary.Synset)
	 */
	@Override
	public void setSynset(Synset synset) {
		this.synset = synset;
	}

	/**
	 * @see edu.harvard.fas.rregan.command.Command#execute()
	 */
	@Override
	public void execute() throws Exception {
		Synset synset = getDictionaryRepository().get(this.synset);
		if (log.isDebugEnabled()) {
			log.debug(synset);
		}
		hypernymWalk(synset);
	}

	/**
	 * @param synset
	 */
	private void hypernymWalk(Synset synset) {
		for (Synset hypernym : synset.getHypernyms()) {
			if (!hyponymAncestors.get(synset).contains(hypernym)) {
				hyponymAncestors.get(synset).add(hypernym);
				hyponymCounts.get(hypernym).count++;
			}
			hypernymWalk(hypernym);
		}
	}
}
