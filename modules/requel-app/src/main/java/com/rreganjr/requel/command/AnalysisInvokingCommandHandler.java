/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rreganjr.command.Command;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.project.command.AnalysisRequestSource;

/**
 * A CommandHandler decorator that executes a command and then triggers analysis
 * of the result. Two paths are supported:
 * <ul>
 * <li>Commands that implement {@link AnalysisRequestSource} are dispatched
 * through the assistant SPI via {@link AnalysisRequestDispatcher} — this keeps
 * the project/command modules free of any SPI dependency.</li>
 * <li>Commands that only implement the legacy {@link AnalyzableEditCommand} have
 * their {@code invokeAnalysis()} called directly (the not-yet-migrated paths).</li>
 * </ul>
 * Analysis is fire-and-forget: a failure must never roll back the command that
 * already committed.
 *
 * @author ron
 */
public class AnalysisInvokingCommandHandler implements CommandHandler {

	private static final Logger log = LoggerFactory.getLogger(AnalysisInvokingCommandHandler.class);

	private final CommandHandler commandHandler;
	private final AnalysisRequestDispatcher analysisRequestDispatcher;

	public AnalysisInvokingCommandHandler(CommandHandler commandHandler,
			AnalysisRequestDispatcher analysisRequestDispatcher) {
		this.commandHandler = commandHandler;
		this.analysisRequestDispatcher = analysisRequestDispatcher;
	}

	public <T extends Command> T execute(T command) throws Exception {
		T executedCommand = commandHandler.execute(command);
		if (executedCommand instanceof AnalysisRequestSource source
				&& source.getAnalysisTarget() != null) {
			try {
				analysisRequestDispatcher.dispatch(source.getAnalysisTarget(),
						source.getAnalysisTriggeredBy());
			} catch (Exception e) {
				log.warn("Assistant dispatch failed for command {}: {}",
						command.getClass().getSimpleName(), e.getMessage(), e);
			}
		} else if (executedCommand instanceof AnalyzableEditCommand analyzable) {
			try {
				analyzable.invokeAnalysis();
			} catch (Exception e) {
				log.warn("NLP analysis failed for command {}: {}",
						command.getClass().getSimpleName(), e.getMessage(), e);
			}
		}
		return executedCommand;
	}
}
