/*
 * $Id: RemoveUnneedLexicalIssuesCommand.java,v 1.1 2009/01/19 09:32:23 rregan Exp $
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirments
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
