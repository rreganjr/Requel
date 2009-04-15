/*
 * $Id: EditGlossaryTermCommand.java,v 1.5 2009/01/27 09:30:16 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project.command;

import java.util.Set;

import edu.harvard.fas.rregan.requel.project.GlossaryTerm;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity;

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
