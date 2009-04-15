/*
 * $Id: DependencyPrinter.java,v 1.5 2009/01/28 04:58:59 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.nlp.impl;

import java.util.HashSet;
import java.util.Set;

import edu.harvard.fas.rregan.nlp.GrammaticalRelation;
import edu.harvard.fas.rregan.nlp.GrammaticalStructureLevel;
import edu.harvard.fas.rregan.nlp.NLPText;

/**
 * An NLPTextWalkerFunction that takes an NLPText and prints out the list of
 * dependency relations. For example:<br>
 * 
 * <pre>
 * </pre>
 * 
 * @author ron
 */
public class DependencyPrinter implements NLPTextWalkerFunction<StringBuilder> {

	private final Set<GrammaticalRelation> relations = new HashSet<GrammaticalRelation>();
	private final int bufferSize;
	private StringBuilder sb;

	/**
	 * 
	 */
	public DependencyPrinter() {
		this(1000);
	}

	/**
	 * @param bufferSize
	 */
	public DependencyPrinter(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	@Override
	public void init() {
		sb = new StringBuilder(bufferSize);
		relations.clear();
	}

	@Override
	public void begin(NLPText text) {
		if (GrammaticalStructureLevel.WORD.equals(text.getGrammaticalStructureLevel())) {
			if (text.getDependentOf().size() > 0) {
				for (GrammaticalRelation rel : text.getDependentOf()) {
					if (!relations.contains(rel)) {
						relations.add(rel);
						sb.append("[dep] ");
						sb.append(rel.getDependent().getText());
						sb.append("-");
						sb.append(rel.getDependent().getWordIndex());
						sb.append(" is the ");
						sb.append(rel.getType().getLongName());
						sb.append(" of ");
						sb.append(rel.getGovernor().getText());
						sb.append("-");
						sb.append(rel.getGovernor().getWordIndex());
						sb.append("\n");
					}
				}
			}
		}
	}

	@Override
	public StringBuilder end(NLPText t) {
		return sb;
	}
}
