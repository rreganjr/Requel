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
package edu.harvard.fas.rregan.nlp;

/**
 * TODO: role this into GrammaticalRelationType
 * 
 * @author ron
 */
public enum GrammaticalStructureLevel {
	/**
	 * Default if the level of the text is unknown.
	 */
	UNKNOWN(),

	/**
	 * 
	 */
	PARAGRAPH(),

	/**
	 * A collection of words without any particular grammatical relation between
	 * them. This may be used by NLPProcessors as a utility for doing its work.
	 */
	BAGOFWORDS(),

	/**
	 * 
	 */
	SENTENCE(),

	/**
	 * A clause is a collection of grammatically-related words including a
	 * predicate and a subject
	 */
	CLAUSE(),

	/**
	 * A phrase is a group of two or more grammatically linked words without a
	 * subject and predicate
	 */
	PHRASE(),

	/**
	 * 
	 */
	WORD();
}
