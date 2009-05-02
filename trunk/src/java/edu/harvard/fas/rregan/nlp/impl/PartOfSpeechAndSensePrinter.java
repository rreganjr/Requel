/*
 * $Id: PartOfSpeechAndSensePrinter.java,v 1.5 2009/01/26 10:19:00 rregan Exp $
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
package edu.harvard.fas.rregan.nlp.impl;

import edu.harvard.fas.rregan.nlp.GrammaticalStructureLevel;
import edu.harvard.fas.rregan.nlp.NLPText;
import edu.harvard.fas.rregan.nlp.PartOfSpeech;

/**
 * Print out an NLPText annotating each word with its part of speech and sense.<br>
 * For example:<br>
 * John walked the dog. John#n walk#v#3 the dog#n#1 .
 * 
 * @author ron
 */
public class PartOfSpeechAndSensePrinter implements NLPTextWalkerFunction<StringBuilder> {

	private final int bufferSize;
	private StringBuilder sb;

	/**
	 * 
	 */
	public PartOfSpeechAndSensePrinter() {
		this(1000);
	}

	public PartOfSpeechAndSensePrinter(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	@Override
	public void init() {
		sb = new StringBuilder(bufferSize);
	}

	@Override
	public void begin(NLPText text) {
		if (GrammaticalStructureLevel.WORD.equals(text.getGrammaticalStructureLevel())) {
			sb.append(text.getLemma());
			if ((text.getPartOfSpeech() != null)
					&& !text.getPartOfSpeech().in(PartOfSpeech.UNKNOWN, PartOfSpeech.PUNCTUATION,
							PartOfSpeech.DETERMINER, PartOfSpeech.PREPOSITION,
							PartOfSpeech.CONJUNCTION)) {
				sb.append("#");
				sb.append(text.getPartOfSpeech().name().substring(0, 1).toLowerCase());
				if (text.getDictionaryWordSense() != null) {
					sb.append("#");
					sb.append(text.getDictionaryWordSense().getRank());
				}
			}
		}
	}

	@Override
	public StringBuilder end(NLPText text) {
		if (GrammaticalStructureLevel.WORD.equals(text.getGrammaticalStructureLevel())) {
			sb.append(" ");
		}
		return sb;
	}
}
