/*
 * $Id$
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
