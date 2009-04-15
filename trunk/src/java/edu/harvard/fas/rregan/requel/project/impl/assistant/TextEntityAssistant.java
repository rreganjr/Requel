/*
 * $Id: TextEntityAssistant.java,v 1.10 2009/01/23 09:54:24 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project.impl.assistant;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity;
import edu.harvard.fas.rregan.requel.project.TextEntity;
import edu.harvard.fas.rregan.requel.user.User;

/**
 * Assistant for analyzing entities based on the TextEntity interface, such as
 * goals and stories. The assistant applies the lexical assistant to the name
 * and text properties of each entity.
 * 
 * @author ron
 */
public class TextEntityAssistant extends ProjectOrDomainEntityAssistant {
	private static final Logger log = Logger.getLogger(TextEntityAssistant.class);

	/**
	 * @param resourceBundleName -
	 *            the full class name to use for the resource bundle.
	 * @param lexicalAssistant -
	 *            assistant for analyzing text for spelling, terms and other
	 *            word oriented analysis.
	 * @param updatedEntityNotifier -
	 *            after an entity is analyzed it is passed to the notifier to
	 *            tell the UI components that reference the entity to refresh
	 * @param assistantUser -
	 *            the user to use as the creator of the annotation entities.
	 */
	public TextEntityAssistant(String resourceBundleName, LexicalAssistant lexicalAssistant,
			User assistantUser) {
		super(resourceBundleName, lexicalAssistant, assistantUser);
	}

	@Override
	public void setEntity(ProjectOrDomainEntity entity) throws IllegalArgumentException {
		super.setEntity(entity);
		if (entity instanceof TextEntity) {
			setPropertyText(PROP_TEXT, ((TextEntity) entity).getText());
		}
	}

	/**
	 * Analyze the text of the text property
	 * 
	 * @param entity
	 */
	@Override
	public void analyze() {
		super.analyze();
	}
}
