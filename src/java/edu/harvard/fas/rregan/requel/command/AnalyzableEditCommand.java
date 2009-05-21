/*
 * $Id$
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

package edu.harvard.fas.rregan.requel.command;

/**
 * An EditCommand with a secondary process for analyzing the results of the edit
 * command after the execute() method completes successfully.
 * 
 * @author ron
 */
public interface AnalyzableEditCommand extends EditCommand {

	/**
	 * Turn on or off analysis. By default it is on.
	 * 
	 * @param analysisEnabled
	 */
	public void setAnalysisEnabled(boolean analysisEnabled);

	/**
	 * After a command executes successfully, the
	 * AssistantInvokingCommandHandler calls this method. It is up to the
	 * command to check if analysis should happen and to apply the appropriate
	 * analysis.
	 * 
	 * @see {@link AnalysisInvokingCommandHandler}
	 */
	public void invokeAnalysis();
}
