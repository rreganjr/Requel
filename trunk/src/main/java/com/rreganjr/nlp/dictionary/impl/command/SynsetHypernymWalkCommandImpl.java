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

import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.nlp.dictionary.DictionaryRepository;
import com.rreganjr.nlp.dictionary.Synset;
import com.rreganjr.nlp.dictionary.command.SynsetHypernymWalkCommand;
import com.rreganjr.nlp.dictionary.impl.repository.init.WordNetHyponymCountInitializer.Counter;

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
	 * @see com.rreganjr.nlp.dictionary.command.SynsetHypernymWalkCommand#setHyponymAncestors(java.util.Map)
	 */
	@Override
	public void setHyponymAncestors(Map<Synset, Set<Synset>> hyponymAncestors) {
		this.hyponymAncestors = hyponymAncestors;
	}

	/**
	 * @see com.rreganjr.nlp.dictionary.command.SynsetHypernymWalkCommand#setHyponymCounts(java.util.Map)
	 */
	@Override
	public void setHyponymCounts(Map<Synset, Counter> hyponymCounts) {
		this.hyponymCounts = hyponymCounts;
	}

	/**
	 * @see com.rreganjr.nlp.dictionary.command.SynsetHypernymWalkCommand#setSynset(com.rreganjr.nlp.dictionary.Synset)
	 */
	@Override
	public void setSynset(Synset synset) {
		this.synset = synset;
	}

	/**
	 * @see com.rreganjr.command.Command#execute()
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
