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
package com.rreganjr.requel.project.command;

import java.util.Set;

import com.rreganjr.requel.command.EditCommand;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.ProjectOrDomainEntity;

/**
 * Take a glossary term and replace its text (name) in the referring entities
 * with the text (name) of the canonical term.
 * 
 * @author ron
 */
public interface ReplaceGlossaryTermCommand extends EditCommand {

	/**
	 * When this is set the glossary term is updated to set the canonical term
	 * before replacing the text in the entities.
	 * 
	 * @param canonicalTerm
	 */
	public void setCanonicalTerm(GlossaryTerm canonicalTerm);

	/**
	 * Set the glossary term to replace with the canonical term.
	 * 
	 * @param glossaryTerm
	 */
	public void setGlossaryTerm(GlossaryTerm glossaryTerm);

	/**
	 * @return The glossary term after all the referring entities have been
	 *         updated.
	 */
	public GlossaryTerm getGlossaryTerm();

	/**
	 * @return The project entities that were changed.
	 */
	public Set<ProjectOrDomainEntity> getUpdatedEntities();

}
