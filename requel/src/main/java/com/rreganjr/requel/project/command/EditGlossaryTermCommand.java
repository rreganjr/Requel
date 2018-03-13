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

import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;

/**
 * @author ron
 */
public interface EditGlossaryTermCommand extends EditTextEntityCommand {

	/**
	 * Set the project or domain entities that refer to the glossary term. This
	 * is the absolute set of referers.<br>
	 * Either this or setAddReferers should be used, this takes precedence.
	 * 
	 * @param referers
	 */
	public void setReferers(Set<ProjectOrDomainEntity> referers);

	/**
	 * Set the project or domain entities to add as referers of the glossary
	 * term. This is for adding new referers to the existing set.<br>
	 * Either this or setReferers should be used, setReferers takes precedence.
	 * 
	 * @param referers
	 */
	public void setAddReferers(Set<ProjectOrDomainEntity> referers);

	/**
	 * Set the glossary term to edit.
	 * 
	 * @param glossaryTerm
	 */
	public void setGlossaryTerm(GlossaryTerm glossaryTerm);

	/**
	 * Get the new or updated glossary term.
	 * 
	 * @return
	 */
	public GlossaryTerm getGlossaryTerm();

	/**
	 * Set the glossary term this term is an alternate case of.
	 * 
	 * @param canonicalTerm
	 */
	public void setCanonicalTerm(GlossaryTerm canonicalTerm);

	/**
	 * Set the project or domain this term is being added to.
	 * 
	 * @param projectOrDomain
	 */
	public void setProjectOrDomain(ProjectOrDomain projectOrDomain);
}
