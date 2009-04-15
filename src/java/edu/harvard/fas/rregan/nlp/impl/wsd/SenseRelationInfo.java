/*
 * $Id: SenseRelationInfo.java,v 1.1 2008/12/14 11:36:11 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.impl.wsd;

import edu.harvard.fas.rregan.nlp.dictionary.Sense;

/**
 * @author ron
 */
public interface SenseRelationInfo {
	public Sense getSense1();

	public Sense getSense2();

	public double getRank();

	public String getReason();
}