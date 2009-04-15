/*
 * $Id: RemoveUnneedLexicalIssuesCommand.java,v 1.1 2009/01/19 09:32:23 rregan Exp $
 * Copyright (c) 2009 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project.command;

import edu.harvard.fas.rregan.nlp.NLPText;
import edu.harvard.fas.rregan.requel.command.EditCommand;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity;

/**
 * @author ron
 */
public interface RemoveUnneedLexicalIssuesCommand extends EditCommand {

	/**
	 * @param projectOrDomain
	 */
	public void setProjectOrDomain(ProjectOrDomain projectOrDomain);

	/**
	 * @return
	 */
	public ProjectOrDomainEntity getThingBeingAnalyzed();

	/**
	 * @param thingBeingAnalyzed
	 */
	public void setThingBeingAnalyzed(ProjectOrDomainEntity thingBeingAnalyzed);

	/**
	 * @param annotatableEntityPropertyName
	 */
	public void setAnnotatableEntityPropertyName(String annotatableEntityPropertyName);

	/**
	 * Set the NLPText version of the property being analyzed.
	 * 
	 * @param nlpText
	 */
	public void setNlpText(NLPText nlpText);

}
