/*
 * $Id: PartOfSpeechAndSensePrinter.java,v 1.5 2009/01/26 10:19:00 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
