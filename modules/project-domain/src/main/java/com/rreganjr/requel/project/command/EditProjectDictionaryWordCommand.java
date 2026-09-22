/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
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

import com.rreganjr.nlp.dictionary.command.EditDictionaryWordCommand;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectScopedCommand;

/**
 * The project-scoped view of {@link EditDictionaryWordCommand} (issue #312).
 * <p>
 * {@code EditDictionaryWordCommand} lives in {@code dictionary-jpa}, which is below
 * {@code project-domain} in the module graph and so cannot name a {@link Project} — #313 keyed
 * the word to a bare {@code Long} for that reason. Authorization needs the project itself:
 * {@code AuthorizingCommandHandler} reads it from {@link ProjectScopedCommand} and walks its
 * stakeholders. This interface is where the two meet, and it is what
 * {@code ProjectCommandFactory} hands back.
 * <p>
 * Callers set the project they already hold rather than an id. The stakeholder collection on a
 * {@code Project} is {@code FetchType.LAZY}, so a project re-loaded by id for the authorization
 * check is detached by the time the check walks it, and every caller is refused — which is
 * exactly what happened when this command first tried to resolve its own project.
 *
 * @author ron
 */
public interface EditProjectDictionaryWordCommand extends EditDictionaryWordCommand,
		ProjectScopedCommand {

	/**
	 * The project whose dictionary receives the word, and the project the caller is authorized
	 * against. Also sets the project id the write itself uses.
	 *
	 * @param project
	 */
	public void setProject(Project project);
}
