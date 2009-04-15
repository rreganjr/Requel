/*
 * $Id: AnalyzableEditCommand.java,v 1.1 2009/02/13 12:08:03 rregan Exp $
 * Copyright (c) 2009 Ron Regan Jr. All Rights Reserved.
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
