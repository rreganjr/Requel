/*
 * $Id: ActorAssistant.java,v 1.4 2009/01/23 09:54:24 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project.impl.assistant;

import edu.harvard.fas.rregan.requel.user.User;

/**
 * Analyses stories and adds annotations with suggestions.
 * 
 * @author ron
 */
public class ActorAssistant extends TextEntityAssistant {

	/**
	 * @param lexicalAssistant -
	 *            assistant for analyzing text for spelling, terms and other
	 *            word oriented analysis.
	 * @param assistantUser -
	 *            the user to use as the creator of the annotation entities.
	 */
	public ActorAssistant(LexicalAssistant lexicalAssistant, User assistantUser) {
		super(ActorAssistant.class.getName(), lexicalAssistant, assistantUser);
	}
}
