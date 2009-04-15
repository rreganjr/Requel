/*
 * $Id: SynsetHypernymWalkCommand.java,v 1.1 2008/12/13 00:40:11 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.dictionary.command;

import java.util.Map;
import java.util.Set;

import edu.harvard.fas.rregan.command.Command;
import edu.harvard.fas.rregan.nlp.dictionary.Synset;
import edu.harvard.fas.rregan.nlp.dictionary.impl.repository.init.WordNetHyponymCountInitializer;
import edu.harvard.fas.rregan.nlp.dictionary.impl.repository.init.WordNetHyponymCountInitializer.Counter;

/**
 * Given a synset, walk up the hypernym graph and update counters.
 * 
 * @author ron
 * @see WordNetHyponymCountInitializer
 */
public interface SynsetHypernymWalkCommand extends Command {

	/**
	 * set the synset to start from and walk up the graph.
	 * 
	 * @param synset
	 */
	public void setSynset(Synset synset);

	/**
	 * set the map of counters for all synsets.
	 * 
	 * @param hyponymCounts
	 */
	public void setHyponymCounts(Map<Synset, Counter> hyponymCounts);

	/**
	 * set the map of ancestors for all synsets.
	 * 
	 * @param hyponymAncestors
	 */
	public void setHyponymAncestors(Map<Synset, Set<Synset>> hyponymAncestors);

}
