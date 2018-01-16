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
package com.rreganjr.requel.project.impl.assistant;

import org.apache.log4j.Logger;

import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.TextEntity;
import com.rreganjr.requel.user.User;

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
