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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rreganjr.command.Command;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.command.AnalysisRequestSource;
import com.rreganjr.requel.project.command.ProjectAnalysisRequestSource;

/**
 * A CommandHandler decorator that executes a command and then triggers analysis
 * of the result. Two paths are supported:
 * <ul>
 * <li>Commands that implement {@link ProjectAnalysisRequestSource} (import, re-run analysis)
 * have every text entity of the project dispatched through the assistant SPI (#268).</li>
 * <li>Commands that implement {@link AnalysisRequestSource} are dispatched
 * through the assistant SPI via {@link AnalysisRequestDispatcher} — this keeps
 * the project/command modules free of any SPI dependency. Their step-sequence
 * targets (#455) are dispatched after, each with the step-sequence task.</li>
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
		if (executedCommand instanceof ProjectAnalysisRequestSource projectSource) {
			if (projectSource.getAnalysisProject() != null) {
				try {
					analysisRequestDispatcher.dispatchProject(projectSource.getAnalysisProject(),
							projectSource.getAnalysisTriggeredBy());
				} catch (Exception e) {
					log.warn("Project analysis dispatch failed for command {}: {}",
							command.getClass().getSimpleName(), e.getMessage(), e);
				}
			}
		} else if (executedCommand instanceof AnalysisRequestSource source) {
			if (source.getAnalysisTarget() != null) {
				try {
					analysisRequestDispatcher.dispatch(source.getAnalysisTarget(),
							source.getAnalysisTriggeredBy());
				} catch (Exception e) {
					log.warn("Assistant dispatch failed for command {}: {}",
							command.getClass().getSimpleName(), e.getMessage(), e);
				}
			}
			dispatchStepSequence(command, source);
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

	/**
	 * #455: re-check the step order of the scenarios an edited or deleted step is in. Each is a
	 * separate run that only the step-sequence assistant serves; one failing doesn't stop the
	 * others or the command.
	 */
	private void dispatchStepSequence(Command command, AnalysisRequestSource source) {
		List<Scenario> scenarios;
		try {
			scenarios = source.getStepSequenceTargets();
		} catch (Exception e) {
			log.warn("Step-sequence targets unavailable for command {}: {}",
					command.getClass().getSimpleName(), e.getMessage(), e);
			return;
		}
		if (scenarios == null) {
			return;
		}
		for (Scenario scenario : scenarios) {
			try {
				analysisRequestDispatcher.dispatch(scenario, source.getAnalysisTriggeredBy(),
						AnalysisRequestSource.STEP_SEQUENCE_TASK);
			} catch (Exception e) {
				log.warn("Step-sequence dispatch failed for command {}: {}",
						command.getClass().getSimpleName(), e.getMessage(), e);
			}
		}
	}
}
