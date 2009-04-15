/*
 * $Id: ReplaceGlossaryTermCommand.java,v 1.3 2009/03/23 11:02:58 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project.command;

import java.util.Set;

import edu.harvard.fas.rregan.requel.command.EditCommand;
import edu.harvard.fas.rregan.requel.project.GlossaryTerm;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity;

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
